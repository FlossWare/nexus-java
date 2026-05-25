package org.flossware.jnexus;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Error handling and edge case tests for NexusClient.
 * Tests malformed responses, network errors, and robustness.
 */
class NexusClientErrorTest {

    private HttpServer server;
    private int port;
    private NexusClient client;

    @BeforeEach
    void setUp() throws IOException {
        port = 8890; // Different port
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.start();

        Credentials credentials = new Credentials("http://localhost:" + port, "testuser", "testpass", null);
        client = new NexusClient(credentials, 0); // No caching
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    // ========== Malformed Response Handling ==========

    @Test
    void testMalformedJSONResponse() {
        server.createContext("/service/rest/v1/components", exchange -> {
            String malformedJSON = "{invalid json}";
            byte[] response = malformedJSON.getBytes();
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });

        assertThrows(IOException.class, () ->
            client.listComponents("test-repo"),
            "Should throw exception for malformed JSON"
        );
    }

    @Test
    void testMissingRequiredFieldsInJSON() {
        // JSON missing "assets" field
        String incompleteJSON = """
            {
              "items": [{
                "id": "test123"
              }]
            }
            """;

        server.createContext("/service/rest/v1/components", exchange -> {
            byte[] response = incompleteJSON.getBytes();
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });

        // Should throw exception for missing required fields
        assertThrows(Exception.class, () ->
            client.listComponents("test-repo"),
            "Should throw exception for missing assets field"
        );
    }

    @Test
    void testEmptyItemsArray() throws Exception {
        String emptyJSON = "{\"items\":[]}";

        server.createContext("/service/rest/v1/components", exchange -> {
            byte[] response = emptyJSON.getBytes();
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });

        List<RepoRecord> records = client.listComponents("test-repo");
        assertTrue(records.isEmpty(), "Empty items array should return empty list");
    }

    @Test
    void testNullItemsArray() throws Exception {
        String nullItemsJSON = "{\"items\":null}";

        server.createContext("/service/rest/v1/components", exchange -> {
            byte[] response = nullItemsJSON.getBytes();
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });

        assertThrows(Exception.class, () ->
            client.listComponents("test-repo"),
            "Null items array should throw exception"
        );
    }

    @Test
    void testUnexpectedDataTypes() throws Exception {
        // fileSize as string instead of number
        String invalidTypesJSON = """
            {
              "items": [{
                "id": "test",
                "assets": [{
                  "path": "test.jar",
                  "fileSize": "not-a-number"
                }]
              }]
            }
            """;

        server.createContext("/service/rest/v1/components", exchange -> {
            byte[] response = invalidTypesJSON.getBytes();
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });

        assertThrows(Exception.class, () ->
            client.listComponents("test-repo"),
            "Invalid data types should throw exception"
        );
    }

    // ========== Network Error Scenarios ==========

    @Test
    void testHTTPErrorCodes() throws IOException {
        // Test individual error codes in separate contexts
        int[] errorCodes = {400, 404, 500, 502, 503, 504};

        for (int statusCode : errorCodes) {
            // Create a new server for each test to avoid context conflicts
            HttpServer testServer = HttpServer.create(new InetSocketAddress(port + 1), 0);
            testServer.start();

            try {
                testServer.createContext("/service/rest/v1/components", exchange -> {
                    exchange.sendResponseHeaders(statusCode, 0);
                    exchange.getResponseBody().close();
                });

                Credentials testCreds = new Credentials("http://localhost:" + (port + 1), "testuser", "testpass", null);
                NexusClient testClient = new NexusClient(testCreds, 0);

                IOException exception = assertThrows(IOException.class, () ->
                    testClient.listComponents("test-repo"),
                    "Should throw exception for status code " + statusCode
                );

                assertTrue(exception.getMessage().contains(String.valueOf(statusCode)),
                    "Exception should mention status code " + statusCode);
            } finally {
                testServer.stop(0);
            }
        }
    }

    @Test
    void testServerErrorRetry() throws Exception {
        AtomicInteger attemptCount = new AtomicInteger(0);

        server.createContext("/service/rest/v1/components", exchange -> {
            int attempt = attemptCount.incrementAndGet();

            if (attempt < 3) {
                // First 2 attempts: return 500
                exchange.sendResponseHeaders(500, 0);
                exchange.getResponseBody().close();
            } else {
                // Third attempt: success
                String successJSON = "{\"items\":[]}";
                byte[] response = successJSON.getBytes();
                exchange.sendResponseHeaders(200, response.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response);
                }
            }
        });

        // Should succeed after retries
        List<RepoRecord> records = client.listComponents("test-repo");
        assertNotNull(records);
        assertEquals(3, attemptCount.get(), "Should retry 3 times total");
    }

    @Test
    void testClientErrorNoRetry() throws Exception {
        AtomicInteger attemptCount = new AtomicInteger(0);

        server.createContext("/service/rest/v1/components", exchange -> {
            attemptCount.incrementAndGet();
            exchange.sendResponseHeaders(400, 0); // Client error
            exchange.getResponseBody().close();
        });

        assertThrows(IOException.class, () ->
            client.listComponents("test-repo")
        );

        assertEquals(1, attemptCount.get(), "Client errors should not be retried");
    }

    @Test
    void testPartialResponse() {
        server.createContext("/service/rest/v1/components", exchange -> {
            // Send incomplete JSON (connection closed mid-stream)
            String partialJSON = "{\"items\":[{\"id\":\"test\",";
            byte[] response = partialJSON.getBytes();
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
            // Close without completing JSON
        });

        assertThrows(IOException.class, () ->
            client.listComponents("test-repo"),
            "Should throw exception for incomplete response"
        );
    }

    // ========== Edge Cases ==========

    @Test
    void testEmptyRepository() throws Exception {
        String emptyRepoJSON = "{\"items\":[]}";

        server.createContext("/service/rest/v1/components", exchange -> {
            byte[] response = emptyRepoJSON.getBytes();
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });

        List<RepoRecord> records = client.listComponents("empty-repo");
        assertTrue(records.isEmpty(), "Empty repository should return empty list");
    }

    @Test
    void testSingleComponent() throws Exception {
        String singleComponentJSON = """
            {
              "items": [{
                "id": "single",
                "assets": [{
                  "path": "test.jar",
                  "fileSize": 1000
                }]
              }]
            }
            """;

        server.createContext("/service/rest/v1/components", exchange -> {
            byte[] response = singleComponentJSON.getBytes();
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });

        List<RepoRecord> records = client.listComponents("single-repo");
        assertEquals(1, records.size(), "Should return single component");
        assertEquals("test.jar", records.get(0).path());
    }

    @Test
    void testVeryLargeFileSize() throws Exception {
        // Test with Long.MAX_VALUE
        String largeFileJSON = """
            {
              "items": [{
                "id": "huge",
                "assets": [{
                  "path": "huge.jar",
                  "fileSize": 9223372036854775807
                }]
              }]
            }
            """;

        server.createContext("/service/rest/v1/components", exchange -> {
            byte[] response = largeFileJSON.getBytes();
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });

        List<RepoRecord> records = client.listComponents("test-repo");
        assertEquals(1, records.size());
        assertEquals(Long.MAX_VALUE, records.get(0).fileSize());
    }

    @Test
    void testZeroFileSize() throws Exception {
        String zeroSizeJSON = """
            {
              "items": [{
                "id": "empty",
                "assets": [{
                  "path": "empty.txt",
                  "fileSize": 0
                }]
              }]
            }
            """;

        server.createContext("/service/rest/v1/components", exchange -> {
            byte[] response = zeroSizeJSON.getBytes();
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });

        List<RepoRecord> records = client.listComponents("test-repo");
        assertEquals(1, records.size());
        assertEquals(0, records.get(0).fileSize());
    }

    @Test
    void testNegativeFileSize() throws Exception {
        // Negative file size (implementation accepts it as-is from Nexus API)
        String negativeFileJSON = """
            {
              "items": [{
                "id": "invalid",
                "assets": [{
                  "path": "invalid.jar",
                  "fileSize": -1000
                }]
              }]
            }
            """;

        server.createContext("/service/rest/v1/components", exchange -> {
            byte[] response = negativeFileJSON.getBytes();
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });

        // Implementation accepts negative values from API (trusts Nexus data)
        List<RepoRecord> records = client.listComponents("test-repo");
        assertEquals(1, records.size(), "Should parse component even with negative size");
        assertEquals(-1000, records.get(0).fileSize(), "Should preserve negative file size from API");
    }

    @Test
    void testSpecialCharactersInPath() throws Exception {
        String specialCharsJSON = """
            {
              "items": [{
                "id": "special",
                "assets": [{
                  "path": "com/example/test%20file.jar",
                  "fileSize": 1000
                }]
              }]
            }
            """;

        server.createContext("/service/rest/v1/components", exchange -> {
            byte[] response = specialCharsJSON.getBytes();
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });

        List<RepoRecord> records = client.listComponents("test-repo");
        assertEquals(1, records.size());
        assertEquals("com/example/test%20file.jar", records.get(0).path());
    }

    @Test
    void testUnicodeInPath() throws Exception {
        String unicodeJSON = """
            {
              "items": [{
                "id": "unicode",
                "assets": [{
                  "path": "com/example/測試-файл-文件.jar",
                  "fileSize": 1000
                }]
              }]
            }
            """;

        server.createContext("/service/rest/v1/components", exchange -> {
            byte[] response = unicodeJSON.getBytes();
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });

        List<RepoRecord> records = client.listComponents("test-repo");
        assertEquals(1, records.size());
        assertTrue(records.get(0).path().contains("測試"));
    }

    // ========== Concurrent Access ==========

    @Test
    void testConcurrentCacheAccess() throws Exception {
        String jsonResponse = """
            {
              "items": [{
                "id": "test",
                "assets": [{
                  "path": "test.jar",
                  "fileSize": 1000
                }]
              }]
            }
            """;

        server.createContext("/service/rest/v1/components", exchange -> {
            byte[] response = jsonResponse.getBytes();
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });

        // Create client with caching enabled
        Credentials credentials = new Credentials("http://localhost:" + port, "testuser", "testpass", null);
        NexusClient cachedClient = new NexusClient(credentials); // 5-minute cache

        // Populate cache
        cachedClient.listComponents("test-repo");

        // Multiple threads reading from cache concurrently
        Thread[] threads = new Thread[10];
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                try {
                    List<RepoRecord> records = cachedClient.listComponents("test-repo");
                    assertNotNull(records);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                }
            });
            threads[i].start();
        }

        // Wait for all threads
        for (Thread thread : threads) {
            thread.join(5000);
            assertFalse(thread.isAlive(), "Thread should complete");
        }

        assertEquals(10, successCount.get(), "All concurrent reads should succeed");
        assertEquals(0, errorCount.get(), "No errors should occur");
    }

    @Test
    void testCacheClearDuringRead() throws Exception {
        String jsonResponse = "{\"items\":[]}";

        server.createContext("/service/rest/v1/components", exchange -> {
            byte[] response = jsonResponse.getBytes();
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });

        // Create client with caching
        Credentials credentials = new Credentials("http://localhost:" + port, "testuser", "testpass", null);
        NexusClient cachedClient = new NexusClient(credentials);

        // Populate cache
        cachedClient.listComponents("test-repo");

        // Clear cache while another thread might be reading
        Thread clearThread = new Thread(() -> {
            cachedClient.clearAllCache();
        });
        clearThread.start();

        // Read from cache (or fetch if cleared)
        assertDoesNotThrow(() -> cachedClient.listComponents("test-repo"),
            "Should handle cache clear gracefully");

        clearThread.join();
    }
}

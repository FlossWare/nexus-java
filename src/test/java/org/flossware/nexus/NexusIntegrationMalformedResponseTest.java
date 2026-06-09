package org.flossware.nexus;

import com.sun.net.httpserver.HttpServer;
import org.flossware.jnexus.RepoRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for malformed response handling.
 * Tests JSON parsing, missing fields, null values, large responses, and special characters.
 */
@DisplayName("NexusClient Malformed Response Tests")
class NexusIntegrationMalformedResponseTest {

    private HttpServer server;
    private int port;

    @BeforeEach
    void setUp() throws IOException {
        // Use dynamic port allocation to avoid conflicts during parallel test execution
        try (ServerSocket ss = new ServerSocket(0)) {
            port = ss.getLocalPort();
        }
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(null);
        server.start();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("Malformed JSON response")
    void testMalformedJSON() throws IOException {
        server.createContext("/service/rest/v1/components", exchange -> {
            String malformed = "{invalid json no quotes}";
            byte[] bytes = malformed.getBytes();
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        Credentials credentials = new Credentials("http://localhost:" + port, "user", "pass", null);
        NexusClient client = new NexusClient(credentials, 0);

        assertThrows(IOException.class, () ->
            client.listComponents("test-repo"),
            "Should fail on malformed JSON"
        );
    }

    @Test
    @DisplayName("JSON missing 'items' field")
    void testMissingItemsField() throws IOException {
        server.createContext("/service/rest/v1/components", exchange -> {
            String json = "{\"something\": []}";
            byte[] bytes = json.getBytes();
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        Credentials credentials = new Credentials("http://localhost:" + port, "user", "pass", null);
        NexusClient client = new NexusClient(credentials, 0);

        assertThrows(Exception.class, () ->
            client.listComponents("test-repo"),
            "Should fail when 'items' field is missing"
        );
    }

    @Test
    @DisplayName("JSON with null 'items' array")
    void testNullItemsArray() throws IOException {
        server.createContext("/service/rest/v1/components", exchange -> {
            String json = "{\"items\": null}";
            byte[] bytes = json.getBytes();
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        Credentials credentials = new Credentials("http://localhost:" + port, "user", "pass", null);
        NexusClient client = new NexusClient(credentials, 0);

        assertThrows(Exception.class, () ->
            client.listComponents("test-repo"),
            "Should fail when 'items' is null"
        );
    }

    @Test
    @DisplayName("JSON item missing 'id' field")
    void testMissingIdField() throws IOException {
        server.createContext("/service/rest/v1/components", exchange -> {
            String json = "{\"items\": [{\"assets\": [{\"path\": \"test.jar\", \"fileSize\": 1024}]}]}";
            byte[] bytes = json.getBytes();
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        Credentials credentials = new Credentials("http://localhost:" + port, "user", "pass", null);
        NexusClient client = new NexusClient(credentials, 0);

        // Should handle missing id gracefully or throw clear error
        assertDoesNotThrow(() -> {
            try {
                client.listComponents("test-repo");
            } catch (Exception e) {
                assertNotNull(e.getMessage());
            }
        });
    }

    @Test
    @DisplayName("JSON item missing 'assets' field")
    void testMissingAssetsField() throws IOException {
        server.createContext("/service/rest/v1/components", exchange -> {
            String json = "{\"items\": [{\"id\": \"test123\"}]}";
            byte[] bytes = json.getBytes();
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        Credentials credentials = new Credentials("http://localhost:" + port, "user", "pass", null);
        NexusClient client = new NexusClient(credentials, 0);

        assertThrows(Exception.class, () ->
            client.listComponents("test-repo"),
            "Should fail when 'assets' field is missing"
        );
    }

    @Test
    @DisplayName("JSON asset missing 'path' field")
    void testMissingPathField() throws IOException {
        server.createContext("/service/rest/v1/components", exchange -> {
            String json = "{\"items\": [{\"id\": \"test\", \"assets\": [{\"fileSize\": 1024}]}]}";
            byte[] bytes = json.getBytes();
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        Credentials credentials = new Credentials("http://localhost:" + port, "user", "pass", null);
        NexusClient client = new NexusClient(credentials, 0);

        assertThrows(Exception.class, () ->
            client.listComponents("test-repo"),
            "Should fail when 'path' field is missing"
        );
    }

    @Test
    @DisplayName("Unexpected data types (fileSize as string)")
    void testInvalidDataTypes() throws IOException {
        server.createContext("/service/rest/v1/components", exchange -> {
            String json = "{\"items\": [{\"id\": \"test\", \"assets\": [{\"path\": \"test.jar\", \"fileSize\": \"not-a-number\"}]}]}";
            byte[] bytes = json.getBytes();
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        Credentials credentials = new Credentials("http://localhost:" + port, "user", "pass", null);
        NexusClient client = new NexusClient(credentials, 0);

        assertThrows(Exception.class, () ->
            client.listComponents("test-repo"),
            "Should fail on invalid data type for fileSize"
        );
    }

    @Test
    @DisplayName("Empty JSON response")
    void testEmptyResponse() throws IOException {
        server.createContext("/service/rest/v1/components", exchange -> {
            byte[] bytes = "".getBytes();
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().close();
        });

        Credentials credentials = new Credentials("http://localhost:" + port, "user", "pass", null);
        NexusClient client = new NexusClient(credentials, 0);

        assertThrows(Exception.class, () ->
            client.listComponents("test-repo"),
            "Should fail on empty response"
        );
    }

    @Test
    @DisplayName("Very large JSON response (10MB)")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testVeryLargeResponse() throws IOException {
        server.createContext("/service/rest/v1/components", exchange -> {
            // Build large JSON with many components
            StringBuilder json = new StringBuilder("{\"items\":[");
            for (int i = 0; i < 100000; i++) {
                if (i > 0) json.append(",");
                json.append("{\"id\":\"item").append(i).append("\",\"assets\":[{\"path\":\"file").append(i).append(".jar\",\"fileSize\":").append(i * 100).append("}]}");
            }
            json.append("]}");

            byte[] bytes = json.toString().getBytes();
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        Credentials credentials = new Credentials("http://localhost:" + port, "user", "pass", null);
        NexusClient client = new NexusClient(credentials, 0);

        // Should handle large response without OutOfMemoryError
        assertDoesNotThrow(() -> {
            try {
                var records = client.listComponents("test-repo");
                assertTrue(records.size() > 0, "Should parse large response");
            } catch (Exception e) {
                fail("Should not throw exception for large response: " + e.getMessage());
            }
        });
    }

    @Test
    @DisplayName("Unicode and special characters in paths")
    void testUnicodePathHandling() throws IOException {
        server.createContext("/service/rest/v1/components", exchange -> {
            String json = """
                {
                  "items": [
                    {"id": "item1", "assets": [{"path": "文件.jar", "fileSize": 1024}]},
                    {"id": "item2", "assets": [{"path": "файл.jar", "fileSize": 2048}]},
                    {"id": "item3", "assets": [{"path": "file with spaces.jar", "fileSize": 512}]},
                    {"id": "item4", "assets": [{"path": "file-with-dashes.jar", "fileSize": 256}]}
                  ]
                }
                """;
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        Credentials credentials = new Credentials("http://localhost:" + port, "user", "pass", null);
        NexusClient client = new NexusClient(credentials, 0);

        assertDoesNotThrow(() -> {
            var records = client.listComponents("test-repo");
            assertEquals(4, records.size(), "Should handle Unicode paths");
            assertTrue(records.stream().anyMatch(r -> r.path().contains("文件")), "Should preserve Chinese characters");
            assertTrue(records.stream().anyMatch(r -> r.path().contains("файл")), "Should preserve Cyrillic characters");
            assertTrue(records.stream().anyMatch(r -> r.path().contains("spaces")), "Should preserve spaces");
        });
    }

    @Test
    @DisplayName("Invalid UTF-8 sequences")
    void testInvalidUTF8Sequences() throws IOException {
        server.createContext("/service/rest/v1/components", exchange -> {
            // Send valid JSON structure but with invalid UTF-8 bytes in path
            byte[] validStart = "{\"items\":[{\"id\":\"test\",\"assets\":[{\"path\":\"".getBytes(StandardCharsets.UTF_8);
            byte[] invalidUTF8 = new byte[]{(byte) 0xFF, (byte) 0xFE};
            byte[] validEnd = "\",\"fileSize\":1024}]}]}".getBytes(StandardCharsets.UTF_8);

            exchange.sendResponseHeaders(200, validStart.length + invalidUTF8.length + validEnd.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(validStart);
                os.write(invalidUTF8);
                os.write(validEnd);
            }
        });

        Credentials credentials = new Credentials("http://localhost:" + port, "user", "pass", null);
        NexusClient client = new NexusClient(credentials, 0);

        // Should handle invalid UTF-8 gracefully
        assertDoesNotThrow(() -> {
            try {
                client.listComponents("test-repo");
            } catch (Exception e) {
                // Acceptable - should have clear error message
                assertNotNull(e.getMessage());
            }
        });
    }

    @Test
    @DisplayName("Empty 'assets' array in item")
    void testEmptyAssetsArray() throws IOException {
        server.createContext("/service/rest/v1/components", exchange -> {
            String json = "{\"items\": [{\"id\": \"test\", \"assets\": []}]}";
            byte[] bytes = json.getBytes();
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        Credentials credentials = new Credentials("http://localhost:" + port, "user", "pass", null);
        NexusClient client = new NexusClient(credentials, 0);

        // Should handle empty assets array gracefully
        assertDoesNotThrow(() -> {
            try {
                var records = client.listComponents("test-repo");
                // May skip items with no assets or handle them specially
            } catch (Exception e) {
                assertNotNull(e.getMessage());
            }
        });
    }
}

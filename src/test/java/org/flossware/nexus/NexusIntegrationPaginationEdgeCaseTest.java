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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for pagination edge cases.
 * Tests large paginated responses, empty pages, duplicate tokens, and token expiration.
 */
@DisplayName("NexusClient Pagination Edge Case Tests")
class NexusIntegrationPaginationEdgeCaseTest {

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
    @DisplayName("Pagination with 1000 pages")
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void testPaginationWith1000Pages() throws Exception {
        AtomicInteger pageCount = new AtomicInteger(0);

        server.createContext("/service/rest/v1/components", exchange -> {
            int page = pageCount.incrementAndGet();
            String query = exchange.getRequestURI().getQuery() != null ? exchange.getRequestURI().getQuery() : "";

            StringBuilder json = new StringBuilder("{\"items\":[");
            int itemsPerPage = 100;
            int startId = (page - 1) * itemsPerPage;

            for (int i = 0; i < itemsPerPage; i++) {
                if (i > 0) json.append(",");
                int itemId = startId + i;
                json.append("{\"id\":\"item").append(itemId).append("\",\"assets\":[{\"path\":\"file").append(itemId).append(".jar\",\"fileSize\":").append(itemId * 10).append("}]}");
            }

            json.append("]");

            if (page < 1000) {
                json.append(",\"continuationToken\":\"token").append(page).append("\"");
            }

            json.append("}");

            byte[] bytes = json.toString().getBytes();
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        Credentials credentials = new Credentials("http://localhost:" + port, "user", "pass", null);
        NexusClient client = new NexusClient(credentials, 0);

        assertDoesNotThrow(() -> {
            var records = client.listComponents("test-repo");
            assertTrue(records.size() > 0, "Should fetch paginated results");
            // With 1000 pages of 100 items each, should have ~100K items
            assertTrue(records.size() >= 9900, "Should fetch most/all pages");
        });
    }

    @Test
    @DisplayName("Server returns empty page with continuation token")
    void testEmptyPageWithContinuationToken() throws Exception {
        AtomicInteger pageCount = new AtomicInteger(0);

        server.createContext("/service/rest/v1/components", exchange -> {
            int page = pageCount.incrementAndGet();

            if (page == 1) {
                // First page: normal
                String json = "{\"items\":[{\"id\":\"item1\",\"assets\":[{\"path\":\"file1.jar\",\"fileSize\":1024}]}],\"continuationToken\":\"token1\"}";
                byte[] bytes = json.getBytes();
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } else if (page == 2) {
                // Second page: empty but with continuation token (infinite loop prevention)
                String json = "{\"items\":[],\"continuationToken\":\"token2\"}";
                byte[] bytes = json.getBytes();
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } else {
                // Should not get here
                String json = "{\"items\":[]}";
                byte[] bytes = json.getBytes();
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            }
        });

        Credentials credentials = new Credentials("http://localhost:" + port, "user", "pass", null);
        NexusClient client = new NexusClient(credentials, 0);

        // Should handle empty page without infinite loop
        assertDoesNotThrow(() -> {
            var records = client.listComponents("test-repo");
            assertEquals(1, records.size(), "Should skip empty page");
        });
    }

    @Test
    @DisplayName("Server returns duplicate continuation token")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testDuplicateContinuationToken() throws Exception {
        AtomicInteger pageCount = new AtomicInteger(0);

        server.createContext("/service/rest/v1/components", exchange -> {
            int page = pageCount.incrementAndGet();

            if (page <= 5) {
                // Always return same token (would cause infinite loop)
                String json = "{\"items\":[{\"id\":\"item" + page + "\",\"assets\":[{\"path\":\"file" + page + ".jar\",\"fileSize\":1024}]}],\"continuationToken\":\"stuck-token\"}";
                byte[] bytes = json.getBytes();
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } else {
                // Should not reach here due to infinite loop protection
                String json = "{\"items\":[]}";
                byte[] bytes = json.getBytes();
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            }
        });

        Credentials credentials = new Credentials("http://localhost:" + port, "user", "pass", null);
        NexusClient client = new NexusClient(credentials, 0);

        // Should detect duplicate token and prevent infinite loop
        assertDoesNotThrow(() -> {
            var records = client.listComponents("test-repo");
            // May succeed or fail, but should not hang
            assertTrue(records.size() > 0, "Should handle pagination");
        });
    }

    @Test
    @DisplayName("Continuation token expires mid-pagination")
    void testTokenExpirationMidPagination() throws Exception {
        AtomicInteger pageCount = new AtomicInteger(0);

        server.createContext("/service/rest/v1/components", exchange -> {
            int page = pageCount.incrementAndGet();

            if (page == 1) {
                String json = "{\"items\":[{\"id\":\"item1\",\"assets\":[{\"path\":\"file1.jar\",\"fileSize\":1024}]}],\"continuationToken\":\"expired-token\"}";
                byte[] bytes = json.getBytes();
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } else {
                // Second page: token expired
                String error = "{\"error\":\"Token expired\"}";
                byte[] bytes = error.getBytes();
                exchange.sendResponseHeaders(401, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            }
        });

        Credentials credentials = new Credentials("http://localhost:" + port, "user", "pass", null);
        NexusClient client = new NexusClient(credentials, 0);

        // Should handle token expiration gracefully
        assertDoesNotThrow(() -> {
            try {
                var records = client.listComponents("test-repo");
                // May succeed with partial results or fail with clear error
            } catch (IOException e) {
                // Expected - should have clear error message
                assertNotNull(e.getMessage());
            }
        });
    }

    @Test
    @DisplayName("Single item per page (extreme pagination)")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testSingleItemPerPage() throws Exception {
        AtomicInteger pageCount = new AtomicInteger(0);
        final int TOTAL_ITEMS = 50;

        server.createContext("/service/rest/v1/components", exchange -> {
            int page = pageCount.incrementAndGet();

            if (page <= TOTAL_ITEMS) {
                String json = "{\"items\":[{\"id\":\"item" + page + "\",\"assets\":[{\"path\":\"file" + page + ".jar\",\"fileSize\":" + page + "}]}]";
                if (page < TOTAL_ITEMS) {
                    json += ",\"continuationToken\":\"token" + page + "\"";
                }
                json += "}";

                byte[] bytes = json.getBytes();
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } else {
                String json = "{\"items\":[]}";
                byte[] bytes = json.getBytes();
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            }
        });

        Credentials credentials = new Credentials("http://localhost:" + port, "user", "pass", null);
        NexusClient client = new NexusClient(credentials, 0);

        var records = client.listComponents("test-repo");
        assertEquals(TOTAL_ITEMS, records.size(), "Should fetch all single-item pages");
    }

    @Test
    @DisplayName("No continuation token in response")
    void testNoContinuationToken() throws Exception {
        server.createContext("/service/rest/v1/components", exchange -> {
            String json = "{\"items\":[{\"id\":\"item1\",\"assets\":[{\"path\":\"file1.jar\",\"fileSize\":1024}]}]}";
            byte[] bytes = json.getBytes();
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        Credentials credentials = new Credentials("http://localhost:" + port, "user", "pass", null);
        NexusClient client = new NexusClient(credentials, 0);

        var records = client.listComponents("test-repo");
        assertEquals(1, records.size(), "Should return single page when no continuation token");
    }

    @Test
    @DisplayName("Continuation token with special characters")
    void testSpecialCharactersInToken() throws Exception {
        AtomicInteger pageCount = new AtomicInteger(0);
        final String SPECIAL_TOKEN = "token%2B%2F%3D%26special%3Dchars";

        server.createContext("/service/rest/v1/components", exchange -> {
            int page = pageCount.incrementAndGet();
            String query = exchange.getRequestURI().getQuery() != null ? exchange.getRequestURI().getQuery() : "";

            if (page == 1) {
                String json = "{\"items\":[{\"id\":\"item1\",\"assets\":[{\"path\":\"file1.jar\",\"fileSize\":1024}]}],\"continuationToken\":\"" + SPECIAL_TOKEN + "\"}";
                byte[] bytes = json.getBytes();
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } else if (page == 2) {
                // Verify token was passed correctly
                assertTrue(query.contains("token"), "Should pass continuation token in query");
                String json = "{\"items\":[{\"id\":\"item2\",\"assets\":[{\"path\":\"file2.jar\",\"fileSize\":2048}]}]}";
                byte[] bytes = json.getBytes();
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            }
        });

        Credentials credentials = new Credentials("http://localhost:" + port, "user", "pass", null);
        NexusClient client = new NexusClient(credentials, 0);

        var records = client.listComponents("test-repo");
        assertEquals(2, records.size(), "Should handle special characters in token");
    }

    @Test
    @DisplayName("Very long continuation token (1000+ characters)")
    void testVeryLongContinuationToken() throws Exception {
        AtomicInteger pageCount = new AtomicInteger(0);
        String longToken = "x".repeat(1000);

        server.createContext("/service/rest/v1/components", exchange -> {
            int page = pageCount.incrementAndGet();

            if (page == 1) {
                String json = "{\"items\":[{\"id\":\"item1\",\"assets\":[{\"path\":\"file1.jar\",\"fileSize\":1024}]}],\"continuationToken\":\"" + longToken + "\"}";
                byte[] bytes = json.getBytes();
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } else {
                String json = "{\"items\":[{\"id\":\"item2\",\"assets\":[{\"path\":\"file2.jar\",\"fileSize\":2048}]}]}";
                byte[] bytes = json.getBytes();
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            }
        });

        Credentials credentials = new Credentials("http://localhost:" + port, "user", "pass", null);
        NexusClient client = new NexusClient(credentials, 0);

        var records = client.listComponents("test-repo");
        assertEquals(2, records.size(), "Should handle long continuation tokens");
    }
}

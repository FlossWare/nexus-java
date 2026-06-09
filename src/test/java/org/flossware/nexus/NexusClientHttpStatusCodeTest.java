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
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for HTTP status code handling in NexusClient.
 * <p>
 * Covers scenarios from issue #67:
 * - HTTP 401 Unauthorized with clear error message
 * - HTTP 403 Forbidden indicating permission issue
 * - HTTP 429 Rate Limited with retry and backoff
 * - HTTP 500 Server Error with retry logic
 * - HTTP 502 Bad Gateway with retry
 * - HTTP 503 Service Unavailable with retry
 * - Retry count verification for retryable vs non-retryable errors
 * - Delete operation error codes
 * </p>
 */
@DisplayName("NexusClient HTTP Status Code Handling Tests")
class NexusClientHttpStatusCodeTest {

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

    private NexusClient createClient() {
        Credentials credentials = new Credentials("http://localhost:" + port, "testuser", "testpass", null);
        return new NexusClient(credentials, 0);
    }

    @Test
    @DisplayName("HTTP 401 Unauthorized should produce clear error message")
    void testHTTP401Unauthorized() {
        server.createContext("/service/rest/v1/components", exchange -> {
            String body = "Unauthorized: Invalid credentials";
            byte[] response = body.getBytes();
            exchange.sendResponseHeaders(401, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });

        NexusClient client = createClient();
        IOException exception = assertThrows(IOException.class, () ->
            client.listComponents("test-repo"),
            "Should throw IOException for 401"
        );

        String message = exception.getMessage();
        assertTrue(message.contains("401"),
            "Error message should contain status code 401: " + message);
    }

    @Test
    @DisplayName("HTTP 403 Forbidden should produce clear error message indicating permission issue")
    void testHTTP403Forbidden() {
        server.createContext("/service/rest/v1/components", exchange -> {
            String body = "Forbidden: Insufficient permissions";
            byte[] response = body.getBytes();
            exchange.sendResponseHeaders(403, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });

        NexusClient client = createClient();
        IOException exception = assertThrows(IOException.class, () ->
            client.listComponents("test-repo"),
            "Should throw IOException for 403"
        );

        String message = exception.getMessage();
        assertTrue(message.contains("403"),
            "Error message should contain status code 403: " + message);
    }

    @Test
    @DisplayName("HTTP 401 should NOT be retried (auth errors are not transient)")
    void testHTTP401NotRetried() {
        AtomicInteger requestCount = new AtomicInteger(0);

        server.createContext("/service/rest/v1/components", exchange -> {
            requestCount.incrementAndGet();
            exchange.sendResponseHeaders(401, 0);
            exchange.getResponseBody().close();
        });

        NexusClient client = createClient();
        assertThrows(IOException.class, () -> client.listComponents("test-repo"));

        assertEquals(1, requestCount.get(),
            "HTTP 401 should not be retried");
    }

    @Test
    @DisplayName("HTTP 403 should NOT be retried (permission errors are not transient)")
    void testHTTP403NotRetried() {
        AtomicInteger requestCount = new AtomicInteger(0);

        server.createContext("/service/rest/v1/components", exchange -> {
            requestCount.incrementAndGet();
            exchange.sendResponseHeaders(403, 0);
            exchange.getResponseBody().close();
        });

        NexusClient client = createClient();
        assertThrows(IOException.class, () -> client.listComponents("test-repo"));

        assertEquals(1, requestCount.get(),
            "HTTP 403 should not be retried");
    }

    @Test
    @DisplayName("HTTP 429 Rate Limited should be retried with backoff")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testHTTP429RateLimitedWithRetry() throws Exception {
        AtomicInteger requestCount = new AtomicInteger(0);

        server.createContext("/service/rest/v1/components", exchange -> {
            int attempt = requestCount.incrementAndGet();

            if (attempt < 3) {
                // First two attempts: rate limited
                exchange.sendResponseHeaders(429, 0);
                exchange.getResponseBody().close();
            } else {
                // Third attempt: success
                String response = "{\"items\":[]}";
                byte[] bytes = response.getBytes();
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            }
        });

        NexusClient client = createClient();
        List<RepoRecord> records = client.listComponents("test-repo");

        assertNotNull(records, "Should eventually succeed after rate limit retries");
        assertEquals(3, requestCount.get(), "Should retry after 429 status");
    }

    @Test
    @DisplayName("HTTP 500 Server Error should be retried")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testHTTP500ServerErrorRetried() throws Exception {
        AtomicInteger requestCount = new AtomicInteger(0);

        server.createContext("/service/rest/v1/components", exchange -> {
            int attempt = requestCount.incrementAndGet();

            if (attempt < 3) {
                exchange.sendResponseHeaders(500, 0);
                exchange.getResponseBody().close();
            } else {
                String response = "{\"items\":[{\"id\":\"comp1\",\"assets\":[{\"path\":\"test.jar\",\"fileSize\":1000}]}]}";
                byte[] bytes = response.getBytes();
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            }
        });

        NexusClient client = createClient();
        List<RepoRecord> records = client.listComponents("test-repo");

        assertEquals(1, records.size(), "Should return records after retry succeeds");
        assertEquals(3, requestCount.get(), "Should retry on 500 errors");
    }

    @Test
    @DisplayName("HTTP 502 Bad Gateway should be retried")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testHTTP502BadGatewayRetried() throws Exception {
        AtomicInteger requestCount = new AtomicInteger(0);

        server.createContext("/service/rest/v1/components", exchange -> {
            int attempt = requestCount.incrementAndGet();

            if (attempt < 2) {
                exchange.sendResponseHeaders(502, 0);
                exchange.getResponseBody().close();
            } else {
                String response = "{\"items\":[]}";
                byte[] bytes = response.getBytes();
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            }
        });

        NexusClient client = createClient();
        List<RepoRecord> records = client.listComponents("test-repo");

        assertNotNull(records, "Should succeed after 502 retry");
        assertEquals(2, requestCount.get(), "Should retry on 502");
    }

    @Test
    @DisplayName("HTTP 503 Service Unavailable should be retried")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testHTTP503ServiceUnavailableRetried() throws Exception {
        AtomicInteger requestCount = new AtomicInteger(0);

        server.createContext("/service/rest/v1/components", exchange -> {
            int attempt = requestCount.incrementAndGet();

            if (attempt < 2) {
                exchange.sendResponseHeaders(503, 0);
                exchange.getResponseBody().close();
            } else {
                String response = "{\"items\":[]}";
                byte[] bytes = response.getBytes();
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            }
        });

        NexusClient client = createClient();
        List<RepoRecord> records = client.listComponents("test-repo");

        assertNotNull(records, "Should succeed after 503 retry");
        assertEquals(2, requestCount.get(), "Should retry on 503");
    }

    @Test
    @DisplayName("Delete non-existent component should throw IOException with 404")
    void testDeleteNonExistentComponent() {
        server.createContext("/service/rest/v1/components/", exchange -> {
            exchange.sendResponseHeaders(404, 0);
            exchange.getResponseBody().close();
        });

        NexusClient client = createClient();
        IOException exception = assertThrows(IOException.class, () ->
            client.deleteComponent("non-existent-id"),
            "Should throw IOException for 404 on delete"
        );

        assertTrue(exception.getMessage().contains("404"),
            "Error should mention 404 status");
    }

    @Test
    @DisplayName("Delete with 500 error should retry and eventually succeed")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testDeleteRetryOnServerError() throws Exception {
        AtomicInteger requestCount = new AtomicInteger(0);

        server.createContext("/service/rest/v1/components/", exchange -> {
            int attempt = requestCount.incrementAndGet();

            if (attempt < 2) {
                exchange.sendResponseHeaders(500, 0);
                exchange.getResponseBody().close();
            } else {
                exchange.sendResponseHeaders(204, -1);
                exchange.getResponseBody().close();
            }
        });

        NexusClient client = createClient();
        assertDoesNotThrow(() -> client.deleteComponent("retry-delete-id"),
            "Delete should succeed after retry");

        assertEquals(2, requestCount.get(), "Should retry delete on server error");
    }

    @Test
    @DisplayName("Delete with 200 status should also be accepted as success")
    void testDeleteAccepts200AsSuccess() throws Exception {
        server.createContext("/service/rest/v1/components/", exchange -> {
            exchange.sendResponseHeaders(200, 0);
            exchange.getResponseBody().close();
        });

        NexusClient client = createClient();
        assertDoesNotThrow(() -> client.deleteComponent("comp-id"),
            "Delete should accept 200 as success");
    }

    @Test
    @DisplayName("Max retries exhausted should throw last exception")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testMaxRetriesExhausted() {
        AtomicInteger requestCount = new AtomicInteger(0);

        server.createContext("/service/rest/v1/components", exchange -> {
            requestCount.incrementAndGet();
            exchange.sendResponseHeaders(500, 0);
            exchange.getResponseBody().close();
        });

        NexusClient client = createClient();
        IOException exception = assertThrows(IOException.class, () ->
            client.listComponents("test-repo"),
            "Should throw after max retries exhausted"
        );

        assertTrue(exception.getMessage().contains("500"),
            "Should throw last 500 error after retries exhausted");
        assertEquals(3, requestCount.get(),
            "Should have tried 3 times (default max retries)");
    }

    @Test
    @DisplayName("HTTP 404 on list should NOT be retried (not transient)")
    void testHTTP404NotRetried() {
        AtomicInteger requestCount = new AtomicInteger(0);

        server.createContext("/service/rest/v1/components", exchange -> {
            requestCount.incrementAndGet();
            exchange.sendResponseHeaders(404, 0);
            exchange.getResponseBody().close();
        });

        NexusClient client = createClient();
        assertThrows(IOException.class, () -> client.listComponents("test-repo"));

        assertEquals(1, requestCount.get(),
            "HTTP 404 should not be retried");
    }
}

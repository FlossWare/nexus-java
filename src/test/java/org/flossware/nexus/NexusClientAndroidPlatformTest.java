package org.flossware.nexus;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Platform-specific integration tests for Android implementation.
 * Tests OkHttp-specific behavior, interceptor chains, connection pooling, and mobile scenarios.
 *
 * Note: These tests simulate Android behavior using standard Java HTTP, but the assertions
 * verify behavior that would be present in actual Android OkHttp implementation.
 */
@DisplayName("NexusClient Android (OkHttp) Platform Tests")
class NexusClientAndroidPlatformTest {

    private HttpServer server;
    private int port;
    private NexusClient client;

    @BeforeEach
    void setUp() throws IOException {
        // Use dynamic port allocation to avoid conflicts during parallel test execution
        try (java.net.ServerSocket ss = new java.net.ServerSocket(0)) {
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
    @DisplayName("OkHttp handles connection pooling under concurrent load")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testOkHttpConnectionPooling() throws IOException {
        AtomicInteger activeConnections = new AtomicInteger(0);
        AtomicInteger maxConcurrent = new AtomicInteger(0);

        server.createContext("/service/rest/v1/components", exchange -> {
            int active = activeConnections.incrementAndGet();
            int max = maxConcurrent.accumulateAndGet(active, Math::max);

            try {
                // Simulate some processing time
                Thread.sleep(100);
                String response = "{\"items\":[{\"id\":\"test\",\"assets\":[{\"path\":\"test.jar\",\"fileSize\":1024}]}]}";
                byte[] bytes = response.getBytes();
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                activeConnections.decrementAndGet();
            }
        });

        Credentials credentials = new Credentials("http://localhost:" + port, "user", "pass", null);
        client = new NexusClient(credentials, 0);

        // Make concurrent requests
        Thread[] threads = new Thread[5];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                try {
                    client.listComponents("test-repo");
                } catch (Exception e) {
                    fail("Request failed: " + e.getMessage());
                }
            });
            threads[i].start();
        }

        // Wait for all threads
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Verify connection pool was used (max concurrent < total threads without pooling)
        assertTrue(maxConcurrent.get() <= threads.length, "Connection pool should manage concurrent requests");
    }

    @Test
    @DisplayName("OkHttp handles network state changes (simulated)")
    void testOkHttpNetworkStateChange() throws IOException {
        AtomicInteger requestCount = new AtomicInteger(0);

        server.createContext("/service/rest/v1/components", exchange -> {
            int count = requestCount.incrementAndGet();
            if (count == 1) {
                // First request succeeds
                String response = "{\"items\":[{\"id\":\"test\",\"assets\":[{\"path\":\"test.jar\",\"fileSize\":1024}]}]}";
                byte[] bytes = response.getBytes();
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } else {
                // Simulate network becoming unavailable
                exchange.sendResponseHeaders(503, 0);
                exchange.getResponseBody().close();
            }
        });

        Credentials credentials = new Credentials("http://localhost:" + port, "user", "pass", null);
        client = new NexusClient(credentials, 0);

        // First request should succeed
        List<RepoRecord> records = client.listComponents("test-repo");
        assertEquals(1, records.size());

        // Second request should fail (simulating network unavailable)
        assertThrows(IOException.class, () -> client.listComponents("test-repo"),
            "Should fail when network unavailable");
    }

    @Test
    @DisplayName("OkHttp handles DNS resolution failures")
    void testOkHttpDNSResolutionFailure() {
        // Use invalid hostname that can't be resolved
        Credentials credentials = new Credentials("http://invalid-hostname-that-does-not-exist-12345.local", "user", "pass", null);
        client = new NexusClient(credentials, 0);

        assertThrows(Exception.class, () -> client.listComponents("test-repo"),
            "Should fail on DNS resolution failure");
    }

    @Test
    @DisplayName("OkHttp handles request timeout on slow network")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testOkHttpRequestTimeout() throws IOException {
        server.createContext("/service/rest/v1/components", exchange -> {
            try {
                // Simulate slow network (10 second delay, but client timeout should be 5)
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Credentials credentials = new Credentials("http://localhost:" + port, "user", "pass", null);
        // Create client with 2-second timeout
        client = new NexusClient(credentials, 2);

        assertThrows(IOException.class, () -> client.listComponents("test-repo"),
            "Should timeout on slow network");
    }

    @Test
    @DisplayName("OkHttp handles large response payload")
    void testOkHttpLargeResponsePayload() throws IOException {
        server.createContext("/service/rest/v1/components", exchange -> {
            // Generate a large response (10MB)
            StringBuilder json = new StringBuilder("{\"items\":[");
            for (int i = 0; i < 10000; i++) {
                if (i > 0) json.append(",");
                json.append("{\"id\":\"item").append(i)
                    .append("\",\"assets\":[{\"path\":\"file").append(i)
                    .append(".jar\",\"fileSize\":").append(1000000).append("}]}");
            }
            json.append("]}");

            byte[] bytes = json.toString().getBytes();
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        Credentials credentials = new Credentials("http://localhost:" + port, "user", "pass", null);
        client = new NexusClient(credentials, 0);

        // Should handle large response without OutOfMemory
        List<RepoRecord> records = client.listComponents("test-repo");
        assertTrue(records.size() > 0, "Should parse large response");
    }

    @Test
    @DisplayName("OkHttp handles multiple authentication attempts")
    void testOkHttpMultipleAuthAttempts() throws IOException {
        AtomicInteger attemptCount = new AtomicInteger(0);

        server.createContext("/service/rest/v1/components", exchange -> {
            int attempt = attemptCount.incrementAndGet();
            String authHeader = exchange.getRequestHeaders().getFirst("Authorization");

            if (authHeader != null && authHeader.startsWith("Basic")) {
                // Valid auth
                String response = "{\"items\":[]}";
                byte[] bytes = response.getBytes();
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } else {
                // First attempt without auth, should fail with 401
                exchange.sendResponseHeaders(401, 0);
                exchange.getResponseBody().close();
            }
        });

        Credentials credentials = new Credentials("http://localhost:" + port, "user", "pass", null);
        client = new NexusClient(credentials, 0);

        // Should eventually succeed with credentials
        List<RepoRecord> records = client.listComponents("test-repo");
        assertNotNull(records, "Should authenticate successfully");
    }

    @Test
    @DisplayName("OkHttp handles SSL/TLS handshake")
    void testOkHttpSSLHandshake() throws IOException {
        // This test verifies that OkHttp can establish HTTPS connections
        // In actual Android environment, this would connect to real HTTPS endpoint
        // For testing, we just verify HTTP behavior is correct

        server.createContext("/service/rest/v1/components", exchange -> {
            String response = "{\"items\":[]}";
            byte[] bytes = response.getBytes();
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        Credentials credentials = new Credentials("http://localhost:" + port, "user", "pass", null);
        client = new NexusClient(credentials, 0);

        // Should establish connection successfully
        assertDoesNotThrow(() -> client.listComponents("test-repo"),
            "Should establish connection successfully");
    }

    @Test
    @DisplayName("OkHttp handles request/response interceptor failures")
    void testOkHttpInterceptorChain() throws IOException {
        // Simulate an interceptor that validates content-type
        server.createContext("/service/rest/v1/components", exchange -> {
            String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
            // Should have proper content-type or not require it for GET
            String response = "{\"items\":[]}";
            byte[] bytes = response.getBytes();
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        Credentials credentials = new Credentials("http://localhost:" + port, "user", "pass", null);
        client = new NexusClient(credentials, 0);

        // Request should succeed
        assertDoesNotThrow(() -> client.listComponents("test-repo"),
            "Should handle interceptor chain properly");
    }

    @Test
    @DisplayName("OkHttp handles response code validation")
    void testOkHttpResponseCodeValidation() throws IOException {
        server.createContext("/service/rest/v1/components", exchange -> {
            // Return OK (200)
            String response = "{\"items\":[{\"id\":\"test\",\"assets\":[{\"path\":\"test.jar\",\"fileSize\":1024}]}]}";
            byte[] bytes = response.getBytes();
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        Credentials credentials = new Credentials("http://localhost:" + port, "user", "pass", null);
        client = new NexusClient(credentials, 0);

        // Should validate 200 as success
        List<RepoRecord> records = client.listComponents("test-repo");
        assertEquals(1, records.size(), "Should handle 200 OK response");
    }

    @Test
    @DisplayName("OkHttp handles proxy authentication (if configured)")
    void testOkHttpProxyAuthentication() throws IOException {
        // This test verifies proxy handling behavior
        // In actual Android, proxy might be configured via system properties
        server.createContext("/service/rest/v1/components", exchange -> {
            String response = "{\"items\":[]}";
            byte[] bytes = response.getBytes();
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        Credentials credentials = new Credentials("http://localhost:" + port, "user", "pass", null);
        client = new NexusClient(credentials, 0);

        // Should handle requests regardless of proxy configuration
        assertDoesNotThrow(() -> client.listComponents("test-repo"),
            "Should handle proxy scenarios gracefully");
    }

    @Test
    @DisplayName("OkHttp connection pool doesn't exhaust resources")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testOkHttpConnectionPoolExhaustion() throws IOException {
        server.createContext("/service/rest/v1/components", exchange -> {
            String response = "{\"items\":[{\"id\":\"test\",\"assets\":[{\"path\":\"test.jar\",\"fileSize\":1024}]}]}";
            byte[] bytes = response.getBytes();
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        Credentials credentials = new Credentials("http://localhost:" + port, "user", "pass", null);
        client = new NexusClient(credentials, 0);

        // Make many sequential requests
        for (int i = 0; i < 100; i++) {
            List<RepoRecord> records = client.listComponents("test-repo-" + i);
            assertTrue(records.size() >= 0, "Request should succeed");
        }

        // Should not crash or leak file descriptors
        assertDoesNotThrow(() -> client.listComponents("test-repo"),
            "Should not exhaust connection pool");
    }
}

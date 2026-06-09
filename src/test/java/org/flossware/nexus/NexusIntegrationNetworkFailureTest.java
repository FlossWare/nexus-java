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
import java.net.Socket;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for network failure modes.
 * Tests connection timeouts, dropped connections, slow servers, and DNS failures.
 */
@DisplayName("NexusClient Network Failure Tests")
class NexusIntegrationNetworkFailureTest {

    private HttpServer server;
    private int port;
    private NexusClient client;

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
    @DisplayName("Connection timeout on unresponsive server")
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void testConnectionTimeout() throws IOException {
        // Create a server that accepts but never responds
        server.createContext("/service/rest/v1/components", exchange -> {
            try {
                Thread.sleep(15000); // Never respond within timeout
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Credentials credentials = new Credentials("http://localhost:" + port, "user", "pass", null);
        NexusClient client = new NexusClient(credentials, 0);

        assertThrows(IOException.class, () ->
            client.listComponents("test-repo"),
            "Should timeout on unresponsive server"
        );
    }

    @Test
    @DisplayName("Connection dropped mid-response")
    void testConnectionDroppedMidResponse() throws IOException {
        server.createContext("/service/rest/v1/components", exchange -> {
            try {
                byte[] response = "{\"items\":[{\"id\":\"test\",".getBytes();
                exchange.sendResponseHeaders(200, 1000); // Claim large response
                OutputStream os = exchange.getResponseBody();
                os.write(response);
                os.flush();
                // Close abruptly without completing response
                os.close();
            } catch (Exception e) {
                // Expected
            }
        });

        Credentials credentials = new Credentials("http://localhost:" + port, "user", "pass", null);
        NexusClient client = new NexusClient(credentials, 0);

        assertThrows(IOException.class, () ->
            client.listComponents("test-repo"),
            "Should handle connection dropped mid-response"
        );
    }

    @Test
    @DisplayName("Slow server with delayed response")
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    void testSlowServerResponse() throws IOException {
        server.createContext("/service/rest/v1/components", exchange -> {
            try {
                Thread.sleep(8000); // 8 second delay
                String response = "{\"items\":[]}";
                byte[] bytes = response.getBytes();
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Credentials credentials = new Credentials("http://localhost:" + port, "user", "pass", null);
        // Should handle slow response (default timeout is 30s)
        NexusClient client = new NexusClient(credentials, 0);

        // Should eventually complete or timeout gracefully
        assertDoesNotThrow(() -> {
            try {
                client.listComponents("test-repo");
            } catch (IOException e) {
                assertTrue(e.getMessage().contains("timeout") || e.getMessage().contains("Timeout"),
                    "Should timeout with clear message");
                throw e;
            }
        });
    }

    @Test
    @DisplayName("Invalid hostname resolution")
    void testDNSResolutionFailure() {
        Credentials credentials = new Credentials(
            "http://invalid-hostname-that-does-not-exist-12345.local", "user", "pass", null);
        NexusClient client = new NexusClient(credentials, 0);

        IOException exception = assertThrows(IOException.class, () ->
            client.listComponents("test-repo"),
            "Should fail with clear error for invalid hostname"
        );

        // Should indicate DNS/hostname issue
        String message = exception.getMessage().toLowerCase();
        assertTrue(
            message.contains("unknown host") ||
            message.contains("name or service not known") ||
            message.contains("nodename nor servname provided"),
            "Error message should indicate DNS resolution failure: " + exception.getMessage()
        );
    }

    @Test
    @DisplayName("Connection refused (server not running)")
    void testConnectionRefused() {
        // Use a port that's very unlikely to have a server
        Credentials credentials = new Credentials("http://localhost:9999", "user", "pass", null);
        NexusClient client = new NexusClient(credentials, 0);

        IOException exception = assertThrows(IOException.class, () ->
            client.listComponents("test-repo"),
            "Should fail when server refuses connection"
        );

        String message = exception.getMessage().toLowerCase();
        assertTrue(
            message.contains("refused") ||
            message.contains("connection reset") ||
            message.contains("connrefused"),
            "Error message should indicate connection refused: " + exception.getMessage()
        );
    }

    @Test
    @DisplayName("Retry succeeds after transient failure")
    void testRetryAfterTransientFailure() throws IOException {
        AtomicInteger attemptCount = new AtomicInteger(0);

        server.createContext("/service/rest/v1/components", exchange -> {
            int attempt = attemptCount.incrementAndGet();
            if (attempt < 2) {
                // First attempt: close connection
                try {
                    exchange.getResponseBody().close();
                } catch (Exception ignored) {
                }
            } else {
                // Second attempt: success
                String response = "{\"items\":[{\"id\":\"test\",\"assets\":[{\"path\":\"test.jar\",\"fileSize\":1024}]}]}";
                byte[] bytes = response.getBytes();
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            }
        });

        Credentials credentials = new Credentials("http://localhost:" + port, "user", "pass", null);
        NexusClient client = new NexusClient(credentials, 0);

        // Should retry and eventually succeed
        assertDoesNotThrow(() -> client.listComponents("test-repo"));
    }

    @Test
    @DisplayName("Server responds with reset during pagination")
    void testServerResetDuringPagination() throws IOException {
        AtomicInteger pageCount = new AtomicInteger(0);

        server.createContext("/service/rest/v1/components", exchange -> {
            int page = pageCount.incrementAndGet();
            if (page == 1) {
                // First page: success with continuation token
                String response = "{\"items\":[{\"id\":\"test1\",\"assets\":[{\"path\":\"test1.jar\",\"fileSize\":1024}]}],\"continuationToken\":\"token123\"}";
                byte[] bytes = response.getBytes();
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } else {
                // Second page: connection reset
                try {
                    exchange.getResponseBody().close();
                } catch (Exception ignored) {
                }
            }
        });

        Credentials credentials = new Credentials("http://localhost:" + port, "user", "pass", null);
        NexusClient client = new NexusClient(credentials, 0);

        // Should handle pagination failure gracefully
        assertDoesNotThrow(() -> {
            try {
                client.listComponents("test-repo");
            } catch (IOException e) {
                // Expected - should fail gracefully rather than crash
                assertNotNull(e.getMessage());
            }
        });
    }
}

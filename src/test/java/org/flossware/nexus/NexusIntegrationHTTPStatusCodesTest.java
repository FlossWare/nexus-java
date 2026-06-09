package org.flossware.nexus;

import com.sun.net.httpserver.HttpServer;
import org.flossware.jnexus.RepoRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for HTTP status code handling.
 * Tests auth failures, permissions, rate limiting, and server errors.
 */
@DisplayName("NexusClient HTTP Status Code Tests")
class NexusIntegrationHTTPStatusCodesTest {

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
    @DisplayName("HTTP 401 Unauthorized with invalid credentials")
    void testHTTP401Unauthorized() throws IOException {
        server.createContext("/service/rest/v1/components", exchange -> {
            exchange.sendResponseHeaders(401, 0);
            exchange.getResponseBody().close();
        });

        Credentials credentials = new Credentials("http://localhost:" + port, "baduser", "badpass", null);
        client = new NexusClient(credentials, 0);

        IOException exception = assertThrows(IOException.class, () ->
            client.listComponents("test-repo"),
            "Should fail with 401 Unauthorized"
        );

        String message = exception.getMessage().toLowerCase();
        assertTrue(
            message.contains("401") || message.contains("unauthorized"),
            "Error should indicate authentication failure: " + exception.getMessage()
        );
    }

    @Test
    @DisplayName("HTTP 403 Forbidden - insufficient permissions")
    void testHTTP403Forbidden() throws IOException {
        server.createContext("/service/rest/v1/components", exchange -> {
            exchange.sendResponseHeaders(403, 0);
            exchange.getResponseBody().close();
        });

        Credentials credentials = new Credentials("http://localhost:" + port, "limiteduser", "pass", null);
        client = new NexusClient(credentials, 0);

        IOException exception = assertThrows(IOException.class, () ->
            client.listComponents("test-repo"),
            "Should fail with 403 Forbidden"
        );

        String message = exception.getMessage().toLowerCase();
        assertTrue(
            message.contains("403") || message.contains("forbidden"),
            "Error should indicate permission issue: " + exception.getMessage()
        );
    }

    @Test
    @DisplayName("HTTP 404 Not Found - repository doesn't exist")
    void testHTTP404NotFound() throws IOException {
        server.createContext("/service/rest/v1/components", exchange -> {
            exchange.sendResponseHeaders(404, 0);
            exchange.getResponseBody().close();
        });

        Credentials credentials = new Credentials("http://localhost:" + port, "user", "pass", null);
        client = new NexusClient(credentials, 0);

        IOException exception = assertThrows(IOException.class, () ->
            client.listComponents("nonexistent-repo"),
            "Should fail with 404 Not Found"
        );

        String message = exception.getMessage().toLowerCase();
        assertTrue(
            message.contains("404") || message.contains("not found"),
            "Error should indicate resource not found: " + exception.getMessage()
        );
    }

    @Test
    @DisplayName("HTTP 429 Too Many Requests - rate limited")
    void testHTTP429RateLimited() throws IOException {
        server.createContext("/service/rest/v1/components", exchange -> {
            exchange.getResponseHeaders().add("Retry-After", "5");
            exchange.sendResponseHeaders(429, 0);
            exchange.getResponseBody().close();
        });

        Credentials credentials = new Credentials("http://localhost:" + port, "user", "pass", null);
        client = new NexusClient(credentials, 0);

        IOException exception = assertThrows(IOException.class, () ->
            client.listComponents("test-repo"),
            "Should fail with 429 Too Many Requests"
        );

        String message = exception.getMessage().toLowerCase();
        assertTrue(
            message.contains("429") || message.contains("rate limited") || message.contains("too many"),
            "Error should indicate rate limiting: " + exception.getMessage()
        );
    }

    @Test
    @DisplayName("HTTP 500 Internal Server Error")
    void testHTTP500ServerError() throws IOException {
        server.createContext("/service/rest/v1/components", exchange -> {
            exchange.sendResponseHeaders(500, 0);
            exchange.getResponseBody().close();
        });

        Credentials credentials = new Credentials("http://localhost:" + port, "user", "pass", null);
        client = new NexusClient(credentials, 0);

        IOException exception = assertThrows(IOException.class, () ->
            client.listComponents("test-repo"),
            "Should fail with 500 Server Error"
        );

        String message = exception.getMessage().toLowerCase();
        assertTrue(
            message.contains("500") || message.contains("server error"),
            "Error should indicate server error: " + exception.getMessage()
        );
    }

    @Test
    @DisplayName("HTTP 502 Bad Gateway")
    void testHTTP502BadGateway() throws IOException {
        server.createContext("/service/rest/v1/components", exchange -> {
            exchange.sendResponseHeaders(502, 0);
            exchange.getResponseBody().close();
        });

        Credentials credentials = new Credentials("http://localhost:" + port, "user", "pass", null);
        client = new NexusClient(credentials, 0);

        IOException exception = assertThrows(IOException.class, () ->
            client.listComponents("test-repo"),
            "Should fail with 502 Bad Gateway"
        );

        String message = exception.getMessage().toLowerCase();
        assertTrue(
            message.contains("502") || message.contains("bad gateway"),
            "Error should indicate gateway error: " + exception.getMessage()
        );
    }

    @Test
    @DisplayName("HTTP 503 Service Unavailable")
    void testHTTP503ServiceUnavailable() throws IOException {
        server.createContext("/service/rest/v1/components", exchange -> {
            exchange.sendResponseHeaders(503, 0);
            exchange.getResponseBody().close();
        });

        Credentials credentials = new Credentials("http://localhost:" + port, "user", "pass", null);
        client = new NexusClient(credentials, 0);

        IOException exception = assertThrows(IOException.class, () ->
            client.listComponents("test-repo"),
            "Should fail with 503 Service Unavailable"
        );

        String message = exception.getMessage().toLowerCase();
        assertTrue(
            message.contains("503") || message.contains("unavailable"),
            "Error should indicate service unavailable: " + exception.getMessage()
        );
    }

    @Test
    @DisplayName("HTTP 504 Gateway Timeout")
    void testHTTP504GatewayTimeout() throws IOException {
        server.createContext("/service/rest/v1/components", exchange -> {
            exchange.sendResponseHeaders(504, 0);
            exchange.getResponseBody().close();
        });

        Credentials credentials = new Credentials("http://localhost:" + port, "user", "pass", null);
        client = new NexusClient(credentials, 0);

        IOException exception = assertThrows(IOException.class, () ->
            client.listComponents("test-repo"),
            "Should fail with 504 Gateway Timeout"
        );

        String message = exception.getMessage().toLowerCase();
        assertTrue(
            message.contains("504") || message.contains("timeout"),
            "Error should indicate gateway timeout: " + exception.getMessage()
        );
    }

    @Test
    @DisplayName("Retry succeeds after transient 500 error")
    void testRetryAfterTransient500() throws IOException {
        AtomicInteger attemptCount = new AtomicInteger(0);

        server.createContext("/service/rest/v1/components", exchange -> {
            int attempt = attemptCount.incrementAndGet();
            if (attempt < 2) {
                exchange.sendResponseHeaders(500, 0);
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

        Credentials credentials = new Credentials("http://localhost:" + port, "user", "pass", null);
        client = new NexusClient(credentials, 0);

        assertDoesNotThrow(() -> client.listComponents("test-repo"),
            "Should retry and succeed after transient 500 error");
    }

    @Test
    @DisplayName("Retry succeeds after transient 503 error")
    void testRetryAfterTransient503() throws IOException {
        AtomicInteger attemptCount = new AtomicInteger(0);

        server.createContext("/service/rest/v1/components", exchange -> {
            int attempt = attemptCount.incrementAndGet();
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

        Credentials credentials = new Credentials("http://localhost:" + port, "user", "pass", null);
        client = new NexusClient(credentials, 0);

        assertDoesNotThrow(() -> client.listComponents("test-repo"),
            "Should retry and succeed after transient 503 error");
    }
}

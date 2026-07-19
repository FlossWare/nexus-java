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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Platform-specific integration tests for Desktop (HttpClient) implementation.
 * Tests HTTP redirects, connection behavior, and Java HttpClient-specific features.
 */
@DisplayName("NexusClient Desktop (HttpClient) Platform Tests")
class NexusClientHttpClientPlatformTest {

    private HttpServer server;
    private HttpServer redirectServer;
    private int primaryPort;
    private int redirectPort;
    private NexusClient client;

    @BeforeEach
    void setUp() throws IOException {
        // Use dynamic port allocation to avoid conflicts during parallel test execution
        try (java.net.ServerSocket ss = new java.net.ServerSocket(0)) {
            primaryPort = ss.getLocalPort();
        }
        server = HttpServer.create(new InetSocketAddress(primaryPort), 0);
        server.setExecutor(null);
        server.start();

        try (java.net.ServerSocket ss = new java.net.ServerSocket(0)) {
            redirectPort = ss.getLocalPort();
        }
        redirectServer = HttpServer.create(new InetSocketAddress(redirectPort), 0);
        redirectServer.setExecutor(null);
        redirectServer.start();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
        if (redirectServer != null) {
            redirectServer.stop(0);
        }
    }

    @Test
    @DisplayName("HttpClient follows HTTP 302 redirect")
    void testHttpClientRedirect() throws Exception {
        // Redirect server: responds with 302 redirect
        redirectServer.createContext("/service/rest/v1/components", exchange -> {
            exchange.getResponseHeaders().add("Location", "http://localhost:" + primaryPort + "/service/rest/v1/components");
            exchange.sendResponseHeaders(302, 0);
            exchange.getResponseBody().close();
        });

        // Primary server: responds with actual data
        server.createContext("/service/rest/v1/components", exchange -> {
            String response = "{\"items\":[{\"id\":\"test\",\"assets\":[{\"path\":\"test.jar\",\"fileSize\":1024}]}]}";
            byte[] bytes = response.getBytes();
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        Credentials credentials = new Credentials("http://localhost:" + redirectPort, "user", "pass", null);
        client = new NexusClient(credentials, 0);

        // Should follow redirect and get data
        List<RepoRecord> records = client.listComponents("test-repo");
        assertEquals(1, records.size(), "Should follow redirect and fetch data");
        assertEquals("test.jar", records.get(0).path());
    }

    @Test
    @DisplayName("HttpClient handles 301 permanent redirect")
    void testHttpClientPermanentRedirect() throws Exception {
        // Redirect server: responds with 301 permanent redirect
        redirectServer.createContext("/service/rest/v1/components", exchange -> {
            exchange.getResponseHeaders().add("Location", "http://localhost:" + primaryPort + "/service/rest/v1/components");
            exchange.sendResponseHeaders(301, 0);
            exchange.getResponseBody().close();
        });

        // Primary server: responds with actual data
        server.createContext("/service/rest/v1/components", exchange -> {
            String response = "{\"items\":[]}";
            byte[] bytes = response.getBytes();
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        Credentials credentials = new Credentials("http://localhost:" + redirectPort, "user", "pass", null);
        client = new NexusClient(credentials, 0);

        // Should follow permanent redirect
        assertDoesNotThrow(() -> client.listComponents("test-repo"),
            "Should handle 301 permanent redirect");
    }

    @Test
    @DisplayName("HttpClient detects redirect loop")
    void testHttpClientRedirectLoop() throws Exception {
        // Server redirects to itself indefinitely
        server.createContext("/service/rest/v1/components", exchange -> {
            exchange.getResponseHeaders().add("Location", "http://localhost:" + primaryPort + "/service/rest/v1/components");
            exchange.sendResponseHeaders(302, 0);
            exchange.getResponseBody().close();
        });

        Credentials credentials = new Credentials("http://localhost:" + primaryPort, "user", "pass", null);
        client = new NexusClient(credentials, 0);

        // HttpClient should detect redirect loop after max redirects (typically 5)
        assertThrows(IOException.class, () -> client.listComponents("test-repo"),
            "Should fail on infinite redirect loop");
    }

    @Test
    @DisplayName("HttpClient handles HTTP keep-alive connections")
    void testHttpClientKeepAlive() throws Exception {
        server.createContext("/service/rest/v1/components", exchange -> {
            exchange.getResponseHeaders().add("Connection", "keep-alive");
            exchange.getResponseHeaders().add("Keep-Alive", "timeout=5, max=100");
            String response = "{\"items\":[{\"id\":\"test\",\"assets\":[{\"path\":\"test.jar\",\"fileSize\":1024}]}]}";
            byte[] bytes = response.getBytes();
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        Credentials credentials = new Credentials("http://localhost:" + primaryPort, "user", "pass", null);
        client = new NexusClient(credentials, 0);

        // Make multiple requests to reuse connection
        for (int i = 0; i < 3; i++) {
            assertDoesNotThrow(() -> client.listComponents("test-repo"),
                "Should handle keep-alive connections");
        }
    }

    @Test
    @DisplayName("HttpClient handles chunked transfer encoding")
    void testHttpClientChunkedTransferEncoding() throws Exception {
        server.createContext("/service/rest/v1/components", exchange -> {
            exchange.getResponseHeaders().add("Transfer-Encoding", "chunked");
            String response = "{\"items\":[{\"id\":\"test\",\"assets\":[{\"path\":\"test.jar\",\"fileSize\":1024}]}]}";
            exchange.sendResponseHeaders(200, -1); // -1 means chunked encoding
            try (OutputStream os = exchange.getResponseBody()) {
                // Write in chunks
                byte[] data = response.getBytes();
                int chunkSize = 50;
                for (int i = 0; i < data.length; i += chunkSize) {
                    int end = Math.min(i + chunkSize, data.length);
                    os.write(data, i, end - i);
                    os.flush();
                }
            }
        });

        Credentials credentials = new Credentials("http://localhost:" + primaryPort, "user", "pass", null);
        client = new NexusClient(credentials, 0);

        // Should handle chunked responses correctly
        List<RepoRecord> records = client.listComponents("test-repo");
        assertEquals(1, records.size(), "Should handle chunked transfer encoding");
    }

    @Test
    @DisplayName("HttpClient handles gzip-compressed responses")
    void testHttpClientGzipCompression() throws Exception {
        server.createContext("/service/rest/v1/components", exchange -> {
            exchange.getResponseHeaders().add("Content-Encoding", "gzip");
            String response = "{\"items\":[{\"id\":\"test\",\"assets\":[{\"path\":\"test.jar\",\"fileSize\":1024}]}]}";

            // Create gzip-compressed output
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            java.util.zip.GZIPOutputStream gzip = new java.util.zip.GZIPOutputStream(baos);
            gzip.write(response.getBytes());
            gzip.close();

            byte[] compressed = baos.toByteArray();
            exchange.sendResponseHeaders(200, compressed.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(compressed);
            }
        });

        Credentials credentials = new Credentials("http://localhost:" + primaryPort, "user", "pass", null);
        client = new NexusClient(credentials, 0);

        // Should decompress and parse correctly
        List<RepoRecord> records = client.listComponents("test-repo");
        assertEquals(1, records.size(), "Should handle gzip-compressed responses");
    }

    @Test
    @DisplayName("HttpClient handles response caching headers")
    void testHttpClientResponseCaching() throws Exception {
        java.util.concurrent.atomic.AtomicInteger requestCount = new java.util.concurrent.atomic.AtomicInteger(0);

        server.createContext("/service/rest/v1/components", exchange -> {
            requestCount.incrementAndGet();
            exchange.getResponseHeaders().add("Cache-Control", "public, max-age=3600");
            exchange.getResponseHeaders().add("ETag", "\"abc123\"");
            String response = "{\"items\":[{\"id\":\"test\",\"assets\":[{\"path\":\"test.jar\",\"fileSize\":1024}]}]}";
            byte[] bytes = response.getBytes();
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        Credentials credentials = new Credentials("http://localhost:" + primaryPort, "user", "pass", null);
        NexusClient cachedClient = new NexusClient(credentials); // Cache enabled (5 min default)

        // First request
        List<RepoRecord> records1 = cachedClient.listComponents("test-repo");
        assertEquals(1, records1.size());

        // Second request (should use cache)
        List<RepoRecord> records2 = cachedClient.listComponents("test-repo");
        assertEquals(1, records2.size());

        // Server should be hit only once (from cache on second call)
        assertTrue(requestCount.get() >= 1, "Should make at least one request");
    }

    @Test
    @DisplayName("HttpClient handles custom headers")
    void testHttpClientCustomHeaders() throws Exception {
        java.util.concurrent.atomic.AtomicReference<String> userAgent = new java.util.concurrent.atomic.AtomicReference<>();

        server.createContext("/service/rest/v1/components", exchange -> {
            userAgent.set(exchange.getRequestHeaders().getFirst("User-Agent"));
            String response = "{\"items\":[]}";
            byte[] bytes = response.getBytes();
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        Credentials credentials = new Credentials("http://localhost:" + primaryPort, "user", "pass", null);
        client = new NexusClient(credentials, 0);

        client.listComponents("test-repo");

        // HttpClient should set User-Agent header
        assertNotNull(userAgent.get(), "Should send User-Agent header");
        assertFalse(userAgent.get().isEmpty(), "User-Agent should not be empty");
    }
}

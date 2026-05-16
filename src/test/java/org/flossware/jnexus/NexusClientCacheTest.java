package org.flossware.nexus;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for NexusClient caching functionality.
 *
 * @author sfloess
 * @since 1.0
 */
class NexusClientCacheTest {

    private HttpServer server;
    private NexusClient client;
    private Path credentialsFile;
    private AtomicInteger requestCount;

    @BeforeEach
    void setUp() throws IOException {
        requestCount = new AtomicInteger(0);

        // Create mock HTTP server
        server = HttpServer.create(new InetSocketAddress(8889), 0);
        server.createContext("/service/rest/v1/components", new ComponentsHandler());
        server.setExecutor(null);
        server.start();

        // Create temporary credentials directory structure
        Path tempDir = Files.createTempDirectory("nexus-test");
        Path flosswareDir = tempDir.resolve(".flossware").resolve("nexus");
        Files.createDirectories(flosswareDir);
        credentialsFile = flosswareDir.resolve("nexus.properties");

        Files.writeString(credentialsFile, """
            nexus.url=http://localhost:8889
            nexus.user=testuser
            nexus.password=testpass
            """);

        System.setProperty("user.home", tempDir.toString());
        Credentials credentials = new Credentials();
        client = new NexusClient(credentials, 2); // 2 second TTL for testing
    }

    @AfterEach
    void tearDown() throws IOException {
        if (server != null) {
            server.stop(0);
        }
        if (credentialsFile != null) {
            // Clean up the entire temp directory structure
            Path tempDir = credentialsFile.getParent().getParent().getParent();
            Files.walk(tempDir)
                .sorted(Comparator.reverseOrder()) // Delete files before directories
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException e) {
                        // Ignore cleanup errors
                    }
                });
        }
    }

    @Test
    void testCacheMiss() throws IOException, InterruptedException {
        requestCount.set(0);

        List<RepoRecord> records = client.listComponents("test-repo");

        assertEquals(2, records.size());
        assertEquals(1, requestCount.get(), "Should make 1 HTTP request on cache miss");
    }

    @Test
    void testCacheHit() throws IOException, InterruptedException {
        requestCount.set(0);

        // First call - cache miss
        List<RepoRecord> records1 = client.listComponents("test-repo");
        assertEquals(1, requestCount.get());

        // Second call - cache hit
        List<RepoRecord> records2 = client.listComponents("test-repo");
        assertEquals(1, requestCount.get(), "Should not make another HTTP request on cache hit");

        assertEquals(records1.size(), records2.size());
    }

    @Test
    void testCacheExpiration() throws IOException, InterruptedException {
        requestCount.set(0);

        // First call
        client.listComponents("test-repo");
        assertEquals(1, requestCount.get());

        // Wait for cache to expire (TTL = 2 seconds)
        Thread.sleep(2500);

        // Second call after expiration
        client.listComponents("test-repo");
        assertEquals(2, requestCount.get(), "Should make new request after cache expiration");
    }

    @Test
    void testForceRefresh() throws IOException, InterruptedException {
        requestCount.set(0);

        // First call
        client.listComponents("test-repo");
        assertEquals(1, requestCount.get());

        // Force refresh - should bypass cache
        client.listComponents("test-repo", true);
        assertEquals(2, requestCount.get(), "Force refresh should bypass cache");
    }

    @Test
    void testCachePerRepository() throws IOException, InterruptedException {
        requestCount.set(0);

        // Call for repo1
        client.listComponents("repo1");
        assertEquals(1, requestCount.get());

        // Call for repo2 - different cache key
        client.listComponents("repo2");
        assertEquals(2, requestCount.get());

        // Call for repo1 again - cache hit
        client.listComponents("repo1");
        assertEquals(2, requestCount.get(), "Should use cache for repo1");

        // Call for repo2 again - cache hit
        client.listComponents("repo2");
        assertEquals(2, requestCount.get(), "Should use cache for repo2");
    }

    @Test
    void testClearCache() throws IOException, InterruptedException {
        requestCount.set(0);

        // First call
        client.listComponents("test-repo");
        assertEquals(1, requestCount.get());

        // Clear cache
        client.clearCache("test-repo");

        // Second call - should fetch again
        client.listComponents("test-repo");
        assertEquals(2, requestCount.get(), "Should make new request after cache clear");
    }

    @Test
    void testClearAllCache() throws IOException, InterruptedException {
        requestCount.set(0);

        // Call multiple repos
        client.listComponents("repo1");
        client.listComponents("repo2");
        assertEquals(2, requestCount.get());

        // Clear all cache
        client.clearAllCache();

        // Call again - should fetch both
        client.listComponents("repo1");
        client.listComponents("repo2");
        assertEquals(4, requestCount.get(), "Should make new requests after clearAllCache");
    }

    @Test
    void testIsCached() throws IOException, InterruptedException {
        assertFalse(client.isCached("test-repo"), "Should not be cached initially");

        client.listComponents("test-repo");
        assertTrue(client.isCached("test-repo"), "Should be cached after fetch");

        client.clearCache("test-repo");
        assertFalse(client.isCached("test-repo"), "Should not be cached after clear");
    }

    @Test
    void testGetCacheAge() throws IOException, InterruptedException {
        assertEquals(-1, client.getCacheAge("test-repo"), "Should return -1 for non-cached repo");

        client.listComponents("test-repo");
        long age1 = client.getCacheAge("test-repo");
        assertTrue(age1 >= 0 && age1 < 1, "Cache age should be less than 1 second immediately after fetch");

        Thread.sleep(1000);
        long age2 = client.getCacheAge("test-repo");
        assertTrue(age2 >= 1 && age2 < 2, "Cache age should be around 1 second");
    }

    @Test
    void testCacheDisabled() throws IOException, InterruptedException {
        requestCount.set(0);

        // Create client with cache disabled (TTL = 0)
        NexusClient noCacheClient = new NexusClient(new Credentials(), 0);

        // First call
        noCacheClient.listComponents("test-repo");
        assertEquals(1, requestCount.get());

        // Second call - should fetch again (no cache)
        noCacheClient.listComponents("test-repo");
        assertEquals(2, requestCount.get(), "Should make new request when cache disabled");

        assertFalse(noCacheClient.isCached("test-repo"), "Should not cache when TTL=0");
    }

    @Test
    void testCacheReturnsImmutableCopy() throws IOException, InterruptedException {
        List<RepoRecord> records1 = client.listComponents("test-repo");
        List<RepoRecord> records2 = client.listComponents("test-repo");

        // Should be different list instances (defensive copy)
        assertNotSame(records1, records2, "Cache should return a copy, not the same instance");
        assertEquals(records1, records2, "Contents should be equal");
    }

    /**
     * Mock HTTP handler that returns component JSON.
     */
    private class ComponentsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            requestCount.incrementAndGet();

            String response = """
                {
                  "items": [
                    {
                      "id": "id1",
                      "assets": [
                        {
                          "path": "com/example/artifact-1.0.0.jar",
                          "fileSize": 1000
                        }
                      ]
                    },
                    {
                      "id": "id2",
                      "assets": [
                        {
                          "path": "com/example/artifact-2.0.0.jar",
                          "fileSize": 2000
                        }
                      ]
                    }
                  ]
                }
                """;

            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }
}

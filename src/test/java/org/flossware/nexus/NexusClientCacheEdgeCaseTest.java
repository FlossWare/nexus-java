package org.flossware.nexus;

import com.sun.net.httpserver.HttpServer;
import org.flossware.jnexus.ComponentMetadata;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Edge case tests for NexusClient caching behavior.
 * <p>
 * Covers scenarios from issue #67:
 * - Concurrent cache access with 10 threads
 * - Cache invalidation during read
 * - Cache expiry boundary condition
 * - Cache with many repositories
 * - Metadata cache edge cases
 * - Cache and force refresh interaction
 * - Close clears cache
 * </p>
 */
@DisplayName("NexusClient Cache Edge Case Tests")
class NexusClientCacheEdgeCaseTest {

    private HttpServer server;
    private int port;
    private AtomicInteger requestCount;

    @BeforeEach
    void setUp() throws IOException {
        // Use dynamic port allocation to avoid conflicts during parallel test execution
        try (ServerSocket ss = new ServerSocket(0)) {
            port = ss.getLocalPort();
        }
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(null);
        requestCount = new AtomicInteger(0);

        server.createContext("/service/rest/v1/components", exchange -> {
            requestCount.incrementAndGet();
            String response = """
                {
                    "items": [{
                        "id": "cached-comp",
                        "format": "maven2",
                        "assets": [{
                            "path": "cached.jar",
                            "fileSize": 1000,
                            "contentType": "application/java-archive"
                        }]
                    }]
                }
                """;
            byte[] bytes = response.getBytes();
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        server.start();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    private NexusClient createClient(long cacheTtlSeconds) {
        Credentials credentials = new Credentials("http://localhost:" + port, "testuser", "testpass", null);
        return new NexusClient(credentials, cacheTtlSeconds);
    }

    @Test
    @DisplayName("10 threads reading same repository concurrently should be thread-safe")
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void testConcurrentCacheAccessWith10Threads() throws Exception {
        NexusClient client = createClient(300); // 5-minute cache

        // Populate cache first
        client.listComponents("test-repo");

        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        AtomicReference<Throwable> firstError = new AtomicReference<>();

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    startLatch.await(); // All threads start at the same time
                    List<RepoRecord> records = client.listComponents("test-repo");
                    assertNotNull(records);
                    assertEquals(1, records.size());
                    successCount.incrementAndGet();
                } catch (Throwable e) {
                    firstError.compareAndSet(null, e);
                    errorCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        // Start all threads simultaneously
        startLatch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);

        assertEquals(threadCount, successCount.get(),
            "All concurrent reads should succeed");
        assertEquals(0, errorCount.get(),
            "No errors should occur: " + (firstError.get() != null ? firstError.get().getMessage() : "none"));
        // Only 1 HTTP request should have been made (the initial one for cache population)
        assertEquals(1, requestCount.get(),
            "All threads should have read from cache (only 1 HTTP request)");
    }

    @Test
    @DisplayName("Cache invalidation during concurrent read should not cause race condition")
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void testCacheInvalidationDuringRead() throws Exception {
        NexusClient client = createClient(300);

        // Populate cache
        client.listComponents("test-repo");
        requestCount.set(0); // Reset counter

        int threadCount = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount + 1);
        AtomicInteger errorCount = new AtomicInteger(0);

        // Start reader threads
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < 100; j++) {
                        List<RepoRecord> records = client.listComponents("test-repo");
                        assertNotNull(records);
                    }
                } catch (Throwable e) {
                    errorCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        // Start invalidation thread
        new Thread(() -> {
            try {
                startLatch.await();
                for (int j = 0; j < 50; j++) {
                    client.clearCache("test-repo");
                    Thread.sleep(1); // Small delay
                }
            } catch (Throwable e) {
                errorCount.incrementAndGet();
            } finally {
                doneLatch.countDown();
            }
        }).start();

        startLatch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);

        assertEquals(0, errorCount.get(),
            "No errors should occur during concurrent read/invalidate");
    }

    @Test
    @DisplayName("Cache expiry boundary condition should refresh correctly")
    void testCacheExpiryBoundaryCondition() throws Exception {
        NexusClient client = createClient(1); // 1-second TTL

        // Populate cache
        client.listComponents("test-repo");
        assertEquals(1, requestCount.get(), "Initial request should be made");

        // Read immediately - should be cached
        client.listComponents("test-repo");
        assertEquals(1, requestCount.get(), "Should use cache (not expired yet)");

        // Wait just past the TTL
        Thread.sleep(1200);

        // Read after expiry - should fetch fresh
        client.listComponents("test-repo");
        assertEquals(2, requestCount.get(), "Should fetch fresh data after TTL expiry");
    }

    @Test
    @DisplayName("Cache with many different repositories should keep all cached")
    void testCacheWithManyRepositories() throws Exception {
        NexusClient client = createClient(300);

        // Cache 100 different repositories
        for (int i = 0; i < 100; i++) {
            client.listComponents("repo-" + i);
        }

        assertEquals(100, requestCount.get(), "Should make 100 HTTP requests");

        // All should be cached
        for (int i = 0; i < 100; i++) {
            assertTrue(client.isCached("repo-" + i),
                "Repository repo-" + i + " should be cached");
        }

        // Reading again should use cache
        requestCount.set(0);
        for (int i = 0; i < 100; i++) {
            client.listComponents("repo-" + i);
        }

        assertEquals(0, requestCount.get(), "All reads should use cache");
    }

    @Test
    @DisplayName("Metadata cache should be independent from component cache")
    void testMetadataCacheIndependence() throws Exception {
        NexusClient client = createClient(300);

        // Fetch components
        client.listComponents("test-repo");
        assertEquals(1, requestCount.get());

        // Fetch metadata - should be a separate request
        client.listComponentsWithMetadata("test-repo");
        assertEquals(2, requestCount.get(),
            "Metadata should be fetched separately from component list");

        // Both should be independently cached
        requestCount.set(0);
        client.listComponents("test-repo");
        client.listComponentsWithMetadata("test-repo");
        assertEquals(0, requestCount.get(),
            "Both component and metadata caches should be hit");
    }

    @Test
    @DisplayName("clearCache should clear both component and metadata caches for a repository")
    void testClearCacheBothTypes() throws Exception {
        NexusClient client = createClient(300);

        // Populate both caches
        client.listComponents("test-repo");
        client.listComponentsWithMetadata("test-repo");
        assertEquals(2, requestCount.get());

        // Clear cache for the repository
        client.clearCache("test-repo");

        // Both should be cleared
        requestCount.set(0);
        client.listComponents("test-repo");
        client.listComponentsWithMetadata("test-repo");
        assertEquals(2, requestCount.get(),
            "Both caches should have been cleared");
    }

    @Test
    @DisplayName("clearAllCache should clear caches for all repositories")
    void testClearAllCacheBothTypes() throws Exception {
        NexusClient client = createClient(300);

        // Populate caches for multiple repos
        client.listComponents("repo-1");
        client.listComponents("repo-2");
        client.listComponentsWithMetadata("repo-1");
        assertEquals(3, requestCount.get());

        // Clear all
        client.clearAllCache();

        assertFalse(client.isCached("repo-1"), "repo-1 should no longer be cached");
        assertFalse(client.isCached("repo-2"), "repo-2 should no longer be cached");
    }

    @Test
    @DisplayName("Force refresh should bypass cache even when valid")
    void testForceRefreshBypassesCache() throws Exception {
        NexusClient client = createClient(300);

        // Populate cache
        client.listComponents("test-repo");
        assertEquals(1, requestCount.get());

        // Force refresh - should fetch even though cached
        client.listComponents("test-repo", true);
        assertEquals(2, requestCount.get(),
            "Force refresh should bypass cache");

        // Cache should be updated after force refresh
        requestCount.set(0);
        client.listComponents("test-repo");
        assertEquals(0, requestCount.get(),
            "Cache should be updated after force refresh");
    }

    @Test
    @DisplayName("Cache disabled (TTL=0) should never cache")
    void testCacheDisabledNeverCaches() throws Exception {
        NexusClient client = createClient(0);

        client.listComponents("test-repo");
        client.listComponents("test-repo");
        client.listComponents("test-repo");

        assertEquals(3, requestCount.get(),
            "With TTL=0, every call should hit the server");
        assertFalse(client.isCached("test-repo"),
            "Should never report as cached with TTL=0");
        assertEquals(-1, client.getCacheAge("test-repo"),
            "Cache age should be -1 when not cached");
    }

    @Test
    @DisplayName("close() should clear all caches")
    void testCloseClarsCache() throws Exception {
        NexusClient client = createClient(300);

        // Populate cache
        client.listComponents("test-repo");
        assertTrue(client.isCached("test-repo"));

        // Close client
        client.close();

        // Cache should be cleared (isCached will access empty map)
        assertFalse(client.isCached("test-repo"),
            "Cache should be cleared after close()");
    }

    @Test
    @DisplayName("Cache returns defensive copies, not shared references")
    void testCacheReturnsCopies() throws Exception {
        NexusClient client = createClient(300);

        List<RepoRecord> records1 = client.listComponents("test-repo");
        List<RepoRecord> records2 = client.listComponents("test-repo");

        assertNotSame(records1, records2,
            "Cache should return different list instances");
        assertEquals(records1, records2,
            "But contents should be equal");
    }
}

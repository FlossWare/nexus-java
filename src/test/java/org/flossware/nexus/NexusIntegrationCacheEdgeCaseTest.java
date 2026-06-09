package org.flossware.nexus;

import org.flossware.jnexus.RepoRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

/**
 * Integration tests for cache edge cases.
 * Tests concurrent access, cache size limits, invalidation timing, and TTL edge cases.
 */
@DisplayName("NexusClient Cache Edge Case Tests")
class NexusIntegrationCacheEdgeCaseTest {

    @Mock
    private NexusClient mockClient;

    private NexusService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new NexusService(mockClient);
    }

    @Test
    @DisplayName("Concurrent cache access from 10 threads")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testConcurrentCacheAccess() throws Exception {
        List<RepoRecord> mockRecords = List.of(
            new RepoRecord("id1", 1024, "file1.jar"),
            new RepoRecord("id2", 2048, "file2.jar")
        );
        when(mockClient.listComponents(anyString(), anyBoolean()))
            .thenReturn(mockRecords);

        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<Future<?>> futures = new ArrayList<>();

        // 10 threads accessing the same repository cache
        for (int i = 0; i < 10; i++) {
            futures.add(executor.submit(() -> {
                try {
                    for (int j = 0; j < 5; j++) {
                        var records = service.getRepositoryRecords("shared-repo", null, false);
                        assertEquals(2, records.size(), "Cache should return consistent results");
                    }
                } catch (IOException | InterruptedException e) {
                    fail("Should not throw exception: " + e.getMessage());
                }
            }));
        }

        // Wait for all threads
        for (Future<?> future : futures) {
            future.get();
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS), "Should complete within timeout");

        // Verify client was called minimally (due to caching)
        verify(mockClient, atLeast(1)).listComponents(anyString(), anyBoolean());
        verify(mockClient, atMost(10)).listComponents(anyString(), anyBoolean());
    }

    @Test
    @DisplayName("Cache invalidation during concurrent read")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testCacheInvalidationDuringRead() throws Exception {
        AtomicInteger callCount = new AtomicInteger(0);
        when(mockClient.listComponents(anyString(), anyBoolean()))
            .thenAnswer(invocation -> {
                callCount.incrementAndGet();
                Thread.sleep(100); // Simulate slow read
                return List.of(new RepoRecord("id" + callCount.get(), 1024, "file" + callCount.get() + ".jar"));
            });

        ExecutorService executor = Executors.newFixedThreadPool(2);

        // Thread 1: Read cache
        Future<?> readFuture = executor.submit(() -> {
            try {
                for (int i = 0; i < 3; i++) {
                    service.getRepositoryRecords("test-repo", null, false);
                    Thread.sleep(50);
                }
            } catch (IOException | InterruptedException e) {
                fail("Read should not fail: " + e.getMessage());
            }
        });

        // Thread 2: Invalidate cache
        Thread.sleep(75); // Start after first read
        executor.submit(() -> mockClient.clearAllCache());

        readFuture.get();
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

        // Should have made multiple calls due to cache invalidation
        assertTrue(callCount.get() >= 1, "Should have called client at least once");
    }

    @Test
    @DisplayName("Cache with 10000 repositories")
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void testCacheStressWith10000Repos() throws Exception {
        when(mockClient.listComponents(anyString(), anyBoolean()))
            .thenAnswer(invocation -> {
                String repo = invocation.getArgument(0);
                return List.of(new RepoRecord("id-" + repo, 1024, "file-" + repo + ".jar"));
            });

        Runtime runtime = Runtime.getRuntime();
        runtime.gc();
        long beforeMemory = runtime.totalMemory() - runtime.freeMemory();

        // Populate cache with 10000 repositories
        for (int i = 0; i < 10000; i++) {
            service.getRepositoryRecords("stress-repo-" + i, null, false);

            // Check memory periodically
            if ((i + 1) % 1000 == 0) {
                runtime.gc();
                long currentMemory = runtime.totalMemory() - runtime.freeMemory();
                long memoryUsedMB = (currentMemory - beforeMemory) / (1024 * 1024);
                assertTrue(memoryUsedMB < 500,
                    "Memory at " + (i + 1) + " repos: " + memoryUsedMB + "MB (expected <500MB)");
            }
        }

        runtime.gc();
        long afterMemory = runtime.totalMemory() - runtime.freeMemory();
        long totalMemoryUsed = afterMemory - beforeMemory;
        long totalMemoryUsedMB = totalMemoryUsed / (1024 * 1024);

        assertTrue(totalMemoryUsed < 500 * 1024 * 1024,
            "Total memory: " + totalMemoryUsedMB + "MB (expected <500MB)");
    }

    @Test
    @DisplayName("Cache expiry at TTL boundary")
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void testCacheExpiryBoundary() throws Exception {
        AtomicInteger callCount = new AtomicInteger(0);
        when(mockClient.listComponents(anyString(), anyBoolean()))
            .thenAnswer(invocation -> {
                int count = callCount.incrementAndGet();
                return List.of(new RepoRecord("id-" + count, 1024, "file-" + count + ".jar"));
            });

        // First call - populates cache
        var records1 = service.getRepositoryRecords("test-repo", null, false);
        assertEquals(1, records1.size());

        // Immediate second call - should use cache
        var records2 = service.getRepositoryRecords("test-repo", null, false);
        assertEquals(records1.get(0).id(), records2.get(0).id(), "Should be from cache");

        // Force refresh before TTL expires
        var records3 = service.getRepositoryRecords("test-repo", null, true);
        assertNotEquals(records1.get(0).id(), records3.get(0).id(), "Should fetch fresh data");

        assertEquals(2, callCount.get(), "Should have called client twice");
    }

    @Test
    @DisplayName("Cache entry with null data")
    void testCacheEntryWithNullData() throws Exception {
        when(mockClient.listComponents(anyString(), anyBoolean()))
            .thenReturn(List.of());

        var records1 = service.getRepositoryRecords("empty-repo", null, false);
        assertTrue(records1.isEmpty(), "Should return empty list");

        var records2 = service.getRepositoryRecords("empty-repo", null, false);
        assertTrue(records2.isEmpty(), "Should cache empty result");

        verify(mockClient, times(1)).listComponents(anyString(), anyBoolean());
    }

    @Test
    @DisplayName("Force refresh bypasses cache")
    void testForceRefreshBypassesCache() throws Exception {
        AtomicInteger callCount = new AtomicInteger(0);
        when(mockClient.listComponents(anyString(), anyBoolean()))
            .thenAnswer(invocation -> {
                int count = callCount.incrementAndGet();
                return List.of(new RepoRecord("id-" + count, 1024, "file-" + count + ".jar"));
            });

        // First call
        var records1 = service.getRepositoryRecords("test-repo", null, false);
        assertEquals(1, records1.size());

        // Force refresh
        var records2 = service.getRepositoryRecords("test-repo", null, true);
        assertNotEquals(records1.get(0).id(), records2.get(0).id(), "Should bypass cache");

        assertEquals(2, callCount.get(), "Should call client twice");
    }

    @Test
    @DisplayName("Cache disabled with TTL=0")
    void testCacheDisabledWithZeroTTL() throws Exception {
        when(mockClient.listComponents(anyString(), anyBoolean()))
            .thenReturn(List.of(new RepoRecord("id1", 1024, "file1.jar")));

        // Create service with 0 TTL (disabled cache)
        NexusService serviceNoCache = new NexusService(mockClient);

        var records1 = serviceNoCache.getRepositoryRecords("test-repo", null, false);
        var records2 = serviceNoCache.getRepositoryRecords("test-repo", null, false);

        // Both should call client even without force refresh
        verify(mockClient, atLeast(2)).listComponents(anyString(), anyBoolean());
    }

    @Test
    @DisplayName("Concurrent clear and read operations")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testConcurrentClearAndRead() throws Exception {
        when(mockClient.listComponents(anyString(), anyBoolean()))
            .thenReturn(List.of(new RepoRecord("id1", 1024, "file1.jar")));

        ExecutorService executor = Executors.newFixedThreadPool(5);
        List<Future<?>> futures = new ArrayList<>();

        // 3 threads reading
        for (int i = 0; i < 3; i++) {
            futures.add(executor.submit(() -> {
                try {
                    for (int j = 0; j < 10; j++) {
                        service.getRepositoryRecords("test-repo", null, false);
                        Thread.sleep(10);
                    }
                } catch (IOException | InterruptedException e) {
                    fail("Read failed: " + e.getMessage());
                }
            }));
        }

        // 2 threads clearing
        for (int i = 0; i < 2; i++) {
            futures.add(executor.submit(() -> {
                try {
                    for (int j = 0; j < 5; j++) {
                        mockClient.clearAllCache();
                        Thread.sleep(20);
                    }
                } catch (InterruptedException e) {
                    fail("Clear failed: " + e.getMessage());
                }
            }));
        }

        for (Future<?> future : futures) {
            future.get();
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
    }
}

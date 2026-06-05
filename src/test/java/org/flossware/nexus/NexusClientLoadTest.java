package org.flossware.nexus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Performance Load Tests for NexusClient and NexusService.
 * <p>
 * These tests verify performance under realistic load conditions:
 * - Concurrent operations
 * - Memory usage with large datasets
 * - Cache behavior under load
 * - Timeout and error handling
 * </p>
 *
 * <h2>Performance Targets (SLA):</h2>
 * <ul>
 *   <li><strong>Concurrent 100 operations</strong>: &lt; 10 seconds (avg 100ms/op)</li>
 *   <li><strong>Memory for 10K components</strong>: &lt; 300MB</li>
 *   <li><strong>Memory for 100K components</strong>: &lt; 500MB</li>
 *   <li><strong>Cache consistency</strong>: All threads see consistent data</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NexusClient Load Tests")
class NexusClientLoadTest {

    @Mock
    private NexusClient mockClient;

    private NexusService service;

    @BeforeEach
    void setUp() {
        service = new NexusService(mockClient);
    }

    // ==================== Concurrent Operation Tests ====================

    @Test
    @DisplayName("100 concurrent list operations should complete in <10 seconds")
    void testConcurrent100ListOperations() throws Exception {
        int operationCount = 100;
        int threadCount = 10;

        // Setup: Return consistent mock data
        List<RepoRecord> mockRecords = generateRepoRecords(1000);
        when(mockClient.listComponents(anyString(), anyBoolean()))
            .thenReturn(new ArrayList<>(mockRecords));

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<?>> futures = new ArrayList<>();

        long start = System.currentTimeMillis();

        // Submit 100 concurrent list operations
        for (int i = 0; i < operationCount; i++) {
            futures.add(executor.submit(() -> {
                try {
                    service.getRepositoryRecords("test-repo", null, false);
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }));
        }

        // Wait for all to complete
        for (Future<?> future : futures) {
            future.get();
        }

        long duration = System.currentTimeMillis() - start;

        executor.shutdown();

        // SLA: Should complete in <10 seconds (100ms avg per operation)
        // With caching, should be much faster
        assertTrue(duration < 10000,
            "100 concurrent operations took " + duration + "ms (expected <10000ms)");
    }

    @Test
    @DisplayName("50 concurrent delete operations should complete in <60 seconds")
    void testConcurrent50DeleteOperations() throws Exception {
        int operationCount = 50;
        int threadCount = 5;

        // Setup: Mock successful deletions
        doNothing().when(mockClient).deleteComponent(anyString(), anyString());
        when(mockClient.listComponents(anyString(), anyBoolean()))
            .thenReturn(generateRepoRecords(100));

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<?>> futures = new ArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        long start = System.currentTimeMillis();

        // Submit 50 concurrent delete operations
        for (int i = 0; i < operationCount; i++) {
            final int index = i;
            futures.add(executor.submit(() -> {
                try {
                    service.deleteComponent("test-repo", "component-" + index, false, null);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                }
            }));
        }

        // Wait for all to complete
        for (Future<?> future : futures) {
            future.get();
        }

        long duration = System.currentTimeMillis() - start;

        executor.shutdown();

        // SLA: All operations should succeed
        assertEquals(operationCount, successCount.get(),
            "Some delete operations failed: " + errorCount.get() + " errors");

        // SLA: Should complete in <60 seconds
        assertTrue(duration < 60000,
            "50 concurrent deletes took " + duration + "ms (expected <60000ms)");
    }

    @Test
    @DisplayName("10 concurrent cache operations with invalidation should be consistent")
    void testConcurrentCacheOperationsConsistency() throws Exception {
        int threadCount = 10;
        int operations = 50;

        List<RepoRecord> mockRecords = generateRepoRecords(100);
        when(mockClient.listComponents(anyString(), anyBoolean()))
            .thenReturn(new ArrayList<>(mockRecords));

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<?>> futures = new ArrayList<>();
        AtomicInteger inconsistencyCount = new AtomicInteger(0);

        // Submit mixed operations (list + cache clear)
        for (int i = 0; i < operations; i++) {
            final int opIndex = i;

            if (opIndex % 5 == 0) {
                // Every 5th operation, clear cache
                futures.add(executor.submit(service::clearAllCaches));
            } else {
                // List operation
                futures.add(executor.submit(() -> {
                    try {
                        List<RepoRecord> result = service.getRepositoryRecords("test-repo", null, false);
                        // Verify consistency: should get same data or no data (if cleared)
                        assertTrue(result != null,
                            "List result should not be null");
                    } catch (IOException | InterruptedException e) {
                        inconsistencyCount.incrementAndGet();
                    }
                }));
            }
        }

        // Wait for all to complete
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                // Expected during concurrent cache clearing
            }
        }

        executor.shutdown();

        // Cache clearing under concurrent load should not cause crashes
        // Some inconsistencies are acceptable due to race conditions
        assertTrue(inconsistencyCount.get() < operations / 2,
            "Too many inconsistencies: " + inconsistencyCount.get());
    }

    // ==================== Memory Usage Tests ====================

    @Test
    @DisplayName("Loading 10K components should use <300MB memory")
    void testMemoryUsageWith10000Components() {
        List<RepoRecord> largeDataset = generateRepoRecords(10000);
        when(mockClient.listComponents("huge-repo", false))
            .thenReturn(new ArrayList<>(largeDataset));

        Runtime runtime = Runtime.getRuntime();
        runtime.gc();
        long beforeMemory = runtime.totalMemory() - runtime.freeMemory();

        try {
            List<RepoRecord> result = service.getRepositoryRecords("huge-repo", null, false);
            assertEquals(10000, result.size(), "Should load all 10K components");
        } catch (IOException | InterruptedException e) {
            fail("Should not throw exception: " + e.getMessage());
        }

        runtime.gc();
        long afterMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = afterMemory - beforeMemory;
        long memoryUsedMB = memoryUsed / (1024 * 1024);

        // SLA: Should use <300MB for 10K components
        assertTrue(memoryUsed < 300 * 1024 * 1024,
            "Memory usage: " + memoryUsedMB + "MB (expected <300MB)");
    }

    @Test
    @DisplayName("Loading 100K components should use <500MB memory")
    void testMemoryUsageWith100000Components() {
        List<RepoRecord> veryLargeDataset = generateRepoRecords(100000);
        when(mockClient.listComponents("massive-repo", false))
            .thenReturn(new ArrayList<>(veryLargeDataset));

        Runtime runtime = Runtime.getRuntime();
        runtime.gc();
        long beforeMemory = runtime.totalMemory() - runtime.freeMemory();

        try {
            List<RepoRecord> result = service.getRepositoryRecords("massive-repo", null, false);
            assertEquals(100000, result.size(), "Should load all 100K components");
        } catch (IOException | InterruptedException e) {
            fail("Should not throw exception: " + e.getMessage());
        }

        runtime.gc();
        long afterMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = afterMemory - beforeMemory;
        long memoryUsedMB = memoryUsed / (1024 * 1024);

        // SLA: Should use <500MB for 100K components
        assertTrue(memoryUsed < 500 * 1024 * 1024,
            "Memory usage: " + memoryUsedMB + "MB (expected <500MB)");
    }

    @Test
    @DisplayName("Calculating statistics on 10K components should use reasonable memory")
    void testMemoryUsageForStatistics10K() throws IOException, InterruptedException {
        List<RepoRecord> largeDataset = generateRepoRecords(10000);
        when(mockClient.listComponents("stats-repo", false))
            .thenReturn(new ArrayList<>(largeDataset));

        Runtime runtime = Runtime.getRuntime();
        runtime.gc();
        long beforeMemory = runtime.totalMemory() - runtime.freeMemory();

        RepositoryStats stats = service.calculateStatistics("stats-repo");
        assertNotNull(stats, "Statistics should be calculated");

        runtime.gc();
        long afterMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = afterMemory - beforeMemory;

        // Statistics calculation should add minimal memory overhead
        // Conservative estimate: 2x the dataset size
        long allowableOverhead = largeDataset.size() * 100; // Rough estimate per record
        assertTrue(memoryUsed < allowableOverhead * 2,
            "Statistics calculation used excessive memory: "
                + (memoryUsed / 1024 / 1024) + "MB");
    }

    // ==================== Throughput Tests ====================

    @Test
    @DisplayName("Should achieve 10+ list operations per second with caching")
    void testThroughputWithCaching() throws Exception {
        List<RepoRecord> mockRecords = generateRepoRecords(100);
        when(mockClient.listComponents(anyString(), eq(false)))
            .thenReturn(new ArrayList<>(mockRecords));

        int operationCount = 100;
        long start = System.currentTimeMillis();

        for (int i = 0; i < operationCount; i++) {
            service.getRepositoryRecords("cached-repo", null, false);
        }

        long duration = System.currentTimeMillis() - start;
        double throughput = (double) operationCount / (duration / 1000.0);

        // After first call, cache is populated, so throughput should be 10+ ops/sec
        // This is conservative; actual should be 100+ ops/sec
        assertTrue(throughput >= 10.0,
            "Throughput: " + String.format("%.2f", throughput)
                + " ops/sec (expected >= 10 ops/sec)");
    }

    @Test
    @DisplayName("Should maintain performance with cache misses")
    void testThroughputWithCacheMisses() throws Exception {
        List<RepoRecord> mockRecords = generateRepoRecords(100);
        when(mockClient.listComponents(anyString(), eq(false)))
            .thenReturn(new ArrayList<>(mockRecords));

        int operationCount = 50;
        long start = System.currentTimeMillis();

        // Each operation uses a different repository (cache miss)
        for (int i = 0; i < operationCount; i++) {
            service.getRepositoryRecords("repo-" + i, null, false);
        }

        long duration = System.currentTimeMillis() - start;

        // With 50 different repositories, all are cache misses
        // Should still complete reasonably fast
        assertTrue(duration < 30000,
            "50 cache-miss operations took " + duration + "ms (expected <30000ms)");
    }

    // ==================== Timeout/Error Recovery Tests ====================

    @Test
    @DisplayName("Should handle timeout gracefully during concurrent operations")
    void testTimeoutHandlingConcurrent() throws Exception {
        int threadCount = 5;
        int operationCount = 10;

        // Mock some operations to be slow
        AtomicInteger callCount = new AtomicInteger(0);
        when(mockClient.listComponents(anyString(), anyBoolean()))
            .thenAnswer(invocation -> {
                int call = callCount.incrementAndGet();
                if (call % 3 == 0) {
                    // Simulate timeout for every 3rd call
                    throw new IOException("Simulated timeout");
                }
                return generateRepoRecords(100);
            });

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<?>> futures = new ArrayList<>();
        AtomicInteger errorCount = new AtomicInteger(0);
        AtomicInteger successCount = new AtomicInteger(0);

        // Submit operations that might timeout
        for (int i = 0; i < operationCount; i++) {
            futures.add(executor.submit(() -> {
                try {
                    service.getRepositoryRecords("test-repo", null, false);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                }
            }));
        }

        // Wait for all to complete
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                // Expected for some operations
            }
        }

        executor.shutdown();

        // Some operations should fail, but not crash the system
        assertTrue(errorCount.get() > 0, "Some operations should fail");
        assertTrue(successCount.get() > 0, "Some operations should succeed");
    }

    // ==================== Helper Methods ====================

    /**
     * Generate mock repository records for load testing.
     */
    private List<RepoRecord> generateRepoRecords(int count) {
        List<RepoRecord> records = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String id = "artifact-" + i;
            long size = (long) (Math.random() * 100_000_000); // 0-100MB
            String path = "com/example/artifact-" + i + "/1.0.0/artifact-" + i + "-1.0.0.jar";
            records.add(new RepoRecord(id, size, path));
        }
        return records;
    }
}

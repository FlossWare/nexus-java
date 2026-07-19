package org.flossware.nexus;

import org.flossware.jnexus.ComponentMetadata;
import org.flossware.jnexus.NexusHttpClient;
import org.flossware.jnexus.RepoRecord;
import org.flossware.jnexus.RepositoryStats;
import org.flossware.jnexus.SearchCriteria;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * JMH Benchmarks for NexusClient and NexusService operations.
 * <p>
 * These benchmarks measure performance characteristics of core operations:
 * - List operations (cached vs uncached)
 * - Filtering and search operations
 * - Statistics calculations
 * - Cache overhead and invalidation
 * </p>
 *
 * <p>Run with: mvn test -Pbenchmark</p>
 *
 * <h2>Performance Targets (SLA):</h2>
 * <ul>
 *   <li><strong>List Cached (100)</strong>: p50 &lt; 50ms, p95 &lt; 100ms, p99 &lt; 200ms</li>
 *   <li><strong>List Cached (1000)</strong>: p50 &lt; 100ms, p95 &lt; 250ms, p99 &lt; 500ms</li>
 *   <li><strong>Statistics (1000)</strong>: p50 &lt; 500ms, p95 &lt; 1000ms</li>
 *   <li><strong>Cache Hit Overhead</strong>: &lt; 1ms</li>
 * </ul>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Benchmark)
public class NexusClientBenchmark {

    private NexusService service;
    private MockNexusClient mockClient;
    private List<RepoRecord> smallDataset;
    private List<RepoRecord> mediumDataset;
    private List<RepoRecord> largeDataset;
    private List<ComponentMetadata> smallMetadataDataset;
    private List<ComponentMetadata> mediumMetadataDataset;
    private List<ComponentMetadata> largeMetadataDataset;

    @Setup
    public void setup() throws IOException, InterruptedException {
        // Create a mock NexusHttpClient with deterministic test data
        mockClient = new MockNexusClient();
        service = new NexusService(mockClient);

        // Generate test datasets
        smallDataset = generateRepoRecords(100);
        mediumDataset = generateRepoRecords(1000);
        largeDataset = generateRepoRecords(10000);
        smallMetadataDataset = generateComponentMetadata(100);
        mediumMetadataDataset = generateComponentMetadata(1000);
        largeMetadataDataset = generateComponentMetadata(10000);
    }

    @TearDown
    public void tearDown() {
        if (service != null) {
            service.clearAllCache();
        }
    }

    // ==================== List Operation Benchmarks ====================

    /**
     * Benchmark: List 100 components (cached).
     * SLA: p50 < 50ms
     */
    @Benchmark
    public void listComponents_100_cached(Blackhole bh) throws IOException, InterruptedException {
        List<RepoRecord> result = service.getRepositoryRecords("test-repo-100", null, false);
        bh.consume(result);
    }

    /**
     * Benchmark: List 1000 components (cached).
     * SLA: p50 < 100ms
     */
    @Benchmark
    public void listComponents_1000_cached(Blackhole bh) throws IOException, InterruptedException {
        List<RepoRecord> result = service.getRepositoryRecords("test-repo-1000", null, false);
        bh.consume(result);
    }

    /**
     * Benchmark: List 10000 components (cached).
     * SLA: p50 < 500ms
     */
    @Benchmark
    public void listComponents_10000_cached(Blackhole bh) throws IOException, InterruptedException {
        List<RepoRecord> result = service.getRepositoryRecords("test-repo-10000", null, false);
        bh.consume(result);
    }

    /**
     * Benchmark: Cache hit overhead for repeated calls.
     * SLA: < 1ms overhead
     */
    @Benchmark
    public void cacheHit_overhead(Blackhole bh) throws IOException, InterruptedException {
        // Pre-populate cache
        service.getRepositoryRecords("cache-test", null, false);

        // Measure cache hit
        long start = System.nanoTime();
        List<RepoRecord> result = service.getRepositoryRecords("cache-test", null, false);
        long duration = System.nanoTime() - start;

        bh.consume(result);
        bh.consume(duration);
    }

    // ==================== Filtering & Search Benchmarks ====================

    /**
     * Benchmark: Simple regex filtering on 1000 components.
     * Tests: service.searchComponents() with regex filter
     */
    @Benchmark
    public void search_regexFilter_1000(Blackhole bh) throws IOException, InterruptedException {
        SearchCriteria criteria = new SearchCriteria.Builder()
            .repository("test-repo-1000")
            .regexFilter(".*\\.jar$")
            .build();

        List<ComponentMetadata> result = service.searchComponents(criteria, false);
        bh.consume(result);
    }

    /**
     * Benchmark: Complex filtering (size + date + extension) on 1000 components.
     * Tests: Multiple filter conditions combined
     */
    @Benchmark
    public void search_complexFilter_1000(Blackhole bh) throws IOException, InterruptedException {
        Instant twoMonthsAgo = Instant.now().minus(java.time.Duration.ofDays(60));

        SearchCriteria criteria = new SearchCriteria.Builder()
            .repository("test-repo-1000")
            .minSize(100_000L)
            .maxSize(10_000_000L)
            .createdAfter(twoMonthsAgo)
            .fileExtension(".jar")
            .build();

        List<ComponentMetadata> result = service.searchComponents(criteria, false);
        bh.consume(result);
    }

    /**
     * Benchmark: File extension filtering on 1000 components.
     * Tests: Extension-based filtering performance
     */
    @Benchmark
    public void search_extensionFilter_1000(Blackhole bh) throws IOException, InterruptedException {
        SearchCriteria criteria = new SearchCriteria.Builder()
            .repository("test-repo-1000")
            .fileExtension(".war")
            .build();

        List<ComponentMetadata> result = service.searchComponents(criteria, false);
        bh.consume(result);
    }

    // ==================== Statistics Benchmarks ====================

    /**
     * Benchmark: Calculate statistics on 100 components.
     * SLA: < 100ms
     */
    @Benchmark
    public void statistics_100(Blackhole bh) throws IOException, InterruptedException {
        List<ComponentMetadata> components = mockClient.listComponentsWithMetadata("test-repo-100", false);
        RepositoryStats stats = service.calculateStatistics("test-repo-100", components);
        bh.consume(stats);
    }

    /**
     * Benchmark: Calculate statistics on 1000 components.
     * SLA: < 500ms
     */
    @Benchmark
    public void statistics_1000(Blackhole bh) throws IOException, InterruptedException {
        List<ComponentMetadata> components = mockClient.listComponentsWithMetadata("test-repo-1000", false);
        RepositoryStats stats = service.calculateStatistics("test-repo-1000", components);
        bh.consume(stats);
    }

    /**
     * Benchmark: Calculate statistics on 10000 components.
     * SLA: < 2000ms (2 seconds)
     */
    @Benchmark
    public void statistics_10000(Blackhole bh) throws IOException, InterruptedException {
        List<ComponentMetadata> components = mockClient.listComponentsWithMetadata("test-repo-10000", false);
        RepositoryStats stats = service.calculateStatistics("test-repo-10000", components);
        bh.consume(stats);
    }

    // ==================== Cache Operations Benchmarks ====================

    /**
     * Benchmark: Cache invalidation overhead.
     * SLA: < 10ms
     */
    @Benchmark
    public void cacheInvalidation_overhead(Blackhole bh) {
        service.clearAllCache();
        bh.consume(true);
    }

    /**
     * Benchmark: Cache check time (before population).
     * SLA: < 0.1ms
     */
    @Benchmark
    public void cacheCheck_beforePopulation(Blackhole bh) throws IOException, InterruptedException {
        // This measures the time to check cache for non-existent entry
        service.getRepositoryRecords("non-existent-" + System.nanoTime(), null, false);
        bh.consume(true);
    }

    // ==================== Helper Methods ====================

    /**
     * Generate mock repository records for benchmarking.
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

    /**
     * Generate mock component metadata for benchmarking.
     */
    private List<ComponentMetadata> generateComponentMetadata(int count) {
        List<ComponentMetadata> metadata = new ArrayList<>(count);
        Instant baseTime = Instant.parse("2024-01-01T00:00:00Z");

        for (int i = 0; i < count; i++) {
            String id = "artifact-" + i;
            long size = (long) (Math.random() * 100_000_000);
            String path = "com/example/artifact-" + i + "/1.0.0/artifact-" + i + "-1.0.0.jar";
            String contentType = i % 2 == 0 ? "application/java-archive" : "application/octet-stream";
            String format = "maven2";
            Instant created = baseTime.plusSeconds(i * 3600);
            Instant modified = created.plusSeconds(86400);
            String checksum = "sha256-" + i;

            metadata.add(new ComponentMetadata(
                id, path, size, contentType, format, created, modified, checksum
            ));
        }
        return metadata;
    }

    /**
     * Mock NexusHttpClient implementation for deterministic benchmarking.
     * <p>
     * This mock implementation provides deterministic data for benchmarking
     * without making actual HTTP calls or involving real cache behavior.
     * </p>
     */
    private class MockNexusClient implements NexusHttpClient {

        @Override
        public List<RepoRecord> listComponents(String repository, boolean forceRefresh)
                throws IOException, InterruptedException {
            // Return appropriate dataset based on repository name
            if ("test-repo-100".equals(repository)) {
                return new ArrayList<>(smallDataset);
            } else if ("test-repo-1000".equals(repository)) {
                return new ArrayList<>(mediumDataset);
            } else if ("test-repo-10000".equals(repository)) {
                return new ArrayList<>(largeDataset);
            } else if ("cache-test".equals(repository)) {
                return new ArrayList<>(smallDataset);
            }
            return new ArrayList<>();
        }

        @Override
        public List<ComponentMetadata> listComponentsWithMetadata(
                String repository, boolean forceRefresh)
                throws IOException, InterruptedException {
            // Return metadata dataset based on repository name
            if ("test-repo-100".equals(repository)) {
                return new ArrayList<>(smallMetadataDataset);
            } else if ("test-repo-1000".equals(repository)) {
                return new ArrayList<>(mediumMetadataDataset);
            } else if ("test-repo-10000".equals(repository)) {
                return new ArrayList<>(largeMetadataDataset);
            }
            return new ArrayList<>();
        }

        @Override
        public void deleteComponent(String componentId)
                throws IOException, InterruptedException {
            // No-op for benchmarking
        }

        @Override
        public void clearCache(String repository) {
            // No-op for benchmarking
        }

        @Override
        public void clearAllCache() {
            // No-op for benchmarking
        }

        @Override
        public boolean isCached(String repository) {
            // Always return false for benchmarking
            return false;
        }

        @Override
        public long getCacheAge(String repository) {
            // Return -1 for benchmarking (not cached)
            return -1;
        }
    }
}

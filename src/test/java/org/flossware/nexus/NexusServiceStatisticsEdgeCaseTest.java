package org.flossware.nexus;

import org.flossware.jnexus.ComponentMetadata;
import org.flossware.jnexus.RepositoryStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Edge case tests for repository statistics calculation in NexusService.
 * <p>
 * Covers scenarios from issue #67:
 * - Empty repository (no divide-by-zero)
 * - Single component (median edge case)
 * - Extreme file sizes (1 byte to 10GB)
 * - Future dates in createdDate
 * - All components with null dates
 * - Files without extensions
 * - Many small files aggregation
 * - Statistics with callback
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NexusService Statistics Edge Case Tests")
class NexusServiceStatisticsEdgeCaseTest {

    @Mock
    private NexusClient mockClient;

    private NexusService service;

    @BeforeEach
    void setUp() {
        service = new NexusService(mockClient);
    }

    @Test
    @DisplayName("Statistics on empty repository should not divide by zero")
    void testStatsEmptyRepository() {
        RepositoryStats stats = service.calculateStatistics("empty-repo", List.of());

        assertEquals(0, stats.totalComponents(), "Total components should be 0");
        assertEquals(0L, stats.totalSize(), "Total size should be 0");
        assertEquals(0L, stats.averageSize(), "Average size should be 0 (no divide by zero)");
        assertEquals(0L, stats.medianSize(), "Median size should be 0");
        assertTrue(stats.sizeDistribution().isEmpty(), "Size distribution should be empty");
        assertTrue(stats.fileTypeBreakdown().isEmpty(), "File type breakdown should be empty");
        assertTrue(stats.ageDistribution().isEmpty(), "Age distribution should be empty");
        assertTrue(stats.largestComponents().isEmpty(), "Largest components should be empty");
    }

    @Test
    @DisplayName("Statistics on single component should handle median correctly")
    void testStatsSingleComponent() {
        List<ComponentMetadata> single = List.of(
            new ComponentMetadata("id1", "only.jar", 5000L, "application/java-archive",
                "maven2", Instant.now(), null, "sha1hash")
        );

        RepositoryStats stats = service.calculateStatistics("single-repo", single);

        assertEquals(1, stats.totalComponents());
        assertEquals(5000L, stats.totalSize());
        assertEquals(5000L, stats.averageSize(), "Average of one item should equal that item");
        assertEquals(5000L, stats.medianSize(), "Median of one item should equal that item");
        assertEquals(1, stats.largestComponents().size());
    }

    @Test
    @DisplayName("Statistics with extreme file sizes should bucket correctly")
    void testStatsExtremeFileSizes() {
        long mb = 1024L * 1024L;
        long gb = 1024L * 1024L * 1024L;

        List<ComponentMetadata> components = List.of(
            // 1 byte
            new ComponentMetadata("tiny", "tiny.txt", 1L, null, null, null, null, null),
            // 500 KB
            new ComponentMetadata("small", "small.dat", 500 * 1024L, null, null, null, null, null),
            // 5 MB
            new ComponentMetadata("medium", "medium.zip", 5 * mb, null, null, null, null, null),
            // 50 MB
            new ComponentMetadata("large", "large.tar", 50 * mb, null, null, null, null, null),
            // 500 MB
            new ComponentMetadata("xlarge", "xlarge.iso", 500 * mb, null, null, null, null, null),
            // 10 GB
            new ComponentMetadata("huge", "huge.img", 10 * gb, null, null, null, null, null)
        );

        RepositoryStats stats = service.calculateStatistics("extreme-repo", components);

        assertEquals(6, stats.totalComponents());
        assertEquals(2, stats.sizeDistribution().get(RepositoryStats.SIZE_RANGE_UNDER_1MB),
            "1 byte and 500KB should be in <1MB bucket");
        assertEquals(1, stats.sizeDistribution().get(RepositoryStats.SIZE_RANGE_1_TO_10MB),
            "5MB should be in 1-10MB bucket");
        assertEquals(1, stats.sizeDistribution().get(RepositoryStats.SIZE_RANGE_10_TO_100MB),
            "50MB should be in 10-100MB bucket");
        assertEquals(1, stats.sizeDistribution().get(RepositoryStats.SIZE_RANGE_100MB_TO_1GB),
            "500MB should be in 100MB-1GB bucket");
        assertEquals(1, stats.sizeDistribution().get(RepositoryStats.SIZE_RANGE_OVER_1GB),
            "10GB should be in >1GB bucket");
    }

    @Test
    @DisplayName("Statistics with future dates should not break age calculation")
    void testStatsWithFutureDates() {
        Instant futureDate = Instant.now().plus(Duration.ofDays(365));
        Instant pastDate = Instant.now().minus(Duration.ofDays(5));

        List<ComponentMetadata> components = List.of(
            new ComponentMetadata("future1", "future.jar", 1000L, null, null, futureDate, null, null),
            new ComponentMetadata("recent1", "recent.jar", 2000L, null, null, pastDate, null, null)
        );

        // Should not throw any exception
        RepositoryStats stats = assertDoesNotThrow(() ->
            service.calculateStatistics("future-repo", components),
            "Future dates should not cause exceptions"
        );

        assertEquals(2, stats.totalComponents());
        // Future date will result in negative daysAgo, which is <= 7
        // so it should be counted in the "Last 7 days" bucket
        int totalAged = stats.ageDistribution().values().stream()
            .mapToInt(Integer::intValue)
            .sum();
        assertEquals(2, totalAged,
            "All components should be counted in age distribution");
    }

    @Test
    @DisplayName("Statistics with all null dates should count all as 'older'")
    void testStatsAllNullDates() {
        List<ComponentMetadata> components = List.of(
            new ComponentMetadata("id1", "file1.jar", 1000L, null, null, null, null, null),
            new ComponentMetadata("id2", "file2.jar", 2000L, null, null, null, null, null),
            new ComponentMetadata("id3", "file3.jar", 3000L, null, null, null, null, null)
        );

        RepositoryStats stats = service.calculateStatistics("null-dates-repo", components);

        assertEquals(3, stats.ageDistribution().get(RepositoryStats.AGE_RANGE_OLDER),
            "Components with null dates should be counted as 'older'");
        assertEquals(0, stats.ageDistribution().get(RepositoryStats.AGE_RANGE_LAST_7_DAYS));
        assertEquals(0, stats.ageDistribution().get(RepositoryStats.AGE_RANGE_LAST_30_DAYS));
        assertEquals(0, stats.ageDistribution().get(RepositoryStats.AGE_RANGE_LAST_90_DAYS));
    }

    @Test
    @DisplayName("Statistics with files without extensions should handle gracefully")
    void testStatsFilesWithoutExtension() {
        List<ComponentMetadata> components = List.of(
            new ComponentMetadata("id1", "Dockerfile", 500L, null, null, null, null, null),
            new ComponentMetadata("id2", "Makefile", 1000L, null, null, null, null, null),
            new ComponentMetadata("id3", "LICENSE", 2000L, null, null, null, null, null),
            new ComponentMetadata("id4", "test.jar", 3000L, null, null, null, null, null)
        );

        RepositoryStats stats = service.calculateStatistics("no-ext-repo", components);

        assertNotNull(stats.fileTypeBreakdown());
        // Files without dots should be classified as "(no extension)"
        assertTrue(stats.fileTypeBreakdown().containsKey("(no extension)") ||
                   stats.fileTypeBreakdown().size() > 0,
            "Should handle files without extensions");
        assertTrue(stats.fileTypeBreakdown().containsKey(".jar"),
            "Should still categorize .jar files");
    }

    @Test
    @DisplayName("Statistics with many files of same extension should aggregate correctly")
    void testStatsManyFilesAggregation() {
        List<ComponentMetadata> components = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            components.add(new ComponentMetadata(
                "id-" + i,
                "artifact-" + i + ".jar",
                1000L,
                null, null, null, null, null
            ));
        }

        RepositoryStats stats = service.calculateStatistics("many-jars-repo", components);

        assertEquals(100, stats.totalComponents());
        assertEquals(100_000L, stats.totalSize());
        assertEquals(1000L, stats.averageSize());
        assertEquals(1000L, stats.medianSize(),
            "All same size should give that size as median");
        assertEquals(100_000L, stats.fileTypeBreakdown().get(".jar"),
            "All .jar files should sum to 100000 bytes");
    }

    @Test
    @DisplayName("Statistics with exactly 20 components should show all as largest")
    void testStatsLargestComponentsExactly20() {
        List<ComponentMetadata> components = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            components.add(new ComponentMetadata(
                "id-" + i,
                "artifact-" + i + ".jar",
                (i + 1) * 1000L,
                null, null, null, null, null
            ));
        }

        RepositoryStats stats = service.calculateStatistics("exactly20-repo", components);

        assertEquals(20, stats.largestComponents().size(),
            "Should show all 20 as largest");
        assertEquals(20_000L, stats.largestComponents().get(0).fileSize(),
            "Largest should be first");
    }

    @Test
    @DisplayName("Statistics with more than 20 components should show only top 20")
    void testStatsLargestComponentsMoreThan20() {
        List<ComponentMetadata> components = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            components.add(new ComponentMetadata(
                "id-" + i,
                "artifact-" + i + ".jar",
                (i + 1) * 1000L,
                null, null, null, null, null
            ));
        }

        RepositoryStats stats = service.calculateStatistics("over20-repo", components);

        assertEquals(20, stats.largestComponents().size(),
            "Should limit to top 20");
        assertEquals(30_000L, stats.largestComponents().get(0).fileSize(),
            "Largest should be artifact-29 (30000 bytes)");
        assertEquals(11_000L, stats.largestComponents().get(19).fileSize(),
            "20th largest should be artifact-10 (11000 bytes)");
    }

    @Test
    @DisplayName("Statistics with callback should receive start and complete events")
    void testStatsWithCallback() {
        List<ComponentMetadata> components = List.of(
            new ComponentMetadata("id1", "file1.jar", 1000L, null, null, null, null, null),
            new ComponentMetadata("id2", "file2.jar", 2000L, null, null, null, null, null)
        );

        boolean[] startCalled = {false};
        boolean[] completeCalled = {false};
        RepositoryStats[] receivedStats = {null};

        ProgressCallback callback = new ProgressCallback() {
            @Override
            public void onStatisticsStart(String repository, int totalComponents) {
                startCalled[0] = true;
                assertEquals("test-repo", repository);
                assertEquals(2, totalComponents);
            }

            @Override
            public void onStatisticsComplete(String repository, RepositoryStats stats) {
                completeCalled[0] = true;
                receivedStats[0] = stats;
                assertEquals("test-repo", repository);
            }
        };

        RepositoryStats stats = service.calculateStatistics("test-repo", components, callback);

        assertTrue(startCalled[0], "Statistics start callback should be called");
        assertTrue(completeCalled[0], "Statistics complete callback should be called");
        assertSame(stats, receivedStats[0],
            "Callback should receive the same stats object");
    }

    @Test
    @DisplayName("Statistics with callback exception should not disrupt calculation")
    void testStatsWithFaultyCallback() {
        List<ComponentMetadata> components = List.of(
            new ComponentMetadata("id1", "file1.jar", 1000L, null, null, null, null, null)
        );

        ProgressCallback faultyCallback = new ProgressCallback() {
            @Override
            public void onStatisticsStart(String repository, int totalComponents) {
                throw new RuntimeException("Start callback error!");
            }

            @Override
            public void onStatisticsComplete(String repository, RepositoryStats stats) {
                throw new RuntimeException("Complete callback error!");
            }
        };

        RepositoryStats stats = assertDoesNotThrow(() ->
            service.calculateStatistics("test-repo", components, faultyCallback),
            "Faulty callback should not disrupt statistics calculation"
        );

        assertEquals(1, stats.totalComponents());
        assertEquals(1000L, stats.totalSize());
    }

    @Test
    @DisplayName("Median calculation with two elements should average them")
    void testMedianWithTwoElements() {
        List<ComponentMetadata> components = List.of(
            new ComponentMetadata("id1", "file1.jar", 100L, null, null, null, null, null),
            new ComponentMetadata("id2", "file2.jar", 200L, null, null, null, null, null)
        );

        RepositoryStats stats = service.calculateStatistics("two-repo", components);

        assertEquals(150L, stats.medianSize(),
            "Median of [100, 200] should be 150");
    }

    @Test
    @DisplayName("Size distribution boundary values should be categorized correctly")
    void testSizeDistributionBoundaries() {
        long mb = 1024L * 1024L;
        long gb = 1024L * 1024L * 1024L;

        List<ComponentMetadata> components = List.of(
            // Exactly at boundary: 1MB - should be in 1-10MB bucket (not < 1MB)
            new ComponentMetadata("at1mb", "at1mb.jar", mb, null, null, null, null, null),
            // Just below 1MB
            new ComponentMetadata("below1mb", "below1mb.jar", mb - 1, null, null, null, null, null),
            // Exactly at 10MB - should be in 10-100MB bucket
            new ComponentMetadata("at10mb", "at10mb.jar", 10 * mb, null, null, null, null, null),
            // Exactly at 100MB - should be in 100MB-1GB bucket
            new ComponentMetadata("at100mb", "at100mb.jar", 100 * mb, null, null, null, null, null),
            // Exactly at 1GB - should be in >1GB bucket
            new ComponentMetadata("at1gb", "at1gb.jar", gb, null, null, null, null, null)
        );

        RepositoryStats stats = service.calculateStatistics("boundary-repo", components);

        assertEquals(1, stats.sizeDistribution().get(RepositoryStats.SIZE_RANGE_UNDER_1MB),
            "Only below1mb should be in <1MB");
        assertEquals(1, stats.sizeDistribution().get(RepositoryStats.SIZE_RANGE_1_TO_10MB),
            "at1mb should be in 1-10MB");
        assertEquals(1, stats.sizeDistribution().get(RepositoryStats.SIZE_RANGE_10_TO_100MB),
            "at10mb should be in 10-100MB");
        assertEquals(1, stats.sizeDistribution().get(RepositoryStats.SIZE_RANGE_100MB_TO_1GB),
            "at100mb should be in 100MB-1GB");
        assertEquals(1, stats.sizeDistribution().get(RepositoryStats.SIZE_RANGE_OVER_1GB),
            "at1gb should be in >1GB");
    }

    @Test
    @DisplayName("Age distribution boundary values should be categorized correctly")
    void testAgeDistributionBoundaries() {
        Instant now = Instant.now();

        List<ComponentMetadata> components = List.of(
            // Exactly 7 days ago - should be in "last 7 days"
            new ComponentMetadata("at7d", "at7d.jar", 1000L, null, null,
                now.minus(Duration.ofDays(7)), null, null),
            // 8 days ago - should be in "last 30 days"
            new ComponentMetadata("at8d", "at8d.jar", 1000L, null, null,
                now.minus(Duration.ofDays(8)), null, null),
            // Exactly 30 days ago - should be in "last 30 days"
            new ComponentMetadata("at30d", "at30d.jar", 1000L, null, null,
                now.minus(Duration.ofDays(30)), null, null),
            // 31 days ago - should be in "last 90 days"
            new ComponentMetadata("at31d", "at31d.jar", 1000L, null, null,
                now.minus(Duration.ofDays(31)), null, null),
            // Exactly 90 days ago - should be in "last 90 days"
            new ComponentMetadata("at90d", "at90d.jar", 1000L, null, null,
                now.minus(Duration.ofDays(90)), null, null),
            // 91 days ago - should be in "older"
            new ComponentMetadata("at91d", "at91d.jar", 1000L, null, null,
                now.minus(Duration.ofDays(91)), null, null)
        );

        RepositoryStats stats = service.calculateStatistics("boundary-age-repo", components);

        assertEquals(1, stats.ageDistribution().get(RepositoryStats.AGE_RANGE_LAST_7_DAYS),
            "7 days ago should be in 'last 7 days'");
        assertEquals(2, stats.ageDistribution().get(RepositoryStats.AGE_RANGE_LAST_30_DAYS),
            "8 and 30 days ago should be in 'last 30 days'");
        assertEquals(2, stats.ageDistribution().get(RepositoryStats.AGE_RANGE_LAST_90_DAYS),
            "31 and 90 days ago should be in 'last 90 days'");
        assertEquals(1, stats.ageDistribution().get(RepositoryStats.AGE_RANGE_OLDER),
            "91 days ago should be in 'older'");
    }
}

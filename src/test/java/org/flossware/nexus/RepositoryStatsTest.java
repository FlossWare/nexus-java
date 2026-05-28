package org.flossware.nexus;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RepositoryStatsTest {

    @Test
    void testRepositoryStatsCreation() {
        List<ComponentMetadata> largest = List.of(
            new ComponentMetadata("id1", "file1.jar", 1000L, "type1", "format1", null, null, null),
            new ComponentMetadata("id2", "file2.jar", 2000L, "type2", "format2", null, null, null)
        );

        Map<String, Integer> sizeDistribution = Map.of(
            RepositoryStats.SIZE_RANGE_UNDER_1MB, 10,
            RepositoryStats.SIZE_RANGE_1_TO_10MB, 5
        );

        Map<String, Long> fileTypeBreakdown = Map.of(
            ".jar", 5000L,
            ".war", 3000L
        );

        Map<String, Integer> ageDistribution = Map.of(
            RepositoryStats.AGE_RANGE_LAST_7_DAYS, 2,
            RepositoryStats.AGE_RANGE_LAST_30_DAYS, 5
        );

        RepositoryStats stats = new RepositoryStats(
            "test-repo",
            15,
            8000L,
            533L,
            500L,
            sizeDistribution,
            fileTypeBreakdown,
            ageDistribution,
            largest
        );

        assertEquals("test-repo", stats.repository());
        assertEquals(15, stats.totalComponents());
        assertEquals(8000L, stats.totalSize());
        assertEquals(533L, stats.averageSize());
        assertEquals(500L, stats.medianSize());
        assertEquals(sizeDistribution, stats.sizeDistribution());
        assertEquals(fileTypeBreakdown, stats.fileTypeBreakdown());
        assertEquals(ageDistribution, stats.ageDistribution());
        assertEquals(largest, stats.largestComponents());
    }

    @Test
    void testGetTotalSizeMB() {
        RepositoryStats stats = new RepositoryStats(
            "test-repo",
            1,
            2 * 1024 * 1024L, // 2 MB
            2 * 1024 * 1024L,
            2 * 1024 * 1024L,
            Map.of(),
            Map.of(),
            Map.of(),
            List.of()
        );

        assertEquals(2.0, stats.getTotalSizeMB(), 0.01);
    }

    @Test
    void testGetTotalSizeGB() {
        RepositoryStats stats = new RepositoryStats(
            "test-repo",
            1,
            3L * 1024 * 1024 * 1024, // 3 GB
            3L * 1024 * 1024 * 1024,
            3L * 1024 * 1024 * 1024,
            Map.of(),
            Map.of(),
            Map.of(),
            List.of()
        );

        assertEquals(3.0, stats.getTotalSizeGB(), 0.01);
    }

    @Test
    void testGetAverageSizeMB() {
        RepositoryStats stats = new RepositoryStats(
            "test-repo",
            10,
            10 * 1024 * 1024L, // 10 MB total
            1 * 1024 * 1024L,  // 1 MB average
            1 * 1024 * 1024L,
            Map.of(),
            Map.of(),
            Map.of(),
            List.of()
        );

        assertEquals(1.0, stats.getAverageSizeMB(), 0.01);
    }

    @Test
    void testGetMedianSizeMB() {
        RepositoryStats stats = new RepositoryStats(
            "test-repo",
            5,
            5 * 1024 * 1024L,
            1 * 1024 * 1024L,
            512 * 1024L,  // 0.5 MB median
            Map.of(),
            Map.of(),
            Map.of(),
            List.of()
        );

        assertEquals(0.5, stats.getMedianSizeMB(), 0.01);
    }

    @Test
    void testGetTotalSizeMBWithZeroSize() {
        RepositoryStats stats = new RepositoryStats(
            "empty-repo",
            0,
            0L,
            0L,
            0L,
            Map.of(),
            Map.of(),
            Map.of(),
            List.of()
        );

        assertEquals(0.0, stats.getTotalSizeMB(), 0.01);
        assertEquals(0.0, stats.getTotalSizeGB(), 0.01);
        assertEquals(0.0, stats.getAverageSizeMB(), 0.01);
        assertEquals(0.0, stats.getMedianSizeMB(), 0.01);
    }

    @Test
    void testGetTotalSizeMBWithSmallSize() {
        RepositoryStats stats = new RepositoryStats(
            "test-repo",
            1,
            512L, // Less than 1 KB
            512L,
            512L,
            Map.of(),
            Map.of(),
            Map.of(),
            List.of()
        );

        assertTrue(stats.getTotalSizeMB() < 0.001);
        assertTrue(stats.getTotalSizeGB() < 0.000001);
    }

    @Test
    void testGetTotalSizeGBWithLargeSize() {
        long largeSize = 100L * 1024 * 1024 * 1024; // 100 GB
        RepositoryStats stats = new RepositoryStats(
            "large-repo",
            1,
            largeSize,
            largeSize,
            largeSize,
            Map.of(),
            Map.of(),
            Map.of(),
            List.of()
        );

        assertEquals(100.0, stats.getTotalSizeGB(), 0.01);
    }

    @Test
    void testSizeDistributionConstants() {
        assertEquals("< 1 MB", RepositoryStats.SIZE_RANGE_UNDER_1MB);
        assertEquals("1-10 MB", RepositoryStats.SIZE_RANGE_1_TO_10MB);
        assertEquals("10-100 MB", RepositoryStats.SIZE_RANGE_10_TO_100MB);
        assertEquals("100 MB - 1 GB", RepositoryStats.SIZE_RANGE_100MB_TO_1GB);
        assertEquals("> 1 GB", RepositoryStats.SIZE_RANGE_OVER_1GB);
    }

    @Test
    void testAgeDistributionConstants() {
        assertEquals("Last 7 days", RepositoryStats.AGE_RANGE_LAST_7_DAYS);
        assertEquals("Last 30 days", RepositoryStats.AGE_RANGE_LAST_30_DAYS);
        assertEquals("Last 90 days", RepositoryStats.AGE_RANGE_LAST_90_DAYS);
        assertEquals("Older than 90 days", RepositoryStats.AGE_RANGE_OLDER);
    }

    @Test
    void testEmptyStatistics() {
        RepositoryStats stats = new RepositoryStats(
            "empty-repo",
            0,
            0L,
            0L,
            0L,
            Map.of(),
            Map.of(),
            Map.of(),
            List.of()
        );

        assertEquals("empty-repo", stats.repository());
        assertEquals(0, stats.totalComponents());
        assertEquals(0L, stats.totalSize());
        assertTrue(stats.sizeDistribution().isEmpty());
        assertTrue(stats.fileTypeBreakdown().isEmpty());
        assertTrue(stats.ageDistribution().isEmpty());
        assertTrue(stats.largestComponents().isEmpty());
    }

    @Test
    void testLargestComponentsLimit() {
        List<ComponentMetadata> many = List.of(
            new ComponentMetadata("id1", "file1.jar", 1000L, null, null, null, null, null),
            new ComponentMetadata("id2", "file2.jar", 2000L, null, null, null, null, null),
            new ComponentMetadata("id3", "file3.jar", 3000L, null, null, null, null, null)
        );

        RepositoryStats stats = new RepositoryStats(
            "test-repo",
            3,
            6000L,
            2000L,
            2000L,
            Map.of(),
            Map.of(),
            Map.of(),
            many
        );

        assertEquals(3, stats.largestComponents().size());
    }

    @Test
    void testRepositoryStatsEquality() {
        RepositoryStats stats1 = new RepositoryStats(
            "test-repo",
            10,
            5000L,
            500L,
            450L,
            Map.of(),
            Map.of(),
            Map.of(),
            List.of()
        );

        RepositoryStats stats2 = new RepositoryStats(
            "test-repo",
            10,
            5000L,
            500L,
            450L,
            Map.of(),
            Map.of(),
            Map.of(),
            List.of()
        );

        assertEquals(stats1, stats2);
        assertEquals(stats1.hashCode(), stats2.hashCode());
    }

    @Test
    void testRepositoryStatsInequality() {
        RepositoryStats stats1 = new RepositoryStats(
            "repo1",
            10,
            5000L,
            500L,
            450L,
            Map.of(),
            Map.of(),
            Map.of(),
            List.of()
        );

        RepositoryStats stats2 = new RepositoryStats(
            "repo2",
            20,
            10000L,
            500L,
            450L,
            Map.of(),
            Map.of(),
            Map.of(),
            List.of()
        );

        assertNotEquals(stats1, stats2);
    }

    @Test
    void testRepositoryStatsToString() {
        RepositoryStats stats = new RepositoryStats(
            "test-repo",
            15,
            8000L,
            533L,
            500L,
            Map.of(),
            Map.of(),
            Map.of(),
            List.of()
        );

        String str = stats.toString();
        assertTrue(str.contains("test-repo"));
        assertTrue(str.contains("15"));
        assertTrue(str.contains("8000"));
    }
}

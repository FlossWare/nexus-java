package org.flossware.nexus;

import org.flossware.jnexus.ComponentMetadata;
import org.flossware.jnexus.RepoRecord;
import org.flossware.jnexus.RepositoryStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

/**
 * Integration tests for statistics calculation edge cases.
 * Tests empty repositories, single components, extreme sizes, and date edge cases.
 */
@DisplayName("Statistics Edge Case Tests")
class NexusIntegrationStatisticsEdgeCaseTest {

    @Mock
    private NexusClient mockClient;

    private NexusService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new NexusService(mockClient);
    }

    @Test
    @DisplayName("Statistics on empty repository")
    void testStatsOnEmptyRepository() throws Exception {
        List<ComponentMetadata> components = List.of();

        RepositoryStats stats = service.calculateStatistics("empty-repo", components);

        assertEquals(0, stats.totalComponents(), "Should have zero components");
        assertEquals(0, stats.totalSize(), "Should have zero total size");
        // Average/median of empty set should handle gracefully
        assertNotNull(stats.toString(), "Should have valid string representation");
    }

    @Test
    @DisplayName("Statistics on single component")
    void testStatsOnSingleComponent() throws Exception {
        List<ComponentMetadata> components = List.of(
            new ComponentMetadata("id1", "file.jar", 1024, "application/java-archive", "maven2", null, null, null)
        );

        RepositoryStats stats = service.calculateStatistics("single-repo", components);

        assertEquals(1, stats.totalComponents());
        assertEquals(1024, stats.totalSize());
        // Median of single item should be that item
        assertEquals(1024, stats.medianSize());
        assertNotNull(stats.toString());
    }

    @Test
    @DisplayName("Statistics with extreme file sizes (1 byte to 10GB)")
    void testStatsWithExtremeFileSizes() throws Exception {
        List<ComponentMetadata> components = List.of(
            new ComponentMetadata("id1", "tiny.jar", 1, "type", "fmt", null, null, null),                    // 1 byte
            new ComponentMetadata("id2", "1mb.jar", 1024 * 1024, "type", "fmt", null, null, null),          // 1MB
            new ComponentMetadata("id3", "100mb.jar", 100 * 1024 * 1024, "type", "fmt", null, null, null),  // 100MB
            new ComponentMetadata("id4", "10gb.jar", 10L * 1024 * 1024 * 1024, "type", "fmt", null, null, null) // 10GB
        );

        RepositoryStats stats = service.calculateStatistics("extreme-repo", components);

        assertEquals(4, stats.totalComponents());
        long expectedTotal = 1 + (1024 * 1024) + (100 * 1024 * 1024) + (10L * 1024 * 1024 * 1024);
        assertEquals(expectedTotal, stats.totalSize());

        // Verify bucketing works
        assertNotNull(stats.sizeDistribution(), "Should have valid buckets");
    }

    @Test
    @DisplayName("Statistics with many files in 1MB bucket")
    void testStatsWithManySmallFiles() throws Exception {
        List<ComponentMetadata> components = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            components.add(new ComponentMetadata("id" + i, "file" + i + ".jar",
                512 * 1024, "type", "fmt", null, null, null)); // 512KB each
        }

        RepositoryStats stats = service.calculateStatistics("many-small-repo", components);

        assertEquals(1000, stats.totalComponents());
        assertEquals(512 * 1024L * 1000, stats.totalSize());
        assertNotNull(stats.toString());
    }

    @Test
    @DisplayName("Statistics with same-sized files (no variance)")
    void testStatsWithIdenticalFileSizes() throws Exception {
        List<ComponentMetadata> components = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            components.add(new ComponentMetadata("id" + i, "file" + i + ".jar",
                1024, "type", "fmt", null, null, null)); // All 1KB
        }

        RepositoryStats stats = service.calculateStatistics("uniform-repo", components);

        assertEquals(100, stats.totalComponents());
        assertEquals(100 * 1024, stats.totalSize());
        // Average and median should be same
        assertEquals(1024, stats.averageSize());
        assertEquals(1024, stats.medianSize());
    }

    @Test
    @DisplayName("Statistics handles files with createdDate in future")
    void testStatsWithFutureDates() throws Exception {
        Instant now = Instant.now();
        Instant tomorrow = now.plus(1, ChronoUnit.DAYS);
        Instant nextYear = now.plus(365, ChronoUnit.DAYS);

        List<ComponentMetadata> components = List.of(
            new ComponentMetadata("id1", "file1.jar", 1024, "type", "fmt", tomorrow, null, null),
            new ComponentMetadata("id2", "file2.jar", 2048, "type", "fmt", nextYear, null, null)
        );

        // Should handle future dates gracefully in statistics
        RepositoryStats stats = service.calculateStatistics("future-repo", components);

        assertNotNull(stats, "Should calculate stats even with future dates");
        assertEquals(2, stats.totalComponents());
    }

    @Test
    @DisplayName("Statistics age distribution with all very old files")
    void testStatsWithVeryOldFiles() throws Exception {
        Instant veryOld = Instant.now().minus(3650, ChronoUnit.DAYS); // 10 years ago

        List<ComponentMetadata> components = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            components.add(new ComponentMetadata("id" + i, "old-file" + i + ".jar",
                1024, "type", "fmt", veryOld, null, null));
        }

        RepositoryStats stats = service.calculateStatistics("old-repo", components);

        assertEquals(50, stats.totalComponents());
        // All should be in "older" bucket
        assertEquals(50, stats.ageDistribution().get(RepositoryStats.AGE_RANGE_OLDER));
    }

    @Test
    @DisplayName("Statistics with zero-byte files")
    void testStatsWithZeroByteFiles() throws Exception {
        List<ComponentMetadata> components = List.of(
            new ComponentMetadata("id1", "empty1.jar", 0, "type", "fmt", null, null, null),
            new ComponentMetadata("id2", "empty2.jar", 0, "type", "fmt", null, null, null),
            new ComponentMetadata("id3", "normal.jar", 1024, "type", "fmt", null, null, null)
        );

        RepositoryStats stats = service.calculateStatistics("mixed-repo", components);

        assertEquals(3, stats.totalComponents());
        assertEquals(1024, stats.totalSize());
        assertNotNull(stats.toString());
    }

    @Test
    @DisplayName("Statistics with mixed file extensions")
    void testStatsWithMixedExtensions() throws Exception {
        List<ComponentMetadata> components = List.of(
            new ComponentMetadata("id1", "file.jar", 1024, "type", "fmt", null, null, null),
            new ComponentMetadata("id2", "file.war", 2048, "type", "fmt", null, null, null),
            new ComponentMetadata("id3", "file.ear", 512, "type", "fmt", null, null, null),
            new ComponentMetadata("id4", "file.pom", 256, "type", "fmt", null, null, null),
            new ComponentMetadata("id5", "file.txt", 128, "type", "fmt", null, null, null)
        );

        RepositoryStats stats = service.calculateStatistics("mixed-ext-repo", components);

        assertEquals(5, stats.totalComponents());
        // Verify file type breakdown
        assertEquals(1024L, stats.fileTypeBreakdown().get(".jar"));
        assertEquals(2048L, stats.fileTypeBreakdown().get(".war"));
    }

    @Test
    @DisplayName("Statistics large dataset (10000 files)")
    void testStatsWithLargeDataset() throws Exception {
        List<ComponentMetadata> components = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            long size = (long)(Math.random() * 100 * 1024 * 1024); // 0-100MB random
            components.add(new ComponentMetadata("id" + i, "file" + i + ".jar",
                size, "type", "fmt", null, null, null));
        }

        assertDoesNotThrow(() -> {
            RepositoryStats stats = service.calculateStatistics("large-repo", components);
            assertEquals(10000, stats.totalComponents());
            assertNotNull(stats.toString());
        });
    }

    @Test
    @DisplayName("Statistics file type breakdown accuracy")
    void testStatsFileTypeBreakdown() throws Exception {
        List<ComponentMetadata> components = List.of(
            new ComponentMetadata("id1", "app.jar", 1000, "type", "fmt", null, null, null),
            new ComponentMetadata("id2", "lib.jar", 2000, "type", "fmt", null, null, null),
            new ComponentMetadata("id3", "web.war", 3000, "type", "fmt", null, null, null),
            new ComponentMetadata("id4", "config.xml", 4000, "type", "fmt", null, null, null),
            new ComponentMetadata("id5", "readme.txt", 5000, "type", "fmt", null, null, null)
        );

        RepositoryStats stats = service.calculateStatistics("breakdown-repo", components);

        // Should track size by extension
        assertEquals(3000L, stats.fileTypeBreakdown().get(".jar"));
        assertEquals(3000L, stats.fileTypeBreakdown().get(".war"));
        assertEquals(4000L, stats.fileTypeBreakdown().get(".xml"));
        assertEquals(5000L, stats.fileTypeBreakdown().get(".txt"));
        assertEquals(5, stats.totalComponents());
        assertEquals(15000, stats.totalSize());
    }

    @Test
    @DisplayName("Statistics median calculation with odd number of items")
    void testStatsMedianOddCount() throws Exception {
        List<ComponentMetadata> components = List.of(
            new ComponentMetadata("id1", "file1.jar", 100, "type", "fmt", null, null, null),
            new ComponentMetadata("id2", "file2.jar", 500, "type", "fmt", null, null, null),
            new ComponentMetadata("id3", "file3.jar", 1000, "type", "fmt", null, null, null),
            new ComponentMetadata("id4", "file4.jar", 2000, "type", "fmt", null, null, null),
            new ComponentMetadata("id5", "file5.jar", 5000, "type", "fmt", null, null, null)
        );

        RepositoryStats stats = service.calculateStatistics("median-odd-repo", components);

        assertEquals(5, stats.totalComponents());
        // Median should be 1000 (middle value)
        assertEquals(1000, stats.medianSize());
    }

    @Test
    @DisplayName("Statistics median calculation with even number of items")
    void testStatsMedianEvenCount() throws Exception {
        List<ComponentMetadata> components = List.of(
            new ComponentMetadata("id1", "file1.jar", 100, "type", "fmt", null, null, null),
            new ComponentMetadata("id2", "file2.jar", 500, "type", "fmt", null, null, null),
            new ComponentMetadata("id3", "file3.jar", 1000, "type", "fmt", null, null, null),
            new ComponentMetadata("id4", "file4.jar", 2000, "type", "fmt", null, null, null)
        );

        RepositoryStats stats = service.calculateStatistics("median-even-repo", components);

        assertEquals(4, stats.totalComponents());
        // Median should be average of 500 and 1000 = 750
        assertEquals(750, stats.medianSize());
    }

    @Test
    @DisplayName("Statistics with all null creation dates")
    void testStatsWithAllNullDates() throws Exception {
        List<ComponentMetadata> components = List.of(
            new ComponentMetadata("id1", "file1.jar", 1024, "type", "fmt", null, null, null),
            new ComponentMetadata("id2", "file2.jar", 2048, "type", "fmt", null, null, null)
        );

        RepositoryStats stats = service.calculateStatistics("null-dates-repo", components);

        assertEquals(2, stats.totalComponents());
        // Components with null dates should be counted as "older"
        assertEquals(2, stats.ageDistribution().get(RepositoryStats.AGE_RANGE_OLDER));
    }

    @Test
    @DisplayName("Statistics with files without extensions")
    void testStatsWithNoExtensionFiles() throws Exception {
        List<ComponentMetadata> components = List.of(
            new ComponentMetadata("id1", "Dockerfile", 512, "type", "fmt", null, null, null),
            new ComponentMetadata("id2", "Makefile", 256, "type", "fmt", null, null, null),
            new ComponentMetadata("id3", "LICENSE", 1024, "type", "fmt", null, null, null)
        );

        RepositoryStats stats = service.calculateStatistics("no-ext-repo", components);

        assertEquals(3, stats.totalComponents());
        // Files without extensions should be grouped under "(no extension)"
        assertNotNull(stats.fileTypeBreakdown().get("(no extension)"));
        assertEquals(1792L, stats.fileTypeBreakdown().get("(no extension)"));
    }
}

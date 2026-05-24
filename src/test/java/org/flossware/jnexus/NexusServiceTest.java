package org.flossware.jnexus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NexusServiceTest {

    @Mock
    private NexusClient mockClient;

    private NexusService service;
    private ByteArrayOutputStream outputStream;
    private PrintStream originalOut;

    @BeforeEach
    void setUp() {
        service = new NexusService(mockClient);
        outputStream = new ByteArrayOutputStream();
        originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
    }

    @Test
    void testListRepositoryWithoutFilter() throws IOException, InterruptedException {
        List<RepoRecord> mockRecords = List.of(
            new RepoRecord("id1", 1000, "path/to/artifact1.jar"),
            new RepoRecord("id2", 2000, "path/to/artifact2.jar")
        );

        when(mockClient.listComponents(eq("test-repo"), anyBoolean())).thenReturn(mockRecords);

        service.listRepository("test-repo", null);

        verify(mockClient, times(1)).listComponents(eq("test-repo"), anyBoolean());
        String output = outputStream.toString();
        assertTrue(output.contains("artifact1.jar"));
        assertTrue(output.contains("artifact2.jar"));
        assertTrue(output.contains("Total components"));
    }

    @Test
    void testListRepositoryWithFilter() throws IOException, InterruptedException {
        List<RepoRecord> mockRecords = List.of(
            new RepoRecord("id1", 1000, "path/to/artifact-1.0.0.jar"),
            new RepoRecord("id2", 2000, "path/to/artifact-1.0.1-SNAPSHOT.jar"),
            new RepoRecord("id3", 3000, "path/to/artifact-2.0.0.jar")
        );

        when(mockClient.listComponents(eq("test-repo"), anyBoolean())).thenReturn(mockRecords);

        service.listRepository("test-repo", ".*SNAPSHOT.*");

        verify(mockClient, times(1)).listComponents(eq("test-repo"), anyBoolean());
        String output = outputStream.toString();
        assertFalse(output.contains("artifact-1.0.0.jar"));
        assertTrue(output.contains("SNAPSHOT"));
        assertTrue(output.contains("Matching components:"));
    }

    @Test
    void testDeleteFromRepositoryDryRun() throws IOException, InterruptedException {
        List<RepoRecord> mockRecords = List.of(
            new RepoRecord("id1", 1000, "path/to/artifact1.jar")
        );

        when(mockClient.listComponents(eq("test-repo"), anyBoolean())).thenReturn(mockRecords);

        service.deleteFromRepository("test-repo", null, true);

        verify(mockClient, times(1)).listComponents(eq("test-repo"), anyBoolean());
        verify(mockClient, never()).deleteComponent(anyString());

        String output = outputStream.toString();
        assertTrue(output.contains("DRY-RUN MODE"));
    }

    @Test
    void testDeleteFromRepositoryActual() throws IOException, InterruptedException {
        List<RepoRecord> mockRecords = List.of(
            new RepoRecord("id1", 1000, "path/to/artifact1.jar"),
            new RepoRecord("id2", 2000, "path/to/artifact2.jar")
        );

        when(mockClient.listComponents(eq("test-repo"), anyBoolean())).thenReturn(mockRecords);
        doNothing().when(mockClient).deleteComponent(anyString());

        service.deleteFromRepository("test-repo", null, false);

        verify(mockClient, times(1)).listComponents(eq("test-repo"), anyBoolean());
        verify(mockClient, times(1)).deleteComponent("id1");
        verify(mockClient, times(1)).deleteComponent("id2");

        String output = outputStream.toString();
        assertTrue(output.contains("Deleting:"));
        assertTrue(output.contains("Deleted 2 of 2"));
    }

    @Test
    void testDeleteFromRepositoryWithFilter() throws IOException, InterruptedException {
        List<RepoRecord> mockRecords = List.of(
            new RepoRecord("id1", 1000, "path/to/artifact-1.0.0.jar"),
            new RepoRecord("id2", 2000, "path/to/artifact-SNAPSHOT.jar")
        );

        when(mockClient.listComponents(eq("test-repo"), anyBoolean())).thenReturn(mockRecords);
        doNothing().when(mockClient).deleteComponent(anyString());

        service.deleteFromRepository("test-repo", ".*SNAPSHOT.*", false);

        verify(mockClient, times(1)).listComponents(eq("test-repo"), anyBoolean());
        verify(mockClient, times(1)).deleteComponent("id2");
        verify(mockClient, never()).deleteComponent("id1");
    }

    @Test
    void testDeleteFromRepositoryNoMatches() throws IOException, InterruptedException {
        List<RepoRecord> mockRecords = List.of(
            new RepoRecord("id1", 1000, "path/to/artifact.jar")
        );

        when(mockClient.listComponents(eq("test-repo"), anyBoolean())).thenReturn(mockRecords);

        service.deleteFromRepository("test-repo", ".*SNAPSHOT.*", false);

        verify(mockClient, times(1)).listComponents(eq("test-repo"), anyBoolean());
        verify(mockClient, never()).deleteComponent(anyString());

        String output = outputStream.toString();
        assertTrue(output.contains("No components match"));
    }

    @Test
    void testListRepositoryWithInvalidRegex() {
        // Test that invalid regex is caught early
        assertThrows(IllegalArgumentException.class, () -> {
            service.listRepository("test-repo", "[invalid(regex");
        });

        // Verify that listComponents was never called
        verifyNoInteractions(mockClient);
    }

    @Test
    void testDeleteFromRepositoryWithInvalidRegex() {
        // Test that invalid regex is caught early
        assertThrows(IllegalArgumentException.class, () -> {
            service.deleteFromRepository("test-repo", "*invalid+regex", false);
        });

        // Verify that listComponents was never called
        verifyNoInteractions(mockClient);
    }

    @Test
    void testListRepositoryWithValidComplexRegex() throws IOException, InterruptedException {
        List<RepoRecord> mockRecords = List.of(
            new RepoRecord("id1", 1000, "com/example/app-1.0.jar")
        );

        when(mockClient.listComponents(eq("test-repo"), anyBoolean())).thenReturn(mockRecords);

        // Test with a complex but valid regex
        service.listRepository("test-repo", "^com/[a-z]+/.*\\.jar$");

        verify(mockClient, times(1)).listComponents(eq("test-repo"), anyBoolean());
    }

    @Test
    void testSearchComponentsWithNoFilters() throws IOException, InterruptedException {
        List<ComponentMetadata> mockMetadata = List.of(
            new ComponentMetadata("id1", "file1.jar", 1000L, "type1", "format1", null, null, null),
            new ComponentMetadata("id2", "file2.jar", 2000L, "type2", "format2", null, null, null)
        );

        when(mockClient.listComponentsWithMetadata(eq("test-repo"), anyBoolean())).thenReturn(mockMetadata);

        SearchCriteria criteria = new SearchCriteria.Builder().repository("test-repo").build();
        List<ComponentMetadata> results = service.searchComponents(criteria, false);

        assertEquals(2, results.size());
        verify(mockClient, times(1)).listComponentsWithMetadata(eq("test-repo"), eq(false));
    }

    @Test
    void testSearchComponentsWithSizeFilter() throws IOException, InterruptedException {
        List<ComponentMetadata> mockMetadata = List.of(
            new ComponentMetadata("id1", "small.jar", 500L, "type1", "format1", null, null, null),
            new ComponentMetadata("id2", "medium.jar", 1500L, "type2", "format2", null, null, null),
            new ComponentMetadata("id3", "large.jar", 3000L, "type3", "format3", null, null, null)
        );

        when(mockClient.listComponentsWithMetadata(eq("test-repo"), anyBoolean())).thenReturn(mockMetadata);

        SearchCriteria criteria = new SearchCriteria.Builder()
            .repository("test-repo")
            .minSize(1000L)
            .maxSize(2000L)
            .build();

        List<ComponentMetadata> results = service.searchComponents(criteria, false);

        assertEquals(1, results.size());
        assertEquals("medium.jar", results.get(0).path());
    }

    @Test
    void testSearchComponentsWithDateFilter() throws IOException, InterruptedException {
        java.time.Instant early = java.time.Instant.parse("2024-01-01T00:00:00Z");
        java.time.Instant middle = java.time.Instant.parse("2024-06-15T12:00:00Z");
        java.time.Instant late = java.time.Instant.parse("2024-12-31T23:59:59Z");

        List<ComponentMetadata> mockMetadata = List.of(
            new ComponentMetadata("id1", "old.jar", 1000L, "type1", "format1", early, early, null),
            new ComponentMetadata("id2", "recent.jar", 2000L, "type2", "format2", middle, middle, null),
            new ComponentMetadata("id3", "latest.jar", 3000L, "type3", "format3", late, late, null)
        );

        when(mockClient.listComponentsWithMetadata(eq("test-repo"), anyBoolean())).thenReturn(mockMetadata);

        SearchCriteria criteria = new SearchCriteria.Builder()
            .repository("test-repo")
            .createdAfter(java.time.Instant.parse("2024-06-01T00:00:00Z"))
            .createdBefore(java.time.Instant.parse("2024-07-01T00:00:00Z"))
            .build();

        List<ComponentMetadata> results = service.searchComponents(criteria, false);

        assertEquals(1, results.size());
        assertEquals("recent.jar", results.get(0).path());
    }

    @Test
    void testSearchComponentsWithExtensionFilter() throws IOException, InterruptedException {
        List<ComponentMetadata> mockMetadata = List.of(
            new ComponentMetadata("id1", "app.jar", 1000L, "type1", "format1", null, null, null),
            new ComponentMetadata("id2", "app.war", 2000L, "type2", "format2", null, null, null),
            new ComponentMetadata("id3", "lib.jar", 3000L, "type3", "format3", null, null, null)
        );

        when(mockClient.listComponentsWithMetadata(eq("test-repo"), anyBoolean())).thenReturn(mockMetadata);

        SearchCriteria criteria = new SearchCriteria.Builder()
            .repository("test-repo")
            .fileExtension(".jar")
            .build();

        List<ComponentMetadata> results = service.searchComponents(criteria, false);

        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(m -> m.path().endsWith(".jar")));
    }

    @Test
    void testSearchComponentsWithCombinedFilters() throws IOException, InterruptedException {
        java.time.Instant created = java.time.Instant.parse("2024-06-15T12:00:00Z");

        List<ComponentMetadata> mockMetadata = List.of(
            new ComponentMetadata("id1", "small-old.jar", 500L, "type1", "format1", java.time.Instant.parse("2024-01-01T00:00:00Z"), null, null),
            new ComponentMetadata("id2", "large-recent.jar", 5000L, "type2", "format2", created, null, null),
            new ComponentMetadata("id3", "medium-recent.jar", 1500L, "type3", "format3", created, null, null),
            new ComponentMetadata("id4", "medium-recent.war", 1500L, "type4", "format4", created, null, null)
        );

        when(mockClient.listComponentsWithMetadata(eq("test-repo"), anyBoolean())).thenReturn(mockMetadata);

        SearchCriteria criteria = new SearchCriteria.Builder()
            .repository("test-repo")
            .minSize(1000L)
            .maxSize(2000L)
            .createdAfter(java.time.Instant.parse("2024-06-01T00:00:00Z"))
            .fileExtension(".jar")
            .build();

        List<ComponentMetadata> results = service.searchComponents(criteria, false);

        assertEquals(1, results.size());
        assertEquals("medium-recent.jar", results.get(0).path());
    }

    @Test
    void testCalculateStatisticsBasicCounts() throws IOException, InterruptedException {
        List<ComponentMetadata> components = List.of(
            new ComponentMetadata("id1", "file1.jar", 1000L, "type1", "format1", null, null, null),
            new ComponentMetadata("id2", "file2.jar", 2000L, "type2", "format2", null, null, null),
            new ComponentMetadata("id3", "file3.jar", 3000L, "type3", "format3", null, null, null)
        );

        RepositoryStats stats = service.calculateStatistics("test-repo", components);

        assertEquals("test-repo", stats.repository());
        assertEquals(3, stats.totalComponents());
        assertEquals(6000L, stats.totalSize());
        assertEquals(2000L, stats.averageSize());
        assertEquals(2000L, stats.medianSize());
    }

    @Test
    void testCalculateStatisticsSizeDistribution() throws IOException, InterruptedException {
        List<ComponentMetadata> components = List.of(
            new ComponentMetadata("id1", "tiny.jar", 500L, null, null, null, null, null), // < 1MB
            new ComponentMetadata("id2", "small.jar", 5 * 1024 * 1024L, null, null, null, null, null), // 1-10MB
            new ComponentMetadata("id3", "medium.jar", 50 * 1024 * 1024L, null, null, null, null, null), // 10-100MB
            new ComponentMetadata("id4", "large.jar", 500 * 1024 * 1024L, null, null, null, null, null), // 100MB-1GB
            new ComponentMetadata("id5", "huge.jar", 2L * 1024 * 1024 * 1024, null, null, null, null, null) // >1GB
        );

        RepositoryStats stats = service.calculateStatistics("test-repo", components);

        assertEquals(1, stats.sizeDistribution().get(RepositoryStats.SIZE_RANGE_UNDER_1MB));
        assertEquals(1, stats.sizeDistribution().get(RepositoryStats.SIZE_RANGE_1_TO_10MB));
        assertEquals(1, stats.sizeDistribution().get(RepositoryStats.SIZE_RANGE_10_TO_100MB));
        assertEquals(1, stats.sizeDistribution().get(RepositoryStats.SIZE_RANGE_100MB_TO_1GB));
        assertEquals(1, stats.sizeDistribution().get(RepositoryStats.SIZE_RANGE_OVER_1GB));
    }

    @Test
    void testCalculateStatisticsFileTypeBreakdown() throws IOException, InterruptedException {
        List<ComponentMetadata> components = List.of(
            new ComponentMetadata("id1", "app.jar", 1000L, null, null, null, null, null),
            new ComponentMetadata("id2", "lib.jar", 2000L, null, null, null, null, null),
            new ComponentMetadata("id3", "webapp.war", 3000L, null, null, null, null, null)
        );

        RepositoryStats stats = service.calculateStatistics("test-repo", components);

        assertEquals(3000L, stats.fileTypeBreakdown().get(".jar"));
        assertEquals(3000L, stats.fileTypeBreakdown().get(".war"));
    }

    @Test
    void testCalculateStatisticsAgeDistribution() throws IOException, InterruptedException {
        java.time.Instant now = java.time.Instant.now();
        java.time.Instant last7Days = now.minus(java.time.Duration.ofDays(5));
        java.time.Instant last30Days = now.minus(java.time.Duration.ofDays(20));
        java.time.Instant last90Days = now.minus(java.time.Duration.ofDays(60));
        java.time.Instant older = now.minus(java.time.Duration.ofDays(120));

        List<ComponentMetadata> components = List.of(
            new ComponentMetadata("id1", "file1.jar", 1000L, null, null, last7Days, null, null),
            new ComponentMetadata("id2", "file2.jar", 2000L, null, null, last30Days, null, null),
            new ComponentMetadata("id3", "file3.jar", 3000L, null, null, last90Days, null, null),
            new ComponentMetadata("id4", "file4.jar", 4000L, null, null, older, null, null)
        );

        RepositoryStats stats = service.calculateStatistics("test-repo", components);

        assertEquals(1, stats.ageDistribution().get(RepositoryStats.AGE_RANGE_LAST_7_DAYS));
        assertEquals(1, stats.ageDistribution().get(RepositoryStats.AGE_RANGE_LAST_30_DAYS));
        assertEquals(1, stats.ageDistribution().get(RepositoryStats.AGE_RANGE_LAST_90_DAYS));
        assertEquals(1, stats.ageDistribution().get(RepositoryStats.AGE_RANGE_OLDER));
    }

    @Test
    void testCalculateStatisticsLargestComponents() throws IOException, InterruptedException {
        List<ComponentMetadata> components = List.of(
            new ComponentMetadata("id1", "small.jar", 100L, null, null, null, null, null),
            new ComponentMetadata("id2", "medium.jar", 500L, null, null, null, null, null),
            new ComponentMetadata("id3", "large.jar", 1000L, null, null, null, null, null)
        );

        RepositoryStats stats = service.calculateStatistics("test-repo", components);

        assertEquals(3, stats.largestComponents().size());
        assertEquals("large.jar", stats.largestComponents().get(0).path());
        assertEquals("medium.jar", stats.largestComponents().get(1).path());
        assertEquals("small.jar", stats.largestComponents().get(2).path());
    }

    @Test
    void testCalculateStatisticsEmptyRepository() throws IOException, InterruptedException {
        RepositoryStats stats = service.calculateStatistics("empty-repo", List.of());

        assertEquals("empty-repo", stats.repository());
        assertEquals(0, stats.totalComponents());
        assertEquals(0L, stats.totalSize());
        assertEquals(0L, stats.averageSize());
        assertEquals(0L, stats.medianSize());
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }
}

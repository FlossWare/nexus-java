package org.flossware.nexus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.PatternSyntaxException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Edge case tests for NexusService.
 * Tests null/invalid inputs, size/date edge cases, and service-level robustness.
 */
class NexusServiceEdgeCaseTest {

    @Mock
    private NexusClient mockClient;

    private NexusService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new NexusService(mockClient);
    }

    // ========== Null/Empty/Invalid Input Tests ==========

    @Test
    void testNullRepositoryName() {
        assertThrows(IllegalArgumentException.class, () ->
            Credentials.validateRepository(null),
            "Should reject null repository name"
        );
    }

    @Test
    void testEmptyRepositoryName() {
        assertThrows(IllegalArgumentException.class, () ->
            Credentials.validateRepository(""),
            "Should reject empty repository name"
        );
    }

    @Test
    void testWhitespaceOnlyRepositoryName() {
        assertThrows(IllegalArgumentException.class, () ->
            Credentials.validateRepository("   "),
            "Should reject whitespace-only repository name"
        );

        assertThrows(IllegalArgumentException.class, () ->
            Credentials.validateRepository("\t\n\r"),
            "Should reject tab/newline repository name"
        );
    }

    @Test
    void testInvalidRegexPattern() throws Exception {
        when(mockClient.listComponents(anyString(), anyBoolean()))
            .thenReturn(List.of(new RepoRecord("id1", 1000, "test.jar")));

        // Invalid regex pattern should throw IllegalArgumentException during validation
        assertThrows(IllegalArgumentException.class, () ->
            service.deleteFromRepository("test-repo", "[invalid", false, null),
            "Should throw IllegalArgumentException for invalid regex pattern"
        );
    }

    @Test
    void testNullRegexFilter() throws Exception {
        List<RepoRecord> mockRecords = List.of(
            new RepoRecord("id1", 1000, "test1.jar"),
            new RepoRecord("id2", 2000, "test2.jar")
        );

        when(mockClient.listComponents(anyString(), anyBoolean())).thenReturn(mockRecords);

        // Null regex should match everything
        service.deleteFromRepository("test-repo", null, true, null);

        // Both components should be "deleted" (dry-run)
        verify(mockClient, never()).deleteComponent(anyString());
    }

    @Test
    void testEmptyRegexFilter() throws Exception {
        List<RepoRecord> mockRecords = List.of(
            new RepoRecord("id1", 1000, "test.jar")
        );

        when(mockClient.listComponents(anyString(), anyBoolean())).thenReturn(mockRecords);

        // Empty regex should match everything
        service.deleteFromRepository("test-repo", "", true, null);

        verify(mockClient, never()).deleteComponent(anyString());
    }

    // ========== Size Filter Edge Cases ==========

    @Test
    void testNegativeMinSize() {
        assertThrows(IllegalArgumentException.class, () ->
            new SearchCriteria.Builder()
                .repository("test")
                .minSize(-100L)
                .build(),
            "Should reject negative minSize"
        );
    }

    @Test
    void testNegativeMaxSize() {
        assertThrows(IllegalArgumentException.class, () ->
            new SearchCriteria.Builder()
                .repository("test")
                .maxSize(-100L)
                .build(),
            "Should reject negative maxSize"
        );
    }

    @Test
    void testMinSizeGreaterThanMaxSize() {
        assertThrows(IllegalArgumentException.class, () ->
            new SearchCriteria.Builder()
                .repository("test")
                .minSize(1000L)
                .maxSize(500L)
                .build(),
            "Should reject minSize > maxSize"
        );
    }

    @Test
    void testZeroMinSize() throws Exception {
        SearchCriteria criteria = new SearchCriteria.Builder()
            .repository("test")
            .minSize(0L)
            .build();

        List<ComponentMetadata> mockComponents = List.of(
            new ComponentMetadata("id1", "empty.txt", 0, null, null, null, null, null),
            new ComponentMetadata("id2", "test.jar", 1000, null, null, null, null, null)
        );

        when(mockClient.listComponentsWithMetadata(anyString(), anyBoolean()))
            .thenReturn(mockComponents);

        List<ComponentMetadata> results = service.searchComponents(criteria, false);
        assertEquals(2, results.size(), "Zero minSize should include all components");
    }

    @Test
    void testMaxSizeLongMaxValue() throws Exception {
        SearchCriteria criteria = new SearchCriteria.Builder()
            .repository("test")
            .maxSize(Long.MAX_VALUE)
            .build();

        List<ComponentMetadata> mockComponents = List.of(
            new ComponentMetadata("id1", "huge.jar", Long.MAX_VALUE, null, null, null, null, null),
            new ComponentMetadata("id2", "small.jar", 1000, null, null, null, null, null)
        );

        when(mockClient.listComponentsWithMetadata(anyString(), anyBoolean()))
            .thenReturn(mockComponents);

        List<ComponentMetadata> results = service.searchComponents(criteria, false);
        assertEquals(2, results.size(), "Long.MAX_VALUE maxSize should include all components");
    }

    @Test
    void testEqualMinAndMaxSize() throws Exception {
        SearchCriteria criteria = new SearchCriteria.Builder()
            .repository("test")
            .minSize(1000L)
            .maxSize(1000L)
            .build();

        List<ComponentMetadata> mockComponents = List.of(
            new ComponentMetadata("id1", "smaller.jar", 999, null, null, null, null, null),
            new ComponentMetadata("id2", "exact.jar", 1000, null, null, null, null, null),
            new ComponentMetadata("id3", "larger.jar", 1001, null, null, null, null, null)
        );

        when(mockClient.listComponentsWithMetadata(anyString(), anyBoolean()))
            .thenReturn(mockComponents);

        List<ComponentMetadata> results = service.searchComponents(criteria, false);
        assertEquals(1, results.size(), "Should match only exact size");
        assertEquals("exact.jar", results.get(0).path());
    }

    // ========== Date Filter Edge Cases ==========

    @Test
    void testCreatedAfterAfterCreatedBefore() {
        Instant now = Instant.now();
        Instant past = now.minus(1, ChronoUnit.DAYS);

        assertThrows(IllegalArgumentException.class, () ->
            new SearchCriteria.Builder()
                .repository("test")
                .createdAfter(now)
                .createdBefore(past)
                .build(),
            "Should reject createdAfter > createdBefore"
        );
    }

    @Test
    void testEqualCreatedDates() throws Exception {
        Instant now = Instant.now();

        SearchCriteria criteria = new SearchCriteria.Builder()
            .repository("test")
            .createdAfter(now)
            .createdBefore(now)
            .build();

        List<ComponentMetadata> mockComponents = List.of(
            new ComponentMetadata("id1", "test.jar", 1000, null, null, now, null, null)
        );

        when(mockClient.listComponentsWithMetadata(anyString(), anyBoolean()))
            .thenReturn(mockComponents);

        List<ComponentMetadata> results = service.searchComponents(criteria, false);
        assertEquals(1, results.size(), "Equal dates should match (isBefore and isAfter both return false for equal)");
    }

    @Test
    void testFarFutureDate() throws Exception {
        Instant farFuture = Instant.parse("2999-12-31T23:59:59Z");

        SearchCriteria criteria = new SearchCriteria.Builder()
            .repository("test")
            .createdAfter(farFuture)
            .build();

        List<ComponentMetadata> mockComponents = List.of(
            new ComponentMetadata("id1", "test.jar", 1000, null, null, Instant.now(), null, null)
        );

        when(mockClient.listComponentsWithMetadata(anyString(), anyBoolean()))
            .thenReturn(mockComponents);

        List<ComponentMetadata> results = service.searchComponents(criteria, false);
        assertTrue(results.isEmpty(), "Far future date should match nothing");
    }

    @Test
    void testFarPastDate() throws Exception {
        Instant farPast = Instant.parse("1970-01-01T00:00:00Z");

        SearchCriteria criteria = new SearchCriteria.Builder()
            .repository("test")
            .createdBefore(farPast)
            .build();

        List<ComponentMetadata> mockComponents = List.of(
            new ComponentMetadata("id1", "test.jar", 1000, null, null, Instant.now(), null, null)
        );

        when(mockClient.listComponentsWithMetadata(anyString(), anyBoolean()))
            .thenReturn(mockComponents);

        List<ComponentMetadata> results = service.searchComponents(criteria, false);
        assertTrue(results.isEmpty(), "Far past date should match nothing for current data");
    }

    @Test
    void testNullCreatedDate() throws Exception {
        Instant now = Instant.now();

        SearchCriteria criteria = new SearchCriteria.Builder()
            .repository("test")
            .createdAfter(now.minus(1, ChronoUnit.DAYS))
            .build();

        List<ComponentMetadata> mockComponents = List.of(
            new ComponentMetadata("id1", "no-date.jar", 1000, null, null, null, null, null),
            new ComponentMetadata("id2", "with-date.jar", 2000, null, null, now, null, null)
        );

        when(mockClient.listComponentsWithMetadata(anyString(), anyBoolean()))
            .thenReturn(mockComponents);

        List<ComponentMetadata> results = service.searchComponents(criteria, false);
        assertEquals(2, results.size(), "Components with null createdDate pass all date filters");
    }

    // ========== File Extension Edge Cases ==========

    @Test
    void testFileExtensionWithoutDot() throws Exception {
        SearchCriteria criteria = new SearchCriteria.Builder()
            .repository("test")
            .fileExtension("jar")  // Without dot
            .build();

        List<ComponentMetadata> mockComponents = List.of(
            new ComponentMetadata("id1", "test.jar", 1000, null, null, null, null, null)
        );

        when(mockClient.listComponentsWithMetadata(anyString(), anyBoolean()))
            .thenReturn(mockComponents);

        List<ComponentMetadata> results = service.searchComponents(criteria, false);
        assertEquals(1, results.size(), "Extension without dot should work");
    }

    @Test
    void testFileExtensionWithDot() throws Exception {
        SearchCriteria criteria = new SearchCriteria.Builder()
            .repository("test")
            .fileExtension(".jar")  // With dot
            .build();

        List<ComponentMetadata> mockComponents = List.of(
            new ComponentMetadata("id1", "test.jar", 1000, null, null, null, null, null)
        );

        when(mockClient.listComponentsWithMetadata(anyString(), anyBoolean()))
            .thenReturn(mockComponents);

        List<ComponentMetadata> results = service.searchComponents(criteria, false);
        assertEquals(1, results.size(), "Extension with dot should work");
    }

    @Test
    void testEmptyFileExtension() throws Exception {
        SearchCriteria criteria = new SearchCriteria.Builder()
            .repository("test")
            .fileExtension("")
            .build();

        List<ComponentMetadata> mockComponents = List.of(
            new ComponentMetadata("id1", "test.jar", 1000, null, null, null, null, null)
        );

        when(mockClient.listComponentsWithMetadata(anyString(), anyBoolean()))
            .thenReturn(mockComponents);

        List<ComponentMetadata> results = service.searchComponents(criteria, false);
        assertEquals(1, results.size(), "Empty extension should match all");
    }

    @Test
    void testMultipleDotExtension() throws Exception {
        SearchCriteria criteria = new SearchCriteria.Builder()
            .repository("test")
            .fileExtension(".tar.gz")
            .build();

        List<ComponentMetadata> mockComponents = List.of(
            new ComponentMetadata("id1", "archive.tar.gz", 1000, null, null, null, null, null),
            new ComponentMetadata("id2", "test.jar", 2000, null, null, null, null, null)
        );

        when(mockClient.listComponentsWithMetadata(anyString(), anyBoolean()))
            .thenReturn(mockComponents);

        List<ComponentMetadata> results = service.searchComponents(criteria, false);
        assertEquals(1, results.size(), "Multiple dot extension should work");
        assertEquals("archive.tar.gz", results.get(0).path());
    }

    // ========== Statistics Edge Cases ==========

    @Test
    void testStatisticsWithEmptyRepository() {
        List<ComponentMetadata> emptyList = new ArrayList<>();

        RepositoryStats stats = service.calculateStatistics("empty-repo", emptyList, null);

        assertEquals(0, stats.totalComponents());
        assertEquals(0, stats.totalSize());
        assertEquals(0, stats.averageSize());
        assertEquals(0, stats.medianSize());
        assertTrue(stats.largestComponents().isEmpty());
    }

    @Test
    void testStatisticsWithSingleComponent() {
        List<ComponentMetadata> singleComponent = List.of(
            new ComponentMetadata("id1", "test.jar", 1000, null, null, null, null, null)
        );

        RepositoryStats stats = service.calculateStatistics("single-repo", singleComponent, null);

        assertEquals(1, stats.totalComponents());
        assertEquals(1000, stats.totalSize());
        assertEquals(1000, stats.averageSize());
        assertEquals(1000, stats.medianSize());
        assertEquals(1, stats.largestComponents().size());
    }

    @Test
    void testStatisticsWithAllZeroSizes() {
        List<ComponentMetadata> zeroSizes = List.of(
            new ComponentMetadata("id1", "empty1.txt", 0, null, null, null, null, null),
            new ComponentMetadata("id2", "empty2.txt", 0, null, null, null, null, null),
            new ComponentMetadata("id3", "empty3.txt", 0, null, null, null, null, null)
        );

        RepositoryStats stats = service.calculateStatistics("zero-repo", zeroSizes, null);

        assertEquals(3, stats.totalComponents());
        assertEquals(0, stats.totalSize());
        assertEquals(0, stats.averageSize());
        assertEquals(0, stats.medianSize());
    }

    @Test
    void testStatisticsWithVeryLargeSizes() {
        List<ComponentMetadata> largeSizes = List.of(
            new ComponentMetadata("id1", "huge1.jar", Long.MAX_VALUE / 2, null, null, null, null, null),
            new ComponentMetadata("id2", "huge2.jar", Long.MAX_VALUE / 2, null, null, null, null, null)
        );

        assertDoesNotThrow(() -> {
            RepositoryStats stats = service.calculateStatistics("large-repo", largeSizes, null);
            assertTrue(stats.totalSize() > 0, "Should handle very large sizes");
        });
    }

    @Test
    void testStatisticsMedianWithEvenCount() {
        List<ComponentMetadata> evenCount = List.of(
            new ComponentMetadata("id1", "test1.jar", 1000, null, null, null, null, null),
            new ComponentMetadata("id2", "test2.jar", 2000, null, null, null, null, null),
            new ComponentMetadata("id3", "test3.jar", 3000, null, null, null, null, null),
            new ComponentMetadata("id4", "test4.jar", 4000, null, null, null, null, null)
        );

        RepositoryStats stats = service.calculateStatistics("even-repo", evenCount, null);

        assertEquals(2500, stats.medianSize(), "Median of even count should be average of middle two");
    }

    @Test
    void testStatisticsMedianWithOddCount() {
        List<ComponentMetadata> oddCount = List.of(
            new ComponentMetadata("id1", "test1.jar", 1000, null, null, null, null, null),
            new ComponentMetadata("id2", "test2.jar", 2000, null, null, null, null, null),
            new ComponentMetadata("id3", "test3.jar", 3000, null, null, null, null, null)
        );

        RepositoryStats stats = service.calculateStatistics("odd-repo", oddCount, null);

        assertEquals(2000, stats.medianSize(), "Median of odd count should be middle element");
    }

    // ========== Empty List Handling ==========

    @Test
    void testDeleteWithEmptyRepository() throws Exception {
        when(mockClient.listComponents(anyString(), anyBoolean()))
            .thenReturn(List.of());

        service.deleteFromRepository("empty-repo", null, false, null);

        verify(mockClient, never()).deleteComponent(anyString());
    }

    @Test
    void testSearchWithEmptyRepository() throws Exception {
        SearchCriteria criteria = new SearchCriteria.Builder()
            .repository("empty-repo")
            .build();

        when(mockClient.listComponentsWithMetadata(anyString(), anyBoolean()))
            .thenReturn(List.of());

        List<ComponentMetadata> results = service.searchComponents(criteria, false);

        assertTrue(results.isEmpty());
    }
}

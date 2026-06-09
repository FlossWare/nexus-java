package org.flossware.nexus;

import org.flossware.jnexus.RepoRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for NexusService cache status and management methods.
 * <p>
 * Covers scenarios from issue #67:
 * - getCacheStatus for cached vs uncached repos
 * - clearCache and clearAllCache delegation
 * - formatRecordsWithHeaders output format
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NexusService Cache Status and Format Tests")
class NexusServiceCacheStatusTest {

    @Mock
    private NexusClient mockClient;

    private NexusService service;

    @BeforeEach
    void setUp() {
        service = new NexusService(mockClient);
    }

    @Test
    @DisplayName("getCacheStatus should return 'Not cached' when repo is not cached")
    void testGetCacheStatusNotCached() {
        when(mockClient.isCached("test-repo")).thenReturn(false);

        String status = service.getCacheStatus("test-repo");
        assertEquals("Not cached", status);
    }

    @Test
    @DisplayName("getCacheStatus should return age when repo is cached")
    void testGetCacheStatusCached() {
        when(mockClient.isCached("test-repo")).thenReturn(true);
        when(mockClient.getCacheAge("test-repo")).thenReturn(42L);

        String status = service.getCacheStatus("test-repo");
        assertTrue(status.contains("Cached"),
            "Status should say 'Cached'");
        assertTrue(status.contains("42"),
            "Status should contain age in seconds");
    }

    @Test
    @DisplayName("clearCache should delegate to client")
    void testClearCache() {
        service.clearCache("test-repo");
        verify(mockClient).clearCache("test-repo");
    }

    @Test
    @DisplayName("clearAllCache should delegate to client")
    void testClearAllCache() {
        service.clearAllCache();
        verify(mockClient).clearAllCache();
    }

    @Test
    @DisplayName("formatRecordsWithHeaders should produce correctly formatted output")
    void testFormatRecordsWithHeaders() {
        java.util.List<RepoRecord> records = java.util.List.of(
            new RepoRecord("abc123", 1234567, "com/example/artifact-1.0.0.jar"),
            new RepoRecord("def456", 7890123, "com/example/artifact-2.0.0.jar")
        );

        String formatted = service.formatRecordsWithHeaders(records);

        // Should contain headers
        assertTrue(formatted.contains("ID"), "Should contain ID header");
        assertTrue(formatted.contains("File Size"), "Should contain File Size header");
        assertTrue(formatted.contains("Path"), "Should contain Path header");

        // Should contain data
        assertTrue(formatted.contains("abc123"), "Should contain first ID");
        assertTrue(formatted.contains("def456"), "Should contain second ID");
        assertTrue(formatted.contains("com/example/artifact-1.0.0.jar"),
            "Should contain first path");

        // Should contain total row
        assertTrue(formatted.contains("TOTAL: 2 components"),
            "Should contain total count");
        assertTrue(formatted.contains("MB"),
            "Should show total in MB");
    }

    @Test
    @DisplayName("formatRecordsWithHeaders with empty list should not produce total row")
    void testFormatRecordsWithHeadersEmpty() {
        String formatted = service.formatRecordsWithHeaders(java.util.List.of());

        // Should contain headers but no total row
        assertTrue(formatted.contains("ID"), "Should contain headers even for empty list");
        assertFalse(formatted.contains("TOTAL"),
            "Should not contain total row for empty list");
    }

    @Test
    @DisplayName("formatRecordsWithHeaders should use thousand separators for file size")
    void testFormatRecordsWithThousandSeparators() {
        java.util.List<RepoRecord> records = java.util.List.of(
            new RepoRecord("id1", 1234567890, "large-file.jar")
        );

        String formatted = service.formatRecordsWithHeaders(records);

        assertTrue(formatted.contains("1,234,567,890"),
            "File size should have thousand separators");
    }
}

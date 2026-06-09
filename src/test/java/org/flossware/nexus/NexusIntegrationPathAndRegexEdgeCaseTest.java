package org.flossware.nexus;

import org.flossware.jnexus.RepoRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

/**
 * Integration tests for component path and regex edge cases.
 * Tests special characters, very long paths, path traversal, and regex validation.
 */
@DisplayName("Path and Regex Edge Case Tests")
class NexusIntegrationPathAndRegexEdgeCaseTest {

    @Mock
    private NexusClient mockClient;

    private NexusService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new NexusService(mockClient);
    }

    @Test
    @DisplayName("Path with spaces")
    void testPathWithSpaces() throws Exception {
        List<RepoRecord> records = List.of(
            new RepoRecord("id1", 1024, "my file with spaces.jar")
        );
        when(mockClient.listComponents(anyString(), anyBoolean()))
            .thenReturn(records);

        var fetched = service.getRepositoryRecords("test-repo", null, false);
        assertEquals(1, fetched.size());
        assertEquals("my file with spaces.jar", fetched.get(0).path());
    }

    @Test
    @DisplayName("Path with URL-unsafe characters")
    void testPathWithURLUnsafeCharacters() throws Exception {
        List<RepoRecord> records = List.of(
            new RepoRecord("id1", 1024, "file?special&chars=value.jar")
        );
        when(mockClient.listComponents(anyString(), anyBoolean()))
            .thenReturn(records);

        var fetched = service.getRepositoryRecords("test-repo", null, false);
        assertEquals(1, fetched.size());
        assertTrue(fetched.get(0).path().contains("?"));
    }

    @Test
    @DisplayName("Path with Unicode characters")
    void testPathWithUnicodeCharacters() throws Exception {
        List<RepoRecord> records = List.of(
            new RepoRecord("id1", 1024, "文件.jar"),
            new RepoRecord("id2", 2048, "файл.jar"),
            new RepoRecord("id3", 512, "arquivo.jar")
        );
        when(mockClient.listComponents(anyString(), anyBoolean()))
            .thenReturn(records);

        var fetched = service.getRepositoryRecords("test-repo", null, false);
        assertEquals(3, fetched.size());
        assertTrue(fetched.stream().anyMatch(r -> r.path().contains("文件")));
        assertTrue(fetched.stream().anyMatch(r -> r.path().contains("файл")));
    }

    @Test
    @DisplayName("Path with emoji characters")
    void testPathWithEmojiCharacters() throws Exception {
        List<RepoRecord> records = List.of(
            new RepoRecord("id1", 1024, "🚀-rocket.jar"),
            new RepoRecord("id2", 2048, "💎-diamond.jar")
        );
        when(mockClient.listComponents(anyString(), anyBoolean()))
            .thenReturn(records);

        var fetched = service.getRepositoryRecords("test-repo", null, false);
        assertEquals(2, fetched.size());
        assertTrue(fetched.stream().anyMatch(r -> r.path().contains("🚀")));
        assertTrue(fetched.stream().anyMatch(r -> r.path().contains("💎")));
    }

    @Test
    @DisplayName("Very long path (1000+ characters)")
    void testVeryLongPath() throws Exception {
        String longPath = "com/very/deep/package/structure/" + "sub/".repeat(100) + "file.jar";
        List<RepoRecord> records = List.of(
            new RepoRecord("id1", 1024, longPath)
        );
        when(mockClient.listComponents(anyString(), anyBoolean()))
            .thenReturn(records);

        var fetched = service.getRepositoryRecords("test-repo", null, false);
        assertEquals(1, fetched.size());
        assertEquals(longPath, fetched.get(0).path());
    }

    @Test
    @DisplayName("Path with multiple dots")
    void testPathWithMultipleDots() throws Exception {
        List<RepoRecord> records = List.of(
            new RepoRecord("id1", 1024, "file.1.2.3.4.5.jar")
        );
        when(mockClient.listComponents(anyString(), anyBoolean()))
            .thenReturn(records);

        var fetched = service.getRepositoryRecords("test-repo", null, false);
        assertEquals(1, fetched.size());
        assertEquals("file.1.2.3.4.5.jar", fetched.get(0).path());
    }

    @Test
    @DisplayName("Path with dashes and underscores")
    void testPathWithDashesAndUnderscores() throws Exception {
        List<RepoRecord> records = List.of(
            new RepoRecord("id1", 1024, "file-with-dashes_and_underscores.jar")
        );
        when(mockClient.listComponents(anyString(), anyBoolean()))
            .thenReturn(records);

        var fetched = service.getRepositoryRecords("test-repo", null, false);
        assertEquals(1, fetched.size());
        assertEquals("file-with-dashes_and_underscores.jar", fetched.get(0).path());
    }

    @Test
    @DisplayName("Path traversal attempt rejection")
    void testPathTraversalAttemptRejection() throws Exception {
        List<RepoRecord> records = List.of(
            new RepoRecord("id1", 1024, "../../etc/passwd"),
            new RepoRecord("id2", 2048, "..\\..\\windows\\system32\\config\\sam")
        );
        when(mockClient.listComponents(anyString(), anyBoolean()))
            .thenReturn(records);

        // Should handle traversal attempts as regular paths
        var fetched = service.getRepositoryRecords("test-repo", null, false);
        assertEquals(2, fetched.size());
        // These should be treated as literal paths, not actual traversal
    }

    @Test
    @DisplayName("Regex pattern: match by extension")
    void testRegexPatternMatchByExtension() throws Exception {
        List<RepoRecord> records = List.of(
            new RepoRecord("id1", 1024, "file1.jar"),
            new RepoRecord("id2", 2048, "file2.war"),
            new RepoRecord("id3", 512, "file3.jar")
        );
        when(mockClient.listComponents(anyString(), anyBoolean()))
            .thenReturn(records);

        // Delete only .jar files
        service.deleteFromRepository("test-repo", ".*\\.jar$", false, null);

        verify(mockClient).deleteComponent("id1");
        verify(mockClient, never()).deleteComponent("id2");
        verify(mockClient).deleteComponent("id3");
    }

    @Test
    @DisplayName("Regex pattern: complex matching")
    void testRegexPatternComplexMatching() throws Exception {
        List<RepoRecord> records = List.of(
            new RepoRecord("id1", 1024, "v1.0.0-RELEASE.jar"),
            new RepoRecord("id2", 2048, "v1.0.1-SNAPSHOT.jar"),
            new RepoRecord("id3", 512, "v2.0.0-RELEASE.jar")
        );
        when(mockClient.listComponents(anyString(), anyBoolean()))
            .thenReturn(records);

        // Match v1.*.SNAPSHOT
        service.deleteFromRepository("test-repo", "v1\\..*SNAPSHOT.*", false, null);

        verify(mockClient, never()).deleteComponent("id1");
        verify(mockClient).deleteComponent("id2");
        verify(mockClient, never()).deleteComponent("id3");
    }

    @Test
    @DisplayName("Invalid regex pattern")
    void testInvalidRegexPattern() throws Exception {
        List<RepoRecord> records = List.of(
            new RepoRecord("id1", 1024, "file.jar")
        );
        when(mockClient.listComponents(anyString(), anyBoolean()))
            .thenReturn(records);

        assertThrows(IllegalArgumentException.class, () ->
            service.deleteFromRepository("test-repo", "[invalid", false, null),
            "Should reject invalid regex pattern"
        );
    }

    @Test
    @DisplayName("Regex with Unicode characters")
    void testRegexWithUnicodeCharacters() throws Exception {
        List<RepoRecord> records = List.of(
            new RepoRecord("id1", 1024, "文件.jar"),
            new RepoRecord("id2", 2048, "file.jar")
        );
        when(mockClient.listComponents(anyString(), anyBoolean()))
            .thenReturn(records);

        // Match Chinese characters
        service.deleteFromRepository("test-repo", "文.*", false, null);

        verify(mockClient).deleteComponent("id1");
        verify(mockClient, never()).deleteComponent("id2");
    }

    @Test
    @DisplayName("Case-sensitive regex matching")
    void testCaseSensitiveRegexMatching() throws Exception {
        List<RepoRecord> records = List.of(
            new RepoRecord("id1", 1024, "FILE.JAR"),
            new RepoRecord("id2", 2048, "file.jar"),
            new RepoRecord("id3", 512, "File.Jar")
        );
        when(mockClient.listComponents(anyString(), anyBoolean()))
            .thenReturn(records);

        // Match lowercase only
        service.deleteFromRepository("test-repo", "[a-z]+\\.jar", false, null);

        verify(mockClient, never()).deleteComponent("id1");
        verify(mockClient).deleteComponent("id2");
        verify(mockClient, never()).deleteComponent("id3");
    }

    @Test
    @DisplayName("Regex alternation pattern")
    void testRegexAlternationPattern() throws Exception {
        List<RepoRecord> records = List.of(
            new RepoRecord("id1", 1024, "test-1.0.0.jar"),
            new RepoRecord("id2", 2048, "prod-1.0.0.jar"),
            new RepoRecord("id3", 512, "dev-1.0.0.jar")
        );
        when(mockClient.listComponents(anyString(), anyBoolean()))
            .thenReturn(records);

        // Match test or dev
        service.deleteFromRepository("test-repo", "(test|dev)-.*", false, null);

        verify(mockClient).deleteComponent("id1");
        verify(mockClient, never()).deleteComponent("id2");
        verify(mockClient).deleteComponent("id3");
    }

    @Test
    @DisplayName("Null regex pattern matches all")
    void testNullRegexPatternMatchesAll() throws Exception {
        List<RepoRecord> records = List.of(
            new RepoRecord("id1", 1024, "file1.jar"),
            new RepoRecord("id2", 2048, "file2.jar")
        );
        when(mockClient.listComponents(anyString(), anyBoolean()))
            .thenReturn(records);

        // Null regex should match all
        service.deleteFromRepository("test-repo", null, true, null); // dry-run = true

        verify(mockClient).listComponents(anyString(), anyBoolean());
        verify(mockClient, never()).deleteComponent(anyString()); // dry-run, so no delete
    }

    @Test
    @DisplayName("Empty regex pattern")
    void testEmptyRegexPattern() throws Exception {
        List<RepoRecord> records = List.of(
            new RepoRecord("id1", 1024, "file.jar")
        );
        when(mockClient.listComponents(anyString(), anyBoolean()))
            .thenReturn(records);

        // Empty regex matches empty string only
        service.deleteFromRepository("test-repo", "", false, null);

        // Should not delete anything
        verify(mockClient, never()).deleteComponent(anyString());
    }

    @Test
    @DisplayName("Regex with special characters: dot, star, plus")
    void testRegexWithSpecialCharacters() throws Exception {
        List<RepoRecord> records = List.of(
            new RepoRecord("id1", 1024, "file.1.2.3.jar"),
            new RepoRecord("id2", 2048, "file123jar"),
            new RepoRecord("id3", 512, "file...jar")
        );
        when(mockClient.listComponents(anyString(), anyBoolean()))
            .thenReturn(records);

        // Match file.N.N.N.jar (literal dots)
        service.deleteFromRepository("test-repo", "file(\\.[0-9]+)+\\.jar", false, null);

        verify(mockClient).deleteComponent("id1");
        verify(mockClient, never()).deleteComponent("id2");
        verify(mockClient, never()).deleteComponent("id3");
    }
}

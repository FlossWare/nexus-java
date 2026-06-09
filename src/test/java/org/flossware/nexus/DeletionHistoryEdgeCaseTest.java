package org.flossware.nexus;

import org.flossware.jnexus.RepoRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Edge case tests for DeletionHistory.
 * <p>
 * Covers scenarios from issue #67:
 * - Boundary conditions for max history size
 * - Concurrent access to history
 * - Export with special characters
 * - fromRepoRecords conversion
 * - JSON escaping edge cases
 * </p>
 */
@DisplayName("DeletionHistory Edge Case Tests")
class DeletionHistoryEdgeCaseTest {

    private DeletionHistory history;

    @BeforeEach
    void setUp() {
        history = new DeletionHistory();
    }

    @Test
    @DisplayName("Max history size of 1 should only keep most recent")
    void testMaxHistorySizeOne() {
        DeletionHistory smallHistory = new DeletionHistory(1);

        smallHistory.recordDeletion("id-1", "first.jar", 1000, "repo");
        assertEquals(1, smallHistory.size());

        smallHistory.recordDeletion("id-2", "second.jar", 2000, "repo");
        assertEquals(1, smallHistory.size(), "Should still be 1");

        List<DeletionHistory.DeletedComponent> deletions = smallHistory.getAllDeletions();
        assertEquals("second.jar", deletions.get(0).path(),
            "Should keep only the most recent deletion");
    }

    @Test
    @DisplayName("maxHistory of 0 should throw IllegalArgumentException")
    void testMaxHistoryZero() {
        assertThrows(IllegalArgumentException.class, () ->
            new DeletionHistory(0),
            "maxHistory of 0 should be rejected"
        );
    }

    @Test
    @DisplayName("maxHistory of negative should throw IllegalArgumentException")
    void testMaxHistoryNegative() {
        assertThrows(IllegalArgumentException.class, () ->
            new DeletionHistory(-1),
            "Negative maxHistory should be rejected"
        );
    }

    @Test
    @DisplayName("History at max capacity should evict oldest entry")
    void testHistoryEviction() {
        DeletionHistory bounded = new DeletionHistory(3);

        bounded.recordDeletion("id-1", "first.jar", 1000, "repo");
        bounded.recordDeletion("id-2", "second.jar", 2000, "repo");
        bounded.recordDeletion("id-3", "third.jar", 3000, "repo");
        assertEquals(3, bounded.size());

        bounded.recordDeletion("id-4", "fourth.jar", 4000, "repo");
        assertEquals(3, bounded.size(), "Size should not exceed max");

        List<DeletionHistory.DeletedComponent> all = bounded.getAllDeletions();
        // Should be newest first
        assertEquals("fourth.jar", all.get(0).path(), "Newest should be first");
        assertEquals("second.jar", all.get(2).path(), "Oldest remaining should be last");
        assertTrue(all.stream().noneMatch(d -> d.path().equals("first.jar")),
            "First entry should have been evicted");
    }

    @Test
    @DisplayName("getRecentDeletions with limit larger than size should return all")
    void testGetRecentDeletionsLargerLimit() {
        history.recordDeletion("id-1", "a.jar", 1000, "repo");
        history.recordDeletion("id-2", "b.jar", 2000, "repo");

        List<DeletionHistory.DeletedComponent> recent = history.getRecentDeletions(100);
        assertEquals(2, recent.size(), "Should return all when limit > size");
    }

    @Test
    @DisplayName("getRecentDeletions on empty history should return empty list")
    void testGetRecentDeletionsEmpty() {
        List<DeletionHistory.DeletedComponent> recent = history.getRecentDeletions(10);
        assertTrue(recent.isEmpty());
    }

    @Test
    @DisplayName("getDeletionsByRepository should filter correctly")
    void testGetDeletionsByRepository() {
        history.recordDeletion("id-1", "a.jar", 1000, "maven-releases");
        history.recordDeletion("id-2", "b.jar", 2000, "npm-hosted");
        history.recordDeletion("id-3", "c.jar", 3000, "maven-releases");

        List<DeletionHistory.DeletedComponent> mavenDeletions =
            history.getDeletionsByRepository("maven-releases");
        assertEquals(2, mavenDeletions.size());

        List<DeletionHistory.DeletedComponent> npmDeletions =
            history.getDeletionsByRepository("npm-hosted");
        assertEquals(1, npmDeletions.size());

        List<DeletionHistory.DeletedComponent> nonExistent =
            history.getDeletionsByRepository("docker-hosted");
        assertTrue(nonExistent.isEmpty());
    }

    @Test
    @DisplayName("getTotalDeletedSize should sum all file sizes correctly")
    void testGetTotalDeletedSize() {
        assertEquals(0, history.getTotalDeletedSize(), "Empty history should return 0");

        history.recordDeletion("id-1", "a.jar", 1000, "repo");
        history.recordDeletion("id-2", "b.jar", 2000, "repo");
        history.recordDeletion("id-3", "c.jar", 3000, "repo");

        assertEquals(6000, history.getTotalDeletedSize());
    }

    @Test
    @DisplayName("clear should remove all entries")
    void testClear() {
        history.recordDeletion("id-1", "a.jar", 1000, "repo");
        history.recordDeletion("id-2", "b.jar", 2000, "repo");

        history.clear();

        assertTrue(history.isEmpty());
        assertEquals(0, history.size());
        assertEquals(0, history.getTotalDeletedSize());
    }

    @Test
    @DisplayName("exportToJson should create valid JSON file")
    void testExportToJson(@TempDir Path tempDir) throws IOException {
        history.recordDeletion("id-1", "artifact.jar", 1000, "maven-releases");
        history.recordDeletion("id-2", "package.tgz", 2000, "npm-hosted");

        Path outputFile = tempDir.resolve("deletions.json");
        history.exportToJson(outputFile);

        assertTrue(Files.exists(outputFile), "JSON file should be created");
        String content = Files.readString(outputFile);

        assertTrue(content.contains("\"totalComponents\": 2"),
            "Should contain component count");
        assertTrue(content.contains("artifact.jar"),
            "Should contain component path");
        assertTrue(content.contains("npm-hosted"),
            "Should contain repository name");
    }

    @Test
    @DisplayName("toJson should produce valid JSON string")
    void testToJson() {
        history.recordDeletion("id-1", "test.jar", 1234, "repo");

        String json = history.toJson();

        assertTrue(json.contains("\"totalComponents\": 1"));
        assertTrue(json.contains("\"totalSize\": 1234"));
        assertTrue(json.contains("\"path\": \"test.jar\""));
    }

    @Test
    @DisplayName("JSON escaping should handle special characters")
    void testJsonEscaping() {
        // Test with path containing JSON-special characters
        history.recordDeletion("id-1", "path/with\"quotes.jar", 1000, "repo");
        history.recordDeletion("id-2", "path/with\\backslash.jar", 2000, "repo");
        history.recordDeletion("id-3", "path/with\ttab.jar", 3000, "repo");

        String json = history.toJson();

        // Should be valid JSON (escaped properly)
        assertTrue(json.contains("\\\""), "Quotes should be escaped");
        assertTrue(json.contains("\\\\"), "Backslashes should be escaped");
        assertTrue(json.contains("\\t"), "Tabs should be escaped");
    }

    @Test
    @DisplayName("Jackson serialization should handle null repository and regex")
    void testFormatAsJsonWithNullFields() {
        List<DeletionHistory.DeletedComponent> components = List.of(
            new DeletionHistory.DeletedComponent("id-1", "test.jar", 1000, "repo", Instant.now())
        );

        String json = DeletionHistory.formatAsJson(components, null, null);

        assertNotNull(json);
        assertTrue(json.contains("\"exportedAt\""), "Should contain exportedAt");
        assertTrue(json.contains("\"summary\""), "Should contain summary");
        assertTrue(json.contains("\"components\""), "Should contain components");
        // When null, these fields should be absent or have null value in JSON
        assertTrue(json.contains("\"repository\":null") || !json.contains("\"repository\""),
            "Repository should be null or absent");
        assertTrue(json.contains("\"criteria\":null") || !json.contains("\"criteria\""),
            "Criteria should be null or absent");
    }

    @Test
    @DisplayName("fromRepoRecords should convert correctly")
    void testFromRepoRecords() {
        List<RepoRecord> records = List.of(
            new RepoRecord("id-1", 1000, "artifact1.jar"),
            new RepoRecord("id-2", 2000, "artifact2.jar")
        );

        List<DeletionHistory.DeletedComponent> components =
            DeletionHistory.fromRepoRecords(records, "maven-releases");

        assertEquals(2, components.size());
        assertEquals("id-1", components.get(0).id());
        assertEquals("artifact1.jar", components.get(0).path());
        assertEquals(1000, components.get(0).fileSize());
        assertEquals("maven-releases", components.get(0).repository());
        assertNotNull(components.get(0).deletedAt(),
            "Should have a timestamp");
    }

    @Test
    @DisplayName("Concurrent access to history should be thread-safe")
    void testConcurrentAccess() throws Exception {
        int threadCount = 10;
        int operationsPerThread = 100;
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < operationsPerThread; j++) {
                    history.recordDeletion(
                        "id-" + threadId + "-" + j,
                        "artifact-" + threadId + "-" + j + ".jar",
                        j * 100L,
                        "repo-" + threadId
                    );
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join(5000);
        }

        // With 10 threads * 100 ops = 1000 records, but max is 1000
        // All should be recorded since default max is 1000
        assertEquals(1000, history.size(),
            "Should have recorded all 1000 deletions");
    }

    @Test
    @DisplayName("formatAsJson with repository and regex should include metadata")
    void testFormatAsJsonWithMetadata() {
        List<DeletionHistory.DeletedComponent> components = List.of(
            DeletionHistory.fromRepoRecords(
                List.of(new RepoRecord("id-1", 1000, "test.jar")),
                "maven-releases"
            ).get(0)
        );

        String json = DeletionHistory.formatAsJson(components, "maven-releases", ".*SNAPSHOT.*");

        assertTrue(json.contains("\"repository\": \"maven-releases\""),
            "Should include repository");
        assertTrue(json.contains("\"regex\": \".*SNAPSHOT.*\""),
            "Should include regex criteria");
    }
}

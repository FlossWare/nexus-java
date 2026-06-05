package org.flossware.nexus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the DeletionHistory class.
 */
class DeletionHistoryTest {

    private DeletionHistory history;

    @BeforeEach
    void setUp() {
        history = new DeletionHistory();
    }

    @Test
    void testNewHistoryIsEmpty() {
        assertTrue(history.isEmpty());
        assertEquals(0, history.size());
    }

    @Test
    void testRecordDeletion() {
        history.recordDeletion("id1", "path/artifact1.jar", 1000, "maven-releases");

        assertFalse(history.isEmpty());
        assertEquals(1, history.size());
    }

    @Test
    void testRecordMultipleDeletions() {
        history.recordDeletion("id1", "path/artifact1.jar", 1000, "maven-releases");
        history.recordDeletion("id2", "path/artifact2.jar", 2000, "maven-releases");
        history.recordDeletion("id3", "path/artifact3.jar", 3000, "maven-snapshots");

        assertEquals(3, history.size());
    }

    @Test
    void testGetRecentDeletionsOrderedNewestFirst() throws InterruptedException {
        history.recordDeletion("id1", "path/first.jar", 1000, "repo1");
        // Small delay to ensure distinct timestamps
        Thread.sleep(10);
        history.recordDeletion("id2", "path/second.jar", 2000, "repo1");

        List<DeletionHistory.DeletedComponent> recent = history.getRecentDeletions(10);

        assertEquals(2, recent.size());
        assertEquals("id2", recent.get(0).id()); // Most recent first
        assertEquals("id1", recent.get(1).id());
    }

    @Test
    void testGetRecentDeletionsWithLimit() {
        history.recordDeletion("id1", "path/artifact1.jar", 1000, "repo1");
        history.recordDeletion("id2", "path/artifact2.jar", 2000, "repo1");
        history.recordDeletion("id3", "path/artifact3.jar", 3000, "repo1");

        List<DeletionHistory.DeletedComponent> recent = history.getRecentDeletions(2);

        assertEquals(2, recent.size());
    }

    @Test
    void testGetAllDeletions() {
        history.recordDeletion("id1", "path/artifact1.jar", 1000, "repo1");
        history.recordDeletion("id2", "path/artifact2.jar", 2000, "repo1");

        List<DeletionHistory.DeletedComponent> all = history.getAllDeletions();

        assertEquals(2, all.size());
    }

    @Test
    void testMaxHistoryEnforced() {
        DeletionHistory smallHistory = new DeletionHistory(3);

        smallHistory.recordDeletion("id1", "path1.jar", 100, "repo");
        smallHistory.recordDeletion("id2", "path2.jar", 200, "repo");
        smallHistory.recordDeletion("id3", "path3.jar", 300, "repo");
        smallHistory.recordDeletion("id4", "path4.jar", 400, "repo"); // Should evict id1

        assertEquals(3, smallHistory.size());

        List<DeletionHistory.DeletedComponent> all = smallHistory.getAllDeletions();
        // id1 should have been evicted (oldest)
        assertTrue(all.stream().noneMatch(d -> d.id().equals("id1")));
        // id2, id3, id4 should remain
        assertTrue(all.stream().anyMatch(d -> d.id().equals("id2")));
        assertTrue(all.stream().anyMatch(d -> d.id().equals("id3")));
        assertTrue(all.stream().anyMatch(d -> d.id().equals("id4")));
    }

    @Test
    void testMaxHistoryMinimumValue() {
        assertThrows(IllegalArgumentException.class, () -> new DeletionHistory(0));
        assertThrows(IllegalArgumentException.class, () -> new DeletionHistory(-1));
    }

    @Test
    void testClear() {
        history.recordDeletion("id1", "path1.jar", 1000, "repo");
        history.recordDeletion("id2", "path2.jar", 2000, "repo");

        assertFalse(history.isEmpty());

        history.clear();

        assertTrue(history.isEmpty());
        assertEquals(0, history.size());
    }

    @Test
    void testGetTotalDeletedSize() {
        history.recordDeletion("id1", "path1.jar", 1000, "repo");
        history.recordDeletion("id2", "path2.jar", 2500, "repo");
        history.recordDeletion("id3", "path3.jar", 500, "repo");

        assertEquals(4000, history.getTotalDeletedSize());
    }

    @Test
    void testGetTotalDeletedSizeEmpty() {
        assertEquals(0, history.getTotalDeletedSize());
    }

    @Test
    void testGetDeletionsByRepository() {
        history.recordDeletion("id1", "path1.jar", 1000, "repo-a");
        history.recordDeletion("id2", "path2.jar", 2000, "repo-b");
        history.recordDeletion("id3", "path3.jar", 3000, "repo-a");
        history.recordDeletion("id4", "path4.jar", 4000, "repo-b");
        history.recordDeletion("id5", "path5.jar", 5000, "repo-a");

        List<DeletionHistory.DeletedComponent> repoADeletions = history.getDeletionsByRepository("repo-a");
        List<DeletionHistory.DeletedComponent> repoBDeletions = history.getDeletionsByRepository("repo-b");

        assertEquals(3, repoADeletions.size());
        assertEquals(2, repoBDeletions.size());
        assertTrue(repoADeletions.stream().allMatch(d -> d.repository().equals("repo-a")));
        assertTrue(repoBDeletions.stream().allMatch(d -> d.repository().equals("repo-b")));
    }

    @Test
    void testGetDeletionsByRepositoryNotFound() {
        history.recordDeletion("id1", "path1.jar", 1000, "repo-a");

        List<DeletionHistory.DeletedComponent> result = history.getDeletionsByRepository("nonexistent");

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetMaxHistory() {
        assertEquals(DeletionHistory.DEFAULT_MAX_HISTORY, history.getMaxHistory());
        assertEquals(50, new DeletionHistory(50).getMaxHistory());
    }

    @Test
    void testDeletedComponentRecord() {
        Instant now = Instant.now();
        DeletionHistory.DeletedComponent comp = new DeletionHistory.DeletedComponent(
            "test-id", "com/example/test.jar", 12345, "maven-releases", now
        );

        assertEquals("test-id", comp.id());
        assertEquals("com/example/test.jar", comp.path());
        assertEquals(12345, comp.fileSize());
        assertEquals("maven-releases", comp.repository());
        assertEquals(now, comp.deletedAt());
    }

    @Test
    void testExportToJson(@TempDir Path tempDir) throws IOException {
        history.recordDeletion("id1", "com/example/artifact-1.0.jar", 12345, "maven-releases");
        history.recordDeletion("id2", "org/test/library-2.0.jar", 67890, "maven-snapshots");

        Path exportPath = tempDir.resolve("test-export.json");
        history.exportToJson(exportPath);

        assertTrue(Files.exists(exportPath));
        String content = Files.readString(exportPath);

        // Verify JSON structure
        assertTrue(content.contains("\"exportedAt\""));
        assertTrue(content.contains("\"summary\""));
        assertTrue(content.contains("\"totalComponents\": 2"));
        assertTrue(content.contains("\"components\""));
        assertTrue(content.contains("\"id1\""));
        assertTrue(content.contains("\"id2\""));
        assertTrue(content.contains("com/example/artifact-1.0.jar"));
        assertTrue(content.contains("org/test/library-2.0.jar"));
        assertTrue(content.contains("maven-releases"));
        assertTrue(content.contains("maven-snapshots"));
    }

    @Test
    void testExportToJsonCreatesParentDirectories(@TempDir Path tempDir) throws IOException {
        history.recordDeletion("id1", "path.jar", 100, "repo");

        Path exportPath = tempDir.resolve("subdir").resolve("deep").resolve("export.json");
        history.exportToJson(exportPath);

        assertTrue(Files.exists(exportPath));
    }

    @Test
    void testToJson() {
        history.recordDeletion("id1", "path/artifact.jar", 5000, "repo");

        String json = history.toJson();

        assertNotNull(json);
        assertTrue(json.contains("\"exportedAt\""));
        assertTrue(json.contains("\"totalComponents\": 1"));
        assertTrue(json.contains("\"id1\""));
        assertTrue(json.contains("path/artifact.jar"));
    }

    @Test
    void testToJsonEmpty() {
        String json = history.toJson();

        assertNotNull(json);
        assertTrue(json.contains("\"totalComponents\": 0"));
        assertTrue(json.contains("\"components\": ["));
    }

    @Test
    void testFormatAsJsonWithCriteria() {
        List<DeletionHistory.DeletedComponent> components = List.of(
            new DeletionHistory.DeletedComponent("id1", "path1.jar", 1000, "repo", Instant.now())
        );

        String json = DeletionHistory.formatAsJson(components, "maven-releases", ".*SNAPSHOT.*");

        assertTrue(json.contains("\"repository\": \"maven-releases\""));
        assertTrue(json.contains("\"criteria\""));
        assertTrue(json.contains("\"regex\": \".*SNAPSHOT.*\""));
    }

    @Test
    void testFormatAsJsonWithNullCriteria() {
        List<DeletionHistory.DeletedComponent> components = List.of(
            new DeletionHistory.DeletedComponent("id1", "path1.jar", 1000, "repo", Instant.now())
        );

        String json = DeletionHistory.formatAsJson(components, null, null);

        assertFalse(json.contains("\"repository\""));
        assertFalse(json.contains("\"criteria\""));
    }

    @Test
    void testFromRepoRecords() {
        List<RepoRecord> records = List.of(
            new RepoRecord("id1", 1000, "path1.jar"),
            new RepoRecord("id2", 2000, "path2.jar")
        );

        List<DeletionHistory.DeletedComponent> components =
            DeletionHistory.fromRepoRecords(records, "maven-releases");

        assertEquals(2, components.size());
        assertEquals("id1", components.get(0).id());
        assertEquals("path1.jar", components.get(0).path());
        assertEquals(1000, components.get(0).fileSize());
        assertEquals("maven-releases", components.get(0).repository());
        assertNotNull(components.get(0).deletedAt());
    }

    @Test
    void testEscapeJson() {
        assertEquals("", DeletionHistory.escapeJson(null));
        assertEquals("simple", DeletionHistory.escapeJson("simple"));
        assertEquals("with \\\"quotes\\\"", DeletionHistory.escapeJson("with \"quotes\""));
        assertEquals("back\\\\slash", DeletionHistory.escapeJson("back\\slash"));
        assertEquals("new\\nline", DeletionHistory.escapeJson("new\nline"));
        assertEquals("tab\\there", DeletionHistory.escapeJson("tab\there"));
        assertEquals("carriage\\rreturn", DeletionHistory.escapeJson("carriage\rreturn"));
    }

    @Test
    void testDefaultMaxHistory() {
        assertEquals(1000, DeletionHistory.DEFAULT_MAX_HISTORY);
    }

    @Test
    void testThreadSafety() throws InterruptedException {
        DeletionHistory concurrentHistory = new DeletionHistory(500);

        // Run multiple threads adding records concurrently
        Thread[] threads = new Thread[10];
        for (int t = 0; t < threads.length; t++) {
            final int threadId = t;
            threads[t] = new Thread(() -> {
                for (int i = 0; i < 50; i++) {
                    concurrentHistory.recordDeletion(
                        "t" + threadId + "-id" + i,
                        "path/artifact" + i + ".jar",
                        1000 + i,
                        "repo-" + threadId
                    );
                }
            });
        }

        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }

        // All 500 records should be present (10 threads x 50 records each = 500, max is 500)
        assertEquals(500, concurrentHistory.size());
    }
}

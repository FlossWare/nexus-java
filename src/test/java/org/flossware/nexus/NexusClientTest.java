package org.flossware.nexus;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NexusClientTest {

    @Test
    void testCredentialsRequired() {
        assertThrows(IllegalStateException.class, () -> {
            new Credentials();
        }, "Should throw when credentials are not configured");
    }

    @Test
    void testRepoRecordCreation() {
        RepoRecord record = new RepoRecord("test-id", 12345, "path/to/artifact.jar");

        assertEquals("test-id", record.id());
        assertEquals(12345, record.fileSize());
        assertEquals("path/to/artifact.jar", record.path());
    }

    @Test
    void testRepoRecordEquality() {
        RepoRecord record1 = new RepoRecord("id1", 100, "path1");
        RepoRecord record2 = new RepoRecord("id1", 100, "path1");
        RepoRecord record3 = new RepoRecord("id2", 200, "path2");

        assertEquals(record1, record2);
        assertNotEquals(record1, record3);
    }

    @Test
    void testRepoRecordWithZeroFileSize() {
        RepoRecord record = new RepoRecord("empty-id", 0, "empty.jar");

        assertEquals("empty-id", record.id());
        assertEquals(0, record.fileSize());
        assertEquals("empty.jar", record.path());
    }

    @Test
    void testRepoRecordWithLargeFileSize() {
        long largeSize = 1024L * 1024L * 1024L * 5L; // 5 GB
        RepoRecord record = new RepoRecord("large-id", (int) Math.min(largeSize, Integer.MAX_VALUE), "large.jar");

        assertNotNull(record);
        assertEquals("large-id", record.id());
    }

    @Test
    void testRepoRecordHashCode() {
        RepoRecord record1 = new RepoRecord("id1", 100, "path1");
        RepoRecord record2 = new RepoRecord("id1", 100, "path1");
        RepoRecord record3 = new RepoRecord("id2", 200, "path2");

        assertEquals(record1.hashCode(), record2.hashCode());
        assertNotEquals(record1.hashCode(), record3.hashCode());
    }

    @Test
    void testRepoRecordToString() {
        RepoRecord record = new RepoRecord("test-id", 12345, "path/to/artifact.jar");
        String toString = record.toString();

        assertNotNull(toString);
        assertTrue(toString.contains("test-id"));
        assertTrue(toString.contains("12345"));
        assertTrue(toString.contains("path/to/artifact.jar"));
    }

    @Test
    void testRepoRecordWithComplexPath() {
        String complexPath = "com/example/group/artifact/1.0.0-SNAPSHOT/artifact-1.0.0-20230101.123456-1.jar";
        RepoRecord record = new RepoRecord("complex-id", 54321, complexPath);

        assertEquals(complexPath, record.path());
    }

    @Test
    void testRepoRecordWithSpecialCharactersInId() {
        String specialId = "component-id-with-dashes_and_underscores.and.dots";
        RepoRecord record = new RepoRecord(specialId, 1000, "path.jar");

        assertEquals(specialId, record.id());
    }
}

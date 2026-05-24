package org.flossware.jnexus;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NexusClientTest {

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

    @Test
    void testComponentMetadataToRepoRecordConversion() {
        ComponentMetadata metadata = new ComponentMetadata(
            "test-id",
            "path/to/file.jar",
            12345L,
            "application/java-archive",
            "maven2",
            null,
            null,
            null
        );

        RepoRecord record = metadata.toRepoRecord();

        assertEquals(metadata.id(), record.id());
        assertEquals(metadata.fileSize(), record.fileSize());
        assertEquals(metadata.path(), record.path());
    }

    @Test
    void testComponentMetadataPreservesAllFields() {
        java.time.Instant created = java.time.Instant.parse("2024-01-15T10:30:00Z");
        java.time.Instant modified = java.time.Instant.parse("2024-01-16T14:20:00Z");

        ComponentMetadata metadata = new ComponentMetadata(
            "abc123",
            "com/example/artifact-1.0.0.jar",
            1234567L,
            "application/java-archive",
            "maven2",
            created,
            modified,
            "sha1:abcdef123456"
        );

        assertEquals("abc123", metadata.id());
        assertEquals("com/example/artifact-1.0.0.jar", metadata.path());
        assertEquals(1234567L, metadata.fileSize());
        assertEquals("application/java-archive", metadata.contentType());
        assertEquals("maven2", metadata.format());
        assertEquals(created, metadata.createdDate());
        assertEquals(modified, metadata.lastModified());
        assertEquals("sha1:abcdef123456", metadata.checksum());
    }

    @Test
    void testComponentMetadataWithNullOptionalFields() {
        ComponentMetadata metadata = new ComponentMetadata(
            "id1",
            "path.jar",
            1000L,
            null,
            null,
            null,
            null,
            null
        );

        assertNotNull(metadata);
        assertEquals("id1", metadata.id());
        assertNull(metadata.contentType());
        assertNull(metadata.format());
        assertNull(metadata.createdDate());
        assertNull(metadata.lastModified());
        assertNull(metadata.checksum());
    }
}

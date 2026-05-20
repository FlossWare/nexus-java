package org.flossware.jnexus;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ComponentMetadataTest {

    @Test
    void testComponentMetadataCreation() {
        Instant created = Instant.parse("2024-01-15T10:30:00Z");
        Instant modified = Instant.parse("2024-01-16T14:20:00Z");

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
    void testComponentMetadataWithNullFields() {
        ComponentMetadata metadata = new ComponentMetadata(
            "id1",
            "path/to/file.jar",
            1000L,
            null,
            null,
            null,
            null,
            null
        );

        assertEquals("id1", metadata.id());
        assertEquals("path/to/file.jar", metadata.path());
        assertEquals(1000L, metadata.fileSize());
        assertNull(metadata.contentType());
        assertNull(metadata.format());
        assertNull(metadata.createdDate());
        assertNull(metadata.lastModified());
        assertNull(metadata.checksum());
    }

    @Test
    void testToRepoRecord() {
        Instant created = Instant.parse("2024-01-15T10:30:00Z");
        ComponentMetadata metadata = new ComponentMetadata(
            "abc123",
            "com/example/artifact-1.0.0.jar",
            1234567L,
            "application/java-archive",
            "maven2",
            created,
            created,
            "sha1:abcdef"
        );

        RepoRecord record = metadata.toRepoRecord();

        assertEquals("abc123", record.id());
        assertEquals(1234567L, record.fileSize());
        assertEquals("com/example/artifact-1.0.0.jar", record.path());
    }

    @Test
    void testToRepoRecordPreservesData() {
        ComponentMetadata metadata = new ComponentMetadata(
            "test-id",
            "test-path.jar",
            999L,
            "application/octet-stream",
            "raw",
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
    void testComponentMetadataEquality() {
        Instant created = Instant.parse("2024-01-15T10:30:00Z");

        ComponentMetadata metadata1 = new ComponentMetadata(
            "id1", "path1", 1000L, "type1", "format1", created, created, "checksum1"
        );

        ComponentMetadata metadata2 = new ComponentMetadata(
            "id1", "path1", 1000L, "type1", "format1", created, created, "checksum1"
        );

        assertEquals(metadata1, metadata2);
        assertEquals(metadata1.hashCode(), metadata2.hashCode());
    }

    @Test
    void testComponentMetadataInequality() {
        Instant created = Instant.parse("2024-01-15T10:30:00Z");

        ComponentMetadata metadata1 = new ComponentMetadata(
            "id1", "path1", 1000L, "type1", "format1", created, created, "checksum1"
        );

        ComponentMetadata metadata2 = new ComponentMetadata(
            "id2", "path2", 2000L, "type2", "format2", created, created, "checksum2"
        );

        assertNotEquals(metadata1, metadata2);
    }

    @Test
    void testComponentMetadataToString() {
        Instant created = Instant.parse("2024-01-15T10:30:00Z");

        ComponentMetadata metadata = new ComponentMetadata(
            "abc123",
            "com/example/artifact.jar",
            1234567L,
            "application/java-archive",
            "maven2",
            created,
            created,
            "sha1:abcdef"
        );

        String str = metadata.toString();
        assertTrue(str.contains("abc123"));
        assertTrue(str.contains("com/example/artifact.jar"));
        assertTrue(str.contains("1234567"));
        assertTrue(str.contains("application/java-archive"));
        assertTrue(str.contains("maven2"));
    }

    @Test
    void testComponentMetadataWithLargeFileSize() {
        long largeSize = Long.MAX_VALUE;
        ComponentMetadata metadata = new ComponentMetadata(
            "large-id",
            "large-file.bin",
            largeSize,
            "application/octet-stream",
            "raw",
            null,
            null,
            null
        );

        assertEquals(largeSize, metadata.fileSize());
    }

    @Test
    void testComponentMetadataWithZeroFileSize() {
        ComponentMetadata metadata = new ComponentMetadata(
            "empty-id",
            "empty-file.txt",
            0L,
            "text/plain",
            "raw",
            null,
            null,
            null
        );

        assertEquals(0L, metadata.fileSize());
    }
}

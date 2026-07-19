package org.flossware.jnexus;

import java.time.Instant;

/**
 * Represents detailed metadata for a repository component from Nexus.
 * <p>
 * This record extends beyond basic component information to include
 * metadata such as creation dates, content types, and checksums.
 * This enriched data enables advanced filtering, auditing, and analysis.
 * </p>
 *
 * @param id           the unique identifier of the component in Nexus
 * @param path         the full path to the component artifact
 * @param fileSize     the size of the component file in bytes
 * @param contentType  the MIME type of the component (e.g., "application/java-archive")
 * @param format       the repository format (e.g., "maven2", "npm", "docker")
 * @param createdDate  the timestamp when the component was uploaded to Nexus (null if not available)
 * @param lastModified the timestamp when the component was last modified (null if not available)
 * @param checksum     the primary checksum of the component (SHA1 or MD5, null if not available)
 * @author sfloess
 * @since 1.0
 */
public record ComponentMetadata(
    String id,
    String path,
    long fileSize,
    String contentType,
    String format,
    Instant createdDate,
    Instant lastModified,
    String checksum
) {
    /**
     * Converts this metadata record to a simple RepoRecord.
     * <p>
     * Useful for backward compatibility when only basic information is needed.
     * </p>
     *
     * @return a RepoRecord containing id, fileSize, and path
     */
    public RepoRecord toRepoRecord() {
        return new RepoRecord(id, fileSize, path);
    }
}

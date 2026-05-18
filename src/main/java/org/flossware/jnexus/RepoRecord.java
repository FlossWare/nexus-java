package org.flossware.jnexus;

/**
 * Represents a repository component record from Nexus.
 * <p>
 * This record encapsulates the essential information about a component stored
 * in a Nexus repository, including its unique identifier, file size, and path.
 * </p>
 *
 * @param id       the unique identifier of the component in Nexus
 * @param fileSize the size of the component file in bytes (supports files up to ~8 exabytes)
 * @param path     the full path to the component artifact
 * @author sfloess
 * @since 1.0
 */
public record RepoRecord(String id, long fileSize, String path) {

}

package org.flossware.jnexus;

import java.io.IOException;
import java.util.List;

/**
 * Platform-agnostic HTTP client interface for Nexus API.
 * <p>
 * This interface abstracts the HTTP communication layer to support multiple
 * platform implementations:
 * - Desktop: NexusClientHttp (uses java.net.http.HttpClient)
 * - Android: NexusClientOkHttp (uses OkHttp)
 * </p>
 */
public interface NexusHttpClient {

    /**
     * Lists components in a repository.
     *
     * @param repository the repository name
     * @param forceRefresh if true, bypasses cache and fetches fresh data
     * @return list of repository records
     * @throws IOException if HTTP communication fails
     * @throws InterruptedException if the operation is interrupted
     */
    List<RepoRecord> listComponents(String repository, boolean forceRefresh)
            throws IOException, InterruptedException;

    /**
     * Lists components with full metadata.
     *
     * @param repository the repository name
     * @param forceRefresh if true, bypasses cache and fetches fresh data
     * @return list of component metadata
     * @throws IOException if HTTP communication fails
     * @throws InterruptedException if the operation is interrupted
     */
    List<ComponentMetadata> listComponentsWithMetadata(String repository, boolean forceRefresh)
            throws IOException, InterruptedException;

    /**
     * Deletes a component by ID.
     *
     * @param componentId the component ID to delete
     * @throws IOException if HTTP communication fails
     */
    void deleteComponent(String componentId) throws IOException;

    /**
     * Clears cache for a specific repository.
     *
     * @param repository the repository name
     */
    void clearCache(String repository);

    /**
     * Clears all cached data.
     */
    void clearAllCache();
}

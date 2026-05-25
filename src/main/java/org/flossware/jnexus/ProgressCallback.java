package org.flossware.jnexus;

/**
 * Callback interface for tracking progress of long-running Nexus operations.
 * <p>
 * Implement this interface to receive progress notifications during list, delete,
 * and statistics operations. This is useful for:
 * </p>
 * <ul>
 *   <li>Displaying progress bars in GUIs</li>
 *   <li>Logging progress to files or monitoring systems</li>
 *   <li>Tracking operation metrics for analytics</li>
 *   <li>Providing user feedback in long-running batch operations</li>
 * </ul>
 *
 * <h2>Usage Examples:</h2>
 * <pre>
 * // Simple console progress tracker
 * ProgressCallback consoleCallback = new ProgressCallback() {
 *     {@literal @}Override
 *     public void onFetchProgress(String repository, int componentsFetched) {
 *         System.out.println("Fetched " + componentsFetched + " components from " + repository);
 *     }
 *
 *     {@literal @}Override
 *     public void onDeleteProgress(int deleted, int total, double percentage) {
 *         System.out.printf("Deleted %d of %d (%.1f%%)%n", deleted, total, percentage);
 *     }
 *
 *     {@literal @}Override
 *     public void onDeleteComplete(int deleted, int failed) {
 *         System.out.println("Deletion complete: " + deleted + " deleted, " + failed + " failed");
 *     }
 * };
 *
 * // GUI progress bar
 * ProgressCallback guiCallback = new ProgressCallback() {
 *     {@literal @}Override
 *     public void onDeleteProgress(int deleted, int total, double percentage) {
 *         SwingUtilities.invokeLater(() -> {
 *             progressBar.setValue((int) percentage);
 *             statusLabel.setText(deleted + " of " + total);
 *         });
 *     }
 * };
 *
 * // Use with NexusService
 * NexusService service = new NexusService(client);
 * service.deleteFromRepository("maven-snapshots", ".*old.*", false, consoleCallback);
 * </pre>
 *
 * <h2>Thread Safety:</h2>
 * <p>
 * Callback methods may be called from different threads. Implementations should
 * be thread-safe if they update shared state (e.g., GUI components, shared counters).
 * </p>
 *
 * <h2>Exception Handling:</h2>
 * <p>
 * Exceptions thrown by callback methods are logged but do not stop the operation.
 * The operation will continue even if a callback fails.
 * </p>
 *
 * @author sfloess
 * @since 1.0
 * @see NexusService
 */
public interface ProgressCallback {

    /**
     * Called when components are fetched during pagination.
     * <p>
     * This method is called after each page of results is retrieved from the Nexus API.
     * It allows tracking progress of large list operations that span multiple pages.
     * </p>
     *
     * @param repository the name of the repository being fetched
     * @param componentsFetched the total number of components fetched so far
     */
    default void onFetchProgress(String repository, int componentsFetched) {
        // Default: no-op
    }

    /**
     * Called periodically during delete operations to report progress.
     * <p>
     * This method is called after every 5 deletions (when total &gt; 10) to provide
     * regular progress updates without overwhelming the callback with events.
     * </p>
     *
     * @param deleted the number of components successfully deleted so far
     * @param total the total number of components to delete
     * @param percentage the completion percentage (0.0 to 100.0)
     */
    default void onDeleteProgress(int deleted, int total, double percentage) {
        // Default: no-op
    }

    /**
     * Called when a single component is successfully deleted.
     * <p>
     * This provides fine-grained tracking of each deletion. For high-frequency
     * updates, consider using {@link #onDeleteProgress} instead.
     * </p>
     *
     * @param componentId the ID of the deleted component
     * @param path the path of the deleted component
     */
    default void onComponentDeleted(String componentId, String path) {
        // Default: no-op
    }

    /**
     * Called when a component deletion fails.
     * <p>
     * This allows tracking individual failures during batch delete operations.
     * The operation will continue with remaining components even after a failure.
     * </p>
     *
     * @param componentId the ID of the component that failed to delete
     * @param path the path of the component that failed to delete
     * @param error the error message describing the failure
     */
    default void onComponentDeleteFailed(String componentId, String path, String error) {
        // Default: no-op
    }

    /**
     * Called when a delete operation completes.
     * <p>
     * This provides final summary statistics for the entire delete operation.
     * </p>
     *
     * @param deleted the total number of components successfully deleted
     * @param failed the total number of components that failed to delete
     */
    default void onDeleteComplete(int deleted, int failed) {
        // Default: no-op
    }

    /**
     * Called when statistics calculation starts.
     * <p>
     * This indicates the beginning of a potentially long-running statistics operation.
     * </p>
     *
     * @param repository the name of the repository being analyzed
     * @param totalComponents the total number of components to analyze
     */
    default void onStatisticsStart(String repository, int totalComponents) {
        // Default: no-op
    }

    /**
     * Called when statistics calculation completes.
     * <p>
     * This indicates the end of the statistics operation.
     * </p>
     *
     * @param repository the name of the repository analyzed
     * @param stats the calculated statistics
     */
    default void onStatisticsComplete(String repository, RepositoryStats stats) {
        // Default: no-op
    }

    /**
     * Called when an operation encounters an error.
     * <p>
     * This is called for errors that don't stop the entire operation
     * (e.g., individual delete failures, pagination errors).
     * </p>
     *
     * @param operation the name of the operation (e.g., "delete", "fetch")
     * @param error the error message
     */
    default void onError(String operation, String error) {
        // Default: no-op
    }
}

package org.flossware.jnexus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Service layer for Nexus repository operations.
 * <p>
 * This class provides high-level business logic for listing and deleting components
 * from Nexus repositories. It delegates HTTP communication to {@link NexusClient}
 * and handles filtering, output formatting, and statistics.
 * </p>
 *
 * @author sfloess
 * @since 1.0
 */
public class NexusService {
    private static final Logger logger = LoggerFactory.getLogger(NexusService.class);

    private final NexusClient client;

    /**
     * Constructs a new NexusService with the specified client.
     *
     * @param client the NexusClient to use for HTTP operations
     */
    public NexusService(NexusClient client) {
        this.client = client;
    }

    /**
     * Validates a regex pattern.
     *
     * @param regex the regex pattern to validate
     * @throws IllegalArgumentException if the pattern is invalid
     */
    private void validateRegex(String regex) {
        if (regex == null || regex.isEmpty()) {
            return; // null or empty regex is valid (means no filtering)
        }
        try {
            Pattern.compile(regex);
        } catch (PatternSyntaxException e) {
            throw new IllegalArgumentException("Invalid regex pattern: " + e.getMessage(), e);
        }
    }

    /**
     * Lists components in a repository, optionally filtered by a regex pattern.
     * <p>
     * Fetches all components from the repository and displays them to stdout.
     * If a regex filter is provided, only matching components are displayed,
     * but statistics include the total count of all components.
     * Uses cached results if available.
     * </p>
     *
     * @param repository  the name of the repository to list from
     * @param regexFilter optional regex pattern to filter component paths (null for no filter)
     * @throws IOException          if an HTTP error occurs
     * @throws InterruptedException if the operation is interrupted
     */
    public void listRepository(String repository, String regexFilter) throws IOException, InterruptedException {
        listRepository(repository, regexFilter, false);
    }

    /**
     * Lists components in a repository with optional cache refresh.
     * <p>
     * Fetches all components from the repository and displays them to stdout.
     * If a regex filter is provided, only matching components are displayed,
     * but statistics include the total count of all components.
     * </p>
     *
     * @param repository  the name of the repository to list from
     * @param regexFilter optional regex pattern to filter component paths (null for no filter)
     * @param forceRefresh if true, bypasses cache and fetches fresh data
     * @throws IOException          if an HTTP error occurs
     * @throws InterruptedException if the operation is interrupted
     */
    public void listRepository(String repository, String regexFilter, boolean forceRefresh) throws IOException, InterruptedException {
        validateRegex(regexFilter);

        List<RepoRecord> allRecords = client.listComponents(repository, forceRefresh);

        List<RepoRecord> filteredRecords = regexFilter == null
            ? allRecords
            : allRecords.stream()
                .filter(record -> record.path().matches(regexFilter))
                .toList();

        printRecords(filteredRecords);
        printStatistics(allRecords.size(), filteredRecords);
    }

    /**
     * Deletes components from a repository, optionally filtered by a regex pattern.
     * <p>
     * Fetches all components from the repository and deletes those that match the filter
     * (or all if no filter is specified). In dry-run mode, components that would be
     * deleted are displayed but no actual deletion occurs.
     * Always fetches fresh data (bypasses cache) since repository state may have changed.
     * </p>
     * <p>
     * If deletion of individual components fails, the error is logged to stderr and
     * processing continues with remaining components.
     * </p>
     *
     * @param repository  the name of the repository to delete from
     * @param regexFilter optional regex pattern to filter component paths (null for all components)
     * @param dryRun      if true, shows what would be deleted without actually deleting
     * @throws IOException          if an HTTP error occurs
     * @throws InterruptedException if the operation is interrupted
     */
    public void deleteFromRepository(String repository, String regexFilter, boolean dryRun)
            throws IOException, InterruptedException {
        validateRegex(regexFilter);

        // Always refresh for delete operations to get current state
        List<RepoRecord> allRecords = client.listComponents(repository, true);

        List<RepoRecord> recordsToDelete = regexFilter == null
            ? allRecords
            : allRecords.stream()
                .filter(record -> record.path().matches(regexFilter))
                .toList();

        if (recordsToDelete.isEmpty()) {
            System.out.println("No components match the criteria.");
            return;
        }

        System.out.println(dryRun ? "Would delete:" : "Deleting:");
        printRecords(recordsToDelete);

        if (!dryRun) {
            int deleted = 0;
            int total = recordsToDelete.size();
            boolean showProgress = total > 10; // Show progress for >10 components

            for (RepoRecord record : recordsToDelete) {
                try {
                    client.deleteComponent(record.id());
                    deleted++;
                    logger.info("Deleted: {}", record.path());

                    if (showProgress && deleted % 5 == 0) {
                        System.out.printf("Progress: %d of %d components deleted (%.1f%%)%n",
                            deleted, total, (deleted * 100.0 / total));
                    }
                } catch (IOException e) {
                    logger.error("Failed to delete {}: {}", record.path(), e.getMessage());
                }
            }
            System.out.println("\nDeleted " + deleted + " of " + recordsToDelete.size() + " components");

            // Clear cache after deletion since repository changed
            client.clearCache(repository);
        }

        printStatistics(allRecords.size(), recordsToDelete);
    }

    /**
     * Lists components in a repository and returns them for GUI display.
     * <p>
     * Fetches all components from the repository and returns filtered results.
     * This method is designed for GUI use where the caller wants the raw data
     * instead of formatted console output.
     * </p>
     *
     * @param repository  the name of the repository to list from
     * @param regexFilter optional regex pattern to filter component paths (null for no filter)
     * @param forceRefresh if true, bypasses cache and fetches fresh data
     * @return filtered list of repository records
     * @throws IOException          if an HTTP error occurs
     * @throws InterruptedException if the operation is interrupted
     * @throws IllegalArgumentException if the regex pattern is invalid
     */
    public List<RepoRecord> getRepositoryRecords(String repository, String regexFilter, boolean forceRefresh)
            throws IOException, InterruptedException {
        validateRegex(regexFilter);

        List<RepoRecord> allRecords = client.listComponents(repository, forceRefresh);

        return regexFilter == null
            ? allRecords
            : allRecords.stream()
                .filter(record -> record.path().matches(regexFilter))
                .toList();
    }

    /**
     * Gets cache status for a repository.
     *
     * @param repository the repository name
     * @return cache status string
     */
    public String getCacheStatus(String repository) {
        if (client.isCached(repository)) {
            long age = client.getCacheAge(repository);
            return String.format("Cached (%ds old)", age);
        }
        return "Not cached";
    }

    /**
     * Clears cache for a specific repository.
     *
     * @param repository the repository name
     */
    public void clearCache(String repository) {
        client.clearCache(repository);
    }

    /**
     * Clears all cached data.
     */
    public void clearAllCache() {
        client.clearAllCache();
    }

    /**
     * Formats repository records as a string with column headers.
     * <p>
     * Each record is displayed with its ID, file size (with thousand separators),
     * and path. Includes column headers for better readability.
     * </p>
     *
     * @param records the list of records to format
     * @return formatted string with column headers and data
     */
    public String formatRecordsWithHeaders(List<RepoRecord> records) {
        StringBuilder sb = new StringBuilder();

        // Column headers
        sb.append(String.format("%-50s  %15s  %s%n", "ID", "File Size", "Path"));
        sb.append(String.format("%-50s  %15s  %s%n",
            "=".repeat(50), "=".repeat(15), "=".repeat(50)));

        // Data rows
        for (RepoRecord record : records) {
            sb.append(String.format("%-50s  %,15d  %s%n",
                record.id(), record.fileSize(), record.path()));
        }

        // Summary
        sb.append("\n");
        sb.append(String.format("Total: %d components%n", records.size()));

        return sb.toString();
    }

    /**
     * Prints a formatted list of repository records to stdout.
     * <p>
     * Each record is displayed with its ID, file size (with thousand separators),
     * and path.
     * </p>
     *
     * @param records the list of records to print
     */
    private void printRecords(List<RepoRecord> records) {
        for (RepoRecord record : records) {
            System.out.printf("%s  %,15d  %s%n", record.id(), record.fileSize(), record.path());
        }
    }

    /**
     * Prints statistics about the repository and displayed records.
     * <p>
     * Shows total component count, matching component count (if different),
     * and total size in both bytes and megabytes.
     * </p>
     *
     * @param totalComponents   the total number of components in the repository
     * @param displayedRecords  the list of records that were displayed/matched
     */
    private void printStatistics(int totalComponents, List<RepoRecord> displayedRecords) {
        long totalSize = displayedRecords.stream()
            .mapToLong(RepoRecord::fileSize)
            .sum();

        System.out.println("\n");
        System.out.println("Total components in repository: " + totalComponents);
        if (displayedRecords.size() != totalComponents) {
            System.out.println("Matching components:            " + displayedRecords.size());
        }
        System.out.printf("Total size:                     %,d bytes (%.2f MB)%n",
            totalSize, totalSize / 1024.0 / 1024.0);
        System.out.println();
    }
}

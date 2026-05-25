package org.flossware.jnexus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

/**
 * Service layer for Nexus repository operations.
 * <p>
 * This class provides high-level business logic for listing, searching, deleting, and analyzing
 * components from Nexus repositories. It delegates HTTP communication to {@link NexusClient}
 * and handles filtering, output formatting, statistics, and user interaction.
 * </p>
 *
 * <h2>Core Functionality:</h2>
 * <ul>
 *   <li><strong>List Operations</strong> - List and filter repository components with caching support</li>
 *   <li><strong>Search Operations</strong> - Advanced filtering by size, date, extension, regex</li>
 *   <li><strong>Delete Operations</strong> - Safe deletion with dry-run, progress tracking, and error reporting</li>
 *   <li><strong>Statistics</strong> - Comprehensive repository analytics and metrics</li>
 * </ul>
 *
 * <h2>Usage Examples:</h2>
 * <pre>
 * // Initialize service
 * Credentials credentials = new Credentials();
 * NexusClient client = new NexusClient(credentials);
 * NexusService service = new NexusService(client);
 *
 * // List components with filtering
 * service.listRepository("maven-releases", ".*SNAPSHOT.*");
 *
 * // Get components for programmatic use
 * List&lt;RepoRecord&gt; components = service.getRepositoryRecords("maven-releases", null, false);
 *
 * // Advanced search with multiple criteria
 * SearchCriteria criteria = new SearchCriteria.Builder()
 *     .repository("maven-releases")
 *     .minSize(1_000_000L)
 *     .fileExtension(".jar")
 *     .createdAfter(Instant.parse("2024-01-01T00:00:00Z"))
 *     .build();
 * List&lt;ComponentMetadata&gt; results = service.searchComponents(criteria, false);
 *
 * // Safe deletion with dry-run
 * service.deleteFromRepository("maven-snapshots", ".*old.*", true);  // Preview only
 * service.deleteFromRepository("maven-snapshots", ".*old.*", false); // Actual deletion
 *
 * // Repository statistics
 * List&lt;ComponentMetadata&gt; components = client.listComponentsWithMetadata("maven-releases", false);
 * RepositoryStats stats = service.calculateStatistics("maven-releases", components);
 * System.out.println("Total size: " + stats.getTotalSizeGB() + " GB");
 * </pre>
 *
 * <h2>Safety Features:</h2>
 * <ul>
 *   <li>Dry-run mode for safe previews before deletion</li>
 *   <li>Regex pattern validation before operations</li>
 *   <li>Error collection and reporting for batch operations</li>
 *   <li>Progress tracking for long-running deletions</li>
 *   <li>Cache management to maintain data consistency</li>
 * </ul>
 *
 * @author sfloess
 * @since 1.0
 * @see NexusClient
 * @see Credentials
 * @see SearchCriteria
 * @see RepositoryStats
 */
public class NexusService {
    private static final Logger logger = LoggerFactory.getLogger(NexusService.class);

    private final NexusHttpClient client;

    /**
     * Constructs a new NexusService with the specified client.
     *
     * @param client the NexusHttpClient to use for HTTP operations
     */
    public NexusService(NexusHttpClient client) {
        this.client = client;
    }

    /**
     * Safely gets an exception message, using class name as fallback if message is null.
     *
     * @param e the exception
     * @return the exception message or class name if message is null
     */
    private String safeExceptionMessage(Exception e) {
        String message = e.getMessage();
        return message != null ? message : e.getClass().getSimpleName();
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
            throw new IllegalArgumentException("Invalid regex pattern: " + safeExceptionMessage(e), e);
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
        deleteFromRepository(repository, regexFilter, dryRun, null);
    }

    /**
     * Deletes components from a repository with progress tracking.
     * <p>
     * Same as {@link #deleteFromRepository(String, String, boolean)} but accepts
     * an optional {@link ProgressCallback} to track deletion progress.
     * </p>
     *
     * @param repository  the name of the repository to delete from
     * @param regexFilter optional regex pattern to filter component paths (null for all components)
     * @param dryRun      if true, shows what would be deleted without actually deleting
     * @param callback    optional callback for progress notifications (null for no callbacks)
     * @throws IOException          if an HTTP error occurs
     * @throws InterruptedException if the operation is interrupted
     */
    public void deleteFromRepository(String repository, String regexFilter, boolean dryRun, ProgressCallback callback)
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

        if (dryRun) {
            printDryRunVisualization(recordsToDelete);
        } else {
            System.out.println("Deleting:");
            printRecords(recordsToDelete);
            int deleted = 0;
            int total = recordsToDelete.size();
            boolean showProgress = total > 10; // Show progress for >10 components
            List<String> failures = new ArrayList<>();

            for (RepoRecord record : recordsToDelete) {
                try {
                    client.deleteComponent(record.id());
                    deleted++;
                    logger.info("Deleted: {}", record.path());

                    // Callback: component deleted
                    if (callback != null) {
                        try {
                            callback.onComponentDeleted(record.id(), record.path());
                        } catch (Exception e) {
                            logger.warn("Progress callback error: {}", safeExceptionMessage(e));
                        }
                    }

                    if (showProgress && deleted % 5 == 0) {
                        System.out.printf("Progress: %d of %d components deleted (%.1f%%)%n",
                            deleted, total, (deleted * 100.0 / total));

                        // Callback: delete progress
                        if (callback != null) {
                            try {
                                callback.onDeleteProgress(deleted, total, deleted * 100.0 / total);
                            } catch (Exception e) {
                                logger.warn("Progress callback error: {}", safeExceptionMessage(e));
                            }
                        }
                    }
                } catch (IOException e) {
                    String errorMsg = safeExceptionMessage(e);
                    failures.add(record.path() + " - " + errorMsg);
                    logger.error("Failed to delete {}: {}", record.path(), errorMsg);

                    // Callback: delete failed
                    if (callback != null) {
                        try {
                            callback.onComponentDeleteFailed(record.id(), record.path(), errorMsg);
                        } catch (Exception ex) {
                            logger.warn("Progress callback error: {}", safeExceptionMessage(ex));
                        }
                    }
                }
            }

            System.out.println("\nDeleted " + deleted + " of " + recordsToDelete.size() + " components");

            if (!failures.isEmpty()) {
                System.err.println("\n⚠ WARNING: " + failures.size() + " component(s) failed to delete:");
                for (String failure : failures) {
                    System.err.println("  - " + failure);
                }
            }

            // Callback: delete complete
            if (callback != null) {
                try {
                    callback.onDeleteComplete(deleted, failures.size());
                } catch (Exception e) {
                    logger.warn("Progress callback error: {}", safeExceptionMessage(e));
                }
            }

            // Clear ALL cache after deletion to prevent stale "All" repository cache
            client.clearAllCache();
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
     * Formats repository records as a string with column headers and summary.
     * <p>
     * Each record is displayed with its ID, file size (with thousand separators),
     * and path. Includes column headers and a summary row with totals.
     * </p>
     *
     * @param records the list of records to format
     * @return formatted string with column headers, data, and summary
     */
    public String formatRecordsWithHeaders(List<RepoRecord> records) {
        StringBuilder sb = new StringBuilder();

        // Column headers
        sb.append(String.format("%-50s  %15s  %s%n", "ID", "File Size", "Path"));
        sb.append(String.format("%-50s  %15s  %s%n",
            "=".repeat(50), "=".repeat(15), "=".repeat(50)));

        // Data rows and calculate total
        long totalBytes = 0;
        for (RepoRecord record : records) {
            sb.append(String.format("%-50s  %,15d  %s%n",
                record.id(), record.fileSize(), record.path()));
            totalBytes += record.fileSize();
        }

        // Summary row
        if (!records.isEmpty()) {
            sb.append(String.format("%-50s  %15s  %s%n",
                "=".repeat(50), "=".repeat(15), "=".repeat(50)));
            sb.append(String.format("%-50s  %,15d  %s%n",
                "TOTAL: " + records.size() + " components",
                totalBytes,
                String.format("(%.2f MB)", totalBytes / 1024.0 / 1024.0)));
        }

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
     * Prints a visual representation of what would be deleted in dry-run mode.
     * <p>
     * Shows a clear header, bordered list of components, and summary statistics
     * to help users understand the impact before running an actual deletion.
     * </p>
     *
     * @param recordsToDelete the list of records that would be deleted
     */
    private void printDryRunVisualization(List<RepoRecord> recordsToDelete) {
        long totalSize = recordsToDelete.stream()
            .mapToLong(RepoRecord::fileSize)
            .sum();

        // Header
        System.out.println();
        System.out.println("╔" + "═".repeat(78) + "╗");
        System.out.println("║" + centerText("DRY-RUN MODE: SIMULATION ONLY - NO ACTUAL DELETION", 78) + "║");
        System.out.println("╠" + "═".repeat(78) + "╣");
        System.out.println("║" + centerText("The following components WOULD BE DELETED:", 78) + "║");
        System.out.println("╚" + "═".repeat(78) + "╝");
        System.out.println();

        // List components
        printRecords(recordsToDelete);

        // Summary box
        System.out.println();
        System.out.println("┌" + "─".repeat(78) + "┐");
        System.out.println("│" + centerText("DRY-RUN SUMMARY", 78) + "│");
        System.out.println("├" + "─".repeat(78) + "┤");
        System.out.printf("│  Components that would be deleted: %-47d│%n", recordsToDelete.size());
        System.out.printf("│  Total size: %,d bytes (%.2f MB)%-38s│%n",
            totalSize, totalSize / 1024.0 / 1024.0, " ");
        System.out.println("├" + "─".repeat(78) + "┤");
        System.out.println("│" + centerText("⚠ NO ACTUAL DELETION PERFORMED", 78) + "│");
        System.out.println("│" + centerText("To delete these components, run without --dry-run flag", 78) + "│");
        System.out.println("└" + "─".repeat(78) + "┘");
        System.out.println();
    }

    /**
     * Centers text within a given width.
     *
     * @param text the text to center
     * @param width the total width
     * @return centered text padded with spaces
     */
    private String centerText(String text, int width) {
        int padding = (width - text.length()) / 2;
        int extraPadding = (width - text.length()) % 2;
        return " ".repeat(padding) + text + " ".repeat(padding + extraPadding);
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

    /**
     * Searches components using advanced filter criteria.
     * <p>
     * Fetches all components with metadata from the repository and applies
     * client-side filtering based on the provided criteria. All filter criteria
     * are optional (null values mean no filtering on that criterion).
     * </p>
     *
     * @param criteria the search criteria containing all filter parameters
     * @param forceRefresh if true, bypasses cache and fetches fresh data
     * @return a list of components matching all specified criteria
     * @throws IOException          if an HTTP error occurs
     * @throws InterruptedException if the operation is interrupted
     */
    public List<ComponentMetadata> searchComponents(SearchCriteria criteria, boolean forceRefresh)
            throws IOException, InterruptedException {
        // Validate regex filter if present
        if (criteria.regexFilter() != null) {
            validateRegex(criteria.regexFilter());
        }

        // Fetch all components with metadata
        List<ComponentMetadata> allComponents = client.listComponentsWithMetadata(
            criteria.repository(),
            forceRefresh
        );

        // Apply filters sequentially
        return allComponents.stream()
            .filter(component -> matchesRegexFilter(component, criteria.regexFilter()))
            .filter(component -> matchesSizeRange(component, criteria.minSize(), criteria.maxSize()))
            .filter(component -> matchesDateRange(component, criteria.createdAfter(), criteria.createdBefore()))
            .filter(component -> matchesFileExtension(component, criteria.fileExtension()))
            .filter(component -> matchesComponentNamePattern(component, criteria.componentNamePattern()))
            .toList();
    }

    /**
     * Checks if a component matches the regex filter.
     */
    private boolean matchesRegexFilter(ComponentMetadata component, String regexFilter) {
        if (regexFilter == null || regexFilter.isEmpty()) {
            return true;
        }
        return component.path().matches(regexFilter);
    }

    /**
     * Checks if a component matches the size range filter.
     */
    private boolean matchesSizeRange(ComponentMetadata component, Long minSize, Long maxSize) {
        if (minSize != null && component.fileSize() < minSize) {
            return false;
        }
        if (maxSize != null && component.fileSize() > maxSize) {
            return false;
        }
        return true;
    }

    /**
     * Checks if a component matches the date range filter.
     */
    private boolean matchesDateRange(ComponentMetadata component, Instant createdAfter, Instant createdBefore) {
        if (component.createdDate() == null) {
            return true; // If no creation date, pass all date filters
        }
        if (createdAfter != null && component.createdDate().isBefore(createdAfter)) {
            return false;
        }
        if (createdBefore != null && component.createdDate().isAfter(createdBefore)) {
            return false;
        }
        return true;
    }

    /**
     * Checks if a component matches the file extension filter.
     */
    private boolean matchesFileExtension(ComponentMetadata component, String fileExtension) {
        if (fileExtension == null || fileExtension.isEmpty()) {
            return true;
        }
        return component.path().endsWith(fileExtension);
    }

    /**
     * Checks if a component matches the component name pattern filter.
     */
    private boolean matchesComponentNamePattern(ComponentMetadata component, String componentNamePattern) {
        if (componentNamePattern == null || componentNamePattern.isEmpty()) {
            return true;
        }
        // Match against the component ID (which typically contains group/name/version)
        return component.id().matches(componentNamePattern);
    }

    /**
     * Calculates comprehensive statistics for a list of components.
     * <p>
     * Analyzes the provided components to generate size distribution,
     * file type breakdown, age distribution, and identifies the largest components.
     * </p>
     *
     * @param repository the name of the repository being analyzed
     * @param components the list of components to analyze
     * @return a RepositoryStats record containing all calculated statistics
     */
    public RepositoryStats calculateStatistics(String repository, List<ComponentMetadata> components) {
        return calculateStatistics(repository, components, null);
    }

    /**
     * Calculates comprehensive statistics with progress tracking.
     * <p>
     * Same as {@link #calculateStatistics(String, List)} but accepts an optional
     * {@link ProgressCallback} to track calculation progress.
     * </p>
     *
     * @param repository the name of the repository being analyzed
     * @param components the list of components to analyze
     * @param callback optional callback for progress notifications (null for no callbacks)
     * @return a RepositoryStats record containing all calculated statistics
     */
    public RepositoryStats calculateStatistics(String repository, List<ComponentMetadata> components, ProgressCallback callback) {
        // Callback: statistics start
        if (callback != null) {
            try {
                callback.onStatisticsStart(repository, components.size());
            } catch (Exception e) {
                logger.warn("Progress callback error: {}", safeExceptionMessage(e));
            }
        }
        if (components.isEmpty()) {
            return new RepositoryStats(
                repository,
                0,
                0L,
                0L,
                0L,
                Map.of(),
                Map.of(),
                Map.of(),
                List.of()
            );
        }

        int totalComponents = components.size();

        // Calculate total size
        long totalSize = components.stream()
            .mapToLong(ComponentMetadata::fileSize)
            .sum();

        // Calculate average size
        long averageSize = totalSize / totalComponents;

        // Calculate median size
        long medianSize = calculateMedianSize(components);

        // Calculate size distribution
        Map<String, Integer> sizeDistribution = calculateSizeDistribution(components);

        // Calculate file type breakdown
        Map<String, Long> fileTypeBreakdown = calculateFileTypeBreakdown(components);

        // Calculate age distribution
        Map<String, Integer> ageDistribution = calculateAgeDistribution(components);

        // Get largest components (top 20)
        List<ComponentMetadata> largestComponents = components.stream()
            .sorted(Comparator.comparingLong(ComponentMetadata::fileSize).reversed())
            .limit(20)
            .toList();

        RepositoryStats stats = new RepositoryStats(
            repository,
            totalComponents,
            totalSize,
            averageSize,
            medianSize,
            sizeDistribution,
            fileTypeBreakdown,
            ageDistribution,
            largestComponents
        );

        // Callback: statistics complete
        if (callback != null) {
            try {
                callback.onStatisticsComplete(repository, stats);
            } catch (Exception e) {
                logger.warn("Progress callback error: {}", safeExceptionMessage(e));
            }
        }

        return stats;
    }

    /**
     * Calculates the median file size from a list of components.
     */
    private long calculateMedianSize(List<ComponentMetadata> components) {
        List<Long> sizes = components.stream()
            .map(ComponentMetadata::fileSize)
            .sorted()
            .toList();

        int middle = sizes.size() / 2;
        if (sizes.size() % 2 == 0) {
            return (sizes.get(middle - 1) + sizes.get(middle)) / 2;
        } else {
            return sizes.get(middle);
        }
    }

    /**
     * Calculates size distribution across predefined buckets.
     */
    private Map<String, Integer> calculateSizeDistribution(List<ComponentMetadata> components) {
        Map<String, Integer> distribution = new LinkedHashMap<>();

        // Define size ranges in bytes
        long mb = 1024L * 1024L;
        long gb = 1024L * 1024L * 1024L;

        int under1mb = 0;
        int from1to10mb = 0;
        int from10to100mb = 0;
        int from100mbTo1gb = 0;
        int over1gb = 0;

        for (ComponentMetadata component : components) {
            long size = component.fileSize();
            if (size < mb) {
                under1mb++;
            } else if (size < 10 * mb) {
                from1to10mb++;
            } else if (size < 100 * mb) {
                from10to100mb++;
            } else if (size < gb) {
                from100mbTo1gb++;
            } else {
                over1gb++;
            }
        }

        distribution.put(RepositoryStats.SIZE_RANGE_UNDER_1MB, under1mb);
        distribution.put(RepositoryStats.SIZE_RANGE_1_TO_10MB, from1to10mb);
        distribution.put(RepositoryStats.SIZE_RANGE_10_TO_100MB, from10to100mb);
        distribution.put(RepositoryStats.SIZE_RANGE_100MB_TO_1GB, from100mbTo1gb);
        distribution.put(RepositoryStats.SIZE_RANGE_OVER_1GB, over1gb);

        return distribution;
    }

    /**
     * Calculates file type breakdown by extension.
     */
    private Map<String, Long> calculateFileTypeBreakdown(List<ComponentMetadata> components) {
        return components.stream()
            .collect(Collectors.groupingBy(
                this::getFileExtension,
                Collectors.summingLong(ComponentMetadata::fileSize)
            ));
    }

    /**
     * Extracts file extension from a path.
     */
    private String getFileExtension(ComponentMetadata component) {
        String path = component.path();
        int lastDot = path.lastIndexOf('.');
        if (lastDot > 0 && lastDot < path.length() - 1) {
            return path.substring(lastDot);
        }
        return "(no extension)";
    }

    /**
     * Calculates age distribution based on creation dates.
     */
    private Map<String, Integer> calculateAgeDistribution(List<ComponentMetadata> components) {
        Map<String, Integer> distribution = new LinkedHashMap<>();

        Instant now = Instant.now();
        int last7days = 0;
        int last30days = 0;
        int last90days = 0;
        int older = 0;

        for (ComponentMetadata component : components) {
            if (component.createdDate() == null) {
                older++; // Count components without creation date as "older"
                continue;
            }

            long daysAgo = Duration.between(component.createdDate(), now).toDays();

            if (daysAgo <= 7) {
                last7days++;
            } else if (daysAgo <= 30) {
                last30days++;
            } else if (daysAgo <= 90) {
                last90days++;
            } else {
                older++;
            }
        }

        distribution.put(RepositoryStats.AGE_RANGE_LAST_7_DAYS, last7days);
        distribution.put(RepositoryStats.AGE_RANGE_LAST_30_DAYS, last30days);
        distribution.put(RepositoryStats.AGE_RANGE_LAST_90_DAYS, last90days);
        distribution.put(RepositoryStats.AGE_RANGE_OLDER, older);

        return distribution;
    }
}

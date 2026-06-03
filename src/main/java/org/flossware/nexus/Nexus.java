package org.flossware.nexus;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Command-line interface for interacting with Sonatype Nexus repositories.
 * <p>
 * This is the main entry point for the Nexus CLI tool. It provides three subcommands:
 * </p>
 * <ul>
 *   <li><strong>list</strong> - List components in a repository with optional filtering</li>
 *   <li><strong>delete</strong> - Delete components from a repository with safety features</li>
 *   <li><strong>stats</strong> - Display repository statistics and analytics</li>
 * </ul>
 * <p>
 * The tool uses Picocli for command-line parsing and supports standard help and version options.
 * </p>
 *
 * <h2>Usage Examples:</h2>
 * <pre>
 * # List all components in a repository
 * jnexus list maven-releases
 *
 * # List with regex filter
 * jnexus list maven-releases --regex ".*SNAPSHOT.*"
 *
 * # List with metadata and advanced filters
 * jnexus list maven-releases --show-metadata --min-size 1000000 --extension .jar
 *
 * # Delete with dry-run (safe preview)
 * jnexus delete maven-snapshots --regex ".*old.*" --dry-run
 *
 * # Delete with confirmation
 * jnexus delete maven-snapshots --regex ".*old.*"
 *
 * # Repository statistics
 * jnexus stats maven-releases --format json
 *
 * # Use different configuration profile
 * jnexus --profile prod list maven-releases
 *
 * # Verbose mode for debugging
 * jnexus -v list maven-releases
 * </pre>
 *
 * <h2>Configuration:</h2>
 * <p>
 * Credentials can be configured via:
 * </p>
 * <ol>
 *   <li>Environment variables: NEXUS_URL, NEXUS_USER, NEXUS_PASSWORD</li>
 *   <li>Properties file: ~/.flossware/nexus/nexus.properties</li>
 *   <li>Profile-based properties: ~/.flossware/nexus/nexus-{profile}.properties</li>
 * </ol>
 *
 * @author sfloess
 * @since 1.0
 * @see <a href="https://github.com/FlossWare/jnexus">GitHub Repository</a>
 */
@Command(
    name = "jnexus",
    description = "CLI tool for interacting with Sonatype Nexus repositories",
    mixinStandardHelpOptions = true,
    version = "jnexus 2.0.0",
    subcommands = {
        JNexus.ListCommand.class,
        JNexus.DeleteCommand.class,
        JNexus.StatsCommand.class
    }
)
public class JNexus implements Callable<Integer> {

    // Shared Scanner for System.in - never close this to avoid closing System.in
    private static final java.util.Scanner CONSOLE_SCANNER = new java.util.Scanner(System.in);

    @Option(
        names = {"-v", "--verbose"},
        description = "Enable verbose output (debug level logging)"
    )
    private boolean verbose;

    @Option(
        names = {"-q", "--quiet"},
        description = "Quiet mode (only show warnings and errors)"
    )
    private boolean quiet;

    @Option(
        names = {"-p", "--profile"},
        description = "Configuration profile to use (e.g., dev, prod, staging)"
    )
    private String profile;

    /**
     * Configures logging level based on verbose/quiet flags.
     */
    private void configureLogging() {
        Logger rootLogger = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        if (verbose) {
            rootLogger.setLevel(Level.DEBUG);
        } else if (quiet) {
            rootLogger.setLevel(Level.WARN);
        } else {
            rootLogger.setLevel(Level.INFO);
        }
    }

    /**
     * Executes when the tool is run without a subcommand.
     * Displays usage information.
     *
     * @return exit code 0
     */
    @Override
    public Integer call() {
        configureLogging();
        CommandLine.usage(this, System.out);
        return 0;
    }

    /**
     * Subcommand for listing components in a Nexus repository.
     * <p>
     * Lists all components in the specified repository, or only those matching
     * the optional regex filter pattern. Displays component ID, file size, and path.
     * </p>
     */
    @Command(
        name = "list",
        description = "List components in a Nexus repository"
    )
    static class ListCommand implements Callable<Integer> {
        @CommandLine.ParentCommand
        private JNexus parent;

        @Parameters(index = "0", description = "Repository name")
        private String repository;

        @Parameters(index = "1", arity = "0..1", description = "Optional regex filter for component paths")
        private String regexFilter;

        @Option(names = {"--min-size"}, description = "Minimum file size in bytes")
        private Long minSize;

        @Option(names = {"--max-size"}, description = "Maximum file size in bytes")
        private Long maxSize;

        @Option(names = {"--created-after"}, description = "Filter by creation date (ISO format: 2024-01-01T00:00:00Z)")
        private String createdAfter;

        @Option(names = {"--created-before"}, description = "Filter by creation date (ISO format: 2024-01-01T00:00:00Z)")
        private String createdBefore;

        @Option(names = {"--extension"}, description = "Filter by file extension (e.g., .jar, .war)")
        private String extension;

        @Option(names = {"--show-metadata"}, description = "Display full component metadata")
        private boolean showMetadata;

        /**
         * Executes the list command.
         * <p>
         * Loads credentials, creates client and service instances, and lists
         * components from the specified repository with optional advanced filtering.
         * </p>
         *
         * @return exit code: 0 for success, 1 for error
         */
        @Override
        public Integer call() {
            parent.configureLogging();
            org.slf4j.Logger logger = LoggerFactory.getLogger(JNexus.class);

            try {
                Credentials credentials = new Credentials(parent.profile);
                NexusClient client = new NexusClient(credentials);
                NexusService service = new NexusService(client);

                if (credentials.getProfile() != null) {
                    logger.debug("Using profile: {}", credentials.getProfile());
                }

                System.out.println("Listing components in repository: " + repository);
                printActiveFilters();
                System.out.println();

                // Check if advanced filters are used
                boolean hasAdvancedFilters = minSize != null || maxSize != null
                    || createdAfter != null || createdBefore != null || extension != null;

                if (hasAdvancedFilters || showMetadata) {
                    // Use advanced search with metadata
                    SearchCriteria.Builder criteriaBuilder = SearchCriteria.builder()
                        .repository(repository)
                        .regexFilter(regexFilter)
                        .minSize(minSize)
                        .maxSize(maxSize)
                        .fileExtension(extension);

                    // Parse date strings to Instant if provided
                    if (createdAfter != null) {
                        criteriaBuilder.createdAfter(java.time.Instant.parse(createdAfter));
                    }
                    if (createdBefore != null) {
                        criteriaBuilder.createdBefore(java.time.Instant.parse(createdBefore));
                    }

                    SearchCriteria criteria = criteriaBuilder.build();
                    List<ComponentMetadata> components = service.searchComponents(criteria, false);

                    if (showMetadata) {
                        printComponentsWithMetadata(components);
                    } else {
                        printComponents(components);
                    }
                    printComponentStatistics(components.size(), components);
                } else {
                    // Use simple list (backward compatible)
                    service.listRepository(repository, regexFilter);
                }

                return 0;
            } catch (java.time.format.DateTimeParseException e) {
                logger.error("Invalid date format: {}", e.getParsedString());
                logger.error("Please use ISO 8601 format (examples: 2024-01-01T00:00:00Z or 2024-01-15T10:30:00.000Z)");
                return 1;
            } catch (IllegalArgumentException e) {
                logger.error("Error: {}", e.getMessage());
                return 1;
            } catch (Exception e) {
                logger.error("Error: {}", e.getMessage());
                if (parent.verbose) {
                    logger.error("Stack trace:", e);
                }
                return 1;
            }
        }

        private void printActiveFilters() {
            if (regexFilter != null) {
                System.out.println("Regex filter: " + regexFilter);
            }
            if (minSize != null) {
                System.out.printf("Min size: %,d bytes%n", minSize);
            }
            if (maxSize != null) {
                System.out.printf("Max size: %,d bytes%n", maxSize);
            }
            if (createdAfter != null) {
                System.out.println("Created after: " + createdAfter);
            }
            if (createdBefore != null) {
                System.out.println("Created before: " + createdBefore);
            }
            if (extension != null) {
                System.out.println("Extension: " + extension);
            }
        }

        private void printComponents(List<ComponentMetadata> components) {
            System.out.println("ID                                    File Size (bytes)  Path");
            System.out.println("====================================  ================  ================================");

            for (ComponentMetadata component : components) {
                System.out.printf("%s  %,15d  %s%n",
                    component.id(),
                    component.fileSize(),
                    component.path());
            }
        }

        private void printComponentsWithMetadata(List<ComponentMetadata> components) {
            System.out.println("ID                                    File Size          Created              Type              Path");
            System.out.println("====================================  ================  ===================  ===============  ================================");

            java.text.DateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            dateFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));

            for (ComponentMetadata component : components) {
                String createdDate = component.createdDate() != null
                    ? dateFormat.format(java.util.Date.from(component.createdDate()))
                    : "N/A";
                String contentType = component.contentType() != null ? component.contentType() : "unknown";

                System.out.printf("%s  %,15d  %19s  %15s  %s%n",
                    component.id(),
                    component.fileSize(),
                    createdDate,
                    truncate(contentType, 15),
                    component.path());
            }
        }

        private void printComponentStatistics(int totalComponents, List<ComponentMetadata> components) {
            long totalSize = components.stream()
                .mapToLong(ComponentMetadata::fileSize)
                .sum();

            System.out.println("\n");
            System.out.println("Total components: " + totalComponents);
            System.out.printf("Total size:       %,d bytes (%.2f MB / %.4f GB)%n",
                totalSize,
                totalSize / 1024.0 / 1024.0,
                totalSize / 1024.0 / 1024.0 / 1024.0);
            System.out.println();
        }

        private String truncate(String text, int maxLength) {
            if (text.length() <= maxLength) {
                return text;
            }
            return text.substring(0, maxLength - 3) + "...";
        }
    }

    /**
     * Subcommand for deleting components from a Nexus repository.
     * <p>
     * Deletes components from the specified repository, with optional filtering
     * and safety features including:
     * </p>
     * <ul>
     *   <li>Confirmation prompts (unless --yes is used)</li>
     *   <li>Dry-run mode to preview deletions (--dry-run)</li>
     *   <li>Regex filtering to target specific components</li>
     * </ul>
     */
    @Command(
        name = "delete",
        description = "Delete components from a Nexus repository"
    )
    static class DeleteCommand implements Callable<Integer> {
        @CommandLine.ParentCommand
        private JNexus parent;

        @Parameters(index = "0", description = "Repository name")
        private String repository;

        @Parameters(index = "1", arity = "0..1", description = "Optional regex filter for component paths to delete")
        private String regexFilter;

        @Option(
            names = {"-n", "--dry-run"},
            description = "Show what would be deleted without actually deleting"
        )
        private boolean dryRun;

        @Option(
            names = {"-y", "--yes"},
            description = "Skip confirmation prompt"
        )
        private boolean skipConfirmation;

        /**
         * Executes the delete command.
         * <p>
         * Prompts for confirmation (unless --yes or --dry-run), loads credentials,
         * creates client and service instances, and deletes components from the
         * specified repository.
         * </p>
         *
         * @return exit code: 0 for success or user cancellation, 1 for error
         */
        @Override
        public Integer call() {
            parent.configureLogging();
            org.slf4j.Logger logger = LoggerFactory.getLogger(JNexus.class);

            try {
                if (!dryRun && !skipConfirmation) {
                    System.out.println("WARNING: This will permanently delete components from repository: " + repository);
                    if (regexFilter != null) {
                        System.out.println("Filter: " + regexFilter);
                    }
                    System.out.print("Are you sure? (yes/no): ");
                    String confirmation = System.console() != null
                        ? System.console().readLine()
                        : CONSOLE_SCANNER.nextLine();

                    if (!"yes".equalsIgnoreCase(confirmation)) {
                        System.out.println("Cancelled.");
                        return 0;
                    }
                }

                Credentials credentials = new Credentials(parent.profile);
                NexusClient client = new NexusClient(credentials);
                NexusService service = new NexusService(client);

                if (credentials.getProfile() != null) {
                    logger.debug("Using profile: {}", credentials.getProfile());
                }

                if (dryRun) {
                    System.out.println("DRY RUN - No components will be deleted");
                }
                System.out.println("Repository: " + repository);
                if (regexFilter != null) {
                    System.out.println("Filter: " + regexFilter);
                }
                System.out.println();

                service.deleteFromRepository(repository, regexFilter, dryRun);
                return 0;
            } catch (IllegalArgumentException e) {
                logger.error("Error: {}", e.getMessage());
                return 1;
            } catch (Exception e) {
                logger.error("Error: {}", e.getMessage());
                if (parent.verbose) {
                    logger.error("Stack trace:", e);
                }
                return 1;
            }
        }
    }

    /**
     * Subcommand for showing repository statistics.
     * <p>
     * Analyzes all components in a repository and displays comprehensive statistics
     * including size distribution, file type breakdown, age analysis, and largest components.
     * </p>
     */
    @Command(
        name = "stats",
        description = "Show repository statistics"
    )
    static class StatsCommand implements Callable<Integer> {
        @CommandLine.ParentCommand
        private JNexus parent;

        @Parameters(index = "0", description = "Repository name")
        private String repository;

        @Option(names = {"--format"}, description = "Output format: text or json (default: text)")
        private String format = "text";

        /**
         * Executes the stats command.
         * <p>
         * Loads credentials, fetches all components with metadata, calculates statistics,
         * and displays them in the requested format.
         * </p>
         *
         * @return exit code: 0 for success, 1 for error
         */
        @Override
        public Integer call() {
            parent.configureLogging();
            org.slf4j.Logger logger = LoggerFactory.getLogger(JNexus.class);

            try {
                Credentials credentials = new Credentials(parent.profile);
                NexusClient client = new NexusClient(credentials);
                NexusService service = new NexusService(client);

                if (credentials.getProfile() != null) {
                    logger.debug("Using profile: {}", credentials.getProfile());
                }

                System.out.println("Calculating statistics for repository: " + repository);
                System.out.println();

                // Fetch all components with metadata
                List<ComponentMetadata> components = client.listComponentsWithMetadata(repository);

                // Calculate statistics
                RepositoryStats stats = service.calculateStatistics(repository, components);

                // Display statistics
                if ("json".equalsIgnoreCase(format)) {
                    printStatsAsJson(stats);
                } else {
                    printStatsAsText(stats);
                }

                return 0;
            } catch (IllegalArgumentException e) {
                logger.error("Error: {}", e.getMessage());
                return 1;
            } catch (Exception e) {
                logger.error("Error: {}", e.getMessage());
                if (parent.verbose) {
                    logger.error("Stack trace:", e);
                }
                return 1;
            }
        }

        private void printStatsAsText(RepositoryStats stats) {
            NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);

            System.out.println("Repository Statistics: " + stats.repository());
            System.out.println("=====================================");
            System.out.printf("Total Components: %,d%n", stats.totalComponents());
            System.out.printf("Total Size:       %s bytes (%.2f MB / %.4f GB)%n",
                numberFormat.format(stats.totalSize()),
                stats.getTotalSizeMB(),
                stats.getTotalSizeGB());
            System.out.printf("Average Size:     %s bytes (%.2f MB)%n",
                numberFormat.format(stats.averageSize()),
                stats.getAverageSizeMB());
            System.out.printf("Median Size:      %s bytes (%.2f MB)%n",
                numberFormat.format(stats.medianSize()),
                stats.getMedianSizeMB());
            System.out.println();

            // Size Distribution
            System.out.println("Size Distribution:");
            for (Map.Entry<String, Integer> entry : stats.sizeDistribution().entrySet()) {
                double percentage = (entry.getValue() * 100.0) / stats.totalComponents();
                System.out.printf("  %-20s %,6d components (%5.1f%%)%n",
                    entry.getKey() + ":",
                    entry.getValue(),
                    percentage);
            }
            System.out.println();

            // File Type Breakdown
            System.out.println("File Type Breakdown:");
            long totalSize = stats.totalSize();
            for (Map.Entry<String, Long> entry : stats.fileTypeBreakdown().entrySet()) {
                double percentage = (entry.getValue() * 100.0) / totalSize;
                double sizeMB = entry.getValue() / 1024.0 / 1024.0;
                System.out.printf("  %-15s %10.2f MB (%5.1f%%)%n",
                    entry.getKey() + ":",
                    sizeMB,
                    percentage);
            }
            System.out.println();

            // Age Distribution
            System.out.println("Age Distribution:");
            for (Map.Entry<String, Integer> entry : stats.ageDistribution().entrySet()) {
                System.out.printf("  %-20s %,6d components%n",
                    entry.getKey() + ":",
                    entry.getValue());
            }
            System.out.println();

            // Largest Components
            System.out.println("Largest Components (Top 10):");
            System.out.println("  ID                                    Size          Path");
            System.out.println("  ====================================  ============  ================================");

            stats.largestComponents().stream()
                .limit(10)
                .forEach(component -> {
                    double sizeMB = component.fileSize() / 1024.0 / 1024.0;
                    System.out.printf("  %s  %10.2f MB  %s%n",
                        component.id(),
                        sizeMB,
                        truncate(component.path(), 32));
                });
            System.out.println();
        }

        private void printStatsAsJson(RepositoryStats stats) {
            // Simple JSON output (could use Jackson for proper formatting)
            System.out.println("{");
            System.out.printf("  \"repository\": \"%s\",%n", stats.repository());
            System.out.printf("  \"totalComponents\": %d,%n", stats.totalComponents());
            System.out.printf("  \"totalSize\": %d,%n", stats.totalSize());
            System.out.printf("  \"averageSize\": %d,%n", stats.averageSize());
            System.out.printf("  \"medianSize\": %d,%n", stats.medianSize());

            System.out.println("  \"sizeDistribution\": {");
            int sizeDistCount = 0;
            for (Map.Entry<String, Integer> entry : stats.sizeDistribution().entrySet()) {
                System.out.printf("    \"%s\": %d%s%n",
                    entry.getKey(),
                    entry.getValue(),
                    ++sizeDistCount < stats.sizeDistribution().size() ? "," : "");
            }
            System.out.println("  },");

            System.out.println("  \"fileTypeBreakdown\": {");
            int fileTypeCount = 0;
            for (Map.Entry<String, Long> entry : stats.fileTypeBreakdown().entrySet()) {
                System.out.printf("    \"%s\": %d%s%n",
                    entry.getKey(),
                    entry.getValue(),
                    ++fileTypeCount < stats.fileTypeBreakdown().size() ? "," : "");
            }
            System.out.println("  },");

            System.out.println("  \"ageDistribution\": {");
            int ageDistCount = 0;
            for (Map.Entry<String, Integer> entry : stats.ageDistribution().entrySet()) {
                System.out.printf("    \"%s\": %d%s%n",
                    entry.getKey(),
                    entry.getValue(),
                    ++ageDistCount < stats.ageDistribution().size() ? "," : "");
            }
            System.out.println("  }");

            System.out.println("}");
        }

        private String truncate(String text, int maxLength) {
            if (text.length() <= maxLength) {
                return text;
            }
            return text.substring(0, maxLength - 3) + "...";
        }
    }

    /**
     * Main entry point for the Nexus CLI tool.
     * <p>
     * Parses command-line arguments using Picocli and executes the appropriate
     * subcommand. The exit code from the command is used as the process exit code.
     * </p>
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        int exitCode = new CommandLine(new JNexus()).execute(args);
        System.exit(exitCode);
    }
}

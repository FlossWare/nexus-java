package org.flossware.nexus;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

/**
 * Command-line interface for interacting with Sonatype Nexus repositories.
 * <p>
 * This is the main entry point for the Nexus CLI tool. It provides two subcommands:
 * </p>
 * <ul>
 *   <li><strong>list</strong> - List components in a repository with optional filtering</li>
 *   <li><strong>delete</strong> - Delete components from a repository with safety features</li>
 * </ul>
 * <p>
 * The tool uses Picocli for command-line parsing and supports standard help and version options.
 * </p>
 *
 * @author sfloess
 * @since 1.0
 */
@Command(
    name = "nexus",
    description = "CLI tool for interacting with Sonatype Nexus repositories",
    mixinStandardHelpOptions = true,
    version = "nexus 1.0",
    subcommands = {
        Nexus.ListCommand.class,
        Nexus.DeleteCommand.class
    }
)
public class Nexus implements Callable<Integer> {

    /**
     * Executes when the tool is run without a subcommand.
     * Displays usage information.
     *
     * @return exit code 0
     */
    @Override
    public Integer call() {
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
        @Parameters(index = "0", description = "Repository name")
        private String repository;

        @Parameters(index = "1", arity = "0..1", description = "Optional regex filter for component paths")
        private String regexFilter;

        /**
         * Executes the list command.
         * <p>
         * Loads credentials, creates client and service instances, and lists
         * components from the specified repository.
         * </p>
         *
         * @return exit code: 0 for success, 1 for error
         */
        @Override
        public Integer call() {
            try {
                Credentials credentials = new Credentials();
                NexusClient client = new NexusClient(credentials);
                NexusService service = new NexusService(client);

                System.out.println("Listing components in repository: " + repository);
                if (regexFilter != null) {
                    System.out.println("Filter: " + regexFilter);
                }
                System.out.println();

                service.listRepository(repository, regexFilter);
                return 0;
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                e.printStackTrace();
                return 1;
            }
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
            try {
                if (!dryRun && !skipConfirmation) {
                    System.out.println("WARNING: This will permanently delete components from repository: " + repository);
                    if (regexFilter != null) {
                        System.out.println("Filter: " + regexFilter);
                    }
                    System.out.print("Are you sure? (yes/no): ");
                    String confirmation = System.console() != null
                        ? System.console().readLine()
                        : new java.util.Scanner(System.in).nextLine();

                    if (!"yes".equalsIgnoreCase(confirmation)) {
                        System.out.println("Cancelled.");
                        return 0;
                    }
                }

                Credentials credentials = new Credentials();
                NexusClient client = new NexusClient(credentials);
                NexusService service = new NexusService(client);

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
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                e.printStackTrace();
                return 1;
            }
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
        int exitCode = new CommandLine(new Nexus()).execute(args);
        System.exit(exitCode);
    }
}

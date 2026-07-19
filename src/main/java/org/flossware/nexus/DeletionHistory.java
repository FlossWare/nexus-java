package org.flossware.nexus;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.flossware.jnexus.RepoRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

/**
 * Tracks deletion history for undo/recovery reference during a session.
 * <p>
 * This class maintains an in-memory record of all components deleted during
 * the current session. Since Nexus deletions are permanent and cannot be
 * reversed via API, this history serves as a reference for manual recovery
 * (e.g., re-uploading from backups).
 * </p>
 *
 * <h2>Key Features:</h2>
 * <ul>
 *   <li><strong>Session-scoped</strong> - History is lost when the application exits</li>
 *   <li><strong>Bounded size</strong> - Configurable maximum history size (default: 1000)</li>
 *   <li><strong>Thread-safe</strong> - Safe for use from multiple threads</li>
 *   <li><strong>Export support</strong> - Can export history to JSON for later reference</li>
 * </ul>
 *
 * <h2>Usage Examples:</h2>
 * <pre>
 * DeletionHistory history = new DeletionHistory();
 *
 * // Record a deletion
 * history.recordDeletion("id1", "com/example/artifact.jar", 12345, "maven-releases");
 *
 * // Get recent deletions
 * List&lt;DeletedComponent&gt; recent = history.getRecentDeletions(10);
 *
 * // Export to JSON file
 * history.exportToJson(Path.of("deleted-2026-06-03.json"));
 *
 * // Clear history
 * history.clear();
 * </pre>
 *
 * @author sfloess
 * @since 2.1
 * @see NexusService
 */
public class DeletionHistory {
    private static final Logger logger = LoggerFactory.getLogger(DeletionHistory.class);

    /**
     * Default maximum number of deletion records to retain.
     */
    public static final int DEFAULT_MAX_HISTORY = 1000;

    /**
     * Jackson ObjectMapper configured for JSON serialization with proper Instant handling.
     */
    private static final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private final Deque<DeletedComponent> history = new ArrayDeque<>();
    private final int maxHistory;

    /**
     * Represents a component that was deleted from a Nexus repository.
     *
     * @param id         the unique identifier of the deleted component
     * @param path       the path of the deleted component artifact
     * @param fileSize   the size of the deleted component in bytes
     * @param repository the name of the repository from which the component was deleted
     * @param deletedAt  the timestamp when the deletion occurred
     */
    public record DeletedComponent(
        String id,
        String path,
        long fileSize,
        String repository,
        Instant deletedAt
    ) {}

    /**
     * Constructs a new DeletionHistory with the default maximum size of 1000 entries.
     */
    public DeletionHistory() {
        this(DEFAULT_MAX_HISTORY);
    }

    /**
     * Constructs a new DeletionHistory with the specified maximum size.
     *
     * @param maxHistory the maximum number of deletion records to retain
     * @throws IllegalArgumentException if maxHistory is less than 1
     */
    public DeletionHistory(int maxHistory) {
        if (maxHistory < 1) {
            throw new IllegalArgumentException("maxHistory must be at least 1, got: " + maxHistory);
        }
        this.maxHistory = maxHistory;
    }

    /**
     * Records a component deletion in the history.
     * <p>
     * If the history has reached its maximum size, the oldest entry is removed
     * to make room for the new one.
     * </p>
     *
     * @param id         the unique identifier of the deleted component
     * @param path       the path of the deleted component artifact
     * @param fileSize   the size of the deleted component in bytes
     * @param repository the name of the repository from which the component was deleted
     */
    public synchronized void recordDeletion(String id, String path, long fileSize, String repository) {
        if (history.size() >= maxHistory) {
            history.removeFirst(); // Remove oldest
        }
        history.addLast(new DeletedComponent(id, path, fileSize, repository, Instant.now()));
        logger.debug("Recorded deletion: {} from {} ({} bytes)", path, repository, fileSize);
    }

    /**
     * Returns the most recent deletions, ordered from newest to oldest.
     *
     * @param limit the maximum number of records to return
     * @return a list of the most recent deleted components, newest first
     */
    public synchronized List<DeletedComponent> getRecentDeletions(int limit) {
        return history.reversed().stream()
            .limit(limit)
            .toList();
    }

    /**
     * Returns all deletions in the history, ordered from newest to oldest.
     *
     * @return a list of all deleted components, newest first
     */
    public synchronized List<DeletedComponent> getAllDeletions() {
        return history.reversed().stream().toList();
    }

    /**
     * Returns the total number of deletions recorded in the history.
     *
     * @return the number of deletion records
     */
    public synchronized int size() {
        return history.size();
    }

    /**
     * Returns true if the history contains no deletion records.
     *
     * @return true if no deletions have been recorded
     */
    public synchronized boolean isEmpty() {
        return history.isEmpty();
    }

    /**
     * Returns the maximum number of deletion records this history will retain.
     *
     * @return the maximum history size
     */
    public int getMaxHistory() {
        return maxHistory;
    }

    /**
     * Clears all deletion records from the history.
     */
    public synchronized void clear() {
        history.clear();
        logger.debug("Deletion history cleared");
    }

    /**
     * Returns the total size in bytes of all deleted components in the history.
     *
     * @return total size of deleted components in bytes
     */
    public synchronized long getTotalDeletedSize() {
        return history.stream()
            .mapToLong(DeletedComponent::fileSize)
            .sum();
    }

    /**
     * Returns deletions filtered by repository name.
     *
     * @param repository the repository name to filter by
     * @return a list of deleted components from the specified repository, newest first
     */
    public synchronized List<DeletedComponent> getDeletionsByRepository(String repository) {
        return history.reversed().stream()
            .filter(d -> d.repository().equals(repository))
            .toList();
    }

    /**
     * Exports the deletion history to a JSON file.
     * <p>
     * The exported file contains all deletion records with metadata for
     * later reference and potential manual recovery.
     * </p>
     *
     * @param outputPath the path to write the JSON file to
     * @throws IOException if the file cannot be written
     */
    public synchronized void exportToJson(Path outputPath) throws IOException {
        List<DeletedComponent> deletions = getAllDeletions();
        String json = formatAsJson(deletions, null, null);

        // Create parent directories if they exist
        if (outputPath.getParent() != null) {
            Files.createDirectories(outputPath.getParent());
        }
        try (Writer writer = Files.newBufferedWriter(outputPath)) {
            writer.write(json);
        }
        logger.info("Exported {} deletion records to {}", deletions.size(), outputPath);
    }

    /**
     * Exports the deletion history to a JSON string.
     *
     * @return JSON string representing the deletion history
     */
    public synchronized String toJson() {
        return formatAsJson(getAllDeletions(), null, null);
    }

    /**
     * Formats a list of deleted components as a JSON string for pre-delete export.
     * <p>
     * This method is used to create an export file before deletions occur, including
     * the criteria used for the deletion operation.
     * </p>
     *
     * @param components  the components that will be (or were) deleted
     * @param repository  the repository name (for the export metadata)
     * @param regexFilter the regex filter used (can be null)
     * @return JSON string representing the deletion export
     */
    public static String formatAsJson(List<DeletedComponent> components, String repository, String regexFilter) {
        try {
            long totalSize = components.stream().mapToLong(DeletedComponent::fileSize).sum();
            ExportData export = new ExportData(
                Instant.now(),
                repository,
                regexFilter != null ? new ExportData.Criteria(regexFilter) : null,
                new ExportData.Summary(components.size(), totalSize),
                components
            );
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(export);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize deletion history to JSON", e);
            throw new RuntimeException("JSON serialization error", e);
        }
    }

    /**
     * Creates a list of DeletedComponent records from RepoRecords before deletion.
     * <p>
     * This is a convenience method for building export data from the component list
     * that is about to be deleted.
     * </p>
     *
     * @param records    the list of records to convert
     * @param repository the repository name
     * @return a list of DeletedComponent records with the current timestamp
     */
    public static List<DeletedComponent> fromRepoRecords(List<RepoRecord> records, String repository) {
        Instant now = Instant.now();
        return records.stream()
            .map(r -> new DeletedComponent(r.id(), r.path(), r.fileSize(), repository, now))
            .toList();
    }

    /**
     * Internal data structure for JSON export.
     */
    record ExportData(
        Instant exportedAt,
        String repository,
        Criteria criteria,
        Summary summary,
        List<DeletedComponent> components
    ) {
        /**
         * Deletion criteria for export metadata.
         */
        record Criteria(String regex) {}

        /**
         * Export summary statistics.
         */
        record Summary(
            int totalComponents,
            long totalSize
        ) {}
    }
}

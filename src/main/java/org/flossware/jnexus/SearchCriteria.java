package org.flossware.jnexus;

import java.time.Instant;

/**
 * Represents search and filter criteria for querying Nexus repository components.
 * <p>
 * This record encapsulates all available filter parameters including size ranges,
 * date ranges, file extensions, and pattern matching. All filter fields are optional
 * (nullable); a null value means no filtering on that criterion.
 * </p>
 *
 * @param repository            the name of the repository to search
 * @param regexFilter           optional regex pattern to filter component paths (null for no filter)
 * @param minSize               minimum file size in bytes (null for no minimum)
 * @param maxSize               maximum file size in bytes (null for no maximum)
 * @param createdAfter          filter components created after this timestamp (null for no filter)
 * @param createdBefore         filter components created before this timestamp (null for no filter)
 * @param fileExtension         filter by file extension including the dot (e.g., ".jar", null for no filter)
 * @param componentNamePattern  regex pattern for component name/group/version (null for no filter)
 * @author sfloess
 * @since 1.0
 */
public record SearchCriteria(
    String repository,
    String regexFilter,
    Long minSize,
    Long maxSize,
    Instant createdAfter,
    Instant createdBefore,
    String fileExtension,
    String componentNamePattern
) {
    /**
     * Validates the search criteria.
     * <p>
     * Ensures that:
     * </p>
     * <ul>
     *   <li>Repository is not null or blank</li>
     *   <li>If both minSize and maxSize are specified, minSize &lt;= maxSize</li>
     *   <li>If both date bounds are specified, createdAfter is before createdBefore</li>
     * </ul>
     *
     * @throws IllegalArgumentException if validation fails
     */
    public SearchCriteria {
        if (repository == null || repository.isBlank()) {
            throw new IllegalArgumentException("Repository cannot be null or blank");
        }

        if (minSize != null && minSize < 0) {
            throw new IllegalArgumentException("Minimum size cannot be negative");
        }

        if (maxSize != null && maxSize < 0) {
            throw new IllegalArgumentException("Maximum size cannot be negative");
        }

        if (minSize != null && maxSize != null && minSize > maxSize) {
            throw new IllegalArgumentException("Minimum size cannot be greater than maximum size");
        }

        if (createdAfter != null && createdBefore != null && createdAfter.isAfter(createdBefore)) {
            throw new IllegalArgumentException("Created-after date cannot be after created-before date");
        }
    }

    /**
     * Builder for constructing SearchCriteria with a fluent API.
     * <p>
     * Provides a readable, type-safe way to build complex search criteria
     * with multiple optional filters. Only the repository is required;
     * all other filters are optional.
     * </p>
     *
     * <h2>Usage Examples:</h2>
     * <pre>
     * // Simple repository search
     * SearchCriteria simple = new SearchCriteria.Builder()
     *     .repository("maven-releases")
     *     .build();
     *
     * // Filter by size range
     * SearchCriteria sizeFilter = new SearchCriteria.Builder()
     *     .repository("maven-releases")
     *     .minSize(1_000_000L)      // 1 MB minimum
     *     .maxSize(100_000_000L)    // 100 MB maximum
     *     .build();
     *
     * // Filter by date range
     * SearchCriteria dateFilter = new SearchCriteria.Builder()
     *     .repository("maven-snapshots")
     *     .createdAfter(Instant.parse("2024-01-01T00:00:00Z"))
     *     .createdBefore(Instant.parse("2024-12-31T23:59:59Z"))
     *     .build();
     *
     * // Filter by file extension
     * SearchCriteria extFilter = new SearchCriteria.Builder()
     *     .repository("maven-releases")
     *     .fileExtension(".jar")
     *     .build();
     *
     * // Complex multi-criteria search
     * SearchCriteria complex = new SearchCriteria.Builder()
     *     .repository("maven-releases")
     *     .regexFilter(".*com/example/.*")
     *     .minSize(1_000_000L)
     *     .maxSize(50_000_000L)
     *     .createdAfter(Instant.now().minus(Duration.ofDays(30)))
     *     .fileExtension(".jar")
     *     .componentNamePattern(".*-SNAPSHOT.*")
     *     .build();
     *
     * // Use with NexusService
     * NexusService service = new NexusService(client);
     * List&lt;ComponentMetadata&gt; results = service.searchComponents(complex, false);
     * </pre>
     *
     * @since 1.0
     */
    public static class Builder {
        private String repository;
        private String regexFilter;
        private Long minSize;
        private Long maxSize;
        private Instant createdAfter;
        private Instant createdBefore;
        private String fileExtension;
        private String componentNamePattern;

        /**
         * Creates a new Builder instance.
         */
        public Builder() {
        }

        /**
         * Sets the repository name (required).
         *
         * @param repository the repository name
         * @return this builder
         */
        public Builder repository(String repository) {
            this.repository = repository;
            return this;
        }

        /**
         * Sets the regex filter for component paths (optional).
         *
         * @param regexFilter the regex pattern
         * @return this builder
         */
        public Builder regexFilter(String regexFilter) {
            this.regexFilter = regexFilter;
            return this;
        }

        /**
         * Sets the minimum file size in bytes (optional).
         *
         * @param minSize the minimum size
         * @return this builder
         */
        public Builder minSize(Long minSize) {
            this.minSize = minSize;
            return this;
        }

        /**
         * Sets the maximum file size in bytes (optional).
         *
         * @param maxSize the maximum size
         * @return this builder
         */
        public Builder maxSize(Long maxSize) {
            this.maxSize = maxSize;
            return this;
        }

        /**
         * Sets the created-after date filter (optional).
         *
         * @param createdAfter components created after this date
         * @return this builder
         */
        public Builder createdAfter(Instant createdAfter) {
            this.createdAfter = createdAfter;
            return this;
        }

        /**
         * Sets the created-before date filter (optional).
         *
         * @param createdBefore components created before this date
         * @return this builder
         */
        public Builder createdBefore(Instant createdBefore) {
            this.createdBefore = createdBefore;
            return this;
        }

        /**
         * Sets the file extension filter (optional).
         *
         * @param fileExtension the extension including the dot (e.g., ".jar")
         * @return this builder
         */
        public Builder fileExtension(String fileExtension) {
            this.fileExtension = fileExtension;
            return this;
        }

        /**
         * Sets the component name pattern filter (optional).
         *
         * @param componentNamePattern regex pattern for component ID
         * @return this builder
         */
        public Builder componentNamePattern(String componentNamePattern) {
            this.componentNamePattern = componentNamePattern;
            return this;
        }

        /**
         * Builds and validates the SearchCriteria.
         *
         * @return a new SearchCriteria instance
         * @throws IllegalArgumentException if validation fails
         */
        public SearchCriteria build() {
            return new SearchCriteria(
                repository,
                regexFilter,
                minSize,
                maxSize,
                createdAfter,
                createdBefore,
                fileExtension,
                componentNamePattern
            );
        }
    }

    /**
     * Creates a new builder for SearchCriteria.
     *
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Checks if any filters are active (non-null).
     *
     * @return true if at least one filter is specified, false if no filters
     */
    public boolean hasFilters() {
        return regexFilter != null
            || minSize != null
            || maxSize != null
            || createdAfter != null
            || createdBefore != null
            || fileExtension != null
            || componentNamePattern != null;
    }
}

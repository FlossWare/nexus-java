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

        public Builder repository(String repository) {
            this.repository = repository;
            return this;
        }

        public Builder regexFilter(String regexFilter) {
            this.regexFilter = regexFilter;
            return this;
        }

        public Builder minSize(Long minSize) {
            this.minSize = minSize;
            return this;
        }

        public Builder maxSize(Long maxSize) {
            this.maxSize = maxSize;
            return this;
        }

        public Builder createdAfter(Instant createdAfter) {
            this.createdAfter = createdAfter;
            return this;
        }

        public Builder createdBefore(Instant createdBefore) {
            this.createdBefore = createdBefore;
            return this;
        }

        public Builder fileExtension(String fileExtension) {
            this.fileExtension = fileExtension;
            return this;
        }

        public Builder componentNamePattern(String componentNamePattern) {
            this.componentNamePattern = componentNamePattern;
            return this;
        }

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

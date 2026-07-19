package org.flossware.jnexus;

import java.util.List;
import java.util.Map;

/**
 * Represents comprehensive statistics for a Nexus repository.
 * <p>
 * This record provides aggregate views of repository contents including
 * size distributions, file type breakdowns, age analysis, and largest components.
 * All statistics are calculated from a snapshot of repository components at a
 * specific point in time.
 * </p>
 *
 * @param repository          the name of the repository these statistics represent
 * @param totalComponents     total number of components in the repository
 * @param totalSize           total size of all components in bytes
 * @param averageSize         average component size in bytes
 * @param medianSize          median component size in bytes
 * @param sizeDistribution    components grouped by size ranges (range name → count)
 * @param fileTypeBreakdown   components grouped by file extension (extension → total size in bytes)
 * @param ageDistribution     components grouped by age (age range → count)
 * @param largestComponents   list of the largest components (up to top 20), sorted by size descending
 * @author sfloess
 * @since 1.0
 */
public record RepositoryStats(
    String repository,
    int totalComponents,
    long totalSize,
    long averageSize,
    long medianSize,
    Map<String, Integer> sizeDistribution,
    Map<String, Long> fileTypeBreakdown,
    Map<String, Integer> ageDistribution,
    List<ComponentMetadata> largestComponents
) {
    /**
     * Size range bucket names for distribution analysis.
     */
    public static final String SIZE_RANGE_UNDER_1MB = "< 1 MB";
    public static final String SIZE_RANGE_1_TO_10MB = "1-10 MB";
    public static final String SIZE_RANGE_10_TO_100MB = "10-100 MB";
    public static final String SIZE_RANGE_100MB_TO_1GB = "100 MB - 1 GB";
    public static final String SIZE_RANGE_OVER_1GB = "> 1 GB";

    /**
     * Age range bucket names for distribution analysis.
     */
    public static final String AGE_RANGE_LAST_7_DAYS = "Last 7 days";
    public static final String AGE_RANGE_LAST_30_DAYS = "Last 30 days";
    public static final String AGE_RANGE_LAST_90_DAYS = "Last 90 days";
    public static final String AGE_RANGE_OLDER = "Older than 90 days";

    /**
     * Compact constructor that validates the statistics.
     *
     * @throws IllegalArgumentException if validation fails
     */
    public RepositoryStats {
        if (repository == null || repository.isBlank()) {
            throw new IllegalArgumentException("Repository cannot be null or blank");
        }
        if (totalComponents < 0) {
            throw new IllegalArgumentException("Total components cannot be negative");
        }
        if (totalSize < 0) {
            throw new IllegalArgumentException("Total size cannot be negative");
        }
        if (averageSize < 0) {
            throw new IllegalArgumentException("Average size cannot be negative");
        }
        if (medianSize < 0) {
            throw new IllegalArgumentException("Median size cannot be negative");
        }
    }

    /**
     * Gets the total size in megabytes.
     *
     * @return total size in MB
     */
    public double getTotalSizeMB() {
        return totalSize / 1024.0 / 1024.0;
    }

    /**
     * Gets the total size in gigabytes.
     *
     * @return total size in GB
     */
    public double getTotalSizeGB() {
        return totalSize / 1024.0 / 1024.0 / 1024.0;
    }

    /**
     * Gets the average size in megabytes.
     *
     * @return average size in MB
     */
    public double getAverageSizeMB() {
        return averageSize / 1024.0 / 1024.0;
    }

    /**
     * Gets the median size in megabytes.
     *
     * @return median size in MB
     */
    public double getMedianSizeMB() {
        return medianSize / 1024.0 / 1024.0;
    }
}

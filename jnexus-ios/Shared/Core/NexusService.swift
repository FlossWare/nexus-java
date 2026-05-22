//
//  NexusService.swift
//  JNexus
//
//  Core business logic for Nexus operations
//  Ports the Java NexusService from jnexus-core
//

import Foundation

/// Service layer for Nexus repository operations
/// Handles filtering, statistics calculation, and batch operations
class NexusService {
    private let client: NexusHttpClient

    init(client: NexusHttpClient) {
        self.client = client
    }

    // MARK: - Search and Filtering

    /// Search components with advanced criteria
    /// - Parameters:
    ///   - criteria: Search filters to apply
    ///   - forceRefresh: Bypass cache if true
    /// - Returns: Filtered list of components with metadata
    func searchComponents(criteria: SearchCriteria, forceRefresh: Bool) async throws -> [ComponentMetadata] {
        var components = try await client.listComponentsWithMetadata(
            repository: criteria.repository,
            forceRefresh: forceRefresh
        )

        // Apply regex filter
        if let regex = criteria.regexFilter, !regex.isEmpty {
            components = try filterByRegex(components: components, pattern: regex)
        }

        // Apply size range filter
        if let min = criteria.minSize {
            components = components.filter { $0.fileSize >= min }
        }
        if let max = criteria.maxSize {
            components = components.filter { $0.fileSize <= max }
        }

        // Apply date range filter
        if let after = criteria.createdAfter {
            components = components.filter { component in
                guard let created = component.createdDate else { return false }
                return created >= after
            }
        }
        if let before = criteria.createdBefore {
            components = components.filter { component in
                guard let created = component.createdDate else { return false }
                return created <= before
            }
        }

        // Apply file extension filter
        if let ext = criteria.fileExtension, !ext.isEmpty {
            let extension = ext.hasPrefix(".") ? ext : ".\(ext)"
            components = components.filter { $0.path.hasSuffix(extension) }
        }

        // Apply component name pattern
        if let pattern = criteria.componentNamePattern, !pattern.isEmpty {
            components = try filterByRegex(components: components, pattern: pattern)
        }

        return components
    }

    /// Filter components by regex pattern
    private func filterByRegex(components: [ComponentMetadata], pattern: String) throws -> [ComponentMetadata] {
        let regex = try NSRegularExpression(pattern: pattern, options: [])
        return components.filter { component in
            let range = NSRange(component.path.startIndex..., in: component.path)
            return regex.firstMatch(in: component.path, options: [], range: range) != nil
        }
    }

    // MARK: - Statistics

    /// Calculate comprehensive repository statistics
    /// - Parameters:
    ///   - repository: Repository name
    ///   - components: List of components to analyze
    /// - Returns: Statistics object
    func calculateStatistics(repository: String, components: [ComponentMetadata]) -> RepositoryStats {
        let totalComponents = components.count
        let totalSize = components.reduce(0) { $0 + $1.fileSize }
        let averageSize = totalComponents > 0 ? totalSize / Int64(totalComponents) : 0

        // Calculate median
        let sortedSizes = components.map { $0.fileSize }.sorted()
        let medianSize: Int64
        if sortedSizes.isEmpty {
            medianSize = 0
        } else if sortedSizes.count % 2 == 0 {
            let mid = sortedSizes.count / 2
            medianSize = (sortedSizes[mid - 1] + sortedSizes[mid]) / 2
        } else {
            medianSize = sortedSizes[sortedSizes.count / 2]
        }

        // Size distribution buckets
        let sizeDistribution = calculateSizeDistribution(components: components)

        // File type breakdown
        let fileTypeBreakdown = calculateFileTypeBreakdown(components: components)

        // Age distribution
        let ageDistribution = calculateAgeDistribution(components: components)

        // Largest components (top 20)
        let largestComponents = Array(components.sorted { $0.fileSize > $1.fileSize }.prefix(20))

        return RepositoryStats(
            totalComponents: totalComponents,
            totalSize: totalSize,
            averageSize: averageSize,
            medianSize: medianSize,
            sizeDistribution: sizeDistribution,
            fileTypeBreakdown: fileTypeBreakdown,
            ageDistribution: ageDistribution,
            largestComponents: largestComponents
        )
    }

    private func calculateSizeDistribution(components: [ComponentMetadata]) -> [String: Int] {
        var distribution: [String: Int] = [
            "<1MB": 0,
            "1-10MB": 0,
            "10-100MB": 0,
            "100MB-1GB": 0,
            ">1GB": 0
        ]

        for component in components {
            let sizeMB = Double(component.fileSize) / 1_048_576
            if sizeMB < 1 {
                distribution["<1MB"]! += 1
            } else if sizeMB < 10 {
                distribution["1-10MB"]! += 1
            } else if sizeMB < 100 {
                distribution["10-100MB"]! += 1
            } else if sizeMB < 1024 {
                distribution["100MB-1GB"]! += 1
            } else {
                distribution[">1GB"]! += 1
            }
        }

        return distribution
    }

    private func calculateFileTypeBreakdown(components: [ComponentMetadata]) -> [String: Int64] {
        var breakdown: [String: Int64] = [:]

        for component in components {
            let ext = (component.path as NSString).pathExtension
            let extension = ext.isEmpty ? "(no extension)" : ".\(ext)"
            breakdown[extension, default: 0] += component.fileSize
        }

        return breakdown
    }

    private func calculateAgeDistribution(components: [ComponentMetadata]) -> [String: Int] {
        var distribution: [String: Int] = [
            "Last 7 days": 0,
            "Last 30 days": 0,
            "Last 90 days": 0,
            "Older than 90 days": 0
        ]

        let now = Date()
        let sevenDaysAgo = Calendar.current.date(byAdding: .day, value: -7, to: now)!
        let thirtyDaysAgo = Calendar.current.date(byAdding: .day, value: -30, to: now)!
        let ninetyDaysAgo = Calendar.current.date(byAdding: .day, value: -90, to: now)!

        for component in components {
            guard let created = component.createdDate else { continue }

            if created >= sevenDaysAgo {
                distribution["Last 7 days"]! += 1
            } else if created >= thirtyDaysAgo {
                distribution["Last 30 days"]! += 1
            } else if created >= ninetyDaysAgo {
                distribution["Last 90 days"]! += 1
            } else {
                distribution["Older than 90 days"]! += 1
            }
        }

        return distribution
    }

    // MARK: - Delete Operations

    /// Delete multiple components matching regex filter
    /// - Parameters:
    ///   - repository: Repository name
    ///   - regexFilter: Pattern to match
    ///   - dryRun: If true, don't actually delete
    /// - Returns: List of deleted (or would-be-deleted) component IDs
    func deleteComponents(repository: String, regexFilter: String?, dryRun: Bool) async throws -> [String] {
        var components = try await client.listComponentsWithMetadata(
            repository: repository,
            forceRefresh: true  // Always fresh for delete
        )

        // Apply regex filter if provided
        if let regex = regexFilter, !regex.isEmpty {
            components = try filterByRegex(components: components, pattern: regex)
        }

        var deletedIds: [String] = []

        for component in components {
            if !dryRun {
                try await client.deleteComponent(componentId: component.id)
            }
            deletedIds.append(component.id)
        }

        // Clear cache after successful deletion
        if !dryRun {
            client.clearCache(repository: repository)
        }

        return deletedIds
    }
}

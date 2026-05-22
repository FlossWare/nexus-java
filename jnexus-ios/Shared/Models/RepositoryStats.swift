//
//  RepositoryStats.swift
//  JNexus
//
//  Repository statistics and analytics
//  Mirrors the Java RepositoryStats from jnexus-core
//

import Foundation

/// Comprehensive repository statistics
struct RepositoryStats: Codable {
    let totalComponents: Int
    let totalSize: Int64
    let averageSize: Int64
    let medianSize: Int64
    let sizeDistribution: [String: Int]  // bucket name -> count
    let fileTypeBreakdown: [String: Int64]  // extension -> total size
    let ageDistribution: [String: Int]  // age range -> count
    let largestComponents: [ComponentMetadata]

    /// Total size in megabytes
    var totalSizeMB: Double {
        Double(totalSize) / 1_048_576
    }

    /// Total size in gigabytes
    var totalSizeGB: Double {
        Double(totalSize) / 1_073_741_824
    }

    /// Average size in megabytes
    var averageSizeMB: Double {
        Double(averageSize) / 1_048_576
    }

    /// Median size in megabytes
    var medianSizeMB: Double {
        Double(medianSize) / 1_048_576
    }

    /// CodingKeys for JSON serialization
    enum CodingKeys: String, CodingKey {
        case totalComponents
        case totalSize
        case averageSize
        case medianSize
        case sizeDistribution
        case fileTypeBreakdown
        case ageDistribution
        case largestComponents
    }
}

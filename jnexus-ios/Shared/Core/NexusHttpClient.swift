//
//  NexusHttpClient.swift
//  JNexus
//
//  Created by FlossWare on 2026-05-22.
//  Copyright © 2026 FlossWare. All rights reserved.
//
//  Platform-agnostic HTTP client protocol for Nexus API
//  iOS implementation: NexusClientURLSession
//

import Foundation

/// Protocol defining HTTP operations for Nexus Repository Manager API.
///
/// This protocol mirrors the Java `NexusHttpClient` interface from jnexus-core, providing
/// a platform-agnostic abstraction for HTTP communication with Nexus API.
///
/// Implementations must provide:
/// - Caching with configurable TTL (default 5 minutes)
/// - Retry logic with exponential backoff for transient failures
/// - Pagination handling via continuation tokens
/// - HTTP Basic Authentication
///
/// - Note: iOS/macOS implementation uses URLSession
/// - SeeAlso: `NexusClientURLSession`
protocol NexusHttpClient {
    /// Lists components in a repository.
    ///
    /// Fetches all components from the specified repository, handling pagination automatically
    /// via continuation tokens. Results are cached with a 5-minute TTL unless `forceRefresh` is true.
    ///
    /// - Parameters:
    ///   - repository: Repository name (e.g., "maven-releases")
    ///   - forceRefresh: If true, bypass cache and fetch fresh data from API
    /// - Returns: Array of basic component records with id, fileSize, and path
    /// - Throws: `NexusError` if HTTP request fails or response cannot be parsed
    func listComponents(repository: String, forceRefresh: Bool) async throws -> [RepoRecord]

    /// Lists components with full metadata.
    ///
    /// Similar to `listComponents()` but includes additional metadata fields:
    /// contentType, format, createdDate, lastModified, and checksum.
    ///
    /// - Parameters:
    ///   - repository: Repository name (e.g., "maven-releases")
    ///   - forceRefresh: If true, bypass cache and fetch fresh data from API
    /// - Returns: Array of components with detailed metadata
    /// - Throws: `NexusError` if HTTP request fails or response cannot be parsed
    func listComponentsWithMetadata(repository: String, forceRefresh: Bool) async throws -> [ComponentMetadata]

    /// Deletes a component by its unique identifier.
    ///
    /// Sends a DELETE request to the Nexus API. This operation is permanent and cannot be undone.
    ///
    /// - Parameter componentId: Unique component identifier from Nexus API
    /// - Throws: `NexusError` if deletion fails (network error, 404, permission denied, etc.)
    func deleteComponent(componentId: String) async throws

    /// Clears cache for a specific repository.
    ///
    /// Removes cached component data for the given repository. Next fetch will query the API.
    ///
    /// - Parameter repository: Repository name to clear from cache
    func clearCache(repository: String)

    /// Clears all cached data across all repositories.
    ///
    /// Removes all cached components from memory. Useful for freeing memory or forcing fresh data.
    func clearAllCache()

    /// Checks if a repository has valid cached data.
    ///
    /// Returns true if the repository is cached and the cache has not expired (within TTL).
    ///
    /// - Parameter repository: Repository name to check
    /// - Returns: True if cached and not expired, false otherwise
    func isCached(repository: String) -> Bool

    /// Returns the age of cached data for a repository.
    ///
    /// Useful for displaying cache status to users.
    ///
    /// - Parameter repository: Repository name
    /// - Returns: Seconds since last cache update, or nil if not cached
    func getCacheAge(repository: String) -> TimeInterval?
}

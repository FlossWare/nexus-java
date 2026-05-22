//
//  Credentials.swift
//  JNexus
//
//  Created by FlossWare on 2026-05-22.
//  Copyright © 2026 FlossWare. All rights reserved.
//
//  Platform-agnostic credentials provider protocol
//  iOS implementation: CredentialsKeychain
//

import Foundation

/// Protocol defining credential storage and retrieval for Nexus authentication.
///
/// This protocol mirrors the Java `Credentials` interface from jnexus-core, providing
/// a platform-agnostic abstraction for credential management.
///
/// Implementations must provide:
/// - Secure storage of sensitive data (URL, username, password)
/// - Persistent storage of settings and defaults
/// - Thread-safe access to all properties
///
/// - Note: iOS/macOS implementation uses Keychain for credentials, UserDefaults for settings
/// - SeeAlso: `CredentialsKeychain`
protocol Credentials {
    // MARK: - Read Properties

    /// Nexus server URL (e.g., "https://nexus.example.com").
    ///
    /// - Returns: Server URL if configured, nil otherwise
    var url: String? { get }

    /// Nexus username for authentication.
    ///
    /// - Returns: Username if configured, nil otherwise
    var user: String? { get }

    /// Nexus password for authentication.
    ///
    /// - Returns: Password if configured, nil otherwise
    /// - Important: Stored securely in Keychain (iOS/macOS)
    var password: String? { get }

    /// Configuration profile name for multi-environment support.
    ///
    /// Profiles allow different credentials for dev, staging, prod, etc.
    ///
    /// - Returns: Profile name (e.g., "dev", "prod"), or nil for default profile
    var profile: String? { get }

    /// List of repository names configured for this Nexus instance.
    ///
    /// Used for repository dropdowns and batch operations.
    ///
    /// - Returns: Array of repository names, empty array if none configured
    var repositories: [String] { get }

    /// Default repository name for pre-populating UI fields.
    ///
    /// - Returns: Repository name, or empty string if not configured
    var defaultRepository: String { get }

    /// Default regex filter pattern for component filtering.
    ///
    /// - Returns: Regex pattern string, or empty string if not configured
    var defaultRegex: String { get }

    /// Default dry-run mode flag for delete operations.
    ///
    /// When true, delete operations show what would be deleted without actually deleting.
    ///
    /// - Returns: True for dry-run by default, false for actual deletion
    var defaultDryRun: Bool { get }

    /// HTTP request timeout in seconds.
    ///
    /// - Returns: Timeout value in seconds, defaults to 30 if not configured
    var httpTimeoutSeconds: Int { get }

    // MARK: - Methods

    /// Check if all required credentials are configured.
    ///
    /// Required fields: URL, username, and password.
    ///
    /// - Returns: True if all required credentials are present and non-empty
    func hasCredentials() -> Bool

    // MARK: - Write Methods

    /// Save Nexus connection credentials.
    ///
    /// - Parameters:
    ///   - url: Nexus server URL (e.g., "https://nexus.example.com")
    ///   - user: Username for authentication
    ///   - password: Password for authentication
    /// - Throws: `KeychainError` if Keychain storage fails
    func saveCredentials(url: String, user: String, password: String) throws

    /// Save repository list for this Nexus instance.
    ///
    /// - Parameter repositories: Array of repository names
    /// - Throws: Storage error (rarely happens with UserDefaults)
    func saveRepositories(_ repositories: [String]) throws

    /// Save default values for UI pre-population.
    ///
    /// - Parameters:
    ///   - repository: Default repository name
    ///   - regex: Default regex pattern
    ///   - dryRun: Default dry-run flag
    /// - Throws: Storage error (rarely happens with UserDefaults)
    func saveDefaults(repository: String, regex: String, dryRun: Bool) throws

    /// Save HTTP timeout configuration.
    ///
    /// - Parameter seconds: Timeout duration in seconds (typically 10-300)
    /// - Throws: Storage error (rarely happens with UserDefaults)
    func saveHttpTimeout(_ seconds: Int) throws

    /// Save profile name for multi-environment configuration.
    ///
    /// - Parameter profile: Profile name (e.g., "dev", "staging", "prod")
    /// - Throws: Storage error (rarely happens with UserDefaults)
    func saveProfile(_ profile: String) throws

    /// Clear all stored credentials and settings.
    ///
    /// Removes all data from Keychain and UserDefaults. Use for logout or reset.
    ///
    /// - Throws: `KeychainError` if Keychain deletion fails
    func clearAll() throws
}

//
//  CredentialsKeychain.swift
//  JNexus
//
//  Keychain-based credentials storage implementation
//  Uses iOS Keychain for secure encrypted storage
//

import Foundation
import Security

/// Keychain implementation of Credentials protocol
/// Stores sensitive data in iOS Keychain, non-sensitive in UserDefaults
class CredentialsKeychain: Credentials {
    private let keychainService = "org.flossware.jnexus"
    private let defaults = UserDefaults.standard

    // MARK: - Read Properties

    var url: String? {
        getKeychainItem(key: "nexus.url")
    }

    var user: String? {
        getKeychainItem(key: "nexus.user")
    }

    var password: String? {
        getKeychainItem(key: "nexus.password")
    }

    var profile: String? {
        defaults.string(forKey: "nexus.profile")
    }

    var repositories: [String] {
        defaults.stringArray(forKey: "nexus.repositories") ?? []
    }

    var defaultRepository: String {
        defaults.string(forKey: "nexus.default.repository") ?? ""
    }

    var defaultRegex: String {
        defaults.string(forKey: "nexus.default.regex") ?? ""
    }

    var defaultDryRun: Bool {
        defaults.bool(forKey: "nexus.default.dryrun")  // false if not set
    }

    var httpTimeoutSeconds: Int {
        let value = defaults.integer(forKey: "nexus.http.timeout.seconds")
        return value > 0 ? value : 30  // default 30 seconds
    }

    // MARK: - Methods

    func hasCredentials() -> Bool {
        url != nil && user != nil && password != nil
    }

    func saveCredentials(url: String, user: String, password: String) throws {
        try setKeychainItem(key: "nexus.url", value: url)
        try setKeychainItem(key: "nexus.user", value: user)
        try setKeychainItem(key: "nexus.password", value: password)
    }

    func saveRepositories(_ repositories: [String]) throws {
        defaults.set(repositories, forKey: "nexus.repositories")
    }

    func saveDefaults(repository: String, regex: String, dryRun: Bool) throws {
        defaults.set(repository, forKey: "nexus.default.repository")
        defaults.set(regex, forKey: "nexus.default.regex")
        defaults.set(dryRun, forKey: "nexus.default.dryrun")
    }

    func saveHttpTimeout(_ seconds: Int) throws {
        defaults.set(seconds, forKey: "nexus.http.timeout.seconds")
    }

    func saveProfile(_ profile: String) throws {
        defaults.set(profile, forKey: "nexus.profile")
    }

    func clearAll() throws {
        // Clear keychain items
        try? deleteKeychainItem(key: "nexus.url")
        try? deleteKeychainItem(key: "nexus.user")
        try? deleteKeychainItem(key: "nexus.password")

        // Clear UserDefaults
        defaults.removeObject(forKey: "nexus.profile")
        defaults.removeObject(forKey: "nexus.repositories")
        defaults.removeObject(forKey: "nexus.default.repository")
        defaults.removeObject(forKey: "nexus.default.regex")
        defaults.removeObject(forKey: "nexus.default.dryrun")
        defaults.removeObject(forKey: "nexus.http.timeout.seconds")
    }

    // MARK: - Keychain Helper Methods

    private func getKeychainItem(key: String) -> String? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: keychainService,
            kSecAttrAccount as String: key,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]

        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)

        guard status == errSecSuccess,
              let data = result as? Data,
              let value = String(data: data, encoding: .utf8) else {
            return nil
        }

        return value
    }

    private func setKeychainItem(key: String, value: String) throws {
        guard let data = value.data(using: .utf8) else {
            throw KeychainError.encodingFailed
        }

        // Delete existing item first
        try? deleteKeychainItem(key: key)

        // Add new item
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: keychainService,
            kSecAttrAccount as String: key,
            kSecValueData as String: data,
            kSecAttrAccessible as String: kSecAttrAccessibleWhenUnlocked
        ]

        let status = SecItemAdd(query as CFDictionary, nil)
        guard status == errSecSuccess else {
            throw KeychainError.saveFailed(status)
        }
    }

    private func deleteKeychainItem(key: String) throws {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: keychainService,
            kSecAttrAccount as String: key
        ]

        let status = SecItemDelete(query as CFDictionary)
        guard status == errSecSuccess || status == errSecItemNotFound else {
            throw KeychainError.deleteFailed(status)
        }
    }
}

// MARK: - Error Types

enum KeychainError: Error, LocalizedError {
    case encodingFailed
    case saveFailed(OSStatus)
    case deleteFailed(OSStatus)

    var errorDescription: String? {
        switch self {
        case .encodingFailed:
            return "Failed to encode value"
        case .saveFailed(let status):
            return "Failed to save to keychain: \(status)"
        case .deleteFailed(let status):
            return "Failed to delete from keychain: \(status)"
        }
    }
}

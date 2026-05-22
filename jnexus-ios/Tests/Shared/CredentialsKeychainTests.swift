//
//  CredentialsKeychainTests.swift
//  JNexusTests
//
//  Unit tests for Keychain credentials storage
//

import XCTest
@testable import JNexus

class CredentialsKeychainTests: XCTestCase {
    var credentials: CredentialsKeychain!

    override func setUp() {
        super.setUp()
        credentials = CredentialsKeychain()
        try? credentials.clearAll()  // Clean slate for each test
    }

    override func tearDown() {
        try? credentials.clearAll()
        credentials = nil
        super.tearDown()
    }

    // MARK: - Save and Load Tests

    func testSaveAndLoadCredentials() throws {
        // Save credentials
        try credentials.saveCredentials(
            url: "https://nexus.example.com",
            user: "testuser",
            password: "testpass"
        )

        // Verify loaded correctly
        XCTAssertEqual(credentials.url, "https://nexus.example.com")
        XCTAssertEqual(credentials.user, "testuser")
        XCTAssertEqual(credentials.password, "testpass")
    }

    func testHasCredentials() throws {
        // Initially no credentials
        XCTAssertFalse(credentials.hasCredentials())

        // Save partial credentials
        try credentials.saveCredentials(url: "https://nexus.example.com", user: "testuser", password: "")
        XCTAssertFalse(credentials.hasCredentials())  // Missing password

        // Save complete credentials
        try credentials.saveCredentials(
            url: "https://nexus.example.com",
            user: "testuser",
            password: "testpass"
        )
        XCTAssertTrue(credentials.hasCredentials())
    }

    // MARK: - Repository Tests

    func testSaveAndLoadRepositories() throws {
        let repos = ["maven-releases", "maven-snapshots", "npm-public"]
        try credentials.saveRepositories(repos)

        XCTAssertEqual(credentials.repositories, repos)
    }

    func testEmptyRepositories() {
        XCTAssertEqual(credentials.repositories, [])
    }

    // MARK: - Defaults Tests

    func testSaveAndLoadDefaults() throws {
        try credentials.saveDefaults(
            repository: "maven-releases",
            regex: ".*SNAPSHOT.*",
            dryRun: false
        )

        XCTAssertEqual(credentials.defaultRepository, "maven-releases")
        XCTAssertEqual(credentials.defaultRegex, ".*SNAPSHOT.*")
        XCTAssertEqual(credentials.defaultDryRun, false)
    }

    func testDefaultValues() {
        // Before saving, should return sensible defaults
        XCTAssertEqual(credentials.defaultRepository, "")
        XCTAssertEqual(credentials.defaultRegex, "")
        XCTAssertEqual(credentials.defaultDryRun, false)
        XCTAssertEqual(credentials.httpTimeoutSeconds, 30)
    }

    // MARK: - HTTP Timeout Tests

    func testSaveAndLoadHttpTimeout() throws {
        try credentials.saveHttpTimeout(60)
        XCTAssertEqual(credentials.httpTimeoutSeconds, 60)
    }

    // MARK: - Clear Tests

    func testClearAll() throws {
        // Save some data
        try credentials.saveCredentials(url: "https://nexus.example.com", user: "testuser", password: "testpass")
        try credentials.saveRepositories(["maven-releases"])
        try credentials.saveDefaults(repository: "maven-releases", regex: ".*", dryRun: false)

        // Clear all
        try credentials.clearAll()

        // Verify all cleared
        XCTAssertNil(credentials.url)
        XCTAssertNil(credentials.user)
        XCTAssertNil(credentials.password)
        XCTAssertEqual(credentials.repositories, [])
        XCTAssertEqual(credentials.defaultRepository, "")
    }

    // MARK: - Profile Tests

    func testSaveAndLoadProfile() throws {
        try credentials.saveProfile("production")
        XCTAssertEqual(credentials.profile, "production")

        try credentials.saveProfile("dev")
        XCTAssertEqual(credentials.profile, "dev")
    }
}

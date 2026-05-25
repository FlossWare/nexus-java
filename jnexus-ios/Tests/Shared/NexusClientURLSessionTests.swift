//
//  NexusClientURLSessionTests.swift
//  JNexusTests
//
//  Unit tests for URLSession HTTP client
//

import XCTest
@testable import JNexus

class NexusClientURLSessionTests: XCTestCase {
    var credentials: MockCredentials!
    var mockSession: URLSession!
    var requestCount: Int = 0

    override func setUp() {
        super.setUp()
        credentials = MockCredentials()
        requestCount = 0

        // Configure URLSession with mock protocol
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [MockURLProtocol.self]
        mockSession = URLSession(configuration: config)
    }

    override func tearDown() {
        mockSession = nil
        credentials = nil
        MockURLProtocol.requestHandler = nil
        super.tearDown()
    }

    // MARK: - Caching Tests

    func testCacheHit() async throws {
        // Setup: Mock successful response
        MockURLProtocol.requestHandler = { [weak self] request in
            self?.requestCount += 1
            let response = HTTPURLResponse(
                url: request.url!,
                statusCode: 200,
                httpVersion: nil,
                headerFields: nil
            )!
            let json = """
            {
                "items": [
                    {"id": "test1", "repository": "test-repo", "assets": [{"path": "test.jar", "fileSize": 1000}]}
                ],
                "continuationToken": null
            }
            """
            return (response, json.data(using: .utf8)!)
        }

        let client = NexusClientURLSession(credentials: credentials, session: mockSession)

        // First fetch - should make HTTP request
        let records1 = try await client.listComponents(repository: "test-repo", forceRefresh: false)
        XCTAssertEqual(records1.count, 1)
        XCTAssertEqual(requestCount, 1, "First fetch should make HTTP request")
        XCTAssertTrue(client.isCached(repository: "test-repo"), "Repository should be cached")

        // Second fetch - should use cache (no new HTTP request)
        let records2 = try await client.listComponents(repository: "test-repo", forceRefresh: false)
        XCTAssertEqual(records2.count, 1)
        XCTAssertEqual(requestCount, 1, "Second fetch should use cache, no new request")
    }

    func testCacheMiss() async throws {
        // Setup: Mock successful response
        MockURLProtocol.requestHandler = { [weak self] request in
            self?.requestCount += 1
            let response = HTTPURLResponse(
                url: request.url!,
                statusCode: 200,
                httpVersion: nil,
                headerFields: nil
            )!
            let json = """
            {
                "items": [
                    {"id": "test1", "repository": "test-repo", "assets": [{"path": "test.jar", "fileSize": 1000}]}
                ],
                "continuationToken": null
            }
            """
            return (response, json.data(using: .utf8)!)
        }

        let client = NexusClientURLSession(credentials: credentials, session: mockSession)

        // First fetch
        _ = try await client.listComponents(repository: "test-repo", forceRefresh: false)
        XCTAssertEqual(requestCount, 1)

        // Second fetch with forceRefresh - should bypass cache
        _ = try await client.listComponents(repository: "test-repo", forceRefresh: true)
        XCTAssertEqual(requestCount, 2, "forceRefresh should bypass cache and make new request")
    }

    func testCacheExpiry() async throws {
        // Setup: Mock successful response
        MockURLProtocol.requestHandler = { [weak self] request in
            self?.requestCount += 1
            let response = HTTPURLResponse(
                url: request.url!,
                statusCode: 200,
                httpVersion: nil,
                headerFields: nil
            )!
            let json = """
            {
                "items": [
                    {"id": "test1", "repository": "test-repo", "assets": [{"path": "test.jar", "fileSize": 1000}]}
                ],
                "continuationToken": null
            }
            """
            return (response, json.data(using: .utf8)!)
        }

        // Create client with 1-second cache TTL for testing
        let client = NexusClientURLSession(credentials: credentials, session: mockSession, cacheTTL: 1.0)

        // First fetch
        _ = try await client.listComponents(repository: "test-repo", forceRefresh: false)
        XCTAssertEqual(requestCount, 1)
        XCTAssertTrue(client.isCached(repository: "test-repo"))

        // Wait for cache to expire
        try await Task.sleep(nanoseconds: 1_500_000_000) // 1.5 seconds

        XCTAssertFalse(client.isCached(repository: "test-repo"), "Cache should have expired")

        // Third fetch - should make new request due to expiry
        _ = try await client.listComponents(repository: "test-repo", forceRefresh: false)
        XCTAssertEqual(requestCount, 2, "Expired cache should trigger new request")
    }

    // MARK: - Retry Logic Tests

    func testRetryOnTimeout() async throws {
        // Setup: Mock timeout error
        MockURLProtocol.requestHandler = { [weak self] request in
            self?.requestCount += 1
            throw URLError(.timedOut)
        }

        let client = NexusClientURLSession(credentials: credentials, session: mockSession)

        // Attempt fetch - should retry 3 times then fail
        do {
            _ = try await client.listComponents(repository: "test-repo", forceRefresh: false)
            XCTFail("Should have thrown timeout error")
        } catch {
            XCTAssertEqual(requestCount, 3, "Should retry 3 times on timeout")
            XCTAssertTrue(error is URLError)
        }
    }

    func testNoRetryOn4xx() async throws {
        // Setup: Mock 404 error
        MockURLProtocol.requestHandler = { [weak self] request in
            self?.requestCount += 1
            let response = HTTPURLResponse(
                url: request.url!,
                statusCode: 404,
                httpVersion: nil,
                headerFields: nil
            )!
            return (response, Data())
        }

        let client = NexusClientURLSession(credentials: credentials, session: mockSession)

        // Attempt fetch - should fail immediately without retry
        do {
            _ = try await client.listComponents(repository: "test-repo", forceRefresh: false)
            XCTFail("Should have thrown HTTP 404 error")
        } catch {
            XCTAssertEqual(requestCount, 1, "Should not retry on 4xx errors")
        }
    }

    // MARK: - Pagination Tests

    func testPagination() async throws {
        var pageNumber = 0

        // Setup: Mock paginated response
        MockURLProtocol.requestHandler = { [weak self] request in
            self?.requestCount += 1
            pageNumber += 1

            let response = HTTPURLResponse(
                url: request.url!,
                statusCode: 200,
                httpVersion: nil,
                headerFields: nil
            )!

            // First page has continuation token, second page doesn't
            let json: String
            if pageNumber == 1 {
                json = """
                {
                    "items": [
                        {"id": "item1", "repository": "test-repo", "assets": [{"path": "item1.jar", "fileSize": 1000}]}
                    ],
                    "continuationToken": "page2token"
                }
                """
            } else {
                json = """
                {
                    "items": [
                        {"id": "item2", "repository": "test-repo", "assets": [{"path": "item2.jar", "fileSize": 2000}]}
                    ],
                    "continuationToken": null
                }
                """
            }

            return (response, json.data(using: .utf8)!)
        }

        let client = NexusClientURLSession(credentials: credentials, session: mockSession)

        // Fetch all pages
        let records = try await client.listComponents(repository: "test-repo", forceRefresh: false)

        XCTAssertEqual(requestCount, 2, "Should make 2 requests for 2 pages")
        XCTAssertEqual(records.count, 2, "Should combine results from both pages")
        XCTAssertEqual(records[0].id, "item1")
        XCTAssertEqual(records[1].id, "item2")
    }

    // MARK: - JSON Parsing Tests

    func testParseBasicResponse() throws {
        let json = """
        {
            "items": [
                {
                    "id": "component-123",
                    "repository": "maven-releases",
                    "assets": [
                        {
                            "path": "org/example/artifact/1.0.0/artifact-1.0.0.jar",
                            "fileSize": 12345
                        }
                    ]
                }
            ],
            "continuationToken": null
        }
        """

        let data = json.data(using: .utf8)!
        let decoder = JSONDecoder()

        struct Response: Codable {
            let items: [Item]
            let continuationToken: String?

            struct Item: Codable {
                let id: String
                let repository: String
                let assets: [Asset]

                struct Asset: Codable {
                    let path: String
                    let fileSize: Int64
                }
            }
        }

        let response = try decoder.decode(Response.self, from: data)

        XCTAssertEqual(response.items.count, 1)
        XCTAssertEqual(response.items[0].id, "component-123")
        XCTAssertEqual(response.items[0].repository, "maven-releases")
        XCTAssertEqual(response.items[0].assets[0].path, "org/example/artifact/1.0.0/artifact-1.0.0.jar")
        XCTAssertEqual(response.items[0].assets[0].fileSize, 12345)
        XCTAssertNil(response.continuationToken)
    }

    func testParseMetadataResponse() throws {
        let json = """
        {
            "items": [
                {
                    "id": "component-456",
                    "repository": "npm-public",
                    "format": "npm",
                    "assets": [
                        {
                            "path": "lodash/4.17.21/lodash-4.17.21.tgz",
                            "fileSize": 67890,
                            "contentType": "application/x-compressed",
                            "blobCreated": "2024-01-15T10:30:00.000Z",
                            "lastModified": "2024-01-15T10:30:00.000Z",
                            "checksum": {
                                "sha1": "abc123",
                                "md5": "def456"
                            }
                        }
                    ]
                }
            ],
            "continuationToken": null
        }
        """

        let data = json.data(using: .utf8)!
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601

        struct Response: Codable {
            let items: [Item]

            struct Item: Codable {
                let id: String
                let repository: String
                let format: String
                let assets: [Asset]

                struct Asset: Codable {
                    let path: String
                    let fileSize: Int64
                    let contentType: String
                    let blobCreated: String
                    let lastModified: String
                    let checksum: Checksum

                    struct Checksum: Codable {
                        let sha1: String
                        let md5: String
                    }
                }
            }
        }

        let response = try decoder.decode(Response.self, from: data)
        let item = response.items[0]
        let asset = item.assets[0]

        XCTAssertEqual(item.id, "component-456")
        XCTAssertEqual(item.format, "npm")
        XCTAssertEqual(asset.contentType, "application/x-compressed")
        XCTAssertEqual(asset.blobCreated, "2024-01-15T10:30:00.000Z")
        XCTAssertEqual(asset.lastModified, "2024-01-15T10:30:00.000Z")
        XCTAssertEqual(asset.checksum.sha1, "abc123")
        XCTAssertEqual(asset.checksum.md5, "def456")
    }
}

// MARK: - Mock URLProtocol

class MockURLProtocol: URLProtocol {
    static var requestHandler: ((URLRequest) throws -> (HTTPURLResponse, Data))?

    override class func canInit(with request: URLRequest) -> Bool {
        true
    }

    override class func canonicalRequest(for request: URLRequest) -> URLRequest {
        request
    }

    override func startLoading() {
        guard let handler = MockURLProtocol.requestHandler else {
            fatalError("MockURLProtocol request handler not set")
        }

        do {
            let (response, data) = try handler(request)
            client?.urlProtocol(self, didReceive: response, cacheStoragePolicy: .notAllowed)
            client?.urlProtocol(self, didLoad: data)
            client?.urlProtocolDidFinishLoading(self)
        } catch {
            client?.urlProtocol(self, didFailWithError: error)
        }
    }

    override func stopLoading() {}
}

// MARK: - Mock Credentials

class MockCredentials: Credentials {
    var url: String? = "https://nexus.example.com"
    var user: String? = "testuser"
    var password: String? = "testpass"
    var profile: String? = nil
    var repositories: [String] = []
    var defaultRepository: String = ""
    var defaultRegex: String = ""
    var defaultDryRun: Bool = true
    var httpTimeoutSeconds: Int = 30

    func hasCredentials() -> Bool {
        true
    }

    func saveCredentials(url: String, user: String, password: String) throws {}
    func saveRepositories(_ repositories: [String]) throws {}
    func saveDefaults(repository: String, regex: String, dryRun: Bool) throws {}
    func saveHttpTimeout(_ seconds: Int) throws {}
    func saveProfile(_ profile: String) throws {}
    func clearAll() throws {}
}

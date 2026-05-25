//
//  NexusClientURLSession.swift
//  JNexus
//
//  URLSession-based HTTP client implementation
//  Implements caching, retry logic, and pagination
//

import Foundation

/// URLSession implementation of NexusHttpClient
/// Provides caching, retry logic, and pagination for Nexus API
///
/// Thread-safety: Cache dictionaries are protected by NSLock to prevent data races
/// when multiple async tasks access the same repository concurrently.
class NexusClientURLSession: NexusHttpClient {
    private let session: URLSession
    private let credentials: Credentials
    private var cache: [String: CacheEntry] = [:]
    private var metadataCache: [String: MetadataCacheEntry] = [:]
    private let cacheLock = NSLock()  // Protects cache and metadataCache access
    private let cacheTTL: TimeInterval
    private let maxRetries = 3
    private let initialRetryDelay: TimeInterval = 1.0

    // MARK: - Initialization

    init(credentials: Credentials, session: URLSession? = nil, cacheTTL: TimeInterval = 300) {
        self.credentials = credentials
        self.cacheTTL = cacheTTL

        if let session = session {
            // Use provided session (for testing)
            self.session = session
        } else {
            // Create default session
            let config = URLSessionConfiguration.default
            config.timeoutIntervalForRequest = TimeInterval(credentials.httpTimeoutSeconds)
            config.timeoutIntervalForResource = TimeInterval(credentials.httpTimeoutSeconds)
            self.session = URLSession(configuration: config)
        }
    }

    // MARK: - Cache Entries

    private struct CacheEntry {
        let records: [RepoRecord]
        let timestamp: Date

        func isExpired(ttl: TimeInterval) -> Bool {
            Date().timeIntervalSince(timestamp) > ttl
        }
    }

    private struct MetadataCacheEntry {
        let records: [ComponentMetadata]
        let timestamp: Date

        func isExpired(ttl: TimeInterval) -> Bool {
            Date().timeIntervalSince(timestamp) > ttl
        }
    }

    // MARK: - NexusHttpClient Implementation

    func listComponents(repository: String, forceRefresh: Bool) async throws -> [RepoRecord] {
        // Check cache (thread-safe read)
        if !forceRefresh {
            cacheLock.lock()
            let entry = cache[repository]
            cacheLock.unlock()

            if let entry = entry, !entry.isExpired(ttl: cacheTTL) {
                return entry.records
            }
        }

        // Fetch with pagination
        var allRecords: [RepoRecord] = []
        var continuationToken: String?

        repeat {
            let (records, token) = try await fetchPage(repository: repository, continuationToken: continuationToken)
            allRecords.append(contentsOf: records)
            continuationToken = token
        } while continuationToken != nil

        // Update cache (thread-safe write)
        cacheLock.lock()
        cache[repository] = CacheEntry(records: allRecords, timestamp: Date())
        cacheLock.unlock()

        return allRecords
    }

    func listComponentsWithMetadata(repository: String, forceRefresh: Bool) async throws -> [ComponentMetadata] {
        // Check cache (thread-safe read)
        if !forceRefresh {
            cacheLock.lock()
            let entry = metadataCache[repository]
            cacheLock.unlock()

            if let entry = entry, !entry.isExpired(ttl: cacheTTL) {
                return entry.records
            }
        }

        // Fetch with pagination
        var allRecords: [ComponentMetadata] = []
        var continuationToken: String?

        repeat {
            let (records, token) = try await fetchMetadataPage(repository: repository, continuationToken: continuationToken)
            allRecords.append(contentsOf: records)
            continuationToken = token
        } while continuationToken != nil

        // Update cache (thread-safe write)
        cacheLock.lock()
        metadataCache[repository] = MetadataCacheEntry(records: allRecords, timestamp: Date())
        cacheLock.unlock()

        return allRecords
    }

    func deleteComponent(componentId: String) async throws {
        guard let url = credentials.url else {
            throw NexusError.invalidCredentials
        }

        let urlString = "\(url)/service/rest/v1/components/\(componentId)"
        guard let requestURL = URL(string: urlString) else {
            throw NexusError.invalidURL(urlString)
        }

        var request = URLRequest(url: requestURL)
        request.httpMethod = "DELETE"
        request.setValue(buildAuthHeader(), forHTTPHeaderField: "Authorization")

        let (_, response) = try await fetchWithRetry(request: request)

        guard let httpResponse = response as? HTTPURLResponse,
              (200...299).contains(httpResponse.statusCode) else {
            throw NexusError.httpError(statusCode: (response as? HTTPURLResponse)?.statusCode ?? 0)
        }
    }

    func clearCache(repository: String) {
        cacheLock.lock()
        cache.removeValue(forKey: repository)
        metadataCache.removeValue(forKey: repository)
        cacheLock.unlock()
    }

    func clearAllCache() {
        cacheLock.lock()
        cache.removeAll()
        metadataCache.removeAll()
        cacheLock.unlock()
    }

    func isCached(repository: String) -> Bool {
        cacheLock.lock()
        let entry = cache[repository]
        cacheLock.unlock()

        if let entry = entry {
            return !entry.isExpired(ttl: cacheTTL)
        }
        return false
    }

    func getCacheAge(repository: String) -> TimeInterval? {
        cacheLock.lock()
        let entry = cache[repository]
        cacheLock.unlock()

        if let entry = entry {
            return Date().timeIntervalSince(entry.timestamp)
        }
        return nil
    }

    // MARK: - Private Methods

    private func fetchPage(repository: String, continuationToken: String?) async throws -> ([RepoRecord], String?) {
        guard let url = credentials.url else {
            throw NexusError.invalidCredentials
        }

        guard var components = URLComponents(string: "\(url)/service/rest/v1/components") else {
            throw NexusError.invalidURL("\(url)/service/rest/v1/components")
        }

        components.queryItems = [URLQueryItem(name: "repository", value: repository)]
        if let token = continuationToken {
            components.queryItems?.append(URLQueryItem(name: "continuationToken", value: token))
        }

        guard let requestURL = components.url else {
            throw NexusError.invalidURL("\(url)/service/rest/v1/components")
        }

        var request = URLRequest(url: requestURL)
        request.setValue(buildAuthHeader(), forHTTPHeaderField: "Authorization")

        let (data, response) = try await fetchWithRetry(request: request)

        guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
            throw NexusError.httpError(statusCode: (response as? HTTPURLResponse)?.statusCode ?? 0)
        }

        let componentsResponse = try parseComponentsResponse(data: data)
        return (componentsResponse.items, componentsResponse.continuationToken)
    }

    private func fetchMetadataPage(repository: String, continuationToken: String?) async throws -> ([ComponentMetadata], String?) {
        guard let url = credentials.url else {
            throw NexusError.invalidCredentials
        }

        guard var components = URLComponents(string: "\(url)/service/rest/v1/components") else {
            throw NexusError.invalidURL("\(url)/service/rest/v1/components")
        }

        components.queryItems = [URLQueryItem(name: "repository", value: repository)]
        if let token = continuationToken {
            components.queryItems?.append(URLQueryItem(name: "continuationToken", value: token))
        }

        guard let requestURL = components.url else {
            throw NexusError.invalidURL("\(url)/service/rest/v1/components")
        }

        var request = URLRequest(url: requestURL)
        request.setValue(buildAuthHeader(), forHTTPHeaderField: "Authorization")

        let (data, response) = try await fetchWithRetry(request: request)

        guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
            throw NexusError.httpError(statusCode: (response as? HTTPURLResponse)?.statusCode ?? 0)
        }

        let componentsResponse = try parseMetadataResponse(data: data)
        return (componentsResponse.items, componentsResponse.continuationToken)
    }

    private func fetchWithRetry(request: URLRequest, attempt: Int = 0) async throws -> (Data, URLResponse) {
        do {
            return try await session.data(for: request)
        } catch {
            if attempt < maxRetries && isRetryable(error: error) {
                let delay = initialRetryDelay * pow(2.0, Double(attempt))  // Exponential backoff
                try await Task.sleep(nanoseconds: UInt64(delay * 1_000_000_000))
                return try await fetchWithRetry(request: request, attempt: attempt + 1)
            }
            throw NexusError.networkError(error)
        }
    }

    private func isRetryable(error: Error) -> Bool {
        if let urlError = error as? URLError {
            return [.timedOut, .networkConnectionLost, .notConnectedToInternet, .cannotConnectToHost].contains(urlError.code)
        }
        return false
    }

    private func buildAuthHeader() -> String {
        guard let user = credentials.user, let password = credentials.password else {
            return ""
        }
        let authString = "\(user):\(password)"
        guard let authData = authString.data(using: .utf8) else {
            // UTF-8 encoding should never fail for valid credentials, but handle defensively
            return ""
        }
        let base64Auth = authData.base64EncodedString()
        return "Basic \(base64Auth)"
    }

    // MARK: - JSON Parsing

    private func parseComponentsResponse(data: Data) throws -> ComponentsResponse {
        let json = try JSONSerialization.jsonObject(with: data) as? [String: Any]
        guard let items = json?["items"] as? [[String: Any]] else {
            throw NexusError.parseError("Missing 'items' field")
        }

        let records = try items.compactMap { item -> RepoRecord? in
            guard let id = item["id"] as? String,
                  let assets = item["assets"] as? [[String: Any]],
                  let firstAsset = assets.first else {
                return nil
            }

            guard let path = firstAsset["path"] as? String,
                  let fileSize = firstAsset["fileSize"] as? Int64 else {
                return nil
            }

            return RepoRecord(id: id, fileSize: fileSize, path: path)
        }

        let continuationToken = json?["continuationToken"] as? String

        return ComponentsResponse(items: records, continuationToken: continuationToken)
    }

    private func parseMetadataResponse(data: Data) throws -> MetadataResponse {
        let json = try JSONSerialization.jsonObject(with: data) as? [String: Any]
        guard let items = json?["items"] as? [[String: Any]] else {
            throw NexusError.parseError("Missing 'items' field")
        }

        let formatter = ISO8601DateFormatter()

        let records = try items.compactMap { item -> ComponentMetadata? in
            guard let id = item["id"] as? String,
                  let assets = item["assets"] as? [[String: Any]],
                  let firstAsset = assets.first else {
                return nil
            }

            guard let path = firstAsset["path"] as? String,
                  let fileSize = firstAsset["fileSize"] as? Int64 else {
                return nil
            }

            let contentType = firstAsset["contentType"] as? String
            let format = item["format"] as? String

            var createdDate: Date?
            if let blobCreated = firstAsset["blobCreated"] as? String {
                createdDate = formatter.date(from: blobCreated)
            }

            var lastModified: Date?
            if let lastModifiedStr = firstAsset["lastModified"] as? String {
                lastModified = formatter.date(from: lastModifiedStr)
            }

            var checksum: String?
            if let checksums = firstAsset["checksum"] as? [String: String] {
                checksum = checksums["sha1"] ?? checksums["md5"]
            }

            return ComponentMetadata(
                id: id,
                fileSize: fileSize,
                path: path,
                contentType: contentType,
                format: format,
                createdDate: createdDate,
                lastModified: lastModified,
                checksum: checksum
            )
        }

        let continuationToken = json?["continuationToken"] as? String

        return MetadataResponse(items: records, continuationToken: continuationToken)
    }

    // MARK: - Response Structures

    private struct ComponentsResponse {
        let items: [RepoRecord]
        let continuationToken: String?
    }

    private struct MetadataResponse {
        let items: [ComponentMetadata]
        let continuationToken: String?
    }
}

// MARK: - Error Types

enum NexusError: Error, LocalizedError {
    case httpError(statusCode: Int)
    case invalidCredentials
    case invalidURL(String)
    case networkError(Error)
    case parseError(String)

    var errorDescription: String? {
        switch self {
        case .httpError(let code):
            return "HTTP error: \(code)"
        case .invalidCredentials:
            return "Invalid or missing credentials"
        case .invalidURL(let url):
            return "Invalid URL: \(url)"
        case .networkError(let error):
            return "Network error: \(error.localizedDescription)"
        case .parseError(let message):
            return "Parse error: \(message)"
        }
    }
}

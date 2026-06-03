# JNexus iOS/macOS - Implementation Guide

Back to [Main Documentation](CLAUDE.md)

## Overview

The iOS/macOS module (`jnexus-ios/`) provides native Swift applications for iOS 16.0+, iPadOS 16.0+, and macOS 13.0+ with SwiftUI interfaces. It achieves 95% code reuse across platforms while maintaining platform-specific user experiences.

**Note**: Unlike the Java modules, iOS/macOS re-implements the business logic in Swift rather than sharing code (Swift ↔ Java interop is too complex). The implementation mirrors the Java architecture and semantics.

## Technology Stack

**Core Technologies:**
- **Swift 5.9+**: Primary language for all code
- **SwiftUI**: Modern declarative UI framework
- **Xcode 15+**: IDE and build system

**Platform Technologies:**
- **URLSession**: Native HTTP client (Apple's standard networking API)
- **Keychain Services**: Secure credential storage (AES-256 hardware-backed)
- **Security Framework**: Native encryption and keychain access
- **Async/await**: Native Swift concurrency (mirrors Kotlin coroutines)
- **Codable**: Automatic JSON serialization/deserialization
- **UserDefaults**: Non-sensitive configuration storage
- **Combine**: Reactive programming (minimal use, prefer async/await)

**Zero External Dependencies**: Uses only Apple frameworks - no CocoaPods, SPM packages, or Carthage.

## iOS/macOS Module Structure

### Shared Code (95% reuse)

**Core Protocols:**
- `NexusHttpClient.swift`: HTTP client protocol (mirrors Java interface)
- `Credentials.swift`: Credentials protocol (mirrors Java interface)
- `NexusService.swift`: Business logic ported from Java (~200 lines)
- `AppState.swift`: ObservableObject for dependency injection

**Platform Implementations:**
- `NexusClientURLSession.swift`: URLSession-based HTTP client (~350 lines)
  - Caching with 5-minute TTL, retry with exponential backoff, pagination
- `CredentialsKeychain.swift`: Keychain Services storage (~180 lines)
  - AES-256 hardware-backed encryption, UserDefaults for non-sensitive settings

**Data Models** (Swift structs, Codable):
- `RepoRecord.swift`: Basic component record (id, fileSize, path)
- `ComponentMetadata.swift`: Enhanced component with full metadata
- `SearchCriteria.swift`: Advanced search filters with Builder pattern
- `RepositoryStats.swift`: Comprehensive statistics

**UI Screens** (SwiftUI, adaptive layouts):
- `RepositoryListView.swift`: List/refresh/delete with swipe gestures (~200 lines)
- `SearchView.swift`: Advanced filters with DisclosureGroup (~180 lines)
- `StatsView.swift`: Repository analytics with GroupBox sections (~200 lines)
- `SettingsView.swift`: Credential management with Form (~150 lines)

### iOS-specific Code (5%)

- `JNexusApp.swift`: App entry point with @main, StateObject initialization
- `ContentView.swift`: TabView with 4 tabs (List, Search, Stats, Settings)
- `Info.plist`: App configuration, network security (HTTPS-only, local networking allowed)

### macOS-specific Code (5%)

- `JNexusApp.swift`: App entry point with WindowGroup and Settings scene
- `ContentView.swift`: NavigationSplitView with sidebar (replaces TabView)
- `MenuCommands.swift`: Menu bar commands and keyboard shortcuts (⌘L, ⌘R, ⌘F, ⌘,)
- `JNexus.entitlements`: App Sandbox, network client, keychain access
- `Info.plist`: macOS minimum version 13.0 (Ventura)

### Platform Differences

| Feature | iOS/iPadOS | macOS |
|---------|------------|-------|
| Navigation | TabView with bottom tabs | NavigationSplitView with sidebar |
| Settings | Tab in main window | Separate Settings window (⌘,) |
| Keyboard shortcuts | Limited (system only) | Full menu bar commands |
| Multi-window | No (single window) | Yes (multiple windows) |
| Layout | Adaptive (portrait/landscape) | Fixed sidebar + detail |

**iPad-specific**: Uses split view in landscape via `horizontalSizeClass` detection.

## Layered Architecture

```
UI Layer (SwiftUI screens: List, Search, Stats, Settings)
    ↓
Service Layer (NexusService.swift - ported from Java)
    ↓
Client Layer (NexusClientURLSession.swift implements NexusHttpClient protocol)
    ↓
HTTP/Nexus API (URLSession)
```

## HTTP Client (NexusClientURLSession.swift)

Implements NexusHttpClient protocol using URLSession.

### Key Features

- HTTP/2 support via URLSession
- Automatic pagination with continuation tokens
- JSON parsing with JSONDecoder (Codable)
- Metadata extraction from Nexus API responses
- Retry logic with exponential backoff (1s, 2s, 4s)
- Retries on: 5xx, 408, 429, connection errors, timeouts

### Caching

- Time-based caching with Swift actor for thread safety
- Default TTL: 5 minutes (300 seconds)
- Separate caches for RepoRecord and ComponentMetadata
- Cache entry: `(records: [RepoRecord], timestamp: Date)`

### Implementation Pattern

```swift
actor NexusClientURLSession: NexusHttpClient {
    private var cache: [String: CacheEntry] = [:]
    private let cacheTTL: TimeInterval = 300
    
    func listComponents(repository: String, forceRefresh: Bool = false) async throws -> [RepoRecord] {
        if !forceRefresh, let entry = cache[repository], 
           Date().timeIntervalSince(entry.timestamp) < cacheTTL {
            return entry.records
        }
        
        var allRecords: [RepoRecord] = []
        var continuationToken: String? = nil
        
        repeat {
            let (records, token) = try await fetchPage(repository: repository, token: continuationToken)
            allRecords.append(contentsOf: records)
            continuationToken = token
        } while continuationToken != nil
        
        cache[repository] = CacheEntry(records: allRecords, timestamp: Date())
        return allRecords
    }
}
```

### Methods

- `listComponents(repository:forceRefresh:) async throws -> [RepoRecord]`
- `listComponentsWithMetadata(repository:forceRefresh:) async throws -> [ComponentMetadata]`
- `deleteComponent(repository:componentId:) async throws`
- `clearCache()` and `clearCache(repository:)`

### Retry Logic

```swift
private func fetchWithRetry<T>(request: URLRequest, retries: Int = 3) async throws -> T where T: Decodable {
    var lastError: Error?
    var delay: TimeInterval = 1.0
    
    for attempt in 0..<retries {
        do {
            let (data, response) = try await URLSession.shared.data(for: request)
            
            guard let httpResponse = response as? HTTPURLResponse else {
                throw NexusError.invalidResponse
            }
            
            guard httpResponse.statusCode == 200 else {
                if shouldRetry(statusCode: httpResponse.statusCode) && attempt < retries - 1 {
                    try await Task.sleep(nanoseconds: UInt64(delay * 1_000_000_000))
                    delay *= 2
                    continue
                }
                throw NexusError.httpError(httpResponse.statusCode)
            }
            
            return try JSONDecoder().decode(T.self, from: data)
            
        } catch {
            lastError = error
            if attempt < retries - 1 {
                try await Task.sleep(nanoseconds: UInt64(delay * 1_000_000_000))
                delay *= 2
            }
        }
    }
    
    throw lastError ?? NexusError.unknown
}

private func shouldRetry(statusCode: Int) -> Bool {
    return statusCode >= 500 || statusCode == 408 || statusCode == 429
}
```

## Credential Storage (CredentialsKeychain.swift)

Uses Keychain Services for secure credential storage.

### Encryption

- **Keychain Services**: AES-256 hardware-backed encryption (automatic)
- **Access Control**: kSecAttrAccessibleWhenUnlocked (encrypted at rest)
- **Storage**: System keychain (inaccessible to other apps)
- **UserDefaults**: Non-sensitive settings (repositories, defaults, timeout)

### Implementation

```swift
class CredentialsKeychain: Credentials {
    private let keychainService = "com.flossware.jnexus"
    
    func saveCredential(key: String, value: String) throws {
        let data = Data(value.utf8)
        
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: keychainService,
            kSecAttrAccount as String: key,
            kSecValueData as String: data,
            kSecAttrAccessible as String: kSecAttrAccessibleWhenUnlocked
        ]
        
        // Delete existing
        SecItemDelete(query as CFDictionary)
        
        // Add new
        let status = SecItemAdd(query as CFDictionary, nil)
        guard status == errSecSuccess else {
            throw CredentialsError.keychainError(status)
        }
    }
    
    func getCredential(key: String) throws -> String? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: keychainService,
            kSecAttrAccount as String: key,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]
        
        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        
        guard status == errSecSuccess else {
            if status == errSecItemNotFound {
                return nil
            }
            throw CredentialsError.keychainError(status)
        }
        
        guard let data = result as? Data else {
            throw CredentialsError.invalidData
        }
        
        return String(data: data, encoding: .utf8)
    }
}
```

### Methods

**Credentials protocol methods:**
- `getUrl() -> String?`
- `getUser() -> String?`
- `getPassword() -> String?`
- `getRepositories() -> [String]`
- `getDefaultRepository() -> String?`
- `getDefaultRegex() -> String?`
- `getDefaultDryRun() -> Bool`
- `getHttpTimeoutSeconds() -> Int`

**Storage methods:**
- `saveCredentials(url:user:password:) throws`
- `saveRepositories(_ repositories: [String])`
- `saveDefaults(repository:regex:dryRun:)`
- `clearAll() throws`
- `hasCredentials() -> Bool`

## Business Logic (NexusService.swift)

Ported from Java NexusService.java with identical semantics.

### Key Features

- Filtering components by regex, size, date, extension
- Advanced search with SearchCriteria
- Statistics calculation (size distribution, file types, age distribution)
- Delete operations with dry-run support

### Implementation Pattern

```swift
class NexusService {
    private let client: NexusHttpClient
    
    init(client: NexusHttpClient) {
        self.client = client
    }
    
    func searchComponents(criteria: SearchCriteria) async throws -> [ComponentMetadata] {
        var components = try await client.listComponentsWithMetadata(
            repository: criteria.repository
        )
        
        // Apply filters (mirrors Java implementation)
        if let minSize = criteria.minSize {
            components = components.filter { $0.fileSize >= minSize }
        }
        
        if let maxSize = criteria.maxSize {
            components = components.filter { $0.fileSize <= maxSize }
        }
        
        if let createdAfter = criteria.createdAfter {
            components = components.filter { 
                if let created = $0.createdDate {
                    return created > createdAfter
                }
                return false
            }
        }
        
        // ... more filters
        
        return components
    }
    
    func calculateStatistics(repository: String) async throws -> RepositoryStats {
        let components = try await client.listComponentsWithMetadata(repository: repository)
        
        // Mirror Java statistics calculation
        let totalComponents = components.count
        let totalSize = components.reduce(0) { $0 + $1.fileSize }
        let averageSize = totalComponents > 0 ? totalSize / Int64(totalComponents) : 0
        
        // ... more calculations
        
        return RepositoryStats(
            totalComponents: totalComponents,
            totalSize: totalSize,
            averageSize: averageSize,
            medianSize: calculateMedian(components),
            sizeDistribution: calculateSizeDistribution(components),
            fileTypeBreakdown: calculateFileTypes(components),
            ageDistribution: calculateAgeDistribution(components),
            largestComponents: findLargest(components, count: 20)
        )
    }
}
```

## SwiftUI Screens

### RepositoryListView.swift

List, refresh, and delete components with swipe gestures.

**Features:**
- Repository Picker with "All" option
- List/Refresh buttons (async/await with Task)
- List with swipe-to-delete gesture
- Delete confirmation alert
- Component details sheet (tap to view metadata)
- Pull-to-refresh gesture
- Loading indicator
- Error display

**Implementation Pattern:**
```swift
struct RepositoryListView: View {
    @EnvironmentObject var appState: AppState
    @State private var components: [RepoRecord] = []
    @State private var loading = false
    @State private var error: String?
    
    var body: some View {
        NavigationView {
            List {
                ForEach(components) { component in
                    ComponentRow(component: component)
                        .onTapGesture {
                            selectedComponent = component
                        }
                        .swipeActions(edge: .trailing) {
                            Button(role: .destructive) {
                                componentToDelete = component
                                showDeleteConfirmation = true
                            } label: {
                                Label("Delete", systemImage: "trash")
                            }
                        }
                }
            }
            .refreshable {
                await loadComponents(forceRefresh: true)
            }
            .navigationTitle("Components")
            .toolbar {
                ToolbarItem(placement: .primaryAction) {
                    Button("Refresh") {
                        Task { await loadComponents(forceRefresh: true) }
                    }
                }
            }
        }
    }
    
    private func loadComponents(forceRefresh: Bool = false) async {
        loading = true
        defer { loading = false }
        
        do {
            components = try await appState.service.listComponents(
                repository: selectedRepository,
                forceRefresh: forceRefresh
            )
            error = nil
        } catch {
            self.error = error.localizedDescription
        }
    }
}
```

### SearchView.swift

Advanced filters with DisclosureGroup.

**Features:**
- Collapsible filter sections (DisclosureGroup)
- Size range inputs (TextField with number formatter)
- Date inputs (DatePicker with ISO 8601 binding)
- Extension and regex filters
- Clear filters button
- SearchCriteria.Builder pattern

**Implementation Pattern:**
```swift
struct SearchView: View {
    @EnvironmentObject var appState: AppState
    @State private var minSize: String = ""
    @State private var maxSize: String = ""
    @State private var createdAfter: Date?
    @State private var extension: String = ""
    
    var body: some View {
        Form {
            Section {
                Picker("Repository", selection: $selectedRepository) {
                    ForEach(repositories, id: \.self) { repo in
                        Text(repo).tag(repo)
                    }
                }
            }
            
            DisclosureGroup("Size Filters") {
                TextField("Min Size (bytes)", text: $minSize)
                    .keyboardType(.numberPad)
                TextField("Max Size (bytes)", text: $maxSize)
                    .keyboardType(.numberPad)
            }
            
            DisclosureGroup("Date Filters") {
                DatePicker("Created After", selection: Binding(
                    get: { createdAfter ?? Date() },
                    set: { createdAfter = $0 }
                ), displayedComponents: .date)
            }
            
            Section {
                Button("Search") {
                    Task { await performSearch() }
                }
                Button("Clear Filters") {
                    clearFilters()
                }
            }
        }
        .navigationTitle("Search")
    }
}
```

### StatsView.swift

Repository analytics with GroupBox sections.

**Features:**
- Overview metrics (total, average, median with MB/GB formatting)
- Size distribution with progress bars
- File types sorted by size
- Age distribution with bucket counts
- Largest components list (top 10)

**Implementation Pattern:**
```swift
struct StatsView: View {
    @EnvironmentObject var appState: AppState
    @State private var stats: RepositoryStats?
    
    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                if let stats = stats {
                    GroupBox("Overview") {
                        HStack {
                            Text("Total Components:")
                            Spacer()
                            Text("\(stats.totalComponents)")
                        }
                        HStack {
                            Text("Total Size:")
                            Spacer()
                            Text(formatSize(stats.totalSize))
                        }
                    }
                    
                    GroupBox("Size Distribution") {
                        ForEach(stats.sizeDistribution.keys.sorted(), id: \.self) { range in
                            HStack {
                                Text(range)
                                Spacer()
                                Text("\(stats.sizeDistribution[range] ?? 0)")
                                ProgressView(value: Double(stats.sizeDistribution[range] ?? 0),
                                           total: Double(stats.totalComponents))
                                    .frame(width: 100)
                            }
                        }
                    }
                }
            }
            .padding()
        }
        .navigationTitle("Statistics")
    }
}
```

### SettingsView.swift

Credential management with Form.

**Features:**
- Credential inputs (URL, user, password with SecureField)
- Show/hide password toggle
- Repository list (comma-separated TextField)
- Default values (repository, regex, dry-run toggle)
- HTTP timeout configuration
- Save/Clear buttons with success/error feedback
- Security info explaining Keychain Services encryption

**Implementation Pattern:**
```swift
struct SettingsView: View {
    @EnvironmentObject var appState: AppState
    @State private var url: String = ""
    @State private var user: String = ""
    @State private var password: String = ""
    @State private var showPassword = false
    
    var body: some View {
        Form {
            Section("Nexus Server") {
                TextField("URL", text: $url)
                    .autocapitalization(.none)
                TextField("Username", text: $user)
                    .autocapitalization(.none)
                
                HStack {
                    if showPassword {
                        TextField("Password", text: $password)
                    } else {
                        SecureField("Password", text: $password)
                    }
                    Button {
                        showPassword.toggle()
                    } label: {
                        Image(systemName: showPassword ? "eye.slash" : "eye")
                    }
                }
            }
            
            Section("Repositories") {
                TextField("Comma-separated", text: $repositories)
            }
            
            Section {
                Button("Save") {
                    saveCredentials()
                }
                Button("Clear All", role: .destructive) {
                    clearCredentials()
                }
            }
            
            Section {
                Text("Credentials are encrypted using Keychain Services with AES-256 hardware-backed encryption.")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
        .navigationTitle("Settings")
    }
}
```

## Dependency Injection (AppState.swift)

ObservableObject provides shared instances across the app.

**Implementation:**
```swift
class AppState: ObservableObject {
    let credentials: CredentialsKeychain
    let client: NexusClientURLSession
    let service: NexusService
    
    init() {
        self.credentials = CredentialsKeychain()
        
        let url = credentials.getUrl() ?? ""
        let user = credentials.getUser() ?? ""
        let password = credentials.getPassword() ?? ""
        let timeout = credentials.getHttpTimeoutSeconds()
        
        self.client = NexusClientURLSession(
            url: url,
            user: user,
            password: password,
            timeoutSeconds: timeout
        )
        
        self.service = NexusService(client: client)
    }
    
    func reinitialize() {
        // Called after saving new credentials
        // ... re-create client and service
    }
}
```

**Usage in App:**
```swift
@main
struct JNexusApp: App {
    @StateObject private var appState = AppState()
    
    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(appState)
        }
    }
}
```

## Building and Running

### Xcode Build

1. Open `jnexus-ios/JNexus.xcodeproj` in Xcode
2. Select target: JNexus (iOS), JNexus (macOS)
3. Build: ⌘B
4. Run: ⌘R

### Command Line Build (iOS)

```bash
xcodebuild -project jnexus-ios/JNexus.xcodeproj \
    -scheme JNexus-iOS \
    -configuration Release \
    -destination 'generic/platform=iOS'
```

### Command Line Build (macOS)

```bash
xcodebuild -project jnexus-ios/JNexus.xcodeproj \
    -scheme JNexus-macOS \
    -configuration Release
```

### Install on iOS Device

1. Connect device via USB
2. Select device in Xcode
3. Run: ⌘R (requires Apple Developer account for signing)

### macOS Distribution

1. Archive: Product → Archive
2. Distribute: Organizer → Distribute App
3. Options: Developer ID signed, notarized

## Testing

### Unit Tests

- Test NexusService filtering logic
- Test SearchCriteria builder
- Test RepositoryStats calculations
- Mock NexusHttpClient protocol

**Test Pattern:**
```swift
class NexusServiceTests: XCTestCase {
    var service: NexusService!
    var mockClient: MockNexusClient!
    
    override func setUp() {
        mockClient = MockNexusClient()
        service = NexusService(client: mockClient)
    }
    
    func testSearchComponents_withSizeFilter() async throws {
        mockClient.mockComponents = [
            ComponentMetadata(id: "1", fileSize: 1000, ...),
            ComponentMetadata(id: "2", fileSize: 2000, ...)
        ]
        
        let criteria = SearchCriteria(
            repository: "maven-releases",
            minSize: 1500
        )
        
        let results = try await service.searchComponents(criteria: criteria)
        
        XCTAssertEqual(results.count, 1)
        XCTAssertEqual(results[0].id, "2")
    }
}
```

### UI Tests

- Test navigation between tabs/views
- Test credential save/load flow
- Test list refresh
- Test delete confirmation

**Test Pattern:**
```swift
class JNexusUITests: XCTestCase {
    func testSettingsFlow() throws {
        let app = XCUIApplication()
        app.launch()
        
        app.tabBars.buttons["Settings"].tap()
        
        let urlField = app.textFields["URL"]
        urlField.tap()
        urlField.typeText("https://nexus.example.com")
        
        app.buttons["Save"].tap()
        
        XCTAssertTrue(app.alerts["Success"].exists)
    }
}
```

## Common Development Tasks

### Adding a New Screen

1. Create new Swift file in `Shared/UI/Screens/`
2. Define `struct YourView: View`
3. Add navigation in iOS `ContentView.swift` (TabView)
4. Add navigation in macOS `ContentView.swift` (NavigationSplitView)
5. Test on iPhone, iPad, and Mac
6. Update README.md and CHANGELOG.md

### Adding a New Feature to Settings

1. Add property to `CredentialsKeychain.swift`
2. Add UI field to `SettingsView.swift`
3. Update `AppState.swift` if needed
4. Test keychain storage
5. Update CHANGELOG.md

### Modifying HTTP Client

1. Update `NexusClientURLSession.swift`
2. Keep protocol compatible with `NexusHttpClient`
3. Test with real Nexus server
4. Consider Java/Android compatibility if changing shared semantics

## Debugging

### Xcode Console

Use print statements or breakpoints:
```swift
print("Fetching page with token: \(token ?? "nil")")
```

### Network Debugging

Enable URLSession logging:
```swift
let configuration = URLSessionConfiguration.default
configuration.httpAdditionalHeaders = ["X-Debug": "true"]
```

Use Charles Proxy or Proxyman for HTTP inspection.

### Keychain Debugging

Cannot directly view keychain values. To debug:
```swift
print("Stored URL: \(credentials.getUrl() ?? "none")")
```

Use Keychain Access app on macOS to inspect stored items (service: `com.flossware.jnexus`).

### SwiftUI Preview

Use Xcode previews for rapid UI iteration:
```swift
struct SettingsView_Previews: PreviewProvider {
    static var previews: some View {
        SettingsView()
            .environmentObject(AppState())
    }
}
```

Back to [Main Documentation](CLAUDE.md)

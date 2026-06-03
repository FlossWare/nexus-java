# JNexus - Codebase Documentation for AI Assistants

## Project Overview

**JNexus** is a cross-platform tool for managing components in Sonatype Nexus Repository Manager. It provides list, delete, search, and statistics operations with advanced filtering, safety features, and minimal dependencies.

**Multi-platform support:**
- **Desktop**: Java 21 with 4 UIs (CLI, Swing, AWT, Terminal) - See [CLAUDE_DESKTOP.md](CLAUDE_DESKTOP.md)
- **Android**: Native mobile app (Android 8.0+) with Jetpack Compose UI - See [CLAUDE_ANDROID.md](CLAUDE_ANDROID.md)
- **iOS/iPadOS/macOS**: Native Swift apps with SwiftUI - See [CLAUDE_IOS.md](CLAUDE_IOS.md)

**Multi-module architecture:**
- `jnexus-core`: Shared business logic (Java 11, platform-agnostic)
- `jnexus-android`: Android app (Gradle, Kotlin, Jetpack Compose)
- `jnexus-ios`: iOS/iPadOS/macOS apps (Xcode, Swift, SwiftUI)
- `src/`: Desktop app (Maven, Java 21)

## Documentation Structure

- **[CLAUDE_DESKTOP.md](CLAUDE_DESKTOP.md)**: Desktop implementation (Java 21, 4 UIs, Maven)
- **[CLAUDE_ANDROID.md](CLAUDE_ANDROID.md)**: Android implementation (Kotlin, Jetpack Compose, Gradle)
- **[CLAUDE_IOS.md](CLAUDE_IOS.md)**: iOS/macOS implementation (Swift, SwiftUI, Xcode)
- **[DEVELOPMENT_GUIDE.md](DEVELOPMENT_GUIDE.md)**: Common development tasks, debugging, conventions
- **[TEST_COVERAGE.md](TEST_COVERAGE.md)**: Testing strategy and coverage details
- **[CI-CD.md](CI-CD.md)**: Build and release processes
- **[SECURITY.md](SECURITY.md)**: Security considerations and best practices
- **[CONTRIBUTING.md](CONTRIBUTING.md)**: Contribution guidelines

## Key Architectural Decisions

### Why No Spring Boot?
- **Original Problem**: Spring Boot added 50MB JAR size and 3-5 second startup overhead for a simple CLI tool
- **Solution**: Refactored to plain Java 21 with minimal dependencies
- **Result**: 2.7MB JAR, <200ms startup time

### Technology Choices Summary

**Desktop (Java 21):**
- Picocli (CLI), Java HttpClient (HTTP/2), Swing/AWT/jcurses (UIs), Maven (build)
- Zero external dependencies for core HTTP and GUI functionality

**Shared Core (Java 11):**
- Jackson (JSON), SLF4J (logging), JUnit 5 + Mockito (testing), Gradle (build)
- All dependencies are Android-compatible

**Android (Kotlin + Java):**
- OkHttp (HTTP), Jetpack Compose (UI), Material Design 3, EncryptedSharedPreferences (security)
- Kotlin Coroutines (async), Desugaring (Java 11 features on Android 8.0+)

**iOS/macOS (Swift):**
- URLSession (HTTP), SwiftUI (UI), Keychain Services (security), Codable (JSON)
- Zero external dependencies - uses only Apple frameworks

See platform-specific docs for detailed technology choices.

## Code Architecture

### Multi-Module Structure

```
jnexus/                         (root project)
├── jnexus-core/               (Shared library - Java 11, Gradle)
│   ├── NexusService.java           # Business logic (filtering, statistics)
│   ├── NexusHttpClient.java        # HTTP client interface
│   ├── Credentials.java            # Credentials interface
│   └── [data models]               # RepoRecord, ComponentMetadata, etc.
├── jnexus-android/            (Android app - Kotlin, Gradle)
│   ├── NexusClientOkHttp.java      # OkHttp implementation
│   ├── CredentialsAndroid.java     # Encrypted storage
│   └── ui/screens/                 # Jetpack Compose screens
├── jnexus-ios/                (iOS/iPadOS/macOS apps - Swift, Xcode)
│   ├── Shared/                     # 95% code reuse
│   │   ├── Core/                   # Business logic (ported from Java)
│   │   ├── Platform/               # URLSession + Keychain implementations
│   │   └── UI/Screens/             # SwiftUI screens (shared)
│   ├── iOS/                        # iOS-specific (5%)
│   └── macOS/                      # macOS-specific (5%)
└── src/                       (Desktop app - Java 21, Maven)
    ├── JNexus.java                 # CLI
    ├── JNexusSwing.java            # Swing GUI
    ├── JNexusAWT.java              # AWT GUI
    ├── JNexusUI.java               # Terminal UI
    ├── NexusClient.java            # java.net.http implementation
    └── Credentials.java            # File-based storage
```

### Platform Abstraction Layers

**NexusHttpClient Interface/Protocol:**
- **Java** (jnexus-core): Interface definition
- **Desktop**: `NexusClient` (uses java.net.http.HttpClient)
- **Android**: `NexusClientOkHttp` (uses OkHttp)
- **iOS/macOS**: `NexusClientURLSession` (uses URLSession)
- Methods: `listComponents()`, `listComponentsWithMetadata()`, `deleteComponent()`, `clearCache()`

**Credentials Interface/Protocol:**
- **Java** (jnexus-core): Interface definition
- **Desktop**: `Credentials` class (uses ~/.flossware/nexus/nexus.properties)
- **Android**: `CredentialsAndroid` (uses EncryptedSharedPreferences with AES256_GCM)
- **iOS/macOS**: `CredentialsKeychain` (uses Keychain Services with AES-256 hardware-backed)
- Methods: `getUrl()`, `getUser()`, `getPassword()`, `getRepositories()`, etc.

**Note**: iOS/macOS re-implements these as Swift protocols with identical semantics but no code sharing with Java (Swift ↔ Java interop too complex).

### Layered Architecture (All Platforms)

```
UI Layer (CLI, GUI, or mobile screens)
    ↓
Service Layer (NexusService - filtering, statistics, business logic)
    ↓
Client Layer (NexusHttpClient implementation - platform-specific)
    ↓
HTTP/Nexus API (java.net.http, OkHttp, or URLSession)
```

### Shared Data Models (in jnexus-core)

All data models are Java records, shared between desktop and Android (iOS/macOS use equivalent Swift structs):
- **RepoRecord.java**: Basic component record (id, fileSize, path)
- **ComponentMetadata.java**: Enhanced component with full metadata (contentType, format, createdDate, lastModified, checksum)
- **SearchCriteria.java**: Advanced search filters (size range, date range, file extension, component name pattern) with Builder pattern
- **RepositoryStats.java**: Comprehensive statistics (size distribution, file type breakdown, age distribution, largest components)

## Design Patterns

### Interface Segregation Pattern (Platform Abstraction)
- **NexusHttpClient** interface abstracts HTTP layer
  - Desktop: `NexusClient` (java.net.http)
  - Android: `NexusClientOkHttp` (OkHttp)
  - iOS/macOS: `NexusClientURLSession` (URLSession)
- **Credentials** interface abstracts storage layer
  - Desktop: File-based (`~/.flossware/nexus/nexus.properties`)
  - Android: `CredentialsAndroid` (EncryptedSharedPreferences)
  - iOS/macOS: `CredentialsKeychain` (Keychain Services)
- Allows jnexus-core to be platform-agnostic

### Strategy Pattern
- Multiple configuration sources (env vars vs properties file) with fallback
- Multiple HTTP implementations (java.net.http vs OkHttp vs URLSession)
- Multiple credential storage strategies (file vs encrypted prefs vs keychain)

### Builder Pattern
- HttpClient creation with configuration (all platforms)
- SearchCriteria with fluent API
- Platform-specific builders (OkHttpClient.Builder, URLSession configuration)

### Repository Pattern
- NexusHttpClient abstracts HTTP API details from business logic
- All platforms have different implementations but same interface/protocol

### Dependency Injection Pattern
- Android: `NexusApplication` class provides singleton instances
- iOS/macOS: `AppState` ObservableObject provides shared instances
- Desktop: Direct instantiation in UI classes

### Cache-Aside Pattern
- Time-based caching with configurable TTL
- Cache key: repository name
- Cache value: List<RepoRecord> + timestamp (also separate cache for ComponentMetadata)
- Default TTL: 5 minutes (300 seconds)
- Thread-safe using ConcurrentHashMap (Java) or actor isolation (Swift)
- Identical pattern implemented on all platforms

## Enhanced Features (Metadata, Search, Statistics)

### Component Metadata Extraction
Extracts full component metadata from Nexus API responses:
- **Metadata fields**: contentType, format, createdDate, lastModified, checksum
- **Graceful handling**: Missing/null fields use defaults
- **Caching**: Separate metadata cache with same TTL pattern

### Advanced Search and Filtering
**NexusService.searchComponents()** applies multiple filter types:
- **Size range**: minSize and maxSize in bytes
- **Date range**: createdAfter and createdBefore as Instant
- **File extension**: Filter by extension
- **Component name pattern**: Regex matching on component name
- **Path regex**: Existing regex filter on full path
- **Combination**: All filters applied sequentially (AND logic)
- **Client-side**: All filtering done after fetching (Nexus API v1 doesn't support query parameters)

### Repository Statistics
**NexusService.calculateStatistics()** computes comprehensive analytics:
- **Basic metrics**: totalComponents, totalSize, averageSize, medianSize
- **Size distribution**: 5 buckets (<1MB, 1-10MB, 10-100MB, 100MB-1GB, >1GB)
- **File type breakdown**: Map of extension → total size
- **Age distribution**: 4 buckets (last 7 days, 30 days, 90 days, older)
- **Largest components**: Top 20 components sorted by size descending

## Important Implementation Details

### Pagination
- Nexus API uses continuation tokens for pagination
- Client implementations transparently handle all pages
- URL construction: `baseUrl + "&continuationToken=" + token`

### Authentication
- HTTP Basic Auth via Authorization header
- Base64 encoding: `Base64.getEncoder().encodeToString((user + ":" + password).getBytes())`

### Configuration
All platforms support profile-based configuration:
- **Required fields**: nexus.url, nexus.user, nexus.password
- **Optional fields**: nexus.repositories, nexus.default.*, nexus.http.timeout.seconds
- **Profile support**: Multiple configuration files for different environments
  - Desktop: `~/.flossware/nexus/nexus-{profile}.properties`
  - Android: Encrypted SharedPreferences with profile prefix
  - iOS/macOS: UserDefaults with profile key

### Error Handling
- Service layer catches individual delete failures and continues processing
- HTTP errors throw appropriate exceptions with status code
- Configuration errors throw IllegalStateException (or equivalent)

### Caching
- **Cache key**: repository name (String)
- **Cache value**: List of records + timestamp
- **Default TTL**: 300 seconds (5 minutes)
- **Thread safety**: Platform-appropriate concurrency mechanisms
- **Cache invalidation**: Automatic expiration, manual via `clearCache()`, auto-clear after deletions
- **Bypass mechanism**: Force fresh fetch option
- **Disable caching**: Set TTL to 0

**Caching Strategy:**
- List operations use cache by default (fast, may be slightly stale)
- Delete operations always fetch fresh data (accuracy over speed)
- Delete operations clear cache after completion (maintain consistency)

## Known Limitations

### Single Asset Per Component (Design Choice)
- Component parsing only uses first asset
- Components can have multiple assets (e.g., JAR + POM + sources)
- Displays path/size of first asset only
- **Rationale**: Simplicity and common case optimization
- **Impact**: Statistics may underreport total size for multi-asset components
- **Future Enhancement**: Could add `--show-all-assets` flag if needed

### No Parallel Deletion
- Components deleted sequentially
- Could be parallelized with ExecutorService/async patterns
- Current approach is safer and easier to track

## Security Considerations

See [SECURITY.md](SECURITY.md) for complete security documentation.

**Key points:**
- Desktop: File-based credentials with filesystem permissions
- Android: EncryptedSharedPreferences with AES256_GCM
- iOS/macOS: Keychain Services with AES-256 hardware-backed encryption
- All platforms: HTTPS-only communication, no credential logging
- Delete operations require confirmation (unless explicitly skipped)
- No delete undo - operations are permanent

## Build and Release

See [CI-CD.md](CI-CD.md) for complete build and release documentation.

**Quick reference:**
- **Desktop**: `./mvnw clean package` → 2.7MB JAR
- **Android**: `gradle :jnexus-android:assembleDebug` → APK
- **iOS/macOS**: Xcode build (⌘B) → .app bundles
- **CI/CD**: GitHub Actions for all platforms

## Future Enhancements (Not Yet Implemented)

### High Priority
- Upload command for publishing artifacts
- Batch operations from file input
- Progress bars for large operations

### Medium Priority
- Export/import component lists (CSV, JSON)
- Google Play Store distribution for Android app
- F-Droid submission for Android app

### Low Priority
- Tab completion for bash/zsh
- Native image compilation with GraalVM
- Plugin system for custom operations
- Web UI companion tool

## When Making Changes

### Always Update
1. Unit tests (verify existing tests still pass)
2. Javadoc/documentation (for public API changes)
3. README.md (for user-facing changes)
4. CHANGELOG.md (for all changes)
5. Platform-specific CLAUDE_*.md files (for architectural changes)

### Never
- Break backward compatibility without major version bump
- Add Spring or other heavy frameworks
- Commit credentials or sensitive data
- Modify behavior without adding tests

## Questions or Issues?

- Check platform-specific docs: [Desktop](CLAUDE_DESKTOP.md), [Android](CLAUDE_ANDROID.md), [iOS/macOS](CLAUDE_IOS.md)
- See [DEVELOPMENT_GUIDE.md](DEVELOPMENT_GUIDE.md) for common development tasks
- Review [TEST_COVERAGE.md](TEST_COVERAGE.md) for testing patterns
- Check [CONTRIBUTING.md](CONTRIBUTING.md) for development guidelines
- Open GitHub issue for bugs or feature requests

# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## iOS/macOS v1.0 - 2026-05-22

### Added
- **iOS/iPadOS/macOS Native Apps** (jnexus-ios/) - NEW native Swift applications for Apple platforms
  - **Multiplatform architecture**:
    - Shared Swift codebase (~95% code reuse across iOS, iPadOS, macOS)
    - Platform-specific UI adaptations for each platform
  - **iOS/iPadOS features**:
    - TabView navigation with 4 tabs (List, Search, Stats, Settings)
    - Swipe-to-delete gestures
    - iPad-optimized layouts (split view in landscape, adaptive sizing)
    - iPad multitasking support (Split View, Slide Over)
    - All orientations supported (portrait, landscape)
  - **macOS features**:
    - Sidebar navigation with NavigationSplitView
    - Menu bar with keyboard shortcuts (⌘L List, ⌘R Refresh, ⌘F Search, ⌘, Settings)
    - Separate Settings window (⌘,)
    - Multi-window support with window restoration
    - Window size constraints (min 800x600)
  - **Core functionality** (all platforms):
    - List components with caching (5-minute TTL)
    - Advanced search with filters (size, date, extension, regex)
    - Repository statistics (distribution, file types, age, largest components)
    - Component metadata viewing (content type, format, dates, checksum)
    - Delete with confirmation dialogs
  - **Platform implementations**:
    - `NexusClientURLSession`: URLSession-based HTTP client
      - Caching with 5-minute TTL (same as Android)
      - Retry logic with exponential backoff (1s, 2s, 4s)
      - Automatic pagination with continuation tokens
      - Configurable timeouts
    - `CredentialsKeychain`: iOS/macOS Keychain secure storage
      - AES-256 hardware-backed encryption (Secure Enclave when available)
      - URL, username, password stored in Keychain
      - Settings (repositories, defaults) stored in UserDefaults
  - **Security**:
    - App Sandbox enabled (macOS)
    - HTTPS-only network traffic (allows local networking)
    - Keychain access entitlement
    - No data persistence except credentials and settings
  - **Requirements**:
    - iOS/iPadOS: 16.0+ (for async/await, modern SwiftUI)
    - macOS: 13.0+ (Ventura, for SwiftUI NavigationSplitView)
  - **Distribution**:
    - Enterprise/Ad-Hoc distribution (IPA for iOS, DMG for macOS)
    - No App Store submission required
  - **Documentation**:
    - `jnexus-ios/README.md`: Complete Apple platforms documentation
    - Building from source instructions (Xcode 15+)
    - Installation guides for iOS/iPadOS/macOS

### Technical Details
- **SwiftUI**: Declarative UI framework with adaptive layouts
- **Async/await**: Modern Swift concurrency for network operations
- **Codable**: Automatic JSON serialization for all data models
- **Security framework**: Native Keychain access APIs
- **No external dependencies**: Uses only Apple frameworks

## [1.2] - 2026-05-22

### Fixed
- **Android Release APK**: Changed release workflow to publish debug-signed APK instead of unsigned release APK
  - Debug APKs are automatically signed with debug keystore and can be installed without manual signing
  - Fixes "app not installed as package appears invalid" error
  - Release workflow now builds `assembleDebug` instead of `assembleRelease`
  - APK artifacts now upload even when unit tests fail (`if: success() || failure()`)
- **README**: Added release badges and quick download section for better visibility
  - Latest release badge with version number
  - Direct download links for Android APK
  - Quick Download section prominently placed

### Added
- **Android Mobile App** (jnexus-android/) - NEW native Android application
  - **Multi-module architecture**:
    - `jnexus-core`: Shared business logic module (Java 11, platform-agnostic)
    - `jnexus-android`: Android app module (Kotlin, Jetpack Compose, Material Design 3)
    - Desktop modules continue to use existing `src/` with Maven
  - **Platform abstraction interfaces**:
    - `NexusHttpClient` interface for HTTP layer (desktop uses java.net.http, Android uses OkHttp)
    - `Credentials` interface for credential storage (desktop uses file-based, Android uses EncryptedSharedPreferences)
  - **Android implementation**:
    - `NexusClientOkHttp`: OkHttp-based HTTP client with same caching, retry logic, and pagination as desktop
    - `CredentialsAndroid`: Secure credential storage using AES256_GCM encryption
    - `NexusApplication`: Application class for dependency injection
  - **Jetpack Compose UI with 4 screens**:
    - **List Screen**: Browse repository components
      - Repository dropdown selector
      - List (cached) and Refresh (force update) buttons
      - Component cards with size, creation date
      - Tap to view full metadata dialog
      - Delete with confirmation
    - **Search Screen**: Advanced filtering
      - Size range filters (min/max bytes)
      - Date range filters (ISO 8601 format)
      - File extension filter
      - Component name pattern
      - Path regex filter
      - Collapsible filter panel
    - **Stats Screen**: Repository analytics
      - Overview: total components, total size (MB/GB), average, median
      - Size distribution (5 buckets with percentages)
      - File type breakdown (top 10)
      - Age distribution (7/30/90 days, older)
      - Largest components (top 10)
    - **Settings Screen**: Configuration
      - Nexus URL, username, password (encrypted)
      - Repository list (comma-separated)
      - Default values (repository, regex, dry-run)
      - HTTP timeout
      - Save/clear actions
  - **Technical features**:
    - Material Design 3 UI with bottom navigation
    - Coroutines for async operations
    - Same SearchCriteria and RepositoryStats as desktop
    - Same caching (5-minute TTL) and retry logic (3 retries, exponential backoff)
    - Desugaring for Java 11 features (records) on Android 8.0+
  - **Security**:
    - AES256_GCM encryption for credentials
    - HTTPS-only (`usesCleartextTraffic=false`)
    - Minimal permissions (INTERNET, ACCESS_NETWORK_STATE)
  - **Testing**:
    - Unit tests for `NexusClientOkHttp` (OkHttp MockWebServer)
    - Instrumented tests for `CredentialsAndroid` (Android runtime)
  - **Documentation**:
    - `jnexus-android/README.md`: Complete Android documentation
    - `jnexus-android/ANDROID.md`: Setup and usage guide
  - **Requirements**: Android 8.0+ (API 26+)
  - **Build system**: Gradle 8.2.2, Kotlin 1.9.22, Compose 1.5.8

### Changed
- Refactored core business logic into shared `jnexus-core` module
  - Moved `NexusService`, `ComponentMetadata`, `SearchCriteria`, `RepositoryStats`, `RepoRecord` to jnexus-core
  - Created `NexusHttpClient` and `Credentials` interfaces for platform abstraction
  - Desktop `NexusClient` unchanged (implements NexusHttpClient implicitly via same method signatures)
  - All 155 desktop tests still passing, no breaking changes
- Updated project structure:
  - Root `build.gradle` and `settings.gradle` for Gradle multi-module build
  - Desktop continues using `pom.xml` and Maven (backward compatible)
  - New `jnexus-core/build.gradle` for shared library
  - New `jnexus-android/build.gradle` for Android app
- Updated README.md:
  - Added Android mobile app section
  - Updated from "four interfaces" to "five interfaces"
  - Added Android requirements and installation instructions

## [1.1] - 2024

### Added
- **Enhanced Nexus API Integration** - Advanced search, filtering, and statistics
  - **ComponentMetadata** data model with full metadata fields (contentType, format, createdDate, lastModified, checksum)
  - **SearchCriteria** data model with advanced filter support
  - **RepositoryStats** data model for comprehensive statistics
  - **Advanced Search & Filtering**:
    - Size range filters (min/max bytes)
    - Date range filters (created after/before)
    - File extension filter
    - Component name pattern matching
    - All filters can be combined
  - **Repository Statistics**:
    - Size distribution across 5 buckets (<1MB, 1-10MB, 10-100MB, 100MB-1GB, >1GB)
    - File type breakdown by extension
    - Age distribution (last 7/30/90 days, older)
    - Largest components (top 20)
    - Average and median size calculations
- **Enhanced CLI** (JNexus.java):
  - New `stats` command for repository statistics (text and JSON output)
  - Enhanced `list` command with new options:
    - `--min-size BYTES` - Minimum file size filter
    - `--max-size BYTES` - Maximum file size filter
    - `--created-after DATE` - Creation date filter (ISO 8601 format)
    - `--created-before DATE` - Creation date filter
    - `--extension EXT` - File extension filter
    - `--show-metadata` - Display full component metadata
- **Enhanced Swing GUI** (JNexusSwing.java):
  - **Advanced Filters Panel** (collapsible):
    - Min/max size filters
    - Created date range filters (ISO 8601 format)
    - File extension filter
  - **Enhanced Table Display**:
    - 7 columns: ID, File Size (Bytes), File Size (MB), File Size (GB), Created, Content Type, Path
    - Numeric sorting for all size columns
    - Date display in "yyyy-MM-dd HH:mm" format
  - **Component Details Dialog**:
    - Double-click any row to view full metadata
    - Shows ID, path, file size, content type, format, created date, last modified, checksum
  - **Statistics Dialog** with 5 tabs:
    - Overview: total components, total size, average, median
    - Size Distribution: histogram with percentages
    - File Types: breakdown by extension with sizes
    - Age Distribution: components by age ranges
    - Largest Components: top 20 by size
  - **Statistics Button**: Analyze current results
- **Metadata Caching**: Separate cache for component metadata with same TTL pattern

## [1.0] - 2024

### Added
- **Swing GUI** (JNexusSwing.java) - Modern graphical interface
  - Native look and feel using UIManager.setLookAndFeel
  - Background task execution with SwingWorker
  - GridBagLayout for responsive design
  - JOptionPane for error and confirmation dialogs
  - Scrollable JTextArea for results
  - Launcher script: jnexus-swing.sh
- **AWT GUI** (JNexusAWT.java) - Classic graphical interface
  - Pure AWT components (Frame, Button, TextField, TextArea, etc.)
  - Maximum compatibility with older Java installations
  - Thread-based background execution
  - Custom dialog implementations
  - Launcher script: jnexus-awt.sh
- Interactive terminal UI using jcurses library
- Ncurses-based full-screen interface with keyboard navigation
- UI components: text fields, buttons, checkboxes, result panels
- Separate "List" and "Refresh" buttons in UI for cache control
- **Intelligent caching system** for Nexus API queries
  - Time-based caching with 5-minute TTL (configurable)
  - Thread-safe using ConcurrentHashMap
  - Cache management methods: clearCache(), clearAllCache(), isCached(), getCacheAge()
  - Force refresh option to bypass cache
  - Automatic cache invalidation after delete operations
  - Cache disabled mode (TTL=0)
- **SLF4J logging framework** (replaces System.out.println)
  - Logback as implementation
  - Proper log levels: DEBUG, INFO, WARN, ERROR
  - Separate test configuration (logback-test.xml)
- **Verbose and quiet modes** via CLI flags
  - `--verbose` / `-v` for DEBUG level logging
  - `--quiet` / `-q` for WARN/ERROR only
  - Default: INFO level
- **HTTP retry logic** with exponential backoff
  - 3 retries maximum for transient failures
  - Retries on: connection errors, timeouts, 5xx server errors
  - Initial delay: 1 second, doubles each retry
- **Regex pattern validation**
  - Early validation before API calls
  - Clear error messages for invalid patterns
  - Prevents runtime PatternSyntaxException
- **Configurable HTTP timeout**
  - Environment variable: `NEXUS_HTTP_TIMEOUT`
  - Properties file: `nexus.http.timeout.seconds`
  - Default: 30 seconds
- **Progress indicators** for large delete operations
  - Shows progress every 5 deletions for >10 components
  - Example: "Progress: 25 of 100 components deleted (25.0%)"
- Comprehensive cache testing (NexusClientCacheTest.java with 11 tests)
- Cache status display in UI (shows cache age in seconds)
- Updated documentation for caching and UI features
- 6 new tests for validation, timeout config, and UI defaults

### Changed
- UI button layout simplified: "List Components" → "List", "Delete Components" → "Delete"
- List operations now use cached data by default (faster response)
- Delete operations always fetch fresh data (accuracy over speed)
- Status bar shows cache state and age
- NexusClient.listComponents() now has overload with forceRefresh parameter
- NexusService methods updated to support cache control
- Test suite updated to 61 tests total (55 original + 6 new tests)
- All System.out.println replaced with SLF4J logger calls
- Cache/debug messages moved to DEBUG level
- User messages at INFO level
- Errors at ERROR level

### Fixed
- **File size overflow**: Changed RepoRecord.fileSize from int to long
  - Now supports files up to ~8 exabytes (was limited to ~2GB)
  - Updated NexusClient to use getLong() instead of getInt()
- **Error handling**: Removed printStackTrace in production code
  - Stack traces only shown in verbose mode
  - User-friendly error messages by default
- **Hard-coded timeout**: HTTP timeout now configurable
- **No retry logic**: Added automatic retry for transient failures
- **Invalid regex crashes**: Now validated early with clear errors
- **Silent failures**: Proper logging at appropriate levels

### Technical Details
- Cache-aside pattern implementation
- Defensive copying to prevent external modification of cached data
- Cache entries include timestamp for TTL calculation
- Per-repository cache isolation
- Retry logic uses exponential backoff: 1s, 2s, 4s
- Logback configuration: pattern-less for clean user output
- Separate logback-test.xml for test output capture
- JAR size increased to ~3.7MB (from 2.7MB) with SLF4J/Logback additions

## [1.0] - 2026-05-16

### Added
- Initial release of JNexus CLI tool
- Java 21 support
- `list` command to display repository components
- `delete` command to remove repository components
- Regex filtering for both list and delete operations
- Dry-run mode for delete operations (`--dry-run` flag)
- Confirmation prompts for delete operations
- Skip confirmation with `--yes` flag
- Pagination support for large repositories
- Statistics display (component count, total size)
- Support for configuration via environment variables
- Support for configuration via properties file (~/.flossware/nexus/nexus.properties)
- Comprehensive unit test suite (44 tests)
- Integration tests with mock HTTP server
- Complete Javadoc documentation
- User documentation (README.md)
- Contributing guidelines (CONTRIBUTING.md)

### Technical Details
- Removed Spring Boot dependencies (reduced from ~50MB to 2.7MB)
- Implemented with plain Java 21 and minimal dependencies
- Startup time reduced from 3-5 seconds to <200ms
- Uses Picocli for command-line parsing
- Uses Java's HttpClient for HTTP communication
- Uses Jackson for JSON processing

## [0.x] - Pre-1.0 (Legacy Spring Boot Version)

### Changed
- Used Spring Boot 3.0.8 framework
- Java 17 support
- Larger JAR size (~50MB)
- Slower startup time (3-5 seconds)
- RestTemplate for HTTP communication
- Spring-based dependency injection

---

## Release Notes Format

### Version Number Format (X.Y)
This project uses **X.Y** versioning (e.g., 1.0, 1.1, 2.0):
- **X (Major)** version when you make incompatible API changes or major feature releases
- **Y (Minor)** version when you add functionality, bug fixes, or improvements in a backwards compatible manner

**Note:** We use X.Y format instead of X.Y.Z (Semantic Versioning) to simplify version management for this CLI tool

### Change Categories
- **Added** for new features
- **Changed** for changes in existing functionality
- **Deprecated** for soon-to-be removed features
- **Removed** for now removed features
- **Fixed** for any bug fixes
- **Security** for vulnerability fixes

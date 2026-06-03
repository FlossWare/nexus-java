# JNexus Desktop - Implementation Guide

Back to [Main Documentation](CLAUDE.md)

## Overview

The desktop module (`src/`) provides a Java 21 application with 4 different user interfaces:
- **CLI**: Command-line interface with Picocli
- **Swing GUI**: Modern graphical interface with table-based display
- **AWT GUI**: Classic graphical interface for maximum compatibility
- **Terminal UI**: Full-screen ncurses interface with jcurses

All UIs share the same core components and connect to the jnexus-core business logic.

## Technology Stack

**Core Technologies:**
- **Java 21**: Modern Java with records, text blocks, pattern matching
- **Picocli**: Lightweight CLI framework with subcommands and option parsing
- **Java HttpClient**: Built-in HTTP/2 client (no external dependencies)
- **Maven**: Build system

**UI Technologies:**
- **Swing**: Modern GUI framework (built into JDK)
- **AWT**: Classic GUI framework (built into JDK, maximum compatibility)
- **jcurses**: Terminal UI library for ncurses-based interface

**Dependencies:**
- Jackson (JSON processing)
- org.json (JSON manipulation)
- SLF4J (logging API)
- JUnit 5 + Mockito (testing)

## Desktop Module Components

### Core Components

- **JNexus.java**: CLI parsing, user interaction, command routing (list, delete, stats commands)
- **JNexusSwing.java**: Modern Swing GUI with table-based display and analytics
- **JNexusAWT.java**: Classic AWT GUI with text-based display
- **JNexusUI.java**: Terminal UI using jcurses with text-based profile selection
- **NexusClient.java**: HTTP communication, pagination, JSON parsing, metadata extraction (java.net.http)
- **Credentials.java**: Configuration management (env vars → properties file) + optional UI defaults + profile discovery

### Layered Architecture

```
UI Layer (JNexus.java, JNexusSwing.java, JNexusAWT.java, JNexusUI.java)
    ↓
Service Layer (NexusService.java from jnexus-core)
    ↓
Client Layer (NexusClient.java implements NexusHttpClient)
    ↓
HTTP/Nexus API (java.net.http.HttpClient)
```

## User Interfaces

### 1. Swing GUI (JNexusSwing.java)

Modern graphical interface with enhanced table-based display and analytics.

**Key Features:**
- **JTable with 7 columns**: ID, File Size (Bytes), File Size (MB), File Size (GB), Created, Content Type, Path
- **Sortable columns** with numeric sorting for size columns (JTable.setAutoCreateRowSorter)
- **Multi-row selection** with ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
- **Custom cell renderer** highlights summary row (light blue background)
- **Smart Delete Selected button** - visible only when rows are selected
- **Selection listener** - updates status bar with selection summary

**Advanced Filters Panel** (collapsible with toggle button):
- Min/max size filters (text fields for bytes)
- Created date range filters (text fields for ISO 8601 format)
- File extension filter (text field)
- Uses GridBagLayout for responsive filter layout

**Component Details Dialog**:
- Double-click any row to view full metadata
- Shows: ID, path, file size, content type, format, created date, last modified, checksum
- Implemented with JOptionPane.showMessageDialog

**Statistics Dialog** (accessible via "Statistics" button):
- JTabbedPane with 5 tabs: Overview, Size Distribution, File Types, Age Distribution, Largest Components
- Overview: total components, total size (MB/GB), average, median
- Size Distribution: histogram with 5 buckets and percentages
- File Types: breakdown by extension with sizes
- Age Distribution: components by age ranges (7/30/90 days, older)
- Largest Components: top 20 by size in JTable

**Additional Features:**
- Smart Status Area: Shows totals from table footer
- SwingWorker for background tasks
- Native look and feel via UIManager
- Busy cursor (WAIT_CURSOR) during operations
- Enter key listener on regex field triggers List operation
- Repository dropdown selector - JComboBox with "All" + configured repos
- Nexus URL and Config File displays - read-only fields showing connection details
- Automatic profile selection - JOptionPane.showInputDialog with dropdown

**Best for**: Desktop users who prefer modern GUIs with spreadsheet-like interface and analytics

### 2. AWT GUI (JNexusAWT.java)

Classic graphical interface with formatted text output.

**Key Features:**
- Uses NexusService.formatRecordsWithHeaders() for column-aligned display
- Summary footer with total components and bytes
- Uses Thread for background tasks
- Pure AWT components (Frame, Button, TextField, TextArea, Choice)
- Busy cursor (WAIT_CURSOR) during operations
- Enter key listeners on text fields trigger List operation
- Repository dropdown - Choice component auto-fills Repository field
- Automatic profile selection - Custom Dialog with Choice component

**Best for**: Maximum compatibility, remote desktop, VNC

### 3. Terminal UI (JNexusUI.java)

Full-screen ncurses interface.

**Key Features:**
- Uses jcurses library
- Keyboard navigation (TAB, arrows, SPACE, etc.)
- Automatic profile selection - Text menu before ncurses initialization
- Repository display - Shows `nexus.repositories` list as comma-separated (dynamically adjusts layout)
- Pre-populated with default values from Credentials

**Best for**: SSH sessions, terminal users, servers

### 4. CLI (JNexus.java)

Command-line interface with three commands: list, delete, stats.

**List command** - Enhanced with advanced filtering options:
- `--min-size BYTES`: Minimum file size filter
- `--max-size BYTES`: Maximum file size filter
- `--created-after DATE`: Creation date filter (ISO 8601 format)
- `--created-before DATE`: Creation date filter
- `--extension EXT`: File extension filter
- `--show-metadata`: Display full component metadata (not just path)

**Delete command** - Remove components with confirmation prompts

**Stats command** - Repository statistics:
- `--format text`: Human-readable text output (default)
- `--format json`: JSON output for scripting
- Displays: totals, size distribution, file types, age distribution, largest components

**Global flags:**
- `--verbose`: Verbose output
- `--quiet`: Quiet mode
- `--profile PROFILE`: Specify configuration profile

**Best for**: Scripting, automation, CI/CD, analytics

## UI Implementation Patterns

### Background Task Execution

- **Swing**: SwingWorker<String, Void> for async operations
- **AWT**: new Thread(() -> { ... }).start() with EventQueue.invokeLater
- **Terminal**: Direct execution in event loop
- **CLI**: Direct execution (synchronous)

### Data Access Patterns

- **Swing**: Uses `service.getRepositoryRecords()` to get List<RepoRecord>, populates JTable directly
- **AWT**: Uses `service.getRepositoryRecords()` + `service.formatRecordsWithHeaders()` for formatted text
- **Terminal**: Captures System.out from `service.listRepository()`
- **CLI**: Prints directly to stdout via `service.listRepository()`

### Output Capture (AWT/Terminal/Delete operations)

Some GUIs capture System.out/System.err to display in results area:
```java
ByteArrayOutputStream baos = new ByteArrayOutputStream();
PrintStream ps = new PrintStream(baos);
System.setOut(ps);
try {
    service.listRepository(...);
} finally {
    System.setOut(originalOut);
}
String output = baos.toString();
```

### Dialog Patterns

- **Swing**: JOptionPane.showMessageDialog / showConfirmDialog
- **AWT**: Custom Dialog with Button listeners
- **Terminal**: Direct status label updates
- **CLI**: Console input with confirmation prompts

### Profile Selection (When Multiple Profiles Exist)

- **Swing**: `JOptionPane.showInputDialog()` with array of profile names
- **AWT**: Custom `Dialog` with `Choice` component (dropdown) and OK/Cancel buttons
- **Terminal**: Text menu with numbered options before ncurses initialization
- **CLI**: Use `--profile` flag to specify profile explicitly

All GUIs use `Credentials.discoverProfiles()` to find available profiles:
```java
List<String> profiles = Credentials.discoverProfiles();
if (profiles.size() > 1) {
    // Show selection dialog
    String selected = showProfileSelectionDialog(profiles);
    // Convert "default" to null for Credentials constructor
    credentials = new Credentials("default".equals(selected) ? null : selected);
} else if (profiles.size() == 1) {
    // Auto-select single profile
    String profile = "default".equals(profiles.get(0)) ? null : profiles.get(0);
    credentials = new Credentials(profile);
} else {
    // Show error - no configuration found
}
```

### Available Repositories Display Pattern

```java
if (!credentials.getRepositories().isEmpty()) {
    // Swing: JTextArea with comma-separated list
    // AWT: TextArea with comma-separated list
    // Terminal: JLabel with comma-separated list
    String repoList = String.join(", ", credentials.getRepositories());
}
```

## Configuration (Credentials.java)

Loads configuration from multiple sources:

**Required fields**: `nexus.url`, `nexus.user`, `nexus.password`
- Environment variables: `NEXUS_URL`, `NEXUS_USER`, `NEXUS_PASSWORD` (highest priority)
- Properties file: `~/.flossware/nexus/nexus.properties` (default)
- Profile-based properties file: `~/.flossware/nexus/nexus-{profile}.properties`

**Optional UI defaults**: `nexus.default.repository`, `nexus.default.regex`, `nexus.default.dryrun`
- Only loaded from properties file
- Pre-populate UI fields on startup
- Empty strings and `true` used as defaults if not specified

**Optional repository list**: `nexus.repositories`
- Comma-separated list of repository names
- Used by GUIs for dropdown menus or batch operations
- Example: `maven-releases,maven-snapshots,npm-public`
- Returns empty list if not configured

**Optional HTTP configuration**: `nexus.http.timeout.seconds`
- Also available via `NEXUS_HTTP_TIMEOUT` environment variable
- Default: 30 seconds

**Profile support**: Multiple configuration files for different environments
- Specify via `NEXUS_PROFILE` environment variable or `--profile` CLI flag
- Profile name determines file: `nexus-{profile}.properties`
- Examples: `dev` → `nexus-dev.properties`, `prod` → `nexus-prod.properties`
- Null/empty/blank profile defaults to `nexus.properties`
- Inspired by Spring Boot profiles for familiar workflow

### Example Properties File

```properties
# Required
nexus.url=https://nexus.example.com
nexus.user=admin
nexus.password=secret

# Optional UI defaults
nexus.default.repository=maven-releases  # Auto-populated if nexus.repositories is set
nexus.default.regex=.*SNAPSHOT.*
nexus.default.dryrun=true

# Optional repository list (comma-separated)
nexus.repositories=maven-releases,maven-snapshots,npm-public,docker-hosted

# Optional HTTP timeout
nexus.http.timeout.seconds=30
```

### Interactive Credential Collection Behavior

- When credentials are entered via dialog and saved, the first repository in the list becomes the default
- This auto-populates `nexus.default.repository` in the saved properties file
- On next launch, the Repository field is pre-filled with this value

### Profile-Based Configuration Example

```bash
# Create profile-specific files
~/.flossware/nexus/nexus-dev.properties
~/.flossware/nexus/nexus-prod.properties
~/.flossware/nexus/nexus-staging.properties

# Use via environment variable
export NEXUS_PROFILE=dev
./jnexus.sh list my-repository

# Or via CLI flag
./jnexus.sh --profile prod list my-repository
```

## HTTP Client (NexusClient.java)

Implements NexusHttpClient interface using java.net.http.HttpClient.

**Key Features:**
- HTTP/2 support
- Automatic pagination with continuation tokens
- JSON parsing with Jackson + org.json
- Metadata extraction from Nexus API responses
- Retry logic with exponential backoff (1s, 2s, 4s)
- Retries on: 5xx, 408, 429, connection errors, timeouts

**Caching:**
- Time-based caching with ConcurrentHashMap
- Default TTL: 5 minutes (300 seconds)
- Separate caches for RepoRecord and ComponentMetadata
- Thread-safe using ConcurrentHashMap
- `isCached()` and `getCacheAge()` helper methods

**Methods:**
- `listComponents(repository)`: List basic component records (cached)
- `listComponents(repository, forceRefresh)`: Force fresh fetch (bypass cache)
- `listComponentsWithMetadata(repository)`: List components with full metadata (cached)
- `deleteComponent(repository, componentId)`: Delete a component (always fresh)
- `clearCache()`: Clear all caches
- `clearCache(repository)`: Clear cache for specific repository

## Testing

### Configuration Tests (CredentialsTest.java)
- Test loading from properties files
- Test missing/blank credentials validation
- Test UI defaults parsing
- Test repository list parsing (single, multiple, with spaces)
- Test HTTP timeout configuration
- Test profile-based configuration loading
- Test profile selection (dev, prod, staging)
- Test default profile when null/empty/blank
- Test profile file not found error handling
- Use `@TempDir` for isolated file system tests

### Unit Tests (NexusServiceTest.java)
- Mock NexusClient
- Test filtering logic
- Test statistics calculation
- Test error handling

### Cache Tests (NexusClientCacheTest.java)
- Test cache hit/miss behavior
- Test cache expiration after TTL
- Test force refresh bypasses cache
- Test cache per repository (isolation)
- Test cache clearing (single and all)
- Test cache status methods (isCached, getCacheAge)
- Test cache disabled mode (TTL=0)
- Test defensive copy behavior

### Integration Tests (NexusClientIntegrationTest.java)
- Use Java's built-in `com.sun.net.httpserver.HttpServer`
- Test pagination with real HTTP
- Test authentication headers
- Test various HTTP status codes

### Test Data Patterns
- Use `@TempDir` for file system tests
- Use `ByteArrayOutputStream` to capture console output
- Mock HttpClient responses with Mockito
- Use `AtomicInteger` to count HTTP requests in cache tests

## Build and Deployment

### Building with Maven

```bash
./mvnw clean package
```

**Artifacts:**
- `target/jnexus-1.0-jar-with-dependencies.jar`: Fat JAR (2.7MB, standalone)

### Running

```bash
java -jar target/jnexus-1.0-jar-with-dependencies.jar [command] [options]
```

### Launcher Scripts

- `jnexus.sh`: CLI launcher
- `jnexus-swing.sh`: Swing GUI launcher
- `jnexus-awt.sh`: AWT GUI launcher
- `jnexus-ui.sh`: Terminal UI launcher

### CI/CD

See [CI-CD.md](../CI-CD.md) for complete CI/CD documentation.

**GitHub Actions** (`.github/workflows/main.yml`):
- Builds JAR on every push
- Runs 155 tests
- Deploys to packagecloud.io
- Auto-bumps version

## Common Development Tasks

### Adding a New UI

1. Create new class extending appropriate framework (Swing/AWT/jcurses)
2. Initialize NexusClient and NexusService in constructor
3. Implement profile selection dialog (if multiple profiles exist)
4. Create input fields for repository, regex, dry-run
5. Add available repositories display if `credentials.getRepositories()` is not empty
6. Add buttons for List, Refresh, Delete, Clear, Quit
7. Implement background task execution pattern
8. Capture System.out for results display
9. Create launcher script (jnexus-{name}.sh)
10. Update README.md and CHANGELOG.md

### Adding a New Command

1. Create subclass in `JNexus.java` implementing `Callable<Integer>`
2. Add `@Command` annotation with name and description
3. Add to `subcommands` array in main `@Command`
4. Add method to `NexusService.java` for business logic
5. Add tests for new command

### Adding a New Nexus API Operation

1. Add method to `NexusClient.java` for HTTP communication
2. Add method to `NexusService.java` for business logic and output
3. Add unit tests in `NexusServiceTest.java`
4. Add integration test in `NexusClientIntegrationTest.java`
5. Add command in `JNexus.java` to expose functionality

### Modifying JSON Parsing

- JSON response parsed via Jackson → Map → JSONObject
- This two-step approach allows both Jackson and org.json libraries
- Consider refactoring to use only Jackson if adding complex JSON handling

## Debugging

### Remote Debugging

```bash
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 \
  -jar target/jnexus-1.0-jar-with-dependencies.jar list my-repo
```

### Verbose Output

Add to service methods:
```java
System.out.println("DEBUG: fetching page with token: " + token);
```

### HTTP Request Inspection

Add to `NexusClient.fetchComponents()`:
```java
System.out.println("Request Headers: " + request.headers());
System.out.println("Response: " + response.body());
```

Back to [Main Documentation](CLAUDE.md)

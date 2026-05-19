# JNexus CLI - Codebase Documentation for AI Assistants

## Project Overview

**JNexus CLI** is a Java 21 command-line tool for managing components in Sonatype Nexus Repository Manager. It provides list and delete operations with regex filtering, safety features, and minimal dependencies.

## Key Architectural Decisions

### Why No Spring Boot?
- **Original Problem**: Spring Boot added 50MB JAR size and 3-5 second startup overhead for a simple CLI tool
- **Solution**: Refactored to plain Java 21 with minimal dependencies
- **Result**: 2.7MB JAR, <200ms startup time

### Technology Choices
- **Java 21**: Modern features (records, text blocks, pattern matching)
- **Picocli**: Lightweight CLI framework with subcommands and option parsing
- **Java HttpClient**: Built-in HTTP/2 client (no external dependencies)
- **Jackson**: Standard JSON processing
- **Swing**: Modern GUI framework (built into JDK, no external dependencies)
- **AWT**: Classic GUI framework (built into JDK, maximum compatibility)
- **jcurses**: Terminal UI library for ncurses-based interface
- **SLF4J + Logback**: Logging framework with proper log levels
- **JUnit 5 + Mockito**: Modern testing stack

## Code Architecture

### Layered Architecture
```
UI Layer (JNexus.java, JNexusSwing.java, JNexusAWT.java, JNexusUI.java)
    ↓
Service Layer (NexusService.java)
    ↓
Client Layer (NexusClient.java)
    ↓
HTTP/Nexus API
```

### Separation of Concerns
- **JNexus.java**: CLI parsing, user interaction, command routing
- **JNexusSwing.java**: Modern Swing GUI (JFrame, JPanel, SwingWorker) with automatic profile selection
- **JNexusAWT.java**: Classic AWT GUI (Frame, Button, TextField) with automatic profile selection
- **JNexusUI.java**: Terminal UI using jcurses with text-based profile selection, pre-populated with default values from Credentials
- **NexusService.java**: Business logic, filtering, statistics, output formatting
- **NexusClient.java**: HTTP communication, pagination, JSON parsing
- **Credentials.java**: Configuration management (env vars → properties file) + optional UI defaults + profile discovery
- **RepoRecord.java**: Immutable data model (Java record)

## Design Patterns

### Strategy Pattern
- Multiple configuration sources (env vars vs properties file) with fallback

### Builder Pattern
- HttpClient creation with configuration

### Repository Pattern
- NexusClient abstracts HTTP API details from business logic

### Cache-Aside Pattern
- Time-based caching in NexusClient with configurable TTL
- Cache key: repository name
- Cache value: List<RepoRecord> + timestamp
- Default TTL: 5 minutes (300 seconds)
- Thread-safe using ConcurrentHashMap

## Important Implementation Details

### Pagination
- Nexus API uses continuation tokens for pagination
- `NexusClient.listComponents()` transparently handles all pages
- URL construction: `baseUrl + "&continuationToken=" + token`

### Authentication
- HTTP Basic Auth via Authorization header
- Base64 encoding: `Base64.getEncoder().encodeToString((user + ":" + password).getBytes())`

### Configuration
**Credentials.java** loads configuration from multiple sources:
- **Required fields**: `nexus.url`, `nexus.user`, `nexus.password`
  - Environment variables: `NEXUS_URL`, `NEXUS_USER`, `NEXUS_PASSWORD` (highest priority)
  - Properties file: `~/.flossware/nexus/nexus.properties` (default)
  - Profile-based properties file: `~/.flossware/nexus/nexus-{profile}.properties`
- **Optional UI defaults**: `nexus.default.repository`, `nexus.default.regex`, `nexus.default.dryrun`
  - Only loaded from properties file
  - Pre-populate UI fields on startup
  - Empty strings and `true` used as defaults if not specified
- **Optional repository list**: `nexus.repositories`
  - Comma-separated list of repository names
  - Used by GUIs for dropdown menus or batch operations
  - Example: `maven-releases,maven-snapshots,npm-public`
  - Returns empty list if not configured
- **Optional HTTP configuration**: `nexus.http.timeout.seconds`
  - Also available via `NEXUS_HTTP_TIMEOUT` environment variable
  - Default: 30 seconds
- **Profile support**: Multiple configuration files for different environments
  - Specify via `NEXUS_PROFILE` environment variable or `--profile` CLI flag
  - Profile name determines file: `nexus-{profile}.properties`
  - Examples: `dev` → `nexus-dev.properties`, `prod` → `nexus-prod.properties`
  - Null/empty/blank profile defaults to `nexus.properties`
  - Inspired by Spring Boot profiles for familiar workflow

**Example properties file:**
```properties
# Required
nexus.url=https://nexus.example.com
nexus.user=admin
nexus.password=secret

# Optional UI defaults
nexus.default.repository=maven-releases
nexus.default.regex=.*SNAPSHOT.*
nexus.default.dryrun=true

# Optional repository list (comma-separated)
nexus.repositories=maven-releases,maven-snapshots,npm-public,docker-hosted

# Optional HTTP timeout
nexus.http.timeout.seconds=30
```

**Profile-based configuration example:**
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

### Error Handling
- Service layer catches individual delete failures and continues processing
- HTTP errors throw IOException with status code
- Configuration errors throw IllegalStateException

### Regex Filtering
- Applied in service layer after fetching all components
- Uses Java's `String.matches(regex)` method
- Invalid regex throws PatternSyntaxException

### Caching
- Implemented in `NexusClient` using `ConcurrentHashMap<String, CacheEntry>`
- **Cache key**: repository name (String)
- **Cache value**: `CacheEntry(List<RepoRecord> records, Instant timestamp)`
- **Default TTL**: 300 seconds (5 minutes)
- **Thread safety**: ConcurrentHashMap ensures safe concurrent access
- **Cache invalidation**: Automatic expiration after TTL, manual via `clearCache()`, auto-clear after deletions
- **Bypass mechanism**: `listComponents(repo, true)` forces fresh fetch
- **Disable caching**: Set TTL to 0 in constructor
- **Defensive copies**: Cache returns new LinkedList to prevent external modification
- **Cache status**: Methods `isCached()` and `getCacheAge()` for monitoring

**Caching Strategy:**
- List operations use cache by default (fast, may be slightly stale)
- Delete operations always fetch fresh data (accuracy over speed)
- Delete operations clear cache after completion (maintain consistency)
- UI exposes both cached (List) and fresh (Refresh) operations

## Testing Strategy

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

### Test Data Patterns
- Use `@TempDir` for file system tests
- Use `ByteArrayOutputStream` to capture console output
- Mock HttpClient responses with Mockito

## User Interfaces

### Four UI Options

1. **Swing GUI (JNexusSwing.java)**
   - Modern graphical interface
   - Uses SwingWorker for background tasks
   - Native look and feel via UIManager
   - **Automatic profile selection** - JOptionPane.showInputDialog with dropdown
   - **Available repositories display** - Shows `nexus.repositories` list if configured
   - Best for: Desktop users who prefer modern GUIs

2. **AWT GUI (JNexusAWT.java)**
   - Classic graphical interface
   - Uses Thread for background tasks
   - Pure AWT components (Frame, Button, etc.)
   - **Automatic profile selection** - Custom Dialog with Choice component
   - **Available repositories display** - Shows `nexus.repositories` list if configured
   - Best for: Maximum compatibility, remote desktop, VNC

3. **Terminal UI (JNexusUI.java)**
   - Full-screen ncurses interface
   - Uses jcurses library
   - Keyboard navigation (TAB, arrows, SPACE, etc.)
   - **Automatic profile selection** - Text menu before ncurses initialization
   - **Available repositories display** - Shows `nexus.repositories` list if configured (dynamically adjusts layout)
   - Best for: SSH sessions, terminal users, servers

4. **CLI (JNexus.java)**
   - Command-line interface
   - Uses Picocli for argument parsing
   - Supports --verbose, --quiet, and --profile flags
   - Best for: Scripting, automation, CI/CD

### UI Implementation Patterns

**Background Task Execution:**
- **Swing**: SwingWorker<String, Void> for async operations
- **AWT**: new Thread(() -> { ... }).start() with EventQueue.invokeLater
- **Terminal**: Direct execution in event loop
- **CLI**: Direct execution (synchronous)

**Output Capture:**
All GUIs capture System.out/System.err to display in results area:
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

**Dialog Patterns:**
- **Swing**: JOptionPane.showMessageDialog / showConfirmDialog
- **AWT**: Custom Dialog with Button listeners
- **Terminal**: Direct status label updates
- **CLI**: Console input with confirmation prompts

**Profile Selection (When Multiple Profiles Exist):**
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

**Available Repositories Display Pattern:**
```java
if (!credentials.getRepositories().isEmpty()) {
    // Swing: JTextArea with comma-separated list
    // AWT: TextArea with comma-separated list
    // Terminal: JLabel with comma-separated list
    String repoList = String.join(", ", credentials.getRepositories());
}
```

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

## Known Limitations

### Single Asset Per Component (Design Choice)
- `NexusClient.parseComponentsResponse()` only uses first asset
- Components can have multiple assets (e.g., JAR + POM + sources)
- Displays path/size of first asset only
- **Rationale**: Simplicity and common case optimization
  - Most components have a single primary asset
  - Multi-asset handling would require UI changes to display multiple paths
  - File size aggregation across assets could be misleading
  - Current approach provides consistent, predictable output
- **Impact**: Statistics may underreport total size for multi-asset components
- **Future Enhancement**: Could add `--show-all-assets` flag if needed

### No Parallel Deletion
- Components deleted sequentially
- Could be parallelized with ExecutorService
- Current approach is safer and easier to track

## Security Considerations

### Credential Storage
- Never commit `nexus.properties` file
- Environment variables preferred for CI/CD
- Properties file for local development only
- No credential encryption (rely on file system permissions)

### Confirmation Prompts
- Delete requires "yes" confirmation (not just "y")
- Prevents accidental deletion from shell history or scripts
- Can be skipped with `--yes` flag when intended

### No Delete Undo
- Nexus delete operations are permanent
- Always recommend testing with `--dry-run` first
- Document this prominently in user-facing documentation

## Build and Release

### Building
```bash
./mvnw clean package
```

### Artifacts
- `nexus-1.0.jar`: Thin JAR (18KB, requires classpath)
- `jnexus-1.0-jar-with-dependencies.jar`: Fat JAR (2.7MB, standalone)

### Running
```bash
java -jar target/jnexus-1.0-jar-with-dependencies.jar [command] [options]
```

## Future Enhancements (Not Yet Implemented)

### High Priority
- Upload command for publishing artifacts
- Search command with advanced filtering
- Batch operations from file input
- Progress bars for large operations

### Medium Priority
- Support for multiple repository operations
- Export/import component lists (CSV, JSON)
- Verbose/debug logging modes
- Configuration profiles for multiple Nexus instances

### Low Priority
- Tab completion for bash/zsh
- Native image compilation with GraalVM
- Plugin system for custom operations
- Web UI companion tool

## Debugging Tips

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

## Code Conventions

### Naming
- Commands use verb form: `list`, `delete`, not `ListCommand`
- Methods use action verbs: `fetchComponents()`, not `getComponents()`
- Test methods: `testMethodName_whenCondition_thenExpectedBehavior()`

### Constants
- No magic numbers; use named constants
- Configuration paths: use `Paths.get()` with constants

### Error Messages
- User-facing: Simple, actionable
- Developer-facing: Include context (file path, HTTP status, etc.)

## When Making Changes

### Always Update
1. Unit tests (verify existing tests still pass)
2. Javadoc (for public API changes)
3. README.md (for user-facing changes)
4. CHANGELOG.md (for all changes)
5. This file (for architectural changes)

### Never
- Break backward compatibility without major version bump
- Add Spring or other heavy frameworks
- Commit credentials or sensitive data
- Modify behavior without adding tests

## Questions or Issues?

- Check existing tests for usage examples
- Review Javadoc for API documentation
- See CONTRIBUTING.md for development guidelines
- Open GitHub issue for bugs or feature requests

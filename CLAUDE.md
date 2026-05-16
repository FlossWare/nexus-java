# Nexus CLI - Codebase Documentation for AI Assistants

## Project Overview

**Nexus CLI** is a Java 21 command-line tool for managing components in Sonatype Nexus Repository Manager. It provides list and delete operations with regex filtering, safety features, and minimal dependencies.

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
- **JUnit 5 + Mockito**: Modern testing stack

## Code Architecture

### Layered Architecture
```
CLI Layer (Nexus.java)
    ↓
Service Layer (NexusService.java)
    ↓
Client Layer (NexusClient.java)
    ↓
HTTP/Nexus API
```

### Separation of Concerns
- **Nexus.java**: CLI parsing, user interaction, command routing
- **NexusService.java**: Business logic, filtering, statistics, output formatting
- **NexusClient.java**: HTTP communication, pagination, JSON parsing
- **Credentials.java**: Configuration management (env vars → properties file) + optional UI defaults
- **NexusUI.java**: Terminal UI using jcurses, pre-populated with default values from Credentials
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
  - Properties file: `~/.flossware/nexus/nexus.properties`
- **Optional UI defaults**: `nexus.default.repository`, `nexus.default.regex`, `nexus.default.dryrun`
  - Only loaded from properties file
  - Pre-populate terminal UI fields on startup
  - Empty strings and `true` used as defaults if not specified

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

### Unit Tests (NexusServiceTest.java)
- Mock NexusClient
- Test filtering logic
- Test statistics calculation
- Test error handling

### Integration Tests (NexusClientIntegrationTest.java)
- Use Java's built-in `com.sun.net.httpserver.HttpServer`
- Test pagination with real HTTP
- Test authentication headers
- Test various HTTP status codes

### Test Data Patterns
- Use `@TempDir` for file system tests
- Use `ByteArrayOutputStream` to capture console output
- Mock HttpClient responses with Mockito

## Common Development Tasks

### Adding a New Command
1. Create subclass in `Nexus.java` implementing `Callable<Integer>`
2. Add `@Command` annotation with name and description
3. Add to `subcommands` array in main `@Command`
4. Add method to `NexusService.java` for business logic
5. Add tests for new command

### Adding a New Nexus API Operation
1. Add method to `NexusClient.java` for HTTP communication
2. Add method to `NexusService.java` for business logic and output
3. Add unit tests in `NexusServiceTest.java`
4. Add integration test in `NexusClientIntegrationTest.java`
5. Add command in `Nexus.java` to expose functionality

### Modifying JSON Parsing
- JSON response parsed via Jackson → Map → JSONObject
- This two-step approach allows both Jackson and org.json libraries
- Consider refactoring to use only Jackson if adding complex JSON handling

## Known Limitations

### File Size Limitation
- `RepoRecord.fileSize` is `int` (max ~2GB)
- Nexus can store larger files
- Should be migrated to `long` if supporting very large artifacts

### Single Asset Per Component
- `NexusClient.parseComponentsResponse()` only uses first asset
- Components can have multiple assets
- Displays path/size of first asset only

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
- `nexus-1.0-jar-with-dependencies.jar`: Fat JAR (2.7MB, standalone)

### Running
```bash
java -jar target/nexus-1.0-jar-with-dependencies.jar [command] [options]
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
  -jar target/nexus-1.0-jar-with-dependencies.jar list my-repo
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

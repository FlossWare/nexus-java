# JNexus - Development Guide

Back to [Main Documentation](CLAUDE.md)

## Quick Links

- **Desktop Development**: See [CLAUDE_DESKTOP.md](CLAUDE_DESKTOP.md)
- **Android Development**: See [CLAUDE_ANDROID.md](CLAUDE_ANDROID.md)
- **iOS/macOS Development**: See [CLAUDE_IOS.md](CLAUDE_IOS.md)
- **Testing**: See [TEST_COVERAGE.md](TEST_COVERAGE.md)
- **CI/CD**: See [CI-CD.md](CI-CD.md)
- **Security**: See [SECURITY.md](SECURITY.md)

## Development Environment Setup

### Desktop (Java)

**Requirements:**
- Java 21 JDK (OpenJDK or Oracle)
- Maven 3.6+
- Git

**Setup:**
```bash
# Clone repository
git clone https://github.com/FlossWare/nexus-java.git
cd nexus-java

# Build
./mvnw clean package

# Run tests
./mvnw test

# Run CLI
java -jar target/jnexus-1.0-jar-with-dependencies.jar --help
```

### Android

**Requirements:**
- Android Studio Arctic Fox or later
- Android SDK 34
- Gradle 8.0+
- Java 11+ (for Gradle)

**Setup:**
```bash
# Clone repository
git clone https://github.com/FlossWare/nexus-java.git
cd nexus-java

# Build from command line
gradle :jnexus-android:assembleDebug

# Or open in Android Studio
# File → Open → select jnexus-android/
```

### iOS/macOS

**Requirements:**
- macOS 13.0+ (Ventura or later)
- Xcode 15+ (includes Swift 5.9+)
- Apple Developer account (for device testing)

**Setup:**
```bash
# Clone repository
git clone https://github.com/FlossWare/nexus-java.git
cd nexus-java/jnexus-ios

# Open in Xcode
open JNexus.xcodeproj

# Or build from command line
xcodebuild -scheme JNexus-iOS -configuration Debug
```

## Code Conventions

### Naming

**Java/Kotlin:**
- Classes: PascalCase (e.g., `NexusClient`, `RepositoryStats`)
- Methods: camelCase (e.g., `listComponents()`, `calculateStatistics()`)
- Constants: UPPER_SNAKE_CASE (e.g., `DEFAULT_TIMEOUT`, `CACHE_TTL`)
- Variables: camelCase (e.g., `componentId`, `totalSize`)
- Commands: verb form (e.g., `list`, `delete`, not `ListCommand`)

**Swift:**
- Types: PascalCase (e.g., `NexusService`, `RepoRecord`)
- Functions/methods: camelCase (e.g., `listComponents()`, `saveCredentials()`)
- Constants: camelCase (e.g., `defaultTimeout`, `cacheTTL`)
- Variables: camelCase (e.g., `componentId`, `totalSize`)

### Test Methods

**Java:**
```java
@Test
void testMethodName_whenCondition_thenExpectedBehavior() {
    // Arrange
    // Act
    // Assert
}
```

**Kotlin:**
```kotlin
@Test
fun `method name - when condition - then expected behavior`() {
    // Arrange
    // Act
    // Assert
}
```

**Swift:**
```swift
func testMethodName_whenCondition_thenExpectedBehavior() {
    // Arrange
    // Act
    // Assert
}
```

### Constants

No magic numbers; use named constants:

**Java:**
```java
private static final int DEFAULT_HTTP_TIMEOUT_SECONDS = 30;
private static final long CACHE_TTL_SECONDS = 300;
private static final int MAX_RETRIES = 3;
```

**Swift:**
```swift
private let defaultHttpTimeout: TimeInterval = 30
private let cacheTTL: TimeInterval = 300
private let maxRetries = 3
```

### Configuration Paths

Use constants and platform-appropriate path handling:

**Java:**
```java
private static final Path CONFIG_DIR = Paths.get(System.getProperty("user.home"), ".flossware", "nexus");
private static final Path CONFIG_FILE = CONFIG_DIR.resolve("nexus.properties");
```

**Swift:**
```swift
private let configDir = FileManager.default.homeDirectoryForCurrentUser
    .appendingPathComponent(".flossware")
    .appendingPathComponent("nexus")
```

### Error Messages

**User-facing**: Simple, actionable
```java
throw new IllegalStateException("Nexus URL not configured. Set NEXUS_URL environment variable or create ~/.flossware/nexus/nexus.properties");
```

**Developer-facing**: Include context
```java
throw new IOException("Failed to fetch components from repository '" + repository + "': HTTP " + statusCode);
```

## Common Development Tasks

### Adding a New Filter to SearchCriteria

1. **Add field to SearchCriteria** (jnexus-core/SearchCriteria.java):
   ```java
   private final String newFilter;
   ```

2. **Update Builder**:
   ```java
   public Builder newFilter(String newFilter) {
       this.newFilter = newFilter;
       return this;
   }
   ```

3. **Update NexusService.searchComponents()**:
   ```java
   if (criteria.getNewFilter() != null) {
       components = components.stream()
           .filter(c -> c.someField().matches(criteria.getNewFilter()))
           .collect(Collectors.toList());
   }
   ```

4. **Add tests**:
   ```java
   @Test
   void testSearchComponents_withNewFilter() {
       // Test implementation
   }
   ```

5. **Update platform-specific UIs**:
   - Desktop: Add field to Swing/AWT/Terminal/CLI
   - Android: Add field to SearchScreen.kt
   - iOS: Add field to SearchView.swift

6. **Update documentation**: README.md, CHANGELOG.md

### Adding a New Command (Desktop CLI)

1. **Create command class** in JNexus.java:
   ```java
   @Command(name = "newcmd", description = "New command description")
   static class NewCommand implements Callable<Integer> {
       @ParentCommand
       private JNexus parent;
       
       @Option(names = {"-o", "--option"}, description = "Option description")
       private String option;
       
       @Override
       public Integer call() throws Exception {
           // Implementation
           return 0;
       }
   }
   ```

2. **Add to subcommands**:
   ```java
   @Command(subcommands = {
       ListCommand.class,
       DeleteCommand.class,
       StatsCommand.class,
       NewCommand.class  // Add here
   })
   ```

3. **Add business logic** to NexusService.java:
   ```java
   public void newCommandOperation(String param) {
       // Implementation
   }
   ```

4. **Add tests** in NexusServiceTest.java

5. **Update documentation**: README.md, CHANGELOG.md

### Adding a New UI Component (Android)

1. **Create new Composable** in ui/screens/:
   ```kotlin
   @Composable
   fun NewScreen(
       appState: AppState = LocalAppState.current
   ) {
       Scaffold(
           topBar = { /* TopAppBar */ }
       ) { padding ->
           // Content
       }
   }
   ```

2. **Add navigation** in MainActivity.kt:
   ```kotlin
   NavigationBar {
       NavigationBarItem(
           icon = { Icon(Icons.Default.NewIcon, "New") },
           label = { Text("New") },
           selected = selectedTab == 4,
           onClick = { selectedTab = 4 }
       )
   }
   ```

3. **Test on different screen sizes** (phone, tablet, landscape)

4. **Update documentation**: README.md, CHANGELOG.md

### Adding a New Feature to Credentials

1. **Add to interface** (jnexus-core/Credentials.java):
   ```java
   String getNewFeature();
   ```

2. **Implement in all platforms**:
   - Desktop: src/Credentials.java
   - Android: jnexus-android/CredentialsAndroid.java
   - iOS: jnexus-ios/Shared/Platform/CredentialsKeychain.swift

3. **Add tests** for each platform

4. **Update UI** to expose the feature (if user-configurable)

5. **Update documentation**: README.md, CHANGELOG.md

## Debugging

### Desktop Debugging

**Remote Debugging:**
```bash
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 \
  -jar target/jnexus-1.0-jar-with-dependencies.jar list my-repo
```

**Attach from IDE:** Connect to localhost:5005

**Verbose Output:**
```java
System.out.println("DEBUG: fetching page with token: " + token);
```

**HTTP Request Inspection:**
```java
System.out.println("Request Headers: " + request.headers());
System.out.println("Response Body: " + response.body());
```

### Android Debugging

**Logcat Filtering:**
```bash
adb logcat | grep JNexus
```

**Log Levels:**
```java
Log.d("JNexus", "Debug message");
Log.i("JNexus", "Info message");
Log.w("JNexus", "Warning message");
Log.e("JNexus", "Error message");
```

**HTTP Logging (OkHttp):**
```java
HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
logging.setLevel(HttpLoggingInterceptor.Level.BODY);
client = new OkHttpClient.Builder()
    .addInterceptor(logging)
    .build();
```

**Charles Proxy/Proxyman:** Configure device proxy for HTTP inspection

**Database Inspection:**
```bash
adb shell
run-as com.flossware.jnexus
cat shared_prefs/nexus_credentials.xml  # Encrypted, won't be readable
```

### iOS/macOS Debugging

**Console Output:**
```swift
print("Debug: Fetching page with token: \(token ?? "nil")")
```

**Network Debugging:**
```swift
let configuration = URLSessionConfiguration.default
configuration.httpAdditionalHeaders = ["X-Debug": "true"]
```

**Charles Proxy/Proxyman:** Configure Mac proxy for HTTP inspection

**Keychain Inspection:**
```bash
# On macOS
open /Applications/Utilities/Keychain\ Access.app
# Search for service: com.flossware.jnexus
```

**SwiftUI View Debugging:**
```swift
struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
            .environmentObject(AppState())
    }
}
```

**Instruments:** Use for performance profiling (Xcode → Product → Profile)

## Testing

See [TEST_COVERAGE.md](TEST_COVERAGE.md) for comprehensive testing documentation.

### Running Tests

**Desktop:**
```bash
./mvnw test
```

**Android Unit Tests:**
```bash
gradle :jnexus-android:testDebugUnitTest
```

**Android Instrumented Tests:**
```bash
gradle :jnexus-android:connectedDebugAndroidTest
```

**iOS/macOS:**
```bash
xcodebuild test -scheme JNexus-iOS -destination 'platform=iOS Simulator,name=iPhone 15'
xcodebuild test -scheme JNexus-macOS
```

### Test Coverage

**Desktop (JaCoCo):**
```bash
./mvnw test jacoco:report
open target/site/jacoco/index.html
```

**Android:**
```bash
gradle :jnexus-android:testDebugUnitTestCoverage
open jnexus-android/build/reports/coverage/test/debug/index.html
```

## Git Workflow

### Branching Strategy

- `main`: Stable release branch
- `develop`: Integration branch (not currently used)
- Feature branches: `feature/description`
- Bug fix branches: `bugfix/description`
- Release branches: `release/v1.2`

### Commit Messages

Follow conventional commits format:

```
type(scope): subject

body (optional)

footer (optional)
```

**Types:**
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation only
- `style`: Formatting, missing semicolons, etc.
- `refactor`: Code change that neither fixes a bug nor adds a feature
- `test`: Adding missing tests
- `chore`: Changes to build process or auxiliary tools

**Examples:**
```
feat(android): add search screen with advanced filters

Add SearchScreen.kt with collapsible filter panel for size range,
date range, file extension, and regex filters.

Closes #123
```

```
fix(desktop): correct cache expiration logic

Cache was not expiring after TTL due to incorrect timestamp comparison.
Changed to use Instant.now().isAfter(cacheTimestamp.plus(ttl)).

Fixes #456
```

### Pull Request Guidelines

1. **Create feature branch** from main
2. **Implement changes** with tests
3. **Update documentation** (README.md, CHANGELOG.md, platform docs)
4. **Run tests** locally
5. **Push to GitHub**
6. **Create PR** with description of changes
7. **Wait for CI** to pass (GitHub Actions)
8. **Address review comments** if any
9. **Squash and merge** to main

**PR Title Format:**
```
[Platform] Brief description

Examples:
[Desktop] Add statistics command with JSON output
[Android] Implement credential encryption
[iOS] Add swipe-to-delete gesture
[All] Update to Java 21 and Swift 5.9
```

## When Making Changes

### Always Update

1. **Unit tests** - Verify existing tests still pass, add new tests for new functionality
2. **Documentation** - Update relevant docs:
   - README.md for user-facing changes
   - CLAUDE.md or platform-specific docs for architectural changes
   - DEVELOPMENT_GUIDE.md for new development patterns
   - Javadoc/KDoc/Swift comments for public API changes
3. **CHANGELOG.md** - Add entry in "Unreleased" section
4. **Version numbers** - Bump version if making a release (follow semantic versioning)

### Never

- **Break backward compatibility** without major version bump
- **Add heavy frameworks** (Spring, etc.) - keep dependencies minimal
- **Commit credentials or sensitive data** - use .gitignore
- **Modify behavior without adding tests** - maintain test coverage
- **Push directly to main** - use pull requests for code review
- **Ignore CI failures** - fix failing tests before merging

### Semantic Versioning

Follow [SemVer 2.0.0](https://semver.org/):

- **MAJOR** (1.x.x): Incompatible API changes
- **MINOR** (x.1.x): Add functionality in backward-compatible manner
- **PATCH** (x.x.1): Backward-compatible bug fixes

**Examples:**
- `1.0.0` → `1.0.1`: Fix cache expiration bug
- `1.0.0` → `1.1.0`: Add statistics command
- `1.0.0` → `2.0.0`: Change NexusHttpClient interface (breaking)

## Code Review Checklist

### General
- [ ] Code follows project conventions
- [ ] No compiler warnings
- [ ] No magic numbers (use named constants)
- [ ] Error handling is appropriate
- [ ] No hardcoded credentials or sensitive data
- [ ] Documentation is updated

### Testing
- [ ] Unit tests added for new functionality
- [ ] Unit tests pass locally
- [ ] Integration tests pass (if applicable)
- [ ] Edge cases are tested
- [ ] Test coverage maintained or improved

### Documentation
- [ ] README.md updated (if user-facing)
- [ ] CHANGELOG.md updated
- [ ] Platform-specific docs updated (if architectural)
- [ ] Javadoc/KDoc/Swift comments added (if public API)
- [ ] Code comments explain "why" not "what"

### Performance
- [ ] No unnecessary object creation in loops
- [ ] Caching used appropriately
- [ ] Database/network calls minimized
- [ ] Memory leaks checked (Android/iOS)

### Security
- [ ] Input validation on user data
- [ ] No SQL injection vulnerabilities
- [ ] No XSS vulnerabilities
- [ ] Credentials stored securely
- [ ] HTTPS enforced for network calls

## Resources

### Documentation
- [Main Documentation](CLAUDE.md)
- [Desktop Guide](CLAUDE_DESKTOP.md)
- [Android Guide](CLAUDE_ANDROID.md)
- [iOS/macOS Guide](CLAUDE_IOS.md)
- [Test Coverage](TEST_COVERAGE.md)
- [CI/CD](CI-CD.md)
- [Security](SECURITY.md)
- [Contributing](CONTRIBUTING.md)

### External Resources
- [Nexus Repository REST API](https://help.sonatype.com/repomanager3/rest-and-integration-api)
- [Picocli Documentation](https://picocli.info/)
- [Jetpack Compose Docs](https://developer.android.com/jetpack/compose)
- [SwiftUI Documentation](https://developer.apple.com/documentation/swiftui)
- [OkHttp Documentation](https://square.github.io/okhttp/)
- [URLSession Documentation](https://developer.apple.com/documentation/foundation/urlsession)

### Community
- [GitHub Issues](https://github.com/FlossWare/nexus-java/issues)
- [GitHub Discussions](https://github.com/FlossWare/nexus-java/discussions)
- [Contributing Guidelines](CONTRIBUTING.md)
- [Code of Conduct](CODE_OF_CONDUCT.md)

Back to [Main Documentation](CLAUDE.md)

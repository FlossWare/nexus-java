# Contributing to JNexus CLI

Thank you for your interest in contributing to the JNexus CLI tool! This document provides guidelines and instructions for contributing.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [Project Structure](#project-structure)
- [Coding Standards](#coding-standards)
- [Testing](#testing)
- [Submitting Changes](#submitting-changes)
- [Reporting Issues](#reporting-issues)

## Code of Conduct

- Be respectful and inclusive
- Focus on constructive feedback
- Help create a welcoming environment for all contributors

## Getting Started

1. Fork the repository
2. Clone your fork: `git clone https://github.com/YOUR_USERNAME/nexus.git`
3. Create a feature branch: `git checkout -b feature/your-feature-name`
4. Make your changes
5. Submit a pull request

## Development Setup

### Prerequisites

- Java 21 or higher
- Maven 3.6+ (or use the included Maven wrapper)
- Git
- Access to a Nexus repository for testing (optional)

### Building the Project

```bash
./mvnw clean package
```

### Running Tests

```bash
./mvnw test
```

### Running Locally

```bash
java -jar target/jnexus-1.0-jar-with-dependencies.jar --help
```

## Project Structure

```
src/main/java/org/flossware/jnexus/
├── JNexus.java          # CLI entry point with Picocli commands
├── JNexusSwing.java     # Swing GUI (modern graphical interface)
├── JNexusAWT.java       # AWT GUI (classic graphical interface)
├── JNexusUI.java        # Terminal UI (ncurses-based interface)
├── NexusClient.java     # HTTP client for Nexus API
├── NexusService.java    # Business logic layer
├── Credentials.java     # Configuration management
└── RepoRecord.java      # Data model (Java record)

src/test/java/org/flossware/jnexus/
├── NexusServiceTest.java           # Service layer tests
├── NexusServiceAdvancedTest.java   # Advanced service scenarios
├── NexusClientTest.java            # Basic client tests
├── NexusClientCacheTest.java       # Cache functionality tests
├── NexusClientIntegrationTest.java # Integration tests with mock server
└── CredentialsTest.java            # Credentials loading tests

src/main/resources/
└── logback.xml                     # Logging configuration

src/test/resources/
└── logback-test.xml                # Test logging configuration

Launcher Scripts:
├── jnexus.sh           # CLI launcher
├── jnexus-swing.sh     # Swing GUI launcher
├── jnexus-awt.sh       # AWT GUI launcher
└── jnexus-ui.sh        # Terminal UI launcher
```

## Coding Standards

### Java Code Style

- Use 4 spaces for indentation (no tabs)
- Maximum line length: 120 characters
- Follow standard Java naming conventions:
  - Classes: `PascalCase`
  - Methods: `camelCase`
  - Constants: `UPPER_SNAKE_CASE`
  - Variables: `camelCase`

### Code Quality

- Write clean, readable code
- Keep methods focused and concise (Single Responsibility Principle)
- Avoid deep nesting (max 3 levels)
- Use meaningful variable and method names
- Don't add comments for obvious code; use comments for "why" not "what"

### Javadoc

All public classes and methods must have Javadoc:

```java
/**
 * Brief description of what this does.
 * <p>
 * More detailed explanation if needed.
 * </p>
 *
 * @param paramName description of parameter
 * @return description of return value
 * @throws ExceptionType when this exception is thrown
 */
public ReturnType methodName(ParamType paramName) {
    // implementation
}
```

## Testing

### Test Coverage

- All new features must include unit tests
- Aim for >80% code coverage for core logic (Service/Client layers)
- Test both success and failure scenarios
- Test edge cases and boundary conditions

**Exception:** GUI code (Swing, AWT, Terminal UI) is tested manually
- GUI testing requires specialized frameworks (AssertJ Swing, TestFX)
- Core business logic is fully tested in Service/Client layers
- GUIs are thin wrappers around tested business logic
- Manual testing is standard practice for simple GUIs

### Test Structure

Follow the Arrange-Act-Assert pattern:

```java
@Test
void testMethodName() {
    // Arrange - setup test data and mocks
    SomeClass obj = new SomeClass();
    
    // Act - execute the method under test
    Result result = obj.doSomething();
    
    // Assert - verify the results
    assertEquals(expected, result);
}
```

### Integration Tests

- Use `@TempDir` for file system tests
- Use mock HTTP servers for HTTP integration tests (see `NexusClientIntegrationTest.java`)
- Clean up resources in `@AfterEach` methods
- Use `ByteArrayOutputStream` to capture System.out for output verification

### GUI Testing (Manual)

Since GUI testing requires specialized frameworks and headless environments,
GUIs are tested manually:

1. **Before submitting GUI changes:**
   - Build: `./mvnw clean package`
   - Test Swing GUI: `./jnexus-swing.sh`
   - Test AWT GUI: `./jnexus-awt.sh`
   - Test Terminal UI: `./jnexus-ui.sh`
   - Verify all buttons and fields work correctly
   - Test with valid and invalid inputs
   - Verify confirmation dialogs appear

2. **Test checklist for GUIs:**
   - [ ] Application launches without errors
   - [ ] All fields pre-populate with defaults (if configured)
   - [ ] List operation retrieves and displays data
   - [ ] Refresh operation bypasses cache
   - [ ] Delete operation shows confirmation dialog
   - [ ] Dry-run checkbox works correctly
   - [ ] Error messages display clearly
   - [ ] Status bar updates appropriately
   - [ ] Application exits cleanly

## Submitting Changes

### Pull Request Process

1. **Update Tests**: Ensure all tests pass and add new tests for your changes
2. **Update Documentation**: Update README.md, Javadoc, and relevant docs
3. **Run Full Build**: `./mvnw clean package` must succeed
4. **Commit Messages**: Write clear, descriptive commit messages
5. **Pull Request Description**: Explain what changes you made and why

### Commit Message Format

```
[Type] Brief description (50 chars or less)

More detailed explanation if needed (wrap at 72 characters).
Explain the problem this commit solves and why you chose this approach.

Fixes #issue-number
```

Types:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `test`: Adding or updating tests
- `refactor`: Code refactoring
- `chore`: Maintenance tasks

### Pull Request Checklist

- [ ] Code compiles without errors or warnings
- [ ] All tests pass (`./mvnw test`)
- [ ] New code has adequate test coverage
- [ ] Javadoc is complete for public APIs
- [ ] README.md is updated if needed
- [ ] No unrelated changes are included
- [ ] Commit messages are clear and descriptive

## Reporting Issues

JNexus uses structured GitHub Issue Templates to help organize and categorize issues effectively. When reporting issues, use the appropriate template for your situation.

### Bug Reports

Use the **Bug Report** template when you encounter a problem or unexpected behavior.

The template will guide you to provide:

- Target interface (Android, iOS, macOS, Swing, AWT, Terminal UI, or CLI)
- JNexus version in X.Y.Z format
- Expected vs. actual behavior
- Clear reproduction steps
- Dry-run verification (for deletion issues)
- Verbose logs or error traces
- Environment details (OS, Java version, device info)

**For Security Issues:** Do NOT use the bug report template for security vulnerabilities. Instead:
1. Email **sfloess@redhat.com** with details
2. Or use [GitHub Security Advisories](https://github.com/FlossWare/nexus-java/security/advisories/new)
3. Never report security issues in public GitHub issues

### Feature Requests

Use the **Feature Request** template when proposing new functionality or improvements.

The template will guide you to provide:

- Problem statement: What limitation does this solve?
- Proposed solution: How should the feature work?
- Target interfaces: Which platforms should this be implemented in?
- Alternative solutions: What else have you considered?
- Use cases and examples

### Documentation Updates

Use the **Documentation Update** template when you find:

- Outdated information
- Missing steps or unclear explanations
- Typos or grammatical errors
- Misleading examples

The template includes a checklist of all documentation files for easy reference.

## Development Tips

### Running Against a Real Nexus Instance

Create a test configuration:

```bash
mkdir -p ~/.flossware/nexus
cat > ~/.flossware/nexus/nexus.properties <<EOF
nexus.url=https://your-test-nexus.com
nexus.user=your-username
nexus.password=your-password
EOF
```

### Testing with Mock Server

See `NexusClientIntegrationTest.java` for examples of using Java's built-in
`HttpServer` for integration testing.

### Debugging

Run with remote debugging enabled:

```bash
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 \
  -jar target/jnexus-1.0-jar-with-dependencies.jar list my-repo
```

Then attach your IDE debugger to port 5005.

## Versioning and Releases

This project uses **X.Y** versioning (e.g., 1.0, 1.1, 2.0):
- **X (Major)**: Incompatible API changes or major feature releases  
- **Y (Minor)**: New features, bug fixes, improvements (backwards compatible)

### Version Enforcement

The Maven enforcer plugin validates that versions follow the X.Y format:
- ✅ Valid: `1.0`, `1.1`, `2.0`
- ❌ Invalid: `1.0.0`, `1.0-SNAPSHOT`, `1.0.0-RELEASE`

### Creating a Release

**Releases are now fully automated!** Push a version tag and all platforms build, test, and release automatically.

**Unified Release Process (Recommended):**

```bash
# 1. Update versions in all files
# 2. Update CHANGELOG.md with release notes
# 3. Commit changes
git add pom.xml jnexus-core/build.gradle jnexus-android/build.gradle \
        jnexus-ios/iOS/Info.plist jnexus-ios/macOS/Info.plist CHANGELOG.md
git commit -m "chore: bump version to 2.1.0"
git push origin main

# 4. Tag and release (triggers unified workflow for all platforms)
git tag v2.1.0
git push origin v2.1.0
# Done! Workflow builds Desktop JAR, Android APK, iOS IPA, and macOS DMG
```

**Important:** Make sure to update the version in **all** of these files:
- `pom.xml` - Desktop Maven version
- `jnexus-core/build.gradle` - Shared library version
- `jnexus-android/build.gradle` - Android app version
- `jnexus-ios/iOS/Info.plist` - iOS app version
- `jnexus-ios/macOS/Info.plist` - macOS app version
- `CHANGELOG.md` - Release notes (must be formatted correctly)

For detailed release instructions, troubleshooting, and rollback procedures, see [RELEASE_PROCESS.md](RELEASE_PROCESS.md).

### Automated Release Workflow

The unified release workflow (`.github/workflows/release.yml`) automatically:

1. **Validates** version consistency across all platform files
2. **Builds** all 4 platform artifacts in parallel:
   - Desktop JAR (Java 21, Maven, ~5 min)
   - Android APK (Gradle, Kotlin, ~8 min)
   - iOS IPA (Xcode, Swift, ~10 min, optional signing)
   - macOS DMG (Xcode, Swift, ~8 min, optional signing)
3. **Creates** a GitHub Release with all artifacts and release notes
4. **Deploys** Desktop JAR to PackageCloud.io
5. **Posts** a release summary with status of all platforms

Total build time: **10-15 minutes** (iOS/macOS builds are slowest)

**Monitor the workflow:**
1. Go to your repository → **Actions** tab
2. Click the **Unified Release** workflow run
3. View progress and build logs for each platform
4. Check the final GitHub Release page for artifacts

**The unified release workflow:**
- ✅ Validates version consistency across all files
- ✅ Builds all 4 platforms in parallel (~10-15 minutes)
- ✅ Creates GitHub Release with all artifacts
- ✅ Deploys Desktop JAR to PackageCloud.io
- ✅ Extracts changelog from CHANGELOG.md

**For detailed instructions, see [RELEASE_PROCESS.md](RELEASE_PROCESS.md)**

### Alternative: Manual Release (Desktop Only)

For desktop-only releases without Android/iOS:

```bash
./ci/rev-version.sh
```

This will:
1. Read current version from `pom.xml`
2. Increment minor version (1.0 → 1.1)
3. Update `pom.xml` with new version
4. Commit changes with `[ci skip]` tag
5. Create git tag `v1.1`
6. Push changes and tag to remote

**Note:** Major version bumps (e.g., 1.9 → 2.0) require manual editing of
`pom.xml` before running the script.

### CI/CD Platform Support

- **GitHub Actions** (Primary): Unified release workflow + Desktop CI
- **GitLab CI** (Secondary): Desktop CI only

For detailed CI/CD documentation, see [CI-CD.md](CI-CD.md).

## Questions?

If you have questions about contributing, please open an issue with the
`question` label, and we'll be happy to help!

Thank you for contributing to JNexus CLI!

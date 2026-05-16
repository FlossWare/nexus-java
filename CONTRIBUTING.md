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
├── NexusClient.java    # HTTP client for Nexus API
├── NexusService.java   # Business logic layer
├── Credentials.java    # Configuration management
└── RepoRecord.java     # Data model

src/test/java/org/flossware/jnexus/
├── NexusServiceTest.java           # Service layer tests
├── NexusServiceAdvancedTest.java   # Advanced service scenarios
├── NexusClientTest.java            # Basic client tests
├── NexusClientIntegrationTest.java # Integration tests with mock server
└── CredentialsTest.java            # Credentials loading tests
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
- Aim for >80% code coverage
- Test both success and failure scenarios
- Test edge cases and boundary conditions

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
- Use mock HTTP servers for HTTP integration tests
- Clean up resources in `@AfterEach` methods

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

### Bug Reports

When reporting bugs, please include:

- JNexus CLI version: `java -jar jnexus-1.0-jar-with-dependencies.jar --version`
- Java version: `java -version`
- Operating system
- Steps to reproduce the issue
- Expected behavior
- Actual behavior
- Error messages or stack traces

### Feature Requests

When requesting features, please include:

- Clear description of the feature
- Use case / why it's needed
- Example of how it would work
- Any alternative solutions you've considered

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

Only maintainers can create releases. There are three options:

**1. Automated (GitHub Actions - Recommended)**
Push to `main` branch and the CI/CD pipeline automatically:
- Increments version
- Builds and tests
- Deploys to PackageCloud
- Creates git tag

**2. Automated (GitLab CI)**
Push to `main`, then manually trigger `release` job in pipeline.

**3. Manual Script**
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

For detailed CI/CD documentation, see [CI-CD.md](CI-CD.md).

## Questions?

If you have questions about contributing, please open an issue with the
`question` label, and we'll be happy to help!

Thank you for contributing to JNexus CLI!

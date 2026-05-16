# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0] - 2026-05-15

### Added
- Initial release of Nexus CLI tool
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

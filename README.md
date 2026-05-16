# Nexus CLI Tool

A Java 21 command-line tool for interacting with Sonatype Nexus repositories. Supports listing and deleting components with optional regex filtering.

## Features

- List components in a repository with optional filtering
- Delete components with safety features (confirmation prompts, dry-run mode)
- Regex-based filtering for targeted operations
- **Intelligent caching** with 5-minute TTL (reduces server load, faster responses)
- Interactive terminal UI with ncurses (jcurses)
- Command-line interface for scripting and automation
- Fast startup and minimal dependencies
- Support for environment variables or configuration file

## Requirements

- Java 21 or higher
- Maven (or use included Maven wrapper `./mvnw`)
- Access to a Nexus repository with valid credentials

## Installation

### Build from source

```bash
./mvnw clean package
```

This creates an executable JAR at `target/nexus-1.0-jar-with-dependencies.jar`.

## Configuration

Configure your Nexus credentials using either environment variables or a properties file.

### Option 1: Environment Variables

```bash
export NEXUS_URL=https://your-nexus-server.com
export NEXUS_USER=your-username
export NEXUS_PASSWORD=your-password-or-token
```

### Option 2: Properties File

1. Create the configuration directory:
   ```bash
   mkdir -p ~/.flossware/nexus
   ```

2. Copy the example properties file:
   ```bash
   cp src/main/resources/nexus.properties.example ~/.flossware/nexus/nexus.properties
   ```

3. Edit `~/.flossware/nexus/nexus.properties` with your credentials:
   ```properties
   nexus.url=https://your-nexus-server.com
   nexus.user=your-username
   nexus.password=your-password-or-token
   ```

## Usage

The Nexus CLI provides two interfaces: a command-line interface (CLI) and an interactive terminal UI.

### Terminal UI (Interactive)

The terminal UI provides a full-screen ncurses interface for interacting with Nexus:

```bash
./nexus-ui.sh
```

**Requirements:**
- ncurses library installed:
  - Ubuntu/Debian: `sudo apt-get install libncurses-dev`
  - Fedora/RHEL: `sudo dnf install ncurses-devel`
  - Arch: `sudo pacman -S ncurses`

**Controls:**
- `TAB` / Arrow keys: Navigate between fields
- `SPACE` / `ENTER`: Activate buttons or toggle checkboxes
- Type directly in text fields
- `Q` / `ESC`: Quit

**Features:**
- Repository name and regex filter inputs
- Dry-run checkbox for safe preview of deletions
- List and Delete buttons for operations
- Live results display
- Status messages

### Command-Line Interface (CLI)

For scripting and automation, use the traditional CLI:

```bash
./nexus.sh <command> [options]
```

Or execute the JAR directly:

```bash
java -jar target/nexus-1.0-jar-with-dependencies.jar <command> [options]
```

### Commands

#### List components

List all components in a repository:
```bash
./nexus.sh list my-repository
```

List components matching a regex pattern:
```bash
./nexus.sh list my-repository ".*SNAPSHOT.*"
```

#### Delete components

**WARNING: Delete operations are permanent. Always use `--dry-run` first!**

Preview what would be deleted (dry-run):
```bash
./nexus.sh delete --dry-run my-repository
```

Delete all components in a repository (with confirmation):
```bash
./nexus.sh delete my-repository
```

Delete components matching a regex pattern:
```bash
./nexus.sh delete my-repository ".*-1\.0\.0-SNAPSHOT.*"
```

Skip confirmation prompt:
```bash
./nexus.sh delete --yes my-repository ".*SNAPSHOT.*"
```

### Options

- `--help`, `-h` - Show help message
- `--version`, `-V` - Show version information
- `--dry-run`, `-n` - (Delete only) Show what would be deleted without deleting
- `--yes`, `-y` - (Delete only) Skip confirmation prompt

### Examples

```bash
# List all snapshots in the releases repository
./nexus.sh list releases ".*SNAPSHOT.*"

# Dry-run: see what would be deleted
./nexus.sh delete --dry-run snapshots ".*-2023.*"

# Delete old snapshot versions (with confirmation)
./nexus.sh delete snapshots ".*-1\.0\..*-SNAPSHOT.*"

# Delete all components in a repository (skip confirmation)
./nexus.sh delete --yes old-repository
```

## Development

### Run tests

```bash
./mvnw test
```

### Build without tests

```bash
./mvnw package -DskipTests
```

### Project Structure

```
src/main/java/org/flossware/nexus/
├── Nexus.java          # Main CLI entry point with Picocli commands
├── NexusClient.java    # HTTP client for Nexus API
├── NexusService.java   # Business logic for list/delete operations
├── Credentials.java    # Configuration management
└── RepoRecord.java     # Data model for repository components

src/test/java/org/flossware/nexus/
├── NexusServiceTest.java  # Unit tests for business logic
└── NexusClientTest.java   # Tests for client and data models
```

## Safety Features

1. **Confirmation prompts** - Delete operations require explicit confirmation (unless `--yes` flag is used)
2. **Dry-run mode** - Preview deletions before executing with `--dry-run`
3. **Regex filtering** - Target specific components instead of entire repositories
4. **Clear error messages** - Helpful feedback when operations fail

## Performance

- **Startup time**: < 1 second (vs 3-5 seconds with Spring Boot)
- **JAR size**: ~5-10 MB (vs ~50 MB with Spring Boot)
- **Memory**: Minimal footprint, no embedded web server

## Versioning

This project uses **X.Y** versioning (e.g., 1.0, 1.1, 2.0):
- **X (Major)**: Incompatible API changes or major feature releases
- **Y (Minor)**: New features, bug fixes, or improvements (backwards compatible)

See [CHANGELOG.md](CHANGELOG.md) for release history.

### Releasing a New Version

**Manual release:**
```bash
# Bump version, tag, and push
./ci/rev-version.sh
```

**Automated release (CI/CD):**
- **GitHub Actions**: Automatically bumps version, builds, tests, and deploys on push to `main`
- **GitLab CI**: Manual pipeline jobs for deployment and release

See [CI-CD.md](CI-CD.md) for complete CI/CD documentation.

### Version Bump Script

The `./ci/rev-version.sh` script will:
1. Increment the minor version (e.g., 1.0 → 1.1)
2. Update `pom.xml`
3. Commit the version change
4. Create a git tag (e.g., `v1.1`)
5. Push changes and tag to remote

## License

Licensed under the Apache License 2.0. See LICENSE file for details.

## Contributing

Contributions are welcome! Please submit pull requests or open issues on GitHub.

See [CONTRIBUTING.md](CONTRIBUTING.md) for development guidelines.

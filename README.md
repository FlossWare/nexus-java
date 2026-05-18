# JNexus CLI Tool

A Java 21 command-line tool for interacting with Sonatype Nexus repositories. Supports listing and deleting components with optional regex filtering.

## Features

- List components in a repository with optional filtering
- Delete components with safety features (confirmation prompts, dry-run mode)
- Regex-based filtering for targeted operations
- **Intelligent caching** with 5-minute TTL (reduces server load, faster responses)
- **Automatic retry logic** with exponential backoff for transient network failures
- **Configurable HTTP timeouts** via environment variables or properties file
- **Verbose and quiet modes** for detailed logging or minimal output
- **Multiple user interfaces:**
  - **Swing GUI** - Modern graphical interface with native look and feel
  - **AWT GUI** - Classic graphical interface for maximum compatibility
  - **Terminal UI** - Full-screen ncurses interface for terminal users
  - **Command-line interface** - For scripting and automation
- Fast startup and minimal dependencies
- Support for environment variables or configuration file

## Known Limitations

- **Multi-asset components**: For components with multiple assets (e.g., JAR + POM + sources), only the first asset's path and size are displayed. This keeps output simple and consistent. Statistics may underreport total repository size for multi-asset components.

## Requirements

- Java 21 or higher
- Maven (or use included Maven wrapper `./mvnw`)
- Access to a Nexus repository with valid credentials

## Installation

### Build from source

```bash
./mvnw clean package
```

This creates an executable JAR at `target/jnexus-1.0-jar-with-dependencies.jar`.

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
   cp src/main/resources/jnexus.properties.example ~/.flossware/nexus/nexus.properties
   ```

3. Edit `~/.flossware/nexus/nexus.properties` with your credentials:
   ```properties
   nexus.url=https://your-nexus-server.com
   nexus.user=your-username
   nexus.password=your-password-or-token
   
   # Optional: UI default values (uncomment to use)
   #nexus.default.repository=maven-releases
   #nexus.default.regex=.*SNAPSHOT.*
   #nexus.default.dryrun=true
   
   # Optional: HTTP timeout configuration (default: 30 seconds)
   #nexus.http.timeout.seconds=60
   ```
   
   The optional defaults will pre-populate the terminal UI fields on startup.

### Environment Variables

You can also configure settings via environment variables:

```bash
export NEXUS_URL=https://your-nexus-server.com
export NEXUS_USER=your-username
export NEXUS_PASSWORD=your-password-or-token
export NEXUS_HTTP_TIMEOUT=60  # Optional: HTTP timeout in seconds
```

Environment variables take precedence over properties file values for authentication credentials.

## Usage

JNexus provides **four different interfaces** to suit your preference:

1. **Swing GUI** - Modern graphical interface (recommended for desktop users)
2. **AWT GUI** - Classic graphical interface (maximum compatibility)
3. **Terminal UI** - Full-screen ncurses interface (for terminal users)
4. **Command-line Interface** - For scripting and automation

### Swing GUI (Recommended)

The Swing interface provides a modern, native-looking graphical interface:

```bash
./jnexus-swing.sh
```

**Features:**
- Modern look and feel that matches your operating system
- Responsive design with background task execution
- Repository name and regex filter inputs
- Dry-run checkbox for safe preview of deletions
- List (cached) and Refresh (bypass cache) buttons
- Delete button with confirmation dialog
- Scrollable results area
- Status bar with operation feedback

**Screenshot:**
```
┌─────────────────────────────────────────────────────────┐
│ Nexus Repository Manager - Swing UI                    │
├─────────────────────────────────────────────────────────┤
│ Repository Configuration                                │
│   Repository:  [maven-releases                        ] │
│   Regex Filter:[.*SNAPSHOT.*                          ] │
│   ☑ Dry Run (preview only, no actual deletion)         │
│   [List] [Refresh] [Delete] [Clear Results] [Quit]     │
├─────────────────────────────────────────────────────────┤
│ Results                                                 │
│ ┌─────────────────────────────────────────────────────┐ │
│ │ id1              1,024  artifact-1.0.0.jar          │ │
│ │ id2              2,048  artifact-2.0.0.jar          │ │
│ │                                                     │ │
│ └─────────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────┤
│ List completed - Cached (125s old)                      │
└─────────────────────────────────────────────────────────┘
```

### AWT GUI (Classic)

The AWT interface provides a classic graphical interface using only AWT components:

```bash
./jnexus-awt.sh
```

**Features:**
- Classic AWT look and feel
- Works on systems where Swing might have issues
- Same functionality as Swing interface
- Lightweight and fast

**Why use AWT instead of Swing?**
- Maximum compatibility with older Java installations
- Lower memory footprint
- Works well in remote desktop/VNC scenarios
- Familiar to users of classic Java applications

### Terminal UI (Interactive)

The terminal UI provides a full-screen ncurses interface for terminal users:

```bash
./jnexus-ui.sh
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
- List, Refresh, and Delete buttons
- Live results display
- Status messages

**Why use Terminal UI?**
- Works over SSH without X11 forwarding
- Lightweight and fast
- Familiar to terminal users
- No graphics dependencies beyond ncurses

### Command-Line Interface (CLI)

For scripting and automation, use the traditional CLI:

```bash
./jnexus.sh <command> [options]
```

Or execute the JAR directly:

```bash
java -jar target/jnexus-1.0-jar-with-dependencies.jar <command> [options]
```

### Commands

#### List components

List all components in a repository:
```bash
./jnexus.sh list my-repository
```

List components matching a regex pattern:
```bash
./jnexus.sh list my-repository ".*SNAPSHOT.*"
```

#### Delete components

**WARNING: Delete operations are permanent. Always use `--dry-run` first!**

Preview what would be deleted (dry-run):
```bash
./jnexus.sh delete --dry-run my-repository
```

Delete all components in a repository (with confirmation):
```bash
./jnexus.sh delete my-repository
```

Delete components matching a regex pattern:
```bash
./jnexus.sh delete my-repository ".*-1\.0\.0-SNAPSHOT.*"
```

Skip confirmation prompt:
```bash
./jnexus.sh delete --yes my-repository ".*SNAPSHOT.*"
```

### Global Options

The following options can be used with any command:

**Verbose mode** - Enable debug logging:
```bash
./jnexus.sh --verbose list my-repository
./jnexus.sh -v delete my-repository
```

**Quiet mode** - Only show warnings and errors:
```bash
./jnexus.sh --quiet list my-repository
./jnexus.sh -q delete my-repository
```

### Options

- `--help`, `-h` - Show help message
- `--version`, `-V` - Show version information
- `--dry-run`, `-n` - (Delete only) Show what would be deleted without deleting
- `--yes`, `-y` - (Delete only) Skip confirmation prompt

### Examples

```bash
# List all snapshots in the releases repository
./jnexus.sh list releases ".*SNAPSHOT.*"

# Dry-run: see what would be deleted
./jnexus.sh delete --dry-run snapshots ".*-2023.*"

# Delete old snapshot versions (with confirmation)
./jnexus.sh delete snapshots ".*-1\.0\..*-SNAPSHOT.*"

# Delete all components in a repository (skip confirmation)
./jnexus.sh delete --yes old-repository
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
src/main/java/org/flossware/jnexus/
├── JNexus.java          # Main CLI entry point with Picocli commands
├── NexusClient.java    # HTTP client for Nexus API
├── NexusService.java   # Business logic for list/delete operations
├── Credentials.java    # Configuration management
└── RepoRecord.java     # Data model for repository components

src/test/java/org/flossware/jnexus/
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

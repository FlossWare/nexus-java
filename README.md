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
   
   # Optional: Repository list (comma-separated, for dropdowns/batch operations)
   #nexus.repositories=maven-releases,maven-snapshots,npm-public,docker-hosted
   
   # Optional: HTTP timeout configuration (default: 30 seconds)
   #nexus.http.timeout.seconds=60
   ```
   
   The optional defaults will pre-populate the UI fields on startup.
   The repository list can be used for dropdown menus in GUIs or batch operations.

### Option 3: Multiple Profiles

For managing multiple environments (dev, staging, prod), you can create profile-specific property files:

1. Create profile-specific configuration files:
   ```bash
   cp src/main/resources/nexus-dev.properties.example ~/.flossware/nexus/nexus-dev.properties
   cp src/main/resources/nexus-prod.properties.example ~/.flossware/nexus/nexus-prod.properties
   ```

2. Edit each file with the appropriate credentials for that environment.

3. Use profiles via environment variable:
   ```bash
   export NEXUS_PROFILE=dev
   ./jnexus.sh list my-repository
   ```

4. Or via CLI flag:
   ```bash
   ./jnexus.sh --profile prod list my-repository
   ```

**Profile naming convention:**
- Default: `nexus.properties` (no profile specified)
- Dev: `nexus-dev.properties` (NEXUS_PROFILE=dev or --profile dev)
- Production: `nexus-prod.properties` (NEXUS_PROFILE=prod or --profile prod)
- Staging: `nexus-staging.properties` (NEXUS_PROFILE=staging or --profile staging)

This is similar to Spring Boot profiles and allows you to easily switch between environments without editing configuration files.

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
- **Table-based display** with sortable columns (click header to sort)
- **4 columns**: ID, File Size (Bytes), File Size (MB), Path
- **Multi-row selection** with CTRL/SHIFT click
- **Smart Delete Selected button** - appears only when rows are selected
- **Selection status display** - shows total size of selected components
- **Summary row** - shows total components and bytes (non-editable, highlighted)
- **Interactive credential collection** - if no configuration files exist, shows a dialog to enter credentials
- **Save credentials option** - after entering credentials, optionally save them for future use
- **Automatic profile selection** - if multiple configuration files exist, shows a dialog to choose which one to use
- **Repository dropdown selector** - choose from "All" or configured repositories (no text field needed)
- **Nexus URL display** - see which Nexus server you're connected to
- **Config file display** - see which configuration file is being used
- Regex filter input with **Enter key shortcut**
- Dry-run checkbox for safe preview of deletions
- List (cached) and Refresh (bypass cache) buttons
- Delete All button with confirmation dialog
- **Busy cursor** during operations with disabled buttons
- Status bar with operation feedback and selection summary

**Screenshot:**
```
┌─────────────────────────────────────────────────────────────────────────────┐
│ Nexus Repository Manager - Swing UI                                        │
├─────────────────────────────────────────────────────────────────────────────┤
│ Repository Configuration                                                    │
│   Repository:  [maven-releases ▼] (All, maven-releases, npm-public)        │
│   Regex Filter: [.*SNAPSHOT.*                   ] (Enter to List)          │
│   ☑ Dry Run (preview only, no actual deletion)                             │
│   Nexus URL:    https://nexus.corp.redhat.com                              │
│   Config File:  ~/.flossware/nexus/nexus.properties                        │
│   [List] [Refresh] [Delete All] [Delete Selected*] [Clear] [Quit]          │
│                                   *appears when rows selected               │
├─────────────────────────────────────────────────────────────────────────────┤
│ Results                                     (Click column headers to sort)  │
│ ┌─────────────────────────────────────────────────────────────────────────┐ │
│ │ ID ▲            File Size (Bytes) ▼  File Size (MB)  Path              │ │
│ │───────────────────────────────────────────────────────────────────────  │ │
│ │ com.example.app      1,024,567          0.98         path/to/app.jar   │ │
│ │ com.example.lib      2,048,123          1.95         path/to/lib.jar ✓ │ │
│ │ com.example.plugin     512,890          0.49         path/to/plugin.jar│ │
│ │ TOTAL: 3 components  3,585,580          3.42                           │ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────────────────────┤
│ Selected: 1 component(s) - 2,048,123 bytes (1.95 MB)                       │
└─────────────────────────────────────────────────────────────────────────────┘

Features demonstrated:
• Repository dropdown with "All" option (no text field needed)
• 4 columns: ID, File Size (Bytes), File Size (MB), Path
• Sortable columns (click headers to sort)
• Multi-select rows with CTRL/SHIFT (✓ indicates selection)
• Selection status shows total size of selected components
• Delete Selected button appears only when rows are selected
• Summary row with totals (light blue background, non-editable)
• Nexus URL and Config File displayed for reference
• Enter key in Regex Filter triggers List operation
• Busy cursor shows during operations
```

### AWT GUI (Classic)

The AWT interface provides a classic graphical interface using only AWT components:

```bash
./jnexus-awt.sh
```

**Features:**
- Classic AWT look and feel
- Works on systems where Swing might have issues
- **Formatted text output** with column headers and summary footer
- **Interactive credential collection** - if no configuration files exist, shows a dialog to enter credentials
- **Save credentials option** - after entering credentials, optionally save them to a properties file for future use
- **Automatic profile selection** - if multiple configuration files exist, shows a dialog to choose which one to use
- **Repository dropdown selector** - choose from "All" or configured repositories
- **Nexus URL display** - see which Nexus server you're connected to
- **Config file display** - see which configuration file is being used
- Regex filter input with **Enter key shortcut**
- Dry-run checkbox for safe preview of deletions
- **Busy cursor** during operations with disabled buttons
- Same core functionality as Swing interface
- Lightweight and fast

**Screenshot:**
```
┌─────────────────────────────────────────────────────────────────────────────┐
│ Nexus Repository Manager - AWT UI                                          │
├─────────────────────────────────────────────────────────────────────────────┤
│ Repository Configuration                                                    │
│   Repository:  [maven-releases ▼] (All, maven-releases, npm-public)        │
│   Regex Filter: [.*SNAPSHOT.*                   ] (Enter to List)          │
│   ☑ Dry Run (preview only, no actual deletion)                             │
│   Nexus URL:    https://nexus.corp.redhat.com                              │
│   Config File:  ~/.flossware/nexus/nexus.properties                        │
│   [List] [Refresh] [Delete] [Clear] [Quit]                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│ Results                                                                     │
│ ┌─────────────────────────────────────────────────────────────────────────┐ │
│ │ ID                                File Size        Path                │ │
│ │ ═══════════════════════════════   ═══════════════  ══════════════════  │ │
│ │ com.example.app-1.0                   1,024,567    path/to/app-1.0.jar │ │
│ │ com.example.lib-2.0                   2,048,123    path/to/lib-2.0.jar │ │
│ │ com.example.plugin-3.0                  512,890    path/to/plugin.jar  │ │
│ │ ═══════════════════════════════   ═══════════════  ══════════════════  │ │
│ │ TOTAL: 3 components                   3,585,580    (3.42 MB)           │ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────────────────────┤
│ List completed - 3 components - Cached (25s old)                            │
└─────────────────────────────────────────────────────────────────────────────┘
```

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
- **Interactive credential collection** - if no configuration files exist, prompts for credentials via console input (before starting ncurses)
- **Save credentials option** - after entering credentials, optionally save them to a properties file for future use
- **Automatic profile selection** - if multiple configuration files exist, shows a text menu to choose which one to use (before starting ncurses)
- **Repository selection** - shows configured repositories from `nexus.repositories` property (if configured)
- Repository name and regex filter inputs
- Dry-run checkbox for safe preview of deletions
- List, Refresh, and Delete buttons
- Live results display with formatted output
- Status messages

**Screenshot:**
```
┌────────────────────────────────────────────────────────────────────────────┐
│ Nexus Repository Manager - Terminal UI                                    │
│                                                                            │
│ Repository Configuration                                                   │
│   Repository:     [maven-releases________________]                        │
│   Regex Filter:   [.*SNAPSHOT.*_________________]                         │
│   [X] Dry Run (preview only, no actual deletion)                          │
│                                                                            │
│   Available Repos: maven-releases, maven-snapshots, npm-public            │
│                                                                            │
│   [ List ] [ Refresh ] [ Delete ] [ Clear ] [ Quit ]                      │
│                                                                            │
├────────────────────────────────────────────────────────────────────────────┤
│ Results                                                                    │
│                                                                            │
│ ID                                File Size        Path                   │
│ ═══════════════════════════════   ═══════════════  ═══════════════════    │
│ com.example.app-1.0                   1,024,567    path/to/app-1.0.jar    │
│ com.example.lib-2.0                   2,048,123    path/to/lib-2.0.jar    │
│ com.example.plugin-3.0                  512,890    path/to/plugin.jar     │
│                                                                            │
│                                                                            │
├────────────────────────────────────────────────────────────────────────────┤
│ List completed - 3 components - Cached (25s old)                           │
└────────────────────────────────────────────────────────────────────────────┘
```

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

**Profile mode** - Use a specific configuration profile:
```bash
./jnexus.sh --profile dev list my-repository
./jnexus.sh -p prod delete my-repository
```

### Options

- `--help`, `-h` - Show help message
- `--version`, `-V` - Show version information
- `--verbose`, `-v` - Enable debug logging
- `--quiet`, `-q` - Only show warnings and errors
- `--profile`, `-p` - Use a specific configuration profile (e.g., dev, prod, staging)
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

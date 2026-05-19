# Running JNexus

This guide explains how to run the different interfaces of the JNexus tool.

JNexus provides **four different user interfaces**:
1. **Swing GUI** - Modern graphical interface (recommended for desktop users)
2. **AWT GUI** - Classic graphical interface (maximum compatibility)
3. **Terminal UI** - Full-screen ncurses interface (for SSH/terminal users)
4. **Command-Line Interface** - For scripting and automation

## Prerequisites

### 1. Build the Project

```bash
./mvnw clean package
```

This creates `target/jnexus-1.0-jar-with-dependencies.jar`.

### 2. Configure Credentials

Choose one of these methods:

**Environment Variables:**
```bash
export NEXUS_URL=https://your-nexus-server.com
export NEXUS_USER=your-username
export NEXUS_PASSWORD=your-password
```

**Properties File (Default):**
```bash
mkdir -p ~/.flossware/nexus
cp src/main/resources/jnexus.properties.example ~/.flossware/nexus/nexus.properties
# Edit the file with your credentials
nano ~/.flossware/nexus/nexus.properties
```

The properties file also supports optional UI defaults:
```properties
# Required credentials
nexus.url=https://your-nexus-server.com
nexus.user=your-username
nexus.password=your-password

# Optional repository list (comma-separated)
nexus.repositories=maven-releases,maven-snapshots,npm-public

# Optional UI defaults (uncomment to use)
nexus.default.repository=maven-releases  # Auto-populated if nexus.repositories is set
nexus.default.regex=.*SNAPSHOT.*
nexus.default.dryrun=true
```

**Note:** When you enter credentials through the interactive dialog and provide a repository list, the first repository automatically becomes the default. This pre-populates the Repository field on next launch.

**Profile-Based Configuration (Multiple Environments):**

For managing multiple environments (dev, staging, prod), create profile-specific property files:

```bash
# Create profile-specific files
mkdir -p ~/.flossware/nexus
cp src/main/resources/nexus-dev.properties.example ~/.flossware/nexus/nexus-dev.properties
cp src/main/resources/nexus-prod.properties.example ~/.flossware/nexus/nexus-prod.properties

# Edit each file with environment-specific credentials
nano ~/.flossware/nexus/nexus-dev.properties
nano ~/.flossware/nexus/nexus-prod.properties
```

Use profiles via environment variable:
```bash
export NEXUS_PROFILE=dev
./jnexus.sh list my-repository
```

Or via CLI flag:
```bash
./jnexus.sh --profile prod list my-repository
```

When configured, all UIs will pre-populate these values on startup.

### 3. For Terminal UI Only: Install ncurses

The Swing and AWT GUIs do not require ncurses (they use Java's built-in GUI libraries). Only install ncurses if you plan to use the terminal UI:

**Ubuntu/Debian:**
```bash
sudo apt-get install libncurses-dev
```

**Fedora/RHEL:**
```bash
sudo dnf install ncurses-devel
```

**Arch Linux:**
```bash
sudo pacman -S ncurses
```

## Running the Swing GUI (Recommended)

The Swing GUI provides a modern, native-looking graphical interface:

```bash
./jnexus-swing.sh
```

### Swing GUI Features

- **Modern look and feel** that matches your operating system
- **Responsive design** with background task execution
- **Table-based display** with sortable columns - click column headers to sort by ID, File Size, or Path
- **Multi-row selection** - select multiple rows with CTRL/SHIFT click
- **Delete selected rows** - delete specific components directly from the table
- **Summary row** - shows total components and total bytes (non-editable, highlighted in light blue)
- **Repository dropdown** - select from configured repositories to auto-fill the Repository field
- **Enter key shortcuts** - press Enter in Repository or Regex fields to trigger List operation
- **Busy cursor** - visual feedback during operations with disabled buttons
- **Easy to use** with familiar GUI controls
- **No special dependencies** - uses Java's built-in Swing library
- **Interactive credential collection** - if no configuration files exist, shows a dialog to enter credentials
- **Automatic profile selection** - if multiple profiles exist, shows a selection dialog on startup

### Using the Swing GUI

1. **Start the application:**
   ```bash
   ./jnexus-swing.sh
   ```

   **Note:** If multiple configuration profiles exist (e.g., `nexus.properties`, `nexus-dev.properties`, `nexus-prod.properties`), a dialog will appear asking you to select which profile to use. The application will automatically use it if only one profile exists.

2. **Enter repository details:**
   - **Repository**: Name of the Nexus repository (e.g., `maven-releases`)
     - You can type directly or select from the **Available Repos dropdown** (if configured)
     - Selecting from dropdown auto-fills the Repository field
     - Press **Enter** to trigger List operation
   - **Regex Filter**: Optional pattern to filter components (e.g., `.*SNAPSHOT.*`)
     - Press **Enter** to trigger List operation
   - **Dry Run**: Check this box to preview deletions without actually deleting

3. **Choose your operation:**
   - **List** - Shows components in table (uses cached data if available, fast)
   - **Refresh** - Shows components in table (bypasses cache, always fresh)
   - **Delete All** - Deletes all components matching the filter (shows confirmation dialog first)
   - **Delete Selected** - Deletes only the rows you selected in the table
   - **Clear Results** - Clears the table
   - **Quit** - Exits the application

4. **Work with the results table:**
   - **Sort columns** - Click column headers (ID, File Size, Path) to sort ascending/descending
   - **Select rows** - Click to select one row, CTRL+click for multiple, SHIFT+click for range
   - **Delete selected** - Use "Delete Selected" button to remove specific components
   - **View summary** - Bottom row shows total components and total bytes (highlighted in light blue)

5. **Check status** at the bottom of the window
   - Shows operation progress, cache status, and component count

### Swing GUI Benefits

- ✅ Works on all platforms (Windows, Mac, Linux)
- ✅ No terminal required
- ✅ Familiar GUI controls (buttons, text fields, checkboxes)
- ✅ Background task execution keeps UI responsive
- ✅ Built-in confirmation dialogs for destructive operations

### Direct JAR Execution

```bash
java --enable-preview -cp target/jnexus-1.0-jar-with-dependencies.jar org.flossware.jnexus.JNexusSwing
```

## Running the AWT GUI (Classic)

The AWT GUI provides a classic graphical interface using pure AWT components:

```bash
./jnexus-awt.sh
```

### AWT GUI Features

- **Classic AWT look** familiar to Java users
- **Maximum compatibility** - works on older Java installations
- **Formatted text output** with column headers and summary footer
- **Repository dropdown** - select from configured repositories to auto-fill the Repository field
- **Enter key shortcuts** - press Enter in Repository or Regex fields to trigger List operation
- **Busy cursor** - visual feedback during operations with disabled buttons
- **Lightweight** - lower memory footprint than Swing
- **Works well** in remote desktop/VNC scenarios
- **Interactive credential collection** - if no configuration files exist, shows a dialog to enter credentials
- **Automatic profile selection** - if multiple profiles exist, shows a selection dialog on startup

### Using the AWT GUI

The AWT GUI has the same functionality and usage as the Swing GUI, but uses classic AWT components (Frame, Button, TextField, etc.) instead of modern Swing components.

1. **Start the application:**
   ```bash
   ./jnexus-awt.sh
   ```

   **Note:** If multiple configuration profiles exist, a dialog with a dropdown list will appear asking you to select which profile to use. The application will automatically use it if only one profile exists.

2. Follow the same steps as the Swing GUI

### When to Use AWT Instead of Swing

- Working on older systems with Java 8 or earlier
- Remote desktop or VNC sessions where Swing rendering has issues
- Prefer classic Java look and feel
- Need lower memory footprint
- Maximum compatibility with all Java installations

### Direct JAR Execution

```bash
java --enable-preview -cp target/jnexus-1.0-jar-with-dependencies.jar org.flossware.jnexus.JNexusAWT
```

## Running the Terminal UI

The terminal UI provides an interactive full-screen interface:

```bash
./jnexus-ui.sh
```

### Terminal UI Controls

| Key | Action |
|-----|--------|
| `TAB` | Move to next field/button |
| `Shift+TAB` or `↑` | Move to previous field/button |
| `↓` | Move to next field/button |
| `SPACE` or `ENTER` | Activate focused button or toggle checkbox |
| Type directly | Enter text in focused text field |
| `Backspace` | Delete character in text field |
| `Q` or `ESC` | Quit application |

### Terminal UI Layout

```
┌────────────────────────────────────────────────────────────┐
│ Nexus Repository Manager                                   │
│                                                             │
│ Repository:  [________________]                             │
│                                                             │
│ Regex Filter: [________________]  [✓] Dry Run              │
│                                                             │
│ [List] [Refresh] [Delete] [Clear] [Quit]                   │
│                                                             │
│ Status: Ready - List:cached, Refresh:bypass cache...       │
│                                                             │
│ Results:                                                    │
│ ┌─────────────────────────────────────────────────────────┐│
│ │ (Output appears here)                                   ││
│ └─────────────────────────────────────────────────────────┘│
└────────────────────────────────────────────────────────────┘
```

### Using the Terminal UI

1. **Start the application:**
   ```bash
   ./jnexus-ui.sh
   ```

   **Note:** If multiple configuration profiles exist, a text menu will appear (before starting the ncurses interface) asking you to select which profile to use. Simply type the number corresponding to your choice and press Enter. The application will automatically use it if only one profile exists.

2. **Navigate to the Repository field** (use TAB)

3. **Type the repository name** (e.g., `maven-snapshots`)

4. **Optionally, enter a regex filter** (e.g., `.*SNAPSHOT.*`)

5. **Choose your operation:**
   - **List** - Shows components (uses cached data if available, fast)
   - **Refresh** - Shows components (always fetches fresh data, slower)
   - **Delete** - Deletes components (always uses fresh data, clears cache after)

6. **Check the Dry Run checkbox** (enabled by default) to preview deletions safely

7. **View results** in the Results panel
   - Status bar shows cache age: "Cached (23s old)" or "Not cached"

8. **Press Q or ESC** to quit

### Caching Behavior

The UI uses intelligent caching to reduce server load:

- **List button**: Uses cached results (5-minute TTL by default)
  - First query fetches from server and caches results
  - Subsequent queries within 5 minutes use cached data (instant response)
  - Status shows: "List completed - Cached (Xs old)"

- **Refresh button**: Always bypasses cache and fetches fresh data
  - Use when you need current, up-to-date information
  - Updates the cache with fresh results
  - Status shows: "Cache MISS for repository: X - fetching from server"

- **Delete operation**: Always uses fresh data and clears cache
  - Ensures deletion operates on current repository state
  - Cache invalidated after deletion to prevent stale data

**Benefits:**
- Faster responses for repeated queries
- Reduced load on Nexus server
- User control over data freshness
- Automatic cache invalidation

## Running the Command-Line Interface (CLI)

For scripting and automation, use the CLI:

### Using the wrapper script

```bash
./jnexus.sh <command> [options]
```

### Direct JAR execution

```bash
java -jar target/jnexus-1.0-jar-with-dependencies.jar <command> [options]
```

### CLI Commands

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

**Always use --dry-run first!**

Preview what would be deleted:
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

### CLI Options

| Option | Short | Description |
|--------|-------|-------------|
| `--help` | `-h` | Show help message |
| `--version` | `-V` | Show version information |
| `--verbose` | `-v` | Enable debug logging |
| `--quiet` | `-q` | Only show warnings and errors |
| `--profile` | `-p` | Use a specific configuration profile (e.g., dev, prod, staging) |
| `--dry-run` | `-n` | (Delete only) Preview deletions without executing |
| `--yes` | `-y` | (Delete only) Skip confirmation prompt |

### CLI Examples

```bash
# List all snapshots
./jnexus.sh list releases ".*SNAPSHOT.*"

# Preview deletion
./jnexus.sh delete --dry-run snapshots ".*-2023.*"

# Delete with confirmation
./jnexus.sh delete snapshots ".*-1\.0\..*-SNAPSHOT.*"

# Automated deletion (use with caution!)
./jnexus.sh delete --yes old-repository

# Use development profile
./jnexus.sh --profile dev list maven-snapshots

# Use production profile with verbose logging
./jnexus.sh --profile prod --verbose list maven-releases

# Delete in staging environment
./jnexus.sh --profile staging delete --dry-run test-repo ".*RC.*"
```

## Troubleshooting

### Terminal UI Issues

**Error: "ncurses library is not available!"**
- Install ncurses development library (see Prerequisites above)

**Error: "Failed to initialize Nexus client"**
- Check credentials configuration
- Verify NEXUS_URL, NEXUS_USER, NEXUS_PASSWORD are set
- Or verify ~/.flossware/nexus/nexus.properties exists and is valid

**Terminal display is garbled:**
- Try resizing terminal to at least 120x40 characters
- Check terminal emulator supports ncurses (most do)

**Can't type in text fields:**
- Make sure the field is focused (use TAB to navigate)
- The focused field will have `>` and `<` markers

### CLI Issues

**Error: "Could not find or load main class"**
- Run `./mvnw clean package` to rebuild
- Verify `target/jnexus-1.0-jar-with-dependencies.jar` exists

**Error: "Connection refused" or "Unknown host"**
- Verify NEXUS_URL is correct and accessible
- Check network connectivity to Nexus server

**Error: "401 Unauthorized"**
- Verify NEXUS_USER and NEXUS_PASSWORD are correct
- Check that user has appropriate permissions in Nexus

## Advanced Usage

### Running UI with custom Java options

```bash
java --enable-preview --enable-native-access=ALL-UNNAMED \
    -Xmx512m \
    -cp target/jnexus-1.0-jar-with-dependencies.jar \
    org.flossware.jnexus.JNexusUI
```

### Running CLI with custom Java options

```bash
java -Xmx512m \
    -jar target/jnexus-1.0-jar-with-dependencies.jar \
    list my-repository
```

### Override credentials via environment for single command

```bash
# Override with environment variables
NEXUS_URL=https://other-nexus.com \
NEXUS_USER=admin \
NEXUS_PASSWORD=secret \
./jnexus.sh list test-repo

# Use profile via environment variable
NEXUS_PROFILE=dev ./jnexus.sh list test-repo

# Combine profile with command flags
NEXUS_PROFILE=prod ./jnexus.sh --verbose list maven-releases
```

## Questions?

See [README.md](README.md) for general information or [CONTRIBUTING.md](CONTRIBUTING.md) for development details.

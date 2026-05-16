# Running JNexus CLI

This guide explains how to run the different interfaces of the JNexus CLI tool.

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

**Properties File:**
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

# Optional UI defaults (uncomment to use)
nexus.default.repository=maven-releases
nexus.default.regex=.*SNAPSHOT.*
nexus.default.dryrun=true
```

When configured, the terminal UI will pre-populate these values on startup.

### 3. For Terminal UI: Install ncurses

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
NEXUS_URL=https://other-nexus.com \
NEXUS_USER=admin \
NEXUS_PASSWORD=secret \
./jnexus.sh list test-repo
```

## Questions?

See [README.md](README.md) for general information or [CONTRIBUTING.md](CONTRIBUTING.md) for development details.

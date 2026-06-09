# JNexus Terminal UI Guide

Back to [Getting Started](../getting-started.md) | [Configuration](../advanced/configuration-guide.md)

## Overview

The Terminal UI provides a full-screen ncurses interface for managing Nexus repositories directly from the terminal. It works over SSH without X11 forwarding, making it ideal for server environments and terminal-only workflows.

## System Requirements

- Java 21 or higher
- ncurses library installed
- Terminal emulator with ncurses support (most modern terminals)

### Installing ncurses

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

## Installation

```bash
git clone https://github.com/FlossWare/nexus-java.git
cd nexus-java
./mvnw clean package
```

## Launching

```bash
./jnexus-ui.sh
```

Or directly:
```bash
java --enable-preview --enable-native-access=ALL-UNNAMED \
    -cp target/jnexus-1.0-jar-with-dependencies.jar \
    org.flossware.nexus.NexusUI
```

Note the `--enable-native-access=ALL-UNNAMED` flag is required for the jcurses library to interface with ncurses.

## First Launch

### Credential Configuration

If no configuration files exist, the Terminal UI prompts for credentials via console input **before** starting the ncurses interface:
```
No configuration files found.
Enter Nexus URL: https://nexus.example.com
Enter username: admin
Enter password: ****
Save credentials? (y/n): y
Enter repositories (comma-separated, optional): maven-releases,maven-snapshots
```

### Profile Selection

If multiple profiles exist, a numbered text menu appears **before** the ncurses interface starts:
```
Multiple configuration profiles found:
  1. default
  2. dev
  3. prod
Select profile (1-3): 2
```

## Interface Tour

### Screen Layout

```
+--------------------------------------------------------------------+
| Nexus Repository Manager - Terminal UI                              |
|                                                                      |
| Repository Configuration                                            |
|   Repository:     > [maven-releases________________] <              |
|   Regex Filter:   [.*SNAPSHOT.*_________________]                   |
|   [X] Dry Run (preview only, no actual deletion)                    |
|                                                                      |
|   Available Repos: maven-releases, maven-snapshots, npm-public      |
|                                                                      |
|   [ List ] [ Refresh ] [ Delete ] [ Clear ] [ Quit ]                |
|                                                                      |
+--------------------------------------------------------------------+
| Results                                                              |
|                                                                      |
| ID                              File Size        Path               |
| ===============================  ===============  ================  |
| com.example:app:1.0-SNAPSHOT        1,024,567    path/to/app.jar    |
| com.example:lib:2.0                 2,048,123    path/to/lib.jar    |
|                                                                      |
+--------------------------------------------------------------------+
| List completed - 2 components - Cached (25s old)                     |
+--------------------------------------------------------------------+
```

### Focus Indicators

The currently focused field is marked with `>` and `<` markers:
- `> [text field content] <` -- this field has focus
- `[text field content]` -- this field does not have focus

### Available Repositories

If `nexus.repositories` is configured in your properties file, the list of available repositories is displayed below the input fields for reference.

## Keyboard Controls

| Key | Action |
|-----|--------|
| `TAB` | Move to next field/button |
| `Shift+TAB` | Move to previous field/button |
| `Down Arrow` | Move to next field/button |
| `Up Arrow` | Move to previous field/button |
| `SPACE` | Activate focused button or toggle checkbox |
| `ENTER` | Activate focused button or toggle checkbox |
| Type directly | Enter text in focused text field |
| `Backspace` | Delete character in text field |
| `Q` | Quit application |
| `ESC` | Quit application |

## Workflows

### Listing Components

1. Press TAB to navigate to the Repository field
2. Type the repository name (e.g., `maven-snapshots`)
3. TAB to the Regex Filter field (optional)
4. Type a filter pattern (e.g., `.*SNAPSHOT.*`)
5. TAB to the **List** button
6. Press SPACE or ENTER to list

### Refreshing (Bypass Cache)

1. Set repository and filters as above
2. TAB to the **Refresh** button
3. Press SPACE or ENTER
4. Results are fetched fresh from the server (cache bypassed)

### Deleting Components

1. Set repository and filters
2. Ensure **Dry Run** is checked (TAB to checkbox, SPACE to toggle)
3. TAB to **Delete** and press SPACE to preview
4. If satisfied, uncheck Dry Run and delete again
5. Cache is automatically cleared after deletion

### Clearing Results

TAB to the **Clear** button and press SPACE to clear the results area.

### Quitting

Press `Q` or `ESC` at any time, or TAB to the **Quit** button and press SPACE.

## Caching Behavior

The Terminal UI uses the same 5-minute TTL cache as all other interfaces:

- **List**: Uses cached data (fast, instant if cached)
- **Refresh**: Always fetches fresh data from the server
- **Delete**: Always uses fresh data, clears cache after deletion

The status bar shows cache status: `Cached (Xs old)` or `Not cached`.

## Terminal Recommendations

- Minimum terminal size: **120 columns x 40 rows** for comfortable viewing
- If the display looks garbled, try resizing your terminal
- Works well with common terminal emulators: gnome-terminal, xterm, iTerm2, Windows Terminal, PuTTY

## When to Use Terminal UI

| Scenario | Use Terminal UI? |
|----------|-----------------|
| SSH without X11 forwarding | Yes |
| Server without desktop environment | Yes |
| tmux / screen sessions | Yes |
| Quick interactive operations | Yes |
| Need sorting/filtering/stats | No -- use Swing GUI |
| Scripting / automation | No -- use CLI |
| Remote desktop / VNC | Consider AWT GUI |

## Tips and Tricks

1. **Pre-populate fields**: Configure `nexus.default.repository` and `nexus.default.regex` in your properties file to pre-fill fields on startup
2. **Quick navigation**: TAB moves forward through all elements; Shift+TAB moves backward
3. **Repository reference**: The "Available Repos" line shows your configured repositories -- type one of those names into the Repository field
4. **Check dry run**: Dry Run is enabled by default -- always verify before unchecking

## Troubleshooting

| Issue | Solution |
|-------|----------|
| `ncurses library is not available!` | Install ncurses-dev package (see requirements above) |
| `Failed to initialize Nexus client` | Check credentials configuration |
| Display is garbled | Resize terminal to at least 120x40 |
| Cannot type in fields | Use TAB to focus the field first (look for `>` markers) |
| Colors look wrong | Check terminal supports 256 colors (`echo $TERM`) |
| Java warning about native access | The `--enable-native-access=ALL-UNNAMED` flag is required |

## Video Tutorials

While there is no dedicated Terminal UI video, these related tutorials are helpful:

- [Getting Started with JNexus CLI](../video-tutorials/scripts/02-getting-started-cli.md) (5 min) -- CLI workflows also apply to Terminal UI
- [JNexus in 90 Seconds](../video-tutorials/scripts/01-jnexus-in-90-seconds.md) (1.5 min) -- Quick overview across all platforms
- [Troubleshooting Common Issues](../video-tutorials/scripts/10-troubleshooting.md) (4 min) -- Connection and configuration troubleshooting

See the [Video Tutorials catalog](../video-tutorials/README.md) for all available tutorials.

Back to [Getting Started](../getting-started.md) | [All Guides](../getting-started.md#platform-guides)

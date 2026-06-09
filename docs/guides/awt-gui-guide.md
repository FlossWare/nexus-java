# JNexus AWT GUI Guide

Back to [Getting Started](../getting-started.md) | [Configuration](../advanced/configuration-guide.md)

## Overview

The AWT GUI provides a classic graphical interface using pure Java AWT components. It offers the same core functionality as the Swing GUI but with maximum compatibility, lower memory usage, and better behavior in remote desktop and VNC environments.

## System Requirements

- Java 21 or higher
- Desktop environment with display (X11, Wayland, or macOS)
- No additional dependencies (AWT is built into the JDK)

## Installation

```bash
git clone https://github.com/FlossWare/nexus-java.git
cd nexus-java
./mvnw clean package
```

## Launching

```bash
./jnexus-awt.sh
```

Or directly:
```bash
java --enable-preview -cp target/jnexus-1.0-jar-with-dependencies.jar org.flossware.nexus.NexusAWT
```

## First Launch

### Credential Configuration

If no configuration files exist, the AWT GUI shows a dialog to enter:
- Nexus URL
- Username
- Password
- Repository list (optional)

After entering credentials, you can optionally save them to a properties file.

### Profile Selection

If multiple profiles exist, a custom dialog with a dropdown (Choice component) and OK/Cancel buttons appears on startup.

## Interface Tour

### Main Window Layout

```
+-----------------------------------------------------------------------+
| Nexus Repository Manager - AWT UI                                      |
+-----------------------------------------------------------------------+
| Repository Configuration                                              |
|   Repository:  [maven-releases v]  (dropdown: All, maven-releases...) |
|   Regex Filter: [                     ]  (Enter to trigger List)      |
|   [x] Dry Run (preview only, no actual deletion)                      |
|   Nexus URL:   https://nexus.example.com          (read-only)         |
|   Config File: ~/.flossware/nexus/nexus.properties (read-only)        |
|   [List] [Refresh] [Delete] [Clear] [Quit]                            |
+-----------------------------------------------------------------------+
| Results                                                                |
| +-------------------------------------------------------------------+ |
| | ID                              File Size        Path             | |
| | ===============================  ===============  =============== | |
| | com.example:app:1.0-SNAPSHOT        1,024,567    path/to/app.jar  | |
| | com.example:lib:2.0                 2,048,123    path/to/lib.jar  | |
| | ===============================  ===============  =============== | |
| | TOTAL: 2 components                3,072,690    (2.93 MB)         | |
| +-------------------------------------------------------------------+ |
+-----------------------------------------------------------------------+
| List completed - 2 components - Cached (25s old)                       |
+-----------------------------------------------------------------------+
```

### Key UI Components

- **Repository dropdown**: AWT Choice component with "All" and configured repositories
- **Regex Filter**: AWT TextField with Enter key listener to trigger List
- **Dry Run checkbox**: AWT Checkbox for safe deletion preview
- **Nexus URL / Config File**: Read-only display fields
- **Results area**: AWT TextArea with formatted text output (column-aligned)
- **Status bar**: Shows operation results and cache age

### Differences from Swing GUI

| Feature | Swing GUI | AWT GUI |
|---------|-----------|---------|
| Results display | JTable (7 columns, sortable) | TextArea (formatted text, 3 columns) |
| Column sorting | Click headers to sort | Not available |
| Row selection | Multi-select with Ctrl/Shift | Not available |
| Delete Selected | Yes (per-row selection) | No (Delete All only) |
| Component details | Double-click for metadata | Not available |
| Statistics dialog | 5-tab dialog | Not available |
| Advanced filters | Collapsible panel | Not available |
| Look and feel | Native OS look | Classic AWT look |

## Workflows

### Listing Components

1. Select a repository from the dropdown
2. Optionally enter a regex pattern
3. Click **List** (cached) or **Refresh** (fresh)
4. Results appear as formatted text with column headers and summary footer

### Deleting Components

1. Set your repository and regex filter
2. Check **Dry Run** for a preview
3. Click **Delete** to see what would be deleted
4. Uncheck **Dry Run** and click **Delete** again to perform actual deletion

### Clearing Results

Click **Clear** to remove all text from the results area.

## Keyboard Shortcuts

| Shortcut | Action |
|----------|--------|
| Enter (in Regex field) | Trigger List operation |

## When to Use AWT

The AWT GUI is the best choice when:

- **Remote desktop / VNC**: Swing rendering can have issues over RDP or VNC; AWT renders more reliably
- **Older Java**: If running on older or minimal Java installations where Swing has rendering problems
- **Low memory**: AWT uses less memory than Swing
- **Simplicity**: When you only need basic list/delete functionality without analytics

For full features (statistics, advanced filters, column sorting, multi-select delete), use the [Swing GUI](swing-gui-guide.md) instead.

## Tips and Tricks

1. **Text output is copyable**: Select text in the results area and use Ctrl+C to copy
2. **Regex in real time**: Type your regex and press Enter -- no need to click List
3. **Watch cache age**: The status bar shows "Cached (Xs old)" so you know data freshness
4. **Busy cursor**: When the cursor changes to a wait cursor, an operation is in progress

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Blank window on startup | Check Java version (`java -version` must be 21+) |
| Text appears garbled | Resize the window or change system font settings |
| Window does not appear | Check `DISPLAY` environment variable is set correctly |
| Slow rendering over VNC | AWT should be faster than Swing -- if issues persist, use Terminal UI |

## Video Tutorials

While there is no dedicated AWT video, these related tutorials are helpful:

- [JNexus Swing GUI Walkthrough](../video-tutorials/scripts/03-swing-gui-walkthrough.md) (6 min) -- The Swing GUI shares the same workflow concepts as AWT
- [JNexus in 90 Seconds](../video-tutorials/scripts/01-jnexus-in-90-seconds.md) (1.5 min) -- Quick overview across all platforms
- [Troubleshooting Common Issues](../video-tutorials/scripts/10-troubleshooting.md) (4 min) -- Connection and configuration troubleshooting

See the [Video Tutorials catalog](../video-tutorials/README.md) for all available tutorials.

Back to [Getting Started](../getting-started.md) | [All Guides](../getting-started.md#platform-guides)

# JNexus Swing GUI Guide

Back to [Getting Started](../getting-started.md) | [Configuration](../advanced/configuration-guide.md) | [Filtering](../advanced/filtering-guide.md)

## Overview

The Swing GUI is the recommended desktop interface for JNexus. It provides a modern, spreadsheet-like table interface with sortable columns, advanced filters, component details, and repository statistics -- all without needing the terminal.

## System Requirements

- Java 21 or higher
- Desktop environment with display (X11, Wayland, or macOS)
- No additional dependencies (Swing is built into the JDK)

## Installation

```bash
git clone https://github.com/FlossWare/nexus-java.git
cd nexus-java
./mvnw clean package
```

## Launching

```bash
./jnexus-swing.sh
```

Or directly:
```bash
java --enable-preview -cp target/jnexus-1.0-jar-with-dependencies.jar org.flossware.nexus.NexusSwing
```

## First Launch

### Credential Configuration

If no configuration files exist (`~/.flossware/nexus/nexus.properties`), the Swing GUI shows an interactive dialog asking for:
- Nexus URL
- Username
- Password
- Repository list (optional, comma-separated)

After entering credentials, you can optionally save them to a properties file for future use. When you save with a repository list, the first repository automatically becomes the default.

### Profile Selection

If multiple configuration profiles exist (e.g., `nexus.properties`, `nexus-dev.properties`, `nexus-prod.properties`), a dialog appears on startup asking you to select which profile to use. If only one profile exists, it is selected automatically.

## Interface Tour

### Main Window Layout

```
+-----------------------------------------------------------------------+
| Nexus Repository Manager - Swing UI                                    |
+-----------------------------------------------------------------------+
| Repository Configuration                                              |
|   Repository:  [maven-releases v]  (dropdown: All, maven-releases...) |
|   Regex Filter: [                     ]  (Enter to trigger List)      |
|   [x] Dry Run (preview only, no actual deletion)                      |
|   Nexus URL:   https://nexus.example.com          (read-only)         |
|   Config File: ~/.flossware/nexus/nexus.properties (read-only)        |
|   > Advanced Filters  (click to expand/collapse)                      |
|   [List] [Refresh] [Delete All] [Delete Selected] [Clear] [Stats] [Quit] |
+-----------------------------------------------------------------------+
| Results                                    (click column headers to sort) |
| +-------------------------------------------------------------------+ |
| | ID     | File Size (Bytes) | Size (MB) | Size (GB) | Created | Content Type | Path | |
| |--------|-------------------|-----------|-----------|---------|--------------|------| |
| | com... |       1,024,567   |      0.98 |     0.00  | 2024-.. | application/java-archive | path/... | |
| | com... |       2,048,123   |      1.95 |     0.00  | 2024-.. | application/java-archive | path/... | |
| | TOTAL: 2 components  3,072,690   2.93      0.00                              | |
| +-------------------------------------------------------------------+ |
+-----------------------------------------------------------------------+
| Total: 2 component(s) - 3,072,690 bytes (2.93 MB / 0.00 GB)          |
+-----------------------------------------------------------------------+
```

### Repository Configuration Panel

- **Repository dropdown**: JComboBox listing "All" plus all configured repositories from `nexus.repositories`
- **Regex Filter**: Text field for regex pattern; press Enter to trigger List operation
- **Dry Run checkbox**: When checked, delete operations only preview without actually deleting
- **Nexus URL**: Read-only field showing the connected server
- **Config File**: Read-only field showing which properties file is loaded

### Advanced Filters Panel

Click "Advanced Filters" to expand/collapse the panel.

| Filter | Input | Example |
|--------|-------|---------|
| Min Size | Bytes (integer) | `1048576` (1 MB) |
| Max Size | Bytes (integer) | `104857600` (100 MB) |
| Created After | ISO 8601 date | `2024-01-01T00:00:00Z` |
| Created Before | ISO 8601 date | `2024-12-31T23:59:59Z` |
| File Extension | Extension with dot | `.jar`, `.war`, `.pom` |

All filters are optional and can be combined. They are applied on top of the regex filter.

### Results Table

The table displays 7 columns:

| Column | Type | Description |
|--------|------|-------------|
| ID | Text | Component identifier |
| File Size (Bytes) | Numeric | Raw size in bytes |
| File Size (MB) | Numeric | Size in megabytes |
| File Size (GB) | Numeric | Size in gigabytes |
| Created | Date | Creation timestamp |
| Content Type | Text | MIME type (e.g., `application/java-archive`) |
| Path | Text | Full path in the repository |

**Table features:**
- Click column headers to sort ascending/descending (numeric sorting for size columns)
- The last row is a summary row (light blue background) showing totals
- The summary row is non-editable and non-selectable for deletion

### Action Buttons

| Button | Action | Notes |
|--------|--------|-------|
| List | Fetch components (uses cache) | Fastest, may show slightly stale data |
| Refresh | Fetch components (bypass cache) | Always fresh from Nexus server |
| Delete All | Delete all listed components | Shows confirmation dialog first |
| Delete Selected | Delete selected rows only | Only visible when rows are selected |
| Clear Results | Clear the table | Removes all displayed data |
| Statistics | Open statistics dialog | Analyzes currently listed components |
| Quit | Exit application | |

### Status Bar

The status bar at the bottom shows:
- **No selection**: `Total: X component(s) - Y bytes (Z MB / W GB)`
- **With selection**: `Selected: A component(s) - B bytes (C MB / D GB) | Total: X component(s) - Y bytes (Z MB / W GB)`

## Workflows

### Listing Components

1. Select a repository from the dropdown (or "All" for all repositories)
2. Optionally enter a regex pattern in the filter field
3. Click **List** (uses cache) or **Refresh** (always fresh)
4. Results appear in the table

### Searching for Large Files

1. Select a repository
2. Click **Advanced Filters** to expand the panel
3. Enter a minimum size value (e.g., `10485760` for 10 MB)
4. Click **List**
5. Click the "File Size (Bytes)" column header to sort by size

### Viewing Component Details

1. List components
2. **Double-click** any row in the table
3. A dialog shows full metadata: ID, path, file size, content type, format, created date, last modified, checksum

### Deleting Components

**Delete all matching components:**
1. Set your repository and filters
2. Ensure Dry Run is checked for a preview first
3. Click **Delete All** -- a confirmation dialog appears
4. Uncheck Dry Run and repeat to perform actual deletion

**Delete specific components:**
1. List components
2. Select rows with click (single), Ctrl+click (add to selection), or Shift+click (range)
3. The **Delete Selected** button appears when rows are selected
4. Click **Delete Selected** -- a confirmation dialog appears
5. Confirm to proceed

### Viewing Statistics

1. List components for a repository
2. Click **Statistics**
3. A dialog opens with 5 tabs:
   - **Overview**: Total components, total size (MB/GB), average, median
   - **Size Distribution**: Histogram across 5 size buckets with percentages
   - **File Types**: Breakdown by extension with sizes and percentages
   - **Age Distribution**: Components grouped by age (7/30/90 days, older)
   - **Largest Components**: Top 20 components sorted by size (displayed in a JTable)

## Keyboard Shortcuts

| Shortcut | Action |
|----------|--------|
| Enter (in Regex field) | Trigger List operation |
| Ctrl+Click | Add row to selection |
| Shift+Click | Select range of rows |
| Double-click row | Open component details dialog |

## Tips and Tricks

1. **Sort by size**: Click the "File Size (Bytes)" column header to find the largest components quickly
2. **Multi-select and delete**: Use Ctrl+Click to select individual components for targeted deletion
3. **Check status bar**: The status bar always shows the total count and size of listed components
4. **Use Refresh sparingly**: List uses cache (fast), Refresh hits the server (slower but current)
5. **Advanced Filters persist**: Filter values remain until you clear them, so clear filters before a new search
6. **Profile on startup**: If you work with multiple Nexus servers, set up profiles and select on launch
7. **Busy cursor**: When the cursor changes to a wait cursor, operations are in progress -- buttons are disabled until complete

## When to Use Swing vs Other Interfaces

| Scenario | Recommended Interface |
|----------|----------------------|
| Browsing and exploring repositories | Swing GUI |
| Analyzing repository statistics | Swing GUI |
| Selecting specific components to delete | Swing GUI (multi-select) |
| Scripting or automation | CLI |
| SSH without X11 forwarding | Terminal UI |
| Remote desktop / VNC issues | AWT GUI |
| Mobile / on-the-go | Android or iOS app |

## Video Tutorials

These video tutorials cover Swing GUI workflows in detail:

- [JNexus Swing GUI Walkthrough](../video-tutorials/scripts/03-swing-gui-walkthrough.md) (6 min) -- Complete tour of the Swing interface
- [Safe Deletion with Dry-Run Mode](../video-tutorials/scripts/04-safe-deletion-dry-run.md) (3 min) -- GUI dry-run demo and safety features
- [Advanced Filtering and Search](../video-tutorials/scripts/05-advanced-filtering.md) (4 min) -- Using advanced filter panel
- [Repository Statistics and Analytics](../video-tutorials/scripts/06-repository-statistics.md) (5 min) -- Statistics dialog walkthrough

See the [Video Tutorials catalog](../video-tutorials/README.md) for all available tutorials.

Back to [Getting Started](../getting-started.md) | [All Guides](../getting-started.md#platform-guides)

# JNexus Statistics Guide

Back to [Getting Started](../getting-started.md) | [Filtering Guide](filtering-guide.md) | [CLI Guide](../guides/cli-guide.md)

## Overview

JNexus provides comprehensive repository analytics through the `stats` command (CLI) and Statistics dialog/screen (GUIs, mobile). Statistics help you understand repository composition, identify large artifacts, track growth, and plan cleanup operations.

## Accessing Statistics

### CLI

```bash
# Text format (default)
./jnexus.sh stats maven-releases

# JSON format (for scripting)
./jnexus.sh stats maven-releases --format json
```

### Swing GUI

1. List components for a repository
2. Click the **Statistics** button
3. A dialog opens with 5 tabbed panels

### Android / iOS / macOS

1. Navigate to the **Stats** tab/section
2. Select a repository
3. Tap **Calculate Statistics**

## Statistics Breakdown

### Overview Metrics

| Metric | Description |
|--------|-------------|
| Total Components | Number of components in the repository |
| Total Size | Sum of all component sizes (displayed in MB and GB) |
| Average Size | Mean component size (total size / component count) |
| Median Size | Middle value when components are sorted by size |

The median is often more useful than the average because a few very large artifacts can skew the average upward.

### Size Distribution

Components are grouped into 5 size buckets:

| Bucket | Range |
|--------|-------|
| < 1 MB | 0 bytes to 1,048,575 bytes |
| 1-10 MB | 1,048,576 to 10,485,759 bytes |
| 10-100 MB | 10,485,760 to 104,857,599 bytes |
| 100 MB - 1 GB | 104,857,600 to 1,073,741,823 bytes |
| > 1 GB | 1,073,741,824 bytes and above |

Each bucket shows the count of components and percentage of total. This helps identify whether your repository is dominated by many small files or a few large ones.

### File Type Breakdown

Components are grouped by file extension (e.g., `.jar`, `.pom`, `.war`, `.xml`). For each extension, the statistics show:

- Number of files with that extension
- Total size of all files with that extension
- Percentage of total repository size

This reveals which file types consume the most storage. Common findings:
- `.jar` files typically dominate Maven repositories
- `.pom` and `.xml` files are usually small but numerous
- `-sources.jar` and `-javadoc.jar` can add significant size

### Age Distribution

Components are grouped into 4 age buckets based on their creation date:

| Bucket | Description |
|--------|-------------|
| Last 7 days | Recently published artifacts |
| Last 30 days | Artifacts from the past month |
| Last 90 days | Artifacts from the past quarter |
| Older than 90 days | Artifacts older than 90 days |

This helps identify:
- How actively the repository is being published to
- How much old content could potentially be cleaned up
- Growth trends over time

### Largest Components

The largest components (up to 20 stored, CLI displays top 10), sorted by size in descending order. Each entry shows:
- Component ID
- File size (in bytes, MB, and GB)
- Path in the repository

This immediately identifies candidates for cleanup if storage space is a concern.

## CLI Output Examples

### Text Format

```
Repository Statistics: maven-releases
=====================================

Overview:
  Total Components: 1,247
  Total Size: 3,456,789,012 bytes (3,296.51 MB / 3.22 GB)
  Average Size: 2,771,643 bytes (2.64 MB)
  Median Size: 524,288 bytes (0.50 MB)

Size Distribution:
  < 1 MB:              847 components (67.9%)
  1-10 MB:             312 components (25.0%)
  10-100 MB:            75 components ( 6.0%)
  100 MB - 1 GB:        12 components ( 1.0%)
  > 1 GB:                1 components ( 0.1%)

File Type Breakdown:
  .jar:           2,046.33 MB (62.1%)
  .pom:              43.56 MB ( 1.3%)
  .war:             942.01 MB (28.6%)
  .xml:              11.77 MB ( 0.4%)

Age Distribution:
  Last 7 days:          45 components
  Last 30 days:        123 components
  Last 90 days:        287 components
  Older than 90 days:  792 components

Largest Components (Top 10):
  1. com.example:big-app:2.0  1,234,567,890 bytes (1,177.38 MB)
  2. com.example:war-app:1.5    456,789,012 bytes (435.62 MB)
  ...
```

### JSON Format

```json
{
  "repository": "maven-releases",
  "totalComponents": 1247,
  "totalSize": 3456789012,
  "averageSize": 2771643,
  "medianSize": 524288,
  "sizeDistribution": {
    "< 1 MB": 847,
    "1-10 MB": 312,
    "10-100 MB": 75,
    "100 MB - 1 GB": 12,
    "> 1 GB": 1
  },
  "fileTypeBreakdown": {
    ".jar": 2145678901,
    ".pom": 45678901,
    ".war": 987654321,
    ".xml": 12345678
  },
  "ageDistribution": {
    "Last 7 days": 45,
    "Last 30 days": 123,
    "Last 90 days": 287,
    "Older than 90 days": 792
  }
}
```

## Swing GUI Statistics Dialog

The Swing GUI Statistics dialog has 5 tabs:

| Tab | Content |
|-----|---------|
| Overview | Total components, total size, average, median |
| Size Distribution | Histogram with percentages for each bucket |
| File Types | Table of extensions with sizes and percentages |
| Age Distribution | Counts for each age bucket |
| Largest Components | JTable with largest components sorted by size |

## Use Cases

### Identifying Cleanup Targets

```bash
# Find the biggest consumers of storage
./jnexus.sh stats maven-releases

# Then filter for the specific file types or sizes
./jnexus.sh list maven-releases --min-size 104857600 --show-metadata

# Preview deletion
./jnexus.sh delete --dry-run maven-releases ".*old-version.*"
```

### Monitoring Repository Growth

Run stats periodically and save JSON output:
```bash
#!/bin/bash
DATE=$(date +%Y-%m-%d)
./jnexus.sh stats maven-releases --format json > "stats-$DATE.json"
```

### Comparing Repositories

```bash
for repo in maven-releases maven-snapshots npm-public; do
    echo "=== $repo ==="
    ./jnexus.sh stats "$repo"
    echo
done
```

### Finding Source and Javadoc Bloat

```bash
# Sources JARs
./jnexus.sh list maven-releases ".*-sources\.jar$" --show-metadata

# Javadoc JARs
./jnexus.sh list maven-releases ".*-javadoc\.jar$" --show-metadata
```

## Known Limitations

- **Multi-asset components**: Statistics count only the first asset per component. Components with multiple assets (e.g., JAR + POM + sources) may underreport total size. See [CLAUDE.md](../../CLAUDE.md) for details.
- **Client-side computation**: All statistics are computed client-side. For very large repositories, the initial data fetch may be slow.
- **Cache interaction**: Stats use cached component data. Use Refresh before Stats if you need current numbers.

## Data Model

Statistics are encapsulated in the `RepositoryStats` class (Java) / struct (Swift):

- `totalComponents`: int
- `totalSize`: long (bytes)
- `averageSize`: long (bytes)
- `medianSize`: long (bytes)
- `sizeDistribution`: Map<String, Integer> (bucket name to count)
- `fileTypeBreakdown`: Map<String, Long> (extension to total bytes)
- `ageDistribution`: Map<String, Integer> (bucket name to count)
- `largestComponents`: List<ComponentMetadata> (up to top 20 by size, descending)

## Video Tutorial

See [Repository Statistics and Analytics](../video-tutorials/scripts/06-repository-statistics.md) (5 min) for a visual walkthrough of overview metrics, size distribution, file type breakdown, age distribution, and largest components.

Back to [Getting Started](../getting-started.md) | [Filtering Guide](filtering-guide.md)

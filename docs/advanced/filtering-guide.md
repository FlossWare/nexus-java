# JNexus Filtering Guide

Back to [Getting Started](../getting-started.md) | [CLI Guide](../guides/cli-guide.md) | [Statistics Guide](statistics-guide.md)

## Overview

JNexus provides powerful filtering capabilities to narrow down component results. All filtering is done client-side after fetching data from the Nexus API (the Nexus Repository Manager v1 API does not support query parameters for filtering). Multiple filters can be combined and are applied sequentially using AND logic.

## Filter Types

### 1. Regex Filter (Path Pattern)

Filter components by matching their full path against a regular expression.

**Available on:** All platforms (CLI, Swing, AWT, Terminal, Android, iOS, macOS)

**CLI:**
```bash
# Second positional argument is the regex
./jnexus.sh list maven-releases ".*SNAPSHOT.*"
```

**GUI/Mobile:** Enter the pattern in the Regex Filter field.

**Common patterns:**

| Pattern | Matches |
|---------|---------|
| `.*SNAPSHOT.*` | All SNAPSHOT artifacts |
| `.*-1\.0\..*` | Version 1.0.x artifacts |
| `com/example/.*` | All artifacts under com/example |
| `.*\.jar$` | All JAR files |
| `.*\.(jar\|war\|ear)$` | JAR, WAR, and EAR files |
| `(?i).*test.*` | Case-insensitive match for "test" |
| `^(?!.*SNAPSHOT).*$` | Everything except SNAPSHOTs (negation) |

**Regex tips:**
- `.*` matches any sequence of characters
- `\.` matches a literal dot (without backslash, `.` matches any character)
- `$` matches end of string
- `^` matches start of string
- `(?i)` enables case-insensitive matching
- `(?!...)` is a negative lookahead

### 2. Size Range Filters

Filter components by file size in bytes.

**Available on:** CLI, Swing GUI, Android, iOS, macOS

**CLI:**
```bash
# Minimum size (bytes)
./jnexus.sh list maven-releases --min-size 1048576

# Maximum size (bytes)
./jnexus.sh list maven-releases --max-size 10485760

# Size range
./jnexus.sh list maven-releases --min-size 1048576 --max-size 10485760
```

**Common size values:**

| Size | Bytes |
|------|-------|
| 1 KB | 1024 |
| 10 KB | 10240 |
| 100 KB | 102400 |
| 1 MB | 1048576 |
| 10 MB | 10485760 |
| 100 MB | 104857600 |
| 1 GB | 1073741824 |

### 3. Date Range Filters

Filter components by their creation date.

**Available on:** CLI, Swing GUI, Android, iOS, macOS

**CLI:**
```bash
# Created after a date
./jnexus.sh list maven-releases --created-after 2024-01-01T00:00:00Z

# Created before a date
./jnexus.sh list maven-releases --created-before 2024-06-30T23:59:59Z

# Date range
./jnexus.sh list maven-releases \
    --created-after 2024-01-01T00:00:00Z \
    --created-before 2024-03-31T23:59:59Z
```

**Date format:** ISO 8601 with timezone: `YYYY-MM-DDTHH:MM:SSZ`

**GUI:** In the Swing GUI Advanced Filters panel, enter dates as ISO 8601 strings. On iOS/macOS, native DatePicker is used instead.

### 4. File Extension Filter

Filter components by their file extension.

**Available on:** CLI, Swing GUI, Android, iOS, macOS

**CLI:**
```bash
./jnexus.sh list maven-releases --extension .jar
./jnexus.sh list maven-releases --extension .war
./jnexus.sh list maven-releases --extension .pom
```

**Note:** Include the dot in the extension (e.g., `.jar`, not `jar`).

### 5. Component Name Pattern

Filter components by matching their name against a pattern (used in SearchCriteria).

**Available on:** Android, iOS, macOS (via Search screen)

This filter matches against the component name/ID, as distinct from the regex filter which matches against the full path.

## Combining Filters

All filters use AND logic -- a component must match all active filters to appear in results.

**CLI example combining all filter types:**
```bash
./jnexus.sh list maven-releases ".*com/example/.*" \
    --min-size 100000 \
    --max-size 10000000 \
    --created-after 2024-01-01T00:00:00Z \
    --created-before 2024-06-30T23:59:59Z \
    --extension .jar \
    --show-metadata
```

This finds JAR files under `com/example/` between 100KB and 10MB, created in the first half of 2024, and shows full metadata.

## Filter Application Order

Internally, filters are applied sequentially:

1. Fetch all components from repository (or cache)
2. Apply regex filter on full path
3. Apply min-size filter
4. Apply max-size filter
5. Apply created-after date filter
6. Apply created-before date filter
7. Apply file extension filter
8. Apply component name pattern (if using SearchCriteria)

Since all filters use AND logic, the order does not affect the result. However, the regex filter is typically applied first because it is the most commonly used.

## Platform-Specific Filter Availability

| Filter | CLI | Swing | AWT | Terminal | Android | iOS | macOS |
|--------|-----|-------|-----|----------|---------|-----|-------|
| Regex (path) | Yes | Yes | Yes | Yes | Yes | Yes | Yes |
| Min/Max size | Yes | Yes | No | No | Yes | Yes | Yes |
| Date range | Yes | Yes | No | No | Yes | Yes | Yes |
| Extension | Yes | Yes | No | No | Yes | Yes | Yes |
| Name pattern | No | No | No | No | Yes | Yes | Yes |
| Show metadata | Yes | Auto | No | No | Auto | Auto | Auto |

"Auto" means metadata is always displayed when available (GUI shows all columns).

## SearchCriteria Builder Pattern

On platforms that support advanced search (Android, iOS, macOS), filters are constructed using the SearchCriteria Builder pattern:

**Java (Android):**
```java
SearchCriteria criteria = SearchCriteria.builder()
    .repository("maven-releases")
    .minSize(1048576L)
    .maxSize(10485760L)
    .createdAfter(Instant.parse("2024-01-01T00:00:00Z"))
    .fileExtension(".jar")
    .regexFilter(".*SNAPSHOT.*")
    .build();

List<ComponentMetadata> results = service.searchComponents(criteria);
```

**Swift (iOS/macOS):**
```swift
let criteria = SearchCriteria(
    repository: "maven-releases",
    minSize: 1048576,
    maxSize: 10485760,
    createdAfter: ISO8601DateFormatter().date(from: "2024-01-01T00:00:00Z"),
    fileExtension: ".jar"
)

let results = try await service.searchComponents(criteria: criteria)
```

## Use Cases

### Find Old SNAPSHOTs

```bash
./jnexus.sh list maven-snapshots ".*SNAPSHOT.*" \
    --created-before 2024-01-01T00:00:00Z
```

### Find Large Artifacts for Cleanup

```bash
./jnexus.sh list maven-releases --min-size 104857600 --show-metadata
```

### Find All WARs in a Specific Path

```bash
./jnexus.sh list maven-releases ".*com/mycompany/.*" --extension .war
```

### Find Recently Published Artifacts

```bash
./jnexus.sh list maven-releases \
    --created-after $(date -u -d '7 days ago' +%Y-%m-%dT%H:%M:%SZ)
```

### Analyze Specific File Types

```bash
# Find all POM files
./jnexus.sh list maven-releases --extension .pom --show-metadata

# Find all source JARs
./jnexus.sh list maven-releases ".*-sources\.jar$"
```

## Performance Considerations

- All filtering happens client-side after fetching the complete component list
- The first query fetches all components and caches them (5-minute TTL)
- Subsequent queries with different filters use cached data and are nearly instant
- For very large repositories (10,000+ components), the initial fetch may take several seconds
- Use the `Refresh` button only when you need current data; `List` uses cache

## Video Tutorial

See [Advanced Filtering and Search](../video-tutorials/scripts/05-advanced-filtering.md) (4 min) for a visual walkthrough of regex patterns, size/date/extension filters, and combining multiple filters.

Back to [Getting Started](../getting-started.md) | [CLI Guide](../guides/cli-guide.md) | [Statistics Guide](statistics-guide.md)

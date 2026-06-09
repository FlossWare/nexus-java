# JNexus Command-Line Interface Guide

Back to [Getting Started](../getting-started.md) | [Configuration](../advanced/configuration-guide.md) | [Filtering](../advanced/filtering-guide.md)

## Overview

The CLI is the primary interface for scripting, automation, and CI/CD integration. It provides three main commands (`list`, `delete`, `stats`) plus a `history` command for tracking deletions, all powered by Picocli.

## System Requirements

- Java 21 or higher
- Maven (or included Maven wrapper `./mvnw`)
- Access to a Nexus repository with valid credentials

## Installation

### Build from Source

```bash
git clone https://github.com/FlossWare/nexus-java.git
cd nexus-java
./mvnw clean package
```

This creates `target/jnexus-1.0-jar-with-dependencies.jar`.

### Verify Installation

```bash
java -jar target/jnexus-1.0-jar-with-dependencies.jar --help
```

You should see the command help output with available subcommands.

### Using the Wrapper Script

The `jnexus.sh` script auto-builds if needed and finds the correct JAR:

```bash
./jnexus.sh --help
```

## Quick Start

```bash
# Configure credentials
export NEXUS_URL=https://nexus.example.com
export NEXUS_USER=your-username
export NEXUS_PASSWORD=your-password

# List components
./jnexus.sh list maven-releases

# Search with filters
./jnexus.sh list maven-releases ".*SNAPSHOT.*" --min-size 1000000

# Delete with dry-run
./jnexus.sh delete --dry-run maven-releases ".*SNAPSHOT.*"

# Repository statistics
./jnexus.sh stats maven-releases
```

## Commands Reference

### list

List components in a Nexus repository with optional filtering.

**Syntax:**
```
jnexus list <repository> [regex] [options]
```

**Arguments:**
| Argument | Required | Description |
|----------|----------|-------------|
| `repository` | Yes | Name of the Nexus repository |
| `regex` | No | Regex pattern to filter component paths |

**Options:**
| Option | Description |
|--------|-------------|
| `--min-size BYTES` | Minimum file size in bytes |
| `--max-size BYTES` | Maximum file size in bytes |
| `--created-after DATE` | Filter by creation date (ISO 8601: `2024-01-01T00:00:00Z`) |
| `--created-before DATE` | Filter by creation date (ISO 8601) |
| `--extension EXT` | Filter by file extension (e.g., `.jar`, `.war`) |
| `--show-metadata` | Display full component metadata |

**Examples:**
```bash
# List all components
./jnexus.sh list maven-releases

# List SNAPSHOT components
./jnexus.sh list maven-releases ".*SNAPSHOT.*"

# List large JAR files
./jnexus.sh list maven-releases --min-size 10485760 --extension .jar

# List components created in 2024
./jnexus.sh list maven-releases \
    --created-after 2024-01-01T00:00:00Z \
    --created-before 2024-12-31T23:59:59Z

# List with full metadata (content type, format, dates, checksum)
./jnexus.sh list maven-releases --show-metadata

# Combine multiple filters
./jnexus.sh list maven-releases ".*SNAPSHOT.*" \
    --min-size 100000 \
    --extension .war \
    --show-metadata
```

**Output Format:**
```
ID                              File Size        Path
================================ ===============  ============================
com.example:app:1.0-SNAPSHOT        1,024,567    com/example/app/1.0-SNAPSHOT/app-1.0-SNAPSHOT.jar
com.example:lib:2.0                 2,048,123    com/example/lib/2.0/lib-2.0.jar
================================ ===============  ============================
TOTAL: 2 components                 3,072,690    (2.93 MB)
```

### delete

Delete components from a Nexus repository with safety features.

**Syntax:**
```
jnexus delete <repository> [regex] [options]
```

**Arguments:**
| Argument | Required | Description |
|----------|----------|-------------|
| `repository` | Yes | Name of the Nexus repository |
| `regex` | No | Regex pattern to filter component paths |

**Options:**
| Option | Short | Description |
|--------|-------|-------------|
| `--dry-run` | `-n` | Preview deletions without executing |
| `--yes` | `-y` | Skip confirmation prompt |
| `--export-before-delete FILE` | -- | Export component list to a JSON file before deleting |

**Examples:**
```bash
# ALWAYS preview first with dry-run
./jnexus.sh delete --dry-run maven-snapshots ".*SNAPSHOT.*"

# Delete with confirmation prompt
./jnexus.sh delete maven-snapshots ".*-1.0.*-SNAPSHOT.*"

# Delete without confirmation (use with caution)
./jnexus.sh delete --yes maven-snapshots ".*old-version.*"

# Delete all components in a repository (dangerous)
./jnexus.sh delete --yes old-repository

# Export before deleting (for audit trail)
./jnexus.sh delete --yes --export-before-delete backup.json maven-snapshots ".*old.*"
```

**Safety features:**
1. **Dry-run mode** (`--dry-run`): Shows what would be deleted without deleting anything
2. **Confirmation prompt**: Asks "Are you sure?" before proceeding (unless `--yes` is passed)
3. **Fresh data**: Delete always fetches fresh data (never uses cache) to ensure accuracy
4. **Cache invalidation**: Cache is cleared after deletion to prevent stale data
5. **Export before delete** (`--export-before-delete`): Save a JSON record of components before they are removed

### stats

Display comprehensive repository statistics and analytics.

**Syntax:**
```
jnexus stats <repository> [options]
```

**Arguments:**
| Argument | Required | Description |
|----------|----------|-------------|
| `repository` | Yes | Name of the Nexus repository |

**Options:**
| Option | Description |
|--------|-------------|
| `--format FORMAT` | Output format: `text` (default) or `json` |

**Examples:**
```bash
# Text format (human-readable)
./jnexus.sh stats maven-releases

# JSON format (for scripting)
./jnexus.sh stats maven-releases --format json
```

**Text output includes:**
- Total components and total size (MB/GB)
- Average and median component size
- Size distribution across 5 buckets (<1MB, 1-10MB, 10-100MB, 100MB-1GB, >1GB)
- File type breakdown by extension
- Age distribution (last 7/30/90 days, older)
- Top 10 largest components

### history

View deletion history from the current session.

**Syntax:**
```
jnexus history [options]
```

**Options:**
| Option | Description |
|--------|-------------|
| `--export FILE` | Export deletion history to a JSON file |
| `--limit N` | Maximum number of records to display (default: all) |
| `--clear` | Clear the deletion history after displaying it |

**Examples:**
```bash
# View all deletions from this session
./jnexus.sh history

# Export history to a file
./jnexus.sh history --export deletions.json

# Show last 5 deletions
./jnexus.sh history --limit 5

# Display and then clear history
./jnexus.sh history --clear
```

## Global Options

These options apply to all commands and must be placed before the subcommand:

| Option | Short | Description |
|--------|-------|-------------|
| `--help` | `-h` | Show help message |
| `--version` | `-V` | Show version information |
| `--verbose` | `-v` | Enable debug logging |
| `--quiet` | `-q` | Only show warnings and errors |
| `--profile` | `-p` | Use a specific configuration profile |

**Examples:**
```bash
# Verbose output for debugging
./jnexus.sh --verbose list maven-releases

# Quiet mode (warnings and errors only)
./jnexus.sh --quiet delete --dry-run maven-snapshots

# Use production profile
./jnexus.sh --profile prod list maven-releases

# Combine global options
./jnexus.sh --profile dev --verbose list maven-snapshots
```

## Profiles

Profiles allow you to manage multiple Nexus environments (dev, staging, prod).

**Setup:**
```bash
# Create profile-specific files
cp ~/.flossware/nexus/nexus.properties ~/.flossware/nexus/nexus-dev.properties
cp ~/.flossware/nexus/nexus.properties ~/.flossware/nexus/nexus-prod.properties

# Edit each file with environment-specific credentials
```

**Usage:**
```bash
# Via CLI flag
./jnexus.sh --profile dev list maven-snapshots
./jnexus.sh --profile prod list maven-releases

# Via environment variable
export NEXUS_PROFILE=dev
./jnexus.sh list maven-snapshots
```

See [Configuration Guide](../advanced/configuration-guide.md) for full details.

## Environment Variables

| Variable | Description | Overrides |
|----------|-------------|-----------|
| `NEXUS_URL` | Nexus server URL | `nexus.url` property |
| `NEXUS_USER` | Username | `nexus.user` property |
| `NEXUS_PASSWORD` | Password or token | `nexus.password` property |
| `NEXUS_PROFILE` | Configuration profile name | `--profile` flag |
| `NEXUS_HTTP_TIMEOUT` | HTTP timeout in seconds | `nexus.http.timeout.seconds` property |

Environment variables take precedence over properties file values.

## Scripting Examples

### Bash: Clean Old Snapshots

```bash
#!/bin/bash
# clean-snapshots.sh -- Remove SNAPSHOT artifacts older than 90 days

REPO="maven-snapshots"
CUTOFF=$(date -u -d '90 days ago' +%Y-%m-%dT%H:%M:%SZ)

echo "Cleaning snapshots older than $CUTOFF from $REPO..."

# Preview first
./jnexus.sh delete --dry-run "$REPO" ".*SNAPSHOT.*"

read -p "Proceed with deletion? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    ./jnexus.sh delete --yes "$REPO" ".*SNAPSHOT.*"
fi
```

### Bash: Repository Size Report

```bash
#!/bin/bash
# repo-report.sh -- Generate size reports for all repositories

REPOS="maven-releases maven-snapshots npm-public docker-hosted"

for repo in $REPOS; do
    echo "=== $repo ==="
    ./jnexus.sh stats "$repo" --format json
    echo
done
```

### Bash: Find Large Artifacts Across Repositories

```bash
#!/bin/bash
# find-large.sh -- Find artifacts over 100MB

REPOS="maven-releases maven-snapshots"
MIN_SIZE=104857600  # 100 MB

for repo in $REPOS; do
    echo "--- $repo ---"
    ./jnexus.sh list "$repo" --min-size $MIN_SIZE --show-metadata
done
```

### CI/CD Integration (GitHub Actions)

```yaml
name: Nexus Cleanup
on:
  schedule:
    - cron: '0 2 * * 0'  # Weekly at 2 AM Sunday

jobs:
  cleanup:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
      - run: ./mvnw clean package -DskipTests
      - run: |
          ./jnexus.sh delete --yes maven-snapshots ".*SNAPSHOT.*"
        env:
          NEXUS_URL: ${{ secrets.NEXUS_URL }}
          NEXUS_USER: ${{ secrets.NEXUS_USER }}
          NEXUS_PASSWORD: ${{ secrets.NEXUS_PASSWORD }}
```

## Caching Behavior

The CLI uses intelligent caching to reduce Nexus server load:

- **list** command: Uses cached data (5-minute TTL). Subsequent calls within 5 minutes return cached results instantly.
- **delete** command: Always fetches fresh data. Cache is cleared after deletion.
- **stats** command: Uses cached data for component listing, then computes statistics.

To force a fresh fetch with `list`, there is no explicit flag -- use the Swing GUI's "Refresh" button or wait for the cache TTL to expire.

## Troubleshooting

See [Troubleshooting Guide](../advanced/troubleshooting.md) for common errors and solutions.

**Quick fixes:**

| Error | Solution |
|-------|----------|
| `Could not find or load main class` | Run `./mvnw clean package` |
| `Connection refused` | Verify `NEXUS_URL` is correct and accessible |
| `401 Unauthorized` | Check username and password/token |
| `No repository specified` | Add repository name as first argument |

## Tips and Tricks

1. **Always dry-run first**: Before any delete operation, use `--dry-run` to preview
2. **Use profiles**: Keep dev/prod credentials separate with `--profile`
3. **Pipe to other tools**: JSON output from `stats --format json` works well with `jq`
4. **Regex tips**: Use `.*` for wildcards, `\.` for literal dots, `.*SNAPSHOT.*` for SNAPSHOTs
5. **Size values**: Remember sizes are in bytes (1 MB = 1048576, 100 MB = 104857600)
6. **Date format**: Use ISO 8601 with timezone: `2024-01-01T00:00:00Z`

## Video Tutorials

These video tutorials cover CLI workflows in detail:

- [Getting Started with JNexus CLI](../video-tutorials/scripts/02-getting-started-cli.md) (5 min) -- Installation, configuration, and basic commands
- [Safe Deletion with Dry-Run Mode](../video-tutorials/scripts/04-safe-deletion-dry-run.md) (3 min) -- CLI dry-run demo and safety features
- [Advanced Filtering and Search](../video-tutorials/scripts/05-advanced-filtering.md) (4 min) -- Regex, size, date, and extension filters
- [Repository Statistics and Analytics](../video-tutorials/scripts/06-repository-statistics.md) (5 min) -- Stats command deep dive
- [Multi-Profile Configuration](../video-tutorials/scripts/09-multi-profile-config.md) (3 min) -- Managing multiple environments

See the [Video Tutorials catalog](../video-tutorials/README.md) for all available tutorials.

Back to [Getting Started](../getting-started.md) | [All Guides](../getting-started.md#platform-guides)

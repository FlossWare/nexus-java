# JNexus Best Practices

Back to [Getting Started](../getting-started.md) | [Configuration](configuration-guide.md) | [Troubleshooting](troubleshooting.md)

## Security Best Practices

### Credential Management

1. **Use HTTPS exclusively**: Never configure `nexus.url` with `http://` -- credentials are sent as Base64-encoded Basic Auth headers and would be visible on the network.

2. **Use Nexus user tokens instead of passwords**: Generate a token in Nexus UI > Profile > User Token. Tokens can be revoked independently and do not expose your main password.

3. **Use environment variables in CI/CD**:
   ```bash
   # GitHub Actions
   env:
     NEXUS_URL: ${{ secrets.NEXUS_URL }}
     NEXUS_USER: ${{ secrets.NEXUS_USER }}
     NEXUS_PASSWORD: ${{ secrets.NEXUS_PASSWORD }}
   ```

4. **Protect properties files**:
   ```bash
   chmod 600 ~/.flossware/nexus/nexus.properties
   ```

5. **Never commit credentials**: Add to `.gitignore`:
   ```
   .flossware/
   nexus.properties
   nexus-*.properties
   ```

6. **Use minimal permissions**: Create dedicated Nexus users with only the permissions needed (browse for listing, delete for cleanup).

7. **Rotate credentials regularly**: Change passwords and regenerate user tokens periodically.

### Deletion Safety

1. **Always dry-run first**:
   ```bash
   ./jnexus.sh delete --dry-run maven-snapshots ".*SNAPSHOT.*"
   # Review output carefully
   ./jnexus.sh delete maven-snapshots ".*SNAPSHOT.*"
   ```

2. **Use specific regex patterns**: Avoid overly broad patterns. `.*` matches everything.

3. **Start narrow, widen gradually**:
   ```bash
   # Start specific
   ./jnexus.sh delete --dry-run maven-snapshots ".*com/mycompany/old-app/1\.0-SNAPSHOT.*"
   
   # Then widen if needed
   ./jnexus.sh delete --dry-run maven-snapshots ".*com/mycompany/.*/.*-SNAPSHOT.*"
   ```

4. **Delete operations are permanent**: There is no undo. The Nexus API does not support undelete.

5. **Review deletion history**: Use the `history` command to see what was deleted in the current session.

## Performance Best Practices

### Caching

1. **Use List (cached) for browsing**: The List button uses the 5-minute cache for fast response. Only use Refresh when you need current data.

2. **Understand cache behavior**:
   - `List`: Uses cache (fast after first fetch)
   - `Refresh`: Bypasses cache (always hits server)
   - `Delete`: Always fresh, clears cache after

3. **Multiple queries, same repository**: After the first List or Refresh, subsequent queries with different filters are nearly instant because filtering is done client-side on cached data.

### Large Repositories

1. **Use filters to narrow results**: Listing 50,000 components is slower than listing the 200 that match your regex.

2. **Increase timeout for large repos**:
   ```properties
   nexus.http.timeout.seconds=120
   ```

3. **JSON format for scripting**: `--format json` is more efficient to parse programmatically than text.

## Operational Best Practices

### Profile Management

1. **Separate environments**: Use profiles to keep dev/staging/prod credentials separate:
   ```bash
   ~/.flossware/nexus/nexus-dev.properties
   ~/.flossware/nexus/nexus-prod.properties
   ```

2. **Default to dry-run in production**: Set `nexus.default.dryrun=true` in production profiles to prevent accidental deletions.

3. **Use environment variable for CI/CD profiles**:
   ```bash
   export NEXUS_PROFILE=prod
   ```

### Automation and Scripting

1. **Use `--yes` flag carefully**: This skips the confirmation prompt. Only use in scripts where you have already validated the operation.

2. **Log and capture output**:
   ```bash
   ./jnexus.sh delete --yes maven-snapshots ".*old.*" 2>&1 | tee cleanup.log
   ```

3. **Use JSON for programmatic consumption**:
   ```bash
   ./jnexus.sh stats maven-releases --format json | jq '.totalComponents'
   ```

4. **Schedule regular cleanup**: Use cron or CI/CD schedules for automated cleanup:
   ```bash
   # Weekly SNAPSHOT cleanup
   0 2 * * 0 /path/to/jnexus.sh delete --yes maven-snapshots ".*SNAPSHOT.*"
   ```

5. **Monitor with stats**: Track repository growth over time:
   ```bash
   DATE=$(date +%Y-%m-%d)
   ./jnexus.sh stats maven-releases --format json > "/var/log/nexus-stats/$DATE.json"
   ```

### Repository Management

1. **Separate release and snapshot repositories**: Different retention policies for different repository types.

2. **Clean snapshots aggressively, releases carefully**: Snapshots are meant to be temporary; releases should be kept longer.

3. **Review statistics before cleanup**: Use `stats` to understand what is consuming space before deleting.

4. **Test on non-production first**: Always validate your regex patterns and filters on a development Nexus before running against production.

## Interface Selection Guide

Choose the right interface for the job:

| Task | Recommended Interface |
|------|----------------------|
| Automated cleanup script | CLI with `--yes` flag |
| Exploring a repository | Swing GUI (sortable table, statistics) |
| Quick list over SSH | Terminal UI or CLI |
| CI/CD pipeline | CLI with environment variables |
| Team member without Java | Android or iOS/macOS app |
| Remote desktop session | AWT GUI (better VNC rendering) |
| Generating reports | CLI with `--format json` |
| Reviewing large repos | Swing GUI (column sorting, filtering) |

## Common Patterns

### Cleanup Old Snapshots

```bash
#!/bin/bash
# Run weekly to clean snapshots older than 90 days
CUTOFF=$(date -u -d '90 days ago' +%Y-%m-%dT%H:%M:%SZ 2>/dev/null || \
         date -u -v-90d +%Y-%m-%dT%H:%M:%SZ)

echo "Cleaning snapshots older than $CUTOFF..."
./jnexus.sh delete --dry-run maven-snapshots ".*SNAPSHOT.*"

echo "Proceed? (y/n)"
read -r answer
if [ "$answer" = "y" ]; then
    ./jnexus.sh delete --yes maven-snapshots ".*SNAPSHOT.*"
fi
```

### Multi-Repository Report

```bash
#!/bin/bash
echo "Repository Size Report - $(date)"
echo "================================"

for repo in maven-releases maven-snapshots npm-public docker-hosted; do
    echo ""
    echo "--- $repo ---"
    ./jnexus.sh stats "$repo" 2>/dev/null || echo "  (not accessible)"
done
```

### Pre-Release Cleanup

```bash
#!/bin/bash
# Clean up before a major release to free space

# 1. Identify large artifacts
echo "=== Large Artifacts (>100MB) ==="
./jnexus.sh list maven-releases --min-size 104857600

# 2. Check overall statistics
echo ""
echo "=== Repository Statistics ==="
./jnexus.sh stats maven-releases

# 3. Preview snapshot cleanup
echo ""
echo "=== Snapshot Cleanup Preview ==="
./jnexus.sh delete --dry-run maven-snapshots ".*SNAPSHOT.*"
```

## Video Tutorials

For visual demonstrations of best practices, see:

- [Safe Deletion with Dry-Run Mode](../video-tutorials/scripts/04-safe-deletion-dry-run.md) (3 min) -- Demonstrates safe deletion workflow
- [Multi-Profile Configuration](../video-tutorials/scripts/09-multi-profile-config.md) (3 min) -- Environment separation best practices

Back to [Getting Started](../getting-started.md) | [Configuration](configuration-guide.md)

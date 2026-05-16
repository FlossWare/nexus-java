# GitHub Actions Workflows

This directory contains GitHub Actions workflows for CI/CD automation.

## Available Workflows

### main.yml - CD-CI Pipeline

**Trigger:** Push to `main` branch

**What it does:**
1. ✅ Sets up Java 21 environment
2. ✅ Configures Maven settings for PackageCloud deployment
3. ✅ Increments version automatically (X.Y format)
4. ✅ Builds and tests the project
5. ✅ Deploys to PackageCloud
6. ✅ Creates git tag and pushes back to repository

**Workflow Steps:**
```
Setup → Configure → Version Bump → Build → Test → Deploy → Tag → Push
```

**Requirements:**
- Repository secret: `PACKAGECLOUD_TOKEN`
- Auto-provided: `GITHUB_TOKEN`

**Skip Condition:**
- Skips if pusher email is `version-bump@flossware.org`
- This prevents infinite CI loops

## Viewing Workflow Runs

1. Go to repository → **Actions** tab
2. Click on workflow run to see details
3. View logs for each step

## Testing Locally

You can test workflow steps using [act](https://github.com/nektos/act):

```bash
# Install act
brew install act  # macOS
# or follow instructions at https://github.com/nektos/act

# Test the workflow
act push
```

## Troubleshooting

### Workflow doesn't trigger
- Check if branch is `main`
- Verify commit doesn't have `[ci skip]`
- Check pusher email isn't `version-bump@flossware.org`

### Build fails
- Check Java 21 compatibility
- Review test failures in logs
- Verify dependencies are available

### Deploy fails
- Verify `PACKAGECLOUD_TOKEN` secret is set correctly
- Check token hasn't expired
- Verify packagecloud.io is accessible

### Can't push tag back
- Check if branch is protected
- Verify `GITHUB_TOKEN` has write access
- Review repository settings

## Related Documentation

- [CI-CD.md](../../CI-CD.md) - Complete CI/CD documentation
- [VERSIONING.md](../../VERSIONING.md) - Version management guide
- [ci/README.md](../../ci/README.md) - Manual release scripts

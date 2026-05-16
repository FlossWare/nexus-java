# CI/CD Scripts

This directory contains scripts for automating the release process.

## Scripts

### rev-version.sh

Automates version bumping, tagging, and releasing.

**Usage:**
```bash
./ci/rev-version.sh
```

**What it does:**
1. Reads current version from `pom.xml` (e.g., `1.0`)
2. Increments minor version (e.g., `1.0` → `1.1`)
3. Updates `pom.xml` using Maven versions plugin
4. Commits the change with message: `chore: bump version to 1.1 [ci skip]`
5. Creates git tag: `v1.1`
6. Pushes commit and tag to GitHub

**Requirements:**
- Git repository with remote `origin` configured
- Maven installed and in PATH
- Git user configured (or will use "FlossWare CI" defaults)
- Push permissions to the GitHub repository

**Notes:**
- The `[ci skip]` tag prevents CI/CD from triggering on version bump commits
- Only increments **minor** version (Y in X.Y)
- For **major** version bumps (X in X.Y), manually edit `pom.xml` first

## Version Format

This project uses **X.Y** versioning:

| Component | Description | Example |
|-----------|-------------|---------|
| X (Major) | Incompatible API changes, major features | 1.x → 2.x |
| Y (Minor) | New features, bug fixes, improvements | 1.0 → 1.1 |

**Valid versions:** `1.0`, `1.1`, `2.0`, `2.5`  
**Invalid versions:** `1.0.0`, `1.0-SNAPSHOT`, `1.0.0-RELEASE`

The Maven enforcer plugin validates this format during build.

## Manual Release Process

If you need to perform the release steps manually:

### 1. Determine Next Version

```bash
# Get current version
CURRENT=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
echo "Current version: $CURRENT"

# Calculate next version
MAJOR=$(echo $CURRENT | cut -d. -f1)
MINOR=$(echo $CURRENT | cut -d. -f2)
NEXT_MINOR=$((MINOR + 1))
NEXT="${MAJOR}.${NEXT_MINOR}"
echo "Next version: $NEXT"
```

### 2. Update Version

```bash
# Update pom.xml
mvn versions:set -DnewVersion="$NEXT" -DgenerateBackupPoms=false

# Verify change
git diff pom.xml
```

### 3. Commit and Tag

```bash
# Commit
git add pom.xml
git commit -m "chore: bump version to $NEXT [ci skip]"

# Tag
git tag -a "v$NEXT" -m "Release version $NEXT"
```

### 4. Push

```bash
# Push commit
git push origin main  # or your branch name

# Push tag
git push origin "v$NEXT"
```

## Major Version Bump

To bump the major version (e.g., 1.9 → 2.0):

```bash
# Manually edit pom.xml to set version to 2.0
vim pom.xml  # Change <version>1.9</version> to <version>2.0</version>

# Then run the script to commit, tag, and push
./ci/rev-version.sh  # This will bump to 2.1 after your manual 2.0

# OR do it all manually:
git add pom.xml
git commit -m "chore: bump major version to 2.0 [ci skip]"
git tag -a "v2.0" -m "Release version 2.0"
git push origin main
git push origin v2.0
```

## Troubleshooting

### Error: "Working tree has modifications"

The script requires a clean working tree. Commit or stash your changes:

```bash
git status
git add .
git commit -m "Your changes"
# Then run rev-version.sh
```

### Error: "Permission denied"

Ensure you have push permissions to the repository:

```bash
git remote -v  # Check your remote URL
git config --list | grep user  # Check your git identity
```

### Error: "tag already exists"

A tag with that version already exists. Either:
1. Delete the tag: `git tag -d v1.1 && git push origin :refs/tags/v1.1`
2. Or skip that version and increment again

### Testing the Script Locally

To test without pushing:

```bash
# Edit rev-version.sh and comment out the push lines:
# git push origin "${CURRENT_BRANCH}"
# git push origin "v${NEXT_VERSION}"

# Run the script
./ci/rev-version.sh

# Check the results
git log -1
git tag -l

# To undo (before pushing):
git reset --hard HEAD~1
git tag -d v1.1
```

## CI/CD Integration

These scripts can be integrated into GitHub Actions, GitLab CI, or other CI/CD systems:

**GitHub Actions Example:**

```yaml
name: Release
on:
  workflow_dispatch:
    
jobs:
  release:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'
      - name: Release
        run: ./ci/rev-version.sh
```

## Related Documentation

- [CHANGELOG.md](../CHANGELOG.md) - Release history
- [CONTRIBUTING.md](../CONTRIBUTING.md) - Contribution guidelines
- [README.md](../README.md) - Project overview

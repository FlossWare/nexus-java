# JNexus CLI - Versioning Guide

## Version Format: X.Y

This project uses **X.Y** versioning (similar to jcollections):

| Version | Type | When to Use | Example |
|---------|------|-------------|---------|
| **X** (Major) | Breaking changes | Incompatible API changes, major rewrites | 1.x → 2.0 |
| **Y** (Minor) | Features & Fixes | New features, bug fixes, improvements | 1.0 → 1.1 |

**Current Version:** 1.0

## Automated Versioning

### Quick Release

```bash
./ci/rev-version.sh
```

This script:
1. ✅ Reads current version from `pom.xml` (e.g., `1.0`)
2. ✅ Increments minor version automatically (`1.0` → `1.1`)
3. ✅ Updates `pom.xml` with new version
4. ✅ Commits: `chore: bump version to 1.1 [ci skip]`
5. ✅ Creates git tag: `v1.1`
6. ✅ Pushes commit and tag to GitHub

### Version Validation

The Maven enforcer plugin validates version format:

```xml
<evaluateBeanshell>
    <condition>
        String v = "${project.version}";
        v.matches("^\\d+\\.\\d+$")
    </condition>
    <message>VERSION ERROR: Version must be X.Y format and NOT a SNAPSHOT.</message>
</evaluateBeanshell>
```

**Valid:** `1.0`, `1.1`, `2.0`, `10.5` ✅  
**Invalid:** `1.0.0`, `1.0-SNAPSHOT`, `1.0.0-RELEASE` ❌

## Manual Versioning

### Bump Minor Version (1.0 → 1.1)

```bash
mvn versions:set -DnewVersion="1.1" -DgenerateBackupPoms=false
git add pom.xml
git commit -m "chore: bump version to 1.1 [ci skip]"
git tag -a "v1.1" -m "Release version 1.1"
git push origin main
git push origin v1.1
```

### Bump Major Version (1.9 → 2.0)

```bash
mvn versions:set -DnewVersion="2.0" -DgenerateBackupPoms=false
git add pom.xml
git commit -m "chore: bump major version to 2.0 [ci skip]"
git tag -a "v2.0" -m "Release version 2.0 - Major Update"
git push origin main
git push origin v2.0
```

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-05-15 | Initial release - Java 21, removed Spring Boot |

## POM Configuration

### Version Declaration
```xml
<groupId>org.flossware</groupId>
<artifactId>nexus</artifactId>
<version>1.0</version>
```

### SCM Configuration
```xml
<scm>
    <connection>scm:git:https://github.com/FlossWare/jnexus.git</connection>
    <developerConnection>scm:git:https://github.com/FlossWare/jnexus.git</developerConnection>
    <url>https://github.com/FlossWare/jnexus</url>
</scm>
```

### Distribution Management
```xml
<distributionManagement>
    <repository>
        <id>packagecloud-flossware</id>
        <name>packagecloud-flossware</name>
        <url>https://packagecloud.io/flossware/java/maven2/</url>
    </repository>
</distributionManagement>
```

## Maven Plugins

### Versions Plugin
```bash
# Set version
mvn versions:set -DnewVersion="1.1"

# Display current version
mvn help:evaluate -Dexpression=project.version -q -DforceStdout
```

### Enforcer Plugin
Runs automatically during `mvn validate` and `mvn package`

### SCM Plugin
```bash
# Create tag
mvn scm:tag -Dtag=v1.0

# Check status
mvn scm:status
```

## Testing Version Changes

### Test Invalid Version (Should Fail)
```bash
mvn versions:set -DnewVersion="1.0.0-SNAPSHOT"
mvn validate
# Expected: BUILD FAILURE with "VERSION ERROR"
```

### Test Valid Version (Should Pass)
```bash
mvn versions:set -DnewVersion="1.1"
mvn validate
# Expected: BUILD SUCCESS
```

### Restore Version
```bash
mvn versions:set -DnewVersion="1.0"
```

## Git Tags

### List All Tags
```bash
git tag -l
```

### View Tag Details
```bash
git show v1.0
```

### Delete Tag (Local and Remote)
```bash
# Local
git tag -d v1.0

# Remote
git push origin :refs/tags/v1.0
```

## CI/CD Integration

This project includes complete CI/CD configurations for both platforms:

- ✅ **GitHub Actions**: `.github/workflows/main.yml`
- ✅ **GitLab CI**: `.gitlab-ci.yml`

Both configurations automatically:
- Build and test on push to `main`
- Increment version (X.Y format)
- Deploy to PackageCloud
- Create and push git tags

### GitHub Actions (Fully Automated)

Runs automatically on push to `main`:

```yaml
# .github/workflows/main.yml
on:
  push:
    branches: [ main ]

# Automatically:
# 1. Builds and tests
# 2. Increments version (1.0 → 1.1)
# 3. Deploys to PackageCloud
# 4. Creates tag and pushes back
```

### GitLab CI (Manual Control)

Build/test automatic, deploy/release manual:

```yaml
# .gitlab-ci.yml
stages:
  - build   # Automatic
  - test    # Automatic
  - deploy  # Manual job
  - release # Manual job
```

**For complete CI/CD documentation, see [CI-CD.md](CI-CD.md)**

## Troubleshooting

### "Working tree has modifications"
```bash
git status
git stash  # or commit changes
./ci/rev-version.sh
```

### "Permission denied"
```bash
# Make script executable
chmod +x ci/rev-version.sh
```

### "Tag already exists"
```bash
# Delete and recreate
git tag -d v1.1
git push origin :refs/tags/v1.1
./ci/rev-version.sh
```

## Related Files

- `pom.xml` - Version declaration
- `ci/rev-version.sh` - Automated release script
- `ci/README.md` - Detailed CI/CD documentation
- `CHANGELOG.md` - Version history
- `README.md` - Usage instructions

## Questions?

See [CONTRIBUTING.md](CONTRIBUTING.md) for contribution guidelines.

# CI/CD Configuration

This project supports both **GitHub Actions** and **GitLab CI** for continuous integration and deployment.

## Overview

### Desktop (Maven/Java)

| Platform | Config File | Status |
|----------|-------------|--------|
| GitHub Actions | `.github/workflows/main.yml` | ✅ Configured |
| GitLab CI | `.gitlab-ci.yml` | ✅ Configured |

Desktop workflows provide:
- ✅ Automated building and testing on push to main
- ✅ Automated version bumping (X.Y format)
- ✅ Deployment to PackageCloud
- ✅ Git tagging with version numbers
- ✅ Skip CI loops (via `[ci skip]` in commit messages)
- ✅ Path filtering (only runs when desktop files change)

### Android (Gradle/Kotlin)

| Platform | Config File | Status |
|----------|-------------|--------|
| GitHub Actions | `.github/workflows/android.yml` | ✅ Configured (CI) |
| GitHub Actions | `.github/workflows/android-release.yml` | ✅ Configured (Release) |

Android workflows provide:
- ✅ Build debug and release APKs on push to main
- ✅ Run unit tests (jnexus-core + jnexus-android)
- ✅ Upload APK artifacts (30-90 day retention)
- ✅ Create GitHub Releases on version tags
- ✅ Signed debug APKs for easy installation

## GitHub Actions

### Configuration

**File:** `.github/workflows/main.yml`

**Triggers:** Push to `main` branch (excluding version bump commits)

**Workflow:**
1. ✅ Set up Java 21
2. ✅ Configure Maven settings for PackageCloud
3. ✅ Increment version (1.0 → 1.1)
4. ✅ Build and run tests
5. ✅ Deploy to PackageCloud
6. ✅ Commit version change and create git tag
7. ✅ Push changes back to repository

### Required Secrets

Configure these in GitHub Settings → Secrets and variables → Actions:

| Secret Name | Description | Example |
|-------------|-------------|---------|
| `PACKAGECLOUD_TOKEN` | PackageCloud API token | `abc123...` |
| `GITHUB_TOKEN` | Auto-provided by GitHub | (automatic) |

### Manual Triggering

The workflow runs automatically on push to main, but you can also trigger it manually:

```bash
# Push to main triggers the workflow
git push origin main
```

### Viewing Workflow Runs

1. Go to repository on GitHub
2. Click "Actions" tab
3. View workflow runs and logs

### Local Testing

Test the workflow steps locally:

```bash
# Set up Java 21
java -version

# Increment version
mvn -U build-helper:parse-version versions:set -DnewVersion=\${parsedVersion.majorVersion}.\${parsedVersion.nextMinorVersion} versions:commit

# Build and test
mvn clean install

# View new version
mvn help:evaluate -Dexpression=project.version -q -DforceStdout
```

## Android Workflows (GitHub Actions)

### Android CI Workflow

**File:** `.github/workflows/android.yml`

**Triggers:** Push to `main` branch

**Workflow:**
1. ✅ Set up JDK 21
2. ✅ Build jnexus-core (shared library)
3. ✅ Run jnexus-core unit tests
4. ✅ Build Android Debug APK
5. ✅ Build Android Release APK
6. ✅ Run Android unit tests
7. ✅ Upload APKs as artifacts (30-90 day retention)

**APK Artifacts:**
- **Debug APK**: `jnexus-android-debug` (signed with debug keystore)
- **Release APK**: `jnexus-android-release` (unsigned, requires signing)

**Download artifacts:**
```bash
# Via GitHub CLI
gh run download <run-id> -n jnexus-android-debug

# Or from web: Actions → Workflow run → Artifacts section
```

### Android Release Workflow

**File:** `.github/workflows/android-release.yml`

**Triggers:** Git tags matching `v*` (e.g., `v1.2.1`)

**Workflow:**
1. ✅ Build Android Debug APK (signed)
2. ✅ Rename APK with version number
3. ✅ Create GitHub Release
4. ✅ Attach APK to release

**Creating a release:**
```bash
# Tag and push
git tag -a v1.2.1 -m "Release v1.2.1: Description"
git push github v1.2.1

# Release appears at: https://github.com/FlossWare/jnexus/releases
```

### Local Testing (Android)

```bash
# Build debug APK
./gradlew :jnexus-android:assembleDebug

# Build release APK
./gradlew :jnexus-android:assembleRelease

# Run tests
./gradlew :jnexus-android:testDebugUnitTest

# Install on device
adb install jnexus-android/build/outputs/apk/debug/jnexus-android-debug.apk
```

## GitLab CI

### Configuration

**File:** `.gitlab-ci.yml`

**Triggers:** Push to `main` branch (excluding version bump commits)

**Stages:**
1. **build** - Compile source code
2. **test** - Run unit tests with JUnit reporting
3. **deploy** - Deploy to PackageCloud (manual)
4. **release** - Bump version, tag, and push (manual)

**Pipeline:**
```
build → test → package
              ↓
          deploy (manual)
              ↓
          release (manual)
```

### Required Variables

Configure these in GitLab Settings → CI/CD → Variables:

| Variable Name | Description | Protected | Masked |
|--------------|-------------|-----------|--------|
| `PACKAGECLOUD_TOKEN` | PackageCloud API token | ✅ | ✅ |
| `CI_PUSH_TOKEN` | GitLab access token with write permissions | ✅ | ✅ |

### Creating CI_PUSH_TOKEN

1. Go to GitLab → Settings → Access Tokens
2. Create token with:
   - Name: `CI Pipeline Token`
   - Scopes: `write_repository`, `api`
   - Expiration: Set appropriate date
3. Copy token and add to CI/CD variables

### Manual Jobs

The `deploy` and `release` jobs require manual triggering:

**To deploy:**
1. Go to CI/CD → Pipelines
2. Find the pipeline run
3. Click "deploy" job
4. Click "Play" button

**To release:**
1. Go to CI/CD → Pipelines
2. Find the pipeline run
3. Click "release" job
4. Click "Play" button

### Viewing Pipeline Runs

1. Go to repository on GitLab
2. Click "CI/CD" → "Pipelines"
3. View pipeline runs and logs

### Test Reports

GitLab CI generates JUnit test reports visible in:
- Merge Request → Tests tab
- Pipeline → Tests tab

### Local Testing with GitLab Runner

```bash
# Install GitLab Runner locally
# See: https://docs.gitlab.com/runner/install/

# Run pipeline locally
gitlab-runner exec docker build
gitlab-runner exec docker test
```

## Comparison: GitHub Actions vs GitLab CI

| Feature | GitHub Actions | GitLab CI |
|---------|---------------|-----------|
| Automatic on push | ✅ Yes | ✅ Yes |
| Version bumping | ✅ Automatic | ✅ Manual job |
| Deployment | ✅ Automatic | ✅ Manual job |
| Test reporting | Basic | ✅ JUnit integration |
| Cache | ✅ Implicit | ✅ Explicit config |
| Artifacts | ✅ Actions | ✅ Artifacts system |
| Secrets | GitHub Secrets | GitLab Variables |

## Common Workflows

### Making a Release (GitHub)

```bash
# Make your changes
git add .
git commit -m "feat: add new feature"

# Push to main - triggers automatic build, test, version bump, deploy
git push origin main

# Check GitHub Actions for status
```

### Making a Release (GitLab)

```bash
# Make your changes
git add .
git commit -m "feat: add new feature"

# Push to main - triggers automatic build and test
git push origin main

# Go to GitLab → CI/CD → Pipelines
# Manually trigger "deploy" job
# Manually trigger "release" job
```

### Manual Version Bumping (Both)

If you prefer manual control:

```bash
# Use the script
./ci/rev-version.sh

# Or manually
mvn versions:set -DnewVersion="1.1" -DgenerateBackupPoms=false
git add pom.xml
git commit -m "chore: bump version to 1.1 [ci skip]"
git tag -a "v1.1" -m "Release version 1.1"
git push origin main
git push origin v1.1
```

## Skipping CI

Add `[ci skip]` or `[skip ci]` to commit messages:

```bash
git commit -m "docs: update README [ci skip]"
```

This prevents:
- GitHub Actions workflow from running
- GitLab pipeline from starting

## Troubleshooting

### GitHub Actions Issues

**Problem:** Workflow doesn't run
- Check if pusher email is `version-bump@flossware.org`
- Check if commit message contains `[ci skip]`
- Verify branch is `main`

**Problem:** Deployment fails
- Verify `PACKAGECLOUD_TOKEN` secret is set
- Check PackageCloud token hasn't expired
- Check Maven settings configuration

**Problem:** Can't push tag back
- Verify `GITHUB_TOKEN` has write permissions
- Check branch protection rules

### GitLab CI Issues

**Problem:** Pipeline doesn't start
- Check if branch is `main`
- Check if pusher email is `version-bump@flossware.org`
- Verify `.gitlab-ci.yml` is valid (CI Lint)

**Problem:** Release job can't push
- Verify `CI_PUSH_TOKEN` variable is set
- Check token has `write_repository` scope
- Check token hasn't expired

**Problem:** Docker image issues
- Verify runner has Docker executor
- Check image `maven:3.9-eclipse-temurin-21` is available
- Try pulling image manually

## Testing CI Configuration

### Validate GitHub Actions Workflow

```bash
# Install act (GitHub Actions local runner)
# https://github.com/nektos/act

# Test workflow
act push
```

### Validate GitLab CI Configuration

```bash
# Use GitLab CI Lint
# Project → CI/CD → Pipelines → CI Lint

# Or use GitLab CLI
glab ci lint
```

## Best Practices

1. **Test Locally First**
   - Run `mvn clean install` before pushing
   - Test version script: `./ci/rev-version.sh`

2. **Use Feature Branches**
   - Work in feature branches
   - Merge to `main` triggers CI/CD

3. **Review Before Deploy**
   - Check test results before deploying
   - Review version bump in pipeline logs

4. **Monitor Pipelines**
   - Watch for failures
   - Fix issues promptly

5. **Secure Secrets**
   - Never commit tokens
   - Rotate tokens regularly
   - Use masked variables

## Migration Between Platforms

### From GitHub to GitLab

1. Push repository to GitLab
2. Configure CI/CD variables in GitLab
3. `.gitlab-ci.yml` already present
4. Test pipeline run

### From GitLab to GitHub

1. Push repository to GitHub
2. Configure secrets in GitHub
3. `.github/workflows/main.yml` already present
4. Test workflow run

## Related Files

- `.github/workflows/main.yml` - GitHub Actions workflow
- `.gitlab-ci.yml` - GitLab CI configuration
- `ci/rev-version.sh` - Manual version bump script
- `pom.xml` - Maven POM with SCM config
- `VERSIONING.md` - Version format documentation

## Questions?

See [CONTRIBUTING.md](CONTRIBUTING.md) for contribution guidelines.

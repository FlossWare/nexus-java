# GitHub Actions Workflows

This directory contains GitHub Actions workflows for CI/CD automation.

## Available Workflows

### release.yml - Unified Release (All Platforms)

**Trigger:** Push of version tag matching `v*` (e.g., `v2.1.0`)

**What it does:**
1. Extracts version from tag and changelog from CHANGELOG.md
2. Validates version consistency across pom.xml and build.gradle
3. Builds all 4 platforms in parallel:
   - Desktop JAR (Java 21, Maven)
   - Android APK (Gradle, Kotlin) with optional release signing
   - iOS IPA (Xcode) -- requires Apple signing certificates
   - macOS DMG (Xcode) -- requires Apple signing certificates
4. Creates a unified GitHub Release with all artifacts
5. Deploys Desktop JAR to PackageCloud
6. Posts a release summary

**Workflow:**
```
prepare --> build-desktop  ──┐
        --> build-android  ──┤
        --> build-ios      ──├──> create-release --> notify
        --> build-macos    ──┘         │
                                       └──> deploy-packagecloud
```

**Requirements:**
- Repository secret: `PACKAGECLOUD_TOKEN`
- Auto-provided: `GITHUB_TOKEN`
- Optional: Android signing secrets (`ANDROID_SIGNING_KEY`, `ANDROID_KEY_ALIAS`, etc.)
- Optional: iOS signing secrets (`IOS_BUILD_CERTIFICATE`, `IOS_P12_PASSWORD`, etc.)

**Quick start:**
```bash
git tag v2.1.0
git push origin v2.1.0
# All platforms build automatically!
```

---

### main.yml - Desktop CD-CI Pipeline

**Trigger:** Push to `main` branch

**What it does:**
1. Sets up Java 21 environment
2. Configures Maven settings for PackageCloud deployment
3. Increments version automatically (X.Y format)
4. Builds and tests the project
5. Deploys to PackageCloud
6. Creates git tag and pushes back to repository

**Workflow Steps:**
```
Setup --> Configure --> Version Bump --> Build --> Test --> Deploy --> Tag --> Push
```

**Requirements:**
- Repository secret: `PACKAGECLOUD_TOKEN`
- Auto-provided: `GITHUB_TOKEN`

**Skip Condition:**
- Skips if pusher email is `version-bump@flossware.org`
- This prevents infinite CI loops

---

### android.yml - Android CI

**Trigger:** Push to `main` branch or pull requests to `main`

**What it does:**
1. Builds jnexus-core shared library
2. Runs jnexus-core unit tests
3. Builds debug and release Android APKs
4. Runs Android unit tests
5. Uploads APKs and test results as artifacts

---

### android-release.yml - Android Release (Superseded)

**Status:** Superseded by `release.yml`. Retained for backward compatibility. Can be triggered manually via `workflow_dispatch`.

---

### ios.yml - iOS CI

**Trigger:** Push to `main` branch (paths: `jnexus-ios/**`) or pull requests

**What it does:**
1. Builds iOS (simulator) and macOS targets
2. Runs iOS and macOS tests
3. Archives iOS app
4. Uploads test results

---

### quality-gate.yml - Maven Quality Gate

**Trigger:** Push/PR to `main`/`develop`, daily at 2 AM UTC

**What it does:**
1. Runs all Maven quality checks (JaCoCo, SpotBugs, PMD, Checkstyle, OWASP)
2. Comments quality metrics on PRs
3. Creates issues when quality gates fail

---

### dependency-scan.yml - Dependency Vulnerability Scan

**Trigger:** Push/PR to `main`, weekly Monday 6 AM UTC

**What it does:**
1. Runs OWASP Dependency-Check for Maven
2. Submits Gradle dependencies
3. Generates CycloneDX SBOM

---

### performance.yml - Performance Tests

**Trigger:** Push/PR to `main`/`develop`, daily at 3 AM UTC

**What it does:**
1. Runs JMH benchmarks
2. Runs load and stress tests
3. Comments performance results on PRs

---

### javadoc.yml - Publish Javadoc

**Trigger:** Push to `main` (Java source or pom.xml changes)

**What it does:**
1. Generates Javadoc for jnexus-core and desktop modules
2. Deploys to GitHub Pages

## Viewing Workflow Runs

1. Go to repository on GitHub
2. Click **Actions** tab
3. Click on workflow run to see details
4. View logs for each step

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

### iOS/macOS builds skipped
- Signing certificates are optional
- Without signing secrets, only simulator builds run
- IPA/DMG artifacts require Apple Developer account

## Related Documentation

- [CI-CD.md](../../CI-CD.md) - Complete CI/CD documentation
- [VERSIONING.md](../../VERSIONING.md) - Version management guide
- [ci/README.md](../../ci/README.md) - Manual release scripts

# CI/CD Configuration

This project supports both **GitHub Actions** and **GitLab CI** for continuous integration and deployment.

## Overview

### Unified Release (All Platforms)

| Platform | Config File | Status |
|----------|-------------|--------|
| GitHub Actions | `.github/workflows/release.yml` | ✅ **NEW** - Unified Release |

**Unified Release Workflow** provides:
- ✅ Single git tag triggers builds for all 4 platforms
- ✅ Parallel builds: Desktop JAR, Android APK, iOS IPA, macOS DMG
- ✅ Automatic GitHub Release with all artifacts
- ✅ PackageCloud deployment for Desktop JAR
- ✅ Version validation across all platforms
- ✅ Changelog extraction and release notes
- ✅ ~10-15 minute total build time

**Quick start:**
```bash
git tag v2.1.0
git push origin v2.1.0
# Workflow builds all platforms automatically!
```

See [RELEASE_PROCESS.md](RELEASE_PROCESS.md) for detailed instructions.

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
| GitHub Actions | `.github/workflows/android-release.yml` | ⚠️ Superseded by unified release.yml |

Android workflows provide:
- ✅ Build debug and release APKs on push to main
- ✅ Run unit tests (jnexus-core + jnexus-android)
- ✅ Upload APK artifacts (30-90 day retention)
- ✅ Create GitHub Releases on version tags (now via unified workflow)
- ✅ Signed debug APKs for easy installation

### iOS/macOS (Swift/Xcode)

| Platform | Config File | Status |
|----------|-------------|--------|
| GitHub Actions | `.github/workflows/ios.yml` | ✅ CI only |
| GitHub Actions | `.github/workflows/release.yml` | ✅ **NEW** - Release via unified workflow |

iOS/macOS workflows provide:
- ✅ Build iOS simulator and macOS builds on push to main
- ✅ Run tests and upload test results
- ✅ Build production IPA and DMG in unified release workflow
- ⚠️ Requires Apple Developer account for signing (optional)

## GitHub Actions

### Unified Release Workflow (NEW)

**File:** `.github/workflows/release.yml`

**Triggers:** Push of version tag matching `v*` (e.g., `v2.1.0`)

**Quick Start:**
```bash
# 1. Bump version in all files (automated!)
./scripts/bump-version.sh 2.1.0

# 2. Update CHANGELOG.md manually
# (Add section for [2.1.0] with your release notes)

# 3. Commit changes
git add .
git commit -m "chore: bump version to 2.1.0"

# 4. Tag and release
git tag v2.1.0
git push origin v2.1.0
# Workflow automatically builds all platforms!
```

**Automated Version Bumping:**
Use the provided `scripts/bump-version.sh` script to update versions across all platform files:
```bash
./scripts/bump-version.sh 2.1.0
```
This updates:
- `pom.xml` (Desktop Maven)
- `jnexus-core/build.gradle` (Shared library)
- `jnexus-android/build.gradle` (Android app)
- `jnexus-ios/iOS/Info.plist` (iOS app)
- `jnexus-ios/macOS/Info.plist` (macOS app)

**Workflow Jobs (Parallel):**
1. ✅ **prepare** - Extract version, validate consistency
2. ✅ **build-desktop** (parallel) - Build Desktop JAR (Java 21, Maven)
3. ✅ **build-android** (parallel) - Build Android APK (Gradle, Kotlin)
4. ✅ **build-ios** (parallel) - Build iOS IPA (Swift, Xcode)
5. ✅ **build-macos** (parallel) - Build macOS DMG (Swift, Xcode)
6. ✅ **create-release** (waits for all) - Create GitHub Release with all artifacts
7. ✅ **deploy** - Deploy Desktop JAR to PackageCloud.io
8. ✅ **notify** - Post release summary

**Output:**
- GitHub Release with all 4 platform artifacts
- Release notes extracted from CHANGELOG.md
- Desktop JAR deployed to PackageCloud.io

**Total Time:** ~10-15 minutes (iOS/macOS builds are slowest)

**Status:** ✅ Ready to use

**See:** [RELEASE_PROCESS.md](RELEASE_PROCESS.md) for complete documentation

---

### Desktop Continuous Integration

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

**Note:** This automatically creates version tags, which trigger the unified release workflow for all platforms.

### Required Secrets

Configure these in GitHub Settings → Secrets and variables → Actions:

#### Core Secrets (Required)

| Secret Name | Description | Example |
|-------------|-------------|---------|
| `PACKAGECLOUD_TOKEN` | PackageCloud API token | `abc123...` |
| `GITHUB_TOKEN` | Auto-provided by GitHub | (automatic) |

#### Android Signing Secrets (Optional - for signed release APKs)

| Secret Name | Description | How to Generate |
|-------------|-------------|-----------------|
| `ANDROID_SIGNING_KEY` | Base64-encoded keystore file | `base64 -i jnexus.keystore \| pbcopy` |
| `ANDROID_KEY_ALIAS` | Key alias in keystore | e.g., `jnexus` |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore password | Set during `keytool -genkey` |
| `ANDROID_KEY_PASSWORD` | Key password | Set during `keytool -genkey` |

Create a keystore (one-time):
```bash
keytool -genkey -v -keystore jnexus.keystore -alias jnexus \
  -keyalg RSA -keysize 2048 -validity 10000
base64 -i jnexus.keystore | pbcopy  # Copy to clipboard for ANDROID_SIGNING_KEY
```

#### iOS/macOS Signing Secrets (Optional - for IPA and DMG builds)

| Secret Name | Description | How to Generate |
|-------------|-------------|-----------------|
| `IOS_BUILD_CERTIFICATE` | Base64-encoded .p12 certificate | `base64 -i certificate.p12 \| pbcopy` |
| `IOS_P12_PASSWORD` | Password for .p12 file | Set during certificate export |
| `IOS_KEYCHAIN_PASSWORD` | Temporary keychain password | Any strong password |
| `IOS_PROVISIONING_PROFILE` | Base64-encoded .mobileprovision | `base64 -i profile.mobileprovision \| pbcopy` |
| `MACOS_BUILD_CERTIFICATE` | Base64-encoded macOS .p12 certificate | `base64 -i mac-certificate.p12 \| pbcopy` |

Export from Xcode:
```bash
# Export certificate from Keychain Access as .p12
base64 -i certificate.p12 | pbcopy
# Export provisioning profile from ~/Library/MobileDevice/Provisioning Profiles/
base64 -i profile.mobileprovision | pbcopy
```

**Note:** iOS/macOS builds will still compile and run simulator tests without signing secrets. The IPA and DMG artifacts are only produced when signing secrets are configured.

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

## Package Manager Distribution

### Homebrew Tap (macOS + Linux)

**Workflow:** `.github/workflows/homebrew.yml` (standalone) and integrated into `release.yml`

**Trigger:** Published release (non-prerelease)

**Setup:**
1. Create the `FlossWare/homebrew-tap` repository on GitHub
2. Add `HOMEBREW_TAP_TOKEN` secret (GitHub PAT with repo access to homebrew-tap)
3. The workflow automatically updates the formula on each release

**User installation:**
```bash
brew tap flossware/tap
brew install jnexus

# Or one-liner:
brew install flossware/tap/jnexus
```

**Required Secret:** `HOMEBREW_TAP_TOKEN` - GitHub PAT with write access to `FlossWare/homebrew-tap`

### Maven Central (jnexus-core Library)

**Workflow:** `.github/workflows/maven-central.yml`

**Trigger:** Published release (non-prerelease)

**Setup:**
1. Create Sonatype OSSRH account at https://issues.sonatype.org
2. Claim groupId `org.flossware`
3. Generate GPG key for artifact signing
4. Add secrets: `OSSRH_USERNAME`, `OSSRH_TOKEN`, `GPG_PRIVATE_KEY`, `GPG_PASSPHRASE`

**Users can then add to their pom.xml:**
```xml
<dependency>
    <groupId>org.flossware</groupId>
    <artifactId>jnexus-core</artifactId>
    <version>2.0.0</version>
</dependency>
```

**Required Secrets:**
| Secret | Description |
|--------|-------------|
| `OSSRH_USERNAME` | Sonatype OSSRH username |
| `OSSRH_TOKEN` | Sonatype OSSRH token |
| `GPG_PRIVATE_KEY` | GPG private key (armor format) |
| `GPG_PASSPHRASE` | GPG key passphrase |

### Snap Package (Linux)

**Workflow:** `.github/workflows/snap.yml`

**Trigger:** Published release (non-prerelease)

**Configuration:** `snap/snapcraft.yaml`

**Setup:**
1. Create Snap Store account at https://snapcraft.io
2. Register the `jnexus` snap name
3. Generate store credentials: `snapcraft export-login --snaps=jnexus --channels=stable -`
4. Add `SNAP_STORE_TOKEN` secret

**User installation:**
```bash
sudo snap install jnexus
```

**Required Secret:** `SNAP_STORE_TOKEN` - Snap Store credentials

### Google Play Store (Android)

**Workflow:** `.github/workflows/google-play.yml`

**Trigger:** Published release (non-prerelease)

**Setup:**
1. Create a Google Play Developer account (99 USD one-time fee)
2. Create a service account in Google Cloud Console
3. Grant the service account access in Google Play Console
4. Download the JSON key and add as `PLAY_STORE_JSON_KEY` secret
5. Configure Android signing secrets (see Android Signing Secrets above)

**Deployment flow:**
- Builds a signed release AAB (Android App Bundle)
- Uploads to Google Play Internal testing track
- Promote to production manually via Google Play Console

**Required Secrets:**
| Secret | Description |
|--------|-------------|
| `PLAY_STORE_JSON_KEY` | Google Play service account JSON key |
| `ANDROID_SIGNING_KEY` | Base64-encoded release keystore |
| `ANDROID_KEY_ALIAS` | Key alias in keystore |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore password |
| `ANDROID_KEY_PASSWORD` | Key password |

### F-Droid (Open-Source Android)

**Metadata:** `fdroid/org.flossware.jnexus.android.yml`

**Submission process:**
1. Fork the [F-Droid Data](https://gitlab.com/fdroid/fdroiddata) repository
2. Copy `fdroid/org.flossware.jnexus.android.yml` to `metadata/org.flossware.jnexus.android.yml`
3. Submit a merge request
4. F-Droid team reviews (1-2 weeks)

**No secrets required** - F-Droid builds from source.

**Requirements satisfied:**
- No proprietary dependencies
- Reproducible builds (Gradle)
- No tracking or analytics

### App Store (iOS / macOS)

**Workflow:** `.github/workflows/app-store.yml`

**Trigger:** Published release (non-prerelease)

**Setup:**
1. Enroll in Apple Developer Program (99 USD/year)
2. Create an App Store Connect API key
3. Configure signing certificates and provisioning profiles
4. Add secrets (see below)

**Deployment flow:**
- Archives iOS and macOS apps separately
- Uploads to App Store Connect via `xcrun altool`
- App Store review is manual (1-2 days)

**Required Secrets:**
| Secret | Description |
|--------|-------------|
| `APP_STORE_CONNECT_API_KEY_ID` | App Store Connect API key ID |
| `APP_STORE_CONNECT_ISSUER_ID` | App Store Connect issuer ID |
| `APP_STORE_CONNECT_API_KEY` | App Store Connect API key (.p8 content) |
| `IOS_BUILD_CERTIFICATE` | Base64-encoded iOS .p12 certificate |
| `IOS_P12_PASSWORD` | iOS certificate password |
| `IOS_KEYCHAIN_PASSWORD` | Temporary keychain password |
| `IOS_PROVISIONING_PROFILE` | Base64-encoded iOS provisioning profile |
| `MACOS_BUILD_CERTIFICATE` | Base64-encoded macOS .p12 certificate |
| `MACOS_P12_PASSWORD` | macOS certificate password |

### Distribution Summary

| Channel | Workflow | Secret Required | Status |
|---------|----------|-----------------|--------|
| GitHub Releases | `release.yml` | `GITHUB_TOKEN` (auto) | Active |
| PackageCloud | `release.yml` | `PACKAGECLOUD_TOKEN` | Active |
| Homebrew Tap | `release.yml` + `homebrew.yml` | `HOMEBREW_TAP_TOKEN` | Ready (needs tap repo) |
| Maven Central | `maven-central.yml` | OSSRH + GPG secrets | Ready (needs OSSRH account) |
| Snap Store | `snap.yml` | `SNAP_STORE_TOKEN` | Ready (needs Snap account) |
| Google Play | `google-play.yml` | `PLAY_STORE_JSON_KEY` + signing | Ready (needs Play account) |
| F-Droid | Manual submission | None (open source) | Ready (metadata in `fdroid/`) |
| App Store (iOS) | `app-store.yml` | App Store Connect + signing | Ready (needs Apple account) |
| App Store (macOS) | `app-store.yml` | App Store Connect + signing | Ready (needs Apple account) |

## Related Files

- `.github/workflows/main.yml` - GitHub Actions desktop CI workflow
- `.github/workflows/release.yml` - Unified release workflow (all platforms)
- `.github/workflows/homebrew.yml` - Homebrew tap update workflow
- `.github/workflows/maven-central.yml` - Maven Central publishing workflow
- `.github/workflows/snap.yml` - Snap package build and publish workflow
- `.github/workflows/google-play.yml` - Google Play Store deployment workflow
- `.github/workflows/app-store.yml` - Apple App Store deployment workflow (iOS + macOS)
- `fdroid/org.flossware.jnexus.android.yml` - F-Droid metadata for submission
- `fastlane/metadata/android/` - Fastlane metadata for Google Play and F-Droid
- `.gitlab-ci.yml` - GitLab CI configuration
- `ci/rev-version.sh` - Manual version bump script
- `pom.xml` - Maven POM with SCM config
- `jnexus-core/pom.xml` - Core library POM with Maven Central release profile
- `snap/snapcraft.yaml` - Snap package configuration
- `VERSIONING.md` - Version format documentation

## Questions?

See [CONTRIBUTING.md](CONTRIBUTING.md) for contribution guidelines.

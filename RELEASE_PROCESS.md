# Unified Release Process

This document describes the automated unified release workflow for building and publishing JNexus artifacts across all platforms (Desktop, Android, iOS, macOS) from a single git tag.

## Overview

The **Unified Release** workflow (`.github/workflows/release.yml`) is triggered by pushing a version tag and automatically:

1. **Validates** version consistency across all platforms
2. **Builds** artifacts for all 4 platforms in parallel
3. **Creates** a GitHub Release with all artifacts
4. **Deploys** the Desktop JAR to PackageCloud.io
5. **Sends** release notifications

**Trigger:** Push a version tag matching `v*`

```bash
git tag v2.1.0
git push origin v2.1.0
# Single command triggers builds for all platforms
```

## Pre-Release Checklist

Before pushing a release tag, ensure:

### 1. Update Version Numbers

Update versions in these files to match the release version (e.g., `2.1.0`):

**Desktop (Maven):**
```xml
<!-- pom.xml -->
<version>2.1.0</version>
```

**Core Library (Gradle):**
```gradle
// jnexus-core/build.gradle
version = '2.1.0'
```

**iOS/macOS (Xcode):**
```xml
<!-- jnexus-ios/iOS/Info.plist -->
<key>CFBundleShortVersionString</key>
<string>2.1.0</string>

<!-- jnexus-ios/macOS/Info.plist -->
<key>CFBundleShortVersionString</key>
<string>2.1.0</string>
```

**Android (Gradle):**
```gradle
// jnexus-android/build.gradle
version = '2.1.0'
```

### 2. Update CHANGELOG.md

Add a section for the new version at the top of `CHANGELOG.md`:

```markdown
## [2.1.0] - 2026-06-15

### Added
- New feature 1
- New feature 2

### Fixed
- Bug fix 1
- Bug fix 2

### Changed
- Enhancement 1
```

### 3. Run Full Build Locally

Test that all builds succeed:

```bash
# Desktop
./mvnw clean package

# Android
gradle :jnexus-core:build
gradle :jnexus-android:assembleDebug

# iOS (on macOS)
cd jnexus-ios
xcodebuild -scheme JNexus build
xcodebuild -scheme JNexus-macOS build
```

### 4. Commit Changes

```bash
git add pom.xml jnexus-core/build.gradle jnexus-ios/iOS/Info.plist \
        jnexus-ios/macOS/Info.plist CHANGELOG.md
git commit -m "chore: bump version to 2.1.0"
git push origin main
```

## Creating a Release

### Step 1: Create and Push Version Tag

```bash
# Create annotated tag
git tag -a v2.1.0 -m "Release v2.1.0: Description of changes"

# Push tag to GitHub (triggers the unified release workflow)
git push origin v2.1.0
```

### Step 2: Monitor Workflow

Check progress on GitHub:

1. Go to repository → **Actions** tab
2. Click **Unified Release** workflow
3. Watch jobs run in parallel:
   - ✓ Prepare (validate versions)
   - ✓ Build Desktop JAR
   - ✓ Build Android APK
   - ✓ Build iOS IPA
   - ✓ Build macOS DMG
   - ✓ Create GitHub Release
   - ✓ Deploy to PackageCloud

Expected time: **10-15 minutes** (iOS/macOS builds are slowest)

### Step 3: Verify Release

After workflow completes:

1. Check **Releases** page: https://github.com/FlossWare/nexus-java/releases
2. Verify all artifacts are attached:
   - ✓ `jnexus-*-jar-with-dependencies.jar`
   - ✓ `jnexus-android-2.1.0.apk`
   - ✓ `jnexus-ios-2.1.0.ipa` (if successful)
   - ✓ `jnexus-macos-2.1.0.dmg` (if successful)
3. Verify release notes were extracted from CHANGELOG.md

## Workflow Details

### Jobs and Workflow

```
prepare
├── get_version
├── extract_changelog
└── verify_version_consistency
    │
    ├──> build-desktop (build Desktop JAR)
    ├──> build-android (build Android APK)
    ├──> build-ios (build iOS IPA)
    └──> build-macos (build macOS DMG)
         │
         └──> create-release (GitHub Release with all artifacts)
              │
              ├──> deploy (PackageCloud.io)
              └──> notify (release summary)
```

### Job Descriptions

#### prepare
- **Runs on:** Ubuntu (fastest)
- **Time:** ~30 seconds
- **Tasks:**
  - Extract version from tag (e.g., `v2.1.0` → `2.1.0`)
  - Extract changelog section from CHANGELOG.md
  - Validate version consistency across pom.xml, build.gradle, Info.plist files
  - Output version and changelog for use by other jobs

#### build-desktop
- **Runs on:** Ubuntu
- **Time:** ~5 minutes
- **Output:** Desktop JAR artifact
- **Commands:**
  ```bash
  mvn clean package -DskipTests
  ```

#### build-android
- **Runs on:** Ubuntu
- **Time:** ~8 minutes
- **Output:** Android APK artifact
- **Commands:**
  ```bash
  gradle :jnexus-core:build
  gradle :jnexus-android:assembleDebug
  ```

#### build-ios
- **Runs on:** macOS (required for Xcode)
- **Time:** ~10 minutes
- **Output:** iOS IPA artifact (may fail without signing certificates)
- **Commands:**
  ```bash
  xcodebuild -scheme JNexus \
    -configuration Release \
    -sdk iphoneos \
    -archivePath build/JNexus.xcarchive \
    archive
  xcodebuild -exportArchive ...
  ```

#### build-macos
- **Runs on:** macOS (required for Xcode)
- **Time:** ~8 minutes
- **Output:** macOS DMG artifact
- **Commands:**
  ```bash
  xcodebuild -scheme JNexus-macOS \
    -configuration Release \
    -sdk macosx \
    -archivePath build/JNexus-macOS.xcarchive \
    archive
  hdiutil create ... (create DMG)
  ```

#### create-release
- **Runs on:** Ubuntu
- **Time:** ~1 minute
- **Tasks:**
  - Download all artifacts from previous jobs
  - Create GitHub Release
  - Attach all artifacts
  - Post release notes from CHANGELOG.md
  - **Does NOT fail** if iOS/macOS jobs fail (uses `if: always()`)

#### deploy
- **Runs on:** Ubuntu
- **Time:** ~1 minute
- **Tasks:**
  - Download Desktop JAR
  - Upload to PackageCloud.io
  - **Skips gracefully** if PACKAGECLOUD_TOKEN not set

#### notify
- **Runs on:** Ubuntu
- **Time:** ~30 seconds
- **Tasks:**
  - Post release summary
  - Show all artifacts built
  - Link to release page

### Parallel Execution

Jobs run in parallel where possible for speed:

```
Total time: ~10-15 minutes
├─ prepare: 30s
├─ build-desktop: 5m (parallel)
├─ build-android: 8m (parallel)
├─ build-ios: 10m (parallel with android/desktop)
├─ build-macos: 8m (parallel with others)
└─ create-release: 1m (waits for all builds)
```

## Required Secrets

Configure these in **GitHub Settings → Secrets and variables → Actions:**

| Secret | Description | Required | Example |
|--------|-------------|----------|---------|
| `PACKAGECLOUD_TOKEN` | PackageCloud API token | ✅ (for Deploy job) | `abc123...` |
| `GITHUB_TOKEN` | GitHub API token (auto-provided) | ✅ | (automatic) |

**Get PackageCloud token:**
1. Go to PackageCloud.io → Account Settings
2. Copy API token
3. Add to GitHub Secrets: `PACKAGECLOUD_TOKEN`

## Rollback Process

If a release fails or needs to be removed:

### Option 1: Delete Tag Locally and Remotely

```bash
# Delete local tag
git tag -d v2.1.0

# Delete remote tag
git push origin :refs/tags/v2.1.0

# Verify deletion
git tag -l | grep v2.1.0  # Should return nothing
```

### Option 2: Delete GitHub Release

```bash
# Using GitHub CLI
gh release delete v2.1.0 --yes

# Or manually via GitHub web UI:
# 1. Go to Releases page
# 2. Click the release
# 3. Click "Delete"
```

### Option 3: Fix and Retry

```bash
# Fix the issue
git add <fixed-files>
git commit -m "fix: resolve release issue"

# Increment version
# (e.g., 2.1.0 → 2.1.1)

# Update version files and CHANGELOG.md
# Then retry:
git tag v2.1.1
git push origin v2.1.1
```

## Troubleshooting

### Prepare Job Fails: Version Mismatch

**Problem:** `prepare` job fails with "version mismatch"

**Solution:** Ensure all version files match the tag:

```bash
VERSION="2.1.0"  # Without 'v' prefix

# Check versions
grep "<version>${VERSION}</version>" pom.xml
grep "version = '${VERSION}'" jnexus-core/build.gradle
grep "<string>${VERSION}</string>" jnexus-ios/iOS/Info.plist
grep "<string>${VERSION}</string>" jnexus-ios/macOS/Info.plist

# Update any that don't match, then re-tag:
git add pom.xml jnexus-core/build.gradle jnexus-ios/iOS/Info.plist jnexus-ios/macOS/Info.plist
git commit --amend --no-edit
git tag -d v${VERSION}
git tag -a v${VERSION} -m "Release v${VERSION}"
git push origin v${VERSION} --force
```

### Build Job Fails

**Problem:** Desktop/Android/iOS build fails

**Solution:** Check the build logs:

1. Go to Actions tab → Unified Release → Failed job
2. Click the job name to see full logs
3. Fix the issue locally, then retry:

```bash
# Re-push the same tag
git tag -d v2.1.0
git tag -a v2.1.0 -m "Release v2.1.0"
git push origin v2.1.0 --force
```

### iOS/macOS Builds Fail (Expected)

**Problem:** iOS or macOS builds show warnings/errors

**Expected:** iOS/macOS builds in CI require:
- Apple Developer account
- Signing certificates (Base64-encoded)
- Provisioning profiles
- Keychain setup

**Current Status:** Builds will fail without signing setup, but GitHub Release is still created with Desktop and Android artifacts.

**To enable iOS/macOS signing:**

See the secrets setup section below.

### Changelog Not Extracted

**Problem:** Release notes show "Release version 2.1.0" instead of actual changelog

**Solution:** Ensure CHANGELOG.md has correct format:

```markdown
## [2.1.0] - 2026-06-15

### Added
- New feature

## [2.0.0] - 2026-06-01

### Added
- Previous feature
```

The workflow extracts text between `## [VERSION]` and next `## [`, so format is critical.

### PackageCloud Deployment Fails

**Problem:** Deploy job shows "deployment failed"

**Solution:** Check PackageCloud token:

```bash
# Verify token is set
gh secret list | grep PACKAGECLOUD

# If not set:
gh secret set PACKAGECLOUD_TOKEN
# (paste token, press Ctrl+D)
```

Deployment gracefully fails if token is not set (still creates GitHub Release).

## iOS Signing Setup (Optional)

To enable iOS IPA building and signing in CI:

### 1. Export Apple Certificate

On your Mac with Xcode:

```bash
# Open Keychain Access
open /Applications/Utilities/Keychain\ Access.app

# Right-click certificate → Export
# Save as: certificate.p12
# Enter password (remember it)

# Encode for GitHub
base64 -i certificate.p12 | pbcopy
```

### 2. Export Provisioning Profile

```bash
# File is here:
~/Library/MobileDevice/Provisioning\ Profiles/

# List profiles
ls ~/Library/MobileDevice/Provisioning\ Profiles/

# Choose the correct one for your app
# Encode for GitHub
base64 -i ~/Library/MobileDevice/Provisioning\ Profiles/profile.mobileprovision | pbcopy
```

### 3. Add to GitHub Secrets

In GitHub Settings → Secrets:

```
IOS_BUILD_CERTIFICATE (base64-encoded .p12)
IOS_P12_PASSWORD (password for .p12)
IOS_KEYCHAIN_PASSWORD (temporary keychain password)
IOS_PROVISIONING_PROFILE (base64-encoded .mobileprovision)
```

## Android Signing Setup (Optional)

To enable release APK signing:

### 1. Create Keystore (One-Time)

```bash
keytool -genkey -v \
  -keystore jnexus.keystore \
  -alias jnexus \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000

# Remember the passwords!
```

### 2. Encode for GitHub

```bash
base64 -i jnexus.keystore | pbcopy
```

### 3. Add to GitHub Secrets

In GitHub Settings → Secrets:

```
ANDROID_SIGNING_KEY (base64-encoded keystore)
ANDROID_KEY_ALIAS (jnexus)
ANDROID_KEYSTORE_PASSWORD (your password)
ANDROID_KEY_PASSWORD (your password)
```

### 4. Update Workflow (Optional)

Uncomment the signing step in `.github/workflows/release.yml`:

```yaml
- name: Sign APK
  uses: r0adkll/sign-android-release@v1
  with:
    releaseDirectory: jnexus-android/build/outputs/apk/release
    signingKeyBase64: ${{ secrets.ANDROID_SIGNING_KEY }}
    alias: ${{ secrets.ANDROID_KEY_ALIAS }}
    keyStorePassword: ${{ secrets.ANDROID_KEYSTORE_PASSWORD }}
    keyPassword: ${{ secrets.ANDROID_KEY_PASSWORD }}
```

## Advanced: Manual Release

To manually control the release process:

```bash
# 1. Build locally
./mvnw clean package  # Desktop
gradle :jnexus-android:assembleDebug  # Android
cd jnexus-ios && xcodebuild archive  # iOS

# 2. Create GitHub Release
gh release create v2.1.0 \
  --title "Release v2.1.0" \
  --notes-file release-notes.md \
  target/jnexus-*-jar-with-dependencies.jar \
  jnexus-android/build/outputs/apk/debug/jnexus-android-debug.apk

# 3. Deploy JAR
curl -X POST \
  -H "Authorization: Bearer $PACKAGECLOUD_TOKEN" \
  -F "package[distro_version_id]=190" \
  -F "package[package_file]=@target/jnexus-*-jar-with-dependencies.jar" \
  https://packagecloud.io/api/v1/repos/flossware/jnexus/packages.json
```

## Release Checklist

- [ ] Updated version in pom.xml
- [ ] Updated version in jnexus-core/build.gradle
- [ ] Updated version in jnexus-ios/iOS/Info.plist
- [ ] Updated version in jnexus-ios/macOS/Info.plist
- [ ] Updated CHANGELOG.md with release notes
- [ ] All builds pass locally (`./mvnw clean package`, `gradle build`, `xcodebuild archive`)
- [ ] Committed version changes to main branch
- [ ] Created version tag: `git tag v2.1.0`
- [ ] Pushed tag: `git push origin v2.1.0`
- [ ] Verified GitHub Actions workflow runs
- [ ] Checked GitHub Release page for all artifacts
- [ ] Verified PackageCloud deployment (if token configured)
- [ ] Posted release announcement (if applicable)

## Integration with CONTRIBUTING.md

Contributors should be aware of this process:

> **Releases are now fully automated!** Push a version tag and all platforms build, test, and release automatically.
>
> See [RELEASE_PROCESS.md](RELEASE_PROCESS.md) for detailed steps.

## FAQ

**Q: How do I release a hotfix?**
```bash
# Fix the bug
git add .
git commit -m "fix: bug description"

# Increment patch version (1.0 → 1.0.1 if using X.Y.Z)
# OR increment minor version (1.0 → 1.1 if using X.Y)
# Update all version files + CHANGELOG.md

# Release as normal
git tag v1.1
git push origin v1.1
```

**Q: Can I skip the iOS build?**
A: The workflow continues even if iOS/macOS builds fail (GitHub Release is still created with Desktop and Android artifacts). iOS builds require Apple Developer account setup; see optional iOS signing section.

**Q: How do I test the workflow locally?**
A: Use `act` to simulate GitHub Actions:
```bash
act push --input tag=v2.1.0
```

**Q: What if I accidentally pushed a bad tag?**
A: See Rollback Process section above.

**Q: How do I update release notes after creating a release?**
A: Edit the release on GitHub:
1. Go to Releases page
2. Click the release
3. Click "Edit"
4. Update notes and save

## Related Documentation

- [CI-CD.md](CI-CD.md) - General CI/CD configuration
- [CONTRIBUTING.md](CONTRIBUTING.md) - Contribution guidelines
- [CHANGELOG.md](CHANGELOG.md) - Release history

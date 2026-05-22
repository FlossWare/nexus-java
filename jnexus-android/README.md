# JNexus Android App

[![Latest Release](https://img.shields.io/github/v/release/FlossWare/jnexus)](https://github.com/FlossWare/jnexus/releases/latest)
[![Android](https://img.shields.io/badge/Android-8.0+-green)](https://developer.android.com/about/versions/oreo)

Native Android mobile application for managing Sonatype Nexus Repository Manager components.

## Features

### 📱 Complete Mobile UI
- **List Screen**: Browse and manage repository components
  - Repository selector with List/Refresh actions
  - Component cards showing size, creation date
  - Tap for full metadata dialog
  - Delete with confirmation
  
- **Search Screen**: Advanced filtering
  - Size range filters (min/max bytes)
  - Date range filters (ISO 8601)
  - File extension filter
  - Component name pattern matching
  - Regex path filtering
  - Collapsible filter panel
  
- **Stats Screen**: Repository analytics
  - Overview metrics (total, average, median)
  - Size distribution (5 buckets with percentages)
  - File type breakdown (top 10)
  - Age distribution (7/30/90 days, older)
  - Largest components (top 10)
  
- **Settings Screen**: Configuration management
  - Secure credential storage (AES256 encryption)
  - Repository configuration
  - HTTP timeout settings
  - Default values (repository, regex, dry-run)

### 🔐 Security
- **Encrypted credential storage** using Android EncryptedSharedPreferences
- AES256-GCM encryption via Android Keystore
- Credentials stored in private app directory

### ⚡ Performance
- **Intelligent caching** with 5-minute TTL
- **Retry logic** with exponential backoff
- **Pagination** automatically handled
- Async operations with Kotlin Coroutines

## Requirements

- **Android 8.0+** (API 26 or higher)
- **Internet permission** (for Nexus API access)
- **Valid Nexus credentials** (URL, username, password)

## Installation

### Download APK

**From GitHub Releases:**
1. Go to [Releases](https://github.com/FlossWare/jnexus/releases/latest)
2. Download `jnexus-android-X.X.X.apk`
3. Enable "Install from unknown sources" in Android settings
4. Install the APK on your device

**Via GitHub CLI:**
```bash
gh release download --pattern "jnexus-android-*.apk"
adb install jnexus-android-*.apk
```

### Build from Source

```bash
# Clone the repository
git clone https://github.com/FlossWare/jnexus.git
cd jnexus

# Build debug APK (signed, ready to install)
./gradlew :jnexus-android:assembleDebug

# Install on connected device
adb install jnexus-android/build/outputs/apk/debug/jnexus-android-debug.apk

# Or build and install in one step
./gradlew :jnexus-android:installDebug
```

## Usage

### First Launch

1. **Open the app** after installation
2. **Go to Settings tab** (bottom navigation)
3. **Configure credentials**:
   - Nexus URL (e.g., `https://nexus.example.com`)
   - Username
   - Password
   - Repository list (optional, comma-separated)
4. **Tap Save** to encrypt and store credentials

### List Components

1. **Go to List tab**
2. **Select a repository** from the dropdown
3. **Tap "List"** to load components (uses cache)
4. **Tap "Refresh"** to force fresh data
5. **Tap a component** to view full metadata
6. **Tap delete icon** to remove a component (with confirmation)

### Advanced Search

1. **Go to Search tab**
2. **Enter repository name**
3. **Tap "Show Filters"** to expand filter panel
4. **Set filters** (size range, dates, extension, regex)
5. **Tap "Search"** to find matching components
6. **Tap "Clear Filters"** to reset

### View Statistics

1. **Go to Stats tab**
2. **Enter repository name**
3. **Tap "Calculate Statistics"**
4. **Scroll through analytics**:
   - Overview metrics
   - Size distribution
   - File types
   - Age distribution
   - Largest components

## Architecture

### Multi-Module Structure

```
jnexus/
├── jnexus-core/          (Shared business logic - Java 11)
│   ├── NexusService.java      # Search, filter, statistics
│   ├── NexusHttpClient.java   # HTTP interface
│   ├── Credentials.java       # Credentials interface
│   └── Data models (records)
├── jnexus-android/       (Android app - Kotlin)
│   ├── NexusClientOkHttp.java     # OkHttp implementation
│   ├── CredentialsAndroid.java    # Encrypted storage
│   ├── NexusApplication.java      # DI container
│   └── ui/screens/                # Jetpack Compose UI
```

### Platform Abstraction

**HTTP Layer:**
- Interface: `NexusHttpClient`
- Desktop: `NexusClient` (java.net.http)
- Android: `NexusClientOkHttp` (OkHttp)

**Credentials Storage:**
- Interface: `Credentials`
- Desktop: File-based (`~/.flossware/nexus/nexus.properties`)
- Android: `CredentialsAndroid` (EncryptedSharedPreferences)

### Technology Stack

- **UI**: Jetpack Compose + Material Design 3
- **HTTP**: OkHttp 4.12.0
- **JSON**: Jackson + org.json
- **Encryption**: AndroidX Security Crypto
- **Async**: Kotlin Coroutines
- **Build**: Gradle 9.5.1 + Android Gradle Plugin 8.7.3
- **Language**: Kotlin (UI) + Java (business logic)

## Development

### Build Variants

```bash
# Debug build (signed, debuggable)
./gradlew :jnexus-android:assembleDebug

# Release build (optimized, ProGuard enabled)
./gradlew :jnexus-android:assembleRelease
```

### Run Tests

```bash
# Unit tests
./gradlew :jnexus-android:testDebugUnitTest

# Instrumented tests (requires Android device/emulator)
./gradlew :jnexus-android:connectedDebugAndroidTest

# All tests
./gradlew :jnexus-android:check
```

### ProGuard Rules

Release builds use ProGuard/R8 for code shrinking and obfuscation. Rules are in `proguard-rules.pro`:
- Keeps Error Prone annotations (used by Tink crypto library)
- Preserves Jackson, OkHttp, org.json classes
- Keeps jnexus-core data models
- Retains Kotlin metadata

## Troubleshooting

### App Won't Install

**Error: "App not installed as package appears invalid"**
- **Cause**: APK is unsigned or corrupted
- **Solution**: Download the debug-signed APK from releases, not the unsigned release APK

### Connection Failed

**Error: "Failed to connect to Nexus"**
- Verify Nexus URL is correct (include `https://` or `http://`)
- Check network connection
- Verify credentials are valid
- Check Nexus server is accessible from mobile network

### Crashes on Launch

**App crashes immediately**
- Check Android version (must be 8.0+)
- Clear app data: Settings → Apps → JNexus → Storage → Clear data
- Reinstall the app

## Documentation

- [Main README](../README.md) - Full project documentation
- [ANDROID.md](ANDROID.md) - Android implementation details
- [CHANGELOG.md](../CHANGELOG.md) - Version history

## License

Licensed under the Apache License 2.0. See [LICENSE](../LICENSE) file for details.

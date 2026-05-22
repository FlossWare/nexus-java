# JNexus Android Mobile App

A native Android app for managing Sonatype Nexus Repository Manager components on mobile devices.

## Features

- **List Components**: Browse repository contents with caching
- **Advanced Search**: Filter by size, date, extension, name pattern, and regex
- **Repository Statistics**: Analyze size distribution, file types, age, and largest components
- **Secure Credentials**: AES256-encrypted storage using Android's EncryptedSharedPreferences
- **Material Design 3**: Modern, consistent Android UI
- **Same Business Logic**: Shares core logic with desktop versions for consistent behavior

## Requirements

- Android 8.0+ (API level 26 or higher)
- Internet connection
- Valid Nexus repository credentials

## Installation

### From Pre-built APK

1. Download the latest APK from the GitHub releases page
2. Enable "Install from unknown sources" in Android settings
3. Install the APK on your device
4. Open the app

### Build from Source

Prerequisites:
- Android SDK
- Gradle 8.2+
- JDK 11+

Build steps:
```bash
# From the project root
./gradlew :jnexus-android:assembleDebug

# Install on connected device/emulator
adb install jnexus-android/build/outputs/apk/debug/jnexus-android-debug.apk
```

## Initial Setup

1. Open the JNexus app
2. Navigate to the **Settings** tab
3. Enter your configuration:
   - **Nexus URL**: Your Nexus server URL (e.g., `https://nexus.example.com`)
   - **Username**: Your Nexus username
   - **Password**: Your Nexus password or API token
   - **Repository List** (optional): Comma-separated repository names (e.g., `maven-releases, maven-snapshots, npm-public`)
   - **Default Repository** (optional): Pre-selected repository name
   - **Default Regex** (optional): Default regex filter (e.g., `.*SNAPSHOT.*`)
   - **Default Dry-Run**: Whether to enable dry-run mode by default
   - **HTTP Timeout**: Timeout in seconds (default: 30)
4. Tap **Save**

Your credentials are encrypted and stored securely using Android's EncryptedSharedPreferences with AES256_GCM encryption.

## Usage

### List Screen

Browse components in a repository:

1. Select a repository from the dropdown (or type manually if not configured)
2. Tap **List** to fetch components (uses cache)
3. Or tap **Refresh** to bypass cache and fetch fresh data
4. Tap any component card to view full metadata
5. Tap the delete icon on a component to delete it (with confirmation)

### Search Screen

Advanced filtering:

1. Select a repository
2. Tap **Show Filters** to expand the filter panel
3. Configure filters:
   - **Min/Max Size**: Filter by file size in bytes
   - **Created After/Before**: Filter by creation date (ISO 8601 format, e.g., `2024-01-01T00:00:00Z`)
   - **File Extension**: Filter by extension (e.g., `.jar`)
   - **Component Name Pattern**: Filter by component name
   - **Path Regex**: Filter by path regex pattern
4. Tap **Search** to apply filters
5. Tap **Clear Filters** to reset all filters

### Stats Screen

Repository analytics:

1. Select a repository
2. Tap **Calculate Statistics**
3. View analytics:
   - **Overview**: Total components, total size (MB/GB), average, median
   - **Size Distribution**: 5 size buckets with percentages
   - **File Types**: Top 10 file types by total size
   - **Age Distribution**: Components by age (7/30/90 days, older)
   - **Largest Components**: Top 10 largest components

### Settings Screen

Configuration management:

- **Connection**: URL, username, password, HTTP timeout
- **Repositories**: Comma-separated list for dropdown menus
- **Defaults**: Default repository, regex, and dry-run mode
- **Actions**:
  - **Save**: Save configuration (encrypted)
  - **Clear**: Clear all configuration data

## Architecture

### Multi-Module Structure

The project uses a multi-module architecture:

```
jnexus/
├── jnexus-core/          # Shared business logic (Java 11)
│   ├── NexusService.java       # Filtering, statistics
│   ├── ComponentMetadata.java  # Data models
│   ├── SearchCriteria.java
│   └── RepositoryStats.java
├── jnexus-android/       # Android app (Kotlin + Compose)
│   ├── NexusClientOkHttp.java  # OkHttp HTTP client
│   ├── CredentialsAndroid.java # Encrypted credentials
│   ├── MainActivity.kt         # Main activity
│   └── ui/screens/             # Jetpack Compose screens
└── src/                  # Desktop app (Java 21 + Maven)
    ├── JNexus.java            # CLI
    ├── JNexusSwing.java       # Swing GUI
    ├── JNexusAWT.java         # AWT GUI
    └── JNexusUI.java          # Terminal UI
```

### Platform Abstractions

The app uses interfaces to abstract platform-specific implementations:

- **NexusHttpClient**: HTTP communication layer
  - Desktop: `java.net.http.HttpClient`
  - Android: `OkHttp`
- **Credentials**: Credential storage layer
  - Desktop: File-based (`~/.flossware/nexus/nexus.properties`)
  - Android: `EncryptedSharedPreferences`

### Key Technologies

- **Jetpack Compose**: Modern declarative UI framework
- **Material Design 3**: Android design system
- **OkHttp**: HTTP client with retry logic and caching
- **EncryptedSharedPreferences**: Secure credential storage
- **Kotlin Coroutines**: Async/concurrent operations
- **Desugaring**: Backport Java 11 features (records) to Android 8.0+

## Security

### Credential Storage

Credentials are encrypted using Android's EncryptedSharedPreferences:

- **Key Encryption**: AES256_SIV
- **Value Encryption**: AES256_GCM
- **Master Key**: Generated and managed by Android Keystore
- **Storage Location**: Private app directory (inaccessible to other apps)

### Network Security

- **HTTPS Only**: `usesCleartextTraffic=false` in manifest
- **Certificate Validation**: Standard Android certificate pinning
- **Timeout Protection**: Configurable HTTP timeout (default 30s)

### Permissions

The app requires minimal permissions:

- `INTERNET`: Required for HTTP communication with Nexus server
- `ACCESS_NETWORK_STATE`: Check network connectivity before requests

## Troubleshooting

### "Please configure credentials in Settings"

You haven't configured your Nexus credentials yet. Go to Settings tab and enter your Nexus URL, username, and password.

### "Failed to initialize encrypted storage"

This can happen on very old Android devices or custom ROMs without proper Keystore support. Try:
1. Clear app data: Settings → Apps → JNexus → Storage → Clear data
2. Reinstall the app
3. If issue persists, your device may not support EncryptedSharedPreferences

### "HTTP 401: Unauthorized"

Your credentials are incorrect. Go to Settings and verify:
- Nexus URL is correct (including https://)
- Username is correct
- Password or API token is correct

### "HTTP 500" or "Connection timeout"

Network or server issues:
- Check your internet connection
- Verify Nexus server is accessible from your device
- Try increasing HTTP timeout in Settings
- Check Nexus server logs for errors

### App crashes on startup

1. Clear app data: Settings → Apps → JNexus → Storage → Clear data
2. If issue persists, check Android version (requires 8.0+)
3. Report the issue with crash logs

## Development

### Running Tests

```bash
# Unit tests (NexusClientOkHttp)
./gradlew :jnexus-android:testDebugUnitTest

# Instrumented tests (CredentialsAndroid)
./gradlew :jnexus-android:connectedDebugAndroidTest
```

### Building Release APK

1. Configure signing in `jnexus-android/build.gradle`:
   ```gradle
   android {
       signingConfigs {
           release {
               storeFile file("path/to/keystore.jks")
               storePassword "password"
               keyAlias "alias"
               keyPassword "password"
           }
       }
       buildTypes {
           release {
               signingConfig signingConfigs.release
           }
       }
   }
   ```

2. Build:
   ```bash
   ./gradlew :jnexus-android:assembleRelease
   ```

### Code Structure

- `NexusClientOkHttp.java`: OkHttp-based HTTP client (mirrors desktop `NexusClient.java`)
- `CredentialsAndroid.java`: Android credential storage
- `NexusApplication.java`: Application class for dependency injection
- `MainActivity.kt`: Main activity with bottom navigation
- `ui/screens/*.kt`: Jetpack Compose screens
  - `RepositoryListScreen.kt`: List components
  - `SearchScreen.kt`: Advanced search
  - `StatsScreen.kt`: Repository statistics
  - `SettingsScreen.kt`: Configuration

## Contributing

See the main [CONTRIBUTING.md](../CONTRIBUTING.md) for contribution guidelines.

When contributing to the Android module:
- Follow Kotlin coding conventions
- Use Jetpack Compose best practices
- Maintain Material Design 3 consistency
- Add unit tests for Java code
- Add instrumented tests for Android-specific code
- Update this README for UI changes

## License

Same license as main JNexus project. See [LICENSE](../LICENSE).

## Changelog

See [CHANGELOG.md](../CHANGELOG.md) for version history.

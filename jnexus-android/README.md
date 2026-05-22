# JNexus Android Module

Android mobile application for managing Sonatype Nexus Repository Manager components.

## Status: In Development (Skeleton Created)

This module contains the foundational structure for the JNexus Android app. The following components have been created:

### ✅ Completed
- Module structure and directories
- `build.gradle` - Android build configuration with all dependencies
- `AndroidManifest.xml` - App manifest with permissions and activity declarations
- Integration with `jnexus-core` shared library

### 🚧 To Be Implemented

#### 1. HTTP Client (`NexusClientOkHttp.java`)
- OkHttp-based implementation of `NexusHttpClient` interface
- Replace `java.net.http.HttpClient` with OkHttp for Android compatibility
- Implement retry logic with exponential backoff
- Cache management (5-minute TTL)
- ~500 lines of code

#### 2. Credentials Storage (`CredentialsAndroid.java`)
- EncryptedSharedPreferences implementation
- Secure credential storage
- Profile management
- ~150 lines of code

#### 3. Application Class (`NexusApplication.java`)
- Dependency injection setup
- Initialize NexusHttpClient, Credentials, NexusService
- ~80 lines of code

#### 4. UI Screens (Jetpack Compose)
- `MainActivity.kt` - Main activity with bottom navigation
- `RepositoryListScreen.kt` - List components with filters
- `SearchScreen.kt` - Advanced search interface
- `StatsScreen.kt` - Repository statistics
- `SettingsScreen.kt` - Credentials and configuration
- ~1,200 lines of code total

#### 5. UI Components
- `ComponentCard.kt` - Component list item
- `FilterPanel.kt` - Collapsible filter controls
- `StatisticsChart.kt` - Charts for statistics
- ~400 lines of code

#### 6. Resources
- `res/values/strings.xml` - String resources
- `res/values/colors.xml` - Material Design 3 colors
- `res/values/themes.xml` - App theme
- `res/mipmap/` - App icons
- ~200 lines of XML

#### 7. Tests
- Unit tests for `NexusClientOkHttp`
- Instrumented tests for `CredentialsAndroid`
- ~300 lines of code

## Building

Once implementation is complete:

```bash
./gradlew :jnexus-android:assembleDebug
```

## Installation

```bash
adb install jnexus-android/build/outputs/apk/debug/jnexus-android-debug.apk
```

## Dependencies

- **jnexus-core**: Shared business logic and data models
- **OkHttp**: HTTP client (Android-compatible)
- **Jetpack Compose**: Modern declarative UI
- **Material Design 3**: UI components
- **EncryptedSharedPreferences**: Secure storage

## Features (Planned)

- ✅ List components with advanced filters (size, date, extension)
- ✅ Delete components with confirmation
- ✅ Repository statistics (size distribution, file types, age)
- ✅ Component metadata viewing
- ✅ Multi-profile support (dev/prod/staging)
- ✅ Caching with 5-minute TTL
- ✅ HTTP retry logic

## Architecture

```
jnexus-android/
├── NexusClientOkHttp.java      (HTTP - OkHttp implementation)
├── CredentialsAndroid.java     (Storage - SharedPreferences)
├── NexusApplication.java       (DI - App initialization)
├── MainActivity.kt             (UI - Main activity)
└── ui/screens/                 (UI - Compose screens)
    ├── RepositoryListScreen.kt
    ├── SearchScreen.kt
    ├── StatsScreen.kt
    └── SettingsScreen.kt
```

## Next Steps

1. Implement `NexusClientOkHttp.java` (copy logic from `NexusClient.java`, replace HttpClient with OkHttp)
2. Implement `CredentialsAndroid.java` (adapt from `Credentials.java`, use SharedPreferences)
3. Create UI screens with Jetpack Compose
4. Add tests
5. Create app icons and resources
6. Build and test on Android device/emulator

## Reference Implementation

See `src/main/java/org/flossware/jnexus/` in the root project for desktop implementations that can be adapted:
- `NexusClient.java` → basis for `NexusClientOkHttp.java`
- `Credentials.java` → basis for `CredentialsAndroid.java`
- `JNexusSwing.java` → UI patterns to adapt for Compose

## Estimated Completion Time

- **NexusClientOkHttp**: 2-3 hours
- **CredentialsAndroid**: 1 hour
- **UI Screens**: 4-5 hours
- **Tests**: 1-2 hours
- **Resources**: 1 hour

**Total**: ~10-12 hours remaining

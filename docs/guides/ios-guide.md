# JNexus iOS/iPadOS App Guide

Back to [Getting Started](../getting-started.md) | [Configuration](../advanced/configuration-guide.md)

## Overview

The JNexus iOS/iPadOS app provides native Nexus repository management on iPhone and iPad. Built entirely with Swift and SwiftUI, it uses zero external dependencies and stores credentials securely in the iOS Keychain with AES-256 hardware-backed encryption.

## System Requirements

- iOS 16.0 or iPadOS 16.0 or higher
- Internet access to your Nexus server

## Installation

### From GitHub Releases

1. Download the IPA from [GitHub Releases](https://github.com/FlossWare/nexus-java/releases/tag/ios-v1.0)
2. Install via AltStore, Sideloadly, or Apple Configurator

### Via TestFlight (Beta)

Contact the development team for TestFlight access.

### Build from Source

Requires a Mac with Xcode 15+:

1. Clone the repository:
   ```bash
   git clone https://github.com/FlossWare/nexus-java.git
   ```
2. Open `jnexus-ios/JNexus.xcodeproj` in Xcode
3. Select the **JNexus-iOS** scheme
4. Select your device or simulator
5. Build and run (Cmd+R)

Note: Running on a physical device requires an Apple Developer account for code signing.

## First Launch

### Configure Credentials

1. Open the app
2. Tap the **Settings** tab (bottom tab bar)
3. Enter your Nexus server details:
   - **URL**: Your Nexus server address
   - **Username**: Your Nexus account
   - **Password**: Your password or user token (SecureField with show/hide toggle)
4. Optionally configure:
   - **Repositories**: Comma-separated list
   - **Default values**: Repository, regex, dry-run
   - **HTTP timeout**: Connection timeout in seconds
5. Tap **Save**
6. Credentials are encrypted via Keychain Services (AES-256 hardware-backed)

## Interface Tour

### Tab Bar

The app uses a bottom tab bar with 4 tabs:

| Tab | Description |
|-----|-------------|
| List | Browse and manage components |
| Search | Advanced filtering with multiple criteria |
| Stats | Repository analytics and statistics |
| Settings | Credential and configuration management |

### List Tab

**Features:**
- **Repository Picker**: Select from "All" or configured repositories
- **List / Refresh buttons**: Fetch components (cached vs. fresh)
- **Component list**: SwiftUI List with component rows
- **Tap component**: View full metadata in a detail sheet
- **Swipe to delete**: Swipe left on a component row to delete
- **Delete confirmation**: Alert dialog before deletion
- **Pull to refresh**: Swipe down to refresh the component list (.refreshable modifier)
- **Loading indicator**: ProgressView during network operations
- **Error display**: Error messages shown inline

### Search Tab

**Features:**
- **Repository selector**: Choose which repository to search
- **Collapsible filter sections**: DisclosureGroup for each filter category
- **Size filters**: Min/max size in bytes (TextField with number keyboard)
- **Date filters**: DatePicker for created after/before dates
- **Extension filter**: Text field for file extension (e.g., `.jar`)
- **Regex filter**: Text field for path pattern matching
- **Search button**: Execute search with all active filters
- **Clear filters**: Reset all filter values to defaults

### Stats Tab

**Features:**
- **Overview**: Total components, total size, average, median (formatted as MB/GB)
- **Size distribution**: Progress bars across 5 size buckets
- **File types**: Sorted by total size per extension
- **Age distribution**: Components grouped by age ranges
- **Largest components**: Top 10 by size

All sections use SwiftUI GroupBox with ScrollView layout.

### Settings Tab

**Features:**
- **Credential inputs**: URL, username, password (SecureField with toggle)
- **Repository list**: Comma-separated TextField
- **Default values**: Repository, regex, dry-run Toggle
- **HTTP timeout**: Numeric TextField
- **Save / Clear buttons**: Save encrypts to Keychain; Clear removes all data
- **Security info**: Caption text explaining Keychain Services encryption

## iPad-Specific Features

On iPad, the app adapts its layout for larger screens:

- **Landscape split view**: Uses `horizontalSizeClass` to detect and display split view layout
- **Larger stat cards**: More spacious GroupBox sections
- **Multi-column layouts**: Better use of screen real estate

The same code runs on both iPhone and iPad -- SwiftUI handles the adaptive layout automatically.

## Gestures

| Gesture | Action |
|---------|--------|
| Tap component | View full metadata |
| Swipe left | Delete component (with confirmation) |
| Pull down | Refresh component list |
| Tap tab bar | Switch between screens |

## Security

### Keychain Storage

- Credentials are encrypted using Keychain Services with AES-256 hardware-backed encryption
- Access level: `kSecAttrAccessibleWhenUnlocked` (encrypted at rest, decrypted only when device is unlocked)
- Stored in the system keychain, inaccessible to other apps
- Non-sensitive settings (repositories, defaults, timeout) stored in UserDefaults

### Network Security

- App Transport Security (ATS) enforces HTTPS by default
- Local networking allowed for development (configured in Info.plist)
- No external dependencies means no third-party network libraries

## Architecture

The iOS app re-implements the business logic in Swift (rather than sharing Java code) while maintaining identical semantics:

- **NexusClientURLSession**: URLSession-based HTTP client with caching and retry
- **NexusService**: Business logic (filtering, statistics, search) ported from Java
- **CredentialsKeychain**: Keychain Services storage
- **AppState**: ObservableObject for dependency injection via @EnvironmentObject

95% of code is shared between iOS and iPadOS targets. The remaining 5% is platform-specific entry points and navigation.

## Tips and Tricks

1. **Pull to refresh**: Swipe down on the list view for a quick refresh
2. **Swipe to delete**: Swipe left on any component for quick deletion
3. **DisclosureGroup**: Tap filter section headers to expand/collapse
4. **Date picker**: Use the native iOS date picker for date filters -- no need to type ISO 8601 strings
5. **Credential toggle**: Tap the eye icon to verify your password is correct before saving
6. **Reinitialize**: After saving new credentials, the app automatically reinitializes the HTTP client

## Troubleshooting

| Issue | Solution |
|-------|----------|
| `Network error` | Check internet connection and Nexus URL |
| `401 Unauthorized` | Verify credentials in Settings |
| `Connection timeout` | Increase HTTP timeout in Settings |
| App crashes on startup | Delete and reinstall the app |
| Cannot save credentials | Check device storage is not full |
| HTTPS error | Ensure Nexus URL starts with `https://` |
| iPad layout issues | Rotate device or restart app |

## Building from Source

### Xcode Build

1. Open `jnexus-ios/JNexus.xcodeproj`
2. Select scheme: **JNexus-iOS**
3. Select target device or simulator
4. Build: Cmd+B
5. Run: Cmd+R

### Command Line Build

```bash
xcodebuild -project jnexus-ios/JNexus.xcodeproj \
    -scheme JNexus-iOS \
    -configuration Release \
    -destination 'generic/platform=iOS'
```

### Running Tests

```bash
xcodebuild test -project jnexus-ios/JNexus.xcodeproj \
    -scheme JNexus-iOS \
    -destination 'platform=iOS Simulator,name=iPhone 15'
```

## Video Tutorials

These video tutorials cover iOS/iPadOS app workflows:

- [JNexus on iOS and macOS](../video-tutorials/scripts/08-jnexus-ios-macos.md) (5 min) -- iOS interface tour, iPad split view, and Keychain integration
- [JNexus in 90 Seconds](../video-tutorials/scripts/01-jnexus-in-90-seconds.md) (1.5 min) -- Quick overview including mobile demo
- [Troubleshooting Common Issues](../video-tutorials/scripts/10-troubleshooting.md) (4 min) -- Connection, auth, and timeout troubleshooting

See the [Video Tutorials catalog](../video-tutorials/README.md) for all available tutorials.

Back to [Getting Started](../getting-started.md) | [All Guides](../getting-started.md#platform-guides)

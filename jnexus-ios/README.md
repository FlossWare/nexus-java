# JNexus - iOS/iPadOS/macOS Native App

Native Swift application for managing Sonatype Nexus repositories on Apple platforms.

## Overview

JNexus provides a native Swift experience for iOS, iPadOS, and macOS with feature parity to the Android app. Built using SwiftUI with ~95% code sharing across platforms.

## Platforms

- **iOS**: iPhone (iOS 16.0+)
- **iPadOS**: iPad with optimized layouts (iOS 16.0+)
- **macOS**: Native macOS app (macOS 13.0/Ventura+)

## Features

### Core Functionality
- **List Components**: Browse repository contents with caching
- **Advanced Search**: Filter by size, date, extension, regex pattern
- **Repository Statistics**: Analytics with size distribution, file types, age analysis
- **Component Management**: View metadata, delete with confirmation

### Platform-Specific Features

**iOS/iPad**:
- TabView navigation with 4 tabs (List, Search, Stats, Settings)
- Swipe-to-delete gestures
- iPad: Split view in landscape, adaptive layouts
- iPad: Multitasking support (Split View, Slide Over)

**macOS**:
- Sidebar navigation
- Menu bar with keyboard shortcuts (⌘L, ⌘R, ⌘F, ⌘,)
- Separate Settings window
- Multi-window support
- Window restoration

### Security
- **Keychain Storage**: All credentials encrypted in iOS/macOS Keychain
- **Hardware-backed**: Uses Secure Enclave when available
- **HTTPS Only**: No cleartext network traffic allowed
- **App Sandbox**: macOS app sandboxed for security

## Requirements

- **iOS**: iOS 16.0 or later
- **iPadOS**: iPadOS 16.0 or later
- **macOS**: macOS 13.0 (Ventura) or later
- **Nexus**: Sonatype Nexus Repository Manager 3.x
- **Network**: Internet connection for Nexus API access

## Installation

### Enterprise/Ad-Hoc Distribution

**iOS/iPadOS**:
1. Download IPA from [GitHub Releases](https://github.com/FlossWare/jnexus/releases)
2. Install via:
   - Direct download on device (iOS 16+)
   - Apple Configurator (USB install)
   - MDM system (enterprise)

**macOS**:
1. Download DMG from [GitHub Releases](https://github.com/FlossWare/jnexus/releases)
2. Open DMG and drag JNexus.app to Applications
3. First launch: Right-click → Open (bypass Gatekeeper)

### Building from Source

See [Building from Source](#building-from-source) section below.

## Usage

### First Launch - Settings Configuration

1. Open JNexus
2. Navigate to Settings tab (iOS/iPad) or Preferences (macOS: ⌘,)
3. Enter Nexus connection details:
   - **Nexus URL**: Your Nexus server (e.g., `https://nexus.example.com`)
   - **Username**: Nexus username
   - **Password**: Nexus password
4. (Optional) Configure repositories and defaults
5. Tap/click "Save Settings"

### List Components

1. Navigate to List tab/screen
2. Enter repository name
3. Tap "List Components" (cached) or "Refresh" (fresh data)
4. Tap component to view full metadata
5. Swipe left to delete (iOS/iPad) or right-click (macOS)

### Search with Filters

1. Navigate to Search tab/screen
2. Enter repository name
3. Expand "Filters" to configure:
   - Min/Max file size (bytes)
   - Created date range
   - File extension (e.g., `.jar`)
   - Regex pattern
4. Tap "Search"

### View Statistics

1. Navigate to Stats tab/screen
2. Enter repository name
3. Tap "Calculate Statistics"
4. View analytics:
   - Overview (total, average, median sizes)
   - Size distribution (5 buckets)
   - File type breakdown (top 10)
   - Age distribution (7/30/90 days)
   - Largest components (top 10)

## Architecture

### Multiplatform Structure

```
jnexus-ios/
├── Shared/                  (95% code reuse)
│   ├── Core/               (Protocols, service, models)
│   ├── Platform/           (URLSession, Keychain)
│   ├── Models/             (Data models)
│   └── UI/                 (Screens, components)
├── iOS/                    (iOS-specific)
│   ├── JNexusApp.swift    (App entry point)
│   └── ContentView.swift  (TabView navigation)
├── macOS/                  (macOS-specific)
│   ├── JNexusApp.swift    (App entry point)
│   ├── ContentView.swift  (Sidebar navigation)
│   └── MenuCommands.swift (Keyboard shortcuts)
└── Tests/                  (Unit and UI tests)
```

### Platform Abstractions

**NexusHttpClient Protocol**:
- iOS/macOS implementation: `NexusClientURLSession` (uses URLSession)
- Features: Caching (5-minute TTL), retry logic (exponential backoff), pagination

**Credentials Protocol**:
- iOS/macOS implementation: `CredentialsKeychain` (uses Keychain + UserDefaults)
- Secure storage: URL, username, password in Keychain (AES-256 encrypted)
- Settings: Repositories, defaults, timeout in UserDefaults

**NexusService**:
- Shared business logic across all platforms
- Filtering, statistics calculation, batch operations
- Platform-agnostic (no iOS/macOS dependencies)

### Data Models

All models are Swift structs conforming to `Codable` and `Identifiable`:
- `RepoRecord`: Basic component (id, fileSize, path)
- `ComponentMetadata`: Enhanced metadata (contentType, format, dates, checksum)
- `SearchCriteria`: Advanced filter criteria (Builder pattern)
- `RepositoryStats`: Analytics (distributions, breakdowns)

## Building from Source

### Prerequisites

1. **Xcode**: 15.0 or later
2. **macOS**: Monterey (12.0) or later
3. **Swift**: 5.9+ (included with Xcode)

### Create Xcode Project

Since Xcode project files are binary and cannot be committed to git, you need to create the project manually:

1. Open Xcode
2. Create new project: **File → New → Project**
3. Choose template: **Multiplatform → App**
4. Configuration:
   - Product Name: **JNexus**
   - Team: Your development team
   - Organization Identifier: **org.flossware**
   - Interface: **SwiftUI**
   - Language: **Swift**
   - Include Tests: **Yes**

5. Configure targets:
   - iOS: Minimum Deployment **iOS 16.0**
   - macOS: Minimum Deployment **macOS 13.0**

6. Add source files:
   - Add `Shared/` folder to both targets (iOS + macOS)
   - Add `iOS/` folder to iOS target only
   - Add `macOS/` folder to macOS target only
   - Add `Tests/` folder to test targets

7. Configure Info.plist:
   - iOS: Use `iOS/Info.plist`
   - macOS: Use `macOS/Info.plist`

8. Configure Entitlements:
   - macOS: Add `macOS/JNexus.entitlements`

### Build and Run

**iOS Simulator**:
```bash
# Command line
xcodebuild -scheme JNexus -sdk iphonesimulator -destination 'platform=iOS Simulator,name=iPhone 15' build

# Or in Xcode: Product → Destination → iPhone 15
```

**macOS**:
```bash
# Command line
xcodebuild -scheme JNexus -sdk macosx build

# Or in Xcode: Product → Destination → My Mac
```

### Run Tests

```bash
# All tests
xcodebuild test -scheme JNexus -sdk iphonesimulator -destination 'platform=iOS Simulator,name=iPhone 15'

# macOS tests
xcodebuild test -scheme JNexus -sdk macosx
```

Or in Xcode: **Product → Test** (⌘U)

## Distribution

### Ad-Hoc (Enterprise)

**iOS/iPadOS**:
1. Archive: **Product → Archive**
2. Organizer → Select archive → **Distribute App**
3. Choose: **Ad Hoc**
4. Select provisioning profile
5. Export IPA
6. Distribute via file sharing or MDM

**macOS**:
1. Archive: **Product → Archive**
2. Organizer → Select archive → **Distribute App**
3. Choose: **Developer ID** (or **Ad Hoc**)
4. Export app
5. (Optional) Notarize with Apple
6. Create DMG or ZIP
7. Distribute via download link

### Creating DMG (macOS)

```bash
# Install create-dmg
brew install create-dmg

# Create DMG
create-dmg \
  --volname "JNexus" \
  --window-pos 200 120 \
  --window-size 600 400 \
  --icon-size 100 \
  --icon "JNexus.app" 175 190 \
  --hide-extension "JNexus.app" \
  --app-drop-link 425 190 \
  "JNexus.dmg" \
  "JNexus.app"
```

## Troubleshooting

### iOS: "Untrusted Developer"

After installing IPA:
1. Open Settings → General → VPN & Device Management
2. Find your developer profile
3. Tap "Trust"

### macOS: "Cannot open because it is from an unidentified developer"

1. Right-click JNexus.app
2. Choose "Open"
3. Click "Open" in confirmation dialog
4. (Or) System Settings → Privacy & Security → "Open Anyway"

### Credentials not saving

- Check keychain access permissions
- iOS: Settings → Privacy → Keychain
- macOS: Keychain Access app → Check for denied items

### Network connection fails

- Verify Nexus URL is HTTPS (HTTP not allowed)
- Exception: Local network (http://localhost, http://192.168.x.x)
- Check firewall settings
- Verify Nexus is accessible from device

## Keyboard Shortcuts (macOS)

| Shortcut | Action |
|----------|--------|
| ⌘L | List Components |
| ⌘R | Refresh |
| ⌘F | Search |
| ⌘, | Preferences |
| ⌘N | New Window |
| ⌘W | Close Window |
| ⌘Q | Quit |

## Technical Details

### Dependencies

No external dependencies - uses only Apple frameworks:
- **SwiftUI**: UI framework
- **Foundation**: Core functionality
- **Security**: Keychain access
- **Combine**: Reactive programming (future use)

### Data Storage

- **Keychain**: Credentials (URL, username, password) - AES-256 encrypted
- **UserDefaults**: Settings (repositories, defaults, timeout) - unencrypted
- **Memory**: HTTP cache (5-minute TTL) - not persisted

### HTTP Client Features

- **Caching**: 5-minute TTL per repository
- **Retry Logic**: 3 attempts with exponential backoff (1s, 2s, 4s)
- **Pagination**: Automatic continuation token handling
- **Timeout**: Configurable (default 30 seconds)
- **Authentication**: HTTP Basic Auth

### Performance

- **Startup**: < 1 second
- **List**: ~1-2 seconds (10,000 components, cached)
- **Search**: ~2-3 seconds (filtered client-side)
- **Stats**: ~3-5 seconds (calculated client-side)
- **Memory**: < 50 MB typical usage

## Contributing

See [CONTRIBUTING.md](../CONTRIBUTING.md) for development guidelines.

## License

Same license as main JNexus project.

## Support

- **Issues**: https://github.com/FlossWare/jnexus/issues
- **Main Documentation**: https://github.com/FlossWare/jnexus

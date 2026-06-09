# JNexus macOS App Guide

Back to [Getting Started](../getting-started.md) | [Configuration](../advanced/configuration-guide.md)

## Overview

The JNexus macOS app provides a native desktop experience for managing Nexus repositories on Mac. Built with SwiftUI, it features sidebar navigation, menu bar integration, keyboard shortcuts, and multi-window support -- all with zero external dependencies.

## System Requirements

- macOS 13.0 (Ventura) or higher
- Internet access to your Nexus server

## Installation

### From GitHub Releases

1. Download the DMG from [GitHub Releases](https://github.com/FlossWare/nexus-java/releases/tag/macos-v1.0)
2. Open the DMG and drag JNexus to your Applications folder
3. On first launch, right-click and select "Open" to bypass Gatekeeper (unsigned app)

### Build from Source

Requires a Mac with Xcode 15+:

1. Clone the repository:
   ```bash
   git clone https://github.com/FlossWare/nexus-java.git
   ```
2. Open `jnexus-ios/JNexus.xcodeproj` in Xcode
3. Select the **JNexus-macOS** scheme
4. Build and run (Cmd+R)

## First Launch

### Configure Credentials

1. Open JNexus
2. Navigate to **Settings** (via sidebar or Cmd+,)
3. Enter your Nexus server details:
   - URL, username, password
   - Repository list (optional)
   - Default values (optional)
   - HTTP timeout (optional)
4. Click **Save**
5. Credentials are encrypted via Keychain Services (AES-256 hardware-backed)

## Interface Tour

### Sidebar Navigation

Unlike the iOS/iPadOS version which uses a bottom tab bar, the macOS app uses a NavigationSplitView with a sidebar:

```
+------------------+---------------------------------------------+
| Sidebar          | Detail View                                 |
|                  |                                             |
| > List           | (Content for selected section)              |
| > Search         |                                             |
| > Statistics     |                                             |
| > Settings       |                                             |
|                  |                                             |
+------------------+---------------------------------------------+
```

Click any item in the sidebar to show its content in the detail view.

### Menu Bar

The macOS app integrates with the system menu bar via MenuCommands.swift:

| Menu | Item | Shortcut | Action |
|------|------|----------|--------|
| File | New Window | Cmd+N | Open a new window |
| Edit | Clear Cache | -- | Clear all cached data |
| View | Refresh | Cmd+R | Refresh current view |
| Tools | List Components | Cmd+L | Navigate to List view |
| Tools | Search | Cmd+F | Navigate to Search view |
| Tools | Statistics | -- | Navigate to Statistics view |
| JNexus | Settings | Cmd+, | Open Settings |
| JNexus | Quit | Cmd+Q | Quit application |

### List View

Same functionality as iOS but with macOS-optimized layout:
- Repository Picker in the toolbar
- List and Refresh buttons
- Component rows with full detail
- Click component for metadata
- Delete with confirmation

### Search View

Same filtering capabilities as iOS:
- Size range filters
- Date range filters (using native macOS DatePicker)
- Extension and regex filters
- Collapsible sections via DisclosureGroup

### Statistics View

Same analytics as iOS but displayed in a wider layout:
- Overview metrics with GroupBox
- Size distribution with progress bars
- File type breakdown
- Age distribution
- Largest components list

### Settings View

On macOS, Settings opens as a separate window (standard macOS Settings pattern) via Cmd+, or the JNexus menu:
- Same fields as iOS (URL, user, password, repositories, defaults, timeout)
- SecureField with show/hide toggle
- Save and Clear buttons
- Keychain encryption info

## Multi-Window Support

The macOS app supports multiple windows:
- **Cmd+N**: Open a new window
- Each window operates independently
- Useful for comparing repositories or viewing statistics while browsing

## Keyboard Shortcuts

| Shortcut | Action |
|----------|--------|
| Cmd+L | Navigate to List view |
| Cmd+R | Refresh current view |
| Cmd+F | Navigate to Search view |
| Cmd+, | Open Settings |
| Cmd+N | New window |
| Cmd+W | Close window |
| Cmd+Q | Quit application |

## macOS vs iOS/iPadOS Differences

| Feature | macOS | iOS/iPadOS |
|---------|-------|------------|
| Navigation | Sidebar (NavigationSplitView) | Bottom tabs (TabView) |
| Settings | Separate window (Cmd+,) | Tab in main window |
| Keyboard shortcuts | Full menu bar commands | Limited system shortcuts |
| Multi-window | Yes (Cmd+N) | No |
| Delete gesture | Click + confirm | Swipe left + confirm |
| Layout | Fixed sidebar + detail | Adaptive portrait/landscape |

## Security

### Keychain Storage

Same as iOS:
- AES-256 hardware-backed encryption via Keychain Services
- Access level: `kSecAttrAccessibleWhenUnlocked`
- System keychain, sandboxed to the app

### App Sandbox

The macOS app runs in the App Sandbox with these entitlements:
- **Network client**: Allowed (for Nexus API communication)
- **Keychain access**: Allowed (for credential storage)
- **File system**: Restricted to app container

## Architecture

The macOS app shares 95% of its code with the iOS app:
- **Shared code** (95%): Core protocols, HTTP client, service layer, data models, UI screens
- **macOS-specific** (5%): App entry point, ContentView (sidebar navigation), MenuCommands, entitlements

See the [iOS Guide](ios-guide.md) for details on the shared architecture.

## Tips and Tricks

1. **Cmd+, for Settings**: Use the standard macOS keyboard shortcut to access settings
2. **Multi-window**: Open multiple windows to compare different repositories side by side
3. **Menu bar shortcuts**: Use Cmd+L to quickly jump to the List view, Cmd+F for Search
4. **Keychain Access app**: Use the macOS Keychain Access utility to inspect stored credentials (service: `com.flossware.jnexus`)
5. **Resize sidebar**: Drag the sidebar divider to adjust width

## Troubleshooting

| Issue | Solution |
|-------|----------|
| App won't open (Gatekeeper) | Right-click > Open, or System Settings > Security > Allow |
| `Network error` | Check internet connection and Nexus URL |
| `401 Unauthorized` | Verify credentials in Settings (Cmd+,) |
| Keychain prompt | Allow JNexus to access Keychain when prompted |
| App not showing in Dock | Check if app is in Applications folder |
| Sidebar missing | Resize window wider or use menu bar to navigate |

## Building from Source

### Xcode Build

1. Open `jnexus-ios/JNexus.xcodeproj`
2. Select scheme: **JNexus-macOS**
3. Build: Cmd+B
4. Run: Cmd+R

### Command Line Build

```bash
xcodebuild -project jnexus-ios/JNexus.xcodeproj \
    -scheme JNexus-macOS \
    -configuration Release
```

### Creating a Distribution Build

1. Archive: Product > Archive in Xcode
2. Distribute: Organizer > Distribute App
3. Select: Developer ID signed, notarized (for non-App Store distribution)

## Video Tutorials

These video tutorials cover macOS app workflows:

- [JNexus on iOS and macOS](../video-tutorials/scripts/08-jnexus-ios-macos.md) (5 min) -- macOS sidebar navigation, keyboard shortcuts, and multi-window support
- [JNexus in 90 Seconds](../video-tutorials/scripts/01-jnexus-in-90-seconds.md) (1.5 min) -- Quick overview across all platforms
- [Multi-Profile Configuration](../video-tutorials/scripts/09-multi-profile-config.md) (3 min) -- Managing multiple Nexus environments

See the [Video Tutorials catalog](../video-tutorials/README.md) for all available tutorials.

Back to [Getting Started](../getting-started.md) | [All Guides](../getting-started.md#platform-guides)

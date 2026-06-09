# 3. Seven Distinct User Interfaces

**Status:** Accepted

**Date:** 2024-Present (evolved over time)

**Deciders:** Scot P. Floess, Development Team

## Context

JNexus serves diverse user bases across multiple platforms with different requirements:

- **DevOps engineers**: Need CLI for scripting and CI/CD automation
- **Desktop users**: Prefer graphical interfaces for interactive work
- **Remote administrators**: SSH sessions without X11 forwarding
- **Legacy systems**: Older Java installations or constrained environments
- **Mobile users (Android)**: On-the-go repository management from phones and tablets
- **Apple ecosystem users**: Native experience on iPhone, iPad, and Mac

A single UI approach cannot satisfy all these use cases. Each user group has different priorities for usability, compatibility, deployment scenarios, and platform expectations.

## Decision

Implement **seven distinct user interfaces** across three platforms, each sharing the same core business logic:

### 1. CLI (JNexus.java) - Picocli-based Command Line
**Use case**: Scripting, automation, CI/CD pipelines

**Features:**
- Three commands: `list`, `delete`, `stats`
- Advanced filtering options (size, date, extension, regex)
- JSON output for scripting (`--format json`)
- Dry-run mode for safe testing
- Verbose and quiet modes

**Advantages:**
- Scriptable and composable with other tools
- Ideal for automated cleanup jobs
- Minimal resource usage
- Non-interactive execution

**Launcher:** `./jnexus.sh [command] [options]`

### 2. Swing GUI (JNexusSwing.java) - Modern Graphical Interface
**Use case**: Desktop users who prefer modern GUIs with advanced features

**Features:**
- JTable-based display with 7 columns (ID, sizes in bytes/MB/GB, date, type, path)
- Sortable columns with numeric sorting
- Multi-row selection and bulk operations
- Advanced filters panel (collapsible)
- Statistics dialog with 5 tabs (overview, distribution, file types, age, largest)
- Component details dialog (double-click to view metadata)
- SwingWorker for non-blocking background tasks
- Native look and feel (platform-specific appearance)

**Advantages:**
- Spreadsheet-like interface familiar to users
- Rich analytics and visualization
- Responsive UI with modern features
- Native OS appearance

**Launcher:** `./jnexus-swing.sh`

### 3. AWT GUI (JNexusAWT.java) - Classic Graphical Interface
**Use case**: Maximum compatibility, remote desktop, VNC, legacy systems

**Features:**
- Pure AWT components (Frame, Button, TextField, TextArea, Choice)
- Text-based output with column alignment
- Repository dropdown selector
- Direct Thread usage for background tasks
- No Swing dependencies

**Advantages:**
- Works on very old Java installations (AWT since Java 1.0)
- Smaller memory footprint than Swing
- Better compatibility with remote desktop protocols
- Reliable on headless systems with X11 forwarding

**Launcher:** `./jnexus-awt.sh`

### 4. Terminal UI (JNexusUI.java) - ncurses-based Full-Screen Interface
**Use case:** SSH sessions, terminal users, servers without GUI

**Features:**
- Full-screen ncurses interface (via jcurses library)
- Keyboard navigation (TAB, arrows, SPACE)
- Text fields, buttons, checkboxes
- Repository list display
- No X11 required

**Advantages:**
- Works over pure SSH (no X11 forwarding needed)
- Familiar to terminal-based admins
- Low bandwidth usage
- Native feel in terminal environment

**Launcher:** `./jnexus-ui.sh`

### 5. Android App (JNexus Android) - Jetpack Compose
**Use case**: Mobile repository management on Android phones and tablets

**Features:**
- Material Design 3 UI with Jetpack Compose
- Repository browsing with search and filtering
- Component details with metadata display
- Statistics dashboard with visual charts
- Delete operations with confirmation dialogs
- Profile management for multiple Nexus instances
- Offline-capable with local caching

**Advantages:**
- Native Android experience (Material Design, gestures, notifications)
- On-the-go repository management
- Push notification potential for monitoring
- Touch-optimized interface

**Distribution:** APK (sideload), planned Google Play Store and F-Droid

### 6. iOS App (JNexus iOS) - SwiftUI
**Use case**: Mobile repository management on iPhone and iPad

**Features:**
- Native SwiftUI interface with iOS design language
- Repository browsing and search
- Component metadata display
- Statistics views
- Secure credential storage via Keychain Services
- Profile-based configuration

**Advantages:**
- Native iOS experience (haptics, gestures, system integration)
- Keychain-based credential security (hardware-backed)
- iPad multitasking support
- System-level dark mode and accessibility

**Distribution:** Xcode build (direct install)

### 7. macOS App (JNexus macOS) - SwiftUI
**Use case**: Native desktop experience on Mac

**Features:**
- Native macOS SwiftUI interface with menu bar
- Shares 95% code with iOS app (Shared/ directory)
- Keyboard shortcuts and macOS conventions
- Touch Bar support (where available)
- Keychain Services for credential storage
- Multi-window support

**Advantages:**
- Native Mac experience (menu bar, keyboard shortcuts, Dock integration)
- Seamless Apple ecosystem integration
- Code reuse with iOS app
- macOS Keychain security

**Distribution:** Xcode build (direct install)

## Consequences

### Positive

- **User choice**: Each user group gets optimal interface for their workflow
- **Flexibility**: Same backend, multiple frontends - choose based on environment
- **Compatibility spectrum**: From modern desktop to pure terminal to mobile
- **Learning path**: CLI users can try GUI, GUI users can automate with CLI
- **Fallback options**: If one UI doesn't work, try another
- **Platform coverage**: Desktop, mobile, and tablet all supported
- **Native experiences**: Each platform uses its native UI toolkit

### Negative

- **Maintenance burden**: Seven UIs to maintain and test across three platforms
- **Feature parity**: New features must be added to all UIs (or documented as platform-specific)
- **Code duplication**: Some UI logic duplicated across implementations
- **Documentation overhead**: Each UI needs separate usage instructions
- **Testing complexity**: Must test each UI separately, across platforms
- **Three programming languages**: Java (desktop), Kotlin (Android), Swift (iOS/macOS)
- **Three build systems**: Maven (desktop), Gradle (Android/core), Xcode (iOS/macOS)

### Accepted Tradeoffs

The maintenance burden is acceptable because:
1. All UIs share the same backend logic (NexusService, NexusHttpClient)
2. UI code is relatively simple (thin presentation layer)
3. Each UI serves distinct, non-overlapping use cases
4. Users strongly prefer their chosen interaction model
5. iOS and macOS share 95% of code (only 5% platform-specific)
6. Android and desktop share jnexus-core business logic

## Design Patterns

All UIs follow the same pattern:
```java
// 1. Load credentials
Credentials credentials = new Credentials(profile);

// 2. Create client and service
NexusClient client = new NexusClient(credentials);
NexusService service = new NexusService(client);

// 3. Execute operations
service.listRepository(repository, regex);
service.deleteComponents(repository, regex, dryRun);
service.calculateStatistics(repository);
```

**Shared behavior:**
- Background task execution (SwingWorker/Thread/direct)
- Profile selection when multiple profiles exist
- Repository dropdown from `credentials.getRepositories()`
- Pre-populated defaults from `credentials.getDefault*()`
- Same caching and retry logic (in NexusClient)

**UI-specific behavior:**
- Output formatting (table vs. text vs. ncurses)
- User interaction (mouse vs. keyboard)
- Progress indication (cursor vs. status label vs. text output)

## Alternatives Considered

### Alternative 1: Single GUI with mode flags
- **Approach**: One UI with `--gui`, `--terminal`, `--headless` flags
- **Rejected because**:
  - Complex conditional logic for rendering
  - Hard to optimize for each environment
  - Poor user experience (generic UI)
  - Difficult to maintain

### Alternative 2: Web-based UI (browser)
- **Approach**: Embedded HTTP server with web interface
- **Rejected because**:
  - Requires network port and potential security concerns
  - Heavier dependencies (web server, HTML/CSS/JS)
  - Not suitable for simple CLI tasks
  - Overkill for single-user tool
  - (Could be added later as 5th option)

### Alternative 3: CLI only, users build their own UIs
- **Approach**: Provide only CLI, let users script their own interfaces
- **Rejected because**:
  - High barrier to entry for non-technical users
  - Limits adoption among GUI-preferring users
  - Nexus management benefits from visual inspection

### Alternative 4: JavaFX GUI
- **Approach**: Use JavaFX instead of Swing
- **Rejected because**:
  - JavaFX not included in JDK 21 (separate dependency)
  - Swing is more widely supported
  - AWT provides even better compatibility
  - Adds complexity without clear benefit

## Usage Distribution

Based on launcher scripts, app availability, and documentation:
- **CLI**: Estimated 30% (automation, scripts, power users)
- **Swing GUI**: Estimated 25% (interactive desktop users)
- **Android App**: Estimated 15% (mobile users, on-the-go management)
- **AWT GUI**: Estimated 10% (remote desktop, VNC, legacy systems)
- **iOS App**: Estimated 8% (iPhone/iPad users)
- **macOS App**: Estimated 7% (Mac-native users)
- **Terminal UI**: Estimated 5% (SSH users, terminal enthusiasts)

## References

- CHANGELOG.md: v1.0 GUI additions
- README.md: Four interface descriptions
- CLAUDE.md: UI Implementation Patterns section

## Impact

**Positive:**
- Broad user base adoption across different environments and platforms
- Users can choose based on preference, platform, and constraints
- Demonstrates flexibility of layered architecture
- Native experiences on every major platform

**Negative:**
- ~7x maintenance work for UI updates (across three languages)
- Occasional feature parity gaps (e.g., Swing got statistics dialog first)
- Documentation must cover all seven UIs across three platforms
- Three different build systems to maintain

## Future Considerations

**Possible 8th option: Web UI**
- Could add embedded web server for browser-based interface
- Would enable remote management via HTTP
- Mobile-friendly responsive design
- Not mutually exclusive with existing UIs

**Platform coverage achieved:**
- Desktop: CLI, Swing, AWT, Terminal UI ✅
- Android: Native Jetpack Compose app ✅
- iOS/iPadOS: Native SwiftUI app ✅
- macOS: Native SwiftUI app ✅

## Related Decisions

- ADR-0001: Picocli over Spring Boot (enabled lightweight multi-UI architecture)
- ADR-0002: Multi-module architecture (same backend for all UIs)
- ADR-0006: Interface-based HTTP client (enables platform-specific HTTP implementations)

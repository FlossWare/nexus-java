# 3. Four UI Approaches for Desktop

**Status:** Accepted

**Date:** 2024-Present (evolved over time)

**Deciders:** Scot P. Floess, Development Team

## Context

JNexus desktop application (Java 21) serves diverse user bases with different requirements:

- **DevOps engineers**: Need CLI for scripting and CI/CD automation
- **Desktop users**: Prefer graphical interfaces for interactive work
- **Remote administrators**: SSH sessions without X11 forwarding
- **Legacy systems**: Older Java installations or constrained environments

A single UI approach cannot satisfy all these use cases. Each user group has different priorities for usability, compatibility, and deployment scenarios.

## Decision

Implement **four distinct user interfaces** sharing the same backend:

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

## Consequences

### Positive

- **User choice**: Each user group gets optimal interface for their workflow
- **Flexibility**: Same backend, multiple frontends - choose based on environment
- **Compatibility spectrum**: From modern desktop to pure terminal
- **Learning path**: CLI users can try GUI, GUI users can automate with CLI
- **Fallback options**: If one UI doesn't work, try another

### Negative

- **Maintenance burden**: Four UIs to maintain and test
- **Feature parity**: New features must be added to all UIs
- **Code duplication**: Some UI logic duplicated across implementations
- **Documentation overhead**: Each UI needs separate usage instructions
- **Testing complexity**: Must test each UI separately

### Accepted Tradeoffs

The maintenance burden is acceptable because:
1. All UIs share the same backend (NexusClient, NexusService)
2. UI code is relatively simple (thin presentation layer)
3. Each UI serves distinct, non-overlapping use cases
4. Users strongly prefer their chosen interaction model

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

Based on launcher scripts and documentation:
- **CLI**: Estimated 40% (automation, scripts, power users)
- **Swing GUI**: Estimated 35% (interactive desktop users)
- **AWT GUI**: Estimated 15% (remote desktop, VNC, legacy systems)
- **Terminal UI**: Estimated 10% (SSH users, terminal enthusiasts)

## References

- CHANGELOG.md: v1.0 GUI additions
- README.md: Four interface descriptions
- CLAUDE.md: UI Implementation Patterns section

## Impact

**Positive:**
- Broad user base adoption across different environments
- Users can choose based on preference and constraints
- Demonstrates flexibility of layered architecture

**Negative:**
- ~4x maintenance work for UI updates
- Occasional feature parity gaps (e.g., Swing got statistics dialog first)
- Documentation must cover all four UIs

## Future Considerations

**Possible 5th option: Web UI**
- Could add embedded web server for browser-based interface
- Would enable remote management via HTTP
- Mobile-friendly responsive design
- Not mutually exclusive with existing UIs

**Possible mobile apps:**
- Android app added in v1.2 ✅
- iOS app added in v1.0 ✅
- Native mobile apps complement desktop UIs

## Related Decisions

- ADR-0001: Picocli over Spring Boot (enabled lightweight multi-UI architecture)
- ADR-0002: Multi-module architecture (same backend for all UIs)

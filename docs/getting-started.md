# JNexus - Getting Started

Back to [README](../README.md) | [Platform Guides](#platform-guides) | [Advanced Topics](advanced/configuration-guide.md)

## What is JNexus?

JNexus is a cross-platform tool for managing components in Sonatype Nexus Repository Manager. It lets you list, search, delete, and analyze artifacts across your Nexus repositories.

JNexus is available on 7 different interfaces across 4 platforms:

| Platform | Interface | Best For |
|----------|-----------|----------|
| Desktop | [CLI](guides/cli-guide.md) | Scripting, automation, CI/CD |
| Desktop | [Swing GUI](guides/swing-gui-guide.md) | Desktop users wanting a modern GUI |
| Desktop | [AWT GUI](guides/awt-gui-guide.md) | Maximum compatibility, remote desktop |
| Desktop | [Terminal UI](guides/terminal-ui-guide.md) | SSH sessions, terminal-only environments |
| Mobile | [Android](guides/android-guide.md) | On-the-go repository management |
| Mobile | [iOS/iPadOS](guides/ios-guide.md) | Apple mobile device users |
| Desktop | [macOS](guides/macos-guide.md) | Native Mac desktop experience |

## Quick Start (5 Minutes)

### Step 1: Choose Your Platform

**Desktop (Java)** -- requires Java 21:
```bash
# Build from source
git clone https://github.com/FlossWare/nexus-java.git
cd nexus-java
./mvnw clean package
```

**Android** -- requires Android 8.0+:
- Download the APK from [GitHub Releases](https://github.com/FlossWare/nexus-java/releases/latest)

**iOS/iPadOS** -- requires iOS 16.0+:
- Download the IPA from [GitHub Releases](https://github.com/FlossWare/nexus-java/releases/tag/ios-v1.0)

**macOS** -- requires macOS 13.0+:
- Download the DMG from [GitHub Releases](https://github.com/FlossWare/nexus-java/releases/tag/macos-v1.0)

### Step 2: Configure Credentials

All platforms need the same three pieces of information:
- **Nexus URL**: Your Nexus server address (e.g., `https://nexus.example.com`)
- **Username**: Your Nexus account username
- **Password**: Your Nexus account password (or user token)

**Desktop -- Environment Variables (recommended for CI/CD):**
```bash
export NEXUS_URL=https://nexus.example.com
export NEXUS_USER=your-username
export NEXUS_PASSWORD=your-password
```

**Desktop -- Properties File (recommended for interactive use):**
```bash
mkdir -p ~/.flossware/nexus
cat > ~/.flossware/nexus/nexus.properties << 'EOF'
nexus.url=https://nexus.example.com
nexus.user=your-username
nexus.password=your-password
EOF
chmod 600 ~/.flossware/nexus/nexus.properties
```

**Android/iOS/macOS:**
Open the Settings screen in the app and enter your credentials. They are encrypted automatically (AES-256).

### Step 3: List Components

**CLI:**
```bash
./jnexus.sh list maven-releases
```

**GUI (Swing/AWT/Terminal):**
1. Launch the GUI (e.g., `./jnexus-swing.sh`)
2. Select a repository from the dropdown
3. Click "List"

**Mobile/macOS:**
1. Open the app
2. Go to the List tab
3. Select a repository and tap List/Refresh

### Step 4: Explore Further

Once you can list components, try these common operations:

**Filter by pattern:**
```bash
./jnexus.sh list maven-releases ".*SNAPSHOT.*"
```

**Preview deletions (dry-run):**
```bash
./jnexus.sh delete --dry-run maven-snapshots ".*old-version.*"
```

**View statistics:**
```bash
./jnexus.sh stats maven-releases
```

## Platform Guides

Detailed guides for each interface:

- [CLI Guide](guides/cli-guide.md) -- Command reference, scripting examples, advanced filtering
- [Swing GUI Guide](guides/swing-gui-guide.md) -- Table interface, statistics dialog, keyboard shortcuts
- [AWT GUI Guide](guides/awt-gui-guide.md) -- Classic interface, compatibility notes
- [Terminal UI Guide](guides/terminal-ui-guide.md) -- Keyboard navigation, ncurses setup
- [Android Guide](guides/android-guide.md) -- Installation, screens, gestures
- [iOS/iPadOS Guide](guides/ios-guide.md) -- Installation, tab navigation, iPad layouts
- [macOS Guide](guides/macos-guide.md) -- Sidebar navigation, menu bar, keyboard shortcuts

## Advanced Topics

- [Configuration Guide](advanced/configuration-guide.md) -- Profiles, environment variables, encryption
- [Filtering Guide](advanced/filtering-guide.md) -- Regex patterns, size/date/extension filters
- [Statistics Guide](advanced/statistics-guide.md) -- Analytics, size distribution, reporting
- [Troubleshooting](advanced/troubleshooting.md) -- Common errors and solutions
- [Best Practices](advanced/best-practices.md) -- Security, automation, performance tips

## Common Workflows

### Cleaning Up Old Snapshots

1. List SNAPSHOT components:
   ```bash
   ./jnexus.sh list maven-snapshots ".*SNAPSHOT.*"
   ```
2. Preview what would be deleted:
   ```bash
   ./jnexus.sh delete --dry-run maven-snapshots ".*SNAPSHOT.*"
   ```
3. Delete (with confirmation prompt):
   ```bash
   ./jnexus.sh delete maven-snapshots ".*SNAPSHOT.*"
   ```

### Analyzing Repository Size

```bash
./jnexus.sh stats maven-releases
```

This shows total size, size distribution, file type breakdown, age distribution, and the 20 largest components.

### Finding Large Artifacts

```bash
./jnexus.sh list maven-releases --min-size 104857600 --show-metadata
```

This lists all artifacts larger than 100 MB with full metadata.

## Video Tutorials

Prefer watching over reading? Check out these video tutorials:

| # | Title | Length | Best For |
|---|-------|--------|----------|
| 1 | [JNexus in 90 Seconds](video-tutorials/scripts/01-jnexus-in-90-seconds.md) | 1:30 | Quick overview for first-time visitors |
| 2 | [Getting Started with CLI](video-tutorials/scripts/02-getting-started-cli.md) | 5:00 | CLI installation and basic usage |
| 3 | [Swing GUI Walkthrough](video-tutorials/scripts/03-swing-gui-walkthrough.md) | 6:00 | Desktop GUI tour |
| 4 | [Safe Deletion with Dry-Run](video-tutorials/scripts/04-safe-deletion-dry-run.md) | 3:00 | Safety features demo |
| 5 | [Advanced Filtering](video-tutorials/scripts/05-advanced-filtering.md) | 4:00 | Power-user filtering |
| 6 | [Repository Statistics](video-tutorials/scripts/06-repository-statistics.md) | 5:00 | Analytics deep dive |
| 7 | [JNexus on Android](video-tutorials/scripts/07-jnexus-android.md) | 4:00 | Android app tour |
| 8 | [JNexus on iOS and macOS](video-tutorials/scripts/08-jnexus-ios-macos.md) | 5:00 | Apple platform tour |
| 9 | [Multi-Profile Configuration](video-tutorials/scripts/09-multi-profile-config.md) | 3:00 | Multi-environment setup |
| 10 | [Troubleshooting](video-tutorials/scripts/10-troubleshooting.md) | 4:00 | Common issues and fixes |

See the [Video Tutorials catalog](video-tutorials/README.md) for full descriptions and the [Production Guide](video-tutorials/PRODUCTION_GUIDE.md) for recording details.

## Need Help?

- [Troubleshooting Guide](advanced/troubleshooting.md)
- [Video Tutorials](video-tutorials/README.md) -- Visual walkthroughs for all platforms
- [GitHub Issues](https://github.com/FlossWare/nexus-java/issues)
- [RUNNING.md](../RUNNING.md) -- Detailed running instructions
- [README.md](../README.md) -- Project overview

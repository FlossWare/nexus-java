# JNexus Video Tutorials

This directory contains scripts, storyboards, and production notes for JNexus video tutorials.

## Video Catalog

| # | Title | Length | Audience | Script |
|---|-------|--------|----------|--------|
| 1 | [JNexus in 90 Seconds](#1-jnexus-in-90-seconds) | 1:30 | First-time visitors | [Script](scripts/01-jnexus-in-90-seconds.md) |
| 2 | [Getting Started with JNexus CLI](#2-getting-started-with-jnexus-cli) | 5:00 | CLI users, DevOps engineers | [Script](scripts/02-getting-started-cli.md) |
| 3 | [JNexus Swing GUI Walkthrough](#3-jnexus-swing-gui-walkthrough) | 6:00 | Desktop GUI users | [Script](scripts/03-swing-gui-walkthrough.md) |
| 4 | [Safe Deletion with Dry-Run Mode](#4-safe-deletion-with-dry-run-mode) | 3:00 | Anyone doing deletions | [Script](scripts/04-safe-deletion-dry-run.md) |
| 5 | [Advanced Filtering and Search](#5-advanced-filtering-and-search) | 4:00 | Power users | [Script](scripts/05-advanced-filtering.md) |
| 6 | [Repository Statistics and Analytics](#6-repository-statistics-and-analytics) | 5:00 | Repository managers | [Script](scripts/06-repository-statistics.md) |
| 7 | [JNexus on Android](#7-jnexus-on-android) | 4:00 | Mobile users | [Script](scripts/07-jnexus-android.md) |
| 8 | [JNexus on iOS and macOS](#8-jnexus-on-ios-and-macos) | 5:00 | Apple ecosystem users | [Script](scripts/08-jnexus-ios-macos.md) |
| 9 | [Multi-Profile Configuration](#9-multi-profile-configuration) | 3:00 | Users with multiple environments | [Script](scripts/09-multi-profile-config.md) |
| 10 | [Troubleshooting Common Issues](#10-troubleshooting-common-issues) | 4:00 | Users experiencing problems | [Script](scripts/10-troubleshooting.md) |

## Documentation

- **[PRODUCTION_GUIDE.md](PRODUCTION_GUIDE.md)** - Recording setup, branding, and publishing instructions
- **[IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md)** - Step-by-step implementation guide with timeline, workflow, and success criteria

## Video Descriptions

### 1. JNexus in 90 Seconds

A quick overview demonstrating the core value proposition: list, filter, and delete Nexus components across CLI, GUI, and mobile interfaces.

### 2. Getting Started with JNexus CLI

Step-by-step tutorial covering installation, configuration via `nexus.properties`, the list command, delete with dry-run, and the statistics command.

### 3. JNexus Swing GUI Walkthrough

Complete tour of the Swing desktop interface: credential setup, interface layout, listing and refreshing, advanced filter panel, multi-select deletion, and the statistics dialog.

### 4. Safe Deletion with Dry-Run Mode

Focused tutorial on using dry-run mode to preview deletions before committing. Covers both CLI (`--dry-run`) and GUI (checkbox) workflows.

### 5. Advanced Filtering and Search

Power-user tutorial covering regex patterns, size range filters, date range filters, file extension filters, and combining multiple filters for precise results.

### 6. Repository Statistics and Analytics

Deep dive into the statistics feature: overview metrics, size distribution buckets, file type breakdown, age distribution, and the largest components list.

### 7. JNexus on Android

Mobile-focused tutorial: APK installation, settings and credential configuration, list screen navigation, search screen with filters, stats screen, and swipe gestures.

### 8. JNexus on iOS and macOS

Apple platform tutorial: installation, iOS interface tour, iPad split view, macOS sidebar navigation, keyboard shortcuts, multi-window support, and Keychain integration.

### 9. Multi-Profile Configuration

Advanced tutorial on managing multiple Nexus environments: creating profile-specific property files, switching profiles via CLI flag or environment variable, and environment-specific workflows.

### 10. Troubleshooting Common Issues

Support-focused tutorial covering connection errors, authentication failures, timeout configuration, cache problems, and where to find help.

## Getting Started

Follow the [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md) to produce and publish all videos. The plan includes:

1. **Phase 1:** Channel setup (week 1)
2. **Phase 2:** Video production (weeks 2-4)
3. **Phase 3:** Editing (weeks 2-4 parallel)
4. **Phase 4:** Publishing (weeks 3-4 parallel)
5. **Phase 5:** Documentation integration (week 5)
6. **Phase 6:** Promotion and metrics (ongoing)

**Estimated effort:** 40-50 hours over 4-6 weeks

## Publishing Checklist

- [ ] YouTube channel "FlossWare" created
- [ ] Playlist "JNexus Tutorials" created
- [ ] All 10 videos recorded and edited
- [ ] Closed captions added to all videos (auto-generated + reviewed)
- [ ] Timestamps added to video descriptions
- [ ] GitHub links added to video descriptions
- [ ] Videos linked from project README.md
- [ ] Videos linked from relevant documentation guides
- [ ] Pinned tutorial thread created in GitHub Discussions

## Maintenance

- Update videos when UI changes significantly
- Add annotations for deprecated features
- Create "What's New" videos for major releases
- Monitor comments and questions for FAQ video ideas
- Track view counts and audience retention for content iteration

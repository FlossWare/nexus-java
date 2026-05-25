# 2. Multi-Module Architecture with jnexus-core

**Status:** Partially Implemented

**Date:** 2026-05-22

**Deciders:** Scot P. Floess, Development Team

## Context

With the introduction of the Android mobile app (v1.2), the project needed to share business logic between desktop and Android platforms. The original desktop implementation (src/) contained all logic in a single module, but Android required:

- Java 11 compatibility (Android desugaring supports Java 11 features on Android 8.0+)
- Platform abstraction for HTTP client (OkHttp instead of java.net.http)
- Platform abstraction for credential storage (EncryptedSharedPreferences instead of file-based)
- Shared data models (RepoRecord, ComponentMetadata, SearchCriteria, RepositoryStats)
- Shared business logic (NexusService for filtering, statistics, etc.)

Without a shared module, all business logic would be duplicated between desktop and Android, violating DRY principles and creating maintenance burden.

## Decision

Create a `jnexus-core` module as a shared Java 11 library containing:

**Interfaces for platform abstraction:**
- `NexusHttpClient`: HTTP operations (list, delete, caching)
- `Credentials`: Credential storage and retrieval

**Shared data models:**
- `RepoRecord`: Basic component record
- `ComponentMetadata`: Enhanced component with full metadata
- `SearchCriteria`: Advanced search filters
- `RepositoryStats`: Repository statistics

**Shared business logic:**
- `NexusService`: Filtering, statistics calculation, formatting

**Platform implementations:**
- **Android**: `NexusClientOkHttp` implements `NexusHttpClient`, `CredentialsAndroid` implements `Credentials`
- **Desktop**: (INTENDED) CredentialsFile implements `Credentials`, NexusClientHttp implements `NexusHttpClient`
- **iOS/macOS**: Swift protocols mirror Java interfaces (no code sharing due to language barrier)

## Consequences

### Positive

- **Code reuse**: Business logic written once, used by all Java platforms
- **Consistency**: Same filtering, statistics algorithms across platforms
- **Testability**: Core logic tested independently of platform code
- **Maintainability**: Bug fixes in one place benefit all platforms
- **Clean architecture**: Platform-specific concerns separated from business logic

### Negative

- **Build complexity**: Gradle multi-module build added to Maven-based desktop
- **Coordination overhead**: Changes to core require testing on all platforms
- **Version management**: Core and platform modules must stay in sync
- **Learning curve**: Contributors must understand the abstraction layers

## Current Status: Partially Implemented

**What works:**
- Android app depends on jnexus-core ✅
- Android implements interfaces correctly ✅
- Core module is complete and well-tested ✅

**What doesn't work:**
- Desktop does NOT depend on jnexus-core ❌
- Desktop has duplicate classes (Credentials, NexusService, etc.) ❌
- Desktop and core implementations have diverged (220 line difference in NexusService) ❌

**Technical debt created:**
- See Issue #17: Desktop module doesn't use jnexus-core (CRITICAL)
- See Issue #18: Desktop NexusService diverged from Core (HIGH)

## Why Desktop Wasn't Migrated

From CHANGELOG v1.2:
> Desktop modules continue to use existing `src/` with Maven

**Reasons:**
1. **Backward compatibility**: Avoid breaking existing desktop installations
2. **Risk mitigation**: Desktop was stable, Android was new - limit blast radius
3. **Timeline pressure**: Android launch prioritized over desktop refactoring
4. **Testing burden**: Desktop had 155 tests - migration risked regressions

**This created technical debt intentionally** - accepted tradeoff to ship Android faster.

## Path Forward

**Option 1: Complete the migration (Recommended)**
- Add jnexus-core dependency to desktop pom.xml
- Create CredentialsFile.java implementing Credentials interface
- Create NexusClientHttp.java implementing NexusHttpClient interface
- Delete duplicate classes from src/
- Update all UI code to use interfaces
- Extensive regression testing

**Option 2: Reverse the decision**
- Delete jnexus-core module
- Move Android to use desktop classes directly
- Accept platform-specific duplicates

**Option 3: Document and accept the duplication**
- Keep both implementations
- Manual synchronization of changes
- Ongoing maintenance burden

**Recommendation**: Option 1 should be pursued when resources allow. The interface pattern is correct, and desktop should adopt it.

## Alternatives Considered

### Alternative 1: Kotlin Multiplatform
- **Approach**: Use Kotlin Multiplatform to share code across JVM, Android, iOS
- **Rejected because**:
  - Desktop is Java 21, would require full Kotlin migration
  - Team expertise in Java, not Kotlin
  - iOS Swift app already in development (no KMP needed)
  - Complexity vs. benefit not justified

### Alternative 2: Single fat JAR with platform detection
- **Approach**: Include both java.net.http and OkHttp, detect platform at runtime
- **Rejected because**:
  - Desktop users don't need OkHttp dependency
  - Android users don't need java.net.http dependency
  - Larger binary sizes
  - Unclear separation of concerns

### Alternative 3: Copy-paste shared code
- **Approach**: No shared module, duplicate business logic in each platform
- **Rejected because**:
  - Violates DRY principle
  - Bug fixes must be applied multiple times
  - Implementations drift over time (already happening)
  - Maintenance nightmare

## References

- CHANGELOG.md: v1.2 Android release, multi-module architecture
- CLAUDE.md: Multi-Module Structure section
- Issue #17: CRITICAL architecture issue (desktop doesn't use core)
- Issue #18: NexusService divergence (220 line difference)

## Impact

**Positive:**
- Android app successfully launched with shared core
- Clear abstraction pattern established
- Foundation for future platforms (iOS complete, web possible)

**Negative:**
- Desktop technical debt created (2039 lines of duplicate code)
- Implementations diverging over time
- Confusion for contributors about which code is canonical

## Related Decisions

- ADR-0004: Java version strategy (why core is Java 11, desktop is Java 21)
- ADR-0006: Interface-based HTTP client abstraction
- Future ADR needed: Plan for desktop migration to jnexus-core

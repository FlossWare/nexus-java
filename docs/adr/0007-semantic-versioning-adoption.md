# 7. Semantic Versioning (X.Y.Z) Adoption

**Status:** Accepted

**Date:** 2026-05-28

**Deciders:** Scot P. Floess, Development Team

## Context

JNexus originally used an **X.Y auto-incrementing** versioning scheme where minor versions were bumped automatically on each release. As the project grew to multiple platforms (desktop, Android, iOS/macOS), this approach created several problems:

**Inconsistent version numbering across modules:**
- Desktop: 1.32 (auto-increment minor on every CI push)
- Core (pom.xml): 1.2
- Core (build.gradle): 1.1 (different from pom.xml!)
- Android: 1.2
- iOS/macOS: 1.0

**Ambiguity about change significance:**
- Version 1.31 vs 1.32: Is this a bug fix, new feature, or breaking change?
- No way for users to assess upgrade risk from version number alone
- Auto-incrementing meant every push bumped the version, even for documentation changes

**No deprecation or compatibility signaling:**
- No mechanism to indicate breaking changes (X.0.0)
- No way to distinguish additive features (X.Y.0) from bug fixes (X.Y.Z)
- Downstream consumers (Android app depending on jnexus-core) had no upgrade safety guarantees

**Maven enforcer validation was overly restrictive:**
- Enforced `X.Y` format only (no patch version allowed)
- Blocked adoption of industry-standard semantic versioning

## Decision

Adopt **Semantic Versioning (SemVer) X.Y.Z** across all modules with a unified version number.

### Version Format

| Component | Meaning | When to Bump | Example |
|-----------|---------|-------------|---------|
| **X** (Major) | Breaking changes | Incompatible API changes, major rewrites | 1.x.x -> 2.0.0 |
| **Y** (Minor) | New features | Backward-compatible new functionality | 2.0.x -> 2.1.0 |
| **Z** (Patch) | Bug fixes | Backward-compatible bug fixes, drop-in replacement | 2.1.0 -> 2.1.1 |

### Unified Version Across All Modules

All modules share the same version number:
- Desktop (pom.xml): 2.0.0
- Core (pom.xml + build.gradle): 2.0.0
- Android (build.gradle versionName): 2.0.0
- iOS (Info.plist CFBundleShortVersionString): 2.0.0
- macOS (Info.plist CFBundleShortVersionString): 2.0.0

### Initial Version: 2.0.0

The version was bumped from 1.x to 2.0.0 because:
- The encryption feature (added in desktop v1.32) was a significant addition
- Unifying all modules required a clean break
- Major version bump signals the multi-platform architecture is stable

### Updated Maven Enforcer

```xml
<!-- Old: X.Y only -->
<condition>
    String v = "${project.version}";
    v.matches("^\\d+\\.\\d+$")
</condition>

<!-- New: X.Y.Z semver -->
<condition>
    String v = "${project.version}";
    v.matches("^\\d+\\.\\d+\\.\\d+$")
</condition>
```

## Consequences

### Positive

- **Clear upgrade signaling**: Users can assess risk from version number alone
- **Industry standard**: SemVer is universally understood in software development
- **Unified versioning**: All modules share one version, eliminating confusion
- **Deprecation support**: Major version bumps provide natural deprecation boundaries
- **Compatibility guarantees**: Minor and patch versions are safe, backward-compatible upgrades
- **CI/CD clarity**: Auto-increment now has meaningful semantics (bump patch for fixes, minor for features)

### Negative

- **More version decisions**: Must decide major vs minor vs patch for each release
- **Breaking change discipline**: Major bumps require deprecation notices and migration guides
- **Coordination overhead**: All modules must be versioned together
- **Longer version strings**: "2.1.3" vs "1.32"

### Accepted Tradeoffs

The overhead is acceptable because:
1. SemVer is the de facto standard -- contributors already understand it
2. Multi-platform projects need clear compatibility signaling
3. The unified version eliminates the confusion of different module versions
4. Downstream consumers (Android depending on core) need upgrade safety guarantees

## Migration Details

### Version Mapping (Old to New)

| Module | Old Version | New Version |
|--------|-------------|-------------|
| Desktop (pom.xml) | 1.32 | 2.0.0 |
| Core (pom.xml) | 1.2 | 2.0.0 |
| Core (build.gradle) | 1.1 | 2.0.0 |
| Android (versionName) | 1.2 | 2.0.0 |
| Android (versionCode) | 2 | 3 |
| iOS (CFBundleShortVersionString) | 1.0 | 2.0.0 |
| iOS (CFBundleVersion) | 1 | 3 |
| macOS (CFBundleShortVersionString) | 1.0 | 2.0.0 |
| macOS (CFBundleVersion) | 1 | 3 |
| CLI @Command version | 1.0 | 2.0.0 |

### CI/CD Script Updates

The `ci/rev-version.sh` script was updated to:
1. Parse X.Y.Z format (three components instead of two)
2. Default to incrementing patch version (Z)
3. Support `--minor` and `--major` flags for larger bumps
4. Validate new version against SemVer regex

## Alternatives Considered

### Alternative 1: Keep X.Y auto-incrementing
- **Approach**: Continue with existing versioning, just synchronize across modules
- **Rejected because**:
  - No way to signal breaking vs non-breaking changes
  - Auto-increment gives every change equal weight
  - Not an industry standard
  - Confusing for external consumers

### Alternative 2: CalVer (Calendar Versioning)
- **Approach**: Use date-based versions like 2026.05.28
- **Rejected because**:
  - Does not convey compatibility information
  - Less common in Java ecosystem
  - Awkward for multiple releases in one day
  - No way to signal breaking changes

### Alternative 3: SemVer per module (independent versions)
- **Approach**: Each module has its own SemVer version
- **Rejected because**:
  - Creates compatibility matrix complexity
  - Users must track which core version works with which desktop version
  - More release coordination overhead
  - Small project does not justify this complexity

### Alternative 4: X.Y format with SemVer semantics
- **Approach**: Keep two-component versions but define X as breaking, Y as everything else
- **Rejected because**:
  - Cannot distinguish features from bug fixes
  - Non-standard interpretation of version numbers
  - Confuses developers who expect SemVer

## References

- Issue #46: Unified semantic versioning across all modules
- CHANGELOG.md: v2.0.0 entry documenting the migration
- VERSIONING.md: Updated versioning guide
- pom.xml: Maven enforcer plugin configuration
- API_COMPATIBILITY.md: API stability policy built on SemVer

## Impact

**Positive:**
- All modules now at 2.0.0, unified and clear
- Users can assess upgrade risk from version number
- Deprecation process formalized (3-version notice before removal)
- API_COMPATIBILITY.md provides detailed stability guarantees

**Negative:**
- One-time migration effort (updating all version references)
- Must maintain discipline about when to bump which component

## Related Decisions

- ADR-0002: Multi-module architecture (versioning spans multiple modules)
- ADR-0004: Java version strategy (core and desktop versions must stay in sync)
- API_COMPATIBILITY.md: Detailed compatibility guarantees built on SemVer

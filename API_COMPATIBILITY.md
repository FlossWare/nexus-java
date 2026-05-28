# JNexus API Compatibility Policy

## Overview

JNexus uses semantic versioning (X.Y.Z) with strict API compatibility guarantees for the `jnexus-core` module, which is consumed by Android and other platforms.

**Current Version:** 2.0.0

## Semantic Versioning Policy

### Major Version (X.0.0)
Breaking changes that require code updates:
- **Interface signature changes** - Method parameters, return types
- **Removed deprecated features** - Features deprecated for 2+ minor versions
- **Minimum version bumps** - Java, Android SDK, Kotlin requirements
- **Behavioral changes** - Changes to core functionality semantics

**Example:** 1.x → 2.0.0
- Added `forceRefresh` parameter to `listComponents()`
- Removed deprecated `clearCache(repository)` overload
- Minimum Android SDK: 26 → 28

**Migration Guide:** Provided in CHANGELOG.md with code examples

### Minor Version (X.Y.0)
Backward-compatible new features:
- **New methods** - Additional methods on existing interfaces
- **New interfaces** - New optional APIs
- **Performance improvements** - No API changes
- **New platforms** - iOS, macOS support
- **Default methods** - Added to interfaces (Java 8+)

**Example:** 2.0.0 → 2.1.0
- Added `listComponentsWithMetadata()` method
- Added `ComponentMetadata` record
- Added `SearchCriteria` builder

**Compatibility:** Code written for 2.0.0 works with 2.1.0 without changes

### Patch Version (X.Y.Z)
Bug fixes and non-breaking improvements:
- **Bug fixes** - Corrected behavior
- **Documentation updates** - Javadoc, README
- **Security patches** - Vulnerability fixes
- **Performance fixes** - No API changes
- **Internal refactoring** - No public API impact

**Example:** 2.0.0 → 2.0.1
- Fixed cache expiration calculation
- Updated Javadoc for `deleteComponent()`
- Security: Upgraded Jackson version

**Compatibility:** Drop-in replacement

## API Stability Levels

### Stable API (@apiNote Stable)
Stable APIs are guaranteed backward compatible within major version:

```java
/**
 * HTTP client interface for Nexus Repository Manager.
 * 
 * @since 2.0.0
 * @apiNote Stable - safe for production use
 */
public interface NexusHttpClient {
    List<RepoRecord> listComponents(String repository, boolean forceRefresh) 
        throws IOException, InterruptedException;
}
```

**Guarantees:**
- Method signatures won't change in minor/patch versions
- Existing methods won't be removed until next major version
- Behavior changes documented in CHANGELOG.md
- 2+ minor version deprecation notice before removal

**Current Stable APIs:**
- `org.flossware.jnexus.NexusHttpClient`
- `org.flossware.jnexus.Credentials`
- `org.flossware.jnexus.RepoRecord`
- `org.flossware.jnexus.ComponentMetadata`
- `org.flossware.jnexus.SearchCriteria`
- `org.flossware.jnexus.RepositoryStats`

### Experimental API (@apiNote Experimental)
Experimental APIs may change in minor versions:

```java
/**
 * Advanced search with ML-powered relevance ranking.
 * 
 * @since 2.1.0
 * @apiNote Experimental - API may change without notice
 */
@Experimental
public interface MLSearchEngine {
    // May change in 2.2.0, 2.3.0, etc.
}
```

**Warnings:**
- Use at your own risk
- May change signature in minor versions
- May be removed in minor versions
- Not recommended for production code

**Current Experimental APIs:**
- None (as of 2.0.0)

### Internal API (No @apiNote)
Internal implementation classes are NOT part of the public API:

```java
// org.flossware.jnexus.internal.* - NOT public API
class InternalCacheImpl { 
    // Can change without notice
}
```

**No Guarantees:**
- May change in any version
- May be removed without deprecation
- Do not depend on these classes

## Deprecation Process

JNexus uses a **3-version deprecation policy** for stable APIs:

### Phase 1: Add Replacement (Version N)
Introduce new method alongside old one:

```java
// Version 2.1.0
// New recommended method
List<ComponentMetadata> listComponentsWithMetadata(String repository, boolean forceRefresh);

// Old method - still works
List<RepoRecord> listComponents(String repository, boolean forceRefresh);
```

### Phase 2: Mark Deprecated (Version N)
Deprecate old method with migration guidance:

```java
// Version 2.1.0
/**
 * @deprecated Use {@link #listComponentsWithMetadata(String, boolean)} instead.
 *             This method will be removed in version 3.0.0.
 */
@Deprecated(since = "2.1.0", forRemoval = true)
List<RepoRecord> listComponents(String repository, boolean forceRefresh);
```

**Behavior:** Still fully functional, but:
- Javadoc warns of deprecation
- IDEs show strike-through
- Compiler warnings (`-Xlint:deprecation`)

### Phase 3: Log Warning (Version N+1)
Add runtime deprecation warnings:

```java
// Version 2.2.0
@Deprecated(since = "2.1.0", forRemoval = true)
public List<RepoRecord> listComponents(String repository, boolean forceRefresh) {
    logger.warn("listComponents() is deprecated and will be removed in 3.0.0. "
              + "Use listComponentsWithMetadata() instead.");
    // Delegate to new implementation
    return listComponentsWithMetadata(repository, forceRefresh).stream()
        .map(m -> new RepoRecord(m.id(), m.fileSize(), m.path()))
        .toList();
}
```

### Phase 4: Remove (Next Major Version)
Remove deprecated method:

```java
// Version 3.0.0
// listComponents() DELETED - use listComponentsWithMetadata()
```

**Timeline:** Minimum 2 minor versions before removal
- Deprecated in 2.1.0
- Warning in 2.2.0
- Removed in 3.0.0

## Upgrade Compatibility Matrix

| Your Code | JNexus Version | Compatible? | Action Required |
|-----------|----------------|-------------|-----------------|
| 1.x | 1.x | ✅ Yes | None |
| 1.x | 2.x | ⚠️ Maybe | Check CHANGELOG for breaking changes |
| 2.0.0 | 2.0.x | ✅ Yes | None - drop-in replacement |
| 2.0.0 | 2.1.0 | ✅ Yes | None - backward compatible |
| 2.1.0 | 2.0.0 | ❌ No | Downgrade not supported |
| 2.x | 3.x | ⚠️ Maybe | Migration guide required |

## API Design Principles

### 1. Additive Changes Only (Minor Versions)
**Good:** Adding optional parameters with defaults

```java
// Version 2.0.0
void deleteComponent(String componentId);

// Version 2.1.0 - Add overload
void deleteComponent(String componentId);
void deleteComponent(String componentId, DeleteOptions options); // NEW
```

**Bad:** Changing existing signatures

```java
// Version 2.0.0
void deleteComponent(String componentId);

// Version 2.1.0 - WRONG (breaking change)
void deleteComponent(String componentId, boolean dryRun); // BREAKS 2.0.0 callers
```

### 2. Default Methods for Interface Evolution
**Good:** Adding default methods to interfaces

```java
// Version 2.0.0
interface NexusHttpClient {
    List<RepoRecord> listComponents(String repository, boolean forceRefresh);
}

// Version 2.1.0 - Add with default
interface NexusHttpClient {
    List<RepoRecord> listComponents(String repository, boolean forceRefresh);
    
    default boolean supportsMetadata() {
        return false; // Safe default
    }
}
```

### 3. Builder Pattern for Complex Parameters
**Good:** Using builders for extensibility

```java
SearchCriteria criteria = new SearchCriteria.Builder()
    .repository("maven-releases")
    .minSize(1_000_000L)
    .build();

// Version 2.2.0 - Add new filter (backward compatible)
SearchCriteria criteria = new SearchCriteria.Builder()
    .repository("maven-releases")
    .minSize(1_000_000L)
    .author("john.doe") // NEW - old code still works
    .build();
```

### 4. Sealed Types for Stability
**Good:** Using records for immutable DTOs

```java
// Version 2.0.0
public record RepoRecord(String id, long fileSize, String path) {
    // Cannot be subclassed - stable contract
}
```

## Testing Strategy

### Backward Compatibility Tests
Test that code written for older versions still works:

```java
@Test
void testBackwardCompatibility_2_0_0() {
    // Simulate code written for 2.0.0
    NexusHttpClient client = new NexusClient(credentials);
    List<RepoRecord> records = client.listComponents("repo", false);
    
    assertNotNull(records);
    assertFalse(records.isEmpty());
}
```

### Deprecation Verification Tests
Ensure deprecated methods still work:

```java
@Test
void testDeprecatedMethodStillWorks() {
    @SuppressWarnings("deprecation")
    var result = client.oldMethod();
    
    assertEquals(expected, result);
}
```

## Breaking Change Checklist

Before making a breaking change, verify:

- [ ] Is this change absolutely necessary?
- [ ] Can it be done additively instead?
- [ ] Has the old API been deprecated for 2+ minor versions?
- [ ] Is a migration guide provided in CHANGELOG.md?
- [ ] Are code examples provided for the upgrade?
- [ ] Is the major version number incremented?
- [ ] Are all deprecation warnings removed?

## Version Support Policy

| Version | Status | Support Until | Notes |
|---------|--------|---------------|-------|
| 2.x | ✅ Active | Ongoing | Current stable release |
| 1.x | ⚠️ Maintenance | 2027-01-01 | Security fixes only |
| 0.x | ❌ Unsupported | N/A | Alpha/Beta - no support |

**Support Levels:**
- **Active:** New features, bug fixes, security patches
- **Maintenance:** Critical bug fixes and security patches only
- **Unsupported:** No updates

## Migration Guides

### Migrating from 1.x to 2.0.0

**Breaking Changes:**

1. **Added forceRefresh parameter**
   ```java
   // 1.x
   client.listComponents("repo");
   
   // 2.0.0
   client.listComponents("repo", false); // false = use cache
   ```

2. **Credentials interface changes**
   ```java
   // 1.x
   Credentials creds = new Credentials();
   
   // 2.0.0
   Credentials creds = new Credentials(); // Works the same
   // OR with profile
   Credentials creds = new Credentials("dev");
   ```

**Recommended Actions:**
1. Search for `listComponents(` in your code
2. Add `false` as second parameter (to maintain caching behavior)
3. Test thoroughly before deploying

## FAQ

**Q: Can I use 2.1.0 as a drop-in replacement for 2.0.0?**  
A: Yes - minor versions are backward compatible.

**Q: Will my code break if I upgrade from 2.0.0 to 2.5.0?**  
A: No - all 2.x versions are compatible. New features are additive only.

**Q: When will deprecated methods be removed?**  
A: After 2+ minor versions, in the next major version. Minimum 6-12 months notice.

**Q: Can I depend on internal classes?**  
A: No - only classes in `org.flossware.jnexus` (top-level) are public API.

**Q: How do I know which version to use?**  
A: Use the latest 2.x version for new projects. Check CHANGELOG.md for migration guides.

## Contact

Questions about API compatibility?
- Open a [GitHub Discussion](https://github.com/FlossWare/jnexus/discussions)
- File an [Issue](https://github.com/FlossWare/jnexus/issues) for compatibility bugs

## References

- [Semantic Versioning 2.0.0](https://semver.org/)
- [Java @Deprecated Annotation](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Deprecated.html)
- [OpenSSF Best Practices](https://www.bestpractices.dev/)

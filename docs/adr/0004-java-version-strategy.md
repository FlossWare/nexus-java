# 4. Java Version Strategy: Java 21 Desktop, Java 11 Core

**Status:** Accepted

**Date:** 2026-05-22

**Deciders:** Scot P. Floess, Development Team

## Context

JNexus spans multiple platforms with different Java version constraints:

**Desktop requirements:**
- Latest Java features for developer productivity
- Records, pattern matching, text blocks, switch expressions
- Modern HTTP/2 client (java.net.http)
- Virtual threads (future consideration)
- No backward compatibility constraints

**Android requirements:**
- Android 8.0+ support (released 2017, still ~15% market share)
- Android desugaring supports Java 11 features (records, etc.)
- Cannot use Java 17+ features (not supported by Android tooling)
- Must balance modernity with device compatibility

**Core module requirements:**
- Must work on both desktop (Java 21) and Android (Java 11)
- Contains shared data models (records) and business logic
- Platform-agnostic abstractions

## Decision

Use **different Java versions** for different modules:

### Desktop Module (src/) - Java 21
```xml
<maven.compiler.source>21</maven.compiler.source>
<maven.compiler.target>21</maven.compiler.target>
```

**Rationale:**
- Desktop users can easily install Java 21 (SDKMAN, package managers, etc.)
- Leverage modern language features for cleaner code
- Performance improvements in Java 21 GC and JIT
- Project demonstrates modern Java best practices

**Features used:**
- Records for data classes (RepoRecord, ComponentMetadata, etc.)
- Text blocks for multi-line strings
- Switch expressions for cleaner control flow
- Pattern matching for instanceof
- var for local variable type inference

### Core Module (jnexus-core/) - Java 11
```groovy
sourceCompatibility = '11'
targetCompatibility = '11'
```

**Rationale:**
- Android desugaring supports Java 11 features on Android 8.0+
- Balances modernity (records) with broad compatibility
- Java 11 is LTS (Long Term Support) version
- Core module must be usable by both desktop and Android

**Features used:**
- Records (via desugaring on Android)
- var for local variable type inference
- Enhanced switch (via desugaring)
- String methods (isBlank, lines, repeat)

**Features avoided:**
- Java 17+ features (sealed classes, pattern matching for switch)
- Any feature not supported by Android desugaring

## Consequences

### Positive

- **Desktop optimization**: Latest language features improve code quality
- **Android compatibility**: Can target Android 8.0+ (broad device support)
- **Shared models**: Records work on both platforms (via desugaring)
- **Future-proof desktop**: Can adopt Java 22+ features as needed
- **LTS core**: Java 11 is stable, well-supported

### Negative

- **Version management**: Must track two Java versions
- **Build complexity**: Different compilers for different modules
- **Developer setup**: Developers need both Java 11 and Java 21 installed
- **Feature fragmentation**: Can't use Java 17-21 features in core module
- **Testing overhead**: Must test core on both Java 11 and Java 21

### Accepted Tradeoffs

The complexity is justified because:
1. Modern Java features significantly improve code quality
2. Android support is critical (mobile users)
3. Desktop users expect latest technology
4. Core module is small and stable (low churn)

## Android Desugaring

Android Gradle Plugin includes D8 desugaring to backport Java 11+ features to older Android versions:

```groovy
android {
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_11
        targetCompatibility JavaVersion.VERSION_11
        coreLibraryDesugaringEnabled true
    }
}

dependencies {
    coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:2.0.4'
}
```

**What gets desugared:**
- Records → classes with equals/hashCode/toString
- Time API (java.time.*) → backport for older Android
- Optional, Stream → backport implementations
- Enhanced switch → traditional switch

## Version Selection Criteria

**Why Java 21 for desktop?**
- Released September 2023, stable and mature
- LTS release (will be supported until 2031)
- Virtual threads (Project Loom) for future async operations
- Performance improvements (G1GC, JIT)
- Latest language features

**Why not Java 17 for desktop?**
- Java 21 is also LTS, no reason to stay behind
- Missing features: virtual threads, record patterns, sequenced collections
- Desktop users can easily upgrade

**Why Java 11 for core?**
- Highest version supported by Android desugaring
- Allows using records (critical for clean data models)
- LTS version (supported until 2027)
- Widely supported across platforms

**Why not Java 8 for core?**
- No records (would need many boilerplate classes)
- Missing modern String, Stream improvements
- Android desugaring handles Java 11 → Android 8.0 backporting

## Build Requirements

**Developers must have:**
- JDK 21 for building desktop module
- JDK 11 for building core module (or JDK 21 with `--release 11`)

**CI/CD:**
- GitHub Actions: Use `actions/setup-java@v3` with multiple Java versions
- Desktop build: Java 21
- Android build: Java 17 (for Android Gradle Plugin 8.x)
- Core tests: Java 11

## Testing Strategy

**Core module:**
- Test with Java 11 (minimum supported version)
- Test with Java 21 (ensure forward compatibility)
- Android instrumented tests verify desugaring works

**Desktop module:**
- Test with Java 21 only

## Alternatives Considered

### Alternative 1: Java 11 for everything
- **Approach**: Use Java 11 for desktop and core
- **Rejected because**:
  - Desktop limited to older language features
  - No virtual threads (future async operations)
  - Slower GC and JIT performance
  - Missed opportunity to showcase modern Java

### Alternative 2: Java 21 for everything
- **Approach**: Use Java 21 for desktop and core
- **Rejected because**:
  - Android desugaring doesn't support Java 17+ features
  - Would require dropping Android 8.0-13 support
  - Limit user base to newest Android versions only

### Alternative 3: Java 17 compromise
- **Approach**: Use Java 17 for both (newest LTS before 21)
- **Rejected because**:
  - Still no Android support for Java 17+ features
  - Desktop limited to older version for no benefit
  - Java 21 is also LTS, no reason to avoid it

### Alternative 4: Kotlin Multiplatform
- **Approach**: Use Kotlin targeting JVM and Android
- **Rejected because**:
  - Team expertise in Java, not Kotlin
  - Desktop already written in Java
  - Kotlin adds complexity without clear benefit
  - See ADR-0002 for full rationale

## References

- pom.xml: Desktop Java 21 configuration
- jnexus-core/build.gradle: Core Java 11 configuration
- jnexus-android/build.gradle: Desugaring configuration
- CLAUDE.md: "Why Java 21 for desktop but Java 17 for core?" section

## Impact

**Positive:**
- Clean, modern codebase (desktop)
- Broad Android compatibility (core)
- Performance benefits (desktop)
- Demonstrates Java best practices

**Negative:**
- Developer machine requires multiple JDK versions
- CI/CD must install multiple Java versions
- Can't use latest features in shared code

## Future Considerations

**When Android desugaring supports Java 17+:**
- Upgrade core to Java 17
- Adopt sealed classes for better type safety
- Use pattern matching for switch

**When Java 21 reaches EOL (2031):**
- Upgrade desktop to next LTS (Java 25, 29, etc.)
- Evaluate Android compatibility at that time

## Related Decisions

- ADR-0002: Multi-module architecture (why core exists separately)
- Future ADR: Async operations with virtual threads (Java 21 feature)

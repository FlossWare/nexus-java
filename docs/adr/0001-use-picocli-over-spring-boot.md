# 1. Use Picocli Over Spring Boot for CLI Framework

**Status:** Accepted

**Date:** 2026-05-16

**Deciders:** Scot P. Floess, Development Team

## Context

The original implementation of JNexus (version 0.x) used Spring Boot as the application framework. While Spring Boot provides excellent features for building enterprise applications, it introduced significant overhead for a simple command-line tool:

- **JAR size**: ~50MB (Spring Boot dependencies)
- **Startup time**: 3-5 seconds (Spring context initialization)
- **Memory footprint**: Higher due to Spring container
- **Complexity**: Required Spring-specific configuration and annotations

For a CLI tool that performs simple HTTP operations against Nexus API, this overhead was excessive and impacted user experience.

## Decision

Refactor from Spring Boot to Picocli (lightweight CLI framework) with plain Java 21.

**Implementation approach:**
- Replace Spring Boot CLI with Picocli for command-line parsing
- Replace RestTemplate with java.net.http.HttpClient (built-in Java 21)
- Remove Spring dependency injection and use direct instantiation
- Use Jackson for JSON processing (no Spring MVC)
- Use SLF4J with Logback for logging (no Spring Boot starters)

## Consequences

### Positive

- **Massive size reduction**: 2.7MB JAR (was ~50MB) - 95% smaller
- **Instant startup**: <200ms (was 3-5 seconds) - 15-25x faster
- **Simpler architecture**: No framework magic, straightforward Java code
- **Easier debugging**: Clear control flow, no proxy classes or aspect weaving
- **Lower memory usage**: No Spring container overhead
- **Minimal dependencies**: Only essential libraries (Picocli, Jackson, SLF4J, jcurses)

### Negative

- **No dependency injection**: Manual object instantiation and wiring
- **No auto-configuration**: Manual HTTP client and service setup
- **No Spring ecosystem**: Can't use Spring Data, Spring Security, etc.
- **More boilerplate**: Need to write configuration that Spring Boot auto-configures

### Accepted Tradeoffs

The negative consequences are acceptable because:
1. JNexus is a simple tool - doesn't need enterprise features
2. Manual wiring is straightforward for a small codebase
3. User experience (size, speed) is critical for a CLI tool
4. Simplicity aids maintainability

## Alternatives Considered

### Alternative 1: Keep Spring Boot, optimize with native image
- **Approach**: Use GraalVM native-image to compile Spring Boot app
- **Rejected because**:
  - Still requires extensive Spring configuration
  - Native image compilation is complex and fragile
  - Reflection configuration for Spring is difficult
  - Limited library compatibility with native image

### Alternative 2: Use Micronaut or Quarkus
- **Approach**: Switch to lighter framework designed for fast startup
- **Rejected because**:
  - Still framework overhead for a simple tool
  - Learning curve for team
  - Dependency on framework ecosystem
  - Overkill for HTTP client operations

### Alternative 3: Plain Java with no CLI framework
- **Approach**: Manual argument parsing with String[] args
- **Rejected because**:
  - Reinventing the wheel for CLI parsing
  - Poor user experience (no help text, validation)
  - Picocli provides this with minimal overhead

## References

- Initial refactoring: Commit fe018f1 (v1.0 release)
- CHANGELOG.md: v1.0 entry documenting Spring Boot removal
- README.md: Performance comparison (50MB → 2.7MB, 3-5s → <200ms)

## Impact

This decision set the foundation for JNexus v1.0+ and enabled:
- Fast, responsive CLI experience
- Easy distribution (small JAR)
- Simple architecture for contributors
- Foundation for multiple UI implementations (Swing, AWT, Terminal)

## Related Decisions

- ADR-0003: Four UI approaches (enabled by lightweight architecture)
- ADR-0004: Java version strategy (Java 21 for desktop performance)

# Architecture Decision Records (ADRs)

This directory contains Architecture Decision Records (ADRs) for the JNexus project.

## What is an ADR?

An Architecture Decision Record (ADR) documents an important architectural decision made during the project's development. It captures:
- **What** decision was made
- **Why** it was made (context, alternatives considered)
- **When** it was made
- **Who** made it
- **Consequences** (positive and negative)

ADRs help new contributors understand the reasoning behind architectural choices and prevent re-litigating past decisions.

## ADR Index

### Accepted (Current Architecture)

- [ADR-0001](0001-use-picocli-over-spring-boot.md) - **Use Picocli Over Spring Boot** (2026-05-16)  
  Why we removed Spring Boot and use lightweight CLI framework

- [ADR-0002](0002-multi-module-architecture.md) - **Multi-Module Architecture** (2026-05-22, Partially Implemented)  
  Why jnexus-core exists for shared code between desktop and Android

- [ADR-0003](0003-four-ui-approaches.md) - **Four UI Approaches** (2024-Present)  
  Why we maintain CLI, Swing GUI, AWT GUI, and Terminal UI

- [ADR-0004](0004-java-version-strategy.md) - **Java Version Strategy** (2026-05-22)  
  Why desktop uses Java 21 and core uses Java 11

- [ADR-0005](0005-extract-encryption-to-jencrypt.md) - **Extract Encryption to JEncrypt** (2026-05-24)  
  Why encryption was extracted to separate jencrypt library

- [ADR-0006](0006-interface-based-http-client.md) - **Interface-Based HTTP Client** (2026-05-22, Partially Implemented)  
  Why NexusHttpClient interface abstracts HTTP layer

### Partially Implemented

**ADR-0002** and **ADR-0006** are marked as "Partially Implemented" because:
- Android uses jnexus-core and implements interfaces correctly ✅
- Desktop does NOT use jnexus-core (Issue #17) ❌
- See [Issue #17](https://github.com/FlossWare/jnexus/issues/17) for details

## ADR Status Values

- **Accepted** - Decision is implemented and current
- **Partially Implemented** - Decision is partially applied (note which parts are incomplete)
- **Superseded** - Decision has been replaced by a newer ADR (link to replacement)
- **Deprecated** - Decision is no longer relevant but kept for historical context
- **Rejected** - Proposal that was considered but not accepted

## ADR Template

When creating a new ADR, use this template:

```markdown
# [Number]. [Title]

**Status:** Accepted | Rejected | Superseded | Deprecated | Partially Implemented

**Date:** YYYY-MM-DD

**Deciders:** [Who made this decision]

## Context

[What is the issue or challenge we're addressing? What forces are at play? What constraints exist?]

## Decision

[What did we decide to do? Describe the solution in detail.]

## Consequences

### Positive

- [Benefit 1]
- [Benefit 2]

### Negative

- [Tradeoff 1]
- [Tradeoff 2]

### Accepted Tradeoffs

[Explain why the negative consequences are acceptable]

## Alternatives Considered

### Alternative 1: [Name]
- **Approach**: [Brief description]
- **Rejected because**: [Reasons]

### Alternative 2: [Name]
- **Approach**: [Brief description]
- **Rejected because**: [Reasons]

## References

- [Link to code]
- [Link to issue]
- [Link to pull request]
- [Link to documentation]

## Impact

[How did this decision affect the project? What changed as a result?]

## Related Decisions

- [ADR-XXXX: Related decision]
```

## Naming Convention

ADRs are numbered sequentially with leading zeros:
- `0001-descriptive-name.md`
- `0002-another-decision.md`
- `0010-tenth-decision.md`

The number provides chronological ordering, and the descriptive name makes it easy to find relevant ADRs.

## When to Write an ADR

Write an ADR for decisions that:
- Have long-term impact on the architecture
- Are hard to reverse (high cost to change)
- Affect multiple parts of the system
- Involve significant tradeoffs
- Are non-obvious or controversial
- Future contributors might question

**Examples:**
- Choosing a framework (Spring Boot → Picocli)
- Selecting a language version (Java 21 vs. Java 17)
- Architectural patterns (interface-based abstraction)
- Technology extraction (jencrypt library)
- Module structure (multi-module vs. monolith)

**Don't write ADRs for:**
- Routine bug fixes
- Code style choices (covered by CONTRIBUTING.md)
- Dependency version updates
- Documentation improvements

## How to Reference ADRs

In code comments, issues, or documentation, reference ADRs as:
```
See ADR-0001 for why we use Picocli instead of Spring Boot.
```

Or use full links:
```
See [ADR-0002](docs/adr/0002-multi-module-architecture.md) for architecture details.
```

## Benefits of ADRs

1. **Onboarding** - New contributors understand architectural context
2. **Prevent re-litigation** - Don't debate already-decided questions
3. **Learn from history** - Understand why past decisions were made
4. **Document tradeoffs** - Capture both benefits and costs
5. **Show evolution** - See how architecture changed over time

## Additional Resources

- [Joel Parker Henderson's ADR Templates](https://github.com/joelparkerhenderson/architecture-decision-record)
- [ThoughtWorks Tech Radar: Lightweight ADRs](https://www.thoughtworks.com/radar/techniques/lightweight-architecture-decision-records)
- [Michael Nygard's Original ADR Article](http://thinkrelevance.com/blog/2011/11/15/documenting-architecture-decisions)

## Contributing

When making significant architectural decisions:
1. Draft an ADR using the template above
2. Discuss with the team (GitHub issue or PR)
3. Update status as the decision evolves
4. Update this README.md index
5. Reference the ADR in related code/docs

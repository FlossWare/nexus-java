# Code Quality Tools

JNexus uses automated code quality analysis to maintain high standards and identify refactoring opportunities.

## Tools Configured

### 1. Checkstyle (Code Style + Basic Complexity)

**Purpose**: Enforce coding standards and detect basic complexity issues.

**Checks**:
- **Cyclomatic Complexity**: Max 15 per method
- **NPath Complexity**: Max 200 per method  
- **JavaNCSS**: Max 50 lines per method, 500 per class
- **Method Length**: Max 100 lines
- **Parameter Number**: Max 7 parameters
- **Nesting Depth**: Max 3 levels for if statements, 2 for try blocks
- **Best Practices**: Simplified boolean expressions, no magic numbers

**Configuration**: `checkstyle.xml`, `checkstyle-suppressions.xml`

**Run**: `mvn checkstyle:checkstyle`

**Report**: `target/checkstyle-result.xml`

### 2. PMD (Advanced Code Analysis)

**Purpose**: Detect code smells, design issues, and copy-paste duplication.

**Checks**:
- **God Class**: Classes with too many responsibilities
- **Cognitive Complexity**: Max 15 per method
- **TooManyMethods**: Max 20 per class
- **Copy-Paste Detection (CPD)**: Duplicate code blocks
- **Performance**: Inefficient string concatenation, unnecessary object creation
- **Security**: Hardcoded credentials, SQL injection risks

**Configuration**: `pmd-ruleset.xml`

**Run**: `mvn pmd:pmd pmd:cpd`

**Reports**: `target/pmd.xml`, `target/cpd.xml`

### 3. SpotBugs (Bug Detection)

**Purpose**: Find potential bugs through static analysis.

**Checks**:
- **Null Pointer**: Potential NullPointerExceptions
- **Resource Leaks**: Unclosed streams, connections
- **Security**: Hardcoded passwords, weak crypto
- **Concurrency**: Thread safety issues
- **Performance**: Inefficient patterns

**Configuration**: `spotbugs-exclude.xml`

**Run**: `mvn spotbugs:spotbugs`

**Report**: `target/spotbugsXml.xml`

## Running Quality Checks

### Individual Tools

```bash
# Checkstyle only
mvn checkstyle:checkstyle

# PMD only
mvn pmd:pmd pmd:cpd

# SpotBugs only
mvn spotbugs:spotbugs
```

### All Tools + Summary

```bash
# Generate comprehensive quality report
./quality-report.sh
```

### Integrated with Build

Quality checks run automatically during:
- `mvn validate` - Checkstyle + PMD
- `mvn verify` - All tools including SpotBugs

## Suppressions

### UI Classes
UI classes (JNexusSwing, JNexusAWT, JNexusUI) have relaxed limits:
- Method length: No limit (complex UI initialization)
- Cyclomatic complexity: Suppressed (event handlers)

### Test Classes
Test classes are exempt from:
- Parameter number limits (parameterized tests)
- Magic number checks (test data)

### Planned Refactoring
`Credentials` class constructor is temporarily suppressed:
- Current complexity: 46 (target: <15)
- Current NPath: 89M (target: <200)
- Planned: Extract methods for validation, loading, encryption

## Current Quality Metrics

### Checkstyle Violations: 8
- Credentials.java: 3 (constructor complexity)
- NexusService.java: 2 (method complexity)
- JNexusUI.java: 2 (method complexity)
- Others: 1

### PMD Violations: ~50
- High Priority (P1-P2): 5
  - Credentials: God Class, high complexity
  - Field naming conventions
- Medium Priority (P3): 45
  - Duplicate literals
  - Local variables could be final
  - Cognitive complexity

### CPD Duplications: 1
- NexusClient.java: 20 duplicate lines (HTTP request building)
  - Candidate for extraction to helper method

### SpotBugs: 0
- No bugs detected

## Refactoring Priorities

### High Priority

**1. Credentials Constructor** (Lines 286-400, 174 lines)
- **Complexity**: Cyclomatic 46, NPath 89M, NCSS 92
- **Issue**: God method doing too much (env loading, file loading, validation, encryption)
- **Fix**: Extract methods:
  - `loadFromEnvironment()`
  - `loadFromPropertiesFile()`
  - `validateCredentials()`
  - `decryptPassword()`
- **Impact**: Complexity 46 → <15, NPath 89M → <200

**2. NexusClient Duplication** (20 lines)
- **Issue**: HTTP request building duplicated in `fetchComponents()` and `fetchMetadataComponents()`
- **Fix**: Extract `buildAuthenticatedGetRequest(String url)` method
- **Impact**: Remove 1 CPD violation, improve maintainability

### Medium Priority

**3. NexusService.searchComponents()** (Line 205)
- **Complexity**: Cyclomatic 18, NPath 760
- **Issue**: Multiple filters applied sequentially
- **Fix**: Extract filter methods or use strategy pattern
- **Impact**: Complexity 18 → <15, improved testability

**4. Credentials God Class**
- **WMC**: 124, **ATFD**: 42, **TCC**: 13%
- **Issue**: Too many responsibilities (loading, validation, encryption, storage)
- **Fix**: Split into:
  - `CredentialsLoader` - Load from env/file
  - `CredentialsValidator` - Validation logic
  - `CredentialsEncryption` - Encrypt/decrypt
  - `Credentials` - Data holder only
- **Impact**: Better separation of concerns, easier testing

## Integration with CI/CD

Quality checks are integrated into the GitHub Actions workflow:

```yaml
- name: Run Quality Checks
  run: mvn verify

- name: Upload Quality Reports
  uses: actions/upload-artifact@v3
  with:
    name: quality-reports
    path: |
      target/checkstyle-result.xml
      target/pmd.xml
      target/cpd.xml
      target/spotbugsXml.xml
```

## Quality Trends

Track quality over time:
- Checkstyle violations: Target <5
- PMD high priority: Target 0
- CPD duplications: Target 0
- SpotBugs bugs: Maintain 0

## Resources

- [Checkstyle Documentation](https://checkstyle.sourceforge.io/)
- [PMD Rules](https://pmd.github.io/latest/pmd_rules_java.html)
- [SpotBugs Bug Patterns](https://spotbugs.readthedocs.io/en/stable/bugDescriptions.html)
- [Cognitive Complexity Paper](https://www.sonarsource.com/docs/CognitiveComplexity.pdf)

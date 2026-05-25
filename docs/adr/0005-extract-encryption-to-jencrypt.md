# 5. Extract Encryption to JEncrypt Library

**Status:** Accepted

**Date:** 2026-05-24

**Deciders:** Scot P. Floess, Development Team

## Context

JNexus v1.30 introduced AES-256-GCM password encryption for secure credential storage. The implementation was initially embedded directly in the JNexus codebase:

- **CredentialEncryption.java** (295 lines): AES-256-GCM encryption/decryption
- **CredentialEncryptionTest.java** (251 lines): Comprehensive tests (35 tests)
- **Integrated with Credentials.java**: Password encryption on save, decryption on load

While this worked well for JNexus, the encryption logic was:
- **General-purpose**: Not specific to credential management
- **Reusable**: Other projects need the same encryption functionality
- **Well-tested**: 96% code coverage, comprehensive edge case handling
- **Self-contained**: No JNexus-specific dependencies

The question arose: Should this encryption functionality remain embedded in JNexus, or be extracted as a standalone library?

## Decision

Extract encryption to **jencrypt** - a standalone, general-purpose encryption library.

### Library Design

**Repository:** https://github.com/FlossWare/jencrypt

**Core class:** `org.flossware.crypto.AESEncryption`
```java
public String encrypt(String plaintext)
public String decrypt(String encryptedValue)
public static boolean isEncrypted(String value)
```

**Helper class:** `org.flossware.crypto.CredentialHelper`
```java
void saveCredential(Properties props, String key, String value)
String loadCredential(Properties props, String key)
boolean isCredentialEncrypted(Properties props, String key)
boolean migrateToEncrypted(Properties props, String key)
```

**Quality requirements:**
- ✅ No wildcard imports (all imports explicit)
- ✅ 96% minimum code coverage (JaCoCo enforced)
- ✅ X.Y versioning only (no -SNAPSHOT)
- ✅ Auto version bumping and publishing (packagecloud.io)

### JNexus Integration

**pom.xml dependency:**
```xml
<dependency>
    <groupId>org.flossware</groupId>
    <artifactId>jencrypt</artifactId>
    <version>1.0</version>
</dependency>
```

**Code changes:**
- Replace `CredentialEncryption` with `org.flossware.crypto.AESEncryption`
- Delete local encryption classes (546 lines removed)
- Update imports in Credentials.java and CredentialsTest.java

## Consequences

### Positive

- **Code reuse**: Other projects can use jencrypt without depending on JNexus
- **Separation of concerns**: Encryption logic not coupled to credential management
- **Better testing**: Encryption tested independently of JNexus
- **Smaller JNexus**: Removed 546 lines of code (295 src + 251 test)
- **Focused library**: jencrypt has single, clear purpose
- **Quality enforcement**: Maven enforcer plugins ensure standards
- **Public distribution**: Available on packagecloud.io for any project

### Negative

- **Additional dependency**: JNexus now depends on external library
- **Version coordination**: Must manage jencrypt version in pom.xml
- **Build complexity**: Two repositories to maintain instead of one
- **Release coordination**: jencrypt must be published before JNexus can use it
- **Initial setup overhead**: Create repo, configure CI/CD, publish to packagecloud

### Accepted Tradeoffs

The overhead is acceptable because:
1. Encryption is stable (unlikely to change frequently)
2. Other FlossWare projects need similar functionality
3. Public library increases FlossWare ecosystem visibility
4. Separation improves architecture and testability

## Library Requirements

### No Wildcard Imports
All imports must be explicit:
```java
// ✅ Good
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

// ❌ Bad
import javax.crypto.*;
```

**Enforcement:** Maven checkstyle or manual code review

### 96% Code Coverage Minimum
**Enforcement:** JaCoCo Maven plugin with build failure:
```xml
<limit>
    <counter>INSTRUCTION</counter>
    <value>COVEREDRATIO</value>
    <minimum>0.96</minimum>
</limit>
```

**Why 96% not 100%?**
- Constructor defensive code (InetAddress.getLocalHost() exception) cannot realistically fail
- 96% allows defensive programming without test complexity

### X.Y Versioning Only
**Enforcement:** Maven enforcer plugin with Beanshell validation:
```xml
<rule implementation="org.apache.maven.plugins.enforcer.EvaluateBeanshell">
    <condition>
        String v = "${project.version}";
        v.matches("^\\d+\\.\\d+$")
    </condition>
</rule>
```

**Allowed:** 1.0, 1.1, 2.0
**Disallowed:** 1.0.0, 1.0-SNAPSHOT, 1.0-beta

### Auto Version Bumping and Publishing
**GitHub Actions workflow:**
1. Build and test on push to main
2. Check JaCoCo coverage (fails if <96%)
3. Package JAR
4. Deploy to packagecloud.io
5. Auto-increment version (Y++)
6. Commit version bump with `[ci skip]`

## Use Cases

### Use Case 1: JNexus (original use case)
```java
AESEncryption encryption = new AESEncryption();
String encryptedPassword = encryption.encrypt(password);
props.setProperty("nexus.password", encryptedPassword);
```

### Use Case 2: Other projects needing machine-specific encryption
```java
AESEncryption encryption = new AESEncryption();
String encryptedApiKey = encryption.encrypt(apiKey);
// Store encryptedApiKey in config file
```

### Use Case 3: Properties file integration
```java
CredentialHelper helper = new CredentialHelper();
helper.saveCredential(props, "database.password", "secret123");
// Automatically encrypts before saving to properties

String password = helper.loadCredential(props, "database.password");
// Automatically decrypts if encrypted, returns plaintext if not
```

## Security Properties

**Encryption algorithm:** AES-256-GCM (authenticated encryption)
- **Key derivation:** PBKDF2-HMAC-SHA256 (100,000 iterations)
- **Salt:** Hostname + user home directory (machine-specific)
- **IV:** Random 12-byte IV per encryption (stored with ciphertext)
- **Authentication tag:** 128-bit GCM tag prevents tampering
- **Encoding:** Base64 for storage in text files

**Machine-specific encryption:**
- Keys derived from hostname + user.home system property
- Encrypted credentials cannot be copied to other machines
- Changing hostname or user home directory invalidates credentials

**Backward compatibility:**
- `isEncrypted()` detects encrypted vs. plaintext
- `CredentialHelper.loadCredential()` handles both formats
- Automatic migration from plaintext to encrypted

## Alternatives Considered

### Alternative 1: Keep encryption in JNexus
- **Approach**: Don't extract, keep CredentialEncryption in JNexus
- **Rejected because**:
  - Encryption logic not specific to JNexus
  - Other FlossWare projects need same functionality
  - Limits code reuse across projects
  - Missed opportunity for ecosystem growth

### Alternative 2: Use existing encryption library
- **Approach**: Depend on Apache Commons Crypto, Google Tink, or Jasypt
- **Rejected because**:
  - Commons Crypto: Too low-level, requires more boilerplate
  - Google Tink: Heavyweight (many dependencies), complex API
  - Jasypt: Older design, no GCM mode, not machine-specific
  - Custom implementation better fits exact requirements

### Alternative 3: Inline encryption code
- **Approach**: Copy-paste encryption code into each project
- **Rejected because**:
  - Violates DRY principle
  - Bug fixes must be applied multiple times
  - No single source of truth for security code
  - Difficult to maintain consistent security across projects

### Alternative 4: Include encryption in a utils library
- **Approach**: Create flossware-utils with encryption + other utilities
- **Rejected because**:
  - Violates single responsibility principle
  - Forces projects to depend on unrelated utilities
  - Encryption deserves focused library and testing
  - Harder to version independently

## Distribution

**Maven repository:** packagecloud.io/flossware/java

**Installation:**
```xml
<!-- Add repository -->
<repositories>
    <repository>
        <id>flossware-java</id>
        <url>https://packagecloud.io/flossware/java/maven2</url>
    </repository>
</repositories>

<!-- Add dependency -->
<dependency>
    <groupId>org.flossware</groupId>
    <artifactId>jencrypt</artifactId>
    <version>1.0</version>
</dependency>
```

## References

- GitHub Issue #2: Implement password encryption (original JNexus requirement)
- jencrypt repository: https://github.com/FlossWare/jencrypt
- packagecloud.io: https://packagecloud.io/flossware/java
- SECURITY.md: Password encryption documentation
- CHANGELOG.md v1.30: jencrypt extraction

## Impact

**JNexus:**
- Removed 546 lines of duplicate code
- Cleaner separation of concerns
- External dependency on jencrypt 1.0

**FlossWare ecosystem:**
- New reusable library for other projects
- Consistent encryption approach across projects
- Public library demonstrates quality standards

**Future projects:**
- Can easily add secure credential storage
- Don't need to reinvent encryption
- Benefit from tested, maintained library

## Future Considerations

**Potential enhancements:**
- Support for different key derivation strategies (not just hostname + user.home)
- Pluggable encryption algorithms (AES-GCM, ChaCha20-Poly1305)
- Key rotation support
- Hardware security module (HSM) integration

**Stability:**
- Current implementation is stable and secure
- No planned breaking changes
- Future enhancements would be backward compatible

## Related Decisions

- GitHub Issue #2: Original encryption requirement
- GitHub Issue #3: URL encoding (separate security concern)
- SECURITY.md: Machine-specific encryption limitations

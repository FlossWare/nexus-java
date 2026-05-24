# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 1.x     | :white_check_mark: |

## Reporting a Vulnerability

If you discover a security vulnerability in JNexus, please report it by:

1. **DO NOT** open a public GitHub issue
2. Email the maintainer directly at: sfloess@redhat.com
3. Include:
   - Description of the vulnerability
   - Steps to reproduce
   - Potential impact
   - Suggested fix (if available)

You can expect:
- Acknowledgment within 48 hours
- Assessment and response within 7 days
- Credit in security advisory (if desired)

## Security Best Practices

### Credential Storage

**Desktop Application:**

The desktop application stores credentials in **plaintext** in `~/.flossware/nexus/nexus.properties`. This is a known limitation.

**Security recommendations:**

1. **File Permissions**: Ensure your properties file has restrictive permissions:
   ```bash
   chmod 600 ~/.flossware/nexus/nexus.properties
   ```

2. **Use Environment Variables**: For CI/CD and automated environments, use environment variables instead of files:
   ```bash
   export NEXUS_URL="https://nexus.example.com"
   export NEXUS_USER="your-username"
   export NEXUS_PASSWORD="your-token"
   ```

3. **Use Tokens, Not Passwords**: Generate Nexus user tokens instead of using your actual password:
   - Log into Nexus UI
   - User → Profile → User Token
   - Generate and use the token as your password

4. **Never Commit Credentials**: Add to `.gitignore`:
   ```gitignore
   # JNexus credentials
   .flossware/
   nexus.properties
   ```

5. **Use Profile-Based Configuration**: Separate credentials for different environments:
   ```bash
   # Development
   ~/.flossware/nexus/nexus-dev.properties
   
   # Production (stored securely, restricted access)
   ~/.flossware/nexus/nexus-prod.properties
   ```

**Android/iOS Applications:**

The mobile applications use encrypted credential storage:
- **Android**: AES256_GCM encryption via EncryptedSharedPreferences
- **iOS**: AES-256 hardware-backed encryption via Keychain Services

These platforms provide secure credential storage by default.

### Network Security

**Always Use HTTPS:**

JNexus will warn you when using HTTP:
```
WARNING: Using HTTP instead of HTTPS. Credentials will be sent over an insecure connection.
         Consider using HTTPS for production environments.
```

**Reasons to use HTTPS:**
- Credentials are sent using HTTP Basic Auth (Base64-encoded, not encrypted)
- HTTP transmits credentials in cleartext over the network
- Man-in-the-middle attacks can intercept credentials
- HTTPS encrypts all traffic, protecting credentials in transit

**Certificate Validation:**

Java's HttpClient validates SSL/TLS certificates by default. If you encounter certificate issues:

1. **Proper Fix**: Add the certificate to your Java truststore:
   ```bash
   keytool -import -alias nexus -file nexus.crt -keystore $JAVA_HOME/lib/security/cacerts
   ```

2. **DO NOT**: Disable certificate validation in production
3. **DO NOT**: Use self-signed certificates in production

### Input Validation

JNexus validates user input to prevent common attacks:

**Repository Names:**
- Alphanumeric characters, dots, hyphens, underscores only
- No path traversal sequences (`../`, `/`, `\`)
- No leading/trailing special characters

**URLs:**
- Valid URI/URL format required
- Must include scheme (http:// or https://)
- Must include hostname
- Malformed URLs are rejected with clear error messages

**Regular Expressions:**
- Regex patterns are validated before use
- Invalid patterns throw IllegalArgumentException
- No regex execution on untrusted input

### Safe Deletion Practices

**Always Test with Dry-Run First:**

```bash
# Desktop CLI
./jnexus.sh delete my-repo --regex ".*SNAPSHOT.*" --dry-run

# Shows what would be deleted WITHOUT actually deleting
```

**Review Before Confirming:**

The CLI requires explicit "yes" confirmation (not just "y") to prevent accidental deletion from shell history or scripts.

**Backup Critical Repositories:**

Before bulk deletions:
1. Export component list: Redirect output to file
2. Create Nexus repository backup
3. Test deletion on a non-production instance first

**Deletion is Permanent:**

Nexus does not provide an "undo" feature. Once deleted, components cannot be recovered unless you have a backup.

### Access Control

**Principle of Least Privilege:**

1. **Create Dedicated Nexus Users**: Don't use admin credentials
2. **Grant Minimum Permissions**: Only grant delete permissions to repositories that need cleanup
3. **Use Role-Based Access Control**: Nexus supports roles - create a "cleanup" role with limited permissions
4. **Rotate Credentials Regularly**: Change passwords/tokens periodically
5. **Audit Access**: Review Nexus audit logs for unauthorized access attempts

### CI/CD Security

**Secrets Management:**

Use your CI/CD platform's secrets management:

```yaml
# GitHub Actions
env:
  NEXUS_URL: ${{ secrets.NEXUS_URL }}
  NEXUS_USER: ${{ secrets.NEXUS_USER }}
  NEXUS_PASSWORD: ${{ secrets.NEXUS_TOKEN }}

# GitLab CI
variables:
  NEXUS_URL: $NEXUS_URL  # Defined in project settings
  NEXUS_USER: $NEXUS_USER
  NEXUS_PASSWORD: $NEXUS_PASSWORD
```

**Never:**
- Hard-code credentials in CI/CD configuration files
- Echo credentials in build logs
- Store credentials in version control
- Share credentials across multiple projects unnecessarily

### Container Security

**When Running in Docker:**

```bash
# Use secrets, not environment variables in docker-compose.yml
docker run -e NEXUS_URL -e NEXUS_USER -e NEXUS_PASSWORD jnexus

# Better: Use Docker secrets
docker secret create nexus_password /path/to/password.txt
docker service create --secret nexus_password jnexus
```

**Minimize Attack Surface:**
- Run as non-root user
- Use minimal base image
- Keep JDK updated
- Scan images for vulnerabilities

### Known Limitations

1. **Desktop Plaintext Storage**: Credentials stored in `~/.flossware/nexus/nexus.properties` are **not encrypted**
   - Mitigation: Use file permissions (chmod 600)
   - Mitigation: Use environment variables for automation
   - Mitigation: Use mobile apps (Android/iOS) for encrypted storage

2. **HTTP Basic Auth**: Credentials sent in HTTP headers (Base64-encoded)
   - Mitigation: Always use HTTPS
   - Mitigation: Use short-lived tokens instead of passwords

3. **No 2FA Support**: JNexus does not support two-factor authentication
   - Nexus itself may support 2FA at the server level
   - Use Nexus server-side 2FA for web UI access

### Security Checklist

Before deploying JNexus in production:

- [ ] Using HTTPS URLs only (no HTTP)
- [ ] Credentials file has chmod 600 permissions
- [ ] Using Nexus user tokens (not passwords)
- [ ] Credentials not committed to version control
- [ ] CI/CD uses secrets management (not plaintext)
- [ ] Tested dry-run before actual deletions
- [ ] Nexus user has minimum required permissions
- [ ] Regular credential rotation scheduled
- [ ] Audit logging enabled in Nexus
- [ ] Using latest JNexus version
- [ ] Using Java 21 with latest security patches

### Secure Deployment Example

```bash
# 1. Create restricted user in Nexus with cleanup permissions only
# (via Nexus UI)

# 2. Generate user token (not password)
# Nexus UI → User → Profile → User Token

# 3. Create properties file with restricted permissions
mkdir -p ~/.flossware/nexus
cat > ~/.flossware/nexus/nexus.properties <<EOF
nexus.url=https://nexus.example.com
nexus.user=cleanup-user
nexus.password=generated-token-here
EOF
chmod 600 ~/.flossware/nexus/nexus.properties

# 4. Verify HTTPS is used
grep "https://" ~/.flossware/nexus/nexus.properties

# 5. Test with dry-run first
./jnexus.sh delete my-repo --regex ".*old.*" --dry-run

# 6. Review output, then execute
./jnexus.sh delete my-repo --regex ".*old.*"
```

## Security Updates

Subscribe to security advisories:
- Watch this repository for security announcements
- Follow releases for security patches
- Update to latest version promptly

## Additional Resources

- [Nexus Security Best Practices](https://help.sonatype.com/repomanager3/security)
- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [Java Security Guide](https://docs.oracle.com/en/java/javase/21/security/)

# JNexus Configuration Guide

Back to [Getting Started](../getting-started.md) | [Troubleshooting](troubleshooting.md)

## Overview

JNexus supports three configuration methods across all platforms. This guide covers every setting, how profiles work, credential encryption, and environment variable overrides.

## Configuration Methods

### Method 1: Properties File (Desktop)

The default configuration method for the desktop application.

**Location:** `~/.flossware/nexus/nexus.properties`

**Setup:**
```bash
mkdir -p ~/.flossware/nexus
cat > ~/.flossware/nexus/nexus.properties << 'EOF'
# Required credentials
nexus.url=https://nexus.example.com
nexus.user=your-username
nexus.password=your-password

# Optional: Repository list (comma-separated)
nexus.repositories=maven-releases,maven-snapshots,npm-public,docker-hosted

# Optional: UI default values
nexus.default.repository=maven-releases
nexus.default.regex=.*SNAPSHOT.*
nexus.default.dryrun=true

# Optional: HTTP timeout (default: 30 seconds)
nexus.http.timeout.seconds=60
EOF

# Secure the file
chmod 600 ~/.flossware/nexus/nexus.properties
```

### Method 2: Environment Variables (Desktop, CI/CD)

Environment variables override properties file values. Recommended for CI/CD pipelines.

```bash
export NEXUS_URL=https://nexus.example.com
export NEXUS_USER=your-username
export NEXUS_PASSWORD=your-password
export NEXUS_HTTP_TIMEOUT=60
export NEXUS_PROFILE=dev
```

**Precedence:** Environment variables > properties file values (for authentication credentials).

### Method 3: In-App Settings (Android, iOS, macOS)

Mobile and macOS apps configure credentials through the Settings screen in the app. Credentials are encrypted automatically.

| Platform | Storage | Encryption |
|----------|---------|------------|
| Android | EncryptedSharedPreferences | AES256_GCM (master key via Android Keystore) |
| iOS/iPadOS | Keychain Services | AES-256 hardware-backed |
| macOS | Keychain Services | AES-256 hardware-backed |

## Configuration Properties Reference

### Required Properties

| Property | Environment Variable | Description |
|----------|---------------------|-------------|
| `nexus.url` | `NEXUS_URL` | Nexus server URL (must use HTTPS) |
| `nexus.user` | `NEXUS_USER` | Username for authentication |
| `nexus.password` | `NEXUS_PASSWORD` | Password or user token |

### Optional Properties

| Property | Environment Variable | Default | Description |
|----------|---------------------|---------|-------------|
| `nexus.repositories` | -- | (empty) | Comma-separated list of repository names |
| `nexus.default.repository` | -- | (empty) | Default repository for UI pre-population |
| `nexus.default.regex` | -- | (empty) | Default regex filter |
| `nexus.default.dryrun` | -- | `false` | Default dry-run setting |
| `nexus.http.timeout.seconds` | `NEXUS_HTTP_TIMEOUT` | `30` | HTTP timeout in seconds |

## Profile Support

Profiles allow you to manage multiple Nexus environments (dev, staging, prod) with separate configuration files.

### Creating Profiles

```bash
# Default profile (no suffix)
~/.flossware/nexus/nexus.properties

# Development profile
~/.flossware/nexus/nexus-dev.properties

# Production profile
~/.flossware/nexus/nexus-prod.properties

# Staging profile
~/.flossware/nexus/nexus-staging.properties
```

Each profile file has the same format as the default properties file, with environment-specific values.

### Using Profiles

**CLI flag:**
```bash
./jnexus.sh --profile dev list maven-snapshots
./jnexus.sh --profile prod list maven-releases
./jnexus.sh -p staging delete --dry-run test-repo
```

**Environment variable:**
```bash
export NEXUS_PROFILE=dev
./jnexus.sh list maven-snapshots
```

**GUI behavior:**
- If multiple profile files exist, a selection dialog appears on startup
- Swing: JOptionPane dropdown dialog
- AWT: Custom Dialog with Choice component
- Terminal: Numbered text menu before ncurses starts
- If only one profile exists, it is selected automatically

### Profile File Naming Convention

| Profile Name | File Name |
|-------------|-----------|
| (default) | `nexus.properties` |
| `dev` | `nexus-dev.properties` |
| `prod` | `nexus-prod.properties` |
| `staging` | `nexus-staging.properties` |
| `qa` | `nexus-qa.properties` |

A null, empty, or blank profile name defaults to `nexus.properties`.

### Profile Discovery

The `Credentials.discoverProfiles()` method scans `~/.flossware/nexus/` for files matching the pattern `nexus*.properties` and returns a list of profile names. The special name `default` is used for the base `nexus.properties` file.

## Desktop Password Encryption

Starting in version 1.30, the desktop application encrypts passwords using [JEncrypt](https://github.com/FlossWare/encrypt-java) when stored in properties files.

### How It Works

- **Algorithm**: AES-256-GCM
- **Key derivation**: PBKDF2-HMAC-SHA256 with 100,000 iterations
- **Machine-specific**: Key derived from hostname and user home directory
- **Random IV**: 12-byte IV per encryption (different ciphertext for same password)
- **Authentication**: 128-bit GCM authentication tag prevents tampering
- **Auto-migration**: Existing plaintext passwords are encrypted on next save

### Implications

- Encrypted credentials cannot be copied to other machines (machine-specific key)
- If you change your hostname or home directory, you must re-enter the password
- Older JNexus versions see the encrypted string as-is (backward compatible but unusable)

### Manual Encryption

```bash
java -jar jencrypt.jar encrypt mypassword
```

## Mobile/macOS Credential Encryption

### Android (EncryptedSharedPreferences)

- **Master key**: Android Keystore with AES256_GCM scheme
- **Key encryption**: AES256_SIV (deterministic, for looking up keys)
- **Value encryption**: AES256_GCM (authenticated encryption for values)
- **Storage**: Private app directory, inaccessible to other apps
- **Access**: Only the JNexus app can read the credentials

### iOS/iPadOS/macOS (Keychain Services)

- **Encryption**: AES-256 hardware-backed (Secure Enclave on devices with it)
- **Access control**: `kSecAttrAccessibleWhenUnlocked` (encrypted at rest)
- **Storage**: System keychain, sandboxed to the app
- **Service ID**: `com.flossware.jnexus`
- **Non-sensitive settings**: UserDefaults (repositories, defaults, timeout)

## Interactive Credential Collection

All desktop GUIs (Swing, AWT, Terminal) support interactive credential entry when no configuration file exists:

1. A dialog prompts for URL, username, password, and optional repository list
2. After entering credentials, you can optionally save them to `~/.flossware/nexus/nexus.properties`
3. When saving with a repository list, the first repository automatically becomes `nexus.default.repository`
4. On next launch, UI fields are pre-populated from the saved defaults

## Example Configurations

### Minimal Configuration

```properties
nexus.url=https://nexus.example.com
nexus.user=admin
nexus.password=secret
```

### Full Configuration

```properties
# Server credentials
nexus.url=https://nexus.example.com
nexus.user=admin
nexus.password=encrypted-password-here

# Repository list for dropdowns and batch operations
nexus.repositories=maven-releases,maven-snapshots,npm-public,docker-hosted

# UI defaults
nexus.default.repository=maven-releases
nexus.default.regex=.*SNAPSHOT.*
nexus.default.dryrun=true

# Network
nexus.http.timeout.seconds=60
```

### CI/CD Configuration

```bash
# In CI/CD pipeline (e.g., GitHub Actions)
export NEXUS_URL=${{ secrets.NEXUS_URL }}
export NEXUS_USER=${{ secrets.NEXUS_USER }}
export NEXUS_PASSWORD=${{ secrets.NEXUS_PASSWORD }}

./jnexus.sh delete --yes maven-snapshots ".*SNAPSHOT.*"
```

## Security Best Practices

1. **Use HTTPS**: Always use `https://` for the Nexus URL
2. **Protect properties files**: `chmod 600 ~/.flossware/nexus/nexus.properties`
3. **Use tokens**: Use Nexus user tokens instead of passwords (Nexus UI > Profile > User Token)
4. **Use environment variables for CI/CD**: Never store credentials in code repositories
5. **Never commit credentials**: Add `*.properties` and `.flossware/` to `.gitignore`
6. **Rotate credentials**: Change passwords and tokens regularly
7. **Minimal permissions**: Use dedicated Nexus users with minimum required permissions

## Video Tutorials

See [Multi-Profile Configuration](../video-tutorials/scripts/09-multi-profile-config.md) (3 min) for a visual walkthrough of creating profiles, switching between environments, and environment-specific workflows.

Back to [Getting Started](../getting-started.md) | [Troubleshooting](troubleshooting.md)

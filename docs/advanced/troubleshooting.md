# JNexus Troubleshooting Guide

Back to [Getting Started](../getting-started.md) | [Configuration](configuration-guide.md)

## Overview

This guide covers common errors and issues across all JNexus platforms, with solutions and diagnostic steps.

## Connection Errors

### Connection Refused

**Error:** `Connection refused` or `ConnectException`

**Causes:**
- Nexus server is not running
- Wrong URL (incorrect host, port, or path)
- Firewall blocking the connection

**Solutions:**
1. Verify the Nexus URL is correct: `curl -I https://nexus.example.com`
2. Check the server is running and accessible from your machine
3. Ensure no firewall or proxy is blocking the connection
4. Verify port number (default Nexus ports: 8081 for HTTP, 8443 for HTTPS)

### Connection Timeout

**Error:** `SocketTimeoutException` or `Connection timed out`

**Causes:**
- Nexus server is slow to respond
- Network latency
- Very large repository taking too long to page through

**Solutions:**
1. Increase the HTTP timeout:
   - Properties: `nexus.http.timeout.seconds=120`
   - Environment: `export NEXUS_HTTP_TIMEOUT=120`
   - Android/iOS: Settings > HTTP Timeout
2. Check network connectivity: `ping nexus.example.com`
3. Try a smaller repository first to confirm connectivity

### Unknown Host

**Error:** `UnknownHostException` or `nodename nor servname provided`

**Causes:**
- Typo in the Nexus URL
- DNS resolution failure
- VPN not connected

**Solutions:**
1. Verify the hostname: `nslookup nexus.example.com`
2. Check for typos in `NEXUS_URL` or `nexus.url`
3. If using VPN, ensure it is connected

### SSL/TLS Errors

**Error:** `SSLHandshakeException` or `certificate verify failed`

**Causes:**
- Self-signed certificate on Nexus server
- Expired certificate
- Certificate authority not trusted

**Solutions:**
1. Update your Java truststore with the Nexus server certificate
2. For testing only (not recommended for production):
   ```bash
   java -Djavax.net.ssl.trustStore=/path/to/truststore.jks \
       -jar target/jnexus-1.0-jar-with-dependencies.jar list my-repo
   ```
3. Contact your Nexus administrator to fix the certificate

## Authentication Errors

### 401 Unauthorized

**Error:** `HTTP 401 Unauthorized`

**Causes:**
- Wrong username or password
- Account locked or disabled
- User token expired

**Solutions:**
1. Verify credentials: try logging into the Nexus web UI with the same credentials
2. Check for typos (especially trailing spaces or newlines)
3. If using a user token, regenerate it: Nexus UI > Profile > User Token
4. Ensure environment variables do not override properties file values unintentionally

### 403 Forbidden

**Error:** `HTTP 403 Forbidden`

**Causes:**
- User lacks permission to access the repository
- Repository requires specific role/privilege

**Solutions:**
1. Contact your Nexus administrator to grant appropriate permissions
2. Verify the repository name is correct: `./jnexus.sh list` shows repositories you have access to
3. Ensure the user has at least "browse" privilege for list operations and "delete" privilege for delete operations

## Build Errors

### Could Not Find or Load Main Class

**Error:** `Could not find or load main class`

**Solutions:**
1. Rebuild the project: `./mvnw clean package`
2. Verify the JAR exists: `ls target/jnexus-*-jar-with-dependencies.jar`
3. Check Java version: `java -version` (must be 21+)

### Maven Build Failure

**Error:** Various Maven build errors

**Solutions:**
1. Check Java version: `java -version` (must be 21+)
2. Clean and rebuild: `./mvnw clean package -DskipTests`
3. Delete local Maven cache and retry: `rm -rf ~/.m2/repository/org/flossware`
4. Check internet connection (Maven needs to download dependencies)

### Android Build Failure

**Error:** Gradle build errors for Android module

**Solutions:**
1. Check Android SDK is installed: `echo $ANDROID_HOME`
2. Accept SDK licenses: `$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses`
3. Check Gradle version: `./gradlew --version`
4. Clean build: `./gradlew clean :jnexus-android:assembleDebug`

## Desktop GUI Issues

### Swing Window Does Not Appear

**Causes:**
- No display environment (running on headless server)
- Java running in headless mode

**Solutions:**
1. Check DISPLAY environment variable: `echo $DISPLAY`
2. If running over SSH, enable X11 forwarding: `ssh -X user@host`
3. Alternatively, use the Terminal UI which does not require X11

### AWT Text Appears Garbled

**Solutions:**
1. Resize the window to trigger a redraw
2. Check system font settings
3. Try a different Java version or distribution

### Terminal UI: ncurses Not Available

**Error:** `ncurses library is not available!`

**Solutions:**
Install the ncurses development package:
```bash
# Ubuntu/Debian
sudo apt-get install libncurses-dev

# Fedora/RHEL
sudo dnf install ncurses-devel

# Arch Linux
sudo pacman -S ncurses
```

### Terminal UI: Cannot Type in Fields

**Solution:** Use TAB to navigate to the field first. The focused field shows `>` and `<` markers.

### Terminal UI: Display Garbled

**Solutions:**
1. Resize terminal to at least 120x40 characters
2. Check terminal supports ncurses: `echo $TERM` (should be `xterm-256color` or similar)
3. Try a different terminal emulator

## Mobile App Issues

### Android: App Crashes on Startup

**Solutions:**
1. Clear app data: Settings > Apps > JNexus > Storage > Clear Data
2. Reinstall the APK
3. Check Android version is 8.0+ (API 26+)

### Android: Cannot Install APK

**Solutions:**
1. Enable "Install from Unknown Sources": Settings > Security > Unknown Sources
2. On Android 8+: Settings > Apps > Browser/Files > Install Unknown Apps
3. Check device storage space

### iOS: Network Errors

**Solutions:**
1. Check App Transport Security is not blocking your Nexus URL
2. Ensure URL uses HTTPS (ATS blocks HTTP by default)
3. For local development servers, local networking is allowed via Info.plist

### iOS/macOS: Keychain Errors

**Solutions:**
1. When prompted, allow JNexus to access Keychain
2. If issues persist, delete and reinstall the app
3. On macOS, check Keychain Access app for stored credentials (service: `com.flossware.jnexus`)

## Configuration Issues

### Properties File Not Found

**Error:** `Failed to initialize Nexus client` or `Configuration not found`

**Solutions:**
1. Verify file exists: `ls -la ~/.flossware/nexus/nexus.properties`
2. Check file permissions: `chmod 600 ~/.flossware/nexus/nexus.properties`
3. If using profiles, check profile file: `ls ~/.flossware/nexus/nexus-*.properties`
4. Use environment variables as a fallback

### Profile Not Found

**Error:** `Configuration file not found for profile: xyz`

**Solutions:**
1. Check profile file exists: `ls ~/.flossware/nexus/nexus-xyz.properties`
2. Verify spelling matches: profile name must match file suffix exactly
3. Use `default` or omit profile to use `nexus.properties`

### Environment Variables Not Taking Effect

**Solutions:**
1. Check variables are exported: `echo $NEXUS_URL`
2. Check for typos in variable names
3. If running from a script, ensure variables are exported (not just set)
4. Note: environment variables override properties file values

## Performance Issues

### Slow Listing for Large Repositories

**Causes:**
- Repository has many components (10,000+)
- Multiple pages need to be fetched from Nexus API
- Network latency

**Solutions:**
1. Use the cache: `List` is faster than `Refresh` after the first fetch
2. Use regex filters to narrow results
3. Increase HTTP timeout for very large repositories
4. Check Nexus server performance (slow server = slow fetching)

### Cache Not Working

**Solutions:**
1. Cache TTL is 5 minutes by default -- wait before checking if cache works
2. Use `List` (cached) instead of `Refresh` (bypasses cache)
3. Delete operations always clear the cache
4. Cache is per-repository: different repositories have independent caches

## Retry Logic

JNexus automatically retries failed requests with exponential backoff:

| Attempt | Delay |
|---------|-------|
| 1st retry | 1 second |
| 2nd retry | 2 seconds |
| 3rd retry | 4 seconds |

**Retried errors:**
- HTTP 5xx (server errors)
- HTTP 408 (request timeout)
- HTTP 429 (too many requests)
- Connection errors
- Socket timeouts

**Not retried:**
- HTTP 401 (unauthorized)
- HTTP 403 (forbidden)
- HTTP 404 (not found)
- HTTP 400 (bad request)

## Getting More Information

### Verbose Mode (Desktop)

Enable debug logging for detailed output:
```bash
./jnexus.sh --verbose list maven-releases
```

### Quiet Mode (Desktop)

Show only warnings and errors:
```bash
./jnexus.sh --quiet list maven-releases
```

### Android Logcat

```bash
adb logcat | grep JNexus
```

### Checking Nexus Server Directly

Test connectivity without JNexus:
```bash
# Check server is reachable
curl -I https://nexus.example.com

# List repositories (authenticated)
curl -u user:password https://nexus.example.com/service/rest/v1/repositories

# List components in a repository
curl -u user:password \
    "https://nexus.example.com/service/rest/v1/components?repository=maven-releases"
```

## Reporting Issues

If you cannot resolve an issue:

1. Check [GitHub Issues](https://github.com/FlossWare/nexus-java/issues) for existing reports
2. Open a new issue with:
   - JNexus version
   - Java version (`java -version`)
   - Operating system and version
   - Full error message or stack trace
   - Steps to reproduce
   - Whether the issue occurs on CLI, GUI, or mobile

## Video Tutorial

See [Troubleshooting Common Issues](../video-tutorials/scripts/10-troubleshooting.md) (4 min) for a visual walkthrough of diagnosing connection errors, authentication failures, timeout issues, and cache problems.

Back to [Getting Started](../getting-started.md) | [Configuration](configuration-guide.md)

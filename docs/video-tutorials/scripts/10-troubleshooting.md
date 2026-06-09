# Video 10: Troubleshooting Common Issues

**Length:** 4:00  
**Audience:** Users experiencing problems  
**Goal:** Cover the most frequent issues and their solutions

---

## Scene 1: Introduction (0:00 - 0:15)

**Visual:** Intro card, then a terminal showing an error message.

**Voiceover:**
> "Running into issues with JNexus? In this video, we'll cover the five most common problems and how to fix them."

---

## Scene 2: Connection Errors (0:15 - 1:15)

**Visual:** Terminal showing a connection refused or timeout error.

```
Error: Failed to connect to nexus.example.com
```

**Voiceover:**
> "Connection errors usually mean JNexus can't reach your Nexus server. Here's a checklist."

**Visual:** Show each step.

```bash
# 1. Verify the URL is correct
grep nexus.url ~/.flossware/nexus/nexus.properties

# 2. Test connectivity
curl -I https://nexus.example.com

# 3. Check for HTTPS requirement
# Make sure the URL starts with https://, not http://

# 4. Check VPN or network access
ping nexus.example.com
```

**Voiceover:**
> "First, verify the URL in your properties file is correct. Second, test basic connectivity with curl. Third, make sure you're using HTTPS -- JNexus uses HTTPS by default. And fourth, if your Nexus server is behind a VPN or firewall, make sure you're connected."

**Visual:** Show fixing the URL and retrying.

> "Most connection errors come down to a typo in the URL or a missing VPN connection."

---

## Scene 3: Authentication Failures (1:15 - 2:00)

**Visual:** Terminal showing a 401 Unauthorized error.

```
Error: HTTP 401 - Unauthorized
```

**Voiceover:**
> "A 401 error means your credentials are wrong or expired."

```bash
# 1. Verify username
grep nexus.user ~/.flossware/nexus/nexus.properties

# 2. Test credentials directly
curl -u "username:password" https://nexus.example.com/service/rest/v1/repositories

# 3. Check if using a token (recommended)
# Nexus UI > User > Profile > User Token

# 4. Regenerate token if expired
# Nexus tokens may have expiration dates
```

**Voiceover:**
> "Verify your username is correct. Test the credentials directly with curl to rule out a JNexus issue. If you're using a Nexus user token -- which is recommended -- check if it's expired. Regenerate it from the Nexus web UI under your user profile."

> "Also check the file permissions on your properties file. If the file is world-readable, another process might have modified it."

---

## Scene 4: Timeout Issues (2:00 - 2:45)

**Visual:** Terminal showing a timeout error.

```
Error: Request timed out after 30 seconds
```

**Voiceover:**
> "Timeout errors occur when the Nexus server takes too long to respond. This is common with large repositories."

```bash
# Increase the timeout in your properties file
echo "nexus.http.timeout.seconds=120" >> ~/.flossware/nexus/nexus.properties

# Or via environment variable
export NEXUS_HTTP_TIMEOUT=120
./nexus-java.sh list large-repository
```

**Voiceover:**
> "Increase the HTTP timeout from the default 30 seconds. For very large repositories with thousands of components, you might need 60 or even 120 seconds."

> "If timeouts persist, the Nexus server itself may be overloaded. Check with your Nexus administrator."

---

## Scene 5: Cache Problems (2:45 - 3:15)

**Visual:** Terminal showing stale data and the Refresh command.

**Voiceover:**
> "JNexus caches list results for five minutes to reduce server load. If you've made changes in the Nexus web UI or from another tool, JNexus might show stale data."

```bash
# CLI: Use Refresh instead of List
./nexus-java.sh list --refresh maven-releases

# GUI: Click the Refresh button instead of List
```

**Voiceover:**
> "Use the Refresh operation instead of List to bypass the cache and fetch fresh data. In the GUI, click the Refresh button. If you need to disable caching entirely, you can set the cache TTL to zero."

---

## Scene 6: Where to Get Help (3:15 - 4:00)

**Visual:** GitHub Issues page, then verbose mode output.

**Voiceover:**
> "If none of these solutions work, try running with verbose mode enabled."

```bash
# Enable verbose logging
./nexus-java.sh --verbose list maven-releases
```

> "Verbose mode shows detailed debug output including HTTP request and response details. This information is invaluable for diagnosing issues."

**Visual:** Show the GitHub Issues page.

> "If you're still stuck, open a GitHub issue. Include the verbose output, your Java version, operating system, and the error message. We'll do our best to help."

```bash
# Include this information in your issue:
java -version
./nexus-java.sh --version
uname -a  # or systeminfo on Windows
```

**Visual:** Fade to outro card with GitHub links.

---

## Production Notes

- Reproduce each error scenario for authentic screenshots/recordings
- Use realistic error messages, not fabricated ones
- Keep solutions concise and actionable
- Test all suggested fixes to verify they work

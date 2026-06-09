# Video 9: Multi-Profile Configuration

**Length:** 3:00  
**Audience:** Users managing multiple Nexus environments  
**Goal:** Demonstrate profile-based configuration for multiple environments

---

## Scene 1: Profile Concept (0:00 - 0:30)

**Visual:** Intro card, then a diagram showing Dev, Staging, and Prod environments.

**Voiceover:**
> "If you work with multiple Nexus instances -- say, development, staging, and production -- switching credentials manually is tedious and error-prone. JNexus profiles let you define separate configurations and switch between them with a single flag."

---

## Scene 2: Creating Profile Files (0:30 - 1:15)

**Visual:** Terminal showing file creation.

```bash
# Default profile (no suffix)
ls ~/.flossware/nexus/nexus.properties

# Create a dev profile
cp ~/.flossware/nexus/nexus.properties ~/.flossware/nexus/nexus-dev.properties

# Create a prod profile
cp ~/.flossware/nexus/nexus.properties ~/.flossware/nexus/nexus-prod.properties
```

**Voiceover:**
> "Profiles are just separate property files with a dash-suffix naming convention. The default profile is nexus.properties. Add '-dev' for development, '-prod' for production, and so on."

**Visual:** Show editing the dev profile with different credentials.

```properties
# nexus-dev.properties
nexus.url=https://nexus-dev.example.com
nexus.user=dev-user
nexus.password=dev-token
nexus.repositories=maven-snapshots,npm-dev
nexus.default.repository=maven-snapshots
```

**Voiceover:**
> "Each profile file has its own URL, credentials, repository list, and default values. This keeps your environments completely isolated."

```bash
# Set file permissions on all profiles
chmod 600 ~/.flossware/nexus/nexus-*.properties
```

---

## Scene 3: Switching Profiles in CLI (1:15 - 1:45)

**Visual:** Terminal with profile flag usage.

```bash
# Use the dev profile
./nexus-java.sh --profile dev list maven-snapshots

# Use the prod profile
./nexus-java.sh -p prod list maven-releases

# Use the default profile (no flag)
./nexus-java.sh list maven-releases
```

**Voiceover:**
> "On the command line, use the --profile flag followed by the profile name. The short form is -p. Without any profile flag, JNexus uses the default nexus.properties file."

```bash
# Or use an environment variable
export NEXUS_PROFILE=dev
./nexus-java.sh list maven-snapshots
```

**Voiceover:**
> "You can also set the NEXUS_PROFILE environment variable. This is useful in shell scripts or CI/CD pipelines where you want to set the profile once and run multiple commands."

---

## Scene 4: Switching Profiles in GUI (1:45 - 2:15)

**Visual:** Swing GUI showing the profile selection dialog at startup.

**Voiceover:**
> "When you launch the Swing or AWT GUI and multiple profile files are detected, JNexus shows a dialog asking which profile to use. Select the one you want and the GUI loads that configuration."

**Visual:** Show the config file display in the GUI confirming which profile is active.

> "The configuration file path is displayed in the GUI so you can always see which profile is active. If you need to switch profiles, close and relaunch the application."

---

## Scene 5: Environment-Specific Workflows (2:15 - 3:00)

**Visual:** Split screen showing dev and prod terminals side by side.

**Voiceover:**
> "Here's a practical workflow. During development, you might clean up snapshots frequently."

```bash
# Clean dev snapshots older than 7 days
./nexus-java.sh -p dev delete --dry-run maven-snapshots ".*SNAPSHOT.*"
```

> "In production, you'd be much more careful -- using dry-run, specific regex patterns, and maybe listing statistics first."

```bash
# Check prod stats before any cleanup
./nexus-java.sh -p prod stats maven-releases

# Preview carefully before touching prod
./nexus-java.sh -p prod delete --dry-run maven-releases ".*-1\.0\..*"
```

> "Profiles make it easy to have aggressive cleanup policies in development while being conservative in production."

**Visual:** Fade to outro card.

---

## Production Notes

- Show at least two profiles (dev and prod) with visually distinct Nexus instances
- Emphasize the safety aspect -- profiles prevent accidentally running against prod
- Show the file system layout with all profile files listed

# Video 2: Getting Started with JNexus CLI

**Length:** 5:00  
**Audience:** CLI users, DevOps engineers  
**Goal:** Walk through installation, configuration, and basic commands

---

## Scene 1: Introduction (0:00 - 0:15)

**Visual:** Intro card, then terminal window.

**Voiceover:**
> "In this tutorial, you'll learn how to install, configure, and use the JNexus command-line interface. By the end, you'll be listing, filtering, and safely deleting Nexus components from the terminal."

---

## Scene 2: Installation (0:15 - 0:45)

**Visual:** Terminal showing build steps.

```bash
# Clone the repository
git clone https://github.com/FlossWare/nexus-java.git
cd nexus-java

# Build with Maven wrapper (no Maven installation needed)
./mvnw clean package

# Verify the JAR was created
ls -la target/nexus-java-1.0-jar-with-dependencies.jar
```

**Voiceover:**
> "Clone the repository and build with the included Maven wrapper. You'll need Java 21 or higher. The build produces a single executable JAR with all dependencies included -- about 5 to 10 megabytes."

---

## Scene 3: Configuration (0:45 - 1:45)

**Visual:** Terminal showing configuration steps, then editing the properties file.

```bash
# Create the configuration directory
mkdir -p ~/.flossware/nexus

# Copy the example properties file
cp src/main/resources/nexus-java.properties.example ~/.flossware/nexus/nexus.properties

# Edit the properties file
vi ~/.flossware/nexus/nexus.properties
```

**Visual:** Show the properties file content:

```properties
nexus.url=https://nexus.example.com
nexus.user=your-username
nexus.password=your-password-or-token

# Optional: default values for UI
#nexus.default.repository=maven-releases
#nexus.default.regex=.*SNAPSHOT.*
#nexus.default.dryrun=true

# Optional: repository list
#nexus.repositories=maven-releases,maven-snapshots
```

**Voiceover:**
> "Create a configuration directory and copy the example properties file. Edit it with your Nexus server URL, username, and password. I recommend using a Nexus user token instead of your actual password for better security."

> "You can also set optional defaults like a default repository, regex pattern, and dry-run mode. These will pre-populate the UI fields when you launch."

**Visual:** Show setting file permissions.

```bash
# Protect your credentials file
chmod 600 ~/.flossware/nexus/nexus.properties
```

**Voiceover:**
> "Don't forget to restrict file permissions so only your user can read the credentials."

---

## Scene 4: List Command (1:45 - 2:45)

**Visual:** Terminal executing list commands with output.

```bash
# List all components in a repository
./nexus-java.sh list maven-releases

# Output shows ID, file size, and path for each component
```

**Voiceover:**
> "The list command shows all components in a repository. You'll see the component ID, file size in bytes, and the path in the repository."

```bash
# Filter with regex
./nexus-java.sh list maven-releases ".*SNAPSHOT.*"

# List with verbose output
./nexus-java.sh --verbose list maven-releases
```

**Voiceover:**
> "Add a regex pattern to filter results. Only components whose path matches the pattern will be shown. Use the verbose flag for detailed debug logging, which is helpful for troubleshooting."

---

## Scene 5: Delete with Dry-Run (2:45 - 4:15)

**Visual:** Terminal showing dry-run followed by actual delete.

```bash
# ALWAYS start with dry-run
./nexus-java.sh delete --dry-run maven-releases ".*-1.0-SNAPSHOT.*"

# Output: "DRY RUN - Would delete: component-id (path/to/artifact.jar)"
```

**Voiceover:**
> "Before deleting anything, always use the dry-run flag. This shows you exactly what would be deleted without actually removing anything. Review the list carefully."

```bash
# When satisfied, delete for real (with confirmation prompt)
./nexus-java.sh delete maven-releases ".*-1.0-SNAPSHOT.*"

# Output: "Delete 5 components? (yes/no): "
# Type: yes
```

**Voiceover:**
> "When you're satisfied with the results, remove the dry-run flag. JNexus will ask for confirmation before proceeding. Type 'yes' to confirm. You can skip this prompt with the --yes flag, but use that with caution."

---

## Scene 6: Statistics Command (4:15 - 5:15)

**Visual:** Terminal showing the stats command output.

```bash
# View repository statistics
./nexus-java.sh stats maven-releases
```

**Voiceover:**
> "The statistics command gives you a comprehensive overview of your repository: total component count, total and average size, size distribution across five buckets, file type breakdown, and age distribution. This is invaluable for understanding what's taking up space and planning cleanup."

---

## Scene 7: Tips and Next Steps (4:45 - 5:00)

**Visual:** Bullet points on screen.

**Voiceover:**
> "A few tips to get the most out of JNexus: Use profiles for multiple environments -- just add a --profile flag. Combine regex patterns with dry-run for safe, targeted cleanup. And check out the Swing GUI if you prefer a graphical interface. Links to all the documentation are in the description below."

**Visual:** Fade to outro card.

---

## Production Notes

- Use a real or realistic Nexus instance with test data
- Ensure demo credentials use example.com, never real servers
- Show actual command output, not mocked text
- Pause briefly after each command to let viewers read the output

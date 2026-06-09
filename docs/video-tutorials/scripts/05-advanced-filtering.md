# Video 5: Advanced Filtering and Search

**Length:** 4:00  
**Audience:** Power users  
**Goal:** Demonstrate all filter types and how to combine them

---

## Scene 1: Introduction (0:00 - 0:15)

**Visual:** Intro card, then the Swing GUI search panel or CLI help output.

**Voiceover:**
> "JNexus provides five types of filters that you can combine for precise component selection. In this tutorial, we'll cover each one and show how they work together."

---

## Scene 2: Regex Patterns (0:15 - 1:15)

**Visual:** Terminal or GUI showing regex filter in action.

```bash
# Match all SNAPSHOT artifacts
./nexus-java.sh list maven-releases ".*SNAPSHOT.*"

# Match a specific version
./nexus-java.sh list maven-releases ".*-2\.1\.0-.*"

# Match all JAR files
./nexus-java.sh list maven-releases ".*\.jar$"

# Match a specific group
./nexus-java.sh list maven-releases "^com/example/.*"
```

**Voiceover:**
> "Regex patterns match against the full component path. Use dot-star for wildcards. Remember to escape literal dots with a backslash. The caret anchors to the start of the path, and the dollar sign anchors to the end."

**Visual:** Show common regex patterns in a reference table.

```
Pattern              Matches
------               -------
.*SNAPSHOT.*         Any path containing SNAPSHOT
.*\.jar$             Paths ending in .jar
^com/example/.*      Paths starting with com/example/
.*-1\.[0-9]-.*       Version 1.0 through 1.9
```

**Voiceover:**
> "Here are some commonly useful patterns. The regex uses standard Java regex syntax, so you have the full power of regular expressions at your disposal."

---

## Scene 3: Size Range Filters (1:15 - 2:00)

**Visual:** GUI advanced filter panel or CLI with search command.

**Voiceover:**
> "Size range filters let you find components by their file size. Set a minimum size, a maximum size, or both. Sizes are specified in bytes."

**Visual:** Show filtering for large files (> 10MB) and for small files (< 1KB).

> "For example, filtering for components larger than 10 megabytes helps you find the biggest space consumers. Filtering for very small components might reveal empty or stub artifacts that could be cleaned up."

---

## Scene 4: Date Range Filters (2:00 - 2:45)

**Visual:** Show date filter inputs.

**Voiceover:**
> "Date range filters let you find components by creation date. Use 'created after' to find recent uploads, or 'created before' to find old artifacts that might be candidates for cleanup."

**Visual:** Show filtering for components older than 90 days.

> "This is particularly useful for retention policies. For example, you might want to find all snapshot artifacts created more than 90 days ago."

---

## Scene 5: File Extension Filters (2:45 - 3:15)

**Visual:** Show file extension filter input.

**Voiceover:**
> "The file extension filter narrows results to specific file types. Enter 'jar' to see only JAR files, 'pom' for POM files, or 'zip' for ZIP archives."

**Visual:** Show results filtered by extension.

> "This is useful when you want to focus on a specific artifact type. For example, if you're analyzing storage usage, you might filter to JAR files only since they typically consume the most space."

---

## Scene 6: Combining Filters (3:15 - 3:45)

**Visual:** GUI with multiple filters applied simultaneously.

**Voiceover:**
> "The real power comes from combining filters. All filters use AND logic -- a component must match every active filter to appear in the results."

**Visual:** Show example: regex for SNAPSHOT + size > 1MB + created before 30 days ago.

> "For example, you could search for SNAPSHOT artifacts larger than one megabyte that were created more than 30 days ago. This gives you a targeted list of large, stale snapshots -- perfect candidates for cleanup."

---

## Scene 7: Wrap-Up (3:45 - 4:00)

**Visual:** Summary bullet points.

**Voiceover:**
> "Remember: filters are applied client-side after fetching data from Nexus. The more specific your initial regex, the less data needs to be processed. And as always, combine filtering with dry-run mode before deleting anything."

**Visual:** Fade to outro card.

---

## Production Notes

- Show side-by-side before/after when filters are applied
- Use a repository with diverse component types and sizes for clear demonstrations
- Highlight the filter fields being used in the GUI with visual indicators

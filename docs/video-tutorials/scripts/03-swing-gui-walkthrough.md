# Video 3: JNexus Swing GUI Walkthrough

**Length:** 6:00  
**Audience:** Desktop users preferring a graphical interface  
**Goal:** Complete tour of the Swing GUI features

---

## Scene 1: Introduction (0:00 - 0:15)

**Visual:** Intro card, then the Swing GUI launching.

**Voiceover:**
> "In this tutorial, we'll walk through the JNexus Swing GUI -- a full-featured desktop interface for managing your Nexus repositories. Everything you can do on the command line, you can do here with point-and-click convenience."

---

## Scene 2: Launch and Credential Setup (0:15 - 1:00)

**Visual:** Terminal launching the Swing GUI, then the credential dialog appearing.

```bash
./nexus-java-swing.sh
```

**Voiceover:**
> "Launch the Swing GUI with the shell script. If this is your first time and no configuration file exists, a dialog will appear asking for your Nexus credentials."

**Visual:** Show the credential input dialog. Fill in URL, username, and password. Check "Save credentials."

**Voiceover:**
> "Enter your Nexus server URL, username, and password. Check the 'Save credentials' option to store them encrypted for future sessions. If you have multiple configuration profiles, a selection dialog will appear instead."

**Visual:** Show the profile selection dialog (if multiple profiles exist).

**Voiceover:**
> "If you have multiple profiles configured -- for example, dev and prod -- you'll see a dialog to choose which one to use."

---

## Scene 3: Interface Tour (1:00 - 2:00)

**Visual:** Slowly pan across the GUI, highlighting each area.

**Voiceover:**
> "Let's tour the interface. At the top, you have the repository configuration panel. The repository dropdown lets you select from your configured repositories, or choose 'All' to query across everything."

**Visual:** Point to the regex filter field.

> "The regex filter field accepts Java regex patterns. You can type a pattern and press Enter to trigger a list operation directly."

**Visual:** Point to the dry-run checkbox.

> "The dry-run checkbox is checked by default for safety. When enabled, delete operations only show what would be removed without actually deleting anything."

**Visual:** Point to the Nexus URL and config file display.

> "Below that, you can see which Nexus server you're connected to and which configuration file is active."

**Visual:** Point to the button bar.

> "The button bar has List for cached results, Refresh to bypass the cache, Delete All for bulk deletion, Clear to reset the results, and Quit to exit. When you select rows in the table, a Delete Selected button appears."

**Visual:** Point to the results table and status bar.

> "The results table has four columns: ID, File Size in bytes, File Size in megabytes, and Path. Click any column header to sort. The status bar at the bottom shows operation feedback."

---

## Scene 4: List and Refresh (2:00 - 2:45)

**Visual:** Click the List button. Show results populating the table.

**Voiceover:**
> "Click List to fetch components. The cursor changes to a busy indicator while loading. Results appear in the table with a summary row at the bottom showing total count and size."

**Visual:** Click Refresh.

> "The Refresh button bypasses the cache and fetches fresh data from the server. Use this after making changes or if you suspect the cache is stale. The default cache duration is five minutes."

---

## Scene 5: Advanced Filters (2:45 - 4:15)

**Visual:** Show the advanced filters panel (expand it if collapsible).

**Voiceover:**
> "JNexus supports advanced filtering through the collapsible filters panel. Let's look at each filter type."

**Visual:** Type a regex in the filter field, press Enter.

> "The regex filter matches against the full component path. For example, entering '.*SNAPSHOT.*' shows only snapshot artifacts."

**Visual:** Show the table filtering in real time.

> "Results update immediately. The summary row recalculates to reflect only the filtered results."

**Visual:** Show additional filter options (size range, date range, extension, name pattern).

> "You can also filter by size range -- for example, only components larger than 10 megabytes. Date range filters let you find components created before or after a specific date. And the file extension filter narrows results to specific file types like JAR, POM, or ZIP."

> "All filters combine with AND logic, so you can stack them for very precise results."

---

## Scene 6: Multi-Select and Delete (4:15 - 5:15)

**Visual:** Click on a row to select it. Ctrl-click to select additional rows. Show the "Delete Selected" button appearing and the selection status.

**Voiceover:**
> "Click a row to select it. Hold Control and click to select multiple rows, or Shift-click for a range. When rows are selected, a 'Delete Selected' button appears, and the status bar shows the total size of your selection."

**Visual:** Click Delete Selected with dry-run enabled. Show the dry-run output.

> "With dry-run enabled, clicking Delete Selected shows you what would be deleted. Review the list in the status area."

**Visual:** Uncheck dry-run. Click Delete Selected. Show the confirmation dialog.

> "When you're ready, uncheck dry-run and delete again. A confirmation dialog ensures you don't delete by accident. This operation is permanent -- there is no undo."

---

## Scene 7: Statistics Dialog (5:15 - 5:45)

**Visual:** Open the statistics dialog. Show each tab.

**Voiceover:**
> "The statistics dialog gives you a comprehensive view of your repository. The Overview tab shows total components, total size, and average size. The Size Distribution tab breaks components into five size buckets. File Type Breakdown shows storage by extension. Age Distribution groups components by when they were created. And the Largest Components tab lists the top 20 by size."

---

## Scene 8: Tips (5:45 - 6:00)

**Visual:** Bullet points overlaid on the GUI.

**Voiceover:**
> "A few tips: Press Enter in the regex field to trigger a list without clicking the button. Sort columns by clicking their headers. And remember, dry-run is your friend -- always preview before deleting."

**Visual:** Fade to outro card.

---

## Production Notes

- Record at maximum window size for readability
- Highlight mouse clicks with a visual indicator
- Use realistic test data with multiple component types
- Ensure the Look and Feel matches the OS (System L&F)

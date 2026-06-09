# Video 4: Safe Component Deletion with Dry-Run Mode

**Length:** 3:00  
**Audience:** Anyone performing delete operations  
**Goal:** Emphasize the importance and usage of dry-run mode

---

## Scene 1: Why Dry-Run Matters (0:00 - 0:30)

**Visual:** Intro card, then a terminal window.

**Voiceover:**
> "Deleting components from Nexus is permanent. There is no recycle bin, no undo, no recovery. A mistyped regex could wipe out an entire repository. That's why JNexus includes dry-run mode -- and you should always use it first."

---

## Scene 2: CLI Dry-Run Demo (0:30 - 1:30)

**Visual:** Terminal with commands.

```bash
# Step 1: Always start with dry-run
./nexus-java.sh delete --dry-run maven-releases ".*-1\.0-SNAPSHOT.*"
```

**Voiceover:**
> "On the command line, add the --dry-run flag -- or its short form, dash n -- to any delete command. JNexus will evaluate the regex, fetch matching components, and list them out, but it won't delete anything."

**Visual:** Show the dry-run output listing matched components.

```
DRY RUN - Would delete: abc123 (com/example/app-1.0-SNAPSHOT/app-1.0-SNAPSHOT.jar)
DRY RUN - Would delete: def456 (com/example/lib-1.0-SNAPSHOT/lib-1.0-SNAPSHOT.jar)
DRY RUN - Would delete: ghi789 (com/example/util-1.0-SNAPSHOT/util-1.0-SNAPSHOT.jar)
DRY RUN complete - 3 components would be deleted
```

**Voiceover:**
> "Review each line carefully. Verify that only the intended components are listed. Check for anything unexpected."

```bash
# Step 2: When satisfied, remove --dry-run
./nexus-java.sh delete maven-releases ".*-1\.0-SNAPSHOT.*"

# Step 3: Confirm when prompted
# Delete 3 components? (yes/no): yes
```

**Voiceover:**
> "When you're confident in the results, remove the dry-run flag and run the delete for real. JNexus will ask for confirmation before proceeding."

---

## Scene 3: GUI Dry-Run Demo (1:30 - 2:30)

**Visual:** Swing GUI with the dry-run checkbox visible.

**Voiceover:**
> "In the Swing GUI, dry-run mode is controlled by a checkbox -- and it's checked by default. This means every deletion starts as a preview."

**Visual:** Enter a regex pattern. Click Delete All with dry-run checked. Show the preview output.

> "Enter your filter pattern and click Delete All. With dry-run enabled, you'll see a list of what would be deleted in the results area."

**Visual:** Uncheck the dry-run checkbox. Click Delete All again. Show the confirmation dialog.

> "Uncheck dry-run when you're ready for the real operation. The confirmation dialog provides one final safety check."

**Visual:** Show selecting specific rows and using Delete Selected with dry-run.

> "The same workflow applies to Delete Selected. Select your target rows, preview with dry-run, then execute."

---

## Scene 4: Reviewing Before Actual Delete (2:30 - 3:00)

**Visual:** Bullet points on screen with examples.

**Voiceover:**
> "Here's a quick checklist for safe deletions. First, always run with dry-run enabled and review the output. Second, check the component count -- does it match your expectations? Third, verify the paths -- are you targeting the right versions and artifacts? Fourth, double-check your regex -- common mistakes include forgetting to escape dots and using greedy matches that capture more than intended."

> "Remember: deletions are permanent. A minute of review can save hours of recovery."

**Visual:** Fade to outro card.

---

## Production Notes

- Show realistic before/after scenarios
- Emphasize the visual difference between dry-run output and actual deletion
- Include a brief example of a regex mistake and how dry-run catches it

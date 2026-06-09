# Video 1: JNexus in 90 Seconds

**Length:** 1:30  
**Audience:** First-time visitors  
**Goal:** Convey the core value proposition quickly

---

## Scene 1: Hook (0:00 - 0:15)

**Visual:** Nexus Repository Manager web UI showing thousands of artifacts in a long list. Scroll through slowly to emphasize volume.

**Voiceover:**
> "Managing thousands of artifacts in Nexus? Finding what you need, filtering through the noise, and cleaning up old releases can be painful and time-consuming."

---

## Scene 2: Solution (0:15 - 0:30)

**Visual:** JNexus logo fades in. Then a quick montage: CLI terminal, Swing GUI, Android app, iOS app -- one second each.

**Voiceover:**
> "Meet JNexus -- a powerful, cross-platform tool for Nexus repository management. Use the CLI for automation, the desktop GUI for interactive work, or the mobile apps for on-the-go management."

---

## Scene 3: CLI Demo (0:30 - 0:50)

**Visual:** Terminal showing the following commands being typed and executed:

```bash
# List components
./nexus-java.sh list maven-releases

# Filter with regex
./nexus-java.sh list maven-releases ".*SNAPSHOT.*"

# Preview deletion (dry-run)
./nexus-java.sh delete --dry-run maven-releases ".*-1.0-SNAPSHOT.*"
```

**Voiceover:**
> "List components in any repository. Apply regex filters to find exactly what you need. Preview deletions with dry-run mode before committing -- so you never delete something by accident."

---

## Scene 4: GUI Demo (0:50 - 1:10)

**Visual:** Swing GUI. Click the List button, apply a filter, select some rows, click Delete Selected with dry-run enabled.

**Voiceover:**
> "Prefer a graphical interface? The Swing GUI gives you sortable tables, multi-select, advanced filters, and repository statistics -- all with a familiar desktop experience."

---

## Scene 5: Mobile Demo (1:10 - 1:20)

**Visual:** Quick clips of the Android app listing components and the iOS app showing the search screen.

**Voiceover:**
> "And with native Android and iOS apps, you can manage your repositories from anywhere. Credentials are encrypted on-device for security."

---

## Scene 6: Call to Action (1:20 - 1:30)

**Visual:** GitHub repository page with the star button highlighted. URL displayed prominently: `github.com/FlossWare/nexus-java`

**Voiceover:**
> "Get started today at github.com/FlossWare/nexus-java. Star the project, download the latest release, and join the community."

**Visual:** Fade to outro card.

---

## Production Notes

- Keep the pacing fast but not rushed -- this is an elevator pitch
- Use screen recordings, not animated mockups
- Background music: Upbeat, low volume, royalty-free
- Ensure all demo data uses example.com domains and fake credentials

# Video 7: JNexus on Android

**Length:** 4:00  
**Audience:** Mobile users, Android developers  
**Goal:** Complete tour of the Android app

---

## Scene 1: Introduction (0:00 - 0:15)

**Visual:** Intro card, then an Android device showing the JNexus app icon.

**Voiceover:**
> "JNexus is available as a native Android app, giving you full Nexus repository management from your phone or tablet. Let's walk through installation, setup, and all the features."

---

## Scene 2: Installation (0:15 - 0:45)

**Visual:** Download page on GitHub, then installing the APK.

**Voiceover:**
> "Download the APK from the GitHub releases page. You'll need Android 8.0 or higher. If you haven't already, enable 'Install from unknown sources' in your device settings."

**Visual:** Show the APK installing and the app appearing on the home screen.

> "Tap the APK to install, then open the app from your home screen or app drawer."

---

## Scene 3: Settings and Credentials (0:45 - 1:30)

**Visual:** Android app Settings screen.

**Voiceover:**
> "On first launch, navigate to the Settings screen using the bottom navigation bar. Enter your Nexus server URL, username, and password."

**Visual:** Show filling in each field, then tapping Save.

> "Optionally, add a comma-separated list of repository names. This populates the repository dropdown on other screens. Set default values for repository, regex, and dry-run if you want them pre-filled."

**Visual:** Show the HTTP timeout field.

> "You can also configure the HTTP timeout for slow connections. The default is 30 seconds."

**Visual:** Show the "Saved" confirmation.

> "Tap Save. Your credentials are encrypted with AES-256-GCM using Android's EncryptedSharedPreferences. They never leave your device in plaintext."

---

## Scene 4: List Screen (1:30 - 2:15)

**Visual:** List screen with repository dropdown and component cards.

**Voiceover:**
> "The List screen is your main workspace. Select a repository from the dropdown at the top. Tap 'List' for cached results or 'Refresh' to bypass the cache."

**Visual:** Show components loading and appearing as cards.

> "Components appear as cards showing the path, file size, and creation date. Tap a card to view full metadata including content type, format, checksum, and last modified date."

**Visual:** Show tapping a component card and seeing the detail view.

> "From the detail view, you can delete the component. A confirmation dialog ensures you don't delete by accident."

---

## Scene 5: Search Screen (2:15 - 3:00)

**Visual:** Search screen with the filter panel.

**Voiceover:**
> "The Search screen provides advanced filtering. Expand the filter panel to see all available options."

**Visual:** Show each filter field.

> "You can filter by size range -- enter minimum and maximum bytes. Filter by date range using ISO 8601 format. Filter by file extension, component name pattern, or path regex."

**Visual:** Show applying filters and seeing results.

> "Tap Search to apply your filters. Results are displayed the same way as the List screen, with full component cards and detail views."

---

## Scene 6: Stats Screen (3:00 - 3:30)

**Visual:** Stats screen showing repository analytics.

**Voiceover:**
> "The Stats screen provides the same comprehensive analytics as the desktop version. Select a repository and tap 'Calculate' to see total components, total size, average and median size, size distribution, file type breakdown, and age distribution."

**Visual:** Scroll through the stats output.

> "Scroll down to see the largest components list. This is particularly useful for quick storage assessments when you're away from your desk."

---

## Scene 7: Security (3:30 - 4:00)

**Visual:** Settings screen highlighting encryption, then a summary slide.

**Voiceover:**
> "A note on security: all credentials are encrypted on-device using AES-256-GCM through Android's EncryptedSharedPreferences. Communication with your Nexus server uses HTTPS. No data is sent to third parties."

> "For the best security, use a Nexus user token instead of your password, and keep your device's lock screen enabled."

**Visual:** Fade to outro card.

---

## Production Notes

- Record on a physical Android device or high-quality emulator
- Use Android Studio's screen recording or scrcpy for captures
- Ensure the device is in a clean state (no notifications during recording)
- Use realistic but fake test data

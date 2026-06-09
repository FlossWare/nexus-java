# Video Production Guide

This guide covers the tools, settings, branding, and workflow for producing JNexus tutorial videos.

## Tools

| Purpose | Tool | License | Notes |
|---------|------|---------|-------|
| Screen Recording | [OBS Studio](https://obsproject.com/) | Free, open-source | Cross-platform, supports scenes |
| Video Editing | [DaVinci Resolve](https://www.blackmagicdesign.com/products/davinciresolve/) | Free tier available | Professional-grade editor |
| Alternative Editor | [Kdenlive](https://kdenlive.org/) | Free, open-source | Lighter weight, Linux-native |
| Voiceover/Audio | [Audacity](https://www.audacityteam.org/) | Free, open-source | Noise reduction, normalization |
| Thumbnails | [GIMP](https://www.gimp.org/) or [Inkscape](https://inkscape.org/) | Free, open-source | Raster and vector graphics |
| Annotations | Built-in OBS text sources | Free | Overlays during recording |

## Recording Settings

### OBS Studio Configuration

**Video:**
- Resolution: 1920x1080 (1080p)
- Frame Rate: 30 fps
- Output Format: MKV (remux to MP4 after recording)
- Encoder: x264 or hardware (NVENC/QSV)
- Rate Control: CRF 18-20 (high quality)

**Audio:**
- Sample Rate: 48 kHz
- Channels: Mono (voiceover) or Stereo
- Bitrate: 128 kbps AAC (final output)

**Scene Setup:**
- Scene 1: Full screen capture (for demos)
- Scene 2: Webcam overlay (optional, for intro/outro)
- Scene 3: Title card (for transitions)

### Terminal Recording Settings

When recording CLI demos:
- Font size: 16-18pt (readable at 1080p)
- Terminal width: 120 columns
- Terminal height: 35 rows
- Color scheme: Dark background with high contrast text
- Prompt: Keep simple (e.g., `$ ` prefix)
- Typing speed: Moderate, with pauses after commands

### GUI Recording Settings

When recording Swing/AWT/mobile demos:
- Window size: Maximize or near-full screen
- System font size: Default or slightly larger
- Mouse cursor: Highlight or enlarge for visibility
- Click effects: Enable visual click indicator in OBS

## Branding

### Intro Card (3 seconds)

```
+------------------------------------------+
|                                          |
|           J N e x u s                    |
|                                          |
|    Nexus Repository Management Tool      |
|                                          |
|    github.com/FlossWare/nexus-java       |
|                                          |
+------------------------------------------+
```

- Background: Dark (#1a1a2e)
- Text: White (#ffffff)
- Accent: Blue (#0066cc)
- Font: Sans-serif (e.g., Inter, Roboto, or system default)
- Duration: 3 seconds with fade-in

### Outro Card (5 seconds)

```
+------------------------------------------+
|                                          |
|    Star the project on GitHub            |
|    github.com/FlossWare/nexus-java       |
|                                          |
|    Subscribe for more tutorials          |
|                                          |
|    Questions? Open a GitHub Issue         |
|                                          |
+------------------------------------------+
```

- Same styling as intro
- Duration: 5 seconds with fade-out
- Include clickable end-screen elements (YouTube)

### Lower Third (persistent during demo)

```
+-------------------------------+
| JNexus - [Video Title]        |
| github.com/FlossWare/nexus-java |
+-------------------------------+
```

- Position: Bottom-left
- Opacity: 80%
- Background: Semi-transparent dark
- Text: White, small font
- Show for first 10 seconds of each scene, then fade

### Color Palette

| Purpose | Color | Hex |
|---------|-------|-----|
| Primary Background | Dark Navy | #1a1a2e |
| Primary Text | White | #ffffff |
| Accent/Links | Blue | #0066cc |
| Success/Highlight | Green | #28a745 |
| Warning | Amber | #ffc107 |
| Error/Danger | Red | #dc3545 |
| Code Background | Dark Gray | #2d2d2d |

## Workflow

### Per-Video Production Steps

1. **Script Review** (15-30 min)
   - Read through the script in `scripts/`
   - Rehearse voiceover timing
   - Prepare demo environment (Nexus instance, test data)

2. **Environment Setup** (15-30 min)
   - Start OBS with correct scene configuration
   - Set terminal/application to recording settings
   - Test audio levels (voiceover should peak at -6 dB)
   - Populate test repositories with sample data

3. **Recording** (1-2 hours)
   - Record in segments (one scene per take)
   - Leave 2-second pauses between scenes for editing
   - Re-record any mistakes immediately
   - Capture both screen and voiceover simultaneously

4. **Editing** (1-3 hours)
   - Import clips into DaVinci Resolve / Kdenlive
   - Trim dead space and mistakes
   - Add intro/outro cards
   - Add lower-third overlay
   - Add chapter markers
   - Normalize audio levels
   - Add background music (optional, low volume)
   - Export as MP4 (H.264, AAC)

5. **Review** (30 min)
   - Watch full video at 1x speed
   - Check for audio sync issues
   - Verify all text is readable
   - Check timing against script
   - Get second opinion if possible

6. **Publishing** (30 min)
   - Upload to YouTube
   - Set title, description (from script template)
   - Add tags: JNexus, Nexus Repository, DevOps, Maven, etc.
   - Add timestamps in description
   - Enable auto-generated captions, then review
   - Set thumbnail
   - Add to "JNexus Tutorials" playlist

### YouTube Description Template

```
[Video Title] | JNexus Tutorial

[1-2 sentence summary of what this video covers]

Timestamps:
0:00 Introduction
[Add chapter timestamps]

Links:
- GitHub: https://github.com/FlossWare/nexus-java
- Documentation: https://github.com/FlossWare/nexus-java/blob/main/README.md
- Issues: https://github.com/FlossWare/nexus-java/issues
- Download: https://github.com/FlossWare/nexus-java/releases/latest

Tags: #JNexus #NexusRepository #DevOps #RepositoryManagement
```

### YouTube Tags

Standard tags for all videos:
- JNexus
- Nexus Repository Manager
- Sonatype Nexus
- DevOps
- Repository Management
- Maven Repository
- Artifact Management
- CLI Tools
- Java
- Open Source

## Accessibility

### Closed Captions

- Enable YouTube auto-generated captions for all videos
- Review and correct auto-generated text within 48 hours of upload
- Pay special attention to technical terms (Nexus, regex, Picocli, etc.)
- Include speaker identification if multiple voices

### Audio Descriptions

- Describe visual elements verbally during recording
- Example: "I'm clicking the List button in the toolbar" (not just clicking)
- Narrate screen changes: "The table now shows 15 components"

### Transcripts

- Post full transcript in video description (YouTube character limit: 5000)
- For longer transcripts, link to the script file in the repository

## Quality Checklist

Before publishing each video, verify:

- [ ] Resolution is 1080p (1920x1080)
- [ ] Audio is clear, no background noise
- [ ] All text on screen is readable
- [ ] Demo data does not contain real credentials or sensitive information
- [ ] Intro and outro cards are present
- [ ] Chapter markers are accurate
- [ ] Closed captions are reviewed and corrected
- [ ] Description includes timestamps and links
- [ ] Thumbnail is set
- [ ] Video is added to the playlist
- [ ] Duration matches the target length (within 30 seconds)

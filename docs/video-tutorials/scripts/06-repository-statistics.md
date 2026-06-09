# Video 6: Repository Statistics and Analytics

**Length:** 5:00  
**Audience:** Repository managers, analysts  
**Goal:** Deep dive into the statistics feature

---

## Scene 1: Introduction (0:00 - 0:15)

**Visual:** Intro card, then the CLI or GUI statistics output.

**Voiceover:**
> "Understanding what's in your Nexus repository is the first step to managing it effectively. JNexus provides comprehensive statistics that help you analyze storage usage, identify bloat, and plan cleanup strategies."

---

## Scene 2: Overview Metrics (0:15 - 1:00)

**Visual:** CLI stats command output, showing the overview section.

```bash
./nexus-java.sh stats maven-releases
```

**Visual:** Highlight each metric as it's discussed.

```
Repository Statistics: maven-releases
=====================================
Total Components:    1,247
Total Size:          3.8 GB
Average Size:        3.1 MB
Median Size:         1.2 MB
```

**Voiceover:**
> "The overview shows four key metrics. Total Components is the raw count of artifacts in the repository. Total Size is the aggregate storage consumption. Average Size gives you a general sense of component size. And Median Size is often more informative than average -- it tells you what a 'typical' component looks like, unaffected by outliers."

> "Notice the difference between average and median here. The average is 3.1 megabytes, but the median is only 1.2 megabytes. This tells us that a few very large components are pulling the average up."

---

## Scene 3: Size Distribution (1:00 - 2:00)

**Visual:** Size distribution output or statistics dialog tab.

```
Size Distribution:
  < 1 MB:         843 components (67.6%)
  1 MB - 10 MB:   312 components (25.0%)
  10 MB - 100 MB:   78 components (6.3%)
  100 MB - 1 GB:    12 components (1.0%)
  > 1 GB:            2 components (0.2%)
```

**Voiceover:**
> "The size distribution breaks components into five buckets. In this example, two thirds of components are under one megabyte -- typical for POM files and small JARs. But those 14 components over 100 megabytes might be worth investigating."

> "This is where you start asking questions. Are those large components necessary? Are they fat JARs that could be replaced with thinner artifacts? Are old versions still needed?"

---

## Scene 4: File Type Breakdown (2:00 - 3:00)

**Visual:** File type breakdown output.

```
File Type Breakdown:
  .jar:   2.1 GB  (55.3%)
  .war:   1.2 GB  (31.6%)
  .pom:   42 MB   (1.1%)
  .xml:   15 MB   (0.4%)
  .zip:   380 MB  (10.0%)
  Other:  63 MB   (1.7%)
```

**Voiceover:**
> "The file type breakdown shows how storage is distributed across different artifact types. JAR files typically dominate in Maven repositories, but WAR files can be significant contributors too."

> "This view helps you identify which artifact types consume the most space. If ZIP files are taking up 10 percent of your storage, it might be worth investigating what those archives contain and whether older versions can be cleaned up."

---

## Scene 5: Age Distribution (3:00 - 3:45)

**Visual:** Age distribution output.

```
Age Distribution:
  Last 7 days:    45 components
  Last 30 days:  187 components
  Last 90 days:  534 components
  Older:         713 components
```

**Voiceover:**
> "Age distribution groups components by when they were created. This is essential for retention policy planning."

> "In this example, over 700 components are more than 90 days old. If your retention policy says snapshots should be cleaned up after 90 days, those are your cleanup targets. Combine this with the date range filter from the search feature to take action."

---

## Scene 6: Largest Components (3:45 - 4:30)

**Visual:** Largest components list.

```
Largest Components (Top 20):
  1. 1.8 GB  com/example/big-app-2.0.war
  2. 1.2 GB  com/example/big-app-1.9.war
  3. 850 MB  com/example/data-bundle-3.1.zip
  ...
```

**Voiceover:**
> "The largest components list shows the top 20 components by size. This is your quick-win list for storage recovery."

> "Notice here that two versions of the same application account for 3 gigabytes. If only the latest version is needed, deleting the older one immediately frees significant space."

---

## Scene 7: Using Statistics for Cleanup (4:30 - 5:00)

**Visual:** Workflow diagram or bullet points.

**Voiceover:**
> "Here's a practical workflow. First, run stats to understand your repository. Second, identify cleanup targets -- large components, old artifacts, unnecessary file types. Third, use the search and filter features to build your deletion list. Fourth, preview with dry-run. And finally, execute the cleanup."

> "Run stats again after cleanup to measure the impact. Over time, you'll develop a data-driven approach to repository management."

**Visual:** Fade to outro card.

---

## Production Notes

- Use a repository with realistic, diverse data for meaningful statistics
- Consider showing the Swing GUI statistics dialog with its tabbed interface
- Emphasize the analytical mindset -- statistics inform decisions

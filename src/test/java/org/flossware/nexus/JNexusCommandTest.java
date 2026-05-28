package org.flossware.nexus;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for JNexus CLI command parsing and validation.
 * <p>
 * Tests CLI exit codes and command parsing without requiring a real Nexus server.
 * Focuses on:
 * </p>
 * <ul>
 *   <li>Command parsing and validation</li>
 *   <li>Error codes for invalid inputs</li>
 *   <li>Date parsing error detection</li>
 * </ul>
 *
 * <p>
 * Note: Error message content is not tested here as it goes to SLF4J logger.
 * These tests focus on exit codes which indicate success/failure.
 * </p>
 */
class JNexusCommandTest {

    @TempDir
    Path tempDir;

    /**
     * Creates a mock nexus.properties file for testing.
     * Uses a fake HTTP URL so tests don't make real network calls.
     */
    private void createMockProperties() throws IOException {
        Path configDir = tempDir.resolve(".flossware/nexus");
        Files.createDirectories(configDir);

        Properties props = new Properties();
        props.setProperty("nexus.url", "http://fake-nexus.example.com");
        props.setProperty("nexus.user", "testuser");
        props.setProperty("nexus.password", "testpass");

        Path propsFile = configDir.resolve("nexus.properties");
        try (var writer = Files.newBufferedWriter(propsFile)) {
            props.store(writer, "Test properties");
        }

        // Set home directory to temp dir so Credentials finds our mock file
        System.setProperty("user.home", tempDir.toString());
    }

    @Test
    void testHelpFlag_showsUsage() {
        String[] args = {"--help"};
        int exitCode = new CommandLine(new JNexus()).execute(args);
        assertEquals(0, exitCode, "Help flag should exit 0");
    }

    @Test
    void testVersionFlag_showsVersion() {
        String[] args = {"--version"};
        int exitCode = new CommandLine(new JNexus()).execute(args);
        assertEquals(0, exitCode, "Version flag should exit 0");
    }

    @Test
    void testListCommand_withInvalidDateFormat_returnsError() throws IOException {
        createMockProperties();

        String[] args = {"list", "test-repo", "--created-after", "invalid-date"};
        int exitCode = new CommandLine(new JNexus()).execute(args);

        assertEquals(1, exitCode, "Invalid date should return exit code 1 (error)");
    }

    @Test
    void testListCommand_withPartialISODate_returnsError() throws IOException {
        createMockProperties();

        // ISO date without time part - should fail with DateTimeParseException
        String[] args = {"list", "test-repo", "--created-after", "2024-01-01"};
        int exitCode = new CommandLine(new JNexus()).execute(args);

        assertEquals(1, exitCode, "Partial ISO date should return exit code 1");
    }

    @Test
    void testListCommand_withInvalidCreatedBefore_returnsError() throws IOException {
        createMockProperties();

        String[] args = {"list", "test-repo", "--created-before", "not-a-date"};
        int exitCode = new CommandLine(new JNexus()).execute(args);

        assertEquals(1, exitCode, "Invalid --created-before should return exit code 1");
    }

    @Test
    void testListCommand_withBothInvalidDates_returnsError() throws IOException {
        createMockProperties();

        String[] args = {"list", "test-repo",
            "--created-after", "bad1",
            "--created-before", "bad2"};
        int exitCode = new CommandLine(new JNexus()).execute(args);

        assertEquals(1, exitCode, "Invalid dates should return exit code 1");
    }

    @Test
    void testListCommand_missingRepository_showsError() {
        String[] args = {"list"};
        int exitCode = new CommandLine(new JNexus()).execute(args);

        assertEquals(2, exitCode, "Missing required parameter should return Picocli's error code 2");
    }

    @Test
    void testDeleteCommand_missingRepository_showsError() {
        String[] args = {"delete"};
        int exitCode = new CommandLine(new JNexus()).execute(args);

        assertEquals(2, exitCode, "Missing required parameter should return Picocli's error code 2");
    }

    @Test
    void testStatsCommand_missingRepository_showsError() {
        String[] args = {"stats"};
        int exitCode = new CommandLine(new JNexus()).execute(args);

        assertEquals(2, exitCode, "Missing required parameter should return Picocli's error code 2");
    }

    @Test
    void testVerboseFlag_isRecognized() {
        String[] args = {"-v", "list", "test-repo"};
        CommandLine cmd = new CommandLine(new JNexus());
        int exitCode = cmd.execute(args);

        // Exit code will be 1 (credential error) not 2 (parsing error)
        assertNotEquals(2, exitCode,
            "Verbose flag should be recognized (no parsing error)");
    }

    @Test
    void testQuietFlag_isRecognized() {
        String[] args = {"-q", "list", "test-repo"};
        CommandLine cmd = new CommandLine(new JNexus());
        int exitCode = cmd.execute(args);

        assertNotEquals(2, exitCode,
            "Quiet flag should be recognized (no parsing error)");
    }

    @Test
    void testProfileFlag_isRecognized() {
        String[] args = {"-p", "prod", "list", "test-repo"};
        CommandLine cmd = new CommandLine(new JNexus());
        int exitCode = cmd.execute(args);

        assertNotEquals(2, exitCode,
            "Profile flag should be recognized (no parsing error)");
    }

    @Test
    void testListCommand_withMinSize_isRecognized() {
        String[] args = {"list", "test-repo", "--min-size", "1000"};
        CommandLine cmd = new CommandLine(new JNexus());
        int exitCode = cmd.execute(args);

        assertNotEquals(2, exitCode, "Min size option should be recognized");
    }

    @Test
    void testListCommand_withMaxSize_isRecognized() {
        String[] args = {"list", "test-repo", "--max-size", "100000"};
        CommandLine cmd = new CommandLine(new JNexus());
        int exitCode = cmd.execute(args);

        assertNotEquals(2, exitCode, "Max size option should be recognized");
    }

    @Test
    void testListCommand_withExtension_isRecognized() {
        String[] args = {"list", "test-repo", "--extension", ".jar"};
        CommandLine cmd = new CommandLine(new JNexus());
        int exitCode = cmd.execute(args);

        assertNotEquals(2, exitCode, "Extension option should be recognized");
    }

    @Test
    void testListCommand_withShowMetadata_isRecognized() {
        String[] args = {"list", "test-repo", "--show-metadata"};
        CommandLine cmd = new CommandLine(new JNexus());
        int exitCode = cmd.execute(args);

        assertNotEquals(2, exitCode, "Show metadata flag should be recognized");
    }

    @Test
    void testDeleteCommand_withDryRun_isRecognized() {
        String[] args = {"delete", "test-repo", "--dry-run"};
        CommandLine cmd = new CommandLine(new JNexus());
        int exitCode = cmd.execute(args);

        assertNotEquals(2, exitCode, "Dry run flag should be recognized");
    }

    @Test
    void testDeleteCommand_withYesFlag_isRecognized() {
        String[] args = {"delete", "test-repo", "--yes"};
        CommandLine cmd = new CommandLine(new JNexus());
        int exitCode = cmd.execute(args);

        assertNotEquals(2, exitCode, "Yes flag should be recognized");
    }

    @Test
    void testStatsCommand_withTextFormat_isRecognized() {
        String[] args = {"stats", "test-repo", "--format", "text"};
        CommandLine cmd = new CommandLine(new JNexus());
        int exitCode = cmd.execute(args);

        assertNotEquals(2, exitCode, "Text format should be recognized");
    }

    @Test
    void testStatsCommand_withJsonFormat_isRecognized() {
        String[] args = {"stats", "test-repo", "--format", "json"};
        CommandLine cmd = new CommandLine(new JNexus());
        int exitCode = cmd.execute(args);

        assertNotEquals(2, exitCode, "JSON format should be recognized");
    }
}

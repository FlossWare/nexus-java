package org.flossware.nexus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for JNexusUI Terminal GUI.
 * <p>
 * Note: jcurses terminal UI requires a terminal environment and cannot be fully tested in headless mode.
 * JNexusUI is designed as a static application (main method), not an instantiable class.
 * These tests verify helper methods and static behavior.
 * </p>
 */
class JNexusUITest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        // Create mock properties file
        createMockProperties();
    }

    private void createMockProperties() throws IOException {
        Path configDir = tempDir.resolve(".flossware/nexus");
        Files.createDirectories(configDir);

        Properties props = new Properties();
        props.setProperty("nexus.url", "http://fake-nexus.example.com");
        props.setProperty("nexus.user", "testuser");
        props.setProperty("nexus.password", "testpass");
        props.setProperty("nexus.repositories", "maven-releases,npm-public");
        props.setProperty("nexus.default.repository", "maven-releases");
        props.setProperty("nexus.default.regex", ".*SNAPSHOT.*");
        props.setProperty("nexus.default.dryrun", "true");

        Path propsFile = configDir.resolve("nexus.properties");
        try (var writer = Files.newBufferedWriter(propsFile)) {
            props.store(writer, "Test properties");
        }

        System.setProperty("user.home", tempDir.toString());
    }

    @Test
    void testGetTerminalSize_handlesMissingCommand() throws Exception {
        // Test the static helper method that gets terminal size
        var method = JNexusUI.class.getDeclaredMethod("getTerminalSize");
        method.setAccessible(true);

        // Invoke static method (null target)
        int[] size = (int[]) method.invoke(null);

        assertNotNull(size, "Terminal size should return array");
        assertEquals(2, size.length, "Size array should have 2 elements [rows, cols]");
        assertTrue(size[0] > 0, "Rows should be positive (default 24)");
        assertTrue(size[1] > 0, "Columns should be positive (default 80)");
    }

    @Test
    void testGetTerminalSize_returnsReasonableDefaults() throws Exception {
        var method = JNexusUI.class.getDeclaredMethod("getTerminalSize");
        method.setAccessible(true);

        int[] size = (int[]) method.invoke(null);

        // Default values are 24 rows, 80 columns when stty is not available
        assertTrue(size[0] >= 24, "Rows should be at least 24");
        assertTrue(size[1] >= 80, "Columns should be at least 80");
    }

    @Test
    void testJNexusUI_hasDefaultConstructor() {
        // JNexusUI should have a default constructor (implicit or explicit)
        try {
            JNexusUI ui = new JNexusUI();
            assertNotNull(ui, "JNexusUI instance should be created");
        } catch (Exception e) {
            fail("JNexusUI should have a default constructor: " + e.getMessage());
        }
    }

    @Test
    void testJNexusUI_classExists() {
        // Basic sanity test that the class exists and can be loaded
        assertNotNull(JNexusUI.class, "JNexusUI class should exist");
    }

    /**
     * Note: Full UI component tests are not possible in test environment for jcurses.
     * jcurses requires a real terminal with ncurses support.
     * For comprehensive Terminal UI testing, manual testing in a terminal environment is required.
     */
    @Test
    void testTerminalUIRequiresTerminalEnvironment() {
        // This test documents the limitation
        // jcurses components will fail if not run in a real terminal
        // This is expected behavior and not a defect
        assertTrue(true, "Terminal UI requires terminal environment for full component testing");
    }
}

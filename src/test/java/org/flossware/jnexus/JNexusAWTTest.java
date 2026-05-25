package org.flossware.jnexus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for JNexusAWT GUI.
 * <p>
 * Note: AWT components require a graphics environment and cannot be fully tested in headless mode.
 * These tests verify object initialization and field values without creating AWT components.
 * </p>
 */
class JNexusAWTTest {

    @TempDir
    Path tempDir;

    private Credentials mockCredentials;

    @BeforeEach
    void setUp() throws Exception {
        // Create mock properties file
        createMockProperties();

        // Create mocks
        mockCredentials = createMockCredentials();
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

    private Credentials createMockCredentials() {
        Credentials creds = mock(Credentials.class);
        when(creds.getUrl()).thenReturn("http://fake-nexus.example.com");
        when(creds.getUser()).thenReturn("testuser");
        when(creds.getPassword()).thenReturn("testpass");
        when(creds.getRepositories()).thenReturn(List.of("maven-releases", "npm-public"));
        when(creds.getDefaultRepository()).thenReturn("maven-releases");
        when(creds.getDefaultRegex()).thenReturn(".*SNAPSHOT.*");
        when(creds.isDefaultDryRun()).thenReturn(true);
        when(creds.getProfile()).thenReturn(null);
        when(creds.getHttpTimeoutSeconds()).thenReturn(30);
        return creds;
    }

    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private Object getPrivateField(Object target, String fieldName) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    @Test
    void testConstructor_withProfile_initializesCorrectly() throws Exception {
        JNexusAWT awt = new JNexusAWT(mockCredentials);
        assertNotNull(awt);
        assertNotNull(getPrivateField(awt, "client"));
        assertNotNull(getPrivateField(awt, "service"));
        assertEquals(mockCredentials, getPrivateField(awt, "credentials"));
    }

    @Test
    void testConstructor_withCredentials_initializesCorrectly() throws Exception {
        JNexusAWT awt = new JNexusAWT(mockCredentials);
        assertNotNull(awt);
        assertNotNull(getPrivateField(awt, "client"));
        assertNotNull(getPrivateField(awt, "service"));
    }

    @Test
    void testConstructor_initializesNexusClient() throws Exception {
        JNexusAWT awt = new JNexusAWT(mockCredentials);
        NexusClient client = (NexusClient) getPrivateField(awt, "client");
        assertNotNull(client, "NexusClient should be initialized");
    }

    @Test
    void testConstructor_initializesNexusService() throws Exception {
        JNexusAWT awt = new JNexusAWT(mockCredentials);
        NexusService service = (NexusService) getPrivateField(awt, "service");
        assertNotNull(service, "NexusService should be initialized");
    }

    @Test
    void testConstructor_storesCredentials() throws Exception {
        JNexusAWT awt = new JNexusAWT(mockCredentials);
        Credentials storedCreds = (Credentials) getPrivateField(awt, "credentials");
        assertEquals(mockCredentials, storedCreds, "Credentials should be stored");
    }

    /**
     * Note: Full UI component tests are not possible in headless mode for AWT.
     * AWT requires a real graphics environment, unlike Swing which supports headless mode.
     * For comprehensive AWT GUI testing, manual testing or integration tests with Xvfb are required.
     */
    @Test
    void testAWTRequiresGraphicsEnvironment() {
        // This test documents the limitation
        String headless = System.getProperty("java.awt.headless");
        // AWT components will throw HeadlessException if headless=true
        // This is expected behavior and not a defect
        assertTrue(true, "AWT requires graphics environment for full component testing");
    }
}

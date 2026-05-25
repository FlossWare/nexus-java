package org.flossware.jnexus;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for JNexusSwing GUI.
 * <p>
 * Tests Swing UI component creation, event handling, and accessibility features
 * in headless mode (no display required).
 * </p>
 */
class JNexusSwingTest {

    @TempDir
    Path tempDir;

    private JNexusSwing swing;
    private NexusClient mockClient;
    private NexusService mockService;
    private Credentials mockCredentials;

    @BeforeAll
    static void setUpHeadless() {
        // Enable headless mode for GUI tests
        System.setProperty("java.awt.headless", "true");
    }

    @BeforeEach
    void setUp() throws Exception {
        // Create mock properties file
        createMockProperties();

        // Create mocks
        mockCredentials = createMockCredentials();
        mockClient = mock(NexusClient.class);
        mockService = mock(NexusService.class);

        // Create JNexusSwing instance without showing GUI
        swing = new JNexusSwing(mockCredentials);

        // Replace mocks via reflection (since fields are private)
        setPrivateField(swing, "client", mockClient);
        setPrivateField(swing, "service", mockService);
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
        when(creds.getHttpTimeoutSeconds()).thenReturn(30); // Set valid timeout
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
        assertNotNull(swing);
        assertEquals(mockClient, getPrivateField(swing, "client"));
        assertEquals(mockService, getPrivateField(swing, "service"));
        assertEquals(mockCredentials, getPrivateField(swing, "credentials"));
    }

    @Test
    void testConstructor_withCredentials_initializesCorrectly() throws Exception {
        JNexusSwing swingWithCreds = new JNexusSwing(mockCredentials);
        assertNotNull(swingWithCreds);
        assertNotNull(getPrivateField(swingWithCreds, "client"));
        assertNotNull(getPrivateField(swingWithCreds, "service"));
    }

    @Test
    void testCreateInputPanel_createsComponents() throws Exception {
        // Access private method via reflection
        var method = JNexusSwing.class.getDeclaredMethod("createInputPanel");
        method.setAccessible(true);
        JPanel panel = (JPanel) method.invoke(swing);

        assertNotNull(panel);
        assertTrue(panel.getComponentCount() > 0, "Input panel should have components");
    }

    @Test
    void testCreateButtonPanel_createsAllButtons() throws Exception {
        var method = JNexusSwing.class.getDeclaredMethod("createButtonPanel");
        method.setAccessible(true);
        JPanel panel = (JPanel) method.invoke(swing);

        assertNotNull(panel);

        // Count buttons in panel
        int buttonCount = 0;
        for (Component comp : panel.getComponents()) {
            if (comp instanceof JButton) {
                buttonCount++;
            }
        }

        assertEquals(7, buttonCount, "Should have 7 buttons (List, Refresh, Delete All, Delete Selected, Clear, Stats, Quit)");
    }

    @Test
    void testButtonMnemonics_allSet() throws Exception {
        var method = JNexusSwing.class.getDeclaredMethod("createButtonPanel");
        method.setAccessible(true);
        JPanel panel = (JPanel) method.invoke(swing);

        // Extract buttons and verify mnemonics
        List<JButton> buttons = new ArrayList<>();
        for (Component comp : panel.getComponents()) {
            if (comp instanceof JButton) {
                buttons.add((JButton) comp);
            }
        }

        // All buttons should have mnemonics set
        for (JButton button : buttons) {
            int mnemonic = button.getMnemonic();
            assertTrue(mnemonic > 0, "Button '" + button.getText() + "' should have mnemonic set");
        }
    }

    @Test
    void testListButton_hasMnemonic() throws Exception {
        var method = JNexusSwing.class.getDeclaredMethod("createButtonPanel");
        method.setAccessible(true);
        JPanel panel = (JPanel) method.invoke(swing);

        JButton listButton = findButtonByText(panel, "List");
        assertNotNull(listButton, "List button should exist");
        assertEquals(KeyEvent.VK_L, listButton.getMnemonic(), "List button should have Alt+L mnemonic");
    }

    @Test
    void testRefreshButton_hasMnemonic() throws Exception {
        var method = JNexusSwing.class.getDeclaredMethod("createButtonPanel");
        method.setAccessible(true);
        JPanel panel = (JPanel) method.invoke(swing);

        JButton refreshButton = findButtonByText(panel, "Refresh");
        assertNotNull(refreshButton, "Refresh button should exist");
        assertEquals(KeyEvent.VK_R, refreshButton.getMnemonic(), "Refresh button should have Alt+R mnemonic");
    }

    @Test
    void testDeleteButton_hasMnemonic() throws Exception {
        var method = JNexusSwing.class.getDeclaredMethod("createButtonPanel");
        method.setAccessible(true);
        JPanel panel = (JPanel) method.invoke(swing);

        JButton deleteButton = findButtonByText(panel, "Delete All");
        assertNotNull(deleteButton, "Delete All button should exist");
        assertEquals(KeyEvent.VK_D, deleteButton.getMnemonic(), "Delete All button should have Alt+D mnemonic");
    }

    @Test
    void testQuitButton_hasMnemonic() throws Exception {
        var method = JNexusSwing.class.getDeclaredMethod("createButtonPanel");
        method.setAccessible(true);
        JPanel panel = (JPanel) method.invoke(swing);

        JButton quitButton = findButtonByText(panel, "Quit");
        assertNotNull(quitButton, "Quit button should exist");
        assertEquals(KeyEvent.VK_Q, quitButton.getMnemonic(), "Quit button should have Alt+Q mnemonic");
    }

    @Test
    void testClearButton_hasMnemonic() throws Exception {
        var method = JNexusSwing.class.getDeclaredMethod("createButtonPanel");
        method.setAccessible(true);
        JPanel panel = (JPanel) method.invoke(swing);

        JButton clearButton = findButtonByText(panel, "Clear Results");
        assertNotNull(clearButton, "Clear Results button should exist");
        assertEquals(KeyEvent.VK_C, clearButton.getMnemonic(), "Clear button should have Alt+C mnemonic");
    }

    @Test
    void testStatsButton_hasMnemonic() throws Exception {
        var method = JNexusSwing.class.getDeclaredMethod("createButtonPanel");
        method.setAccessible(true);
        JPanel panel = (JPanel) method.invoke(swing);

        JButton statsButton = findButtonByText(panel, "Statistics");
        assertNotNull(statsButton, "Statistics button should exist");
        assertEquals(KeyEvent.VK_S, statsButton.getMnemonic(), "Statistics button should have Alt+S mnemonic");
    }

    @Test
    void testCreateMenuBar_createsFileMenu() throws Exception {
        var method = JNexusSwing.class.getDeclaredMethod("createMenuBar");
        method.setAccessible(true);
        JMenuBar menuBar = (JMenuBar) method.invoke(swing);

        assertNotNull(menuBar);
        assertTrue(menuBar.getMenuCount() >= 3, "Should have at least 3 menus (File, Actions, Help)");

        JMenu fileMenu = menuBar.getMenu(0);
        assertEquals("File", fileMenu.getText(), "First menu should be File");
        assertEquals(KeyEvent.VK_F, fileMenu.getMnemonic(), "File menu should have Alt+F mnemonic");
    }

    @Test
    void testCreateMenuBar_createsActionsMenu() throws Exception {
        var method = JNexusSwing.class.getDeclaredMethod("createMenuBar");
        method.setAccessible(true);
        JMenuBar menuBar = (JMenuBar) method.invoke(swing);

        JMenu actionsMenu = menuBar.getMenu(1);
        assertEquals("Actions", actionsMenu.getText(), "Second menu should be Actions");
        assertEquals(KeyEvent.VK_A, actionsMenu.getMnemonic(), "Actions menu should have Alt+A mnemonic");
    }

    @Test
    void testCreateMenuBar_createsHelpMenu() throws Exception {
        var method = JNexusSwing.class.getDeclaredMethod("createMenuBar");
        method.setAccessible(true);
        JMenuBar menuBar = (JMenuBar) method.invoke(swing);

        JMenu helpMenu = menuBar.getMenu(2);
        assertEquals("Help", helpMenu.getText(), "Third menu should be Help");
        assertEquals(KeyEvent.VK_H, helpMenu.getMnemonic(), "Help menu should have Alt+H mnemonic");
    }

    @Test
    void testCreateResultsPanel_createsTable() throws Exception {
        var method = JNexusSwing.class.getDeclaredMethod("createResultsPanel");
        method.setAccessible(true);
        JPanel panel = (JPanel) method.invoke(swing);

        assertNotNull(panel);

        // Find JScrollPane containing the table
        JScrollPane scrollPane = findComponentOfType(panel, JScrollPane.class);
        assertNotNull(scrollPane, "Results panel should contain JScrollPane");

        JTable table = (JTable) scrollPane.getViewport().getView();
        assertNotNull(table, "Scroll pane should contain JTable");
        assertEquals(7, table.getColumnCount(), "Table should have 7 columns");
    }

    @Test
    void testCreateStatusPanel_createsStatusLabel() throws Exception {
        var method = JNexusSwing.class.getDeclaredMethod("createStatusPanel");
        method.setAccessible(true);
        JPanel panel = (JPanel) method.invoke(swing);

        assertNotNull(panel);

        JLabel statusLabel = findComponentOfType(panel, JLabel.class);
        assertNotNull(statusLabel, "Status panel should contain JLabel");
        assertTrue(statusLabel.getText().contains("Ready"), "Status label should show 'Ready' message");
    }

    @Test
    void testCreateAdvancedFiltersPanel_createsFilterFields() throws Exception {
        var method = JNexusSwing.class.getDeclaredMethod("createAdvancedFiltersPanel");
        method.setAccessible(true);
        JPanel panel = (JPanel) method.invoke(swing);

        assertNotNull(panel);

        // Count text fields for filters
        int textFieldCount = countComponentsOfType(panel, JTextField.class);
        assertEquals(5, textFieldCount, "Advanced filters should have 5 text fields (min/max size, created after/before, extension)");
    }

    // Helper methods

    private JButton findButtonByText(Container container, String text) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof JButton) {
                JButton button = (JButton) comp;
                if (button.getText().equals(text)) {
                    return button;
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private <T extends Component> T findComponentOfType(Container container, Class<T> type) {
        for (Component comp : container.getComponents()) {
            if (type.isInstance(comp)) {
                return (T) comp;
            }
            if (comp instanceof Container) {
                T found = findComponentOfType((Container) comp, type);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private int countComponentsOfType(Container container, Class<? extends Component> type) {
        int count = 0;
        for (Component comp : container.getComponents()) {
            if (type.isInstance(comp)) {
                count++;
            }
            if (comp instanceof Container) {
                count += countComponentsOfType((Container) comp, type);
            }
        }
        return count;
    }
}

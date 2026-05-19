package org.flossware.jnexus;

import org.flossware.jcurses.api.*;
import org.flossware.jcurses.ffi.NcursesBridge;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Terminal UI for interacting with Nexus Repository Manager.
 * <p>
 * Provides an interactive curses-based interface for listing and deleting
 * components from Nexus repositories with optional regex filtering.
 * </p>
 *
 * @author sfloess
 * @since 1.0
 */
public class JNexusUI {
    private static final List<Component> focusableComponents = new ArrayList<>();
    private static int currentFocus = 0;
    private static boolean running = true;
    private static char[][] buffer;

    // Ncurses key codes
    private static final int KEY_UP = 259;
    private static final int KEY_DOWN = 258;
    private static final int KEY_ENTER = 10;
    private static final int KEY_TAB = 9;
    private static final int KEY_ESC = 27;
    private static final int KEY_SPACE = 32;
    private static final int KEY_Q = 113;

    // UI Components
    private static JTextField repositoryField;
    private static JTextField regexField;
    private static JCheckbox dryRunCheckbox;
    private static JLabel statusLabel;
    private static JPanel resultsPanel;
    private static final List<JLabel> resultLabels = new ArrayList<>();

    // Services
    private static NexusClient client;
    private static NexusService service;
    private static Credentials credentials;

    public static void main(String[] args) throws Throwable {
        if (!NcursesBridge.isAvailable()) {
            System.err.println("ERROR: ncurses library is not available!");
            System.err.println("Please install ncurses development library:");
            System.err.println("  - Ubuntu/Debian: sudo apt-get install libncurses-dev");
            System.err.println("  - Fedora/RHEL: sudo dnf install ncurses-devel");
            System.err.println("  - Arch: sudo pacman -S ncurses");
            System.exit(1);
        }

        // Discover available profiles before initializing ncurses
        List<String> profiles = Credentials.discoverProfiles();
        String selectedProfile = null;

        if (profiles.isEmpty()) {
            // No configuration found - collect credentials via console
            credentials = collectCredentialsFromConsole();
            if (credentials == null) {
                System.err.println("\nCredential collection cancelled. Exiting.");
                System.exit(0);
            }

            // Ask if user wants to save credentials
            System.out.print("Would you like to save these credentials to ~/.flossware/nexus/nexus.properties? (yes/no): ");
            java.util.Scanner scanner = new java.util.Scanner(System.in);
            String saveResponse = scanner.nextLine().trim().toLowerCase();

            if (saveResponse.equals("yes") || saveResponse.equals("y")) {
                try {
                    credentials.saveToPropertiesFile(null);
                    System.out.println("\nCredentials saved successfully to: ~/.flossware/nexus/nexus.properties");
                } catch (java.io.IOException e) {
                    System.err.println("\nERROR: Failed to save credentials: " + e.getMessage());
                }
            } else {
                System.out.println("\nCredentials not saved (will only be used for this session).");
            }
            System.out.println();
        } else if (profiles.size() > 1) {
            // Multiple profiles found - show selection menu
            selectedProfile = selectProfile(profiles);
            if (selectedProfile == null) {
                System.out.println("Profile selection cancelled.");
                System.exit(0);
            }
        } else {
            // Only one profile found - use it automatically
            selectedProfile = profiles.get(0);
            if ("default".equals(selectedProfile)) {
                selectedProfile = null;
            }
        }

        if (credentials == null) {
            try {
                // Initialize Nexus client with selected profile
                credentials = new Credentials(selectedProfile);
                client = new NexusClient(credentials);
                service = new NexusService(client);
            } catch (Exception e) {
                System.err.println("ERROR: Failed to initialize Nexus client: " + e.getMessage());
                System.err.println("\nPlease configure credentials:");
                System.err.println("  Set environment variables:");
                System.err.println("    export NEXUS_URL=https://your-nexus-server.com");
                System.err.println("    export NEXUS_USER=your-username");
                System.err.println("    export NEXUS_PASSWORD=your-password");
                System.err.println("  Or create ~/.flossware/nexus/nexus.properties");
                System.exit(1);
            }
        } else {
            try {
                // Initialize Nexus client with collected credentials
                client = new NexusClient(credentials);
                service = new NexusService(client);
            } catch (Exception e) {
                System.err.println("ERROR: Failed to initialize Nexus client: " + e.getMessage());
                System.exit(1);
            }
        }

        NcursesBridge.init();
        NcursesBridge.setNonBlocking(false);

        try {
            setupUI();
            runEventLoop();
        } finally {
            NcursesBridge.stop();
        }
    }

    private static String selectProfile(List<String> profiles) {
        System.out.println("\n========================================");
        System.out.println("  Multiple Configuration Profiles Found");
        System.out.println("========================================\n");

        for (int i = 0; i < profiles.size(); i++) {
            System.out.printf("  %d. %s\n", i + 1, profiles.get(i));
        }

        System.out.println("\n  0. Cancel");
        System.out.println("\n========================================");
        System.out.print("Select profile (enter number): ");

        try {
            java.util.Scanner scanner = new java.util.Scanner(System.in);
            int choice = scanner.nextInt();

            if (choice == 0) {
                return null;
            }

            if (choice >= 1 && choice <= profiles.size()) {
                String selected = profiles.get(choice - 1);
                // Convert "default" back to null for Credentials constructor
                return "default".equals(selected) ? null : selected;
            } else {
                System.err.println("\nInvalid selection. Exiting.");
                return null;
            }
        } catch (Exception e) {
            System.err.println("\nInvalid input. Exiting.");
            return null;
        }
    }

    private static Credentials collectCredentialsFromConsole() {
        System.out.println("\n========================================");
        System.out.println("  No Configuration Files Found");
        System.out.println("========================================\n");
        System.out.println("Please enter your Nexus credentials:\n");

        java.util.Scanner scanner = new java.util.Scanner(System.in);

        // Collect URL
        System.out.print("Nexus URL (e.g., https://your-nexus-server.com): ");
        String url = scanner.nextLine().trim();
        if (url.isEmpty()) {
            System.err.println("ERROR: URL cannot be empty.");
            return null;
        }

        // Collect username
        System.out.print("Username: ");
        String user = scanner.nextLine().trim();
        if (user.isEmpty()) {
            System.err.println("ERROR: Username cannot be empty.");
            return null;
        }

        // Collect password
        System.out.print("Password: ");
        String password;
        if (System.console() != null) {
            // Use console for password hiding if available
            char[] passwordChars = System.console().readPassword();
            password = new String(passwordChars);
        } else {
            // Fallback to regular input
            password = scanner.nextLine();
        }
        password = password.trim();
        if (password.isEmpty()) {
            System.err.println("ERROR: Password cannot be empty.");
            return null;
        }

        // Collect repositories (optional)
        System.out.print("Repositories (optional, comma-separated): ");
        String repos = scanner.nextLine().trim();

        System.out.println("\n========================================\n");

        try {
            return new Credentials(url, user, password, repos);
        } catch (IllegalArgumentException e) {
            System.err.println("ERROR: Invalid credentials: " + e.getMessage());
            return null;
        }
    }

    private static void setupUI() {
        buffer = new char[40][120];

        RootPane root = RootPane.getInstance();
        root.setSize(120, 40);

        // Create main frame
        JFrame frame = new JFrame("Nexus Repository Manager - TAB: navigate, SPACE: activate, Q/ESC: quit");
        frame.setLocation(0, 0);
        frame.setSize(120, 38);
        frame.setVisible(true);

        // Create main panel
        JPanel panel = new JPanel();
        panel.setLocation(2, 3);
        panel.setSize(116, 32);
        panel.setBordered(true);

        // Title
        JLabel title = new JLabel("Nexus Repository Manager");
        title.setLocation(4, 5);
        title.setSize(50, 1);
        title.setAlignment(JLabel.ALIGN_LEFT);

        // Repository field
        JLabel repoLabel = new JLabel("Repository:");
        repoLabel.setLocation(4, 7);
        repoLabel.setSize(15, 1);

        repositoryField = new JTextField();
        repositoryField.setLocation(20, 7);
        repositoryField.setSize(40, 1);
        repositoryField.setText(credentials.getDefaultRepository());
        focusableComponents.add(repositoryField);

        // Regex filter field
        JLabel regexLabel = new JLabel("Regex Filter:");
        regexLabel.setLocation(4, 9);
        regexLabel.setSize(15, 1);

        regexField = new JTextField();
        regexField.setLocation(20, 9);
        regexField.setSize(40, 1);
        regexField.setText(credentials.getDefaultRegex());
        focusableComponents.add(regexField);

        // Dry run checkbox
        dryRunCheckbox = new JCheckbox("Dry Run (preview only)");
        dryRunCheckbox.setLocation(65, 9);
        dryRunCheckbox.setSize(30, 1);
        dryRunCheckbox.setChecked(credentials.isDefaultDryRun());
        focusableComponents.add(dryRunCheckbox);

        // Available repositories label (if configured)
        JLabel reposDisplayLabel = null;
        if (!credentials.getRepositories().isEmpty()) {
            JLabel reposLabelText = new JLabel("Available Repos:");
            reposLabelText.setLocation(4, 11);
            reposLabelText.setSize(18, 1);

            reposDisplayLabel = new JLabel(String.join(", ", credentials.getRepositories()));
            reposDisplayLabel.setLocation(23, 11);
            reposDisplayLabel.setSize(90, 1);

            panel.add(reposLabelText);
            panel.add(reposDisplayLabel);
        }

        // Buttons (adjusted y position if repos are shown)
        int buttonY = credentials.getRepositories().isEmpty() ? 12 : 13;

        JButton listButton = new JButton("List");
        listButton.setLocation(4, buttonY);
        listButton.setSize(10, 1);
        listButton.addActionListener(() -> executeList(false));
        focusableComponents.add(listButton);

        JButton refreshButton = new JButton("Refresh");
        refreshButton.setLocation(16, buttonY);
        refreshButton.setSize(12, 1);
        refreshButton.addActionListener(() -> executeList(true));
        focusableComponents.add(refreshButton);

        JButton deleteButton = new JButton("Delete");
        deleteButton.setLocation(30, buttonY);
        deleteButton.setSize(12, 1);
        deleteButton.addActionListener(() -> executeDelete());
        focusableComponents.add(deleteButton);

        JButton clearButton = new JButton("Clear");
        clearButton.setLocation(44, buttonY);
        clearButton.setSize(10, 1);
        clearButton.addActionListener(() -> {
            setupResultsPanel();
            statusLabel.setText("Results cleared");
            markDirty();
        });
        focusableComponents.add(clearButton);

        JButton quitButton = new JButton("Quit");
        quitButton.setLocation(56, buttonY);
        quitButton.setSize(10, 1);
        quitButton.addActionListener(() -> running = false);
        focusableComponents.add(quitButton);

        // Status label (adjusted y position)
        int statusY = credentials.getRepositories().isEmpty() ? 15 : 16;
        statusLabel = new JLabel("Ready - List:cached, Refresh:bypass cache, Delete:always fresh");
        statusLabel.setLocation(4, statusY);
        statusLabel.setSize(110, 1);

        // Results label (adjusted y position)
        int resultsLabelY = credentials.getRepositories().isEmpty() ? 17 : 18;
        JLabel resultsLabel = new JLabel("Results:");
        resultsLabel.setLocation(4, resultsLabelY);
        resultsLabel.setSize(15, 1);

        // Results panel (adjusted y position and size)
        int resultsPanelY = credentials.getRepositories().isEmpty() ? 18 : 19;
        int resultsPanelHeight = credentials.getRepositories().isEmpty() ? 14 : 13;
        resultsPanel = new JPanel();
        resultsPanel.setLocation(4, resultsPanelY);
        resultsPanel.setSize(110, resultsPanelHeight);
        resultsPanel.setBordered(false);

        // Add all components to panel
        panel.add(title);
        panel.add(repoLabel);
        panel.add(repositoryField);
        panel.add(regexLabel);
        panel.add(regexField);
        panel.add(dryRunCheckbox);
        panel.add(listButton);
        panel.add(deleteButton);
        panel.add(clearButton);
        panel.add(quitButton);
        panel.add(statusLabel);
        panel.add(resultsLabel);
        panel.add(resultsPanel);

        frame.add(panel);
        root.add(frame);

        setupResultsPanel();
        markDirty();
    }

    private static void setupResultsPanel() {
        // Remove all existing result labels
        for (JLabel label : resultLabels) {
            resultsPanel.remove(label);
        }
        resultLabels.clear();

        JLabel placeholder = new JLabel("No results. Enter repository and click List (uses cache) or Refresh (bypasses cache)");
        placeholder.setLocation(0, 0);
        placeholder.setSize(108, 1);
        resultsPanel.add(placeholder);
        resultLabels.add(placeholder);
    }

    private static void setResults(String text) {
        // Remove all existing result labels
        for (JLabel label : resultLabels) {
            resultsPanel.remove(label);
        }
        resultLabels.clear();

        String[] lines = text.split("\n");
        int y = 0;
        for (String line : lines) {
            if (y >= 14) break;  // Max height of results panel

            JLabel lineLabel = new JLabel(line);
            lineLabel.setLocation(0, y);
            lineLabel.setSize(108, 1);
            resultsPanel.add(lineLabel);
            resultLabels.add(lineLabel);
            y++;
        }

        markDirty();
    }

    private static void executeList(boolean forceRefresh) {
        String repository = repositoryField.getText().trim();
        if (repository.isEmpty()) {
            statusLabel.setText("ERROR: Repository name is required");
            markDirty();
            return;
        }

        String regex = regexField.getText().trim();
        if (regex.isEmpty()) {
            regex = null;
        }

        String cacheStatus = service.getCacheStatus(repository);
        String mode = forceRefresh ? " (refreshing cache)" : " (" + cacheStatus + ")";
        statusLabel.setText("Listing components from: " + repository + mode + "...");
        markDirty();

        try {
            // Capture service output
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(baos);
            PrintStream oldOut = System.out;
            System.setOut(ps);

            try {
                service.listRepository(repository, regex, forceRefresh);
            } finally {
                System.out.flush();
                System.setOut(oldOut);
            }

            String output = baos.toString();
            setResults(output);

            String newCacheStatus = service.getCacheStatus(repository);
            statusLabel.setText("List completed - " + newCacheStatus);
        } catch (Exception e) {
            setResults("ERROR: " + e.getMessage());
            statusLabel.setText("List failed: " + e.getMessage());
        }

        markDirty();
    }

    private static void executeDelete() {
        String repository = repositoryField.getText().trim();
        if (repository.isEmpty()) {
            statusLabel.setText("ERROR: Repository name is required");
            markDirty();
            return;
        }

        String regex = regexField.getText().trim();
        if (regex.isEmpty()) {
            regex = null;
        }

        boolean dryRun = dryRunCheckbox.isChecked();
        String mode = dryRun ? "(DRY RUN)" : "(ACTUAL DELETE)";
        statusLabel.setText("Deleting from repository: " + repository + " " + mode + "...");
        markDirty();

        try {
            // Capture service output
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(baos);
            PrintStream oldOut = System.out;
            PrintStream oldErr = System.err;
            System.setOut(ps);
            System.setErr(ps);

            try {
                service.deleteFromRepository(repository, regex, dryRun);
            } finally {
                System.out.flush();
                System.err.flush();
                System.setOut(oldOut);
                System.setErr(oldErr);
            }

            String output = baos.toString();
            setResults(output);
            statusLabel.setText("Delete operation completed " + mode);
        } catch (Exception e) {
            setResults("ERROR: " + e.getMessage());
            statusLabel.setText("Delete failed: " + e.getMessage());
        }

        markDirty();
    }

    private static void runEventLoop() throws Throwable {
        while (running) {
            // Render if dirty
            if (RootPane.getInstance().isDirty()) {
                render();
                RootPane.getInstance().clearDirty();
            }

            int ch = NcursesBridge.getChar();

            if (ch != -1) {
                handleKey(ch);
            }

            // Small delay to avoid spinning
            Thread.sleep(10);
        }
    }

    private static void handleKey(int ch) {
        if (ch == KEY_ESC || ch == KEY_Q) {
            running = false;
        } else if (ch == KEY_TAB || ch == KEY_DOWN) {
            moveFocusNext();
        } else if (ch == KEY_UP) {
            moveFocusPrevious();
        } else if (ch == KEY_SPACE || ch == KEY_ENTER) {
            Component focused = getFocusedComponent();
            if (focused != null) {
                activateComponent(focused);
            }
        } else if (ch >= 32 && ch <= 126) {
            // Printable character - pass to focused component
            Component focused = getFocusedComponent();
            if (focused instanceof JTextField field) {
                String current = field.getText();
                field.setText(current + (char) ch);
                markDirty();
            }
        } else if (ch == 127 || ch == 263) {
            // Backspace
            Component focused = getFocusedComponent();
            if (focused instanceof JTextField field) {
                String current = field.getText();
                if (!current.isEmpty()) {
                    field.setText(current.substring(0, current.length() - 1));
                    markDirty();
                }
            }
        }
    }

    private static void activateComponent(Component component) {
        if (component instanceof JButton button) {
            button.doClick();
        } else if (component instanceof JCheckbox checkbox) {
            checkbox.setChecked(!checkbox.isChecked());
            markDirty();
        }
    }

    private static void moveFocusNext() {
        if (focusableComponents.isEmpty()) return;
        currentFocus = (currentFocus + 1) % focusableComponents.size();
        markDirty();
    }

    private static void moveFocusPrevious() {
        if (focusableComponents.isEmpty()) return;
        currentFocus = (currentFocus - 1 + focusableComponents.size()) % focusableComponents.size();
        markDirty();
    }

    private static Component getFocusedComponent() {
        if (focusableComponents.isEmpty()) return null;
        if (currentFocus < 0 || currentFocus >= focusableComponents.size()) return null;
        return focusableComponents.get(currentFocus);
    }

    private static void markDirty() {
        RootPane.getInstance().markDirty();
    }

    private static void render() throws Throwable {
        // Clear buffer
        for (int i = 0; i < 40; i++) {
            for (int j = 0; j < 120; j++) {
                buffer[i][j] = ' ';
            }
        }

        // Paint components
        RootPane.getInstance().paint(buffer);

        // Highlight focused component
        if (currentFocus >= 0 && currentFocus < focusableComponents.size()) {
            Component focused = focusableComponents.get(currentFocus);
            int x = focused.getX();
            int y = focused.getY();
            int w = focused.getWidth();

            // Draw focus indicator
            if (x > 0 && y >= 0 && y < 40) {
                buffer[y][x - 1] = '>';
            }
            if (x + w < 120 && y >= 0 && y < 40) {
                buffer[y][x + w] = '<';
            }
        }

        // Send to ncurses
        NcursesBridge.clear();
        for (int y = 0; y < 40; y++) {
            for (int x = 0; x < 120; x++) {
                NcursesBridge.moveCursor(y, x, buffer[y][x]);
            }
        }
        NcursesBridge.refresh();
    }
}

package org.flossware.nexus;

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
public class NexusUI {
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

    public static void main(String[] args) throws Throwable {
        if (!NcursesBridge.isAvailable()) {
            System.err.println("ERROR: ncurses library is not available!");
            System.err.println("Please install ncurses development library:");
            System.err.println("  - Ubuntu/Debian: sudo apt-get install libncurses-dev");
            System.err.println("  - Fedora/RHEL: sudo dnf install ncurses-devel");
            System.err.println("  - Arch: sudo pacman -S ncurses");
            System.exit(1);
        }

        try {
            // Initialize Nexus client
            Credentials credentials = new Credentials();
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

        NcursesBridge.init();
        NcursesBridge.setNonBlocking(false);

        try {
            setupUI();
            runEventLoop();
        } finally {
            NcursesBridge.stop();
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
        repositoryField.setText("");
        focusableComponents.add(repositoryField);

        // Regex filter field
        JLabel regexLabel = new JLabel("Regex Filter:");
        regexLabel.setLocation(4, 9);
        regexLabel.setSize(15, 1);

        regexField = new JTextField();
        regexField.setLocation(20, 9);
        regexField.setSize(40, 1);
        regexField.setText("");
        focusableComponents.add(regexField);

        // Dry run checkbox
        dryRunCheckbox = new JCheckbox("Dry Run (preview only)");
        dryRunCheckbox.setLocation(65, 9);
        dryRunCheckbox.setSize(30, 1);
        dryRunCheckbox.setChecked(true);
        focusableComponents.add(dryRunCheckbox);

        // Buttons
        JButton listButton = new JButton("List Components");
        listButton.setLocation(4, 12);
        listButton.setSize(20, 1);
        listButton.addActionListener(() -> executeList());
        focusableComponents.add(listButton);

        JButton deleteButton = new JButton("Delete Components");
        deleteButton.setLocation(26, 12);
        deleteButton.setSize(22, 1);
        deleteButton.addActionListener(() -> executeDelete());
        focusableComponents.add(deleteButton);

        JButton clearButton = new JButton("Clear Results");
        clearButton.setLocation(50, 12);
        clearButton.setSize(18, 1);
        clearButton.addActionListener(() -> {
            setupResultsPanel();
            statusLabel.setText("Results cleared");
            markDirty();
        });
        focusableComponents.add(clearButton);

        JButton quitButton = new JButton("Quit");
        quitButton.setLocation(70, 12);
        quitButton.setSize(10, 1);
        quitButton.addActionListener(() -> running = false);
        focusableComponents.add(quitButton);

        // Status label
        statusLabel = new JLabel("Ready - Enter repository name and press List or Delete");
        statusLabel.setLocation(4, 15);
        statusLabel.setSize(110, 1);

        // Results label
        JLabel resultsLabel = new JLabel("Results:");
        resultsLabel.setLocation(4, 17);
        resultsLabel.setSize(15, 1);

        // Results panel (we'll draw text directly in this area)
        resultsPanel = new JPanel();
        resultsPanel.setLocation(4, 18);
        resultsPanel.setSize(110, 14);
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

        JLabel placeholder = new JLabel("No results yet. Enter a repository name and click List Components.");
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

    private static void executeList() {
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

        statusLabel.setText("Listing components from repository: " + repository + "...");
        markDirty();

        try {
            // Capture service output
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(baos);
            PrintStream oldOut = System.out;
            System.setOut(ps);

            try {
                service.listRepository(repository, regex);
            } finally {
                System.out.flush();
                System.setOut(oldOut);
            }

            String output = baos.toString();
            setResults(output);
            statusLabel.setText("List completed successfully");
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

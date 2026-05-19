package org.flossware.jnexus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Swing-based GUI for interacting with Nexus Repository Manager.
 * <p>
 * Provides a modern graphical interface for listing and deleting
 * components from Nexus repositories with optional regex filtering.
 * </p>
 *
 * @author sfloess
 * @since 1.0
 */
public class JNexusSwing {
    private static final Logger logger = LoggerFactory.getLogger(JNexusSwing.class);

    // UI Components
    private JFrame frame;
    private JTextField repositoryField;
    private JTextField regexField;
    private JCheckBox dryRunCheckbox;
    private JTextArea resultsArea;
    private JLabel statusLabel;
    private JButton listButton;
    private JButton refreshButton;
    private JButton deleteButton;
    private JButton clearButton;

    // Services
    private NexusClient client;
    private NexusService service;
    private Credentials credentials;

    public static void main(String[] args) {
        // Set system look and feel for native appearance
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            logger.warn("Could not set system look and feel: {}", e.getMessage());
        }

        SwingUtilities.invokeLater(() -> {
            try {
                // Discover available profiles
                java.util.List<String> profiles = Credentials.discoverProfiles();
                String selectedProfile = null;
                Credentials credentials = null;

                if (profiles.isEmpty()) {
                    // No configuration found - show credential collection dialog
                    credentials = showCredentialDialog();
                    if (credentials == null) {
                        // User cancelled
                        System.exit(0);
                        return;
                    }

                    // Ask if user wants to save credentials
                    int saveChoice = JOptionPane.showConfirmDialog(
                        null,
                        "Would you like to save these credentials to a properties file?\n" +
                        "They will be saved to: ~/.flossware/nexus/nexus.properties",
                        "Save Credentials?",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE
                    );

                    if (saveChoice == JOptionPane.YES_OPTION) {
                        try {
                            credentials.saveToPropertiesFile(null);
                            JOptionPane.showMessageDialog(
                                null,
                                "Credentials saved successfully to:\n~/.flossware/nexus/nexus.properties",
                                "Saved",
                                JOptionPane.INFORMATION_MESSAGE
                            );
                        } catch (IOException e) {
                            JOptionPane.showMessageDialog(
                                null,
                                "Failed to save credentials: " + e.getMessage(),
                                "Save Error",
                                JOptionPane.ERROR_MESSAGE
                            );
                        }
                    }
                } else if (profiles.size() > 1) {
                    // Multiple profiles found - show selection dialog
                    String[] profileArray = profiles.toArray(new String[0]);
                    selectedProfile = (String) JOptionPane.showInputDialog(
                        null,
                        "Multiple configuration profiles found.\nPlease select one:",
                        "Select Profile",
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        profileArray,
                        profileArray[0]
                    );

                    if (selectedProfile == null) {
                        // User cancelled
                        System.exit(0);
                        return;
                    }

                    // Convert "default" back to null for Credentials constructor
                    if ("default".equals(selectedProfile)) {
                        selectedProfile = null;
                    }
                } else {
                    // Only one profile found - use it automatically
                    selectedProfile = profiles.get(0);
                    if ("default".equals(selectedProfile)) {
                        selectedProfile = null;
                    }
                }

                JNexusSwing app;
                if (credentials != null) {
                    // Use credentials from dialog
                    app = new JNexusSwing(credentials);
                } else {
                    // Use credentials from profile
                    app = new JNexusSwing(selectedProfile);
                }
                app.createAndShowGUI();
            } catch (Exception e) {
                logger.error("Failed to start Swing UI: {}", e.getMessage(), e);
                JOptionPane.showMessageDialog(null,
                    "Failed to initialize Nexus client:\n" + e.getMessage() +
                    "\n\nPlease configure credentials:\n" +
                    "  Set environment variables or create ~/.flossware/nexus/nexus.properties",
                    "Initialization Error",
                    JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }

    public JNexusSwing(String profile) throws Exception {
        // Initialize Nexus client with selected profile
        credentials = new Credentials(profile);
        client = new NexusClient(credentials);
        service = new NexusService(client);
    }

    public JNexusSwing(Credentials credentials) throws Exception {
        // Initialize Nexus client with provided credentials
        this.credentials = credentials;
        client = new NexusClient(credentials);
        service = new NexusService(client);
    }

    private static Credentials showCredentialDialog() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // URL field
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        panel.add(new JLabel("Nexus URL:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        JTextField urlField = new JTextField(30);
        urlField.setToolTipText("e.g., https://your-nexus-server.com");
        panel.add(urlField, gbc);

        // Username field
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        panel.add(new JLabel("Username:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        JTextField userField = new JTextField(30);
        panel.add(userField, gbc);

        // Password field
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.0;
        panel.add(new JLabel("Password:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        JPasswordField passwordField = new JPasswordField(30);
        panel.add(passwordField, gbc);

        // Repositories field (optional)
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0.0;
        panel.add(new JLabel("Repositories:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        JTextField reposField = new JTextField(30);
        reposField.setToolTipText("Optional: comma-separated list (e.g., maven-releases,npm-public)");
        panel.add(reposField, gbc);

        // Info label
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        JLabel infoLabel = new JLabel("<html><i>No configuration files found. Please enter your Nexus credentials.</i></html>");
        panel.add(infoLabel, gbc);

        int result = JOptionPane.showConfirmDialog(
            null,
            panel,
            "Enter Nexus Credentials",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );

        if (result == JOptionPane.OK_OPTION) {
            String url = urlField.getText().trim();
            String user = userField.getText().trim();
            String password = new String(passwordField.getPassword()).trim();
            String repos = reposField.getText().trim();

            if (url.isEmpty() || user.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(null,
                    "URL, Username, and Password are required.",
                    "Invalid Input",
                    JOptionPane.ERROR_MESSAGE);
                return showCredentialDialog(); // Recursive call to show dialog again
            }

            try {
                return new Credentials(url, user, password, repos);
            } catch (IllegalArgumentException e) {
                JOptionPane.showMessageDialog(null,
                    "Invalid credentials: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
                return showCredentialDialog(); // Recursive call to show dialog again
            }
        }

        return null; // User cancelled
    }

    private void createAndShowGUI() {
        frame = new JFrame("Nexus Repository Manager - Swing UI");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 700);
        frame.setLocationRelativeTo(null); // Center on screen

        // Create main panel with padding
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Top panel for input fields
        JPanel inputPanel = createInputPanel();
        mainPanel.add(inputPanel, BorderLayout.NORTH);

        // Center panel for results
        JPanel resultsPanel = createResultsPanel();
        mainPanel.add(resultsPanel, BorderLayout.CENTER);

        // Bottom panel for status
        JPanel statusPanel = createStatusPanel();
        mainPanel.add(statusPanel, BorderLayout.SOUTH);

        frame.add(mainPanel);
        frame.setVisible(true);
    }

    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Repository Configuration"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Repository label and field
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        panel.add(new JLabel("Repository:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        repositoryField = new JTextField(credentials.getDefaultRepository(), 30);
        panel.add(repositoryField, gbc);

        // Regex filter label and field
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        panel.add(new JLabel("Regex Filter:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        regexField = new JTextField(credentials.getDefaultRegex(), 30);
        regexField.setToolTipText("Optional regex pattern to filter components (e.g., .*SNAPSHOT.*)");
        panel.add(regexField, gbc);

        // Dry run checkbox
        gbc.gridx = 1;
        gbc.gridy = 2;
        dryRunCheckbox = new JCheckBox("Dry Run (preview only, no actual deletion)");
        dryRunCheckbox.setSelected(credentials.isDefaultDryRun());
        panel.add(dryRunCheckbox, gbc);

        // Available repositories display (if configured)
        if (!credentials.getRepositories().isEmpty()) {
            gbc.gridx = 0;
            gbc.gridy = 3;
            gbc.weightx = 0.0;
            gbc.gridwidth = 1;
            gbc.anchor = GridBagConstraints.NORTHWEST;
            panel.add(new JLabel("Available Repos:"), gbc);

            gbc.gridx = 1;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.BOTH;
            JTextArea reposArea = new JTextArea(2, 30);
            reposArea.setText(String.join(", ", credentials.getRepositories()));
            reposArea.setEditable(false);
            reposArea.setLineWrap(true);
            reposArea.setWrapStyleWord(true);
            reposArea.setBackground(panel.getBackground());
            reposArea.setFont(new JLabel().getFont());
            JScrollPane reposScroll = new JScrollPane(reposArea);
            reposScroll.setBorder(BorderFactory.createEmptyBorder());
            panel.add(reposScroll, gbc);

            gbc.fill = GridBagConstraints.HORIZONTAL;
        }

        // Buttons panel
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(createButtonPanel(), gbc);

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        listButton = new JButton("List");
        listButton.setToolTipText("List components (uses cache)");
        listButton.addActionListener(e -> executeList(false));
        panel.add(listButton);

        refreshButton = new JButton("Refresh");
        refreshButton.setToolTipText("Refresh components (bypasses cache)");
        refreshButton.addActionListener(e -> executeList(true));
        panel.add(refreshButton);

        deleteButton = new JButton("Delete");
        deleteButton.setToolTipText("Delete components matching filter");
        deleteButton.addActionListener(e -> executeDelete());
        panel.add(deleteButton);

        clearButton = new JButton("Clear Results");
        clearButton.addActionListener(e -> {
            resultsArea.setText("");
            setStatus("Results cleared", false);
        });
        panel.add(clearButton);

        JButton quitButton = new JButton("Quit");
        quitButton.addActionListener(e -> System.exit(0));
        panel.add(quitButton);

        return panel;
    }

    private JPanel createResultsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Results"));

        resultsArea = new JTextArea();
        resultsArea.setEditable(false);
        resultsArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        resultsArea.setText("No results. Enter repository name and click List or Refresh.");

        JScrollPane scrollPane = new JScrollPane(resultsArea);
        scrollPane.setPreferredSize(new Dimension(800, 400));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEtchedBorder());

        statusLabel = new JLabel("Ready - List: cached, Refresh: bypass cache, Delete: always fresh");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panel.add(statusLabel, BorderLayout.WEST);

        return panel;
    }

    private void executeList(boolean forceRefresh) {
        String repository = repositoryField.getText().trim();
        if (repository.isEmpty()) {
            setStatus("ERROR: Repository name is required", true);
            JOptionPane.showMessageDialog(frame,
                "Please enter a repository name.",
                "Validation Error",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        String regex = regexField.getText().trim();
        if (regex.isEmpty()) {
            regex = null;
        }

        String cacheStatus = service.getCacheStatus(repository);
        String mode = forceRefresh ? " (refreshing cache)" : " (" + cacheStatus + ")";
        setStatus("Listing components from: " + repository + mode + "...", false);

        // Run in background thread to keep UI responsive
        String finalRegex = regex;
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                try {
                    // Capture service output
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    PrintStream ps = new PrintStream(baos);
                    PrintStream oldOut = System.out;
                    System.setOut(ps);

                    try {
                        service.listRepository(repository, finalRegex, forceRefresh);
                    } finally {
                        System.out.flush();
                        System.setOut(oldOut);
                    }

                    return baos.toString();
                } catch (Exception e) {
                    logger.error("List operation failed", e);
                    return "ERROR: " + e.getMessage();
                }
            }

            @Override
            protected void done() {
                try {
                    String output = get();
                    resultsArea.setText(output);
                    resultsArea.setCaretPosition(0);

                    if (output.startsWith("ERROR:")) {
                        setStatus("List failed: " + output.substring(7), true);
                    } else {
                        String newCacheStatus = service.getCacheStatus(repository);
                        setStatus("List completed - " + newCacheStatus, false);
                    }
                } catch (Exception e) {
                    setStatus("List failed: " + e.getMessage(), true);
                }
            }
        }.execute();
    }

    private void executeDelete() {
        String repository = repositoryField.getText().trim();
        if (repository.isEmpty()) {
            setStatus("ERROR: Repository name is required", true);
            JOptionPane.showMessageDialog(frame,
                "Please enter a repository name.",
                "Validation Error",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        String regex = regexField.getText().trim();
        if (regex.isEmpty()) {
            regex = null;
        }

        boolean dryRun = dryRunCheckbox.isSelected();

        // Confirmation dialog for actual deletions
        if (!dryRun) {
            String message = "WARNING: This will permanently delete components from repository: " + repository;
            if (regex != null) {
                message += "\nFilter: " + regex;
            }
            message += "\n\nAre you sure you want to continue?";

            int choice = JOptionPane.showConfirmDialog(frame,
                message,
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

            if (choice != JOptionPane.YES_OPTION) {
                setStatus("Delete operation cancelled by user", false);
                return;
            }
        }

        String mode = dryRun ? "(DRY RUN)" : "(ACTUAL DELETE)";
        setStatus("Deleting from repository: " + repository + " " + mode + "...", false);

        // Run in background thread
        String finalRegex = regex;
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                try {
                    // Capture service output
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    PrintStream ps = new PrintStream(baos);
                    PrintStream oldOut = System.out;
                    PrintStream oldErr = System.err;
                    System.setOut(ps);
                    System.setErr(ps);

                    try {
                        service.deleteFromRepository(repository, finalRegex, dryRun);
                    } finally {
                        System.out.flush();
                        System.err.flush();
                        System.setOut(oldOut);
                        System.setErr(oldErr);
                    }

                    return baos.toString();
                } catch (Exception e) {
                    logger.error("Delete operation failed", e);
                    return "ERROR: " + e.getMessage();
                }
            }

            @Override
            protected void done() {
                try {
                    String output = get();
                    resultsArea.setText(output);
                    resultsArea.setCaretPosition(0);

                    if (output.startsWith("ERROR:")) {
                        setStatus("Delete failed: " + output.substring(7), true);
                    } else {
                        setStatus("Delete operation completed " + mode, false);
                    }
                } catch (Exception e) {
                    setStatus("Delete failed: " + e.getMessage(), true);
                }
            }
        }.execute();
    }

    private void setStatus(String message, boolean isError) {
        statusLabel.setText(message);
        if (isError) {
            statusLabel.setForeground(Color.RED);
        } else {
            statusLabel.setForeground(Color.BLACK);
        }
    }
}

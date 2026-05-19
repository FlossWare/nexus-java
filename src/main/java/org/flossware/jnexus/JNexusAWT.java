package org.flossware.jnexus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.event.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

/**
 * AWT-based GUI for interacting with Nexus Repository Manager.
 * <p>
 * Provides a classic AWT graphical interface for listing and deleting
 * components from Nexus repositories with optional regex filtering.
 * Uses only AWT components (Frame, Button, TextField, etc.) for maximum compatibility.
 * </p>
 *
 * @author sfloess
 * @since 1.0
 */
public class JNexusAWT {
    private static final Logger logger = LoggerFactory.getLogger(JNexusAWT.class);

    // UI Components
    private Frame frame;
    private TextField repositoryField;
    private TextField regexField;
    private Checkbox dryRunCheckbox;
    private TextArea resultsArea;
    private Label statusLabel;
    private Button listButton;
    private Button refreshButton;
    private Button deleteButton;
    private Button clearButton;

    // Services
    private NexusClient client;
    private NexusService service;
    private Credentials credentials;

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
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
                    if (showSaveCredentialsDialog()) {
                        try {
                            credentials.saveToPropertiesFile(null);
                            showInfoDialog(
                                "Credentials saved successfully to:\n~/.flossware/nexus/nexus.properties",
                                "Saved"
                            );
                        } catch (java.io.IOException e) {
                            showErrorDialog(
                                "Failed to save credentials: " + e.getMessage(),
                                "Save Error"
                            );
                        }
                    }
                } else if (profiles.size() > 1) {
                    // Multiple profiles found - show selection dialog
                    selectedProfile = showProfileSelectionDialog(profiles);
                    if (selectedProfile == null) {
                        // User cancelled
                        System.exit(0);
                        return;
                    }
                } else {
                    // Only one profile found - use it automatically
                    selectedProfile = profiles.get(0);
                    if ("default".equals(selectedProfile)) {
                        selectedProfile = null;
                    }
                }

                JNexusAWT app;
                if (credentials != null) {
                    // Use credentials from dialog
                    app = new JNexusAWT(credentials);
                } else {
                    // Use credentials from profile
                    app = new JNexusAWT(selectedProfile);
                }
                app.createAndShowGUI();
            } catch (Exception e) {
                LoggerFactory.getLogger(JNexusAWT.class).error("Failed to start AWT UI: {}", e.getMessage(), e);
                showErrorDialog(
                    "Failed to initialize Nexus client:\n" + e.getMessage() +
                    "\n\nPlease configure credentials:\n" +
                    "  Set environment variables or\n" +
                    "  create ~/.flossware/nexus/nexus.properties",
                    "Initialization Error");
                System.exit(1);
            }
        });
    }

    private static void showErrorDialog(String message, String title) {
        Dialog errorDialog = new Dialog((Frame) null, title, true);
        errorDialog.setLayout(new BorderLayout());

        TextArea errorText = new TextArea(message, 10, 50, TextArea.SCROLLBARS_VERTICAL_ONLY);
        errorText.setEditable(false);
        errorDialog.add(errorText, BorderLayout.CENTER);

        Button okButton = new Button("OK");
        okButton.addActionListener(e -> errorDialog.dispose());
        Panel buttonPanel = new Panel();
        buttonPanel.add(okButton);
        errorDialog.add(buttonPanel, BorderLayout.SOUTH);

        errorDialog.pack();
        errorDialog.setLocationRelativeTo(null);
        errorDialog.setVisible(true);
    }

    private static void showInfoDialog(String message, String title) {
        Dialog infoDialog = new Dialog((Frame) null, title, true);
        infoDialog.setLayout(new BorderLayout());

        TextArea infoText = new TextArea(message, 6, 50, TextArea.SCROLLBARS_VERTICAL_ONLY);
        infoText.setEditable(false);
        infoDialog.add(infoText, BorderLayout.CENTER);

        Button okButton = new Button("OK");
        okButton.addActionListener(e -> infoDialog.dispose());
        Panel buttonPanel = new Panel();
        buttonPanel.add(okButton);
        infoDialog.add(buttonPanel, BorderLayout.SOUTH);

        infoDialog.pack();
        infoDialog.setLocationRelativeTo(null);
        infoDialog.setVisible(true);
    }

    private static boolean showSaveCredentialsDialog() {
        Dialog dialog = new Dialog((Frame) null, "Save Credentials?", true);
        dialog.setLayout(new BorderLayout(10, 10));

        TextArea messageArea = new TextArea(
            "Would you like to save these credentials to a properties file?\n\n" +
            "They will be saved to:\n~/.flossware/nexus/nexus.properties",
            5, 50, TextArea.SCROLLBARS_NONE
        );
        messageArea.setEditable(false);
        dialog.add(messageArea, BorderLayout.CENTER);

        Panel buttonPanel = new Panel();
        Button yesButton = new Button("Yes");
        Button noButton = new Button("No");

        final boolean[] result = {false};

        yesButton.addActionListener(e -> {
            result[0] = true;
            dialog.dispose();
        });

        noButton.addActionListener(e -> {
            result[0] = false;
            dialog.dispose();
        });

        buttonPanel.add(yesButton);
        buttonPanel.add(noButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);

        return result[0];
    }

    private static String showProfileSelectionDialog(java.util.List<String> profiles) {
        Dialog dialog = new Dialog((Frame) null, "Select Profile", true);
        dialog.setLayout(new BorderLayout(10, 10));

        // Message label
        Label messageLabel = new Label("Multiple configuration profiles found. Please select one:");
        Panel messagePanel = new Panel();
        messagePanel.add(messageLabel);
        dialog.add(messagePanel, BorderLayout.NORTH);

        // Choice (dropdown) for profiles
        Choice profileChoice = new Choice();
        for (String profile : profiles) {
            profileChoice.add(profile);
        }
        Panel choicePanel = new Panel();
        choicePanel.add(new Label("Profile: "));
        choicePanel.add(profileChoice);
        dialog.add(choicePanel, BorderLayout.CENTER);

        // Buttons
        Panel buttonPanel = new Panel();
        Button okButton = new Button("OK");
        Button cancelButton = new Button("Cancel");

        final String[] result = {null};

        okButton.addActionListener(e -> {
            String selected = profileChoice.getSelectedItem();
            // Convert "default" back to null for Credentials constructor
            result[0] = "default".equals(selected) ? null : selected;
            dialog.dispose();
        });

        cancelButton.addActionListener(e -> {
            result[0] = null;
            dialog.dispose();
        });

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);

        return result[0];
    }

    public JNexusAWT(String profile) throws Exception {
        // Initialize Nexus client with selected profile
        credentials = new Credentials(profile);
        client = new NexusClient(credentials);
        service = new NexusService(client);
    }

    public JNexusAWT(Credentials credentials) throws Exception {
        // Initialize Nexus client with provided credentials
        this.credentials = credentials;
        client = new NexusClient(credentials);
        service = new NexusService(client);
    }

    private static Credentials showCredentialDialog() {
        Dialog dialog = new Dialog((Frame) null, "Enter Nexus Credentials", true);
        dialog.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Info label
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        Label infoLabel = new Label("No configuration files found. Please enter your Nexus credentials.");
        dialog.add(infoLabel, gbc);

        // URL field
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        dialog.add(new Label("Nexus URL:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        TextField urlField = new TextField(40);
        dialog.add(urlField, gbc);

        // Username field
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.0;
        dialog.add(new Label("Username:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        TextField userField = new TextField(40);
        dialog.add(userField, gbc);

        // Password field
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0.0;
        dialog.add(new Label("Password:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        TextField passwordField = new TextField(40);
        passwordField.setEchoChar('*');
        dialog.add(passwordField, gbc);

        // Repositories field (optional)
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weightx = 0.0;
        dialog.add(new Label("Repositories:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        TextField reposField = new TextField(40);
        dialog.add(reposField, gbc);

        // Hint label
        gbc.gridx = 1;
        gbc.gridy = 5;
        Label hintLabel = new Label("(Optional: comma-separated, e.g., maven-releases,npm-public)");
        hintLabel.setFont(new Font("Dialog", Font.PLAIN, 10));
        dialog.add(hintLabel, gbc);

        // Buttons
        Panel buttonPanel = new Panel();
        Button okButton = new Button("OK");
        Button cancelButton = new Button("Cancel");

        final Credentials[] result = {null};
        final boolean[] shouldRetry = {false};

        okButton.addActionListener(e -> {
            String url = urlField.getText().trim();
            String user = userField.getText().trim();
            String password = passwordField.getText().trim();
            String repos = reposField.getText().trim();

            if (url.isEmpty() || user.isEmpty() || password.isEmpty()) {
                showErrorDialog(
                    "URL, Username, and Password are required.",
                    "Invalid Input");
                shouldRetry[0] = true;
                dialog.dispose();
                return;
            }

            try {
                result[0] = new Credentials(url, user, password, repos);
                dialog.dispose();
            } catch (IllegalArgumentException ex) {
                showErrorDialog(
                    "Invalid credentials: " + ex.getMessage(),
                    "Error");
                shouldRetry[0] = true;
                dialog.dispose();
            }
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        dialog.add(buttonPanel, gbc);

        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);

        // If validation failed, show dialog again
        if (shouldRetry[0]) {
            return showCredentialDialog();
        }

        return result[0];
    }

    private void createAndShowGUI() {
        frame = new Frame("Nexus Repository Manager - AWT UI");
        frame.setSize(900, 700);
        frame.setLayout(new BorderLayout(10, 10));

        // Add window listener for close button
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        // Top panel for input fields
        Panel inputPanel = createInputPanel();
        frame.add(inputPanel, BorderLayout.NORTH);

        // Center panel for results
        Panel resultsPanel = createResultsPanel();
        frame.add(resultsPanel, BorderLayout.CENTER);

        // Bottom panel for status
        Panel statusPanel = createStatusPanel();
        frame.add(statusPanel, BorderLayout.SOUTH);

        // Center on screen
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setLocation(
            (screenSize.width - frame.getWidth()) / 2,
            (screenSize.height - frame.getHeight()) / 2
        );

        frame.setVisible(true);
    }

    private Panel createInputPanel() {
        Panel panel = new Panel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Title
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        Label titleLabel = new Label("Repository Configuration");
        titleLabel.setFont(new Font("Dialog", Font.BOLD, 14));
        panel.add(titleLabel, gbc);

        // Repository label and field
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        panel.add(new Label("Repository:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        repositoryField = new TextField(credentials.getDefaultRepository(), 40);
        repositoryField.addActionListener(e -> executeList(false)); // Enter triggers List
        panel.add(repositoryField, gbc);

        // Regex filter label and field
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.0;
        panel.add(new Label("Regex Filter:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        regexField = new TextField(credentials.getDefaultRegex(), 40);
        regexField.addActionListener(e -> executeList(false)); // Enter triggers List
        panel.add(regexField, gbc);

        // Dry run checkbox
        gbc.gridx = 1;
        gbc.gridy = 3;
        dryRunCheckbox = new Checkbox("Dry Run (preview only, no actual deletion)");
        dryRunCheckbox.setState(credentials.isDefaultDryRun());
        panel.add(dryRunCheckbox, gbc);

        // Available repositories dropdown (if configured)
        if (!credentials.getRepositories().isEmpty()) {
            logger.debug("Displaying {} repositories: {}",
                credentials.getRepositories().size(), credentials.getRepositories());

            gbc.gridx = 0;
            gbc.gridy = 4;
            gbc.weightx = 0.0;
            gbc.gridwidth = 1;
            gbc.anchor = GridBagConstraints.WEST;
            panel.add(new Label("Available Repos:"), gbc);

            gbc.gridx = 1;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            Choice reposChoice = new Choice();
            reposChoice.add("All");  // Add "All" option first
            for (String repo : credentials.getRepositories()) {
                reposChoice.add(repo);
            }
            reposChoice.addItemListener(e -> {
                String selected = reposChoice.getSelectedItem();
                if (selected != null && !selected.isEmpty()) {
                    if ("All".equals(selected)) {
                        repositoryField.setText("");  // Empty means all repositories
                    } else {
                        repositoryField.setText(selected);
                    }
                }
            });
            panel.add(reposChoice, gbc);

            gbc.anchor = GridBagConstraints.CENTER;
        } else {
            logger.debug("No repositories configured to display");
        }

        // Property file display (read-only)
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.weightx = 0.0;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(new Label("Config File:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        String configFile = credentials.getProfile() == null
            ? "~/.flossware/nexus/nexus.properties"
            : "~/.flossware/nexus/nexus-" + credentials.getProfile() + ".properties";
        TextField configFileField = new TextField(configFile, 40);
        configFileField.setEditable(false);
        configFileField.setBackground(panel.getBackground());
        panel.add(configFileField, gbc);

        // Buttons panel
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(createButtonPanel(), gbc);

        return panel;
    }

    private Panel createButtonPanel() {
        Panel panel = new Panel();
        panel.setLayout(new FlowLayout(FlowLayout.LEFT));

        listButton = new Button("List");
        listButton.addActionListener(e -> executeList(false));
        panel.add(listButton);

        refreshButton = new Button("Refresh");
        refreshButton.addActionListener(e -> executeList(true));
        panel.add(refreshButton);

        deleteButton = new Button("Delete");
        deleteButton.addActionListener(e -> executeDelete());
        panel.add(deleteButton);

        clearButton = new Button("Clear Results");
        clearButton.addActionListener(e -> {
            resultsArea.setText("");
            setStatus("Results cleared");
        });
        panel.add(clearButton);

        Button quitButton = new Button("Quit");
        quitButton.addActionListener(e -> System.exit(0));
        panel.add(quitButton);

        return panel;
    }

    private Panel createResultsPanel() {
        Panel panel = new Panel();
        panel.setLayout(new BorderLayout());

        // Title
        Label titleLabel = new Label("Results");
        titleLabel.setFont(new Font("Dialog", Font.BOLD, 14));
        panel.add(titleLabel, BorderLayout.NORTH);

        resultsArea = new TextArea("No results. Enter repository name and click List or Refresh.", 25, 80);
        resultsArea.setEditable(false);
        resultsArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        panel.add(resultsArea, BorderLayout.CENTER);

        return panel;
    }

    private Panel createStatusPanel() {
        Panel panel = new Panel();
        panel.setLayout(new BorderLayout());

        statusLabel = new Label("Ready - List: cached, Refresh: bypass cache, Delete: always fresh");
        panel.add(statusLabel, BorderLayout.WEST);

        return panel;
    }

    private void executeList(boolean forceRefresh) {
        String repository = repositoryField.getText().trim();
        if (repository.isEmpty()) {
            setStatus("ERROR: Repository name is required");
            showErrorDialog("Please enter a repository name.");
            return;
        }

        String regex = regexField.getText().trim();
        if (regex.isEmpty()) {
            regex = null;
        }

        String cacheStatus = service.getCacheStatus(repository);
        String mode = forceRefresh ? " (refreshing cache)" : " (" + cacheStatus + ")";
        setStatus("Listing components from: " + repository + mode + "...");

        // Set busy cursor
        frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        setButtonsEnabled(false);

        // Run in background thread to keep UI responsive
        String finalRegex = regex;
        new Thread(() -> {
            try {
                // Get records and format them
                List<RepoRecord> records = service.getRepositoryRecords(repository, finalRegex, forceRefresh);
                String output = service.formatRecordsWithHeaders(records);

                // Update UI on event dispatch thread
                EventQueue.invokeLater(() -> {
                    resultsArea.setText(output);
                    resultsArea.setCaretPosition(0);

                    String newCacheStatus = service.getCacheStatus(repository);
                    setStatus("List completed - " + records.size() + " components - " + newCacheStatus);

                    // Restore normal cursor
                    frame.setCursor(Cursor.getDefaultCursor());
                    setButtonsEnabled(true);
                });
            } catch (Exception e) {
                logger.error("List operation failed", e);
                EventQueue.invokeLater(() -> {
                    resultsArea.setText("ERROR: " + e.getMessage());
                    setStatus("List failed: " + e.getMessage());

                    // Restore normal cursor
                    frame.setCursor(Cursor.getDefaultCursor());
                    setButtonsEnabled(true);
                });
            }
        }).start();
    }

    private void executeDelete() {
        String repository = repositoryField.getText().trim();
        if (repository.isEmpty()) {
            setStatus("ERROR: Repository name is required");
            showErrorDialog("Please enter a repository name.");
            return;
        }

        String regex = regexField.getText().trim();
        if (regex.isEmpty()) {
            regex = null;
        }

        boolean dryRun = dryRunCheckbox.getState();

        // Confirmation dialog for actual deletions
        if (!dryRun) {
            String message = "WARNING: This will permanently delete components from repository: " + repository;
            if (regex != null) {
                message += "\nFilter: " + regex;
            }
            message += "\n\nAre you sure you want to continue?";

            if (!showConfirmDialog(message)) {
                setStatus("Delete operation cancelled by user");
                return;
            }
        }

        String mode = dryRun ? "(DRY RUN)" : "(ACTUAL DELETE)";
        setStatus("Deleting from repository: " + repository + " " + mode + "...");

        // Set busy cursor
        frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        setButtonsEnabled(false);

        // Run in background thread
        String finalRegex = regex;
        new Thread(() -> {
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

                String output = baos.toString();

                // Update UI on event dispatch thread
                EventQueue.invokeLater(() -> {
                    resultsArea.setText(output);
                    resultsArea.setCaretPosition(0);

                    if (output.startsWith("ERROR:")) {
                        setStatus("Delete failed: " + output.substring(7));
                    } else {
                        setStatus("Delete operation completed " + mode);
                    }

                    // Restore normal cursor
                    frame.setCursor(Cursor.getDefaultCursor());
                    setButtonsEnabled(true);
                });
            } catch (Exception e) {
                logger.error("Delete operation failed", e);
                EventQueue.invokeLater(() -> {
                    resultsArea.setText("ERROR: " + e.getMessage());
                    setStatus("Delete failed: " + e.getMessage());

                    // Restore normal cursor
                    frame.setCursor(Cursor.getDefaultCursor());
                    setButtonsEnabled(true);
                });
            }
        }).start();
    }

    private void setStatus(String message) {
        statusLabel.setText(message);
    }

    private void showErrorDialog(String message) {
        Dialog dialog = new Dialog(frame, "Error", true);
        dialog.setLayout(new BorderLayout(10, 10));

        Label messageLabel = new Label(message);
        messageLabel.setAlignment(Label.CENTER);
        dialog.add(messageLabel, BorderLayout.CENTER);

        Button okButton = new Button("OK");
        okButton.addActionListener(e -> dialog.dispose());
        Panel buttonPanel = new Panel();
        buttonPanel.add(okButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
    }

    private boolean showConfirmDialog(String message) {
        Dialog dialog = new Dialog(frame, "Confirm", true);
        dialog.setLayout(new BorderLayout(10, 10));

        TextArea messageArea = new TextArea(message, 6, 50, TextArea.SCROLLBARS_NONE);
        messageArea.setEditable(false);
        dialog.add(messageArea, BorderLayout.CENTER);

        final boolean[] result = {false};

        Panel buttonPanel = new Panel();
        Button yesButton = new Button("Yes");
        yesButton.addActionListener(e -> {
            result[0] = true;
            dialog.dispose();
        });
        Button noButton = new Button("No");
        noButton.addActionListener(e -> {
            result[0] = false;
            dialog.dispose();
        });
        buttonPanel.add(yesButton);
        buttonPanel.add(noButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);

        return result[0];
    }

    private void setButtonsEnabled(boolean enabled) {
        listButton.setEnabled(enabled);
        refreshButton.setEnabled(enabled);
        deleteButton.setEnabled(enabled);
        clearButton.setEnabled(enabled);
    }
}

package org.flossware.jnexus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.event.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

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
                JNexusAWT app = new JNexusAWT();
                app.createAndShowGUI();
            } catch (Exception e) {
                LoggerFactory.getLogger(JNexusAWT.class).error("Failed to start AWT UI: {}", e.getMessage(), e);

                // Show error dialog
                Dialog errorDialog = new Dialog((Frame) null, "Initialization Error", true);
                errorDialog.setLayout(new BorderLayout());

                TextArea errorText = new TextArea(
                    "Failed to initialize Nexus client:\n" + e.getMessage() +
                    "\n\nPlease configure credentials:\n" +
                    "  Set environment variables or\n" +
                    "  create ~/.flossware/nexus/nexus.properties",
                    8, 50, TextArea.SCROLLBARS_VERTICAL_ONLY);
                errorText.setEditable(false);
                errorDialog.add(errorText, BorderLayout.CENTER);

                Button okButton = new Button("OK");
                okButton.addActionListener(e2 -> System.exit(1));
                Panel buttonPanel = new Panel();
                buttonPanel.add(okButton);
                errorDialog.add(buttonPanel, BorderLayout.SOUTH);

                errorDialog.pack();
                errorDialog.setLocationRelativeTo(null);
                errorDialog.setVisible(true);
            }
        });
    }

    public JNexusAWT() throws Exception {
        // Initialize Nexus client
        credentials = new Credentials();
        client = new NexusClient(credentials);
        service = new NexusService(client);
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
        panel.add(repositoryField, gbc);

        // Regex filter label and field
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.0;
        panel.add(new Label("Regex Filter:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        regexField = new TextField(credentials.getDefaultRegex(), 40);
        panel.add(regexField, gbc);

        // Dry run checkbox
        gbc.gridx = 1;
        gbc.gridy = 3;
        dryRunCheckbox = new Checkbox("Dry Run (preview only, no actual deletion)");
        dryRunCheckbox.setState(credentials.isDefaultDryRun());
        panel.add(dryRunCheckbox, gbc);

        // Buttons panel
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
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

        // Run in background thread to keep UI responsive
        String finalRegex = regex;
        new Thread(() -> {
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

                String output = baos.toString();

                // Update UI on event dispatch thread
                EventQueue.invokeLater(() -> {
                    resultsArea.setText(output);
                    resultsArea.setCaretPosition(0);

                    if (output.startsWith("ERROR:")) {
                        setStatus("List failed: " + output.substring(7));
                    } else {
                        String newCacheStatus = service.getCacheStatus(repository);
                        setStatus("List completed - " + newCacheStatus);
                    }
                });
            } catch (Exception e) {
                logger.error("List operation failed", e);
                EventQueue.invokeLater(() -> {
                    resultsArea.setText("ERROR: " + e.getMessage());
                    setStatus("List failed: " + e.getMessage());
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
                });
            } catch (Exception e) {
                logger.error("Delete operation failed", e);
                EventQueue.invokeLater(() -> {
                    resultsArea.setText("ERROR: " + e.getMessage());
                    setStatus("Delete failed: " + e.getMessage());
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
}

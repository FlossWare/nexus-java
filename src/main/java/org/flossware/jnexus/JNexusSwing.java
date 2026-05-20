package org.flossware.jnexus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
    private JComboBox<String> repositoryComboBox;
    private JTextField regexField;
    private JCheckBox dryRunCheckbox;
    private JTable resultsTable;
    private DefaultTableModel tableModel;
    private JLabel statusLabel;
    private JButton listButton;
    private JButton refreshButton;
    private JButton deleteButton;
    private JButton deleteSelectedButton;
    private JButton clearButton;
    private JButton statsButton;

    // Advanced filter components
    private JTextField minSizeField;
    private JTextField maxSizeField;
    private JTextField createdAfterField;
    private JTextField createdBeforeField;
    private JTextField extensionField;
    private JPanel advancedFiltersPanel;
    private JButton toggleFiltersButton;

    // Services
    private NexusClient client;
    private NexusService service;
    private Credentials credentials;

    // Current data
    private List<ComponentMetadata> currentComponents = new ArrayList<>();
    private String currentRepository = "";

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

        // Repository dropdown selector
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        panel.add(new JLabel("Repository:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;

        // Build repository list with "All" option
        java.util.List<String> repoOptions = new java.util.ArrayList<>();
        repoOptions.add("All");
        if (!credentials.getRepositories().isEmpty()) {
            repoOptions.addAll(credentials.getRepositories());
        }

        repositoryComboBox = new JComboBox<>(repoOptions.toArray(new String[0]));
        repositoryComboBox.setToolTipText("Select repository (All = search all repositories)");

        // Set default selection
        if (!credentials.getDefaultRepository().isEmpty() && credentials.getRepositories().contains(credentials.getDefaultRepository())) {
            repositoryComboBox.setSelectedItem(credentials.getDefaultRepository());
        } else {
            repositoryComboBox.setSelectedIndex(0); // All
        }

        panel.add(repositoryComboBox, gbc);

        // Regex filter label and field
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        panel.add(new JLabel("Regex Filter:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        regexField = new JTextField(credentials.getDefaultRegex(), 30);
        regexField.setToolTipText("Optional regex pattern to filter components (e.g., .*SNAPSHOT.*)");
        regexField.addActionListener(e -> executeList(false)); // Enter triggers List
        panel.add(regexField, gbc);

        // Dry run checkbox
        gbc.gridx = 1;
        gbc.gridy = 2;
        dryRunCheckbox = new JCheckBox("Dry Run (preview only, no actual deletion)");
        dryRunCheckbox.setSelected(credentials.isDefaultDryRun());
        panel.add(dryRunCheckbox, gbc);

        // Advanced filters toggle button
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.WEST;
        toggleFiltersButton = new JButton("▶ Advanced Filters");
        toggleFiltersButton.setBorderPainted(false);
        toggleFiltersButton.setContentAreaFilled(false);
        toggleFiltersButton.setFocusPainted(false);
        toggleFiltersButton.setForeground(Color.BLUE);
        toggleFiltersButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        toggleFiltersButton.addActionListener(e -> toggleAdvancedFilters());
        panel.add(toggleFiltersButton, gbc);

        // Advanced filters panel (initially hidden)
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        advancedFiltersPanel = createAdvancedFiltersPanel();
        advancedFiltersPanel.setVisible(false);
        panel.add(advancedFiltersPanel, gbc);

        // Nexus URL display (read-only)
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.0;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel("Nexus URL:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JTextField nexusUrlField = new JTextField(credentials.getUrl(), 30);
        nexusUrlField.setEditable(false);
        nexusUrlField.setBackground(panel.getBackground());
        nexusUrlField.setBorder(BorderFactory.createEmptyBorder());
        nexusUrlField.setFont(nexusUrlField.getFont().deriveFont(Font.ITALIC));
        panel.add(nexusUrlField, gbc);

        // Property file display (read-only)
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.weightx = 0.0;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel("Config File:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        String configFile = credentials.getProfile() == null
            ? "~/.flossware/nexus/nexus.properties"
            : "~/.flossware/nexus/nexus-" + credentials.getProfile() + ".properties";
        JTextField configFileField = new JTextField(configFile, 30);
        configFileField.setEditable(false);
        configFileField.setBackground(panel.getBackground());
        configFileField.setBorder(BorderFactory.createEmptyBorder());
        configFileField.setFont(configFileField.getFont().deriveFont(Font.ITALIC));
        panel.add(configFileField, gbc);

        // Buttons panel
        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(createButtonPanel(), gbc);

        return panel;
    }

    private JPanel createAdvancedFiltersPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Advanced Filters"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Min Size
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        panel.add(new JLabel("Min Size (bytes):"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        minSizeField = new JTextField(15);
        minSizeField.setToolTipText("Minimum file size in bytes (e.g., 1048576 for 1 MB)");
        panel.add(minSizeField, gbc);

        // Max Size
        gbc.gridx = 2;
        gbc.weightx = 0.0;
        panel.add(new JLabel("Max Size (bytes):"), gbc);

        gbc.gridx = 3;
        gbc.weightx = 1.0;
        maxSizeField = new JTextField(15);
        maxSizeField.setToolTipText("Maximum file size in bytes (e.g., 10485760 for 10 MB)");
        panel.add(maxSizeField, gbc);

        // Created After
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        panel.add(new JLabel("Created After:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        createdAfterField = new JTextField(15);
        createdAfterField.setToolTipText("ISO format: 2024-01-01T00:00:00Z");
        panel.add(createdAfterField, gbc);

        // Created Before
        gbc.gridx = 2;
        gbc.weightx = 0.0;
        panel.add(new JLabel("Created Before:"), gbc);

        gbc.gridx = 3;
        gbc.weightx = 1.0;
        createdBeforeField = new JTextField(15);
        createdBeforeField.setToolTipText("ISO format: 2024-01-01T00:00:00Z");
        panel.add(createdBeforeField, gbc);

        // Extension
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.0;
        panel.add(new JLabel("File Extension:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.gridwidth = 3;
        extensionField = new JTextField(15);
        extensionField.setToolTipText("File extension including dot (e.g., .jar, .war, .zip)");
        panel.add(extensionField, gbc);

        return panel;
    }

    private void toggleAdvancedFilters() {
        boolean visible = advancedFiltersPanel.isVisible();
        advancedFiltersPanel.setVisible(!visible);
        toggleFiltersButton.setText(visible ? "▶ Advanced Filters" : "▼ Advanced Filters");
        frame.pack(); // Resize window to fit content
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

        deleteButton = new JButton("Delete All");
        deleteButton.setToolTipText("Delete all components matching filter");
        deleteButton.addActionListener(e -> executeDelete());
        panel.add(deleteButton);

        deleteSelectedButton = new JButton("Delete Selected");
        deleteSelectedButton.setToolTipText("Delete selected rows from table");
        deleteSelectedButton.addActionListener(e -> executeDeleteSelected());
        deleteSelectedButton.setVisible(false); // Hidden until rows are selected
        panel.add(deleteSelectedButton);

        clearButton = new JButton("Clear Results");
        clearButton.addActionListener(e -> {
            tableModel.setRowCount(0);
            currentComponents.clear();
            setStatus("Results cleared", false);
        });
        panel.add(clearButton);

        statsButton = new JButton("Statistics");
        statsButton.setToolTipText("Show repository statistics for current results");
        statsButton.addActionListener(e -> showStatisticsDialog());
        panel.add(statsButton);

        JButton quitButton = new JButton("Quit");
        quitButton.addActionListener(e -> System.exit(0));
        panel.add(quitButton);

        return panel;
    }

    private JPanel createResultsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Results"));

        // Create table model with columns
        String[] columnNames = {"ID", "File Size (Bytes)", "File Size (MB)", "File Size (GB)", "Created", "Content Type", "Path"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table read-only
            }
        };

        resultsTable = new JTable(tableModel);
        resultsTable.setFont(new Font("Monospaced", Font.PLAIN, 12));

        // Create custom sorter with numeric comparators for size columns
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);

        // Column 1: File Size (Bytes) - parse comma-formatted numbers
        sorter.setComparator(1, (String s1, String s2) -> {
            try {
                long n1 = numberFormat.parse(s1.replaceAll("[^0-9,]", "")).longValue();
                long n2 = numberFormat.parse(s2.replaceAll("[^0-9,]", "")).longValue();
                return Long.compare(n1, n2);
            } catch (Exception e) {
                return s1.compareTo(s2); // Fallback to string comparison
            }
        });

        // Column 2: File Size (MB) - parse decimal numbers
        sorter.setComparator(2, (String s1, String s2) -> {
            try {
                double d1 = Double.parseDouble(s1);
                double d2 = Double.parseDouble(s2);
                return Double.compare(d1, d2);
            } catch (Exception e) {
                return s1.compareTo(s2); // Fallback to string comparison
            }
        });

        // Column 3: File Size (GB) - parse decimal numbers
        sorter.setComparator(3, (String s1, String s2) -> {
            try {
                double d1 = Double.parseDouble(s1);
                double d2 = Double.parseDouble(s2);
                return Double.compare(d1, d2);
            } catch (Exception e) {
                return s1.compareTo(s2); // Fallback to string comparison
            }
        });

        resultsTable.setRowSorter(sorter);
        resultsTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        resultsTable.getTableHeader().setReorderingAllowed(false);

        // Set column widths
        resultsTable.getColumnModel().getColumn(0).setPreferredWidth(200); // ID
        resultsTable.getColumnModel().getColumn(1).setPreferredWidth(130); // File Size (Bytes)
        resultsTable.getColumnModel().getColumn(2).setPreferredWidth(90);  // File Size (MB)
        resultsTable.getColumnModel().getColumn(3).setPreferredWidth(90);  // File Size (GB)
        resultsTable.getColumnModel().getColumn(4).setPreferredWidth(150); // Created
        resultsTable.getColumnModel().getColumn(5).setPreferredWidth(150); // Content Type
        resultsTable.getColumnModel().getColumn(6).setPreferredWidth(350); // Path

        // Add selection listener to update status and button visibility
        resultsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateSelectionStatus();
            }
        });

        // Add double-click listener for component details
        resultsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = resultsTable.getSelectedRow();
                    if (row >= 0) {
                        showComponentDetailsDialog(row);
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(resultsTable);
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
        String selected = (String) repositoryComboBox.getSelectedItem();
        if (selected == null || selected.isEmpty()) {
            setStatus("ERROR: Repository selection is required", true);
            JOptionPane.showMessageDialog(frame,
                "Please select a repository.",
                "Validation Error",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        // "All" means empty repository (search all)
        String repository = "All".equals(selected) ? "" : selected;
        currentRepository = repository;

        String regex = regexField.getText().trim();
        if (regex.isEmpty()) {
            regex = null;
        }

        setStatus("Listing components from: " + repository + "...", false);

        // Set busy cursor
        frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        setButtonsEnabled(false);

        // Run in background thread to keep UI responsive
        String finalRegex = regex;
        new SwingWorker<List<ComponentMetadata>, Void>() {
            @Override
            protected List<ComponentMetadata> doInBackground() {
                try {
                    // Build search criteria
                    SearchCriteria.Builder builder = SearchCriteria.builder()
                        .repository(repository)
                        .regexFilter(finalRegex);

                    // Add advanced filters if provided
                    String minSizeText = minSizeField.getText().trim();
                    if (!minSizeText.isEmpty()) {
                        builder.minSize(Long.parseLong(minSizeText));
                    }

                    String maxSizeText = maxSizeField.getText().trim();
                    if (!maxSizeText.isEmpty()) {
                        builder.maxSize(Long.parseLong(maxSizeText));
                    }

                    String createdAfterText = createdAfterField.getText().trim();
                    if (!createdAfterText.isEmpty()) {
                        builder.createdAfter(Instant.parse(createdAfterText));
                    }

                    String createdBeforeText = createdBeforeField.getText().trim();
                    if (!createdBeforeText.isEmpty()) {
                        builder.createdBefore(Instant.parse(createdBeforeText));
                    }

                    String extensionText = extensionField.getText().trim();
                    if (!extensionText.isEmpty()) {
                        builder.fileExtension(extensionText);
                    }

                    SearchCriteria criteria = builder.build();
                    return service.searchComponents(criteria, forceRefresh);
                } catch (Exception e) {
                    logger.error("List operation failed", e);
                    return null;
                }
            }

            @Override
            protected void done() {
                try {
                    List<ComponentMetadata> components = get();

                    if (components == null) {
                        setStatus("List failed - check logs for details", true);
                        return;
                    }

                    // Store current results
                    currentComponents = new ArrayList<>(components);

                    // Clear existing rows
                    tableModel.setRowCount(0);

                    // Populate table
                    NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
                    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                    long totalBytes = 0;

                    for (ComponentMetadata component : components) {
                        double sizeMB = component.fileSize() / 1024.0 / 1024.0;
                        double sizeGB = component.fileSize() / 1024.0 / 1024.0 / 1024.0;

                        String createdDate = component.createdDate() != null
                            ? dateFormat.format(Date.from(component.createdDate()))
                            : "N/A";

                        String contentType = component.contentType() != null
                            ? component.contentType()
                            : "unknown";

                        tableModel.addRow(new Object[]{
                            component.id(),
                            numberFormat.format(component.fileSize()),
                            String.format("%.2f", sizeMB),
                            String.format("%.4f", sizeGB),
                            createdDate,
                            contentType,
                            component.path()
                        });
                        totalBytes += component.fileSize();
                    }

                    // Update status with grand total
                    double totalMB = totalBytes / 1024.0 / 1024.0;
                    double totalGB = totalBytes / 1024.0 / 1024.0 / 1024.0;
                    setStatus(String.format("Total: %d component(s) - %s bytes (%.2f MB / %.4f GB)",
                        components.size(), numberFormat.format(totalBytes), totalMB, totalGB), false);

                    // Trigger selection status update to show grand total
                    updateSelectionStatus();
                } catch (Exception e) {
                    setStatus("List failed: " + e.getMessage(), true);
                } finally {
                    // Restore normal cursor
                    frame.setCursor(Cursor.getDefaultCursor());
                    setButtonsEnabled(true);
                }
            }
        }.execute();
    }

    private void executeDelete() {
        String selected = (String) repositoryComboBox.getSelectedItem();
        if (selected == null || selected.isEmpty()) {
            setStatus("ERROR: Repository selection is required", true);
            JOptionPane.showMessageDialog(frame,
                "Please select a repository.",
                "Validation Error",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        // "All" means empty repository (search all)
        String repository = "All".equals(selected) ? "" : selected;

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

        // Set busy cursor
        frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        setButtonsEnabled(false);

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

                    // Show result in a dialog
                    JTextArea textArea = new JTextArea(output);
                    textArea.setEditable(false);
                    textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
                    JScrollPane scrollPane = new JScrollPane(textArea);
                    scrollPane.setPreferredSize(new Dimension(600, 400));

                    if (output.startsWith("ERROR:")) {
                        JOptionPane.showMessageDialog(frame,
                            scrollPane,
                            "Delete Failed",
                            JOptionPane.ERROR_MESSAGE);
                        setStatus("Delete failed: " + output.substring(7), true);
                    } else {
                        JOptionPane.showMessageDialog(frame,
                            scrollPane,
                            "Delete Results - " + mode,
                            JOptionPane.INFORMATION_MESSAGE);
                        setStatus("Delete operation completed " + mode, false);

                        // Clear table after successful deletion (not in dry-run mode)
                        if (!dryRun) {
                            tableModel.setRowCount(0);
                        }
                    }
                } catch (Exception e) {
                    setStatus("Delete failed: " + e.getMessage(), true);
                } finally {
                    // Restore normal cursor
                    frame.setCursor(Cursor.getDefaultCursor());
                    setButtonsEnabled(true);
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

    private void executeDeleteSelected() {
        int[] selectedRows = resultsTable.getSelectedRows();
        if (selectedRows.length == 0) {
            JOptionPane.showMessageDialog(frame,
                "Please select one or more rows to delete.",
                "No Selection",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Get IDs of selected rows (convert view indices to model indices first)
        java.util.ArrayList<String> idsToDeleteList = new java.util.ArrayList<>();
        for (int i = 0; i < selectedRows.length; i++) {
            int modelRow = resultsTable.convertRowIndexToModel(selectedRows[i]);
            String id = (String) tableModel.getValueAt(modelRow, 0);
            if (id != null && !id.isEmpty()) {
                idsToDeleteList.add(id);
            }
        }

        String message = "WARNING: This will permanently delete " + idsToDeleteList.size() +
            " selected component(s) from the repository.\n\nAre you sure you want to continue?";

        int choice = JOptionPane.showConfirmDialog(frame,
            message,
            "Confirm Deletion",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

        if (choice != JOptionPane.YES_OPTION) {
            setStatus("Delete operation cancelled by user", false);
            return;
        }

        setStatus("Deleting " + idsToDeleteList.size() + " selected components...", false);

        // Set busy cursor
        frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        setButtonsEnabled(false);

        String[] idsToDelete = idsToDeleteList.toArray(new String[0]);

        // Run in background thread
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                StringBuilder output = new StringBuilder();
                int deleted = 0;

                for (String id : idsToDelete) {
                    try {
                        client.deleteComponent(id);
                        deleted++;
                        output.append("Deleted ID: ").append(id).append("\n");
                        logger.info("Deleted component: {}", id);
                    } catch (Exception e) {
                        output.append("Failed to delete ID ").append(id).append(": ")
                            .append(e.getMessage()).append("\n");
                        logger.error("Failed to delete {}: {}", id, e.getMessage());
                    }
                }

                // Clear cache after deletion
                String selected = (String) repositoryComboBox.getSelectedItem();
                if (selected != null && !selected.isEmpty() && !"All".equals(selected)) {
                    client.clearCache(selected);
                }

                return "Deleted " + deleted + " of " + idsToDelete.length + " components\n\n" + output;
            }

            @Override
            protected void done() {
                try {
                    String result = get();

                    // Show result in a dialog
                    JTextArea textArea = new JTextArea(result);
                    textArea.setEditable(false);
                    textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
                    JScrollPane scrollPane = new JScrollPane(textArea);
                    scrollPane.setPreferredSize(new Dimension(600, 300));

                    JOptionPane.showMessageDialog(frame,
                        scrollPane,
                        "Delete Results",
                        JOptionPane.INFORMATION_MESSAGE);

                    // Remove deleted rows from table (skip summary row)
                    for (int i = selectedRows.length - 1; i >= 0; i--) {
                        int modelRow = resultsTable.convertRowIndexToModel(selectedRows[i]);
                        String id = (String) tableModel.getValueAt(modelRow, 0);
                        if (id != null && !id.startsWith("TOTAL:")) {
                            tableModel.removeRow(modelRow);
                        }
                    }

                    // Recalculate and update summary row
                    updateSummaryRow();

                    setStatus("Delete operation completed", false);
                } catch (Exception e) {
                    setStatus("Delete failed: " + e.getMessage(), true);
                } finally {
                    // Restore normal cursor
                    frame.setCursor(Cursor.getDefaultCursor());
                    setButtonsEnabled(true);
                }
            }
        }.execute();
    }

    private void updateSummaryRow() {
        // Find and remove existing summary row if present
        for (int i = tableModel.getRowCount() - 1; i >= 0; i--) {
            String id = (String) tableModel.getValueAt(i, 0);
            if (id != null && id.startsWith("TOTAL:")) {
                tableModel.removeRow(i);
                break;
            }
        }

        // Recalculate totals from remaining rows
        int count = 0;
        long totalBytes = 0;
        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String sizeStr = (String) tableModel.getValueAt(i, 1);
            if (sizeStr != null && !sizeStr.isEmpty()) {
                try {
                    // Remove commas and parse
                    long size = numberFormat.parse(sizeStr.replaceAll("[^0-9,]", "")).longValue();
                    totalBytes += size;
                    count++;
                } catch (Exception e) {
                    logger.warn("Failed to parse size: {}", sizeStr);
                }
            }
        }

        // Add new summary row
        if (count > 0) {
            double totalMB = totalBytes / 1024.0 / 1024.0;
            tableModel.addRow(new Object[]{
                "TOTAL: " + count + " components",
                numberFormat.format(totalBytes),
                String.format("%.2f", totalMB),
                ""
            });
        }
    }

    private void updateSelectionStatus() {
        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);

        // Calculate grand total from all rows in table
        int totalComponents = tableModel.getRowCount();
        long grandTotalBytes = 0;
        for (int i = 0; i < totalComponents; i++) {
            String sizeStr = (String) tableModel.getValueAt(i, 1);
            try {
                long size = numberFormat.parse(sizeStr.replaceAll("[^0-9,]", "")).longValue();
                grandTotalBytes += size;
            } catch (Exception e) {
                logger.warn("Failed to parse size: {}", sizeStr);
            }
        }

        int[] selectedRows = resultsTable.getSelectedRows();

        // If no rows selected, just show grand total
        if (selectedRows.length == 0) {
            deleteSelectedButton.setVisible(false);
            if (totalComponents > 0) {
                double grandTotalMB = grandTotalBytes / 1024.0 / 1024.0;
                double grandTotalGB = grandTotalBytes / 1024.0 / 1024.0 / 1024.0;
                setStatus(String.format("Total: %d component(s) - %s bytes (%.2f MB / %.4f GB)",
                    totalComponents, numberFormat.format(grandTotalBytes), grandTotalMB, grandTotalGB), false);
            }
            return;
        }

        // Calculate selected total
        long selectedBytes = 0;
        for (int viewRow : selectedRows) {
            int modelRow = resultsTable.convertRowIndexToModel(viewRow);
            String sizeStr = (String) tableModel.getValueAt(modelRow, 1);
            try {
                long size = numberFormat.parse(sizeStr.replaceAll("[^0-9,]", "")).longValue();
                selectedBytes += size;
            } catch (Exception e) {
                logger.warn("Failed to parse size: {}", sizeStr);
            }
        }

        // Show both selected and grand total
        deleteSelectedButton.setVisible(true);
        double selectedMB = selectedBytes / 1024.0 / 1024.0;
        double selectedGB = selectedBytes / 1024.0 / 1024.0 / 1024.0;
        double grandTotalMB = grandTotalBytes / 1024.0 / 1024.0;
        double grandTotalGB = grandTotalBytes / 1024.0 / 1024.0 / 1024.0;
        setStatus(String.format("Selected: %d component(s) - %s bytes (%.2f MB / %.4f GB) | Total: %d component(s) - %s bytes (%.2f MB / %.4f GB)",
            selectedRows.length, numberFormat.format(selectedBytes), selectedMB, selectedGB,
            totalComponents, numberFormat.format(grandTotalBytes), grandTotalMB, grandTotalGB), false);
    }

    private void showComponentDetailsDialog(int viewRow) {
        int modelRow = resultsTable.convertRowIndexToModel(viewRow);
        if (modelRow < 0 || modelRow >= currentComponents.size()) {
            return;
        }

        ComponentMetadata component = currentComponents.get(modelRow);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        // ID
        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.weightx = 0.0;
        panel.add(new JLabel("ID:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        JTextField idField = new JTextField(component.id());
        idField.setEditable(false);
        panel.add(idField, gbc);

        // Path
        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.weightx = 0.0;
        panel.add(new JLabel("Path:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        JTextField pathField = new JTextField(component.path());
        pathField.setEditable(false);
        panel.add(pathField, gbc);

        // File Size
        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.weightx = 0.0;
        panel.add(new JLabel("File Size:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
        double sizeMB = component.fileSize() / 1024.0 / 1024.0;
        double sizeGB = component.fileSize() / 1024.0 / 1024.0 / 1024.0;
        String sizeText = String.format("%s bytes (%.2f MB / %.4f GB)",
            numberFormat.format(component.fileSize()), sizeMB, sizeGB);
        JTextField sizeField = new JTextField(sizeText);
        sizeField.setEditable(false);
        panel.add(sizeField, gbc);

        // Content Type
        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.weightx = 0.0;
        panel.add(new JLabel("Content Type:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        JTextField typeField = new JTextField(component.contentType() != null ? component.contentType() : "unknown");
        typeField.setEditable(false);
        panel.add(typeField, gbc);

        // Format
        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.weightx = 0.0;
        panel.add(new JLabel("Format:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        JTextField formatField = new JTextField(component.format() != null ? component.format() : "unknown");
        formatField.setEditable(false);
        panel.add(formatField, gbc);

        // Created Date
        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.weightx = 0.0;
        panel.add(new JLabel("Created:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
        String createdText = component.createdDate() != null
            ? dateFormat.format(Date.from(component.createdDate()))
            : "N/A";
        JTextField createdField = new JTextField(createdText);
        createdField.setEditable(false);
        panel.add(createdField, gbc);

        // Last Modified
        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.weightx = 0.0;
        panel.add(new JLabel("Last Modified:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        String modifiedText = component.lastModified() != null
            ? dateFormat.format(Date.from(component.lastModified()))
            : "N/A";
        JTextField modifiedField = new JTextField(modifiedText);
        modifiedField.setEditable(false);
        panel.add(modifiedField, gbc);

        // Checksum
        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.weightx = 0.0;
        panel.add(new JLabel("Checksum:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        JTextField checksumField = new JTextField(component.checksum() != null ? component.checksum() : "N/A");
        checksumField.setEditable(false);
        panel.add(checksumField, gbc);

        JOptionPane.showMessageDialog(frame,
            panel,
            "Component Details",
            JOptionPane.INFORMATION_MESSAGE);
    }

    private void showStatisticsDialog() {
        if (currentComponents.isEmpty()) {
            JOptionPane.showMessageDialog(frame,
                "No components loaded. Please run List or Refresh first.",
                "No Data",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Calculate statistics
        RepositoryStats stats = service.calculateStatistics(currentRepository, currentComponents);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Overview", createOverviewPanel(stats));
        tabs.addTab("Size Distribution", createSizeDistributionPanel(stats));
        tabs.addTab("File Types", createFileTypePanel(stats));
        tabs.addTab("Age Distribution", createAgePanel(stats));
        tabs.addTab("Largest Components", createLargestComponentsPanel(stats));

        JDialog dialog = new JDialog(frame, "Repository Statistics: " + stats.repository(), true);
        dialog.add(tabs);
        dialog.setSize(900, 600);
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
    }

    private JPanel createOverviewPanel(RepositoryStats stats) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
        int row = 0;

        // Total Components
        gbc.gridx = 0;
        gbc.gridy = row++;
        panel.add(new JLabel("Total Components:"), gbc);
        gbc.gridx = 1;
        panel.add(new JLabel(numberFormat.format(stats.totalComponents())), gbc);

        // Total Size
        gbc.gridx = 0;
        gbc.gridy = row++;
        panel.add(new JLabel("Total Size:"), gbc);
        gbc.gridx = 1;
        String totalSizeText = String.format("%s bytes (%.2f MB / %.4f GB)",
            numberFormat.format(stats.totalSize()),
            stats.getTotalSizeMB(),
            stats.getTotalSizeGB());
        panel.add(new JLabel(totalSizeText), gbc);

        // Average Size
        gbc.gridx = 0;
        gbc.gridy = row++;
        panel.add(new JLabel("Average Size:"), gbc);
        gbc.gridx = 1;
        String avgSizeText = String.format("%s bytes (%.2f MB)",
            numberFormat.format(stats.averageSize()),
            stats.getAverageSizeMB());
        panel.add(new JLabel(avgSizeText), gbc);

        // Median Size
        gbc.gridx = 0;
        gbc.gridy = row++;
        panel.add(new JLabel("Median Size:"), gbc);
        gbc.gridx = 1;
        String medianSizeText = String.format("%s bytes (%.2f MB)",
            numberFormat.format(stats.medianSize()),
            stats.getMedianSizeMB());
        panel.add(new JLabel(medianSizeText), gbc);

        return panel;
    }

    private JPanel createSizeDistributionPanel(RepositoryStats stats) {
        String[] columnNames = {"Size Range", "Count", "Percentage"};
        Object[][] data = new Object[stats.sizeDistribution().size()][3];

        int i = 0;
        for (Map.Entry<String, Integer> entry : stats.sizeDistribution().entrySet()) {
            double percentage = (entry.getValue() * 100.0) / stats.totalComponents();
            data[i][0] = entry.getKey();
            data[i][1] = NumberFormat.getNumberInstance(Locale.US).format(entry.getValue());
            data[i][2] = String.format("%.1f%%", percentage);
            i++;
        }

        JTable table = new JTable(data, columnNames);
        table.setEnabled(false);
        JScrollPane scrollPane = new JScrollPane(table);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createFileTypePanel(RepositoryStats stats) {
        String[] columnNames = {"File Extension", "Total Size", "Percentage"};
        Object[][] data = new Object[stats.fileTypeBreakdown().size()][3];

        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
        int i = 0;
        for (Map.Entry<String, Long> entry : stats.fileTypeBreakdown().entrySet()) {
            double percentage = (entry.getValue() * 100.0) / stats.totalSize();
            double sizeMB = entry.getValue() / 1024.0 / 1024.0;
            data[i][0] = entry.getKey();
            data[i][1] = String.format("%.2f MB", sizeMB);
            data[i][2] = String.format("%.1f%%", percentage);
            i++;
        }

        JTable table = new JTable(data, columnNames);
        table.setEnabled(false);
        JScrollPane scrollPane = new JScrollPane(table);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createAgePanel(RepositoryStats stats) {
        String[] columnNames = {"Age Range", "Count"};
        Object[][] data = new Object[stats.ageDistribution().size()][2];

        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
        int i = 0;
        for (Map.Entry<String, Integer> entry : stats.ageDistribution().entrySet()) {
            data[i][0] = entry.getKey();
            data[i][1] = numberFormat.format(entry.getValue());
            i++;
        }

        JTable table = new JTable(data, columnNames);
        table.setEnabled(false);
        JScrollPane scrollPane = new JScrollPane(table);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createLargestComponentsPanel(RepositoryStats stats) {
        String[] columnNames = {"ID", "Size (MB)", "Path"};
        List<ComponentMetadata> largest = stats.largestComponents();
        Object[][] data = new Object[Math.min(20, largest.size())][3];

        for (int i = 0; i < Math.min(20, largest.size()); i++) {
            ComponentMetadata component = largest.get(i);
            double sizeMB = component.fileSize() / 1024.0 / 1024.0;
            data[i][0] = component.id();
            data[i][1] = String.format("%.2f", sizeMB);
            data[i][2] = component.path();
        }

        JTable table = new JTable(data, columnNames);
        table.setEnabled(false);
        JScrollPane scrollPane = new JScrollPane(table);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private void setButtonsEnabled(boolean enabled) {
        listButton.setEnabled(enabled);
        refreshButton.setEnabled(enabled);
        deleteButton.setEnabled(enabled);
        deleteSelectedButton.setEnabled(enabled);
        clearButton.setEnabled(enabled);
        statsButton.setEnabled(enabled);
    }
}


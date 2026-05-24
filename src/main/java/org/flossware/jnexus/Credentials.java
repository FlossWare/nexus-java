package org.flossware.jnexus;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Manages Nexus repository credentials and configuration.
 * <p>
 * This class reads Nexus credentials from multiple sources in the following priority:
 * </p>
 * <ol>
 *   <li>Environment variables: NEXUS_URL, NEXUS_USER, NEXUS_PASSWORD</li>
 *   <li>Properties file (profile-based):
 *     <ul>
 *       <li>If NEXUS_PROFILE is set: ~/.flossware/nexus/nexus-{profile}.properties</li>
 *       <li>Otherwise: ~/.flossware/nexus/nexus.properties (default)</li>
 *     </ul>
 *   </li>
 * </ol>
 * <p>
 * All three credentials (URL, user, password) must be provided or an
 * {@link IllegalStateException} will be thrown during construction.
 * </p>
 * <p>
 * <strong>Profile Support:</strong> Use NEXUS_PROFILE environment variable to switch
 * between different configurations. For example:
 * </p>
 * <ul>
 *   <li>NEXUS_PROFILE=dev → loads nexus-dev.properties</li>
 *   <li>NEXUS_PROFILE=prod → loads nexus-prod.properties</li>
 *   <li>NEXUS_PROFILE=staging → loads nexus-staging.properties</li>
 * </ul>
 *
 * @author sfloess
 * @since 1.0
 */
public class Credentials {
    private final String url;
    private final String user;
    private final String password;

    // Optional UI defaults
    private final String defaultRepository;
    private final String defaultRegex;
    private final boolean defaultDryRun;

    // Optional repository list
    private final List<String> repositories;

    // Optional HTTP configuration
    private final int httpTimeoutSeconds;
    private final int maxRetries;
    private final long initialRetryDelayMs;

    // Optional logging configuration
    private final String logLevel;

    // Profile used for loading configuration
    private final String profile;

    /**
     * Constructs a new Credentials instance by loading configuration from
     * environment variables or properties file.
     * <p>
     * Environment variables take precedence over properties file values.
     * If any required credential is missing or blank, an exception is thrown.
     * Profile is determined from NEXUS_PROFILE environment variable.
     * </p>
     *
     * @throws IllegalStateException if any required credential (URL, user, or password)
     *                               is not configured or is blank
     */
    public Credentials() {
        this(System.getenv("NEXUS_PROFILE"));
    }

    /**
     * Constructs a new Credentials instance with explicit values.
     * <p>
     * Used when credentials are collected interactively (e.g., from a dialog).
     * Does not load from environment variables or properties files.
     * </p>
     *
     * @param url the Nexus server URL
     * @param user the Nexus username
     * @param password the Nexus password
     * @param repositoriesStr comma-separated list of repositories (optional, can be null or empty)
     * @throws IllegalArgumentException if any required credential (URL, user, or password) is null or blank
     */
    public Credentials(String url, String user, String password, String repositoriesStr) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL cannot be null or blank");
        }
        if (user == null || user.isBlank()) {
            throw new IllegalArgumentException("Username cannot be null or blank");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password cannot be null or blank");
        }

        // Validate URL format and security
        validateUrl(url);

        this.url = url;
        this.user = user;
        this.password = password;
        this.profile = null;

        // Parse repositories if provided
        if (repositoriesStr != null && !repositoriesStr.isBlank()) {
            this.repositories = Arrays.stream(repositoriesStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        } else {
            this.repositories = Collections.emptyList();
        }

        // Use defaults for optional fields
        // If repositories were provided, use the first one as the default
        this.defaultRepository = repositories.isEmpty() ? "" : repositories.get(0);
        this.defaultRegex = "";
        this.defaultDryRun = true;
        this.httpTimeoutSeconds = 30;
        this.maxRetries = 3;
        this.initialRetryDelayMs = 1000;
        this.logLevel = "INFO";

        // Set system property for logback
        System.setProperty("nexus.log.level", this.logLevel);
    }

    /**
     * Constructs a new Credentials instance with a specific profile.
     * <p>
     * Environment variables take precedence over properties file values.
     * If any required credential is missing or blank, an exception is thrown.
     * </p>
     *
     * @param profile the profile name to use (null or empty for default)
     * @throws IllegalStateException if any required credential (URL, user, or password)
     *                               is not configured or is blank
     */
    public Credentials(String profile) {
        this.profile = (profile != null && !profile.isBlank()) ? profile : null;
        String url = System.getenv("NEXUS_URL");
        String user = System.getenv("NEXUS_USER");
        String password = System.getenv("NEXUS_PASSWORD");

        // Load from properties file if any required field is missing
        Properties props = new Properties();
        if (url == null || user == null || password == null) {
            props = loadPropertiesFile();
            url = props.getProperty("nexus.url", url);
            user = props.getProperty("nexus.user", user);
            password = props.getProperty("nexus.password", password);
        } else {
            // Even if env vars are set, load properties for optional UI defaults
            props = loadPropertiesFile();
        }

        if (url == null || url.isBlank()) {
            throw new IllegalStateException(
                "Nexus URL not configured. Set NEXUS_URL environment variable or nexus.url in ~/.flossware/nexus/nexus.properties"
            );
        }

        if (user == null || user.isBlank()) {
            throw new IllegalStateException(
                "Nexus user not configured. Set NEXUS_USER environment variable or nexus.user in ~/.flossware/nexus/nexus.properties"
            );
        }

        if (password == null || password.isBlank()) {
            throw new IllegalStateException(
                "Nexus password not configured. Set NEXUS_PASSWORD environment variable or nexus.password in ~/.flossware/nexus/nexus.properties"
            );
        }

        // Validate URL format and security
        try {
            validateUrl(url);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Invalid Nexus URL: " + e.getMessage(), e);
        }

        this.url = url;
        this.user = user;
        this.password = password;

        // Load optional UI defaults from properties file
        this.defaultRepository = props.getProperty("nexus.default.repository", "");
        this.defaultRegex = props.getProperty("nexus.default.regex", "");
        this.defaultDryRun = Boolean.parseBoolean(props.getProperty("nexus.default.dryrun", "true"));

        // Load optional repository list (comma-separated)
        String repoListProp = props.getProperty("nexus.repositories", "");
        if (repoListProp != null && !repoListProp.isBlank()) {
            this.repositories = Arrays.stream(repoListProp.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        } else {
            this.repositories = Collections.emptyList();
        }

        // Load optional HTTP configuration with validation
        String timeoutEnv = System.getenv("NEXUS_HTTP_TIMEOUT");
        String timeoutProp = props.getProperty("nexus.http.timeout.seconds");
        String timeoutStr = timeoutEnv != null ? timeoutEnv : timeoutProp;

        int timeout = 30; // default
        if (timeoutStr != null) {
            try {
                timeout = Integer.parseInt(timeoutStr);
                if (timeout <= 0) {
                    System.err.println("Warning: HTTP timeout must be positive. Using default: 30 seconds");
                    timeout = 30;
                }
            } catch (NumberFormatException e) {
                System.err.println("Warning: Invalid HTTP timeout value '" + timeoutStr + "'. Using default: 30 seconds");
            }
        }
        this.httpTimeoutSeconds = timeout;

        // Load optional retry configuration with validation
        String maxRetriesEnv = System.getenv("NEXUS_MAX_RETRIES");
        String maxRetriesProp = props.getProperty("nexus.http.max.retries");
        String maxRetriesStr = maxRetriesEnv != null ? maxRetriesEnv : maxRetriesProp;

        int retries = 3; // default
        if (maxRetriesStr != null) {
            try {
                retries = Integer.parseInt(maxRetriesStr);
                if (retries < 0 || retries > 10) {
                    System.err.println("Warning: Max retries must be between 0 and 10. Using default: 3");
                    retries = 3;
                }
            } catch (NumberFormatException e) {
                System.err.println("Warning: Invalid max retries value '" + maxRetriesStr + "'. Using default: 3");
            }
        }
        this.maxRetries = retries;

        String retryDelayEnv = System.getenv("NEXUS_RETRY_DELAY_MS");
        String retryDelayProp = props.getProperty("nexus.http.retry.delay.ms");
        String retryDelayStr = retryDelayEnv != null ? retryDelayEnv : retryDelayProp;

        long delay = 1000; // default 1 second
        if (retryDelayStr != null) {
            try {
                delay = Long.parseLong(retryDelayStr);
                if (delay < 0 || delay > 60000) {
                    System.err.println("Warning: Retry delay must be between 0 and 60000ms. Using default: 1000ms");
                    delay = 1000;
                }
            } catch (NumberFormatException e) {
                System.err.println("Warning: Invalid retry delay value '" + retryDelayStr + "'. Using default: 1000ms");
            }
        }
        this.initialRetryDelayMs = delay;

        // Load optional logging configuration
        String logLevelEnv = System.getenv("NEXUS_LOG_LEVEL");
        String logLevelProp = props.getProperty("nexus.log.level");
        String logLevelStr = logLevelEnv != null ? logLevelEnv : logLevelProp;

        String level = "INFO"; // default
        if (logLevelStr != null) {
            String upperLevel = logLevelStr.toUpperCase();
            if (upperLevel.equals("TRACE") || upperLevel.equals("DEBUG") ||
                upperLevel.equals("INFO") || upperLevel.equals("WARN") ||
                upperLevel.equals("ERROR") || upperLevel.equals("OFF")) {
                level = upperLevel;
            } else {
                System.err.println("Warning: Invalid log level '" + logLevelStr + "'. Valid values: TRACE, DEBUG, INFO, WARN, ERROR, OFF. Using default: INFO");
            }
        }
        this.logLevel = level;

        // Set system property for logback to pick up
        System.setProperty("nexus.log.level", this.logLevel);
    }

    /**
     * Validates a Nexus server URL.
     * <p>
     * Checks that the URL:
     * - Is not null or blank
     * - Is a valid URI/URL format
     * - Uses HTTP or HTTPS scheme
     * - Has a valid hostname
     * - Warns if using HTTP instead of HTTPS (security risk)
     * </p>
     *
     * @param url the URL to validate
     * @throws IllegalArgumentException if the URL is invalid
     */
    private static void validateUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL cannot be null or blank");
        }

        try {
            URI uri = new URI(url);

            // Validate scheme
            String scheme = uri.getScheme();
            if (scheme == null) {
                throw new IllegalArgumentException("URL must include a scheme (http:// or https://): " + url);
            }
            if (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https")) {
                throw new IllegalArgumentException("URL scheme must be http or https: " + url);
            }

            // Validate hostname
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                throw new IllegalArgumentException("URL must include a hostname: " + url);
            }

            // Warn about HTTP (not HTTPS)
            if (scheme.equalsIgnoreCase("http")) {
                System.err.println("WARNING: Using HTTP instead of HTTPS. Credentials will be sent over an insecure connection.");
                System.err.println("         Consider using HTTPS for production environments.");
            }

        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL format: " + url + " - " + e.getMessage(), e);
        }
    }

    /**
     * Validates a repository name.
     * <p>
     * Repository names must:
     * - Not be null or blank
     * - Only contain alphanumeric characters, hyphens, underscores, and dots
     * - Not contain path traversal sequences (../)
     * - Not start or end with special characters
     * </p>
     *
     * @param repository the repository name to validate
     * @throws IllegalArgumentException if the repository name is invalid
     */
    public static void validateRepository(String repository) {
        if (repository == null || repository.isBlank()) {
            throw new IllegalArgumentException("Repository name cannot be null or blank");
        }

        // Check for path traversal
        if (repository.contains("..") || repository.contains("/") || repository.contains("\\")) {
            throw new IllegalArgumentException("Repository name cannot contain path traversal sequences or slashes: " + repository);
        }

        // Check for valid characters (alphanumeric, hyphen, underscore, dot)
        Pattern validPattern = Pattern.compile("^[a-zA-Z0-9._-]+$");
        if (!validPattern.matcher(repository).matches()) {
            throw new IllegalArgumentException("Repository name can only contain alphanumeric characters, dots, hyphens, and underscores: " + repository);
        }

        // Check for leading/trailing special characters
        if (repository.startsWith(".") || repository.startsWith("-") || repository.startsWith("_") ||
            repository.endsWith(".") || repository.endsWith("-") || repository.endsWith("_")) {
            throw new IllegalArgumentException("Repository name cannot start or end with special characters: " + repository);
        }
    }

    /**
     * Loads credentials from the properties file.
     * <p>
     * If a profile is set, loads from ~/.flossware/nexus/nexus-{profile}.properties.
     * Otherwise, loads from ~/.flossware/nexus/nexus.properties.
     * If the file doesn't exist or cannot be read, returns an empty Properties object
     * and logs a warning to stderr.
     * </p>
     *
     * @return a Properties object containing the loaded properties, or empty if the
     *         file doesn't exist or couldn't be loaded
     */
    private Properties loadPropertiesFile() {
        Properties props = new Properties();
        String fileName = this.profile != null ? "nexus-" + this.profile + ".properties" : "nexus.properties";
        Path configPath = Paths.get(System.getProperty("user.home"), ".flossware", "nexus", fileName);

        if (Files.exists(configPath)) {
            try (InputStream input = Files.newInputStream(configPath)) {
                props.load(input);
            } catch (IOException e) {
                System.err.println("Warning: Could not load properties file from " + configPath + ": " + e.getMessage());
            }
        } else if (this.profile != null) {
            System.err.println("Warning: Profile '" + this.profile + "' specified but file not found: " + configPath);
        }

        return props;
    }

    /**
     * Gets the Nexus server URL.
     *
     * @return the Nexus server URL (without trailing slash)
     */
    public String getUrl() {
        return url;
    }

    /**
     * Gets the Nexus username.
     *
     * @return the Nexus username for authentication
     */
    public String getUser() {
        return user;
    }

    /**
     * Gets the Nexus password.
     *
     * @return the Nexus password or token for authentication
     */
    public String getPassword() {
        return password;
    }

    /**
     * Gets the default repository name for the UI.
     *
     * @return the default repository name, or empty string if not configured
     */
    public String getDefaultRepository() {
        return defaultRepository;
    }

    /**
     * Gets the default regex filter for the UI.
     *
     * @return the default regex filter, or empty string if not configured
     */
    public String getDefaultRegex() {
        return defaultRegex;
    }

    /**
     * Gets the default dry-run mode for the UI.
     *
     * @return true if dry-run should be enabled by default, false otherwise
     */
    public boolean isDefaultDryRun() {
        return defaultDryRun;
    }

    /**
     * Gets the list of configured repositories.
     * <p>
     * Returns a list of repository names loaded from the nexus.repositories
     * property (comma-separated). This can be used by GUIs to populate
     * dropdown lists or for batch operations.
     * </p>
     *
     * @return unmodifiable list of repository names, or empty list if not configured
     */
    public List<String> getRepositories() {
        return Collections.unmodifiableList(repositories);
    }

    /**
     * Gets the HTTP connection timeout in seconds.
     *
     * @return the HTTP timeout in seconds (default: 30)
     */
    public int getHttpTimeoutSeconds() {
        return httpTimeoutSeconds;
    }

    /**
     * Gets the maximum number of retry attempts for failed HTTP requests.
     *
     * @return the maximum number of retries (default: 3)
     */
    public int getMaxRetries() {
        return maxRetries;
    }

    /**
     * Gets the initial retry delay in milliseconds for exponential backoff.
     *
     * @return the initial retry delay in milliseconds (default: 1000)
     */
    public long getInitialRetryDelayMs() {
        return initialRetryDelayMs;
    }

    /**
     * Gets the configured log level.
     *
     * @return the log level (TRACE, DEBUG, INFO, WARN, ERROR, or OFF; default: INFO)
     */
    public String getLogLevel() {
        return logLevel;
    }

    /**
     * Gets the active profile name.
     *
     * @return the profile name, or null if using default configuration
     */
    public String getProfile() {
        return profile;
    }

    /**
     * Saves these credentials to a properties file.
     * <p>
     * Creates the configuration directory if it doesn't exist.
     * If profile is null, saves to nexus.properties (default).
     * Otherwise, saves to nexus-{profile}.properties.
     * </p>
     *
     * @param profileName the profile name (null for default)
     * @throws IOException if the file cannot be written
     */
    public void saveToPropertiesFile(String profileName) throws IOException {
        Path configDir = Paths.get(System.getProperty("user.home"), ".flossware", "nexus");
        Files.createDirectories(configDir);

        String fileName = (profileName != null && !profileName.isBlank())
            ? "nexus-" + profileName + ".properties"
            : "nexus.properties";
        Path configPath = configDir.resolve(fileName);

        Properties props = new Properties();
        props.setProperty("nexus.url", url);
        props.setProperty("nexus.user", user);
        props.setProperty("nexus.password", password);

        if (!repositories.isEmpty()) {
            props.setProperty("nexus.repositories", String.join(",", repositories));
        }

        if (!defaultRepository.isEmpty()) {
            props.setProperty("nexus.default.repository", defaultRepository);
        }

        if (!defaultRegex.isEmpty()) {
            props.setProperty("nexus.default.regex", defaultRegex);
        }

        props.setProperty("nexus.default.dryrun", String.valueOf(defaultDryRun));

        if (httpTimeoutSeconds != 30) {
            props.setProperty("nexus.http.timeout.seconds", String.valueOf(httpTimeoutSeconds));
        }

        if (maxRetries != 3) {
            props.setProperty("nexus.http.max.retries", String.valueOf(maxRetries));
        }

        if (initialRetryDelayMs != 1000) {
            props.setProperty("nexus.http.retry.delay.ms", String.valueOf(initialRetryDelayMs));
        }

        if (!logLevel.equals("INFO")) {
            props.setProperty("nexus.log.level", logLevel);
        }

        try (java.io.OutputStream output = Files.newOutputStream(configPath)) {
            props.store(output, "Nexus Configuration - Saved by JNexus");
        }
    }

    /**
     * Discovers available configuration profiles in ~/.flossware/nexus directory.
     * <p>
     * Scans for files matching the pattern nexus*.properties and extracts profile names:
     * </p>
     * <ul>
     *   <li>nexus.properties → "default"</li>
     *   <li>nexus-dev.properties → "dev"</li>
     *   <li>nexus-prod.properties → "prod"</li>
     * </ul>
     *
     * @return list of profile names found, sorted alphabetically with "default" first if it exists
     */
    public static List<String> discoverProfiles() {
        Path configDir = Paths.get(System.getProperty("user.home"), ".flossware", "nexus");
        List<String> profiles = new java.util.ArrayList<>();

        if (!Files.exists(configDir) || !Files.isDirectory(configDir)) {
            return profiles;
        }

        try {
            Files.list(configDir)
                .filter(path -> path.getFileName().toString().startsWith("nexus") &&
                               path.getFileName().toString().endsWith(".properties"))
                .forEach(path -> {
                    String fileName = path.getFileName().toString();
                    if (fileName.equals("nexus.properties")) {
                        profiles.add("default");
                    } else if (fileName.startsWith("nexus-") && fileName.endsWith(".properties")) {
                        // Extract profile name: nexus-dev.properties -> dev
                        String profileName = fileName.substring(6, fileName.length() - 11);
                        profiles.add(profileName);
                    }
                });
        } catch (IOException e) {
            System.err.println("Warning: Could not scan profiles directory: " + e.getMessage());
        }

        // Sort profiles alphabetically, but keep "default" first if it exists
        profiles.sort((a, b) -> {
            if (a.equals("default")) return -1;
            if (b.equals("default")) return 1;
            return a.compareTo(b);
        });

        return profiles;
    }
}

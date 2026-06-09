package org.flossware.nexus;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.flossware.crypto.AESEncryption;
import org.flossware.jnexus.HttpConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages Nexus repository credentials and configuration.
 * <p>
 * This class reads Nexus credentials from multiple sources in the following priority order:
 * </p>
 * <ol>
 *   <li><strong>Environment variables</strong> (highest priority):
 *     <ul>
 *       <li>NEXUS_URL - Nexus server URL</li>
 *       <li>NEXUS_USER - Username</li>
 *       <li>NEXUS_PASSWORD - Password or token</li>
 *       <li>NEXUS_PROFILE - Configuration profile name</li>
 *       <li>NEXUS_HTTP_TIMEOUT - HTTP timeout in seconds</li>
 *       <li>NEXUS_MAX_RETRIES - Maximum retry attempts</li>
 *       <li>NEXUS_RETRY_DELAY_MS - Initial retry delay</li>
 *       <li>NEXUS_LOG_LEVEL - Logging level</li>
 *     </ul>
 *   </li>
 *   <li><strong>Properties file</strong> (fallback):
 *     <ul>
 *       <li>If NEXUS_PROFILE is set: ~/.flossware/nexus/nexus-{profile}.properties</li>
 *       <li>Otherwise: ~/.flossware/nexus/nexus.properties (default)</li>
 *     </ul>
 *   </li>
 * </ol>
 * <p>
 * All three required credentials (URL, user, password) must be provided or an
 * {@link IllegalStateException} will be thrown during construction.
 * </p>
 *
 * <h2>Configuration Properties:</h2>
 * <p>
 * The properties file supports the following settings:
 * </p>
 * <pre>
 * # Required credentials
 * nexus.url=https://nexus.example.com
 * nexus.user=your-username
 * nexus.password=your-token
 *
 * # Optional UI defaults
 * nexus.default.repository=maven-releases
 * nexus.default.regex=.*SNAPSHOT.*
 * nexus.default.dryrun=true
 *
 * # Optional repository list (for dropdowns/batch ops)
 * nexus.repositories=maven-releases,maven-snapshots,npm-public
 *
 * # Optional HTTP configuration
 * nexus.http.timeout.seconds=30
 * nexus.http.max.retries=3
 * nexus.http.retry.delay.ms=1000
 *
 * # Optional logging configuration
 * nexus.log.level=INFO
 * </pre>
 *
 * <h2>Profile Support:</h2>
 * <p>
 * Use NEXUS_PROFILE environment variable to switch between different configurations:
 * </p>
 * <pre>
 * # Development environment
 * export NEXUS_PROFILE=dev
 * # Loads ~/.flossware/nexus/nexus-dev.properties
 *
 * # Production environment
 * export NEXUS_PROFILE=prod
 * # Loads ~/.flossware/nexus/nexus-prod.properties
 *
 * # Staging environment
 * export NEXUS_PROFILE=staging
 * # Loads ~/.flossware/nexus/nexus-staging.properties
 * </pre>
 *
 * <h2>Password Encryption:</h2>
 * <p>
 * Starting in version 1.30, passwords are automatically encrypted using AES-256-GCM
 * when saved to properties files. Key features:
 * </p>
 * <ul>
 *   <li><strong>Automatic encryption</strong>: Passwords are encrypted on save</li>
 *   <li><strong>Automatic decryption</strong>: Encrypted passwords are transparently decrypted on load</li>
 *   <li><strong>Backward compatibility</strong>: Existing plaintext passwords still work and are auto-migrated on next save</li>
 *   <li><strong>Machine-specific keys</strong>: Encryption keys derived from hostname and user home directory</li>
 *   <li><strong>Non-portable</strong>: Encrypted credentials cannot be copied to other machines</li>
 * </ul>
 * <p>
 * <strong>Security note:</strong> While encrypted passwords are more secure than plaintext,
 * environment variables (NEXUS_PASSWORD) remain the most secure option for sensitive environments
 * as they never touch disk.
 * </p>
 *
 * <h2>Usage Examples:</h2>
 * <pre>
 * // Load from default configuration (env vars or ~/.flossware/nexus/nexus.properties)
 * Credentials credentials = new Credentials();
 *
 * // Load from specific profile
 * Credentials prodCreds = new Credentials("prod");
 *
 * // Create from explicit values (for interactive dialogs)
 * Credentials dialogCreds = new Credentials(
 *     "https://nexus.example.com",
 *     "username",
 *     "password",
 *     "maven-releases,maven-snapshots"
 * );
 *
 * // Save credentials to properties file
 * credentials.saveToPropertiesFile(null);        // Save to default
 * credentials.saveToPropertiesFile("prod");      // Save to nexus-prod.properties
 *
 * // Discover available profiles
 * List&lt;String&gt; profiles = Credentials.discoverProfiles();
 * System.out.println("Available profiles: " + profiles);
 *
 * // Validate repository name
 * Credentials.validateRepository("maven-releases");  // OK
 * Credentials.validateRepository("../etc/passwd");   // Throws IllegalArgumentException
 *
 * // Access configuration
 * String url = credentials.getUrl();
 * int timeout = credentials.getHttpTimeoutSeconds();
 * List&lt;String&gt; repos = credentials.getRepositories();
 * </pre>
 *
 * <h2>Security Considerations:</h2>
 * <p>
 * <strong>WARNING:</strong> Desktop applications store credentials in <strong>plaintext</strong>
 * in the properties file. Follow these security best practices:
 * </p>
 * <ul>
 *   <li>Set file permissions: {@code chmod 600 ~/.flossware/nexus/nexus.properties}</li>
 *   <li>Use environment variables for CI/CD instead of files</li>
 *   <li>Use Nexus user tokens instead of passwords</li>
 *   <li>Always use HTTPS URLs (HTTP triggers a warning)</li>
 *   <li>Never commit credentials to version control</li>
 * </ul>
 * <p>
 * Mobile platforms (Android/iOS) use encrypted storage:
 * </p>
 * <ul>
 *   <li><strong>Android:</strong> AES256_GCM via EncryptedSharedPreferences</li>
 *   <li><strong>iOS/macOS:</strong> AES-256 hardware-backed Keychain</li>
 * </ul>
 *
 * @author sfloess
 * @since 1.0
 * @see NexusClient
 * @see NexusService
 */
public class Credentials implements HttpConfig {
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

    // Logging
    private static final Logger logger = LoggerFactory.getLogger(Credentials.class);

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

        // Load credentials from environment variables and/or properties file
        Properties props = loadPropertiesFile();
        String[] creds = loadRequiredCredentials(props);
        this.url = creds[0];
        this.user = creds[1];
        this.password = creds[2];

        // Load optional UI defaults from properties file
        this.defaultRepository = props.getProperty("nexus.default.repository", "");
        this.defaultRegex = props.getProperty("nexus.default.regex", "");
        this.defaultDryRun = Boolean.parseBoolean(props.getProperty("nexus.default.dryrun", "true"));
        this.repositories = parseRepositoryList(props.getProperty("nexus.repositories", ""));

        // Load optional HTTP and retry configuration
        this.httpTimeoutSeconds = loadIntConfig("NEXUS_HTTP_TIMEOUT",
            props.getProperty("nexus.http.timeout.seconds"), 30, 1, Integer.MAX_VALUE,
            "HTTP timeout must be positive");
        this.maxRetries = loadIntConfig("NEXUS_MAX_RETRIES",
            props.getProperty("nexus.http.max.retries"), 3, 0, 10,
            "Max retries must be between 0 and 10");
        this.initialRetryDelayMs = loadLongConfig("NEXUS_RETRY_DELAY_MS",
            props.getProperty("nexus.http.retry.delay.ms"), 1000, 0, 60000,
            "Retry delay must be between 0 and 60000ms");

        // Load optional logging configuration
        this.logLevel = loadLogLevel(props);
        System.setProperty("nexus.log.level", this.logLevel);
    }

    /**
     * Loads required credentials from environment variables with properties file fallback.
     * <p>
     * Environment variables (NEXUS_URL, NEXUS_USER, NEXUS_PASSWORD) take precedence.
     * Falls back to properties file values. Validates that all required fields are present.
     * Handles password decryption for encrypted properties file values.
     * </p>
     *
     * @param props the loaded properties file
     * @return a String array of [url, user, password]
     * @throws IllegalStateException if any required credential is missing or invalid
     */
    private String[] loadRequiredCredentials(Properties props) {
        String url = System.getenv("NEXUS_URL");
        String user = System.getenv("NEXUS_USER");
        String password = System.getenv("NEXUS_PASSWORD");

        // Fill in missing values from properties file
        if (url == null) {
            url = props.getProperty("nexus.url");
        }
        if (user == null) {
            user = props.getProperty("nexus.user");
        }
        if (password == null) {
            password = loadPassword(props);
        }

        // Validate required fields
        validateRequired(url, "Nexus URL", "NEXUS_URL", "nexus.url");
        validateRequired(user, "Nexus user", "NEXUS_USER", "nexus.user");
        validateRequired(password, "Nexus password", "NEXUS_PASSWORD", "nexus.password");

        // Validate URL format and security
        try {
            validateUrl(url);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Invalid Nexus URL: " + e.getMessage(), e);
        }

        return new String[]{url, user, password};
    }

    /**
     * Loads and decrypts password from properties file.
     *
     * @param props the loaded properties
     * @return decrypted password, or null if not present
     * @throws IllegalStateException if decryption fails
     */
    private static String loadPassword(Properties props) {
        String rawPassword = props.getProperty("nexus.password");
        if (rawPassword == null) {
            return null;
        }
        if (AESEncryption.isEncrypted(rawPassword)) {
            try {
                AESEncryption encryption = new AESEncryption();
                String decrypted = encryption.decrypt(rawPassword);
                logger.debug("Successfully decrypted password from properties file");
                return decrypted;
            } catch (Exception e) {
                logger.error("Failed to decrypt password - credentials may be corrupted or from a different machine", e);
                throw new IllegalStateException(
                    "Failed to decrypt password. Credentials may be corrupted or created on a different machine. " +
                    "Delete ~/.flossware/nexus/nexus.properties and reconfigure.", e);
            }
        }
        logger.warn("Password is stored in plaintext - will be encrypted on next save");
        return rawPassword;
    }

    /**
     * Validates that a required configuration value is present and non-blank.
     *
     * @param value    the value to check
     * @param name     human-readable name for error messages
     * @param envVar   the environment variable name
     * @param propKey  the properties file key
     * @throws IllegalStateException if the value is null or blank
     */
    private static void validateRequired(String value, String name, String envVar, String propKey) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                name + " not configured. Set " + envVar +
                " environment variable or " + propKey +
                " in ~/.flossware/nexus/nexus.properties"
            );
        }
    }

    /**
     * Parses a comma-separated repository list into an unmodifiable list.
     *
     * @param repoListStr comma-separated repository names
     * @return list of trimmed, non-empty repository names
     */
    private static List<String> parseRepositoryList(String repoListStr) {
        if (repoListStr == null || repoListStr.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(repoListStr.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());
    }

    /**
     * Loads an integer configuration value from environment variable or properties,
     * with range validation and default fallback.
     *
     * @param envVarName   the environment variable name to check first
     * @param propValue    the properties file value (fallback)
     * @param defaultValue the default if neither source provides a valid value
     * @param min          minimum valid value (inclusive)
     * @param max          maximum valid value (inclusive)
     * @param rangeMessage warning message when value is out of range
     * @return the resolved configuration value
     */
    private static int loadIntConfig(String envVarName, String propValue,
            int defaultValue, int min, int max, String rangeMessage) {
        String envValue = System.getenv(envVarName);
        String valueStr = envValue != null ? envValue : propValue;
        if (valueStr == null) {
            return defaultValue;
        }
        try {
            int value = Integer.parseInt(valueStr);
            if (value < min || value > max) {
                logger.warn("{}. Using default: {}", rangeMessage, defaultValue);
                return defaultValue;
            }
            return value;
        } catch (NumberFormatException e) {
            logger.warn("Invalid value '{}' for {}. Using default: {}", valueStr, envVarName, defaultValue);
            return defaultValue;
        }
    }

    /**
     * Loads a long configuration value from environment variable or properties,
     * with range validation and default fallback.
     *
     * @param envVarName   the environment variable name to check first
     * @param propValue    the properties file value (fallback)
     * @param defaultValue the default if neither source provides a valid value
     * @param min          minimum valid value (inclusive)
     * @param max          maximum valid value (inclusive)
     * @param rangeMessage warning message when value is out of range
     * @return the resolved configuration value
     */
    private static long loadLongConfig(String envVarName, String propValue,
            long defaultValue, long min, long max, String rangeMessage) {
        String envValue = System.getenv(envVarName);
        String valueStr = envValue != null ? envValue : propValue;
        if (valueStr == null) {
            return defaultValue;
        }
        try {
            long value = Long.parseLong(valueStr);
            if (value < min || value > max) {
                logger.warn("{}. Using default: {}", rangeMessage, defaultValue);
                return defaultValue;
            }
            return value;
        } catch (NumberFormatException e) {
            logger.warn("Invalid value '{}' for {}. Using default: {}", valueStr, envVarName, defaultValue);
            return defaultValue;
        }
    }

    /**
     * Loads and validates the log level configuration.
     *
     * @param props the loaded properties
     * @return validated log level string (TRACE, DEBUG, INFO, WARN, ERROR, or OFF)
     */
    private static String loadLogLevel(Properties props) {
        String envValue = System.getenv("NEXUS_LOG_LEVEL");
        String propValue = props.getProperty("nexus.log.level");
        String logLevelStr = envValue != null ? envValue : propValue;
        if (logLevelStr == null) {
            return "INFO";
        }
        String upperLevel = logLevelStr.toUpperCase();
        if (Set.of("TRACE", "DEBUG", "INFO", "WARN", "ERROR", "OFF").contains(upperLevel)) {
            return upperLevel;
        }
        logger.warn("Invalid log level '{}'. Valid values: TRACE, DEBUG, INFO, WARN, ERROR, OFF. Using default: INFO", logLevelStr);
        return "INFO";
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
                logger.warn("Using HTTP instead of HTTPS. Credentials will be sent over an insecure connection. Consider using HTTPS for production environments.");
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
                logger.warn("Could not load properties file from {}: {}", configPath, e.getMessage());
            }
        } else if (this.profile != null) {
            logger.warn("Profile '{}' specified but file not found: {}", this.profile, configPath);
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
    @Override
    public int getHttpTimeoutSeconds() {
        return httpTimeoutSeconds;
    }

    /**
     * Gets the maximum number of retry attempts for failed HTTP requests.
     * <p>
     * This is read from configuration (env var or properties file):
     * - Environment variable: NEXUS_MAX_RETRIES
     * - Properties file: nexus.http.max.retries
     * - Default: 3
     * </p>
     *
     * @return the maximum number of retries (default: 3, range: 0-10)
     */
    @Override
    public int getMaxRetries() {
        return maxRetries;
    }

    /**
     * Gets the initial retry delay in milliseconds for exponential backoff.
     * <p>
     * This is read from configuration (env var or properties file):
     * - Environment variable: NEXUS_RETRY_DELAY_MS
     * - Properties file: nexus.http.retry.delay.ms
     * - Default: 1000ms
     * </p>
     *
     * @return the initial retry delay in milliseconds (default: 1000, range: 0-60000)
     */
    @Override
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

        // Encrypt password before saving
        AESEncryption encryption = new AESEncryption();
        String encryptedPassword = encryption.encrypt(password);
        props.setProperty("nexus.password", encryptedPassword);
        logger.info("Password encrypted using AES-256-GCM before saving to properties file");

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
            props.store(output, "Nexus Configuration - Saved by Nexus");
        }

        // Set file permissions to 600 (rw-------, user read/write only) for security
        try {
            Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rw-------");
            Files.setPosixFilePermissions(configPath, perms);
            logger.info("Set file permissions to 600 (user read/write only): {}", configPath);
        } catch (UnsupportedOperationException e) {
            // Windows doesn't support POSIX permissions
            logger.warn("Cannot set POSIX permissions on this platform (Windows?). Please restrict file access manually: {}", configPath);
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
            logger.warn("Could not scan profiles directory: {}", e.getMessage());
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

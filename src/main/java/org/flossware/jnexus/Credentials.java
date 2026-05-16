package org.flossware.jnexus;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Manages Nexus repository credentials and configuration.
 * <p>
 * This class reads Nexus credentials from multiple sources in the following priority:
 * </p>
 * <ol>
 *   <li>Environment variables: NEXUS_URL, NEXUS_USER, NEXUS_PASSWORD</li>
 *   <li>Properties file: ~/.flossware/nexus/nexus.properties</li>
 * </ol>
 * <p>
 * All three credentials (URL, user, password) must be provided or an
 * {@link IllegalStateException} will be thrown during construction.
 * </p>
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

    /**
     * Constructs a new Credentials instance by loading configuration from
     * environment variables or properties file.
     * <p>
     * Environment variables take precedence over properties file values.
     * If any required credential is missing or blank, an exception is thrown.
     * </p>
     *
     * @throws IllegalStateException if any required credential (URL, user, or password)
     *                               is not configured or is blank
     */
    public Credentials() {
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

        this.url = url;
        this.user = user;
        this.password = password;

        // Load optional UI defaults from properties file
        this.defaultRepository = props.getProperty("nexus.default.repository", "");
        this.defaultRegex = props.getProperty("nexus.default.regex", "");
        this.defaultDryRun = Boolean.parseBoolean(props.getProperty("nexus.default.dryrun", "true"));
    }

    /**
     * Loads credentials from the properties file located at
     * ~/.flossware/nexus/nexus.properties.
     * <p>
     * If the file doesn't exist or cannot be read, returns an empty Properties object
     * and logs a warning to stderr.
     * </p>
     *
     * @return a Properties object containing the loaded properties, or empty if the
     *         file doesn't exist or couldn't be loaded
     */
    private Properties loadPropertiesFile() {
        Properties props = new Properties();
        Path configPath = Paths.get(System.getProperty("user.home"), ".flossware", "nexus", "nexus.properties");

        if (Files.exists(configPath)) {
            try (InputStream input = Files.newInputStream(configPath)) {
                props.load(input);
            } catch (IOException e) {
                System.err.println("Warning: Could not load properties file from " + configPath + ": " + e.getMessage());
            }
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
}

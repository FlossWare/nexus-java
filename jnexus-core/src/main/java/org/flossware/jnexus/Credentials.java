package org.flossware.jnexus;

import java.util.List;

/**
 * Platform-agnostic credentials provider interface.
 * <p>
 * This interface abstracts credential storage and retrieval to support
 * multiple platform implementations:
 * - Desktop: CredentialsFile (uses ~/.flossware/nexus/nexus.properties)
 * - Android: CredentialsAndroid (uses EncryptedSharedPreferences)
 * </p>
 * <p>
 * Credentials extends HttpConfig to provide both authentication and HTTP
 * configuration parameters. This allows HTTP client implementations to accept
 * a single Credentials object that provides all necessary configuration.
 * </p>
 */
public interface Credentials extends HttpConfig {

    /**
     * Gets the Nexus server URL.
     *
     * @return the Nexus URL (e.g., "https://nexus.example.com")
     */
    String getUrl();

    /**
     * Gets the Nexus username.
     *
     * @return the username
     */
    String getUser();

    /**
     * Gets the Nexus password.
     *
     * @return the password
     */
    String getPassword();

    /**
     * Gets the active profile name.
     *
     * @return the profile name, or null for default profile
     */
    String getProfile();

    /**
     * Gets the list of available repositories.
     *
     * @return comma-separated list of repository names, or empty list
     */
    List<String> getRepositories();

    /**
     * Gets the default repository name.
     *
     * @return the default repository, or empty string
     */
    String getDefaultRepository();

    /**
     * Gets the default regex filter.
     *
     * @return the default regex, or empty string
     */
    String getDefaultRegex();

    /**
     * Gets the default dry-run mode.
     *
     * @return true if dry-run mode is default
     */
    boolean getDefaultDryRun();

    /**
     * Gets the HTTP timeout in seconds.
     *
     * @return the timeout in seconds (default: 30)
     */
    int getHttpTimeoutSeconds();

    /**
     * Gets the maximum number of retry attempts for failed HTTP requests.
<<<<<<< HEAD
     *
     * @return the maximum number of retries (default: 3)
     */
    int getMaxRetries();

    /**
     * Gets the initial retry delay in milliseconds for exponential backoff.
     *
     * @return the initial retry delay in milliseconds (default: 1000)
     */
    long getInitialRetryDelayMs();
=======
     * <p>
     * Applies exponential backoff with initial delay from {@link #getInitialRetryDelayMs()}.
     * </p>
     *
     * @return the maximum number of retries (default: 3, valid range: 0-10)
     */
    @Override
    default int getMaxRetries() {
        return 3;
    }

    /**
     * Gets the initial retry delay in milliseconds for exponential backoff.
     * <p>
     * Actual delay increases exponentially: delay, 2×delay, 4×delay, etc.
     * For example, with initialDelay=1000:
     * - Attempt 1 fails: wait 1000ms
     * - Attempt 2 fails: wait 2000ms
     * - Attempt 3 fails: wait 4000ms
     * </p>
     *
     * @return the initial retry delay in milliseconds (default: 1000, valid range: 0-60000)
     */
    @Override
    default long getInitialRetryDelayMs() {
        return 1000;
    }
>>>>>>> e17d8af (chore: Remove .claude directory and add to .gitignore)
}

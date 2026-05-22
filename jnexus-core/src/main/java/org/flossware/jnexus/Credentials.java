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
 */
public interface Credentials {

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
}

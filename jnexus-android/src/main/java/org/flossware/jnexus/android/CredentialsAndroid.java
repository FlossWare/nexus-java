package org.flossware.jnexus.android;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import org.flossware.jnexus.Credentials;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Android credentials implementation using encrypted SharedPreferences.
 * <p>
 * Replaces file-based configuration with Android secure storage.
 * Uses {@link EncryptedSharedPreferences} to securely store Nexus credentials
 * including URL, username, password, and configuration options.
 * </p>
 * <p>
 * Encryption is handled automatically using AES256_GCM for both keys and values.
 * </p>
 *
 * @author sfloess
 * @since 1.1
 */
public class CredentialsAndroid implements Credentials {
    private static final String PREFS_NAME = "nexus_credentials";
    private static final String KEY_URL = "nexus.url";
    private static final String KEY_USER = "nexus.user";
    private static final String KEY_PASSWORD = "nexus.password";
    private static final String KEY_PROFILE = "nexus.profile";
    private static final String KEY_REPOSITORIES = "nexus.repositories";
    private static final String KEY_DEFAULT_REPO = "nexus.default.repository";
    private static final String KEY_DEFAULT_REGEX = "nexus.default.regex";
    private static final String KEY_DEFAULT_DRYRUN = "nexus.default.dryrun";
    private static final String KEY_TIMEOUT = "nexus.http.timeout.seconds";

    private final SharedPreferences prefs;

    /**
     * Constructs a new CredentialsAndroid instance.
     * <p>
     * Initializes encrypted SharedPreferences using the master key with AES256_GCM encryption.
     * </p>
     *
     * @param context the Android application context
     * @throws IOException if encrypted storage initialization fails
     */
    public CredentialsAndroid(Context context) throws IOException {
        try {
            // Create encrypted preferences using AndroidX Security library
            MasterKey masterKey = new MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();

            this.prefs = EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            throw new IOException("Failed to initialize encrypted storage", e);
        }
    }

    @Override
    public String getUrl() {
        return prefs.getString(KEY_URL, null);
    }

    @Override
    public String getUser() {
        return prefs.getString(KEY_USER, null);
    }

    @Override
    public String getPassword() {
        return prefs.getString(KEY_PASSWORD, null);
    }

    @Override
    public String getProfile() {
        return prefs.getString(KEY_PROFILE, null);
    }

    @Override
    public List<String> getRepositories() {
        String repos = prefs.getString(KEY_REPOSITORIES, "");
        if (repos.isEmpty()) {
            return List.of();
        }
        return Arrays.stream(repos.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
    }

    @Override
    public String getDefaultRepository() {
        return prefs.getString(KEY_DEFAULT_REPO, "");
    }

    @Override
    public String getDefaultRegex() {
        return prefs.getString(KEY_DEFAULT_REGEX, "");
    }

    @Override
    public boolean getDefaultDryRun() {
        return prefs.getBoolean(KEY_DEFAULT_DRYRUN, true);
    }

    @Override
    public int getHttpTimeoutSeconds() {
        return prefs.getInt(KEY_TIMEOUT, 30);
    }

    // Save methods for settings screen

    /**
     * Saves Nexus server credentials.
     * <p>
     * Credentials are encrypted automatically by EncryptedSharedPreferences.
     * </p>
     *
     * @param url the Nexus server URL
     * @param user the username
     * @param password the password
     */
    public void saveCredentials(String url, String user, String password) {
        prefs.edit()
            .putString(KEY_URL, url)
            .putString(KEY_USER, user)
            .putString(KEY_PASSWORD, password)
            .apply();
    }

    /**
     * Saves the list of available repositories.
     *
     * @param repositories comma-separated list of repository names
     */
    public void saveRepositories(String repositories) {
        prefs.edit()
            .putString(KEY_REPOSITORIES, repositories)
            .apply();
    }

    /**
     * Saves default configuration values.
     *
     * @param defaultRepo the default repository name
     * @param defaultRegex the default regex filter
     * @param defaultDryRun the default dry-run mode
     */
    public void saveDefaults(String defaultRepo, String defaultRegex, boolean defaultDryRun) {
        prefs.edit()
            .putString(KEY_DEFAULT_REPO, defaultRepo)
            .putString(KEY_DEFAULT_REGEX, defaultRegex)
            .putBoolean(KEY_DEFAULT_DRYRUN, defaultDryRun)
            .apply();
    }

    /**
     * Saves HTTP timeout configuration.
     *
     * @param timeoutSeconds the timeout in seconds
     */
    public void saveHttpTimeout(int timeoutSeconds) {
        prefs.edit()
            .putInt(KEY_TIMEOUT, timeoutSeconds)
            .apply();
    }

    /**
     * Saves the active profile name.
     *
     * @param profile the profile name
     */
    public void saveProfile(String profile) {
        prefs.edit()
            .putString(KEY_PROFILE, profile)
            .apply();
    }

    /**
     * Checks if credentials have been configured.
     *
     * @return true if URL, user, and password are all set
     */
    public boolean hasCredentials() {
        String url = getUrl();
        String user = getUser();
        String password = getPassword();
        return url != null && !url.isEmpty() &&
               user != null && !user.isEmpty() &&
               password != null && !password.isEmpty();
    }

    /**
     * Clears all stored credentials and configuration.
     */
    public void clearAll() {
        prefs.edit().clear().apply();
    }
}

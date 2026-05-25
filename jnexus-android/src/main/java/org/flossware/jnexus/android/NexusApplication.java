package org.flossware.jnexus.android;

import android.app.Application;
import org.flossware.jnexus.*;

import java.io.IOException;

/**
 * Android application class for dependency injection and initialization.
 * <p>
 * Initializes and provides access to:
 * </p>
 * <ul>
 *   <li>Credentials (CredentialsAndroid)</li>
 *   <li>HTTP client (NexusClientOkHttp)</li>
 *   <li>Business logic service (NexusService)</li>
 * </ul>
 * <p>
 * These instances are created once on application startup and shared across all activities.
 * </p>
 *
 * @author sfloess
 * @since 1.1
 */
public class NexusApplication extends Application {
    private CredentialsAndroid credentials;
    private NexusClientOkHttp httpClient;
    private NexusService service;

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            // Initialize credentials (encrypted SharedPreferences)
            credentials = new CredentialsAndroid(this);

            // Initialize HTTP client if credentials exist
            if (credentials.hasCredentials()) {
                httpClient = new NexusClientOkHttp(credentials);
                service = new NexusService(httpClient);
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize application", e);
        }
    }

    /**
     * Gets the HTTP client instance.
     * <p>
     * May be null if credentials have not been configured yet.
     * </p>
     *
     * @return the HTTP client, or null if not initialized
     */
    public NexusClientOkHttp getHttpClient() {
        return httpClient;
    }

    /**
     * Gets the credentials instance.
     *
     * @return the credentials provider
     */
    public CredentialsAndroid getCredentials() {
        return credentials;
    }

    /**
     * Gets the service instance.
     * <p>
     * May be null if credentials have not been configured yet.
     * </p>
     *
     * @return the business logic service, or null if not initialized
     */
    public NexusService getService() {
        return service;
    }

    /**
     * Reinitializes the HTTP client and service with updated credentials.
     * <p>
     * Call this method after saving new credentials in the settings screen.
     * Properly closes the old HTTP client before creating a new one to avoid resource leaks.
     * </p>
     */
    public void reinitializeServices() {
        // Close old client to release OkHttp resources (connection pools, threads)
        if (httpClient != null) {
            httpClient.close();
        }

        if (credentials.hasCredentials()) {
            httpClient = new NexusClientOkHttp(credentials);
            service = new NexusService(httpClient);
        } else {
            httpClient = null;
            service = null;
        }
    }
}

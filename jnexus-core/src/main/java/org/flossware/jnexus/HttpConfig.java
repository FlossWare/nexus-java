package org.flossware.jnexus;

/**
 * HTTP transport configuration for Nexus client implementations.
 * <p>
 * This interface abstracts HTTP-level configuration (timeouts, retries) from
 * credential storage concerns. This separation follows the Single Responsibility
 * Principle by keeping authentication logic separate from HTTP transport configuration.
 * </p>
 * <p>
 * All platform implementations use this interface to configure HTTP client behavior:
 * - Desktop: Reads from Credentials object
 * - Android: Reads from Credentials object
 * - iOS/macOS: Reads from AppState or similar configuration source
 * </p>
 *
 * @author sfloess
 * @since 1.0
 */
public interface HttpConfig {

    /**
     * Gets the HTTP connection timeout in seconds.
     *
     * @return the timeout in seconds (default: 30)
     */
    int getHttpTimeoutSeconds();

    /**
     * Gets the maximum number of retry attempts for failed HTTP requests.
     * <p>
     * Applies exponential backoff with initial delay from {@link #getInitialRetryDelayMs()}.
     * </p>
     *
     * @return the maximum number of retries (default: 3, valid range: 0-10)
     */
    int getMaxRetries();

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
    long getInitialRetryDelayMs();
}

package org.flossware.jnexus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Utility class for internationalization (i18n) and localization (l10n).
 * <p>
 * Provides access to localized messages from resource bundles.
 * Supports English (default), Spanish, French, and German.
 * </p>
 * <p>
 * Locale selection priority:
 * </p>
 * <ol>
 *   <li>JNEXUS_LANG environment variable (e.g., "es", "fr", "de")</li>
 *   <li>System default locale (Locale.getDefault())</li>
 *   <li>English fallback if bundle not found</li>
 * </ol>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * // Simple message
 * String title = Messages.get("dialog.title.error");
 *
 * // Message with parameters
 * String error = Messages.get("error.list.failed", exception.getMessage());
 * }</pre>
 *
 * @author sfloess
 * @since 2.0
 */
public class Messages {
    private static final Logger logger = LoggerFactory.getLogger(Messages.class);

    private static final String BUNDLE_NAME = "messages";
    private static final String LANG_ENV_VAR = "JNEXUS_LANG";

    private static ResourceBundle bundle;
    private static Locale currentLocale;

    static {
        // Initialize with locale from environment or system default
        initialize();
    }

    /**
     * Initialize the resource bundle with the appropriate locale.
     * <p>
     * Locale selection:
     * </p>
     * <ol>
     *   <li>JNEXUS_LANG environment variable</li>
     *   <li>System default locale</li>
     *   <li>English fallback</li>
     * </ol>
     */
    private static void initialize() {
        Locale locale = determineLocale();
        try {
            bundle = ResourceBundle.getBundle(BUNDLE_NAME, locale);
            currentLocale = bundle.getLocale();
            logger.info("Loaded resource bundle for locale: {}", currentLocale);
        } catch (MissingResourceException e) {
            logger.warn("Resource bundle not found for locale: {}, falling back to English", locale);
            bundle = ResourceBundle.getBundle(BUNDLE_NAME, Locale.ENGLISH);
            currentLocale = Locale.ENGLISH;
        }
    }

    /**
     * Determine the locale to use based on environment and system settings.
     *
     * @return the locale to use
     */
    private static Locale determineLocale() {
        // Check JNEXUS_LANG environment variable first
        String lang = System.getenv(LANG_ENV_VAR);
        if (lang != null && !lang.isEmpty()) {
            logger.info("Using locale from {} environment variable: {}", LANG_ENV_VAR, lang);
            return Locale.forLanguageTag(lang);
        }

        // Use system default locale
        Locale systemLocale = Locale.getDefault();
        logger.info("Using system default locale: {}", systemLocale);
        return systemLocale;
    }

    /**
     * Get a localized message for the given key.
     *
     * @param key the message key
     * @return the localized message, or the key itself if not found
     */
    public static String get(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            logger.warn("Message key not found: {}", key);
            return key;  // Return key as fallback
        }
    }

    /**
     * Get a localized message with parameters.
     * <p>
     * Uses {@link MessageFormat} for parameter substitution.
     * </p>
     *
     * @param key    the message key
     * @param params the parameters to substitute into the message
     * @return the formatted message
     */
    public static String get(String key, Object... params) {
        String message = get(key);
        try {
            return MessageFormat.format(message, params);
        } catch (IllegalArgumentException e) {
            logger.warn("Error formatting message for key: {} with params: {}", key, params, e);
            return message;  // Return unformatted message as fallback
        }
    }

    /**
     * Get the current locale being used.
     *
     * @return the current locale
     */
    public static Locale getCurrentLocale() {
        return currentLocale;
    }

    /**
     * Reload the resource bundle with a new locale.
     * <p>
     * Useful for changing language at runtime.
     * Only accepts explicitly supported locales.
     * </p>
     *
     * @param locale the new locale to use
     * @throws IllegalArgumentException if locale is not supported
     */
    public static void setLocale(Locale locale) {
        // Validate that the locale is supported
        boolean isSupported = false;
        for (Locale supported : getAvailableLocales()) {
            if (supported.getLanguage().equals(locale.getLanguage())) {
                isSupported = true;
                break;
            }
        }

        if (!isSupported) {
            throw new IllegalArgumentException("Locale not supported: " + locale +
                ". Supported locales: en, es, fr, de");
        }

        try {
            bundle = ResourceBundle.getBundle(BUNDLE_NAME, locale);
            currentLocale = bundle.getLocale();
            logger.info("Switched to locale: {}", currentLocale);
        } catch (MissingResourceException e) {
            logger.error("Failed to load resource bundle for locale: {}", locale, e);
            throw new IllegalArgumentException("Locale not supported: " + locale, e);
        }
    }

    /**
     * Get available locales.
     * <p>
     * Returns locales for which resource bundles exist.
     * </p>
     *
     * @return array of supported locales
     */
    public static Locale[] getAvailableLocales() {
        return new Locale[]{
            Locale.ENGLISH,
            Locale.forLanguageTag("es"),  // Spanish
            Locale.forLanguageTag("fr"),  // French
            Locale.forLanguageTag("de")   // German
        };
    }

    /**
     * Private constructor to prevent instantiation.
     */
    private Messages() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
}

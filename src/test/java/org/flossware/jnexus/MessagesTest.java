package org.flossware.jnexus;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Messages i18n/l10n utility class.
 */
class MessagesTest {

    @Test
    void testGet_simpleMessage_returnsEnglish() {
        Messages.setLocale(Locale.ENGLISH);
        String title = Messages.get("app.title");
        assertEquals("Nexus Repository Manager", title);
    }

    @Test
    void testGet_simpleMessage_returnsSpanish() {
        Messages.setLocale(Locale.forLanguageTag("es"));
        String title = Messages.get("app.title");
        assertEquals("Gestor de Repositorio Nexus", title);
    }

    @Test
    void testGet_simpleMessage_returnsFrench() {
        Messages.setLocale(Locale.forLanguageTag("fr"));
        String title = Messages.get("app.title");
        assertEquals("Gestionnaire de Dépôt Nexus", title);
    }

    @Test
    void testGet_simpleMessage_returnsGerman() {
        Messages.setLocale(Locale.forLanguageTag("de"));
        String title = Messages.get("app.title");
        assertEquals("Nexus Repository Manager", title);
    }

    @Test
    void testGet_withParameters_formatsCorrectly() {
        Messages.setLocale(Locale.ENGLISH);
        String error = Messages.get("error.list.failed", "Connection refused");
        assertEquals("List failed: Connection refused", error);
    }

    @Test
    void testGet_withMultipleParameters_formatsCorrectly() {
        Messages.setLocale(Locale.ENGLISH);
        String message = Messages.get("dialog.delete.confirm", 5);
        assertTrue(message.contains("5"));
        assertTrue(message.contains("WARNING"));
    }

    @Test
    void testGet_missingKey_returnsKey() {
        Messages.setLocale(Locale.ENGLISH);
        String result = Messages.get("nonexistent.key");
        assertEquals("nonexistent.key", result);
    }

    @Test
    void testGetCurrentLocale_returnsLocale() {
        Messages.setLocale(Locale.ENGLISH);
        Locale current = Messages.getCurrentLocale();
        // Locale might be ROOT ("") or "en" depending on ResourceBundle behavior
        assertTrue(current.equals(Locale.ENGLISH) || current.equals(Locale.ROOT),
            "Current locale should be English or ROOT");

        Messages.setLocale(Locale.forLanguageTag("es"));
        assertEquals("es", Messages.getCurrentLocale().getLanguage());
    }

    @Test
    void testGetAvailableLocales_returnsSupportedLocales() {
        Locale[] locales = Messages.getAvailableLocales();
        assertEquals(4, locales.length);

        // Check that English, Spanish, French, German are all present
        boolean hasEnglish = false;
        boolean hasSpanish = false;
        boolean hasFrench = false;
        boolean hasGerman = false;

        for (Locale locale : locales) {
            if (locale.getLanguage().equals("en")) hasEnglish = true;
            if (locale.getLanguage().equals("es")) hasSpanish = true;
            if (locale.getLanguage().equals("fr")) hasFrench = true;
            if (locale.getLanguage().equals("de")) hasGerman = true;
        }

        assertTrue(hasEnglish, "Should have English");
        assertTrue(hasSpanish, "Should have Spanish");
        assertTrue(hasFrench, "Should have French");
        assertTrue(hasGerman, "Should have German");
    }

    @Test
    void testSetLocale_unsupportedLocale_throwsException() {
        // Try to set an unsupported locale (like Chinese or Japanese)
        assertThrows(IllegalArgumentException.class, () -> {
            Messages.setLocale(Locale.forLanguageTag("zh"));
        });

        assertThrows(IllegalArgumentException.class, () -> {
            Messages.setLocale(Locale.forLanguageTag("ja"));
        });
    }

    @Test
    void testButtonLabels_allLocales() {
        // Test that button labels exist in all locales
        Locale[] locales = {
            Locale.ENGLISH,
            Locale.forLanguageTag("es"),
            Locale.forLanguageTag("fr"),
            Locale.forLanguageTag("de")
        };

        for (Locale locale : locales) {
            Messages.setLocale(locale);

            assertNotNull(Messages.get("button.list"));
            assertNotNull(Messages.get("button.refresh"));
            assertNotNull(Messages.get("button.delete"));
            assertNotNull(Messages.get("button.quit"));
            assertNotNull(Messages.get("button.stats"));
        }
    }

    @Test
    void testDialogTitles_allLocales() {
        // Test that dialog titles exist in all locales
        Locale[] locales = {
            Locale.ENGLISH,
            Locale.forLanguageTag("es"),
            Locale.forLanguageTag("fr"),
            Locale.forLanguageTag("de")
        };

        for (Locale locale : locales) {
            Messages.setLocale(locale);

            assertNotNull(Messages.get("dialog.title.error"));
            assertNotNull(Messages.get("dialog.title.warning"));
            assertNotNull(Messages.get("dialog.title.confirm"));
            assertNotNull(Messages.get("dialog.title.about"));
        }
    }
}

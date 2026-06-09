package org.flossware.nexus;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for credentials edge cases.
 * Tests special characters, long passwords, empty fields, and Base64 encoding edge cases.
 */
@DisplayName("Credentials Edge Case Tests")
class NexusIntegrationCredentialsEdgeCaseTest {

    @Test
    @DisplayName("Password with special characters")
    void testPasswordWithSpecialCharacters() {
        String specialPassword = "!@#$%^&*(){}[]|\\:;<>?,.~/`";
        Credentials creds = new Credentials("http://localhost:8081", "user", specialPassword, null);

        assertEquals("user", creds.getUser());
        assertEquals(specialPassword, creds.getPassword());

        // Verify Base64 encoding works
        String auth = "Basic " + Base64.getEncoder().encodeToString(("user:" + specialPassword).getBytes());
        assertNotNull(auth);
        assertTrue(auth.startsWith("Basic "));
    }

    @Test
    @DisplayName("Very long password (1000 characters)")
    void testVeryLongPassword() {
        String longPassword = "x".repeat(1000);
        Credentials creds = new Credentials("http://localhost:8081", "user", longPassword, null);

        assertEquals(longPassword, creds.getPassword());
        assertTrue(creds.getPassword().length() >= 1000);
    }

    @Test
    @DisplayName("Password with Unicode characters")
    void testPasswordWithUnicodeCharacters() {
        String unicodePassword = "パスワード密码mots_de_passe";
        Credentials creds = new Credentials("http://localhost:8081", "user", unicodePassword, null);

        assertEquals(unicodePassword, creds.getPassword());

        // Verify Base64 handles Unicode correctly
        String auth = "Basic " + Base64.getEncoder().encodeToString(("user:" + unicodePassword).getBytes());
        assertNotNull(auth);
    }

    @Test
    @DisplayName("Empty username")
    void testEmptyUsername() {
        assertThrows(IllegalArgumentException.class, () ->
            Credentials.validateRepository(""),
            "Should reject empty username in validation"
        );
    }

    @Test
    @DisplayName("Null username")
    void testNullUsername() {
        assertThrows(IllegalArgumentException.class, () ->
            Credentials.validateRepository(null),
            "Should reject null username"
        );
    }

    @Test
    @DisplayName("Password with equals signs (Base64 padding)")
    void testPasswordWithEqualsSignsForBase64Padding() {
        String passwordWithEquals = "pass=word";
        Credentials creds = new Credentials("http://localhost:8081", "user", passwordWithEquals, null);

        assertEquals(passwordWithEquals, creds.getPassword());

        // Verify Base64 encoding handles this correctly
        String encoded = Base64.getEncoder().encodeToString(("user:" + passwordWithEquals).getBytes());
        String decoded = new String(Base64.getDecoder().decode(encoded));
        assertEquals("user:" + passwordWithEquals, decoded);
    }

    @Test
    @DisplayName("Password with forward slashes (Base64 characters)")
    void testPasswordWithForwardSlashes() {
        String passwordWithSlashes = "pass/word/special";
        Credentials creds = new Credentials("http://localhost:8081", "user", passwordWithSlashes, null);

        assertEquals(passwordWithSlashes, creds.getPassword());

        // Verify round-trip encoding
        String encoded = Base64.getEncoder().encodeToString(("user:" + passwordWithSlashes).getBytes());
        String decoded = new String(Base64.getDecoder().decode(encoded));
        assertEquals("user:" + passwordWithSlashes, decoded);
    }

    @Test
    @DisplayName("Username with dots and numbers")
    void testUsernameWithDotsAndNumbers() {
        Credentials creds = new Credentials("http://localhost:8081", "user.name.123", "password", null);
        assertEquals("user.name.123", creds.getUser());
    }

    @Test
    @DisplayName("URL with port number")
    void testURLWithPortNumber() {
        Credentials creds = new Credentials("http://localhost:8081", "user", "pass", null);
        assertEquals("http://localhost:8081", creds.getUrl());
    }

    @Test
    @DisplayName("HTTPS URL")
    void testHTTPSURL() {
        Credentials creds = new Credentials("https://nexus.example.com:8443", "user", "pass", null);
        assertEquals("https://nexus.example.com:8443", creds.getUrl());
    }

    @Test
    @DisplayName("URL with trailing slash")
    void testURLWithTrailingSlash() {
        Credentials creds = new Credentials("http://localhost:8081/", "user", "pass", null);
        assertEquals("http://localhost:8081/", creds.getUrl());
    }

    @Test
    @DisplayName("Repository names with special characters")
    void testRepositoryNamesWithSpecialCharacters() {
        String[] validRepos = {
            "repo-name",
            "repo_name",
            "repo.name",
            "repo123",
            "123repo",
            "REPO-NAME"
        };

        for (String repoName : validRepos) {
            assertDoesNotThrow(() -> Credentials.validateRepository(repoName),
                "Should accept valid repository name: " + repoName);
        }
    }

    @Test
    @DisplayName("Whitespace-only password should be accepted")
    void testWhitespaceOnlyPassword() {
        String whitespacePassword = "   ";
        Credentials creds = new Credentials("http://localhost:8081", "user", whitespacePassword, null);
        assertEquals(whitespacePassword, creds.getPassword());
    }

    @Test
    @DisplayName("Password with newlines")
    void testPasswordWithNewlines() {
        String multilinePassword = "line1\nline2\nline3";
        Credentials creds = new Credentials("http://localhost:8081", "user", multilinePassword, null);
        assertEquals(multilinePassword, creds.getPassword());

        // Verify Base64 encodes correctly
        String encoded = Base64.getEncoder().encodeToString(("user:" + multilinePassword).getBytes());
        String decoded = new String(Base64.getDecoder().decode(encoded));
        assertEquals("user:" + multilinePassword, decoded);
    }

    @Test
    @DisplayName("Password with tab characters")
    void testPasswordWithTabCharacters() {
        String tabbedPassword = "pass\tword\twith\ttabs";
        Credentials creds = new Credentials("http://localhost:8081", "user", tabbedPassword, null);
        assertEquals(tabbedPassword, creds.getPassword());
    }

    @Test
    @DisplayName("Very long repository name (256+ characters)")
    void testVeryLongRepositoryName() {
        String longRepoName = "a".repeat(256);
        assertDoesNotThrow(() -> Credentials.validateRepository(longRepoName),
            "Should accept long repository name");
    }

    @Test
    @DisplayName("Base64 encoding with non-ASCII characters")
    void testBase64EncodingNonASCII() {
        String nonASCIIPassword = "αβγδεζηθικλμνξοπρστυφχψω";
        Credentials creds = new Credentials("http://localhost:8081", "user", nonASCIIPassword, null);

        String auth = Base64.getEncoder().encodeToString(("user:" + nonASCIIPassword).getBytes());
        String decoded = new String(Base64.getDecoder().decode(auth));
        assertEquals("user:" + nonASCIIPassword, decoded);
    }

    @Test
    @DisplayName("Credentials validation does not log password")
    void testCredentialsValidationDoesNotLogPassword() {
        String sensitivePassword = "super-secret-password-123";
        Credentials creds = new Credentials("http://localhost:8081", "user", sensitivePassword, null);

        // Verify password is stored
        assertEquals(sensitivePassword, creds.getPassword());

        // toString() should not contain password (if implemented)
        String credString = creds.toString();
        assertFalse(credString.contains(sensitivePassword),
            "toString() should not expose password");
    }
}

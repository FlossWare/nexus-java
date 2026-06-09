package org.flossware.nexus;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration edge case tests for Credentials and authentication behavior.
 * <p>
 * Covers scenarios from issue #67:
 * - Password with special characters in Base64 encoding
 * - Very long password
 * - Empty username/password validation
 * - URL validation edge cases
 * - Repository name validation edge cases
 * - Credentials with all special characters
 * </p>
 */
@DisplayName("Credentials Integration Edge Case Tests")
class CredentialsIntegrationEdgeCaseTest {

    private HttpServer server;
    private int port;

    @BeforeEach
    void setUp() throws IOException {
        // Use dynamic port allocation to avoid conflicts during parallel test execution
        try (java.net.ServerSocket ss = new java.net.ServerSocket(0)) {
            port = ss.getLocalPort();
        }
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(null);
        server.start();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("Password with all special characters should be Base64 encoded correctly")
    void testPasswordWithSpecialCharacters() throws Exception {
        String specialPassword = "!@#$%^&*(){}[]|\\:;<>?,.~/`";
        AtomicReference<String> capturedAuth = new AtomicReference<>();

        server.createContext("/service/rest/v1/components", exchange -> {
            capturedAuth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            String response = "{\"items\":[]}";
            byte[] bytes = response.getBytes();
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        Credentials credentials = new Credentials(
            "http://localhost:" + port, "user", specialPassword, null);
        NexusClient client = new NexusClient(credentials, 0);

        client.listComponents("test-repo");

        String auth = capturedAuth.get();
        assertNotNull(auth, "Authorization header should be present");
        assertTrue(auth.startsWith("Basic "), "Should use Basic auth");

        String decoded = new String(Base64.getDecoder().decode(auth.substring(6)));
        assertEquals("user:" + specialPassword, decoded,
            "Special characters should be preserved in Base64 encoding");
    }

    @Test
    @DisplayName("Very long password (1000+ characters) should not be truncated")
    void testVeryLongPassword() throws Exception {
        StringBuilder longPassword = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            longPassword.append("abcde"); // 1000 characters
        }
        String password = longPassword.toString();

        AtomicReference<String> capturedAuth = new AtomicReference<>();

        server.createContext("/service/rest/v1/components", exchange -> {
            capturedAuth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            String response = "{\"items\":[]}";
            byte[] bytes = response.getBytes();
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        Credentials credentials = new Credentials(
            "http://localhost:" + port, "user", password, null);
        NexusClient client = new NexusClient(credentials, 0);

        client.listComponents("test-repo");

        String auth = capturedAuth.get();
        String decoded = new String(Base64.getDecoder().decode(auth.substring(6)));
        assertEquals("user:" + password, decoded,
            "Long password should not be truncated");
        assertEquals(1005, decoded.length(),
            "Decoded auth should be 'user:' + 1000 char password");
    }

    @Test
    @DisplayName("Empty username should be rejected")
    void testEmptyUsername() {
        assertThrows(IllegalArgumentException.class, () ->
            new Credentials("http://localhost:" + port, "", "password", null),
            "Empty username should be rejected"
        );
    }

    @Test
    @DisplayName("Blank username should be rejected")
    void testBlankUsername() {
        assertThrows(IllegalArgumentException.class, () ->
            new Credentials("http://localhost:" + port, "   ", "password", null),
            "Blank username should be rejected"
        );
    }

    @Test
    @DisplayName("Null username should be rejected")
    void testNullUsername() {
        assertThrows(IllegalArgumentException.class, () ->
            new Credentials("http://localhost:" + port, null, "password", null),
            "Null username should be rejected"
        );
    }

    @Test
    @DisplayName("Empty password should be rejected")
    void testEmptyPassword() {
        assertThrows(IllegalArgumentException.class, () ->
            new Credentials("http://localhost:" + port, "user", "", null),
            "Empty password should be rejected"
        );
    }

    @Test
    @DisplayName("Blank password should be rejected")
    void testBlankPassword() {
        assertThrows(IllegalArgumentException.class, () ->
            new Credentials("http://localhost:" + port, "user", "   ", null),
            "Blank password should be rejected"
        );
    }

    @Test
    @DisplayName("Null password should be rejected")
    void testNullPassword() {
        assertThrows(IllegalArgumentException.class, () ->
            new Credentials("http://localhost:" + port, "user", null, null),
            "Null password should be rejected"
        );
    }

    @Test
    @DisplayName("URL with trailing slash should be accepted")
    void testUrlWithTrailingSlash() {
        assertDoesNotThrow(() ->
            new Credentials("http://localhost:" + port + "/", "user", "pass", null),
            "URL with trailing slash should be accepted"
        );
    }

    @Test
    @DisplayName("URL with path should be accepted")
    void testUrlWithPath() {
        assertDoesNotThrow(() ->
            new Credentials("http://localhost:" + port + "/nexus", "user", "pass", null),
            "URL with path should be accepted"
        );
    }

    @Test
    @DisplayName("URL without scheme should be rejected")
    void testUrlWithoutScheme() {
        assertThrows(IllegalArgumentException.class, () ->
            new Credentials("localhost:" + port, "user", "pass", null),
            "URL without scheme should be rejected"
        );
    }

    @Test
    @DisplayName("Repository name with leading dot should be rejected")
    void testRepositoryNameLeadingDot() {
        assertThrows(IllegalArgumentException.class, () ->
            Credentials.validateRepository(".hidden-repo"),
            "Leading dot should be rejected"
        );
    }

    @Test
    @DisplayName("Repository name with trailing hyphen should be rejected")
    void testRepositoryNameTrailingHyphen() {
        assertThrows(IllegalArgumentException.class, () ->
            Credentials.validateRepository("repo-"),
            "Trailing hyphen should be rejected"
        );
    }

    @Test
    @DisplayName("Repository name with double dots should be rejected")
    void testRepositoryNameDoubleDots() {
        assertThrows(IllegalArgumentException.class, () ->
            Credentials.validateRepository("repo..name"),
            "Double dots should be rejected (path traversal)"
        );
    }

    @Test
    @DisplayName("Valid repository names with dots, hyphens, underscores should be accepted")
    void testValidRepositoryNames() {
        assertDoesNotThrow(() -> Credentials.validateRepository("repo.name"));
        assertDoesNotThrow(() -> Credentials.validateRepository("repo-name"));
        assertDoesNotThrow(() -> Credentials.validateRepository("repo_name"));
        assertDoesNotThrow(() -> Credentials.validateRepository("repo123"));
        assertDoesNotThrow(() -> Credentials.validateRepository("a"));
        assertDoesNotThrow(() -> Credentials.validateRepository("my-repo.v2"));
    }

    @Test
    @DisplayName("Repositories list parsing should handle various formats")
    void testRepositoriesListParsing() {
        Credentials credentials = new Credentials(
            "http://localhost:" + port, "user", "pass",
            "maven-releases, maven-snapshots , npm-public,docker"
        );

        assertEquals(4, credentials.getRepositories().size(),
            "Should parse 4 repositories");
        assertEquals("maven-releases", credentials.getRepositories().get(0));
        assertEquals("maven-snapshots", credentials.getRepositories().get(1));
        assertEquals("npm-public", credentials.getRepositories().get(2));
        assertEquals("docker", credentials.getRepositories().get(3));
    }

    @Test
    @DisplayName("Empty repositories string should result in empty list")
    void testEmptyRepositoriesString() {
        Credentials credentials = new Credentials(
            "http://localhost:" + port, "user", "pass", ""
        );

        assertTrue(credentials.getRepositories().isEmpty(),
            "Empty string should result in empty list");
    }

    @Test
    @DisplayName("Null repositories string should result in empty list")
    void testNullRepositoriesString() {
        Credentials credentials = new Credentials(
            "http://localhost:" + port, "user", "pass", null
        );

        assertTrue(credentials.getRepositories().isEmpty(),
            "Null should result in empty list");
    }

    @Test
    @DisplayName("Password with colon should be Base64 encoded correctly")
    void testPasswordWithColon() throws Exception {
        String passwordWithColon = "pass:word:with:colons";
        AtomicReference<String> capturedAuth = new AtomicReference<>();

        server.createContext("/service/rest/v1/components", exchange -> {
            capturedAuth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            String response = "{\"items\":[]}";
            byte[] bytes = response.getBytes();
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        Credentials credentials = new Credentials(
            "http://localhost:" + port, "user", passwordWithColon, null);
        NexusClient client = new NexusClient(credentials, 0);

        client.listComponents("test-repo");

        String auth = capturedAuth.get();
        String decoded = new String(Base64.getDecoder().decode(auth.substring(6)));
        assertEquals("user:" + passwordWithColon, decoded,
            "Password with colons should be preserved (only first colon separates user:pass)");
    }

    @Test
    @DisplayName("Username with Unicode characters should be Base64 encoded correctly")
    void testUnicodeUsername() throws Exception {
        String unicodeUser = "user-测试";
        AtomicReference<String> capturedAuth = new AtomicReference<>();

        server.createContext("/service/rest/v1/components", exchange -> {
            capturedAuth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            String response = "{\"items\":[]}";
            byte[] bytes = response.getBytes();
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        Credentials credentials = new Credentials(
            "http://localhost:" + port, unicodeUser, "password", null);
        NexusClient client = new NexusClient(credentials, 0);

        client.listComponents("test-repo");

        String auth = capturedAuth.get();
        assertNotNull(auth, "Authorization header should be present");
        String decoded = new String(Base64.getDecoder().decode(auth.substring(6)));
        assertEquals(unicodeUser + ":password", decoded,
            "Unicode username should be preserved in Base64 encoding");
    }
}

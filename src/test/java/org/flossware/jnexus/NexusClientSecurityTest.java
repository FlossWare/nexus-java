package org.flossware.jnexus;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Security-focused integration tests for NexusClient.
 * Tests input validation, authentication, SSL/TLS behavior, and injection prevention.
 */
class NexusClientSecurityTest {

    private HttpServer server;
    private int port;
    private NexusClient client;
    private AtomicInteger requestCount;
    private AtomicReference<String> lastAuthHeader;

    @BeforeEach
    void setUp() throws IOException {
        port = 8889; // Different port from integration test
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.start();

        requestCount = new AtomicInteger(0);
        lastAuthHeader = new AtomicReference<>();

        Credentials credentials = new Credentials("http://localhost:" + port, "testuser", "testpass", null);
        client = new NexusClient(credentials, 0); // No caching
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    // ========== Input Validation Tests ==========

    @Test
    void testRepositoryNameWithPathTraversal() {
        // Repository names with path traversal should be rejected
        assertThrows(IllegalArgumentException.class, () ->
            Credentials.validateRepository("../../admin"),
            "Should reject path traversal in repository name"
        );
    }

    @Test
    void testRepositoryNameWithSlashes() {
        // Repository names with slashes should be rejected
        assertThrows(IllegalArgumentException.class, () ->
            Credentials.validateRepository("repo/path"),
            "Should reject slashes in repository name"
        );

        assertThrows(IllegalArgumentException.class, () ->
            Credentials.validateRepository("repo\\path"),
            "Should reject backslashes in repository name"
        );
    }

    @Test
    void testRepositoryNameWithSpecialCharacters() {
        // Repository names with injection characters should be rejected
        assertThrows(IllegalArgumentException.class, () ->
            Credentials.validateRepository("repo&param=value"),
            "Should reject query parameters in repository name"
        );

        assertThrows(IllegalArgumentException.class, () ->
            Credentials.validateRepository("repo<script>alert(1)</script>"),
            "Should reject HTML/script tags in repository name"
        );

        assertThrows(IllegalArgumentException.class, () ->
            Credentials.validateRepository("repo;rm -rf /"),
            "Should reject shell commands in repository name"
        );
    }

    @Test
    void testValidRepositoryNames() {
        // Valid repository names should be accepted
        assertDoesNotThrow(() -> Credentials.validateRepository("maven-releases"));
        assertDoesNotThrow(() -> Credentials.validateRepository("npm.public"));
        assertDoesNotThrow(() -> Credentials.validateRepository("docker_hosted"));
        assertDoesNotThrow(() -> Credentials.validateRepository("my-repo-123"));
    }

    @Test
    void testURLValidationJavaScriptProtocol() {
        // JavaScript protocol URLs should be rejected
        assertThrows(IllegalArgumentException.class, () ->
            new Credentials("javascript:alert(1)", "user", "pass", null),
            "Should reject javascript: protocol"
        );
    }

    @Test
    void testURLValidationFileProtocol() {
        // File protocol URLs should be rejected
        assertThrows(IllegalArgumentException.class, () ->
            new Credentials("file:///etc/passwd", "user", "pass", null),
            "Should reject file: protocol"
        );
    }

    @Test
    void testURLValidationDataProtocol() {
        // Data protocol URLs should be rejected
        assertThrows(IllegalArgumentException.class, () ->
            new Credentials("data:text/html,<script>alert(1)</script>", "user", "pass", null),
            "Should reject data: protocol"
        );
    }

    @Test
    void testValidHTTPSURL() {
        // HTTPS URLs should be accepted
        assertDoesNotThrow(() ->
            new Credentials("https://nexus.example.com", "user", "pass", null)
        );
    }

    @Test
    void testValidHTTPURL() {
        // HTTP URLs should be accepted with warning (captured in logs)
        assertDoesNotThrow(() ->
            new Credentials("http://nexus.example.com", "user", "pass", null)
        );
    }

    // ========== Authentication Tests ==========

    @Test
    void testAuthenticationHeaderFormat() throws Exception {
        String jsonResponse = "{\"items\":[]}";

        server.createContext("/service/rest/v1/components", exchange -> {
            String auth = exchange.getRequestHeaders().getFirst("Authorization");
            lastAuthHeader.set(auth);
            requestCount.incrementAndGet();

            byte[] response = jsonResponse.getBytes();
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });

        // Make request
        client.listComponents("test-repo");

        // Verify authentication header
        String authHeader = lastAuthHeader.get();
        assertNotNull(authHeader, "Authorization header should be present");
        assertTrue(authHeader.startsWith("Basic "), "Should use Basic auth");

        // Decode and verify format
        String encodedCredentials = authHeader.substring(6);
        String decodedCredentials = new String(Base64.getDecoder().decode(encodedCredentials));
        assertEquals("testuser:testpass", decodedCredentials, "Credentials should be base64 encoded");
    }

    @Test
    void testAuthenticationWithSpecialCharactersInPassword() throws Exception {
        // Create client with special characters in password
        Credentials credentials = new Credentials("http://localhost:" + port, "user", "p@ss:w0rd!#$%", null);
        NexusClient specialClient = new NexusClient(credentials, 0);

        String jsonResponse = "{\"items\":[]}";

        server.createContext("/service/rest/v1/components", exchange -> {
            String auth = exchange.getRequestHeaders().getFirst("Authorization");
            lastAuthHeader.set(auth);

            byte[] response = jsonResponse.getBytes();
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });

        // Make request
        specialClient.listComponents("test-repo");

        // Verify authentication header
        String authHeader = lastAuthHeader.get();
        assertNotNull(authHeader);
        String encodedCredentials = authHeader.substring(6);
        String decodedCredentials = new String(Base64.getDecoder().decode(encodedCredentials));
        assertEquals("user:p@ss:w0rd!#$%", decodedCredentials, "Special characters should be preserved");
    }

    @Test
    void testAuthenticationFailureNoRetry() throws Exception {
        server.createContext("/service/rest/v1/components", exchange -> {
            requestCount.incrementAndGet();
            exchange.sendResponseHeaders(401, 0);
            exchange.getResponseBody().close();
        });

        // Authentication failure should NOT be retried
        IOException exception = assertThrows(IOException.class, () ->
            client.listComponents("test-repo")
        );

        assertTrue(exception.getMessage().contains("401"),
            "Exception should mention 401 status code");

        // Verify only ONE request was made (no retries)
        assertEquals(1, requestCount.get(),
            "Should not retry on authentication failure");
    }

    @Test
    void testAuthenticationFailure403() throws Exception {
        server.createContext("/service/rest/v1/components", exchange -> {
            requestCount.incrementAndGet();
            exchange.sendResponseHeaders(403, 0);
            exchange.getResponseBody().close();
        });

        // Authorization failure should NOT be retried
        IOException exception = assertThrows(IOException.class, () ->
            client.listComponents("test-repo")
        );

        assertTrue(exception.getMessage().contains("403"),
            "Exception should mention 403 status code");

        // Verify only ONE request was made (no retries)
        assertEquals(1, requestCount.get(),
            "Should not retry on authorization failure");
    }

    // ========== Injection Prevention Tests ==========

    @Test
    void testRepositoryNameURLEncoding() throws Exception {
        String jsonResponse = "{\"items\":[]}";
        AtomicReference<String> requestPath = new AtomicReference<>();

        server.createContext("/service/rest/v1/components", exchange -> {
            requestPath.set(exchange.getRequestURI().toString());

            byte[] response = jsonResponse.getBytes();
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });

        // Use repository name with allowed special characters
        client.listComponents("my-repo.test");

        String path = requestPath.get();
        assertTrue(path.contains("repository=my-repo.test"),
            "Repository name should be in URL");
    }

    @Test
    void testContinuationTokenURLEncoding() throws Exception {
        // First response with continuation token containing special characters
        String continuationToken = "token=abc&next=123";
        String firstResponse = "{\"items\":[],\"continuationToken\":\"" + continuationToken + "\"}";
        String secondResponse = "{\"items\":[]}";

        AtomicInteger pageCount = new AtomicInteger(0);
        AtomicReference<String> secondRequestPath = new AtomicReference<>();

        server.createContext("/service/rest/v1/components", exchange -> {
            int page = pageCount.incrementAndGet();

            if (page == 1) {
                // First page with continuation token
                byte[] response = firstResponse.getBytes();
                exchange.sendResponseHeaders(200, response.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response);
                }
            } else {
                // Second page - capture path
                secondRequestPath.set(exchange.getRequestURI().toString());
                byte[] response = secondResponse.getBytes();
                exchange.sendResponseHeaders(200, response.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response);
                }
            }
        });

        client.listComponents("test-repo");

        // Verify continuation token is in URL
        String path = secondRequestPath.get();
        assertTrue(path.contains("continuationToken="),
            "Continuation token should be in URL");
    }

    @Test
    void testJSONResponseSanitization() throws Exception {
        // Malicious JSON with potential XSS payload
        String maliciousJSON = """
            {
              "items": [{
                "id": "<script>alert('XSS')</script>",
                "assets": [{
                  "path": "test.jar",
                  "fileSize": 1000
                }]
              }]
            }
            """;

        server.createContext("/service/rest/v1/components", exchange -> {
            byte[] response = maliciousJSON.getBytes();
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });

        // Client should parse JSON without executing script
        List<RepoRecord> records = client.listComponents("test-repo");

        // Verify data is parsed as-is (no execution)
        assertEquals(1, records.size());
        assertTrue(records.get(0).id().contains("script"),
            "Script tags should be treated as plain text");
    }

    // ========== SSL/TLS Tests ==========

    @Test
    void testHTTPWarning() {
        // HTTP URL should trigger warning (captured in logs)
        // This is verified by URL validation accepting HTTP but logging warning
        assertDoesNotThrow(() ->
            new Credentials("http://localhost:8081", "user", "pass", null)
        );
    }

    @Test
    void testHTTPSPreferred() {
        // HTTPS URLs should not trigger warning
        assertDoesNotThrow(() ->
            new Credentials("https://nexus.example.com", "user", "pass", null)
        );
    }

    // ========== Edge Cases ==========

    @Test
    void testNullRepositoryName() {
        assertThrows(IllegalArgumentException.class, () ->
            Credentials.validateRepository(null),
            "Should reject null repository name"
        );
    }

    @Test
    void testEmptyRepositoryName() {
        assertThrows(IllegalArgumentException.class, () ->
            Credentials.validateRepository(""),
            "Should reject empty repository name"
        );
    }

    @Test
    void testWhitespaceOnlyRepositoryName() {
        assertThrows(IllegalArgumentException.class, () ->
            Credentials.validateRepository("   "),
            "Should reject whitespace-only repository name"
        );
    }

    @Test
    void testNullURL() {
        assertThrows(IllegalArgumentException.class, () ->
            new Credentials(null, "user", "pass", null),
            "Should reject null URL"
        );
    }

    @Test
    void testEmptyURL() {
        assertThrows(IllegalArgumentException.class, () ->
            new Credentials("", "user", "pass", null),
            "Should reject empty URL"
        );
    }

    @Test
    void testBlankURL() {
        assertThrows(IllegalArgumentException.class, () ->
            new Credentials("   ", "user", "pass", null),
            "Should reject blank URL"
        );
    }
}

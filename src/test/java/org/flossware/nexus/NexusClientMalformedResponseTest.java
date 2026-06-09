package org.flossware.nexus;

import com.sun.net.httpserver.HttpServer;
import org.flossware.jnexus.ComponentMetadata;
import org.flossware.jnexus.RepoRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for malformed response handling in NexusClient.
 * <p>
 * Covers scenarios from issue #67:
 * - Unexpected null values in JSON
 * - Unicode and special characters in paths
 * - Extra unexpected fields in JSON
 * - Missing optional metadata fields
 * - Invalid date formats in metadata
 * - Components with empty/missing assets
 * - Mixed valid and invalid components
 * </p>
 */
@DisplayName("NexusClient Malformed Response Handling Tests")
class NexusClientMalformedResponseTest {

    private HttpServer server;
    private int port;
    private NexusClient client;

    @BeforeEach
    void setUp() throws IOException {
        // Use dynamic port allocation to avoid conflicts during parallel test execution
        try (ServerSocket ss = new ServerSocket(0)) {
            port = ss.getLocalPort();
        }
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(null);
        server.start();

        Credentials credentials = new Credentials("http://localhost:" + port, "testuser", "testpass", null);
        client = new NexusClient(credentials, 0);
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("JSON with extra unexpected fields should be parsed gracefully")
    void testExtraUnexpectedFields() throws Exception {
        String jsonResponse = """
            {
                "items": [{
                    "id": "comp1",
                    "unexpectedField": "some-value",
                    "anotherUnknown": 12345,
                    "assets": [{
                        "path": "test.jar",
                        "fileSize": 1000,
                        "unknownAssetField": true
                    }]
                }],
                "extraTopLevel": "ignored"
            }
            """;

        server.createContext("/service/rest/v1/components", exchange -> {
            byte[] response = jsonResponse.getBytes();
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });

        List<RepoRecord> records = client.listComponents("test-repo");
        assertEquals(1, records.size(), "Should parse valid fields and ignore unknown ones");
        assertEquals("comp1", records.get(0).id());
        assertEquals("test.jar", records.get(0).path());
        assertEquals(1000, records.get(0).fileSize());
    }

    @Test
    @DisplayName("Unicode characters in component paths should be preserved")
    void testUnicodeAndSpecialCharactersInPath() throws Exception {
        String jsonResponse = """
            {
                "items": [
                    {"id": "unicode1", "assets": [{"path": "com/example/测试-文件.jar", "fileSize": 100}]},
                    {"id": "unicode2", "assets": [{"path": "com/example/файл.jar", "fileSize": 200}]},
                    {"id": "emoji1", "assets": [{"path": "com/example/rocket🚀.jar", "fileSize": 300}]},
                    {"id": "rtl1", "assets": [{"path": "com/example/ملف.jar", "fileSize": 400}]}
                ]
            }
            """;

        server.createContext("/service/rest/v1/components", exchange -> {
            byte[] response = jsonResponse.getBytes();
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });

        List<RepoRecord> records = client.listComponents("test-repo");
        assertEquals(4, records.size(), "Should parse all Unicode paths");

        // Chinese characters
        assertTrue(records.get(0).path().contains("测试"),
            "Chinese characters should be preserved");
        // Cyrillic characters
        assertTrue(records.get(1).path().contains("файл"),
            "Cyrillic characters should be preserved");
        // RTL Arabic characters
        assertTrue(records.get(3).path().contains("ملف"),
            "RTL characters should be preserved");
    }

    @Test
    @DisplayName("Component with multiple assets should use first asset only")
    void testMultipleAssetsUsesFirstOnly() throws Exception {
        String jsonResponse = """
            {
                "items": [{
                    "id": "multi-asset",
                    "assets": [
                        {"path": "artifact.jar", "fileSize": 5000},
                        {"path": "artifact.pom", "fileSize": 1000},
                        {"path": "artifact-sources.jar", "fileSize": 3000}
                    ]
                }]
            }
            """;

        server.createContext("/service/rest/v1/components", exchange -> {
            byte[] response = jsonResponse.getBytes();
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });

        List<RepoRecord> records = client.listComponents("test-repo");
        assertEquals(1, records.size(), "Should return single record for multi-asset component");
        assertEquals("artifact.jar", records.get(0).path(), "Should use first asset path");
        assertEquals(5000, records.get(0).fileSize(), "Should use first asset file size");
    }

    @Test
    @DisplayName("Mixed valid and empty-assets components should skip empty ones")
    void testMixedValidAndEmptyAssetsComponents() throws Exception {
        String jsonResponse = """
            {
                "items": [
                    {"id": "valid1", "assets": [{"path": "test1.jar", "fileSize": 1000}]},
                    {"id": "empty1", "assets": []},
                    {"id": "valid2", "assets": [{"path": "test2.jar", "fileSize": 2000}]},
                    {"id": "empty2", "assets": []}
                ]
            }
            """;

        server.createContext("/service/rest/v1/components", exchange -> {
            byte[] response = jsonResponse.getBytes();
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });

        List<RepoRecord> records = client.listComponents("test-repo");
        assertEquals(2, records.size(), "Should skip components with empty assets");
        assertEquals("valid1", records.get(0).id());
        assertEquals("valid2", records.get(1).id());
    }

    @Test
    @DisplayName("Metadata with missing optional fields should use defaults")
    void testMetadataMissingOptionalFields() throws Exception {
        String jsonResponse = """
            {
                "items": [{
                    "id": "minimal",
                    "assets": [{
                        "path": "minimal.jar",
                        "fileSize": 500
                    }]
                }]
            }
            """;

        server.createContext("/service/rest/v1/components", exchange -> {
            byte[] response = jsonResponse.getBytes();
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });

        List<ComponentMetadata> metadata = client.listComponentsWithMetadata("test-repo");
        assertEquals(1, metadata.size());

        ComponentMetadata comp = metadata.get(0);
        assertEquals("minimal", comp.id());
        assertEquals("minimal.jar", comp.path());
        assertEquals(500, comp.fileSize());
        assertEquals("unknown", comp.contentType(), "Missing contentType should default to 'unknown'");
        assertEquals("unknown", comp.format(), "Missing format should default to 'unknown'");
        assertNull(comp.createdDate(), "Missing blobCreated should be null");
        assertNull(comp.lastModified(), "Missing lastModified should be null");
        assertNull(comp.checksum(), "Missing checksum should be null");
    }

    @Test
    @DisplayName("Metadata with invalid date format should handle gracefully")
    void testMetadataInvalidDateFormat() throws Exception {
        String jsonResponse = """
            {
                "items": [{
                    "id": "bad-date",
                    "format": "maven2",
                    "assets": [{
                        "path": "bad-date.jar",
                        "fileSize": 1000,
                        "contentType": "application/java-archive",
                        "blobCreated": "not-a-date",
                        "lastModified": "2024-13-45T99:99:99Z"
                    }]
                }]
            }
            """;

        server.createContext("/service/rest/v1/components", exchange -> {
            byte[] response = jsonResponse.getBytes();
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });

        List<ComponentMetadata> metadata = client.listComponentsWithMetadata("test-repo");
        assertEquals(1, metadata.size(), "Should parse component despite invalid dates");

        ComponentMetadata comp = metadata.get(0);
        assertNull(comp.createdDate(), "Invalid blobCreated should result in null date");
        assertNull(comp.lastModified(), "Invalid lastModified should result in null date");
        assertEquals("bad-date.jar", comp.path(), "Other fields should be parsed normally");
    }

    @Test
    @DisplayName("Metadata with MD5-only checksum should use MD5")
    void testMetadataMD5OnlyChecksum() throws Exception {
        String jsonResponse = """
            {
                "items": [{
                    "id": "md5-only",
                    "format": "raw",
                    "assets": [{
                        "path": "artifact.bin",
                        "fileSize": 1500,
                        "contentType": "application/octet-stream",
                        "checksum": {"md5": "d41d8cd98f00b204e9800998ecf8427e"}
                    }]
                }]
            }
            """;

        server.createContext("/service/rest/v1/components", exchange -> {
            byte[] response = jsonResponse.getBytes();
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });

        List<ComponentMetadata> metadata = client.listComponentsWithMetadata("test-repo");
        assertEquals(1, metadata.size());
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", metadata.get(0).checksum(),
            "Should fall back to MD5 when SHA1 is missing");
    }

    @Test
    @DisplayName("Metadata with empty checksum object should handle gracefully")
    void testMetadataEmptyChecksumObject() throws Exception {
        String jsonResponse = """
            {
                "items": [{
                    "id": "empty-checksum",
                    "format": "raw",
                    "assets": [{
                        "path": "artifact.bin",
                        "fileSize": 1500,
                        "contentType": "application/octet-stream",
                        "checksum": {}
                    }]
                }]
            }
            """;

        server.createContext("/service/rest/v1/components", exchange -> {
            byte[] response = jsonResponse.getBytes();
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });

        List<ComponentMetadata> metadata = client.listComponentsWithMetadata("test-repo");
        assertEquals(1, metadata.size());
        assertNull(metadata.get(0).checksum(),
            "Empty checksum object should result in null checksum");
    }

    @Test
    @DisplayName("Metadata with null checksum should handle gracefully")
    void testMetadataNullChecksum() throws Exception {
        String jsonResponse = """
            {
                "items": [{
                    "id": "null-checksum",
                    "format": "raw",
                    "assets": [{
                        "path": "artifact.bin",
                        "fileSize": 1500,
                        "contentType": "application/octet-stream",
                        "checksum": null
                    }]
                }]
            }
            """;

        server.createContext("/service/rest/v1/components", exchange -> {
            byte[] response = jsonResponse.getBytes();
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });

        List<ComponentMetadata> metadata = client.listComponentsWithMetadata("test-repo");
        assertEquals(1, metadata.size());
        assertNull(metadata.get(0).checksum(),
            "Null checksum should result in null checksum");
    }

    @Test
    @DisplayName("Path with spaces should be preserved as-is")
    void testPathWithSpaces() throws Exception {
        String jsonResponse = """
            {
                "items": [{
                    "id": "spaces",
                    "assets": [{
                        "path": "my file with spaces.jar",
                        "fileSize": 1000
                    }]
                }]
            }
            """;

        server.createContext("/service/rest/v1/components", exchange -> {
            byte[] response = jsonResponse.getBytes();
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });

        List<RepoRecord> records = client.listComponents("test-repo");
        assertEquals(1, records.size());
        assertEquals("my file with spaces.jar", records.get(0).path(),
            "Path with spaces should be preserved");
    }

    @Test
    @DisplayName("Very long path should not be truncated")
    void testVeryLongPath() throws Exception {
        // Generate a path > 1000 characters
        StringBuilder longPath = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            longPath.append("com/example/very/deep/package/path/level");
            longPath.append(i);
            longPath.append("/");
        }
        longPath.append("artifact-with-a-really-long-name-1.0.0-SNAPSHOT.jar");
        String expectedPath = longPath.toString();

        String jsonResponse = """
            {
                "items": [{
                    "id": "long-path",
                    "assets": [{
                        "path": "%s",
                        "fileSize": 1000
                    }]
                }]
            }
            """.formatted(expectedPath);

        server.createContext("/service/rest/v1/components", exchange -> {
            byte[] response = jsonResponse.getBytes();
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });

        List<RepoRecord> records = client.listComponents("test-repo");
        assertEquals(1, records.size());
        assertEquals(expectedPath, records.get(0).path(),
            "Long path should not be truncated");
        assertTrue(records.get(0).path().length() > 1000,
            "Path should be longer than 1000 characters");
    }
}

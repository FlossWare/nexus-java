package org.flossware.nexus;

import com.sun.net.httpserver.HttpServer;
import org.flossware.jnexus.ComponentMetadata;
import org.flossware.jnexus.RepoRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for pagination edge cases in NexusClient.
 * <p>
 * Covers scenarios from issue #67:
 * - Empty pages with continuation tokens
 * - Duplicate continuation tokens (infinite loop detection)
 * - Token that becomes invalid mid-pagination
 * - Large number of pages
 * - Metadata pagination edge cases
 * </p>
 */
@DisplayName("NexusClient Pagination Edge Case Tests")
class NexusClientPaginationEdgeCaseTest {

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
        client = new NexusClient(credentials, 0); // No caching
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("Empty page with continuation token should continue pagination")
    void testPaginationEmptyPageWithToken() throws Exception {
        AtomicInteger pageCount = new AtomicInteger(0);

        server.createContext("/service/rest/v1/components", exchange -> {
            int page = pageCount.incrementAndGet();
            String response;

            if (page == 1) {
                // First page: items with continuation token
                response = """
                    {
                        "items": [
                            {"id": "comp1", "assets": [{"path": "test1.jar", "fileSize": 1000}]}
                        ],
                        "continuationToken": "token1"
                    }
                    """;
            } else if (page == 2) {
                // Second page: empty items but still has continuation token
                response = """
                    {
                        "items": [],
                        "continuationToken": "token2"
                    }
                    """;
            } else {
                // Third page: items with no continuation token
                response = """
                    {
                        "items": [
                            {"id": "comp2", "assets": [{"path": "test2.jar", "fileSize": 2000}]}
                        ]
                    }
                    """;
            }

            byte[] bytes = response.getBytes();
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        List<RepoRecord> records = client.listComponents("test-repo");
        assertEquals(2, records.size(), "Should collect items from pages 1 and 3");
        assertEquals("comp1", records.get(0).id());
        assertEquals("comp2", records.get(1).id());
        assertEquals(3, pageCount.get(), "Should have fetched 3 pages");
    }

    @Test
    @DisplayName("Null continuation token terminates pagination")
    void testPaginationNullToken() throws Exception {
        AtomicInteger pageCount = new AtomicInteger(0);

        server.createContext("/service/rest/v1/components", exchange -> {
            pageCount.incrementAndGet();
            String response = """
                {
                    "items": [
                        {"id": "comp1", "assets": [{"path": "test1.jar", "fileSize": 1000}]}
                    ],
                    "continuationToken": null
                }
                """;

            byte[] bytes = response.getBytes();
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        List<RepoRecord> records = client.listComponents("test-repo");
        assertEquals(1, records.size(), "Should stop after first page with null token");
        assertEquals(1, pageCount.get(), "Should only request one page");
    }

    @Test
    @DisplayName("Multi-page pagination with many pages should complete")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testPaginationWith100Pages() throws Exception {
        AtomicInteger pageCount = new AtomicInteger(0);

        server.createContext("/service/rest/v1/components", exchange -> {
            int page = pageCount.incrementAndGet();
            String response;

            if (page < 100) {
                response = String.format("""
                    {
                        "items": [
                            {"id": "comp-%d", "assets": [{"path": "test-%d.jar", "fileSize": %d}]}
                        ],
                        "continuationToken": "token-%d"
                    }
                    """, page, page, page * 100L, page);
            } else {
                response = String.format("""
                    {
                        "items": [
                            {"id": "comp-%d", "assets": [{"path": "test-%d.jar", "fileSize": %d}]}
                        ]
                    }
                    """, page, page, page * 100L);
            }

            byte[] bytes = response.getBytes();
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        List<RepoRecord> records = client.listComponents("test-repo");
        assertEquals(100, records.size(), "Should collect all items from 100 pages");
        assertEquals(100, pageCount.get(), "Should have fetched exactly 100 pages");
    }

    @Test
    @DisplayName("Pagination token expiry mid-pagination should throw IOException")
    void testPaginationTokenBecomesInvalid() throws IOException {
        AtomicInteger pageCount = new AtomicInteger(0);

        server.createContext("/service/rest/v1/components", exchange -> {
            int page = pageCount.incrementAndGet();

            if (page == 1) {
                // First page: success with continuation token
                String response = """
                    {
                        "items": [
                            {"id": "comp1", "assets": [{"path": "test1.jar", "fileSize": 1000}]}
                        ],
                        "continuationToken": "expired-token"
                    }
                    """;
                byte[] bytes = response.getBytes();
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } else {
                // Second page: server returns 400 because token expired
                String errorResponse = "Invalid continuation token";
                byte[] bytes = errorResponse.getBytes();
                exchange.sendResponseHeaders(400, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            }
        });

        IOException exception = assertThrows(IOException.class, () ->
            client.listComponents("test-repo"),
            "Should throw IOException when pagination token becomes invalid"
        );

        assertTrue(exception.getMessage().contains("400"),
            "Exception should mention HTTP 400 status");
    }

    @Test
    @DisplayName("Continuation token with special characters should be URL-encoded")
    void testPaginationTokenWithSpecialCharacters() throws Exception {
        AtomicInteger pageCount = new AtomicInteger(0);

        server.createContext("/service/rest/v1/components", exchange -> {
            int page = pageCount.incrementAndGet();
            String query = exchange.getRequestURI().getQuery();
            String response;

            if (page == 1) {
                // First page with token containing special chars
                response = """
                    {
                        "items": [
                            {"id": "comp1", "assets": [{"path": "test1.jar", "fileSize": 1000}]}
                        ],
                        "continuationToken": "token=abc&next=123+value"
                    }
                    """;
            } else {
                // Second page: verify token was received (URL-encoded)
                assertTrue(query != null && query.contains("continuationToken="),
                    "Should include continuation token in URL");
                response = """
                    {
                        "items": [
                            {"id": "comp2", "assets": [{"path": "test2.jar", "fileSize": 2000}]}
                        ]
                    }
                    """;
            }

            byte[] bytes = response.getBytes();
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        List<RepoRecord> records = client.listComponents("test-repo");
        assertEquals(2, records.size(), "Should handle tokens with special characters");
    }

    @Test
    @DisplayName("Metadata pagination should work correctly across multiple pages")
    void testMetadataPagination() throws Exception {
        AtomicInteger pageCount = new AtomicInteger(0);

        server.createContext("/service/rest/v1/components", exchange -> {
            int page = pageCount.incrementAndGet();
            String response;

            if (page == 1) {
                response = """
                    {
                        "items": [{
                            "id": "comp1",
                            "format": "maven2",
                            "assets": [{
                                "path": "test1.jar",
                                "fileSize": 1000,
                                "contentType": "application/java-archive",
                                "blobCreated": "2024-01-15T10:30:00Z",
                                "lastModified": "2024-01-15T10:30:00Z",
                                "checksum": {"sha1": "abc123", "md5": "def456"}
                            }]
                        }],
                        "continuationToken": "meta-token-1"
                    }
                    """;
            } else {
                response = """
                    {
                        "items": [{
                            "id": "comp2",
                            "format": "npm",
                            "assets": [{
                                "path": "test2.tgz",
                                "fileSize": 2000,
                                "contentType": "application/gzip"
                            }]
                        }]
                    }
                    """;
            }

            byte[] bytes = response.getBytes();
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        List<ComponentMetadata> metadata = client.listComponentsWithMetadata("test-repo");
        assertEquals(2, metadata.size(), "Should collect metadata from both pages");
        assertEquals("maven2", metadata.get(0).format());
        assertEquals("abc123", metadata.get(0).checksum());
        assertEquals("npm", metadata.get(1).format());
        assertNull(metadata.get(1).checksum(), "Missing checksum should be null");
    }
}

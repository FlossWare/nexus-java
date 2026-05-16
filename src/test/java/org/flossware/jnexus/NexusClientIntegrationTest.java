package org.flossware.jnexus;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NexusClientIntegrationTest {

    private HttpServer server;
    private int port;
    private Credentials credentials;
    private String originalHome;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws IOException {
        originalHome = System.getProperty("user.home");
        System.setProperty("user.home", tempDir.toString());

        port = 8888;
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.start();

        Path configDir = tempDir.resolve(".flossware/nexus");
        Files.createDirectories(configDir);
        Path configFile = configDir.resolve("nexus.properties");
        Files.writeString(configFile,
            "nexus.url=http://localhost:" + port + "\n" +
            "nexus.user=testuser\n" +
            "nexus.password=testpass\n"
        );

        credentials = new Credentials();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
        if (originalHome != null) {
            System.setProperty("user.home", originalHome);
        }
    }

    @Test
    void testListComponentsWithSinglePage() throws IOException, InterruptedException {
        String jsonResponse = """
            {
                "items": [
                    {
                        "id": "component1",
                        "assets": [
                            {
                                "fileSize": 1024,
                                "path": "com/example/artifact/1.0.0/artifact-1.0.0.jar"
                            }
                        ]
                    },
                    {
                        "id": "component2",
                        "assets": [
                            {
                                "fileSize": 2048,
                                "path": "com/example/artifact/2.0.0/artifact-2.0.0.jar"
                            }
                        ]
                    }
                ]
            }
            """;

        server.createContext("/service/rest/v1/components", exchange -> {
            String auth = exchange.getRequestHeaders().getFirst("Authorization");
            String expectedAuth = "Basic " + Base64.getEncoder().encodeToString("testuser:testpass".getBytes());

            if (expectedAuth.equals(auth)) {
                byte[] response = jsonResponse.getBytes();
                exchange.sendResponseHeaders(200, response.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response);
                }
            } else {
                exchange.sendResponseHeaders(401, 0);
                exchange.getResponseBody().close();
            }
        });

        NexusClient client = new NexusClient(credentials);
        List<RepoRecord> records = client.listComponents("test-repo");

        assertEquals(2, records.size());
        assertEquals("component1", records.get(0).id());
        assertEquals(1024, records.get(0).fileSize());
        assertEquals("com/example/artifact/1.0.0/artifact-1.0.0.jar", records.get(0).path());
    }

    @Test
    void testListComponentsWithPagination() throws IOException, InterruptedException {
        server.createContext("/service/rest/v1/components", exchange -> {
            String query = exchange.getRequestURI().getQuery();
            String response;

            if (query == null || !query.contains("continuationToken")) {
                response = """
                    {
                        "items": [
                            {
                                "id": "component1",
                                "assets": [{"fileSize": 1024, "path": "artifact1.jar"}]
                            }
                        ],
                        "continuationToken": "token123"
                    }
                    """;
            } else {
                response = """
                    {
                        "items": [
                            {
                                "id": "component2",
                                "assets": [{"fileSize": 2048, "path": "artifact2.jar"}]
                            }
                        ]
                    }
                    """;
            }

            byte[] responseBytes = response.getBytes();
            exchange.sendResponseHeaders(200, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        });

        NexusClient client = new NexusClient(credentials);
        List<RepoRecord> records = client.listComponents("test-repo");

        assertEquals(2, records.size());
        assertEquals("component1", records.get(0).id());
        assertEquals("component2", records.get(1).id());
    }

    @Test
    void testListComponentsWithEmptyAssets() throws IOException, InterruptedException {
        String jsonResponse = """
            {
                "items": [
                    {
                        "id": "component1",
                        "assets": []
                    }
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

        NexusClient client = new NexusClient(credentials);
        List<RepoRecord> records = client.listComponents("test-repo");

        assertTrue(records.isEmpty());
    }

    @Test
    void testListComponentsHttpError() {
        server.createContext("/service/rest/v1/components", exchange -> {
            exchange.sendResponseHeaders(500, 0);
            exchange.getResponseBody().close();
        });

        NexusClient client = new NexusClient(credentials);

        IOException exception = assertThrows(IOException.class, () -> {
            client.listComponents("test-repo");
        });

        assertTrue(exception.getMessage().contains("HTTP 500"));
    }

    @Test
    void testListComponentsUnauthorized() {
        server.createContext("/service/rest/v1/components", exchange -> {
            exchange.sendResponseHeaders(401, 0);
            exchange.getResponseBody().close();
        });

        NexusClient client = new NexusClient(credentials);

        IOException exception = assertThrows(IOException.class, () -> {
            client.listComponents("test-repo");
        });

        assertTrue(exception.getMessage().contains("HTTP 401"));
    }

    @Test
    void testDeleteComponentSuccess() throws IOException, InterruptedException {
        server.createContext("/service/rest/v1/components/", exchange -> {
            if (exchange.getRequestMethod().equals("DELETE")) {
                exchange.sendResponseHeaders(204, -1);
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
            exchange.getResponseBody().close();
        });

        NexusClient client = new NexusClient(credentials);
        assertDoesNotThrow(() -> client.deleteComponent("component123"));
    }

    @Test
    void testDeleteComponentNotFound() {
        server.createContext("/service/rest/v1/components/", exchange -> {
            exchange.sendResponseHeaders(404, 0);
            exchange.getResponseBody().close();
        });

        NexusClient client = new NexusClient(credentials);

        IOException exception = assertThrows(IOException.class, () -> {
            client.deleteComponent("nonexistent");
        });

        assertTrue(exception.getMessage().contains("HTTP 404"));
    }

    @Test
    void testAuthenticationHeaderIsCorrect(@TempDir Path tempDir) throws IOException, InterruptedException {
        server.createContext("/service/rest/v1/components", exchange -> {
            String auth = exchange.getRequestHeaders().getFirst("Authorization");
            assertNotNull(auth);
            assertTrue(auth.startsWith("Basic "));

            String decoded = new String(Base64.getDecoder().decode(auth.substring(6)));
            assertEquals("testuser:testpass", decoded);

            String response = "{\"items\": []}";
            byte[] responseBytes = response.getBytes();
            exchange.sendResponseHeaders(200, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        });

        NexusClient client = new NexusClient(credentials);
        client.listComponents("test-repo");
    }
}

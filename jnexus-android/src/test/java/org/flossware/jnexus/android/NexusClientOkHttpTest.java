package org.flossware.jnexus.android;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.flossware.jnexus.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for NexusClientOkHttp.
 */
class NexusClientOkHttpTest {

    private MockWebServer mockServer;
    private Credentials mockCredentials;
    private NexusClientOkHttp client;

    @BeforeEach
    void setUp() throws IOException {
        mockServer = new MockWebServer();
        mockServer.start();

        mockCredentials = Mockito.mock(Credentials.class);
        when(mockCredentials.getUrl()).thenReturn(mockServer.url("").toString().replaceAll("/$", ""));
        when(mockCredentials.getUser()).thenReturn("testuser");
        when(mockCredentials.getPassword()).thenReturn("testpass");
        when(mockCredentials.getHttpTimeoutSeconds()).thenReturn(30);

        client = new NexusClientOkHttp(mockCredentials, 0); // Disable caching for tests
    }

    @AfterEach
    void tearDown() throws IOException {
        mockServer.shutdown();
    }

    @Test
    void testListComponents_Success() throws IOException, InterruptedException {
        String jsonResponse = """
            {
              "items": [
                {
                  "id": "component1",
                  "assets": [
                    {
                      "path": "com/example/artifact-1.0.jar",
                      "fileSize": 1234
                    }
                  ]
                }
              ],
              "continuationToken": null
            }
            """;

        mockServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(jsonResponse)
            .addHeader("Content-Type", "application/json"));

        List<RepoRecord> components = client.listComponents("maven-releases", false);

        assertEquals(1, components.size());
        assertEquals("component1", components.get(0).id());
        assertEquals(1234, components.get(0).fileSize());
        assertEquals("com/example/artifact-1.0.jar", components.get(0).path());
    }

    @Test
    void testListComponents_Pagination() throws IOException, InterruptedException {
        String page1 = """
            {
              "items": [
                {
                  "id": "component1",
                  "assets": [{"path": "file1.jar", "fileSize": 100}]
                }
              ],
              "continuationToken": "token123"
            }
            """;

        String page2 = """
            {
              "items": [
                {
                  "id": "component2",
                  "assets": [{"path": "file2.jar", "fileSize": 200}]
                }
              ],
              "continuationToken": null
            }
            """;

        mockServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(page1)
            .addHeader("Content-Type", "application/json"));

        mockServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(page2)
            .addHeader("Content-Type", "application/json"));

        List<RepoRecord> components = client.listComponents("maven-releases", false);

        assertEquals(2, components.size());
        assertEquals("component1", components.get(0).id());
        assertEquals("component2", components.get(1).id());
    }

    @Test
    void testListComponentsWithMetadata_Success() throws IOException, InterruptedException {
        String jsonResponse = """
            {
              "items": [
                {
                  "id": "component1",
                  "format": "maven2",
                  "assets": [
                    {
                      "path": "com/example/artifact-1.0.jar",
                      "fileSize": 1234,
                      "contentType": "application/java-archive",
                      "blobCreated": "2024-01-15T10:30:00.000Z",
                      "lastModified": "2024-01-15T10:30:00.000Z",
                      "checksum": {
                        "sha1": "abc123",
                        "md5": "def456"
                      }
                    }
                  ]
                }
              ],
              "continuationToken": null
            }
            """;

        mockServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(jsonResponse)
            .addHeader("Content-Type", "application/json"));

        List<ComponentMetadata> components = client.listComponentsWithMetadata("maven-releases", false);

        assertEquals(1, components.size());
        ComponentMetadata component = components.get(0);
        assertEquals("component1", component.id());
        assertEquals("com/example/artifact-1.0.jar", component.path());
        assertEquals(1234, component.fileSize());
        assertEquals("application/java-archive", component.contentType());
        assertEquals("maven2", component.format());
        assertEquals(Instant.parse("2024-01-15T10:30:00.000Z"), component.createdDate());
        assertEquals(Instant.parse("2024-01-15T10:30:00.000Z"), component.lastModified());
        assertEquals("abc123", component.checksum());
    }

    @Test
    void testListComponentsWithMetadata_MissingOptionalFields() throws IOException, InterruptedException {
        String jsonResponse = """
            {
              "items": [
                {
                  "id": "component1",
                  "assets": [
                    {
                      "path": "file.jar",
                      "fileSize": 100
                    }
                  ]
                }
              ],
              "continuationToken": null
            }
            """;

        mockServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(jsonResponse)
            .addHeader("Content-Type", "application/json"));

        List<ComponentMetadata> components = client.listComponentsWithMetadata("maven-releases", false);

        assertEquals(1, components.size());
        ComponentMetadata component = components.get(0);
        assertEquals("unknown", component.contentType());
        assertEquals("unknown", component.format());
        assertNull(component.createdDate());
        assertNull(component.lastModified());
        assertNull(component.checksum());
    }

    @Test
    void testDeleteComponent_Success() throws IOException {
        mockServer.enqueue(new MockResponse()
            .setResponseCode(204));

        assertDoesNotThrow(() -> client.deleteComponent("component1"));
    }

    @Test
    void testDeleteComponent_Failure() {
        mockServer.enqueue(new MockResponse()
            .setResponseCode(404)
            .setBody("Not found"));

        IOException exception = assertThrows(IOException.class, () ->
            client.deleteComponent("nonexistent")
        );

        assertTrue(exception.getMessage().contains("404"));
    }

    @Test
    void testListComponents_HttpError() {
        mockServer.enqueue(new MockResponse()
            .setResponseCode(500)
            .setBody("Internal server error"));

        IOException exception = assertThrows(IOException.class, () ->
            client.listComponents("maven-releases", false)
        );

        assertTrue(exception.getMessage().contains("500"));
    }

    @Test
    void testClearCache() throws IOException, InterruptedException {
        // Create client with caching enabled
        NexusClientOkHttp cachingClient = new NexusClientOkHttp(mockCredentials, 300);

        String jsonResponse = """
            {
              "items": [
                {
                  "id": "component1",
                  "assets": [{"path": "file.jar", "fileSize": 100}]
                }
              ],
              "continuationToken": null
            }
            """;

        // First request - should hit server
        mockServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(jsonResponse)
            .addHeader("Content-Type", "application/json"));

        cachingClient.listComponents("maven-releases", false);
        assertTrue(cachingClient.isCached("maven-releases"));

        // Clear cache
        cachingClient.clearCache("maven-releases");
        assertFalse(cachingClient.isCached("maven-releases"));
    }

    @Test
    void testClearAllCache() throws IOException, InterruptedException {
        // Create client with caching enabled
        NexusClientOkHttp cachingClient = new NexusClientOkHttp(mockCredentials, 300);

        String jsonResponse = """
            {
              "items": [],
              "continuationToken": null
            }
            """;

        // Cache two repositories
        mockServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(jsonResponse)
            .addHeader("Content-Type", "application/json"));
        mockServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(jsonResponse)
            .addHeader("Content-Type", "application/json"));

        cachingClient.listComponents("repo1", false);
        cachingClient.listComponents("repo2", false);

        assertTrue(cachingClient.isCached("repo1"));
        assertTrue(cachingClient.isCached("repo2"));

        // Clear all
        cachingClient.clearAllCache();

        assertFalse(cachingClient.isCached("repo1"));
        assertFalse(cachingClient.isCached("repo2"));
    }

    @Test
    void testCacheExpiration() throws IOException, InterruptedException {
        // Create client with very short TTL
        NexusClientOkHttp cachingClient = new NexusClientOkHttp(mockCredentials, 1);

        String jsonResponse = """
            {
              "items": [],
              "continuationToken": null
            }
            """;

        mockServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(jsonResponse)
            .addHeader("Content-Type", "application/json"));

        cachingClient.listComponents("maven-releases", false);
        assertTrue(cachingClient.isCached("maven-releases"));

        // Wait for cache to expire
        Thread.sleep(1100);

        assertFalse(cachingClient.isCached("maven-releases"));
    }

    @Test
    void testRetryOnServerError() throws IOException, InterruptedException {
        // First two attempts fail, third succeeds
        mockServer.enqueue(new MockResponse().setResponseCode(500));
        mockServer.enqueue(new MockResponse().setResponseCode(500));
        mockServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody("""
                {
                  "items": [],
                  "continuationToken": null
                }
                """)
            .addHeader("Content-Type", "application/json"));

        List<RepoRecord> components = client.listComponents("maven-releases", false);
        assertNotNull(components);
        assertEquals(0, components.size());

        // Should have made 3 requests
        assertEquals(3, mockServer.getRequestCount());
    }

    @Test
    void testNoRetryOn404() {
        // 404 should not be retried
        mockServer.enqueue(new MockResponse().setResponseCode(404));

        assertThrows(IOException.class, () ->
            client.listComponents("maven-releases", false)
        );

        // Should have made only 1 request (no retries)
        assertEquals(1, mockServer.getRequestCount());
    }
}

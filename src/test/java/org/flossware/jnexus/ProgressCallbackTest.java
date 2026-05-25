package org.flossware.jnexus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for ProgressCallback interface and integration with NexusService.
 */
class ProgressCallbackTest {

    @Mock
    private NexusClient mockClient;

    private NexusService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new NexusService(mockClient);
    }

    @Test
    void testDeleteWithProgressCallback() throws IOException, InterruptedException {
        // Create a tracking callback
        TrackingCallback callback = new TrackingCallback();

        // Mock data
        List<RepoRecord> mockRecords = List.of(
            new RepoRecord("id1", 1000, "path/to/artifact1.jar"),
            new RepoRecord("id2", 2000, "path/to/artifact2.jar"),
            new RepoRecord("id3", 3000, "path/to/artifact3.jar")
        );

        when(mockClient.listComponents(eq("test-repo"), anyBoolean())).thenReturn(mockRecords);

        // Execute deletion with callback
        service.deleteFromRepository("test-repo", null, false, callback);

        // Verify deletions
        verify(mockClient, times(3)).deleteComponent(anyString());

        // Verify callback was invoked
        assertEquals(3, callback.deletedComponents.size());
        assertEquals(0, callback.failedComponents.size());
        assertTrue(callback.deleteCompleteCalled);
        assertEquals(3, callback.finalDeleted);
        assertEquals(0, callback.finalFailed);
    }

    @Test
    void testDeleteWithFailuresCallback() throws IOException, InterruptedException {
        // Create a tracking callback
        TrackingCallback callback = new TrackingCallback();

        // Mock data
        List<RepoRecord> mockRecords = List.of(
            new RepoRecord("id1", 1000, "path/to/artifact1.jar"),
            new RepoRecord("id2", 2000, "path/to/artifact2.jar")
        );

        when(mockClient.listComponents(eq("test-repo"), anyBoolean())).thenReturn(mockRecords);

        // Make second deletion fail
        doNothing().when(mockClient).deleteComponent("id1");
        doThrow(new IOException("HTTP 404")).when(mockClient).deleteComponent("id2");

        // Execute deletion with callback
        service.deleteFromRepository("test-repo", null, false, callback);

        // Verify callback tracked failure
        assertEquals(1, callback.deletedComponents.size());
        assertEquals(1, callback.failedComponents.size());
        assertEquals("path/to/artifact2.jar", callback.failedComponents.get(0));
        assertTrue(callback.deleteCompleteCalled);
        assertEquals(1, callback.finalDeleted);
        assertEquals(1, callback.finalFailed);
    }

    @Test
    void testDeleteWithNullCallback() throws IOException, InterruptedException {
        // Null callback should not cause errors
        List<RepoRecord> mockRecords = List.of(
            new RepoRecord("id1", 1000, "path/to/artifact1.jar")
        );

        when(mockClient.listComponents(eq("test-repo"), anyBoolean())).thenReturn(mockRecords);

        // Should not throw
        assertDoesNotThrow(() ->
            service.deleteFromRepository("test-repo", null, false, null)
        );

        verify(mockClient, times(1)).deleteComponent("id1");
    }

    @Test
    void testCalculateStatisticsWithCallback() {
        // Create a tracking callback
        TrackingCallback callback = new TrackingCallback();

        // Mock data
        List<ComponentMetadata> components = List.of(
            new ComponentMetadata("id1", "path1.jar", 1000, "application/java-archive", "maven2", null, null, null),
            new ComponentMetadata("id2", "path2.jar", 2000, "application/java-archive", "maven2", null, null, null)
        );

        // Execute with callback
        RepositoryStats stats = service.calculateStatistics("test-repo", components, callback);

        // Verify callback was invoked
        assertTrue(callback.statisticsStartCalled);
        assertEquals("test-repo", callback.statsRepository);
        assertEquals(2, callback.statsTotalComponents);
        assertTrue(callback.statisticsCompleteCalled);
        assertNotNull(callback.completedStats);
        assertEquals(2, stats.totalComponents());
    }

    @Test
    void testCallbackExceptionHandling() throws IOException, InterruptedException {
        // Create a callback that throws exceptions
        ProgressCallback failingCallback = new ProgressCallback() {
            @Override
            public void onComponentDeleted(String componentId, String path) {
                throw new RuntimeException("Callback error");
            }

            @Override
            public void onDeleteComplete(int deleted, int failed) {
                throw new RuntimeException("Callback error");
            }
        };

        // Mock data
        List<RepoRecord> mockRecords = List.of(
            new RepoRecord("id1", 1000, "path/to/artifact1.jar")
        );

        when(mockClient.listComponents(eq("test-repo"), anyBoolean())).thenReturn(mockRecords);

        // Should not throw even though callback throws
        assertDoesNotThrow(() ->
            service.deleteFromRepository("test-repo", null, false, failingCallback)
        );

        // Operation should still complete
        verify(mockClient, times(1)).deleteComponent("id1");
    }

    /**
     * Test implementation of ProgressCallback that tracks all invocations.
     */
    private static class TrackingCallback implements ProgressCallback {
        List<String> deletedComponents = new ArrayList<>();
        List<String> failedComponents = new ArrayList<>();
        List<Double> progressUpdates = new ArrayList<>();
        boolean deleteCompleteCalled = false;
        int finalDeleted = 0;
        int finalFailed = 0;

        boolean statisticsStartCalled = false;
        String statsRepository = null;
        int statsTotalComponents = 0;
        boolean statisticsCompleteCalled = false;
        RepositoryStats completedStats = null;

        @Override
        public void onComponentDeleted(String componentId, String path) {
            deletedComponents.add(path);
        }

        @Override
        public void onComponentDeleteFailed(String componentId, String path, String error) {
            failedComponents.add(path);
        }

        @Override
        public void onDeleteProgress(int deleted, int total, double percentage) {
            progressUpdates.add(percentage);
        }

        @Override
        public void onDeleteComplete(int deleted, int failed) {
            deleteCompleteCalled = true;
            finalDeleted = deleted;
            finalFailed = failed;
        }

        @Override
        public void onStatisticsStart(String repository, int totalComponents) {
            statisticsStartCalled = true;
            statsRepository = repository;
            statsTotalComponents = totalComponents;
        }

        @Override
        public void onStatisticsComplete(String repository, RepositoryStats stats) {
            statisticsCompleteCalled = true;
            completedStats = stats;
        }
    }
}

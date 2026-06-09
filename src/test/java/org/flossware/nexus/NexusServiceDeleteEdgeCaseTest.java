package org.flossware.nexus;

import org.flossware.jnexus.RepoRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Edge case tests for delete operations in NexusService.
 * <p>
 * Covers scenarios from issue #67:
 * - Delete non-existent component (error message clarity)
 * - Delete already-deleted component (idempotency)
 * - Bulk delete 1000 components (stress test)
 * - Delete with progress callbacks
 * - Delete with all failures
 * - Delete with callback exceptions
 * - Delete using pre-fetched records
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NexusService Delete Edge Case Tests")
class NexusServiceDeleteEdgeCaseTest {

    @Mock
    private NexusClient mockClient;

    private NexusService service;
    private ByteArrayOutputStream outputStream;
    private ByteArrayOutputStream errorStream;
    private PrintStream originalOut;
    private PrintStream originalErr;

    @BeforeEach
    void setUp() {
        service = new NexusService(mockClient);
        outputStream = new ByteArrayOutputStream();
        errorStream = new ByteArrayOutputStream();
        originalOut = System.out;
        originalErr = System.err;
        System.setOut(new PrintStream(outputStream));
        System.setErr(new PrintStream(errorStream));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    @DisplayName("Delete non-existent component should report error but continue")
    void testDeleteNonExistentComponent() throws Exception {
        List<RepoRecord> mockRecords = List.of(
            new RepoRecord("non-existent-id", 1000, "missing.jar"),
            new RepoRecord("valid-id", 2000, "present.jar")
        );

        when(mockClient.listComponents(eq("test-repo"), anyBoolean()))
            .thenReturn(mockRecords);
        doThrow(new IOException("HTTP 404: Component not found"))
            .when(mockClient).deleteComponent("non-existent-id");
        doNothing().when(mockClient).deleteComponent("valid-id");

        service.deleteFromRepository("test-repo", null, false);

        verify(mockClient).deleteComponent("non-existent-id");
        verify(mockClient).deleteComponent("valid-id");

        String output = outputStream.toString();
        assertTrue(output.contains("Deleted 1 of 2"),
            "Should report 1 of 2 deleted");

        String errors = errorStream.toString();
        assertTrue(errors.contains("missing.jar"),
            "Error output should mention the failed component path");
    }

    @Test
    @DisplayName("Delete same component twice should handle failure on second attempt")
    void testDeleteAlreadyDeletedComponent() throws Exception {
        List<RepoRecord> mockRecords = List.of(
            new RepoRecord("comp-1", 1000, "artifact.jar")
        );

        when(mockClient.listComponents(eq("test-repo"), anyBoolean()))
            .thenReturn(mockRecords);
        doNothing().when(mockClient).deleteComponent("comp-1");

        // First delete succeeds
        service.deleteFromRepository("test-repo", null, false);
        verify(mockClient, times(1)).deleteComponent("comp-1");

        // Reset mock for second call
        reset(mockClient);
        when(mockClient.listComponents(eq("test-repo"), anyBoolean()))
            .thenReturn(mockRecords);
        doThrow(new IOException("HTTP 404: Component already deleted"))
            .when(mockClient).deleteComponent("comp-1");

        // Second delete of same component should handle gracefully
        service.deleteFromRepository("test-repo", null, false);

        String errors = errorStream.toString();
        assertTrue(errors.contains("failed to delete") || errors.contains("Failed"),
            "Should report failure for already-deleted component");
    }

    @Test
    @DisplayName("Bulk delete 1000 components should complete successfully")
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void testDelete1000Components() throws Exception {
        int count = 1000;
        List<RepoRecord> mockRecords = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            mockRecords.add(new RepoRecord("id-" + i, i * 100L, "artifact-" + i + ".jar"));
        }

        when(mockClient.listComponents(eq("test-repo"), anyBoolean()))
            .thenReturn(mockRecords);
        doNothing().when(mockClient).deleteComponent(anyString());

        service.deleteFromRepository("test-repo", null, false);

        verify(mockClient, times(count)).deleteComponent(anyString());

        String output = outputStream.toString();
        assertTrue(output.contains("Deleted 1000 of 1000"),
            "Should report all 1000 deleted");
        // Should have progress updates since > 10 components
        assertTrue(output.contains("Progress:"),
            "Should show progress for large bulk delete");
    }

    @Test
    @DisplayName("Delete with all failures should report all errors")
    void testDeleteAllFailures() throws Exception {
        List<RepoRecord> mockRecords = List.of(
            new RepoRecord("id-1", 1000, "artifact1.jar"),
            new RepoRecord("id-2", 2000, "artifact2.jar"),
            new RepoRecord("id-3", 3000, "artifact3.jar")
        );

        when(mockClient.listComponents(eq("test-repo"), anyBoolean()))
            .thenReturn(mockRecords);
        doThrow(new IOException("Server error"))
            .when(mockClient).deleteComponent(anyString());

        service.deleteFromRepository("test-repo", null, false);

        String output = outputStream.toString();
        assertTrue(output.contains("Deleted 0 of 3"),
            "Should report 0 of 3 deleted");

        String errors = errorStream.toString();
        assertTrue(errors.contains("3 component(s) failed to delete"),
            "Should report 3 failures: " + errors);
    }

    @Test
    @DisplayName("Delete with progress callback should receive all events")
    void testDeleteWithProgressCallback() throws Exception {
        List<RepoRecord> mockRecords = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            mockRecords.add(new RepoRecord("id-" + i, i * 100L, "artifact-" + i + ".jar"));
        }

        when(mockClient.listComponents(eq("test-repo"), anyBoolean()))
            .thenReturn(mockRecords);
        doNothing().when(mockClient).deleteComponent(anyString());

        AtomicInteger deletedCallbacks = new AtomicInteger(0);
        AtomicInteger progressCallbacks = new AtomicInteger(0);
        AtomicInteger completeCallbacks = new AtomicInteger(0);

        ProgressCallback callback = new ProgressCallback() {
            @Override
            public void onComponentDeleted(String id, String path) {
                deletedCallbacks.incrementAndGet();
            }

            @Override
            public void onDeleteProgress(int deleted, int total, double percent) {
                progressCallbacks.incrementAndGet();
                assertTrue(deleted <= total, "Deleted should be <= total");
                assertTrue(percent >= 0 && percent <= 100, "Percent should be 0-100");
            }

            @Override
            public void onDeleteComplete(int deleted, int failed) {
                completeCallbacks.incrementAndGet();
                assertEquals(15, deleted, "All 15 should be deleted");
                assertEquals(0, failed, "No failures expected");
            }
        };

        service.deleteFromRepository("test-repo", null, false, callback);

        assertEquals(15, deletedCallbacks.get(), "Should get callback for each deletion");
        assertTrue(progressCallbacks.get() > 0, "Should get progress callbacks for > 10 items");
        assertEquals(1, completeCallbacks.get(), "Should get exactly one complete callback");
    }

    @Test
    @DisplayName("Delete with callback that throws exception should not disrupt operation")
    void testDeleteWithFaultyCallback() throws Exception {
        List<RepoRecord> mockRecords = List.of(
            new RepoRecord("id-1", 1000, "artifact1.jar"),
            new RepoRecord("id-2", 2000, "artifact2.jar")
        );

        when(mockClient.listComponents(eq("test-repo"), anyBoolean()))
            .thenReturn(mockRecords);
        doNothing().when(mockClient).deleteComponent(anyString());

        ProgressCallback faultyCallback = new ProgressCallback() {
            @Override
            public void onComponentDeleted(String id, String path) {
                throw new RuntimeException("Callback error!");
            }

            @Override
            public void onDeleteComplete(int deleted, int failed) {
                throw new RuntimeException("Complete callback error!");
            }
        };

        // Should not throw despite faulty callback
        assertDoesNotThrow(() ->
            service.deleteFromRepository("test-repo", null, false, faultyCallback),
            "Faulty callbacks should not disrupt delete operation"
        );

        verify(mockClient, times(2)).deleteComponent(anyString());

        String output = outputStream.toString();
        assertTrue(output.contains("Deleted 2 of 2"),
            "Should still report successful deletes");
    }

    @Test
    @DisplayName("Delete with pre-fetched records should work correctly")
    void testDeleteFromRepositoryWithRecords() throws Exception {
        List<RepoRecord> allRecords = List.of(
            new RepoRecord("id-1", 1000, "release-1.0.jar"),
            new RepoRecord("id-2", 2000, "snapshot-1.0-SNAPSHOT.jar"),
            new RepoRecord("id-3", 3000, "release-2.0.jar")
        );

        doNothing().when(mockClient).deleteComponent(anyString());

        service.deleteFromRepositoryWithRecords("test-repo", ".*SNAPSHOT.*", false, allRecords);

        // Should only delete the SNAPSHOT component
        verify(mockClient, times(1)).deleteComponent("id-2");
        verify(mockClient, never()).deleteComponent("id-1");
        verify(mockClient, never()).deleteComponent("id-3");

        // Should NOT call listComponents since we're using pre-fetched records
        verify(mockClient, never()).listComponents(anyString(), anyBoolean());
    }

    @Test
    @DisplayName("Delete with failed callback on failure event should not crash")
    void testDeleteFailedWithFaultyFailureCallback() throws Exception {
        List<RepoRecord> mockRecords = List.of(
            new RepoRecord("id-1", 1000, "artifact1.jar")
        );

        when(mockClient.listComponents(eq("test-repo"), anyBoolean()))
            .thenReturn(mockRecords);
        doThrow(new IOException("Delete failed"))
            .when(mockClient).deleteComponent("id-1");

        ProgressCallback faultyCallback = new ProgressCallback() {
            @Override
            public void onComponentDeleteFailed(String id, String path, String error) {
                throw new RuntimeException("Callback error on failure!");
            }

            @Override
            public void onDeleteComplete(int deleted, int failed) {
                throw new RuntimeException("Complete callback error!");
            }
        };

        assertDoesNotThrow(() ->
            service.deleteFromRepository("test-repo", null, false, faultyCallback),
            "Faulty failure callback should not crash the delete operation"
        );
    }

    @Test
    @DisplayName("Dry run should not call deleteComponent or clearAllCache")
    void testDryRunDoesNotModify() throws Exception {
        List<RepoRecord> mockRecords = List.of(
            new RepoRecord("id-1", 1000, "artifact1.jar"),
            new RepoRecord("id-2", 2000, "artifact2.jar")
        );

        when(mockClient.listComponents(eq("test-repo"), anyBoolean()))
            .thenReturn(mockRecords);

        service.deleteFromRepository("test-repo", null, true);

        verify(mockClient, never()).deleteComponent(anyString());
        verify(mockClient, never()).clearAllCache();

        String output = outputStream.toString();
        assertTrue(output.contains("DRY-RUN"),
            "Should indicate dry-run mode");
        assertTrue(output.contains("SIMULATION"),
            "Should indicate simulation");
    }

    @Test
    @DisplayName("Delete should clear all cache after completion")
    void testDeleteClearsCacheAfterCompletion() throws Exception {
        List<RepoRecord> mockRecords = List.of(
            new RepoRecord("id-1", 1000, "artifact1.jar")
        );

        when(mockClient.listComponents(eq("test-repo"), anyBoolean()))
            .thenReturn(mockRecords);
        doNothing().when(mockClient).deleteComponent(anyString());

        service.deleteFromRepository("test-repo", null, false);

        verify(mockClient).clearAllCache();
    }

    @Test
    @DisplayName("Deletion history should record each successful deletion")
    void testDeletionHistoryRecording() throws Exception {
        DeletionHistory history = new DeletionHistory();
        NexusService serviceWithHistory = new NexusService(mockClient, history);

        List<RepoRecord> mockRecords = List.of(
            new RepoRecord("id-1", 1000, "artifact1.jar"),
            new RepoRecord("id-2", 2000, "artifact2.jar"),
            new RepoRecord("id-3", 3000, "artifact3.jar")
        );

        when(mockClient.listComponents(eq("test-repo"), anyBoolean()))
            .thenReturn(mockRecords);
        doNothing().when(mockClient).deleteComponent("id-1");
        doThrow(new IOException("failed")).when(mockClient).deleteComponent("id-2");
        doNothing().when(mockClient).deleteComponent("id-3");

        serviceWithHistory.deleteFromRepository("test-repo", null, false);

        assertEquals(2, history.size(),
            "Should record 2 successful deletions (not the failed one)");

        List<DeletionHistory.DeletedComponent> deletions = history.getAllDeletions();
        assertTrue(deletions.stream().anyMatch(d -> d.path().equals("artifact1.jar")),
            "Should record artifact1.jar");
        assertTrue(deletions.stream().anyMatch(d -> d.path().equals("artifact3.jar")),
            "Should record artifact3.jar");
        assertTrue(deletions.stream().noneMatch(d -> d.path().equals("artifact2.jar")),
            "Should NOT record failed artifact2.jar");
    }
}

package org.flossware.nexus;

import org.flossware.jnexus.RepoRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for delete operation edge cases.
 * Tests non-existent components, already-deleted items, concurrent deletes, and bulk operations.
 */
@DisplayName("NexusClient Delete Operation Edge Case Tests")
class NexusIntegrationDeleteEdgeCaseTest {

    @Mock
    private NexusClient mockClient;

    private NexusService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new NexusService(mockClient);
    }

    @Test
    @DisplayName("Delete non-existent component")
    void testDeleteNonExistentComponent() throws Exception {
        when(mockClient.listComponents(anyString(), anyBoolean()))
            .thenReturn(List.of()); // No components exist

        service.deleteFromRepository("test-repo", null, false, null);

        verify(mockClient).listComponents(anyString(), anyBoolean());
        verify(mockClient, never()).deleteComponent(anyString());
    }

    @Test
    @DisplayName("Delete already-deleted component (idempotency)")
    void testDeleteAlreadyDeletedComponent() throws Exception {
        List<RepoRecord> initialRecords = List.of(
            new RepoRecord("id1", 1024, "file1.jar")
        );

        when(mockClient.listComponents(anyString(), anyBoolean()))
            .thenReturn(initialRecords)
            .thenReturn(List.of()); // Second call: component already gone

        // First delete
        service.deleteFromRepository("test-repo", null, false, null);
        verify(mockClient).deleteComponent("id1");

        // Reset mock
        reset(mockClient);
        when(mockClient.listComponents(anyString(), anyBoolean()))
            .thenReturn(List.of());

        // Second delete (should be no-op)
        service.deleteFromRepository("test-repo", null, false, null);
        verify(mockClient, never()).deleteComponent(anyString());
    }

    @Test
    @DisplayName("Delete with partial failures")
    void testDeleteWithPartialFailures() throws Exception {
        List<RepoRecord> records = List.of(
            new RepoRecord("id1", 1024, "file1.jar"),
            new RepoRecord("id2", 2048, "file2.jar"),
            new RepoRecord("id3", 512, "file3.jar")
        );

        when(mockClient.listComponents(anyString(), anyBoolean()))
            .thenReturn(records);

        // First and third succeed, second fails
        when(mockClient.deleteComponent(anyString()))
            .thenAnswer(invocation -> {
                String id = invocation.getArgument(0);
                if ("id2".equals(id)) {
                    throw new IOException("Delete failed for id2");
                }
                return null;
            });

        // Should continue despite failures
        assertDoesNotThrow(() ->
            service.deleteFromRepository("test-repo", null, false, null)
        );

        verify(mockClient).deleteComponent("id1");
        verify(mockClient).deleteComponent("id2");
        verify(mockClient).deleteComponent("id3");
    }

    @Test
    @DisplayName("Bulk delete 1000 components")
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void testBulkDelete1000Components() throws Exception {
        List<RepoRecord> records = new ArrayList<>();
        for (int i = 1; i <= 1000; i++) {
            records.add(new RepoRecord("id" + i, i * 100, "file" + i + ".jar"));
        }

        when(mockClient.listComponents(anyString(), anyBoolean()))
            .thenReturn(records);

        when(mockClient.deleteComponent(anyString()))
            .thenAnswer(invocation -> {
                Thread.sleep(10); // Simulate network call
                return null;
            });

        long startTime = System.currentTimeMillis();
        service.deleteFromRepository("test-repo", null, false, null);
        long duration = System.currentTimeMillis() - startTime;

        verify(mockClient, times(1000)).deleteComponent(anyString());
        assertTrue(duration < 60000, "Should complete 1000 deletes in <60 seconds");
    }

    @Test
    @DisplayName("Delete respects dry-run mode")
    void testDeleteDryRunMode() throws Exception {
        List<RepoRecord> records = List.of(
            new RepoRecord("id1", 1024, "file1.jar"),
            new RepoRecord("id2", 2048, "file2.jar")
        );

        when(mockClient.listComponents(anyString(), anyBoolean()))
            .thenReturn(records);

        service.deleteFromRepository("test-repo", null, true, null); // dry-run = true

        verify(mockClient).listComponents(anyString(), anyBoolean());
        verify(mockClient, never()).deleteComponent(anyString()); // Should NOT delete
    }

    @Test
    @DisplayName("Delete with regex filter")
    void testDeleteWithRegexFilter() throws Exception {
        List<RepoRecord> records = List.of(
            new RepoRecord("id1", 1024, "test-file.jar"),
            new RepoRecord("id2", 2048, "production-file.jar"),
            new RepoRecord("id3", 512, "test-other.jar")
        );

        when(mockClient.listComponents(anyString(), anyBoolean()))
            .thenReturn(records);

        // Delete only files matching "test-*"
        service.deleteFromRepository("test-repo", "test-.*", false, null);

        // Should delete id1 and id3 (test-file and test-other), not id2
        verify(mockClient).deleteComponent("id1");
        verify(mockClient, never()).deleteComponent("id2");
        verify(mockClient).deleteComponent("id3");
    }

    @Test
    @DisplayName("Delete during concurrent pagination")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testDeleteDuringPagination() throws Exception {
        List<RepoRecord> records = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            records.add(new RepoRecord("id" + i, i * 100, "file" + i + ".jar"));
        }

        when(mockClient.listComponents(anyString(), anyBoolean()))
            .thenReturn(records);

        when(mockClient.deleteComponent(anyString()))
            .thenAnswer(invocation -> {
                Thread.sleep(5); // Simulate network call
                return null;
            });

        ExecutorService executor = Executors.newFixedThreadPool(2);

        executor.submit(() -> {
            try {
                service.deleteFromRepository("test-repo", null, false, null);
            } catch (IOException | InterruptedException e) {
                fail("Delete failed: " + e.getMessage());
            }
        });

        executor.submit(() -> {
            try {
                Thread.sleep(50);
                var freshRecords = service.getRepositoryRecords("test-repo", null, true);
                assertNotNull(freshRecords);
            } catch (IOException | InterruptedException e) {
                // Expected - concurrent operations may interfere
            }
        });

        executor.shutdown();
        assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("Delete with null callback")
    void testDeleteWithNullCallback() throws Exception {
        List<RepoRecord> records = List.of(
            new RepoRecord("id1", 1024, "file1.jar")
        );

        when(mockClient.listComponents(anyString(), anyBoolean()))
            .thenReturn(records);

        assertDoesNotThrow(() ->
            service.deleteFromRepository("test-repo", null, false, null),
            "Should handle null callback gracefully"
        );

        verify(mockClient).deleteComponent("id1");
    }

    @Test
    @DisplayName("Delete component with special characters in path")
    void testDeleteComponentWithSpecialCharacters() throws Exception {
        List<RepoRecord> records = List.of(
            new RepoRecord("id1", 1024, "file with spaces.jar"),
            new RepoRecord("id2", 2048, "file-with-dashes.jar"),
            new RepoRecord("id3", 512, "file_with_underscores.jar"),
            new RepoRecord("id4", 256, "file.multiple.dots.jar")
        );

        when(mockClient.listComponents(anyString(), anyBoolean()))
            .thenReturn(records);

        service.deleteFromRepository("test-repo", null, false, null);

        for (int i = 1; i <= 4; i++) {
            verify(mockClient).deleteComponent("id" + i);
        }
    }

    @Test
    @DisplayName("Delete maintains order (FIFO)")
    void testDeleteMaintainsOrder() throws Exception {
        List<RepoRecord> records = List.of(
            new RepoRecord("id1", 1024, "file1.jar"),
            new RepoRecord("id2", 2048, "file2.jar"),
            new RepoRecord("id3", 512, "file3.jar")
        );

        when(mockClient.listComponents(anyString(), anyBoolean()))
            .thenReturn(records);

        List<String> deleteOrder = new ArrayList<>();
        when(mockClient.deleteComponent(anyString()))
            .thenAnswer(invocation -> {
                String id = invocation.getArgument(0);
                deleteOrder.add(id);
                return null;
            });

        service.deleteFromRepository("test-repo", null, false, null);

        // Verify order (should delete in same order as list)
        assertEquals(List.of("id1", "id2", "id3"), deleteOrder);
    }
}

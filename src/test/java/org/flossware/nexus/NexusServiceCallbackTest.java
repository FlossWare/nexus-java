package org.flossware.nexus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for NexusService callback exception handling.
 * Tests uncovered exception handling paths in progress callbacks.
 */
class NexusServiceCallbackTest {

    @Mock
    private NexusHttpClient mockClient;

    @Mock
    private ProgressCallback mockCallback;

    private NexusService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new NexusService(mockClient);
    }

    @Test
    void testDeleteComponents_callbackExceptionOnDeleteProgress() throws IOException, InterruptedException {
        // Setup: Component list (use RepoRecord, not ComponentMetadata)
        List<RepoRecord> components = Arrays.asList(
            new RepoRecord("id1", 1000L, "path1.jar"),
            new RepoRecord("id2", 2000L, "path2.jar"),
            new RepoRecord("id3", 3000L, "path3.jar")
        );

        when(mockClient.listComponents("test-repo", true)).thenReturn(components);
        doNothing().when(mockClient).deleteComponent(anyString());

        // Callback throws exception on progress update
        doThrow(new RuntimeException("Callback error"))
            .when(mockCallback).onDeleteProgress(anyInt(), anyInt(), anyDouble());

        // Execute deletion with callback - should not fail despite callback exception
        assertDoesNotThrow(() -> {
            service.deleteFromRepository("test-repo", ".*", false, mockCallback);
        });

        // Verify all deletions still happened
        verify(mockClient, times(3)).deleteComponent(anyString());
    }

    @Test
    void testDeleteComponents_callbackExceptionOnDeleteFailed() throws IOException, InterruptedException {
        // Setup: Component list (use RepoRecord, not ComponentMetadata)
        List<RepoRecord> components = Arrays.asList(
            new RepoRecord("id1", 1000L, "path1.jar"),
            new RepoRecord("id2", 2000L, "path2.jar")
        );

        when(mockClient.listComponents("test-repo", true)).thenReturn(components);

        // First delete succeeds, second fails
        doNothing().when(mockClient).deleteComponent("id1");
        doThrow(new IOException("Delete failed"))
            .when(mockClient).deleteComponent("id2");

        // Callback throws exception when notified of failure
        doThrow(new RuntimeException("Callback error"))
            .when(mockCallback).onComponentDeleteFailed(anyString(), anyString(), anyString());

        // Execute deletion - should not fail despite callback exception
        assertDoesNotThrow(() -> {
            service.deleteFromRepository("test-repo", ".*", false, mockCallback);
        });

        // Verify both delete attempts were made
        verify(mockClient).deleteComponent("id1");
        verify(mockClient).deleteComponent("id2");

        // Verify callback was called despite throwing exception
        verify(mockCallback).onComponentDeleteFailed(eq("id2"), eq("path2.jar"), anyString());
    }

    @Test
    void testDeleteComponents_nullCallbackSafe() throws IOException, InterruptedException {
        // Setup (use RepoRecord, not ComponentMetadata)
        List<RepoRecord> components = Arrays.asList(
            new RepoRecord("id1", 1000L, "path1.jar")
        );

        when(mockClient.listComponents("test-repo", true)).thenReturn(components);
        doNothing().when(mockClient).deleteComponent(anyString());

        // Execute with null callback - should not fail
        assertDoesNotThrow(() -> {
            service.deleteFromRepository("test-repo", ".*", false, null);
        });

        // Verify deletion happened
        verify(mockClient).deleteComponent("id1");
    }

    @Test
    void testDeleteComponents_callbackExceptionOnComplete() throws IOException, InterruptedException {
        // Setup (use RepoRecord, not ComponentMetadata)
        List<RepoRecord> components = Arrays.asList(
            new RepoRecord("id1", 1000L, "path1.jar")
        );

        when(mockClient.listComponents("test-repo", true)).thenReturn(components);
        doNothing().when(mockClient).deleteComponent(anyString());

        // Callback throws exception on complete
        doThrow(new RuntimeException("Callback complete error"))
            .when(mockCallback).onDeleteComplete(anyInt(), anyInt());

        // Execute deletion - should not fail despite callback exception
        assertDoesNotThrow(() -> {
            service.deleteFromRepository("test-repo", ".*", false, mockCallback);
        });

        // Verify deletion happened
        verify(mockClient).deleteComponent("id1");
    }
}

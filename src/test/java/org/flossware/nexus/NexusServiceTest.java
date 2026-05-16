package org.flossware.nexus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NexusServiceTest {

    @Mock
    private NexusClient mockClient;

    private NexusService service;
    private ByteArrayOutputStream outputStream;
    private PrintStream originalOut;

    @BeforeEach
    void setUp() {
        service = new NexusService(mockClient);
        outputStream = new ByteArrayOutputStream();
        originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
    }

    @Test
    void testListRepositoryWithoutFilter() throws IOException, InterruptedException {
        List<RepoRecord> mockRecords = List.of(
            new RepoRecord("id1", 1000, "path/to/artifact1.jar"),
            new RepoRecord("id2", 2000, "path/to/artifact2.jar")
        );

        when(mockClient.listComponents("test-repo")).thenReturn(mockRecords);

        service.listRepository("test-repo", null);

        verify(mockClient, times(1)).listComponents("test-repo");
        String output = outputStream.toString();
        assertTrue(output.contains("artifact1.jar"));
        assertTrue(output.contains("artifact2.jar"));
        assertTrue(output.contains("Total components"));
    }

    @Test
    void testListRepositoryWithFilter() throws IOException, InterruptedException {
        List<RepoRecord> mockRecords = List.of(
            new RepoRecord("id1", 1000, "path/to/artifact-1.0.0.jar"),
            new RepoRecord("id2", 2000, "path/to/artifact-1.0.1-SNAPSHOT.jar"),
            new RepoRecord("id3", 3000, "path/to/artifact-2.0.0.jar")
        );

        when(mockClient.listComponents("test-repo")).thenReturn(mockRecords);

        service.listRepository("test-repo", ".*SNAPSHOT.*");

        verify(mockClient, times(1)).listComponents("test-repo");
        String output = outputStream.toString();
        assertFalse(output.contains("artifact-1.0.0.jar"));
        assertTrue(output.contains("SNAPSHOT"));
        assertTrue(output.contains("Matching components:"));
    }

    @Test
    void testDeleteFromRepositoryDryRun() throws IOException, InterruptedException {
        List<RepoRecord> mockRecords = List.of(
            new RepoRecord("id1", 1000, "path/to/artifact1.jar")
        );

        when(mockClient.listComponents("test-repo")).thenReturn(mockRecords);

        service.deleteFromRepository("test-repo", null, true);

        verify(mockClient, times(1)).listComponents("test-repo");
        verify(mockClient, never()).deleteComponent(anyString());

        String output = outputStream.toString();
        assertTrue(output.contains("Would delete:"));
    }

    @Test
    void testDeleteFromRepositoryActual() throws IOException, InterruptedException {
        List<RepoRecord> mockRecords = List.of(
            new RepoRecord("id1", 1000, "path/to/artifact1.jar"),
            new RepoRecord("id2", 2000, "path/to/artifact2.jar")
        );

        when(mockClient.listComponents("test-repo")).thenReturn(mockRecords);
        doNothing().when(mockClient).deleteComponent(anyString());

        service.deleteFromRepository("test-repo", null, false);

        verify(mockClient, times(1)).listComponents("test-repo");
        verify(mockClient, times(1)).deleteComponent("id1");
        verify(mockClient, times(1)).deleteComponent("id2");

        String output = outputStream.toString();
        assertTrue(output.contains("Deleting:"));
        assertTrue(output.contains("Deleted 2 of 2"));
    }

    @Test
    void testDeleteFromRepositoryWithFilter() throws IOException, InterruptedException {
        List<RepoRecord> mockRecords = List.of(
            new RepoRecord("id1", 1000, "path/to/artifact-1.0.0.jar"),
            new RepoRecord("id2", 2000, "path/to/artifact-SNAPSHOT.jar")
        );

        when(mockClient.listComponents("test-repo")).thenReturn(mockRecords);
        doNothing().when(mockClient).deleteComponent(anyString());

        service.deleteFromRepository("test-repo", ".*SNAPSHOT.*", false);

        verify(mockClient, times(1)).listComponents("test-repo");
        verify(mockClient, times(1)).deleteComponent("id2");
        verify(mockClient, never()).deleteComponent("id1");
    }

    @Test
    void testDeleteFromRepositoryNoMatches() throws IOException, InterruptedException {
        List<RepoRecord> mockRecords = List.of(
            new RepoRecord("id1", 1000, "path/to/artifact.jar")
        );

        when(mockClient.listComponents("test-repo")).thenReturn(mockRecords);

        service.deleteFromRepository("test-repo", ".*SNAPSHOT.*", false);

        verify(mockClient, times(1)).listComponents("test-repo");
        verify(mockClient, never()).deleteComponent(anyString());

        String output = outputStream.toString();
        assertTrue(output.contains("No components match"));
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }
}

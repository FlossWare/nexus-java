package org.flossware.jnexus;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NexusServiceAdvancedTest {

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
    void testListRepositoryWithEmptyResults() throws IOException, InterruptedException {
        when(mockClient.listComponents(eq("empty-repo"), anyBoolean())).thenReturn(new ArrayList<>());

        service.listRepository("empty-repo", null);

        verify(mockClient, times(1)).listComponents(eq("empty-repo"), anyBoolean());
        String output = outputStream.toString();
        assertTrue(output.contains("Total components in repository: 0"));
    }

    @Test
    void testListRepositoryWithFilterMatchingNone() throws IOException, InterruptedException {
        List<RepoRecord> mockRecords = List.of(
            new RepoRecord("id1", 1000, "com/example/artifact-1.0.0.jar"),
            new RepoRecord("id2", 2000, "com/example/artifact-2.0.0.jar")
        );

        when(mockClient.listComponents(eq("test-repo"), anyBoolean())).thenReturn(mockRecords);

        service.listRepository("test-repo", ".*SNAPSHOT.*");

        String output = outputStream.toString();
        assertTrue(output.contains("Total components in repository: 2"));
        assertTrue(output.contains("Matching components:            0"));
        assertFalse(output.contains("artifact-1.0.0"));
    }

    @Test
    void testListRepositoryWithComplexRegex() throws IOException, InterruptedException {
        List<RepoRecord> mockRecords = List.of(
            new RepoRecord("id1", 1000, "com/example/app-1.0.0-SNAPSHOT.jar"),
            new RepoRecord("id2", 2000, "com/example/app-1.0.1-SNAPSHOT.jar"),
            new RepoRecord("id3", 3000, "com/example/app-1.0.0.jar"),
            new RepoRecord("id4", 4000, "com/other/lib-2.0.0-SNAPSHOT.jar")
        );

        when(mockClient.listComponents(eq("test-repo"), anyBoolean())).thenReturn(mockRecords);

        service.listRepository("test-repo", "com/example/.*-SNAPSHOT.*");

        String output = outputStream.toString();
        assertTrue(output.contains("Matching components:            2"));
        assertTrue(output.contains("app-1.0.0-SNAPSHOT"));
        assertTrue(output.contains("app-1.0.1-SNAPSHOT"));
        assertFalse(output.contains("lib-2.0.0-SNAPSHOT"));
    }

    @Test
    void testListRepositoryIOException() throws IOException, InterruptedException {
        when(mockClient.listComponents(eq("error-repo"), anyBoolean()))
            .thenThrow(new IOException("Network error"));

        assertThrows(IOException.class, () -> {
            service.listRepository("error-repo", null);
        });

        verify(mockClient, times(1)).listComponents(eq("error-repo"), anyBoolean());
    }

    @Test
    void testDeleteFromRepositoryWithEmptyResults() throws IOException, InterruptedException {
        when(mockClient.listComponents(eq("empty-repo"), anyBoolean())).thenReturn(new ArrayList<>());

        service.deleteFromRepository("empty-repo", null, false);

        verify(mockClient, times(1)).listComponents(eq("empty-repo"), anyBoolean());
        verify(mockClient, never()).deleteComponent(anyString());

        String output = outputStream.toString();
        assertTrue(output.contains("No components match"));
    }

    @Test
    void testDeleteFromRepositoryPartialFailure() throws IOException, InterruptedException {
        List<RepoRecord> mockRecords = List.of(
            new RepoRecord("id1", 1000, "artifact1.jar"),
            new RepoRecord("id2", 2000, "artifact2.jar"),
            new RepoRecord("id3", 3000, "artifact3.jar")
        );

        when(mockClient.listComponents(eq("test-repo"), anyBoolean())).thenReturn(mockRecords);
        doNothing().when(mockClient).deleteComponent("id1");
        doThrow(new IOException("Delete failed")).when(mockClient).deleteComponent("id2");
        doNothing().when(mockClient).deleteComponent("id3");

        service.deleteFromRepository("test-repo", null, false);

        verify(mockClient, times(1)).deleteComponent("id1");
        verify(mockClient, times(1)).deleteComponent("id2");
        verify(mockClient, times(1)).deleteComponent("id3");

        String output = outputStream.toString();
        assertTrue(output.contains("Deleted 2 of 3"));

        String errors = errorStream.toString();
        assertTrue(errors.contains("Failed to delete artifact2.jar"));
    }

    @Test
    void testDeleteFromRepositoryDryRunMultipleItems() throws IOException, InterruptedException {
        List<RepoRecord> mockRecords = List.of(
            new RepoRecord("id1", 1000, "artifact1.jar"),
            new RepoRecord("id2", 2000, "artifact2.jar"),
            new RepoRecord("id3", 3000, "artifact3.jar")
        );

        when(mockClient.listComponents(eq("test-repo"), anyBoolean())).thenReturn(mockRecords);

        service.deleteFromRepository("test-repo", null, true);

        verify(mockClient, times(1)).listComponents(eq("test-repo"), anyBoolean());
        verify(mockClient, never()).deleteComponent(anyString());

        String output = outputStream.toString();
        assertTrue(output.contains("Would delete:"));
        assertTrue(output.contains("artifact1.jar"));
        assertTrue(output.contains("artifact2.jar"));
        assertTrue(output.contains("artifact3.jar"));
    }

    @Test
    void testDeleteFromRepositoryWithFilteredResults() throws IOException, InterruptedException {
        List<RepoRecord> mockRecords = List.of(
            new RepoRecord("id1", 1000, "com/example/app-1.0.0.jar"),
            new RepoRecord("id2", 2000, "com/example/app-1.0.0-SNAPSHOT.jar"),
            new RepoRecord("id3", 3000, "com/example/app-2.0.0-SNAPSHOT.jar"),
            new RepoRecord("id4", 4000, "com/example/app-2.0.0.jar")
        );

        when(mockClient.listComponents(eq("test-repo"), anyBoolean())).thenReturn(mockRecords);
        doNothing().when(mockClient).deleteComponent(anyString());

        service.deleteFromRepository("test-repo", ".*SNAPSHOT.*", false);

        verify(mockClient, times(1)).listComponents(eq("test-repo"), anyBoolean());
        verify(mockClient, times(1)).deleteComponent("id2");
        verify(mockClient, times(1)).deleteComponent("id3");
        verify(mockClient, never()).deleteComponent("id1");
        verify(mockClient, never()).deleteComponent("id4");

        String output = outputStream.toString();
        assertTrue(output.contains("Deleted 2 of 2"));
    }

    @Test
    void testDeleteFromRepositoryLargeDataSet() throws IOException, InterruptedException {
        List<RepoRecord> mockRecords = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            mockRecords.add(new RepoRecord("id" + i, i * 1000, "artifact" + i + ".jar"));
        }

        when(mockClient.listComponents(eq("large-repo"), anyBoolean())).thenReturn(mockRecords);
        doNothing().when(mockClient).deleteComponent(anyString());

        service.deleteFromRepository("large-repo", null, false);

        verify(mockClient, times(1)).listComponents(eq("large-repo"), anyBoolean());
        verify(mockClient, times(100)).deleteComponent(anyString());

        String output = outputStream.toString();
        assertTrue(output.contains("Deleted 100 of 100"));
    }

    @Test
    void testListRepositoryStatisticsCalculation() throws IOException, InterruptedException {
        List<RepoRecord> mockRecords = List.of(
            new RepoRecord("id1", 1024 * 1024, "file1.jar"),
            new RepoRecord("id2", 2 * 1024 * 1024, "file2.jar"),
            new RepoRecord("id3", 512 * 1024, "file3.jar")
        );

        when(mockClient.listComponents(eq("test-repo"), anyBoolean())).thenReturn(mockRecords);

        service.listRepository("test-repo", null);

        String output = outputStream.toString();
        assertTrue(output.contains("Total size:"));
        assertTrue(output.contains("MB"));
    }

    @Test
    void testDeleteFromRepositoryInvalidRegex() {
        // No need to stub listComponents - regex validation happens before the call
        assertThrows(IllegalArgumentException.class, () -> {
            service.deleteFromRepository("test-repo", "[invalid(regex", false);
        });
    }

    @Test
    void testListRepositoryFormatsOutput() throws IOException, InterruptedException {
        List<RepoRecord> mockRecords = List.of(
            new RepoRecord("abc123", 1234567, "com/example/artifact-1.0.0.jar")
        );

        when(mockClient.listComponents(eq("test-repo"), anyBoolean())).thenReturn(mockRecords);

        service.listRepository("test-repo", null);

        String output = outputStream.toString();
        assertTrue(output.contains("abc123"));
        assertTrue(output.contains("1,234,567"));
        assertTrue(output.contains("com/example/artifact-1.0.0.jar"));
    }
}

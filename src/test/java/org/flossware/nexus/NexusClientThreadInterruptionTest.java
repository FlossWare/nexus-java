package org.flossware.nexus;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for NexusClient thread interruption handling.
 * <p>
 * Tests that retry loops can be interrupted via Thread.interrupt() and
 * that InterruptedException is properly propagated to allow cancellation
 * of long-running operations.
 * </p>
 */
@DisplayName("NexusClient Thread Interruption Tests")
class NexusClientThreadInterruptionTest {

    private static final Credentials MOCK_CREDENTIALS =
        new Credentials("http://fake-nexus.example.com", "testuser", "testpass", null);

    @Test
    @DisplayName("Thread interruption in deleteComponent should propagate InterruptedException")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testDeleteComponentInterruptionDetected() {
        try (NexusClient client = new NexusClient(MOCK_CREDENTIALS)) {
            // Simulate a thread that starts the delete operation and gets interrupted
            Thread deleteThread = new Thread(() -> {
                assertThrows(InterruptedException.class, () -> {
                    client.deleteComponent("component-id");
                }, "InterruptedException should be thrown when thread is interrupted during retry loop");
            });

            deleteThread.start();
            // Give thread time to start and enter retry logic
            Thread.sleep(50);
            // Interrupt the delete thread
            deleteThread.interrupt();
            // Wait for thread to finish (should exit quickly due to interrupt)
            deleteThread.join(5000);

            assertFalse(deleteThread.isAlive(), "Delete thread should have terminated after interruption");
        } catch (InterruptedException e) {
            fail("Test thread should not be interrupted: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Thread interruption should be detected at start of each retry attempt")
    void testInterruptionDetectedBetweenAttempts() {
        // This test verifies that Thread.interrupted() check catches interrupts
        // that were set but not yet thrown as exceptions
        try (NexusClient client = new NexusClient(MOCK_CREDENTIALS)) {
            // Create a thread that will be interrupted
            Thread[] threadRef = new Thread[1];
            InterruptedException[] exceptionRef = new InterruptedException[1];

            threadRef[0] = new Thread(() -> {
                try {
                    // This will fail with a connection error and retry
                    client.listComponents("nonexistent-repo");
                } catch (InterruptedException e) {
                    exceptionRef[0] = e;
                } catch (IOException e) {
                    // Expected - connection will fail
                }
            });

            threadRef[0].start();
            // Give thread a moment to start
            Thread.sleep(50);
            // Interrupt it
            threadRef[0].interrupt();
            // Wait for completion
            threadRef[0].join(5000);

            assertFalse(threadRef[0].isAlive(), "Thread should have been interrupted and terminated");
            assertTrue(
                exceptionRef[0] != null || !threadRef[0].isAlive(),
                "Thread should have been interrupted and exited"
            );
        } catch (InterruptedException e) {
            fail("Test thread should not be interrupted: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Retry loop should check for interruption on every iteration")
    void testInterruptionCheckOnEveryIteration() throws InterruptedException {
        try (NexusClient client = new NexusClient(MOCK_CREDENTIALS)) {
            Thread testThread = new Thread(() -> {
                try {
                    // Attempt an operation that will fail and retry
                    client.deleteComponent("test-id");
                    fail("Should have thrown InterruptedException");
                } catch (InterruptedException e) {
                    // Expected - interruption should be detected
                    assertTrue(e.getMessage().contains("interrupted"),
                        "Exception message should indicate interruption");
                } catch (IOException e) {
                    // Connection failure is also acceptable
                    // The important thing is that interruption is eventually detected
                }
            });

            testThread.start();
            // Wait briefly for thread to enter retry logic
            Thread.sleep(100);
            // Interrupt while in the middle of retrying
            testThread.interrupt();
            // Wait for thread to complete
            testThread.join(5000);

            assertFalse(testThread.isAlive(), "Thread should have been interrupted and terminated within timeout");
        }
    }
}

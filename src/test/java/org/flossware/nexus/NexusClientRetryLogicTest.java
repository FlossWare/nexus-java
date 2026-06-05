package org.flossware.nexus;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.PortUnreachableException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.http.HttpTimeoutException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for NexusClient retry logic.
 * <p>
 * Tests the isRetryable() method to ensure it correctly identifies
 * transient errors that should trigger automatic retries, while avoiding
 * false positives and handling edge cases.
 * </p>
 */
@DisplayName("NexusClient Retry Logic Tests")
class NexusClientRetryLogicTest {

    private static final Credentials MOCK_CREDENTIALS = new Credentials() {
        @Override
        public String getUrl() { return "http://fake-nexus.example.com"; }
        @Override
        public String getUser() { return "testuser"; }
        @Override
        public String getPassword() { return "testpass"; }
        @Override
        public String getProfile() { return null; }
        @Override
        public java.util.List<String> getRepositories() { return java.util.List.of(); }
        @Override
        public int getHttpTimeoutSeconds() { return 30; }
        @Override
        public int getMaxRetries() { return 3; }
        @Override
        public long getInitialRetryDelayMs() { return 1000; }
    };

    /**
     * Helper method to invoke the private isRetryable() method via reflection.
     */
    private boolean isRetryable(NexusClient client, IOException e) throws Exception {
        Method method = NexusClient.class.getDeclaredMethod("isRetryable", IOException.class);
        method.setAccessible(true);
        return (Boolean) method.invoke(client, e);
    }

    @Test
    @DisplayName("SocketTimeoutException should be retryable")
    void testSocketTimeoutException_isRetryable() throws Exception {
        try (NexusClient client = new NexusClient(MOCK_CREDENTIALS)) {
            SocketTimeoutException e = new SocketTimeoutException("Connection timed out");
            assertTrue(isRetryable(client, e), "SocketTimeoutException should be retryable");
        }
    }

    @Test
    @DisplayName("HttpTimeoutException should be retryable")
    void testHttpTimeoutException_isRetryable() throws Exception {
        try (NexusClient client = new NexusClient(MOCK_CREDENTIALS)) {
            HttpTimeoutException e = new HttpTimeoutException("HTTP request timed out");
            assertTrue(isRetryable(client, e), "HttpTimeoutException should be retryable");
        }
    }

    @Test
    @DisplayName("ConnectException should be retryable")
    void testConnectException_isRetryable() throws Exception {
        try (NexusClient client = new NexusClient(MOCK_CREDENTIALS)) {
            ConnectException e = new ConnectException("Connection refused");
            assertTrue(isRetryable(client, e), "ConnectException should be retryable");
        }
    }

    @Test
    @DisplayName("NoRouteToHostException should be retryable")
    void testNoRouteToHostException_isRetryable() throws Exception {
        try (NexusClient client = new NexusClient(MOCK_CREDENTIALS)) {
            NoRouteToHostException e = new NoRouteToHostException("No route to host");
            assertTrue(isRetryable(client, e), "NoRouteToHostException should be retryable");
        }
    }

    @Test
    @DisplayName("UnknownHostException should be retryable")
    void testUnknownHostException_isRetryable() throws Exception {
        try (NexusClient client = new NexusClient(MOCK_CREDENTIALS)) {
            UnknownHostException e = new UnknownHostException("Unknown host");
            assertTrue(isRetryable(client, e), "UnknownHostException should be retryable");
        }
    }

    @Test
    @DisplayName("PortUnreachableException should be retryable")
    void testPortUnreachableException_isRetryable() throws Exception {
        try (NexusClient client = new NexusClient(MOCK_CREDENTIALS)) {
            PortUnreachableException e = new PortUnreachableException("Port unreachable");
            assertTrue(isRetryable(client, e), "PortUnreachableException should be retryable");
        }
    }

    @Test
    @DisplayName("HTTP 500 error should be retryable")
    void testHttp500_isRetryable() throws Exception {
        try (NexusClient client = new NexusClient(MOCK_CREDENTIALS)) {
            IOException e = new IOException("HTTP 500: Internal Server Error");
            assertTrue(isRetryable(client, e), "HTTP 500 should be retryable");
        }
    }

    @Test
    @DisplayName("HTTP 503 error should be retryable")
    void testHttp503_isRetryable() throws Exception {
        try (NexusClient client = new NexusClient(MOCK_CREDENTIALS)) {
            IOException e = new IOException("HTTP 503: Service Unavailable");
            assertTrue(isRetryable(client, e), "HTTP 503 should be retryable");
        }
    }

    @Test
    @DisplayName("HTTP 504 error should be retryable")
    void testHttp504_isRetryable() throws Exception {
        try (NexusClient client = new NexusClient(MOCK_CREDENTIALS)) {
            IOException e = new IOException("HTTP 504: Gateway Timeout");
            assertTrue(isRetryable(client, e), "HTTP 504 should be retryable");
        }
    }

    @Test
    @DisplayName("HTTP 408 (Request Timeout) should be retryable")
    void testHttp408_isRetryable() throws Exception {
        try (NexusClient client = new NexusClient(MOCK_CREDENTIALS)) {
            IOException e = new IOException("HTTP 408: Request Timeout");
            assertTrue(isRetryable(client, e), "HTTP 408 should be retryable");
        }
    }

    @Test
    @DisplayName("HTTP 429 (Too Many Requests) should be retryable")
    void testHttp429_isRetryable() throws Exception {
        try (NexusClient client = new NexusClient(MOCK_CREDENTIALS)) {
            IOException e = new IOException("HTTP 429: Too Many Requests");
            assertTrue(isRetryable(client, e), "HTTP 429 should be retryable");
        }
    }

    @Test
    @DisplayName("Message with 'timeout' keyword should be retryable")
    void testTimeoutKeyword_isRetryable() throws Exception {
        try (NexusClient client = new NexusClient(MOCK_CREDENTIALS)) {
            IOException e = new IOException("Connection timeout");
            assertTrue(isRetryable(client, e), "Message with 'timeout' should be retryable");
        }
    }

    @Test
    @DisplayName("Message with 'timed out' keyword should be retryable")
    void testTimedOutKeyword_isRetryable() throws Exception {
        try (NexusClient client = new NexusClient(MOCK_CREDENTIALS)) {
            IOException e = new IOException("Read timed out");
            assertTrue(isRetryable(client, e), "Message with 'timed out' should be retryable");
        }
    }

    @Test
    @DisplayName("Message with 'connection reset' should be retryable")
    void testConnectionResetKeyword_isRetryable() throws Exception {
        try (NexusClient client = new NexusClient(MOCK_CREDENTIALS)) {
            IOException e = new IOException("Connection reset by peer");
            assertTrue(isRetryable(client, e), "Message with 'connection reset' should be retryable");
        }
    }

    @Test
    @DisplayName("Message with 'connection refused' should be retryable")
    void testConnectionRefusedKeyword_isRetryable() throws Exception {
        try (NexusClient client = new NexusClient(MOCK_CREDENTIALS)) {
            IOException e = new IOException("Connection refused");
            assertTrue(isRetryable(client, e), "Message with 'connection refused' should be retryable");
        }
    }

    @Test
    @DisplayName("Message with 'rate limit' should be retryable")
    void testRateLimitKeyword_isRetryable() throws Exception {
        try (NexusClient client = new NexusClient(MOCK_CREDENTIALS)) {
            IOException e = new IOException("Rate limit exceeded");
            assertTrue(isRetryable(client, e), "Message with 'rate limit' should be retryable");
        }
    }

    @Test
    @DisplayName("Message with 'too many requests' should be retryable")
    void testTooManyRequestsKeyword_isRetryable() throws Exception {
        try (NexusClient client = new NexusClient(MOCK_CREDENTIALS)) {
            IOException e = new IOException("Too many requests from this IP");
            assertTrue(isRetryable(client, e), "Message with 'too many requests' should be retryable");
        }
    }

    @Test
    @DisplayName("HTTP 400 error should NOT be retryable")
    void testHttp400_notRetryable() throws Exception {
        try (NexusClient client = new NexusClient(MOCK_CREDENTIALS)) {
            IOException e = new IOException("HTTP 400: Bad Request");
            assertFalse(isRetryable(client, e), "HTTP 400 should not be retryable");
        }
    }

    @Test
    @DisplayName("HTTP 401 error should NOT be retryable")
    void testHttp401_notRetryable() throws Exception {
        try (NexusClient client = new NexusClient(MOCK_CREDENTIALS)) {
            IOException e = new IOException("HTTP 401: Unauthorized");
            assertFalse(isRetryable(client, e), "HTTP 401 should not be retryable");
        }
    }

    @Test
    @DisplayName("HTTP 403 error should NOT be retryable")
    void testHttp403_notRetryable() throws Exception {
        try (NexusClient client = new NexusClient(MOCK_CREDENTIALS)) {
            IOException e = new IOException("HTTP 403: Forbidden");
            assertFalse(isRetryable(client, e), "HTTP 403 should not be retryable");
        }
    }

    @Test
    @DisplayName("HTTP 404 error should NOT be retryable")
    void testHttp404_notRetryable() throws Exception {
        try (NexusClient client = new NexusClient(MOCK_CREDENTIALS)) {
            IOException e = new IOException("HTTP 404: Not Found");
            assertFalse(isRetryable(client, e), "HTTP 404 should not be retryable");
        }
    }

    @Test
    @DisplayName("Generic IOException should NOT be retryable")
    void testGenericIOException_notRetryable() throws Exception {
        try (NexusClient client = new NexusClient(MOCK_CREDENTIALS)) {
            IOException e = new IOException("Some random error");
            assertFalse(isRetryable(client, e), "Generic IOException should not be retryable");
        }
    }

    @Test
    @DisplayName("IOException with null message should be handled safely")
    void testNullMessage_notRetryable() throws Exception {
        try (NexusClient client = new NexusClient(MOCK_CREDENTIALS)) {
            IOException e = new IOException();
            assertFalse(isRetryable(client, e), "IOException with null message should not be retryable");
        }
    }

    @Test
    @DisplayName("HTTP 502 (Bad Gateway) should be retryable")
    void testHttp502_isRetryable() throws Exception {
        try (NexusClient client = new NexusClient(MOCK_CREDENTIALS)) {
            IOException e = new IOException("HTTP 502: Bad Gateway");
            assertTrue(isRetryable(client, e), "HTTP 502 should be retryable");
        }
    }

    @Test
    @DisplayName("HTTP status with spaces in message should be recognized")
    void testHttpStatusWithSpaces_isRetryable() throws Exception {
        try (NexusClient client = new NexusClient(MOCK_CREDENTIALS)) {
            IOException e = new IOException("Failed: HTTP 503 Service Unavailable");
            assertTrue(isRetryable(client, e), "HTTP 503 with spaces should be retryable");
        }
    }

    @Test
    @DisplayName("Case-insensitive keyword matching for 'timeout'")
    void testTimeoutKeyword_caseInsensitive() throws Exception {
        try (NexusClient client = new NexusClient(MOCK_CREDENTIALS)) {
            IOException e = new IOException("Connection TIMEOUT");
            assertTrue(isRetryable(client, e), "Uppercase TIMEOUT should be recognized");
        }
    }

    @Test
    @DisplayName("HTTP 501 (Not Implemented) should NOT be retryable")
    void testHttp501_notRetryable() throws Exception {
        try (NexusClient client = new NexusClient(MOCK_CREDENTIALS)) {
            IOException e = new IOException("HTTP 501: Not Implemented");
            assertFalse(isRetryable(client, e), "HTTP 501 should not be retryable");
        }
    }
}

package org.flossware.nexus;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.flossware.jnexus.ComponentMetadata;
import org.flossware.jnexus.NexusHttpClient;
import org.flossware.jnexus.RepoRecord;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
<<<<<<< HEAD
import java.util.concurrent.CountDownLatch;
=======
import java.util.concurrent.ExecutorService;
>>>>>>> e17d8af (chore: Remove .claude directory and add to .gitignore)
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * HTTP client for interacting with the Nexus Repository Manager REST API.
 * <p>
 * This class provides low-level HTTP operations for the Nexus REST API, including
 * listing, searching, and deleting components. It uses Java 21's built-in {@link HttpClient}
 * for HTTP communication and handles authentication using HTTP Basic Auth.
 * </p>
 *
 * <h2>Key Features:</h2>
 * <ul>
 *   <li><strong>Automatic Pagination</strong> - Transparently follows continuation tokens to retrieve all results</li>
 *   <li><strong>Smart Caching</strong> - 5-minute TTL cache with configurable duration and manual control</li>
<<<<<<< HEAD
 *   <li><strong>Non-Blocking Retry Logic</strong> - Exponential backoff with ScheduledExecutorService (GUI-safe)</li>
=======
 *   <li><strong>Exponential Backoff Retry Logic</strong> - Automatic retries with exponential backoff for transient errors</li>
>>>>>>> e17d8af (chore: Remove .claude directory and add to .gitignore)
 *   <li><strong>Type Safety</strong> - Exception type checking before message parsing for robust error handling</li>
 *   <li><strong>Metadata Support</strong> - Full component metadata extraction including dates and checksums</li>
 * </ul>
 *
 * <h2>Resource Management:</h2>
 * <p>
 * This class implements {@link AutoCloseable} for proper resource management.
 * Use try-with-resources for automatic cleanup:
 * </p>
 * <pre>
 * Credentials credentials = new Credentials();
 * try (NexusClient client = new NexusClient(credentials)) {
 *     List&lt;RepoRecord&gt; components = client.listComponents("maven-releases");
 *     // ... use client ...
 * } // client.close() called automatically
 * </pre>
 * <p>
 * For long-running GUI applications, close the client when the window closes to free resources.
 * </p>
 *
 * <h2>Usage Examples:</h2>
 * <pre>
 * // Basic usage with try-with-resources
 * try (NexusClient client = new NexusClient(credentials)) {
 *     // List components (uses cache)
 *     List&lt;RepoRecord&gt; components = client.listComponents("maven-releases");
 *
 *     // Force refresh (bypass cache)
 *     List&lt;RepoRecord&gt; fresh = client.listComponents("maven-releases", true);
 *
 *     // List with metadata
 *     List&lt;ComponentMetadata&gt; metadata = client.listComponentsWithMetadata("maven-releases");
 *
 *     // Delete a component
 *     client.deleteComponent("component-id");
 *
 *     // Cache management
 *     boolean cached = client.isCached("maven-releases");
 *     long age = client.getCacheAge("maven-releases");
 *     client.clearCache("maven-releases");
 *     client.clearAllCache();
 * }
 *
 * // Custom cache TTL (10 minutes)
 * try (NexusClient customClient = new NexusClient(credentials, 600)) {
 *     // ... use client ...
 * }
 *
 * // Disable caching (TTL = 0)
 * try (NexusClient nocacheClient = new NexusClient(credentials, 0)) {
 *     // ... use client ...
 * }
 * </pre>
 *
 * <h2>Configuration:</h2>
 * <p>
 * Client behavior is configured via {@link Credentials} (which extends {@link HttpConfig}):
 * </p>
 * <ul>
 *   <li><strong>HTTP Timeout</strong> - Connection timeout in seconds (via {@link HttpConfig#getHttpTimeoutSeconds()}, default: 30)</li>
 *   <li><strong>Max Retries</strong> - Maximum retry attempts for failed requests (via {@link HttpConfig#getMaxRetries()}, default: 3)</li>
 *   <li><strong>Retry Delay</strong> - Initial retry delay with exponential backoff (via {@link HttpConfig#getInitialRetryDelayMs()}, default: 1000ms)</li>
 *   <li><strong>Cache TTL</strong> - Time-to-live for cached results (default: 300s)</li>
 * </ul>
 *
 * <h2>Thread Safety:</h2>
 * <p>
 * This class is thread-safe. The cache uses {@link java.util.concurrent.ConcurrentHashMap}
 * for concurrent access, and the {@link HttpClient} is thread-safe by design.
 * </p>
 *
 * @author sfloess
 * @since 1.0
 * @see Credentials
 * @see NexusService
 * @see RepoRecord
 * @see ComponentMetadata
 */
public class NexusClient implements NexusHttpClient, AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(NexusClient.class);

    private final String baseUrl;
    private final HttpClient httpClient;
    private final ExecutorService httpExecutor;
    private final String authHeader;
    private final ObjectMapper objectMapper;

    // Cache configuration
    private static final long DEFAULT_CACHE_TTL_SECONDS = 300; // 5 minutes
    private final long cacheTtlSeconds;

    // Retry configuration (configurable via Credentials)
    private final int maxRetries;
    private final long initialRetryDelayMs;

    // Non-blocking retry scheduler for GUI-safe operations
    // Single thread is sufficient for delay scheduling (doesn't execute HTTP requests)
    private final ScheduledExecutorService retryScheduler;

    // Cache storage: repository -> CacheEntry
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final Map<String, MetadataCacheEntry> metadataCache = new ConcurrentHashMap<>();

    /**
     * Cache entry holding component list and timestamp.
     */
    private record CacheEntry(List<RepoRecord> records, Instant timestamp) {
        boolean isExpired(long ttlSeconds) {
            return Instant.now().isAfter(timestamp.plusSeconds(ttlSeconds));
        }
    }

    /**
     * Cache entry holding component metadata list and timestamp.
     */
    private record MetadataCacheEntry(List<ComponentMetadata> records, Instant timestamp) {
        boolean isExpired(long ttlSeconds) {
            return Instant.now().isAfter(timestamp.plusSeconds(ttlSeconds));
        }
    }

    /**
     * Constructs a new NexusClient with the provided credentials.
     * <p>
     * Initializes the HTTP client with a 30-second connection timeout,
     * prepares the Basic Authentication header, and uses default cache TTL of 5 minutes.
     * </p>
     *
     * @param credentials the Nexus credentials containing URL, username, and password
     */
    public NexusClient(Credentials credentials) {
        this(credentials, DEFAULT_CACHE_TTL_SECONDS);
    }

    /**
     * Constructs a new NexusClient with the provided credentials and custom cache TTL.
     * <p>
     * Initializes the HTTP client with the configured connection timeout (default 30 seconds)
     * and prepares the Basic Authentication header.
     * </p>
     *
     * @param credentials the Nexus credentials containing URL, username, and password
     * @param cacheTtlSeconds cache time-to-live in seconds (0 to disable caching)
     */
    public NexusClient(Credentials credentials, long cacheTtlSeconds) {
        this.baseUrl = credentials.getUrl();
        this.objectMapper = new ObjectMapper();
        this.cacheTtlSeconds = cacheTtlSeconds;

        String auth = credentials.getUser() + ":" + credentials.getPassword();
        this.authHeader = "Basic " + Base64.getEncoder().encodeToString(auth.getBytes());

        // Initialize retry configuration from credentials
        this.maxRetries = credentials.getMaxRetries();
        this.initialRetryDelayMs = credentials.getInitialRetryDelayMs();

        // Build optimized HTTP client with connection pooling and HTTP/2
<<<<<<< HEAD
        this.httpClient = buildOptimizedHttpClient(credentials.getHttpTimeoutSeconds());

        // Create a single-threaded scheduler for non-blocking retry delays
        // This allows GUI applications to remain responsive during retries
        this.retryScheduler = Executors.newScheduledThreadPool(1, runnable -> {
            Thread t = new Thread(runnable, "Nexus-Retry-Scheduler");
            t.setDaemon(true);
            return t;
        });
=======
        // Store executor so it can be properly shut down in close()
        this.httpExecutor = Executors.newVirtualThreadPerTaskExecutor();
        this.httpClient = buildOptimizedHttpClient(credentials.getHttpTimeoutSeconds(), this.httpExecutor);
>>>>>>> e17d8af (chore: Remove .claude directory and add to .gitignore)
    }

    /**
     * Builds an optimized HttpClient with connection pooling and HTTP/2 support.
     * <p>
     * Optimizations:
     * </p>
     * <ul>
     *   <li><strong>HTTP/2</strong> - Enables multiplexing (multiple requests over one connection)</li>
     *   <li><strong>Virtual Threads</strong> - Lightweight threads for efficient I/O-bound operations (Java 21)</li>
     *   <li><strong>Compression</strong> - Automatically handles gzip/deflate responses</li>
     * </ul>
     * <p>
     * Uses virtual threads instead of a fixed thread pool. Virtual threads are ideal for
     * I/O-bound HTTP operations: they are lightweight (no platform thread per request),
     * scale automatically, and do not require pool sizing decisions.
     * </p>
     *
     * @param timeoutSeconds connection timeout in seconds
     * @param executor the executor service to use for async operations
     * @return configured HttpClient instance
     */
    private static HttpClient buildOptimizedHttpClient(int timeoutSeconds, ExecutorService executor) {
        return HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)  // Prefer HTTP/2 for multiplexing
            .connectTimeout(Duration.ofSeconds(timeoutSeconds))
            .executor(executor)  // Virtual threads for efficient I/O-bound operations
            .build();
    }

    /**
     * Lists all components in the specified repository.
     * <p>
     * This method automatically handles pagination by following continuation tokens
     * returned by the Nexus API until all components have been retrieved.
     * Results are cached for the configured TTL duration (default 5 minutes).
     * </p>
     *
     * @param repository the name of the repository to list components from
     * @return a list of all components found in the repository
     * @throws IOException          if an I/O error occurs during the HTTP request
     * @throws InterruptedException if the operation is interrupted
     */
    public List<RepoRecord> listComponents(String repository) throws IOException, InterruptedException {
        return listComponents(repository, false);
    }

    /**
     * Lists all components in the specified repository with optional cache bypass.
     * <p>
     * This method automatically handles pagination by following continuation tokens
     * returned by the Nexus API until all components have been retrieved.
     * Results are cached for the configured TTL duration unless forceRefresh is true.
     * </p>
     *
     * @param repository the name of the repository to list components from
     * @param forceRefresh if true, bypasses cache and fetches fresh data
     * @return a list of all components found in the repository
     * @throws IOException          if an I/O error occurs during the HTTP request
     * @throws InterruptedException if the operation is interrupted
     */
    @Override
    public List<RepoRecord> listComponents(String repository, boolean forceRefresh) throws IOException, InterruptedException {
        // Validate repository name
        Credentials.validateRepository(repository);

        // Check cache if not forcing refresh and caching is enabled
        if (!forceRefresh && cacheTtlSeconds > 0) {
            CacheEntry entry = cache.get(repository);
            if (entry != null && !entry.isExpired(cacheTtlSeconds)) {
                long age = Duration.between(entry.timestamp(), Instant.now()).getSeconds();
                logger.debug("Cache HIT for repository: {} (age: {}s)", repository, age);
                return new LinkedList<>(entry.records()); // Return copy to prevent modification
            }
        }

        // Cache miss or expired - fetch from server
        logger.debug("Cache MISS for repository: {} - fetching from server", repository);
        List<RepoRecord> records = new LinkedList<>();
        String encodedRepository = URLEncoder.encode(repository, StandardCharsets.UTF_8);
        String url = baseUrl + "/service/rest/v1/components?repository=" + encodedRepository;
        String continuationToken = null;

        do {
            String fetchUrl = continuationToken == null
                ? url
                : url + "&continuationToken=" + URLEncoder.encode(continuationToken, StandardCharsets.UTF_8);

            ComponentsResponse response = fetchComponents(fetchUrl);
            records.addAll(response.records());
            continuationToken = response.continuationToken();
        } while (continuationToken != null);

        // Update cache if caching is enabled
        if (cacheTtlSeconds > 0) {
            cache.put(repository, new CacheEntry(new LinkedList<>(records), Instant.now()));
        }

        return records;
    }

    /**
     * Lists all components with full metadata in the specified repository.
     * <p>
     * This method automatically handles pagination and uses the default cache behavior.
     * </p>
     *
     * @param repository the name of the repository to list components from
     * @return a list of all components with full metadata found in the repository
     * @throws IOException          if an I/O error occurs during the HTTP request
     * @throws InterruptedException if the operation is interrupted
     */
    public List<ComponentMetadata> listComponentsWithMetadata(String repository) throws IOException, InterruptedException {
        return listComponentsWithMetadata(repository, false);
    }

    /**
     * Lists all components with full metadata in the specified repository with optional cache bypass.
     * <p>
     * This method automatically handles pagination by following continuation tokens
     * returned by the Nexus API until all components have been retrieved.
     * Results are cached for the configured TTL duration unless forceRefresh is true.
     * </p>
     * <p>
     * Extracts full metadata including content type, format, creation date, last modified date,
     * and checksums from the Nexus API response.
     * </p>
     *
     * @param repository the name of the repository to list components from
     * @param forceRefresh if true, bypasses cache and fetches fresh data
     * @return a list of all components with full metadata found in the repository
     * @throws IOException          if an I/O error occurs during the HTTP request
     * @throws InterruptedException if the operation is interrupted
     */
    @Override
    public List<ComponentMetadata> listComponentsWithMetadata(String repository, boolean forceRefresh) throws IOException, InterruptedException {
        // Validate repository name
        Credentials.validateRepository(repository);

        // Check cache if not forcing refresh and caching is enabled
        if (!forceRefresh && cacheTtlSeconds > 0) {
            MetadataCacheEntry entry = metadataCache.get(repository);
            if (entry != null && !entry.isExpired(cacheTtlSeconds)) {
                long age = Duration.between(entry.timestamp(), Instant.now()).getSeconds();
                logger.debug("Metadata cache HIT for repository: {} (age: {}s)", repository, age);
                return new LinkedList<>(entry.records()); // Return copy to prevent modification
            }
        }

        // Cache miss or expired - fetch from server
        logger.debug("Metadata cache MISS for repository: {} - fetching from server", repository);
        List<ComponentMetadata> records = new LinkedList<>();
        String encodedRepository = URLEncoder.encode(repository, StandardCharsets.UTF_8);
        String url = baseUrl + "/service/rest/v1/components?repository=" + encodedRepository;
        String continuationToken = null;

        do {
            String fetchUrl = continuationToken == null
                ? url
                : url + "&continuationToken=" + URLEncoder.encode(continuationToken, StandardCharsets.UTF_8);

            ComponentsMetadataResponse response = fetchComponentsWithMetadata(fetchUrl);
            records.addAll(response.records());
            continuationToken = response.continuationToken();
        } while (continuationToken != null);

        // Update cache if caching is enabled
        if (cacheTtlSeconds > 0) {
            metadataCache.put(repository, new MetadataCacheEntry(new LinkedList<>(records), Instant.now()));
        }

        return records;
    }

    /**
     * Fetches a single page of components from the given URL with retry logic.
     *
     * @param url the complete URL to fetch components from (including any continuation token)
     * @return a ComponentsResponse containing the records and optional continuation token
     * @throws IOException          if an HTTP error occurs or the response status is not 200
     * @throws InterruptedException if the operation is interrupted
     */
    private ComponentsResponse fetchComponents(String url) throws IOException, InterruptedException {
        logger.debug("Fetching: {}", url);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", authHeader)
            .header("Accept", "application/json")
            .header("Accept-Encoding", "gzip, deflate")  // Enable compression
            .GET()
            .build();

        IOException lastException = null;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            // Check for thread interruption at the start of each attempt
            if (Thread.interrupted()) {
                throw new InterruptedException("Retry loop interrupted");
            }

            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    throw new IOException("HTTP " + response.statusCode() + ": " + response.body());
                }

                return parseComponentsResponse(response.body());
            } catch (IOException e) {
                lastException = e;
                if (attempt < maxRetries && isRetryable(e)) {
                    long delay = initialRetryDelayMs * (1L << (attempt - 1));
                    logger.warn("Request failed (attempt {}/{}): {}. Retrying in {}ms...",
                        attempt, maxRetries, safeExceptionMessage(e), delay);
                    delayWithExponentialBackoff(delay);
                } else {
                    break;
                }
            }
        }

        throw lastException;
    }

    /**
     * Fetches a single page of components with metadata from the given URL with retry logic.
     *
     * @param url the complete URL to fetch components from (including any continuation token)
     * @return a ComponentsMetadataResponse containing the metadata records and optional continuation token
     * @throws IOException          if an HTTP error occurs or the response status is not 200
     * @throws InterruptedException if the operation is interrupted
     */
    private ComponentsMetadataResponse fetchComponentsWithMetadata(String url) throws IOException, InterruptedException {
        logger.debug("Fetching with metadata: {}", url);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", authHeader)
            .header("Accept", "application/json")
            .header("Accept-Encoding", "gzip, deflate")  // Enable compression
            .GET()
            .build();

        IOException lastException = null;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            // Check for thread interruption at the start of each attempt
            if (Thread.interrupted()) {
                throw new InterruptedException("Retry loop interrupted");
            }

            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    throw new IOException("HTTP " + response.statusCode() + ": " + response.body());
                }

                return parseComponentsResponseWithMetadata(response.body());
            } catch (IOException e) {
                lastException = e;
                if (attempt < maxRetries && isRetryable(e)) {
                    long delay = initialRetryDelayMs * (1L << (attempt - 1));
                    logger.warn("Request failed (attempt {}/{}): {}. Retrying in {}ms...",
                        attempt, maxRetries, safeExceptionMessage(e), delay);
                    delayWithExponentialBackoff(delay);
                } else {
                    break;
                }
            }
        }

        throw lastException;
    }

    /**
     * Safely gets an exception message, using class name as fallback if message is null.
     *
     * @param e the exception
     * @return the exception message or class name if message is null
     */
    private String safeExceptionMessage(Exception e) {
        String message = e.getMessage();
        return message != null ? message : e.getClass().getSimpleName();
    }

    /**
<<<<<<< HEAD
     * Implements non-blocking exponential backoff using ScheduledExecutorService.
     * <p>
     * This method schedules a delay on a background scheduler rather than blocking
     * the calling thread with Thread.sleep(). The delay doubles with each retry attempt:
     * initialDelay, 2×initialDelay, 4×initialDelay, etc.
     * </p>
     * <p>
     * <strong>Why non-blocking?</strong>
     * Blocking the calling thread with Thread.sleep() prevents GUI event dispatch threads
     * (Swing/AWT) from remaining responsive. By delegating the delay to a background scheduler
     * via CountDownLatch, the UI can continue processing events while waiting for the retry.
     * </p>
     * <p>
     * <strong>Implementation Details:</strong>
     * Uses a CountDownLatch to wait for the scheduled delay, which allows the delay to complete
     * on a background thread while the calling thread waits. This is interruptible, allowing
     * operations to be cancelled if needed.
     * </p>
     *
     * @param attempt the current retry attempt number (1-based), used to calculate exponential delay
     */
    private void sleepWithExponentialBackoff(int attempt) {
        long delay = initialRetryDelayMs * (1L << (attempt - 1)); // Exponential backoff: delay * 2^(attempt-1)
        delayWithExponentialBackoffNonBlocking(delay);
    }

    /**
     * Performs non-blocking delay using ScheduledExecutorService and CountDownLatch.
     * <p>
     * The delay is executed on the retryScheduler thread pool while the calling thread
     * waits on a CountDownLatch. This keeps the delay non-blocking from the perspective
     * of event loops while maintaining the blocking semantics needed for the retry loop.
     * </p>
     *
     * @param delayMs the delay in milliseconds
     */
    private void delayWithExponentialBackoffNonBlocking(long delayMs) {
        try {
            // Use CountDownLatch to coordinate the delay
            CountDownLatch latch = new CountDownLatch(1);
            // Schedule the latch countdown to happen after the delay
            retryScheduler.schedule(latch::countDown, delayMs, TimeUnit.MILLISECONDS);
            // Wait for the latch with timeout to prevent indefinite hang if scheduler shuts down
            // Add 1 second buffer to detect scheduler shutdown
            if (!latch.await(delayMs + 1000, TimeUnit.MILLISECONDS)) {
                logger.warn("Retry delay timeout - scheduler may be shutting down");
            }
        } catch (InterruptedException e) {
            // Restore interrupt status for the calling thread
            Thread.currentThread().interrupt();
            logger.debug("Retry delay interrupted");
=======
     * Implements exponential backoff delay using Thread.sleep().
     * <p>
     * This is a simple, standard approach appropriate for a CLI tool where blocking
     * is normal and expected. The delay can be interrupted via Thread.interrupt(),
     * allowing proper cancellation if needed.
     * </p>
     *
     * @param delayMs the delay in milliseconds to wait before the next retry attempt
     * @throws InterruptedException if the delay is interrupted
     */
    private void delayWithExponentialBackoff(long delayMs) throws InterruptedException {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
>>>>>>> e17d8af (chore: Remove .claude directory and add to .gitignore)
        }
    }

    /**
     * Determines if an exception is retryable.
     * <p>
     * Prioritizes exception type checking over message parsing for robustness.
     * Retryable conditions include:
     * - Network timeouts (SocketTimeoutException, HttpTimeoutException)
     * - Connection failures (ConnectException, NoRouteToHostException, UnknownHostException)
     * - HTTP 5xx server errors (detected from exception message)
     * - Temporary network issues (PortUnreachableException)
     * </p>
     *
     * @param e the exception to check
     * @return true if the exception is retryable, false otherwise
     */
    private boolean isRetryable(IOException e) {
        // 1. Check exception type first (most reliable)
        if (e instanceof java.net.SocketTimeoutException ||
            e instanceof java.net.http.HttpTimeoutException ||
            e instanceof java.net.ConnectException ||
            e instanceof java.net.NoRouteToHostException ||
            e instanceof java.net.UnknownHostException ||
            e instanceof java.net.PortUnreachableException) {
            return true;
        }

        // 2. Check HTTP status code from exception message
        String message = e.getMessage();
        if (message != null) {
            String lowerMessage = message.toLowerCase();
            // Check for HTTP 5xx status codes (server errors - typically transient)
            // Check for HTTP 408 (Request Timeout)
            // Check for HTTP 429 (Too Many Requests - rate limiting)
            // Check for HTTP 503 (Service Unavailable)
            // Check for HTTP 504 (Gateway Timeout)
            if (lowerMessage.matches(".*http\\s+(408|429|5\\d{2}).*")) {
                return true;
            }
            // Also check for explicit timeout/connection keywords as fallback
            if (lowerMessage.contains("timeout") ||
                lowerMessage.contains("timed out") ||
                lowerMessage.contains("connection reset") ||
                lowerMessage.contains("connection refused") ||
                lowerMessage.contains("too many requests") ||
                lowerMessage.contains("rate limit")) {
                return true;
            }
        }

        // 3. Not retryable
        return false;
    }

    /**
     * Parses a JSON response from the Nexus API into component records.
     * <p>
     * Extracts component information including ID, file size, and path from the
     * first asset of each component. Components with no assets are skipped.
     * </p>
     *
     * @param jsonResponse the JSON string response from Nexus API
     * @return a ComponentsResponse containing parsed records and continuation token (if any)
     * @throws IOException if JSON parsing fails
     */
    private ComponentsResponse parseComponentsResponse(String jsonResponse) throws IOException {
        Map<String, Object> map = objectMapper.readValue(jsonResponse, new TypeReference<>() {});
        JSONObject json = new JSONObject(map);

        List<RepoRecord> records = new LinkedList<>();
        JSONArray items = json.getJSONArray("items");

        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            String id = item.getString("id");
            JSONArray assets = item.getJSONArray("assets");

            if (assets.length() > 0) {
                // Note: Only processes the first asset per component
                // Components can have multiple assets, but for simplicity we use the first
                JSONObject asset = assets.getJSONObject(0);
                long fileSize = asset.getLong("fileSize");
                String path = asset.getString("path");
                records.add(new RepoRecord(id, fileSize, path));
            }
        }

        String continuationToken = json.isNull("continuationToken")
            ? null
            : json.getString("continuationToken");

        return new ComponentsResponse(records, continuationToken);
    }

    /**
     * Parses a JSON response from the Nexus API into component metadata records.
     * <p>
     * Extracts full component metadata including ID, file size, path, content type,
     * format, creation date, last modified date, and checksum from the first asset
     * of each component. Components with no assets are skipped.
     * </p>
     * <p>
     * Handles missing/null fields gracefully by using default values:
     * </p>
     * <ul>
     *   <li>contentType: defaults to "unknown"</li>
     *   <li>format: defaults to "unknown"</li>
     *   <li>createdDate: null if not present</li>
     *   <li>lastModified: null if not present</li>
     *   <li>checksum: null if not present</li>
     * </ul>
     *
     * @param jsonResponse the JSON string response from Nexus API
     * @return a ComponentsMetadataResponse containing parsed metadata records and continuation token (if any)
     * @throws IOException if JSON parsing fails
     */
    private ComponentsMetadataResponse parseComponentsResponseWithMetadata(String jsonResponse) throws IOException {
        Map<String, Object> map = objectMapper.readValue(jsonResponse, new TypeReference<>() {});
        JSONObject json = new JSONObject(map);

        List<ComponentMetadata> records = new LinkedList<>();
        JSONArray items = json.getJSONArray("items");

        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            String id = item.getString("id");

            // Extract format from component level (e.g., "maven2", "npm", "docker")
            String format = item.optString("format", "unknown");

            JSONArray assets = item.getJSONArray("assets");

            if (assets.length() > 0) {
                // Note: Only processes the first asset per component
                // Components can have multiple assets, but for simplicity we use the first
                JSONObject asset = assets.getJSONObject(0);

                // Required fields
                long fileSize = asset.getLong("fileSize");
                String path = asset.getString("path");

                // Optional fields with defaults
                String contentType = asset.optString("contentType", "unknown");

                // Parse timestamps (ISO 8601 format)
                Instant createdDate = null;
                if (asset.has("blobCreated") && !asset.isNull("blobCreated")) {
                    try {
                        createdDate = Instant.parse(asset.getString("blobCreated"));
                    } catch (java.time.format.DateTimeParseException e) {
                        logger.warn("Failed to parse blobCreated date: {} - {}",
                            asset.getString("blobCreated"), e.getMessage());
                    }
                }

                Instant lastModified = null;
                if (asset.has("lastModified") && !asset.isNull("lastModified")) {
                    try {
                        lastModified = Instant.parse(asset.getString("lastModified"));
                    } catch (java.time.format.DateTimeParseException e) {
                        logger.warn("Failed to parse lastModified date: {} - {}",
                            asset.getString("lastModified"), e.getMessage());
                    }
                }

                // Extract checksum (prefer SHA1, fallback to MD5)
                String checksum = null;
                if (asset.has("checksum") && !asset.isNull("checksum")) {
                    JSONObject checksumObj = asset.getJSONObject("checksum");
                    if (checksumObj.has("sha1")) {
                        checksum = checksumObj.getString("sha1");
                    } else if (checksumObj.has("md5")) {
                        checksum = checksumObj.getString("md5");
                    }
                }

                records.add(new ComponentMetadata(
                    id,
                    path,
                    fileSize,
                    contentType,
                    format,
                    createdDate,
                    lastModified,
                    checksum
                ));
            }
        }

        String continuationToken = json.isNull("continuationToken")
            ? null
            : json.getString("continuationToken");

        return new ComponentsMetadataResponse(records, continuationToken);
    }

    /**
     * Deletes a component from the Nexus repository with retry logic.
     *
     * @param componentId the unique identifier of the component to delete
     * @throws IOException          if an HTTP error occurs (status code not 200 or 204)
     * @throws InterruptedException if the operation is interrupted
     */
    @Override
    public void deleteComponent(String componentId) throws IOException, InterruptedException {
        String url = baseUrl + "/service/rest/v1/components/" + componentId;

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", authHeader)
            .DELETE()
            .build();

        IOException lastException = null;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            // Check for thread interruption at the start of each attempt
            if (Thread.interrupted()) {
                throw new InterruptedException("Retry loop interrupted");
            }

            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 204 && response.statusCode() != 200) {
                    throw new IOException("Failed to delete component " + componentId + ": HTTP " + response.statusCode());
                }

                return; // Success
            } catch (IOException e) {
                lastException = e;
                if (attempt < maxRetries && isRetryable(e)) {
                    long delay = initialRetryDelayMs * (1L << (attempt - 1)); // Exponential backoff
                    logger.warn("Delete failed for {} (attempt {}/{}): {}. Retrying in {}ms...",
                        componentId, attempt, maxRetries, safeExceptionMessage(e), delay);
<<<<<<< HEAD
                    sleepWithExponentialBackoff(attempt);
=======
                    delayWithExponentialBackoff(delay);
>>>>>>> e17d8af (chore: Remove .claude directory and add to .gitignore)
                } else {
                    break;
                }
            }
        }

        throw lastException;
    }

    /**
     * Clears the cache for a specific repository.
     *
     * @param repository the name of the repository to clear from cache
     */
    @Override
    public void clearCache(String repository) {
        // Validate repository name
        Credentials.validateRepository(repository);

        cache.remove(repository);
        metadataCache.remove(repository);
        logger.debug("Cache cleared for repository: {}", repository);
    }

    /**
     * Clears all cached repository data.
     */
    @Override
    public void clearAllCache() {
        int size = cache.size() + metadataCache.size();
        cache.clear();
        metadataCache.clear();
        logger.debug("Cleared cache for {} repositories", size);
    }

    /**
     * Checks if cache contains valid (non-expired) data for a repository.
     *
     * @param repository the name of the repository to check
     * @return true if cache contains valid data, false otherwise
     */
    @Override
    public boolean isCached(String repository) {
        // Validate repository name
        Credentials.validateRepository(repository);

        if (cacheTtlSeconds == 0) return false;
        CacheEntry entry = cache.get(repository);
        return entry != null && !entry.isExpired(cacheTtlSeconds);
    }

    /**
     * Gets the cache age for a repository in seconds.
     *
     * @param repository the name of the repository to check
     * @return cache age in seconds, or -1 if not cached
     */
    @Override
    public long getCacheAge(String repository) {
        // Validate repository name
        Credentials.validateRepository(repository);

        CacheEntry entry = cache.get(repository);
        if (entry == null) return -1;
        return Duration.between(entry.timestamp(), Instant.now()).getSeconds();
    }


    /**
     * Gets the retry scheduler for advanced use cases requiring non-blocking retry logic.
     * <p>
     * <strong>Advanced API - Use with caution.</strong>
     * </p>
     * <p>
     * This method is intended for GUI frameworks that need non-blocking retry delays.
     * The scheduler is a single-threaded ScheduledExecutorService suitable for scheduling
     * retry callbacks without blocking the calling thread.
     * </p>
     * <p>
     * Example usage in Swing:
     * </p>
     * <pre>
     * ScheduledExecutorService scheduler = client.getRetryScheduler();
     * scheduler.schedule(
     *     () -> SwingUtilities.invokeLater(() -> {
     *         // Retry logic here - will run on EDT
     *     }),
     *     1000,  // delay in ms
     *     TimeUnit.MILLISECONDS
     * );
     * </pre>
     *
     * @return the ScheduledExecutorService used for retry delays
     * @since 1.0
     */
    public ScheduledExecutorService getRetryScheduler() {
        return retryScheduler;
    }

    /**
     * Closes this client and releases any resources held by it.
     * <p>
<<<<<<< HEAD
     * This method clears all caches and shuts down the retry scheduler.
     * The underlying {@link HttpClient} doesn't have an explicit close() method in Java 21,
     * but clearing our references helps with resource cleanup in long-running applications.
=======
     * This method clears all caches. The underlying {@link HttpClient} doesn't have an explicit
     * close() method in Java 21, but clearing our references helps with resource cleanup in
     * long-running applications.
>>>>>>> e17d8af (chore: Remove .claude directory and add to .gitignore)
     * </p>
     * <p>
     * This client should be used with try-with-resources for proper lifecycle management:
     * </p>
     * <pre>
     * try (NexusClient client = new NexusClient(credentials)) {
     *     NexusService service = new NexusService(client);
     *     service.listRepository("maven-releases", null);
     * }
     * </pre>
     * <p>
     * <strong>Note:</strong> After calling close(), this client should not be used for further operations.
     * Any pending retry delays scheduled via {@link #getRetryScheduler()} will be cancelled.
     * </p>
     */
    @Override
    public void close() {
        // Shut down the HTTP executor to release thread resources
        httpExecutor.shutdownNow();

        // Clear caches to free memory
        cache.clear();
        metadataCache.clear();

<<<<<<< HEAD
        // Shutdown the retry scheduler gracefully
        try {
            retryScheduler.shutdown();
            if (!retryScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                retryScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            retryScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        logger.debug("NexusClient closed - caches cleared, scheduler shutdown, and resources will be freed by GC");
=======
        logger.debug("NexusClient closed - executor shut down, caches cleared");
>>>>>>> e17d8af (chore: Remove .claude directory and add to .gitignore)
    }

    /**
     * Internal record for holding a page of component records and an optional continuation token.
     *
     * @param records           the list of component records on this page
     * @param continuationToken optional token for fetching the next page, or null if this is the last page
     */
    private record ComponentsResponse(List<RepoRecord> records, String continuationToken) {}

    private record ComponentsMetadataResponse(List<ComponentMetadata> records, String continuationToken) {}
}

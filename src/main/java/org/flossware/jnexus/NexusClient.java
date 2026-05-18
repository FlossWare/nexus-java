package org.flossware.jnexus;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HTTP client for interacting with the Nexus Repository Manager REST API.
 * <p>
 * This class provides methods to list and delete components from Nexus repositories.
 * It uses Java's built-in {@link HttpClient} for HTTP communication and handles
 * authentication using HTTP Basic Auth.
 * </p>
 * <p>
 * The client automatically handles pagination when listing components, following
 * continuation tokens provided by the Nexus API.
 * </p>
 *
 * @author sfloess
 * @since 1.0
 */
public class NexusClient {
    private static final Logger logger = LoggerFactory.getLogger(NexusClient.class);

    private final String baseUrl;
    private final HttpClient httpClient;
    private final String authHeader;
    private final ObjectMapper objectMapper;

    // Cache configuration
    private static final long DEFAULT_CACHE_TTL_SECONDS = 300; // 5 minutes
    private final long cacheTtlSeconds;

    // Retry configuration
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_RETRY_DELAY_MS = 1000; // 1 second

    // Cache storage: repository -> CacheEntry
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    /**
     * Cache entry holding component list and timestamp.
     */
    private record CacheEntry(List<RepoRecord> records, Instant timestamp) {
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

        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(credentials.getHttpTimeoutSeconds()))
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
    public List<RepoRecord> listComponents(String repository, boolean forceRefresh) throws IOException, InterruptedException {
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
        String url = baseUrl + "/service/rest/v1/components?repository=" + repository;
        String continuationToken = null;

        do {
            String fetchUrl = continuationToken == null
                ? url
                : url + "&continuationToken=" + continuationToken;

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
            .GET()
            .build();

        IOException lastException = null;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    throw new IOException("HTTP " + response.statusCode() + ": " + response.body());
                }

                return parseComponentsResponse(response.body());
            } catch (IOException e) {
                lastException = e;
                if (attempt < MAX_RETRIES && isRetryable(e)) {
                    long delay = INITIAL_RETRY_DELAY_MS * (1L << (attempt - 1)); // Exponential backoff
                    logger.warn("Request failed (attempt {}/{}): {}. Retrying in {}ms...",
                        attempt, MAX_RETRIES, e.getMessage(), delay);
                    Thread.sleep(delay);
                } else {
                    break;
                }
            }
        }

        throw lastException;
    }

    /**
     * Determines if an exception is retryable.
     *
     * @param e the exception to check
     * @return true if the exception is retryable, false otherwise
     */
    private boolean isRetryable(IOException e) {
        String message = e.getMessage().toLowerCase();
        // Retry on connection errors, timeouts, and 5xx server errors
        return message.contains("timeout") ||
               message.contains("connection") ||
               message.contains("http 5");
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
     * Deletes a component from the Nexus repository with retry logic.
     *
     * @param componentId the unique identifier of the component to delete
     * @throws IOException          if an HTTP error occurs (status code not 200 or 204)
     * @throws InterruptedException if the operation is interrupted
     */
    public void deleteComponent(String componentId) throws IOException, InterruptedException {
        String url = baseUrl + "/service/rest/v1/components/" + componentId;

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", authHeader)
            .DELETE()
            .build();

        IOException lastException = null;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 204 && response.statusCode() != 200) {
                    throw new IOException("Failed to delete component " + componentId + ": HTTP " + response.statusCode());
                }

                return; // Success
            } catch (IOException e) {
                lastException = e;
                if (attempt < MAX_RETRIES && isRetryable(e)) {
                    long delay = INITIAL_RETRY_DELAY_MS * (1L << (attempt - 1)); // Exponential backoff
                    logger.warn("Delete failed for {} (attempt {}/{}): {}. Retrying in {}ms...",
                        componentId, attempt, MAX_RETRIES, e.getMessage(), delay);
                    Thread.sleep(delay);
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
    public void clearCache(String repository) {
        cache.remove(repository);
        logger.debug("Cache cleared for repository: {}", repository);
    }

    /**
     * Clears all cached repository data.
     */
    public void clearAllCache() {
        int size = cache.size();
        cache.clear();
        logger.debug("Cleared cache for {} repositories", size);
    }

    /**
     * Checks if cache contains valid (non-expired) data for a repository.
     *
     * @param repository the name of the repository to check
     * @return true if cache contains valid data, false otherwise
     */
    public boolean isCached(String repository) {
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
    public long getCacheAge(String repository) {
        CacheEntry entry = cache.get(repository);
        if (entry == null) return -1;
        return Duration.between(entry.timestamp(), Instant.now()).getSeconds();
    }

    /**
     * Internal record for holding a page of component records and an optional continuation token.
     *
     * @param records           the list of component records on this page
     * @param continuationToken optional token for fetching the next page, or null if this is the last page
     */
    private record ComponentsResponse(List<RepoRecord> records, String continuationToken) {}
}

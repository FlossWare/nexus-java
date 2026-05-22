package org.flossware.jnexus.android;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.flossware.jnexus.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Android HTTP client implementation using OkHttp.
 * <p>
 * Replaces java.net.http.HttpClient with OkHttp for Android compatibility.
 * Provides the same functionality as NexusClient with identical caching,
 * retry logic, and pagination handling.
 * </p>
 *
 * @author sfloess
 * @since 1.1
 */
public class NexusClientOkHttp implements NexusHttpClient {
    private static final Logger logger = LoggerFactory.getLogger(NexusClientOkHttp.class);

    private final String baseUrl;
    private final OkHttpClient httpClient;
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
     * Constructs a new NexusClientOkHttp with the provided credentials.
     * <p>
     * Initializes the OkHttp client with configured timeout,
     * prepares the Basic Authentication header, and uses default cache TTL of 5 minutes.
     * </p>
     *
     * @param credentials the Nexus credentials containing URL, username, and password
     */
    public NexusClientOkHttp(org.flossware.jnexus.Credentials credentials) {
        this(credentials, DEFAULT_CACHE_TTL_SECONDS);
    }

    /**
     * Constructs a new NexusClientOkHttp with the provided credentials and custom cache TTL.
     * <p>
     * Initializes the OkHttp client with the configured connection timeout (default 30 seconds)
     * and prepares the Basic Authentication header.
     * </p>
     *
     * @param credentials the Nexus credentials containing URL, username, and password
     * @param cacheTtlSeconds cache time-to-live in seconds (0 to disable caching)
     */
    public NexusClientOkHttp(Credentials credentials, long cacheTtlSeconds) {
        this.baseUrl = credentials.getUrl();
        this.objectMapper = new ObjectMapper();
        this.cacheTtlSeconds = cacheTtlSeconds;

        String auth = credentials.getUser() + ":" + credentials.getPassword();
        this.authHeader = "Basic " + Base64.getEncoder().encodeToString(auth.getBytes());

        int timeoutSeconds = credentials.getHttpTimeoutSeconds();
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .addInterceptor(new RetryInterceptor(MAX_RETRIES, INITIAL_RETRY_DELAY_MS))
            .build();
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
    public List<RepoRecord> listComponents(String repository, boolean forceRefresh)
            throws IOException, InterruptedException {
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
    public List<ComponentMetadata> listComponentsWithMetadata(String repository, boolean forceRefresh)
            throws IOException, InterruptedException {
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
        String url = baseUrl + "/service/rest/v1/components?repository=" + repository;
        String continuationToken = null;

        do {
            String fetchUrl = continuationToken == null
                ? url
                : url + "&continuationToken=" + continuationToken;

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

        Request request = new Request.Builder()
            .url(url)
            .header("Authorization", authHeader)
            .header("Accept", "application/json")
            .get()
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String body = response.body() != null ? response.body().string() : "";
                throw new IOException("HTTP " + response.code() + ": " + body);
            }

            if (response.body() == null) {
                throw new IOException("Response body is null");
            }

            return parseComponentsResponse(response.body().string());
        }
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

        Request request = new Request.Builder()
            .url(url)
            .header("Authorization", authHeader)
            .header("Accept", "application/json")
            .get()
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String body = response.body() != null ? response.body().string() : "";
                throw new IOException("HTTP " + response.code() + ": " + body);
            }

            if (response.body() == null) {
                throw new IOException("Response body is null");
            }

            return parseComponentsResponseWithMetadata(response.body().string());
        }
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
                    } catch (Exception e) {
                        logger.warn("Failed to parse blobCreated date: {}", asset.getString("blobCreated"));
                    }
                }

                Instant lastModified = null;
                if (asset.has("lastModified") && !asset.isNull("lastModified")) {
                    try {
                        lastModified = Instant.parse(asset.getString("lastModified"));
                    } catch (Exception e) {
                        logger.warn("Failed to parse lastModified date: {}", asset.getString("lastModified"));
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
     * @throws IOException if an HTTP error occurs (status code not 200 or 204)
     */
    @Override
    public void deleteComponent(String componentId) throws IOException {
        String url = baseUrl + "/service/rest/v1/components/" + componentId;

        Request request = new Request.Builder()
            .url(url)
            .header("Authorization", authHeader)
            .delete()
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.code() != 204 && response.code() != 200) {
                String body = response.body() != null ? response.body().string() : "";
                throw new IOException("Failed to delete component " + componentId + ": HTTP " + response.code() + ": " + body);
            }
        }
    }

    /**
     * Clears the cache for a specific repository.
     *
     * @param repository the name of the repository to clear from cache
     */
    @Override
    public void clearCache(String repository) {
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
     * OkHttp retry interceptor for exponential backoff.
     */
    private static class RetryInterceptor implements Interceptor {
        private final int maxRetries;
        private final long initialDelayMs;

        RetryInterceptor(int maxRetries, long initialDelayMs) {
            this.maxRetries = maxRetries;
            this.initialDelayMs = initialDelayMs;
        }

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            Response response = null;
            IOException lastException = null;

            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                try {
                    response = chain.proceed(request);

                    // Success or non-retryable error
                    if (response.isSuccessful() || !isRetryable(response.code())) {
                        return response;
                    }

                    // Close unsuccessful response before retrying
                    response.close();

                } catch (IOException e) {
                    lastException = e;
                    if (attempt == maxRetries || !isRetryableException(e)) {
                        throw e;
                    }
                }

                // Exponential backoff
                if (attempt < maxRetries) {
                    long delay = initialDelayMs * (1L << (attempt - 1));
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Retry interrupted", e);
                    }
                }
            }

            if (lastException != null) {
                throw lastException;
            }

            throw new IOException("Max retries exceeded");
        }

        private boolean isRetryable(int code) {
            // Retry on server errors (5xx), request timeout (408), or too many requests (429)
            return code >= 500 || code == 408 || code == 429;
        }

        private boolean isRetryableException(IOException e) {
            String message = e.getMessage();
            if (message == null) return false;
            String lowerMessage = message.toLowerCase();
            // Retry on connection errors, timeouts, and server errors
            return lowerMessage.contains("timeout") ||
                   lowerMessage.contains("connection") ||
                   lowerMessage.contains("http 5");
        }
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

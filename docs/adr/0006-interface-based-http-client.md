# 6. Interface-Based HTTP Client Abstraction

**Status:** Accepted (Android), Partially Implemented (Desktop)

**Date:** 2026-05-22

**Deciders:** Scot P. Floess, Development Team

## Context

JNexus communicates with Nexus Repository Manager via REST API over HTTP. Different platforms have different HTTP client requirements and constraints:

**Desktop (Java 21):**
- `java.net.http.HttpClient` (built into JDK 11+, modern HTTP/2 support)
- No external dependencies needed
- Excellent performance and features

**Android:**
- `java.net.http.HttpClient` not available on Android
- `HttpURLConnection` (legacy, poor API, limited features)
- OkHttp (de facto standard, excellent API, HTTP/2, connection pooling)

**iOS/macOS:**
- URLSession (native Swift HTTP client)
- Completely different language and API

Without abstraction, each platform would have tightly coupled HTTP code, making it difficult to:
- Share business logic (filtering, pagination, statistics)
- Test HTTP code in isolation
- Switch HTTP implementations
- Support new platforms

## Decision

Create `NexusHttpClient` interface to abstract HTTP operations:

### Core Interface (Java)

**File:** `jnexus-core/src/main/java/org/flossware/jnexus/NexusHttpClient.java`

```java
public interface NexusHttpClient {
    List<RepoRecord> listComponents(String repository, boolean forceRefresh) 
        throws IOException, InterruptedException;
    
    List<ComponentMetadata> listComponentsWithMetadata(String repository, boolean forceRefresh)
        throws IOException, InterruptedException;
    
    void deleteComponent(String componentId) 
        throws IOException, InterruptedException;
    
    void clearCache(String repository);
    void clearAllCache();
    boolean isCached(String repository);
    Long getCacheAge(String repository);
}
```

### Platform Implementations

**Desktop:** `NexusClient` (java.net.http.HttpClient)
- Location: `src/main/java/org/flossware/jnexus/NexusClient.java`
- Status: ❌ Does NOT formally implement interface (duck typing)
- HTTP client: `java.net.http.HttpClient`

**Android:** `NexusClientOkHttp` (OkHttp)
- Location: `jnexus-android/src/main/java/org/flossware/jnexus/NexusClientOkHttp.java`
- Status: ✅ Formally implements interface
- HTTP client: `com.squareup.okhttp3.OkHttpClient`

**iOS/macOS:** `NexusClientURLSession` (Swift protocol)
- Location: `jnexus-ios/Shared/Platform/NexusClientURLSession.swift`
- Status: ✅ Implements Swift protocol (mirrors Java interface)
- HTTP client: `URLSession` (Foundation)

## Consequences

### Positive

- **Platform independence**: Business logic doesn't know about HTTP implementation
- **Testability**: Can mock NexusHttpClient for unit tests
- **Flexibility**: Easy to swap HTTP clients (e.g., java.net.http → OkHttp)
- **Shared business logic**: NexusService works with any implementation
- **Clear contracts**: Interface documents required operations
- **Type safety**: Compiler enforces interface compliance

### Negative

- **Indirection**: One more layer between business logic and HTTP
- **Implementation burden**: Each platform must implement full interface
- **Feature parity**: All implementations must support same operations
- **Exception handling**: Interface defines throws IOException, implementations must comply

### Accepted Tradeoffs

The indirection is minimal and justified because:
1. Business logic is significantly more complex than HTTP calls
2. Platform differences (java.net.http vs. OkHttp) are substantial
3. Testing benefits are significant
4. Future platform support is easier

## Current Status: Partially Implemented

**What works:**
- Interface defined in jnexus-core ✅
- Android implements interface correctly ✅
- iOS/macOS Swift protocol mirrors interface ✅
- NexusService uses interface for all HTTP operations ✅

**What doesn't work:**
- Desktop NexusClient does NOT formally implement interface ❌
- Desktop uses duck typing (same method signatures) instead ❌
- This creates inconsistency and technical debt ❌

**From CLAUDE.md:**
> Desktop NexusClient unchanged (implements NexusHttpClient implicitly via same method signatures)

**This was a conscious decision** during Android development to:
- Avoid breaking existing desktop code
- Minimize risk to stable desktop implementation
- Ship Android faster without desktop refactoring

**But it created technical debt:** See Issue #17 (Desktop doesn't use jnexus-core)

## Implementation Details

### Desktop (java.net.http)

**HTTP client creation:**
```java
HttpClient httpClient = HttpClient.newBuilder()
    .connectTimeout(Duration.ofSeconds(timeoutSeconds))
    .build();
```

**Request execution:**
```java
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create(url))
    .header("Authorization", authHeader)
    .GET()
    .build();

HttpResponse<String> response = httpClient.send(request, 
    HttpResponse.BodyHandlers.ofString());
```

**Features:**
- Synchronous API (send)
- Automatic HTTP/2 support
- Built-in timeout handling
- No external dependencies

### Android (OkHttp)

**HTTP client creation:**
```java
OkHttpClient httpClient = new OkHttpClient.Builder()
    .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
    .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
    .addInterceptor(new RetryInterceptor(maxRetries, initialDelayMs))
    .build();
```

**Request execution:**
```java
Request request = new Request.Builder()
    .url(url)
    .header("Authorization", authHeader)
    .get()
    .build();

Response response = httpClient.newCall(request).execute();
String body = response.body().string();
```

**Features:**
- Synchronous API (execute)
- Interceptor pattern for retry logic
- Connection pooling
- Automatic cookie handling
- Excellent Android integration

### iOS/macOS (URLSession)

**HTTP client creation:**
```swift
let config = URLSessionConfiguration.default
config.timeoutIntervalForRequest = TimeInterval(httpTimeoutSeconds)
config.timeoutIntervalForResource = TimeInterval(httpTimeoutSeconds)
let session = URLSession(configuration: config)
```

**Request execution:**
```swift
var request = URLRequest(url: url)
request.setValue("Basic \(base64Auth)", forHTTPHeaderField: "Authorization")

let (data, response) = try await session.data(for: request)
let json = try JSONDecoder().decode(ComponentsResponse.self, from: data)
```

**Features:**
- Async/await API (modern Swift concurrency)
- Native Apple framework
- Hardware-accelerated SSL/TLS
- Automatic certificate validation

## Common Patterns Across Implementations

All implementations share:

1. **Caching:**
   - 5-minute TTL (300 seconds)
   - Thread-safe cache (ConcurrentHashMap or Swift actor)
   - Per-repository cache keys
   - Force refresh option

2. **Retry logic:**
   - 3 maximum retries
   - Exponential backoff (1s, 2s, 4s)
   - Retry on: timeouts, connection errors, 5xx HTTP errors
   - No retry on: 4xx client errors (except 408, 429)

3. **Pagination:**
   - Follow Nexus continuation tokens
   - Accumulate results across pages
   - Transparent to caller

4. **Authentication:**
   - HTTP Basic Auth
   - Base64-encoded username:password
   - Authorization header on all requests

5. **JSON parsing:**
   - Jackson (desktop, Android)
   - Codable (iOS/macOS)
   - Error handling for malformed JSON

## Testing Strategy

### Mock-based testing (NexusService)
```java
@Mock
private NexusHttpClient mockClient;

@Test
void testSearchComponents() {
    when(mockClient.listComponentsWithMetadata("repo", false))
        .thenReturn(testComponents);
    
    SearchCriteria criteria = new SearchCriteria.Builder()
        .repository("repo")
        .minSize(1000L)
        .build();
    
    List<ComponentMetadata> results = service.searchComponents(criteria, false);
    // Assert filtered results
}
```

### HTTP-level testing (each implementation)

**Desktop:** MockWebServer (OkHttp library, works with java.net.http)
```java
MockWebServer server = new MockWebServer();
server.enqueue(new MockResponse().setBody("{...}"));
// Test NexusClient against mock server
```

**Android:** MockWebServer (OkHttp native mock)
```java
MockWebServer server = new MockWebServer();
server.enqueue(new MockResponse().setBody("{...}"));
// Test NexusClientOkHttp against mock server
```

**iOS/macOS:** URLProtocol mocking
```swift
class MockURLProtocol: URLProtocol {
    static var mockResponse: HTTPURLResponse?
    static var mockData: Data?
    // Override loading methods
}
```

## Alternatives Considered

### Alternative 1: Platform-specific NexusService implementations
- **Approach**: Duplicate NexusService for each platform (NexusServiceDesktop, NexusServiceAndroid)
- **Rejected because**:
  - Massive code duplication (591 lines × 3 platforms)
  - Bug fixes must be applied multiple times
  - Filtering and statistics logic is platform-agnostic
  - Testing overhead (test same logic 3× times)

### Alternative 2: Abstract HTTP calls in NexusService
- **Approach**: NexusService contains HTTP code with platform detection
- **Rejected because**:
  - Tight coupling between business logic and HTTP
  - Can't compile on Android (java.net.http doesn't exist)
  - Complex conditional logic (#if Android, #if iOS)
  - Poor testability (can't mock HTTP easily)

### Alternative 3: Use Apache HttpClient everywhere
- **Approach**: Use Apache HttpClient 5.x on all Java platforms
- **Rejected because**:
  - Desktop doesn't need external dependency (java.net.http works great)
  - Android developers prefer OkHttp (better API, better Android integration)
  - Adds unnecessary dependency weight
  - java.net.http is more modern (HTTP/2, reactive)

### Alternative 4: Use OkHttp everywhere
- **Approach**: Use OkHttp on desktop and Android
- **Rejected because**:
  - Desktop doesn't benefit from OkHttp features
  - Adds external dependency where not needed
  - java.net.http is built-in, faster startup
  - OkHttp is overkill for desktop use case

## Path Forward

**Recommendation:** Desktop should formally implement the interface.

**Required changes:**
```java
// Current
public class NexusClient {
    public List<RepoRecord> listComponents(String repository, boolean forceRefresh) { ... }
}

// Proposed
public class NexusClient implements NexusHttpClient {
    @Override
    public List<RepoRecord> listComponents(String repository, boolean forceRefresh) { ... }
}
```

**Benefits:**
- Type safety (compiler enforces interface compliance)
- Consistency across platforms
- Enables true polymorphism (NexusHttpClient client = new NexusClient())
- Resolves Issue #17 (desktop doesn't use jnexus-core)

**Challenges:**
- Desktop Credentials must also implement Credentials interface (783 lines → interface)
- Desktop must depend on jnexus-core in pom.xml
- All UI code already works (same method signatures), minimal changes needed

## References

- ADR-0002: Multi-module architecture (why interface exists)
- Issue #17: Desktop doesn't use jnexus-core
- NexusHttpClient.java: Interface definition
- NexusClient.java: Desktop implementation (implicit)
- NexusClientOkHttp.java: Android implementation (explicit)
- NexusClientURLSession.swift: iOS/macOS implementation

## Impact

**Positive:**
- Android successfully uses interface pattern ✅
- Business logic (NexusService) is platform-agnostic ✅
- Easy to test with mocks ✅
- Clear separation of concerns ✅

**Negative:**
- Desktop doesn't use interface (technical debt) ❌
- Inconsistency between desktop and Android ❌
- Can't use polymorphism on desktop ❌

## Related Decisions

- ADR-0002: Multi-module architecture
- ADR-0004: Java version strategy (why core is Java 11)
- Issue #17: Desktop technical debt

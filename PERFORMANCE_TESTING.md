# Performance Testing Guide

This document describes how to run, interpret, and contribute to the performance test suite for JNexus.

## Overview

The performance testing infrastructure includes:

1. **JMH Benchmarks** (`NexusClientBenchmark.java`)
   - Micro-benchmarks for core operations
   - Precise timing measurements
   - JVM warmup and stability
   - Framework: OpenJDK JMH 1.37

2. **Load Tests** (`NexusClientLoadTest.java`)
   - Concurrent operation testing
   - Memory usage validation
   - Throughput measurement
   - Timeout/error handling
   - Framework: JUnit 5 + custom metrics

3. **CI/CD Integration** (`.github/workflows/performance.yml`)
   - Automated benchmark execution on every push
   - Performance regression detection
   - Result artifacts and historical tracking
   - PR comments with performance impact

## Running Benchmarks Locally

### Prerequisites

```bash
# Ensure Maven 3.9+ installed
mvn --version

# Ensure JDK 21+ installed
java -version

# Install dependencies (if not cached)
mvn clean install -DskipTests -B
```

### Run All Benchmarks

```bash
# Run complete benchmark suite
mvn test -Pbenchmark -B

# With detailed output
mvn test -Pbenchmark -B -X

# Save results to file
mvn test -Pbenchmark -B 2>&1 | tee benchmark-results.txt
```

### Run Specific Benchmark

```bash
# Run single benchmark
mvn test -Pbenchmark -Dtest=NexusClientBenchmark#listComponents_1000_cached -B

# Run multiple benchmarks by pattern
mvn test -Pbenchmark -Dtest=NexusClientBenchmark#listComponents* -B

# Run benchmarks with regex filter
mvn test -Pbenchmark -Dtest=NexusClientBenchmark#cache* -B
```

### Interpret Benchmark Output

Example benchmark output:

```
Iteration   1: 95.432 ms/op
Iteration   2: 92.128 ms/op
Iteration   3: 94.205 ms/op
Iteration   4: 93.847 ms/op
Iteration   5: 91.356 ms/op

Average: 93.394 ms/op ± 1.445 ms/op
```

Breaking this down:
- **Iteration**: Individual measurement (5 iterations x 1 second each)
- **ms/op**: Milliseconds per operation (throughput mode shows ops/sec)
- **±**: Confidence interval (95%)

SLA check for `listComponents_1000_cached`:
```
Target: p50 < 100ms ✓ (Actual: 93.394ms)
```

## Running Load Tests

### Prerequisites

Same as benchmarks - no additional setup required.

### Run All Load Tests

```bash
# Run complete load test suite
mvn test -Dtest=NexusClientLoadTest -B

# With failure details
mvn test -Dtest=NexusClientLoadTest -B -X

# Generate test report
mvn surefire-report:report
```

### Run Specific Load Test

```bash
# Test concurrent operations
mvn test -Dtest=NexusClientLoadTest#testConcurrent100ListOperations -B

# Test memory usage
mvn test -Dtest=NexusClientLoadTest#testMemoryUsageWith10000Components -B

# Test throughput
mvn test -Dtest=NexusClientLoadTest#testThroughputWithCaching -B
```

### Interpret Load Test Output

Example load test output:

```
NexusClientLoadTest > testConcurrent100ListOperations PASSED (3245ms)
  Total Operations: 100
  Successful: 100 (100%)
  Failed: 0 (0%)
  Duration: 3.245 seconds
  Throughput: 30.8 ops/sec
```

Key metrics:
- **Duration**: Total time for all operations (including thread scheduling)
- **Successful**: Operations that completed without exception
- **Failed**: Operations that threw exception
- **Throughput**: Operations per second (total / duration)
- **SLA**: Check against documented target (see [PERFORMANCE.md](PERFORMANCE.md))

## Understanding Benchmark Results

### Warmup Phase

The first 3 iterations are warmup (not counted in results):
- JVM JIT compilation settles down
- Memory initialization completes
- Cache behavior stabilizes

Results reported are from final 5 iterations with stable performance.

### Confidence Intervals

The `± 1.445 ms/op` represents 95% confidence interval:
- Smaller interval = more stable/repeatable results
- Larger interval = more variability (could indicate system noise)
- If interval > 10% of average, consider re-running

### Mode: AverageTime

All benchmarks use `AverageTime` mode:
- Measures time per operation (ms/op)
- Most intuitive for latency-sensitive operations
- Alternative: `Throughput` (ops/sec) for batch operations

## Performance SLAs

### Benchmark Targets

These are the target performance levels (p50 = median):

| Benchmark | Target | SLA Status |
|-----------|--------|-----------|
| listComponents_100_cached | <50ms | ✓ |
| listComponents_1000_cached | <100ms | ✓ |
| listComponents_10000_cached | <500ms | ✓ |
| statistics_100 | <100ms | ✓ |
| statistics_1000 | <500ms | ✓ |
| statistics_10000 | <2000ms | ✓ |
| cacheHit_overhead | <1ms | ✓ |

### Load Test Targets

| Test | Target | SLA Status |
|------|--------|-----------|
| 100 concurrent operations | <10 seconds | ✓ |
| 50 concurrent deletes | <60 seconds | ✓ |
| Memory (10K components) | <300MB | ✓ |
| Throughput (cached) | >10 ops/sec | ✓ |

## Continuous Integration

### Automatic Performance Testing

The `.github/workflows/performance.yml` workflow:

1. **Runs on every push** to main/develop branches
2. **Runs on every PR** for performance-related changes
3. **Runs daily** (3 AM UTC) for trend analysis

### Performance Regression Detection

If a benchmark is >50% slower than baseline:
1. GitHub Action warns with comment on PR
2. Build does NOT fail (informational only)
3. Developer can investigate and decide

### Accessing Results

To view performance test results:

1. **In PR**: Look for "Performance Tests" comment
2. **In Actions**: Browse `performance.yml` workflow run
3. **Artifacts**: Download benchmark/load test results for analysis

## Adding New Benchmarks

### Template for New Benchmark

```java
@Benchmark
public void myNewBenchmark_scenario(Blackhole bh) throws Exception {
    // Setup input
    List<RepoRecord> data = generateRepoRecords(1000);

    // Measure operation
    List<RepoRecord> result = service.getRepositoryRecords("repo", null, false);

    // Consume result (prevent JVM optimization)
    bh.consume(result);
}
```

### Best Practices

1. **Use Blackhole for all outputs**
   ```java
   // CORRECT: Prevent JVM from optimizing away computation
   bh.consume(result);

   // WRONG: JVM might optimize this away
   assert result != null;
   ```

2. **Keep operations small**
   - Focus on single operation
   - Avoid setup/teardown in benchmark
   - Use @Setup/@TearDown for initialization

3. **Document SLA**
   ```java
   /**
    * Benchmark: List 10K components.
    * SLA: p50 < 500ms
    */
   @Benchmark
   public void listComponents_10000_cached(Blackhole bh) { ... }
   ```

4. **Run multiple times**
   ```bash
   # Run same benchmark 5 times
   for i in {1..5}; do
     mvn test -Pbenchmark -Dtest=NexusClientBenchmark#myBenchmark -B
   done
   ```

## Adding New Load Tests

### Template for New Load Test

```java
@Test
@DisplayName("Brief description of what is tested")
void testNewScenario() throws Exception {
    int operationCount = 50;

    // Setup
    when(mockClient.listComponents(anyString(), anyBoolean()))
        .thenReturn(generateRepoRecords(1000));

    // Execute
    long start = System.currentTimeMillis();
    for (int i = 0; i < operationCount; i++) {
        service.getRepositoryRecords("repo-" + i, null, false);
    }
    long duration = System.currentTimeMillis() - start;

    // Assert SLA
    assertTrue(duration < 10000, "Should complete in <10 seconds");
}
```

### Best Practices

1. **Clear test names**
   - Use `@DisplayName` for human-readable names
   - Name suggests what is tested

2. **Measure the right thing**
   - Measure end-to-end time (don't measure test setup)
   - Use `System.currentTimeMillis()` for timing
   - Account for JVM initialization overhead

3. **Use assertions properly**
   ```java
   // GOOD: Clear assertion with context
   assertTrue(duration < 10000,
       "100 operations took " + duration + "ms (expected <10000ms)");

   // BAD: Vague assertion
   assertTrue(duration < 10000);
   ```

4. **Mock consistently**
   - Use same mock setup for reproducibility
   - Document assumptions (e.g., "100ms per HTTP call")

## Troubleshooting Performance Tests

### Benchmark fails with "Too many warnings"

**Cause**: JVM can't find safe point for measurement  
**Solution**: Increase warmup iterations in @Warmup annotation

```java
@Warmup(iterations = 5, time = 1)  // Increased from 3 to 5
```

### Load test times out

**Cause**: Test is taking longer than expected  
**Solution**: 
1. Reduce operation count
2. Increase test timeout
3. Check system resources (CPU, memory)

```java
@Test(timeout = 30000)  // 30 second timeout
void slowTest() { ... }
```

### Benchmark results show high variance

**Cause**: System interference (background tasks, GC, etc.)  
**Solution**:
1. Close other applications
2. Increase measurement time
3. Run multiple times and average

```bash
# Run 3 times and average results
mvn test -Pbenchmark -Dtest=MyBenchmark -B
mvn test -Pbenchmark -Dtest=MyBenchmark -B
mvn test -Pbenchmark -Dtest=MyBenchmark -B
```

### Memory test fails

**Cause**: Insufficient heap or unexpected allocation  
**Solution**:
1. Increase JVM heap: `java -Xmx2g ...`
2. Force garbage collection before measurement
3. Check for memory leaks in NexusClient

```java
Runtime runtime = Runtime.getRuntime();
runtime.gc();
long beforeMemory = runtime.totalMemory() - runtime.freeMemory();
// ... operation ...
runtime.gc();
long afterMemory = runtime.totalMemory() - runtime.freeMemory();
```

## Performance Regression Guide

### What constitutes a regression?

Any change >50% slower than baseline:
- **Before**: 93ms
- **After**: 140ms
- **Change**: 150% of baseline → **REGRESSION ALERT**

### How to investigate regression

1. **Check commit message**
   - Did you change caching logic?
   - Did you add nested loops?
   - Did you change data structures?

2. **Run benchmark locally**
   ```bash
   mvn test -Pbenchmark -Dtest=NexusClientBenchmark#affectedBench -B
   ```

3. **Compare results**
   - Is it consistent? (run 3+ times)
   - Is it on all dataset sizes?
   - Is it in warmup or measurement phase?

4. **Analyze code change**
   - Look for added allocations
   - Look for added I/O
   - Look for added loops
   - Use JProfiler or YourKit for detailed analysis

5. **Fix approach**
   - Revert change? (if not critical)
   - Optimize change? (cache, parallelize, etc.)
   - Adjust SLA? (if performance trade-off is acceptable)

## Performance Analysis Tools

### JProfiler

Built-in CPU profiler in IDE:

```bash
# IntelliJ IDEA: Run → Profile 'NexusClientBenchmark'
# This shows CPU time breakdown by method
```

### Java Flight Recorder (JFR)

Built-in JVM profiler:

```bash
# Record 60 seconds of JFR data
java -XX:+UnlockCommercialFeatures \
     -XX:+FlightRecorder \
     -XX:StartFlightRecording=duration=60s,filename=recording.jfr \
     -cp . org.openjdk.jmh.Main NexusClientBenchmark

# View results (in any JFR viewer)
```

### Memory Analysis

```bash
# Generate heap dump on OutOfMemoryError
java -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath=/tmp/heap.bin \
     -cp . org.junit.runner.JUnitCore NexusClientLoadTest
```

## Performance Benchmarking Best Practices

1. **Isolate variables**
   - Change only one thing per benchmark
   - Control dataset size and complexity

2. **Warm up properly**
   - Let JIT compilation settle
   - Don't measure warmup iterations

3. **Measure real scenarios**
   - Test realistic dataset sizes
   - Include network latency (mock appropriately)

4. **Document assumptions**
   - What hardware was used?
   - What was the load?
   - What were external factors?

5. **Repeat measurements**
   - Don't trust single runs
   - Average multiple runs
   - Check for outliers

## References

- [PERFORMANCE.md](PERFORMANCE.md) - Performance targets and SLAs
- [JMH Documentation](https://github.com/openjdk/jmh)
- [Java Microbenchmarking Best Practices](https://shipilev.net/blog/2014/nanotrusting-nanotime/)
- [Profiling Guide](DEVELOPMENT_GUIDE.md)

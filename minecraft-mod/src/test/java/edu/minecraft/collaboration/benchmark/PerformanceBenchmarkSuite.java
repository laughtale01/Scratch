package edu.minecraft.collaboration.benchmark;

import edu.minecraft.collaboration.core.DependencyInjector;
import edu.minecraft.collaboration.security.RateLimiter;
import edu.minecraft.collaboration.security.InputValidator;
import edu.minecraft.collaboration.security.AuthenticationManager;
import edu.minecraft.collaboration.network.CollaborationMessageProcessor;
import edu.minecraft.collaboration.config.ConfigurationManager;
import edu.minecraft.collaboration.test.categories.PerformanceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive performance benchmark suite for Minecraft Collaboration Mod
 */
@DisplayName("Performance Benchmark Suite")
@PerformanceTest
public class PerformanceBenchmarkSuite {
    
    private static final int WARMUP_ITERATIONS = 100;
    private static final int BENCHMARK_ITERATIONS = 1000;
    private static final int CONCURRENT_THREADS = 10;
    private static final int TIMEOUT_SECONDS = 30;
    
    private DependencyInjector injector;
    private RateLimiter rateLimiter;
    private InputValidator inputValidator;
    private AuthenticationManager authManager;
    private CollaborationMessageProcessor messageProcessor;
    private ConfigurationManager configManager;
    
    @BeforeEach
    void setUp() {
        injector = DependencyInjector.getInstance();
        rateLimiter = injector.getService(RateLimiter.class);
        authManager = injector.getService(AuthenticationManager.class);
        messageProcessor = injector.getService(CollaborationMessageProcessor.class);
        configManager = injector.getService(ConfigurationManager.class);
    }
    
    @Test
    @DisplayName("Rate Limiter Performance Benchmark")
    @Timeout(TIMEOUT_SECONDS)
    void benchmarkRateLimiter() {
        System.out.println("🚀 Rate Limiter Performance Benchmark");
        
        // Warmup
        warmupRateLimiter();
        
        // Benchmark single-threaded performance
        BenchmarkResult singleThreaded = benchmarkRateLimiterSingleThreaded();
        System.out.printf("Single-threaded: %.2f ops/sec, avg latency: %.3f ms%n", 
            singleThreaded.operationsPerSecond, singleThreaded.averageLatencyMs);
        
        // Benchmark multi-threaded performance
        BenchmarkResult multiThreaded = benchmarkRateLimiterMultiThreaded();
        System.out.printf("Multi-threaded (%d threads): %.2f ops/sec, avg latency: %.3f ms%n", 
            CONCURRENT_THREADS, multiThreaded.operationsPerSecond, multiThreaded.averageLatencyMs);
        
        // Performance assertions
        assertTrue(singleThreaded.operationsPerSecond > 10000, "Rate limiter should handle >10k ops/sec");
        assertTrue(singleThreaded.averageLatencyMs < 1.0, "Average latency should be <1ms");
        assertTrue(multiThreaded.operationsPerSecond > 5000, "Multi-threaded should handle >5k ops/sec total");
    }
    
    @Test
    @DisplayName("Input Validation Performance Benchmark")
    @Timeout(TIMEOUT_SECONDS)
    void benchmarkInputValidation() {
        System.out.println("🔍 Input Validation Performance Benchmark");
        
        // Test different validation scenarios
        Map<String, BenchmarkResult> results = new HashMap<>();
        
        // Username validation
        results.put("Username", benchmarkUsernameValidation());
        
        // Coordinate validation
        results.put("Coordinates", benchmarkCoordinateValidation());
        
        // Block type validation
        results.put("BlockType", benchmarkBlockTypeValidation());
        
        // Chat message validation
        results.put("ChatMessage", benchmarkChatMessageValidation());
        
        // Print results
        results.forEach((test, result) -> {
            System.out.printf("%s validation: %.2f ops/sec, avg latency: %.3f ms%n",
                test, result.operationsPerSecond, result.averageLatencyMs);
            
            // Performance assertions
            assertTrue(result.operationsPerSecond > 1000, 
                test + " validation should handle >1k ops/sec");
            assertTrue(result.averageLatencyMs < 5.0, 
                test + " validation latency should be <5ms");
        });
    }
    
    @Test
    @DisplayName("Authentication Performance Benchmark")
    @Timeout(TIMEOUT_SECONDS)
    void benchmarkAuthentication() {
        System.out.println("🔐 Authentication Performance Benchmark");
        
        // Token generation benchmark
        BenchmarkResult tokenGeneration = benchmarkTokenGeneration();
        System.out.printf("Token generation: %.2f ops/sec, avg latency: %.3f ms%n",
            tokenGeneration.operationsPerSecond, tokenGeneration.averageLatencyMs);
        
        // Token validation benchmark
        BenchmarkResult tokenValidation = benchmarkTokenValidation();
        System.out.printf("Token validation: %.2f ops/sec, avg latency: %.3f ms%n",
            tokenValidation.operationsPerSecond, tokenValidation.averageLatencyMs);
        
        // Connection authentication benchmark
        BenchmarkResult connectionAuth = benchmarkConnectionAuthentication();
        System.out.printf("Connection auth: %.2f ops/sec, avg latency: %.3f ms%n",
            connectionAuth.operationsPerSecond, connectionAuth.averageLatencyMs);
        
        // Performance assertions
        assertTrue(tokenGeneration.operationsPerSecond > 100, "Token generation should handle >100 ops/sec");
        assertTrue(tokenValidation.operationsPerSecond > 1000, "Token validation should handle >1k ops/sec");
        assertTrue(connectionAuth.operationsPerSecond > 500, "Connection auth should handle >500 ops/sec");
    }
    
    @Test
    @DisplayName("Message Processing Performance Benchmark")
    @Timeout(TIMEOUT_SECONDS)
    void benchmarkMessageProcessing() {
        System.out.println("📨 Message Processing Performance Benchmark");
        
        // Test different message types
        Map<String, BenchmarkResult> results = new HashMap<>();
        
        results.put("SimpleCommand", benchmarkSimpleCommandProcessing());
        results.put("JsonCommand", benchmarkJsonCommandProcessing());
        results.put("ComplexCommand", benchmarkComplexCommandProcessing());
        
        // Print results
        results.forEach((messageType, result) -> {
            System.out.printf("%s processing: %.2f ops/sec, avg latency: %.3f ms%n",
                messageType, result.operationsPerSecond, result.averageLatencyMs);
            
            // Performance assertions
            assertTrue(result.operationsPerSecond > 100, 
                messageType + " processing should handle >100 ops/sec");
            assertTrue(result.averageLatencyMs < 50.0, 
                messageType + " processing latency should be <50ms");
        });
    }
    
    @Test
    @DisplayName("Configuration Manager Performance Benchmark")
    @Timeout(TIMEOUT_SECONDS)
    void benchmarkConfigurationManager() {
        System.out.println("⚙️ Configuration Manager Performance Benchmark");
        
        // Property access benchmark
        BenchmarkResult propertyAccess = benchmarkPropertyAccess();
        System.out.printf("Property access: %.2f ops/sec, avg latency: %.3f ms%n",
            propertyAccess.operationsPerSecond, propertyAccess.averageLatencyMs);
        
        // Property setting benchmark
        BenchmarkResult propertySetting = benchmarkPropertySetting();
        System.out.printf("Property setting: %.2f ops/sec, avg latency: %.3f ms%n",
            propertySetting.operationsPerSecond, propertySetting.averageLatencyMs);
        
        // Performance assertions
        assertTrue(propertyAccess.operationsPerSecond > 10000, "Property access should handle >10k ops/sec");
        assertTrue(propertySetting.operationsPerSecond > 1000, "Property setting should handle >1k ops/sec");
    }
    
    @Test
    @DisplayName("Memory Usage Benchmark")
    @Timeout(TIMEOUT_SECONDS)
    void benchmarkMemoryUsage() {
        System.out.println("💾 Memory Usage Benchmark");
        
        Runtime runtime = Runtime.getRuntime();
        
        // Measure baseline memory
        System.gc();
        long baselineMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // Create many objects to test memory pressure
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            tokens.add(authManager.generateToken("user" + i, AuthenticationManager.UserRole.STUDENT));
        }
        
        // Measure memory after object creation
        System.gc();
        long afterCreationMemory = runtime.totalMemory() - runtime.freeMemory();
        
        long memoryIncrease = afterCreationMemory - baselineMemory;
        double memoryPerToken = (double) memoryIncrease / tokens.size();
        
        System.out.printf("Memory usage: baseline=%.2f MB, after creation=%.2f MB%n",
            baselineMemory / 1024.0 / 1024.0, afterCreationMemory / 1024.0 / 1024.0);
        System.out.printf("Memory per token: %.2f bytes%n", memoryPerToken);
        
        // Memory assertions
        assertTrue(memoryPerToken < 1000, "Memory per token should be <1KB");
        assertTrue(memoryIncrease < 50 * 1024 * 1024, "Total memory increase should be <50MB");
        
        // Clean up
        tokens.clear();
        System.gc();
    }
    
    // Warmup methods
    private void warmupRateLimiter() {
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            rateLimiter.allowCommand("warmup-" + i);
            if (i % 10 == 0) rateLimiter.resetLimit("warmup-" + i);
        }
    }
    
    // Rate limiter benchmarks
    private BenchmarkResult benchmarkRateLimiterSingleThreaded() {
        return executeBenchmark("rateLimiter-single", BENCHMARK_ITERATIONS, () -> {
            rateLimiter.allowCommand("benchmark-single");
        });
    }
    
    private BenchmarkResult benchmarkRateLimiterMultiThreaded() {
        AtomicInteger counter = new AtomicInteger(0);
        
        return executeConcurrentBenchmark("rateLimiter-multi", BENCHMARK_ITERATIONS, CONCURRENT_THREADS, () -> {
            int id = counter.incrementAndGet();
            rateLimiter.allowCommand("benchmark-multi-" + (id % 100));
        });
    }
    
    // Input validation benchmarks
    private BenchmarkResult benchmarkUsernameValidation() {
        return executeBenchmark("username-validation", BENCHMARK_ITERATIONS, () -> {
            InputValidator.validateUsername("testUser123");
        });
    }
    
    private BenchmarkResult benchmarkCoordinateValidation() {
        return executeBenchmark("coordinate-validation", BENCHMARK_ITERATIONS, () -> {
            InputValidator.validateCoordinates("100", "64", "200");
        });
    }
    
    private BenchmarkResult benchmarkBlockTypeValidation() {
        return executeBenchmark("blocktype-validation", BENCHMARK_ITERATIONS, () -> {
            InputValidator.validateBlockType("minecraft:stone");
        });
    }
    
    private BenchmarkResult benchmarkChatMessageValidation() {
        return executeBenchmark("chatmessage-validation", BENCHMARK_ITERATIONS, () -> {
            InputValidator.validateChatMessage("Hello, world!");
        });
    }
    
    // Authentication benchmarks
    private BenchmarkResult benchmarkTokenGeneration() {
        return executeBenchmark("token-generation", BENCHMARK_ITERATIONS / 10, () -> {
            authManager.generateToken("benchmarkUser", AuthenticationManager.UserRole.STUDENT);
        });
    }
    
    private BenchmarkResult benchmarkTokenValidation() {
        String token = authManager.generateToken("benchmarkUser", AuthenticationManager.UserRole.STUDENT);
        
        return executeBenchmark("token-validation", BENCHMARK_ITERATIONS, () -> {
            authManager.validateToken(token);
        });
    }
    
    private BenchmarkResult benchmarkConnectionAuthentication() {
        String token = authManager.generateToken("benchmarkUser", AuthenticationManager.UserRole.STUDENT);
        
        return executeBenchmark("connection-auth", BENCHMARK_ITERATIONS / 2, () -> {
            String connId = "benchmark-conn-" + UUID.randomUUID().toString().substring(0, 8);
            authManager.authenticateConnection(connId, token);
            authManager.removeConnection(connId);
        });
    }
    
    // Message processing benchmarks
    private BenchmarkResult benchmarkSimpleCommandProcessing() {
        return executeBenchmark("simple-command", BENCHMARK_ITERATIONS / 10, () -> {
            messageProcessor.processMessage("ping", null);
        });
    }
    
    private BenchmarkResult benchmarkJsonCommandProcessing() {
        String jsonMessage = "{\"action\":\"getPosition\",\"data\":{}}";
        
        return executeBenchmark("json-command", BENCHMARK_ITERATIONS / 10, () -> {
            messageProcessor.processMessage(jsonMessage, null);
        });
    }
    
    private BenchmarkResult benchmarkComplexCommandProcessing() {
        String complexMessage = "{\"action\":\"placeBlock\",\"x\":100,\"y\":64,\"z\":200,\"blockType\":\"stone\",\"data\":{\"metadata\":\"test\"}}";
        
        return executeBenchmark("complex-command", BENCHMARK_ITERATIONS / 20, () -> {
            messageProcessor.processMessage(complexMessage, null);
        });
    }
    
    // Configuration manager benchmarks
    private BenchmarkResult benchmarkPropertyAccess() {
        return executeBenchmark("property-access", BENCHMARK_ITERATIONS, () -> {
            configManager.getProperty("server.host", "localhost");
        });
    }
    
    private BenchmarkResult benchmarkPropertySetting() {
        AtomicInteger counter = new AtomicInteger(0);
        
        return executeBenchmark("property-setting", BENCHMARK_ITERATIONS / 10, () -> {
            int id = counter.incrementAndGet();
            configManager.setProperty("benchmark.property." + id, "value" + id);
        });
    }
    
    // Utility methods
    private BenchmarkResult executeBenchmark(String name, int iterations, Runnable operation) {
        // Warmup
        for (int i = 0; i < Math.min(iterations / 10, 100); i++) {
            operation.run();
        }
        
        // Benchmark
        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            operation.run();
        }
        long endTime = System.nanoTime();
        
        long totalTimeNs = endTime - startTime;
        double totalTimeMs = totalTimeNs / 1_000_000.0;
        double operationsPerSecond = (iterations * 1000.0) / totalTimeMs;
        double averageLatencyMs = totalTimeMs / iterations;
        
        return new BenchmarkResult(name, iterations, totalTimeMs, operationsPerSecond, averageLatencyMs);
    }
    
    private BenchmarkResult executeConcurrentBenchmark(String name, int totalOperations, int threadCount, Runnable operation) {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicLong totalTime = new AtomicLong(0);
        
        try {
            List<CompletableFuture<Long>> futures = new ArrayList<>();
            
            int operationsPerThread = totalOperations / threadCount;
            
            for (int t = 0; t < threadCount; t++) {
                CompletableFuture<Long> future = CompletableFuture.supplyAsync(() -> {
                    long startTime = System.nanoTime();
                    for (int i = 0; i < operationsPerThread; i++) {
                        operation.run();
                    }
                    return System.nanoTime() - startTime;
                }, executor);
                
                futures.add(future);
            }
            
            // Wait for all threads to complete and sum up times
            long maxTime = 0;
            for (CompletableFuture<Long> future : futures) {
                long threadTime = future.get();
                maxTime = Math.max(maxTime, threadTime);
            }
            
            double totalTimeMs = maxTime / 1_000_000.0;
            double operationsPerSecond = (totalOperations * 1000.0) / totalTimeMs;
            double averageLatencyMs = totalTimeMs / totalOperations;
            
            return new BenchmarkResult(name, totalOperations, totalTimeMs, operationsPerSecond, averageLatencyMs);
            
        } catch (Exception e) {
            throw new RuntimeException("Benchmark failed", e);
        } finally {
            executor.shutdown();
        }
    }
    
    // Result class
    private static class BenchmarkResult {
        final String name;
        final int iterations;
        final double totalTimeMs;
        final double operationsPerSecond;
        final double averageLatencyMs;
        
        BenchmarkResult(String name, int iterations, double totalTimeMs, double operationsPerSecond, double averageLatencyMs) {
            this.name = name;
            this.iterations = iterations;
            this.totalTimeMs = totalTimeMs;
            this.operationsPerSecond = operationsPerSecond;
            this.averageLatencyMs = averageLatencyMs;
        }
    }
}
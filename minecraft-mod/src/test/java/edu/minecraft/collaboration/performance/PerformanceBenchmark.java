package com.yourname.minecraftcollaboration.performance;

import com.yourname.minecraftcollaboration.monitoring.MetricsCollector;
import com.yourname.minecraftcollaboration.collaboration.CollaborationManager;
import com.yourname.minecraftcollaboration.models.Invitation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance benchmarks for the Minecraft Collaboration system
 * Run with: ./gradlew test --tests PerformanceBenchmark
 */
@Tag("benchmark")
class PerformanceBenchmark {
    
    private MetricsCollector metrics;
    private CollaborationManager collaborationManager;
    private BatchBlockPlacer blockPlacer;
    
    @BeforeEach
    void setUp() {
        metrics = MetricsCollector.getInstance();
        collaborationManager = CollaborationManager.getInstance();
        blockPlacer = new BatchBlockPlacer(null); // Mock world
    }
    
    @Test
    @DisplayName("Benchmark: Single block placement")
    void benchmarkSingleBlockPlacement() {
        int iterations = 10000;
        List<Long> times = new ArrayList<>();
        
        // Warmup
        for (int i = 0; i < 1000; i++) {
            blockPlacer.queueBlock(new BlockPos(i, 64, 0), Blocks.STONE.defaultBlockState());
        }
        blockPlacer.clear();
        
        // Benchmark
        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            blockPlacer.queueBlock(new BlockPos(i, 64, 0), Blocks.STONE.defaultBlockState());
            long end = System.nanoTime();
            times.add(end - start);
        }
        
        // Analysis
        double avgTimeNs = times.stream().mapToLong(Long::longValue).average().orElse(0);
        double avgTimeMs = avgTimeNs / 1_000_000;
        long maxTimeNs = times.stream().mapToLong(Long::longValue).max().orElse(0);
        double maxTimeMs = maxTimeNs / 1_000_000.0;
        
        System.out.println("=== Single Block Placement Benchmark ===");
        System.out.println("Iterations: " + iterations);
        System.out.println("Average time: " + String.format("%.3f ms", avgTimeMs));
        System.out.println("Max time: " + String.format("%.3f ms", maxTimeMs));
        System.out.println("Operations/second: " + String.format("%.0f", 1000.0 / avgTimeMs));
        
        // Performance assertion
        assertTrue(avgTimeMs < 0.1, "Single block placement should be < 0.1ms");
    }
    
    @Test
    @DisplayName("Benchmark: Batch block placement (1000 blocks)")
    void benchmarkBatchBlockPlacement() {
        int batchSize = 1000;
        int iterations = 100;
        List<Long> times = new ArrayList<>();
        
        for (int iter = 0; iter < iterations; iter++) {
            // Queue blocks
            for (int i = 0; i < batchSize; i++) {
                blockPlacer.queueBlock(
                    new BlockPos(i % 100, 64 + (i / 10000), (i / 100) % 100), 
                    Blocks.STONE.defaultBlockState()
                );
            }
            
            // Measure placement time
            long start = System.nanoTime();
            // Simulate batch placement
            blockPlacer.processBatch();
            long end = System.nanoTime();
            times.add(end - start);
            
            blockPlacer.clear();
        }
        
        // Analysis
        double avgTimeMs = times.stream().mapToLong(Long::longValue).average().orElse(0) / 1_000_000;
        double blocksPerSecond = (batchSize * 1000.0) / avgTimeMs;
        
        System.out.println("\n=== Batch Block Placement Benchmark ===");
        System.out.println("Batch size: " + batchSize);
        System.out.println("Iterations: " + iterations);
        System.out.println("Average time per batch: " + String.format("%.2f ms", avgTimeMs));
        System.out.println("Blocks per second: " + String.format("%.0f", blocksPerSecond));
        
        // Performance assertion
        assertTrue(blocksPerSecond > 10000, "Should place > 10,000 blocks/second");
    }
    
    @Test
    @DisplayName("Benchmark: Collaboration invitation processing")
    void benchmarkInvitationProcessing() {
        int invitationCount = 1000;
        List<Long> createTimes = new ArrayList<>();
        List<Long> acceptTimes = new ArrayList<>();
        
        // Benchmark invitation creation
        for (int i = 0; i < invitationCount; i++) {
            String sender = "Player" + (i % 100);
            String recipient = "Player" + ((i + 1) % 100);
            
            long start = System.nanoTime();
            Invitation invitation = collaborationManager.createInvitation(sender, recipient);
            long end = System.nanoTime();
            createTimes.add(end - start);
            
            // Benchmark acceptance
            start = System.nanoTime();
            collaborationManager.acceptInvitation(invitation.getId());
            end = System.nanoTime();
            acceptTimes.add(end - start);
        }
        
        // Analysis
        double avgCreateMs = createTimes.stream().mapToLong(Long::longValue).average().orElse(0) / 1_000_000;
        double avgAcceptMs = acceptTimes.stream().mapToLong(Long::longValue).average().orElse(0) / 1_000_000;
        
        System.out.println("\n=== Invitation Processing Benchmark ===");
        System.out.println("Invitations: " + invitationCount);
        System.out.println("Average creation time: " + String.format("%.3f ms", avgCreateMs));
        System.out.println("Average acceptance time: " + String.format("%.3f ms", avgAcceptMs));
        System.out.println("Total operations/second: " + 
            String.format("%.0f", 1000.0 / (avgCreateMs + avgAcceptMs)));
        
        // Performance assertion
        assertTrue(avgCreateMs < 1.0, "Invitation creation should be < 1ms");
        assertTrue(avgAcceptMs < 1.0, "Invitation acceptance should be < 1ms");
    }
    
    @Test
    @DisplayName("Benchmark: Concurrent WebSocket message processing")
    void benchmarkConcurrentMessageProcessing() throws InterruptedException {
        int threadCount = 10;
        int messagesPerThread = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<CompletableFuture<Long>> futures = new ArrayList<>();
        
        // Create concurrent tasks
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            CompletableFuture<Long> future = CompletableFuture.supplyAsync(() -> {
                long totalTime = 0;
                for (int i = 0; i < messagesPerThread; i++) {
                    long start = System.nanoTime();
                    
                    // Simulate message processing
                    String message = String.format(
                        "{\"command\":\"placeBlock\",\"args\":{\"x\":\"%d\",\"y\":\"64\",\"z\":\"%d\",\"block\":\"stone\"}}",
                        threadId * 100 + i, i
                    );
                    
                    // Process message (mocked)
                    metrics.incrementCounter(MetricsCollector.Metrics.WS_MESSAGES_RECEIVED);
                    
                    long end = System.nanoTime();
                    totalTime += (end - start);
                }
                return totalTime;
            }, executor);
            futures.add(future);
        }
        
        // Wait for completion
        long startTime = System.currentTimeMillis();
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0])
        );
        allFutures.get(30, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();
        
        // Calculate results
        long totalMessages = threadCount * messagesPerThread;
        double totalTimeSeconds = (endTime - startTime) / 1000.0;
        double messagesPerSecond = totalMessages / totalTimeSeconds;
        
        System.out.println("\n=== Concurrent Message Processing Benchmark ===");
        System.out.println("Threads: " + threadCount);
        System.out.println("Messages per thread: " + messagesPerThread);
        System.out.println("Total messages: " + totalMessages);
        System.out.println("Total time: " + String.format("%.2f seconds", totalTimeSeconds));
        System.out.println("Messages per second: " + String.format("%.0f", messagesPerSecond));
        
        executor.shutdown();
        
        // Performance assertion
        assertTrue(messagesPerSecond > 1000, "Should process > 1000 messages/second");
    }
    
    @Test
    @DisplayName("Benchmark: Memory allocation for large structures")
    void benchmarkMemoryAllocation() {
        int iterations = 100;
        List<Long> memoryUsages = new ArrayList<>();
        Runtime runtime = Runtime.getRuntime();
        
        for (int iter = 0; iter < iterations; iter++) {
            // Force garbage collection
            System.gc();
            Thread.yield();
            
            long beforeMemory = runtime.totalMemory() - runtime.freeMemory();
            
            // Create large structure (10x10x10 cube = 1000 blocks)
            List<BlockPos> positions = new ArrayList<>();
            for (int x = 0; x < 10; x++) {
                for (int y = 0; y < 10; y++) {
                    for (int z = 0; z < 10; z++) {
                        positions.add(new BlockPos(x, y + 64, z));
                    }
                }
            }
            
            // Queue all blocks
            for (BlockPos pos : positions) {
                blockPlacer.queueBlock(pos, Blocks.STONE.defaultBlockState());
            }
            
            long afterMemory = runtime.totalMemory() - runtime.freeMemory();
            long memoryUsed = afterMemory - beforeMemory;
            memoryUsages.add(memoryUsed);
            
            // Clear for next iteration
            blockPlacer.clear();
            positions.clear();
        }
        
        // Analysis
        double avgMemoryKB = memoryUsages.stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(0) / 1024.0;
        
        double maxMemoryKB = memoryUsages.stream()
            .mapToLong(Long::longValue)
            .max()
            .orElse(0) / 1024.0;
        
        System.out.println("\n=== Memory Allocation Benchmark ===");
        System.out.println("Structure size: 10x10x10 (1000 blocks)");
        System.out.println("Iterations: " + iterations);
        System.out.println("Average memory used: " + String.format("%.2f KB", avgMemoryKB));
        System.out.println("Max memory used: " + String.format("%.2f KB", maxMemoryKB));
        System.out.println("Memory per block: " + String.format("%.2f bytes", avgMemoryKB * 1024 / 1000));
        
        // Performance assertion
        assertTrue(avgMemoryKB < 1000, "1000 blocks should use < 1MB memory");
    }
    
    @Test
    @DisplayName("Benchmark: Metrics collection overhead")
    void benchmarkMetricsOverhead() {
        int iterations = 100000;
        
        // Benchmark without metrics
        long startNoMetrics = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            // Simulate operation without metrics
            int result = i * 2;
        }
        long endNoMetrics = System.nanoTime();
        double timeNoMetricsMs = (endNoMetrics - startNoMetrics) / 1_000_000.0;
        
        // Benchmark with metrics
        long startWithMetrics = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            metrics.incrementCounter("benchmark.test");
            int result = i * 2;
        }
        long endWithMetrics = System.nanoTime();
        double timeWithMetricsMs = (endWithMetrics - startWithMetrics) / 1_000_000.0;
        
        // Analysis
        double overheadMs = timeWithMetricsMs - timeNoMetricsMs;
        double overheadPerOperation = overheadMs / iterations * 1000; // microseconds
        double overheadPercentage = (overheadMs / timeNoMetricsMs) * 100;
        
        System.out.println("\n=== Metrics Collection Overhead Benchmark ===");
        System.out.println("Iterations: " + iterations);
        System.out.println("Time without metrics: " + String.format("%.2f ms", timeNoMetricsMs));
        System.out.println("Time with metrics: " + String.format("%.2f ms", timeWithMetricsMs));
        System.out.println("Total overhead: " + String.format("%.2f ms", overheadMs));
        System.out.println("Overhead per operation: " + String.format("%.3f ﾎｼs", overheadPerOperation));
        System.out.println("Overhead percentage: " + String.format("%.1f%%", overheadPercentage));
        
        // Performance assertion
        assertTrue(overheadPerOperation < 1.0, "Metrics overhead should be < 1ﾎｼs per operation");
    }
    
    /**
     * Helper method to print benchmark summary
     */
    private void printSummary() {
        System.out.println("\n=== PERFORMANCE BENCHMARK SUMMARY ===");
        System.out.println("All benchmarks completed successfully");
        System.out.println("Results indicate good performance characteristics");
        System.out.println("Consider running with profiler for detailed analysis");
    }
}
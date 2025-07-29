package edu.minecraft.collaboration.performance;

import edu.minecraft.collaboration.monitoring.MetricsCollector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * Micro-benchmarks for specific operations
 */
@Tag("benchmark")
class MicroBenchmarks {
    
    private static final int WARMUP_ITERATIONS = 1000;
    private static final int BENCHMARK_ITERATIONS = 10000;
    
    @BeforeEach
    void setUp() {
        // Ensure JIT compilation
        warmup();
    }
    
    private void warmup() {
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            Math.sqrt(i);
            new ArrayList<>().add(i);
            new HashMap<>().put(i, i);
        }
    }
    
    @Test
    @DisplayName("Micro-benchmark: String concatenation vs StringBuilder")
    void benchmarkStringOperations() {
        // String concatenation
        long startConcat = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            String result = "Player" + i + " joined at " + System.currentTimeMillis();
        }
        long endConcat = System.nanoTime();
        
        // StringBuilder
        long startBuilder = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            String result = new StringBuilder()
                .append("Player").append(i)
                .append(" joined at ").append(System.currentTimeMillis())
                .toString();
        }
        long endBuilder = System.nanoTime();
        
        // String.format
        long startFormat = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            String result = String.format("Player%d joined at %d", i, System.currentTimeMillis());
        }
        long endFormat = System.nanoTime();
        
        // Results
        double concatMs = (endConcat - startConcat) / 1_000_000.0;
        double builderMs = (endBuilder - startBuilder) / 1_000_000.0;
        double formatMs = (endFormat - startFormat) / 1_000_000.0;
        
        System.out.println("=== String Operations Benchmark ===");
        System.out.println("Iterations: " + BENCHMARK_ITERATIONS);
        System.out.println("String concatenation: " + String.format("%.2f ms", concatMs));
        System.out.println("StringBuilder: " + String.format("%.2f ms", builderMs));
        System.out.println("String.format: " + String.format("%.2f ms", formatMs));
        System.out.println("StringBuilder is " + String.format("%.1fx faster than concatenation", concatMs / builderMs));
    }
    
    @Test
    @DisplayName("Micro-benchmark: Collection types performance")
    void benchmarkCollections() {
        int size = 10000;
        
        // ArrayList
        long startArrayList = System.nanoTime();
        List<Integer> arrayList = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            arrayList.add(i);
        }
        for (int i = 0; i < size; i++) {
            arrayList.contains(i);
        }
        long endArrayList = System.nanoTime();
        
        // LinkedList
        long startLinkedList = System.nanoTime();
        List<Integer> linkedList = new LinkedList<>();
        for (int i = 0; i < size; i++) {
            linkedList.add(i);
        }
        for (int i = 0; i < size; i++) {
            linkedList.contains(i);
        }
        long endLinkedList = System.nanoTime();
        
        // HashSet
        long startHashSet = System.nanoTime();
        Set<Integer> hashSet = new HashSet<>();
        for (int i = 0; i < size; i++) {
            hashSet.add(i);
        }
        for (int i = 0; i < size; i++) {
            hashSet.contains(i);
        }
        long endHashSet = System.nanoTime();
        
        // Results
        double arrayListMs = (endArrayList - startArrayList) / 1_000_000.0;
        double linkedListMs = (endLinkedList - startLinkedList) / 1_000_000.0;
        double hashSetMs = (endHashSet - startHashSet) / 1_000_000.0;
        
        System.out.println("\n=== Collection Performance Benchmark ===");
        System.out.println("Size: " + size);
        System.out.println("ArrayList (add + contains): " + String.format("%.2f ms", arrayListMs));
        System.out.println("LinkedList (add + contains): " + String.format("%.2f ms", linkedListMs));
        System.out.println("HashSet (add + contains): " + String.format("%.2f ms", hashSetMs));
        System.out.println("HashSet is " + String.format("%.1fx faster than ArrayList for contains", arrayListMs / hashSetMs));
    }
    
    @Test
    @DisplayName("Micro-benchmark: Map implementations")
    void benchmarkMaps() {
        int operations = 10000;
        
        // HashMap
        long startHashMap = System.nanoTime();
        Map<String, Integer> hashMap = new HashMap<>();
        for (int i = 0; i < operations; i++) {
            hashMap.put("key" + i, i);
        }
        for (int i = 0; i < operations; i++) {
            hashMap.get("key" + i);
        }
        long endHashMap = System.nanoTime();
        
        // TreeMap
        long startTreeMap = System.nanoTime();
        Map<String, Integer> treeMap = new TreeMap<>();
        for (int i = 0; i < operations; i++) {
            treeMap.put("key" + i, i);
        }
        for (int i = 0; i < operations; i++) {
            treeMap.get("key" + i);
        }
        long endTreeMap = System.nanoTime();
        
        // ConcurrentHashMap
        long startConcurrentMap = System.nanoTime();
        Map<String, Integer> concurrentMap = new ConcurrentHashMap<>();
        for (int i = 0; i < operations; i++) {
            concurrentMap.put("key" + i, i);
        }
        for (int i = 0; i < operations; i++) {
            concurrentMap.get("key" + i);
        }
        long endConcurrentMap = System.nanoTime();
        
        // Results
        double hashMapMs = (endHashMap - startHashMap) / 1_000_000.0;
        double treeMapMs = (endTreeMap - startTreeMap) / 1_000_000.0;
        double concurrentMapMs = (endConcurrentMap - startConcurrentMap) / 1_000_000.0;
        
        System.out.println("\n=== Map Performance Benchmark ===");
        System.out.println("Operations: " + operations);
        System.out.println("HashMap: " + String.format("%.2f ms", hashMapMs));
        System.out.println("TreeMap: " + String.format("%.2f ms", treeMapMs));
        System.out.println("ConcurrentHashMap: " + String.format("%.2f ms", concurrentMapMs));
        System.out.println("HashMap is " + String.format("%.1fx faster than TreeMap", treeMapMs / hashMapMs));
    }
    
    @Test
    @DisplayName("Micro-benchmark: Stream vs traditional loop")
    void benchmarkStreamVsLoop() {
        List<Integer> numbers = new ArrayList<>();
        for (int i = 0; i < 100000; i++) {
            numbers.add(i);
        }
        
        // Traditional loop
        long startLoop = System.nanoTime();
        int sumLoop = 0;
        for (Integer n : numbers) {
            if (n % 2 == 0) {
                sumLoop += n * 2;
            }
        }
        long endLoop = System.nanoTime();
        
        // Stream
        long startStream = System.nanoTime();
        int sumStream = numbers.stream()
            .filter(n -> n % 2 == 0)
            .mapToInt(n -> n * 2)
            .sum();
        long endStream = System.nanoTime();
        
        // Parallel Stream
        long startParallel = System.nanoTime();
        int sumParallel = numbers.parallelStream()
            .filter(n -> n % 2 == 0)
            .mapToInt(n -> n * 2)
            .sum();
        long endParallel = System.nanoTime();
        
        // Results
        double loopMs = (endLoop - startLoop) / 1_000_000.0;
        double streamMs = (endStream - startStream) / 1_000_000.0;
        double parallelMs = (endParallel - startParallel) / 1_000_000.0;
        
        System.out.println("\n=== Stream vs Loop Benchmark ===");
        System.out.println("List size: " + numbers.size());
        System.out.println("Traditional loop: " + String.format("%.2f ms", loopMs));
        System.out.println("Stream: " + String.format("%.2f ms", streamMs));
        System.out.println("Parallel stream: " + String.format("%.2f ms", parallelMs));
        System.out.println("Loop is " + String.format("%.1fx faster than stream", streamMs / loopMs));
        
        assertEquals(sumLoop, sumStream, "Results should match");
    }
    
    @Test
    @DisplayName("Micro-benchmark: Synchronization methods")
    void benchmarkSynchronization() {
        int iterations = 1000000;
        
        // Synchronized method
        class SynchronizedCounter {
            private int count = 0;
            synchronized void increment() { count++; }
            synchronized int get() { return count; }
        }
        
        SynchronizedCounter syncCounter = new SynchronizedCounter();
        long startSync = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            syncCounter.increment();
        }
        long endSync = System.nanoTime();
        
        // AtomicInteger
        AtomicInteger atomicCounter = new AtomicInteger(0);
        long startAtomic = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            atomicCounter.incrementAndGet();
        }
        long endAtomic = System.nanoTime();
        
        // Volatile (not thread-safe for increment, just for comparison)
        class VolatileCounter {
            private volatile int count = 0;
            void increment() { count++; } // NOT thread-safe
            int get() { return count; }
        }
        
        VolatileCounter volatileCounter = new VolatileCounter();
        long startVolatile = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            volatileCounter.increment();
        }
        long endVolatile = System.nanoTime();
        
        // Results
        double syncMs = (endSync - startSync) / 1_000_000.0;
        double atomicMs = (endAtomic - startAtomic) / 1_000_000.0;
        double volatileMs = (endVolatile - startVolatile) / 1_000_000.0;
        
        System.out.println("\n=== Synchronization Benchmark ===");
        System.out.println("Iterations: " + iterations);
        System.out.println("Synchronized: " + String.format("%.2f ms", syncMs));
        System.out.println("AtomicInteger: " + String.format("%.2f ms", atomicMs));
        System.out.println("Volatile (unsafe): " + String.format("%.2f ms", volatileMs));
        System.out.println("AtomicInteger is " + String.format("%.1fx faster than synchronized", syncMs / atomicMs));
    }
    
    @Test
    @DisplayName("Micro-benchmark: Exception handling overhead")
    void benchmarkExceptionHandling() {
        int iterations = 100000;
        
        // No exception
        long startNoException = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            int result = processWithoutException(i);
        }
        long endNoException = System.nanoTime();
        
        // With try-catch but no exception thrown
        long startTryCatch = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            try {
                int result = processWithoutException(i);
            } catch (Exception e) {
                // Never reached
            }
        }
        long endTryCatch = System.nanoTime();
        
        // With exception thrown and caught
        long startException = System.nanoTime();
        for (int i = 0; i < iterations / 100; i++) { // Fewer iterations due to overhead
            try {
                int result = processWithException();
            } catch (Exception e) {
                // Exception caught
            }
        }
        long endException = System.nanoTime();
        
        // Results
        double noExceptionMs = (endNoException - startNoException) / 1_000_000.0;
        double tryCatchMs = (endTryCatch - startTryCatch) / 1_000_000.0;
        double exceptionMs = (endException - startException) / 1_000_000.0 * 100; // Normalize
        
        System.out.println("\n=== Exception Handling Benchmark ===");
        System.out.println("Iterations: " + iterations);
        System.out.println("No exception: " + String.format("%.2f ms", noExceptionMs));
        System.out.println("Try-catch (no throw): " + String.format("%.2f ms", tryCatchMs));
        System.out.println("Exception thrown: " + String.format("%.2f ms (normalized)", exceptionMs));
        System.out.println("Try-catch overhead: " + String.format("%.1f%%", ((tryCatchMs - noExceptionMs) / noExceptionMs) * 100));
        System.out.println("Exception overhead: " + String.format("%.1fx", exceptionMs / noExceptionMs));
    }
    
    private int processWithoutException(int value) {
        return value * 2;
    }
    
    private int processWithException() throws Exception {
        throw new Exception("Test exception");
    }
    
    private void assertEquals(int expected, int actual, String message) {
        if (expected != actual) {
            throw new AssertionError(message + " - Expected: " + expected + ", Actual: " + actual);
        }
    }
}
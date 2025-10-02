package edu.minecraft.collaboration.performance;

import edu.minecraft.collaboration.test.categories.IntegrationTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Performance tests for WebSocket server
 */
@IntegrationTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("WebSocket Performance Tests")
@org.junit.jupiter.api.Tag("minecraft-dependent")
@org.junit.jupiter.api.Disabled("Requires Minecraft runtime - run in integration environment")
public class WebSocketPerformanceTest extends edu.minecraft.collaboration.test.MinecraftTestBase {
    
    private static String WS_URL;
    private static final int WARM_UP_MESSAGES = 100;
    private HttpClient httpClient;
    private static edu.minecraft.collaboration.test.util.WebSocketTestServer testServer;
    
    @BeforeAll
    static void startServer() throws Exception {
        // Start test WebSocket server
        testServer = edu.minecraft.collaboration.test.util.WebSocketTestServer.startTestServer(14711);
        if (testServer == null) {
            System.out.println("WebSocket server could not start - performance tests will be skipped");
        } else {
            WS_URL = "ws://localhost:" + testServer.getActualPort();
            System.out.println("Performance test WebSocket URL: " + WS_URL);
        }
    }
    
    @AfterAll
    static void stopServer() throws Exception {
        if (testServer != null) {
            testServer.stop();
        }
    }
    
    @BeforeEach
    void setUp() {
        httpClient = HttpClient.newHttpClient();
    }
    
    @Test
    @Order(1)
    @DisplayName("Should handle single client throughput")
    void testSingleClientThroughput() throws Exception {
        assumeTrue(testServer != null, "WebSocket server not available - skipping test");
        
        int messageCount = 1000;
        CountDownLatch responseLatch = new CountDownLatch(messageCount);
        AtomicLong totalResponseTime = new AtomicLong(0);
        AtomicInteger responseCount = new AtomicInteger(0);
        
        WebSocket webSocket = httpClient.newWebSocketBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .buildAsync(URI.create(WS_URL), new WebSocket.Listener() {
                private final ConcurrentHashMap<String, Long> sentTimes = new ConcurrentHashMap<>();
                
                @Override
                public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                    if (last) {
                        String response = data.toString();
                        // Calculate response time if we can match the message
                        Long sentTime = sentTimes.remove(response.hashCode() + "");
                        if (sentTime != null) {
                            long responseTime = System.nanoTime() - sentTime;
                            totalResponseTime.addAndGet(responseTime);
                            responseCount.incrementAndGet();
                        }
                        responseLatch.countDown();
                    }
                    return WebSocket.Listener.super.onText(webSocket, data, last);
                }
            })
            .get(5, TimeUnit.SECONDS);
        
        // Warm up
        for (int i = 0; i < WARM_UP_MESSAGES; i++) {
            webSocket.sendText("warmup_" + i, true);
            Thread.sleep(10);
        }
        Thread.sleep(1000); // Wait for warm-up to complete
        
        // Measure throughput
        long startTime = System.nanoTime();
        
        for (int i = 0; i < messageCount; i++) {
            String message = "getPlayerPos";
            webSocket.sendText(message, true);
        }
        
        // Wait for all responses
        boolean completed = responseLatch.await(30, TimeUnit.SECONDS);
        long endTime = System.nanoTime();
        
        assertTrue(completed, "Not all messages received responses");
        
        // Calculate metrics
        double totalTimeSeconds = (endTime - startTime) / 1_000_000_000.0;
        double messagesPerSecond = messageCount / totalTimeSeconds;
        double avgResponseTimeMs = responseCount.get() > 0 
            ? (totalResponseTime.get() / responseCount.get()) / 1_000_000.0 
            : 0.0;
        
        System.out.println("Single Client Performance:");
        System.out.println("- Messages sent: " + messageCount);
        System.out.println("- Total time: " + String.format("%.2f", totalTimeSeconds) + " seconds");
        System.out.println("- Throughput: " + String.format("%.2f", messagesPerSecond) + " msg/sec");
        System.out.println("- Avg response time: " + String.format("%.2f", avgResponseTimeMs) + " ms");
        
        // Performance assertions
        assertTrue(messagesPerSecond > 100, "Throughput should be > 100 msg/sec");
        assertTrue(avgResponseTimeMs < 50, "Average response time should be < 50ms");
        
        webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Test completed");
    }
    
    @Test
    @Order(2)
    @DisplayName("Should handle concurrent clients")
    void testConcurrentClients() throws Exception {
        assumeTrue(testServer != null, "WebSocket server not available - skipping test");
        
        int clientCount = 10;
        int messagesPerClient = 100;
        ExecutorService executor = Executors.newFixedThreadPool(clientCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(clientCount);
        AtomicInteger successfulConnections = new AtomicInteger(0);
        AtomicInteger totalMessages = new AtomicInteger(0);
        
        List<Future<ClientResult>> futures = new ArrayList<>();
        
        for (int i = 0; i < clientCount; i++) {
            final int clientId = i;
            futures.add(executor.submit(() -> {
                try {
                    WebSocket webSocket = httpClient.newWebSocketBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .buildAsync(URI.create(WS_URL), new WebSocket.Listener() {})
                        .get(5, TimeUnit.SECONDS);
                    
                    successfulConnections.incrementAndGet();
                    startLatch.await(); // Wait for all clients to connect
                    
                    // Send messages
                    for (int j = 0; j < messagesPerClient; j++) {
                        webSocket.sendText("client_" + clientId + "_msg_" + j, true);
                        totalMessages.incrementAndGet();
                        Thread.sleep(10); // Small delay to avoid overwhelming
                    }
                    
                    webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Client " + clientId + " completed");
                    return new ClientResult(clientId, messagesPerClient, true);
                    
                } catch (Exception e) {
                    return new ClientResult(clientId, 0, false);
                } finally {
                    completionLatch.countDown();
                }
            }));
        }
        
        // Wait for all clients to connect
        Thread.sleep(2000);
        
        // Start all clients sending messages
        long startTime = System.nanoTime();
        startLatch.countDown();
        
        // Wait for all clients to complete
        boolean completed = completionLatch.await(60, TimeUnit.SECONDS);
        long endTime = System.nanoTime();
        
        assertTrue(completed, "Not all clients completed");
        executor.shutdown();
        
        // Collect results
        int successfulClients = 0;
        for (Future<ClientResult> future : futures) {
            ClientResult result = future.get();
            if (result.success) {
                successfulClients++;
            }
        }
        
        // Calculate metrics
        double totalTimeSeconds = (endTime - startTime) / 1_000_000_000.0;
        double totalThroughput = totalMessages.get() / totalTimeSeconds;
        
        System.out.println("\nConcurrent Clients Performance:");
        System.out.println("- Clients: " + clientCount);
        System.out.println("- Successful connections: " + successfulConnections.get());
        System.out.println("- Total messages: " + totalMessages.get());
        System.out.println("- Total time: " + String.format("%.2f", totalTimeSeconds) + " seconds");
        System.out.println("- Total throughput: " + String.format("%.2f", totalThroughput) + " msg/sec");
        System.out.println("- Avg throughput per client: " + String.format("%.2f", totalThroughput / clientCount) + " msg/sec");
        
        // Assertions
        assertEquals(clientCount, successfulConnections.get(), "All clients should connect");
        assertEquals(clientCount, successfulClients, "All clients should complete successfully");
        assertTrue(totalThroughput > 500, "Total throughput should be > 500 msg/sec");
    }
    
    @Test
    @Order(3)
    @DisplayName("Should maintain performance under load")
    @Timeout(value = 2, unit = TimeUnit.MINUTES)
    void testSustainedLoad() throws Exception {
        assumeTrue(testServer != null, "WebSocket server not available - skipping test");
        
        int duration = 30; // seconds
        int clientCount = 5;
        int messagesPerSecondPerClient = 10;
        
        ExecutorService executor = Executors.newFixedThreadPool(clientCount);
        CountDownLatch stopLatch = new CountDownLatch(clientCount);
        AtomicBoolean running = new AtomicBoolean(true);
        AtomicInteger totalMessagesSent = new AtomicInteger(0);
        AtomicInteger errors = new AtomicInteger(0);
        
        List<Future<Void>> futures = new ArrayList<>();
        
        for (int i = 0; i < clientCount; i++) {
            final int clientId = i;
            futures.add(executor.submit(() -> {
                try {
                    WebSocket webSocket = httpClient.newWebSocketBuilder()
                        .buildAsync(URI.create(WS_URL), new WebSocket.Listener() {
                            @Override
                            public void onError(WebSocket webSocket, Throwable error) {
                                errors.incrementAndGet();
                            }
                        })
                        .get(5, TimeUnit.SECONDS);
                    
                    while (running.get()) {
                        for (int j = 0; j < messagesPerSecondPerClient; j++) {
                            webSocket.sendText("load_test_" + clientId + "_" + j, true);
                            totalMessagesSent.incrementAndGet();
                        }
                        Thread.sleep(1000); // Send batch every second
                    }
                    
                    webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Load test completed");
                } catch (Exception e) {
                    errors.incrementAndGet();
                } finally {
                    stopLatch.countDown();
                }
                return null;
            }));
        }
        
        // Run load test
        Thread.sleep(duration * 1000);
        running.set(false);
        
        // Wait for all clients to stop
        boolean completed = stopLatch.await(10, TimeUnit.SECONDS);
        assertTrue(completed, "All clients should stop");
        executor.shutdown();
        
        // Calculate metrics
        double messagesPerSecond = (double) totalMessagesSent.get() / duration;
        double errorRate = (double) errors.get() / totalMessagesSent.get() * 100;
        
        System.out.println("\nSustained Load Test Results:");
        System.out.println("- Duration: " + duration + " seconds");
        System.out.println("- Clients: " + clientCount);
        System.out.println("- Total messages sent: " + totalMessagesSent.get());
        System.out.println("- Messages per second: " + String.format("%.2f", messagesPerSecond));
        System.out.println("- Errors: " + errors.get());
        System.out.println("- Error rate: " + String.format("%.2f", errorRate) + "%");
        
        // Assertions
        assertTrue(errorRate < 1.0, "Error rate should be < 1%");
        assertTrue(messagesPerSecond > clientCount * messagesPerSecondPerClient * 0.9, 
            "Should maintain at least 90% of target throughput");
    }
    
    /**
     * Helper class to store client results
     */
    private static class ClientResult {
        final int clientId;
        final int messagesSent;
        final boolean success;
        
        ClientResult(int clientId, int messagesSent, boolean success) {
            this.clientId = clientId;
            this.messagesSent = messagesSent;
            this.success = success;
        }
    }
}
package com.yourname.minecraftcollaboration.integration;

import com.yourname.minecraftcollaboration.network.WebSocketHandler;
import com.yourname.minecraftcollaboration.network.CollaborationMessageProcessor;
import org.junit.jupiter.api.*;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Integration tests for WebSocket communication
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class WebSocketIntegrationTest {
    
    private static final int TEST_PORT = 14799; // Different port for testing
    private WebSocketHandler server;
    private TestWebSocketClient client;
    
    @BeforeAll
    public void setupServer() throws Exception {
        // Start test WebSocket server
        CollaborationMessageProcessor processor = new CollaborationMessageProcessor();
        server = new WebSocketHandler(TEST_PORT, processor);
        server.start();
        
        // Wait for server to start
        Thread.sleep(1000);
    }
    
    @AfterAll
    public void teardownServer() {
        if (server != null) {
            server.stopServer();
        }
    }
    
    @BeforeEach
    public void setupClient() throws URISyntaxException, InterruptedException {
        client = new TestWebSocketClient(new URI("ws://localhost:" + TEST_PORT));
        client.connectBlocking();
    }
    
    @AfterEach
    public void teardownClient() {
        if (client != null && client.isOpen()) {
            client.closeBlocking();
        }
    }
    
    @Test
    public void testConnection() throws InterruptedException {
        // Wait for welcome message
        assertTrue(client.waitForMessage(2, TimeUnit.SECONDS));
        String welcomeMessage = client.getLastMessage();
        
        assertNotNull(welcomeMessage);
        assertTrue(welcomeMessage.contains("welcome"));
    }
    
    @Test
    public void testSimpleCommand() throws InterruptedException {
        // Wait for welcome message
        client.waitForMessage(2, TimeUnit.SECONDS);
        
        // Send a simple command
        client.send("{\"command\":\"connect\"}");
        
        // Wait for response
        assertTrue(client.waitForMessage(2, TimeUnit.SECONDS));
        String response = client.getLastMessage();
        
        assertNotNull(response);
        assertTrue(response.contains("success") || response.contains("connected"));
    }
    
    @Test
    public void testRateLimiting() throws InterruptedException {
        // Wait for welcome message
        client.waitForMessage(2, TimeUnit.SECONDS);
        
        // Send many commands quickly
        for (int i = 0; i < 15; i++) {
            client.send("{\"command\":\"getPlayerPos\"}");
        }
        
        // Wait for rate limit response
        Thread.sleep(500);
        
        // Check that we got a rate limit error
        boolean foundRateLimit = false;
        for (String message : client.getAllMessages()) {
            if (message.contains("rateLimitExceeded")) {
                foundRateLimit = true;
                break;
            }
        }
        
        assertTrue(foundRateLimit, "Expected rate limit error but didn't receive one");
    }
    
    @Test
    public void testInvalidCommand() throws InterruptedException {
        // Wait for welcome message
        client.waitForMessage(2, TimeUnit.SECONDS);
        
        // Send invalid command
        client.send("{\"command\":\"thisDoesNotExist\"}");
        
        // Wait for error response
        assertTrue(client.waitForMessage(2, TimeUnit.SECONDS));
        String response = client.getLastMessage();
        
        assertNotNull(response);
        assertTrue(response.contains("unknownCommand") || response.contains("error"));
    }
    
    @Test
    public void testMalformedJSON() throws InterruptedException {
        // Wait for welcome message
        client.waitForMessage(2, TimeUnit.SECONDS);
        
        // Send malformed JSON
        client.send("{invalid json}");
        
        // Should still get a response (error)
        assertTrue(client.waitForMessage(2, TimeUnit.SECONDS));
        String response = client.getLastMessage();
        
        assertNotNull(response);
        // Legacy format commands are supported, so this might not error
    }
    
    // Test WebSocket client implementation
    private static class TestWebSocketClient extends WebSocketClient {
        private final CountDownLatch messageLatch = new CountDownLatch(1);
        private final AtomicReference<String> lastMessage = new AtomicReference<>();
        private final java.util.List<String> allMessages = new java.util.ArrayList<>();
        
        public TestWebSocketClient(URI serverUri) {
            super(serverUri);
        }
        
        @Override
        public void onOpen(ServerHandshake handshakedata) {
            // Connection opened
        }
        
        @Override
        public void onMessage(String message) {
            allMessages.add(message);
            lastMessage.set(message);
            messageLatch.countDown();
        }
        
        @Override
        public void onClose(int code, String reason, boolean remote) {
            // Connection closed
        }
        
        @Override
        public void onError(Exception ex) {
            ex.printStackTrace();
        }
        
        public boolean waitForMessage(long timeout, TimeUnit unit) throws InterruptedException {
            return messageLatch.await(timeout, unit);
        }
        
        public String getLastMessage() {
            return lastMessage.get();
        }
        
        public java.util.List<String> getAllMessages() {
            return new java.util.ArrayList<>(allMessages);
        }
    }
}
package edu.minecraft.collaboration.integration;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import edu.minecraft.collaboration.network.WebSocketHandler;
import edu.minecraft.collaboration.server.CollaborationServer;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.jupiter.api.*;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end tests for WebSocket communication
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class WebSocketE2ETest {
    
    private static final int TEST_PORT = 14799; // Use different port for testing
    private static final String WS_URL = "ws://localhost:" + TEST_PORT;
    private static final int TIMEOUT_SECONDS = 10;
    
    private CollaborationServer server;
    private TestWebSocketClient client;
    private final Gson gson = new Gson();
    
    @BeforeEach
    public void setup() throws Exception {
        // Start test server
        server = new CollaborationServer(TEST_PORT, TEST_PORT + 1, null);
        server.start();
        
        // Wait for server to start
        Thread.sleep(1000);
    }
    
    @AfterEach
    public void teardown() throws Exception {
        if (client != null && client.isOpen()) {
            client.closeBlocking();
        }
        
        if (server != null) {
            server.stop();
        }
    }
    
    @Test
    @DisplayName("Test WebSocket connection")
    public void testConnection() throws Exception {
        CountDownLatch connectLatch = new CountDownLatch(1);
        AtomicReference<String> welcomeMessage = new AtomicReference<>();
        
        client = new TestWebSocketClient(new URI(WS_URL)) {
            @Override
            public void onOpen(ServerHandshake handshake) {
                super.onOpen(handshake);
                connectLatch.countDown();
            }
            
            @Override
            public void onMessage(String message) {
                super.onMessage(message);
                if (welcomeMessage.get() == null) {
                    welcomeMessage.set(message);
                }
            }
        };
        
        client.connectBlocking();
        assertTrue(connectLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS), "Should connect within timeout");
        assertTrue(client.isOpen(), "Client should be connected");
        
        // Wait for welcome message
        Thread.sleep(500);
        assertNotNull(welcomeMessage.get(), "Should receive welcome message");
        
        // Parse welcome message
        JsonObject welcome = gson.fromJson(welcomeMessage.get(), JsonObject.class);
        assertEquals("welcome", welcome.get("type").getAsString());
    }
    
    @Test
    @DisplayName("Test player position request")
    public void testGetPlayerPosition() throws Exception {
        connectClient();
        
        CountDownLatch responseLatch = new CountDownLatch(1);
        AtomicReference<JsonObject> response = new AtomicReference<>();
        
        client.setMessageHandler(message -> {
            JsonObject json = gson.fromJson(message, JsonObject.class);
            if ("playerPos".equals(json.get("type").getAsString())) {
                response.set(json);
                responseLatch.countDown();
            }
        });
        
        // Send get player position command
        JsonObject command = new JsonObject();
        command.addProperty("command", "getPlayerPos");
        command.add("args", new JsonObject());
        
        client.send(gson.toJson(command));
        
        assertTrue(responseLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS), "Should receive response within timeout");
        
        JsonObject posResponse = response.get();
        assertNotNull(posResponse);
        assertEquals("playerPos", posResponse.get("type").getAsString());
        assertTrue(posResponse.has("data"));
    }
    
    @Test
    @DisplayName("Test block placement command")
    public void testPlaceBlock() throws Exception {
        connectClient();
        
        CountDownLatch responseLatch = new CountDownLatch(1);
        AtomicReference<JsonObject> response = new AtomicReference<>();
        
        client.setMessageHandler(message -> {
            JsonObject json = gson.fromJson(message, JsonObject.class);
            if ("placeBlock".equals(json.get("type").getAsString()) || 
                "error".equals(json.get("type").getAsString())) {
                response.set(json);
                responseLatch.countDown();
            }
        });
        
        // Send place block command
        JsonObject command = new JsonObject();
        command.addProperty("command", "placeBlock");
        
        JsonObject args = new JsonObject();
        args.addProperty("x", "10");
        args.addProperty("y", "64");
        args.addProperty("z", "10");
        args.addProperty("block", "stone");
        command.add("args", args);
        
        client.send(gson.toJson(command));
        
        assertTrue(responseLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS), "Should receive response within timeout");
        
        JsonObject blockResponse = response.get();
        assertNotNull(blockResponse);
        // Should receive either success or error (no players online)
        assertTrue("placeBlock".equals(blockResponse.get("type").getAsString()) || 
                  "error".equals(blockResponse.get("type").getAsString()));
    }
    
    @Test
    @DisplayName("Test rate limiting")
    public void testRateLimiting() throws Exception {
        connectClient();
        
        CountDownLatch rateLimitLatch = new CountDownLatch(1);
        AtomicReference<JsonObject> rateLimitResponse = new AtomicReference<>();
        
        client.setMessageHandler(message -> {
            JsonObject json = gson.fromJson(message, JsonObject.class);
            if ("error".equals(json.get("type").getAsString()) && 
                "rateLimitExceeded".equals(json.get("error").getAsString())) {
                rateLimitResponse.set(json);
                rateLimitLatch.countDown();
            }
        });
        
        // Send many commands quickly to trigger rate limit
        JsonObject command = new JsonObject();
        command.addProperty("command", "getPlayerPos");
        command.add("args", new JsonObject());
        String commandStr = gson.toJson(command);
        
        for (int i = 0; i < 15; i++) { // Send more than rate limit (10/sec)
            client.send(commandStr);
        }
        
        assertTrue(rateLimitLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS), "Should hit rate limit within timeout");
        
        JsonObject errorResponse = rateLimitResponse.get();
        assertNotNull(errorResponse);
        assertEquals("error", errorResponse.get("type").getAsString());
        assertEquals("rateLimitExceeded", errorResponse.get("error").getAsString());
    }
    
    @Test
    @DisplayName("Test collaboration invitation command")
    public void testCollaborationInvitation() throws Exception {
        connectClient();
        
        CountDownLatch responseLatch = new CountDownLatch(1);
        AtomicReference<String> response = new AtomicReference<>();
        
        client.setMessageHandler(message -> {
            response.set(message);
            responseLatch.countDown();
        });
        
        // Send invitation using legacy format
        client.send("collaboration.invite(TestFriend)");
        
        assertTrue(responseLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS), "Should receive response within timeout");
        
        String inviteResponse = response.get();
        assertNotNull(inviteResponse);
        // Response could be error (no players) or success
        assertTrue(inviteResponse.contains("error") || inviteResponse.contains("success"));
    }
    
    @Test
    @DisplayName("Test invalid JSON handling")
    public void testInvalidJson() throws Exception {
        connectClient();
        
        CountDownLatch responseLatch = new CountDownLatch(1);
        AtomicReference<JsonObject> response = new AtomicReference<>();
        
        client.setMessageHandler(message -> {
            try {
                JsonObject json = gson.fromJson(message, JsonObject.class);
                if ("error".equals(json.get("type").getAsString())) {
                    response.set(json);
                    responseLatch.countDown();
                }
            } catch (Exception e) {
                // Ignore parse errors
            }
        });
        
        // Send invalid JSON
        client.send("{invalid json}");
        
        assertTrue(responseLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS), "Should receive error response within timeout");
        
        JsonObject errorResponse = response.get();
        assertNotNull(errorResponse);
        assertEquals("error", errorResponse.get("type").getAsString());
    }
    
    @Test
    @DisplayName("Test concurrent connections")
    public void testConcurrentConnections() throws Exception {
        int numClients = 5;
        TestWebSocketClient[] clients = new TestWebSocketClient[numClients];
        CountDownLatch connectLatch = new CountDownLatch(numClients);
        
        try {
            // Connect multiple clients
            for (int i = 0; i < numClients; i++) {
                clients[i] = new TestWebSocketClient(new URI(WS_URL)) {
                    @Override
                    public void onOpen(ServerHandshake handshake) {
                        super.onOpen(handshake);
                        connectLatch.countDown();
                    }
                };
                clients[i].connect();
            }
            
            assertTrue(connectLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS), 
                "All clients should connect within timeout");
            
            // Verify all clients are connected
            for (TestWebSocketClient c : clients) {
                assertTrue(c.isOpen(), "Client should be connected");
            }
            
        } finally {
            // Clean up all clients
            for (TestWebSocketClient c : clients) {
                if (c != null && c.isOpen()) {
                    c.closeBlocking();
                }
            }
        }
    }
    
    // Helper methods
    
    private void connectClient() throws Exception {
        CountDownLatch connectLatch = new CountDownLatch(1);
        
        client = new TestWebSocketClient(new URI(WS_URL)) {
            @Override
            public void onOpen(ServerHandshake handshake) {
                super.onOpen(handshake);
                connectLatch.countDown();
            }
        };
        
        client.connectBlocking();
        assertTrue(connectLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS), "Should connect within timeout");
        
        // Wait for welcome message
        Thread.sleep(500);
    }
    
    // Test WebSocket client implementation
    
    private static class TestWebSocketClient extends WebSocketClient {
        private MessageHandler messageHandler;
        
        public TestWebSocketClient(URI serverUri) {
            super(serverUri);
        }
        
        @Override
        public void onOpen(ServerHandshake handshake) {
            System.out.println("Connected to server");
        }
        
        @Override
        public void onMessage(String message) {
            System.out.println("Received: " + message);
            if (messageHandler != null) {
                messageHandler.handle(message);
            }
        }
        
        @Override
        public void onClose(int code, String reason, boolean remote) {
            System.out.println("Connection closed: " + reason);
        }
        
        @Override
        public void onError(Exception ex) {
            ex.printStackTrace();
        }
        
        public void setMessageHandler(MessageHandler handler) {
            this.messageHandler = handler;
        }
        
        @FunctionalInterface
        interface MessageHandler {
            void handle(String message);
        }
    }
}
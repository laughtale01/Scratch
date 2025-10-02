package edu.minecraft.collaboration.integration;

import edu.minecraft.collaboration.test.MinecraftTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.DisplayName;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for collaboration features
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Collaboration Feature Integration Tests")
@org.junit.jupiter.api.Tag("minecraft-dependent")
@org.junit.jupiter.api.Disabled("Requires Minecraft runtime - run in integration environment")
public class CollaborationFeatureIntegrationTest extends MinecraftTestBase {
    
    private WebSocket webSocket;
    private HttpClient httpClient;
    private static edu.minecraft.collaboration.test.util.WebSocketTestServer testServer;
    private static String wsUrl;
    
    @BeforeAll
    static void startServer() throws Exception {
        // Start test WebSocket server
        testServer = edu.minecraft.collaboration.test.util.WebSocketTestServer.startTestServer(14711);
        if (testServer == null) {
            System.out.println("WebSocket server could not start - integration tests will be skipped");
        } else {
            wsUrl = "ws://localhost:" + testServer.getActualPort();
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
    
    @AfterEach
    void tearDown() {
        if (webSocket != null && !webSocket.isOutputClosed()) {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Test completed");
        }
    }
    
    @Test
    @Order(1)
    @DisplayName("Should create and accept invitation")
    void testInvitationFlow() throws Exception {
        if (testServer == null) {
            return; // Skip if server not available
        }
        // Given
        // wsUrl is already set in @BeforeAll
        CompletableFuture<String> responseFuture = new CompletableFuture<>();
        
        WebSocket.Listener listener = createMessageListener(responseFuture);
        webSocket = httpClient.newWebSocketBuilder()
            .buildAsync(URI.create(wsUrl), listener)
            .get(5, TimeUnit.SECONDS);
        
        // When - Create invitation
        String createInviteCmd = "{\"type\":\"command\",\"action\":\"createInvitation\"," +
            "\"sender\":\"Player1\",\"recipient\":\"Player2\"}";
        webSocket.sendText(createInviteCmd, true);
        
        String response = responseFuture.get(5, TimeUnit.SECONDS);
        
        // Then
        assertNotNull(response);
        assertTrue(response.contains("success") || response.contains("created"));
    }
    
    @Test
    @Order(2)
    @DisplayName("Should handle visit request workflow")
    void testVisitRequestFlow() throws Exception {
        if (testServer == null) {
            return; // Skip if server not available
        }
        // Given
        // wsUrl is already set in @BeforeAll
        CompletableFuture<String> responseFuture = new CompletableFuture<>();
        
        WebSocket.Listener listener = createMessageListener(responseFuture);
        webSocket = httpClient.newWebSocketBuilder()
            .buildAsync(URI.create(wsUrl), listener)
            .get(5, TimeUnit.SECONDS);
        
        // When - Create visit request
        String visitRequestCmd = "{\"type\":\"command\",\"action\":\"requestVisit\"," +
            "\"visitor\":\"Visitor1\",\"host\":\"Host1\"}";
        webSocket.sendText(visitRequestCmd, true);
        
        String response = responseFuture.get(5, TimeUnit.SECONDS);
        
        // Then
        assertNotNull(response);
        assertTrue(response.contains("request") || response.contains("pending"));
    }
    
    @Test
    @Order(3)
    @DisplayName("Should execute building commands")
    void testBuildingCommands() throws Exception {
        if (testServer == null) {
            return; // Skip if server not available
        }
        // Given
        // wsUrl is already set in @BeforeAll
        CompletableFuture<String> responseFuture = new CompletableFuture<>();
        
        WebSocket.Listener listener = createMessageListener(responseFuture);
        webSocket = httpClient.newWebSocketBuilder()
            .buildAsync(URI.create(wsUrl), listener)
            .get(5, TimeUnit.SECONDS);
        
        // When - Build a circle
        String buildCircleCmd = "{\"type\":\"command\",\"action\":\"buildCircle\"," +
            "\"x\":0,\"y\":64,\"z\":0,\"radius\":5,\"block\":\"stone\"}";
        webSocket.sendText(buildCircleCmd, true);
        
        String response = responseFuture.get(10, TimeUnit.SECONDS);
        
        // Then
        assertNotNull(response);
        assertTrue(response.contains("success") || response.contains("built"));
    }
    
    @Test
    @Order(4)
    @DisplayName("Should handle chat messages")
    void testChatFunctionality() throws Exception {
        if (testServer == null) {
            return; // Skip if server not available
        }
        // Given
        // wsUrl is already set in @BeforeAll
        CompletableFuture<String> responseFuture = new CompletableFuture<>();
        
        WebSocket.Listener listener = createMessageListener(responseFuture);
        webSocket = httpClient.newWebSocketBuilder()
            .buildAsync(URI.create(wsUrl), listener)
            .get(5, TimeUnit.SECONDS);
        
        // When - Send chat message
        String chatCmd = "{\"type\":\"command\",\"action\":\"chat\"," +
            "\"message\":\"Hello from integration test!\"}";
        webSocket.sendText(chatCmd, true);
        
        String response = responseFuture.get(5, TimeUnit.SECONDS);
        
        // Then
        assertNotNull(response);
        assertTrue(response.contains("success") || response.contains("sent"));
    }
    
    @Test
    @Order(5)
    @DisplayName("Should get player position")
    void testGetPlayerPosition() throws Exception {
        if (testServer == null) {
            return; // Skip if server not available
        }
        // Given
        // wsUrl is already set in @BeforeAll
        CompletableFuture<String> responseFuture = new CompletableFuture<>();
        
        WebSocket.Listener listener = createMessageListener(responseFuture);
        webSocket = httpClient.newWebSocketBuilder()
            .buildAsync(URI.create(wsUrl), listener)
            .get(5, TimeUnit.SECONDS);
        
        // When - Get position
        String getPosCmd = "{\"type\":\"query\",\"action\":\"getPlayerPos\"}";
        webSocket.sendText(getPosCmd, true);
        
        String response = responseFuture.get(5, TimeUnit.SECONDS);
        
        // Then
        assertNotNull(response);
        assertTrue(response.contains("x") || response.contains("position"));
    }
    
    /**
     * Create a WebSocket listener that captures the first complete message
     */
    private WebSocket.Listener createMessageListener(CompletableFuture<String> future) {
        return new WebSocket.Listener() {
            private StringBuilder messageBuilder = new StringBuilder();
            
            @Override
            public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                messageBuilder.append(data);
                if (last) {
                    future.complete(messageBuilder.toString());
                    messageBuilder = new StringBuilder();
                }
                return WebSocket.Listener.super.onText(webSocket, data, last);
            }
            
            @Override
            public void onError(WebSocket webSocket, Throwable error) {
                future.completeExceptionally(error);
            }
        };
    }
}
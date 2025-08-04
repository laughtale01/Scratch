package edu.minecraft.collaboration.test.util;

/**
 * Starts the test WebSocket server
 */
public class StartTestServer {
    public static void main(String[] args) throws Exception {
        System.out.println("Starting test WebSocket server on port 14711...");
        WebSocketTestServer server = WebSocketTestServer.startTestServer(14711);
        System.out.println("Test server started. Press Ctrl+C to stop.");
        
        // Keep the server running
        Thread.currentThread().join();
    }
}
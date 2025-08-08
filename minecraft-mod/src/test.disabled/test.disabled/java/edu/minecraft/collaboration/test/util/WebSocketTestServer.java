package edu.minecraft.collaboration.test.util;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Test WebSocket server for integration tests
 */
public class WebSocketTestServer extends WebSocketServer {
    
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final CountDownLatch startLatch = new CountDownLatch(1);
    private int actualPort;
    
    public WebSocketTestServer(int port) {
        super(new InetSocketAddress(port));
        this.actualPort = port;
    }
    
    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("Test server: New connection from " + conn.getRemoteSocketAddress());
    }
    
    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("Test server: Connection closed");
    }
    
    @Override
    public void onMessage(WebSocket conn, String message) {
        // Echo back for simple tests
        if (message.equals("getPlayerPos")) {
            conn.send("{\"status\":\"success\",\"type\":\"position\",\"x\":100,\"y\":64,\"z\":200}");
        } else if (message.startsWith("warmup_")) {
            // Ignore warmup messages
        } else if (message.startsWith("client_") || message.startsWith("load_test_")) {
            // Echo back for performance tests
            conn.send("{\"echo\":\"" + message + "\"}");
        } else if (message.contains("\"type\":\"command\"") || message.contains("\"action\":")) {
            // Handle collaboration commands
            if (message.contains("createInvitation")) {
                conn.send("{\"status\":\"success\",\"message\":\"Invitation created\"}");
            } else if (message.contains("requestVisit")) {
                conn.send("{\"status\":\"success\",\"message\":\"Visit request pending\"}");
            } else if (message.contains("buildCircle")) {
                conn.send("{\"status\":\"success\",\"message\":\"Circle built\"}");
            } else if (message.contains("chat")) {
                conn.send("{\"status\":\"success\",\"message\":\"Message sent\"}");
            } else if (message.contains("getPlayerPos")) {
                conn.send("{\"status\":\"success\",\"type\":\"position\",\"x\":100,\"y\":64,\"z\":200}");
            } else {
                conn.send("{\"status\":\"error\",\"message\":\"Unknown command\"}");
            }
        } else if (message.equals("invalidCommand")) {
            conn.send("{\"status\":\"error\",\"message\":\"unknown command\"}");
        } else {
            conn.send("{\"status\":\"success\",\"response\":\"" + message + "\"}");
        }
    }
    
    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("Test server error: " + ex.getMessage());
    }
    
    @Override
    public void onStart() {
        actualPort = getPort();
        System.out.println("Test WebSocket server started on port " + actualPort);
        started.set(true);
        startLatch.countDown();
    }
    
    public boolean waitForStart(long timeout, TimeUnit unit) throws InterruptedException {
        return startLatch.await(timeout, unit);
    }
    
    public boolean isStarted() {
        return started.get();
    }
    
    public int getActualPort() {
        return actualPort;
    }
    
    public static WebSocketTestServer startTestServer(int port) throws Exception {
        try {
            // Try to use an alternative port if the default is in use
            WebSocketTestServer server = null;
            Exception lastException = null;
            
            for (int portOffset = 0; portOffset < 10; portOffset++) {
                try {
                    int testPort = port + portOffset;
                    server = new WebSocketTestServer(testPort);
                    server.setReuseAddr(true); // Allow port reuse
                    server.start();
                    if (server.waitForStart(2, TimeUnit.SECONDS)) {
                        System.out.println("Test server started on port " + server.getActualPort());
                        return server;
                    }
                } catch (Exception e) {
                    lastException = e;
                    if (server != null) {
                        try {
                            server.stop();
                        } catch (Exception stopEx) {
                            // Ignore
                        }
                    }
                }
            }
            
            // Log and return null for tests that can skip server
            System.err.println("Test server start failed after trying multiple ports: " + 
                (lastException != null ? lastException.getMessage() : "Unknown error"));
            return null;
        } catch (Exception e) {
            // Log and return null for tests that can skip server
            System.err.println("Test server start failed: " + e.getMessage());
            return null;
        }
    }
}
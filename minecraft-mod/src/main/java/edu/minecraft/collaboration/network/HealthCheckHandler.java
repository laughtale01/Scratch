package edu.minecraft.collaboration.network;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * HTTP health check endpoint for monitoring
 */
public final class HealthCheckHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(HealthCheckHandler.class);
    private static HttpServer healthServer;
    private static final int HEALTH_PORT = 14711;

    // Private constructor to prevent instantiation
    private HealthCheckHandler() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Start the health check HTTP server
     */
    public static void startHealthCheck() {
        try {
            healthServer = HttpServer.create(new InetSocketAddress(HEALTH_PORT), 0);
            healthServer.createContext("/health", new HealthHandler());
            healthServer.createContext("/status", new StatusHandler());
            healthServer.createContext("/metrics", new MetricsHandler());
            healthServer.setExecutor(Executors.newSingleThreadExecutor());
            healthServer.start();

            LOGGER.info("Health check server started on port {}", HEALTH_PORT);
        } catch (IOException e) {
            LOGGER.error("Failed to start health check server", e);
        }
    }

    /**
     * Stop the health check server
     */
    public static void stopHealthCheck() {
        if (healthServer != null) {
            healthServer.stop(0);
            LOGGER.info("Health check server stopped");
        }
    }

    /**
     * Basic health check handler
     */
    static class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            JsonObject response = new JsonObject();
            response.addProperty("status", "ok");
            response.addProperty("timestamp", System.currentTimeMillis());

            String responseString = response.toString();
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseString.length());

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseString.getBytes());
            }
        }
    }

    /**
     * Detailed status handler
     */
    static class StatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            JsonObject response = new JsonObject();
            response.addProperty("status", "running");
            response.addProperty("version", "1.0.0");
            response.addProperty("uptime", getUptime());
            response.addProperty("connections", WebSocketHandler.getConnectionCount());
            response.addProperty("timestamp", System.currentTimeMillis());

            // Memory information
            JsonObject memory = new JsonObject();
            Runtime runtime = Runtime.getRuntime();
            memory.addProperty("total", runtime.totalMemory());
            memory.addProperty("free", runtime.freeMemory());
            memory.addProperty("used", runtime.totalMemory() - runtime.freeMemory());
            memory.addProperty("max", runtime.maxMemory());
            response.add("memory", memory);

            String responseString = response.toString();
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseString.length());

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseString.getBytes());
            }
        }

        private long getUptime() {
            return System.currentTimeMillis() - WebSocketHandler.getStartTime();
        }
    }

    /**
     * Metrics handler for monitoring
     */
    static class MetricsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            JsonObject response = new JsonObject();

            // WebSocket metrics
            JsonObject websocket = new JsonObject();
            websocket.addProperty("connections", WebSocketHandler.getConnectionCount());
            websocket.addProperty("total_messages", WebSocketHandler.getTotalMessages());
            websocket.addProperty("errors", WebSocketHandler.getErrorCount());
            response.add("websocket", websocket);

            // Command metrics
            JsonObject commands = new JsonObject();
            commands.addProperty("total", WebSocketHandler.getTotalCommands());
            commands.addProperty("successful", WebSocketHandler.getSuccessfulCommands());
            commands.addProperty("failed", WebSocketHandler.getFailedCommands());
            response.add("commands", commands);

            // System metrics
            JsonObject system = new JsonObject();
            system.addProperty("threads", Thread.activeCount());
            system.addProperty("processors", Runtime.getRuntime().availableProcessors());
            response.add("system", system);

            String responseString = response.toString();
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseString.length());

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseString.getBytes());
            }
        }
    }
}

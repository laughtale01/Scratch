package com.github.minecraftedu.network;

import com.github.minecraftedu.MinecraftEduMod;
import net.minecraft.server.MinecraftServer;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimpleWebSocketServer {
    private static final String WEBSOCKET_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    private final int port;
    private final MinecraftServer minecraftServer;
    private ServerSocket serverSocket;
    private ExecutorService executor;
    private volatile boolean running = false;

    public SimpleWebSocketServer(int port, MinecraftServer minecraftServer) {
        this.port = port;
        this.minecraftServer = minecraftServer;
        this.executor = Executors.newCachedThreadPool();
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        MinecraftEduMod.LOGGER.info("WebSocket server started on port " + port);

        // Accept connections in a separate thread
        executor.submit(() -> {
            while (running) {
                try {
                    Socket client = serverSocket.accept();
                    MinecraftEduMod.LOGGER.info("Client connected: " + client.getRemoteSocketAddress());
                    executor.submit(() -> handleClient(client));
                } catch (IOException e) {
                    if (running) {
                        MinecraftEduMod.LOGGER.error("Error accepting client", e);
                    }
                }
            }
        });
    }

    private void handleClient(Socket client) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
             OutputStream out = client.getOutputStream()) {

            // Read HTTP headers
            String line;
            StringBuilder headers = new StringBuilder();
            String webSocketKey = null;

            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                headers.append(line).append("\r\n");
                if (line.startsWith("Sec-WebSocket-Key:")) {
                    webSocketKey = line.substring("Sec-WebSocket-Key:".length()).trim();
                }
            }

            if (webSocketKey == null) {
                MinecraftEduMod.LOGGER.warn("No WebSocket key found in headers");
                client.close();
                return;
            }

            // Perform WebSocket handshake
            String acceptKey = generateAcceptKey(webSocketKey);
            String response = "HTTP/1.1 101 Switching Protocols\r\n" +
                    "Upgrade: websocket\r\n" +
                    "Connection: Upgrade\r\n" +
                    "Sec-WebSocket-Accept: " + acceptKey + "\r\n" +
                    "\r\n";

            out.write(response.getBytes(StandardCharsets.UTF_8));
            out.flush();

            MinecraftEduMod.LOGGER.info("WebSocket handshake completed");

            // Handle WebSocket frames
            InputStream in = client.getInputStream();
            while (running && !client.isClosed()) {
                // Read WebSocket frame
                int firstByte = in.read();
                if (firstByte == -1) break;

                boolean fin = (firstByte & 0x80) != 0;
                int opcode = firstByte & 0x0F;

                int secondByte = in.read();
                if (secondByte == -1) break;

                boolean masked = (secondByte & 0x80) != 0;
                int payloadLength = secondByte & 0x7F;

                // Extended payload length
                if (payloadLength == 126) {
                    payloadLength = (in.read() << 8) | in.read();
                } else if (payloadLength == 127) {
                    payloadLength = 0;
                    for (int i = 0; i < 8; i++) {
                        payloadLength = (payloadLength << 8) | in.read();
                    }
                }

                // Masking key
                byte[] maskingKey = new byte[4];
                if (masked) {
                    in.read(maskingKey);
                }

                // Payload data
                byte[] payload = new byte[payloadLength];
                in.read(payload);

                if (masked) {
                    for (int i = 0; i < payload.length; i++) {
                        payload[i] = (byte) (payload[i] ^ maskingKey[i % 4]);
                    }
                }

                // Handle message based on opcode
                if (opcode == 0x1) { // Text frame
                    String message = new String(payload, StandardCharsets.UTF_8);
                    MinecraftEduMod.LOGGER.info("Received WebSocket message: " + message);
                    handleWebSocketMessage(message, out);
                } else if (opcode == 0x8) { // Close frame
                    MinecraftEduMod.LOGGER.info("Client requested close");
                    break;
                } else if (opcode == 0x9) { // Ping frame
                    // Send pong
                    sendPong(out, payload);
                }
            }

        } catch (Exception e) {
            MinecraftEduMod.LOGGER.error("Error handling client", e);
        } finally {
            try {
                client.close();
                MinecraftEduMod.LOGGER.info("Client disconnected");
            } catch (IOException e) {
                MinecraftEduMod.LOGGER.error("Error closing client", e);
            }
        }
    }

    private void handleWebSocketMessage(String message, OutputStream out) {
        try {
            // Parse JSON and delegate to handler
            MinecraftWebSocketHandler handler = new MinecraftWebSocketHandler(minecraftServer);
            String response = handler.handleMessage(message);

            if (response != null) {
                sendTextFrame(out, response);
            }
        } catch (Exception e) {
            MinecraftEduMod.LOGGER.error("Error handling message", e);
        }
    }

    private void sendTextFrame(OutputStream out, String text) throws IOException {
        byte[] payload = text.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream frame = new ByteArrayOutputStream();

        // First byte: FIN + opcode (0x1 for text)
        frame.write(0x81);

        // Second byte: payload length (no mask for server-to-client)
        if (payload.length < 126) {
            frame.write(payload.length);
        } else if (payload.length < 65536) {
            frame.write(126);
            frame.write((payload.length >> 8) & 0xFF);
            frame.write(payload.length & 0xFF);
        } else {
            frame.write(127);
            for (int i = 7; i >= 0; i--) {
                frame.write((int) ((payload.length >> (i * 8)) & 0xFF));
            }
        }

        // Payload
        frame.write(payload);

        out.write(frame.toByteArray());
        out.flush();
    }

    private void sendPong(OutputStream out, byte[] payload) throws IOException {
        ByteArrayOutputStream frame = new ByteArrayOutputStream();
        frame.write(0x8A); // FIN + opcode 0xA (pong)
        frame.write(payload.length);
        frame.write(payload);
        out.write(frame.toByteArray());
        out.flush();
    }

    private String generateAcceptKey(String webSocketKey) {
        try {
            String combined = webSocketKey + WEBSOCKET_GUID;
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] hash = md.digest(combined.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            MinecraftEduMod.LOGGER.error("Error generating accept key", e);
            return "";
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            if (executor != null) {
                executor.shutdownNow();
            }
            MinecraftEduMod.LOGGER.info("WebSocket server stopped");
        } catch (IOException e) {
            MinecraftEduMod.LOGGER.error("Error stopping server", e);
        }
    }
}

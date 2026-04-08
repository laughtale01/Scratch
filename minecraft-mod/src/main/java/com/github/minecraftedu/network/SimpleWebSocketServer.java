package com.github.minecraftedu.network;

import com.github.minecraftedu.MinecraftEduMod;
import com.github.minecraftedu.commands.CommandExecutor;
import net.minecraft.server.MinecraftServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimpleWebSocketServer {
    private static final String WEBSOCKET_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    private final int port;
    private final MinecraftServer minecraftServer;
    private final CommandExecutor commandExecutor;
    private ServerSocket serverSocket;
    private ExecutorService executor;
    private volatile boolean running = false;

    // アクティブ接続追跡（停止時に明示的にcloseするため）
    private final Set<Socket> activeClients =
        Collections.synchronizedSet(new HashSet<>());

    public SimpleWebSocketServer(int port, MinecraftServer minecraftServer, CommandExecutor commandExecutor) {
        this.port = port;
        this.minecraftServer = minecraftServer;
        this.commandExecutor = commandExecutor;
        this.executor = Executors.newCachedThreadPool();
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket();
        serverSocket.setReuseAddress(true);  // TIME_WAIT 状態のソケット回収を許可
        serverSocket.bind(new InetSocketAddress(port));
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
        // 停止時の明示的なソケットclose対象として追跡
        activeClients.add(client);
        // クライアントごとにハンドラーを作成してセッション状態を維持
        // CommandExecutorは全クライアントで共有
        MinecraftWebSocketHandler handler = new MinecraftWebSocketHandler(minecraftServer, commandExecutor);

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
                    byte[] ext = new byte[2];
                    if (!readFully(in, ext, 2)) break;
                    payloadLength = ((ext[0] & 0xFF) << 8) | (ext[1] & 0xFF);
                } else if (payloadLength == 127) {
                    byte[] ext = new byte[8];
                    if (!readFully(in, ext, 8)) break;
                    long longLength = 0;
                    for (int i = 0; i < 8; i++) {
                        longLength = (longLength << 8) | (ext[i] & 0xFF);
                    }
                    if (longLength > Integer.MAX_VALUE) {
                        MinecraftEduMod.LOGGER.warn("Payload too large: " + longLength);
                        break;
                    }
                    payloadLength = (int) longLength;
                }

                // Masking key
                byte[] maskingKey = new byte[4];
                if (masked) {
                    if (!readFully(in, maskingKey, 4)) break;
                }

                // Payload data
                byte[] payload = new byte[payloadLength];
                if (!readFully(in, payload, payloadLength)) break;

                if (masked) {
                    for (int i = 0; i < payload.length; i++) {
                        payload[i] = (byte) (payload[i] ^ maskingKey[i % 4]);
                    }
                }

                // Handle message based on opcode
                if (opcode == 0x1) { // Text frame
                    String message = new String(payload, StandardCharsets.UTF_8);
                    MinecraftEduMod.LOGGER.info("Received WebSocket message: " + message);
                    handleWebSocketMessage(handler, message, out);
                } else if (opcode == 0x8) { // Close frame
                    MinecraftEduMod.LOGGER.info("Client requested close");
                    break;
                } else if (opcode == 0x9) { // Ping frame
                    // Send pong
                    sendPong(out, payload);
                }
            }

        } catch (Exception e) {
            // 停止中の SocketException 等は想定内なので debug レベルに落とす
            if (running) {
                MinecraftEduMod.LOGGER.error("Error handling client", e);
            } else {
                MinecraftEduMod.LOGGER.debug("Client connection closed during shutdown: " + e.getMessage());
            }
        } finally {
            activeClients.remove(client);
            try {
                client.close();
                MinecraftEduMod.LOGGER.info("Client disconnected");
            } catch (IOException e) {
                MinecraftEduMod.LOGGER.error("Error closing client", e);
            }
        }
    }

    private boolean readFully(InputStream in, byte[] buffer, int length) throws IOException {
        int offset = 0;
        while (offset < length) {
            int read = in.read(buffer, offset, length - offset);
            if (read == -1) {
                return false;
            }
            offset += read;
        }
        return true;
    }

    private void handleWebSocketMessage(MinecraftWebSocketHandler handler, String message, OutputStream out) {
        try {
            // Parse JSON and delegate to handler（セッション状態を維持するため既存のハンドラーを使用）
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

        byte[] bytes = frame.toByteArray();
        // sendCloseToAllClients との競合を防ぐため OutputStream で同期
        synchronized (out) {
            out.write(bytes);
            out.flush();
        }
    }

    private void sendPong(OutputStream out, byte[] payload) throws IOException {
        ByteArrayOutputStream frame = new ByteArrayOutputStream();
        frame.write(0x8A); // FIN + opcode 0xA (pong)
        frame.write(payload.length);
        frame.write(payload);
        byte[] bytes = frame.toByteArray();
        // sendCloseToAllClients / sendTextFrame と同じロックで直列化
        synchronized (out) {
            out.write(bytes);
            out.flush();
        }
    }

    /**
     * 全クライアントへ正常クローズ通知（Close フレーム）を送信
     * RFC 6455: opcode 0x8 + status code 1001 (Going Away)
     */
    private void sendCloseToAllClients() {
        byte[] closeFrame = new byte[] {
            (byte) 0x88,              // FIN + Close opcode
            (byte) 0x02,              // payload length 2
            (byte) 0x03, (byte) 0xE9  // status code 1001 (Going Away)
        };
        // activeClients のスナップショットを取り、その上でループ
        Socket[] snapshot;
        synchronized (activeClients) {
            snapshot = activeClients.toArray(new Socket[0]);
        }
        for (Socket s : snapshot) {
            try {
                if (!s.isClosed()) {
                    OutputStream os = s.getOutputStream();
                    synchronized (os) {  // sendTextFrame と同じロックで直列化
                        os.write(closeFrame);
                        os.flush();
                    }
                }
            } catch (IOException ignored) {
                // 既に切れている可能性 → 無視
            }
        }
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
        // 二重呼び出しガード（idempotent）
        if (!running) {
            MinecraftEduMod.LOGGER.debug("WebSocket server stop() called while not running");
            return;
        }
        running = false;
        try {
            // (1) 全クライアントへ Close フレーム送信（Scratch 側 onclose を 1001 で発火させる）
            try { sendCloseToAllClients(); } catch (Exception ignored) {}

            // (2) 各クライアントソケットを明示的に閉じる
            //     （ハンドラスレッドの InputStream.read() ブロックを解除するため）
            Socket[] snapshot;
            synchronized (activeClients) {
                snapshot = activeClients.toArray(new Socket[0]);
            }
            for (Socket s : snapshot) {
                try { s.close(); } catch (IOException ignored) {}
            }

            // (3) ServerSocket を閉じる（accept ループの終了）
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }

            // (4) executor を shutdown → 待機 → 必要なら shutdownNow
            if (executor != null) {
                executor.shutdown();
                try {
                    if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                        executor.shutdownNow();
                        if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                            MinecraftEduMod.LOGGER.warn("Some handler threads did not terminate cleanly");
                        }
                    }
                } catch (InterruptedException ie) {
                    executor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }

            MinecraftEduMod.LOGGER.info("WebSocket server stopped");
        } catch (IOException e) {
            MinecraftEduMod.LOGGER.error("Error stopping server", e);
        }
    }
}

package com.github.minecraftedu.network;

import com.github.minecraftedu.MinecraftEduMod;
import com.github.minecraftedu.commands.CommandExecutor;
import net.minecraft.server.MinecraftServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayDeque;
import java.util.Base64;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimpleWebSocketServer {
    private static final String WEBSOCKET_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    // 同時につなげる数の上限（Scratch のタブ数本分。ハンドシェイクが済んだ接続を数え、超えたら断る）
    static final int MAX_CLIENTS = 8;
    // ハンドシェイクの前の接続の数の上限（上の 8 本とは別に数える。いっぱいなら一番古いものを切る）
    // （ブラウザが前もって開く接続や、何も送らない接続をためても、本物の Scratch が入れなくならないように。
    //  本物の Scratch は、つないですぐハンドシェイクを送るので、押し出される前に済む。
    //  Minecraft が止まったあとに、たまった接続をまとめて受け付けても押し出されにくいよう、多めにしておく）
    private static final int MAX_PENDING_HANDSHAKES = 64;
    // 接続を受け持つスレッドの数の上限（ハンドシェイクの前とつないだあとの全部に、断る返事を送る途中の分を足す）。
    // 押し出した接続のスレッドが終わる前に次を受け付けて、スレッドが増え続けないように
    private static final int MAX_HANDLERS = MAX_PENDING_HANDSHAKES + MAX_CLIENTS + 8;
    // 受け持つスレッドがいっぱいのとき、空くのを待つ時間（押し出したスレッドはすぐ終わるので、ふつうは一瞬で空く）
    private static final int HANDLER_WAIT_MS = 1000;
    // 1 つのメッセージの大きさの上限（命令の JSON には十分。巨大な長さを名乗ってメモリを使い切らせないため）
    private static final int MAX_PAYLOAD_BYTES = 1024 * 1024;
    // ヘッダーの 1 行の長さと行数の上限
    private static final int MAX_HEADER_LINE_BYTES = 8 * 1024;
    private static final int MAX_HEADER_LINES = 100;
    // ハンドシェイク全体の締め切り（何も送らない・少しずつしか送らない接続をいつまでも残さないため。
    // 本物の Scratch は自分の PC の中でつなぐので数ミリ秒で済むが、遅い PC で Minecraft が少し止まっても切らないよう 2 秒）
    private static final int HANDSHAKE_TIMEOUT_MS = 2000;
    // 受付が失敗し続けたときに待つ時間（すぐやり直して CPU とログを使い切らないため）
    private static final int ACCEPT_RETRY_DELAY_MS = 100;
    // ログに出す、相手が送った値（Origin・命令の名前・ブロックの名前など）の長さの上限
    private static final int MAX_LOGGED_CHARS = 100;
    // 断った接続のログを出す間隔（つなぎ直しを繰り返されても、ログが際限なく増えないように）
    private static final long REJECT_LOG_INTERVAL_NANOS = TimeUnit.SECONDS.toNanos(60);
    // つないでよいサイト（Origin）。公開している Scratch と、手元での確認（python3 -m http.server 8000）だけ
    // （localhost の他のポートを許すと、生徒の PC で動く別のアプリの画面を踏み台にされうるため）
    private static final String ALLOWED_ORIGIN = "https://laughtale01.github.io";
    private static final Pattern LOCAL_DEV_ORIGIN = Pattern.compile("^http://(localhost|127\\.0\\.0\\.1):8000$");

    // 同時接続の数を数える（ハンドシェイクが済んだ接続だけ）
    private final Semaphore clientSlots = new Semaphore(MAX_CLIENTS);
    // ハンドシェイクの前の接続（古い順。自分自身で守る）
    private final ArrayDeque<Socket> pendingHandshakes = new ArrayDeque<>();
    // 接続を受け持つスレッドの数を数える
    private final Semaphore handlerSlots = new Semaphore(MAX_HANDLERS);
    /**
     * 接続を断った理由の種類（ログの間引きは種類ごとに行う。1 つにまとめると、
     * 大量に断った種類のせいで、ほかの種類のログが「ほかに N 件」に埋もれるため）
     */
    private enum RejectReason {
        NON_LOCAL("non-local address"),
        TOO_MANY_CLIENTS("too many clients"),
        TOO_MANY_PENDING("too many pending handshakes"),
        HANDLERS_BUSY("handler threads busy"),
        ORIGIN("origin"),
        BAD_REQUEST("bad request"),
        HANDSHAKE_TIMEOUT("handshake timed out"),
        HANDSHAKE_ERROR("handshake failed"),
        BAD_FRAME("bad frame");

        final String label;

        RejectReason(String label) {
            this.label = label;
        }
    }

    /**
     * 断った接続のログをまとめるための状態（種類ごとに 1 つ。rejectLogLock で守る）
     */
    private static class RejectLogState {
        boolean loggedOnce = false;
        long lastLogNanos;
        long suppressed = 0;
    }

    private final Object rejectLogLock = new Object();
    private final Map<RejectReason, RejectLogState> rejectLogStates = new EnumMap<>(RejectReason.class);
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
        // 待ち受けはこれまでどおり全部のアドレスで行う（ブラウザが localhost を ::1 と 127.0.0.1 の
        // どちらで引いてもつながるように）。自分の PC 以外からの接続は、受け付けた直後に切る
        serverSocket.bind(new InetSocketAddress(port));
        running = true;
        MinecraftEduMod.LOGGER.info("WebSocket server started on port " + port);

        // 接続の受け付けは、別のスレッドで回す
        executor.submit(() -> {
            while (running) {
                Socket client = null;
                boolean pendingAdded = false;
                boolean handlerAcquired = false;
                boolean handedOff = false;
                try {
                    client = serverSocket.accept();
                    // 停止の途中に受け付けた接続は、そのまま閉じる（finally で閉じる）
                    if (!running) {
                        continue;
                    }
                    // 自分の PC（ループバック）からの接続だけを受け付ける（同じ LAN の別の端末からはつなげない）
                    if (!client.getInetAddress().isLoopbackAddress()) {
                        logRejected(RejectReason.NON_LOCAL, client.getInetAddress().getHostAddress());
                        continue;
                    }
                    // ハンドシェイクの前の接続として数える（同時接続の 8 本は、ハンドシェイクが済むときに数える）。
                    // いっぱいなら一番古いものを切る
                    Socket evicted = addPending(client);
                    pendingAdded = true;
                    if (evicted != null) {
                        logRejected(RejectReason.TOO_MANY_PENDING, null);
                        closeQuietly(evicted);
                    }
                    // 受け持つスレッドが空くのを待つ（空かなければ、この接続を閉じる。finally で一覧から外す）
                    if (!handlerSlots.tryAcquire(HANDLER_WAIT_MS, TimeUnit.MILLISECONDS)) {
                        if (running) {
                            logRejected(RejectReason.HANDLERS_BUSY, null);
                        }
                        continue;
                    }
                    handlerAcquired = true;
                    // つながったことは、ハンドシェイクが済んでから info で出す
                    // （ここでは debug でも出さない。断る接続をくり返されると、debug.log の古い記録が押し出されるため）
                    final Socket accepted = client;
                    executor.submit(() -> {
                        try {
                            handleClient(accepted);
                        } finally {
                            handlerSlots.release();
                        }
                    });
                    handedOff = true;
                } catch (Throwable e) {
                    // どんな失敗でも受付ループは止めない（止まると再接続できなくなるため）
                    if (running) {
                        MinecraftEduMod.LOGGER.error("Error accepting client", e);
                        // 同じ失敗が続くとき（ファイルの数の上限など）に、すぐやり直して回り続けないよう少し待つ
                        try {
                            Thread.sleep(ACCEPT_RETRY_DELAY_MS);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                } finally {
                    // handleClient に渡せなかった接続は、ここで一覧から外して閉じる（渡せた接続は handleClient が片付ける）
                    if (!handedOff) {
                        if (pendingAdded) {
                            removePending(client);
                        }
                        if (handlerAcquired) {
                            handlerSlots.release();
                        }
                        closeQuietly(client);
                    }
                }
            }
        });
    }

    /**
     * ハンドシェイクの前の接続の一覧に足す。いっぱいなら、一番古いものを一覧から外して返す（呼ぶ側で閉じる）
     */
    private Socket addPending(Socket client) {
        synchronized (pendingHandshakes) {
            Socket oldest = pendingHandshakes.size() >= MAX_PENDING_HANDSHAKES ? pendingHandshakes.pollFirst() : null;
            pendingHandshakes.addLast(client);
            return oldest;
        }
    }

    /**
     * ハンドシェイクの前の接続の一覧から外す。入っていなければ false（押し出されて切られたあとなど。2 回呼んでもよい）
     */
    private boolean removePending(Socket client) {
        synchronized (pendingHandshakes) {
            return pendingHandshakes.remove(client);
        }
    }

    /**
     * つないでよいサイト（Origin）か確認
     * ブラウザは WebSocket の接続で必ず Origin を送る。無いもの・許可していないものは断る
     */
    static boolean isAllowedOrigin(String origin) {
        if (origin == null) return false;
        return ALLOWED_ORIGIN.equals(origin) || LOCAL_DEV_ORIGIN.matcher(origin).matches();
    }

    /**
     * ソケットを閉じる（閉じるときの失敗は気にしない）
     */
    private static void closeQuietly(Socket socket) {
        if (socket == null) return;
        try {
            socket.close();
        } catch (IOException ignored) {
            // 閉じられなくても、これ以上できることはない
        }
    }

    /**
     * 断った接続をログに出す
     * 種類ごとに、1 回目はすぐ出し、そのあとは 60 秒に 1 行まで（間に断った数をまとめて書く）
     * （別のサイトのページからでも、普通のリクエストをくり返し送らせてログを増やせるため、
     *   断る道はすべてここを通す）
     * detail は相手が送った値を含みうるので、呼ぶ側で sanitizeForLog などを通しておく
     */
    private void logRejected(RejectReason reason, String detail) {
        long now = System.nanoTime();
        long suppressed;
        synchronized (rejectLogLock) {
            RejectLogState state = rejectLogStates.computeIfAbsent(reason, r -> new RejectLogState());
            if (state.loggedOnce && now - state.lastLogNanos < REJECT_LOG_INTERVAL_NANOS) {
                state.suppressed++;
                return;
            }
            suppressed = state.suppressed;
            state.suppressed = 0;
            state.lastLogNanos = now;
            state.loggedOnce = true;
        }
        MinecraftEduMod.LOGGER.warn("Rejected WebSocket connection (" + reason.label + ")"
            + (detail != null ? ": " + detail : "")
            + (suppressed > 0 ? " (and " + suppressed + " more since the last log)" : ""));
    }

    /**
     * ログに出してよい形にする（相手が送った値に改行や制御文字があっても、偽の行を作れないように）
     * Origin・命令の名前・ブロックの名前・例外の文などに使う。見える ASCII（0x20〜0x7E）以外はすべて ? にする
     * （ログは英数字だけで足りる。子どもが日本語で打った名前も ? になる）
     * （Unicode の行区切り U+2028 や向きを変える文字なども、これで残らない）
     */
    public static String sanitizeForLog(String value) {
        if (value == null) return "(none)";
        String cut = value.length() > MAX_LOGGED_CHARS ? value.substring(0, MAX_LOGGED_CHARS) + "..." : value;
        StringBuilder sb = new StringBuilder(cut.length());
        for (int i = 0; i < cut.length(); i++) {
            char c = cut.charAt(i);
            sb.append(c >= 0x20 && c <= 0x7E ? c : '?');
        }
        return sb.toString();
    }

    /**
     * ハンドシェイクの上限（行が長すぎる・多すぎる）に当たったときの例外
     * 想定内の失敗なので、ログは例外の詳しい中身を付けずに warn で出す
     */
    private static class HandshakeLimitException extends IOException {
        HandshakeLimitException(String message) {
            super(message);
        }
    }

    /**
     * ヘッダーを 1 行読む（CRLF まで）。長すぎる行は HandshakeLimitException。接続が終わったら null
     * deadlineNanos（System.nanoTime の値）を過ぎて、まだ届いていないバイトを待つことになったら SocketTimeoutException
     * （setSoTimeout は 1 回の読み取りの待ち時間にしか効かないので、読むたびに残り時間を設定し直す。
     *  締め切りを過ぎても、もう届いているバイトは読み切る。読む途中で Minecraft が止まっても、届いたヘッダーで切らないため。
     *  長さと行数の上限があるので、送り続けても終わる）
     */
    private static String readHeaderLine(InputStream in, Socket client, long deadlineNanos) throws IOException {
        ByteArrayOutputStream line = new ByteArrayOutputStream();
        int b;
        while (true) {
            long remainingMs = TimeUnit.NANOSECONDS.toMillis(deadlineNanos - System.nanoTime());
            if (remainingMs > 0) {
                client.setSoTimeout((int) Math.min(remainingMs, Integer.MAX_VALUE));
            } else if (in.available() == 0) {
                // 締め切りを過ぎて、届いているバイトも無い（available はソケットにも聞くので、締め切りを過ぎたときだけ呼ぶ）
                throw new SocketTimeoutException("WebSocket handshake timed out");
            }
            if ((b = in.read()) == -1) break;
            if (b == '\n') {
                byte[] bytes = line.toByteArray();
                int len = bytes.length;
                if (len > 0 && bytes[len - 1] == '\r') len--;
                return new String(bytes, 0, len, StandardCharsets.UTF_8);
            }
            if (line.size() >= MAX_HEADER_LINE_BYTES) {
                throw new HandshakeLimitException("WebSocket header line too long");
            }
            line.write(b);
        }
        return line.size() == 0 ? null : line.toString(StandardCharsets.UTF_8);
    }

    private static void sendHttpError(OutputStream out, String status) throws IOException {
        out.write(("HTTP/1.1 " + status + "\r\nContent-Length: 0\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    private void handleClient(Socket client) {
        // ハンドシェイクの締め切り（受け持ちが始まってからの時間で数える。受け付けたあと、受け持つスレッドが空くのを
        // 待ったときは、その分あとになる）
        long handshakeDeadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(HANDSHAKE_TIMEOUT_MS);
        // ハンドシェイクが済んだか（済んだ接続だけ、切れたことを info で出す）
        boolean handshakeDone = false;
        // 同時接続の枠（8 本）を取ったか（取った接続だけ、終わるときに返す）
        boolean slotAcquired = false;
        // ハンドシェイクの前の一覧から、自分で外したか（外していないのに一覧に無ければ、押し出されて切られた）
        boolean leftPending = false;

        // ヘッダーとフレームは同じストリームから読む（先読みしたバイトを取りこぼさないため）
        // （下の finally で必ず一覧から外し、取った枠を返すため、ここから先は全部 try の中に置く）
        try (InputStream in = new BufferedInputStream(client.getInputStream());
             OutputStream out = client.getOutputStream()) {

            // 停止時の明示的なソケットclose対象として追跡
            activeClients.add(client);
            // 一覧に入れる前に stop() が一覧を閉じ終えていたら、ここで終える（読み取りで最大 2 秒残らないように）
            if (!running) {
                return;
            }
            // クライアントごとにハンドラーを作成してセッション状態を維持
            // CommandExecutorは全クライアントで共有
            MinecraftWebSocketHandler handler = new MinecraftWebSocketHandler(minecraftServer, commandExecutor);

            // HTTP のヘッダーを読む（行の長さと行数に上限を付けて、名前は小文字にそろえる）
            Map<String, String> headers = new HashMap<>();
            String requestLine = readHeaderLine(in, client, handshakeDeadline);
            if (requestLine == null) return;
            int lines = 0;
            String line;
            while ((line = readHeaderLine(in, client, handshakeDeadline)) != null && !line.isEmpty()) {
                if (++lines > MAX_HEADER_LINES) {
                    throw new HandshakeLimitException("Too many WebSocket header lines");
                }
                int colon = line.indexOf(':');
                if (colon > 0) {
                    headers.put(line.substring(0, colon).trim().toLowerCase(Locale.ROOT), line.substring(colon + 1).trim());
                }
            }

            String webSocketKey = headers.get("sec-websocket-key");
            if (webSocketKey == null || webSocketKey.isEmpty()) {
                // 別のサイトのページからも <img> などで送らせられるので、ログは間引く
                logRejected(RejectReason.BAD_REQUEST, "no websocket key");
                sendHttpError(out, "400 Bad Request");
                return;
            }

            // 公開している Scratch（と手元での開発）以外のサイトからの接続は断る
            String origin = headers.get("origin");
            if (!isAllowedOrigin(origin)) {
                logRejected(RejectReason.ORIGIN, sanitizeForLog(origin));
                sendHttpError(out, "403 Forbidden");
                return;
            }

            // ハンドシェイクの前の一覧から外す（外せなかったら、一番古い接続として押し出されて切られたあと）
            if (!removePending(client)) {
                return;
            }
            leftPending = true;
            // 同時接続の枠（8 本）は、ここで取る（ハンドシェイクの前の接続や、ほかのサイトからの接続は、枠を使わない）
            if (!clientSlots.tryAcquire()) {
                logRejected(RejectReason.TOO_MANY_CLIENTS, null);
                sendHttpError(out, "503 Service Unavailable");
                return;
            }
            slotAcquired = true;

            // WebSocket のハンドシェイクの返事を送る
            String acceptKey = generateAcceptKey(webSocketKey);
            String response = "HTTP/1.1 101 Switching Protocols\r\n" +
                    "Upgrade: websocket\r\n" +
                    "Connection: Upgrade\r\n" +
                    "Sec-WebSocket-Accept: " + acceptKey + "\r\n" +
                    "\r\n";

            out.write(response.getBytes(StandardCharsets.UTF_8));
            out.flush();

            handshakeDone = true;
            MinecraftEduMod.LOGGER.info("WebSocket handshake completed: " + client.getRemoteSocketAddress());

            // ハンドシェイクの後は、命令を待つ間に切れないよう時間制限を外す（つなげるのは自分の PC からだけ）
            client.setSoTimeout(0);

            // WebSocket のフレームを読む
            while (running && !client.isClosed()) {
                // フレームを 1 つ読む
                int firstByte = in.read();
                if (firstByte == -1) break;

                boolean fin = (firstByte & 0x80) != 0;
                int opcode = firstByte & 0x0F;

                int secondByte = in.read();
                if (secondByte == -1) break;

                boolean masked = (secondByte & 0x80) != 0;
                int payloadLength = secondByte & 0x7F;

                // クライアントからのフレームは必ずマスクされている（RFC 6455）。されていなければ切る
                if (!masked) {
                    // 決まりに合わないフレームのログも、くり返されても増えないよう間引く
                    logRejected(RejectReason.BAD_FRAME, "unmasked");
                    break;
                }

                // 長さが 126 以上のときは、続きのバイトに長さが入っている
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
                    if (longLength < 0 || longLength > MAX_PAYLOAD_BYTES) {
                        logRejected(RejectReason.BAD_FRAME, "payload too large: " + longLength);
                        break;
                    }
                    payloadLength = (int) longLength;
                }

                // 大きすぎるメッセージは、メモリを確保する前に断る
                if (payloadLength > MAX_PAYLOAD_BYTES) {
                    logRejected(RejectReason.BAD_FRAME, "payload too large: " + payloadLength);
                    break;
                }
                // 制御フレーム（Close / Ping / Pong）は 125 バイトまで（RFC 6455）
                if (opcode >= 0x8 && payloadLength > 125) {
                    logRejected(RejectReason.BAD_FRAME, "control frame too large: " + payloadLength);
                    break;
                }

                // マスクの鍵（マスクの無いフレームは上で切ったので、ここに来るのはいつもマスク付き）
                byte[] maskingKey = new byte[4];
                if (!readFully(in, maskingKey, 4)) break;

                // 中身を読んで、マスクを外す
                byte[] payload = new byte[payloadLength];
                if (!readFully(in, payload, payloadLength)) break;
                for (int i = 0; i < payload.length; i++) {
                    payload[i] = (byte) (payload[i] ^ maskingKey[i % 4]);
                }

                // 種類（opcode）ごとに扱う
                if (opcode == 0x1) { // 文字のフレーム
                    String message = new String(payload, StandardCharsets.UTF_8);
                    // 受け取った全文はログに出さない（ログが大きくなり、偽の行も差し込めてしまうため）
                    MinecraftEduMod.LOGGER.debug("Received WebSocket message (" + payload.length + " bytes)");
                    handleWebSocketMessage(handler, message, out);
                } else if (opcode == 0x8) { // Close のフレーム
                    MinecraftEduMod.LOGGER.info("Client requested close");
                    break;
                } else if (opcode == 0x9) { // Ping のフレーム
                    // Pong を返す
                    sendPong(out, payload);
                }
            }

        } catch (SocketTimeoutException e) {
            // ハンドシェイクの締め切りを過ぎた（想定内なので詳しい中身は出さない）
            logRejected(RejectReason.HANDSHAKE_TIMEOUT, null);
        } catch (HandshakeLimitException e) {
            // ヘッダーが長すぎる・多すぎる（想定内なので詳しい中身は出さない。長い URL なら別のサイトからも起こせる）
            logRejected(RejectReason.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            // ハンドシェイクの前の通信の失敗か
            boolean beforeHandshakeIo = running && !handshakeDone && e instanceof IOException;
            if (beforeHandshakeIo) {
                // 自分で一覧から外していないのに一覧に無ければ、一番古い接続として押し出されて切られた。
                // 押し出したときに、間引いた too many pending handshakes を出しているので、ここでは debug でも出さない
                // （押し出しをくり返されると、debug.log の古い記録が押し出されるため）。
                // removePending は一覧から外す（finally でも外すので、2 回になってもよい）
                boolean evicted = !leftPending && !removePending(client);
                if (!evicted) {
                    // 途中で切られた等は、別のサイトからもくり返し起こせるので間引く
                    // （詳しい中身は出さず、例外の種類だけ出す。プログラムの不具合は下の error で詳しく出す）
                    logRejected(RejectReason.HANDSHAKE_ERROR, e.getClass().getSimpleName());
                }
            } else if (running && e instanceof IOException) {
                // つないだあとに、相手が乱暴に切った（タブを閉じた等）。よくあることで、くり返されても増えないよう debug
                MinecraftEduMod.LOGGER.debug("Client connection lost: " + e.getClass().getSimpleName());
            } else if (running) {
                MinecraftEduMod.LOGGER.error("Error handling client", e);
            } else {
                // 停止中の SocketException 等は想定内なので debug レベルに落とす
                MinecraftEduMod.LOGGER.debug("Client connection closed during shutdown: " + e.getMessage());
            }
        } finally {
            activeClients.remove(client);
            removePending(client);
            if (slotAcquired) {
                clientSlots.release();
            }
            try {
                client.close();
                // ハンドシェイクの前に切れた接続は出さない（断った理由は logRejected で出している）
                if (handshakeDone) {
                    MinecraftEduMod.LOGGER.info("Client disconnected");
                }
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

    private void handleWebSocketMessage(MinecraftWebSocketHandler handler, String message, OutputStream out) throws IOException {
        try {
            // JSON の読み取りと命令の実行はハンドラーに任せる（セッションの状態を保つため、接続ごとのハンドラーを使う）
            String response = handler.handleMessage(message);

            if (response != null) {
                sendTextFrame(out, response);
            }
        } catch (IOException e) {
            // 返事を書けなかった（相手が切れた）。外に投げて、読み取りの「切れた」の道（debug）で終える
            throw e;
        } catch (Exception e) {
            MinecraftEduMod.LOGGER.error("Error handling message", e);
        }
    }

    private void sendTextFrame(OutputStream out, String text) throws IOException {
        byte[] payload = text.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream frame = new ByteArrayOutputStream();

        // 1 バイト目: FIN と種類（0x1 は文字）
        frame.write(0x81);

        // 2 バイト目: 中身の長さ（サーバーからクライアントへはマスクを付けない）
        if (payload.length < 126) {
            frame.write(payload.length);
        } else if (payload.length < 65536) {
            frame.write(126);
            frame.write((payload.length >> 8) & 0xFF);
            frame.write(payload.length & 0xFF);
        } else {
            frame.write(127);
            for (int i = 7; i >= 0; i--) {
                // long でずらす（int のままだと 32 ビット以上ずらせず、長さの上の桁が壊れるため）
                frame.write((int) (((long) payload.length >> (i * 8)) & 0xFF));
            }
        }

        // 中身
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
        // 受け取る Ping は 125 バイトまでに絞っているので、長さは 1 バイトで書ける
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
     * RFC 6455: 種類 0x8（Close）と、理由のコード 1001（Going Away: サーバーが止まる）
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

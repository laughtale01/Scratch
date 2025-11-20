# マルチプレイヤー対応 詳細設計書

## 目次

1. [概要](#概要)
2. [アーキテクチャ](#アーキテクチャ)
3. [接続管理システム](#接続管理システム)
4. [認証とセッション管理](#認証とセッション管理)
5. [権限システム](#権限システム)
6. [コマンド処理とキューイング](#コマンド処理とキューイング)
7. [競合解決メカニズム](#競合解決メカニズム)
8. [実装詳細](#実装詳細)
9. [API仕様](#api仕様)
10. [テストシナリオ](#テストシナリオ)

---

## 概要

### 目標

複数のScratchクライアント（最大10）が同時に1つのMinecraftサーバーに接続し、協調的または独立的にワールドを操作できるシステムを実現する。

### 主要機能

1. **複数クライアント同時接続**: 最大10クライアント
2. **役割ベースアクセス制御 (RBAC)**: Teacher/Student/Observer
3. **リアルタイム同期**: 全クライアントへのイベント配信
4. **競合解決**: 同時操作時の適切な処理
5. **セッション管理**: 接続状態の追跡と再接続サポート

### 設計原則

- **スケーラビリティ**: 将来的に20+クライアントまで拡張可能
- **耐障害性**: クライアント切断時の自動復旧
- **セキュリティ**: 不正アクセス防止と権限分離
- **パフォーマンス**: 低レイテンシ（< 100ms）

---

## アーキテクチャ

### システム構成

```
┌─────────────────────────────────────────────────────────┐
│                  Scratchクライアント層                    │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐        │
│  │ Client 1   │  │ Client 2   │  │ Client 10  │        │
│  │ (Teacher)  │  │ (Student)  │  │ (Student)  │ ...    │
│  └─────┬──────┘  └─────┬──────┘  └─────┬──────┘        │
│        │                │                │               │
└────────┼────────────────┼────────────────┼───────────────┘
         │                │                │
         │     WebSocket (ws://host:14711/minecraft)
         │                │                │
┌────────▼────────────────▼────────────────▼───────────────┐
│               WebSocketServer (Netty)                     │
│  ┌──────────────────────────────────────────────────┐   │
│  │          ConnectionManagerService                 │   │
│  │  ┌────────────────────────────────────────────┐  │   │
│  │  │  activeConnections: Map<SessionId, Conn>   │  │   │
│  │  │  sessionStore: SessionStore                │  │   │
│  │  │  authService: AuthenticationService        │  │   │
│  │  └────────────────────────────────────────────┘  │   │
│  └──────────────────────────────────────────────────┘   │
└───────────────────────────┬──────────────────────────────┘
                            │
┌───────────────────────────▼──────────────────────────────┐
│               CommandDispatcher                           │
│  ┌──────────────────────────────────────────────────┐   │
│  │  1. 権限チェック (PermissionService)              │   │
│  │  2. コマンドキューイング (CommandQueue)           │   │
│  │  3. 競合解決 (ConflictResolver)                  │   │
│  │  4. 実行 (MinecraftCommandExecutor)              │   │
│  │  5. イベント配信 (EventBroadcaster)              │   │
│  └──────────────────────────────────────────────────┘   │
└───────────────────────────┬──────────────────────────────┘
                            │
┌───────────────────────────▼──────────────────────────────┐
│           Minecraft Server (1.20.x + Forge)               │
│                    ワールド操作                            │
└───────────────────────────────────────────────────────────┘
```

### データフロー

```
[Client] --1. Connect--> [ConnectionManager] --2. Auth--> [AuthService]
                                │
                                ├--3. Create Session--> [SessionStore]
                                │
                                ├--4. Add to Pool--> [activeConnections]
                                │
                                └--5. Send Welcome--> [Client]

[Client] --6. Send Command--> [CommandDispatcher]
                                │
                                ├--7. Check Permission--> [PermissionService]
                                │
                                ├--8. Enqueue--> [CommandQueue]
                                │
                                ├--9. Resolve Conflict--> [ConflictResolver]
                                │
                                ├--10. Execute--> [MinecraftServer]
                                │
                                └--11. Broadcast Event--> [All Clients]
```

---

## 接続管理システム

### ConnectionManager クラス

#### 責務

1. WebSocket接続の受け入れ
2. クライアント認証
3. セッション作成と管理
4. 接続プールの維持
5. ハートビート/ヘルスチェック

#### クラス定義（Java）

```java
package com.github.minecraftedu.network;

import io.netty.channel.ChannelHandlerContext;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

public class ConnectionManager {

    // アクティブ接続プール
    private final ConcurrentHashMap<String, ClientConnection> activeConnections;

    // 最大接続数
    private static final int MAX_CONNECTIONS = 10;

    // サービス依存性
    private final AuthenticationService authService;
    private final SessionStore sessionStore;
    private final EventBroadcaster broadcaster;

    public ConnectionManager(
        AuthenticationService authService,
        SessionStore sessionStore,
        EventBroadcaster broadcaster
    ) {
        this.activeConnections = new ConcurrentHashMap<>();
        this.authService = authService;
        this.sessionStore = sessionStore;
        this.broadcaster = broadcaster;
    }

    /**
     * 新しいクライアント接続を処理
     */
    public ConnectionResult handleNewConnection(
        ChannelHandlerContext ctx,
        String clientId,
        String authToken
    ) {
        // 1. 接続数チェック
        if (activeConnections.size() >= MAX_CONNECTIONS) {
            return ConnectionResult.error("Server is full");
        }

        // 2. 認証
        AuthResult authResult = authService.authenticate(clientId, authToken);
        if (!authResult.isSuccess()) {
            return ConnectionResult.error("Authentication failed");
        }

        // 3. セッション作成
        String sessionId = UUID.randomUUID().toString();
        ClientSession session = ClientSession.builder()
            .sessionId(sessionId)
            .clientId(clientId)
            .clientName(authResult.getClientName())
            .role(authResult.getRole())
            .permissions(authResult.getPermissions())
            .connectionTime(System.currentTimeMillis())
            .build();

        // 4. 接続オブジェクト作成
        ClientConnection connection = new ClientConnection(ctx, session);
        activeConnections.put(sessionId, connection);
        sessionStore.save(session);

        // 5. 接続通知をブロードキャスト
        broadcaster.broadcastEvent(new ClientConnectedEvent(session));

        // 6. ウェルカムメッセージ送信
        connection.send(WelcomeMessage.create(session));

        return ConnectionResult.success(sessionId);
    }

    /**
     * クライアント切断処理
     */
    public void handleDisconnection(String sessionId) {
        ClientConnection connection = activeConnections.remove(sessionId);
        if (connection != null) {
            ClientSession session = connection.getSession();
            session.setActive(false);
            sessionStore.update(session);

            // 切断通知をブロードキャスト
            broadcaster.broadcastEvent(new ClientDisconnectedEvent(session));
        }
    }

    /**
     * 再接続処理
     */
    public ConnectionResult handleReconnection(
        ChannelHandlerContext ctx,
        String oldSessionId,
        String authToken
    ) {
        // 古いセッション取得
        ClientSession oldSession = sessionStore.get(oldSessionId);
        if (oldSession == null) {
            return ConnectionResult.error("Session not found");
        }

        // 再認証
        if (!authService.validateToken(oldSession.getClientId(), authToken)) {
            return ConnectionResult.error("Re-authentication failed");
        }

        // 新しいセッションID生成
        String newSessionId = UUID.randomUUID().toString();
        ClientSession newSession = oldSession.renew(newSessionId);

        // 接続復元
        ClientConnection connection = new ClientConnection(ctx, newSession);
        activeConnections.put(newSessionId, connection);
        sessionStore.save(newSession);

        broadcaster.broadcastEvent(new ClientReconnectedEvent(newSession));
        connection.send(ReconnectionMessage.create(newSession));

        return ConnectionResult.success(newSessionId);
    }

    /**
     * ハートビートチェック
     */
    public void performHealthCheck() {
        long now = System.currentTimeMillis();
        long timeout = 60_000; // 60秒

        activeConnections.values().forEach(conn -> {
            if (now - conn.getLastHeartbeat() > timeout) {
                handleDisconnection(conn.getSession().getSessionId());
            } else {
                conn.send(new HeartbeatMessage());
            }
        });
    }

    /**
     * すべてのアクティブセッション取得
     */
    public List<ClientSession> getActiveSessions() {
        return activeConnections.values().stream()
            .map(ClientConnection::getSession)
            .collect(Collectors.toList());
    }

    /**
     * 特定セッション取得
     */
    public Optional<ClientConnection> getConnection(String sessionId) {
        return Optional.ofNullable(activeConnections.get(sessionId));
    }
}
```

### ClientConnection クラス

```java
package com.github.minecraftedu.network;

import io.netty.channel.ChannelHandlerContext;
import com.google.gson.Gson;

public class ClientConnection {

    private final ChannelHandlerContext context;
    private final ClientSession session;
    private long lastHeartbeat;
    private final Gson gson;

    public ClientConnection(ChannelHandlerContext ctx, ClientSession session) {
        this.context = ctx;
        this.session = session;
        this.lastHeartbeat = System.currentTimeMillis();
        this.gson = new Gson();
    }

    /**
     * メッセージ送信
     */
    public void send(Object message) {
        String json = gson.toJson(message);
        context.writeAndFlush(json);
    }

    /**
     * ハートビート更新
     */
    public void updateHeartbeat() {
        this.lastHeartbeat = System.currentTimeMillis();
    }

    public long getLastHeartbeat() {
        return lastHeartbeat;
    }

    public ClientSession getSession() {
        return session;
    }

    public boolean isActive() {
        return context.channel().isActive();
    }
}
```

---

## 認証とセッション管理

### 認証フロー

```
1. [Client] → [Server]: Connect Request + Auth Token
2. [Server]: Validate Token
3. [Server]: Check User Database / Config
4. [Server]: Assign Role (Teacher/Student/Observer)
5. [Server]: Create Session
6. [Server] → [Client]: Session ID + Permissions
```

### 認証方式

#### シンプルトークン認証（初期実装）

```java
public class AuthenticationService {

    // config.jsonから読み込んだトークンマップ
    private final Map<String, UserConfig> userConfigs;

    public AuthResult authenticate(String clientId, String token) {
        UserConfig config = userConfigs.get(clientId);

        if (config == null) {
            return AuthResult.failure("User not found");
        }

        if (!config.getToken().equals(token)) {
            return AuthResult.failure("Invalid token");
        }

        return AuthResult.success(
            clientId,
            config.getName(),
            config.getRole(),
            config.getPermissions()
        );
    }
}
```

**config.json 例**:

```json
{
  "users": [
    {
      "clientId": "teacher_001",
      "name": "山田先生",
      "token": "teacher-secret-token-abc123",
      "role": "TEACHER",
      "permissions": ["ALL"]
    },
    {
      "clientId": "student_001",
      "name": "田中太郎",
      "token": "student-token-xyz789",
      "role": "STUDENT_FULL",
      "permissions": ["PLACE_BLOCK", "SUMMON_ENTITY", "CHAT"]
    },
    {
      "clientId": "observer_001",
      "name": "佐藤花子",
      "token": "observer-token-def456",
      "role": "OBSERVER",
      "permissions": ["READ_ONLY"]
    }
  ]
}
```

#### 将来的な拡張（OAuth 2.0）

```java
public class OAuthAuthenticationService implements AuthenticationService {

    private final OAuthClient oauthClient;

    @Override
    public AuthResult authenticate(String clientId, String token) {
        // OAuth 2.0 トークン検証
        OAuthValidationResult validation = oauthClient.validateToken(token);

        if (!validation.isValid()) {
            return AuthResult.failure("Invalid OAuth token");
        }

        // ユーザー情報取得
        UserInfo userInfo = oauthClient.getUserInfo(token);

        // 役割マッピング
        Role role = mapOAuthRoleToSystemRole(userInfo.getRoles());

        return AuthResult.success(
            clientId,
            userInfo.getName(),
            role,
            getPermissionsForRole(role)
        );
    }
}
```

### SessionStore

```java
public interface SessionStore {
    void save(ClientSession session);
    void update(ClientSession session);
    ClientSession get(String sessionId);
    void delete(String sessionId);
    List<ClientSession> getAllActive();
}

public class InMemorySessionStore implements SessionStore {
    private final ConcurrentHashMap<String, ClientSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void save(ClientSession session) {
        sessions.put(session.getSessionId(), session);
    }

    @Override
    public ClientSession get(String sessionId) {
        return sessions.get(sessionId);
    }

    // ... その他のメソッド
}

// 永続化版（将来実装）
public class DatabaseSessionStore implements SessionStore {
    private final DataSource dataSource;

    // SQLite/PostgreSQLへの保存実装
}
```

---

## 権限システム

### 役割定義

```java
public enum Role {
    TEACHER(100, "教師"),
    STUDENT_FULL(50, "生徒（フル権限）"),
    STUDENT_LIMITED(25, "生徒（制限付き）"),
    OBSERVER(0, "観察者");

    private final int priority;
    private final String displayName;

    Role(int priority, String displayName) {
        this.priority = priority;
        this.displayName = displayName;
    }

    public boolean canOverride(Role other) {
        return this.priority > other.priority;
    }
}
```

### 権限定義

```java
public enum Permission {
    // 基本操作
    CHAT("チャット送信"),
    PLACE_BLOCK("ブロック配置"),
    BREAK_BLOCK("ブロック破壊"),

    // エンティティ
    SUMMON_ENTITY("エンティティ召喚"),
    MODIFY_ENTITY("エンティティ変更"),

    // プレイヤー操作
    TELEPORT("テレポート"),
    SET_GAMEMODE("ゲームモード変更"),
    GIVE_ITEM("アイテム付与"),

    // ワールド操作
    SET_WEATHER("天気変更"),
    SET_TIME("時刻変更"),
    FILL_BLOCKS("範囲ブロック配置"),

    // 高度な操作
    EXECUTE_COMMAND("任意コマンド実行"),
    MANAGE_CLIENTS("クライアント管理"),

    // 教育機能
    CREATE_TUTORIAL("チュートリアル作成"),
    ASSIGN_CHALLENGE("課題割り当て"),
    GRADE_WORK("作品評価"),

    // 読み取り専用
    READ_ONLY("読み取り専用");

    private final String description;

    Permission(String description) {
        this.description = description;
    }
}
```

### 役割と権限のマッピング

```java
public class PermissionMapper {

    private static final Map<Role, Set<Permission>> ROLE_PERMISSIONS = Map.of(
        Role.TEACHER, EnumSet.allOf(Permission.class), // すべての権限

        Role.STUDENT_FULL, EnumSet.of(
            Permission.CHAT,
            Permission.PLACE_BLOCK,
            Permission.BREAK_BLOCK,
            Permission.SUMMON_ENTITY,
            Permission.TELEPORT,
            Permission.SET_WEATHER,
            Permission.SET_TIME
        ),

        Role.STUDENT_LIMITED, EnumSet.of(
            Permission.CHAT,
            Permission.PLACE_BLOCK
        ),

        Role.OBSERVER, EnumSet.of(
            Permission.READ_ONLY
        )
    );

    public static Set<Permission> getPermissionsForRole(Role role) {
        return ROLE_PERMISSIONS.getOrDefault(role, EnumSet.noneOf(Permission.class));
    }
}
```

### PermissionService

```java
public class PermissionService {

    /**
     * 権限チェック
     */
    public boolean hasPermission(ClientSession session, Permission permission) {
        return session.getPermissions().contains(permission);
    }

    /**
     * 複数権限チェック（すべて必要）
     */
    public boolean hasAllPermissions(ClientSession session, Permission... permissions) {
        return session.getPermissions().containsAll(Arrays.asList(permissions));
    }

    /**
     * 複数権限チェック（いずれか必要）
     */
    public boolean hasAnyPermission(ClientSession session, Permission... permissions) {
        return Arrays.stream(permissions)
            .anyMatch(p -> session.getPermissions().contains(p));
    }

    /**
     * コマンドに必要な権限を取得
     */
    public Set<Permission> getRequiredPermissions(Command command) {
        switch (command.getType()) {
            case SET_BLOCK:
                return Set.of(Permission.PLACE_BLOCK);
            case SUMMON:
                return Set.of(Permission.SUMMON_ENTITY);
            case EXECUTE_COMMAND:
                return Set.of(Permission.EXECUTE_COMMAND);
            case SET_WEATHER:
                return Set.of(Permission.SET_WEATHER);
            // ... その他
            default:
                return Set.of();
        }
    }

    /**
     * 権限チェックとエラーメッセージ
     */
    public PermissionCheckResult checkPermission(
        ClientSession session,
        Command command
    ) {
        Set<Permission> required = getRequiredPermissions(command);

        for (Permission perm : required) {
            if (!hasPermission(session, perm)) {
                return PermissionCheckResult.denied(
                    String.format(
                        "権限が不足しています: %s (役割: %s)",
                        perm.getDescription(),
                        session.getRole().getDisplayName()
                    )
                );
            }
        }

        return PermissionCheckResult.allowed();
    }
}
```

---

## コマンド処理とキューイング

### CommandQueue

```java
public class CommandQueue {

    private final PriorityBlockingQueue<QueuedCommand> queue;
    private final ExecutorService executor;
    private volatile boolean running;

    public CommandQueue(int threadPoolSize) {
        this.queue = new PriorityBlockingQueue<>(100, new CommandComparator());
        this.executor = Executors.newFixedThreadPool(threadPoolSize);
        this.running = true;

        startProcessing();
    }

    /**
     * コマンドをキューに追加
     */
    public CompletableFuture<CommandResult> enqueue(
        ClientSession session,
        Command command
    ) {
        QueuedCommand queuedCommand = QueuedCommand.builder()
            .command(command)
            .session(session)
            .timestamp(System.currentTimeMillis())
            .priority(calculatePriority(session, command))
            .build();

        queue.offer(queuedCommand);

        return queuedCommand.getFuture();
    }

    /**
     * 優先度計算
     */
    private int calculatePriority(ClientSession session, Command command) {
        int rolePriority = session.getRole().getPriority();
        int commandPriority = command.getPriority();

        // 教師の操作は優先度高
        // 緊急コマンド（例: ストップ）は最高優先度
        return rolePriority + commandPriority;
    }

    /**
     * キュー処理開始
     */
    private void startProcessing() {
        executor.submit(() -> {
            while (running) {
                try {
                    QueuedCommand queuedCmd = queue.take();
                    processCommand(queuedCmd);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }

    /**
     * コマンド実行
     */
    private void processCommand(QueuedCommand queuedCmd) {
        try {
            CommandResult result = executeCommand(
                queuedCmd.getSession(),
                queuedCmd.getCommand()
            );
            queuedCmd.getFuture().complete(result);
        } catch (Exception e) {
            queuedCmd.getFuture().completeExceptionally(e);
        }
    }

    /**
     * キュー停止
     */
    public void shutdown() {
        running = false;
        executor.shutdown();
    }
}
```

---

## 競合解決メカニズム

### 競合タイプ

| 競合タイプ | 説明 | 解決戦略 |
|-----------|------|---------|
| **同一ブロック書き込み** | 複数クライアントが同じ座標にブロックを置く | Last-Write-Wins（後勝ち） |
| **エンティティ操作競合** | 同じエンティティへの同時操作 | 役割優先（教師 > 生徒） |
| **コマンド順序依存** | A→Bの順序が重要なコマンド | タイムスタンプ順序保証 |
| **リソース枯渇** | アイテム付与時の在庫不足 | 先着順 |

### ConflictResolver

```java
public class ConflictResolver {

    // 最近のブロック操作履歴（座標 → 操作情報）
    private final Cache<BlockPos, BlockOperation> recentOperations;

    public ConflictResolver() {
        // 60秒間の操作履歴を保持
        this.recentOperations = CacheBuilder.newBuilder()
            .expireAfterWrite(60, TimeUnit.SECONDS)
            .build();
    }

    /**
     * ブロック配置の競合チェック
     */
    public ConflictResolution resolveBlockPlacement(
        ClientSession session,
        BlockPlaceCommand command
    ) {
        BlockPos pos = command.getPosition();
        BlockOperation existing = recentOperations.getIfPresent(pos);

        if (existing == null) {
            // 競合なし
            recentOperations.put(pos, new BlockOperation(session, command));
            return ConflictResolution.allow();
        }

        long timeDiff = System.currentTimeMillis() - existing.getTimestamp();

        // 500ms以内の操作を競合とみなす
        if (timeDiff < 500) {
            // 役割優先度で解決
            if (session.getRole().canOverride(existing.getSession().getRole())) {
                recentOperations.put(pos, new BlockOperation(session, command));
                return ConflictResolution.allowWithWarning(
                    "別のクライアントの操作を上書きしました"
                );
            } else {
                return ConflictResolution.deny(
                    String.format(
                        "%s が先にブロックを配置しています",
                        existing.getSession().getClientName()
                    )
                );
            }
        }

        // 十分時間が経過している場合は許可
        recentOperations.put(pos, new BlockOperation(session, command));
        return ConflictResolution.allow();
    }

    /**
     * エンティティ操作の競合チェック
     */
    public ConflictResolution resolveEntityOperation(
        ClientSession session,
        EntityCommand command
    ) {
        // 同様のロジック...
    }
}
```

### 競合通知

```java
// すべてのクライアントに通知
public void notifyConflict(ConflictEvent event) {
    broadcaster.broadcastEvent(new ConflictNotification(
        event.getAffectedSessions(),
        event.getConflictType(),
        event.getResolution()
    ));
}
```

---

## 実装詳細

### WebSocketサーバー（Netty）

```java
public class MinecraftWebSocketServer {

    private final int port;
    private final ConnectionManager connectionManager;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    public void start() throws InterruptedException {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();

                        // HTTP
                        pipeline.addLast(new HttpServerCodec());
                        pipeline.addLast(new HttpObjectAggregator(65536));

                        // WebSocket
                        pipeline.addLast(new WebSocketServerProtocolHandler("/minecraft"));

                        // カスタムハンドラ
                        pipeline.addLast(new MinecraftWebSocketHandler(connectionManager));
                    }
                });

            ChannelFuture future = bootstrap.bind(port).sync();
            System.out.println("WebSocket server started on port " + port);

            future.channel().closeFuture().sync();
        } finally {
            shutdown();
        }
    }

    public void shutdown() {
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
    }
}
```

---

## API仕様

### WebSocketメッセージ

#### 接続リクエスト

```json
{
  "type": "connect",
  "version": "1.0",
  "payload": {
    "clientId": "student_001",
    "authToken": "student-token-xyz789",
    "clientInfo": {
      "userAgent": "Scratch 3.0",
      "version": "1.0.0"
    }
  }
}
```

#### 接続レスポンス（成功）

```json
{
  "type": "connect_response",
  "success": true,
  "payload": {
    "sessionId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "clientName": "田中太郎",
    "role": "STUDENT_FULL",
    "permissions": ["CHAT", "PLACE_BLOCK", "SUMMON_ENTITY", "TELEPORT"],
    "serverInfo": {
      "maxClients": 10,
      "currentClients": 3,
      "minecraftVersion": "1.20.1"
    }
  }
}
```

#### クライアント接続通知（ブロードキャスト）

```json
{
  "type": "event",
  "eventType": "client_connected",
  "timestamp": 1699876543210,
  "payload": {
    "sessionId": "...",
    "clientName": "田中太郎",
    "role": "STUDENT_FULL"
  }
}
```

---

## テストシナリオ

### 機能テスト

#### TC-MP-001: 複数クライアント同時接続

```
Given: サーバーが起動している
When: 5つのクライアントが同時に接続する
Then: すべてのクライアントが正常に接続される
And: 各クライアントに一意のセッションIDが割り当てられる
And: すべてのクライアントに接続通知がブロードキャストされる
```

#### TC-MP-002: 権限による操作制限

```
Given: OBSERVER役割のクライアントが接続している
When: ブロック配置コマンドを送信する
Then: "権限が不足しています" エラーが返される
And: ブロックは配置されない
```

#### TC-MP-003: 同一ブロック競合解決

```
Given: Teacher と Student が接続している
When: 両者が同時に座標(100, 64, 50)にブロックを置く
And: Teacherの操作が100ms遅れる
Then: Teacherのブロックが優先される
And: Studentに競合通知が送信される
```

### 負荷テスト

#### LT-MP-001: 10クライアント同時操作

```
Given: 10クライアントが接続している
When: 各クライアントが1秒間に10コマンドを送信する（計100コマンド/秒）
Then: すべてのコマンドが5秒以内に処理される
And: 平均レスポンス時間が100ms未満である
```

---

**作成日**: 2025-11-12
**対象バージョン**: 1.0.0

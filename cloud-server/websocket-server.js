/**
 * Minecraft-Scratch WebSocket Server for Production
 * 本番環境用WebSocketサーバー
 */

const WebSocket = require('ws');
const https = require('https');
const fs = require('fs');
const path = require('path');

// 設定
const config = {
    port: process.env.WS_PORT || 14711,
    ssl: {
        enabled: process.env.SSL_ENABLED === 'true',
        cert: process.env.SSL_CERT_PATH || '/etc/letsencrypt/live/your-domain/fullchain.pem',
        key: process.env.SSL_KEY_PATH || '/etc/letsencrypt/live/your-domain/privkey.pem'
    },
    cors: {
        origin: process.env.CORS_ORIGIN || '*'
    },
    limits: {
        maxConnections: parseInt(process.env.MAX_CONNECTIONS) || 200,
        maxRoomsPerClass: parseInt(process.env.MAX_ROOMS) || 1,
        commandsPerSecond: parseInt(process.env.RATE_LIMIT) || 10
    }
};

// 教室管理
class ClassroomManager {
    constructor() {
        this.classrooms = new Map();
    }

    createClassroom(code) {
        if (!this.classrooms.has(code)) {
            this.classrooms.set(code, {
                code: code,
                students: new Map(),
                created: new Date(),
                lastActivity: new Date()
            });
            console.log(`[Classroom] Created: ${code}`);
        }
        return this.classrooms.get(code);
    }

    joinClassroom(code, clientId, ws) {
        const classroom = this.classrooms.get(code);
        if (classroom) {
            classroom.students.set(clientId, {
                id: clientId,
                ws: ws,
                joined: new Date(),
                minecraft: null,
                scratch: null
            });
            classroom.lastActivity = new Date();
            console.log(`[Classroom] Student ${clientId} joined ${code}`);
            return true;
        }
        return false;
    }

    leaveClassroom(clientId) {
        for (const [code, classroom] of this.classrooms) {
            if (classroom.students.has(clientId)) {
                classroom.students.delete(clientId);
                console.log(`[Classroom] Student ${clientId} left ${code}`);
                
                // 空の教室を削除
                if (classroom.students.size === 0) {
                    this.classrooms.delete(code);
                    console.log(`[Classroom] Deleted empty classroom: ${code}`);
                }
                return;
            }
        }
    }

    broadcast(code, message, excludeId = null) {
        const classroom = this.classrooms.get(code);
        if (classroom) {
            classroom.students.forEach((student, id) => {
                if (id !== excludeId && student.ws.readyState === WebSocket.OPEN) {
                    student.ws.send(JSON.stringify(message));
                }
            });
        }
    }

    getClassroomByClient(clientId) {
        for (const [code, classroom] of this.classrooms) {
            if (classroom.students.has(clientId)) {
                return classroom;
            }
        }
        return null;
    }
}

// レート制限
class RateLimiter {
    constructor(limit, window) {
        this.limit = limit;
        this.window = window;
        this.clients = new Map();
    }

    check(clientId) {
        const now = Date.now();
        const client = this.clients.get(clientId) || { count: 0, resetTime: now + this.window };

        if (now > client.resetTime) {
            client.count = 0;
            client.resetTime = now + this.window;
        }

        client.count++;
        this.clients.set(clientId, client);

        return client.count <= this.limit;
    }

    cleanup() {
        const now = Date.now();
        for (const [id, client] of this.clients) {
            if (now > client.resetTime + 60000) {
                this.clients.delete(id);
            }
        }
    }
}

// メインサーバー
class MinecraftScratchServer {
    constructor() {
        this.clients = new Map();
        this.classroomManager = new ClassroomManager();
        this.rateLimiter = new RateLimiter(config.limits.commandsPerSecond, 1000);
        this.setupServer();
    }

    setupServer() {
        if (config.ssl.enabled) {
            // HTTPS/WSS サーバー
            const server = https.createServer({
                cert: fs.readFileSync(config.ssl.cert),
                key: fs.readFileSync(config.ssl.key)
            });

            this.wss = new WebSocket.Server({ server });
            server.listen(config.port, () => {
                console.log(`[Server] WSS server running on port ${config.port}`);
            });
        } else {
            // WS サーバー（開発用）
            this.wss = new WebSocket.Server({ port: config.port });
            console.log(`[Server] WS server running on port ${config.port}`);
        }

        this.wss.on('connection', this.handleConnection.bind(this));

        // 定期クリーンアップ
        setInterval(() => {
            this.rateLimiter.cleanup();
            this.cleanupInactiveClassrooms();
        }, 60000);
    }

    handleConnection(ws, req) {
        const clientId = this.generateClientId();
        const clientIp = req.headers['x-forwarded-for'] || req.socket.remoteAddress;
        
        console.log(`[Connection] New client: ${clientId} from ${clientIp}`);

        // 接続数チェック
        if (this.clients.size >= config.limits.maxConnections) {
            ws.send(JSON.stringify({
                type: 'error',
                message: 'Server full. Please try again later.',
                message_ja: 'サーバーがいっぱいです。後でもう一度お試しください。'
            }));
            ws.close();
            return;
        }

        // クライアント登録
        this.clients.set(clientId, {
            ws: ws,
            id: clientId,
            ip: clientIp,
            connected: new Date(),
            type: null, // 'minecraft' or 'scratch'
            classroom: null
        });

        // Welcome message
        ws.send(JSON.stringify({
            type: 'welcome',
            clientId: clientId,
            message: 'Connected to Minecraft-Scratch Bridge Server',
            message_ja: 'Minecraft-Scratchブリッジサーバーに接続しました'
        }));

        ws.on('message', (data) => this.handleMessage(clientId, data));
        ws.on('close', () => this.handleDisconnect(clientId));
        ws.on('error', (error) => this.handleError(clientId, error));
    }

    handleMessage(clientId, data) {
        try {
            const client = this.clients.get(clientId);
            if (!client) return;

            // レート制限チェック
            if (!this.rateLimiter.check(clientId)) {
                client.ws.send(JSON.stringify({
                    type: 'error',
                    message: 'Rate limit exceeded',
                    message_ja: 'コマンドが多すぎます。少し待ってください。'
                }));
                return;
            }

            const message = JSON.parse(data.toString());
            console.log(`[Message] From ${clientId}:`, message.type);

            switch (message.type) {
                case 'register':
                    this.handleRegister(clientId, message);
                    break;
                case 'join_classroom':
                    this.handleJoinClassroom(clientId, message);
                    break;
                case 'minecraft_command':
                    this.handleMinecraftCommand(clientId, message);
                    break;
                case 'scratch_action':
                    this.handleScratchAction(clientId, message);
                    break;
                case 'ping':
                    client.ws.send(JSON.stringify({ type: 'pong' }));
                    break;
                default:
                    // 教室内ブロードキャスト
                    this.broadcastToClassroom(clientId, message);
            }
        } catch (error) {
            console.error(`[Error] Processing message from ${clientId}:`, error);
            const client = this.clients.get(clientId);
            if (client) {
                client.ws.send(JSON.stringify({
                    type: 'error',
                    message: 'Invalid message format',
                    message_ja: 'メッセージの形式が正しくありません'
                }));
            }
        }
    }

    handleRegister(clientId, message) {
        const client = this.clients.get(clientId);
        if (client) {
            client.type = message.clientType; // 'minecraft' or 'scratch'
            console.log(`[Register] Client ${clientId} as ${message.clientType}`);
            
            client.ws.send(JSON.stringify({
                type: 'registered',
                clientType: message.clientType,
                message: `Registered as ${message.clientType} client`,
                message_ja: `${message.clientType}クライアントとして登録されました`
            }));
        }
    }

    handleJoinClassroom(clientId, message) {
        const { classroomCode } = message;
        if (!classroomCode) return;

        // 教室を作成または取得
        const classroom = this.classroomManager.createClassroom(classroomCode);
        
        // 教室に参加
        const client = this.clients.get(clientId);
        if (client && this.classroomManager.joinClassroom(classroomCode, clientId, client.ws)) {
            client.classroom = classroomCode;
            
            client.ws.send(JSON.stringify({
                type: 'joined_classroom',
                classroomCode: classroomCode,
                studentCount: classroom.students.size,
                message: `Joined classroom ${classroomCode}`,
                message_ja: `教室 ${classroomCode} に参加しました`
            }));

            // 他の生徒に通知
            this.classroomManager.broadcast(classroomCode, {
                type: 'student_joined',
                studentId: clientId,
                totalStudents: classroom.students.size
            }, clientId);
        }
    }

    handleMinecraftCommand(clientId, message) {
        const client = this.clients.get(clientId);
        if (!client || !client.classroom) return;

        // Scratchクライアントに転送
        this.classroomManager.broadcast(client.classroom, {
            type: 'minecraft_update',
            data: message.data,
            from: clientId
        }, clientId);
    }

    handleScratchAction(clientId, message) {
        const client = this.clients.get(clientId);
        if (!client || !client.classroom) return;

        // Minecraftクライアントに転送
        this.classroomManager.broadcast(client.classroom, {
            type: 'scratch_command',
            command: message.command,
            args: message.args,
            from: clientId
        }, clientId);
    }

    broadcastToClassroom(clientId, message) {
        const client = this.clients.get(clientId);
        if (client && client.classroom) {
            this.classroomManager.broadcast(client.classroom, message, clientId);
        }
    }

    handleDisconnect(clientId) {
        const client = this.clients.get(clientId);
        if (client) {
            console.log(`[Disconnect] Client ${clientId} (${client.type})`);
            
            // 教室から退出
            if (client.classroom) {
                this.classroomManager.leaveClassroom(clientId);
            }
            
            this.clients.delete(clientId);
        }
    }

    handleError(clientId, error) {
        console.error(`[Error] Client ${clientId}:`, error);
    }

    cleanupInactiveClassrooms() {
        const now = new Date();
        const timeout = 30 * 60 * 1000; // 30分

        for (const [code, classroom] of this.classroomManager.classrooms) {
            if (now - classroom.lastActivity > timeout && classroom.students.size === 0) {
                this.classroomManager.classrooms.delete(code);
                console.log(`[Cleanup] Removed inactive classroom: ${code}`);
            }
        }
    }

    generateClientId() {
        return `client_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
    }
}

// サーバー起動
const server = new MinecraftScratchServer();

// Graceful shutdown
process.on('SIGTERM', () => {
    console.log('[Server] Shutting down...');
    server.wss.close(() => {
        process.exit(0);
    });
});

console.log('[Server] Minecraft-Scratch WebSocket Server Started');
console.log('[Server] Configuration:', {
    port: config.port,
    ssl: config.ssl.enabled,
    maxConnections: config.limits.maxConnections
});
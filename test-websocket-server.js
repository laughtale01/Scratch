const WebSocket = require('ws');

const PORT = 14711;
const wss = new WebSocket.Server({ port: PORT });

console.log(`Test WebSocket server listening on port ${PORT}`);

wss.on('connection', (ws, req) => {
    const clientAddress = req.socket.remoteAddress;
    console.log(`[${new Date().toISOString()}] New connection from ${clientAddress}`);
    
    // Send welcome message
    const welcome = {
        type: 'welcome',
        status: 'connected',
        message: 'Test WebSocket Server Ready',
        version: '1.0.0'
    };
    ws.send(JSON.stringify(welcome));
    console.log(`[${new Date().toISOString()}] Sent welcome message`);
    
    ws.on('message', (message) => {
        console.log(`[${new Date().toISOString()}] Received: ${message}`);
        
        try {
            const data = JSON.parse(message);
            console.log(`[${new Date().toISOString()}] Parsed command: ${data.command}`);
            
            let response = {};
            
            switch (data.command) {
                case 'ping':
                    response = {
                        status: 'pong',
                        time: Date.now()
                    };
                    break;
                    
                case 'chat':
                    const chatMessage = Array.isArray(data.args) 
                        ? data.args[0] 
                        : (data.args?.message || 'No message');
                    response = {
                        status: 'success',
                        command: 'chat',
                        message: chatMessage
                    };
                    console.log(`[${new Date().toISOString()}] Chat message: ${chatMessage}`);
                    break;
                    
                case 'getPlayerPos':
                    response = {
                        status: 'success',
                        x: 100,
                        y: 64,
                        z: 200
                    };
                    break;
                    
                default:
                    response = {
                        status: 'error',
                        error: `Unknown command: ${data.command}`
                    };
            }
            
            const responseStr = JSON.stringify(response);
            ws.send(responseStr);
            console.log(`[${new Date().toISOString()}] Sent response: ${responseStr}`);
            
        } catch (e) {
            console.error(`[${new Date().toISOString()}] Error parsing message:`, e);
            
            // Try legacy format
            const match = message.toString().match(/^(\w+)\((.*)\)$/);
            if (match) {
                const [, command, args] = match;
                console.log(`[${new Date().toISOString()}] Legacy command: ${command}, args: ${args}`);
                
                let response = {};
                switch (command) {
                    case 'ping':
                        response = { status: 'pong', time: Date.now() };
                        break;
                    case 'chat':
                        response = { status: 'success', command: 'chat', message: args };
                        break;
                    default:
                        response = { status: 'error', error: `Unknown command: ${command}` };
                }
                
                ws.send(JSON.stringify(response));
            } else {
                ws.send(JSON.stringify({ status: 'error', error: 'Invalid message format' }));
            }
        }
    });
    
    ws.on('close', () => {
        console.log(`[${new Date().toISOString()}] Connection closed from ${clientAddress}`);
    });
    
    ws.on('error', (error) => {
        console.error(`[${new Date().toISOString()}] WebSocket error:`, error);
    });
});

console.log('Test server ready. Send commands like:');
console.log('  {"command":"ping"}');
console.log('  {"command":"chat","args":["Hello"]}');
console.log('  {"command":"getPlayerPos"}');
const http = require('http');
const fs = require('fs');
const path = require('path');

const server = http.createServer((req, res) => {
    if (req.url === '/' || req.url === '/test') {
        fs.readFile(path.join(__dirname, 'test-connection.html'), (err, data) => {
            if (err) {
                res.writeHead(404, {'Content-Type': 'text/html'});
                res.end('<h1>File not found</h1>');
                return;
            }
            res.writeHead(200, {'Content-Type': 'text/html'});
            res.end(data);
        });
    } else {
        res.writeHead(404, {'Content-Type': 'text/html'});
        res.end('<h1>Page not found</h1>');
    }
});

const PORT = 3000;
server.listen(PORT, () => {
    console.log(`Test server running at http://localhost:${PORT}`);
    console.log('Open http://localhost:3000/test to run WebSocket connection test');
});

// WebSocket connection test using Node.js WebSocket client
const WebSocket = require('ws');

function testWebSocketConnection() {
    console.log('\n=== WebSocket Connection Test ===');
    
    try {
        const ws = new WebSocket('ws://localhost:14711');
        
        ws.on('open', function open() {
            console.log('✅ Connected to Minecraft WebSocket server');
            
            // Test 1: Send chat message
            const chatMessage = 'chat(Hello from Node.js test!)';
            ws.send(chatMessage);
            console.log('📤 Sent chat message:', chatMessage);
            
            // Test 2: Send get position command
            setTimeout(() => {
                const positionCommand = 'getPosition()';
                ws.send(positionCommand);
                console.log('📤 Sent position command:', positionCommand);
            }, 1000);
            
            // Test 3: Send connection status
            setTimeout(() => {
                const statusCommand = 'getConnectionStatus()';
                ws.send(statusCommand);
                console.log('📤 Sent status command:', statusCommand);
            }, 2000);
            
            // Close connection after tests
            setTimeout(() => {
                ws.close();
                console.log('📡 Test completed, connection closed');
            }, 5000);
        });
        
        ws.on('message', function message(data) {
            console.log('📨 Received from Minecraft:', data.toString());
        });
        
        ws.on('close', function close(code, reason) {
            console.log('❌ Connection closed:', code, reason.toString());
        });
        
        ws.on('error', function error(err) {
            console.log('❌ WebSocket error:', err.message);
        });
        
    } catch (error) {
        console.log('❌ Failed to create WebSocket:', error.message);
    }
}

// Start WebSocket test after a short delay
setTimeout(testWebSocketConnection, 2000);
const WebSocket = require('ws');

console.log('Testing WebSocket connection to Minecraft mod...');

const ws = new WebSocket('ws://localhost:14711');

ws.on('open', () => {
    console.log('✅ Connected to Minecraft WebSocket server!');
    
    // Test basic commands
    setTimeout(() => {
        console.log('\n1. Testing player position...');
        ws.send('player.getPos()');
    }, 200);
    
    setTimeout(() => {
        console.log('\n2. Testing chat message (legacy format)...');
        ws.send('chat.post(Hello from test script!)');
    }, 400);
    
    setTimeout(() => {
        console.log('\n3. Testing chat message (JSON format)...');
        ws.send(JSON.stringify({
            command: 'chat',
            args: { message: 'Hello from JSON!' }
        }));
    }, 600);
    
    setTimeout(() => {
        console.log('\n4. Testing connection status...');
        ws.send('minecraft.status()');
    }, 800);
    
    // Close after 2 seconds
    setTimeout(() => {
        console.log('Closing connection...');
        ws.close();
    }, 2000);
});

ws.on('message', (data) => {
    const message = data.toString();
    console.log('📨 Received:', message);
    
    // Parse response if it's JSON
    try {
        const json = JSON.parse(message);
        console.log('   JSON Response:', json);
    } catch (e) {
        // Not JSON, that's OK
    }
});

ws.on('error', (error) => {
    console.error('❌ WebSocket error:', error.message);
});

ws.on('close', () => {
    console.log('Connection closed');
    process.exit(0);
});

// Timeout after 5 seconds
setTimeout(() => {
    console.error('❌ Connection timeout - is Minecraft running with the mod?');
    process.exit(1);
}, 5000);
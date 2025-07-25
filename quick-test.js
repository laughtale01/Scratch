const WebSocket = require('ws');

console.log('Testing minecraft.status() fix...');
const ws = new WebSocket('ws://localhost:14711');

ws.on('open', function open() {
    console.log('✅ Connected');
    
    setTimeout(() => {
        console.log('📤 Testing minecraft.status()');
        ws.send('minecraft.status()');
    }, 1000);
    
    setTimeout(() => {
        ws.close();
        console.log('Test completed');
        process.exit(0);
    }, 3000);
});

ws.on('message', function message(data) {
    console.log('📨 Response:', data.toString());
});

ws.on('error', function error(err) {
    console.log('❌ Error:', err.message);
});
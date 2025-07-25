// Minecraft Extension Test Script
// This script tests basic WebSocket connection and commands

const WebSocket = require('ws');

const TEST_PORT = 14711;
const TEST_HOST = 'localhost';

console.log('🧪 Minecraft Extension Test Script');
console.log('==================================');

// Create WebSocket connection
const ws = new WebSocket(`ws://${TEST_HOST}:${TEST_PORT}`);

ws.on('open', () => {
    console.log('✅ Connected to Minecraft WebSocket server');
    
    // Test 1: Send a place block command
    const placeBlockCmd = JSON.stringify({
        command: 'placeBlock',
        args: {
            x: '0',
            y: '64', 
            z: '0',
            block: 'stone'
        }
    });
    
    console.log('📤 Sending placeBlock command...');
    ws.send(placeBlockCmd);
    
    // Test 2: Get player position
    setTimeout(() => {
        const getPosCmd = JSON.stringify({
            command: 'getPlayerPos',
            args: {}
        });
        
        console.log('📤 Sending getPlayerPos command...');
        ws.send(getPosCmd);
    }, 1000);
    
    // Test 3: Send chat message
    setTimeout(() => {
        const chatCmd = JSON.stringify({
            command: 'chat',
            args: {
                message: 'Hello from Scratch test!'
            }
        });
        
        console.log('📤 Sending chat command...');
        ws.send(chatCmd);
    }, 2000);
    
    // Test 4: Build a circle
    setTimeout(() => {
        const circleCmd = JSON.stringify({
            command: 'buildCircle',
            args: {
                x: '10',
                y: '64',
                z: '10',
                radius: '5',
                block: 'glass'
            }
        });
        
        console.log('📤 Sending buildCircle command...');
        ws.send(circleCmd);
    }, 3000);
    
    // Close connection after tests
    setTimeout(() => {
        console.log('🔌 Closing connection...');
        ws.close();
    }, 5000);
});

ws.on('message', (data) => {
    console.log('📥 Received:', data.toString());
});

ws.on('error', (error) => {
    console.error('❌ WebSocket error:', error);
});

ws.on('close', () => {
    console.log('🔌 Connection closed');
    console.log('==================================');
    console.log('✨ Test completed!');
});

// Handle process termination
process.on('SIGINT', () => {
    console.log('\n⚠️  Interrupted by user');
    ws.close();
    process.exit(0);
});
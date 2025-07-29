const WebSocket = require('ws');
const { performance } = require('perf_hooks');

console.log('🔥 Minecraft Collaboration - Load Testing');
console.log('='.repeat(50));

// Configuration
const CONFIG = {
    serverUrl: 'ws://localhost:14711',
    numClients: 50,  // Number of concurrent connections
    testDuration: 30000,  // 30 seconds
    commandInterval: 100,  // Send command every 100ms per client
    reportInterval: 5000   // Report stats every 5 seconds
};

// Statistics
const stats = {
    connections: {
        successful: 0,
        failed: 0,
        closed: 0
    },
    messages: {
        sent: 0,
        received: 0,
        errors: 0
    },
    latency: {
        samples: [],
        min: Infinity,
        max: -Infinity,
        avg: 0
    },
    throughput: {
        messagesPerSecond: 0,
        bytesPerSecond: 0,
        totalBytes: 0
    }
};

// Test commands to cycle through
const testCommands = [
    { command: 'player.getPos', args: {} },
    { command: 'getBlock', args: { x: '0', y: '64', z: '0' } },
    { command: 'placeBlock', args: { x: '10', y: '65', z: '10', block: 'stone' } },
    { command: 'removeBlock', args: { x: '10', y: '65', z: '10' } },
    { command: 'chat.post', args: { message: 'Load test message' } },
    { command: 'getPlayerList', args: {} }
];

class LoadTestClient {
    constructor(id) {
        this.id = id;
        this.ws = null;
        this.connected = false;
        this.commandIndex = 0;
        this.pendingMessages = new Map(); // Track sent messages for latency
        this.messageInterval = null;
        this.bytesReceived = 0;
    }

    connect() {
        return new Promise((resolve, reject) => {
            try {
                this.ws = new WebSocket(CONFIG.serverUrl);
                
                this.ws.on('open', () => {
                    this.connected = true;
                    stats.connections.successful++;
                    console.log(`✅ Client ${this.id} connected`);
                    this.startSendingCommands();
                    resolve();
                });

                this.ws.on('message', (data) => {
                    const receivedTime = performance.now();
                    stats.messages.received++;
                    
                    const message = data.toString();
                    this.bytesReceived += message.length;
                    stats.throughput.totalBytes += message.length;
                    
                    // Try to match response to sent message
                    try {
                        const response = JSON.parse(message);
                        if (response.requestId && this.pendingMessages.has(response.requestId)) {
                            const sentTime = this.pendingMessages.get(response.requestId);
                            const latency = receivedTime - sentTime;
                            
                            stats.latency.samples.push(latency);
                            stats.latency.min = Math.min(stats.latency.min, latency);
                            stats.latency.max = Math.max(stats.latency.max, latency);
                            
                            this.pendingMessages.delete(response.requestId);
                        }
                    } catch (e) {
                        // Not JSON, might be legacy format
                    }
                });

                this.ws.on('error', (error) => {
                    stats.messages.errors++;
                    console.error(`❌ Client ${this.id} error:`, error.message);
                });

                this.ws.on('close', () => {
                    this.connected = false;
                    stats.connections.closed++;
                    this.stopSendingCommands();
                });

                // Timeout for connection
                setTimeout(() => {
                    if (!this.connected) {
                        stats.connections.failed++;
                        reject(new Error(`Client ${this.id} connection timeout`));
                    }
                }, 5000);

            } catch (error) {
                stats.connections.failed++;
                reject(error);
            }
        });
    }

    startSendingCommands() {
        this.messageInterval = setInterval(() => {
            if (this.connected && this.ws.readyState === WebSocket.OPEN) {
                this.sendRandomCommand();
            }
        }, CONFIG.commandInterval);
    }

    stopSendingCommands() {
        if (this.messageInterval) {
            clearInterval(this.messageInterval);
            this.messageInterval = null;
        }
    }

    sendRandomCommand() {
        const command = testCommands[this.commandIndex % testCommands.length];
        this.commandIndex++;
        
        const requestId = `${this.id}-${Date.now()}-${Math.random()}`;
        const message = JSON.stringify({
            ...command,
            requestId
        });
        
        const sentTime = performance.now();
        this.pendingMessages.set(requestId, sentTime);
        
        try {
            this.ws.send(message);
            stats.messages.sent++;
            
            // Clean up old pending messages (older than 10 seconds)
            const cutoffTime = sentTime - 10000;
            for (const [id, time] of this.pendingMessages.entries()) {
                if (time < cutoffTime) {
                    this.pendingMessages.delete(id);
                }
            }
        } catch (error) {
            stats.messages.errors++;
        }
    }

    disconnect() {
        this.stopSendingCommands();
        if (this.ws) {
            this.ws.close();
        }
    }
}

// Load test orchestration
async function runLoadTest() {
    console.log('\n📊 Test Configuration:');
    console.log(`- Concurrent clients: ${CONFIG.numClients}`);
    console.log(`- Test duration: ${CONFIG.testDuration / 1000} seconds`);
    console.log(`- Command interval: ${CONFIG.commandInterval}ms per client`);
    console.log('\nStarting load test...\n');

    const clients = [];
    const startTime = performance.now();

    // Create and connect clients
    console.log('🚀 Spawning clients...');
    for (let i = 0; i < CONFIG.numClients; i++) {
        const client = new LoadTestClient(i);
        clients.push(client);
        
        // Stagger connections slightly to avoid overwhelming the server
        await new Promise(resolve => setTimeout(resolve, 50));
        
        client.connect().catch(error => {
            console.error(`Failed to connect client ${i}:`, error.message);
        });
    }

    // Wait for connections to establish
    await new Promise(resolve => setTimeout(resolve, 2000));

    // Report statistics periodically
    const reportInterval = setInterval(() => {
        reportStatistics(startTime);
    }, CONFIG.reportInterval);

    // Run test for specified duration
    await new Promise(resolve => setTimeout(resolve, CONFIG.testDuration));

    // Stop the test
    console.log('\n🛑 Stopping load test...');
    clearInterval(reportInterval);

    // Disconnect all clients
    clients.forEach(client => client.disconnect());

    // Wait for cleanup
    await new Promise(resolve => setTimeout(resolve, 1000));

    // Final report
    console.log('\n' + '='.repeat(50));
    console.log('📈 FINAL LOAD TEST RESULTS');
    console.log('='.repeat(50));
    reportStatistics(startTime, true);
}

function reportStatistics(startTime, isFinal = false) {
    const elapsedTime = (performance.now() - startTime) / 1000; // seconds
    
    // Calculate averages
    const avgLatency = stats.latency.samples.length > 0
        ? stats.latency.samples.reduce((a, b) => a + b, 0) / stats.latency.samples.length
        : 0;
    
    const messagesPerSecond = stats.messages.sent / elapsedTime;
    const bytesPerSecond = stats.throughput.totalBytes / elapsedTime;
    
    console.log(`\n⏱️  Elapsed Time: ${elapsedTime.toFixed(1)}s`);
    console.log('\n📊 Connection Statistics:');
    console.log(`   ✅ Successful: ${stats.connections.successful}`);
    console.log(`   ❌ Failed: ${stats.connections.failed}`);
    console.log(`   🔌 Closed: ${stats.connections.closed}`);
    console.log(`   📍 Active: ${stats.connections.successful - stats.connections.closed}`);
    
    console.log('\n📨 Message Statistics:');
    console.log(`   📤 Sent: ${stats.messages.sent}`);
    console.log(`   📥 Received: ${stats.messages.received}`);
    console.log(`   ❌ Errors: ${stats.messages.errors}`);
    console.log(`   📊 Success Rate: ${((stats.messages.received / stats.messages.sent) * 100).toFixed(2)}%`);
    
    console.log('\n⚡ Performance Metrics:');
    console.log(`   🔄 Messages/sec: ${messagesPerSecond.toFixed(2)}`);
    console.log(`   📦 Throughput: ${(bytesPerSecond / 1024).toFixed(2)} KB/s`);
    console.log(`   ⏱️  Avg Latency: ${avgLatency.toFixed(2)}ms`);
    console.log(`   ⬇️  Min Latency: ${stats.latency.min === Infinity ? 'N/A' : stats.latency.min.toFixed(2) + 'ms'}`);
    console.log(`   ⬆️  Max Latency: ${stats.latency.max === -Infinity ? 'N/A' : stats.latency.max.toFixed(2) + 'ms'}`);
    
    if (isFinal) {
        console.log('\n🏁 Test Summary:');
        console.log(`   Total duration: ${elapsedTime.toFixed(1)}s`);
        console.log(`   Total messages: ${stats.messages.sent + stats.messages.received}`);
        console.log(`   Total data transferred: ${(stats.throughput.totalBytes / 1024 / 1024).toFixed(2)} MB`);
        
        // Performance rating
        let rating = '⭐⭐⭐⭐⭐';
        if (avgLatency > 100) rating = '⭐⭐⭐⭐';
        if (avgLatency > 200) rating = '⭐⭐⭐';
        if (avgLatency > 500) rating = '⭐⭐';
        if (avgLatency > 1000) rating = '⭐';
        
        console.log(`\n🏆 Performance Rating: ${rating}`);
        
        // Recommendations
        console.log('\n💡 Recommendations:');
        if (avgLatency > 100) {
            console.log('   - Consider optimizing message processing');
        }
        if (stats.messages.errors > stats.messages.sent * 0.01) {
            console.log('   - High error rate detected, check server stability');
        }
        if (stats.connections.failed > CONFIG.numClients * 0.1) {
            console.log('   - Many connection failures, check server capacity');
        }
        if (messagesPerSecond < CONFIG.numClients * 5) {
            console.log('   - Low throughput, consider performance optimization');
        }
    }
}

// Error handling
process.on('unhandledRejection', (error) => {
    console.error('❌ Unhandled error:', error);
    process.exit(1);
});

// Run the load test
runLoadTest().catch(error => {
    console.error('❌ Load test failed:', error);
    process.exit(1);
});
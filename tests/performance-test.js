const WebSocket = require('ws');

console.log('⚡ Performance Test');
console.log('==================');

const WS_URL = 'ws://localhost:14711';

// Performance metrics
const metrics = {
    connectionTime: [],
    responseTime: [],
    throughput: [],
    memoryUsage: [],
    errors: 0
};

// Test configuration
const config = {
    warmupRequests: 10,
    testRequests: 100,
    concurrentConnections: 5,
    payloadSizes: [100, 1000, 5000, 10000],  // bytes
    testDuration: 30000  // 30 seconds
};

// Helper functions
function measureTime(startTime) {
    return Date.now() - startTime;
}

function formatBytes(bytes) {
    return (bytes / 1024 / 1024).toFixed(2) + ' MB';
}

function calculateStats(values) {
    if (values.length === 0) return { avg: 0, min: 0, max: 0, p95: 0 };
    
    const sorted = values.sort((a, b) => a - b);
    const avg = values.reduce((a, b) => a + b, 0) / values.length;
    const min = sorted[0];
    const max = sorted[sorted.length - 1];
    const p95Index = Math.floor(values.length * 0.95);
    const p95 = sorted[p95Index] || max;
    
    return { avg, min, max, p95 };
}

// Test 1: Connection establishment time
async function testConnectionTime() {
    console.log('\n📊 Test 1: Connection Establishment Time');
    console.log('=======================================');
    
    const times = [];
    
    for (let i = 0; i < 10; i++) {
        const startTime = Date.now();
        
        await new Promise((resolve, reject) => {
            const ws = new WebSocket(WS_URL);
            
            ws.on('open', () => {
                const connectionTime = measureTime(startTime);
                times.push(connectionTime);
                ws.close();
                resolve();
            });
            
            ws.on('error', reject);
            
            setTimeout(() => reject(new Error('Connection timeout')), 5000);
        }).catch(() => {
            metrics.errors++;
        });
    }
    
    const stats = calculateStats(times);
    console.log(`Average: ${stats.avg.toFixed(2)}ms`);
    console.log(`Min: ${stats.min}ms, Max: ${stats.max}ms`);
    console.log(`95th percentile: ${stats.p95.toFixed(2)}ms`);
    
    metrics.connectionTime = times;
}

// Test 2: Response time for various operations
async function testResponseTime() {
    console.log('\n📊 Test 2: Response Time');
    console.log('=======================');
    
    const operations = [
        { name: 'getPlayerPos', command: { command: 'getPlayerPos', args: {} } },
        { name: 'placeBlock', command: { command: 'placeBlock', args: { x: '0', y: '64', z: '0', block: 'stone' } } },
        { name: 'getBlock', command: { command: 'getBlock', args: { x: '0', y: '64', z: '0' } } },
        { name: 'chat', command: { command: 'chat', args: { message: 'Performance test' } } }
    ];
    
    const ws = new WebSocket(WS_URL);
    await new Promise(resolve => ws.on('open', resolve));
    
    for (const op of operations) {
        const times = [];
        
        console.log(`\nTesting ${op.name}...`);
        
        // Warmup
        for (let i = 0; i < config.warmupRequests; i++) {
            await sendAndMeasure(ws, op.command);
        }
        
        // Actual test
        for (let i = 0; i < config.testRequests; i++) {
            const time = await sendAndMeasure(ws, op.command);
            if (time > 0) times.push(time);
        }
        
        const stats = calculateStats(times);
        console.log(`  Average: ${stats.avg.toFixed(2)}ms`);
        console.log(`  Min: ${stats.min}ms, Max: ${stats.max}ms`);
        console.log(`  95th percentile: ${stats.p95.toFixed(2)}ms`);
        
        metrics.responseTime.push({ operation: op.name, stats });
    }
    
    ws.close();
}

async function sendAndMeasure(ws, command) {
    return new Promise((resolve) => {
        const startTime = Date.now();
        let responded = false;
        
        const messageHandler = () => {
            if (!responded) {
                responded = true;
                const responseTime = measureTime(startTime);
                ws.removeListener('message', messageHandler);
                resolve(responseTime);
            }
        };
        
        ws.on('message', messageHandler);
        ws.send(JSON.stringify(command));
        
        setTimeout(() => {
            if (!responded) {
                ws.removeListener('message', messageHandler);
                metrics.errors++;
                resolve(-1);
            }
        }, 5000);
    });
}

// Test 3: Throughput test
async function testThroughput() {
    console.log('\n📊 Test 3: Throughput Test');
    console.log('=========================');
    
    const ws = new WebSocket(WS_URL);
    await new Promise(resolve => ws.on('open', resolve));
    
    const testDuration = 10000; // 10 seconds
    const startTime = Date.now();
    let commandsSent = 0;
    let responsesReceived = 0;
    
    console.log('Running throughput test for 10 seconds...');
    
    // Send commands continuously
    const sendInterval = setInterval(() => {
        if (Date.now() - startTime < testDuration) {
            ws.send(JSON.stringify({
                command: 'getPlayerPos',
                args: {}
            }));
            commandsSent++;
        } else {
            clearInterval(sendInterval);
        }
    }, 10); // Send every 10ms
    
    ws.on('message', () => {
        responsesReceived++;
    });
    
    await new Promise(resolve => setTimeout(resolve, testDuration + 1000));
    
    ws.close();
    
    const duration = (Date.now() - startTime) / 1000;
    const throughput = responsesReceived / duration;
    
    console.log(`Commands sent: ${commandsSent}`);
    console.log(`Responses received: ${responsesReceived}`);
    console.log(`Success rate: ${(responsesReceived / commandsSent * 100).toFixed(2)}%`);
    console.log(`Throughput: ${throughput.toFixed(2)} responses/second`);
    
    metrics.throughput.push({ commandsSent, responsesReceived, throughput });
}

// Test 4: Concurrent connections performance
async function testConcurrentConnections() {
    console.log('\n📊 Test 4: Concurrent Connections');
    console.log('================================');
    
    const connectionCounts = [1, 5, 10, 20];
    
    for (const count of connectionCounts) {
        console.log(`\nTesting with ${count} concurrent connections...`);
        
        const connections = [];
        const results = [];
        
        // Create connections
        for (let i = 0; i < count; i++) {
            const ws = new WebSocket(WS_URL);
            connections.push(ws);
            
            await new Promise((resolve, reject) => {
                ws.on('open', resolve);
                ws.on('error', reject);
                setTimeout(() => reject(new Error('Connection timeout')), 5000);
            }).catch(() => {
                metrics.errors++;
            });
        }
        
        // Send commands from all connections
        const startTime = Date.now();
        const promises = connections.map(async (ws, index) => {
            const times = [];
            
            for (let i = 0; i < 10; i++) {
                const time = await sendAndMeasure(ws, {
                    command: 'getPlayerPos',
                    args: {}
                });
                if (time > 0) times.push(time);
            }
            
            return times;
        });
        
        const allTimes = await Promise.all(promises);
        const totalTime = measureTime(startTime);
        
        // Close all connections
        connections.forEach(ws => ws.close());
        
        // Calculate average response time across all connections
        const flatTimes = allTimes.flat();
        const stats = calculateStats(flatTimes);
        
        console.log(`  Total time: ${totalTime}ms`);
        console.log(`  Average response: ${stats.avg.toFixed(2)}ms`);
        console.log(`  Success rate: ${(flatTimes.length / (count * 10) * 100).toFixed(2)}%`);
    }
}

// Test 5: Large payload handling
async function testLargePayloads() {
    console.log('\n📊 Test 5: Large Payload Handling');
    console.log('=================================');
    
    const ws = new WebSocket(WS_URL);
    await new Promise(resolve => ws.on('open', resolve));
    
    for (const size of config.payloadSizes) {
        console.log(`\nTesting ${size} byte payload...`);
        
        const largeMessage = 'A'.repeat(size);
        const times = [];
        
        for (let i = 0; i < 10; i++) {
            const startTime = Date.now();
            
            const responseTime = await new Promise((resolve) => {
                let responded = false;
                
                const messageHandler = () => {
                    if (!responded) {
                        responded = true;
                        ws.removeListener('message', messageHandler);
                        resolve(measureTime(startTime));
                    }
                };
                
                ws.on('message', messageHandler);
                ws.send(JSON.stringify({
                    command: 'chat',
                    args: { message: largeMessage }
                }));
                
                setTimeout(() => {
                    if (!responded) {
                        ws.removeListener('message', messageHandler);
                        resolve(-1);
                    }
                }, 10000);
            });
            
            if (responseTime > 0) {
                times.push(responseTime);
            } else {
                metrics.errors++;
            }
        }
        
        const stats = calculateStats(times);
        console.log(`  Average response: ${stats.avg.toFixed(2)}ms`);
        console.log(`  Success rate: ${(times.length / 10 * 100).toFixed(2)}%`);
    }
    
    ws.close();
}

// Test 6: Memory usage monitoring
async function testMemoryUsage() {
    console.log('\n📊 Test 6: Memory Usage');
    console.log('======================');
    
    const initialMemory = process.memoryUsage();
    console.log(`Initial memory: ${formatBytes(initialMemory.heapUsed)}`);
    
    // Create multiple connections and send many messages
    const connections = [];
    
    for (let i = 0; i < 10; i++) {
        const ws = new WebSocket(WS_URL);
        connections.push(ws);
        await new Promise(resolve => ws.on('open', resolve));
    }
    
    // Send many messages
    for (let i = 0; i < 1000; i++) {
        const ws = connections[i % connections.length];
        ws.send(JSON.stringify({
            command: 'getPlayerPos',
            args: {}
        }));
    }
    
    await new Promise(resolve => setTimeout(resolve, 5000));
    
    const afterMemory = process.memoryUsage();
    console.log(`After test memory: ${formatBytes(afterMemory.heapUsed)}`);
    console.log(`Memory increase: ${formatBytes(afterMemory.heapUsed - initialMemory.heapUsed)}`);
    
    // Close connections
    connections.forEach(ws => ws.close());
    
    // Check memory after cleanup
    global.gc && global.gc(); // Force GC if available
    await new Promise(resolve => setTimeout(resolve, 2000));
    
    const finalMemory = process.memoryUsage();
    console.log(`Final memory: ${formatBytes(finalMemory.heapUsed)}`);
    
    metrics.memoryUsage = {
        initial: initialMemory.heapUsed,
        peak: afterMemory.heapUsed,
        final: finalMemory.heapUsed
    };
}

// Run all performance tests
async function runAllPerformanceTests() {
    console.log('Starting performance tests...');
    console.log(`Target: ${WS_URL}`);
    console.log('Note: Ensure Minecraft with the mod is running!\n');
    
    const startTime = Date.now();
    
    try {
        await testConnectionTime();
        await testResponseTime();
        await testThroughput();
        await testConcurrentConnections();
        await testLargePayloads();
        await testMemoryUsage();
        
        const totalTime = measureTime(startTime);
        
        // Summary
        console.log('\n' + '='.repeat(50));
        console.log('⚡ Performance Test Summary:');
        console.log(`Total test duration: ${(totalTime / 1000).toFixed(2)} seconds`);
        console.log(`Total errors: ${metrics.errors}`);
        
        // Connection performance
        if (metrics.connectionTime.length > 0) {
            const connStats = calculateStats(metrics.connectionTime);
            console.log(`\nConnection time: avg ${connStats.avg.toFixed(2)}ms (p95: ${connStats.p95.toFixed(2)}ms)`);
        }
        
        // Response time summary
        console.log('\nResponse times by operation:');
        metrics.responseTime.forEach(({ operation, stats }) => {
            console.log(`  ${operation}: avg ${stats.avg.toFixed(2)}ms (p95: ${stats.p95.toFixed(2)}ms)`);
        });
        
        // Throughput summary
        if (metrics.throughput.length > 0) {
            const throughput = metrics.throughput[0];
            console.log(`\nThroughput: ${throughput.throughput.toFixed(2)} ops/sec`);
        }
        
        // Memory usage
        if (metrics.memoryUsage) {
            const memIncrease = metrics.memoryUsage.peak - metrics.memoryUsage.initial;
            console.log(`\nMemory usage increase: ${formatBytes(memIncrease)}`);
        }
        
        // Performance rating
        const avgResponseTime = metrics.responseTime.reduce((sum, r) => sum + r.stats.avg, 0) / metrics.responseTime.length;
        
        console.log('\n🏆 Performance Rating:');
        if (avgResponseTime < 50 && metrics.errors < 5) {
            console.log('⭐⭐⭐⭐⭐ Excellent - Very fast and reliable');
        } else if (avgResponseTime < 100 && metrics.errors < 10) {
            console.log('⭐⭐⭐⭐ Good - Fast with minimal errors');
        } else if (avgResponseTime < 200 && metrics.errors < 20) {
            console.log('⭐⭐⭐ Acceptable - Reasonable performance');
        } else {
            console.log('⭐⭐ Needs Improvement - Consider optimization');
        }
        
        process.exit(0);
        
    } catch (error) {
        console.error('\n💥 Performance test error:', error);
        process.exit(1);
    }
}

// Execute tests
runAllPerformanceTests();
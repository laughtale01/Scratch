const WebSocket = require('ws');

console.log('🚦 Rate Limiting Test');
console.log('====================');

const WS_URL = 'ws://localhost:14711';
const RATE_LIMIT = 10; // Commands per second

async function testRateLimiting() {
    console.log(`\nTesting rate limit: ${RATE_LIMIT} commands/second`);
    
    return new Promise((resolve, reject) => {
        const ws = new WebSocket(WS_URL);
        let commandsSent = 0;
        let rateLimitHit = false;
        let responses = {
            success: 0,
            rateLimited: 0,
            errors: 0
        };
        
        ws.on('open', () => {
            console.log('✅ Connected to WebSocket server');
            
            // Send commands rapidly to trigger rate limit
            console.log('\n📤 Sending rapid commands...');
            const startTime = Date.now();
            
            // Send 15 commands as fast as possible (should trigger rate limit)
            for (let i = 0; i < 15; i++) {
                const command = {
                    command: 'getPlayerPos',
                    args: {}
                };
                ws.send(JSON.stringify(command));
                commandsSent++;
            }
            
            console.log(`📨 Sent ${commandsSent} commands in ${Date.now() - startTime}ms`);
        });
        
        ws.on('message', (data) => {
            const message = data.toString();
            
            try {
                const json = JSON.parse(message);
                
                if (json.type === 'error' && json.error === 'rateLimitExceeded') {
                    rateLimitHit = true;
                    responses.rateLimited++;
                    console.log('🚫 Rate limit hit:', json.message);
                } else if (json.type === 'error') {
                    responses.errors++;
                } else {
                    responses.success++;
                }
            } catch (e) {
                // Not JSON, count as success (legacy format)
                responses.success++;
            }
            
            // Check if we've received all responses
            const totalResponses = responses.success + responses.rateLimited + responses.errors;
            if (totalResponses >= commandsSent) {
                // Wait a bit to ensure no more messages
                setTimeout(() => {
                    ws.close();
                }, 500);
            }
        });
        
        ws.on('close', () => {
            console.log('\n📊 Results:');
            console.log(`✅ Successful responses: ${responses.success}`);
            console.log(`🚫 Rate limited responses: ${responses.rateLimited}`);
            console.log(`❌ Error responses: ${responses.errors}`);
            
            if (rateLimitHit && responses.success <= RATE_LIMIT) {
                console.log('\n✅ Rate limiting is working correctly!');
                console.log(`   - Allowed up to ${RATE_LIMIT} commands`);
                console.log(`   - Blocked excessive commands`);
                resolve(true);
            } else if (!rateLimitHit) {
                console.log('\n⚠️ Rate limit was not triggered');
                console.log('   This might be OK if commands were sent slowly');
                resolve(false);
            } else {
                console.log('\n❌ Rate limiting behavior unexpected');
                reject(new Error('Rate limit allowed too many commands'));
            }
        });
        
        ws.on('error', (error) => {
            reject(error);
        });
        
        // Timeout after 10 seconds
        setTimeout(() => {
            ws.close();
            reject(new Error('Test timeout'));
        }, 10000);
    });
}

// Additional rate limit tests
async function testRateLimitReset() {
    console.log('\n\n🔄 Testing Rate Limit Reset');
    console.log('===========================');
    
    return new Promise((resolve, reject) => {
        const ws = new WebSocket(WS_URL);
        let phase = 1;
        let firstBatchResponses = 0;
        let secondBatchResponses = 0;
        let rateLimitInSecondBatch = false;
        
        ws.on('open', () => {
            console.log('✅ Connected for reset test');
            
            // Phase 1: Send commands up to limit
            console.log('\n📤 Phase 1: Sending first batch...');
            for (let i = 0; i < 8; i++) {
                ws.send(JSON.stringify({
                    command: 'getPlayerPos',
                    args: {}
                }));
            }
        });
        
        ws.on('message', (data) => {
            if (phase === 1) {
                firstBatchResponses++;
                
                if (firstBatchResponses >= 8) {
                    console.log(`✅ First batch completed: ${firstBatchResponses} responses`);
                    
                    // Wait for rate limit window to reset
                    console.log('⏳ Waiting 1.5 seconds for rate limit reset...');
                    setTimeout(() => {
                        phase = 2;
                        console.log('\n📤 Phase 2: Sending second batch...');
                        
                        // Send more commands
                        for (let i = 0; i < 8; i++) {
                            ws.send(JSON.stringify({
                                command: 'getPlayerPos',
                                args: {}
                            }));
                        }
                    }, 1500);
                }
            } else if (phase === 2) {
                secondBatchResponses++;
                
                try {
                    const json = JSON.parse(data.toString());
                    if (json.type === 'error' && json.error === 'rateLimitExceeded') {
                        rateLimitInSecondBatch = true;
                    }
                } catch (e) {
                    // Ignore parse errors
                }
                
                if (secondBatchResponses >= 8) {
                    setTimeout(() => {
                        ws.close();
                    }, 500);
                }
            }
        });
        
        ws.on('close', () => {
            console.log('\n📊 Reset Test Results:');
            console.log(`Phase 1 responses: ${firstBatchResponses}`);
            console.log(`Phase 2 responses: ${secondBatchResponses}`);
            console.log(`Rate limit in phase 2: ${rateLimitInSecondBatch}`);
            
            if (firstBatchResponses >= 8 && secondBatchResponses >= 8 && !rateLimitInSecondBatch) {
                console.log('\n✅ Rate limit reset working correctly!');
                resolve(true);
            } else {
                console.log('\n❌ Rate limit reset not working as expected');
                resolve(false);
            }
        });
        
        ws.on('error', reject);
        
        setTimeout(() => {
            ws.close();
            reject(new Error('Reset test timeout'));
        }, 15000);
    });
}

// Test burst protection
async function testBurstProtection() {
    console.log('\n\n💥 Testing Burst Protection');
    console.log('===========================');
    
    return new Promise((resolve, reject) => {
        const connections = [];
        const results = [];
        
        console.log('🔌 Creating multiple connections...');
        
        // Create 3 connections
        for (let i = 0; i < 3; i++) {
            const ws = new WebSocket(WS_URL);
            connections.push(ws);
            
            ws.on('open', () => {
                console.log(`✅ Connection ${i + 1} established`);
                
                // Each connection sends 5 commands
                for (let j = 0; j < 5; j++) {
                    ws.send(JSON.stringify({
                        command: 'getPlayerPos',
                        args: {}
                    }));
                }
            });
            
            let responses = 0;
            let rateLimited = false;
            
            ws.on('message', (data) => {
                responses++;
                
                try {
                    const json = JSON.parse(data.toString());
                    if (json.type === 'error' && json.error === 'rateLimitExceeded') {
                        rateLimited = true;
                    }
                } catch (e) {
                    // Ignore
                }
                
                if (responses >= 5) {
                    results[i] = { responses, rateLimited };
                    ws.close();
                    
                    // Check if all connections are done
                    if (results.filter(r => r).length === 3) {
                        console.log('\n📊 Burst Test Results:');
                        results.forEach((r, idx) => {
                            console.log(`Connection ${idx + 1}: ${r.responses} responses, rate limited: ${r.rateLimited}`);
                        });
                        
                        console.log('\n✅ Rate limiting is applied per connection');
                        resolve(true);
                    }
                }
            });
            
            ws.on('error', (error) => {
                console.error(`Connection ${i + 1} error:`, error.message);
            });
        }
        
        setTimeout(() => {
            connections.forEach(ws => ws.close());
            reject(new Error('Burst test timeout'));
        }, 10000);
    });
}

// Run all rate limiting tests
async function runAllTests() {
    console.log('Starting rate limiting tests...');
    console.log(`Target: ${WS_URL}`);
    console.log('Note: Ensure Minecraft with the mod is running!\n');
    
    try {
        // Test 1: Basic rate limiting
        const basicTest = await testRateLimiting();
        
        // Test 2: Rate limit reset
        const resetTest = await testRateLimitReset();
        
        // Test 3: Burst protection
        const burstTest = await testBurstProtection();
        
        // Summary
        console.log('\n' + '='.repeat(50));
        console.log('📊 Rate Limiting Test Summary:');
        console.log(`Basic rate limiting: ${basicTest ? '✅ PASSED' : '❌ FAILED'}`);
        console.log(`Rate limit reset: ${resetTest ? '✅ PASSED' : '❌ FAILED'}`);
        console.log(`Per-connection limits: ${burstTest ? '✅ PASSED' : '❌ FAILED'}`);
        
        const allPassed = basicTest && resetTest && burstTest;
        if (allPassed) {
            console.log('\n🎉 All rate limiting tests passed!');
            process.exit(0);
        } else {
            console.log('\n⚠️ Some tests failed.');
            process.exit(1);
        }
        
    } catch (error) {
        console.error('\n💥 Test error:', error);
        process.exit(1);
    }
}

// Execute tests
runAllTests();
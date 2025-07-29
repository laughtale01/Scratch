const WebSocket = require('ws');

console.log('🧪 WebSocket Communication Test');
console.log('================================');

const WS_URL = 'ws://localhost:14711';
let testsPassed = 0;
let testsFailed = 0;

// Test helper functions
function test(name, fn) {
    console.log(`\n📝 Test: ${name}`);
    return fn()
        .then(() => {
            console.log(`✅ PASSED: ${name}`);
            testsPassed++;
        })
        .catch(error => {
            console.error(`❌ FAILED: ${name}`);
            console.error(`   Error: ${error.message}`);
            testsFailed++;
        });
}

function delay(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

// Main test suite
async function runTests() {
    // Test 1: Basic connection
    await test('WebSocket connection', () => {
        return new Promise((resolve, reject) => {
            const ws = new WebSocket(WS_URL);
            
            ws.on('open', () => {
                ws.close();
                resolve();
            });
            
            ws.on('error', (error) => {
                reject(new Error(`Connection failed: ${error.message}`));
            });
            
            setTimeout(() => {
                reject(new Error('Connection timeout'));
            }, 5000);
        });
    });

    // Test 2: Welcome message
    await test('Welcome message reception', () => {
        return new Promise((resolve, reject) => {
            const ws = new WebSocket(WS_URL);
            let receivedWelcome = false;
            
            ws.on('message', (data) => {
                const message = data.toString();
                try {
                    const json = JSON.parse(message);
                    if (json.type === 'welcome') {
                        receivedWelcome = true;
                        ws.close();
                        resolve();
                    }
                } catch (e) {
                    // Not JSON, ignore
                }
            });
            
            ws.on('close', () => {
                if (!receivedWelcome) {
                    reject(new Error('No welcome message received'));
                }
            });
            
            setTimeout(() => {
                ws.close();
                reject(new Error('Welcome message timeout'));
            }, 5000);
        });
    });

    // Test 3: JSON command format
    await test('JSON command format', () => {
        return new Promise((resolve, reject) => {
            const ws = new WebSocket(WS_URL);
            
            ws.on('open', () => {
                const command = {
                    command: 'getPlayerPos',
                    args: {}
                };
                ws.send(JSON.stringify(command));
            });
            
            ws.on('message', (data) => {
                const message = data.toString();
                try {
                    const json = JSON.parse(message);
                    if (json.type === 'playerPos' || json.type === 'error') {
                        ws.close();
                        resolve();
                    }
                } catch (e) {
                    // Not JSON, ignore
                }
            });
            
            setTimeout(() => {
                ws.close();
                reject(new Error('No response to JSON command'));
            }, 5000);
        });
    });

    // Test 4: Legacy command format
    await test('Legacy command format', () => {
        return new Promise((resolve, reject) => {
            const ws = new WebSocket(WS_URL);
            
            ws.on('open', () => {
                ws.send('player.getPos()');
            });
            
            ws.on('message', (data) => {
                const message = data.toString();
                if (message.includes('playerPos') || message.includes('error')) {
                    ws.close();
                    resolve();
                }
            });
            
            setTimeout(() => {
                ws.close();
                reject(new Error('No response to legacy command'));
            }, 5000);
        });
    });

    // Test 5: Invalid command handling
    await test('Invalid command handling', () => {
        return new Promise((resolve, reject) => {
            const ws = new WebSocket(WS_URL);
            
            ws.on('open', () => {
                ws.send('invalidCommand()');
            });
            
            ws.on('message', (data) => {
                const message = data.toString();
                if (message.includes('error') && message.includes('unknownCommand')) {
                    ws.close();
                    resolve();
                } else {
                    ws.close();
                    reject(new Error('Invalid command not properly handled'));
                }
            });
            
            setTimeout(() => {
                ws.close();
                reject(new Error('No error response for invalid command'));
            }, 5000);
        });
    });

    // Test 6: Multiple concurrent connections
    await test('Multiple concurrent connections', () => {
        return new Promise((resolve, reject) => {
            const connections = [];
            const numConnections = 5;
            let connected = 0;
            
            for (let i = 0; i < numConnections; i++) {
                const ws = new WebSocket(WS_URL);
                connections.push(ws);
                
                ws.on('open', () => {
                    connected++;
                    if (connected === numConnections) {
                        // All connected, close them
                        connections.forEach(conn => conn.close());
                        resolve();
                    }
                });
                
                ws.on('error', (error) => {
                    reject(new Error(`Connection ${i} failed: ${error.message}`));
                });
            }
            
            setTimeout(() => {
                connections.forEach(conn => conn.close());
                reject(new Error(`Only ${connected}/${numConnections} connections established`));
            }, 10000);
        });
    });

    // Test 7: Message size handling
    await test('Large message handling', () => {
        return new Promise((resolve, reject) => {
            const ws = new WebSocket(WS_URL);
            
            ws.on('open', () => {
                // Send a large chat message
                const largeMessage = 'A'.repeat(500);
                const command = {
                    command: 'chat',
                    args: {
                        message: largeMessage
                    }
                };
                ws.send(JSON.stringify(command));
            });
            
            ws.on('message', (data) => {
                const message = data.toString();
                // Should receive some response (success or error)
                if (message.includes('chat') || message.includes('error')) {
                    ws.close();
                    resolve();
                }
            });
            
            setTimeout(() => {
                ws.close();
                reject(new Error('No response to large message'));
            }, 5000);
        });
    });

    // Test 8: Connection persistence
    await test('Connection persistence', () => {
        return new Promise((resolve, reject) => {
            const ws = new WebSocket(WS_URL);
            let messageCount = 0;
            
            ws.on('open', () => {
                // Send multiple commands
                const interval = setInterval(() => {
                    if (messageCount >= 3) {
                        clearInterval(interval);
                        ws.close();
                        resolve();
                        return;
                    }
                    
                    ws.send(JSON.stringify({
                        command: 'getPlayerPos',
                        args: {}
                    }));
                }, 1000);
            });
            
            ws.on('message', () => {
                messageCount++;
            });
            
            ws.on('close', () => {
                if (messageCount < 3) {
                    reject(new Error('Connection closed prematurely'));
                }
            });
            
            setTimeout(() => {
                ws.close();
                reject(new Error('Connection persistence test timeout'));
            }, 10000);
        });
    });
}

// Run all tests
console.log('Starting WebSocket communication tests...');
console.log(`Target: ${WS_URL}`);
console.log('Note: Ensure Minecraft with the mod is running!');

runTests()
    .then(() => {
        console.log('\n' + '='.repeat(40));
        console.log('📊 Test Results:');
        console.log(`✅ Passed: ${testsPassed}`);
        console.log(`❌ Failed: ${testsFailed}`);
        console.log(`📈 Success Rate: ${Math.round(testsPassed / (testsPassed + testsFailed) * 100)}%`);
        
        if (testsFailed === 0) {
            console.log('\n🎉 All tests passed!');
            process.exit(0);
        } else {
            console.log('\n⚠️ Some tests failed. Check the logs above.');
            process.exit(1);
        }
    })
    .catch(error => {
        console.error('\n💥 Test suite error:', error);
        process.exit(1);
    });
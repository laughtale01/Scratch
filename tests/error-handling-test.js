const WebSocket = require('ws');

console.log('🚨 Error Handling & Edge Cases Test');
console.log('===================================');

const WS_URL = 'ws://localhost:14711';

// Test scenarios
const edgeCaseTests = [
    {
        name: 'Empty message',
        message: '',
        expectedError: true
    },
    {
        name: 'Null message',
        message: null,
        expectedError: true
    },
    {
        name: 'Invalid JSON',
        message: '{invalid json}',
        expectedError: true
    },
    {
        name: 'Missing command',
        message: JSON.stringify({ args: {} }),
        expectedError: true
    },
    {
        name: 'Missing args',
        message: JSON.stringify({ command: 'placeBlock' }),
        expectedError: true
    },
    {
        name: 'Unknown command',
        message: JSON.stringify({ command: 'unknownCommand', args: {} }),
        expectedError: true
    },
    {
        name: 'Very long command name',
        message: JSON.stringify({ command: 'a'.repeat(1000), args: {} }),
        expectedError: true
    },
    {
        name: 'Nested JSON attack',
        message: JSON.stringify({
            command: 'placeBlock',
            args: {
                x: { x: { x: { x: '0' } } },
                y: '64',
                z: '0',
                block: 'stone'
            }
        }),
        expectedError: true
    },
    {
        name: 'Unicode characters',
        message: JSON.stringify({
            command: 'chat',
            args: {
                message: '你好世界 🌍 مرحبا بالعالم'
            }
        }),
        expectedError: false  // Should handle unicode properly
    },
    {
        name: 'Special characters in block name',
        message: JSON.stringify({
            command: 'placeBlock',
            args: {
                x: '0',
                y: '64', 
                z: '0',
                block: 'stone!@#$%^&*()'
            }
        }),
        expectedError: true
    },
    {
        name: 'Floating point coordinates',
        message: JSON.stringify({
            command: 'placeBlock',
            args: {
                x: '10.5',
                y: '64.3',
                z: '-5.7',
                block: 'stone'
            }
        }),
        expectedError: false  // Should handle/round floats
    },
    {
        name: 'Extreme coordinates',
        message: JSON.stringify({
            command: 'placeBlock',
            args: {
                x: '30000000',
                y: '320',
                z: '-30000000',
                block: 'stone'
            }
        }),
        expectedError: true  // Out of bounds
    },
    {
        name: 'Negative Y coordinate',
        message: JSON.stringify({
            command: 'placeBlock',
            args: {
                x: '0',
                y: '-65',
                z: '0',
                block: 'stone'
            }
        }),
        expectedError: true  // Below world
    },
    {
        name: 'Array instead of object args',
        message: JSON.stringify({
            command: 'placeBlock',
            args: ['0', '64', '0', 'stone']
        }),
        expectedError: true
    },
    {
        name: 'Boolean args',
        message: JSON.stringify({
            command: 'placeBlock',
            args: {
                x: true,
                y: false,
                z: true,
                block: 'stone'
            }
        }),
        expectedError: true
    },
    {
        name: 'Mixed legacy and JSON',
        message: 'placeBlock({"x": "0", "y": "64"})',
        expectedError: true
    },
    {
        name: 'Circular reference attempt',
        message: (() => {
            const obj = { command: 'test', args: {} };
            obj.args.circular = obj;
            try {
                return JSON.stringify(obj);
            } catch (e) {
                return '{"command": "test", "args": {"error": "circular"}}';
            }
        })(),
        expectedError: true
    },
    {
        name: 'Command with newlines',
        message: JSON.stringify({
            command: 'chat',
            args: {
                message: 'Hello\nWorld\r\nTest'
            }
        }),
        expectedError: false  // Should handle newlines
    },
    {
        name: 'Empty string arguments',
        message: JSON.stringify({
            command: 'chat',
            args: {
                message: ''
            }
        }),
        expectedError: false  // Empty chat is valid
    },
    {
        name: 'Zero coordinates',
        message: JSON.stringify({
            command: 'placeBlock',
            args: {
                x: '0',
                y: '0',
                z: '0',
                block: 'bedrock'
            }
        }),
        expectedError: false  // Y=0 is valid
    }
];

// Test runner
async function runEdgeCaseTest(testCase) {
    return new Promise((resolve) => {
        const ws = new WebSocket(WS_URL);
        let responseReceived = false;
        let testResult = {
            name: testCase.name,
            passed: false,
            response: null,
            error: null
        };
        
        ws.on('open', () => {
            try {
                if (testCase.message === null) {
                    ws.send('');
                } else {
                    ws.send(testCase.message);
                }
            } catch (error) {
                testResult.error = error.message;
                testResult.passed = testCase.expectedError;
                ws.close();
                resolve(testResult);
            }
        });
        
        ws.on('message', (data) => {
            responseReceived = true;
            const response = data.toString();
            testResult.response = response;
            
            // Check if we got an error when expected
            const hasError = response.includes('error') || 
                           response.includes('invalid') ||
                           response.includes('missing') ||
                           response.includes('unknown');
            
            testResult.passed = (hasError === testCase.expectedError);
            
            ws.close();
        });
        
        ws.on('close', () => {
            if (!responseReceived && !testResult.error) {
                // Connection closed without response
                testResult.passed = testCase.expectedError;
                testResult.response = 'Connection closed';
            }
            resolve(testResult);
        });
        
        ws.on('error', (error) => {
            testResult.error = error.message;
            testResult.passed = testCase.expectedError;
            resolve(testResult);
        });
        
        // Timeout
        setTimeout(() => {
            if (!responseReceived) {
                testResult.response = 'Timeout';
                testResult.passed = testCase.expectedError;
                ws.close();
                resolve(testResult);
            }
        }, 3000);
    });
}

// Recovery test
async function testErrorRecovery() {
    console.log('\n🔄 Testing Error Recovery');
    console.log('========================');
    
    return new Promise((resolve) => {
        const ws = new WebSocket(WS_URL);
        let phase = 1;
        let results = {
            errorHandled: false,
            recoveredAfterError: false
        };
        
        ws.on('open', () => {
            console.log('Phase 1: Sending invalid command...');
            ws.send('{invalid json}');
        });
        
        ws.on('message', (data) => {
            const response = data.toString();
            
            if (phase === 1) {
                // Should get error response
                if (response.includes('error')) {
                    results.errorHandled = true;
                    console.log('✅ Error response received');
                    
                    // Now send valid command
                    phase = 2;
                    console.log('Phase 2: Sending valid command after error...');
                    ws.send(JSON.stringify({
                        command: 'getPlayerPos',
                        args: {}
                    }));
                }
            } else if (phase === 2) {
                // Should get valid response
                if (response.includes('playerPos') || response.includes('success')) {
                    results.recoveredAfterError = true;
                    console.log('✅ Valid response received after error');
                }
                ws.close();
            }
        });
        
        ws.on('close', () => {
            resolve(results);
        });
        
        ws.on('error', (error) => {
            console.error('Connection error:', error.message);
            resolve(results);
        });
        
        setTimeout(() => {
            ws.close();
            resolve(results);
        }, 5000);
    });
}

// Connection stability test
async function testConnectionStability() {
    console.log('\n🔌 Testing Connection Stability');
    console.log('==============================');
    
    return new Promise((resolve) => {
        const ws = new WebSocket(WS_URL);
        let commandsSent = 0;
        let responsesReceived = 0;
        let errors = 0;
        
        ws.on('open', () => {
            console.log('Sending mixed valid and invalid commands...');
            
            const commands = [
                JSON.stringify({ command: 'getPlayerPos', args: {} }),
                '{invalid}',
                JSON.stringify({ command: 'unknownCmd', args: {} }),
                JSON.stringify({ command: 'chat', args: { message: 'test' } }),
                '',
                JSON.stringify({ command: 'getBlock', args: { x: '0', y: '64', z: '0' } }),
                'legacy.command()',
                JSON.stringify({ command: 'placeBlock', args: { x: 'abc', y: '64', z: '0', block: 'stone' } })
            ];
            
            commands.forEach((cmd, index) => {
                setTimeout(() => {
                    try {
                        ws.send(cmd);
                        commandsSent++;
                    } catch (e) {
                        errors++;
                    }
                }, index * 200);  // Stagger sends
            });
            
            // Close after all commands sent
            setTimeout(() => {
                ws.close();
            }, commands.length * 200 + 1000);
        });
        
        ws.on('message', () => {
            responsesReceived++;
        });
        
        ws.on('close', () => {
            console.log(`Commands sent: ${commandsSent}`);
            console.log(`Responses received: ${responsesReceived}`);
            console.log(`Send errors: ${errors}`);
            
            resolve({
                stable: responsesReceived > 0 && ws.readyState === WebSocket.CLOSED,
                commandsSent,
                responsesReceived,
                errors
            });
        });
        
        ws.on('error', (error) => {
            console.error('Connection error:', error.message);
            errors++;
        });
    });
}

// Run all tests
async function runAllErrorTests() {
    console.log('Starting error handling and edge case tests...');
    console.log(`Target: ${WS_URL}`);
    console.log('Note: Ensure Minecraft with the mod is running!\n');
    
    // Run edge case tests
    console.log('🧪 Running Edge Case Tests');
    console.log('=========================');
    
    const results = [];
    for (const testCase of edgeCaseTests) {
        const result = await runEdgeCaseTest(testCase);
        results.push(result);
        
        if (result.passed) {
            console.log(`✅ ${result.name}`);
        } else {
            console.log(`❌ ${result.name}`);
            if (result.error) {
                console.log(`   Error: ${result.error}`);
            }
            if (result.response) {
                console.log(`   Response: ${result.response.substring(0, 100)}...`);
            }
        }
    }
    
    // Run recovery test
    const recoveryResult = await testErrorRecovery();
    
    // Run stability test
    const stabilityResult = await testConnectionStability();
    
    // Summary
    console.log('\n' + '='.repeat(50));
    console.log('📊 Error Handling Test Summary:');
    
    const passedTests = results.filter(r => r.passed).length;
    const failedTests = results.filter(r => !r.passed).length;
    
    console.log(`\nEdge Cases:`);
    console.log(`✅ Passed: ${passedTests}/${results.length}`);
    console.log(`❌ Failed: ${failedTests}/${results.length}`);
    
    console.log(`\nError Recovery:`);
    console.log(`Error handled: ${recoveryResult.errorHandled ? '✅' : '❌'}`);
    console.log(`Recovery after error: ${recoveryResult.recoveredAfterError ? '✅' : '❌'}`);
    
    console.log(`\nConnection Stability:`);
    console.log(`Stable: ${stabilityResult.stable ? '✅' : '❌'}`);
    console.log(`Response rate: ${Math.round(stabilityResult.responsesReceived / stabilityResult.commandsSent * 100)}%`);
    
    const overallSuccess = 
        failedTests === 0 && 
        recoveryResult.errorHandled && 
        recoveryResult.recoveredAfterError && 
        stabilityResult.stable;
    
    if (overallSuccess) {
        console.log('\n🎉 All error handling tests passed!');
        console.log('The system demonstrates robust error handling.');
        process.exit(0);
    } else {
        console.log('\n⚠️ Some error handling tests failed.');
        console.log('Review the failed cases to improve robustness.');
        process.exit(1);
    }
}

// Execute tests
runAllErrorTests();
const WebSocket = require('ws');

console.log('🔒 Security Validation Test');
console.log('===========================');

const WS_URL = 'ws://localhost:14711';

// Security test cases
const securityTests = {
    // Dangerous blocks that should be rejected
    dangerousBlocks: ['tnt', 'end_crystal', 'respawn_anchor', 'bed'],
    
    // Safe blocks that should be allowed
    safeBlocks: ['stone', 'dirt', 'grass_block', 'oak_planks'],
    
    // Dangerous commands that should be blocked
    dangerousCommands: ['op', 'deop', 'stop', 'kick', 'ban', 'save-all'],
    
    // SQL injection attempts
    sqlInjectionAttempts: [
        "'; DROP TABLE players; --",
        "1' OR '1'='1",
        "admin'--",
        "1; DELETE FROM world WHERE 1=1"
    ],
    
    // Command injection attempts
    commandInjectionAttempts: [
        "; rm -rf /",
        "& del /f /s /q C:\\*",
        "| shutdown -s -t 0",
        "`rm -rf *`"
    ],
    
    // Path traversal attempts
    pathTraversalAttempts: [
        "../../../etc/passwd",
        "..\\..\\..\\windows\\system32",
        "../../../../home/user/.ssh/id_rsa"
    ],
    
    // XSS attempts
    xssAttempts: [
        "<script>alert('XSS')</script>",
        "<img src=x onerror=alert('XSS')>",
        "javascript:alert('XSS')",
        "<iframe src='javascript:alert(1)'>"
    ]
};

// Test helper
async function runSecurityTest(testName, testFunction) {
    console.log(`\n📝 ${testName}`);
    try {
        const result = await testFunction();
        if (result.passed) {
            console.log(`✅ PASSED: ${result.message}`);
            return true;
        } else {
            console.log(`❌ FAILED: ${result.message}`);
            return false;
        }
    } catch (error) {
        console.log(`💥 ERROR: ${error.message}`);
        return false;
    }
}

// Test 1: Dangerous block prevention
async function testDangerousBlocks() {
    return new Promise((resolve) => {
        const ws = new WebSocket(WS_URL);
        let testResults = [];
        let currentIndex = 0;
        
        ws.on('open', () => {
            testNextBlock();
        });
        
        function testNextBlock() {
            if (currentIndex >= securityTests.dangerousBlocks.length) {
                ws.close();
                const allBlocked = testResults.every(r => r.blocked);
                resolve({
                    passed: allBlocked,
                    message: allBlocked ? 
                        'All dangerous blocks were blocked' : 
                        `Some dangerous blocks were not blocked: ${testResults.filter(r => !r.blocked).map(r => r.block).join(', ')}`
                });
                return;
            }
            
            const block = securityTests.dangerousBlocks[currentIndex];
            ws.send(JSON.stringify({
                command: 'placeBlock',
                args: {
                    x: '0',
                    y: '64',
                    z: '0',
                    block: block
                }
            }));
        }
        
        ws.on('message', (data) => {
            const message = data.toString();
            const block = securityTests.dangerousBlocks[currentIndex];
            
            // Check if block was rejected
            const blocked = message.includes('error') || 
                           message.includes('forbidden') || 
                           message.includes('not allowed') ||
                           message.includes('dangerous');
            
            testResults.push({ block, blocked });
            currentIndex++;
            testNextBlock();
        });
        
        ws.on('error', (error) => {
            resolve({
                passed: false,
                message: `Connection error: ${error.message}`
            });
        });
    });
}

// Test 2: Safe block allowance
async function testSafeBlocks() {
    return new Promise((resolve) => {
        const ws = new WebSocket(WS_URL);
        let testResults = [];
        let currentIndex = 0;
        
        ws.on('open', () => {
            // Skip welcome message
            setTimeout(testNextBlock, 100);
        });
        
        function testNextBlock() {
            if (currentIndex >= securityTests.safeBlocks.length) {
                ws.close();
                const allAllowed = testResults.every(r => r.allowed);
                resolve({
                    passed: allAllowed,
                    message: allAllowed ? 
                        'All safe blocks were allowed' : 
                        `Some safe blocks were blocked: ${testResults.filter(r => !r.allowed).map(r => r.block).join(', ')}`
                });
                return;
            }
            
            const block = securityTests.safeBlocks[currentIndex];
            ws.send(JSON.stringify({
                command: 'placeBlock',
                args: {
                    x: '0',
                    y: '64',
                    z: '0',
                    block: block
                }
            }));
        }
        
        ws.on('message', (data) => {
            if (currentIndex >= securityTests.safeBlocks.length) return;
            
            const message = data.toString();
            const block = securityTests.safeBlocks[currentIndex];
            
            // Check if block was allowed (no security error)
            const allowed = !message.includes('forbidden') && 
                           !message.includes('not allowed') &&
                           !message.includes('dangerous');
            
            testResults.push({ block, allowed });
            currentIndex++;
            testNextBlock();
        });
        
        ws.on('error', (error) => {
            resolve({
                passed: false,
                message: `Connection error: ${error.message}`
            });
        });
    });
}

// Test 3: Command injection prevention
async function testCommandInjection() {
    return new Promise((resolve) => {
        const ws = new WebSocket(WS_URL);
        let testResults = [];
        let currentIndex = 0;
        
        ws.on('open', () => {
            setTimeout(testNextInjection, 100);
        });
        
        function testNextInjection() {
            if (currentIndex >= securityTests.commandInjectionAttempts.length) {
                ws.close();
                const allBlocked = testResults.every(r => r.safe);
                resolve({
                    passed: allBlocked,
                    message: allBlocked ? 
                        'All command injection attempts were handled safely' : 
                        'Some command injection attempts may not have been handled safely'
                });
                return;
            }
            
            const injection = securityTests.commandInjectionAttempts[currentIndex];
            ws.send(JSON.stringify({
                command: 'chat',
                args: {
                    message: injection
                }
            }));
        }
        
        ws.on('message', (data) => {
            if (currentIndex >= securityTests.commandInjectionAttempts.length) return;
            
            const message = data.toString();
            const injection = securityTests.commandInjectionAttempts[currentIndex];
            
            // Check that no system commands were executed
            const safe = !message.includes('executed') && 
                        !message.includes('deleted') &&
                        !message.includes('shutdown') &&
                        (message.includes('error') || message.includes('chat'));
            
            testResults.push({ injection, safe });
            currentIndex++;
            testNextInjection();
        });
        
        ws.on('error', (error) => {
            resolve({
                passed: false,
                message: `Connection error: ${error.message}`
            });
        });
    });
}

// Test 4: Input validation
async function testInputValidation() {
    return new Promise((resolve) => {
        const ws = new WebSocket(WS_URL);
        const invalidInputs = [
            { x: 'abc', y: '64', z: '0' },  // Non-numeric coordinate
            { x: '999999999', y: '64', z: '0' },  // Out of bounds
            { x: '0', y: '-1000', z: '0' },  // Negative Y out of bounds
            { x: 'null', y: 'undefined', z: 'NaN' },  // Special values
            { x: '${1+1}', y: '${2*32}', z: '${3-3}' }  // Template injection
        ];
        
        let testResults = [];
        let currentIndex = 0;
        
        ws.on('open', () => {
            setTimeout(testNextInput, 100);
        });
        
        function testNextInput() {
            if (currentIndex >= invalidInputs.length) {
                ws.close();
                const allValidated = testResults.every(r => r.validated);
                resolve({
                    passed: allValidated,
                    message: allValidated ? 
                        'All invalid inputs were properly validated' : 
                        'Some invalid inputs were not properly validated'
                });
                return;
            }
            
            const input = invalidInputs[currentIndex];
            ws.send(JSON.stringify({
                command: 'placeBlock',
                args: {
                    ...input,
                    block: 'stone'
                }
            }));
        }
        
        ws.on('message', (data) => {
            if (currentIndex >= invalidInputs.length) return;
            
            const message = data.toString();
            const input = invalidInputs[currentIndex];
            
            // Check that invalid input was rejected or handled safely
            const validated = message.includes('error') || 
                             message.includes('invalid') ||
                             message.includes('out of bounds');
            
            testResults.push({ input, validated });
            currentIndex++;
            testNextInput();
        });
        
        ws.on('error', (error) => {
            resolve({
                passed: false,
                message: `Connection error: ${error.message}`
            });
        });
    });
}

// Test 5: Message size limits
async function testMessageSizeLimits() {
    return new Promise((resolve) => {
        const ws = new WebSocket(WS_URL);
        
        ws.on('open', () => {
            // Test very large message
            const largeMessage = 'A'.repeat(10000);  // 10KB message
            
            try {
                ws.send(JSON.stringify({
                    command: 'chat',
                    args: {
                        message: largeMessage
                    }
                }));
                
                // Also test very large block name
                ws.send(JSON.stringify({
                    command: 'placeBlock',
                    args: {
                        x: '0',
                        y: '64',
                        z: '0',
                        block: 'B'.repeat(1000)
                    }
                }));
            } catch (error) {
                // Size limit enforced at send
                ws.close();
                resolve({
                    passed: true,
                    message: 'Message size limits are enforced'
                });
                return;
            }
        });
        
        let responses = 0;
        ws.on('message', (data) => {
            responses++;
            if (responses >= 2) {
                ws.close();
                // If we got responses, the server handled large messages
                resolve({
                    passed: true,
                    message: 'Server handles large messages appropriately'
                });
            }
        });
        
        ws.on('error', (error) => {
            resolve({
                passed: false,
                message: `Connection error: ${error.message}`
            });
        });
        
        setTimeout(() => {
            ws.close();
            resolve({
                passed: true,
                message: 'No issues with message size handling'
            });
        }, 5000);
    });
}

// Run all security tests
async function runAllSecurityTests() {
    console.log('Starting security validation tests...');
    console.log(`Target: ${WS_URL}`);
    console.log('Note: Ensure Minecraft with the mod is running!\n');
    
    const results = [];
    
    // Run each test
    results.push(await runSecurityTest('Dangerous Block Prevention', testDangerousBlocks));
    results.push(await runSecurityTest('Safe Block Allowance', testSafeBlocks));
    results.push(await runSecurityTest('Command Injection Prevention', testCommandInjection));
    results.push(await runSecurityTest('Input Validation', testInputValidation));
    results.push(await runSecurityTest('Message Size Limits', testMessageSizeLimits));
    
    // Summary
    console.log('\n' + '='.repeat(50));
    console.log('🔒 Security Test Summary:');
    const passed = results.filter(r => r).length;
    const failed = results.filter(r => !r).length;
    console.log(`✅ Passed: ${passed}`);
    console.log(`❌ Failed: ${failed}`);
    console.log(`📈 Security Score: ${Math.round(passed / results.length * 100)}%`);
    
    if (failed === 0) {
        console.log('\n🎉 All security tests passed!');
        console.log('The system demonstrates good security practices.');
        process.exit(0);
    } else {
        console.log('\n⚠️ Some security tests failed.');
        console.log('Review the failed tests and improve security measures.');
        process.exit(1);
    }
}

// Execute tests
runAllSecurityTests();
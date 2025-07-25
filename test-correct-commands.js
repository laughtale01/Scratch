const WebSocket = require('ws');

function testMinecraftCommands() {
    console.log('\n=== Minecraft WebSocket Command Test ===');
    
    const ws = new WebSocket('ws://localhost:14711');
    let testStep = 0;
    
    const tests = [
        'minecraft.connect()',
        'minecraft.status()',
        'player.getPos()',
        'chat.post(Hello from WebSocket test!)',
        'world.setBlock(100,65,100,diamond_block)',
        'world.getBlock(100,65,100)',
        'collaboration.getCurrentWorld()',
        'collaboration.getInvitations()',
        'collaboration.invite(TestFriend)',
        'collaboration.returnHome()'
    ];
    
    ws.on('open', function open() {
        console.log('✅ Connected to Minecraft WebSocket server');
        
        function runNextTest() {
            if (testStep < tests.length) {
                const command = tests[testStep];
                console.log(`\n📤 Test ${testStep + 1}/${tests.length}: ${command}`);
                ws.send(command);
                testStep++;
                
                // Wait for response before next test
                setTimeout(runNextTest, 2000);
            } else {
                console.log('\n🎉 All tests completed!');
                setTimeout(() => {
                    ws.close();
                    process.exit(0);
                }, 1000);
            }
        }
        
        // Start testing after connection is established
        setTimeout(runNextTest, 1000);
    });
    
    ws.on('message', function message(data) {
        const response = data.toString();
        console.log('📨 Response:', response);
        
        // Analyze response
        if (response.includes('error.')) {
            console.log('   ⚠️  Error response detected');
        } else if (response.includes('success') || response.includes('connected') || response.includes('sent')) {
            console.log('   ✅ Success response');
        } else {
            console.log('   ℹ️  Info response');
        }
    });
    
    ws.on('close', function close(code, reason) {
        console.log('\n❌ Connection closed:', code, reason.toString());
    });
    
    ws.on('error', function error(err) {
        console.log('❌ WebSocket error:', err.message);
    });
}

// Run the test
testMinecraftCommands();
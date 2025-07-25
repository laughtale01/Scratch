const WebSocket = require('ws');

console.log('🎮 Minecraft協調学習システム - 統合テスト');
console.log('='.repeat(50));

const ws = new WebSocket('ws://localhost:14711');

let testStep = 0;
const testSteps = [
    // 接続テスト
    { 
        name: '接続確認', 
        command: 'minecraft.status()',
        delay: 200
    },
    
    // 招待テスト
    { 
        name: '友達を招待', 
        command: 'collaboration.invite(TestFriend)',
        delay: 500
    },
    
    // 招待リスト確認
    { 
        name: '招待リスト確認', 
        command: 'collaboration.getInvitations()',
        delay: 500
    },
    
    // 訪問リクエスト
    { 
        name: '訪問リクエスト送信', 
        command: 'collaboration.requestVisit(HostPlayer)',
        delay: 500
    },
    
    // 現在のワールド確認
    { 
        name: '現在のワールド確認', 
        command: 'collaboration.getCurrentWorld()',
        delay: 500
    },
    
    // 帰宅テスト
    { 
        name: '通常帰宅', 
        command: 'collaboration.returnHome()',
        delay: 500
    },
    
    // プレイヤー位置取得
    { 
        name: 'プレイヤー位置', 
        command: 'player.getPos()',
        delay: 300
    },
    
    // チャットメッセージ
    { 
        name: 'チャットメッセージ送信', 
        command: 'chat.post(協調機能テスト完了！)',
        delay: 300
    }
];

function runNextTest() {
    if (testStep >= testSteps.length) {
        console.log('\n✅ すべてのテストが完了しました！');
        ws.close();
        return;
    }
    
    const test = testSteps[testStep];
    console.log(`\n[テスト${testStep + 1}] ${test.name}`);
    console.log('送信:', test.command);
    
    ws.send(test.command);
    testStep++;
    
    setTimeout(runNextTest, test.delay);
}

ws.on('open', () => {
    console.log('✅ WebSocketサーバーに接続しました！');
    console.log('ポート: 14711');
    console.log('\nテストを開始します...\n');
    
    setTimeout(runNextTest, 500);
});

ws.on('message', (data) => {
    const message = data.toString();
    console.log('📨 受信:', message);
    
    // レスポンスの解析
    if (message.includes('error')) {
        console.log('   ❌ エラー検出');
    } else if (message.includes('success')) {
        console.log('   ✅ 成功');
    }
    
    // JSON形式の場合はパース
    try {
        const json = JSON.parse(message);
        console.log('   JSON:', json);
    } catch (e) {
        // JSONでない場合は無視
    }
});

ws.on('error', (error) => {
    console.error('❌ WebSocketエラー:', error.message);
    console.error('Minecraftが起動していることを確認してください。');
});

ws.on('close', () => {
    console.log('\n接続が終了しました。');
});

// タイムアウト設定
setTimeout(() => {
    console.log('\n⏱️ テストタイムアウト');
    ws.close();
    process.exit(0);
}, 15000);
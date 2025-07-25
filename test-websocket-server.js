#!/usr/bin/env node
// WebSocket接続テストスクリプト
// Minecraft modのWebSocketサーバー（ポート14711）への接続をテストします

const WebSocket = require('ws');

console.log('=== WebSocket接続テスト開始 ===');
console.log('接続先: ws://localhost:14711');

const ws = new WebSocket('ws://localhost:14711');

ws.on('open', function open() {
    console.log('✓ WebSocketサーバーに接続成功');
    console.log('テストメッセージを送信中...');
    
    // 各種コマンドをテスト
    const testCommands = [
        'player.getPos()',
        'player.chat("Hello from test script!")',
        'block.place(stone,0,65,0)',
        'info.getPlayerName()'
    ];
    
    testCommands.forEach((cmd, index) => {
        setTimeout(() => {
            console.log(`送信: ${cmd}`);
            ws.send(cmd);
        }, 1000 * (index + 1));
    });
    
    // 5秒後に接続を閉じる
    setTimeout(() => {
        console.log('テスト完了。接続を閉じます。');
        ws.close();
    }, 6000);
});

ws.on('message', function message(data) {
    console.log(`受信: ${data}`);
});

ws.on('error', function error(err) {
    console.error('✗ エラー:', err.message);
    console.log('Minecraft modが起動していることを確認してください。');
});

ws.on('close', function close() {
    console.log('接続が閉じられました。');
    process.exit(0);
});

// タイムアウト処理
setTimeout(() => {
    console.log('タイムアウト: 接続できませんでした。');
    process.exit(1);
}, 10000);
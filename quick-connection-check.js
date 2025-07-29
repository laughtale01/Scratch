/**
 * 迅速なWebSocket接続チェック
 */

const WebSocket = require('ws');

function checkMinecraftConnection() {
    console.log('🔍 Minecraft WebSocketサーバー状態確認...');
    
    const ws = new WebSocket('ws://localhost:14711');
    
    let connected = false;
    
    ws.on('open', () => {
        connected = true;
        console.log('✅ WebSocketサーバー接続成功！');
        console.log('🎮 Minecraftは正常に動作中');
        ws.close();
    });
    
    ws.on('message', (data) => {
        console.log('📨 サーバーからのメッセージ:', data.toString());
    });
    
    ws.on('error', (error) => {
        console.log('❌ 接続エラー:', error.code || 'CONNECTION_FAILED');
        if (error.code === 'ECONNREFUSED') {
            console.log('🚨 WebSocketサーバーが起動していません');
            console.log('💡 確認事項:');
            console.log('   1. Minecraftが起動しているか');
            console.log('   2. ワールドにログインしているか');
            console.log('   3. Modが正常に読み込まれているか');
            console.log('   4. ポート14711が利用可能か');
        }
    });
    
    ws.on('close', () => {
        if (connected) {
            console.log('🔌 接続終了 - サーバーは正常');
        } else {
            console.log('❌ 接続失敗 - サーバーが見つかりません');
        }
    });
    
    // タイムアウト設定
    setTimeout(() => {
        if (!connected) {
            console.log('⏰ 接続タイムアウト');
            console.log('📋 チェック項目:');
            console.log('   □ Minecraft + Forge 1.20.1 起動済み');
            console.log('   □ Minecraft Collaboration Mod 読み込み済み');
            console.log('   □ ワールドにログイン済み');
            console.log('   □ チャットで Mod のメッセージ確認');
            ws.close();
        }
    }, 5000);
}

checkMinecraftConnection();
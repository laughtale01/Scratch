/**
 * 簡単なWebSocket接続テスト
 * Minecraftコラボレーションシステムの基本接続を確認
 */

const WebSocket = require('ws');

async function testWebSocketConnection() {
    console.log('🔍 WebSocket接続テストを開始...');
    
    const testResults = {
        connectionTest: false,
        messageTest: false,
        commandTest: false
    };
    
    try {
        // WebSocketサーバーに接続
        const ws = new WebSocket('ws://localhost:14711');
        
        ws.on('open', () => {
            console.log('✅ WebSocket接続成功 (ポート: 14711)');
            testResults.connectionTest = true;
            
            // 基本的なコマンドをテスト
            console.log('🧪 基本コマンドテスト...');
            ws.send(JSON.stringify({
                command: 'connect',
                args: []
            }));
            
            setTimeout(() => {
                ws.send(JSON.stringify({
                    command: 'status',
                    args: []
                }));
                
                ws.send(JSON.stringify({
                    command: 'getPlayerPosition',
                    args: []
                }));
            }, 100);
        });
        
        ws.on('message', (data) => {
            try {
                const response = JSON.parse(data.toString());
                console.log('📨 レスポンス受信:', response);
                testResults.messageTest = true;
                
                if (response.command || response.type) {
                    testResults.commandTest = true;
                    console.log('✅ コマンドレスポンス処理成功');
                }
            } catch (e) {
                console.log('📨 レスポンス受信 (テキスト):', data.toString());
                testResults.messageTest = true;
            }
        });
        
        ws.on('error', (error) => {
            console.log('❌ WebSocket接続エラー:', error.message);
            if (error.code === 'ECONNREFUSED') {
                console.log('🚨 WebSocketサーバーが起動していません (ポート: 14711)');
                console.log('💡 Minecraftを起動してModが読み込まれていることを確認してください');
            }
        });
        
        ws.on('close', () => {
            console.log('🔌 WebSocket接続終了');
            displayTestResults(testResults);
        });
        
        // 5秒後に接続を閉じる
        setTimeout(() => {
            if (ws.readyState === WebSocket.OPEN) {
                ws.close();
            } else {
                displayTestResults(testResults);
            }
        }, 5000);
        
    } catch (error) {
        console.log('❌ テスト実行エラー:', error.message);
        displayTestResults(testResults);
    }
}

function displayTestResults(results) {
    console.log('\n📊 テスト結果サマリー:');
    console.log('━━━━━━━━━━━━━━━━━━━━');
    console.log(`🔌 WebSocket接続: ${results.connectionTest ? '✅ 成功' : '❌ 失敗'}`);
    console.log(`📨 メッセージ通信: ${results.messageTest ? '✅ 成功' : '❌ 失敗'}`);
    console.log(`🎮 コマンド処理: ${results.commandTest ? '✅ 成功' : '❌ 失敗'}`);
    console.log('━━━━━━━━━━━━━━━━━━━━');
    
    const successCount = Object.values(results).filter(Boolean).length;
    const totalCount = Object.keys(results).length;
    
    if (successCount === totalCount) {
        console.log('🎉 すべてのテストが成功しました！');
        console.log('✨ Minecraftコラボレーションシステムは正常に動作しています');
    } else if (successCount > 0) {
        console.log(`⚠️  一部のテストが失敗しました (${successCount}/${totalCount})`);
        console.log('💡 Minecraftとmodが正しく起動していることを確認してください');
    } else {
        console.log('❌ すべてのテストが失敗しました');
        console.log('💡 システムが起動していない可能性があります');
    }
}

function testScratchExtension() {
    console.log('\n🧩 Scratch拡張基本機能チェック...');
    
    try {
        const MinecraftExtension = require('./scratch-extension/src/index.js');
        console.log('✅ Scratch拡張ファイルの読み込み成功');
        
        // 模擬ランタイムでの初期化テスト
        const mockRuntime = {
            on: () => {},
            emit: () => {},
            registerExtensionDevice: () => {}
        };
        
        const extension = new MinecraftExtension(mockRuntime);
        const info = extension.getInfo();
        
        console.log(`✅ 拡張情報取得成功:`);
        console.log(`   - ID: ${info.id}`);
        console.log(`   - 名前: ${info.name}`);
        console.log(`   - ブロック数: ${info.blocks.length}`);
        
        console.log('🧩 Scratch拡張は正常に構成されています');
        
    } catch (error) {
        console.log('❌ Scratch拡張エラー:', error.message);
    }
}

// メイン実行
async function main() {
    console.log('🚀 Minecraftコラボレーションシステム 統合テスト');
    console.log('═══════════════════════════════════════════════');
    
    testScratchExtension();
    await testWebSocketConnection();
    
    console.log('\n🏁 テスト完了');
}

main().catch(console.error);
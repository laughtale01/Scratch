/**
 * Minecraft起動状態とMod読み込み確認ツール
 */

const fs = require('fs');
const path = require('path');

console.log('🔍 Minecraft起動状態確認ツール');
console.log('═══════════════════════════════════');

// ログファイルの場所を推定
const possibleLogPaths = [
    path.join(process.env.APPDATA || '', '.minecraft', 'logs', 'latest.log'),
    path.join(process.env.APPDATA || '', '.minecraft', 'logs', 'debug.log'),
    path.join(process.env.USERPROFILE || '', '.minecraft', 'logs', 'latest.log'),
    './logs/latest.log',
    '../.minecraft/logs/latest.log'
];

function checkMinecraftLogs() {
    console.log('📁 Minecraftログファイル検索中...');
    
    let logFound = false;
    
    for (const logPath of possibleLogPaths) {
        if (fs.existsSync(logPath)) {
            console.log(`✅ ログファイル発見: ${logPath}`);
            logFound = true;
            
            try {
                const logContent = fs.readFileSync(logPath, 'utf8');
                const lines = logContent.split('\n').slice(-100); // 最後の100行
                
                analyzeMinecraftLog(lines);
            } catch (error) {
                console.log(`❌ ログファイル読み取りエラー: ${error.message}`);
            }
            break;
        }
    }
    
    if (!logFound) {
        console.log('❌ Minecraftログファイルが見つかりません');
        console.log('💡 確認事項:');
        console.log('   - Minecraftが起動しているか');
        console.log('   - .minecraftフォルダのパスが正しいか');
    }
}

function analyzeMinecraftLog(lines) {
    console.log('\n🔍 ログ解析結果:');
    
    // Mod関連のログを検索
    const modPattern = /minecraftcollaboration|collaboration|websocket/i;
    const errorPattern = /error|exception|failed/i;
    const serverPattern = /server.*start|websocket.*start|listening.*port/i;
    
    let modLoaded = false;
    let serverStarted = false;
    let errors = [];
    
    lines.forEach((line, index) => {
        if (modPattern.test(line)) {
            if (line.includes('initialized') || line.includes('loaded')) {
                modLoaded = true;
                console.log(`✅ Mod読み込み確認: ${line.trim()}`);
            }
            
            if (serverPattern.test(line)) {
                serverStarted = true;
                console.log(`✅ サーバー起動確認: ${line.trim()}`);
            }
            
            if (errorPattern.test(line)) {
                errors.push(line.trim());
            }
        }
    });
    
    console.log('\n📊 状態サマリー:');
    console.log(`🔧 Mod読み込み: ${modLoaded ? '✅ 成功' : '❌ 未確認'}`);
    console.log(`🌐 WebSocketサーバー: ${serverStarted ? '✅ 起動' : '❌ 未確認'}`);
    
    if (errors.length > 0) {
        console.log('\n🚨 検出されたエラー:');
        errors.forEach((error, i) => {
            console.log(`   ${i + 1}. ${error}`);
        });
    }
    
    if (!modLoaded) {
        console.log('\n💡 Mod読み込み問題の可能性:');
        console.log('   - build.gradle のパッケージ名確認');
        console.log('   - JAR ファイルが mods フォルダに配置されているか');
        console.log('   - Forge バージョンの互換性');
    }
    
    if (!serverStarted) {
        console.log('\n💡 WebSocketサーバー起動問題の可能性:');
        console.log('   - ポート 14711 が他のプロセスで使用中');
        console.log('   - ファイアウォールの設定');
        console.log('   - Java WebSocket ライブラリの依存関係');
    }
}

// ポートチェック
function checkPort() {
    console.log('\n🔌 ポート 14711 使用状況確認...');
    
    const { execSync } = require('child_process');
    
    try {
        const result = execSync('netstat -an | findstr :14711', { encoding: 'utf8' });
        if (result.trim()) {
            console.log('✅ ポート 14711 は使用中:');
            console.log(result.trim());
        } else {
            console.log('❌ ポート 14711 は使用されていません');
        }
    } catch (error) {
        console.log('⚠️  ポート確認できませんでした');
    }
}

// メイン実行
checkMinecraftLogs();
checkPort();

console.log('\n🎯 次のステップ:');
console.log('1. Minecraftが起動し、ワールドにログインしていることを確認');
console.log('2. チャットで "/forge" コマンドを実行し、Modリストを確認');
console.log('3. "minecraftcollaboration" Modが表示されることを確認');
console.log('4. ログに "WebSocket server started" メッセージが表示されることを確認');
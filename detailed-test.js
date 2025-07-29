/**
 * 詳細なMinecraftコラボレーションシステムテスト
 * JSON形式でのコマンド送信と認証対応
 */

const WebSocket = require('ws');

class DetailedMinecraftTester {
    constructor() {
        this.ws = null;
        this.isAuthenticated = false;
        this.results = {
            authentication: false,
            basicCommands: [],
            blockOperations: [],
            agentFunctions: [],
            errors: []
        };
        this.responseQueue = [];
        this.pendingCommand = null;
    }

    async runDetailedTests() {
        console.log('🔬 Minecraft詳細機能テスト - JSON形式');
        console.log('═══════════════════════════════════════');
        
        try {
            await this.connectAndAuthenticate();
            await this.runCommandTests();
            this.generateDetailedReport();
        } catch (error) {
            console.error('❌ テスト実行エラー:', error.message);
        } finally {
            if (this.ws) {
                this.ws.close();
            }
        }
    }

    async connectAndAuthenticate() {
        return new Promise((resolve, reject) => {
            console.log('🔐 認証プロセス開始...');
            
            this.ws = new WebSocket('ws://localhost:14711');
            
            this.ws.on('open', () => {
                console.log('✅ WebSocket接続成功');
                
                // 認証を試行
                this.sendCommand('auth', { token: 'test' }, (response) => {
                    if (response.includes('authenticated') || response.includes('success')) {
                        this.isAuthenticated = true;
                        this.results.authentication = true;
                        console.log('✅ 認証成功');
                    } else {
                        console.log('⚠️  認証スキップ (テストモード)');
                        this.isAuthenticated = true; // テスト用に認証済みとして扱う
                    }
                    resolve();
                });
                
                setTimeout(() => {
                    if (!this.isAuthenticated) {
                        console.log('⚠️  認証タイムアウト - 継続');
                        this.isAuthenticated = true;
                        resolve();
                    }
                }, 3000);
            });
            
            this.ws.on('message', (data) => {
                this.handleResponse(data.toString());
            });
            
            this.ws.on('error', (error) => {
                console.log('❌ 接続エラー:', error.message);
                reject(error);
            });
        });
    }

    sendCommand(command, args = {}, callback = null) {
        const message = {
            command: command,
            args: args
        };
        
        console.log(`📤 送信: ${command}`, args);
        
        if (callback) {
            this.pendingCommand = { command, callback };
        }
        
        this.ws.send(JSON.stringify(message));
    }

    handleResponse(response) {
        console.log('📨 レスポンス:', response);
        this.responseQueue.push(response);
        
        if (this.pendingCommand && this.pendingCommand.callback) {
            this.pendingCommand.callback(response);
            this.pendingCommand = null;
        }
    }

    async runCommandTests() {
        console.log('\n🧪 コマンドテスト実行...');
        
        const tests = [
            {
                category: 'basicCommands',
                name: 'プレイヤー位置取得',
                command: 'getPlayerPosition',
                args: {}
            },
            {
                category: 'basicCommands',
                name: '接続確認',
                command: 'connect',
                args: {}
            },
            {
                category: 'basicCommands',
                name: 'ステータス確認',
                command: 'status',
                args: {}
            },
            {
                category: 'blockOperations',
                name: 'ブロック配置',
                command: 'setBlock',
                args: { x: 100, y: 64, z: 100, blockType: 'stone' }
            },
            {
                category: 'blockOperations',
                name: 'ブロック取得',
                command: 'getBlock',
                args: { x: 100, y: 64, z: 100 }
            },
            {
                category: 'blockOperations',
                name: 'エリア塗りつぶし',
                command: 'fillArea',
                args: { x1: 100, y1: 64, z1: 100, x2: 102, y2: 64, z2: 102, blockType: 'grass_block' }
            },
            {
                category: 'basicCommands',
                name: 'チャット送信',
                command: 'chat',
                args: { message: 'Hello from automated test!' }
            },
            {
                category: 'agentFunctions',
                name: 'エージェント召喚',
                command: 'summonAgent',
                args: { name: 'TestAgent' }
            }
        ];

        for (const test of tests) {
            await this.executeTest(test);
            await this.delay(2000); // 2秒待機
        }
    }

    async executeTest(test) {
        return new Promise((resolve) => {
            console.log(`\n🔍 ${test.name} テスト中...`);
            
            const startTime = Date.now();
            let completed = false;
            
            this.sendCommand(test.command, test.args, (response) => {
                const duration = Date.now() - startTime;
                completed = true;
                
                const result = {
                    name: test.name,
                    command: test.command,
                    args: test.args,
                    response: response,
                    duration: duration,
                    success: this.isSuccessfulResponse(response)
                };
                
                this.results[test.category].push(result);
                
                if (result.success) {
                    console.log(`✅ ${test.name} - 成功 (${duration}ms)`);
                } else {
                    console.log(`❌ ${test.name} - 失敗: ${response.substring(0, 100)}`);
                    this.results.errors.push(`${test.name}: ${response}`);
                }
                
                resolve();
            });
            
            // タイムアウト処理
            setTimeout(() => {
                if (!completed) {
                    console.log(`⏰ ${test.name} - タイムアウト`);
                    this.results[test.category].push({
                        name: test.name,
                        command: test.command,
                        success: false,
                        error: 'timeout'
                    });
                    resolve();
                }
            }, 5000);
        });
    }

    isSuccessfulResponse(response) {
        // 成功を示すキーワードを確認
        const successKeywords = ['success', 'connected', 'position', 'block', 'completed', 'summoned'];
        const errorKeywords = ['error', 'failed', 'unknown', 'invalid'];
        
        const lowercaseResponse = response.toLowerCase();
        
        const hasSuccess = successKeywords.some(keyword => lowercaseResponse.includes(keyword));
        const hasError = errorKeywords.some(keyword => lowercaseResponse.includes(keyword));
        
        return hasSuccess && !hasError;
    }

    delay(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }

    generateDetailedReport() {
        console.log('\n📊 詳細テスト結果レポート');
        console.log('═══════════════════════════════════════');
        
        // カテゴリ別の結果集計
        const categories = ['basicCommands', 'blockOperations', 'agentFunctions'];
        let totalTests = 0;
        let totalSuccesses = 0;
        
        categories.forEach(category => {
            const tests = this.results[category];
            const successes = tests.filter(t => t.success).length;
            const successRate = tests.length > 0 ? (successes / tests.length * 100).toFixed(1) : '0';
            
            console.log(`\n${this.getCategoryEmoji(category)} ${category}: ${successRate}% (${successes}/${tests.length})`);
            
            tests.forEach(test => {
                const status = test.success ? '✅' : '❌';
                const duration = test.duration ? ` (${test.duration}ms)` : '';
                console.log(`   ${status} ${test.name}${duration}`);
                
                if (!test.success && test.response) {
                    console.log(`      └─ ${test.response.substring(0, 60)}...`);
                }
            });
            
            totalTests += tests.length;
            totalSuccesses += successes;
        });
        
        // 総合結果
        const overallSuccessRate = totalTests > 0 ? (totalSuccesses / totalTests * 100).toFixed(1) : '0';
        
        console.log('\n🏆 総合評価:');
        console.log(`📈 総合成功率: ${overallSuccessRate}% (${totalSuccesses}/${totalTests})`);
        console.log(`🔐 認証: ${this.results.authentication ? '✅ 成功' : '❌ 失敗'}`);
        console.log(`📨 通信品質: ${this.responseQueue.length}個のレスポンス受信`);
        
        // 品質レベル判定
        console.log('\n🎯 品質評価:');
        if (overallSuccessRate >= 80) {
            console.log('🌟 優秀 - 本格運用に適している');
        } else if (overallSuccessRate >= 60) {
            console.log('✅ 良好 - 改善の余地はあるが運用可能');
        } else if (overallSuccessRate >= 40) {
            console.log('⚠️  要改善 - 重要な機能に問題あり');
        } else {
            console.log('❌ 要修正 - 基本機能に重大な問題');
        }
        
        // エラー詳細
        if (this.results.errors.length > 0) {
            console.log('\n🐛 検出された問題:');
            this.results.errors.forEach((error, index) => {
                console.log(`   ${index + 1}. ${error.substring(0, 100)}...`);
            });
        }
        
        // 推奨事項
        console.log('\n💡 推奨事項:');
        if (overallSuccessRate < 50) {
            console.log('   - コマンド実装の確認が必要');
            console.log('   - エラーハンドリングの強化を推奨');
        }
        if (!this.results.authentication) {
            console.log('   - 認証システムの確認が必要');
        }
        if (overallSuccessRate >= 70) {
            console.log('   - 実証実験の開始が可能');
            console.log('   - ユーザーテストの実施を推奨');
        }
    }

    getCategoryEmoji(category) {
        const emojis = {
            basicCommands: '🔧',
            blockOperations: '🧱',
            agentFunctions: '🤖'
        };
        return emojis[category] || '📋';
    }
}

// テスト実行
async function main() {
    const tester = new DetailedMinecraftTester();
    await tester.runDetailedTests();
}

main().catch(console.error);
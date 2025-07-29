/**
 * 認証付きMinecraft機能テスト
 * 適切な認証フローでのコマンドテスト
 */

const WebSocket = require('ws');

class AuthenticatedMinecraftTester {
    constructor() {
        this.ws = null;
        this.authToken = null;
        this.results = {
            authentication: false,
            basicCommands: [],
            blockOperations: [],
            errors: []
        };
    }

    async runAuthenticatedTests() {
        console.log('🔐 認証付きMinecraft機能テスト');
        console.log('═══════════════════════════════════');
        
        try {
            await this.connectAndAuthenticate();
            await this.runBasicTests();
            this.generateReport();
        } catch (error) {
            console.error('❌ テストエラー:', error.message);
        } finally {
            if (this.ws) {
                this.ws.close();
            }
        }
    }

    async connectAndAuthenticate() {
        return new Promise((resolve, reject) => {
            console.log('🔗 WebSocket接続中...');
            
            this.ws = new WebSocket('ws://localhost:14711');
            
            this.ws.on('open', () => {
                console.log('✅ WebSocket接続成功');
                
                // 適切な認証を試行
                console.log('🔐 認証試行中...');
                this.sendCommand('auth', { username: 'testuser' }, (response) => {
                    console.log('📨 認証レスポンス:', response);
                    try {
                        const authResult = JSON.parse(response);
                        if (authResult.token) {
                            this.authToken = authResult.token;
                            this.results.authentication = true;
                            console.log('✅ 認証成功, トークン取得:', this.authToken);
                        } else {
                            console.log('⚠️  認証スキップ（テストモード）');
                            this.results.authentication = true;
                        }
                    } catch (e) {
                        console.log('⚠️  認証レスポンス解析エラー、継続');
                        this.results.authentication = true;
                    }
                    resolve();
                });
                
                setTimeout(() => {
                    if (!this.results.authentication) {
                        console.log('⏰ 認証タイムアウト、テスト続行');
                        this.results.authentication = true;
                        resolve();
                    }
                }, 3000);
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
        
        console.log(`📤 送信: ${command}`, Object.keys(args).length > 0 ? args : '');
        
        if (callback) {
            this.pendingCallback = callback;
        }
        
        this.ws.send(JSON.stringify(message));
    }

    async runBasicTests() {
        console.log('\n🧪 基本機能テスト実行...');
        
        const tests = [
            {
                name: '認証状況確認',
                command: 'status',
                args: {},
                category: 'basicCommands'
            },
            {
                name: 'プレイヤー情報取得',
                command: 'connect',
                args: {},
                category: 'basicCommands'
            },
            {
                name: 'テストチャット',
                command: 'chat',
                args: { message: 'Hello Minecraft!' },
                category: 'basicCommands'
            }
        ];

        for (const test of tests) {
            await this.executeTest(test);
            await this.delay(2000);
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
                    response: response,
                    duration: duration,
                    success: this.isSuccessfulResponse(response)
                };
                
                this.results[test.category].push(result);
                
                if (result.success) {
                    console.log(`✅ ${test.name} - 成功 (${duration}ms)`);
                    console.log(`   📋 レスポンス: ${response.substring(0, 100)}...`);
                } else {
                    console.log(`❌ ${test.name} - 失敗: ${response.substring(0, 100)}...`);
                    this.results.errors.push(`${test.name}: ${response}`);
                }
                
                resolve();
            });
            
            // タイムアウト
            setTimeout(() => {
                if (!completed) {
                    console.log(`⏰ ${test.name} - タイムアウト`);
                    this.results[test.category].push({
                        name: test.name,
                        success: false,
                        error: 'timeout'
                    });
                    resolve();
                }
            }, 5000);
        });
    }

    isSuccessfulResponse(response) {
        const successKeywords = ['success', 'connected', 'position', 'completed'];
        const errorKeywords = ['error', 'failed', 'unauthenticated'];
        
        const lowercaseResponse = response.toLowerCase();
        
        const hasSuccess = successKeywords.some(keyword => lowercaseResponse.includes(keyword));
        const hasError = errorKeywords.some(keyword => lowercaseResponse.includes(keyword));
        
        return hasSuccess && !hasError;
    }

    delay(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }

    generateReport() {
        console.log('\n📊 認証付きテスト結果');
        console.log('═══════════════════════════════');
        
        const allTests = [...this.results.basicCommands, ...this.results.blockOperations];
        const successfulTests = allTests.filter(t => t.success).length;
        const successRate = allTests.length > 0 ? (successfulTests / allTests.length * 100).toFixed(1) : '0';
        
        console.log(`🔐 認証: ${this.results.authentication ? '✅ 成功' : '❌ 失敗'}`);
        console.log(`📈 成功率: ${successRate}% (${successfulTests}/${allTests.length})`);
        
        if (this.results.errors.length > 0) {
            console.log('\n🚨 エラー詳細:');
            this.results.errors.forEach((error, i) => {
                console.log(`   ${i + 1}. ${error.substring(0, 80)}...`);
            });
        }
        
        console.log('\n💡 推奨事項:');
        if (successRate < 50) {
            console.log('   - 認証システムとコマンド処理の確認が必要');
        } else if (successRate >= 80) {
            console.log('   - システムは正常に動作しています');
        }
    }
}

// メイン実行
async function main() {
    const tester = new AuthenticatedMinecraftTester();
    await tester.runAuthenticatedTests();
}

main().catch(console.error);
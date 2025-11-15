module.exports = {
    // テストファイルのパターン
    testMatch: [
        '**/__tests__/**/*.test.js',
        '**/?(*.)+(spec|test).js'
    ],

    // カバレッジ収集対象
    collectCoverageFrom: [
        'src/extensions/scratch3_minecraft/**/*.js',
        '!**/node_modules/**'
    ],

    // カバレッジディレクトリ
    coverageDirectory: 'coverage',

    // カバレッジレポーターの設定
    coverageReporters: [
        'text',           // コンソール出力
        'text-summary',   // サマリー
        'html',           // HTMLレポート
        'lcov'            // LCOVレポート（CIツールで使用）
    ],

    // テスト環境
    testEnvironment: 'node',

    // テストのタイムアウト（ミリ秒）
    testTimeout: 10000,

    // カバレッジの閾値（将来的に引き上げる）
    coverageThreshold: {
        global: {
            statements: 15,
            branches: 10,
            functions: 15,
            lines: 15
        }
    },

    // コンソール出力の抑制
    silent: false,

    // テスト結果の詳細表示
    verbose: true
};

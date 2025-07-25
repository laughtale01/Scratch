# Minecraft Collaboration Project - 引き継ぎ資料
## 作業日: 2025年1月12日

### プロジェクト概要
- **プロジェクトパス**: D:\minecraft_collaboration_project
- **目的**: Scratch 3.0とMinecraft 1.20.1を連携させる教育用コラボレーションシステム
- **主要機能**: WebSocket通信によるScratch拡張機能からMinecraftの操作

### 環境構築完了状況

#### 1. Java環境
- **インストール済み**: Java 17 (Eclipse Adoptium OpenJDK 17.0.15.6-hotspot)
- **JAVA_HOME**: `C:\Program Files\Eclipse Adoptium\jdk-17.0.15.6-hotspot`
- **状態**: ✅ 正常動作確認済み

#### 2. Node.js環境
- **インストール済み**: Node.js v24.4.0 (fnm経由)
- **npm**: v10.9.2
- **状態**: ✅ 正常動作確認済み

#### 3. Gradle環境
- **インストール済み**: Gradle 7.6.4
- **インストール方法**: 手動インストール（Chocolateyでは特定バージョンが見つからなかったため）
- **状態**: ✅ 正常動作確認済み

### 実装完了内容

#### Scratch拡張機能 (scratch-extension)
1. **実装済み機能**:
   - Minecraftへの接続/切断
   - ブロックの配置
   - プレイヤー位置の取得
   - チャットメッセージの送信
   - WebSocket通信の実装

2. **ビルド状態**: ✅ 成功
3. **ファイル**: `/scratch-extension/src/index.js`

#### Minecraft Mod (minecraft-mod)
1. **実装済み機能**:
   - WebSocketサーバー (ポート14711)
   - CollaborationServer
   - メッセージプロセッサー

2. **ビルド状態**: ✅ 成功
3. **実行時の問題**: ⚠️ WebSocketライブラリのクラスローディング問題

### 現在の課題と対応状況

#### 1. WebSocketライブラリのClassNotFoundException ✅ 解決済み (2025-01-25)
**問題**: 実行時に`org.java_websocket.server.WebSocketServer`が見つからない

**解決方法**:
1. ✅ jarJar設定を使用した適切な依存関係パッケージング
2. ✅ シングルプレイヤーモードでのWebSocketサーバー初期化を追加
3. ✅ FMLClientSetupEventでのサーバー起動実装

**最終的なbuild.gradle設定**:
```gradle
// Enable jarJar
jarJar.enable()

dependencies {
    minecraft 'net.minecraftforge:forge:1.20.1-47.2.0'
    
    // WebSocket support - using jarJar to include in mod
    jarJar(group: 'org.java-websocket', name: 'Java-WebSocket', version: '[1.5.4,)')
    implementation "org.java-websocket:Java-WebSocket:1.5.4"
    
    // JSON processing
    implementation 'com.google.code.gson:gson:2.10.1'
    
    // Logging
    implementation 'org.slf4j:slf4j-api:2.0.9'
    implementation 'org.slf4j:slf4j-simple:2.0.9'
}
```

**MinecraftCollaborationMod.javaの修正**:
- クライアント側でもWebSocketサーバーを起動するように`doClientStuff`メソッドを更新
- シャットダウンフックを追加して適切なクリーンアップを実装

### 次回作業時の推奨事項

1. **動作確認手順**:
   ```powershell
   cd D:\minecraft_collaboration_project\minecraft-mod
   .\gradlew.bat clean build
   .\gradlew.bat runClient
   ```

2. **WebSocket接続テスト**:
   ```powershell
   cd D:\minecraft_collaboration_project
   node test-websocket.js
   ```

3. **ログ確認ポイント**:
   - "WebSocket library is available" - ライブラリが正常にロードされた
   - "WebSocket server started successfully in single-player mode" - サーバーが起動した
   - "WebSocket server listening on port: 14711" - ポート14711でリッスン中

### ファイル構成
```
D:\minecraft_collaboration_project/
├── minecraft-mod/
│   ├── build.gradle (WebSocket依存関係設定)
│   ├── gradle.properties (Java設定)
│   └── src/main/java/com/yourname/minecraftcollaboration/
│       ├── MinecraftCollaborationMod.java
│       ├── network/
│       │   ├── WebSocketHandler.java
│       │   └── CollaborationMessageProcessor.java
│       └── server/
│           ├── CollaborationServer.java
│           └── CollaborationCoordinator.java
└── scratch-extension/
    ├── package.json
    ├── webpack.config.js
    └── src/
        └── index.js (Minecraft操作ブロック実装)
```

### 重要な注意事項
1. ユーザーはScratch GUI 1.16.5を使用しているが、プロジェクトはMinecraft 1.20.1用
2. PowerShellでのコマンド実行時、改行に注意（コピーペースト時の問題あり）
3. gradle.propertiesのJavaパスはWindows形式で記載

### 連絡事項
- ✅ WebSocketライブラリの問題は解決済み（2025-01-25）
- ✅ jarJar設定により、WebSocketライブラリが正しくパッケージングされるようになった
- ✅ シングルプレイヤーモードでもWebSocketサーバーが起動するように改善
- 次のステップ：Scratch拡張機能との統合テストと協調機能の実装
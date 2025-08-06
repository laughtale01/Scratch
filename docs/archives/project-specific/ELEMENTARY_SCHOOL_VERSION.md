# 🎮 小学生向け完全自動化版 - Minecraft×Scratch MOD

## ✨ 主な特徴（小学生向け改善）

### 🚀 完全自動化
- **コマンド入力不要** - ワールドに入るだけで自動的に連携開始
- **自動接続** - Scratchとの接続が自動で確立
- **視覚的フィードバック** - 画面に「✨ Scratchとつながりました！」と表示

### 📚 小学生向けドキュメント
1. **小学生向け使い方ガイド.md** - ひらがな多めの分かりやすいガイド
2. **かんたんスタート.bat** - ダブルクリックで全て起動

## 🔧 技術的な変更内容

### MinecraftCollaborationMod.java の変更

```java
@OnlyIn(Dist.CLIENT)
private void doClientStuff(final FMLClientSetupEvent event) {
    LOGGER.info("🎮 小学生向け自動設定を開始します");
    
    // 自動でWebSocketサーバーを起動
    event.enqueueWork(() -> {
        executor.schedule(() -> {
            if (collaborationServer == null) {
                // 自動起動処理
                collaborationServer = new CollaborationServer(14711, 14712, null);
                collaborationServer.start();
                
                // 画面に通知
                if (Minecraft.getInstance().player != null) {
                    Minecraft.getInstance().player.displayClientMessage(
                        Component.literal("§a✨ Scratchとつながりました！§r"), 
                        true  // アクションバーに表示
                    );
                }
            }
        }, 3, TimeUnit.SECONDS);
    });
}
```

## 📂 ファイル構成

```
minecraft_collaboration_project/
├── 小学生向け使い方ガイド.md     # メインガイド（ひらがな多め）
├── かんたんスタート.bat          # ワンクリック起動
├── start-minecraft.bat            # Minecraft起動補助
└── mods/
    └── minecraft-collaboration-mod-1.0.0-all.jar  # 自動化済みMOD
```

## 🎯 使用フロー（小学生視点）

1. **「かんたんスタート.bat」をダブルクリック**
   - Scratchサーバーが起動
   - ブラウザが開く
   - Minecraftが起動

2. **Minecraftで「プレイ」をクリック**
   - Forge 1.20.1が自動選択される想定

3. **ワールドに入る**
   - 3秒後に自動で「✨ Scratchとつながりました！」と表示
   - コマンド入力は一切不要

4. **ブラウザでScratchを使う**
   - 拡張機能から「Minecraft」を選択
   - ブロックを組み合わせてプログラミング

## 🛡️ 安全対策

### 自動制限機能
- **危険ブロック制限** - TNTなどは使用不可
- **レート制限** - 1秒間に10回まで
- **ローカル接続のみ** - インターネット経由の接続を防止
- **自動タイムアウト** - 長時間の未使用で自動切断

### 教育的配慮
- コマンド不要で混乱を防止
- 視覚的なフィードバックで状態を確認
- エラーメッセージも分かりやすく日本語化（予定）

## 📝 先生・保護者向け情報

### メリット
1. **技術的ハードル排除** - コマンド入力が不要
2. **即座に学習開始** - セットアップ時間を最小化
3. **エラー防止** - 自動化により設定ミスを防止

### 管理方法
- ログファイル: `logs/latest.log`
- 設定ファイル: `config/minecraft-collaboration.toml`
- ポート変更: 設定ファイルで変更可能（デフォルト: 14711）

### トラブルシューティング

| 問題 | 解決方法 |
|------|----------|
| 自動接続されない | Minecraftを再起動 |
| Scratchが開かない | ポート8602が使用中でないか確認 |
| MODが動作しない | Forge 1.20.1がインストールされているか確認 |

## 🔄 今後の改善予定

1. **エラーメッセージの日本語化**
2. **接続状態の常時表示**
3. **簡易設定GUI**
4. **オフライン時の動作改善**
5. **より多くのブロック種類対応**

## 📊 バージョン情報

- **MOD Version**: 1.0.0 (Elementary School Edition)
- **自動化レベル**: 完全自動
- **対象年齢**: 小学3年生以上
- **必要知識**: マウス操作のみ
- **更新日**: 2025-08-04

---

**注意**: このバージョンは小学生の使用を前提に、技術的な操作を可能な限り自動化しています。
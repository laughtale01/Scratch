# 🎉 Minecraft協調学習システム v1.0.0 リリースノート

## 📅 リリース日
2025年1月25日

## 🎯 概要
ScratchとMinecraft 1.20.1を連携した小学生向け協調学習システムの初回リリースです。
子供たちが楽しみながらプログラミングを学び、友達と協力して創造的な活動ができる環境を提供します。

## ✨ 主な機能

### 🤝 協調学習機能
- **友達招待システム**: 自分のワールドに友達を招待可能
- **ワールド訪問**: 友達のワールドへの訪問申請と承認
- **リアルタイム通知**: 招待や訪問申請の即時通知
- **安全な帰宅**: 通常帰宅と緊急帰宅の2種類

### 🔧 技術的特徴
- **Minecraft Forge 1.20.1**: 最新版のForge対応
- **Scratch 3.0拡張機能**: 7つの専用ブロック実装
- **WebSocket通信**: 安定した双方向通信（ポート14711）
- **jarJar統合**: 依存関係の適切なパッケージング

### 🛡️ 安全機能
- ローカルネットワーク限定接続
- 招待制による接続管理
- 緊急帰宅時の体力・空腹度自動回復
- 日本語での分かりやすい通知

## 📦 含まれるファイル

### Minecraft Mod
```
minecraft-mod/build/libs/minecraft-collaboration-mod-1.0.0-all.jar
```

### Scratch拡張機能
```
scratch-extension/dist/
├── minecraft-collaboration-extension.js
└── その他のビルドファイル
```

### ドキュメント
- `README.md` - プロジェクト概要
- `QUICKSTART.md` - クイックスタートガイド
- `docs/troubleshooting.md` - トラブルシューティング
- その他多数の技術ドキュメント

## 🚀 インストール方法

詳細は[QUICKSTART.md](QUICKSTART.md)を参照してください。

### 簡易手順
1. Minecraft Forge 1.20.1をインストール
2. Modファイルをmodsフォルダにコピー
3. Scratch 3.0で拡張機能を読み込み
4. Minecraftを起動して接続

## 🔧 動作環境

### 必須
- Minecraft Java Edition 1.20.1
- Minecraft Forge 47.2.0
- Java 17以上
- Scratch 3.0（デスクトップ版推奨）

### 推奨
- Windows 10/11
- メモリ 4GB以上
- ローカルネットワーク環境

## 📝 既知の問題

- インターネット経由の接続は未対応（ローカルネットワークのみ）
- 同時接続は10クライアントまでを推奨
- 大規模ワールドでのパフォーマンス最適化が必要

## 🙏 謝辞

このプロジェクトは以下の方々の貢献により実現しました：
- takecxさん - RemoteControllerModの基本設計
- Minecraft Forgeコミュニティ
- Scratch開発チーム

## 📄 ライセンス

MIT License - 教育目的での利用を推奨

## 🔮 今後の予定

- v1.1.0: グループ機能、時間制限機能
- v1.2.0: 教育機能強化、先生モード
- v2.0.0: マルチサーバー対応

詳細は[ROADMAP.md](ROADMAP.md)を参照

## 📞 フィードバック

問題報告や機能要望は以下へ：
- GitHub Issues: https://github.com/laughtale01/Scratch/issues

---

楽しい協調学習を！ 🎮✨
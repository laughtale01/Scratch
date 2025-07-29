# 🗑️ ファイル整理計画
## 作成日: 2025年7月26日

---

## 📋 整理対象ファイル

### 1. 移動対象ファイル

#### scripts/へ移動（ルートディレクトリのスクリプト）
- [ ] install-gradle-manual.ps1
- [ ] run-build.ps1
- [ ] setup-scratch-gui.sh
- [ ] test-minecraft-mod.sh

#### tests/へ移動（ルートディレクトリのテストファイル）
- [ ] browser-test.js
- [ ] quick-test.js
- [ ] update-extension-url.js

#### docs/archives/へ移動（HTMLテストファイル）
- [ ] scratch-gui-launcher.html
- [ ] scratch-page.html
- [ ] simple-websocket-test.html
- [ ] static-access.html
- [ ] test-connection.html
- [ ] test-status.html
- [ ] test-websocket-connection.html

### 2. 削除対象ファイル

#### 役割終了・古いレポート
- [ ] HANDOVER_REPORT_2025-01-12.md（古い引き継ぎレポート）
- [ ] FLYOUT_ZOOM_STATUS.md（解決済み問題）
- [ ] UI_IMPROVEMENTS_SUMMARY.md（実装完了）
- [ ] final-test-report.md（古いテストレポート）
- [ ] fix-scratch-borders.md（解決済み問題）
- [ ] QUALITY_REPORT_2025-07-25.md（新しいレポートで置き換え済み）
- [ ] TEST_RESULTS_2025-01-25.md（新しいレポートで置き換え済み）
- [ ] PROJECT_SUMMARY.md（他のレポートと重複）

#### 一時ファイル・設定
- [ ] "-c"（不明なディレクトリ）
- [ ] "webpack --mode=production"（不明なディレクトリ）
- [ ] "nul"（Windows一時ファイル）
- [ ] scratch-gui-border-fix.css（scratch-gui内に統合済み）
- [ ] scratch-gui-custom-fix.css（scratch-gui内に統合済み）

### 3. 統合対象ドキュメント

#### セットアップガイドの統合
現在のファイル：
- docs/setup/UNIFIED_SETUP_GUIDE.md（メイン）
- 他のセットアップガイドはarchivesへ

#### GitHub関連ドキュメントの統合
現在のファイル：
- docs/GITHUB_GUIDE.md（メイン）
- 他のGitHub関連はarchivesへ

### 4. 大きなディレクトリの処理

#### scratch-gui/（1GB以上）
**推奨アクション**:
1. 別リポジトリへ移動
2. または必要最小限のファイルのみ残す
3. .gitignoreに追加

#### node_modules/
- [ ] .gitignoreに追加（未追加の場合）

---

## 🔄 整理後の構造

```
minecraft_collaboration_project/
├── 📁 assets/              # 画像・リソース
├── 📁 config/              # 設定ファイル
├── 📁 docs/                # ドキュメント
│   ├── archives/           # 古いドキュメント
│   ├── setup/              # セットアップガイド
│   ├── development/        # 開発ガイド
│   └── api/               # API仕様
├── 📁 minecraft-mod/       # Minecraft Mod
├── 📁 scratch-extension/   # Scratch拡張
├── 📁 scripts/             # スクリプト
├── 📁 tests/               # テスト
├── 📄 README.md            # プロジェクト概要
├── 📄 CLAUDE.md            # Claude設定
├── 📄 LICENSE              # ライセンス
└── 📄 .gitignore           # Git除外設定
```

---

## 🚀 実行手順

### フェーズ1: ファイル移動
1. scripts/ディレクトリへスクリプト移動
2. tests/ディレクトリへテストファイル移動
3. docs/archives/へ古いドキュメント移動

### フェーズ2: ファイル削除
1. 役割終了したレポートの削除
2. 一時ファイルの削除
3. 重複ファイルの削除

### フェーズ3: 最終確認
1. プロジェクト構造の確認
2. .gitignoreの更新
3. READMEの更新

---

## ⚠️ 注意事項

1. **削除前の確認**
   - 重要な情報が含まれていないか確認
   - 必要に応じてバックアップ

2. **Git履歴の保持**
   - 削除してもGit履歴から復元可能
   - 必要に応じてタグを付ける

3. **依存関係の確認**
   - 他のファイルから参照されていないか確認
   - リンクの更新が必要な場合は実施

---

## 📊 期待される効果

1. **構造の明確化**
   - ファイルの場所が予測可能に
   - 新規開発者の理解が容易に

2. **メンテナンス性向上**
   - 関連ファイルがまとまる
   - 更新対象が明確に

3. **リポジトリサイズ削減**
   - 不要ファイルの削除
   - scratch-guiの分離検討

---

最終更新: 2025年7月26日
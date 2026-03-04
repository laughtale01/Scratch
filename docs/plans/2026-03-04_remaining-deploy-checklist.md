# 次回セッション用: 残デプロイチェックリスト

作成日: 2026-03-04  
最終更新: 2026-03-04  
対象プロジェクト: `laughtale-scratch-bcc8a`  
対象ブランチ: `gh-pages`  
対象コミット: `c0b325f`

## 0. 現在ステータス（2026-03-04 更新）

- 本ドキュメントの「残デプロイ」対象は実行完了。
- `functions` は単体再実行で `Deploy complete!` を確認。
- `firestore.rules` / `firestore.indexes.json` / `storage.rules` は再デプロイ完了。
- `gh-pages` は `origin/gh-pages` と `c0b325f` で一致。
- 注意: `firebase.json` に `hosting` セクションがないため、Firebase Hosting 反映は本手順の対象外。

## 1. 前回セッションの状況

- `git push origin gh-pages` は完了済み。
- `predeploy_gallery_auth.ps1 -Deploy -AllowIncompleteExecutionArtifacts` 実行時、
  `functions` 解析フェーズで失敗:
  - `Error: User code failed to load. Cannot determine backend specification. Timeout after 10000`
- よって、`functions` デプロイは未完了。
- 同一実行で `hosting/firestore rules/indexes/storage rules` が反映済みかは未確定（次回冒頭で再デプロイして確定させる）。

## 2. 実施結果（旧: 次回に実施する残デプロイ）

1. `functions` デプロイ再実行（完了）
2. `hosting/firestore:rules/firestore:indexes/storage` の再デプロイ（完了）
3. 補足: 実行時は PowerShell の `--only` 解釈を避けるため `--only "..."` で実施

## 3. 実行手順と結果

1. ログイン確認
   - `firebase login:list`
   - 結果: 実行済み（`laughtale.education@gmail.com` でログイン確認）

2. まず静的ゲート
   - `powershell -ExecutionPolicy Bypass -File .\scripts\run_verification_suite.ps1`
   - 結果: 実行済み（`PASSED`）

3. 本番反映（今回の残件をまとめて再実行）
   - `powershell -ExecutionPolicy Bypass -File .\scripts\predeploy_gallery_auth.ps1 -Deploy -AllowIncompleteExecutionArtifacts`
   - 結果: 実行済み（`functions` 解析タイムアウトで失敗）

4. もし `functions` だけ失敗した場合の切り分け再実行
   - `firebase deploy --project laughtale-scratch-bcc8a --only functions`
   - `firebase deploy --project laughtale-scratch-bcc8a --only hosting,firestore:rules,firestore:indexes,storage`
   - 結果: 実行済み
     - `functions`: `Deploy complete!`
     - `firestore rules/indexes/storage`: `Deploy complete!`

## 4. 完了判定（Done条件）

- 判定: 完了
- 確認:
  - デプロイコマンドが `exit code 0` で終了
  - Firebase CLI 出力に `Deploy complete!` が表示
  - 少なくとも以下が反映完了:
  - `functions/index.js`（`auditUserDocumentCoverage` 含む）
  - `firestore.rules`
  - `firestore.indexes.json`
  - `storage.rules`
  - `gallery.html`, `admin.html`, `admin.js`, `admin-mobile.html`, `firebase-integration.js`

## 5. 失敗時の即時チェック項目

1. Firebase再認証
   - `firebase login --reauth`
   - 状態: 今回は未実施（デプロイ成功のため不要）

2. Functions依存関係の健全性
   - `cd functions`
   - `npm ci`
   - `node --check index.js`
   - `cd ..`
   - 状態: `node --check index.js` は静的ゲートで実施済み。`npm ci` は未実施（今回は不要）

3. Functions単体デプロイで再試行
   - `firebase deploy --project laughtale-scratch-bcc8a --only functions`
   - 状態: 実施済み（成功）

4. それでも失敗する場合
   - エラーログ全文を `docs/testing/logs/` に保存して原因切り分けを継続
   - 状態: 最終的に成功したため未実施

## 6. 補足

- 現在の計画進捗は実装ベースで高いが、E2E/障害注入/運用証跡は未完了。
- 本ドキュメントは「デプロイ残件」のみを対象とする。

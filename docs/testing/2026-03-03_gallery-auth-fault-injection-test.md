# Gallery/Auth Fault Injection Test (Manual)

## Goal

- ギャラリー表示不具合の再発防止のため、想定障害時の挙動が設計どおりかを確認する。

## Preconditions

- `scripts/predeploy_gallery_auth.ps1` dry-run が成功していること
- 対象環境に最新デプロイが反映済みであること
- admin / teacher / student テストアカウントを利用できること

## Cases

1. Firestore read permission error
- Setup:
  - 一時的に対象ユーザーの `users/{uid}` または `projects` 読み取りを拒否する条件を作る
- Action:
  - ログイン後に `gallery.html` を開く
- Expected:
  - 読み込みエラーが利用者向け文言で表示される
  - 再試行ボタンが表示される
  - 前ユーザーのデータが残存しない

2. Missing index (`failed-precondition`) simulation
- Setup:
  - 検証環境で該当複合indexを一時未適用にする
- Action:
  - teacher / student でギャラリーを開く
- Expected:
  - インデックス不足相当のエラーが表示される
  - 画面がハングせず再試行可能

3. Network unstable (`unavailable`) simulation
- Setup:
  - ブラウザDevToolsのNetworkをOfflineにする
- Action:
  - ギャラリー読み込み、再試行操作
- Expected:
  - 接続不安定文言が表示される
  - Online復帰後に再試行で回復できる

4. Missing users document
- Setup:
  - Authユーザーを残し `users/{uid}` を一時的に欠損状態にする
- Action:
  - 該当ユーザーでログインしギャラリー表示
- Expected:
  - 設定未整備を示すエラー表示
  - 画面が壊れず再試行可能

5. Teacher without classroomId
- Setup:
  - teacherユーザーの `classroomId` を未設定にする
- Action:
  - ログインしてギャラリー表示
- Expected:
  - 「講師アカウントに教室が設定されていません」系の明示エラー
  - 空表示誤判定にならない

## Completion Criteria

- 全ケースで期待挙動を確認
- 失敗ケースは再現手順とログを記録
- 結果を `2026-03-03_gallery-auth-fault-injection-results-template.md` に記録

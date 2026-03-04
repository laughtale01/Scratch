# ギャラリー表示不具合（独自ログイン連携）詳細実装計画書

作成日: 2026-03-03
対象リポジトリ: `D:\laughtale01-scratch`
対象機能: `gallery.html` / `firebase-integration.js` / `firestore.rules` / `firebase.json`

---

## 1. 目的

独自ログイン利用時に `gallery.html` が正常表示されない問題について、現行実装を1つずつ確認したうえで、再現性のある原因切り分けと安全な改修計画を策定する。

本計画は次を満たす。

- 原因を「認証」「権限」「クエリ」「状態管理」「導線」の5層で分離する
- 修正を段階的に導入し、各段階で検証可能にする
- 既存機能（クラウド保存/読み込み、管理画面、設定画面）への副作用を抑える
- ロールバック可能なデプロイ手順を明示する

---

## 2. 現行実装の逐次確認（事実ベース）

### 2.1 ギャラリーへの導線

対象: `firebase-integration.js`

- `openGallery()` で `window.open('gallery.html', '_blank')` を実行している
- ギャラリーメニュー項目は全ログインユーザーに表示される（ロール非依存）
- つまり `admin/teacher/student` すべてが同一の `gallery.html` を開く設計

確認ポイント:

- UI制御では `admin` 専用リンクだけを出し分けているが、ギャラリーは出し分けていない
- 「表示可能なロール」と「Firestoreで読めるデータ範囲」の整合を取っていない

### 2.2 ギャラリー初期化処理

対象: `gallery.html`

- `auth.onAuthStateChanged` でログイン状態を監視
- ログイン済みなら `didLoad` が `false` のときだけ `loadData()` を1回実行
- 未ログインならログインオーバーレイを表示

確認ポイント:

- 初回読み込み失敗時でも `didLoad=true` になるため、同一ページ内で再試行されない
- ログアウト後に別ユーザーで再ログインしても `loadData()` が再実行されない

### 2.3 ギャラリーデータ取得クエリ

対象: `gallery.html` `loadData()`

実行順:

1. `db.collection('classrooms').get()`
2. `db.collection('users').get()`
3. `db.collection('projects').where('isSubmitted', '==', true).orderBy('submittedAt', 'desc').get()`

確認ポイント:

- 全件取得を前提としているため、ロール別ルールと衝突しやすい
- 1つでも失敗すると `catch` に入り、全体表示が失敗する
- 部分表示へのフォールバックがない

### 2.4 Firestoreセキュリティルール

対象: `firestore.rules`

要点:

- `users` 読み取り: admin=全件、teacher=自教室+自分、student=自分のみ
- `classrooms` 読み取り: admin=全件、それ以外=自教室のみ
- `projects` 読み取り: admin=全件、teacher=自教室、student=自分のみ
- 多くの判定が `getUserData()`（`users/{request.auth.uid}`）前提

確認ポイント:

- `users.get()` / `classrooms.get()` の無条件全件クエリは `teacher/student` で拒否される
- `projects where isSubmitted==true` も `student` では自分以外が混在し拒否される
- 独自ログイン経由で Auth ユーザーは存在しても `users/{uid}` 未作成なら判定が成立しない

### 2.5 インデックス定義の管理状態

対象: `firebase.json`

- `firestore.rules` のみ定義
- `firestore.indexes.json` の参照定義なし
- リポジトリ内にも index 定義ファイルがない

確認ポイント:

- `where(isSubmitted) + orderBy(submittedAt)` は複合インデックス要求が発生しやすい
- 環境依存（コンソール手動作成済みか否か）になるため再現性が低い

---

## 3. 根本原因（優先度順）

### P0: 権限制約と全件クエリの不整合

- ギャラリーのクライアントクエリが全件前提
- ルールはロールごとに閲覧範囲を制限
- 結果として teacher/student で恒常的に `permission-denied`

### P0: ログイン導線の公開範囲と実際の閲覧可能範囲が不一致

- 全ロールにギャラリー入口を出している
- しかし student 向けに成立するクエリ条件が実装されていない

### P1: インデックス管理がコード化されていない

- クエリ失敗が環境依存になる
- 新環境で `failed-precondition` が再発しやすい

### P1: `didLoad` による再読込阻害

- 失敗復帰・ユーザー切替時に UI が古い/空のまま固定される

### P2: 独自ログインと `users` ドキュメント整備の契約不明確

- 認証成功しても `users/{uid}` が未整備ならルール評価で詰まる

---

## 4. 改修方針（採用案）

### 方針A（推奨）: ロール別クエリに分離し、ルール準拠で表示

概要:

- `gallery.html` でまず `users/{uid}` を取得してロールを特定
- ロールごとに取得クエリを切り替える
- 全件取得をやめ、許可範囲内のクエリだけ実行

ロール別の想定:

- admin: 現行同等（全提出作品）
- teacher: 自教室の提出作品
- student: 自分の提出作品（または自教室公開のみに変更するなら別ルール設計）

利点:

- 現行ルールを大きく変えずに実装可能
- 不要な個人情報全件読み取りを抑止

### 方針B（代替）: ギャラリーを教室公開向けにルールを拡張

概要:

- student に「同教室提出作品の読み取り」を許可するルールを追加
- クエリも `classroomId == 自教室` を必須化

注意:

- 権限緩和を伴うため、要件合意とレビュー必須

本計画では、まず方針Aで安定化し、必要に応じて方針Bを後続検討とする。

---

## 5. 詳細実装計画（タスク分解）

## Phase 0: 事前準備（半日）

1. 現行挙動の再現ログを取得
- ブラウザコンソールで `permission-denied` / `failed-precondition` を記録
- ロール別（admin/teacher/student）で同一手順を実施

2. 独自ログインのユーザープロビジョニング契約確認
- Auth作成時に `users/{uid}` が確実に作られるか
- `role`, `classroomId`, `displayName` の必須保証を確認

成果物:

- ロール別再現表
- エラーコード一覧

## Phase 1: ギャラリーのデータ取得再設計（1日）

対象ファイル:

- `gallery.html`

実装項目:

1. `loadData()` を分割
- `loadCurrentUserProfile()`
- `loadClassroomsForRole(profile)`
- `loadUsersForRole(profile)`
- `loadSubmittedProjectsForRole(profile)`

2. ロール別クエリ条件を導入
- admin: 現行同等
- teacher: `where('classroomId', '==', profile.classroomId)` を利用
- student: `where('userId', '==', auth.currentUser.uid)` を利用

3. エラーハンドリングを分離
- `users` 取得失敗時も `projects` は表示できるよう degrade
- 画面上に「一部情報のみ表示中」を明示

4. `didLoad` の見直し
- ログアウト時に `didLoad=false`、データ配列初期化
- ログインユーザーが変わったら必ず再ロード
- `loadData` 成功時のみ `didLoad=true` に変更

受け入れ条件:

- teacher/student で `permission-denied` が発生しない
- ユーザー切替時に表示内容が追従する

## Phase 2: UI導線と表示仕様の整合（0.5日）

対象ファイル:

- `firebase-integration.js`
- `gallery.html`

実装項目:

1. ギャラリー導線の表示条件を明文化
- 全ロール表示を維持するか、teacher/admin 限定にするか決定
- 決定に合わせてメニュー表示制御を追加

2. ギャラリーヘッダーに閲覧スコープ表示
- 例: `表示範囲: 自教室 / 自分の提出作品 / 全教室`

3. 空状態文言をロール別に最適化
- student: 「自分の提出作品はまだありません」
- teacher: 「この教室の提出作品はまだありません」

受け入れ条件:

- UI上の説明と実データ範囲が一致する

## Phase 3: Firestoreインデックスのコード化（0.5日）

対象ファイル:

- `firestore.indexes.json`（新規）
- `firebase.json`（参照追加）

実装項目:

1. 複合インデックス定義を追加
- `projects`: `isSubmitted ASC`, `submittedAt DESC`
- teacherクエリ用に必要なら `classroomId ASC`, `isSubmitted ASC`, `submittedAt DESC`
- studentクエリ用に必要なら `userId ASC`, `isSubmitted ASC`, `submittedAt DESC`

2. デプロイ手順を文書化
- `firebase deploy --only firestore:indexes`

受け入れ条件:

- 新規環境でもクエリエラーが再発しない

## Phase 4: ルール調整（必要時のみ、0.5日）

対象ファイル:

- `firestore.rules`

実装項目:

- 方針Aで解決するならルール変更なし
- 方針Bを採用する場合のみ student の同教室提出作品閲覧を最小権限で追加

受け入れ条件:

- 権限レビュー承認
- 既存管理画面のアクセス制御を破壊しない

## Phase 5: 結合テスト・回帰テスト（1日）

対象:

- `index.html` + `firebase-integration.js`
- `gallery.html`
- `admin.html`
- `settings.html`

実施内容:

1. ロール別E2E
- admin: 全提出作品表示、絞り込み、プレビュー
- teacher: 自教室のみ表示
- student: 自分のみ（または仕様確定範囲）

2. 認証状態遷移
- 未ログイン -> ログイン -> ログアウト -> 別ユーザーログイン
- タブをまたいだセッション共有確認

3. エラー注入
- `users` ドキュメント欠損ユーザーでログイン
- インデックス未作成環境での失敗メッセージ確認

4. 回帰確認
- クラウド保存/読み込み
- 提出/取消
- 管理画面遷移
- 設定画面遷移

受け入れ条件:

- 重大バグ0件、既知軽微不具合のみ

---

## 6. 実装時の詳細仕様

### 6.1 ギャラリーの内部状態モデル

追加する状態:

- `currentProfile`:
  - `uid`
  - `role`
  - `classroomId`
  - `displayName`
- `lastLoadedUid`
- `loadStatus`: `idle/loading/partial/success/error`
- `loadErrors`: `{ users?: string, classrooms?: string, projects?: string }`

このモデルを導入し、`didLoad` 単独管理を廃止する。

### 6.2 取得順序の標準化

1. 認証済みユーザー確認
2. `users/{uid}` 取得（存在しない場合は明示エラー）
3. role確定
4. roleに応じたクエリ構築
5. プロジェクト取得（最重要）
6. 付随情報（users/classrooms）取得
7. 描画

### 6.3 部分表示ポリシー

- プロジェクト取得成功 + 補助データ失敗時:
  - カード表示は続行
  - 作成者名は `不明`
  - 教室名は `-`
  - ヘッダーに注意文を表示

- プロジェクト取得失敗時:
  - 空状態ではなくエラー状態を表示
  - 再試行ボタンを表示

### 6.4 エラーメッセージ標準化

想定コード:

- `permission-denied`
- `failed-precondition`
- `unauthenticated`
- `unavailable`

表示方針:

- ユーザー向け: 短い日本語メッセージ
- 開発者向け: `console.error` に詳細コード・クエリ条件・uid/roleを出力

### 6.5 独自ログイン連携の契約

必須契約:

- Auth成功時点で `users/{uid}` が存在
- `role` は `admin|teacher|student`
- `classroomId` は `teacher/student` で必須

不足時の挙動:

- ギャラリーで「アカウント設定が未完了です」を表示
- 管理者連絡導線を提示

---

## 7. 変更対象ファイル一覧

必須変更:

- `gallery.html`（主要改修）
- `firebase-integration.js`（導線表示制御）
- `firebase.json`（indexes参照）
- `firestore.indexes.json`（新規）

条件付き変更:

- `firestore.rules`（要件次第）

ドキュメント更新:

- `docs/firebase-cloud-save-spec.md`（ギャラリー閲覧範囲仕様）

---

## 8. テスト計画（詳細マトリクス）

### 8.1 ロール x データ可視範囲

1. admin
- 期待: 全提出作品が件数一致
- クエリエラー: なし

2. teacher
- 期待: 自教室提出作品のみ
- 他教室作品の非表示保証

3. student
- 期待: 自分の提出作品のみ（方針A）
- 他人作品が見えないこと

### 8.2 認証遷移

1. 未ログインで `gallery.html` 直アクセス
- 期待: ログインオーバーレイ表示

2. ログイン成功
- 期待: ローディング -> 一覧表示

3. ログアウト
- 期待: 状態クリア、オーバーレイ再表示

4. 別ユーザー再ログイン
- 期待: 前ユーザーの表示が残らない

### 8.3 障害注入

1. `users/{uid}` 欠損
- 期待: 明示エラー、クラッシュしない

2. インデックス未作成
- 期待: `failed-precondition` を捕捉し案内表示

3. ネットワーク断
- 期待: 再試行可能UI

---

## 9. デプロイ計画

1. `gallery.html` の改修を先行デプロイ（ルール変更なし）
2. `firestore.indexes.json` を追加し index デプロイ
3. 反映待ち（インデックス作成完了監視）
4. 結合テスト
5. 必要時のみ `firestore.rules` を最小変更でデプロイ

注意:

- インデックスは作成完了まで時間がかかる可能性あり
- 完了前は既知エラーとして扱う

---

## 10. ロールバック計画

ロールバック条件:

- teacher/student で新規 `permission-denied` 増加
- admin の一覧表示遅延が許容閾値超過
- クラウド保存導線に副作用発生

手順:

1. `gallery.html` / `firebase-integration.js` を直前タグへ戻す
2. `firestore.rules` 変更があれば同時に戻す
3. インデックスは残置可（無害）
4. 障害報告テンプレートで再発防止タスク化

---

## 11. 実装順チェックリスト

- [x] ロール別クエリ設計を確定
- [x] `gallery.html` の状態管理を再設計
- [x] 部分表示・再試行UIを実装
- [x] `didLoad` 問題を解消
- [x] ギャラリー導線表示方針を確定
- [x] `firestore.indexes.json` を追加
- [x] `firebase.json` に indexes 参照追加
- [ ] ロール別E2Eを実施
- [ ] 障害注入テストを実施
- [x] 運用手順書更新
- [x] 実行証跡ゲート（predeploy/verification）を実装
- [x] リリース可否レポート自動生成を実装

---

## 12. 工数見積もり

- Phase 0: 0.5日
- Phase 1: 1.0日
- Phase 2: 0.5日
- Phase 3: 0.5日
- Phase 4: 0.5日（必要時）
- Phase 5: 1.0日

合計:

- ルール変更なし: 約3.5日
- ルール変更あり: 約4.0日

---

## 13. 追加メモ（実装時の注意）

- Firestoreルールは「許可されるドキュメント集合に対してクエリ条件が十分絞られているか」を満たす必要がある
- UIで隠してもセキュリティは強化されないため、必ずルール準拠クエリで実装する
- エラー時に `error.message` をそのままユーザー表示しすぎない（内部情報漏えい回避）
- 既存の `admin.html` / `admin-mobile.html` も同様の全件クエリ問題を含む可能性があるため、別チケットで横展開調査する

---

## 14. プラン妥当性レビュー結果（2026-03-03）

本計画に対して、現実装の静的検証を実施した結果を以下に記載する。

### 14.1 結論

- 実装可能性: 高い（段階導入で安全に実装可能）
- 動作成立性: 高い（ただし下記「必須修正」を反映することが条件）
- 他機能への負の影響: 低い（変更範囲を `gallery.html` 主体に限定した場合）

### 14.2 必須修正（計画に反映済みとみなす前提条件）

1. 非adminでの `users` 全体取得を禁止する
- teacher/student で `users` をコレクションクエリするとルールと衝突しやすい
- 対応:
  - admin: 既存どおり一覧取得可
  - teacher: プロジェクトに出現した `userId` のみ `doc(id).get()` で補完
  - student: `users/{currentUid}` のみ取得

2. 非adminでの `classrooms` 全体取得を禁止する
- 対応:
  - admin: 一覧取得可
  - teacher/student: `classrooms/{currentUser.classroomId}` の単票取得のみ

3. `didLoad` は成功時確定に変更する
- 失敗時に `didLoad=true` へ進むと再試行不能になる
- `loadData()` 成功時にのみロード完了状態を確定する

4. `openInScratch` の既存導線は別途不整合
- 現状 `index.html?loadProject=...` を解釈する処理が確認できない
- 本件改修のスコープ外でも、ギャラリーUX上は未完のため別タスク化が必要

### 14.3 追加で必要なインデックス（推奨）

- `projects`: `isSubmitted ASC, submittedAt DESC`
- `projects`: `classroomId ASC, isSubmitted ASC, submittedAt DESC`
- `projects`: `userId ASC, isSubmitted ASC, submittedAt DESC`
- `projects`: `userId ASC, updatedAt DESC`（既存クラウド読み込みクエリの環境安定化）

### 14.4 他機能影響評価

1. `firebase-integration.js`（クラウド保存/読み込み）
- 影響: 低
- 理由: 主要改修は `gallery.html` 内で完結可能。`openGallery` 表示制御のみなら副作用は限定的。

2. `settings.html`
- 影響: なし
- 理由: `users/{uid}` 単票アクセスのみで、計画変更と競合しない。

3. `admin.js` / `admin-mobile.html`
- 影響: なし（直接変更しない場合）
- 注意: 既存で teacher + classroom未設定時にクエリ失敗潜在リスクがあるため別件管理推奨。

4. `firestore.rules`
- 影響: 低
- 理由: 方針A（ルール据え置き、クライアントをルール準拠化）なら既存権限モデルを維持できる。

### 14.5 未解決リスク（実機検証が必要）

- Firebase実環境でのインデックス反映待ち時間による一時エラー
- 独自ログイン基盤側で `users/{uid}` を未作成のアカウント混在
- 大量データ時の `users` 補完取得（doc個別取得）のレイテンシ

### 14.6 Go/No-Go判定

- Go条件:
  - 14.2 の必須修正を設計に組み込む
  - 14.3 のインデックスを事前作成する
  - Phase 5 のロール別E2Eを通過する

- No-Go条件:
  - `users/{uid}` 欠損アカウントが多数残存
  - teacher/student で `permission-denied` が再発

---

## 15. 仕様確定事項（実装前に固定するルール）

### 15.1 閲覧範囲の確定（方針A）

- admin: 全提出作品を表示
- teacher: 自教室の提出作品のみ表示
- student: 自分の提出作品のみ表示

### 15.2 データ取得ポリシー

- adminのみ `users` / `classrooms` の一覧取得を許可
- teacher/student は単票取得または必要最小限の補完取得のみ許可
- 取得失敗時は部分表示を許容し、全面失敗を避ける

### 15.3 失敗時のUIポリシー

- `projects` 取得失敗: エラー状態 + 再試行ボタン
- `users/classrooms` 取得失敗: 一覧は表示継続 + 注意バナー
- `users/{uid}` 不在: 設定未完了エラーを表示して処理停止

---

## 16. 実装差分の具体化（変更単位）

### 16.1 `gallery.html`

追加:

- `currentProfile`, `lastLoadedUid`, `loadStatus`, `loadErrors`
- `loadCurrentUserProfile()`
- `buildProjectQueryByRole(profile)`
- `loadAuxiliaryDataByRole(profile, projects)`
- `renderNoticeBanner()`
- `retryLoadData()`

変更:

- `loadData()` をオーケストレーション関数へ再編
- `didLoad` を削除し `lastLoadedUid + loadStatus` 管理へ置換
- `auth.onAuthStateChanged` でユーザー切替検知時に必ず再ロード

削除:

- 非adminで成立しない全件取得前提ロジック

### 16.2 `firebase-integration.js`

最小変更:

- 必要に応じてギャラリー導線に説明文またはロール別表示制御を追加
- 既存の保存/読込/提出ロジックには手を入れない

### 16.3 `firebase.json` と `firestore.indexes.json`

- `firebase.json` に indexes 参照を追加
- `firestore.indexes.json` を新規作成し、計画済み複合インデックスを定義

---

## 17. 検証プロトコル（実行手順）

### 17.1 事前データセット

- admin 1名
- teacher 2名（別教室）
- student 4名（各教室2名）
- 提出済みプロジェクトを教室横断で最低6件作成

### 17.2 手動E2E（必須）

1. student Aでギャラリー表示
- 自分提出のみ見える
- 他生徒作品は表示されない

2. teacher Aでギャラリー表示
- 教室A作品のみ見える
- 教室B作品は表示されない

3. adminでギャラリー表示
- 全教室作品が見える

4. 同一タブでログアウト -> 別ロールで再ログイン
- 表示範囲が前ユーザーのキャッシュを引き継がない

5. 補助データ失敗試験
- `users` 参照を意図的に失敗させても一覧が継続表示される

### 17.3 回帰確認（必須）

- クラウドに保存
- クラウドから読み込み
- 提出/提出取消
- 管理画面遷移
- 設定画面遷移

---

## 18. 監視と運用（リリース後）

### 18.1 収集ログ

- `gallery_load_start`（uid, role）
- `gallery_load_success`（件数, 所要ms）
- `gallery_load_partial`（失敗種別）
- `gallery_load_error`（error.code）

### 18.2 アラート閾値

- `permission-denied` が24時間で一定閾値超過
- `failed-precondition` が連続発生
- 平均ロード時間が基準値超過

### 18.3 初期運用期間

- リリース後7日間を監視強化期間とする
- 重大エラー再発時は即時ロールバック判断

---

## 19. 中止・切戻し判定の厳格化

即時切戻し条件:

- adminで一覧0件化（データ存在時）
- teacher/student で閲覧範囲逸脱（他教室/他人作品表示）
- 保存/読込/提出のいずれかが失敗率上昇

段階停止条件:

- インデックス未反映が長時間継続
- `users/{uid}` 欠損が運用許容を超える

---

## 20. 完了定義（Definition of Done）

- 仕様確定事項（Section 15）と実装一致
- 手動E2E（Section 17.2）全通過
- 回帰確認（Section 17.3）全通過
- インデックス定義がリポジトリ管理下にある
- リリース後監視項目（Section 18）が運用に組み込まれている

---

## 21. 別側面レビュー（追加検証結果）

本セクションは、既存の権限/クエリ整合以外の観点で、実装可能性と安全性を再評価した結果を記載する。

### 21.1 データ完全性（Data Integrity）

判定: 追加対策が必要

確認結果:

- `functions/index.js` の `createUser` は Auth作成成功後に Firestore書き込み失敗した場合、Authのみ作成された不整合状態が残る。
- `createUsers` も同様に部分失敗時の補償トランザクションがない。
- この不整合ユーザーはログインできても `users/{uid}` が欠損し、ギャラリーや管理機能で `permission-denied` を誘発する。

対策（計画追加）:

- Cloud Functions に補償処理を追加（Firestore保存失敗時は `auth.deleteUser(uid)` を試行）
- `users/{uid}` 欠損検知ジョブ（管理者向け監査）を追加
- 既存欠損ユーザーの修復スクリプトを準備

### 21.2 ルール関数の堅牢性（Rules Hardening）

判定: 要注意

確認結果:

- `firestore.rules` の `getUserData()` は呼び出し元の `users/{uid}` 存在を前提としている。
- 欠損時の評価で拒否が連鎖し、原因切り分けが難しくなる。

対策（計画追加）:

- `hasUserData()` を導入し、存在確認を先に行うヘルパー構成へ改修を検討
- エラー時は拒否は維持しつつ、クライアント側表示を「設定未完了」に統一

### 21.3 性能・スケーラビリティ（Performance）

判定: admin大規模データでリスクあり

確認結果:

- ギャラリーはページングなしで全件描画する実装構造。
- teacherで作成者名補完を `doc(id).get()` 連発で実装するとN+1問題が発生する。

対策（計画追加）:

- 初期表示件数上限（例: 50件）+ 続きを読む導入
- `userId` 補完は `documentId in (...)` のチャンク取得（10件単位）を基本とする
- 将来の大量データ向けにサーバー側集約API化を検討

### 21.4 UX整合性（機能期待との一致）

判定: 一部別タスク化が必要

確認結果:

- ギャラリーの「Scratchで開く」は `index.html?loadProject=...` 連携だが、現実装で受け取り処理が確認できない。
- ギャラリー修正と直接衝突はしないが、利用者には「開けない」不具合として認識される可能性が高い。

対策（計画追加）:

- 本件とは別チケットで「URL経由ロード」実装有無を確定し、未実装なら導入
- それまでの暫定措置としてボタン文言に制約表示を検討

### 21.5 運用・障害復旧性（Operability）

判定: 強化余地あり

確認結果:

- 現在は画面ログ中心で、障害の早期検知が難しい。

対策（計画追加）:

- 監視イベントに `uid`,`role`,`queryType`,`error.code` を含める
- `permission-denied` の急増を運用アラート化
- リリース後7日間の観測指標を明文化

### 21.6 他機能への副作用再評価

判定: 低リスク（条件付き）

条件:

- 変更を `gallery.html` と index定義追加に限定する
- `firebase-integration.js` は導線表示制御のみ変更する
- `firestore.rules` は方針Aでは原則据え置き

注意:

- Cloud Functions 補償処理を同時導入する場合は、ユーザー管理系（admin.js）で回帰確認を必須化する

### 21.7 追加Go条件

既存Go条件に加えて以下を満たすこと。

- 欠損ユーザー（Authのみ/Firestoreのみ）の件数が把握され、修復手順が用意されている
- teacher向け補完取得の実装がN+1対策済みである
- 初期表示件数上限とページング方針が決定している

---

## 22. 実装テンプレート（疑似コード）

### 22.1 `gallery.html` ロードフロー

```javascript
async function loadGalleryForCurrentUser() {
  setLoadStatus('loading');
  clearState();

  const authUser = auth.currentUser;
  if (!authUser) return showLogin();

  const profile = await loadCurrentUserProfile(authUser.uid); // users/{uid}
  if (!profile) return showConfigError();

  const projectQuery = buildProjectQueryByRole(profile);
  const projects = await projectQuery.limit(PAGE_SIZE).get();

  const aux = await loadAuxiliaryDataByRole(profile, projects);
  // aux取得失敗は致命扱いしない
  if (aux.partialErrors.length > 0) setLoadStatus('partial');
  else setLoadStatus('success');

  render(projects, aux);
}
```

### 22.2 ロール別クエリ構築

```javascript
function buildProjectQueryByRole(profile) {
  let q = db.collection('projects').where('isSubmitted', '==', true);
  if (profile.role === 'admin') return q.orderBy('submittedAt', 'desc');
  if (profile.role === 'teacher') {
    return q.where('classroomId', '==', profile.classroomId).orderBy('submittedAt', 'desc');
  }
  return q.where('userId', '==', profile.uid).orderBy('submittedAt', 'desc');
}
```

---

## 23. インデックス定義サンプル（初版）

`firestore.indexes.json` に最低限以下を含める。

```json
{
  "indexes": [
    {
      "collectionGroup": "projects",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "isSubmitted", "order": "ASCENDING" },
        { "fieldPath": "submittedAt", "order": "DESCENDING" }
      ]
    },
    {
      "collectionGroup": "projects",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "classroomId", "order": "ASCENDING" },
        { "fieldPath": "isSubmitted", "order": "ASCENDING" },
        { "fieldPath": "submittedAt", "order": "DESCENDING" }
      ]
    },
    {
      "collectionGroup": "projects",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "userId", "order": "ASCENDING" },
        { "fieldPath": "isSubmitted", "order": "ASCENDING" },
        { "fieldPath": "submittedAt", "order": "DESCENDING" }
      ]
    },
    {
      "collectionGroup": "projects",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "userId", "order": "ASCENDING" },
        { "fieldPath": "updatedAt", "order": "DESCENDING" }
      ]
    }
  ],
  "fieldOverrides": []
}
```

注記:

- 実際のコンソールエラーリンクで要求定義を再確認し、差分があれば更新する。

---

## 24. 欠損ユーザー修復手順（運用必須）

### 24.1 検出対象

- Authに存在するが Firestore `users/{uid}` がない
- Firestoreに存在するが Auth にユーザーがいない
- `role` 欠損、`classroomId` 不整合（teacher/studentでnull）

### 24.2 修復方針

1. Authのみ存在:
- `users/{uid}` を最小項目で補完（`role`, `displayName`, `classroomId`, `createdAt`）
- 一時ロールは `student` に固定せず、管理者確認待ち状態を明示

2. Firestoreのみ存在:
- 管理者確認後に不要レコードを削除、またはAuth再作成

3. 属性欠損:
- 管理画面から補完できない場合は管理者用補正スクリプトで修復

### 24.3 予防策

- `createUser/createUsers` で Firestore保存失敗時に `auth.deleteUser` を補償実行
- 週次監査で欠損件数をレポート

---

## 25. 受け入れ判定表（最終）

| 観点 | 判定条件 | 合否 |
|------|----------|------|
| 正確性 | ロール別閲覧範囲が仕様どおり |  |
| 安全性 | 他教室/他人作品が漏えいしない |  |
| 可用性 | `users/classrooms` 補助取得失敗時も一覧表示維持 |  |
| 性能 | 初期表示が許容時間内（閾値定義済み） |  |
| 運用性 | 監視イベント/アラートが稼働 |  |
| 回帰 | 保存/読込/提出/管理/設定に障害なし |  |
| 復旧性 | ロールバック手順で即時復旧可能 |  |

判定ルール:

- 1項目でも未達なら本番反映不可。

---

## 26. 再検証（別側面・第2版）

### 26.1 競合状態（Race Condition）対策

懸念:

- `auth.onAuthStateChanged` の連続発火時に、先に開始した `loadData` が後から完了し、新しいユーザー状態を上書きする可能性がある。

対策:

- `loadRequestId` を導入し、描画直前に最新リクエストか検証する。
- ユーザー切替時は以前の読み込み結果を破棄する。

受け入れ条件:

- ログアウト直後の再ログインで、前ユーザーの作品が一瞬でも表示されない。

### 26.2 認証IDの正規化（Case Normalization）

確認事項:

- `gallery.html` と `firebase-integration.js` は `userId.toLowerCase()` でメール化している。
- `admin-mobile.html` は `toLowerCase()` を使っていない。

リスク:

- ユーザー入力の大文字混在でログイン体験が画面ごとに不一致になる。

対策:

- 全ログイン画面で `userId.toLowerCase()` を統一適用する。
- 既存アカウント命名規約（小文字）を明文化する。

### 26.3 機能仕様ギャップ（`Scratchで開く`）

確認事項:

- ギャラリーのボタンは `index.html?loadProject=...` へ遷移するが、受け側の明示実装を現行コードで確認できない。

リスク:

- ギャラリー表示が改善しても、実行導線で「開けない」障害認知が残る。

対策:

- 本件と別タスクで URLパラメータロード対応を検証・実装する。
- 未対応期間はUI文言を「読み込みはクラウドダイアログから」に変更する暫定策を検討する。

### 26.4 互換性・初期化安全性

確認事項:

- 各HTMLページで `firebase.initializeApp` を個別実行している。
- ページ単位では問題ないが、将来的なスクリプト統合時は二重初期化リスクがある。

予防策:

- 共通初期化関数化する場合は `if (!firebase.apps.length)` ガードを標準化する。

### 26.5 実機検証不足の明示

現時点:

- 本レビューは静的検証であり、実Firebase環境接続での実測は未実施。

必須アクション:

- リリース前に admin/teacher/student 各アカウントで実機E2Eを実施し、証跡（日時、アカウント、結果）を残す。

証跡テンプレート:

- 実施日:
- 実施者:
- ロール:
- アカウント:
- 手順:
- 期待結果:
- 実結果:
- 判定:

---

## 27. 再検証（別側面・第3版）

### 27.1 XSS/表示安全性

確認結果:

- `gallery.html` は主要表示で `escapeHtml` を使っており、表示面は比較的安全。
- 一方 `admin.js` の `updateFilters()` は `displayName` / `classroom.name` を `innerHTML` に直接埋め込んでいる。

リスク:

- 管理画面で保存された表示名にHTML片が含まれると、管理画面UIの選択肢描画に影響する可能性がある。

対策:

- `admin.js` の option生成に `escapeHtml` を適用（本件と別PRでも可）。
- ユーザー名入力バリデーションに危険文字の制限を追加検討。

### 27.2 ID正規化の不統一（再確認）

確認結果:

- `gallery.html` / `firebase-integration.js` は `userId.toLowerCase()` を適用。
- `admin-mobile.html` のログインは小文字化していない。
- Cloud Functions `createUser` は `userId` を小文字化せずAuthユーザー作成する。

リスク:

- 画面ごとにログイン可否が揺れる、または同一人物の入力体験が不一致になる。

対策（必須）:

- ユーザー作成時点で `userId = userId.toLowerCase().trim()` をサーバー側で強制。
- 全ログイン画面で同一正規化ルールを適用。

### 27.3 レガシーデータ互換（submittedAt欠損）

確認結果:

- クエリは `isSubmitted == true` と `orderBy(submittedAt)` の組み合わせを前提としている。
- 旧データに `submittedAt` 欠損があると表示漏れや並び不整合が発生しうる。

対策:

- リリース前に `isSubmitted == true && submittedAt == null/missing` の件数監査を実施。
- 必要なら補正バッチで `submittedAt` を補完する。

### 27.4 教師データ欠損時の境界条件

確認結果:

- teacher の `classroomId` が null の場合、想定クエリが成立しない。

対策:

- teacherログイン時に `classroomId` 未設定ならエラー表示し、ギャラリー読込を停止。
- 管理者に修復を促す導線を表示する。

### 27.5 副作用判定（第3版）

- ギャラリー改修単体の副作用: 低
- 追加で必要な横断修正（推奨）:
  - `admin.js` のoption描画エスケープ
  - `admin-mobile.html` ログインID正規化
  - Cloud Functions `createUser` のID正規化

これらは本件の主問題解決に必須ではないが、再発抑止と運用品質の観点で同時に実施する価値が高い。

---

## 28. 再検証（別側面・第4版）

### 28.1 デプロイ設定ドリフト

確認結果:

- `firebase.json` に `firestore.rules` は登録済みだが、`firestore.indexes` の参照が未設定。
- そのためインデックスを追加しても、デプロイ運用に組み込まれない限り環境差分が再発する。

対策:

- `firebase.json` に `"indexes": "firestore.indexes.json"` を追加する。
- リリース手順に `firestore:indexes` デプロイを必須化する。

### 28.2 教室変更ドリフト（データの時間整合性）

確認結果:

- プロジェクト作成時の `classroomId` は作成時点の `userData.classroomId` を保存する。
- 管理画面でユーザーの `classroomId` を後から変更しても、既存プロジェクトの `classroomId` は更新されない。

影響:

- teacherの閲覧制御は `projects.classroomId` 基準のため、ユーザー移動後に「過去作品の所属教室」が残存する。
- 仕様次第で情報漏えいまたは閲覧欠落に見える可能性がある。

対策:

- 仕様を明確化:
  - A) 履歴保持（作成時教室を保持）
  - B) 現在所属追従（移動時に既存プロジェクトを一括更新）
- Aを採るならUIで「提出時教室」表示を明示。
- Bを採るなら管理操作時にプロジェクト再所属バッチを実行。

### 28.3 参照整合性（孤児データ）

確認結果:

- 教室削除時に `users` / `projects` の参照整合を取る処理がない。
- 教室を削除すると `classroomId` 参照が孤児化し、ギャラリーフィルタや表示名補完で欠落が起きる。

対策:

- 教室削除前に依存件数を厳密チェックし、0件でのみ削除許可。
- もしくは削除時に関連 `users/projects` を既定教室へ移管するトランザクションを導入。

### 28.4 地域設定/関数呼び出し整合

確認結果:

- Cloud Functions は `asia-northeast1` で統一され、`admin.js` 側も同リージョン指定で呼び出している。
- リージョン不一致による失敗リスクは現時点で低い。

### 28.5 第4版総合判定

- 本プランは実装可能。
- ただし本番品質を担保するには、以下を追加Go条件にする。
  - `firestore.indexes` のデプロイ設定をコード化
  - 教室変更時のプロジェクト所属仕様を確定
  - 教室削除の参照整合ルールを明文化

---

## 29. 意思決定ログ（未確定項目の固定テンプレート）

以下3項目は実装前に必ず決裁し、ここへ記録する。

1. 教室変更時の過去プロジェクト所属
- 選択肢A: 提出時教室を保持
- 選択肢B: 現在所属へ追従
- 決定:
- 決定日:
- 決定者:

2. 教室削除時の扱い
- 選択肢A: 依存データが1件でもあれば削除禁止
- 選択肢B: 既定教室へ移管して削除
- 決定:
- 決定日:
- 決定者:

3. studentギャラリー閲覧範囲（再確認）
- 選択肢A: 自分の提出作品のみ
- 選択肢B: 同教室提出作品まで許可
- 決定:
- 決定日:
- 決定者:

---

## 30. データ移行/補正計画（必要時）

### 30.1 `submittedAt` 欠損補正

- 対象: `isSubmitted == true` かつ `submittedAt == null/missing`
- 方針:
  - `updatedAt` があればそれを代替値に採用
  - なければ運用日付で補完し、監査ログに記録

### 30.2 教室所属補正（方針B採用時のみ）

- 対象: ユーザー教室変更後の既存 `projects.classroomId`
- 方針:
  - ユーザー単位のバッチ更新
  - 更新件数・対象IDを監査ログに保存

### 30.3 孤児参照補正

- 対象:
  - `projects.classroomId` が実在しない
  - `users.classroomId` が実在しない
- 方針:
  - 削除ではなく一時隔離教室へ移管して可視化

---

## 31. デプロイ手順（固定版）

1. 事前確認
- `firebase.json` に `firestore.rules` と `firestore.indexes` の両方が定義されていること
- `firestore.indexes.json` が最新であること

2. 反映順序
- `firebase deploy --only firestore:indexes`
- インデックス作成完了確認
- `firebase deploy --only firestore:rules`
- 静的ファイル反映（`gallery.html` 等）

3. 反映後検証
- admin/teacher/student で各1ケースのスモークテスト
- `permission-denied` / `failed-precondition` の監視確認

---

## 32. 最終チェックリスト（実装直前）

- [x] Section 29 の3意思決定が記録済み
- [ ] 欠損ユーザー件数を把握済み
- [x] `firestore.indexes` 設定が `firebase.json` に反映済み
- [ ] E2E担当者と実施日時が確定済み
- [ ] ロールバック担当者と手順が合意済み

---

## 33. 再検証（別側面・第5版）

### 33.1 デプロイ安全性（Config Drift）

確認結果:

- `firebase.json` は現状 `firestore.rules` のみ定義されている。
- インデックスをコード管理する場合、`firestore.indexes` の追加を忘れると、環境差分が残る。

対策:

- `firebase.json` 変更を本件PRの必須差分に含める。
- CI/手動チェックで `firestore.indexes.json` の存在と参照整合を確認する。

### 33.2 環境再現性（Project/Region）

確認結果:

- `.firebaserc` のデフォルトプロジェクトは固定されている。
- Cloud Functions は `asia-northeast1` に統一され、`admin.js` 側呼び出しも同リージョン指定で整合している。

評価:

- リージョン不一致による障害リスクは低い。
- ただし別環境へデプロイする運用では `.firebaserc` 切替手順を明記しないと誤反映リスクが残る。

### 33.3 初期化競合（Firebase App Init）

確認結果:

- 各ページは単独で `firebase.initializeApp` を実行しており、現行遷移モデルでは衝突しない。
- `admin-mobile.html` は `admin.js` を読み込んでFirebase初期化を委譲している。

対策:

- 将来のスクリプト統合時に備えて `firebase.apps.length` ガード方針を維持する。

### 33.4 監査可能性（Evidence）

確認結果:

- 問題の再現・解消を時系列で追える監査フォーマットは計画済みだが、実運用への紐付けが未確定。

対策:

- リリースチケットに「実施証跡」を必須添付項目として追加する。
- 最低限、ロール別3ケースのスクリーンショット/ログ要約を残す。

### 33.5 第5版総合判定

- 本プランは継続して実装可能。
- 本番前の最終Go条件に、以下を追加する。
  - `firebase.json` の indexes 参照追加がマージされていること
  - デプロイ対象プロジェクト確認手順が文書化されていること
  - ロール別実機証跡が添付されていること

---

## 34. 再検証（別側面・第6版）

### 34.1 セッション終了時のデータ残留

確認結果:

- `gallery.html` はログアウト時に `showLogin()` のみ実行し、`projects/users/classrooms` を明示クリアしない。
- 画面オーバーレイで隠れるが、DOM上には直前ユーザーのデータが残存する。

リスク:

- 共有端末での覗き見・UI誤認（低〜中）
- 再ログイン時のデータ混在（`didLoad` 併発時）

対策:

- `onAuthStateChanged(user == null)` で状態を全クリアし、`projectGrid` と `emptyState` を初期状態へ戻す。

### 34.2 失敗後の再試行不能

確認結果:

- 初回 `loadData()` 失敗でも `didLoad=true` になり、同一セッションで再試行されない。

対策:

- `didLoad` 廃止または成功時のみ確定に変更。
- 手動再試行ボタンを追加し、`loadData` を再実行可能にする。

### 34.3 ポップアップ依存導線

確認結果:

- `window.open` を多用（ギャラリー起動、Scratchで開く）。
- ブラウザや端末設定でポップアップブロックされると機能が失敗する。

対策:

- 失敗時フォールバック（同一タブ遷移 or 明示メッセージ）を実装。
- リリーステストに「ポップアップブロックON環境」を追加。

### 34.4 UI状態遷移の安全性

確認結果:

- ローディング表示を `innerHTML` で上書きしており、正常復帰時に表示要素構造が想定外になる可能性がある。

対策:

- ローディングは `textContent` + 専用エラー領域で管理し、DOM構造を壊さない。

### 34.5 第6版総合判定

- 本プランは引き続き実装可能。
- ただし UX/運用品質を担保するため、次を追加Go条件にする。
  - ログアウト時のデータクリアが実装されている
  - 再試行導線がある（自動または手動）
  - ポップアップブロック環境でのフォールバック挙動が検証済み

---

## 35. 再検証（別側面・第7版）

### 35.1 型の揺らぎ（Timestamp前提）

確認結果:

- `gallery.html` は `submittedAt.toDate()` を前提に描画・ソートしている。
- 旧データや補正データで `submittedAt` が Date/string/null の混在になると、実行時例外または並び崩れの原因になる。

対策:

- 変換ヘルパーを導入し、`Timestamp|Date|string|null` を安全に正規化してから利用する。
- ソート比較は `number`（epoch ms）に統一する。

### 35.2 ログアウト時のDOM残留（再確認）

確認結果:

- ログアウト時に配列/辞書キャッシュと `projectGrid` が明示クリアされない。
- オーバーレイで隠しているだけで、同一タブ内にデータが残る。

対策:

- `clearGalleryState()` を実装し、ログアウト時に必ず実行する。
- クリア対象: `projects/users/classrooms`, `projectGrid.innerHTML`, `emptyState`, `loading`。

### 35.3 `innerHTML` でのエラー表示

確認結果:

- 読み込みエラー時に `loading.innerHTML = '...'+error.message` を直接実行している。

リスク:

- 通常は低リスクだが、外部由来文字列をHTMLとして挿入する実装パターンは避けるべき。

対策:

- エラー表示は専用要素へ `textContent` で出力する。

### 35.4 ポップアップ制約（再確認）

確認結果:

- `window.open` 依存導線はブラウザ設定で失敗する可能性がある。

対策:

- `window.open` 失敗時の戻り値チェックを行い、失敗時は同一タブ遷移へフォールバックする。

### 35.5 第7版総合判定

- 本プランは継続して実装可能。
- 追加Go条件:
  - `submittedAt` 型揺らぎを吸収する変換ヘルパーを実装済み
  - ログアウト時の状態クリアが実装済み
  - エラー表示で `innerHTML + error.message` を廃止済み

---

## 36. 失敗モード対処表（実装時の即応ガイド）

| 失敗モード | 想定原因 | 対処 | ブロッカー判定 |
|------|------|------|------|
| `permission-denied`（teacher/student） | ロール不一致クエリ | ロール別クエリへ切替、`users/{uid}` 存在確認 | Yes |
| `failed-precondition` | 複合インデックス不足 | `firestore.indexes.json` 追加・デプロイ | Yes |
| 初回失敗後に復帰しない | `didLoad` 固定化 | 成功時のみ確定、再試行導線追加 | Yes |
| ログアウト後に前データが見える | 状態/DOM未クリア | `clearGalleryState()` 実装 | Yes |
| 並び順が不安定 | `submittedAt` 型揺らぎ | 正規化ヘルパーで epoch 比較 | No（修正必須） |
| ギャラリーが開かない | ポップアップブロック | `window.open` 失敗時フォールバック | No（UX重大） |

---

## 37. 実装依存順（順序固定）

1. クエリ設計と状態管理の改修（`gallery.html`）
2. エラー/再試行/ログアウトクリアの実装
3. 型正規化ヘルパー導入（`submittedAt`）
4. インデックス定義追加（`firestore.indexes.json` + `firebase.json`）
5. 導線のフォールバック（`window.open` 失敗時）
6. ロール別E2Eと回帰試験

順序制約:

- 4を先にやらない場合、1-3の検証で偽陽性エラーが出る可能性がある。
- 2未完了で6を実施しても、セッション系不具合の判定が不安定になる。

---

## 38. 再検証（別側面・第8版）

### 38.1 データライフサイクルの副作用

確認結果:

- ユーザー削除時、対象ユーザーのプロジェクトは削除されずに残る（仕様上警告のみ）。
- ギャラリーは `users[p.userId]` 参照欠損時に `不明` 表示で継続するため、表示自体は成立する。

評価:

- 機能停止はしないが、運用上のデータ品質低下（作成者不明）が蓄積する。

対策:

- 運用仕様として「ユーザー削除後プロジェクトの扱い（保持/移管/削除）」を明文化する。

### 38.2 教室削除時の参照不整合

確認結果:

- 教室削除前チェックは生徒数のみで、教室に紐づくプロジェクト数はチェックしていない。
- 教室削除後、`projects.classroomId` が孤児化する可能性がある。

評価:

- teacher の教室フィルタやギャラリーの教室表示で欠落・不整合が起こりうる。

対策:

- 教室削除前に `projects` 依存件数も0件必須にするか、移管処理を追加する。

### 38.3 互換性と回帰判定

確認結果:

- これらはギャラリー改修の主処理とは独立しているため、プラン実装可能性を直接阻害しない。
- ただし本番運用品質と長期整合性に影響するため、同時に管理すべき。

### 38.4 第8版総合判定

- 本プランは継続して実装可能。
- 追加Go条件:
  - 教室削除時のプロジェクト依存チェック方針が決定済み
  - ユーザー削除後プロジェクトの運用方針が文書化済み

---

## 39. 再検証（別側面・第9版）

### 39.1 ログイン実装の分岐不一致

確認結果:

- `gallery.html` / `firebase-integration.js` は `userId.toLowerCase()` でメール化する。
- `admin-mobile.html` のログインは小文字化せず `userId + '@laughtale.local'` を使用する。

影響:

- 端末や画面によりログイン成功/失敗の体験が不一致になる。

対策:

- 全ログイン入口で同一のID正規化ルールに統一する。

### 39.2 古いブラウザ互換リスク

確認結果:

- `admin-mobile.html` に optional chaining（`?.`）が含まれる。
- 学校端末で古いブラウザが残っている場合、構文エラーで管理画面が起動しない可能性がある。

対策:

- 対象ブラウザを明文化し、必要なら `?.` を非使用構文へ置換する。

### 39.3 監視の偏り

確認結果:

- 現実装は `console.error` と `alert` が中心で、横断的な失敗集計がない。

対策:

- 失敗コード（`permission-denied` / `failed-precondition`）を収集する軽量ログを導入する。

### 39.4 第9版総合判定

- 本プランは継続して実装可能。
- 追加Go条件:
  - ログインID正規化の統一方針が適用済み
  - 対象ブラウザ要件が明文化済み

---

## 40. 再検証（別側面・第10版）

### 40.1 ルールの改ざん耐性（Projects Update）

確認結果:

- `firestore.rules` の `projects` 更新許可は「誰が更新できるか」の判定のみで、更新可能フィールドの制限がない。
- 所有者（student含む）は `classroomId` や `userId` を含む任意フィールドを更新可能な構造になっている。

影響:

- 教室境界の整合性が崩れる可能性がある。
- ギャラリーのロール別表示条件（`classroomId`）が改ざんデータで揺らぐ可能性がある。

対策:

- `projects` 更新ルールに不変条件を追加:
  - `request.resource.data.userId == resource.data.userId`
  - `request.resource.data.classroomId == resource.data.classroomId`（仕様に応じて）
- 仕様上許可する更新項目（`name`, `description`, `thumbnailUrl`, `storageUrl`, `isSubmitted`, `submittedAt`, `updatedAt`, `size` など）を明示し、許可リスト方式で制御する。

### 40.2 テスト自動化不足

確認結果:

- ギャラリー/管理画面/設定画面の Web 実装に対する自動テスト（ユニット/E2E）の整備が確認できない。
- 現状は手動検証に依存しており、回帰検知の再現性が低い。

対策:

- 最低限の自動検証セットを追加:
  - ロール別クエリ構築ロジック（純関数化してテスト）
  - `submittedAt` 正規化ヘルパー
  - ログアウト時クリア処理
- 可能なら Playwright 等で smoke E2E を追加（ログイン→ギャラリー表示→ログアウト）。

### 40.3 第10版総合判定

- 本プランは継続して実装可能。
- ただし本番前の追加Go条件として以下を強く推奨する。
  - `projects` 更新ルールの不変条件が実装済み
  - 最低限の自動テスト（ロジックテスト）が実装済み

---

## 41. 再検証（別側面・第11版）

### 41.1 Storageルール未管理

確認結果:

- リポジトリ上で `storage.rules` が確認できず、`firebase.json` でも Storage ルール参照が未定義。
- 一方、アプリ本体は `projects/` `thumbnails/` `recordings/` へ継続的に読み書きしている。

影響:

- 環境ごとのStorage権限差分が再発しやすい。
- ギャラリー不具合調査時に、Storage権限問題が混在して原因切り分けを難化させる。

対策:

- `storage.rules` をコード管理下に置き、`firebase.json` に参照を追加する。
- 少なくとも `projects/{uid}` と `thumbnails/{uid}` の所有者制約を明文化する。

### 41.2 UI初期化の重複リスク

確認結果:

- `firebase-integration.js` は DOM状態に応じて `init()` を起動し、内部で `onAuthStateChanged` と `document.click` リスナーを登録する。
- 現行では通常1回起動だが、将来的な再初期化経路が入るとリスナー多重登録リスクがある。

対策:

- `init()` に idempotent ガード（`this.initialized`）を追加する。
- グローバル click リスナーは一度だけ登録する。

### 41.3 監視の一元化不足（Web + Functions）

確認結果:

- Web側は `console` と `alert` 中心、Functions側は `console.error` 中心で、同一障害を横断で追跡しにくい。

対策:

- エラーコードとコンテキスト（role, uid, queryType）を共通フォーマット化する。
- リリース後観測で Web/Functions の両ログを紐づける運用手順を追加する。

### 41.4 第11版総合判定

- 本プランは継続して実装可能。
- 追加Go条件:
  - Storageルールのコード管理方針が確定済み
  - `firebase-integration.js` 初期化の再入防止が実装済み

---

## 42. 再検証（別側面・第12版）

### 42.1 オフライン/不安定回線耐性

確認結果:

- Web実装に `retry/backoff` や `navigator.onLine` 連携は確認できない。
- 通信失敗時は即時エラー表示で終了し、自動再試行しない。

影響:

- 学校ネットワークの瞬断で体感失敗率が上がる。

対策:

- `loadData()` に軽量リトライ（例: 2回）を追加。
- 手動再試行ボタンを標準実装する。

### 42.2 同時更新（複数タブ）整合性

確認結果:

- `submitProject/unsubmitProject` は単純 `update` で、排他や競合解決がない。
- 2タブ同時操作で最終書き込み勝ちとなり、利用者の意図とズレる可能性がある。

対策:

- 競合が許容できるか仕様で明示する（Last Write Wins）。
- 必要なら `updatedAt` 比較を伴う条件付き更新へ拡張する。

### 42.3 認証永続化の暗黙設定

確認結果:

- `auth.setPersistence(...)` 設定がなく、SDKデフォルト挙動に依存している。

影響:

- 端末/ブラウザ条件でセッション保持の体感差が出る可能性がある。

対策:

- 期待挙動（タブ閉じ後も維持 or セッション限定）を仕様化し、必要なら明示設定する。

### 42.4 第12版総合判定

- 本プランは継続して実装可能。
- 追加Go条件:
  - 再試行導線（自動または手動）が実装済み
  - 同時更新時の整合ポリシーが仕様化済み
  - 認証永続化方針が明文化済み

---

## 43. 再検証（別側面・第13版）

### 43.1 アクセシビリティ最小要件

確認結果:

- ラベル付き入力は概ね実装済みだが、モーダルや動的更新領域に `aria-live` 等の通知設計が不足している。

対策:

- ログインエラー/読み込みエラー領域に `aria-live=\"polite\"` を付与。
- キーボードフォーカス遷移（モーダル開閉時）を明示管理する。

### 43.2 内部エラー露出

確認結果:

- 一部画面で `error.message` をそのままユーザー表示している。

影響:

- 内部実装情報が露出し、利用者メッセージ品質も不安定になる。

対策:

- 表示用はコード別の定型文に統一し、詳細は `console`/監視ログへ分離する。

### 43.3 ログイン実装の重複による再発リスク

確認結果:

- `gallery.html`、`firebase-integration.js`、`admin-mobile.html` でログイン処理が重複し、正規化やエラー文言に差分がある。

対策:

- 共通ログインユーティリティを導入し、ID正規化・エラー変換を一元化する。

### 43.4 第13版総合判定

- 本プランは継続して実装可能。
- 追加Go条件:
  - ユーザー向けエラー文言の統一方針が適用済み
  - ログイン処理の共通化方針が決定済み

---

## 44. 再検証（別側面・第14版）

### 44.1 日時フォーマット/表示一貫性

確認結果:

- `gallery.html` と `admin.js` と `firebase-integration.js` で日時フォーマット実装が分散している。
- 表示書式が微妙に異なり、画面間で体験差が生じる。

対策:

- 共通 `formatDate` ユーティリティを定義し、全画面で統一する。

### 44.2 UI状態復元（発表モード/プレビュー）

確認結果:

- 発表モードとプレビュー状態は画面遷移や再認証時の明示リセットが弱い。

対策:

- ログアウト時や再読み込み時に `presentation-mode` と `previewModal` を明示初期化する。

### 44.3 メッセージ一貫性

確認結果:

- 類似エラーでも画面ごとに文言がばらつく（例: ログイン失敗系）。

対策:

- エラーコード -> 文言の共通マップを1箇所に集約し、全画面で参照する。

### 44.4 第14版総合判定

- 本プランは継続して実装可能。
- 追加Go条件:
  - 日時フォーマットの共通化方針が適用済み
  - ログアウト時UI状態リセットが実装済み

---

## 45. 再検証（別側面・第15版）

### 45.1 Firestoreルール評価コスト

確認結果:

- `firestore.rules` では `getUserData()` が複数箇所で繰り返し参照される構造。
- 高頻度クエリ時に評価コストが増え、デバッグ時の切り分けも難しくなる。

対策:

- ヘルパー呼び出しの重複を最小化し、条件式を整理する（可読性向上）。
- ルール変更時は emulator で role別クエリを必ず再検証する。

### 45.2 イベントリスナー増殖リスク

確認結果:

- `firebase-integration.js` は `init()` 内で `document.addEventListener('click', ...)` を登録する。
- 将来再初期化経路が入ると、リスナー多重登録で予期せぬ挙動が出る可能性がある。

対策:

- `init()` を idempotent にし、グローバルリスナー登録済みフラグを設ける。

### 45.3 大量データ描画負荷

確認結果:

- `gallery.html` / `admin.js` は `innerHTML = map(...).join('')` による一括描画中心。
- 件数増加時に描画遅延・メモリ負荷が増える。

対策:

- 初期表示件数制限 + ページング/段階読み込みを導入。
- 可能なら仮想化または chunk 描画を検討。

### 45.4 第15版総合判定

- 本プランは継続して実装可能。
- 追加Go条件:
  - `firebase-integration.js` 初期化の冪等性が担保されている
  - 一覧表示に件数上限/ページング方針が適用されている

---

## 46. 再検証（別側面・第16版）

### 46.1 モバイル管理画面の隠れた結合

確認結果:

- `admin-mobile.html` は `admin.js` のメソッドを多数オーバーライドしており、内部実装に強く依存している。
- `admin.js` のメソッド名/引数/DOM要素ID変更が入ると、モバイル側だけ破綻するリスクが高い。

対策:

- 本件改修で `admin.js` に手を入れる場合は、`admin-mobile.html` のオーバーライド整合を同時確認する。
- 可能なら共通API層を定義し、オーバーライド依存を減らす。

### 46.2 設定値の分散管理

確認結果:

- Firebase設定が複数ファイルに重複定義されている（`gallery.html`, `admin.js`, `settings.html`, `firebase-integration.js`）。

影響:

- 将来の設定変更時に不一致が発生しやすい。

対策:

- 共通設定モジュールへ集約し、単一ソース化する。

### 46.3 第16版総合判定

- 本プランは継続して実装可能。
- 追加Go条件:
  - `admin.js` 変更時の `admin-mobile.html` 整合確認手順が定義済み
  - Firebase設定の単一ソース化方針が決定済み

---

## 47. 再検証（別側面・第17版）

### 47.1 機能境界の混線（作品ギャラリー/動画ギャラリー）

確認結果:

- 本件対象の `gallery.html`（提出作品一覧）とは別に、`functions/index.js` には公開動画ギャラリーAPI（`getPublicVideos`）が存在する。
- どちらも「ギャラリー」文脈のため、運用時に障害切り分けが混線しやすい。

対策:

- ドキュメント上で「作品ギャラリー」と「動画ギャラリー」を明確に分離記載する。
- 監視イベント名も機能別に分ける（例: `project_gallery_*`, `video_gallery_*`）。

### 47.2 公開設定のセキュリティ運用

確認結果:

- 動画機能では `file.makePublic()` によりStorageオブジェクトを公開化している。
- 本件の作品ギャラリーとは別機能だが、同一プロジェクト内の公開ポリシーとしては要監査領域。

対策:

- 公開ファイルの対象パス・用途・保持期間を運用ルールで明文化する。
- 作品ギャラリー側のStorageアクセス権と混同しないよう、ルール文書を分離する。

### 47.3 設定重複による変更漏れ（再確認）

確認結果:

- Firebase設定が複数ファイルに重複しており、将来差し替え時の漏れリスクが継続。

対策:

- 設定共通化を本件後の優先リファクタ候補として確定する。

### 47.4 第17版総合判定

- 本プランは継続して実装可能。
- 追加Go条件:
  - 作品ギャラリーと動画ギャラリーの運用境界が文書化済み
  - 公開ファイル運用ルールが明文化済み

---

## 48. 再検証（別側面・第18版）

### 48.1 配信設定の再現性（Hosting構成の明示不足）

確認結果:

- `firebase.json` には `firestore.rules` と `functions` は定義されているが、`hosting` セクションが存在しない。
- リポジトリ上の設定だけでは、`gallery.html` を含む静的ファイル配信設定（`public`, `headers`, `rewrites`）を再現できない状態。

影響:

- ローカル修正が本番配信に反映されない、または環境ごとに配信設定がズレるリスクがある。
- 「コードは修正済みだが画面は直らない」事象が、アプリ不具合と配信不整合で混在する。

対策:

- `firebase.json` に `hosting` 設定を明記し、静的配信もコード管理対象に含める。
- デプロイチェックリストに「対象HTML/JSの配信反映確認（ハッシュ/更新時刻）」を追加する。

### 48.2 段階的失敗耐性（all-or-nothing初期化）

確認結果:

- `gallery.html` の `loadData()` は `classrooms` -> `users` -> `projects` を直列取得し、任意1つの失敗で全体失敗扱いになる。
- 初期化成功/失敗の粒度が粗く、切り分け情報がUIに残らない（`error.message` 表示のみ）。

影響:

- 一部データだけ失敗した場合でも画面全体が停止し、利用者は原因を判別できない。
- ロール制約による失敗と一時通信失敗が同一見え方になり、運用判断が遅れる。

対策:

- 取得単位ごとに結果を分離し、`projects` 成功時は最低限一覧表示を継続する方針へ変更する。
- `permission-denied` / `failed-precondition` / `unavailable` を分類してUIと監視ログに分離出力する。

### 48.3 認証イベント順序と画面残留の整合

確認結果:

- `auth.onAuthStateChanged` の未ログイン分岐は `showLogin()` のみで、表示中データの明示クリアは別処理依存。
- オーバーレイ表示方式のため、状態クリア漏れがあるとログアウト後も旧データが背面に残留し得る。

影響:

- セキュリティ事故でなくても、運用上は「ログアウトしたのに前データが見える」誤認を招く。
- QAで再現率が低い視覚的残留バグとして長期化しやすい。

対策:

- 未ログイン遷移時に `clearGalleryState()` を必須実行し、`projects/users/classrooms` とDOMを同時初期化する。
- `onAuthStateChanged` の状態遷移を `loading -> ready -> signedOut` の有限状態として管理し、UI更新点を一本化する。

### 48.4 第18版総合判定

- 本プランは引き続き実装可能であり、設計妥当性は維持される。
- 追加Go条件:
  - `hosting` 設定のコード管理方針が確定している
  - 初期化処理が部分失敗耐性を持つ設計に更新されている
  - ログアウト時の状態クリアが認証イベント経路で必ず実行される

---

## 49. 再検証（別側面・第19版）

### 49.1 一括ユーザー作成の入力検証ギャップ

確認結果:

- `functions/index.js` の `createUsers` は管理者限定だが、`createUser` で実施している厳密検証（`role` の許可値、`password` 長、`classroomId` 必須条件）を同等には実施していない。
- 不正値を含むユーザー文書が作成されると、`gallery.html` / `admin.js` / `firestore.rules` が期待する `role/classroomId` 契約から外れる可能性がある。

影響:

- ロール判定分岐の欠落、表示崩れ、権限評価の不一致を誘発する。
- 本件ギャラリー改修後でも、データ品質の悪化により再発原因が混入する。

対策:

- `createUsers` に `createUser` と同等のバリデーションを追加する。
- バッチ結果に「拒否理由コード」を含め、運用で不正データ投入を早期検知する。

### 49.2 動画メタデータ保存の所有者検証不足

確認結果:

- `saveVideoMetadata` は引数 `filePath` の存在確認は行うが、`recordings/{context.auth.uid}/` 配下かどうかを検証していない。
- 関数は管理権限で `file.makePublic()` を実行するため、パス推測が成立すると他ユーザーのファイル公開に繋がる設計余地がある。

影響:

- 作品ギャラリーとは別機能だが、同一プロジェクトの公開ポリシーとして重大な越権リスク。
- セキュリティインシデント時に「ギャラリー障害」と混線し、復旧判断を遅らせる。

対策:

- `filePath.startsWith(\`recordings/${context.auth.uid}/\`)` を必須条件にする。
- 不一致時は `permission-denied` を返し、監査ログへ記録する。

### 49.3 公開動画APIのクエリ上限未設定

確認結果:

- `getPublicVideos` の `limit` は `parseInt(req.query.limit) || 20` のみで、上限値を設けていない。
- 大きな `limit` 指定で Firestore read が増大し、レイテンシ/課金/可用性に影響し得る。

影響:

- 本件ギャラリー改修と同時期に負荷問題が表面化すると、原因切り分けが困難になる。

対策:

- `limit` を `1..50` などに clamp する。
- 監視に `query_limit` を追加し、異常値を検知する。

### 49.4 `projects` 更新ルール強化時の互換性条件

確認結果:

- 既存クライアントの更新操作は `saveProject` / `renameProject` / `submitProject` / `unsubmitProject` で更新フィールド集合が異なる。
- ルールを「更新可能フィールド限定」に強化する場合、これら全経路を許可集合に含めないと正常機能まで遮断する。

影響:

- セキュリティ強化の副作用として、保存・提出・名称変更が部分的に失敗する。

対策:

- 更新許可フィールドをユースケース別に明文化し、ルールテストで全経路を回帰確認する。
- 最低限 `storageUrl, thumbnailUrl, size, name, description, isSubmitted, submittedAt, updatedAt` の扱いをケース別に定義する。

### 49.5 第19版総合判定

- 本プランは引き続き正しく実装可能。
- ただし、他機能への負の影響を避けるため次を追加Go条件とする。
  - `createUsers` の入力検証が `createUser` と同等水準に統一済み
  - `saveVideoMetadata` の所有者パス検証が実装済み
  - `getPublicVideos` に取得件数上限が導入済み
  - `projects` 更新ルール強化時の互換テストが全更新経路で合格済み

---

## 50. 再検証（別側面・第20版）

### 50.1 `deleteUser` の削除完了定義が不十分

確認結果:

- `functions/index.js` の `deleteUser` は Authユーザーと `users/{uid}` は削除するが、`projects` は削除しない設計。
- 戻り値で `projectCount` を返して警告は出すものの、データ整合は運用依存のまま残る。

影響:

- ユーザー削除後に「作成者不明プロジェクト」が残留し、ギャラリーや管理画面で欠落表示が発生する。
- 後続改修で `users` 参照を厳密化した場合に、既存データが障害トリガになる。

対策:

- 削除ポリシーを明文化（同時削除 / 移管 / 保持）し、`deleteUser` の処理契約として固定する。
- 最低限、残存プロジェクトID一覧を監査ログに残し、後処理ジョブで確実に解決する。

### 50.2 教室削除の依存チェック不足（再確認の深掘り）

確認結果:

- `admin.js` の `confirmDeleteClassroom` は生徒数のみ確認し、プロジェクト件数・講師割当の整合をチェックしない。
- `deleteClassroom` は単純削除で、関連ドキュメントの再割当や補償処理を実施しない。

影響:

- `projects.classroomId` / `users.classroomId` の孤児化が継続し、権限判定や一覧表示で不整合が蓄積する。
- 講師の `classroomId` が孤児化すると、運用画面上の教室表示が空になり、障害調査を難化させる。

対策:

- 教室削除前に `students/projects/teacher` 依存を全チェックし、ゼロでのみ削除許可とする。
- もしくは「移管先教室必須」の移行フローを用意してから削除する。

### 50.3 プロジェクト削除の非原子的フロー

確認結果:

- `AdminPanel.deleteProject` は Storage削除 -> versions削除 -> Firestore削除を順次実行し、途中失敗時は警告のみで継続する。
- 一部削除成功・一部失敗の状態が残っても、呼び出し側には最終エラー粒度しか見えない。

影響:

- Firestoreドキュメントだけ残る/消える、Storageだけ残る/消えるなどの不整合が発生しうる。
- その後の読み込みで `storageUrl` 404 や履歴欠損が発生し、別機能障害に見える。

対策:

- 削除ジョブを「段階状態付き（pending/deleting/failed/deleted）」で管理し、再実行可能にする。
- 失敗時は対象IDを記録し、定期クリーンアップで収束させる。

### 50.4 第20版総合判定

- 本プランは引き続き実装可能。
- 他機能への負の影響を抑える追加Go条件:
  - `deleteUser` 後の残存プロジェクト処理方針が実装/運用の両面で確定済み
  - 教室削除前の依存チェック対象（student/project/teacher）が仕様化済み
  - プロジェクト削除に再実行可能な失敗収束手段がある

---

## 51. 再検証（別側面・第21版）

### 51.1 管理画面フィルター生成の未エスケープ注入

確認結果:

- `admin.js` の `updateFilters()` は `classrooms.name` / `users.displayName` を `innerHTML` へ直接埋め込み、`escapeHtml` を通していない。
- 入力値が運用データ由来であっても、将来インポートCSVや外部連携経路から特殊文字が混入するとDOM注入の踏み台になる。

影響:

- 管理画面のタブ切替/フィルタ更新でスクリプト実行やDOM破壊が起きる可能性がある。
- 本件ギャラリー改修とは別機能でも、同一データソースを共有するため横断的に障害が波及する。

対策:

- `updateFilters()` の option生成に `escapeHtml` を適用する。
- 可能なら `createElement('option') + textContent` に置換し、文字列テンプレート注入を廃止する。

### 51.2 モバイル管理画面のHTML直結レンダリング

確認結果:

- `admin-mobile.html` のオーバーライド実装は `container.innerHTML = ...` で `displayName` / `classroom.name` を未エスケープで埋め込んでいる。
- 同時に `onclick=\"...('${userId}')\"` のようなインラインイベント埋め込みを多用している。

影響:

- データ値次第で属性境界が崩れ、編集モーダル誤起動や任意イベント発火を誘発し得る。
- `admin.js` 本体を安全化しても、モバイル側オーバーライドが残ると再発する。

対策:

- モバイル側も `escapeHtml` を共通利用し、IDは `data-*` 属性で保持してイベント委譲で処理する。
- `admin.js` / `admin-mobile.html` で同一レンダリングユーティリティを共有する。

### 51.3 エスケープ仕様の不一致（`'` 未対応）

確認結果:

- `gallery.html` と `firebase-integration.js` の `escapeHtml` は `& < > \"` のみで、シングルクォートをエスケープしない。
- `admin.js` は `&#039;` まで対応しており、画面ごとに安全基準が揺れている。

影響:

- シングルクォート区切り属性（例: `onclick='...'`）を使う箇所で、将来的に値注入リスクが残る。
- 画面間で「同じデータでも安全性が異なる」状態となり、運用検証が難しくなる。

対策:

- エスケープ関数を共通化し、最低 `& < > \" '` を同一仕様で処理する。
- 文字列連結で属性を組み立てる箇所を縮小し、`textContent` / `setAttribute` / イベント委譲へ移行する。

### 51.4 CSP導入互換性の欠如

確認結果:

- 対象HTML群（`index.html`, `gallery.html`, `admin.html`, `admin-mobile.html`, `settings.html`）で CSP メタ/ヘッダ設計が見当たらない。
- インライン `onclick` 依存が多く、将来的に厳格CSPへ移行すると画面機能が一斉停止する構造。

影響:

- セキュリティ強化施策（CSP適用）と機能改修が衝突し、リリースリスクが急上昇する。

対策:

- 本件改修の並行タスクとして、インラインイベント依存を段階的に除去する。
- CSP導入ロードマップ（report-only -> 段階強化）を計画書へ明記する。

### 51.5 第21版総合判定

- 本プランは引き続き実装可能。
- 他機能への負の影響抑制の追加Go条件:
  - `admin.js` / `admin-mobile.html` の動的HTML生成が共通サニタイズ方針へ統一済み
  - `escapeHtml` が全画面で同一仕様（`'` 含む）に統一済み
  - CSP移行時に機能停止しないイベント実装方針が定義済み

---

## 52. 再検証（別側面・第22版: 実装反映後）

### 52.1 インラインイベント依存の除去進捗

確認結果:

- `gallery.html` / `firebase-integration.js` / `admin-mobile.html` / `admin.html` の `onclick/onchange/oninput/onsubmit/onerror` を除去済み。
- `admin.js` 側は `data-admin-action` のイベント委譲へ移行済み。

判定:

- CSP導入互換性は大幅改善。将来の `script-src` 強化に対する阻害要因を削減。
- UI操作の主経路（タブ切替、モーダル閉じる、フィルタ、詳細操作）は `addEventListener` ベースで維持。

### 52.2 ギャラリー表示失敗の運用上ボトルネック（インデックス）

確認結果:

- Firestoreクエリの `where + orderBy` 組み合わせに対して、`firestore.indexes.json` を追加し必要複合indexを定義済み。
- 対象:
  - `users`: `classroomId ASC + displayName ASC`
  - `projects`: `classroomId ASC + updatedAt DESC`, `userId ASC + updatedAt DESC`
  - `videos`: `status ASC + createdAt DESC`, `status ASC + classroomId ASC + createdAt DESC`

判定:

- 新規環境/復旧環境での「index不足による空表示・permission誤認」を予防可能。
- ただし実効性はデプロイ後に確定するため、反映確認は必須。

### 52.3 ルール・関数変更の副作用評価

確認結果:

- `firestore.rules` は project更新時の変更可能キー制限と不変項目保護を導入済み。
- `functions/index.js` は `createUsers/createUser` の入力検証・正規化を統一、`saveVideoMetadata` 所有者パス検証を追加済み。
- `getPublicVideos` の `limit` は `1..50` へクランプ済み。

副作用評価:

- 管理画面の正常系（作成/編集/削除）に対する機能退行は低リスク。
- 異常系では従来より早く拒否されるため、運用上は「以前通っていた不正入力」が失敗する可能性がある（意図した強化）。

### 52.4 未完了事項（現時点の制約）

- Firebase Emulator 実行はこの環境で安定成功しておらず、ルールE2E自動検証は未完了。
- 2026-03-03 時点の確認ログ:
  - 既定設定パス利用時: `EPERM: operation not permitted, open 'C:\\Users\\riyum\\.config\\configstore\\firebase-tools.json.*'`
  - 代替設定パス利用時: 認証コンテキスト不在により `Failed to authenticate, have you run firebase login?` および `No emulators to start`
- 本番相当確認として必要な作業:
  - `firestore.rules` / `storage.rules` / `firestore.indexes.json` のデプロイ
  - `docs/testing/2026-03-03_gallery-auth-smoke-test.md` に沿ったロール別手動検証

### 52.5 第22版総合判定

- 本プランで実装は成立しており、主要改修はコード反映済み。
- 負の影響は管理可能な範囲だが、最終Go判定は以下2点の完了を条件とする:
  - ルール/インデックス反映後の実機スモーク完了
  - 管理画面（PC/モバイル）のイベント回帰チェック完了

---

## 53. 再検証（別側面・第23版: ギャラリー障害耐性）

### 53.1 ログイン後ロード失敗時のメッセージ改善

実装反映:

- `gallery.html` に `getFriendlyLoadError()` を追加。
- Firestore主要エラーコード（`permission-denied`, `failed-precondition`, `unavailable`）を利用者向け文言へ変換。
- 再試行ボタンは `addEventListener` で接続（DOM属性依存を排除）。

期待効果:

- 「表示されない」事象時に、権限問題と接続問題を切り分けやすくなる。
- 学校現場での一次切り分け（ネットワーク/権限）を迅速化できる。

### 53.2 独自ログイン連携データ不整合の明示化

実装反映:

- `loadUserData()` で以下を明示検証:
  - `role` が `admin|teacher|student` のいずれか
  - `teacher` の `classroomId` 未設定をエラー化

期待効果:

- 不整合データを黙って student扱い/空表示にせず、設定不備として明示できる。
- 「ログインは成功するがギャラリーが空」の曖昧障害を減らせる。

### 53.3 フィルター生成と日付ソートの堅牢化

実装反映:

- `updateFilters()` を `innerHTML` 連結から `createElement('option')` へ変更。
- 日付ソートは `toMillis()` 経由に統一し、`Timestamp/Date/null` 混在時の比較失敗を回避。

期待効果:

- 表示データ由来のDOM注入リスクを低減。
- データ型ゆらぎ時のソート例外を抑止し、一覧描画の安定性を向上。

### 53.4 第23版総合判定

- 本プランは実装面でさらに強化され、ギャラリー非表示系の既知要因を追加で低減した。
- 最終残タスクは引き続き「デプロイ後の実機検証」であり、静的検証だけで完了判定はしない。

---

## 54. 再検証（別側面・第24版: 検証運用性）

実装反映:

- `scripts/verify_gallery_auth_static.ps1` を追加し、以下を一括検証可能にした:
  - 主要ファイルのインラインイベント残存チェック
  - `admin.js` / `firebase-integration.js` / `functions/index.js` の構文チェック
  - `firebase.json` / `firestore.indexes.json` のJSON妥当性チェック
- 手動検証書へ「0. Static precheck」を追加し、検証開始前の品質ゲートを明文化。

判定:

- 手動テスト前に機械的に壊れを弾けるため、再検証コストと見落としリスクを低減。
- 実機検証未完了という制約は維持されるが、静的回帰の再現性は改善。

---

## 55. 再検証（別側面・第25版: ギャラリーイベント一貫性）

実装反映:

- `gallery.html` の以下を `onclick` プロパティ代入から `addEventListener` へ統一:
  - `openInScratch` ボタン
  - `previewModal` 外側クリックで閉じる処理
- 併せて `selectedPreviewProjectId` を導入し、プレビュー対象IDの状態を明示管理。

判定:

- イベント実装が画面全体で一貫し、将来的なCSP強化との整合が向上。
- プレビュー遷移中の対象IDが明示化され、クリック競合時の誤参照リスクを低減。

---

## 56. 再検証（別側面・第26版: インデックス充足性）

実装反映:

- `firestore.indexes.json` に `gallery.html` の複合 `where` 条件用 index を追加:
  - `projects`: `isSubmitted ASC + classroomId ASC`
  - `projects`: `isSubmitted ASC + userId ASC`

背景:

- ギャラリーは role別に `projects` を以下で取得する:
  - 管理者: `where(isSubmitted == true)`
  - 講師: `where(isSubmitted == true).where(classroomId == X)`
  - 生徒: `where(isSubmitted == true).where(userId == uid)`
- 環境差分やデータ量増加時に複合index不足で `failed-precondition` となる余地を事前排除するため、明示定義を採用。

判定:

- 「ログインは成功するがギャラリーが読み込めない」系の潜在要因をさらに削減。
- 反映効果はデプロイ後に有効化されるため、最終的には実機スモークで確認する。

---

## 57. 再検証（別側面・第27版: 削除確認イベントの安定性）

実装反映:

- `admin.js` の `confirmDeleteBtn.onclick = ...` 代入を廃止。
- `pendingDeleteAction` を導入し、`init()` 時に `confirmDeleteBtn` へ一度だけ click listener を登録。
- `confirmDeleteModal` を閉じる際は `pendingDeleteAction` をクリアして誤実行を防止。

効果:

- 削除対象の切替時にハンドラ上書きが散在せず、確認モーダルの状態遷移が一元化。
- イベント設計が `addEventListener` へ統一され、管理画面のCSP適合性が向上。

---

## 58. 再検証（別側面・第28版: `.onclick` 廃止徹底）

実装反映:

- `firebase-integration.js` の `.onclick` 代入3箇所を `addEventListener` に置換:
  - ログインボタン
  - ユーザーメニューボタン
  - クラウドロードモーダル外側クリック
- 静的検証スクリプトで `.onclick` 代入の残存チェックを追加。

検証結果:

- `scripts/verify_gallery_auth_static.ps1` 実行で `.onclick` 代入残存なしを確認。

判定:

- イベント実装の一貫性が向上し、画面差分による再発リスクを追加で低減。

---

## 59. 再検証（別側面・第29版: 再初期化耐性）

実装反映:

- `firebase-integration.js` に初期化ガードを追加:
  - `initialized` による `init()` 多重実行防止
  - `authListenerBound` による `onAuthStateChanged` 二重購読防止
- UI重複生成防止:
  - style要素に `id=firebase-ui-styles` を付与し、重複注入を回避
  - ログインモーダルに `id=firebase-modal-overlay-root` を付与し、既存DOM再利用
- document click の重複登録防止:
  - `userMenuDocumentClickHandler` を保持し、1回だけバインド

判定:

- GUI再描画や将来的な再初期化経路が入っても、イベント重複・DOM重複による不安定化を抑制できる構造へ改善。

---

## 60. 再検証（別側面・第30版: ギャラリー多重バインド耐性）

実装反映:

- `gallery.html` に以下のガードを追加:
  - `galleryUiEventsBound`（UIイベント多重登録防止）
  - `galleryAuthObserverBound`（`auth.onAuthStateChanged` 二重購読防止）
- 末尾の直接イベント登録を `bindGalleryUiEvents()` / `bindGalleryAuthObserver()` に集約し、初回のみ実行。

判定:

- 将来的な再初期化経路やスクリプト再評価が発生しても、イベント増殖による二重実行リスクを抑制。

---

## 61. 再検証（別側面・第31版: インデックス回帰検知）

実装反映:

- `scripts/verify_firestore_indexes.ps1` を新規追加。
- `firestore.indexes.json` に対して、主要クエリに必要な index 定義の存在を機械検証:
  - `users`: `classroomId + displayName`
  - `projects`: `isSubmitted + classroomId`, `isSubmitted + userId`, `classroomId + updatedAt`, `userId + updatedAt`
  - `videos`: `status + createdAt`, `status + classroomId + createdAt`
- `scripts/verify_gallery_auth_static.ps1` からも index coverage を連動実行。

検証結果:

- `verify_firestore_indexes.ps1`: `Index coverage: PASSED`
- `verify_gallery_auth_static.ps1`: `Static verification: PASSED`

判定:

- index 定義漏れによる `failed-precondition` 再発リスクを、デプロイ前に検知できる運用へ改善。

---

## 62. 再検証（別側面・第32版: DOMイベント代入の混入防止）

実装反映:

- `scripts/verify_gallery_auth_static.ps1` に `.on...` 代入検出（`onclick/change/input/submit/error/load/...`）を追加。
- 既存の inline handler チェックと併用し、HTML属性・JSプロパティ代入の両方を検知可能にした。

検証結果:

- 最新実装で `.onclick` / `.on...` 代入残存なし（`Static verification: PASSED`）。

判定:

- イベント実装方針（`addEventListener` 統一）を機械的に維持できる状態へ改善。

---

## 63. 再検証（別側面・第33版: ログインID正規化の統一）

実装反映:

- `gallery.html`:
  - `normalizeUserId()` / `buildLoginEmail()` を追加
  - `handleLogin()` は正規化済みIDからメール生成
- `admin-mobile.html`:
  - `normalizeUserId()` / `buildLoginEmail()` を追加
  - `@laughtale.local` 連結を共通関数へ集約
- `firebase-integration.js`:
  - `ScratchFirebase.normalizeUserId()` を追加
  - `userIdToEmail()` は正規化を内部適用
  - `login()` で空IDを事前検知

判定:

- 大文字/前後空白を含む入力に対するログイン挙動が画面間で一致。
- 将来的な修正点が1か所に寄り、再発確率を低減。

---

## 64. 再検証（別側面・第34版: 正規化ルールの検証固定化）

実装反映:

- `scripts/verify_gallery_auth_static.ps1` に以下の存在/使用チェックを追加:
  - `gallery.html`: `normalizeUserId()` / `buildLoginEmail()` と利用箇所
  - `admin-mobile.html`: `normalizeUserId()` / `buildLoginEmail()` と利用箇所
  - `firebase-integration.js`: `normalizeUserId()` / `userIdToEmail()`

検証結果:

- 追加チェックを含めて `Static verification: PASSED`。

判定:

- ログイン正規化の実装方針が手動レビュー依存ではなく、静的チェックで継続担保できる状態へ改善。

---

## 65. 再検証（別側面・第35版: 生連結回帰の防止）

実装反映:

- `scripts/verify_gallery_auth_static.ps1` に禁止パターン検出を追加:
  - `toLowerCase() + EMAIL_DOMAIN/LOGIN_EMAIL_DOMAIN`
  - `toLowerCase() + '@laughtale.local'`
- 対象: `gallery.html` / `admin-mobile.html` / `firebase-integration.js`

検証結果:

- 追加ルールを含めて `Static verification: PASSED`。

判定:

- ログイン実装が将来変更されても、正規化ヘルパーを経由しない生連結の混入を早期に検知可能。

---

## 66. 再検証（別側面・第36版: Firebase設定配線の検証）

実装反映:

- `scripts/verify_firebase_config_wiring.ps1` を追加。
- `firebase.json` の配線を機械検証:
  - `firestore.rules` ファイル参照
  - `firestore.indexes` ファイル参照
  - `storage.rules` ファイル参照
  - 参照先ファイルの実在確認
- `verify_gallery_auth_static.ps1` からも連動実行。

検証結果:

- `verify_firebase_config_wiring.ps1`: `Firebase config wiring: PASSED`
- 総合静的検証: `Static verification: PASSED`

判定:

- 「コードは直したが firebase.json が結線漏れで効かない」タイプのリリース事故をデプロイ前に検知可能。

---

## 67. 再検証（別側面・第37版: Security Rules契約の回帰検知）

実装反映:

- `scripts/verify_rules_contract.ps1` を追加。
- `firestore.rules` / `storage.rules` に対して、必須契約の存在を機械検証:
  - project update の変更キー制限と不変項目保護
  - storage の recordings 所有者/管理者制限
  - storage の project/classroom アクセス制約
- `verify_gallery_auth_static.ps1` から連動実行。

検証結果:

- `verify_rules_contract.ps1`: `Rules contract: PASSED`
- 総合静的検証: `Static verification: PASSED`

判定:

- ルールの重要ガードが将来編集で欠落した場合、デプロイ前に検知可能。

---

## 68. 再検証（別側面・第38版: 検証ランナー運用）

実装反映:

- `scripts/run_verification_suite.ps1` を追加。
- 推奨1コマンドで総合静的検証を実行し、`docs/testing/logs/` に時刻付きログを保存可能。
- ログ差分ノイズ抑制のため `docs/testing/logs/.gitignore` を追加。

検証結果:

- `run_verification_suite.ps1` 実行で `Verification suite completed: PASSED` を確認。

判定:

- 検証実行手順のばらつきを抑え、再現性の高い運用フローへ改善。

---

## 69. 再検証（別側面・第39版: プリデプロイゲート）

実装反映:

- `scripts/predeploy_gallery_auth.ps1` を追加。
- 仕様:
  - 検証スイート (`run_verification_suite.ps1`) を必須実行
  - `gallery-auth` スコープの deploy 対象を明示:
    - `hosting,functions,firestore:rules,firestore:indexes,storage`
  - 既定は dry-run（実行コマンド表示のみ）、`-Deploy` 指定時のみ実デプロイ

検証結果:

- dry-run 実行で検証通過と deploy コマンド生成を確認。

判定:

- デプロイ直前に必須検証を強制できるため、手順漏れによる事故リスクを低減。

---

## 70. 再検証（別側面・第40版: 運用スクリプト安全化）

実装反映:

- `predeploy_gallery_auth.ps1`
  - `Invoke-Expression` を廃止し、`firebase` を引数配列で安全実行
  - `firebase` CLI存在チェックを追加
- `run_verification_suite.ps1`
  - transcript開始失敗時は警告表示で継続（検証自体は実行）

検証結果:

- `run_verification_suite.ps1` 単独実行で `Verification suite completed: PASSED` を確認。
- `predeploy_gallery_auth.ps1` dry-run で検証ゲート通過と deploy コマンド生成を確認。

---

## 71. 再検証（別側面・第41版: 検証スクリプト自身の品質保証）

実装反映:

- `scripts/verify_powershell_scripts_syntax.ps1` を追加。
- `scripts/*.ps1` を PowerShell parser で構文検査し、エラー位置を出力。
- `verify_gallery_auth_static.ps1` から連動実行。

検証結果:

- `PowerShell script syntax: PASSED`
- 総合静的検証: `Static verification: PASSED`

判定:

- 検証ツール自体の壊れを早期検知できるため、運用信頼性を向上。

---

## 72. 再検証（別側面・第42版: HTML ID一意性の回帰検知）

実装反映:

- `scripts/verify_html_id_uniqueness.ps1` を追加。
- 対象: `gallery.html` / `admin.html` / `admin-mobile.html`
- `id="..."` を抽出し、重複IDを検知して失敗させる。
- `verify_gallery_auth_static.ps1` から連動実行。

検証結果:

- `HTML id uniqueness: PASSED`
- 総合静的検証: `Static verification: PASSED`

判定:

- UIイベント紐付け不良の原因になりやすい重複IDをデプロイ前に検知可能。

---

## 73. 再検証（別側面・第43版: 欠損ユーザー監査の実装）

実装反映:

- `functions/index.js` に管理者向け callable `auditUserDocumentCoverage` を追加。
- 監査内容:
  - Firebase Auth ユーザー（任意上限）を列挙
  - `users/{uid}` 文書の存在を照合
  - 欠損件数 (`totals.missingUserDocs`) と欠損サンプル (`sampleMissing`) を返却
- オプション:
  - `maxUsers`（1..5000）
  - `onlyLocalDomain`（既定 true）
  - `sampleSize`（1..200）

検証結果:

- `functions/index.js` の構文検証通過。
- 総合静的検証: `Static verification: PASSED`

判定:

- 未完項目「欠損ユーザー件数把握」に対し、実測手段を実装済み。
- 残作業はデプロイ後に当関数を実行し、実数を記録すること。

---

## 74. 再検証（別側面・第44版: 未完項目の実行テンプレート化）

実装反映:

- 障害注入テスト手順書を追加:
  - `docs/testing/2026-03-03_gallery-auth-fault-injection-test.md`
- 障害注入結果テンプレートを追加:
  - `docs/testing/2026-03-03_gallery-auth-fault-injection-results-template.md`
- E2E担当/ロールバック合意テンプレートを追加:
  - `docs/testing/2026-03-03_gallery-auth-e2e-ownership-template.md`
- スモーク手順書へ section 8/9 を追記し、未完項目へ直接接続。

判定:

- 実機実施・体制合意が必要な未完項目について、即実行可能な入力フォームと手順を整備。
- 残タスクは「実行して埋める」段階へ移行。

---

## 75. 再検証（別側面・第45版: 監査UIの運用接続）

実装反映:

- `admin.html` ダッシュボードへ「ユーザー整合監査」カードを追加。
- `admin.js` で `auditUserDocumentCoverage` callable を呼び出し、欠損件数を即表示。
- 管理者以外には監査ボタンを非表示化。

検証結果:

- `node --check admin.js` 通過。
- 総合静的検証: `Static verification: PASSED`

判定:

- 未完項目「欠損ユーザー件数把握」を実機運用に直結できる導線を管理UIへ追加。

---

## 76. 再検証（別側面・第46版: 監査結果記録の定型化）

実装反映:

- `docs/testing/2026-03-03_user-coverage-audit-result-template.md` を追加。
- `auditUserDocumentCoverage` の結果（audited/missing/coverageRate）を標準フォーマットで記録可能にした。
- スモーク手順書から当テンプレートへリンク。

判定:

- 欠損件数把握タスクの証跡を残しやすくなり、未完項目の完了判定を明確化。

---

## 77. 再検証（別側面・第47版: デプロイ直前の証跡強制ゲート）

実装反映:

- `scripts/run_verification_suite.ps1` に `-IncludeExecutionArtifacts` を追加。
  - `check_execution_artifacts.ps1` を連動実行できるようにした。
- `scripts/predeploy_gallery_auth.ps1` を拡張。
  - `-RequireExecutionArtifacts` を追加（dry-runでも証跡チェック可能）。
  - `-Deploy` 実行時は既定で証跡チェックを必須化（`-AllowIncompleteExecutionArtifacts` 指定時のみバイパス）。

検証結果:

- `predeploy_gallery_auth.ps1`（通常dry-run）: PASS
- `predeploy_gallery_auth.ps1 -RequireExecutionArtifacts`: 証跡未記入のため意図どおり FAIL
- `run_verification_suite.ps1`（通常）: PASS

判定:

- 「静的検証は通るが、E2E/障害注入/監査結果が未記入のままデプロイされる」運用事故を防止できる。
- 未完5項目のうち、実行証跡系3項目（障害注入、欠損件数、体制合意）は機械的なブロック対象になった。

---

## 78. 再検証（別側面・第48版: リリース判定の可視化）

実装反映:

- `scripts/generate_release_readiness_report.ps1` を追加。
- 以下を一度に実行して Markdown レポートを出力:
  - `run_verification_suite.ps1`
  - `check_execution_artifacts.ps1`
  - `calc_plan_progress.ps1`
  - `show_plan_todo.ps1`
- 出力先:
  - `docs/testing/executions/release-readiness_YYYYMMDD_HHMMSS.md`

検証結果:

- スクリプト実行によりレポート生成を確認。
- 現在の証跡未記入状態では `Overall: NO-GO` を正しく返すことを確認。

判定:

- 「何が未完で止まっているか」を単一ファイルで監査できる状態に改善。
- 実運用では、レポート `GO` をデプロイ実行の前提条件として扱える。

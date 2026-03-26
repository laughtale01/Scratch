# パスワード確認機能 実装計画書

**作成日**: 2026-03-26
**最終更新**: 2026-03-26（レビュー6回目反映 — 最終版）
**対象ブランチ**: gh-pages
**目的**: 管理画面でユーザーのパスワードを確認できるようにする

---

## 1. 現状分析

### 1.1 現在のパスワード関連フロー

#### ユーザー作成時（単体）
- **フロントエンド**: `admin.js` L724-785 `AdminPanel.createUser()`
  - フォームから `userId`, `displayName`, `password`, `role`, `classroomId` を取得
  - `cloudFunctions.createUser()` を呼び出し
- **バックエンド**: `functions/index.js` L83-185 `exports.createUser`
  - Firebase Auth でユーザー作成（`auth.createUser({ email, password, ... })`）L140-145
  - Firestore `users/{uid}` にドキュメント保存 L149-157
  - **問題点**: Firestoreにはパスワードが保存されていない
  - 保存されるフィールド: `email`, `displayName`, `role`, `classroomId`, `createdAt`, `lastLoginAt`, `saveCount`

#### ユーザー一括作成時
- **バックエンド**: `functions/index.js` L479-575 `exports.createUsers`
  - 配列で受け取りループ処理 L504
  - Firestoreへの保存 L542-550
  - 同じく Firestore にパスワードは保存していない

#### パスワードリセット時
- **フロントエンド**: `admin.js` L911-958
  - `showResetPasswordModal()` L913-922: モーダルを表示
  - `resetPassword()` L927-958: Cloud Function呼び出し
  - **注意**: リセット後に `loadUsers()` を呼んでいない（L946の後にreturnしている）
- **バックエンド**: `functions/index.js` L197-256 `exports.resetPassword`
  - `auth.updateUser(targetUid, { password: newPassword })` L241-243 でFirebase Authのみ更新
  - return文 L247-250 で即座に成功を返す
  - **問題点**: Firestoreにも新パスワードを保存していない

### 1.2 現在のユーザー一覧表示

#### デスクトップ版 (`admin.html` + `admin.js`)
- **テーブルヘッダー** (`admin.html` L1901-1911):
  - 列: ユーザーID | 表示名 | ロール | 教室 | 最終ログイン | 保存回数 | 操作
  - `colspan="7"` で空状態表示 L1915
- **テーブル描画** (`admin.js` L471-518 `renderUsersTable()`):
  - 各行に「編集」「PW」「削除」ボタンを表示 L510-512
  - 「PW」ボタン = パスワードリセットモーダルを開く
  - データは `this.users` 配列（`loadUsers()`でFirestoreから取得したキャッシュ）から描画

#### モバイル版 (`admin-mobile.html`)
- L2074で `admin.js` を読み込み、その後インラインスクリプトでメソッドをオーバーライド
- L2403-2404: `AdminPanel.renderUsersTable` をオーバーライドしてカード形式表示
- パスワードリセットは「編集モーダル」内のボタンから（L1949 `editUserResetPwBtn`）
- カード形式にはPWボタンがない（編集モーダル経由のみ）

### 1.3 現在のFirestore `users` コレクション スキーマ

```
users/{uid}
├── email: string          (例: "tanaka123@laughtale.local")
├── displayName: string    (例: "田中太郎")
├── role: string           ("admin" | "teacher" | "student")
├── classroomId: string|null
├── createdAt: Timestamp
├── lastLoginAt: Timestamp|null
├── saveCount: number
└── (updatedAt: Timestamp) ← updateUserId時のみ
```

### 1.4 権限モデル（現行）

| 操作 | admin | teacher | student |
|------|-------|---------|---------|
| ユーザー作成 | 全て | 自教室の生徒のみ | × |
| パスワードリセット | 全て | 自教室の生徒のみ | × |
| ユーザー削除 | 全て（自分以外） | 自教室の生徒のみ | × |
| ユーザーID変更 | 全て | × | × |

→ **パスワード確認も同じ権限モデルを適用する**（UIレベルでの制御）

### 1.5 Firestoreセキュリティルール現状（`firestore.rules` L63-102）

```
match /users/{userId} {
  allow read: if isAuthenticated() && (
    isAdmin() ||
    (isTeacher() && (resource.data.classroomId == getUserData().classroomId || userId == request.auth.uid)) ||
    userId == request.auth.uid   // ← 生徒も自分自身のドキュメントを読める
  );
  allow create: if false;         // Cloud Functions経由のみ
  allow update: if isAuthenticated() && (...);
  allow delete: if false;         // Cloud Functions経由のみ
}
```

**重要**: 生徒も自分自身のドキュメントを読み取れるため、`currentPassword` フィールドを追加すると、
生徒がブラウザの開発者ツールでFirestoreのレスポンスを確認すれば、自分のパスワードが見える。
**判断**: 対象が小学生の教育アカウントであり、開発者ツールを使う可能性は極めて低いため、
フィールドレベルのアクセス制御は不要と判断する。（Firestoreはフィールド単位の読み取り制限をサポートしていないため、
やるならサブコレクション `users/{uid}/secrets` に分離する必要があるが、過剰設計と判断。）

### 1.6 既存CSSテーマ

管理画面はMinecraftテーマのCSSを使用（ダークテーマ）。

**ボタンスタイル（すべてグラデーション+立体的ボーダー）**:
- `.btn-primary`: `--mc-grass` 系グラデーション（緑）
- `.btn-secondary`: `--mc-button-top/bottom` 系グラデーション（灰）
- `.btn-danger`: `--mc-redstone` 系グラデーション（赤）
- **`.btn-info` は未定義** → 新規追加が必要

**フォーム入力 `.form-input`**:
- `background: var(--mc-void)` = `#0c0c0f`（ほぼ黒）
- `color: var(--mc-text-white)`（白テキスト）
- `border-color: #333333 #666666 #666666 #333333`（立体的ボーダー）

**アラート `.alert-info`**:
- `background: linear-gradient(rgba(74, 237, 217, 0.2), rgba(44, 181, 168, 0.2))`（半透明ダイヤモンド色）
- `border-color: var(--mc-diamond-dark)`, `color: var(--mc-diamond)`

**テーマカラー**:
- `--mc-diamond`: `#4aedd9` / `--mc-diamond-dark`: `#2cb5a8`（ダイヤモンド青緑）
- `--mc-gold`: `#fcdb05` / `--mc-gold-dark`: `#c6a503`（金色 — 警告表示向け）

→ パスワード確認ボタンには `--mc-diamond` 系を使用

### 1.7 モーダル閉じるボタンのパターン差異

**デスクトップ版** (`admin.html`):
- `data-close-modal="モーダルID"` 属性を使用
- L2330-2332で `querySelectorAll('[data-close-modal]')` によるイベント委譲で一括処理

**モバイル版** (`admin-mobile.html`):
- `data-close-modal` のハンドラは**存在しない**
- 代わりに `id` ベースの個別ハンドラを使用:
  ```javascript
  document.getElementById('btnCloseResetPasswordModal')?.addEventListener('click', () => AdminPanel.closeModal('resetPasswordModal'));
  ```
- モーダル外クリック (`modal-overlay` クリック) は `admin.js` L1489-1494で処理（両バージョン共通）

→ **デスクトップ版は `data-close-modal`、モバイル版は `id` ベースのハンドラ**を使い分ける

---

## 2. 実装方針

### 2.1 基本方針

Firestoreの `users/{uid}` ドキュメントに `currentPassword` フィールドを追加し、以下のタイミングで保存・更新する:

1. **ユーザー作成時** → 初期パスワードを `currentPassword` として保存
2. **一括作成時** → 同上
3. **パスワードリセット時** → 新しいパスワードで `currentPassword` を上書き
4. **管理画面表示** → ユーザー一覧からパスワードを確認できるUIを追加

### 2.2 セキュリティ上の注意と判断

- **平文保存である**: Firebase Authのハッシュ化パスワードとは別に、Firestoreに平文で保存する
- **教育用途での許容性**: 対象が小学生の教育アカウントであり、管理者/講師のみがUIから閲覧可能なため、実用上は許容範囲
- **Firestoreセキュリティルール**: 現行ルールでは生徒も自分自身のドキュメントを読めるため、技術的にはDevToolsから自分のパスワードを見ることが可能。ただし小学生がDevToolsを使う可能性は実質ゼロなので許容する
- **生徒自身がパスワードを変更した場合**: 現在のシステムには生徒自身のパスワード変更機能がないため、`currentPassword` は常に最新を反映する

---

## 3. 変更対象ファイルと詳細

### 3.1 `functions/index.js` — Cloud Functions（バックエンド）

#### 変更箇所①: `createUser` 関数（L149-157）

**現在のコード** (L149-157):
```javascript
await db.collection('users').doc(userRecord.uid).set({
  email: email,
  displayName: trimmedDisplayName,
  role: role,
  classroomId: role === 'admin' ? null : classroomId,
  createdAt: admin.firestore.FieldValue.serverTimestamp(),
  lastLoginAt: null,
  saveCount: 0
});
```

**変更後**:
```javascript
await db.collection('users').doc(userRecord.uid).set({
  email: email,
  displayName: trimmedDisplayName,
  role: role,
  classroomId: role === 'admin' ? null : classroomId,
  currentPassword: password,           // ← 追加
  createdAt: admin.firestore.FieldValue.serverTimestamp(),
  lastLoginAt: null,
  saveCount: 0
});
```

**変更内容**: `currentPassword: password` フィールドを1行追加するのみ。

---

#### 変更箇所②: `createUsers` 関数（一括作成）（L542-550）

**現在のコード** (L542-550):
```javascript
await db.collection('users').doc(userRecord.uid).set({
  email: email,
  displayName: trimmedDisplayName,
  role: role,
  classroomId: role === 'admin' ? null : classroomId,
  createdAt: admin.firestore.FieldValue.serverTimestamp(),
  lastLoginAt: null,
  saveCount: 0
});
```

**変更後**:
```javascript
await db.collection('users').doc(userRecord.uid).set({
  email: email,
  displayName: trimmedDisplayName,
  role: role,
  classroomId: role === 'admin' ? null : classroomId,
  currentPassword: password,           // ← 追加
  createdAt: admin.firestore.FieldValue.serverTimestamp(),
  lastLoginAt: null,
  saveCount: 0
});
```

**変更内容**: `createUser` と同一の変更。

---

#### 変更箇所③: `resetPassword` 関数（L239-250）

**現在のコード** (L239-250):
```javascript
try {
  // パスワードを更新
  await auth.updateUser(targetUid, {
    password: newPassword
  });

  console.log(`Password reset for user: ${targetUid}`);

  return {
    success: true,
    message: 'パスワードをリセットしました'
  };

} catch (error) {
```

**変更後**:
```javascript
try {
  // パスワードを更新
  await auth.updateUser(targetUid, {
    password: newPassword
  });

  // Firestoreにも新しいパスワードを保存（失敗しても処理は続行）
  try {
    await db.collection('users').doc(targetUid).update({
      currentPassword: newPassword
    });
  } catch (firestoreError) {
    console.error('resetPassword: Firestoreパスワード保存エラー（Auth側は更新済み）', firestoreError);
  }

  console.log(`Password reset for user: ${targetUid}`);

  return {
    success: true,
    message: 'パスワードをリセットしました'
  };

} catch (error) {
```

**変更内容**: Firebase Auth更新（L241-243）の直後、`console.log`（L245）の前に挿入。

**エラーハンドリングの判断**:
- Firestore更新が失敗した場合でも、Firebase Auth側のパスワード変更は成功している
- Auth側が正（実際のログインに使われる）なので、Firestore更新失敗時はログ出力のみで処理を続行する
- 内側の `try-catch` で囲んで、外側のcatchに伝播させない

---

### 3.2 `admin.js` — 管理画面フロントエンド

#### 変更箇所④: `renderUsersTable()` のテーブル行（L508-514）

**現在のコード** (L508-514):
```javascript
<td>
  <div class="btn-group">
    ${canEdit ? `<button class="btn btn-secondary btn-sm" data-admin-action="edit-user" data-user-id="${this.escapeHtml(user.id)}">編集</button>` : ''}
    ${canResetPassword ? `<button class="btn btn-secondary btn-sm" data-admin-action="reset-user-password" data-user-id="${this.escapeHtml(user.id)}">PW</button>` : ''}
    ${canDelete ? `<button class="btn btn-danger btn-sm" data-admin-action="delete-user" data-user-id="${this.escapeHtml(user.id)}">削除</button>` : ''}
  </div>
</td>
```

**変更後**:
```javascript
<td>
  <div class="btn-group">
    ${canEdit ? `<button class="btn btn-secondary btn-sm" data-admin-action="edit-user" data-user-id="${this.escapeHtml(user.id)}">編集</button>` : ''}
    ${canResetPassword ? `<button class="btn btn-info btn-sm" data-admin-action="show-password" data-user-id="${this.escapeHtml(user.id)}">PW確認</button>` : ''}
    ${canResetPassword ? `<button class="btn btn-secondary btn-sm" data-admin-action="reset-user-password" data-user-id="${this.escapeHtml(user.id)}">PW変更</button>` : ''}
    ${canDelete ? `<button class="btn btn-danger btn-sm" data-admin-action="delete-user" data-user-id="${this.escapeHtml(user.id)}">削除</button>` : ''}
  </div>
</td>
```

**変更点**:
- 「PW」→「PW変更」にラベル変更（役割を明確化）
- 「PW確認」ボタンを新規追加（`data-admin-action="show-password"`）
- `btn-info` クラスで色分け（ダイヤモンド色 = 情報確認、灰色 = 既存操作）

---

#### 変更箇所⑤: パスワード確認メソッド追加（新規）

`AdminPanel` クラスに以下のメソッドを追加する。

**追加位置**: `showResetPasswordModal()` メソッド（L911-922）の直前

```javascript
/**
 * パスワード確認モーダルを表示
 */
static showPasswordModal(userId) {
  const user = this.users.find(u => u.id === userId);
  if (!user) return;

  const userName = user.displayName || user.email;
  const password = user.currentPassword || '（未設定）';

  document.getElementById('showPasswordUserName').textContent = userName;
  document.getElementById('showPasswordValue').value = password;  // input要素なので.valueを使用

  this.showModal('showPasswordModal');
}
```

**注意**: `showPasswordValue` は `<input type="text" readonly>` であるため、
`.textContent` ではなく `.value` を使う必要がある。`.textContent` は `<input>` には効かない。

---

#### 変更箇所⑥: `resetPassword()` メソッドにキャッシュ更新追加（L943-946）

**現在のコード** (L943-946):
```javascript
console.log('AdminPanel: パスワードリセット完了', result.data);
alert('パスワードをリセットしました');

this.closeModal('resetPasswordModal');
```

**変更後**:
```javascript
console.log('AdminPanel: パスワードリセット完了', result.data);
alert('パスワードをリセットしました');

// ローカルキャッシュのパスワードも更新（PW確認ボタンで即座に反映させるため）
const targetUser = this.users.find(u => u.id === targetUid);
if (targetUser) {
  targetUser.currentPassword = newPassword;
}

this.closeModal('resetPasswordModal');
```

**理由**: `resetPassword()` は完了後に `loadUsers()` を呼んでいない（既存実装）。
Firestoreには新パスワードが保存されるが、メモリ上の `this.users` キャッシュは古いまま。
リセット直後に「PW確認」をクリックすると古いパスワード（または「未設定」）が表示されてしまう。
`loadUsers()` を追加する方法もあるが、既存の動作を変えないよう、キャッシュのみ更新する軽量な方法を採用。

---

#### 変更箇所⑦: イベントハンドラ登録（L1515-1516）

**現在のコード** (L1515-1516):
```javascript
if (action === 'edit-user' && userId) AdminPanel.showEditUserModal(userId);
if (action === 'reset-user-password' && userId) AdminPanel.showResetPasswordModal(userId);
```

**変更後**:
```javascript
if (action === 'edit-user' && userId) AdminPanel.showEditUserModal(userId);
if (action === 'show-password' && userId) AdminPanel.showPasswordModal(userId);
if (action === 'reset-user-password' && userId) AdminPanel.showResetPasswordModal(userId);
```

**変更内容**: `show-password` アクションのハンドラを1行追加。

---

### 3.3 `admin.html` — HTML（デスクトップ版）

#### 変更箇所⑧: パスワード確認モーダル追加（新規）

**追加位置**: パスワードリセットモーダル（L2098-2120）の直前

```html
<!-- パスワード確認モーダル -->
<div class="modal-overlay" id="showPasswordModal">
  <div class="modal">
    <div class="modal-header">
      <h3 class="modal-title">🔑 パスワード確認</h3>
      <button class="modal-close" data-close-modal="showPasswordModal">×</button>
    </div>
    <div class="modal-body">
      <div class="alert alert-info">
        ユーザー「<strong><span id="showPasswordUserName"></span></strong>」のパスワード
      </div>
      <div class="form-group">
        <label class="form-label">現在のパスワード</label>
        <div style="display: flex; align-items: center; gap: 8px;">
          <input type="text" class="form-input" id="showPasswordValue" readonly
                 style="font-size: 1.2em; letter-spacing: 2px; font-weight: bold;">
          <button class="btn btn-secondary btn-sm" id="btnCopyPassword"
                  onclick="var btn=this; navigator.clipboard.writeText(document.getElementById('showPasswordValue').value).then(function(){ btn.textContent='✓'; }).catch(function(){ btn.textContent='✗'; }); setTimeout(function(){ btn.textContent='コピー'; }, 1500);">
            コピー
          </button>
        </div>
      </div>
      <div class="alert" style="background: linear-gradient(180deg, rgba(252, 219, 5, 0.15) 0%, rgba(198, 165, 3, 0.15) 100%); border-color: var(--mc-gold-dark); color: var(--mc-gold); margin-top: 12px;">
        ⚠️ パスワード作成後またはリセット後に設定されたパスワードが表示されます。
      </div>
    </div>
    <div class="modal-footer">
      <button class="btn btn-secondary" data-close-modal="showPasswordModal">閉じる</button>
    </div>
  </div>
</div>
```

**設計判断**:
- `readonly` の `<input>` で表示（`.value` で取得可能、コピーしやすい）
- `<input>` のスタイルは `.form-input` のデフォルト（ダークテーマ: 黒背景+白テキスト）をそのまま使用。
  `background: #f8f8f8` のようなライト背景は**使わない**（白テキストが見えなくなるため）。
  `font-size: 1.2em; letter-spacing: 2px; font-weight: bold` のみ追加で視認性を高める
- 「コピー」ボタンは `.value` で取得（`<input>` 要素のため `.textContent` は使わない）
- コピーボタンは `navigator.clipboard.writeText()` のPromiseを `.then()/.catch()` で処理。
  成功時は「✓」、失敗時は「✗」を表示し、1.5秒後に「コピー」に戻る。
  既存コードベースにクリップボードAPIの前例がないため、エラーハンドリングを明示的に実装
- 警告アラートはMinecraftテーマの金色（`--mc-gold`）で統一。
  Bootstrapの `#fff3cd` は**使わない**（ダークテーマと衝突するため）
- モーダルの閉じ方は `data-close-modal` パターン（admin.html L2330-2332のハンドラで自動処理）

---

#### 変更箇所⑨: `btn-info` スタイル追加（Minecraftテーマ準拠）

**追加位置**: `.btn-danger:hover` の定義（L1043-1046）の直後

```css
.btn-info {
  background: linear-gradient(180deg, var(--mc-diamond) 0%, var(--mc-diamond-dark) 100%);
  border-color: #7ff5e5 #1a8a7f #1a8a7f #7ff5e5;
  color: var(--mc-text-white);
  box-shadow: 0 4px 12px rgba(74, 237, 217, 0.3);
}
.btn-info:hover {
  background: linear-gradient(180deg, #6ef0df 0%, var(--mc-diamond) 100%);
  box-shadow: 0 0 20px rgba(74, 237, 217, 0.5);
}
```

**設計判断**:
- 既存の `.btn-primary`（草ブロック緑）、`.btn-secondary`（石ブロック灰）、`.btn-danger`（レッドストーン赤）と同じパターン
- `--mc-diamond` 系カラー（ダイヤモンド青緑）を使用 → `.alert-info` も同色系なのでデザイン統一
- ボーダーはMinecraft風の立体効果（上/左が明るく、下/右が暗い）
- box-shadowも既存パターンに合わせて追加

---

### 3.4 `admin-mobile.html` — HTML（モバイル版）

#### 変更箇所⑩: `btn-info` CSSスタイル追加

**追加位置**: `.btn-danger` の定義（L727-730付近）の直後

```css
.btn-info {
  background: linear-gradient(180deg, var(--mc-diamond) 0%, var(--mc-diamond-dark) 100%);
  border-color: #7ff5e5 #1a8a7f #1a8a7f #7ff5e5;
  color: var(--mc-text-white);
}
.btn-info:hover {
  background: linear-gradient(180deg, #6ef0df 0%, var(--mc-diamond) 100%);
}
```

**注意**: モバイル版にも独自CSSがあるため、デスクトップ版とは別に定義が必要。
（`admin-mobile.html` は `admin.html` のCSSを読み込んでいない — 独立したインラインCSS）

---

#### 変更箇所⑪: パスワード確認モーダルHTML追加

**追加位置**: パスワードリセットモーダル（L1954）の直前

**モバイル版はモーダル閉じボタンに `id` ベースのパターンを使用する**（`data-close-modal` のハンドラがモバイル版にないため）:

```html
<!-- パスワード確認モーダル -->
<div class="modal-overlay" id="showPasswordModal">
  <div class="modal">
    <div class="modal-header">
      <h3 class="modal-title">🔑 パスワード確認</h3>
      <button class="modal-close" id="btnCloseShowPasswordModal">×</button>
    </div>
    <div class="modal-body">
      <div class="alert alert-info">
        「<strong><span id="showPasswordUserName"></span></strong>」のパスワード
      </div>
      <div class="form-group">
        <label class="form-label">現在のパスワード</label>
        <div style="display: flex; align-items: center; gap: 8px;">
          <input type="text" class="form-input" id="showPasswordValue" readonly
                 style="font-size: 1.2em; letter-spacing: 2px; font-weight: bold;">
          <button class="btn btn-secondary btn-sm" id="btnCopyPassword"
                  onclick="var btn=this; navigator.clipboard.writeText(document.getElementById('showPasswordValue').value).then(function(){ btn.textContent='✓'; }).catch(function(){ btn.textContent='✗'; }); setTimeout(function(){ btn.textContent='コピー'; }, 1500);">
            コピー
          </button>
        </div>
      </div>
      <div class="alert" style="background: linear-gradient(180deg, rgba(252, 219, 5, 0.15) 0%, rgba(198, 165, 3, 0.15) 100%); border-color: var(--mc-gold-dark); color: var(--mc-gold); margin-top: 12px;">
        ⚠️ パスワード作成後またはリセット後のパスワードが表示されます。
      </div>
    </div>
    <div class="modal-footer">
      <button class="btn btn-secondary" id="btnCancelShowPasswordModal">閉じる</button>
    </div>
  </div>
</div>
```

**デスクトップ版との差異**:
- `×` ボタン: `data-close-modal="showPasswordModal"` → `id="btnCloseShowPasswordModal"`
- `閉じる` ボタン: `data-close-modal="showPasswordModal"` → `id="btnCancelShowPasswordModal"`
- これはモバイル版の既存パターン（`btnCloseResetPasswordModal` 等）に合わせたもの

---

#### 変更箇所⑫: ユーザー編集モーダル内にPW確認ボタン追加（L1946-1949）

**現在のコード** (L1946-1949):
```html
<div class="modal-footer" style="flex-wrap: wrap; gap: 8px;">
  <button class="btn btn-primary" id="btnEditUserSubmit">保存</button>
  <button class="btn btn-secondary" id="btnCancelEditUserModal">キャンセル</button>
  <button class="btn btn-secondary" id="editUserResetPwBtn" style="background: #e67e22; color: #fff; border: none; width: 100%; margin-top: 4px;">🔐 パスワードリセット</button>
</div>
```

**変更後**:
```html
<div class="modal-footer" style="flex-wrap: wrap; gap: 8px;">
  <button class="btn btn-primary" id="btnEditUserSubmit">保存</button>
  <button class="btn btn-secondary" id="btnCancelEditUserModal">キャンセル</button>
  <button class="btn btn-info" id="editUserShowPwBtn" style="width: 100%; margin-top: 4px;">🔑 パスワード確認</button>
  <button class="btn btn-secondary" id="editUserResetPwBtn" style="background: #e67e22; color: #fff; border: none; width: 100%; margin-top: 4px;">🔐 パスワードリセット</button>
</div>
```

**変更点**: 「🔑 パスワード確認」ボタンを「🔐 パスワードリセット」の直前に追加。

---

#### 変更箇所⑬: イベントハンドラ追加（L2307-2315付近）

**現在のコード** (L2307-2315):
```javascript
document.getElementById('editUserResetPwBtn')?.addEventListener('click', () => {
  const uid = document.getElementById('editUserUid').value;
  AdminPanel.closeModal('editUserModal');
  AdminPanel.showResetPasswordModal(uid);
});

document.getElementById('btnCloseResetPasswordModal')?.addEventListener('click', () => AdminPanel.closeModal('resetPasswordModal'));
document.getElementById('btnCancelResetPasswordModal')?.addEventListener('click', () => AdminPanel.closeModal('resetPasswordModal'));
document.getElementById('btnResetPasswordSubmit')?.addEventListener('click', () => AdminPanel.resetPassword());
```

**変更後（PW確認関連を直前に追加）**:
```javascript
// PW確認ボタン
document.getElementById('editUserShowPwBtn')?.addEventListener('click', () => {
  const uid = document.getElementById('editUserUid').value;
  AdminPanel.closeModal('editUserModal');
  AdminPanel.showPasswordModal(uid);
});

document.getElementById('editUserResetPwBtn')?.addEventListener('click', () => {
  const uid = document.getElementById('editUserUid').value;
  AdminPanel.closeModal('editUserModal');
  AdminPanel.showResetPasswordModal(uid);
});

// PW確認モーダルの閉じるボタン
document.getElementById('btnCloseShowPasswordModal')?.addEventListener('click', () => AdminPanel.closeModal('showPasswordModal'));
document.getElementById('btnCancelShowPasswordModal')?.addEventListener('click', () => AdminPanel.closeModal('showPasswordModal'));

document.getElementById('btnCloseResetPasswordModal')?.addEventListener('click', () => AdminPanel.closeModal('resetPasswordModal'));
document.getElementById('btnCancelResetPasswordModal')?.addEventListener('click', () => AdminPanel.closeModal('resetPasswordModal'));
document.getElementById('btnResetPasswordSubmit')?.addEventListener('click', () => AdminPanel.resetPassword());
```

---

## 4. 既存ユーザーへの対応

### 4.1 問題点

既にFirestoreに存在するユーザーには `currentPassword` フィールドがない。

### 4.2 対応方針

- **マイグレーションは不要**: 既存ユーザーの `currentPassword` は `undefined` となる
- **UI側で対応**: `user.currentPassword || '（未設定）'` のフォールバックにより、未設定の場合は「（未設定）」と表示
- **パスワードリセットで解決**: 管理者がリセットすれば、その時点から `currentPassword` が記録される
- **将来的に**: 新規作成される全ユーザーには自動的にパスワードが記録される

---

## 5. 実装手順（作業順序）

### Step 1: Cloud Functions 修正 & デプロイ（`functions/index.js`）
1. `createUser` 関数: Firestoreへの保存時に `currentPassword` フィールドを追加（1行追加）
2. `createUsers` 関数: 同上（1行追加）
3. `resetPassword` 関数: Auth更新後にFirestoreの `currentPassword` も更新（try-catch付きで7行追加）
4. **デプロイ**: `./deploy.sh functions`

**重要（デプロイ順序）**: Cloud Functionsを**先に**デプロイする。
フロントエンドより先にデプロイすることで、デプロイ完了後に作成されるユーザーには全て `currentPassword` が記録される。
フロントエンドを先にデプロイすると「PW確認」ボタンは表示されるが全て「（未設定）」になり混乱を招く。

### Step 2: デスクトップ版管理画面修正（`admin.html` + `admin.js`）
1. `admin.html`: `btn-info` CSSクラス追加（Minecraftテーマ準拠）
2. `admin.html`: パスワード確認モーダルのHTML追加（`data-close-modal` パターン）
3. `admin.js`: `showPasswordModal()` メソッド追加
4. `admin.js`: `resetPassword()` にキャッシュ更新処理追加
5. `admin.js`: `renderUsersTable()` のボタン変更（「PW」→「PW確認」+「PW変更」）
6. `admin.js`: イベントハンドラに `show-password` アクション追加

### Step 3: モバイル版管理画面修正（`admin-mobile.html`）
1. `btn-info` CSSスタイル追加
2. パスワード確認モーダルのHTML追加（`id` ベースの閉じるボタン）
3. ユーザー編集モーダルに「PW確認」ボタン追加
4. イベントハンドラ追加（PW確認ボタン + モーダル閉じるボタン）

### Step 4: コミット＆プッシュ
```bash
git add functions/index.js admin.js admin.html admin-mobile.html
git commit -m "feat: 管理画面にパスワード確認機能を追加"
git push origin gh-pages
```

### Step 5: 動作確認
1. 管理画面にログイン
2. 新規ユーザーを作成 → Firestoreに `currentPassword` が保存されることを確認
3. ユーザー一覧で「PW確認」ボタンを押す → パスワードが表示されることを確認
4. パスワードリセット → 再度「PW確認」でリセット後のパスワードが表示されることを確認（キャッシュ更新の検証）
5. 既存ユーザー（`currentPassword` 未設定）→「（未設定）」と表示されることを確認
6. 講師アカウントでログイン → 自教室の生徒のみ「PW確認」ボタンが表示されることを確認
7. モバイル版でも同様に動作確認（編集モーダル内のPW確認ボタン、モーダル閉じるボタン）
8. 「コピー」ボタンが正しくクリップボードにコピーされることを確認
9. （API経由のみ）`createUsers` 一括作成でも `currentPassword` が保存されることを確認
   ※ 一括作成のUIは未実装のため、Firebase ConsoleまたはAPI直接呼び出しで確認

---

## 6. 変更サマリー

| ファイル | 変更箇所数 | 変更内容 |
|---------|-----------|---------|
| `functions/index.js` | 3箇所 | `createUser`: 1行追加, `createUsers`: 1行追加, `resetPassword`: 7行追加 |
| `admin.js` | 4箇所 | `showPasswordModal()` 追加, `resetPassword()` キャッシュ更新追加, ボタンラベル変更, イベントハンドラ1行追加 |
| `admin.html` | 2箇所 | パスワード確認モーダルHTML追加, `btn-info` CSS追加（Minecraftテーマ） |
| `admin-mobile.html` | 4箇所 | `btn-info` CSS追加, モーダルHTML追加, PW確認ボタン追加, イベントハンドラ追加 |

**総変更量**: 全ファイル合わせて約90-110行の追加。既存コードへの変更は最小限。

---

## 7. リスク評価

| リスク | 影響度 | 対策 |
|-------|-------|------|
| Firestore更新失敗（リセット時） | 低 | 内側try-catchで囲み、Auth側は正常に更新。ログ出力のみ |
| 既存ユーザーにcurrentPasswordがない | なし | UIで「（未設定）」表示。リセットで解消 |
| パスワードの平文保存 | 低（教育用途） | UIレベルでadmin/teacherのみ閲覧可能。Firestoreルールでは生徒も自分のドキュメントは読めるが、DevTools使用の可能性は実質ゼロ |
| モバイル版の表示崩れ | 低 | 既存のモーダルパターンに従うため崩れにくい |
| デプロイ順序ミス | 中 | Cloud Functionsを必ず先にデプロイ。手順書に明記済み |
| リセット後のキャッシュ不整合 | 中 | フロントエンドでローカルキャッシュも同時更新する対応を追加 |
| ダークテーマとの不整合 | なし（対策済み） | `.form-input` のデフォルトスタイルを使用、警告はテーマ準拠の金色で表示 |
| モバイル版モーダル閉じ不可 | なし（対策済み） | `id` ベースの閉じるハンドラを追加（`data-close-modal` はモバイル版で非対応のため） |
| クリップボードAPI失敗 | 低 | `.then()/.catch()` でPromise処理。HTTPS環境（GitHub Pages）では正常動作。失敗時は「✗」表示 |

---

## 8. 将来の拡張候補（今回は実装しない）

- ユーザー一覧テーブルにパスワード列を追加（クリックで表示/非表示トグル）
- パスワードの自動生成機能
- パスワード変更履歴の記録
- 既存ユーザーへの一括マイグレーションスクリプト
- Firestoreセキュリティルールでのフィールドレベル制御（サブコレクション分離）

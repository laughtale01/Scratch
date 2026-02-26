# 設定画面（settings.html）実装計画書

## 1. 概要

ログインユーザーが自分のパスワードと表示名を変更できる設定画面を新規作成する。

| 項目 | 内容 |
|------|------|
| 新規ファイル | `settings.html` |
| 変更ファイル | `firebase-integration.js` |
| デザイン | Scratch風（index.htmlと統一） |
| レスポンシブ | 1ページでPC・スマホ両対応 |
| 対象ユーザー | 全ロール（admin / teacher / student） |

### 1.1 検証済み事項

| 検証項目 | 結果 |
|---------|------|
| Firebase Auth API互換性（compat SDK 10.7.1） | 全4 API使用可能 ✅ |
| Firestoreセキュリティルール（displayName更新） | 全ロールで許可済み ✅ |
| 既存機能への影響（firebase-integration.js変更3箇所） | 影響なし ✅ |
| Cloud Functions / firestore.rules の変更 | 不要 ✅ |

---

## 2. 画面構成

### 2.1 レイアウト

```
┌──────────────────────────────────────┐
│  ← Scratchに戻る       ⚙ アカウント設定  │  ← ヘッダー
├──────────────────────────────────────┤
│                                      │
│  ┌──────────────────────────────┐    │
│  │ 👤 アカウント情報（読み取り専用）  │    │
│  │                              │    │
│  │  ユーザーID:  tanaka123      │    │
│  │  ロール:      生徒           │    │
│  │  教室:        教室A          │    │
│  └──────────────────────────────┘    │
│                                      │
│  ┌──────────────────────────────┐    │
│  │ ✏️ 表示名の変更                │    │
│  │                              │    │
│  │  現在の表示名:  田中太郎      │    │
│  │  新しい表示名:  [          ]  │    │
│  │              [  変更する  ]   │    │
│  └──────────────────────────────┘    │
│                                      │
│  ┌──────────────────────────────┐    │
│  │ 🔐 パスワードの変更            │    │
│  │                              │    │
│  │  現在のパスワード: [        ]  │    │
│  │  新しいパスワード: [        ]  │    │
│  │  確認（もう一度）:  [        ]  │    │
│  │              [  変更する  ]   │    │
│  └──────────────────────────────┘    │
│                                      │
└──────────────────────────────────────┘
```

### 2.2 セクション詳細

#### セクション1: アカウント情報（読み取り専用）
- ユーザーID: `userData.email` から `@laughtale.local` を除去して表示
- ロール: `admin` → 管理者 / `teacher` → 講師 / `student` → 生徒
- 教室名: Firestoreの `classrooms` コレクションから取得（adminの場合は「-」）

#### セクション2: 表示名の変更
- 現在の表示名を表示
- 新しい表示名を入力するフィールド（1つ、maxlength="20"）
- 「変更する」ボタン
- 成功時: 緑色の成功メッセージを表示（「表示名を変更しました。Scratchページに反映するにはページをリロードしてください。」）
- 失敗時: 赤色のエラーメッセージを表示

#### セクション3: パスワードの変更
- 現在のパスワード（type="password"）
- 新しいパスワード（type="password"、6文字以上）
- 確認（もう一度）（type="password"）
- 「変更する」ボタン
- 成功時: 緑色の成功メッセージを表示（「パスワードを変更しました」）
- 失敗時: 赤色のエラーメッセージを表示

---

## 3. 技術仕様

### 3.1 Firebase SDK読み込み

index.htmlと同じSDKを使用する。

```html
<script src="https://www.gstatic.com/firebasejs/10.7.1/firebase-app-compat.js"></script>
<script src="https://www.gstatic.com/firebasejs/10.7.1/firebase-auth-compat.js"></script>
<script src="https://www.gstatic.com/firebasejs/10.7.1/firebase-firestore-compat.js"></script>
```

※ firebase-storage-compat.js と firebase-functions-compat.js は不要（この画面では使わない）

### 3.2 Firebase初期化

```javascript
const firebaseConfig = {
  apiKey: "AIzaSyBEr2LmctKmBHa_y1Jrx6XQ_6M8vengYhs",
  authDomain: "laughtale-scratch-bcc8a.firebaseapp.com",
  projectId: "laughtale-scratch-bcc8a",
  storageBucket: "laughtale-scratch-bcc8a.firebasestorage.app",
  messagingSenderId: "869020754126",
  appId: "1:869020754126:web:e80beaf67fda60987ff09f",
  measurementId: "G-F8JCCJQ7YB"
};
firebase.initializeApp(firebaseConfig);
const auth = firebase.auth();
const db = firebase.firestore();
```

### 3.3 認証チェック

ページ読み込み時に `auth.onAuthStateChanged` で認証状態を確認。
未ログインの場合は index.html にリダイレクトする。

```javascript
auth.onAuthStateChanged(async (user) => {
  if (user) {
    // Firestoreからユーザー情報を取得
    const userDoc = await db.collection('users').doc(user.uid).get();
    if (userDoc.exists) {
      const userData = userDoc.data();
      displayUserInfo(user, userData);  // 画面にユーザー情報を表示
    }
  } else {
    // 未ログイン → Scratchに戻す
    window.location.href = 'index.html';
  }
});
```

### 3.4 パスワード変更処理

Firebase Auth クライアントSDK の `reauthenticateWithCredential()` と `updatePassword()` を使用する。
Cloud Functionsは不要（ユーザー自身の操作のため）。

```javascript
async function changePassword(currentPassword, newPassword) {
  const user = auth.currentUser;

  // 1. 現在のパスワードで再認証（本人確認）
  const credential = firebase.auth.EmailAuthProvider.credential(
    user.email,
    currentPassword
  );
  await user.reauthenticateWithCredential(credential);

  // 2. 新しいパスワードに更新
  await user.updatePassword(newPassword);
}
```

#### バリデーション

| チェック項目 | 条件 | エラーメッセージ |
|-------------|------|---------------|
| 現在のパスワード | 空でないこと | 「現在のパスワードを入力してください」 |
| 新しいパスワード | 6文字以上 | 「パスワードは6文字以上で入力してください」 |
| 確認入力 | 新パスワードと一致 | 「新しいパスワードが一致しません」 |
| 再認証失敗 | - | 「現在のパスワードが間違っています」 |

#### Firebase Auth エラーコード → 日本語メッセージ

```javascript
const errorMessages = {
  'auth/wrong-password': '現在のパスワードが間違っています',
  'auth/invalid-credential': '現在のパスワードが間違っています',
  'auth/weak-password': 'パスワードは6文字以上で入力してください',
  'auth/too-many-requests': 'しばらく待ってから再試行してください',
  'auth/network-request-failed': 'ネットワークエラーが発生しました',
  'auth/requires-recent-login': 'セッションの有効期限が切れました。ページをリロードして再度ログインしてください',
  'auth/user-token-expired': 'セッションの有効期限が切れました。ページをリロードして再度ログインしてください'
};
```

#### セッションタイムアウト対策（重要）

`auth/requires-recent-login` は長時間操作しなかった場合に発生する。
パスワード変更では `reauthenticateWithCredential()` を先に実行するため通常は発生しないが、
表示名変更時（再認証なし）に発生する可能性がある。

**対策**: エラー発生時にページリロードを促すメッセージを表示する。

```javascript
// 表示名変更でセッション切れが発生した場合
if (error.code === 'auth/requires-recent-login' || error.code === 'auth/user-token-expired') {
  showMessage('displayNameMessage', 'error',
    'セッションの有効期限が切れました。ページをリロードして再度お試しください。');
  return;
}
```

### 3.5 表示名変更処理

Firestoreの `users` コレクションの `displayName` フィールドを直接更新する。
Firestoreルール（firestore.rules 行63-77）で、全ロールが自分自身の displayName を更新可能であることは確認済み（role と classroomId は変更禁止）。

```javascript
async function changeDisplayName(newDisplayName) {
  const user = auth.currentUser;

  // ★ 更新順序: Firestore → Auth（Firestoreが本データのため先に更新）
  // 1. Firestore の users ドキュメントの displayName を更新
  await db.collection('users').doc(user.uid).update({
    displayName: newDisplayName
  });

  // 2. Firebase Auth の displayName を更新
  await user.updateProfile({ displayName: newDisplayName });
}
```

**更新順序の理由**: Firestoreのユーザー情報が管理画面・教室機能の参照元であり、
Auth側のdisplayNameは補助的な役割。Firestoreを先に更新することで、
万が一Auth側の更新が失敗しても本データの整合性を保てる。

#### バリデーション

| チェック項目 | 条件 | エラーメッセージ |
|-------------|------|---------------|
| 新しい表示名 | 空でないこと（半角・全角スペースのみも不可） | 「表示名を入力してください」 |
| 新しい表示名 | 現在と異なること | 「現在と同じ表示名です」 |
| 新しい表示名 | 20文字以内 | 「表示名は20文字以内で入力してください」 |

#### 入力値の正規化

```javascript
// 半角・全角スペースをtrimしてから処理
const trimmed = newDisplayName.replace(/^[\s\u3000]+|[\s\u3000]+$/g, '');
if (!trimmed) {
  showMessage('displayNameMessage', 'error', '表示名を入力してください');
  return;
}
if (trimmed.length > 20) {
  showMessage('displayNameMessage', 'error', '表示名は20文字以内で入力してください');
  return;
}
```

#### Firestoreルールの確認（firestore.rules 行72-76）

```
// 自分自身の更新: role と classroomId は変更禁止
(userId == request.auth.uid &&
  request.resource.data.role == resource.data.role &&
  request.resource.data.classroomId == resource.data.classroomId
)
```

displayName の更新は許可されている。ただし、updateメソッドで role や classroomId を含めないよう注意。
`db.collection('users').doc(uid).update({ displayName: '...' })` は displayName のみの部分更新なので問題なし。

### 3.6 教室名の取得

アカウント情報セクションに教室名を表示するため、classroomId から教室名を取得する。

```javascript
async function getClassroomName(classroomId) {
  if (!classroomId) return '未所属';  // adminユーザー等、教室未割当の場合
  try {
    const doc = await db.collection('classrooms').doc(classroomId).get();
    return doc.exists ? (doc.data().name || '名称未設定') : '不明な教室';
  } catch (e) {
    console.error('教室名の取得に失敗:', e);
    return '取得エラー';
  }
}
```

Firestoreルール（firestore.rules 行91-94）で、全認証ユーザーが自分の教室を読み取り可能であることは確認済み。
**注意**: adminユーザーは `classroomId` が `null` の場合がある。その場合は「未所属」と表示する。

### 3.7 セキュリティ対策

#### XSS防止（必須）

ユーザー入力値（表示名など）をDOMに表示する際は、**必ず `textContent` を使用**する。
`innerHTML` は絶対に使用しない。

```javascript
// ✅ 正しい（XSS安全）
document.getElementById('currentDisplayName').textContent = userData.displayName;
document.getElementById('infoUserId').textContent = userId;
document.getElementById('infoRole').textContent = roleText;
document.getElementById('infoClassroom').textContent = classroomName;

// ❌ 危険（XSS脆弱性）
document.getElementById('currentDisplayName').innerHTML = userData.displayName;
```

**メッセージ表示関数もtextContentを使用する**:
```javascript
function showMessage(elementId, type, message) {
  const el = document.getElementById(elementId);
  el.textContent = message;  // ← innerHTMLではなくtextContent
  el.className = type === 'success' ? 'settings-success' : 'settings-error';
}
```

#### ボタン二重押し防止

処理中はボタンを `disabled` にし、完了後に復帰させる。

```javascript
async function changePassword() {
  const btn = document.getElementById('btnChangePassword');
  btn.disabled = true;
  btn.textContent = '変更中...';
  try {
    // ... 処理 ...
  } finally {
    btn.disabled = false;
    btn.textContent = '変更する';
  }
}
```

---

## 4. デザイン仕様

### 4.1 Scratch風カラーパレット

```css
/* 背景 */
--bg-page: #E5F0FF;         /* Scratch UI Primary */
--bg-card: #FFFFFF;          /* 白 */
--bg-header: #855CD6;        /* Scratch Looks Secondary（紫） */

/* テキスト */
--text-primary: #575E75;     /* Scratch Text Primary */
--text-white: #FFFFFF;
--text-light: #888888;
--text-label: #555555;

/* ボタン */
--btn-primary: #4D97FF;      /* Scratch Motion Primary */
--btn-primary-hover: #3D87EF;
--btn-disabled: #CCCCCC;

/* フォーム */
--border-input: #DDDDDD;
--border-focus: #4D97FF;
--shadow-focus: 0 0 0 2px rgba(77, 151, 255, 0.2);

/* メッセージ */
--color-success: #0FBD8C;    /* Scratch Pen Primary */
--color-error: #E74C3C;
```

### 4.2 フォント

```css
font-family: "Helvetica Neue", Helvetica, Arial, sans-serif;
```

### 4.3 レスポンシブブレークポイント

```css
/* デフォルト: PC（max-width: 480pxのカード） */
.settings-card {
  max-width: 480px;
  margin: 0 auto;
}

/* スマホ: 幅いっぱい */
@media (max-width: 600px) {
  .settings-card {
    max-width: 100%;
    margin: 0 12px;
  }
  .settings-header {
    padding: 12px 16px;
  }
}
```

### 4.4 カードスタイル

```css
.settings-section {
  background: white;
  border-radius: 8px;
  padding: 20px 24px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}
```

### 4.5 フォーム要素スタイル

firebase-integration.js のログインモーダルと統一する。

```css
/* ラベル */
.settings-label {
  display: block;
  margin-bottom: 6px;
  font-size: 14px;
  color: #555;
  font-weight: bold;
}

/* 入力フィールド */
.settings-input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  box-sizing: border-box;
}
.settings-input:focus {
  outline: none;
  border-color: #4d97ff;
  box-shadow: 0 0 0 2px rgba(77, 151, 255, 0.2);
}

/* ボタン */
.settings-btn {
  background: #4d97ff;
  color: white;
  border: none;
  border-radius: 4px;
  padding: 10px 24px;
  font-size: 14px;
  font-weight: bold;
  cursor: pointer;
  transition: background 0.2s;
}
.settings-btn:hover {
  background: #3d87ef;
}
.settings-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}
```

### 4.6 メッセージスタイル

```css
/* 成功メッセージ */
.settings-success {
  color: #0FBD8C;
  background: #e8f8f3;
  border: 1px solid #0FBD8C;
  border-radius: 4px;
  padding: 10px 12px;
  font-size: 13px;
  margin-top: 12px;
}

/* エラーメッセージ */
.settings-error {
  color: #e74c3c;
  background: #fdf0ee;
  border: 1px solid #e74c3c;
  border-radius: 4px;
  padding: 10px 12px;
  font-size: 13px;
  margin-top: 12px;
}
```

---

## 5. firebase-integration.js の変更

### 5.1 メニュー項目の追加

**変更箇所**: 行881（管理画面リンク）の直後、行882（サインアウト）の直前

**現在のコード（行878-884）**:
```javascript
          <div class="firebase-cloud-menu-item" id="firebase-admin-link" style="display: none;" onclick="window.scratchFirebaseUI.openAdmin()">
            <svg ...>...</svg>
            管理画面
          </div>
          <div class="firebase-user-menu-item logout" onclick="window.scratchFirebaseUI.handleLogout()">
            サインアウト
          </div>
```

**追加するコード（管理画面とサインアウトの間）**:
```javascript
          <div class="firebase-cloud-menu-item" id="firebase-settings-link" style="display: none;" onclick="window.scratchFirebaseUI.openSettings()">
            <svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
            アカウント設定
          </div>
```

### 5.2 メニュー表示制御の追加

**変更箇所**: updateUI メソッド内、行1000の直後

**追加するコード**:
```javascript
      // 全ユーザーに設定リンクを表示
      const settingsLink = document.getElementById('firebase-settings-link');
      if (settingsLink) {
        settingsLink.style.display = 'flex';
      }
```

### 5.3 openSettings メソッドの追加

**変更箇所**: 行1013（openAdmin メソッド）の直後

**追加するコード**:
```javascript
  /**
   * 設定画面を開く
   */
  openSettings() {
    window.open('settings.html', '_blank');
  }
```

---

## 6. settings.html のHTML構造

```html
<!DOCTYPE html>
<html lang="ja">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>アカウント設定 - Scratch Education</title>
  <!-- Firebase SDK -->
  <script src="https://www.gstatic.com/firebasejs/10.7.1/firebase-app-compat.js"></script>
  <script src="https://www.gstatic.com/firebasejs/10.7.1/firebase-auth-compat.js"></script>
  <script src="https://www.gstatic.com/firebasejs/10.7.1/firebase-firestore-compat.js"></script>
  <style>
    /* CSS（セクション4のデザイン仕様に準拠） */
  </style>
</head>
<body>
  <!-- ローディング画面 -->
  <div id="loadingScreen">...</div>

  <!-- 未ログイン時のリダイレクト用（通常表示されない） -->

  <!-- メインコンテンツ -->
  <div id="mainContent" style="display: none;">
    <!-- ヘッダー -->
    <header class="settings-header">
      <a href="index.html" class="settings-back">← Scratch に戻る</a>
      <h1>⚙ アカウント設定</h1>
    </header>

    <div class="settings-container">
      <!-- セクション1: アカウント情報 -->
      <div class="settings-section">
        <h2 class="settings-section-title">アカウント情報</h2>
        <div class="settings-info-row">
          <span class="settings-info-label">ユーザーID</span>
          <span class="settings-info-value" id="infoUserId">-</span>
        </div>
        <div class="settings-info-row">
          <span class="settings-info-label">ロール</span>
          <span class="settings-info-value" id="infoRole">-</span>
        </div>
        <div class="settings-info-row">
          <span class="settings-info-label">教室</span>
          <span class="settings-info-value" id="infoClassroom">-</span>
        </div>
      </div>

      <!-- セクション2: 表示名の変更 -->
      <div class="settings-section">
        <h2 class="settings-section-title">表示名の変更</h2>
        <div class="settings-form-group">
          <label class="settings-label">現在の表示名</label>
          <div class="settings-current-value" id="currentDisplayName">-</div>
        </div>
        <div class="settings-form-group">
          <label class="settings-label" for="newDisplayName">新しい表示名</label>
          <input type="text" class="settings-input" id="newDisplayName" placeholder="新しい表示名を入力" maxlength="20">
        </div>
        <button class="settings-btn" id="btnChangeDisplayName" onclick="changeDisplayName()">
          変更する
        </button>
        <div id="displayNameMessage"></div>
      </div>

      <!-- セクション3: パスワードの変更 -->
      <div class="settings-section">
        <h2 class="settings-section-title">パスワードの変更</h2>
        <div class="settings-form-group">
          <label class="settings-label" for="currentPassword">現在のパスワード</label>
          <input type="password" class="settings-input" id="currentPassword" placeholder="現在のパスワードを入力">
        </div>
        <div class="settings-form-group">
          <label class="settings-label" for="newPassword">新しいパスワード</label>
          <input type="password" class="settings-input" id="newPassword" placeholder="6文字以上">
        </div>
        <div class="settings-form-group">
          <label class="settings-label" for="confirmPassword">確認（もう一度）</label>
          <input type="password" class="settings-input" id="confirmPassword" placeholder="新しいパスワードをもう一度入力">
        </div>
        <button class="settings-btn" id="btnChangePassword" onclick="changePassword()">
          変更する
        </button>
        <div id="passwordMessage"></div>
      </div>
    </div>
  </div>

  <script>
    /* JavaScript（セクション3の技術仕様に準拠） */
  </script>
</body>
</html>
```

---

## 7. 処理フロー図

### 7.1 パスワード変更フロー

```
ユーザー操作                    処理
─────────                    ────
[現在のパスワード入力]
[新しいパスワード入力]
[確認パスワード入力]
[変更するボタン押下]
        │
        ▼
  バリデーション
  ├─ 全フィールド入力済み？ ──No──→ エラー表示
  ├─ 新パスワード6文字以上？ ─No──→ エラー表示
  ├─ 新パスワード一致？ ─────No──→ エラー表示
  │
  ▼ Yes
  ボタンをdisabled化（二重押し防止）
        │
        ▼
  reauthenticateWithCredential()
  ├─ 失敗 → エラーメッセージ表示、ボタン復帰
  │  └─ auth/requires-recent-login → リロード促すメッセージ
  │
  ▼ 成功
  updatePassword()
  ├─ 失敗 → エラーメッセージ表示、ボタン復帰
  │
  ▼ 成功
  「パスワードを変更しました」（緑色メッセージ）
  フォームをクリア
  ボタンを復帰
```

### 7.2 表示名変更フロー

```
ユーザー操作                    処理
─────────                    ────
[新しい表示名入力]
[変更するボタン押下]
        │
        ▼
  入力値の正規化（半角・全角スペースtrim）
        │
        ▼
  バリデーション
  ├─ 空でない？ ────────No──→ エラー表示
  ├─ 20文字以内？ ────No──→ エラー表示
  ├─ 現在と異なる？ ───No──→ エラー表示
  │
  ▼ Yes
  ボタンをdisabled化（二重押し防止）
        │
        ▼
  ★ Firestore更新を先に実行（本データのため）
  db.collection('users').doc(uid).update({ displayName })
  ├─ 失敗 → エラーメッセージ表示、ボタン復帰
  │
  ▼ 成功
  user.updateProfile({ displayName })   ← Firebase Auth更新
  ├─ 失敗 → 警告表示（Firestoreは更新済み）
  │
  ▼ 成功
  「表示名を変更しました。Scratchページに反映するにはページをリロードしてください。」
  画面上の「現在の表示名」をtextContentで更新
  ボタンを復帰
```

---

## 8. ファイル変更一覧

| ファイル | 操作 | 変更内容 |
|---------|:----:|---------|
| `settings.html` | **新規作成** | 設定画面全体（HTML + CSS + JavaScript） |
| `firebase-integration.js` | 変更 | メニュー項目「アカウント設定」追加（3箇所） |

### 変更しないファイル

| ファイル | 理由 |
|---------|------|
| `functions/index.js` | パスワード変更はクライアントSDKで完結。Cloud Functions不要 |
| `firestore.rules` | 現行ルールで displayName 更新は許可済み |
| `admin.js` | 管理画面の既存機能に影響なし |
| `gui.js` | Scratch本体に変更なし |
| `index.html` | 変更なし（firebase-integration.jsが動的にメニューを生成） |

---

## 9. firebase-integration.js 変更詳細（行番号指定）

### 変更1: メニューHTML追加（行881の後）

**ファイル**: `firebase-integration.js`
**場所**: 行881（`管理画面</div>`）の直後、行882（`<div class="firebase-user-menu-item logout"`）の直前

**挿入コード**:
```javascript
          <div class="firebase-cloud-menu-item" id="firebase-settings-link" style="display: none;" onclick="window.scratchFirebaseUI.openSettings()">
            <svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
            アカウント設定
          </div>
```

### 変更2: updateUI メソッドに表示制御追加（行1000の後）

**ファイル**: `firebase-integration.js`
**場所**: 行1000（adminLinkの表示制御 `}`）の直後

**挿入コード**:
```javascript
      // 全ユーザーに設定リンクを表示
      const settingsLink = document.getElementById('firebase-settings-link');
      if (settingsLink) {
        settingsLink.style.display = 'flex';
      }
```

### 変更3: openSettings メソッド追加（行1013の後）

**ファイル**: `firebase-integration.js`
**場所**: 行1013（`openAdmin()` メソッドの `}`）の直後

**挿入コード**:
```javascript
  /**
   * 設定画面を開く
   */
  openSettings() {
    window.open('settings.html', '_blank');
  }
```

---

## 10. テスト項目

### 10.1 パスワード変更

| # | テスト項目 | 期待結果 |
|---|----------|---------|
| 1 | 正常系: 正しいパスワードで変更 | 成功メッセージ表示、新パスワードでログイン可能 |
| 2 | 現在のパスワードが間違い | エラーメッセージ表示 |
| 3 | 新パスワードが5文字以下 | バリデーションエラー |
| 4 | 確認パスワードが不一致 | バリデーションエラー |
| 5 | 空フィールド送信 | バリデーションエラー |
| 6 | 変更後に旧パスワードでログイン | ログイン失敗 |
| 7 | 変更後に新パスワードでログイン | ログイン成功 |

### 10.2 表示名変更

| # | テスト項目 | 期待結果 |
|---|----------|---------|
| 1 | 正常系: 新しい表示名で変更 | 成功メッセージ + リロード案内、画面上の表示名が更新 |
| 2 | 空の表示名で送信 | バリデーションエラー |
| 3 | 現在と同じ表示名で送信 | バリデーションエラー |
| 4 | 20文字ちょうどの表示名で送信 | 成功（境界値テスト） |
| 5 | 変更後にScratchページをリロードして確認 | メニューの表示名が更新されている |
| 6 | 変更後に管理画面で確認 | ユーザー一覧の表示名が更新されている |
| 7 | 成功メッセージにリロード案内が含まれることを確認 | 「Scratchページに反映するにはページをリロードしてください」が表示 |

### 10.3 画面遷移・アクセス制御

| # | テスト項目 | 期待結果 |
|---|----------|---------|
| 1 | ログイン状態でメニューから「アカウント設定」 | settings.htmlが新タブで開く |
| 2 | 未ログインで直接settings.htmlにアクセス | index.htmlにリダイレクト |
| 3 | 「Scratchに戻る」リンク | index.htmlに遷移 |
| 4 | PCブラウザで表示 | 中央寄せのカードレイアウト |
| 5 | スマホブラウザで表示 | フル幅のレスポンシブレイアウト |

### 10.4 セキュリティ・エッジケーステスト

| # | テスト項目 | 期待結果 |
|---|----------|---------|
| 1 | 表示名に `<script>alert('xss')</script>` を入力して変更 | スクリプトが実行されずテキストとして表示 |
| 2 | 表示名に全角スペースのみ「　　　」を入力 | 「表示名を入力してください」エラー |
| 3 | 表示名に21文字以上を入力 | 「表示名は20文字以内で入力してください」エラー |
| 4 | 長時間放置後にパスワード変更を試行 | セッション切れの場合は適切なエラーメッセージ |
| 5 | 長時間放置後に表示名変更を試行 | セッション切れの場合は適切なエラーメッセージ |
| 6 | ボタンを連続で素早くクリック | 処理中はボタンがdisabledで二重送信されない |
| 7 | ネットワーク切断中にパスワード変更 | ネットワークエラーメッセージが表示される |
| 8 | adminユーザー（classroomId=null）でアカウント情報表示 | 教室欄に「未所属」と表示 |

### 10.5 ロール別テスト

| # | ロール | テスト項目 | 期待結果 |
|---|--------|----------|---------|
| 1 | admin | パスワード変更 | 成功 |
| 2 | admin | 表示名変更 | 成功 |
| 3 | teacher | パスワード変更 | 成功 |
| 4 | teacher | 表示名変更 | 成功 |
| 5 | student | パスワード変更 | 成功 |
| 6 | student | 表示名変更 | 成功 |

---

## 11. デプロイ手順

```bash
# 1. settings.htmlをコミット
git add settings.html firebase-integration.js
git commit -m "feat: アカウント設定画面を追加（パスワード変更・表示名変更）"

# 2. gh-pagesにプッシュ（GitHub Pagesで自動デプロイ）
git push origin gh-pages

# 3. 確認
# https://laughtale01.github.io/Scratch/settings.html
```

※ Cloud Functions や Firestore ルールの変更は不要。デプロイは gh-pages へのプッシュのみ。

---

## 12. 検証結果と改善対応（2026-02-25追記）

実装前に4つの観点から徹底検証を実施し、以下の改善点を本計画書に反映済み。

### 12.1 検証結果サマリー

| 検証観点 | 結果 | 詳細 |
|---------|------|------|
| Firebase Auth API互換性 | ✅ 問題なし | compat SDK 10.7.1で全4 API使用可能 |
| Firestoreセキュリティルール | ✅ 問題なし | `update({ displayName })` は全ロールで許可済み |
| 既存機能への影響 | ✅ 問題なし | firebase-integration.js変更3箇所、既存機能に影響なし |
| エッジケース・セキュリティ | ⚠️ 7件発見 | すべて本計画書に反映済み |

### 12.2 発見した改善点と対応箇所

| # | 優先度 | 改善点 | 対応内容 | 反映先セクション |
|---|--------|--------|---------|---------------|
| 1 | **高** | セッションタイムアウト (`auth/requires-recent-login`) | エラーコード追加、専用メッセージ表示 | §3.4 |
| 2 | **中** | XSS防止 | `textContent` 使用を必須化、`innerHTML` 禁止 | §3.7（新設） |
| 3 | **中** | 表示名文字数制限 | 20文字制限（バリデーション + maxlength属性） | §3.5, §6 |
| 4 | **中** | Scratchページへの反映通知 | 成功メッセージにリロード案内を追加 | §2.2, §7.2 |
| 5 | **中** | displayName更新順序 | Firestore → Auth の順に変更（本データ優先） | §3.5, §7.2 |
| 6 | **低** | adminの教室名null | null guardと`try/catch`追加 | §3.6 |
| 7 | **低** | 全角スペースのtrim | 正規表現で半角・全角スペースを除去 | §3.5 |

### 12.3 追加実装項目（検証で新規追加）

- **§3.7 セキュリティ対策**（新セクション）: XSS防止、ボタン二重押し防止
- **§10.4 セキュリティ・エッジケーステスト**（8項目追加）
- **エラーコード追加**: `auth/user-token-expired`
- **教室名取得の堅牢化**: `try/catch` + 複数のフォールバック表示

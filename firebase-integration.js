/**
 * Firebase統合モジュール
 * Scratch + Minecraft 教育用プラットフォーム向けクラウドセーブ機能
 *
 * 機能:
 * - ユーザー認証（ログイン/ログアウト）
 * - プロジェクトのクラウド保存/読み込み
 * - バージョン管理
 */

// Firebase設定
const firebaseConfig = {
  apiKey: "AIzaSyBEr2LmctKmBHa_y1Jrx6XQ_6M8vengYhs",
  authDomain: "laughtale-scratch-bcc8a.firebaseapp.com",
  projectId: "laughtale-scratch-bcc8a",
  storageBucket: "laughtale-scratch-bcc8a.firebasestorage.app",
  messagingSenderId: "869020754126",
  appId: "1:869020754126:web:e80beaf67fda60987ff09f",
  measurementId: "G-F8JCCJQ7YB"
};

// Firebase初期化
firebase.initializeApp(firebaseConfig);

// サービス参照
const auth = firebase.auth();
const db = firebase.firestore();
const storage = firebase.storage();

// ドメイン設定（架空ドメイン）
const EMAIL_DOMAIN = '@laughtale.local';

/**
 * ScratchFirebase - Firebaseクラウドセーブ管理クラス
 */
class ScratchFirebase {
  constructor() {
    this.currentUser = null;
    this.userData = null;
    this.onAuthStateChangedCallbacks = [];

    // 認証状態の監視
    auth.onAuthStateChanged(async (user) => {
      this.currentUser = user;
      if (user) {
        await this.loadUserData();
        await this.updateLastLogin();
      } else {
        this.userData = null;
      }
      // コールバックを実行
      this.onAuthStateChangedCallbacks.forEach(cb => cb(user, this.userData));
    });
  }

  /**
   * 認証状態変更時のコールバックを登録
   */
  onAuthStateChanged(callback) {
    this.onAuthStateChangedCallbacks.push(callback);
    // 現在の状態で即座にコールバック
    if (this.currentUser !== undefined) {
      callback(this.currentUser, this.userData);
    }
  }

  /**
   * ユーザーIDからメールアドレスを生成
   */
  userIdToEmail(userId) {
    return userId.toLowerCase() + EMAIL_DOMAIN;
  }

  /**
   * メールアドレスからユーザーIDを抽出
   */
  emailToUserId(email) {
    return email.replace(EMAIL_DOMAIN, '');
  }

  /**
   * ログイン
   */
  async login(userId, password) {
    try {
      const email = this.userIdToEmail(userId);
      const userCredential = await auth.signInWithEmailAndPassword(email, password);
      return { success: true, user: userCredential.user };
    } catch (error) {
      console.error('ログインエラー:', error);
      return { success: false, error: this.getErrorMessage(error.code) };
    }
  }

  /**
   * ログアウト
   */
  async logout() {
    try {
      await auth.signOut();
      return { success: true };
    } catch (error) {
      console.error('ログアウトエラー:', error);
      return { success: false, error: error.message };
    }
  }

  /**
   * ユーザーデータを読み込み
   */
  async loadUserData() {
    if (!this.currentUser) return null;

    try {
      const doc = await db.collection('users').doc(this.currentUser.uid).get();
      if (doc.exists) {
        this.userData = doc.data();
        return this.userData;
      }
      return null;
    } catch (error) {
      console.error('ユーザーデータ読み込みエラー:', error);
      return null;
    }
  }

  /**
   * 最終ログイン日時を更新
   */
  async updateLastLogin() {
    if (!this.currentUser) return;

    try {
      await db.collection('users').doc(this.currentUser.uid).update({
        lastLoginAt: firebase.firestore.FieldValue.serverTimestamp()
      });
    } catch (error) {
      console.error('最終ログイン更新エラー:', error);
    }
  }

  /**
   * ログイン中かどうか
   */
  isLoggedIn() {
    return this.currentUser !== null;
  }

  /**
   * 現在のユーザー情報を取得
   */
  getCurrentUser() {
    if (!this.currentUser || !this.userData) return null;
    return {
      uid: this.currentUser.uid,
      userId: this.emailToUserId(this.currentUser.email),
      displayName: this.userData.displayName,
      role: this.userData.role,
      classroomId: this.userData.classroomId
    };
  }

  /**
   * プロジェクト一覧を取得
   */
  async getProjects() {
    if (!this.currentUser) return [];

    try {
      const snapshot = await db.collection('projects')
        .where('userId', '==', this.currentUser.uid)
        .orderBy('updatedAt', 'desc')
        .get();

      return snapshot.docs.map(doc => ({
        id: doc.id,
        ...doc.data(),
        createdAt: doc.data().createdAt?.toDate(),
        updatedAt: doc.data().updatedAt?.toDate()
      }));
    } catch (error) {
      console.error('プロジェクト一覧取得エラー:', error);
      return [];
    }
  }

  /**
   * プロジェクトを保存（新規）
   */
  async saveNewProject(name, sb3Blob, thumbnailBlob = null) {
    if (!this.currentUser) {
      return { success: false, error: 'ログインが必要です' };
    }

    try {
      // プロジェクトドキュメントを作成
      const projectRef = db.collection('projects').doc();
      const projectId = projectRef.id;

      // Storage にファイルをアップロード
      const storagePath = `projects/${this.currentUser.uid}/${projectId}/current.sb3`;
      const storageRef = storage.ref(storagePath);
      await storageRef.put(sb3Blob);
      const storageUrl = await storageRef.getDownloadURL();

      // サムネイルをアップロード（存在する場合）
      let thumbnailUrl = null;
      if (thumbnailBlob) {
        const thumbPath = `thumbnails/${this.currentUser.uid}/${projectId}.png`;
        const thumbRef = storage.ref(thumbPath);
        await thumbRef.put(thumbnailBlob);
        thumbnailUrl = await thumbRef.getDownloadURL();
      }

      // Firestoreにメタデータを保存
      const projectData = {
        userId: this.currentUser.uid,
        classroomId: this.userData?.classroomId || null,
        name: name,
        description: '',
        thumbnailUrl: thumbnailUrl,
        storageUrl: storageUrl,
        createdAt: firebase.firestore.FieldValue.serverTimestamp(),
        updatedAt: firebase.firestore.FieldValue.serverTimestamp(),
        isSubmitted: false,
        submittedAt: null,
        size: sb3Blob.size
      };

      await projectRef.set(projectData);

      // 保存回数をインクリメント
      await db.collection('users').doc(this.currentUser.uid).update({
        saveCount: firebase.firestore.FieldValue.increment(1)
      });

      return { success: true, projectId: projectId };
    } catch (error) {
      console.error('プロジェクト保存エラー:', error);
      return { success: false, error: error.message };
    }
  }

  /**
   * プロジェクトを上書き保存
   */
  async saveProject(projectId, sb3Blob, thumbnailBlob = null) {
    if (!this.currentUser) {
      return { success: false, error: 'ログインが必要です' };
    }

    try {
      // 現在のバージョンをバックアップ
      await this.createVersionBackup(projectId);

      // Storage にファイルをアップロード
      const storagePath = `projects/${this.currentUser.uid}/${projectId}/current.sb3`;
      const storageRef = storage.ref(storagePath);
      await storageRef.put(sb3Blob);
      const storageUrl = await storageRef.getDownloadURL();

      // サムネイルを更新（存在する場合）
      let thumbnailUrl = null;
      if (thumbnailBlob) {
        const thumbPath = `thumbnails/${this.currentUser.uid}/${projectId}.png`;
        const thumbRef = storage.ref(thumbPath);
        await thumbRef.put(thumbnailBlob);
        thumbnailUrl = await thumbRef.getDownloadURL();
      }

      // Firestoreのメタデータを更新
      const updateData = {
        storageUrl: storageUrl,
        updatedAt: firebase.firestore.FieldValue.serverTimestamp(),
        size: sb3Blob.size
      };
      if (thumbnailUrl) {
        updateData.thumbnailUrl = thumbnailUrl;
      }

      await db.collection('projects').doc(projectId).update(updateData);

      // 保存回数をインクリメント
      await db.collection('users').doc(this.currentUser.uid).update({
        saveCount: firebase.firestore.FieldValue.increment(1)
      });

      return { success: true, projectId: projectId };
    } catch (error) {
      console.error('プロジェクト上書き保存エラー:', error);
      return { success: false, error: error.message };
    }
  }

  /**
   * バージョンバックアップを作成（最大3バージョン保持）
   */
  async createVersionBackup(projectId) {
    try {
      // 現在のファイルを取得
      const currentPath = `projects/${this.currentUser.uid}/${projectId}/current.sb3`;
      const currentRef = storage.ref(currentPath);

      let currentBlob;
      try {
        const url = await currentRef.getDownloadURL();
        const response = await fetch(url);
        currentBlob = await response.blob();
      } catch (e) {
        // ファイルが存在しない場合はスキップ
        return;
      }

      // バージョン履歴を取得
      const versionsSnapshot = await db.collection('projects')
        .doc(projectId)
        .collection('versions')
        .orderBy('createdAt', 'desc')
        .get();

      const versions = versionsSnapshot.docs;

      // 新しいバージョン番号を決定
      const newVersionNum = versions.length > 0
        ? Math.max(...versions.map(v => v.data().versionNum || 0)) + 1
        : 1;

      // バックアップを保存
      const versionPath = `projects/${this.currentUser.uid}/${projectId}/versions/v${newVersionNum}.sb3`;
      const versionRef = storage.ref(versionPath);
      await versionRef.put(currentBlob);
      const versionUrl = await versionRef.getDownloadURL();

      // バージョンメタデータを保存
      await db.collection('projects').doc(projectId).collection('versions').add({
        versionNum: newVersionNum,
        storageUrl: versionUrl,
        createdAt: firebase.firestore.FieldValue.serverTimestamp(),
        size: currentBlob.size
      });

      // 古いバージョンを削除（3つ以上ある場合）
      if (versions.length >= 3) {
        const oldVersions = versions.slice(2); // 最新2つ以外
        for (const oldVersion of oldVersions) {
          const oldData = oldVersion.data();
          // Storageからファイルを削除
          try {
            const oldPath = `projects/${this.currentUser.uid}/${projectId}/versions/v${oldData.versionNum}.sb3`;
            await storage.ref(oldPath).delete();
          } catch (e) {
            console.warn('古いバージョンファイル削除エラー:', e);
          }
          // Firestoreからドキュメントを削除
          await oldVersion.ref.delete();
        }
      }
    } catch (error) {
      console.error('バージョンバックアップエラー:', error);
    }
  }

  /**
   * プロジェクトを読み込み
   */
  async loadProject(projectId) {
    if (!this.currentUser) {
      return { success: false, error: 'ログインが必要です' };
    }

    try {
      // メタデータを取得
      const doc = await db.collection('projects').doc(projectId).get();
      if (!doc.exists) {
        return { success: false, error: 'プロジェクトが見つかりません' };
      }

      const projectData = doc.data();

      // ファイルをダウンロード
      const response = await fetch(projectData.storageUrl);
      const blob = await response.blob();

      return {
        success: true,
        project: {
          id: doc.id,
          name: projectData.name,
          blob: blob
        }
      };
    } catch (error) {
      console.error('プロジェクト読み込みエラー:', error);
      return { success: false, error: error.message };
    }
  }

  /**
   * プロジェクトを削除
   */
  async deleteProject(projectId) {
    if (!this.currentUser) {
      return { success: false, error: 'ログインが必要です' };
    }

    try {
      // Storageからファイルを削除
      const basePath = `projects/${this.currentUser.uid}/${projectId}`;

      // current.sb3を削除
      try {
        await storage.ref(`${basePath}/current.sb3`).delete();
      } catch (e) {}

      // バージョンファイルを削除
      const versionsSnapshot = await db.collection('projects')
        .doc(projectId)
        .collection('versions')
        .get();

      for (const versionDoc of versionsSnapshot.docs) {
        const versionData = versionDoc.data();
        try {
          const versionPath = `${basePath}/versions/v${versionData.versionNum}.sb3`;
          await storage.ref(versionPath).delete();
        } catch (e) {}
        await versionDoc.ref.delete();
      }

      // サムネイルを削除
      try {
        await storage.ref(`thumbnails/${this.currentUser.uid}/${projectId}.png`).delete();
      } catch (e) {}

      // Firestoreからメタデータを削除
      await db.collection('projects').doc(projectId).delete();

      return { success: true };
    } catch (error) {
      console.error('プロジェクト削除エラー:', error);
      return { success: false, error: error.message };
    }
  }

  /**
   * プロジェクト名を変更
   */
  async renameProject(projectId, newName) {
    if (!this.currentUser) {
      return { success: false, error: 'ログインが必要です' };
    }

    try {
      await db.collection('projects').doc(projectId).update({
        name: newName,
        updatedAt: firebase.firestore.FieldValue.serverTimestamp()
      });
      return { success: true };
    } catch (error) {
      console.error('プロジェクト名変更エラー:', error);
      return { success: false, error: error.message };
    }
  }

  /**
   * プロジェクトを提出
   */
  async submitProject(projectId) {
    if (!this.currentUser) {
      return { success: false, error: 'ログインが必要です' };
    }

    try {
      await db.collection('projects').doc(projectId).update({
        isSubmitted: true,
        submittedAt: firebase.firestore.FieldValue.serverTimestamp(),
        updatedAt: firebase.firestore.FieldValue.serverTimestamp()
      });
      return { success: true };
    } catch (error) {
      console.error('プロジェクト提出エラー:', error);
      return { success: false, error: error.message };
    }
  }

  /**
   * プロジェクトの提出を取り消し
   */
  async unsubmitProject(projectId) {
    if (!this.currentUser) {
      return { success: false, error: 'ログインが必要です' };
    }

    try {
      await db.collection('projects').doc(projectId).update({
        isSubmitted: false,
        submittedAt: null,
        updatedAt: firebase.firestore.FieldValue.serverTimestamp()
      });
      return { success: true };
    } catch (error) {
      console.error('提出取り消しエラー:', error);
      return { success: false, error: error.message };
    }
  }

  /**
   * エラーメッセージを日本語に変換
   */
  getErrorMessage(errorCode) {
    const messages = {
      'auth/invalid-email': 'ユーザーIDが無効です',
      'auth/user-disabled': 'このアカウントは無効化されています',
      'auth/user-not-found': 'ユーザーが見つかりません',
      'auth/wrong-password': 'パスワードが間違っています',
      'auth/invalid-credential': 'ユーザーIDまたはパスワードが間違っています',
      'auth/too-many-requests': 'ログイン試行回数が多すぎます。しばらく待ってから再試行してください',
      'auth/network-request-failed': 'ネットワークエラーが発生しました'
    };
    return messages[errorCode] || 'エラーが発生しました';
  }
}

// グローバルインスタンスを作成
window.scratchFirebase = new ScratchFirebase();

/**
 * ScratchFirebaseUI - ログインUI管理クラス
 */
class ScratchFirebaseUI {
  constructor() {
    this.modalContainer = null;
    this.loginButton = null;
    this.userDisplay = null;

    // DOMが読み込まれたらUIを初期化
    if (document.readyState === 'loading') {
      document.addEventListener('DOMContentLoaded', () => this.init());
    } else {
      // 少し遅延させてgui.jsのレンダリングを待つ
      setTimeout(() => this.init(), 1000);
    }
  }

  /**
   * UIを初期化
   */
  init() {
    this.injectStyles();
    this.createModal();
    this.createMenuBarButton();

    // 認証状態の変更を監視
    window.scratchFirebase.onAuthStateChanged((user, userData) => {
      this.updateUI(user, userData);
    });
  }

  /**
   * CSSスタイルを注入
   */
  injectStyles() {
    const style = document.createElement('style');
    style.textContent = `
      /* ログインモーダル */
      .firebase-modal-overlay {
        position: fixed;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background: rgba(0, 0, 0, 0.5);
        display: flex;
        justify-content: center;
        align-items: center;
        z-index: 9999;
        opacity: 0;
        visibility: hidden;
        transition: opacity 0.3s, visibility 0.3s;
      }
      .firebase-modal-overlay.visible {
        opacity: 1;
        visibility: visible;
      }
      .firebase-modal {
        background: white;
        border-radius: 8px;
        padding: 24px;
        width: 320px;
        max-width: 90%;
        box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
        transform: scale(0.9);
        transition: transform 0.3s;
      }
      .firebase-modal-overlay.visible .firebase-modal {
        transform: scale(1);
      }
      .firebase-modal h2 {
        margin: 0 0 20px 0;
        font-size: 20px;
        color: #333;
        text-align: center;
      }
      .firebase-modal-close {
        position: absolute;
        top: 12px;
        right: 12px;
        background: none;
        border: none;
        font-size: 24px;
        cursor: pointer;
        color: #666;
      }
      .firebase-input-group {
        margin-bottom: 16px;
      }
      .firebase-input-group label {
        display: block;
        margin-bottom: 6px;
        font-size: 14px;
        color: #555;
        font-weight: bold;
      }
      .firebase-input-group input {
        width: 100%;
        padding: 10px 12px;
        border: 1px solid #ddd;
        border-radius: 4px;
        font-size: 14px;
        box-sizing: border-box;
      }
      .firebase-input-group input:focus {
        outline: none;
        border-color: #4d97ff;
        box-shadow: 0 0 0 2px rgba(77, 151, 255, 0.2);
      }
      .firebase-error {
        color: #e74c3c;
        font-size: 13px;
        margin-bottom: 12px;
        text-align: center;
        min-height: 20px;
      }
      .firebase-btn {
        width: 100%;
        padding: 12px;
        border: none;
        border-radius: 4px;
        font-size: 14px;
        font-weight: bold;
        cursor: pointer;
        transition: background 0.2s;
      }
      .firebase-btn-primary {
        background: #4d97ff;
        color: white;
      }
      .firebase-btn-primary:hover {
        background: #3d87ef;
      }
      .firebase-btn-primary:disabled {
        background: #ccc;
        cursor: not-allowed;
      }

      /* メニューバーボタン */
      #firebase-menu-container {
        display: flex !important;
        align-items: center !important;
        flex-shrink: 0;
        height: 100%;
      }
      .firebase-menu-button {
        display: flex;
        align-items: center;
        padding: 6px 12px;
        margin-left: 8px;
        border: none;
        border-radius: 4px;
        font-size: 12px;
        font-weight: bold;
        cursor: pointer;
        transition: background 0.2s;
        white-space: nowrap;
      }
      .firebase-login-btn {
        background: #4d97ff;
        color: white;
      }
      .firebase-login-btn:hover {
        background: #3d87ef;
      }
      .firebase-user-btn {
        background: #27ae60;
        color: white;
      }
      .firebase-user-btn:hover {
        background: #219a52;
      }
      .firebase-user-icon {
        width: 20px;
        height: 20px;
        margin-right: 6px;
        background: white;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 12px;
        color: #27ae60;
      }

      /* ユーザーメニュー */
      .firebase-user-menu {
        position: absolute;
        top: 100%;
        right: 0;
        background: white;
        border-radius: 8px;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
        min-width: 180px;
        padding: 8px 0;
        z-index: 1000;
        display: none;
      }
      .firebase-user-menu.visible {
        display: block;
      }
      .firebase-user-menu-item {
        padding: 10px 16px;
        font-size: 13px;
        color: #333;
        cursor: pointer;
        transition: background 0.2s;
      }
      .firebase-user-menu-item:hover {
        background: #f5f5f5;
      }
      .firebase-user-menu-item.logout {
        color: #e74c3c;
        border-top: 1px solid #eee;
        margin-top: 4px;
        padding-top: 12px;
      }
      .firebase-user-info {
        padding: 12px 16px;
        border-bottom: 1px solid #eee;
        margin-bottom: 4px;
      }
      .firebase-user-name {
        font-weight: bold;
        font-size: 14px;
        color: #333;
      }
      .firebase-user-role {
        font-size: 12px;
        color: #888;
        margin-top: 2px;
      }

      /* クラウド保存メニュー項目 */
      .firebase-cloud-menu-item {
        display: flex;
        align-items: center;
        padding: 10px 16px;
        font-size: 13px;
        color: #333;
        cursor: pointer;
        transition: background 0.2s;
      }
      .firebase-cloud-menu-item:hover {
        background: #f5f5f5;
      }
      .firebase-cloud-menu-item svg {
        width: 16px;
        height: 16px;
        margin-right: 8px;
      }
    `;
    document.head.appendChild(style);
  }

  /**
   * ログインモーダルを作成
   */
  createModal() {
    this.modalContainer = document.createElement('div');
    this.modalContainer.className = 'firebase-modal-overlay';
    this.modalContainer.innerHTML = `
      <div class="firebase-modal" style="position: relative;">
        <button class="firebase-modal-close" onclick="window.scratchFirebaseUI.hideModal()">&times;</button>
        <h2>サインイン</h2>
        <form id="firebase-login-form">
          <div class="firebase-input-group">
            <label for="firebase-userid">ユーザーID</label>
            <input type="text" id="firebase-userid" placeholder="例: tanaka123" autocomplete="username" required>
          </div>
          <div class="firebase-input-group">
            <label for="firebase-password">パスワード</label>
            <input type="password" id="firebase-password" placeholder="パスワードを入力" autocomplete="current-password" required>
          </div>
          <div class="firebase-error" id="firebase-error"></div>
          <button type="submit" class="firebase-btn firebase-btn-primary" id="firebase-submit">
            サインイン
          </button>
        </form>
      </div>
    `;
    document.body.appendChild(this.modalContainer);

    // オーバーレイクリックで閉じる
    this.modalContainer.addEventListener('click', (e) => {
      if (e.target === this.modalContainer) {
        this.hideModal();
      }
    });

    // フォーム送信
    const form = document.getElementById('firebase-login-form');
    form.addEventListener('submit', async (e) => {
      e.preventDefault();
      await this.handleLogin();
    });
  }

  /**
   * メニューバーにボタンを追加
   */
  createMenuBarButton() {
    // メニューバーを探す（複数回試行）
    const tryAddButton = () => {
      const menuBar = document.querySelector('[class*="menu-bar_menu-bar"]');
      if (!menuBar) {
        setTimeout(tryAddButton, 500);
        return;
      }

      // 既存のボタンがあれば削除
      const existing = document.getElementById('firebase-menu-container');
      if (existing) existing.remove();

      // コンテナを作成
      const container = document.createElement('div');
      container.id = 'firebase-menu-container';
      container.style.cssText = 'display: flex; align-items: center; position: absolute; right: 12px; top: 50%; transform: translateY(-50%); z-index: 100;';

      // ログインボタン
      this.loginButton = document.createElement('button');
      this.loginButton.className = 'firebase-menu-button firebase-login-btn';
      this.loginButton.innerHTML = 'サインイン';
      this.loginButton.onclick = () => this.showModal();

      // ユーザー表示（ログイン後）
      this.userDisplay = document.createElement('div');
      this.userDisplay.style.cssText = 'display: none; position: relative;';
      this.userDisplay.innerHTML = `
        <button class="firebase-menu-button firebase-user-btn" id="firebase-user-button">
          <span class="firebase-user-icon">U</span>
          <span id="firebase-user-name">ユーザー</span>
        </button>
        <div class="firebase-user-menu" id="firebase-user-menu">
          <div class="firebase-user-info">
            <div class="firebase-user-name" id="firebase-display-name">-</div>
            <div class="firebase-user-role" id="firebase-user-role">-</div>
          </div>
          <div class="firebase-cloud-menu-item" onclick="window.scratchFirebaseUI.showCloudSave()">
            <svg viewBox="0 0 24 24" fill="currentColor"><path d="M19.35 10.04C18.67 6.59 15.64 4 12 4 9.11 4 6.6 5.64 5.35 8.04 2.34 8.36 0 10.91 0 14c0 3.31 2.69 6 6 6h13c2.76 0 5-2.24 5-5 0-2.64-2.05-4.78-4.65-4.96zM14 13v4h-4v-4H7l5-5 5 5h-3z"/></svg>
            クラウドに保存
          </div>
          <div class="firebase-cloud-menu-item" onclick="window.scratchFirebaseUI.showCloudLoad()">
            <svg viewBox="0 0 24 24" fill="currentColor"><path d="M19.35 10.04C18.67 6.59 15.64 4 12 4 9.11 4 6.6 5.64 5.35 8.04 2.34 8.36 0 10.91 0 14c0 3.31 2.69 6 6 6h13c2.76 0 5-2.24 5-5 0-2.64-2.05-4.78-4.65-4.96zM17 13l-5 5-5-5h3V9h4v4h3z"/></svg>
            クラウドから読み込み
          </div>
          <div class="firebase-cloud-menu-item" onclick="window.scratchFirebaseUI.openGallery()">
            <svg viewBox="0 0 24 24" fill="currentColor"><path d="M4 8h4V4H4v4zm6 12h4v-4h-4v4zm-6 0h4v-4H4v4zm0-6h4v-4H4v4zm6 0h4v-4h-4v4zm6-10v4h4V4h-4zm-6 4h4V4h-4v4zm6 6h4v-4h-4v4zm0 6h4v-4h-4v4z"/></svg>
            ギャラリー
          </div>
          <div class="firebase-cloud-menu-item" id="firebase-admin-link" style="display: none;" onclick="window.scratchFirebaseUI.openAdmin()">
            <svg viewBox="0 0 24 24" fill="currentColor"><path d="M19.14 12.94c.04-.31.06-.63.06-.94 0-.31-.02-.63-.06-.94l2.03-1.58c.18-.14.23-.41.12-.61l-1.92-3.32c-.12-.22-.37-.29-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54c-.04-.24-.24-.41-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58c-.04.31-.06.63-.06.94s.02.63.06.94l-2.03 1.58c-.18.14-.23.41-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58zM12 15.6c-1.98 0-3.6-1.62-3.6-3.6s1.62-3.6 3.6-3.6 3.6 1.62 3.6 3.6-1.62 3.6-3.6 3.6z"/></svg>
            管理画面
          </div>
          <div class="firebase-cloud-menu-item" id="firebase-settings-link" style="display: none;" onclick="window.scratchFirebaseUI.openSettings()">
            <svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
            アカウント設定
          </div>
          <div class="firebase-user-menu-item logout" onclick="window.scratchFirebaseUI.handleLogout()">
            サインアウト
          </div>
        </div>
      `;

      container.appendChild(this.loginButton);
      container.appendChild(this.userDisplay);

      // メニューバーにposition: relativeを設定して配置
      menuBar.style.position = 'relative';
      menuBar.appendChild(container);

      // ユーザーメニューの表示/非表示
      const userButton = document.getElementById('firebase-user-button');
      const userMenu = document.getElementById('firebase-user-menu');
      if (userButton && userMenu) {
        userButton.onclick = (e) => {
          e.stopPropagation();
          userMenu.classList.toggle('visible');
        };
        document.addEventListener('click', () => {
          userMenu.classList.remove('visible');
        });
      }
    };

    tryAddButton();
  }

  /**
   * ログインモーダルを表示
   */
  showModal() {
    this.modalContainer.classList.add('visible');
    document.getElementById('firebase-userid').focus();
  }

  /**
   * ログインモーダルを非表示
   */
  hideModal() {
    this.modalContainer.classList.remove('visible');
    document.getElementById('firebase-error').textContent = '';
    document.getElementById('firebase-login-form').reset();
  }

  /**
   * ログイン処理
   */
  async handleLogin() {
    const userId = document.getElementById('firebase-userid').value.trim();
    const password = document.getElementById('firebase-password').value;
    const errorEl = document.getElementById('firebase-error');
    const submitBtn = document.getElementById('firebase-submit');

    if (!userId || !password) {
      errorEl.textContent = 'ユーザーIDとパスワードを入力してください';
      return;
    }

    submitBtn.disabled = true;
    submitBtn.textContent = 'サインイン中...';
    errorEl.textContent = '';

    const result = await window.scratchFirebase.login(userId, password);

    if (result.success) {
      this.hideModal();
    } else {
      errorEl.textContent = result.error;
    }

    submitBtn.disabled = false;
    submitBtn.textContent = 'サインイン';
  }

  /**
   * ログアウト処理
   */
  async handleLogout() {
    await window.scratchFirebase.logout();
    // メニューを閉じる
    const userMenu = document.getElementById('firebase-user-menu');
    if (userMenu) userMenu.classList.remove('visible');
  }

  /**
   * UIを更新
   */
  updateUI(user, userData) {
    if (!this.loginButton || !this.userDisplay) return;

    if (user && userData) {
      // ログイン中
      this.loginButton.style.display = 'none';
      this.userDisplay.style.display = 'block';

      const roleLabels = {
        admin: '管理者',
        teacher: '講師',
        student: '生徒'
      };

      const nameEl = document.getElementById('firebase-user-name');
      const displayNameEl = document.getElementById('firebase-display-name');
      const roleEl = document.getElementById('firebase-user-role');
      const iconEl = this.userDisplay.querySelector('.firebase-user-icon');

      if (nameEl) nameEl.textContent = userData.displayName || 'ユーザー';
      if (displayNameEl) displayNameEl.textContent = userData.displayName || 'ユーザー';
      if (roleEl) roleEl.textContent = roleLabels[userData.role] || userData.role;
      if (iconEl) iconEl.textContent = (userData.displayName || 'U')[0].toUpperCase();

      // 管理者・講師のみ管理画面リンクを表示
      const adminLink = document.getElementById('firebase-admin-link');
      if (adminLink) {
        adminLink.style.display = (userData.role === 'admin' || userData.role === 'teacher') ? 'flex' : 'none';
      }

      // 全ユーザーにアカウント設定リンクを表示
      const settingsLink = document.getElementById('firebase-settings-link');
      if (settingsLink) {
        settingsLink.style.display = 'flex';
      }
    } else {
      // 未ログイン
      this.loginButton.style.display = 'flex';
      this.userDisplay.style.display = 'none';
    }
  }

  /**
   * 管理画面を開く
   */
  openAdmin() {
    window.open('admin.html', '_blank');
  }

  /**
   * 設定画面を開く
   */
  openSettings() {
    window.open('settings.html', '_blank');
  }

  /**
   * ギャラリーを開く
   */
  openGallery() {
    window.open('gallery.html', '_blank');
  }

  /**
   * クラウド保存ダイアログを表示
   */
  showCloudSave() {
    // ユーザーメニューを閉じる
    const userMenu = document.getElementById('firebase-user-menu');
    if (userMenu) userMenu.classList.remove('visible');

    // 現在のプロジェクト名を取得
    const titleEl = document.querySelector('[class*="project-title-input"]');
    const projectName = titleEl?.value || 'プロジェクト';

    // 保存ダイアログを表示
    const name = prompt('プロジェクト名を入力してください:', projectName);
    if (!name) return;

    this.saveCurrentProject(name);
  }

  /**
   * 現在のプロジェクトを保存
   */
  async saveCurrentProject(name) {
    // Scratch VMからプロジェクトを取得
    const vm = window.vm;
    if (!vm) {
      alert('プロジェクトを取得できませんでした');
      return;
    }

    try {
      // .sb3ファイルを生成
      const blob = await vm.saveProjectSb3();

      // クラウドに保存
      const result = await window.scratchFirebase.saveNewProject(name, blob);

      if (result.success) {
        alert('クラウドに保存しました');
      } else {
        alert('保存に失敗しました: ' + result.error);
      }
    } catch (error) {
      console.error('保存エラー:', error);
      alert('保存中にエラーが発生しました');
    }
  }

  /**
   * クラウド読み込みダイアログを表示
   */
  async showCloudLoad() {
    // ユーザーメニューを閉じる
    const userMenu = document.getElementById('firebase-user-menu');
    if (userMenu) userMenu.classList.remove('visible');

    // プロジェクト一覧を取得
    const projects = await window.scratchFirebase.getProjects();

    if (projects.length === 0) {
      alert('保存されているプロジェクトがありません');
      return;
    }

    // モーダルダイアログを作成
    const modal = document.createElement('div');
    modal.id = 'firebase-load-modal';
    modal.style.cssText = `
      position: fixed; top: 0; left: 0; right: 0; bottom: 0;
      background: rgba(0,0,0,0.6); display: flex;
      justify-content: center; align-items: center; z-index: 10000;
    `;

    const formatDate = (timestamp) => {
      if (!timestamp) return '-';
      const date = timestamp.toDate ? timestamp.toDate() : new Date(timestamp);
      return date.toLocaleDateString('ja-JP', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
    };

    const projectItems = projects.map(p => `
      <div class="firebase-project-item" data-id="${p.id}" style="
        padding: 12px; border: 1px solid #ddd; border-radius: 8px;
        margin-bottom: 8px; display: flex; justify-content: space-between;
        align-items: center; background: white;
      ">
        <div style="flex: 1;">
          <div style="font-weight: bold; margin-bottom: 4px;">${this.escapeHtml(p.name)}</div>
          <div style="font-size: 12px; color: #666;">
            ${formatDate(p.updatedAt)}
            ${p.isSubmitted ? '<span style="color: #4caf50; margin-left: 8px;">✓ 提出済み</span>' : ''}
          </div>
        </div>
        <div style="display: flex; gap: 8px;">
          <button onclick="window.scratchFirebaseUI.loadProjectToScratch('${p.id}'); document.getElementById('firebase-load-modal').remove();"
            style="padding: 6px 12px; background: #4c97ff; color: white; border: none; border-radius: 4px; cursor: pointer;">
            読込
          </button>
          <button onclick="window.scratchFirebaseUI.toggleSubmit('${p.id}', ${!p.isSubmitted})"
            style="padding: 6px 12px; background: ${p.isSubmitted ? '#ff9800' : '#4caf50'}; color: white; border: none; border-radius: 4px; cursor: pointer;">
            ${p.isSubmitted ? '取消' : '提出'}
          </button>
          <button onclick="window.scratchFirebaseUI.deleteProject('${p.id}')"
            style="padding: 6px 12px; background: #f44336; color: white; border: none; border-radius: 4px; cursor: pointer;">
            削除
          </button>
        </div>
      </div>
    `).join('');

    modal.innerHTML = `
      <div style="background: white; border-radius: 12px; width: 90%; max-width: 600px; max-height: 80vh; overflow: hidden; display: flex; flex-direction: column;">
        <div style="padding: 16px 20px; border-bottom: 1px solid #e0e0e0; display: flex; justify-content: space-between; align-items: center;">
          <h3 style="margin: 0; font-size: 18px;">クラウドから読み込み</h3>
          <button onclick="document.getElementById('firebase-load-modal').remove()" style="background: none; border: none; font-size: 24px; cursor: pointer; color: #666;">&times;</button>
        </div>
        <div style="padding: 16px 20px; overflow-y: auto; flex: 1;">
          ${projectItems}
        </div>
      </div>
    `;

    modal.onclick = (e) => {
      if (e.target === modal) modal.remove();
    };

    document.body.appendChild(modal);
  }

  /**
   * HTMLエスケープ
   */
  escapeHtml(str) {
    if (!str) return '';
    return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
  }

  /**
   * プロジェクト提出/取消
   */
  async toggleSubmit(projectId, submit) {
    let result;
    if (submit) {
      if (!confirm('このプロジェクトを先生に提出しますか？')) return;
      result = await window.scratchFirebase.submitProject(projectId);
    } else {
      if (!confirm('提出を取り消しますか？')) return;
      result = await window.scratchFirebase.unsubmitProject(projectId);
    }

    if (result.success) {
      alert(submit ? 'プロジェクトを提出しました' : '提出を取り消しました');
      // モーダルを閉じて再表示
      document.getElementById('firebase-load-modal')?.remove();
      this.showCloudLoad();
    } else {
      alert('エラー: ' + result.error);
    }
  }

  /**
   * プロジェクト削除
   */
  async deleteProject(projectId) {
    if (!confirm('このプロジェクトを削除しますか？\nこの操作は取り消せません。')) return;

    const result = await window.scratchFirebase.deleteProject(projectId);
    if (result.success) {
      alert('プロジェクトを削除しました');
      // モーダルを閉じて再表示
      document.getElementById('firebase-load-modal')?.remove();
      this.showCloudLoad();
    } else {
      alert('削除に失敗しました: ' + result.error);
    }
  }

  /**
   * プロジェクトをScratchに読み込み
   */
  async loadProjectToScratch(projectId) {
    const result = await window.scratchFirebase.loadProject(projectId);

    if (!result.success) {
      alert('読み込みに失敗しました: ' + result.error);
      return;
    }

    try {
      const vm = window.vm;
      if (!vm) {
        alert('Scratch VMが見つかりません');
        return;
      }

      // ArrayBufferに変換
      const arrayBuffer = await result.project.blob.arrayBuffer();

      // プロジェクトを読み込み
      await vm.loadProject(arrayBuffer);

      // プロジェクト名を設定
      const titleEl = document.querySelector('[class*="project-title-input"]');
      if (titleEl) {
        titleEl.value = result.project.name;
        // inputイベントを発火
        titleEl.dispatchEvent(new Event('input', { bubbles: true }));
      }

      alert('プロジェクトを読み込みました');
    } catch (error) {
      console.error('読み込みエラー:', error);
      alert('読み込み中にエラーが発生しました');
    }
  }
}

// グローバルUIインスタンスを作成
window.scratchFirebaseUI = new ScratchFirebaseUI();

console.log('Firebase統合モジュールが読み込まれました');

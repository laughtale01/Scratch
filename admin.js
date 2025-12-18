/**
 * 管理画面 JavaScript
 *
 * ユーザー、教室、プロジェクトの管理機能を提供
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
const auth = firebase.auth();
const db = firebase.firestore();
const storage = firebase.storage();
const functions = firebase.functions();

// Cloud Functions のリージョン設定（asia-northeast1）
const functionsAsia = firebase.app().functions('asia-northeast1');
const cloudFunctions = {
  createUser: functionsAsia.httpsCallable('createUser'),
  resetPassword: functionsAsia.httpsCallable('resetPassword'),
  deleteUser: functionsAsia.httpsCallable('deleteUser'),
  createUsers: functionsAsia.httpsCallable('createUsers')
};

/**
 * 管理画面クラス
 */
class AdminPanel {
  // 現在のユーザー情報
  static currentUser = null;
  static currentUserData = null;

  // データキャッシュ
  static users = [];
  static classrooms = [];
  static projects = [];

  /**
   * 初期化
   */
  static init() {
    console.log('AdminPanel: 初期化開始');

    // 認証状態の監視
    auth.onAuthStateChanged(async (user) => {
      if (user) {
        console.log('AdminPanel: ユーザーログイン検出', user.email);
        await this.onUserLoggedIn(user);
      } else {
        console.log('AdminPanel: 未ログイン');
        this.showAccessDenied();
      }
    });

    // タブ切り替えイベント
    document.querySelectorAll('.tab-btn').forEach(btn => {
      btn.addEventListener('click', (e) => {
        this.switchTab(e.target.dataset.tab);
      });
    });
  }

  /**
   * ユーザーログイン時の処理
   */
  static async onUserLoggedIn(user) {
    try {
      // ユーザー情報を取得
      const userDoc = await db.collection('users').doc(user.uid).get();

      if (!userDoc.exists) {
        console.error('AdminPanel: ユーザー文書が存在しません');
        this.showAccessDenied();
        return;
      }

      const userData = userDoc.data();
      console.log('AdminPanel: ユーザー情報取得', userData);

      // 管理者または講師のみアクセス可能
      if (userData.role !== 'admin' && userData.role !== 'teacher') {
        console.warn('AdminPanel: 権限なし', userData.role);
        this.showAccessDenied();
        return;
      }

      this.currentUser = user;
      this.currentUserData = userData;

      // UI更新
      this.hideLoading();
      this.showMainContent();
      this.updateHeader();

      // 権限に応じてUIを調整
      this.adjustUIForRole();

      // データ読み込み
      await this.loadAllData();

    } catch (error) {
      console.error('AdminPanel: ログイン処理エラー', error);
      this.showAccessDenied();
    }
  }

  /**
   * ローディング画面を非表示
   */
  static hideLoading() {
    document.getElementById('loadingScreen').classList.add('hidden');
  }

  /**
   * アクセス拒否画面を表示
   */
  static showAccessDenied() {
    this.hideLoading();
    document.getElementById('accessDenied').classList.add('show');
  }

  /**
   * メインコンテンツを表示
   */
  static showMainContent() {
    document.getElementById('header').style.display = 'flex';
    document.getElementById('mainContainer').classList.add('show');
  }

  /**
   * ヘッダー情報を更新
   */
  static updateHeader() {
    document.getElementById('headerUserName').textContent = this.currentUserData.displayName || 'ユーザー';

    const roleLabels = {
      admin: '管理者',
      teacher: '講師',
      student: '生徒'
    };
    document.getElementById('headerUserRole').textContent = roleLabels[this.currentUserData.role] || this.currentUserData.role;
  }

  /**
   * 権限に応じてUIを調整
   */
  static adjustUIForRole() {
    const isAdmin = this.currentUserData.role === 'admin';

    // 管理者のみがユーザー作成・教室作成可能
    if (!isAdmin) {
      const createUserBtn = document.getElementById('btnCreateUser');
      if (createUserBtn) createUserBtn.style.display = 'none';

      const createClassroomBtn = document.getElementById('btnCreateClassroom');
      if (createClassroomBtn) createClassroomBtn.style.display = 'none';
    }
  }

  /**
   * 全データ読み込み
   */
  static async loadAllData() {
    try {
      await Promise.all([
        this.loadUsers(),
        this.loadClassrooms(),
        this.loadProjects()
      ]);

      this.updateStats();
      this.updateFilters();

    } catch (error) {
      console.error('AdminPanel: データ読み込みエラー', error);
    }
  }

  /**
   * ユーザー一覧を読み込み
   */
  static async loadUsers() {
    try {
      let query = db.collection('users');

      // 講師は自分の教室のユーザーのみ表示
      // ただし、全ユーザーの閲覧は許可（仕様書より）

      const snapshot = await query.orderBy('displayName').get();
      this.users = [];

      snapshot.forEach(doc => {
        this.users.push({
          id: doc.id,
          ...doc.data()
        });
      });

      console.log('AdminPanel: ユーザー読み込み完了', this.users.length);
      this.renderUsersTable();

    } catch (error) {
      console.error('AdminPanel: ユーザー読み込みエラー', error);
    }
  }

  /**
   * 教室一覧を読み込み
   */
  static async loadClassrooms() {
    try {
      const snapshot = await db.collection('classrooms').orderBy('name').get();
      this.classrooms = [];

      snapshot.forEach(doc => {
        this.classrooms.push({
          id: doc.id,
          ...doc.data()
        });
      });

      console.log('AdminPanel: 教室読み込み完了', this.classrooms.length);
      this.renderClassroomsTable();

    } catch (error) {
      console.error('AdminPanel: 教室読み込みエラー', error);
    }
  }

  /**
   * プロジェクト一覧を読み込み
   */
  static async loadProjects() {
    try {
      const snapshot = await db.collection('projects').orderBy('updatedAt', 'desc').get();
      this.projects = [];

      snapshot.forEach(doc => {
        this.projects.push({
          id: doc.id,
          ...doc.data()
        });
      });

      console.log('AdminPanel: プロジェクト読み込み完了', this.projects.length);
      this.renderProjectsGrid();

    } catch (error) {
      console.error('AdminPanel: プロジェクト読み込みエラー', error);
    }
  }

  /**
   * 統計情報を更新
   */
  static updateStats() {
    document.getElementById('statUsers').textContent = this.users.length;
    document.getElementById('statClassrooms').textContent = this.classrooms.length;
    document.getElementById('statProjects').textContent = this.projects.length;
    document.getElementById('statSubmitted').textContent = this.projects.filter(p => p.isSubmitted).length;
  }

  /**
   * フィルターのドロップダウンを更新
   */
  static updateFilters() {
    // 教室フィルター
    const classroomOptions = '<option value="">すべての教室</option>' +
      this.classrooms.map(c => `<option value="${c.id}">${c.name}</option>`).join('');

    document.getElementById('filterUserClassroom').innerHTML = classroomOptions;
    document.getElementById('filterProjectClassroom').innerHTML = classroomOptions;

    // ユーザーフィルター（プロジェクト用）
    const userOptions = '<option value="">すべてのユーザー</option>' +
      this.users.map(u => `<option value="${u.id}">${u.displayName}</option>`).join('');

    document.getElementById('filterProjectUser').innerHTML = userOptions;

    // 新規ユーザー用の教室選択
    const classroomSelectOptions = '<option value="">教室を選択...</option>' +
      this.classrooms.map(c => `<option value="${c.id}">${c.name}</option>`).join('');

    document.getElementById('newUserClassroom').innerHTML = classroomSelectOptions;
    document.getElementById('editUserClassroom').innerHTML = classroomSelectOptions;

    // 講師選択（教室用）
    const teachers = this.users.filter(u => u.role === 'teacher' || u.role === 'admin');
    const teacherOptions = '<option value="">講師を選択...</option>' +
      teachers.map(t => `<option value="${t.id}">${t.displayName}</option>`).join('');

    document.getElementById('newClassroomTeacher').innerHTML = teacherOptions;
    document.getElementById('editClassroomTeacher').innerHTML = teacherOptions;
  }

  /**
   * ユーザーテーブルを描画
   */
  static renderUsersTable(filteredUsers = null) {
    const users = filteredUsers || this.users;
    const tbody = document.getElementById('usersTable');

    if (users.length === 0) {
      tbody.innerHTML = '<tr><td colspan="7" class="empty-state">ユーザーがいません</td></tr>';
      return;
    }

    const roleLabels = { admin: '管理者', teacher: '講師', student: '生徒' };
    const roleBadges = { admin: 'badge-admin', teacher: 'badge-teacher', student: 'badge-student' };

    tbody.innerHTML = users.map(user => {
      const classroom = this.classrooms.find(c => c.id === user.classroomId);
      const classroomName = classroom ? classroom.name : '-';
      const lastLogin = user.lastLoginAt ? this.formatDate(user.lastLoginAt.toDate()) : '-';

      // 編集・削除ボタンの表示制御
      const isAdmin = this.currentUserData.role === 'admin';
      const canEdit = isAdmin || (this.currentUserData.role === 'teacher' && user.classroomId === this.currentUserData.classroomId);
      const canDelete = isAdmin && user.id !== this.currentUser.uid;
      const canResetPassword = isAdmin || (this.currentUserData.role === 'teacher' && user.classroomId === this.currentUserData.classroomId);

      return `
        <tr>
          <td>${this.escapeHtml(user.email?.replace('@laughtale.local', '') || '-')}</td>
          <td>${this.escapeHtml(user.displayName || '-')}</td>
          <td><span class="badge ${roleBadges[user.role]}">${roleLabels[user.role] || user.role}</span></td>
          <td>${this.escapeHtml(classroomName)}</td>
          <td>${lastLogin}</td>
          <td>${user.saveCount || 0}</td>
          <td>
            <div class="btn-group">
              ${canEdit ? `<button class="btn btn-secondary btn-sm" onclick="AdminPanel.showEditUserModal('${user.id}')">編集</button>` : ''}
              ${canResetPassword ? `<button class="btn btn-secondary btn-sm" onclick="AdminPanel.showResetPasswordModal('${user.id}')">PW</button>` : ''}
              ${canDelete ? `<button class="btn btn-danger btn-sm" onclick="AdminPanel.confirmDeleteUser('${user.id}')">削除</button>` : ''}
            </div>
          </td>
        </tr>
      `;
    }).join('');
  }

  /**
   * 教室テーブルを描画
   */
  static renderClassroomsTable() {
    const tbody = document.getElementById('classroomsTable');

    if (this.classrooms.length === 0) {
      tbody.innerHTML = '<tr><td colspan="6" class="empty-state">教室がありません</td></tr>';
      return;
    }

    const isAdmin = this.currentUserData.role === 'admin';

    tbody.innerHTML = this.classrooms.map(classroom => {
      const teacher = this.users.find(u => u.id === classroom.teacherId);
      const teacherName = teacher ? teacher.displayName : '-';
      const studentCount = this.users.filter(u => u.classroomId === classroom.id && u.role === 'student').length;
      const projectCount = this.projects.filter(p => p.classroomId === classroom.id).length;
      const createdAt = classroom.createdAt ? this.formatDate(classroom.createdAt.toDate()) : '-';

      return `
        <tr>
          <td>${this.escapeHtml(classroom.name)}</td>
          <td>${this.escapeHtml(teacherName)}</td>
          <td>${studentCount}</td>
          <td>${projectCount}</td>
          <td>${createdAt}</td>
          <td>
            <div class="btn-group">
              <button class="btn btn-secondary btn-sm" onclick="AdminPanel.showEditClassroomModal('${classroom.id}')">編集</button>
              ${isAdmin ? `<button class="btn btn-danger btn-sm" onclick="AdminPanel.confirmDeleteClassroom('${classroom.id}')">削除</button>` : ''}
            </div>
          </td>
        </tr>
      `;
    }).join('');
  }

  /**
   * プロジェクトグリッドを描画
   */
  static renderProjectsGrid(filteredProjects = null) {
    const projects = filteredProjects || this.projects;
    const grid = document.getElementById('projectsGrid');

    if (projects.length === 0) {
      grid.innerHTML = '<div class="empty-state">プロジェクトがありません</div>';
      return;
    }

    grid.innerHTML = projects.map(project => {
      const user = this.users.find(u => u.id === project.userId);
      const userName = user ? user.displayName : '不明';
      const updatedAt = project.updatedAt ? this.formatDate(project.updatedAt.toDate()) : '-';
      const statusBadge = project.isSubmitted
        ? '<span class="badge badge-submitted">提出済み</span>'
        : '<span class="badge badge-not-submitted">未提出</span>';

      const thumbnailUrl = project.thumbnailUrl || '';

      return `
        <div class="project-card" onclick="AdminPanel.showProjectDetail('${project.id}')">
          <div class="project-thumbnail">
            <img src="${this.escapeHtml(thumbnailUrl)}" alt="${this.escapeHtml(project.name)}" onerror="this.parentElement.innerHTML='📁'">
          </div>
          <div class="project-info">
            <div class="project-name">${this.escapeHtml(project.name)}</div>
            <div class="project-meta">
              ${this.escapeHtml(userName)} • ${updatedAt}
            </div>
            <div style="margin-top: 8px;">${statusBadge}</div>
          </div>
        </div>
      `;
    }).join('');
  }

  /**
   * タブ切り替え
   */
  static switchTab(tabName) {
    // タブボタンの状態更新
    document.querySelectorAll('.tab-btn').forEach(btn => {
      btn.classList.toggle('active', btn.dataset.tab === tabName);
    });

    // タブコンテンツの表示切り替え
    document.querySelectorAll('.tab-content').forEach(content => {
      content.classList.toggle('active', content.id === `tab-${tabName}`);
    });
  }

  /**
   * ユーザーフィルタリング
   */
  static filterUsers() {
    const role = document.getElementById('filterUserRole').value;
    const classroom = document.getElementById('filterUserClassroom').value;
    const search = document.getElementById('filterUserSearch').value.toLowerCase();

    let filtered = this.users;

    if (role) {
      filtered = filtered.filter(u => u.role === role);
    }

    if (classroom) {
      filtered = filtered.filter(u => u.classroomId === classroom);
    }

    if (search) {
      filtered = filtered.filter(u =>
        (u.displayName || '').toLowerCase().includes(search) ||
        (u.email || '').toLowerCase().includes(search)
      );
    }

    this.renderUsersTable(filtered);
  }

  /**
   * プロジェクトフィルタリング
   */
  static filterProjects() {
    const classroom = document.getElementById('filterProjectClassroom').value;
    const userId = document.getElementById('filterProjectUser').value;
    const status = document.getElementById('filterProjectStatus').value;
    const search = document.getElementById('filterProjectSearch').value.toLowerCase();

    let filtered = this.projects;

    if (classroom) {
      filtered = filtered.filter(p => p.classroomId === classroom);
    }

    if (userId) {
      filtered = filtered.filter(p => p.userId === userId);
    }

    if (status === 'submitted') {
      filtered = filtered.filter(p => p.isSubmitted);
    } else if (status === 'not-submitted') {
      filtered = filtered.filter(p => !p.isSubmitted);
    }

    if (search) {
      filtered = filtered.filter(p =>
        (p.name || '').toLowerCase().includes(search)
      );
    }

    this.renderProjectsGrid(filtered);
  }

  // ==================== ユーザー管理 ====================

  /**
   * ユーザー作成モーダルを表示
   */
  static showCreateUserModal() {
    document.getElementById('newUserId').value = '';
    document.getElementById('newUserDisplayName').value = '';
    document.getElementById('newUserPassword').value = '';
    document.getElementById('newUserRole').value = 'student';
    document.getElementById('newUserClassroom').value = '';
    document.getElementById('newUserClassroomGroup').style.display = 'block';

    this.showModal('createUserModal');
  }

  /**
   * 新規ユーザーのロール変更時
   */
  static onNewUserRoleChange() {
    const role = document.getElementById('newUserRole').value;
    document.getElementById('newUserClassroomGroup').style.display =
      role === 'admin' ? 'none' : 'block';
  }

  /**
   * ユーザー作成
   */
  static async createUser() {
    const userId = document.getElementById('newUserId').value.trim();
    const displayName = document.getElementById('newUserDisplayName').value.trim();
    const password = document.getElementById('newUserPassword').value;
    const role = document.getElementById('newUserRole').value;
    const classroomId = document.getElementById('newUserClassroom').value;

    // バリデーション
    if (!userId) {
      alert('ユーザーIDを入力してください');
      return;
    }

    if (!displayName) {
      alert('表示名を入力してください');
      return;
    }

    if (!password || password.length < 6) {
      alert('パスワードは6文字以上で入力してください');
      return;
    }

    if (role !== 'admin' && !classroomId) {
      alert('教室を選択してください');
      return;
    }

    try {
      // Cloud Function を呼び出してユーザー作成
      const result = await cloudFunctions.createUser({
        userId,
        displayName,
        password,
        role,
        classroomId: role === 'admin' ? null : classroomId
      });

      console.log('AdminPanel: ユーザー作成完了', result.data);
      alert('ユーザーを作成しました: ' + userId);

      this.closeModal('createUserModal');
      await this.loadUsers();
      this.updateFilters();
      this.updateStats();

    } catch (error) {
      console.error('AdminPanel: ユーザー作成エラー', error);

      // Cloud Functions が未デプロイの場合
      if (error.code === 'functions/not-found' || error.message.includes('not found')) {
        alert('Cloud Functionsがデプロイされていません。\nFirebase Consoleから手動で作成するか、Cloud Functionsをデプロイしてください。');
      } else {
        alert('ユーザーの作成に失敗しました: ' + (error.message || error));
      }
    }
  }

  /**
   * ユーザー編集モーダルを表示
   */
  static showEditUserModal(userId) {
    const user = this.users.find(u => u.id === userId);
    if (!user) return;

    document.getElementById('editUserUid').value = userId;
    document.getElementById('editUserId').value = user.email?.replace('@laughtale.local', '') || '';
    document.getElementById('editUserDisplayName').value = user.displayName || '';
    document.getElementById('editUserRole').value = user.role || 'student';
    document.getElementById('editUserClassroom').value = user.classroomId || '';

    const role = user.role;
    document.getElementById('editUserClassroomGroup').style.display =
      role === 'admin' ? 'none' : 'block';

    this.showModal('editUserModal');
  }

  /**
   * 編集ユーザーのロール変更時
   */
  static onEditUserRoleChange() {
    const role = document.getElementById('editUserRole').value;
    document.getElementById('editUserClassroomGroup').style.display =
      role === 'admin' ? 'none' : 'block';
  }

  /**
   * ユーザー更新
   */
  static async updateUser() {
    const userId = document.getElementById('editUserUid').value;
    const displayName = document.getElementById('editUserDisplayName').value.trim();
    const role = document.getElementById('editUserRole').value;
    const classroomId = document.getElementById('editUserClassroom').value;

    if (!displayName) {
      alert('表示名を入力してください');
      return;
    }

    if (role !== 'admin' && !classroomId) {
      alert('教室を選択してください');
      return;
    }

    try {
      const updateData = {
        displayName,
        role,
        updatedAt: firebase.firestore.FieldValue.serverTimestamp()
      };

      if (role === 'admin') {
        updateData.classroomId = null;
      } else {
        updateData.classroomId = classroomId;
      }

      await db.collection('users').doc(userId).update(updateData);

      console.log('AdminPanel: ユーザー更新完了');
      this.closeModal('editUserModal');
      await this.loadUsers();
      this.updateFilters();

    } catch (error) {
      console.error('AdminPanel: ユーザー更新エラー', error);
      alert('ユーザーの更新に失敗しました: ' + error.message);
    }
  }

  /**
   * パスワードリセットモーダルを表示
   */
  static showResetPasswordModal(userId) {
    const user = this.users.find(u => u.id === userId);
    if (!user) return;

    document.getElementById('resetPasswordUid').value = userId;
    document.getElementById('resetPasswordUserName').textContent = user.displayName || user.email;
    document.getElementById('newPassword').value = '';

    this.showModal('resetPasswordModal');
  }

  /**
   * パスワードリセット
   */
  static async resetPassword() {
    const targetUid = document.getElementById('resetPasswordUid').value;
    const newPassword = document.getElementById('newPassword').value;

    if (!newPassword || newPassword.length < 6) {
      alert('パスワードは6文字以上で入力してください');
      return;
    }

    try {
      // Cloud Function を呼び出してパスワードリセット
      const result = await cloudFunctions.resetPassword({
        targetUid,
        newPassword
      });

      console.log('AdminPanel: パスワードリセット完了', result.data);
      alert('パスワードをリセットしました');

      this.closeModal('resetPasswordModal');

    } catch (error) {
      console.error('AdminPanel: パスワードリセットエラー', error);

      // Cloud Functions が未デプロイの場合
      if (error.code === 'functions/not-found' || error.message.includes('not found')) {
        alert('Cloud Functionsがデプロイされていません。\nFirebase Consoleから手動で変更してください。');
      } else {
        alert('パスワードのリセットに失敗しました: ' + (error.message || error));
      }
    }
  }

  /**
   * ユーザー削除確認
   */
  static confirmDeleteUser(userId) {
    const user = this.users.find(u => u.id === userId);
    if (!user) return;

    document.getElementById('confirmDeleteMessage').textContent =
      `ユーザー「${user.displayName}」を削除しますか？\nこの操作は取り消せません。`;

    document.getElementById('confirmDeleteBtn').onclick = () => this.deleteUser(userId);

    this.showModal('confirmDeleteModal');
  }

  /**
   * ユーザー削除
   */
  static async deleteUser(targetUid) {
    try {
      // Cloud Function を呼び出してユーザー削除
      const result = await cloudFunctions.deleteUser({
        targetUid
      });

      console.log('AdminPanel: ユーザー削除完了', result.data);

      if (result.data.projectCount > 0) {
        alert(`ユーザーを削除しました。\n（${result.data.projectCount}件のプロジェクトが残っています）`);
      } else {
        alert('ユーザーを削除しました');
      }

      this.closeModal('confirmDeleteModal');
      await this.loadUsers();
      this.updateFilters();
      this.updateStats();

    } catch (error) {
      console.error('AdminPanel: ユーザー削除エラー', error);

      // Cloud Functions が未デプロイの場合
      if (error.code === 'functions/not-found' || error.message.includes('not found')) {
        alert('Cloud Functionsがデプロイされていません。\nFirebase Consoleから手動で削除してください。');
      } else {
        alert('ユーザーの削除に失敗しました: ' + (error.message || error));
      }
    }
  }

  // ==================== 教室管理 ====================

  /**
   * 教室作成モーダルを表示
   */
  static showCreateClassroomModal() {
    document.getElementById('newClassroomName').value = '';
    document.getElementById('newClassroomTeacher').value = '';

    this.showModal('createClassroomModal');
  }

  /**
   * 教室作成
   */
  static async createClassroom() {
    const name = document.getElementById('newClassroomName').value.trim();
    const teacherId = document.getElementById('newClassroomTeacher').value;

    if (!name) {
      alert('教室名を入力してください');
      return;
    }

    try {
      await db.collection('classrooms').add({
        name,
        teacherId: teacherId || null,
        createdAt: firebase.firestore.FieldValue.serverTimestamp()
      });

      console.log('AdminPanel: 教室作成完了');
      this.closeModal('createClassroomModal');
      await this.loadClassrooms();
      this.updateFilters();
      this.updateStats();

    } catch (error) {
      console.error('AdminPanel: 教室作成エラー', error);
      alert('教室の作成に失敗しました: ' + error.message);
    }
  }

  /**
   * 教室編集モーダルを表示
   */
  static showEditClassroomModal(classroomId) {
    const classroom = this.classrooms.find(c => c.id === classroomId);
    if (!classroom) return;

    document.getElementById('editClassroomId').value = classroomId;
    document.getElementById('editClassroomName').value = classroom.name || '';
    document.getElementById('editClassroomTeacher').value = classroom.teacherId || '';

    this.showModal('editClassroomModal');
  }

  /**
   * 教室更新
   */
  static async updateClassroom() {
    const classroomId = document.getElementById('editClassroomId').value;
    const name = document.getElementById('editClassroomName').value.trim();
    const teacherId = document.getElementById('editClassroomTeacher').value;

    if (!name) {
      alert('教室名を入力してください');
      return;
    }

    try {
      await db.collection('classrooms').doc(classroomId).update({
        name,
        teacherId: teacherId || null,
        updatedAt: firebase.firestore.FieldValue.serverTimestamp()
      });

      console.log('AdminPanel: 教室更新完了');
      this.closeModal('editClassroomModal');
      await this.loadClassrooms();
      this.updateFilters();

    } catch (error) {
      console.error('AdminPanel: 教室更新エラー', error);
      alert('教室の更新に失敗しました: ' + error.message);
    }
  }

  /**
   * 教室削除確認
   */
  static confirmDeleteClassroom(classroomId) {
    const classroom = this.classrooms.find(c => c.id === classroomId);
    if (!classroom) return;

    const studentCount = this.users.filter(u => u.classroomId === classroomId && u.role === 'student').length;

    if (studentCount > 0) {
      alert(`この教室には${studentCount}名の生徒がいます。\n先に生徒を他の教室に移動してください。`);
      return;
    }

    document.getElementById('confirmDeleteMessage').textContent =
      `教室「${classroom.name}」を削除しますか？\nこの操作は取り消せません。`;

    document.getElementById('confirmDeleteBtn').onclick = () => this.deleteClassroom(classroomId);

    this.showModal('confirmDeleteModal');
  }

  /**
   * 教室削除
   */
  static async deleteClassroom(classroomId) {
    try {
      await db.collection('classrooms').doc(classroomId).delete();

      console.log('AdminPanel: 教室削除完了');
      this.closeModal('confirmDeleteModal');
      await this.loadClassrooms();
      this.updateFilters();
      this.updateStats();

    } catch (error) {
      console.error('AdminPanel: 教室削除エラー', error);
      alert('教室の削除に失敗しました: ' + error.message);
    }
  }

  // ==================== プロジェクト管理 ====================

  /**
   * プロジェクト詳細を表示
   */
  static async showProjectDetail(projectId) {
    const project = this.projects.find(p => p.id === projectId);
    if (!project) return;

    const user = this.users.find(u => u.id === project.userId);
    const classroom = this.classrooms.find(c => c.id === project.classroomId);

    // バージョン履歴を取得
    let versions = [];
    try {
      const versionsSnapshot = await db.collection('projects').doc(projectId)
        .collection('versions').orderBy('createdAt', 'desc').get();

      versionsSnapshot.forEach(doc => {
        versions.push({ id: doc.id, ...doc.data() });
      });
    } catch (error) {
      console.error('AdminPanel: バージョン取得エラー', error);
    }

    const content = document.getElementById('projectDetailContent');

    content.innerHTML = `
      <div style="display: flex; gap: 20px; margin-bottom: 20px;">
        <div style="flex: 0 0 200px;">
          <img src="${project.thumbnailUrl || ''}"
               alt="${this.escapeHtml(project.name)}"
               style="width: 100%; border-radius: 8px; border: 1px solid #ddd;"
               onerror="this.style.display='none'; this.parentElement.innerHTML='<div style=\\'display:flex;justify-content:center;align-items:center;height:150px;background:#f0f0f0;border-radius:8px;font-size:48px;\\'>📁</div>'">
        </div>
        <div style="flex: 1;">
          <h3 style="margin-bottom: 8px;">${this.escapeHtml(project.name)}</h3>
          <p style="color: #666; margin-bottom: 16px;">${this.escapeHtml(project.description || '説明なし')}</p>
          <table style="font-size: 14px;">
            <tr><td style="color: #666; padding-right: 16px;">作成者:</td><td>${this.escapeHtml(user?.displayName || '不明')}</td></tr>
            <tr><td style="color: #666; padding-right: 16px;">教室:</td><td>${this.escapeHtml(classroom?.name || '-')}</td></tr>
            <tr><td style="color: #666; padding-right: 16px;">作成日:</td><td>${project.createdAt ? this.formatDate(project.createdAt.toDate()) : '-'}</td></tr>
            <tr><td style="color: #666; padding-right: 16px;">更新日:</td><td>${project.updatedAt ? this.formatDate(project.updatedAt.toDate()) : '-'}</td></tr>
            <tr><td style="color: #666; padding-right: 16px;">状態:</td><td>${project.isSubmitted ? '<span class="badge badge-submitted">提出済み</span>' : '<span class="badge badge-not-submitted">未提出</span>'}</td></tr>
          </table>
        </div>
      </div>

      <div style="margin-bottom: 16px;">
        <a href="${project.storageUrl}" target="_blank" class="btn btn-primary" download>
          ダウンロード (.sb3)
        </a>
        <button class="btn btn-danger" style="margin-left: 8px;" onclick="AdminPanel.confirmDeleteProject('${projectId}')">
          削除
        </button>
      </div>

      <h4 style="margin-bottom: 12px;">バージョン履歴</h4>
      ${versions.length > 0 ? `
        <table style="font-size: 14px;">
          <thead>
            <tr>
              <th>バージョン</th>
              <th>日時</th>
              <th>サイズ</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            ${versions.map((v, i) => `
              <tr>
                <td>v${versions.length - i}</td>
                <td>${v.createdAt ? this.formatDate(v.createdAt.toDate()) : '-'}</td>
                <td>${v.size ? this.formatSize(v.size) : '-'}</td>
                <td>
                  <a href="${v.storageUrl}" target="_blank" class="btn btn-secondary btn-sm" download>DL</a>
                  ${i > 0 ? `<button class="btn btn-secondary btn-sm" onclick="AdminPanel.rollbackProject('${projectId}', '${v.id}')">復元</button>` : ''}
                </td>
              </tr>
            `).join('')}
          </tbody>
        </table>
      ` : '<p style="color: #666;">バージョン履歴はありません</p>'}
    `;

    this.showModal('projectDetailModal');
  }

  /**
   * プロジェクトをロールバック
   */
  static async rollbackProject(projectId, versionId) {
    if (!confirm('このバージョンに復元しますか？')) return;

    try {
      // バージョンのURLを取得
      const versionDoc = await db.collection('projects').doc(projectId)
        .collection('versions').doc(versionId).get();

      if (!versionDoc.exists) {
        alert('バージョンが見つかりません');
        return;
      }

      const versionData = versionDoc.data();

      // 現在のURLを更新
      await db.collection('projects').doc(projectId).update({
        storageUrl: versionData.storageUrl,
        updatedAt: firebase.firestore.FieldValue.serverTimestamp()
      });

      alert('プロジェクトを復元しました');
      this.closeModal('projectDetailModal');
      await this.loadProjects();

    } catch (error) {
      console.error('AdminPanel: ロールバックエラー', error);
      alert('復元に失敗しました: ' + error.message);
    }
  }

  /**
   * プロジェクト削除確認
   */
  static confirmDeleteProject(projectId) {
    const project = this.projects.find(p => p.id === projectId);
    if (!project) return;

    document.getElementById('confirmDeleteMessage').textContent =
      `プロジェクト「${project.name}」を削除しますか？\nこの操作は取り消せません。`;

    document.getElementById('confirmDeleteBtn').onclick = () => this.deleteProject(projectId);

    this.closeModal('projectDetailModal');
    this.showModal('confirmDeleteModal');
  }

  /**
   * プロジェクト削除
   */
  static async deleteProject(projectId) {
    try {
      const project = this.projects.find(p => p.id === projectId);

      // Storageからファイルを削除
      if (project?.storageUrl) {
        try {
          const storageRef = storage.refFromURL(project.storageUrl);
          await storageRef.delete();
        } catch (e) {
          console.warn('AdminPanel: Storageファイル削除エラー', e);
        }
      }

      // サムネイルを削除
      if (project?.thumbnailUrl) {
        try {
          const thumbRef = storage.refFromURL(project.thumbnailUrl);
          await thumbRef.delete();
        } catch (e) {
          console.warn('AdminPanel: サムネイル削除エラー', e);
        }
      }

      // バージョンを削除
      const versionsSnapshot = await db.collection('projects').doc(projectId)
        .collection('versions').get();

      for (const doc of versionsSnapshot.docs) {
        const version = doc.data();
        if (version.storageUrl) {
          try {
            const versionRef = storage.refFromURL(version.storageUrl);
            await versionRef.delete();
          } catch (e) {
            console.warn('AdminPanel: バージョンファイル削除エラー', e);
          }
        }
        await doc.ref.delete();
      }

      // プロジェクトドキュメントを削除
      await db.collection('projects').doc(projectId).delete();

      console.log('AdminPanel: プロジェクト削除完了');
      this.closeModal('confirmDeleteModal');
      await this.loadProjects();
      this.updateStats();

    } catch (error) {
      console.error('AdminPanel: プロジェクト削除エラー', error);
      alert('プロジェクトの削除に失敗しました: ' + error.message);
    }
  }

  // ==================== ユーティリティ ====================

  /**
   * モーダルを表示
   */
  static showModal(modalId) {
    document.getElementById(modalId).classList.add('show');
  }

  /**
   * モーダルを閉じる
   */
  static closeModal(modalId) {
    document.getElementById(modalId).classList.remove('show');
  }

  /**
   * ログアウト
   */
  static async logout() {
    try {
      await auth.signOut();
      location.href = 'index.html';
    } catch (error) {
      console.error('AdminPanel: ログアウトエラー', error);
    }
  }

  /**
   * 日付フォーマット
   */
  static formatDate(date) {
    if (!date) return '-';

    const options = {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    };

    return new Intl.DateTimeFormat('ja-JP', options).format(date);
  }

  /**
   * ファイルサイズフォーマット
   */
  static formatSize(bytes) {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  }

  /**
   * HTMLエスケープ
   */
  static escapeHtml(str) {
    if (!str) return '';
    return str
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }
}

// モーダル外クリックで閉じる
document.addEventListener('click', (e) => {
  if (e.target.classList.contains('modal-overlay')) {
    e.target.classList.remove('show');
  }
});

// 初期化
document.addEventListener('DOMContentLoaded', () => {
  AdminPanel.init();
});

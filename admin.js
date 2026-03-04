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
  createUsers: functionsAsia.httpsCallable('createUsers'),
  auditUserDocumentCoverage: functionsAsia.httpsCallable('auditUserDocumentCoverage')
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
  static pendingDeleteAction = null;
  static lastCoverageAudit = null;

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

    const confirmDeleteBtn = document.getElementById('confirmDeleteBtn');
    if (confirmDeleteBtn) {
      confirmDeleteBtn.addEventListener('click', async () => {
        if (typeof this.pendingDeleteAction !== 'function') return;
        const action = this.pendingDeleteAction;
        this.pendingDeleteAction = null;
        await action();
      });
    }
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
    const isTeacher = this.currentUserData.role === 'teacher';

    // 講師はユーザー作成可能（生徒のみ）、教室作成は不可
    const createUserBtn = document.getElementById('btnCreateUser');
    const createClassroomBtn = document.getElementById('btnCreateClassroom');

    if (isAdmin) {
      // 管理者は全て可能
      if (createUserBtn) createUserBtn.style.display = '';
      if (createClassroomBtn) createClassroomBtn.style.display = '';
    } else if (isTeacher) {
      // 講師は生徒作成のみ可能
      if (createUserBtn) {
        createUserBtn.style.display = '';
        createUserBtn.textContent = '+ 新規生徒';
      }
      if (createClassroomBtn) createClassroomBtn.style.display = 'none';

      // 教室管理タブを非表示（講師は自分の教室のみなので管理不要）
      const classroomsTab = document.querySelector('[data-tab="classrooms"]');
      if (classroomsTab) classroomsTab.style.display = 'none';
    } else {
      // その他は全て非表示
      if (createUserBtn) createUserBtn.style.display = 'none';
      if (createClassroomBtn) createClassroomBtn.style.display = 'none';
    }

    const auditBtn = document.getElementById('btnRunUserCoverageAudit');
    if (auditBtn) {
      auditBtn.style.display = isAdmin ? '' : 'none';
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
      const isAdmin = this.currentUserData.role === 'admin';
      const isTeacher = this.currentUserData.role === 'teacher';
      const myClassroomId = this.currentUserData.classroomId;

      let query = db.collection('users');

      // 講師は自分の教室のユーザーのみ取得
      if (isTeacher && myClassroomId) {
        query = query.where('classroomId', '==', myClassroomId);
      }

      const snapshot = await query.orderBy('displayName').get();
      this.users = [];

      snapshot.forEach(doc => {
        const userData = doc.data();
        // 講師は自分自身も表示（classroomIdがnullの講師も含む）
        if (isTeacher && doc.id === this.currentUser.uid) {
          this.users.push({
            id: doc.id,
            ...userData
          });
        } else if (!isTeacher || userData.classroomId === myClassroomId) {
          this.users.push({
            id: doc.id,
            ...userData
          });
        }
      });

      // 講師の場合、自分自身を追加（既に含まれていない場合）
      if (isTeacher) {
        const selfExists = this.users.some(u => u.id === this.currentUser.uid);
        if (!selfExists) {
          this.users.unshift({
            id: this.currentUser.uid,
            ...this.currentUserData
          });
        }
      }

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
      const isTeacher = this.currentUserData.role === 'teacher';
      const myClassroomId = this.currentUserData.classroomId;

      const snapshot = await db.collection('classrooms').orderBy('name').get();
      this.classrooms = [];

      snapshot.forEach(doc => {
        // 講師は自分の教室のみ
        if (isTeacher && myClassroomId) {
          if (doc.id === myClassroomId) {
            this.classrooms.push({
              id: doc.id,
              ...doc.data()
            });
          }
        } else {
          this.classrooms.push({
            id: doc.id,
            ...doc.data()
          });
        }
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
      const isTeacher = this.currentUserData.role === 'teacher';
      const myClassroomId = this.currentUserData.classroomId;

      let query = db.collection('projects');

      // 講師は自分の教室のプロジェクトのみ
      if (isTeacher && myClassroomId) {
        query = query.where('classroomId', '==', myClassroomId);
      }

      const snapshot = await query.orderBy('updatedAt', 'desc').get();
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
   * 欠損ユーザー監査結果を描画
   */
  static renderCoverageAuditResult(result, error = null) {
    const output = document.getElementById('userCoverageAuditOutput');
    if (!output) return;

    if (error) {
      output.textContent = `監査エラー: ${error.message || error}`;
      output.style.color = '#ff6b6b';
      return;
    }

    if (!result) {
      output.textContent = '未実行';
      output.style.color = '';
      return;
    }

    output.style.color = '';
    output.textContent = [
      `監査時刻: ${result.auditedAt || '-'}`,
      `対象Authユーザー数: ${result.totals?.authUsersAudited ?? '-'}`,
      `users欠損件数: ${result.totals?.missingUserDocs ?? '-'}`,
      `カバレッジ: ${result.totals?.coverageRate ?? '-'}%`,
      `欠損サンプル件数: ${(result.sampleMissing || []).length}`
    ].join('\n');
  }

  /**
   * 欠損ユーザー監査を実行（管理者のみ）
   */
  static async runUserCoverageAudit() {
    const btn = document.getElementById('btnRunUserCoverageAudit');
    if (btn) {
      btn.disabled = true;
      btn.textContent = '監査中...';
    }

    try {
      const res = await cloudFunctions.auditUserDocumentCoverage({
        maxUsers: 2000,
        onlyLocalDomain: true,
        sampleSize: 50
      });
      this.lastCoverageAudit = res.data || null;
      this.renderCoverageAuditResult(this.lastCoverageAudit);
    } catch (error) {
      console.error('AdminPanel: ユーザー文書監査エラー', error);
      this.renderCoverageAuditResult(null, error);
    } finally {
      if (btn) {
        btn.disabled = false;
        btn.textContent = '欠損ユーザー監査を実行';
      }
    }
  }

  /**
   * フィルターのドロップダウンを更新
   */
  static updateFilters() {
    const setOptions = (selectId, defaultLabel, items) => {
      const select = document.getElementById(selectId);
      if (!select) return;

      select.innerHTML = '';

      const defaultOption = document.createElement('option');
      defaultOption.value = '';
      defaultOption.textContent = defaultLabel;
      select.appendChild(defaultOption);

      items.forEach(item => {
        const option = document.createElement('option');
        option.value = item.value;
        option.textContent = item.label;
        select.appendChild(option);
      });
    };

    const classroomItems = this.classrooms.map(c => ({
      value: c.id,
      label: c.name || '(名称未設定)'
    }));

    setOptions('filterUserClassroom', 'すべての教室', classroomItems);
    setOptions('filterProjectClassroom', 'すべての教室', classroomItems);
    setOptions('newUserClassroom', '教室を選択...', classroomItems);
    setOptions('editUserClassroom', '教室を選択...', classroomItems);

    const userItems = this.users.map(u => ({
      value: u.id,
      label: u.displayName || '(名称未設定)'
    }));
    setOptions('filterProjectUser', 'すべてのユーザー', userItems);

    const teacherItems = this.users
      .filter(u => u.role === 'teacher' || u.role === 'admin')
      .map(t => ({
        value: t.id,
        label: t.displayName || '(名称未設定)'
      }));

    setOptions('newClassroomTeacher', '講師を選択...', teacherItems);
    setOptions('editClassroomTeacher', '講師を選択...', teacherItems);
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
      const isTeacher = this.currentUserData.role === 'teacher';
      const isSameClassroom = user.classroomId === this.currentUserData.classroomId;
      const isStudentInMyClassroom = isTeacher && isSameClassroom && user.role === 'student';

      // 編集: 管理者は全員、講師は自教室の生徒のみ
      const canEdit = isAdmin || isStudentInMyClassroom;
      // 削除: 管理者は全員（自分以外）、講師は自教室の生徒のみ
      const canDelete = (isAdmin && user.id !== this.currentUser.uid) || isStudentInMyClassroom;
      // パスワードリセット: 管理者は全員、講師は自教室の生徒のみ
      const canResetPassword = isAdmin || isStudentInMyClassroom;

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
              ${canEdit ? `<button class="btn btn-secondary btn-sm" data-admin-action="edit-user" data-user-id="${this.escapeHtml(user.id)}">編集</button>` : ''}
              ${canResetPassword ? `<button class="btn btn-secondary btn-sm" data-admin-action="reset-user-password" data-user-id="${this.escapeHtml(user.id)}">PW</button>` : ''}
              ${canDelete ? `<button class="btn btn-danger btn-sm" data-admin-action="delete-user" data-user-id="${this.escapeHtml(user.id)}">削除</button>` : ''}
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
              <button class="btn btn-secondary btn-sm" data-admin-action="edit-classroom" data-classroom-id="${this.escapeHtml(classroom.id)}">編集</button>
              ${isAdmin ? `<button class="btn btn-danger btn-sm" data-admin-action="delete-classroom" data-classroom-id="${this.escapeHtml(classroom.id)}">削除</button>` : ''}
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

      const thumbnailUrl = this.safeUrl(project.thumbnailUrl || '');

      return `
        <div class="project-card" data-admin-action="open-project-detail" data-project-id="${this.escapeHtml(project.id)}">
          <div class="project-thumbnail">
            <img class="admin-thumb-img" src="${thumbnailUrl}" alt="${this.escapeHtml(project.name)}">
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
    const isTeacher = this.currentUserData.role === 'teacher';
    const myClassroomId = this.currentUserData.classroomId;

    document.getElementById('newUserId').value = '';
    document.getElementById('newUserDisplayName').value = '';
    document.getElementById('newUserPassword').value = '';

    const roleSelect = document.getElementById('newUserRole');
    const classroomSelect = document.getElementById('newUserClassroom');
    const classroomGroup = document.getElementById('newUserClassroomGroup');

    if (isTeacher) {
      // 講師は生徒のみ作成可能
      roleSelect.value = 'student';
      roleSelect.disabled = true;

      // 教室は自動的に自分の教室を選択
      classroomSelect.value = myClassroomId;
      classroomGroup.style.display = 'none'; // 教室選択を非表示
    } else {
      // 管理者は全て選択可能
      roleSelect.value = 'student';
      roleSelect.disabled = false;
      classroomSelect.value = '';
      classroomGroup.style.display = 'block';
    }

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
    const isTeacher = this.currentUserData.role === 'teacher';
    const myClassroomId = this.currentUserData.classroomId;

    const userId = document.getElementById('newUserId').value.trim();
    const displayName = document.getElementById('newUserDisplayName').value.trim();
    const password = document.getElementById('newUserPassword').value;

    // 講師の場合は強制的に生徒・自教室
    const role = isTeacher ? 'student' : document.getElementById('newUserRole').value;
    const classroomId = isTeacher ? myClassroomId : document.getElementById('newUserClassroom').value;

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

    const isTeacher = this.currentUserData.role === 'teacher';

    document.getElementById('editUserUid').value = userId;
    document.getElementById('editUserId').value = user.email?.replace('@laughtale.local', '') || '';
    document.getElementById('editUserDisplayName').value = user.displayName || '';
    document.getElementById('editUserRole').value = user.role || 'student';
    document.getElementById('editUserClassroom').value = user.classroomId || '';

    const roleSelect = document.getElementById('editUserRole');
    const classroomGroup = document.getElementById('editUserClassroomGroup');

    if (isTeacher) {
      // 講師はロール・教室変更不可（表示名のみ変更可能）
      roleSelect.disabled = true;
      classroomGroup.style.display = 'none';
    } else {
      // 管理者は全て編集可能
      roleSelect.disabled = false;
      classroomGroup.style.display = user.role === 'admin' ? 'none' : 'block';
    }

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
    const isTeacher = this.currentUserData.role === 'teacher';

    const userId = document.getElementById('editUserUid').value;
    const displayName = document.getElementById('editUserDisplayName').value.trim();

    if (!displayName) {
      alert('表示名を入力してください');
      return;
    }

    // 講師はロール・教室の変更不可
    const user = this.users.find(u => u.id === userId);
    const role = isTeacher ? user.role : document.getElementById('editUserRole').value;
    const classroomId = isTeacher ? user.classroomId : document.getElementById('editUserClassroom').value;

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

    this.pendingDeleteAction = () => this.deleteUser(userId);

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
    const projectCount = this.projects.filter(p => p.classroomId === classroomId).length;
    const teacherCount = this.users.filter(u =>
      (u.role === 'teacher' || u.role === 'admin') && u.classroomId === classroomId
    ).length;

    if (studentCount > 0) {
      alert(`この教室には${studentCount}名の生徒がいます。\n先に生徒を他の教室に移動してください。`);
      return;
    }

    if (projectCount > 0) {
      alert(`この教室には${projectCount}件のプロジェクトがあります。\n先にプロジェクトの移管または削除を行ってください。`);
      return;
    }

    if (teacherCount > 0) {
      alert(`この教室には${teacherCount}名の講師/管理者が紐づいています。\n先に所属教室の変更を行ってください。`);
      return;
    }

    document.getElementById('confirmDeleteMessage').textContent =
      `教室「${classroom.name}」を削除しますか？\nこの操作は取り消せません。`;

    this.pendingDeleteAction = () => this.deleteClassroom(classroomId);

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
          <img src="${this.safeUrl(project.thumbnailUrl || '')}"
               class="admin-thumb-img"
               alt="${this.escapeHtml(project.name)}"
               style="width: 100%; border-radius: 8px; border: 1px solid #ddd;">
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
        <a href="${this.safeUrl(project.storageUrl)}" target="_blank" class="btn btn-primary" download>
          ダウンロード (.sb3)
        </a>
        <button class="btn btn-danger" style="margin-left: 8px;" data-admin-action="delete-project" data-project-id="${this.escapeHtml(projectId)}">
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
                  <a href="${this.safeUrl(v.storageUrl)}" target="_blank" class="btn btn-secondary btn-sm" download>DL</a>
                  ${i > 0 ? `<button class="btn btn-secondary btn-sm" data-admin-action="rollback-project" data-project-id="${this.escapeHtml(projectId)}" data-version-id="${this.escapeHtml(v.id)}">復元</button>` : ''}
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

    this.pendingDeleteAction = () => this.deleteProject(projectId);

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
    if (modalId === 'confirmDeleteModal') {
      this.pendingDeleteAction = null;
    }
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
   * URLを安全な外部URLのみに制限
   */
  static safeUrl(url) {
    const value = (url || '').trim();
    if (!value) return '';
    if (!/^https?:\/\//i.test(value)) return '';
    return this.escapeHtml(value);
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

document.addEventListener('error', (e) => {
  const target = e.target;
  if (!(target instanceof HTMLImageElement)) return;
  if (!target.classList.contains('admin-thumb-img')) return;
  const parent = target.parentElement;
  if (!parent) return;
  parent.textContent = '📁';
}, true);

document.addEventListener('click', (e) => {
  const actionEl = e.target.closest('[data-admin-action]');
  if (!actionEl) return;

  const action = actionEl.dataset.adminAction;
  const userId = actionEl.dataset.userId;
  const classroomId = actionEl.dataset.classroomId;
  const projectId = actionEl.dataset.projectId;
  const versionId = actionEl.dataset.versionId;

  if (action === 'edit-user' && userId) AdminPanel.showEditUserModal(userId);
  if (action === 'reset-user-password' && userId) AdminPanel.showResetPasswordModal(userId);
  if (action === 'delete-user' && userId) AdminPanel.confirmDeleteUser(userId);
  if (action === 'edit-classroom' && classroomId) AdminPanel.showEditClassroomModal(classroomId);
  if (action === 'delete-classroom' && classroomId) AdminPanel.confirmDeleteClassroom(classroomId);
  if (action === 'open-project-detail' && projectId) AdminPanel.showProjectDetail(projectId);
  if (action === 'delete-project' && projectId) AdminPanel.confirmDeleteProject(projectId);
  if (action === 'rollback-project' && projectId && versionId) AdminPanel.rollbackProject(projectId, versionId);
});

// 初期化
document.addEventListener('DOMContentLoaded', () => {
  AdminPanel.init();
});

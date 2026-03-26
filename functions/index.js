/**
 * Scratch Education Platform - Cloud Functions
 *
 * ユーザー管理用のCloud Functions
 * - ユーザー作成
 * - パスワードリセット
 * - ユーザー削除
 */

const functions = require('firebase-functions');
const admin = require('firebase-admin');
const cors = require('cors')({ origin: true });

// Firebase Admin SDK 初期化
admin.initializeApp();

const db = admin.firestore();
const auth = admin.auth();
const storage = admin.storage();
const LOGIN_EMAIL_DOMAIN = '@laughtale.local';

/**
 * 呼び出し元のユーザー情報を取得
 * @param {string} uid - ユーザーID
 * @returns {Promise<Object|null>} ユーザー情報またはnull
 */
async function getUserData(uid) {
  try {
    const userDoc = await db.collection('users').doc(uid).get();
    if (userDoc.exists) {
      return { uid, ...userDoc.data() };
    }
    return null;
  } catch (error) {
    console.error('getUserData error:', error);
    return null;
  }
}

/**
 * 呼び出し元のロールを確認
 * @param {string} uid - ユーザーID
 * @returns {Promise<string|null>} ロール（admin/teacher/student）またはnull
 */
async function getUserRole(uid) {
  const userData = await getUserData(uid);
  return userData ? userData.role : null;
}

/**
 * 管理者権限を確認
 * @param {string} uid - ユーザーID
 * @returns {Promise<boolean>}
 */
async function isAdmin(uid) {
  const role = await getUserRole(uid);
  return role === 'admin';
}

/**
 * 管理者または講師権限を確認
 * @param {string} uid - ユーザーID
 * @returns {Promise<boolean>}
 */
async function isAdminOrTeacher(uid) {
  const role = await getUserRole(uid);
  return role === 'admin' || role === 'teacher';
}

/**
 * ユーザー作成
 *
 * 管理者: 全てのユーザーを作成可能
 * 講師: 自分の教室の生徒のみ作成可能
 *
 * @param {Object} data
 * @param {string} data.userId - ユーザーID（メールの@前の部分）
 * @param {string} data.displayName - 表示名
 * @param {string} data.password - 初期パスワード
 * @param {string} data.role - ロール（admin/teacher/student）
 * @param {string} data.classroomId - 教室ID（adminの場合は不要）
 */
exports.createUser = functions.region('asia-northeast1').https.onCall(async (data, context) => {
  // 認証確認
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', '認証が必要です');
  }

  // 呼び出し元のユーザー情報を取得
  const callerData = await getUserData(context.auth.uid);
  if (!callerData) {
    throw new functions.https.HttpsError('permission-denied', 'ユーザー情報が見つかりません');
  }

  const callerRole = callerData.role;
  const callerClassroomId = callerData.classroomId;

  // 権限チェック
  if (callerRole !== 'admin' && callerRole !== 'teacher') {
    throw new functions.https.HttpsError('permission-denied', '管理者または講師権限が必要です');
  }

  // パラメータ検証
  const { userId, displayName, password, role, classroomId } = data;
  const normalizedUserId = (userId || '').trim().toLowerCase();
  const trimmedDisplayName = (displayName || '').trim();

  if (!normalizedUserId || !trimmedDisplayName || !password || !role) {
    throw new functions.https.HttpsError('invalid-argument', '必須パラメータが不足しています');
  }

  if (password.length < 6) {
    throw new functions.https.HttpsError('invalid-argument', 'パスワードは6文字以上必要です');
  }

  if (!['admin', 'teacher', 'student'].includes(role)) {
    throw new functions.https.HttpsError('invalid-argument', '無効なロールです');
  }

  if (role !== 'admin' && !classroomId) {
    throw new functions.https.HttpsError('invalid-argument', '教室IDが必要です');
  }

  // 講師の場合の制限
  if (callerRole === 'teacher') {
    // 講師は生徒のみ作成可能
    if (role !== 'student') {
      throw new functions.https.HttpsError('permission-denied', '講師は生徒のみ作成できます');
    }
    // 講師は自分の教室の生徒のみ作成可能
    if (classroomId !== callerClassroomId) {
      throw new functions.https.HttpsError('permission-denied', '自分の教室の生徒のみ作成できます');
    }
  }

  const email = `${normalizedUserId}@laughtale.local`;

  try {
    // Firebase Authでユーザー作成
    const userRecord = await auth.createUser({
      email: email,
      password: password,
      displayName: trimmedDisplayName,
      emailVerified: true // メール確認をスキップ
    });

    try {
      // Firestoreにユーザー情報を保存
      await db.collection('users').doc(userRecord.uid).set({
        email: email,
        displayName: trimmedDisplayName,
        role: role,
        classroomId: role === 'admin' ? null : classroomId,
        currentPassword: password,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
        lastLoginAt: null,
        saveCount: 0
      });
    } catch (firestoreError) {
      // Auth作成後にFirestore保存が失敗した場合は補償削除
      try {
        await auth.deleteUser(userRecord.uid);
      } catch (rollbackError) {
        console.error('createUser rollback error:', rollbackError);
      }
      throw firestoreError;
    }

    console.log(`User created: ${email} (${userRecord.uid})`);

    return {
      success: true,
      uid: userRecord.uid,
      message: 'ユーザーを作成しました'
    };

  } catch (error) {
    console.error('createUser error:', error);

    if (error.code === 'auth/email-already-exists') {
      throw new functions.https.HttpsError('already-exists', 'このユーザーIDは既に使用されています');
    }

    throw new functions.https.HttpsError('internal', 'ユーザー作成に失敗しました: ' + error.message);
  }
});

/**
 * パスワードリセット
 *
 * 管理者: 全ユーザーのパスワードをリセット可能
 * 講師: 自分の教室の生徒のパスワードのみリセット可能
 *
 * @param {Object} data
 * @param {string} data.targetUid - 対象ユーザーのUID
 * @param {string} data.newPassword - 新しいパスワード
 */
exports.resetPassword = functions.region('asia-northeast1').https.onCall(async (data, context) => {
  // 認証確認
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', '認証が必要です');
  }

  const { targetUid, newPassword } = data;

  if (!targetUid || !newPassword) {
    throw new functions.https.HttpsError('invalid-argument', '必須パラメータが不足しています');
  }

  if (newPassword.length < 6) {
    throw new functions.https.HttpsError('invalid-argument', 'パスワードは6文字以上必要です');
  }

  // 呼び出し元のロールを取得
  const callerRole = await getUserRole(context.auth.uid);

  if (!callerRole || callerRole === 'student') {
    throw new functions.https.HttpsError('permission-denied', '権限がありません');
  }

  // 対象ユーザーの情報を取得
  const targetUserDoc = await db.collection('users').doc(targetUid).get();

  if (!targetUserDoc.exists) {
    throw new functions.https.HttpsError('not-found', '対象ユーザーが見つかりません');
  }

  const targetUser = targetUserDoc.data();

  // 講師の場合、自分の教室の生徒のみ変更可能
  if (callerRole === 'teacher') {
    const callerDoc = await db.collection('users').doc(context.auth.uid).get();
    const callerData = callerDoc.data();

    if (targetUser.classroomId !== callerData.classroomId || targetUser.role !== 'student') {
      throw new functions.https.HttpsError('permission-denied', '自分の教室の生徒のみパスワードをリセットできます');
    }
  }

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
    console.error('resetPassword error:', error);
    throw new functions.https.HttpsError('internal', 'パスワードリセットに失敗しました: ' + error.message);
  }
});

/**
 * ユーザー削除
 *
 * 管理者: 全ユーザーを削除可能（自分自身以外）
 * 講師: 自分の教室の生徒のみ削除可能
 *
 * @param {Object} data
 * @param {string} data.targetUid - 削除対象のユーザーUID
 */
exports.deleteUser = functions.region('asia-northeast1').https.onCall(async (data, context) => {
  // 認証確認
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', '認証が必要です');
  }

  // 呼び出し元のユーザー情報を取得
  const callerData = await getUserData(context.auth.uid);
  if (!callerData) {
    throw new functions.https.HttpsError('permission-denied', 'ユーザー情報が見つかりません');
  }

  const callerRole = callerData.role;
  const callerClassroomId = callerData.classroomId;

  // 権限チェック
  if (callerRole !== 'admin' && callerRole !== 'teacher') {
    throw new functions.https.HttpsError('permission-denied', '管理者または講師権限が必要です');
  }

  const { targetUid } = data;

  if (!targetUid) {
    throw new functions.https.HttpsError('invalid-argument', '対象ユーザーIDが必要です');
  }

  // 自分自身は削除不可
  if (targetUid === context.auth.uid) {
    throw new functions.https.HttpsError('invalid-argument', '自分自身を削除することはできません');
  }

  // 対象ユーザーの情報を取得
  const targetUserDoc = await db.collection('users').doc(targetUid).get();
  if (!targetUserDoc.exists) {
    throw new functions.https.HttpsError('not-found', '対象ユーザーが見つかりません');
  }

  const targetUser = targetUserDoc.data();

  // 講師の場合の制限
  if (callerRole === 'teacher') {
    // 講師は生徒のみ削除可能
    if (targetUser.role !== 'student') {
      throw new functions.https.HttpsError('permission-denied', '講師は生徒のみ削除できます');
    }
    // 講師は自分の教室の生徒のみ削除可能
    if (targetUser.classroomId !== callerClassroomId) {
      throw new functions.https.HttpsError('permission-denied', '自分の教室の生徒のみ削除できます');
    }
  }

  try {
    // 対象ユーザーのプロジェクトを取得
    const projectsSnapshot = await db.collection('projects')
      .where('userId', '==', targetUid)
      .get();

    // プロジェクトがある場合は警告（削除はしない）
    if (!projectsSnapshot.empty) {
      console.log(`User ${targetUid} has ${projectsSnapshot.size} projects`);
    }

    // Firebase Authからユーザーを削除
    await auth.deleteUser(targetUid);

    // Firestoreからユーザードキュメントを削除
    await db.collection('users').doc(targetUid).delete();

    console.log(`User deleted: ${targetUid}`);

    return {
      success: true,
      message: 'ユーザーを削除しました',
      projectCount: projectsSnapshot.size
    };

  } catch (error) {
    console.error('deleteUser error:', error);

    if (error.code === 'auth/user-not-found') {
      // Authにユーザーがいない場合でもFirestoreから削除を試みる
      try {
        await db.collection('users').doc(targetUid).delete();
        return {
          success: true,
          message: 'ユーザー情報を削除しました（認証情報は既に存在しませんでした）'
        };
      } catch (firestoreError) {
        throw new functions.https.HttpsError('internal', 'ユーザー削除に失敗しました');
      }
    }

    throw new functions.https.HttpsError('internal', 'ユーザー削除に失敗しました: ' + error.message);
  }
});

/**
 * ユーザーID変更（ログインID変更）
 *
 * 管理者のみが実行可能
 * Firebase Auth email と Firestore users/{uid}.email を同時更新する
 *
 * @param {Object} data
 * @param {string} data.targetUid - 対象ユーザーUID
 * @param {string} data.newUserId - 新しいユーザーID（メールの@前）
 */
exports.updateUserId = functions.region('asia-northeast1').https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', '認証が必要です');
  }

  const callerIsAdmin = await isAdmin(context.auth.uid);
  if (!callerIsAdmin) {
    throw new functions.https.HttpsError('permission-denied', '管理者権限が必要です');
  }

  const targetUid = (data?.targetUid || '').trim();
  const newUserId = (data?.newUserId || '').trim().toLowerCase();

  if (!targetUid || !newUserId) {
    throw new functions.https.HttpsError('invalid-argument', '必須パラメータが不足しています');
  }

  if (!/^[a-z0-9._-]{3,64}$/.test(newUserId)) {
    throw new functions.https.HttpsError(
      'invalid-argument',
      'ユーザーIDは英小文字・数字・._- の3〜64文字で入力してください'
    );
  }

  const newEmail = `${newUserId}${LOGIN_EMAIL_DOMAIN}`;

  let userRecord;
  try {
    userRecord = await auth.getUser(targetUid);
  } catch (error) {
    if (error.code === 'auth/user-not-found') {
      throw new functions.https.HttpsError('not-found', '対象ユーザーが見つかりません');
    }
    throw new functions.https.HttpsError('internal', '対象ユーザー取得に失敗しました: ' + error.message);
  }

  const oldEmail = userRecord.email || null;
  if (!oldEmail) {
    throw new functions.https.HttpsError('failed-precondition', '対象ユーザーにメールアドレスが設定されていません');
  }

  if (oldEmail.toLowerCase() === newEmail.toLowerCase()) {
    return {
      success: true,
      message: '変更はありません',
      uid: targetUid,
      email: oldEmail
    };
  }

  try {
    const existing = await auth.getUserByEmail(newEmail).catch((err) => {
      if (err.code === 'auth/user-not-found') return null;
      throw err;
    });
    if (existing && existing.uid !== targetUid) {
      throw new functions.https.HttpsError('already-exists', 'このユーザーIDは既に使用されています');
    }
  } catch (error) {
    if (error instanceof functions.https.HttpsError) throw error;
    throw new functions.https.HttpsError('internal', '重複チェックに失敗しました: ' + error.message);
  }

  try {
    await auth.updateUser(targetUid, { email: newEmail });

    try {
      await db.collection('users').doc(targetUid).update({
        email: newEmail,
        updatedAt: admin.firestore.FieldValue.serverTimestamp()
      });
    } catch (firestoreError) {
      // Firestore更新失敗時はAuthメールを元に戻す
      try {
        await auth.updateUser(targetUid, { email: oldEmail });
      } catch (rollbackError) {
        console.error('updateUserId rollback error:', rollbackError);
      }
      throw firestoreError;
    }

    return {
      success: true,
      message: 'ユーザーIDを変更しました',
      uid: targetUid,
      email: newEmail
    };
  } catch (error) {
    console.error('updateUserId error:', error);
    if (error instanceof functions.https.HttpsError) throw error;
    if (error.code === 'auth/email-already-exists') {
      throw new functions.https.HttpsError('already-exists', 'このユーザーIDは既に使用されています');
    }
    throw new functions.https.HttpsError('internal', 'ユーザーID変更に失敗しました: ' + error.message);
  }
});

/**
 * 複数ユーザー一括作成
 *
 * 管理者のみが実行可能
 * CSVなどから一括でユーザーを作成する場合に使用
 *
 * @param {Object} data
 * @param {Array} data.users - ユーザー情報の配列
 */
exports.createUsers = functions.region('asia-northeast1').https.onCall(async (data, context) => {
  // 認証確認
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', '認証が必要です');
  }

  // 管理者権限確認
  const callerIsAdmin = await isAdmin(context.auth.uid);
  if (!callerIsAdmin) {
    throw new functions.https.HttpsError('permission-denied', '管理者権限が必要です');
  }

  const { users } = data;

  if (!users || !Array.isArray(users) || users.length === 0) {
    throw new functions.https.HttpsError('invalid-argument', 'ユーザーリストが必要です');
  }

  if (users.length > 50) {
    throw new functions.https.HttpsError('invalid-argument', '一度に作成できるユーザーは50人までです');
  }

  const results = [];
  const errors = [];

  for (const user of users) {
    try {
      const { userId, displayName, password, role, classroomId } = user;
      const normalizedUserId = (userId || '').trim().toLowerCase();
      const trimmedDisplayName = (displayName || '').trim();

      if (!normalizedUserId || !trimmedDisplayName || !password || !role) {
        errors.push({ userId, code: 'invalid-argument', error: '必須パラメータが不足しています' });
        continue;
      }

      if (password.length < 6) {
        errors.push({ userId, code: 'invalid-argument', error: 'パスワードは6文字以上必要です' });
        continue;
      }

      if (!['admin', 'teacher', 'student'].includes(role)) {
        errors.push({ userId, code: 'invalid-argument', error: '無効なロールです' });
        continue;
      }

      if (role !== 'admin' && !classroomId) {
        errors.push({ userId, code: 'invalid-argument', error: '教室IDが必要です' });
        continue;
      }

      const email = `${normalizedUserId}@laughtale.local`;

      // Firebase Authでユーザー作成
      const userRecord = await auth.createUser({
        email: email,
        password: password,
        displayName: displayName,
        emailVerified: true
      });

      try {
        // Firestoreにユーザー情報を保存
        await db.collection('users').doc(userRecord.uid).set({
          email: email,
          displayName: trimmedDisplayName,
          role: role,
          classroomId: role === 'admin' ? null : classroomId,
          currentPassword: password,
          createdAt: admin.firestore.FieldValue.serverTimestamp(),
          lastLoginAt: null,
          saveCount: 0
        });
      } catch (firestoreError) {
        // Auth作成後にFirestore保存が失敗した場合は補償削除
        try {
          await auth.deleteUser(userRecord.uid);
        } catch (rollbackError) {
          console.error('createUsers rollback error:', rollbackError);
        }
        throw firestoreError;
      }

      results.push({ userId: normalizedUserId, uid: userRecord.uid, success: true });

    } catch (error) {
      errors.push({ userId: user.userId, code: error.code || 'internal', error: error.message });
    }
  }

  return {
    success: errors.length === 0,
    created: results.length,
    failed: errors.length,
    results,
    errors
  };
});

/**
 * AuthユーザーとFirestore users文書の整合性を監査
 *
 * 管理者のみ実行可能
 *
 * @param {Object} data
 * @param {number} data.maxUsers - 監査対象の最大Authユーザー数（1-5000, default: 2000）
 * @param {boolean} data.onlyLocalDomain - @laughtale.local のみ対象（default: true）
 * @param {number} data.sampleSize - 欠損サンプル返却件数（1-200, default: 50）
 */
exports.auditUserDocumentCoverage = functions.region('asia-northeast1').https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', '認証が必要です');
  }

  const callerIsAdmin = await isAdmin(context.auth.uid);
  if (!callerIsAdmin) {
    throw new functions.https.HttpsError('permission-denied', '管理者権限が必要です');
  }

  const requestedMaxUsers = parseInt(data?.maxUsers, 10);
  const maxUsers = Number.isFinite(requestedMaxUsers)
    ? Math.min(Math.max(requestedMaxUsers, 1), 5000)
    : 2000;
  const onlyLocalDomain = data?.onlyLocalDomain !== false;
  const requestedSampleSize = parseInt(data?.sampleSize, 10);
  const sampleSize = Number.isFinite(requestedSampleSize)
    ? Math.min(Math.max(requestedSampleSize, 1), 200)
    : 50;

  try {
    const targetAuthUsers = [];
    let nextPageToken = undefined;

    // maxUsersに達するまでAuthユーザーを取得
    do {
      const listResult = await auth.listUsers(1000, nextPageToken);
      nextPageToken = listResult.pageToken;

      for (const user of listResult.users) {
        if (onlyLocalDomain && !(user.email || '').endsWith('@laughtale.local')) {
          continue;
        }
        targetAuthUsers.push({
          uid: user.uid,
          email: user.email || null,
          displayName: user.displayName || null
        });
        if (targetAuthUsers.length >= maxUsers) {
          nextPageToken = undefined;
          break;
        }
      }
    } while (nextPageToken);

    // Firestore users文書の存在確認（大きすぎる読み込みを避けて分割）
    const missing = [];
    for (let i = 0; i < targetAuthUsers.length; i += 300) {
      const chunk = targetAuthUsers.slice(i, i + 300);
      const refs = chunk.map((u) => db.collection('users').doc(u.uid));
      const docs = await db.getAll(...refs);
      docs.forEach((docSnap, idx) => {
        if (!docSnap.exists) {
          missing.push(chunk[idx]);
        }
      });
    }

    return {
      success: true,
      auditedAt: new Date().toISOString(),
      options: {
        maxUsers,
        onlyLocalDomain,
        sampleSize
      },
      totals: {
        authUsersAudited: targetAuthUsers.length,
        missingUserDocs: missing.length,
        coverageRate: targetAuthUsers.length > 0
          ? Number((((targetAuthUsers.length - missing.length) / targetAuthUsers.length) * 100).toFixed(2))
          : 100
      },
      sampleMissing: missing.slice(0, sampleSize)
    };
  } catch (error) {
    console.error('auditUserDocumentCoverage error:', error);
    throw new functions.https.HttpsError('internal', '監査に失敗しました: ' + error.message);
  }
});

// ========================================
// 動画アップロード機能
// ========================================

/**
 * 動画アップロード用の署名付きURLを生成
 *
 * 認証済みユーザーのみ使用可能
 * 動画はFirebase Storageのrecordings/{userId}/{timestamp}.mp4に保存される
 *
 * @param {Object} data
 * @param {string} data.filename - ファイル名（オプション）
 * @param {string} data.contentType - コンテンツタイプ（デフォルト: video/mp4）
 */
exports.getUploadUrl = functions.region('asia-northeast1').https.onCall(async (data, context) => {
  // 認証確認
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', '認証が必要です');
  }

  const userId = context.auth.uid;
  const timestamp = Date.now();
  const filename = data.filename || `recording_${timestamp}.mp4`;
  const contentType = data.contentType || 'video/mp4';

  // ファイルパスを生成
  const filePath = `recordings/${userId}/${filename}`;

  try {
    const bucket = storage.bucket();
    const file = bucket.file(filePath);

    // 署名付きアップロードURLを生成（15分間有効）
    const [uploadUrl] = await file.getSignedUrl({
      version: 'v4',
      action: 'write',
      expires: Date.now() + 15 * 60 * 1000, // 15分
      contentType: contentType
    });

    console.log(`Upload URL generated for user ${userId}: ${filePath}`);

    return {
      success: true,
      uploadUrl: uploadUrl,
      filePath: filePath,
      filename: filename,
      expiresIn: 900 // 15分（秒）
    };

  } catch (error) {
    console.error('getUploadUrl error:', error);
    throw new functions.https.HttpsError('internal', 'アップロードURL生成に失敗しました: ' + error.message);
  }
});

/**
 * 動画アップロード完了後にメタデータを保存
 *
 * @param {Object} data
 * @param {string} data.filePath - アップロードしたファイルのパス
 * @param {string} data.title - 動画タイトル（オプション）
 * @param {string} data.description - 説明（オプション）
 */
exports.saveVideoMetadata = functions.region('asia-northeast1').https.onCall(async (data, context) => {
  // 認証確認
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', '認証が必要です');
  }

  const { filePath, title, description } = data;

  if (!filePath) {
    throw new functions.https.HttpsError('invalid-argument', 'filePathが必要です');
  }

  const userId = context.auth.uid;
  const expectedPrefix = `recordings/${userId}/`;

  if (!filePath.startsWith(expectedPrefix)) {
    throw new functions.https.HttpsError('permission-denied', '自分のアップロードファイルのみ公開できます');
  }

  try {
    // ユーザー情報を取得
    const userData = await getUserData(userId);
    const displayName = userData ? userData.displayName : '不明';
    const classroomId = userData ? userData.classroomId : null;

    // ファイルの公開URLを取得
    const bucket = storage.bucket();
    const file = bucket.file(filePath);

    // ファイルが存在するか確認
    const [exists] = await file.exists();
    if (!exists) {
      throw new functions.https.HttpsError('not-found', 'ファイルが見つかりません');
    }

    // ファイルを公開設定に変更
    await file.makePublic();

    // 公開URLを取得
    const publicUrl = `https://storage.googleapis.com/${bucket.name}/${filePath}`;

    // Firestoreにメタデータを保存
    const videoDoc = await db.collection('videos').add({
      userId: userId,
      userDisplayName: displayName,
      classroomId: classroomId,
      filePath: filePath,
      publicUrl: publicUrl,
      title: title || '無題の録画',
      description: description || '',
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      status: 'published',
      viewCount: 0
    });

    console.log(`Video metadata saved: ${videoDoc.id} for user ${userId}`);

    return {
      success: true,
      videoId: videoDoc.id,
      publicUrl: publicUrl,
      message: '動画を保存しました'
    };

  } catch (error) {
    console.error('saveVideoMetadata error:', error);
    if (error instanceof functions.https.HttpsError) {
      throw error;
    }
    throw new functions.https.HttpsError('internal', 'メタデータ保存に失敗しました: ' + error.message);
  }
});

/**
 * ギャラリー用の動画一覧取得（HTTP関数）
 *
 * 公開された動画の一覧を取得
 * 認証不要（ギャラリーは公開）
 */
exports.getPublicVideos = functions.region('asia-northeast1').https.onRequest(async (req, res) => {
  cors(req, res, async () => {
    try {
      const requestedLimit = parseInt(req.query.limit, 10);
      const limit = Number.isFinite(requestedLimit)
        ? Math.min(Math.max(requestedLimit, 1), 50)
        : 20;
      const classroomId = req.query.classroomId || null;

      let query = db.collection('videos')
        .where('status', '==', 'published')
        .orderBy('createdAt', 'desc')
        .limit(limit);

      if (classroomId) {
        query = db.collection('videos')
          .where('status', '==', 'published')
          .where('classroomId', '==', classroomId)
          .orderBy('createdAt', 'desc')
          .limit(limit);
      }

      const snapshot = await query.get();

      const videos = [];
      snapshot.forEach(doc => {
        const data = doc.data();
        videos.push({
          id: doc.id,
          title: data.title,
          description: data.description,
          publicUrl: data.publicUrl,
          userDisplayName: data.userDisplayName,
          createdAt: data.createdAt ? data.createdAt.toDate().toISOString() : null,
          viewCount: data.viewCount || 0
        });
      });

      res.json({
        success: true,
        videos: videos,
        count: videos.length
      });

    } catch (error) {
      console.error('getPublicVideos error:', error);
      res.status(500).json({
        success: false,
        error: error.message
      });
    }
  });
});

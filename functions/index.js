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

  if (!userId || !displayName || !password || !role) {
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

  const email = `${userId}@laughtale.local`;

  try {
    // Firebase Authでユーザー作成
    const userRecord = await auth.createUser({
      email: email,
      password: password,
      displayName: displayName,
      emailVerified: true // メール確認をスキップ
    });

    // Firestoreにユーザー情報を保存
    await db.collection('users').doc(userRecord.uid).set({
      email: email,
      displayName: displayName,
      role: role,
      classroomId: role === 'admin' ? null : classroomId,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      lastLoginAt: null,
      saveCount: 0
    });

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

      if (!userId || !displayName || !password || !role) {
        errors.push({ userId, error: '必須パラメータが不足しています' });
        continue;
      }

      const email = `${userId}@laughtale.local`;

      // Firebase Authでユーザー作成
      const userRecord = await auth.createUser({
        email: email,
        password: password,
        displayName: displayName,
        emailVerified: true
      });

      // Firestoreにユーザー情報を保存
      await db.collection('users').doc(userRecord.uid).set({
        email: email,
        displayName: displayName,
        role: role,
        classroomId: role === 'admin' ? null : classroomId,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
        lastLoginAt: null,
        saveCount: 0
      });

      results.push({ userId, uid: userRecord.uid, success: true });

    } catch (error) {
      errors.push({ userId: user.userId, error: error.message });
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

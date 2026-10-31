// firestore.rules の users まわりの試験（エミュレーター上だけで動かす）
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { initializeTestEnvironment, assertSucceeds, assertFails } from '@firebase/rules-unit-testing';
import {
  orderBy,
  writeBatch,
  doc, getDoc, setDoc, updateDoc, deleteDoc, collection, query, where, getDocs,
  increment, serverTimestamp, deleteField, Timestamp,
} from 'firebase/firestore';

// 試験するルールのファイル（既定はこの試験ファイルから見た ../firestore.rules。環境変数 RULES で差し替えられる）
const RULES = process.env.RULES || fileURLToPath(new URL('../firestore.rules', import.meta.url));
const env = await initializeTestEnvironment({
  projectId: 'demo-rules-test',
  firestore: { rules: readFileSync(RULES, 'utf8'), host: '127.0.0.1', port: 8085 },
});

// 試験用のデータ（値は架空）
const now = Timestamp.now();
const USERS = {
  admin1: { role: 'admin', classroomId: 'c1', displayName: '管理者', email: 'a@laughtale.local', saveCount: 0 },
  admin2: { role: 'admin', classroomId: null, displayName: '管理者2', email: 'a2@laughtale.local', saveCount: 0 },
  t1: { role: 'teacher', classroomId: 'c1', displayName: '先生1', email: 't1@laughtale.local', saveCount: 0, currentPassword: 'old-teacher-pw' },
  t2: { role: 'teacher', classroomId: 'c1', displayName: '先生2', email: 't2@laughtale.local' },
  tnull: { role: 'teacher', classroomId: null, displayName: '先生null', email: 'tn@laughtale.local', saveCount: 0 },
  s1: { role: 'student', classroomId: 'c1', displayName: '生徒1', email: 's1@laughtale.local', saveCount: 3, currentPassword: 'pw1', lastLoginAt: now, createdAt: now },
  s2: { role: 'student', classroomId: 'c2', displayName: '生徒2', email: 's2@laughtale.local', saveCount: 0, currentPassword: 'pw2' },
  t3: { role: 'teacher', classroomId: 'c2', displayName: '先生3', email: 't3@laughtale.local' },
  s3: { role: 'student', classroomId: 'c1', displayName: '生徒3', email: 's3@laughtale.local', saveCount: 0 },
  snull: { role: 'student', classroomId: null, displayName: '生徒null', email: 'sn@laughtale.local', saveCount: 0 },
};

// 作品の保存先の URL（本番と同じ形。値は架空）
const B = 'https://firebasestorage.googleapis.com/v0/b/laughtale-scratch-bcc8a.firebasestorage.app/o/';
const fileUrl = (uid, pid, rest = 'current.sb3') => `${B}projects%2F${uid}%2F${pid}%2F${rest.replace(/\//g, '%2F')}?alt=media&token=x`;
const thumbUrl = (uid, pid) => `${B}thumbnails%2F${uid}%2F${pid}.png?alt=media&token=x`;
const proj = (uid, pid, classroomId) => ({
  userId: uid, classroomId, name: '作品', description: '', thumbnailUrl: null,
  storageUrl: fileUrl(uid, pid), createdAt: now, updatedAt: now, isSubmitted: false, submittedAt: null, size: 10,
});
const PROJECTS = {
  pS1: proj('s1', 'pS1', 'c1'),
  pS1b: proj('s1', 'pS1b', 'c1'),
  pS2: proj('s2', 'pS2', 'c2'),
  pAdmin: proj('admin2', 'pAdmin', null),
};

async function reset() {
  await env.clearFirestore();
  await env.withSecurityRulesDisabled(async (ctx) => {
    const fs = ctx.firestore();
    for (const [uid, data] of Object.entries(USERS)) {
      await setDoc(doc(fs, 'users', uid), data);
    }
    for (const [pid, data] of Object.entries(PROJECTS)) {
      await setDoc(doc(fs, 'projects', pid), data);
    }
    await setDoc(doc(fs, 'projects', 'pS1', 'versions', 'vOld'), {
      versionNum: 1, storageUrl: fileUrl('s1', 'pS1', 'versions/v1.sb3'), createdAt: now, size: 10,
    });
  });
}

// 同じ利用者の Firestore は 1 回だけ作って使い回す
const dbs = {};
const db = (uid) => (dbs[uid] ??= env.authenticatedContext(uid).firestore());
const anonDb = env.unauthenticatedContext().firestore();
const u = (who, target) => doc(db(who), 'users', target);

let pass = 0, fail = 0;
async function t(name, fn) {
  await reset();
  try { await fn(); pass++; console.log('  ok  ', name); }
  catch (e) { fail++; console.log('  NG  ', name, '-', e.message.split('\n')[0]); }
}

console.log('--- 読み取り');
await t('講師は自教室の生徒を読める', () => assertSucceeds(getDoc(u('t1', 's1'))));
await t('講師は自分を読める', () => assertSucceeds(getDoc(u('t1', 't1'))));
await t('講師は同じ教室の別の講師を読めない', () => assertFails(getDoc(u('t1', 't2'))));
await t('講師は同じ教室ID の管理者を読めない', () => assertFails(getDoc(u('t1', 'admin1'))));
await t('講師は他教室の生徒を読めない', () => assertFails(getDoc(u('t1', 's2'))));
await t('教室 null の講師は教室 null の管理者を読めない', () => assertFails(getDoc(u('tnull', 'admin2'))));
await t('教室 null の講師は教室 null の生徒を読めない', () => assertFails(getDoc(u('tnull', 'snull'))));
await t('講師の一覧クエリ（教室＋生徒）は通る', () => assertSucceeds(getDocs(query(collection(db('t1'), 'users'), where('classroomId', '==', 'c1'), where('role', '==', 'student')))));
await t('講師の一覧クエリ（教室だけ）は拒否される', () => assertFails(getDocs(query(collection(db('t1'), 'users'), where('classroomId', '==', 'c1')))));
await t('管理者は全員を読める', () => assertSucceeds(getDocs(collection(db('admin1'), 'users'))));
await t('生徒は自分を読める', () => assertSucceeds(getDoc(u('s1', 's1'))));
await t('生徒は他の生徒を読めない', () => assertFails(getDoc(u('s1', 's2'))));
await t('未ログインは読めない', () => assertFails(getDoc(doc(anonDb, 'users', 's1'))));

console.log('--- 本人の更新（アプリが実際に送る形）');
await t('保存回数の +1 は通る', () => assertSucceeds(updateDoc(u('s1', 's1'), { saveCount: increment(1) })));
await t('保存回数が無い人の +1 も通る', () => assertSucceeds(updateDoc(u('t2', 't2'), { saveCount: increment(1) })));
await t('最終ログイン日時の更新は通る', () => assertSucceeds(updateDoc(u('s1', 's1'), { lastLoginAt: serverTimestamp() })));
await t('表示名の変更は通る', () => assertSucceeds(updateDoc(u('s1', 's1'), { displayName: 'たろう' })));
await t('設定画面の上限 20 文字の日本語の表示名も通る', () => assertSucceeds(updateDoc(u('s1', 's1'), { displayName: 'あ'.repeat(20) })));
await t('講師の本人の最終ログイン更新も通る', () => assertSucceeds(updateDoc(u('t1', 't1'), { lastLoginAt: serverTimestamp() })));

console.log('--- 本人の更新（攻撃）');
await t('保存回数に HTML を入れられない', () => assertFails(updateDoc(u('s1', 's1'), { saveCount: '<img src=x onerror=alert(1)>' })));
await t('最終ログインに文字列を入れられない', () => assertFails(updateDoc(u('s1', 's1'), { lastLoginAt: 'x' })));
await t('表示名に数値を入れられない', () => assertFails(updateDoc(u('s1', 's1'), { displayName: 123 })));
await t('表示名を空にできない', () => assertFails(updateDoc(u('s1', 's1'), { displayName: '' })));
await t('表示名を 51 文字にできない', () => assertFails(updateDoc(u('s1', 's1'), { displayName: 'あ'.repeat(51) })));
await t('パスワードを書き換えられない', () => assertFails(updateDoc(u('s1', 's1'), { currentPassword: 'x' })));
await t('メールを書き換えられない', () => assertFails(updateDoc(u('s1', 's1'), { email: 'x@laughtale.local' })));
await t('新しい項目を足せない', () => assertFails(updateDoc(u('s1', 's1'), { evil: '<b>' })));
await t('項目を消せない', () => assertFails(updateDoc(u('s1', 's1'), { createdAt: deleteField() })));
await t('ロールを変えられない', () => assertFails(updateDoc(u('s1', 's1'), { role: 'admin' })));
await t('教室を変えられない', () => assertFails(updateDoc(u('s1', 's1'), { classroomId: 'c2' })));
await t('講師も本人のロールを変えられない', () => assertFails(updateDoc(u('t1', 't1'), { role: 'admin' })));

console.log('--- 講師による生徒の更新');
// admin.js の updateUser は displayName・role・classroomId（同じ値）・updatedAt を送る
await t('講師は自教室の生徒の表示名を変えられる', () => assertSucceeds(updateDoc(u('t1', 's1'), { displayName: '新しい名前', role: 'student', classroomId: 'c1', updatedAt: serverTimestamp() })));
await t('講師は生徒の保存回数を変えられない', () => assertFails(updateDoc(u('t1', 's1'), { saveCount: '<img>' })));
await t('講師は生徒の保存回数を整数でも変えられない', () => assertFails(updateDoc(u('t1', 's1'), { saveCount: 4 })));
await t('講師は生徒の保存回数を +1 できない', () => assertFails(updateDoc(u('t1', 's1'), { saveCount: increment(1) })));
await t('講師は生徒のパスワード欄を変えられない', () => assertFails(updateDoc(u('t1', 's1'), { currentPassword: 'x' })));
await t('講師は生徒を講師にできない', () => assertFails(updateDoc(u('t1', 's1'), { role: 'teacher' })));
await t('講師は別の講師を変えられない', () => assertFails(updateDoc(u('t1', 't2'), { displayName: 'x' })));
await t('講師は他教室の生徒を変えられない', () => assertFails(updateDoc(u('t1', 's2'), { displayName: 'x' })));
await t('講師は生徒に新しい項目を足せない', () => assertFails(updateDoc(u('t1', 's1'), { evil: '<b>' })));
await t('講師は生徒のパスワード欄を消せない', () => assertFails(updateDoc(u('t1', 's1'), { currentPassword: deleteField() })));
await t('講師は生徒の表示名に数値を入れられない', () => assertFails(updateDoc(u('t1', 's1'), { displayName: 123 })));
await t('講師は更新日時に文字列を入れられない', () => assertFails(updateDoc(u('t1', 's1'), { updatedAt: 'x' })));
await t('講師は生徒の表示名を 20 文字の日本語にできる', () => assertSucceeds(updateDoc(u('t1', 's1'), { displayName: 'あ'.repeat(20) })));

console.log('--- 管理者・作成');
await t('管理者は講師のパスワード欄を消せる', () => assertSucceeds(updateDoc(u('admin1', 't1'), { role: 'teacher', currentPassword: deleteField() })));
await t('誰も users を直接作れない', () => assertFails(setDoc(u('s1', 'new1'), { role: 'student' })));

console.log('--- 作品の作成（アプリが送る形）');
// firebase-integration.js の saveNewProject と同じ項目
const newProj = (uid, pid, classroomId, extra = {}) => ({
  userId: uid, classroomId, name: 'あたらしい作品', description: '', thumbnailUrl: null,
  storageUrl: fileUrl(uid, pid), createdAt: serverTimestamp(), updatedAt: serverTimestamp(),
  isSubmitted: false, submittedAt: null, size: 1234, ...extra,
});
const pr = (who, pid) => doc(db(who), 'projects', pid);
await t('生徒は自分の教室の作品を作れる', () => assertSucceeds(setDoc(pr('s1', 'pNew'), newProj('s1', 'pNew', 'c1'))));
await t('生徒は教室 null の作品も作れる（教室の読み込み前）', () => assertSucceeds(setDoc(pr('s1', 'pNew'), newProj('s1', 'pNew', null))));
await t('サムネイル付きでも作れる', () => assertSucceeds(setDoc(pr('s1', 'pNew'), newProj('s1', 'pNew', 'c1', { thumbnailUrl: thumbUrl('s1', 'pNew') }))));
await t('管理者は教室 null の作品を作れる', () => assertSucceeds(setDoc(pr('admin1', 'pNew'), newProj('admin1', 'pNew', null))));
await t('管理者は教室ID付きの作品を作れない（講師に見せないため）', () => assertFails(setDoc(pr('admin1', 'pNew'), newProj('admin1', 'pNew', 'c1'))));
await t('教室 null の生徒は教室ID付きの作品を作れない', () => assertFails(setDoc(pr('snull', 'pNew'), newProj('snull', 'pNew', 'c1'))));
await t('users 文書の無い人は教室ID付きの作品を作れない', () => assertFails(setDoc(pr('ghost', 'pNew'), newProj('ghost', 'pNew', 'c1'))));
await t('users 文書の無い人でも教室 null の作品は作れる（自分のファイル置き場だけ）', () => assertSucceeds(setDoc(pr('ghost', 'pNew'), newProj('ghost', 'pNew', null))));
await t('講師は教室 null の作品を作れる', () => assertSucceeds(setDoc(pr('t1', 'pNew'), newProj('t1', 'pNew', null))));
await t('講師は自分の教室IDの作品を作れない（同じ教室の講師に見せないため）', () => assertFails(setDoc(pr('t1', 'pNew'), newProj('t1', 'pNew', 'c1'))));
await t('講師が作った作品は、同じ教室の別の講師から読めない', async () => {
  await assertSucceeds(setDoc(pr('t2', 'pNew'), newProj('t2', 'pNew', null)));
  await assertFails(getDoc(pr('t1', 'pNew')));
});

console.log('--- 作品の作成（攻撃）');
await t('他の教室の作品は作れない', () => assertFails(setDoc(pr('s1', 'pNew'), newProj('s1', 'pNew', 'c2'))));
await t('提出済みの状態では作れない', () => assertFails(setDoc(pr('s1', 'pNew'), newProj('s1', 'pNew', 'c1', { isSubmitted: true }))));
await t('他の子のファイルの URL では作れない', () => assertFails(setDoc(pr('s1', 'pNew'), newProj('s1', 'pNew', 'c1', { storageUrl: fileUrl('s2', 'pS2') }))));
await t('別の作品IDの URL では作れない', () => assertFails(setDoc(pr('s1', 'pNew'), newProj('s1', 'pNew', 'c1', { storageUrl: fileUrl('s1', 'pS1') }))));
await t('外のサーバーの URL では作れない', () => assertFails(setDoc(pr('s1', 'pNew'), newProj('s1', 'pNew', 'c1', { storageUrl: 'https://evil.example/x.sb3' }))));
await t('外のサムネイル URL では作れない', () => assertFails(setDoc(pr('s1', 'pNew'), newProj('s1', 'pNew', 'c1', { thumbnailUrl: 'https://evil.example/x.png' }))));
await t('id 項目を足して作れない', () => assertFails(setDoc(pr('s1', 'pNew'), newProj('s1', 'pNew', 'c1', { id: 'pS1' }))));
await t('名前が数値では作れない', () => assertFails(setDoc(pr('s1', 'pNew'), newProj('s1', 'pNew', 'c1', { name: 1 }))));
await t('更新日時が文字列では作れない', () => assertFails(setDoc(pr('s1', 'pNew'), newProj('s1', 'pNew', 'c1', { updatedAt: 'x' }))));
await t('記号入りの作品ID（p|.*|q）で外の URL を入れられない', () => assertFails(setDoc(pr('s1', 'p|.*|q'), newProj('s1', 'p|.*|q', 'c1', { storageUrl: 'https://evil.example/a%2Fb' }))));
await t('記号入りの作品ID（x|.*）で外のサムネイルを入れられない', () => assertFails(setDoc(pr('s1', 'x|.*'), newProj('s1', 'x|.*', 'c1', { storageUrl: fileUrl('s1', 'x|.*'), thumbnailUrl: 'https://evil.example/a.png' }))));
await t('作品ID「.*」で自分の別の作品を指せない', () => assertFails(setDoc(pr('s1', '.*'), newProj('s1', '.*', 'c1', { storageUrl: fileUrl('s1', 'pS1') }))));
await t('記号入りの作品IDでは正規の URL でも作れない', () => assertFails(setDoc(pr('s1', 'a(b'), newProj('s1', 'a(b', 'c1'))));
await t('URL の後ろに /../ で別のバケットを指せない', () => assertFails(setDoc(pr('s1', 'pNew'), newProj('s1', 'pNew', 'c1', { storageUrl: `${B}projects%2Fs1%2FpNew%2Fcurrent.sb3/../../../../b/OTHER/o/evil.sb3?alt=media` }))));
await t('決まった名前以外のファイルは指せない', () => assertFails(setDoc(pr('s1', 'pNew'), newProj('s1', 'pNew', 'c1', { storageUrl: `${B}projects%2Fs1%2FpNew%2Fother.sb3?alt=media` }))));
await t('クエリに / を入れられない', () => assertFails(setDoc(pr('s1', 'pNew'), newProj('s1', 'pNew', 'c1', { storageUrl: `${B}projects%2Fs1%2FpNew%2Fcurrent.sb3?x=/../../b` }))));
await t('# を付けられない', () => assertFails(setDoc(pr('s1', 'pNew'), newProj('s1', 'pNew', 'c1', { storageUrl: `${B}projects%2Fs1%2FpNew%2Fcurrent.sb3#x` }))));
await t('トークンなしの正規の URL は通る', () => assertSucceeds(setDoc(pr('s1', 'pNew'), newProj('s1', 'pNew', 'c1', { storageUrl: `${B}projects%2Fs1%2FpNew%2Fcurrent.sb3` }))));
await t('他人の作品としては作れない', () => assertFails(setDoc(pr('s1', 'pNew'), newProj('s2', 'pNew', 'c1'))));

console.log('--- 作品の更新（アプリが送る形）');
await t('上書き保存（URL・日時・大きさ）', () => assertSucceeds(updateDoc(pr('s1', 'pS1'), { storageUrl: fileUrl('s1', 'pS1'), updatedAt: serverTimestamp(), size: 99, thumbnailUrl: thumbUrl('s1', 'pS1') })));
await t('復元のあとの上書き保存（履歴の URL から current.sb3 に戻す）', async () => {
  await env.withSecurityRulesDisabled((ctx) => updateDoc(doc(ctx.firestore(), 'projects', 'pS1'), { storageUrl: fileUrl('s1', 'pS1', 'versions/v1.sb3') }));
  const next = fileUrl('s1', 'pS1').replace('token=x', 'token=new');
  await assertSucceeds(updateDoc(pr('s1', 'pS1'), { storageUrl: next, updatedAt: serverTimestamp(), size: 100 }));
  let after = null;
  await env.withSecurityRulesDisabled(async (ctx) => { after = (await getDoc(doc(ctx.firestore(), 'projects', 'pS1'))).data(); });
  if (after.storageUrl !== next) throw new Error('上書き保存の URL が保存されていない');
});
await t('提出', () => assertSucceeds(updateDoc(pr('s1', 'pS1'), { isSubmitted: true, submittedAt: serverTimestamp(), updatedAt: serverTimestamp() })));
await t('提出の取り消し（提出済みの状態から）', async () => {
  await env.withSecurityRulesDisabled((ctx) => updateDoc(doc(ctx.firestore(), 'projects', 'pS1'), { isSubmitted: true, submittedAt: now }));
  await assertSucceeds(updateDoc(pr('s1', 'pS1'), { isSubmitted: false, submittedAt: null, updatedAt: serverTimestamp() }));
  let after = null;
  await env.withSecurityRulesDisabled(async (ctx) => { after = (await getDoc(doc(ctx.firestore(), 'projects', 'pS1'))).data(); });
  if (after.isSubmitted !== false || after.submittedAt !== null) throw new Error('取り消しが保存されていない');
});
await t('名前の変更', () => assertSucceeds(updateDoc(pr('s1', 'pS1'), { name: '新しい名前', updatedAt: serverTimestamp() })));
await t('講師の復元（履歴を読んでから、その URL に戻す）', async () => {
  const v = await assertSucceeds(getDoc(doc(db('t1'), 'projects', 'pS1', 'versions', 'vOld')));
  await assertSucceeds(updateDoc(pr('t1', 'pS1'), { storageUrl: v.data().storageUrl, updatedAt: serverTimestamp() }));
});
await t('持ち主はバックアップのために履歴の一覧を取れる', () => assertSucceeds(getDocs(query(collection(db('s1'), 'projects', 'pS1', 'versions'), orderBy('createdAt', 'desc')))));
await t('講師は自教室の作品の履歴一覧を取れる', () => assertSucceeds(getDocs(query(collection(db('t1'), 'projects', 'pS1', 'versions'), orderBy('createdAt', 'desc')))));
await t('他の子は履歴を読めない', () => assertFails(getDoc(doc(db('s2'), 'projects', 'pS1', 'versions', 'vOld'))));

console.log('--- 作品の更新（攻撃）');
await t('他の子のファイルの URL にできない', () => assertFails(updateDoc(pr('s1', 'pS1'), { storageUrl: fileUrl('s2', 'pS2') })));
await t('自分の別の作品の URL にもできない', () => assertFails(updateDoc(pr('s1', 'pS1'), { storageUrl: fileUrl('s1', 'pS1b') })));
await t('id 項目を足せない', () => assertFails(updateDoc(pr('s1', 'pS1'), { id: 'pS1b' })));
await t('講師も id 項目を足せない', () => assertFails(updateDoc(pr('t1', 'pS1'), { id: '"><img src=x onerror=alert(1)>' })));
await t('項目を消せない', () => assertFails(updateDoc(pr('s1', 'pS1'), { description: deleteField() })));
await t('提出日時に文字列を入れられない', () => assertFails(updateDoc(pr('s1', 'pS1'), { submittedAt: 'x' })));
await t('名前に数値を入れられない', () => assertFails(updateDoc(pr('s1', 'pS1'), { name: 1 })));
await t('持ち主を変えられない', () => assertFails(updateDoc(pr('s1', 'pS1'), { userId: 's2' })));
await t('教室を変えられない', () => assertFails(updateDoc(pr('s1', 'pS1'), { classroomId: 'c2' })));

console.log('--- 前のルールで作られた、記号入りの ID の作品');
// 前のルールでは記号入りの作品IDや持ち主を作れたので、すでにある場合も URL の確認が効くことを確かめる
async function seedOdd(pid, data) {
  await env.withSecurityRulesDisabled((ctx) => setDoc(doc(ctx.firestore(), 'projects', pid), data));
}
await t('記号入りの作品IDの作品は、外の URL に更新できない', async () => {
  await seedOdd('p|.*|q', proj('s1', 'p|.*|q', 'c1'));
  await assertFails(updateDoc(pr('s1', 'p|.*|q'), { storageUrl: 'https://evil.example/a%2Fb' }));
});
await t('記号入りの作品IDの作品は、サムネイルも外の URL にできない', async () => {
  await seedOdd('x|.*', proj('s1', 'x|.*', 'c1'));
  await assertFails(updateDoc(pr('s1', 'x|.*'), { thumbnailUrl: 'https://evil.example/a.png' }));
});
await t('記号入りの作品IDの作品には、外の URL の履歴を作れない', async () => {
  await seedOdd('p|.*|q', proj('s1', 'p|.*|q', 'c1'));
  await assertFails(setDoc(doc(db('s1'), 'projects', 'p|.*|q', 'versions', 'v1'), { versionNum: 1, storageUrl: 'https://evil.example/a%2Fb', createdAt: serverTimestamp() }));
});
await t('記号入りの作品IDの作品でも、名前の変更と提出はできる', async () => {
  await seedOdd('p|.*|q', proj('s1', 'p|.*|q', 'c1'));
  await assertSucceeds(updateDoc(pr('s1', 'p|.*|q'), { name: '新しい名前', isSubmitted: true, submittedAt: serverTimestamp(), updatedAt: serverTimestamp() }));
});
await t('持ち主が記号入りの作品は、講師でも外の URL に更新できない', async () => {
  await seedOdd('pOdd', { ...proj('s1', 'pOdd', 'c1'), userId: 'x|.*' });
  await assertFails(updateDoc(pr('t1', 'pOdd'), { storageUrl: 'https://evil.example/a%2Fb' }));
});
await t('講師は他の子の履歴の URL で復元できない', () => assertFails(updateDoc(pr('t1', 'pS1'), { storageUrl: fileUrl('s2', 'pS2', 'versions/v1.sb3') })));
// ブラウザの URL の読み取りは改行とタブを取り除くので、クエリに改行があっても指す場所は変わらない
const samePath = (u, path) => new URL(u).pathname === new URL(`${B}${path}`).pathname;
await t('クエリに本物の改行（LF）があっても、指す場所は変わらない', async () => {
  const u = `${B}projects%2Fs1%2FpS1%2Fcurrent.sb3?alt=media\n&token=x`;
  if (!samePath(u, 'projects%2Fs1%2FpS1%2Fcurrent.sb3')) throw new Error('場所が変わった');
  await assertSucceeds(updateDoc(pr('s1', 'pS1'), { storageUrl: u }));
});
await t('クエリに本物の改行（CRLF）があっても、指す場所は変わらない', async () => {
  const u = `${B}projects%2Fs1%2FpS1%2Fcurrent.sb3?alt=media\r\n&token=x`;
  if (!samePath(u, 'projects%2Fs1%2FpS1%2Fcurrent.sb3')) throw new Error('場所が変わった');
  await assertSucceeds(updateDoc(pr('s1', 'pS1'), { storageUrl: u }));
});
await t('パスの中の改行（LF）は拒否する', () => assertFails(updateDoc(pr('s1', 'pS1'), { storageUrl: `${B}projects%2Fs1%2FpS1%2Fcurrent.sb3\n?alt=media` })));
await t('パスの中のエンコードした改行（%0A）は拒否する', () => assertFails(updateDoc(pr('s1', 'pS1'), { storageUrl: `${B}projects%2Fs1%2FpS1%2Fcurrent.sb3%0A?alt=media` })));
await t('クエリに \\ や %2F を入れても場所は変わらない（正規の場所なら通る）', () => assertSucceeds(updateDoc(pr('s1', 'pS1'), { storageUrl: `${B}projects%2Fs1%2FpS1%2Fcurrent.sb3?alt=media&x=%2F..%2F%00%23\\` })));
await t('クエリより前に \\ を入れて別の場所は指せない', () => assertFails(updateDoc(pr('s1', 'pS1'), { storageUrl: `${B}projects%2Fs1%2FpS1%2Fcurrent.sb3\\..\\x?alt=media` })));

console.log('--- 作品の読み取り・削除');
await t('講師は自教室の作品を読める', () => assertSucceeds(getDoc(pr('t1', 'pS1'))));
await t('講師は自教室の作品一覧を取れる', () => assertSucceeds(getDocs(query(collection(db('t1'), 'projects'), where('classroomId', '==', 'c1')))));
await t('講師は他教室の作品を読めない', () => assertFails(getDoc(pr('t1', 'pS2'))));
await t('教室 null の講師は教室 null の作品を読めない', () => assertFails(getDoc(pr('tnull', 'pAdmin'))));
await t('教室 null の講師は教室 null の一覧を取れない', () => assertFails(getDocs(query(collection(db('tnull'), 'projects'), where('classroomId', '==', null)))));
await t('教室 null の講師は教室 null の作品を消せない', () => assertFails(deleteDoc(pr('tnull', 'pAdmin'))));
await t('講師は自教室の作品を消せる', () => assertSucceeds(deleteDoc(pr('t1', 'pS1'))));
await t('持ち主は自分の未提出の作品を消せる', () => assertSucceeds(deleteDoc(pr('s1', 'pS1'))));
await t('持ち主は自分の提出済みの作品を消せない', async () => {
  await env.withSecurityRulesDisabled((ctx) => updateDoc(doc(ctx.firestore(), 'projects', 'pS1'), { isSubmitted: true, submittedAt: now }));
  await assertFails(deleteDoc(pr('s1', 'pS1')));
});
await t('他の子は未提出の作品でも消せない', () => assertFails(deleteDoc(pr('s2', 'pS1'))));
await t('持ち主は履歴と作品をまとめて消せる（生徒の削除ボタン）', async () => {
  const b = writeBatch(db('s1'));
  b.delete(doc(db('s1'), 'projects', 'pS1', 'versions', 'vOld'));
  b.delete(pr('s1', 'pS1'));
  await assertSucceeds(b.commit());
});
await t('提出済みなら、まとめ消しは全部取り消され、履歴も残る', async () => {
  await env.withSecurityRulesDisabled((ctx) => updateDoc(doc(ctx.firestore(), 'projects', 'pS1'), { isSubmitted: true, submittedAt: now }));
  const b = writeBatch(db('s1'));
  b.delete(doc(db('s1'), 'projects', 'pS1', 'versions', 'vOld'));
  b.delete(pr('s1', 'pS1'));
  await assertFails(b.commit());
  let v = null;
  await env.withSecurityRulesDisabled(async (ctx) => { v = await getDoc(doc(ctx.firestore(), 'projects', 'pS1', 'versions', 'vOld')); });
  if (!v.exists()) throw new Error('履歴が消えてしまった');
});
await t('講師は履歴と作品をまとめて消せる（管理画面の削除）', async () => {
  const b = writeBatch(db('t1'));
  b.delete(doc(db('t1'), 'projects', 'pS1', 'versions', 'vOld'));
  b.delete(pr('t1', 'pS1'));
  await assertSucceeds(b.commit());
});
await t('生徒は他の子の作品を読めない', () => assertFails(getDoc(pr('s1', 'pS2'))));

console.log('--- 作品の履歴（バージョン）');
const ver = (who, pid) => doc(db(who), 'projects', pid, 'versions', 'v1');
await t('持ち主はバックアップを作れる', () => assertSucceeds(setDoc(ver('s1', 'pS1'), { versionNum: 1, storageUrl: fileUrl('s1', 'pS1', 'versions/v1.sb3'), createdAt: serverTimestamp(), size: 10 })));
await t('他の子の URL ではバックアップを作れない', () => assertFails(setDoc(ver('s1', 'pS1'), { versionNum: 1, storageUrl: fileUrl('s2', 'pS2', 'versions/v1.sb3'), createdAt: serverTimestamp(), size: 10 })));
await t('id 項目を足してバックアップを作れない', () => assertFails(setDoc(ver('s1', 'pS1'), { versionNum: 1, storageUrl: fileUrl('s1', 'pS1', 'versions/v1.sb3'), createdAt: serverTimestamp(), id: 'v2' })));
const verOk = { versionNum: 1, storageUrl: fileUrl('s1', 'pS1', 'versions/v1.sb3'), createdAt: serverTimestamp(), size: 10 };
await t('大きさを省いたバックアップは作れる', () => assertSucceeds(setDoc(ver('s1', 'pS1'), { versionNum: 1, storageUrl: verOk.storageUrl, createdAt: serverTimestamp() })));
await t('版の番号が小数のバックアップは作れない', () => assertFails(setDoc(ver('s1', 'pS1'), { ...verOk, versionNum: 1.5 })));
await t('版の番号が文字列のバックアップは作れない', () => assertFails(setDoc(ver('s1', 'pS1'), { ...verOk, versionNum: '1' })));
await t('作成日時が文字列のバックアップは作れない', () => assertFails(setDoc(ver('s1', 'pS1'), { ...verOk, createdAt: 'x' })));
await t('大きさが負のバックアップは作れない', () => assertFails(setDoc(ver('s1', 'pS1'), { ...verOk, size: -1 })));
await t('大きさが小数のバックアップは作れない', () => assertFails(setDoc(ver('s1', 'pS1'), { ...verOk, size: 1.5 })));
const vOld = (who) => doc(db(who), 'projects', 'pS1', 'versions', 'vOld');
await t('持ち主は自分の作品の古い履歴を消せる（保存のときの整理）', () => assertSucceeds(deleteDoc(vOld('s1'))));
await t('講師は自教室の作品の履歴を消せる（作品の削除）', () => assertSucceeds(deleteDoc(vOld('t1'))));
await t('管理者は履歴を消せる', () => assertSucceeds(deleteDoc(vOld('admin1'))));
await t('他の子は履歴を消せない', () => assertFails(deleteDoc(vOld('s2'))));
await t('教室 null の講師は履歴を消せない', () => assertFails(deleteDoc(vOld('tnull'))));
await t('他の教室の講師は履歴を消せない', () => assertFails(deleteDoc(vOld('t3'))));
await t('同じ教室の別の生徒は履歴を消せない', () => assertFails(deleteDoc(vOld('s3'))));
await t('未ログインでは履歴を消せない', () => assertFails(deleteDoc(doc(anonDb, 'projects', 'pS1', 'versions', 'vOld'))));
await t('親の作品が無い履歴は、持ち主だった子も講師も消せない（管理者だけ）', async () => {
  await env.withSecurityRulesDisabled((ctx) => deleteDoc(doc(ctx.firestore(), 'projects', 'pS1')));
  await assertFails(deleteDoc(vOld('s1')));
  await assertFails(deleteDoc(vOld('t1')));
  await assertSucceeds(deleteDoc(vOld('admin1')));
});
await t('講師は作品の削除で、履歴から作品まで順に消せる', async () => {
  await assertSucceeds(deleteDoc(vOld('t1')));
  await assertSucceeds(deleteDoc(pr('t1', 'pS1')));
});
await t('他人の作品にはバックアップを作れない', () => assertFails(setDoc(ver('s1', 'pS2'), { versionNum: 1, storageUrl: fileUrl('s2', 'pS2', 'versions/v1.sb3'), createdAt: serverTimestamp() })));

console.log(`\n結果: ${pass} 件成功 / ${fail} 件失敗`);
await env.cleanup();
process.exit(fail ? 1 : 0);

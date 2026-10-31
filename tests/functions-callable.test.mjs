// Cloud Functions（createUser・createUsers・resetPassword）を Node 22 のエミュレーターで呼び、
// 平文のパスワードが生徒の分だけ保存されることを確かめる（エミュレーターのみ・値は架空）
const P = 'demo-fn-test';
const AUTH = 'http://127.0.0.1:9099', FS = `http://127.0.0.1:8085/v1/projects/${P}/databases/(default)/documents`;
const FN = (name) => `http://127.0.0.1:5001/${P}/asia-northeast1/${name}`;
const owner = { Authorization: 'Bearer owner', 'Content-Type': 'application/json' };

async function j(url, opts) { const r = await fetch(url, opts); return { status: r.status, body: await r.json().catch(() => ({})) }; }
async function signUp(email, pw) {
  return (await j(`${AUTH}/identitytoolkit.googleapis.com/v1/accounts:signUp?key=fake`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ email, password: pw, returnSecureToken: true }) })).body;
}
async function setUserDoc(uid, fields) {
  return j(`${FS}/users/${uid}`, { method: 'PATCH', headers: owner, body: JSON.stringify({ fields }) });
}
async function getUserDoc(uid) { return (await j(`${FS}/users/${uid}`, { headers: owner })).body; }
async function call(name, token, data) {
  return j(FN(name), { method: 'POST', headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' }, body: JSON.stringify({ data }) });
}
async function uidOf(userId) {
  const q = await j(`${FS}:runQuery`, { method: 'POST', headers: owner, body: JSON.stringify({ structuredQuery: { from: [{ collectionId: 'users' }], where: { fieldFilter: { field: { fieldPath: 'email' }, op: 'EQUAL', value: { stringValue: `${userId}@laughtale.local` } } } } }) });
  return q.body.find(r => r.document)?.document.name.split('/').pop();
}

let pass = 0, fail = 0;
function check(name, cond, detail) {
  if (cond) { pass++; console.log('  ok  ', name); } else { fail++; console.log('  NG  ', name, JSON.stringify(detail).slice(0, 300)); }
}
const hasPw = (doc) => doc.fields && 'currentPassword' in doc.fields;

// 管理者を用意する
const adm = await signUp('admin1@laughtale.local', 'adminpw1');
await setUserDoc(adm.localId, { role: { stringValue: 'admin' }, classroomId: { nullValue: null }, email: { stringValue: 'admin1@laughtale.local' } });
const T = adm.idToken;

console.log('--- createUser');
let r = await call('createUser', T, { userId: 'stu1', displayName: '生徒1', password: 'pass123', role: 'student', classroomId: 'c1' });
check('生徒を作れる', r.status === 200, r);
const stu1 = await uidOf('stu1');
check('生徒には平文のパスワードを保存する', hasPw(await getUserDoc(stu1)), await getUserDoc(stu1));
r = await call('createUser', T, { userId: 'tea1', displayName: '先生1', password: 'pass123', role: 'teacher', classroomId: 'c1' });
check('講師を作れる', r.status === 200, r);
const tea1 = await uidOf('tea1');
check('講師には平文のパスワードを保存しない', !hasPw(await getUserDoc(tea1)), await getUserDoc(tea1));
r = await call('createUser', T, { userId: 'adm2', displayName: '管理者2', password: 'pass123', role: 'admin' });
check('管理者を作れる', r.status === 200, r);
check('管理者には平文のパスワードを保存しない', !hasPw(await getUserDoc(await uidOf('adm2'))), r);

console.log('--- createUsers（まとめて作る）');
r = await call('createUsers', T, { users: [
  { userId: 'stu2', displayName: '生徒2', password: 'pass123', role: 'student', classroomId: 'c1' },
  { userId: 'tea2', displayName: '先生2', password: 'pass123', role: 'teacher', classroomId: 'c1' },
] });
check('まとめて作れる（2 人とも成功）', r.status === 200 && r.body.result?.success === true && r.body.result?.created === 2 && r.body.result?.failed === 0, r);
const stu2Doc = await getUserDoc(await uidOf('stu2'));
const tea2Uid = await uidOf('tea2');
const tea2Doc = tea2Uid ? await getUserDoc(tea2Uid) : {};
check('まとめて作った生徒には保存する', !!stu2Doc.fields && hasPw(stu2Doc), stu2Doc);
check('まとめて作った講師の記録があり、パスワードは保存しない', !!tea2Doc.fields && tea2Doc.fields.role?.stringValue === 'teacher' && !hasPw(tea2Doc), tea2Doc);

console.log('--- resetPassword');
// 前のコードで作った講師のように、平文のパスワードが残っている状態にする
await setUserDoc(tea1, { role: { stringValue: 'teacher' }, classroomId: { stringValue: 'c1' }, email: { stringValue: 'tea1@laughtale.local' }, currentPassword: { stringValue: 'old-pw' } });
r = await call('resetPassword', T, { targetUid: tea1, newPassword: 'newpass1' });
check('講師のパスワードを変えられる', r.status === 200, r);
check('講師に残っていた平文のパスワードを消す', !hasPw(await getUserDoc(tea1)), await getUserDoc(tea1));
r = await call('resetPassword', T, { targetUid: stu1, newPassword: 'newpass2' });
check('生徒のパスワードを変えられる', r.status === 200, r);
check('生徒には新しいパスワードを保存する', (await getUserDoc(stu1)).fields?.currentPassword?.stringValue === 'newpass2', await getUserDoc(stu1));
const login = await j(`${AUTH}/identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=fake`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ email: 'stu1@laughtale.local', password: 'newpass2', returnSecureToken: true }) });
check('生徒は新しいパスワードでログインできる', login.status === 200, login.status);

console.log('--- 権限');
const tea1Login = (await j(`${AUTH}/identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=fake`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ email: 'tea1@laughtale.local', password: 'newpass1', returnSecureToken: true }) })).body;
r = await call('createUser', tea1Login.idToken, { userId: 'tea3', displayName: '先生3', password: 'pass123', role: 'teacher', classroomId: 'c1' });
check('講師は講師を作れない', r.status !== 200, r.status);
r = await call('createUser', tea1Login.idToken, { userId: 'stu3', displayName: '生徒3', password: 'pass123', role: 'student', classroomId: 'c1' });
check('講師は自教室の生徒を作れる', r.status === 200, r);
const pv = await fetch(`${FN('getPublicVideos')}?limit=1`, { headers: { Origin: 'https://laughtale01.github.io' } });
check('getPublicVideos: ログインなしの GET に応答し、ブラウザーから読める（CORS）',
  pv.status === 200 && pv.headers.get('access-control-allow-origin') === 'https://laughtale01.github.io', { status: pv.status, cors: pv.headers.get('access-control-allow-origin') });

console.log('--- ほかの関数（Node 22 で動くか）');
r = await call('updateUserId', T, { targetUid: stu1, newUserId: 'stu1b' });
check('updateUserId: ログインIDを変えられる', r.status === 200, r);
const stu2 = await uidOf('stu2');
r = await call('deleteUser', T, { targetUid: stu2 });
check('deleteUser: ユーザーを消せる', r.status === 200, r);
check('deleteUser: users 文書も消える', (await j(`${FS}/users/${stu2}`, { headers: owner })).status === 404, stu2);
r = await call('auditUserDocumentCoverage', T, {});
check('auditUserDocumentCoverage: 動く', r.status === 200, r);
// getUploadUrl と saveVideoMetadata は、署名付き URL と公開の設定がエミュレーターでできないので試さない

console.log(`\n結果: ${pass} 件成功 / ${fail} 件失敗`);
process.exit(fail ? 1 : 0);

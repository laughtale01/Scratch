// Cloud Functions（createUser・createUsers・resetPassword）を Node 22 のエミュレーターで呼び、
// 平文のパスワードが生徒の分だけ保存されることを確かめる（エミュレーターのみ・値は架空）
const P = 'demo-fn-test';
const AUTH = 'http://127.0.0.1:9099', FS = `http://127.0.0.1:8085/v1/projects/${P}/databases/(default)/documents`;
const BUCKET = `${P}.appspot.com`;
const ST = 'http://127.0.0.1:9199';
// Storage のエミュレーターにファイルを置く（管理者として。ルールを通らない、署名付き URL と同じ立場）
async function putObject(path, contentType, body = 'x') {
  const r = await fetch(`${ST}/upload/storage/v1/b/${BUCKET}/o?uploadType=media&name=${encodeURIComponent(path)}`, { method: 'POST', headers: { Authorization: 'Bearer owner', 'Content-Type': contentType }, body });
  if (r.status !== 200) throw new Error(`試験のファイルを置けなかった: ${path} (${r.status})`);
  return r.status;
}
async function objectExists(path) {
  const r = await fetch(`${ST}/storage/v1/b/${BUCKET}/o/${encodeURIComponent(path)}`, { headers: { Authorization: 'Bearer owner' } });
  if (r.status !== 200 && r.status !== 404) throw new Error(`ファイルの有無を確かめられなかった: ${path} (${r.status})`);
  return r.status === 200;
}
const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));
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
check('getPublicVideos: ブラウザーから読める（CORS）', pv.headers.get('access-control-allow-origin') === 'https://laughtale01.github.io', { status: pv.status, cors: pv.headers.get('access-control-allow-origin') });

console.log('--- ほかの関数（Node 22 で動くか）');
r = await call('updateUserId', T, { targetUid: stu1, newUserId: 'stu1b' });
check('updateUserId: ログインIDを変えられる', r.status === 200, r);
const stu2 = await uidOf('stu2');
r = await call('deleteUser', T, { targetUid: stu2 });
check('deleteUser: ユーザーを消せる', r.status === 200, r);
check('deleteUser: users 文書も消える', (await j(`${FS}/users/${stu2}`, { headers: owner })).status === 404, stu2);
r = await call('auditUserDocumentCoverage', T, {});
check('auditUserDocumentCoverage: 動く', r.status === 200, r);
console.log('--- 録画（公開しない・決まりに合わないものは断る）');
r = await call('getUploadUrl', T, { contentType: 'text/html' });
check('getUploadUrl: 動画でない種類は断る', r.status === 400, r);
r = await call('getUploadUrl', T, { contentType: 'constructor' });
check('getUploadUrl: Object の組み込みの名前（constructor）は断る', r.status === 400, r);
r = await call('getUploadUrl', T, { contentType: ['video/mp4'] });
check('getUploadUrl: 文字でない種類（配列）は断る', r.status === 400, r);
const ghost = await signUp('ghost1@laughtale.local', 'ghostpw1');
r = await call('getUploadUrl', ghost.idToken, { contentType: 'video/mp4' });
check('getUploadUrl: ユーザー情報の無い人は使えない', r.status === 403, r);
const stuLogin = (await j(`${AUTH}/identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=fake`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ email: 'stu1b@laughtale.local', password: 'newpass2', returnSecureToken: true }) })).body;
const goodPath = `recordings/${stu1}/1700000000000-0a1b2c3d.mp4`;
await putObject(goodPath, 'video/mp4');
// 題名は 100 文字までに切る。100 文字目をまたぐ絵文字（サロゲートペア）が半分に割れないこと
r = await call('saveVideoMetadata', stuLogin.idToken, { filePath: goodPath, title: 'あ'.repeat(99) + '😀😀' });
check('saveVideoMetadata: 正しい録画は保存できる', r.status === 200 && r.body.result?.success === true && !r.body.result?.publicUrl, r);
const vq = await j(`${FS}:runQuery`, { method: 'POST', headers: owner, body: JSON.stringify({ structuredQuery: { from: [{ collectionId: 'videos' }] } }) });
const video = vq.body.find(x => x.document)?.document.fields || {};
check('saveVideoMetadata: 公開しない（status は private、公開の URL を持たない）', video.status?.stringValue === 'private' && !('publicUrl' in video), video);
const savedTitle = video.title?.stringValue || '';
check('saveVideoMetadata: 題名は 100 文字まで（絵文字を割らない）', Array.from(savedTitle).length === 100 && savedTitle.endsWith('😀') && !/[\ud800-\udbff]$/.test(savedTitle), savedTitle.slice(-3));
check('saveVideoMetadata: 生徒の録画には教室IDが付く', video.classroomId?.stringValue === 'c1', video.classroomId);
// 期限のないダウンロード用トークンを外す処理は、エミュレーターがトークンを本番と別の形で持つため、ここでは確かめられない（本番で確かめる）
const numPath = `recordings/${stu1}/1700000000002-0a1b2c3f.mp4`;
await putObject(numPath, 'video/mp4');
r = await call('saveVideoMetadata', stuLogin.idToken, { filePath: numPath, title: 12 });
check('saveVideoMetadata: 数の題名も文字にして受け付ける', r.status === 200 && r.body.result?.success === true, r);
r = await call('saveVideoMetadata', stuLogin.idToken, { filePath: numPath, title: { x: 1 } });
check('saveVideoMetadata: 文字にできない題名は断る', r.status === 400, r);
r = await call('saveVideoMetadata', stuLogin.idToken, { filePath: `recordings/${stu1}/my-own-name.mp4` });
check('saveVideoMetadata: サーバーが決めた形でない名前は断る', r.status === 403, r);
r = await call('saveVideoMetadata', stuLogin.idToken, { filePath: `recordings/${tea1}/1700000000000-0a1b2c3d.mp4` });
check('saveVideoMetadata: 他の人の録画は保存できない', r.status === 403, r);
const badTypePath = `recordings/${stu1}/1700000000001-0a1b2c3e.mp4`;
await putObject(badTypePath, 'text/html', '<script>x</script>');
r = await call('saveVideoMetadata', stuLogin.idToken, { filePath: badTypePath });
let gone = false;
for (let i = 0; i < 20 && !gone; i++) { gone = !(await objectExists(badTypePath)); if (!gone) await sleep(500); }
// cleanupRecordingUpload が先に消すこともあるので、どちらが消しても断られることだけを確かめる
check('動画でない中身の録画は保存できず、ファイルも消える（保存の確かめで 400、掃除が先なら 404）', [400, 404].includes(r.status) && gone, { status: r.status, gone });
const oddPath = `recordings/${stu1}/evil.html`;
await putObject(oddPath, 'text/html', '<script>x</script>');
let removed = false;
for (let i = 0; i < 20 && !removed; i++) { removed = !(await objectExists(oddPath)); if (!removed) await sleep(500); }
check('cleanupRecordingUpload: 決まりに合わないアップロードは、保存を呼ばなくても消える', removed, oddPath);
check('cleanupRecordingUpload: 正しい録画は消さない', await objectExists(goodPath), goodPath);

const listUrl = `${FN('getPublicVideos')}?limit=5`;
let lr = await fetch(listUrl);
check('getPublicVideos: ログインなしでは取れない', lr.status === 401, lr.status);
lr = await fetch(listUrl, { headers: { Authorization: `Bearer ${stuLogin.idToken}` } });
check('getPublicVideos: 生徒は取れない', lr.status === 403, lr.status);
const t9 = await signUp('t9@laughtale.local', 't9pass1');
await setUserDoc(t9.localId, { role: { stringValue: 'teacher' }, classroomId: { stringValue: 'c9' }, email: { stringValue: 't9@laughtale.local' } });
lr = await fetch(listUrl, { headers: { Authorization: `Bearer ${t9.idToken}` } });
const t9Body = await lr.json().catch(() => ({}));
check('getPublicVideos: 講師は自分の教室の分だけ（他の教室の録画は出ない）', lr.status === 200 && t9Body.count === 0, { status: lr.status, body: t9Body });
const tEmpty = await signUp('tempty@laughtale.local', 'temptypw1');
await setUserDoc(tEmpty.localId, { role: { stringValue: 'teacher' }, classroomId: { stringValue: '' }, email: { stringValue: 'tempty@laughtale.local' } });
lr = await fetch(listUrl, { headers: { Authorization: `Bearer ${tEmpty.idToken}` } });
check('getPublicVideos: 教室IDが空の講師は取れない（全教室が見えないように）', lr.status === 403, lr.status);
lr = await fetch(listUrl, { headers: { Authorization: 'Bearer not-a-token' } });
check('getPublicVideos: ID トークンが正しくなければ取れない', lr.status === 401, lr.status);
// 自教室の講師と管理者が録画を見られる場合は、署名付き URL を作るため本番の資格情報が要り、エミュレーターでは試せない

console.log(`\n結果: ${pass} 件成功 / ${fail} 件失敗`);
process.exit(fail ? 1 : 0);

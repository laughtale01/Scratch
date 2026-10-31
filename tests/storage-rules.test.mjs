// storage.rules の試験（Firestore と Storage のエミュレーターだけで動かす。値は架空）
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { initializeTestEnvironment, assertSucceeds, assertFails } from '@firebase/rules-unit-testing';
import { doc, setDoc } from 'firebase/firestore';
import { ref, uploadString, getBytes, deleteObject } from 'firebase/storage';

// 試験するルールのファイル（既定はこの試験ファイルから見た ../storage.rules。環境変数 SRULES で差し替えられる）
const SRULES = process.env.SRULES || fileURLToPath(new URL('../storage.rules', import.meta.url));
const env = await initializeTestEnvironment({
  projectId: 'demo-rules-test',
  // Firestore はデータを置くだけ（Storage のルールから firestore.get で読まれる）
  firestore: { rules: 'rules_version = "2"; service cloud.firestore { match /databases/{d}/documents { match /{p=**} { allow read, write: if false; } } }', host: '127.0.0.1', port: 8085 },
  storage: { rules: readFileSync(SRULES, 'utf8'), host: '127.0.0.1', port: 9199 },
});

const USERS = {
  admin1: { role: 'admin', classroomId: null },
  t1: { role: 'teacher', classroomId: 'c1' },
  t2: { role: 'teacher', classroomId: 'c1' },
  t3: { role: 'teacher', classroomId: 'c2' },
  tnull: { role: 'teacher', classroomId: null },
  s1: { role: 'student', classroomId: 'c1' },
  s2: { role: 'student', classroomId: 'c2' },
  snull: { role: 'student', classroomId: null },
};
const PROJECTS = {
  pS1: { userId: 's1', classroomId: 'c1' },
  pS2: { userId: 's2', classroomId: 'c2' },
  pNull: { userId: 'snull', classroomId: null },
  pT2: { userId: 't2', classroomId: null },
  pMoved: { userId: 's2', classroomId: 'c1' },
};
const FILES = [
  'projects/s1/pS1/current.sb3', 'projects/s1/pS1/versions/v1.sb3', 'thumbnails/s1/pS1.png',
  'projects/s2/pS2/current.sb3', 'thumbnails/s2/pS2.png',
  'projects/snull/pNull/current.sb3', 'thumbnails/snull/pNull.png',
  'projects/sX/pS1/current.sb3', 'projects/s1/pNone/current.sb3',
  'thumbnails/t2/pT.png', 'recordings/s1/x.mp4',
  'thumbnails/s1/pS1.extra.png', 'thumbnails/s1/pS1.PNG',
  'projects/t2/pT2/current.sb3', 'thumbnails/t2/pT2.png', 'thumbnails/s2/pMoved.png', 'thumbnails/s1/odd-name.png',
];

async function reset() {
  await env.clearFirestore();
  await env.clearStorage();
  await env.withSecurityRulesDisabled(async (ctx) => {
    const fs = ctx.firestore();
    for (const [id, d] of Object.entries(USERS)) await setDoc(doc(fs, 'users', id), d);
    for (const [id, d] of Object.entries(PROJECTS)) await setDoc(doc(fs, 'projects', id), d);
    const st = ctx.storage();
    for (const path of FILES) await uploadString(ref(st, path), 'x');
  });
}

const sts = {};
const st = (uid) => (sts[uid] ??= env.authenticatedContext(uid).storage());
const anon = env.unauthenticatedContext().storage();
const read = (who, path) => getBytes(ref(who === null ? anon : st(who), path));
const del = (who, path) => deleteObject(ref(st(who), path));
const put = (who, path) => uploadString(ref(st(who), path), 'y');

let pass = 0, fail = 0;
async function t(name, fn) {
  await reset();
  try { await fn(); pass++; console.log('  ok  ', name); }
  catch (e) { fail++; console.log('  NG  ', name, '-', String(e.message).split('\n')[0].slice(0, 120)); }
}

console.log('--- 作品のファイル（持ち主）');
await t('持ち主は自分のファイルを読める', () => assertSucceeds(read('s1', 'projects/s1/pS1/current.sb3')));
await t('持ち主は自分のファイルを上書きできる', () => assertSucceeds(put('s1', 'projects/s1/pS1/current.sb3')));
await t('持ち主は新しい作品のファイルを置ける（作品の文書より先）', () => assertSucceeds(put('s1', 'projects/s1/pNew/current.sb3')));
await t('持ち主は自分のファイルを消せる', () => assertSucceeds(del('s1', 'projects/s1/pS1/versions/v1.sb3')));
await t('他の子のファイルは読めない', () => assertFails(read('s2', 'projects/s1/pS1/current.sb3')));
await t('他の子のファイルは消せない', () => assertFails(del('s2', 'projects/s1/pS1/current.sb3')));
await t('未ログインでは読めない', () => assertFails(read(null, 'projects/s1/pS1/current.sb3')));

console.log('--- 作品のファイル（講師・管理者）');
await t('講師は自教室の生徒のファイルを読める', () => assertSucceeds(read('t1', 'projects/s1/pS1/current.sb3')));
await t('講師は自教室の生徒のファイルを消せる', () => assertSucceeds(del('t1', 'projects/s1/pS1/current.sb3')));
await t('講師は自教室の生徒の履歴のファイルも消せる', () => assertSucceeds(del('t1', 'projects/s1/pS1/versions/v1.sb3')));
await t('講師は生徒のファイルを上書きできない', () => assertFails(put('t1', 'projects/s1/pS1/current.sb3')));
await t('講師は生徒の場所に新しいファイルを置けない', () => assertFails(put('t1', 'projects/s1/pS1/evil.sb3')));
await t('講師は他教室の生徒のファイルを読めない', () => assertFails(read('t1', 'projects/s2/pS2/current.sb3')));
await t('講師は他教室の生徒のファイルを消せない', () => assertFails(del('t1', 'projects/s2/pS2/current.sb3')));
await t('作品の持ち主と場所の持ち主が違えば、講師は読めない', () => assertFails(read('t1', 'projects/sX/pS1/current.sb3')));
await t('作品の文書が無い場所は、講師は読めない', () => assertFails(read('t1', 'projects/s1/pNone/current.sb3')));
await t('教室 null の講師は、教室 null の作品のファイルを読めない', () => assertFails(read('tnull', 'projects/snull/pNull/current.sb3')));
await t('管理者はどの作品のファイルも読める', () => assertSucceeds(read('admin1', 'projects/s2/pS2/current.sb3')));
await t('管理者はどの作品のファイルも消せる', () => assertSucceeds(del('admin1', 'projects/s2/pS2/current.sb3')));
await t('管理者も生徒のファイルを上書きできない', () => assertFails(put('admin1', 'projects/s1/pS1/current.sb3')));

console.log('--- サムネイル');
await t('持ち主は自分のサムネイルを置ける', () => assertSucceeds(put('s1', 'thumbnails/s1/pS1.png')));
await t('講師は自教室の生徒のサムネイルを読める', () => assertSucceeds(read('t1', 'thumbnails/s1/pS1.png')));
await t('講師は自教室の生徒のサムネイルを消せる', () => assertSucceeds(del('t1', 'thumbnails/s1/pS1.png')));
await t('講師は生徒のサムネイルを上書きできない', () => assertFails(put('t1', 'thumbnails/s1/pS1.png')));
await t('講師は他教室の生徒のサムネイルを読めない', () => assertFails(read('t1', 'thumbnails/s2/pS2.png')));
await t('講師は同じ教室の別の講師のサムネイルを読めない', () => assertFails(read('t1', 'thumbnails/t2/pT.png')));
await t('教室 null の講師は、教室 null の生徒のサムネイルを読めない', () => assertFails(read('tnull', 'thumbnails/snull/pNull.png')));
await t('他の子のサムネイルは読めない', () => assertFails(read('s2', 'thumbnails/s1/pS1.png')));

await t('持ち主はまだ無いサムネイルを新しく置ける（作品の文書より先）', () => assertSucceeds(put('s1', 'thumbnails/s1/pNew.png')));
await t('講師はまだ無いサムネイルを置けない', () => assertFails(put('t1', 'thumbnails/s1/pNew.png')));
await t('管理者もまだ無いサムネイルを置けない', () => assertFails(put('admin1', 'thumbnails/s1/pNew.png')));
await t('他の子はまだ無いサムネイルを置けない', () => assertFails(put('s2', 'thumbnails/s1/pNew.png')));
await t('教室を移った生徒の昔の作品のサムネイルは、作品の教室の講師が読める', () => assertSucceeds(read('t1', 'thumbnails/s2/pMoved.png')));
await t('教室を移った生徒の昔の作品のサムネイルは、今の教室の講師は読めない', () => assertFails(read('t3', 'thumbnails/s2/pMoved.png')));
await t('決まった名前でないサムネイルは、講師は読めない', () => assertFails(read('t1', 'thumbnails/s1/odd-name.png')));
await t('作品も持ち主も教室も正しくても、名前に点が 2 つあれば講師は読めない', () => assertFails(read('t1', 'thumbnails/s1/pS1.extra.png')));
await t('作品も持ち主も教室も正しくても、拡張子が大文字なら講師は読めない', () => assertFails(read('t1', 'thumbnails/s1/pS1.PNG')));
await t('管理者は生徒のサムネイルを消せる', () => assertSucceeds(del('admin1', 'thumbnails/s2/pS2.png')));

console.log('--- 講師が作った作品');
await t('同じ教室の別の講師は、講師が作った作品のファイルを読めない', () => assertFails(read('t1', 'projects/t2/pT2/current.sb3')));
await t('同じ教室の別の講師は、講師が作った作品のファイルを消せない', () => assertFails(del('t1', 'projects/t2/pT2/current.sb3')));
await t('同じ教室の別の講師は、講師が作った作品のサムネイルを読めない', () => assertFails(read('t1', 'thumbnails/t2/pT2.png')));
await t('作った講師本人は読める', () => assertSucceeds(read('t2', 'projects/t2/pT2/current.sb3')));
await t('管理者は講師が作った作品も読める', () => assertSucceeds(read('admin1', 'projects/t2/pT2/current.sb3')));

console.log('--- 録画（変更なし）');
await t('持ち主は自分の録画を読める', () => assertSucceeds(read('s1', 'recordings/s1/x.mp4')));
await t('管理者は録画を読める', () => assertSucceeds(read('admin1', 'recordings/s1/x.mp4')));
await t('講師は録画を読めない', () => assertFails(read('t1', 'recordings/s1/x.mp4')));

console.log('--- それ以外');
await t('決まった場所以外には置けない', () => assertFails(put('s1', 'other/s1/x.txt')));

console.log(`\n結果: ${pass} 件成功 / ${fail} 件失敗`);
await env.cleanup();
process.exit(fail ? 1 : 0);

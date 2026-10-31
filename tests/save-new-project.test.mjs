// firebase-integration.js の新規保存（ScratchFirebase.saveNewProject）が送る教室IDを、偽物の SDK で確かめる（値は架空）
import { readFileSync } from 'node:fs';
import vm from 'node:vm';

// 試す firebase-integration.js（既定はこの試験ファイルから見た ../firebase-integration.js。環境変数 FI_JS で差し替えられる）
const src = readFileSync(process.env.FI_JS || new URL('../firebase-integration.js', import.meta.url), 'utf8');

async function save(userData) {
  const written = [];
  const storage = {
    ref: (path) => ({ put: async () => {}, getDownloadURL: async () => `https://example.test/${path}` }),
  };
  const db = {
    collection: (c) => ({
      doc: (id) => ({
        id: id || 'pNew',
        set: async (data) => { written.push({ c, data }); },
        update: async () => {},
      }),
    }),
  };
  const ctx = {
    console: { log() {}, warn() {}, error() {} }, setTimeout() {}, alert() {},
    document: { readyState: 'loading', addEventListener() {}, getElementById: () => null },
    window: { location: { search: '', href: 'https://example.test/' } },
    URLSearchParams,
    firebase: {
      initializeApp() {}, auth: () => ({ onAuthStateChanged() {} }),
      firestore: Object.assign(() => db, { FieldValue: { serverTimestamp: () => 'ts', increment: (n) => n } }),
      storage: () => storage,
    },
  };
  vm.createContext(ctx);
  vm.runInContext(src, ctx);
  const sf = ctx.window.scratchFirebase;
  sf.currentUser = { uid: 'u1' };
  sf.userData = userData;
  const res = await sf.saveNewProject('作品', { size: 10 });
  const project = written.find(w => w.c === 'projects');
  return { res, classroomId: project ? project.data.classroomId : undefined };
}

let pass = 0, fail = 0;
function check(name, cond, detail) {
  if (cond) { pass++; console.log('  ok  ', name); } else { fail++; console.log('  NG  ', name, JSON.stringify(detail)); }
}

let r = await save({ role: 'student', classroomId: 'c1' });
check('生徒: 自分の教室IDで作る', r.res.success && r.classroomId === 'c1', r);
r = await save({ role: 'student', classroomId: null });
check('教室の無い生徒: null で作る', r.res.success && r.classroomId === null, r);
r = await save({ role: 'teacher', classroomId: 'c1' });
check('講師: 教室IDを付けない（null）', r.res.success && r.classroomId === null, r);
r = await save({ role: 'admin', classroomId: 'c1' });
check('管理者: 教室IDを付けない（null）', r.res.success && r.classroomId === null, r);
r = await save(null);
check('利用者の情報の読み込み前: null で作る', r.res.success && r.classroomId === null, r);

console.log(`\n結果: ${pass} 件成功 / ${fail} 件失敗`);
process.exit(fail ? 1 : 0);

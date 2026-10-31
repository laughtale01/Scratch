// firebase-integration.js の生徒の削除（ScratchFirebase.deleteProject）を、偽物の SDK で動かして確かめる
// （提出済みで止まること、記録をまとめて消してからファイルを消すこと、失敗したときの残り方。値は架空）
import { readFileSync } from 'node:fs';
import vm from 'node:vm';

// 試す firebase-integration.js（既定はこの試験ファイルから見た ../firebase-integration.js。環境変数 FI_JS で差し替えられる）
const src = readFileSync(process.env.FI_JS || new URL('../firebase-integration.js', import.meta.url), 'utf8');

function setup({ project, versions = [], commitDenied = false, storageFail = new Set(), storageMissing = new Set() }) {
  const events = [], warns = [];
  const store = { project: project ? { ...project } : null, versions: [...versions] };
  const storage = {
    ref: (path) => ({
      fullPath: path,
      async delete() {
        events.push(`storage:${path}`);
        if (storageFail.has(path)) { const e = new Error('denied'); e.code = 'storage/unauthorized'; throw e; }
        if (storageMissing.has(path)) { const e = new Error('not found'); e.code = 'storage/object-not-found'; throw e; }
      },
    }),
  };
  const projectRef = {
    __path: 'project',
    get: async () => ({ exists: !!store.project, data: () => store.project }),
    collection: () => ({
      get: async () => ({ docs: store.versions.map((v, i) => ({ data: () => v, ref: { __path: `version:${i}` } })) }),
    }),
    delete: async () => { events.push('single:project'); },
  };
  const db = {
    collection: () => ({ doc: () => projectRef }),
    batch: () => {
      const ops = [];
      return {
        delete: (ref) => { ops.push(ref.__path); },
        async commit() {
          events.push('commit');
          if (commitDenied) { const e = new Error('Missing or insufficient permissions.'); e.code = 'permission-denied'; throw e; }
          if (ops.includes('project')) store.project = null;
          store.versions = store.versions.filter((_, i) => !ops.includes(`version:${i}`));
        },
      };
    },
  };
  const ctx = {
    console: { log() {}, warn: (...a) => warns.push(a.map(String).join(' ')), error() {} },
    setTimeout() {}, alert() {}, confirm: () => true,
    document: { readyState: 'loading', addEventListener() {}, getElementById: () => null, createElement: () => ({ style: {} }) },
    window: { location: { search: '', href: 'https://example.test/' } },
    URLSearchParams,
    firebase: {
      initializeApp() {},
      auth: () => ({ onAuthStateChanged() {} }),
      firestore: Object.assign(() => db, { FieldValue: { serverTimestamp: () => 'ts', increment: (n) => n } }),
      storage: () => storage,
    },
  };
  ctx.window.document = ctx.document;
  vm.createContext(ctx);
  vm.runInContext(src, ctx);
  const sf = ctx.window.scratchFirebase;
  sf.currentUser = { uid: 's1' };
  return { sf, events, warns, store };
}

let pass = 0, fail = 0;
function check(name, cond, detail) {
  if (cond) { pass++; console.log('  ok  ', name); } else { fail++; console.log('  NG  ', name, JSON.stringify(detail)); }
}
const base = { userId: 's1', classroomId: 'c1', isSubmitted: false };

{
  const r = setup({ project: base, versions: [{ versionNum: 1 }, { versionNum: 2 }] });
  const res = await r.sf.deleteProject('pA');
  check('未提出: 成功する', res.success === true, res);
  const commitAt = r.events.indexOf('commit');
  check('未提出: 記録をまとめて消してから、ファイルを消す', commitAt === 0 && r.events.slice(1).every(e => e.startsWith('storage:')), r.events);
  check('未提出: 本体・版 2 つ・サムネイルを消す', ['projects/s1/pA/current.sb3', 'projects/s1/pA/versions/v1.sb3', 'projects/s1/pA/versions/v2.sb3', 'thumbnails/s1/pA.png']
    .every(p => r.events.includes(`storage:${p}`)), r.events);
  check('未提出: 1 件ずつの削除は使わない', !r.events.some(e => e.startsWith('single:')), r.events);
  check('未提出: 作品と履歴の記録が消える', r.store.project === null && r.store.versions.length === 0, r.store);
}
{
  const r = setup({ project: { ...base, isSubmitted: true }, versions: [{ versionNum: 1 }] });
  const res = await r.sf.deleteProject('pA');
  check('提出済み: 止まって知らせる', res.success === false && res.error.includes('提出を取り消してから'), res);
  check('提出済み: 記録にもファイルにも触らない', r.events.length === 0, r.events);
}
{
  const r = setup({ project: null });
  const res = await r.sf.deleteProject('pA');
  check('作品が無い: 止まって、何も消さない', res.success === false && r.events.length === 0, { res, events: r.events });
}
{
  // 未提出と読んだ直後に、別のタブで提出された場合（まとめ書きがルールで拒否される）
  const r = setup({ project: base, versions: [{ versionNum: 1 }], commitDenied: true });
  const res = await r.sf.deleteProject('pA');
  check('まとめ書きが拒否された: 失敗を返す', res.success === false, res);
  check('まとめ書きが拒否された: ファイルには触らない', !r.events.some(e => e.startsWith('storage:')), r.events);
  check('まとめ書きが拒否された: 記録も履歴も残る', r.store.project !== null && r.store.versions.length === 1, r.store);
}
{
  const r = setup({ project: base, versions: [{ versionNum: 1 }], storageFail: new Set(['projects/s1/pA/current.sb3']) });
  const res = await r.sf.deleteProject('pA');
  check('ファイル 1 つの削除に失敗: 成功を返す（記録は消えている）', res.success === true, res);
  check('ファイル 1 つの削除に失敗: 残りのファイルも消しにいく', r.events.includes('storage:thumbnails/s1/pA.png') && r.events.includes('storage:projects/s1/pA/versions/v1.sb3'), r.events);
  check('ファイル 1 つの削除に失敗: 警告を残す', r.warns.some(w => w.includes('current.sb3')), r.warns);
  check('ファイル 1 つの削除に失敗: 記録は消えている', r.store.project === null && r.store.versions.length === 0, r.store);
}
{
  const r = setup({ project: base, storageMissing: new Set(['thumbnails/s1/pA.png']) });
  const res = await r.sf.deleteProject('pA');
  check('サムネイルが無い: 警告を出さずに成功する', res.success === true && r.warns.length === 0, { res, warns: r.warns });
}

console.log(`\n結果: ${pass} 件成功 / ${fail} 件失敗`);
process.exit(fail ? 1 : 0);

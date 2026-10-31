// admin.js の AdminPanel.deleteProject を、Storage と Firestore の偽物で動かして確かめる
// （実際に消そうとした Storage の場所と、文書の削除が最後まで進むかを見る。値は架空）
import { readFileSync } from 'node:fs';
import vm from 'node:vm';

const BUCKET = 'laughtale-scratch-bcc8a.firebasestorage.app';
const url = (path, bucket = BUCKET) =>
  `https://firebasestorage.googleapis.com/v0/b/${bucket}/o/${encodeURIComponent(path)}?alt=media&token=x`;

// 偽物の Storage（SDK と同じく、URL の /o/ の後ろをデコードしてパスにする）
// missing: 無いファイル、denied: 権限で拒否されるファイル（今の本番の Storage のルールの動き）
function makeStorage(deleted, missing = new Set(), denied = new Set(), tried = [], events = []) {
  const ref = (bucket, fullPath) => ({
    bucket, fullPath,
    async delete() {
      tried.push(fullPath);
      events.push(`storage:${fullPath}`);
      if (denied.has(fullPath)) { const e = new Error('denied'); e.code = 'storage/unauthorized'; throw e; }
      if (missing.has(fullPath)) { const e = new Error('not found'); e.code = 'storage/object-not-found'; throw e; }
      deleted.push(`${bucket}:${fullPath}`);
    },
  });
  return {
    ref: (path = '') => ref(BUCKET, path),
    refFromURL(u) {
      const m = /^https:\/\/firebasestorage\.googleapis\.com\/v0\/b\/([^/]+)\/o\/([^?#]*)/.exec(u);
      if (!m) throw new Error('refFromURL: 形が違う');
      return ref(m[1], decodeURIComponent(m[2]));
    },
  };
}

// 偽物の Firestore（versions の一覧と、文書の削除だけ。まとめ書きは commit のときに順に消す）
function makeDb(versions, docDeletes, events = [], commitFail = { count: 0, beforeCommit: null }) {
  // 文書を 1 件ずつ消したら印を残す（まとめ書きから 1 件ずつの削除に戻ったら試験で分かるように）
  const docRef = (path) => ({ __path: path, delete: async () => { docDeletes.push(path); events.push(`single:${path}`); } });
  return {
    batch: () => {
      const ops = [];
      return { delete: (ref) => { ops.push(ref.__path); }, commit: async () => {
        events.push('commit');
        if (commitFail.beforeCommit) await commitFail.beforeCommit();
        if (commitFail.count > 0) { commitFail.count--; const e = new Error('network'); e.code = 'unavailable'; throw e; }
        docDeletes.push(...ops);
      } };
    },
    collection: (c) => ({
      doc: (id) => ({
        ...docRef(`${c}/${id}`),
        collection: () => ({
          get: async () => ({ docs: versions.map((v, i) => ({ data: () => v, ref: docRef(`${c}/${id}/versions/${i}`) })) }),
        }),
      }),
    }),
  };
}

// 試す admin.js（既定はこの試験ファイルから見た ../admin.js。環境変数 ADMIN_JS で差し替えられる）
const src = readFileSync(process.env.ADMIN_JS || new URL('../admin.js', import.meta.url), 'utf8');

async function run(project, versions, missing, denied, { commitFailures = 0, duringCommit = null } = {}) {
  const deleted = [], docDeletes = [], warns = [], tried = [], events = [], alerts = [], closed = [];
  const commitFail = { count: commitFailures, beforeCommit: null };
  const storage = makeStorage(deleted, missing, denied, tried, events);
  const ctx = {
    console: { log() {}, warn: (...a) => warns.push(a.map(String).join(' ')), error: (...a) => warns.push('ERROR ' + a.join(' ')) },
    alert: (m) => alerts.push(String(m)), document: { addEventListener() {}, getElementById: () => null },
    window: {},
    firebase: {
      initializeApp() {}, auth: () => ({}), firestore: () => makeDb(versions, docDeletes, events, commitFail), storage: () => storage,
      functions: () => ({}), app: () => ({ functions: () => ({ httpsCallable: () => () => {} }) }),
    },
  };
  vm.createContext(ctx);
  vm.runInContext(src + '\n;globalThis.__AdminPanel = AdminPanel;', ctx);
  const AP = ctx.__AdminPanel;
  AP.projects = [project];
  let loaded = 0;
  // 本物の closeModal と同じく、確認の画面を閉じたら削除の予定も消す
  AP.closeModal = (id) => { closed.push(id); if (id === 'confirmDeleteModal') AP.pendingDeleteAction = null; };
  AP.loadProjects = async () => { loaded++; }; AP.updateStats = () => {};
  if (duringCommit) commitFail.beforeCommit = async () => duringCommit(AP);
  // 確認の画面の「削除」ボタンと同じく、予定を空にしてから実行する
  AP.pendingDeleteAction = null;
  await AP.deleteProject(project.id);
  return { deleted, docDeletes, warns, tried, events, alerts, closed, loaded, pending: AP.pendingDeleteAction };
}

let pass = 0, fail = 0;
function check(name, cond, detail) {
  if (cond) { pass++; console.log('  ok  ', name); } else { fail++; console.log('  NG  ', name, JSON.stringify(detail)); }
}

const base = { id: 'pA', userId: 'sA', storageUrl: url('projects/sA/pA/current.sb3'), thumbnailUrl: url('thumbnails/sA/pA.png') };
const ver = (n, owner = 'sA', pid = 'pA') => ({ storageUrl: url(`projects/${owner}/${pid}/versions/v${n}.sb3`) });

{
  const r = await run(base, [ver(1), ver(2)]);
  check('ふつうの作品: 本体・サムネイル・版 2 つを消す', r.deleted.length === 4 &&
    r.deleted.includes(`${BUCKET}:projects/sA/pA/current.sb3`) && r.deleted.includes(`${BUCKET}:thumbnails/sA/pA.png`) &&
    r.deleted.includes(`${BUCKET}:projects/sA/pA/versions/v1.sb3`) && r.deleted.includes(`${BUCKET}:projects/sA/pA/versions/v2.sb3`), r.deleted);
  check('ふつうの作品: 文書（版 2 つと作品）を消す', r.docDeletes.length === 3 && r.docDeletes.at(-1) === 'projects/pA', r.docDeletes);
  check('ふつうの作品: 警告は出ない', r.warns.length === 0, r.warns);
  const commitAt = r.events.indexOf('commit');
  check('ふつうの作品: Storage のファイルを全部消してから、文書をまとめて消す',
    commitAt > 0 && r.events.slice(commitAt + 1).every(e => !e.startsWith('storage:')) && !r.events.some(e => e.startsWith('single:')), r.events);
}
{
  const r = await run({ ...base, storageUrl: url('projects/sA/pA/versions/v1.sb3') }, [ver(1)]);
  check('復元のあと: current.sb3 も消す（残らない）', r.deleted.includes(`${BUCKET}:projects/sA/pA/current.sb3`), r.deleted);
  check('復元のあと: 同じ版のファイルを二度消さない', r.deleted.filter(d => d.endsWith('v1.sb3')).length === 1, r.deleted);
}
{
  const r = await run({ ...base, storageUrl: url('projects/sB/pB/current.sb3'), thumbnailUrl: url('thumbnails/sB/pB.png') }, [ver(1, 'sB', 'pB')]);
  check('他の子の URL が書かれていても、他の子のファイルは消さない', !r.deleted.some(d => d.includes('/sB/')), r.deleted);
  check('他の子の URL のときも、自分の決まった場所は消す', r.deleted.includes(`${BUCKET}:projects/sA/pA/current.sb3`) &&
    r.deleted.includes(`${BUCKET}:thumbnails/sA/pA.png`), r.deleted);
  check('他の子の URL のときも、文書の削除は最後まで進む', r.docDeletes.at(-1) === 'projects/pA', r.docDeletes);
}
{
  const r = await run({ ...base, storageUrl: url('projects/sA/pA/current.sb3', 'other-bucket') }, []);
  check('別のバケットの URL は消さない', !r.deleted.some(d => d.startsWith('other-bucket')), r.deleted);
}
{
  const r = await run({ ...base, storageUrl: url('projects/sA/pA/other.sb3') }, [{ storageUrl: url('projects/sA/pA/versions/v1.sb3/../../x') }]);
  check('決まった名前以外のファイルや ../ は消さない', !r.deleted.some(d => d.endsWith('other.sb3') || d.includes('..')), r.deleted);
}
{
  const r = await run({ ...base, thumbnailUrl: null }, [], new Set(['thumbnails/sA/pA.png']));
  check('サムネイルが無くても警告を出さずに進む', r.tried.includes('thumbnails/sA/pA.png') &&
    !r.warns.some(w => w.includes('サムネイル')) && r.docDeletes.at(-1) === 'projects/pA', { tried: r.tried, warns: r.warns, docs: r.docDeletes });
}
{
  const r = await run({ ...base, thumbnailUrl: null }, []);
  check('サムネイルの URL が空でも、決まった場所のサムネイルは消す', r.deleted.includes(`${BUCKET}:thumbnails/sA/pA.png`), r.deleted);
}
{
  const r = await run(base, [ver(1)], new Set(['projects/sA/pA/current.sb3']));
  check('本体のファイルが無くても警告を出さずに進む', !r.warns.some(w => w.includes('Storage')) && r.docDeletes.at(-1) === 'projects/pA', { warns: r.warns, docs: r.docDeletes });
}
{
  // 今の本番の Storage のルールは、講師・管理者の他人のファイル操作を拒否する
  const denied = new Set(['projects/sA/pA/current.sb3', 'thumbnails/sA/pA.png', 'projects/sA/pA/versions/v1.sb3']);
  const r = await run(base, [ver(1)], new Set(), denied);
  check('権限で拒否されたら警告を出す', r.warns.some(w => w.includes('Storageファイル')) && r.warns.some(w => w.includes('サムネイル')) &&
    r.warns.some(w => w.includes('バージョンファイル')), r.warns);
  check('権限で拒否されても、同じ場所を二度は試さない', r.tried.filter(p => p.endsWith('current.sb3')).length === 1, r.tried);
  check('権限で拒否されても、文書の削除は最後まで進む', r.docDeletes.at(-1) === 'projects/pA', r.docDeletes);
}
{
  const r = await run({ ...base, userId: '' }, [ver(1)]);
  check('持ち主が空なら、Storage は何も消さない', r.deleted.length === 0, r.deleted);
}

{
  const r = await run(base, [ver(1)], new Set(), new Set(), { commitFailures: 1 });
  check('まとめ書きが失敗: 一覧からもう一度削除するよう知らせる', r.alerts.some(a => a.includes('一覧からもう一度削除してください')), r.alerts);
  check('まとめ書きが失敗: 確認の画面を閉じ、一覧を読み直す', r.closed.includes('confirmDeleteModal') && r.loaded === 1, { closed: r.closed, loaded: r.loaded });
  check('まとめ書きが失敗: 同じ確認の画面からのやり直しは残さない', r.pending === null, r.pending);
}
{
  // 作品の削除の途中で、確認の画面を閉じて別のユーザーの削除の確認を開いた場合
  const userDelete = () => 'deleteUser(B)';
  const r = await run(base, [ver(1)], new Set(), new Set(), { commitFailures: 1, duringCommit: (AP) => { AP.pendingDeleteAction = userDelete; } });
  check('途中で別の確認を開いた（失敗）: その確認の予定を上書きしない', r.pending === userDelete, String(r.pending));
  check('途中で別の確認を開いた（失敗）: その確認の画面を閉じない', !r.closed.includes('confirmDeleteModal'), r.closed);
}
{
  const userDelete = () => 'deleteUser(B)';
  const r = await run(base, [ver(1)], new Set(), new Set(), { duringCommit: (AP) => { AP.pendingDeleteAction = userDelete; } });
  check('途中で別の確認を開いた（成功）: その確認の画面を閉じない', !r.closed.includes('confirmDeleteModal') && r.pending === userDelete, { closed: r.closed });
  check('途中で別の確認を開いた（成功）: 作品は消えている', r.docDeletes.at(-1) === 'projects/pA', r.docDeletes);
}

console.log(`\n結果: ${pass} 件成功 / ${fail} 件失敗`);
process.exit(fail ? 1 : 0);

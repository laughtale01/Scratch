// Minecraft のカテゴリーのブロック（gui.js の Minecraft 拡張）が、MOD へ正しい命令を送るかの試験。
// 本物の Chrome を見えない形で動かし、リポジトリの index.html と gui.js を 127.0.0.1 の小さなサーバーから開く。
// ページの中の WebSocket を偽物の MOD に差し替える（外へは何もつながない）。送られた命令を記録し、問い合わせには決まった返事をする。
// 本番にはつながない: 127.0.0.1 以外への通信と firebase-integration.js は空の中身に差し替える。
// 使い方: node tests/minecraft-blocks.test.mjs
//   CHROME_PATH   … Chrome の実行ファイルの場所（既定は Mac の /Applications/Google Chrome.app/Contents/MacOS/Google Chrome）。
//                   無ければ試験を飛ばし、終わりのコード 77 を返す
//   BLOCKS_GUI_JS … 試す gui.js（既定はリポジトリの gui.js）
import { chromium } from 'playwright-core';
import { createServer } from 'node:http';
import { existsSync, readFileSync, statSync } from 'node:fs';
import { extname, join, normalize, sep } from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = fileURLToPath(new URL('..', import.meta.url));
const GUI_JS = process.env.BLOCKS_GUI_JS || join(ROOT, 'gui.js');
const CHROME = process.env.CHROME_PATH || '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome';
if (!existsSync(CHROME)) {
  console.log(`Chrome が見つからないので、この試験は飛ばします（${CHROME}。CHROME_PATH で指定できます）`);
  process.exit(77); // 77 は「飛ばした」（run-all.sh が成功・しっぱいと分けて出す）
}

// リポジトリの中のファイルだけを返す小さなサーバー（palette-layout.test.mjs と同じ決まり）
const TYPES = { '.html': 'text/html; charset=utf-8', '.js': 'text/javascript; charset=utf-8', '.css': 'text/css', '.json': 'application/json',
  '.svg': 'image/svg+xml', '.png': 'image/png', '.jpg': 'image/jpeg', '.gif': 'image/gif', '.ico': 'image/x-icon',
  '.wav': 'audio/wav', '.mp3': 'audio/mpeg', '.woff': 'font/woff', '.woff2': 'font/woff2', '.ttf': 'font/ttf' };
let HOST = '';
const server = createServer((req, res) => {
  let path;
  try {
    path = decodeURIComponent(new URL(req.url, 'http://x').pathname);
  } catch (e) {
    res.writeHead(400); res.end(); return; // 壊れたアドレスでは落ちずに断る
  }
  if (req.headers.host !== HOST) { res.writeHead(403); res.end(); return; } // 自分宛てでない要求は断る
  // .env・.git など「.」で始まるものと、Windows の短い名前（「~」を含む）は返さない
  if (path.split(/[\\/]/).some(part => part.startsWith('.') || part.includes('~'))) { res.writeHead(403); res.end(); return; }
  const file = path === '/gui.js' ? GUI_JS : normalize(join(ROOT, path === '/' ? 'index.html' : path));
  if (file !== GUI_JS && !file.startsWith(ROOT.endsWith(sep) ? ROOT : ROOT + sep)) { res.writeHead(403); res.end(); return; }
  if (!existsSync(file) || !statSync(file).isFile()) { res.writeHead(404); res.end(); return; }
  res.writeHead(200, { 'Content-Type': TYPES[extname(file)] || 'application/octet-stream' });
  res.end(readFileSync(file));
});
await new Promise(r => server.listen(0, '127.0.0.1', r));
HOST = `127.0.0.1:${server.address().port}`;
const ORIGIN = `http://${HOST}`;

let fails = 0;
const ok = (cond, label, extra) => {
  console.log(`${cond ? 'OK  ' : 'FAIL'} ${label}${extra !== undefined ? ' ' + JSON.stringify(extra) : ''}`);
  if (!cond) fails++;
};

const b = await chromium.launch({ executablePath: CHROME, headless: true,
  args: ['--use-angle=swiftshader', '--enable-unsafe-swiftshader', '--ignore-gpu-blocklist'] });
try {
  const ctx = await b.newContext({ viewport: { width: 1440, height: 900 }, locale: 'ja-JP' });
  await ctx.route('**/*', route => {
    const u = new URL(route.request().url());
    if (u.origin !== ORIGIN || u.pathname === '/firebase-integration.js') {
      return route.fulfill({ status: 200, contentType: 'text/javascript', body: '' });
    }
    return route.continue();
  });
  // 偽物の MOD（ページの WebSocket を差し替える）。送られた命令を window.__sent に記録し、問い合わせには __results で返す
  await ctx.addInitScript(() => {
    window.__sent = [];
    // MOD が返す機能の一覧（stopAll があれば今の守りを持つ MOD。空にすると古い MOD のふり）
    window.__features = ['stopAll', 'pauseQueue'];
    window.__results = {
      getTargetBlock: { x: 1, y: -61, z: 2, blockType: 'minecraft:grass_block' },
      getBlockType: { blockType: 'minecraftedu:vertical_oak_slab' },
      getBlockUnderPlayer: { blockType: 'minecraftedu:vertical_oak_slab', position: { x: -11, y: -61, z: 3 } },
      getRecordingStatus: { isRecording: false }
    };
    class FakeWS {
      constructor(url) {
        this.url = url; this.readyState = 0;
        const open = () => { this.readyState = 1; if (this.onopen) this.onopen(); };
        // __openSync が true なら、すぐ（同じ処理の続きで）開く（つなぎ直しの間に「つながっていない」瞬間を作らないため）
        if (window.__openSync) queueMicrotask(open); else setTimeout(open, 10);
      }
      send(d) {
        const m = JSON.parse(d);
        if (m.type === 'command') window.__sent.push({ action: m.payload.action, params: m.payload.params, t: performance.now(), port: this.url.split(':')[2].split('/')[0] });
        let r = null;
        if (m.type === 'connect') {
          r = { type: 'connect_response', requestId: m.messageId, payload: { success: true, sessionId: 's', serverInfo: { features: window.__features.slice() } } };
        } else if (m.type === 'command') {
          const a = m.payload.action;
          r = { type: 'command_response', requestId: m.messageId, payload: { success: true, action: a, result: window.__results[a] || { message: 'ok' } } };
        }
        const reply = () => { if (this.readyState === 1 && this.onmessage) this.onmessage({ data: JSON.stringify(r) }); };
        // __connectHold が true なら、つなぎ始めの返事（新しい MOD か）を __releaseConnect() まで待たせる
        if (r && m.type === 'connect' && window.__connectHold) { window.__releaseConnect = reply; return; }
        if (r) setTimeout(reply, 0);
      }
      close() {
        this.readyState = 3;
        setTimeout(() => { if (this.onclose) this.onclose({ code: 1000 }); }, 0);
      }
    }
    FakeWS.CONNECTING = 0; FakeWS.OPEN = 1; FakeWS.CLOSING = 2; FakeWS.CLOSED = 3;
    window.WebSocket = FakeWS;
  });
  const errors = [];
  const logs = [];
  // 本物の WebSocket が開いたら記録する（この試験では、ページの WebSocket はすべて偽物なので、開くことは無いはず。別の窓も見張る）
  ctx.on('page', pg => pg.on('websocket', ws => errors.push(`本物の WebSocket が開いた: ${ws.url()}`)));
  const p = await ctx.newPage();
  p.on('pageerror', e => errors.push(e.message));
  p.on('console', m => { if (m.type() === 'log') logs.push(m.text()); });
  p.on('dialog', d => d.dismiss().catch(() => {})); // 動画のアップロードの知らせ（alert）は閉じる
  await p.goto(`${ORIGIN}/`, { waitUntil: 'networkidle', timeout: 90000 });
  await p.waitForTimeout(2000);
  // redux の store を取り出す（言語を変えるため）
  await p.evaluate(() => {
    const el = document.querySelector('[class*="blocks_blocks_"]');
    const key = Object.keys(el).find(k => k.startsWith('__reactFiber') || k.startsWith('__reactInternalInstance'));
    let f = el[key];
    for (let i = 0; f && i < 80; i++, f = f.return) {
      if (f.memoizedProps && f.memoizedProps.store && f.memoizedProps.store.dispatch) { window.__store = f.memoizedProps.store; break; }
    }
  });
  await p.evaluate(() => vm.extensionManager.loadExtensionURL('minecraft'));
  await p.waitForSelector('.lt-mc-lamp', { timeout: 10000 });
  // ページの WebSocket が偽物に置きかわっていなければ、ここで止める（本物の MOD に消す命令などを送らないため）
  if (!(await p.evaluate(() => window.WebSocket.name === 'FakeWS'))) throw new Error('ページの WebSocket が偽物に置きかわっていません');

  // ブロックの処理を直接よぶ（子どもがブロックを動かしたのと同じ処理。返事を待つものは値を返す）
  const call = (opcode, args) => p.evaluate(async ({ opcode, args }) => {
    try {
      const v = await vm.runtime._primitives['minecraft_' + opcode](args, {});
      return { ok: true, value: v === undefined ? null : v };
    } catch (e) {
      return { ok: false, error: String(e && e.message || e) };
    }
  }, { opcode, args });
  const clearSent = () => p.evaluate(() => { window.__sent = []; });
  const sent = () => p.evaluate(() => window.__sent.slice());
  // 最後に送られた命令（action が同じもの）
  const last = async action => (await sent()).filter(m => m.action === action).pop() || null;
  const lampState = () => p.evaluate(() => {
    const el = document.querySelector('.lt-mc-lamp');
    if (!el) return null;
    const hint = el.querySelector('.lt-mc-lamp__hint');
    return { status: el.getAttribute('data-lt-mc-status'), blink: el.classList.contains('lt-mc-lamp--blink'),
      hint: hint ? hint.textContent : null };
  });

  // つなぎ、MOD の返事（新しい MOD か）が届くまで待つ（返事を受けてから送る getRecordingStatus で分かる）
  const connectMod = async () => {
    await clearSent();
    await call('connect', { HOST: '127.0.0.1', PORT: 59999 });
    return p.waitForFunction(() => vm.runtime.ltMinecraftConnection === 'connected' && window.__sent.some(x => x.action === 'getRecordingStatus'),
      null, { timeout: 5000 }).then(() => true, () => false);
  };

  // 今の MOD に学習準備が送る命令（順番どおり。生き物の命令は無い）
  const PREP_SENT = [
    ['setTime', { time: 6000 }], ['setWeather', { weather: 'clear' }],
    ['setGameRule', { rule: 'doDaylightCycle', value: 'true' }], ['setGameRule', { rule: 'doWeatherCycle', value: 'true' }],
    ['setGameRule', { rule: 'doMobSpawning', value: 'false' }], ['setNightVision', { enabled: true }], ['setMoveSpeed', { multiplier: 1 }]
  ];

  // 0. つなぐ
  ok(await connectMod(),
    '（準備）偽物の MOD につながる');

  // 1. 置くブロックのブロックの欄に数（数を返すブロック・まだ何も入れていない変数）が入っても、止まらない
  for (const op of ['setBlock', 'setBlockRelative', 'setBlockRange', 'setBlockSimple', 'setBlockRangeSimple']) {
    await clearSent();
    const r = await call(op, { X: 0, Y: 0, Z: 0, X1: 0, Y1: 0, Z1: 0, X2: 1, Y2: 1, Z2: 1, BLOCK: 0, PLACEMENT: 'bottom', FACING: 'none', OPEN: 'closed' });
    const m = (await sent()).find(x => x.action === 'setBlock' || x.action === 'fillBlocks');
    ok(r.ok && m && m.params.blockType === 'minecraft:0', `${op}: ブロックの欄が数でも例外にならず、MOD に送る（MOD が名前のまちがいを知らせる）`, { r, m });
  }
  // 本物のスクリプトでも、後ろのブロックまで動く（数を返すブロックをブロックの欄に入れる）
  const scriptDone = await p.evaluate(async () => {
    const stage = vm.runtime.getTargetForStage();
    const v = stage.lookupOrCreateVariable('lt_done', 'lt_done');
    v.value = 'まだ';
    const blocks = [
      { id: 'lt_hat', opcode: 'event_whenbroadcastreceived', next: 'lt_set', parent: null, inputs: {},
        fields: { BROADCAST_OPTION: { name: 'BROADCAST_OPTION', value: 'lt_go', id: 'lt_go' } }, shadow: false, topLevel: true, x: 0, y: 0 },
      { id: 'lt_set', opcode: 'minecraft_setBlockSimple', next: 'lt_mark', parent: 'lt_hat',
        inputs: { X: { name: 'X', block: 'lt_x', shadow: 'lt_x' }, Y: { name: 'Y', block: 'lt_y', shadow: 'lt_y' }, Z: { name: 'Z', block: 'lt_z', shadow: 'lt_z' },
          BLOCK: { name: 'BLOCK', block: 'lt_add', shadow: 'lt_menu' } }, fields: {}, shadow: false, topLevel: false },
      { id: 'lt_x', opcode: 'math_number', next: null, parent: 'lt_set', inputs: {}, fields: { NUM: { name: 'NUM', value: '0' } }, shadow: true, topLevel: false },
      { id: 'lt_y', opcode: 'math_number', next: null, parent: 'lt_set', inputs: {}, fields: { NUM: { name: 'NUM', value: '0' } }, shadow: true, topLevel: false },
      { id: 'lt_z', opcode: 'math_number', next: null, parent: 'lt_set', inputs: {}, fields: { NUM: { name: 'NUM', value: '0' } }, shadow: true, topLevel: false },
      { id: 'lt_menu', opcode: 'minecraft_menu_blockTypes', next: null, parent: 'lt_set', inputs: {}, fields: { blockTypes: { name: 'blockTypes', value: 'stone' } }, shadow: true, topLevel: false },
      { id: 'lt_add', opcode: 'operator_add', next: null, parent: 'lt_set',
        inputs: { NUM1: { name: 'NUM1', block: 'lt_n1', shadow: 'lt_n1' }, NUM2: { name: 'NUM2', block: 'lt_n2', shadow: 'lt_n2' } }, fields: {}, shadow: false, topLevel: false },
      { id: 'lt_n1', opcode: 'math_number', next: null, parent: 'lt_add', inputs: {}, fields: { NUM: { name: 'NUM', value: '1' } }, shadow: true, topLevel: false },
      { id: 'lt_n2', opcode: 'math_number', next: null, parent: 'lt_add', inputs: {}, fields: { NUM: { name: 'NUM', value: '2' } }, shadow: true, topLevel: false },
      { id: 'lt_mark', opcode: 'data_setvariableto', next: null, parent: 'lt_set',
        inputs: { VALUE: { name: 'VALUE', block: 'lt_val', shadow: 'lt_val' } }, fields: { VARIABLE: { name: 'VARIABLE', value: 'lt_done', id: 'lt_done' } }, shadow: false, topLevel: false },
      { id: 'lt_val', opcode: 'text', next: null, parent: 'lt_mark', inputs: {}, fields: { TEXT: { name: 'TEXT', value: 'おわり' } }, shadow: true, topLevel: false }
    ];
    for (const blk of blocks) stage.blocks.createBlock(blk);
    vm.runtime.startHats('event_whenbroadcastreceived', { BROADCAST_OPTION: 'lt_go' });
    for (let i = 0; i < 60 && v.value !== 'おわり'; i++) await new Promise(r => setTimeout(r, 50));
    return v.value;
  });
  ok(scriptDone === 'おわり', '本物のスクリプト: ブロックの欄にたし算のブロックを入れても止まらず、後ろのブロックまで動く', scriptDone);

  // 2. 小数の座標は切り下げる（1 個置く・自分から置く・入れ物・ブロックの種類も、まとめて置くブロックと同じ）
  await clearSent();
  await call('setBlockSimple', { X: -10.5, Y: 1.5, Z: 2.7, BLOCK: 'stone' });
  let m = await last('setBlock');
  ok(m && m.params.x === -11 && m.params.y === -59 && m.params.z === 2, '1 個置く: 小数は切り下げる（-10.5 → -11、Y 1.5 → 1、Z 2.7 → 2）', m && m.params);
  await call('setBlock', { X: -0.5, Y: 0, Z: -3.2, BLOCK: 'oak_stairs', PLACEMENT: 'bottom', FACING: 'north', OPEN: 'closed' });
  m = await last('setBlock');
  ok(m && m.params.x === -1 && m.params.y === -60 && m.params.z === -4 && m.params.blockType === 'minecraft:oak_stairs[facing=north]',
    '向きのある置く: 小数は切り下げ、向きはそのまま', m && m.params);
  await call('setBlockRelative', { X: -0.5, Y: 1.9, Z: 0.2, BLOCK: 'stone', PLACEMENT: 'bottom', FACING: 'none', OPEN: 'closed' });
  m = await last('setBlock');
  ok(m && m.params.relativeX === -1 && m.params.relativeY === 1 && m.params.relativeZ === 0, '自分から置く: 小数は切り下げる', m && m.params);
  await call('setContainerItem', { X: -1.5, Y: 2.5, Z: 0.5, SLOT: 0, ITEM: 'arrow', COUNT: 1 });
  m = await last('setContainerItem');
  ok(m && m.params.x === -2 && m.params.y === -58 && m.params.z === 0, '入れ物に入れる: 小数は切り下げる', m && m.params);
  await call('getBlockType', { X: -1.5, Y: 0.5, Z: -0.1 });
  m = await last('getBlockType');
  ok(m && m.params.x === -2 && m.params.y === -60 && m.params.z === -1, 'ブロックの種類: 小数は切り下げる', m && m.params);
  await call('setBlockSimple', { X: 'abc', Y: 0, Z: 0, BLOCK: 'stone' });
  m = await last('setBlock');
  ok(m && m.params.x === null, '数でない座標は、今までどおり MOD に断らせる（JSON で null になる）', m && m.params);

  // 3. まとめて置く: 大きすぎる範囲も MOD に送る（MOD が断ってチャットで理由を出す）
  for (const op of ['setBlockRange', 'setBlockRangeSimple']) {
    await clearSent();
    const r = await call(op, { X1: 0, Y1: 0, Z1: 0, X2: 1000, Y2: 1000, Z2: 1000, BLOCK: 'stone', PLACEMENT: 'bottom', FACING: 'none', OPEN: 'closed' });
    m = await last('fillBlocks');
    ok(r.ok && m && m.params.to.x === 1000, `${op}: 200 万マスより大きくても、何も言わずに止めず MOD に送る`, { r, m: m && m.params });
  }

  // 4. 周囲をクリア・全エンティティをクリア: 数でない・空の中心は送らない（原点のまわりを消さない）
  for (const op of ['clearArea', 'clearAllEntities']) {
    for (const [x, z, want] of [['', '', {}], ['abc', 5, { centerZ: 5 }], [' ', 0, { centerZ: 0 }], [undefined, 3, { centerZ: 3 }],
      ['12', '-3.5', { centerX: 12, centerZ: -4 }], [2.7, 0, { centerX: 2, centerZ: 0 }], [0, 0, { centerX: 0, centerZ: 0 }]]) {
      await clearSent();
      await call(op, { X: x, Z: z });
      m = await last(op);
      ok(m && JSON.stringify(m.params) === JSON.stringify(want), `${op}: X=${JSON.stringify(x)} Z=${JSON.stringify(z)} → ${JSON.stringify(want)}`, m && m.params);
    }
  }

  // 5. 時刻: メニューの名前と、0〜23999 の数
  for (const [t, want] of [['day', 1000], ['noon', 6000], ['midnight', 18000], ['5000', 5000], [5000, 5000], ['0', 0], [12000.7, 12000],
    ['constructor', 1000], ['-1', 1000], ['24000', 1000], ['abc', 1000], ['', 1000]]) {
    await clearSent();
    await call('setTime', { TIME: t });
    m = await last('setTime');
    ok(m && m.params.time === want, `時刻 ${JSON.stringify(t)} → ${want}`, m && m.params);
  }

  // 6. 学習準備: 今の MOD には、生き物を呼ぶのを止める命令も、許可にもどす命令も送らない
  await clearSent();
  await call('prepareForLearning', {});
  const prep = (await sent()).filter(x => x.action === 'setEntitySpawning');
  ok(prep.length === 0, '学習準備は、生き物を呼ぶのを止めない（先生が止めたのを、もどしもしない）', prep);
  ok((await sent()).some(x => x.action === 'setGameRule' && x.params.rule === 'doMobSpawning' && x.params.value === 'false'),
    '学習準備は、今までどおり生き物が自然に出てくるのを止める');
  // 返事がもう来ていれば、その場で（ブロックが動いたその時に）全部送る。ほかのスクリプトより後回しにしない
  const syncSent = await p.evaluate(() => {
    window.__sent = [];
    vm.runtime._primitives.minecraft_prepareForLearning({}, {});
    return window.__sent.map(x => [x.action, x.params]);
  });
  ok(JSON.stringify(syncSent) === JSON.stringify(PREP_SENT), '返事がもう来ていれば、学習準備はその場で 7 つの命令を送る', syncSent);

  // 7. 調べるブロックが返すブロックの名前は、メニューと同じ形（minecraft: / minecraftedu: を外す）
  let r = await call('getTargetBlock', { INFO: 'blockType' });
  ok(r.value === 'grass_block', '見ているブロックの種類: minecraft: を外す', r);
  r = await call('getTargetBlock', { INFO: 'y' });
  ok(r.value === -1, '見ているブロックの y は、今までどおり +60', r);
  r = await call('getBlockType', { X: 0, Y: 0, Z: 0 });
  ok(r.value === 'vertical_oak_slab', 'ブロックの種類: 縦スラブの minecraftedu: を外す', r);
  r = await call('getBlockUnderPlayer', {});
  ok(r.value === 'vertical_oak_slab', '足元のブロック: 縦スラブの minecraftedu: を外す', r);
  await p.evaluate(() => { window.__results.getBlockType = { blockType: 'stone' }; });
  r = await call('getBlockType', { X: 0, Y: 0, Z: 0 });
  ok(r.value === 'stone', 'ブロックの種類: 何も付いていない名前はそのまま', r);

  // 8. 動画の題名: 空・空白・undefined なら「無題の録画」
  for (const [title, want] of [['undefined', '無題の録画'], ['', '無題の録画'], ['  ', '無題の録画'], [undefined, '無題の録画'], [' たんけん ', 'たんけん']]) {
    logs.length = 0;
    await call('uploadVideo', { TITLE: title });
    await p.waitForTimeout(100);
    ok(logs.includes('動画アップロード開始: ' + want), `動画の題名 ${JSON.stringify(title)} → ${want}`, logs.filter(l => l.startsWith('動画')));
  }

  // 9. つないでいないのにブロックが動いたら、ランプが点滅し、ヒントに「先につないでね」
  await call('disconnect', {});
  ok(await p.waitForFunction(() => vm.runtime.ltMinecraftConnection === 'idle', null, { timeout: 5000 }).then(() => true, () => false), '（準備）切断する');
  let l = await lampState();
  ok(l && !l.blink, '（準備）切断しただけでは点滅しない', l);
  await call('setBlockSimple', { X: 0, Y: 0, Z: 0, BLOCK: 'stone' });
  await p.waitForTimeout(200);
  l = await lampState();
  ok(l && l.status === 'idle' && l.blink, 'つないでいないのに置くブロックを動かすと、ランプが点滅する', l);
  await p.click('.lt-mc-lamp__button');
  l = await lampState();
  ok(l && l.hint && l.hint.includes('先に Minecraft につないでね') && l.hint.includes('ブロックを動かすと、つながります'),
    'ランプを押すと「先に Minecraft につないでね」と、つなぎ方が出る', l);
  ok(await p.evaluate(() => document.querySelector('.lt-mc-lamp__live').textContent.includes('先に Minecraft につないでね')),
    '「先に Minecraft につないでね」は、読み上げの文（role=status）にも入る');
  // 言語を変えても（ランプが作り直されても）、「先につないでね」が残る
  await p.evaluate(() => window.__store.dispatch({ type: 'scratch-gui/locales/SELECT_LOCALE', locale: 'ja-Hira' }));
  await p.waitForFunction(() => { const el = document.querySelector('.lt-mc-lamp__live'); return el && el.textContent.includes('さきに Minecraft につないでね'); }, null, { timeout: 8000 })
    .then(() => ok(true, '言語を変えても、「先につないでね」が残る（ひらがなの文になる）'), () => ok(false, '言語を変えても、「先につないでね」が残る（ひらがなの文になる）'));
  await p.evaluate(() => window.__store.dispatch({ type: 'scratch-gui/locales/SELECT_LOCALE', locale: 'ja' }));
  await p.waitForFunction(() => { const el = document.querySelector('.lt-mc-lamp__live'); return el && el.textContent.includes('先に Minecraft につないでね'); }, null, { timeout: 8000 }).catch(() => {});
  await p.click('.lt-mc-lamp__button'); // ヒントを開く（作り直しで閉じている）
  l = await lampState();
  ok(l && l.hint && l.hint.includes('先に Minecraft につないでね'), '（言語をもどしたあとも、ヒントに「先につないでね」）', l);
  await p.click('.lt-mc-lamp__button');
  // つないでいないまま［切断］すると、点滅も「先につないでね」も消える
  await call('setBlockSimple', { X: 0, Y: 0, Z: 0, BLOCK: 'stone' });
  await p.waitForTimeout(100);
  ok(((await lampState()) || {}).blink === true, '（準備）もう一度動かして、点滅している');
  await call('disconnect', {});
  await p.waitForTimeout(200);
  l = await lampState();
  const live = await p.evaluate(() => document.querySelector('.lt-mc-lamp__live').textContent);
  ok(l && !l.blink && !live.includes('先に') && await p.evaluate(() => vm.runtime.ltMinecraftNeedConnection === false),
    'つないでいないまま［切断］すると、点滅も「先につないでね」も消える', { l, live });
  // 値のブロック（返事を待つブロック）だけを動かしても、知らせる
  await p.evaluate(() => {
    window.__needCount = 0;
    if (!window.__needCounting) { window.__needCounting = true; vm.runtime.on('LT_MINECRAFT_NEED_CONNECTION', () => { window.__needCount++; }); }
  });
  r = await call('getPlayerFacing', {});
  ok(r.value === 'north', '（値のブロックは、今までどおり north を返す）', r);
  await p.waitForTimeout(200);
  l = await lampState();
  ok(await p.evaluate(() => window.__needCount) === 1 && l && l.blink, '値のブロックだけを動かしても、ランプに知らせて点滅する', l);
  // 点滅の途中で何度動かしても、終わりの時刻は延びない。点滅は自然に終わり、「先につないでね」は残る。また動かすと点滅し直す
  const until1 = await p.evaluate(() => vm.runtime.ltMinecraftBlinkUntil);
  await call('getPlayerFacing', {});
  ok(await p.evaluate(u => vm.runtime.ltMinecraftBlinkUntil === u, until1), '点滅の途中で何度動かしても、点滅の終わりの時刻は延びない');
  ok(await p.waitForFunction(() => !document.querySelector('.lt-mc-lamp--blink'), null, { timeout: 9000, polling: 100 }).then(() => true, () => false),
    '点滅は 4 秒ほどで自然に終わる');
  ok(await p.evaluate(() => document.querySelector('.lt-mc-lamp__live').textContent.includes('先に Minecraft につないでね')),
    '点滅が終わっても、「先につないでね」は残る');
  await call('getPlayerFacing', {});
  await p.waitForTimeout(200);
  ok(((await lampState()) || {}).blink === true, '点滅が終わったあとにまた動かすと、また点滅する');
  // ステージの表示（モニター）からの問い合わせでは知らせない（前の作品に表示が残っているとき）
  await call('disconnect', {});
  await p.waitForTimeout(200);
  await p.evaluate(() => {
    window.__needCount = 0;
    vm.runtime.monitorBlocks.createBlock({ id: 'lt_mon', opcode: 'minecraft_getPlayerFacing', inputs: {}, fields: {}, next: null, parent: null, shadow: false, topLevel: true });
    vm.runtime.monitorBlocks.changeBlock({ id: 'lt_mon', element: 'checkbox', value: true });
  });
  const monitored = await p.waitForFunction(() => vm.runtime._monitorState.has('lt_mon') && vm.runtime._monitorState.get('lt_mon').get('value') === 'north', null, { timeout: 5000 })
    .then(() => true, () => false);
  await p.waitForTimeout(1000);
  l = await lampState();
  ok(monitored && await p.evaluate(() => window.__needCount) === 0 && l && !l.blink,
    'ステージの表示（向き）が毎フレーム問い合わせても、ランプは点滅しない', { monitored, l });
  await p.evaluate(() => vm.runtime.monitorBlocks.changeBlock({ id: 'lt_mon', element: 'checkbox', value: false }));
  // ふつうのスクリプト（「ずっと」の中の値のブロック）からは、知らせる（モニターだけを外していること）
  const foreverCount = await p.evaluate(async () => {
    window.__needCount = 0;
    const stage = vm.runtime.getTargetForStage();
    stage.lookupOrCreateVariable('lt_dir', 'lt_dir');
    const blocks = [
      { id: 'lt_fh', opcode: 'event_whenbroadcastreceived', next: 'lt_ff', parent: null, inputs: {},
        fields: { BROADCAST_OPTION: { name: 'BROADCAST_OPTION', value: 'lt_go2', id: 'lt_go2' } }, shadow: false, topLevel: true, x: 0, y: 300 },
      { id: 'lt_ff', opcode: 'control_forever', next: null, parent: 'lt_fh', inputs: { SUBSTACK: { name: 'SUBSTACK', block: 'lt_fs', shadow: null } }, fields: {}, shadow: false, topLevel: false },
      { id: 'lt_fs', opcode: 'data_setvariableto', next: null, parent: 'lt_ff',
        inputs: { VALUE: { name: 'VALUE', block: 'lt_fr', shadow: 'lt_ft' } }, fields: { VARIABLE: { name: 'VARIABLE', value: 'lt_dir', id: 'lt_dir' } }, shadow: false, topLevel: false },
      { id: 'lt_ft', opcode: 'text', next: null, parent: 'lt_fs', inputs: {}, fields: { TEXT: { name: 'TEXT', value: '' } }, shadow: true, topLevel: false },
      { id: 'lt_fr', opcode: 'minecraft_getPlayerFacing', next: null, parent: 'lt_fs', inputs: {}, fields: {}, shadow: false, topLevel: false }
    ];
    for (const blk of blocks) stage.blocks.createBlock(blk);
    const threads = vm.runtime.startHats('event_whenbroadcastreceived', { BROADCAST_OPTION: 'lt_go2' });
    await new Promise(r => setTimeout(r, 600));
    for (const t of threads) vm.runtime._stopThread(t);
    return window.__needCount;
  });
  ok(foreverCount > 0, '「ずっと」の中の値のブロック（ふつうのスクリプト）からは、ランプに知らせる', foreverCount);
  // 「先につないでね」が出ている状態から、つながったら消える
  await call('setBlockSimple', { X: 0, Y: 0, Z: 0, BLOCK: 'stone' });
  await p.waitForTimeout(200);
  ok(await p.evaluate(() => vm.runtime.ltMinecraftNeedConnection === true) && ((await lampState()) || {}).blink === true,
    '（準備）つなぐ前に「先につないでね」が出て、点滅している');
  ok(await connectMod(), '（準備）もう一度つながり、返事が届く');
  await p.click('.lt-mc-lamp__button');
  l = await lampState();
  ok(l && l.status === 'connected' && !l.blink && l.hint && !l.hint.includes('先に') &&
    await p.evaluate(() => vm.runtime.ltMinecraftNeedConnection === false), 'つながったら、点滅も「先につないでね」も消える', l);
  await p.click('.lt-mc-lamp__button');
  // つながっている間に動かしても点滅しない
  await call('setBlockSimple', { X: 0, Y: 0, Z: 0, BLOCK: 'stone' });
  await p.waitForTimeout(200);
  l = await lampState();
  ok(l && !l.blink, 'つながっている間は、ブロックを動かしても点滅しない', l);

  // 10. ブロックの定義と、メニューの表示（漢字・ひらがな・英語）
  const info = () => p.evaluate(() => {
    const cat = vm.runtime._blockInfo.find(c => c.id === 'minecraft');
    const blk = opcode => cat.blocks.find(x => x.json && x.json.type === 'minecraft_' + opcode);
    const opts = (opcode, name) => {
      const json = (blk(opcode) || {}).json || {};
      for (const k of Object.keys(json)) {
        if (!k.startsWith('args')) continue;
        const a = json[k].find(x => x.name === name);
        if (a && a.options) return a.options;
      }
      return null;
    };
    const titleXml = blk('uploadVideo').xml;
    const timeMenu = cat.menus.find(x => x.json.type === 'minecraft_menu_timeValues');
    return {
      weather: opts('setWeather', 'WEATHER'),
      rules: opts('setGameRule', 'RULE'),
      values: opts('setGameRule', 'VALUE'),
      facingCheckbox: !!blk('getPlayerFacing').json.checkboxInFlyout,
      underCheckbox: !!blk('getBlockUnderPlayer').json.checkboxInFlyout,
      title: (titleXml.match(/<field name="TEXT">([^<]*)<\/field>/) || [])[1] || null,
      time: timeMenu ? timeMenu.json.args0[0].options : null
    };
  });
  // 言語を変え、Minecraft のブロックの定義がその言語で作り直されるまで待つ（動画の題名の初めの値で見分ける）
  const setLocale = async (loc, title) => {
    await p.evaluate(l => window.__store.dispatch({ type: 'scratch-gui/locales/SELECT_LOCALE', locale: l }), loc);
    await p.waitForFunction(t => {
      const cat = vm.runtime._blockInfo.find(c => c.id === 'minecraft');
      const blk = cat && cat.blocks.find(x => x.json && x.json.type === 'minecraft_uploadVideo');
      return !!blk && blk.xml.includes(`<field name="TEXT">${t}</field>`);
    }, title, { timeout: 10000 }).catch(() => {});
    await p.waitForTimeout(300);
  };
  const EXPECT = {
    ja: { weather: [['晴れ', 'clear'], ['雨', 'rain'], ['雷雨', 'thunder']],
      rules: [['モブスポーン', 'doMobSpawning'], ['時間固定', 'doDaylightCycle'], ['天気固定', 'doWeatherCycle']],
      values: [['オン', 'true'], ['オフ', 'false']], title: 'わたしの作品',
      time: [['朝', 'day'], ['昼', 'noon'], ['夕方', 'sunset'], ['夜', 'night'], ['真夜中', 'midnight']] },
    'ja-Hira': { weather: [['はれ', 'clear'], ['あめ', 'rain'], ['かみなり', 'thunder']],
      rules: [['もぶ すぽーん', 'doMobSpawning'], ['じかんをとめる', 'doDaylightCycle'], ['てんきをとめる', 'doWeatherCycle']],
      values: [['おん', 'true'], ['おふ', 'false']], title: 'わたしのさくひん',
      time: [['あさ', 'day'], ['ひる', 'noon'], ['ゆうがた', 'sunset'], ['よる', 'night'], ['まよなか', 'midnight']] },
    en: { weather: [['Clear', 'clear'], ['Rain', 'rain'], ['Thunder', 'thunder']],
      rules: [['Mob Spawning', 'doMobSpawning'], ['Stop time', 'doDaylightCycle'], ['Stop weather', 'doWeatherCycle']],
      values: [['ON', 'true'], ['OFF', 'false']], title: 'my video',
      time: [['Day', 'day'], ['Noon', 'noon'], ['Sunset', 'sunset'], ['Night', 'night'], ['Midnight', 'midnight']] }
  };
  for (const loc of ['ja', 'ja-Hira', 'en']) {
    await setLocale(loc, EXPECT[loc].title);
    const got = await info();
    const want = EXPECT[loc];
    for (const k of ['weather', 'rules', 'values', 'time']) {
      ok(JSON.stringify(got[k]) === JSON.stringify(want[k]), `${loc}: メニュー ${k}`, got[k]);
    }
    ok(got.title === want.title, `${loc}: 動画の題名の初めの値`, got.title);
    ok(!got.facingCheckbox && !got.underCheckbox, `${loc}: プレーヤーの向き・足元のブロックに、ステージに表示するチェックボックスが無い`, got);
  }
  // 訳の無い言語（ドイツ語）: ゲームのルールは英語の「Stop time」になる（前の「Tageszyklus」は意味が逆だったため）
  await p.evaluate(() => window.__store.dispatch({ type: 'scratch-gui/locales/SELECT_LOCALE', locale: 'de' }));
  // 英語と見分けるため、ドイツ語の訳がある項目（Mob-Spawning）に変わるまで待つ
  ok(await p.waitForFunction(() => {
    const cat = vm.runtime._blockInfo.find(c => c.id === 'minecraft');
    const blk = cat && cat.blocks.find(x => x.json && x.json.type === 'minecraft_setGameRule');
    return !!blk && JSON.stringify(blk.json).includes('Mob-Spawning');
  }, null, { timeout: 10000 }).then(() => true, () => false), '（準備）ドイツ語に変わる');
  const de = await info();
  ok(JSON.stringify(de.rules && de.rules[1]) === JSON.stringify(['Stop time', 'doDaylightCycle']) &&
    JSON.stringify(de.weather && de.weather[0]) === JSON.stringify(['Clear', 'clear']),
    '訳の無い言語では、英語の「Stop time」「Clear」が出る', { rules: de.rules, weather: de.weather });
  await setLocale('ja', 'わたしの作品');

  // 11. 古い MOD（stopAll を返さない。2026-11-08 より前の MOD。守りの無い 11-04 より前のものも含む）には、Scratch の側で今までどおり守る
  await call('disconnect', {});
  await p.evaluate(() => { window.__features = []; });
  ok(await connectMod(), '（準備）古い MOD のふりをした偽物につながり、返事が届く');
  await p.waitForTimeout(200);
  for (const op of ['setBlockRange', 'setBlockRangeSimple']) {
    await clearSent();
    r = await call(op, { X1: 0, Y1: 0, Z1: 0, X2: 1000, Y2: 1000, Z2: 1000, BLOCK: 'stone', PLACEMENT: 'bottom', FACING: 'none', OPEN: 'closed' });
    ok(!r.ok && !(await last('fillBlocks')), `古い MOD: ${op} の 200 万マスより大きい範囲は、今までどおり送らずに断る`, r);
    await clearSent();
    r = await call(op, { X1: 0, Y1: 0, Z1: 0, X2: 10, Y2: 10, Z2: 10, BLOCK: 'stone', PLACEMENT: 'bottom', FACING: 'none', OPEN: 'closed' });
    ok(r.ok && !!(await last('fillBlocks')), `古い MOD: ${op} のふつうの範囲は送る`, r);
  }
  for (const op of ['clearArea', 'clearAllEntities']) {
    await clearSent();
    await call(op, { X: '', Z: 5 });
    ok(!(await last(op)), `古い MOD: ${op} の中心の X が数でないときは、送らない（古い MOD は原点のまわりを消すため）`);
    await call(op, { X: 1, Z: 'abc' });
    ok(!(await last(op)), `古い MOD: ${op} の中心の Z が数でないときも、送らない`);
    await clearSent();
    await call(op, { X: 1, Z: 2 });
    m = await last(op);
    ok(m && m.params.centerX === 1 && m.params.centerZ === 2, `古い MOD: ${op} の中心が数なら送る`, m && m.params);
  }
  await clearSent();
  await call('prepareForLearning', {});
  const oldPrep = (await sent()).filter(x => x.action === 'setEntitySpawning');
  ok(oldPrep.length === 1 && oldPrep[0].params.enabled === false, '古い MOD: 学習準備は、今までどおり生き物を呼ぶのを止める（作品を守る仕組みが無いため）', oldPrep);
  await call('disconnect', {});
  await p.evaluate(() => { window.__features = ['stopAll', 'pauseQueue']; });

  // 12. つないだ直後、MOD の返事（新しい MOD か）が来る前: 古い MOD と同じに守り、学習準備は返事を待ってから決める
  // 返事を待たせてつなぐ
  const holdConnect = async (features, kind) => {
    await p.evaluate(f => { window.__features = f; window.__connectHold = true; window.__releaseConnect = null; }, features);
    await call('connect', { HOST: '127.0.0.1', PORT: 59999 });
    ok(await p.waitForFunction(() => vm.runtime.ltMinecraftConnection === 'connected' && typeof window.__releaseConnect === 'function', null, { timeout: 5000 })
      .then(() => true, () => false), `（準備）${kind}: つながり、返事を待たせている`);
    await clearSent();
  };
  // 学習準備を始める（終わった時刻と、成功か失敗かを記録する）
  const startPrep = () => p.evaluate(() => {
    window.__prep = null;
    window.__prepT0 = performance.now();
    vm.runtime._primitives.minecraft_prepareForLearning({}, {}).then(
      () => { window.__prep = { ok: true, t: performance.now() }; },
      e => { window.__prep = { ok: false, t: performance.now(), error: String(e && e.message || e) }; });
  });
  const release = () => p.evaluate(() => { window.__releaseT = performance.now(); window.__releaseConnect(); });
  const prepResult = () => p.waitForFunction(() => window.__prep, null, { timeout: 3000 }).then(() => p.evaluate(() => window.__prep), () => null);
  const prepActions = async () => (await sent()).filter(x => x.action !== 'getRecordingStatus' && x.action !== 'stopAll').map(x => [x.action, x.params]);

  // 返事の前は、古い MOD と同じに守る
  await holdConnect(['stopAll', 'pauseQueue'], '新しい MOD');
  r = await call('setBlockRangeSimple', { X1: 0, Y1: 0, Z1: 0, X2: 1000, Y2: 1000, Z2: 1000, BLOCK: 'stone' });
  ok(!r.ok && !(await last('fillBlocks')), '返事の前は、200 万マスより大きい範囲を送らずに断る', r);
  await call('clearArea', { X: '', Z: 1 });
  ok(!(await last('clearArea')), '返事の前は、中心が数でない「周囲をクリア」を送らない');
  // 新しい MOD（返事が 0.3 秒・0.7 秒遅れる）: 返事を待ち、返事が来たらすぐ、生き物の命令なしで 7 つを送る
  for (const waitMs of [300, 700]) {
    if (waitMs !== 300) await holdConnect(['stopAll', 'pauseQueue'], '新しい MOD');
    await clearSent();
    await startPrep();
    await p.waitForTimeout(waitMs);
    ok((await prepActions()).length === 0 && await p.evaluate(() => window.__prep === null), `新しい MOD（返事が ${waitMs / 1000} 秒遅れる）: 返事の前は、学習準備はまだ送らずに待つ`);
    await release();
    const res = await prepResult();
    const took = res ? Math.round(res.t - await p.evaluate(() => window.__releaseT)) : null;
    ok(res && res.ok && took < 500, `新しい MOD（返事が ${waitMs / 1000} 秒遅れる）: 返事が来たらすぐ（0.5 秒以内に）成功で終わる`, { res, took });
    ok(JSON.stringify(await prepActions()) === JSON.stringify(PREP_SENT), `新しい MOD（返事が ${waitMs / 1000} 秒遅れる）: 生き物の命令なしで 7 つの命令を送る`, await prepActions());
    await p.evaluate(() => { window.__connectHold = false; });
    await call('disconnect', {});
  }
  // 古い MOD: 返事を待ち、生き物を呼ぶのを止める
  await holdConnect([], '古い MOD');
  await startPrep();
  await p.waitForTimeout(300);
  await release();
  let res = await prepResult();
  let acts = await prepActions();
  ok(res && res.ok && acts.length === 8 && JSON.stringify(acts[5]) === JSON.stringify(['setEntitySpawning', { enabled: false }]),
    '古い MOD: 返事を待ってから、生き物を呼ぶのを止める命令も入れて 8 つ送る', { res, acts });
  await p.evaluate(() => { window.__connectHold = false; });
  await call('disconnect', {});
  // 返事が来ない MOD: 1 秒待ってから、古い MOD として送る（最初の命令が送られるのは、学習準備を始めてから 1 秒以降）
  await holdConnect(['stopAll', 'pauseQueue'], '返事が来ない MOD');
  await startPrep();
  res = await prepResult();
  const firstSend = await p.evaluate(() => {
    const first = window.__sent.find(x => x.action !== 'getRecordingStatus' && x.action !== 'stopAll');
    return first ? Math.round(first.t - window.__prepT0) : null;
  });
  acts = await prepActions();
  ok(res && res.ok && firstSend !== null && firstSend >= 1000 && acts.some(x => x[0] === 'setEntitySpawning' && x[1].enabled === false),
    '返事が来ない MOD: 1 秒より前には送らず、1 秒たったら古い MOD として（生き物を止めて）送る', { firstSend, acts });
  await p.evaluate(() => { window.__connectHold = false; });
  await call('disconnect', {});
  // 「すべてを止める」ブロックを、本物のスクリプトで動かす（止めた回数が増えるまで待つ）
  const stopAllBlock = () => p.evaluate(async () => {
    const stage = vm.runtime.getTargetForStage();
    if (!stage.blocks.getBlock('lt_sh')) {
      stage.blocks.createBlock({ id: 'lt_sh', opcode: 'event_whenbroadcastreceived', next: 'lt_st', parent: null, inputs: {},
        fields: { BROADCAST_OPTION: { name: 'BROADCAST_OPTION', value: 'lt_stop', id: 'lt_stop' } }, shadow: false, topLevel: true, x: 0, y: 600 });
      stage.blocks.createBlock({ id: 'lt_st', opcode: 'control_stop', next: null, parent: 'lt_sh', inputs: {},
        fields: { STOP_OPTION: { name: 'STOP_OPTION', value: 'all' } }, mutation: { tagName: 'mutation', children: [], hasnext: 'false' },
        shadow: false, topLevel: false });
    }
    let stopped = false;
    const onStop = () => { stopped = true; };
    vm.runtime.on('PROJECT_STOP_ALL', onStop);
    vm.runtime.startHats('event_whenbroadcastreceived', { BROADCAST_OPTION: 'lt_stop' });
    for (let i = 0; i < 100 && !stopped; i++) await new Promise(r => setTimeout(r, 20));
    vm.runtime.removeListener('PROJECT_STOP_ALL', onStop);
  });
  // 待っている間に止める: 返事が来ても、学習準備の命令は送らない。古い MOD には生き物を止める命令だけを送る
  const stopCases = [
    ['止めるボタン', ['stopAll', 'pauseQueue'], () => p.evaluate(() => vm.runtime.stopAll()), []],
    ['止めるボタン（古い MOD）', [], () => p.evaluate(() => vm.runtime.stopAll()), [['setEntitySpawning', { enabled: false }]]],
    // スクリプトの中の「すべてを止める」ブロック（本物のブロックを動かす）
    ['「すべてを止める」ブロック', ['stopAll', 'pauseQueue'], () => stopAllBlock(), []],
    ['「すべてを止める」ブロック（古い MOD）', [], () => stopAllBlock(), [['setEntitySpawning', { enabled: false }]]],
    // 止めたうえで返事が来ないまま 1 秒たつ: 分からないので古い MOD として、生き物を止める命令だけを送る（1 秒たってから）
    ['止めるボタン（返事が来ない）', ['stopAll', 'pauseQueue'], () => p.evaluate(() => vm.runtime.stopAll()), [['setEntitySpawning', { enabled: false }]], true]
  ];
  for (const [kind, features, stop, want, noReply] of stopCases) {
    await holdConnect(features, kind);
    logs.length = 0;
    await startPrep();
    await p.waitForTimeout(100);
    await stop();
    if (!noReply) await release();
    res = await prepResult();
    await p.waitForTimeout(200);
    acts = await prepActions();
    const firstAt = await p.evaluate(() => {
      const first = window.__sent.find(x => x.action !== 'getRecordingStatus' && x.action !== 'stopAll');
      return first ? Math.round(first.t - window.__prepT0) : null;
    });
    ok(res && res.ok && JSON.stringify(acts) === JSON.stringify(want) && !logs.includes('学習準備完了') && (!noReply || firstAt >= 1000),
      `待っている間に${kind}で止めると、${noReply ? '1 秒たってから' : '返事が来ても'}${want.length ? '生き物を止める命令だけを送る' : '学習準備の命令は送らない'}（「完了」も出さない）`, { res, acts, firstAt });
    await p.evaluate(() => { window.__connectHold = false; });
    await call('disconnect', {});
  }
  // 本物のスクリプトで学習準備を動かす（「〜を受け取ったとき → 学習準備 → 変数を『おわり』にする」）
  const startPrepScript = () => p.evaluate(() => {
    const stage = vm.runtime.getTargetForStage();
    const v = stage.lookupOrCreateVariable('lt_pdone', 'lt_pdone');
    v.value = 'まだ';
    if (!stage.blocks.getBlock('lt_ph')) {
      stage.blocks.createBlock({ id: 'lt_ph', opcode: 'event_whenbroadcastreceived', next: 'lt_pp', parent: null, inputs: {},
        fields: { BROADCAST_OPTION: { name: 'BROADCAST_OPTION', value: 'lt_prep', id: 'lt_prep' } }, shadow: false, topLevel: true, x: 0, y: 700 });
      stage.blocks.createBlock({ id: 'lt_pp', opcode: 'minecraft_prepareForLearning', next: 'lt_pm', parent: 'lt_ph', inputs: {}, fields: {}, shadow: false, topLevel: false });
      stage.blocks.createBlock({ id: 'lt_pm', opcode: 'data_setvariableto', next: null, parent: 'lt_pp',
        inputs: { VALUE: { name: 'VALUE', block: 'lt_pv', shadow: 'lt_pv' } }, fields: { VARIABLE: { name: 'VARIABLE', value: 'lt_pdone', id: 'lt_pdone' } }, shadow: false, topLevel: false });
      stage.blocks.createBlock({ id: 'lt_pv', opcode: 'text', next: null, parent: 'lt_pm', inputs: {}, fields: { TEXT: { name: 'TEXT', value: 'おわり' } }, shadow: true, topLevel: false });
    }
    window.__prepThreads = vm.runtime.startHats('event_whenbroadcastreceived', { BROADCAST_OPTION: 'lt_prep' });
  });
  // 学習準備のスクリプトが、MOD の返事を待っている（Promise を待っている）ことを確かめる
  const prepScriptWaiting = () => p.waitForFunction(() => {
    const t = window.__prepThreads && window.__prepThreads[0];
    return !!t && t.status === 1 && vm.runtime.isActiveThread(t);
  }, null, { timeout: 3000 }).then(() => true, () => false);
  // 止めない: 返事が来たら 7 つ送り、次のブロックまで進む
  await holdConnect(['stopAll', 'pauseQueue'], '本物のスクリプト');
  await startPrepScript();
  ok(await prepScriptWaiting(), '（準備）本物のスクリプトの学習準備が、返事を待っている');
  await release();
  const done = await p.waitForFunction(() => vm.runtime.getTargetForStage().lookupVariableById('lt_pdone').value === 'おわり', null, { timeout: 3000 })
    .then(() => true, () => false);
  ok(done && JSON.stringify(await prepActions()) === JSON.stringify(PREP_SENT),
    '本物のスクリプトで止めずにいると、返事が来たら 7 つの命令を送り、次のブロックまで進む', { done, acts: await prepActions() });
  await p.evaluate(() => { window.__connectHold = false; });
  await call('disconnect', {});
  // 学習準備を動かしたスクリプトだけを止める（もう一度押したとき）: 新しい MOD には送らない。古い MOD には生き物を止める命令だけ
  for (const [features, kind, want] of [[['stopAll', 'pauseQueue'], 'スクリプトだけを止める', []],
    [[], 'スクリプトだけを止める（古い MOD）', [['setEntitySpawning', { enabled: false }]]]]) {
    await holdConnect(features, kind);
    logs.length = 0;
    await startPrepScript();
    ok(await prepScriptWaiting(), `（準備）${kind}: 学習準備が返事を待っている`);
    await p.evaluate(() => { for (const t of window.__prepThreads) vm.runtime._stopThread(t); });
    await release();
    await p.waitForTimeout(400);
    acts = await prepActions();
    ok(JSON.stringify(acts) === JSON.stringify(want) && !logs.includes('学習準備完了'),
      `待っている間に、学習準備を動かしたスクリプトだけを止めると、返事が来ても${want.length ? '生き物を止める命令だけを送る' : '送らない'}`, acts);
    await p.evaluate(() => { window.__connectHold = false; });
    await call('disconnect', {});
  }
  // 待っている間に［切断］: すぐ終わり、何も送らず、ランプも点滅しない
  await holdConnect(['stopAll', 'pauseQueue'], '［切断］');
  await startPrep();
  await p.waitForTimeout(100);
  const tCut = await p.evaluate(() => performance.now());
  await call('disconnect', {});
  res = await prepResult();
  await p.waitForTimeout(200);
  l = await lampState();
  ok(res && res.ok && res.t - tCut < 300 && (await prepActions()).length === 0 && l && !l.blink && await p.evaluate(() => vm.runtime.ltMinecraftNeedConnection === false),
    '待っている間に［切断］すると、すぐ終わり、何も送らず、ランプも点滅しない', { took: res && Math.round(res.t - tCut), l });
  // 待っている間に、［切断］せずに別の場所（番号）へつなぎ直す: 前の学習準備の命令は、新しい接続へ送らない
  // （新しい接続はすぐ開くので、待っている側は「つながっていない」瞬間を見ない。接続が変わったことで止まるかを確かめる）
  await holdConnect(['stopAll', 'pauseQueue'], 'つなぎ直し');
  await startPrep();
  await p.waitForTimeout(100);
  await p.evaluate(() => {
    window.__connectHold = false;
    window.__openSync = true;
    vm.runtime._primitives.minecraft_connect({ HOST: '127.0.0.1', PORT: 59998 }, {});
  });
  res = await prepResult();
  await p.waitForTimeout(300);
  await p.evaluate(() => { window.__openSync = false; });
  const reSent = await p.evaluate(() => window.__sent.filter(x => x.action !== 'getRecordingStatus' && x.action !== 'stopAll').map(x => [x.port, x.action]));
  ok(res && res.ok && reSent.length === 0 && await p.evaluate(() => vm.runtime.ltMinecraftConnection === 'connected'),
    '待っている間に別の場所へつなぎ直しても、前の学習準備の命令は新しい接続へ送らない', { res, reSent });
  await call('disconnect', {});
  await p.evaluate(() => { window.__features = ['stopAll', 'pauseQueue']; window.__connectHold = false; });

  ok(errors.length === 0, 'ページのエラーなし', errors);
} finally {
  await b.close();
  server.close();
}
console.log(fails === 0 ? '\nすべて OK' : `\n${fails} 件しっぱい`);
process.exit(fails === 0 ? 0 : 1);

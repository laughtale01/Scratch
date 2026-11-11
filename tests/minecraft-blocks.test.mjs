// Minecraft のカテゴリーのブロック（gui.js の Minecraft 拡張）が、MOD へ正しい命令を送るかの試験。
// 本物の Chrome を見えない形で動かし、リポジトリの index.html と gui.js を 127.0.0.1 の小さなサーバーから開く。
// ページの中の WebSocket を偽物の MOD に差し替える（外へは何もつながない）。送られた命令を記録し、問い合わせには決まった返事をする。
// 本番にはつながない: 127.0.0.1 以外への通信と firebase-integration.js は空の中身に差し替える。
// 使い方: node tests/minecraft-blocks.test.mjs
//   CHROME_PATH   … Chrome の実行ファイルの場所（既定は Mac の /Applications/Google Chrome.app/Contents/MacOS/Google Chrome）。
//                   無ければ試験を飛ばし、終わりのコード 77 を返す
//   BLOCKS_GUI_JS … 試す gui.js（既定はリポジトリの gui.js）。別の gui.js を指しているときは、下の 2 つの記録は作り直さない
//                   （リポジトリの記録を、試しの gui.js の中身で上書きしないため）
//   BLOCKS_UPDATE_TEXTS=1 … Minecraft の文（漢字・ひらがな）の記録 tests/fixtures/minecraft-texts.json を作り直す
//                   （ことばを決めて変えたときだけ。前の記録から変わる所を出す。ページのエラーを含め、ほかの試験が
//                   1 つでも失敗していれば作り直さない）
//   BLOCKS_UPDATE_FIXTURE=1 … 今までの作品を守る記録 tests/fixtures/minecraft-blocks-compat.json を作り直す
//                   （わざとブロックやメニューの値を変えたときだけ。前の記録から消えるものを出し、ほかの試験が失敗していれば作り直さない。
//                   ことばも変えたときは BLOCKS_UPDATE_TEXTS=1 も一緒につける）
import { chromium } from 'playwright-core';
import { createServer } from 'node:http';
import { existsSync, lstatSync, readFileSync, renameSync, statSync, writeFileSync } from 'node:fs';
import { dirname, extname, join, normalize, sep } from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = fileURLToPath(new URL('..', import.meta.url));
const GUI_JS = process.env.BLOCKS_GUI_JS || join(ROOT, 'gui.js');
// リポジトリの gui.js を試しているか（記録を作り直してよいのは、このときだけ）。
// 文字の上の比べ（path.resolve や fs.realpathSync も、先に「..」を文字の上で消す）では、「リンクのフォルダ/../gui.js」などで
// 別のファイルを読めてしまう。そこで、OS が実際に開くファイルそのもの（デバイスと i ノードの番号）で比べる。
// 番号は bigint で読む（Windows の 64 ビットの番号は、ふつうの数では丸められて、別のファイルと同じになることがある）。
// 番号が 0 のファイルシステム（FAT など）では比べられないので「ちがう」とする。
// 大きさと書いた時刻も入れておき、記録を書く直前にもう一度比べる（試しているあいだに gui.js が変わったら書かない）
const repoGuiStamp = () => {
  try {
    const a = statSync(GUI_JS, { bigint: true });
    const r = statSync(join(ROOT, 'gui.js'), { bigint: true });
    if (!a.isFile() || a.ino === 0n || a.dev !== r.dev || a.ino !== r.ino) return null;
    return `${a.dev}:${a.ino}:${a.size}:${a.mtimeNs}`;
  } catch (e) {
    return null; // 無いファイルなどは「リポジトリの gui.js ではない」（作り直さない側）
  }
};
const GUI_STAMP = repoGuiStamp();
const IS_REPO_GUI = GUI_STAMP !== null;
// 漢字（々〆〇もふくむ。〆は Unicode では漢字のなかまに入らないので、別に足す）
const HAN = /[\p{Script=Han}々〆〇]/u;
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
    // MOD が知っている MOD のブロックの名前（接続の返事の serverInfo.modBlocks。null にすると返事に入れない＝0.2.0 までの古い MOD のふり）
    window.__modBlocks = ['vertical_oak_slab', 'gold_block_stairs', 'gold_block_slab'];
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
        // __openSync が true なら、すぐ（同じ処理の続きで）開く（つなぎ直しの間に「つながっていない」瞬間を作らないため）。
        // __openHold が true なら、__releaseOpen() まで開かない（つなぎかけ〔黄〕のままにするため）
        if (window.__openHold) window.__releaseOpen = open;
        else if (window.__openSync) queueMicrotask(open); else setTimeout(open, 10);
      }
      send(d) {
        const m = JSON.parse(d);
        if (m.type === 'command') window.__sent.push({ action: m.payload.action, params: m.payload.params, t: performance.now(), port: this.url.split(':')[2].split('/')[0] });
        let r = null;
        if (m.type === 'connect' && window.__connectFail) {
          // __connectFail が true なら、つなぎ始めにエラーの返事を返す（新しい MOD かどうか分からないまま）
          r = { type: 'connect_response', requestId: m.messageId, payload: { success: false, errorCode: 'X', errorMessage: 'fail' } };
        } else if (m.type === 'connect') {
          const serverInfo = { features: window.__features.slice() };
          if (window.__modBlocks) serverInfo.modBlocks = window.__modBlocks.slice();
          r = { type: 'connect_response', requestId: m.messageId, payload: { success: true, sessionId: 's', serverInfo } };
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
  // ページのエラーは、どこで起きたかが分かるよう、呼び出しの先頭の数行も残す
  p.on('pageerror', e => errors.push(`${e.message} @ ${String(e.stack || '').split('\n').slice(1, 4).map(x => x.trim()).join(' | ')}`));
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

  // 今の MOD にはじめの準備が送る命令（順番どおり。生き物の命令は無い）
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

  // 2b. MOD が足した階段・ハーフブロック（金）は minecraftedu: を付けて送る（置き方・向きは Minecraft の階段・ハーフと同じ）。
  //     縦スラブ・Minecraft の金ブロックは今までどおり
  const GOLD_SENT = [
    ['setBlock', { X: 0, Y: 0, Z: 0, BLOCK: 'gold_block_stairs', PLACEMENT: 'top', FACING: 'east', OPEN: 'closed' }, 'setBlock',
      'minecraftedu:gold_block_stairs[half=top,facing=east]'],
    ['setBlock', { X: 0, Y: 0, Z: 0, BLOCK: 'gold_block_slab', PLACEMENT: 'bottom', FACING: 'none', OPEN: 'closed' }, 'setBlock',
      'minecraftedu:gold_block_slab'],
    ['setBlockRelative', { X: 0, Y: 0, Z: 0, BLOCK: 'gold_block_slab', PLACEMENT: 'top', FACING: 'none', OPEN: 'closed' }, 'setBlock',
      'minecraftedu:gold_block_slab[type=top]'],
    ['setBlockRange', { X1: 0, Y1: 0, Z1: 0, X2: 1, Y2: 0, Z2: 0, BLOCK: 'gold_block_slab', PLACEMENT: 'double', FACING: 'none', OPEN: 'closed' }, 'fillBlocks',
      'minecraftedu:gold_block_slab[type=double]'],
    ['setBlockRange', { X1: 0, Y1: 0, Z1: 0, X2: 1, Y2: 0, Z2: 0, BLOCK: 'gold_block_stairs', PLACEMENT: 'bottom', FACING: 'west', OPEN: 'closed' }, 'fillBlocks',
      'minecraftedu:gold_block_stairs[facing=west]'],
    ['setBlockSimple', { X: 0, Y: 0, Z: 0, BLOCK: 'gold_block_stairs' }, 'setBlock', 'minecraftedu:gold_block_stairs'],
    ['setBlockRangeSimple', { X1: 0, Y1: 0, Z1: 0, X2: 1, Y2: 0, Z2: 0, BLOCK: 'gold_block_slab' }, 'fillBlocks', 'minecraftedu:gold_block_slab'],
    ['setBlockSimple', { X: 0, Y: 0, Z: 0, BLOCK: 'gold_block' }, 'setBlock', 'minecraft:gold_block'],
    ['setBlockSimple', { X: 0, Y: 0, Z: 0, BLOCK: 'vertical_gold_block_slab' }, 'setBlock', 'minecraftedu:vertical_gold_block_slab[facing=north]'],
    ['setBlock', { X: 0, Y: 0, Z: 0, BLOCK: 'bamboo_stairs', PLACEMENT: 'top', FACING: 'east', OPEN: 'closed' }, 'setBlock',
      'minecraft:bamboo_stairs[half=top,facing=east]']
  ];
  for (const [op, args, action, want] of GOLD_SENT) {
    await clearSent();
    const r = await call(op, args);
    m = await last(action);
    ok(r.ok && m && m.params.blockType === want, `${op} ${args.BLOCK}${args.PLACEMENT ? ` ${args.PLACEMENT}/${args.FACING}` : ''} → ${want}`, { r, m: m && m.params });
  }

  // 2c. 金の階段・ハーフを知らない MOD（接続の返事の modBlocks に無い）には送らず、帯「このブロックは置けません（MOD が古いため）」で知らせる。
  //     止まらずに次のブロックへ進み、ほかのブロック（Minecraft のブロック・縦スラブ）は今までどおり送る
  const NOTICE_TEXTS = ['This block cannot be placed (the MOD is old)', 'このブロックは置けません（MODが古いため）', 'このブロックはおけません（MODがふるいため）'];
  // 帯（一時停止の帯と同じ入れ物）の文。出ていなければ null
  const noticeText = () => p.evaluate(() => {
    const el = [...document.body.children].find(e => e.tagName === 'DIV' && e.getAttribute('role') === 'status' && e.style.zIndex === '10000');
    return el ? el.textContent : null;
  });
  const hideNotice = () => p.evaluate(() => {
    for (const e of [...document.body.children]) if (e.tagName === 'DIV' && e.getAttribute('role') === 'status' && e.style.zIndex === '10000') e.remove();
  });
  // 置く 5 つの処理で、金を 1 つずつ置く（名前と、送るはずの命令）
  const GOLD_OPS = [
    ['setBlock', { X: 0, Y: 0, Z: 0, BLOCK: 'gold_block_stairs', PLACEMENT: 'top', FACING: 'east', OPEN: 'closed' }, 'setBlock'],
    ['setBlockRelative', { X: 0, Y: 0, Z: 0, BLOCK: 'gold_block_slab', PLACEMENT: 'top', FACING: 'none', OPEN: 'closed' }, 'setBlock'],
    ['setBlockRange', { X1: 0, Y1: 0, Z1: 0, X2: 1, Y2: 0, Z2: 0, BLOCK: 'gold_block_slab', PLACEMENT: 'double', FACING: 'none', OPEN: 'closed' }, 'fillBlocks'],
    ['setBlockSimple', { X: 0, Y: 0, Z: 0, BLOCK: 'gold_block_stairs' }, 'setBlock'],
    ['setBlockRangeSimple', { X1: 0, Y1: 0, Z1: 0, X2: 1, Y2: 0, Z2: 0, BLOCK: 'gold_block_slab' }, 'fillBlocks']
  ];
  // MOD をつなぎ直す（modBlocks を変えて）。同じ場所への［Minecraftにつなぐ］はつなぎ直さないので、先に切る
  const reconnectWith = async modBlocks => {
    await call('disconnect', {});
    await p.evaluate(mb => { window.__modBlocks = mb; }, modBlocks);
    return connectMod();
  };
  ok(await reconnectWith(null), '（準備）古い MOD（返事に modBlocks が無い）につなぐ');
  for (const [op, args, action] of GOLD_OPS) {
    await hideNotice();
    await clearSent();
    const r = await call(op, args);
    const notice = await noticeText();
    ok(r.ok && !(await last(action)) && NOTICE_TEXTS.includes(notice), `古い MOD: ${op} ${args.BLOCK} は送らず、止まらずに帯で知らせる`, { r, notice, sent: await sent() });
  }
  for (const [block, want] of [['stone', 'minecraft:stone'], ['vertical_oak_slab', 'minecraftedu:vertical_oak_slab[facing=north]']]) {
    await hideNotice();
    await clearSent();
    const r = await call('setBlockSimple', { X: 0, Y: 0, Z: 0, BLOCK: block });
    m = await last('setBlock');
    ok(r.ok && m && m.params.blockType === want && (await noticeText()) === null, `古い MOD: ${block} は今までどおり送り、帯は出さない`, { r, m: m && m.params });
  }
  // 階段だけを知っている MOD: 階段は送り、ハーフは送らない（名前ごとに決める）
  ok(await reconnectWith(['gold_block_stairs']), '（準備）金の階段だけを知っている MOD につなぐ');
  await clearSent();
  await call('setBlockSimple', { X: 0, Y: 0, Z: 0, BLOCK: 'gold_block_stairs' });
  await call('setBlockSimple', { X: 1, Y: 0, Z: 0, BLOCK: 'gold_block_slab' });
  const partSent = (await sent()).filter(x => x.action === 'setBlock').map(x => x.params.blockType);
  ok(JSON.stringify(partSent) === JSON.stringify(['minecraftedu:gold_block_stairs']), '階段だけを知っている MOD: 階段は送り、ハーフは送らない', partSent);
  // 返事の前に金を置く: 返事を待ってから決める（古い MOD なら送らない・新しい MOD なら返事が来たらすぐ送る）
  for (const [modBlocks, kind, wantSent] of [[null, '古い MOD', false], [['gold_block_stairs', 'gold_block_slab'], '新しい MOD', true]]) {
    await call('disconnect', {});
    await hideNotice();
    await p.evaluate(mb => { window.__modBlocks = mb; window.__connectHold = true; window.__releaseConnect = null; }, modBlocks);
    await call('connect', { HOST: '127.0.0.1', PORT: 59999 });
    ok(await p.waitForFunction(() => vm.runtime.ltMinecraftConnection === 'connected' && typeof window.__releaseConnect === 'function', null, { timeout: 5000 })
      .then(() => true, () => false), `（準備）${kind}: つながり、返事を待たせている`);
    await clearSent();
    await p.evaluate(() => {
      window.__goldDone = null;
      vm.runtime._primitives.minecraft_setBlockSimple({ X: 0, Y: 0, Z: 0, BLOCK: 'gold_block_stairs' }, {})
        .then(() => { window.__goldDone = { ok: true, t: performance.now() }; }, e => { window.__goldDone = { ok: false, error: String(e) }; });
    });
    await p.waitForTimeout(200);
    const before = (await sent()).filter(x => x.action === 'setBlock').length;
    // 返事の前は、まだ終わっていない（待っている）・帯も出ていない
    const waiting = await p.evaluate(() => window.__goldDone === null) && (await noticeText()) === null;
    await p.evaluate(() => { window.__releaseT = performance.now(); window.__releaseConnect(); });
    const done = await p.waitForFunction(() => window.__goldDone, null, { timeout: 3000 }).then(() => p.evaluate(() => window.__goldDone), () => null);
    const after = (await sent()).filter(x => x.action === 'setBlock').map(x => x.params.blockType);
    const took = done && done.t ? Math.round(done.t - await p.evaluate(() => window.__releaseT)) : null;
    ok(before === 0 && waiting && done && done.ok && took !== null && took >= 0 && took < 500 &&
      JSON.stringify(after) === JSON.stringify(wantSent ? ['minecraftedu:gold_block_stairs'] : []) &&
      (wantSent ? (await noticeText()) === null : NOTICE_TEXTS.includes(await noticeText())),
      `${kind}（返事の前に金を置く）: 返事を待ち、返事が来たらすぐ${wantSent ? '送る' : '送らずに帯で知らせる'}`, { before, done, took, after });
    await p.evaluate(() => { window.__connectHold = false; });
  }
  // 返事を待たせてつなぐ（modBlocks を変えて）。__goldDone は、終わった置く処理の名前の一覧
  const holdGold = async (modBlocks, label) => {
    await call('disconnect', {});
    await hideNotice();
    await p.evaluate(mb => { window.__modBlocks = mb; window.__connectHold = true; window.__releaseConnect = null; window.__goldDone = []; }, modBlocks);
    await call('connect', { HOST: '127.0.0.1', PORT: 59999 });
    ok(await p.waitForFunction(() => vm.runtime.ltMinecraftConnection === 'connected' && typeof window.__releaseConnect === 'function', null, { timeout: 5000 })
      .then(() => true, () => false), `（準備）${label}: つながり、返事を待たせている`);
    await clearSent();
  };
  // 5 つの置く処理で金を置き始める（返事の前なので、すぐには終わらない）
  const startGold = () => p.evaluate(ops => {
    for (const [op, args] of ops) {
      vm.runtime._primitives['minecraft_' + op](args, {})
        .then(() => window.__goldDone.push(op), e => window.__goldDone.push(op + ': ' + e));
    }
  }, GOLD_OPS.map(([op, args]) => [op, args]));
  const goldDone = n => p.waitForFunction(k => window.__goldDone.length >= k, n, { timeout: 3000 })
    .then(() => p.evaluate(() => window.__goldDone.slice()), () => p.evaluate(() => window.__goldDone.slice()));
  const placed = async () => (await sent()).filter(x => x.action === 'setBlock' || x.action === 'fillBlocks');
  const NEW_MOD = ['vertical_oak_slab', 'gold_block_stairs', 'gold_block_slab'];
  // 返事を待っている間に止める（止めるボタン）: 返事が来ても送らない（止めたのにあとから金が置かれないように）
  await holdGold(NEW_MOD, '待っている間に止める');
  await startGold();
  await p.waitForTimeout(100);
  await p.evaluate(() => vm.runtime.stopAll());
  await p.evaluate(() => window.__releaseConnect());
  let gd = await goldDone(5);
  await p.waitForTimeout(100);
  let pl = await placed();
  ok(gd.length === 5 && gd.every(x => !x.includes(':')) && pl.length === 0,
    '新しい MOD: 返事を待っている間に止めると、返事が来ても 5 つの置く処理とも送らずに終わる', { gd, pl });
  // 返事を待っている間に、別の場所（番号）へつなぎ直す（新しい接続はすぐ開き、その返事もまだ来ない）:
  // 前の接続の金を、新しい接続へ送らない（新しい接続が古い MOD なら「なまえが ちがいます」が出てしまうため）
  await holdGold(NEW_MOD, '待っている間につなぎ直す');
  await startGold();
  await p.waitForTimeout(100);
  await p.evaluate(() => {
    window.__modBlocks = null;
    window.__openSync = true;
    window.__releaseConnect = null;
    vm.runtime._primitives.minecraft_connect({ HOST: '127.0.0.1', PORT: 59998 }, {});
  });
  gd = await goldDone(5);
  await p.waitForTimeout(100);
  pl = await placed();
  ok(gd.length === 5 && gd.every(x => !x.includes(':')) && pl.length === 0 && await p.evaluate(() => vm.runtime.ltMinecraftConnection === 'connected'),
    '返事を待っている間に別の場所へつなぎ直すと、5 つの置く処理とも新しい接続へ送らない', { gd, pl });
  await p.evaluate(() => { window.__openSync = false; window.__connectHold = false; if (window.__releaseConnect) window.__releaseConnect(); });
  // 返事を待っている間に、別の場所へつなぎ直しかけ（新しい接続がまだ開いていない＝つなぎかけ〔黄〕）:
  // 切れただけのときと同じ道で「つないでいない」で終わり、前の接続の金をどこにも送らない。つなぎかけの間なので、ランプも点滅しない
  await holdGold(NEW_MOD, '待っている間につなぎ直しかけ');
  await startGold();
  await p.evaluate(() => {
    window.__modBlocks = null;
    window.__openHold = true;
    window.__releaseOpen = null;
    window.__releaseConnect = null;
    vm.runtime._primitives.minecraft_connect({ HOST: '127.0.0.1', PORT: 59998 }, {});
  });
  gd = await goldDone(5);
  await p.waitForTimeout(100);
  pl = await placed();
  const halfState = await p.evaluate(() => ({ conn: vm.runtime.ltMinecraftConnection, need: vm.runtime.ltMinecraftNeedConnection,
    opening: typeof window.__releaseOpen === 'function' }));
  const halfLamp = await lampState();
  ok(gd.length === 5 && gd.every(x => x.includes('Not connected')) && pl.length === 0 && halfState.conn === 'connecting' && halfState.opening &&
    halfState.need !== true && halfLamp && !halfLamp.blink,
    '返事を待っている間に別の場所へつなぎ直しかけ（まだ開いていない）だと、5 つの置く処理とも「つないでいない」で終わり、どこにも送らず、ランプも点滅しない',
    { gd, pl, halfState, halfLamp });
  await p.evaluate(() => { window.__openHold = false; window.__connectHold = false; if (window.__releaseOpen) window.__releaseOpen(); });
  ok(await p.waitForFunction(() => vm.runtime.ltMinecraftConnection === 'connected', null, { timeout: 5000 }).then(() => true, () => false),
    '（あとかたづけ）つなぎかけの接続が開いて、つながる');
  await p.evaluate(() => { if (window.__releaseConnect) window.__releaseConnect(); });
  // 返事を待っている間に、同じブロックを別の場所で 2 回動かす（クローンなど。Scratch は args を使い回す）:
  // 置く場所は、それぞれの呼び出しのときの場所（待っている間に args が書きかわっても、あとの場所にならない）
  await holdGold(NEW_MOD, '待っている間に同じブロックを 2 回');
  await p.evaluate(() => {
    for (const op of ['setBlock', 'setBlockRelative', 'setBlockSimple']) {
      const args = { X: 1, Y: 0, Z: 0, BLOCK: 'gold_block_stairs', PLACEMENT: 'bottom', FACING: 'none', OPEN: 'closed' };
      vm.runtime._primitives['minecraft_' + op](args, {}).then(() => window.__goldDone.push(op));
      args.X = 9;
      vm.runtime._primitives['minecraft_' + op](args, {}).then(() => window.__goldDone.push(op));
    }
  });
  await p.evaluate(() => window.__releaseConnect());
  gd = await goldDone(6);
  const xs = (await placed()).map(x => (x.params.x !== undefined ? x.params.x : x.params.relativeX)).sort((a, b) => a - b);
  ok(gd.length === 6 && JSON.stringify(xs) === '[1,1,1,9,9,9]',
    '返事を待っている間に同じブロックを別の場所で 2 回動かしても、それぞれの場所に送る（setBlock・setBlockRelative・setBlockSimple）', { gd, xs });
  await p.evaluate(() => { window.__connectHold = false; });
  // 返事が 1 秒来ない: 待ってから、今までどおり送る（分からないまま止めない）
  await holdGold(NEW_MOD, '返事が来ない');
  const t0 = Date.now();
  let rg = await call('setBlockSimple', { X: 0, Y: 0, Z: 0, BLOCK: 'gold_block_stairs' });
  const waited = Date.now() - t0;
  m = await last('setBlock');
  ok(rg.ok && waited >= 900 && m && m.params.blockType === 'minecraftedu:gold_block_stairs',
    '返事が 1 秒来ないときは、1 秒待ってから今までどおり送る', { rg, waited, m: m && m.params });
  await p.evaluate(() => { window.__connectHold = false; if (window.__releaseConnect) window.__releaseConnect(); });
  // エラーの返事（新しい MOD か分からない）: 今までどおり送る
  await call('disconnect', {});
  await p.evaluate(() => { window.__connectFail = true; });
  await call('connect', { HOST: '127.0.0.1', PORT: 59999 });
  ok(await p.waitForFunction(() => vm.runtime.ltMinecraftConnection === 'connected', null, { timeout: 5000 }).then(() => true, () => false),
    '（準備）エラーの返事を返す MOD につなぐ');
  await p.waitForTimeout(200);
  await clearSent();
  await hideNotice();
  const tErr = Date.now();
  rg = await call('setBlockSimple', { X: 0, Y: 0, Z: 0, BLOCK: 'gold_block_slab' });
  const errTook = Date.now() - tErr;
  m = await last('setBlock');
  // すぐ送る（エラーの返事で「分からない」と決まっているので、1 秒待たない）
  ok(rg.ok && errTook < 500 && m && m.params.blockType === 'minecraftedu:gold_block_slab' && (await noticeText()) === null,
    'エラーの返事のあと（MOD が金を知っているか分からない）は、待たずに今までどおり送る', { rg, errTook, m: m && m.params });
  await p.evaluate(() => { window.__connectFail = false; });
  // つないでいない: 金も、ほかのブロックと同じく「つないでいない」で終わる
  await call('disconnect', {});
  await clearSent();
  const rGold = await call('setBlockSimple', { X: 0, Y: 0, Z: 0, BLOCK: 'gold_block_stairs' });
  const rStone = await call('setBlockSimple', { X: 0, Y: 0, Z: 0, BLOCK: 'stone' });
  ok(!rGold.ok && !rStone.ok && rGold.error === rStone.error && (await sent()).length === 0,
    'つないでいないときは、金もほかのブロックと同じく「つないでいない」で終わる', { rGold, rStone });
  // 返事を待っている間に切れた（つなぎ直していない）: ほかのブロックと同じく「つないでいない」で終わり、どこにも送らない
  await holdGold(NEW_MOD, '待っている間に切れる');
  await p.evaluate(() => {
    window.__goldCut = null;
    vm.runtime._primitives.minecraft_setBlockSimple({ X: 0, Y: 0, Z: 0, BLOCK: 'gold_block_stairs' }, {})
      .then(() => { window.__goldCut = { ok: true, t: performance.now() }; },
        e => { window.__goldCut = { ok: false, error: String(e && e.message || e), t: performance.now() }; });
  });
  // すぐ切る（返事を 1 秒待ち終える前に切るため、ここでは待たない。置く処理は呼んだ時点で返事を待ち始めている）
  await p.evaluate(() => { window.__tCut = performance.now(); });
  await call('disconnect', {});
  const cut = await p.waitForFunction(() => window.__goldCut, null, { timeout: 3000 }).then(() => p.evaluate(() => window.__goldCut), () => null);
  const cutTook = cut ? Math.round(cut.t - await p.evaluate(() => window.__tCut)) : null;
  await p.waitForTimeout(100);
  const cutNeed = await p.evaluate(() => vm.runtime.ltMinecraftNeedConnection === true);
  const cutLamp = await lampState();
  // 切れたらすぐ（1 秒待たずに）終わり、ほかのブロックと同じく「先につないでね」でランプが点滅する
  ok(cut && !cut.ok && cut.error === rStone.error && cutTook !== null && cutTook >= 0 && cutTook < 500 &&
    (await placed()).length === 0 && cutNeed && cutLamp && cutLamp.blink === true,
    '返事を待っている間に切れると、金もほかのブロックと同じく、すぐ「つないでいない」で終わり、ランプが点滅する（どこにも送らない）',
    { cut, cutTook, cutNeed, cutLamp, stone: rStone.error });
  await p.evaluate(() => { window.__connectHold = false; });
  // 返事を待っている間に止めて、すぐ切れる（止めるボタンのあとに［つなぐのをやめる］など）:
  // 止めたのが先なので、何も言わずに終わり、どこにも送らず、ランプも点滅しない
  await holdGold(NEW_MOD, '待っている間に止めてから切れる');
  await startGold();
  await p.waitForTimeout(100);
  await p.evaluate(async () => { vm.runtime.stopAll(); await vm.runtime._primitives.minecraft_disconnect({}, {}); });
  gd = await goldDone(5);
  await p.waitForTimeout(100);
  pl = await placed();
  let quietLamp = await lampState();
  let quietNeed = await p.evaluate(() => vm.runtime.ltMinecraftNeedConnection === false);
  ok(gd.length === 5 && gd.every(x => !x.includes(':')) && pl.length === 0 && quietNeed && quietLamp && !quietLamp.blink,
    '返事を待っている間に止めてから切れると、5 つの置く処理とも何も言わずに終わり、ランプも点滅しない', { gd, pl, quietNeed, quietLamp });
  await p.evaluate(() => { window.__connectHold = false; });
  // 本物のスクリプトで金を置く（「lt_gold を受け取ったとき → 置くブロック」を 5 つの置く処理それぞれで 1 本ずつ）:
  // 置く処理が util（動いているスクリプト）を渡し、待っている間にそのスクリプトだけを止めたら送らないことを確かめる
  const GOLD_SCRIPT_OPS = [['setBlock', ['X', 'Y', 'Z']], ['setBlockRelative', ['X', 'Y', 'Z']], ['setBlockRange', ['X1', 'Y1', 'Z1', 'X2', 'Y2', 'Z2']],
    ['setBlockSimple', ['X', 'Y', 'Z']], ['setBlockRangeSimple', ['X1', 'Y1', 'Z1', 'X2', 'Y2', 'Z2']]];
  const startGoldScripts = () => p.evaluate(ops => {
    const stage = vm.runtime.getTargetForStage();
    for (const [op, nums] of ops) {
      const h = 'lt_gh_' + op;
      const b = 'lt_gb_' + op;
      if (stage.blocks.getBlock(h)) continue;
      stage.blocks.createBlock({ id: h, opcode: 'event_whenbroadcastreceived', next: b, parent: null, inputs: {},
        fields: { BROADCAST_OPTION: { name: 'BROADCAST_OPTION', value: 'lt_gold', id: 'lt_gold' } }, shadow: false, topLevel: true, x: 0, y: 900 });
      const inputs = { BLOCK: { name: 'BLOCK', block: b + '_m', shadow: b + '_m' } };
      for (const n of nums) inputs[n] = { name: n, block: b + '_' + n, shadow: b + '_' + n };
      stage.blocks.createBlock({ id: b, opcode: 'minecraft_' + op, next: null, parent: h, inputs, fields: {}, shadow: false, topLevel: false });
      stage.blocks.createBlock({ id: b + '_m', opcode: 'minecraft_menu_blockTypes', next: null, parent: b, inputs: {},
        fields: { blockTypes: { name: 'blockTypes', value: 'gold_block_stairs' } }, shadow: true, topLevel: false });
      for (const n of nums) {
        stage.blocks.createBlock({ id: b + '_' + n, opcode: 'math_number', next: null, parent: b, inputs: {},
          fields: { NUM: { name: 'NUM', value: '0' } }, shadow: true, topLevel: false });
      }
    }
    window.__goldThreads = vm.runtime.startHats('event_whenbroadcastreceived', { BROADCAST_OPTION: 'lt_gold' });
    return window.__goldThreads.length;
  }, GOLD_SCRIPT_OPS);
  // 5 本とも、MOD の返事を待っている（Promise を待っている）
  const goldScriptsWaiting = () => p.waitForFunction(() => window.__goldThreads && window.__goldThreads.length === 5 &&
    window.__goldThreads.every(t => t.status === 1 && vm.runtime.isActiveThread(t)), null, { timeout: 3000 }).then(() => true, () => false);
  // 別の（金を置かない）スクリプト: 「lt_other を受け取ったとき → 10 秒待つ」
  const startOtherScript = () => p.evaluate(() => {
    const stage = vm.runtime.getTargetForStage();
    if (!stage.blocks.getBlock('lt_oh')) {
      stage.blocks.createBlock({ id: 'lt_oh', opcode: 'event_whenbroadcastreceived', next: 'lt_ow', parent: null, inputs: {},
        fields: { BROADCAST_OPTION: { name: 'BROADCAST_OPTION', value: 'lt_other', id: 'lt_other' } }, shadow: false, topLevel: true, x: 0, y: 1100 });
      stage.blocks.createBlock({ id: 'lt_ow', opcode: 'control_wait', next: null, parent: 'lt_oh',
        inputs: { DURATION: { name: 'DURATION', block: 'lt_owd', shadow: 'lt_owd' } }, fields: {}, shadow: false, topLevel: false });
      stage.blocks.createBlock({ id: 'lt_owd', opcode: 'math_positive_number', next: null, parent: 'lt_ow', inputs: {},
        fields: { NUM: { name: 'NUM', value: '10' } }, shadow: true, topLevel: false });
    }
    window.__otherThreads = vm.runtime.startHats('event_whenbroadcastreceived', { BROADCAST_OPTION: 'lt_other' });
  });
  for (const [kind, stop, want] of [['止めない', null, 5], ['金を置くスクリプトだけを止める', 'gold', 0], ['別のスクリプトだけを止める', 'other', 5]]) {
    await holdGold(NEW_MOD, '本物のスクリプト・' + kind);
    if (stop === 'other') await startOtherScript();
    ok(await startGoldScripts() === 5 && await goldScriptsWaiting(), `（準備）本物のスクリプト・${kind}: 金を置く 5 本が返事を待っている`);
    if (stop === 'gold') await p.evaluate(() => { for (const t of window.__goldThreads) vm.runtime._stopThread(t); });
    if (stop === 'other') await p.evaluate(() => { for (const t of window.__otherThreads) vm.runtime._stopThread(t); });
    await p.evaluate(() => window.__releaseConnect());
    if (want) await p.waitForFunction(k => window.__sent.filter(x => x.action === 'setBlock' || x.action === 'fillBlocks').length >= k, want, { timeout: 3000 }).catch(() => {});
    await p.waitForTimeout(300);
    pl = await placed();
    ok(pl.length === want && pl.every(x => x.params.blockType === 'minecraftedu:gold_block_stairs'),
      `本物のスクリプトで、返事を待っている間に${kind}と、返事が来たら${want ? '5 つの置く処理とも送る' : '5 つの置く処理とも送らない'}`, pl.map(x => x.action));
    await p.evaluate(() => { window.__connectHold = false; for (const t of (window.__otherThreads || [])) vm.runtime._stopThread(t); });
  }
  // 本物のスクリプトで、返事を待っている間に金を置くスクリプトを止めて、すぐ切れる: 送らず、ランプも点滅しない
  await holdGold(NEW_MOD, '本物のスクリプト・止めてから切れる');
  ok(await startGoldScripts() === 5 && await goldScriptsWaiting(), '（準備）本物のスクリプト・止めてから切れる: 金を置く 5 本が返事を待っている');
  const logStart = logs.length;
  await p.evaluate(async () => {
    for (const t of window.__goldThreads) vm.runtime._stopThread(t);
    await vm.runtime._primitives.minecraft_disconnect({}, {});
  });
  // 5 本とも判定が動いた（「待っている間に止めたので、送りません」が 5 回出た）のを待ってから見る（判定の前に見て OK にしないため）
  const stoppedLogs = () => logs.slice(logStart).filter(x => x.includes('待っている間に止めたので、送りません')).length;
  for (let i = 0; i < 60 && stoppedLogs() < 5; i++) await p.waitForTimeout(50);
  await p.waitForTimeout(100);
  pl = await placed();
  quietLamp = await lampState();
  quietNeed = await p.evaluate(() => vm.runtime.ltMinecraftNeedConnection === false);
  ok(stoppedLogs() === 5 && pl.length === 0 && quietNeed && quietLamp && !quietLamp.blink,
    '本物のスクリプトで、返事を待っている間に止めてから切れると、5 つの置く処理とも「送りません」で終わり、送らず、ランプも点滅しない',
    { stopped: stoppedLogs(), pl, quietNeed, quietLamp });
  await p.evaluate(() => { window.__connectHold = false; });
  // もとにもどす（新しい MOD につなぎ直す）
  await hideNotice();
  ok(await reconnectWith(['vertical_oak_slab', 'gold_block_stairs', 'gold_block_slab']), '（準備）新しい MOD につなぎ直す');
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

  // 4. まわりを平らな地面にもどす・まわりの生き物やものを全部消す: 数でない・空の中心は送らない（原点のまわりを消さない）
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

  // 6. はじめの準備: 今の MOD には、生き物を呼ぶのを止める命令も、許可にもどす命令も送らない
  await clearSent();
  await call('prepareForLearning', {});
  const prep = (await sent()).filter(x => x.action === 'setEntitySpawning');
  ok(prep.length === 0, 'はじめの準備は、生き物を呼ぶのを止めない（先生が止めたのを、もどしもしない）', prep);
  ok((await sent()).some(x => x.action === 'setGameRule' && x.params.rule === 'doMobSpawning' && x.params.value === 'false'),
    'はじめの準備は、今までどおり生き物が自然に出てくるのを止める');
  // 返事がもう来ていれば、その場で（ブロックが動いたその時に）全部送る。ほかのスクリプトより後回しにしない
  const syncSent = await p.evaluate(() => {
    window.__sent = [];
    vm.runtime._primitives.minecraft_prepareForLearning({}, {});
    return window.__sent.map(x => [x.action, x.params]);
  });
  ok(JSON.stringify(syncSent) === JSON.stringify(PREP_SENT), '返事がもう来ていれば、はじめの準備はその場で 7 つの命令を送る', syncSent);

  // 7. 調べるブロックが返すブロックの名前は、メニューと同じ形（minecraft: / minecraftedu: を外す）
  let r = await call('getTargetBlock', { INFO: 'blockType' });
  ok(r.value === 'grass_block', '見ているブロックの種類: minecraft: を外す', r);
  r = await call('getTargetBlock', { INFO: 'y' });
  ok(r.value === -1, '見ているブロックの y は、今までどおり +60', r);
  r = await call('getBlockType', { X: 0, Y: 0, Z: 0 });
  ok(r.value === 'vertical_oak_slab', 'ブロックの種類: 縦スラブの minecraftedu: を外す', r);
  r = await call('getBlockUnderPlayer', {});
  ok(r.value === 'vertical_oak_slab', '足もとのブロック: 縦スラブの minecraftedu: を外す', r);
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
  ok(await p.waitForFunction(() => vm.runtime.ltMinecraftConnection === 'idle', null, { timeout: 5000 }).then(() => true, () => false), '（準備）Minecraftとつなぐのをやめる');
  let l = await lampState();
  ok(l && !l.blink, '（準備）つなぐのをやめただけでは点滅しない', l);
  await call('setBlockSimple', { X: 0, Y: 0, Z: 0, BLOCK: 'stone' });
  await p.waitForTimeout(200);
  l = await lampState();
  ok(l && l.status === 'idle' && l.blink, 'つないでいないのに置くブロックを動かすと、ランプが点滅する', l);
  await p.click('.lt-mc-lamp__button');
  l = await lampState();
  ok(l && l.hint && l.hint.includes('先にMinecraftにつないでね') && l.hint.includes('ブロックを動かすと、つながります'),
    'ランプを押すと「先にMinecraftにつないでね」と、つなぎ方が出る', l);
  ok(await p.evaluate(() => document.querySelector('.lt-mc-lamp__live').textContent.includes('先にMinecraftにつないでね')),
    '「先にMinecraftにつないでね」は、読み上げの文（role=status）にも入る');
  // 言語を変えても（ランプが作り直されても）、「先につないでね」が残る
  await p.evaluate(() => window.__store.dispatch({ type: 'scratch-gui/locales/SELECT_LOCALE', locale: 'ja-Hira' }));
  await p.waitForFunction(() => { const el = document.querySelector('.lt-mc-lamp__live'); return el && el.textContent.includes('さきにMinecraftにつないでね'); }, null, { timeout: 8000 })
    .then(() => ok(true, '言語を変えても、「先につないでね」が残る（ひらがなの文になる）'), () => ok(false, '言語を変えても、「先につないでね」が残る（ひらがなの文になる）'));
  await p.evaluate(() => window.__store.dispatch({ type: 'scratch-gui/locales/SELECT_LOCALE', locale: 'ja' }));
  await p.waitForFunction(() => { const el = document.querySelector('.lt-mc-lamp__live'); return el && el.textContent.includes('先にMinecraftにつないでね'); }, null, { timeout: 8000 }).catch(() => {});
  await p.click('.lt-mc-lamp__button'); // ヒントを開く（作り直しで閉じている）
  l = await lampState();
  ok(l && l.hint && l.hint.includes('先にMinecraftにつないでね'), '（言語をもどしたあとも、ヒントに「先につないでね」）', l);
  await p.click('.lt-mc-lamp__button');
  // つないでいないまま［Minecraftとつなぐのをやめる］すると、点滅も「先につないでね」も消える
  await call('setBlockSimple', { X: 0, Y: 0, Z: 0, BLOCK: 'stone' });
  await p.waitForTimeout(100);
  ok(((await lampState()) || {}).blink === true, '（準備）もう一度動かして、点滅している');
  await call('disconnect', {});
  await p.waitForTimeout(200);
  l = await lampState();
  const live = await p.evaluate(() => document.querySelector('.lt-mc-lamp__live').textContent);
  ok(l && !l.blink && !live.includes('先に') && await p.evaluate(() => vm.runtime.ltMinecraftNeedConnection === false),
    'つないでいないまま［Minecraftとつなぐのをやめる］すると、点滅も「先につないでね」も消える', { l, live });
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
  ok(await p.evaluate(() => document.querySelector('.lt-mc-lamp__live').textContent.includes('先にMinecraftにつないでね')),
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
    ja: { weather: [['晴れ', 'clear'], ['雨', 'rain'], ['かみなり', 'thunder']],
      rules: [['生き物が自然に出てくる', 'doMobSpawning'], ['時間を止める', 'doDaylightCycle'], ['天気を止める', 'doWeatherCycle']],
      values: [['オン', 'true'], ['オフ', 'false']], title: 'わたしの作品',
      time: [['朝', 'day'], ['昼', 'noon'], ['夕方', 'sunset'], ['夜', 'night'], ['真夜中', 'midnight']] },
    'ja-Hira': { weather: [['はれ', 'clear'], ['あめ', 'rain'], ['かみなり', 'thunder']],
      rules: [['いきものがしぜんにでてくる', 'doMobSpawning'], ['じかんをとめる', 'doDaylightCycle'], ['てんきをとめる', 'doWeatherCycle']],
      values: [['オン', 'true'], ['オフ', 'false']], title: 'わたしのさくひん',
      time: [['あさ', 'day'], ['ひる', 'noon'], ['ゆうがた', 'sunset'], ['よる', 'night'], ['まよなか', 'midnight']] },
    en: { weather: [['Clear', 'clear'], ['Rain', 'rain'], ['Thunder', 'thunder']],
      rules: [['Mob Spawning', 'doMobSpawning'], ['Stop time', 'doDaylightCycle'], ['Stop weather', 'doWeatherCycle']],
      values: [['ON', 'true'], ['OFF', 'false']], title: 'my video',
      time: [['Day', 'day'], ['Noon', 'noon'], ['Sunset', 'sunset'], ['Night', 'night'], ['Midnight', 'midnight']] }
  };
  // Minecraft のブロックの文・見出し・すべてのメニュー（その場で作るメニューも）の文を集める
  // 1 つ 1 つは [種類, ブロックかメニューの名前, 文, 値か文の名前]
  const collectTexts = () => p.evaluate(() => {
    const cat = vm.runtime._blockInfo.find(c => c.id === 'minecraft');
    const out = [];
    let label = 0;
    for (const b of cat.blocks) {
      if (b.info !== '---' && b.info.blockType === 'label') {
        const doc = new DOMParser().parseFromString(b.xml, 'text/xml');
        out.push(['label', 'label', doc.documentElement.getAttribute('text'), String(label++)]);
        continue;
      }
      if (!b.json) continue;
      for (const k of Object.keys(b.json)) {
        if (k.startsWith('message')) out.push(['block', b.json.type, b.json[k], k]);
        if (k.startsWith('args')) {
          for (const a of b.json[k]) {
            if (!a || !a.options) continue;
            for (const o of (typeof a.options === 'function' ? a.options() : a.options)) out.push(['menu', b.json.type, o[0], o[1]]);
          }
        }
      }
    }
    for (const m of cat.menus) for (const a of m.json.args0) if (a.options) for (const o of (typeof a.options === 'function' ? a.options() : a.options)) out.push(['menu', m.json.type, o[0], o[1]]);
    for (const [name, m] of Object.entries(cat.menuInfo)) {
      if (typeof m.items === 'function') for (const o of m.items()) out.push(['menu', name, o[0], o[1]]);
    }
    return out;
  });
  const textsBy = {};
  // Minecraft の文の記録（言語ごとの「鍵: 文」。下の言語のループで比べ、BLOCKS_UPDATE_TEXTS=1 のときは作り直す）
  const TEXTS_FILE = join(ROOT, 'tests', 'fixtures', 'minecraft-texts.json');
  let TEXTS_DATA = null;
  let textsReadError = null;
  try {
    if (existsSync(TEXTS_FILE)) {
      TEXTS_DATA = JSON.parse(readFileSync(TEXTS_FILE, 'utf8'));
      // 形の確かめ: { ja: {鍵: 文}, 'ja-Hira': {鍵: 文} }
      const isMap = x => !!x && typeof x === 'object' && !Array.isArray(x);
      if (!isMap(TEXTS_DATA) || !isMap(TEXTS_DATA.ja) || !isMap(TEXTS_DATA['ja-Hira'])) throw new Error('記録の形がちがう（ja と ja-Hira の「鍵: 文」が無い）');
    }
  } catch (e) {
    TEXTS_DATA = null;
    textsReadError = String(e);
  }
  if (textsReadError) console.log(`文の記録 ${TEXTS_FILE} が読めません: ${textsReadError}`);
  const textSnap = {};
  // 漢字版・ひらがな版に無いといけない訳の鍵: 英語の訳の表にある Minecraft の鍵と、gui.js の中で
  // id: 'minecraft.…' と直接書いている鍵（録画のお知らせなど、英語の表に無いものもある）。
  // 訳があるかは、gui.js のソースの訳の表（"ja": { … } など）を直接見る。ページの中の訳の表は、訳が無いと
  // format-message が英語の初めの文を書きもどす（gui.js の translate）ので、抜けていても文があるように見えるため。
  // 記録を作り直すときも確かめる（記録と比べないので、訳が抜けてもほかでは気づけないため）
  const NEED_KEYS = (() => {
    const src = readFileSync(GUI_JS, 'utf8');
    // 言語の訳の表のうち、Minecraft の鍵と文（改行は LF でも CRLF でもよい）。表がちょうど 1 つでなければ null
    const table = loc => {
      const head = new RegExp(`\\r?\\n  "${loc}": \\{\\r?\\n`, 'g');
      const starts = [...src.matchAll(head)];
      if (starts.length !== 1) return null;
      const tail = /\r?\n  \},\r?\n/g;
      tail.lastIndex = starts[0].index + starts[0][0].length;
      const end = tail.exec(src);
      if (!end) return null;
      const map = new Map();
      for (const m of src.slice(starts[0].index, end.index + 1).matchAll(/\n {4}"((?:minecraft|gui\.minecraftLamp)\.[^"]+)": "((?:[^"\\]|\\.)*)"/g)) map.set(m[1], m[2]);
      return map;
    };
    const tables = { en: table('en'), ja: table('ja'), 'ja-Hira': table('ja-Hira') };
    const keys = new Set(tables.en ? tables.en.keys() : []);
    const direct = new Set();
    for (const m of src.matchAll(/\bid:\s*['"]((?:minecraft|gui\.minecraftLamp)\.[A-Za-z0-9_.]*[A-Za-z0-9_])['"]/g)) direct.add(m[1]);
    for (const k of direct) keys.add(k);
    return { keys: [...keys].sort(), tables, enCount: tables.en ? tables.en.size : 0, directCount: direct.size };
  })();
  // 2 つの「鍵: 文」の違い（どちらかにしかない鍵も入れる）
  const textsDiff = (want, now) => [...new Set([...Object.keys(want), ...Object.keys(now)])]
    .filter(k => want[k] !== now[k]).map(k => `${JSON.stringify(k)}: ${JSON.stringify(want[k])} → ${JSON.stringify(now[k])}`);
  for (const loc of ['ja', 'ja-Hira', 'en']) {
    await setLocale(loc, EXPECT[loc].title);
    const got = await info();
    const want = EXPECT[loc];
    for (const k of ['weather', 'rules', 'values', 'time']) {
      ok(JSON.stringify(got[k]) === JSON.stringify(want[k]), `${loc}: メニュー ${k}`, got[k]);
    }
    ok(got.title === want.title, `${loc}: 動画の題名の初めの値`, got.title);
    const texts = textsBy[loc] = await collectTexts();
    const count = kind => texts.filter(t => t[0] === kind).length;
    ok(count('block') >= 46 && count('label') === 10 && count('menu') > 1500, `${loc}: 文を集めた（ブロック・見出し・メニュー）`, { block: count('block'), label: count('label'), menu: count('menu') });
    const find = (menu, value) => (texts.find(t => t[0] === 'menu' && t[1].endsWith(menu) && t[3] === value) || [])[2];
    if (loc === 'ja-Hira') {
      // ひらがなの画面では、Minecraft のブロックの文・見出し・メニューに、漢字を 1 字も出さない（々〆〇・拡張の漢字もふくむ）
      const withKanji = texts.filter(t => HAN.test(String(t[2]))).map(t => `${t[1]}: ${t[2]}`);
      ok(withKanji.length === 0, 'ja-Hira: Minecraft のブロックの文・見出し・メニュー（ブロックの名前・生き物も）に漢字が無い', withKanji.slice(0, 10));
    }
    const NAMES = { ja: ['オークの階段', 'ブタ', '村人'], 'ja-Hira': ['オークのかいだん', 'ブタ', 'むらびと'], en: ['Oak Stairs', 'Pig', 'Villager'] }[loc];
    const gotNames = [find('blockTypes', 'oak_stairs'), find('passiveEntities', 'pig'), find('villagerEntities', 'villager')];
    ok(JSON.stringify(gotNames) === JSON.stringify(NAMES), `${loc}: ブロックの名前と生き物の名前が、その言語で出る`, gotNames);
    // MOD が足した金の階段・ハーフブロックが、すべてのブロックの一覧・建築ブロック・向きのあるブロックのメニューに、その言語の名前で出る
    const GOLD = { ja: ['金ブロックの階段', '金ブロックのハーフブロック'], 'ja-Hira': ['きんブロックのかいだん', 'きんブロックのハーフブロック'],
      en: ['Gold Block Stairs', 'Gold Block Slab'] }[loc];
    for (const menu of ['blockTypes', 'buildingBlocks', 'directionalBlocks']) {
      const gotGold = [find(menu, 'gold_block_stairs'), find(menu, 'gold_block_slab')];
      ok(JSON.stringify(gotGold) === JSON.stringify(GOLD), `${loc}: メニュー ${menu} に金の階段・ハーフブロックが、その言語の名前で出る`, gotGold);
    }
    // 代表の文が、決めたとおりの漢字版・ひらがな版になっている（今の言語の訳の表を直接見る）
    const WANT_MSG = {
      ja: {
        'minecraft.connect': 'Minecraftにつなぐ [HOST]:[PORT]',
        'minecraft.setBlockSimple': 'X:[X] Y:[Y] Z:[Z] に [BLOCK] を置く',
        'minecraft.setBlockRangeSimple': 'X:[X1] Y:[Y1] Z:[Z1] から X:[X2] Y:[Y2] Z:[Z2] まで [BLOCK] でうめる',
        'minecraft.setBlock': 'X:[X] Y:[Y] Z:[Z] に [BLOCK] を置く、置き方:[PLACEMENT] 向き:[FACING] あけしめ:[OPEN]',
        'minecraft.summonEntity': '[ENTITY] を X:[X] Y:[Y] Z:[Z] に呼び出す',
        'minecraft.clearArea': 'X:[X] Z:[Z] のまわりを平らな地面にもどす',
        'minecraft.entityHostile': '敵の生き物 [ENTITY]',
        'minecraft.menu.bottom': '下（ふつう）', 'minecraft.menu.top': '上（さかさま）', 'minecraft.menu.double': '2まい重ね',
        'minecraft.menu.default': 'そのまま', 'minecraft.menu.closed': 'しめる', 'minecraft.menu.open': 'あける',
        'minecraft.entityGroup.passive': '━━ おとなしい生き物 ━━', 'minecraft.entityGroup.hostile': '━━ 敵の生き物 ━━',
        'minecraft.entityGroup.neutral': '━━ おこるとこわい生き物 ━━', 'minecraft.getPlayerFacing': 'プレイヤーの向き',
        'minecraft.prepareForLearning': 'はじめの準備（昼・晴れのまま・生き物が自然に出ない・暗くても見える・歩く速さをふつうに）'
      },
      'ja-Hira': {
        'minecraft.connect': 'Minecraftにつなぐ [HOST]:[PORT]',
        'minecraft.setBlockSimple': 'X:[X] Y:[Y] Z:[Z] に [BLOCK] をおく',
        'minecraft.setBlockRangeSimple': 'X:[X1] Y:[Y1] Z:[Z1] から X:[X2] Y:[Y2] Z:[Z2] まで [BLOCK] でうめる',
        'minecraft.setBlock': 'X:[X] Y:[Y] Z:[Z] に [BLOCK] をおく、おきかた:[PLACEMENT] むき:[FACING] あけしめ:[OPEN]',
        'minecraft.summonEntity': '[ENTITY] を X:[X] Y:[Y] Z:[Z] によびだす',
        'minecraft.clearArea': 'X:[X] Z:[Z] のまわりをたいらなじめんにもどす',
        'minecraft.entityHostile': 'てきのいきもの [ENTITY]',
        'minecraft.menu.bottom': 'した（ふつう）', 'minecraft.menu.top': 'うえ（さかさま）', 'minecraft.menu.double': '2まいかさね',
        'minecraft.menu.default': 'そのまま', 'minecraft.menu.closed': 'しめる', 'minecraft.menu.open': 'あける',
        'minecraft.entityGroup.passive': '━━ おとなしいいきもの ━━', 'minecraft.entityGroup.hostile': '━━ てきのいきもの ━━',
        'minecraft.entityGroup.neutral': '━━ おこるとこわいいきもの ━━', 'minecraft.getPlayerFacing': 'プレイヤーのむき',
        'minecraft.prepareForLearning': 'はじめのじゅんび（ひる・はれのまま・いきものがしぜんにでない・くらくてもみえる・あるくはやさをふつうに）'
      }
    }[loc];
    if (WANT_MSG) {
      // 訳の表の値は、文そのものか { message: 文 } のどちらか（動かすと Minecraft の文は後の形で来る）。文にそろえる
      const msgs = Object.fromEntries(Object.entries(await p.evaluate(() => window.__store.getState().locales.messages))
        .map(([k, v]) => [k, typeof v === 'string' ? v : (v && typeof v.message === 'string' ? v.message : null)]));
      const wrongMsg = Object.entries(WANT_MSG).filter(([k, v]) => msgs[k] !== v).map(([k]) => `${k} = ${msgs[k]}`);
      ok(wrongMsg.length === 0, `${loc}: 代表のブロックの文・メニュー・見出しが、決めたとおりの文`, wrongMsg);
      // 案内の文（ランプのヒント・録画のお知らせ）の「」や［］の中のブロックの名前が、パレットにある本当のブロックの文と同じ
      // （ブロックの文の入力より前の部分か、文そのもの）。名前を書いている文は REF_KEYS に決めておき、
      // かぎかっこが取れたり、文が無くなったり、ほかの文に名前が増えたりしたら気づくようにする
      const REF_KEYS = ['minecraft.recording.alreadyStarted', 'minecraft.recording.notStarted', 'minecraft.recording.stopError',
        'minecraft.recording.notConnected', 'minecraft.recording.serverStopping', 'minecraft.recording.connectionClosed',
        'gui.minecraftLamp.idleHint', 'gui.minecraftLamp.failedStep3'];
      // ブロックの名前でないかぎかっこ
      const NOT_BLOCK = new Set(['ffmpegを入れてください', 'ffmpegをいれてください']);
      // パレットにある本当のブロックの文（collectTexts で集めたもの。入力は %3 などになっていて、
      // 頭の「%1 %2」は拡張のアイコンの場所なので外す）
      const blockTexts = texts.filter(t => t[0] === 'block').map(t => String(t[2]).replace(/^%1 %2/, ''));
      const badRefs = [];
      const refKeysFound = [];
      let refCount = 0;
      for (const k of REF_KEYS) if (typeof msgs[k] !== 'string') badRefs.push(`${k}: 訳の文が無い（${msgs[k]}）`);
      for (const [k, v] of Object.entries(msgs)) {
        if (!/^(minecraft\.recording\.|gui\.minecraftLamp\.)/.test(k) || !v) continue;
        // 「」どうし・［］どうしの組だけを取る
        for (const m of v.matchAll(/「([^「」]+)」|［([^［］]+)］/g)) {
          const name = m[1] ?? m[2];
          if (NOT_BLOCK.has(name)) continue;
          refCount++;
          if (!refKeysFound.includes(k)) refKeysFound.push(k);
          if (!blockTexts.some(t => t === name || t.split(/%\d/)[0].trim() === name)) badRefs.push(`${k}: ${name}`);
        }
      }
      const missingRef = REF_KEYS.filter(k => !refKeysFound.includes(k));
      const extraRef = refKeysFound.filter(k => !REF_KEYS.includes(k));
      ok(blockTexts.length >= 46 && refCount === REF_KEYS.length && missingRef.length === 0 && extraRef.length === 0 && badRefs.length === 0,
        `${loc}: 案内の文の中のブロックの名前（決めた ${REF_KEYS.length} か所）が、パレットにあるブロックの文と同じ`,
        { refCount, missingRef, extraRef, badRefs, blockTexts: blockTexts.length });
      // 漢字版・ひらがな版の Minecraft の文（minecraft.* と gui.minecraftLamp.* の鍵）をぜんぶ、記録（tests/fixtures/minecraft-texts.json）と比べる。
      // ことばを決めて変えたときは BLOCKS_UPDATE_TEXTS=1 で記録を作り直す
      // 訳の鍵の取りちがえ（例: 調べるブロックが、テレポートの文の鍵を使う。置き方のメニューの値 bottom に「上（さかさま）」の
      // 鍵をつける）は、訳の表を見ても分からないので、パレットのブロックごとの文（palette.ブロックの名前.message0 など）・
      // 見出しの文（palette.label.番号）・メニューの項目（palette.menu.ブロックかメニューの名前.値#出てきた順）も記録に入れて比べる。
      // メニューは、ブロックの中のメニューと、拡張のメニューのブロック（minecraft_menu_…）の 2 つから取る
      // （collectTexts の 3 つ目の、その場で作るメニューの元の一覧は、拡張のメニューのブロックと同じものなので入れない）
      const menuSeen = new Map();
      const paletteTexts = texts.filter(t => t[0] === 'block' || t[0] === 'label' || (t[0] === 'menu' && t[1].startsWith('minecraft_')))
        .map(t => {
          if (t[0] === 'label') return [`palette.label.${t[3]}`, t[2]];
          if (t[0] === 'block') return [`palette.${t[1]}.${t[3]}`, t[2]];
          const base = `palette.menu.${t[1]}.${t[3]}`;
          const n = menuSeen.get(base) || 0;
          menuSeen.set(base, n + 1);
          return [`${base}#${n}`, t[2]];
        });
      textSnap[loc] = Object.fromEntries([...Object.entries(msgs).filter(([k]) => /^(minecraft\.|gui\.minecraftLamp\.)/.test(k)), ...paletteTexts]
        .sort(([a], [b]) => (a < b ? -1 : 1)));
      // gui.js のソースの、この言語の訳の表に、使う鍵がすべて（空でない文で）ある
      const srcTable = NEED_KEYS.tables[loc];
      const missingKeys = srcTable ? NEED_KEYS.keys.filter(k => !srcTable.get(k)) : ['（gui.js の中に、この言語の訳の表がちょうど 1 つ見つからない）'];
      ok(NEED_KEYS.enCount > 100 && NEED_KEYS.directCount > 10 && missingKeys.length === 0,
        `${loc}: 使う Minecraft の訳の鍵（英語の表の ${NEED_KEYS.enCount} 個と、gui.js に直接書いた ${NEED_KEYS.directCount} 個。あわせて ${NEED_KEYS.keys.length} 個）が、gui.js の訳の表にすべてある`,
        missingKeys.slice(0, 20));
      if (loc === 'ja-Hira') {
        const hiraKanji = Object.entries(textSnap[loc]).filter(([, v]) => typeof v === 'string' && HAN.test(v)).map(([k, v]) => `${k}: ${v}`);
        ok(hiraKanji.length === 0, 'ja-Hira: Minecraft の文（録画のお知らせ・ランプのヒント・一時停止の帯もふくむ）に漢字が無い', hiraKanji.slice(0, 10));
      }
      const textDiff = textsDiff((TEXTS_DATA && TEXTS_DATA[loc]) || {}, textSnap[loc]);
      if (process.env.BLOCKS_UPDATE_TEXTS === '1') {
        // 作り直すときは比べずに、前の記録から変わる所を出す（意図しない変化に気づくため）
        if (!TEXTS_DATA) console.log(`${loc}: 前の文の記録が${textsReadError ? '読めない' : '無い'}ので、全部が新しく入ります`);
        console.log(`${loc}: 文の記録を作り直すと、${textDiff.length} 件変わります:`);
        for (const d of textDiff) console.log(`  ${d}`);
      } else if (TEXTS_DATA) {
        const want = TEXTS_DATA[loc] || {};
        ok(Object.keys(want).length > 100 && textDiff.length === 0,
          `${loc}: Minecraft の文が、記録（tests/fixtures/minecraft-texts.json）と同じ（違い ${textDiff.length} 件）`, textDiff.slice(0, 20));
      } else {
        ok(false, `${loc}: 文の記録 tests/fixtures/minecraft-texts.json が${textsReadError ? `読めない（${textsReadError}）` : '無い'}（BLOCKS_UPDATE_TEXTS=1 で作る）`);
      }
    }
    ok(!got.facingCheckbox && !got.underCheckbox, `${loc}: プレイヤーの向き・足もとのブロックに、ステージに表示するチェックボックスが無い`, got);
  }
  // 漢字版にある Minecraft の訳の鍵は、ひらがな版にもある（ひらがな版にだけ訳が抜けていないか。gui.js のソースの訳の表で比べる）
  {
    const { ja, 'ja-Hira': hira } = NEED_KEYS.tables;
    const onlyJa = ja && hira ? [...ja.keys()].filter(k => !hira.get(k)) : ['（gui.js の中に、漢字版かひらがな版の訳の表が見つからない）'];
    ok(!!ja && ja.size > 100 && onlyJa.length === 0, '漢字版にある Minecraft の訳の鍵が、ひらがな版にもすべてある（gui.js の訳の表）', onlyJa.slice(0, 20));
  }
  // 訳の抜け: 漢字やひらがなの文が英語と同じになっていたら、その言語の訳が抜けて英語が出ている（決まった例外を除く）
  {
    // 英語と同じ文でよいもの（どの言語でも同じ名前）
    const ALLOW_SAME_AS_EN = new Set(['TNT']);
    // 同じ所どうしを結ぶ鍵。メニューの見出し（値が空）のように同じ鍵が何度も出るものは、出てきた順の番号を付けて分ける
    const toMap = list => {
      const seen = new Map();
      return new Map(list.map(t => {
        const base = `${t[0]}|${t[1]}|${t[3]}`;
        const n = seen.get(base) || 0;
        seen.set(base, n + 1);
        return [`${base}#${n}`, t[2]];
      }));
    };
    const maps = Object.fromEntries(Object.entries(textsBy).map(([loc, list]) => [loc, toMap(list)]));
    const missing = [];
    for (const [k, en] of maps.en) {
      const ja = maps.ja.get(k);
      const hira = maps['ja-Hira'].get(k);
      if (ja === undefined || hira === undefined) { missing.push(`${k}（ja か ja-Hira に無い）`); continue; }
      // 英語の単語（2 文字以上）をふくまない文（「%1 %2%3」や「x」など）は、言語でちがわないので比べない
      if (!/[A-Za-z]{2,}/.test(String(en).replace(/%\d+/g, ''))) continue;
      // 英語と同じでよいもの（どの言語でも同じ名前）だけを許す。漢字とひらがなの両方とも抜けても見つける
      if (ALLOW_SAME_AS_EN.has(en)) continue;
      if (hira === en) missing.push(`ja-Hira: ${k} = ${en}`);
      if (ja === en) missing.push(`ja: ${k} = ${en}`);
    }
    ok(missing.length === 0, '漢字・ひらがなの訳が抜けて、英語が出ている所が無い', missing.slice(0, 10));
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
  const deTexts = await collectTexts();
  const deFind = (menu, value) => (deTexts.find(t => t[0] === 'menu' && t[1].endsWith(menu) && t[3] === value) || [])[2];
  const deEntities = [deFind('passiveEntities', 'pig'), deFind('villagerEntities', 'villager'), (deTexts.find(t => t[1].endsWith('entityTypes') && String(t[2]).startsWith('━━')) || [])[2]];
  ok(JSON.stringify(deEntities) === JSON.stringify(['Pig', 'Villager', '━━ Passive mobs ━━']),
    '訳の無い言語では、生き物の名前と見出しは英語で出る（前は日本語が出ていた）', deEntities);
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
  ok(oldPrep.length === 1 && oldPrep[0].params.enabled === false, '古い MOD: はじめの準備は、今までどおり生き物を呼ぶのを止める（作品を守る仕組みが無いため）', oldPrep);
  await call('disconnect', {});
  await p.evaluate(() => { window.__features = ['stopAll', 'pauseQueue']; });

  // 12. つないだ直後、MOD の返事（新しい MOD か）が来る前: 古い MOD と同じに守り、はじめの準備は返事を待ってから決める
  // 返事を待たせてつなぐ
  const holdConnect = async (features, kind) => {
    await p.evaluate(f => { window.__features = f; window.__connectHold = true; window.__releaseConnect = null; }, features);
    await call('connect', { HOST: '127.0.0.1', PORT: 59999 });
    ok(await p.waitForFunction(() => vm.runtime.ltMinecraftConnection === 'connected' && typeof window.__releaseConnect === 'function', null, { timeout: 5000 })
      .then(() => true, () => false), `（準備）${kind}: つながり、返事を待たせている`);
    await clearSent();
  };
  // はじめの準備を始める（終わった時刻と、成功か失敗かを記録する）
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
  ok(!(await last('clearArea')), '返事の前は、中心が数でない「まわりを平らな地面にもどす」を送らない');
  // 新しい MOD（返事が 0.3 秒・0.7 秒遅れる）: 返事を待ち、返事が来たらすぐ、生き物の命令なしで 7 つを送る
  for (const waitMs of [300, 700]) {
    if (waitMs !== 300) await holdConnect(['stopAll', 'pauseQueue'], '新しい MOD');
    await clearSent();
    await startPrep();
    await p.waitForTimeout(waitMs);
    ok((await prepActions()).length === 0 && await p.evaluate(() => window.__prep === null), `新しい MOD（返事が ${waitMs / 1000} 秒遅れる）: 返事の前は、はじめの準備はまだ送らずに待つ`);
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
  // 返事が来ない MOD: 1 秒待ってから、古い MOD として送る（最初の命令が送られるのは、はじめの準備を始めてから 1 秒以降）
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
  // 待っている間に止める: 返事が来ても、はじめの準備の命令は送らない。古い MOD には生き物を止める命令だけを送る
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
      `待っている間に${kind}で止めると、${noReply ? '1 秒たってから' : '返事が来ても'}${want.length ? '生き物を止める命令だけを送る' : 'はじめの準備の命令は送らない'}（「完了」も出さない）`, { res, acts, firstAt });
    await p.evaluate(() => { window.__connectHold = false; });
    await call('disconnect', {});
  }
  // 本物のスクリプトではじめの準備を動かす（「〜を受け取ったとき → はじめの準備 → 変数を『おわり』にする」）
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
  // はじめの準備のスクリプトが、MOD の返事を待っている（Promise を待っている）ことを確かめる
  const prepScriptWaiting = () => p.waitForFunction(() => {
    const t = window.__prepThreads && window.__prepThreads[0];
    return !!t && t.status === 1 && vm.runtime.isActiveThread(t);
  }, null, { timeout: 3000 }).then(() => true, () => false);
  // 止めない: 返事が来たら 7 つ送り、次のブロックまで進む
  await holdConnect(['stopAll', 'pauseQueue'], '本物のスクリプト');
  logs.length = 0;
  await startPrepScript();
  ok(await prepScriptWaiting(), '（準備）本物のスクリプトのはじめの準備が、返事を待っている');
  await release();
  const done = await p.waitForFunction(() => vm.runtime.getTargetForStage().lookupVariableById('lt_pdone').value === 'おわり', null, { timeout: 3000 })
    .then(() => true, () => false);
  // 「完了」の記録は、ページの中で書かれてから少し遅れて届くので、少し待つ
  const doneLog = done && await (async () => {
    for (let i = 0; i < 20 && !logs.includes('学習準備完了'); i++) await p.waitForTimeout(50);
    return logs.includes('学習準備完了');
  })();
  ok(done && doneLog && JSON.stringify(await prepActions()) === JSON.stringify(PREP_SENT),
    '本物のスクリプトで止めずにいると、返事が来たら 7 つの命令を送り、次のブロックまで進む（「学習準備完了」も出る）', { done, doneLog, acts: await prepActions() });
  await p.evaluate(() => { window.__connectHold = false; });
  await call('disconnect', {});
  // はじめの準備を動かしたスクリプトだけを止める（もう一度押したとき）: 新しい MOD には送らない。古い MOD には生き物を止める命令だけ
  for (const [features, kind, want] of [[['stopAll', 'pauseQueue'], 'スクリプトだけを止める', []],
    [[], 'スクリプトだけを止める（古い MOD）', [['setEntitySpawning', { enabled: false }]]]]) {
    await holdConnect(features, kind);
    logs.length = 0;
    await startPrepScript();
    ok(await prepScriptWaiting(), `（準備）${kind}: はじめの準備が返事を待っている`);
    await p.evaluate(() => { for (const t of window.__prepThreads) vm.runtime._stopThread(t); });
    await release();
    await p.waitForTimeout(400);
    acts = await prepActions();
    ok(JSON.stringify(acts) === JSON.stringify(want) && !logs.includes('学習準備完了'),
      `待っている間に、はじめの準備を動かしたスクリプトだけを止めると、返事が来ても${want.length ? '生き物を止める命令だけを送る' : '送らない'}`, acts);
    await p.evaluate(() => { window.__connectHold = false; });
    await call('disconnect', {});
  }
  // 待っている間に［Minecraftとつなぐのをやめる］: すぐ終わり、何も送らず、ランプも点滅しない
  await holdConnect(['stopAll', 'pauseQueue'], '［Minecraftとつなぐのをやめる］');
  await startPrep();
  await p.waitForTimeout(100);
  const tCut = await p.evaluate(() => performance.now());
  await call('disconnect', {});
  res = await prepResult();
  await p.waitForTimeout(200);
  l = await lampState();
  ok(res && res.ok && res.t - tCut < 300 && (await prepActions()).length === 0 && l && !l.blink && await p.evaluate(() => vm.runtime.ltMinecraftNeedConnection === false),
    '待っている間に［Minecraftとつなぐのをやめる］すると、すぐ終わり、何も送らず、ランプも点滅しない', { took: res && Math.round(res.t - tCut), l });
  // 待っている間に、［Minecraftとつなぐのをやめる］せずに別の場所（番号）へつなぎ直す: 前のはじめの準備の命令は、新しい接続へ送らない
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
    '待っている間に別の場所へつなぎ直しても、前のはじめの準備の命令は新しい接続へ送らない', { res, reSent });
  await call('disconnect', {});
  await p.evaluate(() => { window.__features = ['stopAll', 'pauseQueue']; window.__connectHold = false; });

  // 13. パレットの並び順と見出し（まとまりごとに見出し、まとまりの間に区切り）
  const GROUP_ORDER = [
    ['connect', ['connect', 'disconnect', 'isConnected', 'prepareForLearning']],
    // けす（元にもどせません）は、つなぐ・じゅんびのすぐ下（2026-11-10 ユーザーの指示）
    ['clear', ['clearArea', 'clearAllEntities']],
    ['move', ['teleport', 'chat']],
    ['place', ['setBlockSimple', 'setBlockRangeSimple', 'setBlock', 'setBlockRange', 'setBlockRelative']],
    ['blockNames', ['blockBuilding', 'blockLighting', 'blockDecoration', 'blockNature', 'blockFunctional', 'blockOre', 'blockSpecial']],
    ['mobs', ['summonEntity', 'entityPassive', 'entityNeutral', 'entityHostile', 'entityAquatic', 'entityVillager', 'entityOther', 'entityBoss']],
    ['world', ['setWeather', 'setTime', 'setGameRule', 'setGameMode', 'setNightVision', 'setMoveSpeed', 'setFlySpeed']],
    ['items', ['clearInventory', 'setContainerItem']],
    ['check', ['getPosition', 'getPlayerFacing', 'getTargetBlock', 'getBlockUnderPlayer', 'getBlockUnderPlayerPos', 'getBlockType']],
    ['recording', ['startRecording', 'stopRecording', 'isRecording', 'uploadVideo']]
  ];
  const wantOrder = GROUP_ORDER.flatMap(([, ops], i) => [...(i ? ['---'] : []), 'label', ...ops]);
  const LABELS = {
    ja: ['つなぐ・じゅんび', '消す（元にもどせません）', '動く・話す', 'ブロックを置く', 'ブロックの名前', '生き物', '天気・時間・ルール', '持ち物・入れ物', '場所やブロックを調べる', '録画'],
    'ja-Hira': ['つなぐ・じゅんび', 'けす（もとにもどせません）', 'うごく・はなす', 'ブロックをおく', 'ブロックのなまえ', 'いきもの', 'てんき・じかん・ルール', 'もちもの・いれもの', 'ばしょやブロックをしらべる', 'ろくが'],
    en: ['Connect & get ready', 'Clear (cannot be undone)', 'Move & talk', 'Place blocks', 'Block names', 'Mobs', 'Weather, time & rules', 'Items & containers', 'Check places & blocks', 'Recording']
  };
  // 実際にパレットへ渡す XML（隠すブロックは除かれる）と、パレットに描かれた見出しを読む
  const palette = () => p.evaluate(() => {
    const cat = vm.runtime._blockInfo.find(c => c.id === 'minecraft');
    const entry = vm.runtime.getBlocksXML(vm.editingTarget).find(c => c.id === 'minecraft');
    const doc = new DOMParser().parseFromString(entry.xml, 'text/xml');
    const items = Array.from(doc.documentElement.children).map(el => {
      if (el.tagName === 'block') return el.getAttribute('type').replace(/^minecraft_/, '');
      if (el.tagName === 'label') return 'label:' + el.getAttribute('text');
      if (el.tagName === 'sep') return '---';
      return el.tagName;
    });
    const all = Array.from(document.querySelectorAll('.blocklyFlyout .blocklyFlyoutLabelText')).map(t => t.textContent);
    return {
      order: cat.blocks.map(b => b.info === '---' ? '---' : (b.info.blockType === 'label' ? 'label' : b.info.opcode)),
      visible: items,
      parseError: !!doc.querySelector('parsererror'),
      // パレットの見出しのうち、Minecraft のカテゴリーの名前より後ろ（Scratch のカテゴリーの名前を除く）
      shown: all.slice(all.lastIndexOf('Minecraft') + 1)
    };
  });
  // 今までの作品が開けることを守る記録（下の「今までの作品が開ける」と、言語ごとの入力の確かめで使う）
  const FIXTURE = join(ROOT, 'tests', 'fixtures', 'minecraft-blocks-compat.json');
  let FIXTURE_DATA = null;
  let fixtureReadError = null;
  try {
    if (existsSync(FIXTURE)) {
      FIXTURE_DATA = JSON.parse(readFileSync(FIXTURE, 'utf8'));
      // 形の確かめ: { blocks: {op: {blockType, arguments: {}}}, menus: {名前: {acceptReporters, values: []}} }
      const isMap = x => !!x && typeof x === 'object' && !Array.isArray(x);
      const okShape = isMap(FIXTURE_DATA) && isMap(FIXTURE_DATA.blocks) && isMap(FIXTURE_DATA.menus) &&
        Object.values(FIXTURE_DATA.blocks).every(b => isMap(b) && isMap(b.arguments) && Object.values(b.arguments).every(isMap)) &&
        Object.values(FIXTURE_DATA.menus).every(m => isMap(m) && Array.isArray(m.values));
      if (!okShape) throw new Error('記録の形がちがう（blocks と menus が無いか、中の形がちがう）');
    }
  } catch (e) {
    FIXTURE_DATA = null;
    fixtureReadError = String(e);
  }
  if (fixtureReadError) console.log(`作品の記録 ${FIXTURE} が読めません: ${fixtureReadError}`);
  // 作り直す記録（最後に、失敗が 0 のときだけ書く）
  const pendingWrites = [];
  // 実際に出るのは、隠しているボスを除いた 46 個
  const wantVisibleBlocks = GROUP_ORDER.flatMap(([, ops]) => ops).filter(op => op !== 'entityBoss');
  for (const [loc, title] of [['ja', 'わたしの作品'], ['ja-Hira', 'わたしのさくひん'], ['en', 'my video']]) {
    await setLocale(loc, title);
    const pal = await palette();
    ok(JSON.stringify(pal.order) === JSON.stringify(wantOrder), `${loc}: 拡張の中の並び順（見出し・区切り・ブロック）`, pal.order);
    const wantVisible = GROUP_ORDER.flatMap(([, ops], i) => [...(i ? ['---'] : []), 'label:' + LABELS[loc][i], ...ops.filter(op => op !== 'entityBoss')]);
    const visibleBlocks = pal.visible.filter(x => !x.startsWith('label:') && x !== '---');
    ok(!pal.parseError && JSON.stringify(visibleBlocks) === JSON.stringify(wantVisibleBlocks),
      `${loc}: パレットに出るブロックは 46 個（隠しているボスは出ない）で、決めた順番`, visibleBlocks.length);
    ok(JSON.stringify(pal.visible.filter(x => x === '---' || x.startsWith('label:') || wantVisibleBlocks.includes(x))) === JSON.stringify(wantVisible),
      `${loc}: パレットの XML の見出しの文・区切り・ブロックの並び`, pal.visible);
    // パレットの描き直しは、ブロックの定義が変わったあと少し遅れて起きるので、描かれた見出しがそろうまで待つ
    const drawn = await p.waitForFunction(want => {
      const all = Array.from(document.querySelectorAll('.blocklyFlyout .blocklyFlyoutLabelText')).map(t => t.textContent);
      return JSON.stringify(all.slice(all.lastIndexOf('Minecraft') + 1)) === JSON.stringify(want);
    }, LABELS[loc], { timeout: 5000 }).then(() => true, () => false);
    ok(drawn, `${loc}: Minecraft の所に、見出しが順番どおりに文字で描かれている`, drawn ? undefined : (await palette()).shown);
    // ブロックの入力は、その言語の訳の文の [X] から作られる。訳の文から [X] が抜けると、その言語でだけ入力が消えるので確かめる
    const { lostInputs, lostDefInputs } = await p.evaluate(fx => {
      const cat = vm.runtime._blockInfo.find(c => c.id === 'minecraft');
      const lost = [];
      const lostDef = [];
      for (const b of cat.blocks) {
        if (!b.json) continue;
        const op = b.json.type.replace(/^minecraft_/, '');
        const names = new Set();
        for (const k of Object.keys(b.json)) {
          if (k.startsWith('args')) for (const a of b.json[k]) if (a && a.name) names.add(a.name);
        }
        // 今のブロックの定義にある入力（作品の記録はこれから作るので、作り直すときもここは確かめる）
        for (const arg of Object.keys((b.info && b.info.arguments) || {})) if (!names.has(arg)) lostDef.push(`${op}.${arg}`);
        const want = fx && fx.blocks[op];
        if (!want) continue;
        for (const arg of Object.keys(want.arguments)) if (!names.has(arg)) lost.push(`${op}.${arg}`);
      }
      return { lostInputs: lost, lostDefInputs: lostDef };
    }, FIXTURE_DATA);
    ok(lostDefInputs.length === 0, `${loc}: 訳の文に、今のブロックの定義の入力がすべてある（訳で入力が消えていない）`, lostDefInputs);
    if (process.env.BLOCKS_UPDATE_FIXTURE === '1') {
      if (lostInputs.length) console.log(`${loc}: 作品の記録を作り直すと、記録にある次の入力が消えます: ${JSON.stringify(lostInputs)}`);
    } else {
      ok(FIXTURE_DATA && lostInputs.length === 0, `${loc}: 訳の文に、記録にある入力がすべてある（訳で入力が消えていない）`,
        FIXTURE_DATA ? lostInputs : `作品の記録が${fixtureReadError ? `読めない（${fixtureReadError}）` : '無い'}`);
    }
  }
  // 見出しはブロックではない: 処理を持つ Minecraft のブロックは、ちょうど 47 個（見出しは入らない）
  const prims = await p.evaluate(() => Object.keys(vm.runtime._primitives).filter(k => k.startsWith('minecraft_') && !k.startsWith('minecraft_menu_')).sort());
  ok(JSON.stringify(prims) === JSON.stringify(GROUP_ORDER.flatMap(([, ops]) => ops).map(op => 'minecraft_' + op).sort()),
    '処理を持つ Minecraft のブロックは 47 個で、見出しは入らない', prims);
  // 見出しの部品: 訳す前の文（{id, default}）も訳して出す。文が無い見出しは外す（「undefined」と出さない）
  await setLocale('ja', 'わたしの作品');
  // 拡張の読み込み（_prepareExtensionInfo）から XML まで通す。よい見出し 2 つだけが残り、訳される
  const labelPath = await p.evaluate(() => {
    const info = vm.extensionManager._prepareExtensionInfo('minecraft', { id: 'lttest', name: 'T', blocks: [
      { blockType: 'label', text: 'もじの見出し' },
      { blockType: 'label', text: { id: 'minecraft.label.place', default: 'Place blocks' } },
      { blockType: 'label' },
      { blockType: 'label', text: '' },
      { blockType: 'label', text: { default: 'x' } },
      { blockType: 'label', text: { id: 'minecraft.label.place' } },
      { blockType: 'label', text: { id: '', default: 'x' } },
      { blockType: 'label', text: { id: 'x', default: '' } },
      { blockType: 'label', text: 5 }
    ] });
    return info.blocks.map(b => vm.runtime._convertForScratchBlocks(b, { id: 'lttest' }).xml);
  });
  ok(JSON.stringify(labelPath) === JSON.stringify(['<label text="もじの見出し"></label>', '<label text="ブロックを置く"></label>']),
    '見出しは、文字か {id, default} の形だけが残って訳される（文が無い・訳せない形は外れ、「undefined」「[object Object]」と出ない）', labelPath);
  // 今までの作品が開けること: 記録にあるブロック・入力・メニューの値が、今もすべて同じ形である
  // （足すのはよい。並び順と文言は比べない。記録を作り直すときは BLOCKS_UPDATE_FIXTURE=1 で流す）
  const compat = await p.evaluate(() => {
    const sortObj = o => Object.keys(o).sort().reduce((r, k) => { r[k] = o[k]; return r; }, {});
    const cat = vm.runtime._blockInfo.find(c => c.id === 'minecraft');
    const blocks = {};
    for (const b of cat.blocks) {
      const i = b.info;
      if (i === '---' || i.blockType === 'label') continue;
      const args = {};
      for (const [k, a] of Object.entries(i.arguments || {})) args[k] = { type: a.type, menu: a.menu || null };
      // パレットに出すか隠すか（hideFromPalette）は、作品が開けるかに関係しないので入れない
      blocks[i.opcode] = { blockType: i.blockType, arguments: sortObj(args) };
    }
    const menus = {};
    for (const [name, m] of Object.entries(cat.menuInfo)) {
      menus[name] = { acceptReporters: !!m.acceptReporters,
        // items は、項目の並びか、関数（その場で作るメニュー。呼ぶと [文, 値] の並びを返す）
        values: (Array.isArray(m.items) ? m.items : (typeof m.items === 'function' ? m.items() : []))
          .map(it => (Array.isArray(it) ? it[1] : (typeof it === 'string' ? it : it.value))) };
    }
    return { blocks: sortObj(blocks), menus: sortObj(menus) };
  });
  const fixture = FIXTURE_DATA;
  // 記録にあるものが、今もすべて同じ形であるか（足りない・変わったものを、名前で出す）
  const broken = [];
  // 記録の中身の確かめ（消えるものとは別に出す）。前の記録にも、作り直して書く記録にも使う
  // 記録は減らさない（減ると、今までの作品が開けなくなる）。中身が空の記録で確かめが空のまま通らないように、
  // 記録を作ったときの数より少なければ失敗にする。記録を作り直してブロックやメニューが増えたら、この数も上げる
  const FIXTURE_MIN = { blocks: 47, menus: 29 };
  const fixtureProblems = fx => {
    const out = [];
    if (Object.keys(fx.blocks).length < FIXTURE_MIN.blocks || Object.keys(fx.menus).length < FIXTURE_MIN.menus) {
      out.push(`記録の中身が少なすぎる（ブロック ${Object.keys(fx.blocks).length}・メニュー ${Object.keys(fx.menus).length}。` +
        `FIXTURE_MIN の ${FIXTURE_MIN.blocks}・${FIXTURE_MIN.menus} より少ない）`);
    }
    // 記録の中の入力が使うメニューは、記録のメニューにもある（記録の中でつじつまが合う）
    const noMenu = [...new Set(Object.values(fx.blocks).flatMap(fb => Object.values(fb.arguments).map(fa => fa.menu))
      .filter(m => m && !Object.hasOwn(fx.menus, m)))];
    if (noMenu.length) out.push(`記録の入力が使うメニューが、記録のメニューに無い: ${noMenu.join(', ')}`);
    return out;
  };
  const badFixture = [];
  if (!fixture) {
    badFixture.push(fixtureReadError ? `記録が読めない（${fixtureReadError}）` : '記録のファイルが無い');
  } else {
    badFixture.push(...fixtureProblems(fixture));
    for (const [op, fb] of Object.entries(fixture.blocks)) {
      const cb = Object.hasOwn(compat.blocks, op) ? compat.blocks[op] : undefined;
      if (!cb) { broken.push(`ブロックが無い: ${op}`); continue; }
      if (cb.blockType !== fb.blockType) broken.push(`ブロックの形が変わった: ${op}`);
      for (const [arg, fa] of Object.entries(fb.arguments)) {
        const ca = Object.hasOwn(cb.arguments, arg) ? cb.arguments[arg] : undefined;
        if (!ca) broken.push(`入力が無い: ${op}.${arg}`);
        else if (ca.type !== fa.type || ca.menu !== fa.menu) broken.push(`入力の種類・メニューが変わった: ${op}.${arg}`);
      }
    }
    for (const [name, fm] of Object.entries(fixture.menus)) {
      const cm = Object.hasOwn(compat.menus, name) ? compat.menus[name] : undefined;
      if (!cm) { broken.push(`メニューが無い: ${name}`); continue; }
      if (cm.acceptReporters !== fm.acceptReporters) broken.push(`メニューの形（ブロックを入れられるか）が変わった: ${name}`);
      const now = new Set(cm.values);
      const lost = fm.values.filter(v => !now.has(v));
      if (lost.length) broken.push(`メニューの値が無い: ${name}（${lost.slice(0, 5).join(', ')}${lost.length > 5 ? ` ほか ${lost.length - 5} 件` : ''}）`);
    }
  }
  if (process.env.BLOCKS_UPDATE_FIXTURE === '1') {
    // 記録の作り直し: 前の記録から消えるもの（今までの作品が開けなくなるもの）を出してから書く。
    // ほかの試験が失敗しているときは書かない（こわれた状態を記録しないため）
    if (fixture) {
      console.log(`記録を作り直すと、前の記録から次のものが消えます（今までの作品が開けなくなります）: ${JSON.stringify(broken)}`);
      if (badFixture.length) console.log(`前の記録の中身の問題（作り直すと直ります）: ${JSON.stringify(badFixture)}`);
    } else {
      console.log(`前の作品の記録が${fixtureReadError ? '読めない' : '無い'}ので、全部が新しく入ります`);
    }
    // これから書く記録も、ふつうに流したときの確かめに通る形にする（通らない記録を書かない）
    const badNew = fixtureProblems(compat);
    if (!IS_REPO_GUI) {
      ok(false, `リポジトリの gui.js ではなく ${GUI_JS} を試しているので、今までの作品を守る記録は作り直しません（BLOCKS_GUI_JS を外して流す）`);
    } else if (badNew.length) {
      ok(false, '作り直す作品の記録の中身がそろっていないので、作り直しません', badNew);
    } else {
      // 書くのは最後（ページのエラーまで、ぜんぶの確かめが通ったときだけ）
      pendingWrites.push({ file: FIXTURE, data: compat, name: '今までの作品を守る記録', note: '前の記録から消えるものは、上に出しています' });
    }
  } else {
    ok(badFixture.length === 0, '今までの作品を守る記録（tests/fixtures/minecraft-blocks-compat.json）が読めて、中身がそろっている', badFixture);
    ok(!!fixture && broken.length === 0,
      '今までの作品が開ける: 記録（tests/fixtures/minecraft-blocks-compat.json）にあるブロック・入力・メニューの値が、今もすべて同じ形である', broken);
  }
  // Minecraft の文の記録を作り直す（ことばを決めて変えたときだけ）。書くのは最後
  if (process.env.BLOCKS_UPDATE_TEXTS === '1') {
    const counts = { ja: Object.keys(textSnap.ja || {}).length, 'ja-Hira': Object.keys(textSnap['ja-Hira'] || {}).length };
    ok(counts.ja > 100 && counts['ja-Hira'] > 100, '（作り直し）漢字・ひらがなの文を集められた', counts);
    if (!IS_REPO_GUI) {
      ok(false, `リポジトリの gui.js ではなく ${GUI_JS} を試しているので、文の記録は作り直しません（BLOCKS_GUI_JS を外して流す）`);
    } else {
      pendingWrites.push({ file: TEXTS_FILE, data: textSnap, name: '文の記録', note: '今回は記録と比べていません。変わる所は、上に出しています' });
    }
  }
  // パレットが滑っている途中に言語を変えても、エラーにならない（scratch-blocks の滑る動きの守り）
  const errBefore = errors.length;
  await p.click('.scratchCategoryId-minecraft');
  // 押した直後、まだ滑っている途中であることを確かめてから、同じ処理の中で言語を変える
  const sliding = await p.evaluate(() => {
    const flyout = Blockly.getMainWorkspace().getFlyout();
    const moving = flyout.scrollTarget !== null && flyout.scrollTarget !== undefined;
    window.__store.dispatch({ type: 'scratch-gui/locales/SELECT_LOCALE', locale: 'ja-Hira' });
    return moving;
  });
  ok(sliding, '（準備）言語を変えたとき、パレットはまだ滑っている途中');
  await setLocale('ja-Hira', 'わたしのさくひん');
  await p.waitForTimeout(1000);
  ok(errors.length === errBefore, 'パレットが滑っている途中に言語を変えても、エラーにならない', errors.slice(errBefore));
  await setLocale('ja', 'わたしの作品');

  ok(errors.length === 0, 'ページのエラーなし', errors);
  // 記録の作り直し（BLOCKS_UPDATE_FIXTURE=1・BLOCKS_UPDATE_TEXTS=1）は、ぜんぶの確かめのあとで、失敗が 0 のときだけ書く
  // （こわれた状態を記録しないため）
  const failsBefore = fails;
  if (pendingWrites.length) {
    const names = pendingWrites.map(w => w.name).join('と');
    // 記録のファイルか、それを入れるフォルダがシンボリックリンクなら書かない（リンク先のファイルを上書きしないため）
    const linked = [...new Set(pendingWrites.flatMap(w => [w.file, dirname(w.file)]))].filter(path => {
      try {
        return lstatSync(path).isSymbolicLink();
      } catch (e) {
        return false; // まだ無いファイルはリンクではない
      }
    });
    if (failsBefore > 0) {
      ok(false, `ほかの試験が ${failsBefore} 件失敗しているので、${names}は作り直しません`);
    } else if (repoGuiStamp() !== GUI_STAMP) {
      ok(false, `試しているあいだに gui.js が変わったので、${names}は作り直しません（もう一度流す）`);
    } else if (linked.length) {
      ok(false, `記録のファイルかフォルダがシンボリックリンクなので、${names}は作り直しません（リンク先を上書きしないため）`, linked);
    } else {
      // まず全部を一時ファイル（同じフォルダ）に書いてから、名前をつけかえる。途中で失敗しても、片方の記録だけが新しくはならない。
      // 一時ファイルは、前の失敗で残っていたら上書きせずに止める（wx）
      const tmps = [];
      try {
        for (const w of pendingWrites) {
          const tmp = `${w.file}.tmp-${process.pid}`;
          writeFileSync(tmp, JSON.stringify(w.data, null, 1) + '\n', { flag: 'wx' });
          tmps.push(tmp);
        }
      } catch (e) {
        ok(false, `一時ファイルを書けなかったので、${names}は作り直しません（書けた一時ファイル: ${tmps.join(', ') || 'なし'}）`, String(e));
      }
      if (tmps.length === pendingWrites.length) {
        pendingWrites.forEach((w, i) => {
          renameSync(tmps[i], w.file);
          console.log(`${w.name}を作り直しました（${w.note}）: ${w.file}`);
        });
      }
    }
  }
} finally {
  await b.close();
  server.close();
}
console.log(fails === 0 ? '\nすべて OK' : `\n${fails} 件しっぱい`);
process.exit(fails === 0 ? 0 : 1);

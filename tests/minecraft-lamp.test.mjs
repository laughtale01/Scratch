// Minecraft とつながっているかのランプ（gui.js の MinecraftLamp と、Minecraft 拡張の接続の状態）の試験。
// 本物の Chrome を見えない形で動かし、リポジトリの index.html と gui.js を 127.0.0.1 の小さなサーバーから開く。
// Minecraft の MOD の代わりに、127.0.0.1 で偽物の WebSocket のサーバーを立て、［Minecraftに接続］ブロックを動かしてつなぐ。
// 本番にはつながない: 127.0.0.1 以外への通信と firebase-integration.js は空の中身に差し替え、
//   偽物の MOD 以外へ WebSocket が開いたら試験をしっぱいにする。
// 使い方: node tests/minecraft-lamp.test.mjs
//   CHROME_PATH … Chrome の実行ファイルの場所（既定は Mac の /Applications/Google Chrome.app/Contents/MacOS/Google Chrome）。
//                 無ければ試験を飛ばし、終わりのコード 77 を返す
//   LAMP_GUI_JS … 試す gui.js（既定はリポジトリの gui.js）
import { chromium } from 'playwright-core';
import { createServer } from 'node:http';
import { createHash } from 'node:crypto';
import { existsSync, readFileSync, statSync } from 'node:fs';
import { extname, join, normalize, sep } from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = fileURLToPath(new URL('..', import.meta.url));
const GUI_JS = process.env.LAMP_GUI_JS || join(ROOT, 'gui.js');
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

/**
 * 偽物の MOD（WebSocket のつなぎ始めの手順だけをして、届いた中身は読まない）。
 * hold が true なら、つなぎ始めの返事（101）を release() まで待たせる（「つないでいます…」を作るため）
 */
const fakeMod = async ({ hold = false } = {}) => {
  const sockets = new Set();
  const pending = [];
  const answer = (req, sock) => {
    const accept = createHash('sha1').update(req.headers['sec-websocket-key'] + '258EAFA5-E914-47DA-95CA-C5AB0DC85B11').digest('base64');
    sock.write('HTTP/1.1 101 Switching Protocols\r\nUpgrade: websocket\r\nConnection: Upgrade\r\n' +
      `Sec-WebSocket-Accept: ${accept}\r\n\r\n`);
  };
  const srv = createServer((req, res) => { res.writeHead(404); res.end(); });
  srv.on('upgrade', (req, sock) => {
    sockets.add(sock);
    sock.on('close', () => sockets.delete(sock));
    sock.on('error', () => {});
    sock.on('data', () => {});
    if (hold) pending.push(() => answer(req, sock)); else answer(req, sock);
  });
  await new Promise(r => srv.listen(0, '127.0.0.1', r));
  return {
    port: srv.address().port,
    release: () => { pending.splice(0).forEach(f => { try { f(); } catch (e) { /* もう閉じていれば何もしない */ } }); },
    drop: () => { for (const s of sockets) s.destroy(); pending.length = 0; }, // 切った接続は待っている数からも外す
    pendingCount: () => pending.length,
    sendClose: () => { for (const s of sockets) s.write(Buffer.from([0x88, 0x00])); },
    close: () => new Promise(r => { for (const s of sockets) s.destroy(); srv.close(() => r()); })
  };
};
const MOD = await fakeMod();
const SLOW = await fakeMod({ hold: true });
const MOD2 = await fakeMod();
const MOD3 = await fakeMod();
// つながらない番号（いったん開いて閉じた番号を使う）
const dead = createServer();
await new Promise(r => dead.listen(0, '127.0.0.1', r));
const DEAD_PORT = dead.address().port;
await new Promise(r => dead.close(r));
const OK_PORTS = new Set([MOD.port, SLOW.port, MOD2.port, MOD3.port, DEAD_PORT].map(String));

let fails = 0;
const ok = (cond, label, extra) => {
  console.log(`${cond ? 'OK  ' : 'FAIL'} ${label}${extra !== undefined ? ' ' + JSON.stringify(extra) : ''}`);
  if (!cond) fails++;
};
// ランプの丸の色（getComputedStyle の値）
const COLORS = { idle: 'rgb(154, 160, 166)', connecting: 'rgb(242, 183, 5)', connected: 'rgb(47, 179, 68)', failed: 'rgb(229, 72, 77)' };

{
  const b = await chromium.launch({ executablePath: CHROME, headless: true,
    args: ['--use-angle=swiftshader', '--enable-unsafe-swiftshader', '--ignore-gpu-blocklist'] });
  const ctx = await b.newContext({ viewport: { width: 1440, height: 900 }, locale: 'ja-JP' });
  await ctx.route('**/*', route => {
    const u = new URL(route.request().url());
    if (u.origin !== ORIGIN || u.pathname === '/firebase-integration.js') {
      return route.fulfill({ status: 200, contentType: 'text/javascript', body: '' });
    }
    return route.continue();
  });
  const errors = [];
  // 偽物の MOD 以外へ WebSocket が開いたら記録する（route では止められないため。別の窓も見張る）
  ctx.on('page', pg => pg.on('websocket', ws => {
    let u = null;
    try { u = new URL(ws.url()); } catch (e) { u = null; }
    if (!u || u.protocol !== 'ws:' || u.hostname !== '127.0.0.1' || u.username || u.password || !OK_PORTS.has(u.port)) {
      errors.push(`偽物の MOD の外へ WebSocket が開いた: ${ws.url()}`);
    }
  }));
  const p = await ctx.newPage();
  p.on('pageerror', e => errors.push(e.message));
  // ランプの部品についての React の警告（propTypes・アンマウント後の setState）も拾う
  p.on('console', m => {
    const t = m.text();
    if (m.type() === 'error' && /Warning: (Failed prop type|Can't perform a React state update)/.test(t) && /MinecraftLamp/.test(t)) errors.push(t);
  });
  await p.goto(`${ORIGIN}/`, { waitUntil: 'networkidle', timeout: 90000 });
  await p.waitForTimeout(2000);

  const lamp = () => p.evaluate(() => {
    const el = document.querySelector('.lt-mc-lamp');
    if (!el) return null;
    const hint = el.querySelector('.lt-mc-lamp__hint');
    const btn = el.querySelector('.lt-mc-lamp__button');
    return { status: el.getAttribute('data-lt-mc-status'), blink: el.classList.contains('lt-mc-lamp--blink'),
      text: el.querySelector('.lt-mc-lamp__text')?.textContent ?? btn.textContent, label: btn.getAttribute('aria-label'),
      live: el.querySelector('.lt-mc-lamp__live')?.textContent ?? null,
      color: getComputedStyle(el.querySelector('.lt-mc-lamp__dot')).backgroundColor,
      dotW: Math.round(el.querySelector('.lt-mc-lamp__dot').getBoundingClientRect().width),
      animation: getComputedStyle(btn).animationName,
      hint: hint ? hint.textContent : null, hintLines: hint ? hint.querySelectorAll('p').length : 0,
      expanded: btn.getAttribute('aria-expanded'), controls: btn.getAttribute('aria-controls'),
      title: btn.getAttribute('title'),
      liveRole: el.querySelector('.lt-mc-lamp__live')?.getAttribute('role') ?? null,
      liveInButton: !!btn.querySelector('.lt-mc-lamp__live'),
      hintBox: hint ? (r => ({ left: Math.round(r.left), right: Math.round(r.right), viewW: window.innerWidth }))(hint.getBoundingClientRect()) : null };
  });
  // ヒントが画面の中に収まっているか
  const hintInView = l => !!(l && l.hintBox && l.hintBox.left >= 0 && l.hintBox.right <= l.hintBox.viewW);
  // 待ったうえで、なれなかったら試験のしっぱいとして記録する（あとの項目が別の理由でしっぱいしないように）
  const must = async (status, label) => ok(await waitStatus(status), label || `（準備）${status} になる`);
  const waitStatus = async (status, timeout = 8000) => {
    try {
      await p.waitForFunction(s => { const el = document.querySelector('.lt-mc-lamp'); return el && el.getAttribute('data-lt-mc-status') === s; }, status, { timeout });
      return true;
    } catch (e) {
      if (e && e.name === 'TimeoutError') return false;
      throw e;
    }
  };
  const openHint = async () => {
    await p.click('.lt-mc-lamp__button', { timeout: 3000 }).catch(() => {});
    return !!(await lamp() || {}).hint;
  };
  /**
   * ［Minecraftに接続］・［切断］のブロックを置いて動かす（子どもがブロックを押したのと同じ）。
   * forever が true なら「ずっと」の中に入れる
   */
  const runBlock = (opcode, port, { host = '127.0.0.1', forever = false } = {}) => p.evaluate(({ opcode, port, host, forever }) => {
    const id = 'lt' + Math.random().toString(36).slice(2);
    const blocks = [{ id, opcode, next: null, parent: null, inputs: {}, fields: {}, shadow: false, topLevel: true, x: 40, y: 40 }];
    if (opcode === 'minecraft_connect') {
      blocks[0].inputs = { HOST: { name: 'HOST', block: id + 'h', shadow: id + 'h' }, PORT: { name: 'PORT', block: id + 'p', shadow: id + 'p' } };
      blocks.push({ id: id + 'h', opcode: 'text', next: null, parent: id, inputs: {}, fields: { TEXT: { name: 'TEXT', value: host } }, shadow: true, topLevel: false });
      blocks.push({ id: id + 'p', opcode: 'math_number', next: null, parent: id, inputs: {}, fields: { NUM: { name: 'NUM', value: String(port) } }, shadow: true, topLevel: false });
    }
    let top = opcode;
    if (forever) {
      blocks[0].parent = id + 'f'; blocks[0].topLevel = false;
      blocks.unshift({ id: id + 'f', opcode: 'control_forever', next: null, parent: null, inputs: { SUBSTACK: { name: 'SUBSTACK', block: id, shadow: null } }, fields: {}, shadow: false, topLevel: true, x: 40, y: 40 });
      top = 'control_forever';
    }
    // shareBlocksToTarget はブロックの ID を付け直すので、置く前になかった、いちばん上のブロックを探す
    const before = new Set(Object.keys(vm.editingTarget.blocks._blocks));
    return vm.shareBlocksToTarget(blocks, vm.editingTarget.id).then(() => {
      const all = vm.editingTarget.blocks._blocks;
      vm.emitWorkspaceUpdate(); // コードエリアに出す
      return Object.keys(all).find(k => !before.has(k) && all[k].topLevel && all[k].opcode === top);
    });
  }, { opcode, port, host, forever }).then(async topId => {
    // 画面にブロックが出てから動かす（出る前に動かすと、光らせる所が無くて Blockly がエラーを出す）
    await p.waitForFunction(i => !!document.querySelector(`[data-id="${i}"]`), topId, { timeout: 5000 });
    await p.evaluate(i => vm.runtime.toggleScript(i, { target: vm.editingTarget }), topId);
  });
  // 状態の知らせを数える
  const countEvents = () => p.evaluate(() => {
    window.__ltEvents = [];
    if (!window.__ltCounting) {
      window.__ltCounting = true;
      vm.runtime.on('LT_MINECRAFT_CONNECTION', s => window.__ltEvents.push(s));
    }
  });
  const events = () => p.evaluate(() => window.__ltEvents.slice());
  // ページの中で作られた WebSocket を数える（つながらずに断られたものは Playwright の知らせに出ないため）
  const countTries = () => p.evaluate(() => {
    if (!window.__ltWsWrapped) {
      window.__ltWsWrapped = true;
      const W = window.WebSocket;
      window.WebSocket = class extends W {
        constructor(...a) { super(...a); window.__ltWsUrls.push(String(a[0])); this.addEventListener('close', () => window.__ltWsClosed.push(performance.now())); }
      };
    }
    window.__ltWsUrls = [];
    window.__ltWsClosed = [];
  });
  const tries = port => p.evaluate(q => window.__ltWsUrls.filter(u => u.includes(`:${q}/`)).length, port);

  // 1. Minecraft の拡張を入れるまでは出さない
  ok(await lamp() === null, 'Minecraft の拡張を入れていないときは、ランプを出さない');

  // 2. 拡張を入れると、灰色「つないでいない」
  await p.evaluate(() => vm.extensionManager.loadExtensionURL('minecraft'));
  await must('idle');
  let l = await lamp();
  ok(l && l.status === 'idle' && l.text.includes('つないでいない') && l.color === COLORS.idle && l.dotW >= 10 && !l.blink && l.animation === 'none',
     '拡張を入れると、灰色の「つないでいない」が出る', l);
  ok(l && l.label === null && l.live === 'Minecraft つないでいない' && l.liveRole === 'status' && !l.liveInButton,
     'ボタンに aria-label を付けず（名前は title から）、読み上げの文はボタンの外（role=status）', l);
  ok(l && l.title === 'Minecraft つないでいない（押すとヒント）', 'マウスを乗せたときの説明にも状態が入る', l);

  // 3. つないでいる途中（MOD が返事を待たせている）は黄。ヒントも途中のもの
  await runBlock('minecraft_connect', SLOW.port);
  ok(await waitStatus('connecting'), '返事を待っている間は、黄の「つないでいます…」');
  l = await lamp();
  ok(l && l.color === COLORS.connecting && l.text.includes('つないでいます') && l.live === 'Minecraft つないでいます…', '黄の色と文（読み上げの文も変わる）', l);
  ok(await openHint(), 'ヒントが開く（Esc の確かめの前に）');
  l = await lamp();
  ok(l && l.hint && l.hint.includes('少し待って') && l.expanded === 'true' && l.controls === 'lt-mc-lamp-hint', 'つないでいる途中のヒントと、ボタンとヒントの結び付け', l);
  await p.keyboard.press('Escape');
  ok((await lamp() || {}).hint === null, 'Esc でヒントが閉じる');
  ok(await openHint(), 'もう一度ヒントを開く');
  await p.evaluate(() => document.querySelector('[class*="gui_extension-button_"]').focus());
  await p.waitForTimeout(200);
  ok((await lamp() || {}).hint === null, 'フォーカスがランプの外へ移ると、ヒントが閉じる（Esc がほかの画面に届くように）');
  ok(await openHint(), 'ヒントを開く（中を押す確かめ）');
  await p.click('.lt-mc-lamp__hint p');
  await p.waitForTimeout(200);
  ok((await lamp() || {}).hint !== null, 'ヒントの文を押しても、ヒントは閉じない');
  const escOut = await p.evaluate(() => {
    let got = false; const h = e => { if (e.key === 'Escape') got = true; };
    document.addEventListener('keydown', h);
    document.querySelector('[class*="gui_extension-button_"]').dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
    document.removeEventListener('keydown', h);
    return got;
  });
  await p.waitForTimeout(200);
  ok(escOut && (await lamp() || {}).hint === null, 'ランプの外から来た Esc は、ヒントを閉じつつ、ほかの画面にも届く', escOut);

  // 4. つないでいる途中に［切断］すると灰にもどり、あとで返事が来ても灰のまま
  await runBlock('minecraft_disconnect');
  ok(await waitStatus('idle'), 'つないでいる途中に［切断］すると、灰にもどる');
  SLOW.release();
  await p.waitForTimeout(800);
  ok((await lamp() || {}).status === 'idle', '［切断］したあとに前の返事が来ても、灰のまま');

  // 5. つないでいる途中に別の場所（MOD）へつなぐと緑になり、前の接続が閉じても緑のまま
  await runBlock('minecraft_connect', SLOW.port);
  ok(await waitStatus('connecting'), 'もう一度、返事を待っている');
  await runBlock('minecraft_connect', MOD.port);
  ok(await waitStatus('connected'), '別の場所へつなぐと、緑になる');
  l = await lamp();
  ok(l && l.color === COLORS.connected && l.text.includes('つながっている') && !l.blink, '緑の色と文', l);
  SLOW.release(); SLOW.drop();
  await p.waitForTimeout(800);
  ok((await lamp() || {}).status === 'connected', '前の接続（返事を待たせていた所）が閉じても、緑のまま');
  ok(await openHint(), '緑のときもヒントが開く');
  l = await lamp();
  ok(l && l.hint && l.hint.startsWith('Minecraft つながっている') && l.hint.includes('Minecraft に届きます'), '緑のときのヒント（いちばん上に今の状態）', l);
  ok(await p.evaluate(() => { const ps = document.querySelectorAll('.lt-mc-lamp__hint p'); return ps[0].getAttribute('aria-hidden') === 'true' && ps[1].getAttribute('aria-hidden') === null; }), 'ヒントの先頭の状態の行は読み上げから外し、説明の行は読み上げる');
  await p.click('.lt-mc-lamp__button', { timeout: 3000 }).catch(() => {});
  ok((await lamp() || {}).hint === null, 'もう一度押すと、ヒントが閉じる');

  // 6. つながっていたのに切れる（MOD を閉じた）と、赤になって点滅し、数秒で点滅が止まる
  MOD.drop();
  ok(await waitStatus('failed'), 'つながっていたのに切れると、赤になる');
  l = await lamp();
  ok(l && l.blink && l.animation === 'lt-mc-lamp-blink' && l.color === COLORS.failed && l.text.includes('つながらない'), '切れたときは赤で点滅する', l);
  await countEvents();
  await runBlock('minecraft_connect', MOD.port); // 同じ場所へ、すぐ（2 秒以内）につなぎ直す
  ok(await waitStatus('connected'), '切れたすぐあとに同じ場所へつなぎ直せたら、緑になる');
  l = await lamp();
  let ev0 = await events();
  ok(l && !l.blink && l.animation === 'none' && ev0.join() === 'connected', '黄を通らずに緑になり、点滅も止まる', { l, ev0 });
  MOD.drop();
  await must('failed');
  ok(await p.waitForFunction(() => !document.querySelector('.lt-mc-lamp--blink'), null, { timeout: 7000 }).then(() => true, () => false), '点滅は数秒で止まる');
  l = await lamp();
  ok(l && l.animation === 'none', '止まったあとはアニメーションも無い', l);

  // 7. 初めからつながらない所へは、赤になるだけ（点滅しない）。ヒントは 3 つの手順。外を押すと閉じる
  await runBlock('minecraft_disconnect');
  await must('idle');
  await runBlock('minecraft_connect', DEAD_PORT);
  ok(await waitStatus('failed'), 'つながらない所へつなぐと、赤になる');
  l = await lamp();
  ok(l && !l.blink, '初めからつながらないときは、点滅しない', l);
  ok(await openHint(), '赤のときもヒントが開く');
  l = await lamp();
  ok(l && l.hintLines === 4 && l.hint.includes('Minecraft を起動する') && l.hint.includes('ワールドに入る') && l.hint.includes('緑の旗'),
     '赤のときは、つながらないときの手順が出る', l);
  await p.mouse.click(600, 500);
  ok((await lamp() || {}).hint === null, 'ランプの外を押すと、ヒントが閉じる');
  await runBlock('minecraft_connect', DEAD_PORT); // 赤のまま、同じ場所へもう一度
  await runBlock('minecraft_connect', SLOW.port); // すぐ（2 秒以内）に、別の場所（返事を待たせる）へ
  ok(await waitStatus('connecting'), '赤のすぐあとでも、別の場所へつなぐときは黄になる');
  await runBlock('minecraft_disconnect');
  SLOW.release(); SLOW.drop();
  await must('idle');
  await runBlock('minecraft_connect', SLOW.port);
  await must('connecting');
  for (let i = 0; i < 100 && SLOW.pendingCount() === 0; i++) await p.waitForTimeout(20); // 偽物の MOD に届くのを待つ
  SLOW.drop(); // 返事の前に切る（この場所で赤になる）
  await must('failed');
  await countTries();
  await runBlock('minecraft_connect', SLOW.port); // 同じ場所へすぐ
  await p.waitForTimeout(300);
  ok((await lamp() || {}).status === 'failed', '同じ場所へすぐ（2 秒以内）つなぎ直すときは、赤のまま');
  for (let i = 0; i < 100 && SLOW.pendingCount() === 0; i++) await p.waitForTimeout(20);
  SLOW.drop(); // 2 回目の失敗（拡張はここから 2 秒を数える）
  ok(await p.waitForFunction(() => window.__ltWsClosed.length > 0, null, { timeout: 5000 }).then(() => true, () => false), '（準備）2 回目の失敗がページに届く');
  await p.waitForTimeout(2300); // 届いてから 2 秒を余裕をもって過ぎるまで待つ
  await runBlock('minecraft_connect', SLOW.port); // 2 秒を過ぎてから、同じ場所へ
  ok(await waitStatus('connecting'), '同じ場所でも、2 秒を過ぎてからつなぎ直すときは黄になる');
  await runBlock('minecraft_disconnect');
  SLOW.release(); SLOW.drop();

  // 8. アドレスが壊れている（つなぐ前にエラーになる）ときも赤になる
  await runBlock('minecraft_disconnect');
  await must('idle');
  await runBlock('minecraft_connect', 1, { host: '[' });
  ok(await waitStatus('failed'), 'アドレスが壊れているときも、赤になる');

  // 9. 「ずっと」の中の［接続］が、つながらない所へつなぎ続けても、黄と赤のあいだでちらつかない
  await runBlock('minecraft_disconnect');
  await must('idle');
  await p.waitForTimeout(300);
  await countEvents();
  await countTries();
  await runBlock('minecraft_connect', DEAD_PORT, { forever: true });
  await p.waitForTimeout(2500);
  let ev = await events();
  const tried = await tries(DEAD_PORT);
  await p.evaluate(() => vm.stopAll());
  ok(tried >= 5 && ev.join() === 'connecting,failed', '「ずっと」の中で何度つなぎ直しても、知らせは「黄・赤」の 2 回だけ（ちらつかない）', { tried, ev });

  // 10. 「ずっと」の中でつながっていて切れたとき、つなぎ直しの間も点滅が続く
  await runBlock('minecraft_disconnect');
  await must('idle');
  await runBlock('minecraft_connect', MOD2.port, { forever: true });
  ok(await waitStatus('connected'), '「ずっと」の中でつながる');
  await countEvents();
  await countTries();
  await MOD2.close(); // MOD を止める（ワールドを閉じた）
  ok(await waitStatus('failed'), '止めると赤になる');
  await p.waitForTimeout(1500);
  l = await lamp();
  ev = await events();
  const tries2 = await tries(MOD2.port);
  await p.evaluate(() => vm.stopAll());
  ok(l && l.status === 'failed' && l.blink && tries2 >= 5 && ev.join() === 'failed', 'つなぎ直しが何度も起きている間も、赤のまま点滅が続く（黄にならない）', { l, ev, tries2 });

  // 10b. MOD が閉じるあいさつを送り、接続が閉じかけの間に「ずっと」の［接続］がつなぎ直しても、まず「切れた」が知らされる
  await runBlock('minecraft_disconnect');
  await must('idle');
  await runBlock('minecraft_connect', MOD3.port, { forever: true });
  ok(await waitStatus('connected'), '「ずっと」の中で MOD3 につながる');
  await countEvents();
  MOD3.sendClose();
  await p.waitForTimeout(1500);
  ev = await events();
  await p.evaluate(() => vm.stopAll());
  ok(ev[0] === 'failed' && !ev.includes('connecting'), '閉じかけの間につなぎ直しても、まず「切れた」が知らされる（黄にはならない）', ev);
  await runBlock('minecraft_disconnect');
  await MOD3.close();

  // 11. 言語を変えても（画面が作り直されても）ランプと状態が残り、文が変わる。聞き手は増えない
  await runBlock('minecraft_disconnect');
  await must('idle');
  await runBlock('minecraft_connect', MOD.port);
  await must('connected');
  const listeners0 = await p.evaluate(() => ({ lamp: vm.runtime.listenerCount('LT_MINECRAFT_CONNECTION'), ext: vm.runtime.listenerCount('EXTENSION_ADDED') }));
  await p.click('text=設定'); await p.waitForTimeout(400); await p.click('text=言語'); await p.waitForTimeout(400);
  await p.click('text=English'); await p.waitForTimeout(2500);
  l = await lamp();
  ok(l && l.status === 'connected' && l.text.includes('Connected'), '英語にしても、緑のまま英語の文になる', l);
  await p.click('text=Settings'); await p.waitForTimeout(400); await p.click('text=Language'); await p.waitForTimeout(400);
  await p.click('text=にほんご'); await p.waitForTimeout(2500);
  l = await lamp();
  ok(l && l.status === 'connected' && l.text.includes('つながっている'), 'ひらがなにしても、緑のまま', l);
  const listeners1 = await p.evaluate(() => ({ lamp: vm.runtime.listenerCount('LT_MINECRAFT_CONNECTION'), ext: vm.runtime.listenerCount('EXTENSION_ADDED') }));
  ok(listeners1.lamp === listeners0.lamp && listeners1.ext === listeners0.ext, '言語を 2 回変えても、聞き手は増えない', { listeners0, listeners1 });
  MOD.drop();
  await must('failed');
  const untilBefore = await p.evaluate(() => {
    window.__blinkEnd = null;
    const timer = setInterval(() => { const el = document.querySelector('.lt-mc-lamp'); if (el && !el.classList.contains('lt-mc-lamp--blink')) { window.__blinkEnd = performance.now(); clearInterval(timer); } }, 20);
    return vm.runtime.ltMinecraftBlinkUntil;
  });
  // 点滅の途中で言語を変えても（画面が作り直されても）、点滅が続く
  await p.click('text=/せってい|設定|Settings/'); await p.waitForTimeout(300); await p.click('text=/げんご|言語|Language/'); await p.waitForTimeout(300);
  await p.click('text=日本語'); await p.waitForSelector('.lt-mc-lamp', { timeout: 5000 }); await p.waitForTimeout(500);
  l = await lamp();
  ok(l && l.blink && l.animation === 'lt-mc-lamp-blink', '点滅の途中で言語を変えても、点滅が続く', l);
  const blinkGone = await p.waitForFunction(() => window.__blinkEnd !== null, null, { timeout: 7000, polling: 50 }).then(() => true, () => false);
  const blinkEnd = await p.evaluate(u => ({ before: Math.round(u), after: Math.round(vm.runtime.ltMinecraftBlinkUntil), end: Math.round(window.__blinkEnd || 0) }), untilBefore);
  ok(blinkGone && blinkEnd.after === blinkEnd.before && Math.abs(blinkEnd.end - blinkEnd.before) <= 400, '作り直したあとの点滅は、切れたときに決めた時刻で終わる（4 秒を数え直さない）', blinkEnd);
  await p.click('text=/せってい|設定|Settings/'); await p.waitForTimeout(300); await p.click('text=/げんご|言語|Language/'); await p.waitForTimeout(300);
  await p.click('text=にほんご'); await p.waitForSelector('.lt-mc-lamp', { timeout: 5000 }); await p.waitForTimeout(500);
  ok(await openHint(), 'ひらがなでもヒントが開く');
  l = await lamp();
  // ヒントの中のブロックの名前は、ひらがな版のブロックの文と同じ（すきまを入れない書き方）
  ok(l && l.hint && l.hint.includes('ワールドにはいる') && l.hint.includes('［Minecraftにつなぐ］'), 'ひらがなのヒント', l);
  await p.keyboard.press('Escape');

  // 12. 全画面のときも、ランプが出る
  await p.click('[class*="stage-header_stage-size-row"] > div:last-child [class*="stage-header_stage-button"]', { timeout: 5000 });
  await p.waitForTimeout(1200);
  const overlay = () => p.evaluate(() => !!document.querySelector('[class*="stage-header_stage-header-wrapper-overlay"]'));
  ok(await overlay() && (await lamp()) !== null, '全画面のときもランプが出る');
  // フォーカスが body のまま開いた（押してもフォーカスが入らないブラウザと同じ）ときの Esc も、ヒントだけを閉じる
  await p.evaluate(() => { document.activeElement && document.activeElement.blur(); document.querySelector('.lt-mc-lamp__button').click(); });
  const bodyOpen = await p.evaluate(() => document.activeElement === document.body && !!document.querySelector('.lt-mc-lamp__hint'));
  await p.keyboard.press('Escape');
  await p.waitForTimeout(400);
  ok(bodyOpen && (await lamp() || {}).hint === null && await overlay(), 'フォーカスが body のときの Esc も、ヒントだけ閉じて全画面はそのまま', bodyOpen);
  ok(await openHint() && hintInView(await lamp()), '全画面でもヒントが開いて、画面に収まる', await lamp());
  await p.keyboard.press('Escape');
  await p.waitForTimeout(400);
  ok((await lamp() || {}).hint === null && await overlay(), '全画面でヒントを開いて Esc を押すと、ヒントだけ閉じて全画面はそのまま');
  await p.keyboard.press('Escape');
  await p.waitForTimeout(1200);
  ok(!(await overlay()), 'ヒントが閉じているときの Esc では、いつもどおり全画面を出る');

  // 13. 小さいステージでは丸だけになり、ステージの列を広げない
  await p.setViewportSize({ width: 1024, height: 640 });
  await p.waitForTimeout(800);
  const toggles = await p.$$('[class*="stage-header_stage-size-toggle-group"] [class*="toggle-buttons_button"]');
  await toggles[0].click();
  await p.waitForTimeout(1000);
  const small = await p.evaluate(() => {
    const R = s => document.querySelector(s).getBoundingClientRect();
    const t = document.querySelector('.lt-mc-lamp__text') || document.querySelector('.lt-mc-lamp__button');
    return { pressed: document.querySelectorAll('[class*="stage-header_stage-size-toggle-group"] [class*="toggle-buttons_button"]')[0].getAttribute('aria-pressed'),
      stageW: Math.round(R('[class*="stage_stage_"]').width), columnW: Math.round(R('[class*="gui_stage-and-target-wrapper"]').width),
      textShown: getComputedStyle(t).display !== 'none', label: document.querySelector('.lt-mc-lamp__button').getAttribute('title'),
      lampRight: Math.round(R('.lt-mc-lamp').right), sizeLeft: Math.round(R('[class*="stage-header_stage-size-row"]').left) };
  });
  ok(small.pressed === 'true' && small.stageW <= 250 && small.columnW <= small.stageW + 24 && !small.textShown && small.label && small.lampRight <= small.sizeLeft,
     '小さいステージでは丸だけになり、ステージの列を広げない', small);
  ok(await openHint(), '小さいステージでもヒントが開く');
  l = await lamp();
  ok(hintInView(l) && l.hintLines === 4, '小さいステージでも、ヒントが画面の中に収まる', l);
  ok(l && /^Minecraft つながらない（(押|お)すとヒント）$/.test(l.title), '丸だけのときも、マウスを乗せると状態が分かる', l);
  await p.keyboard.press('Escape');

  // 14. ふつうのステージ（1024px の画面）でターボモードの表示が増えても、はみ出さない
  await toggles[1].click();
  await p.waitForTimeout(800);
  await p.evaluate(() => vm.setTurboMode(true));
  await p.waitForTimeout(800);
  const turbo = await p.evaluate(() => {
    const R = s => document.querySelector(s).getBoundingClientRect();
    return { lampLeft: Math.round(R('.lt-mc-lamp').left), lampRight: Math.round(R('.lt-mc-lamp').right), controlsRight: Math.round(R('[class*="controls_controls-container"]').right),
      sizeLeft: Math.round(R('[class*="stage-header_stage-size-row"]').left), headerRight: Math.round(R('[class*="stage-header_stage-menu-wrapper"]').right),
      columnRight: Math.round(R('[class*="gui_stage-and-target-wrapper"]').right), pageW: document.documentElement.scrollWidth, viewW: window.innerWidth };
  });
  // わざと狭くして、文字が「…」で縮まり、ボタンが大きさボタンにかからないことを確かめる
  const squeezed = await p.evaluate(() => {
    const st = document.createElement('style'); st.id = 'lt-test-squeeze'; st.textContent = '.lt-mc-lamp{max-width:90px;}';
    document.head.appendChild(st);
    const t = document.querySelector('.lt-mc-lamp__text');
    const r = { cut: t.scrollWidth > t.clientWidth, overflow: getComputedStyle(t).textOverflow,
      btnRight: Math.round(document.querySelector('.lt-mc-lamp__button').getBoundingClientRect().right),
      sizeLeft: Math.round(document.querySelector('[class*="stage-header_stage-size-row"]').getBoundingClientRect().left) };
    st.remove();
    return r;
  });
  ok(squeezed.cut && squeezed.overflow === 'ellipsis' && squeezed.btnRight <= squeezed.sizeLeft, '狭いときは、文字を「…」で縮めて、ほかのボタンにかからない', squeezed);
  ok(await openHint() && hintInView(await lamp()), 'ふつうのステージ（1024px）でも、ヒントが画面に収まる', await lamp());
  await p.keyboard.press('Escape');
  await p.evaluate(() => vm.setTurboMode(false));
  ok(turbo.lampLeft >= turbo.controlsRight && turbo.lampRight <= turbo.sizeLeft && turbo.headerRight <= turbo.columnRight && turbo.pageW <= turbo.viewW,
     'ターボモードの表示が増えても、ランプはステージの大きさボタンと重ならず、画面からはみ出さない', turbo);

  // 15. 右から左の言語（アラビア語）の小さいステージでも、ヒントが画面に収まる
  await p.setViewportSize({ width: 1440, height: 900 }); await p.waitForTimeout(800); // メニューの文字が出る広さにもどす
  await p.click('text=/せってい|設定|Settings/'); await p.waitForTimeout(300);
  await p.click('text=/げんご|言語|Language/'); await p.waitForTimeout(300);
  await p.click('text=العربية'); await p.waitForSelector('.lt-mc-lamp', { timeout: 5000 }); await p.waitForTimeout(800);
  const rtlToggles = await p.$$('[class*="stage-header_stage-size-toggle-group"] [class*="toggle-buttons_button"]');
  await rtlToggles[0].click(); await p.waitForTimeout(800);
  ok(await p.evaluate(() => document.querySelector('[dir="rtl"]') !== null), '（前提）アラビア語で右から左になっている');
  ok(await openHint() && hintInView(await lamp()), '右から左の言語の小さいステージでも、ヒントが画面に収まる', await lamp());
  await p.keyboard.press('Escape');

  ok(errors.length === 0, 'ページのエラーなし', errors);
  console.log(fails ? `\n${fails} 件しっぱい` : '\nすべて OK');
  await b.close();
  server.close();
  await MOD.close(); await SLOW.close();
  process.exit(fails ? 1 : 0);
}

// ブロックパレットの幅と大きさ（gui.js の PaletteLayout）の試験。
// 本物の Chrome を見えない形で動かし、リポジトリの index.html と gui.js を 127.0.0.1 の小さなサーバーから開く。
// 本番にはつながない: 127.0.0.1 以外への通信（Firebase の SDK など）と firebase-integration.js は空の中身に差し替える。
//   WebSocket はこの差し替えを通らない（playwright-core 1.47 の route は WebSocket を止めない）ので、
//   127.0.0.1 以外へ WebSocket が開いたら試験をしっぱいにする（今の手順では開かない）。
// 使い方: node tests/palette-layout.test.mjs
//   CHROME_PATH … Chrome の実行ファイルの場所（既定は Mac の /Applications/Google Chrome.app/Contents/MacOS/Google Chrome）。
//                 無ければ試験を飛ばし、終わりのコード 77 を返す
//   PALETTE_GUI_JS … 試す gui.js（既定はリポジトリの gui.js）。直す前の版でしっぱいするかを確かめるときに使う
import { chromium } from 'playwright-core';
import { createServer } from 'node:http';
import { existsSync, readFileSync, statSync } from 'node:fs';
import { extname, join, normalize, sep } from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = fileURLToPath(new URL('..', import.meta.url));
const GUI_JS = process.env.PALETTE_GUI_JS || join(ROOT, 'gui.js');
const CHROME = process.env.CHROME_PATH || '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome';
if (!existsSync(CHROME)) {
  console.log(`Chrome が見つからないので、この試験は飛ばします（${CHROME}。CHROME_PATH で指定できます）`);
  process.exit(77); // 77 は「飛ばした」（run-all.sh が成功・しっぱいと分けて出す）
}

// リポジトリの中のファイルだけを返す小さなサーバー
const TYPES = { '.html': 'text/html; charset=utf-8', '.js': 'text/javascript; charset=utf-8', '.css': 'text/css', '.json': 'application/json',
  '.svg': 'image/svg+xml', '.png': 'image/png', '.jpg': 'image/jpeg', '.gif': 'image/gif', '.ico': 'image/x-icon',
  '.wav': 'audio/wav', '.mp3': 'audio/mpeg', '.woff': 'font/woff', '.woff2': 'font/woff2', '.ttf': 'font/ttf' };
let HOST = ''; // 自分宛ての Host（127.0.0.1:番号）。待ち受けを始めてから決まる
const server = createServer((req, res) => {
  let path;
  try {
    path = decodeURIComponent(new URL(req.url, 'http://x').pathname);
  } catch (e) {
    res.writeHead(400); res.end(); return; // 壊れたアドレスでは落ちずに断る
  }
  // 自分宛てでない要求は断る（ほかのサイトが名前を付け替えて、このサーバーを読むのを防ぐ）
  if (req.headers.host !== HOST) { res.writeHead(403); res.end(); return; }
  // .env・.git など「.」で始まるものは返さない
  // （Windows では「\」も区切りになるので、両方で分ける）
  // （Windows の短い名前 GIT~1・ENV~1 などで「.」で始まるものを開かれないよう、「~」を含むものも断る）
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
const URL_ = `${ORIGIN}/`;

const KEY = 'laughtale.palette.v1';
let fails = 0;
const ok = (cond, label, extra) => {
  console.log(`${cond ? 'OK  ' : 'FAIL'} ${label}${extra !== undefined ? ' ' + JSON.stringify(extra) : ''}`);
  if (!cond) fails++;
};
const near = (a, b, d = 2) => Math.abs(a - b) <= d;

{
  const b = await chromium.launch({ executablePath: CHROME, headless: true,
    args: ['--use-angle=swiftshader', '--enable-unsafe-swiftshader', '--ignore-gpu-blocklist'] });
  const ctx = await b.newContext({ viewport: { width: 1440, height: 900 }, locale: 'ja-JP', hasTouch: true });
  // 本番にはつながない（127.0.0.1 以外と Firebase の連携のスクリプトは空にする）
  const offline = route => {
    const u = new URL(route.request().url());
    if (u.origin !== ORIGIN || u.pathname === '/firebase-integration.js') {
      return route.fulfill({ status: 200, contentType: 'text/javascript', body: '' });
    }
    return route.continue();
  };
  // 127.0.0.1 の外へ WebSocket が開いたら記録する（route では止められないため）
  const watchSockets = (page, list) => page.on('websocket', ws => {
    let u = null;
    try { u = new URL(ws.url()); } catch (e) { u = null; }
    if (!u || u.protocol !== 'ws:' || u.host !== HOST || u.username || u.password) list.push(`127.0.0.1 の外へ WebSocket が開いた: ${ws.url()}`);
  });
  await ctx.route('**/*', offline);
  const errors = [];
  ctx.on('page', pg => watchSockets(pg, errors)); // 別の窓が開いても見張る
  const p = await ctx.newPage();
  // 条件が成り立つまで待つ（時間を決め打ちにしない）。間に合わなければ false
  const waitUntil = async (fn, arg, timeout = 8000) => {
    try {
      await p.waitForFunction(fn, arg, { timeout });
      return true;
    } catch (e) {
      if (e && e.name === 'TimeoutError') return false;
      throw e;
    }
  };
  // パレットの倍率が「元の大きさ × ratio」になるまで待つ
  const waitScale = async ratio => {
    await grab();
    return waitUntil(r => Math.abs(window.__ws.getFlyout().workspace_.scale - 0.675 * r) < 1e-6, ratio);
  };
  p.on('pageerror', e => errors.push(e.message));
  p.on('console', m => { if (m.type() === 'warning' && m.text().includes('[palette]')) errors.push(m.text()); });
  p.on('console', m => { if (m.type() === 'error' && /not rendered|disposed/i.test(m.text())) errors.push(m.text()); });

  const open = async () => {
    await p.goto(URL_, { waitUntil: 'networkidle', timeout: 90000 });
    await p.waitForSelector('.lt-palette-handle', { timeout: 30000 });
    // 指で押したまま待つ試験が、Blockly の長押し（0.75 秒で右クリック扱いになり操作が終わる）とぶつからないようにする
    await grab();
    await p.evaluate(() => { window.__SB.LONGPRESS = 60000; });
    await p.waitForTimeout(1500);
  };
  // React の中から今の workspace を取り出す（試験だけで使う）
  const grab = () => p.evaluate(() => {
    const el = document.querySelector('[class*="blocks_blocks_"]');
    const key = Object.keys(el).find(k => k.startsWith('__reactFiber') || k.startsWith('__reactInternalInstance'));
    let f = el[key];
    for (let i = 0; f && i < 30; i++, f = f.return) {
      const s = f.stateNode;
      if (s && s.workspace && s.ScratchBlocks) { window.__ws = s.workspace; window.__SB = s.ScratchBlocks; return true; }
    }
    return false;
  });
  const st = async () => { await grab(); return p.evaluate(K => {
    const fl = __ws.getFlyout();
    const h = document.querySelector('.lt-palette-handle');
    const hr = h && h.getBoundingClientRect();
    const fr = document.querySelector('.blocklyFlyout').getBoundingClientRect();
    let saved = null; try { saved = localStorage.getItem(K); } catch (e) {}
    return { flyW: fl.getWidth(), tbW: __ws.toolbox_.width, absLeft: __ws.getMetrics().absoluteLeft,
      flyScale: +fl.workspace_.scale.toFixed(4), wsScale: __ws.scale,
      handleX: hr ? Math.round(hr.x) : null, handleW: hr ? Math.round(hr.width) : null,
      flyRight: Math.round(fr.x + fr.width), cat: __ws.toolbox_.getSelectedCategoryId(),
      scroll: +fl.getScrollPos().toFixed(1), saved };
  }, KEY); };
  const handleCenter = async () => { const r = await p.locator('.lt-palette-handle').boundingBox(); return { x: r.x + r.width / 2, y: r.y + r.height / 2 }; };
  const dragHandle = async dx => {
    const c = await handleCenter();
    await p.mouse.move(c.x, c.y); await p.mouse.down();
    for (let i = 1; i <= 10; i++) { await p.mouse.move(c.x + dx * i / 10, c.y); await p.waitForTimeout(20); }
    await p.mouse.up(); await p.waitForTimeout(400);
  };

  await open();
  await p.evaluate(K => { try { localStorage.removeItem(K); } catch (e) {} }, KEY);
  await open();
  let s = await st();
  ok(s.flyW === 250 && s.flyScale === 0.675 && !s.saved, '初めは幅 250・倍率 0.675・保存なし', s);
  ok(near(s.handleX + 2, s.flyRight, 3), 'つまみが境界線の上にある', { handleX: s.handleX, flyRight: s.flyRight });
  const labels = await p.$$eval('.lt-palette-size', els => els.map(e => e.textContent));
  ok(labels.length === 1 && labels[0].includes('ブロックの大きさ') && labels[0].includes('元に戻す'), 'タブの右にボタン（日本語）', labels);
  const tabsBox = await p.evaluate(() => {
    const tabs = [...document.querySelectorAll('[class*="gui_tab_"]')].map(t => t.getBoundingClientRect());
    const li = document.querySelector('.lt-palette-size').getBoundingClientRect();
    return { lastTabRight: Math.round(Math.max(...tabs.map(t => t.right))), liLeft: Math.round(li.left), liTop: Math.round(li.top), liBottom: Math.round(li.bottom), tabsTop: Math.round(Math.min(...tabs.map(t => t.top))) };
  });
  ok(tabsBox.liLeft >= tabsBox.lastTabRight && tabsBox.liLeft - tabsBox.lastTabRight < 30, 'ボタンは「音」タブのすぐ右', tabsBox);

  // --- マウスでドラッグ ---
  await dragHandle(200);
  s = await st();
  ok(near(s.flyW, 450, 3) && s.tbW === s.flyW + 60 && s.absLeft === s.tbW, 'ドラッグで幅 450・コードエリアが縮む', s);
  ok(s.saved && JSON.parse(s.saved).width === s.flyW, '幅を保存した', s.saved);
  ok(near(s.handleX + 2, s.flyRight, 3), 'つまみが新しい境界線についてくる', { handleX: s.handleX, flyRight: s.flyRight });

  // 上限（コードエリア 300 を残す）
  await dragHandle(2000);
  s = await st();
  const blocksW = await p.evaluate(() => document.querySelector('[class*="blocks_blocks_"]').offsetWidth);
  ok(s.flyW === blocksW - 60 - 300, '右へ引っ張りすぎても、コードエリアが 300 残る', { flyW: s.flyW, blocksW });
  // 下限 200
  await dragHandle(-2000);
  s = await st();
  ok(s.flyW === 200, '左へ引っ張りすぎても 200 で止まる', s.flyW);

  // --- 読み込み直し ---
  await dragHandle(150);
  const before = (await st()).flyW;
  await p.locator('.lt-palette-size [data-lt-palette="larger"]').click();
  await p.waitForTimeout(300);
  await open();
  s = await st();
  ok(s.flyW === before && s.flyScale === +(0.675 * 1.1).toFixed(4), '読み込み直しても幅と大きさが残る', { s, before });

  // --- 大きさボタン ---
  await p.locator('.lt-palette-size [data-lt-palette="larger"]').click();
  await waitScale(1.2);
  s = await st();
  ok(s.flyScale === +(0.675 * 1.2).toFixed(4) && s.wsScale === 0.675, '＋でパレットだけ大きくなる（コードエリアは 0.675 のまま）', s);
  for (let i = 0; i < 10; i++) { await p.locator('.lt-palette-size [data-lt-palette="larger"]').click({ force: true }); }
  await waitScale(1.5);
  s = await st();
  const dis = await p.$$eval('.lt-palette-size button', bs => bs.map(x => x.disabled));
  ok(s.flyScale === +(0.675 * 1.5).toFixed(4) && dis[1] === true && dis[0] === false, '150% で止まり、＋が押せなくなる', { s, dis });
  for (let i = 0; i < 12; i++) { await p.locator('.lt-palette-size [data-lt-palette="smaller"]').click({ force: true }); }
  await waitScale(0.6);
  s = await st();
  const dis2 = await p.$$eval('.lt-palette-size button', bs => bs.map(x => x.disabled));
  ok(s.flyScale === +(0.675 * 0.6).toFixed(4) && dis2[0] === true && dis2[1] === false, '60% で止まり、−が押せなくなる', { s, dis2 });

  // --- 大きさを変えても、見ていた所がずれない ---
  await p.evaluate(() => vm.extensionManager.loadExtensionURL('minecraft'));
  await p.waitForTimeout(1500);
  await p.click('.scratchCategoryId-sensing');
  await p.waitForTimeout(1200);
  // 「調べる」の途中（先頭から 150）までスクロールしておく
  await grab();
  await p.evaluate(() => { const tb = __ws.toolbox_; tb.setFlyoutScrollPos(tb.getCategoryPositionById('sensing') + 150); });
  await p.waitForTimeout(300);
  const offsetOf = () => p.evaluate(() => { const tb = __ws.toolbox_; return { cat: tb.getSelectedCategoryId(), offset: Math.round(tb.getCategoryScrollOffset()) }; });
  const a = await offsetOf();
  await p.locator('.lt-palette-size [data-lt-palette="larger"]').click();
  await waitScale(0.7);
  const c = await offsetOf();
  ok(Math.abs((await st()).flyScale - +(0.675 * 0.7).toFixed(4)) < 1e-6 && a.cat === 'sensing' && c.cat === 'sensing' && a.offset >= 140 && Math.abs(c.offset - a.offset) <= 2, '大きさを変えても「調べる」の途中の同じ所を見たまま', { a, c });

  // --- ダブルクリックで長いブロックに合わせる ---
  await p.click('.scratchCategoryId-minecraft');
  await p.waitForTimeout(1200);
  await p.locator('.lt-palette-size [data-lt-palette="reset"]').click();
  await p.waitForTimeout(400);
  s = await st();
  ok(s.flyW === 250 && s.flyScale === 0.675 && !s.saved, '「元に戻す」で幅 250・倍率 0.675・保存も消える', s);
  const hc = await handleCenter();
  await p.mouse.dblclick(hc.x, hc.y);
  await p.waitForTimeout(600);
  s = await st();
  const maxW = blocksW - 60 - 300;
  ok(s.flyW > 250 && s.flyW <= maxW, 'ダブルクリックで幅が広がる（上限まで）', { flyW: s.flyW, maxW });
  // 合わせた幅で、Minecraft のブロックの右はしが見えているか（上限で止まったときは除く）
  const fit = await p.evaluate(() => {
    const fl = __ws.getFlyout(); const fws = fl.workspace_; const tb = __ws.toolbox_;
    const cat = (fl.categoryScrollPositions || []).find(x => x.categoryId === tb.getSelectedCategoryId());
    let need = 0;
    fws.getTopBlocks(false).forEach(bl => { const xy = bl.getRelativeToSurfaceXY(); if (cat && (xy.y < cat.position - 1 || (cat.length && xy.y >= cat.position + cat.length))) return; need = Math.max(need, xy.x + bl.getHeightWidth().width); });
    return { needPx: Math.ceil(need * fws.scale), flyW: fl.getWidth() };
  });
  ok(fit.flyW >= Math.min(fit.needPx, maxW), '合わせた幅にいちばん長いブロックが入る（または上限）', fit);
  // 小さくしてから合わせると、上限より手前で止まる
  for (let i = 0; i < 4; i++) await p.locator('.lt-palette-size [data-lt-palette="smaller"]').click();
  await p.waitForTimeout(400);
  const hc2 = await handleCenter();
  await p.mouse.dblclick(hc2.x, hc2.y);
  await p.waitForTimeout(600);
  const fit2 = await p.evaluate(() => {
    const fl = __ws.getFlyout(); const fws = fl.workspace_; const tb = __ws.toolbox_;
    const cat = (fl.categoryScrollPositions || []).find(x => x.categoryId === tb.getSelectedCategoryId());
    let need = 0;
    fws.getTopBlocks(false).forEach(bl => { const xy = bl.getRelativeToSurfaceXY(); if (cat && (xy.y < cat.position - 1 || (cat.length && xy.y >= cat.position + cat.length))) return; need = Math.max(need, xy.x + bl.getHeightWidth().width); });
    return { needPx: Math.ceil(need * fws.scale), flyW: fl.getWidth(), scale: fws.scale };
  });
  ok(fit2.flyW >= fit2.needPx && fit2.flyW <= fit2.needPx + 30, '60% では長いブロックにぴったり合う', fit2);

  // --- 小さいパレットから出したブロックが、マウスの下に出る ---
  await p.click('.scratchCategoryId-motion');
  await p.waitForTimeout(1200);
  const src = await p.evaluate(() => {
    const fl = __ws.getFlyout();
    const bl = fl.workspace_.getTopBlocks(false).find(x => x.type === 'motion_movesteps');
    const r = bl.getSvgRoot().getBoundingClientRect();
    return { x: r.x + 8, y: r.y + r.height / 2 };
  });
  const countBefore = await p.evaluate(() => __ws.getTopBlocks(false).length);
  const drop = { x: 760, y: 420 };
  await p.mouse.move(src.x, src.y); await p.mouse.down();
  for (let i = 1; i <= 15; i++) { await p.mouse.move(src.x + (drop.x - src.x) * i / 15, src.y + (drop.y - src.y) * i / 15); await p.waitForTimeout(20); }
  await p.mouse.up(); await p.waitForTimeout(600);
  const placed = await p.evaluate(() => {
    const bl = __ws.getTopBlocks(false).find(x => x.type === 'motion_movesteps');
    if (!bl) return null;
    const r = bl.getSvgRoot().getBoundingClientRect();
    return { x: r.x, y: r.y, w: r.width, h: r.height, n: __ws.getTopBlocks(false).length };
  });
  ok(placed && placed.n === countBefore + 1 && drop.x >= placed.x - 2 && drop.x <= placed.x + 40 && drop.y >= placed.y - 4 && drop.y <= placed.y + placed.h + 4, '小さいパレットから出したブロックがマウスの下に出る', { placed, drop });

  // --- パレットに落とすと消える ---
  if (placed) {
    const from = { x: placed.x + 10, y: placed.y + placed.h / 2 };
    const to = { x: 150, y: 400 };
    await p.mouse.move(from.x, from.y); await p.mouse.down();
    for (let i = 1; i <= 15; i++) { await p.mouse.move(from.x + (to.x - from.x) * i / 15, from.y + (to.y - from.y) * i / 15); await p.waitForTimeout(20); }
    await p.mouse.up(); await p.waitForTimeout(600);
    const n = await p.evaluate(() => __ws.getTopBlocks(false).length);
    ok(n === countBefore, 'パレットに落としたブロックが消える', { n, countBefore });
  }

  // --- チェックボックスの位置 ---
  const cb = await p.evaluate(() => {
    const fl = __ws.getFlyout();
    const bl = fl.workspace_.getTopBlocks(false).find(x => x.type === 'motion_xposition');
    const box = fl.checkboxes_ && fl.checkboxes_[bl.id];
    if (!box) return null;
    const br = bl.getSvgRoot().getBoundingClientRect();
    const cr = box.svgRoot.getBoundingClientRect();
    return { blockMid: br.y + br.height / 2, boxMid: cr.y + cr.height / 2, boxRight: cr.right, blockLeft: br.left };
  });
  ok(cb && Math.abs(cb.blockMid - cb.boxMid) < 4 && cb.boxRight <= cb.blockLeft + 2, '「x座標」のチェックボックスがブロックの横にそろう', cb);

  // --- タブを行き来する ---
  await p.click('text=コスチューム'); await p.waitForTimeout(800);
  ok(await p.$('.lt-palette-size') === null, 'コスチュームタブではボタンを出さない');
  await p.click('[class*="gui_tab_"] >> nth=0'); await p.waitForTimeout(1200);
  s = await st();
  ok(await p.$('.lt-palette-size') !== null && s.flyW === fit2.flyW, 'コードタブに戻るとボタンが出て、幅もそのまま', s);

  // --- 窓を小さくすると縮み、大きくすると戻る ---
  await dragHandle(400);
  const wide = (await st()).flyW;
  await p.setViewportSize({ width: 1100, height: 800 }); await p.waitForTimeout(1200);
  const small = await st();
  const bw2 = await p.evaluate(() => document.querySelector('[class*="blocks_blocks_"]').offsetWidth);
  ok(small.flyW <= bw2 - 60 - 300 && small.absLeft === small.tbW, '窓を小さくすると、コードエリア 300 を残して縮む', { wide, small: small.flyW, bw2 });
  await p.setViewportSize({ width: 1440, height: 900 }); await p.waitForTimeout(1200);
  s = await st();
  ok(s.flyW === wide, '窓を元に戻すと、決めた幅に戻る', { wide, now: s.flyW });

  // --- 言語を変える（ブロックの画面が作り直される） ---
  const keep = await st();
  await p.click('text=設定'); await p.waitForTimeout(400); await p.click('text=言語'); await p.waitForTimeout(400);
  await p.click('text=English'); await p.waitForTimeout(3000);
  s = await st();
  const enLabel = await p.$eval('.lt-palette-size', e => e.textContent);
  ok(s.flyW === keep.flyW && s.flyScale === keep.flyScale, '言語を変えても幅と大きさが残る', { keep, s });
  ok(enLabel.includes('Block size') && enLabel.includes('Reset'), 'ボタンが英語になる', enLabel);
  const title = await p.$eval('.lt-palette-handle', e => e.title);
  ok(title.startsWith('Drag'), 'つまみの説明も英語になる', title);
  // ひらがな
  await p.click('text=Settings'); await p.waitForTimeout(400); await p.click('text=Language'); await p.waitForTimeout(400);
  await p.click('text=にほんご'); await p.waitForTimeout(3000);
  const hira = await p.$eval('.lt-palette-size', e => e.textContent);
  ok(hira.includes('ブロックのおおきさ') && hira.includes('もとにもどす'), 'ひらがなのボタン', hira);
  // テーマ（redux の store に直接たのむ。メニューの見た目には頼らない）
  await grab(); // 比べる基準は、テーマを切りかえる直前の画面
  await p.evaluate(() => {
    const el = document.querySelector('[class*="gui_page-wrapper"]');
    const key = Object.keys(el).find(k => k.startsWith('__reactFiber') || k.startsWith('__reactInternalInstance'));
    let f = el[key], store = null;
    for (let i = 0; f && i < 60 && !store; i++, f = f.return) {
      const v = f.memoizedProps && f.memoizedProps.store; if (v && v.dispatch) store = v;
      const c = f.stateNode && f.stateNode.context && f.stateNode.context.store; if (!store && c && c.dispatch) store = c;
    }
    window.__ws0 = window.__ws;
    store.dispatch({ type: 'scratch-gui/theme/SET_THEME', theme: 'high-contrast' });
  });
  await p.waitForTimeout(3000);
  s = await st();
  ok(await p.evaluate(() => window.__ws0 !== window.__ws), 'テーマを変えるとブロックの画面が作り直される（試験の前提）');
  ok(s.flyW === keep.flyW && s.flyScale === keep.flyScale && near(s.handleX + 2, s.flyRight, 3), 'テーマを変えても幅と大きさが残り、つまみも 1 つだけ', s);
  ok(await p.$$eval('.lt-palette-handle', x => x.length) === 1, 'つまみは 1 つだけ');
  await p.keyboard.press('Escape');

  // --- 指でドラッグ・ダブルタップ ---
  const cdp = await ctx.newCDPSession(p);
  const touch = async (type, x, y) => cdp.send('Input.dispatchTouchEvent', { type, touchPoints: type === 'touchEnd' ? [] : [{ x, y }] });
  let t0 = await st();
  let tc = await handleCenter();
  await touch('touchStart', tc.x, tc.y);
  for (let i = 1; i <= 8; i++) { await touch('touchMove', tc.x - 10 * i, tc.y); await p.waitForTimeout(25); }
  await touch('touchEnd'); await p.waitForTimeout(500);
  s = await st();
  ok(near(s.flyW, Math.max(200, t0.flyW - 80), 4), '指でドラッグすると幅が変わる', { before: t0.flyW, after: s.flyW });
  await p.locator('.lt-palette-size [data-lt-palette="reset"]').click(); await p.waitForTimeout(400);
  await p.click('.scratchCategoryId-minecraft'); await p.waitForTimeout(1000);
  tc = await handleCenter();
  await touch('touchStart', tc.x, tc.y); await touch('touchEnd'); await p.waitForTimeout(120);
  await touch('touchStart', tc.x, tc.y); await touch('touchEnd'); await p.waitForTimeout(600);
  s = await st();
  ok(s.flyW > 250, '指でダブルタップすると長いブロックに合わせる', s.flyW);
  const wsCount = await p.evaluate(() => __ws.getTopBlocks(false).length);
  console.log('（参考）コードエリアのブロック数', wsCount);


  // ================= レビュー 1 回目の直しの試験 =================
  await p.setViewportSize({ width: 1440, height: 900 });
  await p.locator('.lt-palette-size [data-lt-palette="reset"]').click(); await p.waitForTimeout(400);
  await p.click('.scratchCategoryId-motion'); await p.waitForTimeout(1200);

  // ボタンはタブの列（tablist）の外。読み上げ名がある
  const aria = await p.evaluate(() => {
    const g = document.querySelector('.lt-palette-size');
    return { inTablist: !!g.closest('[role="tablist"]'), role: g.getAttribute('role'),
      labels: [...g.querySelectorAll('button')].map(b => b.getAttribute('aria-label')) };
  });
  ok(!aria.inTablist && aria.role === 'group' && aria.labels.every(Boolean), 'ボタンはタブの列の外で、読み上げ名がある', aria);
  const tb2 = await p.evaluate(() => {
    const tabs = [...document.querySelectorAll('[class*="gui_tab_"]')].map(t => t.getBoundingClientRect());
    const g = document.querySelector('.lt-palette-size').getBoundingClientRect();
    return { lastTabRight: Math.round(Math.max(...tabs.map(t => t.right))), gLeft: Math.round(g.left), gMid: Math.round(g.top + g.height / 2), tabMid: Math.round((Math.min(...tabs.map(t => t.top)) + Math.max(...tabs.map(t => t.bottom))) / 2) };
  });
  ok(tb2.gLeft >= tb2.lastTabRight && tb2.gLeft - tb2.lastTabRight < 30 && Math.abs(tb2.gMid - tb2.tabMid) < 12, 'ボタンは今までどおり「音」タブのすぐ右', tb2);

  // 2 本目の指が来ても、1 本目のドラッグが続き、保存される
  await grab();
  let w0 = (await st()).flyW;
  let hcc = await handleCenter();
  const tp = pts => pts; // CDP にそのまま渡す指の一覧
  await cdp.send('Input.dispatchTouchEvent', { type: 'touchStart', touchPoints: tp([{ x: hcc.x, y: hcc.y, id: 1 }]) });
  for (let i = 1; i <= 5; i++) { await cdp.send('Input.dispatchTouchEvent', { type: 'touchMove', touchPoints: tp([{ x: hcc.x + 10 * i, y: hcc.y, id: 1 }]) }); await p.waitForTimeout(25); }
  await cdp.send('Input.dispatchTouchEvent', { type: 'touchStart', touchPoints: tp([{ x: hcc.x + 50, y: hcc.y, id: 1 }, { x: hcc.x + 52, y: hcc.y + 200, id: 2 }]) });
  await p.waitForTimeout(50);
  // 2 本目だけを離す（離した指を touchEnd に渡す）
  await cdp.send('Input.dispatchTouchEvent', { type: 'touchEnd', touchPoints: tp([{ x: hcc.x + 52, y: hcc.y + 200, id: 2 }]) });
  for (let i = 6; i <= 10; i++) { await cdp.send('Input.dispatchTouchEvent', { type: 'touchMove', touchPoints: tp([{ x: hcc.x + 10 * i, y: hcc.y, id: 1 }]) }); await p.waitForTimeout(25); }
  await cdp.send('Input.dispatchTouchEvent', { type: 'touchEnd', touchPoints: [] });
  await p.waitForTimeout(500);
  s = await st();
  const active = await p.$eval('.lt-palette-handle', e => e.classList.contains('lt-palette-handle--active'));
  ok(near(s.flyW, w0 + 100, 4) && s.saved && JSON.parse(s.saved).width === s.flyW && !active, '2 本目の指が来ても 1 本目のドラッグが続き、保存され、強調も消える', { w0, s, active });

  // 途中で止められた（touchCancel）あとの 1 回のタップは、ダブルタップにしない
  w0 = (await st()).flyW;
  hcc = await handleCenter();
  await cdp.send('Input.dispatchTouchEvent', { type: 'touchStart', touchPoints: tp([{ x: hcc.x, y: hcc.y, id: 3 }]) });
  await cdp.send('Input.dispatchTouchEvent', { type: 'touchCancel', touchPoints: [] });
  await p.waitForTimeout(80);
  await cdp.send('Input.dispatchTouchEvent', { type: 'touchStart', touchPoints: tp([{ x: hcc.x, y: hcc.y, id: 4 }]) });
  await cdp.send('Input.dispatchTouchEvent', { type: 'touchEnd', touchPoints: [] });
  await p.waitForTimeout(500);
  s = await st();
  ok(s.flyW === w0, '途中で止められたあとの 1 回のタップでは、幅を合わせない', { w0, now: s.flyW });

  // つまみを押したまま窓が縮み、動かさずに離しても、上限がかかる
  await dragHandle(2000);
  const big = (await st()).flyW;
  hcc = await handleCenter();
  await p.mouse.move(hcc.x, hcc.y); await p.mouse.down();
  await p.setViewportSize({ width: 1100, height: 800 }); await p.waitForTimeout(800);
  await p.mouse.up(); await p.waitForTimeout(500);
  s = await st();
  const bw3 = await p.evaluate(() => document.querySelector('[class*="blocks_blocks_"]').offsetWidth);
  ok(s.flyW <= bw3 - 60 - 300, '押したまま窓が縮んでも、離したらコードエリアが 300 残る', { big, now: s.flyW, max: bw3 - 360 });
  await p.setViewportSize({ width: 1440, height: 900 }); await p.waitForTimeout(800);

  // メニューを開いたまま大きさを変えると、メニューが閉じる
  await p.locator('.lt-palette-size [data-lt-palette="reset"]').click(); await p.waitForTimeout(400);
  await p.click('.scratchCategoryId-motion'); await p.waitForTimeout(1200);
  await grab();
  const dd = await p.evaluate(() => {
    const fl = __ws.getFlyout();
    const bl = fl.workspace_.getTopBlocks(false).find(x => x.type === 'motion_goto');
    const menu = bl.getInputTargetBlock('TO');
    const field = menu.inputList[0].fieldRow[0];
    const r = field.getSvgRoot().getBoundingClientRect();
    return { x: r.x + r.width / 2, y: r.y + r.height / 2 };
  });
  await p.mouse.click(dd.x, dd.y); await p.waitForTimeout(500);
  const openBefore = await p.evaluate(() => __SB.DropDownDiv.isVisible());
  await p.locator('.lt-palette-size [data-lt-palette="larger"]').click(); await p.waitForTimeout(500);
  const openAfter = await p.evaluate(() => __SB.DropDownDiv.isVisible());
  ok(openBefore === true && openAfter === false, 'メニューを開いたまま大きさを変えると、メニューが閉じる', { openBefore, openAfter });

  // カテゴリーへスクロールしている途中で大きさを変えても、そのカテゴリーで止まる
  await p.click('.scratchCategoryId-motion'); await p.waitForTimeout(1200);
  await p.evaluate(() => {
    // 並べ直しは、見ていたカテゴリーを読む前に scrollTarget を覚える。そのとき途中だったかを記録する
    window.__sawTarget = false;
    const tb = __ws.toolbox_; const fl = __ws.getFlyout(); const o = tb.getSelectedCategoryId;
    tb.getSelectedCategoryId = function () { if (fl.scrollTarget != null) window.__sawTarget = true; return o.apply(this, arguments); };
    // スクロールの動きを止めておく（行き先 scrollTarget は残るので、並べ直しのときに必ず「途中」になる）
    fl.ltStep = fl.stepScrollAnimation; fl.stepScrollAnimation = function () {};
  });
  await p.click('.scratchCategoryId-operators');
  await p.locator('.lt-palette-size [data-lt-palette="larger"]').click();
  const scScaled = await waitScale(1.2);
  await p.waitForTimeout(300);
  await p.evaluate(() => { const fl = __ws.getFlyout(); fl.stepScrollAnimation = fl.ltStep; delete fl.ltStep; });
  const sc = await p.evaluate(() => { const tb = __ws.toolbox_; const fl = __ws.getFlyout(); return { sawTarget: window.__sawTarget, cat: tb.getSelectedCategoryId(), pos: tb.getCategoryPositionById('operators'), scroll: fl.getScrollPos() }; });
  ok(scScaled && sc.sawTarget && sc.cat === 'operators' && Math.abs(sc.scroll - sc.pos) < 3, 'スクロールの途中で大きさを変えても「演算」で止まる', sc);

  // 並べ直しの回数（左から右の言語・接続ボタンの無い拡張だけ）
  await p.locator('.lt-palette-size [data-lt-palette="reset"]').click(); await p.waitForTimeout(400);
  await p.evaluate(() => { const tb = __ws.toolbox_; window.__rs = 0; const orig = tb.ltOrigRS || (tb.ltOrigRS = tb.refreshSelection); /* 二重にかけない */ tb.refreshSelection = function () { window.__rs++; return orig.apply(this, arguments); }; });
  await dragHandle(150);
  const afterDrag = await p.evaluate(() => window.__rs);
  ok(afterDrag === 0, '幅だけのドラッグでは並べ直さない', afterDrag);
  await dragHandle(2000);
  await p.evaluate(() => { window.__rs = 0; });
  for (let wv = 1400; wv >= 1100; wv -= 20) { await p.setViewportSize({ width: wv, height: 900 }); await p.waitForTimeout(16); }
  await p.waitForTimeout(600);
  const afterResize = await p.evaluate(() => window.__rs);
  s = await st();
  const bw4 = await p.evaluate(() => document.querySelector('[class*="blocks_blocks_"]').offsetWidth);
  ok(afterResize === 0 && s.flyW === bw4 - 360, '窓を何度も小さくしても並べ直さず、幅は上限に合う', { afterResize, flyW: s.flyW, max: bw4 - 360 });
  await p.evaluate(() => { window.__rs = 0; });
  await p.locator('.lt-palette-size [data-lt-palette="larger"]').click();
  await waitUntil(() => window.__rs >= 1); await p.waitForTimeout(400); // 1 回目のあと、余計にもう 1 回走らないか見る
  ok(await p.evaluate(() => window.__rs) === 1, '大きさボタン 1 回で並べ直しは 1 回', await p.evaluate(() => window.__rs));
  await p.setViewportSize({ width: 1440, height: 900 }); await p.waitForTimeout(600);
  await p.locator('.lt-palette-size [data-lt-palette="reset"]').click(); await p.waitForTimeout(300);
  await p.evaluate(() => { window.__rs = 0; });
  await p.locator('.lt-palette-size [data-lt-palette="reset"]').click(); await p.waitForTimeout(300);
  ok(await p.evaluate(() => window.__rs) === 0, '何も変わらない「元に戻す」では並べ直さない', await p.evaluate(() => window.__rs));


  // ================= レビュー 2 回目の直しの試験 =================
  // 「＋」を 5 回続けて押しても、作り直しは 1 回
  await p.setViewportSize({ width: 1440, height: 900 }); await p.waitForTimeout(500);
  await p.locator('.lt-palette-size [data-lt-palette="reset"]').click(); await p.waitForTimeout(400);
  await p.evaluate(() => { window.__rs = 0; });
  await p.evaluate(() => { const b = document.querySelector('[data-lt-palette="larger"]'); for (let i = 0; i < 5; i++) b.click(); });
  await waitUntil(() => window.__rs >= 1); await p.waitForTimeout(400); // 1 回目のあと、余計にもう 1 回走らないか見る
  s = await st();
  ok(await p.evaluate(() => window.__rs) === 1 && s.flyScale === +(0.675 * 1.5).toFixed(4), '「＋」を 5 回続けて押しても作り直しは 1 回', { rs: await p.evaluate(() => window.__rs), s });

  // キーボードで上限まで押すと、フォーカスは反対側のボタンへ
  await p.locator('.lt-palette-size [data-lt-palette="reset"]').click(); await p.waitForTimeout(400);
  await p.focus('.lt-palette-size [data-lt-palette="larger"]');
  for (let i = 0; i < 5; i++) { await p.keyboard.press('Enter'); await p.waitForTimeout(50); } // 1.0 → 1.5 は 5 回
  const foc = await p.evaluate(() => ({ active: document.activeElement && document.activeElement.getAttribute('data-lt-palette'), larger: document.querySelector('[data-lt-palette="larger"]').disabled }));
  ok(foc.larger === true && foc.active === 'smaller', '上限でフォーカスが「−」へ移る', foc);
  await p.waitForTimeout(400);
  await p.locator('.lt-palette-size [data-lt-palette="reset"]').click(); await p.waitForTimeout(400);

  // ================= レビュー 3 回目の直しの試験 =================
  // 1 本目の指でパレットのブロックを運んでいる途中に、2 本目の指でつまみを動かして離しても、
  // 運んでいるブロックは消えず、置いた場所に出て、作品のデータとも合う
  await p.locator('.lt-palette-size [data-lt-palette="reset"]').click(); await p.waitForTimeout(400);
  await p.click('.scratchCategoryId-variables'); await p.waitForTimeout(1200);
  await grab();
  await p.evaluate(() => {
    window.__rs = 0; window.__gestAtRs = false;
    const tb = __ws.toolbox_; const o = tb.ltOrigRS || (tb.ltOrigRS = tb.refreshSelection); /* 二重にかけない */
    tb.refreshSelection = function () { window.__rs++; if (__ws.currentGesture_) window.__gestAtRs = true; return o.apply(this, arguments); };
  });
  const mtBefore = await p.evaluate(() => __ws.getTopBlocks(false).length);
  const mtErrBefore = errors.length;
  const vb = await p.evaluate(() => {
    const fl = __ws.getFlyout();
    const o = fl.workspace_.getTopBlocks(false).filter(x => x.type === 'data_setvariableto').map(x => x.getSvgRoot().getBoundingClientRect()).find(r => r.top > 100 && r.bottom < 800 && r.width > 0);
    return o ? { x: Math.round(o.x + 10), y: Math.round(o.y + o.height / 2) } : null;
  });
  const hbx = await p.locator('.lt-palette-handle').boundingBox();
  const T = (type, pts) => cdp.send('Input.dispatchTouchEvent', { type, touchPoints: pts });
  // 1 本目の指でブロックをつかんでから「＋」を押す（並べ直しは、運び終わるまで待つはず）
  let f1 = { x: vb.x, y: vb.y, id: 11 };
  await T('touchStart', [f1]); await p.waitForTimeout(100);
  const gestOn = await p.evaluate(() => !!__ws.currentGesture_);
  await p.evaluate(() => document.querySelector('[data-lt-palette="larger"]').click());
  for (let i = 1; i <= 8; i++) { f1 = { x: vb.x + 30 * i, y: vb.y + 4 * i, id: 11 }; await T('touchMove', [f1]); await p.waitForTimeout(30); }
  const f2x = Math.round(hbx.x + hbx.width / 2); const f2y = Math.round(hbx.y + 300);
  let f2 = { x: f2x, y: f2y, id: 12 };
  const wBefore2 = await p.evaluate(() => __ws.getFlyout().getWidth());
  await T('touchStart', [f1, f2]); await p.waitForTimeout(60);
  for (let i = 1; i <= 6; i++) { f2 = { x: f2x + 20 * i, y: f2y, id: 12 }; await T('touchMove', [f1, f2]); await p.waitForTimeout(30); }
  await T('touchEnd', [f2]); // 2 本目だけを離す（この CDP では、離した指を touchEnd に渡すと 2 本目の pointerup が出る）
  await p.waitForTimeout(400);
  const wAfter2 = await p.evaluate(K => ({ w: __ws.getFlyout().getWidth(), saved: localStorage.getItem(K) }), KEY);
  const midRs = await p.evaluate(() => ({ rs: window.__rs, gestAtRs: window.__gestAtRs }));
  for (let i = 9; i <= 24; i++) { f1 = { x: vb.x + 30 * i, y: vb.y + 4 * i, id: 11 }; await T('touchMove', [f1]); await p.waitForTimeout(30); }
  const dropAt = { x: f1.x, y: f1.y };
  await T('touchEnd', []); await p.waitForTimeout(800);
  const mt = await p.evaluate(() => {
    const tops = __ws.getTopBlocks(false);
    const vmt = vm.editingTarget.blocks;
    return {
      n: tops.length,
      unrendered: tops.filter(x => !x.rendered).length,
      blocks: tops.map(x => { const xy = x.getRelativeToSurfaceXY(); const v = vmt.getBlock(x.id); const r = x.getSvgRoot().getBoundingClientRect(); return { t: x.type, wx: Math.round(xy.x), wy: Math.round(xy.y), vx: v ? Math.round(v.x) : null, vy: v ? Math.round(v.y) : null, sx: Math.round(r.x), sy: Math.round(r.y), sw: Math.round(r.width), sh: Math.round(r.height) }; }),
      rs: window.__rs, gestAtRs: window.__gestAtRs, scale: __ws.getFlyout().workspace_.scale
    };
  });
  const placedB = mt.blocks.find(x => x.t === 'data_setvariableto');
  ok(mt.n === mtBefore + 1 && mt.unrendered === 0 && placedB && placedB.vx === placedB.wx && placedB.vy === placedB.wy &&
     dropAt.x >= placedB.sx - 4 && dropAt.x <= placedB.sx + placedB.sw && dropAt.y >= placedB.sy - 6 && dropAt.y <= placedB.sy + placedB.sh + 6,
     '運んでいる途中に 2 本目の指でつまみを動かしても、ブロックは置いた場所に 1 つだけ出て、作品のデータと合う', { mt, dropAt, mtBefore });
  ok(near(wAfter2.w, wBefore2 + 120, 6) && wAfter2.saved && JSON.parse(wAfter2.saved).width === wAfter2.w,
     '運んでいる途中でも、2 本目の指で幅が変わり、保存される', { wBefore2, wAfter2 });
  ok(gestOn && !mt.gestAtRs && midRs.rs === 0 && mt.rs >= 1 && Math.abs(mt.scale - 0.675 * 1.1) < 1e-6 && errors.length === mtErrBefore,
     '運んでいる間は並べ直さず、運び終わってから並べ直す', { gestOn, midRs, rs: mt.rs, gestAtRs: mt.gestAtRs, scale: mt.scale, newErrors: errors.slice(mtErrBefore) });
  // 片付け（作ったブロックを消し、幅と大きさを戻す）
  await p.evaluate(() => { __ws.getTopBlocks(false).forEach(x => x.dispose(false)); });
  await p.locator('.lt-palette-size [data-lt-palette="reset"]').click(); await p.waitForTimeout(400);

  // 「＋」の並べ直しを待っている間（ブロックをつかんでいる間）に 2 本目の指でダブルタップしても、
  // 幅はこれからかける大きさで測る（運び終わって並べ直したあと、いちばん長いブロックが入っている）
  await p.click('.scratchCategoryId-motion'); await p.waitForTimeout(1200);
  await grab();
  const mb = await p.evaluate(() => {
    const fl = __ws.getFlyout();
    const r = fl.workspace_.getTopBlocks(false).find(x => x.type === 'motion_movesteps').getSvgRoot().getBoundingClientRect();
    return { x: Math.round(r.x + 10), y: Math.round(r.y + r.height / 2) };
  });
  const hb2 = await p.locator('.lt-palette-handle').boundingBox();
  const g1 = { x: mb.x, y: mb.y, id: 21 };
  await T('touchStart', [g1]); await p.waitForTimeout(100);
  const fitGest = await p.evaluate(() => !!__ws.currentGesture_);
  await p.evaluate(() => { const b = document.querySelector('[data-lt-palette="larger"]'); for (let i = 0; i < 3; i++) b.click(); });
  await p.waitForTimeout(300); // 並べ直しはつかんでいる間は待つ
  const g2 = { x: Math.round(hb2.x + hb2.width / 2), y: Math.round(hb2.y + 300), id: 22 };
  await T('touchStart', [g1, g2]); await T('touchEnd', [g2]); await p.waitForTimeout(80);
  await T('touchStart', [g1, { ...g2, id: 23 }]); await T('touchEnd', [{ ...g2, id: 23 }]); await p.waitForTimeout(150);
  await T('touchEnd', []);
  await waitScale(1.3);
  await p.waitForTimeout(300);
  const fit3 = await p.evaluate(() => {
    const fl = __ws.getFlyout(); const fws = fl.workspace_; const tb = __ws.toolbox_;
    const cat = (fl.categoryScrollPositions || []).find(x => x.categoryId === 'motion');
    let need = 0;
    fws.getTopBlocks(false).forEach(bl => { const xy = bl.getRelativeToSurfaceXY(); if (cat && (xy.y < cat.position - 1 || (cat.length && xy.y >= cat.position + cat.length))) return; need = Math.max(need, xy.x + bl.getHeightWidth().width); });
    const max = document.querySelector('[class*="blocks_blocks_"]').offsetWidth - 360;
    return { needPx: Math.ceil(need * fws.scale), flyW: fl.getWidth(), max, scale: fws.scale };
  });
  ok(fitGest && Math.abs(fit3.scale - 0.675 * 1.3) < 1e-6 && fit3.flyW > 250 && fit3.flyW >= Math.min(fit3.needPx, fit3.max) && fit3.flyW <= Math.min(fit3.needPx + 30, fit3.max),
     '並べ直しを待っている間のダブルタップでも、これからかける大きさで長いブロックに合わせる', { fitGest, fit3 });
  await p.evaluate(() => { __ws.getTopBlocks(false).forEach(x => x.dispose(false)); });
  await p.locator('.lt-palette-size [data-lt-palette="reset"]').click(); await p.waitForTimeout(400);

  // micro:bit（接続ボタン付きの見出し）: ドラッグを止めてから離しても、接続ボタンが新しい幅に合う
  await p.evaluate(() => vm.extensionManager.loadExtensionURL('microbit'));
  await p.waitForTimeout(2000);
  await p.click('.scratchCategoryId-microbit'); await p.waitForTimeout(1200);
  await grab();
  const header = () => p.evaluate(() => {
    const fl = __ws.getFlyout();
    const h = (fl.buttons_ || []).find(b => b instanceof __SB.FlyoutExtensionCategoryHeader && b.imageSrc_);
    if (!h) return null;
    const img = h.imageElement_ && h.imageElement_.getBoundingClientRect();
    const fr = document.querySelector('.blocklyFlyout').getBoundingClientRect();
    return { headerW: h.flyoutWidth_, flyW: fl.getWidth(), imgRight: img ? Math.round(img.right) : null, flyRight: Math.round(fr.right) };
  });
  const slowDrag = async dx => {
    const c0 = await handleCenter();
    await p.mouse.move(c0.x, c0.y); await p.mouse.down();
    for (let i = 1; i <= 10; i++) { await p.mouse.move(c0.x + dx * i / 10, c0.y); await p.waitForTimeout(30); }
    await p.waitForTimeout(150); // 最後の描き直しが済んでから離す
    await p.mouse.up(); await p.waitForTimeout(500);
  };
  await slowDrag(150);
  let hd = await header();
  ok(hd && hd.headerW === hd.flyW && hd.imgRight <= hd.flyRight, 'micro:bit: 広げて止めてから離すと、接続ボタンが新しい幅に合う', hd);
  await slowDrag(-200);
  hd = await header();
  ok(hd && hd.headerW === hd.flyW && hd.imgRight <= hd.flyRight, 'micro:bit: 狭めて止めてから離しても、接続ボタンがパレットの中', hd);

  // micro:bit のまま: パレットのブロックをつかんでいる間に窓が縮んでも、つかんでいる間は並べ直さず、
  // 運び終わってから並べ直す。ブロックは消えずエラーも出ない
  await p.click('.scratchCategoryId-variables'); await p.waitForTimeout(1200);
  await dragHandle(2000);
  await grab();
  await p.evaluate(() => {
    window.__rs2 = 0; window.__gest2 = false;
    const tb = __ws.toolbox_; const o = tb.ltOrigRS || (tb.ltOrigRS = tb.refreshSelection); /* 二重にかけない */
    tb.refreshSelection = function () { window.__rs2++; if (__ws.currentGesture_) window.__gest2 = true; return o.apply(this, arguments); };
  });
  const errBefore = errors.length;
  const nBefore = await p.evaluate(() => __ws.getTopBlocks(false).length);
  const blk = await p.evaluate(() => {
    const fl = __ws.getFlyout();
    // 使い回されない（並べ直すと作り直される）変数のブロックをつかむ
    const bl = fl.workspace_.getTopBlocks(false).filter(x => x.type === 'data_setvariableto').map(x => ({ x, r: x.getSvgRoot().getBoundingClientRect() })).find(o => o.r.top > 100 && o.r.bottom < 800 && o.r.width > 0);
    return bl ? { x: bl.r.x + 10, y: bl.r.y + bl.r.height / 2 } : null;
  });
  let hold = null;
  if (blk) {
    await p.mouse.move(blk.x, blk.y); await p.mouse.down();
    await p.waitForTimeout(100);
    const gest = await p.evaluate(() => !!__ws.currentGesture_);
    await p.setViewportSize({ width: 1150, height: 900 }); // つかんだまま窓を縮める
    await p.waitForTimeout(700); // 遅らせた並べ直しの時間（250ms）を十分に過ぎるまで、つかんだまま待つ
    hold = { gest, rs: await p.evaluate(() => window.__rs2) };
    await p.mouse.move(blk.x + 20, blk.y + 5);
    for (let i = 1; i <= 20; i++) { await p.mouse.move(blk.x + 20 + 25 * i, blk.y + 5 + 3 * i); await p.waitForTimeout(30); }
    await p.mouse.up();
    await waitUntil(() => window.__rs2 >= 1);
    await p.waitForTimeout(300);
  }
  const nAfter = await p.evaluate(() => __ws.getTopBlocks(false).length);
  const rsAfter = await p.evaluate(() => ({ rs: window.__rs2, gest: window.__gest2 }));
  ok(blk && hold && hold.gest && hold.rs === 0 && rsAfter.rs >= 1 && !rsAfter.gest,
     'ブロックをつかんでいる間に窓が縮んでも、つかんでいる間は並べ直さず、運び終わってから並べ直す', { hold, rsAfter });
  ok(blk && nAfter === nBefore + 1 && errors.length === errBefore, 'つかんでいる間に窓が縮んでも、運んだブロックが出てエラーも無い', { blk, nBefore, nAfter, newErrors: errors.slice(errBefore) });
  await p.setViewportSize({ width: 1440, height: 900 }); await p.waitForTimeout(600);

  // 右から左の言語（アラビア語）: 止めてから離すと 1 回並べ直し、ブロックが右に寄る
  await p.click('text=/せってい|設定|Settings/'); await p.waitForTimeout(400);
  await p.click('text=/げんご|言語|Language/'); await p.waitForTimeout(400);
  await p.click('text=العربية'); await p.waitForTimeout(3000);
  await grab();
  await p.click('.scratchCategoryId-motion'); await p.waitForTimeout(1200);
  await p.locator('.lt-palette-size [data-lt-palette="reset"]').click(); await p.waitForTimeout(400); // 広げる余地をつくる
  await p.evaluate(() => { const tb = __ws.toolbox_; window.__rs = 0; const orig = tb.ltOrigRS || (tb.ltOrigRS = tb.refreshSelection); /* 二重にかけない */ tb.refreshSelection = function () { window.__rs++; return orig.apply(this, arguments); }; });
  const rtlGap = () => p.evaluate(() => {
    const fl = __ws.getFlyout(); const fws = fl.workspace_;
    const bl = fws.getTopBlocks(false).find(x => x.type === 'motion_movesteps');
    return Math.round(fl.getWidth() / fws.scale - fl.MARGIN - bl.getRelativeToSurfaceXY().x);
  });
  ok(await p.evaluate(() => __ws.RTL) === true, '（前提）アラビア語で右から左になっている');
  await slowDrag(-120); // 右から左では、左へ動かすと広がる
  const rs1 = await p.evaluate(() => window.__rs);
  const gap1 = await rtlGap();
  ok(rs1 === 1 && Math.abs(gap1) <= 2, 'アラビア語: 止めてから離すと 1 回並べ直し、ブロックが右に寄る', { rs1, gap1, flyW: (await st()).flyW });
  // アラビア語: ブロックをつかんだまま「＋」を 3 回押し、2 本目の指でダブルタップしても、
  // 並べ直したあとに「見た目」のいちばん長いブロックがパレットに収まる（右から左は、今並んでいる倍率で位置を読む）
  await p.locator('.lt-palette-size [data-lt-palette="reset"]').click(); await p.waitForTimeout(500);
  await p.click('.scratchCategoryId-looks'); await p.waitForTimeout(1200);
  await grab();
  const rtlMeasure = () => p.evaluate(() => {
    const fl = __ws.getFlyout(); const fws = fl.workspace_;
    const cat = fl.categoryScrollPositions.find(x => x.categoryId === 'looks');
    if (!cat) return null;
    const fr = document.querySelector('.blocklyFlyout').getBoundingClientRect();
    let minLeft = Infinity; let need = 0;
    fws.getTopBlocks(false).forEach(bl => {
      const xy = bl.getRelativeToSurfaceXY();
      if (xy.y < cat.position - 1 || (cat.length && xy.y >= cat.position + cat.length)) return;
      const r = bl.getSvgRoot().getBoundingClientRect(); minLeft = Math.min(minLeft, r.left);
      need = Math.max(need, fl.getWidth() / fws.scale - (xy.x - bl.getHeightWidth().width));
    });
    return { flyW: fl.getWidth(), scale: fws.scale, flyLeft: Math.round(fr.left), minBlockLeftPx: Math.round(minLeft), needPx: Math.ceil(need * fws.scale), max: document.querySelector('[class*="blocks_blocks_"]').offsetWidth - 360 };
  });
  const rmb = await p.evaluate(() => {
    const fl = __ws.getFlyout();
    const bl = fl.workspace_.getTopBlocks(false).find(x => { const r = x.getSvgRoot().getBoundingClientRect(); return r.top > 150 && r.bottom < 800 && r.width > 0; });
    const r = bl.getSvgRoot().getBoundingClientRect();
    return { x: Math.round(r.right - 10), y: Math.round(r.y + r.height / 2) };
  });
  const rh = await p.locator('.lt-palette-handle').boundingBox();
  const r1 = { ...rmb, id: 31 };
  await T('touchStart', [r1]); await p.waitForTimeout(100);
  const rGest = await p.evaluate(() => !!__ws.currentGesture_);
  await p.evaluate(() => { const bt = document.querySelector('[data-lt-palette="larger"]'); for (let i = 0; i < 3; i++) bt.click(); });
  await p.waitForTimeout(300);
  const r2 = { x: Math.round(rh.x + rh.width / 2), y: Math.round(rh.y + 300), id: 32 };
  await T('touchStart', [r1, r2]); await T('touchEnd', [r2]); await p.waitForTimeout(80);
  await T('touchStart', [r1, { ...r2, id: 33 }]); await T('touchEnd', [{ ...r2, id: 33 }]); await p.waitForTimeout(150);
  const rMid = await p.evaluate(() => ({ flyW: __ws.getFlyout().getWidth(), scale: __ws.getFlyout().workspace_.scale }));
  await T('touchEnd', []);
  await waitScale(1.3); await p.waitForTimeout(400);
  const rAfter = await rtlMeasure();
  ok(rGest && rAfter && rMid.flyW > 250 && Math.abs(rMid.scale - 0.675) < 1e-9 && Math.abs(rAfter.scale - 0.675 * 1.3) < 1e-6 &&
     rAfter.flyW >= Math.min(rAfter.needPx, rAfter.max) && rAfter.flyW <= rAfter.needPx + 30 && rAfter.minBlockLeftPx >= rAfter.flyLeft - 1,
     'アラビア語: 並べ直しを待っている間のダブルタップでも、並べ直したあと長いブロックがパレットに収まる', { rGest, rMid, rAfter });
  await p.evaluate(() => { __ws.getTopBlocks(false).forEach(x => x.dispose(false)); });

  // 途中で失敗しても（ここでは workspace.resize が例外）、「元に戻す」は保存を消し、ボタンの表示を元に戻す
  await p.evaluate(() => { const b = document.querySelector('[data-lt-palette="larger"]'); for (let i = 0; i < 6; i++) b.click(); });
  await waitScale(1.5);
  await slowDrag(-80);
  const before5 = await p.evaluate(K => ({ saved: localStorage.getItem(K), largerOff: document.querySelector('[data-lt-palette="larger"]').disabled }), KEY);
  const warnBefore = errors.length;
  await p.evaluate(() => { window.__origResize = __ws.resize; __ws.resize = () => { throw new Error('試験: 描き直しの失敗'); }; });
  await p.locator('.lt-palette-size [data-lt-palette="reset"]').click(); await p.waitForTimeout(300);
  const after5 = await p.evaluate(K => ({ saved: localStorage.getItem(K), largerOff: document.querySelector('[data-lt-palette="larger"]').disabled }), KEY);
  await p.evaluate(() => { __ws.resize = window.__origResize; });
  const planted = t => t.includes('[palette]') && t.includes('試験: 描き直しの失敗');
  const warned = errors.slice(warnBefore).filter(planted);
  ok(before5.saved && before5.largerOff === true && after5.saved === null && after5.largerOff === false && warned.length >= 1,
     '途中で失敗しても「元に戻す」は保存を消し、「＋」を押せるように戻す（失敗は警告に出る）', { before5, after5, warned });
  errors.splice(warnBefore, errors.length - warnBefore, ...errors.slice(warnBefore).filter(t => !planted(t)));
  await p.evaluate(() => __ws.resize()); await p.waitForTimeout(500); // 失敗させた描き直しを、ここでやり直す
  await p.locator('.lt-palette-size [data-lt-palette="reset"]').click(); await p.waitForTimeout(400);

  // ================= 保存された値が壊れている・保存が使えないとき（別のページで開き直す） =================
  // React の中からパレットの今の幅と倍率を読む
  const palState = q => q.evaluate(() => {
    const el = document.querySelector('[class*="blocks_blocks_"]');
    const key = Object.keys(el).find(k => k.startsWith('__reactFiber') || k.startsWith('__reactInternalInstance'));
    let f = el[key];
    for (let i = 0; f && i < 30; i++, f = f.return) {
      const sn = f.stateNode;
      if (sn && sn.workspace && sn.ScratchBlocks) {
        const fl = sn.workspace.getFlyout();
        return { flyW: fl.getWidth(), scale: fl.workspace_.scale, max: el.offsetWidth - 360 };
      }
    }
    return null;
  });
  // init で保存の中身をしかけたページを開く
  const openWith = async init => {
    const c2 = await b.newContext({ viewport: { width: 1440, height: 900 }, locale: 'ja-JP' });
    await c2.route('**/*', offline);
    await c2.addInitScript(init);
    const errs = [];
    c2.on('page', pg => watchSockets(pg, errs)); // 別の窓が開いても見張る
    const q = await c2.newPage();
    q.on('pageerror', e => errs.push(e.message));
    q.on('console', m => { if (m.type() === 'warning' && m.text().includes('[palette]')) errs.push(m.text()); });
    q.on('console', m => { if (m.type() === 'error' && /not rendered|disposed/i.test(m.text())) errs.push(m.text()); });
    await q.goto(URL_, { waitUntil: 'networkidle', timeout: 90000 });
    await q.waitForSelector('.lt-palette-handle', { timeout: 30000 });
    await q.waitForTimeout(1500);
    return { c2, q, errs };
  };
  const stored = async value => {
    // ページのスクリプトより先に保存の中身をしかける（値は文字列に埋めこむ）
    const r2 = await openWith(`try { localStorage.setItem('laughtale.palette.v1', ${JSON.stringify(value)}); } catch (e) {}`);
    const st2 = await palState(r2.q);
    await r2.c2.close();
    return { st2, errs: r2.errs };
  };
  let sv = await stored('これは JSON ではない');
  ok(sv.st2 && sv.st2.flyW === 250 && Math.abs(sv.st2.scale - 0.675) < 1e-9 && sv.errs.length === 0, '保存の中身が JSON でなくても、元の幅と大きさで開く', sv);
  sv = await stored('{"width":1e9,"zoom":99}');
  ok(sv.st2 && sv.st2.flyW === sv.st2.max && Math.abs(sv.st2.scale - 0.675 * 1.5) < 1e-9 && sv.errs.length === 0, '保存の値が大きすぎても、上限に収めて開く', sv);
  sv = await stored('{"width":"x","zoom":null}');
  ok(sv.st2 && sv.st2.flyW === 250 && Math.abs(sv.st2.scale - 0.675) < 1e-9 && sv.errs.length === 0, '保存の値の型がちがっても、元の幅と大きさで開く', sv);
  // 保存が使えない（読み書きで例外になる）ブラウザでも使え、言語を変えても今の大きさが残る
  {
    const r = await openWith(`(() => { const no = () => { throw new Error('保存は使えません'); }; Storage.prototype.getItem = no; Storage.prototype.setItem = no; Storage.prototype.removeItem = no; })();`);
    await r.q.locator('.lt-palette-size [data-lt-palette="larger"]').click();
    await r.q.waitForTimeout(600);
    const s1 = await palState(r.q);
    await r.q.click('text=設定'); await r.q.waitForTimeout(400); await r.q.click('text=言語'); await r.q.waitForTimeout(400);
    await r.q.click('text=English'); await r.q.waitForTimeout(3000);
    const s2 = await palState(r.q);
    ok(s1 && s2 && Math.abs(s1.scale - 0.675 * 1.1) < 1e-9 && Math.abs(s2.scale - 0.675 * 1.1) < 1e-9 && r.errs.length === 0,
       '保存が使えないブラウザでも大きさを変えられ、言語を変えても残る', { s1, s2, errs: r.errs });
    await r.c2.close();
  }

  ok(errors.length === 0, 'ページのエラーなし', errors);
  console.log(fails ? `\n${fails} 件しっぱい` : '\nすべて OK');
  await b.close();
  server.close();
  process.exit(fails ? 1 : 0);
}

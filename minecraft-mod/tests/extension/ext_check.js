// Scratch のバンドル（gui.js・player.js・blocksonly.js・compatibilitytesting.js）から Scratch3MinecraftBlocks を切り出し、
// 偽物の WebSocket の上で、［接続］と止めるボタンの送る・送らないを確かめる
// 使い方: node ext_check.js <バンドルのファイル>（ふだんは同じフォルダの run.sh が 4 つとも流す）
'use strict';
const fs = require('fs');
const path = require('path');
const vm = require('vm');

const file = process.argv[2];
const text = fs.readFileSync(file, 'utf8').replace(/\r\n/g, '\n');
const start = text.indexOf('class Scratch3MinecraftBlocks {');
const end = text.indexOf('\n}\n', start);
if (start < 0 || end < 0) {
  console.error('Scratch3MinecraftBlocks が見つからない: ' + file);
  process.exit(2);
}
const src = text.slice(start, end + 2);

// 偽物の WebSocket（作ったものを sockets に覚える。open・reply・closeFromServer で、届いたことにする）
// close() は本物と同じく、まず CLOSING になり、あとで（finishPendingCloses で）CLOSED になって close を知らせる
// （つながる途中に close() したときは、本物と同じく close の前に error も知らせる。
//   知らせる時を試験が決めるのは、前のソケットの知らせが新しい接続を開いたあとに届く場合を確かめるため）
const sockets = [];
// close() を呼ばれて、まだ閉じ終わっていないソケット
const pendingCloses = [];
class FakeWebSocket {
  constructor(url) {
    // 本物と同じく、場所の書き方がおかしいときは作るところで投げる
    if (/\s/.test(url)) {
      throw new SyntaxError('invalid url: ' + url);
    }
    this.url = url;
    this.readyState = FakeWebSocket.CONNECTING;
    this.sent = [];
    sockets.push(this);
  }
  send(data) {
    // 本物と同じく、つながる前に送ると投げる
    if (this.readyState === FakeWebSocket.CONNECTING) {
      throw new Error('InvalidStateError: still in CONNECTING state');
    }
    this.sent.push(JSON.parse(data));
  }
  close() {
    if (this.readyState === FakeWebSocket.CLOSED || this.readyState === FakeWebSocket.CLOSING) {
      return;
    }
    // つながる途中に閉じると、本物はつながらなかったことにして、error のあと close を知らせる
    this.closedWhileConnecting = this.readyState === FakeWebSocket.CONNECTING;
    this.readyState = FakeWebSocket.CLOSING;
    pendingCloses.push(this);
  }
  // 閉じ終わった（CLOSED になってから、つながる途中に閉じたときは error、そのあと close を知らせる）
  finishClose() {
    if (this.readyState === FakeWebSocket.CLOSED) {
      return;
    }
    this.readyState = FakeWebSocket.CLOSED;
    if (this.closedWhileConnecting) {
      if (this.onerror) this.onerror({ type: 'error' });
      if (this.onclose) this.onclose({ code: 1006, wasClean: false });
    } else if (this.onclose) {
      this.onclose({ code: 1000, wasClean: true });
    }
  }
  // MOD の側から閉じ始めた（閉じるあいさつの途中。close はまだ知らせない。finishClose で閉じ終わる）
  closingFromServer() { this.readyState = FakeWebSocket.CLOSING; }
  open() { this.readyState = FakeWebSocket.OPEN; if (this.onopen) this.onopen(); }
  reply(msg) { if (this.onmessage) this.onmessage({ data: JSON.stringify(msg) }); }
  closeFromServer() { this.readyState = FakeWebSocket.CLOSED; if (this.onclose) this.onclose({ code: 1006, wasClean: false }); }
  // つながらなかった（MOD が動いていない）。本物のブラウザと同じく、CLOSED になってから error、そのあと close
  fail() {
    this.readyState = FakeWebSocket.CLOSED;
    if (this.onerror) this.onerror({ type: 'error' });
    if (this.onclose) this.onclose({ code: 1006, wasClean: false });
  }
}
FakeWebSocket.CONNECTING = 0;
FakeWebSocket.OPEN = 1;
FakeWebSocket.CLOSING = 2;
FakeWebSocket.CLOSED = 3;

// close() したソケットを、すべて閉じ終わらせる（前のソケットの知らせを届ける）
const finishPendingCloses = () => {
  while (pendingCloses.length) {
    pendingCloses.shift().finishClose();
  }
};

// 拡張機能が出した console.error（試験がわざと起こすものと、中身・回数・順番まで丸ごと比べる。
// ちがうものが出たら、拡張機能の中の例外を黙って受け止めているか、試験の流れが変わった）
// どの節で出たかも一緒に覚える（落ちたときに、どこで出たかが分かるように）
const errorLogs = [];
let currentSection = '（始まる前）';
const realLog = console.log;
console.log = (...a) => {
  if (typeof a[0] === 'string' && a[0].startsWith('--- ')) {
    currentSection = a[0].slice(4);
  }
  realLog(...a);
};
// 引数を文字にする（例外は「名前: 文」、ほかの物は JSON。instanceof は使わない。
// 拡張機能は vm の中で動くので、拡張機能の中で起きた組み込みの例外（TypeError など）は、この試験の Error とは別の realm のものになるため）。
// 文字にできない物（循環のある物など）でも投げない（ここで投げると、拡張機能のハンドラーの途中で止まり、確かめたい動きが変わるため）
const describe = a => {
  try {
    if (a && typeof a === 'object' && typeof a.name === 'string' && typeof a.message === 'string') {
      return a.name + ': ' + a.message;
    }
    return a && typeof a === 'object' ? JSON.stringify(a) : String(a);
  } catch (e) {
    return '（文字にできない物: ' + Object.prototype.toString.call(a) + '）';
  }
};
const EXPECTED_ERRORS = [
  // 受け付けると分かったあとの、命令の処理の中のエラー
  'Server error: {"errorCode":"INTERNAL_ERROR","errorMessage":"failed"}',
  // 接続の返事の代わりに来たエラー
  'Server error: {"errorCode":"SERVER_STOPPING","errorMessage":"stopping"}',
  // MOD が動いていない（fail）
  'WebSocket error: {"type":"error"}',
  // 接続の返事が success:false（今の MOD は返さないが、返したときの道）
  'Connection failed: 試験の失敗',
  // 場所の書き方がおかしく、ソケットを作れない
  'Connection error: SyntaxError: invalid url: ws://local host:14711/minecraft'
];

const listeners = {};
const runtime = {
  on: (ev, fn) => { (listeners[ev] = listeners[ev] || []).push(fn); },
  sequencer: { activeThread: null }
};
const ctx = {
  WebSocket: FakeWebSocket, console: { log() {}, warn() {}, error: (...a) => errorLogs.push({ section: currentSection, text: a.map(describe).join(' ') }) }, setTimeout, clearTimeout, Map, Promise, JSON, Date, Math,
  Array, Error, Object, Number, String,
  _objectSpread: (a, b) => Object.assign({}, a, b), formatMessage: m => m.default
};
vm.createContext(ctx);
const Cls = vm.runInContext('(' + src + ')', ctx);

let passed = 0;
let failed = 0;
const check = (name, ok, detail) => {
  if (ok) {
    passed++;
    console.log('  ok     ' + name);
  } else {
    failed++;
    console.log('  NG     ' + name + '  → ' + JSON.stringify(detail));
  }
};
const stopAllSent = s => s.sent.filter(m => m.payload && m.payload.action === 'stopAll').length;
const stop = () => (listeners.PROJECT_STOP_ALL || []).forEach(fn => fn());
// Promise が決着したか（200 ms 待つ。Promise でない値は、すぐ決着したと見る。壊れた［接続］で試験ごと止まらないように）
const settled = p => Promise.race([
  Promise.resolve(p).then(() => 'resolved', () => 'rejected'),
  new Promise(r => setTimeout(() => r('pending'), 200))
]);
const featureReply = features => ({ type: 'connect_response', payload: { success: true, sessionId: 's', serverInfo: { features } } });

(async () => {
  const ext = new Cls(runtime);
  // 録画の状態を聞き直すしくみは gui.js だけにある（派生バンドルには無いので、偽物も入れない）
  // gui.js だけの確かめは、ファイル名で切り替える（gui.js から聞き直しが消えたら、ここで落ちるように）
  const isGui = path.basename(file) === 'gui.js';
  const hasRecordingSync = typeof ext._syncRecordingStatus === 'function';
  check('録画の状態を聞き直すしくみは gui.js だけにある', hasRecordingSync === isGui, [path.basename(file), hasRecordingSync]);
  let recordingSyncs = 0;
  if (isGui) {
    ext._syncRecordingStatus = () => { recordingSyncs++; return Promise.resolve(); };
  }
  const local = { HOST: 'localhost', PORT: 14711 };

  console.log('--- つながる途中の 2 回目の［接続］は、1 回目と一緒に待つ');
  const p1 = ext.connect(local);
  const p2 = ext.connect(local);
  check('ソケットは 1 つだけ', sockets.length === 1, sockets.length);
  check('つながるまでは、どちらの［接続］も終わらない', (await settled(p1)) === 'pending' && (await settled(p2)) === 'pending', '');
  sockets[0].open();
  check('つながったら、どちらの［接続］も終わる', (await settled(p1)) === 'resolved' && (await settled(p2)) === 'resolved', '');
  const s = sockets[0];
  // つないだすぐあと（接続の返事が来る前）の同じ場所への［接続］も、つなぎ直さず、すぐ次へ進む
  const early = ext.connect(local);
  check('接続の返事の前の同じ場所への［接続］も、つなぎ直さず、すぐ次へ進む', early === undefined && sockets.length === 1,
    [typeof early, sockets.length]);
  if (isGui) {
    check('（gui.js）接続の返事の前の同じ場所への［接続］では、録画の状態を二重に聞かない', recordingSyncs === 0, recordingSyncs);
  }

  console.log('--- 接続の返事が来る前に止めたら送る');
  stop();
  check('返事の前の止めるボタンで stopAll を送る', stopAllSent(s) === 1, stopAllSent(s));

  console.log('--- 返事で受け付けないと分かったら送らない・受け付ければ送る');
  // 前の MOD の返事には、features のキーそのものが無い（MinecraftWebSocketHandler の handleConnect）
  s.reply({ type: 'connect_response', payload: { success: true, sessionId: 's',
    serverInfo: { version: '0.2.0', minecraftVersion: '1.20.1', maxClients: 8, currentClients: 1 } } });
  stop();
  check('前の MOD（返事に features が無い）には送らない', stopAllSent(s) === 1 && ext._canStopAll === false,
    [stopAllSent(s), ext._canStopAll]);
  s.reply(featureReply([]));
  stop();
  check('features に stopAll が無い MOD には送らない', stopAllSent(s) === 1, stopAllSent(s));
  s.reply(featureReply(['stopAll']));
  stop();
  check('受け付ける MOD には送る', stopAllSent(s) === 2, stopAllSent(s));
  s.reply({ type: 'error', requestId: 'r1', payload: { errorCode: 'INTERNAL_ERROR', errorMessage: 'failed' } });
  stop();
  check('受け付けると分かったあとの、命令の処理の中のエラー（INTERNAL_ERROR）では、送るのをやめない', stopAllSent(s) === 3 && ext._canStopAll === true,
    [stopAllSent(s), ext._canStopAll]);

  console.log('--- 同じ場所への［接続］は、つなぎ直さず、すぐ次へ進む');
  const syncsBefore = recordingSyncs;
  const skipped = ext.connect(local);
  check('つながっていれば新しいソケットを作らない', sockets.length === 1 && s.readyState === FakeWebSocket.OPEN, sockets.length);
  check('Promise を返さない（次のフレームまで待たせない）', skipped === undefined, typeof skipped);
  if (isGui) {
    check('（gui.js）つないでから 5 秒たつまでは、録画の状態を聞き直さない', recordingSyncs === syncsBefore, recordingSyncs - syncsBefore);
    ext._recordingSyncedAt = Date.now() - 6000;
    ext.connect(local);
    ext.connect(local);
    check('（gui.js）5 秒たったら、録画の状態を 1 回だけ聞き直す', recordingSyncs === syncsBefore + 1, recordingSyncs - syncsBefore);
  }

  console.log('--- スクリプトの中の「すべてを止める」は送らない。「すべてを止める」だけのクリックは送る');
  const before = stopAllSent(s);
  runtime.sequencer.activeThread = { stackClick: false, topBlock: 'hat', peekStack: () => 'stop' };
  stop();
  check('作品の中の「すべてを止める」は送らない', stopAllSent(s) === before, stopAllSent(s) - before);
  runtime.sequencer.activeThread = { stackClick: true, topBlock: 'show', peekStack: () => 'stop' };
  stop();
  check('「表示する → すべてを止める」のクリックは送らない', stopAllSent(s) === before, stopAllSent(s) - before);
  runtime.sequencer.activeThread = { stackClick: true, topBlock: 'stop', peekStack: () => 'stop' };
  stop();
  check('「すべてを止める」だけのクリックは送る', stopAllSent(s) === before + 1, stopAllSent(s) - before);
  runtime.sequencer.activeThread = null;

  console.log('--- 別の場所への［接続］はつなぎ直す。つながる途中の前の［接続］は失敗で終わり、新しい［接続］は成功する');
  const q1 = ext.connect({ HOST: '127.0.0.1', PORT: 14711 });
  check('別の場所への［接続］は、つなぎ直す（前のソケットは閉じ始める）', sockets.length === 2 && s.readyState === FakeWebSocket.CLOSING,
    [sockets.length, s.readyState]);
  const q2 = ext.connect({ HOST: '127.0.0.2', PORT: 14711 });
  const q2same = ext.connect({ HOST: '127.0.0.2', PORT: 14711 });
  check('つながる途中に別の場所へつなぐと、前の［接続］は失敗で終わる', (await settled(q1)) === 'rejected', '');
  check('新しい場所のソケットは 1 つ（同じ場所の 2 つ目は一緒に待つ）', sockets.length === 3, sockets.length);
  const s3 = sockets[2];
  s3.open();
  check('新しい［接続］と、一緒に待った［接続］は成功する', (await settled(q2)) === 'resolved' && (await settled(q2same)) === 'resolved', '');
  s3.reply(featureReply(['stopAll']));
  // 前の 2 つのソケット（つながっていた s と、つながる途中だった 2 つ目）の close・error が、新しい接続を開いたあとに届いても、
  // 新しい接続はそのまま（片づけで、前のソケットの知らせを受ける関数を外してあるため）
  finishPendingCloses();
  check('前のソケットの close・error が遅れて届いても、新しい接続のまま', ext.socket === s3 && ext.connected === true
    && sockets[1].readyState === FakeWebSocket.CLOSED && s.readyState === FakeWebSocket.CLOSED,
    [ext.socket === s3, ext.connected, sockets[1].readyState, s.readyState]);

  console.log('--- ［切断］のあとは送らない');
  stop();
  check('つながっていて受け付ける MOD には送る（くらべ）', stopAllSent(s3) === 1, stopAllSent(s3));
  ext.disconnect();
  stop();
  check('［切断］のあとは送らない', stopAllSent(s3) === 1 && ext.connected === false && ext._canStopAll === false, [stopAllSent(s3), ext._canStopAll]);

  console.log('--- つながる途中に［切断］すると、一緒に待っていた［接続］も失敗で終わる');
  const w1 = ext.connect(local);
  const w2 = ext.connect(local);
  ext.disconnect();
  check('どちらの［接続］も失敗で終わる', (await settled(w1)) === 'rejected' && (await settled(w2)) === 'rejected', '');

  console.log('--- 接続の返事の代わりにエラーが来たら、受け付けないと見る');
  const r1 = ext.connect(local);
  const s4 = sockets[sockets.length - 1];
  s4.open();
  await settled(r1);
  check('返事の前は「わからない」（null）', ext._canStopAll === null, ext._canStopAll);
  s4.reply({ type: 'error', payload: { errorCode: 'SERVER_STOPPING', errorMessage: 'stopping' } });
  stop();
  check('エラーのあとは送らない', ext._canStopAll === false && stopAllSent(s4) === 0, [ext._canStopAll, stopAllSent(s4)]);

  console.log('--- 受け付ける MOD とつないでいて、切れたら送らない');
  const r2 = ext.connect({ HOST: '127.0.0.3', PORT: 14711 });
  const s5 = sockets[sockets.length - 1];
  s5.open();
  await settled(r2);
  s5.reply(featureReply(['stopAll']));
  s5.closeFromServer();
  stop();
  check('切れたあとは送らない', stopAllSent(s5) === 0 && ext._canStopAll === false && ext.connected === false, [stopAllSent(s5), ext._canStopAll]);

  console.log('--- MOD が動いていない（error のあと close）と、一緒に待っていた［接続］も失敗で終わり、次の［接続］はつなぎ直す');
  const before6 = sockets.length;
  const f1 = ext.connect(local);
  const f2 = ext.connect(local);
  sockets[sockets.length - 1].fail();
  check('どちらの［接続］も失敗で終わる', (await settled(f1)) === 'rejected' && (await settled(f2)) === 'rejected', '');
  check('ソケット・つないだ場所・失敗で終わらせる関数は残らない', ext.socket === null && ext._socketUrl === null
    && ext._connectReject === null && ext.connected === false, [ext.socket === null, ext._socketUrl, ext._connectReject === null]);
  const f3 = ext.connect(local);
  check('次の［接続］は新しいソケットを作る', sockets.length === before6 + 2, sockets.length - before6);
  sockets[sockets.length - 1].open();
  check('新しい［接続］は成功する', (await settled(f3)) === 'resolved', '');
  ext.disconnect();

  console.log('--- MOD の側から閉じ始めた間（閉じるあいさつの途中）の［接続］は、つなぎ直す。閉じ終わっても新しい接続はそのまま');
  const c1 = ext.connect(local);
  const old = sockets[sockets.length - 1];
  old.open();
  await settled(c1);
  old.reply(featureReply(['stopAll']));
  old.closingFromServer();
  const beforeClosing = sockets.length;
  const c2 = ext.connect(local);
  check('閉じ始めたソケットは使わず、新しいソケットを作る', sockets.length === beforeClosing + 1 && ext.socket !== old,
    sockets.length - beforeClosing);
  const fresh = sockets[sockets.length - 1];
  old.finishClose();
  fresh.open();
  check('前のソケットが閉じ終わっても、新しい［接続］は成功する', (await settled(c2)) === 'resolved', '');
  fresh.reply(featureReply(['stopAll']));
  stop();
  check('前のソケットが閉じ終わったあとも、新しい接続のまま（止めるボタンは新しい接続に送る）',
    ext.socket === fresh && ext.connected === true && stopAllSent(fresh) === 1 && stopAllSent(old) === 0,
    [ext.socket === fresh, ext.connected, stopAllSent(fresh), stopAllSent(old)]);
  ext.disconnect();

  console.log('--- 接続の返事が success:false なら、受け付けないと見る');
  const n1 = ext.connect({ HOST: '127.0.0.4', PORT: 14711 });
  const s6 = sockets[sockets.length - 1];
  s6.open();
  await settled(n1);
  s6.reply({ type: 'connect_response', payload: { success: false, errorMessage: '試験の失敗' } });
  stop();
  check('success:false の返事のあとは送らない', ext._canStopAll === false && stopAllSent(s6) === 0, [ext._canStopAll, stopAllSent(s6)]);
  ext.disconnect();

  console.log('--- 場所の書き方がおかしく、ソケットを作れないとき');
  const bad = ext.connect({ HOST: 'local host', PORT: 14711 });
  check('［接続］は失敗で終わり、失敗で終わらせる関数も残らない', (await settled(bad)) === 'rejected' && ext._connectReject === null,
    ext._connectReject === null);

  // 最後に、閉じた前のソケットの知らせも全部届けてから、思わぬエラーが出ていないかを見る
  finishPendingCloses();
  await new Promise(r => setTimeout(r, 0));
  const errorTexts = errorLogs.map(e => e.text);
  const firstDiff = (() => {
    for (let i = 0; i < Math.max(errorTexts.length, EXPECTED_ERRORS.length); i++) {
      if (errorTexts[i] !== EXPECTED_ERRORS[i]) return i;
    }
    return -1;
  })();
  check('console.error に出たものは、試験がわざと起こすものだけ（中身・回数・順番も同じ）', firstDiff === -1, {
    最初にちがう番号: firstDiff,
    期待: EXPECTED_ERRORS[firstDiff],
    実際: errorLogs[firstDiff],
    出たもの全部: errorLogs.map(e => '［' + e.section + '］' + e.text)
  });

  console.log('結果: ' + passed + ' 件成功 / ' + failed + ' 件失敗');
  process.exit(failed ? 1 : 0);
})().catch(e => {
  console.log('NG     例外: ' + (e && e.stack));
  process.exit(1);
});

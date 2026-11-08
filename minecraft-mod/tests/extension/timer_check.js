// Scratch のバンドルから、時計（Timer）・送る速さの制限（RateLimiter）・Runtime の一時停止の部分・見た目のブロックの「N 秒言う／考える」を切り出し、
// 偽物の Date.now と setTimeout の上で、一時停止ボタンで止めた・再開した・止めるボタンで解けたときの動きを確かめる
// （止めている間は VM の時計が進まない・再開しても時計が戻らない・送る速さの制限は本当の時計で数える・音は止めたまま・
//   吹き出しは止めている間は消えない）
// 使い方: node timer_check.js <バンドルのファイル>（ふだんは同じフォルダの run.sh が 4 つとも流す）
'use strict';
const fs = require('fs');
const vm = require('vm');

const file = process.argv[2];
const text = fs.readFileSync(file, 'utf8').replace(/\r\n/g, '\n');

// 「始まりの印」から「終わりの印」の前までを切り出す（見つからなければ、何が無いかを出して止める）。
// limit を渡すと、そこより後ろは探さない（同じ名前のメソッドを、ほかのクラスから拾わないように）
const slice = (from, startMark, endMark, name, limit) => {
  const start = from < 0 ? -1 : text.indexOf(startMark, from);
  const end = start < 0 ? -1 : text.indexOf(endMark, start);
  if (start < 0 || end < 0 || (limit !== undefined && (limit < 0 || end > limit))) {
    console.log('NG     ' + name + ' が見つからない: ' + file);
    process.exit(2);
  }
  return text.slice(start, end);
};

// 時計のモジュール（class Timer から module.exports の前まで）
const timerModule = text.indexOf('!*** ../scratch-vm-official/src/util/timer.js ***!');
const timerSrc = slice(timerModule, 'class Timer {', 'module.exports = Timer;', 'Timer');
// 送る速さの制限（class RateLimiter から module.exports の前まで）
const limiterModule = text.indexOf('!*** ../scratch-vm-official/src/util/rateLimiter.js ***!');
const limiterSrc = slice(limiterModule, 'class RateLimiter {', 'module.exports = RateLimiter;', 'RateLimiter');
// Runtime の一時停止のメソッドと、止めるボタン（stopAll）・終わらせる（quit）（「  名前() {」から、2 つ下げの「  }」まで）
const runtimeStart = text.indexOf('class Runtime extends EventEmitter {');
const runtimeEnd = runtimeStart < 0 ? -1 : text.indexOf('module.exports = Runtime;', runtimeStart);
const method = name => slice(runtimeStart, '\n  ' + name + '() {', '\n  }\n', 'Runtime.' + name, runtimeEnd) + '\n  }\n';
// Runtime の本物の作り（constructor。一時停止の欄の初期値を見る）
const runtimeConstructor = method('constructor');
const runtimeMethods = ['updateCurrentMSecs', 'isPaused', 'pause', 'resume', '_mutePausedAudio', '_endPause', 'stopAll', 'quit']
  .map(method).join('');
// 見た目のブロックの作り（constructor）と「N 秒言う／考える」・吹き出しを消す予約のメソッド（引数があるものも、「  名前(」から 2 つ下げの「  }」まで）
const looksStart = text.indexOf('class Scratch3LooksBlocks {');
const looksEnd = looksStart < 0 ? -1 : text.indexOf('module.exports = Scratch3LooksBlocks;', looksStart);
const looksMethod = name => slice(looksStart, '\n  ' + name + '(', '\n  }\n', 'Scratch3LooksBlocks.' + name, looksEnd) + '\n  }\n';
const looksMethods = ['constructor', 'sayforsecs', 'thinkforsecs', '_onResetBubbles', '_startBubbleTimer', '_armBubbleTimer',
  '_cancelBubbleTimer', '_onRuntimePausedForBubbles', '_onRuntimeResumedForBubbles'].map(looksMethod).join('');

// 偽物の Date（now を試験が決める）
let fakeNow = 1000;
const FakeDate = { now: () => fakeNow };
// 偽物の setTimeout（fakeNow で時間を進めたときに、時間が来たものを呼ぶ）
let fakeTimers = [];
let fakeTimerId = 0;
const fakeSetTimeout = (fn, ms) => { fakeTimerId++; fakeTimers.push({ id: fakeTimerId, fn, at: fakeNow + Number(ms) }); return fakeTimerId; };
const fakeClearTimeout = id => { fakeTimers = fakeTimers.filter(t => t.id !== id); };
// t まで時間を進め、そのあいだに時間が来た予約を、時間の順に呼ぶ
const advanceTo = t => {
  for (;;) {
    const due = fakeTimers.filter(x => x.at <= t).sort((a, b) => a.at - b.at)[0];
    if (!due) break;
    fakeNow = Math.max(fakeNow, due.at);
    fakeTimers = fakeTimers.filter(x => x !== due);
    due.fn();
  }
  fakeNow = t;
};
const ctx = {
  Date: FakeDate, Math, Promise, Set, Object, setTimeout: fakeSetTimeout, clearTimeout: fakeClearTimeout, clearInterval: () => {},
  // 数にできない秒は 0 秒（Scratch の Cast.toNumber と同じ）
  Cast: { toNumber: v => { const n = Number(v); return Number.isNaN(n) ? 0 : n; } },
  __webpack_require__: { g: { setTimeout, clearTimeout } }
};
vm.createContext(ctx);
const Timer = vm.runInContext('(() => {\n' + timerSrc + '\nreturn Timer;\n})()', ctx);
ctx.Timer = Timer;
const RateLimiter = vm.runInContext('(() => {\n' + limiterSrc + '\nreturn RateLimiter;\n})()', ctx);
// Runtime の代わり（一時停止・stopAll・quit は本物。出した知らせを順に覚え、聞き手に届ける）
const FakeRuntime = vm.runInContext(`(class FakeRuntime {
  constructor(audioEngine) {
    this._paused = false; this._pausedAt = 0;
    this._pausedAudioContext = null; this._onPausedAudioStateChange = null; this._pausedGain = null;
    this.events = []; this.audioEngine = audioEngine; this.listeners = {};
    this.targets = []; this.threads = []; this.sequencer = { activeThread: null }; this._steppingInterval = null;
  }
  on(name, fn) { (this.listeners[name] = this.listeners[name] || []).push(fn); }
  static get RUNTIME_PAUSED() { return 'RUNTIME_PAUSED'; }
  static get RUNTIME_RESUMING() { return 'RUNTIME_RESUMING'; }
  static get RUNTIME_RESUMED() { return 'RUNTIME_RESUMED'; }
  static get PROJECT_STOP_ALL() { return 'PROJECT_STOP_ALL'; }
  emit(name) { this.events.push(name); (this.listeners[name] || []).forEach(fn => fn()); }
  resetRunId() {}
  _stopThread() {}
${runtimeMethods}})`, ctx);
ctx.Runtime = FakeRuntime;
// 見た目のブロック（作り・「N 秒言う／考える」・予約は本物。吹き出しは target の id ごとに文と usageId を覚える偽物）
const Looks = vm.runInContext(`(class Scratch3LooksBlocks {
  static get SAY_OR_THINK() { return 'SAY'; }
  say(args, util) { this._updateBubble(util.target, 'say', args.MESSAGE); }
  think(args, util) { this._updateBubble(util.target, 'think', args.MESSAGE); }
  _updateBubble(target, type, text) {
    this.bubbles = this.bubbles || {}; this.usage = (this.usage || 0) + 1;
    this.bubbles[target.id] = { text, type, usageId: this.usage };
  }
  _getBubbleState(target) {
    this.bubbles = this.bubbles || {};
    return this.bubbles[target.id] || (this.bubbles[target.id] = { text: '', type: 'say', usageId: null });
  }
  _onTargetChanged() {}
  _onTargetWillExit() {}
${looksMethods}})`, ctx);

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

// 偽物の AudioContext（suspend・resume を数え、statechange の聞き手を覚える）と、作品の音の入り口（inputNode の音量）
const audioContext = {
  state: 'running', suspends: 0, resumes: 0, listeners: [],
  suspend() { this.suspends++; this.state = 'suspended'; return Promise.resolve(); },
  resume() { this.resumes++; this.state = 'running'; return Promise.resolve(); },
  addEventListener(ev, fn) { if (ev === 'statechange') this.listeners.push(fn); },
  removeEventListener(ev, fn) { if (ev === 'statechange') this.listeners = this.listeners.filter(f => f !== fn); },
  // ほかの所（効果音の一覧を開いたときの StartAudioContext など）が動かした
  resumedByOthers() { this.state = 'running'; this.listeners.slice().forEach(fn => fn()); }
};
const inputNode = { gain: { value: 1 } };

const rt = new FakeRuntime({ audioContext, inputNode });
const clock = () => Timer.PAUSABLE_DATE.now();
// 時計を読んだ値（どの時点でも戻らないことを、最後にまとめて見る）
const samples = [];
const sample = () => { samples.push(clock()); };
// 何回か送れるだけ送る（送れた回数を返す）
const drain = limiter => { let n = 0; while (limiter.okayToSend() && n < 100) n++; return n; };

console.log('--- Runtime の本物の作りに、一時停止の欄の初期値がある（無いと、音量 0 が黙って効かず、再開で例外になる）');
const initLines = ['this._paused = false;', 'this._pausedAt = 0;', 'this._pausedAudioContext = null;',
  'this._onPausedAudioStateChange = null;', 'this._pausedGain = null;'];
const missingInit = initLines.filter(line => !runtimeConstructor.includes(line));
check('Runtime の constructor が、一時停止の 5 つの欄を用意する', missingInit.length === 0, missingInit);

console.log('--- 止めていないときは、VM の時計も Timer も本当の時計と同じに進む');
const t = new Timer();
t.start();
fakeNow = 2000;
sample();
check('止めていないときは、経った時間をそのまま数える', t.timeElapsed() === 1000 && clock() === 2000, [t.timeElapsed(), clock()]);

console.log('--- 止めている間は、VM の時計も Timer も進まない。音は止め、ほかの所が動かしたら作品の音だけ音量 0');
rt.pause();
fakeNow = 5000;
sample();
rt.updateCurrentMSecs();
check('止めている間は、時計が止めた時刻のまま（「〜秒待つ」・「すべる」・タイマーが進まない）',
  t.timeElapsed() === 1000 && clock() === 2000 && rt.currentMSecs === 2000 && rt.isPaused() === true,
  [t.timeElapsed(), clock(), rt.currentMSecs, rt.isPaused()]);
check('止めると、AudioContext を止める（音量はそのまま）', audioContext.suspends === 1 && audioContext.state === 'suspended' && inputNode.gain.value === 1,
  [audioContext.suspends, audioContext.state, inputNode.gain.value]);
audioContext.resumedByOthers();
check('止めている間に、ほかの所が AudioContext を動かしたら、止め直さずに作品の音だけ音量 0（毎フレーム動かし直すのとぶつからない）',
  audioContext.suspends === 1 && audioContext.state === 'running' && inputNode.gain.value === 0,
  [audioContext.suspends, audioContext.state, inputNode.gain.value]);
audioContext.resumedByOthers();
check('何度動かされても、もとの音量（1）を覚えたまま', inputNode.gain.value === 0 && rt._pausedGain === 1, [inputNode.gain.value, rt._pausedGain]);
rt.pause();
check('止めている間にもう一度止めても、何も変わらない', rt.events.join() === 'RUNTIME_PAUSED' && rt._pausedAt === 2000 && audioContext.suspends === 1,
  [rt.events, rt._pausedAt, audioContext.suspends]);

console.log('--- 送る速さの制限（LEGO・micro:bit など）は、止めている間も本当の時計で数える');
const limiter = new RateLimiter(10);
const sentWhilePaused = drain(limiter);
fakeNow = 6000;
check('止めている間でも、1 秒たてば、また送れる（止まった時計で、いつまでも送れなくならない）',
  sentWhilePaused === 10 && limiter.okayToSend() === true, sentWhilePaused);

console.log('--- 再開すると、止めていた時間を引いて続きから進む（時計は戻らない）');
fakeNow = 7000;
sample();
rt.resume();
sample();
check('再開した直後の時計は、止めた時刻と同じ（戻らない・飛ばない）', clock() === 2000 && t.timeElapsed() === 1000, [clock(), t.timeElapsed()]);
fakeNow = 7500;
sample();
rt.updateCurrentMSecs();
check('再開したあとは、止めていた 5 秒を数えずに進む', t.timeElapsed() === 1500 && rt.currentMSecs === 2500, [t.timeElapsed(), rt.currentMSecs]);
check('知らせは、止めた・再開する前・再開した、の順', rt.events.join() === 'RUNTIME_PAUSED,RUNTIME_RESUMING,RUNTIME_RESUMED', rt.events);
check('再開すると、AudioContext を動かし、聞き手を外し、作品の音の音量を戻す',
  audioContext.resumes === 1 && audioContext.state === 'running' && audioContext.listeners.length === 0
  && inputNode.gain.value === 1 && rt._pausedGain === null,
  [audioContext.resumes, audioContext.state, audioContext.listeners.length, inputNode.gain.value, rt._pausedGain]);
audioContext.resumedByOthers();
check('再開したあとは、ほかの所が動かしても音量を変えない', inputNode.gain.value === 1 && audioContext.suspends === 1,
  [inputNode.gain.value, audioContext.suspends]);
rt.resume();
check('止めていないときに再開しても、何も変わらない', rt.events.length === 3 && clock() === 2500, [rt.events, clock()]);
const sentAfter = drain(limiter);
fakeNow = 8500;
check('再開したあとも、送る速さの制限は本当の時計で数える（1 秒で、また送れる）', sentAfter === 10 && limiter.okayToSend() === true, sentAfter);

console.log('--- 聞き手を付けられない AudioContext（addEventListener が無い）でも止める・再開できる');
const plainContext = {
  state: 'running', suspends: 0, resumes: 0,
  suspend() { this.suspends++; this.state = 'suspended'; return Promise.resolve(); },
  resume() { this.resumes++; this.state = 'running'; return Promise.resolve(); }
};
const rtPlain = new FakeRuntime({ audioContext: plainContext, inputNode: { gain: { value: 1 } } });
fakeNow = 8600;
rtPlain.pause();
fakeNow = 8700;
rtPlain.resume();
check('addEventListener が無くても、止めて・再開して、聞き手の欄は空のまま',
  plainContext.suspends === 1 && plainContext.resumes === 1 && rtPlain._pausedAudioContext === null && Timer.pausedSince === null,
  [plainContext.suspends, plainContext.resumes, rtPlain._pausedAudioContext, Timer.pausedSince]);

console.log('--- 音の出口の状態はあとから変わる: 止まり終わる前に running の知らせが来ても、もとの音量を覚えたまま戻す');
// suspend はすぐ終わらず、finishSuspend で終わる（そのとき suspended の知らせを出す）。fire で、いまの状態の知らせを出す
const slowContext = {
  state: 'running', resumes: 0, listeners: [], finishSuspend: null,
  suspend() { return new Promise(res => { this.finishSuspend = () => { this.state = 'suspended'; this.fire(); res(); }; }); },
  resume() { this.resumes++; this.state = 'running'; this.fire(); return Promise.resolve(); },
  addEventListener(ev, fn) { if (ev === 'statechange') this.listeners.push(fn); },
  removeEventListener(ev, fn) { if (ev === 'statechange') this.listeners = this.listeners.filter(f => f !== fn); },
  fire() { this.listeners.slice().forEach(fn => fn()); }
};
const slowInput = { gain: { value: 0.8 } };
const rtSlow = new FakeRuntime({ audioContext: slowContext, inputNode: slowInput });
fakeNow = 8800;
rtSlow.pause();
// 止める前の resume の知らせが遅れて届いた（まだ running）
slowContext.fire();
const mutedEarly = slowInput.gain.value;
slowContext.finishSuspend();
check('止まり終わる前の running の知らせで音量 0 にし、止まり終わっても、もとの音量（0.8）を覚えたまま',
  mutedEarly === 0 && slowInput.gain.value === 0 && rtSlow._pausedGain === 0.8 && slowContext.state === 'suspended',
  [mutedEarly, slowInput.gain.value, rtSlow._pausedGain, slowContext.state]);
fakeNow = 8900;
rtSlow.resume();
check('再開すると、もとの音量（0.8）に戻し、聞き手を外してから動かす',
  slowInput.gain.value === 0.8 && rtSlow._pausedGain === null && slowContext.listeners.length === 0 && slowContext.state === 'running',
  [slowInput.gain.value, rtSlow._pausedGain, slowContext.listeners.length, slowContext.state]);
// 止める前から止まっていた（ページを開いて、まだ何も押していない）
const sleepingContext = {
  state: 'suspended', suspends: 0, listeners: [],
  suspend() { this.suspends++; return Promise.resolve(); },
  resume() { this.state = 'running'; this.listeners.slice().forEach(fn => fn()); return Promise.resolve(); },
  addEventListener(ev, fn) { if (ev === 'statechange') this.listeners.push(fn); },
  removeEventListener(ev, fn) { if (ev === 'statechange') this.listeners = this.listeners.filter(f => f !== fn); }
};
const sleepingInput = { gain: { value: 1 } };
const rtSleeping = new FakeRuntime({ audioContext: sleepingContext, inputNode: sleepingInput });
fakeNow = 8950;
rtSleeping.pause();
// 止めている間に、ほかの所（StartAudioContext）が初めて動かした
sleepingContext.state = 'running';
sleepingContext.listeners.slice().forEach(fn => fn());
const sleepingMuted = sleepingInput.gain.value;
fakeNow = 8990;
rtSleeping.resume();
check('止める前から止まっていても、止めている間にほかの所が動かしたら音量 0、再開で戻す',
  sleepingContext.suspends === 1 && sleepingMuted === 0 && sleepingInput.gain.value === 1 && sleepingContext.listeners.length === 0,
  [sleepingContext.suspends, sleepingMuted, sleepingInput.gain.value, sleepingContext.listeners.length]);

console.log('--- 止めるボタン（緑の旗も。本物の stopAll）で解けたときも、止めていた時間を引いて続きから進む');
rt.events.length = 0;
fakeNow = 9000;
sample();
const clockAtPause = clock();
rt.pause();
audioContext.resumedByOthers();
fakeNow = 12000;
sample();
rt.stopAll();
sample();
fakeNow = 12500;
sample();
check('止めるボタンは、再開した（RESUMING は出さない）を出してから止める知らせを出す',
  rt.events.join() === 'RUNTIME_PAUSED,RUNTIME_RESUMED,PROJECT_STOP_ALL', rt.events);
check('止めるボタンで解いたあとも、時計は戻らず、止めていた 3 秒を数えない。音量も戻る',
  clock() === clockAtPause + 500 && Timer.pausedSince === null && rt.isPaused() === false && inputNode.gain.value === 1,
  [clock() - clockAtPause, Timer.pausedSince, rt.isPaused(), inputNode.gain.value]);

console.log('--- 止めたまま終わらせても（quit）、止めた時計・聞き手を残さない');
fakeNow = 13000;
rt.pause();
fakeNow = 14000;
rt.quit();
check('止めたまま終わらせると、一時停止を解く', rt.isPaused() === false && Timer.pausedSince === null && audioContext.listeners.length === 0,
  [rt.isPaused(), Timer.pausedSince, audioContext.listeners.length]);
check('時計は、どの時点でも戻らない', samples.every((v, i) => i === 0 || v >= samples[i - 1]), samples);

(async () => {
  console.log('--- 「N 秒言う／考える」: 止めている間は数えず、スクリプトが止められても時間がたてば吹き出しを消す');
  const flush = () => new Promise(r => setImmediate(r));
  const rt2 = new FakeRuntime(null);
  const looks = new Looks(rt2);
  const cat = { id: 'cat' };
  const util = { target: cat };
  const bubble = () => looks._getBubbleState(cat).text;
  fakeTimers = [];
  fakeNow = 20000;
  let done = false;
  looks.sayforsecs({ MESSAGE: 'Hi', SECS: 2 }, util).then(() => { done = true; });
  advanceTo(21000);
  rt2.pause();
  advanceTo(30000);
  await flush();
  check('止めている間は、時間がたっても吹き出しは消えず、ブロックも終わらない', bubble() === 'Hi' && done === false, [bubble(), done]);
  rt2.resume();
  advanceTo(30900);
  await flush();
  const before = [bubble(), done];
  advanceTo(31000);
  await flush();
  check('再開したら、残りの 1 秒で吹き出しが消えてブロックが終わる', before[0] === 'Hi' && before[1] === false && bubble() === '' && done === true,
    [before, bubble(), done]);

  // 「N 秒考える」も同じ（止めている間は消えず、再開後の残りで消える）
  fakeNow = 32000;
  let thinkDone = false;
  looks.thinkforsecs({ MESSAGE: 'Hmm', SECS: 1 }, util).then(() => { thinkDone = true; });
  advanceTo(32500);
  rt2.pause();
  advanceTo(36000);
  await flush();
  const thinkPaused = [bubble(), looks._getBubbleState(cat).type, thinkDone];
  rt2.resume();
  advanceTo(36500);
  await flush();
  check('「N 秒考える」も、止めている間は消えず、再開後の残りの 0.5 秒で消える',
    thinkPaused[0] === 'Hmm' && thinkPaused[1] === 'think' && thinkPaused[2] === false && bubble() === '' && thinkDone === true,
    [thinkPaused, bubble(), thinkDone]);

  // スクリプトが止められた（「スプライトの他のスクリプトを止める」など。返した Promise はもう待たれない）
  fakeNow = 40000;
  looks.sayforsecs({ MESSAGE: 'Bye', SECS: 2 }, util);
  advanceTo(42000);
  check('スクリプトが止められても、2 秒たてば吹き出しを消す（元の Scratch と同じ）', bubble() === '' && looks._bubbleTimers.size === 0,
    [bubble(), looks._bubbleTimers.size]);

  // 止めている間に言い始めたら、再開してから数え始める
  fakeNow = 60000;
  rt2.pause();
  let donePausedStart = false;
  looks.sayforsecs({ MESSAGE: 'Wait', SECS: '1.5' }, util).then(() => { donePausedStart = true; });
  advanceTo(65000);
  rt2.resume();
  advanceTo(66400);
  await flush();
  const mid = [bubble(), donePausedStart];
  advanceTo(66500);
  await flush();
  check('止めている間に言い始めたら、再開してから 1.5 秒で消える', mid[0] === 'Wait' && mid[1] === false && bubble() === '' && donePausedStart === true,
    [mid, bubble(), donePausedStart]);

  // 止めたまま止めるボタン（本物の stopAll）: 再開の知らせで全部の予約を張り直し、止める知らせで最後の予約だけやめる（元の Scratch と同じ）
  const dog = { id: 'dog' };
  fakeNow = 70000;
  let firstDone = false;
  let lastDone = false;
  looks.sayforsecs({ MESSAGE: 'First', SECS: 2 }, { target: dog }).then(() => { firstDone = true; });
  looks.sayforsecs({ MESSAGE: 'Last', SECS: 3 }, util).then(() => { lastDone = true; });
  advanceTo(70500);
  rt2.pause();
  advanceTo(75000);
  rt2.stopAll();
  const armed = [...looks._bubbleTimers].map(x => x.handle !== null);
  advanceTo(76500);
  await flush();
  check('止めたまま止めるボタンを押すと、最後の予約だけやめ、ほかの予約は残りの時間で張り直す',
    Timer.pausedSince === null && armed.length === 1 && armed[0] === true && firstDone === true && lastDone === false
    && looks._bubbleTimers.size === 0 && fakeTimers.length === 0,
    [Timer.pausedSince, armed, firstDone, lastDone, looks._bubbleTimers.size, fakeTimers.length]);

  console.log('結果: ' + passed + ' 件成功 / ' + failed + ' 件失敗');
  process.exit(failed ? 1 : 0);
})().catch(e => {
  console.log('NG     例外: ' + (e && e.stack));
  process.exit(1);
});

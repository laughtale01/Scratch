# 止めるボタン（stopAll）と、接続が切れたときに、その接続のまだ置いていない命令が列から外れるかと、
# 一時停止（pauseQueue・resumeQueue）でその接続の命令が止まり、再開したら続きから置くかを、本物の MOD で確かめる
# 試し用の、ふつうの地形のワールドで、F3+P で「フォーカスを失ったら止める」を切ってから流す（授業で使うワールドでは流さない）。
# 空の上（y=200〜）の、まだ作られていない遠くの場所を使う（「埋める」で置いた石は最後に空気に戻す。
# 「周りをきれいにする」は遠くの場所を平らにする。8 節は最後まで平らにする）
# 7 節は、2 つのタブの命令が届いた順に置かれるか。8 節は、「周りをきれいにする」を止めてすぐ流し直して最後まで終わるか。
# 9 節は、一時停止（pauseQueue・resumeQueue）。10 節は、大きな「埋める」を区画の途中で、「周りをきれいにする」を途中で一時停止して再開する。
# 35 秒止めるところが 2 つあるので、全部で 5〜7 分かかる
# 使い方（minecraft-mod で）: python3 -I tests/websocket/stop_check.py --test-world
# （--test-world は「試し用のワールドを開いている」の確かめ。開いたときのログにはワールドの名前が出ず、Esc のあとに出る表示名も
#   ほかのワールドと同じことがあるので、ログでは見分けられない。流す人に付けてもらう。付けなければ、何も送らずに止まる）
# --self-check を足すと、引数を読んで、共通の部品（probe_fill.py の前半）を読み込めるかだけ確かめて止まる（何も送らない。
#   Minecraft の設定も読まない。tests/websocket/run.sh が流す）
import os, sys, time
args = sys.argv[1:]
if '--test-world' not in args:
    sys.exit('試し用のワールド（授業で使うワールドではない）を開いてから、--test-world を付けて流す'
             '（遠くの空の上と地面を書き換え、流すたびに新しいチャンクが作られてセーブが大きくなるため）')
SELF_CHECK = '--self-check' in args
# 共通の部品（probe_fill.py）は sys.argv[1] をポート番号として読むので、この台本の印は取り除いてから読み込む
sys.argv = [sys.argv[0]] + [a for a in args if a not in ('--test-world', '--self-check')]
REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
LOG = os.path.expanduser('~/Library/Application Support/minecraft/logs/latest.log')
ns = {}
# probe_fill.py の前半（関数と定数だけ）を読む。目印は、行の頭にある「CASES = [」（THROTTLE_CASES などの途中では切らない）。
# 目印が無ければ止まる（無いまま読むと、probe_fill.py の本体まで動いて Minecraft に送り始めるため。--self-check でも何も送らないように）
probe_src = open(os.path.join(REPO, 'tests/websocket/probe_fill.py'), encoding='utf-8').read()
probe_head, marker, _ = probe_src.partition('\nCASES = [')
if not marker:
    sys.exit('probe_fill.py の行の頭に目印の「CASES = [」が無いので止める（前半だけを読めないため）')
exec(probe_head, ns)
connect, request, send_only, collect, wait_queue = (ns[k] for k in ('connect', 'request', 'send_only', 'collect', 'wait_queue'))
if SELF_CHECK:
    print('stop_check.py: 引数と共通の部品の読み込み OK（ポート %d）' % ns['PORT'])
    sys.exit(0)
if 'pauseOnLostFocus:true' in open(os.path.expanduser('~/Library/Application Support/minecraft/options.txt'), encoding='utf-8').read():
    sys.exit('F3+P で「フォーカスを失ったら止める」を切ってから流す（options.txt が pauseOnLostFocus:true）')
results = []


def check(name, ok, detail=''):
    results.append(ok)
    print(('  ok     ' if ok else '  NG     ') + name + ('' if ok else '  → %r' % (detail,)), flush=True)


def emptied_within(s, t0, limit):
    """
    t0（time.monotonic。止める・閉じる前に測る）から limit 秒以内に列が空いたか
    （止める命令の返事を待った時間と、列を問い合わせた時間も入れる。wait_queue は問い合わせる前にしか期限を見ないため）
    """
    left = limit - (time.monotonic() - t0)
    return left > 0 and wait_queue(s, left) and time.monotonic() - t0 <= limit


def log_since(start):
    return open(LOG, 'rb').read()[start:].decode('utf-8', 'replace')


def wait_log(start, text, limit):
    """
    ログの start から先に text が出るまで、limit 秒まで待つ（チャットは、Minecraft の画面の側が受け取ってからログに出るので、
    列が空いた直後には、まだ出ていないことがある）。出たら True
    """
    deadline = time.monotonic() + limit
    while True:
        if text in log_since(start):
            return True
        if time.monotonic() >= deadline:
            return False
        time.sleep(0.5)


def open_conn(name):
    s = connect()
    hello = request(s, 'connect', {'clientId': name})
    return s, ((hello.get('payload') or {}).get('serverInfo') or {})


def cmd(s, action, params):
    return request(s, 'command', {'action': action, 'params': params}).get('payload') or {}


def block_type(s, pos):
    """
    pos のブロックの名前（読めなければ、MOD の失敗の返事を入れた 'None: …'）。チャンクを読み込み中のときは、読む命令が約 4 秒で
    「読み込みの途中」を返すので、読めるまで 5 回まで読み直す（1 回 4 秒ほどと、あいだに 2 秒。合わせて 30 秒ほどまで）
    """
    r = {}
    for attempt in range(5):
        r = cmd(s, 'getBlockType', pos)
        if r.get('success') is True:
            return str((r.get('result') or {}).get('blockType'))
        if attempt < 4:
            time.sleep(2)
    return 'None: %r' % (r,)


def is_air(name):
    return name in ('air', 'minecraft:air')


DEBUG_LOG = os.path.join(os.path.dirname(LOG), 'debug.log')


def debug_size():
    return os.path.getsize(DEBUG_LOG) if os.path.exists(DEBUG_LOG) else 0


def debug_since(start):
    """debug.log（latest.log には出ない DEBUG の行も入る）の start から先"""
    if not os.path.exists(DEBUG_LOG):
        return ''
    with open(DEBUG_LOG, 'rb') as f:
        f.seek(start)
        return f.read().decode('utf-8', 'replace')


def gave_up_since(log_pos, debug_pos):
    """
    読み込みをあきらめたログ（latest.log のチャット「まにあわなかった」と、debug.log の「gave up」。
    同じ失敗が 60 秒の間に続くと latest.log には出ないので、debug.log も見る）
    """
    lines = [l for l in log_since(log_pos).splitlines() if 'まにあわなかった' in l or 'gave up' in l]
    return lines + [l for l in debug_since(debug_pos).splitlines() if 'gave up' in l]


def far_line(x0, z0, n):
    return [{'action': 'setBlock', 'params': {'x': x0 + i * 16, 'y': 200, 'z': z0, 'blockType': 'air'}} for i in range(n)]


def send_all(s, commands):
    ids = [send_only(s, 'command', c) for c in commands]
    return collect(s, ids)


log_start = os.path.getsize(LOG)
debug_start = debug_size()
base = 3_000_000 + int(time.time()) % 1000 * 20000

print('--- 1. 接続の返事')
s1, info = open_conn('g1_a')
check('serverInfo.features に stopAll がある', 'stopAll' in (info.get('features') or []), info)
check('serverInfo.features に pauseQueue がある', 'pauseQueue' in (info.get('features') or []), info)
assert wait_queue(s1, 120), '列が空かなかった'

print('--- 2. 遠くへ 600 個送ってすぐ止める')
send_all(s1, far_line(base, base, 600))
t0 = time.monotonic()
r = cmd(s1, 'stopAll', {})
cancelled = (r.get('result') or {}).get('cancelled', -1)
check('stopAll は成功し、400 個より多くやめる', r.get('success') is True and cancelled > 400, r)
check('止めたあと、列は 5 秒以内に空く', emptied_within(s1, t0, 5), time.monotonic() - t0)

print('--- 3. ほかのタブの命令は残り、そのタブを閉じたら空く')
s2, _ = open_conn('g1_b')
send_all(s1, far_line(base, base + 2000, 300))
r = cmd(s2, 'stopAll', {})
check('別の接続の stopAll は、ほかの接続の命令をやめない（0 個）',
      r.get('success') is True and (r.get('result') or {}).get('cancelled') == 0, r)
r = cmd(s2, 'getTargetBlock', {})
check('まだ列に残っている（並んで読めず queued: False）', (r.get('result') or {}).get('queued') is False, r)
t0 = time.monotonic()
s1.close()
check('命令を送ったタブを閉じたら、列は 5 秒以内に空く', emptied_within(s2, t0, 5), time.monotonic() - t0)

print('--- 4. 大きな「埋める」を途中でやめる')
fx, fz = base + 40000, base + 40000
box = {'from': {'x': fx, 'y': 200, 'z': fz}, 'to': {'x': fx + 255, 'y': 229, 'z': fz + 255}}
r = cmd(s2, 'fillBlocks', dict(box, blockType='stone'))
check('大きな「埋める」を受け付ける', r.get('success') is True, r)
time.sleep(2)
t0 = time.monotonic()
r = cmd(s2, 'stopAll', {})
check('途中の「埋める」をやめる（1 個）', (r.get('result') or {}).get('cancelled') == 1, r)
check('やめたあと、列は 5 秒以内に空く', emptied_within(s2, t0, 5), time.monotonic() - t0)
# 最後のマスのチャンクは、やめたのでまだ作られていない。読む命令は約 4 秒で「読み込みの途中」を返すので、読めるまで読み直す
for _ in range(10):
    r = cmd(s2, 'getBlockType', {'x': fx + 255, 'y': 229, 'z': fz + 255})
    if r.get('success') is True:
        break
    time.sleep(2)
check('最後のマスは石にならない', (r.get('result') or {}).get('blockType') in ('air', 'minecraft:air'), r)
# 片づけ（置いたところまでの石を空気に戻す。空気だけの区画は飛ばすので、石のあるところだけ時間がかかる）
r = cmd(s2, 'fillBlocks', dict(box, blockType='air'))
check('片づけの「埋める」（空気）を受け付ける', r.get('success') is True, r)
check('片づけが 120 秒以内に終わる', wait_queue(s2, 120))
r = cmd(s2, 'getBlockType', {'x': fx, 'y': 200, 'z': fz})
check('片づけで、最初のマスは空気に戻った', (r.get('result') or {}).get('blockType') in ('air', 'minecraft:air'), r)

print('--- 5. 「周りをきれいにする」をやめたら、すぐ次を受け付ける')
cx, cz = base + 80000, base + 80000
r = cmd(s2, 'clearArea', {'centerX': cx, 'centerZ': cz})
check('「周りをきれいにする」を受け付ける', r.get('success') is True, r)
r = cmd(s2, 'stopAll', {})
check('「周りをきれいにする」をやめる（1 個）', (r.get('result') or {}).get('cancelled') == 1, r)
r = cmd(s2, 'clearArea', {'centerX': cx, 'centerZ': cz})
check('やめたすぐあとの「周りをきれいにする」を断らない', r.get('success') is True, r)
t0 = time.monotonic()
r = cmd(s2, 'stopAll', {})
check('それもやめる（1 個）', (r.get('result') or {}).get('cancelled') == 1, r)
check('列は 5 秒以内に空く', emptied_within(s2, t0, 5), time.monotonic() - t0)

print('--- 6. 何も無いときに止める')
time.sleep(3.5)  # 知らせの間引き（3 秒）をあける（「0 こ」の知らせが出てしまう壊れ方を、間引きで隠さないように）
zero_start = os.path.getsize(LOG)
r = cmd(s2, 'stopAll', {})
check('何も無ければ 0 個', r.get('success') is True and (r.get('result') or {}).get('cancelled') == 0, r)
# 知らせは、やめた命令が 1 個以上のときだけ出す（チャットはログに出るまで少しかかるので、2 秒まで待って出ないことを見る）
check('0 個のときは、チャットに知らせを出さない', not wait_log(zero_start, 'めいれい 0 こ', 2),
      [l.split('[CHAT] ', 1)[1] for l in log_since(zero_start).splitlines() if '[CHAT] ' in l][-3:])

print('--- 7. 2 つのタブの命令は、届いた順に置かれる（接続ごとの列にしても、順番は 1 本の列と同じ）')
a, _ = open_conn('g1_c')
lx, lz = base + 120000, base + 120000
first = {'x': lx, 'y': 200, 'z': lz}
last = {'x': lx + 59 * 16, 'y': 200, 'z': lz}
beyond = {'x': lx + 60 * 16, 'y': 200, 'z': lz}
# B が先に beyond を金にし、A があとから石にする → 石。A が last まで石を並べ、B があとから last を金にする → 金
r = cmd(s2, 'setBlock', dict(beyond, blockType='gold_block'))
check('B の金（先）を受け付ける', r.get('success') is True, r)
send_all(a, [{'action': 'setBlock', 'params': {'x': lx + i * 16, 'y': 200, 'z': lz, 'blockType': 'stone'}} for i in range(61)])
r = cmd(s2, 'setBlock', dict(last, blockType='gold_block'))
check('B の金（あと）を受け付ける', r.get('success') is True, r)
check('列は 120 秒以内に空く', wait_queue(s2, 120))
def block_at(pos):
    return str((cmd(s2, 'getBlockType', pos).get('result') or {}).get('blockType'))
check('A のあとに送った B の金が残る（last は金）', block_at(last).endswith('gold_block'), block_at(last))
check('B のあとに送った A の石が残る（beyond は石）', block_at(beyond).endswith('stone'), block_at(beyond))
check('A の最初の石も置かれた', block_at(first).endswith('stone'), block_at(first))
# 片づけ
send_all(a, [{'action': 'setBlock', 'params': {'x': lx + i * 16, 'y': 200, 'z': lz, 'blockType': 'air'}} for i in range(61)])
check('片づけの列が 120 秒以内に空く', wait_queue(a, 120))
check('片づけで last は空気に戻った', block_at(last) in ('air', 'minecraft:air'), block_at(last))
a.close()
time.sleep(1)
s2.close()
time.sleep(1)

print('--- 8. 「周りをきれいにする」を途中で止め、すぐ流し直すと、最後まで終わる')
c, _ = open_conn('stop_check_clear')
qx, qz = base + 160000, base + 160000
r = cmd(c, 'clearArea', {'centerX': qx, 'centerZ': qz})
check('「周りをきれいにする」を受け付ける', r.get('success') is True, r)
time.sleep(3)
r = cmd(c, 'stopAll', {})
check('途中でやめる（1 個）', (r.get('result') or {}).get('cancelled') == 1, r)
clear_start = os.path.getsize(LOG)
r = cmd(c, 'clearArea', {'centerX': qx, 'centerZ': qz})
check('すぐ流し直した「周りをきれいにする」を受け付ける', r.get('success') is True, r)
t0 = time.monotonic()
check('流し直した分が 600 秒以内に終わる', wait_queue(c, 600), time.monotonic() - t0)
done_chat = wait_log(clear_start, '周囲クリア完了', 10)
check('チャットに「周囲クリア完了」が出た（10 秒まで待つ）', done_chat,
      [l.split('[CHAT] ', 1)[1] for l in log_since(clear_start).splitlines() if '[CHAT] ' in l][-3:])
print('    （流し直しから終わるまで %.0f 秒）' % (time.monotonic() - t0))
c.close()
time.sleep(1)

print('--- 9. 一時停止（pauseQueue・resumeQueue）: 止めている間は置かず、ほかのタブは進む。30 秒より長く止めても、再開したら最後まで置く')
p, _ = open_conn('pause_a')
q, _ = open_conn('pause_b')
px, pz = base + 200000, base + 200000
pause_log = os.path.getsize(LOG)
pause_debug = debug_size()
# 最後の 1 個だけ石にする（再開したあと、最後まで本当に置いたかを見るため。終わったら空気に戻す）
pause_line = far_line(px, pz, 300)
pause_last = dict(pause_line[-1]['params'])
pause_line[-1] = {'action': 'setBlock', 'params': dict(pause_last, blockType='stone')}
send_all(p, pause_line)
# 1 秒置いてから止める（先頭の命令が、まだ作られていない遠くのチャンクの読み込みを待ち始めてから止めるため。
# 待ち始める前に止めると、止めていた間を数えてあきらめるかを確かめられない）
time.sleep(1)
r = cmd(p, 'pauseQueue', {})
waiting = (r.get('result') or {}).get('waiting', -1)
print('    （止めたとき、並んでいる命令は %d 個。300 個より少なければ、もう置き始めていた）' % waiting)
check('止めると、並んでいる命令の数を返す（1 個より多い）', r.get('success') is True and waiting > 1, r)
paused_at = time.monotonic()
r = cmd(q, 'getTargetBlock', {})
check('止めている間も、ほかのタブの命令は列に並んで進む（queued: True）', (r.get('result') or {}).get('queued') is True, r)
# 比べる基準の数は、ほかのタブの読み取りが列で進んだあと（メインスレッドが一回りしたあと）に取り直す
# （止める命令は、メインスレッドがいま進めている 1 回が終わるのを待たないので、止めた直後の数は 1 つ多いことがある）
r = cmd(p, 'pauseQueue', {})
waiting = (r.get('result') or {}).get('waiting', -1)
# チャンクの読み込みをあきらめるまでの時間（30 秒）より長く止める（再開したとき、止めていた間を数えてすぐあきらめないか）
time.sleep(max(0.0, 35 - (time.monotonic() - paused_at)))
r = cmd(p, 'pauseQueue', {})
check('35 秒止めている間、1 個も置かない（並んでいる数が同じ）', (r.get('result') or {}).get('waiting') == waiting, [waiting, r])
r = cmd(p, 'resumeQueue', {})
# 返事の数は、再開してから数えるので、そのあいだに置き始めた分だけ少ないことがある
resumed_waiting = (r.get('result') or {}).get('waiting', -1)
check('再開すると、止めていない印と、並んでいる数（止めたときの数以下）を返す', r.get('success') is True
      and (r.get('result') or {}).get('paused') is False and 0 <= resumed_waiting <= waiting, [waiting, r])
t0 = time.monotonic()
check('再開したら、止めたところから最後まで置く（180 秒以内）', wait_queue(p, 180), time.monotonic() - t0)
last_pos = {k: pause_last[k] for k in ('x', 'y', 'z')}
last_block = block_type(p, last_pos)
check('最後の 1 個（石）も置いた', last_block.endswith('stone'), last_block)
gave_up = gave_up_since(pause_log, pause_debug)
check('止めていた間を数えて、読み込みをあきらめていない', not gave_up, gave_up[:3])
r = cmd(p, 'setBlock', pause_last)
check('片づけ（最後の 1 個を空気に戻す）を受け付ける', r.get('success') is True, r)
check('片づけの列が 30 秒以内に空く', wait_queue(p, 30))
last_block = block_type(p, last_pos)
check('片づけで、最後の 1 個は空気に戻った', is_air(last_block), last_block)

print('--- 9. 止めている間に止めるボタン（stopAll）を押すと、並んでいた命令はやめ、そのあとはすぐ進む')
send_all(p, far_line(px, pz + 2000, 300))
r = cmd(p, 'pauseQueue', {})
check('止める（止めた印の返事）', r.get('success') is True and (r.get('result') or {}).get('paused') is True, r)
r = cmd(p, 'stopAll', {})
check('止めている列の命令をやめる（1 個より多い）', r.get('success') is True and (r.get('result') or {}).get('cancelled', 0) > 1, r)
check('そのあと届いた命令は、止めずに進む（5 秒以内）', wait_queue(p, 5))

print('--- 9. 止めている間にタブを閉じると、並んでいた命令はやめる')
time.sleep(3.5)  # 知らせの間引き（3 秒）をあける
close_log = os.path.getsize(LOG)
send_all(p, far_line(px, pz + 4000, 300))
r = cmd(p, 'pauseQueue', {})
check('止める（止めた印の返事）', r.get('success') is True and (r.get('result') or {}).get('paused') is True, r)
p.close()
check('閉じたら、つながりが切れて命令をやめた知らせがチャットに出る（10 秒まで待つ）',
      wait_log(close_log, 'つながりが きれたので', 10),
      [l.split('[CHAT] ', 1)[1] for l in log_since(close_log).splitlines() if '[CHAT] ' in l][-3:])
q.close()
time.sleep(1)

print('--- 10. 大きな「埋める」を区画の途中で一時停止して再開しても、止めたマスから続けて、ちょうど範囲の数だけ置く')
# 10 節は debug.log（DEBUG の行）で確かめるので、先に出ているかを見る（2 節の setBlock が「Block placed」を出す）。
# 無ければ（debug.log が無い・DEBUG を出さない設定・切り替わった）、確かめられないので、ここで NG にして終える
debug_ok = 'Block placed' in debug_since(debug_start)
check('debug.log に、この台本の DEBUG の行が出ている（10 節で使う）', debug_ok, DEBUG_LOG)
if not debug_ok:
    print('結果: %d 件成功 / %d 件失敗' % (sum(results), len(results) - sum(results)))
    sys.exit(1)
f10, _ = open_conn('pause_fill')
w10, _ = open_conn('pause_watch')  # 止めている間に、メインスレッドが一回りしたかを見る別の接続
gx, gz = base + 240000, base + 240000
gbox = {'from': {'x': gx, 'y': 200, 'z': gz}, 'to': {'x': gx + 127, 'y': 229, 'z': gz + 127}}
gvolume = 128 * 30 * 128
g_first = {'x': gx, 'y': 200, 'z': gz}
g_last = {'x': gx + 127, 'y': 229, 'z': gz + 127}
# 先にチャンクを作っておく（空気で埋める。空気だけの区画は置かずに飛ばすので、チャンクを作って読み込むだけ）。
# チャンクがそろっていると、置く速さの限りで進むので、止めたときに区画の途中にいることが多くなる
r = cmd(f10, 'fillBlocks', dict(gbox, blockType='air'))
check('チャンクを作る「埋める」（空気）を受け付ける', r.get('success') is True, r)
check('チャンクを作る「埋める」が 180 秒以内に終わる', wait_queue(f10, 180))
fill_log, fill_debug = os.path.getsize(LOG), debug_size()
r = cmd(f10, 'fillBlocks', dict(gbox, blockType='dirt'))
check('大きな「埋める」（土）を受け付ける', r.get('success') is True, r)
# 短く止めて再開するのを、区画の途中で止められるまでくり返す（再開して次に進めるとき、区画の途中なら FillJob が debug.log に出す）
mid, progressed, attempts = [], [], 0
for attempts in range(1, 41):
    time.sleep(0.2)
    r = cmd(f10, 'pauseQueue', {})
    if (r.get('result') or {}).get('waiting') != 1:
        # もう置き終わった。止めた印を残すと、このあとの命令が全部止まった列に並ぶので、再開してから抜ける
        cmd(f10, 'resumeQueue', {})
        break
    # 別の接続の読み取り（列が重いので列を追い越すが、メインスレッドで動くので、止めたときに進めていた 1 回が終わったあと）。
    # これで、止めた列は、ここから先進まない
    r = cmd(w10, 'getTargetBlock', {})
    if r.get('success') is not True:
        print('    （メインスレッドの一回りを確かめる読み取りが失敗した: %r）' % (r,))
    paused_debug = debug_size()
    time.sleep(1)
    # 止めている 1 秒の間に「埋める」が終わっていたら、止めても置き続けている（終わらないまでに少し進む壊れ方は、これでは分からない）
    progressed += [l for l in debug_since(paused_debug).splitlines() if 'blocks with dirt' in l]
    cmd(f10, 'resumeQueue', {})
    time.sleep(0.3)
    mid = [l for l in debug_since(fill_debug).splitlines() if 'FillJob resumed mid-cell' in l]
    if mid:
        break
print('    （%d 回目で区画の途中で止められた: %s）' % (attempts, mid[-1].split(']: ', 1)[-1] if mid else 'なし'))
check('区画の途中で止めて再開した（debug.log に「FillJob resumed mid-cell」が出た）', bool(mid), attempts)
check('止めている 1 秒の間に「埋める」が最後まで終わらない', not progressed, progressed[:2])
check('再開したら、最後まで置く（180 秒以内）', wait_queue(f10, 180))
first_block, last_block = block_type(f10, g_first), block_type(f10, g_last)
check('最初のマスと最後のマスは土', first_block.endswith('dirt') and last_block.endswith('dirt'), [first_block, last_block])
# 置いた数は、debug.log の「Filled N blocks with dirt」がちょうど 1 行で、ちょうど範囲の数か（止めて再開しても、数え違えていない）
filled = [l for l in debug_since(fill_debug).splitlines() if 'blocks with dirt' in l]
check('置いた数は、ちょうど範囲の数（%d）で 1 行' % gvolume,
      len(filled) == 1 and filled[0].rstrip().endswith('Filled %d blocks with dirt' % gvolume), filled[-2:])
gave_up = gave_up_since(fill_log, fill_debug)
check('読み込みをあきらめていない', not gave_up, gave_up[:3])
# 片づけ（最後のマスまで空気に戻ったかと、空気の「埋める」がちょうど範囲の数で終わったかを見る）
clean_debug = debug_size()
r = cmd(f10, 'fillBlocks', dict(gbox, blockType='air'))
check('片づけの「埋める」（空気）を受け付ける', r.get('success') is True, r)
check('片づけが 180 秒以内に終わる', wait_queue(f10, 180))
cleaned = [l for l in debug_since(clean_debug).splitlines() if 'blocks with air' in l]
check('片づけは、ちょうど範囲の数（%d）で 1 行' % gvolume,
      len(cleaned) == 1 and cleaned[0].rstrip().endswith('Filled %d blocks with air' % gvolume), cleaned[-2:])
first_block, last_block = block_type(f10, g_first), block_type(f10, g_last)
check('片づけで、最初のマスと最後のマスは空気に戻った', is_air(first_block) and is_air(last_block), [first_block, last_block])

print('--- 10. 「周りをきれいにする」を途中で一時停止し、35 秒たってから再開しても、最後まで終わる（止めている間は進まない）')
# 「周りをきれいにする」は、となりのチャンクに時間で切れるチケットしか付けないので、止めている間に下ろされることがある。
# 再開したとき、となりを読み直して続け、読み込みをあきらめないかを見る（止めていた間をあきらめの 30 秒に数えない直しが
# 効いているかは、止めたのがチャンクの読み込みを待っている最中だったときにしか分からない。それは 9 節で、1 秒置いてから止めて見る）
cx10, cz10 = base + 280000, base + 280000
clear_log, clear_debug = os.path.getsize(LOG), debug_size()
r = cmd(f10, 'clearArea', {'centerX': cx10, 'centerZ': cz10})
check('「周りをきれいにする」を受け付ける', r.get('success') is True, r)
time.sleep(3)
r = cmd(f10, 'pauseQueue', {})
check('きれいにしている途中で止める（並んでいるのは 1 個）', r.get('success') is True and (r.get('result') or {}).get('waiting') == 1, r)
paused_at = time.monotonic()
# 別の接続から目印のチャットを送り、それがログに出るのを待ってから見張り始める（チャットは列で進むので、止めたときに進めていた
# 1 回が終わったあと。その 1 回が出した「進行中」のチャットは、目印より先にログに出るので、見張りに入らない）
mark = 'stop_check pause %d' % int(time.monotonic() * 1000)
mark_log = os.path.getsize(LOG)
r = request(w10, 'command', {'action': 'chat', 'params': {'message': mark}})
check('目印のチャットがログに出た（10 秒まで待つ）', wait_log(mark_log, mark, 10), r)
quiet_log = os.path.getsize(LOG)
time.sleep(max(0.0, 35 - (time.monotonic() - paused_at)))
moved = [l for l in log_since(quiet_log).splitlines() if '周囲クリア' in l]
check('止めている間は進まない（「周囲クリア進行中」「周囲クリア完了」が出ない）', not moved, moved[:2])
r = cmd(f10, 'resumeQueue', {})
check('再開する', r.get('success') is True and (r.get('result') or {}).get('paused') is False, r)
t0 = time.monotonic()
check('再開したら、最後まで終わる（600 秒以内）', wait_queue(f10, 600), time.monotonic() - t0)
check('チャットに「周囲クリア完了」が出た（10 秒まで待つ）', wait_log(clear_log, '周囲クリア完了', 10),
      [l.split('[CHAT] ', 1)[1] for l in log_since(clear_log).splitlines() if '[CHAT] ' in l][-3:])
gave_up = gave_up_since(clear_log, clear_debug)
check('止めていた間を数えて、読み込みをあきらめていない', not gave_up, gave_up[:3])
f10.close()
w10.close()
time.sleep(1)

text = open(LOG, 'rb').read()[log_start:].decode('utf-8', 'replace')
chats = [l.split('[CHAT] ', 1)[1] for l in text.splitlines() if '[CHAT] ' in l and 'やめました' in l]
print('--- チャット（やめた知らせ）')
for c in chats:
    print('   ', c)
check('やめた知らせがチャットに出た', len(chats) >= 2, chats)
check('スタックトレース・ERROR が出ていない', 'ERROR' not in text and 'Exception' not in text,
      [l for l in text.splitlines() if 'ERROR' in l or 'Exception' in l][:3])
print('結果: %d 件成功 / %d 件失敗' % (sum(results), len(results) - sum(results)))
# 失敗があれば失敗で終わる（`stop_check.py && 次の確かめ` と続けたときに、見逃さないように）
sys.exit(0 if all(results) else 1)

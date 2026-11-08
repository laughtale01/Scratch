# 止めるボタン（stopAll）と、接続が切れたときに、その接続のまだ置いていない命令が列から外れるかを、本物の MOD で確かめる
# 試し用の、ふつうの地形のワールドで、F3+P で「フォーカスを失ったら止める」を切ってから流す（授業で使うワールドでは流さない）。
# 空の上（y=200〜）の、まだ作られていない遠くの場所を使う（「埋める」で置いた石は最後に空気に戻す。
# 「周りをきれいにする」は遠くの場所を平らにする。8 節は最後まで平らにする）
# 7 節は、2 つのタブの命令が届いた順に置かれるか。8 節は、「周りをきれいにする」を止めてすぐ流し直して最後まで終わるか
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


def far_line(x0, z0, n):
    return [{'action': 'setBlock', 'params': {'x': x0 + i * 16, 'y': 200, 'z': z0, 'blockType': 'air'}} for i in range(n)]


def send_all(s, commands):
    ids = [send_only(s, 'command', c) for c in commands]
    return collect(s, ids)


log_start = os.path.getsize(LOG)
base = 3_000_000 + int(time.time()) % 1000 * 20000

print('--- 1. 接続の返事')
s1, info = open_conn('g1_a')
check('serverInfo.features に stopAll がある', 'stopAll' in (info.get('features') or []), info)
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

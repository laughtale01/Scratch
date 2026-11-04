# 本物の MOD（Minecraft で動いているもの）に「ブロックで埋める」などの命令を直接送り、上限の確かめをする
# Scratch（gui.js）は体積を先に確かめて断るので、Scratch からでは MOD 側の確かめまで届かない場合がある。
# このスクリプトは手元の確認用（http://localhost:8000）からの接続として、命令を MOD へ直接送る。
#
# 使い方: 新しくビルドした MOD を入れ、Minecraft で「試し用のスーパーフラットのワールド」を開き、
# オーバーワールド（ふつうの世界。ネザーやエンドではない）でクリエイティブにしてから、別の端末で
#   python3 -I tests/websocket/probe_fill.py
#
# 座標は Minecraft の座標（F3 で出るもの）。Scratch のブロックの Y は、これより 60 大きい。
# 「置ける」はずの範囲には空気（air）を置く。場所は空の上（y=200 以上）なので、スーパーフラットなら
# 何もない所に空気を置くだけで済む（ふつうのワールドで山などがあると、そこが削れる）。断られる範囲は何も置かれない。
# 下限の確かめでは、y=-64 の岩盤の 1 マスに岩盤を置く（もとから岩盤なので変わらない）。
# 相対座標の確かめのため、プレイヤーを x=1000, y=-60, z=0 へテレポートする（終わったら元の場所へ戻す）。
# 期待と違う結果が 1 件でも出たら、そこで止める（古い MOD に流して、本当に動かしてしまわないように）。
import base64, json, os, socket, struct, sys, time, uuid

PORT = int(sys.argv[1]) if len(sys.argv) > 1 else 14711
ORIGIN = 'http://localhost:8000'


def recv_exact(s, n):
    buf = b''
    while len(buf) < n:
        d = s.recv(n - len(buf))
        if not d:
            raise EOFError('MOD が接続を切りました')
        buf += d
    return buf


def send_text(s, text):
    data = text.encode('utf-8')
    head = bytes([0x81])
    n = len(data)
    if n < 126:
        head += bytes([0x80 | n])
    elif n < 65536:
        head += bytes([0x80 | 126]) + struct.pack('>H', n)
    else:
        head += bytes([0x80 | 127]) + struct.pack('>Q', n)
    key = os.urandom(4)
    s.sendall(head + key + bytes(c ^ key[i % 4] for i, c in enumerate(data)))


def read_text(s):
    h = recv_exact(s, 2)
    n = h[1] & 0x7F
    if n == 126:
        n = struct.unpack('>H', recv_exact(s, 2))[0]
    elif n == 127:
        n = struct.unpack('>Q', recv_exact(s, 8))[0]
    return h[0] & 0x0F, recv_exact(s, n)


def connect():
    s = socket.create_connection(('127.0.0.1', PORT), timeout=60)
    key = base64.b64encode(os.urandom(16)).decode()
    s.sendall(('GET /minecraft HTTP/1.1\r\nHost: localhost:%d\r\nUpgrade: websocket\r\nConnection: Upgrade\r\n'
               'Sec-WebSocket-Key: %s\r\nSec-WebSocket-Version: 13\r\nOrigin: %s\r\n\r\n' % (PORT, key, ORIGIN)).encode())
    head = b''
    while not head.endswith(b'\r\n\r\n'):
        d = s.recv(1)
        if not d:
            break
        head += d
    if b' 101 ' not in head.split(b'\r\n', 1)[0]:
        sys.exit('MOD につなげませんでした: %r' % head[:100])
    return s


def request(s, msg_type, payload):
    message_id = str(uuid.uuid4())
    send_text(s, json.dumps({'type': msg_type, 'messageId': message_id, 'payload': payload}))
    # 自分の命令への返事（requestId が同じもの）が来るまで読む
    while True:
        op, data = read_text(s)
        if op != 1:
            continue
        reply = json.loads(data.decode('utf-8'))
        if reply.get('requestId') == message_id:
            return reply


def fill(s, x1, y1, z1, x2, y2, z2, block='air'):
    return request(s, 'command', {'action': 'fillBlocks', 'params': {
        'from': {'x': x1, 'y': y1, 'z': z1}, 'to': {'x': x2, 'y': y2, 'z': z2}, 'blockType': block}})


def chat(s, message):
    return request(s, 'command', {'action': 'chat', 'params': {'message': message}})


def clear_area(s, center_x, center_z):
    return request(s, 'command', {'action': 'clearArea', 'params': {'centerX': center_x, 'centerZ': center_z}})


def clear_entities(s, center_x, center_z):
    return request(s, 'command', {'action': 'clearAllEntities', 'params': {'centerX': center_x, 'centerZ': center_z}})


def set_block(s, x, y, z):
    return request(s, 'command', {'action': 'setBlock', 'params': {'x': x, 'y': y, 'z': z, 'blockType': 'air'}})


def set_block_relative(s, rel_x, rel_y, rel_z):
    return request(s, 'command', {'action': 'setBlock', 'params': {
        'relativeX': rel_x, 'relativeY': rel_y, 'relativeZ': rel_z, 'blockType': 'air'}})


def get_position(s):
    reply = request(s, 'command', {'action': 'getPosition', 'params': {}})
    return (reply.get('payload') or {}).get('result')


def teleport(s, x, y, z):
    return request(s, 'command', {'action': 'teleport', 'params': {'x': x, 'y': y, 'z': z}})


def teleport_and_wait(s, x, y, z):
    """
    テレポートして、プレイヤーの位置が変わるまで待つ（テレポートは Minecraft の中で後から動くので、
    すぐに相対座標の命令を送ると、前の位置で計算されてしまうため）
    """
    reply = teleport(s, x, y, z)
    if (reply.get('payload') or {}).get('success') is not True:
        return reply
    pos = None
    for _ in range(50):
        pos = get_position(s)
        if pos and all(abs(pos.get(k, 1e18) - want) < 1 for k, want in zip('xyz', (x, y, z))):
            return reply
        time.sleep(0.1)
    # 5 秒待っても着かなければ失敗にする（このまま進むと、次の相対座標の確かめが前の場所で計算されてしまうため）
    return dict(reply, probe_error='テレポート先に着かなかった（5 秒待った。最後の場所: %r）' % (pos,))


def return_to_start(s, start):
    """
    はじめの場所へ戻す（戻せたら True）
    接続が切れているなどで戻せなかったときも、例外を出さずに False を返す（元の失敗を隠さないため）
    """
    try:
        s.settimeout(10)
        reply = teleport(s, start['x'], start['y'], start['z'])
        if (reply.get('payload') or {}).get('success') is not True:
            return False
        for _ in range(50):
            pos = get_position(s)
            if pos and all(abs(pos.get(k, 1e18) - start[k]) < 1 for k in 'xyz'):
                return True
            time.sleep(0.1)
        return False
    except Exception:
        # どんな失敗でも、ここでは例外を出さない（Ctrl+C の後のずれた返事や、形の違う返事でも /tp の案内を出すため）
        return False


def summon(s, x, y, z):
    return request(s, 'command', {'action': 'summonEntity', 'params': {'entityType': 'minecraft:pig', 'x': x, 'y': y, 'z': z}})


def set_container_item(s, x, y, z, slot=0):
    return request(s, 'command', {'action': 'setContainerItem', 'params': {
        'x': x, 'y': y, 'z': z, 'slot': slot, 'itemType': 'minecraft:stone', 'count': 1}})


def judge(reply, action, expect):
    """
    返事が期待どおりか確かめる（期待どおりなら None、違えば理由）
    expect が dict なら成功を期待し、result の中身（置いた範囲・数）も照らし合わせる
    expect が None なら、断られたこと（正しい形の返事で success が false）を期待する
    （エラーの返事や、形の違う返事は NG にする。ただし、断った理由は返事では分からない。
      ほかの理由で断られても同じ返事になるので、上限で断ったかは、チャットの文を目で確かめて答えてもらう）
    """
    if reply.get('probe_error'):
        return reply['probe_error']
    payload = reply.get('payload')
    if reply.get('type') != 'command_response' or not isinstance(payload, dict) or payload.get('action') != action:
        return '命令の返事の形ではない'
    if expect is None:
        if payload.get('success') is not False or payload.get('errorCode') != 'COMMAND_FAILED':
            return '断られなかった（または断り方が違う）'
        return None
    if payload.get('success') is not True:
        return '成功しなかった'
    result = payload.get('result') or {}
    for key, want in expect.items():
        if result.get(key) != want:
            return 'result の %s が %r（期待は %r）' % (key, result.get(key), want)
    return None


# 4,304,967,296（= 4294967296 + 10,000,000）は int にすると上の桁が落ちて 10,000,000 になる数
# （前の MOD ではワールドの中として通っていた。0 になる数を使わないのは、前の MOD に流したとき
#  「周りをきれいにする」などが原点の近くで動かないため。こちらなら遠くの何もない所だけで済む）
WRAP = 4304967296
# 相対座標の確かめでプレイヤーを置く場所（スーパーフラットの地面の上）
TP_X, TP_Y, TP_Z = 1000, -60, 0

CASES = [
    # (説明, 命令を送る関数, 命令の名前, 期待する結果（None は断られる）, 断られたときにゲームのチャットに出るはずの文)
    # 「ブロックで埋める」を先に置く（前の MOD なら、ここで NG になって止まる）
    ('横の上限ちょうど 256×1×256（空気を y=300 に）', lambda s: fill(s, 0, 300, 0, 255, 300, 255), 'fillBlocks',
     {'blocksFilled': 65536}, None),
    ('横（x）の上限を 1 マス超える 257×1×256', lambda s: fill(s, 0, 300, 0, 256, 300, 255), 'fillBlocks', None, 'はんいが広すぎます'),
    ('おくゆき（z）の上限を 1 マス超える 256×1×257', lambda s: fill(s, 0, 300, 0, 255, 300, 256), 'fillBlocks', None,
     'はんいが広すぎます'),
    ('体積の上限ちょうど 200×100×100 = 2,000,000（空気を y=200〜299 に。数秒止まることがある）',
     lambda s: fill(s, 0, 200, 0, 199, 299, 99), 'fillBlocks', {'blocksFilled': 2000000}, None),
    ('体積の上限を超える 200×101×100 = 2,020,000', lambda s: fill(s, 0, 200, 0, 199, 300, 99), 'fillBlocks', None,
     'はんいが広すぎます'),
    ('高さの上の外 y=1000〜1010', lambda s: fill(s, 0, 1000, 0, 5, 1010, 5), 'fillBlocks', None, 'その高さには ブロックを おけません'),
    ('高さの下の外 y=-1000〜-65', lambda s: fill(s, 0, -1000, 0, 5, -65, 5), 'fillBlocks', None,
     'その高さには ブロックを おけません'),
    # 6×20×6 = 720（y=300〜319 の 20 段）
    ('高さが上だけ外 y=300〜1000（建てられる 319 までに空気を置く）', lambda s: fill(s, 0, 300, 0, 5, 1000, 5), 'fillBlocks',
     {'blocksFilled': 720, 'from': {'x': 0, 'y': 300, 'z': 0}, 'to': {'x': 5, 'y': 319, 'z': 5}}, None),
    # 一番下の y=-64 は岩盤なので、岩盤を置いても変わらない
    ('高さが下だけ外 y=-1000〜-64（一番下の -64 の 1 マスに岩盤を置く）',
     lambda s: fill(s, 0, -1000, 0, 0, -64, 0, 'bedrock'), 'fillBlocks',
     {'blocksFilled': 1, 'from': {'x': 0, 'y': -64, 'z': 0}, 'to': {'x': 0, 'y': -64, 'z': 0}}, None),
    ('ワールドの外 x=40,000,000', lambda s: fill(s, 40000000, 300, 0, 40000005, 300, 5), 'fillBlocks', None,
     'はんいがワールドの外です'),
    # ワールドの端（-30,000,000 以上 30,000,000 未満）の確かめ。高さ y=1000 の 1 マスにするので、何も置かれない
    # （端の中なら「高さ」の理由で、外なら「ワールドの外」の理由で断られる。チャットの文で、どちらかを見分ける）
    ('ワールドの端の中 x=29,999,999（高さで断られる）', lambda s: fill(s, 29999999, 1000, 0, 29999999, 1000, 0), 'fillBlocks',
     None, 'その高さには ブロックを おけません'),
    ('ワールドの端ちょうど x=30,000,000', lambda s: fill(s, 30000000, 1000, 0, 30000000, 1000, 0), 'fillBlocks', None,
     'はんいがワールドの外です'),
    ('ワールドの端の中 x=-30,000,000（高さで断られる）', lambda s: fill(s, -30000000, 1000, 0, -30000000, 1000, 0), 'fillBlocks',
     None, 'その高さには ブロックを おけません'),
    ('ワールドの端の外 x=-30,000,001', lambda s: fill(s, -30000001, 1000, 0, -30000001, 1000, 0), 'fillBlocks', None,
     'はんいがワールドの外です'),
    ('はじめの x が回り込む数', lambda s: fill(s, WRAP, 300, 0, WRAP + 5, 300, 5), 'fillBlocks', None,
     'はんいがワールドの外です'),
    # 終わりの側だけを回り込む数にする（確かめが抜けていると、10,000,000〜10,000,005 の 6 マスとして通ってしまう）
    ('おわりの z だけが回り込む数', lambda s: fill(s, 0, 300, 10000000, 5, 300, WRAP + 5), 'fillBlocks', None,
     'はんいがワールドの外です'),
    ('周りをきれいにする: 中心 x=40,000,000', lambda s: clear_area(s, 40000000, 0), 'clearArea', None, 'ばしょがワールドの外です'),
    ('周りをきれいにする: 中心 z だけが回り込む数', lambda s: clear_area(s, 0, WRAP), 'clearArea', None, 'ばしょがワールドの外です'),
    ('生き物を全部消す: 中心 x=40,000,000', lambda s: clear_entities(s, 40000000, 0), 'clearAllEntities', None,
     'ばしょがワールドの外です'),
    ('生き物を全部消す: 中心 z だけが回り込む数', lambda s: clear_entities(s, 0, WRAP), 'clearAllEntities', None,
     'ばしょがワールドの外です'),
    ('ブロックを置く: x が回り込む数', lambda s: set_block(s, WRAP, 300, 0), 'setBlock', None, 'ばしょがワールドの外です'),
    ('ブロックを置く（相対）: ずらす x が回り込む数', lambda s: set_block_relative(s, WRAP, 0, 0), 'setBlock', None,
     'ばしょがワールドの外です'),
    ('テレポート: x=40,000,000', lambda s: teleport(s, 40000000, TP_Y, 0), 'teleport', None, 'ばしょがワールドの外です'),
    ('テレポート: x=%d, y=%d, z=%d へ（次の相対座標の確かめのため）' % (TP_X, TP_Y, TP_Z),
     lambda s: teleport_and_wait(s, TP_X, TP_Y, TP_Z), 'teleport', {}, None),
    # ずらす量（29,999,500）はワールドの中だが、プレイヤーの x=1000 と足すと 30,000,500 で外になる
    ('ブロックを置く（相対）: 足すとワールドの外 x=1000+29,999,500', lambda s: set_block_relative(s, 29999500, 0, 0), 'setBlock',
     None, 'ばしょがワールドの外です'),
    ('生き物を出す: x=40,000,000', lambda s: summon(s, 40000000, TP_Y, 0), 'summonEntity', None, 'ばしょがワールドの外です'),
    ('生き物を出す: z=40,000,000', lambda s: summon(s, 0, TP_Y, 40000000), 'summonEntity', None, 'ばしょがワールドの外です'),
    # 高さは Minecraft の /summon と同じく -20,000,000 以上 20,000,000 未満
    # （テレポートも同じ確かめを使う。テレポートで試すと、確かめが抜けた MOD ではプレイヤーが本当に飛ぶので、生き物で試す）
    ('生き物を出す: y=25,000,000（高すぎる）', lambda s: summon(s, 0, 25000000, 0), 'summonEntity', None, 'ばしょがワールドの外です'),
    ('生き物を出す: y=20,000,000（高さの上の端ちょうど）', lambda s: summon(s, 0, 20000000, 0), 'summonEntity', None,
     'ばしょがワールドの外です'),
    ('生き物を出す: y=-20,000,001（高さの下の端の外）', lambda s: summon(s, 0, -20000001, 0), 'summonEntity', None,
     'ばしょがワールドの外です'),
    ('入れ物にアイテムを入れる: z が回り込む数', lambda s: set_container_item(s, 0, 300, WRAP), 'setContainerItem', None,
     'ばしょがワールドの外です'),
    # スロットの確かめ（y=300 に入れ物は無いので、通っても何も変わらない）
    ('入れ物にアイテムを入れる: スロット 0（ふつうの番号は通る）', lambda s: set_container_item(s, 0, 300, 0, slot=0),
     'setContainerItem', {'slot': 0}, None),
    ('入れ物にアイテムを入れる: スロット -1', lambda s: set_container_item(s, 0, 300, 0, slot=-1),
     'setContainerItem', None, 'その スロットの ばんごうは つかえません'),
    # 回り込む数（確かめが抜けていると、上の桁が落ちて 10,000,000 番になる）
    ('入れ物にアイテムを入れる: スロットの番号が回り込む数', lambda s: set_container_item(s, 0, 300, 0, slot=WRAP),
     'setContainerItem', None, 'その スロットの ばんごうは つかえません'),
]

print('このスクリプトは、Minecraft のワールドに空気を置き、「周りをきれいにする」「生き物を全部消す」の命令も送ります。')
print('プレイヤーを x=%d へテレポートします（終わったら元の場所へ戻します）。' % TP_X)
print('断られるはずの命令のたびに止まるので、ゲームのチャットを見て答えてください。')
print('授業で使うワールドではなく、試し用のスーパーフラットのワールドを開き、')
print('オーバーワールド（ネザーやエンドではない）で、クリエイティブにしていますか？')
if input('試し用のワールドなら yes と入力してください: ').strip() != 'yes':
    sys.exit('止めました')

s = connect()
request(s, 'connect', {'clientId': 'probe_fill'})
# 終わったら戻れるよう、はじめの場所を覚えておく
start = get_position(s)
if not start or not all(isinstance(start.get(k), (int, float)) for k in 'xyz'):
    s.close()
    sys.exit('プレイヤーの場所が分かりません。ワールドに入ってから流してください')
# 途中でゲームが落ちても手で戻れるよう、はじめの場所を出しておく
print('はじめの場所: x=%.1f, y=%.1f, z=%.1f（終わったらここへ戻します）' % (start['x'], start['y'], start['z']))
done = 0
failed = False
try:
    for name, send, action, expect, chat_text in CASES:
        # 断られる命令の前に、番号の行をチャットに出す（同じ文が続くので、前の命令の文と見分けるため）
        marker = '[probe] %d 件目' % (done + 1)
        if chat_text:
            chat(s, marker)
        reply = send(s)
        problem = judge(reply, action, expect)
        if problem:
            print('NG  %s: %s' % (name, problem))
            print('      返事: %s' % json.dumps(reply, ensure_ascii=False)[:500])
            print('\n期待と違うので、ここで止めました（%d 件中 %d 件目）。MOD が新しくビルドしたものか確かめてください。'
                  % (len(CASES), done + 1))
            failed = True
            break
        # 断られる命令は、上限で断ったことをチャットの文で確かめる（返事だけでは理由が分からないため）
        # （前の大きな命令を Minecraft が置き終わるまで、チャットに出ないことがある）
        if chat_text and input('    %s: チャットに出るまで待ってから答えてください。「%s」のすぐ下に「%s」が出ましたか？ 出たら y: '
                               % (name, marker, chat_text)).strip() != 'y':
            print('NG  %s: チャットに文が出なかった（上限ではない理由で断られたかもしれない）' % name)
            print('\n期待と違うので、ここで止めました（%d 件中 %d 件目）。' % (len(CASES), done + 1))
            failed = True
            break
        done += 1
        print('ok  %s（%s）' % (name, '成功' if expect is not None else '断られた'))
finally:
    # はじめの場所へ戻す（途中で接続が切れていても、元の失敗を隠さないよう、ここでは例外を出さない）
    try:
        if not return_to_start(s, start):
            failed = True
            print('\nはじめの場所へ戻せませんでした。ゲームで /tp @s %.1f %.1f %.1f と打って戻ってください'
                  % (start['x'], start['y'], start['z']))
    finally:
        s.close()
if failed:
    sys.exit(1)
print('\nすべて期待どおり（%d 件）' % len(CASES))

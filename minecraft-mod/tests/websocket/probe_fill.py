# 本物の MOD（Minecraft で動いているもの）に「ブロックで埋める」などの命令を直接送り、
# 上限の確かめと、断ったときのチャット（とその間引き）の確かめをする
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
# 入れ物の確かめでは、空の上（x=2, y=300, z=2）にチェストを置き、終わったら消す（途中で止まったときも消す）。
# 間引きの確かめでは、時刻を正午にする。
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


def send_only(s, msg_type, payload):
    """返事を待たずに送る（送った命令の messageId を返す）"""
    message_id = str(uuid.uuid4())
    send_text(s, json.dumps({'type': msg_type, 'messageId': message_id, 'payload': payload}))
    return message_id


def collect(s, message_ids):
    """送った命令の返事を、すべて受け取るまで読む（messageId の順に並べて返す）"""
    replies = {}
    while len(replies) < len(message_ids):
        op, data = read_text(s)
        if op != 1:
            continue
        reply = json.loads(data.decode('utf-8'))
        if reply.get('requestId') in message_ids:
            replies[reply['requestId']] = reply
    return [replies[m] for m in message_ids]


def fill(s, x1, y1, z1, x2, y2, z2, block='air'):
    return request(s, 'command', {'action': 'fillBlocks', 'params': {
        'from': {'x': x1, 'y': y1, 'z': z1}, 'to': {'x': x2, 'y': y2, 'z': z2}, 'blockType': block}})


def chat(s, message):
    return request(s, 'command', {'action': 'chat', 'params': {'message': message}})


# 途中で止まったときに片づけるもの（チェストを置いたら True。finally で空気に戻す）
CLEANUP = {'chest': False}


def place_chest(s):
    CLEANUP['chest'] = True
    return set_block(s, 2, 300, 2, 'chest')


def remove_chest(s):
    reply = set_block(s, 2, 300, 2, 'air')
    if (reply.get('payload') or {}).get('success') is not True:
        return reply
    # 「置く」の返事は、Minecraft が実際に置く前に来る。メインスレッドで答える命令で出し終わるのを待ってから、
    # その場所が空気になったかを調べ、消えていたときだけ片づけ済みにする
    # （消えていなければ、finally でもう一度消して、だめなら打ち方を出す）
    request(s, 'command', {'action': 'getTargetBlock', 'params': {}})
    check = request(s, 'command', {'action': 'getBlockType', 'params': {'x': 2, 'y': 300, 'z': 2}})
    block = ((check.get('payload') or {}).get('result') or {}).get('blockType')
    if block != 'air':
        return dict(reply, probe_error='チェストが消えていない（調べたブロック: %r）' % (block,))
    CLEANUP['chest'] = False
    return reply


def clear_area(s, center_x, center_z):
    return request(s, 'command', {'action': 'clearArea', 'params': {'centerX': center_x, 'centerZ': center_z}})


def clear_entities(s, center_x, center_z):
    return request(s, 'command', {'action': 'clearAllEntities', 'params': {'centerX': center_x, 'centerZ': center_z}})


def set_block(s, x, y, z, block='air'):
    return request(s, 'command', {'action': 'setBlock', 'params': {'x': x, 'y': y, 'z': z, 'blockType': block}})


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


def summon(s, x, y, z, entity='minecraft:pig'):
    return request(s, 'command', {'action': 'summonEntity', 'params': {'entityType': entity, 'x': x, 'y': y, 'z': z}})


def set_container_item(s, x, y, z, slot=0, item='minecraft:stone'):
    return request(s, 'command', {'action': 'setContainerItem', 'params': {
        'x': x, 'y': y, 'z': z, 'slot': slot, 'itemType': item, 'count': 1}})


def judge(reply, action, expect, kind='command'):
    """
    返事が期待どおりか確かめる（期待どおりなら None、違えば理由）
    expect が dict なら成功を期待し、result の中身（置いた範囲・数）も照らし合わせる
    expect が None なら、断られたこと（正しい形の返事で success が false）を期待する
    （エラーの返事や、形の違う返事は NG にする。ただし、断った理由は返事では分からない。
      ほかの理由で断られても同じ返事になるので、期待した理由で断ったかは、チャットの文を目で確かめて答えてもらう）
    """
    if reply.get('probe_error'):
        return reply['probe_error']
    payload = reply.get('payload')
    # 送った種類（命令 command か、問い合わせ query か）に合う返事か
    failed_code = {'command': 'COMMAND_FAILED', 'query': 'QUERY_FAILED'}[kind]
    if reply.get('type') != kind + '_response' or not isinstance(payload, dict) or payload.get('action') != action:
        return '命令の返事の形ではない（%s の返事を待っていた）' % kind
    if expect is None:
        if payload.get('success') is not False or payload.get('errorCode') != failed_code:
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
# 確かめる MOD の版（build.gradle の version の頭）
EXPECTED_VERSION = '0.2.0'
# 長くて変な文字の入った名前（文字の向きを変える記号 U+202E と、色を変える §）と、チャットに出るはずの形
ODD_NAME = 'minecraft:\u202e\u00a7l' + 'a' * 40 + '\n'
ODD_NAME_SHOWN = 'minecraft:??l' + 'a' * 19 + '\u2026'
# 32 文字目が絵文字の名前（文字の数で切るので、絵文字は欠けずに残り、33 文字目の b から切られる）
EMOJI_NAME = 'minecraft:' + 'a' * 21 + '\U0001F600' + 'b'
EMOJI_NAME_SHOWN = 'minecraft:' + 'a' * 21 + '\U0001F600' + '\u2026'
# 32 文字より短い名前の中の、改行・タブ・文字の向きを変える記号（U+202E・U+061C）は ? にする
CONTROL_NAME = 'minecraft:\n\t\u202e\u061cb'
CONTROL_NAME_SHOWN = 'minecraft:????b'
# 相対座標の確かめでプレイヤーを置く場所（スーパーフラットの地面の上）
TP_X, TP_Y, TP_Z = 1000, -60, 0

CASES = [
    # (説明, 命令を送る関数, 命令の名前, 期待する結果（None は断られる）, 確かめるチャットの文（None なら確かめない）
    #  [, 送る種類（'command' か 'query'。書かなければ 'command'）])
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
    # 返事は成功（入れ物があるかは、あとで Minecraft の中で分かる）。入れ物が無いことはチャットで知らせる
    ('入れ物にアイテムを入れる: スロット 0（ふつうの番号は通る）', lambda s: set_container_item(s, 0, 300, 0, slot=0),
     'setContainerItem', {'slot': 0}, 'そこに いれものが ありません'),
    ('入れ物にアイテムを入れる: スロット -1', lambda s: set_container_item(s, 0, 300, 0, slot=-1),
     'setContainerItem', None, 'その スロットの ばんごうは つかえません'),
    # 回り込む数（確かめが抜けていると、上の桁が落ちて 10,000,000 番になる）
    ('入れ物にアイテムを入れる: スロットの番号が回り込む数', lambda s: set_container_item(s, 0, 300, 0, slot=WRAP),
     'setContainerItem', None, 'その スロットの ばんごうは つかえません'),
    # 本物のチェスト（スロットは 0〜26）に、範囲の外のスロット 27 を送る。返事は成功（あとで Minecraft の中で分かる）、
    # チャットで知らせて、中身は変わらない。チェストは空の上に置いて、終わったら消す
    ('チェストを置く（空の上 x=2, y=300, z=2）', place_chest, 'setBlock', {'blockPlaced': True}, None),
    ('入れ物にアイテムを入れる: チェストのスロット 27（範囲の外）', lambda s: set_container_item(s, 2, 300, 2, slot=27),
     'setContainerItem', {'slot': 27}, 'その スロットの ばんごうは つかえません'),
    ('チェストを消す', remove_chest, 'setBlock', {'blockPlaced': True}, None),
    # 前は何も知らせずに断っていた失敗（名前の打ちまちがいなど）も、チャットで知らせる
    ('ブロックで埋める: ブロックの名前の打ちまちがい', lambda s: fill(s, 0, 300, 0, 0, 300, 0, 'minecraft:no_such_block'),
     'fillBlocks', None, 'その ブロックの なまえが ちがいます'),
    ('生き物を出す: 生き物の名前の打ちまちがい', lambda s: summon(s, 0, TP_Y, 0, entity='minecraft:no_such_mob'), 'summonEntity',
     None, 'その いきものの なまえが ちがいます'),
    ('入れ物にアイテムを入れる: アイテムの名前の打ちまちがい', lambda s: set_container_item(s, 0, 300, 0, item='minecraft:no_such_item'),
     'setContainerItem', None, 'その アイテムの なまえが ちがいます'),
    ('ゲームモード: ない名前', lambda s: request(s, 'command', {'action': 'setGameMode', 'params': {'mode': 'no_such_mode'}}),
     'setGameMode', None, 'その ゲームモードは ありません'),
    ('MOD が知らない命令', lambda s: request(s, 'command', {'action': 'noSuchCommand', 'params': {}}), 'noSuchCommand', None,
     'その めいれいは この MOD では つかえません'),
    # 長い名前と、表示を乱す文字（色を変える § と、文字の向きを変える記号）は、32 文字で切って ? にして出す
    ('ブロックで埋める: 長くて変な文字の入った名前', lambda s: fill(s, 0, 300, 0, 0, 300, 0, ODD_NAME), 'fillBlocks', None,
     'その ブロックの なまえが ちがいます: ' + ODD_NAME_SHOWN),
    ('ブロックで埋める: 32 文字目が絵文字の名前', lambda s: fill(s, 0, 300, 0, 0, 300, 0, EMOJI_NAME), 'fillBlocks', None,
     'その ブロックの なまえが ちがいます: ' + EMOJI_NAME_SHOWN),
    ('ブロックで埋める: 改行などの入った短い名前', lambda s: fill(s, 0, 300, 0, 0, 300, 0, CONTROL_NAME), 'fillBlocks', None,
     'その ブロックの なまえが ちがいます: ' + CONTROL_NAME_SHOWN),
    # 「[」があって「]」で閉じていない名前（前は文字列の範囲の外を読む例外になっていた）
    ('ブロックで埋める: 「]」で閉じていない名前', lambda s: fill(s, 0, 300, 0, 0, 300, 0, 'stone['), 'fillBlocks', None,
     'その ブロックの なまえが ちがいます: stone['),
    # 座標が数でない（Scratch で数でない値は JSON の null になる）
    ('ブロックを置く: 座標が数でない', lambda s: request(s, 'command', {'action': 'setBlock', 'params': {
        'x': None, 'y': 300, 'z': 0, 'blockType': 'air'}}), 'setBlock', None, 'うまく できませんでした'),
    # params が無い・オブジェクトでない命令は動かさない（空として扱うと、中心 (0, 0) で原点のまわりを平らにしてしまう）
    ('周りをきれいにする: params が無い', lambda s: request(s, 'command', {'action': 'clearArea'}), 'clearArea', None,
     'うまく できませんでした'),
    ('周りをきれいにする: params が配列', lambda s: request(s, 'command', {'action': 'clearArea', 'params': []}), 'clearArea',
     None, 'うまく できませんでした'),
    # params はあるが中心のキーが無い（決まった中心 (0, 0) で動かさない）
    ('周りをきれいにする: 中心のキーが無い', lambda s: request(s, 'command', {'action': 'clearArea', 'params': {}}), 'clearArea',
     None, 'うまく できませんでした'),
    ('生き物を全部消す: 中心の z が無い', lambda s: request(s, 'command', {'action': 'clearAllEntities', 'params': {'centerX': 0}}),
     'clearAllEntities', None, 'うまく できませんでした'),
    # もう 1 つの受け口（query）でも、params が無い・オブジェクトでなければ断る。{} なら動く
    # （「今いる位置」はキーが要らないので、params の扱いだけを確かめられる。6 つ目は送る種類）
    ('問い合わせ: 今いる位置、params が {}（動く）', lambda s: request(s, 'query', {'action': 'getPosition', 'params': {}}),
     'getPosition', {}, None, 'query'),
    ('問い合わせ: 今いる位置、params が無い', lambda s: request(s, 'query', {'action': 'getPosition'}), 'getPosition', None,
     'うまく できませんでした', 'query'),
    ('問い合わせ: 今いる位置、params が配列', lambda s: request(s, 'query', {'action': 'getPosition', 'params': []}),
     'getPosition', None, 'うまく できませんでした', 'query'),
    ('問い合わせ: 今いる位置、params が null', lambda s: request(s, 'query', {'action': 'getPosition', 'params': None}),
     'getPosition', None, 'うまく できませんでした', 'query'),
    ('問い合わせ: 今いる位置、params が文字', lambda s: request(s, 'query', {'action': 'getPosition', 'params': 'x'}),
     'getPosition', None, 'うまく できませんでした', 'query'),
    # 知らない天気・ゲームルールの名前（前は「天気: （名前）」と出して成功にしていた）
    ('天気: ない名前', lambda s: request(s, 'command', {'action': 'setWeather', 'params': {'weather': 'no_such_weather'}}),
     'setWeather', None, 'その てんきは ありません: no_such_weather'),
    ('ゲームルール: ない名前', lambda s: request(s, 'command', {'action': 'setGameRule', 'params': {
        'rule': 'no_such_rule', 'value': 'true'}}), 'setGameRule', None, 'その ルールは ありません: no_such_rule'),
    # チャットの命令の文は、Minecraft と同じ数え方で 256 文字まで（「=」で始まる文は、その行がちょうどその文か確かめる）
    # （a ばかりだと人が数えられないので、終わりの文字で見分ける）
    ('チャット: a を 255 文字と Z（256 文字。切られずに Z で終わる）', lambda s: chat(s, 'a' * 255 + 'Z'), 'chat', {},
     '=' + 'a' * 255 + 'Z'),
    ('チャット: a を 255 文字と ZY（257 文字。Z で終わり、Y は切られる）', lambda s: chat(s, 'a' * 255 + 'ZY'), 'chat', {},
     '=' + 'a' * 255 + 'Z'),
    ('チャット: 254 文字と絵文字と b（絵文字はちょうど入り、b が切られる）', lambda s: chat(s, 'a' * 254 + '\U0001F600' + 'b'), 'chat',
     {}, '=' + 'a' * 254 + '\U0001F600'),
    ('チャット: 255 文字と絵文字（絵文字の途中で切れるので、絵文字ごと落とす）', lambda s: chat(s, 'a' * 255 + '\U0001F600'), 'chat',
     {}, '=' + 'a' * 255),
    ('チャット: 見えない制御文字（ESC・文字の向きを変える記号）は ? にする', lambda s: chat(s, 'x\x1by\u202ez'), 'chat', {},
     '=x?y?z'),
    ('チャット: 壊れた文字（ペアの無いサロゲート）と行の区切りは ? にする', lambda s: chat(s, 'x\ud800y\u2028z'), 'chat', {},
     '=x?y?z'),
    # 絵文字をつなぐ ZWJ（👩‍💻）と、ペルシア語などで使う ZWNJ は残す
    ('チャット: 絵文字をつなぐ文字（ZWJ）と ZWNJ は残す', lambda s: chat(s, '\U0001F469\u200d\U0001F4BB a\u200cb'), 'chat', {},
     '=\U0001F469\u200d\U0001F4BB a\u200cb'),
    # 座標のキーが無い（座標を確かめる所で、値の誤りとして断る）
    ('テレポート: x が無い', lambda s: request(s, 'command', {'action': 'teleport', 'params': {'y': TP_Y, 'z': 0}}), 'teleport',
     None, 'うまく できませんでした'),
    ('ブロックで埋める: はじめの x が無い', lambda s: request(s, 'command', {'action': 'fillBlocks', 'params': {
        'from': {'y': 300, 'z': 0}, 'to': {'x': 0, 'y': 300, 'z': 0}, 'blockType': 'air'}}), 'fillBlocks', None,
     'うまく できませんでした'),
    # 知らないプロパティが 2 つ（プロパティは無視して空気を置く。ログは 1 行に「2 unknown properties」とまとめる）
    ('ブロックで埋める: 知らないプロパティが 2 つ', lambda s: fill(s, 0, 300, 0, 0, 300, 0, 'air[bad1=x,bad2=y]'), 'fillBlocks',
     {'blocksFilled': 1}, None),
    # 速さ・暗視は、キーが無いとチャットで知らせて断る
    ('歩く速さ: 倍率のキーが無い', lambda s: request(s, 'command', {'action': 'setMoveSpeed', 'params': {}}), 'setMoveSpeed', None,
     'うまく できませんでした'),
    ('暗視: オンかオフのキーが無い', lambda s: request(s, 'command', {'action': 'setNightVision', 'params': {}}), 'setNightVision',
     None, 'うまく できませんでした'),
    ('飛ぶ速さ: 倍率のキーが無い', lambda s: request(s, 'command', {'action': 'setFlySpeed', 'params': {}}), 'setFlySpeed', None,
     'うまく できませんでした'),
    # キーが無い（同じ命令で、ちがう種類の例外。これも値の誤りとして、ログは種類ごとに 1 行）
    ('ブロックを置く: ブロックの名前が無い', lambda s: request(s, 'command', {'action': 'setBlock', 'params': {
        'x': 0, 'y': 300, 'z': 0}}), 'setBlock', None, 'うまく できませんでした'),
]

# 失敗のチャットの間引きの確かめ（番号の行をはさまずに、失敗を続けて送る）
# 同じ種類の失敗は 3 秒に 1 回だけ出る。間にほかのチャットが出たら、すぐまた出る
# 手順は ('cmd', 送る関数, 命令の名前[, 期待する結果])・('wait', 秒)・('chat', ふつうのチャットの文)・('sync',)・
# ('burst', [(命令の名前, params, 期待する結果), …]) の並び
# （期待する結果を書かなければ、断られることを期待する。'sync' は Minecraft のメインスレッドで答える命令を 1 つ送り、
#   それより前の命令のチャットが出終わるのを待つ）
# 期待する文の「|」は「どちらか」（ブロックの名前は、Minecraft の言葉の設定で変わるため）
# (説明, 手順, 2 つの番号の行のあいだに、上から順に出るはずの文)
WRAP_SET_BLOCK = ('cmd', lambda s: set_block(s, WRAP, 300, 0), 'setBlock')
WRAP_FILL = ('cmd', lambda s: fill(s, WRAP, 300, 0, WRAP, 300, 0), 'fillBlocks')
NORMAL_CHAT = 'probe ふつうの チャット'
CONTAINER_PARAMS = {'x': 0, 'y': 300, 'z': 0, 'slot': 0, 'itemType': 'minecraft:stone', 'count': 1}
THROTTLE_CASES = [
    ('同じ失敗を 5 回つづけて送る', [WRAP_SET_BLOCK] * 5, ['ばしょがワールドの外です']),
    ('2 種類の失敗をかわりばんこに 3 回ずつ送る', [WRAP_SET_BLOCK, WRAP_FILL] * 3,
     ['ばしょがワールドの外です', 'はんいがワールドの外です']),
    ('名前を変えながら打ちまちがいを 5 回送る',
     [('cmd', (lambda n: (lambda s: fill(s, 0, 300, 0, 0, 300, 0, 'minecraft:no_such_%d' % n)))(n), 'fillBlocks')
      for n in range(5)],
     ['その ブロックの なまえが ちがいます: minecraft:no_such_0']),
    ('同じ失敗を、3 秒あけてもう一度送る', [WRAP_SET_BLOCK, ('sync',), ('wait', 3.5), WRAP_SET_BLOCK],
     ['ばしょがワールドの外です', 'ばしょがワールドの外です']),
    ('同じ失敗の間に、ふつうのチャットを出す', [WRAP_SET_BLOCK, ('chat', NORMAL_CHAT), WRAP_SET_BLOCK],
     ['ばしょがワールドの外です', NORMAL_CHAT, 'ばしょがワールドの外です']),
    ('同じ失敗の間に、時刻を変える（MOD の知らせのチャット）',
     [WRAP_SET_BLOCK, ('cmd', lambda s: request(s, 'command', {'action': 'setTime', 'params': {'time': 6000}}), 'setTime', {}),
      WRAP_SET_BLOCK],
     ['ばしょがワールドの外です', '時刻: 正午', 'ばしょがワールドの外です']),
    ('同じ失敗の間に、ブロックの種類を調べる（ブロックの名前のチャット）',
     [WRAP_SET_BLOCK, ('cmd', lambda s: request(s, 'command', {'action': 'getBlockType', 'params': {'x': 0, 'y': -64, 'z': 0}}),
                       'getBlockType', {}),
      WRAP_SET_BLOCK],
     ['ばしょがワールドの外です', '岩盤|Bedrock', 'ばしょがワールドの外です']),
    # メインスレッドの中で分かる失敗（入れ物が無い）と、ふつうのチャットを、返事を待たずに続けて送る（出る順がそのままか）
    ('入れ物が無い → ふつうのチャット → 入れ物が無い（返事を待たずに続けて送る）',
     [('burst', [('setContainerItem', CONTAINER_PARAMS, {'slot': 0}), ('chat', {'message': NORMAL_CHAT}, {}),
                 ('setContainerItem', CONTAINER_PARAMS, {'slot': 0})])],
     ['そこに いれものが ありません', NORMAL_CHAT, 'そこに いれものが ありません']),
]

print('このスクリプトは、Minecraft のワールドに空気を置き（チェストも置いて消します）、時刻を正午にし、')
print('「周りをきれいにする」「生き物を全部消す」の命令も送ります（どれも断られるはずの形で）。')
print('プレイヤーを x=%d へテレポートします（終わったら元の場所へ戻します）。' % TP_X)
print('チャットを確かめる命令のたびに止まるので、ゲームのチャットを見て答えてください。')
print('授業で使うワールドではなく、試し用のスーパーフラットのワールドを開き、')
print('オーバーワールド（ネザーやエンドではない）で、クリエイティブにしていますか？')
if input('試し用のワールドなら yes と入力してください: ').strip() != 'yes':
    sys.exit('止めました')

s = connect()
# 接続の返事で、新しくビルドした MOD か確かめる（違えば、ワールドに命令を送る前に止める）
hello = request(s, 'connect', {'clientId': 'probe_fill'})
info = ((hello.get('payload') or {}).get('serverInfo') or {})
if (hello.get('payload') or {}).get('success') is not True or not str(info.get('version', '')).startswith(EXPECTED_VERSION) \
        or info.get('maxClients') != 8:
    s.close()
    sys.exit('MOD が期待した版ではありません（版 %r・最大の接続数 %r。期待は %s… と 8）。新しくビルドした jar を入れてください'
             % (info.get('version'), info.get('maxClients'), EXPECTED_VERSION))
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
    for case in CASES:
        name, send, action, expect, chat_text = case[:5]
        kind = case[5] if len(case) > 5 else 'command'
        # チャットを確かめる命令の前に、番号の行をチャットに出す（同じ文が続くので、前の命令の文と見分けるため）
        marker = '[probe] %d 件目' % (done + 1)
        if chat_text:
            chat(s, marker)
        reply = send(s)
        problem = judge(reply, action, expect, kind)
        if problem:
            print('NG  %s: %s' % (name, problem))
            print('      返事: %s' % json.dumps(reply, ensure_ascii=False)[:500])
            print('\n期待と違うので、ここで止めました（%d 件中 %d 件目）。MOD が新しくビルドしたものか確かめてください。'
                  % (len(CASES), done + 1))
            failed = True
            break
        # チャットの文で、期待した理由で断ったか（入れ物が無いなど）を確かめる（返事だけでは理由が分からないため）
        # （前の大きな命令を Minecraft が置き終わるまで、チャットに出ないことがある）
        if chat_text and input('    %s: チャットに出るまで待ってから答えてください。「%s」のすぐ下に「%s」が出ましたか？ 出たら y: '
                               % (name, marker, chat_text)).strip() != 'y':
            print('NG  %s: チャットに文が出なかった（期待とちがう理由で断られたかもしれない）' % name)
            print('\n期待と違うので、ここで止めました（%d 件中 %d 件目）。' % (len(CASES), done + 1))
            failed = True
            break
        done += 1
        print('ok  %s（%s）' % (name, '成功' if expect is not None else '断られた'))
    # 間引きの確かめ（2 つの番号の行のあいだに、期待した文が上から順にそれだけ出るか）
    for t, (name, steps, lines) in enumerate(THROTTLE_CASES if not failed else [], 1):
        begin, end = '[probe] 間引き %d' % t, '[probe] 間引き %d おわり' % t
        chat(s, begin)
        problem = None
        for step in steps:
            if step[0] == 'cmd':
                problem = problem or judge(step[1](s), step[2], step[3] if len(step) > 3 else None)
            elif step[0] == 'wait':
                time.sleep(step[1])
            elif step[0] == 'sync':
                # 「見ているブロック」はメインスレッドで調べて答えるので、返事が来たら、それより前の命令は出終わっている
                # （失敗の返事なら、待ちがずれるので止める）
                problem = problem or judge(request(s, 'command', {'action': 'getTargetBlock', 'params': {}}), 'getTargetBlock', {})
            elif step[0] == 'burst':
                # 返事を待たずに続けて送ってから、まとめて返事を受け取る（メインスレッドに命令がたまった状態を作る）
                ids = [send_only(s, 'command', {'action': a, 'params': prm}) for a, prm, _ in step[1]]
                for (a, _, want), reply in zip(step[1], collect(s, ids)):
                    problem = problem or judge(reply, a, want)
            else:
                chat(s, step[1])
        chat(s, end)
        if problem:
            print('NG  間引き: %s: %s' % (name, problem))
            failed = True
            break
        wanted = '」「'.join(lines)
        if input('    間引き: %s: チャットに出るまで待ってから答えてください。「%s」と「%s」のあいだに出たのは、上から順に「%s」だけでしたか？ そうなら y: '
                 % (name, begin, end, wanted)).strip() != 'y':
            print('NG  間引き: %s: 期待した行ではなかった' % name)
            failed = True
            break
        print('ok  間引き: %s' % name)
finally:
    # 途中で止まったら、置いたチェストを空気に戻す（ここでも例外は出さない）
    if CLEANUP['chest']:
        try:
            remove_chest(s)
        except Exception:
            pass
        if CLEANUP['chest']:
            failed = True
            print('\nチェストを消せませんでした。ゲームで /setblock 2 300 2 air と打って消してください')
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
print('\nすべて期待どおり（%d 件と、間引きの %d 件）' % (len(CASES), len(THROTTLE_CASES)))

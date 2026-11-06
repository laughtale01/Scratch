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
# 順番の確かめでは、空の上（x 0〜99, y 200〜219, z 0〜99）を石で 20 万ブロック埋め（2 回）、(99, 219, 99) に金ブロックを置く。
# また、プレイヤーを x=1000, y=-60, z=0 へテレポートして、その 5 マス上にエメラルドブロックを置く
# （まちがった MOD では、はじめの場所の 5 マス上に置かれることがある）。区画の境の確かめでは、x -18〜-14, y 206〜210,
# z -2〜2 を石で埋める。置いたらすぐ調べる確かめでは、(3, 300, 3) に金ブロックを置く。
# 見ているブロックの確かめでは、空の上（x -43〜-37, y 257〜264, z -43〜-37）に石の箱を作り、中を空気にして、
# プレイヤーを (-40, 258, -40) へテレポートする（終わったら前の場所へ戻してから箱を消す）。どれも確かめたら空気に戻す。
# 間引きの確かめでは、時刻を正午にする。
# 相対座標の確かめのため、プレイヤーを x=1000, y=-60, z=0 へテレポートする（終わったら元の場所へ戻す）。
# 期待と違う結果が 1 件でも出たら、そこで止める（古い MOD に流して、本当に動かしてしまわないように）。
import base64, json, math, os, socket, struct, sys, time, uuid

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


# 途中で止まったときに片づけるもの（チェストを置いたら True。順番の確かめで置いたブロックのマスと、石で埋めた範囲。
# finally で空気に戻す）
# maybe は、エメラルドブロックが置かれたかもしれないマス（テレポートの前の場所の上。もともと何かあったかもしれないので、
# エメラルドブロックだったときだけ消す）
# edge は、区画の境の確かめで石を埋めた範囲（EDGE_BOX）。look は、見ているブロックの確かめで作った石の箱（LOOK_BOX）
CLEANUP = {'chest': False, 'cells': set(), 'maybe': set(), 'box': False, 'edge': False, 'look': False}


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


def get_block_type(s, x, y, z):
    return request(s, 'command', {'action': 'getBlockType', 'params': {'x': x, 'y': y, 'z': z}})


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


def block_type(s, x, y, z):
    """そのマスのブロックの名前（MOD は前の命令が終わってから答える。答えられなければ None）"""
    reply = request(s, 'command', {'action': 'getBlockType', 'params': {'x': x, 'y': y, 'z': z}})
    return ((reply.get('payload') or {}).get('result') or {}).get('blockType')


# 列が空くのを待つ長さ（秒）。一度待ちきれなかったら、そのあとの片づけでは短く待つ（列が空かないまま、
# 片づけのたびに 60 秒ずつ黙って待ち続けないように）
QUEUE_WAIT = {'limit': 60}


def wait_queue(s, limit=None):
    """
    MOD の列にたまった命令が終わるまで待つ。「見ているブロック」は、列がすいていれば列に並んで答え、答えに queued: true が付く
    （列が混んでいると、並ばずに今の様子を答えて queued: false になる）。queued: true の答えが来るまで、limit 秒までやり直す。
    終わったら True
    """
    deadline = time.monotonic() + (QUEUE_WAIT['limit'] if limit is None else limit)
    while time.monotonic() < deadline:
        reply = request(s, 'command', {'action': 'getTargetBlock', 'params': {}})
        payload = reply.get('payload') or {}
        if payload.get('success') is True and (payload.get('result') or {}).get('queued') is True:
            return True
        time.sleep(0.5)
    QUEUE_WAIT['limit'] = 5
    return False


def clear_tracked(s):
    """
    覚えたマスを空気に戻し、空気になったか確かめる（消せたマスだけ忘れる）。全部消せたら True
    空気に戻す命令が断られたマスと、列が空くのを待てなかったときは、覚えたままにする
    （待たずに読むと、まだ列に残っている「置く」の前の空気を読んで、消せたと思ってしまうため）
    """
    if not CLEANUP['cells']:
        return True
    sent = set()
    for cell in sorted(CLEANUP['cells']):
        if (set_block(s, *cell, 'air').get('payload') or {}).get('success') is True:
            sent.add(cell)
    if not wait_queue(s):
        return False
    for cell in sorted(sent):
        if block_type(s, *cell) == 'air':
            CLEANUP['cells'].discard(cell)
    return not CLEANUP['cells']


def settle_maybe(s):
    """
    エメラルドブロックが置かれたかもしれないマスを、列が空いてから調べる（列に残った「置く」が、あとで置くことがあるため）。
    エメラルドブロックなら消すマス（cells）に移し、ほかのブロックなら（もともとあった物なので）忘れる。
    列が空くのを待てなかったときや、調べられなかったマスは、覚えたままにする
    """
    if not CLEANUP['maybe'] or not wait_queue(s):
        return
    for cell in sorted(CLEANUP['maybe']):
        found = block_type(s, *cell)
        if found == 'emerald_block':
            CLEANUP['cells'].add(cell)
        if found is not None:
            CLEANUP['maybe'].discard(cell)


# 順番の確かめで石を埋める範囲（空の上。100×20×100 = 20 万ブロック。MOD が何回にも分けて置く大きさ。
# 空気は、空気しかない所では MOD が飛ばしてすぐ終わるので、石を使う）と、その範囲の終わりのほうのマス・最後のマス
ORDER_BOX = ((0, 200, 0), (99, 219, 99))
ORDER_MARK = (98, 218, 98)
ORDER_LAST = (99, 219, 99)


def fill_order_box(s, block):
    (x1, y1, z1), (x2, y2, z2) = ORDER_BOX
    return fill(s, x1, y1, z1, x2, y2, z2, block)


def clear_order_box(s):
    """石で埋めた範囲を空気に戻し、空気になったか確かめる（すみ・目印・最後のマス）。戻せたら True"""
    fill_order_box(s, 'air')
    if not wait_queue(s):
        return False
    (x1, y1, z1), (x2, y2, z2) = ORDER_BOX
    cells = [(x, y, z) for x in (x1, x2) for y in (y1, y2) for z in (z1, z2)] + [ORDER_MARK, ORDER_LAST]
    if all(block_type(s, *cell) == 'air' for cell in cells):
        CLEANUP['box'] = False
        return True
    return False


def fill_then_set_block(s):
    """
    大きく埋めたすぐあとに、その最後のマスへ置いた金ブロックが、埋める続きで消されないか
    （MOD は大きな仕事を少しずつ進める。あとから届いた命令が途中に割りこむと、金ブロックが石で上書きされる）
    空の上を石で 20 万ブロック埋め、すぐその最後のマスに金ブロックを置く。前の命令が終わるのを待ってから、
    範囲の終わりのほうのマスが石になった（埋めるが本当に終わった）ことと、金ブロックが残っていることを調べ、空気に戻す
    """
    CLEANUP['box'] = True
    try:
        big = fill_order_box(s, 'stone')
        if (big.get('payload') or {}).get('success') is not True:
            return dict(big, probe_error='埋めるのが成功しなかった')
        placed = set_block(s, *ORDER_LAST, 'gold_block')
        if (placed.get('payload') or {}).get('success') is not True:
            return dict(placed, probe_error='金ブロックを置くのが成功しなかった')
        if not wait_queue(s):
            return dict(placed, probe_error='前の命令が終わるのを待てなかった（60 秒）')
        found_mark = block_type(s, *ORDER_MARK)
        if found_mark != 'stone':
            return dict(placed, probe_error='範囲の終わりのほうが石になっていない（%r。埋めるが終わる前に答えが返ったかもしれない）' % found_mark)
        found = block_type(s, *ORDER_LAST)
        if found != 'gold_block':
            return dict(placed, probe_error='置いた金ブロックが残っていない（%r。埋める続きに上書きされたかもしれない）' % found)
        return placed
    finally:
        try:
            if not clear_order_box(s):
                print('    （埋めた石を、まだ消せていない。最後にもう一度消す）')
        except Exception:
            pass


def teleport_then_relative(s):
    """
    大きな仕事のあとに「テレポート → 相対座標で置く」を続けて送ると、テレポートした先を基準に置かれるか
    （MOD は相対座標の場所を、列で順番が来たときのプレイヤーの位置で決める。届いた時点で決めると、テレポートの前の場所に置かれる）
    空の上を石で 20 万ブロック埋め、待たずにプレイヤーを (TP_X, TP_Y, TP_Z) へテレポートし、真上 5 マスにエメラルドブロックを置く
    """
    before = get_position(s)
    if not before:
        return {'probe_error': 'プレイヤーの場所が分からない'}
    old_cell = (math.floor(before['x']), math.floor(before['y']) + 5, math.floor(before['z']))
    # 前の版の MOD は 0 の方へ切り捨てたので、マイナスの座標では 1 マスずれた所に置くことがある。そこも片づけの候補にする
    old_cells = {old_cell, (math.trunc(before['x']), math.trunc(before['y']) + 5, math.trunc(before['z']))}
    new_cell = (TP_X, TP_Y + 5, TP_Z)
    if new_cell in old_cells:
        return {'probe_error': 'プレイヤーがもうテレポート先にいるので、確かめられない'}
    # 使うマスがもともと空気か確かめる（あとで空気に戻すので、もとからあったブロックを消さないように。
    # 前の場所の上がもとからエメラルドブロックだと、置かれたのか見分けられないため）
    if not wait_queue(s):
        return {'probe_error': '前の命令が終わるのを待てなかった（60 秒）'}
    found = {cell: block_type(s, *cell) for cell in sorted(old_cells | {new_cell})}
    if any(name != 'air' for name in found.values()):
        return {'probe_error': 'プレイヤーの 5 マス上か、テレポート先の 5 マス上が、もともと空気ではない（%r。'
                               '空の見える所に立って流してください）' % found}
    CLEANUP['box'] = True
    try:
        big = fill_order_box(s, 'stone')
        if (big.get('payload') or {}).get('success') is not True:
            return dict(big, probe_error='埋めるのが成功しなかった')
        moved = teleport(s, TP_X, TP_Y, TP_Z)
        if (moved.get('payload') or {}).get('success') is not True:
            return dict(moved, probe_error='テレポートが成功しなかった')
        # 片づけのため、どのマスも覚えておく（まちがった場所に置かれたときも消す。前の場所の上は、
        # エメラルドブロックだったときだけ消す）
        CLEANUP['maybe'].update(old_cells)
        CLEANUP['cells'].add(new_cell)
        placed = request(s, 'command', {'action': 'setBlock', 'params': {
            'relativeX': 0, 'relativeY': 5, 'relativeZ': 0, 'blockType': 'emerald_block'}})
        if (placed.get('payload') or {}).get('success') is not True:
            return dict(placed, probe_error='相対座標で置くのが成功しなかった')
        if not wait_queue(s):
            return dict(placed, probe_error='前の命令が終わるのを待てなかった（60 秒）')
        at_new, at_old = block_type(s, *new_cell), block_type(s, *old_cell)
        if at_new is None or at_old is None:
            return dict(placed, probe_error='置いた所を調べられなかった（先: %r、前の場所の上: %r）' % (at_new, at_old))
        if at_new != 'emerald_block' or at_old == 'emerald_block':
            return dict(placed, probe_error='テレポート先の上に置かれていない（先: %r、前の場所の上: %r）' % (at_new, at_old))
        return placed
    finally:
        try:
            # 前の場所の上は、もともとあったブロックを消さないよう、列が空いてから調べ、エメラルドブロックのときだけ消す
            # （待てなかったときや、調べられなかったときは、覚えたままにして、最後にもう一度見る）
            settle_maybe(s)
            if not clear_tracked(s):
                print('    （置いたブロックを、まだ消せていない。最後にもう一度消す）')
            if not clear_order_box(s):
                print('    （埋めた石を、まだ消せていない。最後にもう一度消す）')
        except Exception:
            pass


def read_right_after_place(s):
    """
    置いた直後に（前の命令が終わるのを待たずに）ブロックの種類を調べると、置いたブロックが答えられるか
    （列がすいているときは、調べる命令も列に並ぶので、前の「置く」が終わってから読む。並ばずに読むと、置く前の空気を答える）
    空の上 (3, 300, 3) に金ブロックを置き、すぐ調べ、空気に戻す
    """
    cell = (3, 300, 3)
    if not wait_queue(s):
        return {'probe_error': '前の命令が終わるのを待てなかった（60 秒）'}
    CLEANUP['cells'].add(cell)
    try:
        placed = set_block(s, *cell, 'gold_block')
        if (placed.get('payload') or {}).get('success') is not True:
            return dict(placed, probe_error='金ブロックを置くのが成功しなかった')
        reply = request(s, 'command', {'action': 'getBlockType', 'params': {'x': cell[0], 'y': cell[1], 'z': cell[2]}})
        result = (reply.get('payload') or {}).get('result') or {}
        if result.get('blockType') != 'gold_block' or result.get('queued') is not True:
            return dict(reply, probe_error='置いた金ブロックが答えられなかった（%r、列に並んだか: %r）'
                        % (result.get('blockType'), result.get('queued')))
        return placed
    finally:
        try:
            if not clear_tracked(s):
                print('    （置いたブロックを、まだ消せていない。最後にもう一度消す）')
        except Exception:
            pass


# 区画の境の確かめで石を埋める範囲（x はチャンク -2 と -1、y は 16 マスの区画 12 と 13、z はチャンク -1 と 0 にまたがる）
EDGE_BOX = ((-18, 206, -2), (-14, 210, 2))
# その中の 125 マス全部（はじめに空気か、埋めたら石か、片づけたら空気かを、全部のマスで調べる）と、範囲のすぐ外の 6 マス
EDGE_INSIDE = [(x, y, z) for x in range(-18, -13) for y in range(206, 211) for z in range(-2, 3)]
EDGE_OUTSIDE = [(-19, 208, 0), (-13, 208, 0), (-16, 205, 0), (-16, 211, 0), (-16, 208, -3), (-16, 208, 3)]


def clear_edge_box(s):
    """区画の境の確かめで埋めた範囲を空気に戻し、空気になったか確かめる（埋めるが断られたときも、消せていないとする）。戻せたら True"""
    (x1, y1, z1), (x2, y2, z2) = EDGE_BOX
    cleared = fill(s, x1, y1, z1, x2, y2, z2, 'air')
    if (cleared.get('payload') or {}).get('success') is not True or not wait_queue(s):
        return False
    if all(block_type(s, *cell) == 'air' for cell in EDGE_INSIDE):
        CLEANUP['edge'] = False
        return True
    return False


def fill_across_edges(s):
    """
    チャンクと 16 マスの区画の境をまたぐ小さな範囲を石で埋めると、どの区画も抜けずに置かれ、範囲の外は変わらないか
    （MOD はチャンクごと・区画ごとに分けて置くので、分け方をまちがえると、一部の区画が抜けたり、はみ出したりする）
    範囲の 125 マス全部（8 つに分かれる区画にまたがる）と、範囲のすぐ外の 6 マスを調べ、空気に戻して確かめる
    """
    (x1, y1, z1), (x2, y2, z2) = EDGE_BOX
    if not wait_queue(s):
        return {'probe_error': '前の命令が終わるのを待てなかった（60 秒）'}
    if any(block_type(s, *cell) != 'air' for cell in EDGE_INSIDE + EDGE_OUTSIDE):
        return {'probe_error': '使う所がもともと空気ではない（試し用のスーパーフラットのワールドで流してください）'}
    CLEANUP['edge'] = True
    try:
        filled = fill(s, x1, y1, z1, x2, y2, z2, 'stone')
        if (filled.get('payload') or {}).get('success') is not True:
            return dict(filled, probe_error='埋めるのが成功しなかった')
        if not wait_queue(s):
            return dict(filled, probe_error='前の命令が終わるのを待てなかった（60 秒）')
        missing = [cell for cell in EDGE_INSIDE if block_type(s, *cell) != 'stone']
        spilled = [cell for cell in EDGE_OUTSIDE if block_type(s, *cell) != 'air']
        # 範囲の外に置かれたマスは、もとは空気だったと確かめてあるので、空気に戻す
        CLEANUP['cells'].update(spilled)
        if missing or spilled:
            return dict(filled, probe_error='石が置かれていないマス %r、範囲の外に置かれたマス %r' % (missing[:5], spilled[:5]))
        return filled
    finally:
        try:
            if not clear_edge_box(s):
                print('    （埋めた石を、まだ消せていない。最後にもう一度消す）')
            if CLEANUP['cells'] and not clear_tracked(s):
                print('    （置いたブロックを、まだ消せていない。最後にもう一度消す）')
        except Exception:
            pass


# 「見ているブロック」の確かめで、プレイヤーを入れる石の箱（空の上。外側 7×8×7 の石の中を 5×6×5 の空気にする）と、
# プレイヤーを立たせる場所（箱の床の上。MOD のテレポートはマスのまん中に寄せないので、マスの境 (-40, 258, -40) に立つ。
# 目は床から 1.62 マス（しゃがんでも 1.27）、横の壁まで 2 と 3 マス、天井まで 4.38 マス、いちばん遠いすみまで約 6.1 マス。
# なので、どちらを向いていても 8 マスまでに石の壁に当たり、1 マスまででは、どこにも届かない）
LOOK_BOX = ((-43, 257, -43), (-37, 264, -37))
LOOK_INSIDE = ((-42, 258, -42), (-38, 263, -38))
LOOK_STAND = (-40, 258, -40)
# プレイヤーの目の高さ（立っているとき）
EYE_HEIGHT = 1.62


def look_box_cells():
    """
    箱のすみの 8 マスと、立たせるマス（はじめに空気か、片づけたら空気かを調べる）
    （箱の 392 マス全部は調べない。ブロックの種類を調べる命令は、1 回ごとにチャットへ名前を出すので、チャットが流れてしまうため。
      場所は空の上 y=257 より上なので、ふつうは何も無い）
    """
    (x1, y1, z1), (x2, y2, z2) = LOOK_BOX
    return [(x, y, z) for x in (x1, x2) for y in (y1, y2) for z in (z1, z2)] + [LOOK_STAND]


def angle_diff(a, b):
    """2 つの向き（度）の差（0〜180。-179 度と 179 度は 2 度ちがいとする）"""
    return abs((a - b + 180) % 360 - 180)


def expected_look_cell(pos):
    """
    プレイヤーの場所と向き（getPosition の x・y・z・yaw・pitch）から、見ている先で最初に当たる箱の壁のマスを計算する
    （向きは Minecraft と同じ式で出す。目から少しずつ進んで、箱の中の空気から出た所のマス。8 マスまでに出なければ None）
    """
    yaw, pitch = math.radians(pos['yaw']), math.radians(pos['pitch'])
    step = (-math.sin(yaw) * math.cos(pitch), -math.sin(pitch), math.cos(yaw) * math.cos(pitch))
    eye = (pos['x'], pos['y'] + EYE_HEIGHT, pos['z'])
    (ix1, iy1, iz1), (ix2, iy2, iz2) = LOOK_INSIDE
    t = 0.0
    while t <= 8:
        cell = tuple(math.floor(eye[i] + step[i] * t) for i in range(3))
        if not (ix1 <= cell[0] <= ix2 and iy1 <= cell[1] <= iy2 and iz1 <= cell[2] <= iz2):
            return cell
        t += 0.005
    return None


def clear_look_box(s):
    """石の箱を空気に戻し、空気になったか確かめる（埋めるが断られたときも、消せていないとする）。戻せたら True"""
    (x1, y1, z1), (x2, y2, z2) = LOOK_BOX
    cleared = fill(s, x1, y1, z1, x2, y2, z2, 'air')
    if (cleared.get('payload') or {}).get('success') is not True or not wait_queue(s):
        return False
    if all(block_type(s, *cell) == 'air' for cell in look_box_cells()):
        CLEANUP['look'] = False
        return True
    return False


def look_at_box(s):
    """
    「見ているブロック」が、見ている先のブロックに当たるか（MOD は見ている先を自分でたどるので、たどり方をまちがえると当たらなくなる）
    プレイヤーを空の上の石の箱に入れ、届く長さ 8 で調べて、当たったこと・石であること・場所が箱の壁であることと、
    その場所が、プレイヤーの向きから計算したマスと 1 マスまでしかずれていないこと（たどる向きのまちがいを見つけるため）を確かめる。
    1 マスは許す（マスの境に立っているので、軸に沿った向きでは、ごく小さな差でとなりのマスになる。Minecraft の sin は
    表を引く float の値で、ここの計算と少しちがう。しゃがんでいると目が 1.27 マスに下がる。どれも 1 マスまでのずれになる）。
    向きは、そのときのプレイヤーの向きのままなので、向きによっては、向きのまちがいを見逃すことがある（正しい MOD を
    まちがって NG にすることはない）。
    届く長さ 1 では、どこにも届かないので、当たらないことも確かめる。
    終わったら、前の場所へ戻してから箱を消す（先に消すと、空の上から落ちるため）
    """
    if not wait_queue(s):
        return {'probe_error': '前の命令が終わるのを待てなかった（60 秒）'}
    found = {cell: block_type(s, *cell) for cell in look_box_cells()}
    if any(name is None for name in found.values()):
        return {'probe_error': '使う所を調べられなかった（%r。チャンクの読み込みが間に合わなかったかもしれない）' % found}
    if any(name != 'air' for name in found.values()):
        return {'probe_error': '使う所がもともと空気ではない（%r。試し用のスーパーフラットのワールドで流してください）' % found}
    before = get_position(s)
    if not before:
        return {'probe_error': 'プレイヤーの場所が分からない'}
    CLEANUP['look'] = True
    moved_in = False
    try:
        (x1, y1, z1), (x2, y2, z2) = LOOK_BOX
        (ix1, iy1, iz1), (ix2, iy2, iz2) = LOOK_INSIDE
        made = fill(s, x1, y1, z1, x2, y2, z2, 'stone')
        if (made.get('payload') or {}).get('success') is not True:
            return dict(made, probe_error='石の箱を作るのが成功しなかった（石で埋める）')
        made = fill(s, ix1, iy1, iz1, ix2, iy2, iz2, 'air')
        if (made.get('payload') or {}).get('success') is not True:
            return dict(made, probe_error='石の箱を作るのが成功しなかった（中を空気にする）')
        moved_in = True
        moved = teleport_and_wait(s, *LOOK_STAND)
        if moved.get('probe_error') or (moved.get('payload') or {}).get('success') is not True:
            return dict(moved, probe_error=moved.get('probe_error') or 'テレポートが成功しなかった')
        pos = get_position(s)
        if not pos or not all(isinstance(pos.get(k), (int, float)) for k in ('x', 'y', 'z', 'yaw', 'pitch')):
            return {'probe_error': 'プレイヤーの場所と向きが分からない（%r）' % (pos,)}
        want = expected_look_cell(pos)
        far = request(s, 'command', {'action': 'getTargetBlock', 'params': {'reach': 8}})
        if (far.get('payload') or {}).get('success') is not True:
            return dict(far, probe_error='届く長さ 8 で調べるのが成功しなかった')
        # 向きを読んだときと、見ているブロックを調べたときのあいだに、向きが変わっていないか
        # （場所と向きは列に並ばずに読み、見ているブロックは列に並んで調べるので、そのあいだにマウスが動くと、計算した場所とずれる）
        after = get_position(s)
        if not after or not all(isinstance(after.get(k), (int, float)) for k in ('yaw', 'pitch')) \
                or angle_diff(after['yaw'], pos['yaw']) > 0.5 or abs(after['pitch'] - pos['pitch']) > 0.5:
            return {'probe_error': '調べているあいだに向きが変わった（%r → %r）。マウスを動かさずに、もう一度流してください' % (
                (pos['yaw'], pos['pitch']), after and (after.get('yaw'), after.get('pitch')))}
        hit = (far.get('payload') or {}).get('result') or {}
        cell = (hit.get('x'), hit.get('y'), hit.get('z'))
        on_wall = (all(isinstance(v, int) for v in cell) and x1 <= cell[0] <= x2 and y1 <= cell[1] <= y2 and z1 <= cell[2] <= z2
                   and (cell[0] in (x1, x2) or cell[1] in (y1, y2) or cell[2] in (z1, z2)))
        if hit.get('hit') is not True or hit.get('blockType') != 'minecraft:stone' or not on_wall:
            return dict(far, probe_error='箱の石の壁に当たらなかった（当たったか: %r、種類: %r、場所: %r）'
                        % (hit.get('hit'), hit.get('blockType'), cell))
        if want is None or any(abs(a - b) > 1 for a, b in zip(cell, want)):
            return dict(far, probe_error='見ている向きと違う壁に当たった（当たった場所: %r、向きから計算した場所: %r、yaw %.1f・pitch %.1f）'
                        % (cell, want, pos['yaw'], pos['pitch']))
        near = request(s, 'command', {'action': 'getTargetBlock', 'params': {'reach': 1}})
        near_payload = near.get('payload') or {}
        if near_payload.get('success') is not True:
            return dict(near, probe_error='届く長さ 1 で調べるのが成功しなかった')
        if (near_payload.get('result') or {}).get('hit') is not False:
            return dict(near, probe_error='届く長さ 1 なのに、何かに当たった')
        print('    （向き yaw %.1f・pitch %.1f、当たった場所 %r、計算した場所 %r）' % (pos['yaw'], pos['pitch'], cell, want))
        return far
    finally:
        # 先に前の場所へ戻す（箱を消すと、プレイヤーが空の上から落ちるため）。戻せたと確かめられたときだけ箱を消す
        # （戻せなかったときは、最後の片づけで、はじめの場所へ戻してから消す）
        returned = not moved_in
        try:
            if moved_in:
                back = teleport_and_wait(s, before['x'], before['y'], before['z'])
                returned = not back.get('probe_error') and (back.get('payload') or {}).get('success') is True
        except Exception:
            returned = False
        try:
            if not returned:
                print('    （前の場所へ戻せなかったので、石の箱はまだ消さない。最後にもう一度戻してから消す）')
            elif not clear_look_box(s):
                print('    （石の箱を、まだ消せていない。最後にもう一度消す）')
        except Exception:
            pass


def judge(reply, action, expect, kind='command'):
    """
    返事が期待どおりか確かめる（期待どおりなら None、違えば理由）
    expect が dict なら成功を期待し、result の中身（置いた範囲・数）も照らし合わせる
    expect が None なら、断られたこと（正しい形の返事で success が false）を期待する
    （エラーの返事や、形の違う返事は NG にする。ただし、断った理由は返事では分からない。
      ほかの理由で断られても同じ返事になるので、期待した理由で断ったかは、チャットの文を目で確かめて答えてもらう）
    kind が 'error' なら、消した受け口として、知らない種類（UNKNOWN_TYPE）のエラーの返事を期待する（action と expect は見ない）
    """
    if reply.get('probe_error'):
        return reply['probe_error']
    payload = reply.get('payload')
    # 消した受け口（query）は、知らない種類のエラーの返事を期待する
    if kind == 'error':
        if reply.get('type') != 'error' or not isinstance(payload, dict) or payload.get('errorCode') != 'UNKNOWN_TYPE':
            return '知らない種類のエラーの返事ではない（受け口が残っているかもしれない）'
        return None
    # 送った種類（命令 command）に合う返事か
    if reply.get('type') != kind + '_response' or not isinstance(payload, dict) or payload.get('action') != action:
        return '命令の返事の形ではない（%s の返事を待っていた）' % kind
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

# README の「遠くのチャンクで Minecraft が止まらないことを確かめる手順」は、この行より前（関数と決まった値）だけを
# exec して使う（試験の一覧の始まりの行で切る）。この名前を変えたり、これより前に試験を動かす文を足したりするときは、README も直す
CASES = [
    # (説明, 命令を送る関数, 命令の名前, 期待する結果（None は断られる）, 確かめるチャットの文（None なら確かめない）
    #  [, 期待する返事の種類（'command' なら命令の返事、'error' なら消した受け口へのエラーの返事。書かなければ 'command'）])
    # 「ブロックで埋める」を先に置く（前の MOD なら、ここで NG になって止まる）
    ('横の上限ちょうど 256×1×256（空気を y=300 に）', lambda s: fill(s, 0, 300, 0, 255, 300, 255), 'fillBlocks',
     {'blocksFilled': 65536}, None),
    ('横（x）の上限を 1 マス超える 257×1×256', lambda s: fill(s, 0, 300, 0, 256, 300, 255), 'fillBlocks', None, 'はんいが広すぎます'),
    ('おくゆき（z）の上限を 1 マス超える 256×1×257', lambda s: fill(s, 0, 300, 0, 255, 300, 256), 'fillBlocks', None,
     'はんいが広すぎます'),
    ('体積の上限ちょうど 200×100×100 = 2,000,000（空気を y=200〜299 に。MOD は少しずつ置く）',
     lambda s: fill(s, 0, 200, 0, 199, 299, 99), 'fillBlocks', {'blocksFilled': 2000000}, None),
    ('大きく埋めたすぐあとに、その最後のマスへ置いた金ブロックが残る（順番の確かめ）', fill_then_set_block, 'setBlock', {}, None),
    ('置いた直後に調べると、置いたブロックが答えられる（調べる命令も列に並ぶ）', read_right_after_place, 'setBlock', {}, None),
    ('チャンクと区画の境をまたぐ範囲を埋めると、抜けもはみ出しも無い', fill_across_edges, 'fillBlocks', {}, None),
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
    # どの件も、確かめが抜けていたときに読む場所が、建てられる高さの外になるようにする（遠くのチャンクを読み込まないように）。
    # x・z の件は y=1000 にする。y の件は y そのものが外（回り込む数は int にすると 10,000,000 になる）
    ('ブロックの種類を調べる: x が回り込む数', lambda s: get_block_type(s, WRAP, 1000, 0), 'getBlockType', None,
     'ばしょがワールドの外です'),
    ('ブロックの種類を調べる: z が回り込む数', lambda s: get_block_type(s, 0, 1000, WRAP), 'getBlockType', None,
     'ばしょがワールドの外です'),
    ('ブロックの種類を調べる: y が回り込む数', lambda s: get_block_type(s, 0, WRAP, 0), 'getBlockType', None,
     'ばしょがワールドの外です'),
    ('ブロックの種類を調べる: z=40,000,000', lambda s: get_block_type(s, 0, 1000, 40000000), 'getBlockType', None,
     'ばしょがワールドの外です'),
    ('ブロックの種類を調べる: y=40,000,000', lambda s: get_block_type(s, 0, 40000000, 0), 'getBlockType', None,
     'ばしょがワールドの外です'),
    ('大きな仕事のあとの「テレポート → 相対座標で置く」は、テレポートした先を基準に置く', teleport_then_relative, 'setBlock',
     {}, None),
    ('テレポート: x=40,000,000', lambda s: teleport(s, 40000000, TP_Y, 0), 'teleport', None, 'ばしょがワールドの外です'),
    ('テレポート: x=%d, y=%d, z=%d へ（次の相対座標の確かめのため）' % (TP_X, TP_Y, TP_Z),
     lambda s: teleport_and_wait(s, TP_X, TP_Y, TP_Z), 'teleport', {}, None),
    # ずらす量（29,999,500）はワールドの中だが、プレイヤーの x=1000 と足すと 30,000,500 で外になる
    # 足したあとの場所は、列で順番が来たときのプレイヤーの位置で確かめるので、返事は成功で、順番が来たときに断ってチャットに出す
    ('ブロックを置く（相対）: 足すとワールドの外 x=1000+29,999,500', lambda s: set_block_relative(s, 29999500, 0, 0), 'setBlock',
     {}, 'ばしょがワールドの外です'),
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
    # 出さないもの（作品を壊す・燃やすものの一覧と、Minecraft の /summon でも出せないもの）。
    # 断る決まりがこわれても作品に害が出ないものを使う（ドラゴンの火の玉はダメージの雲だけ、プレイヤーは作られない）
    ('生き物を出す: 出さない一覧のもの（ドラゴンの火の玉）', lambda s: summon(s, 0, TP_Y, 0, entity='minecraft:dragon_fireball'),
     'summonEntity', None, 'その いきものは ここでは だせません'),
    ('生き物を出す: Minecraft でも出せないもの（プレイヤー）', lambda s: summon(s, 0, TP_Y, 0, entity='minecraft:player'),
     'summonEntity', None, 'その いきものは ここでは だせません'),
    # 弾そのもの（雪玉）は、作ってみてから断るので、返事は成功（あとで Minecraft の中で分かる）、チャットで知らせる
    ('生き物を出す: 弾そのもの（雪玉）', lambda s: summon(s, 0, TP_Y, 0, entity='minecraft:snowball'),
     'summonEntity', {}, 'その いきものは ここでは だせません'),
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
    # 使われていない入口は消した（gui.js から送らないため。守る所を減らす）。問い合わせ（query）の受け口は、
    # 知らない種類としてエラーの返事になる（6 つ目は期待する返事の種類）。getBlock は知らない命令として断る
    ('問い合わせ（query）の受け口は無い', lambda s: request(s, 'query', {'action': 'getPosition', 'params': {}}),
     'getPosition', None, None, 'error'),
    ('ブロックを調べる（getBlock）の命令は無い', lambda s: request(s, 'command', {'action': 'getBlock',
     'params': {'x': 0, 'y': -64, 'z': 0}}), 'getBlock', None, 'その めいれいは この MOD では つかえません'),
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
    # プレイヤーを動かすので最後にする（前の件には、テレポート先 x=1000 にいることを使うものがある）
    ('見ているブロック: 石の箱の中で、見ている先の壁に当たる（届く長さ 1 では当たらない）', look_at_box, 'getTargetBlock',
     {'hit': True}, None),
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

print('このスクリプトは、Minecraft のワールドに空気を置き（チェスト・石・金ブロック・エメラルドブロックも置いて消します）、時刻を正午にし、')
print('「周りをきれいにする」「生き物を全部消す」の命令も送ります（どれも断られるはずの形で）。')
print('プレイヤーを x=%d へテレポートし、最後に空の上の石の箱の中 (%d, %d, %d) へもテレポートします（箱は消し、終わったら元の場所へ戻します）。'
      % ((TP_X,) + LOOK_STAND))
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
# 待ち列の入った MOD か確かめる（版の数は前のビルドと同じなので、「見ているブロック」の答えに queued があるかで見る。
# 無いと、列が空くのを待つ確かめが毎回 60 秒待ち続けたうえ、片づけの案内もまちがうため）
aim = (request(s, 'command', {'action': 'getTargetBlock', 'params': {}}).get('payload') or {})
if aim.get('success') is not True or 'queued' not in (aim.get('result') or {}):
    s.close()
    sys.exit('MOD が前のビルドのようです（「見ているブロック」の答えに queued がありません）。新しくビルドした jar を入れてください')
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
        print('ok  %s（%s）' % (name, 'エラーの返事' if kind == 'error' else '成功' if expect is not None else '断られた'))
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
    # はじめの場所へ先に戻す（石の箱の中にいるまま箱を消すと、空の上から落ちるため。ほかの片づけは決まった場所なので、
    # 先に戻しても困らない。途中で接続が切れていても、元の失敗を隠さないよう、ここでは例外を出さない）
    try:
        # 列に残った命令（大きな「埋める」など）のあとにテレポートが並ぶので、先に少し待つ
        # （待たないと、テレポートが動く前に確かめて、戻れたのに「戻せませんでした」と出ることがある）
        s.settimeout(10)
        wait_queue(s, 10)
        returned = return_to_start(s, start)
    except (Exception, KeyboardInterrupt):
        returned = False
    if not returned:
        failed = True
        print('\nはじめの場所へ戻せませんでした。ゲームで /tp @s %.1f %.1f %.1f と打って戻ってください（下の案内より先に打ってください）'
              % (start['x'], start['y'], start['z']))
    # 残っているものを、自動で空気に戻す（ここでも例外は出さない）。Ctrl+C を押されたら自動の片づけはやめて、
    # 手で消す案内だけ出す（案内まで飛ばされないように）。戻せなかったときは、石の箱は消さない（中にいるかもしれないため）
    cleaners = (('box', clear_order_box), ('edge', clear_edge_box), ('look', clear_look_box), ('maybe', settle_maybe),
                ('cells', clear_tracked), ('chest', remove_chest))
    cleaners = tuple((flag, clean) for flag, clean in cleaners if returned or flag != 'look')
    if any(CLEANUP[flag] for flag, _ in cleaners):
        print('\n片づけています（MOD の列が空くのを待つので、少しかかることがあります。'
              'Ctrl+C を押すと、片づけをやめて、手で消す案内を出します）')
    try:
        for flag, clean in cleaners:
            if CLEANUP[flag]:
                try:
                    clean(s)
                except Exception as e:
                    print('    （片づけの途中で %s が起きた）' % type(e).__name__)
    except KeyboardInterrupt:
        print('\n片づけをやめました')
    finally:
        s.close()
    # 消せなかったものを、手で消す案内
    if CLEANUP['box']:
        failed = True
        (bx1, by1, bz1), (bx2, by2, bz2) = ORDER_BOX
        # Minecraft の /fill は 1 回 32,768 ブロックまでなので、高さ 3 段ずつ（3 万ブロック）に分けて出す
        print('\n埋めた石を消せませんでした。ゲームで次の命令を 1 行ずつ打って消してください')
        for low in range(by1, by2 + 1, 3):
            print('  /fill %d %d %d %d %d %d air' % (bx1, low, bz1, bx2, min(low + 2, by2), bz2))
    if CLEANUP['edge']:
        failed = True
        print('\n区画の境の確かめで埋めた石を消せませんでした。ゲームで /fill %d %d %d %d %d %d air と打って消してください'
              % (EDGE_BOX[0] + EDGE_BOX[1]))
    if CLEANUP['look']:
        failed = True
        print('\n「見ているブロック」の確かめで置いた石の箱を消せませんでした。ゲームで /fill %d %d %d %d %d %d air と打って消してください'
              % (LOOK_BOX[0] + LOOK_BOX[1]))
    for (x, y, z) in sorted(CLEANUP['cells']):
        failed = True
        print('\n置いたブロックを消せませんでした。ゲームで /setblock %d %d %d air と打って消してください' % (x, y, z))
    for (x, y, z) in sorted(CLEANUP['maybe']):
        failed = True
        print('\nエメラルドブロックが置かれたかもしれないマスを調べられませんでした。(%d, %d, %d) がエメラルドブロックなら、'
              'ゲームで /setblock %d %d %d air と打って消してください' % (x, y, z, x, y, z))
    if CLEANUP['chest']:
        failed = True
        print('\nチェストを消せませんでした。ゲームで /setblock 2 300 2 air と打って消してください')
if failed:
    sys.exit(1)
print('\nすべて期待どおり（%d 件と、間引きの %d 件）' % (len(CASES), len(THROTTLE_CASES)))

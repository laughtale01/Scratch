# SimpleWebSocketServer の試験（偽物の部品で動かしたサーバーに、いろいろな接続を送って確かめる）
# run.sh から呼ぶ（コンパイルしたクラスの場所を環境変数 CLASSES_DIR で受け取る）
import base64, os, shutil, socket, struct, subprocess, sys, threading, time

PORT = int(sys.argv[1]) if len(sys.argv) > 1 else 24711
GOOD = 'https://laughtale01.github.io'
passed = failed = 0


def check(name, cond, detail=''):
    global passed, failed
    if cond:
        passed += 1; print('  ok  ', name)
    else:
        failed += 1; print('  NG  ', name, detail)


def conn(host='127.0.0.1'):
    return socket.create_connection((host, PORT), timeout=3)


def handshake(origin=GOOD, extra='', lower=False, key=None):
    s = conn()
    key = key if key is not None else base64.b64encode(os.urandom(16)).decode()
    h = ['GET /minecraft HTTP/1.1', 'Host: localhost:%d' % PORT, 'Upgrade: websocket', 'Connection: Upgrade',
         ('sec-websocket-key: ' if lower else 'Sec-WebSocket-Key: ') + key, 'Sec-WebSocket-Version: 13']
    if origin is not None:
        h.append(('origin: ' if lower else 'Origin: ') + origin)
    req = '\r\n'.join(h) + '\r\n' + extra + '\r\n'
    try:
        s.sendall(req.encode())
    except (BrokenPipeError, ConnectionResetError):
        return s, ''
    return s, read_http(s)


def read_http(s):
    # ヘッダーの終わり（空行）まで 1 バイトずつ読む
    # （まとめて読むと、同じパケットで届いた最初のフレームまで読み捨ててしまうため）
    buf = b''
    try:
        while not buf.endswith(b'\r\n\r\n'):
            d = s.recv(1)
            if not d: break
            buf += d
    except (socket.timeout, ConnectionResetError):
        pass
    return buf.split(b'\r\n', 1)[0].decode(errors='replace')


def frame(opcode, payload, masked=True):
    b = bytes([0x80 | opcode])
    n = len(payload)
    m = 0x80 if masked else 0
    if n < 126: b += bytes([m | n])
    elif n < 65536: b += bytes([m | 126]) + struct.pack('>H', n)
    else: b += bytes([m | 127]) + struct.pack('>Q', n)
    if masked:
        k = os.urandom(4)
        b += k + bytes(c ^ k[i % 4] for i, c in enumerate(payload))
    else:
        b += payload
    return b


def recv_exact(s, n):
    buf = b''
    while len(buf) < n:
        d = s.recv(n - len(buf))
        if not d: raise EOFError
        buf += d
    return buf


def read_frame(s):
    h = recv_exact(s, 2)
    n = h[1] & 0x7F
    if n == 126: n = struct.unpack('>H', recv_exact(s, 2))[0]
    elif n == 127: n = struct.unpack('>Q', recv_exact(s, 8))[0]
    return h[0] & 0x0F, recv_exact(s, n)


def is_closed(s, wait=3):
    s.settimeout(wait)
    try:
        while True:
            d = s.recv(65536)
            if not d: return True
    except (ConnectionResetError, BrokenPipeError):
        return True
    except socket.timeout:
        return False


def alive():
    s, st = handshake()
    ok = '101' in st
    if ok:
        s.sendall(frame(1, b'ping-check'))
        ok = read_frame(s)[1].startswith(b'len=10')
    s.close()
    return ok


def rejected_lines(kind):
    # kind は断った理由の種類（origin / bad request / handshake timed out など）
    return [l for l in logs if 'Rejected WebSocket connection (%s)' % kind in l]


# サーバーの出力は別のスレッドで全部読む（READY が出たら知らせる）
# 起動に失敗して READY が出ないまま終わったときも、待ち続けずに止める
logs = []
ready = threading.Event()


def pump(proc):
    for l in proc.stdout:
        logs.append(l)
        if 'READY' in l:
            ready.set()


# 標準入力は、最後の節で受け口を止める（STOP を送る）ために開けておく
srv = subprocess.Popen(['java', '-cp', os.environ['CLASSES_DIR'], 'TestMain', str(PORT)],
                       stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
try:
    threading.Thread(target=pump, args=(srv,), daemon=True).start()
    # READY を待つ（サーバーが先に終わったら、すぐあきらめる）
    deadline = time.time() + 30
    while not ready.wait(0.2) and srv.poll() is None and time.time() < deadline:
        pass
    if not ready.is_set():
        print('試験用のサーバーが起動しませんでした（ポート %d がふさがっていないか確かめてください）' % PORT)
        print(''.join(logs[-20:]))
        sys.exit(1)

    # ハンドシェイクの前の接続の試験は、ほかの断り方のログがまだ間引かれていない、いちばん初めに流す
    # （押し出された接続を、まちがって「handshake failed」に分けたら、ここで 1 行出て見つかるように）
    print('--- ハンドシェイクの前の接続は、8 本の枠に数えない')
    n = len(logs)
    # ブラウザが前もって開く接続（何も送らない）が 8 本あっても、Scratch はつながる
    idle = [conn() for _ in range(8)]
    time.sleep(0.1)
    s, st = handshake()
    check('何も送らない接続が 8 本あっても、Scratch はつながる（何も送らない接続はまだ切れていない）',
          '101' in st and not is_closed(idle[-1], wait=0.05), st); s.close()
    for c in idle: c.close()
    time.sleep(0.3)
    # ハンドシェイクの前の接続が上限（64 本）を超えたら、一番古いものから切る。新しい本物の接続は入れる
    idle = [conn() for _ in range(70)]
    time.sleep(0.1)
    # 先に、切られた 6 本を見る（切られていれば、70 本すべてを受け付け終わったと分かるので、次の「残る」の確かめが確かになる）
    evicted = all(is_closed(c, wait=0.3) for c in idle[:6])
    kept = not any(is_closed(c, wait=0.02) for c in idle[6:10])
    check('ハンドシェイクの前の接続が 64 本を超えたら、一番古いものから切る', evicted and kept, (evicted, kept))
    s, st = handshake()
    check('何も送らない接続が 70 本あっても、Scratch はつながる（何も送らない接続はまだ切れていない）',
          '101' in st and not is_closed(idle[-1], wait=0.05), st); s.close()
    for c in idle: c.close()
    time.sleep(0.5)
    check('押し出したログは WARN 1 行だけ（押し出された側は、DEBUG も「handshake failed」も出さない）',
          len(rejected_lines('too many pending handshakes')) == 1
          and not any(l.startswith('DEBUG') or 'handshake failed' in l for l in logs[n:]),
          [l for l in logs[n:] if 'Rejected' in l or l.startswith('DEBUG')][:4])

    # 断った接続のログは 1 回目だけすぐ出るので、ほかの断り方の確かめの中では、ログの確かめを最初にする
    # （上のハンドシェイクの前の接続の節は、押し出しのログしか出さない）
    print('--- ログ')
    log_start = len(logs)  # この節のログだけを見る（前の節の、本物の接続のつなぐ・切れるを数えないように）
    # Popen の text=True で \r は改行に変わる。無害化が効いていなければ、"[INFO] forged" で始まる偽の行ができる
    first_origin_reject = time.time()
    s, st = handshake('x\r[INFO] forged\x1b[2J\u2028' + 'y' * 300); s.close(); time.sleep(0.3)
    rej = rejected_lines('origin')
    check('断った Origin はログに 1 行出る', len(rej) == 1, rej)
    check('断った Origin の制御文字・ASCII 以外はログで ? に置き換える', bool(rej) and '?[INFO] forged?[2J?' in rej[-1]
          and '\x1b' not in rej[-1] and '\u2028' not in rej[-1], rej[-1:])
    check('断った Origin はログで 100 文字ほどに切る', bool(rej) and len(rej[-1]) < 200, len(rej[-1]) if rej else None)
    check('偽の行はログに入らない', not any(l.startswith('[INFO] forged') for l in logs))
    s, st = handshake(GOOD, extra='X-Long: ' + 'a' * 9000 + '\r\n'); s.close(); time.sleep(0.3)
    check('長すぎるヘッダーは ERROR ではなく WARN で出す', any(l.startswith('WARN') and 'header line too long' in l for l in logs)
          and not any(l.startswith('ERROR') and 'too long' in l for l in logs), [l for l in logs if 'too long' in l][:3])
    check('種類が違えば間引きは別（Origin を断った直後でも、bad request の 1 行目は出る）',
          len(rejected_lines('origin')) == 1 and len(rejected_lines('bad request')) == 1,
          (rejected_lines('origin'), rejected_lines('bad request')))
    # ここから先は、決まった文の行だけでなく、ログ全体（DEBUG も含む）が増えないことを確かめる
    # （直接 WARN を出す形や、接続ごとの debug の行に戻っても気づけるように）
    n = len(logs)
    for _ in range(30):
        s, st = handshake('https://evil.example'); s.close()
    time.sleep(0.3)
    check('断った接続を繰り返しても、ログは 1 行も増えない（60 秒に 1 行まで）', logs[n:] == [], logs[n:][:3])
    # 別のサイトのページからも送らせられる普通のリクエスト（鍵なし・長い URL）も、ログは間引く
    n = len(logs)
    for _ in range(30):
        s = conn(); s.sendall(b'GET / HTTP/1.1\r\nHost: localhost\r\n\r\n'); read_http(s); s.close()
    for _ in range(3):
        s = conn()
        try:
            s.sendall(b'GET /' + b'a' * 9000 + b' HTTP/1.1\r\n\r\n')
        except (BrokenPipeError, ConnectionResetError):
            pass
        read_http(s); s.close()
    time.sleep(0.3)
    check('鍵のないリクエスト・長い URL をくり返しても、ログは 1 行も増えない', logs[n:] == [], logs[n:][:3])
    # ハンドシェイクの途中で切られる（RST）と、サーバーの読み取りが失敗する。ERROR ではなく、間引いて 1 行だけ出す
    n = len(logs)
    for _ in range(10):
        s = conn(); s.sendall(b'GET /minecraft HTTP/1.1\r\nHost: localhost\r\n'); time.sleep(0.1)
        s.setsockopt(socket.SOL_SOCKET, socket.SO_LINGER, struct.pack('ii', 1, 0)); s.close()
    time.sleep(0.3)
    check('ハンドシェイクの途中で切られても、ログは WARN 1 行だけ（ERROR は出さない）',
          len(logs[n:]) == 1 and logs[n].startswith('WARN') and 'handshake failed' in logs[n], logs[n:][:3])
    check('断った接続では connected / disconnected をどのレベルでも出さない',
          not any('Client connected' in l or 'Client disconnected' in l for l in logs[log_start:]),
          [l for l in logs[log_start:] if 'Client' in l][:3])

    print('--- Origin')
    s, st = handshake(GOOD)
    check('公開している Scratch からはつながる', '101' in st, st)
    s.sendall(frame(1, '{"a":"日本語"}'.encode()))
    op, p = read_frame(s)
    check('つながったら命令が届いて返事が来る', op == 1 and p.startswith('len=11:{"a":"日本語'.encode()), p)
    s.close(); time.sleep(0.3)
    check('つながった接続が切れたら、info で知らせる', any(l.startswith('INFO') and 'Client disconnected' in l for l in logs[log_start:]),
          [l for l in logs[log_start:] if 'disconnected' in l][:3])
    # 切れた接続の、まだ置いていない命令をやめてもらう（断った接続では呼ばない。上の節の断った接続を含めて 1 回だけ）
    check('つながった接続が切れたら、命令をやめる知らせ（connectionClosed）を 1 回だけ呼ぶ',
          sum(1 for l in logs[log_start:] if 'stub connectionClosed' in l) == 1,
          [l for l in logs[log_start:] if 'connectionClosed' in l][:3])
    for o in ['http://localhost:8000', 'http://127.0.0.1:8000']:
        s, st = handshake(o); check('手元の確認用（8000 番）はつながる: ' + o, '101' in st, st); s.close()
    for o in [None, 'https://evil.example', 'https://laughtale01.github.io.evil.com', 'http://localhost.evil.com',
              'https://laughtale01.github.io/', 'HTTPS://LAUGHTALE01.GITHUB.IO', 'http://localhost:8000/x', 'null',
              'https://localhost', 'http://localhost:123456', 'http://localhost', 'http://127.0.0.1:8601',
              'http://localhost:80000', 'http://laughtale01.github.io']:
        s, st = handshake(o); check('ほかのサイトは 403: %r' % o, '403' in st, st)
        check('  そのあと切られる', is_closed(s)); s.close()
    s, st = handshake(GOOD, lower=True); check('ヘッダー名が小文字でもつながる', '101' in st, st); s.close()
    # ハンドシェイクと最初のフレームが 1 回で届いても、フレームを取りこぼさない
    s = conn(); key = base64.b64encode(os.urandom(16)).decode()
    s.sendall(('GET /minecraft HTTP/1.1\r\nHost: localhost\r\nUpgrade: websocket\r\nConnection: Upgrade\r\n'
               'Sec-WebSocket-Key: %s\r\nSec-WebSocket-Version: 13\r\nOrigin: %s\r\n\r\n' % (key, GOOD)).encode()
              + frame(1, b'same-packet'))
    st = read_http(s); op, p = read_frame(s)
    check('ハンドシェイクと同じパケットのフレームも受け取る', '101' in st and p.startswith(b'len=11:same-packe'), (st, p)); s.close()
    s, st = handshake(GOOD, key=''); check('鍵が空なら 400', '400' in st, st); s.close()

    print('--- ヘッダーの上限')
    # 黙ったまま待たされた（2 秒で時間切れ）のではなく、すぐ切られたことを確かめる
    t0 = time.time(); s, st = handshake(GOOD, extra='X-Long: ' + 'a' * 9000 + '\r\n')
    check('8KB を超える行は断ってすぐ切る', '101' not in st and is_closed(s, wait=1) and time.time() - t0 < 1, (st, time.time() - t0)); s.close()
    t0 = time.time(); s, st = handshake(GOOD, extra=''.join('X-%d: 1\r\n' % i for i in range(120)))
    check('100 行を超えるヘッダーは断ってすぐ切る', '101' not in st and is_closed(s, wait=1) and time.time() - t0 < 1, (st, time.time() - t0)); s.close()
    s, st = handshake(GOOD, extra=''.join('X-%d: 1\r\n' % i for i in range(90)))
    check('90 行のヘッダーならつながる', '101' in st, st); s.close()

    print('--- ハンドシェイクの時間切れ')
    # 前の接続の「切れた」のログが届いてから数え始める
    time.sleep(0.3); n = len(logs)
    s = conn(); t0 = time.time()
    closed = is_closed(s, wait=8)
    check('何も送らない接続は 2 秒ほどで切られる', closed and 1.5 <= time.time() - t0 <= 3.5, time.time() - t0); s.close()
    # 0.2 秒おきに 1 バイトずつ送り続けても、受け付けてから 2 秒ほどで切られる（1 回ごとの待ち時間ではなく全体の締め切り）
    s = conn(); t0 = time.time(); closed = False
    for ch in b'GET /minecraft HTTP/1.1\r\nX-Slow: aaaaaaaaaaaaaaaaaaaa':
        try:
            s.sendall(bytes([ch]))
        except (BrokenPipeError, ConnectionResetError):
            closed = True; break
        s.settimeout(0.2)
        try:
            if s.recv(1) == b'':
                closed = True; break
        except socket.timeout:
            pass
        except (ConnectionResetError, BrokenPipeError):
            closed = True; break
        if time.time() - t0 > 5: break
    check('少しずつ送り続ける接続も 2 秒ほどで切られる', closed and 1.5 <= time.time() - t0 <= 3.5, (closed, time.time() - t0)); s.close()
    time.sleep(0.3)
    check('時間切れは ERROR ではなく WARN で出す', any('handshake timed out' in l and l.startswith('WARN') for l in logs)
          and not any(l.startswith('ERROR') and 'Timeout' in l for l in logs), [l for l in logs if 'imed out' in l][:3])
    check('2 回時間切れになっても、ログは 1 行だけ', len(rejected_lines('handshake timed out')) == 1 and len(logs[n:]) == 1,
          logs[n:][:3])
    s, st = handshake(); time.sleep(6)
    s.sendall(frame(1, b'after-idle'))
    check('つながった後は 6 秒待っても切れない', read_frame(s)[1].startswith(b'len=10')); s.close()

    print('--- フレーム')
    s, _ = handshake(); s.sendall(frame(1, b'hello', masked=False))
    check('マスクなしのフレームは切る', is_closed(s)); s.close()
    s, _ = handshake(); s.sendall(bytes([0x81, 0x80 | 127]) + struct.pack('>Q', 1 << 40))
    check('1TB を名乗るフレームは切る', is_closed(s)); s.close()
    s, _ = handshake(); s.sendall(bytes([0x81, 0x80 | 127]) + struct.pack('>Q', 1 << 63))
    check('負になる長さのフレームは切る', is_closed(s)); s.close()
    s, _ = handshake(); s.sendall(bytes([0x81, 0x80 | 127]) + struct.pack('>Q', 1024 * 1024 + 1))
    check('1MB を 1 バイト超えるフレームは切る', is_closed(s)); s.close()
    s, _ = handshake(); s.settimeout(10); s.sendall(frame(1, b'x' * (1024 * 1024)))
    check('ちょうど 1MB のメッセージは受け取る', read_frame(s)[1].startswith(b'len=1048576')); s.close()
    s, _ = handshake(); s.sendall(frame(9, b'p' * 125)); op, p = read_frame(s)
    check('125 バイトの Ping に正しい長さの Pong を返す', op == 0xA and p == b'p' * 125, (op, len(p))); s.close()
    s, _ = handshake(); s.sendall(frame(9, b'p' * 126))
    check('126 バイトの Ping は切る', is_closed(s)); s.close()
    s, _ = handshake(); s.sendall(frame(8, b''))
    check('Close を送ると切れる', is_closed(s)); s.close()
    check('ここまでの後もサーバーは動いている', alive())
    # 決まりに合わないフレーム（マスクなし・大きすぎるなど）を何度送っても、ログは間引かれて 1 行だけ
    time.sleep(0.3)
    check('決まりに合わないフレームのログは、くり返しても 1 行だけ（WARN）', len(rejected_lines('bad frame')) == 1
          and all(l.startswith('WARN') for l in rejected_lines('bad frame'))
          and not any(('Unmasked' in l or 'too large' in l) and 'Rejected' not in l for l in logs), rejected_lines('bad frame'))
    # つないだあとに乱暴に切られても（RST）、ERROR は出さない（タブを閉じたときなど、よくあることなので debug）
    n = len(logs)
    for _ in range(3):
        s, _ = handshake(); time.sleep(0.1)
        s.setsockopt(socket.SOL_SOCKET, socket.SO_LINGER, struct.pack('ii', 1, 0)); s.close()
    time.sleep(0.5)
    check('つないだあとに乱暴に切られても、ERROR は出さない（「切れた」の debug の行で終える）',
          not any(l.startswith('ERROR') for l in logs[n:])
          and any(l.startswith('DEBUG') and 'Client connection lost' in l for l in logs[n:]),
          [l for l in logs[n:] if l.startswith('ERROR') or 'connection lost' in l][:3])

    print('--- 同時接続の上限')
    for _ in range(20):
        s, st = handshake('https://evil.example'); s.close()
    time.sleep(0.5)
    held = []
    for i in range(8):
        s, st = handshake(); held.append((s, st))
    check('断った接続の後でも 8 本つながる（枠が返っている）', all('101' in st for _, st in held), [st for _, st in held])
    t0 = time.time(); s9, st9 = handshake()
    check('9 本目はハンドシェイクで断られる（503）', '503' in st9 and is_closed(s9, wait=1) and time.time() - t0 < 1,
          (st9, time.time() - t0)); s9.close()
    held[0][0].close(); time.sleep(0.5)
    s, st = handshake(); check('1 本切ると、また 1 本つなげる', '101' in st, st); s.close()
    for s, _ in held[1:]: s.close()
    time.sleep(0.5)
    check('全部切った後もサーバーは動いている', alive())
    # ハンドシェイクの前に閉じた接続のあとで、枠が余計に返っていないか（もう一度 8 本つなぎ、9 本目が 503 で断られることを確かめる）
    for _ in range(10):
        s, st = handshake('https://evil.example'); s.close()
    s9 = conn(); s9.close()
    time.sleep(0.5)
    held = [handshake() for _ in range(8)]
    check('もう一度 8 本つながる', all('101' in st for _, st in held), [st for _, st in held])
    s9, st9 = handshake()
    check('9 本目はやはり断られる（枠が増えていない）', '503' in st9, st9); s9.close()
    # 8 本つないだままでも、何も送らない接続は 2 秒ほどで切られる（枠を使わずに待つ）
    t0 = time.time(); idle1 = conn()
    check('8 本つないだままでも、何も送らない接続は 2 秒ほどで切られる', is_closed(idle1, wait=5) and 1.5 <= time.time() - t0 <= 3.5,
          time.time() - t0); idle1.close()
    for s, _ in held: s.close()
    time.sleep(0.5)

    print('--- 自分の PC 以外から')
    # 自分の LAN のアドレスは Mac の ipconfig getifaddr で取る（ほかの OS では飛ばす）
    lan = ''
    if sys.platform == 'darwin' and shutil.which('ipconfig'):
        lan = subprocess.run(['ipconfig', 'getifaddr', 'en0'], capture_output=True, text=True).stdout.strip()
    if lan:
        try:
            s = conn(lan)
            check('LAN のアドレス(%s)からの接続はすぐ切られる' % lan, is_closed(s)); s.close()
        except (ConnectionRefusedError, ConnectionResetError):
            check('LAN のアドレス(%s)からはつながらない' % lan, True)
    else:
        print('  （Mac でない・LAN のアドレスが無いので飛ばす）')
    try:
        s = socket.create_connection(('::1', PORT), timeout=3)
        s.sendall(b'GET / HTTP/1.1\r\nSec-WebSocket-Key: a\r\nOrigin: ' + GOOD.encode() + b'\r\n\r\n')
        check('IPv6 の ::1 からはつながる（localhost が ::1 になるブラウザ用）', '101' in read_http(s)); s.close()
    except OSError as e:
        check('IPv6 の ::1 からはつながる', False, e)
    time.sleep(0.3)
    check('受け取った全文はログに出ない', not any('日本語' in l or 'after-idle' in l for l in logs), [l for l in logs if 'Received' in l][:3])

    print('--- 60 秒たったあとのログ（1 分ほど待つ）')
    # 最初に Origin で断ったログから 60 秒たつまで待ち、その間に断った数がまとめて出ることを確かめる
    wait = first_origin_reject + 61 - time.time()
    if wait > 0:
        time.sleep(wait)
    before = len(rejected_lines('origin'))
    s, st = handshake('https://evil.example'); s.close(); time.sleep(0.3)
    after = rejected_lines('origin')
    check('60 秒たつと、断ったログがまた 1 行出る', len(after) == before + 1, after[-2:])
    check('  その行に、間に断った数が書いてある', bool(after) and 'more since the last log' in after[-1], after[-1:])
    s, st = handshake('https://evil.example'); s.close(); time.sleep(0.3)
    check('  そのすぐ後に断っても、また 60 秒は出ない（数はまた 0 から）', len(rejected_lines('origin')) == before + 1,
          rejected_lines('origin')[-2:])

    # 受け口を止めたあとはつながらないので、いちばん最後に流す
    print('--- 受け口を止めるとき（Minecraft を止めるとき）は、つないでいた接続に命令をやめる知らせ（connectionClosed）を呼ばない')
    held = [handshake() for _ in range(2)]
    check('止める前に 2 本つながる', all('101' in st for _, st in held), [st for _, st in held])
    n = len(logs)
    srv.stdin.write('STOP\n'); srv.stdin.flush()
    deadline = time.monotonic() + 15
    # 2 本とも、切れたときの片づけ（finally）を終えるまで待つ（「Client disconnected」は connectionClosed を呼ぶか決めたあとに出る）
    while time.monotonic() < deadline and not (any('STOPPED' in l for l in logs[n:])
                                               and sum(1 for l in logs[n:] if 'Client disconnected' in l) >= 2):
        time.sleep(0.1)
    check('受け口が止まった', any('STOPPED' in l for l in logs[n:]), logs[n:][-5:])
    check('つないでいた 2 本とも切れて、片づけを終えた', sum(1 for l in logs[n:] if 'Client disconnected' in l) == 2
          and all(is_closed(s, wait=1) for s, _ in held), [l for l in logs[n:] if 'disconnected' in l])
    check('止めるときは connectionClosed を呼ばない（列ごと捨てるため）', not any('stub connectionClosed' in l for l in logs[n:]),
          [l for l in logs[n:] if 'connectionClosed' in l][:3])
    for s, _ in held: s.close()
finally:
    srv.kill()

print('\n結果: %d 件成功 / %d 件失敗' % (passed, failed))
sys.exit(1 if failed else 0)

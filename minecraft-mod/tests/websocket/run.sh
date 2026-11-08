#!/usr/bin/env bash
# MOD の WebSocket の受け口（SimpleWebSocketServer.java）の試験を流す
# Minecraft の部品は stubs/ の偽物に差し替え、本物の SimpleWebSocketServer.java をそのままコンパイルして動かす
# はじめに、縦スラブの名前が ModBlocks・ModItems・loot table・タグでそろっているかと、命令の列（tests/buildqueue）、
# Scratch のバンドルの拡張機能（tests/extension）も確かめる
# 使い方（minecraft-mod で）: mise exec -- bash tests/websocket/run.sh
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
python3 -I "$HERE/../data/check_vertical_slab_data.py"
# 本物の MOD で手で流す台本（stop_check.py）が、README の引数で起動できるか（何も送らない）。--test-world が無ければ止まるか
python3 -I "$HERE/stop_check.py" --test-world --self-check
if python3 -I "$HERE/stop_check.py" --self-check >/dev/null 2>&1; then
  echo "NG     stop_check.py が --test-world なしで止まらない"
  exit 1
fi
# 命令の列（BuildQueue）の試験も先に流す
bash "$HERE/../buildqueue/run.sh"
# Scratch のバンドルの拡張機能（［接続］と止めるボタン）の試験も流す
# （node が無いときと、バンドルが無いとき＝main ブランチでは飛ばす。バンドルは gh-pages だけで管理している）
if ! command -v node >/dev/null 2>&1; then
  echo "node が無いので tests/extension を飛ばします"
elif [ ! -f "$HERE/../../../gui.js" ]; then
  echo "gui.js が無い（main ブランチなど）ので tests/extension を飛ばします"
else
  bash "$HERE/../extension/run.sh"
fi
SRC="$HERE/../../src/main/java/com/github/minecraftedu/network/SimpleWebSocketServer.java"
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT
javac -encoding UTF-8 -d "$OUT" $(find "$HERE/stubs" -name '*.java') "$SRC"
# 本物の MOD と同じポートを避けて、試験用のポートで動かす
CLASSES_DIR="$OUT" python3 -I "$HERE/ws_test.py" "${WS_TEST_PORT:-24711}"

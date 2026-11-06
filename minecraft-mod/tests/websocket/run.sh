#!/usr/bin/env bash
# MOD の WebSocket の受け口（SimpleWebSocketServer.java）の試験を流す
# Minecraft の部品は stubs/ の偽物に差し替え、本物の SimpleWebSocketServer.java をそのままコンパイルして動かす
# はじめに、縦スラブの名前が ModBlocks・ModItems・loot table・タグでそろっているかも確かめる
# 使い方（minecraft-mod で）: mise exec -- bash tests/websocket/run.sh
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
python3 -I "$HERE/../data/check_vertical_slab_data.py"
SRC="$HERE/../../src/main/java/com/github/minecraftedu/network/SimpleWebSocketServer.java"
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT
javac -encoding UTF-8 -d "$OUT" $(find "$HERE/stubs" -name '*.java') "$SRC"
# 本物の MOD と同じポートを避けて、試験用のポートで動かす
CLASSES_DIR="$OUT" python3 -I "$HERE/ws_test.py" "${WS_TEST_PORT:-24711}"

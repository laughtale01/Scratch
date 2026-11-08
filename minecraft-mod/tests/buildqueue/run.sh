#!/usr/bin/env bash
# MOD の命令の列（BuildQueue.java）の試験を流す
# Minecraft の部品は stubs/ の偽物に差し替え、本物の BuildQueue.java をそのままコンパイルして動かす
# 使い方（minecraft-mod で）: mise exec -- bash tests/buildqueue/run.sh
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
SRC="$HERE/../../src/main/java/com/github/minecraftedu/commands/BuildQueue.java"
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT
javac -encoding UTF-8 -d "$OUT" $(find "$HERE/stubs" -name '*.java') "$SRC" "$HERE/BuildQueueTest.java"
java -cp "$OUT" com.github.minecraftedu.commands.BuildQueueTest

#!/usr/bin/env bash
# 「埋める」（FillJob）・「周りをきれいにする」（ClearAreaJob）・1 マスの仕事（BlockJob）と、遠すぎる場所を断るきまり（Reach）の試験を流す
# Minecraft の部品は stubs/ の偽物に差し替え、本物の FillJob・BlockJob・ClearAreaJob・Reach・BuildQueue をそのままコンパイルして動かす
# （チケットの付け外し・続きから置く・読み込み待ち・あきらめる・止める・再開・止めるボタンで外す・遠すぎる場所）
# 使い方（minecraft-mod で）: mise exec -- bash tests/jobs/run.sh（tests/websocket/run.sh からも流れる）
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
SRC="$HERE/../../src/main/java/com/github/minecraftedu/commands"
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT
javac -encoding UTF-8 -d "$OUT" $(find "$HERE/stubs" -name '*.java') \
  "$SRC/BuildQueue.java" "$SRC/BlockJob.java" "$SRC/FillJob.java" "$SRC/ClearAreaJob.java" "$SRC/Reach.java" "$HERE/JobsTest.java"
java -cp "$OUT" com.github.minecraftedu.commands.JobsTest

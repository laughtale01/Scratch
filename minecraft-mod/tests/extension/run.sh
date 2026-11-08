#!/usr/bin/env bash
# Scratch の 4 つのバンドルの Minecraft 拡張機能（［接続］と止めるボタン）の試験を流す
# 1 つが失敗しても、4 つとも流してから、失敗があれば失敗で終わる
# 通った件数も、バンドルごとに決めた数とくらべる（gui.js だけの確かめが飛ばされても気づくように。試験を足したら数も直す）
# 使い方（minecraft-mod で）: bash tests/extension/run.sh（node が要る。tests/websocket/run.sh からも流れる）
set -uo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$HERE/../../.."
status=0
for bundle in gui.js player.js blocksonly.js compatibilitytesting.js; do
  echo "== $bundle"
  # gui.js には録画の状態を聞き直す確かめが 3 件多い
  case "$bundle" in
    gui.js) expected=39 ;;
    *) expected=36 ;;
  esac
  out="$(node "$HERE/ext_check.js" "$ROOT/$bundle")" || status=1
  printf '%s\n' "$out"
  passed="$(printf '%s\n' "$out" | sed -n 's/^結果: \([0-9][0-9]*\) 件成功.*/\1/p')"
  if [ "$passed" != "$expected" ]; then
    echo "NG     $bundle の成功した件数が ${passed:-（結果の行が無い）} 件（$expected 件のはず）"
    status=1
  fi
done
exit $status

#!/bin/bash
# 試験をすべて順に動かす（本番にはつながない。エミュレーター・偽物の SDK・127.0.0.1 だけで開く Chrome を使う）
# 使い方: リポジトリのどこからでも  bash tests/run-all.sh
# 要るもの: Java 21 以上、firebase CLI、Node 22（Functions の試験のため）、
#           tests/ と functions/ で npm ci 済みであること（README.md を参照）
#           ブロックパレットの試験は Google Chrome（無ければ飛ばす。CHROME_PATH で場所を指定できる）
set -u
cd "$(dirname "$0")/.." || exit 1
CONFIG=firebase.test.json
failed=0
skipped=0

# firebase CLI を呼ぶ。FIREBASE_JS に firebase-tools の lib/bin/firebase.js を指定すると、
# 今の Node から呼ぶ（Homebrew の firebase は別の Node で動き、Functions もその Node で動くため）
fb() {
  if [ -n "${FIREBASE_JS:-}" ]; then node "$FIREBASE_JS" "$@"; else firebase "$@"; fi
}

run() {
  local label="$1"; shift
  echo "=== $label"
  "$@"
  local code=$?
  # 終わりのコード 77 は「飛ばした」（Chrome が無いときなど）。成功・失敗とは分けて出す
  if [ "$code" = 0 ]; then echo "--- $label: 成功"
  elif [ "$code" = 77 ]; then echo "--- $label: 飛ばした"; skipped=$((skipped + 1))
  else echo "--- $label: 失敗"; failed=1; fi
}

# エミュレーターを使わない試験（node:vm で画面のコードを読み、偽物の SDK で動かす）
run "管理画面の作品の削除" node tests/admin-delete-project.test.mjs
run "生徒の作品の削除" node tests/student-delete-project.test.mjs
run "新規保存の教室ID" node tests/save-new-project.test.mjs
# 本物の Chrome を見えない形で動かす試験（Chrome が無ければ飛ばす。127.0.0.1 の外への通信は空に差し替え、
# WebSocket が外へ開いたら失敗にする）
run "ブロックパレットの幅と大きさ" node tests/palette-layout.test.mjs

# エミュレーターを使う試験（同じエミュレーターを取り合わないよう、1 つずつ動かす）
run "Firestore のルール" fb emulators:exec --config "$CONFIG" --only firestore --project demo-rules-test "node tests/firestore-rules.test.mjs"
run "Storage のルール" fb emulators:exec --config "$CONFIG" --only firestore,storage --project demo-rules-test "node tests/storage-rules.test.mjs"

# Functions は本番と同じ Node 22 で動かす（firebase CLI を動かしている Node が使われるため）
node_major=$(node -p 'process.versions.node.split(".")[0]')
if [ "$node_major" != "22" ]; then
  echo "注意: 今の Node は $(node -v) です。Functions の試験は Node 22 で動かしてください（README.md を参照）"
fi
run "Cloud Functions" fb emulators:exec --config "$CONFIG" --only auth,firestore,storage,functions --project demo-fn-test "node tests/functions-callable.test.mjs"

# 飛ばした試験があれば、成功・失敗にかかわらず最後に出す
if [ "$skipped" != 0 ]; then echo "飛ばした試験: ${skipped} 件"; fi
if [ "$failed" = 0 ]; then
  if [ "$skipped" = 0 ]; then echo "すべての試験が成功しました"; else echo "失敗した試験はありません"; fi
else echo "失敗した試験があります"; fi
exit "$failed"

# 試験

ルール（Firestore・Storage）、画面のコード（管理画面・生徒の画面・Scratch のエディターのブロックパレット）、Cloud Functions の試験です。
**本番にはつながりません。** Firebase のエミュレーターと、偽物の SDK と、127.0.0.1 だけで開く Google Chrome（見えない形）を使います（プロジェクト ID は `demo-` で始まる架空のものです）。

## 試験の一覧

| ファイル | 確かめること | 使うもの |
|---|---|---|
| `firestore-rules.test.mjs` | users・projects・versions のルール（読み取りの範囲、書き換えてよい項目と型、保存先 URL の場所、作品 ID の注入など） | Firestore エミュレーター |
| `storage-rules.test.mjs` | Storage のルール（講師が触れるのは自教室の生徒の作品だけ、書くのは持ち主だけ、録画は動画だけなど） | Firestore・Storage エミュレーター |
| `admin-delete-project.test.mjs` | 管理画面の作品の削除（作品の場所以外を消さない、ファイルを先に・記録を後に、削除の途中で別の確認を開いても取り違えない） | node:vm と偽物の SDK |
| `student-delete-project.test.mjs` | 生徒の作品の削除（提出済みは止める、記録をまとめて消してからファイルを消す） | node:vm と偽物の SDK |
| `save-new-project.test.mjs` | 新規保存で送る教室 ID（生徒は自分の教室、講師・管理者は null） | node:vm と偽物の SDK |
| `palette-layout.test.mjs` | ブロックパレットの幅（つまみのドラッグ・ダブルクリック・指の操作）と、パレットの中のブロックの大きさ（「ブロックの大きさ」ボタン）。保存と読み込み直し、言語・テーマの切りかえ、2 本目の指、ブロックを運んでいる途中、右から左の言語、micro:bit の接続ボタンなど | 本物の Chrome（見えない形）と playwright-core |
| `minecraft-lamp.test.mjs` | ステージの上の「Minecraft とつながっているか」のランプ（拡張を入れたときだけ出る、灰・黄・緑・赤、切れたら点滅、押すとヒント、言語の切りかえ、小さい画面）。127.0.0.1 の偽物の MOD（WebSocket）につなぐ | 本物の Chrome（見えない形）と playwright-core |
| `minecraft-blocks.test.mjs` | Minecraft のブロックが MOD へ送る命令の中身（ブロックの欄に数が入っても止まらない、小数の座標の切り下げ、「まわりを平らな地面にもどす」の空の中心、時刻の数、はじめの準備、調べるブロックが返す名前、動画の題名）、つないでいないときのランプの点滅と「先につないでね」、メニューの訳（漢字・ひらがな・英語）、代表の文が決めたことばどおりか、案内の文の「」［］の中の名前がブロックの文と同じか、使う訳の鍵が漢字版・ひらがな版にすべてあるか、漢字版・ひらがな版の Minecraft の文ぜんぶとパレットのブロックごとの文・見出し・メニューの項目が記録 `tests/fixtures/minecraft-texts.json` と同じか（ことばを決めて変えたときは `BLOCKS_UPDATE_TEXTS=1` で作り直す。手順は下） | 本物の Chrome（見えない形）と playwright-core。ページの中の WebSocket を偽物の MOD に差し替える |
| `functions-callable.test.mjs` | Cloud Functions（平文のパスワードは生徒だけ保存、講師は生徒しか作れない、録画は公開しない・決まりに合わないものは断る・一覧は先生と管理者だけなど） | Auth・Firestore・Storage・Functions エミュレーター |

## 準備（はじめに 1 回）

- Java 21 以上（エミュレーターに必要）
- firebase CLI
- Node 22（Cloud Functions は本番で Node 22 で動くため）

```bash
cd tests && npm ci && cd ..
cd functions && npm ci && cd ..
```

## 動かし方

```bash
# Node 22 で動かす（mise と Homebrew の firebase を使う場合の例）
FIREBASE_JS="$(brew --prefix firebase-cli)/libexec/lib/node_modules/firebase-tools/lib/bin/firebase.js" \
  mise exec node@22 -- bash tests/run-all.sh
```

- firebase CLI は、自分を動かしている Node で Functions を動かします。Homebrew の firebase は別の Node で動くので、`FIREBASE_JS` に firebase-tools の `lib/bin/firebase.js` を指定して、Node 22 から呼び出してください。ログに `Using node@22 from host.` と出れば Node 22 で動いています。
- 1 つだけ動かすときは `run-all.sh` の中のコマンドを参考にしてください。エミュレーターの試験は同じポートを使うので、同時には動かさないでください。
- 画面のコードの試験は、試す対象のファイルを環境変数で差し替えられます（`ADMIN_JS`、`FI_JS`）。ルールの試験も同じです（`RULES`、`SRULES`）。
- `palette-layout.test.mjs` は Google Chrome を使います（既定は Mac の `/Applications/Google Chrome.app/Contents/MacOS/Google Chrome`。ほかの場所は `CHROME_PATH` に Chrome の実行ファイルを指定）。Chrome が無ければ飛ばし（`run-all.sh` には「飛ばした」と出ます）。リポジトリの `index.html` と `gui.js` を 127.0.0.1 の小さなサーバーから開き、127.0.0.1 以外への通信と `firebase-integration.js` は空の中身に差し替えるので、本番にはつながりません。WebSocket はこの差し替えを通らないので、127.0.0.1 の外へ WebSocket が開いたら試験をしっぱいにします（今の手順では開きません）。試す `gui.js` は `PALETTE_GUI_JS` で差し替えられます。`minecraft-lamp.test.mjs` も同じ決まりで動き（Chrome が無ければ飛ばす）、本物の MOD の代わりに 127.0.0.1 の偽物の MOD を立てます。試す `gui.js` は `LAMP_GUI_JS` で差し替えられます。`minecraft-blocks.test.mjs` も同じ決まりで動き（Chrome が無ければ飛ばす）、ページの中の WebSocket を偽物の MOD に差し替えるので、外へは WebSocket を開きません。試す `gui.js` は `BLOCKS_GUI_JS` で差し替えられます。今までの作品が開けることを守るため、Minecraft のすべてのブロックの opcode・形・入力の名前と種類・メニューの値（文言は除く）を `fixtures/minecraft-blocks-compat.json` と比べます。わざとブロックやメニューの値を変えたときだけ、`BLOCKS_UPDATE_FIXTURE=1` を付けて流すと記録を作り直します（作品が開けなくなる変更なので、作り直す前によく確かめること）。この Mac で 1 回 2〜3 分かかります。

### Minecraft のことばを変えたときの手順（`minecraft-blocks.test.mjs`）

記録の作り直し（`BLOCKS_UPDATE_TEXTS=1`・`BLOCKS_UPDATE_FIXTURE=1`）は、ほかの確かめがぜんぶ通ったときだけ、いちばん最後に書きます。1 つでも失敗していたら書きません。`BLOCKS_GUI_JS` で別の gui.js を指しているとき、試しているあいだに gui.js が変わったとき、記録のファイルかフォルダがシンボリックリンクのときも書きません（リポジトリの記録を試しの gui.js の中身で上書きしない・リンク先を上書きしないため）。書くときは、全部を一時ファイルに書いてから名前をつけかえるので、片方の記録だけが新しくなることはありません。

1. gui.js の漢字版（`ja`）とひらがな版（`ja-Hira`）の訳の文を直す。新しい鍵を足したときは、両方に足す。英語の表にある鍵か、gui.js に `id:` で直接書いた鍵が片方に無いと、「使う訳の鍵が、gui.js の訳の表にすべてある」が失敗する。その場で名前を組み立てる鍵（ブロックの名前・メニューの見出し）は、ひらがな版にだけ無いと「漢字版にある鍵が、ひらがな版にもすべてある」、漢字版にだけ無いと「英語が出ている所が無い」が失敗する。訳があるかは、ページの中ではなく gui.js のソースの訳の表で確かめる（ページの中の訳の表には、訳が無いと英語の初めの文が書きもどされるため）。
2. 試験の中で、文をそのまま書いている所を先に直す（ここが古いままだと、ほかの確かめが失敗して記録を作り直せない）。
   - `minecraft-blocks.test.mjs`: `WANT_MSG`（代表の文）・`EXPECT`（メニューと動画の題名）・`NAMES`（ブロックと生き物の名前）・`LABELS`（見出し）・`REF_KEYS`（案内の文の中にブロックの名前を「」［］で書いている鍵）・`NOT_BLOCK`（ブロックの名前でないかぎかっこ）・動画の題名（`setLocale` の 2 つ目の文と、言語のループの題名）・ランプのヒントの「先にMinecraftにつないでね」「ブロックを動かすと、つながります」（9. のつないでいないときの確かめ）・見出しの部品の確かめの「ブロックを置く」（13. の後ろの labelPath）
   - `minecraft-lamp.test.mjs`: ランプの文とヒントの文
3. `BLOCKS_UPDATE_TEXTS=1 node tests/minecraft-blocks.test.mjs` を流す。前の記録から変わる所が 1 行ずつ出るので、変えたつもりの所だけが変わっているかを確かめる（`palette.…` の行は、パレットのブロックごとの文・見出しの文・メニューの項目。`palette.menu.…` はメニューの値ごとの文）。
4. もう一度、何も付けずに流して、すべて OK になるのを確かめてから、gui.js・試験・記録をいっしょにコミットする。

ブロックやメニューの値（opcode・入力の名前・メニューの値）を変えたときは、`BLOCKS_UPDATE_FIXTURE=1` も付けます。作品の記録は減らさない（減ると今までの作品が開けなくなる）ので、試験の中の `FIXTURE_MIN`（記録のブロックとメニューの数の下限）より少ない記録は失敗にします（作り直して書く記録も同じで、少なければ書きません）。ブロックやメニューを足して記録を作り直したら、`FIXTURE_MIN` も新しい数に上げます。

## 試験が見つける壊れ方の例

- 管理画面で、生徒が書いた保存回数が HTML として表示される（XSS）
- 講師が同じ教室の別の講師のパスワードを読める
- 作品 ID に `|` や `*` を入れて、保存先 URL の確認をすり抜ける
- 先生の削除で、他の子のファイルが消える

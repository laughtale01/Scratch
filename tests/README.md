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
| `minecraft-blocks.test.mjs` | Minecraft のブロックが MOD へ送る命令の中身（ブロックの欄に数が入っても止まらない、小数の座標の切り下げ、「周囲をクリア」の空の中心、時刻の数、学習準備、調べるブロックが返す名前、動画の題名）、つないでいないときのランプの点滅と「先につないでね」、メニューの訳（漢字・ひらがな・英語） | 本物の Chrome（見えない形）と playwright-core。ページの中の WebSocket を偽物の MOD に差し替える |
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
- `palette-layout.test.mjs` は Google Chrome を使います（既定は Mac の `/Applications/Google Chrome.app/Contents/MacOS/Google Chrome`。ほかの場所は `CHROME_PATH` に Chrome の実行ファイルを指定）。Chrome が無ければ飛ばし（`run-all.sh` には「飛ばした」と出ます）。リポジトリの `index.html` と `gui.js` を 127.0.0.1 の小さなサーバーから開き、127.0.0.1 以外への通信と `firebase-integration.js` は空の中身に差し替えるので、本番にはつながりません。WebSocket はこの差し替えを通らないので、127.0.0.1 の外へ WebSocket が開いたら試験をしっぱいにします（今の手順では開きません）。試す `gui.js` は `PALETTE_GUI_JS` で差し替えられます。`minecraft-lamp.test.mjs` も同じ決まりで動き（Chrome が無ければ飛ばす）、本物の MOD の代わりに 127.0.0.1 の偽物の MOD を立てます。試す `gui.js` は `LAMP_GUI_JS` で差し替えられます。`minecraft-blocks.test.mjs` も同じ決まりで動き（Chrome が無ければ飛ばす）、ページの中の WebSocket を偽物の MOD に差し替えるので、外へは WebSocket を開きません。試す `gui.js` は `BLOCKS_GUI_JS` で差し替えられます。この Mac で 1 回 2〜3 分かかります。

## 試験が見つける壊れ方の例

- 管理画面で、生徒が書いた保存回数が HTML として表示される（XSS）
- 講師が同じ教室の別の講師のパスワードを読める
- 作品 ID に `|` や `*` を入れて、保存先 URL の確認をすり抜ける
- 先生の削除で、他の子のファイルが消える

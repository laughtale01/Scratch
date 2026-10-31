# 試験

ルール（Firestore・Storage）、画面のコード（管理画面・生徒の画面）、Cloud Functions の試験です。
**本番にはつながりません。** Firebase のエミュレーターと、偽物の SDK だけを使います（プロジェクト ID は `demo-` で始まる架空のものです）。

## 試験の一覧

| ファイル | 確かめること | 使うもの |
|---|---|---|
| `firestore-rules.test.mjs` | users・projects・versions のルール（読み取りの範囲、書き換えてよい項目と型、保存先 URL の場所、作品 ID の注入など） | Firestore エミュレーター |
| `storage-rules.test.mjs` | Storage のルール（講師が触れるのは自教室の生徒の作品だけ、書くのは持ち主だけ、録画は動画だけなど） | Firestore・Storage エミュレーター |
| `admin-delete-project.test.mjs` | 管理画面の作品の削除（作品の場所以外を消さない、ファイルを先に・記録を後に、削除の途中で別の確認を開いても取り違えない） | node:vm と偽物の SDK |
| `student-delete-project.test.mjs` | 生徒の作品の削除（提出済みは止める、記録をまとめて消してからファイルを消す） | node:vm と偽物の SDK |
| `save-new-project.test.mjs` | 新規保存で送る教室 ID（生徒は自分の教室、講師・管理者は null） | node:vm と偽物の SDK |
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

## 試験が見つける壊れ方の例

- 管理画面で、生徒が書いた保存回数が HTML として表示される（XSS）
- 講師が同じ教室の別の講師のパスワードを読める
- 作品 ID に `|` や `*` を入れて、保存先 URL の確認をすり抜ける
- 先生の削除で、他の子のファイルが消える

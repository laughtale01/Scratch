# Minecraft公式翻訳機能を利用したブロック名表示 実装計画書 (改訂版)

## 1. 概要
MinecraftEdu MODにおいて、Scratchからブロックを調査した際、Minecraftのチャット欄にブロック名を「プレイヤーの言語設定に合わせて」自動的に表示する機能を実装する。
独自辞書クラス（A案）は廃止し、Minecraft本体の標準機能である `Component.translatable`（C案）を採用する。

---

## 2. 現状分析 (As-Is)

### 2.1 辞書クラス (`BlockNameTranslator.java`)
*   **現状**: 手動で登録された日本語マッピングを持つ。
*   **課題**: 網羅性が低く、多言語対応のためには膨大な辞書データの管理が必要となる。

### 2.2 コマンド実行クラス (`CommandExecutor.java`)
*   **現状**:
    *   `BlockNameTranslator.getJapaneseName()` で日本語名を取得。
    *   チャット送信: `[MinecraftEdu]` プレフィックス付きで、日本語名と座標を表示。
    *   JSONレスポンス (`lastResult`): `blockType` (英語ID) と `blockTypeJapanese` (日本語名) の両方を含んでいる。

---

## 3. 実装計画 (To-Be)

### 3.1 Phase 1: 独自辞書の廃止
*   **アクション**: `minecraft-mod/src/main/java/com/github/minecraftedu/util/BlockNameTranslator.java` を削除する。

### 3.2 Phase 2: コマンド実行ロジックの変更 (`CommandExecutor.java`)

#### 変更点 1: インポートの整理
不要になる `BlockNameTranslator` のインポートを削除。

#### 変更点 2: `executeGetBlockType` メソッドの修正
*   **チャット表示**:
    *   ブロックから翻訳キー（`block.getDescriptionId()`）を取得。
    *   `Component.translatable(key)` を生成し、`sendRawChatMessage` で送信。
    *   表示形式は「ブロック名のみ」（座標やプレフィックスなし）。
*   **JSONレスポンス (`lastResult`)**:
    *   `blockType` (英語ID): 維持。
    *   `blockTypeJapanese` (日本語名): **廃止（削除）**。
        *   理由: サーバー側で翻訳を行わない（クライアント依存にする）ため、日本語文字列を取得できなくなるため。
        *   影響: Scratch側でこのプロパティを使用している場合、修正が必要になる可能性がある（現状の使用状況から問題ないと判断）。

#### 変更点 3: `sendRawChatMessage` メソッドの追加
```java
/**
 * コンポーネント（翻訳テキスト等）を署名なしで直接送信する
 */
private void sendRawChatMessage(net.minecraft.network.chat.Component message) {
    server.execute(() -> {
        server.getPlayerList().getPlayers().forEach(player -> {
            player.sendSystemMessage(message);
        });
    });
}
```

#### 変更点 4: ログ出力の修正
ログメッセージから削除される変数 `japaneseName` の参照を除去し、英語IDのみを出力する。

---

## 4. 検証とリスク評価

### 4.1 表示の検証
*   **Minecraftチャット**:
    *   日本語設定のプレイヤー → 「石」
    *   英語設定のプレイヤー → 「Stone」
    *   Mod未導入の言語 → 翻訳キーまたは英語名（Fallback）

### 4.2 API変更の影響 (JSONレスポンス)
*   **変更**: `lastResult` から `blockTypeJapanese` フィールドが消滅する。
*   **リスク**: Scratchクライアント側で `response.blockTypeJapanese` を参照しているロジックがある場合、`undefined` になる。
*   **対策**: 必要であればScratch側で英語ID (`blockType`) を元に翻訳するロジックを実装する（今回のスコープ外）。

---

## 5. 作業手順

1.  `minecraft-mod/src/main/java/com/github/minecraftedu/util/BlockNameTranslator.java` の削除。
2.  `minecraft-mod/src/main/java/com/github/minecraftedu/commands/CommandExecutor.java` の修正。
    *   インポート削除
    *   `sendRawChatMessage` 追加
    *   `executeGetBlockType` 修正（ロジック変更、JSON構築修正）
3.  `./gradlew build` でビルド実行。

作成日: 2026年2月3日
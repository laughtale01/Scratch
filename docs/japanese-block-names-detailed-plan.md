# 日本語ブロック名表示機能 詳細実装計画書

## 1. 概要
MinecraftEdu MODにおいて、Scratchからブロックを調査した際、Minecraftのチャット欄に「公式の日本語ブロック名のみ」を表示する機能を実装する。
現在は一部のブロックのみ手動で翻訳されており、表示形式も座標などの付加情報が含まれている。これをMinecraft Java Edition 1.20.1の公式言語データを元に網羅的な辞書へ更新し、表示形式を簡素化する。

---

## 2. 現状分析 (As-Is)

### 2.1 辞書クラス (`BlockNameTranslator.java`)
*   **現状**: 手動で約350種類の主要ブロックを `HashMap` に登録。
*   **問題点**:
    *   Minecraft 1.20.1の全ブロック（約1,100種以上）を網羅できていない。
    *   手動登録のため、表記ゆれや翻訳ミスのリスクがある。
    *   未登録のブロックは英語の内部ID（例: `pink_petals`）がそのまま表示される。

### 2.2 コマンド実行クラス (`CommandExecutor.java`)
*   **現状**:
    *   `sendChatMessage` メソッドを使用しており、全メッセージに自動的に `[MinecraftEdu]` プレフィックスが付与される。
    *   `executeGetBlockType` メソッド内で、以下のようなフォーマット済み文字列を作成している。
        ```text
        "ブロック: " + japaneseName + " (" + x + ", " + y + ", " + z + ")"
        ```
*   **出力例**: `[MinecraftEdu] ブロック: 石 (10, 64, -5)`

---

## 3. 実装計画 (To-Be)

### 3.1 Phase 1: 翻訳辞書の完全化 (`BlockNameTranslator.java`)
Minecraft公式の `ja_jp.json` (Java Edition 1.20.1) データに基づき、辞書クラスを再生成する。

*   **変更内容**: `static` イニシャライザ内の `put` メソッドを、全ブロック分（約1,200行想定）に置き換える。
*   **データソース**: MC 1.20.1 公式言語ファイル
*   **カバー範囲**:
    *   基本ブロック（土、石、木材等）
    *   装飾ブロック（羊毛、カーペット、彩釉テラコッタ等 全色）
    *   機能ブロック（レッドストーン関連、ワークステーション等）
    *   植物・自然（サクラ、マングローブ、スニッファー関連等の1.20新要素含む）
*   **未知のブロックへの対応**:
    *   Mod等で追加された辞書にないブロックは、デバッグの利便性を考慮しつつ、原則英語IDを返す（または要件に応じて「不明なブロック」とするが、今回は基本バニラブロックの網羅を優先）。

### 3.2 Phase 2: 表示ロジックの簡素化 (`CommandExecutor.java`)
「ブロック名のみ」を表示するため、メッセージ送信処理を変更する。

#### 手順 2-1: `sendRawChatMessage` メソッドの追加
プレフィックス `[MinecraftEdu]` を付与しない、生のメッセージ送信メソッドを追加する。

```java
/**
 * プレフィックスなしでチャットメッセージを送信する
 * ブロック名表示など、シンプルな表示が必要な場合に使用
 */
private void sendRawChatMessage(String message) {
    server.execute(() -> {
        server.getPlayerList().getPlayers().forEach(player -> {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(message));
        });
    });
}
```

#### 手順 2-2: `executeGetBlockType` の修正
メッセージ構築ロジックと送信メソッド呼び出しを変更する。

*   **修正前**:
    ```java
    sendChatMessage("ブロック: " + japaneseName + " (" + x + ", " + y + ", " + z + ")");
    ```
*   **修正後**:
    ```java
    // 座標情報や "ブロック:" の前置きを削除し、名前のみを送信
    sendRawChatMessage(japaneseName);
    ```

---

## 4. 影響範囲と検証

### 4.1 影響範囲
*   **`minecraft-mod`**: Javaソースコードのみ修正。
*   **`scratch-client`**: 変更なし（MODからのレスポンスJSON構造 `blockType`, `position` 等は変更しないため、Scratch側のブロック動作には影響しない）。

### 4.2 検証項目
ビルド後、以下のブロックを調査してチャット表示を確認する。

| テスト対象 | 期待される表示（チャット欄） | 備考 |
|------------|----------------------------|------|
| 石 | `石` | 基本ブロック |
| 桃色の羊毛 | `桃色の羊毛` | 色付きブロック |
| サクラのボタン | `サクラのボタン` | 1.20追加ブロック |
| 模様入りの本棚 | `模様入りの本棚` | 1.20追加ブロック |
| ピストン (伸張時) | `ピストンヘッド` または `ピストン` | 内部状態を持つブロックの確認 |

---

## 5. 作業手順

1.  `docs/japanese-block-names-detailed-plan.md` (本文書) の承認。
2.  `BlockNameTranslator.java` を全ブロックデータで上書き。
3.  `CommandExecutor.java` に `sendRawChatMessage` を追加し、`executeGetBlockType` を修正。
4.  `./gradlew build` でビルド実行。
5.  成果物 JAR の配置と動作確認。

作成日: 2026年2月3日

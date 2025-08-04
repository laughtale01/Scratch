# 🔧 テスト修正計画

## 問題の分析

### 1. パッケージ構造の不一致
```
期待: edu.minecraft.collaboration.collaboration.Invitation
実際: com.yourname.minecraftcollaboration.collaboration.Invitation
```

### 2. APIの不一致
- `getName()` が `Component` を返すが、テストは `String` を期待
- `CollaborationCoordinator` のコンストラクタ引数の不一致
- 存在しないメソッドの呼び出し

### 3. 型の不一致
- `boolean` vs `CompletableFuture<Boolean>`
- `Component` vs `String`

## 修正手順

### Phase 1: パッケージ名の統一
1. すべてのテストファイルのimportを修正
2. `edu.minecraft.collaboration` → `com.yourname.minecraftcollaboration`

### Phase 2: API修正
1. `getName()` の戻り値を適切に処理
2. `CollaborationCoordinator` のコンストラクタ修正
3. 存在しないメソッドの実装または削除

### Phase 3: 型の修正
1. 非同期メソッドの戻り値を統一
2. Minecraft APIに合わせた型変換

## 優先順位
1. 🔴 最優先: パッケージ名の修正（全32エラーに影響）
2. 🟡 高優先: API不一致の修正
3. 🟢 中優先: 型の不一致修正
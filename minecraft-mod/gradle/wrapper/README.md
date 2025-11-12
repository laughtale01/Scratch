# Gradle Wrapper JAR について

## 現在の状態

このディレクトリには以下のファイルが必要です：

- ✅ `gradle-wrapper.properties` - 設定ファイル（作成済み）
- ❌ `gradle-wrapper.jar` - Gradleランナー（未作成）

## gradle-wrapper.jar の取得方法

### 方法1: Gradleがインストールされている場合（推奨）

プロジェクトルートで以下を実行：

```bash
cd minecraft-mod
gradle wrapper --gradle-version 8.4
```

これにより `gradle-wrapper.jar` が自動生成されます。

### 方法2: 手動ダウンロード

1. 以下のURLから直接ダウンロード：
   https://raw.githubusercontent.com/gradle/gradle/master/gradle/wrapper/gradle-wrapper.jar

2. このディレクトリに配置：
   ```
   minecraft-mod/gradle/wrapper/gradle-wrapper.jar
   ```

### 方法3: 既存のGradleプロジェクトからコピー

任意のGradleプロジェクトの `gradle/wrapper/gradle-wrapper.jar` をコピー。

## 確認方法

JARファイルが正しく配置されたら、以下で確認：

```bash
cd minecraft-mod
./gradlew --version  # Linux/Mac
gradlew.bat --version  # Windows
```

正常に動作すれば、Gradleのバージョン情報が表示されます。

## トラブルシューティング

### エラー: "Error: Could not find or load main class org.gradle.wrapper.GradleWrapperMain"

→ `gradle-wrapper.jar` が見つかりません。上記の方法で取得してください。

### エラー: "JAVA_HOME is not set"

→ Java 17をインストールし、環境変数を設定してください。

---

**注意**: このファイルはGitリポジトリに含めることが一般的ですが、サイズの関係で省略される場合もあります。
初回ビルド時に自動生成されるため、問題ありません。

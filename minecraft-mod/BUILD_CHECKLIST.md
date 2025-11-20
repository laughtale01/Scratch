# ビルド前チェックリスト

ビルドを実行する前に、以下を確認してください。

## 必須環境

### Java

- [ ] Java JDK 17がインストールされている

```bash
java -version
# 期待される出力: java version "17.x.x"
```

- [ ] `JAVA_HOME`環境変数が設定されている

```bash
# Windows
echo %JAVA_HOME%

# macOS/Linux
echo $JAVA_HOME

# 期待される出力: /path/to/jdk-17
```

### Gradle Wrapper

- [ ] `gradlew` または `gradlew.bat` が存在する
- [ ] `gradle/wrapper/gradle-wrapper.properties` が存在する

**注意**: `gradle-wrapper.jar` は初回ビルド時に自動生成されます。

## ファイル構造確認

```
minecraft-mod/
├── build.gradle              ✓ ビルド設定
├── gradle.properties         ✓ Gradle設定
├── settings.gradle           ✓ プロジェクト設定
├── gradlew                   ✓ Gradleラッパー（Unix）
├── gradlew.bat               ✓ Gradleラッパー（Windows）
├── gradle/
│   └── wrapper/
│       └── gradle-wrapper.properties  ✓
└── src/
    └── main/
        ├── java/             ✓ Javaソースコード
        └── resources/        ✓ リソースファイル
            └── META-INF/
                └── mods.toml ✓ MOD情報
```

## 初回ビルド

初回は依存関係のダウンロードに時間がかかります（5-15分）。

### ステップ1: Gradle Wrapperの準備（オプション）

gradle-wrapper.jarがない場合、以下のいずれかを実行：

**方法A: Gradleがインストールされている場合**

```bash
gradle wrapper --gradle-version 8.4
```

**方法B: wrapper jarなしでビルド**

最初のビルドコマンドで自動的にダウンロードされます。

### ステップ2: ビルド実行

```bash
# Windows
gradlew.bat build

# macOS/Linux
./gradlew build
```

### ステップ3: 結果確認

ビルドが成功すると：

```
BUILD SUCCESSFUL in Xm Xs
```

生成されたファイル：
```
build/libs/minecraftedu-mod-0.1.0-1.20.1.jar
```

## よくあるエラーと対処法

### エラー1: "JAVA_HOME is not set"

**原因**: Java環境変数が設定されていない

**対処法**:
```bash
# Windows（管理者権限のコマンドプロンプト）
setx JAVA_HOME "C:\Program Files\Java\jdk-17"

# macOS/Linux（~/.bashrc または ~/.zshrc に追加）
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
export PATH=$JAVA_HOME/bin:$PATH
```

### エラー2: "Could not find or load main class org.gradle.wrapper.GradleWrapperMain"

**原因**: gradle-wrapper.jarが存在しない

**対処法**:
```bash
# Gradleをインストールして実行
gradle wrapper --gradle-version 8.4
./gradlew build
```

### エラー3: "Connection timeout" や "Could not resolve dependencies"

**原因**: ネットワーク接続の問題、または依存関係サーバーへのアクセス制限

**対処法**:
```bash
# プロキシ設定（必要な場合）
# gradle.properties に追加:
# systemProp.http.proxyHost=proxy.company.com
# systemProp.http.proxyPort=8080

# または、リトライ
./gradlew build --refresh-dependencies
```

### エラー4: "Execution failed for task ':compileJava'"

**原因**: Javaのバージョン不一致、またはソースコードのエラー

**対処法**:
```bash
# Javaバージョン確認
java -version  # 17.x.x であることを確認

# クリーンビルド
./gradlew clean build
```

## ビルドオプション

### クリーンビルド

以前のビルド成果物を削除してから再ビルド：

```bash
./gradlew clean build
```

### 依存関係の強制更新

```bash
./gradlew build --refresh-dependencies
```

### デバッグ情報付きビルド

```bash
./gradlew build --info
```

### 詳細ログ

```bash
./gradlew build --debug > build.log 2>&1
```

## 次のステップ

ビルドが成功したら：

1. ✅ `build/libs/minecraftedu-mod-0.1.0-1.20.1.jar` が生成されている
2. ➡️ Minecraft Forgeをインストール
3. ➡️ JARファイルを `mods` フォルダにコピー
4. ➡️ Minecraftを起動して動作確認

詳細は `../NEXT_STEPS.md` を参照してください。

# Firebase クラウドセーブシステム - 仕様書

**作成日**: 2025-12-17
**更新日**: 2025-12-18
**プロジェクト**: Scratch + Minecraft 教育用プラットフォーム
**Firebase プロジェクトID**: laughtale-scratch-bcc8a

---

## 目次

1. [概要](#概要)
2. [ユーザー構成](#ユーザー構成)
3. [権限マトリクス](#権限マトリクス)
4. [認証方式](#認証方式)
5. [プロジェクト保存](#プロジェクト保存)
6. [UI・言語設定](#ui言語設定)
7. [管理機能](#管理機能)
8. [追加機能](#追加機能)
9. [Firebase設定状況](#firebase設定状況)
10. [技術仕様](#技術仕様)
11. [今後の作業予定](#今後の作業予定)

---

## 概要

Scratch + Minecraft 教育用プラットフォームにおいて、生徒のプロジェクトをクラウドに保存し、どのPCからでもアクセスできるようにするシステム。

### 主な機能
- クラウドへのプロジェクト保存・読み込み
- ユーザー認証（生徒・講師・管理者）
- バージョン管理（ロールバック機能）
- プロジェクト提出機能
- ギャラリー・発表モード

---

## ユーザー構成

### 規模
| 項目 | 内容 |
|------|------|
| 生徒数 | 31〜50名 |
| 教室数 | 1〜3教室（動的に増減可能） |

### 階層構造
```
管理者（正）
├── 講師A（副）── 教室A の生徒たち
├── 講師B（副）── 教室B の生徒たち
└── 講師C（副）── 教室C の生徒たち
```

### ロール定義
| ロール | 説明 |
|--------|------|
| admin | 管理者（正）。システム全体を管理 |
| teacher | 講師（副）。担当教室を管理 |
| student | 生徒。自分のプロジェクトを管理 |

---

## 権限マトリクス

| 操作 | admin | teacher | student |
|------|:-----:|:-------:|:-------:|
| 全教室閲覧 | ✅ | ✅（閲覧のみ） | ❌ |
| 自教室編集 | ✅ | ✅ | - |
| アカウント作成 | ✅ | ❌ | ❌ |
| アカウント削除 | ✅ | ❌ | ❌ |
| パスワードリセット（全員） | ✅ | ❌ | ❌ |
| パスワードリセット（自教室） | ✅ | ✅ | ❌ |
| 教室の作成・削除 | ✅ | ❌ | ❌ |
| プロジェクト閲覧（全員） | ✅ | ✅（閲覧のみ） | ❌ |
| プロジェクト編集（自教室） | ✅ | ✅ | ❌ |
| プロジェクト削除（自分） | ✅ | ✅ | ✅ |
| バージョン閲覧 | ✅ | ✅ | ❌ |
| バージョン復元（ロールバック） | ✅ | ✅ | ❌ |
| 統計情報閲覧 | ✅ | ✅（自教室のみ） | ❌ |

---

## 認証方式

### 基本設定
| 項目 | 内容 |
|------|------|
| 認証サービス | Firebase Authentication |
| 認証方式 | メール/パスワード |
| メール確認 | 無効（架空ドメイン使用のため） |

### ユーザーID形式
| 項目 | 内容 |
|------|------|
| 表示用ID | 自由形式（例: `tanaka123`） |
| 内部メール | `{ユーザーID}@laughtale.local` |
| ドメイン | `laughtale.local`（架空ドメイン） |

### 運用フロー
1. **アカウント作成**: 管理者がユーザーID・初期パスワードを設定
2. **ログイン**: 生徒はユーザーIDとパスワードのみ入力（@以降は自動付与）
3. **パスワードリセット**: 管理者または講師が管理画面から実行

---

## プロジェクト保存

### 基本仕様
| 項目 | 内容 |
|------|------|
| 保存数上限 | 無制限 |
| ファイル形式 | .sb3（Scratch標準形式） |
| 保存先 | Firebase Storage |
| メタデータ保存先 | Firebase Firestore |

### 保存方式
| 方式 | 説明 |
|------|------|
| 上書き保存 | 既存プロジェクトを更新 |
| 新規保存 | 新しいプロジェクトとして保存 |

### バージョン管理
| 項目 | 内容 |
|------|------|
| 保持バージョン数 | 最新3バージョン |
| ロールバック権限 | admin, teacher のみ |
| 自動削除 | 4つ目のバージョン保存時に最古を削除 |

### データ構造（Firestore）

```
/users/{userId}
  - displayName: string
  - email: string
  - role: "admin" | "teacher" | "student"
  - classroomId: string
  - createdAt: timestamp
  - lastLoginAt: timestamp
  - saveCount: number

/classrooms/{classroomId}
  - name: string
  - teacherId: string
  - createdAt: timestamp

/projects/{projectId}
  - userId: string
  - classroomId: string
  - name: string
  - description: string
  - thumbnailUrl: string
  - storageUrl: string
  - createdAt: timestamp
  - updatedAt: timestamp
  - isSubmitted: boolean
  - submittedAt: timestamp | null

/projects/{projectId}/versions/{versionId}
  - storageUrl: string
  - createdAt: timestamp
  - size: number
```

### ストレージ構造（Firebase Storage）

```
/projects/{userId}/{projectId}/
  - current.sb3          # 現在のバージョン
  - versions/
    - v1.sb3
    - v2.sb3
    - v3.sb3
  - thumbnail.png        # サムネイル画像
```

---

## UI・言語設定

### 言語対応
| 言語 | 対応状況 |
|------|---------|
| 日本語 | ✅ 対応 |
| 英語 | ✅ 対応 |

### ログイン画面表示
| オプション | 説明 |
|-----------|------|
| ページ読み込み時 | 起動時にログイン画面を表示 |
| ボタン表示 | メニューにサインインボタンを表示 |

※設定で切り替え可能

### 未ログイン時の動作
- Scratchは使用可能
- ローカル保存（コンピューターに保存）のみ利用可
- クラウド保存は不可

---

## 管理機能

### 管理画面
| 項目 | 内容 |
|------|------|
| URL | `/admin` |
| アクセス権限 | admin, teacher |

### 機能一覧

#### ユーザー管理
- ユーザー一覧表示
- ユーザー作成（adminのみ）
- ユーザー編集
- パスワードリセット
- ユーザー削除（adminのみ）

#### 教室管理
- 教室一覧表示
- 教室作成（adminのみ）
- 教室編集
- 教室削除（adminのみ）
- 担当講師の割り当て

#### プロジェクト管理
- プロジェクト一覧表示（フィルタ: 教室、生徒）
- プロジェクト詳細表示
- バージョン履歴表示
- ロールバック実行
- プロジェクト削除

#### 統計情報
| 項目 | 説明 |
|------|------|
| 最終ログイン日時 | 各ユーザーの最終ログイン |
| 保存回数 | 各ユーザーの累計保存回数 |
| プロジェクト数 | 各ユーザーのプロジェクト数 |
| 提出状況 | 提出済みプロジェクトの一覧 |

---

## 追加機能

### プロジェクト提出機能
| 項目 | 内容 |
|------|------|
| 概要 | 生徒が完成作品を講師に提出 |
| 操作 | 「提出」ボタンをクリック |
| 状態管理 | `isSubmitted` フラグで管理 |
| 表示 | 講師の管理画面に提出済み一覧 |

### ギャラリー・発表モード
| 項目 | 内容 |
|------|------|
| 概要 | 提出されたプロジェクトを一覧表示 |
| 用途 | 授業での発表、作品鑑賞会 |
| 表示形式 | サムネイルグリッド表示 |
| 機能 | クリックでプロジェクト実行 |

---

## Firebase設定状況

### サービス有効化状況
| サービス | 状態 | 設定内容 |
|---------|:----:|---------|
| Authentication | ✅ 完了 | メール/パスワード認証を有効化 |
| Firestore | ✅ 完了 | スタンダードエディション、asia-northeast1 |
| Storage | ✅ 完了 | Blazeプランでセットアップ済み |

### プラン
| 項目 | 状態 |
|------|------|
| プラン | Blaze（従量課金） |

### Firebase設定情報
```javascript
const firebaseConfig = {
  apiKey: "AIzaSyBEr2LmctKmBHa_y1Jrx6XQ_6M8vengYhs",
  authDomain: "laughtale-scratch-bcc8a.firebaseapp.com",
  projectId: "laughtale-scratch-bcc8a",
  storageBucket: "laughtale-scratch-bcc8a.firebasestorage.app",
  messagingSenderId: "869020754126",
  appId: "1:869020754126:web:e80beaf67fda60987ff09f",
  measurementId: "G-F8JCCJQ7YB"
};
```

### 予想コスト（Blazeプラン）
50名 × 10プロジェクト × 500KB = 約250MB

| サービス | 無料枠 | 予想使用量 | 費用 |
|---------|-------|-----------|------|
| Storage | 5GB | ~250MB | 無料 |
| Firestore 読み取り | 50,000回/日 | ~1,000回/日 | 無料 |
| Firestore 書き込み | 20,000回/日 | ~200回/日 | 無料 |

**予想月額: 0円〜数十円程度**

---

## 技術仕様

### 使用技術
| 項目 | 技術 |
|------|------|
| Frontend | Scratch 3.0 (React), JavaScript |
| Backend | Firebase (BaaS) |
| 認証 | Firebase Authentication |
| データベース | Firebase Firestore |
| ストレージ | Firebase Storage |
| ホスティング | GitHub Pages |

### Firebase SDK
```javascript
// 使用予定のFirebase SDKモジュール
import { initializeApp } from 'firebase/app';
import { getAuth, signInWithEmailAndPassword, signOut } from 'firebase/auth';
import { getFirestore, collection, doc, getDoc, setDoc, query, where } from 'firebase/firestore';
import { getStorage, ref, uploadBytes, getDownloadURL } from 'firebase/storage';
```

### セキュリティルール（予定）

#### Firestore Rules
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // ユーザーは自分のデータのみ読み書き可能
    // admin/teacherは権限に応じてアクセス可能
    // 詳細は実装時に設定
  }
}
```

#### Storage Rules
```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    // ユーザーは自分のフォルダのみアクセス可能
    // admin/teacherは権限に応じてアクセス可能
    // 詳細は実装時に設定
  }
}
```

---

## 今後の作業予定

### Phase 1: Firebase設定完了
- [ ] Blazeプランにアップグレード
- [ ] Storage を有効化
- [ ] Firebase設定情報（apiKey等）を取得
- [ ] Firestoreセキュリティルールを設定
- [ ] Storageセキュリティルールを設定

### Phase 2: 基本機能実装
- [ ] gui.js に Firebase SDK を統合
- [ ] ログインUI を実装
- [ ] クラウド保存機能を実装
- [ ] クラウド読み込み機能を実装

### Phase 3: 管理機能実装
- [ ] 管理画面（/admin）を作成
- [ ] ユーザー管理機能を実装
- [ ] 教室管理機能を実装
- [ ] 統計情報表示を実装

### Phase 4: 追加機能実装
- [ ] バージョン管理・ロールバック機能を実装
- [ ] プロジェクト提出機能を実装
- [ ] ギャラリー・発表モードを実装

### Phase 5: テスト・調整
- [ ] 各機能の動作テスト
- [ ] セキュリティテスト
- [ ] UIの調整・多言語対応確認

---

## 変更履歴

| 日付 | 内容 |
|------|------|
| 2025-12-17 | 初版作成。ヒアリングに基づき仕様を策定 |

---

## 参考リンク

- [Firebase Console](https://console.firebase.google.com/u/2/project/laughtale-scratch-bcc8a/overview)
- [Firebase Authentication ドキュメント](https://firebase.google.com/docs/auth)
- [Firebase Firestore ドキュメント](https://firebase.google.com/docs/firestore)
- [Firebase Storage ドキュメント](https://firebase.google.com/docs/storage)

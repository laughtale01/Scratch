# 教育コンテンツ機能 詳細設計書

## 目次

1. [概要](#概要)
2. [チュートリアルシステム](#チュートリアルシステム)
3. [課題システム](#課題システム)
4. [進捗管理システム](#進捗管理システム)
5. [バッジ・達成システム](#バッジ達成システム)
6. [レポート生成](#レポート生成)
7. [データモデル](#データモデル)
8. [UI/UX設計](#uiux設計)
9. [実装ガイド](#実装ガイド)
10. [コンテンツ作成ガイド](#コンテンツ作成ガイド)

---

## 概要

### 教育機能の目的

1. **段階的学習**: 初心者から上級者まで対応したステップバイステップの学習
2. **自動評価**: 学習者の操作を自動的に評価し、即座にフィードバック
3. **進捗可視化**: 学習の進み具合を分かりやすく表示
4. **モチベーション維持**: バッジやポイントでゲーミフィケーション
5. **教師支援**: 課題管理、進捗確認、評価の効率化

### 主要コンポーネント

```
┌─────────────────────────────────────────────────────────┐
│                  教育コンテンツ管理層                      │
├─────────────┬─────────────┬─────────────┬──────────────┤
│ Tutorial    │ Challenge   │ Progress    │ Badge        │
│ System      │ System      │ Tracker     │ System       │
└──────┬──────┴──────┬──────┴──────┬──────┴──────┬───────┘
       │             │             │             │
┌──────▼─────────────▼─────────────▼─────────────▼───────┐
│               ValidationEngine                          │
│    学習者の操作を検証し、フィードバックを提供             │
└──────┬──────────────────────────────────────────────────┘
       │
┌──────▼──────────────────────────────────────────────────┐
│                DataPersistenceLayer                      │
│          JSON / SQLite / PostgreSQL                      │
└──────────────────────────────────────────────────────────┘
```

---

## チュートリアルシステム

### 設計目標

- 初心者でも迷わず進められるガイド付き学習
- 各ステップの自動検証
- コンテキストに応じたヒント提供
- 段階的な難易度上昇

### チュートリアル構造

#### Tutorial データモデル

```java
public class Tutorial {
    private String tutorialId;           // 一意識別子
    private String title;                // タイトル
    private String description;          // 説明
    private DifficultyLevel difficulty;  // 難易度
    private int estimatedMinutes;        // 想定所要時間
    private List<String> prerequisites;  // 前提チュートリアルID
    private List<TutorialStep> steps;    // ステップリスト
    private TutorialMetadata metadata;   // メタデータ

    // ゲッター・セッター
}

public enum DifficultyLevel {
    BEGINNER(1, "初級", "🟢"),
    INTERMEDIATE(2, "中級", "🟡"),
    ADVANCED(3, "上級", "🔴"),
    EXPERT(4, "エキスパート", "⚫");

    private final int level;
    private final String displayName;
    private final String emoji;
}

public class TutorialStep {
    private int stepId;                  // ステップ番号
    private String title;                // ステップタイトル
    private String instruction;          // 指示文
    private String hint;                 // ヒント
    private List<String> imageUrls;      // 説明画像URL
    private ValidationRule validation;   // 検証ルール
    private StepReward reward;           // 報酬
    private int maxAttempts;             // 最大試行回数（0=無制限）
}

public class ValidationRule {
    private ValidationType type;         // 検証タイプ
    private Map<String, Object> params;  // パラメータ

    public enum ValidationType {
        CHAT_MESSAGE,          // チャット送信
        BLOCK_PLACED,          // ブロック配置
        BLOCK_PATTERN,         // パターンマッチング
        ENTITY_SUMMONED,       // エンティティ召喚
        PLAYER_POSITION,       // プレイヤー位置
        ITEM_IN_INVENTORY,     // アイテム所持
        CODE_STRUCTURE,        // Scratchコード構造
        CUSTOM_SCRIPT          // カスタムスクリプト
    }
}

public class StepReward {
    private int points;                  // 獲得ポイント
    private String badgeId;              // 獲得バッジID（optional）
    private String message;              // 報酬メッセージ
}
```

### チュートリアル例（JSON）

```json
{
  "tutorialId": "beginner_001_hello_world",
  "title": "はじめてのプログラミング：Hello World",
  "description": "Scratchを使ってMinecraftにメッセージを送ってみよう",
  "difficulty": "BEGINNER",
  "estimatedMinutes": 10,
  "prerequisites": [],
  "metadata": {
    "author": "MinecraftEdu Team",
    "version": "1.0",
    "tags": ["初心者", "チャット", "基本"],
    "thumbnailUrl": "/assets/tutorials/beginner_001.png"
  },
  "steps": [
    {
      "stepId": 1,
      "title": "Scratchを開こう",
      "instruction": "ブラウザでScratchのページを開いてください。左側に緑色の「Minecraft」カテゴリがあることを確認しましょう。",
      "hint": "もし「Minecraft」カテゴリが見えない場合は、「拡張機能を追加」ボタンをクリックして追加してください。",
      "imageUrls": ["/assets/tutorials/scratch_interface.png"],
      "validation": {
        "type": "MANUAL_CHECK",
        "params": {}
      },
      "reward": {
        "points": 5,
        "message": "よくできました！Scratchの準備ができました。"
      },
      "maxAttempts": 0
    },
    {
      "stepId": 2,
      "title": "Minecraftに接続しよう",
      "instruction": "「Minecraftに接続」ブロックをワークスペースにドラッグして、緑の旗をクリックしてください。",
      "hint": "ホストは「localhost」、ポートは「14711」のままでOKです。",
      "imageUrls": ["/assets/tutorials/connect_block.png"],
      "validation": {
        "type": "CONNECTION_ESTABLISHED",
        "params": {
          "timeout": 30000
        }
      },
      "reward": {
        "points": 10,
        "badgeId": "connected",
        "message": "接続成功！Minecraftと通信できるようになりました。"
      },
      "maxAttempts": 5
    },
    {
      "stepId": 3,
      "title": "メッセージを送ろう",
      "instruction": "「チャットで言う」ブロックを使って、「Hello, Minecraft!」というメッセージを送ってみましょう。",
      "hint": "ブロックをつなげて、テキスト欄に「Hello, Minecraft!」と入力してね。",
      "imageUrls": ["/assets/tutorials/chat_block.png"],
      "validation": {
        "type": "CHAT_MESSAGE",
        "params": {
          "expectedText": "Hello, Minecraft!",
          "caseSensitive": false
        }
      },
      "reward": {
        "points": 15,
        "badgeId": "first_chat",
        "message": "やったね！初めてのメッセージ送信成功です！🎉"
      },
      "maxAttempts": 3
    },
    {
      "stepId": 4,
      "title": "ブロックを置こう",
      "instruction": "プレイヤーの目の前（相対座標 ~0 ~1 ~2）に石ブロックを置いてみましょう。",
      "hint": "「ブロックを置く」ブロックで、座標を「~0」「~1」「~2」に設定し、ブロックタイプを「stone」に選択してください。",
      "imageUrls": ["/assets/tutorials/place_block.png"],
      "validation": {
        "type": "BLOCK_PLACED",
        "params": {
          "blockType": "minecraft:stone",
          "relativePosition": [0, 1, 2],
          "tolerance": 0
        }
      },
      "reward": {
        "points": 20,
        "badgeId": "first_builder",
        "message": "すごい！初めてブロックを置きました！建築家への道が始まります。"
      },
      "maxAttempts": 5
    }
  ]
}
```

### TutorialManager（Java実装）

```java
public class TutorialManager {

    private final Map<String, Tutorial> tutorials;
    private final Map<String, TutorialProgress> userProgress;
    private final ValidationEngine validationEngine;
    private final EventBroadcaster broadcaster;

    public TutorialManager(
        TutorialRepository repository,
        ValidationEngine validationEngine,
        EventBroadcaster broadcaster
    ) {
        this.tutorials = repository.loadAll();
        this.userProgress = new ConcurrentHashMap<>();
        this.validationEngine = validationEngine;
        this.broadcaster = broadcaster;
    }

    /**
     * チュートリアル開始
     */
    public TutorialStartResult startTutorial(
        ClientSession session,
        String tutorialId
    ) {
        Tutorial tutorial = tutorials.get(tutorialId);
        if (tutorial == null) {
            return TutorialStartResult.error("チュートリアルが見つかりません");
        }

        // 前提条件チェック
        for (String prerequisiteId : tutorial.getPrerequisites()) {
            if (!isCompleted(session.getClientId(), prerequisiteId)) {
                return TutorialStartResult.error(
                    "先に「" + tutorials.get(prerequisiteId).getTitle() + "」を完了してください"
                );
            }
        }

        // 進捗初期化
        TutorialProgress progress = new TutorialProgress(
            session.getClientId(),
            tutorialId,
            tutorial.getSteps().size()
        );
        progress.setCurrentStep(1);
        userProgress.put(getProgressKey(session.getClientId(), tutorialId), progress);

        // 最初のステップを返す
        TutorialStep firstStep = tutorial.getSteps().get(0);

        broadcaster.sendToClient(session, new TutorialStepMessage(firstStep));

        return TutorialStartResult.success(tutorial, firstStep);
    }

    /**
     * ステップ検証
     */
    public StepValidationResult validateStep(
        ClientSession session,
        String tutorialId,
        int stepId,
        Object action
    ) {
        Tutorial tutorial = tutorials.get(tutorialId);
        TutorialProgress progress = userProgress.get(
            getProgressKey(session.getClientId(), tutorialId)
        );

        if (progress == null) {
            return StepValidationResult.error("チュートリアルが開始されていません");
        }

        if (progress.getCurrentStep() != stepId) {
            return StepValidationResult.error("順番にステップを進めてください");
        }

        TutorialStep step = tutorial.getSteps().get(stepId - 1);

        // 検証実行
        ValidationResult result = validationEngine.validate(
            step.getValidation(),
            action
        );

        if (result.isValid()) {
            // ステップ完了
            progress.completeStep(stepId);
            progress.addPoints(step.getReward().getPoints());

            // バッジ付与
            if (step.getReward().getBadgeId() != null) {
                progress.addBadge(step.getReward().getBadgeId());
            }

            // 次のステップへ
            if (stepId < tutorial.getSteps().size()) {
                progress.setCurrentStep(stepId + 1);
                TutorialStep nextStep = tutorial.getSteps().get(stepId);
                broadcaster.sendToClient(session, new TutorialStepMessage(nextStep));

                return StepValidationResult.success(
                    step.getReward().getMessage(),
                    nextStep
                );
            } else {
                // チュートリアル完了
                progress.setCompleted(true);
                broadcaster.sendToClient(session, new TutorialCompletedMessage(tutorial));

                return StepValidationResult.tutorialCompleted(
                    "おめでとうございます！チュートリアル「" + tutorial.getTitle() + "」を完了しました！"
                );
            }
        } else {
            // 失敗
            progress.incrementAttempts(stepId);

            // ヒント表示判定（3回失敗したらヒント）
            if (progress.getAttempts(stepId) >= 3 && step.getHint() != null) {
                return StepValidationResult.failed(
                    result.getMessage(),
                    step.getHint()
                );
            }

            return StepValidationResult.failed(result.getMessage(), null);
        }
    }

    /**
     * ヒント取得
     */
    public String getHint(String tutorialId, int stepId) {
        Tutorial tutorial = tutorials.get(tutorialId);
        if (tutorial != null && stepId <= tutorial.getSteps().size()) {
            return tutorial.getSteps().get(stepId - 1).getHint();
        }
        return null;
    }

    /**
     * チュートリアル一覧取得
     */
    public List<TutorialSummary> getTutorialList(ClientSession session) {
        return tutorials.values().stream()
            .map(t -> new TutorialSummary(
                t.getTutorialId(),
                t.getTitle(),
                t.getDescription(),
                t.getDifficulty(),
                t.getEstimatedMinutes(),
                isCompleted(session.getClientId(), t.getTutorialId()),
                canStart(session.getClientId(), t)
            ))
            .collect(Collectors.toList());
    }

    private boolean isCompleted(String userId, String tutorialId) {
        TutorialProgress progress = userProgress.get(getProgressKey(userId, tutorialId));
        return progress != null && progress.isCompleted();
    }

    private boolean canStart(String userId, Tutorial tutorial) {
        return tutorial.getPrerequisites().stream()
            .allMatch(prereq -> isCompleted(userId, prereq));
    }

    private String getProgressKey(String userId, String tutorialId) {
        return userId + ":" + tutorialId;
    }
}
```

### ValidationEngine

```java
public class ValidationEngine {

    private final MinecraftServer mcServer;

    public ValidationResult validate(ValidationRule rule, Object action) {
        switch (rule.getType()) {
            case CHAT_MESSAGE:
                return validateChatMessage(rule, (ChatAction) action);

            case BLOCK_PLACED:
                return validateBlockPlaced(rule, (BlockPlaceAction) action);

            case BLOCK_PATTERN:
                return validateBlockPattern(rule, (BlockPatternAction) action);

            case ENTITY_SUMMONED:
                return validateEntitySummoned(rule, (EntitySummonAction) action);

            // ... その他のタイプ

            default:
                return ValidationResult.invalid("未対応の検証タイプです");
        }
    }

    private ValidationResult validateChatMessage(ValidationRule rule, ChatAction action) {
        String expectedText = (String) rule.getParams().get("expectedText");
        boolean caseSensitive = (boolean) rule.getParams().getOrDefault("caseSensitive", false);

        String actualText = action.getMessage();
        boolean matches = caseSensitive
            ? actualText.equals(expectedText)
            : actualText.equalsIgnoreCase(expectedText);

        if (matches) {
            return ValidationResult.valid();
        } else {
            return ValidationResult.invalid(
                String.format(
                    "メッセージが違います。「%s」と送信してください。",
                    expectedText
                )
            );
        }
    }

    private ValidationResult validateBlockPlaced(ValidationRule rule, BlockPlaceAction action) {
        String expectedBlockType = (String) rule.getParams().get("blockType");
        List<Integer> relativePos = (List<Integer>) rule.getParams().get("relativePosition");
        int tolerance = (int) rule.getParams().getOrDefault("tolerance", 0);

        // ブロックタイプチェック
        if (!action.getBlockType().equals(expectedBlockType)) {
            return ValidationResult.invalid(
                String.format(
                    "ブロックの種類が違います。「%s」を置いてください。",
                    expectedBlockType
                )
            );
        }

        // 座標チェック（相対座標）
        if (relativePos != null) {
            int dx = Math.abs(action.getRelativeX() - relativePos.get(0));
            int dy = Math.abs(action.getRelativeY() - relativePos.get(1));
            int dz = Math.abs(action.getRelativeZ() - relativePos.get(2));

            if (dx <= tolerance && dy <= tolerance && dz <= tolerance) {
                return ValidationResult.valid();
            } else {
                return ValidationResult.invalid(
                    String.format(
                        "位置が違います。相対座標 ~%d ~%d ~%d に置いてください。",
                        relativePos.get(0), relativePos.get(1), relativePos.get(2)
                    )
                );
            }
        }

        return ValidationResult.valid();
    }

    // ... その他の検証メソッド
}
```

---

## 課題システム

### 課題タイプ

| タイプ | 説明 | 評価基準 | 例 |
|-------|------|---------|---|
| **建築課題** | 指定された構造を建築 | 構造一致度、ブロック数、美的要素 | 「5x5の家を作ろう」 |
| **プログラミング課題** | 効率的なコードを書く | コード行数、ブロック使用数、ループ活用 | 「10行以内で階段を作ろう」 |
| **探索課題** | 特定の条件を満たす | 達成/未達成、所要時間 | 「村を見つけよう」 |
| **創造課題** | テーマに沿った作品 | 創造性、技術力（教師評価） | 「未来の都市を作ろう」 |
| **協働課題** | チームで取り組む | チーム貢献度、完成度 | 「みんなで城を建てよう」 |

### Challenge データモデル

```java
public class Challenge {
    private String challengeId;
    private String title;
    private String description;
    private ChallengeType type;
    private DifficultyLevel difficulty;
    private int timeLimit;                    // 制限時間（秒、0=無制限）
    private List<CriteriaDefinition> criteria; // 評価基準
    private int maxScore;                     // 最高点
    private String assignedBy;                // 割り当て教師ID
    private LocalDateTime deadline;           // 締切
    private ChallengeResources resources;     // リソース（説明画像等）
}

public enum ChallengeType {
    BUILD("建築課題"),
    PROGRAMMING("プログラミング課題"),
    EXPLORATION("探索課題"),
    CREATIVE("創造課題"),
    COLLABORATIVE("協働課題");

    private final String displayName;
}

public class CriteriaDefinition {
    private String criteriaId;
    private String name;                      // 基準名
    private CriteriaType type;                // 基準タイプ
    private int weight;                       // 配点
    private Map<String, Object> params;       // パラメータ
    private boolean autoEvaluated;            // 自動評価可能か
}

public enum CriteriaType {
    BLOCK_COUNT,           // ブロック数
    STRUCTURE_MATCH,       // 構造一致度
    CODE_EFFICIENCY,       // コード効率
    CODE_LENGTH,           // コード行数
    TIME_TAKEN,            // 所要時間
    CREATIVITY,            // 創造性（手動評価）
    TECHNICAL_SKILL,       // 技術力（手動評価）
    COMPLETENESS           // 完成度（手動評価）
}
```

### ChallengeSystem（Java実装）

```java
public class ChallengeSystem {

    private final Map<String, Challenge> challenges;
    private final Map<String, ChallengeSubmission> submissions;
    private final AutoEvaluator autoEvaluator;
    private final EventBroadcaster broadcaster;

    /**
     * 課題割り当て（教師用）
     */
    public AssignmentResult assignChallenge(
        ClientSession teacherSession,
        String challengeId,
        List<String> studentIds,
        LocalDateTime deadline
    ) {
        // 権限チェック
        if (teacherSession.getRole() != Role.TEACHER) {
            return AssignmentResult.error("教師のみが課題を割り当てできます");
        }

        Challenge challenge = challenges.get(challengeId);
        if (challenge == null) {
            return AssignmentResult.error("課題が見つかりません");
        }

        // 各生徒に割り当て
        for (String studentId : studentIds) {
            ChallengeAssignment assignment = new ChallengeAssignment(
                UUID.randomUUID().toString(),
                challengeId,
                studentId,
                teacherSession.getClientId(),
                deadline
            );

            // 通知送信
            broadcaster.sendToUser(
                studentId,
                new ChallengeAssignedNotification(challenge, deadline)
            );
        }

        return AssignmentResult.success(studentIds.size());
    }

    /**
     * 課題提出
     */
    public SubmissionResult submitChallenge(
        ClientSession studentSession,
        String challengeId,
        ChallengeWork work
    ) {
        Challenge challenge = challenges.get(challengeId);
        if (challenge == null) {
            return SubmissionResult.error("課題が見つかりません");
        }

        // 提出物作成
        ChallengeSubmission submission = new ChallengeSubmission(
            UUID.randomUUID().toString(),
            challengeId,
            studentSession.getClientId(),
            LocalDateTime.now(),
            work
        );

        // 自動評価
        AutoEvaluationResult autoResult = autoEvaluator.evaluate(challenge, work);
        submission.setAutoScore(autoResult.getScore());
        submission.setAutoFeedback(autoResult.getFeedback());

        // 手動評価が必要な基準があるか確認
        boolean needsManualGrading = challenge.getCriteria().stream()
            .anyMatch(c -> !c.isAutoEvaluated());

        if (needsManualGrading) {
            submission.setStatus(SubmissionStatus.PENDING_REVIEW);

            // 教師に通知
            broadcaster.sendToUser(
                challenge.getAssignedBy(),
                new SubmissionPendingNotification(studentSession, challenge)
            );
        } else {
            submission.setStatus(SubmissionStatus.GRADED);
            submission.setFinalScore(autoResult.getScore());
        }

        submissions.put(submission.getSubmissionId(), submission);

        return SubmissionResult.success(submission, autoResult);
    }

    /**
     * 教師評価
     */
    public GradingResult gradeSubmission(
        ClientSession teacherSession,
        String submissionId,
        Map<String, Integer> manualScores,
        String feedback
    ) {
        if (teacherSession.getRole() != Role.TEACHER) {
            return GradingResult.error("教師のみが評価できます");
        }

        ChallengeSubmission submission = submissions.get(submissionId);
        if (submission == null) {
            return GradingResult.error("提出物が見つかりません");
        }

        // 手動評価スコア設定
        submission.setManualScores(manualScores);
        submission.setTeacherFeedback(feedback);

        // 最終スコア計算（自動評価 + 手動評価）
        int finalScore = calculateFinalScore(
            submission.getChallenge(),
            submission.getAutoScore(),
            manualScores
        );

        submission.setFinalScore(finalScore);
        submission.setStatus(SubmissionStatus.GRADED);
        submission.setGradedAt(LocalDateTime.now());
        submission.setGradedBy(teacherSession.getClientId());

        // 生徒に通知
        broadcaster.sendToUser(
            submission.getStudentId(),
            new GradingCompletedNotification(submission, finalScore, feedback)
        );

        return GradingResult.success(finalScore);
    }

    /**
     * 最終スコア計算
     */
    private int calculateFinalScore(
        Challenge challenge,
        Map<String, Integer> autoScores,
        Map<String, Integer> manualScores
    ) {
        int totalScore = 0;
        int totalWeight = 0;

        for (CriteriaDefinition criteria : challenge.getCriteria()) {
            int score = criteria.isAutoEvaluated()
                ? autoScores.getOrDefault(criteria.getCriteriaId(), 0)
                : manualScores.getOrDefault(criteria.getCriteriaId(), 0);

            totalScore += score * criteria.getWeight();
            totalWeight += criteria.getWeight();
        }

        return totalWeight > 0 ? (totalScore * 100) / totalWeight : 0;
    }
}
```

### AutoEvaluator

```java
public class AutoEvaluator {

    /**
     * 自動評価実行
     */
    public AutoEvaluationResult evaluate(Challenge challenge, ChallengeWork work) {
        Map<String, Integer> scores = new HashMap<>();
        List<String> feedback = new ArrayList<>();

        for (CriteriaDefinition criteria : challenge.getCriteria()) {
            if (!criteria.isAutoEvaluated()) {
                continue;
            }

            CriteriaEvaluationResult result = evaluateCriteria(criteria, work);
            scores.put(criteria.getCriteriaId(), result.getScore());
            feedback.add(result.getFeedback());
        }

        int totalScore = scores.values().stream()
            .mapToInt(Integer::intValue)
            .sum();

        return new AutoEvaluationResult(scores, totalScore, feedback);
    }

    private CriteriaEvaluationResult evaluateCriteria(
        CriteriaDefinition criteria,
        ChallengeWork work
    ) {
        switch (criteria.getType()) {
            case BLOCK_COUNT:
                return evaluateBlockCount(criteria, work);

            case CODE_LENGTH:
                return evaluateCodeLength(criteria, work);

            case CODE_EFFICIENCY:
                return evaluateCodeEfficiency(criteria, work);

            case STRUCTURE_MATCH:
                return evaluateStructureMatch(criteria, work);

            case TIME_TAKEN:
                return evaluateTimeTaken(criteria, work);

            default:
                return CriteriaEvaluationResult.zero("自動評価不可");
        }
    }

    private CriteriaEvaluationResult evaluateBlockCount(
        CriteriaDefinition criteria,
        ChallengeWork work
    ) {
        int targetCount = (int) criteria.getParams().get("targetCount");
        int tolerance = (int) criteria.getParams().getOrDefault("tolerance", 0);
        int actualCount = work.getBlockCount();

        int diff = Math.abs(actualCount - targetCount);

        if (diff <= tolerance) {
            return CriteriaEvaluationResult.full(
                criteria.getWeight(),
                String.format("ブロック数完璧！（%d個）", actualCount)
            );
        } else if (diff <= tolerance * 2) {
            int score = (int) (criteria.getWeight() * 0.7);
            return new CriteriaEvaluationResult(
                score,
                String.format("ブロック数がやや異なります（目標:%d, 実際:%d）", targetCount, actualCount)
            );
        } else {
            int score = (int) (criteria.getWeight() * 0.3);
            return new CriteriaEvaluationResult(
                score,
                String.format("ブロック数が大きく異なります（目標:%d, 実際:%d）", targetCount, actualCount)
            );
        }
    }

    private CriteriaEvaluationResult evaluateCodeLength(
        CriteriaDefinition criteria,
        ChallengeWork work
    ) {
        int maxLines = (int) criteria.getParams().get("maxLines");
        int actualLines = work.getScratchCode().getBlockCount();

        if (actualLines <= maxLines) {
            return CriteriaEvaluationResult.full(
                criteria.getWeight(),
                String.format("コード行数: %d行（目標:%d行以内）✓", actualLines, maxLines)
            );
        } else {
            int penalty = Math.min(actualLines - maxLines, criteria.getWeight());
            int score = Math.max(0, criteria.getWeight() - penalty);
            return new CriteriaEvaluationResult(
                score,
                String.format("コードが長すぎます（%d行、目標:%d行以内）", actualLines, maxLines)
            );
        }
    }

    // ... その他の評価メソッド
}
```

---

## 進捗管理システム

### ProgressTracker

```java
public class ProgressTracker {

    private final Map<String, UserProgress> userProgressMap;
    private final DataPersistenceService persistenceService;

    /**
     * 進捗記録
     */
    public void recordAction(ClientSession session, PlayerAction action) {
        UserProgress progress = getOrCreateProgress(session.getClientId());

        switch (action.getType()) {
            case PLACE_BLOCK:
                progress.incrementBlocksPlaced();
                progress.recordBlockType(action.getBlockType());
                break;

            case SUMMON_ENTITY:
                progress.incrementEntitiesSummoned();
                break;

            case CHAT:
                progress.incrementChatMessages();
                break;

            // ... その他のアクション
        }

        progress.setLastActivity(LocalDateTime.now());
        persistenceService.save(progress);

        // バッジチェック
        checkAndAwardBadges(session, progress);
    }

    /**
     * 進捗取得
     */
    public UserProgress getProgress(String userId) {
        return userProgressMap.getOrDefault(
            userId,
            new UserProgress(userId)
        );
    }

    /**
     * レポート生成
     */
    public ProgressReport generateReport(String userId, DateRange range) {
        UserProgress progress = getProgress(userId);

        return ProgressReport.builder()
            .userId(userId)
            .dateRange(range)
            .tutorialsCompleted(progress.getCompletedTutorials().size())
            .challengesCompleted(progress.getCompletedChallenges().size())
            .totalPoints(progress.getTotalPoints())
            .blocksPlaced(progress.getBlocksPlaced())
            .entitiesSummoned(progress.getEntitiesSummoned())
            .badges(progress.getEarnedBadges())
            .activityTimeline(progress.getActivityTimeline(range))
            .blockUsageStats(progress.getBlockUsageStats())
            .build();
    }

    private UserProgress getOrCreateProgress(String userId) {
        return userProgressMap.computeIfAbsent(
            userId,
            id -> new UserProgress(id)
        );
    }
}
```

### UserProgress データモデル

```java
public class UserProgress {
    private String userId;
    private int totalPoints;
    private int blocksPlaced;
    private int entitiesSummoned;
    private int chatMessages;
    private Set<String> completedTutorials;
    private Set<String> completedChallenges;
    private Set<String> earnedBadges;
    private Map<String, Integer> blockUsageStats;
    private LocalDateTime lastActivity;
    private LocalDateTime joinedAt;

    // メソッド
    public void incrementBlocksPlaced() {
        this.blocksPlaced++;
    }

    public void recordBlockType(String blockType) {
        blockUsageStats.merge(blockType, 1, Integer::sum);
    }

    public void addPoints(int points) {
        this.totalPoints += points;
    }

    public void awardBadge(String badgeId) {
        this.earnedBadges.add(badgeId);
    }

    // ... その他のメソッド
}
```

---

## バッジ・達成システム

### バッジ定義

```json
{
  "badges": [
    {
      "badgeId": "connected",
      "name": "接続マスター",
      "description": "初めてMinecraftに接続した",
      "iconUrl": "/assets/badges/connected.png",
      "rarity": "COMMON",
      "trigger": {
        "type": "CONNECTION_ESTABLISHED",
        "count": 1
      }
    },
    {
      "badgeId": "first_chat",
      "name": "おしゃべり初心者",
      "description": "初めてチャットメッセージを送信した",
      "iconUrl": "/assets/badges/first_chat.png",
      "rarity": "COMMON",
      "trigger": {
        "type": "CHAT_MESSAGE",
        "count": 1
      }
    },
    {
      "badgeId": "builder_100",
      "name": "建築家見習い",
      "description": "100個のブロックを配置した",
      "iconUrl": "/assets/badges/builder_100.png",
      "rarity": "UNCOMMON",
      "trigger": {
        "type": "BLOCKS_PLACED",
        "count": 100
      }
    },
    {
      "badgeId": "builder_1000",
      "name": "建築マスター",
      "description": "1000個のブロックを配置した",
      "iconUrl": "/assets/badges/builder_1000.png",
      "rarity": "RARE",
      "trigger": {
        "type": "BLOCKS_PLACED",
        "count": 1000
      }
    },
    {
      "badgeId": "tutorial_master",
      "name": "チュートリアル制覇",
      "description": "すべてのチュートリアルを完了した",
      "iconUrl": "/assets/badges/tutorial_master.png",
      "rarity": "EPIC",
      "trigger": {
        "type": "TUTORIALS_COMPLETED",
        "count": "ALL"
      }
    },
    {
      "badgeId": "speedster",
      "name": "スピードスター",
      "description": "課題を5分以内にクリアした",
      "iconUrl": "/assets/badges/speedster.png",
      "rarity": "RARE",
      "trigger": {
        "type": "CHALLENGE_TIME",
        "maxSeconds": 300
      }
    },
    {
      "badgeId": "perfectionist",
      "name": "完璧主義者",
      "description": "課題で満点を獲得した",
      "iconUrl": "/assets/badges/perfectionist.png",
      "rarity": "EPIC",
      "trigger": {
        "type": "CHALLENGE_SCORE",
        "minScore": 100
      }
    },
    {
      "badgeId": "creative_genius",
      "name": "創造の天才",
      "description": "創造課題で最高評価を獲得した",
      "iconUrl": "/assets/badges/creative_genius.png",
      "rarity": "LEGENDARY",
      "trigger": {
        "type": "CREATIVE_RATING",
        "minRating": 5
      }
    }
  ]
}
```

### BadgeManager

```java
public class BadgeManager {

    private final Map<String, Badge> badges;
    private final EventBroadcaster broadcaster;

    /**
     * バッジチェックと付与
     */
    public void checkAndAward(ClientSession session, UserProgress progress) {
        for (Badge badge : badges.values()) {
            if (progress.getEarnedBadges().contains(badge.getBadgeId())) {
                continue; // 既に獲得済み
            }

            if (isTriggerMet(badge.getTrigger(), progress)) {
                awardBadge(session, progress, badge);
            }
        }
    }

    private boolean isTriggerMet(BadgeTrigger trigger, UserProgress progress) {
        switch (trigger.getType()) {
            case BLOCKS_PLACED:
                return progress.getBlocksPlaced() >= trigger.getCount();

            case CHAT_MESSAGE:
                return progress.getChatMessages() >= trigger.getCount();

            case TUTORIALS_COMPLETED:
                if ("ALL".equals(trigger.getCount())) {
                    return progress.getCompletedTutorials().size() >= getTotalTutorials();
                } else {
                    return progress.getCompletedTutorials().size() >= (int) trigger.getCount();
                }

            // ... その他のトリガー

            default:
                return false;
        }
    }

    private void awardBadge(ClientSession session, UserProgress progress, Badge badge) {
        progress.awardBadge(badge.getBadgeId());

        broadcaster.sendToClient(
            session,
            new BadgeAwardedNotification(badge)
        );

        // ログ記録
        System.out.println(String.format(
            "Badge awarded: %s to user %s",
            badge.getName(),
            session.getClientName()
        ));
    }
}
```

---

## UI/UX設計

### Scratchクライアント側UI

#### チュートリアルパネル

```
┌─────────────────────────────────────────────────────┐
│  📚 チュートリアル                         [閉じる] │
├─────────────────────────────────────────────────────┤
│                                                      │
│  ステップ 2/4: Minecraftに接続しよう              │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━    │
│                                                      │
│  「Minecraftに接続」ブロックをワークスペースに       │
│  ドラッグして、緑の旗をクリックしてください。       │
│                                                      │
│  [画像: 接続ブロックの例]                           │
│                                                      │
│  💡 ヒント: ホストは「localhost」、ポートは         │
│  「14711」のままでOKです。                          │
│                                                      │
│              [前のステップ] [ヒントを見る]          │
└─────────────────────────────────────────────────────┘
```

#### 進捗ダッシュボード

```
┌─────────────────────────────────────────────────────┐
│  📊 マイ進捗                                        │
├─────────────────────────────────────────────────────┤
│                                                      │
│  総ポイント: 1,250 pt  🏆 バッジ: 8個              │
│                                                      │
│  ┌──────────────┐  ┌──────────────┐                │
│  │ チュートリアル │  │   課題      │                │
│  │   5/10完了   │  │   3/5完了   │                │
│  └──────────────┘  └──────────────┘                │
│                                                      │
│  最近の活動:                                         │
│  • ブロック配置: 250個                              │
│  • エンティティ召喚: 15回                           │
│  • チャット送信: 42回                               │
│                                                      │
│  獲得バッジ:                                         │
│  🏅 接続マスター  🏅 おしゃべり初心者              │
│  🏅 建築家見習い  🏅 スピードスター                │
│                                                      │
└─────────────────────────────────────────────────────┘
```

---

## コンテンツ作成ガイド

### チュートリアル作成手順

1. **ターゲット設定**: 対象者（初心者/中級者/上級者）を明確化
2. **目標定義**: チュートリアル完了後に習得できるスキルを定義
3. **ステップ分割**: 大きな目標を小さなステップに分割（理想: 4-8ステップ）
4. **検証ルール作成**: 各ステップで何を確認するか定義
5. **ヒント準備**: 詰まった時のヒントを準備
6. **テスト**: 実際にプレイして難易度を確認

### チュートリアルテンプレート

```json
{
  "tutorialId": "custom_XXX",
  "title": "[タイトル]",
  "description": "[説明]",
  "difficulty": "BEGINNER/INTERMEDIATE/ADVANCED",
  "estimatedMinutes": 15,
  "prerequisites": [],
  "steps": [
    {
      "stepId": 1,
      "title": "[ステップタイトル]",
      "instruction": "[具体的な指示]",
      "hint": "[ヒント]",
      "validation": {
        "type": "適切なタイプを選択",
        "params": {}
      },
      "reward": {
        "points": 10,
        "message": "[励ましのメッセージ]"
      }
    }
  ]
}
```

---

**作成日**: 2025-11-12
**対象バージョン**: 1.0.0

package com.yourname.minecraftcollaboration.blockpacks;

/**
 * ブロックパックの難易度レベルを定義
 */
public enum DifficultyLevel {
    BEGINNER(1, "Beginner"),
    INTERMEDIATE(2, "Intermediate"),
    ADVANCED(3, "Advanced"),
    EXPERT(4, "Expert");
    
    private final int level;
    private final String displayKey;
    
    DifficultyLevel(int level, String displayKey) {
        this.level = level;
        this.displayKey = displayKey;
    }
    
    /**
     * 言語に応じた表示名を取得
     */
    public String getDisplayName(String language) {
        switch (language) {
            case "ja_JP":
                return getJapaneseName();
            case "en_US":
                return getEnglishName();
            case "zh_CN":
                return getChineseSimplifiedName();
            case "zh_TW":
                return getChineseTraditionalName();
            case "ko_KR":
                return getKoreanName();
            case "es_ES":
                return getSpanishName();
            case "fr_FR":
                return getFrenchName();
            case "de_DE":
                return getGermanName();
            default:
                return getEnglishName();
        }
    }
    
    private String getJapaneseName() {
        switch (this) {
            case BEGINNER: return "初心者";
            case INTERMEDIATE: return "中級者";
            case ADVANCED: return "上級者";
            case EXPERT: return "エキスパート";
            default: return displayKey;
        }
    }
    
    private String getEnglishName() {
        switch (this) {
            case BEGINNER: return "Beginner";
            case INTERMEDIATE: return "Intermediate";
            case ADVANCED: return "Advanced";
            case EXPERT: return "Expert";
            default: return displayKey;
        }
    }
    
    private String getChineseSimplifiedName() {
        switch (this) {
            case BEGINNER: return "初学者";
            case INTERMEDIATE: return "中级";
            case ADVANCED: return "高级";
            case EXPERT: return "专家";
            default: return displayKey;
        }
    }
    
    private String getChineseTraditionalName() {
        switch (this) {
            case BEGINNER: return "初學者";
            case INTERMEDIATE: return "中級";
            case ADVANCED: return "高級";
            case EXPERT: return "專家";
            default: return displayKey;
        }
    }
    
    private String getKoreanName() {
        switch (this) {
            case BEGINNER: return "초보자";
            case INTERMEDIATE: return "중급자";
            case ADVANCED: return "고급자";
            case EXPERT: return "전문가";
            default: return displayKey;
        }
    }
    
    private String getSpanishName() {
        switch (this) {
            case BEGINNER: return "Principiante";
            case INTERMEDIATE: return "Intermedio";
            case ADVANCED: return "Avanzado";
            case EXPERT: return "Experto";
            default: return displayKey;
        }
    }
    
    private String getFrenchName() {
        switch (this) {
            case BEGINNER: return "Débutant";
            case INTERMEDIATE: return "Intermédiaire";
            case ADVANCED: return "Avancé";
            case EXPERT: return "Expert";
            default: return displayKey;
        }
    }
    
    private String getGermanName() {
        switch (this) {
            case BEGINNER: return "Anfänger";
            case INTERMEDIATE: return "Fortgeschritten";
            case ADVANCED: return "Erweitert";
            case EXPERT: return "Experte";
            default: return displayKey;
        }
    }
    
    /**
     * 数値レベルを取得
     */
    public int getLevel() {
        return level;
    }
    
    /**
     * 表示キーを取得
     */
    public String getDisplayKey() {
        return displayKey;
    }
    
    /**
     * 数値レベルから難易度を取得
     */
    public static DifficultyLevel fromLevel(int level) {
        for (DifficultyLevel difficulty : values()) {
            if (difficulty.level == level) {
                return difficulty;
            }
        }
        return BEGINNER;
    }
    
    /**
     * 指定した難易度以上かチェック
     */
    public boolean isAtLeast(DifficultyLevel other) {
        return this.level >= other.level;
    }
    
    /**
     * 指定した難易度以下かチェック
     */
    public boolean isAtMost(DifficultyLevel other) {
        return this.level <= other.level;
    }
}
package edu.minecraft.collaboration.blockpacks;

/**
 * ブロックパック縺ｮ髮｣譏灘ｺｦ繝ｬ繝吶Ν繧貞ｮ夂ｾｩ
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
     * 險隱槭↓蠢懊§縺溯｡ｨ遉ｺ蜷阪ｒ蜿門ｾ・     */
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
            case BEGINNER: return "蛻晏ｿ・・";
            case INTERMEDIATE: return "荳ｭ邏夊・";
            case ADVANCED: return "荳顔ｴ夊・";
            case EXPERT: return "繧ｨ繧ｭ繧ｹ繝代・繝・";
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
            case INTERMEDIATE: return "荳ｭ郤ｧ";
            case ADVANCED: return "高级";
            case EXPERT: return "荳灘ｮｶ";
            default: return displayKey;
        }
    }

    private String getChineseTraditionalName() {
        switch (this) {
            case BEGINNER: return "初學者";
            case INTERMEDIATE: return "荳ｭ邏・";
            case ADVANCED: return "高級";
            case EXPERT: return "蟆亥ｮｶ";
            default: return displayKey;
        }
    }

    private String getKoreanName() {
        switch (this) {
            case BEGINNER: return "초급자";
            case INTERMEDIATE: return "・滝ｸ餓梵";
            case ADVANCED: return "・・餓梵";
            case EXPERT: return "（ｬｸ・";
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
     * 謨ｰ蛟､繝ｬ繝吶Ν繧貞叙蠕・     */
    public int getLevel() {
        return level;
    }

    /**
     * 陦ｨ遉ｺ繧ｭ繝ｼ繧貞叙蠕・     */
    public String getDisplayKey() {
        return displayKey;
    }

    /**
     * 謨ｰ蛟､繝ｬ繝吶Ν縺九ｉ髮｣譏灘ｺｦ繧貞叙蠕・     */
    public static DifficultyLevel fromLevel(int level) {
        for (DifficultyLevel difficulty : values()) {
            if (difficulty.level == level) {
                return difficulty;
            }
        }
        return BEGINNER;
    }

    /**
     * 謖・ｮ壹＠縺滄屮譏灘ｺｦ莉･荳翫°繝√ぉ繝・け
     */
    public boolean isAtLeast(DifficultyLevel other) {
        return this.level >= other.level;
    }

    /**
     * 謖・ｮ壹＠縺滄屮譏灘ｺｦ莉･荳九°繝√ぉ繝・け
     */
    public boolean isAtMost(DifficultyLevel other) {
        return this.level <= other.level;
    }
}

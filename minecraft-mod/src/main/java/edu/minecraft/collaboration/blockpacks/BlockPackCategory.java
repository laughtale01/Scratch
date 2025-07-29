package edu.minecraft.collaboration.blockpacks;

/**
 * Block pack categories definition
 */
public enum BlockPackCategory {
    BASIC("Basic", 1),
    BEGINNER("Beginner", 2),
    EDUCATIONAL("Educational", 3),
    NATURE("Nature", 4),
    CREATIVE("Creative", 5),
    ARCHITECTURAL("Architectural", 6),
    PROGRAMMING("Programming", 7),
    ADVANCED("Advanced", 8),
    CUSTOM("Custom", 9);
    
    private final String displayKey;
    private final int order;
    
    BlockPackCategory(String displayKey, int order) {
        this.displayKey = displayKey;
        this.order = order;
    }
    
    /**
     * Get display name based on language
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
            case BASIC: return "基本";
            case BEGINNER: return "初心者";
            case EDUCATIONAL: return "教育";
            case NATURE: return "自然";
            case CREATIVE: return "クリエイティブ";
            case ARCHITECTURAL: return "建築";
            case PROGRAMMING: return "プログラミング";
            case ADVANCED: return "上級";
            case CUSTOM: return "カスタム";
            default: return displayKey;
        }
    }
    
    private String getEnglishName() {
        switch (this) {
            case BASIC: return "Basic";
            case BEGINNER: return "Beginner";
            case EDUCATIONAL: return "Educational";
            case NATURE: return "Nature";
            case CREATIVE: return "Creative";
            case ARCHITECTURAL: return "Architectural";
            case PROGRAMMING: return "Programming";
            case ADVANCED: return "Advanced";
            case CUSTOM: return "Custom";
            default: return displayKey;
        }
    }
    
    private String getChineseSimplifiedName() {
        switch (this) {
            case BASIC: return "基础";
            case BEGINNER: return "初学者";
            case EDUCATIONAL: return "教育";
            case NATURE: return "自然";
            case CREATIVE: return "创意";
            case ARCHITECTURAL: return "建筑";
            case PROGRAMMING: return "编程";
            case ADVANCED: return "高级";
            case CUSTOM: return "自定义";
            default: return displayKey;
        }
    }
    
    private String getChineseTraditionalName() {
        switch (this) {
            case BASIC: return "基礎";
            case BEGINNER: return "初學者";
            case EDUCATIONAL: return "教育";
            case NATURE: return "自然";
            case CREATIVE: return "創意";
            case ARCHITECTURAL: return "建築";
            case PROGRAMMING: return "編程";
            case ADVANCED: return "高級";
            case CUSTOM: return "自訂";
            default: return displayKey;
        }
    }
    
    private String getKoreanName() {
        switch (this) {
            case BASIC: return "기본";
            case BEGINNER: return "초급자";
            case EDUCATIONAL: return "교육";
            case NATURE: return "자연";
            case CREATIVE: return "창의";
            case ARCHITECTURAL: return "건축";
            case PROGRAMMING: return "프로그래밍";
            case ADVANCED: return "고급";
            case CUSTOM: return "사용자 정의";
            default: return displayKey;
        }
    }
    
    private String getSpanishName() {
        switch (this) {
            case BASIC: return "Básico";
            case BEGINNER: return "Principiante";
            case EDUCATIONAL: return "Educativo";
            case NATURE: return "Naturaleza";
            case CREATIVE: return "Creativo";
            case ARCHITECTURAL: return "Arquitectónico";
            case PROGRAMMING: return "Programación";
            case ADVANCED: return "Avanzado";
            case CUSTOM: return "Personalizado";
            default: return displayKey;
        }
    }
    
    private String getFrenchName() {
        switch (this) {
            case BASIC: return "De base";
            case BEGINNER: return "Débutant";
            case EDUCATIONAL: return "Éducatif";
            case NATURE: return "Nature";
            case CREATIVE: return "Créatif";
            case ARCHITECTURAL: return "Architectural";
            case PROGRAMMING: return "Programmation";
            case ADVANCED: return "Avancé";
            case CUSTOM: return "Personnalisé";
            default: return displayKey;
        }
    }
    
    private String getGermanName() {
        switch (this) {
            case BASIC: return "Grundlegend";
            case BEGINNER: return "Anfänger";
            case EDUCATIONAL: return "Bildung";
            case NATURE: return "Natur";
            case CREATIVE: return "Kreativ";
            case ARCHITECTURAL: return "Architektonisch";
            case PROGRAMMING: return "Programmierung";
            case ADVANCED: return "Fortgeschritten";
            case CUSTOM: return "Benutzerdefiniert";
            default: return displayKey;
        }
    }
    
    public String getDisplayKey() {
        return displayKey;
    }
    
    public int getOrder() {
        return order;
    }
}
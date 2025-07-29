package com.yourname.minecraftcollaboration.localization;

import com.yourname.minecraftcollaboration.MinecraftCollaborationMod;
import org.slf4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 多言語対応のためのメッセージ管理システム
 */
public class LanguageManager {
    private static final Logger LOGGER = MinecraftCollaborationMod.getLogger();
    private static LanguageManager instance;
    
    // サポートされている言語
    public static final String JAPANESE = "ja_JP";
    public static final String ENGLISH = "en_US";
    public static final String CHINESE_SIMPLIFIED = "zh_CN";
    public static final String CHINESE_TRADITIONAL = "zh_TW";
    public static final String KOREAN = "ko_KR";
    public static final String SPANISH = "es_ES";
    public static final String FRENCH = "fr_FR";
    public static final String GERMAN = "de_DE";
    
    // 言語データ
    private final Map<String, Map<String, String>> languages = new ConcurrentHashMap<>();
    private final Map<UUID, String> userLanguages = new ConcurrentHashMap<>();
    private String defaultLanguage = JAPANESE;
    
    // メッセージキャッシュ
    private final Map<String, String> messageCache = new ConcurrentHashMap<>();
    
    private LanguageManager() {
        initializeLanguages();
    }
    
    public static LanguageManager getInstance() {
        if (instance == null) {
            instance = new LanguageManager();
        }
        return instance;
    }
    
    /**
     * 言語データの初期化
     */
    private void initializeLanguages() {
        // 日本語メッセージ
        initializeJapanese();
        
        // 英語メッセージ
        initializeEnglish();
        
        // 中国語（簡体字）メッセージ
        initializeChineseSimplified();
        
        // 中国語（繁体字）メッセージ
        initializeChineseTraditional();
        
        // 韓国語メッセージ
        initializeKorean();
        
        // スペイン語メッセージ
        initializeSpanish();
        
        // フランス語メッセージ
        initializeFrench();
        
        // ドイツ語メッセージ
        initializeGerman();
        
        LOGGER.info("Initialized {} languages with {} total messages", 
            languages.size(), getTotalMessageCount());
    }
    
    /**
     * 日本語メッセージの初期化
     */
    private void initializeJapanese() {
        Map<String, String> ja = new HashMap<>();
        
        // 基本メッセージ
        ja.put("welcome", "Minecraft協調学習システムへようこそ！");
        ja.put("connection.success", "接続に成功しました");
        ja.put("connection.failed", "接続に失敗しました");
        
        // 教師機能
        ja.put("teacher.registered", "教師アカウントに登録されました");
        ja.put("teacher.access_required", "教師権限が必要です");
        ja.put("classroom.mode_enabled", "授業モードが有効になりました");
        ja.put("classroom.mode_disabled", "授業モードが無効になりました");
        ja.put("classroom.permissions_updated", "権限が更新されました");
        ja.put("classroom.students_frozen", "全生徒を一時停止しました");
        ja.put("classroom.students_unfrozen", "全生徒の活動を再開しました");
        ja.put("classroom.students_summoned", "全生徒を召集しました");
        
        // 生徒メッセージ
        ja.put("student.activity_paused", "活動が一時停止されました");
        ja.put("student.activity_resumed", "活動を再開してください");
        ja.put("student.time_limit_set", "制限時間が設定されました: {0}分");
        ja.put("student.time_limit_removed", "制限時間が解除されました");
        ja.put("student.restriction_added", "制限が追加されました: {0}");
        ja.put("student.summoned", "先生の場所に集合しました");
        
        // 協調機能
        ja.put("invitation.sent", "{0}さんに招待を送信しました");
        ja.put("invitation.received", "{0}さんから招待が届きました！");
        ja.put("visit.requested", "{0}さんが訪問を希望しています");
        ja.put("visit.approved", "訪問が承認されました！{0}さんの世界へようこそ！");
        ja.put("home.returned", "ホームワールドに帰還しました");
        ja.put("emergency.returned", "【緊急帰宅】安全にホームワールドに帰還しました！");
        ja.put("emergency.broadcast", "{0}さんが緊急帰宅を使用しました");
        
        // 進捗・達成
        ja.put("progress.report_generated", "学習進捗レポートが生成されました");
        ja.put("progress.reset", "学習進捗がリセットされました");
        ja.put("achievement.earned", "達成を獲得しました: {0}");
        ja.put("milestone.completed", "マイルストーンを達成しました: {0}");
        
        // エージェント
        ja.put("agent.summoned", "エージェント '{0}' を召喚しました");
        ja.put("agent.dismissed", "エージェントを帰しました");
        ja.put("agent.following", "エージェントがついてきます");
        ja.put("agent.stopped_following", "エージェントがついてこなくなりました");
        
        // エラーメッセージ
        ja.put("error.not_found", "見つかりませんでした");
        ja.put("error.permission_denied", "権限がありません");
        ja.put("error.invalid_parameters", "無効なパラメータです");
        ja.put("error.server_error", "サーバーエラーが発生しました");
        ja.put("error.student_not_found", "生徒が見つかりません: {0}");
        ja.put("error.already_exists", "既に存在します");
        
        // 建築メッセージ
        ja.put("building.circle_created", "円を作成しました");
        ja.put("building.sphere_created", "球を作成しました");
        ja.put("building.wall_created", "壁を作成しました");
        ja.put("building.house_created", "家を作成しました");
        ja.put("building.area_filled", "エリアを埋めました");
        ja.put("building.area_cleared", "エリアをクリアしました");
        
        // 言語設定
        ja.put("language.changed", "言語を{0}に変更しました");
        ja.put("language.current", "現在の言語: {0}");
        ja.put("language.default_set", "デフォルト言語を{0}に設定しました");
        ja.put("language.unsupported", "サポートされていない言語です");
        
        // ブロックパック設定
        ja.put("blockpack.applied", "ブロックパック「{0}」を適用しました");
        ja.put("blockpack.created", "カスタムブロックパック「{0}」を作成しました");
        ja.put("blockpack.not_found", "ブロックパック「{0}」が見つかりません");
        ja.put("blockpack.permission_denied", "このブロックパックは教師権限が必要です");
        ja.put("blockpack.invalid_blocks", "有効なブロックが指定されていません");
        ja.put("blockpack.creation_failed", "カスタムブロックパックの作成に失敗しました");
        
        // オフラインモード設定
        ja.put("offline.mode_enabled", "オフラインモードが有効になりました");
        ja.put("offline.mode_disabled", "オフラインモードが無効になりました");
        ja.put("offline.sync_success", "オフラインデータの同期が完了しました");
        ja.put("offline.sync_partial", "オフラインデータの同期が部分的に完了しました");
        ja.put("offline.autosync_enabled", "自動同期が有効になりました");
        ja.put("offline.autosync_disabled", "自動同期が無効になりました");
        ja.put("offline.data_exported", "オフラインデータをエクスポートしました");
        
        languages.put(JAPANESE, ja);
    }
    
    /**
     * 英語メッセージの初期化
     */
    private void initializeEnglish() {
        Map<String, String> en = new HashMap<>();
        
        // 基本メッセージ
        en.put("welcome", "Welcome to Minecraft Collaborative Learning System!");
        en.put("connection.success", "Connection successful");
        en.put("connection.failed", "Connection failed");
        
        // 教師機能
        en.put("teacher.registered", "Registered as teacher account");
        en.put("teacher.access_required", "Teacher access required");
        en.put("classroom.mode_enabled", "Classroom mode enabled");
        en.put("classroom.mode_disabled", "Classroom mode disabled");
        en.put("classroom.permissions_updated", "Permissions updated");
        en.put("classroom.students_frozen", "All students frozen");
        en.put("classroom.students_unfrozen", "All students unfrozen");
        en.put("classroom.students_summoned", "All students summoned");
        
        // 生徒メッセージ
        en.put("student.activity_paused", "Activity paused");
        en.put("student.activity_resumed", "Please resume activity");
        en.put("student.time_limit_set", "Time limit set: {0} minutes");
        en.put("student.time_limit_removed", "Time limit removed");
        en.put("student.restriction_added", "Restriction added: {0}");
        en.put("student.summoned", "Gathered at teacher's location");
        
        // 協調機能
        en.put("invitation.sent", "Invitation sent to {0}");
        en.put("invitation.received", "Invitation received from {0}!");
        en.put("visit.requested", "{0} wants to visit");
        en.put("visit.approved", "Visit approved! Welcome to {0}'s world!");
        en.put("home.returned", "Returned to home world");
        en.put("emergency.returned", "[Emergency Return] Safely returned to home world!");
        en.put("emergency.broadcast", "{0} used emergency return");
        
        // 進捗・達成
        en.put("progress.report_generated", "Learning progress report generated");
        en.put("progress.reset", "Learning progress reset");
        en.put("achievement.earned", "Achievement earned: {0}");
        en.put("milestone.completed", "Milestone completed: {0}");
        
        // エージェント
        en.put("agent.summoned", "Agent '{0}' summoned");
        en.put("agent.dismissed", "Agent dismissed");
        en.put("agent.following", "Agent is following");
        en.put("agent.stopped_following", "Agent stopped following");
        
        // エラーメッセージ
        en.put("error.not_found", "Not found");
        en.put("error.permission_denied", "Permission denied");
        en.put("error.invalid_parameters", "Invalid parameters");
        en.put("error.server_error", "Server error occurred");
        en.put("error.student_not_found", "Student not found: {0}");
        en.put("error.already_exists", "Already exists");
        
        // 建築メッセージ
        en.put("building.circle_created", "Circle created");
        en.put("building.sphere_created", "Sphere created");
        en.put("building.wall_created", "Wall created");
        en.put("building.house_created", "House created");
        en.put("building.area_filled", "Area filled");
        en.put("building.area_cleared", "Area cleared");
        
        // Language settings
        en.put("language.changed", "Language changed to {0}");
        en.put("language.current", "Current language: {0}");
        en.put("language.default_set", "Default language set to {0}");
        en.put("language.unsupported", "Unsupported language");
        
        // Block pack settings
        en.put("blockpack.applied", "Block pack applied: {0}");
        en.put("blockpack.created", "Custom block pack created: {0}");
        en.put("blockpack.not_found", "Block pack not found: {0}");
        en.put("blockpack.permission_denied", "This block pack requires teacher access");
        en.put("blockpack.invalid_blocks", "No valid blocks specified");
        en.put("blockpack.creation_failed", "Failed to create custom block pack");
        
        // Offline mode settings
        en.put("offline.mode_enabled", "Offline mode enabled");
        en.put("offline.mode_disabled", "Offline mode disabled");
        en.put("offline.sync_success", "Offline data sync completed successfully");
        en.put("offline.sync_partial", "Offline data sync partially completed");
        en.put("offline.autosync_enabled", "Auto sync enabled");
        en.put("offline.autosync_disabled", "Auto sync disabled");
        en.put("offline.data_exported", "Offline data exported successfully");
        
        languages.put(ENGLISH, en);
    }
    
    /**
     * 中国語（簡体字）メッセージの初期化
     */
    private void initializeChineseSimplified() {
        Map<String, String> zh = new HashMap<>();
        
        // 基本メッセージ
        zh.put("welcome", "欢迎使用Minecraft协作学习系统！");
        zh.put("connection.success", "连接成功");
        zh.put("connection.failed", "连接失败");
        
        // 教师功能
        zh.put("teacher.registered", "已注册为教师账户");
        zh.put("teacher.access_required", "需要教师权限");
        zh.put("classroom.mode_enabled", "课堂模式已启用");
        zh.put("classroom.mode_disabled", "课堂模式已禁用");
        zh.put("classroom.permissions_updated", "权限已更新");
        zh.put("classroom.students_frozen", "所有学生已暂停");
        zh.put("classroom.students_unfrozen", "所有学生已恢复");
        zh.put("classroom.students_summoned", "所有学生已召集");
        
        // 学生消息
        zh.put("student.activity_paused", "活动已暂停");
        zh.put("student.activity_resumed", "请恢复活动");
        zh.put("student.time_limit_set", "时间限制已设置：{0}分钟");
        zh.put("student.time_limit_removed", "时间限制已移除");
        zh.put("student.restriction_added", "限制已添加：{0}");
        zh.put("student.summoned", "已集合到老师位置");
        
        // 协作功能
        zh.put("invitation.sent", "邀请已发送给{0}");
        zh.put("invitation.received", "收到来自{0}的邀请！");
        zh.put("visit.requested", "{0}想要访问");
        zh.put("visit.approved", "访问已批准！欢迎来到{0}的世界！");
        zh.put("home.returned", "已返回主世界");
        zh.put("emergency.returned", "【紧急返回】安全返回主世界！");
        zh.put("emergency.broadcast", "{0}使用了紧急返回");
        
        // 进度和成就
        zh.put("progress.report_generated", "学习进度报告已生成");
        zh.put("progress.reset", "学习进度已重置");
        zh.put("achievement.earned", "获得成就：{0}");
        zh.put("milestone.completed", "完成里程碑：{0}");
        
        // 代理
        zh.put("agent.summoned", "代理'{0}'已召唤");
        zh.put("agent.dismissed", "代理已解散");
        zh.put("agent.following", "代理正在跟随");
        zh.put("agent.stopped_following", "代理停止跟随");
        
        // 错误消息
        zh.put("error.not_found", "未找到");
        zh.put("error.permission_denied", "权限被拒绝");
        zh.put("error.invalid_parameters", "无效参数");
        zh.put("error.server_error", "服务器错误");
        zh.put("error.student_not_found", "未找到学生：{0}");
        zh.put("error.already_exists", "已存在");
        
        // 建筑消息
        zh.put("building.circle_created", "圆圈已创建");
        zh.put("building.sphere_created", "球体已创建");
        zh.put("building.wall_created", "墙壁已创建");
        zh.put("building.house_created", "房屋已创建");
        zh.put("building.area_filled", "区域已填充");
        zh.put("building.area_cleared", "区域已清理");
        
        // 语言设置
        zh.put("language.changed", "语言已更改为{0}");
        zh.put("language.current", "当前语言: {0}");
        zh.put("language.default_set", "默认语言设置为{0}");
        zh.put("language.unsupported", "不支持的语言");
        
        languages.put(CHINESE_SIMPLIFIED, zh);
    }
    
    /**
     * 中国語（繁体字）メッセージの初期化
     */
    private void initializeChineseTraditional() {
        Map<String, String> zh_tw = new HashMap<>();
        
        // 基本訊息
        zh_tw.put("welcome", "歡迎使用Minecraft協作學習系統！");
        zh_tw.put("connection.success", "連接成功");
        zh_tw.put("connection.failed", "連接失敗");
        
        // 教師功能
        zh_tw.put("teacher.registered", "已註冊為教師帳戶");
        zh_tw.put("teacher.access_required", "需要教師權限");
        zh_tw.put("classroom.mode_enabled", "課堂模式已啟用");
        zh_tw.put("classroom.mode_disabled", "課堂模式已禁用");
        zh_tw.put("classroom.permissions_updated", "權限已更新");
        zh_tw.put("classroom.students_frozen", "所有學生已暫停");
        zh_tw.put("classroom.students_unfrozen", "所有學生已恢復");
        zh_tw.put("classroom.students_summoned", "所有學生已召集");
        
        // 學生訊息
        zh_tw.put("student.activity_paused", "活動已暫停");
        zh_tw.put("student.activity_resumed", "請恢復活動");
        zh_tw.put("student.time_limit_set", "時間限制已設置：{0}分鐘");
        zh_tw.put("student.time_limit_removed", "時間限制已移除");
        zh_tw.put("student.restriction_added", "限制已添加：{0}");
        zh_tw.put("student.summoned", "已集合到老師位置");
        
        // 協作功能
        zh_tw.put("invitation.sent", "邀請已發送給{0}");
        zh_tw.put("invitation.received", "收到來自{0}的邀請！");
        zh_tw.put("visit.requested", "{0}想要訪問");
        zh_tw.put("visit.approved", "訪問已批准！歡迎來到{0}的世界！");
        zh_tw.put("home.returned", "已返回主世界");
        zh_tw.put("emergency.returned", "【緊急返回】安全返回主世界！");
        zh_tw.put("emergency.broadcast", "{0}使用了緊急返回");
        
        // 進度和成就
        zh_tw.put("progress.report_generated", "學習進度報告已生成");
        zh_tw.put("progress.reset", "學習進度已重置");
        zh_tw.put("achievement.earned", "獲得成就：{0}");
        zh_tw.put("milestone.completed", "完成里程碑：{0}");
        
        // 代理
        zh_tw.put("agent.summoned", "代理'{0}'已召喚");
        zh_tw.put("agent.dismissed", "代理已解散");
        zh_tw.put("agent.following", "代理正在跟隨");
        zh_tw.put("agent.stopped_following", "代理停止跟隨");
        
        // 錯誤訊息
        zh_tw.put("error.not_found", "未找到");
        zh_tw.put("error.permission_denied", "權限被拒絕");
        zh_tw.put("error.invalid_parameters", "無效參數");
        zh_tw.put("error.server_error", "伺服器錯誤");
        zh_tw.put("error.student_not_found", "未找到學生：{0}");
        zh_tw.put("error.already_exists", "已存在");
        
        // 建築訊息
        zh_tw.put("building.circle_created", "圓圈已創建");
        zh_tw.put("building.sphere_created", "球體已創建");
        zh_tw.put("building.wall_created", "牆壁已創建");
        zh_tw.put("building.house_created", "房屋已創建");
        zh_tw.put("building.area_filled", "區域已填充");
        zh_tw.put("building.area_cleared", "區域已清理");
        
        // 語言設置
        zh_tw.put("language.changed", "語言已更改為{0}");
        zh_tw.put("language.current", "當前語言: {0}");
        zh_tw.put("language.default_set", "默認語言設置為{0}");
        zh_tw.put("language.unsupported", "不支持的語言");
        
        languages.put(CHINESE_TRADITIONAL, zh_tw);
    }
    
    /**
     * 韓国語メッセージの初期化
     */
    private void initializeKorean() {
        Map<String, String> ko = new HashMap<>();
        
        // 기본 메시지
        ko.put("welcome", "Minecraft 협업 학습 시스템에 오신 것을 환영합니다!");
        ko.put("connection.success", "연결 성공");
        ko.put("connection.failed", "연결 실패");
        
        // 교사 기능
        ko.put("teacher.registered", "교사 계정으로 등록되었습니다");
        ko.put("teacher.access_required", "교사 권한이 필요합니다");
        ko.put("classroom.mode_enabled", "교실 모드가 활성화되었습니다");
        ko.put("classroom.mode_disabled", "교실 모드가 비활성화되었습니다");
        ko.put("classroom.permissions_updated", "권한이 업데이트되었습니다");
        ko.put("classroom.students_frozen", "모든 학생이 일시정지되었습니다");
        ko.put("classroom.students_unfrozen", "모든 학생이 재개되었습니다");
        ko.put("classroom.students_summoned", "모든 학생이 소집되었습니다");
        
        // 학생 메시지
        ko.put("student.activity_paused", "활동이 일시정지되었습니다");
        ko.put("student.activity_resumed", "활동을 재개해주세요");
        ko.put("student.time_limit_set", "시간 제한이 설정되었습니다: {0}분");
        ko.put("student.time_limit_removed", "시간 제한이 해제되었습니다");
        ko.put("student.restriction_added", "제한이 추가되었습니다: {0}");
        ko.put("student.summoned", "선생님 위치로 집합했습니다");
        
        // 협업 기능
        ko.put("invitation.sent", "{0}님에게 초대를 보냈습니다");
        ko.put("invitation.received", "{0}님으로부터 초대를 받았습니다!");
        ko.put("visit.requested", "{0}님이 방문을 원합니다");
        ko.put("visit.approved", "방문이 승인되었습니다! {0}님의 세계에 오신 것을 환영합니다!");
        ko.put("home.returned", "홈 월드로 돌아왔습니다");
        ko.put("emergency.returned", "[긴급 귀환] 안전하게 홈 월드로 돌아왔습니다!");
        ko.put("emergency.broadcast", "{0}님이 긴급 귀환을 사용했습니다");
        
        // 진도 및 성취
        ko.put("progress.report_generated", "학습 진도 보고서가 생성되었습니다");
        ko.put("progress.reset", "학습 진도가 초기화되었습니다");
        ko.put("achievement.earned", "성취를 달성했습니다: {0}");
        ko.put("milestone.completed", "마일스톤을 완료했습니다: {0}");
        
        // 에이전트
        ko.put("agent.summoned", "에이전트 '{0}'를 소환했습니다");
        ko.put("agent.dismissed", "에이전트를 해제했습니다");
        ko.put("agent.following", "에이전트가 따라오고 있습니다");
        ko.put("agent.stopped_following", "에이전트가 따라오기를 중단했습니다");
        
        // 오류 메시지
        ko.put("error.not_found", "찾을 수 없습니다");
        ko.put("error.permission_denied", "권한이 거부되었습니다");
        ko.put("error.invalid_parameters", "유효하지 않은 매개변수입니다");
        ko.put("error.server_error", "서버 오류가 발생했습니다");
        ko.put("error.student_not_found", "학생을 찾을 수 없습니다: {0}");
        ko.put("error.already_exists", "이미 존재합니다");
        
        // 건축 메시지
        ko.put("building.circle_created", "원이 생성되었습니다");
        ko.put("building.sphere_created", "구가 생성되었습니다");
        ko.put("building.wall_created", "벽이 생성되었습니다");
        ko.put("building.house_created", "집이 생성되었습니다");
        ko.put("building.area_filled", "영역이 채워졌습니다");
        ko.put("building.area_cleared", "영역이 정리되었습니다");
        
        // 언어 설정
        ko.put("language.changed", "언어를 {0}로 변경했습니다");
        ko.put("language.current", "현재 언어: {0}");
        ko.put("language.default_set", "기본 언어를 {0}로 설정했습니다");
        ko.put("language.unsupported", "지원되지 않는 언어입니다");
        
        languages.put(KOREAN, ko);
    }
    
    /**
     * スペイン語メッセージの初期化
     */
    private void initializeSpanish() {
        Map<String, String> es = new HashMap<>();
        
        // Mensajes básicos
        es.put("welcome", "¡Bienvenido al Sistema de Aprendizaje Colaborativo de Minecraft!");
        es.put("connection.success", "Conexión exitosa");
        es.put("connection.failed", "Conexión fallida");
        
        // Funciones del profesor
        es.put("teacher.registered", "Registrado como cuenta de profesor");
        es.put("teacher.access_required", "Se requiere acceso de profesor");
        es.put("classroom.mode_enabled", "Modo aula activado");
        es.put("classroom.mode_disabled", "Modo aula desactivado");
        es.put("classroom.permissions_updated", "Permisos actualizados");
        es.put("classroom.students_frozen", "Todos los estudiantes pausados");
        es.put("classroom.students_unfrozen", "Todos los estudiantes reanudados");
        es.put("classroom.students_summoned", "Todos los estudiantes convocados");
        
        // Mensajes de estudiante
        es.put("student.activity_paused", "Actividad pausada");
        es.put("student.activity_resumed", "Por favor reanude la actividad");
        es.put("student.time_limit_set", "Límite de tiempo establecido: {0} minutos");
        es.put("student.time_limit_removed", "Límite de tiempo eliminado");
        es.put("student.restriction_added", "Restricción añadida: {0}");
        es.put("student.summoned", "Reunido en la ubicación del profesor");
        
        // Funciones colaborativas
        es.put("invitation.sent", "Invitación enviada a {0}");
        es.put("invitation.received", "¡Invitación recibida de {0}!");
        es.put("visit.requested", "{0} quiere visitar");
        es.put("visit.approved", "¡Visita aprobada! ¡Bienvenido al mundo de {0}!");
        es.put("home.returned", "Regresado al mundo principal");
        es.put("emergency.returned", "[Regreso de Emergencia] ¡Regresado de forma segura al mundo principal!");
        es.put("emergency.broadcast", "{0} usó el regreso de emergencia");
        
        // Progreso y logros
        es.put("progress.report_generated", "Informe de progreso de aprendizaje generado");
        es.put("progress.reset", "Progreso de aprendizaje reiniciado");
        es.put("achievement.earned", "Logro obtenido: {0}");
        es.put("milestone.completed", "Hito completado: {0}");
        
        // Agente
        es.put("agent.summoned", "Agente '{0}' invocado");
        es.put("agent.dismissed", "Agente despedido");
        es.put("agent.following", "El agente está siguiendo");
        es.put("agent.stopped_following", "El agente dejó de seguir");
        
        // Mensajes de error
        es.put("error.not_found", "No encontrado");
        es.put("error.permission_denied", "Permiso denegado");
        es.put("error.invalid_parameters", "Parámetros inválidos");
        es.put("error.server_error", "Error del servidor");
        es.put("error.student_not_found", "Estudiante no encontrado: {0}");
        es.put("error.already_exists", "Ya existe");
        
        // Mensajes de construcción
        es.put("building.circle_created", "Círculo creado");
        es.put("building.sphere_created", "Esfera creada");
        es.put("building.wall_created", "Pared creada");
        es.put("building.house_created", "Casa creada");
        es.put("building.area_filled", "Área rellenada");
        es.put("building.area_cleared", "Área despejada");
        
        // Configuración de idioma
        es.put("language.changed", "Idioma cambiado a {0}");
        es.put("language.current", "Idioma actual: {0}");
        es.put("language.default_set", "Idioma predeterminado establecido a {0}");
        es.put("language.unsupported", "Idioma no compatible");
        
        languages.put(SPANISH, es);
    }
    
    /**
     * フランス語メッセージの初期化
     */
    private void initializeFrench() {
        Map<String, String> fr = new HashMap<>();
        
        // Messages de base
        fr.put("welcome", "Bienvenue dans le Système d'Apprentissage Collaboratif Minecraft!");
        fr.put("connection.success", "Connexion réussie");
        fr.put("connection.failed", "Échec de la connexion");
        
        // Fonctions enseignant
        fr.put("teacher.registered", "Enregistré comme compte enseignant");
        fr.put("teacher.access_required", "Accès enseignant requis");
        fr.put("classroom.mode_enabled", "Mode classe activé");
        fr.put("classroom.mode_disabled", "Mode classe désactivé");
        fr.put("classroom.permissions_updated", "Permissions mises à jour");
        fr.put("classroom.students_frozen", "Tous les étudiants figés");
        fr.put("classroom.students_unfrozen", "Tous les étudiants débloqués");
        fr.put("classroom.students_summoned", "Tous les étudiants convoqués");
        
        // Messages étudiant
        fr.put("student.activity_paused", "Activité en pause");
        fr.put("student.activity_resumed", "Veuillez reprendre l'activité");
        fr.put("student.time_limit_set", "Limite de temps définie : {0} minutes");
        fr.put("student.time_limit_removed", "Limite de temps supprimée");
        fr.put("student.restriction_added", "Restriction ajoutée : {0}");
        fr.put("student.summoned", "Rassemblé à l'emplacement de l'enseignant");
        
        // Fonctions collaboratives
        fr.put("invitation.sent", "Invitation envoyée à {0}");
        fr.put("invitation.received", "Invitation reçue de {0} !");
        fr.put("visit.requested", "{0} veut visiter");
        fr.put("visit.approved", "Visite approuvée ! Bienvenue dans le monde de {0} !");
        fr.put("home.returned", "Retourné au monde principal");
        fr.put("emergency.returned", "[Retour d'Urgence] Retour sécurisé au monde principal !");
        fr.put("emergency.broadcast", "{0} a utilisé le retour d'urgence");
        
        // Progrès et réalisations
        fr.put("progress.report_generated", "Rapport de progrès d'apprentissage généré");
        fr.put("progress.reset", "Progrès d'apprentissage réinitialisé");
        fr.put("achievement.earned", "Réussite obtenue : {0}");
        fr.put("milestone.completed", "Étape franchie : {0}");
        
        // Agent
        fr.put("agent.summoned", "Agent '{0}' invoqué");
        fr.put("agent.dismissed", "Agent renvoyé");
        fr.put("agent.following", "L'agent suit");
        fr.put("agent.stopped_following", "L'agent a arrêté de suivre");
        
        // Messages d'erreur
        fr.put("error.not_found", "Non trouvé");
        fr.put("error.permission_denied", "Permission refusée");
        fr.put("error.invalid_parameters", "Paramètres invalides");
        fr.put("error.server_error", "Erreur du serveur");
        fr.put("error.student_not_found", "Étudiant non trouvé : {0}");
        fr.put("error.already_exists", "Existe déjà");
        
        // Messages de construction
        fr.put("building.circle_created", "Cercle créé");
        fr.put("building.sphere_created", "Sphère créée");
        fr.put("building.wall_created", "Mur créé");
        fr.put("building.house_created", "Maison créée");
        fr.put("building.area_filled", "Zone remplie");
        fr.put("building.area_cleared", "Zone nettoyée");
        
        // Paramètres de langue
        fr.put("language.changed", "Langue changée en {0}");
        fr.put("language.current", "Langue actuelle : {0}");
        fr.put("language.default_set", "Langue par défaut définie sur {0}");
        fr.put("language.unsupported", "Langue non prise en charge");
        
        languages.put(FRENCH, fr);
    }
    
    /**
     * ドイツ語メッセージの初期化
     */
    private void initializeGerman() {
        Map<String, String> de = new HashMap<>();
        
        // Grundnachrichten
        de.put("welcome", "Willkommen im Minecraft Kollaborativen Lernsystem!");
        de.put("connection.success", "Verbindung erfolgreich");
        de.put("connection.failed", "Verbindung fehlgeschlagen");
        
        // Lehrerfunktionen
        de.put("teacher.registered", "Als Lehrerkonto registriert");
        de.put("teacher.access_required", "Lehrerzugang erforderlich");
        de.put("classroom.mode_enabled", "Klassenzimmermodus aktiviert");
        de.put("classroom.mode_disabled", "Klassenzimmermodus deaktiviert");
        de.put("classroom.permissions_updated", "Berechtigungen aktualisiert");
        de.put("classroom.students_frozen", "Alle Schüler pausiert");
        de.put("classroom.students_unfrozen", "Alle Schüler wieder freigegeben");
        de.put("classroom.students_summoned", "Alle Schüler einberufen");
        
        // Schülernachrichten
        de.put("student.activity_paused", "Aktivität pausiert");
        de.put("student.activity_resumed", "Bitte setzen Sie die Aktivität fort");
        de.put("student.time_limit_set", "Zeitlimit gesetzt: {0} Minuten");
        de.put("student.time_limit_removed", "Zeitlimit entfernt");
        de.put("student.restriction_added", "Beschränkung hinzugefügt: {0}");
        de.put("student.summoned", "Am Lehrerstandort versammelt");
        
        // Kollaborative Funktionen
        de.put("invitation.sent", "Einladung an {0} gesendet");
        de.put("invitation.received", "Einladung von {0} erhalten!");
        de.put("visit.requested", "{0} möchte besuchen");
        de.put("visit.approved", "Besuch genehmigt! Willkommen in {0}s Welt!");
        de.put("home.returned", "Zur Heimatwelt zurückgekehrt");
        de.put("emergency.returned", "[Notfall-Rückkehr] Sicher zur Heimatwelt zurückgekehrt!");
        de.put("emergency.broadcast", "{0} hat Notfall-Rückkehr verwendet");
        
        // Fortschritt und Errungenschaften
        de.put("progress.report_generated", "Lernfortschrittsbericht erstellt");
        de.put("progress.reset", "Lernfortschritt zurückgesetzt");
        de.put("achievement.earned", "Errungenschaft erhalten: {0}");
        de.put("milestone.completed", "Meilenstein abgeschlossen: {0}");
        
        // Agent
        de.put("agent.summoned", "Agent '{0}' beschworen");
        de.put("agent.dismissed", "Agent entlassen");
        de.put("agent.following", "Agent folgt");
        de.put("agent.stopped_following", "Agent folgt nicht mehr");
        
        // Fehlernachrichten
        de.put("error.not_found", "Nicht gefunden");
        de.put("error.permission_denied", "Berechtigung verweigert");
        de.put("error.invalid_parameters", "Ungültige Parameter");
        de.put("error.server_error", "Serverfehler");
        de.put("error.student_not_found", "Schüler nicht gefunden: {0}");
        de.put("error.already_exists", "Existiert bereits");
        
        // Baunachrichten
        de.put("building.circle_created", "Kreis erstellt");
        de.put("building.sphere_created", "Kugel erstellt");
        de.put("building.wall_created", "Wand erstellt");
        de.put("building.house_created", "Haus erstellt");
        de.put("building.area_filled", "Bereich gefüllt");
        de.put("building.area_cleared", "Bereich geleert");
        
        // Spracheinstellungen
        de.put("language.changed", "Sprache geändert zu {0}");
        de.put("language.current", "Aktuelle Sprache: {0}");
        de.put("language.default_set", "Standardsprache auf {0} gesetzt");
        de.put("language.unsupported", "Nicht unterstützte Sprache");
        
        languages.put(GERMAN, de);
    }
    
    /**
     * メッセージを取得
     */
    public String getMessage(String key, String language, Object... args) {
        String cacheKey = language + ":" + key;
        String message = messageCache.get(cacheKey);
        
        if (message == null) {
            Map<String, String> langMap = languages.get(language);
            if (langMap == null) {
                langMap = languages.get(defaultLanguage);
            }
            
            message = langMap.get(key);
            if (message == null) {
                message = "[Missing: " + key + "]";
                LOGGER.warn("Missing translation for key '{}' in language '{}'", key, language);
            }
            
            messageCache.put(cacheKey, message);
        }
        
        // パラメータ置換
        if (args.length > 0) {
            message = String.format(message, args);
        }
        
        return message;
    }
    
    /**
     * プレイヤーの言語でメッセージを取得
     */
    public String getMessage(UUID playerUUID, String key, Object... args) {
        String language = userLanguages.getOrDefault(playerUUID, defaultLanguage);
        return getMessage(key, language, args);
    }
    
    /**
     * プレイヤーの言語を設定
     */
    public void setPlayerLanguage(UUID playerUUID, String language) {
        if (languages.containsKey(language)) {
            userLanguages.put(playerUUID, language);
            LOGGER.info("Set language for player {} to {}", playerUUID, language);
        } else {
            LOGGER.warn("Unsupported language: {}", language);
        }
    }
    
    /**
     * プレイヤーの言語を取得
     */
    public String getPlayerLanguage(UUID playerUUID) {
        return userLanguages.getOrDefault(playerUUID, defaultLanguage);
    }
    
    /**
     * サポートされている言語一覧を取得
     */
    public Set<String> getSupportedLanguages() {
        return new HashSet<>(languages.keySet());
    }
    
    /**
     * デフォルト言語を設定
     */
    public void setDefaultLanguage(String language) {
        if (languages.containsKey(language)) {
            this.defaultLanguage = language;
            LOGGER.info("Default language set to {}", language);
        }
    }
    
    /**
     * 総メッセージ数を取得
     */
    public int getTotalMessageCount() {
        return languages.values().stream()
            .mapToInt(Map::size)
            .sum();
    }
    
    /**
     * 言語のメッセージ数を取得
     */
    public int getMessageCount(String language) {
        Map<String, String> langMap = languages.get(language);
        return langMap != null ? langMap.size() : 0;
    }
    
    /**
     * 言語名を表示用に取得
     */
    public String getLanguageDisplayName(String languageCode) {
        switch (languageCode) {
            case JAPANESE: return "日本語";
            case ENGLISH: return "English";
            case CHINESE_SIMPLIFIED: return "简体中文";
            case CHINESE_TRADITIONAL: return "繁體中文";
            case KOREAN: return "한국어";
            case SPANISH: return "Español";
            case FRENCH: return "Français";
            case GERMAN: return "Deutsch";
            default: return languageCode;
        }
    }
    
    /**
     * メッセージキャッシュをクリア
     */
    public void clearCache() {
        messageCache.clear();
        LOGGER.info("Message cache cleared");
    }
    
    /**
     * 言語データをエクスポート
     */
    public Map<String, Object> exportLanguageData() {
        Map<String, Object> data = new HashMap<>();
        data.put("supportedLanguages", getSupportedLanguages());
        data.put("defaultLanguage", defaultLanguage);
        data.put("totalMessages", getTotalMessageCount());
        data.put("userLanguages", new HashMap<>(userLanguages));
        
        Map<String, Integer> messageCounts = new HashMap<>();
        for (String lang : languages.keySet()) {
            messageCounts.put(lang, getMessageCount(lang));
        }
        data.put("messageCounts", messageCounts);
        
        return data;
    }
}
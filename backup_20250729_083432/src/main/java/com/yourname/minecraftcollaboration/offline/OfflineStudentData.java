package com.yourname.minecraftcollaboration.offline;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * オフライン時の学生データキャッシュ
 */
public class OfflineStudentData {
    private final UUID studentUUID;
    private final Map<String, Object> data;
    private LocalDateTime lastUpdated;
    private LocalDateTime lastSynced;
    private boolean needsSync;
    private int changeCount;
    
    public OfflineStudentData(UUID studentUUID) {
        this.studentUUID = studentUUID;
        this.data = new HashMap<>();
        this.lastUpdated = LocalDateTime.now();
        this.needsSync = false;
        this.changeCount = 0;
    }
    
    /**
     * データを更新
     */
    public void updateData(Map<String, Object> newData) {
        for (Map.Entry<String, Object> entry : newData.entrySet()) {
            Object oldValue = data.get(entry.getKey());
            Object newValue = entry.getValue();
            
            if (!Objects.equals(oldValue, newValue)) {
                data.put(entry.getKey(), newValue);
                markAsChanged();
            }
        }
    }
    
    /**
     * 特定のフィールドを更新
     */
    public void updateField(String field, Object value) {
        Object oldValue = data.get(field);
        if (!Objects.equals(oldValue, value)) {
            data.put(field, value);
            markAsChanged();
        }
    }
    
    /**
     * データ変更をマーク
     */
    private void markAsChanged() {
        this.lastUpdated = LocalDateTime.now();
        this.needsSync = true;
        this.changeCount++;
    }
    
    /**
     * 同期完了をマーク
     */
    public void markSynced() {
        this.lastSynced = LocalDateTime.now();
        this.needsSync = false;
    }
    
    /**
     * 特定のフィールドを取得
     */
    public Object getField(String field) {
        return data.get(field);
    }
    
    /**
     * 特定のフィールドを安全に取得（デフォルト値付き）
     */
    public Object getField(String field, Object defaultValue) {
        return data.getOrDefault(field, defaultValue);
    }
    
    /**
     * 文字列フィールドを取得
     */
    public String getStringField(String field, String defaultValue) {
        Object value = data.get(field);
        return value instanceof String ? (String) value : defaultValue;
    }
    
    /**
     * 整数フィールドを取得
     */
    public int getIntField(String field, int defaultValue) {
        Object value = data.get(field);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    /**
     * ブールフィールドを取得
     */
    public boolean getBooleanField(String field, boolean defaultValue) {
        Object value = data.get(field);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        try {
            return Boolean.parseBoolean(value.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    /**
     * 進捗データを更新
     */
    public void updateProgress(String progressType, Object progressValue) {
        Map<String, Object> progressData = getProgressData();
        progressData.put(progressType, progressValue);
        updateField("progress", progressData);
    }
    
    /**
     * 進捗データを取得
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getProgressData() {
        Object progress = data.get("progress");
        if (progress instanceof Map) {
            return new HashMap<>((Map<String, Object>) progress);
        }
        return new HashMap<>();
    }
    
    /**
     * 活動データを追加
     */
    public void addActivity(String activityType, Object activityData) {
        List<Map<String, Object>> activities = getActivities();
        
        Map<String, Object> activity = new HashMap<>();
        activity.put("type", activityType);
        activity.put("data", activityData);
        activity.put("timestamp", LocalDateTime.now().toString());
        
        activities.add(activity);
        
        // 最新100件のみ保持
        if (activities.size() > 100) {
            activities = activities.subList(activities.size() - 100, activities.size());
        }
        
        updateField("activities", activities);
    }
    
    /**
     * 活動データを取得
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getActivities() {
        Object activities = data.get("activities");
        if (activities instanceof List) {
            return new ArrayList<>((List<Map<String, Object>>) activities);
        }
        return new ArrayList<>();
    }
    
    /**
     * 最近の活動を取得
     */
    public List<Map<String, Object>> getRecentActivities(int count) {
        List<Map<String, Object>> activities = getActivities();
        int size = activities.size();
        int fromIndex = Math.max(0, size - count);
        return activities.subList(fromIndex, size);
    }
    
    /**
     * 達成度を追加
     */
    public void addAchievement(String achievementId, String achievementName) {
        Set<String> achievements = getAchievements();
        achievements.add(achievementId + ":" + achievementName);
        updateField("achievements", new ArrayList<>(achievements));
    }
    
    /**
     * 達成度一覧を取得
     */
    @SuppressWarnings("unchecked")
    public Set<String> getAchievements() {
        Object achievements = data.get("achievements");
        if (achievements instanceof List) {
            return new HashSet<>((List<String>) achievements);
        }
        return new HashSet<>();
    }
    
    /**
     * JSONフォーマットで出力
     */
    public String toJson() {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"studentUUID\": \"").append(studentUUID).append("\",\n");
        json.append("  \"lastUpdated\": \"").append(lastUpdated.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\",\n");
        
        if (lastSynced != null) {
            json.append("  \"lastSynced\": \"").append(lastSynced.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\",\n");
        }
        
        json.append("  \"needsSync\": ").append(needsSync).append(",\n");
        json.append("  \"changeCount\": ").append(changeCount).append(",\n");
        json.append("  \"dataFields\": ").append(data.size()).append(",\n");
        json.append("  \"data\": {\n");
        
        boolean first = true;
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (!first) json.append(",\n");
            
            String key = entry.getKey();
            Object value = entry.getValue();
            
            json.append("    \"").append(key).append("\": ");
            
            if (value instanceof String) {
                json.append("\"").append(value.toString().replace("\"", "\\\"")).append("\"");
            } else if (value instanceof Number || value instanceof Boolean) {
                json.append(value);
            } else if (value instanceof List || value instanceof Map) {
                // 複雑なオブジェクトは文字列として保存
                json.append("\"").append(value.toString().replace("\"", "\\\"")).append("\"");
            } else {
                json.append("\"").append(value != null ? value.toString() : "null").append("\"");
            }
            
            first = false;
        }
        
        json.append("\n  }\n");
        json.append("}");
        
        return json.toString();
    }
    
    /**
     * JSONから復元
     */
    public static OfflineStudentData fromJson(String jsonContent) {
        // 簡易的なJSON解析
        String uuidStr = extractJsonValue(jsonContent, "studentUUID");
        UUID studentUUID = UUID.fromString(uuidStr);
        
        OfflineStudentData studentData = new OfflineStudentData(studentUUID);
        
        // その他のフィールドを復元
        String lastUpdatedStr = extractJsonValue(jsonContent, "lastUpdated");
        if (lastUpdatedStr != null) {
            studentData.lastUpdated = LocalDateTime.parse(lastUpdatedStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
        
        String lastSyncedStr = extractJsonValue(jsonContent, "lastSynced");
        if (lastSyncedStr != null && !lastSyncedStr.isEmpty()) {
            studentData.lastSynced = LocalDateTime.parse(lastSyncedStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
        
        String needsSyncStr = extractJsonValue(jsonContent, "needsSync");
        if (needsSyncStr != null) {
            studentData.needsSync = Boolean.parseBoolean(needsSyncStr);
        }
        
        String changeCountStr = extractJsonValue(jsonContent, "changeCount");
        if (changeCountStr != null) {
            try {
                studentData.changeCount = Integer.parseInt(changeCountStr);
            } catch (NumberFormatException e) {
                studentData.changeCount = 0;
            }
        }
        
        // データセクションを抽出
        Map<String, Object> data = extractDataSection(jsonContent);
        studentData.data.putAll(data);
        
        return studentData;
    }
    
    /**
     * JSON文字列から値を抽出する簡易パーサー
     */
    private static String extractJsonValue(String json, String key) {
        String searchPattern = "\"" + key + "\": \"";
        int startIndex = json.indexOf(searchPattern);
        
        if (startIndex == -1) {
            // 数値/ブール値の場合のパターンも試行
            searchPattern = "\"" + key + "\": ";
            startIndex = json.indexOf(searchPattern);
            if (startIndex == -1) {
                return null;
            }
            startIndex += searchPattern.length();
            int endIndex = json.indexOf(",", startIndex);
            if (endIndex == -1) {
                endIndex = json.indexOf("}", startIndex);
            }
            if (endIndex == -1) {
                endIndex = json.indexOf("\n", startIndex);
            }
            return endIndex != -1 ? json.substring(startIndex, endIndex).trim() : null;
        }
        
        startIndex += searchPattern.length();
        int endIndex = json.indexOf("\"", startIndex);
        
        return endIndex != -1 ? json.substring(startIndex, endIndex) : null;
    }
    
    /**
     * dataセクションを抽出
     */
    private static Map<String, Object> extractDataSection(String json) {
        Map<String, Object> data = new HashMap<>();
        
        int dataStart = json.indexOf("\"data\": {");
        if (dataStart == -1) {
            return data;
        }
        
        dataStart = json.indexOf("{", dataStart);
        int braceCount = 1;
        int currentPos = dataStart + 1;
        
        while (currentPos < json.length() && braceCount > 0) {
            char c = json.charAt(currentPos);
            if (c == '{') braceCount++;
            else if (c == '}') braceCount--;
            currentPos++;
        }
        
        if (braceCount == 0) {
            String dataSection = json.substring(dataStart + 1, currentPos - 1);
            parseSimpleKeyValuePairs(dataSection, data);
        }
        
        return data;
    }
    
    /**
     * 簡単なキー値ペアを解析
     */
    private static void parseSimpleKeyValuePairs(String section, Map<String, Object> data) {
        String[] lines = section.split(",");
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            int colonIndex = line.indexOf(":");
            if (colonIndex == -1) continue;
            
            String key = line.substring(0, colonIndex).trim();
            String value = line.substring(colonIndex + 1).trim();
            
            // クォートを除去
            key = key.replaceAll("^\"|\"$", "");
            value = value.replaceAll("^\"|\"$", "");
            
            data.put(key, value);
        }
    }
    
    /**
     * データ概要の取得
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Student: ").append(studentUUID).append("\n");
        summary.append("Data fields: ").append(data.size()).append("\n");
        summary.append("Changes: ").append(changeCount).append("\n");
        summary.append("Needs sync: ").append(needsSync).append("\n");
        summary.append("Last updated: ").append(lastUpdated.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))).append("\n");
        
        if (lastSynced != null) {
            summary.append("Last synced: ").append(lastSynced.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))).append("\n");
        }
        
        // 主要なデータ概要
        summary.append("Progress items: ").append(getProgressData().size()).append("\n");
        summary.append("Activities: ").append(getActivities().size()).append("\n");
        summary.append("Achievements: ").append(getAchievements().size()).append("\n");
        
        return summary.toString();
    }
    
    // Getters
    public UUID getStudentUUID() {
        return studentUUID;
    }
    
    public Map<String, Object> getData() {
        return new HashMap<>(data);
    }
    
    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }
    
    public LocalDateTime getLastSynced() {
        return lastSynced;
    }
    
    public boolean needsSync() {
        return needsSync;
    }
    
    public int getChangeCount() {
        return changeCount;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        OfflineStudentData that = (OfflineStudentData) obj;
        return Objects.equals(studentUUID, that.studentUUID);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(studentUUID);
    }
    
    @Override
    public String toString() {
        return String.format("OfflineStudentData[uuid=%s, fields=%d, changes=%d, needsSync=%s]",
            studentUUID, data.size(), changeCount, needsSync);
    }
}
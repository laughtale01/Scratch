package com.yourname.minecraftcollaboration.offline;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * オフライン時に実行されたアクションのデータモデル
 */
public class OfflineAction {
    private final String actionId;
    private final String actionType;
    private final Map<String, Object> actionData;
    private final LocalDateTime timestamp;
    private String syncStatus;
    private LocalDateTime syncTime;
    private String syncError;
    private int retryCount;
    
    public OfflineAction(String actionId, String actionType, Map<String, Object> actionData, LocalDateTime timestamp) {
        this.actionId = actionId;
        this.actionType = actionType;
        this.actionData = new HashMap<>(actionData);
        this.timestamp = timestamp;
        this.syncStatus = "PENDING";
        this.retryCount = 0;
    }
    
    /**
     * 同期成功をマーク
     */
    public void markSyncSuccess() {
        this.syncStatus = "SYNCED";
        this.syncTime = LocalDateTime.now();
        this.syncError = null;
    }
    
    /**
     * 同期失敗をマーク
     */
    public void markSyncFailure(String error) {
        this.syncStatus = "FAILED";
        this.syncTime = LocalDateTime.now();
        this.syncError = error;
        this.retryCount++;
    }
    
    /**
     * リトライ準備
     */
    public void prepareRetry() {
        this.syncStatus = "PENDING";
        this.syncError = null;
    }
    
    /**
     * アクションデータを追加
     */
    public void addActionData(String key, Object value) {
        actionData.put(key, value);
    }
    
    /**
     * 最大リトライ回数に達したかチェック
     */
    public boolean hasExceededMaxRetries(int maxRetries) {
        return retryCount >= maxRetries;
    }
    
    /**
     * JSONフォーマットで出力
     */
    public String toJson() {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"actionId\": \"").append(actionId).append("\",\n");
        json.append("  \"actionType\": \"").append(actionType).append("\",\n");
        json.append("  \"timestamp\": \"").append(timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\",\n");
        json.append("  \"syncStatus\": \"").append(syncStatus).append("\",\n");
        json.append("  \"retryCount\": ").append(retryCount).append(",\n");
        
        if (syncTime != null) {
            json.append("  \"syncTime\": \"").append(syncTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\",\n");
        }
        
        if (syncError != null) {
            json.append("  \"syncError\": \"").append(syncError.replace("\"", "\\\"")).append("\",\n");
        }
        
        json.append("  \"actionData\": {\n");
        
        boolean first = true;
        for (Map.Entry<String, Object> entry : actionData.entrySet()) {
            if (!first) json.append(",\n");
            
            Object value = entry.getValue();
            if (value instanceof String) {
                json.append("    \"").append(entry.getKey()).append("\": \"")
                    .append(value.toString().replace("\"", "\\\"")).append("\"");
            } else if (value instanceof Number || value instanceof Boolean) {
                json.append("    \"").append(entry.getKey()).append("\": ").append(value);
            } else {
                json.append("    \"").append(entry.getKey()).append("\": \"").append(value).append("\"");
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
    public static OfflineAction fromJson(String jsonContent) {
        // 簡易的なJSON解析
        String actionId = extractJsonValue(jsonContent, "actionId");
        String actionType = extractJsonValue(jsonContent, "actionType");
        String timestampStr = extractJsonValue(jsonContent, "timestamp");
        
        LocalDateTime timestamp = LocalDateTime.parse(timestampStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        
        // actionDataセクションを抽出
        Map<String, Object> actionData = extractActionData(jsonContent);
        
        OfflineAction action = new OfflineAction(actionId, actionType, actionData, timestamp);
        
        // その他のフィールドを復元
        String syncStatus = extractJsonValue(jsonContent, "syncStatus");
        if (syncStatus != null) {
            action.syncStatus = syncStatus;
        }
        
        String retryCountStr = extractJsonValue(jsonContent, "retryCount");
        if (retryCountStr != null) {
            try {
                action.retryCount = Integer.parseInt(retryCountStr);
            } catch (NumberFormatException e) {
                action.retryCount = 0;
            }
        }
        
        String syncTimeStr = extractJsonValue(jsonContent, "syncTime");
        if (syncTimeStr != null && !syncTimeStr.isEmpty()) {
            action.syncTime = LocalDateTime.parse(syncTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
        
        String syncError = extractJsonValue(jsonContent, "syncError");
        if (syncError != null) {
            action.syncError = syncError;
        }
        
        return action;
    }
    
    /**
     * JSON文字列から値を抽出する簡易パーサー
     */
    private static String extractJsonValue(String json, String key) {
        String searchPattern = "\"" + key + "\": \"";
        int startIndex = json.indexOf(searchPattern);
        
        if (startIndex == -1) {
            // 数値の場合のパターンも試行
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
     * actionDataセクションを抽出
     */
    private static Map<String, Object> extractActionData(String json) {
        Map<String, Object> data = new HashMap<>();
        
        int actionDataStart = json.indexOf("\"actionData\": {");
        if (actionDataStart == -1) {
            return data;
        }
        
        actionDataStart = json.indexOf("{", actionDataStart);
        int braceCount = 1;
        int currentPos = actionDataStart + 1;
        
        while (currentPos < json.length() && braceCount > 0) {
            char c = json.charAt(currentPos);
            if (c == '{') braceCount++;
            else if (c == '}') braceCount--;
            currentPos++;
        }
        
        if (braceCount == 0) {
            String actionDataSection = json.substring(actionDataStart + 1, currentPos - 1);
            parseSimpleKeyValuePairs(actionDataSection, data);
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
     * アクション概要の取得
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Action: ").append(actionType).append(" (").append(actionId).append(")\n");
        summary.append("Time: ").append(timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        summary.append("Status: ").append(syncStatus).append("\n");
        
        if (retryCount > 0) {
            summary.append("Retries: ").append(retryCount).append("\n");
        }
        
        if (syncError != null) {
            summary.append("Error: ").append(syncError).append("\n");
        }
        
        summary.append("Data: ").append(actionData.size()).append(" fields\n");
        
        return summary.toString();
    }
    
    // Getters
    public String getActionId() {
        return actionId;
    }
    
    public String getActionType() {
        return actionType;
    }
    
    public Map<String, Object> getActionData() {
        return new HashMap<>(actionData);
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public String getSyncStatus() {
        return syncStatus;
    }
    
    public LocalDateTime getSyncTime() {
        return syncTime;
    }
    
    public String getSyncError() {
        return syncError;
    }
    
    public int getRetryCount() {
        return retryCount;
    }
    
    public boolean isPending() {
        return "PENDING".equals(syncStatus);
    }
    
    public boolean isSynced() {
        return "SYNCED".equals(syncStatus);
    }
    
    public boolean isFailed() {
        return "FAILED".equals(syncStatus);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        OfflineAction that = (OfflineAction) obj;
        return Objects.equals(actionId, that.actionId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(actionId);
    }
    
    @Override
    public String toString() {
        return String.format("OfflineAction[id=%s, type=%s, status=%s, retries=%d, time=%s]",
            actionId, actionType, syncStatus, retryCount, 
            timestamp.format(DateTimeFormatter.ofPattern("MM-dd HH:mm")));
    }
}
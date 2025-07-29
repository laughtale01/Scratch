package com.yourname.minecraftcollaboration.offline;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * オフラインセッションのデータモデル
 */
public class OfflineSession {
    private final String sessionId;
    private final LocalDateTime startTime;
    private LocalDateTime endTime;
    private final List<String> participantIds;
    private final Map<String, Object> sessionData;
    private int totalActions;
    private String sessionStatus;
    
    public OfflineSession(String sessionId, LocalDateTime startTime) {
        this.sessionId = sessionId;
        this.startTime = startTime;
        this.participantIds = new ArrayList<>();
        this.sessionData = new HashMap<>();
        this.totalActions = 0;
        this.sessionStatus = "ACTIVE";
    }
    
    /**
     * セッションを終了
     */
    public void endSession() {
        this.endTime = LocalDateTime.now();
        this.sessionStatus = "COMPLETED";
    }
    
    /**
     * 参加者を追加
     */
    public void addParticipant(String participantId) {
        if (!participantIds.contains(participantId)) {
            participantIds.add(participantId);
        }
    }
    
    /**
     * セッションデータを追加
     */
    public void addSessionData(String key, Object value) {
        sessionData.put(key, value);
    }
    
    /**
     * アクション数を増加
     */
    public void incrementActionCount() {
        totalActions++;
    }
    
    /**
     * セッション期間を取得
     */
    public long getSessionDurationMinutes() {
        LocalDateTime end = endTime != null ? endTime : LocalDateTime.now();
        return java.time.Duration.between(startTime, end).toMinutes();
    }
    
    /**
     * JSONフォーマットで出力
     */
    public String toJson() {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"sessionId\": \"").append(sessionId).append("\",\n");
        json.append("  \"startTime\": \"").append(startTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\",\n");
        
        if (endTime != null) {
            json.append("  \"endTime\": \"").append(endTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\",\n");
        }
        
        json.append("  \"sessionStatus\": \"").append(sessionStatus).append("\",\n");
        json.append("  \"totalActions\": ").append(totalActions).append(",\n");
        json.append("  \"durationMinutes\": ").append(getSessionDurationMinutes()).append(",\n");
        json.append("  \"participantCount\": ").append(participantIds.size()).append(",\n");
        json.append("  \"participants\": [");
        
        for (int i = 0; i < participantIds.size(); i++) {
            if (i > 0) json.append(", ");
            json.append("\"").append(participantIds.get(i)).append("\"");
        }
        
        json.append("],\n");
        json.append("  \"sessionData\": {\n");
        
        boolean first = true;
        for (Map.Entry<String, Object> entry : sessionData.entrySet()) {
            if (!first) json.append(",\n");
            json.append("    \"").append(entry.getKey()).append("\": \"").append(entry.getValue()).append("\"");
            first = false;
        }
        
        json.append("\n  }\n");
        json.append("}");
        
        return json.toString();
    }
    
    /**
     * JSONから復元
     */
    public static OfflineSession fromJson(String jsonContent) {
        // 簡易的なJSON解析（実際のプロジェクトではJSONライブラリを使用推奨）
        String sessionId = extractJsonValue(jsonContent, "sessionId");
        String startTimeStr = extractJsonValue(jsonContent, "startTime");
        
        LocalDateTime startTime = LocalDateTime.parse(startTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        OfflineSession session = new OfflineSession(sessionId, startTime);
        
        // その他のフィールドを復元
        String endTimeStr = extractJsonValue(jsonContent, "endTime");
        if (endTimeStr != null && !endTimeStr.isEmpty()) {
            session.endTime = LocalDateTime.parse(endTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
        
        String statusStr = extractJsonValue(jsonContent, "sessionStatus");
        if (statusStr != null) {
            session.sessionStatus = statusStr;
        }
        
        String actionsStr = extractJsonValue(jsonContent, "totalActions");
        if (actionsStr != null) {
            try {
                session.totalActions = Integer.parseInt(actionsStr);
            } catch (NumberFormatException e) {
                session.totalActions = 0;
            }
        }
        
        return session;
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
     * セッション概要の取得
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Session: ").append(sessionId).append("\n");
        summary.append("Status: ").append(sessionStatus).append("\n");
        summary.append("Duration: ").append(getSessionDurationMinutes()).append(" minutes\n");
        summary.append("Participants: ").append(participantIds.size()).append("\n");
        summary.append("Total Actions: ").append(totalActions).append("\n");
        summary.append("Started: ").append(startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))).append("\n");
        
        if (endTime != null) {
            summary.append("Ended: ").append(endTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))).append("\n");
        }
        
        return summary.toString();
    }
    
    // Getters
    public String getSessionId() {
        return sessionId;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    public List<String> getParticipantIds() {
        return new ArrayList<>(participantIds);
    }
    
    public Map<String, Object> getSessionData() {
        return new HashMap<>(sessionData);
    }
    
    public int getTotalActions() {
        return totalActions;
    }
    
    public String getSessionStatus() {
        return sessionStatus;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        OfflineSession that = (OfflineSession) obj;
        return Objects.equals(sessionId, that.sessionId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(sessionId);
    }
    
    @Override
    public String toString() {
        return String.format("OfflineSession[id=%s, status=%s, participants=%d, actions=%d, duration=%dmin]",
            sessionId, sessionStatus, participantIds.size(), totalActions, getSessionDurationMinutes());
    }
}
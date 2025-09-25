package edu.minecraft.collaboration.offline;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.List;
import java.util.ArrayList;

/**
 * 繧ｪ繝輔Λ繧､繝ｳ繧ｻ繝・す繝ｧ繝ｳ縺ｮ繝（繧ｿ繝｢繝・Ν
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
     * End session
     */
    public void endSession() {
        this.endTime = LocalDateTime.now();
        this.sessionStatus = "COMPLETED";
    }

    /**
     * Set end time manually
     */
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    /**
     * 蜿ょ刈閠・ｒ霑ｽ蜉
     */
    public void addParticipant(String participantId) {
        if (!participantIds.contains(participantId)) {
            participantIds.add(participantId);
        }
    }

    /**
     * 繧ｻ繝・す繝ｧ繝ｳ繝（繧ｿ繧定ｿｽ蜉
     */
    public void addSessionData(String key, Object value) {
        sessionData.put(key, value);
    }

    /**
     * 繧｢繧ｯ繧ｷ繝ｧ繝ｳ謨ｰ繧貞｢怜刈
     */
    public void incrementActionCount() {
        totalActions++;
    }

    /**
     * 繧ｻ繝・す繝ｧ繝ｳ譛滄俣繧貞叙蠕・     */
    public long getSessionDurationMinutes() {
        LocalDateTime end = endTime != null ? endTime : LocalDateTime.now();
        return java.time.Duration.between(startTime, end).toMinutes();
    }

    /**
     * JSON繝輔か繝ｼ繝槭ャ繝医〒蜃ｺ蜉・     */
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
            if (i > 0) {
                json.append(", ");
            }
            json.append("\"").append(participantIds.get(i)).append("\"");
        }

        json.append("],\n");
        json.append("  \"sessionData\": {\n");

        boolean first = true;
        for (Map.Entry<String, Object> entry : sessionData.entrySet()) {
            if (!first) {
                json.append(",\n");
            }
            json.append("    \"").append(entry.getKey()).append("\": \"").append(entry.getValue()).append("\"");
            first = false;
        }

        json.append("\n  }\n");
        json.append("}");

        return json.toString();
    }

    /**
     * Create from JSON
     */
    public static OfflineSession fromJson(String jsonContent) {
        // Simple JSON parsing for basic project - would recommend using JSON library in production
        String extractedSessionId = extractJsonValue(jsonContent, "sessionId");
        String startTimeStr = extractJsonValue(jsonContent, "startTime");

        LocalDateTime extractedStartTime = LocalDateTime.parse(startTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        OfflineSession session = new OfflineSession(extractedSessionId, extractedStartTime);

        // Other fields restoration
        String extractedEndTimeStr = extractJsonValue(jsonContent, "endTime");
        if (extractedEndTimeStr != null && !extractedEndTimeStr.isEmpty()) {
            session.endTime = LocalDateTime.parse(extractedEndTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
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
     * JSON譁・ｭ怜・縺九ｉ蛟､繧呈歓蜃ｺ縺吶ｋ邁｡譏薙ヱ繝ｼ繧ｵ繝ｼ
     */
    private static String extractJsonValue(String json, String key) {
        String searchPattern = "\"" + key + "\": \"";
        int startIndex = json.indexOf(searchPattern);

        if (startIndex == -1) {
            // 謨ｰ蛟､縺ｮ蝣ｴ蜷医・繝代ち繝ｼ繝ｳ繧りｩｦ陦・            searchPattern = "\"" + key + "\": ";
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
     * 繧ｻ繝・す繝ｧ繝ｳ讎りｦ√・蜿門ｾ・     */
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

    /**
     * Convert to Map for serialization
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("sessionId", sessionId);
        map.put("startTime", startTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        if (endTime != null) {
            map.put("endTime", endTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        map.put("participantIds", new ArrayList<>(participantIds));
        map.put("sessionData", new HashMap<>(sessionData));
        map.put("totalActions", totalActions);
        map.put("sessionStatus", sessionStatus);
        map.put("durationMinutes", getSessionDurationMinutes());
        return map;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
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

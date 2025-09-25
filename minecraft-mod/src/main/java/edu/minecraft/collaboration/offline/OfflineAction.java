package edu.minecraft.collaboration.offline;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 繧ｪ繝輔Λ繧､繝ｳ譎ゅ↓螳溯｡後＆繧後◆繧｢繧ｯ繧ｷ繝ｧ繝ｳ縺ｮ繝（繧ｿ繝｢繝・Ν
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
     * Constructor with auto-generated ID
     */
    public OfflineAction(String actionType, Map<String, Object> actionData, LocalDateTime timestamp) {
        this(UUID.randomUUID().toString(), actionType, actionData, timestamp);
    }

    /**
     * 蜷梧悄謌仙粥繧偵・繝ｼ繧ｯ
     */
    public void markSyncSuccess() {
        this.syncStatus = "SYNCED";
        this.syncTime = LocalDateTime.now();
        this.syncError = null;
    }

    /**
     * 蜷梧悄螟ｱ謨励ｒ繝槭・繧ｯ
     */
    public void markSyncFailure(String error) {
        this.syncStatus = "FAILED";
        this.syncTime = LocalDateTime.now();
        this.syncError = error;
        this.retryCount++;
    }

    /**
     * 繝ｪ繝医Λ繧､貅門ｙ
     */
    public void prepareRetry() {
        this.syncStatus = "PENDING";
        this.syncError = null;
    }

    /**
     * 繧｢繧ｯ繧ｷ繝ｧ繝ｳ繝（繧ｿ繧定ｿｽ蜉
     */
    public void addActionData(String key, Object value) {
        actionData.put(key, value);
    }

    /**
     * 譛螟ｧ繝ｪ繝医Λ繧､蝗樊焚縺ｫ驕斐＠縺溘°繝√ぉ繝・け
     */
    public boolean hasExceededMaxRetries(int maxRetries) {
        return retryCount >= maxRetries;
    }

    /**
     * JSON繝輔か繝ｼ繝槭ャ繝医〒蜃ｺ蜉・     */
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
            if (!first) {
                json.append(",\n");
            }

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
     * Create from JSON
     */
    public static OfflineAction fromJson(String jsonContent) {
        // Simple JSON parsing for basic project - would recommend using JSON library in production
        String extractedActionId = extractJsonValue(jsonContent, "actionId");
        String extractedActionType = extractJsonValue(jsonContent, "actionType");
        String timestampStr = extractJsonValue(jsonContent, "timestamp");

        LocalDateTime extractedTimestamp = LocalDateTime.parse(timestampStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        // actionData section extraction
        Map<String, Object> extractedActionData = extractActionData(jsonContent);

        OfflineAction action = new OfflineAction(extractedActionId, extractedActionType, extractedActionData, extractedTimestamp);

        // Other fields restoration
        String extractedSyncStatus = extractJsonValue(jsonContent, "syncStatus");
        if (extractedSyncStatus != null) {
            action.syncStatus = extractedSyncStatus;
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
     * actionData繧ｻ繧ｯ繧ｷ繝ｧ繝ｳ繧呈歓蜃ｺ
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
            if (c == '{') {
                braceCount++;
            } else if (c == '}') {
                braceCount--;
            }
            currentPos++;
        }

        if (braceCount == 0) {
            String actionDataSection = json.substring(actionDataStart + 1, currentPos - 1);
            parseSimpleKeyValuePairs(actionDataSection, data);
        }

        return data;
    }

    /**
     * 邁｡蜊倥↑繧ｭ繝ｼ蛟､繝壹い繧定ｧ｣譫・     */
    private static void parseSimpleKeyValuePairs(String section, Map<String, Object> data) {
        String[] lines = section.split(",");

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }

            int colonIndex = line.indexOf(":");
            if (colonIndex == -1) {
                continue;
            }

            String key = line.substring(0, colonIndex).trim();
            String value = line.substring(colonIndex + 1).trim();

            // 繧ｯ繧ｩ繝ｼ繝医ｒ髯､蜴ｻ
            key = key.replaceAll("^\"|\"$", "");
            value = value.replaceAll("^\"|\"$", "");

            data.put(key, value);
        }
    }

    /**
     * 繧｢繧ｯ繧ｷ繝ｧ繝ｳ讎りｦ√・蜿門ｾ・     */
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

    /**
     * Convert to Map for serialization
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("actionId", actionId);
        map.put("actionType", actionType);
        map.put("actionData", new HashMap<>(actionData));
        map.put("timestamp", timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        map.put("syncStatus", syncStatus);
        map.put("retryCount", retryCount);
        if (syncTime != null) {
            map.put("syncTime", syncTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        if (syncError != null) {
            map.put("syncError", syncError);
        }
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

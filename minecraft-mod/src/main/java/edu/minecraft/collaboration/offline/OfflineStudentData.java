package edu.minecraft.collaboration.offline;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

/**
 * 繧ｪ繝輔Λ繧､繝ｳ譎ゅ・蟄ｦ逕溘ョ繝ｼ繧ｿ繧ｭ繝｣繝・す繝･
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
     * 繝（繧ｿ繧呈峩譁ｰ
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
     * 迚ｹ螳壹・繝輔ぅ繝ｼ繝ｫ繝峨ｒ譖ｴ譁ｰ
     */
    public void updateField(String field, Object value) {
        Object oldValue = data.get(field);
        if (!Objects.equals(oldValue, value)) {
            data.put(field, value);
            markAsChanged();
        }
    }
    
    /**
     * 繝（繧ｿ螟画峩繧偵・繝ｼ繧ｯ
     */
    private void markAsChanged() {
        this.lastUpdated = LocalDateTime.now();
        this.needsSync = true;
        this.changeCount++;
    }
    
    /**
     * 蜷梧悄螳御ｺ・ｒ繝槭・繧ｯ
     */
    public void markSynced() {
        this.lastSynced = LocalDateTime.now();
        this.needsSync = false;
    }
    
    /**
     * 迚ｹ螳壹・繝輔ぅ繝ｼ繝ｫ繝峨ｒ蜿門ｾ・     */
    public Object getField(String field) {
        return data.get(field);
    }
    
    /**
     * 迚ｹ螳壹・繝輔ぅ繝ｼ繝ｫ繝峨ｒ螳牙・縺ｫ蜿門ｾ暦ｼ医ョ繝輔か繝ｫ繝亥､莉倥″（     */
    public Object getField(String field, Object defaultValue) {
        return data.getOrDefault(field, defaultValue);
    }
    
    /**
     * 譁・ｭ怜・繝輔ぅ繝ｼ繝ｫ繝峨ｒ蜿門ｾ・     */
    public String getStringField(String field, String defaultValue) {
        Object value = data.get(field);
        return value instanceof String ? (String) value : defaultValue;
    }
    
    /**
     * 謨ｴ謨ｰ繝輔ぅ繝ｼ繝ｫ繝峨ｒ蜿門ｾ・     */
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
     * 繝悶・繝ｫ繝輔ぅ繝ｼ繝ｫ繝峨ｒ蜿門ｾ・     */
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
     * 騾ｲ謐励ョ繝ｼ繧ｿ繧呈峩譁ｰ
     */
    public void updateProgress(String progressType, Object progressValue) {
        Map<String, Object> progressData = getProgressData();
        progressData.put(progressType, progressValue);
        updateField("progress", progressData);
    }
    
    /**
     * 騾ｲ謐励ョ繝ｼ繧ｿ繧貞叙蠕・     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getProgressData() {
        Object progress = data.get("progress");
        if (progress instanceof Map) {
            return new HashMap<>((Map<String, Object>) progress);
        }
        return new HashMap<>();
    }
    
    /**
     * 豢ｻ蜍輔ョ繝ｼ繧ｿ繧定ｿｽ蜉
     */
    public void addActivity(String activityType, Object activityData) {
        List<Map<String, Object>> activities = getActivities();
        
        Map<String, Object> activity = new HashMap<>();
        activity.put("type", activityType);
        activity.put("data", activityData);
        activity.put("timestamp", LocalDateTime.now().toString());
        
        activities.add(activity);
        
        // 譛譁ｰ100莉ｶ縺ｮ縺ｿ菫晄戟
        if (activities.size() > 100) {
            activities = activities.subList(activities.size() - 100, activities.size());
        }
        
        updateField("activities", activities);
    }
    
    /**
     * 豢ｻ蜍輔ョ繝ｼ繧ｿ繧貞叙蠕・     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getActivities() {
        Object activities = data.get("activities");
        if (activities instanceof List) {
            return new ArrayList<>((List<Map<String, Object>>) activities);
        }
        return new ArrayList<>();
    }
    
    /**
     * 譛霑代・豢ｻ蜍輔ｒ蜿門ｾ・     */
    public List<Map<String, Object>> getRecentActivities(int count) {
        List<Map<String, Object>> activities = getActivities();
        int size = activities.size();
        int fromIndex = Math.max(0, size - count);
        return activities.subList(fromIndex, size);
    }
    
    /**
     * 驕疲・蠎ｦ繧定ｿｽ蜉
     */
    public void addAchievement(String achievementId, String achievementName) {
        Set<String> achievements = getAchievements();
        achievements.add(achievementId + ":" + achievementName);
        updateField("achievements", new ArrayList<>(achievements));
    }
    
    /**
     * 驕疲・蠎ｦ荳隕ｧ繧貞叙蠕・     */
    @SuppressWarnings("unchecked")
    public Set<String> getAchievements() {
        Object achievements = data.get("achievements");
        if (achievements instanceof List) {
            return new HashSet<>((List<String>) achievements);
        }
        return new HashSet<>();
    }
    
    /**
     * JSON繝輔か繝ｼ繝槭ャ繝医〒蜃ｺ蜉・     */
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
            if (!first) {
                json.append(",\n");
            }
            
            String key = entry.getKey();
            Object value = entry.getValue();
            
            json.append("    \"").append(key).append("\": ");
            
            if (value instanceof String) {
                json.append("\"").append(value.toString().replace("\"", "\\\"")).append("\"");
            } else if (value instanceof Number || value instanceof Boolean) {
                json.append(value);
            } else if (value instanceof List || value instanceof Map) {
                // Complex objects stored as string
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
     * Create from JSON
     */
    public static OfflineStudentData fromJson(String jsonContent) {
        // Simple JSON parsing for basic project - would recommend using JSON library in production
        String extractedUuidStr = extractJsonValue(jsonContent, "studentUUID");
        UUID extractedStudentUUID = UUID.fromString(extractedUuidStr);
        
        OfflineStudentData studentData = new OfflineStudentData(extractedStudentUUID);
        
        // Other fields restoration
        String extractedLastUpdatedStr = extractJsonValue(jsonContent, "lastUpdated");
        if (extractedLastUpdatedStr != null) {
            studentData.lastUpdated = LocalDateTime.parse(extractedLastUpdatedStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
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
        
        // 繝（繧ｿ繧ｻ繧ｯ繧ｷ繝ｧ繝ｳ繧呈歓蜃ｺ
        Map<String, Object> data = extractDataSection(jsonContent);
        studentData.data.putAll(data);
        
        return studentData;
    }
    
    /**
     * JSON譁・ｭ怜・縺九ｉ蛟､繧呈歓蜃ｺ縺吶ｋ邁｡譏薙ヱ繝ｼ繧ｵ繝ｼ
     */
    private static String extractJsonValue(String json, String key) {
        String searchPattern = "\"" + key + "\": \"";
        int startIndex = json.indexOf(searchPattern);
        
        if (startIndex == -1) {
            // 謨ｰ蛟､/繝悶・繝ｫ蛟､縺ｮ蝣ｴ蜷医・繝代ち繝ｼ繝ｳ繧りｩｦ陦・            searchPattern = "\"" + key + "\": ";
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
     * data繧ｻ繧ｯ繧ｷ繝ｧ繝ｳ繧呈歓蜃ｺ
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
            if (c == '{') {
                braceCount++;
            } else if (c == '}') {
                braceCount--;
            }
            currentPos++;
        }
        
        if (braceCount == 0) {
            String dataSection = json.substring(dataStart + 1, currentPos - 1);
            parseSimpleKeyValuePairs(dataSection, data);
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
     * 繝（繧ｿ讎りｦ√・蜿門ｾ・     */
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
        
        // 荳ｻ隕√↑繝（繧ｿ讎りｦ・        summary.append("Progress items: ").append(getProgressData().size()).append("\n");
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
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
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
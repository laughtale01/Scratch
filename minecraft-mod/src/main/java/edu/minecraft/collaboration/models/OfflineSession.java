package edu.minecraft.collaboration.models;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents an offline session with metadata and tracking information
 */
public class OfflineSession {
    
    private final String sessionId;
    private final LocalDateTime startTime;
    private LocalDateTime endTime;
    private SessionStatus status;
    private final Map<String, Object> metadata;
    private int actionsCount;
    private long dataSize;
    
    public enum SessionStatus {
        ACTIVE, COMPLETED, SYNCED, FAILED, EXPIRED
    }
    
    public OfflineSession(String sessionId, LocalDateTime startTime) {
        this.sessionId = sessionId;
        this.startTime = startTime;
        this.status = SessionStatus.ACTIVE;
        this.metadata = new HashMap<>();
        this.actionsCount = 0;
        this.dataSize = 0;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
        if (status == SessionStatus.ACTIVE) {
            this.status = SessionStatus.COMPLETED;
        }
    }
    
    public SessionStatus getStatus() {
        return status;
    }
    
    public void setStatus(SessionStatus status) {
        this.status = status;
    }
    
    public Map<String, Object> getMetadata() {
        return new HashMap<>(metadata);
    }
    
    public void addMetadata(String key, Object value) {
        metadata.put(key, value);
    }
    
    public void removeMetadata(String key) {
        metadata.remove(key);
    }
    
    public Object getMetadata(String key) {
        return metadata.get(key);
    }
    
    public int getActionsCount() {
        return actionsCount;
    }
    
    public void setActionsCount(int actionsCount) {
        this.actionsCount = actionsCount;
    }
    
    public void incrementActionsCount() {
        this.actionsCount++;
    }
    
    public long getDataSize() {
        return dataSize;
    }
    
    public void setDataSize(long dataSize) {
        this.dataSize = dataSize;
    }
    
    public void addDataSize(long additionalSize) {
        this.dataSize += additionalSize;
    }
    
    public long getDurationMinutes() {
        if (endTime == null) {
            return java.time.Duration.between(startTime, LocalDateTime.now()).toMinutes();
        }
        return java.time.Duration.between(startTime, endTime).toMinutes();
    }
    
    public boolean isExpired() {
        return startTime.isBefore(LocalDateTime.now().minusDays(7));
    }
    
    public boolean isActive() {
        return status == SessionStatus.ACTIVE;
    }
    
    /**
     * Convert to map for serialization
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("sessionId", sessionId);
        map.put("startTime", startTime.toString());
        if (endTime != null) {
            map.put("endTime", endTime.toString());
        }
        map.put("status", status.toString());
        map.put("metadata", new HashMap<>(metadata));
        map.put("actionsCount", actionsCount);
        map.put("dataSize", dataSize);
        map.put("durationMinutes", getDurationMinutes());
        return map;
    }
    
    /**
     * Convert to JSON string
     */
    public String toJson() {
        Map<String, Object> map = toMap();
        // Simple JSON representation for now
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) {
                json.append(",");
            }
            json.append("\"").append(entry.getKey()).append("\":");
            if (entry.getValue() instanceof String) {
                json.append("\"").append(entry.getValue()).append("\"");
            } else if (entry.getValue() instanceof Map) {
                json.append("{}"); // Simplified for now
            } else {
                json.append(entry.getValue());
            }
            first = false;
        }
        json.append("}");
        return json.toString();
    }
    
    /**
     * Create from map (deserialization)
     */
    public static OfflineSession fromMap(Map<String, Object> map) {
        String sessionId = (String) map.get("sessionId");
        LocalDateTime startTime = LocalDateTime.parse((String) map.get("startTime"));
        
        OfflineSession session = new OfflineSession(sessionId, startTime);
        
        if (map.containsKey("endTime") && map.get("endTime") != null) {
            session.setEndTime(LocalDateTime.parse((String) map.get("endTime")));
        }
        
        session.setStatus(SessionStatus.valueOf((String) map.get("status")));
        session.setActionsCount(((Number) map.get("actionsCount")).intValue());
        session.setDataSize(((Number) map.get("dataSize")).longValue());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) map.get("metadata");
        if (metadata != null) {
            session.metadata.putAll(metadata);
        }
        
        return session;
    }
    
    @Override
    public String toString() {
        return "OfflineSession{" +
                "sessionId='" + sessionId + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", status=" + status +
                ", actionsCount=" + actionsCount +
                ", durationMinutes=" + getDurationMinutes() +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OfflineSession that = (OfflineSession) o;
        return Objects.equals(sessionId, that.sessionId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(sessionId);
    }
}
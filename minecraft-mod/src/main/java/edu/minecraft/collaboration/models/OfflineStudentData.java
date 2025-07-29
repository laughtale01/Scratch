package edu.minecraft.collaboration.models;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents cached student data for offline access
 */
public class OfflineStudentData {
    
    private final UUID studentUUID;
    private final Map<String, Object> data;
    private LocalDateTime lastUpdated;
    private LocalDateTime lastAccessed;
    private boolean isDirty;
    
    public OfflineStudentData(UUID studentUUID) {
        this.studentUUID = studentUUID;
        this.data = new HashMap<>();
        this.lastUpdated = LocalDateTime.now();
        this.lastAccessed = LocalDateTime.now();
        this.isDirty = false;
    }
    
    public UUID getStudentUUID() {
        return studentUUID;
    }
    
    public Map<String, Object> getData() {
        this.lastAccessed = LocalDateTime.now();
        return new HashMap<>(data);
    }
    
    public void updateData(Map<String, Object> newData) {
        this.data.clear();
        this.data.putAll(newData);
        this.lastUpdated = LocalDateTime.now();
        this.isDirty = true;
    }
    
    public void setData(String key, Object value) {
        this.data.put(key, value);
        this.lastUpdated = LocalDateTime.now();
        this.isDirty = true;
    }
    
    public Object getData(String key) {
        this.lastAccessed = LocalDateTime.now();
        return data.get(key);
    }
    
    public boolean containsKey(String key) {
        return data.containsKey(key);
    }
    
    public void removeData(String key) {
        this.data.remove(key);
        this.lastUpdated = LocalDateTime.now();
        this.isDirty = true;
    }
    
    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }
    
    public LocalDateTime getLastAccessed() {
        return lastAccessed;
    }
    
    public boolean isDirty() {
        return isDirty;
    }
    
    public void markClean() {
        this.isDirty = false;
    }
    
    public void markDirty() {
        this.isDirty = true;
        this.lastUpdated = LocalDateTime.now();
    }
    
    public boolean isStale() {
        return lastUpdated.isBefore(LocalDateTime.now().minusHours(1));
    }
    
    public boolean isExpired() {
        return lastAccessed.isBefore(LocalDateTime.now().minusDays(7));
    }
    
    public int getDataSize() {
        return data.size();
    }
    
    public void clear() {
        this.data.clear();
        this.lastUpdated = LocalDateTime.now();
        this.isDirty = true;
    }
    
    /**
     * Convert to JSON string for persistence
     */
    public String toJson() {
        Map<String, Object> exportData = new HashMap<>();
        exportData.put("studentUUID", studentUUID.toString());
        exportData.put("data", new HashMap<>(data));
        exportData.put("lastUpdated", lastUpdated.toString());
        exportData.put("lastAccessed", lastAccessed.toString());
        exportData.put("isDirty", isDirty);
        
        // Simple JSON representation
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : exportData.entrySet()) {
            if (!first) {
                json.append(",");
            }
            json.append("\"").append(entry.getKey()).append("\":");
            if (entry.getValue() instanceof String) {
                json.append("\"").append(entry.getValue()).append("\"");
            } else if (entry.getValue() instanceof Boolean) {
                json.append(entry.getValue());
            } else if (entry.getValue() instanceof Map) {
                json.append("{}"); // Simplified for now
            } else {
                json.append("\"").append(entry.getValue()).append("\"");
            }
            first = false;
        }
        json.append("}");
        return json.toString();
    }
    
    /**
     * Create from map (for deserialization)
     */
    public static OfflineStudentData fromMap(Map<String, Object> map) {
        UUID studentUUID = UUID.fromString((String) map.get("studentUUID"));
        OfflineStudentData studentData = new OfflineStudentData(studentUUID);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) map.get("data");
        if (data != null) {
            studentData.data.putAll(data);
        }
        
        if (map.containsKey("lastUpdated")) {
            studentData.lastUpdated = LocalDateTime.parse((String) map.get("lastUpdated"));
        }
        
        if (map.containsKey("lastAccessed")) {
            studentData.lastAccessed = LocalDateTime.parse((String) map.get("lastAccessed"));
        }
        
        if (map.containsKey("isDirty")) {
            studentData.isDirty = (Boolean) map.get("isDirty");
        }
        
        return studentData;
    }
    
    @Override
    public String toString() {
        return "OfflineStudentData{" +
                "studentUUID=" + studentUUID +
                ", dataSize=" + data.size() +
                ", lastUpdated=" + lastUpdated +
                ", isDirty=" + isDirty +
                ", isStale=" + isStale() +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OfflineStudentData that = (OfflineStudentData) o;
        return Objects.equals(studentUUID, that.studentUUID);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(studentUUID);
    }
}
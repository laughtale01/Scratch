package edu.minecraft.collaboration.models;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents an action performed while offline that needs to be synchronized
 */
public class OfflineAction {
    
    private final UUID id;
    private final String actionType;
    private final Map<String, Object> actionData;
    private final LocalDateTime timestamp;
    private ActionStatus status;
    private String errorMessage;
    private int retryCount;
    
    public enum ActionStatus {
        PENDING, PROCESSING, COMPLETED, FAILED, EXPIRED
    }
    
    public OfflineAction(String actionType, Map<String, Object> actionData, LocalDateTime timestamp) {
        this.id = UUID.randomUUID();
        this.actionType = actionType;
        this.actionData = new HashMap<>(actionData);
        this.timestamp = timestamp;
        this.status = ActionStatus.PENDING;
        this.retryCount = 0;
    }
    
    public UUID getId() {
        return id;
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
    
    public ActionStatus getStatus() {
        return status;
    }
    
    public void setStatus(ActionStatus status) {
        this.status = status;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public int getRetryCount() {
        return retryCount;
    }
    
    public void incrementRetryCount() {
        this.retryCount++;
    }
    
    public boolean isExpired() {
        return timestamp.isBefore(LocalDateTime.now().minusHours(24));
    }
    
    public boolean canRetry() {
        return retryCount < 3 && status == ActionStatus.FAILED && !isExpired();
    }
    
    /**
     * Convert to map for serialization
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id.toString());
        map.put("actionType", actionType);
        map.put("actionData", new HashMap<>(actionData));
        map.put("timestamp", timestamp.toString());
        map.put("status", status.toString());
        map.put("errorMessage", errorMessage);
        map.put("retryCount", retryCount);
        return map;
    }
    
    /**
     * Create from map (deserialization)
     */
    public static OfflineAction fromMap(Map<String, Object> map) {
        String actionType = (String) map.get("actionType");
        @SuppressWarnings("unchecked")
        Map<String, Object> actionData = (Map<String, Object>) map.get("actionData");
        LocalDateTime timestamp = LocalDateTime.parse((String) map.get("timestamp"));
        
        OfflineAction action = new OfflineAction(actionType, actionData, timestamp);
        action.setStatus(ActionStatus.valueOf((String) map.get("status")));
        action.setErrorMessage((String) map.get("errorMessage"));
        action.retryCount = ((Number) map.get("retryCount")).intValue();
        
        return action;
    }
    
    @Override
    public String toString() {
        return "OfflineAction{" +
                "id=" + id +
                ", actionType='" + actionType + '\'' +
                ", timestamp=" + timestamp +
                ", status=" + status +
                ", retryCount=" + retryCount +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OfflineAction that = (OfflineAction) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
package edu.minecraft.collaboration.monitoring.alerts;

import java.time.Instant;
import java.util.List;

/**
 * Interface for alert notification channels
 */
public interface NotificationChannel {
    
    /**
     * Get the name of this notification channel
     */
    String getName();
    
    /**
     * Send a notification message
     */
    NotificationResult sendNotification(NotificationMessage message);
    
    /**
     * Get the alert severities supported by this channel
     */
    List<AlertSeverity> getSupportedSeverities();
}

/**
 * Notification message
 */
class NotificationMessage {
    
    private final String title;
    private final String content;
    private final AlertSeverity severity;
    private final Instant timestamp;
    
    public NotificationMessage(String title, String content, AlertSeverity severity) {
        this.title = title;
        this.content = content;
        this.severity = severity;
        this.timestamp = Instant.now();
    }
    
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public AlertSeverity getSeverity() { return severity; }
    public Instant getTimestamp() { return timestamp; }
    
    @Override
    public String toString() {
        return String.format(\"NotificationMessage{title='%s', severity=%s}\", title, severity);
    }
}

/**
 * Result of sending a notification
 */
class NotificationResult {
    
    private final boolean success;
    private final String message;
    private final Instant timestamp;
    
    private NotificationResult(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.timestamp = Instant.now();
    }
    
    public static NotificationResult success(String message) {
        return new NotificationResult(true, message);
    }
    
    public static NotificationResult failure(String message) {
        return new NotificationResult(false, message);
    }
    
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public Instant getTimestamp() { return timestamp; }
    
    @Override
    public String toString() {
        return String.format(\"NotificationResult{success=%s, message='%s'}\", success, message);
    }
}

/**
 * Record of a notification that was sent
 */
class NotificationRecord {
    
    private final String alertId;
    private final String ruleName;
    private final AlertSeverity severity;
    private final int channelCount;
    private final Instant timestamp;
    
    public NotificationRecord(String alertId, String ruleName, AlertSeverity severity, 
                            int channelCount, Instant timestamp) {
        this.alertId = alertId;
        this.ruleName = ruleName;
        this.severity = severity;
        this.channelCount = channelCount;
        this.timestamp = timestamp;
    }
    
    public String getAlertId() { return alertId; }
    public String getRuleName() { return ruleName; }
    public AlertSeverity getSeverity() { return severity; }
    public int getChannelCount() { return channelCount; }
    public Instant getTimestamp() { return timestamp; }
    
    @Override
    public String toString() {
        return String.format(\"NotificationRecord{rule='%s', severity=%s, channels=%d, time=%s}\",
            ruleName, severity, channelCount, timestamp);
    }
}"
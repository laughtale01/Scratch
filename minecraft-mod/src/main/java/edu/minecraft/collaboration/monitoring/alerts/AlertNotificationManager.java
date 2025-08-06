package edu.minecraft.collaboration.monitoring.alerts;

import edu.minecraft.collaboration.MinecraftCollaborationMod;
import org.slf4j.Logger;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Simplified Alert Notification Manager
 * Manages alert notifications and channels
 */
public class AlertNotificationManager {
    
    private static final Logger LOGGER = MinecraftCollaborationMod.getLogger();
    
    private final List<NotificationChannel> channels = new CopyOnWriteArrayList<>();
    private final Map<String, NotificationResult> lastResults = new ConcurrentHashMap<>();
    
    public AlertNotificationManager() {
        // Initialize with default console channel
        channels.add(new ConsoleNotificationChannel());
    }
    
    /**
     * Send alert to all configured channels
     */
    public void sendAlert(Alert alert) {
        NotificationMessage message = createMessage(alert);
        
        for (NotificationChannel channel : channels) {
            try {
                NotificationResult result = channel.send(message);
                lastResults.put(channel.getName(), result);
                
                if (!result.isSuccess()) {
                    LOGGER.warn("Failed to send alert via {}: {}", 
                        channel.getName(), result.getErrorMessage());
                }
            } catch (Exception e) {
                LOGGER.error("Error sending alert via {}", channel.getName(), e);
            }
        }
    }
    
    /**
     * Add a notification channel
     */
    public void addChannel(NotificationChannel channel) {
        channels.add(channel);
        LOGGER.info("Added notification channel: {}", channel.getName());
    }
    
    /**
     * Remove a notification channel
     */
    public void removeChannel(String channelName) {
        channels.removeIf(c -> c.getName().equals(channelName));
        lastResults.remove(channelName);
        LOGGER.info("Removed notification channel: {}", channelName);
    }
    
    /**
     * Get all channel statuses
     */
    public Map<String, NotificationResult> getChannelStatuses() {
        return new ConcurrentHashMap<>(lastResults);
    }
    
    /**
     * Create notification message from alert
     */
    private NotificationMessage createMessage(Alert alert) {
        return new NotificationMessage(
            alert.getRuleName(),
            alert.getDescription(),
            alert.getSeverity().toString(),
            alert.getCreatedAt(),
            alert.getDetails()
        );
    }
    
    /**
     * Shutdown manager
     */
    public void shutdown() {
        for (NotificationChannel channel : channels) {
            try {
                channel.close();
            } catch (Exception e) {
                LOGGER.error("Error closing channel {}", channel.getName(), e);
            }
        }
        channels.clear();
        lastResults.clear();
    }
    
    /**
     * Console notification channel implementation
     */
    private static class ConsoleNotificationChannel implements NotificationChannel {
        
        @Override
        public String getName() {
            return "console";
        }
        
        @Override
        public NotificationResult send(NotificationMessage message) {
            try {
                LOGGER.info("[ALERT] {} - {} [{}]", 
                    message.getTitle(), 
                    message.getContent(),
                    message.getSeverity());
                return NotificationResult.success("Console notification sent");
            } catch (Exception e) {
                return NotificationResult.failure("Console notification failed: " + e.getMessage());
            }
        }
        
        @Override
        public void close() {
            // Nothing to close for console
        }
    }
}

/**
 * Notification channel interface
 */
interface NotificationChannel {
    String getName();
    NotificationResult send(NotificationMessage message);
    default void close() {}
}

/**
 * Notification message
 */
class NotificationMessage {
    private final String title;
    private final String content;
    private final String severity;
    private final Instant timestamp;
    private final Map<String, Object> metadata;
    
    public NotificationMessage(String title, String content, String severity, 
                               Instant timestamp, Map<String, Object> metadata) {
        this.title = title;
        this.content = content;
        this.severity = severity;
        this.timestamp = timestamp;
        this.metadata = metadata;
    }
    
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getSeverity() { return severity; }
    public Instant getTimestamp() { return timestamp; }
    public Map<String, Object> getMetadata() { return metadata; }
}

/**
 * Notification result
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
    public String getErrorMessage() { return success ? null : message; }
    public Instant getTimestamp() { return timestamp; }
}
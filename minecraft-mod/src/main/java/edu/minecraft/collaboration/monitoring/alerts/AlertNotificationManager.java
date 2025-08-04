package edu.minecraft.collaboration.monitoring.alerts;

import edu.minecraft.collaboration.MinecraftCollaborationMod;
import org.slf4j.Logger;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Alert Notification Manager for sending alerts through various channels
 */
public class AlertNotificationManager {
    
    private static final Logger LOGGER = MinecraftCollaborationMod.getLogger();
    
    private final List<NotificationChannel> channels = new CopyOnWriteArrayList<>();
    private final List<NotificationRecord> notificationHistory = new CopyOnWriteArrayList<>();
    
    private volatile boolean enabled = true;
    
    public AlertNotificationManager() {
        // Initialize default notification channels
        initializeDefaultChannels();
    }
    
    /**
     * Send notification for an alert
     */
    public CompletableFuture<Void> sendNotification(Alert alert) {
        if (!enabled) {
            return CompletableFuture.completedFuture(null);
        }
        
        return CompletableFuture.runAsync(() -> {
            try {
                NotificationMessage message = createNotificationMessage(alert);
                
                // Send through all configured channels
                List<CompletableFuture<NotificationResult>> channelFutures = new ArrayList<>();
                
                for (NotificationChannel channel : channels) {
                    if (shouldNotifyThroughChannel(channel, alert)) {
                        CompletableFuture<NotificationResult> channelFuture = 
                            CompletableFuture.supplyAsync(() -> channel.sendNotification(message));
                        channelFutures.add(channelFuture);
                    }
                }
                
                // Wait for all notifications to complete
                CompletableFuture<Void> allChannels = CompletableFuture.allOf(
                    channelFutures.toArray(new CompletableFuture[0])
                );
                
                allChannels.thenRun(() -> {
                    // Record notification
                    NotificationRecord record = new NotificationRecord(
                        alert.getId(),
                        alert.getRuleName(),
                        alert.getSeverity(),
                        channelFutures.size(),
                        Instant.now()
                    );
                    notificationHistory.add(record);
                    
                    // Clean up old records
                    cleanupOldRecords();
                    
                    LOGGER.info(\"Sent alert notification: {} through {} channels\", 
                        alert.getRuleName(), channelFutures.size());
                });
                
            } catch (Exception e) {
                LOGGER.error(\"Error sending alert notification\", e);
            }
        });
    }
    
    /**
     * Add a notification channel
     */
    public void addNotificationChannel(NotificationChannel channel) {
        channels.add(channel);
        LOGGER.info(\"Added notification channel: {}\", channel.getName());
    }
    
    /**
     * Remove a notification channel
     */
    public void removeNotificationChannel(String channelName) {
        channels.removeIf(channel -> channel.getName().equals(channelName));
        LOGGER.info(\"Removed notification channel: {}\", channelName);
    }
    
    /**
     * Get notification history
     */
    public List<NotificationRecord> getNotificationHistory() {
        return new ArrayList<>(notificationHistory);
    }
    
    private void initializeDefaultChannels() {
        // Add console logging channel
        addNotificationChannel(new ConsoleNotificationChannel());
        
        // Add file logging channel
        addNotificationChannel(new FileNotificationChannel());
        
        // Email channel would be added here in a real implementation
        // addNotificationChannel(new EmailNotificationChannel());
        
        // Slack/Teams channel would be added here
        // addNotificationChannel(new SlackNotificationChannel());
    }
    
    private NotificationMessage createNotificationMessage(Alert alert) {
        String title = String.format(\"[%s] %s\", alert.getSeverity().name(), alert.getRuleName());
        
        StringBuilder content = new StringBuilder();
        content.append(\"Alert Details:\\n\");
        content.append(String.format(\"- Description: %s\\n\", alert.getDescription()));
        content.append(String.format(\"- Severity: %s\\n\", alert.getSeverity().getDisplayName()));
        content.append(String.format(\"- Created: %s\\n\", alert.getCreatedAt()));
        content.append(String.format(\"- Age: %d minutes\\n\", alert.getAgeInMinutes()));
        
        if (!alert.getDetails().isEmpty()) {
            content.append(\"\\nMetrics:\\n\");
            alert.getDetails().forEach((key, value) -> 
                content.append(String.format(\"- %s: %s\\n\", key, value))
            );
        }
        
        return new NotificationMessage(title, content.toString(), alert.getSeverity());
    }
    
    private boolean shouldNotifyThroughChannel(NotificationChannel channel, Alert alert) {
        // Check if channel supports this severity level
        return channel.getSupportedSeverities().contains(alert.getSeverity());
    }
    
    private void cleanupOldRecords() {
        Instant cutoff = Instant.now().minusSeconds(24 * 60 * 60); // 24 hours
        notificationHistory.removeIf(record -> record.getTimestamp().isBefore(cutoff));
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        LOGGER.info(\"Alert notifications {}\", enabled ? \"enabled\" : \"disabled\");
    }
    
    /**
     * Console notification channel
     */
    private static class ConsoleNotificationChannel implements NotificationChannel {
        
        @Override
        public String getName() {
            return \"console\";
        }
        
        @Override
        public NotificationResult sendNotification(NotificationMessage message) {
            try {
                String logLevel = switch (message.getSeverity()) {
                    case CRITICAL -> \"ERROR\";
                    case HIGH, WARNING -> \"WARN\";
                    default -> \"INFO\";
                };
                
                LOGGER.atLevel(ch.qos.logback.classic.Level.valueOf(logLevel))
                      .log(\"ALERT NOTIFICATION: {}\\n{}\", message.getTitle(), message.getContent());
                
                return NotificationResult.success(\"Console notification sent\");
                
            } catch (Exception e) {
                return NotificationResult.failure(\"Console notification failed: \" + e.getMessage());
            }
        }
        
        @Override
        public List<AlertSeverity> getSupportedSeverities() {
            return List.of(AlertSeverity.values());
        }
    }
    
    /**
     * File notification channel
     */
    private static class FileNotificationChannel implements NotificationChannel {
        
        @Override
        public String getName() {
            return \"file\";
        }
        
        @Override
        public NotificationResult sendNotification(NotificationMessage message) {
            try {
                // In a real implementation, this would write to a dedicated alert log file
                LOGGER.info(\"ALERT FILE LOG: {} - {}\", message.getTitle(), message.getContent());
                return NotificationResult.success(\"File notification logged\");
                
            } catch (Exception e) {
                return NotificationResult.failure(\"File notification failed: \" + e.getMessage());
            }
        }
        
        @Override
        public List<AlertSeverity> getSupportedSeverities() {
            return List.of(AlertSeverity.HIGH, AlertSeverity.CRITICAL);
        }
    }
}"
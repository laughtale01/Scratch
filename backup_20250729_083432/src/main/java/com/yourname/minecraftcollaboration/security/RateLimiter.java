package com.yourname.minecraftcollaboration.security;

import com.yourname.minecraftcollaboration.MinecraftCollaborationMod;
import org.slf4j.Logger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Rate limiter to prevent command spam and DoS attacks
 */
public class RateLimiter {
    private static final Logger LOGGER = MinecraftCollaborationMod.getLogger();
    private static RateLimiter instance;
    
    // Rate limit configuration from SecurityConfig
    private final int maxCommandsPerSecond = SecurityConfig.COMMAND_RATE_LIMIT_PER_SECOND;
    
    // Track command counts per player/connection
    private final ConcurrentHashMap<String, CommandTracker> commandTrackers = new ConcurrentHashMap<>();
    
    // Scheduled executor for cleanup
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
    
    private RateLimiter() {
        // Schedule cleanup of old entries every minute
        cleanupExecutor.scheduleAtFixedRate(this::cleanupOldEntries, 1, 1, TimeUnit.MINUTES);
    }
    
    public static RateLimiter getInstance() {
        if (instance == null) {
            instance = new RateLimiter();
        }
        return instance;
    }
    
    /**
     * Check if a command should be allowed based on rate limiting
     * @param identifier The player name or connection identifier
     * @return true if command is allowed, false if rate limit exceeded
     */
    public boolean allowCommand(String identifier) {
        CommandTracker tracker = commandTrackers.computeIfAbsent(identifier, k -> new CommandTracker());
        
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - tracker.windowStart;
        
        // Reset window if more than 1 second has passed
        if (elapsedTime >= 1000) {
            tracker.resetWindow(currentTime);
        }
        
        // Check if we've exceeded the rate limit
        if (tracker.commandCount.get() >= maxCommandsPerSecond) {
            LOGGER.warn("Rate limit exceeded for identifier: {} ({}+ commands/second)", 
                identifier, maxCommandsPerSecond);
            return false;
        }
        
        // Increment command count and allow
        tracker.commandCount.incrementAndGet();
        tracker.lastAccess = currentTime;
        return true;
    }
    
    /**
     * Get the current command count for an identifier
     * @param identifier The player name or connection identifier
     * @return current command count in the window
     */
    public int getCurrentCommandCount(String identifier) {
        CommandTracker tracker = commandTrackers.get(identifier);
        if (tracker == null) {
            return 0;
        }
        
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - tracker.windowStart;
        
        // Return 0 if window has expired
        if (elapsedTime >= 1000) {
            return 0;
        }
        
        return tracker.commandCount.get();
    }
    
    /**
     * Reset the rate limit for a specific identifier
     * @param identifier The player name or connection identifier
     */
    public void resetLimit(String identifier) {
        CommandTracker tracker = commandTrackers.get(identifier);
        if (tracker != null) {
            tracker.resetWindow(System.currentTimeMillis());
        }
    }
    
    /**
     * Clean up old entries that haven't been accessed recently
     */
    private void cleanupOldEntries() {
        long currentTime = System.currentTimeMillis();
        long maxIdleTime = TimeUnit.MINUTES.toMillis(5); // Remove entries idle for 5+ minutes
        
        commandTrackers.entrySet().removeIf(entry -> {
            CommandTracker tracker = entry.getValue();
            return (currentTime - tracker.lastAccess) > maxIdleTime;
        });
    }
    
    /**
     * Shutdown the rate limiter
     */
    public void shutdown() {
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Inner class to track commands per identifier
     */
    private static class CommandTracker {
        AtomicInteger commandCount = new AtomicInteger(0);
        volatile long windowStart = System.currentTimeMillis();
        volatile long lastAccess = System.currentTimeMillis();
        
        void resetWindow(long newWindowStart) {
            this.windowStart = newWindowStart;
            this.commandCount.set(0);
        }
    }
}
package edu.minecraft.collaboration.security;

import edu.minecraft.collaboration.MinecraftCollaborationMod;
import edu.minecraft.collaboration.config.ConfigurationManager;
import edu.minecraft.collaboration.core.ResourceManager;
import org.slf4j.Logger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Rate limiter to prevent command spam and DoS attacks.
 * Converted from singleton to dependency injection pattern.
 * Now integrates with ResourceManager for proper cleanup.
 */
public final class RateLimiter implements AutoCloseable {
    private static final Logger LOGGER = MinecraftCollaborationMod.getLogger();

    // Configuration
    private final ConfigurationManager configManager;
    private final int maxCommandsPerSecond;
    private final long cleanupIntervalMinutes;
    private final long maxIdleTimeMinutes;

    // Track command counts per player/connection
    private final ConcurrentHashMap<String, CommandTracker> commandTrackers = new ConcurrentHashMap<>();

    // Scheduled executor for cleanup - managed by ResourceManager
    private final ScheduledExecutorService cleanupExecutor;
    private final ResourceManager resourceManager;

    public RateLimiter(ConfigurationManager configManager) {
        this.configManager = configManager;
        this.resourceManager = ResourceManager.getInstance();
        this.maxCommandsPerSecond = configManager.getIntProperty("security.command.rate.limit.per.second", 10);
        this.cleanupIntervalMinutes = configManager.getLongProperty("rate.limiter.cleanup.interval.minutes", 1);
        this.maxIdleTimeMinutes = configManager.getLongProperty("rate.limiter.max.idle.time.minutes", 5);

        // Create executor with proper thread naming
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "RateLimiter-Cleanup");
            t.setDaemon(true);
            return t;
        });

        // Register executor with ResourceManager for proper cleanup
        resourceManager.registerExecutor("RateLimiter-cleanup", cleanupExecutor);

        // Schedule cleanup of old entries
        cleanupExecutor.scheduleAtFixedRate(this::cleanupOldEntries,
                                           cleanupIntervalMinutes,
                                           cleanupIntervalMinutes,
                                           TimeUnit.MINUTES);
        LOGGER.info("RateLimiter initialized with max {} commands per second", maxCommandsPerSecond);
    }

    /**
     * Check if a command should be allowed based on rate limiting
     * @param identifier The player name or connection identifier
     * @return true if command is allowed, false if rate limit exceeded
     */
    public synchronized boolean allowCommand(String identifier) {
        CommandTracker tracker = commandTrackers.computeIfAbsent(identifier, k -> new CommandTracker());

        long currentTime = System.currentTimeMillis();

        // Synchronize on the tracker to prevent race conditions
        synchronized (tracker) {
            long elapsedTime = currentTime - tracker.getWindowStart();

            // Reset window if more than 1 second has passed
            if (elapsedTime >= 1000) {
                tracker.resetWindow(currentTime);
            }

            // Check if we've exceeded the rate limit
            if (tracker.getCommandCount() >= maxCommandsPerSecond) {
                LOGGER.warn("Rate limit exceeded for identifier: {} ({}+ commands/second)",
                    identifier, maxCommandsPerSecond);
                return false;
            }

            // Increment command count and allow
            tracker.incrementAndGet();
            tracker.setLastAccess(currentTime);
            return true;
        }
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

        // Synchronize on the tracker to ensure consistent read
        synchronized (tracker) {
            long currentTime = System.currentTimeMillis();
            long elapsedTime = currentTime - tracker.getWindowStart();

            // Return 0 if window has expired
            if (elapsedTime >= 1000) {
                return 0;
            }

            return tracker.getCommandCount();
        }
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
        long maxIdleTime = TimeUnit.MINUTES.toMillis(maxIdleTimeMinutes);

        commandTrackers.entrySet().removeIf(entry -> {
            CommandTracker tracker = entry.getValue();
            return (currentTime - tracker.getLastAccess()) > maxIdleTime;
        });
    }

    /**
     * Shutdown the rate limiter - delegates to ResourceManager
     */
    public void shutdown() {
        close();
    }

    /**
     * Close the rate limiter and release resources
     */
    @Override
    public void close() {
        LOGGER.debug("Closing RateLimiter");
        // Clear tracking data
        commandTrackers.clear();
        // ResourceManager will handle executor shutdown
        resourceManager.unregisterAndShutdownExecutor("RateLimiter-cleanup");
    }

    /**
     * Inner class to track commands per identifier
     */
    private static class CommandTracker {
        private final AtomicInteger commandCount = new AtomicInteger(0);
        private volatile long windowStart = System.currentTimeMillis();
        private volatile long lastAccess = System.currentTimeMillis();

        int getCommandCount() {
            return commandCount.get();
        }

        int incrementAndGet() {
            return commandCount.incrementAndGet();
        }

        long getWindowStart() {
            return windowStart;
        }

        long getLastAccess() {
            return lastAccess;
        }

        void setLastAccess(long lastAccess) {
            this.lastAccess = lastAccess;
        }

        void resetWindow(long newWindowStart) {
            this.windowStart = newWindowStart;
            this.commandCount.set(0);
        }
    }
}

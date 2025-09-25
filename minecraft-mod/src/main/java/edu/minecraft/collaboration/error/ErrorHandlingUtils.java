package edu.minecraft.collaboration.error;

import edu.minecraft.collaboration.monitoring.MetricsCollector;
import edu.minecraft.collaboration.core.DependencyInjector;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Utility class for common error handling patterns
 */
public final class ErrorHandlingUtils {

    // Private constructor to prevent instantiation
    private ErrorHandlingUtils() {
        throw new UnsupportedOperationException("Utility class");
    }
    private static final Logger LOGGER = LoggerFactory.getLogger(ErrorHandlingUtils.class);
    private static MetricsCollector getMetrics() {
        return DependencyInjector.getInstance().getService(MetricsCollector.class);
    }
    private static final AdvancedErrorHandler ERROR_HANDLER = AdvancedErrorHandler.getInstance();

    /**
     * Execute block operation with error handling
     */
    public static boolean executeBlockOperation(String operationName,
                                              Runnable operation,
                                              ServerPlayer player) {
        AdvancedErrorHandler.ErrorResult<Void> result = ERROR_HANDLER.execute(
            "block." + operationName,
            () -> {
                operation.run();
                return null;
            }
        );

        if (!result.isSuccess()) {
            notifyPlayerError(player, "block.error." + operationName);
            return false;
        }

        getMetrics().incrementCounter(MetricsCollector.Metrics.BLOCKS_PLACED);
        return true;
    }

    /**
     * Execute WebSocket operation with retry
     */
    public static <T> T executeWebSocketOperation(String operationName,
                                                 Supplier<T> operation,
                                                 T defaultValue) {
        AdvancedErrorHandler.ErrorResult<T> result = ERROR_HANDLER.executeWithRetry(
            "websocket." + operationName,
            operation::get,
            3,  // max retries
            1000  // retry delay
        );

        return result.getValueOrDefault(defaultValue);
    }

    /**
     * Execute command with timeout
     */
    public static String executeCommandWithTimeout(String commandName,
                                                  Supplier<String> command,
                                                  long timeoutMs) {
        AdvancedErrorHandler.ErrorResult<String> result = ERROR_HANDLER.executeWithTimeout(
            "command." + commandName,
            command::get,
            timeoutMs
        );

        if (!result.isSuccess()) {
            getMetrics().incrementCounter(MetricsCollector.Metrics.COMMANDS_FAILED);
            return createErrorResponse(commandName, result.getError());
        }

        return result.getValue();
    }

    /**
     * Safe player operation with null checks
     */
    public static void safePlayerOperation(ServerPlayer player,
                                         Consumer<ServerPlayer> operation,
                                         String operationName) {
        if (player == null) {
            LOGGER.warn("Attempted {} on null player", operationName);
            getMetrics().incrementCounter("error.null_player." + operationName);
            return;
        }

        try {
            operation.accept(player);
        } catch (Exception e) {
            LOGGER.error("Error in player operation: {}", operationName, e);
            getMetrics().incrementCounter("error.player_operation." + operationName);
            notifyPlayerError(player, "error.operation.failed");
        }
    }

    /**
     * Safe coordinate validation
     */
    public static boolean validateCoordinates(int x, int y, int z) {
        // Minecraft world bounds
        if (y < -64 || y > 320) {
            LOGGER.warn("Invalid Y coordinate: {}", y);
            return false;
        }

        // Reasonable bounds for X/Z
        int maxCoordinate = 30_000_000;
        if (Math.abs(x) > maxCoordinate || Math.abs(z) > maxCoordinate) {
            LOGGER.warn("Coordinates out of bounds: {}, {}", x, z);
            return false;
        }

        return true;
    }

    /**
     * Safe block validation
     */
    public static boolean validateBlock(String blockId) {
        if (blockId == null || blockId.isEmpty()) {
            return false;
        }

        // Check against dangerous blocks
        String[] dangerousBlocks = {
            "tnt", "end_crystal", "respawn_anchor", "bed"
        };

        String lowerBlockId = blockId.toLowerCase();
        for (String dangerous : dangerousBlocks) {
            if (lowerBlockId.contains(dangerous)) {
                LOGGER.warn("Dangerous block rejected: {}", blockId);
                getMetrics().incrementCounter("error.dangerous_block." + dangerous);
                return false;
            }
        }

        return true;
    }

    /**
     * Notify player of error with localization
     */
    private static void notifyPlayerError(ServerPlayer player, String errorKey) {
        if (player != null) {
            player.sendSystemMessage(
                Component.literal("§c[Error] " + errorKey) // Red color
            );
        }
    }

    /**
     * Create standardized error response
     */
    private static String createErrorResponse(String operation, Exception error) {
        String errorType = error.getClass().getSimpleName();
        String message = error.getMessage() != null ? error.getMessage() : "Unknown error";

        return String.format(
            "{\"type\":\"error\",\"operation\":\"%s\",\"error\":\"%s\",\"message\":\"%s\"}",
            operation, errorType, message
        );
    }

    /**
     * Wrap operation with comprehensive error logging
     */
    public static <T> T wrapWithErrorLogging(String context, Supplier<T> operation) {
        long startTime = System.currentTimeMillis();

        try {
            T result = operation.get();
            long duration = System.currentTimeMillis() - startTime;

            if (duration > 1000) {
                LOGGER.warn("Slow operation in {}: {} ms", context, duration);
                getMetrics().incrementCounter("performance.slow_operation." + context);
            }

            return result;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            LOGGER.error("Error in {} after {} ms", context, duration, e);
            getMetrics().incrementCounter("error.context." + context);
            throw new RuntimeException("Error in " + context, e);
        }
    }

    /**
     * Batch error handler for multiple operations
     */
    public static class BatchErrorHandler {
        private int successCount = 0;
        private int failureCount = 0;
        private final String batchName;

        public BatchErrorHandler(String batchName) {
            this.batchName = batchName;
        }

        public void recordSuccess() {
            successCount++;
        }

        public void recordFailure(String reason) {
            failureCount++;
            LOGGER.debug("Batch {} failure: {}", batchName, reason);
        }

        public void complete() {
            LOGGER.info("Batch {} completed: {} successes, {} failures",
                       batchName, successCount, failureCount);

            getMetrics().incrementCounter("batch.success." + batchName, successCount);
            getMetrics().incrementCounter("batch.failure." + batchName, failureCount);

            if (failureCount > 0) {
                double failureRate = (failureCount * 100.0) / (successCount + failureCount);
                if (failureRate > 50) {
                    LOGGER.warn("High failure rate in batch {}: {:.1f}%", batchName, failureRate);
                }
            }
        }
    }
}

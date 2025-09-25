package edu.minecraft.collaboration.performance;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import edu.minecraft.collaboration.MinecraftCollaborationMod;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Optimized block placement for large-scale building operations
 * Uses batching and chunk-aware placement to improve performance
 */
public class BatchBlockPlacer {
    private static final Logger LOGGER = MinecraftCollaborationMod.getLogger();
    private static final int BATCH_SIZE = 1000;
    private static final int CHUNK_UPDATE_THRESHOLD = 10000;
    private static final long PLACEMENT_TIMEOUT_MS = 30000; // 30 seconds

    private final ServerLevel world;
    private final Queue<BlockPlacement> placementQueue;
    private final AtomicInteger blocksPlaced;
    private final Set<LevelChunk> affectedChunks;

    public BatchBlockPlacer(ServerLevel world) {
        this.world = world;
        this.placementQueue = new ConcurrentLinkedQueue<>();
        this.blocksPlaced = new AtomicInteger(0);
        this.affectedChunks = Collections.synchronizedSet(new HashSet<>());
    }

    /**
     * Queue a block for placement
     */
    public void queueBlock(BlockPos pos, BlockState state) {
        placementQueue.offer(new BlockPlacement(pos, state));
    }

    /**
     * Queue multiple blocks for placement
     */
    public void queueBlocks(Map<BlockPos, BlockState> blocks) {
        blocks.forEach((pos, state) -> queueBlock(pos, state));
    }

    /**
     * Place all queued blocks efficiently
     */
    public CompletableFuture<PlacementResult> placeBlocks() {
        CompletableFuture<PlacementResult> future = CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            int totalBlocks = placementQueue.size();
            int successCount = 0;
            int failCount = 0;

            LOGGER.info("Starting batch placement of {} blocks", totalBlocks);

            List<BlockPlacement> batch = new ArrayList<>(BATCH_SIZE);

            while (!placementQueue.isEmpty()) {
                // Build batch
                batch.clear();
                for (int i = 0; i < BATCH_SIZE && !placementQueue.isEmpty(); i++) {
                    batch.add(placementQueue.poll());
                }

                // Process batch
                world.getProfiler().push("batch_block_placement");
                for (BlockPlacement placement : batch) {
                    try {
                        // Place block with minimal updates
                        world.setBlock(placement.getPos(), placement.getState(), 2);

                        // Track affected chunk
                        LevelChunk chunk = world.getChunkAt(placement.getPos());
                        affectedChunks.add(chunk);

                        successCount++;
                        blocksPlaced.incrementAndGet();
                    } catch (Exception e) {
                        LOGGER.warn("Failed to place block at {}: {}", placement.getPos(), e.getMessage());
                        failCount++;
                    }
                }
                world.getProfiler().pop();

                // Update chunks periodically
                if (blocksPlaced.get() % CHUNK_UPDATE_THRESHOLD == 0) {
                    updateAffectedChunks();
                }
            }

            // Final chunk update
            updateAffectedChunks();

            long duration = System.currentTimeMillis() - startTime;
            double blocksPerSecond = (double) successCount / (duration / 1000.0);

            LOGGER.info("Batch placement complete: {} blocks placed in {}ms ({} blocks/sec)",
                successCount, duration, String.format("%.2f", blocksPerSecond));

            return new PlacementResult(totalBlocks, successCount, failCount, duration);
        });

        // Add timeout handling
        return future.orTimeout(PLACEMENT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .exceptionally(ex -> {
                if (ex.getCause() instanceof TimeoutException) {
                    LOGGER.error("Block placement timed out after {} ms", PLACEMENT_TIMEOUT_MS);
                    // Clear queue to prevent memory buildup
                    int remainingBlocks = placementQueue.size();
                    placementQueue.clear();
                    return new PlacementResult(remainingBlocks, 0, remainingBlocks, PLACEMENT_TIMEOUT_MS);
                }
                LOGGER.error("Error during block placement", ex);
                return new PlacementResult(0, 0, 0, 0);
            });
    }

    /**
     * Place blocks in a specific pattern with optimization
     */
    public CompletableFuture<PlacementResult> placePattern(
            List<BlockPos> positions, BlockState state) {

        if (positions == null || positions.isEmpty()) {
            return CompletableFuture.completedFuture(new PlacementResult(0, 0, 0, 0));
        }

        // Sort positions by chunk for better cache locality
        positions.sort((a, b) -> {
            int chunkCompare = Long.compare(
                getChunkKey(a), getChunkKey(b)
            );
            if (chunkCompare != 0) {
                return chunkCompare;
            }

            // Within same chunk, sort by Y then X then Z
            if (a.getY() != b.getY()) {
                return Integer.compare(a.getY(), b.getY());
            }
            if (a.getX() != b.getX()) {
                return Integer.compare(a.getX(), b.getX());
            }
            return Integer.compare(a.getZ(), b.getZ());
        });

        // Queue all positions
        positions.forEach(pos -> queueBlock(pos, state));

        // Place blocks
        return placeBlocks();
    }

    /**
     * Update all affected chunks
     */
    private void updateAffectedChunks() {
        world.getProfiler().push("chunk_updates");

        synchronized (affectedChunks) {
            for (LevelChunk chunk : affectedChunks) {
                chunk.setUnsaved(true);
                // Send chunk update to clients (simplified)
                world.getChunkSource().chunkMap.getPlayers(
                    chunk.getPos(), false
                ).forEach(player -> {
                    // Use alternative method for chunk updates
                    world.getChunkSource().blockChanged(chunk.getPos().getWorldPosition());
                });
            }
            affectedChunks.clear();
        }

        world.getProfiler().pop();
    }

    /**
     * Get chunk key for sorting
     */
    private long getChunkKey(BlockPos pos) {
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }

    /**
     * Clear the placement queue
     */
    public void clearQueue() {
        placementQueue.clear();
        blocksPlaced.set(0);
        affectedChunks.clear();
    }

    /**
     * Get queue size
     */
    public int getQueueSize() {
        return placementQueue.size();
    }

    /**
     * Block placement data
     */
    private static class BlockPlacement {
        private final BlockPos pos;
        private final BlockState state;

        BlockPlacement(BlockPos pos, BlockState state) {
            this.pos = pos;
            this.state = state;
        }

        public BlockPos getPos() {
            return pos;
        }

        public BlockState getState() {
            return state;
        }
    }

    /**
     * Result of batch placement operation
     */
    public static class PlacementResult {
        private final int totalBlocks;
        private final int successCount;
        private final int failCount;
        private final long durationMs;
        private final double blocksPerSecond;

        PlacementResult(int totalBlocks, int successCount, int failCount, long durationMs) {
            this.totalBlocks = totalBlocks;
            this.successCount = successCount;
            this.failCount = failCount;
            this.durationMs = durationMs;
            this.blocksPerSecond = durationMs > 0
                ? (double) successCount / (durationMs / 1000.0) : 0;
        }

        public int getTotalBlocks() {
            return totalBlocks;
        }

        public int getSuccessCount() {
            return successCount;
        }

        public int getFailCount() {
            return failCount;
        }

        public long getDurationMs() {
            return durationMs;
        }

        public double getBlocksPerSecond() {
            return blocksPerSecond;
        }

        public boolean isSuccess() {
            return failCount == 0;
        }

        public double getSuccessRate() {
            return totalBlocks > 0
                ?
                (double) successCount / totalBlocks * 100 : 0;
        }

        @Override
        public String toString() {
            return String.format(
                "PlacementResult{total=%d, success=%d, fail=%d, duration=%dms, rate=%.2f blocks/sec}",
                totalBlocks, successCount, failCount, durationMs, blocksPerSecond
            );
        }
    }
}

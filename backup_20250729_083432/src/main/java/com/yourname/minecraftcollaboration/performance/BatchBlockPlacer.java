package com.yourname.minecraftcollaboration.performance;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import com.yourname.minecraftcollaboration.MinecraftCollaborationMod;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Optimized block placement for large-scale building operations
 * Uses batching and chunk-aware placement to improve performance
 */
public class BatchBlockPlacer {
    private static final Logger LOGGER = MinecraftCollaborationMod.getLogger();
    private static final int BATCH_SIZE = 1000;
    private static final int CHUNK_UPDATE_THRESHOLD = 10000;
    
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
        return CompletableFuture.supplyAsync(() -> {
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
                        world.setBlock(placement.pos, placement.state, 2);
                        
                        // Track affected chunk
                        LevelChunk chunk = world.getChunkAt(placement.pos);
                        affectedChunks.add(chunk);
                        
                        successCount++;
                        blocksPlaced.incrementAndGet();
                    } catch (Exception e) {
                        LOGGER.warn("Failed to place block at {}: {}", placement.pos, e.getMessage());
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
    }
    
    /**
     * Place blocks in a specific pattern with optimization
     */
    public CompletableFuture<PlacementResult> placePattern(
            List<BlockPos> positions, BlockState state) {
        
        // Sort positions by chunk for better cache locality
        positions.sort((a, b) -> {
            int chunkCompare = Long.compare(
                getChunkKey(a), getChunkKey(b)
            );
            if (chunkCompare != 0) return chunkCompare;
            
            // Within same chunk, sort by Y then X then Z
            if (a.getY() != b.getY()) return Integer.compare(a.getY(), b.getY());
            if (a.getX() != b.getX()) return Integer.compare(a.getX(), b.getX());
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
                // Send chunk update to clients
                world.getChunkSource().chunkMap.getPlayers(
                    chunk.getPos(), false
                ).forEach(player -> {
                    player.connection.send(
                        net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket.create(
                            chunk, world.getLightEngine(), null, null
                        )
                    );
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
        final BlockPos pos;
        final BlockState state;
        
        BlockPlacement(BlockPos pos, BlockState state) {
            this.pos = pos;
            this.state = state;
        }
    }
    
    /**
     * Result of batch placement operation
     */
    public static class PlacementResult {
        public final int totalBlocks;
        public final int successCount;
        public final int failCount;
        public final long durationMs;
        public final double blocksPerSecond;
        
        PlacementResult(int totalBlocks, int successCount, int failCount, long durationMs) {
            this.totalBlocks = totalBlocks;
            this.successCount = successCount;
            this.failCount = failCount;
            this.durationMs = durationMs;
            this.blocksPerSecond = durationMs > 0 ? 
                (double) successCount / (durationMs / 1000.0) : 0;
        }
        
        public boolean isSuccess() {
            return failCount == 0;
        }
        
        public double getSuccessRate() {
            return totalBlocks > 0 ? 
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
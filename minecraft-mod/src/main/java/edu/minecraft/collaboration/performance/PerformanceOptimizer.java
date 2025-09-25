package edu.minecraft.collaboration.performance;

import edu.minecraft.collaboration.MinecraftCollaborationMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.List;
import java.util.ArrayList;

/**
 * Performance optimization utilities for bulk operations
 */
public class PerformanceOptimizer {
    private static final Logger LOGGER = MinecraftCollaborationMod.getLogger();
    private static final int BATCH_SIZE = 1000; // Blocks per batch
    private static final int MAX_CONCURRENT_BATCHES = 4;

    private final ExecutorService executorService;
    private final Semaphore batchSemaphore;

    public PerformanceOptimizer() {
        this.executorService = Executors.newFixedThreadPool(MAX_CONCURRENT_BATCHES);
        this.batchSemaphore = new Semaphore(MAX_CONCURRENT_BATCHES);
    }

    /**
     * Optimized bulk block placement
     */
    public CompletableFuture<Integer> placeBulkBlocks(ServerLevel level, List<BlockPlacement> placements) {
        return CompletableFuture.supplyAsync(() -> {
            int totalPlaced = 0;
            List<List<BlockPlacement>> batches = createBatches(placements, BATCH_SIZE);
            List<CompletableFuture<Integer>> futures = new ArrayList<>();

            for (List<BlockPlacement> batch : batches) {
                CompletableFuture<Integer> future = processBatch(level, batch);
                futures.add(future);
            }

            // Wait for all batches to complete
            for (CompletableFuture<Integer> future : futures) {
                try {
                    totalPlaced += future.get();
                } catch (Exception e) {
                    LOGGER.error("Error processing batch", e);
                }
            }

            return totalPlaced;
        }, executorService);
    }

    /**
     * Process a single batch of block placements
     */
    private CompletableFuture<Integer> processBatch(ServerLevel level, List<BlockPlacement> batch) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                batchSemaphore.acquire();

                int placed = 0;

                // Group by chunk for better performance
                batch.sort((a, b) -> {
                    int chunkA = (a.pos.getX() >> 4) + (a.pos.getZ() >> 4) * 1000;
                    int chunkB = (b.pos.getX() >> 4) + (b.pos.getZ() >> 4) * 1000;
                    return Integer.compare(chunkA, chunkB);
                });

                for (BlockPlacement placement : batch) {
                    if (level.setBlockAndUpdate(placement.pos, placement.state)) {
                        placed++;
                    }
                }

                return placed;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return 0;
            } finally {
                batchSemaphore.release();
            }
        }, executorService);
    }

    /**
     * Create batches from a list of items
     */
    private <T> List<List<T>> createBatches(List<T> items, int batchSize) {
        List<List<T>> batches = new ArrayList<>();

        for (int i = 0; i < items.size(); i += batchSize) {
            int end = Math.min(i + batchSize, items.size());
            batches.add(items.subList(i, end));
        }

        return batches;
    }

    /**
     * Optimized circle building with caching
     */
    public List<BlockPos> generateCirclePositions(BlockPos center, int radius, int y) {
        List<BlockPos> positions = new ArrayList<>();

        // Use midpoint circle algorithm for efficiency
        int x = radius;
        int z = 0;
        int err = 0;

        while (x >= z) {
            // Add 8 symmetric points
            positions.add(new BlockPos(center.getX() + x, y, center.getZ() + z));
            positions.add(new BlockPos(center.getX() + z, y, center.getZ() + x));
            positions.add(new BlockPos(center.getX() - z, y, center.getZ() + x));
            positions.add(new BlockPos(center.getX() - x, y, center.getZ() + z));
            positions.add(new BlockPos(center.getX() - x, y, center.getZ() - z));
            positions.add(new BlockPos(center.getX() - z, y, center.getZ() - x));
            positions.add(new BlockPos(center.getX() + z, y, center.getZ() - x));
            positions.add(new BlockPos(center.getX() + x, y, center.getZ() - z));

            if (err <= 0) {
                z += 1;
                err += 2 * z + 1;
            }
            if (err > 0) {
                x -= 1;
                err -= 2 * x + 1;
            }
        }

        return positions;
    }

    /**
     * Optimized sphere generation
     */
    public List<BlockPos> generateSpherePositions(BlockPos center, int radius) {
        List<BlockPos> positions = new ArrayList<>();
        int radiusSquared = radius * radius;

        // Only check blocks within bounding cube
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    // Use squared distance to avoid sqrt calculation
                    int distSquared = x * x + y * y + z * z;

                    // Hollow sphere - only outer shell
                    if (distSquared <= radiusSquared && distSquared >= (radius - 1) * (radius - 1)) {
                        positions.add(center.offset(x, y, z));
                    }
                }
            }
        }

        return positions;
    }

    /**
     * Shutdown the optimizer
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Block placement data
     */
    public static class BlockPlacement {
        private final BlockPos pos;
        private final BlockState state;

        public BlockPlacement(BlockPos pos, BlockState state) {
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
}

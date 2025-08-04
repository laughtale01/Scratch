package edu.minecraft.collaboration.performance;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import edu.minecraft.collaboration.MinecraftCollaborationMod;
import org.slf4j.Logger;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

/**
 * Optimizes building operations for better performance
 */
public class BuildOptimizer {
    private static final Logger LOGGER = MinecraftCollaborationMod.getLogger();
    
    private final ServerLevel world;
    private final BatchBlockPlacer batchPlacer;
    
    public BuildOptimizer(ServerLevel world) {
        this.world = world;
        this.batchPlacer = new BatchBlockPlacer(world);
    }
    
    /**
     * Build a sphere optimized for performance
     */
    public CompletableFuture<BatchBlockPlacer.PlacementResult> buildSphereOptimized(
            BlockPos center, int radius, BlockState blockState, boolean hollow) {
        
        List<BlockPos> positions = new ArrayList<>();
        
        // Pre-calculate all positions
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    double distance = Math.sqrt(x * x + y * y + z * z);
                    
                    if (hollow) {
                        // Only outer shell
                        if (distance >= radius - 1 && distance <= radius) {
                            positions.add(center.offset(x, y, z));
                        }
                    } else {
                        // Filled sphere
                        if (distance <= radius) {
                            positions.add(center.offset(x, y, z));
                        }
                    }
                }
            }
        }
        
        LOGGER.info("Building sphere: center={}, radius={}, blocks={}", 
            center, radius, positions.size());
        
        return batchPlacer.placePattern(positions, blockState);
    }
    
    /**
     * Build a wall optimized for performance
     */
    public CompletableFuture<BatchBlockPlacer.PlacementResult> buildWallOptimized(
            BlockPos start, BlockPos end, int height, BlockState blockState) {
        
        List<BlockPos> positions = new ArrayList<>();
        
        // Find ground level
        int groundY = findGroundLevel(start);
        
        // Calculate wall path using Bresenham's algorithm
        int x1 = start.getX();
        int z1 = start.getZ();
        int x2 = end.getX();
        int z2 = end.getZ();
        
        int dx = Math.abs(x2 - x1);
        int dz = Math.abs(z2 - z1);
        int sx = x1 < x2 ? 1 : -1;
        int sz = z1 < z2 ? 1 : -1;
        int err = dx - dz;
        
        int x = x1;
        int z = z1;
        
        while (true) {
            // Add column of blocks
            for (int y = 0; y < height; y++) {
                positions.add(new BlockPos(x, groundY + y, z));
            }
            
            if (x == x2 && z == z2) {
                break;
            }
            
            int e2 = 2 * err;
            if (e2 > -dz) {
                err -= dz;
                x += sx;
            }
            if (e2 < dx) {
                err += dx;
                z += sz;
            }
        }
        
        LOGGER.info("Building wall: start={}, end={}, height={}, blocks={}", 
            start, end, height, positions.size());
        
        return batchPlacer.placePattern(positions, blockState);
    }
    
    /**
     * Build a circle optimized for performance
     */
    public CompletableFuture<BatchBlockPlacer.PlacementResult> buildCircleOptimized(
            BlockPos center, int radius, BlockState blockState, boolean filled) {
        
        List<BlockPos> positions = new ArrayList<>();
        int y = center.getY();
        
        if (filled) {
            // Filled circle
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + z * z <= radius * radius) {
                        positions.add(center.offset(x, 0, z));
                    }
                }
            }
        } else {
            // Circle outline using midpoint circle algorithm
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
                    z++;
                    err += 2 * z + 1;
                }
                if (err > 0) {
                    x--;
                    err -= 2 * x + 1;
                }
            }
        }
        
        LOGGER.info("Building circle: center={}, radius={}, filled={}, blocks={}", 
            center, radius, filled, positions.size());
        
        return batchPlacer.placePattern(positions, blockState);
    }
    
    /**
     * Fill an area optimized for performance
     */
    public CompletableFuture<BatchBlockPlacer.PlacementResult> fillAreaOptimized(
            BlockPos corner1, BlockPos corner2, BlockState blockState) {
        
        int minX = Math.min(corner1.getX(), corner2.getX());
        int minY = Math.min(corner1.getY(), corner2.getY());
        int minZ = Math.min(corner1.getZ(), corner2.getZ());
        int maxX = Math.max(corner1.getX(), corner2.getX());
        int maxY = Math.max(corner1.getY(), corner2.getY());
        int maxZ = Math.max(corner1.getZ(), corner2.getZ());
        
        // Calculate volume for initial capacity
        int volume = (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        ArrayList<BlockPos> positions = new ArrayList<>(volume);
        
        // Fill in chunk-friendly order (Y -> X -> Z)
        for (int z = minZ; z <= maxZ; z++) {
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    positions.add(new BlockPos(x, y, z));
                }
            }
        }
        
        LOGGER.info("Filling area: corner1={}, corner2={}, blocks={}", 
            corner1, corner2, positions.size());
        
        return batchPlacer.placePattern(positions, blockState);
    }
    
    /**
     * Build a pyramid optimized for performance
     */
    public CompletableFuture<BatchBlockPlacer.PlacementResult> buildPyramidOptimized(
            BlockPos base, int size, BlockState blockState) {
        
        List<BlockPos> positions = new ArrayList<>();
        int y = base.getY();
        
        for (int level = 0; level < size; level++) {
            int levelSize = size - level;
            
            for (int x = -levelSize; x <= levelSize; x++) {
                for (int z = -levelSize; z <= levelSize; z++) {
                    // Only place edge blocks for hollow pyramid
                    if (Math.abs(x) == levelSize || Math.abs(z) == levelSize) {
                        positions.add(base.offset(x, level, z));
                    }
                }
            }
        }
        
        LOGGER.info("Building pyramid: base={}, size={}, blocks={}", 
            base, size, positions.size());
        
        return batchPlacer.placePattern(positions, blockState);
    }
    
    /**
     * Find ground level at a position
     */
    private int findGroundLevel(BlockPos pos) {
        int x = pos.getX();
        int z = pos.getZ();
        
        // Start from build height and go down
        for (int y = world.getMaxBuildHeight() - 1; y >= world.getMinBuildHeight(); y--) {
            BlockPos checkPos = new BlockPos(x, y, z);
            if (!world.getBlockState(checkPos).isAir()) {
                return y + 1;
            }
        }
        
        // Default to sea level if no ground found
        return 64;
    }
    
    /**
     * Clear the batch queue
     */
    public void clearQueue() {
        batchPlacer.clearQueue();
    }
    
    /**
     * Get queue size
     */
    public int getQueueSize() {
        return batchPlacer.getQueueSize();
    }
}
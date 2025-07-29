package com.yourname.minecraftcollaboration.util;

import net.minecraft.core.BlockPos;

/**
 * Utility class for input validation
 * Provides common validation methods for coordinates, ranges, etc.
 */
public class ValidationUtils {
    
    // Maximum allowed values for safety
    public static final int MAX_COORDINATE = 30000000;  // Minecraft world border
    public static final int MIN_COORDINATE = -30000000;
    public static final int MAX_Y_COORDINATE = 320;     // Build height limit
    public static final int MIN_Y_COORDINATE = -64;     // Bedrock level
    public static final int MAX_FILL_VOLUME = 32768;    // Max blocks for fill operation
    public static final int MAX_RADIUS = 100;           // Max radius for circles/spheres
    public static final int MAX_BUILDING_SIZE = 100;    // Max size for buildings
    
    /**
     * Validate a single coordinate value
     * @param coordinate The coordinate to validate
     * @param isY Whether this is a Y coordinate (has different bounds)
     * @return true if valid
     */
    public static boolean isValidCoordinate(int coordinate, boolean isY) {
        if (isY) {
            return coordinate >= MIN_Y_COORDINATE && coordinate <= MAX_Y_COORDINATE;
        } else {
            return coordinate >= MIN_COORDINATE && coordinate <= MAX_COORDINATE;
        }
    }
    
    /**
     * Validate a BlockPos
     * @param pos The position to validate
     * @return true if all coordinates are valid
     */
    public static boolean isValidBlockPos(BlockPos pos) {
        return isValidCoordinate(pos.getX(), false) &&
               isValidCoordinate(pos.getY(), true) &&
               isValidCoordinate(pos.getZ(), false);
    }
    
    /**
     * Validate a fill area
     * @param x1, y1, z1, x2, y2, z2 The coordinates of the fill area
     * @return true if the area is valid and not too large
     */
    public static boolean isValidFillArea(int x1, int y1, int z1, int x2, int y2, int z2) {
        // Check individual coordinates
        if (!isValidCoordinate(x1, false) || !isValidCoordinate(x2, false) ||
            !isValidCoordinate(z1, false) || !isValidCoordinate(z2, false) ||
            !isValidCoordinate(y1, true) || !isValidCoordinate(y2, true)) {
            return false;
        }
        
        // Calculate volume
        int volume = Math.abs(x2 - x1 + 1) * Math.abs(y2 - y1 + 1) * Math.abs(z2 - z1 + 1);
        return volume > 0 && volume <= MAX_FILL_VOLUME;
    }
    
    /**
     * Validate a radius value
     * @param radius The radius to validate
     * @return true if valid
     */
    public static boolean isValidRadius(int radius) {
        return radius > 0 && radius <= MAX_RADIUS;
    }
    
    /**
     * Validate building dimensions
     * @param width The width of the building
     * @param height The height of the building
     * @param depth The depth of the building
     * @return true if all dimensions are valid
     */
    public static boolean isValidBuildingSize(int width, int height, int depth) {
        return width > 0 && width <= MAX_BUILDING_SIZE &&
               height > 0 && height <= MAX_BUILDING_SIZE &&
               depth > 0 && depth <= MAX_BUILDING_SIZE;
    }
    
    /**
     * Parse integer safely
     * @param value The string to parse
     * @param defaultValue The default value if parsing fails
     * @return The parsed integer or default value
     */
    public static int parseIntSafely(String value, int defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * Clamp a value between min and max
     * @param value The value to clamp
     * @param min The minimum value
     * @param max The maximum value
     * @return The clamped value
     */
    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
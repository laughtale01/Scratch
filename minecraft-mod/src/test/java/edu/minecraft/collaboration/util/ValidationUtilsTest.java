package edu.minecraft.collaboration.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import net.minecraft.core.BlockPos;

/**
 * Unit tests for ValidationUtils
 */
public class ValidationUtilsTest {
    
    @Test
    public void testIsValidCoordinate() {
        // Test X/Z coordinates
        assertTrue(ValidationUtils.isValidCoordinate(0, false));
        assertTrue(ValidationUtils.isValidCoordinate(1000, false));
        assertTrue(ValidationUtils.isValidCoordinate(-1000, false));
        assertTrue(ValidationUtils.isValidCoordinate(30000000, false));
        assertTrue(ValidationUtils.isValidCoordinate(-30000000, false));
        
        // Out of bounds X/Z
        assertFalse(ValidationUtils.isValidCoordinate(30000001, false));
        assertFalse(ValidationUtils.isValidCoordinate(-30000001, false));
        
        // Test Y coordinates
        assertTrue(ValidationUtils.isValidCoordinate(0, true));
        assertTrue(ValidationUtils.isValidCoordinate(64, true));
        assertTrue(ValidationUtils.isValidCoordinate(320, true));
        assertTrue(ValidationUtils.isValidCoordinate(-64, true));
        
        // Out of bounds Y
        assertFalse(ValidationUtils.isValidCoordinate(321, true));
        assertFalse(ValidationUtils.isValidCoordinate(-65, true));
    }
    
    @Test
    public void testIsValidBlockPos() {
        // Valid positions
        assertTrue(ValidationUtils.isValidBlockPos(new BlockPos(0, 64, 0)));
        assertTrue(ValidationUtils.isValidBlockPos(new BlockPos(1000, 100, -500)));
        assertTrue(ValidationUtils.isValidBlockPos(new BlockPos(-1000, 0, 1000)));
        
        // Invalid Y coordinate
        assertFalse(ValidationUtils.isValidBlockPos(new BlockPos(0, 321, 0)));
        assertFalse(ValidationUtils.isValidBlockPos(new BlockPos(0, -65, 0)));
        
        // Invalid X/Z coordinates
        assertFalse(ValidationUtils.isValidBlockPos(new BlockPos(30000001, 64, 0)));
        assertFalse(ValidationUtils.isValidBlockPos(new BlockPos(0, 64, -30000001)));
    }
    
    @Test
    public void testIsValidFillArea() {
        // Valid small area
        assertTrue(ValidationUtils.isValidFillArea(0, 64, 0, 10, 74, 10));
        
        // Valid larger area
        assertTrue(ValidationUtils.isValidFillArea(0, 64, 0, 31, 95, 31));
        
        // Maximum allowed volume (32768 blocks)
        // 32x32x32 = 32768
        assertTrue(ValidationUtils.isValidFillArea(0, 64, 0, 31, 95, 31));
        
        // Too large volume
        assertFalse(ValidationUtils.isValidFillArea(0, 64, 0, 50, 114, 50));
        
        // Invalid coordinates
        assertFalse(ValidationUtils.isValidFillArea(0, -65, 0, 10, 74, 10));
        assertFalse(ValidationUtils.isValidFillArea(30000001, 64, 0, 10, 74, 10));
        
        // Zero volume
        assertFalse(ValidationUtils.isValidFillArea(0, 64, 0, 0, 64, 0));
    }
    
    @Test
    public void testIsValidRadius() {
        // Valid radii
        assertTrue(ValidationUtils.isValidRadius(1));
        assertTrue(ValidationUtils.isValidRadius(50));
        assertTrue(ValidationUtils.isValidRadius(100));
        
        // Invalid radii
        assertFalse(ValidationUtils.isValidRadius(0));
        assertFalse(ValidationUtils.isValidRadius(-1));
        assertFalse(ValidationUtils.isValidRadius(101));
    }
    
    @Test
    public void testIsValidBuildingSize() {
        // Valid sizes
        assertTrue(ValidationUtils.isValidBuildingSize(5, 5, 5));
        assertTrue(ValidationUtils.isValidBuildingSize(50, 50, 50));
        assertTrue(ValidationUtils.isValidBuildingSize(100, 100, 100));
        
        // Invalid sizes
        assertFalse(ValidationUtils.isValidBuildingSize(0, 5, 5));
        assertFalse(ValidationUtils.isValidBuildingSize(5, 0, 5));
        assertFalse(ValidationUtils.isValidBuildingSize(5, 5, 0));
        assertFalse(ValidationUtils.isValidBuildingSize(101, 5, 5));
        assertFalse(ValidationUtils.isValidBuildingSize(5, 101, 5));
        assertFalse(ValidationUtils.isValidBuildingSize(5, 5, 101));
        assertFalse(ValidationUtils.isValidBuildingSize(-1, 5, 5));
    }
    
    @Test
    public void testParseIntSafely() {
        // Valid parsing
        assertEquals(42, ValidationUtils.parseIntSafely("42", 0));
        assertEquals(-100, ValidationUtils.parseIntSafely("-100", 0));
        assertEquals(0, ValidationUtils.parseIntSafely("0", 999));
        
        // Whitespace handling
        assertEquals(42, ValidationUtils.parseIntSafely("  42  ", 0));
        assertEquals(42, ValidationUtils.parseIntSafely("\t42\n", 0));
        
        // Invalid parsing - return default
        assertEquals(999, ValidationUtils.parseIntSafely("abc", 999));
        assertEquals(999, ValidationUtils.parseIntSafely("12.5", 999));
        assertEquals(999, ValidationUtils.parseIntSafely("", 999));
        assertEquals(999, ValidationUtils.parseIntSafely(null, 999));
    }
    
    @Test
    public void testClamp() {
        // Within bounds
        assertEquals(50, ValidationUtils.clamp(50, 0, 100));
        assertEquals(0, ValidationUtils.clamp(0, 0, 100));
        assertEquals(100, ValidationUtils.clamp(100, 0, 100));
        
        // Below minimum
        assertEquals(0, ValidationUtils.clamp(-10, 0, 100));
        assertEquals(0, ValidationUtils.clamp(-1, 0, 100));
        
        // Above maximum
        assertEquals(100, ValidationUtils.clamp(101, 0, 100));
        assertEquals(100, ValidationUtils.clamp(200, 0, 100));
        
        // Negative range
        assertEquals(-5, ValidationUtils.clamp(-5, -10, -1));
        assertEquals(-10, ValidationUtils.clamp(-15, -10, -1));
        assertEquals(-1, ValidationUtils.clamp(5, -10, -1));
    }
}
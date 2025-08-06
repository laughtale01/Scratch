package edu.minecraft.collaboration.util;

import edu.minecraft.collaboration.test.categories.UnitTest;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BlockUtils
 */
@DisplayName("BlockUtils Tests")
@UnitTest
public class BlockUtilsTest {
    
    @Test
    @DisplayName("Should get block from valid string")
    void testGetBlockFromValidString() {
        // When
        Block stone = BlockUtils.getBlockFromString("stone");
        Block dirt = BlockUtils.getBlockFromString("dirt");
        Block oakLog = BlockUtils.getBlockFromString("oak_log");
        
        // Then
        assertNotNull(stone);
        assertNotNull(dirt);
        assertNotNull(oakLog);
        assertEquals(Blocks.STONE, stone);
        assertEquals(Blocks.DIRT, dirt);
        assertEquals(Blocks.OAK_LOG, oakLog);
    }
    
    @Test
    @DisplayName("Should get block from namespaced string")
    void testGetBlockFromNamespacedString() {
        // When
        Block stone = BlockUtils.getBlockFromString("minecraft:stone");
        Block dirt = BlockUtils.getBlockFromString("minecraft:dirt");
        
        // Then
        assertNotNull(stone);
        assertNotNull(dirt);
        assertEquals(Blocks.STONE, stone);
        assertEquals(Blocks.DIRT, dirt);
    }
    
    @Test
    @DisplayName("Should return null for invalid block string")
    void testGetBlockFromInvalidString() {
        // When
        Block invalid = BlockUtils.getBlockFromString("invalid_block_name");
        Block nullBlock = BlockUtils.getBlockFromString(null);
        Block emptyBlock = BlockUtils.getBlockFromString("");
        
        // Then
        assertNull(invalid);
        assertNull(nullBlock);
        assertNull(emptyBlock);
    }
    
    @Test
    @DisplayName("Should handle case insensitive block names")
    void testCaseInsensitiveBlockNames() {
        // When
        Block upperCase = BlockUtils.getBlockFromString("STONE");
        Block mixedCase = BlockUtils.getBlockFromString("StOnE");
        Block normalCase = BlockUtils.getBlockFromString("stone");
        
        // Then
        assertEquals(normalCase, upperCase);
        assertEquals(normalCase, mixedCase);
    }
    
    @Test
    @DisplayName("Should get string from block")
    void testGetStringFromBlock() {
        // When
        String stoneName = BlockUtils.getStringFromBlock(Blocks.STONE);
        String dirtName = BlockUtils.getStringFromBlock(Blocks.DIRT);
        String oakLogName = BlockUtils.getStringFromBlock(Blocks.OAK_LOG);
        
        // Then
        assertNotNull(stoneName);
        assertNotNull(dirtName);
        assertNotNull(oakLogName);
        assertTrue(stoneName.contains("stone"));
        assertTrue(dirtName.contains("dirt"));
        assertTrue(oakLogName.contains("oak_log"));
    }
    
    @Test
    @DisplayName("Should handle null block in string conversion")
    void testGetStringFromNullBlock() {
        // When
        String result = BlockUtils.getStringFromBlock(null);
        
        // Then
        assertNull(result);
    }
    
    @Test
    @DisplayName("Should validate block names")
    void testIsValidBlockName() {
        // When/Then
        assertTrue(BlockUtils.isValidBlockName("stone"));
        assertTrue(BlockUtils.isValidBlockName("minecraft:stone"));
        assertTrue(BlockUtils.isValidBlockName("oak_log"));
        assertTrue(BlockUtils.isValidBlockName("grass_block"));
        
        assertFalse(BlockUtils.isValidBlockName("invalid_block"));
        assertFalse(BlockUtils.isValidBlockName(null));
        assertFalse(BlockUtils.isValidBlockName(""));
        assertFalse(BlockUtils.isValidBlockName("   "));
    }
    
    @Test
    @DisplayName("Should check if block is dangerous")
    void testIsDangerousBlock() {
        // When/Then
        assertTrue(BlockUtils.isDangerousBlock("tnt"));
        assertTrue(BlockUtils.isDangerousBlock("minecraft:tnt"));
        assertTrue(BlockUtils.isDangerousBlock("lava"));
        assertTrue(BlockUtils.isDangerousBlock("fire"));
        
        assertFalse(BlockUtils.isDangerousBlock("stone"));
        assertFalse(BlockUtils.isDangerousBlock("dirt"));
        assertFalse(BlockUtils.isDangerousBlock("oak_log"));
        assertFalse(BlockUtils.isDangerousBlock(null));
    }
    
    @Test
    @DisplayName("Should get block hardness")
    void testGetBlockHardness() {
        // When
        float stoneHardness = BlockUtils.getBlockHardness("stone");
        float dirtHardness = BlockUtils.getBlockHardness("dirt");
        float bedrockHardness = BlockUtils.getBlockHardness("bedrock");
        
        // Then
        assertTrue(stoneHardness > 0);
        assertTrue(dirtHardness >= 0);
        assertTrue(bedrockHardness > stoneHardness); // Bedrock is harder than stone
    }
    
    @Test
    @DisplayName("Should handle invalid block hardness")
    void testGetInvalidBlockHardness() {
        // When
        float invalidHardness = BlockUtils.getBlockHardness("invalid_block");
        float nullHardness = BlockUtils.getBlockHardness(null);
        
        // Then
        assertEquals(-1.0f, invalidHardness);
        assertEquals(-1.0f, nullHardness);
    }
    
    @Test
    @DisplayName("Should check if block is solid")
    void testIsBlockSolid() {
        // When/Then
        assertTrue(BlockUtils.isBlockSolid("stone"));
        assertTrue(BlockUtils.isBlockSolid("dirt"));
        assertTrue(BlockUtils.isBlockSolid("oak_log"));
        
        assertFalse(BlockUtils.isBlockSolid("air"));
        assertFalse(BlockUtils.isBlockSolid("water"));
        assertFalse(BlockUtils.isBlockSolid("invalid_block"));
        assertFalse(BlockUtils.isBlockSolid(null));
    }
    
    @Test
    @DisplayName("Should get block light level")
    void testGetBlockLightLevel() {
        // When
        int torchLight = BlockUtils.getBlockLightLevel("torch");
        int glowstoneLight = BlockUtils.getBlockLightLevel("glowstone");
        int stoneLight = BlockUtils.getBlockLightLevel("stone");
        
        // Then
        assertTrue(torchLight > 0);
        assertTrue(glowstoneLight > torchLight);
        assertEquals(0, stoneLight);
    }
    
    @Test
    @DisplayName("Should handle common building blocks")
    void testCommonBuildingBlocks() {
        // Given
        String[] commonBlocks = {
            "stone", "dirt", "cobblestone", "oak_planks", "oak_log",
            "glass", "brick", "iron_block", "gold_block", "diamond_block",
            "grass_block", "sand", "gravel", "clay"
        };
        
        // When/Then
        for (String blockName : commonBlocks) {
            Block block = BlockUtils.getBlockFromString(blockName);
            assertNotNull(block, "Should find common block: " + blockName);
            assertTrue(BlockUtils.isValidBlockName(blockName), "Should validate common block: " + blockName);
        }
    }
    
    @Test
    @DisplayName("Should round trip block conversion")
    void testRoundTripConversion() {
        // Given
        String[] blockNames = {"stone", "dirt", "oak_log", "glass", "iron_block"};
        
        // When/Then
        for (String originalName : blockNames) {
            Block block = BlockUtils.getBlockFromString(originalName);
            assertNotNull(block, "Block should be found: " + originalName);
            
            String convertedName = BlockUtils.getStringFromBlock(block);
            assertNotNull(convertedName, "Block should convert back to string: " + originalName);
            
            Block reconvertedBlock = BlockUtils.getBlockFromString(convertedName);
            assertEquals(block, reconvertedBlock, "Round trip conversion should work: " + originalName);
        }
    }
}
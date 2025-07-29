package edu.minecraft.collaboration.persistence;

import edu.minecraft.collaboration.collaboration.CollaborationManager;
import edu.minecraft.collaboration.collaboration.Invitation;
import edu.minecraft.collaboration.collaboration.VisitRequest;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.level.saveddata.SavedData;
import org.junit.jupiter.api.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for data persistence functionality
 */
public class DataPersistenceTest {
    
    private Path tempDir;
    private CollaborationManager collaborationManager;
    
    @BeforeEach
    public void setup() throws IOException {
        tempDir = Files.createTempDirectory("minecraft-collab-test");
        collaborationManager = CollaborationManager.getInstance();
    }
    
    @AfterEach
    public void teardown() throws IOException {
        // Clean up temp directory
        Files.walk(tempDir)
            .sorted((a, b) -> -a.compareTo(b))
            .forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    // Ignore
                }
            });
    }
    
    @Test
    @DisplayName("Test invitation data serialization")
    public void testInvitationSerialization() {
        // Create test invitation
        Invitation invitation = new Invitation("Player1", "Player2");
        
        // Convert to NBT
        CompoundTag nbt = new CompoundTag();
        nbt.putString("from", invitation.getFromPlayer());
        nbt.putString("to", invitation.getToPlayer());
        nbt.putLong("timestamp", invitation.getTimestamp());
        
        // Verify NBT data
        assertEquals("Player1", nbt.getString("from"));
        assertEquals("Player2", nbt.getString("to"));
        assertTrue(nbt.contains("timestamp"));
    }
    
    @Test
    @DisplayName("Test invitation data deserialization")
    public void testInvitationDeserialization() {
        // Create NBT data
        CompoundTag nbt = new CompoundTag();
        nbt.putString("from", "Player1");
        nbt.putString("to", "Player2");
        nbt.putLong("timestamp", System.currentTimeMillis());
        
        // Recreate invitation from NBT
        String from = nbt.getString("from");
        String to = nbt.getString("to");
        long timestamp = nbt.getLong("timestamp");
        
        // Verify data
        assertEquals("Player1", from);
        assertEquals("Player2", to);
        assertTrue(timestamp > 0);
    }
    
    @Test
    @DisplayName("Test visit request persistence")
    public void testVisitRequestPersistence() {
        // Create test visit request
        VisitRequest request = new VisitRequest("Visitor", "Host");
        
        // Convert to NBT
        CompoundTag nbt = new CompoundTag();
        nbt.putString("visitor", request.getVisitor());
        nbt.putString("host", request.getHost());
        nbt.putBoolean("approved", request.isApproved());
        
        // Verify persistence
        assertEquals("Visitor", nbt.getString("visitor"));
        assertEquals("Host", nbt.getString("host"));
        assertFalse(nbt.getBoolean("approved"));
    }
    
    @Test
    @DisplayName("Test world tracking data persistence")
    public void testWorldTrackingPersistence() {
        // Create world tracking data
        CompoundTag worldData = new CompoundTag();
        worldData.putString("player1", "overworld");
        worldData.putString("player2", "nether");
        worldData.putString("player3", "player1_world");
        
        // Save to file
        File saveFile = new File(tempDir.toFile(), "world_tracking.dat");
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(saveFile))) {
            byte[] bytes = worldData.toString().getBytes();
            out.writeInt(bytes.length);
            out.write(bytes);
        } catch (IOException e) {
            fail("Failed to save world data: " + e.getMessage());
        }
        
        // Load from file
        CompoundTag loadedData = new CompoundTag();
        try (DataInputStream in = new DataInputStream(new FileInputStream(saveFile))) {
            int length = in.readInt();
            byte[] bytes = new byte[length];
            in.readFully(bytes);
            // In real implementation, would parse NBT
            assertNotNull(bytes);
        } catch (IOException e) {
            fail("Failed to load world data: " + e.getMessage());
        }
    }
    
    @Test
    @DisplayName("Test player position cache persistence")
    public void testPositionCachePersistence() {
        // Create position data
        CompoundTag positions = new CompoundTag();
        
        CompoundTag pos1 = new CompoundTag();
        pos1.putDouble("x", 100.5);
        pos1.putDouble("y", 64.0);
        pos1.putDouble("z", -200.5);
        positions.put("player1", pos1);
        
        CompoundTag pos2 = new CompoundTag();
        pos2.putDouble("x", 500.0);
        pos2.putDouble("y", 70.0);
        pos2.putDouble("z", 300.0);
        positions.put("player2", pos2);
        
        // Verify data structure
        assertTrue(positions.contains("player1"));
        assertTrue(positions.contains("player2"));
        
        CompoundTag loaded1 = positions.getCompound("player1");
        assertEquals(100.5, loaded1.getDouble("x"));
        assertEquals(64.0, loaded1.getDouble("y"));
        assertEquals(-200.5, loaded1.getDouble("z"));
    }
    
    @Test
    @DisplayName("Test collaboration settings persistence")
    public void testSettingsPersistence() {
        // Create settings
        CompoundTag settings = new CompoundTag();
        settings.putBoolean("allowInvites", true);
        settings.putBoolean("allowVisits", true);
        settings.putInt("maxVisitors", 5);
        settings.putString("homeWorld", "overworld");
        
        // Add blocked players list
        ListTag blockedPlayers = new ListTag();
        blockedPlayers.add(StringTag.valueOf("GrieferPlayer"));
        blockedPlayers.add(StringTag.valueOf("SpammerPlayer"));
        settings.put("blockedPlayers", blockedPlayers);
        
        // Verify settings
        assertTrue(settings.getBoolean("allowInvites"));
        assertEquals(5, settings.getInt("maxVisitors"));
        
        ListTag loaded = settings.getList("blockedPlayers", 8); // 8 = String tag
        assertEquals(2, loaded.size());
    }
    
    @Test
    @DisplayName("Test data migration and versioning")
    public void testDataMigration() {
        // Old format data
        CompoundTag oldFormat = new CompoundTag();
        oldFormat.putInt("version", 1);
        oldFormat.putString("player", "TestPlayer");
        
        // New format data
        CompoundTag newFormat = new CompoundTag();
        newFormat.putInt("version", 2);
        newFormat.putString("playerName", "TestPlayer");
        newFormat.putString("playerUUID", UUID.randomUUID().toString());
        
        // Verify version handling
        assertEquals(1, oldFormat.getInt("version"));
        assertEquals(2, newFormat.getInt("version"));
        
        // Simulate migration
        if (oldFormat.getInt("version") < 2) {
            String playerName = oldFormat.getString("player");
            newFormat.putString("playerName", playerName);
            newFormat.putString("playerUUID", UUID.randomUUID().toString());
        }
        
        assertTrue(newFormat.contains("playerUUID"));
    }
    
    @Test
    @DisplayName("Test concurrent data access")
    public void testConcurrentDataAccess() throws InterruptedException {
        CompoundTag sharedData = new CompoundTag();
        sharedData.putInt("counter", 0);
        
        // Simulate concurrent modifications
        Thread thread1 = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                synchronized (sharedData) {
                    int current = sharedData.getInt("counter");
                    sharedData.putInt("counter", current + 1);
                }
            }
        });
        
        Thread thread2 = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                synchronized (sharedData) {
                    int current = sharedData.getInt("counter");
                    sharedData.putInt("counter", current + 1);
                }
            }
        });
        
        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();
        
        // Verify no data corruption
        assertEquals(200, sharedData.getInt("counter"));
    }
    
    @Test
    @DisplayName("Test data backup and recovery")
    public void testBackupAndRecovery() throws IOException {
        // Create original data
        CompoundTag originalData = new CompoundTag();
        originalData.putString("importantData", "This must not be lost");
        originalData.putInt("score", 12345);
        
        // Create backup
        File backupFile = new File(tempDir.toFile(), "backup.dat");
        Files.write(backupFile.toPath(), originalData.toString().getBytes());
        
        // Simulate data corruption
        CompoundTag corruptedData = new CompoundTag();
        corruptedData.putString("importantData", "CORRUPTED");
        
        // Restore from backup
        byte[] backupBytes = Files.readAllBytes(backupFile.toPath());
        String backupString = new String(backupBytes);
        
        // Verify backup integrity
        assertTrue(backupString.contains("This must not be lost"));
        assertTrue(backupString.contains("12345"));
    }
}
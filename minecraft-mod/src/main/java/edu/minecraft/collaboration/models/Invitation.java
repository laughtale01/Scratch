package edu.minecraft.collaboration.models;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents an invitation from one player to another
 */
public class Invitation {
    private final UUID id;
    private final UUID senderId;
    private final UUID recipientId;
    private final String senderName;
    private final String recipientName;
    private final Instant timestamp;
    private final int expiryMinutes;
    private InvitationStatus status;
    
    public enum InvitationStatus {
        PENDING,
        ACCEPTED,
        DECLINED,
        EXPIRED
    }
    
    public Invitation(String senderName, String recipientName) {
        this.id = UUID.randomUUID();
        this.senderId = UUID.randomUUID();
        this.recipientId = UUID.randomUUID();
        this.senderName = senderName;
        this.recipientName = recipientName;
        this.timestamp = Instant.now();
        this.expiryMinutes = 5;
        this.status = InvitationStatus.PENDING;
    }
    
    /**
     * Constructor with all parameters for test compatibility
     */
    public Invitation(UUID id, UUID senderId, UUID recipientId, String senderName, String recipientName, int expiryMinutes) {
        this.id = id;
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.senderName = senderName;
        this.recipientName = recipientName;
        this.timestamp = Instant.now();
        this.expiryMinutes = expiryMinutes;
        this.status = InvitationStatus.PENDING;
    }
    
    // Getters
    public UUID getId() {
        return id;
    }
    
    public UUID getSenderId() {
        return senderId;
    }
    
    public UUID getRecipientId() {
        return recipientId;
    }
    
    public String getSenderName() {
        return senderName;
    }
    
    public String getRecipientName() {
        return recipientName;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public InvitationStatus getStatus() {
        return status;
    }
    
    // Setters
    public void setStatus(InvitationStatus status) {
        this.status = status;
    }
    
    // Check if invitation is expired
    public boolean isExpired() {
        return Instant.now().isAfter(timestamp.plusSeconds(expiryMinutes * 60));
    }
}
package com.yourname.minecraftcollaboration.models;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents an invitation from one player to another
 */
public class Invitation {
    private final UUID id;
    private final String senderName;
    private final String recipientName;
    private final Instant timestamp;
    private InvitationStatus status;
    
    public enum InvitationStatus {
        PENDING,
        ACCEPTED,
        DECLINED,
        EXPIRED
    }
    
    public Invitation(String senderName, String recipientName) {
        this.id = UUID.randomUUID();
        this.senderName = senderName;
        this.recipientName = recipientName;
        this.timestamp = Instant.now();
        this.status = InvitationStatus.PENDING;
    }
    
    // Getters
    public UUID getId() {
        return id;
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
    
    // Check if invitation is expired (older than 5 minutes)
    public boolean isExpired() {
        return Instant.now().isAfter(timestamp.plusSeconds(300));
    }
}
package edu.minecraft.collaboration.models;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a visit request from one player to visit another player's world
 */
public class VisitRequest {
    private final UUID id;
    private final String requesterName;
    private final String hostName;
    private final Instant timestamp;
    private VisitStatus status;
    
    public enum VisitStatus {
        PENDING,
        APPROVED,
        DENIED,
        EXPIRED,
        IN_PROGRESS,
        COMPLETED
    }
    
    public VisitRequest(String requesterName, String hostName) {
        this.id = UUID.randomUUID();
        this.requesterName = requesterName;
        this.hostName = hostName;
        this.timestamp = Instant.now();
        this.status = VisitStatus.PENDING;
    }
    
    // Getters
    public UUID getId() {
        return id;
    }
    
    public String getRequesterName() {
        return requesterName;
    }
    
    public String getHostName() {
        return hostName;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public VisitStatus getStatus() {
        return status;
    }
    
    // Setters
    public void setStatus(VisitStatus status) {
        this.status = status;
    }
    
    // Check if request is expired (older than 2 minutes)
    public boolean isExpired() {
        return Instant.now().isAfter(timestamp.plusSeconds(120));
    }
}
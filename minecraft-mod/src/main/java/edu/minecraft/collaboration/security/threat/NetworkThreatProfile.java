package edu.minecraft.collaboration.security.threat;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Network threat profile for IP-based threat analysis
 */
public class NetworkThreatProfile {
    
    private final String ipAddress;
    private final AtomicInteger threatScore = new AtomicInteger(0);
    private final AtomicLong connectionCount = new AtomicLong(0);
    private final ConcurrentLinkedQueue<Instant> connectionTimes = new ConcurrentLinkedQueue<>();
    
    private volatile boolean knownMalicious = false;
    private volatile boolean isVpnOrProxy = false;
    private volatile boolean isHighRiskCountry = false;
    private volatile Instant firstSeen;
    private volatile Instant lastSeen;
    private volatile String country = "unknown";
    
    public NetworkThreatProfile(String ipAddress) {
        this.ipAddress = ipAddress;
        this.firstSeen = Instant.now();
        this.lastSeen = Instant.now();
        
        // Initialize threat analysis
        analyzeIpAddress();
    }
    
    /**
     * Record a connection from this IP
     */
    public void recordConnection() {
        connectionCount.incrementAndGet();
        connectionTimes.offer(Instant.now());
        lastSeen = Instant.now();
        
        // Clean up old connection times
        cleanupOldConnections();
        
        // Update threat score based on connection patterns
        updateThreatScore();
    }
    
    /**
     * Analyze IP address for threat indicators
     */
    private void analyzeIpAddress() {
        // Basic IP analysis (in a real implementation, this would query threat intelligence APIs)
        
        // Check if IP is from a local network
        if (isLocalNetwork(ipAddress)) {
            threatScore.set(5); // Local network - low threat
            return;
        }
        
        // Check for known patterns
        if (ipAddress.startsWith("192.168.") || ipAddress.startsWith("10.") || ipAddress.equals("127.0.0.1")) {
            threatScore.set(0); // Private network
        } else {
            threatScore.set(15); // External network - baseline threat
            
            // Additional checks would go here in a real implementation:
            // - Query threat intelligence feeds
            // - Check IP reputation databases
            // - Analyze geographic location
            // - Check for VPN/proxy indicators
        }
    }
    
    private void updateThreatScore() {
        // Increase threat score based on connection frequency
        long recentConnections = connectionTimes.stream()
            .filter(time -> time.isAfter(Instant.now().minus(Duration.ofMinutes(10))))
            .count();
        
        if (recentConnections > 50) {
            threatScore.addAndGet(30); // Very high frequency
        } else if (recentConnections > 20) {
            threatScore.addAndGet(15); // High frequency
        } else if (recentConnections > 10) {
            threatScore.addAndGet(5); // Moderate frequency
        }
        
        // Cap the threat score
        if (threatScore.get() > 100) {
            threatScore.set(100);
        }
    }
    
    private void cleanupOldConnections() {
        Instant cutoff = Instant.now().minus(Duration.ofHours(1));
        connectionTimes.removeIf(time -> time.isBefore(cutoff));
    }
    
    /**
     * Clean up old data
     */
    public void cleanupOldData() {
        cleanupOldConnections();
        
        // Reset threat score if no recent activity
        if (lastSeen.isBefore(Instant.now().minus(Duration.ofHours(24)))) {
            threatScore.set(Math.max(5, threatScore.get() - 10));
        }
    }
    
    /**
     * Check if the connection pattern is anomalous
     */
    public boolean hasAnomalousConnectionPattern() {
        // Check for burst patterns
        long connectionsInLastMinute = connectionTimes.stream()
            .filter(time -> time.isAfter(Instant.now().minus(Duration.ofMinutes(1))))
            .count();
        
        return connectionsInLastMinute > 20; // More than 20 connections per minute is anomalous
    }
    
    private boolean isLocalNetwork(String ip) {
        return ip.startsWith("192.168.") || 
               ip.startsWith("10.") || 
               ip.startsWith("172.16.") ||
               ip.equals("127.0.0.1");
    }
    
    // Getters
    public String getIpAddress() { return ipAddress; }
    public int getThreatScore() { return threatScore.get(); }
    public boolean isKnownMalicious() { return knownMalicious; }
    public boolean isVpnOrProxy() { return isVpnOrProxy; }
    public boolean isFromHighRiskCountry() { return isHighRiskCountry; }
    public long getConnectionCount() { return connectionCount.get(); }
    public Instant getFirstSeen() { return firstSeen; }
    public Instant getLastSeen() { return lastSeen; }
    public String getCountry() { return country; }
    
    // Setters for threat intelligence updates
    public void setKnownMalicious(boolean knownMalicious) {
        this.knownMalicious = knownMalicious;
        if (knownMalicious) {
            threatScore.set(100);
        }
    }
    
    public void setVpnOrProxy(boolean isVpnOrProxy) {
        this.isVpnOrProxy = isVpnOrProxy;
        if (isVpnOrProxy) {
            threatScore.addAndGet(20);
        }
    }
    
    public void setHighRiskCountry(boolean isHighRiskCountry) {
        this.isHighRiskCountry = isHighRiskCountry;
        if (isHighRiskCountry) {
            threatScore.addAndGet(25);
        }
    }
    
    public void setCountry(String country) {
        this.country = country;
    }
}
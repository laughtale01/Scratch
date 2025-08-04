package edu.minecraft.collaboration.monitoring.apm;

/**
 * Profile context for tracking method execution performance
 */
public interface ProfileContext extends AutoCloseable {
    
    /**
     * Add an attribute to the profile context
     */
    void addAttribute(String key, Object value);
    
    /**
     * Set an error for the profile context
     */
    void setError(Exception error);
    
    /**
     * Close the profile context and record the performance data
     */
    @Override
    void close();
}
package edu.minecraft.collaboration.monitoring.apm;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a single profiling event
 */
public class ProfileEvent {

    private final String methodName;
    private final Duration duration;
    private final Map<String, Object> attributes;
    private final Exception error;
    private final Instant timestamp;

    public ProfileEvent(String methodName, Duration duration, Map<String, Object> attributes, Exception error) {
        this.methodName = methodName;
        this.duration = duration;
        this.attributes = attributes != null ? new HashMap<>(attributes) : new HashMap<>();
        this.error = error;
        this.timestamp = Instant.now();
    }

    public String getMethodName() { return methodName; }
    public Duration getDuration() { return duration; }
    public Map<String, Object> getAttributes() { return attributes; }
    public Exception getError() { return error; }
    public Instant getTimestamp() { return timestamp; }

    /**
     * Check if this event represents a successful execution
     */
    public boolean isSuccessful() {
        return error == null;
    }

    /**
     * Get duration in milliseconds
     */
    public double getDurationMs() {
        return duration.toNanos() / 1_000_000.0;
    }

    /**
     * Check if this event is considered slow
     */
    public boolean isSlow() {
        return getDurationMs() > 100; // > 100ms
    }

    @Override
    public String toString() {
        return String.format(
            "ProfileEvent{method='%s', duration=%.2fms, success=%s, timestamp=%s}",
            methodName, getDurationMs(), isSuccessful(), timestamp
        );
    }
}

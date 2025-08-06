package edu.minecraft.collaboration.monitoring.alerts;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Result of prediction analysis
 */
public class PredictionResult {
    
    private final Duration predictionWindow;
    private final double predictedCpuAvg;
    private final double predictedCpuMax;
    private final double predictedMemoryAvg;
    private final double predictedMemoryMax;
    private final double predictedResponseTimeAvg;
    private final double predictedResponseTimeMax;
    private final double predictedErrorRateAvg;
    private final double predictedErrorRateMax;
    private final double confidence;
    private final String cpuTrend;
    private final String memoryTrend;
    private final List<AnomalyPrediction> anomalies;
    private final Instant generatedAt;
    private final boolean isError;
    private final String errorMessage;
    
    private PredictionResult(Builder builder) {
        this.predictionWindow = builder.predictionWindow;
        this.predictedCpuAvg = builder.predictedCpuAvg;
        this.predictedCpuMax = builder.predictedCpuMax;
        this.predictedMemoryAvg = builder.predictedMemoryAvg;
        this.predictedMemoryMax = builder.predictedMemoryMax;
        this.predictedResponseTimeAvg = builder.predictedResponseTimeAvg;
        this.predictedResponseTimeMax = builder.predictedResponseTimeMax;
        this.predictedErrorRateAvg = builder.predictedErrorRateAvg;
        this.predictedErrorRateMax = builder.predictedErrorRateMax;
        this.confidence = builder.confidence;
        this.cpuTrend = builder.cpuTrend;
        this.memoryTrend = builder.memoryTrend;
        this.anomalies = new ArrayList<>(builder.anomalies);
        this.generatedAt = Instant.now();
        this.isError = builder.isError;
        this.errorMessage = builder.errorMessage;
    }
    
    public Duration getPredictionWindow() { return predictionWindow; }
    public double getPredictedCpuAvg() { return predictedCpuAvg; }
    public double getPredictedCpuMax() { return predictedCpuMax; }
    public double getPredictedMemoryAvg() { return predictedMemoryAvg; }
    public double getPredictedMemoryMax() { return predictedMemoryMax; }
    public double getPredictedResponseTimeAvg() { return predictedResponseTimeAvg; }
    public double getPredictedResponseTimeMax() { return predictedResponseTimeMax; }
    public double getPredictedErrorRateAvg() { return predictedErrorRateAvg; }
    public double getPredictedErrorRateMax() { return predictedErrorRateMax; }
    public double getConfidence() { return confidence; }
    public String getCpuTrend() { return cpuTrend; }
    public String getMemoryTrend() { return memoryTrend; }
    public List<AnomalyPrediction> getAnomalies() { return anomalies; }
    public Instant getGeneratedAt() { return generatedAt; }
    public boolean isError() { return isError; }
    public String getErrorMessage() { return errorMessage; }
    
    /**
     * Check if any critical thresholds are predicted to be exceeded
     */
    public boolean hasCriticalPredictions() {
        return predictedCpuMax > 90 || predictedMemoryMax > 90 || 
               predictedResponseTimeMax > 2000 || predictedErrorRateMax > 10;
    }
    
    /**
     * Check if any warning thresholds are predicted to be exceeded
     */
    public boolean hasWarningPredictions() {
        return predictedCpuMax > 70 || predictedMemoryMax > 70 || 
               predictedResponseTimeMax > 1000 || predictedErrorRateMax > 5;
    }
    
    /**
     * Get the overall prediction risk level
     */
    public String getRiskLevel() {
        if (hasCriticalPredictions()) {
            return "CRITICAL";
        } else if (hasWarningPredictions()) {
            return "WARNING";
        } else {
            return "NORMAL";
        }
    }
    
    /**
     * Get a summary of the predictions
     */
    public String getSummary() {
        if (isError) {
            return "Prediction Error: " + errorMessage;
        }
        
        return String.format(
            "Predictions (%.0f min): CPU=%.1f%% (max %.1f%%), Memory=%.1f%% (max %.1f%%), " +
            "Response=%.1fms (max %.1fms), Errors=%.2f%% (max %.2f%%), Risk=%s, Confidence=%.0f%%",
            predictionWindow.toMinutes(),
            predictedCpuAvg, predictedCpuMax,
            predictedMemoryAvg, predictedMemoryMax,
            predictedResponseTimeAvg, predictedResponseTimeMax,
            predictedErrorRateAvg, predictedErrorRateMax,
            getRiskLevel(),
            confidence * 100
        );
    }
    
    public static PredictionResult insufficientData(String message) {
        return builder()
            .isError(true)
            .errorMessage(message)
            .build();
    }
    
    public static PredictionResult error(String message) {
        return builder()
            .isError(true)
            .errorMessage(message)
            .build();
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    @Override
    public String toString() {
        return getSummary();
    }
    
    public static class Builder {
        private Duration predictionWindow = Duration.ofMinutes(30);
        private double predictedCpuAvg = 0.0;
        private double predictedCpuMax = 0.0;
        private double predictedMemoryAvg = 0.0;
        private double predictedMemoryMax = 0.0;
        private double predictedResponseTimeAvg = 0.0;
        private double predictedResponseTimeMax = 0.0;
        private double predictedErrorRateAvg = 0.0;
        private double predictedErrorRateMax = 0.0;
        private double confidence = 0.0;
        private String cpuTrend = "STABLE";
        private String memoryTrend = "STABLE";
        private List<AnomalyPrediction> anomalies = new ArrayList<>();
        private boolean isError = false;
        private String errorMessage = "";
        
        public Builder predictionWindow(Duration predictionWindow) {
            this.predictionWindow = predictionWindow;
            return this;
        }
        
        public Builder predictedCpuAvg(double predictedCpuAvg) {
            this.predictedCpuAvg = predictedCpuAvg;
            return this;
        }
        
        public Builder predictedCpuMax(double predictedCpuMax) {
            this.predictedCpuMax = predictedCpuMax;
            return this;
        }
        
        public Builder predictedMemoryAvg(double predictedMemoryAvg) {
            this.predictedMemoryAvg = predictedMemoryAvg;
            return this;
        }
        
        public Builder predictedMemoryMax(double predictedMemoryMax) {
            this.predictedMemoryMax = predictedMemoryMax;
            return this;
        }
        
        public Builder predictedResponseTimeAvg(double predictedResponseTimeAvg) {
            this.predictedResponseTimeAvg = predictedResponseTimeAvg;
            return this;
        }
        
        public Builder predictedResponseTimeMax(double predictedResponseTimeMax) {
            this.predictedResponseTimeMax = predictedResponseTimeMax;
            return this;
        }
        
        public Builder predictedErrorRateAvg(double predictedErrorRateAvg) {
            this.predictedErrorRateAvg = predictedErrorRateAvg;
            return this;
        }
        
        public Builder predictedErrorRateMax(double predictedErrorRateMax) {
            this.predictedErrorRateMax = predictedErrorRateMax;
            return this;
        }
        
        public Builder confidence(double confidence) {
            this.confidence = confidence;
            return this;
        }
        
        public Builder cpuTrend(String cpuTrend) {
            this.cpuTrend = cpuTrend;
            return this;
        }
        
        public Builder memoryTrend(String memoryTrend) {
            this.memoryTrend = memoryTrend;
            return this;
        }
        
        public Builder anomalies(List<AnomalyPrediction> anomalies) {
            this.anomalies = new ArrayList<>(anomalies);
            return this;
        }
        
        public Builder isError(boolean isError) {
            this.isError = isError;
            return this;
        }
        
        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }
        
        public PredictionResult build() {
            return new PredictionResult(this);
        }
    }
}

/**
 * Represents a predicted anomaly
 */
class AnomalyPrediction {
    
    private final String type;
    private final String description;
    private final double confidence;
    private final Duration estimatedTimeToOccurrence;
    
    public AnomalyPrediction(String type, String description, double confidence, Duration estimatedTimeToOccurrence) {
        this.type = type;
        this.description = description;
        this.confidence = confidence;
        this.estimatedTimeToOccurrence = estimatedTimeToOccurrence;
    }
    
    public String getType() { return type; }
    public String getDescription() { return description; }
    public double getConfidence() { return confidence; }
    public Duration getEstimatedTimeToOccurrence() { return estimatedTimeToOccurrence; }
    
    @Override
    public String toString() {
        return String.format("%s: %s (confidence: %.0f%%, ETA: %d min)",
            type, description, confidence * 100, estimatedTimeToOccurrence.toMinutes());
    }
}
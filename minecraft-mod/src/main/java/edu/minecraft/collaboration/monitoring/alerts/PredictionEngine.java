package edu.minecraft.collaboration.monitoring.alerts;

import edu.minecraft.collaboration.MinecraftCollaborationMod;
import org.slf4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.DoubleSummaryStatistics;
import java.util.List;

/**
 * Prediction Engine using statistical analysis and trend detection
 * Implements simple machine learning-inspired algorithms for system prediction
 */
public class PredictionEngine {

    private static final Logger LOGGER = MinecraftCollaborationMod.getLogger();

    private volatile PredictionResult latestPredictions;

    /**
     * Generate predictions based on historical health data
     */
    public PredictionResult generatePredictions(Collection<HealthDataPoint> healthHistory, Duration predictionWindow) {
        try {
            if (healthHistory.size() < 5) {
                return PredictionResult.insufficientData("Need at least 5 data points for prediction");
            }

            // Convert to list and sort by timestamp
            List<HealthDataPoint> sortedData = healthHistory.stream()
                .sorted((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()))
                .toList();

            // Analyze trends for each metric
            TrendAnalysis cpuTrend = analyzeTrend(sortedData, HealthDataPoint::getCpuUsage);
            TrendAnalysis memoryTrend = analyzeTrend(sortedData, HealthDataPoint::getMemoryUsage);
            TrendAnalysis responseTrend = analyzeTrend(sortedData, HealthDataPoint::getResponseTime);
            TrendAnalysis errorTrend = analyzeTrend(sortedData, HealthDataPoint::getErrorRate);

            // Generate predictions
            double predictionMinutes = predictionWindow.toMinutes();

            double predictedCpu = predictValue(cpuTrend, predictionMinutes);
            double predictedMemory = predictValue(memoryTrend, predictionMinutes);
            double predictedResponseTime = predictValue(responseTrend, predictionMinutes);
            double predictedErrorRate = predictValue(errorTrend, predictionMinutes);

            // Calculate confidence based on trend stability
            double confidence = calculatePredictionConfidence(cpuTrend, memoryTrend, responseTrend, errorTrend);

            // Detect anomalies
            List<AnomalyPrediction> anomalies = detectPotentialAnomalies(sortedData, predictionWindow);

            PredictionResult result = PredictionResult.builder()
                .predictionWindow(predictionWindow)
                .predictedCpuAvg(predictedCpu)
                .predictedCpuMax(Math.min(100.0, predictedCpu + cpuTrend.getVolatility() * 2))
                .predictedMemoryAvg(predictedMemory)
                .predictedMemoryMax(Math.min(100.0, predictedMemory + memoryTrend.getVolatility() * 2))
                .predictedResponseTimeAvg(predictedResponseTime)
                .predictedResponseTimeMax(predictedResponseTime + responseTrend.getVolatility() * 2)
                .predictedErrorRateAvg(predictedErrorRate)
                .predictedErrorRateMax(predictedErrorRate + errorTrend.getVolatility() * 2)
                .confidence(confidence)
                .cpuTrend(cpuTrend.getSlope() > 0 ? "INCREASING" : cpuTrend.getSlope() < 0 ? "DECREASING" : "STABLE")
                .memoryTrend(memoryTrend.getSlope() > 0 ? "INCREASING" : memoryTrend.getSlope() < 0 ? "DECREASING" : "STABLE")
                .anomalies(anomalies)
                .build();

            latestPredictions = result;

            LOGGER.debug("Generated predictions: CPU={:.1f}%, Memory={:.1f}%, Confidence={:.2f}",
                predictedCpu, predictedMemory, confidence);

            return result;

        } catch (Exception e) {
            LOGGER.error("Error generating predictions", e);
            return PredictionResult.error("Prediction generation failed: " + e.getMessage());
        }
    }

    /**
     * Analyze trend for a specific metric
     */
    private TrendAnalysis analyzeTrend(List<HealthDataPoint> data, java.util.function.ToDoubleFunction<HealthDataPoint> metricExtractor) {
        if (data.size() < 2) {
            return new TrendAnalysis(0.0, 0.0, 0.0);
        }

        // Extract values and timestamps
        double[] values = data.stream().mapToDouble(metricExtractor).toArray();
        long[] timestamps = data.stream().mapToLong(point -> point.getTimestamp().toEpochMilli()).toArray();

        // Calculate linear regression
        LinearRegression regression = calculateLinearRegression(timestamps, values);

        // Calculate volatility (standard deviation)
        DoubleSummaryStatistics stats = data.stream().mapToDouble(metricExtractor).summaryStatistics();
        double mean = stats.getAverage();
        double variance = data.stream()
            .mapToDouble(metricExtractor)
            .map(value -> Math.pow(value - mean, 2))
            .average()
            .orElse(0.0);
        double volatility = Math.sqrt(variance);

        return new TrendAnalysis(regression.slope, regression.intercept, volatility);
    }

    /**
     * Calculate linear regression for trend analysis
     */
    private LinearRegression calculateLinearRegression(long[] x, double[] y) {
        if (x.length != y.length || x.length < 2) {
            return new LinearRegression(0.0, 0.0);
        }

        int n = x.length;
        double sumX = 0, sumY = 0, sumXY = 0, sumXX = 0;

        for (int i = 0; i < n; i++) {
            sumX += x[i];
            sumY += y[i];
            sumXY += x[i] * y[i];
            sumXX += x[i] * x[i];
        }

        double slope = (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX);
        double intercept = (sumY - slope * sumX) / n;

        return new LinearRegression(slope, intercept);
    }

    /**
     * Predict future value based on trend analysis
     */
    private double predictValue(TrendAnalysis trend, double minutesInFuture) {
        // Convert minutes to milliseconds for prediction
        double futureTimestamp = System.currentTimeMillis() + (minutesInFuture * 60 * 1000);

        // Apply linear prediction with some bounds checking
        double prediction = trend.getSlope() * futureTimestamp + trend.getIntercept();

        // Ensure reasonable bounds
        return Math.max(0.0, Math.min(prediction, 100.0));
    }

    /**
     * Calculate prediction confidence based on trend stability
     */
    private double calculatePredictionConfidence(TrendAnalysis... trends) {
        double baseConfidence = 0.5;

        // Increase confidence for stable trends (low volatility)
        double avgVolatility = 0.0;
        for (TrendAnalysis trend : trends) {
            avgVolatility += trend.getVolatility();
        }
        avgVolatility /= trends.length;

        // Lower volatility = higher confidence
        double volatilityConfidence = Math.max(0.0, 1.0 - (avgVolatility / 50.0));

        // Increase confidence for consistent trend directions
        double trendConsistency = calculateTrendConsistency(trends);

        return Math.min(1.0, baseConfidence + volatilityConfidence * 0.3 + trendConsistency * 0.2);
    }

    /**
     * Calculate trend consistency (how similar trend directions are)
     */
    private double calculateTrendConsistency(TrendAnalysis[] trends) {
        if (trends.length < 2) {
            return 0.0;
        }

        int increasingCount = 0;
        int decreasingCount = 0;
        int stableCount = 0;

        for (TrendAnalysis trend : trends) {
            if (Math.abs(trend.getSlope()) < 0.001) {
                stableCount++;
            } else if (trend.getSlope() > 0) {
                increasingCount++;
            } else {
                decreasingCount++;
            }
        }

        int maxCount = Math.max(Math.max(increasingCount, decreasingCount), stableCount);
        return (double) maxCount / trends.length;
    }

    /**
     * Detect potential anomalies in the prediction window
     */
    private List<AnomalyPrediction> detectPotentialAnomalies(List<HealthDataPoint> data, Duration predictionWindow) {
        List<AnomalyPrediction> anomalies = new ArrayList<>();

        if (data.size() < 5) {
            return anomalies;
        }

        // Get recent data points
        List<HealthDataPoint> recentData = data.stream()
            .filter(point -> point.getTimestamp().isAfter(Instant.now().minus(Duration.ofHours(2))))
            .toList();

        if (recentData.isEmpty()) {
            return anomalies;
        }

        // Check for rapid increases in any metric
        HealthDataPoint latest = recentData.get(recentData.size() - 1);

        if (recentData.size() >= 3) {
            HealthDataPoint previous = recentData.get(recentData.size() - 3);

            // Check for rapid CPU increase
            if (latest.getCpuUsage() - previous.getCpuUsage() > 30) {
                anomalies.add(new AnomalyPrediction(
                    "RAPID_CPU_INCREASE",
                    "CPU usage increased rapidly",
                    0.7,
                    Duration.ofMinutes(15)
                ));
            }

            // Check for rapid memory increase
            if (latest.getMemoryUsage() - previous.getMemoryUsage() > 25) {
                anomalies.add(new AnomalyPrediction(
                    "RAPID_MEMORY_INCREASE",
                    "Memory usage increased rapidly",
                    0.6,
                    Duration.ofMinutes(20)
                ));
            }

            // Check for error rate spike
            if (latest.getErrorRate() - previous.getErrorRate() > 5) {
                anomalies.add(new AnomalyPrediction(
                    "ERROR_RATE_SPIKE",
                    "Error rate spiked significantly",
                    0.8,
                    Duration.ofMinutes(10)
                ));
            }
        }

        return anomalies;
    }

    /**
     * Get the latest prediction results
     */
    public PredictionResult getLatestPredictions() {
        return latestPredictions;
    }

    /**
     * Trend analysis result
     */
    private static class TrendAnalysis {
        private final double slope;
        private final double intercept;
        private final double volatility;

        public TrendAnalysis(double slope, double intercept, double volatility) {
            this.slope = slope;
            this.intercept = intercept;
            this.volatility = volatility;
        }

        public double getSlope() { return slope; }
        public double getIntercept() { return intercept; }
        public double getVolatility() { return volatility; }
    }

    /**
     * Linear regression result
     */
    private static class LinearRegression {
        final double slope;
        final double intercept;

        public LinearRegression(double slope, double intercept) {
            this.slope = slope;
            this.intercept = intercept;
        }
    }
}

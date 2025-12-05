package com.example.bedrockac.checks.combat;

import com.example.bedrockac.checks.Check;
import com.example.bedrockac.player.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerMoveEvent;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Advanced Aim Assist Detection - Utility Class
 * Provides mathematical analysis functions for rotation detection
 * 
 * Based on research from:
 * - Grim AntiCheat
 * - AAC (Advanced Anti-Cheat)
 * - Spartan
 * - Academic papers on statistical anomaly detection
 */
public class RotationAnalysisUtil {

    /**
     * Calculate standard deviation of values
     * Formula: σ = √(Σ(xᵢ - μ)² / n)
     */
    public static double standardDeviation(List<Double> values) {
        if (values.isEmpty()) return 0;
        
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double variance = values.stream()
            .mapToDouble(v -> Math.pow(v - mean, 2))
            .average()
            .orElse(0);
        
        return Math.sqrt(variance);
    }

    /**
     * Calculate coefficient of variation
     * Formula: CV = (σ / μ) × 100
     * 
     * Values:
     * > 15%: Natural
     * 5-15%: Suspicious
     * < 2%: Very suspicious (likely cheating)
     */
    public static double coefficientOfVariation(List<Double> values) {
        if (values.isEmpty()) return 0;
        
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        if (mean == 0) return 0;
        
        double stdDev = standardDeviation(values);
        return (stdDev / mean) * 100;
    }

    /**
     * Calculate Z-score for anomaly detection
     * Formula: Z = (x - μ) / σ
     * 
     * Interpretation:
     * -3 to +3: Normal
     * > +3 or < -3: Outlier (anomaly)
     */
    public static List<Double> zScores(List<Double> values) {
        if (values.isEmpty()) return new ArrayList<>();
        
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double stdDev = standardDeviation(values);
        
        if (stdDev == 0) return values.stream().map(v -> 0.0).collect(Collectors.toList());
        
        final double finalStdDev = stdDev;
        return values.stream()
            .map(v -> (v - mean) / finalStdDev)
            .collect(Collectors.toList());
    }

    /**
     * Calculate skewness (asymmetry of distribution)
     * Formula: Skewness = E[(X - μ)³] / σ³
     * 
     * Values:
     * 0: Symmetric (suspicious)
     * ±0.5 to ±1: Natural
     * > ±1: Highly skewed
     */
    public static double skewness(List<Double> values) {
        if (values.size() < 3) return 0;
        
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double stdDev = standardDeviation(values);
        
        if (stdDev == 0) return 0;
        
        double cubeSum = values.stream()
            .mapToDouble(v -> Math.pow(v - mean, 3))
            .sum();
        
        return (cubeSum / values.size()) / Math.pow(stdDev, 3);
    }

    /**
     * Calculate kurtosis (peakedness of distribution)
     * Formula: Kurtosis = E[(X - μ)⁴] / σ⁴
     * 
     * Values:
     * 3: Normal distribution
     * < 3: Flatter than normal
     * > 5: Very peaked (suspicious - repeated values)
     */
    public static double kurtosis(List<Double> values) {
        if (values.size() < 4) return 0;
        
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double stdDev = standardDeviation(values);
        
        if (stdDev == 0) return 0;
        
        double fourthMoment = values.stream()
            .mapToDouble(v -> Math.pow(v - mean, 4))
            .average()
            .orElse(0);
        
        return fourthMoment / Math.pow(stdDev, 4);
    }

    /**
     * Calculate autocorrelation at lag k
     * Formula: ρ(k) = Cov(Xₜ, Xₜ₊ₖ) / σ²
     * 
     * Values:
     * 0: No correlation (random)
     * > 0.9: Strong correlation (suspicious - dependent)
     */
    public static double autocorrelation(List<Double> values, int lag) {
        if (values.size() < lag + 1) return 0;
        
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double variance = values.stream()
            .mapToDouble(v -> Math.pow(v - mean, 2))
            .average()
            .orElse(0);
        
        if (variance == 0) return 0;
        
        double covariance = 0;
        for (int i = 0; i < values.size() - lag; i++) {
            covariance += (values.get(i) - mean) * (values.get(i + lag) - mean);
        }
        covariance /= (values.size() - lag);
        
        return covariance / variance;
    }

    /**
     * Calculate Pearson correlation coefficient
     * Formula: r = Cov(X, Y) / (σₓ × σᵧ)
     * 
     * Range: -1 to 1
     * Values:
     * 0-0.3: Weak correlation
     * 0.3-0.7: Moderate correlation
     * 0.7-1: Strong correlation (suspicious for Yaw-Pitch)
     */
    public static double pearsonCorrelation(List<Double> x, List<Double> y) {
        if (x.size() != y.size() || x.isEmpty()) return 0;
        
        double meanX = x.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double meanY = y.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        
        double covariance = 0;
        double varX = 0;
        double varY = 0;
        
        for (int i = 0; i < x.size(); i++) {
            double dx = x.get(i) - meanX;
            double dy = y.get(i) - meanY;
            covariance += dx * dy;
            varX += dx * dx;
            varY += dy * dy;
        }
        
        if (varX == 0 || varY == 0) return 0;
        
        return covariance / Math.sqrt(varX * varY);
    }

    /**
     * Detect linearity using linear regression R²
     * Formula: R² = 1 - (SSᵣₑₛ / SSₜₒₜ)
     * 
     * Range: 0 to 1
     * Values:
     * 0.0-0.3: Not linear (natural)
     * 0.3-0.7: Somewhat linear
     * 0.7-0.95: Very linear
     * > 0.95: Perfect linear (suspicious)
     */
    public static double linearityDetection(List<Double> values) {
        if (values.size() < 3) return 0;
        
        // Linear regression: y = mx + b
        double n = values.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        
        for (int i = 0; i < values.size(); i++) {
            sumX += i;
            sumY += values.get(i);
            sumXY += i * values.get(i);
            sumX2 += i * i;
        }
        
        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        double intercept = (sumY - slope * sumX) / n;
        
        // Calculate R²
        double ssRes = 0, ssTot = 0;
        double mean = sumY / n;
        
        for (int i = 0; i < values.size(); i++) {
            double predicted = slope * i + intercept;
            ssRes += Math.pow(values.get(i) - predicted, 2);
            ssTot += Math.pow(values.get(i) - mean, 2);
        }
        
        if (ssTot == 0) return 0;
        return 1 - (ssRes / ssTot);
    }

    /**
     * Detect smoothness using second derivative analysis
     * Measures how much the data deviates from a smooth curve
     * 
     * Range: 0 to 1
     * Values:
     * < 0.5: Rough, jagged (natural)
     * 0.5-0.8: Somewhat smooth
     * > 0.85: Very smooth (suspicious)
     */
    public static double smoothnessScore(List<Double> values) {
        if (values.size() < 3) return 0;
        
        double totalDeviation = 0;
        
        for (int i = 1; i < values.size() - 1; i++) {
            // Expected value if movement was perfectly smooth
            double expected = (values.get(i - 1) + values.get(i + 1)) / 2.0;
            double deviation = Math.abs(values.get(i) - expected);
            totalDeviation += deviation;
        }
        
        double avgDeviation = totalDeviation / (values.size() - 2);
        double smoothness = Math.max(0, 1.0 - (avgDeviation * 0.2));
        
        return Math.min(1.0, smoothness);
    }

    /**
     * Calculate angular acceleration (second derivative)
     * Formula: α = d²θ/dt² ≈ (Δv₂ - Δv₁) / Δt
     * 
     * Returns: List of acceleration values
     */
    public static List<Double> angularAccelerations(List<Double> rotations) {
        List<Double> accelerations = new ArrayList<>();
        
        if (rotations.size() < 3) return accelerations;
        
        for (int i = 1; i < rotations.size() - 1; i++) {
            double v1 = rotations.get(i) - rotations.get(i - 1);
            double v2 = rotations.get(i + 1) - rotations.get(i);
            double acceleration = v2 - v1;
            accelerations.add(acceleration);
        }
        
        return accelerations;
    }

    /**
     * Detect consistent patterns
     * Counts how many consecutive values are nearly identical
     * 
     * Returns: Max consecutive identical pattern count
     */
    public static int detectConsistency(List<Double> values, double threshold) {
        int consecutive = 0;
        int maxConsecutive = 0;
        
        for (int i = 1; i < values.size(); i++) {
            double diff = Math.abs(values.get(i) - values.get(i - 1));
            if (diff < threshold) {
                consecutive++;
                maxConsecutive = Math.max(maxConsecutive, consecutive);
            } else {
                consecutive = 0;
            }
        }
        
        return maxConsecutive;
    }

    /**
     * Calculate Mahalanobis distance for multivariate anomaly detection
     * Useful for detecting unusual combinations of yaw and pitch changes
     */
    public static double mahalanobisDistance(double[] point, double[] mean, double[][] covariance) {
        int n = point.length;
        double[] diff = new double[n];
        
        for (int i = 0; i < n; i++) {
            diff[i] = point[i] - mean[i];
        }
        
        // Matrix inverse (simplified for 2D)
        if (n == 2) {
            double det = covariance[0][0] * covariance[1][1] - covariance[0][1] * covariance[1][0];
            if (Math.abs(det) < 1e-10) return 0;
            
            double[][] invCov = new double[2][2];
            invCov[0][0] = covariance[1][1] / det;
            invCov[0][1] = -covariance[0][1] / det;
            invCov[1][0] = -covariance[1][0] / det;
            invCov[1][1] = covariance[0][0] / det;
            
            double sum = 0;
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 2; j++) {
                    sum += diff[i] * invCov[i][j] * diff[j];
                }
            }
            
            return Math.sqrt(Math.max(0, sum));
        }
        
        return 0;
    }

    /**
     * Calculate entropy of distribution
     * Lower entropy = more predictable (suspicious)
     * Formula: H = -Σ p(x) × log₂(p(x))
     */
    public static double entropyOfDistribution(List<Double> values) {
        if (values.isEmpty()) return 0;
        
        // Create histogram (bins)
        Map<Integer, Integer> histogram = new HashMap<>();
        double min = values.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        double max = values.stream().mapToDouble(Double::doubleValue).max().orElse(1);
        double range = max - min;
        
        if (range == 0) return 0;
        
        for (Double v : values) {
            int bin = (int) ((v - min) / range * 10);
            bin = Math.max(0, Math.min(9, bin));
            histogram.put(bin, histogram.getOrDefault(bin, 0) + 1);
        }
        
        double entropy = 0;
        int total = values.size();
        
        for (int count : histogram.values()) {
            double p = (double) count / total;
            if (p > 0) {
                entropy -= p * (Math.log(p) / Math.log(2));
            }
        }
        
        return entropy;
    }

    /**
     * Detect target tracking patterns
     * Checks if rotations correlate with expected enemy positions
     */
    public static double targetTrackingScore(List<Double> yawDeltas, List<Double> pitchDeltas) {
        if (yawDeltas.size() != pitchDeltas.size() || yawDeltas.isEmpty()) {
            return 0;
        }
        
        // High correlation between yaw and pitch = tracking a specific target
        return Math.abs(pearsonCorrelation(yawDeltas, pitchDeltas));
    }

    /**
     * Generate anomaly score combining multiple metrics
     * Returns: 0.0 (natural) to 1.0 (definitely cheating)
     */
    public static double generateAnomalyScore(List<Double> yawDeltas) {
        if (yawDeltas.size() < 10) return 0;
        
        double score = 0;
        
        // Variance analysis
        double cv = coefficientOfVariation(yawDeltas);
        if (cv < 2) score += 0.3;
        else if (cv < 5) score += 0.15;
        
        // Linearity detection
        double linearity = linearityDetection(yawDeltas);
        if (linearity > 0.95) score += 0.3;
        else if (linearity > 0.8) score += 0.15;
        
        // Smoothness analysis
        double smoothness = smoothnessScore(yawDeltas);
        if (smoothness > 0.95) score += 0.2;
        else if (smoothness > 0.85) score += 0.1;
        
        // Skewness (should not be 0)
        double skew = Math.abs(skewness(yawDeltas));
        if (skew < 0.1) score += 0.1;
        
        // Kurtosis (should be 2.5-3.5 for natural)
        double kurt = kurtosis(yawDeltas);
        if (kurt > 5) score += 0.15;
        
        // Consistency pattern
        int consistency = detectConsistency(yawDeltas, 0.1);
        if (consistency > 10) score += 0.2;
        
        return Math.min(1.0, score);
    }

    /**
     * Generate detailed report of analysis
     */
    public static String generateReport(List<Double> yawDeltas) {
        if (yawDeltas.isEmpty()) return "No data";
        
        StringBuilder report = new StringBuilder();
        
        double variance = standardDeviation(yawDeltas);
        double cv = coefficientOfVariation(yawDeltas);
        double linearity = linearityDetection(yawDeltas);
        double smoothness = smoothnessScore(yawDeltas);
        double skew = skewness(yawDeltas);
        double kurt = kurtosis(yawDeltas);
        int consistency = detectConsistency(yawDeltas, 0.1);
        double anomaly = generateAnomalyScore(yawDeltas);
        
        report.append("=== ROTATION ANALYSIS REPORT ===\n");
        report.append(String.format("Sample Size: %d\n", yawDeltas.size()));
        report.append(String.format("Variance (σ): %.4f°\n", variance));
        report.append(String.format("CV: %.2f%%\n", cv));
        report.append(String.format("Linearity: %.2f%%\n", linearity * 100));
        report.append(String.format("Smoothness: %.2f%%\n", smoothness * 100));
        report.append(String.format("Skewness: %.4f\n", skew));
        report.append(String.format("Kurtosis: %.4f\n", kurt));
        report.append(String.format("Max Consistency: %d\n", consistency));
        report.append(String.format("Anomaly Score: %.2f%%\n", anomaly * 100));
        
        report.append("\n=== DIAGNOSIS ===\n");
        if (anomaly > 0.8) {
            report.append("VERDICT: ⚠️ VERY SUSPICIOUS (Likely Aim Assist)\n");
        } else if (anomaly > 0.6) {
            report.append("VERDICT: ⚠️ SUSPICIOUS (Possible Aim Assist)\n");
        } else if (anomaly > 0.4) {
            report.append("VERDICT: ℹ️ WATCH (Monitor player)\n");
        } else {
            report.append("VERDICT: ✓ NATURAL (Appears legitimate)\n");
        }
        
        return report.toString();
    }
}

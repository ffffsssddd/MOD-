package com.example.bedrockac.checks.combat;

import java.util.*;

/**
 * Test Suite for Rotation Analysis
 * Simulates different scenarios to validate detection accuracy
 */
public class RotationAnalysisTest {

    public static void main(String[] args) {
        System.out.println("=== BedrockAC Rotation Analysis Test Suite ===\n");
        
        testNaturalRotation();
        testAimAssistRotation();
        testImpossibleRotation();
        testVarianceDetection();
        testLinearityDetection();
        testCorrelationDetection();
        testSmoothnessDetection();
    }

    /**
     * Test 1: Natural Human Rotation
     */
    static void testNaturalRotation() {
        System.out.println("TEST 1: Natural Human Rotation");
        System.out.println("=====================================");
        
        // Simulated natural rotation deltas (degrees per tick)
        List<Double> naturalYaws = Arrays.asList(
            5.2, 4.8, 6.1, 5.5, 4.9, 5.3, 6.2, 4.7, 5.8, 5.1,
            4.9, 5.4, 6.3, 5.0, 5.2, 4.6, 5.7, 5.5, 4.8, 5.9
        );
        
        double variance = RotationAnalysisUtil.standardDeviation(naturalYaws);
        double cv = RotationAnalysisUtil.coefficientOfVariation(naturalYaws);
        double linearity = RotationAnalysisUtil.linearityDetection(naturalYaws);
        double smoothness = RotationAnalysisUtil.smoothnessScore(naturalYaws);
        double skew = RotationAnalysisUtil.skewness(naturalYaws);
        double kurt = RotationAnalysisUtil.kurtosis(naturalYaws);
        
        System.out.printf("Variance (σ): %.4f° [Expected: > 0.5°]\n", variance);
        System.out.printf("CV: %.2f%% [Expected: > 15%%]\n", cv);
        System.out.printf("Linearity: %.2f%% [Expected: < 70%%]\n", linearity * 100);
        System.out.printf("Smoothness: %.2f%% [Expected: < 70%%]\n", smoothness * 100);
        System.out.printf("Skewness: %.4f [Expected: ±0.5 to ±1.0]\n", skew);
        System.out.printf("Kurtosis: %.4f [Expected: 2.5-3.5]\n", kurt);
        
        double anomaly = RotationAnalysisUtil.generateAnomalyScore(naturalYaws);
        System.out.printf("Anomaly Score: %.2f%% [Expected: < 40%%]\n", anomaly * 100);
        
        if (variance > 0.5 && cv > 15 && linearity < 0.7 && anomaly < 0.4) {
            System.out.println("✓ RESULT: NATURAL (PASS)\n");
        } else {
            System.out.println("✗ RESULT: FALSE POSITIVE (FAIL)\n");
        }
    }

    /**
     * Test 2: Aim Assist Rotation (Perfect consistency)
     */
    static void testAimAssistRotation() {
        System.out.println("TEST 2: Aim Assist Rotation (Perfect Consistency)");
        System.out.println("=====================================");
        
        // Simulated aim assist: perfectly consistent deltas
        List<Double> assistYaws = Arrays.asList(
            12.0, 12.0, 12.1, 11.9, 12.0, 12.1, 12.0, 11.9,
            12.0, 12.0, 12.1, 11.9, 12.0, 12.1, 12.0, 11.9,
            12.0, 12.0, 12.1, 11.9
        );
        
        double variance = RotationAnalysisUtil.standardDeviation(assistYaws);
        double cv = RotationAnalysisUtil.coefficientOfVariation(assistYaws);
        double linearity = RotationAnalysisUtil.linearityDetection(assistYaws);
        double smoothness = RotationAnalysisUtil.smoothnessScore(assistYaws);
        int consistency = RotationAnalysisUtil.detectConsistency(assistYaws, 0.15);
        
        System.out.printf("Variance (σ): %.4f° [Expected: < 0.1°]\n", variance);
        System.out.printf("CV: %.2f%% [Expected: < 2%%]\n", cv);
        System.out.printf("Linearity: %.2f%% [Expected: > 85%%]\n", linearity * 100);
        System.out.printf("Smoothness: %.2f%% [Expected: > 85%%]\n", smoothness * 100);
        System.out.printf("Consistency Pattern: %d [Expected: > 10]\n", consistency);
        
        double anomaly = RotationAnalysisUtil.generateAnomalyScore(assistYaws);
        System.out.printf("Anomaly Score: %.2f%% [Expected: > 70%%]\n", anomaly * 100);
        
        if (variance < 0.1 && cv < 2 && linearity > 0.85 && anomaly > 0.7) {
            System.out.println("✓ RESULT: SUSPICIOUS (PASS)\n");
        } else {
            System.out.println("✗ RESULT: FALSE NEGATIVE (FAIL)\n");
        }
    }

    /**
     * Test 3: Impossible Rotation (> 90°/tick)
     */
    static void testImpossibleRotation() {
        System.out.println("TEST 3: Impossible Rotation (> 90°/tick)");
        System.out.println("=====================================");
        
        List<Double> impossibleYaws = Arrays.asList(
            45.0, 135.0, // Jump of 90°!
            50.0, 51.0, 50.5
        );
        
        double maxDelta = impossibleYaws.stream()
            .skip(1)
            .mapToDouble((current) -> {
                Double prev = null;
                for (Double d : impossibleYaws) {
                    if (d.equals(current)) break;
                    prev = d;
                }
                return Math.abs(current - prev);
            })
            .max()
            .orElse(0);
        
        System.out.printf("Max Delta: %.1f° [Expected: > 90°]\n", maxDelta);
        
        if (maxDelta > 90) {
            System.out.println("✓ RESULT: IMPOSSIBLE (PASS)\n");
        } else {
            System.out.println("✗ RESULT: NOT DETECTED (FAIL)\n");
        }
    }

    /**
     * Test 4: Variance Threshold Detection
     */
    static void testVarianceDetection() {
        System.out.println("TEST 4: Variance Threshold Detection");
        System.out.println("=====================================");
        
        List<Double> lowVariance = Arrays.asList(
            12.0, 12.01, 12.02, 11.99, 11.98, 12.01, 12.02, 11.99
        );
        
        List<Double> mediumVariance = Arrays.asList(
            10.0, 12.0, 8.0, 11.5, 9.5, 12.5, 10.5, 11.0
        );
        
        List<Double> highVariance = Arrays.asList(
            2.0, 15.0, 3.5, 20.0, 4.5, 18.0, 5.0, 19.0
        );
        
        double var1 = RotationAnalysisUtil.standardDeviation(lowVariance);
        double var2 = RotationAnalysisUtil.standardDeviation(mediumVariance);
        double var3 = RotationAnalysisUtil.standardDeviation(highVariance);
        
        System.out.printf("Low Variance: %.4f° [Expected: < 0.05°]\n", var1);
        System.out.printf("Medium Variance: %.4f° [Expected: 0.5-2.0°]\n", var2);
        System.out.printf("High Variance: %.4f° [Expected: > 2.0°]\n", var3);
        
        boolean pass = var1 < 0.05 && var2 > 0.5 && var3 > 2.0;
        System.out.printf("✓ RESULT: %s\n\n", pass ? "PASS" : "FAIL");
    }

    /**
     * Test 5: Linearity Detection
     */
    static void testLinearityDetection() {
        System.out.println("TEST 5: Linearity Detection");
        System.out.println("=====================================");
        
        // Perfect linear data (R² = 1.0)
        List<Double> perfectLinear = Arrays.asList(
            10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0, 80.0
        );
        
        // Natural random data (R² < 0.5)
        List<Double> randomData = Arrays.asList(
            15.3, 22.8, 18.5, 45.2, 28.1, 35.9, 42.4, 19.7
        );
        
        double linear = RotationAnalysisUtil.linearityDetection(perfectLinear);
        double random = RotationAnalysisUtil.linearityDetection(randomData);
        
        System.out.printf("Perfect Linear R²: %.4f [Expected: > 0.95]\n", linear);
        System.out.printf("Random Data R²: %.4f [Expected: < 0.5]\n", random);
        
        boolean pass = linear > 0.95 && random < 0.5;
        System.out.printf("✓ RESULT: %s\n\n", pass ? "PASS" : "FAIL");
    }

    /**
     * Test 6: Yaw-Pitch Correlation (Entity tracking)
     */
    static void testCorrelationDetection() {
        System.out.println("TEST 6: Yaw-Pitch Correlation (Entity Tracking)");
        System.out.println("=====================================");
        
        // Perfect correlation (r ≈ 1.0) - tracking entity
        List<Double> yaws1 = Arrays.asList(5.0, 10.0, 15.0, 20.0, 25.0, 30.0);
        List<Double> pitches1 = Arrays.asList(2.0, 4.0, 6.0, 8.0, 10.0, 12.0);
        
        // Random correlation (r ≈ 0.0) - natural
        List<Double> yaws2 = Arrays.asList(5.0, 2.0, 15.0, 3.0, 25.0, 7.0);
        List<Double> pitches2 = Arrays.asList(8.0, 3.0, 2.0, 10.0, 4.0, 15.0);
        
        double corr1 = Math.abs(RotationAnalysisUtil.pearsonCorrelation(yaws1, pitches1));
        double corr2 = Math.abs(RotationAnalysisUtil.pearsonCorrelation(yaws2, pitches2));
        
        System.out.printf("Tracking (Perfect Corr): %.4f [Expected: > 0.92]\n", corr1);
        System.out.printf("Natural (Random Corr): %.4f [Expected: < 0.5]\n", corr2);
        
        boolean pass = corr1 > 0.92 && corr2 < 0.5;
        System.out.printf("✓ RESULT: %s\n\n", pass ? "PASS" : "FAIL");
    }

    /**
     * Test 7: Smoothness Score
     */
    static void testSmoothnessDetection() {
        System.out.println("TEST 7: Smoothness Detection");
        System.out.println("=====================================");
        
        // Perfect smooth curve (interpolation)
        List<Double> smoothData = Arrays.asList(
            10.0, 12.5, 15.0, 17.5, 20.0, 22.5, 25.0, 27.5, 30.0
        );
        
        // Jagged natural data
        List<Double> jaggedData = Arrays.asList(
            10.0, 15.5, 12.2, 18.1, 14.0, 19.5, 16.3, 20.1, 18.0
        );
        
        double smoothScore = RotationAnalysisUtil.smoothnessScore(smoothData);
        double jaggedScore = RotationAnalysisUtil.smoothnessScore(jaggedData);
        
        System.out.printf("Smooth Data Score: %.2f%% [Expected: > 85%%]\n", smoothScore * 100);
        System.out.printf("Jagged Data Score: %.2f%% [Expected: < 60%%]\n", jaggedScore * 100);
        
        boolean pass = smoothScore > 0.85 && jaggedScore < 0.6;
        System.out.printf("✓ RESULT: %s\n\n", pass ? "PASS" : "FAIL");
    }

    /**
     * Generate Test Report
     */
    public static void generateTestReport() {
        System.out.println("\n=== COMPREHENSIVE TEST REPORT ===\n");
        
        // Test with real-world scenario
        List<Double> realisticAimAssist = generateRealisticAimAssist();
        List<Double> realisticNatural = generateRealisticNatural();
        
        System.out.println("SCENARIO 1: Realistic Aim Assist");
        System.out.println(RotationAnalysisUtil.generateReport(realisticAimAssist));
        
        System.out.println("\n\nSCENARIO 2: Realistic Natural Player");
        System.out.println(RotationAnalysisUtil.generateReport(realisticNatural));
    }

    private static List<Double> generateRealisticAimAssist() {
        List<Double> data = new ArrayList<>();
        Random rand = new Random(42); // Seed for reproducibility
        
        // Aim assist: 12°/tick with minimal variation
        for (int i = 0; i < 50; i++) {
            double value = 12.0 + (rand.nextDouble() - 0.5) * 0.1; // ±0.05° variation
            data.add(value);
        }
        
        return data;
    }

    private static List<Double> generateRealisticNatural() {
        List<Double> data = new ArrayList<>();
        Random rand = new Random(123);
        
        // Natural: varying speeds with realistic jitter
        for (int i = 0; i < 50; i++) {
            double baseSpeed = 3.0 + Math.sin(i * 0.1) * 3.0; // Smooth oscillation
            double jitter = (rand.nextDouble() - 0.5) * 2.0; // ±1° jitter
            data.add(baseSpeed + jitter);
        }
        
        return data;
    }
}

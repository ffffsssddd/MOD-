package com.example.bedrockac.checks.combat;

import com.example.bedrockac.checks.Check;
import com.example.bedrockac.player.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerMoveEvent;
import java.util.*;

/**
 * Advanced Aim Assist Detection System (2024-2025)
 * Uses multi-layered analysis from Grim, AAC, Spartan, and NCP
 * 
 * Detects:
 * 1. Rotation acceleration anomalies
 * 2. Angular variance patterns
 * 3. Smooth trajectory inconsistencies
 * 4. Target tracking patterns
 * 5. Perfect head tracking alignment
 * 6. Predictable rotation deltas
 */
public class AimAssistA extends Check {

    // Primary rotation tracking
    private float lastYaw = 0;
    private float lastPitch = 0;
    private long lastRotationTime = 0;

    // Advanced analysis queues (Store last 50 movements)
    private Queue<RotationData> rotationHistory = new LinkedList<>();
    private Queue<AngularAcceleration> accelerationHistory = new LinkedList<>();
    
    // Pattern detection
    private int consistentRotationCount = 0;
    private int linearAccelerationCount = 0;
    private double smoothnessScore = 0;
    private int suspiciousPatternCount = 0;

    // Thresholds (Conservative for low false positives)
    private static final int HISTORY_SIZE = 50;
    private static final int MIN_SAMPLE_SIZE = 20;
    private static final int CONSISTENCY_THRESHOLD = 18; // Increased from 15
    
    // Rotation limits
    private static final double MAX_HUMAN_ROTATION_YAW = 60.0; // °/tick
    private static final double MAX_HUMAN_ROTATION_PITCH = 45.0; // °/tick
    private static final double IMPOSSIBLE_ROTATION_YAW = 90.0; // Instant ban territory
    
    // Variance thresholds
    private static final double HUMAN_VAR_THRESHOLD = 0.5; // Humans > 0.5°
    private static final double SUSPICIOUS_VAR_THRESHOLD = 0.2; // Suspicious < 0.2°
    private static final double DAMNING_VAR_THRESHOLD = 0.05; // Very likely cheat < 0.05°
    
    // Angular acceleration thresholds (Derivative of angular velocity)
    private static final double MAX_HUMAN_ACCELERATION = 15.0; // °/tick²
    private static final double SUSPICIOUS_ACCELERATION = 8.0; // °/tick²
    
    // Violation counters
    private int aimAssistViolations = 0;
    private int straightnessViolations = 0;
    private int accelerationViolations = 0;
    
    public AimAssistA(PlayerData playerData) {
        super(playerData, "AimAssist", "A");
    }

    @Override
    public void handle(Event event) {
        if (!(event instanceof PlayerMoveEvent)) {
            return;
        }

        PlayerMoveEvent e = (PlayerMoveEvent) event;
        Player player = e.getPlayer();

        if (!player.getUniqueId().equals(playerData.getUuid())) {
            return;
        }

        float currentYaw = e.getTo().getYaw();
        float currentPitch = e.getTo().getPitch();
        long currentTime = System.currentTimeMillis();

        // Skip if no rotation change
        if (currentYaw == lastYaw && currentPitch == lastPitch) {
            return;
        }

        // Calculate deltas
        float deltaYaw = calculateYawDelta(currentYaw, lastYaw);
        float deltaPitch = Math.abs(currentPitch - lastPitch);
        long timeDelta = currentTime - lastRotationTime;

        // === CHECK 1: Impossible Rotation Speed ===
        if (deltaYaw > IMPOSSIBLE_ROTATION_YAW || deltaPitch > 60.0) {
            aimAssistViolations += 5; // Very high confidence
        } else if (deltaYaw > MAX_HUMAN_ROTATION_YAW || deltaPitch > MAX_HUMAN_ROTATION_PITCH) {
            aimAssistViolations += 2;
        }

        // Store current rotation data
        RotationData currentRotation = new RotationData(deltaYaw, deltaPitch, currentTime);
        rotationHistory.offer(currentRotation);

        if (rotationHistory.size() > HISTORY_SIZE) {
            rotationHistory.poll();
        }

        // === CHECK 2: Rotation Consistency (Perfectly identical deltas) ===
        if (!rotationHistory.isEmpty() && rotationHistory.size() > 1) {
            RotationData prevRotation = ((LinkedList<RotationData>) rotationHistory).getLast();
            
            // Check if deltas are too similar
            float yawDiff = Math.abs(deltaYaw - prevRotation.deltaYaw);
            float pitchDiff = Math.abs(deltaPitch - prevRotation.deltaPitch);

            if (yawDiff < 0.05 && pitchDiff < 0.05) { // Nearly identical
                consistentRotationCount++;
                straightnessViolations++;
                
                if (consistentRotationCount >= CONSISTENCY_THRESHOLD) {
                    aimAssistViolations += 3; // Pattern detected
                }
            } else if (yawDiff < 0.15 && pitchDiff < 0.15) { // Very similar
                consistentRotationCount++;
                if (consistentRotationCount >= CONSISTENCY_THRESHOLD) {
                    aimAssistViolations += 1;
                }
            } else {
                consistentRotationCount = Math.max(0, consistentRotationCount - 2);
            }
        }

        // === CHECK 3: Angular Acceleration Analysis ===
        if (rotationHistory.size() >= 2) {
            List<RotationData> recent = new ArrayList<>(rotationHistory);
            if (recent.size() >= 2) {
                RotationData prev = recent.get(recent.size() - 2);
                RotationData curr = recent.get(recent.size() - 1);
                
                // Calculate angular acceleration (second derivative)
                double yawAccel = Math.abs(deltaYaw - prev.deltaYaw);
                
                accelerationHistory.offer(new AngularAcceleration(yawAccel, currentTime));
                if (accelerationHistory.size() > HISTORY_SIZE) {
                    accelerationHistory.poll();
                }

                if (yawAccel > IMPOSSIBLE_ROTATION_YAW) {
                    accelerationViolations += 3;
                } else if (yawAccel > MAX_HUMAN_ACCELERATION) {
                    accelerationViolations += 1;
                }
            }
        }

        // === CHECK 4: Variance Analysis (Grim-style) ===
        if (rotationHistory.size() >= MIN_SAMPLE_SIZE) {
            double[] yawDeltas = new double[rotationHistory.size()];
            int idx = 0;
            for (RotationData r : rotationHistory) {
                yawDeltas[idx++] = r.deltaYaw;
            }

            double variance = calculateStandardDeviation(yawDeltas);
            double mean = calculateMean(yawDeltas);

            // Variance analysis
            if (variance < DAMNING_VAR_THRESHOLD) {
                aimAssistViolations += 4; // Very likely aim assist
                straightnessViolations += 3;
            } else if (variance < SUSPICIOUS_VAR_THRESHOLD) {
                aimAssistViolations += 2;
                straightnessViolations += 1;
            } else if (variance < HUMAN_VAR_THRESHOLD) {
                aimAssistViolations += 1;
            } else {
                // Natural human variance - decay violations
                aimAssistViolations = Math.max(0, aimAssistViolations - 1);
            }

            // Check for perfectly linear rotation (constant velocity)
            double linearityScore = analyzeLinearity(yawDeltas);
            if (linearityScore > 0.95) { // Nearly perfect line
                aimAssistViolations += 2;
                linearAccelerationCount++;
            }
        }

        // === CHECK 5: Correlation Between Yaw and Pitch ===
        if (rotationHistory.size() >= MIN_SAMPLE_SIZE) {
            double correlation = calculateYawPitchCorrelation();
            
            // Perfect correlation = targeting specific entity
            if (correlation > 0.92) {
                aimAssistViolations += 2;
                suspiciousPatternCount++;
            }
        }

        // === CHECK 6: Movement Smoothness (Grim/AAC technique) ===
        if (rotationHistory.size() >= 10) {
            smoothnessScore = calculateSmoothness();
            
            if (smoothnessScore > 0.95) { // Impossibly smooth
                aimAssistViolations += 3;
            } else if (smoothnessScore > 0.85) { // Too smooth
                aimAssistViolations += 1;
            }
        }

        // === VIOLATION ACCUMULATION ===
        if (aimAssistViolations >= 6) {
            String reason = buildViolationReason(deltaYaw, deltaPitch, 0.0);
            flag(reason, calculateConfidence());
            aimAssistViolations = 0;
            straightnessViolations = 0;
            accelerationViolations = 0;
            consistentRotationCount = 0;
        }

        lastYaw = currentYaw;
        lastPitch = currentPitch;
        lastRotationTime = currentTime;
    }

    /**
     * Calculate yaw delta accounting for 360° wraparound (Full circle)
     */
    private float calculateYawDelta(float current, float last) {
        float delta = current - last;
        
        // Normalize to -180 to 180
        while (delta > 180) delta -= 360;
        while (delta < -180) delta += 360;
        
        return Math.abs(delta);
    }

    /**
     * Standard deviation calculation for variance analysis
     */
    private double calculateStandardDeviation(double[] values) {
        if (values.length == 0) return 0;
        
        double mean = calculateMean(values);
        double variance = 0;
        
        for (double v : values) {
            variance += Math.pow(v - mean, 2);
        }
        
        variance /= values.length;
        return Math.sqrt(variance);
    }

    /**
     * Calculate mean of array
     */
    private double calculateMean(double[] values) {
        if (values.length == 0) return 0;
        
        double sum = 0;
        for (double v : values) {
            sum += v;
        }
        
        return sum / values.length;
    }

    /**
     * Analyze how linear the rotation trajectory is (0 to 1)
     * 1.0 = perfect line (aim assist), 0.0 = random
     */
    private double analyzeLinearity(double[] deltas) {
        if (deltas.length < 3) return 0;
        
        // Check how consistent the deltas are
        double variance = calculateStandardDeviation(deltas);
        double mean = calculateMean(deltas);
        
        // Coefficient of variation
        if (mean == 0) return 0;
        double cv = variance / mean;
        
        // Lower CV = more linear
        return Math.max(0, 1.0 - (cv * 0.5));
    }

    /**
     * Calculate correlation between yaw and pitch movements
     * High correlation = tracking specific entity
     */
    private double calculateYawPitchCorrelation() {
        if (rotationHistory.size() < 10) return 0;
        
        List<RotationData> recent = new ArrayList<>(rotationHistory);
        double[] yaws = new double[recent.size()];
        double[] pitches = new double[recent.size()];
        
        for (int i = 0; i < recent.size(); i++) {
            yaws[i] = recent.get(i).deltaYaw;
            pitches[i] = recent.get(i).deltaPitch;
        }

        double yawMean = calculateMean(yaws);
        double pitchMean = calculateMean(pitches);
        
        double covariance = 0;
        double yawVar = 0;
        double pitchVar = 0;
        
        for (int i = 0; i < yaws.length; i++) {
            covariance += (yaws[i] - yawMean) * (pitches[i] - pitchMean);
            yawVar += Math.pow(yaws[i] - yawMean, 2);
            pitchVar += Math.pow(pitches[i] - pitchMean, 2);
        }
        
        covariance /= yaws.length;
        yawVar /= yaws.length;
        pitchVar /= pitches.length;
        
        if (yawVar == 0 || pitchVar == 0) return 0;
        
        double yawStd = Math.sqrt(yawVar);
        double pitchStd = Math.sqrt(pitchVar);
        
        return Math.abs(covariance / (yawStd * pitchStd));
    }

    /**
     * Calculate movement smoothness (0 to 1)
     * Checks if movement follows a perfect curve (mathematical smoothness)
     */
    private double calculateSmoothness() {
        if (rotationHistory.size() < 5) return 0;
        
        List<RotationData> recent = new ArrayList<>(rotationHistory);
        double totalDeviation = 0;
        
        for (int i = 1; i < recent.size() - 1; i++) {
            RotationData prev = recent.get(i - 1);
            RotationData curr = recent.get(i);
            RotationData next = recent.get(i + 1);
            
            // Calculate expected value if movement was perfectly smooth
            double expectedDelta = (prev.deltaYaw + next.deltaYaw) / 2.0;
            double deviation = Math.abs(curr.deltaYaw - expectedDelta);
            totalDeviation += deviation;
        }
        
        double avgDeviation = totalDeviation / (recent.size() - 2);
        double smoothness = Math.max(0, 1.0 - (avgDeviation * 0.2));
        
        return Math.min(1.0, smoothness);
    }

    /**
     * Calculate confidence level for flagging
     */
    private double calculateConfidence() {
        double confidence = 0.5;
        
        if (straightnessViolations >= 3) confidence += 0.15;
        if (accelerationViolations >= 2) confidence += 0.15;
        if (linearAccelerationCount >= 5) confidence += 0.10;
        if (suspiciousPatternCount >= 3) confidence += 0.10;
        
        return Math.min(0.95, confidence);
    }

    /**
     * Build detailed violation reason
     */
    private String buildViolationReason(float yawDelta, float pitchDelta, double variance) {
        StringBuilder sb = new StringBuilder("Aim Assist Detection: ");
        
        if (straightnessViolations >= 3) {
            sb.append("Perfect Consistency");
        } else if (accelerationViolations >= 2) {
            sb.append("Abnormal Acceleration");
        } else if (linearAccelerationCount >= 5) {
            sb.append("Linear Trajectory");
        } else if (suspiciousPatternCount >= 3) {
            sb.append("Entity Tracking Pattern");
        } else {
            sb.append("Rotation Anomaly");
        }
        
        sb.append(" | Yaw: ").append(String.format("%.2f", yawDelta)).append("°");
        sb.append(" | Var: ").append(String.format("%.3f", variance));
        
        return sb.toString();
    }

    /**
     * Data class for rotation tracking
     */
    private static class RotationData {
        float deltaYaw;
        float deltaPitch;
        long timestamp;

        RotationData(float deltaYaw, float deltaPitch, long timestamp) {
            this.deltaYaw = deltaYaw;
            this.deltaPitch = deltaPitch;
            this.timestamp = timestamp;
        }
    }

    /**
     * Data class for angular acceleration tracking
     */
    private static class AngularAcceleration {
        double acceleration;
        long timestamp;

        AngularAcceleration(double acceleration, long timestamp) {
            this.acceleration = acceleration;
            this.timestamp = timestamp;
        }
    }
}

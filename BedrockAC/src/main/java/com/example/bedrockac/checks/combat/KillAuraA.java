package com.example.bedrockac.checks.combat;

import com.example.bedrockac.checks.Check;
import com.example.bedrockac.player.PlayerData;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;

public class KillAuraA extends Check {

    public KillAuraA(PlayerData playerData) {
        super(playerData, "KillAura", "A");
    }

    @Override
    public void handle(Event event) {
        if (!(event instanceof PlayerMoveEvent)) {
            return;
        }

        Player player = ((PlayerMoveEvent) event).getPlayer();

        // This check only analyzes rotation data, which is updated on PlayerMoveEvent.
        // We only want to run this intensive analysis when it's relevant: right after an attack.
        long timeSinceAttack = System.currentTimeMillis() - playerData.lastAttackTime;
        if (timeSinceAttack > 500) { // Only analyze rotations within 0.5 seconds of an attack
            return;
        }

        // We need enough rotation history to do a meaningful analysis.
        if (playerData.rotationHistory.size() < 15) {
            return;
        }

        List<Vector> recentRotations = playerData.rotationHistory.stream().collect(Collectors.toList());

        // --- Statistical Analysis ---
        // We analyze the Yaw and Pitch changes separately.
        List<Double> yawDeltas = recentRotations.stream().map(Vector::getX).collect(Collectors.toList());
        List<Double> pitchDeltas = recentRotations.stream().map(Vector::getY).collect(Collectors.toList());

        double yawStdDev = calculateStandardDeviation(yawDeltas);
        double pitchStdDev = calculateStandardDeviation(pitchDeltas);
        
        double yawJitter = calculateJitter(yawDeltas);
        double pitchJitter = calculateJitter(pitchDeltas);

        // --- Violation Logic ---
        int vl = getViolations();
        boolean flagged = false;

        // Unnaturally smooth rotations (low standard deviation) are a huge red flag.
        // A real player's aim has noise and imperfection.
        if (yawStdDev < 0.05 && pitchStdDev < 0.05) {
            vl += 4;
            flagged = true;
        } else if (yawStdDev < 0.1 || pitchStdDev < 0.1) {
            vl += 2;
            flagged = true;
        }
        
        // Low jitter means the rate of rotation change is too consistent.
        if (yawJitter < 0.01 && pitchJitter < 0.01) {
            vl += 3;
            flagged = true;
        }

        // Also check for impossible, instant rotations (cinematic/snap hacks)
        if (playerData.deltaYaw > 300.0) { // A 300-degree snap in a single tick
            vl += 10; // This is almost definitive proof of cheating
            flagged = true;
        }

        // --- Predictive Analysis ---
        if (playerData.lastTarget != null && playerData.lastTarget.isValid() && playerData.lastTargetLocationHistory.size() > 5) {
            Location predictedTargetLoc = predictTargetLocation(playerData.lastTarget, playerData.lastTargetLocationHistory, 5); // Predict 5 ticks ahead
            if (predictedTargetLoc != null) {
                Vector playerLookDir = player.getLocation().getDirection();
                Vector toPredictedTarget = predictedTargetLoc.toVector().subtract(player.getEyeLocation().toVector()).normalize();

                double angleToPredicted = playerLookDir.angle(toPredictedTarget); // Angle in radians

                // If player is consistently looking at the predicted location with very low error
                if (angleToPredicted < 0.05) { // ~2.8 degrees
                    vl += 5; // Strong evidence for predictive aimbot
                    flagged = true;
                }
            }
        }
        
        if (flagged) {
            if (vl > 8) {
                flag("KillAura. R-SD:" + f(yawStdDev) + " P-SD:" + f(pitchStdDev) + " J:" + f(yawJitter),
                        (double) vl / 15.0);
            }
            setViolations(vl);
        } else {
            setViolations(Math.max(0, vl - 2)); // Decay slightly faster
        }
    }

    private double calculateStandardDeviation(List<Double> data) {
        if (data.isEmpty()) return 0.0;
        double mean = data.stream().mapToDouble(d -> d).average().orElse(0.0);
        double variance = data.stream().mapToDouble(d -> Math.pow(d - mean, 2)).average().orElse(0.0);
        return Math.sqrt(variance);
    }

    private double calculateJitter(List<Double> data) {
        if (data.size() < 2) return 0.0;
        double totalJitter = 0;
        for (int i = 1; i < data.size(); i++) {
            totalJitter += Math.abs(data.get(i) - data.get(i-1));
        }
        return totalJitter / (data.size() - 1);
    }

    // A simple linear prediction model
    private Location predictTargetLocation(Entity target, Deque<Location> history, int ticksToPredict) {
        if (history.size() < 2) return null; // Need at least two points to calculate velocity

        List<Location> locs = history.stream().collect(Collectors.toList());
        Location last = locs.get(locs.size() - 1);
        Location secondLast = locs.get(locs.size() - 2);

        Vector velocity = last.toVector().subtract(secondLast.toVector());
        return last.clone().add(velocity.multiply(ticksToPredict));
    }

    private String f(double d) {
        return String.format("%.4f", d);
    }
}

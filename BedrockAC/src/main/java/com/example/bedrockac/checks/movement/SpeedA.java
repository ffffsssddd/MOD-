package com.example.bedrockac.checks.movement;

import com.example.bedrockac.checks.Check;
import com.example.bedrockac.player.PlayerData;
import com.example.bedrockac.utils.PlatformDetector;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffectType;

public class SpeedA extends Check {

    private double lastDeltaXZ = 0.0;

    public SpeedA(PlayerData playerData) {
        super(playerData, "Speed", "A");
    }

    @Override
    public void handle(Event event) {
        if (!(event instanceof PlayerMoveEvent)) {
            return;
        }

        Player player = ((PlayerMoveEvent) event).getPlayer();

        // Use the new contextual data from PlayerData to exempt checks
        if (playerData.ticksSinceTeleport < 3 || playerData.ticksSinceVelocity < 3 || playerData.isClimbing || playerData.inWeb) {
            lastDeltaXZ = 0.0; // Reset state during exemptions
            return;
        }

        // All movement data is now pre-calculated in PlayerData
        double horizontalDistance = playerData.deltaXZ;

        // Calculate expected max speed based on rich context
        double maxSpeed = getBaseMaxSpeed(player);

        // Calculate predicted speed based on friction
        double predictedSpeed = lastDeltaXZ * (playerData.onIce ? 0.98 : 0.91); // Use onIce context
        predictedSpeed += player.isSprinting() ? (playerData.onStairs ? 0.09 : 0.1) : 0.08;

        // --- Main Checks ---

        // 1. Absolute Speed Limit Check
        if (horizontalDistance > maxSpeed + 0.05) {
            // This check is good for catching blatant speed hacks
            if (getViolations() > 3) {
                flag("Exceeded max speed. Dist: " + f(horizontalDistance) + " > Max: " + f(maxSpeed), 1.0);
            }
            setViolations(getViolations() + 1);
        }

        // 2. Friction/Deceleration Check (More sensitive and accurate)
        if (playerData.airTicks < 3 && horizontalDistance > predictedSpeed + 0.12) {
             if (getViolations() > 5) {
                flag("Failed friction. Dist: " + f(horizontalDistance) + " > Pred: " + f(predictedSpeed), 0.7);
             }
             setViolations(getViolations() + 1);
        } else {
            // Decay violations only when movement is valid
            setViolations(Math.max(0, getViolations() - 1));
        }

        this.lastDeltaXZ = horizontalDistance;
    }

    private double getBaseMaxSpeed(Player player) {
        // Start with a base speed and modify it based on context from PlayerData
        double baseSpeed = player.isSprinting() ? 0.285 : 0.22;

        // Potion effects
        if (player.hasPotionEffect(PotionEffectType.SPEED)) {
            int amplifier = player.getPotionEffect(PotionEffectType.SPEED).getAmplifier();
            baseSpeed *= (1.0 + (0.2 * (amplifier + 1)));
        }
        if (player.hasPotionEffect(PotionEffectType.SLOW)) {
            int amplifier = player.getPotionEffect(PotionEffectType.SLOW).getAmplifier();
            baseSpeed *= (1.0 - (0.15 * (amplifier + 1)));
        }

        // Environmental effects from PlayerData
        if (playerData.onIce) {
            baseSpeed *= 2.5;
        }
        if (playerData.onSlime) {
             baseSpeed *= 1.2; // Bouncing can be fast
        }
        if (playerData.onStairs) {
            baseSpeed *= 1.3; // Moving up stairs is faster horizontally
        }
        if (playerData.inWeb) {
            baseSpeed = 0; // Should not be moving
        }

        // Air strafing can slightly increase speed
        if (playerData.airTicks > 2) {
             baseSpeed += 0.15;
        }

        return baseSpeed + PlatformDetector.getSpeedTolerance(player);
    }

    // Helper to format doubles
    private String f(double d) {
        return String.format("%.2f", d);
    }
}

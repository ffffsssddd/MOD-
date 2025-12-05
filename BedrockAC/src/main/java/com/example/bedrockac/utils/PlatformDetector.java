package com.example.bedrockac.utils;

import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;

/**
 * Detects whether a player is on Java or Bedrock Edition
 * This is crucial for applying correct detection thresholds
 */
public class PlatformDetector {

    private static FloodgateApi floodgateApi;

    public PlatformDetector(FloodgateApi api) {
        PlatformDetector.floodgateApi = api;
    }

    /**
     * Check if a player is on Bedrock Edition
     */
    public static boolean isBedrock(Player player) {
        if (floodgateApi == null) {
            return false;
        }
        return floodgateApi.isFloodgatePlayer(player.getUniqueId());
    }

    /**
     * Check if a player is on Java Edition
     */
    public static boolean isJava(Player player) {
        return !isBedrock(player);
    }

    /**
     * Get platform-specific reach value
     * Java: 3.0 blocks
     * Bedrock: 3.0-4.0 blocks (variable)
     */
    public static double getMaxReach(Player player) {
        if (isBedrock(player)) {
            return 3.5; // Bedrock can reach slightly further
        }
        return 3.0; // Java standard reach
    }

    /**
     * Get platform-specific max speed
     * Java: 2.0 blocks/tick (sprinting)
     * Bedrock: 2.1 blocks/tick (different physics)
     */
    public static double getMaxSpeed(Player player) {
        if (isBedrock(player)) {
            return 2.1; // Bedrock slightly faster
        }
        return 2.0; // Java standard
    }

    /**
     * Get platform-specific attack speed
     * Java: 1.6 attacks/second (with cooldown)
     * Bedrock: 2-3 attacks/second (different system)
     */
    public static double getMaxAttackSpeed(Player player) {
        if (isBedrock(player)) {
            return 3.0; // Bedrock faster attacks
        }
        return 1.6; // Java standard (1.9+ cooldown)
    }

    /**
     * Get platform-specific ping compensation
     * Java: Higher ping (100-500ms typical)
     * Bedrock: Lower ping (20-100ms typical)
     */
    public static double getPingCompensation(Player player) {
        int ping = player.getPing();

        if (isBedrock(player)) {
            // Bedrock players have lower ping
            // More lenient compensation
            if (ping < 50) return 0.05;
            if (ping < 100) return 0.15;
            if (ping < 200) return 0.30;
            return 0.40;
        } else {
            // Java players have higher ping
            // More generous compensation
            if (ping < 50) return 0.10;
            if (ping < 100) return 0.20;
            if (ping < 200) return 0.35;
            return 0.50;
        }
    }

    /**
     * Get platform-specific flight detection sensitivity
     * Java: More strict (less tolerance for gravity errors)
     * Bedrock: More lenient (more prediction on client)
     */
    public static double getFlightTolerance(Player player) {
        if (isBedrock(player)) {
            return 0.05; // Bedrock: 5cm tolerance
        }
        return 0.02; // Java: 2cm tolerance
    }

    /**
     * Get platform-specific speed check sensitivity
     * Java: More strict
     * Bedrock: More lenient due to client-side movement prediction
     */
    public static double getSpeedTolerance(Player player) {
        if (isBedrock(player)) {
            return 0.03; // Bedrock: 3cm tolerance
        }
        return 0.01; // Java: 1cm tolerance
    }

    /**
     * Get platform-specific AutoClicker StdDev threshold
     * Java: > 8ms is human
     * Bedrock: > 10ms is human (higher latency)
     */
    public static double getAutoClickerStdDevThreshold(Player player) {
        if (isBedrock(player)) {
            return 10.0; // Bedrock: higher variance expected
        }
        return 8.0; // Java: standard threshold
    }

    /**
     * Get platform-specific rotation sensitivity
     * Java: More strict (precise mouse control)
     * Bedrock: More lenient (controller input)
     */
    public static double getRotationSensitivity(Player player) {
        if (isBedrock(player)) {
            return 0.75; // Bedrock: 75% sensitivity (controllers)
        }
        return 1.0; // Java: 100% sensitivity (mouse)
    }

    /**
     * Check if player is using controller (Bedrock only)
     */
    public static boolean isUsingController(Player player) {
        if (!isBedrock(player)) {
            return false; // Java doesn't have controller detection
        }
        
        // Bedrock-specific: Controllers can't rotate as fast
        // This would need additional implementation
        return false; // Placeholder
    }

    /**
     * Get platform name for logging
     */
    public static String getPlatformName(Player player) {
        return isBedrock(player) ? "Bedrock" : "Java";
    }

    /**
     * Get all platform-specific info as a string
     */
    public static String getPlatformInfo(Player player) {
        StringBuilder sb = new StringBuilder();
        sb.append("Platform: ").append(getPlatformName(player)).append(" | ");
        sb.append("Max Reach: ").append(String.format("%.2f", getMaxReach(player))).append(" | ");
        sb.append("Max Speed: ").append(String.format("%.2f", getMaxSpeed(player))).append(" | ");
        sb.append("Max Attack Speed: ").append(String.format("%.2f", getMaxAttackSpeed(player)));
        
        return sb.toString();
    }
}

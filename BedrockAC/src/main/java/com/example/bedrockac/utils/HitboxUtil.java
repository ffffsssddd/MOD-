package com.example.bedrockac.utils;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.util.Vector;

/**
 * Utility class for accurate hitbox calculations in Minecraft
 */
public class HitboxUtil {

    /**
     * Get the exact minimum Y position of an entity's hitbox
     */
    public static double getHitboxMinY(Entity entity) {
        return entity.getLocation().getY();
    }

    /**
     * Get the exact maximum Y position of an entity's hitbox
     */
    public static double getHitboxMaxY(Entity entity) {
        return entity.getLocation().getY() + entity.getHeight();
    }

    /**
     * Get hitbox width (radius from center)
     */
    public static double getHitboxWidth(Entity entity) {
        return entity.getWidth() / 2.0;
    }

    /**
     * Calculate the closest point on the entity's hitbox to the player's eye
     * Returns the distance to that point
     */
    public static double getDistanceToHitbox(Location eyeLocation, Entity target) {
        Location targetLoc = target.getLocation();
        
        // Entity hitbox dimensions
        double hitboxMinY = getHitboxMinY(target);
        double hitboxMaxY = getHitboxMaxY(target);
        double hitboxRadius = getHitboxWidth(target);
        
        // Clamp player's eye position to closest point on hitbox
        double closestX = clamp(
            eyeLocation.getX(),
            targetLoc.getX() - hitboxRadius,
            targetLoc.getX() + hitboxRadius
        );
        
        double closestY = clamp(
            eyeLocation.getY(),
            hitboxMinY,
            hitboxMaxY
        );
        
        double closestZ = clamp(
            eyeLocation.getZ(),
            targetLoc.getZ() - hitboxRadius,
            targetLoc.getZ() + hitboxRadius
        );
        
        // Calculate distance from eye to closest point
        double dx = eyeLocation.getX() - closestX;
        double dy = eyeLocation.getY() - closestY;
        double dz = eyeLocation.getZ() - closestZ;
        
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * Check if player can actually see the target (line of sight)
     * Based on player's looking direction
     */
    public static boolean canSeeTarget(Player player, Entity target) {
        Location eyeLoc = player.getEyeLocation();
        Location targetLoc = target.getLocation().add(0, target.getHeight() / 2, 0);
        
        // Get player's looking direction
        Vector direction = eyeLoc.getDirection();
        
        // Vector from eye to target
        Vector toTarget = targetLoc.subtract(eyeLoc).toVector();
        
        // Dot product - if negative, target is behind player
        double dot = direction.dot(toTarget);
        
        return dot > 0;
    }

    /**
     * Calculate if the reach is within realistic bounds considering hitbox
     */
    public static boolean isReachValid(Player player, Entity target, double maxReach) {
        Location eyeLoc = player.getEyeLocation();
        double distance = getDistanceToHitbox(eyeLoc, target);
        
        // Add small buffer for latency
        double buffer = 0.2;
        int ping = player.getPing();
        if (ping > 0) {
            buffer += Math.min(ping / 2000.0, 0.3);
        }
        
        return distance <= (maxReach + buffer);
    }

    /**
     * Clamp a value between min and max
     */
    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Get entity type specific hitbox height
     * Different mobs have different heights
     */
    public static double getEntityHitboxHeight(Entity entity) {
        String type = entity.getType().name();
        
        // Player hitbox
        if (entity instanceof Player) {
            return 1.8;
        }
        
        // Common mobs
        switch (type) {
            case "ZOMBIE":
            case "HUSK":
            case "SKELETON":
                return 1.95;
            case "CREEPER":
                return 1.7;
            case "SPIDER":
            case "CAVE_SPIDER":
                return 0.9;
            case "ENDERMAN":
                return 2.9;
            case "WITCH":
                return 1.95;
            case "IRON_GOLEM":
                return 2.7;
            case "WITHER":
                return 3.5;
            case "ENDER_DRAGON":
                return 8.0;
            default:
                return entity.getHeight();
        }
    }
}

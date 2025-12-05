package com.example.bedrockac.utils;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.Location;
import org.bukkit.util.Vector;

/**
 * Calculates precise hitbox boundaries for entities
 * Used for accurate Reach detection
 */
public class HitboxCalculator {

    /**
     * Get the bounding box of an entity
     */
    public static BoundingBox getEntityBoundingBox(Entity entity) {
        Location loc = entity.getLocation();
        double x = loc.getX();
        double y = loc.getY();
        double z = loc.getZ();
        
        double width = entity.getWidth();
        double height = entity.getHeight();
        
        // Calculate bounding box corners
        double minX = x - width / 2.0;
        double maxX = x + width / 2.0;
        
        double minY = y;
        double maxY = y + height;
        
        double minZ = z - width / 2.0;
        double maxZ = z + width / 2.0;
        
        return new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * Calculate the closest point on the hitbox to the player's eye
     */
    public static double getClosestPointDistance(Player player, Entity target) {
        Location eyeLoc = player.getEyeLocation();
        BoundingBox box = getEntityBoundingBox(target);
        
        // Find closest point on the bounding box
        double closestX = Math.max(box.minX, Math.min(eyeLoc.getX(), box.maxX));
        double closestY = Math.max(box.minY, Math.min(eyeLoc.getY(), box.maxY));
        double closestZ = Math.max(box.minZ, Math.min(eyeLoc.getZ(), box.maxZ));
        
        // Calculate distance
        double dx = eyeLoc.getX() - closestX;
        double dy = eyeLoc.getY() - closestY;
        double dz = eyeLoc.getZ() - closestZ;
        
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * Check if player is looking at the entity (simple angle check)
     */
    public static boolean isLookingAtEntity(Player player, Entity target) {
        Location playerLoc = player.getEyeLocation();
        Location targetLoc = target.getLocation().add(0, target.getHeight() / 2, 0);
        
        Vector direction = targetLoc.subtract(playerLoc).toVector().normalize();
        Vector playerLook = playerLoc.getDirection().normalize();
        
        // Dot product for angle calculation
        double dot = direction.dot(playerLook);
        
        // Threshold: approximately 45 degrees
        return dot > 0.707; // cos(45°) ≈ 0.707
    }

    /**
     * Get entity center for precise calculations
     */
    public static Location getEntityCenter(Entity entity) {
        Location loc = entity.getLocation();
        return new Location(
            loc.getWorld(),
            loc.getX(),
            loc.getY() + entity.getHeight() / 2.0,
            loc.getZ()
        );
    }

    /**
     * Bounding box class to represent entity hitbox
     */
    public static class BoundingBox {
        public final double minX, minY, minZ;
        public final double maxX, maxY, maxZ;

        public BoundingBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
        }

        public double getWidth() {
            return maxX - minX;
        }

        public double getHeight() {
            return maxY - minY;
        }

        public double getDepth() {
            return maxZ - minZ;
        }

        public boolean contains(double x, double y, double z) {
            return x >= minX && x <= maxX &&
                   y >= minY && y <= maxY &&
                   z >= minZ && z <= maxZ;
        }
    }
}

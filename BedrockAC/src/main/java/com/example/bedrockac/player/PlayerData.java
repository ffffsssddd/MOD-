package com.example.bedrockac.player;

import com.example.bedrockac.checks.CheckManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity; // Added import for Entity
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

import java.util.ArrayDeque;
import java.util.Deque; // Added import for Deque
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerData {

    private final UUID uuid;
    private final CheckManager checkManager;

    // Core Violation Data
    private final Map<String, Integer> violations = new HashMap<>();

    // Movement & Position Context
    public final Deque<Location> locationHistory = new ArrayDeque<>(20); // Changed to public
    private Location lastLocation;
    public double deltaX, deltaY, deltaZ, deltaXZ;
    public int ticksSinceTeleport = 100;
    public int ticksSinceVelocity = 100;
    public int airTicks = 0;
    public int groundTicks = 0;

    // Environmental Context
    public boolean onIce, onSlime, onStairs, inWeb, isClimbing;
    public int ticksOnIce = 0;

    // Rotation Context
    public final Deque<Vector> rotationHistory = new ArrayDeque<>(20);
    public float lastYaw, lastPitch, deltaYaw, deltaPitch;

    // Action Context
    public long lastAttackTime, lastVelocityTime;
    public Vector lastVelocity;

    // Target Context for KillAura Predictive Analysis
    public Entity lastTarget;
    public final Deque<Location> lastTargetLocationHistory = new ArrayDeque<>(20);


    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        this.checkManager = new CheckManager(this);
    }

    /**
     * This method is the heart of the context system.
     * It's called on every PlayerMoveEvent to update the player's state BEFORE any checks are run.
     */
    public void update(PlayerMoveEvent e) {
        Player player = e.getPlayer(); // Ensure player is in scope at the beginning of the method

        // Update locations
        lastLocation = locationHistory.isEmpty() ? e.getFrom() : locationHistory.getLast();
        locationHistory.addLast(e.getTo());
        if (locationHistory.size() > 20) {
            locationHistory.removeFirst();
        }

        // Update deltas
        this.deltaX = e.getTo().getX() - e.getFrom().getX();
        this.deltaY = e.getTo().getY() - e.getFrom().getY();
        this.deltaZ = e.getTo().getZ() - e.getFrom().getZ(); // Corrected typo: e.To() -> e.getTo()
        this.deltaXZ = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        
        // Update rotations
        this.deltaYaw = Math.abs(e.getTo().getYaw() - lastYaw);
        this.deltaPitch = Math.abs(e.getTo().getPitch() - lastPitch);
        this.lastYaw = e.getTo().getYaw();
        this.lastPitch = e.getTo().getPitch();
        rotationHistory.addLast(new Vector(deltaYaw, deltaPitch, 0));
        if (rotationHistory.size() > 20) {
            rotationHistory.removeFirst();
        }

        // Update tick counters
        this.ticksSinceTeleport++;
        // Update velocity information
        // Check for null lastVelocity before using equals to avoid NullPointerException
        if (lastVelocity == null || !player.getVelocity().equals(lastVelocity)) { // Only update if velocity has changed or if lastVelocity is null
            this.lastVelocity = player.getVelocity();
            this.lastVelocityTime = System.currentTimeMillis();
            this.ticksSinceVelocity = 0; // Reset as new velocity is detected
        } else {
            this.ticksSinceVelocity++;
        }
        if (e.getFrom().distanceSquared(e.getTo()) > 100) { // Teleport check
            ticksSinceTeleport = 0;
        }

        // Update ground/air ticks
        if (player.isOnGround()) {
            airTicks = 0;
            groundTicks++;
        } else {
            groundTicks = 0;
            airTicks++;
        }
        
        // Update environmental context
        Block blockBelow = player.getLocation().subtract(0, 1, 0).getBlock();
        Block blockIn = player.getLocation().getBlock();
        
        this.onIce = blockBelow.getType() == Material.ICE || blockBelow.getType() == Material.PACKED_ICE;
        this.onSlime = blockBelow.getType() == Material.SLIME_BLOCK;
        this.onStairs = blockBelow.getType().name().contains("STAIRS");
        this.inWeb = blockIn.getType() == Material.COBWEB;
        this.isClimbing = blockIn.getType() == Material.LADDER || blockIn.getType() == Material.VINE;
        
        if (onIce) {
            ticksOnIce++;
        } else {
            ticksOnIce = 0;
        }
    }


    public UUID getUuid() {
        return uuid;
    }

    public CheckManager getCheckManager() {
        return checkManager;
    }

    public int getViolations(String checkName) {
        return violations.getOrDefault(checkName, 0);
    }

    public void addViolation(String checkName) {
        violations.put(checkName, getViolations(checkName) + 1);
    }

    public void setViolations(String checkName, int amount) {
        if (amount <= 0) {
            violations.remove(checkName);
        } else {
            violations.put(checkName, amount);
        }
    }
    
    public Map<String, Integer> getViolations() {
        return violations;
    }

    public long getLastAttackTime() {
        return lastAttackTime;
    }

    public void setLastAttackTime(long lastAttackTime) {
        this.lastAttackTime = lastAttackTime;
    }

    public int getTicksSinceVelocity() {
        return ticksSinceVelocity;
    }

    public void setTicksSinceVelocity(int ticksSinceVelocity) {
        this.ticksSinceVelocity = ticksSinceVelocity;
    }

    public Vector getLastVelocity() { // Getter for lastVelocity, since it's used elsewhere
        return lastVelocity;
    }

    public void setLastVelocity(Vector lastVelocity) { // Setter for lastVelocity (might be useful for VelocityA)
        this.lastVelocity = lastVelocity;
    }

    public long getLastVelocityTime() { // Getter for lastVelocityTime
        return lastVelocityTime;
    }

    public void setLastVelocityTime(long lastVelocityTime) { // Setter for lastVelocityTime
        this.lastVelocityTime = lastVelocityTime;
    }
}
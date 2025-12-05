package com.example.bedrockac.checks.combat;

import com.example.bedrockac.checks.Check;
import com.example.bedrockac.player.PlayerData;
import com.example.bedrockac.utils.HitboxCalculator;
import com.example.bedrockac.utils.PlatformDetector;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class ReachA extends Check {

    private int reachViolations = 0;
    private static final int VIOLATION_THRESHOLD = 3;
    
    // Real Minecraft physics
    private static final double MAX_REACH = 3.0; // Survival mode reach
    private static final double CREATIVE_REACH = 5.0; // Creative mode (not checked)
    private static final double REACH_BUFFER = 0.2; // Small buffer for edge cases
    private static final double BEDROCK_BUFFER = 0.1; // Bedrock sometimes has slight differences

    public ReachA(PlayerData playerData) {
        super(playerData, "Reach", "A");
    }

    @Override
    public void handle(Event event) {
        if (!(event instanceof EntityDamageByEntityEvent)) {
            return;
        }

        EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;
        if (!(e.getDamager() instanceof Player)) {
            return;
        }

        Player player = (Player) e.getDamager();
        if (!player.getUniqueId().equals(playerData.getUuid())) {
            return;
        }

        if (player.getGameMode() == GameMode.CREATIVE) {
            return; // Creative mode has longer reach - don't check
        }

        Entity target = e.getEntity();
        
        // Skip if target is dead or invalid
        if (target.isDead() || !target.isValid()) {
            return;
        }
        
        // Use precise hitbox calculation
        double distance = HitboxCalculator.getClosestPointDistance(player, target);
        
        // Check if player is actually looking at the target
        if (!HitboxCalculator.isLookingAtEntity(player, target)) {
            return; // Player not looking at target - skip check
        }
        
        // Calculate allowed reach with latency compensation
        // More lenient for normal walking/movement situations
        double maxReach = PlatformDetector.getMaxReach(player);
        double pingCompensation = PlatformDetector.getPingCompensation(player);
        double allowedReach = maxReach + REACH_BUFFER + pingCompensation + 0.1;
        
        if (PlatformDetector.isBedrock(player)) {
            allowedReach += BEDROCK_BUFFER; // Slight adjustment for Bedrock
        }
        
        // Only flag if distance is CLEARLY over the limit
        if (distance > allowedReach + 0.3) { // Extra tolerance for edge cases
            reachViolations++;
            
            // Only flag if we have consistent violations
            if (reachViolations >= VIOLATION_THRESHOLD + 1) {
                flag("Reach Exceeded - Distance: " + String.format("%.2f", distance) + 
                     ", Max: " + String.format("%.2f", allowedReach) + 
                     ", Ping: " + player.getPing() + "ms" +
                     ", Platform: " + PlatformDetector.getPlatformName(player), 0.6);
                reachViolations = 0;
            }
        } else {
            // Decrease violations if player is within reach
            reachViolations = Math.max(0, reachViolations - 2);
        }
    }
}

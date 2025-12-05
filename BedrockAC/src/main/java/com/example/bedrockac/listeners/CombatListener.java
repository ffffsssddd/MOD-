package com.example.bedrockac.listeners;

import com.example.bedrockac.BedrockAC;
import com.example.bedrockac.player.PlayerData;
import com.example.bedrockac.utils.DataLogger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class CombatListener implements Listener {

    private final BedrockAC plugin;

    public CombatListener(BedrockAC plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getDamager();
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);

        if (playerData != null) {
            playerData.setLastAttackTime(System.currentTimeMillis());
            
            // Set last target for predictive analysis
            if (playerData.lastTarget == null || !playerData.lastTarget.equals(event.getEntity())) {
                playerData.lastTarget = event.getEntity();
                playerData.lastTargetLocationHistory.clear(); // Clear history for new target
            }
            // Add current location of target
            playerData.lastTargetLocationHistory.addLast(event.getEntity().getLocation());
            if (playerData.lastTargetLocationHistory.size() > 20) { // Keep buffer size consistent
                playerData.lastTargetLocationHistory.removeFirst();
            }

            playerData.getCheckManager().handle(event);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);

        if (playerData != null) {
            playerData.getCheckManager().handle(event);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);

        if (playerData != null) {
            // Update all contextual data before running checks
            playerData.update(event);

            // Update target's location history if a target exists and is valid
            if (playerData.lastTarget != null && playerData.lastTarget.isValid()) {
                // Only add if location is different to avoid duplicate entries for stationary targets
                if (playerData.lastTargetLocationHistory.isEmpty() ||
                    !playerData.lastTargetLocationHistory.getLast().equals(playerData.lastTarget.getLocation())) {
                    
                    playerData.lastTargetLocationHistory.addLast(playerData.lastTarget.getLocation());
                    if (playerData.lastTargetLocationHistory.size() > 20) { // Keep buffer size consistent
                        playerData.lastTargetLocationHistory.removeFirst();
                    }
                }
            }

            // Log player data for ML
            DataLogger dataLogger = plugin.getPlayerDataManager().getDataLogger(player);
            if (dataLogger != null) { // Check if logging is enabled (via config later)
                dataLogger.logPlayerData(playerData);
            }

            playerData.getCheckManager().handle(event);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);

        if (playerData != null) {
            playerData.getCheckManager().handle(event);
        }
    }

    @EventHandler
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);

        if (playerData != null) {
            playerData.getCheckManager().handle(event);
        }
    }
}
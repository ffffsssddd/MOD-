package com.example.bedrockac.listeners;

import com.example.bedrockac.BedrockAC;
import com.example.bedrockac.player.PlayerDataManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerConnectionListener implements Listener {

    private final PlayerDataManager playerDataManager;

    public PlayerConnectionListener(BedrockAC plugin) {
        this.playerDataManager = plugin.getPlayerDataManager();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        playerDataManager.addPlayer(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        playerDataManager.removePlayer(player);
    }
}

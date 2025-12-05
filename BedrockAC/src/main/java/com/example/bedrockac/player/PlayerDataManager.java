package com.example.bedrockac.player;

import com.example.bedrockac.BedrockAC;
import com.example.bedrockac.utils.DataLogger;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {

    private final BedrockAC plugin;
    private final Map<UUID, PlayerData> playerDataMap = new ConcurrentHashMap<>();
    private final Map<UUID, DataLogger> dataLoggerMap = new ConcurrentHashMap<>();
    private final boolean mlLoggingEnabled; // New field to store config

    public PlayerDataManager(BedrockAC plugin, boolean mlLoggingEnabled) {
        this.plugin = plugin;
        this.mlLoggingEnabled = mlLoggingEnabled;
        startViolationDecayTask();
    }

    public void addPlayer(Player player) {
        PlayerData playerData = new PlayerData(player.getUniqueId());
        playerDataMap.put(player.getUniqueId(), playerData);

        // Initialize DataLogger for the player only if ML logging is enabled
        if (mlLoggingEnabled) {
            DataLogger dataLogger = new DataLogger(player.getUniqueId(), mlLoggingEnabled); // Pass config
            dataLoggerMap.put(player.getUniqueId(), dataLogger);
        }
    }

    public void removePlayer(Player player) {
        playerDataMap.remove(player.getUniqueId());
        
        // Close and remove DataLogger
        DataLogger dataLogger = dataLoggerMap.remove(player.getUniqueId());
        if (dataLogger != null) {
            dataLogger.close();
        }
    }

    public PlayerData getPlayerData(Player player) {
        return playerDataMap.get(player.getUniqueId());
    }
    
    public PlayerData getPlayerData(UUID uuid) {
        return playerDataMap.get(uuid);
    }

    public DataLogger getDataLogger(Player player) {
        return dataLoggerMap.get(player.getUniqueId());
    }

    private void startViolationDecayTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (PlayerData data : playerDataMap.values()) {
                    // This is a placeholder for a more robust decay system.
                    // For example, you might want to decay different checks at different rates.
                    data.getViolations().entrySet().removeIf(entry -> {
                        entry.setValue(entry.getValue() - 1);
                        return entry.getValue() <= 0;
                    });
                }
            }
        }.runTaskTimerAsynchronously(plugin, 20 * 60, 20 * 60); // Run every minute
    }
}

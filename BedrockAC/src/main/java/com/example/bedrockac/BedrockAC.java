package com.example.bedrockac;

import com.example.bedrockac.listeners.CombatListener;
import com.example.bedrockac.listeners.PlayerConnectionListener;
import com.example.bedrockac.player.PlayerDataManager;
import com.example.bedrockac.storage.DatabaseManager;
import com.example.bedrockac.utils.Banner;
import com.example.bedrockac.utils.PlatformDetector;
import org.bukkit.plugin.java.JavaPlugin;
import org.geysermc.floodgate.api.FloodgateApi;

public final class BedrockAC extends JavaPlugin {

    private FloodgateApi floodgateApi;
    private DatabaseManager databaseManager;
    private PlayerDataManager playerDataManager;
    private boolean mlLoggingEnabled; // New field for ML logging config

    @Override
    public void onEnable() {
        // Display banner
        getLogger().info(Banner.getBanner());
        
        // Load configuration
        saveDefaultConfig(); // Creates config.yml if it doesn't exist
        this.mlLoggingEnabled = getConfig().getBoolean("ml_logging_enabled", false); // Default to false
        getLogger().info("ML Data Logging: " + (mlLoggingEnabled ? "Enabled" : "Disabled"));

        if (getServer().getPluginManager().isPluginEnabled("Floodgate")) {
            this.floodgateApi = FloodgateApi.getInstance();
            new PlatformDetector(floodgateApi);
            getLogger().info("✅ Floodgate API initialized - Bedrock support enabled");
        } else {
            getLogger().warning("⚠️ Floodgate not found - Bedrock detection disabled");
        }
        
        this.databaseManager = new DatabaseManager(this);
        this.playerDataManager = new PlayerDataManager(this, this.mlLoggingEnabled);

        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new CombatListener(this), this);
        
        getLogger().info(Banner.getChecksBanner());
        getLogger().info("✅ BedrockAC has been enabled!");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info(Banner.getDisableBanner());
        getLogger().info("BedrockAC has been disabled!");
    }

    public FloodgateApi getFloodgateApi() {
        return floodgateApi;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public boolean isMlLoggingEnabled() {
        return mlLoggingEnabled;
    }
}

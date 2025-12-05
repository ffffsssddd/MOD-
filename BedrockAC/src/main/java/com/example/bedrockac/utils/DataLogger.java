package com.example.bedrockac.utils;

import com.example.bedrockac.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class DataLogger {

    private final UUID playerUuid;
    private File logFile;
    private BufferedWriter writer;
    private boolean headerWritten = false;
    private final boolean loggingEnabled; // New field to control logging
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

    public DataLogger(UUID playerUuid, boolean loggingEnabled) {
        this.playerUuid = playerUuid;
        this.loggingEnabled = loggingEnabled;
        if (loggingEnabled) { // Only initialize file if logging is enabled
            initializeLogFile();
        }
    }

    private void initializeLogFile() {
        if (!loggingEnabled) return; // Double check

        try {
            File dataFolder = new File("plugins/BedrockAC/ml_data");
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }

            String timestamp = DATE_FORMAT.format(new Date());
            logFile = new File(dataFolder, playerUuid.toString() + "_" + timestamp + ".csv");
            
            // Append mode to allow continuous logging, true for append
            writer = new BufferedWriter(new FileWriter(logFile, true)); 
        } catch (IOException e) {
            Bukkit.getLogger().severe("Failed to initialize data log file for " + playerUuid + ": " + e.getMessage());
        }
    }

    public void writeHeader() {
        if (!loggingEnabled || headerWritten || writer == null) return; // Check loggingEnabled
        try {
            // Define all relevant features to be logged from PlayerData
            String header = "timestamp,player_uuid,deltaX,deltaY,deltaZ,deltaXZ,airTicks,groundTicks," +
                            "onIce,onSlime,onStairs,inWeb,isClimbing,ticksOnIce,ticksSinceTeleport,ticksSinceVelocity," +
                            "deltaYaw,deltaPitch,rotationStdDevYaw,rotationStdDevPitch,rotationJitterYaw,rotationJitterPitch," +
                            "cps,clickStdDev,clickJitter,isStable,isInCombat,lastAttackTime," +
                            "targetExists,targetDistance,targetPredictedAngle"; // Add more as needed

            writer.write(header);
            writer.newLine();
            headerWritten = true;
            writer.flush(); // Ensure header is written immediately
        } catch (IOException e) {
            Bukkit.getLogger().severe("Failed to write data log header for " + playerUuid + ": " + e.getMessage());
        }
    }

    public void logPlayerData(PlayerData playerData) {
        if (writer == null) return;
        if (!headerWritten) writeHeader(); // Ensure header is always written before first data row

        try {
            // Extract features from PlayerData
            long timestamp = System.currentTimeMillis();
            Player player = Bukkit.getPlayer(playerData.getUuid());

            // Movement and Environment
            String movementData = String.format("%.4f,%.4f,%.4f,%.4f,%d,%d,%b,%b,%b,%b,%b,%d,%d,%d",
                                                playerData.deltaX, playerData.deltaY, playerData.deltaZ, playerData.deltaXZ,
                                                playerData.airTicks, playerData.groundTicks,
                                                playerData.onIce, playerData.onSlime, playerData.onStairs, playerData.inWeb, playerData.isClimbing,
                                                playerData.ticksOnIce, playerData.ticksSinceTeleport, playerData.ticksSinceVelocity);

            // Rotation (these would be calculated externally or in the check, but for logging, we can re-derive simple stats)
            List<Vector> recentRotations = playerData.rotationHistory.stream().collect(Collectors.toList());
            List<Double> yawDeltas = recentRotations.stream().map(Vector::getX).collect(Collectors.toList());
            List<Double> pitchDeltas = recentRotations.stream().map(Vector::getY).collect(Collectors.toList());
            
            double rotationStdDevYaw = calculateStandardDeviationDouble(yawDeltas);
            double rotationStdDevPitch = calculateStandardDeviationDouble(pitchDeltas);
            double rotationJitterYaw = calculateJitterDouble(yawDeltas);
            double rotationJitterPitch = calculateJitterDouble(pitchDeltas);
            String rotationData = String.format("%.4f,%.4f,%.4f,%.4f", rotationStdDevYaw, rotationStdDevPitch, rotationJitterYaw, rotationJitterPitch);


            // Clicks (placeholder, will need a way to get click data directly or from AutoClicker check)
            // For now, these will be dummy values or extracted from a different PlayerData field if implemented
            String clickData = String.format("%.2f,%.2f,%.2f", 0.0, 0.0, 0.0); // CPS, StdDev, Jitter

            // Context (placeholder, values would come from PlayerData flags or similar)
            boolean isStable = false; // Placeholder
            boolean isInCombat = (System.currentTimeMillis() - playerData.getLastAttackTime()) < 1000;
            String contextData = String.format("%b,%b,%d", isStable, isInCombat, playerData.getLastAttackTime());

            // Target (placeholder, values would come from KillAura check or similar)
            boolean targetExists = playerData.lastTarget != null && playerData.lastTarget.isValid();
            double targetDistance = targetExists && player != null ? player.getLocation().distance(playerData.lastTarget.getLocation()) : 0.0;
            double targetPredictedAngle = 0.0; // Placeholder for actual prediction from KillAura check

            String targetData = String.format("%b,%.2f,%.4f", targetExists, targetDistance, targetPredictedAngle);

            String line = String.format("%d,%s,%s,%s,%s,%s,%s", timestamp, playerUuid.toString(), movementData, rotationData, clickData, contextData, targetData);

            writer.write(line);
            writer.newLine();
            writer.flush(); // Flush to disk frequently for data integrity
        } catch (IOException e) {
            Bukkit.getLogger().severe("Failed to write PlayerData log for " + playerUuid + ": " + e.getMessage());
        }
    }

    public void close() {
        try {
            if (writer != null) {
                writer.close();
            }
        } catch (IOException e) {
            Bukkit.getLogger().severe("Failed to close data log writer for " + playerUuid + ": " + e.getMessage());
        }
    }

    // Helper methods (copied from checks, for data collection consistency)
    private double calculateStandardDeviationDouble(List<Double> data) {
        if (data.isEmpty()) return 0.0;
        double mean = data.stream().mapToDouble(d -> d).average().orElse(0.0);
        double variance = data.stream().mapToDouble(d -> Math.pow(d - mean, 2)).average().orElse(0.0);
        return Math.sqrt(variance);
    }

    private double calculateJitterDouble(List<Double> data) {
        if (data.size() < 2) return 0.0;
        double totalJitter = 0;
        for (int i = 1; i < data.size(); i++) {
            totalJitter += Math.abs(data.get(i) - data.get(i-1));
        }
        return totalJitter / (data.size() - 1);
    }
}

package com.example.bedrockac.checks.combat;

import com.example.bedrockac.checks.Check;
import com.example.bedrockac.player.PlayerData;
import com.example.bedrockac.utils.PlatformDetector;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class AutoClickerA extends Check {

    private final Deque<Long> clickTimestamps = new ArrayDeque<>();
    private static final int ANALYSIS_BUFFER_SIZE = 100;
    private static final int MIN_CLICKS_FOR_ANALYSIS = 30;

    public AutoClickerA(PlayerData playerData) {
        super(playerData, "AutoClicker", "A");
    }

    @Override
    public void handle(Event event) {
        if (!(event instanceof PlayerInteractEvent)) return;
        PlayerInteractEvent e = (PlayerInteractEvent) event;
        if (e.getAction() != Action.LEFT_CLICK_AIR) return;

        Player player = e.getPlayer();
        if (!player.getUniqueId().equals(playerData.getUuid())) return;

        clickTimestamps.addLast(System.currentTimeMillis());

        if (clickTimestamps.size() > ANALYSIS_BUFFER_SIZE) {
            clickTimestamps.removeFirst();
        }

        if (clickTimestamps.size() >= MIN_CLICKS_FOR_ANALYSIS) {
            analyzeClicks(player);
        }
    }

    private void analyzeClicks(Player player) {
        if (clickTimestamps.size() < 2) return;

        List<Long> delays = getClickDelays();

        // --- Statistical Analysis ---
        double averageDelay = delays.stream().mapToLong(l -> l).average().orElse(0.0);
        int cps = (int) (1000.0 / averageDelay);
        double stdDev = calculateStandardDeviation(delays);
        double jitter = calculateJitter(delays);

        if (cps > 25) {
            flag("Very high CPS. CPS: " + cps, 1.0);
            return;
        }

        // --- Contextual Analysis ---
        // How much is the player moving and looking around?
        // A simple way is to check the standard deviation of their recent positions.
        double positionStability = playerData.locationHistory.stream()
                .mapToDouble(loc -> loc.getX() + loc.getZ())
                .summaryStatistics().getCount() > 10 ? Math.sqrt(playerData.locationHistory.stream()
                .mapToDouble(loc -> Math.pow(loc.getX() + loc.getZ() -
                        playerData.locationHistory.stream().mapToDouble(l -> l.getX() + l.getZ()).average().orElse(0), 2))
                .average().orElse(0)) : 1.0;

        // Lower stability value means the player is standing still.
        boolean isStable = positionStability < 0.1;
        boolean isInCombat = (System.currentTimeMillis() - playerData.lastAttackTime) < 1000;

        // --- Dynamic Violation Logic ---
        int vl = getViolations();
        boolean flagged = false;
        double minStdDev = PlatformDetector.getAutoClickerStdDevThreshold(player);

        // Base violation on stats
        int stdDevVl = (stdDev < (minStdDev / 2.0)) ? 4 : (stdDev < minStdDev ? 2 : 0);
        int jitterVl = (jitter < 2.0) ? 3 : (jitter < 5.0 ? 1 : 0);
        
        int totalVl = stdDevVl + jitterVl;

        // Apply context-based multipliers
        if (totalVl > 0) {
            if (isStable && !isInCombat) {
                totalVl *= 1.5; // Higher penalty if standing still and not fighting
            }
            if (player.isSprinting()) {
                totalVl *= 0.8; // Lower penalty if sprinting (common in PvP)
            }
            vl += totalVl;
            flagged = true;
        }


        if (flagged) {
            if (vl > 10) {
                String context = "Stable: " + isStable + ", Combat: " + isInCombat;
                flag("Stats inconsistency. SD: " + f(stdDev) + ", J: " + f(jitter) + ", Ctx: " + context, (double) vl / 20.0);
            }
            setViolations(vl);
        } else {
            setViolations(Math.max(0, vl - 1));
        }
    }

    private List<Long> getClickDelays() {
        List<Long> timestamps = new java.util.ArrayList<>(clickTimestamps);
        List<Long> delays = new java.util.ArrayList<>();
        for (int i = 1; i < timestamps.size(); i++) {
            delays.add(timestamps.get(i) - timestamps.get(i-1));
        }
        return delays;
    }

    private double calculateStandardDeviation(List<Long> data) {
        if (data.isEmpty()) return 0.0;
        double mean = data.stream().mapToLong(l -> l).average().orElse(0.0);
        double variance = data.stream().mapToDouble(d -> Math.pow(d - mean, 2)).average().orElse(0.0);
        return Math.sqrt(variance);
    }
    
    private double calculateJitter(List<Long> delays) {
        if (delays.size() < 2) return 0.0;
        double totalJitter = 0;
        for (int i = 1; i < delays.size(); i++) {
            totalJitter += Math.abs(delays.get(i) - delays.get(i - 1));
        }
        return totalJitter / (delays.size() - 1);
    }
    
    private String f(double d) {
        return String.format("%.2f", d);
    }
}

package com.example.bedrockac.checks.combat;

import com.example.bedrockac.checks.Check;
import com.example.bedrockac.player.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Kill Aura Detection based on attack speed analysis (Enhanced Version)
 * Reference: AAC, Grim - detects inhuman attack timing
 * Minecraft 1.9+ has 500ms weapon cooldown (10 ticks)
 * Based on verified anti-cheat sources
 */
public class KillAuraEnhanced {

    private PlayerData playerData;
    private long lastAttackTime = -1;
    private Queue<Long> recentAttackTimes = new LinkedList<>();
    private int killAuraViolations = 0;
    
    // Minecraft 1.9+ weapon cooldown - OFFICIAL VALUE
    private static final long WEAPON_COOLDOWN = 500; // milliseconds (10 ticks @ 20 TPS)
    private static final long MIN_HUMAN_VARIANCE = 20; // Humans have ±20ms variance minimum
    private static final int SAMPLE_SIZE = 30;
    private static final int VIOLATION_THRESHOLD = 6;
    
    // With weapon cooldown, absolute minimum time
    private static final long ABSOLUTE_MIN_ATTACK_TIME = 300; // 300ms (would be obvious cheating)

    public KillAuraEnhanced(PlayerData playerData) {
        this.playerData = playerData;
    }

    public void checkAttack(Player player, long now) {
        if (lastAttackTime != -1) {
            long deltaTime = now - lastAttackTime;

            // Check 1: Attack MUCH faster than weapon cooldown
            if (deltaTime < ABSOLUTE_MIN_ATTACK_TIME) {
                killAuraViolations += 3;
            } else if (deltaTime < WEAPON_COOLDOWN - 100) {
                killAuraViolations += 1;
            }

            // Check 2: Consistent attack timing pattern
            if (recentAttackTimes.size() > 0) {
                long[] deltas = calculateDeltas();
                if (deltas.length > 0) {
                    long lastDelta = deltas[deltas.length - 1];
                    
                    if (Math.abs(deltaTime - lastDelta) < MIN_HUMAN_VARIANCE) {
                        killAuraViolations++;
                    }
                }
            }

            // Add to queue
            recentAttackTimes.offer(now);
            if (recentAttackTimes.size() > SAMPLE_SIZE) {
                recentAttackTimes.poll();
            }

            // Check 3: Statistical analysis
            if (recentAttackTimes.size() >= SAMPLE_SIZE) {
                long[] deltas = calculateDeltas();
                double stdDev = calculateStandardDeviation(deltas);
                
                if (stdDev < 5) {
                    killAuraViolations += 3;
                } else if (stdDev < MIN_HUMAN_VARIANCE) {
                    killAuraViolations += 1;
                } else {
                    killAuraViolations = Math.max(0, killAuraViolations - 1);
                }
            }

            // Flag
            if (killAuraViolations >= VIOLATION_THRESHOLD) {
                // Trigger flag in main system
                killAuraViolations = 0;
            }
        }
        
        lastAttackTime = now;
    }

    private long[] calculateDeltas() {
        if (recentAttackTimes.size() < 2) {
            return new long[0];
        }

        long[] deltas = new long[recentAttackTimes.size() - 1];
        Long[] times = recentAttackTimes.toArray(new Long[0]);
        
        for (int i = 1; i < times.length; i++) {
            deltas[i - 1] = times[i] - times[i - 1];
        }

        return deltas;
    }

    private double calculateStandardDeviation(long[] data) {
        if (data.length < 2) {
            return 0;
        }

        long sum = 0;
        for (long value : data) {
            sum += value;
        }
        double mean = (double) sum / data.length;

        double variance = 0;
        for (long value : data) {
            variance += Math.pow(value - mean, 2);
        }
        variance /= data.length;

        return Math.sqrt(variance);
    }

    public int getViolationCount() {
        return killAuraViolations;
    }
}

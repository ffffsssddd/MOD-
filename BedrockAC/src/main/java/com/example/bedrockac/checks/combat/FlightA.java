package com.example.bedrockac.checks.combat;

import com.example.bedrockac.checks.Check;
import com.example.bedrockac.player.PlayerData;
import com.example.bedrockac.utils.PlatformDetector;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffectType;

public class FlightA extends Check {

    private double lastDeltaY = 0.0;

    // Constants from research
    private static final double GRAVITY_ACCELERATION = -0.08;
    private static final double SLOW_FALLING_MODIFIER = -0.01;

    public FlightA(PlayerData playerData) {
        super(playerData, "Flight", "A");
    }

    @Override
    public void handle(Event event) {
        if (!(event instanceof PlayerMoveEvent)) {
            return;
        }

        PlayerMoveEvent e = (PlayerMoveEvent) event;
        Player player = e.getPlayer();

        // Basic exemptions from player state
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR
                || player.getAllowFlight() || player.isFlying() || player.isGliding() || player.isInsideVehicle()) {
            return;
        }

        // Contextual exemptions from PlayerData
        if (playerData.ticksSinceTeleport < 3 || playerData.ticksSinceVelocity < 3 || playerData.isClimbing
                || playerData.inWeb || playerData.onSlime) {
            return;
        }

        // Don't check on ground
        if (playerData.groundTicks > 0) {
            lastDeltaY = 0.0; // Reset velocity when on ground
            return;
        }

        // After the first few ticks in the air, start checking
        if (playerData.airTicks > 4) {
            double expectedDeltaY = (lastDeltaY + GRAVITY_ACCELERATION) * 0.98;

            // Potion Effects Adjustments
            if (player.hasPotionEffect(PotionEffectType.JUMP)) {
                // A more advanced implementation could calculate the exact jump boost height
                // and verify it over the full arc of the jump. For now, we exempt it.
                return;
            }
            if (player.hasPotionEffect(PotionEffectType.LEVITATION)) {
                int level = player.getPotionEffect(PotionEffectType.LEVITATION).getAmplifier() + 1;
                // Approximate the effect of levitation
                expectedDeltaY += (0.05 * level - (playerData.deltaY - 0.08)) / 2.0;
            }
            if (player.hasPotionEffect(PotionEffectType.SLOW_FALLING)) {
                // With slow falling, vertical movement should be consistently around -0.01
                expectedDeltaY = SLOW_FALLING_MODIFIER;
            }

            double tolerance = 0.025 + PlatformDetector.getFlightTolerance(player);
            double deviation = Math.abs(playerData.deltaY - expectedDeltaY);

            // A jump from ground will have a high initial deltaY. We allow this.
            boolean isJumping = playerData.groundTicks == 0 && lastDeltaY == 0.0 && playerData.deltaY > 0;

            if (deviation > tolerance && !isJumping) {
                int vl = getViolations() + 1;
                setViolations(vl);

                if (vl > 5) { // Threshold for flagging
                    flag("Gravity inconsistency. Dev: " + f(deviation) + ", AT: " + playerData.airTicks,
                            (double) vl / 10.0);
                }
            } else {
                // Decay violations if the player is behaving correctly
                setViolations(Math.max(0, getViolations() - 1));
            }
        }

        this.lastDeltaY = playerData.deltaY;
    }

    private String f(double d) {
        return String.format("%.3f", d);
    }
}

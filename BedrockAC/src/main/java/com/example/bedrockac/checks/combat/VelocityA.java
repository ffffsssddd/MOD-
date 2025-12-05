package com.example.bedrockac.checks.combat;

import com.example.bedrockac.checks.Check;
import com.example.bedrockac.player.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

public class VelocityA extends Check {

    // Approximate friction factor for horizontal velocity (varies by block and state)
    private static final double EXPECTED_HORIZONTAL_FRICTION = 0.91; 
    // Gravity application per tick (approximate)
    private static final double GRAVITY_PER_TICK = 0.08;
    // Terminal velocity for falling
    private static final double TERMINAL_VELOCITY = -3.92;


    public VelocityA(PlayerData playerData) {
        super(playerData, "Velocity", "A");
    }

    @Override
    public void handle(Event event) {
        if (event instanceof EntityDamageEvent) {
            EntityDamageEvent e = (EntityDamageEvent) event;
            if (!(e.getEntity() instanceof Player)) {
                return;
            }

            Player player = (Player) e.getEntity();
            if (!player.getUniqueId().equals(playerData.getUuid())) {
                return;
            }

            // When player takes damage, reset ticksSinceVelocity to ensure fresh check
            playerData.ticksSinceVelocity = 0; 

        } else if (event instanceof PlayerMoveEvent) {
            PlayerMoveEvent e = (PlayerMoveEvent) event;
            Player player = e.getPlayer();

            if (!player.getUniqueId().equals(playerData.getUuid())) {
                return;
            }

            // Only check if player recently received velocity (e.g., knockback from damage)
            if (playerData.lastVelocity == null || playerData.ticksSinceVelocity > 40) { // 2 seconds window
                return;
            }
            
            // PlayerData.ticksSinceVelocity is in server ticks (50ms per tick)
            double ticksPassed = playerData.ticksSinceVelocity;

            // Clone to avoid modifying the original stored velocity
            Vector expectedVelocity = playerData.lastVelocity.clone(); 
            
            // Apply horizontal friction for each tick
            for (int i = 0; i < ticksPassed; i++) {
                expectedVelocity.setX(expectedVelocity.getX() * EXPECTED_HORIZONTAL_FRICTION);
                expectedVelocity.setZ(expectedVelocity.getZ() * EXPECTED_HORIZONTAL_FRICTION); // Corrected to getZ
            }
            
            // Apply gravity if airborne and not on ground (and not currently being pushed by external forces like pistons/water)
            if (!player.isOnGround() && !playerData.isClimbing && !playerData.inWeb && !player.isFlying()) { // Added more context
                // Apply gravity for each tick passed
                expectedVelocity.setY(expectedVelocity.getY() - (GRAVITY_PER_TICK * ticksPassed));
                // Clamp to terminal velocity
                if (expectedVelocity.getY() < TERMINAL_VELOCITY) {
                    expectedVelocity.setY(TERMINAL_VELOCITY);
                }
            } else {
                // On ground, or in special state, y velocity should quickly approach 0, or be 0.
                // This is a simplification; more complex logic needed for water/lava, etc.
                expectedVelocity.setY(0);
            }

            // Get actual motion vector
            Vector actualMotion = e.getTo().toVector().subtract(e.getFrom().toVector());

            // --- Compare Expected vs Actual Motion ---
            // This comparison needs to be robust. Use a tolerance and compare magnitudes or angles.
            double differenceTolerance = 0.05; // Acceptable difference in motion

            double horizontalExpected = Math.sqrt(Math.pow(expectedVelocity.getX(), 2) + Math.pow(expectedVelocity.getZ(), 2));
            double horizontalActual = Math.sqrt(Math.pow(actualMotion.getX(), 2) + Math.pow(actualMotion.getZ(), 2));
            
            // Check for horizontal velocity reduction (e.g., Anti-KB)
            if (horizontalExpected > horizontalActual + differenceTolerance) {
                flag("Velocity KB Horizontal Reduction: Exp=" + f(horizontalExpected) +
                     " Act=" + f(horizontalActual), 0.7);
            }

            // Check for vertical velocity reduction/manipulation
            if (expectedVelocity.getY() > 0.1 && actualMotion.getY() < expectedVelocity.getY() - differenceTolerance && !player.isOnGround()) {
                flag("Velocity KB Vertical Reduction (Up): ExpY=" + f(expectedVelocity.getY()) +
                     " ActY=" + f(actualMotion.getY()), 0.7);
            }
            if (expectedVelocity.getY() < -0.1 && actualMotion.getY() > expectedVelocity.getY() + differenceTolerance && !player.isOnGround()) {
                flag("Velocity KB Vertical Manipulation (Down): ExpY=" + f(expectedVelocity.getY()) +
                     " ActY=" + f(actualMotion.getY()), 0.7);
            }
        }
    }

    private String f(double d) {
        return String.format("%.2f", d);
    }
}

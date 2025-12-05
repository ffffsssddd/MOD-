package com.example.bedrockac.checks.combat;

import com.example.bedrockac.checks.Check;
import com.example.bedrockac.player.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class CriticalsA extends Check {

    public CriticalsA(PlayerData playerData) {
        super(playerData, "Criticals", "A");
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

        // Check if the damage was critical by checking if player is on ground
        // A player can only crit if they are falling (not on ground, not climbing, etc.)
        // This check focuses on the common "criticals on ground" hack.
        if (player.isOnGround()) {
            // If player is on ground but still attacking, they shouldn't get critical hits
            // Check if they're trying to perform a critical attack while on ground
            double damage = e.getDamage();
            // Critical hits typically deal extra damage (1.5x normal)
            if (damage > 4.0) { // Average damage is ~3, criticals would be ~4.5+
                flag("Possible Crit on Ground - damage=" + String.format("%.2f", damage));
            }
        }
    }
}

package com.example.bedrockac.checks.combat;

import com.example.bedrockac.checks.Check;
import com.example.bedrockac.player.PlayerData;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.block.Action;

public class FastBowA extends Check {

    private long bowStartTime = -1;

    public FastBowA(PlayerData playerData) {
        super(playerData, "FastBow", "A");
    }

    @Override
    public void handle(Event event) {
        if (event instanceof PlayerInteractEvent) {
            PlayerInteractEvent e = (PlayerInteractEvent) event;
            Player player = e.getPlayer();

            if (!player.getUniqueId().equals(playerData.getUuid())) {
                return;
            }

            // Only track right-click interactions for bows
            if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                ItemStack item = e.getItem();
                if (item != null && item.getType() == Material.BOW) {
                    // Player started using a bow
                    bowStartTime = System.currentTimeMillis();
                }
            }

        } else if (event instanceof EntityShootBowEvent) {
            EntityShootBowEvent e = (EntityShootBowEvent) event;
            if (!(e.getEntity() instanceof Player)) {
                return;
            }

            Player player = (Player) e.getEntity();
            if (!player.getUniqueId().equals(playerData.getUuid())) {
                return;
            }

            if (bowStartTime != -1) {
                long chargeTime = System.currentTimeMillis() - bowStartTime;

                // A fully charged bow takes approximately 1.0 second (20 ticks).
                // A basic check: if shot significantly faster than a full charge.
                // This threshold requires tuning and consideration of server TPS, latency, etc.
                if (chargeTime < 800) { // e.g., less than 0.8 seconds
                    flag("FastBow: ChargeTime=" + chargeTime + "ms");
                }
                bowStartTime = -1; // Reset after shot
            }
        }
    }
}

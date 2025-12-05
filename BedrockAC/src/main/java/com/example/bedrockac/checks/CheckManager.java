package com.example.bedrockac.checks;

import com.example.bedrockac.checks.combat.AimAssistA;
import com.example.bedrockac.checks.combat.AutoClickerA;
import com.example.bedrockac.checks.combat.CriticalsA;
import com.example.bedrockac.checks.combat.FastBowA;
import com.example.bedrockac.checks.combat.FlightA;
import com.example.bedrockac.checks.combat.KillAuraA;
import com.example.bedrockac.checks.combat.ReachA;
import com.example.bedrockac.checks.combat.VelocityA;
import com.example.bedrockac.player.PlayerData;
import org.bukkit.event.Event;
import java.util.ArrayList;
import java.util.List;

public class CheckManager {

    private final List<Check> checks = new ArrayList<>();

    public CheckManager(PlayerData playerData) {
        // Register all checks here
        checks.add(new KillAuraA(playerData));
        checks.add(new ReachA(playerData));
        checks.add(new AutoClickerA(playerData));
        checks.add(new VelocityA(playerData));
        checks.add(new AimAssistA(playerData));
        checks.add(new CriticalsA(playerData));
        checks.add(new FastBowA(playerData));
        checks.add(new FlightA(playerData));
    }

    public void handle(Event event) {
        for (Check check : checks) {
            check.handle(event);
        }
    }
    
    public List<Check> getChecks() {
        return checks;
    }
}

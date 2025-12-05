package com.example.bedrockac.checks;

import com.example.bedrockac.player.PlayerData;
import com.example.bedrockac.utils.ViolationLevel;
import com.example.bedrockac.utils.AlertSystem;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

public abstract class Check {

    protected final PlayerData playerData;
    private final String name;
    private final String type;
    private int violations;
    private int violationPoints = 0;

    public Check(PlayerData playerData, String name, String type) {
        this.playerData = playerData;
        this.name = name;
        this.type = type;
    }

    public abstract void handle(Event event);

    /**
     * العلم مع نظام تصنيف الانتهاكات
     */
    protected void flag(String debugInfo) {
        flag(debugInfo, 1.0);
    }

    /**
     * العلم مع مستوى الخطورة (من 0.0 إلى 1.0)
     */
    protected void flag(String debugInfo, double severity) {
        this.violations++;
        
        // حساب مستوى الخطورة
        ViolationLevel.Level level = ViolationLevel.calculateViolationLevel(
            name, 
            severity, 
            violations
        );
        
        violationPoints += level.getPoints();
        playerData.addViolation(name + " " + type);
        
        // الحصول على اللاعب
        Player player = Bukkit.getPlayer(playerData.getUuid());
        
        // إرسال الإشعار إلى الـ OP
        if (player != null) {
            AlertSystem.alertOPs(player, name + " " + type, debugInfo, severity);
        }
        
        // تسجيل مع مستوى الخطورة
        String severityStr = String.format("%.0f%%", severity * 100);
        System.out.println("[" + level.name() + "] Flagged " + playerData.getUuid() + 
                          " for " + name + " " + type + 
                          " (Violations: " + violations + ", Points: " + violationPoints + 
                          ", Severity: " + severityStr + ") " + debugInfo);
        
        // تحذير إذا لزم الأمر
        if (ViolationLevel.shouldWarn(violationPoints) && player != null) {
            AlertSystem.alertWarning(player, violationPoints);
        }
        
        // حظر إذا لزم الأمر
        if (ViolationLevel.shouldBan(violationPoints) && player != null) {
            AlertSystem.alertBan(player, violationPoints);
        }
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }
    
    public int getViolations() {
        return violations;
    }
    
    public int getViolationPoints() {
        return violationPoints;
    }
    
    public void setViolations(int violations) {
        this.violations = violations;
    }
}

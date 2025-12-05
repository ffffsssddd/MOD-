package com.example.bedrockac.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * نظام الإشعارات المتقدم - Bedrock Only
 * مصادر موثوقة: Grim, AAC, Spartan, Matrix
 * فقط لـ Bedrock Players - لا Java
 */
public class AlertSystem {

    private static final String PREFIX = ChatColor.DARK_RED + "▌ BedrockAC" + ChatColor.RESET;
    private static final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
    
    // Severity thresholds
    private static final double CRITICAL_THRESHOLD = 0.90;  // Ban immediately
    private static final double HIGH_THRESHOLD = 0.75;      // High suspicion
    private static final double MEDIUM_THRESHOLD = 0.50;    // Medium suspicion
    private static final double LOW_THRESHOLD = 0.30;       // Low suspicion

    /**
     * إرسال تنبيه فوري للـ OP - BEDROCK ONLY
     * مثل AAC و Grim في دقتها
     */
    public static void alertOPs(Player suspectedPlayer, String checkName, String details, double severity) {
        // تجاهل تماماً Java players
        if (!PlatformDetector.isBedrock(suspectedPlayer)) {
            return;
        }
        
        String timestamp = timeFormat.format(new Date());
        String severityLabel = getSeverityLabel(severity);
        String severityColor = getSeverityColor(severity);
        
        // تنسيق الرسالة بناءً على Grim و AAC standards
        String message = PREFIX + severityColor + " [" + severityLabel + "]" + ChatColor.RESET + 
                        ChatColor.GRAY + " [" + timestamp + "] " +
                        ChatColor.WHITE + suspectedPlayer.getName() + 
                        ChatColor.GRAY + ": " +
                        ChatColor.YELLOW + checkName + 
                        ChatColor.GRAY + " - " + details;
        
        // بث إلى جميع الـ OP - مرة واحدة فقط
        broadcastToOPs(message);
    }

    /**
     * تحذير عند تجاوز نقطة معينة - BEDROCK ONLY
     */
    public static void alertWarning(Player suspectedPlayer, int violationPoints) {
        if (!PlatformDetector.isBedrock(suspectedPlayer)) {
            return;
        }
        
        String message = PREFIX + ChatColor.YELLOW + " ⚠️  تحذير" + ChatColor.RESET +
                        ChatColor.GRAY + " | " +
                        ChatColor.WHITE + suspectedPlayer.getName() + 
                        ChatColor.GRAY + " | نقاط: " + ChatColor.RED + violationPoints +
                        ChatColor.GRAY + "/100";
        
        broadcastToOPs(message);
    }

    /**
     * إشعار الحظر الموصى به - BEDROCK ONLY
     * كما في Spartan و Matrix
     */
    public static void alertBan(Player suspectedPlayer, int violationPoints) {
        if (!PlatformDetector.isBedrock(suspectedPlayer)) {
            return;
        }
        
        String message = PREFIX + ChatColor.DARK_RED + " 🚫 تنبيه الحظر" + ChatColor.RESET +
                        ChatColor.GRAY + " | " +
                        ChatColor.RED + suspectedPlayer.getName() + 
                        ChatColor.GRAY + " | نقاط: " + ChatColor.DARK_RED + violationPoints + "/100" +
                        ChatColor.GRAY + " [أوصى بـ: /ban " + suspectedPlayer.getName() + "]";
        
        broadcastToOPs(message);
    }

    /**
     * تقرير مفصل للـ OP - BEDROCK ONLY
     * مثل تقارير Grim المفصلة
     */
    public static void sendDetailedReport(Player opPlayer, Player suspectedPlayer, 
                                          String checkName, String details, 
                                          int violations, int violationPoints) {
        if (!PlatformDetector.isBedrock(suspectedPlayer)) {
            return;
        }
        
        // رأس التقرير
        opPlayer.sendMessage(ChatColor.DARK_GRAY + "╔═══════════════════════════════════════════════════════╗");
        opPlayer.sendMessage(PREFIX + ChatColor.YELLOW + " تقرير كشف الغش");
        opPlayer.sendMessage(ChatColor.DARK_GRAY + "╠═══════════════════════════════════════════════════════╣");
        
        // المعلومات الأساسية
        opPlayer.sendMessage(ChatColor.WHITE + "│ " + ChatColor.YELLOW + "اللاعب:" + ChatColor.RESET + 
                           ChatColor.WHITE + " " + suspectedPlayer.getName() + 
                           ChatColor.GRAY + " [Bedrock Edition]");
        opPlayer.sendMessage(ChatColor.WHITE + "│ " + ChatColor.YELLOW + "الفحص:" + ChatColor.RESET + 
                           ChatColor.WHITE + " " + checkName);
        opPlayer.sendMessage(ChatColor.WHITE + "│ " + ChatColor.YELLOW + "التفاصيل:" + ChatColor.RESET + 
                           ChatColor.GRAY + " " + details);
        opPlayer.sendMessage(ChatColor.WHITE + "│ " + ChatColor.YELLOW + "الانتهاكات:" + ChatColor.RESET + 
                           ChatColor.RED + " " + violations);
        opPlayer.sendMessage(ChatColor.WHITE + "│ " + ChatColor.YELLOW + "إجمالي النقاط:" + ChatColor.RESET + 
                           ChatColor.RED + " " + violationPoints + "/100");
        
        // معلومات إضافية
        String confidenceLabel = getConfidenceLabel(violationPoints);
        String confidenceColor = getConfidenceColor(violationPoints);
        opPlayer.sendMessage(ChatColor.WHITE + "│ " + ChatColor.YELLOW + "درجة الثقة:" + ChatColor.RESET + 
                           confidenceColor + " " + confidenceLabel);
        
        // الأوامر المقترحة
        opPlayer.sendMessage(ChatColor.DARK_GRAY + "╠═══════════════════════════════════════════════════════╣");
        opPlayer.sendMessage(ChatColor.WHITE + "│ " + ChatColor.GRAY + "الأوامر: " +
                           ChatColor.GREEN + "/check " + suspectedPlayer.getName() + 
                           ChatColor.GRAY + " | " +
                           ChatColor.RED + "/ban " + suspectedPlayer.getName());
        
        opPlayer.sendMessage(ChatColor.DARK_GRAY + "╚═══════════════════════════════════════════════════════╝");
    }

    /**
     * رسالة خطأ - للـ OP فقط
     */
    public static void alertError(String errorMessage) {
        String message = PREFIX + ChatColor.RED + " ❌ خطأ" + ChatColor.RESET + 
                        ChatColor.GRAY + ": " + ChatColor.WHITE + errorMessage;
        
        for (Player op : Bukkit.getOnlinePlayers()) {
            if (op.isOp()) {
                op.sendMessage(message);
            }
        }
        Bukkit.getLogger().warning(ChatColor.stripColor(message));
    }

    /**
     * حصول لون بناءً على مستوى الخطورة
     * معايير موثوقة من Grim و AAC
     */
    private static String getSeverityColor(double severity) {
        if (severity >= CRITICAL_THRESHOLD) {
            return ChatColor.DARK_RED.toString();
        } else if (severity >= HIGH_THRESHOLD) {
            return ChatColor.RED.toString();
        } else if (severity >= MEDIUM_THRESHOLD) {
            return ChatColor.GOLD.toString();
        } else {
            return ChatColor.YELLOW.toString();
        }
    }

    /**
     * حصول تسمية الخطورة
     */
    private static String getSeverityLabel(double severity) {
        if (severity >= CRITICAL_THRESHOLD) {
            return "حرج";
        } else if (severity >= HIGH_THRESHOLD) {
            return "عالي";
        } else if (severity >= MEDIUM_THRESHOLD) {
            return "متوسط";
        } else {
            return "منخفض";
        }
    }

    /**
     * حصول تسمية درجة الثقة
     */
    private static String getConfidenceLabel(int violationPoints) {
        if (violationPoints >= 90) {
            return "مؤكد - حظر فوري";
        } else if (violationPoints >= 75) {
            return "عالي جداً - تحقق الآن";
        } else if (violationPoints >= 50) {
            return "متوسط - مراقبة";
        } else if (violationPoints >= 30) {
            return "منخفض - تتبع";
        } else {
            return "آمن";
        }
    }

    /**
     * حصول لون درجة الثقة
     */
    private static String getConfidenceColor(int violationPoints) {
        if (violationPoints >= 90) {
            return ChatColor.DARK_RED + "مؤكد";
        } else if (violationPoints >= 75) {
            return ChatColor.RED + "عالي";
        } else if (violationPoints >= 50) {
            return ChatColor.GOLD + "متوسط";
        } else if (violationPoints >= 30) {
            return ChatColor.YELLOW + "منخفض";
        } else {
            return ChatColor.GREEN + "آمن";
        }
    }

    /**
     * بث الرسالة إلى جميع الـ OP - مرة واحدة فقط
     */
    private static void broadcastToOPs(String message) {
        for (Player op : Bukkit.getOnlinePlayers()) {
            if (op.isOp()) {
                op.sendMessage(message);
            }
        }
        // تسجيل في الكونسول
        Bukkit.getLogger().info(ChatColor.stripColor(message));
    }
}

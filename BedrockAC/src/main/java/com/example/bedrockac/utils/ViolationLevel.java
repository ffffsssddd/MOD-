package com.example.bedrockac.utils;

/**
 * Violation Rating System - يحدد مستوى الخطورة لكل انتهاك
 * يساعد على تقليل التنبيهات الكاذبة
 */
public class ViolationLevel {

    /**
     * مستويات الانتهاكات
     */
    public enum Level {
        LOW(1),           // تنبيه عادي
        MEDIUM(3),        // مريب جداً
        HIGH(5),          // غش محتمل جداً
        CRITICAL(10);     // حظر فوري

        private final int points;

        Level(int points) {
            this.points = points;
        }

        public int getPoints() {
            return points;
        }
    }

    /**
     * حساب مستوى الخطورة بناءً على عدة عوامل
     */
    public static Level calculateViolationLevel(String checkName, double deviation, int consistentViolations) {
        // Flight Detection
        if (checkName.equals("Flight")) {
            if (consistentViolations >= 10) {
                return Level.CRITICAL; // طيران واضح
            } else if (consistentViolations >= 5) {
                return Level.HIGH; // طيران محتمل
            } else if (consistentViolations >= 3) {
                return Level.MEDIUM;
            } else {
                return Level.LOW;
            }
        }

        // Reach Detection
        if (checkName.equals("Reach")) {
            if (deviation > 1.0) {
                return Level.CRITICAL; // مسافة طويلة جداً
            } else if (deviation > 0.5) {
                return Level.HIGH;
            } else if (deviation > 0.3) {
                return Level.MEDIUM;
            } else {
                return Level.LOW;
            }
        }

        // AutoClicker Detection
        if (checkName.equals("AutoClicker")) {
            if (consistentViolations >= 3) {
                return Level.CRITICAL; // نقرات آلية واضحة
            } else if (consistentViolations >= 2) {
                return Level.HIGH;
            } else {
                return Level.MEDIUM;
            }
        }

        // Kill Aura Detection
        if (checkName.equals("KillAura")) {
            if (consistentViolations >= 5) {
                return Level.CRITICAL;
            } else if (consistentViolations >= 3) {
                return Level.HIGH;
            } else {
                return Level.MEDIUM;
            }
        }

        // Default
        return Level.LOW;
    }

    /**
     * تحديد ما إذا كان يجب حظر اللاعب
     */
    public static boolean shouldBan(int totalViolationPoints) {
        return totalViolationPoints >= 30; // 3 Critical violations أو ما يعادله
    }

    /**
     * تحديد ما إذا كان يجب تحذير اللاعب
     */
    public static boolean shouldWarn(int totalViolationPoints) {
        return totalViolationPoints >= 15; // تحذير عند 15 نقطة
    }

    /**
     * تحديد ما إذا كان يجب حفظ البيانات
     */
    public static boolean shouldLog(Level level) {
        return level.getPoints() >= Level.MEDIUM.getPoints();
    }
}

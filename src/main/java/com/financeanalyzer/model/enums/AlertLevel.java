package com.financeanalyzer.model.enums;

import com.financeanalyzer.util.ConfigLoader;

import java.math.BigDecimal;

/**
 * Categorical alert severity levels for budget utilization thresholds.
 * Thresholds (default 80% and 100%) are configurable from app.properties.
 */
public enum AlertLevel {
    NORMAL("Normal", "#2E7D32"),      // Forest Green
    WARNING("Warning", "#F57C00"),    // Amber / Orange
    EXCEEDED("Exceeded", "#C62828");  // Crimson Red

    private final String displayName;
    private final String colorHex;

    AlertLevel(String displayName, String colorHex) {
        this.displayName = displayName;
        this.colorHex = colorHex;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColorHex() {
        return colorHex;
    }

    /**
     * Determines the AlertLevel based on the budget utilisation percentage.
     *
     * @param utilisation percentage utilisation (e.g. 85.5)
     * @return AlertLevel (NORMAL, WARNING, or EXCEEDED)
     */
    public static AlertLevel fromUtilisation(BigDecimal utilisation) {
        if (utilisation == null) {
            return NORMAL;
        }

        double warningThreshold = ConfigLoader.getDoubleProperty("alert.warning.threshold", 80.0);
        double criticalThreshold = ConfigLoader.getDoubleProperty("alert.critical.threshold", 100.0);

        double val = utilisation.doubleValue();
        if (val >= criticalThreshold) {
            return EXCEEDED;
        } else if (val >= warningThreshold) {
            return WARNING;
        } else {
            return NORMAL;
        }
    }
}

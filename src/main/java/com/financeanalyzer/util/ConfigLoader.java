package com.financeanalyzer.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class responsible for loading configuration parameters from app.properties.
 * Demonstrates singleton pattern and static utility access.
 */
public final class ConfigLoader {

    private static final Logger LOGGER = Logger.getLogger(ConfigLoader.class.getName());
    private static final Properties PROPERTIES = new Properties();
    private static final String CONFIG_FILE = "/app.properties";

    static {
        loadProperties();
    }

    private ConfigLoader() {
        // Prevent instantiation
    }

    private static void loadProperties() {
        try (InputStream input = ConfigLoader.class.getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                LOGGER.log(Level.WARNING, "Unable to find " + CONFIG_FILE + ", using default configurations.");
                return;
            }
            PROPERTIES.load(input);
            LOGGER.log(Level.INFO, "Application configuration loaded successfully from {0}", CONFIG_FILE);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Failed to load properties file from " + CONFIG_FILE, ex);
        }
    }

    /**
     * Retrieves a configuration property as a string.
     *
     * @param key the property key
     * @param defaultValue default value if key is not found
     * @return the property value or defaultValue
     */
    public static String getProperty(String key, String defaultValue) {
        return PROPERTIES.getProperty(key, defaultValue);
    }

    /**
     * Retrieves a configuration property as an integer.
     *
     * @param key the property key
     * @param defaultValue default integer if key is missing or unparseable
     * @return parsed integer value
     */
    public static int getIntProperty(String key, int defaultValue) {
        String val = PROPERTIES.getProperty(key);
        if (val == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "Invalid integer for property {0}: {1}", new Object[]{key, val});
            return defaultValue;
        }
    }

    /**
     * Retrieves a configuration property as a double.
     *
     * @param key the property key
     * @param defaultValue default double if key is missing or unparseable
     * @return parsed double value
     */
    public static double getDoubleProperty(String key, double defaultValue) {
        String val = PROPERTIES.getProperty(key);
        if (val == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(val.trim());
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "Invalid double for property {0}: {1}", new Object[]{key, val});
            return defaultValue;
        }
    }
}

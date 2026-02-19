package com.assignment.scrapping.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Singleton configuration manager.
 * Loads config.properties for app settings.
 * Loads .env for sensitive credentials.
 * All other classes use this — never read files directly.
 */
public class ConfigManager {

    private static final Logger logger = LogManager.getLogger(ConfigManager.class);
    private static ConfigManager instance;

    private final Properties properties = new Properties();
    private final Dotenv dotenv;

    private ConfigManager() {
        loadProperties();
        dotenv = Dotenv.configure()
                .ignoreIfMissing()   // won't crash if .env absent in CI
                .load();
        logger.info("ConfigManager initialized successfully.");
    }

    public static synchronized ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    private void loadProperties() {
        try (InputStream input = getClass()
                .getClassLoader()
                .getResourceAsStream("config.properties")) {

            if (input == null) {
                throw new RuntimeException(
                        "config.properties not found in classpath.");
            }
            properties.load(input);
            logger.debug("config.properties loaded.");

        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to load config.properties", e);
        }
    }

    // ── Property Getters ─────────────────────────────

    /** Gets value from config.properties */
    public String get(String key) {
        String value = properties.getProperty(key);
        if (value == null) {
            logger.warn("Property '{}' not found in config.properties", key);
        }
        return value;
    }

    /** Gets value with a default fallback */
    public String get(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    /** Gets int value from config.properties */
    public int getInt(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            logger.warn("Invalid int for key '{}': '{}'. Using default: {}",
                    key, value, defaultValue);
            return defaultValue;
        }
    }

    // ── Credential Getters (from .env) ───────────────

    /** Gets value from .env file or System environment */
    public String getEnv(String key) {
        // Priority: System env (CI/CD) → .env file (local dev)
        String sysVal = System.getenv(key);
        if (sysVal != null && !sysVal.isEmpty()) return sysVal;

        String dotVal = dotenv.get(key, null);
        if (dotVal == null) {
            logger.warn("Env variable '{}' not found in .env or System env", key);
        }
        return dotVal;
    }

    // ── Convenience Methods ──────────────────────────

    public String getAppUrl()           { return get("app.url"); }
    public String getOpinionPath()      { return get("opinion.section.path"); }
    public int    getImplicitTimeout()  { return getInt("timeout.implicit", 10); }
    public int    getExplicitTimeout()  { return getInt("timeout.explicit", 20); }
    public int    getPageLoadTimeout()  { return getInt("timeout.page.load", 30); }
    public int    getArticleCount()     { return getInt("scrape.article.count", 5); }
    public String getArticlesDir()      { return get("output.articles.dir"); }
    public String getImagesDir()        { return get("output.images.dir"); }
    public String getTranslationSource(){ return get("translation.source.lang"); }
    public String getTranslationTarget(){ return get("translation.target.lang"); }
    public String getBsUsername()       { return getEnv("BROWSERSTACK_USERNAME"); }
    public String getBsAccessKey()      { return getEnv("BROWSERSTACK_ACCESS_KEY"); }
    public String getTranslationApiKey(){
        return getEnv("TRANSLATION_API_KEY"); }
    public String getRapidApiHost()     {
        return getEnv("RAPIDAPI_HOST"); }
}

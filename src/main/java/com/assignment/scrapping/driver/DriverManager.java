package com.assignment.scrapping.driver;

import com.assignment.scrapping.config.ConfigManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.PageLoadStrategy;

import java.net.URI;
import java.time.Duration;


public class DriverManager {

    private static final Logger logger = LogManager.getLogger(DriverManager.class);

    // One WebDriver instance per thread — the core thread-safety mechanism
    private static final ThreadLocal<WebDriver> driverThreadLocal = new ThreadLocal<>();

    private DriverManager() {}

    // ── Getter / Setter ──────────────────────────────

    public static WebDriver getDriver() {
        WebDriver driver = driverThreadLocal.get();
        if (driver == null) {
            throw new IllegalStateException(
                    "WebDriver not initialized for thread: "
                            + Thread.currentThread().getName()
                            + ". Call setDriver() in @BeforeMethod first.");
        }
        return driver;
    }

    public static void setDriver(WebDriver driver) {
        driverThreadLocal.set(driver);
        logger.info("Driver set for thread: {}", Thread.currentThread().getName());
    }


    public static WebDriver createLocalDriver(String browser) {
        logger.info("Creating local driver: {}", browser);

        WebDriver driver = switch (browser.toLowerCase().trim()) {
            case "chrome" -> {
                ChromeOptions options = new ChromeOptions();
                options.setPageLoadStrategy(PageLoadStrategy.EAGER);
                options.addArguments("--disable-blink-features=AutomationControlled");
                options.addArguments("--lang=es");       // keeps site in Spanish
                options.addArguments("--disable-notifications");
                options.addArguments("--no-sandbox");
                options.addArguments("--disable-dev-shm-usage");
                options.addArguments("--blink-settings=imagesEnabled=false");
                options.addArguments("--disable-extensions");
                yield new ChromeDriver(options);
            }
            case "firefox" -> {
                FirefoxOptions options = new FirefoxOptions();
                options.addPreference("intl.accept_languages", "es,es-ES");
                yield new FirefoxDriver(options);
            }
            case "safari" -> new SafariDriver();
            default -> throw new IllegalArgumentException(
                    "Unsupported browser for local execution: " + browser);
        };

        applyTimeouts(driver);
        return driver;
    }


    public static WebDriver createRemoteDriver(
            org.openqa.selenium.remote.DesiredCapabilities capabilities) {
        try {
            ConfigManager config = ConfigManager.getInstance();
            String hubUrl = config.get("browserstack.hub.url");

            logger.info("Connecting to BrowserStack hub: {}", hubUrl);
            WebDriver driver = new RemoteWebDriver(
                    URI.create(hubUrl).toURL(), capabilities);

            applyTimeouts(driver);
            logger.info("RemoteWebDriver created successfully.");
            return driver;

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to create BrowserStack RemoteWebDriver: "
                            + e.getMessage(), e);
        }
    }

    // ── Teardown ─────────────────────────────────────


    public static void quitDriver() {
        WebDriver driver = driverThreadLocal.get();
        if (driver != null) {
            try {
                driver.quit();
                logger.info("Driver quit for thread: {}",
                        Thread.currentThread().getName());
            } catch (Exception e) {
                logger.warn("Error quitting driver: {}", e.getMessage());
            } finally {
                // Always remove — even if quit() throws
                driverThreadLocal.remove();
            }
        }
    }

    // ── Helpers ──────────────────────────────────────

    private static void applyTimeouts(WebDriver driver) {
        ConfigManager config = ConfigManager.getInstance();
        driver.manage().timeouts()
                .implicitlyWait(Duration.ofSeconds(config.getImplicitTimeout()))
                .pageLoadTimeout(Duration.ofSeconds(config.getPageLoadTimeout()));
        try {
            String platformName = "";
            if (driver instanceof RemoteWebDriver) {
                Object platform = ((RemoteWebDriver) driver)
                        .getCapabilities().getCapability("platformName");
                if (platform != null) {
                    platformName = platform.toString().toLowerCase();
                }
            }
            boolean isMobile = platformName.equals("android")
                    || platformName.equals("ios");
            if (!isMobile) {
                driver.manage().window().maximize();
            } else {
                logger.info("Mobile device — skipping window maximize.");
            }
        } catch (Exception e) {
            logger.warn("Could not maximize window: {}", e.getMessage());
        }
    }
}
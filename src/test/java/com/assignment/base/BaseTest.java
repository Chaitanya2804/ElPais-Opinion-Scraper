package com.assignment.base;

import com.assignment.scrapping.config.ConfigManager;
import com.assignment.scrapping.driver.CapabilityFactory;
import com.assignment.scrapping.driver.DriverManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.ITestResult;
import org.testng.annotations.*;

/**
 * Base class for all tests.
 * Handles WebDriver lifecycle:
 *   @BeforeMethod  → create driver (local or BrowserStack)
 *   @AfterMethod   → quit driver, mark BS session pass/fail
 *
 * Tests NEVER create or quit drivers themselves.
 * They only call getDriver().
 *
 * The 'platform' parameter is injected by TestNG XML.
 * When not provided (local run), defaults to "chrome".
 */
public class BaseTest {

    protected final Logger logger = LogManager.getLogger(getClass());
    protected final ConfigManager config = ConfigManager.getInstance();

    // Injected by TestNG XML <parameter name="platform" value="..."/>
    @Parameters({"platform"})
    @BeforeMethod(alwaysRun = true)
    public void setUp(@Optional("local") String platform) {
        logger.info("════════════════════════════════════════════════");
        logger.info("Setting up driver for platform: {}", platform);
        logger.info("Thread: {}", Thread.currentThread().getName());

        WebDriver driver;

        if ("local".equalsIgnoreCase(platform)) {
            // Local execution — use Chrome by default
            String browser = System.getProperty("browser", "chrome");
            driver = DriverManager.createLocalDriver(browser);
        } else {
            // BrowserStack execution — build capabilities for this platform
            driver = DriverManager.createRemoteDriver(
                    CapabilityFactory.getCapabilities(platform));
        }

        // Store in ThreadLocal — thread-safe for parallel execution
        DriverManager.setDriver(driver);
        logger.info("Driver initialized. Session ready.");
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        // Mark BrowserStack session as passed or failed
        markBrowserStackSession(result);

        // Quit and clean up
        DriverManager.quitDriver();

        logger.info("Driver quit. Test result: {}", getResultStatus(result));
        logger.info("════════════════════════════════════════════════");
    }

    // ── Helpers ──────────────────────────────────────

    /**
     * Sends pass/fail status to BrowserStack dashboard via JS executor.
     * Only executes when running on BrowserStack (RemoteWebDriver).
     */
    private void markBrowserStackSession(ITestResult result) {
        try {
            WebDriver driver = DriverManager.getDriver();
            if (driver instanceof org.openqa.selenium.remote.RemoteWebDriver) {
                String status = result.isSuccess() ? "passed" : "failed";
                String reason = result.isSuccess() ? "Test Passed" :
                        result.getThrowable() != null
                                ? result.getThrowable().getMessage()
                                : "Test Failed";

                org.openqa.selenium.JavascriptExecutor js =
                        (org.openqa.selenium.JavascriptExecutor) driver;
                js.executeScript(
                        "browserstack_executor: " +
                                "{\"action\": \"setSessionStatus\", \"arguments\": " +
                                "{\"status\": \"" + status + "\", " +
                                "\"reason\": \"" + sanitize(reason) + "\"}}");

                logger.info("BrowserStack session marked as: {}", status);
            }
        } catch (Exception e) {
            logger.warn("Could not mark BrowserStack session: {}", e.getMessage());
        }
    }

    private String sanitize(String input) {
        if (input == null) return "";
        return input.replaceAll("[\"\\\\]", "")
                .substring(0, Math.min(input.length(), 255));
    }

    private String getResultStatus(ITestResult result) {
        return switch (result.getStatus()) {
            case ITestResult.SUCCESS -> "PASSED ✓";
            case ITestResult.FAILURE -> "FAILED ✗";
            case ITestResult.SKIP    -> "SKIPPED ↷";
            default                  -> "UNKNOWN";
        };
    }

    /** Convenience method for all subclasses */
    protected WebDriver getDriver() {
        return DriverManager.getDriver();
    }
}

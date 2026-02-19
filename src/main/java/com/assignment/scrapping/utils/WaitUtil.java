package com.assignment.scrapping.utils;

import com.assignment.scrapping.config.ConfigManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * Centralized wait utility.
 * Always use these instead of Thread.sleep().
 * Timeout values come from ConfigManager — never hardcoded.
 */
public class WaitUtil {

    private static final Logger logger = LogManager.getLogger(WaitUtil.class);
    private static final int EXPLICIT_TIMEOUT =
            ConfigManager.getInstance().getExplicitTimeout();

    private WaitUtil() {}

    private static WebDriverWait getWait(WebDriver driver) {
        return new WebDriverWait(driver,
                Duration.ofSeconds(EXPLICIT_TIMEOUT));
    }

    private static WebDriverWait getWait(WebDriver driver, int seconds) {
        return new WebDriverWait(driver, Duration.ofSeconds(seconds));
    }

    /** Wait until element is visible */
    public static WebElement waitForVisibility(WebDriver driver, By locator) {
        logger.debug("Waiting for visibility: {}", locator);
        return getWait(driver).until(
                ExpectedConditions.visibilityOfElementLocated(locator));
    }

    /** Wait until element is clickable */
    public static WebElement waitForClickable(WebDriver driver, By locator) {
        logger.debug("Waiting for clickable: {}", locator);
        return getWait(driver).until(
                ExpectedConditions.elementToBeClickable(locator));
    }

    /** Wait for all elements matching locator to be visible */
    public static List<WebElement> waitForAllVisible(
            WebDriver driver, By locator) {
        logger.debug("Waiting for all elements: {}", locator);
        return getWait(driver).until(
                ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
    }

    /** Wait until element is present in DOM (not necessarily visible) */
    public static WebElement waitForPresence(WebDriver driver, By locator) {
        return getWait(driver).until(
                ExpectedConditions.presenceOfElementLocated(locator));
    }

    /** Wait for page to finish loading via document.readyState */
    public static void waitForPageLoad(WebDriver driver) {
        logger.debug("Waiting for page load...");
        try {
            new WebDriverWait(driver, Duration.ofSeconds(
                    ConfigManager.getInstance().getPageLoadTimeout()))
                    .until(d -> {
                        String state = ((JavascriptExecutor) d)
                                .executeScript("return document.readyState")
                                .toString();
                        // Accept both complete and interactive (EAGER strategy)
                        return state.equals("complete") || state.equals("interactive");
                    });
        } catch (TimeoutException e) {
            // Log but don't throw — page may still be usable
            logger.warn("Page load timeout — proceeding with current state.");
        }
    }

    /** Safe find — returns null instead of throwing if not found */
    public static WebElement findSafe(WebDriver driver, By locator) {
        try {
            return driver.findElement(locator);
        } catch (NoSuchElementException e) {
            logger.debug("Element not found (safe): {}", locator);
            return null;
        }
    }

    /** Scroll element into view using JS */
    public static void scrollIntoView(WebDriver driver, WebElement element) {
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].scrollIntoView(true);", element);
        try { Thread.sleep(300); } catch (InterruptedException ignored) {}
    }

    /** Dismiss cookie banner or overlay if present */
    public static void dismissOverlayIfPresent(
            WebDriver driver, By overlayLocator) {
        try {

            List<WebElement> elements = driver.findElements(overlayLocator);

            if (!elements.isEmpty()) {
                WebElement overlay = elements.get(0);
                if (overlay.isDisplayed() && overlay.isEnabled()) {
                    overlay.click();
                    logger.info("Overlay dismissed: {}", overlayLocator);
                    // Small wait after dismissing
                    Thread.sleep(1000);
                }
            } else {
                logger.debug("No overlay found: {}", overlayLocator);
            }
        } catch (ClassCastException e) {
            // Safari RemoteWebDriver serialization quirk — safe to ignore
            logger.warn("ClassCast on overlay check (Safari) — skipping: {}",
                    overlayLocator);
        } catch (Exception e) {
            logger.debug("Overlay not present or not clickable: {}",
                    e.getMessage());
        }
    }
}
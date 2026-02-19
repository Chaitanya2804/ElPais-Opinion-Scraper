package com.assignment.scrapping.pages;

import com.assignment.scrapping.config.ConfigManager;
import com.assignment.scrapping.utils.WaitUtil;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Page Object for El País Home Page.
 * Handles: navigation, cookie consent, language check,
 * and navigating to Opinion section.
 */
public class HomePage extends BasePage {

    private final ConfigManager config = ConfigManager.getInstance();

    // ── Locators ─────────────────────────────────────
    // Defined as constants — easy to update if site changes

    private static final By COOKIE_ACCEPT_BTN =
            By.id("didomi-notice-agree-button");

    private static final By COOKIE_ACCEPT_ALT =
            By.cssSelector("button[class*='accept'], button[id*='accept']");

    private static final By OPINION_NAV_LINK =
            By.cssSelector("a[href*='/opinion']");

    private static final By OPINION_NAV_TEXT =
            By.xpath("//nav//a[normalize-space(text())='Opinión']");

    private static final By MAIN_HEADER =
            By.cssSelector("header, .header, #header");

    public HomePage(WebDriver driver) {
        super(driver);
    }

    // ── Actions ──────────────────────────────────────

    /**
     * Opens El País home page and handles cookie banner.
     */
    public HomePage open() {
        navigateTo(config.getAppUrl());
        handleCookieConsent();
        return this;
    }

    /**
     * Dismisses cookie/GDPR consent banner if present.
     * El País shows this on first visit — must be handled
     * before any other interaction.
     */
    public void handleCookieConsent() {
        logger.info("Checking for cookie consent banner...");

        // Try primary locator first
        WaitUtil.dismissOverlayIfPresent(driver, COOKIE_ACCEPT_BTN);

        // If still present, try alternative
        WaitUtil.dismissOverlayIfPresent(driver, COOKIE_ACCEPT_ALT);
    }

    /**
     * Verifies the page is displayed in Spanish via <html lang="es">.
     */
    public boolean isInSpanish() {
        return isPageInLanguage(config.get("app.language.expected", "es"));
    }

    /**
     * Clicks the Opinion section link in main navigation.
     * Returns OpinionPage for fluent page chaining.
     */
    public OpinionPage navigateToOpinion() {
        logger.info("Navigating to Opinion section...");

        // ✅ For mobile — directly navigate to opinion URL
        // Mobile layouts hide desktop nav links
        boolean isMobile = isMobileDevice();

        if (isMobile) {
            logger.info("Mobile detected — navigating directly to Opinion URL.");
            driver.get(config.getAppUrl() + config.getOpinionPath());
            WaitUtil.waitForPageLoad(driver);
        } else {
            // Desktop — try nav link click
            try {
                click(OPINION_NAV_TEXT);
            } catch (Exception e) {
                logger.warn("Nav text failed, trying href locator...");
                try {
                    click(OPINION_NAV_LINK);
                } catch (Exception e2) {
                    logger.warn("Both nav locators failed — direct navigation.");
                    driver.get(config.getAppUrl() + config.getOpinionPath());
                    WaitUtil.waitForPageLoad(driver);
                }
            }
        }

        logger.info("Opinion URL: {}", driver.getCurrentUrl());
        return new OpinionPage(driver);
    }

    /**
     * Detects if running on a mobile device by checking capabilities.
     */
    private boolean isMobileDevice() {
        try {
            if (driver instanceof org.openqa.selenium.remote.RemoteWebDriver) {
                org.openqa.selenium.Capabilities caps =
                        ((org.openqa.selenium.remote.RemoteWebDriver) driver)
                                .getCapabilities();

                Object platform = caps.getCapability("platformName");
                if (platform != null) {
                    String p = platform.toString().toLowerCase();
                    return p.equals("android") || p.equals("ios");
                }

                Object mobile = caps.getCapability("realMobile");
                if (mobile != null) return true;
            }
        } catch (Exception e) {
            logger.warn("Could not detect mobile: {}", e.getMessage());
        }
        return false;
    }
}
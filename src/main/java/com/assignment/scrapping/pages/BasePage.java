package com.assignment.scrapping.pages;

import com.assignment.scrapping.utils.WaitUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.*;

import java.util.List;

/**
 * Abstract base for all Page Objects.
 * For shared WebDriver reference and common interaction helpers.
 * Every page class extends this — never instantiate directly.
 */
public abstract class BasePage {

    protected final WebDriver driver;
    protected final Logger logger = LogManager.getLogger(getClass());

    protected BasePage(WebDriver driver) {
        if (driver == null) {
            throw new IllegalArgumentException(
                    "WebDriver cannot be null in " + getClass().getSimpleName());
        }
        this.driver = driver;
    }



    protected void navigateTo(String url) {
        logger.info("Navigating to: {}", url);
        driver.get(url);
        WaitUtil.waitForPageLoad(driver);
    }

    protected String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    protected String getPageTitle() {
        return driver.getTitle();
    }

    // ── Element Interactions ─────────────────────────

    protected WebElement find(By locator) {
        return WaitUtil.waitForVisibility(driver, locator);
    }

    protected List<WebElement> findAll(By locator) {
        return WaitUtil.waitForAllVisible(driver, locator);
    }

    protected WebElement findSafe(By locator) {
        return WaitUtil.findSafe(driver, locator);
    }

    protected void click(By locator) {
        WaitUtil.waitForClickable(driver, locator).click();
    }

    protected String getText(By locator) {
        return find(locator).getText().trim();
    }

    protected String getAttribute(WebElement element, String attr) {
        String value = element.getAttribute(attr);
        return value != null ? value.trim() : null;
    }

    protected void scrollTo(WebElement element) {
        WaitUtil.scrollIntoView(driver, element);
    }

    protected String executeScript(String script, Object... args) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        Object result = js.executeScript(script, args);
        return result != null ? result.toString() : null;
    }

    // ── Page Language Verification ───────────────────

    /**
     * Checks the HTML lang attribute to confirm page language.
     * @param expectedLang  e.g. "es" for Spanish
     */
    public boolean isPageInLanguage(String expectedLang) {
        try {
            String lang = driver.findElement(By.tagName("html"))
                    .getAttribute("lang");
            if (lang == null) return false;
            boolean match = lang.toLowerCase().startsWith(expectedLang.toLowerCase());
            logger.info("Page lang attribute: '{}' | Expected: '{}' | Match: {}",
                    lang, expectedLang, match);
            return match;
        } catch (NoSuchElementException e) {
            logger.warn("Could not find <html> tag to check language.");
            return false;
        }
    }
}
package com.assignment.scrapping.driver;

import com.assignment.scrapping.config.ConfigManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.util.HashMap;
import java.util.Map;

/**
 * Builds BrowserStack DesiredCapabilities for each parallel thread.
 * Each capability set maps to one TestNG <test> block in testng-browserstack.xml.
 *
 * Capabilities reference:
 * https://www.browserstack.com/automate/capabilities
 */
public class CapabilityFactory {

    private static final Logger logger =
            LogManager.getLogger(CapabilityFactory.class);

    private CapabilityFactory() {}


    public static DesiredCapabilities getCapabilities(String platform) {
        logger.info("Building capabilities for platform: {}", platform);

        ConfigManager config = ConfigManager.getInstance();
        String username   = config.getBsUsername();
        String accessKey  = config.getBsAccessKey();
        String buildName  = config.get("browserstack.build.name");
        String projectName = config.get("browserstack.project.name");

        // Common BrowserStack options shared across all platforms
        Map<String, Object> bstackOptions = new HashMap<>();
        bstackOptions.put("userName",   username);
        bstackOptions.put("accessKey",  accessKey);
        bstackOptions.put("buildName",  buildName);
        bstackOptions.put("projectName", projectName);
        bstackOptions.put("sessionName", "ElPais-" + platform);
        bstackOptions.put("debug",       true);   // screenshots on BS dashboard
        bstackOptions.put("networkLogs", true);   // network logs on BS dashboard
        bstackOptions.put("consoleLogs", "info");

        DesiredCapabilities caps = new DesiredCapabilities();

        switch (platform.toLowerCase()) {


            case "windows_chrome" -> {
                caps.setCapability("browserName", "Chrome");
                caps.setCapability("browserVersion", "latest");
                bstackOptions.put("os",        "Windows");
                bstackOptions.put("osVersion",  "11");
                bstackOptions.put("sessionName", "ElPais-Windows-Chrome");
            }


            case "windows_firefox" -> {
                caps.setCapability("browserName", "Firefox");
                caps.setCapability("browserVersion", "latest");
                bstackOptions.put("os",        "Windows");
                bstackOptions.put("osVersion",  "11");
                bstackOptions.put("sessionName", "ElPais-Windows-Firefox");
            }


            case "mac_safari" -> {
                caps.setCapability("browserName", "Safari");
                caps.setCapability("browserVersion", "18.0");
                bstackOptions.put("os",        "OS X");
                bstackOptions.put("osVersion",  "Sequoia");
                bstackOptions.put("sessionName", "ElPais-macOS-Safari");
            }


            case "iphone" -> {
                bstackOptions.put("deviceName",   "iPhone 14");
                bstackOptions.put("osVersion",     "17");
                bstackOptions.put("platformName",  "iOS");
                bstackOptions.put("sessionName",   "ElPais-iPhone15");
                bstackOptions.put("appiumVersion", "2.0.0");
                caps.setCapability("browserName", "Safari");
            }


            case "android" -> {
                bstackOptions.put("deviceName",   "Samsung Galaxy S22");
                bstackOptions.put("osVersion",     "13.0");
                bstackOptions.put("platformName",  "Android");
                bstackOptions.put("sessionName",   "ElPais-Android-S23");
                bstackOptions.put("appiumVersion", "2.0.0");
                caps.setCapability("browserName", "Chrome");
            }

            default -> throw new IllegalArgumentException(
                    "Unknown platform: '" + platform + "'. "
                            + "Valid: windows_chrome, windows_firefox, "
                            + "mac_safari, iphone, android");
        }

        caps.setCapability("bstack:options", bstackOptions);
        logger.info("Capabilities built for: {}", platform);
        return caps;
    }
}
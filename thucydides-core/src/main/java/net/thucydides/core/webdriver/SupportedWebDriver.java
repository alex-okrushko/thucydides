package net.thucydides.core.webdriver;

import com.google.common.base.Joiner;
import com.opera.core.systems.OperaDriver;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.android.AndroidDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.iphone.IPhoneDriver;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.sikuli.webdriver.SikuliFirefoxDriver;

/**
 * The list of supported web drivers.
 * These are the drivers that support screenshots.
 */
public enum SupportedWebDriver {
	
	/**
	 * Sikuli Firefox WebDriver driver.
	 */
	SIKULI(SikuliFirefoxDriver.class),
    /**
     * Firefox WebDriver driver.
     */
    FIREFOX(FirefoxDriver.class),

    /**
     * Chrome  WebDriver driver.
     */
    CHROME(ChromeDriver.class),

    /**
     * Opera - use it with SauceLabs
     */
    OPERA(OperaDriver.class),

    /**
     * HTMLUnit - fast, but no screenshots.
     */
    HTMLUNIT(HtmlUnitDriver.class),

    /**
     * Remote web driver
     */
    REMOTE(RemoteWebDriver.class),

    /**
     * Android driver
     */
    ANDROID(AndroidDriver.class),
    /**
     * iPhone driver
     */
    IPHONE(IPhoneDriver.class),
    /**
     * Internet Explorer
     */
    IEXPLORER(InternetExplorerDriver.class),

    /**
     * Safari
     */
    SAFARI(SafariDriver.class);

    private final Class<? extends WebDriver> webdriverClass;

    private SupportedWebDriver(Class<? extends WebDriver> webdriverClass) {
        this.webdriverClass = webdriverClass;
    }

    public Class<? extends WebDriver> getWebdriverClass() {
        return webdriverClass;
    }

    /**
     * HTMLUnit - mainly for testing, as this driver does not support screenshots or much AJAX.
     */
    public static String listOfSupportedDrivers() {
        return Joiner.on(", ").join(SupportedWebDriver.values());
    }

    public static SupportedWebDriver getClosestDriverValueTo(final String value) {
        SupportedWebDriver closestDriver = null;
        int closestDriverDistance = Integer.MAX_VALUE;
        for(SupportedWebDriver supportedDriver : values()) {
            int distance = StringUtils.getLevenshteinDistance(supportedDriver.toString(), value);
            if (distance < closestDriverDistance) {
                closestDriverDistance = distance;
                closestDriver = supportedDriver;
            }
        }
        return closestDriver;
    }

    public static SupportedWebDriver getDriverTypeFor(final String value) throws UnsupportedDriverException {
        try {
            return SupportedWebDriver.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            SupportedWebDriver closestMatchingDriver = getClosestDriverValueTo(value);
            throw new UnsupportedDriverException("Unsupported browser type: " + value
                                                 + ". Did you mean " + closestMatchingDriver.toString().toLowerCase()
                                                 + "?", e);
        }

    }
}
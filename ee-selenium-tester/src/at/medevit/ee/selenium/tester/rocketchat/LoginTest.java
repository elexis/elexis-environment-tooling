package at.medevit.ee.selenium.tester.rocketchat;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import at.medevit.ee.selenium.tester.SeleniumTester;

public class LoginTest {
	
	public static String ROCKETCHAT_URL;
	
	private static ChromeOptions options;
	
	private WebDriver browser;
	private WebDriverWait wait;
	
	@BeforeAll
	public static void beforeAll(){
		System.setProperty("webdriver.chrome.driver", SeleniumTester.getTest_chromedriver());
		options = new ChromeOptions();
		DesiredCapabilities jsCapabilities = DesiredCapabilities.chrome();
		Map<String, Object> prefs = new HashMap<>();
		prefs.put("intl.accept_languages", "de");
		options.setExperimentalOption("prefs", prefs);
		jsCapabilities.setCapability(ChromeOptions.CAPABILITY, options);
		options.addArguments("window-size=1200x600", "no-sandbox", "ignore-certificate-errors");
		ROCKETCHAT_URL = "https://" + SeleniumTester.getEe_hostname() + "/chat";
	}
	
	@BeforeEach
	public void before(){
		browser = new ChromeDriver(options);
		wait = new WebDriverWait(browser, 10);
	}
	
	@AfterEach
	public void afterEach(){
		if (browser != null) {
			browser.close();
		}
	}
	
	private void loadLoginPage(){
		browser.get(ROCKETCHAT_URL);
		
		// wait for login screen
		wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".rc-button--primary")));
	}
	
	@Test
	public void rocketchat_login_ldap() throws IOException{
		// https://saucelabs.com/resources/articles/getting-started-with-webdriver-selenium-for-java-in-eclipse
		// https://chromedriver.storage.googleapis.com/77.0.3865.40/chromedriver_mac64.zip
		loadLoginPage();
		takeScreenShot("login_ldap");
		
		WebElement emailOrUsernameForm = browser.findElement(By.id("emailOrUsername"));
		WebElement passForm = browser.findElement(By.id("pass"));
		WebElement loginButton = browser.findElement(By.cssSelector(".rc-button--primary"));
		
		emailOrUsernameForm.sendKeys(SeleniumTester.getTest_user());
		passForm.sendKeys(SeleniumTester.getTest_password());
		
		loginButton.click();
		
		validateLoggedInAndPerformLogoff("login_ldap");
	}
	
	@Test
	public void rocketchat_login_keycloak() throws IOException{
		loadLoginPage();
		takeScreenShot("login_keycloak");
		
		WebElement keycloakButton =
			browser.findElement(By.xpath("//form[@id='login-card']/div/button/span"));
		keycloakButton.click();
		
		assertTrue(
			browser.getCurrentUrl()
				.contains("keycloak/auth/realms/ElexisEnvironment/protocol/saml?SAMLRequest"),
			browser.getCurrentUrl());
		
		wait.until(ExpectedConditions.presenceOfElementLocated(By.id("username")));
		
		WebElement keycloakUsernameForm = browser.findElement(By.id("username"));
		WebElement keycloakPasswordForm = browser.findElement(By.id("password"));
		WebElement loginButton = browser.findElement(By.id("kc-login"));
		
		keycloakUsernameForm.click();
		keycloakUsernameForm.sendKeys(SeleniumTester.getTest_user());
		keycloakPasswordForm.click();
		keycloakPasswordForm.sendKeys(SeleniumTester.getTest_password());
		loginButton.click();
		
		wait.until(ExpectedConditions.elementToBeClickable(By.className("avatar-image")));
		
		assertTrue(browser.getCurrentUrl().startsWith(ROCKETCHAT_URL + "/home"),
			browser.getCurrentUrl());
		validateLoggedInAndPerformLogoff("login_keycloak");
	}
	
	public void takeScreenShot(String name) throws IOException{
		File screen = ((TakesScreenshot) browser).getScreenshotAs(OutputType.FILE);
		BufferedImage img = ImageIO.read(screen);
		ImageIO.write(img, "png", screen);
		File file =
			new File(SeleniumTester.getTest_outputDir(), name + "_" + System.nanoTime() + ".png");
		System.out.println("Screenshot written to " + file.getAbsolutePath());
		FileUtils.copyFile(screen, file);
	}
	
	/**
	 * Validate that we are really logged in by opening the side menu and looking for our username
	 * 
	 * @throws IOException
	 */
	private void validateLoggedInAndPerformLogoff(String name) throws IOException{
		wait.until(ExpectedConditions.elementToBeClickable(By.className("avatar-image")));
		takeScreenShot(name+"_loggedIn");
		// open popover
		WebElement popover = browser.findElement(By.className("avatar-image"));
		assertNotNull(popover);
		popover.click();
		//
		WebElement findElement = browser.findElement(By.className("rc-popover__title"));
		assertTrue(findElement.getText().toLowerCase()
			.contains(SeleniumTester.getTest_user().toLowerCase()));
		// 
		WebElement logoff = browser.findElement(By.xpath("//span[contains(.,'Abmelden')]"));
		logoff.click();
		takeScreenShot(name+"_loggedOff");
	}
	
}

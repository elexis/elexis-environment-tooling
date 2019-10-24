package at.medevit.ee.selenium.tester;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

public class SeleniumTester {
	
	@Parameter(names = "-h", required = true, description = "Elexis-Environment hostname")
	String hostname;
	
	@Parameter(names = "-l", required = true, description = "username to test")
	String user;
	
	@Parameter(names = "-p", required = true, description = "password to test")
	String password;
	
	@Parameter(names = "-c", required = true, description = "chromedriver executable location")
	String chromedriver = System.getProperty("webdriver.chrome.driver");
	
	@Parameter(names = "-od", description = "base directory for generated output")
	String outputDir;
	
	@Parameter(names = "-rr", description = "run rocketchat tests")
	boolean runRocketchatTests = false;
	
	@Parameter(names = "-rb", description = "run bookstack tests")
	boolean runBookstackTests = false;
	
	protected static String test_user;
	protected static String test_password;
	protected static String ee_hostname;
	protected static String test_chromedriver;
	protected static String test_outputDir;
	
	public static void main(String[] argv) throws IOException{
		
		SeleniumTester main = new SeleniumTester();
		
		JCommander commander = JCommander.newBuilder().addObject(main).build();
		commander.setDefaultProvider(optionName -> {
			if ("-h".equals(optionName)) {
				return System.getenv("EE_HOSTNAME");
			}
			if ("-c".equals(optionName)) {
				if (System.getProperty("webdriver.chrome.driver") != null) {
					return System.getProperty("webdriver.chrome.driver");
				}
				return System.getenv("CHROMEDRIVER_EXECUTABLE");
			}
			return null;
		});
		try {
			commander.parse(argv);
			
			SeleniumTester.test_user = main.user;
			SeleniumTester.test_password = main.password;
			SeleniumTester.ee_hostname = main.hostname;
			SeleniumTester.test_chromedriver = main.chromedriver;
			SeleniumTester.test_outputDir = main.outputDir;
			
			main.run();
		} catch (ParameterException e) {
			System.out.println(e.getMessage() + "\n");
			commander.usage();
		} finally {
			
		}
	}
	
	public void run(){
		List<DiscoverySelector> ds = new ArrayList<>();
		if (runRocketchatTests) {
			ds.add(selectPackage("at.medevit.ee.selenium.tester.rocketchat"));
		}
		if (runBookstackTests) {
			ds.add(selectPackage("at.medevit.ee.selenium.tester.bookstack"));
		}
		LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
			.selectors(ds.toArray(new DiscoverySelector[] {})).build();
		Launcher launcher = LauncherFactory.create();
		SummaryGeneratingListener listener = new SummaryGeneratingListener();
		launcher.registerTestExecutionListeners(listener);
		launcher.registerTestExecutionListeners(new SysOutExecutionListener());
		
		launcher.execute(request);
		TestExecutionSummary summary = listener.getSummary();
		
		long testsSucceededCount = summary.getTestsSucceededCount();
		long testsFailedCount = summary.getTestsFailedCount();
		
		System.out.printf("Total Tests: %d, SUCCESS: %d, FAIL: %d%n", summary.getTestsFoundCount(),
			testsSucceededCount, testsFailedCount);
		System.out.flush();
		
		if (testsFailedCount != 0) {
			System.out.println("--- THERE ARE FAILED TESTS ----");
			System.exit(-1);
		}
		
	}
	
	public static String getEe_hostname(){
		return ee_hostname;
	}
	
	public static String getTest_password(){
		return test_password;
	}
	
	public static String getTest_user(){
		return test_user;
	}
	
	public static String getTest_chromedriver(){
		return test_chromedriver;
	}
	
	public static String getTest_outputDir(){
		return test_outputDir;
	}
	
}

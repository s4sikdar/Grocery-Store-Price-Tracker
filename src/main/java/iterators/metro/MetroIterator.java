package iterators.metro;
import java.lang.*;
import java.util.*;
import java.time.*;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Stream;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.DirectoryStream;
import org.openqa.selenium.Alert;
import org.openqa.selenium.WindowType;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.ProfilesIni;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import javax.xml.stream.XMLStreamException;
import iterators.BaseIterator;
import iterators.xml.*;
import iterators.util.*;


public class MetroIterator extends BaseIterator {
	private WebDriver driver;
	private ArrayList<String> categories_left;
	private ArrayList<String> subcategories_left;
	private boolean cookies_menu_removed;


	public MetroIterator(String config_file_path) {
		super(config_file_path);
	}


	public MetroIterator(String config_file_path, int hours) {
		super(config_file_path, hours);
		this.setUp();
	}


	public MetroIterator(String config_file_path, int hours, int minutes) {
		super(config_file_path, hours, minutes);
		this.setUp();
	}


	/**
	 * setUp - a private helper method to finish set up activities
	 * @return - returns nothing (void)
	 */
	private void setUp() {
		this.driver = null;
		this.subcategories_left = new ArrayList<String>();
		this.cookies_menu_removed = false;
	}


	/**
	 * categoriesFileExists - a private helper method that determines if the xml file storing categories exists,
	 * and returns true if this file exists
	 * @return - returns true if the file exists, returns false otherwise
	 */
	private boolean categoriesFileExists() {
		String currentPath = System.getProperty("user.dir");
		Path pwd = Paths.get(currentPath);
		String fname_for_cities_left = this.getConfigProperty("categories_left_fname");
		Path xml_path = pwd.resolve(fname_for_cities_left);
		File xml_cities_file = new File(xml_path.toString());
		boolean xml_cities_file_exists = xml_cities_file.exists();
		return xml_cities_file_exists;
	}


	/**
	 * subcategoriesFileExists - a private helper method that determines if the xml file storing subcategories
	 * exists, and returns true if this file exists
	 * @return - returns true if the file exists, returns false otherwise
	 */
	private boolean subcategoriesFileExists() {
		String currentPath = System.getProperty("user.dir");
		Path pwd = Paths.get(currentPath);
		String fname_for_cities_left = this.getConfigProperty("subcategories_left_fname");
		Path xml_path = pwd.resolve(fname_for_cities_left);
		File xml_cities_file = new File(xml_path.toString());
		boolean xml_cities_file_exists = xml_cities_file.exists();
		return xml_cities_file_exists;
	}


	/**
	 * removeCookiesButton - a private method that checks if the cookies menu is there and clicks the button to close it
	 * - the method also maximizes the window
	 * @return - returns nothing (void)
	 */
	private void removeCookiesButton() {
		boolean cookies_notice_container_exists;
		boolean close_cookies_button_exists;
		this.driver.manage().window().maximize();
		String cookies_options_container_selector = this.getConfigProperty("cookies_options_container_selector");
		String cookies_decline_selector = this.getConfigProperty("cookies_decline_selector");
		cookies_notice_container_exists = WebElementOperations.elementExists(
			new By.ByCssSelector(cookies_options_container_selector), this.driver, 30, 500L
		);
		if (cookies_notice_container_exists) {
			close_cookies_button_exists = WebElementOperations.elementExistsByJavaScript(
				this.driver, cookies_decline_selector
			);
			if (close_cookies_button_exists) {
				WebElement close_cookies_button = WebElementOperations.fluentWait(
					new By.ByCssSelector(cookies_decline_selector), this.driver, 30, 500L
				);
				new Actions(this.driver)
					.moveToElement(close_cookies_button)
					.click()
					.pause(Duration.ofMillis(3000))
					.perform();
				cookies_notice_container_exists = WebElementOperations.elementExists(
					new By.ByCssSelector(cookies_options_container_selector), this.driver, 3, 500L
				);
			}
		}
		this.cookies_menu_removed = true;
	}


	/**
	 * loadXML - a public method that gets all possible cities where there is a store location, and for each
	 * city, selects a store in that city and scrapes price information for all products in that store
	 * - for each set of product information that has been scraped, an entry is added to the XML file who's name
	 *   is passed into the iterator's constructor
	 * - this method essentialy adds entries to the XML file to be iterated over using the methods next() and
	 *   hasNext()
	 * @return - returns nothing (void)
	 */
	public void loadXML() throws InterruptedException, XMLStreamException {
		FirefoxOptions options = new FirefoxOptions();
		// https://stackoverflow.com/questions/13959704/accepting-sharing-location-browser-popups-through-selenium-webdriver
		options.setPageLoadStrategy(PageLoadStrategy.EAGER);
		options.addPreference("geo.prompt.testing", true);
		options.addPreference("geo.prompt.testing.allow", true);
		//options.addPreference(
		//	"geo.wifi.uri", "https://location.services.mozilla.com/v1/geolocate?key=%MOZILLA_API_KEY%"
		//);
		this.driver = new FirefoxDriver(options);
		this.driver.get(this.getConfigProperty("url"));
		this.removeCookiesButton();
		this.driver.quit();
	}
}

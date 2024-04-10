package iterators.loblaws;
import java.lang.*;
import java.util.HashMap;
import java.util.Set;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import org.openqa.selenium.Alert;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.NoSuchElementException;
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
import iterators.GroceryStorePriceScraper;

public class LoblawsIterator implements GroceryStorePriceScraper {
	private int counter;
	private int limit;
	private String fpath;
	private HashMap<String, String> numbers;
	private Properties configurations;
	private WebDriver driver;
	private HashMap<String, Boolean> cities;

	public LoblawsIterator(String config_file_path, int count, int limit) {
		this.counter = count;
		this.limit = limit;
		this.numbers = new HashMap<>();
		this.cities = new HashMap<>();
		this.fpath = config_file_path;
		File filename = new File(config_file_path);
		//System.out.println(new File(".").getAbsolutePath());
		//System.out.println(filename.exists());
		//System.out.println(filename.canRead());
		//System.out.println(filename.isFile());
		//InputStream propertiesInputStream = null;
                this.configurations = new Properties();
                //propertiesInputStream = LoblawsIterator.class.getClassLoader().getResourceAsStream(config_file_path);
		try {
			this.configurations.load(new FileInputStream(config_file_path));
		} catch (Throwable t) {
			t.printStackTrace();
		}
		this.driver = null;
		//this.configurations = new Properties();
		//this.configurations.load(new FileInputStream(filename));

	}
	
	public HashMap<String, String> next() {
		numbers.put(
			Integer.toString(this.counter),
			"Current counter is " + this.counter + ", limit is " + Integer.toString(this.limit)
		);
		this.counter++;
		return numbers;
	}

	public boolean hasNext() {
		return (this.counter <= this.limit);
	}

	// Code was taken from below stackoverflow link:
	// https://stackoverflow.com/questions/12858972/how-can-i-ask-the-selenium-webdriver-to-wait-for-few-seconds-in-java
	public WebElement fluentWait(final By locator, WebDriver driver, int timeout_duration, long polling_duration) {
	    Wait<WebDriver> wait = new FluentWait<WebDriver>(driver)
		    .withTimeout(Duration.ofSeconds(timeout_duration))
		    .pollingEvery(Duration.ofMillis(polling_duration))
		    .ignoring(NoSuchElementException.class);

	    WebElement foo = wait.until(new Function<WebDriver, WebElement>() {
		public WebElement apply(WebDriver driver) {
		    return driver.findElement(locator);
		}
	    });

	    return foo;
	};

	public boolean elementExists(final By locator, WebDriver driver, int timeout_duration, long polling_duration) {
		try {
			WebElement element_to_find = this.fluentWait(
				locator, driver, timeout_duration, polling_duration
			);
			if (element_to_find != null) {
				return true;
			} else {
				return false;
			}
		} catch (Throwable err) {
			return false;
		}
	}

	public void getAllCities() throws InterruptedException {
		String location_button_selector = this.configurations.getProperty("location_button");
		String change_location_selector = this.configurations.getProperty("change_location_button");
		WebElement location_button = this.fluentWait(
			new By.ByCssSelector(location_button_selector), this.driver, 30, 1000L
		);
		this.driver.manage().window().maximize();
		location_button.click();
		WebElement change_location_button = this.fluentWait(
			new By.ByCssSelector(change_location_selector), this.driver, 30, 1000L
		);
		change_location_button.click();
		WebElement location_input = this.fluentWait(
			new By.ByCssSelector(this.configurations.getProperty("location_input_field")),
			this.driver, 30, 1000L
		);
		new Actions(this.driver)
			.moveToElement(location_input)
			.click()
			.sendKeys(this.configurations.getProperty("location_input_field_value"))
			.pause(Duration.ofMillis(1000))
			.sendKeys(Keys.ENTER)
			.pause(Duration.ofMillis(500))
			.perform();
		int counter = 1;
		StringBuilder store_info_container_selector = new StringBuilder(
			this.configurations.getProperty("store_content_box_selector")
		);
		StringBuilder store_location_text_selector = new StringBuilder(
			this.configurations.getProperty("store_location_text_selector")
		);
		String privacy_policy_selector = this.configurations.getProperty("privacy_policy_selector");
		WebElement privacy_policy_close_button = this.fluentWait(
			new By.ByCssSelector(privacy_policy_selector), this.driver, 30, 1000L
		);
		new Actions(this.driver)
			.moveToElement(privacy_policy_close_button)
			.click()
			.pause(Duration.ofMillis(1000))
			.perform();
		while (
			this.elementExists(
				new By.ByCssSelector(store_info_container_selector.toString()), this.driver, 30, 500L
			)
		) {
			WebElement store_info_container = this.fluentWait(
				new By.ByCssSelector(store_info_container_selector.toString()), this.driver, 30, 500L
			);
			WebElement store_info_location_text = store_info_container.findElement(
				new By.ByCssSelector(store_location_text_selector.toString())
			);
			String store_location_value = store_info_location_text.getText();
			String[] city_and_province = store_location_value.split(
				"[A-Za-z]\\d[A-Za-z][ -]?\\d[A-Za-z]\\d$"
			);
			city_and_province[0] = city_and_province[0].trim();
			String city_province_combination = city_and_province[0];
			this.cities.putIfAbsent(city_province_combination, new Boolean(true));
			int num_index = store_info_container_selector.indexOf(Integer.toString(counter));
			int num_length = Integer.toString(counter).length();
			counter++;
			store_info_container_selector.replace(
				num_index, (num_index + num_length), Integer.toString(counter)
			);
		}

		Set<String> unique_cities = this.cities.keySet();
		for (String city: unique_cities) {
			WebElement location_input_button = this.fluentWait(
				new By.ByCssSelector(this.configurations.getProperty("location_input_field")),
				this.driver, 30, 1000L
			);
			new Actions(this.driver)
				.moveToElement(location_input_button)
				.click()
				.sendKeys(city)
				.pause(Duration.ofMillis(500))
				.sendKeys(Keys.ENTER)
				.pause(Duration.ofMillis(750))
				.perform();
			String branch_info_selector = this.configurations.getProperty("store_content_box_selector");
			WebElement store_info_container = this.fluentWait(
				new By.ByCssSelector(branch_info_selector), this.driver, 30, 1000L
			);
		}

	}

	public String getUrl() throws InterruptedException {
		FirefoxOptions options = new FirefoxOptions();
		// https://stackoverflow.com/questions/13959704/accepting-sharing-location-browser-popups-through-selenium-webdriver
		options.addPreference("geo.prompt.testing", true);
		options.addPreference("geo.prompt.testing.allow", true);
		//options.addPreference(
		//	"geo.wifi.uri", "https://location.services.mozilla.com/v1/geolocate?key=%MOZILLA_API_KEY%"
		//);
		this.driver = new FirefoxDriver(options);
		this.driver.get(this.configurations.getProperty("url"));
		this.getAllCities();
		this.driver.quit();
		return this.configurations.getProperty("url");
	}
}

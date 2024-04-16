package iterators.loblaws;
import java.lang.*;
import java.util.*;
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
import org.openqa.selenium.ElementNotInteractableException;
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
                this.configurations = new Properties();
		try {
			this.configurations.load(new FileInputStream(config_file_path));
		} catch (Throwable t) {
			t.printStackTrace();
		}
		this.driver = null;
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

	public WebElement fluentWaitTillVisibleandClickable(final By locator, WebDriver driver, int timeout_duration, long polling_duration) {
		WebDriverWait wait = new WebDriverWait(
			driver, Duration.ofSeconds(timeout_duration), Duration.ofMillis(polling_duration)
		);

		wait.until(ExpectedConditions.and(
			ExpectedConditions.presenceOfElementLocated(locator),
			ExpectedConditions.visibilityOfElementLocated(locator)
		));
		WebElement element_in_question = driver.findElement(locator);
		return element_in_question;
	}

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

	public boolean elementExistsAndIsInteractable(final By locator, WebDriver driver, int timeout_duration, long polling_duration) {
		try {
			WebElement element_to_find = this.fluentWaitTillVisibleandClickable(
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


	}

	public void tryClickingElement(
		final By locator, WebDriver driver, int timeout_duration, long polling_duration
	) {
		try {
			WebElement element_in_question = this.fluentWait(locator, driver, timeout_duration, polling_duration);
			element_in_question.click();
		} catch (Throwable err) {
			boolean element_still_there = this.elementExists(
				locator, driver, timeout_duration, polling_duration
			);
			if (element_still_there) {
				WebElement target_element = this.fluentWait(
					locator, driver, timeout_duration, polling_duration
				);
				try {
					new Actions(this.driver).moveToElement(target_element).click();
				} catch (Throwable second_err) {
					throw second_err;
				}
			}
		}
	}

	private boolean menuItemToBeIgnored(WebElement element) {
		String element_text = element.getText();
		String main_menu_items_to_select = this.configurations.getProperty("main_menu_items_to_select");
		String[] items_to_select = main_menu_items_to_select.split(",");
		for (int index = 0; index < items_to_select.length; index++) {
			if (element_text.indexOf(items_to_select[index]) >= 0) {
				return false;
			}
		}
		return true;
	}

	private boolean submenuItemToBeIgnored(WebElement element, String css_selector) {
		String element_text = element.getText();
		String grocery_submenu_items_to_ignore = this.configurations.getProperty("grocery_sub_menu_items_to_ignore");
		String home_beauty_items_to_ignore = this.configurations.getProperty("home_beauty_submenu_items_to_ignore");
		String[] grocery_items_to_ignore = grocery_submenu_items_to_ignore.split(",");
		String[] home_items_to_ignore = home_beauty_items_to_ignore.split(",");
		String[] items_to_ignore = grocery_items_to_ignore;
		int opening_brace_index = css_selector.indexOf("(");
		int closing_brace_index = css_selector.indexOf(")", opening_brace_index);
		String number_between_brackets = css_selector.substring((opening_brace_index + 1), closing_brace_index);
		int translated_number = -1;
		try {
			translated_number = Integer.parseInt(number_between_brackets);
		} catch (Throwable err) {
			System.out.println(err);
			return false;
		}
		switch (translated_number) {
			case 1:
				items_to_ignore = grocery_items_to_ignore;
				break;
			case 2:
				items_to_ignore = home_items_to_ignore;
				break;
			default:
				return false;
		}
		for (String item_to_ignore: items_to_ignore) {
			if (element_text.indexOf(item_to_ignore) >= 0) {
				return true;
			}
		}
		return false;
	}

	private StringBuilder incrementSelectorDigit(final StringBuilder selector) {
		StringBuilder new_selector = new StringBuilder(selector.toString());
		int opening_brace_index = new_selector.indexOf("(");
		int closing_brace_index = new_selector.indexOf(")", opening_brace_index);
		String number_between_brackets = new_selector.substring(
			(opening_brace_index + 1), closing_brace_index
		);
		int translated_number = Integer.parseInt(number_between_brackets);
		translated_number++;
		new_selector.replace(
			(opening_brace_index + 1), closing_brace_index, Integer.toString(translated_number)
		);
		return new_selector;
	}

	private StringBuilder changeSelectorDigit(final StringBuilder selector, int digit) {
		StringBuilder new_selector = new StringBuilder(selector.toString());
		int opening_brace_index = new_selector.indexOf("(");
		int closing_brace_index = new_selector.indexOf(")", opening_brace_index);
		String number_between_brackets = new_selector.substring(
			(opening_brace_index + 1), closing_brace_index
		);
		new_selector.replace(
			(opening_brace_index + 1), closing_brace_index, Integer.toString(digit)
		);
		return new_selector;
	}

	public void scrapeAllPrices(String city) {
		List<HashMap<String, String>> prices = new ArrayList<HashMap<String, String>>();
		StringBuilder product_label_selector = new StringBuilder(
			this.configurations.getProperty("product_label_selector")
		);
		StringBuilder product_pricing_info_selector = new StringBuilder(
			this.configurations.getProperty("product_pricing_info_selector")
		);
		StringBuilder next_button_selector = new StringBuilder(
			this.configurations.getProperty("next_button_selector")
		);
		StringBuilder product_parent_container_selector = new StringBuilder(
			this.configurations.getProperty("product_parent_container_selector")
		);
		By next_button_locator = new By.ByCssSelector(next_button_selector.toString());
		By product_label_locator = new By.ByCssSelector(product_label_selector.toString());
		By product_price_info_locator = new By.ByCssSelector(product_pricing_info_selector.toString());
		By product_parent_container_locator = new By.ByCssSelector(
			product_parent_container_selector.toString()
		);
		HashMap<String, String> price_data = new HashMap<>();
		boolean next_button_interactable = true;
		boolean at_bottom = false;
		String next_button_disabled;
		while (next_button_interactable) {
			if (at_bottom) {
				WebElement next_button = this.fluentWait(
					next_button_locator, this.driver, 5, 250L
				);
				// Below javascript code from: https://stackoverflow.com/questions/42982950/how-to-scroll-down-the-page-till-bottomend-page-in-the-selenium-webdriver
				JavascriptExecutor js = (JavascriptExecutor) this.driver;
				js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
				new Actions(this.driver)
					.moveToElement(next_button)
					.click()
					.pause(Duration.ofMillis(500))
					.perform();
				product_label_selector = this.changeSelectorDigit(product_label_selector, 1);
				product_pricing_info_selector = this.changeSelectorDigit(
					product_pricing_info_selector, 1
				);
				product_parent_container_selector = this.changeSelectorDigit(
					product_parent_container_selector, 1
				);
				product_label_locator = new By.ByCssSelector(product_label_selector.toString());
				product_price_info_locator = new By.ByCssSelector(product_pricing_info_selector.toString());
				product_parent_container_locator = new By.ByCssSelector(
					product_parent_container_selector.toString()
				);
			}
			while (this.elementExists(product_label_locator, this.driver, 10, 500L)) {
				WebElement product_label = this.fluentWait(
					product_label_locator, this.driver, 5, 250L
				);
				String product_label_text = product_label.getText();
				WebElement pricing_info = this.fluentWait(
					product_price_info_locator, this.driver, 5, 250L
				);
				WebElement product_parent_container = this.fluentWait(
					product_parent_container_locator, this.driver, 5, 250L
				);
				JavascriptExecutor js = (JavascriptExecutor) this.driver;
				String script_code = "document.querySelector(\"" + product_parent_container_selector.toString() + "\").scrollIntoView(true);";
				js.executeScript(script_code);
				String pricing_info_text = pricing_info.getText();
				price_data = new HashMap<>();
				price_data.put("price_label", product_label_text);
				price_data.put("price_info", pricing_info_text);
				price_data.put("city_of_pruce", city);
				prices.add(price_data);
				product_label_selector = this.incrementSelectorDigit(product_label_selector);
				product_pricing_info_selector = this.incrementSelectorDigit(product_pricing_info_selector);
				product_parent_container_selector = this.incrementSelectorDigit(
					product_parent_container_selector
				);
				product_label_locator = new By.ByCssSelector(product_label_selector.toString());
				product_price_info_locator = new By.ByCssSelector(product_pricing_info_selector.toString());
				product_parent_container_locator = new By.ByCssSelector(
					product_parent_container_selector.toString()
				);
			}
			WebElement next_items_button = this.fluentWait(
				next_button_locator, this.driver, 5, 250L
			);
			next_button_disabled = next_items_button.getAttribute("disabled");
			next_button_interactable = !(Boolean.parseBoolean(next_button_disabled));
			at_bottom = true;
		}
	}

	public void gatherPricesForTownship(String township) throws InterruptedException {
		String location_button_selector = this.configurations.getProperty("location_button");
		String change_location_button_text = this.configurations.getProperty("change_location_button_text");
		this.tryClickingElement(new By.ByCssSelector(location_button_selector), this.driver, 30, 1000L);
		this.tryClickingElement(
			new By.ByPartialLinkText(change_location_button_text), this.driver, 30, 1000L
		);
		WebElement location_input = this.fluentWait(
			new By.ByCssSelector(this.configurations.getProperty("location_input_field")),
			this.driver, 30, 1000L
		);
		new Actions(this.driver)
			.moveToElement(location_input)
			.click()
			.sendKeys(township)
			.pause(Duration.ofMillis(1000))
			.sendKeys(Keys.ENTER)
			.pause(Duration.ofMillis(500))
			.perform();
		String browse_location_button_selector = this.configurations.getProperty("browse_location_button_selector");
		this.tryClickingElement(
			new By.ByCssSelector(browse_location_button_selector), this.driver, 30, 1000L
		);
		try {
			boolean location_confirmed_text_exists = this.elementExists(
				new By.ByCssSelector(this.configurations.getProperty("location_confirmed_heading_selector")),
				this.driver, 30, 500L
			);
			if (location_confirmed_text_exists) {
				this.tryClickingElement(
					new By.ByCssSelector(this.configurations.getProperty("close_location_confirmation_popup_selector")),
					this.driver, 30, 500L
				);
			}
		} catch (Exception err) {
			boolean location_confirmed = this.elementExists(
				new By.ByCssSelector(
					this.configurations.getProperty("location_confirmed_heading_selector")
				),
				this.driver, 30, 500L
			);
			if (!location_confirmed) {
				throw err;
			}
		}
		String main_menu_item_selector = this.configurations.getProperty("main_menu_item_selector");
		String submenu_item_selector_in_main_menu = this.configurations.getProperty(
			"submenu_item_selector_in_main_menu"
		);
		WebElement main_menu_item = this.fluentWait(
			new By.ByCssSelector(main_menu_item_selector),
			this.driver, 30, 1000L
		);
		new Actions(this.driver).moveToElement(main_menu_item).pause(Duration.ofMillis(200)).click().perform();
		WebElement submenu_item = this.fluentWait(
			new By.ByCssSelector(submenu_item_selector_in_main_menu),
			this.driver, 30, 1000L
		);
		new Actions(this.driver).moveToElement(submenu_item).pause(Duration.ofMillis(200)).click().perform();
		WebElement fresh_fruits_button = this.fluentWait(
			new By.ByCssSelector(this.configurations.getProperty("first_item_under_second_submenu")),
			this.driver, 30, 1000L
		);
		new Actions(this.driver)
			.moveToElement(fresh_fruits_button)
			.pause(Duration.ofMillis(200))
			.click()
			.pause(Duration.ofSeconds(2))
			.perform();
		String see_all_link_selector = this.configurations.getProperty("see_all_link_selector");
		WebElement see_all_button = this.fluentWaitTillVisibleandClickable(
			new By.ByCssSelector(see_all_link_selector), this.driver, 30, 1000L
		);
		new Actions(this.driver)
			.moveToElement(see_all_button)
			.pause(Duration.ofMillis(200))
			.click()
			.pause(Duration.ofSeconds(3))
			.perform();
		this.scrapeAllPrices(township);
		this.driver.get(this.configurations.getProperty("url"));
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
		Set<String> unique_cities = this.cities.keySet();
		for (String city: unique_cities) {
			this.gatherPricesForTownship(city);
		}
		this.driver.quit();
		return this.configurations.getProperty("url");
	}
}

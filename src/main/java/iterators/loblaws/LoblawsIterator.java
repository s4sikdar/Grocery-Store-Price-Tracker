package iterators.loblaws;
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
import iterators.GroceryStorePriceScraper;
import iterators.xml.*;



public class LoblawsIterator implements GroceryStorePriceScraper {
	private boolean event_reader_opened;
	private String fpath;
	private Properties configurations;
	private WebDriver driver;
	private HashMap<String, Boolean> cities;
	private XMLParser xml_parser;
	private int hours;
	private int minutes;
	private LocalTime ending_time;
	private boolean timer_started;
	private boolean privacy_policy_button_removed;


	public LoblawsIterator(String config_file_path) {
		this.setUpConfigAndXML(config_file_path);
		this.hours = 0;
		this.minutes = 0;
	}


	public LoblawsIterator(String config_file_path, int hours) {
		this.setUpConfigAndXML(config_file_path);
		this.hours = hours;
		this.minutes = 0;
	}


	public LoblawsIterator(String config_file_path, int hours, int minutes) {
		this.setUpConfigAndXML(config_file_path);
		this.hours = hours;
		this.minutes = minutes;
	}


	/**
	 * setUpConfigAndXML - a private helper method to set up access to the configuration file (a .properties
	 * file), and load the XML parser as well (created to avoid code re-use in the constructors)
	 * @param config_file_path - a String representing the File path to the .properties file
	 * @return - returns nothing (void)
	 */
	private void setUpConfigAndXML(String config_file_path) {
		this.cities = new HashMap<>();
		this.fpath = config_file_path;
		this.driver = null;
		this.event_reader_opened = false;
		this.timer_started = false;
		this.privacy_policy_button_removed = false;
		File filename = new File(this.fpath);
                this.configurations = new Properties();
		try {
			this.configurations.load(new FileInputStream(config_file_path));
			String xml_filename = this.configurations.getProperty("data_xml_filename");
			String root_tag = this.configurations.getProperty("root_xml_tag");
			String mapping_tag = this.configurations.getProperty("mapping_tag");
			this.xml_parser = new XMLParser(xml_filename, root_tag, mapping_tag, true);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}


	/**
	 * timeLimitExists - a private helper method to determine if there is a time limit (if this.hours is 0 and
	 * this.minutes is 0)
	 * @return - returns true if there is no time limit (if this.hours and this.minutes are both 0), returns
	 * false otherwise
	 */
	private boolean timeLimitExists() {
		return !((this.minutes == 0) && (this.hours == 0));
	}


	/**
	 * startTime - a private helper method to start the timer if a time limit has been specified by
	 * this.timeLimitExists() evaluating to true
	 * @return - returns nothing (void)
	 */
	private void startTimer() {
		if ((this.timeLimitExists()) && (!this.timer_started)) {
			this.ending_time = LocalTime.now();
			this.ending_time.plus(this.hours, ChronoUnit.HOURS);
			this.ending_time.plus(this.minutes, ChronoUnit.MINUTES);
			this.timer_started = true;
		}
	}


	/**
	 * timeUp - a private helper method that returns a boolean to determine if time is up or not, based on
	 * the values of this.hours and this.minutes (assuming that this.timeLimitExists() evaluates to true)
	 * @return - returns true if time is up, returns false otherwise
	 */
	private boolean timeUp() {
		return LocalTime.now().isAfter(this.ending_time);
	}


	/**
	 * pauseThenClick: a private helper method that moves the mouse over an element (passed in), pauses
	 * for a duration of milliseconds (passed in parameter), and then clicks the element
	 * - it first scrolls the element into view using the org.openqa.selenium.JavascriptExecutor class
	 *   via the executeScript method (necessary if the element is not currently in the viewport)
	 * @param element - the element you want to hover and click on (is an instance of
	 * org.openqa.selenium.WebElement)
	 * @param pause_timeout - an integer representing the number of milliseconds to pause
	 * (using Duration.ofMillis)
	 * @return - returns nothing (void)
	 * */
	private void pauseThenClick(WebElement element, int pause_timeout) {
		JavascriptExecutor js = (JavascriptExecutor) this.driver;
		js.executeScript("arguments[0].scrollIntoView(false);", element);
		try{
			new Actions(this.driver)
				.moveToElement(element)
				.pause(Duration.ofMillis(pause_timeout))
				.click().perform();
		} catch (org.openqa.selenium.interactions.MoveTargetOutOfBoundsException e) {
			js.executeScript("arguments[0].scrollIntoView(true);", element);
			new Actions(this.driver)
				.moveToElement(element)
				.pause(Duration.ofMillis(pause_timeout))
				.click().perform();
		}
	}


	/**
	 * pauseThenClickThenPause: a private helper method that moves the mouse over an element (passed in),
	 * pauses for a duration of milliseconds (passed in parameter), and then clicks the element
	 * - it first scrolls the element into view using the org.openqa.selenium.JavascriptExecutor class
	 *   via the executeScript method (necessary if the element is not currently in the viewport)
	 * @param element - the element you want to hover and click on (is an instance of
	 * org.openqa.selenium.WebElement)
	 * @param pause_timeout - an integer representing the number of milliseconds to pause
	 * (using Duration.ofMillis)
	 * @param post_click_timeout - an integer representing the number of milliseconds to pause
	 * (using Duration.ofMillis) after clicking the element
	 * @return - returns nothing (void)
	 * */
	private void pauseThenClickThenPause(WebElement element, int pause_timeout, int post_click_timeout) {
		JavascriptExecutor js = (JavascriptExecutor) this.driver;
		js.executeScript("arguments[0].scrollIntoView(false);", element);
		new Actions(this.driver)
			.moveToElement(element)
			.pause(Duration.ofMillis(pause_timeout))
			.click()
			.pause(Duration.ofMillis(post_click_timeout))
			.perform();
	}


	/**
	 * fluentWait - a private helper method that waits for an element to be available, and then returns
	 * a reference to the element
	 * The code was taken from the below stackoverflow link:
	 * https://stackoverflow.com/questions/12858972/how-can-i-ask-the-selenium-webdriver-to-wait-for-few-seconds-in-java
	 * @param locator - the locator that you will use to locate the element (this is an instance of
	 * org.openqa.selenium.By)
	 * @param driver - an instance of org.openqa.selenium.WebDriver representing a reference to the
	 * webdriver used
	 * @param timeout_duration - an integer that represents how long to wait for the element in seconds
	 * @param polling_duration - an instance of java.lang.Long that represents how long to wait for
	 * the element in milliseconds
	 * @return an instance of the web element that you are looking to find with the locator parameter
	 * @throws org.openqa.selenium.NoSuchElementException (throws this exception if the element is not
	 * found in timeout_duration number of seconds)
	 * */
	private WebElement fluentWait(final By locator, WebDriver driver, int timeout_duration, long polling_duration) {
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


	/**
	 * fluentWaitTillVisibleandClickable - a private helper method that waits for an element to be
	 * present in the Document Object Model of an HTML page as well as visible (visible on the page
	 * and having a height and width being greater than 0), then returning a reference to the element
	 * The code was taken from the below stackoverflow link:
	 * https://stackoverflow.com/questions/12858972/how-can-i-ask-the-selenium-webdriver-to-wait-for-few-seconds-in-java
	 * @param locator - the locator that you will use to locate the element (this is an instance of
	 * org.openqa.selenium.By)
	 * @param driver - an instance of org.openqa.selenium.WebDriver representing a reference to the
	 * webdriver used
	 * @param timeout_duration - an integer that represents how long to wait for the element in seconds
	 * @param polling_duration - an instance of java.lang.Long that represents how long to wait for
	 * the element in milliseconds
	 * @return an instance of the web element that you are looking to find with the locator parameter
	 * @throws org.openqa.selenium.NoSuchElementException (throws this exception if the element is not
	 * found in timeout_duration number of seconds)
	 * */
	private WebElement fluentWaitTillVisibleandClickable(final By locator, WebDriver driver, int timeout_duration, long polling_duration) {
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


	/**
	 * elementExists - a private helper method that waits for an element to be available, and then
	 * returns true if the element is found within the given timeout duration, returns false otherwise
	 * @param locator - the locator that you will use to locate the element (this is an instance of
	 * org.openqa.selenium.By)
	 * @param driver - an instance of org.openqa.selenium.WebDriver representing a reference to the
	 * webdriver used
	 * @param timeout_duration - an integer that represents how long to wait for the element in seconds
	 * @param polling_duration - an instance of java.lang.Long that represents how long to wait for
	 * the element in milliseconds
	 * @return true if the element was found, false otherwise
	 * */
	private boolean elementExists(final By locator, WebDriver driver, int timeout_duration, long polling_duration) {
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


	/**
	 * elementExistsAndIsInteractable - a private helper method that waits for an element to be
	 * present in the Document Object Model of an HTML page as well as visible (visible on the page
	 * and having a height and width being greater than 0)
	 * - then returns true if the element is found within the given time out duration
	 *   (returns false otherwise)
	 * @param locator - the locator that you will use to locate the element (this is an instance of
	 * org.openqa.selenium.By)
	 * @param driver - an instance of org.openqa.selenium.WebDriver representing a reference to the
	 * webdriver used
	 * @param timeout_duration - an integer that represents how long to wait for the element in seconds
	 * @param polling_duration - an instance of java.lang.Long that represents how long to wait for
	 * the element in milliseconds
	 * @return true if the element was found, false otherwise
	 * */
	private boolean elementExistsAndIsInteractable(final By locator, WebDriver driver, int timeout_duration, long polling_duration) {
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


	/**
	 * tryClickingElement - a private method to try and click an element you wish to find with
	 * the locator parameter as the locator
	 * - It will first try to find and then click the element, and if an error is thrown it checks if
	 *   the element is there
	 * - If the element is not there, then do nothing, otherwise get the element and try to click on it
	 *   using an ActionChain (throw the error if this fails)
	 * @param locator - the locator that you will use to locate the element (this is an instance of
	 * org.openqa.selenium.By)
	 * @param driver - an instance of org.openqa.selenium.WebDriver representing a reference to the
	 * webdriver used
	 * @param timeout_duration - an integer that represents how long to wait for the element in seconds
	 * @param polling_duration - an instance of java.lang.Long that represents how long to wait for
	 * the element in milliseconds
	 * @return - returns nothing (void)
	 * @throws org.openqa.selenium.interactions.MoveTargetOutOfBoundsException if the element is not in
	 * the viewport (when running Actions(driver).moveToElement(target_element).click().perform();)
	 * */
	private void tryClickingElement(
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
					new Actions(driver).moveToElement(target_element).click().perform();
				} catch (Throwable second_err) {
					throw second_err;
				}
			}
		}
	}


	/**
	 * citiesFileExists - a private helper method that determines if the xml file storing townships exists,
	 * and returns true if this file exists
	 * @return - returns true if the file exists, returns false otherwise
	 */
	private boolean citiesFileExists() {
		String currentPath = System.getProperty("user.dir");
		Path pwd = Paths.get(currentPath);
		String fname_for_cities_left = this.configurations.getProperty("cities_left_fname");
		Path xml_path = pwd.resolve(fname_for_cities_left);
		File xml_cities_file = new File(xml_path.toString());
		boolean xml_cities_file_exists = xml_cities_file.exists();
		return xml_cities_file_exists;
	}


	/**
	 * removePrivacyPolicyButon - a private method that checks if the privacy policy is there on the screen,
	 * and clicks the button to close it
	 * - the method also maximizes the window
	 * @return - returns nothing (void)
	 */
	private void removePrivacyPolicyButon() {
		this.driver.manage().window().maximize();
		String privacy_policy_selector = this.configurations.getProperty("privacy_policy_selector");
		boolean element_exists = this.elementExists(
			new By.ByCssSelector(privacy_policy_selector), this.driver, 60, 1000L
		);
		if (element_exists) {
			WebElement privacy_policy_close_button = this.fluentWait(
				new By.ByCssSelector(privacy_policy_selector), this.driver, 30, 1000L
			);
			privacy_policy_close_button.click();
		}
		this.privacy_policy_button_removed = true;
	}


	/**
	 * getAllCities - a private helper method that searches for all cities in Canada where there is at
	 * least one store
	 * - when you try to change your store location on websites of grocery chains owned by Loblaws, there is
	 *   a search bar on a new page with a list of all store locations across Canada
	 * - This method will scrape the list of all store locations to get all unique cities listed within this
	 *   list of stores, and each entry will be added in this.cities
	 *  @return - returns nothing (void)
	 * */
	private void getAllCities() throws InterruptedException, XMLStreamException {
		boolean not_included = true;
		String fname_for_cities_left = this.configurations.getProperty("cities_left_fname");
		String root_tag_name = this.configurations.getProperty("root_cities_tag");
		String city_tag_name = this.configurations.getProperty("individual_city_tag");
		XMLParser cities_file_xml_stream = new XMLParser(
			fname_for_cities_left, root_tag_name, city_tag_name
		);
		String location_button_selector = this.configurations.getProperty("location_button");
		String change_location_selector = this.configurations.getProperty("change_location_button");
		WebElement location_button = this.fluentWait(
			new By.ByCssSelector(location_button_selector), this.driver, 30, 1000L
		);
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
		if (!this.privacy_policy_button_removed) {
			this.removePrivacyPolicyButon();
		}
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
			Set<String> cities_listed = this.cities.keySet();
			not_included = true;
			for (String city: cities_listed) {
				if (city.equalsIgnoreCase(city_province_combination)) {
					not_included = false;
				}
			}
			if (not_included) {
				this.cities.put(city_province_combination, new Boolean(true));
			}
			int num_index = store_info_container_selector.indexOf(Integer.toString(counter));
			int num_length = Integer.toString(counter).length();
			counter++;
			store_info_container_selector.replace(
				num_index, (num_index + num_length), Integer.toString(counter)
			);
			if (this.timeLimitExists()) {
				if (not_included) {
					cities_file_xml_stream.createXMLNode(city_tag_name, city_province_combination);
				}
			}
		}
		if (this.timeLimitExists()) {
			cities_file_xml_stream.closeProductXmlOutputStream();
		}
	}


	/**
	 * menuItemToBeIgnored - a private helper method to check if the text of the passed in WebElement is the
	 * text of a main menu item that should be ignored, or if the menu item should be clicked on
	 * - the passed in WebElement represents a clickable button or anchor tag that represents a main menu item
	 *   to further expand
	 * - the passed in WebElement should have text inside it
	 * @param element - a WebElement instance representing a main menu item to further expand. The WebElement
	 * should represent a clickable button or anchor tag that has text inside it.
	 * @return - returns true if the menu item should be ignored, returns false otherwise
	 * */
	private boolean menuItemToBeIgnored(WebElement element) {
		String element_text = element.getText();
		boolean string_is_contained;
		String main_menu_items_to_ignore = this.configurations.getProperty("main_menu_items_to_ignore");
		String[] items_to_ignore = main_menu_items_to_ignore.split(";");
		for (String item_to_ignore: items_to_ignore) {
			string_is_contained = element_text.toLowerCase().contains(item_to_ignore.toLowerCase());
			if (string_is_contained) {
				return true;
			}
		}
		return false;
	}


	/**
	 * submenuItemToBeIgnored - a private helper method that determines if the current submenu item is to be
	 * ignored, based on its text and whether the text falls under a submenu item that is to be ignored
	 * - It uses a number in the css selector passed in to determine if this is the first submenu of items
	 *   (under groceries) or the second submenu of items (under home beauty items)
	 * - Based on this, the text is compared with the right list of phrases, to determine if this is a submenu
	 *   item that should be ignored
	 * - The list of phrases are comma-separated-values from the properties files
	 * @param element - a WebElement instance that has the text you wish to search
	 * @param css_selector - a String that represents the css selector of the submenu item inside the
	 * main menu
	 * @return - returns true if the submenu item should be ignored, returns false otherwise
	 */
	private boolean submenuItemToBeIgnored(WebElement element, String css_selector) {
		String element_text = element.getText();
		String grocery_submenu_items_to_ignore = this.configurations.getProperty("grocery_sub_menu_items_to_ignore");
		String home_beauty_items_to_ignore = this.configurations.getProperty("home_beauty_submenu_items_to_ignore");
		String[] grocery_items_to_ignore = grocery_submenu_items_to_ignore.split(";");
		String[] home_items_to_ignore = home_beauty_items_to_ignore.split(";");
		String[] items_to_ignore = grocery_items_to_ignore;
		int opening_brace_index = css_selector.indexOf("(");
		int closing_brace_index = css_selector.indexOf(")", opening_brace_index);
		boolean string_is_contained;
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
			string_is_contained = element_text.toLowerCase().contains(item_to_ignore.toLowerCase());
			if (string_is_contained) {
				return true;
			}
		}
		return false;
	}


	/**
	 * getNumFromConfigurationsFile - a private helper method that gets a value from the properties file
	 * based on the configuration variable to look for (specified by config_var_name)
	 * - It then tries to cast the string value into an integer, and then returns it
	 * - The value read in from the configuration file must be a string that represents a number (example:
	 *   "123" or "1") or this function will throw an error when trying to parse the integer
	 * @param config_var_name = a constant String representing the name of the variable in the properties file
	 * to look for (the variable must be in the configuration file, or this method will likely throw an error)
	 * @return - returns the parsed integer read in from the configuration file
	 */
	private int getNumFromConfigurationsFile(final String config_var_name) {
		String value = this.configurations.getProperty(config_var_name);
		return Integer.parseInt(value);
	}


	/**
	 * incrementSelectorDigit - a private helper method that gets a number from inside of selector (starting
	 * from index starting_index) and then returns a copy of selector with the number incremented
	 * @param selector - a constant StringBuilder representing a css selector that has a number in it 
	 * in between round brackets (this number must be at an index that is greater than or equal to
	 * starting_index plus one, and the StringBuilder must have a length of at least 3)
	 * @param starting_index - an integer representing the starting index from where to look for the number
	 * (starting_index must be greater than or equal to 0, and less than the length of the string derived
	 * from selector)
	 * @return - returns a new StringBuilder instance with the integer incremented if the integer and the
	 * surrounding round brackets can be found, returns a copy of the passed in StringBuilder from selector
	 * otherwise
	 */
	private StringBuilder incrementSelectorDigit(final StringBuilder selector, int starting_index) {
		assert (selector.toString().length() >= 3);
		assert ((0 <= starting_index) && (starting_index < (selector.toString().length())));
		StringBuilder new_selector = new StringBuilder(selector.toString());
		int opening_brace_index = new_selector.indexOf("(", starting_index);
		if (opening_brace_index == -1) {
			return new_selector;
		}
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


	/**
	 * incrementSelectorDigit - a private helper method that gets a number from inside of selector and
	 * then returns a copy of selector with the number incremented (the number is inside round brackets)
	 * @param selector - a constant StringBuilder representing a css selector that has a number in it 
	 * in between round brackets
	 * @return - returns a new StringBuilder instance with the integer incremented if the integer and the
	 * surrounding round brackets can be found, returns a copy of the passed in StringBuilder from selector
	 * otherwise
	 */
	private StringBuilder incrementSelectorDigit(final StringBuilder selector) {
		assert (selector.toString().length() >= 3);
		StringBuilder new_selector = new StringBuilder(selector.toString());
		int opening_brace_index = new_selector.indexOf("(");
		if (opening_brace_index == -1) {
			return new_selector;
		}
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


	/**
	 * changeSelectorDigit - a private helper method that gets a number from inside of selector and
	 * then returns a copy of selector with the number replaced by digit
	 * @param selector - a constant StringBuilder representing a css selector that has a number in it 
	 * in between round brackets
	 * @return - returns a new StringBuilder instance with the integer incremented if the integer and the
	 * surrounding round brackets can be found, returns a copy of the passed in StringBuilder from selector
	 * otherwise
	 */
	private StringBuilder changeSelectorDigit(final StringBuilder selector, int digit) {
		assert (selector.toString().length() >= 3);
		StringBuilder new_selector = new StringBuilder(selector.toString());
		int opening_brace_index = new_selector.indexOf("(");
		if (opening_brace_index == -1) {
			return new_selector;
		}
		int closing_brace_index = new_selector.indexOf(")", opening_brace_index);
		String number_between_brackets = new_selector.substring(
			(opening_brace_index + 1), closing_brace_index
		);
		new_selector.replace(
			(opening_brace_index + 1), closing_brace_index, Integer.toString(digit)
		);
		return new_selector;
	}


	/**
	 * changeSelectorDigit - a private helper method that gets a number from inside of selector and
	 * then returns a copy of selector with the number replaced by digit
	 * - the index of the number has to be at least starting_index plus one
	 * @param selector - a constant StringBuilder representing a css selector that has a number in it 
	 * in between round brackets (the resulting string of the selector must have a length of at least 3)
	 * @param starting_index - an integer representing the index of where to start looking for the opening
	 * round bracket for which the number is stored inside (i.e. where to start looking for "(" in "(3)")
	 * - starting_index has to be at least 0 and less than the length of the resulting string produced
	 *   from selector
	 * @return - returns a new StringBuilder instance with the integer incremented if the integer and the
	 * surrounding round brackets can be found, returns a copy of the passed in StringBuilder from selector
	 * otherwise
	 */
	private StringBuilder changeSelectorDigit(final StringBuilder selector, int digit, int starting_index) {
		assert (selector.toString().length() >= 3);
		assert ((0 <= starting_index) && (starting_index < (selector.toString().length())));
		StringBuilder new_selector = new StringBuilder(selector.toString());
		int opening_brace_index = new_selector.indexOf("(", starting_index);
		if (opening_brace_index == -1) {
			return new_selector;
		}
		int closing_brace_index = new_selector.indexOf(")", opening_brace_index);
		String number_between_brackets = new_selector.substring(
			(opening_brace_index + 1), closing_brace_index
		);
		new_selector.replace(
			(opening_brace_index + 1), closing_brace_index, Integer.toString(digit)
		);
		return new_selector;
	}


	/**
	 * scrapeProductInfo - a private helper method that scrapes all of the information available on the
	 * product and returns a HashMap<String, String> instance with all available product information
	 * - This method will open the link in a new tab, and bring that tab into focus, the link being to
	 *   the information page for that product (its CSS selector being the resulting string of
	 *   product_info_link_selector)
	 * - After gathering all product information, the driver closes the tab and switches back to the original
	 *   tab.
	 * @param township - a string representing the town and province where the product information is being
	 * gathered (i.e. the store location was set to be a store in the town and province given in township, such
	 * as "Toronto, Ontario")
	 * @param product_info_link_selector - a StringBuilder instance that represents the CSS selector for the
	 * link to the product information page (the CSS selector for the specific <a> tag in question)
	 * @return - returns a HashMap<String, String> instance
	 */
	private HashMap<String, String> scrapeProductInfo(String township, StringBuilder product_info_link_selector) {
		HashMap<String, String> product_info = new HashMap<>();
		String brand_name_selector = this.configurations.getProperty("brand_name_selector");
		String product_name_selector = this.configurations.getProperty("product_name_selector");
		String package_size_selector = this.configurations.getProperty("package_size_selector");
		String price_value_selector = this.configurations.getProperty("price_value_selector");
		String price_unit_selector = this.configurations.getProperty("price_unit_selector");
		String comparison_price_value_selector = this.configurations.getProperty(
			"comparison_price_value_selector"
		);
		String comparison_price_unit_selector = this.configurations.getProperty(
			"comparison_price_unit_selector"
		);
		By product_info_link_locator = new By.ByCssSelector(product_info_link_selector.toString());
		By brand_name_locator = new By.ByCssSelector(brand_name_selector);
		By product_name_locator = new By.ByCssSelector(product_name_selector);
		By package_size_locator = new By.ByCssSelector(package_size_selector);
		By price_value_locator = new By.ByCssSelector(price_value_selector);
		By price_unit_locator = new By.ByCssSelector(price_unit_selector);
		By comparison_price_value_locator = new By.ByCssSelector(comparison_price_value_selector);
		By comparison_price_unit_locator = new By.ByCssSelector(comparison_price_unit_selector);
		WebElement product_info_link = this.fluentWait(
			product_info_link_locator, this.driver, 10, 100L
		);
		String href_address = product_info_link.getAttribute("href");
		String original_window = this.driver.getWindowHandle();
		driver.switchTo().newWindow(WindowType.TAB);
		driver.get(href_address);
		try {
			WebElement product_name = this.fluentWait(product_name_locator, this.driver, 30, 100L);
			WebElement price_value = this.fluentWait(price_value_locator, this.driver, 30, 100L);
			WebElement price_unit = this.fluentWait(price_unit_locator, this.driver, 30, 100L);
			String price_info = price_value.getText() + " " + price_unit.getText();
			product_info.put("product_name", product_name.getText());
			product_info.put("price_info", price_info);
			product_info.put("township_location", township);
			boolean brand_name_exists = this.elementExists(brand_name_locator, this.driver, 1, 100L);
			boolean package_size_exists = this.elementExists(package_size_locator, this.driver, 1, 100L);
			boolean comparison_price_value_exists = this.elementExists(
				comparison_price_value_locator, this.driver, 2, 100L
			);
			boolean comparison_price_unit_exists = this.elementExists(
				comparison_price_unit_locator, this.driver, 2, 100L
			);
			if (brand_name_exists) {
				WebElement brand_name = this.driver.findElement(brand_name_locator);
				product_info.put("brand_name", brand_name.getText());
			}
			if (package_size_exists) {
				WebElement package_size_info = this.driver.findElement(package_size_locator);
				product_info.put("package_size", package_size_info.getText());
			}
			if (comparison_price_value_exists && comparison_price_unit_exists) {
				WebElement comparison_price_value = this.fluentWait(
					comparison_price_value_locator, this.driver, 5, 100L
				);
				WebElement comparison_price_unit = this.fluentWait(
					comparison_price_unit_locator, this.driver, 5, 100L
				);
				String comparison_unit_price = comparison_price_value.getText() + " "
								+ comparison_price_unit.getText();
				product_info.put("unit_price_for_comparison", comparison_unit_price);
			}
			driver.close();
			driver.switchTo().window(original_window);
		} catch (Throwable err) {
			driver.close();
			driver.switchTo().window(original_window);
			return product_info;
		}
		String date_pattern = "-MMM-dd-yyyy-HH-mm";
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(date_pattern);
		LocalDateTime current_time = LocalDateTime.now();
		String formatted_date = current_time.format(formatter);
		product_info.put("date", formatted_date);
		return product_info;
	}


	/**
	 * scrapeProductInfoWithRetries - a helper method to scrape product information with a given number of
	 * retries in case the page doesn't load the first time
	 * - after the given number of retries, return an empty hashmap as the result
	 * @param township - a string representing the township where the price is being scraped (will be added to
	 * the hashmap of product info)
	 * @param product_info_link_selector - a StringBuilder representing the CSS Selector for the link to more
	 * information about the product (will be opened in a separate tab, and the information will be scraped)
	 * @param retries - an integer representing the number of retries that are left to scrape the product's
	 * information (must be greater than or equal to 0)
	 */
	private HashMap<String, String> scrapeProductInfoWithRetries(
			String township, StringBuilder product_info_link_selector, int retries
	) {
		assert(retries >= 0);
		HashMap<String, String> information = this.scrapeProductInfo(township, product_info_link_selector);
		if (information.isEmpty()) {
			if (retries == 0) {
				HashMap<String, String> result = new HashMap<>();
				return result;
			}
			return this.scrapeProductInfoWithRetries(township, product_info_link_selector, (retries - 1));
		} else {
			return information;
		}
	}


	/**
	 * scrapeAllPrices - a private method that goes through all the listed products in a subcategory and then
	 * gathers a HashMap of information on each product
	 * - the HashMap of information is added to the XML as an XML tag with the keys as subtags (their values
	 *   being the values in the subtags)
	 * - If there are multiple pages of products in a subcategory, then the method will scrape information for
	 *   all products
	 * @param city - a String representing the city and province combination where the product information
	 * is being scraped (i.e. prices are different in stores in different cities within Canada, and the city
	 * represents the city and province combination where you selected a store location)
	 * @return - returns nothing (void)
	 */
	private void scrapeAllPrices(String city) throws XMLStreamException {
		StringBuilder product_info_link_selector = new StringBuilder(
			this.configurations.getProperty("product_separate_page_link_selector")
		);
		String pagination_selector = this.configurations.getProperty("nav_pagination_element_selector");
		StringBuilder next_button_selector = new StringBuilder(
			this.configurations.getProperty("next_button_selector")
		);
		StringBuilder product_parent_container_selector = new StringBuilder(
			this.configurations.getProperty("product_parent_container_selector")
		);
		By next_button_locator = new By.ByCssSelector(next_button_selector.toString());
		By pagination_locator = new By.ByCssSelector(pagination_selector);
		By product_parent_container_locator = new By.ByCssSelector(
			product_parent_container_selector.toString()
		);
		HashMap<String, String> price_data = new HashMap<>();
		boolean next_button_interactable = true;
		boolean at_bottom = false;
		boolean pagination_exists;
		String next_button_disabled;
		JavascriptExecutor js = (JavascriptExecutor) this.driver;
		WebElement product_parent_container;
		while (next_button_interactable) {
			if (at_bottom) {
				WebElement next_button = this.fluentWait(
					next_button_locator, this.driver, 5, 250L
				);
				// Below javascript code from: https://stackoverflow.com/questions/42982950/how-to-scroll-down-the-page-till-bottomend-page-in-the-selenium-webdriver
				js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
				this.pauseThenClick(next_button, 500);
				product_parent_container_selector = this.changeSelectorDigit(
					product_parent_container_selector, 1
				);
				product_info_link_selector = this.changeSelectorDigit(
					product_info_link_selector, 1
				);
				product_parent_container_locator = new By.ByCssSelector(
					product_parent_container_selector.toString()
				);
			}
			while (this.elementExists(product_parent_container_locator, this.driver, 10, 500L)) {
				product_parent_container = this.fluentWait(
					product_parent_container_locator, this.driver, 5, 250L
				);
				js.executeScript("arguments[0].scrollIntoView(true);", product_parent_container);
				price_data = this.scrapeProductInfoWithRetries(city, product_info_link_selector, 3);
				if (!(price_data.isEmpty())) {
					this.xml_parser.hashmapToXML(price_data);
				}
				product_parent_container_selector = this.incrementSelectorDigit(
					product_parent_container_selector
				);
				product_info_link_selector = this.incrementSelectorDigit(product_info_link_selector);
				product_parent_container_locator = new By.ByCssSelector(
					product_parent_container_selector.toString()
				);
			}
			pagination_exists = this.elementExists(pagination_locator, this.driver, 5, 250L);
			if (!pagination_exists) {
				next_button_interactable = false;
				continue;
			}
			WebElement next_items_button = this.fluentWait(
				next_button_locator, this.driver, 5, 250L
			);
			next_button_disabled = next_items_button.getAttribute("disabled");
			next_button_interactable = !(Boolean.parseBoolean(next_button_disabled));
			at_bottom = true;
		}
	}


	/**
	 * changeLocation - a private helper method that is responsible for changing the location of the grocery
	 * store
	 * - Selects the button to change the location, and then enters the String township in the search bar
	 * - Once all available locations in that township are shown, select the first available one
	 * - Once the location is selected, a modal shows and a dialog shows, which confirms that the location
	 *   has been changed
	 * - Once the dialog shows, click the button to close the dialog
	 * @param township - a String representing the city and province combination that the store's location
	 * is being changed to
	 * @return - returns nothing (void)
	 */
	private void changeLocation(String township) {
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
	}


	/**
	 * gatherPricesForTownship - a private helper method that will change the store location to one being in
	 * a given township, and then for that store it will go over all categories of products, and all products
	 * in all subcategories of those categories
	 * - essentially scrapes all product information (product label, price, unit price, etc.) for a store in
	 *   a given township, and adds the results to an xml file (specified by the configuration file who's name
	 *   is passed into the constructor)
	 * @param township - a String representing the city and province combination that the store's location
	 * is being changed to
	 * @return - returns nothing (void)
	 */
	private void gatherPricesForTownship(String township) throws InterruptedException, XMLStreamException {
		int index_after_brackets;
		boolean item_to_be_ignored;
		boolean menu_item_to_select;
		boolean submenu_item_to_ignore;
		boolean submenu_item_exists;
		boolean main_menu_item_exists;
		boolean main_menu_item_to_ignore;
		int submenu_li_child_index;
		WebElement main_menu_item;
		JavascriptExecutor js = (JavascriptExecutor) this.driver;
		StringBuilder main_menu_item_selector = new StringBuilder(
			this.configurations.getProperty("main_menu_item_selector")
		);
		StringBuilder submenu_item_selector_in_main_menu = new StringBuilder(
			this.configurations.getProperty("submenu_item_selector_in_main_menu")
		);
		submenu_li_child_index = this.getNumFromConfigurationsFile("starting_index_for_submenu_item");
		index_after_brackets = this.getNumFromConfigurationsFile("index_after_first_bracket");
		By submenu_item_locator = new By.ByCssSelector(submenu_item_selector_in_main_menu.toString());
		By main_menu_item_locator = new By.ByCssSelector(main_menu_item_selector.toString());
		this.changeLocation(township);
		if (!this.privacy_policy_button_removed) {
			this.removePrivacyPolicyButon();
		}
		this.startTimer();
		main_menu_item_exists = this.elementExistsAndIsInteractable(
			main_menu_item_locator, this.driver, 5, 100L
		);
		while (main_menu_item_exists) {
			main_menu_item = this.fluentWait(main_menu_item_locator, this.driver, 30, 1000L);
			main_menu_item_to_ignore = this.menuItemToBeIgnored(main_menu_item);
			if (main_menu_item_to_ignore) {
				submenu_item_selector_in_main_menu = this.incrementSelectorDigit(
					submenu_item_selector_in_main_menu
				);
				submenu_item_selector_in_main_menu = this.changeSelectorDigit(
					submenu_item_selector_in_main_menu, 1, submenu_li_child_index
				);
				submenu_item_locator = new By.ByCssSelector(
					submenu_item_selector_in_main_menu.toString()
				);
				main_menu_item_selector = this.incrementSelectorDigit(main_menu_item_selector);
				main_menu_item_locator = new By.ByCssSelector(main_menu_item_selector.toString());
				main_menu_item_exists = this.elementExistsAndIsInteractable(
					main_menu_item_locator, this.driver, 5, 100L
				);
				continue;
			}
			new Actions(this.driver)
				.moveToElement(main_menu_item)
				.pause(Duration.ofMillis(200)).click().perform();
			submenu_item_exists = this.elementExistsAndIsInteractable(
				submenu_item_locator, this.driver, 30, 100L
			);
			while (submenu_item_exists) {
				WebElement submenu_item = this.fluentWait(submenu_item_locator, this.driver, 30, 1000L);
				submenu_item_to_ignore = this.submenuItemToBeIgnored(
					submenu_item, submenu_item_selector_in_main_menu.toString()
				);
				if (!submenu_item_to_ignore) {
					this.pauseThenClick(submenu_item, 200);
					StringBuilder item_under_second_submenu_selector = new StringBuilder(
						this.configurations.getProperty("item_under_second_submenu")
					);
					StringBuilder see_all_link_selector = new StringBuilder(
						this.configurations.getProperty("see_all_link_selector")
					);
					String parent_level_button_selector = this.configurations.getProperty(
						"parent_submenu_subcategory_button_selector"
					);
					By current_submenu_locator = new By.ByCssSelector(
						item_under_second_submenu_selector.toString()
					);
					By see_all_link_locator = new By.ByCssSelector(see_all_link_selector.toString());
					By parent_level_button_locator = new By.ByCssSelector(parent_level_button_selector);
					boolean element_exists = this.elementExists(
						current_submenu_locator, this.driver, 10, 1000L
					);
					while (element_exists) {
						WebElement current_submenu_button = this.fluentWait(
							current_submenu_locator, this.driver, 10, 1000L
						);
						this.pauseThenClickThenPause(current_submenu_button, 200, 200);
						WebElement see_all_button = this.fluentWaitTillVisibleandClickable(
							see_all_link_locator, this.driver, 30, 1000L
						);
						this.pauseThenClickThenPause(see_all_button, 200, 300);
						this.scrapeAllPrices(township);
						js.executeScript("window.scrollTo(0, 0);");
						WebElement parent_menu_button = this.fluentWait(
							parent_level_button_locator,this.driver, 30, 1000L
						);
						this.pauseThenClickThenPause(parent_menu_button, 200, 300);
						item_under_second_submenu_selector = this.incrementSelectorDigit(
							item_under_second_submenu_selector, index_after_brackets
						);
						see_all_link_selector = this.incrementSelectorDigit(
							see_all_link_selector, index_after_brackets
						);
						current_submenu_locator = new By.ByCssSelector(
							item_under_second_submenu_selector.toString()
						);
						see_all_link_locator = new By.ByCssSelector(see_all_link_selector.toString());
						element_exists = this.elementExists(
							current_submenu_locator, this.driver, 10, 1000L
						);
					}
				}
				submenu_item_selector_in_main_menu = this.incrementSelectorDigit(
					submenu_item_selector_in_main_menu, submenu_li_child_index
				);
				submenu_item_locator = new By.ByCssSelector(submenu_item_selector_in_main_menu.toString());
				js.executeScript("window.scrollTo(0, 0);");
				this.pauseThenClick(main_menu_item, 200);
				submenu_item_exists = this.elementExistsAndIsInteractable(submenu_item_locator, this.driver, 30, 100L);
			}
			submenu_item_selector_in_main_menu = this.incrementSelectorDigit(
				submenu_item_selector_in_main_menu
			);
			submenu_item_selector_in_main_menu = this.changeSelectorDigit(
				submenu_item_selector_in_main_menu, 1, submenu_li_child_index
			);
			submenu_item_locator = new By.ByCssSelector(submenu_item_selector_in_main_menu.toString());
			main_menu_item_selector = this.incrementSelectorDigit(main_menu_item_selector);
			main_menu_item_locator = new By.ByCssSelector(main_menu_item_selector.toString());
			main_menu_item_exists = this.elementExistsAndIsInteractable(main_menu_item_locator, this.driver, 5, 100L);
		}
		this.driver.get(this.configurations.getProperty("url"));
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
		String fname_for_cities_left = this.configurations.getProperty("cities_left_fname");
		String root_tag_name = this.configurations.getProperty("root_cities_tag");
		String city_tag_name = this.configurations.getProperty("individual_city_tag");
		DOMParser cities_xml_dom_parser = new DOMParser(fname_for_cities_left, root_tag_name, city_tag_name);
		// https://stackoverflow.com/questions/13959704/accepting-sharing-location-browser-popups-through-selenium-webdriver
		options.addPreference("geo.prompt.testing", true);
		options.addPreference("geo.prompt.testing.allow", true);
		//options.addPreference(
		//	"geo.wifi.uri", "https://location.services.mozilla.com/v1/geolocate?key=%MOZILLA_API_KEY%"
		//);
		this.driver = new FirefoxDriver(options);
		this.driver.get(this.configurations.getProperty("url"));
		boolean cities_file_exists = this.citiesFileExists();
		if (!cities_file_exists) {
			this.getAllCities();
		}
		if (this.timeLimitExists()) {
			while (cities_xml_dom_parser.hasNext()) {
				String township = cities_xml_dom_parser.next();
				this.gatherPricesForTownship(township);
				if (this.timeUp()) {
					cities_xml_dom_parser.writeXML();
					break;
				}
			}
			if (!(cities_xml_dom_parser.hasNext())) {
				cities_xml_dom_parser.delete();
			}
		} else {
			if (cities_file_exists) {
				while (cities_xml_dom_parser.hasNext()) {
					String township = cities_xml_dom_parser.next();
					this.gatherPricesForTownship(township);
					if (this.timeUp()) {
						cities_xml_dom_parser.writeXML();
						break;
					}
				}
				if (!(cities_xml_dom_parser.hasNext())) {
					cities_xml_dom_parser.delete();
				}
			} else {
				Set<String> unique_cities = this.cities.keySet();
				for (String city: unique_cities) {
					this.gatherPricesForTownship(city);
				}
			}
		}
		this.xml_parser.closeProductXmlOutputStream();
		this.driver.quit();
	}


	/**
	 * hasNext - a public method that checks if there are any more entries in the XML file to be iterated over
	 * (entries being any information sets of product data left to iterate over)
	 * @return - returns true if there are entries in the XML file left to iterate over, returns false otherwise
	 */
	public boolean hasNext() throws XMLStreamException {
		return this.xml_parser.hasNext();
	}


	/**
	 * next - a public method that parses the next information set of product data in the XML file, and returns
	 * it has a HashMap<String, String> instance
	 * @return - returns a HashMap<String, String> instance representing the current information set of product
	 * data in the XML file that has been iterated over
	 */
	public HashMap<String, String> next() throws XMLStreamException {
		return this.xml_parser.next();
	}


}

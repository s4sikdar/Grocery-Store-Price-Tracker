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
import org.openqa.selenium.PageLoadStrategy;
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



public class LoblawsIterator extends BaseIterator {
	private WebDriver driver;
	private HashMap<String, Boolean> cities;
	private ArrayList<String> categories_left;
	private ArrayList<String> subcategories_left;
	private boolean privacy_policy_button_removed;
	private boolean store_changed_for_next_round;
	private String store_name;


	public LoblawsIterator(String config_file_path) {
		super(config_file_path);
		this.setUp();
	}


	public LoblawsIterator(String config_file_path, int hours) {
		super(config_file_path, hours);
		this.setUp();
	}


	public LoblawsIterator(String config_file_path, int hours, int minutes) {
		super(config_file_path, hours, minutes);
		this.setUp();
	}


	/**
	 * setUp - a private helper method to finish set up activities
	 * @return - returns nothing (void)
	 */
	private void setUp() {
		this.cities = new HashMap<>();
		this.driver = null;
		this.categories_left = new ArrayList<String>();
		this.subcategories_left = new ArrayList<String>();
		this.privacy_policy_button_removed = false;
		this.store_changed_for_next_round = false;
		this.store_name = this.getConfigProperty("store_name");
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
		File categories_file = new File(xml_path.toString());
		boolean categories_file_exists = categories_file.exists();
		return categories_file_exists;
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
		File subcategories_file = new File(xml_path.toString());
		boolean subcategories_file_exists = subcategories_file.exists();
		return subcategories_file_exists;
	}


	/**
	 * removePrivacyPolicyButon - a private method that checks if the privacy policy is there on the screen,
	 * and clicks the button to close it
	 * - the method also maximizes the window
	 * @return - returns nothing (void)
	 */
	private void removePrivacyPolicyButon() {
		this.driver.manage().window().maximize();
		String privacy_policy_selector = this.getConfigProperty("privacy_policy_selector");
		String second_privacy_policy_selector = this.getConfigProperty(
			"second_privacy_policy_selector"
		);
		boolean first_privacy_policy_button_exists = WebElementOperations.elementExistsByJavaScript(
			this.driver, privacy_policy_selector
		);
		boolean second_privacy_policy_button_exists = WebElementOperations.elementExistsByJavaScript(
			this.driver, second_privacy_policy_selector
		);
		if (first_privacy_policy_button_exists) {
			WebElement privacy_policy_close_button = WebElementOperations.fluentWait(
				new By.ByCssSelector(privacy_policy_selector), this.driver, 30, 1000L
			);
			privacy_policy_close_button.click();
		}
		if (second_privacy_policy_button_exists) {
			WebElement second_privacy_policy_close_button = WebElementOperations.fluentWait(
				new By.ByCssSelector(second_privacy_policy_selector), this.driver, 30, 1000L
			);
			second_privacy_policy_close_button.click();
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
		String fname_for_cities_left = this.getConfigProperty("cities_left_fname");
		String root_tag_name = this.getConfigProperty("root_cities_tag");
		String city_tag_name = this.getConfigProperty("individual_city_tag");
		XMLParser cities_file_xml_stream = new XMLParser(
			fname_for_cities_left, root_tag_name, city_tag_name
		);
		String location_button_selector = this.getConfigProperty("location_button");
		String change_location_selector = this.getConfigProperty("change_location_button");
		WebElement location_button = WebElementOperations.fluentWait(
			new By.ByCssSelector(location_button_selector), this.driver, 30, 1000L
		);
		location_button.click();
		WebElement change_location_button = WebElementOperations.fluentWait(
			new By.ByCssSelector(change_location_selector), this.driver, 30, 1000L
		);
		change_location_button.click();
		WebElement location_input = WebElementOperations.fluentWait(
			new By.ByCssSelector(this.getConfigProperty("location_input_field")),
			this.driver, 30, 1000L
		);
		new Actions(this.driver)
			.moveToElement(location_input)
			.click()
			.sendKeys(this.getConfigProperty("location_input_field_value"))
			.pause(Duration.ofMillis(1000))
			.sendKeys(Keys.ENTER)
			.pause(Duration.ofMillis(500))
			.perform();
		int counter = 1;
		StringBuilder store_info_container_selector = new StringBuilder(
			this.getConfigProperty("store_content_box_selector")
		);
		StringBuilder store_location_text_selector = new StringBuilder(
			this.getConfigProperty("store_location_text_selector")
		);
		if (!this.privacy_policy_button_removed) {
			this.removePrivacyPolicyButon();
		}
		boolean store_info_container_exists = WebElementOperations.elementExists(
			new By.ByCssSelector(store_info_container_selector.toString()), this.driver, 30, 500L
		);
		while (store_info_container_exists) {
			WebElement store_info_container = WebElementOperations.fluentWait(
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
			store_info_container_exists = WebElementOperations.elementExistsByJavaScript(
				this.driver, store_info_container_selector.toString()
			);
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
		String main_menu_items_to_ignore = this.getConfigProperty("main_menu_items_to_ignore");
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
		String grocery_submenu_items_to_ignore = this.getConfigProperty("grocery_sub_menu_items_to_ignore");
		String home_beauty_items_to_ignore = this.getConfigProperty("home_beauty_submenu_items_to_ignore");
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
		String brand_name_selector = this.getConfigProperty("brand_name_selector");
		String product_name_selector = this.getConfigProperty("product_name_selector");
		String package_size_selector = this.getConfigProperty("package_size_selector");
		String price_value_selector = this.getConfigProperty("price_value_selector");
		String price_unit_selector = this.getConfigProperty("price_unit_selector");
		String comparison_price_value_selector = this.getConfigProperty(
			"comparison_price_value_selector"
		);
		String comparison_price_unit_selector = this.getConfigProperty(
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
		WebElement product_info_link = WebElementOperations.fluentWait(
			product_info_link_locator, this.driver, 10, 100L
		);
		String href_address = product_info_link.getAttribute("href");
		String original_window = this.driver.getWindowHandle();
		driver.switchTo().newWindow(WindowType.TAB);
		driver.get(href_address);
		try {
			WebElement product_name = WebElementOperations.fluentWait(
				product_name_locator, this.driver, 40, 100L
			);
			WebElement price_value = WebElementOperations.fluentWait(price_value_locator, this.driver, 30, 100L);
			WebElement price_unit = WebElementOperations.fluentWait(price_unit_locator, this.driver, 30, 100L);
			String price_info = price_value.getText() + " " + price_unit.getText();
			product_info.put("product_name", product_name.getText());
			product_info.put("price_info", price_info);
			product_info.put("township_location", township);
			boolean brand_name_exists = WebElementOperations.elementExistsByJavaScript(
				this.driver, brand_name_selector
			);
			boolean package_size_exists = WebElementOperations.elementExistsByJavaScript(
				this.driver, package_size_selector
			);
			boolean comparison_price_value_exists = WebElementOperations.elementExistsByJavaScript(
				this.driver, comparison_price_value_selector
			);
			boolean comparison_price_unit_exists = WebElementOperations.elementExistsByJavaScript(
				this.driver, comparison_price_unit_selector
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
				WebElement comparison_price_value = WebElementOperations.fluentWait(
					comparison_price_value_locator, this.driver, 5, 100L
				);
				WebElement comparison_price_unit = WebElementOperations.fluentWait(
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
	 * scrapeCompactProductInfo - private helper method that scrpaes all the product info within the page of all products
	 * listed (in other words, it does not open a new tab for the product info page)
	 * - once the information is scraped, it is put into XML
	 * @param township - the String representing the township to put in the XML with the product information
	 * @param product_parent_container - the WebElement representing the parent container with all product information
	 * @return - a HashMap<String, String> instance with product data information (i.e. price, brand name, size, etc.)
	 */
	private HashMap<String, String> scrapeCompactProductInfo(String township, WebElement product_parent_container) {
		HashMap<String, String> product_info = new HashMap<>();
		String product_price_information_selctor = this.getConfigProperty("product_price_information_selctor");
		String price_without_sale_price_selector = this.getConfigProperty("price_without_sale_price_selector");
		String regular_price_selector = this.getConfigProperty("regular_price_selector");
		String brand_selector = this.getConfigProperty("brand_selector");
		String product_title_selector = this.getConfigProperty("product_title_selector");
		String product_package_size_selector = this.getConfigProperty("product_package_size_selector");
		String breadcrumb_selector = this.getConfigProperty("breadcrumb_item_selector");
		String breadcrumb_container_selector = this.getConfigProperty("breadcrumb_container_selector");
		boolean regular_price_shown = WebElementOperations.elementExistsByJavaScript(
			this.driver, regular_price_selector, product_parent_container
		);
		boolean sale_price_shown = WebElementOperations.elementExistsByJavaScript(
			this.driver, price_without_sale_price_selector, product_parent_container
		);
		boolean brand_selector_exists = WebElementOperations.elementExistsByJavaScript(
			this.driver, brand_selector, product_parent_container
		);
		boolean product_title_exists = WebElementOperations.elementExistsByJavaScript(
			this.driver, product_title_selector, product_parent_container
		);
		boolean product_package_size_exists = WebElementOperations.elementExistsByJavaScript(
			this.driver, product_package_size_selector, product_parent_container
		);
		String price_info_selector = "";
		if (regular_price_shown) {
			price_info_selector = regular_price_selector;
		}
		if (sale_price_shown) {
			price_info_selector = price_without_sale_price_selector;
		}
		if (!(price_info_selector.isEmpty())) {
			WebElement price_info = product_parent_container.findElement(new By.ByCssSelector(price_info_selector));
			product_info.put("price", price_info.getText());
		}
		if (brand_selector_exists) {
			WebElement brand_info = product_parent_container.findElement(new By.ByCssSelector(brand_selector));
			product_info.put("brand", brand_info.getText());
		}
		if (product_title_exists) {
			WebElement product_title = product_parent_container.findElement(new By.ByCssSelector(product_title_selector));
			product_info.put("product_title", product_title.getText());
		}
		if (product_package_size_exists) {
			WebElement product_package_size_info = product_parent_container.findElement(
				new By.ByCssSelector(product_package_size_selector)
			);
			String package_size = product_package_size_info.getText();
			String[] sizing_units = package_size.split("[,.]{1}\\s*\\${1}");
			if (sizing_units.length > 1) {
				product_info.put("unit_price", "$" + sizing_units[1]);
			}
			product_info.put("size", sizing_units[0]);
		}
		WebElement breadcrumb_container = this.driver.findElement(new By.ByCssSelector(breadcrumb_container_selector));
		String category_path = WebElementOperations.getInnerText(breadcrumb_container, this.driver).replace("\n", ">");
		String date_pattern = "MMM-dd-yyyy-HH-mm";
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(date_pattern);
		LocalDateTime current_time = LocalDateTime.now();
		String formatted_date = current_time.format(formatter);
		product_info.put("date", formatted_date);
		product_info.put("store_chain_name", this.store_name);
		product_info.put("category_path", category_path);
		product_info.put("township_location", township);
		return product_info;
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
		boolean parent_container_exists;
		StringBuilder product_info_link_selector = new StringBuilder(
			this.getConfigProperty("product_separate_page_link_selector")
		);
		String pagination_selector = this.getConfigProperty("nav_pagination_element_selector");
		StringBuilder next_button_selector = new StringBuilder(
			this.getConfigProperty("next_button_selector")
		);
		StringBuilder product_parent_container_selector = new StringBuilder(
			this.getConfigProperty("product_parent_container_selector")
		);
		String no_items_text_selector = this.getConfigProperty("no_items_available_text_selector");
		String no_items_text = "";
		String no_items_text_to_search = this.getConfigProperty("no_items_available_text");
		By no_items_text_locator = new By.ByCssSelector(no_items_text_selector);
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
		WebElement no_items_text_element;
		while (next_button_interactable) {
			if (at_bottom) {
				WebElement next_button = WebElementOperations.fluentWait(
					next_button_locator, this.driver, 5, 250L
				);
				// Below javascript code from: https://stackoverflow.com/questions/42982950/how-to-scroll-down-the-page-till-bottomend-page-in-the-selenium-webdriver
				js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
				WebElementOperations.pauseThenClick(next_button, 500, this.driver);
				product_parent_container_selector = SelectorOperations.changeSelectorDigit(
					product_parent_container_selector, 1
				);
				product_info_link_selector = SelectorOperations.changeSelectorDigit(
					product_info_link_selector, 1
				);
				product_parent_container_locator = new By.ByCssSelector(
					product_parent_container_selector.toString()
				);
			}
			try {
				no_items_text_element = WebElementOperations.fluentWait(no_items_text_locator, this.driver, 5, 250L);
			} catch (Throwable err) {
				next_button_interactable = false;
				continue;
			}
			no_items_text = no_items_text_element.getText();
			if (no_items_text.equalsIgnoreCase(no_items_text_to_search)) {
				break;
			}
			parent_container_exists = WebElementOperations.elementExists(
				product_parent_container_locator, this.driver, 10, 250L
			);
			while (parent_container_exists) {
				product_parent_container = WebElementOperations.fluentWait(
					product_parent_container_locator, this.driver, 5, 250L
				);
				js.executeScript("arguments[0].scrollIntoView(true);", product_parent_container);
				//price_data = this.scrapeProductInfoWithRetries(city, product_info_link_selector, 3);
				price_data = this.scrapeCompactProductInfo(city, product_parent_container);
				if (!(price_data.isEmpty())) {
					this.xml_parser.hashmapToXML(price_data);
				}
				product_parent_container_selector = SelectorOperations.incrementSelectorDigit(
					product_parent_container_selector
				);
				product_info_link_selector = SelectorOperations.incrementSelectorDigit(product_info_link_selector);
				product_parent_container_locator = new By.ByCssSelector(
					product_parent_container_selector.toString()
				);
				parent_container_exists = WebElementOperations.elementExistsByJavaScript(
					this.driver, product_parent_container_selector.toString()
				);
			}
			pagination_exists = WebElementOperations.elementExistsByJavaScript(this.driver, pagination_selector);
			if (!pagination_exists) {
				next_button_interactable = false;
				continue;
			}
			WebElement next_items_button = WebElementOperations.fluentWait(
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
		String location_button_selector = this.getConfigProperty("location_button");
		String change_location_button_text = this.getConfigProperty("change_location_button_text");
		WebElementOperations.tryClickingElement(new By.ByCssSelector(location_button_selector), this.driver, 30, 1000L);
		WebElementOperations.tryClickingElement(
			new By.ByPartialLinkText(change_location_button_text), this.driver, 30, 1000L
		);
		WebElement location_input = WebElementOperations.fluentWait(
			new By.ByCssSelector(this.getConfigProperty("location_input_field")),
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
		this.store_changed_for_next_round = false;
		String browse_location_button_selector = this.getConfigProperty("browse_location_button_selector");
		String store_location_text_selector = this.getConfigProperty("store_location_text_selector");
		WebElement browse_location_button = WebElementOperations.fluentWait(
			new By.ByCssSelector(browse_location_button_selector), this.driver, 10, 1000L
		);
		WebElement store_info_location_text = WebElementOperations.fluentWait(
			new By.ByCssSelector(store_location_text_selector), this.driver, 10, 1000L
		);
		String store_location_value = store_info_location_text.getText();
		String[] city_and_province = store_location_value.split(
			"[A-Za-z]\\d[A-Za-z][ -]?\\d[A-Za-z]\\d$"
		);
		city_and_province[0] = city_and_province[0].trim();
		String city_province_combination = city_and_province[0];
		String city_value_of_first_result = city_province_combination.split(",")[0].trim();
		String township_city_value = township.split(",")[0].trim();
		if (!(township_city_value.equalsIgnoreCase(city_value_of_first_result))) {
			return;
		}
		WebElementOperations.tryClickingElement(
			new By.ByCssSelector(browse_location_button_selector), this.driver, 30, 1000L
		);
		try {
			boolean location_confirmed_text_exists = WebElementOperations.elementExists(
				new By.ByCssSelector(this.getConfigProperty("location_confirmed_heading_selector")),
				this.driver, 30, 500L
			);
			if (location_confirmed_text_exists) {
				WebElementOperations.tryClickingElement(
					new By.ByCssSelector(this.getConfigProperty("close_location_confirmation_popup_selector")),
					this.driver, 30, 500L
				);
			}
		} catch (Exception err) {
			boolean location_confirmed = WebElementOperations.elementExists(
				new By.ByCssSelector(
					this.getConfigProperty("location_confirmed_heading_selector")
				),
				this.driver, 30, 500L
			);
			if (!location_confirmed) {
				throw err;
			}
		}
		this.store_changed_for_next_round = true;
	}


	/**
	 * obtainCategories - a private method that gets all main menu categories and stores them in this.categories_left,
	 * an ArrayList<String> instance
	 * - is used when a time limit is specified, and therefore you have to keep track of what categories/subcategories
	 *   are left to determine where to pick up from next time
	 * @return - returns nothing (void)
	 */
	private void obtainCategories() {
		StringBuilder main_menu_item_selector;
		By main_menu_item_locator;
		boolean main_menu_item_exists;
		boolean main_menu_item_to_ignore;
		WebElement main_menu_item;
		main_menu_item_selector = new StringBuilder(
			this.getConfigProperty("main_menu_item_selector")
		);
		main_menu_item_locator = new By.ByCssSelector(main_menu_item_selector.toString());
		main_menu_item_exists = WebElementOperations.elementExists(
			main_menu_item_locator, this.driver, 5, 100L
		);
		while (main_menu_item_exists) {
			main_menu_item = WebElementOperations.fluentWait(main_menu_item_locator, this.driver, 5, 500L);
			main_menu_item_to_ignore = this.menuItemToBeIgnored(main_menu_item);
			if (!main_menu_item_to_ignore) {
				String main_menu_item_text = main_menu_item.getText();
				this.categories_left.add(main_menu_item_text);
			}
			main_menu_item_selector = SelectorOperations.incrementSelectorDigit(main_menu_item_selector);
			main_menu_item_locator = new By.ByCssSelector(main_menu_item_selector.toString());
			main_menu_item_exists = WebElementOperations.elementExistsByJavaScript(
				this.driver, main_menu_item_selector.toString()
			);
		}
	}


	/**
	 * obtainSubcategories - a private method that gets all main menu categories and stores them in
	 * this.subcategories_left, an ArrayList<String> instance
	 * - is used when a time limit is specified, and therefore you have to keep track of what categories/subcategories
	 *   are left to determine where to pick up from next time
	 * @param - submenu_item_selector - a StringBuilder representing the submenu item selector (the CSS selector
	 * for the submenu item in question)
	 * @return - returns nothing (void)
	 */
	private void obtainSubcategories(StringBuilder submenu_item_selector) {
		By submenu_item_locator;
		boolean submenu_item_exists;
		boolean submenu_item_to_ignore;
		WebElement submenu_item;
		int submenu_li_child_index;
		submenu_item_locator = new By.ByCssSelector(submenu_item_selector.toString());
		submenu_item_exists = WebElementOperations.elementExists(
			submenu_item_locator, this.driver, 5, 100L
		);
		submenu_li_child_index = this.getNumFromConfigurationsFile("starting_index_for_submenu_item");
		while (submenu_item_exists) {
			submenu_item = WebElementOperations.fluentWait(submenu_item_locator, this.driver, 5, 500L);
			submenu_item_to_ignore = this.submenuItemToBeIgnored(
				submenu_item, submenu_item_selector.toString()
			);
			if (!submenu_item_to_ignore) {
				String submenu_item_text = submenu_item.getText();
				this.subcategories_left.add(submenu_item_text);
			}
			submenu_item_selector = SelectorOperations.incrementSelectorDigit(
				submenu_item_selector, submenu_li_child_index
			);
			submenu_item_locator = new By.ByCssSelector(submenu_item_selector.toString());
			submenu_item_exists = WebElementOperations.elementExistsByJavaScript(
				this.driver, submenu_item_selector.toString()
			);
		}
	}


	/**
	 * writeCategoriesLeft - a private helper method that writes all of the Strings in this.categories_left to an
	 * XML file, with the filename, root tag name, and the tag name for each entry being dependent on the
	 * configuration file represented by this.configurations
	 * @return - returns nothing (void)
	 */
	private void writeCategoriesLeft() throws XMLStreamException {
		String categories_left_fname = this.getConfigProperty("categories_left_fname");
		String root_categories_tag = this.getConfigProperty("root_categories_tag");
		String individual_category_tag = this.getConfigProperty("individual_category_tag");
		XMLParser categories_left_xml = new XMLParser(
			categories_left_fname, root_categories_tag, individual_category_tag
		);
		for (String category: this.categories_left) {
			categories_left_xml.createXMLNode(
				individual_category_tag, category
			);
		}
		categories_left_xml.closeProductXmlOutputStream();
	}


	/**
	 * writeSubCategoriesLeft - a private helper method that writes all of the Strings in this.subcategories_left
	 * to an XML file, with the filename, root tag name, and the tag name for each entry being dependent on the
	 * configuration file represented by this.configurations
	 * @return - returns nothing (void)
	 */
	private void writeSubCategoriesLeft() throws XMLStreamException {
		String subcategories_left_fname = this.getConfigProperty("subcategories_left_fname");
		String root_subcategories_tag = this.getConfigProperty("root_subcategories_tag");
		String individual_subcategory_tag = this.getConfigProperty("individual_subcategory_tag");
		XMLParser subcategories_left_xml = new XMLParser(
			subcategories_left_fname, root_subcategories_tag, individual_subcategory_tag
		);
		for (String subcategory: this.subcategories_left) {
			subcategories_left_xml.createXMLNode(
				individual_subcategory_tag, subcategory
			);
		}
		subcategories_left_xml.closeProductXmlOutputStream();
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
		int index = 0;
		boolean item_to_be_ignored;
		boolean menu_item_to_select;
		boolean submenu_item_to_ignore;
		boolean submenu_item_exists;
		boolean main_menu_item_exists;
		boolean main_menu_item_to_ignore;
		int submenu_li_child_index;
		WebElement main_menu_item;
		String fname_for_categories_left = this.getConfigProperty("categories_left_fname");
		String root_tag_name = this.getConfigProperty("root_categories_tag");
		String category_tag_name = this.getConfigProperty("individual_category_tag");
		String fname_for_subcategories_left = this.getConfigProperty("subcategories_left_fname");
		String subcategory_root_tag_name = this.getConfigProperty("root_subcategories_tag");
		String subcategory_tag_name = this.getConfigProperty("individual_subcategory_tag");
		String main_menu_text;
		String submenu_text;
		DOMParser categories_left = new DOMParser(
			fname_for_categories_left, root_tag_name, category_tag_name
		);
		DOMParser subcategories_left = new DOMParser(
			fname_for_subcategories_left, subcategory_root_tag_name, subcategory_tag_name
		);
		boolean categories_file_exists = this.categoriesFileExists();
		boolean subcategories_file_exists = this.subcategoriesFileExists();
		JavascriptExecutor js = (JavascriptExecutor) this.driver;
		StringBuilder main_menu_item_selector = new StringBuilder(
			this.getConfigProperty("main_menu_item_selector")
		);
		StringBuilder submenu_item_selector_in_main_menu = new StringBuilder(
			this.getConfigProperty("submenu_item_selector_in_main_menu")
		);
		submenu_li_child_index = this.getNumFromConfigurationsFile("starting_index_for_submenu_item");
		index_after_brackets = this.getNumFromConfigurationsFile("index_after_first_bracket");
		By submenu_item_locator = new By.ByCssSelector(submenu_item_selector_in_main_menu.toString());
		By main_menu_item_locator = new By.ByCssSelector(main_menu_item_selector.toString());
		this.changeLocation(township);
		if (this.store_changed_for_next_round == false) {
			return;
		}
		if (!this.privacy_policy_button_removed) {
			this.removePrivacyPolicyButon();
		}
		main_menu_item_exists = WebElementOperations.elementExistsAndIsInteractable(
			main_menu_item_locator, this.driver, 5, 100L
		);
		if (this.timeLimitExists()) {
			if (!categories_file_exists) {
				this.obtainCategories();
			} else {
				while (categories_left.hasNext()) {
					this.categories_left.add(categories_left.next());
				}
			}
		}
		while (main_menu_item_exists) {
			this.startTimer();
			main_menu_item = WebElementOperations.fluentWait(main_menu_item_locator, this.driver, 30, 1000L);
			main_menu_text = main_menu_item.getText();
			main_menu_item_to_ignore = this.menuItemToBeIgnored(main_menu_item);
			main_menu_item_to_ignore = (
				main_menu_item_to_ignore || (this.categories_left.indexOf(main_menu_text) == -1)
			);
			if (main_menu_item_to_ignore) {
				submenu_item_selector_in_main_menu = SelectorOperations.incrementSelectorDigit(
					submenu_item_selector_in_main_menu
				);
				submenu_item_selector_in_main_menu = SelectorOperations.changeSelectorDigit(
					submenu_item_selector_in_main_menu, 1, submenu_li_child_index
				);
				submenu_item_locator = new By.ByCssSelector(
					submenu_item_selector_in_main_menu.toString()
				);
				main_menu_item_selector = SelectorOperations.incrementSelectorDigit(main_menu_item_selector);
				main_menu_item_locator = new By.ByCssSelector(main_menu_item_selector.toString());
				main_menu_item_exists = WebElementOperations.elementExistsAndIsInteractable(
					main_menu_item_locator, this.driver, 5, 100L
				);
				continue;
			}
			WebElementOperations.pauseThenClick(main_menu_item, 200, this.driver);
			submenu_item_exists = WebElementOperations.elementExistsAndIsInteractable(
				submenu_item_locator, this.driver, 30, 100L
			);
			if (this.timeLimitExists()) {
				if (!subcategories_file_exists) {
					this.subcategories_left.clear();
					this.obtainSubcategories(submenu_item_selector_in_main_menu);
				} else {
					this.subcategories_left.clear();
					while (subcategories_left.hasNext()) {
						this.subcategories_left.add(subcategories_left.next());
					}
				}
			}
			while (submenu_item_exists) {
				WebElement submenu_item = WebElementOperations.fluentWait(submenu_item_locator, this.driver, 30, 1000L);
				submenu_text = submenu_item.getText();
				submenu_item_to_ignore = this.submenuItemToBeIgnored(
					submenu_item, submenu_item_selector_in_main_menu.toString()
				);
				submenu_item_to_ignore = (
					submenu_item_to_ignore ||
					(this.subcategories_left.indexOf(submenu_text) == -1)
				);
				if (!submenu_item_to_ignore) {
					WebElementOperations.pauseThenClick(submenu_item, 200, this.driver);
					StringBuilder item_under_second_submenu_selector = new StringBuilder(
						this.getConfigProperty("item_under_second_submenu")
					);
					StringBuilder see_all_link_selector = new StringBuilder(
						this.getConfigProperty("see_all_link_selector")
					);
					String parent_level_button_selector = this.getConfigProperty(
						"parent_submenu_subcategory_button_selector"
					);
					By current_submenu_locator = new By.ByCssSelector(
						item_under_second_submenu_selector.toString()
					);
					By see_all_link_locator = new By.ByCssSelector(see_all_link_selector.toString());
					By parent_level_button_locator = new By.ByCssSelector(parent_level_button_selector);
					boolean element_exists = WebElementOperations.elementExists(
						current_submenu_locator, this.driver, 10, 1000L
					);
					while (element_exists) {
						WebElement current_submenu_button = WebElementOperations.fluentWait(
							current_submenu_locator, this.driver, 10, 1000L
						);
						WebElementOperations.pauseThenClickThenPause(
							current_submenu_button, 200, 200, this.driver
						);
						WebElement see_all_button = WebElementOperations.fluentWaitTillVisibleandClickable(
							see_all_link_locator, this.driver, 30, 1000L
						);
						WebElementOperations.pauseThenClickThenPause(
							see_all_button, 200, 300, this.driver
						);
						this.scrapeAllPrices(township);
						js.executeScript("window.scrollTo(0, 0);");
						WebElement parent_menu_button = WebElementOperations.fluentWait(
							parent_level_button_locator,this.driver, 30, 1000L
						);
						WebElementOperations.pauseThenClickThenPause(
							parent_menu_button, 200, 300, this.driver
						);
						item_under_second_submenu_selector = SelectorOperations.incrementSelectorDigit(
							item_under_second_submenu_selector, index_after_brackets
						);
						see_all_link_selector = SelectorOperations.incrementSelectorDigit(
							see_all_link_selector, index_after_brackets
						);
						current_submenu_locator = new By.ByCssSelector(
							item_under_second_submenu_selector.toString()
						);
						see_all_link_locator = new By.ByCssSelector(see_all_link_selector.toString());
						element_exists = WebElementOperations.elementExists(
							current_submenu_locator, this.driver, 2, 250L
						);
					}
					index = this.subcategories_left.indexOf(submenu_text);
					if (index != -1) {
						this.subcategories_left.remove(index);
						if (this.subcategories_left.isEmpty()) {
							subcategories_left.delete();
						}
					}
					if (this.timeUp()) {
						subcategories_left.delete();
						categories_left.delete();
						this.writeSubCategoriesLeft();
						this.writeCategoriesLeft();
						return;
					}
				}
				submenu_item_selector_in_main_menu = SelectorOperations.incrementSelectorDigit(
					submenu_item_selector_in_main_menu, submenu_li_child_index
				);
				submenu_item_locator = new By.ByCssSelector(submenu_item_selector_in_main_menu.toString());
				js.executeScript("window.scrollTo(0, 0);");
				WebElementOperations.pauseThenClick(main_menu_item, 200, this.driver);
				submenu_item_exists = WebElementOperations.elementExistsAndIsInteractable(submenu_item_locator, this.driver, 30, 100L);
			}
			submenu_item_selector_in_main_menu = SelectorOperations.incrementSelectorDigit(
				submenu_item_selector_in_main_menu
			);
			submenu_item_selector_in_main_menu = SelectorOperations.changeSelectorDigit(
				submenu_item_selector_in_main_menu, 1, submenu_li_child_index
			);
			submenu_item_locator = new By.ByCssSelector(submenu_item_selector_in_main_menu.toString());
			main_menu_text = main_menu_item.getText();
			index = this.categories_left.indexOf(main_menu_text);
			if (index != -1) {
				this.categories_left.remove(index);
				if (this.categories_left.isEmpty()) {
					categories_left.delete();
				}
			}
			if (this.timeUp()) {
				categories_left.delete();
				this.writeCategoriesLeft();
				return;
			}
			main_menu_item_selector = SelectorOperations.incrementSelectorDigit(main_menu_item_selector);
			main_menu_item_locator = new By.ByCssSelector(main_menu_item_selector.toString());
			main_menu_item_exists = WebElementOperations.elementExistsAndIsInteractable(main_menu_item_locator, this.driver, 5, 100L);
		}
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
		String fname_for_cities_left = this.getConfigProperty("cities_left_fname");
		String root_tag_name = this.getConfigProperty("root_cities_tag");
		String city_tag_name = this.getConfigProperty("individual_city_tag");
		DOMParser cities_xml_dom_parser = new DOMParser(fname_for_cities_left, root_tag_name, city_tag_name);
		// https://stackoverflow.com/questions/13959704/accepting-sharing-location-browser-popups-through-selenium-webdriver
		options.setPageLoadStrategy(PageLoadStrategy.EAGER);
		options.addPreference("geo.prompt.testing", true);
		options.addPreference("geo.prompt.testing.allow", true);
		//options.addPreference(
		//	"geo.wifi.uri", "https://location.services.mozilla.com/v1/geolocate?key=%MOZILLA_API_KEY%"
		//);
		this.driver = new FirefoxDriver(options);
		this.driver.get(this.getConfigProperty("url"));
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


}

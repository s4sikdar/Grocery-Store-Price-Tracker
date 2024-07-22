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
import org.openqa.selenium.firefox.FirefoxBinary;
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
		this.categories_left = new ArrayList<String>();
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
	 * closeSignInButton - a private method that closes the sign in window that sometimes shows up when you search for
	 * anything using the search bar (shows up the first time at least)
	 * @return - returns nothing (void)
	 */
	private void closeSignInButton() {
		boolean element_exists = false;
		String sign_in_box_selector = this.getConfigProperty("sign_in_box_selector");
		String close_sign_in_box_selector = this.getConfigProperty("close_sign_in_box_selector");
		element_exists = WebElementOperations.elementExistsAndIsInteractable(
			new By.ByCssSelector(sign_in_box_selector), this.driver, 10, 250L
		);
		//element_exists = WebElementOperations.elementExistsByJavaScript(this.driver, sign_in_box_selector);
		if (element_exists) {
			WebElement close_sign_in_button = WebElementOperations.fluentWaitTillVisibleandClickable(
				new By.ByCssSelector(close_sign_in_box_selector), this.driver, 20, 250L
			);
			//WebElementOperations.pauseThenClickThenPause(close_sign_in_button, 2000, 5000, this.driver);
			close_sign_in_button.click();
		}
	}


	/**
	 * getAllAisleCategories - a private helper method that gets all aisle categories to go through, and then puts them in
	 * this.categories_left
	 * @return - returns nothing (void)
	 */
	private void getAllAisleCategories() {
		boolean element_exists = false;
		int index = this.getNumFromConfigurationsFile("submenu_button_increment_index");
		String aisles_button_text = this.getConfigProperty("aisles_button_id_selector");
		StringBuilder submenu_button_selector = new StringBuilder(this.getConfigProperty("submenu_button_selector"));
		String submenu_name_attribute = this.getConfigProperty("submenu_attribute_with_name");
		String close_ailes_button_selector = this.getConfigProperty("close_aisles_button_selector");
		WebElement submenu_button, close_aisles_button;
		WebElement aisles_button = WebElementOperations.fluentWait(
			new By.ByCssSelector(aisles_button_text), this.driver, 10, 500L
		);
		aisles_button.click();
		element_exists = WebElementOperations.elementExists(
			new By.ByCssSelector(submenu_button_selector.toString()), this.driver, 30, 250L
		);
		submenu_button = WebElementOperations.fluentWait(
			new By.ByCssSelector(submenu_button_selector.toString()), this.driver, 5, 250L
		);
		String name_attribute_value;
		while (element_exists) {
			submenu_button = WebElementOperations.fluentWait(
				new By.ByCssSelector(submenu_button_selector.toString()), this.driver, 5, 250L
			);
			name_attribute_value = submenu_button.getAttribute(submenu_name_attribute);
			this.categories_left.add(name_attribute_value);
			submenu_button_selector = SelectorOperations.incrementSelectorDigit(submenu_button_selector, index);
			element_exists = WebElementOperations.elementExistsByJavaScript(
				this.driver, submenu_button_selector.toString()
			);
		}
		close_aisles_button = WebElementOperations.fluentWait(
			new By.ByCssSelector(close_ailes_button_selector), this.driver, 5, 250L
		);
		close_aisles_button.click();
	}


	/**
	 * selectCorrectFilters - a private helper method responsible for searching all products and then selecting the correct
	 * aisle filter category based on the passed in parameter
	 * @param aisle_category - a String representing the aisle category to filter by
	 * @return - returns true if the category was found as a filter category and selected, returns false otherwise
	 */
	private boolean selectCorrectFilters(String aisle_category) {
		boolean found_category = false;
		String form_selector = this.getConfigProperty("form_selector");
		String filter_selector = this.getConfigProperty("filter_button_selector");
		String filter_show_all_aisles_selector = this.getConfigProperty("filter_show_all_aisles_selector");
		String filter_close_button_selector = this.getConfigProperty("filter_close_button_selector");
		String next_items_selector = this.getConfigProperty("next_items_selector");
		String view_all_products_link_selector = this.getConfigProperty("view_all_products_link_selector");
		WebElement close_aisles_button, form, filter_button, show_all_aisles_button, filter_close_button;
		JavascriptExecutor js = (JavascriptExecutor) this.driver;
		filter_button = WebElementOperations.fluentWaitTillVisibleandClickable(
			new By.ByCssSelector(filter_selector), this.driver, 30, 250L
		);
		filter_button.click();
		show_all_aisles_button = WebElementOperations.fluentWaitTillVisibleandClickable(
			new By.ByCssSelector(filter_show_all_aisles_selector), this.driver, 10, 500L
		);
		js.executeScript("arguments[0].click();", show_all_aisles_button);
		String filter_accordion_option_selector = this.getConfigProperty("filter_accordion_option_selector");
		String filter_show_less_selector = this.getConfigProperty("filter_show_less_selector");
		WebElement fliter_show_less_button = WebElementOperations.fluentWaitTillVisibleandClickable(
			new By.ByCssSelector(filter_show_less_selector), this.driver, 30, 500L
		);
		List<WebElement> input_boxes = (List<WebElement>) js.executeScript(
			"return document.querySelectorAll(arguments[0]);",
			filter_accordion_option_selector
		);
		String id;
		WebElement input_box_in_question = input_boxes.get(0);
		for (WebElement input_box: input_boxes) {
			id = input_box.getAttribute("for");
			if (0 == id.compareToIgnoreCase(aisle_category)) {
				input_box_in_question =  input_box;
				found_category = true;
			}
		}
		if (found_category) {
			WebElementOperations.pauseThenClickThenPause(input_box_in_question, 2000, 3000, this.driver);
			String apply_filter_button_text_selector = this.getConfigProperty("apply_filter_button_selector");
			WebElement apply_filter_button = WebElementOperations.fluentWaitTillVisibleandClickable(
				new By.ByCssSelector(apply_filter_button_text_selector), this.driver, 10, 500L
			);
			WebElementOperations.pauseThenClickThenPause(apply_filter_button, 2000, 3000, this.driver);
			return true;
		} else {
			filter_close_button = WebElementOperations.fluentWaitTillVisibleandClickable(
				new By.ByCssSelector(filter_close_button_selector), this.driver, 10, 500L
			);
			filter_close_button.click();
			return false;
		}
	}


	/**
	 * addInfoToHashMap - a private helper method that adds text to the hashmap passed in, with the text being the innerText
	 * property of HTML element selected by css_selector, with the key in the hashmap being the passed in key_value parameter
	 * @param css_selector - a String representing the CSS selector to find the element in question
	 * @param key_value - the key value for the new entry in product_info
	 * @param product_info - the hash map in question
	 * @return - returns nothing (void)
	 */
	private void addInfoToHashMap(String css_selector, String key_value, HashMap<String, String> product_info) {
		boolean element_exists = WebElementOperations.elementExistsByJavaScript(
			this.driver, css_selector
		);
		if (element_exists) {
			WebElement element = this.driver.findElement(new By.ByCssSelector(css_selector));
			String text_content = WebElementOperations.getInnerText(element, this.driver).trim().replace("\n", " ");
			if (!(text_content.isEmpty())) {
				product_info.put(key_value, text_content);
			}
		}
	}


	/**
	 * addInfoToHashMap - a private helper method that adds text to the hashmap passed in, with the text being the innerText
	 * property of HTML element selected by css_selector, with the key in the hashmap being the passed in key_value parameter
	 * @param css_selector - a String representing the CSS selector to find the element in question
	 * @param key_value - the key value for the new entry in product_info
	 * @param parent_element - the parent element to start searching from (i.e. parent_element.findElement(. . .))
	 * @param product_info - the hash map in question
	 * @return - returns nothing (void)
	 */
	private void addInfoToHashMap(
		String css_selector, String key_value, WebElement parent_element, HashMap<String, String> product_info
	) {
		boolean element_exists = WebElementOperations.elementExistsByJavaScript(
			this.driver, css_selector, parent_element
		);
		if (element_exists) {
			WebElement element = parent_element.findElement(new By.ByCssSelector(css_selector));
			String text_content = WebElementOperations.getInnerText(element, this.driver).trim().replace("\n", " ");
			if (!(text_content.isEmpty())) {
				product_info.put(key_value, text_content);
			}
		}
	}


	/**
	 * scrapePrices - a private helper method to scrape all prices on the page, and put them as XML entries in the file
	 * @return - returns nothing (void)
	 */
	private void scrapePrices() throws XMLStreamException {
		boolean element_exists = false;
		boolean volume_label_exists = false;
		JavascriptExecutor js = (JavascriptExecutor) this.driver;
		StringBuilder container_selector = new StringBuilder(this.getConfigProperty("container_selector"));
		StringBuilder volume_selector = new StringBuilder(this.getConfigProperty("volume_selector"));
		String product_title_selector = this.getConfigProperty("product_title_selector");
		String product_price_selector = this.getConfigProperty("product_price_selector");
		String pricing_unit_value_selector = this.getConfigProperty("pricing_unit_value_selector");
		String comparison_price_unit_selector = this.getConfigProperty("comparison_price_unit_selector");
		String product_brand_selector = this.getConfigProperty("product_brand_selector");
		String missing_container_selector = this.getConfigProperty("missing_container_selector");
		String product_title, product_price, pricing_unit_value, comparison_unit_price, product_brand_name, volume;
		HashMap<String, String> product_info = new HashMap<>();
		WebElement container, product_title_label, product_price_label, pricing_unit_value_label, comparison_unit_price_label,
			   product_brand_label, volume_label;
		element_exists = WebElementOperations.elementExistsAndIsInteractable(
			new By.ByCssSelector(container_selector.toString()), this.driver, 10, 250L
		);
		List<WebElement> product_containers = (List<WebElement>) js.executeScript(
			"return document.querySelectorAll(arguments[0]);", container_selector.toString()
		);
		for (WebElement product_container: product_containers) {
			js.executeScript("arguments[0].scrollIntoView(true);", product_container);
			this.addInfoToHashMap(product_title_selector, "product_title", product_container, product_info);
			this.addInfoToHashMap(product_price_selector, "product_price", product_container, product_info);
			this.addInfoToHashMap(pricing_unit_value_selector, "price_unit_Value", product_container, product_info);
			this.addInfoToHashMap(
				comparison_price_unit_selector, "comparison_unit_price", product_container, product_info
			);
			this.addInfoToHashMap(product_brand_selector, "product_brand_name", product_container, product_info);
			this.addInfoToHashMap(volume_selector.toString(), "volume", product_container, product_info);
			this.xml_parser.hashmapToXML(product_info);
		}
	}


	/**
	 * scrapeAllPrices - the private helper method responsible for scraping all prices for a given aisle category,
	 * and putting the entries in an XML file
	 * @param category - the String representing the category to look for and parse products for
	 * @return - returns nothing (void)
	 */
	private void scrapeAllPrices(String category) throws XMLStreamException {
		boolean next_page_exists = false;
		boolean category_found = false;
		String class_attribute = "";
		String search_input_selector = this.getConfigProperty("search_input_selector");
		String larger_search_input_selector = this.getConfigProperty("larger_search_input_selector");
		String search_button_selector = this.getConfigProperty("search_button_selector");
		String form_selector = this.getConfigProperty("form_selector");
		String filter_selector = this.getConfigProperty("filter_button_selector");
		String next_items_selector = this.getConfigProperty("next_button_selector");
		String view_all_products_link_selector = this.getConfigProperty("view_all_products_link_selector");
		WebElement submenu_button, close_aisles_button, search_input, larger_search_input, search_button,
			   form, filter_button, show_all_aisles_button, filter_close_button, next_items_link;
		JavascriptExecutor js = (JavascriptExecutor) this.driver;
		// First click the smaller search bar that shows on the main page, which leads to a larger search bar
		search_input = WebElementOperations.fluentWait(new By.ByCssSelector(search_input_selector), this.driver, 5, 250L);
		search_input.click();
		larger_search_input = WebElementOperations.fluentWait(
			new By.ByCssSelector(larger_search_input_selector), this.driver, 10, 250L
		);
		// Set the value equal to a couple of spaces, so that all products are returned, and then submit the form
		js.executeScript("arguments[0].value = arguments[1];", larger_search_input, "   ");
		search_button = WebElementOperations.fluentWait(
			new By.ByCssSelector(search_button_selector), this.driver, 5, 250L
		);
		form = WebElementOperations.fluentWait(
			new By.ByCssSelector(form_selector), this.driver, 10, 250L
		);
		js.executeScript("arguments[0].requestSubmit(arguments[1]);", form, search_button);
		// Sometimes there is a sign in box that shows on the search results that has to be closed. I wait till the elements
		// on the page have loaded first (otherwise elementExistsByJavaScript runs too early and fails)
		filter_button = WebElementOperations.fluentWaitTillVisibleandClickable(
			new By.ByCssSelector(filter_selector), this.driver, 30, 250L
		);
		this.closeSignInButton();
		category_found = this.selectCorrectFilters(category);
		if (category_found) {
			do {
				this.scrapePrices();
				next_items_link = WebElementOperations.fluentWait(
					new By.ByCssSelector(next_items_selector), this.driver, 10, 500L
				);
				class_attribute = next_items_link.getAttribute("class");
				if (class_attribute.contains("disabled")) {
					next_page_exists = false;
				} else {
					next_page_exists = true;
					//WebElementOperations.pauseThenClickThenPause(next_items_link, 2000, 2000, this.driver);
					js.executeScript("arguments[0].scrollIntoView(false);", next_items_link);
					next_items_link.click();
					filter_button = WebElementOperations.fluentWaitTillVisibleandClickable(
						new By.ByCssSelector(filter_selector), this.driver, 30, 500L
					);
				}
			} while(next_page_exists);
			new Actions(this.driver).pause(Duration.ofSeconds(1)).perform();
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
		this.getAllAisleCategories();
		while (!(this.categories_left.isEmpty())) {
			this.scrapeAllPrices(this.categories_left.get(0));
			this.categories_left.remove(0);
		}
		this.xml_parser.closeProductXmlOutputStream();
		this.driver.quit();
	}
}

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


	private void scrapePrices() {
		String aisles_button_text = this.getConfigProperty("aisles_button_id_selector");
		boolean element_exists = false;
		int index = this.getNumFromConfigurationsFile("submenu_button_increment_index");
		String submenu_name_attribute = this.getConfigProperty("submenu_attribute_with_name");
		String close_ailes_button_selector = this.getConfigProperty("close_aisles_button_selector");
		ArrayList<String> menu_items = new ArrayList<String>();
		StringBuilder submenu_button_selector = new StringBuilder(this.getConfigProperty("submenu_button_selector"));
		String search_input_selector = this.getConfigProperty("search_input_selector");
		String larger_search_input_selector = this.getConfigProperty("larger_search_input_selector");
		String search_button_selector = this.getConfigProperty("search_button_selector");
		String form_selector = this.getConfigProperty("form_selector");
		String filter_selector = this.getConfigProperty("filter_button_selector");
		String filter_show_all_aisles_selector = this.getConfigProperty("filter_show_all_aisles_selector");
		String filter_close_button_selector = this.getConfigProperty("filter_close_button_selector");
		String next_items_selector = this.getConfigProperty("next_items_selector");
		String sign_in_box_selector = this.getConfigProperty("sign_in_box_selector");
		String close_sign_in_box_selector = this.getConfigProperty("close_sign_in_box_selector");
		String view_all_products_link_selector = this.getConfigProperty("view_all_products_link_selector");
		WebElement submenu_button;
		WebElement close_aisles_button;
		WebElement search_input;
		WebElement larger_search_input;
		WebElement search_button;
		WebElement form;
		WebElement filter_button;
		WebElement show_all_aisles_button;
		WebElement filter_close_button;
		WebElement next_items_link;
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
			menu_items.add(name_attribute_value);
			submenu_button_selector = SelectorOperations.incrementSelectorDigit(submenu_button_selector, index);
			element_exists = WebElementOperations.elementExistsByJavaScript(
				this.driver, submenu_button_selector.toString()
			);
		}
		submenu_button.click();
		close_aisles_button = WebElementOperations.fluentWait(
			new By.ByCssSelector(close_ailes_button_selector), this.driver, 5, 250L
		);
		close_aisles_button.click();
		search_input = WebElementOperations.fluentWait(
			new By.ByCssSelector(search_input_selector), this.driver, 5, 250L
		);
		search_input.click();
		larger_search_input = WebElementOperations.fluentWait(
			new By.ByCssSelector(larger_search_input_selector), this.driver, 10, 250L
		);
		JavascriptExecutor js = (JavascriptExecutor) this.driver;
		js.executeScript("arguments[0].value = arguments[1];", larger_search_input, "   ");
		search_button = WebElementOperations.fluentWait(
			new By.ByCssSelector(search_button_selector), this.driver, 5, 250L
		);
		form = WebElementOperations.fluentWait(
			new By.ByCssSelector(form_selector), this.driver, 10, 250L
		);
		js.executeScript("arguments[0].requestSubmit(arguments[1]);", form, search_button);
		element_exists = WebElementOperations.elementExistsAndIsInteractable(
			new By.ByCssSelector(sign_in_box_selector), this.driver, 10, 250L
		);
		if (element_exists) {
			WebElement close_sign_in_button = WebElementOperations.fluentWaitTillVisibleandClickable(
				new By.ByCssSelector(close_sign_in_box_selector), this.driver, 5, 250L
			);
			WebElementOperations.pauseThenClickThenPause(close_sign_in_button, 2000, 5000, this.driver);
		}
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
		System.out.println(menu_items.get(0));
		String js_script = new StringBuilder()
			.append("arguments[0].click();\n")
			.append("var text_content = '';\n")
			.append("var label_changed = false;\n")
			.append("function verifyAndClickText(element) {\n")
			.append("	text_content = element.textContent.toLowerCase().split(' ').join('');\n")
			.append("	if (text_content == 'apply1filter') {\n")
			.append("		element.click();\n")
			.append("		label_changed = true;\n")
			.append("	}\n")
			.append("}\n")
			.append("setTimeout(verifyAndClickText, 3000, arguments[1]);\n")
			.toString();
		WebElement input_box_in_question = input_boxes.get(0);
		for (WebElement input_box: input_boxes) {
			id = input_box.getAttribute("for");
			if (0 == id.compareToIgnoreCase(menu_items.get(0))) {
				input_box_in_question =  input_box;
			}
		}
		String apply_filter_button_text_selector = this.getConfigProperty("apply_filter_button_selector");
		WebElement apply_filter_button = WebElementOperations.fluentWaitTillVisibleandClickable(
			new By.ByCssSelector(apply_filter_button_text_selector), this.driver, 10, 500L
		);
		js.executeScript(js_script, input_box_in_question, apply_filter_button);
		WebElementOperations.pauseThenClickThenPause(apply_filter_button, 2000, 5000, this.driver);
		new Actions(this.driver).pause(Duration.ofSeconds(5)).perform();
		next_items_link = WebElementOperations.fluentWait(
			new By.ByCssSelector(next_items_selector), this.driver, 10, 500L
		);
		WebElementOperations.pauseThenClickThenPause(next_items_link, 2000, 5000, this.driver);
		filter_button = WebElementOperations.fluentWaitTillVisibleandClickable(
			new By.ByCssSelector(filter_selector), this.driver, 30, 250L
		);
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
		this.scrapePrices();
		this.driver.quit();
	}
}

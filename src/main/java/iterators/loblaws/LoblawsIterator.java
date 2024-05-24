package iterators.loblaws;
import java.lang.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.time.Duration;
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
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.events.*;
import iterators.GroceryStorePriceScraper;



public class LoblawsIterator implements GroceryStorePriceScraper {
	private int counter;
	private int limit;
	private boolean event_reader_opened;
	private String fpath;
	private HashMap<String, String> numbers;
	private Properties configurations;
	private WebDriver driver;
	private HashMap<String, Boolean> cities;
	private FileOutputStream xml_ostream;
	private FileInputStream xml_istream;
	private XMLEventWriter xml_event_writer;
	private XMLEventReader xml_event_reader;
	private XMLEventFactory xml_event_factory;
	private XMLEvent xml_endline;


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
		this.event_reader_opened = false;
	}


	/**
	 * openProductXmlOutputStream: a private helper method that gets the xml filename with product data from
	 * the configuration properties file and then creates an output stream to this file using the STAX API
	 * via the XMLEventWriter API
	 * @return - returns nothing (void)
	 * */
	private void openProductXmlOutputStream() {
		boolean file_already_exists;
		String xml_filename = this.configurations.getProperty("data_xml_filename");
		String root_tag = this.configurations.getProperty("root_xml_tag");
		String currentPath = System.getProperty("user.dir");
		Path pwd = Paths.get(currentPath);
		Path xml_path = pwd.resolve(xml_filename);
		XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
		try {
			File xml_file = new File(xml_path.toString());
			file_already_exists = !(xml_file.createNewFile());
			this.xml_ostream = new FileOutputStream(xml_file, true);
			//this.xml_istream = new FileInputStream(xml_file);
			this.xml_event_writer = xmlOutputFactory.createXMLEventWriter(this.xml_ostream, "UTF-8");
			this.xml_event_factory = XMLEventFactory.newInstance();
			this.xml_endline = this.xml_event_factory.createDTD("\n");
			StartElement root_element = this.xml_event_factory.createStartElement("", "", root_tag);
			StartDocument start_document = this.xml_event_factory.createStartDocument();
			if (!file_already_exists) {
				this.xml_event_writer.add(start_document);
				this.xml_event_writer.add(this.xml_endline);
			}
			this.xml_event_writer.add(root_element);
			this.xml_event_writer.add(this.xml_endline);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}


	/**
	 * closeProductXmlOutputStream: the private helper method that adds the closing root element, and
	 * closes the xml document (code taken from the following link:
	 * https://www.geeksforgeeks.org/xml-eventwriter-in-java-stax/)
	 * @return - returns nothing (void)
	 * */
	private void closeProductXmlOutputStream() throws XMLStreamException {
		String root_tag = this.configurations.getProperty("root_xml_tag");
		this.xml_event_writer.add(this.xml_event_factory.createEndElement("", "", root_tag));
		this.xml_event_writer.add(this.xml_endline);
		this.xml_event_writer.add(this.xml_event_factory.createEndDocument());
		this.xml_event_writer.close();
	}


	private void openProductXmlInputStream() {
		boolean file_already_exists;
		String xml_filename = this.configurations.getProperty("data_xml_filename");
		String root_tag = this.configurations.getProperty("root_xml_tag");
		String currentPath = System.getProperty("user.dir");
		Path pwd = Paths.get(currentPath);
		Path xml_path = pwd.resolve(xml_filename);
		XMLInputFactory xml_input_factory = XMLInputFactory.newInstance();
		try {
			File xml_file = new File(xml_path.toString());
			file_already_exists = !(xml_file.createNewFile());
			this.xml_istream = new FileInputStream(xml_file);
			this.xml_event_reader = xml_input_factory.createXMLEventReader(this.xml_istream);
			this.xml_event_factory = XMLEventFactory.newInstance();
		} catch (Throwable t) {
			t.printStackTrace();
		}
		this.event_reader_opened = true;
	}


	/**
	 * add_tabs: a private helper method to add tabs to the xml file where information is being stored
	 * (code inspired from the following link: https://www.geeksforgeeks.org/xml-eventwriter-in-java-stax/)
	 * @param xml_event_writer - an XMLEventWriter instance
	 * @param tabs - the number of tabs to add, typically for indentation purposes 
	 * (must be greater than or equal to 0)
	 * @return - returns nothing (void)
	 * */
	private void add_tabs(XMLEventWriter xml_event_writer, int tabs) throws XMLStreamException {
		assert(tabs >= 0);
		XMLEvent tab_element = this.xml_event_factory.createDTD("\t");
		for (int i = 0; i < tabs; ++i) {
			xml_event_writer.add(tab_element);
		}
	}


	/**
	 * createXMLNode: a private helper method to create an xml node (code taken from the following link:
	 * https://www.geeksforgeeks.org/xml-eventwriter-in-java-stax/)
	 * @param xml_event_writer - an XMLEventWriter instance
	 * @param node_name - a String representing the node name
	 * @param node_value - a String representing the node value
	 * @return - returns nothing (void)
	 */
	private void createXMLNode(XMLEventWriter xml_event_writer, String node_name, String node_value) 
		throws XMLStreamException {
		assert ((node_name.trim().length()) > 0);
		XMLEvent tab_element = this.xml_event_factory.createDTD("\t");
		StartElement start_tag = this.xml_event_factory.createStartElement("", "", node_name);
		this.add_tabs(xml_event_writer, 1);
		xml_event_writer.add(start_tag);
		Characters content = this.xml_event_factory.createCharacters(node_value);
		xml_event_writer.add(content);
		EndElement end_tag = this.xml_event_factory.createEndElement("", "", node_name);
		xml_event_writer.add(end_tag);
		xml_event_writer.add(this.xml_endline);
	}


	/**
	 * createXMLNode: a private helper method to create an xml node (code taken from the following link:
	 * https://www.geeksforgeeks.org/xml-eventwriter-in-java-stax/)
	 * @param xml_event_writer - an XMLEventWriter instance
	 * @param node_name - a String representing the node name (must be non-empty excluding whitespaces)
	 * @param node_value - a String representing the node value
	 * @param tabs - an integer represnting the number of tabs to indent the tag (must be larger than 0)
	 * @return - returns nothing (void)
	 */
	private void createXMLNode(XMLEventWriter xml_event_writer, String node_name, String node_value, int tabs)
		throws XMLStreamException {
		assert ((node_name.trim().length()) > 0);
		assert (tabs >= 0);
		StartElement start_tag = this.xml_event_factory.createStartElement("", "", node_name);
		this.add_tabs(xml_event_writer, tabs);
		xml_event_writer.add(start_tag);
		Characters content = this.xml_event_factory.createCharacters(node_value);
		xml_event_writer.add(content);
		EndElement end_tag = this.xml_event_factory.createEndElement("", "", node_name);
		xml_event_writer.add(end_tag);
		xml_event_writer.add(this.xml_endline);
	}


	/**
	 * hashmapToXML: a private helper method to translate a HashMap to a set of XML tags representing
	 * the mapping (code taken from the following link:
	 * https://www.geeksforgeeks.org/xml-eventwriter-in-java-stax/)
	 * - the generic tag for the mapping is gathered by the mapping_tag property value in the properties file
	 * - afterwards, each key in the HashMap and its value becomes a tag enclosed within the larger tag
	 *   representing the mapping
	 * - Example: if mapping_tag=info_set, and mapping={"product_name": "marshmallows", "price": "$1.99"}
	 *   then the xml is created as shown below
	 *   <info_set>
	 *   	<product_name>marshmallows</product_name>
	 *   	<price>$1.99</price>
	 *   </info_set>
	 * @param mapping = an instance of the HashMap in question.
	 * @param xml_event_writer - an XMLEventWriter instance
	 * @return - returns nothing (void)
	 * */
	private void hashmapToXML(HashMap<String, String> mapping, XMLEventWriter xml_event_writer)
		throws XMLStreamException {
		String mapping_tag = this.configurations.getProperty("mapping_tag");
		Set<String> keys = mapping.keySet();
		StartElement start_tag = this.xml_event_factory.createStartElement("", "", mapping_tag);
		EndElement end_tag = this.xml_event_factory.createEndElement("", "", mapping_tag);
		this.add_tabs(xml_event_writer, 1);
		xml_event_writer.add(start_tag);
		xml_event_writer.add(this.xml_endline);
		for (String key: keys) {
			this.createXMLNode(xml_event_writer, key, mapping.get(key), 2);
		}
		this.add_tabs(xml_event_writer, 1);
		xml_event_writer.add(end_tag);
		xml_event_writer.add(this.xml_endline);
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


	private void getAllCities() throws InterruptedException {
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


	private boolean menuItemToBeIgnored(WebElement element) {
		String element_text = element.getText();
		boolean string_is_contained;
		String main_menu_items_to_ignore = this.configurations.getProperty("main_menu_items_to_ignore");
		String[] items_to_ignore = main_menu_items_to_ignore.split(",");
		for (String item_to_ignore: items_to_ignore) {
			string_is_contained = element_text.toLowerCase().contains(item_to_ignore.toLowerCase());
			if (string_is_contained) {
				return true;
			}
		}
		return false;
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

	private int getNumFromConfigurationsFile(final String config_var_name) {
		String value = this.configurations.getProperty(config_var_name);
		return Integer.parseInt(value);
	}

	private StringBuilder incrementSelectorDigit(final StringBuilder selector, int starting_index) {
		assert (selector.toString().length() >= 3);
		assert ((0 <= starting_index) && (starting_index < (selector.toString().length())));
		StringBuilder new_selector = new StringBuilder(selector.toString());
		int opening_brace_index = new_selector.indexOf("(", starting_index);
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

	private StringBuilder incrementSelectorDigit(final StringBuilder selector) {
		assert (selector.toString().length() >= 3);
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
		assert (selector.toString().length() >= 3);
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

	private StringBuilder changeSelectorDigit(final StringBuilder selector, int digit, int starting_index) {
		assert (selector.toString().length() >= 3);
		assert ((0 <= starting_index) && (starting_index < (selector.toString().length())));
		StringBuilder new_selector = new StringBuilder(selector.toString());
		int opening_brace_index = new_selector.indexOf("(", starting_index);
		int closing_brace_index = new_selector.indexOf(")", opening_brace_index);
		String number_between_brackets = new_selector.substring(
			(opening_brace_index + 1), closing_brace_index
		);
		new_selector.replace(
			(opening_brace_index + 1), closing_brace_index, Integer.toString(digit)
		);
		return new_selector;
	}

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
			String comparison_unit_price = comparison_price_value.getText() + " " + comparison_price_unit.getText();
			product_info.put("unit_price_for_comparison", comparison_unit_price);
		}
		driver.close();
		driver.switchTo().window(original_window);
		return product_info;
	}

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
				price_data = this.scrapeProductInfo(city, product_info_link_selector);
				this.hashmapToXML(price_data, this.xml_event_writer);
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

	public void gatherPricesForTownship(String township) throws InterruptedException, XMLStreamException {
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

	public void loadXML() throws InterruptedException, XMLStreamException {
		FirefoxOptions options = new FirefoxOptions();
		// https://stackoverflow.com/questions/13959704/accepting-sharing-location-browser-popups-through-selenium-webdriver
		options.addPreference("geo.prompt.testing", true);
		options.addPreference("geo.prompt.testing.allow", true);
		//options.addPreference(
		//	"geo.wifi.uri", "https://location.services.mozilla.com/v1/geolocate?key=%MOZILLA_API_KEY%"
		//);
		this.openProductXmlOutputStream();
		this.driver = new FirefoxDriver(options);
		this.driver.get(this.configurations.getProperty("url"));
		this.getAllCities();
		Set<String> unique_cities = this.cities.keySet();
		for (String city: unique_cities) {
			this.gatherPricesForTownship(city);
		}
		this.closeProductXmlOutputStream();
		this.driver.quit();
	}


	public boolean hasNext() throws XMLStreamException {
		String mapping_tag_name = this.configurations.getProperty("mapping_tag");
		String name;
		if (!this.event_reader_opened) {
			this.openProductXmlInputStream();
		}
		while (this.xml_event_reader.hasNext()) {
			XMLEvent next_event = this.xml_event_reader.nextEvent();
			if (next_event.isStartElement()) {
				StartElement start_element = next_event.asStartElement();
				name = start_element.getName().getLocalPart();
				if (name.equals(mapping_tag_name)) {
					return true;
				}
			}
		}
		return false;
	}


	public HashMap<String, String> next() throws XMLStreamException {
		boolean whitespace_data;
		String mapping_tag_name = this.configurations.getProperty("mapping_tag");
		String root_tag_name = this.configurations.getProperty("root_xml_tag");
		String name;
		Characters value;
		HashMap<String, String> product_details = new HashMap<>();
		if (!this.event_reader_opened) {
			this.openProductXmlInputStream();
		}
		do {
			XMLEvent next_event = this.xml_event_reader.nextEvent();
			if (next_event.isStartElement()) {
				StartElement start_element = next_event.asStartElement();
				name = start_element.getName().getLocalPart();
				if (name.equals(mapping_tag_name)) {
					continue;
				} else if (name.equals(root_tag_name)) {
					continue;
				} else {
					next_event = this.xml_event_reader.nextEvent();
					while (
						(!(next_event.isCharacters())) &&
						(this.xml_event_reader.hasNext())
					) {
						next_event = this.xml_event_reader.nextEvent();
					}
					if (!(this.xml_event_reader.hasNext())) {
						return product_details;
					}
					value = next_event.asCharacters();
					whitespace_data = (
						value.isIgnorableWhiteSpace() || value.isWhiteSpace()
					);
					if (!whitespace_data) {
						product_details.put(name, value.getData());
					}
				}
			} else if (next_event.isEndElement()) {
				EndElement end_element = next_event.asEndElement();
				name = end_element.getName().getLocalPart();
				if (name.equals(mapping_tag_name)) {
					return product_details;
				}
			}
		} while (this.xml_event_reader.hasNext());
		return product_details;
	}

}

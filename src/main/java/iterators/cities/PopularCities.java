package iterators.cities;
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
import iterators.xml.*;
import iterators.util.*;


public class PopularCities {
	private String cities_xml_fname;
	private String root_cities_tag;
	private String individual_city_tag;
	private int limit;
	private XMLParser cities_xml_parser;
	private WebDriver driver;
	private Properties configurations;
	private String cities_url;
	private FirefoxOptions options;


	public PopularCities(
		String cities_xml_fname, String root_cities_tag, String individual_city_tag, String config_fname, String driver_path
	) {
		this.cities_xml_fname = cities_xml_fname;
		this.root_cities_tag = root_cities_tag;
		this.individual_city_tag = individual_city_tag;
		this.limit = limit;
		this.cities_xml_parser = new XMLParser(cities_xml_fname, root_cities_tag, individual_city_tag);
                this.configurations = new Properties();
		System.setProperty("webdriver.firefox.driver", driver_path);
		FirefoxOptions options = new FirefoxOptions();
		//options.setPageLoadStrategy(PageLoadStrategy.EAGER);
		options.addPreference("geo.prompt.testing", true);
		options.addPreference("geo.prompt.testing.allow", true);
		this.options = options;
		this.driver = null;
		try {
			this.configurations.load(new FileInputStream(config_fname));
			this.cities_url = this.configurations.getProperty("url_for_most_popular_cities");
			this.limit = Integer.parseInt(this.configurations.getProperty("limit_of_cities"));
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}


	/**
	 * loadXML - a public helper method that will go to the link with information on the most populated cities, get all of the
	 * most populated cities, and then put all of them into an XML file, after which the WebDriver will close and the XML file
	 * will be created
	 * @return - returns nothing (void)
	 */
	public void loadXML() throws XMLStreamException {
		boolean element_exists;
		WebElement city_link;
		String city_link_text;
		// if there is already an output XML file existing that is associated with this.cities_xml_parser then don't do
		// anything and just return
		if (this.cities_xml_parser.xmlFileExists()) {
			return;
		}
		this.driver = new FirefoxDriver(this.options);
		StringBuilder city_name_selector = new StringBuilder(this.configurations.getProperty("selector_for_city_name"));
		int index_to_increment_from = Integer.parseInt(this.configurations.getProperty("index_to_start_from"));
		this.driver.get(this.cities_url);
		element_exists = WebElementOperations.elementExists(
			new By.ByCssSelector(city_name_selector.toString()), this.driver, 30, 500L
		);
		this.driver.manage().window().maximize();
		int city_count = 1;
		while (element_exists) {
			if (city_count == this.limit) break;
			city_link = WebElementOperations.fluentWait(
				new By.ByCssSelector(city_name_selector.toString()), this.driver, 30, 500L
			);
			city_link_text = city_link.getText().strip();
			this.cities_xml_parser.createXMLNode(this.individual_city_tag, city_link_text);
			city_name_selector = SelectorOperations.incrementSelectorDigit(city_name_selector, index_to_increment_from);
			element_exists = WebElementOperations.elementExistsByJavaScript(this.driver, city_name_selector.toString());
			city_count++;
		}
		this.cities_xml_parser.closeProductXmlOutputStream();
		this.driver.quit();
	}
}

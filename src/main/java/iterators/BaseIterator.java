package iterators;
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
import iterators.GroceryStorePriceScraper;
import iterators.xml.*;
import iterators.util.*;




public class BaseIterator implements GroceryStorePriceScraper {
	private boolean event_reader_opened;
	private String fpath;
	private Properties configurations;
	protected XMLParser xml_parser;
	private int hours;
	private int minutes;
	private LocalTime ending_time;
	private boolean timer_started;


	public BaseIterator(String config_file_path) {
		this.hours = 0;
		this.minutes = 0;
		this.setUpConfigAndXML(config_file_path);
	}


	public BaseIterator(String config_file_path, int hours) {
		this.hours = hours;
		this.minutes = 0;
		this.setUpConfigAndXML(config_file_path);
	}


	public BaseIterator(String config_file_path, int hours, int minutes) {
		this.hours = hours;
		this.minutes = minutes % 60;
		this.hours += (minutes / 60);
		this.setUpConfigAndXML(config_file_path);
	}


	/**
	 * setUpConfigAndXML - a private helper method to set up access to the configuration file (a .properties
	 * file), and load the XML parser as well (created to avoid code re-use in the constructors)
	 * @param config_file_path - a String representing the File path to the .properties file
	 * @return - returns nothing (void)
	 */
	protected void setUpConfigAndXML(String config_file_path) {
		this.fpath = config_file_path;
		this.event_reader_opened = false;
		this.timer_started = false;
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
	protected boolean timeLimitExists() {
		return !((this.minutes == 0) && (this.hours == 0));
	}


	/**
	 * startTime - a private helper method to start the timer if a time limit has been specified by
	 * this.timeLimitExists() evaluating to true
	 * @return - returns nothing (void)
	 */
	protected void startTimer() {
		if ((this.timeLimitExists()) && (!this.timer_started)) {
			this.ending_time = LocalTime.now();
			this.ending_time = this.ending_time.plus(this.hours, ChronoUnit.HOURS);
			this.ending_time = this.ending_time.plus(this.minutes, ChronoUnit.MINUTES);
			this.timer_started = true;
		}
	}


	/**
	 * timeUp - a private helper method that returns a boolean to determine if time is up or not, based on
	 * the values of this.hours and this.minutes (assuming that this.timeLimitExists() evaluates to true)
	 * @return - returns true if time is up, returns false otherwise
	 */
	protected boolean timeUp() {
		return LocalTime.now().isAfter(this.ending_time);
	}


	/**
	 * getConfigProperty - a protected helper method that returns the property property_name in the file specified
	 * by this.configurations
	 * @return - a String representing the value, null if it does not exist
	 */
	protected String getConfigProperty(String property_name) {
		return this.configurations.getProperty(property_name);
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
	protected int getNumFromConfigurationsFile(final String config_var_name) {
		String value = this.getConfigProperty(config_var_name);
		return Integer.parseInt(value);
	}


}

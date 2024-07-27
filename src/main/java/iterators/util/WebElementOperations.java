package iterators.util;
import java.lang.*;
import java.util.*;
import java.time.*;
import java.util.function.Function;
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

public class WebElementOperations {


	/**
	 * pauseThenClick: a public static method that moves the mouse over an element (passed in), pauses
	 * for a duration of milliseconds (passed in parameter), and then clicks the element
	 * - it first scrolls the element into view using the org.openqa.selenium.JavascriptExecutor class
	 *   via the executeScript method (necessary if the element is not currently in the viewport)
	 * @param element - the element you want to hover and click on (is an instance of
	 * org.openqa.selenium.WebElement)
	 * @param pause_timeout - an integer representing the number of milliseconds to pause
	 * (using Duration.ofMillis)
	 * @param driver - a WebDriver instance representing the webdriver that will be used
	 * @return - returns nothing (void)
	 * */
	public static void pauseThenClick(WebElement element, int pause_timeout, WebDriver driver) {
		JavascriptExecutor js = (JavascriptExecutor) driver;
		js.executeScript("arguments[0].scrollIntoView(false);", element);
		try{
			new Actions(driver)
				.moveToElement(element)
				.pause(Duration.ofMillis(pause_timeout))
				.click().perform();
		} catch (org.openqa.selenium.interactions.MoveTargetOutOfBoundsException e) {
			js.executeScript("arguments[0].scrollIntoView(true);", element);
			new Actions(driver)
				.moveToElement(element)
				.pause(Duration.ofMillis(pause_timeout))
				.click().perform();
		}
	}


	/**
	 * pauseThenClickThenPause: a public static method that moves the mouse over an element (passed in),
	 * pauses for a duration of milliseconds (passed in parameter), and then clicks the element
	 * - it first scrolls the element into view using the org.openqa.selenium.JavascriptExecutor class
	 *   via the executeScript method (necessary if the element is not currently in the viewport)
	 * @param element - the element you want to hover and click on (is an instance of
	 * org.openqa.selenium.WebElement)
	 * @param pause_timeout - an integer representing the number of milliseconds to pause
	 * (using Duration.ofMillis)
	 * @param post_click_timeout - an integer representing the number of milliseconds to pause
	 * (using Duration.ofMillis) after clicking the element
	 * @param driver - a WebDriver instance representing the webdriver that will be used
	 * @return - returns nothing (void)
	 * */
	public static void pauseThenClickThenPause(
		WebElement element, int pause_timeout, int post_click_timeout, WebDriver driver
	) {
		JavascriptExecutor js = (JavascriptExecutor) driver;
		js.executeScript("arguments[0].scrollIntoView(false);", element);
		new Actions(driver)
			.moveToElement(element)
			.pause(Duration.ofMillis(pause_timeout))
			.click()
			.pause(Duration.ofMillis(post_click_timeout))
			.perform();
	}


	/**
	 * fluentWait - a public static method that waits for an element to be available, and then returns
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
	public static WebElement fluentWait(
		final By locator, WebDriver driver, int timeout_duration, long polling_duration
	) {
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
	}


	/**
	 * fluentWaitTillVisibleandClickable - a public static method that waits for an element to be
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
	public static WebElement fluentWaitTillVisibleandClickable(
		final By locator, WebDriver driver, int timeout_duration, long polling_duration
	) {
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
	 * elementExistsByJavaScript - a public static method that checks if an element exists using javascript,
	 * and returns true if it does, false if not
	 * - Note that this method is faster than the elementExists method, but it requires that the html is fully
	 *   loaded the instant you use it, as it determines at that instant if the element exists or not
	 * - If you are not sure if the page has fully loaded and you use this method, you may get back a return
	 *   value of false (the element does not exist on the page, even if that element will show on the
	 *   page after it fully loads, meaning it will first take time to load, and the answer is actually true)
	 * @param driver - an instance of org.openqa.selenium.WebDriver representing a reference to the
	 * webdriver used
	 * @param css_selector - a string representing the css selector for the element you want to find
	 * @return - true if the element was found, false otherwise
	 */
	public static boolean elementExistsByJavaScript(WebDriver driver, String css_selector) {
		String script =  "if (document.querySelector(arguments[0])) return true; return false;";
		JavascriptExecutor js = (JavascriptExecutor) driver;
		Boolean element_exists = (Boolean) js.executeScript(script, css_selector);
		return element_exists.booleanValue();
	}


	/**
	 * elementExistsByJavaScript - a public static method that checks if an element exists using javascript,
	 * and returns true if it does, false if not
	 * - It searches at the DOM tree from the WebElement element (by using "element.querySelector" instead of
	 *   "document.querySelector")
	 * - Note that this method is faster than the elementExists method, but it requires that the html is fully
	 *   loaded the instant you use it, as it determines at that instant if the element exists or not
	 * - If you are not sure if the page has fully loaded and you use this method, you may get back a return
	 *   value of false (the element does not exist on the page, even if that element will show on the
	 *   page after it fully loads, meaning it will first take time to load, and the answer is actually true)
	 * @param driver - an instance of org.openqa.selenium.WebDriver representing a reference to the
	 * webdriver used
	 * @param css_selector - a string representing the css selector for the element you want to find
	 * @param element - a WebElement representing the element to start searching from (through element.querySelector)
	 * @return - true if the element was found, false otherwise
	 */
	public static boolean elementExistsByJavaScript(WebDriver driver, String css_selector, WebElement element) {
		String script =  "if (arguments[1].querySelector(arguments[0])) return true; return false;";
		JavascriptExecutor js = (JavascriptExecutor) driver;
		Boolean element_exists = (Boolean) js.executeScript(script, css_selector, element);
		return element_exists.booleanValue();
	}


	/**
	 * elementExistsAndIsInteractableByJavaScript - a public static method that checks if an element exists
	 * using javascript, as well as if the element is visible on the page, returning true if it exists and
	 * false otherwise
	 * - visibility is determined on if the rectangle representing the elemnt has positive width and height, as
	 *   well as if the javascript's checkVisibility method on the element returns true
	 * - Note that this method is faster than the elementExistsAndIsInteractable method, but it requires that
	 *   the html is fully loaded the instant you use it, as it determines at that instant if the element exists
	 *   or not
	 * - If you are not sure if the page has fully loaded and you use this method, you may get back a return
	 *   value of false (the element does not exist on the page, even if that element will show on the
	 *   page after it fully loads, meaning it will first take time to load, and the answer is actually true)
	 * @param driver - an instance of org.openqa.selenium.WebDriver representing a reference to the
	 * webdriver used
	 * @param css_selector - a string representing the css selector for the element you want to find
	 * @return - true if the element was found and is visible, false otherwise
	 */
	public static boolean elementExistsAndIsInteractableByJavaScript(WebDriver driver, String css_selector) {
		JavascriptExecutor js = (JavascriptExecutor) driver;
		String script = new StringBuilder()
			.append("var element = document.querySelector(arguments[0]);\n")
			.append("var rectangle = element.getBoundingClientRect();\n")
			.append("var width = rectangle.right - rectangle.left;\n")
			.append("var height = rectangle.bottom - rectangle.top;\n")
			.append("if ((width > 0) && (height > 0)) return true; return false;").toString();
		String check_visibility_script = "return document.querySelector(arguments[0]).checkVisibility();";
		Boolean element_visible = (Boolean) js.executeScript(check_visibility_script, css_selector);
		Boolean rectangle_dimensions_positive = (Boolean) js.executeScript(script, css_selector);
		boolean element_exists = WebElementOperations.elementExistsByJavaScript(driver, css_selector);
		return ((element_exists && rectangle_dimensions_positive.booleanValue()) && element_visible.booleanValue());
	}


	/**
	 * elementExists - a public static method that waits for an element to be available, and then
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
	public static boolean elementExists(
		final By locator, WebDriver driver, int timeout_duration, long polling_duration
	) {
		try {
			WebElement element_to_find = WebElementOperations.fluentWait(
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
	 * elementExistsAndIsInteractable - a public static method that waits for an element to be
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
	public static boolean elementExistsAndIsInteractable(
		final By locator, WebDriver driver, int timeout_duration, long polling_duration
	) {
		try {
			WebElement element_to_find = WebElementOperations.fluentWaitTillVisibleandClickable(
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
	 * tryClickingElement - a public static method to try and click an element you wish to find with
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
	public static void tryClickingElement(
		final By locator, WebDriver driver, int timeout_duration, long polling_duration
	) {
		try {
			WebElement element_in_question = WebElementOperations.fluentWait(locator, driver, timeout_duration, polling_duration);
			element_in_question.click();
		} catch (Throwable err) {
			boolean element_still_there = WebElementOperations.elementExists(
				locator, driver, timeout_duration, polling_duration
			);
			if (element_still_there) {
				WebElement target_element = WebElementOperations.fluentWait(
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
	 * getInnerText - a private helper method that gets the innerText value of a web element using JavaScript, useful
	 * for getting all text from all recrusive child nodes from WebElement element
	 * - documentation on innerText property: https://developer.mozilla.org/en-US/docs/Web/API/HTMLElement/innerText
	 * @param element - the WebElement for which you want to get the innerText property value from
	 * @param driver - the WebDriver instance to use (needed to cast into a JavascriptExecutor instance)
	 * @return - returns a String of the innerText property value of the element
	 */
	public static String getInnerText(WebElement element, WebDriver driver) {
		JavascriptExecutor js = (JavascriptExecutor) driver;
		return (String) js.executeScript("return arguments[0].innerText;", element);
	}




}

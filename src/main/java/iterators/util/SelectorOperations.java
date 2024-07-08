package iterators.util;
import java.lang.*;
import java.util.*;


public class SelectorOperations {


	/**
	 * incrementSelectorDigit - a public static method that gets a number from inside of selector (starting
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
	public static StringBuilder incrementSelectorDigit(final StringBuilder selector, int starting_index) {
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
	 * incrementSelectorDigit - a public static method that gets a number from inside of selector and
	 * then returns a copy of selector with the number incremented (the number is inside round brackets)
	 * @param selector - a constant StringBuilder representing a css selector that has a number in it
	 * in between round brackets
	 * @return - returns a new StringBuilder instance with the integer incremented if the integer and the
	 * surrounding round brackets can be found, returns a copy of the passed in StringBuilder from selector
	 * otherwise
	 */
	public static StringBuilder incrementSelectorDigit(final StringBuilder selector) {
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
	 * changeSelectorDigit - a public static method that gets a number from inside of selector and
	 * then returns a copy of selector with the number replaced by digit
	 * @param selector - a constant StringBuilder representing a css selector that has a number in it
	 * in between round brackets
	 * @return - returns a new StringBuilder instance with the integer incremented if the integer and the
	 * surrounding round brackets can be found, returns a copy of the passed in StringBuilder from selector
	 * otherwise
	 */
	public static StringBuilder changeSelectorDigit(final StringBuilder selector, int digit) {
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
	 * changeSelectorDigit - a public static method that gets a number from inside of selector and
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
	public static StringBuilder changeSelectorDigit(final StringBuilder selector, int digit, int starting_index) {
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


}

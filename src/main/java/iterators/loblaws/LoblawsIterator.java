package iterators.loblaws;
import java.util.HashMap;
import iterators.GroceryStorePriceScraper;

public class LoblawsIterator implements GroceryStorePriceScraper {
	private int counter;
	private int limit;
	private HashMap<String, String> numbers;

	public LoblawsIterator(int count, int limit) {
		this.counter = count;
		this.limit = limit;
		this.numbers = new HashMap<>();
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
}

package iterators;
import java.util.HashMap;

public interface GroceryStorePriceScraper {
	public HashMap<String, String> next();
	public boolean hasNext();
}

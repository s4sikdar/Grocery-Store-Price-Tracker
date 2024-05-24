package iterators;
import java.util.HashMap;
import javax.xml.stream.XMLStreamException;

public interface GroceryStorePriceScraper {
	public HashMap<String, String> next() throws XMLStreamException;
	public boolean hasNext() throws XMLStreamException;
}

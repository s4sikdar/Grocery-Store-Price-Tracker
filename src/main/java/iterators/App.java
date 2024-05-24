package iterators;

import java.nio.file.Path;
import java.nio.file.Paths;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartDocument;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import iterators.GroceryStorePriceScraper;
import iterators.loblaws.LoblawsIterator;

/**
 * Hello world!
 *
 */
public class App
{
    public static void main( String[] args ) throws InterruptedException, XMLStreamException
    {
        //System.out.println( "Hello World!" );
	String currentPath = System.getProperty("user.dir");
	Path pwd = Paths.get(currentPath);
	Path firefoxdriver_path = pwd.resolve("drivers");
	firefoxdriver_path = firefoxdriver_path.resolve("geckodriver.exe");
	System.setProperty("webdriver.firefox.driver", firefoxdriver_path.toString());
	pwd = pwd.resolve("config");
	pwd = pwd.resolve("loblaws.properties");
	//String config_path = pwd.toString() + "loblaws.properties";
	LoblawsIterator loblaws_iter = new LoblawsIterator(pwd.toString(), 0, 10);
	loblaws_iter.loadXML();
	while (loblaws_iter.hasNext()) {
		System.out.println(loblaws_iter.next());
	}
    }
}

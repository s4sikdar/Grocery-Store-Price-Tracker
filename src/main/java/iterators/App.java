package iterators;

import java.nio.file.Path;
import java.nio.file.Paths;
import iterators.GroceryStorePriceScraper;
import iterators.loblaws.LoblawsIterator;

/**
 * Hello world!
 *
 */
public class App
{
    public static void main( String[] args ) throws InterruptedException
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
	//while (loblaws_iter.hasNext()) {
	//	System.out.println(loblaws_iter.next());
	//}
	System.out.println(loblaws_iter.getUrl());
    }
}

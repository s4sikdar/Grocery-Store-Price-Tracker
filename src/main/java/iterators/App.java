package iterators;

import java.lang.*;
import java.util.*;
import java.io.*;
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
import iterators.DatabaseClient;
import iterators.loblaws.LoblawsIterator;
import iterators.xml.*;
import iterators.util.*;

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

	Path database_properties_path = pwd.resolve("database.properties");
	Path database_xml_properties_path = pwd.resolve("database_xml.properties");
	Path loblaws_properties_path = pwd.resolve("nofrills.properties");

	String database_properties_file_path = database_properties_path.toString();
	String database_xml_properties_file_path = database_xml_properties_path.toString();

	//Properties database_config = new Properties();
	Properties database_xml_config = new Properties();

	try {
		//database_config.load(new FileInputStream(database_properties_file_path));
		database_xml_config.load(new FileInputStream(database_xml_properties_file_path));
	} catch (Throwable err) {
		err.printStackTrace();
		System.exit(1);
	}

	String fruits_and_vegetables_fname = database_xml_config.getProperty("fruits_and_vegetables");
	String dairy_and_eggs_fname = database_xml_config.getProperty("dairy_and_eggs");
	String pantry_fname = database_xml_config.getProperty("pantry");
	String meat_fname = database_xml_config.getProperty("meat");
	String snacks_and_chips_and_candy_fname = database_xml_config.getProperty("snacks_and_chips_and_candy");
	String frozen_food_fname = database_xml_config.getProperty("frozen_food");
	String bakery_fname = database_xml_config.getProperty("bakery");
	String drinks_fname = database_xml_config.getProperty("drinks");
	String deli_fname = database_xml_config.getProperty("deli");
	String fish_and_seafood_fname = database_xml_config.getProperty("fish_and_seafood");
	String root_xml_tag_name = database_xml_config.getProperty("root_xml_tag");
	String mapping_tag_name = database_xml_config.getProperty("mapping_tag");
	HashMap<String, XMLParser> xml_parsers = new HashMap<>();

	XMLParser fruits_and_vegetables = new XMLParser(fruits_and_vegetables_fname, root_xml_tag_name, mapping_tag_name);
	XMLParser dairy_and_eggs = new XMLParser(dairy_and_eggs_fname, root_xml_tag_name, mapping_tag_name);
	XMLParser pantry = new XMLParser(pantry_fname, root_xml_tag_name, mapping_tag_name);
	XMLParser meat = new XMLParser(meat_fname, root_xml_tag_name, mapping_tag_name);
	XMLParser snacks_and_chips_and_candy = new XMLParser(snacks_and_chips_and_candy_fname, root_xml_tag_name, mapping_tag_name);
	XMLParser frozen_food = new XMLParser(frozen_food_fname, root_xml_tag_name, mapping_tag_name);
	XMLParser bakery = new XMLParser(bakery_fname, root_xml_tag_name, mapping_tag_name);
	XMLParser drinks = new XMLParser(drinks_fname, root_xml_tag_name, mapping_tag_name);
	XMLParser deli = new XMLParser(deli_fname, root_xml_tag_name, mapping_tag_name);
	XMLParser fish_and_seafood = new XMLParser(fish_and_seafood_fname, root_xml_tag_name, mapping_tag_name);
	xml_parsers.put("fruits & vegetables", fruits_and_vegetables);
	xml_parsers.put("dairy & eggs", dairy_and_eggs);
	xml_parsers.put("pantry", pantry);
	xml_parsers.put("meat", meat);
	xml_parsers.put("snacks, chips & candy", snacks_and_chips_and_candy);
	xml_parsers.put("frozen food", frozen_food);
	xml_parsers.put("bakery", bakery);
	xml_parsers.put("drinks", drinks);
	xml_parsers.put("deli", deli);
	xml_parsers.put("fisn & seafood", fish_and_seafood);
	Set<String> xml_parsers_keys = xml_parsers.keySet();
	//String config_path = pwd.toString() + "loblaws.properties";
	LoblawsIterator loblaws_iter = new LoblawsIterator(loblaws_properties_path.toString(), 0, 30);
	loblaws_iter.loadXML();
	while (loblaws_iter.hasNext()) {
		HashMap<String, String> product_info = loblaws_iter.next();
		String category = product_info.get("category_path");
		category = category.toLowerCase().split(">")[1];
		for (String key: xml_parsers_keys) {
			if (category.equalsIgnoreCase(key)) {
				try {
					xml_parsers.get(key).hashmapToXML(product_info);
				} catch (XMLStreamException err) {
					err.printStackTrace();
				}
			}
		}
	}
	for (String key: xml_parsers_keys) {
		xml_parsers.get(key).closeProductXmlOutputStream();
	}
    }
}

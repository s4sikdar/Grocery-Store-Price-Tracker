package iterators.xml;
import java.lang.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.time.Duration;
import javax.xml.stream.*;
import javax.xml.stream.events.*;


public class XMLParser {
	private boolean event_reader_opened;
	private FileOutputStream xml_ostream;
	private FileInputStream xml_istream;
	private XMLEventWriter xml_event_writer;
	private XMLEventReader xml_event_reader;
	private XMLEventFactory xml_event_factory;
	private XMLEvent xml_endline;
	private boolean xml_ostream_accessed;
	private String xml_filename;
	private String root_tag;
	private String mapping_tag;

	public XMLParser(String xml_filename, String root_tag, String mapping_tag) {
                this.xml_ostream_accessed = false;
		this.xml_filename = xml_filename;
		this.root_tag = root_tag;
		this.mapping_tag = mapping_tag;
	}


	/**
	 * openProductXmlOutputStream: a public helper method that gets the xml filename with product data from
	 * the configuration properties file and then creates an output stream to this file using the STAX API
	 * via the XMLEventWriter API
	 * @return - returns nothing (void)
	 * */
	private void openProductXmlOutputStream() {
		if (!this.xml_ostream_accessed) {
			boolean file_already_exists;
			String currentPath = System.getProperty("user.dir");
			Path pwd = Paths.get(currentPath);
			Path xml_path = pwd.resolve(this.xml_filename);
			XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
			try {
				File xml_file = new File(xml_path.toString());
				file_already_exists = !(xml_file.createNewFile());
				this.xml_ostream = new FileOutputStream(xml_file, true);
				this.xml_event_writer = xmlOutputFactory.createXMLEventWriter(
					this.xml_ostream, "UTF-8"
				);
				this.xml_event_factory = XMLEventFactory.newInstance();
				this.xml_endline = this.xml_event_factory.createDTD("\n");
				StartElement root_element = this.xml_event_factory.createStartElement(
					"", "", this.root_tag
				);
				StartDocument start_document = this.xml_event_factory.createStartDocument();
				if (!file_already_exists) {
					this.xml_event_writer.add(start_document);
					this.xml_event_writer.add(this.xml_endline);
				}
				this.xml_event_writer.add(root_element);
				this.xml_event_writer.add(this.xml_endline);
				this.xml_ostream_accessed = true;
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}


	/**
	 * closeProductXmlOutputStream: the public helper method that adds the closing root element, and
	 * closes the xml document (code taken from the following link:
	 * https://www.geeksforgeeks.org/xml-eventwriter-in-java-stax/)
	 * @return - returns nothing (void)
	 * */
	public void closeProductXmlOutputStream() throws XMLStreamException {
		if (this.xml_ostream_accessed) {
			this.xml_event_writer.add(this.xml_event_factory.createEndElement("", "", this.root_tag));
			this.xml_event_writer.add(this.xml_endline);
			this.xml_event_writer.add(this.xml_event_factory.createEndDocument());
			this.xml_event_writer.close();
		}
	}


	/**
	 * openProductXmlInputStream - a private helper method that uses the STAX API (via XMLEventReader) and
	 * opens an input stream from the xml file specified by the name passed to the constructor
	 * (this.xml_filename)
	 * - this method must be run before reading in xml tags from the xml file
	 * @return - returns nothing (void)
	 */
	private void openProductXmlInputStream() {
		boolean file_already_exists;
		String currentPath = System.getProperty("user.dir");
		Path pwd = Paths.get(currentPath);
		Path xml_path = pwd.resolve(this.xml_filename);
		XMLInputFactory xml_input_factory = XMLInputFactory.newInstance();
		try {
			File xml_file = new File(xml_path.toString());
			file_already_exists = !(xml_file.createNewFile());
			this.xml_istream = new FileInputStream(xml_file);
			this.xml_event_reader = xml_input_factory.createXMLEventReader(this.xml_istream);
			this.xml_event_factory = XMLEventFactory.newInstance();
		} catch (Throwable t) {
			t.printStackTrace();
		}
		this.event_reader_opened = true;
	}


	/**
	 * add_tabs: a private helper method to add tabs to the xml file where information is being stored
	 * (code inspired from the following link: https://www.geeksforgeeks.org/xml-eventwriter-in-java-stax/)
	 * @param xml_event_writer - an XMLEventWriter instance
	 * @param tabs - the number of tabs to add, typically for indentation purposes 
	 * (must be greater than or equal to 0)
	 * @return - returns nothing (void)
	 * */
	private void add_tabs(XMLEventWriter xml_event_writer, int tabs) throws XMLStreamException {
		assert(tabs >= 0);
		XMLEvent tab_element = this.xml_event_factory.createDTD("\t");
		for (int i = 0; i < tabs; ++i) {
			xml_event_writer.add(tab_element);
		}
	}


	/**
	 * createXMLNode: a public helper method to create an xml node (code taken from the following link:
	 * https://www.geeksforgeeks.org/xml-eventwriter-in-java-stax/)
	 * @param xml_event_writer - an XMLEventWriter instance
	 * @param node_name - a String representing the node name
	 * @param node_value - a String representing the node value
	 * @return - returns nothing (void)
	 */
	public void createXMLNode(String node_name, String node_value) 
		throws XMLStreamException {
		assert ((node_name.trim().length()) > 0);
		this.openProductXmlOutputStream();
		XMLEvent tab_element = this.xml_event_factory.createDTD("\t");
		StartElement start_tag = this.xml_event_factory.createStartElement("", "", node_name);
		this.add_tabs(this.xml_event_writer, 1);
		this.xml_event_writer.add(start_tag);
		Characters content = this.xml_event_factory.createCharacters(node_value);
		this.xml_event_writer.add(content);
		EndElement end_tag = this.xml_event_factory.createEndElement("", "", node_name);
		this.xml_event_writer.add(end_tag);
		this.xml_event_writer.add(this.xml_endline);
	}


	/**
	 * createXMLNode: a public helper method to create an xml node (code taken from the following link:
	 * https://www.geeksforgeeks.org/xml-eventwriter-in-java-stax/)
	 * @param xml_event_writer - an XMLEventWriter instance
	 * @param node_name - a String representing the node name (must be non-empty excluding whitespaces)
	 * @param node_value - a String representing the node value
	 * @param tabs - an integer represnting the number of tabs to indent the tag (must be larger than 0)
	 * @return - returns nothing (void)
	 */
	public void createXMLNode(String node_name, String node_value, int tabs)
		throws XMLStreamException {
		assert ((node_name.trim().length()) > 0);
		assert (tabs >= 0);
		this.openProductXmlOutputStream();
		StartElement start_tag = this.xml_event_factory.createStartElement("", "", node_name);
		this.add_tabs(xml_event_writer, tabs);
		this.xml_event_writer.add(start_tag);
		Characters content = this.xml_event_factory.createCharacters(node_value);
		this.xml_event_writer.add(content);
		EndElement end_tag = this.xml_event_factory.createEndElement("", "", node_name);
		this.xml_event_writer.add(end_tag);
		this.xml_event_writer.add(this.xml_endline);
	}


	/**
	 * hashmapToXML: a public helper method to translate a HashMap to a set of XML tags representing
	 * the mapping (code taken from the following link:
	 * https://www.geeksforgeeks.org/xml-eventwriter-in-java-stax/)
	 * - the generic tag for the mapping is gathered by the mapping_tag property value in the properties file
	 * - afterwards, each key in the HashMap and its value becomes a tag enclosed within the larger tag
	 *   representing the mapping
	 * - Example: if this.mapping_tag=info_set, and mapping={"product_name": "marshmallows", "price": "$1.99"}
	 *   then the xml is created as shown below
	 *   <info_set>
	 *   	<product_name>marshmallows</product_name>
	 *   	<price>$1.99</price>
	 *   </info_set>
	 * @param mapping = an instance of the HashMap in question.
	 * @param xml_event_writer - an XMLEventWriter instance
	 * @return - returns nothing (void)
	 * */
	public void hashmapToXML(HashMap<String, String> mapping) throws XMLStreamException {
		this.openProductXmlOutputStream();
		Set<String> keys = mapping.keySet();
		StartElement start_tag = this.xml_event_factory.createStartElement("", "", this.mapping_tag);
		EndElement end_tag = this.xml_event_factory.createEndElement("", "", this.mapping_tag);
		this.add_tabs(this.xml_event_writer, 1);
		this.xml_event_writer.add(start_tag);
		this.xml_event_writer.add(this.xml_endline);
		for (String key: keys) {
			this.createXMLNode(key, mapping.get(key), 2);
		}
		this.add_tabs(this.xml_event_writer, 1);
		this.xml_event_writer.add(end_tag);
		this.xml_event_writer.add(this.xml_endline);
	}


	/**
	 * hasNext - a public method to check whether or not there are any more tags to be parsed and returned
	 * as a HashMap using the next() method
	 * - the xml is parsed until an opening tag is reached with the tag name being the same as this.mapping_tag
	 * @return - returns true if there are any more tags with the name of this.mapping_tag to be parsed,
	 * returns false otherwise
	 */
	public boolean hasNext() throws XMLStreamException {
		String name;
		if (!this.event_reader_opened) {
			this.openProductXmlInputStream();
		}
		while (this.xml_event_reader.hasNext()) {
			XMLEvent next_event = this.xml_event_reader.nextEvent();
			if (next_event.isStartElement()) {
				StartElement start_element = next_event.asStartElement();
				name = start_element.getName().getLocalPart();
				if (name.equals(this.mapping_tag)) {
					return true;
				}
			}
		}
		return false;
	}


	/**
	 * next - a public method that parses the contents of the next xml tag with the name being this.mapping_tag
	 * and returns the contents in a HashMap
	 * - Each set of xml tags inside this.mapping_tag has the tag name as the key in the HashMap,
	 *   and the value inside the tags as the value
	 * - Example: if this.mapping_tag is "GroceryItem", and the xml tag is as below
	 *   <GroceryItem>
	 *   	<product>Apple</product>
	 *   	<price>$0.99</price>
	 *   </GroceryItem>
	 *   then next() returns the mapping as {"product": "Apple", "price": "$0.99"}
	 * @return - returns a HashMap with the information stored inside the XML Tag
	 */
	public HashMap<String, String> next() throws XMLStreamException {
		boolean whitespace_data;
		String name;
		Characters value;
		HashMap<String, String> product_details = new HashMap<>();
		if (!this.event_reader_opened) {
			this.openProductXmlInputStream();
		}
		do {
			XMLEvent next_event = this.xml_event_reader.nextEvent();
			if (next_event.isStartElement()) {
				StartElement start_element = next_event.asStartElement();
				name = start_element.getName().getLocalPart();
				if (name.equals(this.mapping_tag)) {
					continue;
				} else if (name.equals(this.root_tag)) {
					continue;
				} else {
					next_event = this.xml_event_reader.nextEvent();
					while (
						(!(next_event.isCharacters())) &&
						(this.xml_event_reader.hasNext())
					) {
						next_event = this.xml_event_reader.nextEvent();
					}
					if (!(this.xml_event_reader.hasNext())) {
						return product_details;
					}
					value = next_event.asCharacters();
					whitespace_data = (
						value.isIgnorableWhiteSpace() || value.isWhiteSpace()
					);
					if (!whitespace_data) {
						product_details.put(name, value.getData());
					}
				}
			} else if (next_event.isEndElement()) {
				EndElement end_element = next_event.asEndElement();
				name = end_element.getName().getLocalPart();
				if (name.equals(this.mapping_tag)) {
					return product_details;
				}
			}
		} while (this.xml_event_reader.hasNext());
		return product_details;
	}


}

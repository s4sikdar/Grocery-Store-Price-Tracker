package iterators.xml;
import java.lang.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Stream;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryIteratorException;
import java.time.*;
import java.time.format.DateTimeFormatter;
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
	private boolean add_name_suffix;
	private String xml_filename;
	private String root_tag;
	private String mapping_tag;
	private String current_output_xml_filename;
	private String current_input_xml_filename;
	private ArrayList<String> matched_xml_filenames;
	private String date_pattern;
	private String glob_pattern;


	public XMLParser(String xml_filename, String root_tag, String mapping_tag) {
		this.setProperties(xml_filename, root_tag, mapping_tag, false);
	}


	public XMLParser(String xml_filename, String mapping_tag) {
		this.setProperties(xml_filename, "", mapping_tag, false);
	}


	public XMLParser(String xml_filename, String root_tag, String mapping_tag, boolean name_suffix) {
		this.setProperties(xml_filename, root_tag, mapping_tag, name_suffix);
	}


	public XMLParser(String xml_filename, String mapping_tag, boolean name_suffix) {
		this.setProperties(xml_filename, "", mapping_tag, name_suffix);
	}


	/**
	 * setProperties - a private method that is responsible for setting instance properties in the
	 * constructor
	 * @param xml_filename - a string representing the xml filename
	 * @param root_tag - a string representing the xml root tag
	 * @param mapping_tag - a string representing the xml mapping tag (the tag that contains product
	 * data information on a single product in a single store)
	 * @return - returns nothing (void)
	 */
	private void setProperties(String xml_filename, String root_tag, String mapping_tag, boolean name_suffix) {
                this.xml_ostream_accessed = false;
                this.add_name_suffix = name_suffix;
		this.xml_filename = xml_filename;
		this.root_tag = root_tag;
		this.mapping_tag = mapping_tag;
		// the string pattern derived from the date and time when the file is created,
		// which will be added to the xml filename to give each xml file a unique name.
		this.date_pattern = "-MMM-dd-yyyy-HH-mm";
		String[] prefix_and_suffix = xml_filename.split("\\.");
		// the globbing pattern for which all xml files with product entries will be matched by
		this.glob_pattern = prefix_and_suffix[0] +
		"-[A-Za-z][A-Za-z][A-Za-z]-[0-9][0-9]-[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]" +
		"." + prefix_and_suffix[1];
		this.matched_xml_filenames = new ArrayList<String>();
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
			String[] prefix_and_extension = this.xml_filename.split("\\.");
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern(this.date_pattern);
			LocalDateTime current_time = LocalDateTime.now();
			String formatted_date = current_time.format(formatter);
			String xml_fname = prefix_and_extension[0] + formatted_date + "." + prefix_and_extension[1];
			if (this.add_name_suffix) {
				this.current_output_xml_filename = xml_fname;
			} else {
				this.current_output_xml_filename = this.xml_filename;
			}
			Path xml_path = pwd.resolve(this.current_output_xml_filename);
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
				StartDocument start_document = this.xml_event_factory.createStartDocument();
				if (!file_already_exists) {
					this.xml_event_writer.add(start_document);
					this.xml_event_writer.add(this.xml_endline);
				}
				if (this.root_tag != "") {
					StartElement root_element = this.xml_event_factory.createStartElement(
						"", "", this.root_tag
					);
					this.xml_event_writer.add(root_element);
					this.xml_event_writer.add(this.xml_endline);
				}
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
			if (this.root_tag != "") {
				this.xml_event_writer.add(
					this.xml_event_factory.createEndElement("", "", this.root_tag)
				);
				this.xml_event_writer.add(this.xml_endline);
			}
			this.xml_event_writer.add(this.xml_event_factory.createEndDocument());
			this.xml_event_writer.close();
		}
	}


	/**
	 * listSourceFiles - a private helper method that lists all files in a directory that match the
	 * globbing pattern of this.glob_pattern, code was taken and slightly modified from
	 * DirectoryStream documentation:
	 * https://docs.oracle.com/javase%2F8%2Fdocs%2Fapi%2F%2F/java/nio/file/DirectoryStream.html
	 * @return - a ArrayList<String> instance of all files that match this.glob_pattern
	 * @throws IOException
	 */
	private ArrayList<String> listSourceFiles(Path directory) throws IOException {
		ArrayList<String> result = new ArrayList<>();
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, this.glob_pattern)) {
		   for (Path entry: stream) {
		       result.add(entry.toString());
		   }
		} catch (DirectoryIteratorException ex) {
		   // I/O error encounted during the iteration, the cause is an IOException
		   throw ex.getCause();
		}
		return result;
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
		try {
			ArrayList<String> files_in_pwd = this.listSourceFiles(pwd);
			this.matched_xml_filenames = files_in_pwd;
			this.current_input_xml_filename = this.matched_xml_filenames.get(0);
			this.matched_xml_filenames.remove(0);
			Path xml_path = pwd.resolve(this.current_input_xml_filename);
			XMLInputFactory xml_input_factory = XMLInputFactory.newInstance();
			File xml_file = new File(xml_path.toString());
			file_already_exists = xml_file.exists();
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
		String currentPath = System.getProperty("user.dir");
		Path pwd = Paths.get(currentPath);
		while (!(this.matched_xml_filenames.isEmpty())) {
			try {
				this.current_input_xml_filename = this.matched_xml_filenames.get(0);
				this.matched_xml_filenames.remove(0);
				Path xml_path = pwd.resolve(this.current_input_xml_filename);
				XMLInputFactory xml_input_factory = XMLInputFactory.newInstance();
				File xml_file = new File(xml_path.toString());
				//file_already_exists = !(xml_file.createNewFile());
				this.xml_istream = new FileInputStream(xml_file);
				this.xml_event_reader = xml_input_factory.createXMLEventReader(this.xml_istream);
				this.xml_event_factory = XMLEventFactory.newInstance();
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
			} catch (FileNotFoundException err) {
				err.printStackTrace();
				return false;
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

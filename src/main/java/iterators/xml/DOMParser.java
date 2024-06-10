package iterators.xml;
import java.lang.*;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Entity;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class DOMParser {
	private boolean file_opened;
	private boolean file_exists;
	private int counter;
	private String xml_filename;
	private String root_tag;
	private String mapping_tag;
	private File xml_file;
	private DocumentBuilderFactory document_factory;
	private DocumentBuilder builder;
	private Document doc;
	private Node current_node;
	private Node root_node;


	public DOMParser(String xml_filename, String root_tag, String mapping_tag) {
		this.xml_filename = xml_filename;
		this.root_tag = root_tag;
		this.mapping_tag = mapping_tag;
		this.file_opened = false;
		this.counter = 0;
	}


	/**
	 * parse - a private method that opens and parses the xml file in question, first making sure
	 * the file hasn't yet been parsed, then checking if it is there, and then parsing the document
	 * @return - returns nothing (void)
	 */
	private void parse() {
		if (!this.file_opened) {
			this.file_opened = true;
			String currentPath = System.getProperty("user.dir");
			Path pwd = Paths.get(currentPath);
			Path xml_path = pwd.resolve(this.xml_filename);
			try {
				this.xml_file = new File(xml_path.toString());
				this.file_exists = xml_file.exists();
				if (this.file_exists) {
					this.document_factory = DocumentBuilderFactory.newInstance();
					// simplify the document that is parsed to focus on text content
					this.document_factory.setCoalescing(true);
					this.document_factory.setExpandEntityReferences(true);
					this.document_factory.setIgnoringComments(true);
					this.document_factory.setIgnoringElementContentWhitespace(true);
					this.builder = this.document_factory.newDocumentBuilder();
					this.doc = this.builder.parse(this.xml_file);
					NodeList mapping_tags = this.doc.getElementsByTagName(this.mapping_tag);
					int length = mapping_tags.getLength();
					if (length > 0) {
						this.current_node = mapping_tags.item(this.counter);
						this.root_node = this.current_node.getParentNode();
					}
				}
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}


	/**
	 * getText - a private helper method that gets all the text under the node xml_node, searching
	 * through the entire node subtree starting with xml_node, and getting all text in text nodes that
	 * are direct children, text nodes that are children in element child nodes, and so on
	 * - code to get text was copied from Oracle Java Docs on parsing XML:
	 *   https://docs.oracle.com/javase%2Ftutorial%2F/jaxp/dom/readingXML.html
	 * @param node - a Node instance that represents an element node
	 * @return - returns the String of all text in all subnodes concatenated together
	 */
	private String getText(Node node) {
	    StringBuffer result = new StringBuffer();
	    if (! node.hasChildNodes()) return "";

	    NodeList list = node.getChildNodes();
	    for (int i=0; i < list.getLength(); i++) {
		Node subnode = list.item(i);
		if (subnode.getNodeType() == Node.TEXT_NODE) {
		    result.append(subnode.getNodeValue());
		}
		else if (subnode.getNodeType() == Node.CDATA_SECTION_NODE) {
		    result.append(subnode.getNodeValue());
		}
		else if (subnode.getNodeType() == Node.ENTITY_REFERENCE_NODE) {
		    // Recurse into the subtree for text
		    // (and ignore comments)
		    result.append(getText(subnode));
		}
	    }
	    return result.toString();
	}


	/**
	 * hasNext - a public method to check if inside the this.root_tag element there are any more
	 * tags with the name according to this.mapping_tag, as those tags will have text inside them
	 * to be parsed and returned
	 * @return - true if there are any more tags with the name according to this.mapping_tag to be
	 * parsed, false otherwise
	 */
	public boolean hasNext() {
		this.parse();
		if (!this.file_exists) return false;
		NodeList mapping_tags = this.doc.getElementsByTagName(this.mapping_tag);
		int length = mapping_tags.getLength();
		if (length > 0) {
			return true;
		}
		return false;
	}


	/**
	 * next - a public method that parses the next tag named according to this.mapping_tag, and returns
	 * the text content inside it, and then deletes this next tag
	 * - this method is used in tandem with the hasNext method, and requires that you run this.hasNext()
	 *   first
	 * @return - a String representing the text content inside the next mapping tag
	 */
	public String next() {
		this.parse();
		String text = this.getText(this.current_node);
		this.root_node.removeChild(this.current_node);
		NodeList mapping_tags = this.doc.getElementsByTagName(this.mapping_tag);
		int length = mapping_tags.getLength();
		if (length > 0) {
			this.current_node = mapping_tags.item(this.counter);
			this.root_node = this.current_node.getParentNode();
		}
		return text;
	}


	/**
	 * writeXML - a public method that saves the current state of the XML dom specified by this.doc to the
	 * file called this.xml_file
	 * Context:
	 * - when you iterate over tags, you will iterate over instances of this.mapping_tag one by one,
	 * parsing and returning the text contained in the tag, and deleting and tag after you have parsed the
	 * text
	 * - at this point, the current state of the XML dom is different from the parsed XML file
	 * - this method will write the changes to the XML dom to the XML file specified by this.xml_file
	 * - code copied from here: https://docs.oracle.com/javase%2Ftutorial%2F/jaxp/xslt/writingDom.html
	 * @return - returns nothing (void)
	 */
	public void writeXML() {
		try {
			TransformerFactory transform_factory = TransformerFactory.newInstance();
			Transformer transformer = transform_factory.newTransformer();
			// preserve indentation
			//transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			DOMSource modified_dom_source = new DOMSource(this.doc);
			StreamResult output_result = new StreamResult(this.xml_file);
			transformer.transform(modified_dom_source, output_result);
		} catch (TransformerConfigurationException err) {
			System.out.println("Transformer Factory error:");
			System.out.println(err.getMessage());
			Throwable error = err;
			error.printStackTrace();
		} catch (TransformerException err) {
			System.out.println("Transformation Error:");
			System.out.println(err.getMessage());
			Throwable error = err;
			error.printStackTrace();
		}
	}
}

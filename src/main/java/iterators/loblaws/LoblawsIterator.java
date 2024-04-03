package iterators.loblaws;
import java.util.HashMap;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.File;
import java.io.IOException;
import iterators.GroceryStorePriceScraper;

public class LoblawsIterator implements GroceryStorePriceScraper {
	private int counter;
	private int limit;
	private String fpath;
	private HashMap<String, String> numbers;
	private Properties configurations;

	public LoblawsIterator(String config_file_path, int count, int limit) {
		this.counter = count;
		this.limit = limit;
		this.numbers = new HashMap<>();
		this.fpath = config_file_path;
		File filename = new File(config_file_path);
		//System.out.println(new File(".").getAbsolutePath());
		//System.out.println(filename.exists());
		//System.out.println(filename.canRead());
		//System.out.println(filename.isFile());
		//InputStream propertiesInputStream = null;
                this.configurations = new Properties();
                //propertiesInputStream = LoblawsIterator.class.getClassLoader().getResourceAsStream(config_file_path);
		try {
			this.configurations.load(new FileInputStream(config_file_path));
		} catch (Throwable t) {
			t.printStackTrace();
		}
		//this.configurations = new Properties();
		//this.configurations.load(new FileInputStream(filename));

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

	public String getUrl() {
		return this.configurations.getProperty("url");
		//return this.fpath;
	}
}

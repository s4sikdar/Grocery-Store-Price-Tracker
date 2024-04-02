package iterators;

import iterators.GroceryStorePriceScraper;
import iterators.loblaws.LoblawsIterator;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        //System.out.println( "Hello World!" );
	LoblawsIterator loblaws_iter = new LoblawsIterator(0, 10);
	while (loblaws_iter.hasNext()) {
		System.out.println(loblaws_iter.next());
	}
    }
}

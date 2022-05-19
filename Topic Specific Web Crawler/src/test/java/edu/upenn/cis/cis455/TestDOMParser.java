package edu.upenn.cis.cis455;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.junit.Test;
import org.xml.sax.SAXException;

import edu.upenn.cis.cis455.xpathengine.SAXHandler;
import edu.upenn.cis.cis455.xpathengine.XPathEngineFactory;
import edu.upenn.cis.stormlite.bolt.OutputCollector;

public class TestDOMParser {
	
	
	@Test
	public void testXML() {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		try {
//			String xml = "<!DOCTYPE html><item>\n"
//					+ "<title>\nNYT-TEST<a></a> TEST2</title>\n"
//					+ "<link>http://www.nytimes.com/2006/02/03/business/03econ.html?ex=1296622800&en=4f981f2cff6e9c05&ei=5088&partner=rssnyt&emc=rss</link>\n"
//					+ "<description>The combination of slowing productivity and rising labor costs could set off inflation, and is expected to attract the attention of the Federal Reserve.</description>\n"
//					+ "<author>THE ASSOCIATED PRESS</author>\n"
//					+ "<pubDate>Fri, 03 Feb 2006 00:00:00 EDT</pubDate>\n"
//					+ "<guid isPermaLink=\"false\">http://www.nytimes.com/2006/02/03/business/03econ.html</guid>\n"
//					+ "</item>";
			String xml = "  <head>\r\n"
					+ "        <title>"
					+ "				\r\nValidation autograder cis555\r\n"
					+ "		</title>\r\n"
					+ "    </head>";
			// mock DOMParser;
			SAXParser parser = factory.newSAXParser();
			SAXHandler handler = (SAXHandler) XPathEngineFactory.getSAXHandler();
			handler.init(new OutputCollector(null), "xml", false);
			InputStream in = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8.name()));
			parser.parse(in, handler); // see printed output in console
			
		} catch (ParserConfigurationException e) {
			System.out.println(e.getMessage());
		} catch (SAXException e) {
			System.out.println(e.getMessage());
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}

	@Test
	public void testHTML() {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		try {
			String html = "<!DOCTYPE html>\r\n<html><head><title>Testing data</title></head><body>\r\n"
					+ "<h2>Testing Data</h2>\r\n"
					+ "<ul>\r\n"
					+ "<li><a href=\"tpc\">tpc-h</a></li>\r\n"
					+ "</ul>\r\n"
					+ "<h2>Private data (not to be crawled)</h2>\r\n"
					+ "<ul>\r\n"
					+ "<li><a href=\"private/middleeast.xml\">Middle East data</a></li>\r\n"
					+ "</ul>\r\n"
					+ "</body></html>\r\n";
			
			// mock DOMParser;
			SAXParser parser = factory.newSAXParser();
			SAXHandler handler = (SAXHandler) XPathEngineFactory.getSAXHandler();
			handler.init(new OutputCollector(null), "html", true);
			InputStream in = new ByteArrayInputStream(html.getBytes(StandardCharsets.UTF_8.name()));
			parser.parse(in, handler); // see printed output in console
			
		} catch (ParserConfigurationException e) {
			System.out.println(e.getMessage());
			} catch (SAXException e) {
				System.out.println(e.getMessage());
			} catch (IOException e) {
				System.out.println(e.getMessage());
			}
	}
	
}

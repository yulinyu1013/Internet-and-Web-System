package edu.upenn.cis.cis455.xpathengine;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import edu.upenn.cis.stormlite.bolt.OutputCollector;
import edu.upenn.cis.stormlite.tuple.Values;

/**
 * Handle SAX event
 * */
public class SAXHandler extends DefaultHandler {
	final static Logger logger = LogManager.getLogger(SAXHandler.class);
	private OutputCollector collector;
	private String url;
	private int depth = 0;
	private boolean isHtml;
	private StringBuilder sb = new StringBuilder();
	private int textDepth = -1;
	
	
	public void init(OutputCollector collector, String url, boolean isHtml){
		this.collector = collector;
		this.url = url;
		this.isHtml = isHtml;
	}
	
	@Override
    public void startDocument() throws SAXException {
        System.out.println("start document   : ");
        // logger.info("start document   : ");
    }
	
	@Override
    public void endDocument() throws SAXException {
       System.out.println("end document     : ");
       // logger.info("end document     : ");
       OccurrenceEvent event = new OccurrenceEvent(OccurrenceEvent.Type.Text, "ENDDOC", url, -1, isHtml);
       collector.emit(new Values<Object>(event));
    }
	
	@Override
    public void startElement(String uri, String localName, String qName, Attributes attributes)
    throws SAXException {

		if(sb.toString().replaceAll("\r|\r\n|\n|\\s", "").length() > 0 ) {
				OccurrenceEvent event = new OccurrenceEvent(OccurrenceEvent.Type.Text, sb.toString(), url, depth, isHtml);
				System.out.println("start characters : " + sb.toString()+", level "+ depth);
				logger.info("start characters: " + sb.toString()+", level "+ depth+ ", " +url);
		    	collector.emit(new Values<Object>(event));		
		}
	    sb = new StringBuilder();
	    textDepth = -1;

		
    	OccurrenceEvent event = new OccurrenceEvent(OccurrenceEvent.Type.Open, qName, url, depth, isHtml);
    	System.out.println("start element    : " + qName+", level "+ depth + ", " +url);
    	logger.info("start element    : " + qName+", level "+ depth+ ", " +url);
    	depth++;
    	collector.emit(new Values<Object>(event));
    }

	@Override
    public void endElement(String uri, String localName, String qName)
    throws SAXException {
		//handle text first
		
		if(sb.toString().replaceAll("\r|\r\n|\n|\\s", "").length() > 0 ) {
				OccurrenceEvent event = new OccurrenceEvent(OccurrenceEvent.Type.Text, sb.toString(), url, depth, isHtml);
				System.out.println("start characters : " + sb.toString()+", level "+ depth);
				logger.info("start characters: " + sb.toString()+", level "+ depth+ ", " +url);
		    	collector.emit(new Values<Object>(event));		
		}
		sb = new StringBuilder();
	    textDepth = -1;
		
		
		//handle end element
		depth--;
    	OccurrenceEvent event = new OccurrenceEvent(OccurrenceEvent.Type.Close, qName, url, depth, isHtml);

    	System.out.println("end element      : " + qName+", level "+ depth+", " +url);
    	 logger.info("end element    : " + qName+", level "+ depth+ ", " +url);
    	collector.emit(new Values<Object>(event));
    }
	
	@Override
    public void characters(char ch[], int start, int length)
    throws SAXException {
		textDepth = depth;
		String curr = new String(ch, start, length);
		System.out.println("curr text length: "+length);
		logger.debug("char at level "+textDepth +": "+ new String(ch, start, length));
		sb.append(curr);
		logger.debug("current text: " + sb.toString());
		System.out.println("current text: " + sb.toString());
    }

}

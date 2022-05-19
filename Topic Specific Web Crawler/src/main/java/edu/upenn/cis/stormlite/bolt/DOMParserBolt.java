package edu.upenn.cis.stormlite.bolt;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

import edu.upenn.cis.cis455.crawler.Crawler;
import edu.upenn.cis.cis455.crawler.CrawlerFactory;
import edu.upenn.cis.cis455.storage.Content;

import edu.upenn.cis.cis455.xpathengine.SAXHandler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ccil.cowan.tagsoup.jaxp.SAXParserImpl;
import edu.upenn.cis.cis455.xpathengine.XPathEngineFactory;
import edu.upenn.cis.stormlite.OutputFieldsDeclarer;
import edu.upenn.cis.stormlite.TopologyContext;
import edu.upenn.cis.stormlite.routers.IStreamRouter;
import edu.upenn.cis.stormlite.tuple.Fields;
import edu.upenn.cis.stormlite.tuple.Tuple;


public class DOMParserBolt implements IRichBolt {
	final static Logger logger = LogManager.getLogger(DOMParserBolt.class);
	
	private String executorId = UUID.randomUUID().toString();
	private Crawler crawler = CrawlerFactory.getCrawler();
	private Fields schema = new Fields("OccurrenceEvent"); 
	private OutputCollector collector;
	

	@Override
	public String getExecutorId() {
		return this.executorId;
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(schema);
	}

	@Override
	public void cleanup() {

	}

	@Override
	public void execute(Tuple input) {

		Content doc = (Content) input.getObjectByField("Doc");
		logger.info(doc.getContent());
		InputStream in = null;
		try {
			in = new ByteArrayInputStream(doc.getContent().getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e1) {
		
			
		}
		
		
		if(doc.getType().endsWith("xml")) {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			
			try {
				SAXParser parser = factory.newSAXParser();
				SAXHandler handler = (SAXHandler) XPathEngineFactory.getSAXHandler();
				crawler.setWorking(true); //start working on matching;
				handler.init(collector, doc.getUrl(), false);
				parser.parse(in, handler);
				
			} catch (ParserConfigurationException e) {
			
				logger.debug("xml sax parser config");
			} catch (SAXException e) {
			
				logger.debug("xml sax parser exception");
				logger.debug(e.getMessage());
				for(StackTraceElement s : e.getStackTrace()) {
					logger.info(s.toString());
				}
				
//				e.printStackTrace();
			} catch (IOException e) {
			
				logger.debug("xml sax parsing exception");
			}
			
			
		} else { //html
			
			try {
				SAXParserImpl parser = SAXParserImpl.newInstance(null);
				SAXHandler handler = (SAXHandler) XPathEngineFactory.getSAXHandler();
				handler.init(collector, doc.getUrl(), true);
				crawler.setWorking(true); //start working on matching;
				parser.parse(in, handler);
				
			} catch (SAXException e) {
		
				logger.debug("html sax parser exception");
			} catch (IOException e) {

				logger.debug("html sax parsing exception");
			}
		}
		
		
	}


	@Override
	public void prepare(Map<String, String> stormConf, TopologyContext context, OutputCollector collector) {
		this.collector = collector;
	}

	@Override
	public void setRouter(IStreamRouter router) {
		collector.setRouter(router);
		
	}

	@Override
	public Fields getSchema() {
		return schema;
	}
	
}

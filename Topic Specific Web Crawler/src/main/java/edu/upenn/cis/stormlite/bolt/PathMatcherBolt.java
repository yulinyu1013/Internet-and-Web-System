package edu.upenn.cis.stormlite.bolt;


import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;

import edu.upenn.cis.cis455.crawler.Crawler;
import edu.upenn.cis.cis455.crawler.CrawlerFactory;
import edu.upenn.cis.cis455.storage.Channel;
import edu.upenn.cis.cis455.storage.Storage;
import edu.upenn.cis.cis455.xpathengine.OccurrenceEvent;
import edu.upenn.cis.cis455.xpathengine.XPathEngineFactory;
import edu.upenn.cis.cis455.xpathengine.XPathEngineImpl;
import edu.upenn.cis.stormlite.OutputFieldsDeclarer;
import edu.upenn.cis.stormlite.TopologyContext;
import edu.upenn.cis.stormlite.routers.IStreamRouter;
import edu.upenn.cis.stormlite.tuple.Fields;
import edu.upenn.cis.stormlite.tuple.Tuple;

public class PathMatcherBolt implements IRichBolt {
	static Logger logger = (Logger) LogManager.getLogger(PathMatcherBolt.class);
	private String executorId = UUID.randomUUID().toString();
	private Crawler crawler = CrawlerFactory.getCrawler();
	private Storage db = (Storage) crawler.getDb();
	private Fields schema = new Fields();
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
		logger.info("entering matcher bolt");

		XPathEngineImpl xPathEngine = (XPathEngineImpl) XPathEngineFactory.getXPathEngine();
//		logger.debug("empty engine? "+xPathEngine.toString());
		logger.info("getting event");
		OccurrenceEvent oe = (OccurrenceEvent) input.getObjectByField("OccurrenceEvent");
		
		
		// evaluate events
		boolean[] evaluations = xPathEngine.evaluateEvent(oe);
		
		if(evaluations == null) {
			logger.debug("empty event...");
			return;
		}
		
	
		logger.info("event: "+ oe.getType().toString() + " " +oe.getValue() + ", level "+oe.getDepth());
		
		if(oe.getValue().equals("ENDDOC")) {
			logger.info("End doc!!");
			try {
		    for(int i=0; i < evaluations.length; i++) {
		        if(evaluations[i]) {
		        	Channel updated = xPathEngine.getChannels()[i];
		        	updated.addDoc(oe.getUrl());
		        	logger.info(updated.getName() + "adds a new url: " + oe.getUrl());
		            db.getChannelApi().addChannel(updated);
		            logger.info("url added to channel");
		        }else {
		        	logger.info("no match!");
		        }
		    }
		    crawler.setWorking(false);
		    crawler.setWorking(false);
			}catch(Exception e) {
				logger.debug("DB Exception");
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

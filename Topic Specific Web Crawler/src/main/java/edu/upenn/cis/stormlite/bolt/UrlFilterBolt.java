package edu.upenn.cis.stormlite.bolt;

import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.crawler.Crawler;
import edu.upenn.cis.cis455.crawler.CrawlerFactory;
import edu.upenn.cis.cis455.crawler.utils.URLInfo;
import edu.upenn.cis.cis455.storage.Storage;
import edu.upenn.cis.stormlite.OutputFieldsDeclarer;
import edu.upenn.cis.stormlite.TopologyContext;
import edu.upenn.cis.stormlite.routers.IStreamRouter;
import edu.upenn.cis.stormlite.tuple.Fields;
import edu.upenn.cis.stormlite.tuple.Tuple;

public class UrlFilterBolt implements IRichBolt{
	final static Logger logger = LogManager.getLogger(UrlFilterBolt.class);
	
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

		try {
			String url = input.getStringByField("ExtractedUrl");
			String normalizedUrl = new URLInfo(url).toString();
			if(!crawler.getCrawledUrl().contains(normalizedUrl)) {

				logger.info("adding url to queue...");
				crawler.getUrlFrontier().add(normalizedUrl);
				logger.info("url added...");
					
			}
		}catch(Exception e) {
			logger.debug("Likely crawling is done.");
		}
		

		crawler.setWorking(false);
		
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

package edu.upenn.cis.stormlite.spout;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.crawler.Crawler;
import edu.upenn.cis.cis455.crawler.CrawlerFactory;
import edu.upenn.cis.stormlite.OutputFieldsDeclarer;
import edu.upenn.cis.stormlite.TopologyContext;
import edu.upenn.cis.stormlite.routers.IStreamRouter;
import edu.upenn.cis.stormlite.tuple.Fields;
import edu.upenn.cis.stormlite.tuple.Values;

public class CrawlerQueueSpout implements IRichSpout{
	
	final static Logger logger = LogManager.getLogger(CrawlerQueueSpout.class);
	private String executorId = UUID.randomUUID().toString();
	private SpoutOutputCollector collector;
	private Crawler crawler =  CrawlerFactory.getCrawler();
	private BlockingQueue<String> urlQueue = CrawlerFactory.getCrawler().getUrlFrontier();

	@Override
	public String getExecutorId() {
		return this.executorId;
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields("URL"));
		
	}

	@Override
	public void open(Map<String, String> config, TopologyContext topo, SpoutOutputCollector collector) {
		this.collector = collector;
	}

	@Override
	public void close() { 
		return;
		
	}

	@Override
	public void nextTuple() {
//		logger.info("spout starts...");
//		logger.info("num working threads: "+ crawler.getNumWorkingThread());
		
		if(urlQueue != null && !urlQueue.isEmpty() && crawler.getNumIndexed() < crawler.getMaxNumDoc()){	
			try {
				logger.info("trying to take url from queue...");
				String url = urlQueue.take();	
				crawler.setWorking(true);
				crawler.setWorking(true);
				collector.emit(new Values<Object>(url));
				logger.info("Url sent to fetcher: "+ url);
			
			} catch (InterruptedException e) {
				logger.debug("url take interrputed!");
				e.printStackTrace();
			}
		}
		
		Thread.yield();
		
	}

	@Override
	public void setRouter(IStreamRouter router) {
		this.collector.setRouter(router);
		
	}

}

package edu.upenn.cis.stormlite.bolt;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import edu.upenn.cis.cis455.crawler.Crawler;
import edu.upenn.cis.cis455.crawler.CrawlerFactory;
import edu.upenn.cis.cis455.storage.Content;
import edu.upenn.cis.stormlite.OutputFieldsDeclarer;
import edu.upenn.cis.stormlite.TopologyContext;
import edu.upenn.cis.stormlite.routers.IStreamRouter;
import edu.upenn.cis.stormlite.tuple.Fields;
import edu.upenn.cis.stormlite.tuple.Tuple;
import edu.upenn.cis.stormlite.tuple.Values;

public class LinkExtractorBolt implements IRichBolt{
	final static Logger logger = LogManager.getLogger(LinkExtractorBolt.class);
			

	private String executorId = UUID.randomUUID().toString();
	private Crawler crawler = CrawlerFactory.getCrawler();
	private Fields schema = new Fields("ExtractedUrl");
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
		logger.info("entering link extractor...");
		
		Content doc = (Content) input.getObjectByField("Doc");
		
		if(doc.getType().equals("text/html")) {
			String body = doc.getContent();
			Document html = Jsoup.parse(body);
			Elements links = html.getElementsByAttribute("href");
			for (Element link : links) {
				String url = link.attr("abs:href");
				
				if(url.isEmpty()) {
					try {
						url = new URL(new URL(doc.getUrl()), link.attr("href")).toString();
					} catch (MalformedURLException e) {
						logger.info("bad url");
//						e.printStackTrace();
					}
				}
				
				logger.info("link found: "+ url);				
				crawler.setWorking(true);// for filter
				collector.emit(new Values<Object>(url));
				}	
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

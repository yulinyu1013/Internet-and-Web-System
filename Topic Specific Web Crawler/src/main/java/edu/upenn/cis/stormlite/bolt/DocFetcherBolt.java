package edu.upenn.cis.stormlite.bolt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.crawler.Crawler;
import edu.upenn.cis.cis455.crawler.CrawlerFactory;
import edu.upenn.cis.cis455.crawler.utils.HttpReqRes;
import edu.upenn.cis.cis455.crawler.utils.RobotsTxtInfo;
import edu.upenn.cis.cis455.crawler.utils.URLInfo;
import edu.upenn.cis.cis455.storage.Content;
import edu.upenn.cis.cis455.storage.ContentSeen;
import edu.upenn.cis.cis455.storage.Storage;
import edu.upenn.cis.stormlite.OutputFieldsDeclarer;
import edu.upenn.cis.stormlite.TopologyContext;
import edu.upenn.cis.stormlite.routers.IStreamRouter;
import edu.upenn.cis.stormlite.tuple.Fields;
import edu.upenn.cis.stormlite.tuple.Tuple;
import edu.upenn.cis.stormlite.tuple.Values;

public class DocFetcherBolt implements IRichBolt {
	final static Logger logger = LogManager.getLogger(DocFetcherBolt.class);
	private String executorId = UUID.randomUUID().toString();
	private final String USERAGENT = "cis455crawler";
	private Crawler crawler = CrawlerFactory.getCrawler();
	private Storage db = (Storage) crawler.getDb();
	private Fields schema = new Fields("Doc", "URL");
	private OutputCollector collector;
	
	
	//current task
	boolean isCrawled = false;
	boolean isModified = false;
    private String url = null;
    private String normalizedUrl = null;
    private URLInfo urlInfo = null;
	
	
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
		isCrawled = false;
		isModified = false;
	    url = null;
	    normalizedUrl = null;
	}

	@Override
	public void execute(Tuple input) {
		logger.info("Enter doc fetcher...");
		url = input.getStringByField("URL");
		urlInfo = new URLInfo(url);
		normalizedUrl = urlInfo.toString();
		Content doc;
		
		try {
			new URL(url);
		} catch (MalformedURLException e) {
			crawler.setWorking(false);
			crawler.setWorking(false);
			return;
		}
		
		if(ifAllowedAndNotDelay(url)) {
			if(ifCrawl(url)) {
				logger.info(url + ": downloading");
				doc = crawlDocument(urlInfo); 
				
				if(doc != null) {
					logger.debug(doc.getContent());
					collector.emit(new Values<Object>(doc, normalizedUrl));

					logger.info("url sent to extractor and parser...");
//					logger.info(crawler.getNumWorkingThread());
				}else {//reach max
					crawler.setWorking(false);
					crawler.setWorking(false);
					logger.debug("crawled doc is NULL; check if crawler is done");
				}
				
			}else {
				if (isCrawled && !isModified){ //304
					logger.info(url + ": not modified"); 
					doc = crawlLocalDocument(normalizedUrl);
					if(doc!=null) {
						collector.emit(new Values<Object>(doc, normalizedUrl));
					}else {// reach max
						crawler.setWorking(false);
						crawler.setWorking(false);
						logger.debug("doc from db is NULL; check if crawler is done");
					}
					
				} else {// 301/302/400/500, stop working
					crawler.setWorking(false);
					crawler.setWorking(false);
				}
			}
		} else {
			crawler.setWorking(false); // disallowed, stop working
			crawler.setWorking(false);
		}

		logger.info("Done fetching...");
		
		
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
	
	/**
	 * fetch robots.txt
	 * @throws IOException 
	 * */
	private RobotsTxtInfo getRobotsTxtInfo(URLInfo urlInfo) {
		
		RobotsTxtInfo r = null;
		ConcurrentHashMap<String, RobotsTxtInfo> seenRobotsTxt = crawler.getSeenRobotsTxt();
		if(seenRobotsTxt.containsKey(urlInfo.getHostName())) {
			logger.info("robots.txt is seen...");
			return seenRobotsTxt.get(urlInfo.getHostName());
		}

		logger.info("robots.txt is not seen...");
		

		try {
			URL baseURL = new URL((urlInfo.isSecure() ? "https://" : "http://") + urlInfo.getHostName());
		
			String robotStr = new URL(baseURL, "/robots.txt").toString();
			URLInfo robotUrl = new URLInfo(robotStr);
			logger.info("Crawling robots.txt: "+ robotStr);
			logger.info("Sending a head request...");
			HttpReqRes headRes = new HttpReqRes().getResponse(robotUrl, "HEAD", null);
			
			
			// create
			if(headRes.getStatusCode() == 200) {
				HttpReqRes getRes = new HttpReqRes().getResponse(robotUrl, "GET", null);
				r = parseRobotTxt(getRes);
				logger.info(getRes.getBody());
				Instant now = Instant.now();
				r.setLastChecked(now);
				if(Instant.now().isBefore(now.plusMillis(r.getCrawlDelay(USERAGENT)))) {
					logger.info("wait for robot");
				}
				while(Instant.now().isBefore(now.plusMillis(r.getCrawlDelay(USERAGENT)))) {
					// wait...
				}

				logger.info("Adding robots.txt...");
				seenRobotsTxt.put(urlInfo.getHostName(), r);
			}
		} catch (MalformedURLException e) {
	
			logger.debug("found bad url from robot checking");
		} catch (IOException e) {

			logger.debug("found bad response from robot checking");
		}
		
		 
		return r;
	}
	
	
	/**
	 * Parse robots.txt
	 * */
	private RobotsTxtInfo parseRobotTxt(HttpReqRes res) {
		logger.info("Parsing robots.txt...");
		
		String body = res.getBody();
//		logger.info("body: " + body);
		RobotsTxtInfo r = new RobotsTxtInfo();
		String userAgent = null;
		
		try {
			BufferedReader reader = new BufferedReader(new StringReader(body));
			String line = reader.readLine();
			
			while(line != null) {
			
				String[] header = line.split(":");
				
				if(header.length != 2) {
					userAgent = null;
			
				} else {
					String key = header[0].trim();
					String value = header[1].trim();
					
					if(key.equalsIgnoreCase("User-agent")){
//						logger.info("User-Agent found: "+ value);
						r.addUserAgent(value);
						userAgent = value;
					}
					
					if(key.equalsIgnoreCase("Disallow")) {
						r.addDisallow(userAgent, value);
					}
					
					if(key.equalsIgnoreCase("Crawl-delay")) {
						r.addCrawlDelay(userAgent, Integer.parseInt(value));
					}
				}
				
	
				line = reader.readLine();
			}
			
			logger.info("Finish parsing robots.txt...");
		} catch (IOException e) {
			logger.debug("Error during parsing robots.txt");
//			e.printStackTrace();
		}

		return r;
		
	}

	
	private boolean ifAllowedAndNotDelay(String url) {
		
		URLInfo urlInfo = new URLInfo(url);
		RobotsTxtInfo robot = getRobotsTxtInfo(urlInfo);
		
		if(robot == null) {
			logger.info("No robot.txt");
			return true;
		}
		
		String path = urlInfo.getFilePath();
		logger.info("path: " + path);
		ArrayList<String> disallowedUrls;
		
		// check user agent
		logger.info("Checking user agent: " + USERAGENT);
		if(robot.containsUserAgent(USERAGENT)) {
			logger.info(USERAGENT + " found");
			disallowedUrls = robot.getDisallows(USERAGENT);
			if(disallowedUrls != null) {
				logger.info("Checking disallowed urls...");
				for (String u : disallowedUrls) {
					logger.info("Check url: " + u);
					if (path.startsWith(u)) {
							logger.info(url + ": disallowed");
//							crawler.setWorking(false);
						return false;
					}
				}
			}
			
			//check delay
			 logger.info("Checking delay...");
			 Instant now = Instant.now();
			 Instant delayNeeded = robot.getLastChecked().plusSeconds(robot.getCrawlDelay(USERAGENT));
			 if(delayNeeded.minusMillis(now.toEpochMilli()).toEpochMilli() > 0) {
				 logger.info("delay needed to pass time:" + delayNeeded);
				 robot.setLastChecked(robot.getLastChecked().plusSeconds(robot.getCrawlDelay(USERAGENT)));
				 
				 if(Instant.now().isBefore(delayNeeded)) {
						logger.info("wait to pass delay");
				 }
				 while(Instant.now().isBefore(delayNeeded)){
					 // wait;
				 }
				 
				 return true;

			 }else {
				 robot.setLastChecked(now);
			 }
	

		} else if(robot.containsUserAgent("*")) {
			disallowedUrls = robot.getDisallows("*");
			if(disallowedUrls != null) {
				logger.info("Checking disallowed urls...");
				for (String u : disallowedUrls) {
					if (path.startsWith(u)) {
						logger.info(url + ": disallowed");
//						crawler.setWorking(false);
						return false;
					}
				}
			}
			 logger.info("Checking delay...");
			 Instant now = Instant.now();
			 Instant delayNeeded = robot.getLastChecked().plusSeconds(robot.getCrawlDelay("*"));
			 if(delayNeeded.minusMillis(now.toEpochMilli()).toEpochMilli() > 0) {
				 logger.info("delay needed to pass time:" + delayNeeded);
				 robot.setLastChecked(robot.getLastChecked().plusSeconds(robot.getCrawlDelay("*")));
				 
				 if(Instant.now().isBefore(delayNeeded)) {
						logger.info("wait to pass delay");
				}
				 while(Instant.now().isBefore(delayNeeded)){
					 // wait;
				 }
				 
				 return true;

			 }else {
				 robot.setLastChecked(now);
			 }

		}
		
		logger.info("Url allowed...");
		return true;
		
		

		
	}
	
	/**
	 * Check if the content is crawled and modified
	 * @throws IOException 
	 * */
	private boolean ifCrawl(String url)  {
		URLInfo urlInfo = new URLInfo(url);
		String normalizedUrl = urlInfo.toString();
		
		try {
			//check maxDoc
			logger.info("Checking maxDoc...");
			if(crawler.getNumIndexed() >= crawler.getMaxNumDoc()) {
				logger.info("Exceed maxDoc, stop crawling");
				return false;
			}
				
			Instant lastChecked = null;
			
			// check if is crawled
			logger.info("Checking if url is crawled...");
			//TODO: handle exception
			Content doc = db.getDocApi().getDocument(normalizedUrl);
			if(doc != null) {
				logger.info("Url is crawled...");
				isCrawled = true;
				lastChecked = doc.getLastChecked();
			} else {
				logger.info("Url is not crawled...");
			}
	
			
			
			// send head request
			logger.info("Sending head request for: " + url);
		
		 	HttpReqRes res = new HttpReqRes().getResponse(urlInfo, "HEAD", lastChecked);
	
		
			int code = res.getStatusCode();
			logger.info("code: " + code);
			
			if(code == 301 || code == 302) { // redirect
				logger.info("Url redirect...");
				if(res.getLocation()!= null) {
					logger.info("Adding location to queue...");
					crawler.getUrlFrontier().put(res.getLocation());
				}
				return false;
				
			} 
			
			if (code == 304) { // no modified
				logger.info("Url not modified...");
				return false;
				
			} 
			
			if (code == 200) { // new or update
				
				if(isCrawled) {
					logger.info("Url is modified...");
					isModified = true;
				}
				
				logger.info("Unseen url...");
				
				return isValidSize(res) && isValidType(res);
			}
		} catch (IOException e) {
			logger.debug("IOException from ifcrawl.");
//			e.printStackTrace();
		} catch (InterruptedException e) {

//			e.printStackTrace();
			logger.debug("InterruptedException from ifcrawl.");
		} catch(NullPointerException e) {
			
			logger.debug("found db closed when check if crawlable. likely crawler is done.");
		}

		// others: 400/500 error
		logger.info("May be 400/500 error. Stop...");
		return false;
	}

	
	
    /**
     * Check doc size
     */
	public boolean isValidSize(HttpReqRes res) {
		if(res.getContentLength() > crawler.getSize() * 1024 * 1024) {
			logger.info("Exceed max size. Stop...");
			return false;
		}else {
			return true;
		}
	}

	
    /**
     * Check doc type
     */
	public boolean isValidType(HttpReqRes res) {
		if(res.getContentType() == null) {
			logger.info("No content type. Stop...");
			return false;
		}
		
		if(!res.getContentType().equals("text/html")
				&& !res.getContentType().equals("text/xml") 
				&& !res.getContentType().equals("application/xml")
				&& !res.getContentType().endsWith("+xml")) {
			logger.info("Invalid content type. Stop...");
			return false;
		}
		
		logger.info("Pass pre-crawling check...");
		return true;
	}
	
	
	
	private Content crawlDocument(URLInfo urlInfo) {
		Content doc = null;
		
		logger.info("Sending req to the host url...");
		HttpReqRes res;
		try {
			res = new HttpReqRes().getResponse(urlInfo, "GET", null);
			if(res.getStatusCode() == 200) {
				logger.info("Generating doc...");
				doc = new Content(normalizedUrl, res.getContentType(), res.getContentLength(), res.getBody(), Instant.now());
				
				if(isCrawled) { // and modified
					if(crawler.getNumIndexed() >= crawler.getMaxNumDoc()) {
						return null;
					}
					// update doc
					db.addDocument(doc); 
					crawler.incCount();
					crawler.getCrawledUrl().add(doc.getUrl());
					return doc;
				}
				
				// check md5 content seen - same content, different url
				logger.info("Checking md5 of current doc...");
				ContentSeen temp = new ContentSeen(normalizedUrl, res.getBody());
				
				if (!db.getContentSeenApi().containsMD5(temp)) {
					logger.info("Content unseen...");
					
					// add new md5
					logger.info("Adding it to contentSeen table...");
					db.getContentSeenApi().addContentSeen(temp);
					
					// add new doc
					if(crawler.getNumIndexed() >= crawler.getMaxNumDoc()) {
						return null;
					}
					db.addDocument(doc); 
					crawler.incCount();
					crawler.getCrawledUrl().add(doc.getUrl());
					return doc;

				}else {
					crawler.setWorking(false);
					logger.info("Content seen...");
					
		
				}
			}
			
		} catch (IOException e) {
			logger.debug("found bad response when crawling doc...");
//			e.printStackTrace();
		} catch(NullPointerException e) {
			
			logger.debug("found db closed when crawling doc. likely crawler is done or null pointer for urlInfo");
//			e.printStackTrace();
		}
		
		return doc;
		
	}
		
	
	
	
	private Content crawlLocalDocument(String url) {
		// get doc from local
		logger.info("Getting doc from content table for url: " + normalizedUrl);
		try {
			if(crawler.getNumIndexed() >= crawler.getMaxNumDoc()) {
				return null;
			}
			Content doc = db.getDocument(url);
			return doc;
		} catch(NullPointerException e) {
			logger.debug("found db closed when crawling doc. likely crawler is done.");
		}
		
		return null;
		
		
	}

}

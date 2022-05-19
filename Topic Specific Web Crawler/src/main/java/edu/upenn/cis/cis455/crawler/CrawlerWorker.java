package edu.upenn.cis.cis455.crawler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import edu.upenn.cis.cis455.crawler.utils.HttpReqRes; 
import edu.upenn.cis.cis455.crawler.utils.RobotsTxtInfo;
import edu.upenn.cis.cis455.crawler.utils.URLInfo; 
import edu.upenn.cis.cis455.storage.Content;
import edu.upenn.cis.cis455.storage.ContentSeen;
import edu.upenn.cis.cis455.storage.StorageInterface;

public class CrawlerWorker implements Runnable {
	final static Logger logger = LogManager.getLogger(CrawlerWorker.class);
	
	//crawler
	private Crawler crawler;
	private BlockingQueue<String> urlQueue;
    private StorageInterface db;
    private int maxSize;
    private final String USERAGENT = "cis455crawler";
    private Thread t = new Thread(this);
    
    // task
    private ConcurrentHashMap<String, RobotsTxtInfo> seenRobotsTxt;
    private String url = null;
    private String normalizedUrl = null;
    private RobotsTxtInfo robot = null;
    private URLInfo urlInfo = null;
    private boolean isModified = false;
    private boolean isCrawled = false;
    private volatile boolean shutdown = false;
    
    //==============For test=============
	
	public void setUrl(String url) {
		this.url = url;
		this.urlInfo = new URLInfo(url);
		this.normalizedUrl = this.urlInfo.toString();
	}

	
	//=====================================

	public boolean isShutdown() {
		return shutdown;
	}

	public void setShutdown(boolean shutdown) {
		this.shutdown = shutdown;
	}

	public Thread getThread() {
		return t;
	}

	public CrawlerWorker(Crawler crawler) {
		this.crawler = crawler;
		this.urlQueue = crawler.getUrlFrontier();
		this.db = crawler.getDb();
		this.maxSize = crawler.getSize();
		this.seenRobotsTxt = crawler.getSeenRobotsTxt();
	}
	
	@Override
	public void run() {
		
		while(!crawler.isDone() && !isShutdown()) {
//			 logger.info("New task...");
			// reset
		    url = null;
		    normalizedUrl = null;
		    robot = null;
		    urlInfo = null;
		    isModified = false;
		    isCrawled = false;
		    
			try {
				
				// 1. Dequeue frontier URL
//				 logger.info("Dequeueing url...");
				
				//TODO: needed for patch
				if(crawler.isDone()) {
//					logger.info("crawler done!");
					break;
				}
			
				
				url = urlQueue.take();		
				crawler.setWorking(true);
				logger.info("working on: "+ url);
				
				urlInfo = new URLInfo(url);
				normalizedUrl = urlInfo.toString();
				
				// 2. check if crawlable
				logger.info("Checking robots.txt...");
				if(ifAllowedAndNotDelay()) {
					logger.info("Checking if crawlable...");
					if(ifCrawl()) { // If it hasn't been crawled before or crawled but modified
						 logger.info(url + ": downloading");
						
						// crawl document
						crawlDocument();
						
					}else {// If it has been crawled before and not modified
						if (isCrawled && !isModified){
							 logger.info(url + ": not modified");
							crawlLocalDocument();
						}
					}
					
				} 
				
				crawler.setWorking(false);

				
			} catch (IOException e ) {
				e.printStackTrace();
				logger.debug("Failed from step 2; likely the crawler is closed.");
				
				
			} catch(NullPointerException e) {
				logger.info("null pointer; db is closed!");
				
			} catch(IllegalStateException e){
				logger.debug("db is closed.");
				e.printStackTrace();
				break;
				
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				logger.debug("take interrupted.");

			} 	
		}
		
		if(crawler.isDone()) {
			 logger.info(Thread.currentThread().getName() + " is Done.");
		}
	
		
		crawler.notifyThreadExited();
	}
	
	/**
	 * fetch robots.txt
	 * @throws IOException 
	 * */
	private RobotsTxtInfo getRobotsTxtInfo() throws IOException, NullPointerException {
		
		RobotsTxtInfo r = null;
		
	
		if(seenRobotsTxt.containsKey(urlInfo.getHostName())) {
			// logger.info("robots.txt is seen...");
			return seenRobotsTxt.get(urlInfo.getHostName());
		}

		// logger.info("robots.txt is not seen...");
	
		// send https/http request and get response
		
		URL baseURL = new URL((urlInfo.isSecure() ? "https://" : "http://") + urlInfo.getHostName());
		String robotStr = new URL(baseURL, "/robots.txt").toString();
		URLInfo robotUrl = new URLInfo(robotStr);

		HttpReqRes headRes = new HttpReqRes().getResponse(robotUrl, "HEAD", null);
		
		
		// create
		if(headRes.getStatusCode() == 200) {
			HttpReqRes getRes = new HttpReqRes().getResponse(robotUrl, "GET", null);
			r = parseRobotTxt(getRes);
			Instant now = Instant.now();
			r.setLastChecked(now); //TODO: for patch:comment?
			try {
				Thread.sleep(r.getCrawlDelay(USERAGENT));
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// logger.info("Adding robots.txt...");
			seenRobotsTxt.put(urlInfo.getHostName(), r);
		}
		 
		return r;
	}
	
	
	/**
	 * Parse robots.txt
	 * */
	private RobotsTxtInfo parseRobotTxt(HttpReqRes res) {
		// logger.info("Parsing robots.txt...");
		
		String body = res.getBody();
		// logger.info("body: " + body);
		RobotsTxtInfo r = new RobotsTxtInfo();
		String userAgent = null;
		
		try {
			BufferedReader reader = new BufferedReader(new StringReader(body));
			String line = reader.readLine();
			
			while(line != null) {
				// logger.info("Robot line: "+ line);
				String[] header = line.split(":");
				
				if(header.length != 2) {
					userAgent = null;
			
				} else {
					String key = header[0].trim();
					String value = header[1].trim();
					
					if(key.equalsIgnoreCase("User-agent")){
//						// logger.info("User-Agent found: "+ value);
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
			
			// logger.info("Finish parsing robots.txt...");
		} catch (IOException e) {
			logger.debug("Error during parsing robots.txt");
//			e.printStackTrace();
		}

		return r;
		
	}

	
	/**
	 * Check if robots.txt allows crawling
	 * @throws IOException 
	 * */
	private boolean ifAllowedAndNotDelay() throws IOException, NullPointerException {

		try {

			new URL(url);
			
			robot = getRobotsTxtInfo();
			
			if(robot == null) {
				// logger.info("No robot.txt");
				return true;
			}
			
			String path = urlInfo.getFilePath();
			// logger.info("path: " + path);
			ArrayList<String> disallowedUrls;
			
			// check user agent
			// logger.info("Checking user agent: " + USERAGENT);
			if(robot.containsUserAgent(USERAGENT)) {
				// logger.info(USERAGENT + " found");
				disallowedUrls = robot.getDisallows(USERAGENT);
				if(disallowedUrls != null) {
					// logger.info("Checking disallowed urls...");
					for (String url : disallowedUrls) {
						// logger.info("Check url: " + url);
						if (path.startsWith(url)) {
							logger.info(this.url + ": disallowed");
							return false;
						}
					}
				}
				
				//check delay
				 logger.info("Checking delay...");
				 Instant now = Instant.now();
				 Instant delayNeeded = robot.getLastChecked().plusSeconds(robot.getCrawlDelay(USERAGENT)).minusMillis(now.toEpochMilli());
				 if(delayNeeded.toEpochMilli() > 0) {
					 logger.info("delay needed:" + delayNeeded.toEpochMilli());
					 robot.setLastChecked(robot.getLastChecked().plusSeconds(robot.getCrawlDelay(USERAGENT)));
					 try {
						Thread.sleep(delayNeeded.toEpochMilli());
						return true;
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						logger.debug("failed to sleep; many be crawler is done!");
					}
				 }else {
					 robot.setLastChecked(now);
				 }
//				if(robot.needDelay(USERAGENT)) {
//					logger.info("Url will be handled past delay");
//					urlQueue.add(url);
//					return false;
//				}

			} else if(robot.containsUserAgent("*")) {
				disallowedUrls = robot.getDisallows("*");
				if(disallowedUrls != null) {
					// logger.info("Checking disallowed urls...");
					for (String url : disallowedUrls) {
						if (path.startsWith(url)) {
							logger.info(this.url + ": disallowed");
							return false;
						}
					}
				}
				 logger.info("Checking delay...");
				 Instant now = Instant.now();
				 Instant delayNeeded = robot.getLastChecked().plusSeconds(robot.getCrawlDelay("*")).minusMillis(now.toEpochMilli());
				 if(delayNeeded.toEpochMilli() > 0) {
					 logger.info("delay needed:" + delayNeeded.toEpochMilli());
					 robot.setLastChecked(robot.getLastChecked().plusSeconds(robot.getCrawlDelay("*")));
					 try {
						Thread.sleep(delayNeeded.toEpochMilli());
						return true;
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						logger.debug("failed to sleep; many be crawler is done!");
					}
				 }else {
					 robot.setLastChecked(now);
				 }
	
			}
			
			// logger.info("Url allowed...");
			return true;
			
	
		}  catch (MalformedURLException e) {
			 logger.debug("Url malformed");
			return false;
			
		} 
//		catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			logger.debug("check robot interrputed! something wrong with put!");
//			return false;
//		}
	}

	

	/**
	 * Check if the content is crawled and modified
	 * @throws IOException 
	 * */
	private boolean ifCrawl() throws IOException {
		
		//check maxDoc
		// logger.info("Checking maxDoc...");
		if(!(crawler.getNumIndexed() < crawler.getMaxNumDoc())) {
			// logger.info("Exceed maxDoc, stop crawling");
			return false;
		}
			
		Instant lastChecked = null;
		
		// check if is crawled
		// logger.info("Checking if url is crawled...");
		Content doc = db.getDocApi().getDocument(normalizedUrl);
		if(doc != null) {
			// logger.info("Url is crawled...");
			isCrawled = true;
			lastChecked = doc.getLastChecked();
		}

		// logger.info("Url is not crawled...");
		
		// send head request
		// logger.info("Sending head request for: " + url);
		HttpReqRes res = new HttpReqRes().getResponse(urlInfo, "HEAD", lastChecked);
		
		int code = res.getStatusCode();
		// logger.info("code: " + code);
		
		if(code == 301 || code == 302) { // redirect
			// logger.info("Url redirect...");
			if(res.getLocation()!= null) {
				 logger.info("Adding location to queue...");
				try {
					urlQueue.put(res.getLocation());
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					logger.info("Maybe done crawling...");
				}
			}
			return false;
			
		} 
		
		if (code == 304) { // no modified
			// logger.info("Url not modified...");
			return false;
			
		} 
		
		if (code == 200) { // new or update
			
			if(isCrawled) {
				// logger.info("Url is modified...");
				isModified = true;
			}
			
			// logger.info("Unseen url...");
			
			return isValidSize(res) && isValidType(res);
		}

		// others: 400/500 error
		// logger.info("May be 400/500 error. Stop...");
		return false;
	}

    /**
     * Check doc size
     */
	public boolean isValidSize(HttpReqRes res) {
		if(res.getContentLength() > maxSize * 1024 * 1024) {
			// logger.info("Exceed max size. Stop...");
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
			// logger.info("No content type. Stop...");
			return false;
		}
		
		if(!res.getContentType().equals("text/html")
				&& !res.getContentType().equals("text/xml") 
				&& !res.getContentType().equals("application/xml")
				&& !res.getContentType().endsWith("+xml")) {
			// logger.info("Invalid content type. Stop...");
			return false;
		}
		
		// logger.info("Pass pre-crawling check...");
		return true;
	}
	
	
	/**
     * Crawl unseen/modified document 
	 * @throws IOException 
     */
	private void crawlDocument() throws IOException, NullPointerException {
		
		// logger.info("Sending req to the host url...");
		HttpReqRes res = new HttpReqRes().getResponse(urlInfo, "GET", null);
		Content doc = null;
		
		if(res.getStatusCode() == 200) {
			// logger.info("Generating doc...");
			doc = new Content(normalizedUrl, res.getContentType(), res.getContentLength(), res.getBody(), res.getLastModified());
			
			if(isCrawled) { // and modified
				// update doc
				// logger.info("Updating existing doc...");
//				logger.info("key: "+ doc.getUrl());
//				logger.info("key: "+ doc.getContent());
				if(crawler.getNumIndexed() >= crawler.getMaxNumDoc()) {
					return;
				}
				db.addDocument(doc); 
				crawler.incCount();
				crawler.addCrawledUrl(doc.getUrl());
				return;
			}
			
			// check md5 content seen - same content, different url
			// logger.info("Checking md5 of current doc...");
			ContentSeen temp = new ContentSeen(normalizedUrl, res.getBody());
			
			if (!db.getContentSeenApi().containsMD5(temp)) {
				// logger.info("Content unseen...");
				
				// add new md5
				// logger.info("Adding it to contentSeen table...");
				db.getContentSeenApi().addContentSeen(temp);
				
				// add new doc
				// logger.info("Adding doc to content table...");
//					logger.info("key: "+ doc.getUrl());
//					logger.info("value: "+ doc.getContent());
				if(db.getCorpusSize() >= crawler.getMaxNumDoc()) {
					return;
				}
				db.addDocument(doc);
				crawler.incCount();
				crawler.addCrawledUrl(doc.getUrl());
				
				// match channels
				// logger.info("Matching channels...(M2)");
				matchChannel(normalizedUrl, doc); 
				
				// extract link
				if(res.getContentType().equals("text/html")) {
					// logger.info("Doc is a html; extracting link...");
					extractUrl(res.getBody());
				}
			}else {
				// logger.info("Content seen...");
			}
	
			
			
		}
	}
	
	/**
     * Crawl local document 
	 * @throws IOException 
     */
	private void crawlLocalDocument() throws IOException, NullPointerException {
		
		// get doc from local
		// logger.info("Getting doc from content table for url: " + normalizedUrl);
		try {
			Content doc = db.getDocument(normalizedUrl);
			
			// match channels
			// logger.info("Matching channels...(M2)");
			matchChannel(url, doc);
			
			// if html, extract url
			if(doc.getType().equals("text/html")) {
				// logger.info("Doc is a html; extracting link...");
				extractUrl(doc.getContent());
			}
			
		} catch(NullPointerException e) {
			logger.debug("found db closed when crawling doc. likely crawler is done.");
		}
		
	
	}
	
	/**
	 * Extract url
	 * */
	private void extractUrl(String body) {
		// logger.info("Parsing string content to html...");
		org.jsoup.nodes.Document doc = Jsoup.parse(body);
		// logger.info(doc.toString());
		// logger.info("extracting href...");
		Elements links = doc.getElementsByAttribute("href");
		for (Element link : links) {
			// logger.info(link.toString());
			
			String url = link.attr("abs:href");
			
			if(url.isEmpty()) {
				try {
					url = new URL(new URL(this.url), link.attr("href")).toString();
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			logger.info("link found: "+ url);
			if(db.getDocApi().getDocument(url) == null) {
//				 logger.info("Unseen url; adding it to queue...");
				try {
					urlQueue.put(url);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			
			}
		}
	
	}
	
	/**
	 * Match Channel - M2
	 * */
	private void matchChannel(String url, Content doc) {
		
	}
	
	
	public String take() throws InterruptedException {
		String url = null;
			synchronized(urlQueue) {
				url = urlQueue.take();
			}
		return url;

	}

		
}

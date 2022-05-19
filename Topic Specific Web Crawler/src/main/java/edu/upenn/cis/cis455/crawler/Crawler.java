package edu.upenn.cis.cis455.crawler;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.upenn.cis.cis455.crawler.utils.RobotsTxtInfo;
import edu.upenn.cis.cis455.storage.Channel;
import edu.upenn.cis.cis455.storage.Storage;
import edu.upenn.cis.cis455.storage.StorageFactory;
import edu.upenn.cis.cis455.storage.StorageInterface;
import edu.upenn.cis.cis455.xpathengine.XPathEngineFactory;
import edu.upenn.cis.cis455.xpathengine.XPathEngineImpl;
import edu.upenn.cis.stormlite.Config;
import edu.upenn.cis.stormlite.LocalCluster;
import edu.upenn.cis.stormlite.Topology;
import edu.upenn.cis.stormlite.TopologyBuilder;
import edu.upenn.cis.stormlite.bolt.DOMParserBolt;
import edu.upenn.cis.stormlite.bolt.DocFetcherBolt;
import edu.upenn.cis.stormlite.bolt.IRichBolt;
import edu.upenn.cis.stormlite.bolt.LinkExtractorBolt;
import edu.upenn.cis.stormlite.bolt.PathMatcherBolt;
import edu.upenn.cis.stormlite.bolt.UrlFilterBolt;
import edu.upenn.cis.stormlite.spout.CrawlerQueueSpout;
import edu.upenn.cis.stormlite.spout.IRichSpout;

public class Crawler implements CrawlMaster {
	final static Logger logger = LogManager.getLogger(Crawler.class);

    static final int NUM_WORKERS = 10;
    private Storage db;
    private int maxSize = 0;
    private int maxNumDoc = -1; //maxdoc
    private volatile int numIndexed;
    private volatile boolean isDone = false;
    private volatile int numWorkingThread = 0;
    private volatile int numThreadExit = 0; 
    private ConcurrentHashMap<String, RobotsTxtInfo> seenRobotsTxt = new ConcurrentHashMap<>(10000);
    private ExecutorService executor = Executors.newFixedThreadPool(NUM_WORKERS);
    private volatile BlockingQueue<String> urlQueue = new ArrayBlockingQueue<>(10000);
    private Set<String> crawledUrl = new HashSet<String>();
    
    
    //stormlite config 
    private static final String URLQUEUE_SPOUT = "URLQUEUE_SPOUT";
    private static final String DOC_FETCHER_BOLT = "DOC_FETCHER_BOLT";
    private static final String LINK_EXTRACTOR_BOLT = "LINK_EXTRACTOR_BOLT";
    private static final String URL_FILTER_BOLT = "URL_FILTER_BOLT";
    private static final String DOM_PARSER_BOLT = "DOM_PARSER_BOLT";
    private static final String PATH_MATCHER_BOLT = "PATH_MATCHER_BOLT";
    
    private LocalCluster cluster;
    
    
    public Crawler(String startUrl, StorageInterface db, int size, int count) {
        this.db = (Storage) db;
        this.maxSize = size;
        this.maxNumDoc = count;

  
		urlQueue.add(startUrl);
		logger.debug("queue size: "+ urlQueue.size());
        System.out.println("init crawler...");
    }

    /**
     * Getters and Setters
     * */
	public StorageInterface getDb() {
		return db;
	}

	public int getSize() {
		return maxSize;
	}


	public int getMaxNumDoc() {
		return maxNumDoc;
	}


	public synchronized int getNumIndexed() {
		return numIndexed;
	}


	public ExecutorService getExecutor() {
		return executor;
	}
	


	public BlockingQueue<String> getUrlFrontier() {
		return urlQueue;
	}
	

	public ConcurrentHashMap<String, RobotsTxtInfo> getSeenRobotsTxt() {
		return seenRobotsTxt;
	}


	public RobotsTxtInfo getSeenRobotsTxt(String host) {
		synchronized(seenRobotsTxt) {
			return seenRobotsTxt.get(host);
		}
	}
	
	public void addSeenRobotsTxt(String host, RobotsTxtInfo r) {
		synchronized(seenRobotsTxt) {
			seenRobotsTxt.put(host, r);
		}
	}

	

	public Set<String> getCrawledUrl() {
		synchronized(crawledUrl) {
			return crawledUrl;
		}
	}
	
	public void addCrawledUrl(String url) {
		synchronized(crawledUrl) {
			crawledUrl.add(url);
		}
	}
	

	public LocalCluster getCluster() {
		return cluster;
	}

	public void setCluster(LocalCluster cluster) {
		this.cluster = cluster;
	}

	/**
     * Main thread
     */
    public void start() {    	
		// init xpath engine with all channels in this crawl
    	logger.info("init xpath engine...");
		XPathEngineImpl xPathEngine = new XPathEngineImpl();
		List<Channel> channels = db.getChannelApi().getAllChannels();
		List<String> xpaths = new ArrayList<String>();
		
		for(int i = 0; i < channels.size(); i++) {
			String currPath = channels.get(i).getXpath();
			xpaths.add(currPath);
		}
		
		xPathEngine.setXPaths(xpaths.toArray(new String[xpaths.size()]));
		xPathEngine.setChannels(channels.toArray(new Channel[channels.size()]));
		XPathEngineFactory.setXPathEngine(xPathEngine);
		logger.info(xpaths.size() +" xpaths added to engine");
		
 
    	cluster = new LocalCluster();
    	
        // Stormlite configs
        Config config = new Config();
        
        IRichSpout crawlerQueueSpout = new CrawlerQueueSpout();
        IRichBolt docFetcherBolt = new DocFetcherBolt();
        IRichBolt linkExtractorBolt = new LinkExtractorBolt();
        IRichBolt urlFilterBolt = new UrlFilterBolt();
        IRichBolt domParserBolt = new DOMParserBolt();
        IRichBolt  pathMatcherBolt = new PathMatcherBolt();
        
        TopologyBuilder builder = new TopologyBuilder();
        builder.setSpout(URLQUEUE_SPOUT, crawlerQueueSpout, 1);
        
        builder.setBolt(DOC_FETCHER_BOLT, docFetcherBolt, 4).shuffleGrouping(URLQUEUE_SPOUT);
        builder.setBolt(LINK_EXTRACTOR_BOLT, linkExtractorBolt, 4).shuffleGrouping(DOC_FETCHER_BOLT);
        builder.setBolt(URL_FILTER_BOLT, urlFilterBolt, 4).shuffleGrouping(LINK_EXTRACTOR_BOLT);
        builder.setBolt(DOM_PARSER_BOLT, domParserBolt, 4).shuffleGrouping(DOC_FETCHER_BOLT);
        builder.setBolt(PATH_MATCHER_BOLT, pathMatcherBolt,1).shuffleGrouping(DOM_PARSER_BOLT);
        

		System.out.println("submitting cluster");
        cluster.submitTopology("crawler", config, builder.createTopology());
        System.out.println("submitted!");
    }

    /**
     * We've indexed another document
     */
    @Override
    public synchronized void incCount() {
    	
    	numIndexed++;
    }
    
    
    /**
     * Workers can poll this to see if they should exit, ie the crawl is done
     */
    @Override
    public boolean isDone() { 
    	return ((getNumIndexed() >= getMaxNumDoc()  || urlQueue.isEmpty()) && getNumWorkingThread() <= 0);
    }

    /**
     * Workers should notify when they are processing an URL
     */
    @Override
    public synchronized void setWorking(boolean working) {
    
    	if(working) {
    		numWorkingThread++;
    	} else {
    		numWorkingThread--;
    	}
 
    }
    
    /**
     * Get number of working thread
     */
    public synchronized int getNumWorkingThread() {
    	return this.numWorkingThread;
 
    }

    /**
     * Workers should call this when they exit, so the master knows when it can shut
     * down
     */
    @Override
    public void notifyThreadExited() { //TODO
    	numThreadExit++;
    }

    

    /**
     * Main program: init database, start crawler, wait for it to notify that it is
     * done, then close.
     */
    public static void main(String args[]) {
   
    	logger.info("Parsing crawler args...");
    	if (args.length < 3 || args.length > 5) {
            System.out.println("Usage: Crawler {start URL} {database environment path} {max doc size in MB} {number of files to index}");
            System.exit(1);
        }
    	logger.info(args[1]);
    	logger.info("Crawler starting");
        String startUrl = args[0];
        String envPath = args[1];
        Integer size = Integer.valueOf(args[2]);
        Integer count = args.length == 4 ? Integer.valueOf(args[3]) : 100;
         
        StorageInterface db = StorageFactory.getDatabaseInstance(envPath); 
        Crawler crawler = new Crawler(startUrl, db, size, count); 
        CrawlerFactory.setCrawler(crawler);
        crawler.start();

        logger.debug("Starting crawl of " + count + " documents, starting at " + startUrl);

        
        while(!crawler.isDone()) {
            try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
        
        crawler.getCluster().killTopology("crawler");
        crawler.getCluster().shutdown();
        crawler.setCluster(null);
        
        
        logger.debug("Num of docs indexed: " + crawler.getNumIndexed());
        logger.debug("Num of docs in db: " + db.getCorpusSize());
        
//        try {
//        	logger.debug("Num docs in channel: " + ((Storage)db).getChannelApi().getChannel("nyt").getDocUrls().size());
//        } catch(Exception e) {
//        	logger.debug("failed to print channel");
//        }
        
        
        logger.info("closing db...");
        db.close();
        for(String url : crawler.getCrawledUrl()) {
        	logger.info(url);
        }
        
        logger.info("Done crawling!");
        
    }

}

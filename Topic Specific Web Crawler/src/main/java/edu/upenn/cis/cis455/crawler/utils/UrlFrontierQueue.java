package edu.upenn.cis.cis455.crawler.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class UrlFrontierQueue {
	final static Logger logger = LogManager.getLogger(UrlFrontierQueue.class);
	
    private BlockingQueue<String> frontierUrl;
    private List<String> crawledUrl = new ArrayList<String>();
    
    public UrlFrontierQueue(int queueSize) {
    	this.frontierUrl = new ArrayBlockingQueue<>(queueSize);
    	
    }
    
    /**
     * Add url to queue
     */
	public void put(String url) { //TODO: patch: no sync
		logger.info("Entering put: " + url);
		URLInfo info = new URLInfo(url);
		
		if(info.getHostName()!=null) {
	        try {
	        	// logger.info("Adding "+ url +" to list");
	        	frontierUrl.put(url);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				logger.debug("put interrupted");
			}
		}
	}
	
    /**
     * Poll url from queue
     */
	public String take() {
		
		try {
			String url = frontierUrl.take();
			return url;
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			logger.debug("Take interrupted");
		}
		
		return null;
	}
    
    /**
     * Check if queue is empty
     */
	public synchronized boolean isEmpty() {
		return frontierUrl.isEmpty();
	}

    /**
     * Add crawled urls
     */
	public synchronized List<String> getCrawledUrl() {
		return crawledUrl;
	}
    
}

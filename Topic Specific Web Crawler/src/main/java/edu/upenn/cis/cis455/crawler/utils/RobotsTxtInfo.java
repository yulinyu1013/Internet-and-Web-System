package edu.upenn.cis.cis455.crawler.utils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Helper class to store robots.txt
 */
public class RobotsTxtInfo {
	final static Logger logger = LogManager.getLogger(RobotsTxtInfo.class);
	
	private HashMap<String,ArrayList<String>> disallows = new HashMap<String,ArrayList<String>>();
	private HashMap<String,Integer> crawlDelays = new HashMap<String,Integer>();
	private ArrayList<String> userAgents = new ArrayList<String>();
	private Instant lastChecked = Instant.EPOCH;
	
	public Instant getLastChecked() {
		return lastChecked;
	}

	public synchronized void setLastChecked(Instant lastChecked) {
		this.lastChecked = lastChecked;
	}
	
    /**
     * Check delay
     */
	public synchronized boolean needDelay(String userAgent) {
		// logger.info("Last Checked: "+ lastChecked.toString());
//		 logger.info("Delay: " + crawlDelays.getOrDefault(userAgent, 0));
		Instant now = Instant.now();
//		 logger.info("Now: "+ now);
		Instant pastTime = lastChecked.plusSeconds(crawlDelays.getOrDefault(userAgent, 0));
		 logger.info("Need to pass: "+ pastTime.toString());
		if(pastTime.isBefore(now)) {
			lastChecked = now;
			return false;
		}
		
		logger.info("Delay needed");
		return true;
	}

    /**
     * Add disallowed urls to corresponding user agent
     */
	public void addDisallow(String key, String value){
		if(disallows.containsKey(key)){
			ArrayList<String> temp = disallows.get(key);
			temp.add(value);
			disallows.put(key, temp);
		}
		else{
			ArrayList<String> temp = new ArrayList<String>();
			temp.add(value);
			disallows.put(key, temp);
		}
	}
	
	
    /**
     * Getters and Setters
     */
	public void addCrawlDelay(String key, Integer value){
		crawlDelays.put(key, value);
	}
	
	public void addUserAgent(String userAgent){
		this.userAgents.add(userAgent);
	}
	
	public ArrayList<String> getDisallows(String key){
		return disallows.get(key);
	}
	
	
	public int getCrawlDelay(String key){
		return crawlDelays.getOrDefault(key, 0);
	}
	
	public boolean containsUserAgent(String userAgent) {
		return userAgents.contains(userAgent);
	}
	
	public boolean containsCrawlDelay(String userAgent) {
		return crawlDelays.containsKey(userAgent);
	}
	

	
	
}

package edu.upenn.cis.cis455.m1.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.m2.interfaces.RegisteredItem;

/**
 * A Helper Class for route matching and params setting
 * **/
public class HttpRequestWrapper {
	
	private HttpRequestWrapper() {};
	final static Logger logger = LogManager.getLogger(HttpRequestWrapper.class);
    
	public static void changeMatch(HttpRequest req, RegisteredItem item) {
		logger.info("setting req matching item");
        req.setMatchingItem(item); 
    }

}

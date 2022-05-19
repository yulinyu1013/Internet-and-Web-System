package edu.upenn.cis.cis455.m2.interfaces;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A class that store a registered route/filter
 */
public class RegisteredItem {
	final static Logger logger = LogManager.getLogger(RegisteredItem.class);
	
	private String method;
	private String path;
	private Object item;
	private boolean isRoute;
	

	public RegisteredItem(String method, String path, Object handler, boolean isRoute) {
		this.method = method;
		this.path = path;
		this.item = handler;
		this.isRoute = isRoute;
	}
	
	/**
	 * getter and setters
	 */
	public String getMethod() {
		return method;
	}

	public String getPath() {
		return path;
	}

	public Object getItem() {
		return item;
	}
	
	public boolean isRoute() {
		return isRoute;
	}
	
    @Override
    public String toString() {
        return method + " " + path + " " + item;
    }
	
    
	/**
	 * Match request with current filter/route and return boolean value
	 */
	public boolean isMatched(String method, String path) { 
		logger.info("Matching with "+this.method+ " "+this.path);
		// 1. check method
		if(!method.equals(this.method)) {	
			return false; 
		}
		
		// 2. special case: if it is a filter for all paths
		if((method.equals("BEFORE") || method.equals("AFTER")) && (this.path.equals("/*allpaths"))) {
			return true;
		}
		
		// 3. check path
		
		// 3.1 Check trailing slash in a path; /path != /path/
		if (!this.path.endsWith("*")) {
			if(((path.endsWith("/") && !this.path.endsWith("/"))
                || (this.path.endsWith("/") && !path.endsWith("/")))) {
				return false; 
			} 
        }
		
		// 3.2 check path params
		 List<String> thisPath= new ArrayList<String>(Arrays.asList(this.path.split("/")));
		 List<String> reqPath= new ArrayList<String>(Arrays.asList(path.split("/")));
		 
		 if(thisPath.size() != reqPath.size()) {
			 
			 logger.info("not equal size; checking wild card...");
			 if (this.path.endsWith("*")) { // check wild card
				 
				if((reqPath.size() + 1 == thisPath.size() ) && path.endsWith("/")) {
					logger.info("handling trailing slash...");
					reqPath.add(""); // deal with "/hello/" to match "/hello/*"
					logger.info("finish handling trailing slash...");
				}
				
				if(reqPath.size() >= thisPath.size()) {
					logger.info("Matching parts...");
					return isMatchedParts(thisPath, reqPath);
				}
				
			 }
			 
			 return false;

		 } else {
			 logger.info("equal size");
			 return isMatchedParts(thisPath, reqPath);
		 }
		
	}
	
	/**
	 * Helper method to match parts of two paths
	 */
	private boolean isMatchedParts(List<String> p1, List<String> p2) {
		logger.info("Matching parts...");
		for (int i = 0; i < p1.size(); i++) {
			 
			 String thisPart = p1.get(i);
			 String reqPart = p2.get(i);

			 if ((!thisPart.startsWith(":")) && !thisPart.equals(reqPart) && !thisPart.equals("*")) {
                   return false;
            }
		 }
		 
		 return true;
	}




	
}

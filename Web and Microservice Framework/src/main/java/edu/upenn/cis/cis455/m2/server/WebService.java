/**
 * CIS 455/555 route-based HTTP framework
 * 
 * Z. Ives, 8/2017
 * 
 * Portions excerpted from or inspired by Spark Framework, 
 * 
 *                 http://sparkjava.com,
 * 
 * with license notice included below.
 */

/*
 * Copyright 2011- Per Wendel
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.upenn.cis.cis455.m2.server;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.m2.interfaces.Route;
import edu.upenn.cis.cis455.m2.interfaces.Filter;
import edu.upenn.cis.cis455.m2.interfaces.HttpRoutes;
import edu.upenn.cis.cis455.m2.interfaces.HttpSession;

public class WebService extends edu.upenn.cis.cis455.m1.server.WebService {
    final static Logger logger = LogManager.getLogger(WebService.class);
    
    private static WebService webService = null;
    public HttpRoutes routes = new HttpRoutes();
	public Map<String, HttpSession> sessions = new HashMap<>();

	/**
     * Get Instance Object
     */
    public static WebService getInstance() {
        if (webService == null) webService = new WebService();
        return webService; 
    }

    ///////////////////////////////////////////////////
    // For more advanced capabilities
    ///////////////////////////////////////////////////
    
    
	/**
     * Handle an HTTP POST request to the path
     */
    public void get(String path, Route route) {
    	logger.info("adding a get route...");
    	routes.addRoute("GET", path, route);
    }
    
    /**
     * Handle an HTTP POST request to the path
     */
    public void post(String path, Route route) {
    	logger.info("adding a post route...");
    	routes.addRoute("POST", path, route);
    }

    /**
     * Handle an HTTP PUT request to the path
     */
    public void put(String path, Route route) {
    	logger.info("adding a put route...");
    	routes.addRoute("PUT", path, route);
    }

    /**
     * Handle an HTTP DELETE request to the path
     */
    public void delete(String path, Route route) {
    	logger.info("adding a delete route...");
    	routes.addRoute("DELETE", path, route);
    }

    /**
     * Handle an HTTP HEAD request to the path
     */
    public void head(String path, Route route) {
    	logger.info("adding a head route...");
    	routes.addRoute("HEAD", path, route);
    }

    /**
     * Handle an HTTP OPTIONS request to the path
     */
    public void options(String path, Route route) {
    	logger.info("adding an options route...");
    	routes.addRoute("OPTIONS", path, route);
    }
 
    ///////////////////////////////////////////////////
    // HTTP request filtering
    ///////////////////////////////////////////////////

    /**
     * Add filters that get called before a request
     */
    public void before(Filter filter) { // apply to all registered routes
    	logger.info("adding a before filter for all...");
    	routes.addFilter("BEFORE", "/*allpaths", filter);
    }

    /**
     * Add filters that get called after a request
     */
    public void after(Filter filter) { 
    	logger.info("adding a after filter for all...");
    	routes.addFilter("AFTER", "/*allpaths", filter);
    }

    /**
     * Add filters that get called before a request
     */
    public void before(String path, Filter filter) {
    	logger.info("adding a before filter ...");
    	routes.addFilter("BEFORE", path, filter);
    }

    /**
     * Add filters that get called after a request
     */
    public void after(String path, Filter filter) {
    	logger.info("adding an after filter...");
    	routes.addFilter("AFTER", path, filter);
    }

}

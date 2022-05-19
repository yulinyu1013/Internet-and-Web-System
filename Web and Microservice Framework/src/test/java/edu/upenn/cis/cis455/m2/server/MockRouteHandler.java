package edu.upenn.cis.cis455.m2.server;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.m1.server.HttpRequest;
import edu.upenn.cis.cis455.m1.server.HttpRequestWrapper;
import edu.upenn.cis.cis455.m1.server.HttpResponse;
import edu.upenn.cis.cis455.m2.interfaces.Filter;
import edu.upenn.cis.cis455.m2.interfaces.HttpRoutes;
import edu.upenn.cis.cis455.m2.interfaces.RegisteredItem;
import edu.upenn.cis.cis455.m2.interfaces.Route;

public class MockRouteHandler {
	final static Logger logger = LogManager.getLogger(MockRouteHandler.class);
	
	public static void handleBefore(HttpRequest req, HttpResponse res, HttpRoutes routes) throws Exception {
		logger.info("Getting before filters...");
		logger.info(req.pathInfo());
		List<RegisteredItem>  beforeFilters = routes.getBeforeFilters("BEFORE", req.pathInfo());
		logger.info("# of Before filters: " + beforeFilters.size());
		for(RegisteredItem filter : beforeFilters) {
			HttpRequestWrapper.changeMatch(req, filter);
			Filter f = ((Filter) filter.getItem());
			f.handle(req, res);
		}
		
	}
	
	public static boolean handleRoute(HttpRequest req, HttpResponse res, HttpRoutes routes) throws Exception {
		RegisteredItem route = routes.getMatchingRoute(req.requestMethod(), req.pathInfo());
		logger.info("Getting routes...");
		if(route == null) {
			logger.info("Route not found");
			return false;
		}
		
		logger.info("Route found: "+ route.getMethod() + " "+ route.getPath());
		logger.info("Updating req query params...");
		HttpRequestWrapper.changeMatch(req, route);
		logger.info("handling route...");
		Route r = ((Route) route.getItem());
		logger.info("getting returning object...");
		Object body = r.handle(req, res);
		logger.info("object body: " + body);
		logger.info("checking returning object...");
		if((res.status() != 301) && (res.status() != 302) && body != null && !body.equals("")) {
			
			res.body(body.toString());
			logger.info("response body: " + res.body());
			
		}
		 
		return true;
	}
	
	
	
	public static void handleAfter(HttpRequest req, HttpResponse res, HttpRoutes routes) throws Exception {
		logger.info("Getting after filters...");
		List<RegisteredItem>  afterFilters = routes.getAfterFilters("AFTER", req.pathInfo());
		logger.info("# of After filters: " + afterFilters.size());
		for(RegisteredItem filter : afterFilters) {
			HttpRequestWrapper.changeMatch(req, filter);
			Filter f = ((Filter) filter.getItem());
			f.handle(req, res);
		}
		
	}
}

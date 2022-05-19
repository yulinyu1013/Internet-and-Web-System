package edu.upenn.cis.cis455.m2.interfaces;

import java.util.ArrayList;
import java.util.List;


/**
 * Holds the routes and filters and performs matching from HTTP requests to routes/filters.
 *
 */
public class HttpRoutes {
	
	private List<RegisteredItem> routes = new ArrayList<>();
	
	/**
	 * Register route
	 * 
	 * **/
	public void addRoute(String method, String path, Route route) {
		RegisteredItem entry = new RegisteredItem(method, path, route, true);
		routes.add(entry);
	} 
	
	
	/**
	 * Register filter
	 * 
	 * **/
	public void addFilter(String method, String path, Filter filter) {
		RegisteredItem entry = new RegisteredItem(method, path, filter, false);
		routes.add(entry);
	}
	
	/**
	 * @return Matching Route
	 * 
	 * **/
	public RegisteredItem getMatchingRoute(String method, String path){
		
		for(RegisteredItem r : routes) {
			if(r.isMatched(method, path) && r.isRoute()) {
				return r;
			}
		}
		
		return null;
	}
	
	/**
	 * @return All matching before filters
	 * 
	 * **/
	public List<RegisteredItem> getBeforeFilters(String method, String path){
		List<RegisteredItem> beforeFilters= new ArrayList<>();
			
		for(RegisteredItem r : routes) {
			if(r.isMatched(method, path) && r.getMethod().equals("BEFORE")) {
				beforeFilters.add(r);
			}
		}
		
		return beforeFilters;
	}
	
	/**
	 * @return All matching after filters
	 * 
	 * **/
	public List<RegisteredItem> getAfterFilters(String method, String path){
		List<RegisteredItem> afterFilters= new ArrayList<>();
		
		for(RegisteredItem r : routes) {
			if(r.isMatched(method, path) && r.getMethod().equals("AFTER")) {
				afterFilters.add(r);
			}
		}
		
		return afterFilters;
	}
	
}

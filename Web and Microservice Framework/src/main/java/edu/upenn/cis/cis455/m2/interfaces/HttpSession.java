package edu.upenn.cis.cis455.m2.interfaces;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * HttpSession
 */
public class HttpSession extends Session {
	
	private UUID id;
	private long creationTime;
	private long lastAccessedTime;
	private boolean valid;
	private boolean isNew;


	private int maxInactiveInterval = 10 * 60; // 10 min
	private Map<String, Object> attributes = new HashMap<String, Object>();

	
	public HttpSession() {
		id = UUID.randomUUID();
		Date date = new Date();
		creationTime = date.getTime();
		lastAccessedTime = creationTime;
		valid = true; 
		isNew = true;
	}
	

	@Override
	public String id() {
		return id.toString();
	}

	@Override
	public long creationTime() { 
		return creationTime;
		
	}

	@Override
	public long lastAccessedTime() {
		return lastAccessedTime;
	}

	@Override
	public void invalidate() {
		valid = false;
		
	}
 
	@Override
	public int maxInactiveInterval() {
		return maxInactiveInterval;
	}

	@Override
	public void maxInactiveInterval(int interval) { // in seconds
		maxInactiveInterval =  interval;
		
	}

	@Override
	public void access() {
		long now = new Date().getTime();
		if (now - lastAccessedTime > maxInactiveInterval * 1000) {
			invalidate();
			return;
		}
		
		lastAccessedTime = now;
	}

	@Override
	public void attribute(String name, Object value) { 
		attributes.put(name, value);
	}

	@Override
	public Object attribute(String name) {
		return attributes.containsKey(name) ? attributes.get(name) : null;
	}

	@Override
	public Set<String> attributes() {
		return attributes.keySet();
	}

	@Override
	public void removeAttribute(String name) {
		attributes.remove(name);
		
	}

	/**
	 * Check status of a session
	 */
	public boolean isValid() {
		return valid;
	}
	
	public boolean isNew() {
		return isNew;
	}
	
	public void setNew(boolean isNew) {
		this.isNew = isNew;
	}

}

package edu.upenn.cis.cis455.m2.interfaces;


/**
 * A cookie class
 */
public class Cookie {
	private String name;
	private String value;
	private String path;
	private int maxAge;
	private boolean secured;
	private boolean httpOnly;
	
	public Cookie(String name, String value) {
		this.name = name;
		this.value = value;
	}

	/**
	 * A getter and setters
	 */
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public int getMaxAge() {
		return maxAge;
	}

	public void setMaxAge(int maxAge) {
		this.maxAge = maxAge;
	}

	public boolean isSecured() {
		return secured;
	}

	public void setSecured(boolean secured) {
		this.secured = secured;
	}

	public boolean isHttpOnly() {
		return httpOnly;
	}

	public void setHttpOnly(boolean httpOnly) {
		this.httpOnly = httpOnly;
	}

	@Override
	public String toString() {
		
		StringBuilder sb = new StringBuilder();
		sb.append("Set-Cookie: " + getName()+"=" + getValue());
		
		if(!getPath().equals("")) {
			sb.append("; ");
			sb.append("Path="+getPath());
		}
		
		if(getMaxAge()!= -1) {
			sb.append("; ");
			sb.append("Max-Age="+getMaxAge());
		}
		
		if(isSecured()) {
			sb.append("; ");
			sb.append("Secured");
		}
		
		if(isHttpOnly()) {
			sb.append("; ");
			sb.append("HttpOnly");
		}
		
		
		return sb.toString();
	}

}

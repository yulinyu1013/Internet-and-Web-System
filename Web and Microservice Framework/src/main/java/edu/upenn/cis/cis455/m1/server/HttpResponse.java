package edu.upenn.cis.cis455.m1.server;



import java.util.HashMap;
import java.util.Map;
import edu.upenn.cis.cis455.m2.interfaces.Cookie;
import edu.upenn.cis.cis455.m2.interfaces.Response;

public class HttpResponse extends Response {
	
//  protected int statusCode = 200;
//  protected String contentType = null; // e.g., "text/plain";
//  protected byte[] body;
	protected StringBuilder strHeaders;
	protected Map<String, String> headers = new HashMap<>();
	protected Map<Map<String, String>, Cookie> cookies = new HashMap<>();


	@Override
	public String getHeaders() {
		strHeaders = new StringBuilder();
		for(String header : headers.keySet()) {
			strHeaders.append(header+": " + headers.get(header)+"\r\n");
		}
		strHeaders.append("Content-Type: " + type() + "\r\n");
		strHeaders.append("Content-Length: " + (bodyRaw() != null ? bodyRaw().length:0) + "\r\n");
		for(Cookie cookie : cookies.values()) {
			strHeaders.append(cookie.toString()+"\r\n");
			
		}
		strHeaders.append("Connection: close\r\n");

		return strHeaders.toString();
	}
	
	 
    /**
     * Add a header key/value
     */
	
	@Override
	public void header(String header, String value) {
		headers.put(header, value);
		
	}
	
	
    /**
     * Trigger an HTTP redirect to a new location
     */
	@Override
	public void redirect(String location) { //TODO: is location a url?
		redirect(location, 302);
		
	}
	
    /**
     * Trigger a redirect with a specific HTTP 3xx status code
     */
	@Override
	public void redirect(String location, int httpStatusCode) {
		if(httpStatusCode < 300 || httpStatusCode > 307) {
			throw new IllegalArgumentException("Wrong code for redirect");
		}
		status(httpStatusCode);
		header("Location", location);
		
	}
	
	
	@Override
	public void cookie(String name, String value) {
		cookie(name, value, -1, false);
		
	}

	@Override
	public void cookie(String name, String value, int maxAge) {
		cookie(name, value, maxAge, false);
		
	}

	@Override
	public void cookie(String name, String value, int maxAge, boolean secured) {
		cookie(name, value, maxAge, secured, false);
		
	}

	@Override
	public void cookie(String name, String value, int maxAge, boolean secured, boolean httpOnly) {
		cookie("", name, value, maxAge, secured, httpOnly);
		
	}

	@Override
	public void cookie(String path, String name, String value) {
		cookie(path, name, value, -1);
		
	}

	@Override
	public void cookie(String path, String name, String value, int maxAge) {
		cookie(path, name, value, maxAge, false);
		
	}

	@Override
	public void cookie(String path, String name, String value, int maxAge, boolean secured) {
		cookie(path, name, value, maxAge, secured, false);
		
	}

	@Override
	public void cookie(String path, String name, String value, int maxAge, boolean secured, boolean httpOnly) {
		String cleanedName = name.replaceAll(" ", "%20"); // handle space
		String cleanedValue = value.replaceAll(" ", "%20");
		Cookie cookie = new Cookie(cleanedName, cleanedValue);
		cookie.setPath(path);
		cookie.setMaxAge(maxAge);
		cookie.setSecured(secured);
		cookie.setHttpOnly(httpOnly);
		Map<String, String> key = new HashMap<>();
		key.put(path, name);
		cookies.put(key, cookie); 
	}

	@Override
	public void removeCookie(String name) {
		removeCookie(null, name);
		
	}

	@Override
	public void removeCookie(String path, String name) {
		String cleanedName = name.replaceAll(" ", "%20");
		Map<String, String> key = new HashMap<>();
		key.put(path, cleanedName);
		cookies.remove(key);
	}
	

}

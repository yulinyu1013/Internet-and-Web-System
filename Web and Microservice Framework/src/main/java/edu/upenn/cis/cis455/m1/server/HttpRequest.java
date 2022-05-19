package edu.upenn.cis.cis455.m1.server;


import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.m2.interfaces.HttpSession;
import edu.upenn.cis.cis455.m2.interfaces.RegisteredItem; 
import edu.upenn.cis.cis455.m2.interfaces.Request;
import edu.upenn.cis.cis455.m2.interfaces.Session;
import edu.upenn.cis.cis455.m2.server.WebService;

public class HttpRequest extends Request {
	final static Logger logger = LogManager.getLogger(HttpRequest.class);
	
	private Map<String, String> pre = new HashMap<String, String>(); // first line
	private Map<String, List<String>> queryParams;
	private Map<String, String> params;
	private Map<String, String> cookies= new HashMap<String, String>();
	private Map<String, String> requestHeaders = new HashMap<String, String>();
	private Map<String, Object> attributes = new HashMap<String, Object>();
	private String body;
	private Socket socket;
	private HttpSession session = null;
	private String[] splat;

	public HttpRequest() {};
	
	/**
	 * Constructor
	 * **/
	public HttpRequest(Map<String, String> pre,
			Map<String, List<String>> queryParams, 
			Map<String, String> requestHeaders,
			String body,
			Socket socket) {
		this.pre = pre;
		this.queryParams = queryParams;
		this.requestHeaders = requestHeaders;
		this.body = body;
		this.socket = socket;
		

	}
	
	/**
	 * Method for internal use by filters and routes for matching params
	 * **/
	protected void setMatchingItem(RegisteredItem item) {
		logger.info("Setting params and splat...");
		List<String> thisPathParts = Arrays.asList(item.getPath().split("/"));
		List<String> reqPathParts = Arrays.asList(pathInfo().split("/"));
		int thisPathLength = thisPathParts.size();
		int reqPathLength = reqPathParts.size();
		
		logger.info("This path size: " + thisPathLength);
		logger.info("Req path size: " + reqPathLength);
		
		//getting params
		params = new HashMap<String, String>();
		for (int i = 0; i < Math.min(thisPathLength, reqPathLength); i++) {
			 String thisPart = thisPathParts.get(i);
			 
            if (thisPart.startsWith(":")) {
            	String reqPart = reqPathParts.get(i); 
            	logger.info(thisPart + ": "+ reqPart);
                params.put(thisPart, reqPart);
            }
        }
		
		params = Collections.unmodifiableMap(params);
		logger.info("Finish setting query params...");
		
		//getting splat
		List<String> splat = new ArrayList<String>();
		for (int i = 0; i < Math.min(thisPathLength, reqPathLength); i++) { 

			String thisPart = thisPathParts.get(i);
		
            if (thisPart.equals("*")) {
            	logger.info(i + " Splat found...");
            	String reqPart = reqPathParts.get(i);
                StringBuilder sb = new StringBuilder();
                sb.append(reqPart);
                
                logger.info("splat: " + sb.toString());
                logger.info("Parsing splat value...");
                if (thisPathLength != reqPathLength) { //handle the last part with "*"
                	if(thisPathLength == i + 1 ) {
                		for(int j = i + 1; j < reqPathLength; j++) {
                			sb.append("/"); // since there may be multiple parts
                			sb.append(reqPathParts.get(j));
                		}	
                	}
                }
                
                splat.add(sb.toString());
                
            }
            
            logger.info(i + " Splat not found...");
        }
		
		this.splat = splat.toArray(new String[splat.size()]);
		
		logger.info("Finish setting splat...");
		
	}
	
	@Override
	public String requestMethod() {
		return pre.get("method");
	}

	@Override
	public String pathInfo() { //e.g. /genapp/customers
		String temp = pre.get("uri");
		if (temp.startsWith("http://")) { //handle absolute url
			String temp2 = temp.split("//")[1];
			return temp2.substring(temp2.indexOf("/"), (temp2.indexOf("?") == -1?temp2.length():temp2.indexOf("?")));
		}
		return pre.get("uri");
	}

	
	@Override
	public String protocol() {
		return pre.get("protocolVersion");
	}
	
	
	@Override
	public String host() {
		return headers("host");
	}


	@Override
	public String userAgent() {
		return headers("user-agent");
	}

	@Override
	public int port() {
		return socket.getPort();
	}

	@Override
	public String uri() { //e.g. http://0.0.0.0:8000/genapp/customers
		String temp = pre.get("uri"); //already decode %20
		if (temp.startsWith("http://")) { //handle absolute url
			return temp.substring(0, (temp.indexOf("?")== -1 ? temp.length() : temp.indexOf("?")));
		}
		
		String uri = "http://" + host() + pathInfo();
//		try {
//			return URLEncoder.encode(uri, "UTF-8");
//		} catch (UnsupportedEncodingException e) {
//			return uri;
//		}
		return uri;
	}
	
	@Override
	public String url() {//e.g url: http://0.0.0.0:8000/genapp/customers?name=Joe%20Bloggs
		String temp = pre.get("uri");
		if (temp.startsWith("http://")) { //handle absolute url
			return temp;
		}
		
		String url = "http://" + host() + pathInfo() + (queryString() != "" ? "?" + pre.get("queryString") : "");
//		try {
//			return URLEncoder.encode(url, "UTF-8");
//		} catch (UnsupportedEncodingException e) {
//			return url;
//		}
		return url;
	}


	@Override
	public String contentType() {
		return headers("content-type");
	}


	@Override
	public String ip() { 
		return socket.getRemoteSocketAddress().toString();
	}


	@Override
	public String body() {
		return body; 
	}


	@Override
	public int contentLength() {
		return Integer.parseInt(headers("content-length")!= null ? headers("content-length"): "0");
	}


	@Override
	public String headers(String name) {
		logger.info("Checking header: " + name);
		if (requestHeaders.containsKey(name)) {
			logger.info(name + "found");
			return requestHeaders.get(name);
		} 
		logger.info(name + " not found...");
		return null;
	}


	@Override
	public Set<String> headers() {
		return requestHeaders.keySet();
	}

	@Override
	public Session session() {
		if(session != null) {
			logger.info("Returning the same session in the middle of the request...");
			return session;
		} else {
			logger.info("Checking existing sessions...");
			if(cookies().containsKey("JSESSIONID")) {
				logger.info("JSessionid found from request...");
				String id = cookies().get("JSESSIONID");
				if(id != null) {
					logger.info("Corresponding session found from server...");
					HttpSession s = WebService.getInstance().sessions.get(id);
					 if(s != null) {
						 s.access();
						 if(s.isValid()) {
							 logger.info("A valid existing session found...");
							 s.setNew(false);
							 session = s;
							 return session; // return old session
						 }else {
							 logger.info("An invalid existing session found...");
							 logger.info("Removing expired session...");
							 WebService.getInstance().sessions.remove(id);
						 }
					 }
				}
			}
			logger.info("Creating a new session...");
			session = new HttpSession();
			WebService.getInstance().sessions.put(session.id(), session);
			return session;
		}
	}

	@Override
	public Session session(boolean create) {
		
		if(create) {
			session = new HttpSession();
			WebService.getInstance().sessions.put(session.id(), session);
			return session;
		}
		
		if(session != null) {
			logger.info("Returning the same session in the middle of the request...");
			return session;
		} else {
			logger.info("Checking existing sessions...");
			logger.info("Checking JSESSIONID...");
			if(cookies().containsKey("JSESSIONID")) {
				logger.info("JSessionid found from request...");
				String id = cookies().get("JSESSIONID");
				if(id != null) {
					logger.info("Corresponding session found from server...");
					HttpSession s = WebService.getInstance().sessions.get(id);
					 if(s != null) {
						 s.access();
						 if(s.isValid()) {
							 logger.info("A valid existing session found...");
							 s.setNew(false);
							 session = s;
							 return session; // return old session
						 }else {
							 logger.info("An invalid existing session found...");
							 logger.info("Removing expired session...");
							 WebService.getInstance().sessions.remove(id);
						 }
					 }
				}
			}
			
			return null; // which should be null
		}
	}

	@Override
	public Map<String, String> params() {
		return params;
	}

	@Override
	public String queryParams(String param) {
		if(queryParams.containsKey(param)) {
			List<String> values = queryParamsValues(param);
			if(values.size()==1) {
				return values.get(0);
			}
		}
		return null;
	}

	@Override
	public List<String> queryParamsValues(String param) {
		return queryParams.containsKey(param) ? queryParams.get(param) : null;
	}

	@Override
	public Set<String> queryParams() {
		return queryParams.keySet();
	}

	@Override
	public String queryString() {
		return pre.get("queryString");
	}

	@Override
	public void attribute(String attrib, Object val) {
		attributes.put(attrib, val);
		logger.info("New Attribute added: " + attribute(attrib).toString());
	}

	@Override
	public Object attribute(String attrib) {
		return attributes.containsKey(attrib) ? attributes.get(attrib) : null;
	}

	@Override
	public Set<String> attributes() {
		return attributes.keySet();
	}

	@Override
	public Map<String, String> cookies() {
		logger.info("Getting cookies...");
		cookies = new HashMap<>();
		if(headers("cookie")!= null) {
			logger.info("Cookies found...");
			String[] browserCookies = headers("cookie").split("; ");
			for(String cookie : browserCookies) {
				String[] pair = cookie.split("=");
				cookies.put(pair[0], pair[1]);
			}
		}
		return cookies;
	}

	@Override
	public String[] splat() {
		return splat;
	}

}

package edu.upenn.cis.cis455.m1.handling;


import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.exceptions.HaltException;
import edu.upenn.cis.cis455.m1.server.HttpRequest;
import edu.upenn.cis.cis455.m1.server.HttpResponse;
import edu.upenn.cis.cis455.m2.interfaces.HttpSession;

/**
 * Handles marshaling between HTTP Requests and Responses
 */
public class HttpIoHandler {
    final static Logger logger = LogManager.getLogger(HttpIoHandler.class);

    
    /**
     * Receive incoming data. Parse data and return a request object.
     * Throw error if it is a bad request.
     */
    
    public static HttpRequest parseRequest(Socket socket) {
    	logger.info("Parsing request...");
    	InputStream in;
    	HttpRequest req = null;
    		
		try {
			// init setup
			in = socket.getInputStream();
			Map<String, String> pre = new HashMap<String, String>();
	    	Map<String, String> headers = new HashMap<String, String>();
	    	Map<String, List<String>> queryParams = new HashMap<String, List<String>>();
	    	String body = HttpRequestParser.parseRequest("", in, pre, headers, queryParams);
	    	req = new HttpRequest(pre, queryParams, headers, body, socket); 
	    	logger.info("queryparams: "+ queryParams.size());
	    	logger.info("req body: "+ req.body());
	    	logger.info("\r\n"+req.requestMethod() + " "+ req.pathInfo() +" " +req.protocol() );
	    	return req;
		} catch (IOException e) {
			logger.error("Cannot get input stream from socket.");
			sendException(socket, null, new HaltException(500, "error from parseRequest"), null);
		} catch (HaltException e1) {
			logger.error("Bad/Invalid Request Found");
			sendException(socket, null, e1, req);
		}
		
		return null;
    }
    
    /**
     * Sends an exception back, in the form of an HTTP response code and message.
     * Returns true if we are supposed to keep the connection open (for persistent
     * connections).
     *
     */
    public static boolean sendException(Socket socket, HttpResponse res, HaltException except, HttpRequest req) {
    	logger.info("sending exception...");
    	StringBuilder headers = new StringBuilder();
    	
    	//generate exception content
    	String firstLine = HandlerHelper.generateFirstline(except.statusCode()); 
    	String body = "<!DOCTYPE html>\r\n<html><body>\r\n" 
    			+ "<h1>" + except.statusCode() + " " +  HandlerHelper.statusMsg(except.statusCode())+ "</h1>\r\n"
    			+ "</body></html>\r\n";
    
    	headers.append("Content-Type: text/html\r\n"
    			+ "Content-Length: " + body.getBytes().length + "\r\n");
    	
        
    	if(res!=null) {
    		//update date
			res.header("Date", HandlerHelper.responseDate());
			
	        //update body type - text/html
	        if(res.type() == null) {
	        	res.type("text/html");
	        }
	        // check session
	        if(req!=null && req.session(false) != null && ((HttpSession) req.session(false)).isNew()) {
	        	res.cookie("JSESSIONID", req.session(false).id());
	        }
	    	
	    	String[] headersFromRes = res.getHeaders().split("\r\n");
	    	for(String header : headersFromRes) {
	    		if((!header.startsWith("Content-Type")) && (!header.startsWith("Content-Length"))) {
	    			headers.append(header+"\r\n");
	    		}
	    	}
    	} else {
    		headers.append("Date: "+HandlerHelper.responseDate()+"\r\n");
            if(req!=null && req.session(false) != null && ((HttpSession) req.session(false)).isNew()) {
            	headers.append("Set-Cookie: JSESSIONID="+req.session(false).id() + "\r\n");
            }
    		headers.append("Connection: close\r\n\r\n");
    	}

    	

    	logger.info("\r\n"+ firstLine + headers.toString()+body);
    	
    	try {
    		DataOutputStream ds = new DataOutputStream(socket.getOutputStream());
			ds.writeBytes(firstLine);
			ds.writeBytes(headers.toString());
			ds.writeBytes("\r\n"+body);
			ds.flush();
		} catch (IOException e) {
			logger.debug("Cannot get output stream from socket.");
			e.printStackTrace();
		} 
    	logger.info("finish sending exception...");
    	return false;
    }


    /**
     * Sends data back. Returns true if we are supposed to keep the connection open
     * (for persistent connections).
     */
    
    public static boolean sendResponse(Socket socket, HttpRequest req, HttpResponse res) {
    	try {
    		//update date
    		res.header("Date", HandlerHelper.responseDate());
    		
            //update body type - text/html
            if(res.type() == null) {
            	res.type("text/html");
            }
            
            //generate first line
            String firstLine = HandlerHelper.generateFirstline(res.status());
    		
            // check session
            logger.info("checking session before sending response...");
            if(req!=null && req.session(false) != null && ((HttpSession) req.session(false)).isNew()) {
            	res.cookie("JSESSIONID",req.session(false).id());
            }
			// send response
    		logger.info("\r\n"+firstLine+res.getHeaders()+"\r\n"+res.body());
    		OutputStream out = socket.getOutputStream();
    		DataOutputStream ds = new DataOutputStream(out);
    		
    		ds.writeBytes(firstLine);
    		ds.writeBytes(res.getHeaders());
    		
    		logger.info("Check if body needed to be sent: "+ req.requestMethod());
    		
    		// successful response has no body for head
    		if(!req.requestMethod().equals("HEAD")) {
    			ds.writeBytes("\r\n");
    			ds.writeBytes(res.body());
    		}

    		ds.flush();
    		
    		logger.info("Response sent.");
    		
		} catch (IOException e) {
			logger.error("Failed to sent response.");
			sendException(socket, res, new HaltException(500), req);
			
		} 
		
    	
    	return false;
        
    }
    
}

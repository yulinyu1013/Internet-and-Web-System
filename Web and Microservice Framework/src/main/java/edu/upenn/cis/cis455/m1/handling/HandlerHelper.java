package edu.upenn.cis.cis455.m1.handling;

import java.io.File;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.m1.server.HttpRequest;
import edu.upenn.cis.cis455.m1.server.WebService;


public class HandlerHelper {
	final static Logger logger = LogManager.getLogger(HandlerHelper.class);
	
	
	private HandlerHelper() {};
	
//	Request Helpers
	
	/**
	 * Helper function to get content type from request 
	 * **/
	public static String getMIMEContentType(String file) {
		
		if (file.endsWith(".htm") || file.endsWith(".html")) {
			return "text/html";
		}
		
		if (file.endsWith(".css")) {
			return "text/css";
		}
		
		if (file.endsWith(".jpg") || file.endsWith(".jpeg")) {
			return "image/jpeg";
		}
		
		if (file.endsWith(".png")) {
			return "image/png";
		} 
		
		if (file.endsWith(".gif")) {
			return "image/gif";
		}
		
		if (file.endsWith(".txt")) {
			return "text/plain";
		}
		
		return "application/octet-stream";
	}
	
	/**
	 * Helper function to check if the path a valid unix path;
	 * **/
	public static boolean isValidPath(String path) {
	    try {
	        Paths.get(path);
	    } catch (InvalidPathException e) {
	        return false;
	    }
	    return true;
	}
	
	
	/**
	 * Helper function to check if the path a valid unix path;
	 * **/
	public static int checkModifiedHeader(HttpRequest req) {
		File file = new File(WebService.getInstance().directory+req.pathInfo());
		
		if(req.headers("if-modified-since")!= null) {
			Date modifiedSince = dateParser(req.headers("if-modified-since")); 	
			logger.info("file.lastModified: "+ file.lastModified());
			if(modifiedSince != null && modifiedSince.getTime() >= file.lastModified()) {
				logger.info("finish checking modified header...");
				return 304;
			}
		} else if (req.headers("if-unmodified-since")!= null) {
			Date unmodifiedSince = dateParser(req.headers("if-unmodified-since")); 
			logger.info("file.lastModified: "+ file.lastModified());
			if(unmodifiedSince != null  && unmodifiedSince.getTime() < file.lastModified()) {
				logger.info("finish checking modified header...");
				return 412;
			}
		}
		logger.info("finish checking modified header...");
		return 200;
		
	}
	
	/**
	 * Helper function to parse date from request headers
	 * **/
	public static Date dateParser(String str) {
		// set up all three possible formats: https://www.jmarshall.com/easy/http/#http1.1s7
		// https://stackoverflow.com/questions/13593796/if-modified-since-date-format
		SimpleDateFormat format1 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
		format1.setTimeZone(TimeZone.getTimeZone("GMT"));
		SimpleDateFormat format2 = new SimpleDateFormat("E, dd-MMM-yy HH:mm:ss z");
		format2.setTimeZone(TimeZone.getTimeZone("GMT"));
		SimpleDateFormat format3 = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy");
		format3.setTimeZone(TimeZone.getTimeZone("GMT"));
		
		Date date = null;
		try {
			date = format1.parse(str);
		} catch (ParseException e) {
			try {
				date = format2.parse(str);
			} catch (ParseException e1) {
				try {
					date = format3.parse(str);
				} catch (ParseException e2) {

					date = null;
				}
			}
		}
		
		if (date.getTime() > System.currentTimeMillis()){ // ignore future date header
			return null;
		}
		return date;
	}
	
	
	
	
//  Response/Exception Helpers
	
	/**
	 * Helper function to parse generate response date
	 * **/
	public static String responseDate() {
		SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
		format.setTimeZone(TimeZone.getTimeZone("GMT"));
		return format.format(Calendar.getInstance(Locale.US).getTime());
	}
	
	/**
	 * Helper function to generate first line of the response
	 * **/
	public static String generateFirstline(int code) {
		return "HTTP/1.1 " + code + " " + statusMsg(code) + "\r\n";
	}
	
	
	/**
	 * Helper function to generate status message
	 * **/
	public static String statusMsg(int code) {
		
		switch(code) {
			case 200: return "OK";
			case 301: return "Moved Permanently";
			case 302: return "Found";
			case 304: return "Not Modified";
			case 400: return "Bad Request";
			case 403: return "Forbidden";
			case 404: return "Not Found";
			case 412: return "Precondition Failed";
			case 500: return "Internal Server Error";
			case 501: return "Not Implemented";
			case 505: return "HTTP Version Not Supported";
			default: return "Undefined"; 
		}
	}

}

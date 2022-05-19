package edu.upenn.cis.cis455.m1.handling;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.exceptions.HaltException;
import edu.upenn.cis.cis455.m1.server.HttpRequest;
import edu.upenn.cis.cis455.m1.server.HttpResponse;
import edu.upenn.cis.cis455.m1.server.HttpWorker;
import edu.upenn.cis.cis455.m1.server.ThreadPool;
import edu.upenn.cis.cis455.m1.server.WebService;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configuration;

public class StaticFileHandler {
	final static Logger logger = LogManager.getLogger(StaticFileHandler.class);
	
	public static void handleRequest(HttpRequest req, HttpResponse res, ThreadPool pool) throws HaltException {	
		
		logger.info("Checking request method...");
		if (!req.requestMethod().equals("GET") && !req.requestMethod().equals("HEAD")) {

			throw new HaltException(501, "Not a valid method to request static file");
		} 
		
		// special urls
		logger.info("Checking if it is a special url..."); 
		
		// shutdown
		if (req.requestMethod().equals("GET") && req.pathInfo().equals("/shutdown")) {
			logger.info("Handling shutdown...");
			res.status(200);
			res.header("Date", HandlerHelper.responseDate());
			res.type("text/html");
	    	String body = "<!DOCTYPE html>\r\n<html><body>\r\n" 
	    			+ "<h1>The server is shut down.</h1>\r\n"
	    			+ "</body></html>\r\n";
			res.body(body);
		} 
		
		// control
		else if (req.requestMethod().equals("GET") && req.pathInfo().equals("/control")) {
			handleControlPanel(req, res, pool);
		}
		
		else if (req.requestMethod().equals("GET") && req.pathInfo().equals("/control/log")) {
			handleAppLog(req, res);
		}
		
		// ordinary paths
		else {
			logger.info("Checking files...");
			String path = WebService.getInstance().directory + req.pathInfo();
			
			// check if path is valid
			if(HandlerHelper.isValidPath(path)==false) {
				logger.info("Invalid path found...");
				throw new HaltException(400, "Bad Request: invalid path");
			}
			
			File file = new File(path);
			
			// check if file exists
			if(!file.exists()) {
				logger.info("File not found...");
				throw new HaltException(404, "Not Found");
			}
			
			// check if root dir not the same as the static one
			try {
				logger.info("File path:" + file.getCanonicalPath());
				if(!file.getCanonicalPath().startsWith(new File(WebService.getInstance().directory).getCanonicalPath())) {
					throw new HaltException(403, "Cannot request files outside the static directory!");
				}				
				
			} catch (IOException e1) {
				logger.error("Cannot get file canonical path");
				throw new HaltException(400, "Bad Request: invalid path");
			} 
			
			// check if it is a single file
			 if (file.isFile()) { 
				try {
					handleFile(file, req, res);
				} catch (IOException e) {
					logger.error("fail to handle file");
					throw new HaltException(500, "Internal Server Error");
				}
				
			} // check if it is a directory
			 else if(file.isDirectory()) {
				try {
					handleDirectory(file, req, res);
				} catch (IOException e) {
					logger.error("fail to handle index.html");
					throw new HaltException(500, "Internal Server Error");
				}
			}else {
				throw new HaltException(400, "unknown");
			}
			
		}
		
	}
	
	/**
	 * Helper function to handle GET /control
	 * **/
	private static void handleAppLog(HttpRequest req, HttpResponse res) {
		StringBuilder log = new StringBuilder("Error Log");
		log.append(System.lineSeparator());
		log.append(System.lineSeparator());
		
		LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();
        Map<String, Appender> appenders = config.getAppenders();
        String logFile = null;
        
        for (String name: appenders.keySet()) {
            Appender appender = appenders.get(name);
            if (appender instanceof FileAppender) {
                logFile = ((FileAppender) appender).getFileName();
                break;
            }
        }
        
        try {
        	List<String> lines = Files.readAllLines(Path.of(logFile));
        	for(int i = lines.size()-1; i>=0; i--) {
        		String s = lines.get(i);
        		if (s.indexOf("ERROR") >= 0) {
        			log.append(s);
        			log.append(System.lineSeparator());
        		}
        	}
        	
		} catch (IOException e) {
			logger.info("No error log found.");
		}
		
        res.body(log.toString());
        res.type("text/plain");
		
		
	}
	
	/**
	 * Helper function to handle GET /control
	 * **/
	private static void handleControlPanel(HttpRequest req, HttpResponse res, ThreadPool pool) {
		logger.info("Handling control panel...");
		
		//set up first line
		res.status(200);
		
		//set up header and body related info
//		res.header("Date", HandlerHelper.responseDate());
		res.type("text/html");
		
		StringBuilder panel = new StringBuilder();
		
		panel.append("<!DOCTYPE html>\r\n<html><body>\r\n");
		panel.append("<h1>Control Panel</h1>\r\n");
		panel.append("<table>\r\n"
				+ "<thead>\r\n"
				+ "  <tr>\r\n"
				+ "    <th>Thread</th>\r\n"
				+ "    <th>Status</th>\r\n"
				+ "    <th>Url</th>\r\n"
				+ "  </tr>\r\n"
				+ "</thead>\r\n"
				+ "<tbody>\r\n");
		for(HttpWorker worker : pool.getPool()) {
			// a) a list of all the threads in the thread pool, 
			// b) the status of each thread ('waiting' or the URL it is currently handling)
			if(worker.getThreadStatus().equals("WAITING")) {
				panel.append("  <tr>\r\n"
						+ "    <td>"+worker.getThreadName()+"</td>\r\n"
						+ "    <td>"+worker.getThreadStatus()+"</td>\r\n"
						+ "    <td></td>\r\n"
						+ "  </tr>\r\n");
			} else {
				panel.append("  <tr>\r\n"
						+ "    <td>"+worker.getThreadName()+"</td>\r\n"
						+ "    <td>"+worker.getThreadStatus()+"</td>\r\n"
						+ "    <td>"+worker.getReq().url()+"</td>\r\n"
						+ "  </tr>\r\n");
			}
		}
		// c) a button that shuts down the server
		panel.append("<table>\r\n" + "</tbody>\r\n" + "</table>\r\n");
		panel.append("<br><a href=\"/control/log\">App log</a></br>\r\n");
		panel.append("<br><a href=\"/shutdown\">shutdown</a></br>\r\n");
		panel.append("</body></html>\r\n");
		
		res.body(panel.toString());
	}
	
	
	/**
	 * Helper function to handle a single file
	 * **/
	private static void handleFile(File file, HttpRequest req, HttpResponse res) throws IOException, HaltException{
		logger.info("Handling a single file...");
		
		//set the first line
		int code = HandlerHelper.checkModifiedHeader(req);
		
		res.status(code);
		
		//set the header and body related info
		if(file.lastModified()!=0) {
			String lastModified = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z").format(file.lastModified());
			res.header("Last-Modified", lastModified);
		}
		
		if(code==412) {
			throw new HaltException(412);
		}
		res.header("Date", HandlerHelper.responseDate());
		res.type(HandlerHelper.getMIMEContentType(file.getName()));
		res.bodyRaw(Files.readAllBytes(file.toPath()));
	}
	
	
	/**
	 * Helper function to handle a single directory
	 * **/
	public static void handleDirectory(File file, HttpRequest req, HttpResponse res) throws IOException, HaltException {
		logger.info("Handling a directory...");
		// set the first line
		res.status(HandlerHelper.checkModifiedHeader(req));
		
		//set up header and body related info
		logger.info("Checking statusCode...");
		if(file.lastModified()!=0) {
			String lastModified = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z").format(file.lastModified());
			res.header("Last-Modified", lastModified);;
		}
//		res.header("Date", HandlerHelper.responseDate());
		res.type("text/html");
		
		//get list of files in the directory
		logger.info("Getting file list...");
		String[] fileList = file.list();
		
		// check if the directory has index.html
		if(Arrays.asList(fileList).contains("index.html")) {
			File f = new File(file.getCanonicalFile().toString()+"/index.html");
			res.bodyRaw(Files.readAllBytes(f.toPath()));
			
		} else {
			logger.info("Handling dir w/o index.html...");
			StringBuilder list = new StringBuilder();
			list.append("<!DOCTYPE html>\r\n<html><body>\r\n");
			list.append("<h1>Index of "+req.pathInfo()+"</h1>\r\n");
			
			list.append("<table>\r\n"
					+ "<thead>\r\n"
					+ "  <tr>\r\n"
					+ "    <th>  File  </th>\r\n"
					+ "    <th>  Last Modified  </th>\r\n"
					+ "    <th>  Size(bytes)  </th>\r\n"
					+ "  </tr>\r\n"
					+ "</thead>\r\n"
					+ "<tbody>\r\n");
			
		
			for(String f: fileList) {
			    Map<String, Object> attributes;
			    logger.info("Mapping file to url...");        
				logger.info(req.uri());
				if(req.pathInfo().endsWith("/")) {
					attributes =  Files.readAttributes(Path.of(WebService.getInstance().directory+req.pathInfo()+f), "lastModifiedTime,size");
					list.append("  <tr>\r\n"
							+ "    <td>"+"<a href=\"" + req.uri() + f + "\">" + f + "</a>"+"</td>\r\n"
							+ "    <td>"+attributes.get("lastModifiedTime")+"</td>\r\n"
							+ "    <td>"+attributes.get("size")+"</td>\r\n"
							+ "  </tr>\r\n");

				} else {
					attributes =  Files.readAttributes(Path.of(WebService.getInstance().directory+req.pathInfo()+"/"+f), "lastModifiedTime,size");
					list.append("  <tr>\r\n"
							+ "    <td>"+"<a href=\"" + req.uri() + "/" + f + "\">" + f + "</a>"+"</td>\r\n"
							+ "    <td>"+attributes.get("lastModifiedTime")+"</td>\r\n"
							+ "    <td>"+attributes.get("size")+"</td>\r\n"
							+ "  </tr>\r\n");
					
				}
				
				
				//back to parent directory
				list.append("  <tr>\r\n"
						+ "    <td>"+"<a href=\"" + req.uri().substring(0, req.uri().lastIndexOf("/"))  + "\">" + ".." + "</a>"+"</td>\r\n"
						+ "    <td>"+"</td>\r\n"
						+ "    <td>"+"</td>\r\n"
						+ "  </tr>\r\n");
			}
			
			list.append("<table>\r\n"
					+ "</tbody>\r\n"
					+ "</table>\r\n");
			
			list.append("</body></html>\r\n");
			
			res.body(list.toString());
			logger.info("finishing handling directory...");
		}
		
	}
}

package edu.upenn.cis.cis455.m2.server;

import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.exceptions.HaltException;
import edu.upenn.cis.cis455.m1.handling.HttpIoHandler;
import edu.upenn.cis.cis455.m1.handling.StaticFileHandler;
import edu.upenn.cis.cis455.m1.server.HttpRequest;
import edu.upenn.cis.cis455.m1.server.HttpResponse;
import edu.upenn.cis.cis455.m2.interfaces.HttpRoutes;


public class MockWorker {
	final static Logger logger = LogManager.getLogger(MockWorker.class);
	
	
	public static void work(HttpRequest req, HttpResponse res, Socket socket, HttpRoutes routes) {
		
		try {
			// parse request
//			logger.info("Parsing socket data to req...");
//			req = HttpIoHandler.parseRequest(socket); 
			
			logger.info("Generating response...");
			
			// handle before filters
			logger.info("Handling before filters...");
			MockRouteHandler.handleBefore(req, res, routes);
			
			// handle route
			logger.info("Handling route...");
			boolean handled = MockRouteHandler.handleRoute(req, res, routes);
			
			// if no matching route, check static file
			if(!handled) {
				logger.info("No matching route, handling static file...");
				StaticFileHandler.handleRequest(req, res, null);
			}
			
			// handle after filters
			logger.info("Handling after filter...");
			MockRouteHandler.handleAfter(req, res, routes);
		
			logger.info("Finish generating response...");
			// send response
			logger.info("Sending response...");
			HttpIoHandler.sendResponse(socket, req, res);
		
		} catch (InterruptedException e1) {
			logger.debug("Failed to take task from queue. Likely that server is down.");
			
		} catch (HaltException e1) {
			HttpIoHandler.sendException(socket, res, e1, req);
		} 
		 catch (Exception e) {
			 if(e instanceof HaltException) {
				 HttpIoHandler.sendException(socket, res, (HaltException) e, req);
			 } else {
				 HttpIoHandler.sendException(socket, res, new HaltException(500), req); 
			 }
			
		} 
	}
}

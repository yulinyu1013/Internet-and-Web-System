package edu.upenn.cis.cis455.m1.server;

import java.io.IOException;
import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.exceptions.HaltException;
import edu.upenn.cis.cis455.m1.handling.HttpIoHandler;
import edu.upenn.cis.cis455.m1.handling.RouteHandler; 
import edu.upenn.cis.cis455.m1.handling.StaticFileHandler;
import edu.upenn.cis.cis455.m2.server.WebService;



/**
 * Stub class for a thread worker that handles Web requests
 */
public class HttpWorker implements Runnable {
	final static Logger logger = LogManager.getLogger(HttpWorker.class);
	private HttpTaskQueue taskQueue;
	private ThreadPool pool;
	private Thread thread;
	private volatile boolean isShutdown = false;
	private Socket socket = null;
	private HttpRequest req = null;
	private HttpResponse res = new HttpResponse();
	private HttpTask t;
	
	
	public HttpWorker(HttpTaskQueue taskQueue, ThreadPool pool) {
		this.taskQueue = taskQueue;
		this.pool = pool; 
		this.thread = new Thread(this);
	}
	
	/**
     * Start/stop worker
     */
	public void start() {
		this.thread.start();
	}
	
	public void shutdown() {
		this.isShutdown = true;
		if(this.getThreadStatus().equals("WAITING")) {
			thread.interrupt();
		}

	}
	
	public boolean isShutdown() {
		return isShutdown;
	}
	
	/**
     * getters and setters
     */
	public String getThreadName() {
		return this.thread.getName();
	}
	
	public String getThreadStatus() {
		return this.thread.getState().toString();
	}
    
    public HttpRequest getReq() {
		return req;
	}

    
	@Override
    public void run() {
		while(!isShutdown()) {
			try {
				logger.info("Taking task from queue...");
				t = taskQueue.take();
				
				// initial setup
				logger.info("Taking socket from task...");
				socket = t.getSocket();
				
	    		// parse request
				logger.info("Parsing socket data to req...");
				req = HttpIoHandler.parseRequest(socket);
				
				
				if(req == null) continue;
				logger.info("Generating response...");
				
				// handle before filters
				logger.info("Handling before filters...");
				RouteHandler.handleBefore(req, res);
				
				// handle route
				logger.info("Handling route...");
				boolean handled = RouteHandler.handleRoute(req, res);
				
				// if no matching route, check static file
				if(!handled) {
					logger.info("No matching route, handling static file...");
					StaticFileHandler.handleRequest(req, res, pool);
				}
				
				// handle after filters
				logger.info("Handling after filter...");
				RouteHandler.handleAfter(req, res);
			
				logger.info("Finish generating response...");
				
				// send response
				logger.info("Sending response...");
				HttpIoHandler.sendResponse(socket, req, res);
				
				if(req.pathInfo().equals("/shutdown")) {
					logger.info("Shutting down server...");
					WebService.getInstance().stop();
				}
				
			} catch (InterruptedException e1) {
				logger.info("Exceptions caught by worker...");
				logger.debug("Failed to take task from queue. Likely that server is down.");
				
			} 
			catch (HaltException e) {
				logger.info("Exceptions caught by worker...");
				logger.error(((HaltException) e).statusCode() + " error found with request: " + req.requestMethod() + " " + req.pathInfo());
				HttpIoHandler.sendException(socket, res, e, req);
			}  
			 
			 catch (Exception e) {
				 logger.info("Exceptions caught by worker...");
				 
				 if(e instanceof HaltException) {
					 logger.error(((HaltException) e).statusCode() + " error found with request: " + req.requestMethod() + " " + req.pathInfo());
					 HttpIoHandler.sendException(socket, res, (HaltException) e, req);
				 } else {
					 logger.debug("Internal server error during request: " + (req!=null ? req.requestMethod() + " " + req.pathInfo() : ""));
					 HttpIoHandler.sendException(socket, res, new HaltException(500), req); 
				 }
				
			} 
			finally {
				try {
					if(socket != null) {
						logger.info("Closing socket...");
						socket.close();
					}
					
					logger.info("Work finished.");
					
				} catch (IOException e) {
					e.printStackTrace();
					logger.error("Failed to close socket after the worker finished.");
				}
				
			}
			
		}
		logger.info(getThreadName() + " for worker stopped.");

    }

}

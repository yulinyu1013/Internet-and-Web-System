package edu.upenn.cis.cis455.m1.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Stub for your HTTP server, which listens on a ServerSocket and handles
 * requests
 */
public class HttpListener implements Runnable {
	final static Logger logger = LogManager.getLogger(HttpListener.class);
	
	private ServerSocket serverSocket;
	private HttpTaskQueue taskQueue;
	private Thread thread = null;
	private volatile boolean isShutdown = false;
	
	/**
     * Constructor
     */
	public HttpListener(HttpTaskQueue taskQueue, int port, String ipAddress)  {
		this.taskQueue = taskQueue;

		try {
			logger.info("Initiating server socket...");
			this.serverSocket = new ServerSocket(port, 200, InetAddress.getByName(ipAddress));
		} catch (UnknownHostException e) {
			logger.debug("Failed to start server socket: unknown host.", e.getMessage());
		} catch (IOException e) {
			logger.error("Failed to start server socket.", e.getMessage());
		}
		this.thread = new Thread(this);
	};
	


	@Override
    public void run() {
        while(!isShutdown()) {
        	try {
				Socket socket = serverSocket.accept();
				HttpTask task = new HttpTask(socket);
				logger.info("Sending a task to queue...");
				taskQueue.put(task);
			} catch (IOException e) {
				logger.debug("Failed to accept request from socket.");
			} catch (InterruptedException e) {
				logger.debug("Failed to add request to queue.");
			}
        }
        
        logger.info("Exit the listener while loop...");
        logger.info(thread.getName() + " for listener stopped");
    }
	
	
	/**
     * Launches the listener
     */
	public void start() {
		thread.start();
	}
	
	
	/**
     * Shut down the listener
     */
    public void shutdown() {
    	logger.info("Shuting down the listener...");
    	this.isShutdown = true;
        
        try {
        	logger.info("Shuting down the server socket...");
			serverSocket.close();
			logger.info("Server Socket closed.");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.debug("Failed to add request to queue.", e.getMessage());
		}

    }
    
	/**
     * Check status of listener
     */
	public boolean isShutdown() {
		return isShutdown;
	}
	
	
}

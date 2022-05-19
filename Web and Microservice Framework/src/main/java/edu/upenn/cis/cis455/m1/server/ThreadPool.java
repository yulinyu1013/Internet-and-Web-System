package edu.upenn.cis.cis455.m1.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ThreadPool {
	final static Logger logger = LogManager.getLogger(ThreadPool.class);
	
	private int numThreads; 
	private HttpWorker[] pool; 
	private HttpTaskQueue taskQueue;
	
	public ThreadPool(HttpTaskQueue taskQueue, int numThreads) {
		this.taskQueue = taskQueue;
		this.numThreads = numThreads;
	} 

	/**
	 * Start the thread pool 
	 **/
	public void start() {
		
		this.pool = new HttpWorker[numThreads];
		
		for(int i = 0; i < numThreads; i++) {
			pool[i] = new HttpWorker(taskQueue, this);  
		}
		
		for(HttpWorker worker : pool) {
			worker.start();
		}
		logger.info("Thread Pool starts...");	
		
	}
	
	/**
	 * Shut down the thread pool 
	 **/
	public void shutdown() {
	
		for(HttpWorker worker : pool) {
			logger.info("Shutting down "+ worker.getThreadName());
			worker.shutdown();
		}
		
		logger.info("Thread Pool terminated...");
	}

	public HttpWorker[] getPool() {
		return pool;
	}
	
}

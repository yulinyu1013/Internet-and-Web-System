package edu.upenn.cis.cis455.m1.server;

import java.util.LinkedList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Stub class for implementing the queue of HttpTasks
 */
public class HttpTaskQueue {
	final static Logger logger = LogManager.getLogger(HttpTaskQueue.class);
	
	private LinkedList<HttpTask> queue = new LinkedList<HttpTask>();
	private int limit = 500;
	
	public HttpTaskQueue() {
	
	}

	
	public LinkedList<HttpTask> getQueue() {
		return queue;
	}


	public int getLimit() {
		return limit;
	}


	public void setLimit(int limit) {
		this.limit = limit;
	}

    /**
     * Synchronized method for the listener to add request to the queue
     */
	public synchronized void put(HttpTask t) throws InterruptedException {
		logger.info("Queue size before adding: " + this.queue.size());
		while(this.queue.size() == this.limit) {
			logger.info("Queue is full... Please wait...");
			wait();
		}
		
		logger.info("Adding task to queue...");
		this.queue.add(t);
		logger.info("Notifying workers...");
		notify();
		
	}
	
    /**
     * Synchronized method for the thread pool to pull request from the queue
     */
	public synchronized HttpTask take() throws InterruptedException {
		while(queue.isEmpty()) {
			logger.info("Queue is empty... Please wait...");
			wait();
		}
	
		notify();
		
		logger.info("Pulling task from queue...");
		return this.queue.poll();
	}
}

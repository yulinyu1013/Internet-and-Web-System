package edu.upenn.cis455.mapreduce.worker;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;

public class WorkerStatus {
	private String ip;
	private String port;
	private String status;
	private String job; //classname
	private String keysRead;
	private String keysWritten;
	private String results;
	private Instant lastChecked; // To see when was the last update
	private boolean removed;
	
	
	public WorkerStatus(String ip, String port, String status, String job, String keysRead, String keysWritten, String result, Instant lastChecked) {
		super();
		this.ip = ip;
		this.port = port;
		this.status = status;
		this.job = job;
		this.keysRead = keysRead;
		this.keysWritten = keysWritten;
		this.results = result;
		this.lastChecked = lastChecked;
		this.removed = false;
	}
	
	public String getIp() {
		return ip;
	}
	
	public String getPort() {
		return port;
	}
	
	public String getStatus() {
		return status;
	}
	
	public String getJob() {
		return job;
	}
	
	public String getKeysRead() {
		return keysRead;
	}
	
	public String getKeysWritten() {
		return keysWritten;
	}
	
	public String getResult() {
		return results;
	}

	public Instant getLastChecked() {
		return lastChecked;
	}

	public boolean isRemoved() {
		return removed;
	}

	public void setRemoved(boolean removed) {
		this.removed = removed;
	}
	

	
	
}

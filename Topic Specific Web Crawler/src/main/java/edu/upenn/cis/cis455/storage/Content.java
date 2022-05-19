package edu.upenn.cis.cis455.storage;

import java.io.Serializable;
import java.time.Instant;

public class Content implements Serializable {

	private static final long serialVersionUID = -3874637689627504391L;
	private String url;
	private String type;
	private int length;
	private String content;
	private Instant lastChecked;
	
	public Content(String url, String content) {
		super();
		this.url = url;
		this.content = content;

	}
	
	
	public Content(String url, String type, int length, String content, Instant lastChecked) {
		this.url = url;
		this.type = type;
		this.length = length;
		this.content = content;
		this.lastChecked = lastChecked;
	}
	
	
	public String getUrl() {
		return url;
	}


	public void setUrl(String url) {
		this.url = url;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public Instant getLastChecked() {
		return lastChecked;
	}

	public void setLastChecked(Instant lastChecked) {
		this.lastChecked = lastChecked;
	}

}

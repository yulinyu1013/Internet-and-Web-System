package edu.upenn.cis.cis455.storage;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import edu.upenn.cis.cis455.crawler.utils.URLInfo;


public class Channel implements Serializable {

	private static final long serialVersionUID = -1410832544856694739L;
	private String name;
	private String xpath;
	private String username;
	private Set<String> docUrls = new HashSet<String>();
	
	public String getName() {
		return name;
	}

	public String getXpath() {
		return xpath;
	}

	public String getUsername() {
		return username;
	}

	public Set<String> getDocUrls() {
		return docUrls;
	}

	public Channel(String name, String xpath, String username, Set<String> docUrls) {
		super();
		this.name = name;
		this.xpath = xpath;
		this.username = username;
		this.docUrls = docUrls;
	}
	
	public void addDoc(String url) {
		docUrls.add(url);
	}
	
}

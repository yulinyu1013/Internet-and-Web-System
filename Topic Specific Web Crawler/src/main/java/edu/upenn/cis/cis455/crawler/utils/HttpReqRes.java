package edu.upenn.cis.cis455.crawler.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;


import javax.net.ssl.HttpsURLConnection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class HttpReqRes {
	
	final static Logger logger = LogManager.getLogger(HttpReqRes.class);
	private int statusCode = 404;
	private String contentType = null;
	private int contentLength = 0;
	private Instant lastModified = null;
	private String location = null;
	private String body = "";
	// req
	private boolean isHead;
	
    /**
     * Getters and setters
     */
	public int getStatusCode() {
		return statusCode;
	}

	public String getContentType() {
		return contentType;
	}

	public int getContentLength() {
		return contentLength;
	}

	public String getLocation() {
		return location;
	}

	public String getBody() {
		return body;
	}

	public Instant getLastModified() {
		return lastModified;
	}
	
	public HttpReqRes() {}
	

    /**
     * Constructor for test
     */
	public HttpReqRes(int statusCode, String contentType, int contentLength, Instant lastModified, String location,
			String body) {
		super();
		this.statusCode = statusCode;
		this.contentType = contentType;
		this.contentLength = contentLength;
		this.lastModified = lastModified;
		this.location = location;
		this.body = body;
	}

    /**
     * Get response
     */
	public HttpReqRes getResponse(URLInfo urlInfo, String requestMethod, Instant lastChecked) throws IOException  {
		isHead = requestMethod.equals("HEAD");
		
		if(urlInfo.isSecure()) {
			sendHttpsRequest(urlInfo, requestMethod, lastChecked);
		}else {
			sendHttpRequest(urlInfo, requestMethod, lastChecked);
		}
		
		return this;
	}
	
    /**
     * Send https request
     */
	private void sendHttpsRequest(URLInfo urlInfo, String requestMethod, Instant lastChecked) throws IOException  {
		// init connection
//		logger.info(urlInfo.toString());
		try {
			URL url = new URL(urlInfo.toString());
			HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
			if (connection == null) {
				return;
			}
			
			// config and send request
			connection.setRequestMethod(requestMethod);
			connection.setRequestProperty("Host", urlInfo.getHostName());
			connection.setRequestProperty("User-Agent", "cis455crawler");
			if(lastChecked != null) {
				connection.setIfModifiedSince(lastChecked.toEpochMilli());
			}
			
			connection.setDoInput(true);
			connection.setDoOutput(true);
			HttpURLConnection.setFollowRedirects(false);
			
			// get response
			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			
			try {
		
				//get headers
				statusCode = connection.getResponseCode();
				contentLength = connection.getContentLength();
				contentType = connection.getContentType();
				location = connection.getHeaderField("Location");
				if(connection.getLastModified() != 0) {
					lastModified = Instant.ofEpochMilli(connection.getLastModified());
				}
				else {
					if(connection.getDate() != 0) {
						lastModified = Instant.ofEpochMilli(connection.getLastModified());					
					}
				}
			
				//get body
				
				StringBuilder sb = new StringBuilder();
				String line = in.readLine();
				while(line != null) {
					sb.append(line+"\r\n");
					line = in.readLine();
				}
				body = sb.toString();

			} catch (IOException e) {
				// TODO Auto-generated catch block
//				e.printStackTrace();
			}
			
			connection.disconnect();
		}catch(Exception e){
			
		}
	}
	
	
    /**
     * Send http request
     */
	private void sendHttpRequest(URLInfo urlInfo, String requestMethod, Instant lastChecked) throws IOException  {
		
//		logger.info(urlInfo.toString());
		try {
			URL url = new URL(urlInfo.toString());
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			if (connection == null) {
				return;
			}
			
			// config and send request
			connection.setRequestMethod(requestMethod);
			connection.setRequestProperty("Host", urlInfo.getHostName());
			connection.setRequestProperty("User-Agent", "cis455crawler");
			if(lastChecked != null) {
				connection.setIfModifiedSince(lastChecked.toEpochMilli());
			}
			
			connection.setDoInput(true);
			connection.setDoOutput(true);
			HttpURLConnection.setFollowRedirects(false);
			
			// get response
			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			
			try {
		
				//get headers
				statusCode = connection.getResponseCode();
				contentLength = connection.getContentLength();
				contentType = connection.getContentType();
				location = connection.getHeaderField("Location");
				if(connection.getLastModified() != 0) {
					lastModified = Instant.ofEpochMilli(connection.getLastModified());
				}
				else {
					if(connection.getDate() != 0) {
						lastModified = Instant.ofEpochMilli(connection.getLastModified());					
					}
				}
			
				//get body
				
				StringBuilder sb = new StringBuilder();
				String line = in.readLine();
				while(line != null) {
					sb.append(line+"\r\n");
					line = in.readLine();
				}
				body = sb.toString();
					
			} catch (IOException e) {
				// TODO Auto-generated catch block
//				e.printStackTrace();
			}
			
			connection.disconnect();
			
		}catch(Exception e){
			
		}
		
	}
	
}

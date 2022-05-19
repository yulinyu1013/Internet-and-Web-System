package edu.upenn.cis.cis455.storage;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ContentSeen implements Serializable{

	private static final long serialVersionUID = 4341706936772205363L;
	private String url;
	private byte[] md5;
	
	public String getUrl() {
		return url;
	}

	public byte[] getMd5() {
		return md5;
	}

	public ContentSeen(String url, String content) {
		this.url = url;
		try {
			this.md5 = hashContent(content);
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private byte[] hashContent(String content) throws NoSuchAlgorithmException {
		MessageDigest digest = MessageDigest.getInstance("MD5");
		return digest.digest(content.getBytes(StandardCharsets.UTF_8));
	}

	
}

package edu.upenn.cis.cis455.storage;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class User implements Serializable {

	private static final long serialVersionUID = 8266627306154364677L;
	private String username;
	private byte[] hashedPassword;
	
	public User(String username, String password) throws NoSuchAlgorithmException {
		this.username = username;
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		this.hashedPassword = digest.digest(password.getBytes(StandardCharsets.UTF_8));
	}

	public byte[] getHashedPassword() {
		return hashedPassword;
	}

	public String getUsername() {
		return username;
	}


}

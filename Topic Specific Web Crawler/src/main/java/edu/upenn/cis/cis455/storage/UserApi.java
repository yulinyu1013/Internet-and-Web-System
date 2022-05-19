package edu.upenn.cis.cis455.storage;

import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.Transaction;

public class UserApi {
	
	private static Storage database;
	
	public UserApi(Storage db) {
		database = db;
	}
	
	public void addUser(User user) {
		DatabaseEntry theKey = new DatabaseEntry();
		DatabaseEntry theData = new DatabaseEntry();
		
		database.getUserKeyBinding().objectToEntry(user.getUsername(), theKey);
		database.getUserValBinding().objectToEntry(user, theData);
		
		Transaction trans = database.getEnv().beginTransaction(null, null);
		database.getUserDB().put(null, theKey, theData);
		trans.commit();
	}
	
	public User getUser(String username) {
		DatabaseEntry theKey = new DatabaseEntry();
		DatabaseEntry theData = new DatabaseEntry();
		
		database.getUserKeyBinding().objectToEntry(username, theKey);
		
		Transaction trans = database.getEnv().beginTransaction(null, null);
		OperationStatus status = database.getUserDB().get(null, theKey, theData, LockMode.DEFAULT);
		trans.commit();
		
		if(status == OperationStatus.SUCCESS) {
			return (User) database.getUserValBinding().entryToObject(theData);
		}
		
		return null;
		
	}

}

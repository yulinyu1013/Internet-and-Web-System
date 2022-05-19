package edu.upenn.cis.cis455.storage;

import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;

public class ContentSeenApi {
	private static Storage db;
	
	public ContentSeenApi(Storage database) {
		db = database;
	}
	
	public void addContentSeen(ContentSeen cs) {
		DatabaseEntry theKey = new DatabaseEntry();
		DatabaseEntry theData = new DatabaseEntry();
		
		db.getContentSeenKeyBinding().objectToEntry(cs.getMd5(), theKey);
		db.getContentSeenValBinding().objectToEntry(cs, theData);
		
//		Transaction trans = Storage.getEnv().beginTransaction(null, null);
		db.getContentSeenDB().put(null, theKey, theData);
//		trans.commit();
	}
	
	
	public boolean containsMD5(ContentSeen cs) {
		DatabaseEntry theKey = new DatabaseEntry();
		DatabaseEntry theData = new DatabaseEntry();
		
		db.getContentSeenKeyBinding().objectToEntry(cs.getMd5(), theKey);
		
//		Transaction trans = Storage.getEnv().beginTransaction(null, null);
		OperationStatus status = db.getContentSeenDB().get(null, theKey, theData, LockMode.DEFAULT);
//		trans.commit();
		if(status == OperationStatus.SUCCESS) {
			return true;
		}
		
		return false;
		
	}
	
	
}

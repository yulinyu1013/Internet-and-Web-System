package edu.upenn.cis.cis455.storage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.Transaction;

public class ContentApi {
	final static Logger logger = LogManager.getLogger(ContentApi.class);
	private static Storage database;

	public ContentApi(Storage db) {
		database = db;
	}
	
	public void addDocument(Content doc) {
		DatabaseEntry theKey = new DatabaseEntry(doc.getUrl().getBytes());
		DatabaseEntry theData = new DatabaseEntry();
		
//		database.getDocKeyBinding().objectToEntry(doc.getUrl(), theKey);
		System.out.println("Add doc with the key: " + new String(theKey.getData()));
		database.getDocValBinding().objectToEntry(doc, theData);
		
		Transaction trans = database.getEnv().beginTransaction(null, null);
		database.getDocDB().put(null, theKey, theData);
		trans.commit();		
	
	}
	
	
	public synchronized Content getDocument(String url) {
		DatabaseEntry theKey = new DatabaseEntry(url.getBytes());
		DatabaseEntry theData = new DatabaseEntry();
		
//		database.getDocKeyBinding().objectToEntry(url, theKey);
		System.out.println(new String(theKey.getData()));
		logger.info("Getting doc with url: "+ new String(theKey.getData()));
		
		Transaction trans = database.getEnv().beginTransaction(null, null);
		OperationStatus status = database.getDocDB().get(null, theKey, theData, LockMode.DEFAULT);
		logger.info("Get doc status: " + status.toString());
		trans.commit();
		
		if(status == OperationStatus.SUCCESS) {
			Content doc = (Content) database.getDocValBinding().entryToObject(theData);
			logger.debug("type: " + doc.getType());
//			logger.debug("Doc from db: "+ doc.getContent());
			return doc;
		}
		
		return null;
	}
}

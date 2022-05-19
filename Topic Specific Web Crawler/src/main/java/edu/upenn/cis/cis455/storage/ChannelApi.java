package edu.upenn.cis.cis455.storage;

import java.util.ArrayList;
import java.util.List;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.Transaction;

public class ChannelApi {
	private static Storage db;
	
	public ChannelApi(Storage database) {
		db = database;
	}
	
	public synchronized void addChannel(Channel c) {
		DatabaseEntry theKey = new DatabaseEntry(c.getName().getBytes());
		DatabaseEntry theData = new DatabaseEntry();
		
//		db.getChannelKeyBinding().objectToEntry(c.getName(), theKey);
		db.getChannelValBinding().objectToEntry(c, theData);
		
		Transaction trans = db.getEnv().beginTransaction(null, null);
		db.getChannelDB().put(null, theKey, theData);
		trans.commit();
		
	}

	
	public synchronized Channel getChannel(String name) {
		DatabaseEntry theKey = new DatabaseEntry(name.getBytes());
		DatabaseEntry theData = new DatabaseEntry();
		
//		db.getChannelKeyBinding().objectToEntry(name, theKey);
		
		Transaction trans = db.getEnv().beginTransaction(null, null);
		OperationStatus status = db.getChannelDB().get(null, theKey, theData, LockMode.DEFAULT);
		trans.commit();
		
		if( status == OperationStatus.SUCCESS) {
			return (Channel) db.getChannelValBinding().entryToObject(theData);
		}
		return null;
		
	}
	
	public synchronized List<Channel> getAllChannels(){
		List<Channel> channels = new ArrayList<Channel>();
		
		Cursor cursor = null;
		try {
		    cursor = db.getChannelDB().openCursor(null, null);

		    DatabaseEntry foundKey = new DatabaseEntry();
		    DatabaseEntry foundData = new DatabaseEntry();

		    while (cursor.getNext(foundKey, foundData, LockMode.DEFAULT) ==
		        OperationStatus.SUCCESS) {
		    	Channel c = (Channel) db.getChannelValBinding().entryToObject(foundData);
		    	channels.add(c);
		    }
		    return channels;
		    
		} catch (DatabaseException e) {
		    System.err.println("Error accessing database." + e);
		} finally {
		    cursor.close();
		}
		
		return channels;
	}	
	
}

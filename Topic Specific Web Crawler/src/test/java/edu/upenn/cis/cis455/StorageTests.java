package edu.upenn.cis.cis455;

import static org.mockito.Mockito.reset; 

import java.io.File;
import java.io.IOException;
import java.time.Instant;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.upenn.cis.cis455.storage.Content;
import edu.upenn.cis.cis455.storage.ContentSeen;
import edu.upenn.cis.cis455.storage.Storage;
import edu.upenn.cis.cis455.storage.StorageFactory;

import edu.upenn.cis.cis455.storage.User;
import junit.framework.TestCase;

public class StorageTests extends TestCase { 
	 
	String pathStr = "./database";
	Storage db;
	
    @Before
    public void setUp() { 
    	try {
			Thread.sleep(1000); 
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
//			e.printStackTrace(); 
		}
    	 
    	try {
			FileUtils.cleanDirectory(new File(pathStr));
		} catch (IOException e) {
			// TODO Auto-generated catch block
//			e.printStackTrace();
		}

    	db = (Storage) StorageFactory.getDatabaseInstance(pathStr);
    	
    }
    
    @Test
    public void testAddAndGetUser() {
    	System.out.println(db.getUserDB()==null);
    	db.addUser("test", "test");
    	User test = db.getUserApi().getUser("test");
    	assertTrue(test.getUsername().equals("test"));
    }
     

    @Test
    public void testAddAndGetContent() {
    	String url = "http://www.test.com";
    	Content test = new Content(url, "text/html", 10, "test", Instant.now());
    	db.addDocument(test);
    	Content test2 = db.getDocument(url);
    	assertTrue(test.getContent().equals(test2.getContent()));
    }
    
    @Test
    public void testAddAndUpdateContent() {
    	String url = "http://www.test.com";
    	Content test = new Content(url, "text/html", 10, "test", Instant.now());
    	Content test1 = new Content(url, "text/html", 12, "updated", Instant.now());
    	db.addDocument(test);
    	db.addDocument(test1);
    	Content test2 = db.getDocument(url);
    	assertTrue(test1.getContent().equals(test2.getContent()));
    	assertEquals(db.getCorpusSize(),1);
    }

    
    @Test
    public void testGetContentCount() {
    	String url = "http://www.test.com";
    	String url1 = "http://www.test1.com";
    	Content test = new Content(url, "text/html", 10, "test", Instant.now());
    	Content test1 = new Content(url1, "text/html", 12, "updated", Instant.now());
    	db.addDocument(test);
    	db.addDocument(test1);
    	assertEquals(db.getCorpusSize(),2);
    }

    
    @Test
    public void testAddAndGetContentSeen() {
    	String url = "http://www.test.com";
    	String content = "contentSeen";
    	ContentSeen test = new ContentSeen(url, content);
    	db.getContentSeenApi().addContentSeen(test);
    	assertTrue(db.getContentSeenApi().containsMD5(test));
    }
    
    
    @After
    public void tearDown() {
    	db.close();

    	
    }
}

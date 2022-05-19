package edu.upenn.cis.cis455;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.upenn.cis.cis455.crawler.Crawler;
import edu.upenn.cis.cis455.crawler.CrawlerFactory;
import edu.upenn.cis.cis455.storage.Storage;
import edu.upenn.cis.cis455.storage.StorageFactory;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class CrawlerTestExitEarly {

   
	 public static void main(String[] args) throws Exception {
    	
    	String pathStr = "./database";
    	
    	try {
			FileUtils.cleanDirectory(new File(pathStr));
			System.out.println("DB clear");
		} catch (IOException e) {
			// TODO Auto-generated catch block
//			e.printStackTrace();
		}   
   
    	try {
    	
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
//			e.printStackTrace();
		}
    	
    	
    	String startUrl = "https://crawltest.cis.upenn.edu/nytimes/";
    	int size = 1;
    	int count = 100; 
    	Storage db = (Storage) StorageFactory.getDatabaseInstance(pathStr);
    	System.out.println("Creating crawler...");
    	Crawler crawler = new Crawler(startUrl, db, size, count); 
    	CrawlerFactory.setCrawler(crawler);
    	System.out.println("Starting crawler...");
    	crawler.start();
    	
        while (!crawler.isDone()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
//                e.printStackTrace();
            }
        }
        crawler.getCluster().killTopology("crawler");
        crawler.getCluster().shutdown();
        crawler.setCluster(null);
        
        System.out.println("Num of docs indexed: " + crawler.getNumIndexed());
        System.out.println("Num of docs in db: " + db.getCorpusSize());
        
        // shutdown dbs and pool
        db.close();
        
    }

}

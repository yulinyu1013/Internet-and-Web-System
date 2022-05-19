package edu.upenn.cis.cis455;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;


import edu.upenn.cis.cis455.crawler.Crawler;
import edu.upenn.cis.cis455.crawler.CrawlerFactory;
import edu.upenn.cis.cis455.storage.Storage;
import edu.upenn.cis.cis455.storage.StorageFactory;


public class CrawlerTestMarie  {

    
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
    	
    	
    	String startUrl = "https://crawltest.cis.upenn.edu/marie/";
    	int size = 1;
    	int count = 10;
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
                e.printStackTrace();
            }
        }
        
        crawler.getCluster().killTopology("crawler");
        crawler.getCluster().shutdown();
        crawler.setCluster(null);
        
        System.out.println("Num of docs indexed: " + crawler.getNumIndexed());
        System.out.println("Num of docs in db: " + db.getCorpusSize());
        

        db.close();

    }

}

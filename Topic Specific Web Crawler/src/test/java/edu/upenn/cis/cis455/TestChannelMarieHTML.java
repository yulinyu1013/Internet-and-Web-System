package edu.upenn.cis.cis455;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;

import org.apache.commons.io.FileUtils;

import edu.upenn.cis.cis455.crawler.Crawler;
import edu.upenn.cis.cis455.crawler.CrawlerFactory;
import edu.upenn.cis.cis455.storage.Channel;
import edu.upenn.cis.cis455.storage.Storage;
import edu.upenn.cis.cis455.storage.StorageFactory;
import edu.upenn.cis.cis455.storage.StorageInterface;

public class TestChannelMarieHTML {
public static void main(String args[]) {
    	
    	System.out.println("Crawler starting");
        String startUrl;
        String envPath = "./database";
        Integer size = 1;
        Integer count = 100;

        
    	try {
			FileUtils.cleanDirectory(new File(envPath));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}    
    	startUrl = "https://crawltest.cis.upenn.edu/marie/";

    	
        StorageInterface db = StorageFactory.getDatabaseInstance(envPath); 

        db.addUser("test1", "1");
        db.addUser("test2", "2");
        Channel newChannel = new Channel("marie", "/html/body/ul/li/a[text()=\"tpc-h\"]", "test1", new HashSet<String>());


		((Storage)db).getChannelApi().addChannel(newChannel);

		
        Crawler crawler = new Crawler(startUrl, db, size, count); 
        CrawlerFactory.setCrawler(crawler);
        crawler.start();
        
        System.out.println("Starting crawl of " + count + " documents, starting at " + startUrl);

        
        while(!crawler.isDone()) {
            try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
//				e.printStackTrace();
			}
        }
        
        crawler.getCluster().killTopology("crawler");
        crawler.getCluster().shutdown();
        crawler.setCluster(null);
        
        
        System.out.println("Num of docs indexed: " + crawler.getNumIndexed());
        System.out.println("Num of docs in db: " + db.getCorpusSize());
        System.out.println("Num docs in channel: " + ((Storage)db).getChannelApi().getChannel("marie").getDocUrls().size());
        
        // shutdown dbs
        System.out.println("closing db...");
        db.close();
        for(String url : crawler.getCrawledUrl()) {
        	System.out.println(url);
        }
        
        System.out.println("Done crawling!");
        
  
    }
}

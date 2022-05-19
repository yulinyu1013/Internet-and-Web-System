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

/**
 * Test Crawler and Channel Matching as a whole
 * */
public class TestChannelNYT {
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
//			e.printStackTrace();
		}    
    	startUrl = "https://crawltest.cis.upenn.edu/nytimes/";
    	
    	
        StorageInterface db = StorageFactory.getDatabaseInstance(envPath); 
 
        db.addUser("test1", "1");
        db.addUser("test2", "2");
	 
        Channel newChannel = new Channel("nyt", "/rss/channel/title", "test1", new HashSet<String>());
        Channel newChannel1 = new Channel("nyt1", "/rss/channel/title[contains(text(), \"Africa\")]", "test1", new HashSet<String>());
        Channel newChannel2 = new Channel("nyt2", "/rss/channel/title[contains(text(), \"Americas\")]", "test1", new HashSet<String>());
        Channel newChannel3 = new Channel("nyt3", "/rss/channel/title[contains(text(), \"Week in Review\")]", "test2", new HashSet<String>());
        Channel newChannel4 = new Channel("nyt4", "/rss/channel/title[text()=\"NYT > Science\"]", "test2", new HashSet<String>());
	    ((Storage)db).getChannelApi().addChannel(newChannel);
		((Storage)db).getChannelApi().addChannel(newChannel1);
		((Storage)db).getChannelApi().addChannel(newChannel2);
		((Storage)db).getChannelApi().addChannel(newChannel3);
		((Storage)db).getChannelApi().addChannel(newChannel4);
		
		
        Crawler crawler = new Crawler(startUrl, db, size, count); 
        CrawlerFactory.setCrawler(crawler);
        crawler.start();
        
        System.out.println("Starting crawl of " + count + " documents, starting at " + startUrl);

        
        while(!crawler.isDone()) {
            try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
//				e.printStackTrace();
			}
        }
        
        crawler.getCluster().killTopology("crawler");
        crawler.getCluster().shutdown();
        crawler.setCluster(null);
        
        
        System.out.println("Num of docs indexed: " + crawler.getNumIndexed());
        System.out.println("Num of docs in db: " + db.getCorpusSize());
	    System.out.println("Num docs in channel: " + ((Storage)db).getChannelApi().getChannel("nyt").getDocUrls().size());

        
        System.out.println("closing db...");
        db.close();
        for(String url : crawler.getCrawledUrl()) {
        	System.out.println(url);
        }
        
        System.out.println("Done crawling!");
        
  
	    }
}

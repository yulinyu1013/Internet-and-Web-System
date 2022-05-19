package edu.upenn.cis.cis455;

import static org.junit.Assert.assertFalse;

import org.junit.After;
import org.junit.Test;

import edu.upenn.cis.cis455.crawler.Crawler;
import edu.upenn.cis.cis455.crawler.CrawlerWorker;
import edu.upenn.cis.cis455.crawler.utils.HttpReqRes;
import junit.framework.TestCase;
 
 
public class CrawlerWorkerTest { 
	
	
	@Test
    public void testSizeAndType() {
		Crawler mockCrawler = new Crawler("",null,1, 1);
		CrawlerWorker worker = new CrawlerWorker(mockCrawler);
		HttpReqRes mockRes = new HttpReqRes(200, "text/plain", 2000000, null, null, "test");
		assertFalse(worker.isValidSize(mockRes));
		assertFalse(worker.isValidType(mockRes));
		
	}
	 
	
    @After
    public void tearDown() {
    	try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
//			e.printStackTrace();
		}
    }
	
	
} 
 
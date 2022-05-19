package edu.upenn.cis.cis455;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class RunAllTests extends TestCase  
{
  public static Test suite() 
  {
    try {
      Class[] testClasses = {
        /* TODO: Add the names of your unit test classes here */
        // Class.forName("your.class.name.here") 
//    		  Class.forName(StorageTest.class.getName()),
//    		  Class.forName(CrawlerWorkerTest.class.getName()),
//    		  Class.forName(CrawlerTestMax.class.getName()),
//    		  Class.forName(CrawlerTestMarie.class.getName()),
//    		  Class.forName(CrawlerTestExitEarly.class.getName())
    		  
    		  
      };   
      
      return new TestSuite(testClasses);
    } catch(Exception e){
      e.printStackTrace();
    } 
    
    return null;
  }
}

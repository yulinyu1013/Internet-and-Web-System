package edu.upenn.cis.cis455.m2.server;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.reset;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.upenn.cis.cis455.TestHelper;
import edu.upenn.cis.cis455.m1.handling.HttpIoHandler;
import edu.upenn.cis.cis455.m1.server.HttpRequest;
import edu.upenn.cis.cis455.m1.server.HttpResponse;
import edu.upenn.cis.cis455.m2.interfaces.HttpRoutes;

public class TestSplat {
	HttpRoutes routes;
	final static Logger logger = LogManager.getLogger(TestSplat.class);
	
	 @Before
	 public void setUp() {
        org.apache.logging.log4j.core.config.Configurator.setLevel("edu.upenn.cis.cis455", Level.DEBUG);
        routes = new HttpRoutes();
        
        routes.addRoute("GET", "/say/*/to/*", (req, res) -> {
//        	logger.info(req.splat().length);
        	for(String s : req.splat()) {
        		logger.info(s);
        	}
        	return "Number of splat parameters: " + req.splat().length;
        });
	 }
	 
	@Test
    public void testSplat1() throws IOException {
        String sampleGetRequest = 
                "GET /say/hello/to/world HTTP/1.1\r\n" +
                "User-Agent: Mozilla/4.0 (compatible; MSIE5.01; Windows NT)\r\n" +
                "Host: www.cis.upenn.edu\r\n" +
                "Accept-Language: en-us\r\n" +
                "Accept-Encoding: gzip, deflate\r\n" +
                "Connection: Keep-Alive\r\n\r\n";
        
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Socket s = TestHelper.getMockSocket(
            sampleGetRequest, 
            byteArrayOutputStream);
        
        logger.info("Parsing socket data to req...");
        HttpRequest req = HttpIoHandler.parseRequest(s);
        HttpResponse res = new HttpResponse();
         
        MockWorker.work(req, res, s, routes);
        
        String result = byteArrayOutputStream.toString("UTF-8").replace("\r", "");
        System.out.println(result);
        
        assertTrue(result.startsWith("HTTP/1.1 200"));
        assertTrue(req.splat()[0].equals("hello"));
        assertTrue(req.splat()[1].equals("world")); 
    }
	
	@Test
    public void testSplat2() throws IOException {
        String sampleGetRequest = 
                "GET /say/good%20luck/to/everyone/in/cis555 HTTP/1.1\r\n" +
                "User-Agent: Mozilla/4.0 (compatible; MSIE5.01; Windows NT)\r\n" +
                "Host: www.cis.upenn.edu\r\n" +
                "Accept-Language: en-us\r\n" +
                "Accept-Encoding: gzip, deflate\r\n" +
                "Connection: Keep-Alive\r\n\r\n";
        
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Socket s = TestHelper.getMockSocket(
            sampleGetRequest, 
            byteArrayOutputStream);
        
        logger.info("Parsing socket data to req...");
        HttpRequest req = HttpIoHandler.parseRequest(s);
        HttpResponse res = new HttpResponse();
         
        MockWorker.work(req, res, s, routes);
        
        String result = byteArrayOutputStream.toString("UTF-8").replace("\r", "");
        System.out.println(result);
        
        assertTrue(result.startsWith("HTTP/1.1 200"));
        assertTrue(req.splat()[0].equals("good luck"));
        assertTrue(req.splat()[1].equals("everyone/in/cis555")); 
    }
	
	
	
    @After
    public void tearDown() {
    	reset();
    }
}

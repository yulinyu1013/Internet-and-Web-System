package edu.upenn.cis.cis455.m2.server;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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
import static edu.upenn.cis.cis455.SparkController.*;

public class TestRequestWithRoute {
	HttpRoutes routes;
	final static Logger logger = LogManager.getLogger(TestRequestWithRoute.class);
	
    @Before
    public void setUp() {
        org.apache.logging.log4j.core.config.Configurator.setLevel("edu.upenn.cis.cis455", Level.DEBUG);
        routes = new HttpRoutes();
        
        routes.addFilter("BEFORE", "/*allpaths", (req, res) -> {
        	req.attribute("Before Attribute 1", "for all");
        	res.cookie("Before 1", "for all");
    
        });
        
        routes.addFilter("BEFORE", "/hello/*", (req, res) -> {
        	res.cookie("Before 2", "for hello");
    
        });
        
        routes.addRoute("HEAD", "/hello/:name", (req, res) -> {
        	res.body("Hello " + req.params("name"));
        	return null;
        });
        
        routes.addRoute("GET", "/hello/:name", (req, res) -> {
        	res.body("Hello " + req.params("name"));
        	return null;
        });
        
        //test if get the first route
        routes.addRoute("GET", "/hello/:name2", (req, res) -> {
        	res.body("Hello " + req.params("name2"));
        	return null;
        });
        
        routes.addRoute("GET", "/add/:x/:y", (req, res) -> {
        	return Integer.parseInt(req.params("x")) + Integer.parseInt(req.params("y"));
        });
        
        routes.addRoute("GET", "/mul/", (req, res) -> {
        	logger.info(req.queryParams("x"));
        	logger.info(req.queryString());
        			
        	req.queryParams("x");
        	return Integer.parseInt(req.queryParams("x")) * Integer.parseInt(req.queryParams("y"));
        });
        
        routes.addRoute("POST", "/testpost/", (req, res) -> {
        	return req.queryParams("field1") + req.queryParams("field2");
        });
        
        routes.addRoute("POST", "/testtext/", (req, res) -> {
        	return "text added: " + req.body();
        });
        
        routes.addRoute("PUT", "/file/*", (req, res) -> {
        	res.cookie("updated", "true");
        	return null;
        });
        
        routes.addRoute("DELETE", "/file/*", (req, res) -> {
        	return "file deleted!";
        });
        
        routes.addRoute("DELETE", "/halt", (req, res) -> {
        	halt();
        	return null;
        });
        
        routes.addRoute("OPTIONS", "/*", (req, res) -> {
        	res.header("Allow", "OPTIONS, GET, HEAD, POST, PUT, DELETE");
        	return null;
        });
        
        routes.addFilter("AFTER", "/mul/*", (req, res) -> {
        	res.cookie("After 1", "for mul");
    
        });
        
        routes.addFilter("AFTER", "/*allpaths", (req, res) -> {
        	res.cookie("After 2", "for all");
    
        });
    
    }
    
    /**
     * Test Hello Route
     * */
	@Test
    public void testGetHello() throws IOException {
        String sampleGetRequest = 
                "GET /hello/yulin HTTP/1.1\r\n" +
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
        assertTrue(res.body().equals("Hello yulin"));
        
    }
	
	@Test
    public void testHeadHello() throws IOException {
        String sampleGetRequest = 
                "HEAD /hello/yulin HTTP/1.1\r\n" +
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
        assertFalse(result.endsWith("Hello yulin"));
        
    }
	
	@Test
    public void testStaticFileNotFound() throws IOException {
        String sampleGetRequest = 
                "GET /404.jpg HTTP/1.1\r\n" +
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
        
        assertTrue(result.startsWith("HTTP/1.1 404"));
        
    }
	
	@Test
    public void testStaticFile501() throws IOException {
        String sampleGetRequest = 
                "POST /501.jpg HTTP/1.1\r\n" +
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
        
        assertTrue(result.startsWith("HTTP/1.1 501"));
        
    }
	
	
	@Test
    public void testAdd() throws IOException {
        String sampleGetRequest = 
                "GET /add/3/4 HTTP/1.1\r\n" +
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
        assertTrue(res.body().equals("7"));
    }
	
	
	@Test
    public void testGetMul() throws IOException {
        String sampleGetRequest = 
                "GET /mul/?x=3&y=4 HTTP/1.1\r\n" +
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
        assertTrue(res.body().equals("12"));
    }
	
	@Test
    public void testPostForm() throws IOException {
        String sampleGetRequest = 
                "POST /testpost/ HTTP/1.1\r\n" +
                "User-Agent: Mozilla/4.0 (compatible; MSIE5.01; Windows NT)\r\n" +
                "Host: www.cis.upenn.edu\r\n" +
                "Content-Type: application/x-www-form-urlencoded\r\n"+
                "Content-Length: 27\r\n"+
                "Accept-Language: en-us\r\n" +
                "Accept-Encoding: gzip, deflate\r\n" +
                "Connection: Keep-Alive\r\n\r\n"+
                "field1=value1&field2=value2";
        
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
        assertTrue(result.endsWith("value2"));
    }
	
	
	@Test
    public void testPostText() throws IOException {
        String sampleGetRequest = 
                "POST /testtext/ HTTP/1.1\r\n" +
                "User-Agent: Mozilla/4.0 (compatible; MSIE5.01; Windows NT)\r\n" +
                "Host: www.cis.upenn.edu\r\n" +
                "Content-Type: text/plain\r\n"+
                "Content-Length: 27\r\n"+
                "Accept-Language: en-us\r\n" +
                "Accept-Encoding: gzip, deflate\r\n" +
                "Connection: Keep-Alive\r\n\r\n"+
                "good job";
        
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
        assertTrue(result.endsWith(res.body()));
    }
	
	
	@Test
    public void testPut() throws IOException {
        String sampleGetRequest = 
                "PUT /file/new.html HTTP/1.1\r\n" +
                "User-Agent: Mozilla/4.0 (compatible; MSIE5.01; Windows NT)\r\n" +
                "Host: www.cis.upenn.edu\r\n" +
                "Content-Type: text/html\r\n"+
                "Content-Length: 16\r\n"+
                "Accept-Language: en-us\r\n" +
                "Accept-Encoding: gzip, deflate\r\n" +
                "Connection: Keep-Alive\r\n\r\n"+
                "<p>New File</p>";
        
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Socket s = TestHelper.getMockSocket(
            sampleGetRequest, 
            byteArrayOutputStream);
        
        logger.info("Parsing socket data to req...");
        HttpRequest req = HttpIoHandler.parseRequest(s);
        HttpResponse res = new HttpResponse();
        
        MockWorker.work(req, res, s, routes);
        
        String result = byteArrayOutputStream.toString("UTF-8").replace("\r", "");
        assertTrue(result.startsWith("HTTP/1.1 200"));
        assertNull(res.bodyRaw());

    }
	
	@Test
    public void testDelete() throws IOException {
        String sampleGetRequest = 
                "DELETE /file/new.html HTTP/1.1\r\n" +
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
        logger.info("method: "+req.requestMethod()); 
        
        String result = byteArrayOutputStream.toString("UTF-8").replace("\r", "");
        assertTrue(result.startsWith("HTTP/1.1 200"));
        assertTrue(result.endsWith("file deleted!"));
        assertNotNull(req.attribute("Before Attribute 1"));

    }
	
	@Test
    public void testOptions() throws IOException {
        String sampleGetRequest = 
                "OPTIONS /index.html HTTP/1.1\r\n" +
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
        logger.info("method: "+req.requestMethod()); 
        
        String result = byteArrayOutputStream.toString("UTF-8").replace("\r", "");
        System.out.println("result: \r\n" + result);
        assertTrue(result.startsWith("HTTP/1.1 200"));

    }
	
	
	@Test
    public void testHalt() throws IOException {
        String sampleGetRequest = 
                "DELETE /halt HTTP/1.1\r\n" +
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
        logger.info("method: "+req.requestMethod()); 
        
        String result = byteArrayOutputStream.toString("UTF-8").replace("\r", "");
        System.out.println("result: \r\n" + result);
        assertTrue(result.startsWith("HTTP/1.1 200"));
    }
    
    @After
    public void tearDown() {
    	reset();
    } 
    
	

}

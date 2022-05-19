package edu.upenn.cis.cis455.m1.server;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.reset;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;

import org.apache.logging.log4j.Level;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.upenn.cis.cis455.TestHelper;
import edu.upenn.cis.cis455.m1.handling.HttpIoHandler;

public class TestSendResponse {
	
	@Before
    public void setUp() {
        org.apache.logging.log4j.core.config.Configurator.setLevel("edu.upenn.cis.cis455", Level.DEBUG);
    }
	
    public HttpResponse getAndSetSampleRes() {
    	HttpResponse sampleRes = new HttpResponse();
		sampleRes.status(200);
		sampleRes.type("text/plain");
		sampleRes.body("This is a sample response body.");
		return sampleRes;
	}


	@Test
    public void testSendResponseGet() throws IOException {
        String sampleGetRequest = 
                "GET /index.html HTTP/1.1\r\n" +
                "User-Agent: Mozilla/4.0 (compatible; MSIE5.01; Windows NT)\r\n" +
                "Host: localhost:45555";
        
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Socket s = TestHelper.getMockSocket(
            sampleGetRequest, 
            byteArrayOutputStream);
        
        HttpRequest req = HttpIoHandler.parseRequest(s);
        HttpIoHandler.sendResponse(s, req, getAndSetSampleRes());
        String result = byteArrayOutputStream.toString("UTF-8").replace("\r", "");
        assertTrue(result.startsWith("HTTP/1.1 200"));
        assertTrue(result.endsWith("This is a sample response body."));
    }
    
    
    @Test
    public void testSendResponseHead() throws IOException {
        String sampleHeadRequest = 
                "HEAD /index.html HTTP/1.1\r\n" +
                "User-Agent: Mozilla/4.0 (compatible; MSIE5.01; Windows NT)\r\n" +
                "Host: localhost:45555";
        
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Socket s = TestHelper.getMockSocket(
            sampleHeadRequest, 
            byteArrayOutputStream);
        HttpRequest req = HttpIoHandler.parseRequest(s);
        HttpIoHandler.sendResponse(s, req, getAndSetSampleRes());
        String result = byteArrayOutputStream.toString("UTF-8").replace("\r", "");
        assertTrue(result.startsWith("HTTP/1.1 200"));
        assertFalse(result.endsWith("This is a sample response body."));

    }
    
    @After
    public void tearDown() {
    	reset();
    }

}

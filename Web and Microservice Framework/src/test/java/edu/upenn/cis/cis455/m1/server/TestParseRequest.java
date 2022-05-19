package edu.upenn.cis.cis455.m1.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;

import org.apache.logging.log4j.Level;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.upenn.cis.cis455.TestHelper;
import edu.upenn.cis.cis455.exceptions.HaltException;
import edu.upenn.cis.cis455.m1.handling.HttpIoHandler;
import static org.mockito.Mockito.*;

public class TestParseRequest {
    @Before
    public void setUp() {
        org.apache.logging.log4j.core.config.Configurator.setLevel("edu.upenn.cis.cis455", Level.DEBUG);
    }
    

    @Test
    public void testParseRequestFailed() throws IOException {
        String sampleGetRequest = 
                "GET /a/b/hello.htm?q=x&v=12%200\r\n" +
                "User-Agent: Mozilla/4.0 (compatible; MSIE5.01; Windows NT)\r\n" +
                "Host: www.cis.upenn.edu\r\n";
        
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Socket s = TestHelper.getMockSocket(
            sampleGetRequest, 
            byteArrayOutputStream); 
        
        try {
        	HttpIoHandler.parseRequest(s);
        } catch (HaltException e) {
        	 HttpIoHandler.sendException(s, new HttpResponse(), e, new HttpRequest());
        }
        String result = byteArrayOutputStream.toString("UTF-8").replace("\r", "");
        System.out.println(result);
        
        assertTrue(result.startsWith("HTTP/1.1 400"));
    }
    
    @Test
    public void testParseRequest() throws IOException {
        String sampleGetRequest = 
                "GET /index.html HTTP/1.1\r\n" +
                "User-Agent: Mozilla/4.0 (compatible; MSIE5.01; Windows NT)\r\n" +
                "Host: localhost:45555\r\n";
        
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Socket s = TestHelper.getMockSocket(
            sampleGetRequest, 
            byteArrayOutputStream);
        
        HttpRequest req = HttpIoHandler.parseRequest(s);

        
        assertEquals(req.host(), "localhost:45555");
        assertEquals(req.requestMethod(), "GET");
        assertEquals(req.pathInfo(), "/index.html");
    }
    
    @After
    public void tearDown() {
    	reset();
    }
}

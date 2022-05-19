package edu.upenn.cis.cis455.m1.server;

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
import edu.upenn.cis.cis455.exceptions.HaltException;
import edu.upenn.cis.cis455.m1.handling.HttpIoHandler;
import edu.upenn.cis.cis455.m1.handling.StaticFileHandler;

public class TestHandleRequest {
	
    @Before
    public void setUp() {
        org.apache.logging.log4j.core.config.Configurator.setLevel("edu.upenn.cis.cis455", Level.DEBUG);
    }
    
    @Test
    public void testHandleRequest() throws IOException {
        String sampleGetRequest = 
                "PUT /a/b/hello.htm?q=x&v=12%200 HTTP/1.1\r\n" +
                "User-Agent: Mozilla/4.0 (compatible; MSIE5.01; Windows NT)\r\n" +
                "Host: www.cis.upenn.edu\r\n" +
                "Accept-Language: en-us\r\n" +
                "Accept-Encoding: gzip, deflate\r\n" +
                "Cookie: name1=value1; name2=value2; name3=value3\r\n" +
                "Connection: Keep-Alive\r\n\r\n";
        
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Socket s = TestHelper.getMockSocket(
            sampleGetRequest, 
            byteArrayOutputStream);
        
        HttpRequest req = HttpIoHandler.parseRequest(s);
        HttpResponse res = new HttpResponse();
        try { 
        	StaticFileHandler.handleRequest(req, res, null);
        } catch (HaltException e) {
        	HttpIoHandler.sendException(s, res, e, req);
        }
        
        String result = byteArrayOutputStream.toString("UTF-8").replace("\r", "");
        System.out.println(result);
        
        assertTrue(result.startsWith("HTTP/1.1 501"));
    }
    
    @After
    public void tearDown() {
    	reset();
    }
}

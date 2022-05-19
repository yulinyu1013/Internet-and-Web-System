package edu.upenn.cis.cis455.m1.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.*;
import edu.upenn.cis.cis455.TestHelper;
import edu.upenn.cis.cis455.exceptions.HaltException;
import edu.upenn.cis.cis455.m1.handling.HttpIoHandler;

import org.apache.logging.log4j.Level;

public class TestSendException {
    @Before
    public void setUp() {
        org.apache.logging.log4j.core.config.Configurator.setLevel("edu.upenn.cis.cis455", Level.DEBUG);
    }
    
    
    @Test
    public void testSendException() throws IOException {
        String sampleGetRequest = 
                "GET /a/b/hello.htm?q=x&v=12%200 HTTP/1.1\r\n" +
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
        
        HaltException halt = new HaltException(404, "Not found");
        
        HttpIoHandler.sendException(s, new HttpResponse(), halt, new HttpRequest());
        String result = byteArrayOutputStream.toString("UTF-8").replace("\r", "");
        System.out.println(result);
        
        assertTrue(result.startsWith("HTTP/1.1 404"));
    }

    

    
    @After
    public void tearDown() {
    	reset();
    }
}

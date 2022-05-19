package edu.upenn.cis.cis455.m1.handling;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.exceptions.ClosedConnectionException;
//This needs to be caught by the request handler in the event of an error
import edu.upenn.cis.cis455.exceptions.HaltException;

import javax.servlet.http.HttpServletResponse;



/**
 * Header parsing help, largely derived from NanoHttpd, copyright notice above.
 * Feel free to modify as needed.
 */
public class HttpRequestParser {
    final static Logger logger = LogManager.getLogger(HttpRequestParser.class);
    
    /**
     * Initial fetch buffer for the HTTP request header
     *  
     */
    static final int BUFSIZE = 8192;
    
    
    /**
     * Decodes the sent headers and loads the data into Key/value pairs
     */
    public static void decodeHeader(BufferedReader in, Map<String, String> pre, Map<String, List<String>> queryParams,
                                    Map<String, String> headers) throws HaltException {
        try {
            // Read the request line
            String requestLine = in.readLine();
            if (requestLine == null) {
                return;
            }
            StringTokenizer st = new StringTokenizer(requestLine);

            // Get method
//            logger.info("Checking method...");
            if (!st.hasMoreTokens()) {
            	logger.info("Missing method...");
                throw new HaltException(HttpServletResponse.SC_BAD_REQUEST, "BAD REQUEST: Syntax error. Missing request method. Usage: GET /example/file.html HTTP/1.1");
            }
            
            String method = st.nextToken();
            if((!method.equals("GET"))&& (!method.equals("HEAD"))&& (!method.equals("POST"))&& (!method.equals("PUT"))&&(!method.equals("DELETE"))&& (!method.equals("OPTIONS"))) {
            	logger.info("Not Implemented...");
            	throw new HaltException(501);
            }
            pre.put("method", method);

            // Get URI and params
            if (!st.hasMoreTokens()) {
            	logger.info("Missing uri...");
                throw new HaltException(HttpServletResponse.SC_BAD_REQUEST, "BAD REQUEST: Missing URI. Usage: GET /example/file.html HTTP/1.1");
            }
            String uri = st.nextToken();
            
            //check absolute url
            if(uri.startsWith("http")) {
            	try {
                    new URL(uri).toURI();
                }
                catch (Exception e) {
                    throw new HaltException(400, "invalid url");
                }
            }
            int qmi = uri.indexOf('?');
            if (qmi >= 0) {
                pre.put("queryString", uri.substring(qmi + 1));
                pre.put("uri",decodePercent(uri.substring(0, qmi)));
                decodeParms(uri.substring(qmi + 1), queryParams);
            } else {
                pre.put("queryString", "");
                pre.put("uri",decodePercent(uri));
            }

            // Get HTTP protocol
            // NOTE: this now forces header names lower case since they are case insensitive and vary by client.
            logger.info("Checking protocol...");
            if (!st.hasMoreTokens()) {
                throw new HaltException(HttpServletResponse.SC_BAD_REQUEST, "BAD REQUEST: Missing Protocal. Usage: GET /example/file.html HTTP/1.1");
            }
            
            String protocol = st.nextToken();
            
            //handle lowercase
            if( !protocol.toLowerCase().startsWith("http") || protocol.equals("http/1.1") || protocol.equals("http/1.0")) {
            	throw new HaltException(400);
            }
            if(!protocol.equals("HTTP/1.1") && !protocol.equals("HTTP/1.0")) {
            	throw new HaltException(505, "HTTP Version Not Supported");
            }
            
            pre.put("protocolVersion", protocol);

            // Get Headers
            String lastKey = null;
            String line = in.readLine();
            while (line != null && !line.trim().isEmpty()) {
                int p = line.indexOf(':');
                if (p >= 0) {
                	String header = line.substring(0, p).trim().toLowerCase(Locale.US);
	            	String value = line.substring(p + 1).trim();
	            	headers.put(header, value);
	            	lastKey = header;
                } else if (lastKey != null && line.startsWith(" ") || line.startsWith("\t")) {
                    String newPart = line.trim();
                    headers.put(lastKey, headers.get(lastKey) + newPart);
                }
                line = in.readLine();
            }
            
            //handle missing host
    		if(!headers.containsKey("host")) {
    			throw new HaltException(400, "Missing host.");	
    		}
            
            
        } catch (IOException ioe) {
            throw new HaltException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage());
        }
    }
    
    /**
     * Decodes parameters in percent-encoded URI-format ( e.g.
     * "name=Jack%20Daniels&pass=Single%20Malt" ) and adds them to given Map.
     */
    public static String decodeParms(String queryParams, Map<String, List<String>> p) {
        logger.info("decoding query params...");
    	String queryParameterString = "";
        
        if (queryParams == null) {
            return queryParameterString;
        }

        queryParameterString = queryParams;
        StringTokenizer st = new StringTokenizer(queryParams, "&");
        while (st.hasMoreTokens()) {
            String e = st.nextToken();
            int sep = e.indexOf('=');
            String key = null;
            String value = null;

            if (sep >= 0) {
                key = decodePercent(e.substring(0, sep)).trim();
                value = decodePercent(e.substring(sep + 1));
                logger.info(key+": "+value);
            } else {
                key = decodePercent(e).trim();
                value = "";
            }

            List<String> values = p.get(key);
            if (values == null) {
                values = new ArrayList<String>();
                p.put(key, values);
            }

            values.add(value);
        }
        
        return queryParameterString;
    }


    /**
     * Decode percent encoded <code>String</code> values.
     * 
     * @param str
     *            the percent encoded <code>String</code>
     * @return expanded form of the input, for example "foo%20bar" becomes
     *         "foo bar"
     */
    public static String decodePercent(String str) {
        String decoded = null;
        try {
//        	logger.info(str.replaceAll("%20", " "));
//        	str = str.replaceAll("%20", " ");
            decoded = URLDecoder.decode(str, "UTF8");
        } catch (UnsupportedEncodingException ignored) {
            logger.warn("Encoding not supported, ignored", ignored);
        }
        return decoded;
    }
    
    
    /**
     * Decodes the sent body and loads the data into string
     */
    public static String decodeBody(BufferedReader in, Map<String, String> headers) throws IOException { //read message body in bytes
		logger.info("Parsing body...");
		if(headers.containsKey("content-length")){
			logger.info("content length: "+ headers.get("content-length"));
			int numBytes = Integer.parseInt(headers.get("content-length"));
			char[] bytes = new char[numBytes];
			
			int bytesRead = in.read(bytes, 0, numBytes);
			if(numBytes != bytesRead){
				logger.info("Read number of bytes != Content Length");
			}
			String body = new String(bytes);
			
			return body;
			
		}
		return null;
	}
    
    /**
     * Parse the initial request header
     * 
     * @param remoteIp IP address of client
     * @param inputStream Socket input stream (not yet read)
     * @param headers Map to receive header key/values
     * @param queryParams Map to receive parameter key/value-lists
     */
    public static String parseRequest(
                        String remoteIp, 
                        InputStream inputStream, 
                        Map<String, String> pre,
                        Map<String, String> headers,
                        Map<String, List<String>> queryParams) throws IOException, HaltException {
        int splitbyte = 0;
        int rlen = 0;
//        String uri = "";
        
        try {
            // Read the first 8192 bytes.
            // The full header should fit in here.
            // Apache's default header limit is 8KB.
            // Do NOT assume that a single read will get the entire header
            // at once!
            byte[] buf = new byte[BUFSIZE];
            splitbyte = 0;
            rlen = 0;

            int read = -1;
            inputStream.mark(BUFSIZE);
            try {
                read = inputStream.read(buf, 0, BUFSIZE);
            } catch (IOException e) {
                throw new ClosedConnectionException();
            }
            if (read == -1) {
                throw new HaltException(HttpServletResponse.SC_BAD_REQUEST);
            }
            while (read > 0) {
                rlen += read;
                splitbyte = findHeaderEnd(buf, rlen);
                if (splitbyte > 0) {
                    break;
                }
                read = inputStream.read(buf, rlen, BUFSIZE - rlen);
            }

            if (splitbyte < rlen) {
                inputStream.reset();
                inputStream.skip(splitbyte);
            }

            headers.clear();

            // Create a BufferedReader for parsing the header.
            BufferedReader hin = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(buf, 0, rlen)));

            decodeHeader(hin, pre, queryParams, headers);

            if (null != remoteIp) {
                headers.put("remote-addr", remoteIp);
                headers.put("http-client-ip", remoteIp);
            }
            
            String body = decodeBody(hin, headers);
            logger.info("body: "+ body);
            logger.info("content type: "+ headers.get("content-type"));
          //handle application/x-www-form-urlencoded
	    	if((headers.containsKey("content-type")) && (headers.get("content-type").equals("application/x-www-form-urlencoded"))) {
	    		logger.info("Parsing query params from body...");
	    		HttpRequestParser.decodeParms(body, queryParams);
	    	}
	    	
	    	return body;
            

        } catch (SocketException e) {
            // throw it out to close socket object (finalAccept)
            throw new ClosedConnectionException();
        } catch (SocketTimeoutException ste) {
            // treat socket timeouts the same way we treat socket exceptions
            // i.e. close the stream & finalAccept object by throwing the
            // exception up the call stack.
            throw new ClosedConnectionException();
        }
        
    }
    
    /**
     * Find byte index separating header from body. It must be the last byte of
     * the first two sequential new lines.
     */
    static int findHeaderEnd(final byte[] buf, int rlen) {
        int splitbyte = 0;
        while (splitbyte + 1 < rlen) {

            // RFC2616
            if (buf[splitbyte] == '\r' && buf[splitbyte + 1] == '\n' && splitbyte + 3 < rlen && buf[splitbyte + 2] == '\r' && buf[splitbyte + 3] == '\n') {
                return splitbyte + 4;
            }

            // tolerance
            if (buf[splitbyte] == '\n' && buf[splitbyte + 1] == '\n') {
                return splitbyte + 2;
            }
            splitbyte++;
        }
        return 0;
    }

}

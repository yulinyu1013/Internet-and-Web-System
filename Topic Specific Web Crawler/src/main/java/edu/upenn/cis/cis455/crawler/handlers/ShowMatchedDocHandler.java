package edu.upenn.cis.cis455.crawler.handlers;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Set;

import edu.upenn.cis.cis455.crawler.utils.URLInfo;
import edu.upenn.cis.cis455.storage.Channel;
import edu.upenn.cis.cis455.storage.Content;
import edu.upenn.cis.cis455.storage.Storage;
import edu.upenn.cis.cis455.storage.StorageInterface;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Spark;

public class ShowMatchedDocHandler implements Route {
	Storage db;
	

	public ShowMatchedDocHandler(StorageInterface db) {
		this.db = (Storage) db;
	}


	@Override
	public Object handle(Request req, Response res) throws Exception {
		StringBuilder sb = new StringBuilder();
        sb.append("<html><body>");
       
        
        String name = req.queryParams("channel");
        
        sb.append("<h1>Matched Contents for Channel </h1>");
        
        Channel channel = db.getChannelApi().getChannel(name);
        
        if(channel == null) {
        	Spark.halt(404, "Channel not found!");
        	return null;
        }
        
        sb.append("<div class=\"channelheader\">");
        sb.append("<h3>Channel Name: " + channel.getName() + "</h3>\r\n");
        sb.append("<h3>created by: " + channel.getUsername() + "</h3>\r\n");
        Set<String> docUrls = channel.getDocUrls();
        if(docUrls.size()==0) {
        	sb.append("No matches found for this channel. Try to crawl again!");
        } else {
        	
   
            for(String url : docUrls) {
            	
                
                
                Content doc = db.getDocument(new URLInfo(url).toString());
                String lastCrawled = doc.getLastChecked().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
//                sb.append("<h4>Crawled Document</h4>");
                sb.append("<div>Crawled on: " + lastCrawled + "</div>\r\n");
                sb.append("<div>Location: " +doc.getUrl()+"</div>\r\n"); 
                sb.append("	<p>");
                sb.append("		<div class=\"document\">");
                sb.append(doc.getContent());
                sb.append("		</div>");
                sb.append("	</p>");
                
                
            }
        }
        sb.append("</div>");
  
        sb.append("</body></html>");
        res.type("text/html");
		return sb.toString();
	}

}

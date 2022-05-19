package edu.upenn.cis.cis455.crawler.handlers;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.storage.Channel;
import edu.upenn.cis.cis455.storage.Storage;
import edu.upenn.cis.cis455.storage.StorageInterface;
import spark.Request;
import spark.Response;
import spark.Route;

public class LoggedInMainPageHandler implements Route {
	final static Logger logger = LogManager.getLogger(LoggedInMainPageHandler.class);
	Storage db;
	
	public LoggedInMainPageHandler(StorageInterface db) {
		this.db = (Storage) db;
	}

	@Override
	public Object handle(Request req, Response res) throws Exception {
		StringBuilder sb = new StringBuilder();
        sb.append("<html><body>");
        sb.append("<h1>Welcome, " + req.session().attribute("user")+"</h1>");
        
        logger.info("getting all channels from db...");
        List<Channel> channels = db.getChannelApi().getAllChannels();
        logger.info("Channel size: "+ channels.size());
        sb.append("<h5>All Channels</h5>");
        sb.append("<ul>");
        for(Channel channel: channels) {
        	sb.append("<li><a href=\"/show?channel="+ channel.getName()+ "\">"+channel.getName()+"</a></li>");
        }
        sb.append("</ul>");
  
        sb.append("<a href=\"/logout\">Logout</a>");
        sb.append("</body></html>");
        res.type("text/html");
		return sb.toString();
	}

}

package edu.upenn.cis.cis455.crawler.handlers;



import java.util.HashSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.storage.Channel;
import edu.upenn.cis.cis455.storage.Storage;
import edu.upenn.cis.cis455.storage.StorageInterface;
import edu.upenn.cis.cis455.storage.User;
import edu.upenn.cis.cis455.xpathengine.XPathEngineImpl;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Spark;

public class CreateXPathHandler implements Route {
	final static Logger logger = LogManager.getLogger(CreateXPathHandler.class);
	Storage db;
	
	public CreateXPathHandler(StorageInterface db) {
		this.db = (Storage) db;
	}

	@Override
	public Object handle(Request req, Response res) throws Exception {
		if(req.params().size()>1) Spark.halt(400, "bad request");
		String name = req.params("name");
		logger.info("checking channel db...");
		Channel channel = null;
		try {
			channel = db.getChannelApi().getChannel(name);
		} catch (Exception e){
			
		}
		
		if(channel != null) {
			logger.info("channel found in db");
			Spark.halt(409, "Channel already exists!");
			return null;
		}
		String xpath = req.queryParams("xpath");
		
		if(xpath==null || !(new XPathEngineImpl().isValid(xpath))) {
			Spark.halt(400, "invalid xpath");
			return null;
		}
		
		
		logger.info("not found; creating new channel...");
		logger.info("getting user...");
		User user = db.getUserApi().getUser(req.session().attribute("user"));
		logger.info("generating channel...");
		Channel newChannel = new Channel(name, req.queryParams("xpath"), user.getUsername(), new HashSet<String>());
		logger.info("adding channel to db...");
		db.getChannelApi().addChannel(newChannel);
		logger.info("channel added...");
		res.type("text/html");
		res.body("<html><body> Successfully created channel: "+ name +" </body></html>");
		return res.body();
	}

}

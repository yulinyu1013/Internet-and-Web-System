package edu.upenn.cis.cis455.crawler.handlers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.crawler.utils.URLInfo;
import edu.upenn.cis.cis455.storage.Content;
import edu.upenn.cis.cis455.storage.StorageInterface;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Spark;

public class DocLookupHandler implements Route {
	final static Logger logger = LogManager.getLogger(DocLookupHandler.class);
	StorageInterface db;
	
	public DocLookupHandler(StorageInterface db) {
		this.db = db;
	}

	@Override
	public Object handle(Request req, Response res) throws Exception {
		String url = new URLInfo(req.queryParams("url")).toString();
		logger.info("Request url: "+ url);
		Content doc = db.getDocument(url);
		
		if(doc == null) {
			logger.info("Content not found");
			Spark.halt(404, "Not Found");
			return null;
		} 
		
		res.status(200);
		res.type(doc.getType());
		return doc.getContent();
	}

}

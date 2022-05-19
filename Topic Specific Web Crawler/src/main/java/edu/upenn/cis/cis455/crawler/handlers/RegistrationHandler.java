package edu.upenn.cis.cis455.crawler.handlers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.storage.StorageInterface;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.HaltException;
import spark.Spark;



public class RegistrationHandler implements Route {
	final static Logger logger = LogManager.getLogger(RegistrationHandler.class);
    StorageInterface db;

    public RegistrationHandler(StorageInterface db) {
        this.db = db;
    }

	@Override
	public Object handle(Request req, Response res) throws HaltException {
        String user = req.queryParams("username");
        String pass = req.queryParams("password");
        
        boolean added = db.addUser(user, pass);
        
        if (!added) {
        	Spark.halt(409, "<html><body>User already exists. <a href=\"/register.html\">Back to Register</a></body></html>");
        	return null;
        	
        }
        logger.info("Successfully add a new user!");
        
        res.status(200);
        res.type("text/html");
    	StringBuilder sb = new StringBuilder();
        sb.append("<html><body>");
        sb.append("<h1>Registration Success!</h1>");
        sb.append("<a href=\"/login-form.html\">Back to Login</a>");
        sb.append("</body></html>");
//        res.body(sb.toString());
        return sb.toString();
        
        
        
	}

}

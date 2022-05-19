package edu.upenn.cis.cis455.crawler.handlers;

import spark.Request;
import spark.Route;
import spark.Response;
import spark.HaltException;
import spark.Session;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.storage.StorageInterface;

public class LoginHandler implements Route {
	final static Logger logger = LogManager.getLogger(LoginHandler.class);

	private StorageInterface db;
    public LoginHandler(StorageInterface db) {
        this.db = db;
    }

    @Override
    public String handle(Request req, Response resp) throws HaltException {
        String user = req.queryParams("username");
        String pass = req.queryParams("password");

        logger.info("Login request for " + user + " and " + pass);
        if (db.getSessionForUser(user, pass)) {
            logger.info("Logged in!");
            Session session = req.session();
            session.maxInactiveInterval(5*60);
            session.attribute("user", user);
            session.attribute("password", pass);
            resp.redirect("/index.html");
        } else {
            logger.info("Invalid credentials");
            resp.redirect("/login-form.html");
        }

        return "";
    }
}

package edu.upenn.cis.cis455.crawler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;

import static spark.Spark.*;
import edu.upenn.cis.cis455.crawler.handlers.LoginFilter;
import edu.upenn.cis.cis455.storage.StorageFactory;
import edu.upenn.cis.cis455.storage.StorageInterface;
import edu.upenn.cis.cis455.crawler.handlers.LoginHandler;
import edu.upenn.cis.cis455.crawler.handlers.RegistrationHandler;
import edu.upenn.cis.cis455.crawler.handlers.ShowMatchedDocHandler;
import edu.upenn.cis.cis455.crawler.handlers.CreateXPathHandler;
import edu.upenn.cis.cis455.crawler.handlers.DocLookupHandler;
import edu.upenn.cis.cis455.crawler.handlers.LoggedInMainPageHandler;


public class WebInterface {
	final static Logger logger = (Logger) LogManager.getLogger(WebInterface.class);
	
	
    public static void main(String args[]) {
    	org.apache.logging.log4j.core.config.Configurator.setLevel("web", Level.DEBUG);
    	org.apache.logging.log4j.core.config.Configurator.setLevel("org", Level.OFF);
    	LogManager.getLogger(WebInterface.class);
        if (args.length < 1 || args.length > 2) {
        	logger.info("Syntax: WebInterface {path} {root}");
            System.exit(1);
        }
        
        // create a data storage directory if it does not already exist.
        if (!Files.exists(Paths.get(args[0]))) {
            try {
            	logger.info("Create new directory...");
                Files.createDirectory(Paths.get(args[0]));
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }else { // for testing purpose
//        	try {
//				FileUtils.cleanDirectory(new File(args[0]));
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
        }

        port(45555);
        logger.info("Connect to database...");
        StorageInterface database = StorageFactory.getDatabaseInstance(args[0]);
        logger.info("Num of docs: "+ database.getCorpusSize());
        LoginFilter testIfLoggedIn = new LoginFilter(database);

        if (args.length == 2) {
            staticFiles.externalLocation(args[1]);
            staticFileLocation(args[1]);
        }


        before("/*", "*/*", testIfLoggedIn);
        
        post("/register", new RegistrationHandler(database));
        
        
        post("/login", new LoginHandler(database));
       
        
        get("/logout", (req, res) -> {
        	req.session().invalidate();
        	res.redirect("/login-form.html");
        	return "";
        });
        
        get("/index.html", new LoggedInMainPageHandler(database));
        
        get("/lookup", new DocLookupHandler(database));
        
        get("/", new LoggedInMainPageHandler(database));
        
        get("/create/:name", new CreateXPathHandler(database));
        
        get("/show", new ShowMatchedDocHandler(database));

        awaitInitialization();
    }
}

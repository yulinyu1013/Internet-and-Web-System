package edu.upenn.cis.cis455;

import static edu.upenn.cis.cis455.SparkController.*;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.m2.interfaces.HttpSession;


/**
 * Initialization / skeleton class.
 * Note that this should set up a basic web server for Milestone 1.
 * For Milestone 2 you can use this to set up a basic server.
 * 
 * CAUTION - ASSUME WE WILL REPLACE THIS WHEN WE TEST MILESTONE 2,
 * SO ALL OF YOUR METHODS SHOULD USE THE STANDARD INTERFACES.
 * 
 * @author zives
 *
 */
public class WebServer {
	final static Logger logger = LogManager.getLogger(WebServer.class);
    public static void main(String[] args) {
        org.apache.logging.log4j.core.config.Configurator.setLevel("edu.upenn.cis.cis455", Level.DEBUG);

        // TODO: make sure you parse *BOTH* command line arguments properly
        logger.info("Parsing parameters command line...");
        
        if(args.length == 2) {
        	port(Integer.parseInt(args[0]));
        	staticFileLocation(args[1]);
        } 
        
        // All user routes should go below here...
        
        //Test set 1
//    	before((req, res) -> res.cookie("before", "forall"));
//    	
//    	get("/testbeforefilter", (req, res) ->  halt());
//    	
//    	get("/session", (req, res) -> {
//    		req.session().maxInactiveInterval(5);
//    		return "a session has been created.";
//    	});
//    	
//    	before("/login", (req, res) -> {
//    		req.session(true);
//    		res.cookie("before", "login");
//    		System.out.println(req.session(false).id());
//    		System.out.println(((HttpSession) req.session(false)).isNew());
//    	});
//    	
    	get("/login", (req, res) -> {
    		req.session().maxInactiveInterval(5);
    		System.out.println(req.session(false).id());
    		System.out.println(((HttpSession) req.session(false)).isNew());
    		return "login.";
    	});
//    	
//    	get("/hello", (req, res) -> {
//    		req.session(false);
//    		return "hello world!";
//    	});
//    	
//      	get("/redirect", (req, res)->{
//    		res.redirect("/hello");
//    		return null;
//    	});
//    	
//    	get("/add/:x/:y", (req, res) -> {
//    		return Integer.parseInt(req.params("x")) + Integer.parseInt(req.params("y"));
//    	});
//    	
//    	get("/mul", (req, res) -> {
//    		return Integer.parseInt(req.queryParams("x")) * Integer.parseInt(req.queryParams("y"));
//    	});
//    	
//    	get("/halt", (req, res)->{
//    		req.session();
//    		halt(404, "test cookie!");
//    		return null;
//    	});
//    	
//    	after((req, res) -> {
//    		res.cookie("after", "forall");
//    
//    	});
        
        
        // test set 2
//        before((req, res) -> {
//        	req.attribute("Before Attribute 1", "for all");
//        	res.cookie("Before 1", "for all");
//        });
//        
//        before("/hello/*", (req, res) -> {
//        	res.cookie("Before 2", "for hello");
//    
//        });
//        
//        head("/hello/:name", (req, res) -> {
//        	res.body("Hello " + req.params("name"));
//        	return null;
//        });
//        
//        get("/hello/:name", (req, res) -> {
//        	res.body("Hello " + req.params("name"));
//        	return null;
//        });
//        
//        get("/hello/:name2", (req, res) -> {
//        	res.body("Hello " + req.params("name2"));
//        	return null;
//        });
//        
//        get("/add/:x/:y", (req, res) -> {
//        	return Integer.parseInt(req.params("x")) + Integer.parseInt(req.params("y"));
//        });
//        
//        get( "/mul/", (req, res) -> {
//        	logger.info(req.queryParams("x"));
//        	logger.info(req.queryString());
//        			
//        	return Integer.parseInt(req.queryParams("x")) * Integer.parseInt(req.queryParams("y"));
//        });
//        
//        post("/testpost/", (req, res) -> {
//        	return req.queryParams("field1") + req.queryParams("field2");
//        });
//        
//        post("/testtext/", (req, res) -> {
//        	return "text added: " + req.body();
//        });
//        
//        put("/file/*", (req, res) -> {
//        	res.cookie("updated", "true");
//        	return null;
//        });
//        
//        delete("/file/*", (req, res) -> {
//        	return "file deleted!";
//        });
//        
//        delete("/halt", (req, res) -> {
//        	halt();
//        	return null;
//        });
//        
//        options("/*", (req, res) -> {
//        	res.header("Allow", "OPTIONS, GET, HEAD, POST, PUT, DELETE");
//        	return null;
//        });
//        
//        after("/mul/*", (req, res) -> {
//        	res.cookie("After 1", "for mul");
//    
//        });
//        
//        after((req, res) -> {
//        	res.cookie("After 2", "for all");
//        });
        
        

        // ... and above here. Leave this comment for the Spark comparator tool

        System.out.println("Waiting to handle requests!");
        logger.info("Initializing main webserver...");
        awaitInitialization();
        logger.info("Done. Ready to handle request...");
    }
}

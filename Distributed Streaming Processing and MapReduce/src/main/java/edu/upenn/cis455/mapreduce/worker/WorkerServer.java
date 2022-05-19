package edu.upenn.cis455.mapreduce.worker;

import static spark.Spark.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.upenn.cis.stormlite.Config;
import edu.upenn.cis.stormlite.DistributedCluster;
import edu.upenn.cis.stormlite.TopologyContext;
import edu.upenn.cis.stormlite.distributed.WorkerHelper;
import edu.upenn.cis.stormlite.distributed.WorkerJob;
import edu.upenn.cis.stormlite.routers.StreamRouter;
import edu.upenn.cis.stormlite.tuple.Tuple;
import edu.upenn.cis455.mapreduce.RunJobRoute;
import spark.Spark;

/**
 * Simple listener for worker creation 
 * 
 * @author zives
 *
 */
public class WorkerServer {
    static Logger log = LogManager.getLogger(WorkerServer.class);

    public static DistributedCluster cluster = new DistributedCluster();

    List<TopologyContext> contexts = new ArrayList<>(); 

    static List<String> topologies = new ArrayList<>();
    
    public static Config config = new Config();
    
    public WorkerJob workerJob;
    
    public static volatile boolean shutdown = false;
    
    public static int myPort;
    
    public static Config getConfig() {
		return config;
	}

	public WorkerServer(int myPort) throws MalformedURLException {

        log.info("Creating server listener at socket " + myPort);

        port(myPort);
        final ObjectMapper om = new ObjectMapper();
        om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
       
        Spark.post("/definejob", (req, res) -> {

            try {
                workerJob = om.readValue(req.body(), WorkerJob.class); // deserialize

                try {
                    log.info("Processing job definition request " + workerJob.getConfig().get("job") +
                            " on machine " + workerJob.getConfig().get("workerIndex")); 

                    contexts.add(cluster.submitTopology(workerJob.getConfig().get("job"), workerJob.getConfig(), 
                            workerJob.getTopology())); 
                    
                    // Add a new topology
                    synchronized (topologies) {
                        topologies.add(workerJob.getConfig().get("job"));
                    }
                } catch (ClassNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                return "Job launched";
            } catch (IOException e) {
                e.printStackTrace();

                // Internal server error
                res.status(500);
                return e.getMessage();
            } 

        });

        Spark.post("/runjob", new RunJobRoute(cluster)); // start the cluster

        Spark.post("/pushdata/:stream", (req, res) -> {
            try {
                String stream = req.params(":stream");
                log.debug("Worker received: " + req.body());
                Tuple tuple = om.readValue(req.body(), Tuple.class);

                log.debug("Worker received: " + tuple + " for " + stream);

                // Find the destination stream and route to it
                StreamRouter router = cluster.getStreamRouter(stream);

                if (contexts.isEmpty())
                    log.error("No topology context -- were we initialized??");

                TopologyContext ourContext = contexts.get(contexts.size() - 1);

                // Instrumentation for tracking progress
                if (!tuple.isEndOfStream())
                    ourContext.incSendOutputs(router.getKey(tuple.getValues()));
                
                // handle tuple vs end of stream for our *local nodes only*
                // Please look at StreamRouter and its methods (execute, executeEndOfStream, executeLocally, executeEndOfStreamLocally)
                if(tuple.isEndOfStream()) {
                	router.executeEndOfStreamLocally(ourContext, tuple.getSourceExecutor());
                }else {
                	router.executeLocally(tuple, ourContext, tuple.getSourceExecutor());
                }
                return "OK";
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();

                res.status(500);
                return e.getMessage();
            }

        });
        
        Spark.get("/shutdown", (req, res) -> {
        	log.info("Got shutdown request from master; shutting down...");
        	WorkerServer.shutdown();
        	res.type("text/html");
        	return "Worker has successfully shut down."; 
        });
        

    }
    
    /**
     * The following methods are for worker status
     */
	private String getWorkerState() {
		
		if(contexts.size() == 0) return "INIT";
		
		TopologyContext ourContext = contexts.get(contexts.size() - 1);
		
		if (ourContext.getState() == TopologyContext.STATE.INIT) {
			return "INIT";
		}
		if(ourContext.getState() == TopologyContext.STATE.MAPPING) {
			return "MAPPING";
		}
		if(ourContext.getState() == TopologyContext.STATE.REDUCING) {
			return "REDUCING";
		}
		if(ourContext.getState() == TopologyContext.STATE.IDLE) {
			return "IDLE";
		}
		 
		return null;
	}
	
	
	private int getKeysRead() {
		if(contexts.size() == 0) return 0;
		
		TopologyContext ourContext = contexts.get(contexts.size() - 1);
		
		if(ourContext == null) return 0;

		return ourContext.getKeysRead().get();
	}

	private int getKeysWritten() {
		if(contexts.size() == 0) return 0;
		
		TopologyContext ourContext = contexts.get(contexts.size() - 1);
		
		if(ourContext == null) return 0;
		
		return ourContext.getKeysWritten().get();
	}

	
	private String getResults() {
		if(contexts.size() == 0) return "[]";
		TopologyContext ourContext = contexts.get(contexts.size() - 1);
		
		if(ourContext == null) return "[]";
		ArrayList<String> results = ourContext.getResults();
		
		synchronized (results) {
			return results.toString();
		}
	}

    public static void createWorker(Map<String, String> config) {
        if (!config.containsKey("workerList"))
            throw new RuntimeException("Worker spout doesn't have list of worker IP addresses/ports");

        if (!config.containsKey("workerIndex"))
            throw new RuntimeException("Worker spout doesn't know its worker ID");
        else {
            String[] addresses = WorkerHelper.getWorkers(config);
            String myAddress = addresses[Integer.valueOf(config.get("workerIndex"))];

            log.debug("Initializing worker " + myAddress);

            URL url;
            try {
                url = new URL(myAddress);

                new WorkerServer(url.getPort());
            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public static void shutdown() {
        
        String strURL = "http://" + config.get("masterIpPort") + "/workershutdown?port="+myPort;
		URL url = null;
		try {
			url = new URL(strURL);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("GET");
			if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
				log.debug("Inform Shutdown Response Not OK : " + conn.getResponseCode());
			}
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		shutdown = true;
    }
    
    

    /**
     * Simple launch for worker server.  Note that you may want to change / replace
     * most of this.
     * 
     * @param args
     * @throws MalformedURLException
     */
    public static void main(String args[]) throws MalformedURLException {
    	org.apache.logging.log4j.core.config.Configurator.setLevel("web", Level.DEBUG);
    	org.apache.logging.log4j.core.config.Configurator.setLevel("org.eclipse.jetty", Level.ERROR);
        if (args.length < 3) {
            System.out.println("Usage: WorkerServer [port number] [master host/IP]:[master port] [storage directory]");
            System.exit(1);
        }

        myPort = Integer.valueOf(args[0]);

        log.info("Worker node startup, on port " + myPort);

        WorkerServer worker = new WorkerServer(myPort);
        
        // you may want to adapt parts of edu.upenn.cis.stormlite.mapreduce.TestMapReduce
        // here
        
        config.put("workerList", "[127.0.0.1:"+args[0]+"]");
        config.put("masterIpPort", args[1]);
        config.put("storageDir", args[2]); 
        config.put("workerIndex", "0");
        
        // periodic thread to check worker status
        
        Runnable statusCheck = () -> {
        	
        	while(!WorkerServer.shutdown) {
        		log.info("Updating status to master...");
            	StringBuilder sb = new StringBuilder();
            	sb.append("port=" + myPort);
            	sb.append("&status=" + worker.getWorkerState());
            	sb.append("&job="+(worker.workerJob==null ? "None" : worker.workerJob.getConfig().get("job"))); 
            	sb.append("&keysRead=" + worker.getKeysRead()); 
            	sb.append("&keysWritten=" + worker.getKeysWritten());
            	try {
    				sb.append("&results=" + URLEncoder.encode(worker.getResults(), "UTF-8"));
    			} catch (UnsupportedEncodingException e1) {
    				// TODO Auto-generated catch block
    				e1.printStackTrace();
    			}

            	String strUrl = "http://" + args[1] + "/workerstatus?" + sb.toString();
    			try {
    				URL url = new URL(strUrl);
    				System.out.println(url.toString());
    	        	HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    				conn.setDoOutput(true);
    				conn.setRequestMethod("GET");
    				
    				if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
    					log.info("Worker Status sent to master");
    				}
    				
    				try {
    					Thread.sleep(10000);
    				} catch (InterruptedException e) {
    					// TODO Auto-generated catch block
    					log.info("failed to sleep; likely that the server is closed.");
    				}
    			} catch (MalformedURLException e) {
    				// TODO Auto-generated catch block
    				e.printStackTrace();
    			} catch (IOException e) {
    				// TODO Auto-generated catch block
    				log.info("failed to send worker status; likely that the server is closed.");
    			} 
    					
        	}
        	stop();
        };
        
        new Thread(statusCheck).start();
        
    }
}

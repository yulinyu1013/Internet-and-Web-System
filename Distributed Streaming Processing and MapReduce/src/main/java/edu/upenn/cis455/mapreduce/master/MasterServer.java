package edu.upenn.cis455.mapreduce.master;

import static spark.Spark.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.upenn.cis.stormlite.Config;
import edu.upenn.cis.stormlite.Topology;
import edu.upenn.cis.stormlite.TopologyBuilder;
import edu.upenn.cis.stormlite.bolt.MapBolt;
import edu.upenn.cis.stormlite.bolt.PrintBolt;
import edu.upenn.cis.stormlite.bolt.ReduceBolt;
import edu.upenn.cis.stormlite.distributed.WorkerHelper;
import edu.upenn.cis.stormlite.distributed.WorkerJob;
import edu.upenn.cis.stormlite.spout.FileSpout;
import edu.upenn.cis.stormlite.spout.FileSpoutImpl;
import edu.upenn.cis.stormlite.tuple.Fields;
import edu.upenn.cis455.mapreduce.worker.WorkerStatus;


public class MasterServer {  
	static Logger log = LogManager.getLogger(MasterServer.class);
	
	private static final String WORD_SPOUT = "WORD_SPOUT";
    private static final String MAP_BOLT = "MAP_BOLT";
    private static final String REDUCE_BOLT = "REDUCE_BOLT";
    private static final String PRINT_BOLT = "PRINT_BOLT";
    
    static Config config = new Config();
    static int myPort = 45555;

    
    static HashMap<String, WorkerStatus> workerStatusMap = new HashMap<String, WorkerStatus>();// ip:port, status
    
    public static void registerStatusPage() {
        get("/status", (request, response) -> {
            response.type("text/html");
            StringBuilder sb = new StringBuilder();
            sb.append("<html><head><title>Master</title></head><body>\r\n");
            
            // 1. check worker status
            log.info("checking worker status...");
            synchronized(workerStatusMap) {
                int index = 0;
                Iterator<String> workers = workerStatusMap.keySet().iterator();
    			while (workers.hasNext()) {
    				WorkerStatus ws = workerStatusMap.get(workers.next());
    				if(Instant.now().minusMillis(ws.getLastChecked().toEpochMilli()).toEpochMilli() > 30000) {
    					workers.remove();
    					continue; 
    				}
    				sb.append("<div>");
    				sb.append(index + ": port="+ws.getPort()+", status="+ws.getStatus()+", job=" + ws.getJob() 
    					+ ", keysRead="+ws.getKeysRead()+", keysWritten="+ws.getKeysWritten()
    					+", results="+ws.getResult());
    				sb.append("</div>\r\n");
    				index++;
    			}
			}
			// 2. submit job
			log.info("generating job form...");
            sb.append("<form method=\"POST\" action=\"/submitjob\">\r\n"
            		+ "    Job Name: <input type=\"text\" name=\"jobname\"/><br/>\r\n"
            		+ "    Class Name: <input type=\"text\" name=\"classname\"/><br/>\r\n"
            		+ "    Input Directory: <input type=\"text\" name=\"input\"/><br/>\r\n"
            		+ "    Output Directory: <input type=\"text\" name=\"output\"/><br/>\r\n"
            		+ "    Map Threads: <input type=\"text\" name=\"map\"/><br/>\r\n"
            		+ "    Reduce Threads: <input type=\"text\" name=\"reduce\"/><br/>\r\n"
            		+ "    <input type=\"submit\" value=\"Submit\"><br/>\r\n"
            		+ "</form>");
            
            //name for sanity check
            sb.append("<div> Yulin Yu (yulinyu) </div>");
            sb.append("</body></html>");
            
            return sb.toString();
        });

    }
    
    public static void getWorkerStatus() {
        get("/workerstatus", (request, response) -> {
        	String ip = request.ip();
        	String port = request.queryParams("port");
        	String status = request.queryParams("status");
        	String job = request.queryParams("job");
        	String keysRead = request.queryParams("keysRead");
        	String keysWritten = request.queryParams("keysWritten");
        	String results = request.queryParams("results");
        	Instant now = Instant.now();
        	String key = ip+":"+port;
        	
        	WorkerStatus workerStatus = new WorkerStatus(ip, port, status, job, keysRead, keysWritten, results, now);

        	workerStatusMap.put(key, workerStatus);

            return "";
        });

    }
    
    public static void submitJob() {
        post("/submitjob", (request, response) -> {
        	log.info("submitting job...");
        	try {
        		// 1. populate config
        		log.info("1. populating config...");
        		
        		// Job name
    			config.put("job", request.queryParams("jobname"));

    			// IP: port for /workerstatus to be sent
    			config.put("masterPort", "127.0.0.1:" + myPort);

    			// Class with map and reduce function
    			// e.g. edu.upenn.cis455.mapreduce.WordCount
    			config.put("mapClass", request.queryParams("classname"));
    			config.put("reduceClass", request.queryParams("classname"));

    			// Input Directory
    			config.put("inputDirectory", request.queryParams("input"));
    			
    			// Output Directory
    			config.put("outputDirectory", request.queryParams("output"));

    			// Numbers of executors 
    			config.put("spoutExecutors", "1"); 
    			config.put("mapExecutors", request.queryParams("map"));
    			config.put("reduceExecutors", request.queryParams("reduce"));
    			
    			log.info("1. populating active workers config...");
		        StringBuilder workerList = new StringBuilder();
		        workerList.append("[");
		        
		        Iterator<String> workerIterator = workerStatusMap.keySet().iterator();
				while (workerIterator.hasNext()) {
					WorkerStatus ws = workerStatusMap.get(workerIterator.next());
					if(!ws.isRemoved() &&Instant.now().minusMillis(ws.getLastChecked().toEpochMilli()).toEpochMilli() <= 30000) {
						log.info("adding worker: "+ws.getIp() + ":"+ ws.getPort());
						workerList.append(ws.getIp() + ":"+ ws.getPort());
						if(workerIterator.hasNext()) {
							workerList.append(",");
						}
					}
				}

				workerList.append("]");
				System.out.println(workerList.toString());
				config.put("workerList", workerList.toString());
    			      	
				// 2. create a topo
				log.info("2. creating a topo...");
		        FileSpout spout = new FileSpoutImpl();
		        MapBolt bolt = new MapBolt();
		        ReduceBolt bolt2 = new ReduceBolt();
		        PrintBolt printer = new PrintBolt();
	        	
		        TopologyBuilder builder = new TopologyBuilder();
	
		        // Only one source ("spout") for the words
				builder.setSpout(WORD_SPOUT, spout, Integer.valueOf(config.get("spoutExecutors")));
				
		        // Parallel mappers, each of which gets specific words
		        builder.setBolt(MAP_BOLT, bolt, Integer.valueOf(config.get("mapExecutors"))).fieldsGrouping(WORD_SPOUT, new Fields("value"));
		        
		        // Parallel reducers, each of which gets specific words
		        builder.setBolt(REDUCE_BOLT, bolt2, Integer.valueOf(config.get("reduceExecutors"))).fieldsGrouping(MAP_BOLT, new Fields("key"));
	
		        // Only use the first printer bolt for reducing to a single point
		        builder.setBolt(PRINT_BOLT, printer, 1).firstGrouping(REDUCE_BOLT);
		        
		        Topology topo = builder.createTopology();
		     
		        WorkerJob job = new WorkerJob(topo, config);

	        	// 3. define and run job
		        log.info("3. defining and running job...");
		        ObjectMapper mapper = new ObjectMapper();
		        mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
		        String[] workers = WorkerHelper.getWorkers(config);
				int i = 0;
				for (String dest: workers) {
					log.info("defining job...");
			        config.put("workerIndex", String.valueOf(i++));
					if (sendJob(dest, "POST", config, "definejob", 
							mapper.writerWithDefaultPrettyPrinter().writeValueAsString(job)).getResponseCode() != 
							HttpURLConnection.HTTP_OK) {
						throw new RuntimeException("Job definition request failed");
					}
				}
				for (String dest: workers) {
					log.info("running job...");
					if (sendJob(dest, "POST", config, "runjob", "").getResponseCode() != 
							HttpURLConnection.HTTP_OK) {
						throw new RuntimeException("Job execution request failed");
					}
				}
				response.type("text/html");
				return "";
			} catch (JsonProcessingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	
            return "";
        });
    }
    
    
    
    public static void shutdown() {
        get("/shutdown", (request,response) -> {
        	response.type("text/html");
        	log.info("shutting down workers...");
			for(String dest : workerStatusMap.keySet()) {
				if(!workerStatusMap.get(dest).isRemoved()) {
					
					try {
    					log.info("master shutting down worker: "+ dest);
    					
    					URL url = new URL("http://" + dest + "/shutdown");
    					
    					log.info("Sending request to " + url.toString());
    					
    					HttpURLConnection conn = (HttpURLConnection)url.openConnection();
    					conn.setDoOutput(true);
    					conn.setRequestMethod("GET");
    					if(conn.getResponseCode()!= HttpURLConnection.HTTP_OK){
    						log.debug("Failed to sent shutdown request to workers");
    					};
    					
    				} catch(Exception e) {
    					e.printStackTrace();
    				}
    			}
    				
        	}

			stop();
			return "shutdown";
		});
    }
    
    public static void workerInformShutdown() {
        get("/workershutdown", (request, response) -> {
        	response.type("text/html");
        	
        	String ip = request.ip();
			String port = request.queryParams("port");
			String worker = ip + ":" + port;
			log.info("removing worker from active list: " + worker);

			workerStatusMap.get(worker).setRemoved(true); 

			log.info("removed...");
			return "received shutdown info";
		});
    }

    /**
     * The mainline for launching a MapReduce Master.  This should
     * handle at least the status and workerstatus routes, and optionally
     * initialize a worker as well.
     * 
     * @param args
     */
    public static void main(String[] args) {
    	org.apache.logging.log4j.core.config.Configurator.setLevel("org.eclipse.jetty", Level.ERROR);
        if (args.length < 1) {
            System.out.println("Usage: MasterServer [port number]");
            System.exit(1);
        }
        
        myPort = Integer.valueOf(args[0]);
        port(myPort);

        log.info("Master node startup, on port " + myPort);
        
        getWorkerStatus();
        registerStatusPage();
        submitJob();
        workerInformShutdown();
        shutdown();
    }
    
    
    static HttpURLConnection sendJob(String dest, String reqType, Config config, String job, String parameters) throws IOException {
    	
    	URL url = new URL(dest + "/" + job);
		
		log.info("Sending request to " + url.toString());
		
		HttpURLConnection conn = (HttpURLConnection)url.openConnection();
		conn.setDoOutput(true);
		conn.setRequestMethod(reqType);
		
		if (reqType.equals("POST")) {
			conn.setRequestProperty("Content-Type", "application/json");
			
			OutputStream os = conn.getOutputStream();
			byte[] toSend = parameters.getBytes();
			os.write(toSend);
			os.flush();
		} else
			conn.getOutputStream();
		
		return conn;
    }
}


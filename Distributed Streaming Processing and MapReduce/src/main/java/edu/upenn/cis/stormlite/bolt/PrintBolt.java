package edu.upenn.cis.stormlite.bolt;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.stormlite.OutputFieldsDeclarer;
import edu.upenn.cis.stormlite.TopologyContext;
import edu.upenn.cis.stormlite.distributed.ConsensusTracker;
import edu.upenn.cis.stormlite.distributed.WorkerHelper;
import edu.upenn.cis.stormlite.routers.StreamRouter;
import edu.upenn.cis.stormlite.tuple.Fields;
import edu.upenn.cis.stormlite.tuple.Tuple;
import edu.upenn.cis455.mapreduce.worker.WorkerServer;

public class PrintBolt implements IRichBolt {
	static Logger log = LogManager.getLogger(PrintBolt.class);
	Fields myFields = new Fields();

    /**
     * To make it easier to debug: we have a unique ID for each
     * instance of the PrintBolt, aka each "executor"
     */
    String executorId = UUID.randomUUID().toString();
    
    private TopologyContext context;
    
    int neededVotesToComplete = 0;
    
	Set<String> votedEos = new HashSet<>(); // handle duplicated votes
    
	/**
	 * This object can help determine when we have
	 * reached enough votes for EOS
	 */
	ConsensusTracker votesForEos;
	
	FileWriter fw = null;
	BufferedWriter bw = null;

	@Override
	public String getExecutorId() {

		return executorId;
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(myFields);
		
	}

	@Override
	public void cleanup() {
		
	}

	@Override
	public boolean execute(Tuple input) {
		if (!input.isEndOfStream()) {
			String key = input.getStringByField("key");
	        String value = input.getStringByField("value");
	        
			synchronized (bw) {
		        try {
					bw.write("("+key + "," + value+ ")\r\n");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			if(context.results.size() < 100) {
				context.results.add("(" + key +","+ value + ")");
			}
			
			
		}else {
			if (votedEos.contains(input.getSourceExecutor())) {
                return true;
            }
            votedEos.add(input.getSourceExecutor());
			
			if(votesForEos.voteForEos(input.getSourceExecutor())) {
				//complete job, idle
				context.getKeysRead().set(0);
				context.setState(TopologyContext.STATE.IDLE);
				try {
					bw.close();
					fw.close();
					WorkerServer.cluster.killTopology(executorId);
					log.info("Finish writing output.txt");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		}
		return false;
	}

	@Override
	public void prepare(Map<String, String> stormConf, TopologyContext context, OutputCollector collector) {
		
	    this.context = context;
		
		// make output dir
	    File outDir = Paths.get(WorkerServer.getConfig().get("storageDir"), stormConf.get("outputDirectory")).toFile();
	    if(!outDir.exists()) {
	    	if(!outDir.mkdir()) {
	    		log.debug("Failed to make output dir.");
	    	}
	    }
	    // make output.txt
		File file = Paths.get(WorkerServer.getConfig().get("storageDir"), stormConf.get("outputDirectory"), "output.txt").toFile();
		log.info("Output path: "+ file.getAbsolutePath());
    	
    	// init writer
    	try {
			fw = new FileWriter(file.getAbsolutePath(), false);
			bw = new BufferedWriter(fw);
		} catch (IOException e) {
			log.debug("Failed to create a file writer");
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
		
		
        // determine how many EOS votes needed and set up ConsensusTracker (or however
        // you want to handle consensus)
        int numWorker = WorkerHelper.getWorkers(stormConf).length;
        int numReduce = Integer.parseInt(stormConf.get("reduceExecutors"));

        neededVotesToComplete = numWorker * numReduce;
        votesForEos = new ConsensusTracker(neededVotesToComplete);
		
	}

	@Override
	public void setRouter(StreamRouter router) {
		// do nothing
		
	}

	@Override
	public Fields getSchema() {
		return myFields;
	}

}

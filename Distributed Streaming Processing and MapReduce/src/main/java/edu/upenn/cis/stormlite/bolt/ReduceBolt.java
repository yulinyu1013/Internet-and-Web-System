package edu.upenn.cis.stormlite.bolt;

import java.io.File;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sleepycat.persist.EntityCursor;

import edu.upenn.cis.stormlite.OutputFieldsDeclarer;
import edu.upenn.cis.stormlite.TopologyContext;
import edu.upenn.cis.stormlite.distributed.ConsensusTracker;
import edu.upenn.cis.stormlite.distributed.WorkerHelper;
import edu.upenn.cis.stormlite.routers.StreamRouter;
import edu.upenn.cis.stormlite.tuple.Fields;
import edu.upenn.cis.stormlite.tuple.Tuple;
import edu.upenn.cis455.mapreduce.Job;
import edu.upenn.cis455.mapreduce.TupleDB;
import edu.upenn.cis455.mapreduce.worker.WorkerServer;

/**
 * A simple adapter that takes a MapReduce "Job" and calls the "reduce"
 * on a per-tuple basis
 * 
 */
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class ReduceBolt implements IRichBolt {
	static Logger log = LogManager.getLogger(ReduceBolt.class);

	
	Job reduceJob;

	/**
	 * This object can help determine when we have
	 * reached enough votes for EOS
	 */
	ConsensusTracker votesForEos;

	/**
     * To make it easier to debug: we have a unique ID for each
     * instance of the WordCounter, aka each "executor"
     */
    String executorId = UUID.randomUUID().toString();
    
	Fields schema = new Fields("key", "value");
	
	boolean sentEos = false;
	
	Set<String> votedEos = new HashSet<>(); // handle duplicated votes
	
	/**
	 * Buffer for state, by key
	 */
	Map<String, List<String>> stateByKey = new HashMap<>();

	/**
     * This is where we send our output stream
     */
    private OutputCollector collector;
    
    private TopologyContext context;
    
    int neededVotesToComplete = 0;
    
    String subDir = "";
    
    TupleDB tupleDB;
    
    public ReduceBolt() {
    }
    
    /**
     * Initialization, just saves the output stream destination
     */
    @Override
    public void prepare(Map<String,String> stormConf, 
    		TopologyContext context, OutputCollector collector) {
        this.collector = collector;
        this.context = context;

        if (!stormConf.containsKey("reduceClass"))
        	throw new RuntimeException("Mapper class is not specified as a config option");
        else {
        	String mapperClass = stormConf.get("reduceClass");
        	
        	try {
				reduceJob = (Job)Class.forName(mapperClass).newInstance();
			} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new RuntimeException("Unable to instantiate the class " + mapperClass);
			}
        }
        if (!stormConf.containsKey("mapExecutors")) {
        	throw new RuntimeException("Reducer class doesn't know how many map bolt executors");
        }
        
        //prepare DB
        String db = "worker_"+stormConf.get("workerIndex")+"_executor_"+this.getExecutorId();
        subDir = Paths.get(WorkerServer.getConfig().get("storageDir"), db).toAbsolutePath().toString();        
        tupleDB = new TupleDB(subDir, db);

        // determine how many EOS votes needed and set up ConsensusTracker (or however
        // you want to handle consensus)
        int numWorker = WorkerHelper.getWorkers(stormConf).length;
        int numMap = Integer.parseInt(stormConf.get("mapExecutors"));

        neededVotesToComplete = numWorker * numMap;
        votesForEos = new ConsensusTracker(neededVotesToComplete);
        System.out.println("votes needed in reducer: "+neededVotesToComplete);
    }

    /**
     * Process a tuple received from the stream, buffering by key
     * until we hit end of stream
     */
    @Override
    public synchronized boolean execute(Tuple input) {
    	if (sentEos) {
	        if (!input.isEndOfStream()) {
	        	System.out.println("Reducer "+this.getExecutorId()+" getting upexpected " + input.toString() + " from mapper " + input.getSourceExecutor());
	        	throw new RuntimeException("We received data after we thought the stream had ended!");
	        }	
	    	// Already done!    
	        return false;
	        
		} else { // not sent
			if (input.isEndOfStream()) {
	
	    		log.debug("Processing EOS from mapper: " + input.getSourceExecutor());
				// only if at EOS do we trigger the reduce operation over what's in BerkeleyDB for
	    		// the associated key, and output all state
	    		if (votedEos.contains(input.getSourceExecutor())) {
	                return true;
	            }
	            votedEos.add(input.getSourceExecutor());
	    		// You may find votesForEos useful to determine when consensus is reached
	    		if(votesForEos.voteForEos(input.getSourceExecutor())) {
	    			log.info("Concensus met! Start reducing ops...");
	    			
	    			// track worker status - reducing
	    			if(context.getState() != TopologyContext.STATE.REDUCING) {
	    				context.getKeysRead().set(0);
		    			context.getKeysWritten().set(0);
		    			context.setState(TopologyContext.STATE.REDUCING);
	    			}
	    	
	    			try {
	    				EntityCursor<String> keys = tupleDB.getAllKeys(); // get all words
	    				String key = keys.nextNoDup(); // next nonrepeated word
	    				
	    				while (key != null) {
	    					
	    					System.out.println("reducing key: "+ key);   					

	    					context.getKeysRead().incrementAndGet();
	    					List<String> values = tupleDB.getAllTuples(key); // get all (Word, 1)
	    					
	    					reduceJob.reduce(key, values.iterator(), collector, executorId);
	    					context.getKeysWritten().incrementAndGet();
	    					context.incReduceOutputs(key);

	    					key = keys.nextNoDup();
	    				}
	    				
	    				keys.close();
	    				
	    				//clean db
	    				tupleDB.clean();
	    				tupleDB.close();
	    				FileUtils.deleteDirectory(new File(subDir));
	    				
	    				
	    			} catch(Exception e) {
	    				e.printStackTrace();
	    				
	    			} 
	    			collector.emitEndOfStream(executorId);
	    			sentEos = true;
    			
	    		}
			} else { //not EOS
	    		// collect the tuples by key into BerkeleyDB (until EOS arrives, in the above condition)
	    		log.debug("Processing " + input.toString() + " from " + input.getSourceExecutor());
	    		System.out.println("Reducer "+this.getExecutorId()+" processing " + input.toString() + " from mapper " + input.getSourceExecutor());
	    		String key = input.getStringByField("key");
		        String value = input.getStringByField("value");
		        tupleDB.addTuple(key, value); // words, 1
	
	    	}   
		}
			
    	return true;
    }

    /**
     * Shutdown, just frees memory
     */
    @Override
    public void cleanup() {
    }

    /**
     * Lets the downstream operators know our schema
     */
    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(schema);
    }

    /**
     * Used for debug purposes, shows our exeuctor/operator's unique ID
     */
	@Override
	public String getExecutorId() {
		return executorId;
	}

	/**
	 * Called during topology setup, sets the router to the next
	 * bolt
	 */
	@Override
	public void setRouter(StreamRouter router) {
		this.collector.setRouter(router);
	}

	/**
	 * The fields (schema) of our output stream
	 */
	@Override
	public Fields getSchema() {
		return schema;
	}

}

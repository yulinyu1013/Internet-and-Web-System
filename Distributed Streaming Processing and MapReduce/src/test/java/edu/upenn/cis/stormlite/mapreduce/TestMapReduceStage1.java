package edu.upenn.cis.stormlite.mapreduce;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.stormlite.Config;
import edu.upenn.cis.stormlite.DistributedCluster;

import edu.upenn.cis.stormlite.Topology;
import edu.upenn.cis.stormlite.TopologyBuilder;
import edu.upenn.cis.stormlite.bolt.MapBolt;
import edu.upenn.cis.stormlite.bolt.PrintBolt;
import edu.upenn.cis.stormlite.bolt.ReduceBolt;
import edu.upenn.cis.stormlite.spout.FileSpout;
import edu.upenn.cis.stormlite.tuple.Fields;
import edu.upenn.cis455.mapreduce.worker.WorkerServer;

public class TestMapReduceStage1 {
	
	static Logger log = LogManager.getLogger(TestMapReduceStage1.class);
	private static final String WORD_SPOUT = "WORD_SPOUT";
    private static final String MAP_BOLT = "MAP_BOLT";
    private static final String REDUCE_BOLT = "REDUCE_BOLT";
    private static final String PRINT_BOLT = "PRINT_BOLT";
    
    static void createSampleMapReduce(Config config) {
        // Job name
        config.put("job", "stage1");
        
        // Class with map function
        config.put("mapClass", "edu.upenn.cis455.mapreduce.job.WordCount");
        
        // Class with reduce function
        config.put("reduceClass", "edu.upenn.cis455.mapreduce.job.WordCount");
        
        // Numbers of executors (per node)
        config.put("spoutExecutors", "1");
        config.put("mapExecutors", "3");
        config.put("reduceExecutors", "2");
        
    }
    
    public static void main(String[] args) throws Exception {
    	Config config = new Config();
    	config.put("workerList", "[127.0.0.1:45555]");
    	config.put("workerIndex", "0");
    	config.put("storageDir", "");
    	config.put("outputDirectory", "");
    	config.put("inputDirectory", "");
    	
    	createSampleMapReduce(config);
    	WorkerServer.config = config;
    	
    	FileSpout spout = new WordFileSpout();
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
        
//        LocalCluster cluster = new LocalCluster();
        DistributedCluster cluster = new DistributedCluster();
        cluster.submitTopology("stage1", config, topo);
        cluster.startTopology();
        
        Thread.sleep(3000);
        cluster.killTopology("stage1");
        cluster.shutdown();
        System.exit(0);
    	
    }
}

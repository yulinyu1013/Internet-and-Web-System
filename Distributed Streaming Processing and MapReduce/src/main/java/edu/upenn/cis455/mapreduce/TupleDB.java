package edu.upenn.cis455.mapreduce;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.persist.EntityCursor;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;
import com.sleepycat.persist.SecondaryIndex;
import com.sleepycat.persist.StoreConfig;

/**
 * BDB to store mapping results
 * */
public class TupleDB {
	static Logger log = LogManager.getLogger(TupleDB.class);
	private Environment env;
	private EntityStore store;
	private PrimaryIndex<Integer, TupleStore> pi; 
    private SecondaryIndex<String, Integer, TupleStore> si;
	
    
    /**
     * Constructor
     * */
	public TupleDB(String envDir, String db) {
		try {
			
			// 1. config env
			EnvironmentConfig envConfig = new EnvironmentConfig();
			envConfig.setTransactional(true);
			envConfig.setAllowCreate(true);

			// 2. check dir
			if (!Files.exists(Paths.get(envDir))) {
	            try {
	            	log.info("Create new directory...");
	                Files.createDirectory(Paths.get(envDir));
	            } catch (IOException e) {
	                // TODO Auto-generated catch block
	                e.printStackTrace();
	            }
	        }else { 
	        	try {
	        		log.info("clean exisiting db: "+envDir);
					FileUtils.cleanDirectory(new File(envDir));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	        }
			
			env = new Environment(new File(envDir), envConfig);
			
			// 3. config store
			StoreConfig storeConfig = new StoreConfig();
			storeConfig.setTransactional(true);
			storeConfig.setAllowCreate(true);
			store = new EntityStore(env, db, storeConfig);
			
	        pi = store.getPrimaryIndex(Integer.class, TupleStore.class);
	        si = store.getSecondaryIndex(pi, String.class, "key");
	        
		} catch(DatabaseException e) {
			e.printStackTrace();
		}

	}
	
	
	/**
	 * Add tuple
	 * */
    public void addTuple(String key, String value) {
        TupleStore tupleStore = new TupleStore(key, value);
        pi.put(tupleStore);
    }
	
    /**
     * Get all keys
     * */
    public EntityCursor<String> getAllKeys() {
        EntityCursor<String> entityCursor = si.keys();
        return entityCursor;
    }
    
    /**
     * Get all tuples of a key
     * */
    public List<String> getAllTuples(String key) {
    	
        EntityCursor<TupleStore> cursor = si.subIndex(key).entities();
        List<String> tuples = new ArrayList<String>();
        
		for(TupleStore t : cursor) {
			tuples.add(t.getValue());
		}
		
		cursor.close();
        return tuples;
    }
    
    /**
     * Clean all instances of tuple
     * */
    public void clean() {
        store.truncateClass(TupleStore.class);
    }
    
    /**
     * Shutdown DB
     * */
    public void close() {
        try {
            store.close();
            env.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
	
}

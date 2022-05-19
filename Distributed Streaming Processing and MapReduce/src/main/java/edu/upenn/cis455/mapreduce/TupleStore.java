package edu.upenn.cis455.mapreduce;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
import com.sleepycat.persist.model.Relationship;
import com.sleepycat.persist.model.SecondaryKey;

/**
 * BDB instance
 * */
@Entity
public class TupleStore {
    @PrimaryKey(sequence = "TupleStoreIndex")
    private Integer primaryKey;

    @SecondaryKey(relate = Relationship.MANY_TO_ONE)
    private String key;
    private String value;
    
    public TupleStore() {
    
    }
    
    public TupleStore(String key, String value) {
        this.key = key;
        this.value = value;
    }

	public Integer getPrimaryKey() {
		return primaryKey;
	}

	public String getKey() {
		return key;
	}

	public String getValue() {
		return value;
	}
}

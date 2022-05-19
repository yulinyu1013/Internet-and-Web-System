package edu.upenn.cis.cis455.storage;

import java.io.File;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sleepycat.bind.EntryBinding;
import com.sleepycat.bind.serial.SerialBinding;
import com.sleepycat.bind.serial.StoredClassCatalog;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;


public class Storage implements StorageInterface {
	final static Logger logger = LogManager.getLogger(Storage.class);
	private static Storage storage;
	
	// env (db)
	private static String envDir = null;
	private Environment env = null;
	
	// db (table)
	private StoredClassCatalog classCatalog;
	private Database catalogDB;
	private Database userDB;
	private Database docDB;
	private Database channelDB; 
	private Database contentSeenDB;
	
	// binding
	private EntryBinding<String> userKeyBinding;
	private EntryBinding<User> userValBinding;
	private EntryBinding<String> docKeyBinding;
	private EntryBinding<Content> docValBinding;
	private EntryBinding<String> channelKeyBinding;
	private EntryBinding<Channel> channelValBinding;
	private EntryBinding<byte[]> contentSeenKeyBinding;
	private EntryBinding<ContentSeen> contentSeenValBinding;
	// api
	private UserApi userApi;
	private ContentApi docApi;
	private ChannelApi channelApi;
	private ContentSeenApi contentSeenApi;
	
	
	/**
     * Init singleton object
     */
    public static void init(String dir) {
    	storage = new Storage(dir);
 
    }
    
	/**
     * Get singleton object
     */
    public static Storage getInstance() {
    	if(storage == null) {
    		logger.debug("Storage Not initialized!");
    	} 
    	return storage;
    }

	private Storage(String dir) {
		if(env == null) {
			envDir = dir;
			
			// open the environment
			logger.info("Init env...");
			EnvironmentConfig envConfig = new EnvironmentConfig();
			envConfig.setTransactional(true);
			envConfig.setAllowCreate(true);
			env = new Environment(new File(dir), envConfig);
			
			logger.info("Config dbs...");
			// config for persistent database
			DatabaseConfig dbConfig = new DatabaseConfig();
			dbConfig.setTransactional(true);
			dbConfig.setAllowCreate(true);
			
			// config for contentSeen
			DatabaseConfig dbConfig2 = new DatabaseConfig();
			dbConfig2.setAllowCreate(true);
			dbConfig2.setTemporary(true);
			
			logger.info("Init dbs...");
			// open catalog db
			catalogDB = env.openDatabase(null, "ClassCatalog",dbConfig);
			classCatalog = new StoredClassCatalog(catalogDB);
			
			// open user db
			userDB = env.openDatabase(null, "User", dbConfig);
			userKeyBinding = new SerialBinding<String>(classCatalog, String.class);
			userValBinding = new SerialBinding<User>(classCatalog, User.class);
			
			// open doc db
			docDB = env.openDatabase(null, "Content", dbConfig);
			docKeyBinding = new SerialBinding<String>(classCatalog, String.class);
			docValBinding = new SerialBinding<Content>(classCatalog, Content.class);
			
			// open channel db
			channelDB = env.openDatabase(null, "Channel", dbConfig);
			channelKeyBinding = new SerialBinding<String>(classCatalog, String.class);
			channelValBinding = new SerialBinding<Channel>(classCatalog, Channel.class);
			
			// open content seen db
			contentSeenDB = env.openDatabase(null, "ContentSeen", dbConfig2);
			contentSeenKeyBinding = new SerialBinding<byte[]>(classCatalog, byte[].class);
			contentSeenValBinding = new SerialBinding<ContentSeen>(classCatalog, ContentSeen.class);
			
			logger.info("Init apis...");
			// instantiate internal apis
			userApi = new UserApi(this);
			docApi = new ContentApi(this);
			channelApi = new ChannelApi(this);
			contentSeenApi = new ContentSeenApi(this);
			
		} else {
			if(!envDir.equals(dir)) {
				logger.debug("The database has been instantiated with a different directory");
			}
		}
	}
	
	/**
	 * Env related API
	 * */
	@Override
	public void close() {
		logger.info("Closing dbs...");
		userDB.close();
		docDB.close();
		channelDB.close(); //Storage (M1)
		contentSeenDB.close();
		classCatalog.close();
		catalogDB.close();
		env.close();
		
		
	}
	
	public Environment getEnv() {
		return env;
	}

	
	/**
	 * Document Related APIs
	 * */
	@Override
	public int getCorpusSize() {
		return (int) docDB.count();
	}

	@Override
	public void addDocument(String url, String documentContents) {
		Content doc = new Content(url, documentContents);
		docApi.addDocument(doc);
	}
	
	@Override
    public void addDocument(Content doc) {
		docApi.addDocument(doc);
	}

	@Override
	public Content getDocument(String url) {
		return docApi.getDocument(url);
	}
	
	@Override
	public ContentApi getDocApi() {
		return docApi;
	}

	public Database getDocDB() {
		return docDB;
	}

	public EntryBinding<String> getDocKeyBinding() {
		return docKeyBinding;
	}

	public EntryBinding<Content> getDocValBinding() {
		return docValBinding;
	}

	/**
	 * User Related APIs
	 * */
	@Override
	public boolean addUser(String username, String password) {
		
		try {
			User user = new User(username, password);
			
			//if user already in db
			if(userApi.getUser(username) != null) {
				return false; 
			}
			userApi.addUser(user);
			return true;
			
		} catch (NoSuchAlgorithmException e1) {
			e1.printStackTrace();
		}
		
		return false;
		
	}

	@Override
	public boolean getSessionForUser(String username, String password) {
		try {
			User user = new User(username, password);
			User userInDB = userApi.getUser(username);
			
			//user no exist
			if(userInDB == null) {
				logger.info("User not exist.");
				return false; 
			}
			
			//wrong password
			if(!Arrays.equals(user.getHashedPassword(),userInDB.getHashedPassword())){
				logger.info("Wrong password.");
				return false;
			}
			
			//valid credentials
			return true;
			
		} catch (NoSuchAlgorithmException e) {

			e.printStackTrace();
		}
		
		return false;
	}
	

	public Database getUserDB() {
		return userDB;
	}
	
	public UserApi getUserApi() {
		return userApi;
	}

	public EntryBinding<String> getUserKeyBinding() {
		return userKeyBinding;
	}

	public EntryBinding<User> getUserValBinding() {
		return userValBinding;
	}
	
	
	
	/**
	 * ContentSeen APIs
	 * */
	@Override
	public ContentSeenApi getContentSeenApi() {
		return contentSeenApi;
	}

	public Database getContentSeenDB() {
		return contentSeenDB;
	}


	public EntryBinding<byte[]> getContentSeenKeyBinding() {
		return contentSeenKeyBinding;
	}

	public EntryBinding<ContentSeen> getContentSeenValBinding() {
		return contentSeenValBinding;
	}

	/**
	 * Channel Related APIs (M2)
	 * */
	public EntryBinding<String> getChannelKeyBinding() {
		return channelKeyBinding;
	}

	public EntryBinding<Channel> getChannelValBinding() {
		return channelValBinding;
	}

	public ChannelApi getChannelApi() {
		return channelApi;
	}

	public Database getChannelDB() {
		return channelDB;
	}



}

package edu.upenn.cis.cis455.storage;


public interface StorageInterface {

    /**
     * How many documents so far?
     */
    public int getCorpusSize();

    /**
     * Add a new document, getting its ID
     */
    public void addDocument(String url, String documentContents);
    
    /**
     * Add a new document, getting its ID
     */
    public void addDocument(Content doc);


    /**
     * Retrieves a document's contents by URL - modified
     */
    public Content getDocument(String url);

    
    public ContentApi getDocApi();
    
    public ContentSeenApi getContentSeenApi();
    
    /**
     * Adds a user and returns an ID
     */
    public boolean addUser(String username, String password);
    
    
    /**
     * Tries to log in the user, or else throws a HaltException
     */
    public boolean getSessionForUser(String username, String password);

    /**
     * Shuts down / flushes / closes the storage system
     */
    public void close();
}

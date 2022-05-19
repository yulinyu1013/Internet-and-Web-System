package edu.upenn.cis.cis455.storage;

public class StorageFactory {
    public static StorageInterface getDatabaseInstance(String directory) {
    	Storage.init(directory);
        return Storage.getInstance();
    }
}

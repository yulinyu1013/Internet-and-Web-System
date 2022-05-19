package edu.upenn.cis.stormlite.spout;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.file.Paths;
import java.util.ArrayList;

import edu.upenn.cis455.mapreduce.worker.WorkerServer;

public class FileSpoutImpl extends FileSpout{
	
	ArrayList<BufferedReader> readers = new ArrayList<BufferedReader>();

	@Override
	public String getFilename() {
		// TODO Auto-generated method stub
	
		
		return null; 
	}

}

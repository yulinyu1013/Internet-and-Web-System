package edu.upenn.cis.cis455.xpathengine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class XPathNode {
	final static Logger logger = LogManager.getLogger(XPathNode.class);
	public Map<String, Integer> stateMap = new HashMap<String, Integer>(); 
	public List<String> steps = new ArrayList<String>();
	public List<Boolean> contains = new ArrayList<Boolean>();
	
	public XPathNode(String xpath) {
		String[] nodes = xpath.trim().split("/");
		
		for(int i=1; i < nodes.length; i++) {
			int k;
			String nodeName = "";
			String currNode = nodes[i];
			
			for(k = 0; k < currNode.length(); k++) {
				if(currNode.charAt(k) == '[') {
					break;
				}
				nodeName += currNode.charAt(k);
			}
			
			steps.add(nodeName);
			System.out.println("added node: " + nodeName);
			contains.add(false);
			
			k++;
			if(k < currNode.length() - 1) { // contain content filter
				String test = "";
				String value = null;
				
				while(currNode.charAt(k) != '(') {
					test += currNode.charAt(k++);
				}
				
				if(test.trim().equals("text")) {
					value = currNode.split("=")[1]; //get the part after "="
					value = value.split("]$")[0].trim();
					value = value.replaceAll("^\"|\"$", "");
					steps.add(value);
					contains.add(false);
				} else {
					value = currNode.split(",")[1]; //get the part after ","
					value = value.split("]$")[0].trim();
					value = value.split("\\)$")[0].trim();
					value = value.replaceAll("^\"|\"$", "");
					steps.add(value);
					System.out.println("added node: " + nodeName);
					contains.add(true);
				}
				
			}
		}
	}
	
	public boolean evaluate(OccurrenceEvent oe) {
		String docUrl = oe.getUrl();
		OccurrenceEvent.Type type = oe.getType();
		String value = oe.getValue();
		int depth = oe.getDepth();
		boolean isHtml = oe.isHtml();
		
		int currLevel = 0;
		
		if(!stateMap.containsKey(docUrl)) {
			stateMap.put(docUrl, currLevel);
		}else {
			currLevel = stateMap.get(docUrl);
		}
		System.out.println("Evaluating level " + currLevel);
		
//		logger.info("event: "+oe.getType().toString() +", "+oe.getValue()+", level "+ oe.getDepth());
		if(currLevel >= steps.size()) {
			logger.info("match! from xpath node");
			return true;
		}
		
		if(type == OccurrenceEvent.Type.Open) {
			System.out.println("Examming open node...");
			String step = steps.get(currLevel);
			logger.info("Evaluating state level " + currLevel+": "+step);
			if(isHtml) { // case insensitive
				step = step.toLowerCase();
				value = value.toLowerCase();
			}
			System.out.println("not html");
			System.out.println("step: " + step);
			System.out.println("value: " + value);
			if(step.equals(value) && currLevel == depth) {
//				logger.info("match! move to next level");
				System.out.println("State matched!");
				currLevel++;
				stateMap.put(docUrl, currLevel);
				return currLevel >= steps.size() ;
			}
		}
		
		if(type == OccurrenceEvent.Type.Text) {
			if(contains.get(currLevel)) {
				System.out.println("contains");
				if(value.contains(steps.get(currLevel)) && currLevel == depth) {
					currLevel++;
					stateMap.put(docUrl, currLevel);
					return currLevel >= steps.size();
				}
			} else {
				logger.info("Evaluating state level " + currLevel+": "+steps.get(currLevel));
				logger.info("Evaluating oe value: " + value);
				if(steps.get(currLevel).equals(value) && currLevel == depth) {
					currLevel++;
					stateMap.put(docUrl, currLevel);
					return currLevel >= steps.size();
				}
			}
		}
		
		if(type == OccurrenceEvent.Type.Close && currLevel > 0 && steps.get(currLevel-1).equals(value)) { 
			currLevel--;
			stateMap.put(docUrl, currLevel);
		}
	
		
		return false;
	}
	
}

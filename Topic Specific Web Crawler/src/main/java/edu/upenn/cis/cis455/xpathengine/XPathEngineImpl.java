package edu.upenn.cis.cis455.xpathengine;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.storage.Channel;

public class XPathEngineImpl implements XPathEngine {
	final static Logger logger = LogManager.getLogger(XPathEngineImpl.class);
	
	private XPathNode[] xPathNodes;
	private String[] xpaths;
	private Channel[] channels;

	public Channel[] getChannels() {
		return channels;
	}

	public void setChannels(Channel[] channels) {
		this.channels = channels;
	}

	public String[] getXpaths() {
		return xpaths;
	}

	@Override
	public void setXPaths(String[] expressions) {
		xPathNodes = new XPathNode[expressions.length];
		
		for(int i = 0; i < expressions.length; i++) {
			xPathNodes[i] = new XPathNode(expressions[i]);
		}
		
	}

	@Override
	public boolean[] evaluateEvent(OccurrenceEvent event) {
		
		if(xPathNodes == null || event == null) {
			return null;
		}
		
		boolean[] isMatched = new boolean[xPathNodes.length];
		
	    for(int i=0; i<xPathNodes.length; i++) {
	    	
	    	isMatched[i] = xPathNodes[i].evaluate(event);
	    }
			
		return isMatched;
	}
	
	
	/**
	 * Validate xpath
	 * */
	public boolean isValid(String xpath) {
		
		if(xpath.length()==0) {return false;} 
		
		XPathFactory factory = XPathFactory.newInstance();
		XPath xp = factory.newXPath();
		try {
			xp.compile(xpath);
		} catch (XPathExpressionException e) {

			return false; // invalid xpath
		}
		
		String[] nodes = xpath.split("/");
		for(int i = 1; i < nodes.length; i++) {
			int k;
			String nodeName = "";
			String currNode = nodes[i];
			
			//check nodeName
			for(k = 0; k < currNode.length(); k++) {
				if(currNode.charAt(k) == '[') {
					break;
				}
				nodeName += currNode.charAt(k);
			}
			
			
			if(nodeName.equals("") || nodeName.indexOf(" ") >=0 || (! nodeName.matches(".*[a-zA-Z]+.*") && ! nodeName.matches(".*\\d.*") )) {
				return false;
			}
			
			k++;
			
			// check [test]
			if(k < currNode.length()) { // meaning containing [
				
				if(!currNode.endsWith("]")) {
					return false;
				}
				
				//check test text
				String test = "";
				while(currNode.charAt(k) != '(') {
					test += currNode.charAt(k++);
					if(k == currNode.length()) { // reach the end
						return false;
					}
				}
				System.out.println(test);
				if(!test.trim().equals("text") && !test.trim().equals("contains")) {
					System.out.println("invalid test");
					return false;
				}
				String part1 = "";
				String part2 = "";
				
				if(test.trim().equals("text")) {
					String[] parts = currNode.substring(k+1).split("=");
					part1 = parts[0];
					part2 = parts[1];
					System.out.println("p1: "+ part1);
					System.out.println("p2: "+ part2);
					if(part1.length() ==0 || part2.length() ==0) {
						return false;
					}
					
					if(!part1.trim().equals(")")) {
						return false;
					}
					
					if(!part2.trim().startsWith("\"") || !part2.replaceAll(" ", "").endsWith("\"]")){
						return false;
					}
					
				}else if (test.trim().equals("contains")) { 
					String[] parts = currNode.substring(k+1).split(",");
					part1 = parts[0];
					part2 = parts[1];
					System.out.println("p1: "+ part1);
					System.out.println("p2: "+ part2);
					if(part1.length() ==0 || part2.length() ==0) {
						return false;
					}
					
					if(!part1.replaceAll(" ", "").equals("text()")) {
						return false;
					}
					
					if(!part2.trim().startsWith("\"") || !part2.replaceAll(" ", "").endsWith("\")]")){
						return false;
					}
					
				}
				
			}
		}
		
		
		
		return true;
	}
	
	
}

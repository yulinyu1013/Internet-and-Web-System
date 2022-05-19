package edu.upenn.cis.cis455.xpathengine;

/**
 This class encapsulates the tokens we care about parsing in XML (or HTML)
 */
public class OccurrenceEvent {
	public enum Type {Open, Close, Text};
	
	Type type;
	String value;
	String url;
	int depth;
	boolean isHtml;
	

	public OccurrenceEvent(Type t, String value, String url, int depth, boolean isHtml) {
		this.type = t;
		this.value = value;
		this.url = url;
		this.depth = depth;
		this.isHtml = isHtml;
	}

	public boolean isHtml() {
		return isHtml;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
	
	public String getUrl() {
		return url;
	}
	
	public int getDepth() {
		return depth;
	}
	

	public String toString() {
		if (type == Type.Open) 
			return "<" + value + ">";
		else if (type == Type.Close)
			return "</" + value + ">";
		else
			return value;
	}
}

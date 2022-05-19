package edu.upenn.cis.cis455.crawler;


public class CrawlerFactory {
	
	private static Crawler crawler = null;

	/**
	 * Only get called once
	 * */
	public static void setCrawler(Crawler c) {
		crawler = c;
	}
	
	/**
	 * Return the singleton
	 * */
	public static Crawler getCrawler() {
		return crawler;
	}
	
}

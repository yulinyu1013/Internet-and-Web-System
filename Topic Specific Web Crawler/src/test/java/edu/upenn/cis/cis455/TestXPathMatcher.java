package edu.upenn.cis.cis455;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import edu.upenn.cis.cis455.xpathengine.OccurrenceEvent;
import edu.upenn.cis.cis455.xpathengine.XPathEngineFactory;
import edu.upenn.cis.cis455.xpathengine.XPathEngineImpl;
import edu.upenn.cis.cis455.xpathengine.XPathNode;

public class TestXPathMatcher {
	
	/***
	 * 4 tests in a single test method
	 * */
	@Test
	public void testIsValid() {
		XPathEngineImpl engine = new XPathEngineImpl();
		assertFalse(engine.isValid("/a /b/c"));
		assertFalse(engine.isValid("/a//b/c"));
		assertTrue(engine.isValid("/a/b[text  (  )  =  \"theEntireText\"  ]"));
		assertTrue(engine.isValid("/a/b[text()=\"theEntireText\"]"));
		assertTrue(engine.isValid("/xyz/abc[contains(text ( ),  \"someSubstring\" ) ]"));
		assertTrue(engine.isValid("/xyz/abc[contains(text(),\"someSubstring\")]"));
		assertTrue(engine.isValid("/rss/channel/title[text()=\"Business\"]"));
		
	}
	/***
	 * 5 tests in a single test method
	 * */
	@Test
	public void testSingleMatch() {
		//easy
		XPathNode xpathBasic = new XPathNode("/a/b/c");
		
		//xml
		assertFalse(xpathBasic.evaluate(new OccurrenceEvent(OccurrenceEvent.Type.Open, "a", "doc", 0, false)));
		assertFalse(xpathBasic.evaluate(new OccurrenceEvent(OccurrenceEvent.Type.Open, "bad", "doc", 1, false)));
		assertFalse(xpathBasic.evaluate(new OccurrenceEvent(OccurrenceEvent.Type.Close, "bad", "doc", 1, false)));
		assertFalse(xpathBasic.evaluate(new OccurrenceEvent(OccurrenceEvent.Type.Open, "b", "doc", 1, false)));
		assertTrue(xpathBasic.evaluate(new OccurrenceEvent(OccurrenceEvent.Type.Open, "c", "doc", 2, false)));
		
		//html
		assertFalse(xpathBasic.evaluate(new OccurrenceEvent(OccurrenceEvent.Type.Open, "A", "doc2", 0, true)));
		assertFalse(xpathBasic.evaluate(new OccurrenceEvent(OccurrenceEvent.Type.Open, "B", "doc2", 1, true)));
		assertTrue(xpathBasic.evaluate(new OccurrenceEvent(OccurrenceEvent.Type.Open, "C", "doc2", 2, true)));
	
		// with content filter in the end
		System.out.println();
		XPathNode xpathText = new XPathNode("/a/b[text  (  )  =  \"theEntireText\"  ]");
	
		assertFalse(xpathText.evaluate(new OccurrenceEvent(OccurrenceEvent.Type.Open, "a", "doc", 0, false)));
		assertFalse(xpathText.evaluate(new OccurrenceEvent(OccurrenceEvent.Type.Open, "b", "doc", 1, false)));
		assertTrue(xpathText.evaluate(new OccurrenceEvent(OccurrenceEvent.Type.Text, "theEntireText", "doc", 2, false)));
				
		System.out.println();
		XPathNode xpathContains = new XPathNode("/xyz/abc[contains(text(),  \"someSubstring\")]");
		
		assertFalse(xpathContains.evaluate(new OccurrenceEvent(OccurrenceEvent.Type.Open, "xyz", "doc", 0, false)));
		assertFalse(xpathContains.evaluate(new OccurrenceEvent(OccurrenceEvent.Type.Open, "abc", "doc", 1, false)));
		assertTrue(xpathContains.evaluate(new OccurrenceEvent(OccurrenceEvent.Type.Text, "xxsomeSubstringxx", "doc", 2, false)));
		
		// with content filter in the middle
		System.out.println();
		XPathNode xpathTextMiddle = new XPathNode("/d/e/foo[text(  )=  \"something\"]/bar");
		
		assertFalse(xpathTextMiddle.evaluate(new OccurrenceEvent(OccurrenceEvent.Type.Open, "d", "doc", 0, false)));
		assertFalse(xpathTextMiddle.evaluate(new OccurrenceEvent(OccurrenceEvent.Type.Open, "e", "doc", 1, false)));
		assertFalse(xpathTextMiddle.evaluate(new OccurrenceEvent(OccurrenceEvent.Type.Open, "foo", "doc", 2, false)));
		assertFalse(xpathTextMiddle.evaluate(new OccurrenceEvent(OccurrenceEvent.Type.Open, "foofoo", "doc", 1, false)));
		assertFalse(xpathTextMiddle.evaluate(new OccurrenceEvent(OccurrenceEvent.Type.Text, "something", "doc", 3, false)));
		assertTrue(xpathTextMiddle.evaluate(new OccurrenceEvent(OccurrenceEvent.Type.Open, "bar", "doc", 4, false)));
	
	}
	/***
	 * 5 tests in a single test method
	 * */
	@Test
	public void testXPathEngineImpl() {
		XPathEngineImpl engine =  new XPathEngineImpl();
		List<String> xpaths = new ArrayList<String>();
		xpaths.add("/a/b/c");
		xpaths.add("/a/b[text  (  )  =  \"theEntireText\"  ]");
		xpaths.add("/xyz/abc[contains(text(),  \"someSubstring\")]");
		xpaths.add("/d/e/foo[text(  )=  \"something\"]/bar");
		engine.setXPaths(xpaths.toArray(new String[xpaths.size()]));
		engine.evaluateEvent(new OccurrenceEvent(OccurrenceEvent.Type.Open, "a", "doc", 0, false));
		engine.evaluateEvent(new OccurrenceEvent(OccurrenceEvent.Type.Open, "d", "doc", 1, false));
		engine.evaluateEvent(new OccurrenceEvent(OccurrenceEvent.Type.Open, "d", "doc", 1, false));
		engine.evaluateEvent(new OccurrenceEvent(OccurrenceEvent.Type.Open, "b", "doc", 1, false));
		boolean[] result = engine.evaluateEvent(new OccurrenceEvent(OccurrenceEvent.Type.Open, "c", "doc", 2, false));
		assertTrue(result[0]);
		
		engine.evaluateEvent(new OccurrenceEvent(OccurrenceEvent.Type.Open, "a", "doc2", 0, false));
		engine.evaluateEvent(new OccurrenceEvent(OccurrenceEvent.Type.Open, "d", "doc2", 1, false));
		engine.evaluateEvent(new OccurrenceEvent(OccurrenceEvent.Type.Open, "d", "doc2", 1, false));
		engine.evaluateEvent(new OccurrenceEvent(OccurrenceEvent.Type.Open, "B", "doc2", 1, false));
		boolean[] result2 = engine.evaluateEvent(new OccurrenceEvent(OccurrenceEvent.Type.Open, "c", "doc2", 2, false));
		assertFalse(result2[0]);
		
		engine.evaluateEvent(new OccurrenceEvent(OccurrenceEvent.Type.Open, "a", "doc3", 0, false));
		engine.evaluateEvent(new OccurrenceEvent(OccurrenceEvent.Type.Open, "b", "doc3", 1, false));
		boolean[] result3 = engine.evaluateEvent(new OccurrenceEvent(OccurrenceEvent.Type.Text, "theEntireWrongText", "doc3", 2, false));
		assertFalse(result3[1]);
		result3 = engine.evaluateEvent(new OccurrenceEvent(OccurrenceEvent.Type.Open, "theEntireText", "doc3", 2, false));
		assertTrue(result3[1]);		
		
		engine.evaluateEvent(new OccurrenceEvent(OccurrenceEvent.Type.Open, "xyz", "doc", 0, false));
		engine.evaluateEvent(new OccurrenceEvent(OccurrenceEvent.Type.Open, "abc", "doc", 1, false));
		boolean[] result4 = engine.evaluateEvent(new OccurrenceEvent(OccurrenceEvent.Type.Text, "xxsomeSubstringxx", "doc", 2, false));
		assertTrue(result4[2]);	
		
		engine.evaluateEvent(new OccurrenceEvent(OccurrenceEvent.Type.Open, "d", "doc", 0, false));
		engine.evaluateEvent(new OccurrenceEvent(OccurrenceEvent.Type.Open, "e", "doc", 1, false));
		engine.evaluateEvent(new OccurrenceEvent(OccurrenceEvent.Type.Open, "foo", "doc", 2, false));
		engine.evaluateEvent(new OccurrenceEvent(OccurrenceEvent.Type.Open, "foofoo", "doc", 1, false));
		engine.evaluateEvent(new OccurrenceEvent(OccurrenceEvent.Type.Text, "something", "doc", 3, false));
		boolean[] result5 = engine.evaluateEvent(new OccurrenceEvent(OccurrenceEvent.Type.Open, "bar", "doc", 4, false));
		assertTrue(result5[3]);
		
	}

}

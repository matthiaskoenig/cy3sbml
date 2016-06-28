package org.cy3sbml.oven;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.JSBML;
import org.sbml.jsbml.SBMLDocument;

public class TestMaxElementDepth {
	public static final String JDK_MAX_ELEMENT_DEPTH = "jdk.xml.maxElementDepth";
	
	
	public static void main(String[] args) throws XMLStreamException, IOException{

		/*
		A new property, maxElementDepth, is added to provide applications the ability to set limit on maximum element depth in an xml file that they parse. This may be helpful for applications that may use too much resources when processing an xml file with excessive element depth.

	    Name: http://java.sun.com/xml/jaxp/properties/maxElementDepth
	    Definition: Limit the maximum element depth
	    Value: A positive integer. 0 is treated as no limit. Negative numbers are treated as 0.
	    Default value: 0
	    System property: jdk.xml.maxElementDepth
		**/
		
		System.setProperty(JDK_MAX_ELEMENT_DEPTH, "5000");
		String path = "/home/mkoenig/git/cy3sbml/src/test/resources/models/BiGG/iMM1415.xml";
		
		// from file okay
		SBMLDocument doc = JSBML.readSBMLFromFile(path);
		
		System.out.println("maxElementDepth: " + System.getProperty(JDK_MAX_ELEMENT_DEPTH));
		System.setProperty("http://www.oracle.com/xml/jaxp/properties/getEntityCountInfo", "yes");
	}
	
}


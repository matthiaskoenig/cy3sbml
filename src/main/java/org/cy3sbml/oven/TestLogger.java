package org.cy3sbml.oven;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestLogger {

	private static Logger logger;
	
	public static void main(String[] args){
		logger = LoggerFactory.getLogger(TestLogger.class);
		System.out.println("Hello world!");
		logger.error("log message");
	}
	
}

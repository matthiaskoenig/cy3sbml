package org.cy3sbml;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * Necessary to store the information interested in, because bc stops to exist at some point.
 * Access to full manifest information and registered services is possible via the bundle.
 */
public class BundleInformation {
	private String name;
	private String version;
	
	/*
	private static BundleInformation uniqueInstance;
	public static synchronized BundleInformation getInstance(BundleContext bc){
		if (uniqueInstance == null){
			uniqueInstance = new BundleInformation(bc);
		}
		return uniqueInstance;
	}
	public static synchronized BundleInformation getInstance(){
		return uniqueInstance;
	}
	*/
	
	
	public BundleInformation(BundleContext bc){
		Bundle bundle = bc.getBundle();
		name = bundle.getSymbolicName();
		version = bundle.getVersion().toString();
	}
	
	public String getInfo(){
		return getName() + "-v" + getVersion();
	}
	public String getName(){
		return name;
	}
	public String getVersion(){
		return version;
	}
}

package org.cy3sbml.archive;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * The BundleInformation provides runtime information on the bundle.
 * 
 * Important things like 
 * - name
 * - version
 * - dependencies
 * can be accessed and used programmatically.
 * 
 * All String generation using names and versions should use the 
 * BundleInformation directly.
 */
public class BundleInformation {
	private String name;
	private String version;
	
	/** Constructor. */
	public BundleInformation(BundleContext bc){
		Bundle bundle = bc.getBundle();
		name = bundle.getSymbolicName();
		version = bundle.getVersion().toString();
	}
	
	/** {name}-v{version} of bundle. */
	public String getInfo(){
		return getName() + "-v" + getVersion();
	}
	
	/** Name of bundle. */
	public String getName(){
		return name;
	}
	
	/** Version of bundle. */
	public String getVersion(){
		return version;
	}
}

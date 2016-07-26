package org.cy3sbml;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Enumeration;

/**
 * Class extracts the bundle resources to given app directory.
 * This provides access to the local resources via 
 * file:// uris. 
 * Required to allow JavaFX access to bundle resources. JavaFX does currently
 * not support the access via bundle: uris.
 */
public class ResourceExtractor {
	private static Logger logger = LoggerFactory.getLogger(ResourceExtractor.class);
	
	/** Resources made available via the ResourceExtractor. */
	public final String GUI_RESOURCES = "/gui/";   
	
	private static File appDirectory;
	private final BundleContext bc;

	public ResourceExtractor(final BundleContext bc, final File appDirectory) {
		this.bc = bc;
		setAppDirectory(appDirectory);
	}
	
	public static void setAppDirectory(File appDirectory){
		ResourceExtractor.appDirectory = appDirectory;
	}
	
	/** 
	 * Replacement of getClass().getResource("/gui/info.html");
	 * which does not work for bundle resources in context of JavaFX. 
	 */
	public static String getResource(String resource){
		return fileURIforResource(resource);
	}
	
	/**
	 * Returns the file URI in the application folder
	 * for the given resource string.
	 * 
	 * For instance "/gui/query.html" 
	 */
	public static String fileURIforResource(String resource){
		File file = new File(appDirectory + resource);
		if (!file.exists()){
			System.out.println("ERROR: Resource is not available");
		}
		URI fileURI = file.toURI();
		
		return fileURI.toString();
	}
	
	/** 
	 * Extracts the bundle resources from the BundleContext in the 
	 * application directory.
	 * 
	 * BundleContext and application directory have to be provided.
	 */
	public void extract(){
		if (bc == null || appDirectory == null){
			System.out.println("WARNING BundleContext or application directory not set. " +
					"Files not extracted");
			return;
		}
		logger.info("-------------------------------------------------");
		logger.info("Extract bundle resources");
		logger.info("-------------------------------------------------");
		// bundle root
		Bundle bundle = bc.getBundle();
		URL rootURL = bundle.getEntry("/");
		logger.info("bundle root: " + rootURL);
		
		/* FIXME: we don't care about existing resources, just overwriting them
		 * This will accumulate files with versions and should be cleaned up.
		 * Also copying everything on every bundle startup is overkill.
		 * 
		 // Delete if resources are already available
		 if(destination.exists()) {
				// Maybe there is an old version
				final File versionFile = new File(destination, VERSION_NAME);
				if(!versionFile.exists()) {
					logger.info("Version file not found.  Creating new preview template...");
					deleteAll(destination);
		*/
		extractDirectory(rootURL, GUI_RESOURCES);
		logger.info("-------------------------------------------------");
	}
	
	/* 
	 * Extract the resources in given directory.  
	 */
	private void extractDirectory(URL rootURL, String directory){
		// list all GUI resources of bundle and extract them
		Enumeration<String> e = bc.getBundle().getEntryPaths(directory);
		
		while(e.hasMoreElements()){
			String path = e.nextElement();
				
			// copy via stream from bundle URL to application file
			try {
				URL inURL = new URL(rootURL.toString() + path);

				try {
					InputStream inputStream = inURL.openConnection().getInputStream();
					File outFile = new File(appDirectory + "/" + path);
					// create directory
					if (path.endsWith("/")){
						outFile.mkdirs();
						// extract subdirectory recursively
						extractDirectory(rootURL, "/" + path);
						
					}else{
						// create directories for file if required
						File parent = outFile.getParentFile();
						if (!parent.exists() && !parent.mkdirs()){
						    throw new IllegalStateException("Couldn't create dir: " + parent);
						}
						
						logger.info(" --> " + outFile.getAbsolutePath());
						OutputStream outputStream = new FileOutputStream(outFile);
				
						int read = 0;
						byte[] bytes = new byte[1024];
				
						while ((read = inputStream.read(bytes)) != -1) {
								outputStream.write(bytes, 0, read);
						}
						outputStream.close();
					}
				} catch (IOException ioException) {
					ioException.printStackTrace();
					return;
				}
				
				
			} catch (MalformedURLException urlException) {
				urlException.printStackTrace();
				return;
			}	
		}
		
	}	
}

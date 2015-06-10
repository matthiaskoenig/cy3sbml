package org.cy3sbml.miriam;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.apache.commons.lang.StringUtils;


import uk.ac.ebi.miriam.lib.MiriamLink;


/** Handling the MiriamResource Information for given entries.
 * Data is cached to reduce the webservice overhead.
 */
public class MiriamResourceInfo {
	private static CacheManager cacheManager;
	private static Cache miriamCache; 
	
	// Cache Configuration
	static {
		// Create a singleton CacheManager using defaults, then list caches.
		
		// String[] cacheNames = CacheManager.getInstance().getCacheNames();
		//URL url = getClass().getResource("/anotherconfigurationname.xml");
		//CacheManager manager = CacheManager.newInstance(url);
		cacheManager = CacheManager.create();
		
		//Create a Cache and add it to the CacheManager, then use it. Note that Caches are not usable until they have been added to a CacheManager.
		// Memory only Cache
		miriamCache = new Cache("miriamCache", 5000, false, true, 6000, 6000);
		cacheManager.addCache(miriamCache);
		
		
		// Shutdown after use 
		//CacheManager.getInstance().shutdown();
		
	}

	public static String getInfoFromMiriamResource(MiriamLink link, String resourceURI) {
		String text = "";
		
		// Get the locations from 
		String[] locations = getLocationsFromMiriamResource(link, resourceURI); 
		if (locations != null){
			for (String location : locations) {
				text += String.format("<a href=\"%s\">%s</a><br>", 
										location, parseServerFromLocation(location));
			}
		}
		return text;
	}

	/** Parses the server information from the location String. */
	private static String parseServerFromLocation(String location) {
		String[] items = location.split("/");
		String[] serveritems = new String[items.length - 1];
		for (int i = 0; i < serveritems.length; ++i) {
			serveritems[i] = items[i];
		}
		String server = StringUtils.join(serveritems, "/");
		return server;
	}
	
	
	/** Here the Miriam calls are made which are cashed. */
	public static String[] getLocationsFromMiriamResource(MiriamLink link, String resourceURI){
		String[] locations = null;
		
		// check in cache
		Element element = miriamCache.get(resourceURI);
		if (element != null){
			locations = (String[]) element.getObjectValue();
			CySBML.LOGGER.info("cached: " + resourceURI);
		} else {
			locations = link.getLocations(resourceURI);
			if (locations != null){
				// update the cache
				element = new Element(resourceURI, locations);
				miriamCache.put(element);
				CySBML.LOGGER.info("miriamCache added: " + resourceURI);
			} else {
				CySBML.LOGGER.warning("Miriam locations could not be retrieved: " + resourceURI);
			}
		}
		return locations; 
	}
	
}

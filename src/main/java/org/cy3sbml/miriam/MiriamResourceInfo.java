package org.cy3sbml.miriam;

import java.util.Arrays;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.miriam.lib.MiriamLink;


/** Handling the MiriamResource Information for given entries.
 * Data is cached in memory cache based on ehcache to reduce 
 * the webservice overhead and minimize the calls to MIRIAM.
 */
public class MiriamResourceInfo {
	private static final Logger logger = LoggerFactory.getLogger(MiriamResourceInfo.class);
	
	private static CacheManager cacheManager;
	private static Cache miriamCache; 
	
	// Cache Configuration
	static {
		// Create a singleton CacheManager using defaults
		cacheManager = CacheManager.create();		
		// Memory only Cache
		miriamCache = new Cache("miriamCache", 5000, false, true, 6000, 6000);
		cacheManager.addCache(miriamCache);
	}

	public static String getInfoFromURI(MiriamLink link, String resourceURI) {
		String text = "";
		String[] locations = getLocationsFromURI(link, resourceURI); 
		if (locations != null){
			String[] items = new String[locations.length];
			for (int k=0; k<locations.length; k++) {
				String location = locations[k];
				items[k] = String.format("<a href=\"%s\">%s</a><br>", location, serverFromLocation(location));
			}
			text = StringUtils.join(items, "");
		} else {
			logger.warn("No locations for URI: " + resourceURI);
		}
		return text;
	}

	private static String serverFromLocation(String location) {
		// get everything instead of the last item 
		String[] items = location.split("/");
		String[] serverItems = Arrays.copyOfRange(items, 0, items.length-1);
		return StringUtils.join(serverItems, "/"); 
	}
	
	/** Get MIRIAM information from cache or via webservice lookup. */
	public static String[] getLocationsFromURI(MiriamLink link, String resourceURI){
		String[] locations = null;
		
		// check in cache
		Element element = miriamCache.get(resourceURI);
		if (element != null){
			logger.debug("cached: " + resourceURI);
			locations = (String[]) element.getObjectValue();
		} else {
			logger.info("Webservice lookup: " + resourceURI);
			locations = link.getLocations(resourceURI);
			if (locations != null){
				// update the cache
				element = new Element(resourceURI, locations);
				miriamCache.put(element);
				logger.debug("Added to cache: " + resourceURI);
			} else {
				logger.warn("Miriam locations could not be retrieved: " + resourceURI);
			}
		}
		return locations; 
	}	
}

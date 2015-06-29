package org.cy3sbml.miriam;

import java.util.Arrays;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.miriam.lib.MiriamLink;


/** Handling the MiriamResource Information for given entries.
 * Data is cached in memory cache based on ehcache to reduce 
 * the webservice overhead and minimize the calls to MIRIAM.
 * 
 * Repeated queries are fetched from cache. 
 * Provides fast access to webservice information in combination with preloading of resources
 * during loading of files.
 */
public class MiriamResourceInfo {
	private static final Logger logger = LoggerFactory.getLogger(MiriamResourceInfo.class);
	
	private static CacheManager cacheManager;
	private static Cache miriamCache;
	
	static {
		// Create a singleton CacheManager using defaults
		cacheManager = CacheManager.create();
		
		// Cache configuration
		// memory cache with overflow to disk (java.io.tmpdir)
		CacheConfiguration config = new CacheConfiguration();
		config.setName("miriamCache");
		config.setMaxEntriesLocalHeap(5000);
		// which lives eternal (lifetime of Cytoscape session)
		// Note that the eternal attribute, when set to "true", overrides timeToLive 
		// and timeToIdle so that no expiration can take place.
		config.setEternal(true);
		
		// Create and add the cache
		miriamCache = new Cache(config);
		cacheManager.addCache(miriamCache);
	}

	public static String getInfoFromURI(MiriamLink link, String resourceURI) {
		// logger.info(resourceURI);
		String text = "";
		String[] locations = getLocationsFromURI(link, resourceURI); 
		if (locations != null){
			if (locations.length == 0){
				logger.warn("No locations for URI:" + resourceURI);
			}
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
	
	/** Get MIRIAM information from cache or via web service lookup. */
	public static String[] getLocationsFromURI(MiriamLink link, String resourceURI){
		String[] locations = null;
		
		// check in cache
		Element element = miriamCache.get(resourceURI);
		if (element != null){
			logger.debug("cached: " + resourceURI);
			locations = (String[]) element.getObjectValue();
		} else {
			logger.debug("Webservice lookup: " + resourceURI);
			locations = link.getLocations(resourceURI);
			
			if (locations != null){
				// update the cache
				element = new Element(resourceURI, locations);
				miriamCache.put(element);
				logger.debug("Added to cache: " + resourceURI);
			} else {
				// TODO: currently problems if collection only has one resource
				logger.debug("Miriam locations could not be retrieved: " + resourceURI);
			}
		}
		return locations; 
	}	
}

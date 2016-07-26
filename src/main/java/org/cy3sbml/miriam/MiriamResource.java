package org.cy3sbml.miriam;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;

import org.cy3sbml.miriam.registry.RegistryLocalProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles the MiriamResource Information for given resourceURIs.
 *
 * The data is cached in memory cache using ehcache. This was
 * initially implemented to reduce minimize the web service calls to MIRIAM.
 * Provides fast access to webservice information in combination with preloading of resources
 * during loading of files. The created information objects are cached to reduce load on the
 * web services.
 *
 * All information is now retrieved from local  MIRIAM xml file which is available from
 *  http://www.ebi.ac.uk/miriam/main/export/xml/
 */
public class MiriamResource {
	private static final Logger logger = LoggerFactory.getLogger(MiriamResource.class);
	
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

	/**
     * Get locations for given URI.
     *
     * MIRIAM information is loaded from cache if existing.
     * If not a lookup via the local RegistryDatabase is performed.
	 */
	public static String[] getLocationsFromURI(String resourceURI){
		String[] locations = null;
		
		// check in cache
		Element element = miriamCache.get(resourceURI);
		if (element != null){
			logger.debug("cached: " + resourceURI);
			locations = (String[]) element.getObjectValue();
		}

		// not in cache, lookup element
		else {
            locations = new RegistryLocalProvider().getLocations(resourceURI);
            // update the cache
			if (locations != null){
				element = new Element(resourceURI, locations);
				miriamCache.put(element);
				logger.debug("Added to cache: " + resourceURI);
			} else {
				logger.debug("Miriam locations could not be retrieved: " + resourceURI);
			}
		}
		return locations; 
	}	
}

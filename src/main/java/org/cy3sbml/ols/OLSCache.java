package org.cy3sbml.ols;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Caching OLS information.
 *
 *  The data is cached in memory cache using ehcache. This was
 * initially implemented to reduce minimize the web service calls to MIRIAM.
 * Provides fast access to webservice information in combination with preloading of resources
 * during loading of files. The created information objects are cached to reduce load on the
 * web services.
 */
public class OLSCache {
    private static final Logger logger = LoggerFactory.getLogger(OLSCache.class);

    private static CacheManager cacheManager;
    private static Cache cache;

    static {
        // Create a singleton CacheManager using defaults
        cacheManager = CacheManager.create();

        // Cache configuration
        // memory cache with overflow to disk (java.io.tmpdir)
        CacheConfiguration config = new CacheConfiguration();
        config.setName("cache");
        config.setMaxEntriesLocalHeap(5000);
        // which lives eternal (lifetime of Cytoscape session)
        // Note that the eternal attribute, when set to "true", overrides timeToLive
        // and timeToIdle so that no expiration can take place.
        config.setEternal(true);

        // Create and add the cache
        cache = new Cache(config);
        cacheManager.addCache(cache);
    }

    /**
     * Get OLS object for given URI.
     * Unique URI is used for caching.
     */
    public static OLSObject getOLSObject(String resourceURI){
        OLSObject olsObject;

        // check in cache
        Element element = cache.get(resourceURI);
        if (element != null){
            logger.debug("cached: " + resourceURI);
            olsObject = (OLSObject) element.getObjectValue();
        }
        // not in cache, lookup element
        else {
            olsObject = new OLSObject(resourceURI);
            // update the cache
            if (olsObject != null){
                element = new Element(resourceURI, olsObject);
                cache.put(element);
                logger.debug("Added to cache: " + resourceURI);
            } else {
                logger.debug("Miriam locations could not be retrieved: " + resourceURI);
            }
        }
        return olsObject;
    }
}

package org.cy3sbml.ols;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.utilities.ols.web.service.model.Term;


/**
 * Caching OLS information.
 *
 * The data is cached in memory cache using ehcache.
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
    public static Term getTerm(String identifier){
        Term term;

        // check in cache
        Element element = cache.get(identifier);
        if (element != null){
            logger.debug("Term found in cache: " + identifier);
            term = (Term) element.getObjectValue();
        }
        // not in cache, lookup element
        else {
            term = OLSAccess.getTerm(identifier);
            // update the cache
            if (term != null){
                element = new Element(identifier, term);
                cache.put(element);
                logger.debug("Put term in cache: " + identifier);
            } else {
                logger.debug("Term could not be retrieved: " + identifier);
            }
        }
        return term;
    }
}

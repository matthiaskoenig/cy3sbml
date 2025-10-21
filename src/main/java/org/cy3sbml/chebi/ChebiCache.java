package org.cy3sbml.chebi;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Cache ChebiHTML for accessions.
 */
public class ChebiCache {
    private static final Logger logger = LoggerFactory.getLogger(ChebiCache.class);

    private static CacheManager cacheManager;
    private static Cache cache;

    static {
        // Create singleton CacheManager using defaults
        cacheManager = CacheManager.create();
        CacheConfiguration config = new CacheConfiguration();
        config.setName("ChebiCache");
        config.setMaxEntriesLocalHeap(5000);
        config.setEternal(true);

        cache = new Cache(config);
        cacheManager.addCache(cache);
    }

    /**
     * Get ChebiEntry with cache support.
     *
     * @param accession chebi accession id, e.g. "15377"
     * @return
     */
    public static String getChebiHTML(String accession) {
        String entry;

        // check in cache
        Element element = cache.get(accession);
        if (element != null) {
            logger.debug("ChebiHTML in cache: " + accession);
            entry = (String) element.getObjectValue();
        }
        // not in cache, lookup element
        else {
            entry = ChebiAccess.getChebiHTML(accession);
            // update the cache
            if (entry != null) {
                element = new Element(accession, entry);
                cache.put(element);
                logger.debug("Put in cache: " + accession);
            } else {
                logger.debug("Object could not be retrieved: " + accession);
            }
        }
        return entry;
    }

    public static void main(String[] args) {
        // Test the chebi access
        String identifier = "CHEBI:15377";
        String html = getChebiHTML(identifier);
        System.out.println(html);

    }
}

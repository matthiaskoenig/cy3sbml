package org.cy3sbml.uniprot;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;

import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cache UniProtEntry for accessions.
 */
public class UniprotCache {
    private static final Logger logger = LoggerFactory.getLogger(org.cy3sbml.ols.OLSCache.class);

    private static CacheManager cacheManager;
    private static Cache cache;

    static {
        // Create singleton CacheManager using defaults
        cacheManager = CacheManager.create();

        // Cache configuration
        // memory cache with overflow to disk (java.io.tmpdir)
        CacheConfiguration config = new CacheConfiguration();
        config.setName("UniprotCache");
        config.setMaxEntriesLocalHeap(5000);
        config.setEternal(true);

        cache = new Cache(config);
        cacheManager.addCache(cache);
    }

    /**
     * Get UniProtEntry with cache support.
     * @param accession uniprot accession id, e.g. "P10415"
     * @return
     */
    public static UniProtEntry getUniProtEntry(String accession){
        UniProtEntry entry;

        // check in cache
        Element element = cache.get(accession);
        if (element != null){
            logger.debug("UniProtEntry in cache: " + accession);
            entry = (UniProtEntry) element.getObjectValue();
        }
        // not in cache, lookup element
        else {
            entry = UniprotAccess.getUniProtEntry(accession);
            // update the cache
            if (entry != null){
                element = new Element(accession, entry);
                cache.put(element);
                logger.debug("Put in cache: " + accession);
            } else {
                logger.debug(String.format("Object could not be retrieved: ", accession));
            }
        }
        return entry;
    }
}

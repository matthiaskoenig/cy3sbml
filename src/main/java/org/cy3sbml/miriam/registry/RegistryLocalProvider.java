
package org.cy3sbml.miriam.registry;


import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Set;

import org.apache.log4j.Logger;
import org.cy3sbml.miriam.registry.data.DataType;
import org.cy3sbml.miriam.registry.data.PhysicalLocation;

/**
 * Identifiers.org Registry Local Services provider.
 *
 * Provides access to the data.
 */
public class RegistryLocalProvider {
	private Logger logger = Logger.getLogger(RegistryLocalProvider.class);

	/** version of the latest Java library */
	private final String JAVA_LIBRARY_VERSION = "20130212";

	
	/** Creates a new getInstance() of {@link RegistryLocalProvider}. */
	public RegistryLocalProvider() {
        logger.debug("RegistryLocalProvider Constructor");        
	}
	
	
	/** Retrieve some information about these Services. */
	public String getServicesInfo() {
	    return "Identiers.org Registry Local Services. For more information: http://identifiers.org/registry/. " +
	    		"Developed and maintained by the BioModels.net team (biomodels-net-support@lists.sf.net).";
	}
	
	
	/** Retrieves the current version of MIRIAM Registry Local Services. */
	public String getServicesVersion()
	{
	    return JAVA_LIBRARY_VERSION;
	}

	/**
	 * Retrieves the latest version of the Java library available.
	 * @return latest version of the Java library available  
	 */
	public String getJavaLibraryVersion()
	{
	    return JAVA_LIBRARY_VERSION;
	}
	
	
	/**
	 * Retrieves the unique official URN of a data collection (example: <em>urn:miriam:uniprot</em>).
	 * 
	 * @param name name primary name (or synonym) of a data collection (examples: "ChEBI", "UniProt", "GO")
	 * @return unique (official) URN of the data collection
	 * @deprecated Use {@link #getDataTypeURI(String)} instead.
	 */
	public String getDataTypeURN(String name) {
		logger.debug("Call of the (deprecated) 'GetDataTypeURN()' request...");
	    DataType dataType = getDataType(name);
		
	    if (dataType != null) {
			return dataType.getURN();	    	
	    }
        return null;
	}
	

	/**
	 * Retrieves the unique (official) URN of the data collection (example: "urn:miriam:uniprot") and all the deprecated ones.
	 *
	 * @param name name or synonym of a data collection (examples: "ChEBI", "UniProt")
	 * @return unique URN and all the deprecated ones
	 * @deprecated Use {@link #getDataTypeURIs(String)} instead.
	 */
	@Deprecated
	public String[] getDataTypeURNs(String name)
	{
		logger.debug("Call of the 'GetDataTypeURNs()' request...");
		String[] result = null;

	    DataType dataType = getDataType(name);

	    if (dataType != null)
	    {
	    	ArrayList<String> urns = new ArrayList<String>();

	    	urns.add(dataType.getURN());

	    	for (String deprecatedURI : dataType.getDeprecatedURIs())
	    	{
	    		if (RegistryUtilities.isURN(deprecatedURI))
	    		{
	    			urns.add(deprecatedURI);
	    		}
	    	}

	    	result = urns.toArray(new String[urns.size()]);
	    }

		return result;
	}


	/**
	 * Retrieves the unique (official) URL (not a physical URL but a URI) of the data collection (example: "http://identifiers.org/taxonomy").
	 *
	 * @param name name of a data collection (examples: "ChEBI", "UniProt")
	 * @return unique URL of the data collection
	 * @deprecated Use {@link #getDataTypeURI(String)} instead.
	 */
	@Deprecated
	public String getDataTypeURL(String name)
	{
		logger.debug("Call of the 'GetDataTypeURL()' request...");

		return getDataTypeURI(name);
	}


	/**
	 * Retrieves the unique (official) URL (not a physical URL but a URI) of the data collection (example: "http://identifiers.org/taxonomy") and all the deprecated ones.
	 *
	 * @param name name of a data collection (examples: "ChEBI", "UniProt")
	 * @return unique URL of the data collection and all the deprecated ones
	 * @deprecated You can use {@link #getDataTypeURIs(String)} instead.
	 */
	@Deprecated
	public String[] getDataTypeURLs(String name)
	{
		logger.debug("Call of the 'GetDataTypeURLs()' request...");
		String[] result = null;

	    DataType dataType = getDataType(name);

	    if (dataType != null)
	    {
	    	ArrayList<String> urns = new ArrayList<String>();

	    	urns.add(dataType.getURL());

	    	for (String deprecatedURI : dataType.getDeprecatedURIs())
	    	{
	    		if (RegistryUtilities.isURL(deprecatedURI))
	    		{
	    			urns.add(deprecatedURI);
	    		}
	    	}

	    	result = urns.toArray(new String[urns.size()]);
	    }

		return result;
	}


	/**
	 * Retrieves the unique (official) URL or URN of the data collection (example: "http://identifiers.org/taxonomy", "urn:miriam:uniprot").
	 * 
	 * @param name name of the data collection (examples: "ChEBI", "UniProt")
	 * @param type type of the URI the user wants to retrieve ('URN' or 'URL')
	 * @return unique URL or URN of the data collection
	 * 
	 */
	public String getDataTypeURI(String name, String type)
	{
		logger.debug("Call of the 'GetDataTypeURI()' request...");
		String result = null;
		
		if (type.equalsIgnoreCase("URN"))
		{
			result = getDataTypeURN(name);
		}
		// URL is the default (even in case of an empty "type" field)
		else
		{
			result = getDataTypeURI(name);
		}
		
		return result;
	}
	
	
	 /**
     * Retrieves the unique (official) URI of a data collection (example: http://identifiers.org/taxonomy).
     * 
     * @param name name, synonym, id or URI of a data collection (examples: "UniProt", "MIR:00000005") and a URI (for example "urn:miriam:uniprot") 
     * @return unique URI of the data collection
     */
    public String getDataTypeURI(String name)
    {
    	
	    DataType dataType = getDataType(name);

	    if (dataType != null) 
	    {
			return dataType.getURL();
	    }
	    
        return null;
    }


    /**
     * Retrieves all the information about a data collection.
     * 
     * @param name identifier(for example "MIR:00000005"), name, synonym or a URI  of a data collection 
     * @return <code>DataType</code> object containing all the information relative to the given identifier
     */
	private DataType getDataType(String name) 
	{
		return RegistryUtilities.getDataType(name);
	}


	/**
     * Retrieves all the URLs or URNs of the data collection (examples: "urn:miriam:uniprot", "http://identifiers.org/taxonomy") including all the deprecated ones.
     *
     * @param name name of the data collection (examples: "ChEBI", "UniProt")
     * @param type type of the URI the user wants to recover ('URN' or 'URL')
     * @return all the URIs (URLs or URNs) of the data collection including all the deprecated ones
     * @deprecated Use {@link #getDataTypeURIs(String)} instead.
	 */
    @Deprecated
	public String[] getDataTypeURIs(String name, String type)
	{
		logger.debug("Call of the 'GetDataTypeURIs(String, String)' request...");
		String[] result = null;

		if (type.equalsIgnoreCase("URN"))
		{
			result = getDataTypeURNs(name);
		}
		// URL is the default (even in case of an empty "type" field)
		else
		{
			result = getDataTypeURLs(name);
		}

		return result;
	}


    /**
     * Retrieves all the URIs of a data collection, including all the deprecated ones (examples: "http://identifiers.org/uniprot", "urn:miriam:uniprot", 
     * "http://www.uniprot.org/", "urn:lsid:uniprot.org:uniprot", ...).
     * 
     * @param name name, synonym, id or URI of a data collection (examples: "UniProt", "MIR:00000005") and a URI (for example "urn:miriam:uniprot")
     * @return all the URIs of a data collection (including the deprecated ones)
     */
    public String[] getDataTypeURIs(String name)
    {        
	    DataType dataType = getDataType(name);

	    if (dataType != null) 
	    {
	    	ArrayList<String> urns = new ArrayList<String>();
	    	
	    	urns.add(dataType.getURN());
	    	urns.add(dataType.getURL());
	    	
	    	for (String deprecatedURI : dataType.getDeprecatedURIs())
	    	{
	    		urns.add(deprecatedURI);
	    	}
	    	
	    	return urns.toArray(new String[urns.size()]);
	    }

	    return null;
    }


	/**
	 * Retrieves all the URIs (URNs and URLs) of the data collection
	 *
	 * @param name name of a data collection (examples: "ChEBI", "UniProt")
	 * @return all the URIs (URLs and URNs) of the data collection including all the deprecated ones
	 * @deprecated Use {@link #getDataTypeURIs(String)} instead.
	 */
    @Deprecated
	public String[] getDataTypeAllURIs(String name)
	{
		logger.debug("Call of the 'GetDataTypeAllURIs()' request...");

		return getDataTypeURIs(name);
	}


	/**
	 * Retrieves the unique URN of the data-entry (example: "urn:miriam:uniprot:P62158").
	 *
	 * @param name name of a data collection (examples: "ChEBI", "UniProt")
	 * @param id identifier of an element (examples: "GO:0045202", "P62158")
	 * @return unique URN of the data-entry
	 * @deprecated Use {@link #getURI(String, String)} instead.
	 */
    @Deprecated
	public String getURN(String name, String id)
	{
		logger.debug("Call of the 'GetURN()' request...");
		String result = new String();

		result = getDataTypeURN(name);
		// result is empty
		if (result.equalsIgnoreCase(""))
		{
			logger.warn("No URN for the data collection named: " + name);
		}
		else
		{
			try
            {
                result += ":" + URLEncoder.encode(id, "UTF-8");
            }
            catch (UnsupportedEncodingException e)
            {
                logger.error("An UnsupportedEncodingException exception occurred while encoding the identifier: '" + id + "'!");
                logger.error(e.getMessage());
            }
		}

		return result;
	}


	/**
	 * Retrieves unique URL of the data-entry (example: "urn:miriam:obo.go:GO%3A0045202").
	 *
	 * @param name name of a data collection (examples: "ChEBI", "UniProt")
	 * @param id identifier of an element (examples: "GO:0045202", "P62158")
	 * @return unique URN of the data-entry
	 * @deprecated Use {@link #getURI(String, String)} instead.
	 */
	@Deprecated
	public String getURL(String name, String id)
	{
		logger.debug("Call of the 'GetURL()' request...");
		String result = new String();

		DataType dataType = getDataType(name);

	    if (dataType == null)
	    {
			logger.debug("No URL for the data collection named: " + name);
		}
		else if (RegistryUtilities.checkRegexp(id, dataType.getRegexp()))
		{
			result = dataType.getURL() + id; // identifiers.org contain already a trailing '/'
		}
		else
		{
			result = null;
		}

		return result;
	}


	/**
	 * Retrieves the unique URI (URL or URN) of the data-entry (example: "urn:miriam:obo.go:GO%3A0045202").
	 *
	 * @param name name of a data collection (examples: "ChEBI", "UniProt")
	 * @param id identifier of an element (examples: "GO:0045202", "P62158")
	 * @param type type of the URI the user wants to recover ('URN' or 'URL')
	 * @return unique URI of the data-entry
	 * @deprecated Use {@link #getURI(String, String)} instead.
	 */
	@Deprecated
	public String getURI(String name, String id, String type)
	{
		logger.debug("Call of the 'GetURI(String, String, String)' request...");
		String result = new String();

		if (type.equalsIgnoreCase("URN"))
		{
			result = getURN(name, id);
		}
		else   // URL is the default (even in case of an empty "type" field)
		{
			result = getURL(name, id);
		}

		return result;
	}


    /**
     * Retrieves the unique URI of a specific entity (example: "urn:miriam:obo.go:GO%3A0045202").
     * If the data collection does not exist (or is not recognized), an empty String is returned.
     * If the identifier is invalid for the given data collection, 'null' is returned.
     * 
     * @param name name of a data collection (examples: "ChEBI", "UniProt")
     * @param id identifier of an entity within the data collection (examples: "GO:0045202", "P62158")
     * @return unique MIRIAM URI of a given entity
     */
	public String getURI(String name, String id)
	{
	    logger.debug("Call of the 'GetURI(String, String)' request...");
        
	    return getURL(name, id);
	}
	
	
	/**
     * Retrieves the unique URIs for a list of specific entities (example: "urn:miriam:obo.go:GO%3A0045202").
     * If a data collection does not exist (or is not recognized), an empty String is returned for this data collection.
     * If an identifier is invalid for the given data collection, 'null' is returned for this data collection.
     * If the provided lists do not have the same size, 'null' is returned.
     * 
     * @param names list of data collection names (examples: "ChEBI", "UniProt")
     * @param ids list of entities identifiers (examples: "GO:0045202", "P62158")
     * @return list of MIRIAM URIs
     */
	public String[] getURIs(String[] names, String[] ids)
	{
	    logger.debug("Call of the 'GetURIs(String[], String[])' request...");
	    String[] result = null;
        
	    // safety check
	    if ((names.length == ids.length))
	    {
	    	result = new String[names.length];
	    	
	    	for (int i=0; i<names.length; ++i)
		    {
	    		result[i] = getURI(names[i], ids[i]);
		    }
	    }
	    
        return result;
	}
	
	
	/**
	 * Retrieves the definition of a data collection.
	 * 
	 * @param nickname name or URI (URN or URL) of a data collection
	 * @return definition of the data collection
	 */
	public String getDataTypeDef(String nickname)
	{
		logger.debug("Call of the 'GetDataTypeDef(String)' request...");
		
		DataType dataType = getDataType(nickname);

		if (dataType != null) 
		{
			return dataType.getDefinition();
		}
		
		return new String();
	}


	/**
     * Retrieves the physical locationS (URLs) of web pageS about the data-entry.
     *
     * @param nickname name (can be a synonym) or URN or URL of a data collection (examples: "ChEBI", "UniProt")
     * @param id identifier of an element (examples: "GO:0045202", "P62158")
     * @return physical locationS of web pageS about the data-entry
     * @deprecated Use {@link #getLocations(String, String)} instead.
	 */
	@Deprecated
	public String[] getDataEntries(String nickname, String id)
	{
		logger.debug("Call of the 'GetDataEntries(nickname, id)' request...");

		return getLocations(nickname, id);
	}

	
	/**
     * Retrieves the (non obsolete) physical locationS (URLs) of web pageS providing knowledge about an entity.
     * If the URI is not recognized or the data collection does not exist, an empty array is returned.
     * If the identifier is invalid for the data collection, 'null' is returned.
     * All special characters in the data entry part of the URLs are properly encoded.
     * 
     * @param nickname name (can be a synonym) or URI of a data collection (examples: "Gene Ontology", "UniProt")
     * @param id identifier of an entity within the given data collection (examples: "GO:0045202", "P62158")
     * @return physical locationS of web pageS providing knowledge about the given entity
     */
	public String[] getLocations(String nickname, String id)
    {
        logger.debug("Call of the 'GetLocations(nickname, id)' request...");

		DataType dataType = getDataType(nickname);

		if (dataType != null) 
		{
			if (!RegistryUtilities.checkRegexp(id, dataType.getRegexp()))
			{
		        logger.warn("The provided ID :'" + id + "' does not comply to the regexp '" + dataType.getRegexp() + "'");				
				return null;
			}
			
			ArrayList<String> locationURLs = new ArrayList<String>();
			
			for (PhysicalLocation location : dataType.getPhysicalLocations())
			{
				if (!location.isObsolete())
				{
					locationURLs.add(location.getUrlPrefix() + id + location.getUrlSuffix());
				}
			}

			return locationURLs.toArray(new String[locationURLs.size()]);
		}
		
        return new String[0];
    }
    
    
    /**
     * Retrieves the physical locationS (URLs) of web pageS about the data-entry.
     * 
     * @param uri MIRIAM URI of an element (example: 'urn:miriam:obo.go:GO%3A0045202')
     * @return physical locationS of web pageS about the data-entry
     * @deprecated Use {@link #getLocations(String)} instead.
     */
	@Deprecated
    public String[] getDataEntries(String uri)
    {
        logger.debug("Call of the 'GetDataEntries(uri)' request...");

        return getLocations(uri);
    }
	
	
    /**
     * Retrieves the (non obsolete) physical locationS (URLs) of web pageS providing knowledge about a specific entity.
     * If the URI is not recognized or the data collection does not exist, an empty array is returned.
     * If the identifier is invalid for the data collection, 'null' is returned.
     * All special characters in the data entry part of the URLs are properly encoded.
     * 
     * @param uri MIRIAM URI of an entity (example: 'urn:miriam:obo.go:GO%3A0045202')
     * @return physical locationS of web pageS providing knowledge about the given entity
     */
    public String[] getLocations(String uri)
    {
        logger.debug("Call of the 'GetLocations(uri)' request...");

        String id = RegistryUtilities.getElementPart(uri);
        String dataTypeUri = RegistryUtilities.getDataPart(uri);
        		
        return getLocations(dataTypeUri, id);
     }
	
    
    /**
     * Retrieves the physical location (URL) of a web page about the data-entry, using a specific resource.
     * 
     * @param uri MIRIAM URI of an element (example: 'urn:miriam:obo.go:GO%3A0045202')
     * @param resource internal identifier of a resource (example: 'MIR:00100005')
     * @return physical location of a web page about the data-entry, using a specific resource
     * @deprecated Use {@link #getLocation(String, String)} instead.
     */
    @Deprecated
    public String getDataEntry(String uri, String resource)
    {
        logger.debug("Call of the 'GetDataEntry()' request...");

        return getLocation(uri, resource);
    }
    
    
    /**
     * Retrieves the physical location (URL) of a web page providing knowledge about a specific entity, using a specific resource.
     * 
     * @param uri MIRIAM URI of an entity (example: 'urn:miriam:obo.go:GO%3A0045202')
     * @param resource internal identifier of a resource (example: 'MIR:00100005')
     * @return physical location of a web page providing knowledge about the given entity, using a specific resource or null
     * if the uri is invalid or the identifier does not pass the data collection regexp.
     */
    public String getLocation(String uri, String resource)
    {
        logger.debug("Call of the 'GetLocation()' request...");
        String result = null;
        
        String id = RegistryUtilities.getElementPart(uri);
        String dataTypeUri = RegistryUtilities.getDataPart(uri);
 
        DataType dataType = getDataType(dataTypeUri);

		if (dataType != null && RegistryUtilities.checkRegexp(id, dataType.getRegexp())) 
		{
			for (PhysicalLocation location : dataType.getPhysicalLocations())
			{
				if (location.getId().equals(resource))
				{
					result = location.getUrlPrefix() + id + location.getUrlSuffix();
				}
			}
		}
        
        return result;
    }
    
    
    /**
     * Retrieves the list of (non obsolete) generic physical locations (URLs) of web pageS providing the dataset of a given data collection.
     * <p>Warning: those URLs cannot be directly used! They contain a token which needs to be replaced by an actual data entry identifier.
     * <br>If the data collection is not recognized or does not exist, 'null' is returned.
     * 
     * @param nickname name (can be a synonym) or URI of a data collection (examples: "Gene Ontology", "UniProt", "urn:miriam:biomodels.db")
     * @param token placeholder which will be put in the URLs at the location of the data entry identifier (default: $id)
     * @return list of (non obsolete) generic physical locations (URLs) of web pageS providing the dataset of a given data collection or null
     */
    public String[] getLocationsWithToken(String nickname, String token)
    {
    	logger.debug("Call of the 'getLocationsWithToken(nickname, token)' request...");

		DataType dataType = getDataType(nickname);

		if (dataType != null) 
		{
			ArrayList<String> locationURLs = new ArrayList<String>();
			
	        // checks token provided is suitable, otherwise revert to default one
	        if ((null == token) || (token.matches("\\s*")))
	        {
	        	token = "$id";
	        }

			for (PhysicalLocation location : dataType.getPhysicalLocations())
			{
				if (!location.isObsolete())
				{
					locationURLs.add(location.getUrlPrefix() + token + location.getUrlSuffix());
				}
			}

			return locationURLs.toArray(new String[locationURLs.size()]);
		}
		
        return null;

    }
	
    
	/**
	 * Retrieves all the (non obsolete) physical locations (URLs) of the services providing the data collection (web page).
	 * 
	 * @param nickname name (can be a synonym) or URL or URN of a data collection
	 * @return array of strings containing all the address of the main page of the resources of the data collection
	 */
	public String[] getDataResources(String nickname)
	{
		logger.debug("Call of the 'getDataResources()' request...");

		DataType dataType = getDataType(nickname);

		if (dataType != null) 
		{
			ArrayList<String> locationURLs = new ArrayList<String>();
			
			for (PhysicalLocation location : dataType.getPhysicalLocations())
			{
				if (!location.isObsolete())
				{
					locationURLs.add(location.getUrlRoot());
				}
			}

			return locationURLs.toArray(new String[locationURLs.size()]);
		}
		
        return null;
	}
	
	
	/**
	 * Says if a URI of a data collection is deprecated.
	 * 
	 * @param uri (URN or URL) of a data collection
	 * @return answer ("true" or "false") to the question: is this URI deprecated?
	 */
	public boolean isDeprecated(String uri)
	{
		logger.debug("Call of the 'isDeprecated()' request...");

        String dataTypeUri = RegistryUtilities.getDataPart(uri);
 
        DataType dataType = getDataType(dataTypeUri);
        		
        if (dataType != null)
        {
        	for (String deprecatedUri : dataType.getDeprecatedURIs())
        	{
        		if (deprecatedUri.equals(dataTypeUri))
        		{
                	return true;        			
        		}
        	}
        }
        
		return false;
	}
	
	
	/**
	 * Retrieves the official URI (it will always be URN) of a data collection corresponding to the deprecated one.
	 * 
	 * @param uri deprecated URI (URN or URL) of a data collection 
	 * @return the official URI corresponding to the deprecated one
	 * @deprecated Use {@link #getOfficialDataTypeURI(String)} instead.
	 */
	@Deprecated
	public String getOfficialURI(String uri)
	{
		logger.debug("Call of the 'getOfficialURI()' request...");
		
		return getOfficialDataTypeURI(uri);
	}
	
	
	/**
     * Retrieves the official URI (it will always be a URL) of a data collection.
     * 
     * @param nickname name (can be a synonym) or MIRIAM URI (even deprecated one) of a data collection (for example: "ChEBI", "http://www.ebi.ac.uk/chebi/", ...)
     * @return the official URI of the data collection or null if the data collection was not found
     */
    public String getOfficialDataTypeURI(String nickname)
    {
        logger.debug("Call of the 'getOfficialDataTypeURI()' request...");

		DataType dataType = getDataType(nickname);
		
		if (dataType != null) 
		{
			return dataType.getURL();
		}
		
		return null;
    }
	
	
	/**
     * Transforms a MIRIAM URI into its official equivalent (to transform obsolete URIs into current valid ones).
     * The parameter can be an obsolete URI (URN or URL), but the returned one will always be a URN.
     * This process involve a percent-encoding of all reserved characters (like ':').
     * 
     * @param uri deprecated URI (URN or URL), example: "http://www.ebi.ac.uk/chebi/#CHEBI:17891"
     * @return the official URI corresponding to the deprecated one (for example: "http://identifiers.org/obo.chebi/CHEBI:17891") or 'null' if the URN does not exist
     */
    public String getMiriamURI(String uri)
    {
        logger.debug("Call of the 'getMiriamURI()' request...");

        String id = RegistryUtilities.getElementPart(uri);
        String dataTypeUri = RegistryUtilities.getDataPart(uri);
        
        DataType dataType = getDataType(dataTypeUri);
        		
        if (dataType != null)
        {
        	String result = dataType.getURL();

        	if (id != null && id.trim().length() > 0)
        	{
        		 if (RegistryUtilities.checkRegexp(id, dataType.getRegexp())) 
        		 {
        			 result += id;
        		 }
        		 else
        		 {
        			 result = null;
        		 }
        	}
        	
        	return result;
        }
        
        return null;
    }
	
	
	/**
	 * Retrieves the pattern (regular expression) used by the identifiers within a data collection.
	 * 
	 * @param nickname data collection name (or synonym) or URI (URL or URN)
	 * @return pattern of the data collection
	 */
	public String getDataTypePattern(String nickname)
	{
		logger.debug("Call of the 'getDataTypePattern()' request...");
        
        DataType dataType = getDataType(nickname);
        		
        if (dataType != null)
        {
        	return dataType.getRegexp();
        }
        
		return new String();
	}
	
	
	/**
	 * Retrieves the general information about a precise resource of a data collection.
	 * 
	 * @param id identifier of a resource (example: "MIR:00100005")
	 * 
	 * @return general information about a resource
	 */
	public String getResourceInfo(String id)
	{
		logger.debug("Call of the 'getResourceInfo()' request...");

		for (DataType dataType : RegistryDatabase.getInstance().getDataTypes())
		{
			for (PhysicalLocation location : dataType.getPhysicalLocations()) 
			{
				if (location.getId().equals(id)) 
				{
					return location.getInfo();
				}
			}
		}
		
		return new String();
	}
	
	
	/**
	 * Retrieves the institution which manages a precise resource of a data collection.
	 * 
	 * @param id identifier of a resource (example: "MIR:00100005")
	 * @return institution which manages a resource
	 */
	public String getResourceInstitution(String id)
	{
		logger.debug("Call of the 'getResourceInstitution()' request...");

		for (DataType dataType : RegistryDatabase.getInstance().getDataTypes())
		{
			for (PhysicalLocation location : dataType.getPhysicalLocations()) 
			{
				if (location.getId().equals(id)) 
				{
					return location.getInstitution();
				}
			}
		}
		
		return new String();
	}
	
	
	/**
	 * Retrieves the location of the servers of a location.
	 * 
	 * @param id identifier of a resource (example: "MIR:00100005")
	 * @return location of the servers of a resource
	 */
	public String getResourceLocation(String id)
	{
		logger.debug("Call of the 'getResourceLocation()' request...");

		for (DataType dataType : RegistryDatabase.getInstance().getDataTypes())
		{
			for (PhysicalLocation location : dataType.getPhysicalLocations()) 
			{
				if (location.getId().equals(id)) 
				{
					return location.getLocation();
				}
			}
		}
		
		return new String();
	}
	
	
	/**
	 * Retrieves all the synonym names of a data collection (this list includes the original name).
	 * 
	 * @param name name or synonym of a data collection
	 * @return all the synonym names of the data collection
	 */
	public String[] getDataTypeSynonyms(String name)
	{
		logger.debug("Call of the 'GetDataTypeSynonyms()' request...");

        DataType dataType = getDataType(name);
        		
        if (dataType != null)
        {
        	ArrayList<String> synonymsList = new ArrayList<String>();
        	
        	synonymsList.add(dataType.getName());
        	
        	synonymsList.addAll(dataType.getSynonyms());
        	
        	return synonymsList.toArray(new String[synonymsList.size()]);
        }
        
		return null;
	}
	
	
	/**
	 * Retrieves the common name of a data collection.
	 * 
	 * @param uri URI (URL or URN) of a data collection
	 * @return the common name of the data collection
	 */
	public String getName(String uri)
	{
		logger.debug("Call of the 'getName()' request...");

		DataType dataType = getDataType(uri);
		
        if (dataType != null)
        {
        	return dataType.getName();
        }
		
		return new String();
	}
	
	
	/**
	 * Retrieves all the names (with synonyms) of a data collection.
	 * 
	 * @param uri URI (URL or URN) of a data collection
	 * @return the common name of the data collection and all the synonyms
	 */
	public String[] getNames(String uri)
	{
		logger.debug("Call of the 'getNames()' request...");

		return getDataTypeSynonyms(uri);
	}
	
	
    /**
     * Retrieves the list of names of all the data collections available.
     * 
     * @return list of the name of all the data collections
     */
    public String[] getDataTypesName()
    {
        logger.debug("Call of the 'getDataTypesName()' request...");
        
        ArrayList<String> dataTypeNames = new ArrayList<String>();
        
        for (DataType dataType : RegistryDatabase.getInstance().getDataTypes())
        {
        	dataTypeNames.add(dataType.getName());
        }
        
        return dataTypeNames.toArray(new String[dataTypeNames.size()]);
    }
    
    
    /**
     * Retrieves the internal identifier (stable and perennial) of all the data collections (for example: "MIR:00000005").
     * 
     * @return list of the identifier of all the data collections
     */
    public String[] getDataTypesId()
    {
        logger.debug("Call of the 'getDataTypesId()' request...");

        Set<String> dataTypeIds = RegistryDatabase.getInstance().getDataTypeMap().keySet();
        
        return dataTypeIds.toArray(new String[dataTypeIds.size()]);
    }
    
    
    /**
     * Checks if the identifier given follows the regular expression of the data collection (also provided).
     * 
     * @param identifier internal identifier used by the data collection
     * @param datatype name, synonym or MIRIAM URI of a data collection
     * @return "true" if the identifier follows the regular expression, "false" otherwise
     */
    public boolean checkRegExp(String identifier, String datatype)
    {
        // looks for the pattern of the data collection
        String pattern = getDataTypePattern(datatype);

        return RegistryUtilities.checkRegexp(identifier, pattern);
    }
    
    
    /**
     * Converts a MIRIAM URI into its equivalent Identifiers.org URL.
     * This takes care of any necessary conversion, for example in the case the URI provided is obsolete.
     * 
     * @param uri a MIRIAM URI
     * @return the Identifiers.org URL corresponding to the provided MIRIAM URI or 'null' if the provided URI does not exist
     */
    public String convertURN(String uri)
    {
    	logger.debug("Call of the 'convertURN(String)' request...");
    	
    	String id = RegistryUtilities.getElementPart(uri);
    	String oldDatatypeUri = RegistryUtilities.getDataPart(uri); 

		DataType dataType = getDataType(oldDatatypeUri);
		
        if (dataType != null && RegistryUtilities.checkRegexp(id, dataType.getRegexp()))
        {
        	return dataType.getURL() + id;
        }
    	
    	return null;
    }
    
    
    /**
     * Converts a list of MIRIAM URIs into their equivalent Identifiers.org URLs.
     * This takes care of any necessary conversion, for example in the case a URI provided is obsolete.
     * If a URI is invalid, 'null' is returned for this URI.
     * 
     * @param uris a list of MIRIAM URIs
     * @return a list of Identifiers.org URLs corresponding to the provided URIs
     */
    public String[] convertURNs(String[] uris)
    {
	    logger.debug("Call of the 'convertURNs(String[])' request...");
	    String[] urls = null;
	    
	    if (null != uris)
	    {
	    	urls = new String[uris.length];

	    	for (int i = 0; i < uris.length; ++i)
	    	{
	    		urls[i] = convertURN(uris[i]);
	    	}
	    }
	    
	    return urls;
    }
    
    
    /**
     * Converts an Identifiers.org URL into its equivalent MIRIAM URN.
     * This performs a check of the identifier based on the recorded regular expression.
     * 
     * @param url an Identifiers.org URL
     * @return the MIRIAM URN corresponding to the provided Identifiers.org URL or 'null' if the provided URL is invalid
     */
    public String convertURL(String url)
    {
    	logger.debug("Call of the 'convertURL(String)' request...");

    	String id = RegistryUtilities.getElementPart(url);
    	String oldDatatypeUri = RegistryUtilities.getDataPart(url); 

		DataType dataType = getDataType(oldDatatypeUri);
		
        if (dataType != null && RegistryUtilities.checkRegexp(id, dataType.getRegexp()))
        {
        	return dataType.getURN() + ":" + RegistryUtilities.identifierEncode(id);
        }

    	return null;
    }
    
    
    /**
     * Converts a list of Identifiers.org URLs into their equivalent MIRIAM URNs.
     * This performs a check of the identifier based on the recorded regular expression.
     * If a URL is invalid, 'null' is returned for this URL.
     * 
     * @param urls a list of Identifiers.org URLs
     * @return a list of MIRIAM URNs corresponding to the provided Identifiers.org URLs
     */
    public String[] convertURLs(String[] urls)
    {
    	logger.debug("Call of the 'convertURLs(String[])' request...");
	    String[] uris = null;
	    
	    if (null != urls)
	    {
	    	uris = new String[urls.length];

	    	for (int i = 0; i < urls.length; ++i)
	    	{
	    		uris[i] = convertURL(urls[i]);
	    	}
	    }
	    
	    return uris;
    }
    
}

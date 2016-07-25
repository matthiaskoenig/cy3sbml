package org.cy3sbml.miriam.registry;


import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.cy3sbml.miriam.registry.data.DataType;
import org.cy3sbml.miriam.registry.data.PhysicalLocation;

/**
 * Utility methods to manipulate URIs or identifiers.
 */

public class RegistryUtilities {
        
    // private static final int ID_NAME = 1;
    // private static final int ID_SYNONYM = 2;
    /**
     * 
     */
    private static final int ID_URL = 3;
    /**
     * 
     */
    private static final int ID_URN = 4;
    /**
     * 
     */
    private static final int ID_UNKNOWN = 5;
    /**
     * 
     */
    private static final String IDENTIFIERS_ORG_ROOT_URL = "http://identifiers.org";
    
    
    /**
     * Replaces each substring of the {@code base} string that matches the given regular expression with the given replacement. 
     * 
     * @param base the original String
     * @param pattern the regular expression to which the string is to be matched
     * @param replacement the string to be substituted for each match
     * @return the modified String
     */
    public static String replace(String base, String pattern, String replacement)
    {
        int begin = 0;
        int end = 0;
        StringBuffer result = new StringBuffer();
        
        while ((end = base.indexOf(pattern, begin)) >= 0)
        {
            result.append(base.substring(begin, end));
            result.append(replacement);
            begin = end + pattern.length();
        }
        result.append(base.substring(begin));
        
        return result.toString();
    }
    
    
    /**
     * Converts a URL into a (X)HTML valid way: replace '&amp;' by '&amp;amp;'
     * 
     * @param url physical URL
     * @return the same URL, but W3C valid
     */
    public static String urlConvert(String url)
    {
        String valid = new String();
        valid = replace(url, "&", "&amp;");
        
        return valid;
    }
    
    
    /**
     * Replaces each substring of the {@code original} string that matches the given regular expression with the given replacement. 
     * 
     * 
     * @param original the original String
     * @param pattern the regular expression to which the string is to be matched
     * @param replacement the string to be substituted for each match
     * @return modified character string
     */
    public static String transURL(String original, char pattern, String replacement)
    {
        String newStr = "";
        
        for (int j = 0; j < original.length(); ++j)
        {
            if (original.charAt(j) == pattern)
            {
                newStr += replacement;
            }
            else
            {
                newStr += original.charAt(j);
            }
        }
        
        return newStr;
    }
    
    
    /**
     * Tests if a string is composed only of space(s) or empty or null
     * 
     * @param str character string
     * @return response to the question: "is this character string only composed of space(s)?"
     */
    public static boolean isEmpty(String str)
    {
        boolean space = true;
        
        if ((str == null) || (str.equalsIgnoreCase("")))
        {
            return space; // true
        }
        else
        {
            for (int i = 0; i < str.length(); ++i)
            {
                if (str.charAt(i) != ' ')
                {
                    space = false;
                }
            }
        }
        
        return space;
    }
    
    
    /**
     * Returns a new <code>String</code> equivalent to the string in parameter, but with all the spaces replaced by '%20' (to have
     * valid XHTML links)
     * 
     * @param oldStr a string (usually a name with space)
     * @return the string in parameter without any space but "%20" instead
     */
    public static String nameTrans(String oldStr)
    {
        String newStr = "";
        
        for (int j = 0; j < oldStr.length(); ++j)
        {
            if (oldStr.charAt(j) == ' ')
            {
                newStr += "%20";
            }
            else
            {
                newStr += oldStr.charAt(j);
            }
        }
        
        return newStr;
    }
    
      
    
    /**
     * Searches the type of the URI (URL or URN?).
     * <p>
     * WARNING: doesn't check if the parameter is a valid URI!
     * 
     * @param uri a Uniform Request Identifier (can be a URL or a URN)
     * @return a boolean with the answer to the question above
     */
    public static String getURIType(String uri)
    {
        // "urn:" not found at the beginning of the URI
        if (uri.startsWith("urn:"))
        {
            return "URN";
        }
        else
        {
            return "URL";
        }
    }
    
    

    
    /**
     * Generates a random word (could be used as a password) of a given length.
     * 
     * @param length a given length
     * @return random password
     */
    public static String randomPassGen(Integer length)
    {
        String generated = new String();
        Random rand = new Random();
        
        for (int i=0; i<length.intValue(); i++)
        {
            String tmp = new Character((char)((int) 34 + ((int)(rand.nextFloat() * 93)))).toString();
            generated = generated + tmp;
        }
        
        return generated;
    }
    
    
    
    /**
     * Converts a MIRIAM URN into its equivalent Identifiers.org URL.
     * The method expects a fully valid and official URN, please use 'getMiriamURI(String)' beforehand if you are unsure.
     * 
     * @param urn a MIRIAM URN
     * @return the Identifiers.org URL corresponding to the provided URN or 'null' if the provided URN does not exist
     */
	public static String convertURN(String urn)
	{
		String url = null;
		String namespace = null;
		String identifier = null;
		
		if (null != urn)
		{
			// retrieves the data collection namespace and identifier
			String[] urnParts = urn.split(":");
			namespace = urnParts[2];
			identifier = urnParts[3];
			
			// remove any encoding of the identifier part (if any)
			try
			{
				identifier =  URLDecoder.decode(identifier, "UTF-8");
			}
			catch (UnsupportedEncodingException e)
			{
			    Logger logger = Logger.getLogger(RegistryUtilities.class);
			    
				logger.error("UnsupportedEncodingException raised while URL decoding >" + identifier + "<!");
				logger.error(e.getMessage());
				identifier = null;
			}
			
			// generates the Identifiers.org URL (if possible)
			if ((null != namespace) && (null != identifier))
			{
				url = IDENTIFIERS_ORG_ROOT_URL + "/" + namespace + "/" + identifier;
			}
		}
		else   // null query
		{
			// null returned
		}
		
		return url;
	}
	
	
	/**
     * Converts an Identifiers.org URL into its equivalent MIRIAM URN.
     * The identifier part should not be percent-encoded, but we decode it anyway (in case).
     * This method performs a check of the identifier based on the recorded regular expression.
     * @param url an Identifiers.org URL
     * @return the MIRIAM URN corresponding to the provided Identifiers.org URL or 'null' if the provided URL does not exist
     */
	public static String convertURL(String url)
	{
		String urn = null;
		String[] parts = null;
		
		if (null != url)
		{
			// removes any parameters that could be present in the URL (for example: "?format=html")
			if (url.lastIndexOf("?") > 0)
			{
				url = url.substring(0, url.lastIndexOf("?"));
			}
			
			// retrieves the namespace and identifier parts from the URL
			parts = getURLParts(url);
			
			if (null != parts)
			{
				// decodes the identifier: there should be any, but anyway does not do anything if no encoding has been applied
				parts[1] = identifierDecode(parts[1]);
				
				String regexp = null;
				regexp = getDataCollectionRegExpFromNamespace(parts[0]);
				
				if (null != regexp)   // if equals 'null', this mainly means that the namespace has not been recognised
				{
					// checks that the identifier matches the regexp
					if (checkRegexp(parts[1], regexp))
					{
						urn = "urn:miriam:" + parts[0] + ":" + identifierEncode(parts[1]);
					}
				}
			}
		}
		else   // null query
		{
			// null returned
		}
		
		return urn;
	}
	
	/**
	 * Retrieves the regular expression associated with a data collection, given its namespace.
	 * 
	 * @return the regexp or null if the namespace provided does not exist
	 */
	public static String getDataCollectionRegExpFromNamespace(String namespace)
	{
		DataType datatype = getDataType(namespace);
		
		if (datatype != null)
		{
			return datatype.getRegexp();
		}
		
        return null;
	}
	
	
    /**
     * Retrieves all the information about a data collection.
     * 
     * @param id identifier of a data collection (for example "MIR:00000005")
     * @return <code>DataType</code> object containing all the information relative to the given identifier
     */
    public static DataType getDataTypeById(String id)
    {
    	return RegistryDatabase.getInstance().getDatatypeById(id);
    }

    /**
     * Retrieves all the information about a data collection.
     * 
     * @param name identifier(for example "MIR:00000005"), name, synonym or a URI  of a data collection 
     * @return <code>DataType</code> object containing all the information relative to the given identifier
     */
	public static DataType getDataType(String name) 
	{
		if (name == null || name.trim().length() == 0)
		{
			return null;
		}
		
		// Trying name
		DataType dataType = RegistryDatabase.getInstance().getDataTypeByName(name);
		
		
	    if (dataType == null) 
	    {
	    	// Trying id
	    	dataType = RegistryDatabase.getInstance().getDatatypeById(name);
	    }
	    
	    if (dataType == null) 
	    {
	    	// Trying uri
	    	dataType = RegistryDatabase.getInstance().getDataTypeByURI(name);
	    }
	    
		return dataType;
	}

	
	/**
	 * Extracts the namespace and identifier parts from an Identifiers.org URL.
	 * 
	 * @param url Identifiers.org URL
	 * @return array with the namespace as the first element, and the identifier as the second and last element, or null if the provided String does not seem a Identifiers.org URL
	 */
	public static String[] getURLParts(String url)
	{
		String[] parts = null;
		
		if ((null != url) && (url.startsWith("http://identifiers.org/")) && (url.length() > 23))   // necessary basic checks
		{
			// removes the prefix part ('http://identifiers.org/')
			url = url.substring(23);
			
			int indexFirstSlash = url.indexOf("/");
			
			// seems a valid Identifiers.org URL
			if ((indexFirstSlash != -1) && (url.length() > indexFirstSlash + 1))
			{
				parts = new String[2];
				parts[0] = url.substring(0, indexFirstSlash);
				parts[1] = url.substring(indexFirstSlash + 1);
			}
		}
		
		return parts;
	}
	
    
    /**
     * Retrieves all the information about a physical location.
     * 
     * @param id identifier of a resource (for example: "MIR:00100005")
     * @return <code>Resource</code> object containing all the information relative to the given identifier
     */
    public static PhysicalLocation getPhysicalLocation(String id)
    {
		for (DataType dataType : RegistryDatabase.getInstance().getDataTypes())
		{
			for (PhysicalLocation location : dataType.getPhysicalLocations()) 
			{
				if (location.getId().equals(id)) 
				{
					return location;
				}
			}
		}

        return null;
    }
    
        
    /**
     * Encodes the provided identifier if necessary: if it contains a reserved character used as a delimiter (":" only)
     */
    public static String identifierEncode(String id)
    {
    	if (null != id)
    	{
	        if (id.contains(":"))
	        {
	            try
	            {
	                id = URLEncoder.encode(id, "UTF-8");
	            }
	            catch (UnsupportedEncodingException e)
	            {
	            	Logger logger = Logger.getLogger(RegistryUtilities.class);
	                logger.error("An error occurred when encoding the following identifier: '" + id + "'!");
	                logger.error("UnsupportedEncodingException raised: " + e.getMessage());
	                // lets' not do anything to the provided identifier
	            }
	        }
    	}
    	else   // null input
    	{
    		// returns null
    	}
        
        return id;
    }
    
    /**
     * Decodes the provided identifier.
     */
    public static String identifierDecode(String id)
    {
    	if (null != id)
    	{
    		try
            {
                id = URLDecoder.decode(id, "UTF-8");
            }
            catch (UnsupportedEncodingException e)
            {
            	Logger logger = Logger.getLogger(RegistryUtilities.class);
                logger.error("An error occurred when decoding the following identifier: '" + id + "'!");
                logger.error("UnsupportedEncodingException raised: " + e.getMessage());
                // lets' not do anything to the provided identifier
            }
    	}
    	
    	return id;
    }
 
    /**
     * Returns the data collection part of a URI.
     * 
     * @param uri a URI (example: "urn:miriam:pubmed:10812475", "http://identifiers.org/pubmed/10812475" or the deprecated form "http://www.pubmed.gov/#10812475")
     * @return the data collection part of a URI
     */
    public static String getDataCollectionPartFromURI(String uri)
    {
    	return getDataPart(uri);
    }

    /**
     * Returns the data collection part of a URI.
     * 
     * @param uri a URI (example: "urn:miriam:pubmed:10812475", "http://identifiers.org/pubmed/10812475" or the deprecated form "http://www.pubmed.gov/#10812475")
     * @return the data collection part of a URI
     */
    public static String getDataPart(String uri)
    {
        int index;
        int type = getUriType(uri);
        
        if (type == ID_URL)   // URL
        {
            index = uri.lastIndexOf("#");
            
            // '#' not found
            if (index == -1 && uri.startsWith(IDENTIFIERS_ORG_ROOT_URL))
            {
            	// searches for the first '/' after http://identifiers.org/
            	index = uri.indexOf("/", 25); // uri.lastIndexOf("/");            	
            	if (index != -1)
            	{
            		index += 1;   // to include the final '/'
            	}
            }            
            else if (index == -1)
            {
            	// old url without a '#'
            	// searches for the last '/'
            	index = uri.lastIndexOf("/");            	
            	if (index != -1)
            	{
            		index += 1;   // to include the final '/'
            	}            	
            }            
        }
        else if (type == ID_URN)   // URN
        {
        	uri = urlMagicEncode(uri); // Will encode any ':' present in the identifiers part
            index = uri.lastIndexOf(":");
        }
        else   // neither URN nor URL
        {
        	index = -1;
        }
        
        // data collection found
        if (index != -1)
        {
            return uri.substring(0, index);
        }
        else   // data collection part not found, returns the full URI
        {
            return uri;
        }
    }
    
    
    /**
     * Returns the identifier part of a URI.
     * 
     * @param uri a URI (example: "urn:miriam:pubmed:10812475", "http://identifiers.org/pubmed/10812475"  or the deprecated form "http://www.pubmed.gov/#10812475")
     * @return the identifier part of a URI (or null if the part was not found)
     */
    public static String getIdentifierFromURI(String uri)
    {
    	return getElementPart(uri);
    }
    
    /**
     * Returns the identifier part of a URI.
     * 
     * @param uri a URI (example: "urn:miriam:pubmed:10812475", "http://identifiers.org/pubmed/10812475"  or the deprecated form "http://www.pubmed.gov/#10812475")
     * @return the identifier part of a URI (or null if the part was not found)
     */
    public static String getElementPart(String uri)
    {
        int index;
        int type = getUriType(uri);
        
        if (type == ID_URL)   // URL
        {
            index = uri.lastIndexOf("#");
            
            // '#' not found
            if (index == -1 && uri.startsWith(IDENTIFIERS_ORG_ROOT_URL))
            {
            	// searches for the first '/' after http://identifiers.org/
            	index = uri.indexOf("/", 25);
            }            
            else if (index == -1)
            {
            	// old url without a '#'
            	// searches for the last '/'
            	index = uri.lastIndexOf("/");            	
            }            
        }
        else if (type == ID_URN)   // URN
        {
        	uri = urlMagicEncode(uri); // Will encode any ':' present in the identifiers part
            index = uri.lastIndexOf(":");
        }
        else   // neither a URN nor a URL
        {
        	index = -1;
        }
        
        // identifier part found
        if (index != -1)
        {
            String tmp = uri.substring(index+1, uri.length());
            String id = null;
            int questionMarkIndex = tmp.indexOf("?");
            
            if (questionMarkIndex != -1)
            {
            	// There are some arguments after the identifier in the url, for ex : http://identifiers.org/pubmed/16333295?profile=demo
            	tmp = tmp.substring(0, questionMarkIndex);
            }
            
            try
            {
                id = URLDecoder.decode(tmp, "UTF-8");
                
                return id;
            }
            catch (UnsupportedEncodingException e)
            {
            	Logger logger = Logger.getLogger(RegistryUtilities.class);
                logger.error("An error occurred when encoding the following identifier: '" + tmp + "'!");
                logger.error("UnsupportedEncodingException raised: " + e.getMessage());
                
                return tmp;   
            }
        }
        else   // identifier part not found
        {
            return null;
        }
    }
    
    /**
     * Encodes URIs in order to make them comply with the URN syntax.
     * 
     * This encodes the character ':' (if any) in the identifier part with its percent-encoded equivalent '%3A'. Works as well with any other reserved character. 
     * This should only be useful if the MIRIAM URIs don't follow the URN syntax!
     */
    public static String urlMagicEncode(String uri)
    {
        StringBuilder temp = null;
        String id;
        
        // no transformation necessary for URLs
        if (getUriType(uri) == ID_URN)
        {
            String[] parts = uri.split(":");
            // ':' presents in the identifier part
            // one exception: "urn:lsid:uniprot.org:uniprot:P12345"
            if ((parts.length == 5) && (! parts[3].equalsIgnoreCase("uniprot")))
            {
                temp = new StringBuilder();
                
                // data collection part: no encoding
                for (int i=0; i<3; ++i)
                {
                    temp.append(parts[i] + ":");
                }
                // identifier part: URLencode
                id = parts[3] + ":" + parts[4];
                temp.append(identifierEncode(id));
            }
        }
        
        // no transformation necessary
        if (null == temp)
        {
            temp = new StringBuilder();
            temp.append(uri);
        }
        
        return temp.toString();
    }
    
    /**
     * Retrieves the type of the element (URN or URL)
     * 
     * @param element a URN or URL
     * @return the type of the element, using an integer based constant with the following possible 'url' or 'urn'
     */
    public static int getUriType(String element)
    {
       int result;
       
       if (isURN(element))
       {
           result = ID_URN;
       }
       else if (isURL(element))
       {
           result = ID_URL;
       }
       else
       {
    	   result = ID_UNKNOWN;
       }
       
       return result;
    }
    
    
    /**
     * Checks if a URI is a URL.
     */
    public static Boolean isURL(String uri)
    {
    	Boolean isURL = false;
    	
    	try
		{
			@SuppressWarnings("unused")
			URL url = new URL(uri);
			isURL = true;
		}
		catch (MalformedURLException e)
		{
			// no need to manage this exception: we now know if the URI is a URL
		}
		
		return isURL;
    }
    
    /**
     * Checks if a URI is a URN.
     */
    public static Boolean isURN(String uri)
    {
    	Boolean isURN = false;
    	
    	if (null != uri)
		{
			isURN = uri.startsWith("urn:");
		}
    	
    	return isURN;
    }
    
    /**
     * Checks if a String matches a given regular expression.
     * 
     * @param element given string to check
     * @param pattern regular expression
     * @return true if the String matches the regular expression.
     * @see Matcher#matches()
     */
    public static Boolean checkRegexp(String element, String pattern)
    {
        if ((null != element) && (! element.isEmpty()) && (null != pattern) && (! pattern.isEmpty()))
        {
            Pattern pat = Pattern.compile(pattern);
            Matcher matcher = pat.matcher(element);
            
            return matcher.matches();
        }
        else
        {
            return false;
        }
    }
  
 
}

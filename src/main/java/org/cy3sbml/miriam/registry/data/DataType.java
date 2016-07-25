package org.cy3sbml.miriam.registry.data;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.cy3sbml.miriam.registry.RegistryUtilities;


/**
 * Stores all the information about a data type.
 */
public class DataType implements Comparable<DataType>
{
	/**
	 * The logger for this class.
	 */
	private Logger logger = Logger.getLogger(DataType.class);
			
	/** stable identifier of the data type (something starting by 'MIR:000' and followed by 5 digits) */
	private String id = new String();
	/** official name of the data type */
	private String name = new String();
	/** name of the data type for HTML links (with "%20" instead of spaces)*/
	private String nameURL = new String();
	/** synonyms of the name of the data type */
	private List<String> synonyms = new ArrayList<String>();
	/** official namespace of the data type */
	private String namespace = new String();
	/** official URL of the data type */
	private String URL = new String();
	/** official URN of the data type */
	private String URN = new String();
	/** deprecated URIs */
	private List<String> deprecatedURIs = new ArrayList<String>();
	/** definition of the data type */
	private String definition = new String();
	/** regular expression of the data type */
	private String regexp = new String();
	/** resources (= physical locations) */
	private List<PhysicalLocation> physicalLocations = new ArrayList<PhysicalLocation>();
	
	/**
	 * Stores the {@link Annotation}s for this <code>DataType</code>
	 */
	private ArrayList<Annotation> annotations = new ArrayList<Annotation>();
	/**
	 * Stores the list of {@link Tag} for this {@link DataType}
	 */
	private ArrayList<String> tags = new ArrayList<String>();
	
	/** list of physical locations of pieces of documentation of the data type */
	private List<String> documentationURLs = new ArrayList<String>();
	/** type of the identifiers of pieces of documentation of the data type (PubMed, DOI, ...) */
	private List<String> documentationTypes = new ArrayList<String>();
	/** date of creation of the data type (the Date and String versions are linked and are modified together) */
	private Date dateCreation = new Date(0);
	/** date of creation of the data type as a String for direct display in JSP following the good pattern */
	private String dateCreationStr = new String();   
	/** date of last modification of the data type (the Date and String versions are linked and are modified together) */
	private Date dateModification = new Date(0);
	/** for direct display in JSP following the good pattern */
	private String dateModificationStr = new String();
	/** if the data type is obsolete or not */
	private boolean obsolete;
	/** why the data type is obsolete */
	private String obsoleteComment = new String();
	/** if the data type is obsolete, this field must have a value */
	private String replacedBy = new String();
	/** if the data type is restricted or not */
	private boolean restricted;
	/** whether or not some restriction exist on the access and usage of the data set */
    private List<Restriction> restrictions;   // the kind of limitations, if any, null otherwise
	/**
	 * Default constructor
	 */
	public DataType()
	{
		// nothing here, for the moment.
	}
	
	
	/**
	 * Destroys the object (free the memory)
	 */
	public void destroy()
	{
		this.id = "";
		this.name = "";
		this.nameURL = "";
		(this.synonyms).clear();
		this.URL = "";
		this.URN = "";
		(this.deprecatedURIs).clear();
		this.definition = "";
		this.regexp = "";
		(this.physicalLocations).clear();
		(this.documentationURLs).clear();
		(this.documentationTypes).clear();
		this.dateCreation = new Date(0);
		this.dateCreationStr = "";
		this.dateModification = new Date(0);
		this.dateModificationStr = "";
		this.obsolete = false;
		this.replacedBy = "";
	}
	
	
	/**
	 * Overrides the 'toString()' method for the 'DataType' object
	 * @return a string which contains all the information about the data type
	 */
	public String toString()
	{
		StringBuilder tmp = new StringBuilder();
		
		tmp.append("\n");
		if (this.isObsolete())
		{
		    tmp.append("WARNING: this data type is obsolete and replaced by: " + this.replacedBy);
		}
		tmp.append("+ Internal ID:        " + getId() + "\n");
		tmp.append("+ Name:               " + getName() + "\n");
		tmp.append("+ Synonyms:           " + getSynonyms().toString() + "\n");
		tmp.append("+ Definition:         " + getDefinition() + "\n");
		tmp.append("+ Regular Expression: " + getRegexp() + "\n");
		tmp.append("+ Official URL:       " + getURL() + "\n");
		tmp.append("+ Official URN:       " + getURN() + "\n");
		tmp.append("+ Deprecated URI(s):  " + getDeprecatedURIs().toString() + "\n");
		tmp.append("+ Data Physical Location(s): " + "\n");
		for (int i=0; i<getResources().size(); ++i)
		{
			tmp.append("    * Physical Location #" + i + ":\n");
			tmp.append(getResource(i).toString() + "\n");
		}

		tmp.append("+ Data Annotation(s): " + "\n");
		for (Annotation annotation : getAnnotations())
		{
			tmp.append(" \t+ Format: " + annotation.getFormat() + "\n");
			
			for (Tag tag : annotation.getTags()) {
				tmp.append("\t  * Element : ");
				tmp.append(tag.getName() + "\n");
			}
		}

		tmp.append("+ Documentation URL(s): " + "\n");
		for (int i=0; i<getDocumentationURLs().size(); ++i)
		{
			tmp.append("       - " + getDocumentationURL(i) + "\n");
		}
		
		return tmp.toString();
	}
	
	
	/**
	 * Searches the type of the URI in parameter (URL or URN?)
	 * <p>
	 * WARNING: doesn't check if the parameter is a valid URI!
	 * @param uri a Uniform Request Identifier (can be a URL or a URN)
	 * @return a boolean with the answer to the question above
	 */
	public String getURIType(String uri) {
		// "urn:" not found in the URI
		if ((uri.indexOf("urn:")) == -1)
		{
			return "URL";
		}
		else
		{
			return "URN";
		}
	}
	
	
	/**
	 * Returns the answer to the question: is this URI a URL?
	 * @param uri a Uniform Request Identifier
	 * @return a boolean with the answer to the question above
	 */
	public boolean isURL(String uri) {
		return getURIType(uri).equalsIgnoreCase("URL");
	}

	/**
	 * Returns the answer to the question: is this URI a URN?
	 * @param uri a Uniform Request Identifier
	 * @return a boolean with the answer to the question above
	 */
	public boolean isURN(String uri){
		return getURIType(uri).equalsIgnoreCase("URN");
	}
	
	
	/**
	 * Returns the answer to the question: is the deprecated URI, identified by the index, a URN?
	 * @param i index of a deprecated URI
	 * @return a boolean with the answer to the question above
	 */
	public boolean isDeprecatedURN(int i)
	{
		if (getURIType(getDeprecatedURI(i)).equalsIgnoreCase("URN"))
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	
	/**
	 * Returns the answer to the question: is the deprecated URI, identified by the index, a URL?
	 * @param i index of a deprecated URI
	 * @return a boolean with the answer to the question above
	 */
	public boolean isDeprecatedURL(int i)
	{
		if (getURIType(getDeprecatedURI(i)).equalsIgnoreCase("URL"))
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	
	/**
	 * Getter of the definition of the data type
	 * @return definition of the data type
	 */
	public String getDefinition()
	{
		return definition;
	}
	
	
	/**
	 * Setter of the definition of the data type
	 * @param definition definition of the data type
	 */
	public void setDefinition(String definition)
	{
		this.definition = removeSpace(definition);
	}
	
	
	/**
	 * Returns all the deprecated URIs of the data type
	 * @return all the deprecated URIs of the data type
	 */
	public List<String> getDeprecatedURIs()
	{
		return deprecatedURIs;
	}
	
	
	/**
	 * Returns one precise deprecated URI of the data type
	 * @param i index of the deprecated URI
	 * @return one precise deprecated URI of the data type
	 */
	public String getDeprecatedURI(int i)
	{
		return (String) deprecatedURIs.get(i);
	}
	
	/**
	 * Setter of the deprecated forms of the URI (URN or URL) of the data type
	 * @param deprecatedURIs list of all the deprecated URI of the data type
	 */
	public void setDeprecatedURIs(ArrayList<String> deprecatedURIs)
	{
		for (int i=0; i<deprecatedURIs.size(); ++i)
		{
			this.deprecatedURIs.add(removeSpace((String) deprecatedURIs.get(i)));
		}
	}
	
	
	/**
	 * Getter of the stable ID (in the database) of the data type
	 * @return the internal ID of the data type
	 */
	public String getId()
	{
		return this.id;
	}
	
	
	/**
	 * Setter of the internal ID (in the database) of the data type
	 * @param id internal ID of the data type
	 */
	public void setId(String id)
	{
		this.id = id;
	}
	
	
	/**
	 * Getter of the official name (not a synonym) of the data type
	 * @return name of the data type
	 */
	public String getName()
	{
		return this.name;
	}
	
	
	/**
	 * Setter of the official name of the data type
	 * @param name name of the data type
	 */
	public void setName(String name)
	{
		this.name = removeSpace(name);
	}
	
	
	/**
	 * Getter of the HTML name (without any space)
	 * @return
	 */
	public String getNameURL()
	{
		return this.nameURL;
	}
	
	
	/**
	 * Setter of the HTML name (without any space)
	 * @param nameURL
	 */
	public void setNameURL(String nameURL)
	{
		this.nameURL = removeSpace(nameURL);
	}
	
	
	/**
	 * Getter of the regular expression of the data type
	 * @return regular expression of the data type
	 */
	public String getRegexp()
	{
		return this.regexp;
	}
	
	
	/**
	 * Setter of the regular expression of the data type
	 * @param regexp regular expression of the data type
	 */
	public void setRegexp(String regexp)
	{
		this.regexp = removeSpace(regexp);
	}
	
	
	/**
	 * Getter of the synonyms of the name of the data type
	 * @return list of all the synonyms of the name of the data type
	 */
	public List<String> getSynonyms()
	{
		return this.synonyms;
	}
	
	
	/**
	 * Getter of one of the synonyms of the name of the data type
	 * @param i index of the synonym
	 * @return one precise synonym of the name of the data type
	 */
	public String getSynonym(int i)
	{
		return (String) this.synonyms.get(i);
	}
	
	
	/**
	 * Setter of the synonyms of the data type
	 * @param synonyms list of all the synonyms of the data type
	 */
	public void setSynonyms(ArrayList<String> synonyms)
	{
		for (int i=0; i<synonyms.size(); ++i)
		{
			this.synonyms.add(removeSpace((String) synonyms.get(i)));	
		}
	}
	
	public ArrayList<String> getTags()
	{
		return this.tags;
	}
	
	/**
	 * Getter of the official URL of the data type
	 * @return URL of the data type
	 */
	public String getURL()
	{
		return this.URL;
	}
	
	
	/**
	 * Setter of the official URL of the data type
	 * @param url URL of the data type
	 */
	public void setURL(String url)
	{
		this.URL = removeSpace(url);
	}
	
	
	/**
	 * Getter of the official URN of the data type
	 * @return URN of the data type
	 */
	public String getURN()
	{
		return this.URN;
	}
	
	
	/**
	 * Setter of the official URN of the data type
	 * @param urn URN of the data type
	 */
	public void setURN(String urn)
	{
		this.URN = removeSpace(urn);
	}
	
	
	/**
	 * Getter of the resources (physical locations) of the data type
	 * @return the resources of the data type
	 */
	public List<PhysicalLocation> getResources()
	{
		return this.physicalLocations;
	}

	/**
	 * Gets the physical locations of the data type.
	 * 
	 * @return the physical locations of the data type
	 */
	public List<PhysicalLocation> getPhysicalLocations()
	{
		return this.physicalLocations;
	}

	
	/**
	 * Getter of a specific resource (physical location) of the data type
	 * @return a precise resource of the data type
	 */
	public PhysicalLocation getResource(int index)
	{
		return (PhysicalLocation) this.physicalLocations.get(index);
	}
	
	/**
	 * Gets a specific physical location of the data type.
	 * 
	 * @return a precise physical location of the data type
	 */
	public PhysicalLocation getPhysicalLocation(int index)
	{
		return (PhysicalLocation) this.physicalLocations.get(index);
	}
	
	
	/**
	 * Setter of the resources (physical locations) of the data type
	 * @param physicalLocations list of the resources of the data type
	 */
	public void setResources(ArrayList<PhysicalLocation> physicalLocations)
	{
		this.physicalLocations = physicalLocations;
	}

	/**
	 * Sets the physical locations of the data type.
	 * 
	 * @param physicalLocations list of the physical locations of the data type
	 */
	public void setPhysicalLocations(ArrayList<PhysicalLocation> physicalLocations)
	{
		this.physicalLocations = physicalLocations;
	}
	
	
	/**
	 * Adds another resource to the data type
	 * @param res the new resource to add to the data type
	 */
	public void addResource(PhysicalLocation res)
	{
		this.physicalLocations.add(res);
	}
	
	/**
	 * Adds another physical location to the data type.
	 * 
	 * @param res the new physical location to add to the data type
	 */
	public void addPhysicalLocation(PhysicalLocation res)
	{
		this.physicalLocations.add(res);
	}
	
	
	public ArrayList<Annotation> getAnnotations() {
		return annotations;
	}


	public void setAnnotations(ArrayList<Annotation> annotations) {
		this.annotations = annotations;
	}


	/**
	 * Getter of the prefix of the physical location of all the data entries (one precise element)
	 * @return the prefix of the physical location of all the data entries 
	 */
	public ArrayList<String> getDataEntriesPrefix()
	{
		ArrayList<String> result = new ArrayList<String>();
		
		for (int i=0; i<physicalLocations.size(); ++i)
		{
			result.add(((PhysicalLocation) physicalLocations.get(i)).getUrlPrefix());
		}
		
		return result;
	}
	
	
	/**
	 * Getter of the prefix of the physical location of one data entry (one precise element)
	 * <p>
	 * WARNING: no check of the validity of the parameter ('out of range' possible...)
	 * @param index index of the resource
	 * @return the prefix of the physical location of one precise the data entry 
	 */
	public String getDataEntryPrefix(int index)
	{
		return (String) (((PhysicalLocation) physicalLocations.get(index)).getUrlPrefix());
	}
	
	
	/**
	 * Getter of the suffix of the physical location of all the data entries (one precise element)
	 * @return the suffix of the physical location of all the data entries 
	 */
	public ArrayList<String> getDataEntriesSuffix()
	{
		ArrayList<String> result = new ArrayList<String>();
		
		for (int i=0; i<physicalLocations.size(); ++i)
		{
			result.add(((PhysicalLocation) physicalLocations.get(i)).getUrlSuffix());
		}
		
		return result;
	}
	
	
	/**
	 * Getter of the suffix of the physical location of one data entry (one precise element)
	 * @param index index of the resource
	 * @return the suffix of the physical location of one precise the data entry 
	 */
	public String getDataEntrySuffix(int index)
	{
		return (String) (((PhysicalLocation) physicalLocations.get(index)).getUrlSuffix());
	}
	
	
	/**
	 * Getter of the physical locations of all the resources (information page)
	 * @return the physical locations of all the resources
	 */
	public ArrayList<String> getDataResources()
	{
		ArrayList<String> result = new ArrayList<String>();
		
		for (int i=0; i<physicalLocations.size(); ++i)
		{
			result.add(((PhysicalLocation) physicalLocations.get(i)).getUrlRoot());
		}
		
		return result;
	}
	
	
	/**
	 * Getter of the physical location of one precise resource (information page)
	 * @param index index of the resource
	 * @return the physical location of one precise resource
	 */
	public String getDataResource(int index)
	{
		return (String) (((PhysicalLocation) physicalLocations.get(index)).getUrlRoot());
	}
	
    /**
     * Adds a physical location for one piece of documentation of the data type
     * 
     * @param url (PubMed, DOI, ...)
     */
    public void addDocumentationUrl(String url)
    {
        this.documentationTypes.add(url);
    }
    
	/**
	 * Getter of the physical locations (URLs) of all the pieces of documentation of the data type
	 * @return physical locations of all the pieces of documentation of the data type
	 */
	public List<String> getDocumentationURLs()
	{
		return documentationURLs;
	}
	
	
	/**
	 * Getter of the physical location (URL) of one piece of documentation
	 * @param index index of one documentation
	 * @return physical location of one piece of documentation of the data type
	 */
	public String getDocumentationURL(int index)
	{
		return (String) documentationURLs.get(index);
	}
	
	
	/**
	 * Setter of physical locations (URLs) of pieces of documentation of the data type
	 * @param docs_url list physical locations (URLs)
	 */
	public void setDocumentationURLs(ArrayList<String> docs_url)
	{
		for (int i=0; i<docs_url.size(); ++i)
		{
			this.documentationURLs.add(removeSpace((String) docs_url.get(i)));
		}
	}
	   
    
    /**
     * Sets the type of the identifier of all the pieces of documentation of the data type
     * @param documentationIDsType the documentationIDsType to set
     */
    public void setDocumentationTypes(ArrayList<String> documentationIDsType)
    {
        this.documentationTypes = documentationIDsType;
    }
    
    
    /**
     * Adds a type for an identifier of one piece of documentation of the data type
     * @param type (PubMed, DOI, ...)
     */
    public void addDocumentationType(String type)
    {
        this.documentationTypes.add(type);
    }
    
    
    /**
     * Gets the type of the identifier of one piece of documentation of the data type
     * 
     * @param index a documentation index
     * @return the documentationIDsType
     */
    public String getDocumentationType(int index)
    {
        return documentationTypes.get(index);
    }
	
	
	
	
	/**
	 * Getter of the date (Date) of creation of the data type
	 * @return dateCreation date of creation of the data type
	 */
	public Date getDateCreation()
	{
		return this.dateCreation;
	}
	
	
	/**
	 * Setter of the date (Date) of creation of the data type
	 * @param dateCreation date of creation of the data type
	 */
	public void setDateCreation(Date dateCreation)
	{
		this.dateCreation = dateCreation;
		
		// modification of the String form of the creation date
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'GMT'");
		this.dateCreationStr = dateFormat.format(this.dateCreation);
	}
	
	/**
	 * Getter of the date (String) of creation of the data type
	 * @return dateCreation date of creation of the data type
	 */
	public String getDateCreationStr()
	{
		return this.dateCreationStr;
	}
	
	
	/**
	 * Setter of the date (String) of creation of the data type
	 * @param dateCreationStr date of creation of the data type
	 */
	public void setDateCreationStr(String dateCreationStr)
	{
		this.dateCreationStr = dateCreationStr;
		
		// modification of the Date form of the creation date
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		try
		{
			this.dateCreation = dateFormat.parse(dateCreationStr);
		}
		catch (Exception e)
		{
			logger.error("Date conversion error (" + dateCreationStr + ")" + e);
			this.dateCreation = new Date(0);   // 1st January 1970
		}
	}
	
	
	/**
	 * Gets the date (Date) of last modification of the data type
	 * @return date of last modification of the data type
	 */
	public Date getDateModification()
	{
		return this.dateModification;
	}
	
	
	/**
	 * Sets the date (Date) of last modification of the data type
	 * @param dateModification date of last modification of the data type
	 */
	public void setDateModification(Date dateModification)
	{
		this.dateModification = dateModification;
		
		// modification of the String form of the last modification date
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'GMT'");
		this.dateModificationStr = dateFormat.format(this.dateModification);
	}
	
	
	/**
	 * Gets the date (String) of last modification of the data type
	 * @return date of last modification of the data type
	 */
	public String getDateModificationStr()
	{
		return this.dateModificationStr;
	}
	
	
	/**
	 * Sets the date (String) of last modification of the data type
	 * @param dateModificationStr date of last modification of the data type
	 */
	public void setDateModificationStr(String dateModificationStr)
	{
		this.dateModificationStr = dateModificationStr;
		
		// modification of the Date form of the last modification date
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		try
		{
			this.dateModification = dateFormat.parse(dateModificationStr);
		}
		catch (Exception e)
		{
			logger.error("Date conversion error (" + dateModificationStr + ")" + e);
			this.dateModification = new Date(0);   // 1st January 1970
		}
	}
	
	
	/**
	 * Check all the mandatory parameters of the data type, 
	 *  if something is missing or wrong, the method will return 'false'
	 * @return a boolean saying if the data type is valid or not
	 */
	public boolean isValid()
	{
		return !(RegistryUtilities.isEmpty(getName()) || RegistryUtilities.isEmpty(getDefinition()) || RegistryUtilities.isEmpty(getRegexp()) || (RegistryUtilities.isEmpty(getURL()) && RegistryUtilities.isEmpty(getURN())) || ((getDataEntriesPrefix().isEmpty()) || (getDataResources().isEmpty())));
	}
	
	
	/**
	 * Checks if the data type has (at least) one resource is official
	 * (there is at least one resource and the resources are not all deprecated)
	 * @return a boolean which says if the data type has (at least) one official resource
	 */
	public boolean hasOfficialResource()
	{
		boolean result = false;
		
		for (int i=0; i<(getResources()).size(); ++i)
		{
			// one resource (at least) is not obsolete
			if (! (getResource(i)).isObsolete())
			{
				result = true;
			}
		}
		
		return result; 
	}
	
	/*
	 * Removes the space at the beginning and at the end of the chain of characters
	 * @param original a string that usually comes from a HTML from
	 * @return the same string without any space at the beginning and at the end
	 */
	private String removeSpace(String original)
	{
		String spaceFree = new String();
		int index;
		int begin = 0;
		int end = 0;
		
		if (RegistryUtilities.isEmpty(original))
		{
			spaceFree = "";
		}
		else
		{
			// checks the beginning of the string
			index = 0;
			while ((index<original.length()) && (original.charAt(index) == ' '))
			{
				index ++;
			}
			begin = index;
			
			// check the end of the string
			index = original.length() - 1;
			while ((index>0) && (original.charAt(index) == ' '))
			{
				index --;
			}
			end = index;
			
			// creation of the substring
			spaceFree = original.substring(begin, end+1);
		}
		
		return spaceFree;
	}
	
	
    /**
     * 
     * @return the obsolete
     */
    public boolean isObsolete()
    {
        return this.obsolete;
    }
    
    
    /**
     * 
     * @param obsolete the obsolete to set
     */
    public void setObsolete(boolean obsolete)
    {
        this.obsolete = obsolete;
    }
    
    
    /**
     * 
     * @param obsolete the obsolete to set
     */
    public void setObsolete(int obsolete)
    {
        if (obsolete == 0)
        {
            this.obsolete = false;
        }
        else
        {
            this.obsolete = true;
        }
    }
    
    
    /**
     * 
     * @return the replacedBy
     */
    public String getReplacedBy()
    {
        return this.replacedBy;
    }
    
    
    /**
     * 
     * @param replacedBy the replacedBy to set
     */
    public void setReplacedBy(String replacedBy)
    {
        this.replacedBy = replacedBy;
    }
    
    
    /**
     * 
     * @return the comment
     */
    public String getObsoleteComment()
    {
        return this.obsoleteComment;
    }
    
    
    /**
     * 
     * @param obsoleteComment the comment to set
     */
    public void setObsoleteComment(String obsoleteComment)
    {
        this.obsoleteComment = obsoleteComment;
    }


	public String getNamespace() {
		return namespace;
	}


	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}


	public boolean isRestricted() {
		return restricted;
	}


	public void setRestricted(boolean restricted) {
		this.restricted = restricted;
	}

    /**
     * @return the list of restriction(s)
     */
    public List<Restriction> getRestrictions()
    {
        return this.restrictions;
    }
    
        
    /**
     * Adds a new restriction to the data collection.
     * @param restriction the restriction to set
     */
    public void addRestriction(Restriction restriction)
    {
        if (null == this.restrictions)
        {
            this.restrictions = new ArrayList<Restriction>();
        }
        this.restrictions.add(restriction);
    }
    
    /**
     * 
     * @param restrictions
     */
    public void setRestrictions(List<Restriction> restrictions)
    {
        this.restrictions = restrictions;
    }
	public int compareTo(DataType o)
	{
		return getId().compareTo(o.getId());
	}
	
}

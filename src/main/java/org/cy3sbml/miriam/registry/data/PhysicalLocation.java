package org.cy3sbml.miriam.registry.data;

import java.util.TreeSet;


/**
 * <p>
 * Stores all the information about a physical location of a {@link DataType}.
 * 
 * <p>
 * Implements {@link Comparable} to be able to use the objects of this class inside a {@link TreeSet}
 * for example.

 */
public class PhysicalLocation implements Comparable<Object>
{
  /** stable identifier of the PhysicalLocation (something starting by 'MIR:001' and followed by 5 digits) */
  private String id = new String();
  /** prefix part of the physical location (URL) */
  private String urlPrefix = new String();
  /** suffix part of the physical location (URL) */
  private String urlSuffix = new String();
  /** address of the front page of the PhysicalLocation */
  private String urlRoot = new String();
  /** some useful information about the PhysicalLocation */
  private String info = new String();
  /** institution which manage the PhysicalLocation */
  private String institution = new String();
  /** country of the institution */
  private String location = new String();
  /** example of an identifier used by this PhysicalLocation */
  private String example = new String();
  /** is the PhysicalLocation obsolete or not? */
  private boolean obsolete;
  /** is the PhysicalLocation the preferred one 
   * @deprecated replaced by the primary attribute
   */
  @Deprecated 
  private boolean preferred;
  /** is the PhysicalLocation the primary PhysicalLocation */
  private boolean primary;  
  /** the state of the PhysicalLocation. up or not up, ... */
  private String state;
  /** the reliability of the PhysicalLocation. Percentage of up time */
  private String reliability;


  /**
   * <p>Default constructor (empty object).
   */
  public PhysicalLocation()
  {
    // default parameters
    this.obsolete = false;
  }


  /**
   * Returns a string which contains all the information about the {@link PhysicalLocation}
   * 
   * @return a string which contains all the information about the {@link PhysicalLocation}
   */
  public String toString()
  {
    String tmp = new String();

    tmp += "       - ID:          " + getId() + "\n";
    tmp += "       - URL prefix:  " + getUrlPrefix() + "\n";
    tmp += "       - URL suffix:  " + getUrlSuffix() + "\n";
    tmp += "       - URL root:    " + getUrlRoot() + "\n";
    tmp += "       - Information: " + getInfo() + "\n";
    tmp += "       - Institution: " + getInstitution() + "\n";
    tmp += "       - Location:    " + getLocation() + "\n";
    tmp += "       - Example:     " + getExample() + "\n";
    tmp += "       - Obsolete:    " + isObsolete() + "\n";
    tmp += "       - Preferred:   " + isPreferred() + "\n";
    tmp += "       - Primary:     " + isPrimary() + "\n";
    tmp += "       - State:       " + getState() + "\n";
    tmp += "       - Reliability: " + getReliability() + "\n";
    
    return tmp;
  }


  /**
   * Tests if two <code>Resource</code> objects are the same (only checks the ID).
   * @see Object#equals(Object)
   */
  public boolean equals(PhysicalLocation res)
  {
    return (this.id.equals(res.id));
  }


  /**
   * Checks if two <code>Resource</code> objects have the same content (and same ID).
   * @param res the other <code>Resource</code> to compare to
   * @return true is the {@link PhysicalLocation} have the same content.
   */
  public boolean hasSameContent(PhysicalLocation res)
  {
    return ((this.id.equals(res.id)) &&
        (this.urlPrefix.equals(res.urlPrefix)) &&
        (this.urlSuffix.equals(res.urlSuffix)) &&
        (this.urlRoot.equals(res.urlRoot)) &&
        (this.info.equals(res.info)) &&
        (this.institution.equals(res.institution)) &&
        (this.location.equals(res.location)) &&
        (this.example.equals(res.example)) &&
        (this.obsolete == res.obsolete) &&
        (this.preferred == res.preferred) &&
        (this.primary == res.primary) &&
        (this.state == res.state) &&
        (this.reliability == res.reliability)); 
  }


    /**
     * Checks if two <code>Resource</code> are similar (based on simple statistics studies).
     *
     * <p>
     * 7 attributes take into account (url_prefix, url_suffix, url_root, info, institution, location, obsolete).
     *
     * @param res the other <code>Resource</code> to compare to
     * @return 'true' if number of similarities &gt;= 4 (7 attributes tested)
     */
    public boolean couldBeSimilar(PhysicalLocation res)
    {
        int nb = 0;

        if (this.urlPrefix.equals(res.urlPrefix))
        {
            nb ++;
        }
        if (this.urlSuffix.equals(res.urlSuffix)){
            nb ++;
        }
        if (this.urlRoot.equals(res.urlRoot))
        {
            nb ++;
        }
        if (this.info.equals(res.info))
        {
            nb ++;
        }
        if (this.institution.equals(res.institution))
        {
            nb ++;
        }
        if (this.location.equals(res.location))
        {
            nb ++;
        }
        if (this.obsolete == res.obsolete)
        {
            nb ++;
        }

        return (nb >= 4);
    }


    /**
     * Gets the stable identifier of the resource
     * 
     * @return the stable identifier of the resource
     */
    public String getId()
    {
      return id;
    }


    /**
     * Sets the stable identifier of the resource
     * 
     * @param id the stable identifier of the resource
     */
    public void setId(String id)
    {
      this.id = id;
    }


  /**
   * Getter of some general information about the resource
   * @return some general information about the resource
   */
  public String getInfo()
  {
    return info;
  }


  /**
   * Setter of some general information about the resource
   * @param info some general information about the resource
   */
  public void setInfo(String info)
  {
    this.info = info;
  }

  /**
   * Getter of the institution managing the resource
   * @return the institution managing the resource
   */
  public String getInstitution()
  {
    return institution;
  }


  /**
   * Setter of the institution managing the resource
   * @param institution the institution managing the resource
   */
  public void setInstitution(String institution)
  {
    this.institution = institution;
  }


  /**
   * Getter of the country of the institution
   * @return the country of the institution
   */
  public String getLocation()
  {
    return location;
  }


  /**
   * Setter of the country of the institution
   * @param location the country of the institution
   */
  public void setLocation(String location)
  {
    this.location = location;
  }


  /**
   * Getter of the obsolete parameter
   * @return if the resource is obsolete or not
   */
  public boolean isObsolete()
  {
    return obsolete;
  }


  /**
   * Setter of the obsolete parameter
   * @param obsolete the resource is obsolete or not (that is the question)
   */
  public void setObsolete(boolean obsolete)
  {
    this.obsolete = obsolete;
  }


  /**
   * Getter of the prefix part of the address (link to an element)
   * @return the prefix part of the address (link to an element)
   */
  public String getUrlPrefix()
  {
    return urlPrefix;
  }
  
  
  /**
   * Setter of the prefix part of the address (link to an element)
   * @param url_prefix the prefix part of the address (link to an element)
   */
  public void setUrlPrefix(String url_prefix)
  {
    this.urlPrefix = url_prefix;
  }
  
  
  /**
   * Getter of the resource address (front page)
   * @return the resource address (front page)
   */
  public String getUrlRoot()
  {
    return urlRoot;
  }
  
  
  /**
   * Setter of the resource address (front page)
   * @param url_root the resource address (front page)
   */
  public void setUrlRoot(String url_root)
  {
    this.urlRoot = url_root;
  }
  
  
  /**
   * Getter of the suffix part of the address (link to an element)
   * @return the suffix part of the address (link to an element)
   */
  public String getUrlSuffix()
  {
    return urlSuffix;
  }
  
  
  /**
   * Setter of the suffix part of the address (link to an element)
   * @param url_suffix the suffix part of the address (link to an element)
   */
  public void setUrlSuffix(String url_suffix)
  {
    this.urlSuffix = url_suffix;
  }
  
  
  /**
   * Getter of the example
   * @return the example
   */
  public String getExample()
  {
    return this.example;
  }
  
  
  /**
   * Setter of the example.
   * @param example the example to set
   */
  public void setExample(String example)
  {
    this.example = example;
  }
  

  /**
   * Returns the reliability of the {@link PhysicalLocation}, represented
   * by the percentage of detected up time.
   * 
   * @return the reliability of the {@link PhysicalLocation}
   */
  public String getReliability() {
    return reliability;
  }


  /**
   * Sets the reliability
   * 
   * @param reliability the reliability of the {@link PhysicalLocation}
   */
  public void setReliability(String reliability) {
    this.reliability = reliability;
  }


  /**
   * Returns the state of the {@link PhysicalLocation}, meaning if it is 
   * detected as up, probably up, down or other.
   * 
   * @return the state of the {@link PhysicalLocation}
   */
  public String getState() {
    return state;
  }


  /**
   * Sets the state
   * 
   * @param state the state of the {@link PhysicalLocation}
   */
  public void setState(String state) {
    this.state = state;
  }


  /**
   * Returns true is this {@link PhysicalLocation} is the preferred one
   * for the {@link DataType}.
   * 
   * @return true is this {@link PhysicalLocation} is the preferred one
   * for the {@link DataType}.
   */
  public boolean isPreferred() {
    return preferred;
  }


  /**
   * Sets the preferred attribute.
   * 
   * @param preferred the preferred attribute for this {@link PhysicalLocation}.
   */
  public void setPreferred(boolean preferred) {
    this.preferred = preferred;
  }

  

  /**
   * Returns true if this {@link PhysicalLocation} is the primary one.
   * 
   * @return true if this {@link PhysicalLocation} is the primary one.
   */
  public boolean isPrimary() {
    return primary;
  }


  /**
   * Sets the primary attribute.
   * 
   * @param primary the primary to set
   */
  public void setPrimary(boolean primary) {
    this.primary = primary;
  }


  /**
   * Compares to objects and determine whether they are equivalent or not.
   * 
   * Mandatory method for the class to be able to implements 'Comparable'
   * <p>
   * WARNING: the test only uses the ID of the Resource object!
   * 
   * @param obj unknown object
   * @return 0 if the two objects are the same
   */
  public int compareTo(Object obj)
  {
    PhysicalLocation res = (PhysicalLocation) obj;

    // different identifiers
    if ((this.getId()).compareToIgnoreCase(res.getId()) != 0)
    {
      return -1;
    }
    else   // same identifier
    {
      return 0;
    }
  }


  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 113;
    int result = 1;
    result = prime * result + ((example == null) ? 0 : example.hashCode());
    result = prime * result + ((id == null) ? 0 : id.hashCode());
    result = prime * result + ((info == null) ? 0 : info.hashCode());
    result = prime * result
        + ((institution == null) ? 0 : institution.hashCode());
    result = prime * result + ((location == null) ? 0 : location.hashCode());
    result = prime * result + (obsolete ? 1231 : 1237);
    result = prime * result + (preferred ? 1231 : 1237);
    result = prime * result + (primary ? 1231 : 1237);
    result = prime * result
        + ((reliability == null) ? 0 : reliability.hashCode());
    result = prime * result + ((state == null) ? 0 : state.hashCode());
    result = prime * result + ((urlPrefix == null) ? 0 : urlPrefix.hashCode());
    result = prime * result + ((urlRoot == null) ? 0 : urlRoot.hashCode());
    result = prime * result + ((urlSuffix == null) ? 0 : urlSuffix.hashCode());
    return result;
  }


  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    PhysicalLocation other = (PhysicalLocation) obj;
    if (example == null) {
      if (other.example != null) {
        return false;
      }
    } else if (!example.equals(other.example)) {
      return false;
    }
    if (id == null) {
      if (other.id != null) {
        return false;
      }
    } else if (!id.equals(other.id)) {
      return false;
    }
    if (info == null) {
      if (other.info != null) {
        return false;
      }
    } else if (!info.equals(other.info)) {
      return false;
    }
    if (institution == null) {
      if (other.institution != null) {
        return false;
      }
    } else if (!institution.equals(other.institution)) {
      return false;
    }
    if (location == null) {
      if (other.location != null) {
        return false;
      }
    } else if (!location.equals(other.location)) {
      return false;
    }
    if (obsolete != other.obsolete) {
      return false;
    }
    if (preferred != other.preferred) {
      return false;
    }
    if (primary != other.primary) {
      return false;
    }
    if (reliability == null) {
      if (other.reliability != null) {
        return false;
      }
    } else if (!reliability.equals(other.reliability)) {
      return false;
    }
    if (state == null) {
      if (other.state != null) {
        return false;
      }
    } else if (!state.equals(other.state)) {
      return false;
    }
    if (urlPrefix == null) {
      if (other.urlPrefix != null) {
        return false;
      }
    } else if (!urlPrefix.equals(other.urlPrefix)) {
      return false;
    }
    if (urlRoot == null) {
      if (other.urlRoot != null) {
        return false;
      }
    } else if (!urlRoot.equals(other.urlRoot)) {
      return false;
    }
    if (urlSuffix == null) {
      if (other.urlSuffix != null) {
        return false;
      }
    } else if (!urlSuffix.equals(other.urlSuffix)) {
      return false;
    }
    return true;
  }
  
  

}

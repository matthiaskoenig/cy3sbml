package org.cy3sbml.miriam.registry.data;

/**
 * Object which stores all the information about a kind of restriction.
 */
public class RestrictionType
{
    private Integer id;   // internal identifier
    private String category;   // few words description (shared)
    private String desc;   // one sentence description (shared)
    
    
    /**
     * Default constructor.
     */
    public RestrictionType()
    {
        this.id = null;
        this.category = null;
        this.desc = null;
    }
    
    /**
     * Default constructor.
     */
    public RestrictionType(Integer id, String category, String desc)
    {
        this.id = id;
        this.category = category;
        this.desc = desc;
    }
    
    
    /**
     * Indicates whether some other object is "equal to" this one.
     */
    @Override
    public boolean equals(Object obj)
    {
        return ((this.id).compareTo(((RestrictionType) obj).id) == 0);
    }
    
    /**
     * Compares two object of type <code>RestrictionType</code> based on their identifier.
     */
    public int compareTo(Object type)
    {
        RestrictionType data = (RestrictionType) type;
        return ((this.id).compareTo(data.id));
    }
    
    
    /**
     * Returns a string representation of this object.
     */
    @Override
    public String toString()
    {
        StringBuilder tmp = new StringBuilder();
        
        tmp.append("Type of restriction:\n");
        tmp.append("  Category:    " + getCategory() + " (" + getId() + ")\n");
        tmp.append("  Description: " + getDesc() + "\n");
        
        return tmp.toString();
    }
    
    
    /**
     * Getter
     * @return the internal id
     */
    public Integer getId()
    {
        return this.id;
    }
    
    /**
     * Setter
     * @param id the internal id to set
     */
    public void setId(Integer id)
    {
        this.id = id;
    }
    
    /**
     * Getter
     * @return the category
     */
    public String getCategory()
    {
        return this.category;
    }
    
    /**
     * Setter
     * @param category the category to set
     */
    public void setCategory(String category)
    {
        this.category = category;
    }
    
    /**
     * Getter
     * @return the desc
     */
    public String getDesc()
    {
        return this.desc;
    }
    
    /**
     * Setter
     * @param desc the desc to set
     */
    public void setDesc(String desc)
    {
        this.desc = desc;
    }
}

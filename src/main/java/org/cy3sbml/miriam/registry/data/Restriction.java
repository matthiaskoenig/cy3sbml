/*
 * The identifiers.org registry is an online resource created to 
 * catalogue biological data types,
 * their URIs and the corresponding physical URLs,
 * whether these are controlled vocabularies or databases.
 * Ref. http://identifiers.org/registry
 *
 * Copyright (C) 2006-2015  EMBL-EBI - European Bioinformatics Institute
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 */


package org.cy3sbml.miriam.registry.data;


/**
 * <p>
 * Object which stores all the information about one restriction associated with a data collection.
 */
public class Restriction
{
    private Integer id;   // internal identifier
    private String info;   // specific to this data collection
    private String link;   // optional (specific to this data collection)
    private String linkText;   // optional (specific to this data collection)
    private RestrictionType type;   // type of restriction
    
    
    /**
     * Default constructor.
     */
    public Restriction()
    {
        this.id = null;
        this.info = null;
        this.link = null;
        this.linkText = null;
        this.setType(new RestrictionType());
    }
    
    
    /**
     * Indicates whether some other object is "equal to" this one.
     */
    @Override
    public boolean equals(Object obj)
    {
        return ((this.id).compareTo(((Restriction) obj).id) == 0);
    }
    
    
    /**
     * Compares two object of type <code>Restriction</code> based on their identifier.
     */
    public int compareTo(Object restriction)
    {
        Restriction data = (Restriction) restriction;
        return ((this.id).compareTo(data.id));
    }
    
    
    /**
     * Getter
     * @return the info
     */
    public String getInfo()
    {
        return this.info;
    }
    
    /**
     * Setter
     * @param info the info to set
     */
    public void setInfo(String info)
    {
        this.info = info;
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
     * @return the link
     */
    public String getLink()
    {
        return this.link;
    }
    
    /**
     * Setter
     * @param link the link to set
     */
    public void setLink(String link)
    {
        this.link = link;
    }
    
    /**
     * Getter
     * @return the linkText
     */
    public String getLinkText()
    {
        return this.linkText;
    }
    
    /**
     * Setter
     * @param linkText the linkText to set
     */
    public void setLinkText(String linkText)
    {
        this.linkText = linkText;
    }
    
    /**
     * Getter
     * @return
     */
    public RestrictionType getType()
    {
        return this.type;
    }
    
    /**
     * Setter
     * @param type
     */
    public void setType(RestrictionType type)
    {
        this.type = type;
    }
}

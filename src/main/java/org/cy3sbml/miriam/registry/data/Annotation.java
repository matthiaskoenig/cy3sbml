package org.cy3sbml.miriam.registry.data;

import java.util.ArrayList;


/** Annotation part of a data type. */
public class Annotation {
    private String format;  // can be SBML, CellML, BioPAX, ...
    private ArrayList<Tag> tags;  // list of Tag(s)

    /** Default constructor. */
    public Annotation(String format) {
        this.format = format;
        this.tags = new ArrayList<>();
    }

    /** Constructor with parameter. */
    public Annotation(String format, ArrayList<Tag> tags)
    {
       this.format = format;
       this.tags = new ArrayList<Tag>(tags);
    }

    /** Returns string representation of the object. */
    public String toString() {
        StringBuffer str = new StringBuffer();
        
        str.append("Annotation\n");
        str.append("- format: " + this.format + "\n");
        str.append("- tags: \n");
        for (int i=0; i<this.getTags().size(); ++i) {
            str.append(this.getTag(i).toString("\t"));
        }
        return str.toString();
    }

    /** Get format. */
    public String getFormat()
    {
        return this.format;
    }

    /** Set format. */
    public void setFormat(String format)
    {
        this.format = format;
    }

    /** Get tags. */
    public ArrayList<Tag> getTags()
    {
        return this.tags;
    }

    /** Get specific tag (using its index) */
    public Tag getTag(int index)
    {
        return this.tags.get(index);
    }

    /** Set tags. */
    public void setTags(ArrayList<Tag> tags)
    {
        this.tags = new ArrayList<>(tags);
    }

    /** Adds a tag to the current list of tags of the annotation. */
    public void addTag(Tag tag)
    {
        this.tags.add(tag);
    }
}

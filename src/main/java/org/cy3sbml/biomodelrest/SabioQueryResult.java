package org.cy3sbml.biomodelrest;

import org.sbml.jsbml.History;
import org.sbml.jsbml.JSBML;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;

import javax.xml.stream.XMLStreamException;
import java.util.ArrayList;
import java.util.Date;

/**
 * Result of the given web service query.
 */
public class SabioQueryResult {

	final private String query;
	final private Integer status;
	final private String xml;
	final private SBMLDocument doc;
	
	public SabioQueryResult(final String query, Integer status, String xml){
		this.query = query;
		this.status = status;
		this.xml = xml;
		this.doc = docWithInformation(xml, query);
	}
	
	/** Returns true if the request was successful. */
	public boolean success(){
		return (status == 200);
	}
	
	public String getQuery(){
		return query;
	}
	
	public SBMLDocument getSBMLDocument(){
		return doc;
	}

	public String getXML(){
	    return xml;
    }
	
	public Integer getStatus(){
		return status;
	}
	
	/*
	 * Read the kineticLaws from the given SBML. 
	 * xml must not be SBML, i.e. some query results are xml but not SBML. 
	 */
	public ArrayList<SabioKineticLaw> getKineticLaws(){
		return SabioKineticLaw.parseKineticLaws(doc);
	}

	/**
	 * Inserts additional XML information into the file.
	 */
	private static SBMLDocument docWithInformation(String xml, String query){
	    SBMLDocument doc = null;
        try {
            doc = JSBML.readSBMLFromString(xml);
            Model model = doc.getModel();

            // ModelHistory with create date
            History h = model.createHistory();
            Date date = new Date();
            h.setCreatedDate(date);

            // Update notes
            String notes = model.getNotesString();
            String start = "<body xmlns=\"http://www.w3.org/1999/xhtml\">\n";
            // insert query & cy3sabiork information
            String addition = "<h3>cy3sabiork web service query</h3>\n" +
                    "<p>This SBML model was created using <b><a href=\"https://github.com/matthiaskoenig/cy3sabiork\">cy3sabiork</a></b><br />\n" +
                    "<b>Query</b>: <code>" + query + "</code><br />\n" +
                    "<b>Date</b>: <code>" + date + "</code><br />\n" +
                    "</p>\n";
            notes = notes.replace(start, start+addition);
            // System.out.println("------------------------");
            // System.out.println(notes);
            //System.out.println("------------------------");
            model.setNotes(notes);



        } catch (XMLStreamException e) {
            e.printStackTrace();
        }
        return doc;
	}

}

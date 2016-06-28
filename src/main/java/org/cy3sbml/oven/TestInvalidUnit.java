package org.cy3sbml.oven;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.JSBML;
import org.sbml.jsbml.KineticLaw;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.UnitDefinition;

public class TestInvalidUnit {
	
	public static void main(String[] args) throws XMLStreamException, IOException{
		String path = "/home/mkoenig/git/cy3sbml/src/test/resources/models/BioModels-r29_sbml_curated/BIOMD0000000002.xml";
		
		// from file okay
		SBMLDocument doc = JSBML.readSBMLFromFile(path);
		Model model = doc.getModel();
		for (Reaction r: model.getListOfReactions()){
			if (r.isSetKineticLaw()){
				KineticLaw law = r.getKineticLaw();
				UnitDefinition ud = law.getDerivedUnitDefinition();
			}
		}
	}
	
}

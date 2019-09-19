package org.cy3sbml.oven;


import org.sbml.jsbml.JSBML;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;


public class InvalidReactionUnits {

    public static void main(String[] args) throws IOException, XMLStreamException {

        SBMLDocument doc = JSBML.readSBMLFromFile("/home/mkoenig/Desktop/limax_pkpd_39.xml");
        Model model = doc.getModel();
        for (Reaction reaction: model.getListOfReactions()){
            System.out.println(reaction + " " + reaction.getKineticLaw().getDerivedUnits());
        }
    }
}

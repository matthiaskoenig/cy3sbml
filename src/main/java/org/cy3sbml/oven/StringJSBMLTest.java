package org.cy3sbml.oven;

import org.sbml.jsbml.*;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;


public class StringJSBMLTest {


    public static void main(String[] args) throws IOException, XMLStreamException {
        String path = "/home/mkoenig/git/cy3sbml/src/main/java/org/cy3sbml/oven/yeast_glycolysis.xml";
        SBMLDocument doc = JSBML.readSBMLFromFile(path);
        System.out.println(doc);

        Model model = doc.getModel();
        for (Reaction r: model.getListOfReactions()){
            System.out.println(r);
            String units = r.getDerivedUnits();
            System.out.println(units);

            NamedSBaseWithDerivedUnit nsdu = (NamedSBaseWithDerivedUnit) r;
            units = nsdu.getDerivedUnits();
            System.out.println(units);
        }
    }

}

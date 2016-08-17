package org.cy3sbml.oven;

import org.sbml.jsbml.*;
import javax.xml.stream.XMLStreamException;


public class JSBMLNotesTest {

    private static final Integer SBML_LEVEL = 3;
    private static final Integer SBML_VERSION = 1;

    public static void main(String[] args) throws XMLStreamException {

        // you can just set the level and version in the constructor
        // will be used for all SBases afterwards as far as I know
        SBMLDocument sbmldoc = new SBMLDocument(SBML_LEVEL, SBML_VERSION);

        Model sbmlmodel = sbmldoc.createModel("MODEL1");
        Parameter par = sbmlmodel.createParameter("test");

        par.setNotes("testing notes");

        String sbml = new SBMLWriter().writeSBMLToString(sbmldoc);
        System.out.println(sbml);


        // nicer output with tidy writer
        System.out.println("--------------------------------------");
        String sbmlTidy = new TidySBMLWriter().writeSBMLToString(sbmldoc);
        System.out.println(sbmlTidy);
    }
}

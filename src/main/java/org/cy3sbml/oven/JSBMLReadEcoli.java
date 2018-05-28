package org.cy3sbml.oven;

import org.sbml.jsbml.*;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;


public class JSBMLReadEcoli {

    public static void main(String[] args) throws XMLStreamException, IOException {

        System.out.println("Read ecoli_core 1");
        SBMLDocument doc = JSBML.readSBMLFromFile("/home/mkoenig/Desktop/e_coli_core.sbml");
        System.out.println("Read ecoli_core 2");
        doc = SBMLReader.read(new File("/home/mkoenig/Desktop/e_coli_core.sbml"));
    }
}

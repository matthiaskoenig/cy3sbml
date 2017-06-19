package org.cy3sbml.oven;

import org.sbml.jsbml.JSBML;
import org.sbml.jsbml.SBMLDocument;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;

/**
 * Created by mkoenig on 12.09.16.
 */
public class PropertyNotAvailableExample {


    public static void main(String[] args) throws IOException, XMLStreamException {
        SBMLDocument doc = JSBML.readSBMLFromFile("/home/mkoenig/Desktop/01205-sbml-l1v2.xml");

    }
}

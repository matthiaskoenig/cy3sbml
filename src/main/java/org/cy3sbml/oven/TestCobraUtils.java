package org.cy3sbml.oven;


import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.Properties;

import org.sbml.jsbml.*;
import org.sbml.jsbml.util.CobraUtil;

public class TestCobraUtils {
    public static void main(String[] args) throws XMLStreamException, IOException {
        System.setProperty("logfile.name","/tmp/test.log");

        String path = "/home/mkoenig/git/cy3sbml/src/main/java/org/cy3sbml/oven/COBRAUtils.xml";
        System.out.println("<--------------------->");

        // from file okay
        SBMLDocument doc = JSBML.readSBMLFromFile(path);
        NamedSBase sbase = doc.getModel();

        Properties props = CobraUtil.parseCobraNotes(sbase);
        for(Object key : props.keySet()){
            String keyString = key.toString();
            String valueString = props.getProperty((String) key);
            System.out.println(keyString + " : " + valueString);
        }

        System.out.println("<--------------------->");
    }
}

package org.cy3sbml.oven;


import org.sbml.jsbml.JSBML;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLErrorLog;
import org.sbml.jsbml.validator.SBMLValidator;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


public class ValidatorUnitsMissing {
    /**
     * Full validation of SBMLDocument.
     *
     * One can control the consistency checks that are performed when
     * checkConsistency() is called with the
     * setConsistencyChecks(SBMLValidator.CHECK_CATEGORY, boolean).
     */
    public static SBMLErrorLog validateSBML(SBMLDocument doc) {

        doc.setConsistencyChecks(SBMLValidator.CHECK_CATEGORY.UNITS_CONSISTENCY, true);
        doc.setConsistencyChecks(SBMLValidator.CHECK_CATEGORY.IDENTIFIER_CONSISTENCY, true);
        doc.setConsistencyChecks(SBMLValidator.CHECK_CATEGORY.GENERAL_CONSISTENCY, true);
        doc.setConsistencyChecks(SBMLValidator.CHECK_CATEGORY.SBO_CONSISTENCY, true);
        doc.setConsistencyChecks(SBMLValidator.CHECK_CATEGORY.MATHML_CONSISTENCY, true);
        doc.setConsistencyChecks(SBMLValidator.CHECK_CATEGORY.OVERDETERMINED_MODEL, true);
        doc.setConsistencyChecks(SBMLValidator.CHECK_CATEGORY.MODELING_PRACTICE, true);

        // JSBML offline validator
        // Integer code = doc.checkConsistencyOffline();

        // Online validator
        Integer code = doc.checkConsistency();

        if (code < 0) {
            System.out.println("Error in SBML validation");
        }
        return doc.getErrorLog();
    }


    public static void main(String[] args) throws IOException, XMLStreamException {

        SBMLDocument doc = JSBML.readSBMLFromFile("/home/mkoenig/Desktop/BIOMD0000000001.xml");
        SBMLErrorLog errorLog = validateSBML(doc);
        assertNotNull(errorLog);
        assertEquals(60, errorLog.getErrorCount());
    }

}

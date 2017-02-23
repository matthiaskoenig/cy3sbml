package org.cy3sbml.oven;


import org.cy3sbml.util.SBMLUtil;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLErrorLog;


/**
 * Testing out of bounds issue
 */
public class ValidationOutOfBounds {
    public static final String TEST_MODEL = "/models/tests/diauxic_fba.xml";

    /**
     * Validation function showing the error
     * @param doc
     * @return
     */
    public static SBMLErrorLog validateSBML(SBMLDocument doc) {

        // Online validator
        Integer code = doc.checkConsistency();

        if (code < 0) {
            System.out.println("Error in SBML validation");
        }
        return doc.getErrorLog();
    }

    public static void main(String[] args) throws Exception {
        System.out.println(TEST_MODEL);
        SBMLDocument doc = SBMLUtil.readSBMLDocument(TEST_MODEL);
        SBMLErrorLog errorLog = validateSBML(doc);
    }
}

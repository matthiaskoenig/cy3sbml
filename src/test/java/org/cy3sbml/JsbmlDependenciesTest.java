package org.cy3sbml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Parameter;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLError;

/**
 * The runtime dependencies JSBML needs are on the classpath of the app.
 */
class JsbmlDependenciesTest {

    /**
     * JSBML creates the error of an invalid value with its SBMLErrorFactory, which
     * reads the error messages from its SBMLErrors.json resource with json-simple, and adds it
     * to the error log.
     */
    @Test
    void invalidUnitsAreAddedToTheErrorLog() {
        SBMLDocument document = new SBMLDocument(3, 1);
        Model model = document.createModel("m");
        Parameter parameter = model.createParameter("p");

        parameter.setUnits("undefined_unit");

        assertEquals(1, document.getErrorLog().getNumErrors());
        // the message comes from SBMLErrors.json, which must be in the JSBML jar
        SBMLError error = document.getErrorLog().getError(0);
        assertEquals(10313, error.getCode());
        assertFalse(error.getMessage().isBlank(), error.toString());
    }
}

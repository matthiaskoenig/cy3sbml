package org.cy3sbml;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Parameter;
import org.sbml.jsbml.SBMLDocument;

/**
 * The runtime dependencies JSBML needs are on the classpath of the app.
 */
class JsbmlDependenciesTest {

    /**
     * JSBML creates the error of an invalid value with its SBMLErrorFactory, which
     * reads the error messages with json-simple, and adds it to the error log.
     */
    @Test
    void invalidUnitsAreAddedToTheErrorLog() {
        SBMLDocument document = new SBMLDocument(3, 1);
        Model model = document.createModel("m");
        Parameter parameter = model.createParameter("p");

        parameter.setUnits("undefined_unit");

        assertEquals(1, document.getErrorLog().getNumErrors());
    }
}

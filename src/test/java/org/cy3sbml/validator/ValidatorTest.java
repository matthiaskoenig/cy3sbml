package org.cy3sbml.validator;

import org.cy3sbml.SBMLCoreTest;
import org.cy3sbml.util.SBMLUtil;
import org.junit.Test;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLErrorLog;


import static org.junit.Assert.*;


public class ValidatorTest {
    @Test
    public void validateSBML() throws Exception {
        SBMLDocument doc = SBMLUtil.readSBMLDocument(SBMLCoreTest.TEST_MODEL_CORE_01);
        Validator validator = new Validator(doc);
        assertNotNull(validator);
        assertEquals(true, validator.getValid());
        SBMLErrorLog errorLog = validator.getErrorLog();
        assertNotNull(errorLog);
        assertEquals(60, errorLog.getErrorCount());
    }

    @Test
    public void createHtml() throws Exception {
        SBMLDocument doc = SBMLUtil.readSBMLDocument(SBMLCoreTest.TEST_MODEL_CORE_01);
        Validator validator = new Validator(doc);
        assertNotNull(validator);
        String html = validator.createHtml();
        assertNotNull(html);
    }

}
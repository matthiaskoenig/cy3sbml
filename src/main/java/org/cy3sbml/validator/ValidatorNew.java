package org.cy3sbml.validator;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.cy3sbml.gui.SBaseHTMLFactory;
import org.cy3sbml.util.SBMLUtil;
import org.sbml.jsbml.*;
import org.sbml.jsbml.validator.SBMLValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Validation of SBMLDocuments and creation of
 * validation reports including errors and warnings.
 *
 * This should run the validation and create the HTML
 * output of the report.
 * The validation results are stored and can be easily retrieved.
 *
 * Implemented via new JSBML functionality.
 *
 * TODO: add options for filtering (SBMLValidator.CHECK_CATEGORY);
 * TODO: unittests
 * TODO: caching of validation
 * TODO: html report
 * TODO: implement new version based on JSBML validator
 */
public class ValidatorNew {
	private static final Logger logger = LoggerFactory.getLogger(ValidatorNew.class);

    public static final List<String> SEVERITIES;
    static {
        List<String> list = new LinkedList<>();
        list.add("Fatal");
        list.add("Error");
        list.add("Warning");
        list.add("Info");
        SEVERITIES = Collections.unmodifiableList(list);
    }

    private Boolean valid;
	private SBMLErrorLog errorLog;
	private Map<String, List<SBMLError>> errorMap;

	public ValidatorNew(SBMLDocument doc){
		try {
			errorLog = validateSBML(doc);
			createErrorMap();
		} catch (Exception e) {
			logger.warn("SBMLDocument could not be validated.", e);
			e.printStackTrace();
			reset();
		}
	}

    /** Reset the validator. */
	private void reset(){
	    valid = false;
		errorLog = null;
		errorMap = null;
	}

	/**
     * Here the validation is performed.
     *
     * One can control the consistency checks that are performed when
     * checkConsistency() is called with the
     * setConsistencyChecks(SBMLValidator.CHECK_CATEGORY, boolean)
     */
	public static SBMLErrorLog validateSBML(SBMLDocument doc){

        doc.setConsistencyChecks(SBMLValidator.CHECK_CATEGORY.UNITS_CONSISTENCY, true);
	    doc.setConsistencyChecks(SBMLValidator.CHECK_CATEGORY.IDENTIFIER_CONSISTENCY, true);
        doc.setConsistencyChecks(SBMLValidator.CHECK_CATEGORY.GENERAL_CONSISTENCY, true);
        doc.setConsistencyChecks(SBMLValidator.CHECK_CATEGORY.SBO_CONSISTENCY, true);
        doc.setConsistencyChecks(SBMLValidator.CHECK_CATEGORY.MATHML_CONSISTENCY, true);
        doc.setConsistencyChecks(SBMLValidator.CHECK_CATEGORY.OVERDETERMINED_MODEL, true);
        doc.setConsistencyChecks(SBMLValidator.CHECK_CATEGORY.MODELING_PRACTICE, true);

	    Integer code = doc.checkConsistency();
        if (code < 0){
            logger.error("Error validating SBML file");
        }
        return doc.getErrorLog();
	}

    /**
     * Store the errors in HashMap by severity.
     */
	private void createErrorMap(){
        errorMap = new HashMap<>();
	    if (errorLog == null){
	        logger.warn("No error log available");
			return;
		}
		System.out.println("Number of errors: " +  errorLog.getNumErrors());
        valid = true;
		for (SBMLError error : errorLog.getValidationErrors()){
            // store under severity
            String severity = error.getSeverity();
            if (error.isError() || error.isFatal()){
                valid = false;
            }
            List<SBMLError> errors = errorMap.getOrDefault(severity, new LinkedList<>());
            errors.add(error);
            errorMap.put(severity, errors);
            System.out.println(error);
		}
	}

    /**
     * Create HTML of validator output.
     * FIXME: set baseDir and title
     */
    public String createHTML(){
        String baseDir = "file:///home/mkoenig/git/cy3sbml/src/main/resources/gui/";
        String title = "title";

        String html = String.format(SBaseHTMLFactory.HTML_START_TEMPLATE, baseDir, title);
        html += String.format(
                "<h2>%s%s</h2>\n",
                SBaseHTMLFactory.EXPORT_HTML, "SBML Validation");


        String validStr = "valid";
        if (!valid){
            validStr = "invalid";
        }
        html += String.format("<h3 class=\"%s\">This document is %s SBML</h3>\n", validStr, validStr);
        html += SBaseHTMLFactory.TABLE_START;
        for (String severity: errorMap.keySet()){
            int count = 1;
            List<SBMLError> errors = errorMap.get(severity);
            for (SBMLError e : errors){
                html += "\t<tr><td>\n";
                html += String.format("\t\t<span class=\"error%s\">E%d %s (%s)</span><br />\n", severity, count, e.getCategory(), e.getSeverity());
                // html += String.format("\t\t<span class=\"errorShortMessage\">%s</span><br />\n", e.getShortMessage());
                html += String.format("\t\t<span class=\"errorLine\">Line: %s </span>\n", e.getLine());
                html += String.format("\t\t<span class=\"errorMessage\">%s</span>\n", StringEscapeUtils.escapeHtml(e.getMessage()));
                html += "\t</td></tr>\n";
                count++;
            }
        }
        html += SBaseHTMLFactory.TABLE_END;
        html += SBaseHTMLFactory.HTML_STOP_TEMPLATE;
        return html;
    }


	/**
     * Create example validation HTML report.
	 */
	public static void main(String[] args) throws IOException {
        String baseDir = "file:///home/mkoenig/git/cy3sbml/src/main/resources/gui/";
        SBMLDocument doc = SBMLUtil.readSBMLDocument("/models/BIOMD0000000001.xml");
        Model model = doc.getModel();
        System.out.println(model.getId());
        ValidatorNew validator = new ValidatorNew(doc);
        String html = validator.createHTML();
        FileUtils.writeStringToFile(new File("/home/mkoenig/tmp/validation.html"), html);
    }

}

package org.cy3sbml.validator;

import org.apache.commons.codec.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.cy3sbml.gui.BrowserHyperlinkListener;
import org.cy3sbml.gui.SBaseHTMLFactory;
import org.cy3sbml.util.SBMLUtil;
import org.sbml.jsbml.*;
import org.sbml.jsbml.validator.SBMLValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validator of SBMLDocuments and creation of
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
 * TODO: implement new version based on JSBML validator (offline validator)
 */
public class Validator {
	private static final Logger logger = LoggerFactory.getLogger(Validator.class);

    public static final String VALID = "valid";
    public static final String INVALID = "invalid";
    public static final String EXPORT_HTML = String.format(
            "<small><a href=\"%s\"><span class=\"fa fa-share-square-o\" aria-hidden=\"true\" style=\"color:black\" title=\"Export HTML information\"></span></a></small>&nbsp;&nbsp;",
            BrowserHyperlinkListener.URL_HTML_VALIDATION);

    public static final List<String> SEVERITIES;
    static {
        List<String> list = new LinkedList<>();
        list.add("Fatal");
        list.add("Error");
        list.add("Warning");
        list.add("Info");
        SEVERITIES = Collections.unmodifiableList(list);
    }

    private SBMLDocument document;
    private Boolean valid;
	private SBMLErrorLog errorLog;
	private Map<String, List<SBMLError>> errorMap;

	public Validator(SBMLDocument doc){
	    this.document = doc;
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
		}
	}

    /**
     * Create HTML of validator output.
     */
    public String createHtml(){
        // title
        String title = "Model";
        if (document.isSetModel()){
            Model model = document.getModel();
            if (model.isSetId()){
                title = model.getId();
            } else if (model.isSetName()){
                title = model.getName();
            }
        }

        // html
        String html = String.format(SBaseHTMLFactory.HTML_START_TEMPLATE, SBaseHTMLFactory.getBaseDir(), title);
        html += String.format(
                "<h2>%s %s</h2>\n",
                EXPORT_HTML, title);

        if (errorLog == null){
            html += "SBML Validator failed.";
        } else {
            // create html for errorMap
            String validStr = (valid) ? VALID : INVALID;
            html += String.format("<h3 class=\"%s\">This document is %s SBML</h3>\n", validStr, validStr);
            // html += "<table id=\"table\" class=\"display\" width=\"100%\" cellspacing=\"0\">";
            html += "<table id=\"table\" class=\"table table-striped table-condensed table-hover\">";

            html += "\t<thead>\n" +
                    "\t\t<tr><th>metaid</th><th>severity</th><th>line</th><th>category</th><th>code</th><th>package</th>" +
                    "<th>excerpt</th><th>message</th></tr>\n" +
                    "\t</thead>" +
                    "\t<tbody>\n";
            for (String severity : errorMap.keySet()) {
                List<SBMLError> errors = errorMap.get(severity);
                for (SBMLError e : errors) {
                    String metaId = metaIdFromError(e);
                    String metaIdHtml;
                    if (metaId == null){
                        metaIdHtml = "<span class=\"fa fa-ban\" aria-hidden=\"true\" style=\"color:black\" title=\"No metaId in excerpt.\"></span>";
                    } else {
                        metaIdHtml = String.format("<a href=\"%s%s\"> <span class=\"fa fa-link\" aria-hidden=\"true\" style=\"color:black\" title=\"Link to node.\"></span> %s</a>",
                                BrowserHyperlinkListener.URL_SELECT_SBASE, metaId, metaId);
                    }
                    html += String.format(
                            "\t<tr><td>%s</td>\n" +
                            "\t\t<td><span class=\"error%s\">%s</span></td>\n" +
                            "\t\t<td><span class=\"errorLine\">%s</span></td>\n" +
                            "\t\t<td>%s</td>\n" +
                            "\t\t<td>%s</td>\n" +
                            "\t\t<td><span class=\"collection\">%s</span></td>\n" +
                            "\t\t<td><code>%s</code></td>\n" +
                            "\t\t<td><span class=\"errorMessage\" title=\"%s\">%s</span></td></tr>\n",
                            metaIdHtml,
                            e.getSeverity(), e.getSeverity(),
                            e.getLine(),
                            e.getCategory(),
                            e.getCode(),
                            e.getPackage(),
                            StringEscapeUtils.escapeHtml(e.getExcerpt()),
                            StringEscapeUtils.escapeHtml(e.getMessage()), StringEscapeUtils.escapeHtml(e.getShortMessage().getMessage())
                            );
                }
            }
            html += "\t</tbody>\n";
            html += SBaseHTMLFactory.TABLE_END;
        }

        html += SBaseHTMLFactory.HTML_STOP_TEMPLATE;
        return html;
    }

    /** Returns the metaid for the given error, or null if no
     * metaid in excerpt.
     *
     * @param e
     */
    private String metaIdFromError(SBMLError e){
        String excerpt = e.getExcerpt();
        Pattern pattern = Pattern.compile("metaid=\"(.*?)\"");
        Matcher matcher = pattern.matcher(excerpt);
        if (!matcher.find()){
            return null;
        } else {
            return matcher.group(1);
        }
    }


	/**
     * Create example validation HTML report.
	 */
	public static void main(String[] args) throws IOException {
        SBMLDocument doc = SBMLUtil.readSBMLDocument("/models/BIOMD0000000001.xml");
        System.out.println("validation");
        Validator validator = new Validator(doc);
        SBaseHTMLFactory.setBaseDir("file:///home/mkoenig/git/cy3sbml/src/main/resources/gui/");
        String html = validator.createHtml();
        FileUtils.writeStringToFile(new File("/home/mkoenig/tmp/validation.html"), html, Charsets.UTF_8);
    }

}

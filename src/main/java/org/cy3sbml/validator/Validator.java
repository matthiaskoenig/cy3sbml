package org.cy3sbml.validator;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.io.FileUtils;
import org.cy3sbml.gui.SBaseHTMLFactory;
import org.cy3sbml.util.IOUtil;
import org.cy3sbml.util.SBMLUtil;
import org.sbml.jsbml.*;
import org.sbml.jsbml.validator.SBMLValidator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validation of SBMLDocuments and creation of
 * validation reports including errors and warnings.
 *
 * This should run the validation and create the HTML
 * output of the report.
 * The validation results are stored and can be easily retrieved.
 * TODO: unittests
 * TODO: caching of validation
 * TODO: html report
 * TODO: implement new version based on JSBML validator
 */
public class Validator {
	private static final Logger logger = LoggerFactory.getLogger(Validator.class);
	
	public final static String SEVERITY_INFO = "Info";
	public final static String SEVERITY_WARNING = "Warning";
	public final static String SEVERITY_ERROR = "Error";
	public final static String SEVERITY_FATAL = "Fatal";
	public final static String SEVERITY_ALL = "All";
	
	private SBMLErrorLog errorLog;
	private Map<String, List<SBMLError> > errorMap;
	
	public Validator(SBMLDocument doc){
		try {
			errorLog = validateSBML(doc);
			errorMap = createErrorMapFromErrorLog(errorLog);
		} catch (Exception e) {
			logger.warn("SBMLDocument could not be validated.", e);
			e.printStackTrace();
			reset();
		}
	}
	
	private void reset(){
		errorLog = null;
		errorMap = null;
	}
	
	public SBMLErrorLog getErrorLog() {
		return errorLog;
	}
	
	public Map<String, List<SBMLError>> getErrorMap() {
		return errorMap;
	}


	/** Get all errors. */
	public List<SBMLError> getErrorList(){
		String[] keys = {
				SEVERITY_INFO,
				SEVERITY_WARNING,
				SEVERITY_ERROR,
				SEVERITY_FATAL,
				SEVERITY_ALL
		};
		return getErrorListForKeys(keys);

	}

	public List<SBMLError> getErrorListForKeys(String[] keys){
		List<SBMLError> eList = new LinkedList<SBMLError>();
		if (keys != null){
			for (String key: keys){
				if (errorMap.containsKey(key)){
					eList.addAll(errorMap.get(key));
				}
			}
		}
		return eList;
	}
	
	/* Validate the SBML and get the resulting ErrorLog */
	public static SBMLErrorLog validateSBML(SBMLDocument doc) throws SBMLException, XMLStreamException{
		SBMLErrorLog eLog = null;
		try {
			// create tmp file for validation
			final File tempFile = File.createTempFile("validation-tmp", ".xml");
			tempFile.deleteOnExit();
		    SBMLWriter.write(doc, tempFile, ' ' , (short) 2);
			eLog = SBMLValidator.checkConsistency(tempFile.getAbsolutePath(), new HashMap<String, String>());
		} catch (IOException e) {
			logger.error("Validation failed.", e);
			e.printStackTrace();
		}
		return eLog;
	}
	
	private static Map<String, List<SBMLError> > createErrorMapFromErrorLog(SBMLErrorLog errorLog){
		if (errorLog == null){
			return null;
		}
		Map<String, List<SBMLError> > errorMap = new HashMap<String, List<SBMLError> >();
		for (SBMLError error : errorLog.getValidationErrors()){
			addErrorToErrorMap(error, errorMap);
		}
		return errorMap;
	}

	private static void addErrorToErrorMap(SBMLError error, Map<String, List<SBMLError>> errorMap){
		// store under severity
		String key = error.getSeverity();		
		List<SBMLError> eList = getErrorListForKeyFromErrorMap(key, errorMap);
		eList.add(error);
		errorMap.put(key,eList);
		
		// store under all errors
		key = SEVERITY_ALL;
		eList = getErrorListForKeyFromErrorMap(key, errorMap);
		eList.add(error);
		errorMap.put(key,eList);
	}

	private static List<SBMLError> getErrorListForKeyFromErrorMap(String key, 
									Map<String, List<SBMLError>> emap){
		List<SBMLError> eList = null;
		if (emap.containsKey(key)){
			eList = emap.get(key);
		} else {
			eList = new LinkedList<SBMLError>();
		}
		return eList;
	}

	
	/* Get the ErrorLog in Table format. */
	public Object[][] getErrorTable(){
		if (errorLog == null){
			return null;
		}
		Object[][] errorData = new Object[5][2];
		errorData[0][0] = SEVERITY_INFO;
		errorData[1][0] = SEVERITY_WARNING;
		errorData[2][0] = SEVERITY_ERROR;
		errorData[3][0] = SEVERITY_FATAL;
		errorData[4][0] = SEVERITY_ALL;
		for (int k=0; k<5; ++k){
			errorData[k][1] = 0;
		}
		for (String key : errorMap.keySet()){
			for (int k=0; k<5; ++k){
				if (errorData[k][0].equals(key)){
					errorData[k][1] = errorMap.get(key).size();
					continue;
				}
			}
		}
		return errorData;
	}
	
	/* Create String information for the complete errorList */
	public String getErrorLogString(){
		if (errorLog == null)
			return "SBML could not be validated - no ErrorLog";
		List<SBMLError> eList = new LinkedList<SBMLError>();
		for (int k=0; k<errorLog.getErrorCount(); ++k){
			eList.add(errorLog.getError(k));
		}
		return createHTML(eList);
	}

    /**
     * Create HTML of validator output.
     * FIXME: set baseDir and title
     */
    public String createHTML(List<SBMLError> eList){
        String baseDir = "file:///home/mkoenig/git/cy3sbml/src/main/resources/gui/";
        String title = "title";

        String html = String.format(SBaseHTMLFactory.HTML_START_TEMPLATE, baseDir, title);
        html += String.format(
                "<h2>%s%s</h2>\n",
                SBaseHTMLFactory.EXPORT_HTML, "SBML Validation");
        int count = 1;
        html += SBaseHTMLFactory.TABLE_START;
        for (SBMLError e : eList){
            html += "\t<tr><td>\n";
            html += String.format("\t\t<span class=\"errorHead\">E%d %s (%s)</span><br />\n", count, e.getCategory(), e.getSeverity());
            html += String.format("\t\t<span class=\"errorShortMessage\">%s</span><br />\n", e.getShortMessage());
            html += String.format("\t\t<span class=\"errorLine\">Line: %s </span>\n", e.getLine());
            html += String.format("\t\t<span class=\"errorMessage\">%s</span>\n", e.getMessage());
            html += "\t</td></tr>\n";
            count++;
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
        Validator validator = new Validator(doc);
        String html = validator.createHTML(validator.getErrorList());
        FileUtils.writeStringToFile(new File("/home/mkoenig/tmp/validation.html"), html);
    }

}

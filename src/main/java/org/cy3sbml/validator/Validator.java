package org.cy3sbml.validator;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLError;
import org.sbml.jsbml.SBMLErrorLog;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.SBMLWriter;
import org.sbml.jsbml.validator.SBMLValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Class for handling the SBML validation warnings and errors. */
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
		return getErrorListString(eList);
	}
	
	/* Creates String representation of the error log. */ 
	public String getErrorListString(List<SBMLError> eList){
		String text = "";
		int count = 1;
		for (SBMLError e : eList){
			text += String.format("<h3>E%d %s (%s) </h3>", count, e.getCategory(), e.getSeverity());
			text += e.getShortMessage() + "<br>";
			text += String.format("<span color=\"red\">Line: %s </span>", e.getLine());
			text += String.format("<span color=\"blue\">%s</span>", e.getMessage());
			count++;
		}
		return text;
	}
}

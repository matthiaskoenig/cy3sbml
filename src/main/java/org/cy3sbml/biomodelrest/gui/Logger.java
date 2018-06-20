package org.cy3sbml.biomodelrest.gui;

import javafx.application.Platform;
import javafx.scene.control.TextArea;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/* 
 * Minimal logger for the GUI. 
 * Writes information in the given TextArea.
 */
@SuppressWarnings("restriction")
public class Logger {
	private TextArea log;

	public enum LogType {
		DEBUG, INFO, WARNING, ERROR
	}
	
	public Logger(TextArea log){
		this.log = log;
	}
	
	public static final String LOG_DEBUG = "[DEBUG]";
	public static final String LOG_INFO = "[INFO]";
	public static final String LOG_WARNING = "[WARNING]";
	public static final String LOG_ERROR = "[ERROR]";
	public static final DateFormat DATEFORMAT = new SimpleDateFormat("HH:mm:ss");
	
	
    private void logText(String text, LogType logType){
    	Platform.runLater(new Runnable() {
            @Override
            public void run() {
            	Calendar now = Calendar.getInstance();            	
            	String newText = "[" + DATEFORMAT.format(now.getTime()) + " " + logType.toString() + "] " + text;  
            	log.setText(newText + "\n" + log.getText());
            	System.out.println(newText);
            }
        });  	
    }
     
    public void error(String text){
    	logText(text, LogType.ERROR);
    }
    
    public void warn(String text){
    	logText(text, LogType.WARNING);
    }
    
    public void debug(String text){
    	logText(text, LogType.DEBUG);
    }
    
    public void info(String text){
    	logText(text, LogType.INFO);
    }

}

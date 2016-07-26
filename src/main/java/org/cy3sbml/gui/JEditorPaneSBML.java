package org.cy3sbml.gui;

import java.awt.Font;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JEditorPane;
import javax.swing.text.Document;
import javax.swing.SwingUtilities;

import org.cy3sbml.miriam.SBaseInfoThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JEditorPaneSBML extends JEditorPane implements SBMLPanel{
	private static final Logger logger = LoggerFactory.getLogger(JEditorPaneSBML.class);
	private static final long serialVersionUID = 1L;

	private long lastInformationThreadId = -1;
	
	public JEditorPaneSBML(){
		super();
		logger.debug("JEditorPaneSBML created");
		setEditable(false);
		setFont(new Font("Dialog", Font.PLAIN, 11));
		setContentType("text/html");
		setHelp();
	}
	
	public void setHelp(){
		setHTMLResource(GUIConstants.HTML_HELP_RESOURCE);
	}
	public void setExamples(){
		setHTMLResource(GUIConstants.HTML_EXAMPLE_RESOURCE);
	}

	/** Set given URL in the ResultsPanel. */
	private void setHTMLResource(String resource){	
		try {
			// here static HTML is set 
			URL url = new URL(ResultsPanel.class.getResource(resource).toString());
			// access to outer class methods
			JEditorPaneSBML.this.setPage(url);
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		}
	}
	
   /**
    * Set URL content in textPane. 
	* To force a document reload it is necessary to clear the
    * stream description property of the document.
    */
	public void setPage(URL page){
		// Necessary to use invokeLater to handle the Swing GUI update
		SwingUtilities.invokeLater(new Runnable(){
			@Override
			public void run() {
				Document doc = JEditorPaneSBML.this.getDocument();
			    doc.putProperty(Document.StreamDescriptionProperty, null);
			    // call the super of outer class
				try {
					JEditorPaneSBML.super.setPage(page);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}    
		});
	}
	
   /** Set text. */
   @Override
	public void setText(String text){
		// Necessary to use invokeLater to handle the Swing GUI update
		SwingUtilities.invokeLater(new Runnable(){
			@Override
			public void run() {
				JEditorPaneSBML.super.setText(text);
			} 
		});
	}

	/**
	 * Update Text in the navigation panel.
	 * Only updates information if the current thread is the last requested thread 
	 * for updating text. 
	 */
	@Override
    public void setText(SBaseInfoThread infoThread){
    	if (infoThread.getId() == lastInformationThreadId){
    		this.setText(infoThread.info);
    	}
    }
    
   /** 
    * Create information string for SBML Node and display. 
    */
   @Override
	public void showSBaseInfo(Object obj) {
	   Set<Object> objSet = new HashSet<Object>();
	   objSet.add(obj);
	   showSBaseInfo(objSet);
   }
   
    /**
    * Display information for set of nodes.
    */
    @Override
    public void showSBaseInfo(Set<Object> objSet) {
        this.setText("Retrieving information via WebServices ...");
        // starting threads for webservice calls
        SBaseInfoThread thread = new SBaseInfoThread(objSet, this);
        lastInformationThreadId = thread.getId();
        thread.start();
    }
}

package org.cy3sbml.gui;

import java.awt.Font;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JEditorPane;
import javax.swing.text.Document;

import org.cy3sbml.miriam.NamedSBaseInfoThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JEditorPaneSBML extends JEditorPane{
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
		URL url;
		try {
			// TODO: here the rendered template information has to be set, i.e. not static
			// HTML, but rendering HTML template with the given information
			// Use velocity or alternative for HTML rendering.
			// Update in combination with JavaFX component.
			url = new URL(ResultsPanel.class.getResource("/info.html").toString());
			this.setPage(url);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
   /**
    * Set URL content in textPane. 
	* To force a document reload it is necessary to clear the
    * stream description property of the document.
    */
	public void setPage(URL page) throws IOException {
		Document doc = this.getDocument();
	    doc.putProperty(Document.StreamDescriptionProperty, null);
		super.setPage(page);
	}
			
	/**
	 * Update Text in the navigation panel.
	 * Only updates information if the current thread is the last requested thread 
	 * for updating text. 
	 */
    public void updateText(NamedSBaseInfoThread infoThread){
    	if (infoThread.getId() == lastInformationThreadId){
    		this.setText(infoThread.info);
    	}
    }
    
   /** 
    * Create information string for SBML Node and display. 
    */ 
	public void showNSBInfo(Object obj) {
	   Set<Object> objSet = new HashSet<Object>();
	   objSet.add(obj);
	   showNSBInfo(objSet);
   }
   
  /**
   * Display information for set of nodes.
   */
   public void showNSBInfo(Set<Object> objSet) {
	   this.setText("Retrieving information via WebServices ...");
	   // starting threads for webservice calls
	   NamedSBaseInfoThread thread = new NamedSBaseInfoThread(objSet, this);
	   lastInformationThreadId = thread.getId();
	   thread.start();
   }
}

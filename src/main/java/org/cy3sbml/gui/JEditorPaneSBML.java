package org.cy3sbml.gui;

import java.awt.Font;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JEditorPane;
import javax.swing.text.Document;

import org.cy3sbml.BundleInformation;
import org.cy3sbml.miriam.NamedSBaseInfoThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JEditorPaneSBML extends JEditorPane{
	private static final Logger logger = LoggerFactory.getLogger(JEditorPaneSBML.class);
	private static final long serialVersionUID = 1L;

	
	private long lastInformationThreadId = -1;
	
	public JEditorPaneSBML(){
		super();
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
			url = new URL(ResultsPanel.class.getResource("/info.html").toString());
			this.setPage(url);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/** To force a document reload it is necessary to clear the
    * stream description property of the document.
    */
	public void setPage(URL page) throws IOException {
		Document doc = this.getDocument();
	    doc.putProperty(Document.StreamDescriptionProperty, null);
		super.setPage(page);
	}
			
	/** Update Text in the navigation panel.
	 * Only updates information if the current thread is the last requested thread 
	 * for updating text. */
    public void updateText(NamedSBaseInfoThread infoThread){
    	if (infoThread.getId() == lastInformationThreadId){
    		this.setText(infoThread.info);
    	}
    }
    
	/////////////////// MIRIAM INFORMATION /////////////////////////////

   /** Create the information string for the SBML Node and display in the textPane. */ 
	public void showNSBInfo(Object obj) {
	   Set<Object> objSet = new HashSet<Object>();
	   objSet.add(obj);
	   showNSBInfo(objSet);
   }
   
   public void showNSBInfo(Set<Object> objSet) {
	   NamedSBaseInfoThread thread = new NamedSBaseInfoThread(objSet, this);
	   lastInformationThreadId = thread.getId();
	   thread.start();
   }
    
}

package org.cy3sbml.biomodel;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import javax.swing.JOptionPane;

import org.apache.commons.io.IOUtils;
import org.cy3sbml.ServiceAdapter;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;

public class LoadBioModelTaskFactory implements TaskFactory{
    public static final String SUFFIX = ".xml"; // has to match the reader
	
	private ServiceAdapter adapter;
	private File file;
	
	
	// TODO: create taskIterator for list of ids
	public LoadBioModelTaskFactory(String id, ServiceAdapter adapter) {
		this.adapter = adapter;
		
		// TODO: reading SBML & creating the temp file should be in a separate task
		BioModelWSInterface bmInterface = new BioModelWSInterface(adapter.connectionProxy);
		InputStream instream = null;
		try {	
			String sbml = bmInterface.getBioModelSBMLById(id);	
			if (sbml == null || sbml.equals("") || sbml.startsWith(id)){
				JOptionPane.showMessageDialog(adapter.cySwingApplication.getJFrame(),
						String.format("<html>No SBML for BioModel Id : <b>%s</b></html>", id));
			}
			else{
				instream = new ByteArrayInputStream(sbml.getBytes("UTF-8"));
			}
			// convert to tmp file and use the core-task read Network from file task
			final File tempFile = File.createTempFile(id, SUFFIX);
			tempFile.deleteOnExit();
			
			// TODO: create file for storage in cy3sbml folder
			//adapter.cy3sbmlDirectory
			
			try (FileOutputStream out = new FileOutputStream(tempFile)) {
				IOUtils.copy(instream, out);
			}
			file = tempFile;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public TaskIterator createTaskIterator() {
		return adapter.loadNetworkFileTaskFactory.createTaskIterator(file);
	}

	@Override
	public boolean isReady() {
		return false;
	}

}

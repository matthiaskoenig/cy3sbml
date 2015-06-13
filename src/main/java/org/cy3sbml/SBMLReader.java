package org.cy3sbml;

/*
 * #%L
 * Cytoscape BioPAX Impl (biopax-impl)
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2006 - 2013 The Cytoscape Consortium
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 2.1 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import javax.swing.SwingUtilities;

import org.cy3sbml.mapping.NamedSBase2CyNodeMapping;
import org.cy3sbml.miriam.NamedSBaseInfoThread;
import org.cytoscape.io.CyFileFilter;
import org.cytoscape.io.read.AbstractInputStreamTaskFactory;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.events.NetworkViewAddedEvent;
import org.cytoscape.view.model.events.NetworkViewAddedListener;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.work.TaskIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SBMLReader extends AbstractInputStreamTaskFactory implements NetworkViewAddedListener {
	private static final Logger logger = LoggerFactory.getLogger(SBMLReader.class);
	private final ServiceAdapter cyServices;

	public SBMLReader(CyFileFilter filter, ServiceAdapter cyServices)
	{
		super(filter);
		this.cyServices = cyServices;
	}
	

	@Override
	public TaskIterator createTaskIterator(InputStream is, String inputName) {		
		logger.info("createTaskIterator: input stream name: " + inputName);
		try {
			return new TaskIterator(
				new SBMLReaderTask(copy(is), inputName, cyServices)
			);
		} catch (IOException e) {
			throw new SBMLReaderError(e.toString());
		}
	}


	@Override
	public void handleEvent(NetworkViewAddedEvent e) {
		logger.info("handle NetworkViewAddedEvent in SBMLReader");
		try {
			// always apply the style and layout to new BioPAX views;
			// i.e., not only for the first time when one's created.
			final CyNetworkView view = e.getNetworkView();
			final CyNetwork network = view.getModel();	
			if(isSBMLNetwork(network)) {	
				VisualStyle style = null;		
				
				/*
				// TODO: Store some kind infromation for the network & use to switch visual styles
				String kind = cyNetwork.getRow(cyNetwork).get(SBML.SBML_NETWORK, String.class);
				if ("DEFAULT".equals(kind))
					style = visualStyleUtil.getBioPaxVisualStyle();
				else if ("SIF".equals(kind))
					style = visualStyleUtil.getBinarySifVisualStyle();
				*/
				//apply style and layout			
				
				String styleName = (String) cyServices.cy3sbmlProperty("cy3sbml.visualStyle");
				style = getVisualStyleByName(styleName);
				logger.info("VisualStyle to set: " + style.getTitle());
				
				if(style != null) {
					final VisualStyle vs = style;			
					//apply style and layout			
					SwingUtilities.invokeLater(new Runnable() {
							public void run() {			
								layout(view);
								cyServices.visualMappingManager.setVisualStyle(vs, view);
								vs.apply(view);		
								view.updateView();
								logger.info("Style set and updated: " + vs.getTitle());
							}
					});
				}
			}
		
		} catch(Throwable t){
			t.printStackTrace();
		}		
	}
	
	private void layout(CyNetworkView view) {
		// do layout
		CyLayoutAlgorithm layout = cyServices.cyLayoutAlgorithmManager.getLayout("force-directed");
		if (layout == null) {
			layout = cyServices.cyLayoutAlgorithmManager.getLayout(CyLayoutAlgorithmManager.DEFAULT_LAYOUT_NAME);
			logger.warn("'force-directed' layout not found; will use the default one.");
		}
		cyServices.taskManager.execute(layout.createTaskIterator(view, 
				layout.getDefaultLayoutContext(), CyLayoutAlgorithm.ALL_NODE_VIEWS,""));
	}	
	
	private boolean isSBMLNetwork(CyNetwork cyNetwork) {
		//true if the attribute column exists
		CyTable cyTable = cyNetwork.getDefaultNetworkTable();
		return cyTable.getColumn(SBML.SBML_NETWORK) != null;
	}
	
	private VisualStyle getVisualStyleByName(String styleName){
		VisualMappingManager vmm = cyServices.visualMappingManager;
		Set<VisualStyle> styles = vmm.getAllVisualStyles();
		// another ugly fix because styles can not be get by name
		for (VisualStyle style: styles){
			if (style.getTitle().equals(styleName)){
				logger.info("style found in VisualStyles: " + styleName + " == " + style.getTitle());
				return style;
			}
		}
		logger.warn("style not found in VisualStyles, default style used.");
		return vmm.getDefaultVisualStyle();
	}
	
		
	private InputStream copy(InputStream is) throws IOException {
		ByteArrayOutputStream copy = new ByteArrayOutputStream();
		int chunk = 0;
		byte[] data = new byte[1024*1024];
		while((-1 != (chunk = is.read(data)))) {
			copy.write(data, 0, chunk);
		}
		is.close();
		return new ByteArrayInputStream( copy.toByteArray() );
	}	
	
	/* TODO: still necessary ? check
	private void selectSBMLTableAttributes(){
		String[] nAtts = {CySBMLConstants.ATT_TYPE,
						  CySBMLConstants.ATT_NAME,
						  CySBMLConstants.ATT_COMPARTMENT,
						  CySBMLConstants.ATT_METAID,
						  CySBMLConstants.ATT_SBOTERM};
		
		String[] eAtts = {Semantics.INTERACTION,
						  CySBMLConstants.ATT_STOICHIOMETRY,
						  CySBMLConstants.ATT_METAID,
						  CySBMLConstants.ATT_SBOTERM};
		AttributeUtils.selectTableAttributes(Arrays.asList(nAtts), Arrays.asList(eAtts));
	}
	*/

}

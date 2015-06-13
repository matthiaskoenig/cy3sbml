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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.cytoscape.io.read.CyNetworkReader;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.util.ListSingleSelection;
import org.sbml.jsbml.JSBML;
import org.sbml.jsbml.KineticLaw;
import org.sbml.jsbml.LocalParameter;
import org.sbml.jsbml.ModifierSpeciesReference;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.SpeciesReference;
import org.cy3sbml.gui.ResultsPanel;
import org.cy3sbml.mapping.NamedSBase2CyNodeMapping;
import org.cy3sbml.miriam.NamedSBaseInfoThread;
import org.cy3sbml.util.AttributeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * SBMLReaderTask
 * based on Cytoscape constructs of SBMLNetworkReader and BioPax reader.
 */
public class SBMLReaderTask extends AbstractTask implements CyNetworkReader {	
	private static final Logger logger = LoggerFactory.getLogger(SBMLReaderTask.class);
	
	private static final int BUFFER_SIZE = 16384;
	
	private String inputName;
	private final InputStream stream;
	private final ServiceAdapter adapter;
	private SBMLDocument document;
	private CyNetwork network;
	
	
	/*
	private static final String CREATE_NEW_COLLECTION = "A new network collection";
	private final HashMap<String, CyRootNetwork> nameToRootNetworkMap;
	
	private final Collection<CyNetwork> networks;
	private CyRootNetwork rootNetwork;	
	private CyNetworkReader anotherReader;
	*/

	/**
	 * SBML parsing/converting options.
	 */
	private static enum ReaderMode {
		/**
		 * Default SBML to Cytoscape network/view mapping: 
		 * species and reaction objects will be CyNodes interconnected by edges that 
		 * correspond to the listOf type of the species in the reactions. 
		 */
		DEFAULT("Default"),
		
		/** Layout SBML network */
		LAYOUT("Layout"),
		
		/** GRN SBML network */
		GRN("GRN");
		
		private final String name;

		private ReaderMode(String name) {
			this.name = name;
		}
		
		@Override
		public String toString() {
			return name;
		}
		
		static String[] names() {
			ReaderMode vals[] = ReaderMode.values();
			String names[] = new String[vals.length];
			for(int i= 0; i < vals.length; i++)
				names[i] = vals[i].toString();
			return names;
		}
	}

	
	public ListSingleSelection<ReaderMode> readerMode; 
	
	/*
	@ProvidesTitle()
	public String tunableDialogTitle() {
		return "SBML Reader Task";
	}
	
	@Tunable(description = "Model Mapping:", groups = {"Options"}, 
			tooltip="<html>Choose how to read BioPAX:" +
					"<ul>" +
					"<li><strong>Default</strong>: map states, interactions to nodes; properties - to edges, attributes;</li>"+
					"<li><strong>SIF</strong>: convert BioPAX to SIF, use a SIF reader, add attributes;</li>" +
					"<li><strong>SBGN</strong>: convert BioPAX to SBGN, find a SBGN reader, etc.</li>" +
					"</ul></html>"
			, gravity=500, xorChildren=true)
	public ListSingleSelection<ReaderMode> readerMode;
	
	@Tunable(description = "Network Collection:" , groups = {"Options","Default"}, tooltip="Choose a Network Collection", 
			dependsOn="readerMode=Default", 
			gravity=701, xorKey="Default")
	public ListSingleSelection<String> rootNetworkSelection;
	
	@Tunable(description = "Network View Renderer:", groups = {"Options","Default"}, gravity=702, xorKey="Default", dependsOn="readerMode=Default")
	public ListSingleSelection<NetworkViewRenderer> rendererList;

	//TODO select inference rules (multi-selection) for the SIF converter
	//TODO migrate from sif-converter to new biopax pattern module
	@Tunable(description = "Binary interactions to infer:" , groups = {"Options","SIF"}, tooltip="Select inference rules", 
			gravity=703, xorKey="SIF")
	public ListMultipleSelection<String> sifSelection;
	
	//TODO init SBGN options if required
	@Tunable(description = "SBGN Options:" , groups = {"Options","SBGN"}, tooltip="Currently not available", 
			gravity=704, xorKey="SBGN")
	public ListSingleSelection<String> sbgnSelection;
	*/
	


	/**
	 * Constructor
	 */ 
	public SBMLReaderTask(InputStream stream, String inputName, ServiceAdapter adapter) {
		this.stream = stream;
		this.adapter = adapter;
		this.inputName = inputName;
		readerMode = new ListSingleSelection<SBMLReaderTask.ReaderMode>(ReaderMode.values());
		readerMode.setSelectedValue(ReaderMode.DEFAULT);
	}
	
	
	@SuppressWarnings("deprecation")
	public void run(TaskMonitor taskMonitor) throws Exception {
		logger.info("Start Reader.run()");
		try {
		
			taskMonitor.setTitle("cy3sbml reader");
			taskMonitor.setProgress(0.0);
			if(cancelled) return;
				
				
			String version = JSBML.getJSBMLVersionString();
			logger.info("JSBML version: " + version);
			
			String xml = readString(stream);
			document = JSBML.readSBMLFromString(xml);
			network = adapter.cyNetworkFactory.createNetwork();
			
			AttributeUtil.set(network, network, SBML.SBML_NETWORK, "DEFAULT", String.class);
			
			
			org.sbml.jsbml.Model model = document.getModel();
			
			// Switch depending on the reader mode
			// Create multiple networks and handle analog to the biopax reader
			ReaderMode selectedMode = readerMode.getSelectedValue();
			switch (selectedMode) {
			case DEFAULT:
				logger.info("DEFAULT");
			case LAYOUT:
				logger.info("DEFAULT");
			case GRN:
				logger.info("DEFAULT");
			}
			
			// Create a node for each Species
			Map<String, CyNode> speciesById = new HashMap<String, CyNode>();
			for (Species species : model.getListOfSpecies()) {
				CyNode node = network.addNode();
				speciesById.put(species.getId(), node);
				CyRow attributes = network.getRow(node);
				
				// TODO: handle via AttributeUtil
				checkNodeSchema(attributes);
				
				attributes.set(SBML.NODE_NAME_ATTR_LABEL, species.getName());
				attributes.set(SBML.SBML_TYPE_ATTR, SBML.SBML_TYPE_SPECIES);
				attributes.set(SBML.SBML_ID_ATTR, species.getId());
	
				attributes.set(SBML.SBML_INITIAL_CONCENTRATION_ATTR, species.getInitialConcentration());
				attributes.set(SBML.SBML_INITIAL_AMOUNT_ATTR, species.getInitialAmount());
				attributes.set(SBML.SBML_CHARGE_ATTR, species.getCharge());
				
				String compartment = species.getCompartment();
				if (compartment != null) {
					attributes.set(SBML.SBML_COMPARTMENT_ATTR, compartment);
				}
			}
			taskMonitor.setProgress(0.5);
			
			// Create a node for each Reaction
			Map<String, CyNode> reactionsById = new HashMap<String, CyNode>();
			for (Reaction reaction : model.getListOfReactions()) {
				CyNode node = network.addNode();
				reactionsById.put(reaction.getId(), node);
				CyRow attributes = network.getRow(node);
				checkNodeSchema(attributes);
				String name = reaction.getName();
				if (name == null) {
					attributes.set(SBML.NODE_NAME_ATTR_LABEL, reaction.getId());
				} else {
					attributes.set(SBML.NODE_NAME_ATTR_LABEL, name);
				}
				attributes.set(SBML.SBML_TYPE_ATTR, SBML.SBML_TYPE_REACTION);
				attributes.set(SBML.SBML_ID_ATTR, reaction.getId());
				
				for (SpeciesReference product : reaction.getListOfProducts()) {
					CyNode sourceNode = speciesById.get(product.getSpecies());
					CyEdge edge = network.addEdge(sourceNode, node, true);
					CyRow edgeAttributes = network.getRow(edge);
					checkEdgeSchema(edgeAttributes);
					edgeAttributes.set(SBML.INTERACTION_TYPE_ATTR, SBML.INTERACTION_TYPE_REACTION_PRODUCT);
				}
				
				for (SpeciesReference reactant : reaction.getListOfReactants()) {
					CyNode sourceNode = speciesById.get(reactant.getSpecies());
					CyEdge edge = network.addEdge(sourceNode, node, true);
					CyRow edgeAttributes = network.getRow(edge);
					checkEdgeSchema(edgeAttributes);
					edgeAttributes.set(SBML.INTERACTION_TYPE_ATTR, SBML.INTERACTION_TYPE_REACTION_REACTANT);
				}
				
				for (ModifierSpeciesReference modifier : reaction.getListOfModifiers()) {
					CyNode sourceNode = speciesById.get(modifier.getSpecies());
					CyEdge edge = network.addEdge(sourceNode, node, true);
					CyRow edgeAttributes = network.getRow(edge);
					checkEdgeSchema(edgeAttributes);
					edgeAttributes.set(SBML.INTERACTION_TYPE_ATTR, SBML.INTERACTION_TYPE_REACTION_MODIFIER);
				}
				
				KineticLaw law = reaction.getKineticLaw();
				if (law != null) {
					for (LocalParameter parameter : law.getListOfParameters()) {
						String parameterName = parameter.getName();
						String key = String.format(SBML.KINETIC_LAW_ATTR_TEMPLATE, parameterName);
						checkSchema(attributes, key, Double.class);
						attributes.set(key, parameter.getValue());
						
						String units = parameter.getUnits();
						if (units != null) {
							String unitsKey = String.format(SBML.KINETIC_LAW_UNITS_ATTR_TEMPLATE, parameterName);
							checkSchema(attributes, unitsKey, String.class);
							attributes.set(unitsKey, units);
						}
					}
				}
			}
			taskMonitor.setProgress(1.0);
			logger.info("End Reader.run()");
		
		} catch (Throwable t){
			logger.error("Could not read SBML into Cytoscape!", t);
			t.printStackTrace();
			throw new SBMLReaderError("BioPAX reader failed to build a BioPAX model " +
					"(check the data for syntax errors) - " + t);
		}
	}
	


	@Override
	public CyNetwork[] getNetworks() {
		return new CyNetwork[] { network };
	}

	
	/* Looks, unless called directly, this runs once the view is created 
	 * for the first time, i.e., after the network is imported from a biopax file/stream 
	 * (so it's up to the user or another app. then to apply custom style/layout to 
	 * new view, should the first one is destroyed and new one created.
	 */
	@Override
	public CyNetworkView buildCyNetworkView(final CyNetwork network) {
		logger.info("buildCyNetworkView");
		CyNetworkView view;		
		
		// Set SBML in SBMLManager 
		SBMLManager sbmlManager = SBMLManager.getInstance();
		NamedSBase2CyNodeMapping mapping = NamedSBase2CyNodeMapping.fromSBMLNetwork(document, network);
		sbmlManager.addSBML2NetworkEntry(document, network, mapping);
		sbmlManager.updateCurrent(network);
		
		// Display the model information in the results pane
		ResultsPanel.getInstance().getTextPane().showNSBInfo(document.getModel());
		
		// Preload SBML WebService information
		NamedSBaseInfoThread.preloadAnnotationsForSBMLDocument(document);
		
		// create view depending on mode
		view = adapter.cyNetworkViewFactory.createNetworkView(network);
		
		/* TODO: handle different views
		ReaderMode currentMode = readerMode.getSelectedValue();
		switch (currentMode) {
			case DEFAULT:
				view = adapter.cyNetworkViewFactory.createNetworkView(network);
				break;
			default:
				view = adapter.cyNetworkViewFactory.createNetworkView(network);
				break;
		}
		*/
	
		logger.debug("network: " + network.toString());
		logger.debug("view: " + view.toString());
		
		if(!adapter.cyNetworkViewManager.getNetworkViews(network).contains(view)){
			adapter.cyNetworkViewManager.addNetworkView(view);
		}
		return view;
	}
	
	private void checkEdgeSchema(CyRow attributes) {
		checkSchema(attributes, SBML.INTERACTION_TYPE_ATTR, String.class);
	}

	private void checkNodeSchema(CyRow attributes) {
		checkSchema(attributes, SBML.SBML_TYPE_ATTR, String.class);
		checkSchema(attributes, SBML.SBML_ID_ATTR, String.class);
		checkSchema(attributes, SBML.SBML_INITIAL_CONCENTRATION_ATTR, Double.class);
		checkSchema(attributes, SBML.SBML_INITIAL_AMOUNT_ATTR, Double.class);
		checkSchema(attributes, SBML.SBML_CHARGE_ATTR, Integer.class);
		checkSchema(attributes, SBML.SBML_COMPARTMENT_ATTR, String.class);
	}

	private <T> void checkSchema(CyRow attributes, String attributeName, Class<T> type) {
		if (attributes.getTable().getColumn(attributeName) == null)
			attributes.getTable().createColumn(attributeName, type, false);
	}

	private static String readString(InputStream source) throws IOException {
		StringWriter writer = new StringWriter();
		BufferedReader reader = new BufferedReader(new InputStreamReader(source));
		try {
			char[] buffer = new char[BUFFER_SIZE];
			int charactersRead = reader.read(buffer, 0, buffer.length);
			while (charactersRead != -1) {
				writer.write(buffer, 0, charactersRead);
				charactersRead = reader.read(buffer, 0, buffer.length);
			}
		} finally {
			reader.close();
		}
		return writer.toString();
	}

	public void cancel() {
	}

}

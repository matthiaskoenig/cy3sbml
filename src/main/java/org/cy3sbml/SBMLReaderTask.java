package org.cy3sbml;


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
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.util.ListSingleSelection;
import org.sbml.jsbml.JSBML;
import org.sbml.jsbml.KineticLaw;
import org.sbml.jsbml.LocalParameter;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.ModifierSpeciesReference;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.SpeciesReference;
import org.sbml.jsbml.ext.qual.Input;
import org.sbml.jsbml.ext.qual.Output;
import org.sbml.jsbml.ext.qual.QualConstants;
import org.sbml.jsbml.ext.qual.QualModelPlugin;
import org.sbml.jsbml.ext.qual.QualitativeSpecies;
import org.sbml.jsbml.ext.qual.Transition;
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
			
			Model model = document.getModel();
			
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
			
			// TODO: Create the full graph with all information
			// TODO: Create the subgraph for the reaction species network
						
			// mark network as SBML network
			AttributeUtil.set(network, network, SBML.NETWORKTYPE_ATTR, "DEFAULT", String.class);
			
			// Network attributes
			AttributeUtil.set(network, network, SBML.ATTR_ID, model.getId(), String.class);
			if (model.isSetName()){
				AttributeUtil.set(network, network, SBML.ATTR_NAME, model.getName(), String.class);
			}
			if (model.isSetMetaId()){
				AttributeUtil.set(network, network, SBML.ATTR_METAID, model.getMetaId(), String.class);
			}
			if (model.isSetSBOTerm()){
				AttributeUtil.set(network, network, SBML.ATTR_SBOTERM, model.getSBOTermID(), String.class);
			}
			if (model.isSetConversionFactor()){
				AttributeUtil.set(network, network, SBML.ATTR_CONVERSION_FACTOR, model.getConversionFactor(), String.class);
			}
			if (model.isSetAreaUnits()){
				AttributeUtil.set(network, network, SBML.ATTR_AREA_UNITS, model.getAreaUnits(), String.class);
			}
			if (model.isSetExtentUnits()){
				AttributeUtil.set(network, network, SBML.ATTR_EXTENT_UNITS, model.getExtentUnits(), String.class);	
			}
			if (model.isSetLengthUnits()){
				AttributeUtil.set(network, network, SBML.ATTR_LENGTH_UNITS, model.getLengthUnits(), String.class);	
			}
			if (model.isSetSubstanceUnits()){
				AttributeUtil.set(network, network, SBML.ATTR_SUBSTANCE_UNITS, model.getSubstanceUnits(), String.class);
			}
			if (model.isSetTimeUnits()){
				AttributeUtil.set(network, network, SBML.ATTR_TIME_UNITS, model.getTimeUnits(), String.class);
			}
			if (model.isSetVolumeUnits()){
				AttributeUtil.set(network, network, SBML.ATTR_VOLUME_UNITS, model.getVolumeUnits(), String.class);	
			}
			

			// keep node lookup table by sbml id
			Map<String, CyNode> nodeById = new HashMap<String, CyNode>();
			
			// Create nodes for species
			for (Species species : model.getListOfSpecies()) {
				String id = species.getId();
				CyNode node = network.addNode();
				nodeById.put(species.getId(), node);
				AttributeUtil.set(network, node, SBML.ATTR_ID, species.getId(), String.class);
				AttributeUtil.set(network, node, SBML.ATTR_TYPE, SBML.NODETYPE_SPECIES, String.class);
				if (species.isSetName()){
					AttributeUtil.set(network, node, SBML.ATTR_NAME, species.getName(), String.class);
					AttributeUtil.set(network, node, SBML.LABEL, species.getName(), String.class);
				} else {
					AttributeUtil.set(network, node, SBML.LABEL, id, String.class);
				}
				if (species.isSetSBOTerm()){
					AttributeUtil.set(network, node, SBML.ATTR_SBOTERM, species.getSBOTermID(), String.class);
				}
				if (species.isSetMetaId()){
					AttributeUtil.set(network, node, SBML.ATTR_METAID, species.getMetaId(), String.class);
				}
				if (species.isSetCompartment()){
					AttributeUtil.set(network, node, SBML.ATTR_COMPARTMENT, species.getCompartment(), String.class);
				}
				if (species.isSetInitialConcentration()){
					AttributeUtil.set(network, node, SBML.ATTR_INITIAL_CONCENTRATION, species.getInitialConcentration(), Double.class);
				}
				if (species.isSetInitialAmount()){
					AttributeUtil.set(network, node, SBML.ATTR_INITIAL_AMOUNT, species.getInitialAmount(), Double.class);
				}
				if (species.isSetBoundaryCondition()){
					AttributeUtil.set(network, node, SBML.ATTR_BOUNDARY_CONDITION, species.getBoundaryCondition(), Boolean.class);
				}
				if (species.isSetConstant()){
					AttributeUtil.set(network, node, SBML.ATTR_CONSTANT, species.getConstant(), Boolean.class);
				}
				if (species.isSetHasOnlySubstanceUnits()){
					AttributeUtil.set(network, node, SBML.ATTR_HAS_ONLY_SUBSTANCE_UNITS, species.getHasOnlySubstanceUnits(), Boolean.class);
				}
				if (species.isSetCharge()){
					AttributeUtil.set(network, node, SBML.ATTR_CHARGE, species.getCharge(), Integer.class);
				}
				if (species.isSetConversionFactor()){
					AttributeUtil.set(network, node, SBML.ATTR_CONVERSION_FACTOR, species.getConversionFactor(), String.class);
				}
				if (species.isSetSubstanceUnits()){
					AttributeUtil.set(network, node, SBML.ATTR_SUBSTANCE_UNITS, species.getSubstanceUnits(), String.class);
				}
				if (species.isSetUnits()){
					AttributeUtil.set(network, node, SBML.ATTR_UNITS, species.getUnits(), String.class);
				}
				if (species.isSetValue()){
					AttributeUtil.set(network, node, SBML.ATTR_VALUE, species.getValue(), Double.class);
				}
				AttributeUtil.set(network, node, SBML.ATTR_DERIVED_UNITS, species.getDerivedUnits(), String.class);
			}
			taskMonitor.setProgress(0.5);
			
			// Create reaction nodes
			for (Reaction reaction : model.getListOfReactions()) {
				String id = reaction.getId();
				CyNode node = network.addNode();
				nodeById.put(id, node);
				AttributeUtil.set(network, node, SBML.ATTR_ID, id, String.class);
				AttributeUtil.set(network, node, SBML.ATTR_TYPE, SBML.NODETYPE_REACTION, String.class);
				if (reaction.isSetName()){
					AttributeUtil.set(network, node, SBML.ATTR_NAME, reaction.getName(), String.class);
					AttributeUtil.set(network, node, SBML.LABEL, reaction.getName(), String.class);
				} else {
					AttributeUtil.set(network, node, SBML.LABEL, id, String.class);
				}
				if (reaction.isSetSBOTerm()){
					AttributeUtil.set(network, node, SBML.ATTR_SBOTERM, reaction.getSBOTermID(), String.class);	
				}
				if (reaction.isSetMetaId()){
					AttributeUtil.set(network, node, SBML.ATTR_METAID, reaction.getMetaId(), String.class);
				}
				if (reaction.isSetCompartment()){
					AttributeUtil.set(network, node, SBML.ATTR_COMPARTMENT, reaction.getCompartment(), String.class);
				}
				// Reactions are reversible by default
				if (reaction.isSetReversible()){
					AttributeUtil.set(network, node, SBML.ATTR_REVERSIBLE, reaction.getReversible(), Boolean.class);
				} else {
					AttributeUtil.set(network, node, SBML.ATTR_REVERSIBLE, true, Boolean.class);
				}
				if (reaction.isSetFast()){
					AttributeUtil.set(network, node, SBML.ATTR_FAST, reaction.getFast(), Boolean.class);
				}
				if (reaction.isSetKineticLaw()){
					AttributeUtil.set(network, node, SBML.ATTR_KINETIC_LAW, reaction.getKineticLaw().getFormula(), String.class);	
				}
				AttributeUtil.set(network, node, SBML.ATTR_DERIVED_UNITS, reaction.getDerivedUnits(), String.class);
			
				// Backwards compatability of reader (anybody using this?)
				if (reaction.isSetKineticLaw()){
					KineticLaw law = reaction.getKineticLaw();
					if (law.isSetListOfLocalParameters()){
						for (LocalParameter parameter: law.getListOfLocalParameters()){
							if (parameter.isSetValue()){
								String key = String.format(SBML.KINETIC_LAW_ATTR_TEMPLATE, parameter.getId());
								AttributeUtil.set(network, node, key, parameter.getValue(), Double.class);
							}
							
							if (parameter.isSetUnits()){
								String unitsKey = String.format(SBML.KINETIC_LAW_UNITS_ATTR_TEMPLATE, parameter.getId());
								AttributeUtil.set(network, node, unitsKey, parameter.getUnits(), String.class);
							}
						}
					}
				}
				
				//reactants
				Double stoichiometry;
				for (SpeciesReference speciesRef : reaction.getListOfReactants()) {
					CyNode reactant = nodeById.get(speciesRef.getSpecies());
					CyEdge edge = network.addEdge(node, reactant, true);
					AttributeUtil.set(network, edge, SBML.INTERACTION_ATTR, SBML.INTERACTION_REACTION_REACTANT, String.class);
					
					if (speciesRef.isSetStoichiometry()){
						stoichiometry = speciesRef.getStoichiometry();
					} else {
						stoichiometry = 1.0;
					}
					AttributeUtil.set(network, edge, SBML.ATTR_STOICHIOMETRY, stoichiometry, Double.class);
					if (speciesRef.isSetSBOTerm()){
						AttributeUtil.set(network, edge, SBML.ATTR_SBOTERM, speciesRef.getSBOTermID(), String.class);
					}
					if (speciesRef.isSetMetaId()){
						AttributeUtil.set(network, edge, SBML.ATTR_METAID, speciesRef.getMetaId(), String.class);
					}
				}
				
				//products
				for (SpeciesReference speciesRef : reaction.getListOfProducts()) {
					CyNode product = nodeById.get(speciesRef.getSpecies());
					CyEdge edge = network.addEdge(node, product, true);
					AttributeUtil.set(network, edge, SBML.INTERACTION_ATTR, SBML.INTERACTION_REACTION_PRODUCT, String.class);
					
					if (speciesRef.isSetStoichiometry()){
						stoichiometry = speciesRef.getStoichiometry();
					} else {
						stoichiometry = 1.0;
					}
					AttributeUtil.set(network, edge, SBML.ATTR_STOICHIOMETRY, stoichiometry, Double.class);
					if (speciesRef.isSetSBOTerm()){
						AttributeUtil.set(network, edge, SBML.ATTR_SBOTERM, speciesRef.getSBOTermID(), String.class);
					}
					if (speciesRef.isSetMetaId()){
						AttributeUtil.set(network, edge, SBML.ATTR_METAID, speciesRef.getMetaId(), String.class);
					}
				}
				
				//modifier
				for (ModifierSpeciesReference msRef : reaction.getListOfModifiers()) {
					CyNode modifier = nodeById.get(msRef.getSpecies());
					CyEdge edge = network.addEdge(node, modifier, true);
					AttributeUtil.set(network, edge, SBML.INTERACTION_ATTR, SBML.INTERACTION_REACTION_MODIFIER, String.class);
					
					stoichiometry = 1.0;
					AttributeUtil.set(network, edge, SBML.ATTR_STOICHIOMETRY, stoichiometry, Double.class);
					if (msRef.isSetSBOTerm()){
						AttributeUtil.set(network, edge, SBML.ATTR_SBOTERM, msRef.getSBOTermID(), String.class);
					}
					if (msRef.isSetMetaId()){
						AttributeUtil.set(network, edge, SBML.ATTR_METAID, msRef.getMetaId(), String.class);
					}
				}
			}
				
			////////////// QUALITATIVE SBML MODEL ////////////////////////////////////////////
			 QualModelPlugin qModel = (QualModelPlugin) model.getExtension(QualConstants.namespaceURI);
			 
			 if (qModel != null){
				 logger.info("Reading qualitative model");
				 //QualSpecies 
				 for (QualitativeSpecies qSpecies : qModel.getListOfQualitativeSpecies()){	
					String qsid = qSpecies.getId(); 
				 	CyNode node = network.addNode();
				 	nodeById.put(qsid, node);
					AttributeUtil.set(network, node, SBML.ATTR_ID, qSpecies.getId(), String.class);
					AttributeUtil.set(network, node, SBML.ATTR_TYPE, SBML.NODETYPE_QUAL_SPECIES, String.class);
					if (qSpecies.isSetName()){
						AttributeUtil.set(network, node, SBML.ATTR_NAME, qSpecies.getName(), String.class);
						AttributeUtil.set(network, node, SBML.LABEL, qSpecies.getName(), String.class);
					} else {
						AttributeUtil.set(network, node, SBML.LABEL, qsid, String.class);
					}
					if (qSpecies.isSetSBOTerm()){
						AttributeUtil.set(network, node, SBML.ATTR_SBOTERM, qSpecies.getSBOTermID(), String.class);
					}
					if (qSpecies.isSetMetaId()){
						AttributeUtil.set(network, node, SBML.ATTR_METAID, qSpecies.getMetaId(), String.class);
					}
					if (qSpecies.isSetCompartment()){
						AttributeUtil.set(network, node, SBML.ATTR_COMPARTMENT, qSpecies.getCompartment(), String.class);
					}
					if (qSpecies.isSetInitialLevel()){
						AttributeUtil.set(network, node, SBML.ATTR_INITIAL_LEVEL, qSpecies.getInitialLevel(), Integer.class);	
					}
					if (qSpecies.isSetMaxLevel()){
						AttributeUtil.set(network, node, SBML.ATTR_MAX_LEVEL, qSpecies.getMaxLevel(), Integer.class);
					}				 
					if (qSpecies.isSetConstant()){
						AttributeUtil.set(network, node, SBML.ATTR_CONSTANT, qSpecies.getConstant(), Boolean.class);
					}
				}
			 }
			
			// QualTransitions
			for (Transition transition : qModel.getListOfTransitions()){
				String qtid = transition.getId();
				CyNode node = network.addNode();
			 	nodeById.put(qtid, node);
				AttributeUtil.set(network, node, SBML.ATTR_ID, qtid, String.class);
				AttributeUtil.set(network, node, SBML.ATTR_TYPE, SBML.NODETYPE_QUAL_TRANSITION, String.class);
				if (transition.isSetName()){
					AttributeUtil.set(network, node, SBML.ATTR_NAME, transition.getName(), String.class);
					AttributeUtil.set(network, node, SBML.LABEL, transition.getName(), String.class);
				} else {
					AttributeUtil.set(network, node, SBML.LABEL, qtid, String.class);
				}
				if (transition.isSetSBOTerm()){
					AttributeUtil.set(network, node, SBML.ATTR_SBOTERM, transition.getSBOTermID(), String.class);
				}
				if (transition.isSetMetaId()){
					AttributeUtil.set(network, node, SBML.ATTR_METAID, transition.getMetaId(), String.class);
				}
				
				//inputs
				for (Input input : transition.getListOfInputs()) {
					CyNode inNode = nodeById.get(input.getQualitativeSpecies());
					CyEdge edge = network.addEdge(node, inNode, true);
					AttributeUtil.set(network, edge, SBML.INTERACTION_ATTR, SBML.INTERACTION_TRANSITION_INPUT, String.class);
					AttributeUtil.set(network, edge, SBML.ATTR_STOICHIOMETRY, 1.0, Double.class);
					if (input.isSetSBOTerm()){
						AttributeUtil.set(network, edge, SBML.ATTR_SBOTERM, input.getSBOTermID(), String.class);
					}
					if (input.isSetMetaId()){
						AttributeUtil.set(network, edge, SBML.ATTR_METAID, input.getMetaId(), String.class);
					}
				}
					
				//outputs
				for (Output output : transition.getListOfOutputs()) {
					CyNode outNode = nodeById.get(output.getQualitativeSpecies());
					CyEdge edge = network.addEdge(node, outNode, true);
					AttributeUtil.set(network, edge, SBML.INTERACTION_ATTR, SBML.INTERACTION_TRANSITION_OUTPUT, String.class);
					AttributeUtil.set(network, edge, SBML.ATTR_STOICHIOMETRY, 1.0, Double.class);
					if (output.isSetSBOTerm()){
						AttributeUtil.set(network, edge, SBML.ATTR_SBOTERM, output.getSBOTermID(), String.class);
					}
					if (output.isSetMetaId()){
						AttributeUtil.set(network, edge, SBML.ATTR_METAID, output.getMetaId(), String.class);
					}	
				}
			}
			 

			taskMonitor.setProgress(1.0);
			logger.info("End Reader.run()");
		
		} catch (Throwable t){
			logger.error("Could not read SBML into Cytoscape!", t);
			t.printStackTrace();
			throw new SBMLReaderError("cy3sbml reader failed to build a SBML model " +
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

	/*
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
	*/

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

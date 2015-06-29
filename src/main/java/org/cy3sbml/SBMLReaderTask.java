package org.cy3sbml;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.Collection;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.cytoscape.io.read.CyNetworkReader;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.sbml.jsbml.ASTNode;
import org.sbml.jsbml.Annotation;
import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.InitialAssignment;
import org.sbml.jsbml.JSBML;
import org.sbml.jsbml.KineticLaw;
import org.sbml.jsbml.LocalParameter;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.ModifierSpeciesReference;
import org.sbml.jsbml.NamedSBase;
import org.sbml.jsbml.Parameter;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.Rule;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.SpeciesReference;
import org.sbml.jsbml.UnitDefinition;
import org.sbml.jsbml.ext.comp.CompConstants;
import org.sbml.jsbml.ext.comp.CompModelPlugin;
import org.sbml.jsbml.ext.fbc.And;
import org.sbml.jsbml.ext.fbc.Association;
import org.sbml.jsbml.ext.fbc.FBCConstants;
import org.sbml.jsbml.ext.fbc.FBCModelPlugin;
import org.sbml.jsbml.ext.fbc.FBCReactionPlugin;
import org.sbml.jsbml.ext.fbc.FBCSpeciesPlugin;
import org.sbml.jsbml.ext.fbc.FluxBound;
import org.sbml.jsbml.ext.fbc.FluxBound.Operation;
import org.sbml.jsbml.ext.fbc.FluxObjective;
import org.sbml.jsbml.ext.fbc.GeneProduct;
import org.sbml.jsbml.ext.fbc.GeneProductRef;
import org.sbml.jsbml.ext.fbc.GeneProteinAssociation;
import org.sbml.jsbml.ext.fbc.Objective;
import org.sbml.jsbml.ext.fbc.Or;
import org.sbml.jsbml.ext.layout.Layout;
import org.sbml.jsbml.ext.layout.LayoutConstants;
import org.sbml.jsbml.ext.layout.LayoutModelPlugin;
import org.sbml.jsbml.ext.layout.SpeciesGlyph;
import org.sbml.jsbml.ext.qual.FunctionTerm;
import org.sbml.jsbml.ext.qual.Input;
import org.sbml.jsbml.ext.qual.Output;
import org.sbml.jsbml.ext.qual.QualConstants;
import org.sbml.jsbml.ext.qual.QualModelPlugin;
import org.sbml.jsbml.ext.qual.QualitativeSpecies;
import org.sbml.jsbml.ext.qual.Transition;
import org.sbml.jsbml.xml.XMLNode;
import org.cy3sbml.gui.ResultsPanel;
import org.cy3sbml.layout.LayoutPreprocessor;
import org.cy3sbml.mapping.NamedSBase2CyNodeMapping;
import org.cy3sbml.mapping.One2ManyMapping;
import org.cy3sbml.miriam.NamedSBaseInfoThread;
import org.cy3sbml.util.AttributeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SBMLReaderTask
 * parts based on SBMLNetworkReader core-impl and BioPax reader.
 * 
 * The reader creates the master SBML network graph with various subnetworks created from 
 * the full graph.
 */
public class SBMLReaderTask extends AbstractTask implements CyNetworkReader {	
	private static final Logger logger = LoggerFactory.getLogger(SBMLReaderTask.class);
	
	private static final String CREATE_NEW_COLLECTION = "A new network collection";
	private static final int BUFFER_SIZE = 16384;
	
	private String inputName;
	private final InputStream stream;
	private final ServiceAdapter adapter;
	private SBMLDocument document;
	
	private CyNetwork network;       // global network of all SBML information
	private CyNetwork coreNetwork;   // core reaction, species, (qualSpecies, qualTransitions), fbc network
	
	private CyRootNetwork rootNetwork;
	private Map<String, CyNode> nodeById; // node dictionary
	private final Collection<CyNetwork> networks;
	
	/** Constructor */ 
	public SBMLReaderTask(InputStream stream, String inputName, ServiceAdapter adapter) {
		
		this.stream = stream;
		this.adapter = adapter;
		this.inputName = inputName;
		
		nodeById = new HashMap<String, CyNode>();
		networks = new HashSet<CyNetwork>();		
	}
	
	
	// TODO: Create the full graph with all information
	// TODO: Create the subgraph for the reaction species network
	/**
	 * Parse the SBML networks.
	 */
	public void run(TaskMonitor taskMonitor) throws Exception {
	
		logger.info("---------------------------------");
		logger.info("Start Reader.run()");
		logger.info("---------------------------------");
		try {
			taskMonitor.setTitle("cy3sbml reader");
			taskMonitor.setProgress(0.0);
			if(cancelled){
				return;
			}
			
			// Read model
			logger.debug("JSBML version: " + JSBML.getJSBMLVersionString());
			String xml = readString(stream);
			
			// TODO: get and display all the reader warnings
			document = JSBML.readSBMLFromString(xml);
			Model model = document.getModel();
		
			// create an empty root network
			network = adapter.cyNetworkFactory.createNetwork();
			
			// To create a new CySubNetwork with the same CyNetwork's CyRootNetwork, cast your CyNetwork to
			// CySubNetwork and call the CySubNetwork.getRootNetwork() method:
			// 		CyRootNetwork rootNetwork = ((CySubNetwork)network).getRootNetwork(); 
			// CyRootNetwork also provides methods to create and add new subnetworks (see CyRootNetwork.addSubNetwork()). 
			rootNetwork = ((CySubNetwork) network).getRootNetwork();
						
			// Core model
			readCore(model);
			taskMonitor.setProgress(0.5);
				
			QualModelPlugin qualModel = (QualModelPlugin) model.getExtension(QualConstants.namespaceURI); 
			if (qualModel != null){
				readQual(model, qualModel);
			}
			
			FBCModelPlugin fbcModel = (FBCModelPlugin) model.getExtension(FBCConstants.namespaceURI);
			if (fbcModel != null){
				readFBC(model, fbcModel);
			}
			
			CompModelPlugin compModel = (CompModelPlugin) model.getExtension(CompConstants.namespaceURI);
			if (compModel != null){
				logger.info("comp model found, but not yet supported");
			}
			
			LayoutModelPlugin layoutModel = (LayoutModelPlugin) model.getExtension(LayoutConstants.namespaceURI);
			if (layoutModel != null){
				logger.info("layout model found, but not yet supported");
				//readLayouts(model, qualModel, layoutModel);	
			}

			// main SBML network consisting of the following nodes and edges
			String[] nodeTypes = {
				SBML.NODETYPE_SPECIES,
				SBML.NODETYPE_REACTION,
				SBML.NODETYPE_QUAL_SPECIES,
				SBML.NODETYPE_QUAL_TRANSITION,
				SBML.NODETYPE_FBC_GENEPRODUCT,
				SBML.NODETYPE_FBC_AND,
				SBML.NODETYPE_FBC_OR
			}; 
			String[] edgeTypes = {
				SBML.INTERACTION_REACTION_REACTANT,
				SBML.INTERACTION_REACTION_PRODUCT,
				SBML.INTERACTION_REACTION_MODIFIER,
				SBML.INTERACTION_QUAL_TRANSITION_INPUT,
				SBML.INTERACTION_QUAL_TRANSITION_OUTPUT,
				SBML.INTERACTION_FBC_GENEPRODUCT_SPECIES,
				SBML.INTERACTION_FBC_ASSOCIATION_ASSOCIATION,
				SBML.INTERACTION_FBC_ASSOCIATION_REACTION
			};
			// O(1) lookup
			HashSet<String> nodeTypesSet = new HashSet<String>(java.util.Arrays.asList(nodeTypes));
			HashSet<String> edgeTypesSet = new HashSet<String>(java.util.Arrays.asList(edgeTypes));
	
			// collect nodes and edges
			HashSet<CyNode> nodes = new HashSet<CyNode>();
			for (CyNode n : network.getNodeList()){
				// check the type
				CyRow row = network.getRow(n, CyNetwork.DEFAULT_ATTRS);
				String type = row.get(SBML.ATTR_TYPE, String.class);
				if (nodeTypesSet.contains(type)){
					nodes.add(n);	
				}
			}
	
			HashSet<CyEdge> edges = new HashSet<CyEdge>();
			for (CyEdge e : network.getEdgeList()){
				// check the type
				CyRow row = network.getRow(e, CyNetwork.DEFAULT_ATTRS);
				String type = row.get(SBML.INTERACTION_ATTR, String.class);
				if (edgeTypesSet.contains(type)){
					edges.add(e);	
				}
			}
			coreNetwork = rootNetwork.addSubNetwork(nodes, edges);
			// set name of network
			String name = network.getRow(network).get(CyNetwork.NAME, String.class);
			coreNetwork.getRow(coreNetwork).set(CyNetwork.NAME, "Core: "+ name);
			 
			// [2] layout subnetworks
			// TODO: create layout subnetworks (different mechanism necessary, 
			//   probably via direct subnetwork generation)
			
			taskMonitor.setProgress(1.0);
			logger.info("---------------------------------");
			logger.info("End Reader.run()");
			logger.info("---------------------------------");
		
		} catch (Throwable t){
			logger.error("Could not read SBML into Cytoscape!", t);
			t.printStackTrace();
			throw new SBMLReaderError("cy3sbml reader failed to build a SBML model " +
					"(check the data for syntax errors) - " + t);
		}
	}
	
	
	private CyNode createNamedSBaseNode(NamedSBase sbase, String type){
		String id = sbase.getId();
		// create node and add to network
	 	CyNode n = network.addNode();
	 	nodeById.put(id, n);
	 	// set the attributes
		AttributeUtil.set(network, n, SBML.ATTR_ID, id, String.class);
		AttributeUtil.set(network, n, SBML.ATTR_TYPE, type, String.class);
		if (sbase.isSetName()){
			AttributeUtil.set(network, n, SBML.ATTR_NAME, sbase.getName(), String.class);
			AttributeUtil.set(network, n, SBML.LABEL, sbase.getName(), String.class);
		} else {
			AttributeUtil.set(network, n, SBML.LABEL, id, String.class);
		}
		if (sbase.isSetSBOTerm()){
			AttributeUtil.set(network, n, SBML.ATTR_SBOTERM, sbase.getSBOTermID(), String.class);
		}
		if (sbase.isSetMetaId()){
			AttributeUtil.set(network, n, SBML.ATTR_METAID, sbase.getMetaId(), String.class);
		}
		return n;
	}
	
	/** Probably not all information parsed.
	 * TODO: implement general function for copying node attributes */
	private void copyNodeAttributes(CyNode sourceNode, CyNode targetNode){
		logger.warn("copyNodeAttributes NOT IMPLEMENTED");
		/*	
		String sId = sourceNode.getIdentifier();
		String tId = targetNode.getIdentifier();
		String info = null;
		info = (String) nodeAttributes.getAttribute(sId, CySBMLConstants.ATT_ID);
		nodeAttributes.setAttribute(tId, CySBMLConstants.ATT_ID, info);
		
		info = (String) nodeAttributes.getAttribute(sId, CySBMLConstants.ATT_TYPE);
		nodeAttributes.setAttribute(tId, CySBMLConstants.ATT_TYPE, info);
		
		info = (String) nodeAttributes.getAttribute(sId, CySBMLConstants.ATT_NAME);
		if (info != null){
			nodeAttributes.setAttribute(tId, CySBMLConstants.ATT_NAME, info);
		}
		info = (String) nodeAttributes.getAttribute(sId, CySBMLConstants.ATT_COMPARTMENT);
		if (info != null){
			nodeAttributes.setAttribute(tId, CySBMLConstants.ATT_COMPARTMENT, info);
		}
		info = (String) nodeAttributes.getAttribute(sId, CySBMLConstants.ATT_SBOTERM);
		if (info != null){
			nodeAttributes.setAttribute(tId, CySBMLConstants.ATT_SBOTERM, info);
		}
		*/
	}
	
	
	// --- CORE -----------------------------------------------------------------------------------
	
	/**
	 * Create nodes, edges and attributes from Core Model.
	 * @param model
	 */
	private void readCore(Model model){
		logger.info("** core **");
		// Mark network as SBML
		AttributeUtil.set(network, network, SBML.NETWORKTYPE_ATTR, SBML.NETWORKTYPE_SBML, String.class);
		AttributeUtil.set(network, network, SBML.LEVEL_VERSION, String.format("L%1$s V%2$s", document.getLevel(), document.getVersion()), String.class);
		
		// Network attributes
		if (model.isSetId()){
			AttributeUtil.set(network, network, SBML.ATTR_ID, model.getId(), String.class);	
		}
		if (model.isSetName()){
			AttributeUtil.set(network, network, SBML.ATTR_NAME, model.getName(), String.class);
		}
		if (model.isSetMetaId()){
			AttributeUtil.set(network, network, SBML.ATTR_METAID, model.getMetaId(), String.class);
		}
		if (model.isSetSBOTerm()){
			AttributeUtil.set(network, network, SBML.ATTR_SBOTERM, model.getSBOTermID(), String.class);
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
		if (model.isSetAreaUnits()){
			AttributeUtil.set(network, network, SBML.ATTR_AREA_UNITS, model.getAreaUnits(), String.class);
		}
		if (model.isSetLengthUnits()){
			AttributeUtil.set(network, network, SBML.ATTR_LENGTH_UNITS, model.getLengthUnits(), String.class);	
		}
		if (model.isSetExtentUnits()){
			AttributeUtil.set(network, network, SBML.ATTR_EXTENT_UNITS, model.getExtentUnits(), String.class);	
		}
		if (model.isSetConversionFactor()){
			AttributeUtil.set(network, network, SBML.ATTR_CONVERSION_FACTOR, model.getConversionFactor(), String.class);
		}
		
		// FunctionDefinitions (not parsed)
		// for (FunctionDefinition fdef : model.getListOfFunctionDefinitions()){}
		
		// UnitDefinitions (not parsed)
		// for (UnitDefinition udef : model.getListOfUnitDefinitions()){}
		
		// Create nodes for compartments
		for (Compartment compartment : model.getListOfCompartments()) {
			CyNode node = createNamedSBaseNode(compartment, SBML.NODETYPE_COMPARTMENT);

			if (compartment.isSetSpatialDimensions()){
				AttributeUtil.set(network, node, SBML.ATTR_SPATIAL_DIMENSIONS, compartment.getSpatialDimensions(), Double.class);
			}
			if (compartment.isSetSize()){
				AttributeUtil.set(network, node, SBML.ATTR_SPATIAL_DIMENSIONS, compartment.getSize(), Double.class);
			}
			if (compartment.isSetUnits()){
				AttributeUtil.set(network, node, SBML.ATTR_UNITS, compartment.getUnits(), String.class);
			}
			if (compartment.isSetConstant()){
				AttributeUtil.set(network, node, SBML.ATTR_CONSTANT, compartment.getConstant(), Boolean.class);
			}
		}
		
		// Create nodes for parameters
		for (Parameter parameter : model.getListOfParameters()) {
			CyNode node = createNamedSBaseNode(parameter, SBML.NODETYPE_PARAMETER);
			if (parameter.isSetValue()){
				AttributeUtil.set(network, node, SBML.ATTR_INITIAL_AMOUNT, parameter.getValue(), Double.class);
			}
			if (parameter.isSetUnits()){
				AttributeUtil.set(network, node, SBML.ATTR_UNITS, parameter.getUnits(), String.class);
			}
			if (parameter.isSetConstant()){
				AttributeUtil.set(network, node, SBML.ATTR_CONSTANT, parameter.getConstant(), Boolean.class);
			}
			// a UnitDefinition that represent the derived unit of this quantity, or null if it is not possible to derive a unit.
			// If neither the Species object's 'substanceUnits' attribute nor the enclosing Model object's 'substanceUnits' attribute are set, 
			// then the unit of that species' quantity is undefined.
			UnitDefinition udef = parameter.getDerivedUnitDefinition();
			if (udef != null){
				AttributeUtil.set(network, node, SBML.ATTR_DERIVED_UNITS, udef.toString(), String.class);
			}
		}
		
		// Create nodes for species
		for (Species species : model.getListOfSpecies()) {
			CyNode node = createNamedSBaseNode(species, SBML.NODETYPE_SPECIES);
			if (species.isSetCompartment()){
				AttributeUtil.set(network, node, SBML.ATTR_COMPARTMENT, species.getCompartment(), String.class);
				// add edge to compartment
				CyNode comp = nodeById.get(species.getCompartment());
				CyEdge edge = network.addEdge(node, comp, true);
				AttributeUtil.set(network, edge, SBML.INTERACTION_ATTR, SBML.INTERACTION_SPECIES_COMPARTMENT, String.class);
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
			if (species.isSetInitialAmount()){
				AttributeUtil.set(network, node, SBML.ATTR_INITIAL_AMOUNT, species.getInitialAmount(), Double.class);
			}
			if (species.isSetInitialConcentration()){
				AttributeUtil.set(network, node, SBML.ATTR_INITIAL_CONCENTRATION, species.getInitialConcentration(), Double.class);
			}
			if (species.isSetUnits()){
				AttributeUtil.set(network, node, SBML.ATTR_UNITS, species.getUnits(), String.class);
			}
			if (species.isSetValue()){
				AttributeUtil.set(network, node, SBML.ATTR_VALUE, species.getValue(), Double.class);
			}
			// a UnitDefinition that represent the derived unit of this quantity, or null if it is not possible to derive a unit.
			// If neither the Species object's 'substanceUnits' attribute nor the enclosing Model object's 'substanceUnits' attribute are set, 
			// then the unit of that species' quantity is undefined.
			UnitDefinition udef = species.getDerivedUnitDefinition();
			if (udef != null){
				AttributeUtil.set(network, node, SBML.ATTR_DERIVED_UNITS, udef.toString(), String.class);
			}
		}
		
		// Create reaction nodes
		for (Reaction reaction : model.getListOfReactions()) {
			CyNode node = createNamedSBaseNode(reaction, SBML.NODETYPE_REACTION);
	
			if (reaction.isSetCompartment()){
				AttributeUtil.set(network, node, SBML.ATTR_COMPARTMENT, reaction.getCompartment(), String.class);
				// add edge to compartment
				CyNode comp = nodeById.get(reaction.getCompartment());
				CyEdge edge = network.addEdge(node, comp, true);
				AttributeUtil.set(network, edge, SBML.INTERACTION_ATTR, SBML.INTERACTION_REACTION_COMPARTMENT, String.class);
			}
			// Reactions set reversible by default
			if (reaction.isSetReversible()){
				AttributeUtil.set(network, node, SBML.ATTR_REVERSIBLE, reaction.getReversible(), Boolean.class);
			} else {
				AttributeUtil.set(network, node, SBML.ATTR_REVERSIBLE, true, Boolean.class);
			}
			if (reaction.isSetFast()){
				AttributeUtil.set(network, node, SBML.ATTR_FAST, reaction.getFast(), Boolean.class);
			}
			if (reaction.isSetKineticLaw()){
				AttributeUtil.set(network, node, SBML.ATTR_KINETIC_LAW, reaction.getKineticLaw().getMath().toFormula(), String.class);	
			}
			UnitDefinition udef = reaction.getDerivedUnitDefinition();
			// Returns:  a UnitDefinition that represent the derived unit of this quantity, or null if it is not possible to derive a unit.
			// If neither the Species object's 'substanceUnits' attribute nor the enclosing Model object's 'substanceUnits' attribute are set, 
			// then the unit of that species' quantity is undefined.
			if (udef != null){
				AttributeUtil.set(network, node, SBML.ATTR_DERIVED_UNITS, udef.toString(), String.class);
			}
		
			if (reaction.isSetKineticLaw()){
				KineticLaw law = reaction.getKineticLaw();
				
				/*
				// Create a kinetic law node
				String id = reaction.getId() + "_law";
			 	CyNode lawNode = network.addNode();
			 	nodeById.put(id, lawNode);
				AttributeUtil.set(network, lawNode, SBML.ATTR_ID, id, String.class);
				AttributeUtil.set(network, lawNode, SBML.ATTR_TYPE, SBML.NODETYPE_KINETIC_LAW, String.class);
				// connect to reaction
				CyEdge edge = network.addEdge(node, lawNode, true);
				AttributeUtil.set(network, edge, SBML.INTERACTION_ATTR, SBML.INTERACTION_REACTION_KINETICLAW, String.class);
				*/
				
				// global parameters
				if (law.isSetMath()){
					ASTNode astNode = law.getMath();
					List<Parameter> parameters = astNode.findReferencedGlobalParameters();
					// make unique (often parameter multiple times in equation
					for (Parameter parameter : new HashSet<Parameter>(parameters)){
						// add edge
						CyNode parameterNode = nodeById.get(parameter.getId());
						CyEdge edge = network.addEdge(parameterNode, node, true);
						AttributeUtil.set(network, edge, SBML.INTERACTION_ATTR, SBML.INTERACTION_PARAMETER_REACTION, String.class);	
					}
				}
				// local parameters
				// Backwards compatibility of reader (anybody using this?)
				// TODO: create LocalParameter nodes for parameters (and add edge)
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
			// Reactants
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
			// Products
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
			// Modifiers
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
		
		// InitialAssignments (not parsed)
		// for (InitialAssignment assignment : model.getListOfInitialAssignments()) {}
		
		// Rules (not parsed)
		//for (Rule rule : model.getListOfRules()){}
				
		// Constraints (not parsed)
		// for (Constraint constraint : model.getListOfConstraints()){}
		
		// Events (not parsed)
		// for (Event event : model.getListOfEvents()){}
	}
	

	/**
	 * Create nodes, edges and attributes from Qualitative Model.
	 * @param qModel
	 */
	private void readQual(Model model, QualModelPlugin qModel){
		logger.info("** qual **");
		 // QualSpecies 
		 for (QualitativeSpecies qSpecies : qModel.getListOfQualitativeSpecies()){	
			CyNode node = createNamedSBaseNode(qSpecies, SBML.NODETYPE_QUAL_SPECIES);
			if (qSpecies.isSetCompartment()){
				AttributeUtil.set(network, node, SBML.ATTR_COMPARTMENT, qSpecies.getCompartment(), String.class);
				// add edge to compartment
				CyNode comp = nodeById.get(qSpecies.getCompartment());
				CyEdge edge = network.addEdge(node, comp, true);
				AttributeUtil.set(network, edge, SBML.INTERACTION_ATTR, SBML.INTERACTION_SPECIES_COMPARTMENT, String.class);
			}
			if (qSpecies.isSetConstant()){
				AttributeUtil.set(network, node, SBML.ATTR_CONSTANT, qSpecies.getConstant(), Boolean.class);
			}
			if (qSpecies.isSetInitialLevel()){
				AttributeUtil.set(network, node, SBML.ATTR_QUAL_INITIAL_LEVEL, qSpecies.getInitialLevel(), Integer.class);	
			}
			if (qSpecies.isSetMaxLevel()){
				AttributeUtil.set(network, node, SBML.ATTR_QUAL_MAX_LEVEL, qSpecies.getMaxLevel(), Integer.class);
			}				 
		}
		// QualTransitions
		for (Transition transition : qModel.getListOfTransitions()){
			CyNode node = createNamedSBaseNode(transition, SBML.NODETYPE_QUAL_TRANSITION);
			// Inputs
			for (Input input : transition.getListOfInputs()) {
				CyNode inNode = nodeById.get(input.getQualitativeSpecies());
				CyEdge edge = network.addEdge(node, inNode, true);
				AttributeUtil.set(network, edge, SBML.INTERACTION_ATTR, SBML.INTERACTION_QUAL_TRANSITION_INPUT, String.class);

				// required (no checking of required -> NullPointerException risk)
				AttributeUtil.set(network, edge, SBML.ATTR_QUAL_TRANSITION_EFFECT, input.getTransitionEffect().toString(), String.class);
				AttributeUtil.set(network, edge, SBML.ATTR_QUAL_QUALITATIVE_SPECIES, input.getQualitativeSpecies().toString(), String.class);
				// optional
				if (input.isSetId()){
					AttributeUtil.set(network, edge, SBML.ATTR_ID, input.getId(), String.class);
				}
				if (input.isSetName()){
					AttributeUtil.set(network, edge, SBML.ATTR_NAME, input.getName(), String.class);
				}
				if (input.isSetSign()){
					AttributeUtil.set(network, edge, SBML.ATTR_QUAL_SIGN, input.getSign().toString(), String.class);
				}
				if (input.isSetSBOTerm()){
					AttributeUtil.set(network, edge, SBML.ATTR_SBOTERM, input.getSBOTermID(), String.class);
				}
				if (input.isSetMetaId()){
					AttributeUtil.set(network, edge, SBML.ATTR_METAID, input.getMetaId(), String.class);
				}
				if (input.isSetThresholdLevel()){
					AttributeUtil.set(network, edge, SBML.ATTR_QUAL_THRESHOLD_LEVEL, input.getThresholdLevel(), Integer.class);
				}
			}
			// Outputs
			for (Output output : transition.getListOfOutputs()) {
				CyNode outNode = nodeById.get(output.getQualitativeSpecies());
				CyEdge edge = network.addEdge(node, outNode, true);
				AttributeUtil.set(network, edge, SBML.INTERACTION_ATTR, SBML.INTERACTION_QUAL_TRANSITION_OUTPUT, String.class);
				
				// required
				AttributeUtil.set(network, edge, SBML.ATTR_QUAL_QUALITATIVE_SPECIES, output.getQualitativeSpecies().toString(), String.class);
				AttributeUtil.set(network, edge, SBML.ATTR_QUAL_TRANSITION_EFFECT, output.getTransitionEffect().toString(), String.class);
				// optional
				if (output.isSetId()){
					AttributeUtil.set(network, edge, SBML.ATTR_ID, output.getId(), String.class);
				}
				if (output.isSetName()){
					AttributeUtil.set(network, edge, SBML.ATTR_NAME, output.getName(), String.class);
				}
				if (output.isSetSBOTerm()){
					AttributeUtil.set(network, edge, SBML.ATTR_SBOTERM, output.getSBOTermID(), String.class);
				}
				if (output.isSetMetaId()){
					AttributeUtil.set(network, edge, SBML.ATTR_METAID, output.getMetaId(), String.class);
				}
				if (output.isSetOutputLevel()){
					AttributeUtil.set(network, edge, SBML.ATTR_QUAL_OUTPUT_LEVEL, output.getOutputLevel(), String.class);
				}
			}
			
			// parse the default term / function terms
			if (transition.isSetListOfFunctionTerms()){
				List<Integer> resultLevels = new LinkedList<Integer>();
				for (FunctionTerm term: transition.getListOfFunctionTerms()){
					resultLevels.add(term.getResultLevel());
				}
				AttributeUtil.set(network, node, SBML.ATTR_QUAL_RESULT_LEVELS, resultLevels, List.class);
			}
		}
	}
	
	
	/** Creates network information from fbc model. */
	private void readFBC(Model model, FBCModelPlugin fbcModel){
		logger.info("** fbc **");

		// Model attributes
		if (fbcModel.isSetStrict()){
			AttributeUtil.set(network, network, SBML.ATTR_FBC_STRICT, fbcModel.getStrict(), Boolean.class);
		}
		
		// Species attributes
		for (Species species: model.getListOfSpecies()){
			FBCSpeciesPlugin fbcSpecies = (FBCSpeciesPlugin) species.getExtension(FBCConstants.namespaceURI);
			if (fbcSpecies == null){
				// Check if species has overwritten fbc information
				continue;
			}
			CyNode node = nodeById.get(species.getId());
			// optional
			if (fbcSpecies.isSetCharge()){
				AttributeUtil.set(network, node, SBML.ATTR_FBC_CHARGE, fbcSpecies.getCharge(), Integer.class);
			}
			if (fbcSpecies.isSetChemicalFormula()){
				AttributeUtil.set(network, node, SBML.ATTR_FBC_CHEMICAL_FORMULA, fbcSpecies.getChemicalFormula(), String.class);
			}
		}
		
		// List of flux objectives (handled via reaction attributes)
		// (activeObjective is not parsed)
		for (Objective objective : fbcModel.getListOfObjectives()){
			// one reaction attribute column per objective
			String key = String.format(SBML.ATTR_FBC_OBJECTIVE_TEMPLATE, objective.getId());
			for (FluxObjective fluxObjective : objective.getListOfFluxObjectives()){
				String reactionId = fluxObjective.getReaction();
				CyNode node = nodeById.get(reactionId);
				AttributeUtil.set(network, node, key, fluxObjective.getCoefficient(), Double.class);
			}
		}
		
		// GeneProducts as nodes
		for (GeneProduct geneProduct : fbcModel.getListOfGeneProducts()){
			CyNode node = createNamedSBaseNode(geneProduct, SBML.NODETYPE_FBC_GENEPRODUCT);
			// Overwrite label
			AttributeUtil.set(network, node, SBML.LABEL, geneProduct.getLabel(), String.class);
			
			// edge to associated species
			if (geneProduct.isSetAssociatedSpecies()){
				CyNode speciesNode = nodeById.get(geneProduct.getAssociatedSpecies());
				CyEdge edge = network.addEdge(speciesNode, node, true);
				AttributeUtil.set(network, edge, SBML.INTERACTION_ATTR, SBML.INTERACTION_FBC_GENEPRODUCT_SPECIES, String.class);
			}
		}
		
		// Reaction attributes
		for (Reaction reaction: model.getListOfReactions()){
			FBCReactionPlugin fbcReaction = (FBCReactionPlugin) reaction.getExtension(FBCConstants.namespaceURI);
			
			if (fbcReaction == null){
				continue;
			}
			// optional bounds
			CyNode node = nodeById.get(reaction.getId());
			if (fbcReaction.isSetLowerFluxBound()){
				AttributeUtil.set(network, node, SBML.ATTR_FBC_LOWER_FLUX_BOUND, fbcReaction.getLowerFluxBound(), String.class);
				// add edge
				CyNode parameterNode = nodeById.get(fbcReaction.getLowerFluxBound());
				CyEdge edge = network.addEdge(parameterNode, node, true);
				AttributeUtil.set(network, edge, SBML.INTERACTION_ATTR, SBML.INTERACTION_PARAMETER_REACTION, String.class);
			}
			if (fbcReaction.isSetUpperFluxBound()){
				AttributeUtil.set(network, node, SBML.ATTR_FBC_UPPER_FLUX_BOUND, fbcReaction.getUpperFluxBound(), String.class);
				// add edge
				CyNode parameterNode = nodeById.get(fbcReaction.getLowerFluxBound());
				CyEdge edge = network.addEdge(parameterNode, node, true);
				AttributeUtil.set(network, edge, SBML.INTERACTION_ATTR, SBML.INTERACTION_PARAMETER_REACTION, String.class);
			}
			
			// Create GeneProteinAssociation (GPA) network
			if (fbcReaction.isSetGeneProteinAssociation()){
				GeneProteinAssociation gpa = fbcReaction.getGeneProteinAssociation();
				
				// handle And, Or, GeneProductRef recursively
				Association association = gpa.getAssociation();
				processAssociation(node, SBML.NODETYPE_REACTION, association);
			}
		}
		
		// parse fbc v1 fluxBounds and geneAssociations
		if (fbcModel.getVersion() == 1){
			// geneAssociations
			if (model.isSetAnnotation()){
				// fbc v1 geneAssociations not in specification or supported by JSBML, so not parsed
				Annotation annotation = model.getAnnotation();
				XMLNode xmlNode = annotation.getXMLNode();
				
				for (int k=0; k<xmlNode.getChildCount(); k++){
					XMLNode child = xmlNode.getChild(k);
					String name = child.getName();
					if (name.equals("listOfGeneAssociations") | name.equals("geneAssociation")){
						logger.warn("GeneAssociations of fbc v1 not supported in JSBML.");
						break;
					}
				}
			}
			
			// fluxBounds
			for (FluxBound fluxBound : fbcModel.getListOfFluxBounds()){
				String reactionId = fluxBound.getReaction();
				CyNode n = nodeById.get(reactionId);
				String operation = fluxBound.getOperation().toString();
				Double value = fluxBound.getValue();
				if (operation.equals(Operation.EQUAL)){
					AttributeUtil.set(network, n, SBML.ATTR_FBC_LOWER_FLUX_BOUND, value.toString(), String.class);
					AttributeUtil.set(network, n, SBML.ATTR_FBC_UPPER_FLUX_BOUND, value.toString(), String.class);
				} else if (operation.equals(Operation.GREATER_EQUAL)){
					AttributeUtil.set(network, n, SBML.ATTR_FBC_LOWER_FLUX_BOUND, value.toString(), String.class);
				} else if (operation.equals(Operation.LESS_EQUAL)){
					AttributeUtil.set(network, n, SBML.ATTR_FBC_UPPER_FLUX_BOUND, value.toString(), String.class);
				}
			}
		}
	}

	/** Recursive function for processing the Associations. 
	 * TODO: remove code redundancy 
	 */
	private void processAssociation(CyNode parentNode, String parentType, Association association){
		
		if (association.getClass().equals(GeneProductRef.class)){
			GeneProductRef gpRef = (GeneProductRef) association;
			CyNode gpNode = nodeById.get(gpRef.getGeneProduct());
	
			CyEdge edge = network.addEdge(gpNode, parentNode, true);
			AttributeUtil.set(network, edge, SBML.ATTR_STOICHIOMETRY, 1.0, Double.class);
			if (parentType.equals(SBML.NODETYPE_REACTION)){
				AttributeUtil.set(network, edge, SBML.INTERACTION_ATTR, SBML.INTERACTION_FBC_ASSOCIATION_REACTION, String.class);
			} else {
				AttributeUtil.set(network, edge, SBML.INTERACTION_ATTR, SBML.INTERACTION_FBC_ASSOCIATION_ASSOCIATION, String.class);
			}
			
		}else if (association.getClass().equals(And.class)){
			And andRef = (And) association;
			
			// Create and node & edge
			CyNode andNode = network.addNode();
			AttributeUtil.set(network, andNode, SBML.LABEL, "AND", String.class);
			AttributeUtil.set(network, andNode, SBML.NODETYPE_ATTR, SBML.NODETYPE_FBC_AND, String.class);
			CyEdge edge = network.addEdge(andNode, parentNode, true);
			if (parentType.equals(SBML.NODETYPE_REACTION)){
				AttributeUtil.set(network, edge, SBML.INTERACTION_ATTR, SBML.INTERACTION_FBC_ASSOCIATION_REACTION, String.class);
			} else {
				AttributeUtil.set(network, edge, SBML.INTERACTION_ATTR, SBML.INTERACTION_FBC_ASSOCIATION_ASSOCIATION, String.class);
			}
			
			// Get the association children
			for (Association a : andRef.getListOfAssociations()){
				processAssociation(andNode, SBML.NODETYPE_FBC_AND, a);
			}
			
		}else if (association.getClass().equals(Or.class)){
			Or orRef = (Or) association;
			
			// Create and node & edge
			CyNode orNode = network.addNode();
			AttributeUtil.set(network, orNode, SBML.LABEL, "OR", String.class);
			AttributeUtil.set(network, orNode, SBML.NODETYPE_ATTR, SBML.NODETYPE_FBC_OR, String.class);
			CyEdge edge = network.addEdge(orNode, parentNode, true);
			if (parentType.equals(SBML.NODETYPE_REACTION)){
				AttributeUtil.set(network, edge, SBML.INTERACTION_ATTR, SBML.INTERACTION_FBC_ASSOCIATION_REACTION, String.class);
			} else {
				AttributeUtil.set(network, edge, SBML.INTERACTION_ATTR, SBML.INTERACTION_FBC_ASSOCIATION_ASSOCIATION, String.class);
			}
			
			// Get the association children
			for (Association a : orRef.getListOfAssociations()){
				processAssociation(orNode, SBML.NODETYPE_FBC_AND, a);
			}
		}
	}
	
	// --- LAYOUT -----------------------------------------------------------------------------------
	
		/** Creates the layouts stored in the layout extension. */
		private void readLayouts(Model model, QualModelPlugin qualModel, LayoutModelPlugin layoutModel){
			logger.info("** layout **");
			for (Layout layout : layoutModel.getListOfLayouts()){
				// TODO: manage the multiple layout networks
				// layoutNetwork = rootNetwork.addSubNetwork();
				readLayout(model, qualModel, layout);
			}
		}
		
		/** Read a single layout. */
		private void readLayout(Model model, QualModelPlugin qualModel, Layout layout){

			// Process the layouts (Generate full id set and all edges for elements)
			LayoutPreprocessor preprocessor = new LayoutPreprocessor(model, qualModel, layout);
			layout = preprocessor.getProcessedLayout();
			
			// now generate nodes and edges
			// TODO: AttributeUtil.set(layoutNetwork, layoutNetwork, SBML.NETWORKTYPE_ATTR, SBML.NETWORKTYPE_LAYOUT, String.class);
			
			// addSpeciesGlyphNodes
			for (SpeciesGlyph glyph : layout.getListOfSpeciesGlyphs()) {
				// create the node
				String id = glyph.getId();
				// creates node in network
				CyNode node = createNamedSBaseNode(glyph, SBML.NODETYPE_LAYOUT_SPECIESGLYPH);
				
				// get species node and copy information
				if (glyph.isSetSpecies()){
					String speciesId = glyph.getSpecies();
					if (nodeById.containsKey(speciesId)){
						CyNode sNode = nodeById.get(speciesId);
						
						// copy node attributes from species node to speciesGlyph node
						copyNodeAttributes(sNode, node);	
					}
					
				} else {
					AttributeUtil.set(network, node, SBML.ATTR_ID, id, String.class);
					AttributeUtil.set(network, node, SBML.ATTR_TYPE, SBML.NODETYPE_LAYOUT_SPECIESGLYPH, String.class);
				}
			}
			
			// addReactionGlyphNodes();
			
			// addModelEdges();
			// addQualitativeModelEdges();
		}
	
	
	@Override
	public CyNetwork[] getNetworks() {
		return new CyNetwork[] { network, coreNetwork };
	}

	@Override
	public CyNetworkView buildCyNetworkView(final CyNetwork network) {
		logger.info("buildCyNetworkView");
		// Preload SBML WebService information
		NamedSBaseInfoThread.preloadAnnotationsForSBMLDocument(document);
				
		// Set SBML in SBMLManager 
		SBMLManager sbmlManager = SBMLManager.getInstance();
		// Look for already existing mappings (of read networks)
		One2ManyMapping<String, Long> mapping = sbmlManager.getMapping(network);
		mapping = NamedSBase2CyNodeMapping.fromSBMLNetwork(document, network, mapping);
		
		// existing mapping is updated
		sbmlManager.addSBML2NetworkEntry(document, network, mapping);
		// update the current network
		sbmlManager.updateCurrent(network);
		
		// Display the model information in the results pane
		ResultsPanel.getInstance().getTextPane().showNSBInfo(document.getModel());
		
		// create view depending on mode
		CyNetworkView view = adapter.cyNetworkViewFactory.createNetworkView(network);
		
		logger.debug("network: " + network.toString());
		logger.debug("view: " + view.toString());
		return view;
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

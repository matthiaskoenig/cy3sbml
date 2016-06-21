package org.cy3sbml;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.cytoscape.io.read.CyNetworkReader;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

// SBML CORE
import org.sbml.jsbml.ASTNode;
import org.sbml.jsbml.AbstractMathContainer;
import org.sbml.jsbml.Annotation;
import org.sbml.jsbml.AssignmentRule;
import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.FunctionDefinition;
import org.sbml.jsbml.InitialAssignment;
import org.sbml.jsbml.JSBML;
import org.sbml.jsbml.KineticLaw;
import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.LocalParameter;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.ModifierSpeciesReference;
import org.sbml.jsbml.NamedSBase;
import org.sbml.jsbml.Parameter;
import org.sbml.jsbml.QuantityWithUnit;
import org.sbml.jsbml.RateRule;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.Rule;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBase;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.SpeciesReference;
import org.sbml.jsbml.Symbol;
import org.sbml.jsbml.UnitDefinition;
import org.sbml.jsbml.util.CobraUtil;
import org.sbml.jsbml.xml.XMLNode;
// SBML QUAL
import org.sbml.jsbml.ext.qual.FunctionTerm;
import org.sbml.jsbml.ext.qual.Input;
import org.sbml.jsbml.ext.qual.Output;
import org.sbml.jsbml.ext.qual.QualConstants;
import org.sbml.jsbml.ext.qual.QualModelPlugin;
import org.sbml.jsbml.ext.qual.QualitativeSpecies;
import org.sbml.jsbml.ext.qual.Transition;
// SBML FBC
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
import org.sbml.jsbml.ext.fbc.GeneProductAssociation;
import org.sbml.jsbml.ext.fbc.GeneProductRef;
import org.sbml.jsbml.ext.fbc.Objective;
import org.sbml.jsbml.ext.fbc.Or;
// SBML COMP
import org.sbml.jsbml.ext.comp.CompConstants;
import org.sbml.jsbml.ext.comp.CompModelPlugin;
import org.sbml.jsbml.ext.comp.Port;
// SBML GROUPS
import org.sbml.jsbml.ext.groups.GroupsConstants;
import org.sbml.jsbml.ext.groups.GroupsModelPlugin;
// DISTRIB
import org.sbml.jsbml.ext.distrib.DistribConstants;
import org.sbml.jsbml.ext.distrib.DistribSBasePlugin;
import org.sbml.jsbml.ext.distrib.Uncertainty;

// LAYOUT
import org.sbml.jsbml.ext.layout.Layout;
import org.sbml.jsbml.ext.layout.LayoutConstants;
import org.sbml.jsbml.ext.layout.LayoutModelPlugin;
import org.sbml.jsbml.ext.layout.SpeciesGlyph;

import org.cy3sbml.gui.ResultsPanel;
import org.cy3sbml.layout.LayoutPreprocessor;
import org.cy3sbml.mapping.IdNodeMap;
import org.cy3sbml.mapping.One2ManyMapping;
import org.cy3sbml.miriam.SBaseInfoThread;
import org.cy3sbml.util.ASTNodeUtil;
import org.cy3sbml.util.AnnotationUtil;
import org.cy3sbml.util.AttributeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The SBMLReaderTask creates CyNetworks from SBMLDocuments.
 * 
 * The reader creates the master SBML network graph with various subnetworks 
 * created from the full graph.
 */
@SuppressWarnings("deprecation")
public class SBMLReaderTask extends AbstractTask implements CyNetworkReader {	
	private static final Logger logger = LoggerFactory.getLogger(SBMLReaderTask.class);
	
	private static final int BUFFER_SIZE = 16384;
	
	private String fileName;
	private final InputStream stream;
	private final CyNetworkFactory networkFactory;
	private final CyNetworkViewFactory viewFactory;
	@SuppressWarnings("unused")
	private final CyNetworkViewManager viewManager;
	private SBMLDocument document;
	
	private Boolean error = false;
	private CyNetwork network;       // global network of all SBML information
	private CyNetwork mainNetwork;   // core reaction, species, (qualSpecies, qualTransitions), fbc network
	
	private CyRootNetwork rootNetwork;
	private Map<String, CyNode> nodeById; // node dictionary
	
	
	/** Constructor */ 
	public SBMLReaderTask(InputStream stream, String fileName, CyNetworkFactory networkFactory, 
			CyNetworkViewFactory viewFactory,
			CyNetworkViewManager viewManager) {
		
		this.stream = stream;
		this.networkFactory = networkFactory;
		this.viewFactory = viewFactory;
		this.viewManager = viewManager;
		this.fileName = fileName;
	}
	
	
	/** Parse the SBML networks. */
	public void run(TaskMonitor taskMonitor) throws Exception {
		logger.info("---------------------------------");
		logger.info("Start Reader.run()");
		logger.info("---------------------------------");
		try {
			if (taskMonitor != null){
				taskMonitor.setTitle("cy3sbml reader");
				taskMonitor.setProgress(0.0);
			}
			if(cancelled){
				return;
			}
			
			// Read model
			logger.debug("JSBML version: " + JSBML.getJSBMLVersionString());
			String xml = readString(stream);
			
			// TODO: store and display JSBML reader warnings
			document = JSBML.readSBMLFromString(xml);
			Model model = null;
			if (document.isSetModel()){
				model = document.getModel();
			} else {
				logger.warn("No model in SBML file. Please check the model definition.");
				model = document.createModel();
				model.setId("null_model");
			}
		
			
			// Create empty root network and node map
			network = networkFactory.createNetwork();
			nodeById = new HashMap<String, CyNode>();
			
			// To create a new CySubNetwork with the same CyNetwork's CyRootNetwork, cast your CyNetwork to
			// CySubNetwork and call the CySubNetwork.getRootNetwork() method:
			// 		CyRootNetwork rootNetwork = ((CySubNetwork)network).getRootNetwork(); 
			// CyRootNetwork also provides methods to create and add new subnetworks (see CyRootNetwork.addSubNetwork()). 
			rootNetwork = ((CySubNetwork) network).getRootNetwork();
			
			//////////////////////////////////////////////////////////////////
			// Read SBML & Extensions
			//////////////////////////////////////////////////////////////////
			// Creates the main network of all information
			readCore(model);
			if (taskMonitor != null){
				taskMonitor.setProgress(0.5);
			}

			
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
				readComp(model, compModel);
			}
			
			GroupsModelPlugin groupsModel = (GroupsModelPlugin) model.getExtension(GroupsConstants.namespaceURI);
			if (groupsModel != null){
				logger.info("groups model found, but not yet supported");
			}
			
			LayoutModelPlugin layoutModel = (LayoutModelPlugin) model.getExtension(LayoutConstants.namespaceURI);
			if (layoutModel != null){
				logger.info("layout model found, but not yet supported");
				//readLayouts(model, qualModel, layoutModel);	
			}
			
			readDistrib(model);

			// Add compartment codes dynamically for colors
			addCompartmentCodes(network, model);
						
			//////////////////////////////////////////////////////////////////
			
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
				String type = row.get(SBML.NODETYPE_ATTR, String.class);
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
			
			// Create the main subnetwork if nodes exist in main network.
			// Models can be fully encoded via parameters & rules,
			// resulting in an empty main network (for instance BIOMD0000000020).
			if (nodes.size() > 0){
				mainNetwork = rootNetwork.addSubNetwork(nodes, edges);
				
				// set name of main network
				String name = network.getRow(network).get(CyNetwork.NAME, String.class);
				if (name == null){
					// name not set, try backup name via id
					name = network.getRow(network).get(SBML.ATTR_ID, String.class);
					// still not set, use the file name
					if (name == null){
						name = fileName;
					}
				}
				mainNetwork.getRow(mainNetwork).set(CyNetwork.NAME, "Main: "+ name);	
			}
			
			// Layout subnetworks
			// TODO: create layout subnetworks (different mechanism necessary, 
			//   probably via direct subnetwork generation)
			
			if (taskMonitor != null){
				taskMonitor.setProgress(1.0);
			}
			logger.info("---------------------------------");
			logger.info("End Reader.run()");
			logger.info("---------------------------------");
		
		} catch (Throwable t){
			logger.error("Could not read SBML into Cytoscape!", t);
			error = true;
			t.printStackTrace();
			throw new SBMLReaderError("cy3sbml reader failed to build a SBML model " +
					"(check the data for syntax errors) - " + t);
			
			// TODO: run validator on the file and display the results
			// with high probability this SBML file is corrupt and can not be parsed.
			// => Display the information 
			// => send SBML to author
		}
	}
	
	/** 
	 * Returns the error status for unit testing. 
	 */
	public Boolean getError(){
		return error;
	}

	/**
	 * Adds integer compartment codes as node attribute.
	 * 
	 * The compartmentCodes can be used in the visual mapping for dynamical
	 * visualization of compartment colors.
	 */
	private void addCompartmentCodes(CyNetwork network, Model model){
		// Calculate compartment code mapping
		HashMap<String, Integer> compartmentCodes = new HashMap<String, Integer>();
		Integer compartmentCode = 1;
		for (Compartment c: model.getListOfCompartments()){
			String cid = c.getId();
			if (!compartmentCodes.containsKey(cid)){
				compartmentCodes.put(cid, compartmentCode);
				compartmentCode += 1;
			}
		}
		// set compartment code attribute
		for (CyNode n : network.getNodeList()){
			String cid = AttributeUtil.get(network, n, SBML.ATTR_COMPARTMENT, String.class);
			Integer code = compartmentCodes.get(cid);
			AttributeUtil.set(network, n, SBML.ATTR_COMPARTMENT_CODE, code, Integer.class);
		}
	}
	
	/** 
	 * Sets metaId and SBOTerm. 
	 * RDF & COBRA attributes are set. 
	 */
	private void setSBaseAttributes(CyIdentifiable cyObject, SBase sbase){
		if (sbase.isSetSBOTerm()){
			AttributeUtil.set(network, cyObject, SBML.ATTR_SBOTERM, sbase.getSBOTermID(), String.class);
		}
		if (sbase.isSetMetaId()){
			AttributeUtil.set(network, cyObject, SBML.ATTR_METAID, sbase.getMetaId(), String.class);
		}
		// COBRA attributes
		Properties props = CobraUtil.parseCobraNotes(sbase);
		for(Object key : props.keySet()){
			String keyString = key.toString();
			String valueString = props.getProperty((String) key);
			AttributeUtil.set(network, cyObject, keyString, valueString, String.class);
		}
		// RDF attributes
		props = AnnotationUtil.parseCVTerms(sbase);
		for(Object key : props.keySet()){
			String keyString = key.toString();
			String valueString = props.getProperty((String) key);
			AttributeUtil.set(network, cyObject, keyString, valueString, String.class);
		}
	}
	
	private CyNode createNamedSBaseNode(NamedSBase sbase, String type){
		String id = sbase.getId();
		// create node and add to network
	 	CyNode n = network.addNode();
	 	nodeById.put(id, n);
	 	// set the attributes
		AttributeUtil.set(network, n, SBML.ATTR_ID, id, String.class);
		AttributeUtil.set(network, n, SBML.NODETYPE_ATTR, type, String.class);
		setSBaseAttributes(n, sbase);
		if (sbase.isSetName()){
			AttributeUtil.set(network, n, SBML.ATTR_NAME, sbase.getName(), String.class);
			AttributeUtil.set(network, n, SBML.LABEL, sbase.getName(), String.class);
		} else {
			AttributeUtil.set(network, n, SBML.LABEL, id, String.class);
		}
		return n;
	}
	
	/* Handle QuantityWithUnit. 
	 * Among others localParameters. */
	private CyNode createQuantityWithUnitNode(QuantityWithUnit q, String type){
		CyNode n = createNamedSBaseNode((NamedSBase) q, type);
		if (q.isSetValue()){
			AttributeUtil.set(network, n, SBML.ATTR_VALUE, q.getValue(), Double.class);
		}
		if (q.isSetUnits()){
			AttributeUtil.set(network, n, SBML.ATTR_UNITS, q.getUnits(), String.class);
		}
		UnitDefinition udef = q.getDerivedUnitDefinition();
		if (udef != null){
			AttributeUtil.set(network, n, SBML.ATTR_DERIVED_UNITS, udef.toString(), String.class);
		}
		return n;
	}
	
	/* Handle symbol nodes.
	 * Among others species and parameters. */
	private CyNode createSymbolNode(Symbol symbol, String type){
		CyNode n = createQuantityWithUnitNode(symbol, type);
		if (symbol.isSetConstant()){
			AttributeUtil.set(network, n, SBML.ATTR_CONSTANT, symbol.getConstant(), Boolean.class);
		}
		return n;
	}
	
	/* Handle AbstractMathContainer nodes.
	 * Direct known subclasses
	 *     AnalyticVolume, 
	 *     Constraint, 
	 *     Delay, 
	 *     EventAssignment, 
	 *     FunctionDefinition, 
	 *     FunctionTerm, 
	 *     Index, 
	 *     InitialAssignment, 
	 *     KineticLaw, 
	 *     Priority, 
	 *     Rule, 
	 *     StoichiometryMath, 
	 *     Trigger
	 */
	private CyNode createAbstractMathContainerNode(AbstractMathContainer container, String type){
		CyNode n = network.addNode();
		AttributeUtil.set(network, n, SBML.NODETYPE_ATTR, type, String.class);
		setSBaseAttributes(n, container);
		
		String derivedUnits = container.getDerivedUnits();
		AttributeUtil.set(network, n, SBML.ATTR_DERIVED_UNITS, derivedUnits, String.class);
	
		if (container.isSetMath()){
			ASTNode astNode = container.getMath();
			AttributeUtil.set(network, n, SBML.ATTR_MATH, astNode.toFormula(), String.class);
		}
		
		return n;
	}
	
	/** Creates math subgraph for given math container and node. */
	private void createMathNetwork(AbstractMathContainer container, CyNode containerNode, String edgeType){
		if (container.isSetMath()){
			ASTNode astNode = container.getMath();
			AttributeUtil.set(network, containerNode, SBML.ATTR_MATH, astNode.toFormula(), String.class);
			for (NamedSBase nsb : ASTNodeUtil.findReferencedNamedSBases(astNode)){
				// This can be parameters, localParameters, species, ...
				// add edge if node exists
				CyNode nsbNode = nodeById.get(nsb.getId());
				if (nsbNode != null){
					CyEdge edge = network.addEdge(nsbNode, containerNode, true);
					AttributeUtil.set(network, edge, SBML.INTERACTION_ATTR, edgeType, String.class);	
				}else{
					logger.warn("Node for id in math not found: " + nsb.getId());
				}
			}
		}
	}
	
	////////////////////////////////////////////////////////////////////////////
	// SBML CORE
	////////////////////////////////////////////////////////////////////////////
	/**
	 * Read SBML core.
	 * 
	 * Create nodes, edges and attributes from SBML core.
	 * @param model
	 */
	private void readCore(Model model){
		logger.info("** core **");
		// Mark network as SBML
		AttributeUtil.set(network, network, SBML.NETWORKTYPE_ATTR, SBML.NETWORKTYPE_SBML, String.class);
		AttributeUtil.set(network, network, SBML.LEVEL_VERSION, String.format("L%1$s V%2$s", document.getLevel(), document.getVersion()), String.class);
		// metaId and SBO
		setSBaseAttributes(network, model);
		
		// Network attributes
		if (model.isSetId()){
			AttributeUtil.set(network, network, SBML.ATTR_ID, model.getId(), String.class);	
		}
		if (model.isSetName()){
			AttributeUtil.set(network, network, SBML.ATTR_NAME, model.getName(), String.class);
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
	
		// TODO: UnitDefinitions (not parsed)
		
		// FunctionDefinitions
		for (FunctionDefinition fd : model.getListOfFunctionDefinitions()){
			CyNode fdNode = createNamedSBaseNode(fd, SBML.NODETYPE_FUNCTION_DEFINITION);
			
			String derivedUnits = fd.getDerivedUnits();
			AttributeUtil.set(network, fdNode, SBML.ATTR_DERIVED_UNITS, derivedUnits, String.class);
		
			createMathNetwork(fd, fdNode, SBML.INTERACTION_REFERENCE_FUNCTIONDEFINITION);
		}

		
		// Nodes for compartments		
		for (Compartment compartment : model.getListOfCompartments()) {
			CyNode node = createSymbolNode(compartment, SBML.NODETYPE_COMPARTMENT);
			if (compartment.isSetSpatialDimensions()){
				AttributeUtil.set(network, node, SBML.ATTR_SPATIAL_DIMENSIONS, compartment.getSpatialDimensions(), Double.class);
			}
			if (compartment.isSetSize()){
				AttributeUtil.set(network, node, SBML.ATTR_SIZE, compartment.getSize(), Double.class);
			}
		}
		
		// Nodes for parameters
		for (Parameter parameter : model.getListOfParameters()) {
			@SuppressWarnings("unused")
			CyNode node = createSymbolNode(parameter, SBML.NODETYPE_PARAMETER);
		}
	
		
		// Nodes for species
		for (Species species : model.getListOfSpecies()) {
			CyNode node = createSymbolNode(species, SBML.NODETYPE_SPECIES);
			if (species.isSetCompartment()){
				// edge to compartment
				AttributeUtil.set(network, node, SBML.ATTR_COMPARTMENT, species.getCompartment(), String.class);
				CyNode comp = nodeById.get(species.getCompartment());
				if (comp != null){
					CyEdge edge = network.addEdge(node, comp, true);
					AttributeUtil.set(network, edge, SBML.INTERACTION_ATTR, SBML.INTERACTION_SPECIES_COMPARTMENT, String.class);
				} else {
					logger.error(String.format("Compartment does not exist for species: %s for %s", species.getCompartment(), species.getId()));
				}
			}
			if (species.isSetBoundaryCondition()){
				AttributeUtil.set(network, node, SBML.ATTR_BOUNDARY_CONDITION, species.getBoundaryCondition(), Boolean.class);
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
		}	
		
		// Reactions
		for (Reaction reaction : model.getListOfReactions()) {
			CyNode node = createNamedSBaseNode(reaction, SBML.NODETYPE_REACTION);
	
			if (reaction.isSetCompartment()){
				AttributeUtil.set(network, node, SBML.ATTR_COMPARTMENT, reaction.getCompartment(), String.class);
				// connect to reaction to compartment
				CyNode comp = nodeById.get(reaction.getCompartment());
				if (comp != null){
					CyEdge edge = network.addEdge(node, comp, true);
					AttributeUtil.set(network, edge, SBML.INTERACTION_ATTR, SBML.INTERACTION_REACTION_COMPARTMENT, String.class);
				} else {
					logger.error(String.format("Compartment does not exist for reaction: %s for %s", reaction.getCompartment(), reaction.getId()));
				}
			}
			
			if (reaction.isSetReversible()){
				AttributeUtil.set(network, node, SBML.ATTR_REVERSIBLE, reaction.getReversible(), Boolean.class);
			} else {
				// Reactions set reversible by default
				AttributeUtil.set(network, node, SBML.ATTR_REVERSIBLE, true, Boolean.class);
			}
			if (reaction.isSetFast()){
				AttributeUtil.set(network, node, SBML.ATTR_FAST, reaction.getFast(), Boolean.class);
			}
			if (reaction.isSetKineticLaw()){
				KineticLaw law = reaction.getKineticLaw();
				if (law.isSetMath()){
					AttributeUtil.set(network, node, SBML.ATTR_KINETIC_LAW, law.getMath().toFormula(), String.class);	
				} else {
					logger.warn(String.format("No math set for kinetic law in reaction: %s", reaction.getId()));
				}
			}
			UnitDefinition udef = reaction.getDerivedUnitDefinition();
			if (udef != null){
				AttributeUtil.set(network, node, SBML.ATTR_DERIVED_UNITS, udef.toString(), String.class);
			}
		
			// Reactants
			Double stoichiometry;
			for (SpeciesReference speciesRef : reaction.getListOfReactants()) {
				CyNode reactant = nodeById.get(speciesRef.getSpecies());
				if (reactant != null){
					CyEdge edge = network.addEdge(node, reactant, true);
					AttributeUtil.set(network, edge, SBML.INTERACTION_ATTR, SBML.INTERACTION_REACTION_REACTANT, String.class);
					setSBaseAttributes(edge, speciesRef);  // metaId and sbo
					
					if (speciesRef.isSetStoichiometry()){
						stoichiometry = speciesRef.getStoichiometry();
					} else {
						// default to 1.0
						stoichiometry = 1.0;
					}
					AttributeUtil.set(network, edge, SBML.ATTR_STOICHIOMETRY, stoichiometry, Double.class);
				} else {
					logger.error(String.format("Reactant does not exist for reaction: %s for %s", speciesRef.getSpecies(), reaction.getId()));
				}
			}
			// Products
			for (SpeciesReference speciesRef : reaction.getListOfProducts()) {
				CyNode product = nodeById.get(speciesRef.getSpecies());
				if (product != null){
					CyEdge edge = network.addEdge(node, product, true);
					AttributeUtil.set(network, edge, SBML.INTERACTION_ATTR, SBML.INTERACTION_REACTION_PRODUCT, String.class);
					setSBaseAttributes(edge, speciesRef);  // metaId and sbo
					if (speciesRef.isSetStoichiometry()){
						stoichiometry = speciesRef.getStoichiometry();
					} else {
						// default to 1.0
						stoichiometry = 1.0;
					}
					AttributeUtil.set(network, edge, SBML.ATTR_STOICHIOMETRY, stoichiometry, Double.class);
				} else {
					logger.error(String.format("Product does not exist for reaction: %s for %s", speciesRef.getSpecies(), reaction.getId()));
				}

			}
			// Modifiers
			for (ModifierSpeciesReference msRef : reaction.getListOfModifiers()) {
				CyNode modifier = nodeById.get(msRef.getSpecies());
				if (modifier != null){
					CyEdge edge = network.addEdge(node, modifier, true);
					AttributeUtil.set(network, edge, SBML.INTERACTION_ATTR, SBML.INTERACTION_REACTION_MODIFIER, String.class);
					setSBaseAttributes(edge, msRef);  // metaId and sbo	
				} else {
					logger.error(String.format("ModifierSpecies does not exist for reaction: %s for %s", msRef.getSpecies(), reaction.getId()));
				}
			}
			
			// Kinetic law 
			if (reaction.isSetKineticLaw()){
				KineticLaw law = reaction.getKineticLaw();
				String reactionId = reaction.getId();
				String lawId = String.format("%s_law", reactionId);
				// node
			 	CyNode lawNode = createAbstractMathContainerNode(law, SBML.NODETYPE_KINETIC_LAW);
			 	nodeById.put(lawId, lawNode);
				AttributeUtil.set(network, lawNode, SBML.ATTR_ID, lawId, String.class);
				AttributeUtil.set(network, lawNode, SBML.LABEL, lawId, String.class);
				
				// edge to reaction
				CyEdge edge = network.addEdge(node, lawNode, true);
				AttributeUtil.set(network, edge, SBML.INTERACTION_ATTR, SBML.INTERACTION_REACTION_KINETICLAW, String.class);
				
				// local parameter nodes
				if (law.isSetListOfLocalParameters()){
					for (LocalParameter lp: law.getListOfLocalParameters()){
						String lpId = lp.getId();
						// necessary to create new identifiers due to scoping of local parameters
						String tmpId = String.format("%s_%s", reactionId, lpId);
						// FIXME: this changes the model during the reading, but necessary for the unique
						// 		  mapping. Not a problem currently, but will when writing SBML.
						lp.setId(tmpId);
						// create node
						CyNode lpNode = createQuantityWithUnitNode(lp, SBML.NODETYPE_LOCAL_PARAMTER);
						
						// edge to reaction
						CyEdge lpEdge = network.addEdge(lpNode, lawNode, true);
						AttributeUtil.set(network, lpEdge, SBML.INTERACTION_ATTR, SBML.INTERACTION_LOCALPARAMETER_KINETICLAW, String.class);
					}
				}
				
				// referenced nodes in math
				createMathNetwork(law, lawNode, SBML.INTERACTION_REFERENCE_KINETICLAW);
			}
		}
		
		// InitialAssignments set as attributes on target nodes (variables)
		for (InitialAssignment assignment : model.getListOfInitialAssignments()){
			String variable = assignment.getVariable();
			CyNode targetNode = nodeById.get(variable);
			if (targetNode != null){
				
				String id = variable + "_assignment";
			 	CyNode assignmentNode = createAbstractMathContainerNode(assignment, SBML.NODETYPE_INITIAL_ASSIGNMENT);
			 	nodeById.put(id, assignmentNode);
				AttributeUtil.set(network, assignmentNode, SBML.ATTR_ID, id, String.class);
				AttributeUtil.set(network, assignmentNode, SBML.LABEL, id, String.class);
			 	
				// edge to variable 
				CyNode variableNode = nodeById.get(variable);
				if (variableNode != null){
					CyEdge edge = network.addEdge(targetNode, assignmentNode, true);
					AttributeUtil.set(network, edge, SBML.INTERACTION_ATTR, SBML.INTERACTION_VARIABLE_INITIAL_ASSIGNMENT, String.class);	
				} else {
					logger.warn(String.format("Variable is neither Compartment, Species or Parameter, probably SpeciesReference: %s in %s", variable, id ));
				}

				// referenced nodes in math
				createMathNetwork(assignment, assignmentNode, SBML.INTERACTION_REFERENCE_INITIAL_ASSIGNMENT);
				if (assignment.isSetMath()){
					ASTNode astNode = assignment.getMath();
					AttributeUtil.set(network, targetNode, SBML.ATTR_INITIAL_ASSIGNMENT, astNode.toFormula(), String.class);
				}		
			} else {
				logger.error(String.format("Variable does not exist for InitialAssignment: %s for %s", assignment.getVariable(), "?"));
			}
		}
		
		// Rule nodes (Important for rate rule based models)
		// FIXME: only AssignmentRules and RateRules handled
		for (Rule rule : model.getListOfRules()){
			String variable = null;
			if (rule.isAssignment()){
				AssignmentRule assignmentRule = (AssignmentRule) rule;
				variable = assignmentRule.getVariable();
			} else if (rule.isRate()){
				RateRule rateRule = (RateRule) rule;
				variable = rateRule.getVariable();
			}
			if (variable != null){
				
				String id = variable + "_rule";
			 	CyNode ruleNode = createAbstractMathContainerNode(rule, SBML.NODETYPE_RULE);
			 	nodeById.put(id, ruleNode);
				AttributeUtil.set(network, ruleNode, SBML.ATTR_ID, id, String.class);
				AttributeUtil.set(network, ruleNode, SBML.LABEL, id, String.class);
				
				// edge to variable 
				// an assignment rule can refer to the identifer of a Species, SpeciesReference,
				// Compartment, or global Parameter object in the model
				// The case SpeciesReference is not handled !
				CyNode variableNode = nodeById.get(variable);
				if (variableNode != null){
					CyEdge edge = network.addEdge(variableNode, ruleNode, true);
					AttributeUtil.set(network, edge, SBML.INTERACTION_ATTR, SBML.INTERACTION_VARIABLE_RULE, String.class);	
				} else {
					logger.warn(String.format("Variable is neither Compartment, Species or Parameter, probably SpeciesReference: %s in %s", variable, id ));
				}
				
				// referenced nodes in math
				createMathNetwork(rule, ruleNode, SBML.INTERACTION_REFERENCE_RULE);
			}
		}
		
			
		// Constraints (not parsed)
		// for (Constraint constraint : model.getListOfConstraints()){}
		
		// Events (not parsed)
		// for (Event event : model.getListOfEvents()){}
	}
		
	////////////////////////////////////////////////////////////////////////////
	// SBML QUAL
	////////////////////////////////////////////////////////////////////////////
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
					AttributeUtil.set(network, edge, SBML.ATTR_QUAL_OUTPUT_LEVEL, output.getOutputLevel(), Integer.class);
				}
			}
			
			// parse the default term / function terms
			if (transition.isSetListOfFunctionTerms()){
				List<Integer> resultLevels = new LinkedList<Integer>();
				for (FunctionTerm term: transition.getListOfFunctionTerms()){
					resultLevels.add(term.getResultLevel());
				}
				AttributeUtil.setList(network, node, SBML.ATTR_QUAL_RESULT_LEVELS, resultLevels, Integer.class);
			}
		}
	}
	
	////////////////////////////////////////////////////////////////////////////
	// SBML FBC
	////////////////////////////////////////////////////////////////////////////
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
				CyNode parameterNode = nodeById.get(fbcReaction.getUpperFluxBound());
				CyEdge edge = network.addEdge(parameterNode, node, true);
				AttributeUtil.set(network, edge, SBML.INTERACTION_ATTR, SBML.INTERACTION_PARAMETER_REACTION, String.class);
			}
			
			// Create GeneProteinAssociation (GPA) network
			if (fbcReaction.isSetGeneProductAssociation()){
				GeneProductAssociation gpa = fbcReaction.getGeneProductAssociation();
				
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
			if (gpNode != null){
				CyEdge edge = network.addEdge(gpNode, parentNode, true);
				if (parentType.equals(SBML.NODETYPE_REACTION)){
					AttributeUtil.set(network, edge, SBML.INTERACTION_ATTR, SBML.INTERACTION_FBC_ASSOCIATION_REACTION, String.class);
				} else {
					AttributeUtil.set(network, edge, SBML.INTERACTION_ATTR, SBML.INTERACTION_FBC_ASSOCIATION_ASSOCIATION, String.class);
				}
			} else {
				logger.error(String.format("GeneProduct does not exist for GeneAssociation: %s in %s", gpRef.getGeneProduct(), association));
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
	
	////////////////////////////////////////////////////////////////////////////
	// SBML COMP
	////////////////////////////////////////////////////////////////////////////
	/** Create network information from comp model. */
	private void readComp(Model model, CompModelPlugin compModel){
		logger.info("** comp **");

		// TODO: model ListOfSubmodels
		//          -> listOfDeletions
		
		// List of ports
		for (Port port : compModel.getListOfPorts()) {
			CyNode node = createNamedSBaseNode(port, SBML.NODETYPE_COMP_PORT);
			
			if (port.isSetPortRef()){
				AttributeUtil.set(network, node, SBML.ATTR_COMP_PORTREF, port.getPortRef(), String.class);
			}
			if (port.isSetIdRef()){
				String idRef = port.getIdRef();
				AttributeUtil.set(network, node, SBML.ATTR_COMP_IDREF, idRef, String.class);
				// add edge
				CyNode portNode = nodeById.get(idRef);
				if (portNode == null){
					// for instance referring to submodel (not part of master network yet)
					logger.warn("No target found for port with idRef: ", idRef);
				} else {
					CyEdge edge = network.addEdge(node, portNode, true);
					AttributeUtil.set(network, edge, SBML.INTERACTION_ATTR, SBML.INTERACTION_COMP_PORT_ID, String.class);	
				}
				
			}
			if (port.isSetUnitRef()){
				AttributeUtil.set(network, node, SBML.ATTR_COMP_UNITREF, port.getUnitRef(), String.class);
			}
			if (port.isSetMetaIdRef()){
				AttributeUtil.set(network, node, SBML.ATTR_COMP_METAIDREF, port.getMetaIdRef(), String.class);
			}
		}
		
		// TODO: SBase replacedBy & listOfReplacedElements
		
	}
	
	
	////////////////////////////////////////////////////////////////////////////
	// SBML DISTRIB
	////////////////////////////////////////////////////////////////////////////
	/**
	 * Create uncertainty information from distrib package. 
	 */
	private void readDistrib(Model model){
		// TODO: necessary to display for all the SBase elements
		// TODO: write the string as attribute to the respective node
		
		logger.debug("** distrib **");
		// Compartments
		readUncertainties(model.getListOfCompartments());
		
		// Species
		readUncertainties(model.getListOfSpecies());
		
		// Parameters
		readUncertainties(model.getListOfParameters());
		
	}
	
	private void readUncertainties(ListOf<?> listOfSBase){
		for (SBase sbase: listOfSBase){
			DistribSBasePlugin dSBase = (DistribSBasePlugin) sbase.getExtension(DistribConstants.namespaceURI);
			if (dSBase != null && dSBase.isSetUncertainty()){
				Uncertainty uc = dSBase.getUncertainty();
				if (uc.isSetUncertML()){
					readUncertainty(sbase, uc);
				}
			}			
		}
	}
	
	/* Parse the uncertainty information. 
	 * Use the respective Java API.
	 * https://github.com/52North/uncertml-api.git 
	 */
	private void readUncertainty(SBase sbase, Uncertainty uc){
		String id = null;
		String name = null;
		if (uc.isSetId()){
			id = uc.getId();
		}
		if (uc.isSetName()){
			name = uc.getName();
		}
		
		// XML node
		XMLNode uncertML = uc.getUncertML();
		//XMLParser ucParser = new XMLParser();
		
		// TODO: parse the uncertainty XML
					// Problems with the library
					// String xmlString = uncertML.toXMLString();
					// IUncertainty iuc = ucParser.parse(xmlString);
					// logger.info(iuc.toString());
		
		if (sbase instanceof NamedSBase){
			logger.info(String.format("UncertML <%s|%s> for %s: %s", name, id, ((NamedSBase) sbase).getId(), uncertML.toString()));
		} else {
			logger.info(String.format("UncertML <%s|%s>: %s", name, id, uncertML.toString()));
		}	
	}
	
	
	////////////////////////////////////////////////////////////////////////////
	// SBML LAYOUT
	////////////////////////////////////////////////////////////////////////////
	/** 
	 * Creates the layouts stored in the layout extension. 
	 * TODO: implement
	 */
	@SuppressWarnings("unused")
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
					AttributeUtil.copyNodeAttributes(network, sNode, node);	
				}
				
			} else {
				AttributeUtil.set(network, node, SBML.ATTR_ID, id, String.class);
				AttributeUtil.set(network, node, SBML.NODETYPE_ATTR, SBML.NODETYPE_LAYOUT_SPECIESGLYPH, String.class);
			}
		}
		
		// addReactionGlyphNodes();
		
		// addModelEdges();
		// addQualitativeModelEdges();
	}
	
	
	@Override
	public CyNetwork[] getNetworks() {
		if (mainNetwork == null){
			return new CyNetwork[] { network };	
		} else {
			return new CyNetwork[] { mainNetwork, network };
		}
	}

	
	/*
	 * (non-Javadoc)
	 * @see org.cytoscape.io.read.CyNetworkReader#buildCyNetworkView(org.cytoscape.model.CyNetwork)
	 */
	@Override
	public CyNetworkView buildCyNetworkView(final CyNetwork network) {
		logger.debug("buildCyNetworkView");
		
		// Preload SBML WebService information
		SBaseInfoThread.preloadAnnotationsForSBMLDocument(document);
				
		// Set SBML in SBMLManager 
		SBMLManager sbmlManager = SBMLManager.getInstance();
		
		// Look for already existing mappings (of read networks)
		One2ManyMapping<String, Long> mapping = sbmlManager.getMapping(network);
		mapping = IdNodeMap.fromSBMLNetwork(document, network, mapping);
		
		// existing mapping is updated
		sbmlManager.addSBML2NetworkEntry(document, network, mapping);
		// update the current network
		sbmlManager.updateCurrent(network);
		
		// Display the model information in the results pane
		ResultsPanel.getInstance().getTextPane().showSBaseInfo(document.getModel());	
		
		// Create the view
		CyNetworkView view = viewFactory.createNetworkView(network);
				
		logger.debug("network: " + network.toString());
		logger.debug("view: " + view.toString());
		return view;
	}	
	
	/**
	 * Read String from InputStream.
	 */
	public static String readString(InputStream source) throws IOException {
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

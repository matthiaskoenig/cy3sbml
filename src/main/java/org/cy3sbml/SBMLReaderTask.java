package org.cy3sbml;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.cy3sbml.mapping.CyIdSBaseMap;
import org.cytoscape.io.read.CyNetworkReader;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.property.CyProperty;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;

// SBML CORE
import org.sbml.jsbml.*;
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
// SBML_LAYOUT
import org.sbml.jsbml.ext.layout.Layout;
import org.sbml.jsbml.ext.layout.LayoutConstants;
import org.sbml.jsbml.ext.layout.LayoutModelPlugin;
import org.sbml.jsbml.ext.layout.SpeciesGlyph;

import org.cy3sbml.util.*;
import org.cy3sbml.layout.LayoutPreprocessor;
import org.cy3sbml.mapping.One2ManyMapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamException;


/**
 * The SBMLReaderTask creates CyNetworks from SBMLDocuments.
 * 
 * The reader creates the master SBML network graph with various subnetworks 
 * created from the full graph.
 */
public class SBMLReaderTask extends AbstractTask implements CyNetworkReader {	
	private static final Logger logger = LoggerFactory.getLogger(SBMLReaderTask.class);

	private String fileName;
	private final InputStream stream;
	private final CyNetworkFactory networkFactory;
	private final CyNetworkViewFactory viewFactory;
    private final VisualMappingManager visualMappingManager;
    private final CyLayoutAlgorithmManager cyLayoutAlgorithmManager;
	private final CyProperty<Properties> cy3sbmlProperties;

	private SBMLDocument document;

	private CyRootNetwork rootNetwork;
	private CyNetwork network;       // global network of all SBML information
	private CyNetwork coreNetwork;   // core reaction, species, (qualSpecies, qualTransitions), fbc network

	private Map<String, CyNode> nodeByCyId; // node dictionary
    private Boolean error = false;

    private TaskMonitor taskMonitor;

	/** Constructor */
	public SBMLReaderTask(InputStream stream, String fileName,
                          CyNetworkFactory networkFactory,
						  CyNetworkViewFactory viewFactory,
                          VisualMappingManager visualMappingManager,
                          CyLayoutAlgorithmManager cyLayoutAlgorithmManager,
                          CyProperty<Properties> cy3sbmlProperties) {
		
		this.stream = stream;
		this.fileName = fileName;
		this.networkFactory = networkFactory;
		this.viewFactory = viewFactory;
        this.visualMappingManager = visualMappingManager;
        this.cyLayoutAlgorithmManager = cyLayoutAlgorithmManager;
		this.cy3sbmlProperties = cy3sbmlProperties;
	}

	/** Testing constructor. */
	public SBMLReaderTask (InputStream stream, String fileName, CyNetworkFactory networkFactory){
	    this(stream, fileName, networkFactory, null, null, null, null);
    }


    /** Returns the error status for unit testing. */
    public Boolean getError(){
        return error;
    }

    /** Get created networks from the reader. */
    @Override
    public CyNetwork[] getNetworks() {
        if (coreNetwork == null){
            return new CyNetwork[] { network };
        } else {
            return new CyNetwork[] {coreNetwork, network };
        }
    }

    /**
     * Builds NetworkViews for given network.
     * The SBML is registered in the SBMLManager for the network.
     */
    @Override
    public CyNetworkView buildCyNetworkView(final CyNetwork network) {
        logger.debug("buildCyNetworkView");

        // Set SBML in SBMLManager
        SBMLManager sbmlManager = SBMLManager.getInstance();

        // SBMLManager is only available in the OSGI context
        if (sbmlManager != null) {

            // get existing mappings (of read networks)
            One2ManyMapping<String, Long> mapping = sbmlManager.getMapping(network);
            mapping = mappingFromNetwork(network, mapping);

            // existing mapping is updated
            sbmlManager.addSBMLForNetwork(document, network, mapping);
            // update the current network
            sbmlManager.updateCurrent(network);
        } else {
            logger.warn("No mapping found for SBML network.");
        }

        // Create view
        CyNetworkView view = viewFactory.createNetworkView(network);

        // Set style
        // VisualMappingManager only available in OSGI context
        if (visualMappingManager != null) {
            String styleName = (String) cy3sbmlProperties.getProperties().get(SBML.PROPERTY_VISUAL_STYLE);
            VisualStyle style = SBMLStyleManager.getVisualStyleByName(visualMappingManager, styleName);
            if (style != null) {
                visualMappingManager.setVisualStyle(style, view);
            }
        }

        // layout
		if (cyLayoutAlgorithmManager != null) {
			CyLayoutAlgorithm layout = cyLayoutAlgorithmManager.getLayout(SBML.SBML_LAYOUT);
			if (layout == null) {
				layout = cyLayoutAlgorithmManager.getLayout(CyLayoutAlgorithmManager.DEFAULT_LAYOUT_NAME);
				logger.warn(String.format("'{}' layout not found; will use the default one.", SBML.SBML_LAYOUT));
			}
			TaskIterator itr = layout.createTaskIterator(view, layout.getDefaultLayoutContext(), CyLayoutAlgorithm.ALL_NODE_VIEWS, "");
			Task nextTask = itr.next();
			try {
				nextTask.run(taskMonitor);
			} catch (Exception e) {
				throw new RuntimeException("Could not finish layout", e);
			}
		}

        // finished
        return view;
    }

    /**
     * Create mapping between cyIds and cytoscape nodes.
     *
     * The mapping between CyNetwork elements and SBML elements uses
     * the unique SUIDs of CyNodes and unique cyIds of SBase SBML elements.
     */
    public static One2ManyMapping<String, Long> mappingFromNetwork(CyNetwork network, One2ManyMapping<String, Long> mapping){
        if (mapping == null){
            mapping = new One2ManyMapping<>();
        }
        List<CyNode> nodes = network.getNodeList();
        for (CyNode node : nodes){
            CyRow attributes = network.getRow(node);
            String cyId = attributes.get(SBML.ATTR_CYID, String.class);
            mapping.put(cyId, node.getSUID());
        }
        return mapping;
    }

    /** Cancel task. */
    @Override
    public void cancel() {}


	/** Parse SBML networks. */
	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {
		logger.debug("<--- Start Reader --->");
        this.taskMonitor = taskMonitor;
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
			String xml = IOUtil.inputStream2String(stream);
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
			nodeByCyId = new HashMap<>();
			
			// To create a new CySubNetwork with the same CyNetwork's CyRootNetwork, cast your CyNetwork to
			// CySubNetwork and call the CySubNetwork.getRootNetwork() method:
			// 		CyRootNetwork rootNetwork = ((CySubNetwork)network).getRootNetwork(); 
			// CyRootNetwork also provides methods to create and add new subnetworks (see CyRootNetwork.addSubNetwork()). 
			rootNetwork = ((CySubNetwork) network).getRootNetwork();

            //////////////////////////////////////////////////////////////////

			// <core>
			readCore(model);
			if (taskMonitor != null){
				taskMonitor.setProgress(0.4);
			}

            // <qual>
			QualModelPlugin qualModel = (QualModelPlugin) model.getExtension(QualConstants.namespaceURI); 
			if (qualModel != null){
				readQual(model, qualModel);
			}

			// <fbc>
			FBCModelPlugin fbcModel = (FBCModelPlugin) model.getExtension(FBCConstants.namespaceURI);
			if (fbcModel != null){
				readFBC(model, fbcModel);
			}

			// <comp>
			CompModelPlugin compModel = (CompModelPlugin) model.getExtension(CompConstants.namespaceURI);
			if (compModel != null){
				readComp(model, compModel);
			}

			// <groups>
			GroupsModelPlugin groupsModel = (GroupsModelPlugin) model.getExtension(GroupsConstants.namespaceURI);
			if (groupsModel != null){
                readGroups(model, groupsModel);
			}

			// <layout>
			LayoutModelPlugin layoutModel = (LayoutModelPlugin) model.getExtension(LayoutConstants.namespaceURI);
			if (layoutModel != null){
			    readLayouts(model, qualModel, layoutModel);
			}

			// Add compartment codes dynamically for colors
			addCompartmentCodes(network, model);
            addSBMLTypesExtended(network, model);
            addSBMLInteractionExtended(network);
						
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

			// O(1) lookup (collect nodes and edges)
			HashSet<String> nodeTypesSet = new HashSet<>(java.util.Arrays.asList(nodeTypes));
			HashSet<CyNode> coreNodes = new HashSet<>();
			for (CyNode n : network.getNodeList()){
				CyRow row = network.getRow(n, CyNetwork.DEFAULT_ATTRS);
				String type = row.get(SBML.NODETYPE_ATTR, String.class);
				if (nodeTypesSet.contains(type)){
					coreNodes.add(n);
				}
			}

            HashSet<String> edgeTypesSet = new HashSet<>(java.util.Arrays.asList(edgeTypes));
			HashSet<CyEdge> coreEdges = new HashSet<>();
			for (CyEdge e : network.getEdgeList()){
				CyRow row = network.getRow(e, CyNetwork.DEFAULT_ATTRS);
				String type = row.get(SBML.INTERACTION_ATTR, String.class);
				if (edgeTypesSet.contains(type)){
					coreEdges.add(e);
				}
			}

            // Set naming
			String name = getNetworkName();
            rootNetwork.getRow(rootNetwork).set(CyNetwork.NAME, String.format("%s", name));
            network.getRow(network).set(CyNetwork.NAME, String.format("All: %s", name));

			// Create core subnetwork if any node in main network
			if (coreNodes.size() > 0){
				coreNetwork = rootNetwork.addSubNetwork(coreNodes, coreEdges);
				coreNetwork.getRow(coreNetwork).set(CyNetwork.NAME, String.format("Core: %s", name));
			}

			if (taskMonitor != null){
				taskMonitor.setProgress(0.8);
			}
			logger.debug("<--- End Reader --->");
			
		
		} catch (Throwable t){
			logger.error("Could not read SBML into Cytoscape!", t);
			error = true;
			t.printStackTrace();
			throw new SBMLReaderError("cy3sbml reader failed to build a SBML model. " +
					"Please validate the file in the online SBML validator at 'http://www.sbml.org/validator/'" +
                    "and report the issue at 'https://github.com/matthiaskoenig/cy3sbml/issues'" + t);
		}
	}

	/**
     * Get network name.
     * Is used for naming the network and the network collection.
     */
	private String getNetworkName(){
       // name of root network
        String name = network.getRow(network).get(SBML.ATTR_NAME, String.class);
        if (name == null){
            // name not set, try backup name via id
            name = network.getRow(network).get(SBML.ATTR_ID, String.class);
            // still not set, use the file name
            if (name == null){
            	String[] tokens = fileName.split(File.separator);
                name = tokens[tokens.length-1];
            }
        }
        return name;
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
		logger.debug("<core>");

        // SBMLDocument & Model //
		// Mark network as SBML
		AttributeUtil.set(network, network, SBML.NETWORKTYPE_ATTR, SBML.NETWORKTYPE_SBML, String.class);
		AttributeUtil.set(network, network, SBML.LEVEL_VERSION, String.format("L%1$s V%2$s", document.getLevel(), document.getVersion()), String.class);

        // metaId, SBO, id, name
		setNamedSBaseAttributes(network, model);
		
		// Model attributes
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

        // UnitDefinition //
        for (UnitDefinition ud: model.getListOfUnitDefinitions()){
            createUnitDefinitionGraph(ud);
        }
		
		// FunctionDefinition //
		for (FunctionDefinition fd : model.getListOfFunctionDefinitions()){

		    // implements both interfaces
            CyNode n = createNode(fd.getId(), SBML.NODETYPE_FUNCTION_DEFINITION);
            setNamedSBaseAttributes(n, fd);
            setAbstractMathContainerNodeAttributes(n, fd);

			// Do not create the math network for the function definition
			// The objects of the FunctionDefinition ASTNode can have different naming conventions
			// than the objects, i.e. a lambda(x), does not mean that it is called with
			// an object x
			// createMathNetwork(fd, fdNode, SBML.INTERACTION_REFERENCE_FUNCTIONDEFINITION);
		}

		// Compartment //
		for (Compartment compartment : model.getListOfCompartments()) {
		    CyNode n = createNode(compartment.getId(), SBML.NODETYPE_COMPARTMENT);
            setSymbolNodeAttributes(n, compartment);
            // edge to unit
            createUnitEdge(n, compartment);

			if (compartment.isSetSpatialDimensions()){
				AttributeUtil.set(network, n, SBML.ATTR_SPATIAL_DIMENSIONS, compartment.getSpatialDimensions(), Double.class);
			}
			if (compartment.isSetSize()){
				AttributeUtil.set(network, n, SBML.ATTR_SIZE, compartment.getSize(), Double.class);
			}
		}
		
		// Parameter //
		for (Parameter parameter : model.getListOfParameters()) {
		    CyNode n = createNode(parameter.getId(), SBML.NODETYPE_PARAMETER);
            setSymbolNodeAttributes(n, parameter);
            // edge to unit
            createUnitEdge(n, parameter);
		}

		// Species //
		for (Species species : model.getListOfSpecies()) {
		    CyNode n = createNode(species.getId(), SBML.NODETYPE_SPECIES);
			setSymbolNodeAttributes(n, species);
            // edge to unit
            createUnitEdge(n, species);

            // edge to compartment
			if (species.isSetCompartment()){
				AttributeUtil.set(network, n, SBML.ATTR_COMPARTMENT, species.getCompartment(), String.class);
				CyNode comp = nodeByCyId.get(species.getCompartment());
				if (comp != null){
				    createEdge(n, comp, SBML.INTERACTION_SPECIES_COMPARTMENT);
				} else {
					logger.error(String.format("Compartment does not exist for species: %s for %s", species.getCompartment(), species.getId()));
				}
			}
			if (species.isSetBoundaryCondition()){
				AttributeUtil.set(network, n, SBML.ATTR_BOUNDARY_CONDITION, species.getBoundaryCondition(), Boolean.class);
			}
			if (species.isSetHasOnlySubstanceUnits()){
				AttributeUtil.set(network, n, SBML.ATTR_HAS_ONLY_SUBSTANCE_UNITS, species.getHasOnlySubstanceUnits(), Boolean.class);
			}
			if (species.isSetCharge()){
				AttributeUtil.set(network, n, SBML.ATTR_CHARGE, species.getCharge(), Integer.class);
			}
			if (species.isSetConversionFactor()){
				AttributeUtil.set(network, n, SBML.ATTR_CONVERSION_FACTOR, species.getConversionFactor(), String.class);
			}
			if (species.isSetSubstanceUnits()){
				AttributeUtil.set(network, n, SBML.ATTR_SUBSTANCE_UNITS, species.getSubstanceUnits(), String.class);
			}
			if (species.isSetInitialAmount()){
				AttributeUtil.set(network, n, SBML.ATTR_INITIAL_AMOUNT, species.getInitialAmount(), Double.class);
			}
			if (species.isSetInitialConcentration()){
				AttributeUtil.set(network, n, SBML.ATTR_INITIAL_CONCENTRATION, species.getInitialConcentration(), Double.class);
			}
		}	
		
		// Reaction //
		for (Reaction reaction : model.getListOfReactions()) {
		    CyNode n = createNode(reaction.getId(), SBML.NODETYPE_REACTION);
            setNamedSBaseWithDerivedUnitAttributes(n, reaction);

			if (reaction.isSetReversible()){
				AttributeUtil.set(network, n, SBML.ATTR_REVERSIBLE, reaction.getReversible(), Boolean.class);
			} else {
				// reversible=true by default
				AttributeUtil.set(network, n, SBML.ATTR_REVERSIBLE, true, Boolean.class);
			}
			if (reaction.isSetFast()){
				AttributeUtil.set(network, n, SBML.ATTR_FAST, reaction.getFast(), Boolean.class);
			}
            if (reaction.isSetCompartment()){
                AttributeUtil.set(network, n, SBML.ATTR_COMPARTMENT, reaction.getCompartment(), String.class);
                CyNode comp = nodeByCyId.get(reaction.getCompartment());
                if (comp != null){
                    // edge to compartment
                    createEdge(n, comp, SBML.INTERACTION_REACTION_COMPARTMENT);
                } else {
                    logger.error(String.format("Compartment does not exist for reaction: %s for %s",
                            reaction.getCompartment(), reaction.getId()));
                }
            }
		
			// Reactants
			for (SpeciesReference speciesRef : reaction.getListOfReactants()) {
				CyNode reactant = nodeByCyId.get(speciesRef.getSpecies());
				if (reactant != null){
				    CyEdge edge = createEdge(n, reactant, SBML.INTERACTION_REACTION_REACTANT);
					setSBaseAttributes(edge, speciesRef);

                    Double stoichiometry = (speciesRef.isSetStoichiometry()) ? speciesRef.getStoichiometry() : 1.0;
					AttributeUtil.set(network, edge, SBML.ATTR_STOICHIOMETRY, stoichiometry, Double.class);
				} else {
					logger.error(String.format("Reactant does not exist for reaction: %s for %s",
                            speciesRef.getSpecies(), reaction.getId()));
				}
			}
			// Products
			for (SpeciesReference speciesRef : reaction.getListOfProducts()) {
				CyNode product = nodeByCyId.get(speciesRef.getSpecies());
				if (product != null){
				    CyEdge edge = createEdge(n, product, SBML.INTERACTION_REACTION_PRODUCT);
					setSBaseAttributes(edge, speciesRef);

                    Double stoichiometry = (speciesRef.isSetStoichiometry()) ? speciesRef.getStoichiometry() : 1.0;
					AttributeUtil.set(network, edge, SBML.ATTR_STOICHIOMETRY, stoichiometry, Double.class);
				} else {
					logger.error(String.format("Product does not exist for reaction: %s for %s",
                            speciesRef.getSpecies(), reaction.getId()));
				}
			}
			// Modifiers
			for (ModifierSpeciesReference msRef : reaction.getListOfModifiers()) {
				CyNode modifier = nodeByCyId.get(msRef.getSpecies());
				if (modifier != null){
				    CyEdge edge = createEdge(n, modifier, SBML.INTERACTION_REACTION_MODIFIER);
					setSBaseAttributes(edge, msRef);
				} else {
					logger.error(String.format("ModifierSpecies does not exist for reaction: %s for %s",
                            msRef.getSpecies(), reaction.getId()));
				}
			}
			
			// Kinetic law
			if (reaction.isSetKineticLaw()){
				KineticLaw law = reaction.getKineticLaw();
				String lawCyId = CyIdSBaseMap.kineticLawCyId(reaction);
                CyNode lawNode = createNode(lawCyId, SBML.NODETYPE_KINETIC_LAW);
                setAbstractMathContainerNodeAttributes(lawNode, law);
				
				// edge to reaction
				CyEdge edge = network.addEdge(n, lawNode, true);
				AttributeUtil.set(network, edge, SBML.INTERACTION_ATTR, SBML.INTERACTION_REACTION_KINETICLAW, String.class);
				
				// local parameter nodes
				if (law.isSetListOfLocalParameters()){
					for (LocalParameter lp: law.getListOfLocalParameters()){
					    String cyId = CyIdSBaseMap.localParameterCyId(reaction, lp);
                        // This changes the SBMLDocument !
                        // but only reliable way to handle LocalParameters in math networks
                        lp.setId(cyId);

                        CyNode lpNode = createNode(cyId, SBML.NODETYPE_LOCAL_PARAMTER);
						setQuantityWithUnitAttributes(lpNode, lp);
                        // edge to unit
                        createUnitEdge(lpNode, lp);

						// edge to reaction
                        createEdge(lpNode, lawNode, SBML.INTERACTION_LOCALPARAMETER_KINETICLAW);
					}
				}
				
				// referenced nodes in math
                if (law.isSetMath()){
                    // set math on reaction
                    AttributeUtil.set(network, n, SBML.ATTR_KINETIC_LAW, law.getMath().toFormula(), String.class);
                    createMathNetwork(law, lawNode, SBML.INTERACTION_REFERENCE_KINETICLAW);
                } else {
                    logger.warn(String.format("No math set for kinetic law in reaction: %s", reaction.getId()));
                }
			}
		}
		
		// InitialAssignment //
		for (InitialAssignment assignment : model.getListOfInitialAssignments()){
			String variable = assignment.getVariable();
			CyNode targetNode = nodeByCyId.get(variable);
			if (targetNode != null){
				
				String cyId = CyIdSBaseMap.initialAssignmentCyId(variable);
			 	CyNode assignmentNode = createNode(cyId, SBML.NODETYPE_INITIAL_ASSIGNMENT);
                setAbstractMathContainerNodeAttributes(assignmentNode, assignment);

				// edge to variable 
				CyNode variableNode = nodeByCyId.get(variable);
				if (variableNode != null){
				    createEdge(targetNode, assignmentNode, SBML.INTERACTION_VARIABLE_INITIAL_ASSIGNMENT);
				} else {
					logger.warn(String.format("Variable is neither Compartment, Species or Parameter, probably SpeciesReference: %s in %s", variable, cyId ));
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
		
		// Rule //
        ListOf<Rule> rules = model.getListOfRules();
		for (int k=0; k<rules.size(); k++){
		    Rule rule = rules.get(k);

            String cyId = null;
            String ruleType = null;
            String variable = null;

            if (rule instanceof AlgebraicRule){
                cyId = "algebraicRule_" + k;
                ruleType = SBML.NODETYPE_ALGEBRAIC_RULE;
            } else {
                variable = SBMLUtil.getVariableFromRule(rule);
                cyId = CyIdSBaseMap.ruleCyId(variable);
                if (rule instanceof AssignmentRule){
                    ruleType = SBML.NODETYPE_ASSIGNMENT_RULE;
                } else if (rule instanceof RateRule){
                    ruleType = SBML.NODETYPE_RATE_RULE;
                }
            }
            CyNode n = createNode(cyId, ruleType);
            setAbstractMathContainerNodeAttributes(n, rule);
            // referenced nodes in math
            createMathNetwork(rule, n, SBML.INTERACTION_REFERENCE_RULE);

            // edge to variable for rateRule and assignmentRule
			if (variable != null) {
                CyNode variableNode = nodeByCyId.get(variable);
                if (variableNode != null) {
                    createEdge(variableNode, n, SBML.INTERACTION_VARIABLE_RULE);
                } else {
                    /*  An assignment rule can refer to the identifier of a Species, SpeciesReference,
                        Compartment, or global Parameter object in the model
                        The case SpeciesReference is not handled !
                    */
                    logger.warn(String.format("Variable is neither Compartment, Species or Parameter, probably SpeciesReference: %s in %s",
                            variable, rule));
                }
            }
		}

		// Constraints
        // No models with constraints exist for testing.
        ListOf<Constraint> constraints = model.getListOfConstraints();
        for (int k=0; k<constraints.size(); k++){
            Constraint constraint = constraints.get(k);
		    String cyId = CyIdSBaseMap.constraintCyId(k);
            CyNode n = createNode(cyId, SBML.NODETYPE_CONSTRAINT);
            setAbstractMathContainerNodeAttributes(n, constraint);
            if (constraint.isSetMessage()){
                try {
                    AttributeUtil.set(network, n, SBML.ATTR_MESSAGE,
                            constraint.getMessageString(), String.class);
                }catch (XMLStreamException e) {
                    logger.error("Message string could not be created for constraint.", e);
                    e.printStackTrace();
                }
            }
        }
		
		// Events
        ListOf<Event> events = model.getListOfEvents();
		for (int k=0; k<events.size(); k++){
		    Event event = events.get(k);
            String cyId = CyIdSBaseMap.eventCyId(k);
            CyNode n = createNode(cyId, SBML.NODETYPE_EVENT);

            setNamedSBaseWithDerivedUnitAttributes(n, event);

            if (event.isSetUseValuesFromTriggerTime()){
                AttributeUtil.set(network, n, SBML.ATTR_USE_VALUES_FROM_TRIGGER_TIME,
                        event.getUseValuesFromTriggerTime(), Boolean.class);
            }

            // TODO: implement
            /*
            for (EventAssignment ea: event.getListOfEventAssignments()){
                String eaCyId =
                CyNode eaNode = createNode(cyId, SBML.NODETYPE_EVENT_ASSIGNMENT);
                setAbstractMathContainerNodeAttributes(eaNode, ea);

                // edge to event

                // edge to variable
                if (ea.isSetVariable()){
                    String variable = ea.getVariable();
                    CyNode variableNode = nodeByCyId.get(variable);
                    if (variableNode != null) {
                        createEdge(variableNode, eaNode, SBML.INTERACTION_VARIABLE_EVENT_ASSIGNMENT);
                    } else {
                        //  An assignment rule can refer to the identifier of a Species, SpeciesReference,
                        //  Compartment, or global Parameter object in the model
                        //  The case SpeciesReference is not handled !

                        logger.warn(String.format("Variable is neither Compartment, Species or Parameter, probably SpeciesReference: %s in %s",
                                variable, ea));
                    }
                } else {
                    logger.error("Variable not set in EventAssignment: " + ea);
                }
                // referenced nodes in math
                createMathNetwork(ea, eaNode, SBML.INTERACTION_REFERENCE_EVENT_ASSIGNMENT);
            }
            */

        }
	}
		
	////////////////////////////////////////////////////////////////////////////
	// SBML QUAL
	////////////////////////////////////////////////////////////////////////////
	/**
	 * Create nodes, edges and attributes from Qualitative Model.
	 * @param qModel
	 */
	private void readQual(Model model, QualModelPlugin qModel){
		logger.debug("<qual>");

        // QualSpecies //
        for (QualitativeSpecies qSpecies : qModel.getListOfQualitativeSpecies()){
            CyNode n = createNode(qSpecies.getId(), SBML.NODETYPE_QUAL_SPECIES);
            setNamedSBaseAttributes(n, qSpecies);
            
            if (qSpecies.isSetCompartment()){
                AttributeUtil.set(network, n, SBML.ATTR_COMPARTMENT, qSpecies.getCompartment(), String.class);
                // edge to compartment
                CyNode comp = nodeByCyId.get(qSpecies.getCompartment());
                createEdge(n, comp, SBML.INTERACTION_SPECIES_COMPARTMENT);
            }
            if (qSpecies.isSetConstant()){
                AttributeUtil.set(network, n, SBML.ATTR_CONSTANT, qSpecies.getConstant(), Boolean.class);
            }
            if (qSpecies.isSetInitialLevel()){
                AttributeUtil.set(network, n, SBML.ATTR_QUAL_INITIAL_LEVEL, qSpecies.getInitialLevel(), Integer.class);
            }
            if (qSpecies.isSetMaxLevel()){
                AttributeUtil.set(network, n, SBML.ATTR_QUAL_MAX_LEVEL, qSpecies.getMaxLevel(), Integer.class);
            }
		}
		// QualTransitions
		for (Transition transition : qModel.getListOfTransitions()){
			CyNode n = createNode(transition.getId(), SBML.NODETYPE_QUAL_TRANSITION);
            setNamedSBaseAttributes(n, transition);

			// Inputs
			for (Input input : transition.getListOfInputs()) {
				CyNode inNode = nodeByCyId.get(input.getQualitativeSpecies());
                CyEdge e = createEdge(n, inNode, SBML.INTERACTION_QUAL_TRANSITION_INPUT);


				// required (no checking of required -> NullPointerException risk)
				AttributeUtil.set(network, e, SBML.ATTR_QUAL_TRANSITION_EFFECT, input.getTransitionEffect().toString(), String.class);
				AttributeUtil.set(network, e, SBML.ATTR_QUAL_QUALITATIVE_SPECIES, input.getQualitativeSpecies().toString(), String.class);
				// optional
				if (input.isSetId()){
					AttributeUtil.set(network, e, SBML.ATTR_ID, input.getId(), String.class);
				}
				if (input.isSetName()){
					AttributeUtil.set(network, e, SBML.ATTR_NAME, input.getName(), String.class);
				}
				if (input.isSetSign()){
					AttributeUtil.set(network, e, SBML.ATTR_QUAL_SIGN, input.getSign().toString(), String.class);
				}
				if (input.isSetSBOTerm()){
					AttributeUtil.set(network, e, SBML.ATTR_SBOTERM, input.getSBOTermID(), String.class);
				}
				if (input.isSetMetaId()){
					AttributeUtil.set(network, e, SBML.ATTR_METAID, input.getMetaId(), String.class);
				}
				if (input.isSetThresholdLevel()){
					AttributeUtil.set(network, e, SBML.ATTR_QUAL_THRESHOLD_LEVEL, input.getThresholdLevel(), Integer.class);
				}
			}

			// Outputs
			for (Output output : transition.getListOfOutputs()) {
				CyNode outNode = nodeByCyId.get(output.getQualitativeSpecies());
				CyEdge e = createEdge(n, outNode, SBML.INTERACTION_QUAL_TRANSITION_OUTPUT);

				// required
				AttributeUtil.set(network, e, SBML.ATTR_QUAL_QUALITATIVE_SPECIES, output.getQualitativeSpecies().toString(), String.class);
				AttributeUtil.set(network, e, SBML.ATTR_QUAL_TRANSITION_EFFECT, output.getTransitionEffect().toString(), String.class);
				// optional
				if (output.isSetId()){
					AttributeUtil.set(network, e, SBML.ATTR_ID, output.getId(), String.class);
				}
				if (output.isSetName()){
					AttributeUtil.set(network, e, SBML.ATTR_NAME, output.getName(), String.class);
				}
				if (output.isSetSBOTerm()){
					AttributeUtil.set(network, e, SBML.ATTR_SBOTERM, output.getSBOTermID(), String.class);
				}
				if (output.isSetMetaId()){
					AttributeUtil.set(network, e, SBML.ATTR_METAID, output.getMetaId(), String.class);
				}
				if (output.isSetOutputLevel()){
					AttributeUtil.set(network, e, SBML.ATTR_QUAL_OUTPUT_LEVEL, output.getOutputLevel(), Integer.class);
				}
			}
			
			// parse the default term / function terms
			if (transition.isSetListOfFunctionTerms()){
				List<Integer> resultLevels = new LinkedList<Integer>();
				for (FunctionTerm term: transition.getListOfFunctionTerms()){
					resultLevels.add(term.getResultLevel());
				}
				AttributeUtil.setList(network, n, SBML.ATTR_QUAL_RESULT_LEVELS, resultLevels, Integer.class);
			}
		}
	}
	
	////////////////////////////////////////////////////////////////////////////
	// SBML FBC
	////////////////////////////////////////////////////////////////////////////
	/** Creates network information from fbc model. */
	private void readFBC(Model model, FBCModelPlugin fbcModel){
		logger.debug("<fbc>");

		// Model attributes
		if (fbcModel.isSetStrict()){
			AttributeUtil.set(network, network, SBML.ATTR_FBC_STRICT, fbcModel.getStrict(), Boolean.class);
		}
		
		// Species attributes
		for (Species species: model.getListOfSpecies()){
			FBCSpeciesPlugin fbcSpecies = (FBCSpeciesPlugin) species.getExtension(FBCConstants.namespaceURI);
			if (fbcSpecies != null){
                CyNode n = nodeByCyId.get(species.getId());
                // optional
                if (fbcSpecies.isSetCharge()) {
                    AttributeUtil.set(network, n, SBML.ATTR_FBC_CHARGE, fbcSpecies.getCharge(), Integer.class);
                }
                if (fbcSpecies.isSetChemicalFormula()) {
                    AttributeUtil.set(network, n, SBML.ATTR_FBC_CHEMICAL_FORMULA, fbcSpecies.getChemicalFormula(), String.class);
                }
            }
		}
		
		// List of flux objectives (handled via reaction attributes)
		// (activeObjective is not parsed)
		for (Objective objective : fbcModel.getListOfObjectives()){
			// one reaction attribute column per objective
			String key = String.format(SBML.ATTR_FBC_OBJECTIVE_TEMPLATE, objective.getId());
			for (FluxObjective fluxObjective : objective.getListOfFluxObjectives()){
				String reactionId = fluxObjective.getReaction();
				CyNode node = nodeByCyId.get(reactionId);
				AttributeUtil.set(network, node, key, fluxObjective.getCoefficient(), Double.class);
			}
		}
		
		// GeneProducts as nodes
		for (GeneProduct geneProduct : fbcModel.getListOfGeneProducts()){
		    CyNode n = createNode(geneProduct.getId(), SBML.NODETYPE_FBC_GENEPRODUCT);
            setNamedSBaseAttributes(n, geneProduct);

			// Overwrite label
            if (geneProduct.isSetLabel()){
                AttributeUtil.set(network, n, SBML.LABEL, geneProduct.getLabel(), String.class);
            }

			// edge to associated species
			if (geneProduct.isSetAssociatedSpecies()){
				CyNode speciesNode = nodeByCyId.get(geneProduct.getAssociatedSpecies());
                createEdge(speciesNode, n, SBML.INTERACTION_FBC_GENEPRODUCT_SPECIES);
			}
		}
		
		// Reaction attributes
		for (Reaction reaction: model.getListOfReactions()){
			FBCReactionPlugin fbcReaction = (FBCReactionPlugin) reaction.getExtension(FBCConstants.namespaceURI);
			
			if (fbcReaction != null){
                // optional bounds
                CyNode node = nodeByCyId.get(reaction.getId());
                if (fbcReaction.isSetLowerFluxBound()){
                    AttributeUtil.set(network, node, SBML.ATTR_FBC_LOWER_FLUX_BOUND, fbcReaction.getLowerFluxBound(), String.class);
                    // add edge
                    CyNode parameterNode = nodeByCyId.get(fbcReaction.getLowerFluxBound());
                    CyEdge edge = network.addEdge(parameterNode, node, true);
                    AttributeUtil.set(network, edge, SBML.INTERACTION_ATTR, SBML.INTERACTION_PARAMETER_REACTION, String.class);
                }
                if (fbcReaction.isSetUpperFluxBound()){
                    AttributeUtil.set(network, node, SBML.ATTR_FBC_UPPER_FLUX_BOUND, fbcReaction.getUpperFluxBound(), String.class);
                    // add edge
                    CyNode parameterNode = nodeByCyId.get(fbcReaction.getUpperFluxBound());
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
				CyNode n = nodeByCyId.get(reactionId);
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

	/**
     * Recursive function for processing the Associations.
     * FIXME: unnecessary code duplication
     */
	private void processAssociation(CyNode parentNode, String parentType, Association association){
		// GeneProductRef
		if (association.getClass().equals(GeneProductRef.class)){
			GeneProductRef gpRef = (GeneProductRef) association;
			CyNode gpNode = nodeByCyId.get(gpRef.getGeneProduct());
			if (gpNode != null){
				if (parentType.equals(SBML.NODETYPE_REACTION)){
				    createEdge(gpNode, parentNode, SBML.INTERACTION_FBC_ASSOCIATION_REACTION);
				} else {
                    createEdge(gpNode, parentNode, SBML.INTERACTION_FBC_ASSOCIATION_ASSOCIATION);
				}
			} else {
				logger.error(String.format("GeneProduct does not exist for GeneAssociation: %s in %s",
                        gpRef.getGeneProduct(), association));
			}
		}
		// And
		else if (association.getClass().equals(And.class)){
			And andRef = (And) association;
			
			// Create and node & edge
			CyNode andNode = network.addNode();
			AttributeUtil.set(network, andNode, SBML.LABEL, "AND", String.class);
			AttributeUtil.set(network, andNode, SBML.NODETYPE_ATTR, SBML.NODETYPE_FBC_AND, String.class);
			if (parentType.equals(SBML.NODETYPE_REACTION)){
			    createEdge(andNode, parentNode, SBML.INTERACTION_FBC_ASSOCIATION_REACTION);
			} else {
                createEdge(andNode, parentNode, SBML.INTERACTION_FBC_ASSOCIATION_ASSOCIATION);
			}
			// recursive association children
			for (Association a : andRef.getListOfAssociations()){
				processAssociation(andNode, SBML.NODETYPE_FBC_AND, a);
			}
		}
		// or
		else if (association.getClass().equals(Or.class)){
			Or orRef = (Or) association;
			
			// Create and node & edge
			CyNode orNode = network.addNode();
			AttributeUtil.set(network, orNode, SBML.LABEL, "OR", String.class);
			AttributeUtil.set(network, orNode, SBML.NODETYPE_ATTR, SBML.NODETYPE_FBC_OR, String.class);
			if (parentType.equals(SBML.NODETYPE_REACTION)){
			    createEdge(orNode, parentNode, SBML.INTERACTION_FBC_ASSOCIATION_REACTION);
			} else {
                createEdge(orNode, parentNode, SBML.INTERACTION_FBC_ASSOCIATION_ASSOCIATION);
			}
			
			// recursive association children
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
		logger.debug("<comp>");

		// TODO: model ListOfSubmodels
		
		// Port //
		for (Port port : compModel.getListOfPorts()) {
		    CyNode n = createNode(port.getId(), SBML.NODETYPE_COMP_PORT);
            setNamedSBaseAttributes(n, port);

			if (port.isSetPortRef()){
				AttributeUtil.set(network, n, SBML.ATTR_COMP_PORTREF, port.getPortRef(), String.class);
			}
			if (port.isSetIdRef()){
				String idRef = port.getIdRef();
				AttributeUtil.set(network, n, SBML.ATTR_COMP_IDREF, idRef, String.class);
				// add edge
				CyNode portNode = nodeByCyId.get(idRef);
                if (port != null){
                    createEdge(n, portNode, SBML.INTERACTION_COMP_PORT_ID);
                } else {
                    // for instance referring to submodel (not part of master network yet)
                    logger.warn("No target found for port with idRef: ", idRef);
                }
            }

			if (port.isSetUnitRef()){
				AttributeUtil.set(network, n, SBML.ATTR_COMP_UNITREF, port.getUnitRef(), String.class);
			}
			if (port.isSetMetaIdRef()){
				AttributeUtil.set(network, n, SBML.ATTR_COMP_METAIDREF, port.getMetaIdRef(), String.class);
			}
		}
		
		// TODO: SBase replacedBy & listOfReplacedElements & listOfDeletions
		
	}

	////////////////////////////////////////////////////////////////////////////
	// SBML GROUPS
	////////////////////////////////////////////////////////////////////////////
	/**
	 * Creates groups.
	 * TODO: implement
	 */
	private void readGroups(Model model, GroupsModelPlugin groupsModel){
		logger.debug("<groups>");
        logger.debug("\tgroups model found, but not supported");
	}

	////////////////////////////////////////////////////////////////////////////
	// SBML SBML_LAYOUT
	////////////////////////////////////////////////////////////////////////////
	/** 
	 * Creates the layouts stored in the layout extension. 
	 * TODO: implement
	 */
	private void readLayouts(Model model, QualModelPlugin qualModel, LayoutModelPlugin layoutModel){
		logger.debug("<layout>");
        logger.info("\tlayout model found, but not supported");

		for (Layout layout : layoutModel.getListOfLayouts()){
			// layoutNetwork = rootNetwork.addSubNetwork();
			// readLayout(model, qualModel, layout);
		}
	}
	
	/** Read single layout. */
	private void readLayout(Model model, QualModelPlugin qualModel, Layout layout){

		// Process layouts (Generate full id set and all edges for elements)
		LayoutPreprocessor preprocessor = new LayoutPreprocessor(model, qualModel, layout);
		layout = preprocessor.getProcessedLayout();
		
		// now generate nodes and edges
		// TODO: AttributeUtil.set(layoutNetwork, layoutNetwork, SBML.NETWORKTYPE_ATTR, SBML.NETWORKTYPE_LAYOUT, String.class);
		
		// addSpeciesGlyphNodes
		for (SpeciesGlyph glyph : layout.getListOfSpeciesGlyphs()) {
			// create the node
			String id = glyph.getId();
			// creates node in network
            CyNode n = createNode(glyph.getId(), SBML.NODETYPE_LAYOUT_SPECIESGLYPH);
            setNamedSBaseAttributes(n, glyph);
			
			// get species node and copyInputStream information
			if (glyph.isSetSpecies()){
				String speciesId = glyph.getSpecies();
				if (nodeByCyId.containsKey(speciesId)){
					CyNode sNode = nodeByCyId.get(speciesId);
					
					// copyInputStream node attributes from species node to speciesGlyph node
					AttributeUtil.copyNodeAttributes(network, sNode, n);
				}
				
			}
		}
		// addReactionGlyphNodes();
		// addModelEdges();
		// addQualitativeModelEdges();
	}

    ////////////////////////////////////////////////////////////////////////////
    // HELPER FUNCTIONS
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Creates SBML node.
     * Only function to create node in the network. The node is stored
     * in the node mapping and the corresponding node attribute is set.
     *
     * @param cyId unique id for the SBase/Object
     * @param sbmlType SBML type of the node
     * @return
     */
	private CyNode createNode(String cyId, String sbmlType){
        CyNode n = network.addNode();
        AttributeUtil.set(network, n, SBML.ATTR_CYID, cyId, String.class);
        AttributeUtil.set(network, n, SBML.NODETYPE_ATTR, sbmlType, String.class);
        AttributeUtil.set(network, n, SBML.LABEL, cyId, String.class);
        nodeByCyId.put(cyId, n);
        return n;
    }

    /**
     * Creates SBML edge.
     * Only function to create edges in the network.
     * All edges are directed.
     *
     * @param source
     * @param target
     * @param interactionType
     * @return
     */
    private CyEdge createEdge(CyNode source, CyNode target, String interactionType){
        CyEdge e = network.addEdge(source, target, true);
        AttributeUtil.set(network, e, SBML.INTERACTION_ATTR, interactionType, String.class);
        return e;
    }

    /**
     * Creates an edge to the unit for the quantity.
     *
     * @param q QuantityWithUnit
     * @return edge if unit is available, null otherwise
     */
    private CyEdge createUnitEdge(CyNode n, QuantityWithUnit q){
        CyEdge e = null;

        // edge to unit
        if (q.isSetUnits()){
            UnitDefinition ud = q.getUnitsInstance();
            CyNode udNode = nodeByCyId.get(CyIdSBaseMap.unitDefinitionCyId(ud));
            /*
             The UnitDefinition instance which has the unitsID of this SBaseWithUnit as id.
             Null if it doesn't exist. In case that the unit of this SBaseWithUnit represents
             a base Unit, a new UnitDefinition will be created and returned by this method.
             This new UnitDefinition will only contain the one unit represented by the unit
             identifier in this SBaseWithUnit. Note that the corresponding model will not
             contain this UnitDefinition. The identifier of this new UnitDefinition will
             be set to the same value as the name of the base Unit.

             I.e. in the case of a base unit we have to create the UnitDefinition node first.
             */
            if (ud != null && udNode == null){
                logger.debug(String.format("Base UnitDefinition encountered. Creating UnitDefinition graph.", ud));
                createUnitDefinitionGraph(ud);
                udNode = nodeByCyId.get(CyIdSBaseMap.unitDefinitionCyId(ud));
            }
            // now the udNode should exist for sure
            if (udNode != null){
                e = createEdge(n, udNode, SBML.INTERACTION_SBASE_UNITDEFINITION);
            } else {
                logger.error(String.format("UnitDefinition node not found for <%s>:", q, q.getId()));
            }
        }
        return e;
    }

    /**
     * Creates the graph for a given UnitDefinition.
     * This is used for all UnitDefinitions in the ListOfUnitDefinitions, but
     * also for the UnitInstances of base units, which are not necessarily part
     * of the ListOfUnits. For instance substanceUnits of species.
     *
     * @param ud
     */
    private void createUnitDefinitionGraph(UnitDefinition ud){
        CyNode n = createNode(CyIdSBaseMap.unitDefinitionCyId(ud),
                SBML.NODETYPE_UNIT_DEFINITION);
        setNamedSBaseAttributes(n, ud);

        for (Unit unit: ud.getListOfUnits()){
            if (ud.isSetId() && unit.isSetKind()){
                CyNode uNode = createNode(CyIdSBaseMap.unitCyId(ud, unit),
                        SBML.NODETYPE_UNIT);
                setUnitAttributes(uNode, unit);

                // edge to UnitDefinition
                createEdge(uNode, n, SBML.INTERACTION_UNIT_UNITDEFINITION);
            }else{
                logger.warn(String.format("Unit could not be created due to missing " +
                        "UnitDefinition id or unit kind: ", ud));
            }
        }
    }


    /**
     * Sets metaId and SBOTerm.
     * RDF & COBRA attributes are set.
     */
    /**
     * Set attributes for SBase.
     * In addition RDF & COBRA attributes are set.
     *
     * @param n
     * @param sbase
     */
    private void setSBaseAttributes(CyIdentifiable n, SBase sbase){
        if (sbase.isSetSBOTerm()){
            AttributeUtil.set(network, n, SBML.ATTR_SBOTERM, sbase.getSBOTermID(), String.class);
        }
        if (sbase.isSetMetaId()){
            AttributeUtil.set(network, n, SBML.ATTR_METAID, sbase.getMetaId(), String.class);
        }
        // RDF attributes
        // This creates Cytoscape attributes from the CV terms
        Properties props = AnnotationUtil.parseCVTerms(sbase);
        for(Object key : props.keySet()){
            String keyString = key.toString();
            String valueString = props.getProperty((String) key);
            AttributeUtil.set(network, n, keyString, valueString, String.class);
        }
        // COBRA attributes
        if ((sbase instanceof Reaction) || (sbase instanceof Species)){
            Properties cobraProps = CobraUtil.parseCobraNotes(sbase);
            props.putAll(cobraProps);
        }
        // create attributes for properties
        for(Object key : props.keySet()){
            String keyString = key.toString();
            String valueString = props.getProperty((String) key);
            AttributeUtil.set(network, n, keyString, valueString, String.class);
        }
    }


	/**
	 * Set attributes for NamedSBase.
	 *
     * @param n CyIdentifiable to set attributes on
	 * @param nsb NamedSBase
	 * @return
	 */
    private void setNamedSBaseAttributes(CyIdentifiable n, NamedSBase nsb){
        setSBaseAttributes(n, nsb);
        if (nsb.isSetId()) {
            String id = nsb.getId();
            AttributeUtil.set(network, n, SBML.ATTR_ID, id, String.class);
            AttributeUtil.set(network, n, SBML.LABEL, id, String.class);
        }
        if (nsb.isSetName()){
            String name = nsb.getName();
            AttributeUtil.set(network, n, SBML.ATTR_NAME, name, String.class);
            AttributeUtil.set(network, n, SBML.LABEL, name, String.class);
        }
    }

    /**
     * Set attributes for NamedSBaseWithDerivedUnit.
     *
     * @param n
     * @param nsbu
     */
    private void setNamedSBaseWithDerivedUnitAttributes(CyIdentifiable n, NamedSBaseWithDerivedUnit nsbu) {
        setNamedSBaseAttributes(n, nsbu);
        UnitDefinition udef = nsbu.getDerivedUnitDefinition();
        if (udef != null){
            AttributeUtil.set(network, n, SBML.ATTR_DERIVED_UNITS, udef.toString(), String.class);
        }
    }

    /**
     * Set attributes for QuantityWithUnit.
     * e.g. LocalParameters.
     *
     * @param n
     * @param q
     */
    private void setQuantityWithUnitAttributes(CyIdentifiable n, QuantityWithUnit q){
        setNamedSBaseWithDerivedUnitAttributes(n, q);
        if (q.isSetValue()) {
            AttributeUtil.set(network, n, SBML.ATTR_VALUE, q.getValue(), Double.class);
        }
        if (q.isSetUnits()){
            AttributeUtil.set(network, n, SBML.ATTR_UNITS, q.getUnits(), String.class);
        }
    }

    /**
     * Set attributes for Symbol.
     * e.g. Species or Parameters
     *
     * @param n
     * @param symbol
     * @return
     */
    private void setSymbolNodeAttributes(CyIdentifiable n, Symbol symbol){
        setQuantityWithUnitAttributes(n, symbol);
        if (symbol.isSetConstant()){
            AttributeUtil.set(network, n, SBML.ATTR_CONSTANT, symbol.getConstant(), Boolean.class);
        }
    }

    /**
     * Set attributes for AbstractMathContainer.
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
     *
     * @param n
     * @param container
     */
    private void setAbstractMathContainerNodeAttributes(CyIdentifiable n, AbstractMathContainer container){
        setSBaseAttributes(n, container);

        String derivedUnits = container.getDerivedUnits();
        AttributeUtil.set(network, n, SBML.ATTR_DERIVED_UNITS, derivedUnits, String.class);
        if (container.isSetMath()){
            ASTNode astNode = container.getMath();
            AttributeUtil.set(network, n, SBML.ATTR_MATH, astNode.toFormula(), String.class);
        }
    }

    /**
     * Set attributes for unit.
     * @param n
     * @param u
     */
    private void setUnitAttributes(CyIdentifiable n, Unit u){
        setSBaseAttributes(n, u);

        String kind = u.getKind().toString();
        AttributeUtil.set(network, n, SBML.LABEL, kind, String.class);
        AttributeUtil.set(network, n, SBML.ATTR_UNIT_KIND, kind, String.class);
        if (u.isSetExponent()){
            AttributeUtil.set(network, n, SBML.ATTR_UNIT_EXPONENT, u.getExponent(), Double.class);
        }
        if (u.isSetScale()){
            AttributeUtil.set(network, n, SBML.ATTR_UNIT_SCALE, u.getScale(), Integer.class);
        }
        if (u.isSetMultiplier()){
            AttributeUtil.set(network, n, SBML.ATTR_UNIT_MULTIPLIER, u.getMultiplier(), Double.class);
        }
    }


    /**
     * Creates math subgraph for given math container and node.
     *
     * @param container
     * @param containerNode
     * @param edgeType
     */
    private void createMathNetwork(AbstractMathContainer container, CyNode containerNode, String edgeType){
        if (container.isSetMath()){
            ASTNode astNode = container.getMath();
            AttributeUtil.set(network, containerNode, SBML.ATTR_MATH, astNode.toFormula(), String.class);

            // Get the refenced objects in math.
            // This can be parameters, localParameters, species, ...
            // create edge if node exists
            for (NamedSBase nsb : ASTNodeUtil.findReferencedNamedSBases(astNode)){
                CyNode nsbNode = nodeByCyId.get(nsb.getId());

                if (nsbNode != null){
                    createEdge(nsbNode, containerNode, edgeType);
                }else{
                    logger.warn("Node for id <" + nsb.getId() +"> not found in math <" + astNode.toFormula() + ">");
                }
            }
        }
    }



    //////////////////////////////////////////////////////////////////////////////////////////

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
     * Adds extended sbml types as node attributes.
     *
     * The extended SBML types can be used in the visual mapping.
     * This allows for instance to distinguish reversible and irreversible reactions.
     */
    private void addSBMLTypesExtended(CyNetwork network, Model model){
        for (CyNode n : network.getNodeList()){
            String type = AttributeUtil.get(network, n, SBML.NODETYPE_ATTR, String.class);
            if (type == null){
                logger.error(String.format("SBML.NODETYPE_ATTR not set for SBML node: %s", n));
            }
            // additional subtypes
            if (type.equals(SBML.NODETYPE_REACTION)){
                Boolean reversible = AttributeUtil.get(network, n, SBML.ATTR_REVERSIBLE, Boolean.class);
                if (reversible == true){
                    type = SBML.NODETYPE_REACTION_REVERSIBLE;
                } else {
                    type = SBML.NODETYPE_REACTION_IRREVERSIBLE;
                }
            }
            AttributeUtil.set(network, n, SBML.NODETYPE_ATTR_EXTENDED, type, String.class);
        }
    }

    /**
     * Adds extended sbml interaction as edge attributes.
     *
     * The extended SBML types can be used in the visual mapping.
     * This allows for instance to distinguish modifiers from activators and inhibitors.
     */
    private void addSBMLInteractionExtended(CyNetwork network){
        for (CyEdge e : network.getEdgeList()){
            String type = AttributeUtil.get(network, e, SBML.INTERACTION_ATTR, String.class);

            if (type != null) {
                // additional subtypes
                if (type.equals(SBML.INTERACTION_REACTION_MODIFIER)) {
                    String sboterm = AttributeUtil.get(network, e, SBML.ATTR_SBOTERM, String.class);
                    if (SBML.SBO_INHIBITORS.contains(sboterm)) {
                        type = SBML.INTERACTION_REACTION_INHIBITOR;
                    } else if (SBML.SBO_ACTIVATORS.contains(sboterm)) {
                        type = SBML.INTERACTION_REACTION_ACTIVATOR;
                    }
                }
                AttributeUtil.set(network, e, SBML.INTERACTION_ATTR_EXTENDED, type, String.class);
            } else {
                logger.error("interaction type not set for edge: " + e);
            }
        }
    }

}

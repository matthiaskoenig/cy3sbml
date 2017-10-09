package org.cy3sbml;

import java.io.File;
import java.io.InputStream;
import java.util.*;

import org.cy3sbml.styles.StyleManager;
import org.cy3sbml.util.filter.SBaseFilter;
import org.cytoscape.group.CyGroup;
import org.cytoscape.group.CyGroupFactory;
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
import org.sbml.jsbml.ext.comp.*;
import org.sbml.jsbml.ext.groups.*;
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
// SBML GROUPS
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
 * <p>
 * The reader creates the master SBML network graph with various subnetworks
 * created from the full graph.
 */
public class SBMLReaderTask extends AbstractTask implements CyNetworkReader {
    private static final Logger logger = LoggerFactory.getLogger(SBMLReaderTask.class);

    private final String fileName;
    private final InputStream stream;
    private final CyNetworkFactory networkFactory;
    private final CyGroupFactory groupFactory;
    private final CyNetworkViewFactory viewFactory;
    private final VisualMappingManager visualMappingManager;
    private final CyLayoutAlgorithmManager cyLayoutAlgorithmManager;
    private final CyProperty<Properties> cy3sbmlProperties;

    private SBMLDocument document;

    private LinkedList<CyNetwork> cyNetworks;
    private TaskMonitor taskMonitor;

    private Map<String, CyNode> metaId2Node;  // node dictionary
    private Map<String, CyNode> id2Node;      // node dictionary
    private Set<CyGroup> cyGroupSet;          // storage of groups to create in subnetworks
    private Map<String, UnitDefinition> baseUnitDefinitions; // base UnitDefinition lookup
    private Boolean error = false;


    /**
     * Constructor
     */
    public SBMLReaderTask(InputStream stream, String fileName,
                          CyNetworkFactory networkFactory,
                          CyGroupFactory cyGroupFactory,
                          CyNetworkViewFactory viewFactory,
                          VisualMappingManager visualMappingManager,
                          CyLayoutAlgorithmManager cyLayoutAlgorithmManager,
                          CyProperty<Properties> cy3sbmlProperties) {

        this.stream = stream;
        this.fileName = fileName;
        this.networkFactory = networkFactory;
        this.groupFactory = cyGroupFactory;
        this.viewFactory = viewFactory;
        this.visualMappingManager = visualMappingManager;
        this.cyLayoutAlgorithmManager = cyLayoutAlgorithmManager;
        this.cy3sbmlProperties = cy3sbmlProperties;

        // networks returned by the reader
        cyNetworks = new LinkedList<>();

    }

    /**
     * Testing constructor.
     */
    public SBMLReaderTask(InputStream stream, String fileName,
                          CyNetworkFactory networkFactory, CyGroupFactory groupFactory) {
        this(stream, fileName, networkFactory, groupFactory, null, null, null, null);
    }

    /**
     * Get created networks from the reader.
     * Here all the registered networks are returned.
     */
    @Override
    public CyNetwork[] getNetworks() {
        return cyNetworks.toArray(new CyNetwork[cyNetworks.size()]);
    }

    /**
     * Build NetworkViews for given network.
     *
     * Here the SBMLDocument is registered in the SBMLManager for the given network,
     * a VisualStyle is applied,
     * and a LayoutAlgorithm is applied.
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
            VisualStyle style = StyleManager.getVisualStyleByName(visualMappingManager, styleName);
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
     * <p>
     * The mapping between CyNetwork elements and SBML elements uses
     * the unique SUIDs of CyNodes and unique cyIds of SBase SBML elements.
     */
    public static One2ManyMapping<String, Long> mappingFromNetwork(CyNetwork network, One2ManyMapping<String, Long> mapping) {
        if (mapping == null) {
            mapping = new One2ManyMapping<>();
        }

        // necessary to go via the root network so that the group nodes
        // are included which are only set in the rootNetwork
        CyRootNetwork rootNetwork = ((CySubNetwork) network).getRootNetwork();

        List<CyNode> nodes = rootNetwork.getNodeList();
        for (CyNode node : nodes) {
            CyRow attributes = rootNetwork.getRow(node);
            String cyId = attributes.get(SBML.ATTR_CYID, String.class);
            mapping.put(cyId, node.getSUID());
        }
        return mapping;
    }

    /**
     * Returns the error status for unit testing.
     */
    public Boolean getError() {
        return error;
    }

    /**
     * Cancel task.
     */
    @Override
    public void cancel() {
    }


    /**
     * Parse SBML networks.
     */
    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        logger.debug("<--- Start Reader --->");
        this.taskMonitor = taskMonitor;
        try {
            if (taskMonitor != null) {
                taskMonitor.setTitle("cy3sbml reader");
                taskMonitor.setProgress(0.0);
            }
            if (cancelled) {
                return;
            }

            //////////////////////////////////////////////////////////////////
            // Read SBMLDocument
            //////////////////////////////////////////////////////////////////
            logger.debug("JSBML version: " + JSBML.getJSBMLVersionString());
            String xml = IOUtil.inputStream2String(stream);
            document = JSBML.readSBMLFromString(xml);


            //////////////////////////////////////////////////////////////////
            // Read ModelDefinitions
            //////////////////////////////////////////////////////////////////
            /* Models can be defined either as a single core model
               or as additional ExternalModelDefinitions and ModelDefinitions
               within the comp package.
               For every ModelDefinition a separate network is created.
               Necessary to create networks for given models.
            */

            // TODO: create a SBMLDocument network (containing the ModelDefinitions & External ModelDefinitions, and submodels)
            CyNetwork sbmlNetwork;

            if (document.isSetModel()) {
                // TODO: add node sbmlNetwork
                Model model = document.getModel();
                // creates the network for the model
                createNetworksFromModel(model);
            } else {
                logger.warn("No core model in SBMLDocument! Check model definition.");
            }

            // comp ModelDefinitions
            CompSBMLDocumentPlugin compDoc = (CompSBMLDocumentPlugin) document.getExtension(CompConstants.namespaceURI);
            if (compDoc != null) {

                // ExternalModelDefinition
                logger.info("<ExternalModelDefinition>");
                for (ExternalModelDefinition emd : compDoc.getListOfExternalModelDefinitions()) {

                    // TODO: add node sbmlNetwork
                    logger.info("ExternalModelDefinition: " + emd.toString());
                    emd.getId();
                    emd.getName();
                    emd.getSource();
                    emd.getModelRef();
                    emd.getMd5();

                    // TODO: fixme
                    // Model must be loaded from the source (currently not implemented)
                    // Model emdModel = emd.getReferencedModel();
                    // if (emdModel != null) {
                        // TODO: add node sbmlNetwork
                        // createNetworksFromModel(emdModel);
                    // }
                    logger.warn("Model reading from ExternalModelDefinition not supported: " + emd);
                }

                // ModelDefinition //
                logger.info("<ModelDefinition>");
                for (ModelDefinition md : compDoc.getListOfModelDefinitions()) {
                    // TODO: add node sbmlNetwork
                    logger.info("ModelDefinition: " + md.toString());

                    Model mdModel = md.getModel();
                    if (mdModel != null) {
                        // TODO: add node sbmlNetwork
                        createNetworksFromModel(mdModel);
                        logger.info("creating model for: " + md.getModel().getId());

                    } else {
                        logger.error("Model could not be read from ModelDefinition: " + md);
                    }
                }
            }

            // flattened comp model
            if (compDoc != null) {
                // The composite model defined in that case is simply the composed model that results from
                // following the chain of inclusions.

                // Section 3.9 on page 32 discusses the important topic of identifier scoping (to find the nodes it is
                // necessary to scope the identifiers)

                // TODO: network of model with instantiated submodels, i.e. the flattend comp model
                // currently no flattening routine in JSBML
            }


            if (taskMonitor != null) {
                taskMonitor.setProgress(0.8);
            }
            logger.debug("<--- End Reader --->");


        } catch (Throwable t) {
            logger.error("Could not read SBML into Cytoscape!", t);
            error = true;
            t.printStackTrace();
            throw new SBMLReaderError("cy3sbml reader failed to build a SBML model. " +
                    "Please validate the file in the online SBML validator at 'http://www.sbml.org/validator/'" +
                    "and report the issue at 'https://github.com/matthiaskoenig/cy3sbml/issues'" + t);
        }
    }




    ////////////////////////////////////////////////////////////////////////////
    // Networks
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Creates network for given model.
     * This can be the main model or an external model definition.
     * @param model
     */
    private void createNetworksFromModel(Model model){
        // Reset lookup maps for given model
        // different models can have same metaIds and ids.
        metaId2Node = new HashMap<>();
        id2Node = new HashMap<>();
        cyGroupSet = new HashSet<>();
        baseUnitDefinitions = new HashMap<>();

        CyNetwork network = readModelInNetwork(model);
        // Create the different subnetworks
        addAllNetworks(network);
    }

    /**
     * Creates the given model into a network.
     * The network is the master network containing all nodes and edges belonging
     * to the model.
     *
     * @param model
     * @return
     */
    private CyNetwork readModelInNetwork(Model model){
        // new network
        CyNetwork network = networkFactory.createNetwork();

        // <core>
        readCore(network, model);
        if (taskMonitor != null) {
            taskMonitor.setProgress(0.4);
        }
        // <qual>
        readQual(network, model);
        // <fbc>
        readFBC(network, model);
        // <comp>
        readComp(network, model);
        // <groups>
        readGroups(network, model);
        // <layout>
        readLayouts(network, model);

        // Add compartment codes dynamically for colors
        addCompartmentCodes(network, model);
        addSBMLTypesExtended(network, model);
        addSBMLInteractionExtended(network);
        return network;
    }

    /**
     * Adds all networks to the list of base networks.
     */
    private void addAllNetworks(CyNetwork network){

        // root network
        CyRootNetwork rootNetwork = ((CySubNetwork) network).getRootNetwork();
        String name = getNetworkName(network);
        rootNetwork.getRow(rootNetwork).set(CyNetwork.NAME, String.format("%s", name));

        // all network
        network.getRow(network).set(CyNetwork.NAME, String.format("%s: %s", SBML.PREFIX_SUBNETWORK_ALL, name));

        // Kinetic network
        CyNetwork kineticNetwork = addSubNetwork(rootNetwork, network, SBML.kineticNodeTypes, SBML.kineticEdgeTypes);
        kineticNetwork.getRow(kineticNetwork).set(CyNetwork.NAME, String.format("%s: %s", SBML.PREFIX_SUBNETWORK_KINETIC, name));

        // base network
        CyNetwork baseNetwork = addSubNetwork(rootNetwork, network, SBML.coreNodeTypes, SBML.coreEdgeTypes);
        baseNetwork.getRow(baseNetwork).set(CyNetwork.NAME, String.format("%s: %s", SBML.PREFIX_SUBNETWORK_BASE, name));


        // add groups to networks
        // TODO: check
        CyNetwork[] networks = {baseNetwork, kineticNetwork};
        for (CyNetwork net : networks) {
            for (CyGroup cyGroup : cyGroupSet) {
                cyGroup.addGroupToNetwork(net);
            }
        }

        // add the networks to the created networks
        cyNetworks.add(network);
        if (baseNetwork != null) {
            cyNetworks.add(baseNetwork);
            cyNetworks.add(kineticNetwork);
        }
    }

    /**
     * Adds a subnetwork to the network.
     *
     * @param rootNetwork
     * @param nodeTypes
     * @param edgeTypes
     * @return
     */
    private static CyNetwork addSubNetwork(CyRootNetwork rootNetwork, CyNetwork network, String[] nodeTypes, String[] edgeTypes) {
        // O(1) lookup (collect nodes and edges)
        HashSet<CyNode> coreNodes = getNetworkNodes(network,
                new HashSet<>(java.util.Arrays.asList(nodeTypes))
        );
        HashSet<CyEdge> coreEdges = getNetworkEdges(network,
                new HashSet<>(java.util.Arrays.asList(edgeTypes))
        );
        // only add edges with nodes in the nodes list
        HashSet<CyEdge> filteredEdges = new HashSet<>();
        for (CyEdge e : coreEdges){
            if (coreNodes.contains(e.getSource()) && coreNodes.contains(e.getTarget())){
                filteredEdges.add(e);
            }
        }
        return rootNetwork.addSubNetwork(coreNodes, filteredEdges);
    }


    /**
     * Get network edges with given edge types.
     *
     * @param network
     * @param edgeTypes
     * @return
     */
    private static HashSet<CyEdge> getNetworkEdges(CyNetwork network, HashSet<String> edgeTypes) {
        HashSet<CyEdge> edges = new HashSet<>();
        for (CyEdge e : network.getEdgeList()) {
            CyRow row = network.getRow(e, CyNetwork.DEFAULT_ATTRS);
            String type = row.get(SBML.INTERACTION_ATTR, String.class);
            if (edgeTypes.contains(type)) {
                edges.add(e);
            }
        }
        return edges;
    }

    /**
     * Get network nodes with given edge types.
     *
     * @param network
     * @param nodeTypes
     * @return
     */
    private static HashSet<CyNode> getNetworkNodes(CyNetwork network, HashSet<String> nodeTypes) {
        HashSet<CyNode> nodes = new HashSet<>();
        for (CyNode n : network.getNodeList()) {
            CyRow row = network.getRow(n, CyNetwork.DEFAULT_ATTRS);
            String type = row.get(SBML.NODETYPE_ATTR, String.class);
            if (nodeTypes.contains(type)) {
                nodes.add(n);
            }
        }
        return nodes;
    }

    /**
     * Get network name.
     * Is used for naming the network and the network collection.
     */
    private String getNetworkName(CyNetwork network) {
        // name of root network
        String name = network.getRow(network).get(SBML.ATTR_NAME, String.class);
        if (name == null) {
            // name not set, try backup name via id
            name = network.getRow(network).get(SBML.ATTR_ID, String.class);
            // still not set, use the file name
            if (name == null) {
                String[] tokens = fileName.split(File.separator);
                name = tokens[tokens.length - 1];
            }
        }
        return name;
    }

    ////////////////////////////////////////////////////////////////////////////
    // SBML CORE
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Read SBML core.
     * <p>
     * Create nodes, edges and attributes from SBML core.
     *
     * @param network
     * @param model
     */
    private void readCore(CyNetwork network, Model model) {
        logger.debug("<core>");

        // SBMLDocument & Model //
        // Mark network as SBML
        AttributeUtil.set(network, network, SBML.NETWORKTYPE_ATTR, SBML.NETWORKTYPE_SBML, String.class);
        AttributeUtil.set(network, network, SBML.LEVEL_VERSION, String.format("L%1$s V%2$s", document.getLevel(), document.getVersion()), String.class);

        // metaId, SBO, id, name
        setNamedSBaseAttributes(network, network, model);

        // Model attributes
        if (model.isSetSubstanceUnits()) {
            AttributeUtil.set(network, network, SBML.ATTR_SUBSTANCE_UNITS, model.getSubstanceUnits(), String.class);
        }
        if (model.isSetTimeUnits()) {
            AttributeUtil.set(network, network, SBML.ATTR_TIME_UNITS, model.getTimeUnits(), String.class);
        }
        if (model.isSetVolumeUnits()) {
            AttributeUtil.set(network, network, SBML.ATTR_VOLUME_UNITS, model.getVolumeUnits(), String.class);
        }
        if (model.isSetAreaUnits()) {
            AttributeUtil.set(network, network, SBML.ATTR_AREA_UNITS, model.getAreaUnits(), String.class);
        }
        if (model.isSetLengthUnits()) {
            AttributeUtil.set(network, network, SBML.ATTR_LENGTH_UNITS, model.getLengthUnits(), String.class);
        }
        if (model.isSetExtentUnits()) {
            AttributeUtil.set(network, network, SBML.ATTR_EXTENT_UNITS, model.getExtentUnits(), String.class);
        }
        if (model.isSetConversionFactor()) {
            AttributeUtil.set(network, network, SBML.ATTR_CONVERSION_FACTOR, model.getConversionFactor(), String.class);
        }

        // UnitDefinition //
        for (UnitDefinition ud : model.getListOfUnitDefinitions()) {
            createUnitDefinitionGraph(network, ud);
        }

        // FunctionDefinition //
        for (FunctionDefinition fd : model.getListOfFunctionDefinitions()) {

            // implements both interfaces
            CyNode n = createNode(network, fd, SBML.NODETYPE_FUNCTION_DEFINITION);
            setNamedSBaseAttributes(network, n, fd);
            setAbstractMathContainerNodeAttributes(network, n, fd);

            // Do not create the math network for the function definition
            // The objects of the FunctionDefinition ASTNode can have different naming conventions
            // than the objects, i.e. a lambda(x), does not mean that it is called with
            // an object x
            // createMathNetwork(fd, fdNode, SBML.INTERACTION_REFERENCE_FUNCTIONDEFINITION);
        }

        // Compartment //
        for (Compartment compartment : model.getListOfCompartments()) {
            CyNode n = createNode(network, compartment, SBML.NODETYPE_COMPARTMENT);
            setSymbolNodeAttributes(network, n, compartment);
            // edge to unit
            createUnitEdge(network, n, compartment);

            if (compartment.isSetSpatialDimensions()) {
                AttributeUtil.set(network, n, SBML.ATTR_SPATIAL_DIMENSIONS, compartment.getSpatialDimensions(), Double.class);
            }
            if (compartment.isSetSize()) {
                AttributeUtil.set(network, n, SBML.ATTR_SIZE, compartment.getSize(), Double.class);
            }
        }

        // Parameter //
        for (Parameter parameter : model.getListOfParameters()) {
            CyNode n = createNode(network, parameter, SBML.NODETYPE_PARAMETER);
            setSymbolNodeAttributes(network, n, parameter);
            // edge to unit
            createUnitEdge(network, n, parameter);
        }

        // Species //
        for (Species species : model.getListOfSpecies()) {
            CyNode n = createNode(network, species, SBML.NODETYPE_SPECIES);
            setSymbolNodeAttributes(network, n, species);
            // edge to unit
            createUnitEdge(network, n, species);

            // edge to compartment
            if (species.isSetCompartment()) {
                AttributeUtil.set(network, n, SBML.ATTR_COMPARTMENT, species.getCompartment(), String.class);
                Compartment comp = species.getCompartmentInstance();
                if (comp != null) {
                    CyNode compNode = metaId2Node.get(comp.getMetaId());
                    createEdge(network, n, compNode, SBML.INTERACTION_SPECIES_COMPARTMENT);
                } else {
                    logger.error(String.format("Compartment does not exist for species: %s for %s", species.getCompartment(), species.getId()));
                }
            }
            if (species.isSetBoundaryCondition()) {
                AttributeUtil.set(network, n, SBML.ATTR_BOUNDARY_CONDITION, species.getBoundaryCondition(), Boolean.class);
            }
            if (species.isSetHasOnlySubstanceUnits()) {
                AttributeUtil.set(network, n, SBML.ATTR_HAS_ONLY_SUBSTANCE_UNITS, species.getHasOnlySubstanceUnits(), Boolean.class);
            }
            if (species.isSetCharge()) {
                AttributeUtil.set(network, n, SBML.ATTR_CHARGE, species.getCharge(), Integer.class);
            }
            if (species.isSetConversionFactor()) {
                AttributeUtil.set(network, n, SBML.ATTR_CONVERSION_FACTOR, species.getConversionFactor(), String.class);
            }
            if (species.isSetSubstanceUnits()) {
                AttributeUtil.set(network, n, SBML.ATTR_SUBSTANCE_UNITS, species.getSubstanceUnits(), String.class);
            }
            if (species.isSetInitialAmount()) {
                AttributeUtil.set(network, n, SBML.ATTR_INITIAL_AMOUNT, species.getInitialAmount(), Double.class);
            }
            if (species.isSetInitialConcentration()) {
                AttributeUtil.set(network, n, SBML.ATTR_INITIAL_CONCENTRATION, species.getInitialConcentration(), Double.class);
            }
        }

        // Reaction //
        for (Reaction reaction : model.getListOfReactions()) {
            CyNode n = createNode(network, reaction, SBML.NODETYPE_REACTION);
            setNamedSBaseWithDerivedUnitAttributes(network, n, reaction);

            if (reaction.isSetReversible()) {
                AttributeUtil.set(network, n, SBML.ATTR_REVERSIBLE, reaction.getReversible(), Boolean.class);
            } else {
                // reversible=true by default
                AttributeUtil.set(network, n, SBML.ATTR_REVERSIBLE, true, Boolean.class);
            }
            if (reaction.isSetFast()) {
                AttributeUtil.set(network, n, SBML.ATTR_FAST, reaction.getFast(), Boolean.class);
            }
            if (reaction.isSetCompartment()) {
                AttributeUtil.set(network, n, SBML.ATTR_COMPARTMENT, reaction.getCompartment(), String.class);
                Compartment comp = reaction.getCompartmentInstance();
                if (comp != null) {
                    // edge to compartment
                    CyNode compNode = metaId2Node.get(comp.getMetaId());
                    createEdge(network, n, compNode, SBML.INTERACTION_REACTION_COMPARTMENT);
                } else {
                    logger.error(String.format("Compartment does not exist for reaction: %s for %s",
                            reaction.getCompartment(), reaction.getId()));
                }
            }

            // Reactants
            for (SpeciesReference speciesRef : reaction.getListOfReactants()) {
                Species species = speciesRef.getSpeciesInstance();
                if (species != null) {
                    CyNode reactantNode = metaId2Node.get(species.getMetaId());
                    CyEdge edge = createEdge(network, n, reactantNode, SBML.INTERACTION_REACTION_REACTANT);
                    setSBaseAttributes(network, edge, speciesRef);

                    Double stoichiometry = (speciesRef.isSetStoichiometry()) ? speciesRef.getStoichiometry() : 1.0;
                    AttributeUtil.set(network, edge, SBML.ATTR_STOICHIOMETRY, stoichiometry, Double.class);
                } else {
                    logger.error(String.format("Reactant does not exist for reaction: %s for %s",
                            speciesRef.getSpecies(), reaction.getId()));
                }
            }
            // Products
            for (SpeciesReference speciesRef : reaction.getListOfProducts()) {
                Species species = speciesRef.getSpeciesInstance();

                if (species != null) {
                    CyNode productNode = metaId2Node.get(species.getMetaId());
                    CyEdge edge = createEdge(network, n, productNode, SBML.INTERACTION_REACTION_PRODUCT);
                    setSBaseAttributes(network, edge, speciesRef);

                    Double stoichiometry = (speciesRef.isSetStoichiometry()) ? speciesRef.getStoichiometry() : 1.0;
                    AttributeUtil.set(network, edge, SBML.ATTR_STOICHIOMETRY, stoichiometry, Double.class);
                } else {
                    logger.error(String.format("Product does not exist for reaction: %s for %s",
                            speciesRef.getSpecies(), reaction.getId()));
                }
            }
            // Modifiers
            for (ModifierSpeciesReference msRef : reaction.getListOfModifiers()) {
                Species species = msRef.getSpeciesInstance();
                if (species != null) {
                    CyNode modifierNode = metaId2Node.get(species.getMetaId());
                    CyEdge edge = createEdge(network, n, modifierNode, SBML.INTERACTION_REACTION_MODIFIER);
                    setSBaseAttributes(network, edge, msRef);
                } else {
                    logger.error(String.format("ModifierSpecies does not exist for reaction: %s for %s",
                            msRef.getSpecies(), reaction.getId()));
                }
            }

            // Kinetic law
            if (reaction.isSetKineticLaw()) {
                KineticLaw law = reaction.getKineticLaw();
                CyNode lawNode = createNode(network, law, SBML.NODETYPE_KINETIC_LAW);
                setAbstractMathContainerNodeAttributes(network, lawNode, law);
                AttributeUtil.set(network, lawNode, SBML.LABEL, reaction.getId(), String.class);


                // edge to reaction
                CyEdge edge = network.addEdge(n, lawNode, true);
                AttributeUtil.set(network, edge, SBML.INTERACTION_ATTR, SBML.INTERACTION_REACTION_KINETICLAW, String.class);

                // local parameter nodes
                if (law.isSetListOfLocalParameters()) {
                    for (LocalParameter lp : law.getListOfLocalParameters()) {
                        // This changes the SBMLDocument !
                        // but only reliable way to handle LocalParameters in math networks
                        String lpId = MappingUtil.localParameterId(lp);
                        lp.setId(lpId);

                        CyNode lpNode = createNode(network, lp, SBML.NODETYPE_LOCAL_PARAMETER);
                        setQuantityWithUnitAttributes(network, lpNode, lp);
                        // edge to unit
                        createUnitEdge(network, lpNode, lp);

                        // edge to law
                        createEdge(network, lpNode, lawNode, SBML.INTERACTION_LOCALPARAMETER_KINETICLAW);
                    }
                }

                // referenced nodes in math
                if (law.isSetMath()) {
                    // set math on reaction
                    AttributeUtil.set(network, n, SBML.ATTR_KINETIC_LAW, law.getMath().toFormula(), String.class);
                    createMathNetwork(network, law, lawNode, SBML.INTERACTION_REFERENCE_KINETICLAW);
                } else {
                    logger.warn(String.format("No math set for kinetic law in reaction: %s", reaction.getId()));
                }
            }
        }

        // InitialAssignment //
        for (InitialAssignment assignment : model.getListOfInitialAssignments()) {
            Variable variable = assignment.getVariableInstance();
            if (variable != null) {
                CyNode assignmentNode = createNode(network, assignment, SBML.NODETYPE_INITIAL_ASSIGNMENT);
                setAbstractMathContainerNodeAttributes(network, assignmentNode, assignment);
                AttributeUtil.set(network, assignmentNode, SBML.ATTR_VARIABLE, variable.getId(), String.class);

                // edge to variable
                CyNode variableNode = metaId2Node.get(variable.getMetaId());
                if (variableNode != null) {
                    createEdge(network, variableNode, assignmentNode, SBML.INTERACTION_VARIABLE_INITIAL_ASSIGNMENT);
                    if (assignment.isSetMath()) {
                        ASTNode astNode = assignment.getMath();
                        AttributeUtil.set(network, variableNode, SBML.ATTR_INITIAL_ASSIGNMENT, astNode.toFormula(), String.class);
                    }
                } else {
                    logger.warn(String.format("Variable is neither Compartment, Species or Parameter, probably SpeciesReference: %s in %s", variable, assignment));
                }
                // referenced nodes in math
                createMathNetwork(network, assignment, assignmentNode, SBML.INTERACTION_REFERENCE_INITIAL_ASSIGNMENT);

            } else {
                logger.error(String.format("Variable does not exist for InitialAssignment: %s for %s", assignment.getVariable(), "?"));
            }
        }

        // Rule //
        for (Rule rule : model.getListOfRules()) {
            String ruleType = null;
            Variable variable = null;

            if (rule instanceof AlgebraicRule) {
                ruleType = SBML.NODETYPE_ALGEBRAIC_RULE;
            } else {
                variable = SBMLUtil.getVariableFromRule(rule);
                if (rule instanceof AssignmentRule) {
                    ruleType = SBML.NODETYPE_ASSIGNMENT_RULE;
                } else if (rule instanceof RateRule) {
                    ruleType = SBML.NODETYPE_RATE_RULE;
                }
            }

            CyNode n = createNode(network, rule, ruleType);
            setAbstractMathContainerNodeAttributes(network, n, rule);
            // referenced nodes in math
            createMathNetwork(network, rule, n, SBML.INTERACTION_REFERENCE_RULE);

            String label = SBMLUtil.TEMPLATE_ALGEBRAIC_RULE;
            // edge to variable for rateRule and assignmentRule
            if (variable != null) {
                if (rule instanceof AssignmentRule) {
                    label = String.format(SBMLUtil.TEMPLATE_ASSIGNMENT_RULE, variable.getId());
                } else if (rule instanceof RateRule) {
                    label = String.format(SBMLUtil.TEMPLATE_RATE_RULE, variable.getId());
                }
                AttributeUtil.set(network, n, SBML.ATTR_VARIABLE, variable.getId(), String.class);

                CyNode variableNode = metaId2Node.get(variable.getMetaId());
                if (variableNode != null) {
                    createEdge(network, variableNode, n, SBML.INTERACTION_VARIABLE_RULE);
                } else {
                    //  An assignment rule can refer to the identifier of a Species, SpeciesReference,
                    //    Compartment, or global Parameter object in the model
                    //    The case SpeciesReference is not handled !
                    logger.warn(String.format("Variable is neither Compartment, Species or Parameter, probably SpeciesReference: %s in %s",
                            variable, rule));
                }
            }
            AttributeUtil.set(network, n, SBML.LABEL, label, String.class);
        }

        // Constraints
        // No models with constraints exist for testing.
        for (Constraint constraint : model.getListOfConstraints()) {
            CyNode n = createNode(network, constraint, SBML.NODETYPE_CONSTRAINT);
            setAbstractMathContainerNodeAttributes(network, n, constraint);
            if (constraint.isSetMessage()) {
                try {
                    AttributeUtil.set(network, n, SBML.ATTR_MESSAGE,
                            constraint.getMessageString(), String.class);
                } catch (XMLStreamException e) {
                    logger.error("Message string could not be created for constraint.", e);
                    e.printStackTrace();
                }
            }
        }

        // Events
        for (Event event : model.getListOfEvents()) {

            CyNode n = createNode(network, event, SBML.NODETYPE_EVENT);
            setNamedSBaseWithDerivedUnitAttributes(network, n, event);

            if (event.isSetUseValuesFromTriggerTime()) {
                AttributeUtil.set(network, n, SBML.ATTR_USE_VALUES_FROM_TRIGGER_TIME,
                        event.getUseValuesFromTriggerTime(), Boolean.class);
            }

            // edge via trigger math
            createMathNetwork(network, event.getTrigger(), n, SBML.INTERACTION_TRIGGER_EVENT);
            // edge via priority math
            if (event.isSetPriority()) {
                createMathNetwork(network, event.getPriority(), n, SBML.INTERACTION_PRIORITY_EVENT);
            }
            // edge via delay math
            if (event.isSetDelay()) {
                createMathNetwork(network, event.getDelay(), n, SBML.INTERACTION_PRIORITY_EVENT);
            }

            for (EventAssignment ea : event.getListOfEventAssignments()) {
                CyNode eaNode = createNode(network, ea, SBML.NODETYPE_EVENT_ASSIGNMENT);
                setAbstractMathContainerNodeAttributes(network, eaNode, ea);

                // edge to event
                createEdge(network, n, eaNode, SBML.INTERACTION_EVENT_EVENT_ASSIGNMENT);

                // edge to variable
                if (ea.isSetVariable()) {
                    Variable variable = ea.getVariableInstance();
                    CyNode variableNode = metaId2Node.get(variable.getMetaId());
                    AttributeUtil.set(network, eaNode, SBML.LABEL, variable.getId(), String.class);
                    AttributeUtil.set(network, eaNode, SBML.ATTR_VARIABLE, variable.getId(), String.class);

                    if (variableNode != null) {
                        createEdge(network, variableNode, eaNode, SBML.INTERACTION_VARIABLE_EVENT_ASSIGNMENT);
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
                createMathNetwork(network, ea, eaNode, SBML.INTERACTION_REFERENCE_EVENT_ASSIGNMENT);
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // SBML QUAL
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Create nodes, edges and attributes from Qualitative Model.
     *
     * @param network
     * @param model
     */
    private void readQual(CyNetwork network, Model model) {
        logger.debug("<qual>");

        QualModelPlugin qualModel = (QualModelPlugin) model.getExtension(QualConstants.namespaceURI);
        if (qualModel == null) {
            return;
        }

        // QualSpecies //
        for (QualitativeSpecies qSpecies : qualModel.getListOfQualitativeSpecies()) {
            CyNode n = createNode(network, qSpecies, SBML.NODETYPE_QUAL_SPECIES);
            setNamedSBaseAttributes(network, n, qSpecies);

            if (qSpecies.isSetCompartment()) {
                AttributeUtil.set(network, n, SBML.ATTR_COMPARTMENT, qSpecies.getCompartment(), String.class);
                // edge to compartment
                Compartment comp = qSpecies.getCompartmentInstance();
                CyNode compNode = metaId2Node.get(comp.getMetaId());
                createEdge(network, n, compNode, SBML.INTERACTION_SPECIES_COMPARTMENT);
            }
            if (qSpecies.isSetConstant()) {
                AttributeUtil.set(network, n, SBML.ATTR_CONSTANT, qSpecies.getConstant(), Boolean.class);
            }
            if (qSpecies.isSetInitialLevel()) {
                AttributeUtil.set(network, n, SBML.ATTR_QUAL_INITIAL_LEVEL, qSpecies.getInitialLevel(), Integer.class);
            }
            if (qSpecies.isSetMaxLevel()) {
                AttributeUtil.set(network, n, SBML.ATTR_QUAL_MAX_LEVEL, qSpecies.getMaxLevel(), Integer.class);
            }
        }
        // QualTransitions
        for (Transition transition : qualModel.getListOfTransitions()) {
            CyNode n = createNode(network, transition, SBML.NODETYPE_QUAL_TRANSITION);
            setNamedSBaseAttributes(network, n, transition);

            // Inputs
            for (Input input : transition.getListOfInputs()) {
                String qSpeciesId = input.getQualitativeSpecies();
                QualitativeSpecies qSpecies = qualModel.getQualitativeSpecies(qSpeciesId);

                CyNode inNode = metaId2Node.get(qSpecies.getMetaId());
                CyEdge e = createEdge(network, n, inNode, SBML.INTERACTION_QUAL_TRANSITION_INPUT);


                // required (no checking of required -> NullPointerException risk)
                AttributeUtil.set(network, e, SBML.ATTR_QUAL_TRANSITION_EFFECT, input.getTransitionEffect().toString(), String.class);
                AttributeUtil.set(network, e, SBML.ATTR_QUAL_QUALITATIVE_SPECIES, input.getQualitativeSpecies().toString(), String.class);
                // optional
                if (input.isSetId()) {
                    AttributeUtil.set(network, e, SBML.ATTR_ID, input.getId(), String.class);
                }
                if (input.isSetName()) {
                    AttributeUtil.set(network, e, SBML.ATTR_NAME, input.getName(), String.class);
                }
                if (input.isSetSign()) {
                    AttributeUtil.set(network, e, SBML.ATTR_QUAL_SIGN, input.getSign().toString(), String.class);
                }
                if (input.isSetSBOTerm()) {
                    AttributeUtil.set(network, e, SBML.ATTR_SBOTERM, input.getSBOTermID(), String.class);
                }
                if (input.isSetMetaId()) {
                    AttributeUtil.set(network, e, SBML.ATTR_METAID, input.getMetaId(), String.class);
                }
                if (input.isSetThresholdLevel()) {
                    AttributeUtil.set(network, e, SBML.ATTR_QUAL_THRESHOLD_LEVEL, input.getThresholdLevel(), Integer.class);
                }
            }

            // Outputs
            for (Output output : transition.getListOfOutputs()) {
                String qSpeciesString = output.getQualitativeSpecies();
                QualitativeSpecies qSpecies = qualModel.getQualitativeSpecies(qSpeciesString);
                CyNode outNode = metaId2Node.get(qSpecies.getMetaId());
                CyEdge e = createEdge(network, n, outNode, SBML.INTERACTION_QUAL_TRANSITION_OUTPUT);

                // required
                AttributeUtil.set(network, e, SBML.ATTR_QUAL_QUALITATIVE_SPECIES, output.getQualitativeSpecies().toString(), String.class);
                AttributeUtil.set(network, e, SBML.ATTR_QUAL_TRANSITION_EFFECT, output.getTransitionEffect().toString(), String.class);
                // optional
                if (output.isSetId()) {
                    AttributeUtil.set(network, e, SBML.ATTR_ID, output.getId(), String.class);
                }
                if (output.isSetName()) {
                    AttributeUtil.set(network, e, SBML.ATTR_NAME, output.getName(), String.class);
                }
                if (output.isSetSBOTerm()) {
                    AttributeUtil.set(network, e, SBML.ATTR_SBOTERM, output.getSBOTermID(), String.class);
                }
                if (output.isSetMetaId()) {
                    AttributeUtil.set(network, e, SBML.ATTR_METAID, output.getMetaId(), String.class);
                }
                if (output.isSetOutputLevel()) {
                    AttributeUtil.set(network, e, SBML.ATTR_QUAL_OUTPUT_LEVEL, output.getOutputLevel(), Integer.class);
                }
            }

            // parse the default term / function terms
            if (transition.isSetListOfFunctionTerms()) {
                List<Integer> resultLevels = new LinkedList<Integer>();
                for (FunctionTerm term : transition.getListOfFunctionTerms()) {
                    resultLevels.add(term.getResultLevel());
                }
                AttributeUtil.setList(network, n, SBML.ATTR_QUAL_RESULT_LEVELS, resultLevels, Integer.class);
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // SBML FBC
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Creates network information from fbc model.
     */
    private void readFBC(CyNetwork network, Model model) {
        logger.debug("<fbc>");

        FBCModelPlugin fbcModel = (FBCModelPlugin) model.getExtension(FBCConstants.namespaceURI);
        if (fbcModel == null) {
            return;
        }

        // Model attributes
        if (fbcModel.isSetStrict()) {
            AttributeUtil.set(network, network, SBML.ATTR_FBC_STRICT, fbcModel.getStrict(), Boolean.class);
        }

        // Species attributes
        for (Species species : model.getListOfSpecies()) {
            FBCSpeciesPlugin fbcSpecies = (FBCSpeciesPlugin) species.getExtension(FBCConstants.namespaceURI);
            if (fbcSpecies != null) {
                CyNode n = metaId2Node.get(species.getMetaId());
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
        for (Objective objective : fbcModel.getListOfObjectives()) {
            // one reaction attribute column per objective
            String key = String.format(SBML.ATTR_FBC_OBJECTIVE_TEMPLATE, objective.getId());
            for (FluxObjective fluxObjective : objective.getListOfFluxObjectives()) {
                Reaction reaction = fluxObjective.getReactionInstance();
                CyNode node = metaId2Node.get(reaction.getMetaId());
                AttributeUtil.set(network, node, key, fluxObjective.getCoefficient(), Double.class);
            }
        }

        // GeneProducts as nodes
        for (GeneProduct geneProduct : fbcModel.getListOfGeneProducts()) {
            CyNode n = createNode(network, geneProduct, SBML.NODETYPE_FBC_GENEPRODUCT);
            setNamedSBaseAttributes(network, n, geneProduct);

            // Overwrite label
            if (geneProduct.isSetLabel()) {
                AttributeUtil.set(network, n, SBML.LABEL, geneProduct.getLabel(), String.class);
            }

            // edge to associated species
            if (geneProduct.isSetAssociatedSpecies()) {
                // id lookup
                CyNode speciesNode = id2Node.get(geneProduct.getAssociatedSpecies());
                createEdge(network, speciesNode, n, SBML.INTERACTION_FBC_GENEPRODUCT_SPECIES);
            }
        }

        // Reaction attributes
        for (Reaction reaction : model.getListOfReactions()) {
            FBCReactionPlugin fbcReaction = (FBCReactionPlugin) reaction.getExtension(FBCConstants.namespaceURI);

            if (fbcReaction != null) {
                // optional bounds
                CyNode node = metaId2Node.get(reaction.getMetaId());
                if (fbcReaction.isSetLowerFluxBound()) {
                    AttributeUtil.set(network, node, SBML.ATTR_FBC_LOWER_FLUX_BOUND, fbcReaction.getLowerFluxBound(), String.class);
                    // add edge
                    Parameter p = model.getParameter(fbcReaction.getLowerFluxBound());
                    CyNode parameterNode = metaId2Node.get(p.getMetaId());
                    CyEdge edge = network.addEdge(parameterNode, node, true);
                    AttributeUtil.set(network, edge, SBML.INTERACTION_ATTR, SBML.INTERACTION_PARAMETER_REACTION, String.class);
                }
                if (fbcReaction.isSetUpperFluxBound()) {
                    AttributeUtil.set(network, node, SBML.ATTR_FBC_UPPER_FLUX_BOUND, fbcReaction.getUpperFluxBound(), String.class);
                    // add edge
                    Parameter p = model.getParameter(fbcReaction.getUpperFluxBound());
                    CyNode parameterNode = metaId2Node.get(p.getMetaId());
                    CyEdge edge = network.addEdge(parameterNode, node, true);
                    AttributeUtil.set(network, edge, SBML.INTERACTION_ATTR, SBML.INTERACTION_PARAMETER_REACTION, String.class);
                }

                // Create GeneProteinAssociation (GPA) network
                if (fbcReaction.isSetGeneProductAssociation()) {
                    GeneProductAssociation gpa = fbcReaction.getGeneProductAssociation();

                    // handle And, Or, GeneProductRef recursively
                    Association association = gpa.getAssociation();
                    processAssociation(network, node, SBML.NODETYPE_REACTION, association);
                }
            }
        }

        // parse fbc v1 fluxBounds and geneAssociations
        if (fbcModel.getVersion() == 1) {
            // geneAssociations
            if (model.isSetAnnotation()) {
                // fbc v1 geneAssociations not in specification or supported by JSBML, so not parsed
                Annotation annotation = model.getAnnotation();
                XMLNode xmlNode = annotation.getXMLNode();

                for (int k = 0; k < xmlNode.getChildCount(); k++) {
                    XMLNode child = xmlNode.getChild(k);
                    String name = child.getName();
                    if (name.equals("listOfGeneAssociations") | name.equals("geneAssociation")) {
                        logger.warn("GeneAssociations of fbc v1 not supported in JSBML.");
                        break;
                    }
                }
            }

            // fluxBounds
            for (FluxBound fluxBound : fbcModel.getListOfFluxBounds()) {
                Reaction reaction = fluxBound.getReactionInstance();
                CyNode n = metaId2Node.get(reaction.getMetaId());
                String operation = fluxBound.getOperation().toString();
                Double value = fluxBound.getValue();
                if (operation.equals(Operation.EQUAL)) {
                    AttributeUtil.set(network, n, SBML.ATTR_FBC_LOWER_FLUX_BOUND, value.toString(), String.class);
                    AttributeUtil.set(network, n, SBML.ATTR_FBC_UPPER_FLUX_BOUND, value.toString(), String.class);
                } else if (operation.equals(Operation.GREATER_EQUAL)) {
                    AttributeUtil.set(network, n, SBML.ATTR_FBC_LOWER_FLUX_BOUND, value.toString(), String.class);
                } else if (operation.equals(Operation.LESS_EQUAL)) {
                    AttributeUtil.set(network, n, SBML.ATTR_FBC_UPPER_FLUX_BOUND, value.toString(), String.class);
                }
            }
        }
    }

    /**
     * Recursive function for processing the Associations.
     * FIXME: unnecessary code duplication
     */
    private void processAssociation(CyNetwork network, CyNode parentNode, String parentType, Association association) {
        // GeneProductRef
        if (association.getClass().equals(GeneProductRef.class)) {
            GeneProductRef gpRef = (GeneProductRef) association;
            CyNode gpNode = metaId2Node.get(gpRef.getGeneProductInstance().getMetaId());
            if (gpNode != null) {
                if (parentType.equals(SBML.NODETYPE_REACTION)) {
                    createEdge(network, gpNode, parentNode, SBML.INTERACTION_FBC_ASSOCIATION_REACTION);
                } else {
                    createEdge(network, gpNode, parentNode, SBML.INTERACTION_FBC_ASSOCIATION_ASSOCIATION);
                }
            } else {
                logger.error(String.format("GeneProduct does not exist for GeneAssociation: %s in %s",
                        gpRef.getGeneProduct(), association));
            }
        }
        // And
        else if (association.getClass().equals(And.class)) {
            And andRef = (And) association;

            // Create and node & edge
            CyNode andNode = network.addNode();
            AttributeUtil.set(network, andNode, SBML.LABEL, "AND", String.class);
            AttributeUtil.set(network, andNode, SBML.NODETYPE_ATTR, SBML.NODETYPE_FBC_AND, String.class);
            if (parentType.equals(SBML.NODETYPE_REACTION)) {
                createEdge(network, andNode, parentNode, SBML.INTERACTION_FBC_ASSOCIATION_REACTION);
            } else {
                createEdge(network, andNode, parentNode, SBML.INTERACTION_FBC_ASSOCIATION_ASSOCIATION);
            }
            // recursive association children
            for (Association a : andRef.getListOfAssociations()) {
                processAssociation(network, andNode, SBML.NODETYPE_FBC_AND, a);
            }
        }
        // or
        else if (association.getClass().equals(Or.class)) {
            Or orRef = (Or) association;

            // Create and node & edge
            CyNode orNode = network.addNode();
            AttributeUtil.set(network, orNode, SBML.LABEL, "OR", String.class);
            AttributeUtil.set(network, orNode, SBML.NODETYPE_ATTR, SBML.NODETYPE_FBC_OR, String.class);
            if (parentType.equals(SBML.NODETYPE_REACTION)) {
                createEdge(network, orNode, parentNode, SBML.INTERACTION_FBC_ASSOCIATION_REACTION);
            } else {
                createEdge(network, orNode, parentNode, SBML.INTERACTION_FBC_ASSOCIATION_ASSOCIATION);
            }

            // recursive association children
            for (Association a : orRef.getListOfAssociations()) {
                processAssociation(network, orNode, SBML.NODETYPE_FBC_AND, a);
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // SBML COMP
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Instantiates the model with all submodels
     * This is a flattening routine of the model.
     *
     * @param model
     */
    private void readFlattenedModel(Model model){

         CompModelPlugin compModel = (CompModelPlugin) model.getExtension(CompConstants.namespaceURI);
        if (compModel == null){
            // no model to flatten
            return;
        }


        //The Model object referenced by the Submodel object establishes the object names-
        // paces for the portRef, idRef, unitRef and metaIdRef attributes:

        logger.info("<Submodel>");
        for (Submodel submodel: compModel.getListOfSubmodels()){

            logger.info(submodel.toString());

            // Deletion
            for (Deletion deletion: submodel.getListOfDeletions()){
                // TODO: add edge
                logger.info(deletion.toString());

                // SbaseRef
                // TODO delete the components from network
                deletion.getIdRef();

                /*
                1. An object that has been “deleted” is considered inaccessible. Any element that has been deleted (or replaced,
                as discussed in Section 3.6) may not be referenced by an SBaseRef object.
                2. If the deleted object has child objects and other structures, the child objects and substructure are also
                considered to be deleted.
                3. It is not an error to delete explicitly an object that is already deleted by implication (for example as a result of
                point number 2 above). The resulting model is the same.
                */
            }

            // TODO: generic method for getting node for SbaseRef
            // SBaseRef provides attributes portRef, idRef, unitRef 12
            // and metaIdRef, and a recursive subcomponent, sBaseRef
        }

        // ReplacedElement & ReplacedBy
        // Replacements are defined within the submodelRef namespace
        /*
        A replacement implies that dependencies involving the replaced object must be updated: all references to the
        replaced object elsewhere in the model are taken to refer to the replacement object instead. For example, if one
        species replaces another, then any reference to the original species in mathematical formulas, or lists of reactants
        or products or modifiers in reactions, or initial assignments, or any other SBML construct, are taken to refer to
        the replacement species.

        Moreover, any annotations that refer to
        the replaced species’ metaid value must be made to refer to the replacement species’ metaid value instead; and 26
        anything else that referred either to an object identifier (i.e., attributes such as the id attribute whose types inherit 27
        from the SId primitive data type) or the meta identifier (i.e., the metaid attribute or any other attribute that inherits 28
        from the ID primitive data type) must be made to refer to the replacement species object instead.

        1. The replaced element is considered to be removed from the model.
        2. Attributes having values of type SIdRef that refer to the replaced element are considered to refer to the 4
            replacement element instead.
        3. Attributes having values of type UnitSIdRef that refer to the replaced unit are considered to refer to the 6
            replacement element instead.
        4. Within mathematical formulas, MathML <cn> elements that refer to a replaced element are considered to
            refer to the replacement element instead.
        5. Annotations with attributes having values of type IDREF that refer to the meta identifier of replaced elements
                are considered to refer to the replacement element. In particular, this rule applies to the rdf:about attribute
         */

        logger.info("<ReplacedElement & ReplacedBy>");
        List<SBase> sbases = (List<SBase>) document.filter(new SBaseFilter());
        for (SBase sbase: sbases){
            CompSBasePlugin compSBase = (CompSBasePlugin) sbase.getExtension(CompConstants.namespaceURI);
            if (compSBase != null){
                logger.info("compSBase: " + compSBase.toString());
                for (ReplacedElement replacedElement : compSBase.getListOfReplacedElements()){
                    logger.info(replacedElement.toString());
                    // SBaseRef
                    // TODO:
                    replacedElement.getSubmodelRef();

                    replacedElement.getConversionFactor();

                    replacedElement.getDeletion();
                    /*
                    When deletion is set, it means the ReplacedElement object is actually an annotation to indicate that the replacement object
                    replaces something deleted from a submodel. The use of the deletion attribute overrides the use of the attributes
                    inherited from SBaseRef: instead of using, e.g., portRef or idRef, the ReplacedElement instance sets deletion to
                    the identifier of the Deletion object. In addition, the referenced Deletion must be a child of the Submodel referenced
                    by the submodelRef attribute
                    */

                }
                if (compSBase.isSetReplacedBy()){
                    // TODO
                    ReplacedBy replacedBy = compSBase.getReplacedBy();
                    logger.info(replacedBy.toString());
                }
            }
        }
    }


    /**
     * Create network information from comp model.
     */
    private void readComp(CyNetwork network, Model model) {
        logger.debug("<comp>");

        CompModelPlugin compModel = (CompModelPlugin) model.getExtension(CompConstants.namespaceURI);
        if (compModel == null) {
            return;
        }

        // Submodel //
        /*
            Submodels are instantiations of models contained within other models.
            A Submodel object must say which Model object it instantiates, and may additionally define how the Model object is
            to be modified before it is instantiated in the enclosing model.
         */
        logger.info("<Submodel>");
        for (Submodel submodel: compModel.getListOfSubmodels()){

            logger.info(submodel.toString());
            CyNode n = createNode(network, submodel, SBML.NODETYPE_COMP_SUBMODEL);
            setNamedSBaseAttributes(network, n, submodel);

            AttributeUtil.set(network, n, SBML.ATTR_COMP_MODELREF, submodel.getModelRef(), String.class);
            if (submodel.isSetTimeConversionFactor()){
                AttributeUtil.set(network, n, SBML.ATTR_COMP_TIME_CONVERSION_FACTOR, submodel.getTimeConversionFactor(), String.class);
            }
            if (submodel.isSetExtentConversionFactor()){
                AttributeUtil.set(network, n, SBML.ATTR_COMP_EXTENT_CONVERSION_FACTOR, submodel.getExtentConversionFactor(), String.class);
            }

            // Deletion
            for (Deletion deletion: submodel.getListOfDeletions()){
                // TODO: add edge
                logger.info(deletion.toString());
                CyNode nd = createNode(network, deletion, SBML.NODETYPE_COMP_DELETION);
                setNamedSBaseAttributes(network, nd, deletion);

                // SbaseRef
                // TODO
                deletion.getIdRef();
            }

            // TODO: generic method for getting node for SbaseRef
            // SBaseRef provides attributes portRef, idRef, unitRef 12
            // and metaIdRef, and a recursive subcomponent, sBaseRef
        }

        // Port //
        logger.info("<Port>");
        // create port nodes
        for (Port port : compModel.getListOfPorts()) {
            logger.info(port.toString());
            CyNode n = createNode(network, port, SBML.NODETYPE_COMP_PORT);
            setNamedSBaseAttributes(network, n, port);
            setSBaseRefAttributes(network, n, port);
        }
        // create port edges
        for (Port port : compModel.getListOfPorts()){
            CyNode source = AttributeUtil.getNodeByAttribute(network, SBML.ATTR_PORT_SID, port.getId());
            createSBaseRefEdge(network, source, port, model.getId());
        }

        logger.info("<ReplacedElement & ReplacedBy>");
        // only sbases in current model
        List<SBase> sbases = (List<SBase>) model.filter(new SBaseFilter());
        for (SBase sbase: sbases){
            CompSBasePlugin compSBase = (CompSBasePlugin) sbase.getExtension(CompConstants.namespaceURI);
            if (compSBase != null){
                logger.info(compSBase.toString());

                CyNode source = AttributeUtil.getNodeByAttribute(network, SBML.ATTR_CYID, sbase.getMetaId());

                for (ReplacedElement replacedElement : compSBase.getListOfReplacedElements()){

                    logger.info(replacedElement.toString());
                    // SBaseRef
                    // targets can be from other submodels

                    CyNode target = createNode(network, replacedElement, SBML.NODETYPE_COMP_REPLACED_ELEMENT);
                    setSBaseRefAttributes(network, target, replacedElement);
                    AttributeUtil.set(network, target, SBML.LABEL,
                            String.format("%s replacedElement", sbase.getMetaId()), String.class);

                    // edge to replacing element
                    createEdge(network, source, target, SBML.INTERACTION_COMP_SBASE_REPLACED_ELEMENT);

                    createSBaseRefEdge(network, target, replacedElement, model.getId());

                    AttributeUtil.set(network, target, SBML.ATTR_COMP_SUBMODELREF, replacedElement.getSubmodelRef(), String.class);
                    if (replacedElement.isSetConversionFactor()){
                        // FIXME
                        // replacedElement.getConversionFactor();
                    }
                    if (replacedElement.isSetDeletion()){
                        // replacedElement.getDeletion();
                    }

                    /*
                    When deletion is set, it means the ReplacedElement object is actually an annotation to indicate that the replacement object
                    replaces something deleted from a submodel. The use of the deletion attribute overrides the use of the attributes
                    inherited from SBaseRef: instead of using, e.g., portRef or idRef, the ReplacedElement instance sets deletion to
                    the identifier of the Deletion object. In addition, the referenced Deletion must be a child of the Submodel referenced
                    by the submodelRef attribute
                    */

                }
                if (compSBase.isSetReplacedBy()){
                    ReplacedBy replacedBy = compSBase.getReplacedBy();
                    logger.info(replacedBy.toString());
                    CyNode target = createNode(network, replacedBy, SBML.NODETYPE_COMP_REPLACED_BY);
                    setSBaseRefAttributes(network, target, replacedBy);

                    AttributeUtil.set(network, target, SBML.LABEL,
                            String.format("%s replacedBy", sbase.getMetaId()), String.class);

                    // edge to replacing element
                    createEdge(network, source, target, SBML.INTERACTION_COMP_SBASE_REPLACED_BY);
                    createSBaseRefEdge(network, target, replacedBy, model.getId());
                }
            }
        }

    }

    /**
     * Creates the edge for the given source node and sBaseRef.
     *
     * Finds the target of the SbaseRef and adds the edge to it.
     *  @param network
     * @param sbaseNode
     * @param sBaseRef
     */
    private void createSBaseRefEdge(CyNetwork network, CyNode sbaseNode, SBaseRef sBaseRef, String model){

        String submodel = null;
        // necessary to check if sBaseRef in own model
        if (sBaseRef instanceof ReplacedElement){
            ReplacedElement replacedElement = (ReplacedElement) sBaseRef;
            submodel = replacedElement.getSubmodelRef();
        } else if (sBaseRef instanceof ReplacedBy){
            ReplacedBy replacedBy = (ReplacedBy) sBaseRef;
            submodel = replacedBy.getSubmodelRef();
        }
        // empty submodel points to same model
        if (submodel != null && submodel.length()==0){
            submodel = model;
        }


        CyNode target = null;
        String interaction = null;

        // link to other submodel component
        if (submodel != null && !submodel.equals(model)){
            logger.warn("SBaseRef to other submodel. Link not created.");
        } else {
            if (sBaseRef.isSetPortRef()) {
                String portRef = sBaseRef.getPortRef();
                target = AttributeUtil.getNodeByAttribute(network, SBML.ATTR_PORT_SID, portRef);
                interaction = SBML.INTERACTION_COMP_SBASEREF_PORT;
            } else if (sBaseRef.isSetIdRef()) {
                String idRef = sBaseRef.getIdRef();
                target = AttributeUtil.getNodeByAttribute(network, SBML.ATTR_ID, idRef);
                interaction = SBML.INTERACTION_COMP_SBASEREF_ID;
            } else if (sBaseRef.isSetUnitRef()) {
                String unitRef = sBaseRef.getUnitRef();
                target = AttributeUtil.getNodeByAttribute(network, SBML.ATTR_UNIT_SID, unitRef);
                interaction = SBML.INTERACTION_COMP_SBASEREF_UNIT;
            } else if (sBaseRef.isSetMetaIdRef()) {
                String metaIdRef = sBaseRef.getMetaIdRef();
                target = AttributeUtil.getNodeByAttribute(network, SBML.ATTR_METAID, metaIdRef);
                interaction = SBML.INTERACTION_COMP_SBASEREF_METAID;
            }

            // handle the recursive case
            // FIXME:
            // if (port.isSetSBaseRef()){
            //     port.getSBaseRef();
            // }

            createEdge(network, sbaseNode, target, interaction);
        }

    }

    ////////////////////////////////////////////////////////////////////////////
    // SBML GROUPS
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Create groups.
     * Groups are implemented as group nodes.
     * <p>
     * A CyGroup is created either as an empty group
     * CyGroup emptyGroup = groupFactory.createGroup(network, true);
     * or by turning an existing node into an empty group:
     * CyGroup emptyGroup = groupFactory.createGroup(network, node, true);
     */
    private void readGroups(CyNetwork network, Model model) {
        logger.debug("<groups>");

        GroupsModelPlugin groupsModel = (GroupsModelPlugin) model.getExtension(GroupsConstants.namespaceURI);
        if (groupsModel == null) {
            return;
        }

        for (Group group : groupsModel.getListOfGroups()) {
            logger.debug(String.format("Reading group: <%s>", group));

            // empty group node & sets attributes
            CyGroup cyGroup = createGroup(network, group);

            // collect nodes from members
            List<CyNode> nodes = new LinkedList<>();
            ListOfMembers membersList = group.getListOfMembers();
            for (Member member : membersList) {

                // resolve object & node
                SBase sbase = member.getSBaseInstance();
                CyNode memberNode = metaId2Node.get(sbase.getMetaId());

                if (memberNode != null) {
                    nodes.add(memberNode);
                } else {
                    logger.error(String.format("Member <%s> of group <%s> not found via metaId.", group, member));
                }

                // Information transfer to members

                // Unlike most lists of objects in SBML, the sboTerm attribute and the Notes
                // and Annotation children are taken from the ListOfMembers to apply directly to every
                // SBML element referenced by each child Member of this ListOfMembers,
                // if that referenced element has no such definition.
                // Thus, if a referenced element has no defined sboTerm, child Notes, or child Annotation,
                // that element should be considered to now have the sboTerm, child Notes, or child Annotation of the ListOfMembers.

                // ! this changes the SBMLDocument
                if (membersList.isSetSBOTerm() && !sbase.isSetSBOTerm()) {
                    sbase.setSBOTerm(membersList.getSBOTerm());
                }
                if (membersList.isSetNotes() && !sbase.isSetNotes()) {
                    sbase.setNotes(membersList.getNotes());
                }
                if (membersList.isSetAnnotation() && !sbase.isSetAnnotation()) {
                    sbase.setAnnotation(membersList.getAnnotation());
                }
            }
            logger.debug(String.format("Adding %s nodes to cyGroup", nodes.size()));
            cyGroup.addNodes(nodes);
        }

    }


    ////////////////////////////////////////////////////////////////////////////
    // SBML SBML_LAYOUT
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Creates the layouts stored in the layout extension.
     * TODO: implement
     */
    private void readLayouts(CyNetwork network, Model model) {
        logger.debug("<layout>");

        LayoutModelPlugin layoutModel = (LayoutModelPlugin) model.getExtension(LayoutConstants.namespaceURI);
        QualModelPlugin qualModel = (QualModelPlugin) model.getExtension(QualConstants.namespaceURI);

        if (layoutModel != null){
            logger.warn("Layouts found, but not yet supported.");
            for (Layout layout : layoutModel.getListOfLayouts()) {
                // layoutNetwork = rootNetwork.addSubNetwork();
                // readLayout(model, qualModel, layout);
            }
        }
    }

    /**
     * Read single layout.
     */
    private void readLayout(CyNetwork network, Model model, QualModelPlugin qualModel, Layout layout) {

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
            CyNode n = createNode(network, glyph, SBML.NODETYPE_LAYOUT_SPECIESGLYPH);
            setNamedSBaseAttributes(network, n, glyph);

            // get species node and copyInputStream information
            if (glyph.isSetSpecies()) {
                Species species = (Species) glyph.getSpeciesInstance();
                if (metaId2Node.containsKey(species.getMetaId())) {
                    CyNode sNode = metaId2Node.get(species.getMetaId());

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
     * Creates SBML node for SBase
     * Only function to create node in the network. The node is stored
     * in the node mapping and the corresponding node attribute is set.
     *
     * @param sbase    sbase to add node for
     * @param sbmlType SBML type of the node
     * @return
     */
    private CyNode createNode(CyNetwork network, SBase sbase, String sbmlType) {
        CyNode n = network.addNode();
        // Set unique metaId
        MappingUtil.setSBaseMetaId(document, sbase);
        // Set attributes
        String metaId = sbase.getMetaId();
        AttributeUtil.set(network, n, SBML.ATTR_CYID, metaId, String.class);
        AttributeUtil.set(network, n, SBML.NODETYPE_ATTR, sbmlType, String.class);
        AttributeUtil.set(network, n, SBML.LABEL, metaId, String.class);
        // store nodes
        metaId2Node.put(metaId, n);
        if (sbase instanceof NamedSBase) {
            NamedSBase nsb = (NamedSBase) sbase;
            if (nsb.isSetId()) {
                id2Node.put(nsb.getId(), n);
            }
        }
        return n;
    }

    /**
     * Creates group node for the given group.
     *
     * @param group
     * @return
     */
    private CyGroup createGroup(CyNetwork network, Group group) {
        // metaId for identification
        MappingUtil.setSBaseMetaId(document, group);

        CyGroup cyGroup = groupFactory.createGroup(network, true);
        cyGroupSet.add(cyGroup);

        // set attributes
        //  cyGroup nodes are registered in the root network, so the corresponding
        //  attributes must be set on the root network.
        CyNode n = cyGroup.getGroupNode();
        String metaId = group.getMetaId();
        CyRootNetwork rootNetwork = ((CySubNetwork) network).getRootNetwork();
        AttributeUtil.set(rootNetwork, n, SBML.ATTR_CYID, metaId, String.class);
        AttributeUtil.set(rootNetwork, n, SBML.NODETYPE_ATTR, SBML.NODETYPE_GROUP, String.class);
        AttributeUtil.set(rootNetwork, n, SBML.LABEL, metaId, String.class);
        setNamedSBaseAttributes(rootNetwork, n, group);

        // store nodes
        metaId2Node.put(metaId, n);
        if (group.isSetId()) {
            id2Node.put(group.getId(), n);
        }
        return cyGroup;
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
    private CyEdge createEdge(CyNetwork network, CyNode source, CyNode target, String interactionType) {
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
    private CyEdge createUnitEdge(CyNetwork network, CyNode n, QuantityWithUnit q) {
        CyEdge e = null;

        // edge to unit
        if (q.isSetUnits()) {
            // Every time a new unit instance is created for base units !
            UnitDefinition ud = q.getUnitsInstance();
            CyNode udNode = metaId2Node.get(ud.getMetaId());
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
            if (ud != null && udNode == null) {
                logger.debug(String.format("Base UnitDefinition encountered. Creating UnitDefinition graph.", ud));
                String unitSid = ud.getId();
                if (baseUnitDefinitions.containsKey(unitSid)) {
                    // This base unit was encountered before and the network created
                    ud = baseUnitDefinitions.get(unitSid);
                } else {
                    // The base unit must be stored for later lookup
                    createUnitDefinitionGraph(network, ud);
                    baseUnitDefinitions.put(unitSid, ud);
                }
                // get the unique node
                udNode = metaId2Node.get(ud.getMetaId());
            }
            // now the udNode should exist for sure
            if (udNode != null) {
                e = createEdge(network, n, udNode, SBML.INTERACTION_SBASE_UNITDEFINITION);
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
    private void createUnitDefinitionGraph(CyNetwork network, UnitDefinition ud) {
        CyNode n = createNode(network, ud, SBML.NODETYPE_UNIT_DEFINITION);
        setNamedSBaseAttributes(network, n, ud);

        for (Unit unit : ud.getListOfUnits()) {
            if (ud.isSetId() && unit.isSetKind()) {
                CyNode uNode = createNode(network, unit, SBML.NODETYPE_UNIT);
                setUnitAttributes(network, uNode, unit);

                // edge to UnitDefinition
                createEdge(network, uNode, n, SBML.INTERACTION_UNIT_UNITDEFINITION);
            } else {
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
    private static void setSBaseAttributes(CyNetwork network, CyIdentifiable n, SBase sbase) {
        if (sbase.isSetSBOTerm()) {
            AttributeUtil.set(network, n, SBML.ATTR_SBOTERM, sbase.getSBOTermID(), String.class);
        }
        if (sbase.isSetMetaId()) {
            AttributeUtil.set(network, n, SBML.ATTR_METAID, sbase.getMetaId(), String.class);
        }
        // RDF attributes
        // This creates Cytoscape attributes from the CV terms
        Properties props = AnnotationUtil.parseCVTerms(sbase);
        for (Object key : props.keySet()) {
            String keyString = key.toString();
            String valueString = props.getProperty((String) key);
            AttributeUtil.set(network, n, keyString, valueString, String.class);
        }
        // COBRA attributes
        if ((sbase instanceof Reaction) || (sbase instanceof Species)) {
            Properties cobraProps = CobraUtil.parseCobraNotes(sbase);
            props.putAll(cobraProps);
        }
        // create attributes for properties
        for (Object key : props.keySet()) {
            String keyString = key.toString();
            String valueString = props.getProperty((String) key);
            AttributeUtil.set(network, n, keyString, valueString, String.class);
        }
    }


    /**
     * Set attributes for NamedSBase.
     *
     * @param n   CyIdentifiable to set attributes on
     * @param nsb NamedSBase
     * @return
     */
    private static void setNamedSBaseAttributes(CyNetwork network, CyIdentifiable n, NamedSBase nsb) {
        setSBaseAttributes(network, n, nsb);
        if (nsb.isSetId()) {
            String id = nsb.getId();
            // set in the correct namespace
            if (nsb instanceof UnitDefinition || nsb instanceof Unit) {
                AttributeUtil.set(network, n, SBML.ATTR_UNIT_SID, id, String.class);
            }
            else if (nsb instanceof Port) {
                AttributeUtil.set(network, n, SBML.ATTR_PORT_SID, id, String.class);
            } else {
                AttributeUtil.set(network, n, SBML.ATTR_ID, id, String.class);
            }
            AttributeUtil.set(network, n, SBML.LABEL, id, String.class);
        }
        if (nsb.isSetName()) {
            String name = nsb.getName();
            AttributeUtil.set(network, n, SBML.ATTR_NAME, name, String.class);
            AttributeUtil.set(network, n, SBML.LABEL, name, String.class);
        }
    }

    /**
     * Set attributes for SBaseRef.
     * Attributes are mutually exclusive.
     *
     * @param network
     * @param n
     * @param sbaseRef
     */
    private static void setSBaseRefAttributes(CyNetwork network, CyIdentifiable n, SBaseRef sbaseRef) {
        if (sbaseRef.isSetPortRef()) {
            AttributeUtil.set(network, n, SBML.ATTR_COMP_PORTREF, sbaseRef.getPortRef(), String.class);
        }
        else if (sbaseRef.isSetIdRef()) {
            AttributeUtil.set(network, n, SBML.ATTR_COMP_IDREF, sbaseRef.getIdRef(), String.class);
        }
        else if (sbaseRef.isSetUnitRef()) {
            AttributeUtil.set(network, n, SBML.ATTR_COMP_UNITREF, sbaseRef.getUnitRef(), String.class);
        }
        else if (sbaseRef.isSetMetaIdRef()) {
            AttributeUtil.set(network, n, SBML.ATTR_COMP_METAIDREF, sbaseRef.getMetaIdRef(), String.class);
        }
    }

    /**
     * Set attributes for NamedSBaseWithDerivedUnit.
     *
     * @param n
     * @param nsbu
     */
    private static void setNamedSBaseWithDerivedUnitAttributes(CyNetwork network, CyIdentifiable n, NamedSBaseWithDerivedUnit nsbu) {
        setNamedSBaseAttributes(network, n, nsbu);
        AttributeUtil.set(network, n, SBML.ATTR_DERIVED_UNITS, nsbu.getDerivedUnits(), String.class);
    }

    /**
     * Set attributes for QuantityWithUnit.
     * e.g. LocalParameters.
     *
     * @param n
     * @param q
     */
    private static void setQuantityWithUnitAttributes(CyNetwork network, CyIdentifiable n, QuantityWithUnit q) {
        setNamedSBaseWithDerivedUnitAttributes(network, n, q);
        if (q.isSetValue()) {
            AttributeUtil.set(network, n, SBML.ATTR_VALUE, q.getValue(), Double.class);
        }
        if (q.isSetUnits()) {
            AttributeUtil.set(network, n, SBML.ATTR_UNITS, q.getUnits(), String.class);
        }
    }

    /**
     * Set attributes for Symbol, e.g. Species or Parameters.
     *
     * @param network
     * @param n
     * @param symbol
     */
    private static void setSymbolNodeAttributes(CyNetwork network, CyIdentifiable n, Symbol symbol) {
        setQuantityWithUnitAttributes(network, n, symbol);
        if (symbol.isSetConstant()) {
            AttributeUtil.set(network, n, SBML.ATTR_CONSTANT, symbol.getConstant(), Boolean.class);
        }
    }

    /**
     * Set attributes for AbstractMathContainer.
     * Direct known subclasses
     * AnalyticVolume,
     * Constraint,
     * Delay,
     * EventAssignment,
     * FunctionDefinition,
     * FunctionTerm,
     * Index,
     * InitialAssignment,
     * KineticLaw,
     * Priority,
     * Rule,
     * StoichiometryMath,
     * Trigger
     *
     * @param n
     * @param container
     */
    private void setAbstractMathContainerNodeAttributes(CyNetwork network, CyIdentifiable n, AbstractMathContainer container) {
        setSBaseAttributes(network, n, container);

        AttributeUtil.set(network, n, SBML.ATTR_DERIVED_UNITS, container.getDerivedUnits(), String.class);
        if (container.isSetMath()) {
            ASTNode astNode = container.getMath();
            AttributeUtil.set(network, n, SBML.ATTR_MATH, astNode.toFormula(), String.class);
        }
    }

    /**
     * Set attributes for unit.
     *
     * @param n
     * @param u
     */
    private void setUnitAttributes(CyNetwork network, CyIdentifiable n, Unit u) {
        setSBaseAttributes(network, n, u);

        String kind = u.getKind().toString();
        AttributeUtil.set(network, n, SBML.LABEL, kind, String.class);
        AttributeUtil.set(network, n, SBML.ATTR_UNIT_KIND, kind, String.class);
        if (u.isSetExponent()) {
            AttributeUtil.set(network, n, SBML.ATTR_UNIT_EXPONENT, u.getExponent(), Double.class);
        }
        if (u.isSetScale()) {
            AttributeUtil.set(network, n, SBML.ATTR_UNIT_SCALE, u.getScale(), Integer.class);
        }
        if (u.isSetMultiplier()) {
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
    private void createMathNetwork(CyNetwork network, AbstractMathContainer container, CyNode containerNode, String edgeType) {
        if (container.isSetMath()) {
            ASTNode astNode = container.getMath();
            AttributeUtil.set(network, containerNode, SBML.ATTR_MATH, astNode.toFormula(), String.class);

            // Get the refenced objects in math.
            // This can be parameters, localParameters, species, ...
            // create edge if node exists
            for (NamedSBase nsb : ASTNodeUtil.findReferencedNamedSBases(astNode)) {
                CyNode nsbNode = metaId2Node.get(nsb.getMetaId());

                if (nsbNode != null) {
                    createEdge(network, nsbNode, containerNode, edgeType);
                } else {
                    logger.warn("Node for metaId <" + nsb.getMetaId() + "> not found in math <" + astNode.toFormula() + ">");
                }
            }
        }
    }


    //////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Adds integer compartment codes as node attribute.
     * <p>
     * The compartmentCodes can be used in the visual mapping for dynamical
     * visualization of compartment colors.
     */
    private void addCompartmentCodes(CyNetwork network, Model model) {
        // Calculate compartment code mapping
        HashMap<String, Integer> compartmentCodes = new HashMap<String, Integer>();
        Integer compartmentCode = 1;
        for (Compartment c : model.getListOfCompartments()) {
            String cid = c.getId();
            if (!compartmentCodes.containsKey(cid)) {
                compartmentCodes.put(cid, compartmentCode);
                compartmentCode += 1;
            }
        }
        // set compartment code attribute
        for (CyNode n : network.getNodeList()) {
            String cid = AttributeUtil.get(network, n, SBML.ATTR_COMPARTMENT, String.class);
            Integer code = compartmentCodes.get(cid);
            AttributeUtil.set(network, n, SBML.ATTR_COMPARTMENT_CODE, code, Integer.class);
        }
    }

    /**
     * Adds extended sbml types as node attributes.
     * <p>
     * The extended SBML types can be used in the visual mapping.
     * This allows for instance to distinguish reversible and irreversible reactions.
     */
    private void addSBMLTypesExtended(CyNetwork network, Model model) {
        for (CyNode n : network.getNodeList()) {
            String type = AttributeUtil.get(network, n, SBML.NODETYPE_ATTR, String.class);
            if (type == null) {
                logger.error(String.format("SBML.NODETYPE_ATTR not set for SBML node: %s", n));
            } else {
                // additional subtypes
                if (type.equals(SBML.NODETYPE_REACTION)) {
                    Boolean reversible = AttributeUtil.get(network, n, SBML.ATTR_REVERSIBLE, Boolean.class);
                    if (reversible) {
                        type = SBML.NODETYPE_REACTION_REVERSIBLE;
                    } else {
                        type = SBML.NODETYPE_REACTION_IRREVERSIBLE;
                    }
                }
                AttributeUtil.set(network, n, SBML.NODETYPE_ATTR_EXTENDED, type, String.class);
            }

        }
    }

    /**
     * Adds extended sbml interaction as edge attributes.
     * <p>
     * The extended SBML types can be used in the visual mapping.
     * This allows for instance to distinguish modifiers from activators and inhibitors.
     */
    private void addSBMLInteractionExtended(CyNetwork network) {
        for (CyEdge e : network.getEdgeList()) {
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

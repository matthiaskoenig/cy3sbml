package org.cy3sbml.reader;

import java.io.InputStream;
import java.util.*;
import javax.swing.tree.TreeNode;
import javax.xml.stream.XMLStreamException;
import org.cy3sbml.SBML;
import org.cy3sbml.SBMLManager;
import org.cy3sbml.SBMLReaderError;
import org.cy3sbml.mapping.One2ManyMapping;
import org.cy3sbml.styles.StyleManager;
import org.cy3sbml.util.*;
import org.cy3sbml.util.filter.SBaseFilter;
import org.cytoscape.group.CyGroup;
import org.cytoscape.group.CyGroupFactory;
import org.cytoscape.io.read.CyNetworkReader;
import org.cytoscape.model.CyEdge;
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
import org.cytoscape.work.Tunable;
import org.cytoscape.work.swing.RequestsUIHelper;
import org.cytoscape.work.swing.TunableUIHelper;
import org.sbml.jsbml.*;
import org.sbml.jsbml.ext.comp.*;
import org.sbml.jsbml.ext.groups.*;
import org.sbml.jsbml.ext.layout.LayoutConstants;
import org.sbml.jsbml.ext.layout.LayoutModelPlugin;
import org.sbml.jsbml.ext.qual.FunctionTerm;
import org.sbml.jsbml.ext.qual.Input;
import org.sbml.jsbml.ext.qual.Output;
import org.sbml.jsbml.ext.qual.QualConstants;
import org.sbml.jsbml.ext.qual.QualModelPlugin;
import org.sbml.jsbml.ext.qual.QualitativeSpecies;
import org.sbml.jsbml.ext.qual.Transition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The SBMLReaderTask creates CyNetworks from SBMLDocuments.
 * <p>
 * The reader creates the master SBML network graph with various subnetworks
 * created from the full graph.
 */
public class SBMLReaderTask extends AbstractTask implements CyNetworkReader, RequestsUIHelper {
    private static final Logger logger = LoggerFactory.getLogger(SBMLReaderTask.class);

    @Tunable(description = "Tick if you want to automatically layout the imported network")
    public boolean doLayout = true;

    private final String fileName;
    private final InputStream stream;
    private final CyNetworkFactory networkFactory;
    private final CyGroupFactory groupFactory;
    private final CyNetworkViewFactory viewFactory;
    private final VisualMappingManager visualMappingManager;
    private final CyLayoutAlgorithmManager cyLayoutAlgorithmManager;
    private final CyProperty<Properties> cy3sbmlProperties;

    private SBMLDocument document;

    private List<CyNetwork> cyNetworks;
    private TaskMonitor taskMonitor;

    private Boolean error = false;

    /**
     * Constructor
     */
    public SBMLReaderTask(
            InputStream stream,
            String fileName,
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
        cyNetworks = new ArrayList<>();
    }

    /**
     * Testing constructor.
     */
    public SBMLReaderTask(
            InputStream stream, String fileName, CyNetworkFactory networkFactory, CyGroupFactory groupFactory) {
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
     * <p>
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
        if (doLayout) {
            if (cyLayoutAlgorithmManager != null) {
                CyLayoutAlgorithm layout = cyLayoutAlgorithmManager.getLayout(SBML.SBML_LAYOUT);
                if (layout == null) {
                    layout = cyLayoutAlgorithmManager.getLayout(CyLayoutAlgorithmManager.DEFAULT_LAYOUT_NAME);
                    logger.warn("'{}' layout not found; will use the default one.", SBML.SBML_LAYOUT);
                }
                TaskIterator itr = layout.createTaskIterator(
                        view, layout.getDefaultLayoutContext(), CyLayoutAlgorithm.ALL_NODE_VIEWS, "");
                Task nextTask = itr.next();
                try {
                    nextTask.run(taskMonitor);
                } catch (Exception e) {
                    throw new RuntimeException("Could not finish layout", e);
                }
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
    public static One2ManyMapping<String, Long> mappingFromNetwork(
            CyNetwork network, One2ManyMapping<String, Long> mapping) {
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
    public void cancel() {}

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

            // TODO: create a SBMLDocument network (containing the ModelDefinitions & External ModelDefinitions, and
            // submodels)

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
            throw new SBMLReaderError("cy3sbml reader failed to build a SBML model. "
                    + "Please validate the file in the online SBML validator at 'http://www.sbml.org/validator/'"
                    + "and report the issue at 'https://github.com/matthiaskoenig/cy3sbml/issues'"
                    + t);
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // Networks
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Creates network for given model.
     * This can be the main model or an external model definition.
     */
    private void createNetworksFromModel(Model model) {
        // different models can have same metaIds and ids, so every model
        // is read with its own context
        ConversionContext context = readModelInNetwork(model);
        // Create the different subnetworks
        addAllNetworks(context.network(), context.groups());
    }

    /**
     * Creates the given model into a network.
     * The network is the master network containing all nodes and edges belonging
     * to the model.
     */
    private ConversionContext readModelInNetwork(Model model) {
        // new network
        CyNetwork network = networkFactory.createNetwork();
        ConversionContext context = new ConversionContext(document, network, groupFactory);

        // <core>
        readCore(context, model);
        if (taskMonitor != null) {
            taskMonitor.setProgress(0.4);
        }
        // <qual>
        readQual(context, model);
        // <fbc>
        new FbcReader().read(context, model);
        // <comp>
        readComp(context, model);
        // <groups>
        readGroups(context, model);
        // <layout>
        readLayouts(model);

        // Add compartment codes dynamically for colors
        addCompartmentCodes(network, model);
        addSBMLTypesExtended(network);
        addSBMLInteractionExtended(network);
        return context;
    }

    /**
     * Adds all networks to the list of base networks.
     */
    private void addAllNetworks(CyNetwork network, Set<CyGroup> cyGroupSet) {

        // root network
        CyRootNetwork rootNetwork = ((CySubNetwork) network).getRootNetwork();
        String name = getNetworkName(network);
        // String name = AttributeUtil.get(network, network, SBML.ATTR_ID, String.class);
        rootNetwork.getRow(rootNetwork).set(CyNetwork.NAME, String.format("%s", name));

        // all network
        network.getRow(network).set(CyNetwork.NAME, String.format("%s__%s", SBML.PREFIX_SUBNETWORK_ALL, name));

        // Kinetic network
        CyNetwork kineticNetwork = addSubNetwork(rootNetwork, network, SBML.kineticNodeTypes, SBML.kineticEdgeTypes);
        kineticNetwork
                .getRow(kineticNetwork)
                .set(CyNetwork.NAME, String.format("%s__%s", SBML.PREFIX_SUBNETWORK_KINETIC, name));

        // base network
        CyNetwork baseNetwork = addSubNetwork(rootNetwork, network, SBML.coreNodeTypes, SBML.coreEdgeTypes);
        baseNetwork.getRow(baseNetwork).set(CyNetwork.NAME, name);

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
            cyNetworks.add(kineticNetwork);
            cyNetworks.add(baseNetwork);
        }
    }

    /**
     * Adds a subnetwork to the network.
     */
    private static CyNetwork addSubNetwork(
            CyRootNetwork rootNetwork, CyNetwork network, List<String> nodeTypes, List<String> edgeTypes) {
        // O(1) lookup (collect nodes and edges)
        Set<CyNode> coreNodes = getNetworkNodes(network, new HashSet<>(nodeTypes));
        Set<CyEdge> coreEdges = getNetworkEdges(network, new HashSet<>(edgeTypes));
        // only add edges with nodes in the nodes list
        HashSet<CyEdge> filteredEdges = new HashSet<>();
        for (CyEdge e : coreEdges) {
            if (coreNodes.contains(e.getSource()) && coreNodes.contains(e.getTarget())) {
                filteredEdges.add(e);
            }
        }
        return rootNetwork.addSubNetwork(coreNodes, filteredEdges);
    }

    /**
     * Get network edges with given edge types.
     */
    private static Set<CyEdge> getNetworkEdges(CyNetwork network, Set<String> edgeTypes) {
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
     */
    private static Set<CyNode> getNetworkNodes(CyNetwork network, Set<String> nodeTypes) {
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
        String name = network.getRow(network).get(SBML.ATTR_ID, String.class);
        if (name == null) {
            String[] tokens = fileName.split("/\\\\", -1);
            name = tokens[tokens.length - 1];
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
     */
    private void readCore(ConversionContext context, Model model) {
        CyNetwork network = context.network();
        logger.debug("<core>");

        // SBMLDocument & Model //
        // Mark network as SBML
        AttributeUtil.set(network, network, SBML.NETWORKTYPE_ATTR, SBML.NETWORKTYPE_SBML, String.class);
        AttributeUtil.set(
                network,
                network,
                SBML.LEVEL_VERSION,
                String.format("L%1$s V%2$s", document.getLevel(), document.getVersion()),
                String.class);

        // metaId, SBO, id, name
        AttributeWriter.setNamedSBaseAttributes(network, network, model);

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
            UnitGraphBuilder.createUnitDefinitionGraph(context, ud);
        }

        // FunctionDefinition //
        for (FunctionDefinition fd : model.getListOfFunctionDefinitions()) {

            // implements both interfaces
            CyNode n = context.createNode(fd, SBML.NODETYPE_FUNCTION_DEFINITION);
            AttributeWriter.setNamedSBaseAttributes(network, n, fd);
            AttributeWriter.setAbstractMathContainerNodeAttributes(network, n, fd);

            // Do not create the math network for the function definition
            // The objects of the FunctionDefinition ASTNode can have different naming conventions
            // than the objects, i.e. a lambda(x), does not mean that it is called with
            // an object x
            // MathGraphBuilder.createMathNetwork(fd, fdNode, SBML.INTERACTION_REFERENCE_FUNCTIONDEFINITION);
        }

        // Compartment //
        for (Compartment compartment : model.getListOfCompartments()) {
            CyNode n = context.createNode(compartment, SBML.NODETYPE_COMPARTMENT);
            AttributeWriter.setSymbolNodeAttributes(network, n, compartment);
            // edge to unit
            UnitGraphBuilder.createUnitEdge(context, n, compartment);

            if (compartment.isSetSpatialDimensions()) {
                AttributeUtil.set(
                        network, n, SBML.ATTR_SPATIAL_DIMENSIONS, compartment.getSpatialDimensions(), Double.class);
            }
            if (compartment.isSetSize()) {
                AttributeUtil.set(network, n, SBML.ATTR_SIZE, compartment.getSize(), Double.class);
            }
        }

        // Parameter //
        for (Parameter parameter : model.getListOfParameters()) {
            CyNode n = context.createNode(parameter, SBML.NODETYPE_PARAMETER);
            AttributeWriter.setSymbolNodeAttributes(network, n, parameter);
            // edge to unit
            UnitGraphBuilder.createUnitEdge(context, n, parameter);
        }

        // Species //
        for (Species species : model.getListOfSpecies()) {
            CyNode n = context.createNode(species, SBML.NODETYPE_SPECIES);
            AttributeWriter.setSymbolNodeAttributes(network, n, species);
            // edge to unit
            UnitGraphBuilder.createUnitEdge(context, n, species);

            // edge to compartment
            if (species.isSetCompartment()) {
                AttributeUtil.set(network, n, SBML.ATTR_COMPARTMENT, species.getCompartment(), String.class);
                Compartment comp = species.getCompartmentInstance();
                if (comp != null) {
                    CyNode compNode = context.nodeByMetaId(comp.getMetaId()).orElse(null);
                    context.createEdge(n, compNode, SBML.INTERACTION_SPECIES_COMPARTMENT);
                } else {
                    logger.error(String.format(
                            "Compartment does not exist for species: %s for %s",
                            species.getCompartment(), species.getId()));
                }
            }
            if (species.isSetBoundaryCondition()) {
                AttributeUtil.set(
                        network, n, SBML.ATTR_BOUNDARY_CONDITION, species.getBoundaryCondition(), Boolean.class);
            }
            if (species.isSetHasOnlySubstanceUnits()) {
                AttributeUtil.set(
                        network,
                        n,
                        SBML.ATTR_HAS_ONLY_SUBSTANCE_UNITS,
                        species.getHasOnlySubstanceUnits(),
                        Boolean.class);
            }
            if (species.isSetCharge()) {
                // reason: charge is deprecated in SBML L3, but still read from older SBML levels
                @SuppressWarnings("deprecation")
                int charge = species.getCharge();
                AttributeUtil.set(network, n, SBML.ATTR_CHARGE, charge, Integer.class);
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
                AttributeUtil.set(
                        network, n, SBML.ATTR_INITIAL_CONCENTRATION, species.getInitialConcentration(), Double.class);
            }
        }

        // Reaction //
        for (Reaction reaction : model.getListOfReactions()) {
            CyNode n = context.createNode(reaction, SBML.NODETYPE_REACTION);
            AttributeWriter.setNamedSBaseWithDerivedUnitAttributes(network, n, reaction);

            if (reaction.isSetReversible()) {
                AttributeUtil.set(network, n, SBML.ATTR_REVERSIBLE, reaction.getReversible(), Boolean.class);
            } else {
                // reversible=true by default
                AttributeUtil.set(network, n, SBML.ATTR_REVERSIBLE, true, Boolean.class);
            }
            if (reaction.isSetFast()) {
                // reason: fast is deprecated in SBML L3V2, but still read from older SBML versions
                @SuppressWarnings("deprecation")
                boolean fast = reaction.getFast();
                AttributeUtil.set(network, n, SBML.ATTR_FAST, fast, Boolean.class);
            }
            if (reaction.isSetCompartment()) {
                AttributeUtil.set(network, n, SBML.ATTR_COMPARTMENT, reaction.getCompartment(), String.class);
                Compartment comp = reaction.getCompartmentInstance();
                if (comp != null) {
                    // edge to compartment
                    CyNode compNode = context.nodeByMetaId(comp.getMetaId()).orElse(null);
                    context.createEdge(n, compNode, SBML.INTERACTION_REACTION_COMPARTMENT);
                } else {
                    logger.error(String.format(
                            "Compartment does not exist for reaction: %s for %s",
                            reaction.getCompartment(), reaction.getId()));
                }
            }

            // Reactants
            for (SpeciesReference speciesRef : reaction.getListOfReactants()) {
                Species species = speciesRef.getSpeciesInstance();
                if (species != null) {
                    CyNode reactantNode =
                            context.nodeByMetaId(species.getMetaId()).orElse(null);
                    CyEdge edge = context.createEdge(n, reactantNode, SBML.INTERACTION_REACTION_REACTANT);
                    AttributeWriter.setSBaseAttributes(network, edge, speciesRef);

                    Double stoichiometry = speciesRef.isSetStoichiometry() ? speciesRef.getStoichiometry() : 1.0;
                    AttributeUtil.set(network, edge, SBML.ATTR_STOICHIOMETRY, stoichiometry, Double.class);
                } else {
                    logger.error(String.format(
                            "Reactant does not exist for reaction: %s for %s",
                            speciesRef.getSpecies(), reaction.getId()));
                }
            }
            // Products
            for (SpeciesReference speciesRef : reaction.getListOfProducts()) {
                Species species = speciesRef.getSpeciesInstance();

                if (species != null) {
                    CyNode productNode =
                            context.nodeByMetaId(species.getMetaId()).orElse(null);
                    CyEdge edge = context.createEdge(n, productNode, SBML.INTERACTION_REACTION_PRODUCT);
                    AttributeWriter.setSBaseAttributes(network, edge, speciesRef);

                    Double stoichiometry = speciesRef.isSetStoichiometry() ? speciesRef.getStoichiometry() : 1.0;
                    AttributeUtil.set(network, edge, SBML.ATTR_STOICHIOMETRY, stoichiometry, Double.class);
                } else {
                    logger.error(String.format(
                            "Product does not exist for reaction: %s for %s",
                            speciesRef.getSpecies(), reaction.getId()));
                }
            }
            // Modifiers
            for (ModifierSpeciesReference msRef : reaction.getListOfModifiers()) {
                Species species = msRef.getSpeciesInstance();
                if (species != null) {
                    CyNode modifierNode =
                            context.nodeByMetaId(species.getMetaId()).orElse(null);
                    CyEdge edge = context.createEdge(n, modifierNode, SBML.INTERACTION_REACTION_MODIFIER);
                    AttributeWriter.setSBaseAttributes(network, edge, msRef);
                } else {
                    logger.error(String.format(
                            "ModifierSpecies does not exist for reaction: %s for %s",
                            msRef.getSpecies(), reaction.getId()));
                }
            }

            // Kinetic law
            if (reaction.isSetKineticLaw()) {
                KineticLaw law = reaction.getKineticLaw();
                CyNode lawNode = context.createNode(law, SBML.NODETYPE_KINETIC_LAW);
                AttributeWriter.setAbstractMathContainerNodeAttributes(network, lawNode, law);
                AttributeUtil.set(network, lawNode, SBML.LABEL, reaction.getId(), String.class);

                // edge to reaction
                CyEdge edge = network.addEdge(n, lawNode, true);
                AttributeUtil.set(
                        network, edge, SBML.INTERACTION_ATTR, SBML.INTERACTION_REACTION_KINETICLAW, String.class);

                // local parameter nodes
                if (law.isSetListOfLocalParameters()) {
                    for (LocalParameter lp : law.getListOfLocalParameters()) {
                        // This changes the SBMLDocument !
                        // but only reliable way to handle LocalParameters in math networks
                        String lpId = MappingUtil.localParameterId(lp);
                        lp.setId(lpId);

                        CyNode lpNode = context.createNode(lp, SBML.NODETYPE_LOCAL_PARAMETER);
                        AttributeWriter.setQuantityWithUnitAttributes(network, lpNode, lp);
                        // edge to unit
                        UnitGraphBuilder.createUnitEdge(context, lpNode, lp);

                        // edge to law
                        context.createEdge(lpNode, lawNode, SBML.INTERACTION_LOCALPARAMETER_KINETICLAW);
                    }
                }

                // referenced nodes in math
                if (law.isSetMath()) {
                    // set math on reaction
                    AttributeUtil.set(
                            network, n, SBML.ATTR_KINETIC_LAW, law.getMath().toFormula(), String.class);
                    MathGraphBuilder.createMathNetwork(context, law, lawNode, SBML.INTERACTION_REFERENCE_KINETICLAW);
                } else {
                    logger.warn(String.format("No math set for kinetic law in reaction: %s", reaction.getId()));
                }
            }
        }

        // InitialAssignment //
        for (InitialAssignment assignment : model.getListOfInitialAssignments()) {
            Variable variable = assignment.getVariableInstance();
            if (variable != null) {
                CyNode assignmentNode = context.createNode(assignment, SBML.NODETYPE_INITIAL_ASSIGNMENT);
                AttributeWriter.setAbstractMathContainerNodeAttributes(network, assignmentNode, assignment);
                AttributeUtil.set(network, assignmentNode, SBML.ATTR_VARIABLE, variable.getId(), String.class);

                // edge to variable
                CyNode variableNode = context.nodeByMetaId(variable.getMetaId()).orElse(null);
                if (variableNode != null) {
                    context.createEdge(variableNode, assignmentNode, SBML.INTERACTION_VARIABLE_INITIAL_ASSIGNMENT);
                    if (assignment.isSetMath()) {
                        ASTNode astNode = assignment.getMath();
                        AttributeUtil.set(
                                network, variableNode, SBML.ATTR_INITIAL_ASSIGNMENT, astNode.toFormula(), String.class);
                    }
                } else {
                    logger.warn(String.format(
                            "Variable is neither Compartment, Species or Parameter, probably SpeciesReference: %s in %s",
                            variable, assignment));
                }
                // referenced nodes in math
                MathGraphBuilder.createMathNetwork(
                        context, assignment, assignmentNode, SBML.INTERACTION_REFERENCE_INITIAL_ASSIGNMENT);

            } else {
                logger.error(String.format(
                        "Variable does not exist for InitialAssignment: %s for %s", assignment.getVariable(), "?"));
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

            CyNode n = context.createNode(rule, ruleType);
            AttributeWriter.setAbstractMathContainerNodeAttributes(network, n, rule);
            // referenced nodes in math
            MathGraphBuilder.createMathNetwork(context, rule, n, SBML.INTERACTION_REFERENCE_RULE);

            String label = SBMLUtil.TEMPLATE_ALGEBRAIC_RULE;
            // edge to variable for rateRule and assignmentRule
            if (variable != null) {
                if (rule instanceof AssignmentRule) {
                    label = String.format(SBMLUtil.TEMPLATE_ASSIGNMENT_RULE, variable.getId());
                } else if (rule instanceof RateRule) {
                    label = String.format(SBMLUtil.TEMPLATE_RATE_RULE, variable.getId());
                }
                AttributeUtil.set(network, n, SBML.ATTR_VARIABLE, variable.getId(), String.class);

                CyNode variableNode = context.nodeByMetaId(variable.getMetaId()).orElse(null);
                if (variableNode != null) {
                    context.createEdge(variableNode, n, SBML.INTERACTION_VARIABLE_RULE);
                } else {
                    //  An assignment rule can refer to the identifier of a Species, SpeciesReference,
                    //    Compartment, or global Parameter object in the model
                    //    The case SpeciesReference is not handled !
                    logger.warn(String.format(
                            "Variable is neither Compartment, Species or Parameter, probably SpeciesReference: %s in %s",
                            variable, rule));
                }
            }
            AttributeUtil.set(network, n, SBML.LABEL, label, String.class);
        }

        // Constraints
        // No models with constraints exist for testing.
        for (Constraint constraint : model.getListOfConstraints()) {
            CyNode n = context.createNode(constraint, SBML.NODETYPE_CONSTRAINT);
            AttributeWriter.setAbstractMathContainerNodeAttributes(network, n, constraint);
            if (constraint.isSetMessage()) {
                try {
                    AttributeUtil.set(network, n, SBML.ATTR_MESSAGE, constraint.getMessageString(), String.class);
                } catch (XMLStreamException e) {
                    logger.error("Message string could not be created for constraint.", e);
                    e.printStackTrace();
                }
            }
        }

        // Events
        for (Event event : model.getListOfEvents()) {

            CyNode n = context.createNode(event, SBML.NODETYPE_EVENT);
            AttributeWriter.setNamedSBaseWithDerivedUnitAttributes(network, n, event);

            if (event.isSetUseValuesFromTriggerTime()) {
                AttributeUtil.set(
                        network,
                        n,
                        SBML.ATTR_USE_VALUES_FROM_TRIGGER_TIME,
                        event.getUseValuesFromTriggerTime(),
                        Boolean.class);
            }
            // edge via trigger math
            if (event.isSetTrigger()) {
                MathGraphBuilder.createMathNetwork(context, event.getTrigger(), n, SBML.INTERACTION_TRIGGER_EVENT);
            }
            // edge via priority math
            if (event.isSetPriority()) {
                MathGraphBuilder.createMathNetwork(context, event.getPriority(), n, SBML.INTERACTION_PRIORITY_EVENT);
            }
            // edge via delay math
            if (event.isSetDelay()) {
                MathGraphBuilder.createMathNetwork(context, event.getDelay(), n, SBML.INTERACTION_PRIORITY_EVENT);
            }

            for (EventAssignment ea : event.getListOfEventAssignments()) {
                CyNode eaNode = context.createNode(ea, SBML.NODETYPE_EVENT_ASSIGNMENT);
                AttributeWriter.setAbstractMathContainerNodeAttributes(network, eaNode, ea);

                // edge to event
                context.createEdge(n, eaNode, SBML.INTERACTION_EVENT_EVENT_ASSIGNMENT);

                // edge to variable
                if (ea.isSetVariable()) {
                    Variable variable = ea.getVariableInstance();
                    CyNode variableNode =
                            context.nodeByMetaId(variable.getMetaId()).orElse(null);
                    AttributeUtil.set(network, eaNode, SBML.LABEL, variable.getId(), String.class);
                    AttributeUtil.set(network, eaNode, SBML.ATTR_VARIABLE, variable.getId(), String.class);

                    if (variableNode != null) {
                        context.createEdge(variableNode, eaNode, SBML.INTERACTION_VARIABLE_EVENT_ASSIGNMENT);
                    } else {
                        //  An assignment rule can refer to the identifier of a Species, SpeciesReference,
                        //  Compartment, or global Parameter object in the model
                        //  The case SpeciesReference is not handled !

                        logger.warn(String.format(
                                "Variable is neither Compartment, Species or Parameter, probably SpeciesReference: %s in %s",
                                variable, ea));
                    }
                } else {
                    logger.error("Variable not set in EventAssignment: " + ea);
                }

                // referenced nodes in math
                MathGraphBuilder.createMathNetwork(context, ea, eaNode, SBML.INTERACTION_REFERENCE_EVENT_ASSIGNMENT);
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // SBML QUAL
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Create nodes, edges and attributes from Qualitative Model.
     */
    private void readQual(ConversionContext context, Model model) {
        CyNetwork network = context.network();
        logger.debug("<qual>");

        QualModelPlugin qualModel = (QualModelPlugin) model.getExtension(QualConstants.namespaceURI);
        if (qualModel == null) {
            return;
        }

        // QualSpecies //
        for (QualitativeSpecies qSpecies : qualModel.getListOfQualitativeSpecies()) {
            CyNode n = context.createNode(qSpecies, SBML.NODETYPE_QUAL_SPECIES);
            AttributeWriter.setNamedSBaseAttributes(network, n, qSpecies);

            if (qSpecies.isSetCompartment()) {
                AttributeUtil.set(network, n, SBML.ATTR_COMPARTMENT, qSpecies.getCompartment(), String.class);
                // edge to compartment
                Compartment comp = qSpecies.getCompartmentInstance();
                CyNode compNode = context.nodeByMetaId(comp.getMetaId()).orElse(null);
                context.createEdge(n, compNode, SBML.INTERACTION_SPECIES_COMPARTMENT);
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
            CyNode n = context.createNode(transition, SBML.NODETYPE_QUAL_TRANSITION);
            AttributeWriter.setNamedSBaseAttributes(network, n, transition);

            // Inputs
            for (Input input : transition.getListOfInputs()) {
                String qSpeciesId = input.getQualitativeSpecies();
                QualitativeSpecies qSpecies = qualModel.getQualitativeSpecies(qSpeciesId);

                CyNode inNode = context.nodeByMetaId(qSpecies.getMetaId()).orElse(null);
                CyEdge e = context.createEdge(n, inNode, SBML.INTERACTION_QUAL_TRANSITION_INPUT);

                // required (no checking of required -> NullPointerException risk)
                AttributeUtil.set(
                        network,
                        e,
                        SBML.ATTR_QUAL_TRANSITION_EFFECT,
                        input.getTransitionEffect().toString(),
                        String.class);
                AttributeUtil.set(
                        network,
                        e,
                        SBML.ATTR_QUAL_QUALITATIVE_SPECIES,
                        input.getQualitativeSpecies().toString(),
                        String.class);
                // optional
                if (input.isSetId()) {
                    AttributeUtil.set(network, e, SBML.ATTR_ID, input.getId(), String.class);
                }
                if (input.isSetName()) {
                    AttributeUtil.set(network, e, SBML.ATTR_NAME, input.getName(), String.class);
                }
                if (input.isSetSign()) {
                    AttributeUtil.set(
                            network, e, SBML.ATTR_QUAL_SIGN, input.getSign().toString(), String.class);
                }
                if (input.isSetSBOTerm()) {
                    AttributeUtil.set(network, e, SBML.ATTR_SBOTERM, input.getSBOTermID(), String.class);
                }
                if (input.isSetMetaId()) {
                    AttributeUtil.set(network, e, SBML.ATTR_METAID, input.getMetaId(), String.class);
                }
                if (input.isSetThresholdLevel()) {
                    AttributeUtil.set(
                            network, e, SBML.ATTR_QUAL_THRESHOLD_LEVEL, input.getThresholdLevel(), Integer.class);
                }
            }

            // Outputs
            for (Output output : transition.getListOfOutputs()) {
                String qSpeciesString = output.getQualitativeSpecies();
                QualitativeSpecies qSpecies = qualModel.getQualitativeSpecies(qSpeciesString);
                CyNode outNode = context.nodeByMetaId(qSpecies.getMetaId()).orElse(null);
                CyEdge e = context.createEdge(n, outNode, SBML.INTERACTION_QUAL_TRANSITION_OUTPUT);

                // required
                AttributeUtil.set(
                        network,
                        e,
                        SBML.ATTR_QUAL_QUALITATIVE_SPECIES,
                        output.getQualitativeSpecies().toString(),
                        String.class);
                AttributeUtil.set(
                        network,
                        e,
                        SBML.ATTR_QUAL_TRANSITION_EFFECT,
                        output.getTransitionEffect().toString(),
                        String.class);
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
                List<Integer> resultLevels = new ArrayList<Integer>();
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

    ////////////////////////////////////////////////////////////////////////////
    // SBML COMP
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Create network information from comp model.
     */
    private void readComp(ConversionContext context, Model model) {
        CyNetwork network = context.network();
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
        logger.debug("<Submodel>");
        for (Submodel submodel : compModel.getListOfSubmodels()) {

            logger.debug(submodel.toString());
            CyNode n = context.createNode(submodel, SBML.NODETYPE_COMP_SUBMODEL);
            AttributeWriter.setNamedSBaseAttributes(network, n, submodel);

            AttributeUtil.set(network, n, SBML.ATTR_COMP_MODELREF, submodel.getModelRef(), String.class);
            if (submodel.isSetTimeConversionFactor()) {
                AttributeUtil.set(
                        network,
                        n,
                        SBML.ATTR_COMP_TIME_CONVERSION_FACTOR,
                        submodel.getTimeConversionFactor(),
                        String.class);
            }
            if (submodel.isSetExtentConversionFactor()) {
                AttributeUtil.set(
                        network,
                        n,
                        SBML.ATTR_COMP_EXTENT_CONVERSION_FACTOR,
                        submodel.getExtentConversionFactor(),
                        String.class);
            }

            // Deletion
            for (Deletion deletion : submodel.getListOfDeletions()) {
                // TODO: add edge
                logger.debug(deletion.toString());
                CyNode nd = context.createNode(deletion, SBML.NODETYPE_COMP_DELETION);
                AttributeWriter.setNamedSBaseAttributes(network, nd, deletion);

                // SbaseRef
                // TODO
                deletion.getIdRef();
            }

            // TODO: generic method for getting node for SbaseRef
            // SBaseRef provides attributes portRef, idRef, unitRef 12
            // and metaIdRef, and a recursive subcomponent, sBaseRef
        }

        // Port //
        logger.debug("<Port>");
        // create port nodes
        for (Port port : compModel.getListOfPorts()) {
            logger.debug(port.toString());
            CyNode n = context.createNode(port, SBML.NODETYPE_COMP_PORT);
            AttributeWriter.setNamedSBaseAttributes(network, n, port);
            AttributeWriter.setSBaseRefAttributes(network, n, port);
        }
        // create port edges
        for (Port port : compModel.getListOfPorts()) {
            CyNode source = AttributeUtil.getNodeByAttribute(network, SBML.ATTR_PORT_SID, port.getId());
            createSBaseRefEdge(context, source, port, model.getId());
        }

        logger.debug("<ReplacedElement & ReplacedBy>");
        // only sbases in current model
        for (TreeNode node : model.filter(new SBaseFilter())) {
            SBase sbase = (SBase) node;
            CompSBasePlugin compSBase = (CompSBasePlugin) sbase.getExtension(CompConstants.namespaceURI);
            if (compSBase != null) {
                logger.debug(compSBase.toString());

                CyNode source = AttributeUtil.getNodeByAttribute(network, SBML.ATTR_CYID, sbase.getMetaId());

                // replacedElements (SBaseRef)
                for (ReplacedElement replacedElement : compSBase.getListOfReplacedElements()) {

                    logger.debug(replacedElement.toString());
                    //
                    // targets can be from other submodels
                    CyNode target = context.createNode(replacedElement, SBML.NODETYPE_COMP_REPLACED_ELEMENT);
                    AttributeWriter.setSBaseRefAttributes(network, target, replacedElement);

                    String ref = getRefFromSBaseRef(replacedElement);
                    String submodel = "";
                    if (replacedElement.isSetSubmodelRef()) {
                        submodel = replacedElement.getSubmodelRef();
                    }

                    AttributeUtil.set(
                            network, target, SBML.LABEL, String.format("<%s:%s>", submodel, ref), String.class);

                    // edge to replacing element
                    context.createEdge(source, target, SBML.INTERACTION_COMP_SBASE_REPLACED_ELEMENT);

                    createSBaseRefEdge(context, target, replacedElement, model.getId());

                    AttributeUtil.set(
                            network,
                            target,
                            SBML.ATTR_COMP_SUBMODELREF,
                            replacedElement.getSubmodelRef(),
                            String.class);
                    if (replacedElement.isSetConversionFactor()) {
                        // FIXME
                        // replacedElement.getConversionFactor();
                    }
                    if (replacedElement.isSetDeletion()) {
                        // FIXME
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

                // replacedBy
                if (compSBase.isSetReplacedBy()) {
                    ReplacedBy replacedBy = compSBase.getReplacedBy();
                    logger.debug(replacedBy.toString());
                    CyNode target = context.createNode(replacedBy, SBML.NODETYPE_COMP_REPLACED_BY);
                    AttributeWriter.setSBaseRefAttributes(network, target, replacedBy);

                    String ref = getRefFromSBaseRef(replacedBy);
                    String submodel = "";
                    if (replacedBy.isSetSubmodelRef()) {
                        submodel = replacedBy.getSubmodelRef();
                    }

                    AttributeUtil.set(
                            network, target, SBML.LABEL, String.format("<%s:%s>", submodel, ref), String.class);

                    // edge to replacing element
                    context.createEdge(source, target, SBML.INTERACTION_COMP_SBASE_REPLACED_BY);
                    createSBaseRefEdge(context, target, replacedBy, model.getId());
                }

                // deletion
            }
        }
    }

    /**
     * Get String of referenced element from port.
     *
     * @return String
     */
    private String getRefFromSBaseRef(SBaseRef sbaseRef) {

        String ref = "";
        if (sbaseRef.isSetPortRef()) {
            ref = sbaseRef.getPortRef();
        } else if (sbaseRef.isSetIdRef()) {
            ref = sbaseRef.getIdRef();
        } else if (sbaseRef.isSetUnitRef()) {
            ref = sbaseRef.getUnitRef();
        } else if (sbaseRef.isSetMetaIdRef()) {
            ref = sbaseRef.getMetaIdRef();
        }
        return ref;
    }

    /**
     * Creates the edge for the given source node and sBaseRef.
     * <p>
     * Finds the target of the SbaseRef and adds the edge to it.
     */
    private void createSBaseRefEdge(ConversionContext context, CyNode sbaseNode, SBaseRef sBaseRef, String model) {
        CyNetwork network = context.network();

        String submodel = null;
        // necessary to check if sBaseRef in own model
        if (sBaseRef instanceof ReplacedElement replacedElement) {
            submodel = replacedElement.getSubmodelRef();
        } else if (sBaseRef instanceof ReplacedBy replacedBy) {
            submodel = replacedBy.getSubmodelRef();
        }
        // empty submodel points to same model
        if (submodel != null && submodel.length() == 0) {
            submodel = model;
        }

        CyNode target = null;
        String interaction = null;

        // link to other submodel component
        if (submodel != null && !submodel.equals(model)) {
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

            context.createEdge(sbaseNode, target, interaction);
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
    private void readGroups(ConversionContext context, Model model) {
        logger.debug("<groups>");

        GroupsModelPlugin groupsModel = (GroupsModelPlugin) model.getExtension(GroupsConstants.namespaceURI);
        if (groupsModel == null) {
            return;
        }

        for (Group group : groupsModel.getListOfGroups()) {
            logger.debug(String.format("Reading group: <%s>", group));

            // empty group node & sets attributes
            CyGroup cyGroup = context.createGroup(group);

            // collect nodes from members
            List<CyNode> nodes = new ArrayList<>();
            ListOfMembers membersList = group.getListOfMembers();
            for (Member member : membersList) {

                // resolve object & node
                SBase sbase = member.getSBaseInstance();
                CyNode memberNode = context.nodeByMetaId(sbase.getMetaId()).orElse(null);

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
                // that element should be considered to now have the sboTerm, child Notes, or child Annotation of the
                // ListOfMembers.

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
    private void readLayouts(Model model) {
        logger.debug("<layout>");

        LayoutModelPlugin layoutModel = (LayoutModelPlugin) model.getExtension(LayoutConstants.namespaceURI);

        if (layoutModel != null) {
            logger.warn("Layouts found, but not yet supported.");
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // HELPER FUNCTIONS
    ////////////////////////////////////////////////////////////////////////////

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
    private void addSBMLTypesExtended(CyNetwork network) {
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

    @Override
    public void setUIHelper(TunableUIHelper arg0) {
        // TODO Auto-generated method stub
    }
}

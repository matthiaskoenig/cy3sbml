package org.cy3sbml.reader;

import java.io.InputStream;
import java.util.*;
import org.cy3sbml.SBML;
import org.cy3sbml.SBMLManager;
import org.cy3sbml.SBMLReaderError;
import org.cy3sbml.mapping.One2ManyMapping;
import org.cy3sbml.styles.StyleManager;
import org.cy3sbml.util.*;
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
        new CoreReader().read(context, model);
        if (taskMonitor != null) {
            taskMonitor.setProgress(0.4);
        }
        // <qual>
        new QualReader().read(context, model);
        // <fbc>
        new FbcReader().read(context, model);
        // <comp>
        new CompReader().read(context, model);
        // <groups>
        new GroupsReader().read(context, model);
        // <layout>
        new LayoutReader().read(context, model);

        // Add compartment codes dynamically for colors
        CoreReader.addDerivedAttributes(network, model);
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
    // SBML QUAL
    ////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////
    // SBML FBC
    ////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////
    // SBML COMP
    ////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////
    // SBML GROUPS
    ////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////
    // SBML SBML_LAYOUT
    ////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////
    // HELPER FUNCTIONS
    ////////////////////////////////////////////////////////////////////////////

    //////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void setUIHelper(TunableUIHelper arg0) {
        // TODO Auto-generated method stub
    }
}

package org.cy3sbml.reader;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.cy3sbml.SBML;
import org.cy3sbml.SBMLManager;
import org.cy3sbml.SBMLReaderError;
import org.cy3sbml.mapping.One2ManyMapping;
import org.cy3sbml.styles.StyleManager;
import org.cytoscape.group.CyGroupFactory;
import org.cytoscape.io.read.CyNetworkReader;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNode;
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
import org.sbml.jsbml.JSBML;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.ext.comp.CompConstants;
import org.sbml.jsbml.ext.comp.CompSBMLDocumentPlugin;
import org.sbml.jsbml.ext.comp.ExternalModelDefinition;
import org.sbml.jsbml.ext.comp.ModelDefinition;
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

    private final InputStream stream;
    private final CyNetworkFactory networkFactory;
    private final CyGroupFactory groupFactory;
    private final CyNetworkViewFactory viewFactory;
    private final VisualMappingManager visualMappingManager;
    private final CyLayoutAlgorithmManager cyLayoutAlgorithmManager;
    private final CyProperty<Properties> cy3sbmlProperties;

    private final List<PackageReader> readers;
    private final SubnetworkBuilder subnetworkBuilder;

    private SBMLDocument document;

    private final List<CyNetwork> cyNetworks;
    private TaskMonitor taskMonitor;

    private Boolean error = false;

    /** Creates the reader for the SBML in the stream. */
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
        this.networkFactory = networkFactory;
        this.groupFactory = cyGroupFactory;
        this.viewFactory = viewFactory;
        this.visualMappingManager = visualMappingManager;
        this.cyLayoutAlgorithmManager = cyLayoutAlgorithmManager;
        this.cy3sbmlProperties = cy3sbmlProperties;

        // package readers in the order they are applied to every model
        readers = List.of(
                new CoreReader(),
                new QualReader(),
                new FbcReader(),
                new CompReader(),
                new GroupsReader(),
                new LayoutReader(),
                // attributes derived from the complete network, must be last
                new DerivedAttributes());
        subnetworkBuilder = new SubnetworkBuilder(fileName);

        cyNetworks = new ArrayList<>();
    }

    /** Creates the reader without view, style and layout support, e.g. for tests. */
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
        // SBMLManager is only available in the OSGI context
        SBMLManager sbmlManager = SBMLManager.getInstance();
        if (sbmlManager != null) {
            // the existing mapping (of read networks) is updated
            One2ManyMapping<String, Long> mapping = mappingFromNetwork(network, sbmlManager.getMapping(network));
            sbmlManager.addSBMLForNetwork(document, network, mapping);
            sbmlManager.updateCurrent(network);
        } else {
            logger.warn("No mapping found for SBML network.");
        }

        CyNetworkView view = viewFactory.createNetworkView(network);
        // VisualMappingManager only available in OSGI context
        if (visualMappingManager != null) {
            String styleName = (String) cy3sbmlProperties.getProperties().get(SBML.PROPERTY_VISUAL_STYLE);
            VisualStyle style = StyleManager.getVisualStyleByName(visualMappingManager, styleName);
            if (style != null) {
                visualMappingManager.setVisualStyle(style, view);
            }
        }

        if (doLayout && cyLayoutAlgorithmManager != null) {
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
            } catch (Exception e) { // Task.run declares Exception
                throw new RuntimeException("Could not finish layout", e);
            }
        }
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
        for (CyNode node : rootNetwork.getNodeList()) {
            String cyId = rootNetwork.getRow(node).get(SBML.ATTR_CYID, String.class);
            mapping.put(cyId, node.getSUID());
        }
        return mapping;
    }

    /** Returns true if reading failed. */
    public Boolean getError() {
        return error;
    }

    /** Reads the SBML and creates the networks for its models. */
    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        this.taskMonitor = taskMonitor;
        try {
            if (taskMonitor != null) {
                taskMonitor.setTitle("cy3sbml reader");
                taskMonitor.setProgress(0.0);
            }
            if (cancelled) {
                return;
            }

            logger.debug("JSBML version: " + JSBML.getJSBMLVersionString());
            // the XML parser decodes the stream with the encoding of the XML declaration
            document = SBMLReader.read(stream);

            // Models are defined either as the core model or as comp ModelDefinitions.
            // For every model a separate network is created.
            if (document.isSetModel()) {
                createNetworksFromModel(document.getModel());
            } else {
                logger.warn("No core model in SBMLDocument! Check model definition.");
            }
            CompSBMLDocumentPlugin compDoc = (CompSBMLDocumentPlugin) document.getExtension(CompConstants.namespaceURI);
            if (compDoc != null) {
                readModelDefinitions(compDoc);
            }
            // no network of the flattened comp model yet (#401)

            if (cancelled) {
                // a cancelled import returns no networks, not the ones read so far
                cyNetworks.clear();
                return;
            }
            if (taskMonitor != null) {
                taskMonitor.setProgress(0.8);
            }
        } catch (Throwable t) {
            logger.error("Could not read SBML into Cytoscape!", t);
            error = true;
            // never return a partial set of networks
            cyNetworks.clear();
            String message = "cy3sbml reader failed to build a SBML model. "
                    + "Please validate the file in the online SBML validator at 'http://www.sbml.org/validator/' "
                    + "and report the issue at 'https://github.com/matthiaskoenig/cy3sbml/issues': " + t;
            if (taskMonitor != null) {
                taskMonitor.showMessage(TaskMonitor.Level.ERROR, message);
            }
            throw new SBMLReaderError(message, t);
        }
    }

    /**
     * Creates the networks for the comp ModelDefinitions.
     * ExternalModelDefinitions are not supported, their models must be loaded from the source.
     */
    private void readModelDefinitions(CompSBMLDocumentPlugin compDoc) {
        logger.info("<ExternalModelDefinition>");
        for (ExternalModelDefinition emd : compDoc.getListOfExternalModelDefinitions()) {
            logger.warn("Model reading from ExternalModelDefinition not supported: " + emd);
        }

        logger.info("<ModelDefinition>");
        for (ModelDefinition md : compDoc.getListOfModelDefinitions()) {
            logger.info("ModelDefinition: " + md.toString());
            Model mdModel = md.getModel();
            if (mdModel != null) {
                createNetworksFromModel(mdModel);
                logger.info("creating model for: " + md.getModel().getId());
            } else {
                logger.error("Model could not be read from ModelDefinition: " + md);
            }
        }
    }

    /**
     * Creates the networks for the given model.
     * This can be the main model or a comp ModelDefinition.
     * <p>
     * The network with all nodes and edges of the model is read by the package
     * readers, then the subnetworks are created from it. Different models can have
     * the same metaIds and ids, so every model is read with its own context.
     */
    private void createNetworksFromModel(Model model) {
        if (cancelled) {
            return;
        }
        CyNetwork network = networkFactory.createNetwork();
        ConversionContext context = new ConversionContext(document, network, groupFactory);
        for (PackageReader reader : readers) {
            reader.read(context, model);
        }
        if (taskMonitor != null) {
            taskMonitor.setProgress(0.4);
        }

        CyRootNetwork rootNetwork = ((CySubNetwork) network).getRootNetwork();
        cyNetworks.addAll(subnetworkBuilder.build(rootNetwork, network, context.groups()));
    }

    /** The reader has no custom UI. */
    @Override
    public void setUIHelper(TunableUIHelper helper) {}
}

package org.cy3sbml.reader;

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import javax.xml.stream.XMLStreamException;
import org.cy3sbml.SBML;
import org.cy3sbml.SBMLManager;
import org.cy3sbml.SBMLReaderError;
import org.cy3sbml.comp.CompModels;
import org.cy3sbml.comp.ModelResolution;
import org.cy3sbml.comp.SBaseRefResolver;
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
import org.sbml.jsbml.ext.comp.CompModelPlugin;
import org.sbml.jsbml.ext.comp.CompSBMLDocumentPlugin;
import org.sbml.jsbml.ext.comp.ModelDefinition;
import org.sbml.jsbml.ext.comp.Submodel;
import org.sbml.jsbml.ext.comp.util.CompFlatteningConverter;
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
    private final SBMLManager sbmlManager;

    private final String fileName;
    // location of the file, to resolve the relative sources of external model definitions
    private final URI location;
    private final List<PackageReader> readers;
    private final SubnetworkBuilder subnetworkBuilder;

    private SBMLDocument document;
    // resolves the comp references of the document, shared by the networks of its models
    private SBaseRefResolver sBaseRefResolver;

    private final List<CyNetwork> cyNetworks;
    // the document of the model of each network collection, by the SUID of the root network
    private final Map<Long, SBMLDocument> documents = new HashMap<>();
    // the model of each network collection, by the SUID of the root network
    private final Map<Long, Model> models = new HashMap<>();
    private TaskMonitor taskMonitor;

    private Boolean error = false;

    /** Creates the reader for the SBML in the stream. */
    public SBMLReaderTask(
            InputStream stream,
            String fileName,
            URI location,
            CyNetworkFactory networkFactory,
            CyGroupFactory cyGroupFactory,
            CyNetworkViewFactory viewFactory,
            VisualMappingManager visualMappingManager,
            CyLayoutAlgorithmManager cyLayoutAlgorithmManager,
            CyProperty<Properties> cy3sbmlProperties,
            SBMLManager sbmlManager) {

        this.stream = stream;
        this.fileName = fileName;
        this.location = location;
        this.networkFactory = networkFactory;
        this.groupFactory = cyGroupFactory;
        this.viewFactory = viewFactory;
        this.visualMappingManager = visualMappingManager;
        this.cyLayoutAlgorithmManager = cyLayoutAlgorithmManager;
        this.cy3sbmlProperties = cy3sbmlProperties;
        this.sbmlManager = sbmlManager;

        // package readers in the order they are applied to every model
        readers = List.of(
                new CoreReader(),
                new QualReader(),
                new FbcReader(),
                new CompReader(),
                new GroupsReader(),
                new DistribReader(),
                new LayoutReader(),
                // attributes derived from the complete network, must be last
                new DerivedAttributes());
        subnetworkBuilder = new SubnetworkBuilder(fileName);

        cyNetworks = new ArrayList<>();
    }

    /** Creates the reader without view, style, layout and SBMLManager support, e.g. for tests. */
    public SBMLReaderTask(
            InputStream stream, String fileName, CyNetworkFactory networkFactory, CyGroupFactory groupFactory) {
        this(stream, fileName, null, networkFactory, groupFactory);
    }

    /**
     * Creates the reader for the file at the location without view, style, layout and
     * SBMLManager support, e.g. for tests.
     */
    public SBMLReaderTask(
            InputStream stream,
            String fileName,
            URI location,
            CyNetworkFactory networkFactory,
            CyGroupFactory groupFactory) {
        this(stream, fileName, location, networkFactory, groupFactory, null, null, null, null, null);
    }

    /** The location of the file, if known. */
    public Optional<URI> getLocation() {
        return Optional.ofNullable(location);
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
        if (sbmlManager != null) {
            // the existing mapping (of read networks) is updated
            One2ManyMapping<String, Long> mapping = mappingFromNetwork(network, sbmlManager.getMapping(network));
            CyRootNetwork rootNetwork = ((CySubNetwork) network).getRootNetwork();
            sbmlManager.addSBMLForNetwork(documents.getOrDefault(rootNetwork.getSUID(), document), network, mapping);
            sbmlManager.addSBaseRefResolver(sBaseRefResolver);
            Model model = models.get(rootNetwork.getSUID());
            if (model != null) {
                sbmlManager.addModelForNetwork(network, model);
            }
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

            logger.debug("JSBML version: {}", JSBML.getJSBMLVersionString());
            // the XML parser decodes the stream with the encoding of the XML declaration
            document = SBMLReader.read(stream);
            if (location != null) {
                document.setLocationURI(location.toString());
            }
            sBaseRefResolver = new SBaseRefResolver(new CompModels(document));

            // one network collection for every model: the main model, the comp model
            // definitions, the models of external model definitions and the flat model
            for (ModelSource source : modelSources()) {
                createNetworksFromModel(source);
            }

            if (cancelled) {
                // a cancelled import returns no networks, not the ones read so far
                cyNetworks.clear();
                documents.clear();
                models.clear();
                return;
            }
            if (taskMonitor != null) {
                taskMonitor.setProgress(0.8);
            }
        } catch (Throwable t) {
            error = true;
            // never return a partial set of networks
            cyNetworks.clear();
            documents.clear();
            models.clear();
            // Cytoscape shows the message of the thrown error to the user
            String message = String.format(
                    "cy3sbml could not read the SBML file '%s': %s. Check the file with the SBML validator at "
                            + "https://sbml.org/facilities/validator/ and report the problem at "
                            + "https://github.com/matthiaskoenig/cy3sbml/issues if the file is valid.",
                    fileName, describe(t));
            // the error keeps the cause, Cytoscape logs it with its stack trace
            logger.error(message);
            throw new SBMLReaderError(message, t);
        }
    }

    /**
     * Short description of a read failure for the user: the first line of the message and,
     * for XML errors, the line in the file.
     */
    private static String describe(Throwable t) {
        String message = t.getMessage();
        if (message == null || message.isBlank()) {
            return t.getClass().getSimpleName();
        }
        String description = message.strip().lines().findFirst().orElse(message).strip();
        if (t instanceof XMLStreamException xmlError && xmlError.getLocation() != null) {
            description += " (line " + xmlError.getLocation().getLineNumber() + ")";
        }
        return description;
    }

    /**
     * The models of the document, each read into its own network collection: the main
     * model, the comp model definitions, the models of the external model definitions
     * (also of the ones in the external files, each model once) and the flat model if the
     * main model has submodels.
     */
    private List<ModelSource> modelSources() {
        List<ModelSource> sources = new ArrayList<>();
        if (document.isSetModel()) {
            sources.add(new ModelSource(document.getModel(), document, ModelSource.Kind.MAIN));
        } else {
            logger.warn("No core model in SBMLDocument! Check model definition.");
        }
        if (!(document.getExtension(CompConstants.shortLabel) instanceof CompSBMLDocumentPlugin compDocument)) {
            return sources;
        }
        for (ModelDefinition modelDefinition : compDocument.getListOfModelDefinitions()) {
            sources.add(new ModelSource(modelDefinition, document, ModelSource.Kind.MODEL_DEFINITION));
        }
        // the models read so far, an external model definition can refer back to them
        Set<Model> externalModels = Collections.newSetFromMap(new IdentityHashMap<>());
        sources.forEach(source -> externalModels.add(source.model()));
        for (CompModels.External external : sBaseRefResolver.models().externalModels()) {
            if (external.resolution() instanceof ModelResolution.Resolved resolved) {
                if (externalModels.add(resolved.model())) {
                    sources.add(new ModelSource(resolved.model(), resolved.document(), ModelSource.Kind.EXTERNAL));
                }
            } else if (external.resolution() instanceof ModelResolution.Failed failed) {
                logger.warn(
                        "The external model definition '{}' could not be read: {}",
                        external.definition().getId(),
                        failed.reason());
            }
        }
        flatModel().ifPresent(sources::add);
        return sources;
    }

    /**
     * The flat model of the main model, if it has submodels and all of them can be
     * instantiated.
     */
    private Optional<ModelSource> flatModel() {
        if (!document.isSetModel() || !hasSubmodels(document.getModel())) {
            return Optional.empty();
        }
        Optional<String> unresolved =
                unresolvedSubmodel(document.getModel(), List.of(), Collections.newSetFromMap(new IdentityHashMap<>()));
        if (unresolved.isPresent()) {
            logger.warn("The flat model is not created, a submodel cannot be instantiated: {}", unresolved.get());
            return Optional.empty();
        }
        try {
            SBMLDocument flat = new CompFlatteningConverter().flatten(document);
            return Optional.of(new ModelSource(flat.getModel(), flat, ModelSource.Kind.FLAT));
        } catch (RuntimeException e) {
            // the JSBML flattening fails for a model that instantiates itself, for example
            logger.warn("The flat model could not be created: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private static boolean hasSubmodels(Model model) {
        return model.getExtension(CompConstants.shortLabel) instanceof CompModelPlugin plugin
                && plugin.getSubmodelCount() > 0;
    }

    /**
     * The reason a submodel of the model or of its submodels cannot be instantiated: its
     * model cannot be resolved, or the submodels instantiate each other.
     *
     * @param path the models on the way to the model, to detect a cycle
     * @param checked the models whose submodels can all be instantiated
     */
    // models by identity, JSBML's equals compares the content
    @SuppressWarnings("ReferenceEquality")
    private Optional<String> unresolvedSubmodel(Model model, List<Model> path, Set<Model> checked) {
        if (checked.contains(model)
                || !(model.getExtension(CompConstants.shortLabel) instanceof CompModelPlugin plugin)) {
            return Optional.empty();
        }
        if (path.stream().anyMatch(m -> m == model)) {
            return Optional.of(String.format("The submodels of the model '%s' instantiate it again.", model.getId()));
        }
        List<Model> next = new ArrayList<>(path);
        next.add(model);
        for (Submodel submodel : plugin.getListOfSubmodels()) {
            ModelResolution resolution = sBaseRefResolver.models().resolve(submodel);
            if (resolution instanceof ModelResolution.Failed failed) {
                return Optional.of(failed.reason());
            }
            Optional<String> nested =
                    unresolvedSubmodel(((ModelResolution.Resolved) resolution).model(), next, checked);
            if (nested.isPresent()) {
                return nested;
            }
        }
        checked.add(model);
        return Optional.empty();
    }

    /**
     * Creates the networks for the model of the source.
     * <p>
     * The network with all nodes and edges of the model is read by the package
     * readers, then the subnetworks are created from it. Different models can have
     * the same metaIds and ids, so every model is read with its own context.
     */
    private void createNetworksFromModel(ModelSource source) {
        if (cancelled) {
            return;
        }
        CyNetwork network = networkFactory.createNetwork();
        ConversionContext context = new ConversionContext(source.document(), network, groupFactory, sBaseRefResolver);
        for (PackageReader reader : readers) {
            reader.read(context, source.model());
        }
        if (taskMonitor != null) {
            taskMonitor.setProgress(0.4);
        }

        CyRootNetwork rootNetwork = ((CySubNetwork) network).getRootNetwork();
        String prefix = source.kind() == ModelSource.Kind.FLAT ? SBML.PREFIX_NETWORK_FLAT : null;
        cyNetworks.addAll(subnetworkBuilder.build(rootNetwork, network, context::createGroups, prefix));
        // after the subnetworks, the layout networks copy the finished nodes of the model
        String name = rootNetwork.getRow(rootNetwork).get(CyNetwork.NAME, String.class);
        cyNetworks.addAll(new LayoutNetworkBuilder(context).build(rootNetwork, name));
        documents.put(rootNetwork.getSUID(), source.document());
        models.put(rootNetwork.getSUID(), source.model());
    }

    /** The reader has no custom UI. */
    @Override
    public void setUIHelper(TunableUIHelper helper) {}
}

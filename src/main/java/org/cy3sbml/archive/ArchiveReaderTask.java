package org.cy3sbml.archive;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.cy3sbml.ServiceAdapter;
import org.cy3sbml.gui.WebViewPanel;
import org.cy3sbml.styles.StyleManager;
import org.cy3sbml.util.AttributeUtil;
import org.cytoscape.io.read.CyNetworkReader;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Create CyNetworks from Archives.
 * <p>
 * This can be either a COMBINE Archive or ResearchObject,
 * or any other file type supported by taverna robundle.
 */
public class ArchiveReaderTask extends AbstractTask implements CyNetworkReader {
    private static final Logger logger = LoggerFactory.getLogger(ArchiveReaderTask.class);

    public static final String ARCHIVE_LAYOUT = "force-directed";
    public static final String ARCHIVE_STYLE = "robundle";

    public static final String NODE_ATTR_AGGREGATE_TYPE = "aggregate-type";
    public static final String AGGREGATE_TYPE_URI = "uri";
    public static final String AGGREGATE_TYPE_FILE = "file";
    public static final String AGGREGATE_TYPE_FOLDER = "folder";
    public static final String AGGREGATE_TYPE_ROOT = "root";

    public static final String TYPE_AGGREGATE = "aggregate";
    public static final String TYPE_FOLDER = "folder";

    public static final String NODE_ATTR_TYPE = "type";
    public static final String NODE_ATTR_NAME = "shared name";
    public static final String NODE_ATTR_PATH = "path";
    public static final String NODE_ATTR_FORMAT = "format";
    public static final String NODE_ATTR_MEDIATYPE = "mediatype";
    public static final String NODE_IMAGE = "image";

    public static final String NODE_ATTR_AUTHORED_BY = "authoredBy";
    public static final String NODE_ATTR_AUTHORED_ON = "authoredOn";
    public static final String NODE_ATTR_CREATED_BY = "createdBy";
    public static final String NODE_ATTR_CREATED_ON = "createdOn";

    private String fileName;
    private final CyNetworkFactory networkFactory;

    private final CyNetworkViewFactory viewFactory;
    private final VisualMappingManager visualMappingManager;
    private final CyLayoutAlgorithmManager layoutAlgorithmManager;

    private CyRootNetwork rootNetwork;
    private CyNetwork network; // global network of all SBML information

    private HashMap<String, CyNode> path2node;
    private HashMap<CyNode, String> node2path;

    private TaskMonitor taskMonitor;

    /**
     * Constructor.
     */
    public ArchiveReaderTask(
            InputStream stream,
            String fileName,
            CyNetworkFactory networkFactory,
            CyNetworkViewFactory viewFactory,
            VisualMappingManager visualMappingManager,
            CyLayoutAlgorithmManager layoutAlgorithmManager) {

        this.fileName = fileName;
        this.networkFactory = networkFactory;
        this.viewFactory = viewFactory;
        this.visualMappingManager = visualMappingManager;
        this.layoutAlgorithmManager = layoutAlgorithmManager;
    }

    /**
     * Get networks from reader.
     */
    @Override
    public CyNetwork[] getNetworks() {
        CyNetwork[] networks = {network};
        return networks;
    }

    /**
     * Build NetworkView for given network.
     */
    @Override
    public CyNetworkView buildCyNetworkView(final CyNetwork network) {
        logger.debug("buildCyNetworkView");

        // create view
        CyNetworkView view = viewFactory.createNetworkView(network);

        // set style
        if (visualMappingManager != null) {
            // VisualMappingManager only available in OSGI context
            VisualStyle style = StyleManager.getVisualStyleByName(visualMappingManager, ARCHIVE_STYLE);
            if (style != null) {
                visualMappingManager.setVisualStyle(style, view);
            }
        }

        // apply layout
        if (layoutAlgorithmManager != null) {
            CyLayoutAlgorithm layout = layoutAlgorithmManager.getLayout(ARCHIVE_LAYOUT);
            if (layout == null) {
                layout = layoutAlgorithmManager.getLayout(CyLayoutAlgorithmManager.DEFAULT_LAYOUT_NAME);
                logger.warn("'{}' layout not found; default layout used.", ARCHIVE_LAYOUT);
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

        // read SBMLFiles
        readFilesFromBundle();

        return view;
    }

    /**
     * Reads secondary file form given bundle.
     */
    private void readFilesFromBundle() {
        // Get all SBML files from bundle

        // not implemented yet (#116)
        List<Path> paths = new ArrayList<>();

        // read the files
        logger.info("Reading files from bundle");
        ServiceAdapter adapter = WebViewPanel.getInstance().getAdapter();
        for (Path path : paths) {

            logger.info("Reading: <" + path + ">");
            try {
                File tempFile = File.createTempFile("tmp-file", ".xml");
                tempFile.deleteOnExit();

                Files.copy(path, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                try {
                    TaskIterator iterator = adapter.loadNetworkFileTaskFactory.createTaskIterator(tempFile);
                    adapter.synchronousTaskManager.execute(iterator);
                } catch (java.lang.IllegalStateException e) {
                    logger.warn("No NetworkReader for the given file format");
                }
            } catch (IOException e) {
                logger.error("Could not extract the archive entry: " + path, e);
            }
        }
    }

    /**
     * Cancel task.
     */
    @Override
    public void cancel() {}

    /**
     * Creates the archive network.
     * <p>
     * The heavy lifting is performed by the robundle implementation.
     * <p>
     * RO
     * read the RO manifest file
     * one central file describing the content
     * .ro/metadata.json
     * <p>
     * many files describing the individual metadata
     * metadata.rdf
     * metadata.json
     * <p>
     * OMEX
     * read OMEX manifest file
     * only one central file describing
     * manifest.xml (content)
     * metadata.rdf (metadata about content)
     */
    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        logger.debug("<--- Start Archive Reader --->");
        this.taskMonitor = taskMonitor;
        try {
            if (taskMonitor != null) {
                taskMonitor.setTitle("archive reader");
                taskMonitor.setProgress(0.0);
            }
            if (cancelled) {
                return;
            }

            // mapping of archive content to CyNodes
            path2node = new HashMap<>();
            node2path = new HashMap<>();

            // Create empty root network and node map
            network = networkFactory.createNetwork();
            AttributeUtil.set(network, network, NODE_ATTR_PATH, fileName, String.class);

            // To create a new CySubNetwork with the same CyNetwork's CyRootNetwork, cast your CyNetwork to
            // CySubNetwork and call the CySubNetwork.getRootNetwork() method:
            // CyRootNetwork also provides methods to create and add new subnetworks (see
            // CyRootNetwork.addSubNetwork()).
            rootNetwork = ((CySubNetwork) network).getRootNetwork();

            //////////////////////////////////////////////////////////////////
            // Read information from manifest file
            //////////////////////////////////////////////////////////////////

            // reading the archive content is not implemented yet (#116)

            // set image attributes
            for (CyNode n : node2path.keySet()) {
                setImageAttribute(n);
            }

            //////////////////////////////////////////////////////////////////
            // Base network
            //////////////////////////////////////////////////////////////////

            // Set name
            String[] tokens = fileName.split("/", -1);
            String name = tokens[tokens.length - 1];
            rootNetwork.getRow(rootNetwork).set(CyNetwork.NAME, String.format("%s", name));
            network.getRow(network).set(CyNetwork.NAME, String.format("%s Content", name));

            if (taskMonitor != null) {
                taskMonitor.setProgress(0.8);
            }
            logger.debug("<--- End Archive Reader --->");

        } catch (Throwable t) {
            logger.error("Could not read Archive!", t);
        }
    }

    private String getNameFromPath(String path) {
        // folders and root
        if (path.endsWith("/")) {
            return path;
        }
        // files (file name)
        String[] tokens = path.split("/", -1);
        return tokens[tokens.length - 1];
    }

    /**
     * Creates gr node for the given aggregate.
     */
    private void createParentForNode(CyNode n) {
        logger.debug("createParentForNode: " + n);

        // get single node
        String path = node2path.get(n);
        String[] tokens = path.split("/");
        Integer Nparts = tokens.length;
        logger.debug("path:" + path);
        if (tokens.length > 1) {
            String[] newTokens = Arrays.copyOfRange(tokens, 0, Nparts - 1);
            String parentPath;
            if (newTokens.length == 1) {
                parentPath = newTokens[0] + "/";
            } else {
                parentPath = StringUtils.join(newTokens, "/") + "/";
            }
            logger.debug("parentPath:" + parentPath);

            // create parent node and edge
            CyNode nParent;
            if (!path2node.containsKey(parentPath)) {
                // parent node does not exist (create node and edge)
                nParent = network.addNode();
                AttributeUtil.set(network, nParent, NODE_ATTR_NAME, getNameFromPath(parentPath), String.class);
                AttributeUtil.set(network, nParent, NODE_ATTR_PATH, parentPath, String.class);
                AttributeUtil.set(network, nParent, NODE_ATTR_TYPE, TYPE_FOLDER, String.class);
                AttributeUtil.set(network, nParent, NODE_ATTR_AGGREGATE_TYPE, AGGREGATE_TYPE_FOLDER, String.class);

                node2path.put(nParent, parentPath);
                path2node.put(parentPath, nParent);
                network.addEdge(nParent, n, true);
            } else {
                // parent node exists
                nParent = path2node.get(parentPath);
                // check for edge
                List<CyNode> neighbors = network.getNeighborList(n, CyEdge.Type.DIRECTED);
                if (!neighbors.contains(nParent)) {
                    network.addEdge(nParent, n, true);
                }
            }
            // recursively go up in the hierarchy
            createParentForNode(nParent);
        }
    }

    /**
     * Image extension of the folder with the given path.
     * <p>
     * The folders of individual studies, models and assays, i.e. the folders directly inside
     * a "studies", "models" or "assays" folder, get the study, model and assay image,
     * all other folders the folder image.
     */
    static String folderExtension(String path) {
        // "/studies/s1/" and "studies/s1/" both give [studies, s1]
        String[] tokens = StringUtils.strip(path, "/").split("/", -1);
        if (tokens.length < 2) {
            return "folder";
        }
        return switch (tokens[tokens.length - 2]) {
            case "studies" -> "study";
            case "models" -> "model";
            case "assays" -> "assay";
            default -> "folder";
        };
    }

    /**
     * Creates the image link for a given node.
     */
    private void setImageAttribute(CyNode n) {

        // read attribute
        String mediaType = AttributeUtil.get(network, n, NODE_ATTR_MEDIATYPE, String.class);
        String format = AttributeUtil.get(network, n, NODE_ATTR_FORMAT, String.class);
        String path = AttributeUtil.get(network, n, NODE_ATTR_PATH, String.class);

        // image for node from mediaType
        String extension;

        if (path.equals("/")) {
            extension = "researchobject";
        } else if (path.endsWith("/")) {
            extension = folderExtension(path);
        } else {
            if (mediaType == null) {
                extension = "blank";
            } else {
                logger.debug("mediaType: " + mediaType);
                if (mediaType.equals("application/octet-stream")) {
                    extension = "bin";
                } else {
                    extension = getExtensionFromMediaType(mediaType);
                }
            }
        }

        // in case of COMBINE archives we have additional information from format which we can use
        if (format != null) {
            if (format.contains("sbml")) {
                extension = "sbml";
            } else if (format.contains("sed-ml")) {
                extension = "sedml";
            } else if (format.contains("sbgn")) {
                extension = "sbgn";
            } else if (format.contains("cellml")) {
                extension = "cellml";
            } else if (format.endsWith("text/plain")) {
                extension = "txt";
            } else {
                extension = getExtensionFromMediaType(format);
            }
        }

        String imageLink = String.format(
                "https://raw.githubusercontent.com/matthiaskoenig/cy3robundle/master/src/main/resources/gui/images/mediatype/%s.png",
                extension);
        AttributeUtil.set(network, n, NODE_IMAGE, imageLink, String.class);
    }

    /**
     * Get the extension from the given mediaType or format String.
     * Examples:
     * http://purl.org/NET/mediatypes/image/svg+xml
     * application/rdf+xml
     */
    private String getExtensionFromMediaType(String mediaType) {
        String tokens[] = mediaType.split("/");
        String extension = tokens[tokens.length - 1];
        // handle +xml
        if (extension.contains("+")) {
            tokens = extension.split("\\+");
            extension = tokens[0];
        }
        return extension;
    }
}

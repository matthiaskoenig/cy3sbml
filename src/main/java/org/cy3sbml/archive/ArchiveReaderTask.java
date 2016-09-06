package org.cy3sbml.archive;

import org.apache.commons.lang.StringUtils;
import org.apache.taverna.robundle.Bundle;
import org.apache.taverna.robundle.Bundles;
import org.apache.taverna.robundle.manifest.Agent;
import org.apache.taverna.robundle.manifest.Manifest;
import org.apache.taverna.robundle.manifest.PathAnnotation;
import org.apache.taverna.robundle.manifest.PathMetadata;
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

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipError;

/**
 * Create CyNetworks from Archives.
 *
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
	private final InputStream stream;
	private final CyNetworkFactory networkFactory;

	private final CyNetworkViewFactory viewFactory;
    private final VisualMappingManager visualMappingManager;
    private final CyLayoutAlgorithmManager layoutAlgorithmManager;


	private CyRootNetwork rootNetwork;
	private CyNetwork network;       // global network of all SBML information

    private Bundle bundle;
    private HashMap<String, CyNode> path2node;
    private HashMap<CyNode, String> node2path;

    private TaskMonitor taskMonitor;

	/**
     * Constructor.
     */
	public ArchiveReaderTask(InputStream stream, String fileName,
                             CyNetworkFactory networkFactory,
                             CyNetworkViewFactory viewFactory,
                             VisualMappingManager visualMappingManager,
                             CyLayoutAlgorithmManager layoutAlgorithmManager) {

		this.stream = stream;
		this.fileName = fileName;
		this.networkFactory = networkFactory;
		this.viewFactory = viewFactory;
        this.visualMappingManager = visualMappingManager;
        this.layoutAlgorithmManager = layoutAlgorithmManager;
	}

    /**
     * Get networks from reader.
     *
     * @return
     */
    @Override
    public CyNetwork[] getNetworks() {
        CyNetwork[] networks = { network };
        return networks;
    }

    /**
     * Build NetworkView for given network.
     *
     * @param network
     * @return
     */
    @Override
    public CyNetworkView buildCyNetworkView(final CyNetwork network) {
        logger.debug("buildCyNetworkView");

        // set bundle in BundleManager
        BundleManager bundleManager = BundleManager.getInstance();

        // BundleManager is only available in the OSGI context
        if (bundleManager != null) {

            bundleManager.addBundleForNetwork(bundle, network);
            // update current network
            bundleManager.updateCurrent(network);
            System.out.println(bundleManager.toString());
        } else {
            logger.warn("No mapping found for SBML network.");
        }



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
				logger.warn(String.format("'{}' layout not found; default layout used.", ARCHIVE_LAYOUT));
			}
			TaskIterator itr = layout.createTaskIterator(view, layout.getDefaultLayoutContext(), CyLayoutAlgorithm.ALL_NODE_VIEWS, "");
			Task nextTask = itr.next();
			try {
				nextTask.run(taskMonitor);
			} catch (Exception e) {
				throw new RuntimeException("Could not finish layout", e);
			}
		}

        return view;
    }

    /**
     * Cancel task.
     */
    @Override
    public void cancel() {}

    /**
     * Creates the archive network.
     *
     * The heavy lifting is performed by the robundle implementation.
     *
     * RO
     *  read the RO manifest file
     *  one central file describing the content
     *  .ro/metadata.json
     *
     *  many files describing the individual metadata
     *      metadata.rdf
     *      metadata.json
     *
     * OMEX
     *  read OMEX manifest file
     *  only one central file describing
     *      manifest.xml (content)
     *      metadata.rdf (metadata about content)
     *
     * @param taskMonitor
     * @throws Exception
     */
	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {
		logger.debug("<--- Start Archive Reader --->");
        this.taskMonitor = taskMonitor;
		try {
			if (taskMonitor != null){
				taskMonitor.setTitle("archive reader");
				taskMonitor.setProgress(0.0);
			}
			if (cancelled){
				return;
			}

			// mapping of archive content to CyNodes
            path2node = new HashMap<>();
            node2path = new HashMap<>();

            /*
            if (stream instanceof ZipInputStream){
                logger.info("ZipInputStream found in reader.");

                //    This should no happen currently, because the ZipInputStream
                //     is packed into a BufferecInputStream and unreadable as a
                //     consequence.
                //     We have to rename *.zip files to deal with this.
                ZipInputStream zis = (ZipInputStream) stream;

                // read entries from zip file
                ZipEntry ze = null;
                while ((ze = zis.getNextEntry()) != null) {
                    System.out.println("Unzipping " + ze.getName());

                    // write files
                    FileOutputStream fout = new FileOutputStream(ze.getName());
                    for (int c = zin.read(); c != -1; c = zin.read()) {
                        fout.write(c);
                    }

                    zis.closeEntry();
                    // fout.close();
                }
                zis.close();
            } else {
                logger.error("Stream is not ZipInputStream");
                System.out.println(stream);
            }
            */


			// Create empty root network and node map
			network = networkFactory.createNetwork();
            AttributeUtil.set(network, network, NODE_ATTR_PATH, fileName, String.class);

			// To create a new CySubNetwork with the same CyNetwork's CyRootNetwork, cast your CyNetwork to
			// CySubNetwork and call the CySubNetwork.getRootNetwork() method:
			// 		CyRootNetwork rootNetwork = ((CySubNetwork)network).getRootNetwork();
			// CyRootNetwork also provides methods to create and add new subnetworks (see CyRootNetwork.addSubNetwork()).
			rootNetwork = ((CySubNetwork) network).getRootNetwork();


            //////////////////////////////////////////////////////////////////
            // Read information from manifest file
            //////////////////////////////////////////////////////////////////

            // Read archive
            try {
                bundle = Bundles.openBundle(stream);
                System.out.println(bundle);

                Manifest manifest = bundle.getManifest();
                System.out.println("------------------------");
                System.out.println(manifest);
                System.out.println("CreatedBy: " + manifest.getCreatedBy());
                System.out.println("CreatedOn: " + manifest.getCreatedOn());

                System.out.println("\n<manifest>");
                List<Path> pathList = manifest.getManifest();
                for (Path p: pathList){
                    System.out.println(p);
                }

                // locations & aggregates (either files or uris)
                System.out.println("\n<aggregates>");
                List<PathMetadata> aggregates = manifest.getAggregates();
                for (PathMetadata metaData: aggregates){
                    System.out.println(metaData);
                    // create aggregate node
                    CyNode n = createNodeForPath(metaData);
                }

                // create archive tree
                for (PathMetadata metaData: aggregates){
                    createTreeForPath(metaData);
                }

                System.out.println("\n<annotations>");
                for (PathAnnotation a: manifest.getAnnotations()){
                    System.out.println(a);
                    // createEdgesForAnnotation(a);
                }
                if (taskMonitor != null){
                    taskMonitor.setProgress(0.4);
                }
                System.out.println("------------------------");
            }catch(ZipError e){
                logger.error("Could not read the zip file.");
                logger.error("Rename archives ending in *.zip with *.zip1");
            }

            // set image attributes
            for (CyNode n: node2path.keySet()){
                setImageAttribute(n);
            }


			//////////////////////////////////////////////////////////////////
            // Base network
            //////////////////////////////////////////////////////////////////

            // Set name
            String[] tokens = fileName.split("/");
            String name = tokens[tokens.length-1];
            rootNetwork.getRow(rootNetwork).set(CyNetwork.NAME, String.format("%s", name));
            network.getRow(network).set(CyNetwork.NAME, String.format("%s Content", name));

			if (taskMonitor != null){
				taskMonitor.setProgress(0.8);
			}
			logger.debug("<--- End Archive Reader --->");
			
		
		} catch (Throwable t){
			logger.error("Could not read Archive!", t);
			t.printStackTrace();
		}
	}



    /**
     * Creates the node for the given aggregate.
     * @param md
     * @return
     */
	private CyNode createNodeForPath(PathMetadata md){
	    // Create single node
	    CyNode n = network.addNode();
        String path = md.toString();
        AttributeUtil.set(network, n, NODE_ATTR_PATH, path, String.class);
        path2node.put(path, n);
        node2path.put(n, path);

        // Set attributes
        String name = getNameFromPath(path);
        AttributeUtil.set(network, n, NODE_ATTR_NAME, name, String.class);
        if (md.getUri() != null) {
            AttributeUtil.set(network, n, NODE_ATTR_AGGREGATE_TYPE, AGGREGATE_TYPE_URI, String.class);
            AttributeUtil.set(network, n, NODE_ATTR_TYPE, TYPE_AGGREGATE, String.class);
        }
        if (md.getFile() != null) {
            AttributeUtil.set(network, n, NODE_ATTR_TYPE, TYPE_AGGREGATE, String.class);
            AttributeUtil.set(network, n, NODE_ATTR_AGGREGATE_TYPE, AGGREGATE_TYPE_FILE, String.class);
        }

        // handle the root node
        if (name.equals("/")) {
            AttributeUtil.set(network, n, NODE_ATTR_AGGREGATE_TYPE, AGGREGATE_TYPE_ROOT, String.class);
        }

        if (md.getConformsTo() != null){
            AttributeUtil.set(network, n, NODE_ATTR_FORMAT, md.getConformsTo().toString(), String.class);
        }

        if (md.getMediatype() != null) {
            AttributeUtil.set(network, n, NODE_ATTR_MEDIATYPE, md.getMediatype(), String.class);
        }

        if (md.getAuthoredBy() != null){
            String text = getAgentsString(md.getAuthoredBy());
            AttributeUtil.set(network, n, NODE_ATTR_AUTHORED_BY, text, String.class);
        }
        if (md.getAuthoredOn() != null){
            FileTime time = md.getAuthoredOn();
            AttributeUtil.set(network, n, NODE_ATTR_AUTHORED_ON, time.toString(), String.class);
        }
        if (md.getCreatedBy() != null){
            String text = getAgentString(md.getCreatedBy());
            AttributeUtil.set(network, n, NODE_ATTR_CREATED_BY, text, String.class);
        }
        if (md.getCreatedOn() != null){
            FileTime time = md.getCreatedOn();
            AttributeUtil.set(network, n, NODE_ATTR_CREATED_ON, time.toString(), String.class);
        }
        return n;
    }

    /**
     * Add annotation edges to the network.
     */
    @Deprecated
    private void createEdgesForAnnotation(PathAnnotation a){
        logger.info("createEdgesForAnnotation :" + a);
        List<URI> aboutURIs = a.getAboutList();
        URI contentURI = a.getContent();

        String content = contentURI.toString();
        CyNode nContent = path2node.get(content);
        // wrong prefix in content
        String fixedContent = content;
        if (nContent == null && content.startsWith("/.ro/")){
            fixedContent = content.replace("/.ro", "");
            nContent = path2node.get(fixedContent);
        }

        if (nContent != null){
            for (URI uri: aboutURIs){
                CyNode nAbout = path2node.get(uri.toString());
                if (nAbout != null){
                    // add edge
                    logger.info("Edge added for annotation.");
                    network.addEdge(nContent, nAbout, true);
                } else {
                    logger.error("About node not found for: " + uri);
                }
            }
        } else {
            logger.error("Content node not found for: " + contentURI);
        }
    }


    private String getNameFromPath(String path){
        // folders and root
        if (path.endsWith("/")){
            return path;
        }
        // files (file name)
        String[] tokens = path.split("/");
        return tokens[tokens.length-1];
    }


    /**
     * Creates the Tree leading to root for given path.
     * @param md
     * @return
     */
    private void createTreeForPath(PathMetadata md) {
        // Create single node
        String path = md.toString();
        CyNode n = path2node.get(path);
        createParentForNode(n);
    }


    /**
     * Creates gr node for the given aggregate.
     *
     * @param n
     * @return
     */
    private void createParentForNode(CyNode n) {
        logger.debug("createParentForNode: " + n);

        // get single node
        String path = node2path.get(n);
        String[] tokens = path.split("/");
        Integer Nparts = tokens.length;
        logger.debug("path:" + path);
        if (tokens.length>1){
            String [] newTokens = Arrays.copyOfRange(tokens, 0, Nparts-1);
            String parentPath;
            if (newTokens.length == 1){
                parentPath = newTokens[0] + "/";
            } else {
                parentPath = StringUtils.join(newTokens, "/") + "/";
            }
            logger.debug("parentPath:" + parentPath);

            // create parent node and edge
            CyNode nParent;
            if (!path2node.containsKey(parentPath)){
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
                if (! neighbors.contains(nParent)){
                    network.addEdge(nParent, n, true);
                }
            }
            // recursively go up in the hierarchy
            createParentForNode(nParent);
        }
    }

    /**
     * Creates the image link for a given node.
     *
     * @param n
     */
    private void setImageAttribute(CyNode n){
        final String TEMPLATE = "https://raw.githubusercontent.com/matthiaskoenig/cy3robundle/master/src/main/resources/gui/images/mediatype/%s.png";

        // read attribute
        String mediaType = AttributeUtil.get(network, n, NODE_ATTR_MEDIATYPE, String.class);
        String format = AttributeUtil.get(network, n, NODE_ATTR_FORMAT, String.class);
        String path = AttributeUtil.get(network, n, NODE_ATTR_PATH, String.class);

        // image for node from mediaType
        String extension;

        if (path.equals("/")){
            extension = "researchobject";
        } else if (path.endsWith("/")){
            extension = "folder";
            // handle subset of folder aggregates
            String[] tokens = path.split("/");
            if (tokens.length > 2) {
                String type = tokens[tokens.length - 2];
                if (type.equals("studies")){
                    extension = "study";
                } else if (type.equals("models")){
                    extension = "model";
                } else if (type.equals("assays")){
                    extension = "assay";
                }
            }
        } else {
            if (mediaType == null) {
                extension = "blank";
            } else {
                logger.debug("mediaType: " + mediaType);
                if (mediaType.equals("application/octet-stream")){
                    extension = "bin";
                } else {
                    extension = getExtensionFromMediaType(mediaType);
                }
            }
        }

        // in case of COMBINE archives we have additional information from format which we can use
        if (format != null){
            if (format.contains("sbml")){
                extension = "sbml";
            } else if (format.contains("sed-ml")){
                extension = "sedml";
            } else if (format.contains("sbgn")){
                extension = "sbgn";
            } else if (format.contains("cellml")) {
                extension = "cellml";
            } else if (format.endsWith("text/plain")){
                extension = "txt";
            } else {
                extension = getExtensionFromMediaType(format);
            }
        }

        String imageLink = String.format(TEMPLATE, extension);
        AttributeUtil.set(network, n, NODE_IMAGE, imageLink, String.class);
    }

    /**
     * Get the extension from the given mediaType or format String.
     * Examples:
     *  http://purl.org/NET/mediatypes/image/svg+xml
     *  application/rdf+xml
     *
     * @param mediaType
     */
    private String getExtensionFromMediaType(String mediaType){
        String tokens[] = mediaType.split("/");
        String extension = tokens[tokens.length-1];
        // handle +xml
        if (extension.contains("+")){
            tokens = extension.split("\\+");
            extension = tokens[0];
        }
        return extension;
    }

    /**
     * Agents string representation.
     *
     * @param agents
     * @return
     */
    private String getAgentsString(List<Agent> agents){
        String text = "";
        for (Agent a: agents){
            text += getAgentString(a) + "; ";
        }
        return text;
    }

    /**
     * Create String for agent.
     *
     * @param agent
     * @return
     */
    private String getAgentString(Agent agent){
        String text = String.format(
                "%s (uri=%s, orcid=%s)",
                agent.getName(), agent.getUri(), agent.getOrcid());
        return text;
    }


}

package org.cy3sbml.reader;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.cy3sbml.SBML;
import org.cytoscape.group.CyGroup;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.subnetwork.CyRootNetwork;

/**
 * Names the network read from a model and creates its kinetic and base subnetworks.
 */
final class SubnetworkBuilder {
    private final String fileName;

    /**
     * @param fileName name of the read file, names the networks of models without id
     */
    SubnetworkBuilder(String fileName) {
        this.fileName = fileName;
    }

    /**
     * Names the root network and the network with all nodes, and adds the kinetic and
     * the base subnetwork with the groups to the root network.
     *
     * @param rootNetwork root network of the network
     * @param network     network with all nodes and edges of the model
     * @param groups      groups to add to the subnetworks
     * @param prefix      prefix of the names, e.g. {@link SBML#PREFIX_NETWORK_FLAT}, or null
     * @return the networks in the order all, kinetic, base
     */
    List<CyNetwork> build(CyRootNetwork rootNetwork, CyNetwork network, Set<CyGroup> groups, String prefix) {
        String name = prefix == null ? getNetworkName(network) : prefix + "__" + getNetworkName(network);
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
        for (CyNetwork net : List.of(baseNetwork, kineticNetwork)) {
            for (CyGroup cyGroup : groups) {
                cyGroup.addGroupToNetwork(net);
            }
        }
        return List.of(network, kineticNetwork, baseNetwork);
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
     * Get network nodes with given node types.
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
            name = fileName(fileName);
        }
        return name;
    }

    /**
     * Returns the file name of the given input name: Cytoscape passes the file name for a
     * file and the URL for a URL; a path or URL is reduced to its last segment.
     */
    static String fileName(String inputName) {
        if (inputName == null) {
            return "";
        }
        String path = inputName;
        try {
            URI uri = new URI(inputName);
            // a scheme of one letter is a Windows drive
            if (uri.getScheme() != null && uri.getScheme().length() > 1 && uri.getPath() != null) {
                path = uri.getPath();
            }
        } catch (URISyntaxException e) {
            // not a URI, e.g. a Windows path
        }
        String name = path.substring(Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\')) + 1);
        return name.isEmpty() ? inputName : name;
    }
}

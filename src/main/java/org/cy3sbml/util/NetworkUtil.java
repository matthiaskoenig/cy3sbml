package org.cy3sbml.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.cy3sbml.SBML;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helpers for the networks of a collection: the root network, the subnetworks, and the
 * selection of nodes.
 */
public class NetworkUtil {
    private static final Logger logger = LoggerFactory.getLogger(NetworkUtil.class);

    /**
     * The SUID of the root network of the network.
     *
     * @param network a subnetwork, may be null
     * @return the root network SUID, null if the network is null
     */
    public static Long getRootNetworkSUID(CyNetwork network) {
        return network == null
                ? null
                : ((CySubNetwork) network).getRootNetwork().getSUID();
    }

    /**
     * The root network of the network.
     *
     * @param network a subnetwork, may be null
     * @return the root network, null if the network is null
     */
    public static CyNetwork getRootNetwork(CyNetwork network) {
        return network == null ? null : ((CySubNetwork) network).getRootNetwork();
    }

    /**
     * Whether the network was created by cy3sbml: its network table has the SBML network
     * type column. It does not require that the network is in the mapping.
     *
     * @param cyNetwork the network
     * @return true for an SBML network
     */
    public static boolean isSBMLNetwork(CyNetwork cyNetwork) {
        // true if the attribute column exists
        CyTable cyTable = cyNetwork.getDefaultNetworkTable();
        return cyTable.getColumn(SBML.NETWORKTYPE_ATTR) != null;
    }

    /**
     * Returns the subnetwork with the given suffix, e.g. {@link SBML#SUFFIX_SUBNETWORK_KINETIC}:
     * the first network named like its root network plus the suffix.
     * Returns null if no such network exists.
     */
    public static CyNetwork getSubnetwork(CyNetwork[] networks, String suffix) {
        for (CyNetwork network : networks) {
            if (isSubnetwork(network, suffix)) {
                return network;
            }
        }
        return null;
    }

    /** True if the network is named like its root network plus the suffix. */
    private static boolean isSubnetwork(CyNetwork network, String suffix) {
        if (!(network instanceof CySubNetwork subNetwork)) {
            return false;
        }
        CyRootNetwork root = subNetwork.getRootNetwork();
        String rootName = root.getRow(root).get(CyNetwork.NAME, String.class);
        String name = network.getRow(network).get(CyNetwork.NAME, String.class);
        return rootName != null && (rootName + suffix).equals(name);
    }

    /**
     * The network of the collection with the root network SUID that has a node for the
     * element with the cyId: the base network if it has the node, else the kinetic or the
     * all network.
     */
    public static Optional<CyNetwork> findTargetNetwork(Collection<CyNetwork> networks, Long rootSUID, String cyId) {
        return networksOfRoot(networks, rootSUID).stream()
                .filter(n -> AttributeUtil.getNodeByAttribute(n, SBML.ATTR_CYID, cyId) != null)
                .findFirst();
    }

    /** The base network of the collection with the root network SUID. */
    public static Optional<CyNetwork> findBaseNetwork(Collection<CyNetwork> networks, Long rootSUID) {
        return networksOfRoot(networks, rootSUID).stream().findFirst();
    }

    /** The networks of the collection, the base network first, then the kinetic and the all network. */
    private static List<CyNetwork> networksOfRoot(Collection<CyNetwork> networks, Long rootSUID) {
        List<CyNetwork> candidates = new ArrayList<>();
        for (CyNetwork network : networks) {
            if (network instanceof CySubNetwork subNetwork
                    && rootSUID.equals(subNetwork.getRootNetwork().getSUID())) {
                candidates.add(network);
            }
        }
        candidates.sort(Comparator.comparingInt(NetworkUtil::subnetworkRank));
        return candidates;
    }

    private static int subnetworkRank(CyNetwork network) {
        if (isSubnetwork(network, SBML.SUFFIX_SUBNETWORK_BASE)) {
            return 0;
        }
        return isSubnetwork(network, SBML.SUFFIX_SUBNETWORK_KINETIC) ? 1 : 2;
    }

    // ------------------------------------------------------------
    // Selection
    // ------------------------------------------------------------

    /**
     * Selects the node with the cyId (metaId), unselecting all other nodes.
     *
     * @param network the network
     * @param metaId the cyId of the node
     */
    public static void selectByMetaId(CyNetwork network, String metaId) {
        logger.debug("Select node for metaId: {}", metaId);
        CyNode node = AttributeUtil.getNodeByAttribute(network, SBML.ATTR_CYID, metaId);
        selectNodeInNetwork(network, node);
    }

    /**
     * Selects the node with the SBML id, unselecting all other nodes.
     *
     * @param network the network
     * @param id the SBML id of the node
     */
    public static void selectById(CyNetwork network, String id) {
        logger.debug("Select node for id: {}", id);
        CyNode node = AttributeUtil.getNodeByAttribute(network, SBML.ATTR_ID, id);
        selectNodeInNetwork(network, node);
    }

    /**
     * Selects the node in the network, unselecting all other nodes. Does nothing for null.
     *
     * @param network the network
     * @param node the node, may be null
     */
    public static void selectNodeInNetwork(CyNetwork network, CyNode node) {
        if (node != null) {
            // unselect all
            List<CyNode> nodes = CyTableUtil.getNodesInState(network, CyNetwork.SELECTED, true);
            for (CyNode n : nodes) {
                AttributeUtil.set(network, n, CyNetwork.SELECTED, false, Boolean.class);
            }
            AttributeUtil.set(network, node, CyNetwork.SELECTED, true, Boolean.class);
        }
    }
}

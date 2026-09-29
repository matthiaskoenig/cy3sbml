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
 * Utils for working with networks.
 */
public class NetworkUtil {
    private static final Logger logger = LoggerFactory.getLogger(NetworkUtil.class);

    /**
     * Get SUID of root network.
     * Returns null if the network is null.
     */
    public static Long getRootNetworkSUID(CyNetwork network) {
        Long suid = null;
        if (network != null) {
            CyRootNetwork rootNetwork = ((CySubNetwork) network).getRootNetwork();
            suid = rootNetwork.getSUID();
        }
        return suid;
    }

    /**
     * Get rootNetwork for given network.
     */
    public static CyNetwork getRootNetwork(CyNetwork network) {
        CyNetwork rootNetwork = null;
        if (network != null) {
            rootNetwork = ((CySubNetwork) network).getRootNetwork();
        }
        return rootNetwork;
    }

    /**
     * Check if network is an SBMLNetwork.
     * This uses a attribute in the network table to check the type of the network.
     * It does not require that the network is in the mapping.
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
     * Select node by metaId.
     */
    public static void selectByMetaId(CyNetwork network, String metaId) {
        logger.info(String.format("Select node for metaId: %s", metaId));

        CyNode node = AttributeUtil.getNodeByAttribute(network, SBML.ATTR_CYID, metaId);
        selectNodeInNetwork(network, node);
    }

    /**
     * Select node by id.
     */
    public static void selectById(CyNetwork network, String id) {
        logger.info(String.format("Select node for id: %s", id));
        CyNode node = AttributeUtil.getNodeByAttribute(network, SBML.ATTR_ID, id);
        selectNodeInNetwork(network, node);
    }

    /**
     * Selects given node in network.
     * Unselects all other nodes.
     */
    public static void selectNodeInNetwork(CyNetwork network, CyNode node) {
        if (node != null) {
            // unselect all
            List<CyNode> nodes = CyTableUtil.getNodesInState(network, CyNetwork.SELECTED, true);
            for (CyNode n : nodes) {
                AttributeUtil.set(network, n, CyNetwork.SELECTED, false, Boolean.class);
            }
            // select node
            logger.info("selected node");
            AttributeUtil.set(network, node, CyNetwork.SELECTED, true, Boolean.class);
        }
    }
}

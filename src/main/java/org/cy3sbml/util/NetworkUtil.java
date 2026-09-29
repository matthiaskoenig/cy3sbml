package org.cy3sbml.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
     * Returns the network which starts with a given SubNetwork prefix.
     * Returns null if no such network exists.
     */
    public static CyNetwork getNetworkBySubNetworkPrefix(CyNetwork[] networks, String prefixSubnetwork) {
        CyNetwork network = null;
        for (CyNetwork n : networks) {
            String networkName = AttributeUtil.get(n, n, CyNetwork.NAME, String.class);
            if (networkName.startsWith(prefixSubnetwork)) {
                network = n;
                break;
            }
        }
        return network;
    }

    /**
     * The network of the model with the id that has a node for the element with the cyId:
     * the base network of the model if it has the node, else its kinetic or its all network.
     * Flat networks are left out, their models have the id of the main model.
     */
    public static Optional<CyNetwork> findTargetNetwork(Collection<CyNetwork> networks, String modelId, String cyId) {
        Map<CyRootNetwork, List<CyNetwork>> byRoot = new LinkedHashMap<>();
        for (CyNetwork network : networks) {
            if (network instanceof CySubNetwork subNetwork) {
                byRoot.computeIfAbsent(subNetwork.getRootNetwork(), root -> new ArrayList<>())
                        .add(network);
            }
        }
        for (Map.Entry<CyRootNetwork, List<CyNetwork>> entry : byRoot.entrySet()) {
            CyRootNetwork root = entry.getKey();
            String rootName = root.getRow(root).get(CyNetwork.NAME, String.class);
            if (rootName == null || rootName.startsWith(SBML.PREFIX_NETWORK_FLAT + "__")) {
                continue;
            }
            List<CyNetwork> candidates = new ArrayList<>(entry.getValue());
            boolean ofModel =
                    candidates.stream().anyMatch(n -> modelId.equals(n.getRow(n).get(SBML.ATTR_ID, String.class)));
            if (!ofModel) {
                continue;
            }
            // base network first, then kinetic, then all
            candidates.sort(Comparator.comparingInt(n -> subnetworkRank(n, rootName)));
            for (CyNetwork candidate : candidates) {
                if (AttributeUtil.getNodeByAttribute(candidate, SBML.ATTR_CYID, cyId) != null) {
                    return Optional.of(candidate);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * The base network of the model with the id, flat networks left out.
     */
    public static Optional<CyNetwork> findModelNetwork(Collection<CyNetwork> networks, String modelId) {
        for (CyNetwork network : networks) {
            if (!(network instanceof CySubNetwork subNetwork)
                    || !modelId.equals(network.getRow(network).get(SBML.ATTR_ID, String.class))) {
                continue;
            }
            CyRootNetwork root = subNetwork.getRootNetwork();
            String rootName = root.getRow(root).get(CyNetwork.NAME, String.class);
            if (rootName == null || rootName.startsWith(SBML.PREFIX_NETWORK_FLAT + "__")) {
                continue;
            }
            // the base network has the name of the collection
            return root.getSubNetworkList().stream()
                    .<CyNetwork>map(n -> n)
                    .filter(n -> rootName.equals(n.getRow(n).get(CyNetwork.NAME, String.class)))
                    .findFirst()
                    .or(() -> Optional.of(network));
        }
        return Optional.empty();
    }

    private static int subnetworkRank(CyNetwork network, String rootName) {
        String name = network.getRow(network).get(CyNetwork.NAME, String.class);
        if (rootName.equals(name)) {
            return 0;
        }
        return name != null && name.startsWith(SBML.PREFIX_SUBNETWORK_KINETIC + "__") ? 1 : 2;
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

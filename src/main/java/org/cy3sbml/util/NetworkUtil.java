package org.cy3sbml.util;

import java.util.List;

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
    public static Long getRootNetworkSUID(CyNetwork network){
        Long suid = null;
        if (network != null){
            CyRootNetwork rootNetwork = ((CySubNetwork)network).getRootNetwork();
            suid = rootNetwork.getSUID();
        }
        return suid;
    }

    /**
     * Get rootNetwork for given network.
     */
    public static CyNetwork getRootNetwork(CyNetwork network){
        CyNetwork rootNetwork = null;
        if (network != null){
            rootNetwork = ((CySubNetwork)network).getRootNetwork();
        }
        return rootNetwork;
    }

    /**
     * Check if network is an SBMLNetwork.
     * This uses a attribute in the network table to check the type of the network.
     * It does not require that the network is in the mapping.
     */
    public static boolean isSBMLNetwork(CyNetwork cyNetwork) {
        //true if the attribute column exists
        CyTable cyTable = cyNetwork.getDefaultNetworkTable();
        return cyTable.getColumn(SBML.NETWORKTYPE_ATTR) != null;
    }

    /**
     * Returns the network which starts with a given SubNetwork prefix.
     * Returns null if no such network exists.
     *
     * @param networks
     * @param prefixSubnetwork
     * @return
     */
    public static CyNetwork getNetworkBySubNetworkPrefix(CyNetwork[] networks, String prefixSubnetwork){
        CyNetwork network = null;
        for (CyNetwork n: networks){
            String networkName = AttributeUtil.get(n, n, CyNetwork.NAME, String.class);
            if (networkName.startsWith(prefixSubnetwork)){
                network = n;
                break;
            }
        }
        return network;
    }



    ////////////////////////////////////////////////////////
    // Selection
    ////////////////////////////////////////////////////////

    /**
     * Select node by metaId.
     *
     * @param network
     * @param metaId
     */
    public static void selectByMetaId(CyNetwork network, String metaId){
        logger.info(String.format("Select node for metaId: %s", metaId));

        CyNode node = AttributeUtil.getNodeByAttribute(network, SBML.ATTR_CYID, metaId);
        selectNodeInNetwork(network, node);
    }

    /**
     * Select node by id.
     *
     * @param network
     * @param id
     */
    public static void selectById(CyNetwork network, String id){
        logger.info(String.format("Select node for id: %s", id));
        CyNode node = AttributeUtil.getNodeByAttribute(network, SBML.ATTR_ID, id);
        selectNodeInNetwork(network, node);
    }



    /**
     * Selects given node in network.
     * Unselects all other nodes.
     * @param network
     * @param node
     */
    public static void selectNodeInNetwork(CyNetwork network, CyNode node){
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

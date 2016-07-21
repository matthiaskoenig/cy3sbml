package org.cy3sbml.util;

import org.cy3sbml.SBML;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;

/**
 * Utils for working with networks.
 */
public class NetworkUtil {

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

}

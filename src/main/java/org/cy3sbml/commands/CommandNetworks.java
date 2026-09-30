package org.cy3sbml.commands;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.cy3sbml.SBML;
import org.cy3sbml.SBMLManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;

/**
 * The networks and models of the command results, and the resolution of the network
 * argument of the commands.
 */
final class CommandNetworks {
    static final String TYPE_BASE = SBML.SUBNETWORK_BASE;
    static final String TYPE_KINETIC = SBML.SUBNETWORK_KINETIC;
    static final String TYPE_ALL = SBML.SUBNETWORK_ALL;
    static final String TYPE_LAYOUT = SBML.SUBNETWORK_LAYOUT;
    static final String TYPE_OTHER = "other";
    private static final List<String> TYPE_ORDER = List.of(TYPE_BASE, TYPE_KINETIC, TYPE_ALL, TYPE_LAYOUT, TYPE_OTHER);

    private CommandNetworks() {}

    /**
     * The network argument of a command, the current network if not given.
     *
     * @throws IllegalArgumentException if there is no network
     */
    static CyNetwork network(CyNetwork network, CommandServices services) {
        CyNetwork result =
                network != null ? network : services.applicationManager().getCurrentNetwork();
        if (result == null) {
            throw new IllegalArgumentException("No network: give the network argument or select a network.");
        }
        return result;
    }

    /**
     * The SBML document of the network.
     *
     * @throws IllegalArgumentException if the network was not imported by cy3sbml
     */
    static SBMLDocument document(CyNetwork network, SBMLManager sbmlManager) {
        SBMLDocument document = sbmlManager.getSBMLDocument(network);
        if (document == null) {
            throw new IllegalArgumentException("The network '" + name(network) + "' (SUID " + network.getSUID()
                    + ") is not an SBML network of cy3sbml.");
        }
        return document;
    }

    /**
     * The view of the network.
     *
     * @throws IllegalArgumentException if the network has no view
     */
    static CyNetworkView view(CyNetwork network, CyNetworkViewManager viewManager) {
        Collection<CyNetworkView> views = viewManager.getNetworkViews(network);
        if (views.isEmpty()) {
            throw new IllegalArgumentException(
                    "The network '" + name(network) + "' (SUID " + network.getSUID() + ") has no view.");
        }
        return views.iterator().next();
    }

    /** The name of the network. */
    static String name(CyNetwork network) {
        String name = network.getRow(network).get(CyNetwork.NAME, String.class);
        return name != null ? name : "";
    }

    /**
     * The type of an SBML network: {@code base}, {@code kinetic}, {@code all} or
     * {@code layout} from the column {@value SBML#SUBNETWORK_ATTR}; for the networks of
     * sessions of older versions without the column from the suffix of the name, and
     * {@code other} for any other network.
     */
    static String type(CyNetwork network, String rootName) {
        CyRow row = network.getRow(network);
        if (row.getTable().getColumn(SBML.SUBNETWORK_ATTR) != null) {
            String type = row.get(SBML.SUBNETWORK_ATTR, String.class);
            if (type != null) {
                return type;
            }
        }
        String name = name(network);
        if (name.contains(SBML.SUFFIX_SUBNETWORK_LAYOUT)) {
            return TYPE_LAYOUT;
        }
        if (name.endsWith(SBML.SUFFIX_SUBNETWORK_KINETIC)) {
            return TYPE_KINETIC;
        }
        if (name.endsWith(SBML.SUFFIX_SUBNETWORK_ALL)) {
            return TYPE_ALL;
        }
        if (name.equals(rootName)) {
            return TYPE_BASE;
        }
        return TYPE_OTHER;
    }

    /** A network of a result: SUID, name and type. */
    static Map<String, Object> networkJson(CyNetwork network, String rootName) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("suid", network.getSUID());
        json.put("name", name(network));
        json.put("type", type(network, rootName));
        return json;
    }

    /** The root network of a network. */
    static Optional<CyRootNetwork> root(CyNetwork network) {
        if (network instanceof CySubNetwork subNetwork) {
            return Optional.of(subNetwork.getRootNetwork());
        }
        return Optional.empty();
    }

    /**
     * A model of a result: the root network, the model id and name and the given networks
     * of the root network, ordered by type (base, kinetic, all, layout, other).
     */
    static Map<String, Object> modelJson(
            CyRootNetwork root, List<? extends CyNetwork> networks, SBMLManager sbmlManager) {
        String rootName = name(root);
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("rootNetwork", root.getSUID());
        json.put("name", rootName);
        // the model of the collection, e.g. a comp model definition, not the main model
        Model model = sbmlManager.getModel(root.getSUID());
        json.put("modelId", model != null && model.isSetId() ? model.getId() : "");
        json.put("modelName", model != null && model.isSetName() ? model.getName() : "");
        // base, kinetic, all, then the layout and other networks in the order of their SUIDs
        List<CyNetwork> sorted = new ArrayList<>(networks);
        sorted.sort(Comparator.<CyNetwork>comparingInt(network -> TYPE_ORDER.indexOf(type(network, rootName)))
                .thenComparing(CyNetwork::getSUID));
        List<Map<String, Object>> networksJson = new ArrayList<>();
        for (CyNetwork network : sorted) {
            networksJson.add(networkJson(network, rootName));
        }
        json.put("networks", networksJson);
        return json;
    }
}

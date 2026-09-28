package org.cy3sbml.reader;

import java.util.HashMap;
import org.cy3sbml.SBML;
import org.cy3sbml.util.AttributeUtil;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adds the attributes derived from the complete network: compartment codes,
 * extended node types and extended interaction types.
 * <p>
 * It is the last step of the reader list of {@link SBMLReaderTask}, so that it covers
 * the nodes and edges of all package readers.
 */
final class DerivedAttributes implements PackageReader {
    private static final Logger logger = LoggerFactory.getLogger(DerivedAttributes.class);

    @Override
    public void read(ConversionContext context, Model model) {
        CyNetwork network = context.network();
        addCompartmentCodes(network, model);
        addSBMLTypesExtended(network);
        addSBMLInteractionExtended(network);
    }

    /**
     * Adds integer compartment codes as node attribute.
     * <p>
     * The compartmentCodes can be used in the visual mapping for dynamical
     * visualization of compartment colors.
     */
    private static void addCompartmentCodes(CyNetwork network, Model model) {
        // Calculate compartment code mapping
        HashMap<String, Integer> compartmentCodes = new HashMap<>();
        Integer compartmentCode = 1;
        for (Compartment c : model.getListOfCompartments()) {
            String cid = c.getId();
            if (!compartmentCodes.containsKey(cid)) {
                compartmentCodes.put(cid, compartmentCode);
                compartmentCode += 1;
            }
        }
        // set compartment code attribute
        for (CyNode n : network.getNodeList()) {
            String cid = AttributeUtil.get(network, n, SBML.ATTR_COMPARTMENT, String.class);
            Integer code = compartmentCodes.get(cid);
            AttributeUtil.set(network, n, SBML.ATTR_COMPARTMENT_CODE, code, Integer.class);
        }
    }

    /**
     * Adds extended sbml types as node attributes.
     * <p>
     * The extended SBML types can be used in the visual mapping.
     * This allows for instance to distinguish reversible and irreversible reactions.
     */
    private static void addSBMLTypesExtended(CyNetwork network) {
        for (CyNode n : network.getNodeList()) {
            String type = AttributeUtil.get(network, n, SBML.NODETYPE_ATTR, String.class);
            if (type == null) {
                logger.error(String.format("SBML.NODETYPE_ATTR not set for SBML node: %s", n));
            } else {
                // additional subtypes
                if (type.equals(SBML.NODETYPE_REACTION)) {
                    Boolean reversible = AttributeUtil.get(network, n, SBML.ATTR_REVERSIBLE, Boolean.class);
                    if (Boolean.TRUE.equals(reversible)) {
                        type = SBML.NODETYPE_REACTION_REVERSIBLE;
                    } else {
                        type = SBML.NODETYPE_REACTION_IRREVERSIBLE;
                    }
                }
                AttributeUtil.set(network, n, SBML.NODETYPE_ATTR_EXTENDED, type, String.class);
            }
        }
    }

    /**
     * Adds extended sbml interaction as edge attributes.
     * <p>
     * The extended SBML types can be used in the visual mapping.
     * This allows for instance to distinguish modifiers from activators and inhibitors.
     */
    private static void addSBMLInteractionExtended(CyNetwork network) {
        for (CyEdge e : network.getEdgeList()) {
            String type = AttributeUtil.get(network, e, SBML.INTERACTION_ATTR, String.class);
            if (type != null) {
                // additional subtypes
                if (type.equals(SBML.INTERACTION_REACTION_MODIFIER)) {
                    String sboterm = AttributeUtil.get(network, e, SBML.ATTR_SBOTERM, String.class);
                    if (SBML.SBO_INHIBITORS.contains(sboterm)) {
                        type = SBML.INTERACTION_REACTION_INHIBITOR;
                    } else if (SBML.SBO_ACTIVATORS.contains(sboterm)) {
                        type = SBML.INTERACTION_REACTION_ACTIVATOR;
                    }
                }
                AttributeUtil.set(network, e, SBML.INTERACTION_ATTR_EXTENDED, type, String.class);
            } else {
                logger.error("interaction type not set for edge: {}", e);
            }
        }
    }
}

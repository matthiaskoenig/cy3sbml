package org.cy3sbml.reader;

import org.cy3sbml.SBML;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.sbml.jsbml.QuantityWithUnit;
import org.sbml.jsbml.Unit;
import org.sbml.jsbml.UnitDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates the unit subgraphs: UnitDefinition nodes with their Unit nodes, and the
 * edges from quantities to their units.
 */
final class UnitGraphBuilder {
    private static final Logger logger = LoggerFactory.getLogger(UnitGraphBuilder.class);

    private UnitGraphBuilder() {}

    /**
     * Creates an edge to the unit for the quantity.
     *
     * @param q QuantityWithUnit
     * @return edge if unit is available, null otherwise
     */
    static CyEdge createUnitEdge(ConversionContext context, CyNode n, QuantityWithUnit q) {
        CyEdge e = null;

        // edge to unit
        if (q.isSetUnits()) {
            // Every time a new unit instance is created for base units !
            UnitDefinition ud = q.getUnitsInstance();
            CyNode udNode = context.nodeByMetaId(ud.getMetaId()).orElse(null);
            /*
            The UnitDefinition instance which has the unitsID of this SBaseWithUnit as id.
            Null if it doesn't exist. In case that the unit of this SBaseWithUnit represents
            a base Unit, a new UnitDefinition will be created and returned by this method.
            This new UnitDefinition will only contain the one unit represented by the unit
            identifier in this SBaseWithUnit. Note that the corresponding model will not
            contain this UnitDefinition. The identifier of this new UnitDefinition will
            be set to the same value as the name of the base Unit.

            I.e. in the case of a base unit we have to create the UnitDefinition node first.
            */
            if (ud != null && udNode == null) {
                logger.debug("Base UnitDefinition encountered. Creating UnitDefinition graph: {}", ud);
                String unitSid = ud.getId();
                if (context.baseUnitDefinitions().containsKey(unitSid)) {
                    // This base unit was encountered before and the network created
                    ud = context.baseUnitDefinitions().get(unitSid);
                } else {
                    // The base unit must be stored for later lookup
                    createUnitDefinitionGraph(context, ud);
                    context.baseUnitDefinitions().put(unitSid, ud);
                }
                // get the unique node
                udNode = context.nodeByMetaId(ud.getMetaId()).orElse(null);
            }
            // now the udNode should exist for sure
            if (udNode != null) {
                e = context.createEdge(n, udNode, SBML.INTERACTION_SBASE_UNITDEFINITION);
            } else {
                logger.error("UnitDefinition node not found for <{}>: {}", q, q.getId());
            }
        }
        return e;
    }

    /**
     * Creates the graph for a given UnitDefinition.
     * This is used for all UnitDefinitions in the ListOfUnitDefinitions, but
     * also for the UnitInstances of base units, which are not necessarily part
     * of the ListOfUnits. For instance substanceUnits of species.
     */
    static void createUnitDefinitionGraph(ConversionContext context, UnitDefinition ud) {
        CyNetwork network = context.network();
        CyNode n = context.createNode(ud, SBML.NODETYPE_UNIT_DEFINITION);
        AttributeWriter.setNamedSBaseAttributes(network, n, ud);

        for (Unit unit : ud.getListOfUnits()) {
            if (ud.isSetId() && unit.isSetKind()) {
                CyNode uNode = context.createNode(unit, SBML.NODETYPE_UNIT);
                AttributeWriter.setUnitAttributes(network, uNode, unit);

                // edge to UnitDefinition
                context.createEdge(uNode, n, SBML.INTERACTION_UNIT_UNITDEFINITION);
            } else {
                logger.warn("Unit could not be created due to missing UnitDefinition id or unit kind: {}", ud);
            }
        }
    }
}

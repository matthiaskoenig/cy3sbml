package org.cy3sbml.reader;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.cy3sbml.SBML;
import org.cy3sbml.comp.SBaseRefResolver;
import org.cy3sbml.util.AttributeUtil;
import org.cy3sbml.util.MappingUtil;
import org.cytoscape.group.CyGroup;
import org.cytoscape.group.CyGroupFactory;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.sbml.jsbml.NamedSBase;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBase;
import org.sbml.jsbml.UnitDefinition;
import org.sbml.jsbml.ext.comp.Port;
import org.sbml.jsbml.ext.groups.Group;

/**
 * State of the conversion of one SBML model into one network.
 * <p>
 * The context owns the network and the lookups from SBML ids and metaIds to the
 * created nodes. Different models can use the same ids and metaIds, so every
 * model gets its own context.
 */
final class ConversionContext {
    private final SBMLDocument document;
    private final CyNetwork network;
    private final CyGroupFactory groupFactory;
    private final SBaseRefResolver sBaseRefResolver;

    private final Map<String, CyNode> metaId2Node = new HashMap<>();
    private final Map<String, CyNode> id2Node = new HashMap<>();
    // storage of groups to create in subnetworks
    private final Set<CyGroup> groups = new HashSet<>();
    // base UnitDefinition lookup
    private final Map<String, UnitDefinition> baseUnitDefinitions = new HashMap<>();

    /**
     * Creates the context for the network.
     *
     * @param document document of the model, used to create unique metaIds
     * @param sBaseRefResolver resolver of the comp references, shared by the models of the
     *     document
     */
    ConversionContext(
            SBMLDocument document, CyNetwork network, CyGroupFactory groupFactory, SBaseRefResolver sBaseRefResolver) {
        this.document = document;
        this.network = network;
        this.groupFactory = groupFactory;
        this.sBaseRefResolver = sBaseRefResolver;
    }

    CyNetwork network() {
        return network;
    }

    /**
     * Creates SBML node for SBase.
     * Only function to create SBase nodes in the network. The node is stored
     * in the node mapping and the corresponding node attribute is set.
     *
     * @param sbase    sbase to add node for
     * @param sbmlType SBML type of the node
     */
    CyNode createNode(SBase sbase, String sbmlType) {
        CyNode n = network.addNode();
        // Set unique metaId
        MappingUtil.setSBaseMetaId(document, sbase);
        // Set attributes
        String metaId = sbase.getMetaId();
        AttributeUtil.set(network, n, SBML.ATTR_CYID, metaId, String.class);
        AttributeUtil.set(network, n, SBML.NODETYPE_ATTR, sbmlType, String.class);
        AttributeUtil.set(network, n, SBML.LABEL, metaId, String.class);
        // store nodes, only SIds: unit definitions (UnitSId) and ports (PortSId) have their own namespaces
        metaId2Node.put(metaId, n);
        if (sbase instanceof NamedSBase nsb
                && nsb.isSetId()
                && !(sbase instanceof UnitDefinition)
                && !(sbase instanceof Port)) {
            id2Node.put(nsb.getId(), n);
        }
        return n;
    }

    /**
     * Creates SBML edge.
     * Only function to create edges between SBase nodes in the network.
     * All edges are directed.
     */
    CyEdge createEdge(CyNode source, CyNode target, String interactionType) {
        CyEdge e = network.addEdge(source, target, true);
        AttributeUtil.set(network, e, SBML.INTERACTION_ATTR, interactionType, String.class);
        return e;
    }

    /**
     * Creates group node for the given group.
     */
    CyGroup createGroup(Group group) {
        // metaId for identification
        MappingUtil.setSBaseMetaId(document, group);

        CyGroup cyGroup = groupFactory.createGroup(network, true);
        groups.add(cyGroup);

        // set attributes
        //  cyGroup nodes are registered in the root network, so the corresponding
        //  attributes must be set on the root network.
        CyNode n = cyGroup.getGroupNode();
        String metaId = group.getMetaId();
        CyRootNetwork rootNetwork = ((CySubNetwork) network).getRootNetwork();
        AttributeUtil.set(rootNetwork, n, SBML.ATTR_CYID, metaId, String.class);
        AttributeUtil.set(rootNetwork, n, SBML.NODETYPE_ATTR, SBML.NODETYPE_GROUP, String.class);
        AttributeUtil.set(rootNetwork, n, SBML.LABEL, metaId, String.class);
        AttributeWriter.setNamedSBaseAttributes(rootNetwork, n, group);

        // store nodes
        metaId2Node.put(metaId, n);
        if (group.isSetId()) {
            id2Node.put(group.getId(), n);
        }
        return cyGroup;
    }

    /** Resolver of the comp references of the document. */
    SBaseRefResolver sBaseRefResolver() {
        return sBaseRefResolver;
    }

    /** The node of the element with the SId. */
    Optional<CyNode> nodeById(String id) {
        return Optional.ofNullable(id2Node.get(id));
    }

    Optional<CyNode> nodeByMetaId(String metaId) {
        return Optional.ofNullable(metaId2Node.get(metaId));
    }

    /**
     * Groups created in the network, which are added to the subnetworks.
     */
    Set<CyGroup> groups() {
        return Collections.unmodifiableSet(groups);
    }

    /**
     * Base UnitDefinitions by unit id. JSBML creates a new UnitDefinition instance for
     * every lookup of a base unit, so the first instance is stored here for later lookup.
     */
    Map<String, UnitDefinition> baseUnitDefinitions() {
        return baseUnitDefinitions;
    }
}

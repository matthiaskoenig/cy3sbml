package org.cy3sbml.reader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.cy3sbml.SBML;
import org.cy3sbml.comp.SBaseRefResolver;
import org.cy3sbml.util.AttributeUtil;
import org.cy3sbml.util.MappingUtil;
import org.cytoscape.group.CyGroup;
import org.cytoscape.group.CyGroupFactory;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.sbml.jsbml.NamedSBase;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBase;
import org.sbml.jsbml.UnitDefinition;
import org.sbml.jsbml.ext.comp.Port;
import org.sbml.jsbml.ext.groups.Group;
import org.sbml.jsbml.ext.layout.Layout;

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
    // edges that represent an SBase (species references), by instance
    private final IdentityHashMap<SBase, CyEdge> sbase2Edge = new IdentityHashMap<>();
    // SBML groups, created as Cytoscape groups in the network and its subnetworks
    private final List<Group> groups = new ArrayList<>();
    private final Map<Group, CyGroup> cyGroups = new LinkedHashMap<>();
    // layouts of the model, created as layout networks by LayoutNetworkBuilder
    private final List<Layout> layouts = new ArrayList<>();
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

    /** Document of the model. */
    SBMLDocument document() {
        return document;
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
     * Creates the SBML edge that represents the SBase, e.g. the edge of a species
     * reference, which {@link #edgeOf(SBase)} finds.
     */
    CyEdge createEdge(CyNode source, CyNode target, String interactionType, SBase sbase) {
        CyEdge e = createEdge(source, target, interactionType);
        sbase2Edge.put(sbase, e);
        return e;
    }

    /**
     * Registers the SBML group, whose Cytoscape groups {@link #createGroups(CyNetwork)} creates.
     */
    void addGroup(Group group) {
        // metaId for identification
        MappingUtil.setSBaseMetaId(document, group);
        groups.add(group);
    }

    /**
     * Creates the Cytoscape groups of the registered SBML groups in the network, the network
     * of the context or one of its subnetworks, see {@link GroupBuilder}. The group nodes of
     * the network of the context are found by id and metaId.
     *
     * @return the created groups by SBML group
     */
    Map<Group, CyGroup> createGroups(CyNetwork net) {
        boolean contextNetwork = net.equals(network);
        Map<Group, CyGroup> created =
                new GroupBuilder(groupFactory, this::nodeByMetaId, net, contextNetwork).build(groups);
        if (contextNetwork) {
            for (Map.Entry<Group, CyGroup> entry : created.entrySet()) {
                Group group = entry.getKey();
                CyNode n = entry.getValue().getGroupNode();
                metaId2Node.put(group.getMetaId(), n);
                if (group.isSetId()) {
                    id2Node.put(group.getId(), n);
                }
            }
            cyGroups.putAll(created);
        }
        return created;
    }

    /**
     * Registers the layout, whose layout network {@link LayoutNetworkBuilder} creates after the
     * subnetworks.
     */
    void addLayout(Layout layout) {
        layouts.add(layout);
    }

    /** The registered layouts in document order. */
    List<Layout> layouts() {
        return Collections.unmodifiableList(layouts);
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

    /** The edge that represents the SBase, e.g. a species reference. */
    Optional<CyEdge> edgeOf(SBase sbase) {
        return Optional.ofNullable(sbase2Edge.get(sbase));
    }

    /**
     * Cytoscape groups created in the network of the context, by SBML group.
     */
    Map<Group, CyGroup> groups() {
        return Collections.unmodifiableMap(cyGroups);
    }

    /**
     * Base UnitDefinitions by unit id. JSBML creates a new UnitDefinition instance for
     * every lookup of a base unit, so the first instance is stored here for later lookup.
     */
    Map<String, UnitDefinition> baseUnitDefinitions() {
        return baseUnitDefinitions;
    }
}

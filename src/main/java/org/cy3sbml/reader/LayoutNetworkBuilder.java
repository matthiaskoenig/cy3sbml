package org.cy3sbml.reader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.cy3sbml.SBML;
import org.cy3sbml.util.AttributeUtil;
import org.cy3sbml.util.MappingUtil;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.sbml.jsbml.SBase;
import org.sbml.jsbml.ext.layout.AbstractReferenceGlyph;
import org.sbml.jsbml.ext.layout.CompartmentGlyph;
import org.sbml.jsbml.ext.layout.GeneralGlyph;
import org.sbml.jsbml.ext.layout.GraphicalObject;
import org.sbml.jsbml.ext.layout.Layout;
import org.sbml.jsbml.ext.layout.ReactionGlyph;
import org.sbml.jsbml.ext.layout.ReferenceGlyph;
import org.sbml.jsbml.ext.layout.SpeciesGlyph;
import org.sbml.jsbml.ext.layout.SpeciesReferenceGlyph;
import org.sbml.jsbml.ext.layout.SpeciesReferenceRole;
import org.sbml.jsbml.ext.layout.TextGlyph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates a layout network for every layout of a model (#71): a subnetwork of the root network
 * of the model with a node per glyph at the position and in the size of the glyph.
 * <p>
 * A glyph that references an element with a node in the network of the model represents that
 * node: its node is a copy of the shared columns of the node, so several glyphs of one element
 * (aliases) have the same {@code cyId}. The geometry and the glyph are in local columns of the
 * layout network, see {@link SBML#ATTR_LAYOUT_GLYPH}.
 */
final class LayoutNetworkBuilder {
    private static final Logger logger = LoggerFactory.getLogger(LayoutNetworkBuilder.class);

    /** Interaction types of the edges between a reaction or transition and its participants. */
    static final Set<String> PARTICIPANT_INTERACTIONS = Set.of(
            SBML.INTERACTION_REACTION_REACTANT,
            SBML.INTERACTION_REACTION_PRODUCT,
            SBML.INTERACTION_REACTION_MODIFIER,
            SBML.INTERACTION_REACTION_ACTIVATOR,
            SBML.INTERACTION_REACTION_INHIBITOR,
            SBML.INTERACTION_QUAL_TRANSITION_INPUT,
            SBML.INTERACTION_QUAL_TRANSITION_OUTPUT);

    /** Node types of the reactions and transitions. */
    private static final Set<String> REACTION_TYPES = Set.of(SBML.NODETYPE_REACTION, SBML.NODETYPE_QUAL_TRANSITION);

    /** Width and height of a generated node and of a reaction glyph without dimensions. */
    static final double GENERATED_SIZE = 12.0;

    private final ConversionContext context;

    /**
     * @param context context of the model, after all package readers ran
     */
    LayoutNetworkBuilder(ConversionContext context) {
        this.context = context;
    }

    /**
     * Creates the layout networks of the registered layouts in the root network.
     *
     * @param rootNetwork root network of the model
     * @param name        name of the root network, the prefix of the network names
     * @return the layout networks in the order of the layouts
     */
    List<CyNetwork> build(CyRootNetwork rootNetwork, String name) {
        List<CyNetwork> networks = new ArrayList<>();
        int index = 0;
        for (Layout layout : context.layouts()) {
            index++;
            String id = layout.isSetId() ? layout.getId() : String.valueOf(index);
            CySubNetwork network = rootNetwork.addSubNetwork();
            try {
                new LayoutGraph(rootNetwork, network).read(layout);
                network.getRow(network).set(CyNetwork.NAME, name + SBML.SUFFIX_SUBNETWORK_LAYOUT + id);
                AttributeUtil.set(network, network, SBML.NETWORKTYPE_ATTR, SBML.NETWORKTYPE_LAYOUT, String.class);
                AttributeUtil.set(network, network, SBML.SUBNETWORK_ATTR, SBML.SUBNETWORK_LAYOUT, String.class);
                setLocal(network, network, SBML.ATTR_LAYOUT_ID, layout.isSetId() ? layout.getId() : null, String.class);
                networks.add(network);
            } catch (RuntimeException e) {
                // a layout never fails the import
                logger.warn("The layout '{}' could not be read: {}", id, e.getMessage(), e);
                rootNetwork.removeSubNetwork(network);
            }
        }
        return networks;
    }

    /** Sets the value in the local table of the network, creating the column if needed. */
    private static void setLocal(CyNetwork network, CyIdentifiable entry, String column, Object value, Class<?> type) {
        if (value == null) {
            return;
        }
        CyRow row = network.getRow(entry, CyNetwork.LOCAL_ATTRS);
        CyTable table = row.getTable();
        if (table.getColumn(column) == null) {
            table.createColumn(column, type, false);
        }
        row.set(column, value);
    }

    /** The layout network of one layout. */
    private final class LayoutGraph {
        private final CyRootNetwork rootNetwork;
        private final CySubNetwork network;
        // glyph nodes by glyph id, glyph ids are unique in a layout
        private final Map<String, CyNode> glyphNodes = new HashMap<>();
        // glyph nodes by glyph instance, also of the glyphs without id
        private final IdentityHashMap<GraphicalObject, CyNode> nodeOfGlyph = new IdentityHashMap<>();
        // the glyph nodes of every represented node of the network of the model
        private final Map<CyNode, List<CyNode>> glyphsOfNode = new LinkedHashMap<>();
        // the represented node of the network of the model of every glyph node
        private final Map<CyNode, CyNode> representedBy = new HashMap<>();
        private final Map<CyNode, GlyphBox> boxes = new HashMap<>();
        private final List<GeneralGlyph> generalGlyphs = new ArrayList<>();

        LayoutGraph(CyRootNetwork rootNetwork, CySubNetwork network) {
            this.rootNetwork = rootNetwork;
            this.network = network;
        }

        void read(Layout layout) {
            for (CompartmentGlyph glyph : layout.getListOfCompartmentGlyphs()) {
                glyphNode(glyph, SBML.NODETYPE_LAYOUT_COMPARTMENTGLYPH);
            }
            for (SpeciesGlyph glyph : layout.getListOfSpeciesGlyphs()) {
                glyphNode(glyph, SBML.NODETYPE_LAYOUT_SPECIESGLYPH);
            }
            for (ReactionGlyph glyph : layout.getListOfReactionGlyphs()) {
                glyphNode(glyph, SBML.NODETYPE_LAYOUT_REACTIONGLYPH);
            }
            for (GraphicalObject glyph : layout.getListOfAdditionalGraphicalObjects()) {
                additionalGlyph(glyph);
            }
            labels(layout);
            for (ReactionGlyph glyph : layout.getListOfReactionGlyphs()) {
                reactionGlyphEdges(glyph);
            }
            for (GeneralGlyph glyph : generalGlyphs) {
                referenceGlyphEdges(glyph);
            }
            generatedReactions();
        }

        /** Additional graphical objects, with the sub glyphs of general glyphs. */
        private void additionalGlyph(GraphicalObject glyph) {
            if (glyph instanceof TextGlyph) {
                // a label, not a node
                return;
            }
            glyphNode(glyph, glyphType(glyph));
            if (glyph instanceof GeneralGlyph generalGlyph) {
                generalGlyphs.add(generalGlyph);
            }
            if (glyph instanceof GeneralGlyph generalGlyph && generalGlyph.isSetListOfSubGlyphs()) {
                for (GraphicalObject subGlyph : generalGlyph.getListOfSubGlyphs()) {
                    additionalGlyph(subGlyph);
                }
            }
        }

        private static String glyphType(GraphicalObject glyph) {
            if (glyph instanceof CompartmentGlyph) {
                return SBML.NODETYPE_LAYOUT_COMPARTMENTGLYPH;
            } else if (glyph instanceof SpeciesGlyph) {
                return SBML.NODETYPE_LAYOUT_SPECIESGLYPH;
            } else if (glyph instanceof ReactionGlyph) {
                return SBML.NODETYPE_LAYOUT_REACTIONGLYPH;
            } else if (glyph instanceof GeneralGlyph) {
                return SBML.NODETYPE_LAYOUT_GENERALGLYPH;
            }
            return SBML.NODETYPE_LAYOUT_GRAPHICALOBJECT;
        }

        /** Creates the node of the glyph. */
        private CyNode glyphNode(GraphicalObject glyph, String glyphType) {
            CyNode node = network.addNode();
            Optional<CyNode> represented = representedNode(glyph);
            if (represented.isPresent()) {
                represent(node, represented.get());
            } else {
                // a node of its own, found by the metaId of the glyph
                if (glyph instanceof AbstractReferenceGlyph referenceGlyph && referenceGlyph.isSetReference()) {
                    logger.debug(
                            "The element '{}' of the glyph '{}' is not in the model.",
                            referenceGlyph.getReference(),
                            glyph.getId());
                }
                MappingUtil.setSBaseMetaId(context.document(), glyph);
                String label = glyph.isSetName() ? glyph.getName() : glyph.getId();
                AttributeUtil.set(network, node, SBML.ATTR_CYID, glyph.getMetaId(), String.class);
                AttributeUtil.set(network, node, SBML.NODETYPE_ATTR, glyphType, String.class);
                AttributeUtil.set(network, node, SBML.ATTR_ID, glyph.isSetId() ? glyph.getId() : null, String.class);
                AttributeUtil.set(
                        network, node, SBML.ATTR_NAME, glyph.isSetName() ? glyph.getName() : null, String.class);
                AttributeUtil.set(network, node, CyNetwork.NAME, label, String.class);
                AttributeUtil.set(network, node, SBML.LABEL, label, String.class);
            }
            if (glyph.isSetId()) {
                glyphNodes.put(glyph.getId(), node);
            }
            nodeOfGlyph.put(glyph, node);
            setLocal(network, node, SBML.ATTR_LAYOUT_GLYPH, glyphKey(glyph), String.class);
            setLocal(network, node, SBML.ATTR_LAYOUT_GLYPH_TYPE, glyphType, String.class);
            // reaction glyphs are small, often only a position (KEGG layouts)
            setBox(node, GlyphBox.of(glyph, glyph instanceof ReactionGlyph ? GENERATED_SIZE : GlyphBox.DEFAULT_SIZE));
            return node;
        }

        /**
         * Identifies the glyph in the layout network: the id, else the metaId, which is the
         * same in every import of the file (glyphs without id are invalid, but common, e.g.
         * the KEGG layouts of KEGGtranslator).
         */
        private String glyphKey(GraphicalObject glyph) {
            if (glyph.isSetId()) {
                return glyph.getId();
            }
            MappingUtil.setSBaseMetaId(context.document(), glyph);
            return glyph.getMetaId();
        }

        /**
         * The node of the network of the model the glyph represents: the element of its
         * reference, else of its metaidRef.
         */
        private Optional<CyNode> representedNode(GraphicalObject glyph) {
            if (glyph instanceof AbstractReferenceGlyph referenceGlyph && referenceGlyph.isSetReference()) {
                return context.nodeById(referenceGlyph.getReference());
            }
            if (glyph.isSetMetaidRef()) {
                return context.nodeByMetaId(glyph.getMetaidRef());
            }
            return Optional.empty();
        }

        /** Makes the node a copy of the represented node of the network of the model. */
        private void represent(CyNode node, CyNode modelNode) {
            CyTable shared = rootNetwork.getSharedNodeTable();
            copyRow(shared, shared.getRow(modelNode.getSUID()), shared.getRow(node.getSUID()));
            network.getRow(node)
                    .set(CyNetwork.NAME, shared.getRow(node.getSUID()).get(SBML.ATTR_NAME, String.class));
            glyphsOfNode.computeIfAbsent(modelNode, n -> new ArrayList<>()).add(node);
            representedBy.put(node, modelNode);
        }

        private void setBox(CyNode node, GlyphBox box) {
            boxes.put(node, box);
            setLocal(network, node, SBML.ATTR_LAYOUT_X, box.x(), Double.class);
            setLocal(network, node, SBML.ATTR_LAYOUT_Y, box.y(), Double.class);
            setLocal(network, node, SBML.ATTR_LAYOUT_WIDTH, box.width(), Double.class);
            setLocal(network, node, SBML.ATTR_LAYOUT_HEIGHT, box.height(), Double.class);
        }

        // ------------------------------------------------------------
        // edges
        // ------------------------------------------------------------

        /**
         * The edges of a reaction glyph: one per species reference glyph, or, without species
         * reference glyphs, the edges of its reaction to all glyphs of the participants.
         */
        private void reactionGlyphEdges(ReactionGlyph glyph) {
            CyNode node = nodeOfGlyph.get(glyph);
            if (glyph.isSetListOfSpeciesReferenceGlyphs() && glyph.getSpeciesReferenceGlyphCount() > 0) {
                for (SpeciesReferenceGlyph speciesReferenceGlyph : glyph.getListOfSpeciesReferenceGlyphs()) {
                    speciesReferenceEdge(node, speciesReferenceGlyph);
                }
            } else if (representedBy.containsKey(node)) {
                participantEdges(representedBy.get(node), node);
            }
        }

        /**
         * The edge of a species reference glyph between the reaction glyph and the species
         * glyph: a copy of the edge of the model it stands for, else an edge of the role.
         */
        private void speciesReferenceEdge(CyNode reactionNode, SpeciesReferenceGlyph glyph) {
            CyNode speciesNode = glyph.isSetSpeciesGlyph() ? glyphNodes.get(glyph.getSpeciesGlyph()) : null;
            if (speciesNode == null) {
                logger.debug(
                        "The species glyph of the species reference glyph '{}' is not in the layout.", glyph.getId());
                return;
            }
            SpeciesReferenceRole role = glyph.isSetSpeciesReferenceRole() ? glyph.getSpeciesReferenceRole() : null;
            CyNode reaction = representedBy.get(reactionNode);
            Optional<CyEdge> modelEdge = modelEdge(glyph, reaction, representedBy.get(speciesNode), role);
            if (modelEdge.isPresent()) {
                copyEdge(modelEdge.get(), reaction, reactionNode, speciesNode);
                return;
            }
            boolean transition = reaction != null
                    && SBML.NODETYPE_QUAL_TRANSITION.equals(
                            context.network().getRow(reaction).get(SBML.NODETYPE_ATTR, String.class));
            // all edges of a reaction or transition start at it, as in the network of the model
            CyEdge edge = network.addEdge(reactionNode, speciesNode, true);
            AttributeUtil.set(network, edge, SBML.INTERACTION_ATTR, roleInteraction(role, transition), String.class);
        }

        /**
         * The edge of the model of a species reference glyph: the edge of its species
         * reference if it connects the reaction and the species, else the edge between them
         * that fits the role, else the only edge between them.
         */
        private Optional<CyEdge> modelEdge(
                SpeciesReferenceGlyph glyph, CyNode reaction, CyNode species, SpeciesReferenceRole role) {
            // without the reaction and the species in the model there is no edge between them
            if (reaction == null || species == null) {
                return Optional.empty();
            }
            if (glyph.isSetSpeciesReference() && glyph.getModel() != null) {
                SBase speciesReference = glyph.getModel().findNamedSBase(glyph.getSpeciesReference());
                Optional<CyEdge> edge = speciesReference == null ? Optional.empty() : context.edgeOf(speciesReference);
                // the species reference of another reaction or species is not the edge
                if (edge.isPresent() && connects(edge.get(), reaction, species)) {
                    return edge;
                }
            }
            CyNetwork modelNetwork = context.network();
            List<CyEdge> edges = modelNetwork.getConnectingEdgeList(reaction, species, CyEdge.Type.ANY).stream()
                    .filter(e -> PARTICIPANT_INTERACTIONS.contains(interaction(modelNetwork, e)))
                    .toList();
            Set<String> fitting = roleInteractions(role);
            List<CyEdge> fits = edges.stream()
                    .filter(e -> fitting.contains(interaction(modelNetwork, e)))
                    .toList();
            if (fits.size() == 1) {
                return Optional.of(fits.get(0));
            }
            return edges.size() == 1 ? Optional.of(edges.get(0)) : Optional.empty();
        }

        /**
         * Edges from the node of a reaction or transition to its participants, to the glyph of
         * every participant nearest to the node.
         */
        private void participantEdges(CyNode reaction, CyNode reactionNode) {
            GlyphBox box = boxes.get(reactionNode);
            for (CyEdge modelEdge : participantModelEdges(reaction)) {
                nearestGlyph(other(modelEdge, reaction), box)
                        .ifPresent(glyph -> copyEdge(modelEdge, reaction, reactionNode, glyph));
            }
        }

        /** The glyph of the node nearest to the box, the first of equally near glyphs. */
        private Optional<CyNode> nearestGlyph(CyNode node, GlyphBox box) {
            CyNode nearest = null;
            for (CyNode glyph : glyphsOfNode.getOrDefault(node, List.of())) {
                if (nearest == null
                        || boxes.get(glyph).distance(box) < boxes.get(nearest).distance(box)) {
                    nearest = glyph;
                }
            }
            return Optional.ofNullable(nearest);
        }

        private List<CyEdge> participantModelEdges(CyNode reaction) {
            CyNetwork modelNetwork = context.network();
            return modelNetwork.getAdjacentEdgeList(reaction, CyEdge.Type.ANY).stream()
                    .filter(e -> PARTICIPANT_INTERACTIONS.contains(interaction(modelNetwork, e)))
                    .filter(e -> !other(e, reaction).equals(reaction))
                    .toList();
        }

        /**
         * A generated node for every reaction and transition without glyph whose participants
         * have glyphs: a small node without label between the glyph of every participant
         * nearest to the centroid of all glyphs of the participants, connected to them.
         */
        private void generatedReactions() {
            CyNetwork modelNetwork = context.network();
            for (CyNode reaction : modelNetwork.getNodeList()) {
                if (glyphsOfNode.containsKey(reaction)
                        || !REACTION_TYPES.contains(
                                modelNetwork.getRow(reaction).get(SBML.NODETYPE_ATTR, String.class))) {
                    continue;
                }
                List<CyNode> participants = participantModelEdges(reaction).stream()
                        .map(e -> other(e, reaction))
                        .distinct()
                        .toList();
                List<GlyphBox> allGlyphs = participants.stream()
                        .flatMap(p -> glyphsOfNode.getOrDefault(p, List.of()).stream())
                        .map(boxes::get)
                        .toList();
                if (allGlyphs.isEmpty()) {
                    continue;
                }
                GlyphBox centre = GlyphBox.centroid(allGlyphs, GENERATED_SIZE);
                // the nearest glyph of every participant, by participant model edge
                Map<CyEdge, CyNode> nearest = new LinkedHashMap<>();
                for (CyEdge modelEdge : participantModelEdges(reaction)) {
                    nearestGlyph(other(modelEdge, reaction), centre).ifPresent(g -> nearest.put(modelEdge, g));
                }
                CyNode node = network.addNode();
                represent(node, reaction);
                // not drawn in the layout: no label
                AttributeUtil.set(network, node, SBML.LABEL, "", String.class);
                setLocal(network, node, SBML.ATTR_LAYOUT_GLYPH_TYPE, SBML.NODETYPE_LAYOUT_GENERATED, String.class);
                setBox(
                        node,
                        GlyphBox.centroid(
                                nearest.values().stream()
                                        .distinct()
                                        .map(boxes::get)
                                        .toList(),
                                GENERATED_SIZE));
                nearest.forEach((modelEdge, glyph) -> copyEdge(modelEdge, reaction, node, glyph));
            }
        }

        /** The edges of the reference glyphs of a general glyph to the referenced glyphs. */
        private void referenceGlyphEdges(GeneralGlyph glyph) {
            CyNode node = nodeOfGlyph.get(glyph);
            if (!glyph.isSetListOfReferenceGlyphs()) {
                return;
            }
            for (ReferenceGlyph referenceGlyph : glyph.getListOfReferenceGlyphs()) {
                CyNode target = referenceGlyph.isSetGlyph() ? glyphNodes.get(referenceGlyph.getGlyph()) : null;
                if (target == null) {
                    logger.debug("The glyph of the reference glyph '{}' is not in the layout.", referenceGlyph.getId());
                    continue;
                }
                CyEdge edge = network.addEdge(node, target, true);
                AttributeUtil.set(
                        network, edge, SBML.INTERACTION_ATTR, SBML.INTERACTION_LAYOUT_REFERENCE, String.class);
                setLocal(
                        network,
                        edge,
                        SBML.ATTR_LAYOUT_ROLE,
                        referenceGlyph.isSetRole() ? referenceGlyph.getRole() : null,
                        String.class);
            }
        }

        /**
         * Adds a copy of the edge of the model between the reaction and a participant, with
         * the direction of the model edge (from the reaction), between the node of the
         * reaction and the glyph of the participant.
         */
        private void copyEdge(CyEdge modelEdge, CyNode reaction, CyNode reactionNode, CyNode participantNode) {
            CyEdge edge = modelEdge.getSource().equals(reaction)
                    ? network.addEdge(reactionNode, participantNode, true)
                    : network.addEdge(participantNode, reactionNode, true);
            CyTable shared = rootNetwork.getSharedEdgeTable();
            copyRow(shared, shared.getRow(modelEdge.getSUID()), shared.getRow(edge.getSUID()));
            network.getRow(edge)
                    .set(CyNetwork.NAME, shared.getRow(edge.getSUID()).get(SBML.ATTR_NAME, String.class));
        }

        /**
         * The text glyphs of a glyph are its label: the text, else the label of the origin of
         * text. Text glyphs without graphical object are not shown.
         */
        private void labels(Layout layout) {
            Map<CyNode, List<String>> labels = new LinkedHashMap<>();
            for (TextGlyph textGlyph : layout.getListOfTextGlyphs()) {
                CyNode node = textGlyph.isSetGraphicalObject() ? glyphNodes.get(textGlyph.getGraphicalObject()) : null;
                Optional<String> text = text(textGlyph);
                if (node == null || text.isEmpty()) {
                    logger.debug("The text glyph '{}' is not shown.", textGlyph.getId());
                    continue;
                }
                labels.computeIfAbsent(node, n -> new ArrayList<>()).add(text.get());
            }
            labels.forEach((node, texts) ->
                    AttributeUtil.set(network, node, SBML.LABEL, String.join(" ", texts), String.class));
        }

        private Optional<String> text(TextGlyph textGlyph) {
            if (textGlyph.isSetText()) {
                return Optional.of(textGlyph.getText());
            }
            if (textGlyph.isSetOriginOfText()) {
                String origin = textGlyph.getOriginOfText();
                return Optional.of(context.nodeById(origin)
                        .map(n -> context.network().getRow(n).get(SBML.LABEL, String.class))
                        .orElse(origin));
            }
            return Optional.empty();
        }
    }

    private static String interaction(CyNetwork network, CyEdge edge) {
        return network.getRow(edge).get(SBML.INTERACTION_ATTR, String.class);
    }

    private static boolean connects(CyEdge edge, CyNode node, CyNode other) {
        return (edge.getSource().equals(node) && edge.getTarget().equals(other))
                || (edge.getSource().equals(other) && edge.getTarget().equals(node));
    }

    private static CyNode other(CyEdge edge, CyNode node) {
        return edge.getSource().equals(node) ? edge.getTarget() : edge.getSource();
    }

    /** The interactions of the model edges that fit the role, all for an undefined role. */
    private static Set<String> roleInteractions(SpeciesReferenceRole role) {
        if (role == null) {
            return PARTICIPANT_INTERACTIONS;
        }
        return switch (role) {
            case SUBSTRATE, SIDESUBSTRATE ->
                Set.of(SBML.INTERACTION_REACTION_REACTANT, SBML.INTERACTION_QUAL_TRANSITION_INPUT);
            case PRODUCT, SIDEPRODUCT ->
                Set.of(SBML.INTERACTION_REACTION_PRODUCT, SBML.INTERACTION_QUAL_TRANSITION_OUTPUT);
            case MODIFIER, ACTIVATOR, INHIBITOR ->
                Set.of(
                        SBML.INTERACTION_REACTION_MODIFIER,
                        SBML.INTERACTION_REACTION_ACTIVATOR,
                        SBML.INTERACTION_REACTION_INHIBITOR,
                        SBML.INTERACTION_QUAL_TRANSITION_INPUT);
            case UNDEFINED -> PARTICIPANT_INTERACTIONS;
        };
    }

    /** The interaction of an edge of a species reference glyph without edge in the model. */
    private static String roleInteraction(SpeciesReferenceRole role, boolean transition) {
        if (role == SpeciesReferenceRole.PRODUCT || role == SpeciesReferenceRole.SIDEPRODUCT) {
            return transition ? SBML.INTERACTION_QUAL_TRANSITION_OUTPUT : SBML.INTERACTION_REACTION_PRODUCT;
        }
        if (transition) {
            return SBML.INTERACTION_QUAL_TRANSITION_INPUT;
        }
        if (role == SpeciesReferenceRole.SUBSTRATE || role == SpeciesReferenceRole.SIDESUBSTRATE) {
            return SBML.INTERACTION_REACTION_REACTANT;
        } else if (role == SpeciesReferenceRole.ACTIVATOR) {
            return SBML.INTERACTION_REACTION_ACTIVATOR;
        } else if (role == SpeciesReferenceRole.INHIBITOR) {
            return SBML.INTERACTION_REACTION_INHIBITOR;
        }
        return SBML.INTERACTION_REACTION_MODIFIER;
    }

    /** Copies the values of all columns but the primary key from one row of the table to another. */
    private static void copyRow(CyTable table, CyRow from, CyRow to) {
        for (CyColumn column : table.getColumns()) {
            if (column.isPrimaryKey()) {
                continue;
            }
            Object value = from.getRaw(column.getName());
            if (value != null) {
                to.set(column.getName(), value);
            }
        }
    }
}

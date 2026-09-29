package org.cy3sbml.reader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.cy3sbml.SBML;
import org.cy3sbml.util.AttributeUtil;
import org.cy3sbml.util.MappingUtil;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.sbml.jsbml.ext.layout.AbstractReferenceGlyph;
import org.sbml.jsbml.ext.layout.CompartmentGlyph;
import org.sbml.jsbml.ext.layout.GeneralGlyph;
import org.sbml.jsbml.ext.layout.GraphicalObject;
import org.sbml.jsbml.ext.layout.Layout;
import org.sbml.jsbml.ext.layout.ReactionGlyph;
import org.sbml.jsbml.ext.layout.SpeciesGlyph;
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
        // the glyph nodes of every represented node of the network of the model
        private final Map<CyNode, List<CyNode>> glyphsOfNode = new LinkedHashMap<>();

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
        }

        /** Additional graphical objects, with the sub glyphs of general glyphs. */
        private void additionalGlyph(GraphicalObject glyph) {
            if (glyph instanceof TextGlyph) {
                // a label, not a node
                return;
            }
            glyphNode(glyph, glyphType(glyph));
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
            setLocal(network, node, SBML.ATTR_LAYOUT_GLYPH, glyphKey(glyph), String.class);
            setLocal(network, node, SBML.ATTR_LAYOUT_GLYPH_TYPE, glyphType, String.class);
            setBox(node, GlyphBox.of(glyph));
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
        }

        private void setBox(CyNode node, GlyphBox box) {
            setLocal(network, node, SBML.ATTR_LAYOUT_X, box.x(), Double.class);
            setLocal(network, node, SBML.ATTR_LAYOUT_Y, box.y(), Double.class);
            setLocal(network, node, SBML.ATTR_LAYOUT_WIDTH, box.width(), Double.class);
            setLocal(network, node, SBML.ATTR_LAYOUT_HEIGHT, box.height(), Double.class);
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

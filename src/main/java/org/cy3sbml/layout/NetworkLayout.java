package org.cy3sbml.layout;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.ext.layout.BoundingBox;
import org.sbml.jsbml.ext.layout.Dimensions;
import org.sbml.jsbml.ext.layout.GraphicalObject;
import org.sbml.jsbml.ext.layout.Layout;
import org.sbml.jsbml.ext.layout.Point;
import org.sbml.jsbml.ext.layout.ReactionGlyph;
import org.sbml.jsbml.ext.layout.SpeciesGlyph;
import org.sbml.jsbml.ext.qual.QualConstants;
import org.sbml.jsbml.ext.qual.QualModelPlugin;
import org.sbml.jsbml.ext.qual.QualitativeSpecies;
import org.sbml.jsbml.ext.qual.Transition;

/**
 * Get information for Layout positions and boundary boxes.
 * - Compartment information is ignored.
 * - Only the first layout is used, additional layouts are not taken into account
 * <p>
 * Special care has to be taken about the differences between the layout and the underlying graph.
 * In the Layout one species can have multiple layouts information or no layout information attached.
 * Only layout information which is a one-to-one mapping to species and reactions is used.
 * <p>
 * - no layout information : generic layout information is generated
 * - multiple layout information : last layout information is used.
 */
public class NetworkLayout {

    final String LAYOUT_NS = "http://www.sbml.org/sbml/level3/version1/layout/version1";
    public static final String ATT_LAYOUT_HEIGHT = "Layout Height";
    public static final String ATT_LAYOUT_WIDTH = "Layout Width";

    public static final double GENERIC_HEIGHT = 30.0;
    public static final double GENERIC_WIDTH = 30.0;
    public static final double GENERIC_X = 0.0;
    public static final double GENERIC_Y = 0.0;

    private double min_x = 0.0;
    private double min_y = 0.0;
    private double max_x = 0.0;
    private double max_y = 0.0;

    private SBMLDocument document;
    private Layout layout;
    private Map<String, BoundingBox> speciesBoundingBoxes;
    private Map<String, BoundingBox> reactionBoundingBoxes;
    private Map<String, BoundingBox> speciesGlyphBoundingBoxes;
    private Map<String, BoundingBox> reactionGlyphBoundingBoxes;

    /**
     * Read the Layout information from the SBML if available
     */
    public NetworkLayout(SBMLDocument document, Layout layout) {
        this.document = document;
        this.layout = layout;
        initNetworkLayout();
    }

    private void initNetworkLayout() {

        // Create Layout information for all species and reactions
        speciesBoundingBoxes = getSpeciesBoundingBoxesFromLayout(layout);
        reactionBoundingBoxes = getReactionBoundingBoxesFromLayout(layout);
        setMissingSpeciesBoundingBoxes();
        setMissingReactionBoundingBoxes();
        setMissingQSpeciesBoundingBoxes();
        setMissingQTransitionBoundingBoxes();

        // Create Layout information for all speciesGlyphs and reactionGlyphs
        speciesGlyphBoundingBoxes = getSpeciesGlyphBoundingBoxesFromLayout(layout);
        reactionGlyphBoundingBoxes = getReactionGlyphBoundingBoxesFromLayout(layout);

        // Maximum ranges
        setXYRange(speciesBoundingBoxes.values());
        setXYRange(reactionBoundingBoxes.values());
        setXYRange(speciesGlyphBoundingBoxes.values());
        setXYRange(reactionGlyphBoundingBoxes.values());
    }

    /**
     * Calculate the maximum and minium X and Y values for the given layout.
     * Is needed to set the unset nodes in the layout.
     */
    private void setXYRange(Collection<BoundingBox> boxes) {
        Point point;
        double x;
        double y;
        boolean setX = false;
        boolean setY = false;
        if (min_x != 0.0 || max_x != 0.0) {
            setX = true;
        }
        if (min_y != 0.0 || max_y != 0.0) {
            setX = true;
        }

        for (BoundingBox box : boxes) {
            point = box.getPosition();
            x = point.getX();
            y = point.getY();
            if (x != 0.0) {
                if (!setX) {
                    min_x = x;
                    max_x = x;
                    setX = true;
                } else {
                    if (x < min_x) {
                        min_x = x;
                    }
                    if (x > max_x) {
                        max_x = x;
                    }
                }
            }

            if (y != 0.0) {
                if (!setY) {
                    min_y = y;
                    max_y = y;
                    setY = true;
                } else {
                    if (y < min_y) {
                        min_y = y;
                    }
                    if (y > max_y) {
                        max_y = y;
                    }
                }
            }
        }
    }

    private static Map<String, BoundingBox> getSpeciesBoundingBoxesFromLayout(Layout layout) {
        Map<String, BoundingBox> boxesMap = new HashMap<String, BoundingBox>();
        if (layout.isSetListOfSpeciesGlyphs()) {
            for (SpeciesGlyph glyph : layout.getListOfSpeciesGlyphs()) {
                if (glyph.isSetSpecies() && glyph.isSetBoundingBox()) {
                    String id = glyph.getSpecies();
                    BoundingBox box = getBoundingBoxFromGlyph(glyph);
                    boxesMap.put(id, box);
                }
            }
        }
        return boxesMap;
    }

    private static Map<String, BoundingBox> getReactionBoundingBoxesFromLayout(Layout layout) {
        Map<String, BoundingBox> boxesMap = new HashMap<String, BoundingBox>();
        if (layout.isSetListOfReactionGlyphs()) {
            for (ReactionGlyph glyph : layout.getListOfReactionGlyphs()) {
                if (glyph.isSetReaction() && glyph.isSetBoundingBox()) {
                    String id = glyph.getReaction();
                    BoundingBox box = getBoundingBoxFromGlyph(glyph);
                    boxesMap.put(id, box);
                }
            }
        }
        return boxesMap;
    }

    private static Map<String, BoundingBox> getSpeciesGlyphBoundingBoxesFromLayout(Layout layout) {
        Map<String, BoundingBox> boxesMap = new HashMap<String, BoundingBox>();
        if (layout.isSetListOfSpeciesGlyphs()) {
            for (SpeciesGlyph glyph : layout.getListOfSpeciesGlyphs()) {
                String id = glyph.getId();
                BoundingBox box = getBoundingBoxFromGlyph(glyph);
                boxesMap.put(id, box);
            }
        }
        return boxesMap;
    }

    private static Map<String, BoundingBox> getReactionGlyphBoundingBoxesFromLayout(Layout layout) {
        Map<String, BoundingBox> boxesMap = new HashMap<String, BoundingBox>();
        if (layout.isSetListOfReactionGlyphs()) {
            for (ReactionGlyph glyph : layout.getListOfReactionGlyphs()) {
                String id = glyph.getId();
                BoundingBox box = getBoundingBoxFromGlyph(glyph);
                boxesMap.put(id, box);
            }
        }
        return boxesMap;
    }

    private static BoundingBox getBoundingBoxFromGlyph(GraphicalObject glyph) {
        BoundingBox box;
        if (glyph.isSetBoundingBox()) {
            box = glyph.getBoundingBox();
            setMissingBoundaryBoxInformation(box);
        } else {
            box = createGenericBoundingBox();
        }
        return box;
    }

    private static void setMissingBoundaryBoxInformation(BoundingBox box) {
        Dimensions dim;
        Point point;
        // Fill Dimension
        if (!box.isSetDimensions()) {
            dim = new Dimensions();
            dim.setHeight(GENERIC_HEIGHT);
            dim.setWidth(GENERIC_WIDTH);
            box.setDimensions(dim);
        } else {
            dim = box.getDimensions();
            if (!dim.isSetHeight()) {
                dim.setHeight(GENERIC_HEIGHT);
            }
            if (!dim.isSetWidth()) {
                dim.setWidth(GENERIC_WIDTH);
            }
        }
        // Fill Positions
        if (!box.isSetPosition()) {
            point = new Point();
            point.setX(GENERIC_X);
            point.setY(GENERIC_Y);
            box.setPosition(point);
        } else {
            point = box.getPosition();
            if (!point.isSetX()) {
                point.setX(GENERIC_X);
            }
            if (!point.isSetY()) {
                point.setY(GENERIC_Y);
            }
        }
    }

    private static BoundingBox createGenericBoundingBox() {
        Dimensions dim = new Dimensions();
        dim.setWidth(GENERIC_WIDTH);
        dim.setHeight(GENERIC_HEIGHT);
        Point point = new Point();
        point.setX(GENERIC_X);
        point.setY(GENERIC_Y);
        BoundingBox box = new BoundingBox();
        box.setDimensions(dim);
        box.setPosition(point);
        return box;
    }

    /**
     * For all species in the network which have no bounding box, a
     * generic bounding box is generated.
     */
    private void setMissingSpeciesBoundingBoxes() {
        Map<String, BoundingBox> boxes = speciesBoundingBoxes;
        BoundingBox genericBox = createGenericBoundingBox();
        String key;
        for (Species species : document.getModel().getListOfSpecies()) {
            key = species.getId();
            if (!boxes.containsKey(key)) {
                boxes.put(key, genericBox);
            }
        }
    }

    private void setMissingReactionBoundingBoxes() {
        Map<String, BoundingBox> boxes = reactionBoundingBoxes;
        BoundingBox genericBox = createGenericBoundingBox();
        String key;
        for (Reaction reaction : document.getModel().getListOfReactions()) {
            key = reaction.getId();
            if (!boxes.containsKey(key)) {
                boxes.put(key, genericBox);
            }
        }
    }

    private void setMissingQSpeciesBoundingBoxes() {
        Map<String, BoundingBox> boxes = speciesBoundingBoxes;
        BoundingBox genericBox = createGenericBoundingBox();
        String key;
        Model model = document.getModel();
        QualModelPlugin qModel = (QualModelPlugin) model.getExtension(QualConstants.namespaceURI);
        if (qModel != null) {
            for (QualitativeSpecies qspecies : qModel.getListOfQualitativeSpecies()) {
                key = qspecies.getId();
                if (!boxes.containsKey(key)) {
                    boxes.put(key, genericBox);
                }
            }
        }
    }

    private void setMissingQTransitionBoundingBoxes() {
        Map<String, BoundingBox> boxes = reactionBoundingBoxes;
        BoundingBox genericBox = createGenericBoundingBox();
        String key;
        Model model = document.getModel();
        QualModelPlugin qModel = (QualModelPlugin) model.getExtension(QualConstants.namespaceURI);
        if (qModel != null) {
            for (Transition qtransition : qModel.getListOfTransitions()) {
                key = qtransition.getId();
                if (!boxes.containsKey(key)) {
                    boxes.put(key, genericBox);
                }
            }
        }
    }
}

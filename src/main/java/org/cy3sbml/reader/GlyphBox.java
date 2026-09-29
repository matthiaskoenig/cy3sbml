package org.cy3sbml.reader;

import java.util.Collection;
import org.sbml.jsbml.ext.layout.BoundingBox;
import org.sbml.jsbml.ext.layout.Dimensions;
import org.sbml.jsbml.ext.layout.GraphicalObject;
import org.sbml.jsbml.ext.layout.Point;

/**
 * Position and size of the node of a layout glyph.
 *
 * @param x      x of the centre of the bounding box
 * @param y      y of the centre of the bounding box
 * @param width  width of the bounding box
 * @param height height of the bounding box
 */
record GlyphBox(double x, double y, double width, double height) {
    /** Width and height of a glyph without (positive) dimensions, see {@link #of(GraphicalObject, double)}. */
    static final double DEFAULT_SIZE = 30.0;

    /** The box of the glyph with the default size {@link #DEFAULT_SIZE}, see {@link #of(GraphicalObject, double)}. */
    static GlyphBox of(GraphicalObject glyph) {
        return of(glyph, DEFAULT_SIZE);
    }

    /**
     * The box of the bounding box of the glyph. A missing position is the origin, a missing
     * or non-positive width or height is the default size. A glyph without width and height
     * is a point (e.g. the reaction glyphs of the KEGG layouts): its position is the centre.
     *
     * @param defaultSize width and height of a glyph without them
     */
    static GlyphBox of(GraphicalObject glyph, double defaultSize) {
        double left = 0;
        double top = 0;
        double width = Double.NaN;
        double height = Double.NaN;
        if (glyph.isSetBoundingBox()) {
            BoundingBox box = glyph.getBoundingBox();
            if (box.isSetPosition()) {
                Point position = box.getPosition();
                left = valueOr(position.getX(), 0);
                top = valueOr(position.getY(), 0);
            }
            if (box.isSetDimensions()) {
                Dimensions dimensions = box.getDimensions();
                width = size(dimensions.getWidth());
                height = size(dimensions.getHeight());
            }
        }
        if (Double.isNaN(width) && Double.isNaN(height)) {
            return new GlyphBox(left, top, defaultSize, defaultSize);
        }
        width = Double.isNaN(width) ? defaultSize : width;
        height = Double.isNaN(height) ? defaultSize : height;
        return new GlyphBox(left + width / 2, top + height / 2, width, height);
    }

    /** Distance between the centres of the boxes. */
    double distance(GlyphBox other) {
        return Math.hypot(x - other.x, y - other.y);
    }

    /** The box of the given size at the mean of the centres of the boxes. */
    static GlyphBox centroid(Collection<GlyphBox> boxes, double size) {
        double x = boxes.stream().mapToDouble(GlyphBox::x).average().orElse(0);
        double y = boxes.stream().mapToDouble(GlyphBox::y).average().orElse(0);
        return new GlyphBox(x, y, size, size);
    }

    // JSBML returns NaN for an unset coordinate
    private static double valueOr(double value, double fallback) {
        return Double.isNaN(value) ? fallback : value;
    }

    // NaN for an unset or non-positive size
    private static double size(double size) {
        return size > 0 ? size : Double.NaN;
    }
}

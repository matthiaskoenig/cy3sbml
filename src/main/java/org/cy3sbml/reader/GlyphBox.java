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
    /** Width and height of a glyph without (positive) dimensions. */
    static final double DEFAULT_SIZE = 30.0;

    /**
     * The box of the bounding box of the glyph. A missing position is the origin, a missing
     * or non-positive width or height is {@link #DEFAULT_SIZE}.
     */
    static GlyphBox of(GraphicalObject glyph) {
        double left = 0;
        double top = 0;
        double width = DEFAULT_SIZE;
        double height = DEFAULT_SIZE;
        if (glyph.isSetBoundingBox()) {
            BoundingBox box = glyph.getBoundingBox();
            if (box.isSetPosition()) {
                Point position = box.getPosition();
                left = valueOr(position.getX(), 0);
                top = valueOr(position.getY(), 0);
            }
            if (box.isSetDimensions()) {
                Dimensions dimensions = box.getDimensions();
                width = sizeOr(dimensions.getWidth());
                height = sizeOr(dimensions.getHeight());
            }
        }
        return new GlyphBox(left + width / 2, top + height / 2, width, height);
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

    private static double sizeOr(double size) {
        return Double.isNaN(size) || size <= 0 ? DEFAULT_SIZE : size;
    }
}

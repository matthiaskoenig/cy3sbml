package org.cy3sbml.reader;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.sbml.jsbml.ext.layout.BoundingBox;
import org.sbml.jsbml.ext.layout.Dimensions;
import org.sbml.jsbml.ext.layout.Point;
import org.sbml.jsbml.ext.layout.SpeciesGlyph;

class GlyphBoxTest {

    @Test
    void centreAndSizeOfTheBoundingBox() {
        SpeciesGlyph glyph = glyph();
        BoundingBox box = glyph.createBoundingBox(40, 30, 0);
        box.setPosition(new Point(10, 20, 0, 3, 1));

        assertEquals(new GlyphBox(30, 35, 40, 30), GlyphBox.of(glyph));
    }

    @Test
    void missingBoundingBoxIsAPointAtTheOrigin() {
        assertEquals(new GlyphBox(0, 0, 30, 30), GlyphBox.of(glyph()));
    }

    /** A glyph without dimensions is a point: its position is the centre (KEGG reaction glyphs). */
    @Test
    void positionWithoutDimensionsIsTheCentre() {
        SpeciesGlyph glyph = glyph();
        BoundingBox box = glyph.createBoundingBox();
        box.setPosition(new Point(10, 20, 0, 3, 1));

        assertEquals(new GlyphBox(10, 20, 30, 30), GlyphBox.of(glyph));
        assertEquals(new GlyphBox(10, 20, 12, 12), GlyphBox.of(glyph, 12));
    }

    @Test
    void missingPositionGivesTheOrigin() {
        SpeciesGlyph glyph = glyph();
        glyph.createBoundingBox(40, 30, 0);

        assertEquals(new GlyphBox(20, 15, 40, 30), GlyphBox.of(glyph));
    }

    @Test
    void unsetValuesAreDefaults() {
        SpeciesGlyph glyph = glyph();
        BoundingBox box = glyph.createBoundingBox();
        box.setPosition(new Point(3, 1));
        box.setDimensions(new Dimensions(3, 1));

        assertEquals(new GlyphBox(0, 0, 30, 30), GlyphBox.of(glyph));
    }

    @Test
    void zeroSizeIsAPoint() {
        SpeciesGlyph glyph = glyph();
        BoundingBox box = glyph.createBoundingBox(0, 0, 0);
        box.setPosition(new Point(10, 20, 0, 3, 1));

        assertEquals(new GlyphBox(10, 20, 30, 30), GlyphBox.of(glyph));
    }

    @Test
    void missingWidthIsTheDefaultWidth() {
        SpeciesGlyph glyph = glyph();
        BoundingBox box = glyph.createBoundingBox(0, 20, 0);
        box.setPosition(new Point(10, 20, 0, 3, 1));

        assertEquals(new GlyphBox(25, 30, 30, 20), GlyphBox.of(glyph));
    }

    @Test
    void centroidOfTheCentres() {
        GlyphBox centroid = GlyphBox.centroid(List.of(new GlyphBox(10, 0, 5, 5), new GlyphBox(30, 20, 5, 5)), 20);

        assertEquals(new GlyphBox(20, 10, 20, 20), centroid);
    }

    private static SpeciesGlyph glyph() {
        return new SpeciesGlyph("g", 3, 1);
    }
}

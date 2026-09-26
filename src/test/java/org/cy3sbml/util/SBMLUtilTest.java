package org.cy3sbml.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.cy3sbml.SBML;
import org.junit.jupiter.api.Test;
import org.sbml.jsbml.ext.qual.QualitativeSpecies;

class SBMLUtilTest {

    @Test
    void qualitativeSpeciesMapShowsInitialAndMaxLevel() {
        QualitativeSpecies species = new QualitativeSpecies("s1", 3, 1);
        species.setInitialLevel(1);
        species.setMaxLevel(2);

        Map<String, String> map = SBMLUtil.createQualitativeSpeciesMap(species);

        assertEquals("1/2", map.get(SBML.ATTR_QUAL_INITIAL_LEVEL + "/" + SBML.ATTR_QUAL_MAX_LEVEL));
    }
}

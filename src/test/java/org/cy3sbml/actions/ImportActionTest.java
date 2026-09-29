package org.cy3sbml.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.List;
import org.cytoscape.util.swing.FileChooserFilter;
import org.junit.jupiter.api.Test;

class ImportActionTest {

    /** The file dialog of Import SBML offers SBML files and COMBINE archives in one filter. */
    @Test
    void fileDialogListsSbmlFilesAndCombineArchives() {
        Collection<FileChooserFilter> filters = ImportAction.fileFilters();

        assertEquals(1, filters.size());
        List<String> extensions = List.of(filters.iterator().next().getExtensions());
        for (String extension : List.of("", "xml", "sbml", "omex", "sedx")) {
            assertTrue(extensions.contains(extension), extension);
        }
    }
}

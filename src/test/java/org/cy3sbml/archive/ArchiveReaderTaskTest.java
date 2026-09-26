package org.cy3sbml.archive;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ArchiveReaderTaskTest {

    @ParameterizedTest
    @CsvSource({
        "/studies/s1/, study",
        "/models/m1/, model",
        "/assays/a1/, assay",
        "studies/s1/, study",
        "./models/m1/, model",
        "/studies/, folder",
        "/studies/s1/data/, folder",
        "/other/x/, folder",
        "/other/, folder",
    })
    void folderImageFollowsTheParentFolderType(String path, String extension) {
        assertEquals(extension, ArchiveReaderTask.folderExtension(path));
    }
}

package org.cy3sbml.styles;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

public class StyleFactoryTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void createStyle01() throws Exception {
        File tempFile = folder.newFile("test.xml");
        StyleInfo_cy3sbml info = new StyleInfo_cy3sbml();
        StyleFactory.createStyle(info, tempFile);
    }

    @Test
    public void createStyle02() throws Exception {
        File tempFile = folder.newFile("test.xml");
        StyleInfo_cy3sbmlDark info = new StyleInfo_cy3sbmlDark();
        StyleFactory.createStyle(info, tempFile);
    }

}
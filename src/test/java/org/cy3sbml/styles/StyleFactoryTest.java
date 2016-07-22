package org.cy3sbml.styles;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.junit.Assert.*;

public class StyleFactoryTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void createStyle01() throws Exception {
        File tempFile = folder.newFile("test.xml");
        StyleInfo01 info = new StyleInfo01();
        StyleFactory.createStyle(info, tempFile);
    }

    @Test
    public void createStyle02() throws Exception {
        File tempFile = folder.newFile("test.xml");
        StyleInfo02 info = new StyleInfo02();
        StyleFactory.createStyle(info, tempFile);
    }

}
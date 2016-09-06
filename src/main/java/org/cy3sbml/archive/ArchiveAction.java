package org.cy3sbml.archive;

import org.cy3sbml.gui.GUIConstants;
import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.task.read.LoadNetworkFileTaskFactory;
import org.cytoscape.util.swing.FileChooserFilter;
import org.cytoscape.util.swing.FileUtil;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Collection;
import java.util.HashSet;

/**
 * Importing SBML networks.
 */
public class ArchiveAction extends AbstractCyAction {
    private static final Logger logger = LoggerFactory.getLogger(ArchiveAction.class);
    private static final long serialVersionUID = 1L;

    private CySwingApplication cySwingApplication;
    private FileUtil fileUtil;
    private LoadNetworkFileTaskFactory loadNetworkFileTaskFactory;
    private TaskManager taskManager;

    /**
     * Constructor.
     *
     * @param cySwingApplication
     * @param fileUtil
     * @param loadNetworkFileTaskFactory
     * @param taskManager
     */
    public ArchiveAction(CySwingApplication cySwingApplication,
                         FileUtil fileUtil,
                         LoadNetworkFileTaskFactory loadNetworkFileTaskFactory,
                         TaskManager taskManager) {
        super(ArchiveAction.class.getSimpleName());
        this.cySwingApplication = cySwingApplication;
        this.fileUtil = fileUtil;
        this.loadNetworkFileTaskFactory = loadNetworkFileTaskFactory;
        this.taskManager = taskManager;


        ImageIcon icon = new ImageIcon(getClass().getResource(GUIConstants.ICON_ARCHIVE));
        putValue(LARGE_ICON_KEY, icon);

        this.putValue(SHORT_DESCRIPTION, GUIConstants.DESCRIPTION_ARCHIVE);
        setToolbarGravity(GUIConstants.GRAVITY_ARCHIVE);
    }

    public boolean isInToolBar() {
        return true;
    }

    /**
     * Load archive files.
     * TODO: run this through the task manager analoque to the file import
     *
     * @param e
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        logger.debug("actionPerformed()");

        // open new file open dialog
        Collection<FileChooserFilter> filters = new HashSet<>();
        String[] extensions = {"", "zip", "omex", "sedx", "sbex", "cmex", "sbox", "neux", "phex", "ro"};
        filters.add(new FileChooserFilter("Archive files, Research Bundles, COMBINE Archives (*, *.zip, *.omex, *.ro)", extensions));


        File[] files = fileUtil.getFiles(cySwingApplication.getJFrame(),
                GUIConstants.DESCRIPTION_ARCHIVE, FileDialog.LOAD, filters);

        if ((files != null) && (files.length != 0)) {
            for (int i = 0; i < files.length; i++) {
                logger.info("Load: " + files[i].getName());
                TaskIterator iterator = loadNetworkFileTaskFactory.createTaskIterator(files[i]);
                taskManager.execute(iterator);
            }
        }
    }


}

package org.cy3sbml.actions;

import java.awt.FileDialog;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import javax.swing.ImageIcon;

import org.cy3sbml.gui.GUIConstants;
import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.util.swing.FileChooserFilter;
import org.cytoscape.work.TaskIterator;
import org.cy3sbml.ServiceAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Importing SBML networks..
 */
public class ImportAction extends AbstractCyAction {
    private static final Logger logger = LoggerFactory.getLogger(ImportAction.class);
    private static final long serialVersionUID = 1L;
    private ServiceAdapter adapter;

    public ImportAction(ServiceAdapter adapter) {
        super(ImportAction.class.getSimpleName());
        this.adapter = adapter;
        ImageIcon icon = new ImageIcon(getClass().getResource(GUIConstants.ICON_IMPORT));
        putValue(LARGE_ICON_KEY, icon);

        this.putValue(SHORT_DESCRIPTION, GUIConstants.DESCRIPTION_IMPORT);
        this.putValue(LONG_DESCRIPTION, "Use the File Import dialog to load SBML files. Select multiple files to import them at once, or drag and drop files onto the network pane.");
        setToolbarGravity(GUIConstants.GRAVITY_IMPORT);

        this.inToolBar = true;
        this.inMenuBar = false;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        logger.debug("actionPerformed()");

        // open new file open dialog
        Collection<FileChooserFilter> filters = new HashSet<>();
        String[] extensions = {"", "xml", "sbml"};
        filters.add(new FileChooserFilter("SBML files (*, *.xml, *.sbml)", extensions));

        File[] files = adapter.fileUtil.getFiles(adapter.cySwingApplication.getJFrame(),
                GUIConstants.DESCRIPTION_IMPORT, FileDialog.LOAD, filters);

        if ((files != null) && (files.length != 0)) {
            for (int i = 0; i < files.length; i++) {
                logger.info("Load: " + files[i].getName());
                TaskIterator iterator = adapter.loadNetworkFileTaskFactory.createTaskIterator(files[i]);
                adapter.synchronousTaskManager.execute(iterator);
            }
        }
    }


}

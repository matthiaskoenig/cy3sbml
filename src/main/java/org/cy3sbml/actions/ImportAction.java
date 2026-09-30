package org.cy3sbml.actions;

import java.awt.FileDialog;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.swing.ImageIcon;
import org.cy3sbml.ServiceAdapter;
import org.cy3sbml.archive.CombineArchiveFileFilter;
import org.cy3sbml.gui.GUIConstants;
import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.util.swing.FileChooserFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Imports SBML files and COMBINE archives chosen in a file dialog.
 */
public final class ImportAction extends AbstractCyAction {
    private static final Logger logger = LoggerFactory.getLogger(ImportAction.class);
    private static final long serialVersionUID = 1L;
    private final ServiceAdapter adapter;

    /** Creates the toolbar action. */
    public ImportAction(ServiceAdapter adapter) {
        super(ImportAction.class.getSimpleName());
        this.adapter = adapter;
        ImageIcon icon = new ImageIcon(getClass().getResource(GUIConstants.ICON_IMPORT));
        putValue(LARGE_ICON_KEY, icon);

        this.putValue(SHORT_DESCRIPTION, GUIConstants.DESCRIPTION_IMPORT);
        setToolbarGravity(GUIConstants.GRAVITY_IMPORT);

        this.inToolBar = true;
        this.inMenuBar = false;
    }

    /** The files of the file dialog: SBML files and COMBINE archives. */
    static Collection<FileChooserFilter> fileFilters() {
        List<String> extensions = new ArrayList<>(List.of("", "xml", "sbml"));
        extensions.addAll(CombineArchiveFileFilter.EXTENSIONS);
        return List.of(new FileChooserFilter(
                "SBML files and COMBINE archives (*, *.xml, *.sbml, *.omex, ...)", extensions.toArray(new String[0])));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        logger.debug("actionPerformed()");

        File[] files = adapter.fileUtil.getFiles(
                adapter.cySwingApplication.getJFrame(),
                GUIConstants.DESCRIPTION_IMPORT,
                FileDialog.LOAD,
                fileFilters());

        if (files == null) {
            return;
        }
        for (File file : files) {
            logger.info("Load: {}", file.getName());
            adapter.dialogTaskManager.execute(adapter.loadNetworkFileTaskFactory.createTaskIterator(file));
        }
    }
}

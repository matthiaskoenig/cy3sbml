package org.cy3sbml.styles;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import org.cytoscape.session.events.SessionLoadedEvent;
import org.cytoscape.session.events.SessionLoadedListener;
import org.cytoscape.task.read.LoadVizmapFileTaskFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads the visual styles of cy3sbml and their layout styles, at start and after a
 * session is loaded.
 */
public class StyleManager implements SessionLoadedListener {
    private static final Logger logger = LoggerFactory.getLogger(StyleManager.class);

    private final LoadVizmapFileTaskFactory loadVizmapFileTaskFactory;
    private final VisualMappingManager vmm;
    private final String[] styles;
    private final LayoutStyleFactory layoutStyleFactory;

    /**
     * @param styles             names of the styles, loaded from the resources
     * @param layoutStyleFactory creates the layout style of every style
     */
    public StyleManager(
            LoadVizmapFileTaskFactory loadVizmapFileTaskFactory,
            VisualMappingManager vmm,
            String[] styles,
            LayoutStyleFactory layoutStyleFactory) {
        logger.debug("StyleManager created");
        this.loadVizmapFileTaskFactory = loadVizmapFileTaskFactory;
        this.vmm = vmm;
        this.styles = styles;
        this.layoutStyleFactory = layoutStyleFactory;
    }

    /**
     * Load the visual styles of the app and add the layout style of every style (#71), if
     * they do not exist yet (e.g. from a session).
     */
    public void loadStyles() {
        for (String styleName : styles) {
            if (!styleName.equals(getVisualStyleByName(vmm, styleName).getTitle())) {
                loadStyle(styleName);
            }
            addLayoutStyle(styleName);
        }
    }

    /** Loads the style from its resource {@code /styles/<name>.xml}. */
    private void loadStyle(String styleName) {
        logger.info("Load visual style: {}", styleName);
        String resource = String.format("/styles/%s.xml", styleName);
        try (InputStream styleStream = getClass().getResourceAsStream(resource)) {
            if (styleStream == null) {
                logger.error("Visual style resource not found: {}", resource);
                return;
            }
            loadVizmapFileTaskFactory.loadStyles(styleStream);
        } catch (IOException e) {
            logger.error("Visual style could not be loaded: {}", resource, e);
        }
    }

    /** Adds the layout style of the style, if the style exists and its layout style not. */
    private void addLayoutStyle(String styleName) {
        String layoutStyleName = LayoutStyleFactory.layoutStyleName(styleName);
        VisualStyle base = getVisualStyleByName(vmm, styleName);
        if (!styleName.equals(base.getTitle())
                || layoutStyleName.equals(
                        getVisualStyleByName(vmm, layoutStyleName).getTitle())) {
            return;
        }
        logger.info("Add visual style: {}", layoutStyleName);
        vmm.addVisualStyle(layoutStyleFactory.create(base));
    }

    /**
     * Get the visual style by name.
     * If no style for given styleName exists, the default style is returned.
     * <p>
     * This is a fix until the function is implemented on the vmm
     * https://code.cytoscape.org/redmine/issues/2174
     */
    public static VisualStyle getVisualStyleByName(VisualMappingManager vmm, String styleName) {
        Set<VisualStyle> styles = vmm.getAllVisualStyles();
        for (VisualStyle style : styles) {
            if (style.getTitle().equals(styleName)) {
                return style;
            }
        }
        logger.debug("style [{}] not in VisualStyles, default style used.", styleName);
        return vmm.getDefaultVisualStyle();
    }

    /** Loads the styles missing in the loaded session. */
    @Override
    public void handleEvent(SessionLoadedEvent e) {
        logger.debug("SessionLoadedEvent");
        loadStyles();
    }
}

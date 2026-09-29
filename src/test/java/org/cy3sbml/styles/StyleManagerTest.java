package org.cy3sbml.styles;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;
import org.cy3sbml.SBML;
import org.cytoscape.task.read.LoadVizmapFileTaskFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Loading the cy3sbml styles adds the layout style of every style (#71). */
class StyleManagerTest {

    private final Set<VisualStyle> styles = new HashSet<>();
    private VisualMappingManager vmm;
    private LayoutStyleFactory layoutStyleFactory;
    private VisualStyle base;
    private VisualStyle layoutStyle;

    @BeforeEach
    void setUp() {
        vmm = mock(VisualMappingManager.class);
        when(vmm.getAllVisualStyles()).thenReturn(styles);
        VisualStyle defaultStyle = style("default");
        when(vmm.getDefaultVisualStyle()).thenReturn(defaultStyle);
        base = style(SBML.STYLE_CY3SBML);
        styles.add(base);
        layoutStyle = style(SBML.STYLE_CY3SBML + SBML.STYLE_SUFFIX_LAYOUT);
        layoutStyleFactory = mock(LayoutStyleFactory.class);
        when(layoutStyleFactory.create(base)).thenReturn(layoutStyle);
    }

    @Test
    void layoutStyleIsAdded() {
        styleManager().loadStyles();

        verify(vmm).addVisualStyle(layoutStyle);
    }

    @Test
    void existingLayoutStyleIsKept() {
        // e.g. from a session
        styles.add(style(SBML.STYLE_CY3SBML + SBML.STYLE_SUFFIX_LAYOUT));

        styleManager().loadStyles();

        verify(layoutStyleFactory, never()).create(any());
        verify(vmm, never()).addVisualStyle(any());
    }

    private StyleManager styleManager() {
        return new StyleManager(
                mock(LoadVizmapFileTaskFactory.class), vmm, new String[] {SBML.STYLE_CY3SBML}, layoutStyleFactory);
    }

    private static VisualStyle style(String title) {
        VisualStyle style = mock(VisualStyle.class);
        when(style.getTitle()).thenReturn(title);
        return style;
    }
}

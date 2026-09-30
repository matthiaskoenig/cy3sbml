package org.cy3sbml.styles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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

    /** A missing style is loaded from its resource, which is closed afterwards. */
    @Test
    void missingStyleIsLoadedAndItsResourceClosed() {
        styles.remove(base);
        LoadVizmapFileTaskFactory loader = mock(LoadVizmapFileTaskFactory.class);
        List<InputStream> streams = new ArrayList<>();
        when(loader.loadStyles(any(InputStream.class))).thenAnswer(invocation -> {
            streams.add(invocation.getArgument(0));
            return Set.of();
        });

        new StyleManager(loader, vmm, new String[] {SBML.STYLE_CY3SBML}, layoutStyleFactory).loadStyles();

        assertEquals(1, streams.size());
        assertThrows(IOException.class, () -> streams.get(0).read());
    }

    /** An existing style, e.g. from a session, is not loaded again. */
    @Test
    void existingStyleIsNotLoaded() {
        LoadVizmapFileTaskFactory loader = mock(LoadVizmapFileTaskFactory.class);

        new StyleManager(loader, vmm, new String[] {SBML.STYLE_CY3SBML}, layoutStyleFactory).loadStyles();

        verify(loader, never()).loadStyles(any(InputStream.class));
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

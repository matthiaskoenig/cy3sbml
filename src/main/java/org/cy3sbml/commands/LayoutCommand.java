package org.cy3sbml.commands;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;

/**
 * {@code cy3sbml layout save} and {@code cy3sbml layout load}: save the node positions of a
 * network view in a layout file and move the nodes to the positions of a layout file, like
 * the Save Layout and Load Layout buttons.
 */
final class LayoutCommand extends AbstractTaskFactory {
    static final String NAME_SAVE = "layout save";
    static final String NAME_LOAD = "layout load";

    private final CommandServices services;
    private final boolean save;

    private LayoutCommand(CommandServices services, boolean save) {
        this.services = services;
        this.save = save;
    }

    static LayoutCommand save(CommandServices services) {
        return new LayoutCommand(services, true);
    }

    static LayoutCommand load(CommandServices services) {
        return new LayoutCommand(services, false);
    }

    @Override
    public TaskIterator createTaskIterator() {
        return new TaskIterator(new LayoutTask(services, save));
    }

    /** Saves the node positions of the view of the network in the file, or loads them from it. */
    public static final class LayoutTask extends JsonTask {
        @Tunable(
                description = "Network",
                longDescription = CommandArguments.NETWORK,
                exampleStringValue = CommandArguments.NETWORK_EXAMPLE,
                context = "nogui")
        public CyNetwork network;

        @Tunable(
                description = "Layout file",
                longDescription = "Path of the layout file (XML).",
                exampleStringValue = "/home/user/layout.xml",
                context = "nogui")
        public String file;

        private final CommandServices services;
        private final boolean save;

        LayoutTask(CommandServices services, boolean save) {
            this.services = services;
            this.save = save;
        }

        @Override
        public void run(TaskMonitor taskMonitor) throws IOException {
            if (file == null || file.isBlank()) {
                throw new IllegalArgumentException("Give the layout file with the argument file.");
            }
            CyNetwork target = CommandNetworks.network(network, services);
            CommandNetworks.document(target, services.sbmlManager());
            CyNetworkView view = CommandNetworks.view(target, services.networkViewManager());
            File layoutFile = new File(file.strip());
            int nodes;
            if (save) {
                nodes = services.layoutTools().saveLayoutOfViewInFile(view, layoutFile);
            } else {
                if (!layoutFile.isFile()) {
                    throw new IllegalArgumentException("The layout file '" + file + "' does not exist.");
                }
                nodes = services.layoutTools().loadLayoutForViewFromFile(view, layoutFile);
            }
            Map<String, Object> json = new LinkedHashMap<>();
            json.put("file", layoutFile.getAbsolutePath());
            json.put("nodes", nodes);
            setResult(json);
        }
    }
}

package org.cy3sbml.commands;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.ext.SBasePlugin;

/**
 * {@code cy3sbml networks}: the SBML models open in Cytoscape with their networks, SBML
 * level and version and packages.
 */
final class NetworksCommand extends AbstractTaskFactory {
    static final String NAME = "networks";

    private final CommandServices services;

    NetworksCommand(CommandServices services) {
        this.services = services;
    }

    @Override
    public TaskIterator createTaskIterator() {
        return new TaskIterator(new NetworksTask(services));
    }

    /** Returns the SBML models with their networks, SBML level and version and packages. */
    public static final class NetworksTask extends JsonTask {
        private final CommandServices services;

        NetworksTask(CommandServices services) {
            this.services = services;
        }

        @Override
        public void run(TaskMonitor taskMonitor) {
            // the root networks with an SBML document, in the order of their SUIDs
            TreeSet<CyRootNetwork> roots = new TreeSet<>(Comparator.comparing(CyRootNetwork::getSUID));
            for (CyNetwork network : services.networkManager().getNetworkSet()) {
                if (services.sbmlManager().getSBMLDocument(network) != null) {
                    CommandNetworks.root(network).ifPresent(roots::add);
                }
            }
            List<Map<String, Object>> models = new ArrayList<>();
            for (CyRootNetwork root : roots) {
                List<CyNetwork> networks = new ArrayList<>(root.getSubNetworkList());
                networks.removeIf(network -> !services.networkManager().networkExists(network.getSUID()));
                networks.sort(Comparator.comparing(CyNetwork::getSUID));
                Map<String, Object> model = CommandNetworks.modelJson(root, networks, services.sbmlManager());
                SBMLDocument document = services.sbmlManager().getSBMLDocument(root.getSUID());
                model.put("level", document.getLevel());
                model.put("version", document.getVersion());
                model.put("packages", packages(document, services.sbmlManager().getModel(root.getSUID())));
                models.add(model);
            }
            setResult(Map.of("models", models));
        }

        /**
         * The packages of the document and the model of the collection, with their
         * versions, e.g. {@code {"fbc": 2}}.
         */
        private static Map<String, Integer> packages(SBMLDocument document, Model model) {
            Map<String, Integer> packages = new TreeMap<>();
            List<SBasePlugin> plugins =
                    new ArrayList<>(document.getExtensionPackages().values());
            if (model != null) {
                plugins.addAll(model.getExtensionPackages().values());
            }
            for (SBasePlugin plugin : plugins) {
                packages.put(plugin.getPackageName(), plugin.getPackageVersion());
            }
            return packages;
        }
    }
}

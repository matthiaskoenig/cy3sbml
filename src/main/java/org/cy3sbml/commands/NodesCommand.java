package org.cy3sbml.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.cy3sbml.SBML;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;

/**
 * {@code cy3sbml nodes}: the nodes of SBML ids in a network, to map data with SBML ids onto
 * the nodes.
 */
final class NodesCommand extends AbstractTaskFactory {
    static final String NAME = "nodes";

    private final CommandServices services;

    NodesCommand(CommandServices services) {
        this.services = services;
    }

    @Override
    public TaskIterator createTaskIterator() {
        return new TaskIterator(new NodesTask(services));
    }

    public static final class NodesTask extends JsonTask {
        @Tunable(
                description = "Network",
                longDescription = CommandArguments.NETWORK,
                exampleStringValue = CommandArguments.NETWORK_EXAMPLE,
                context = "nogui")
        public CyNetwork network;

        @Tunable(
                description = "SBML ids",
                longDescription = "Comma separated SBML ids. Without ids, the nodes of all SBML ids of the network.",
                exampleStringValue = "glc,g6p,HK",
                context = "nogui")
        public String sbmlIds;

        private final CommandServices services;

        NodesTask(CommandServices services) {
            this.services = services;
        }

        @Override
        public void run(TaskMonitor taskMonitor) {
            CyNetwork target = CommandNetworks.network(network, services);
            CommandNetworks.document(target, services.sbmlManager());
            Set<String> ids = new LinkedHashSet<>();
            if (sbmlIds != null && !sbmlIds.isBlank()) {
                Arrays.stream(sbmlIds.split(","))
                        .map(String::strip)
                        .filter(id -> !id.isEmpty())
                        .forEach(ids::add);
            }
            // all ids of the network in their order, or the given ids in the given order
            Map<String, List<Long>> nodes = ids.isEmpty() ? new TreeMap<>() : new LinkedHashMap<>();
            ids.forEach(id -> nodes.put(id, new ArrayList<>()));
            for (CyNode node : target.getNodeList()) {
                String id = target.getRow(node).get(SBML.ATTR_ID, String.class);
                if (id == null || (!ids.isEmpty() && !ids.contains(id))) {
                    continue;
                }
                nodes.computeIfAbsent(id, i -> new ArrayList<>()).add(node.getSUID());
            }
            nodes.values().forEach(suids -> suids.sort(null));
            setResult(Map.of("nodes", nodes));
        }
    }
}

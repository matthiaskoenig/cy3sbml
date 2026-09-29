package org.cy3sbml.commands;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.cy3sbml.cofactors.CofactorViews;
import org.cytoscape.command.util.NodeList;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;

/**
 * {@code cy3sbml cofactors split} and {@code cy3sbml cofactors merge}: split cofactor nodes
 * into one node per edge and merge them back, like the toolbar buttons.
 */
final class CofactorsCommand extends AbstractTaskFactory {
    static final String NAME_SPLIT = "cofactors split";
    static final String NAME_MERGE = "cofactors merge";

    private final CommandServices services;
    private final boolean split;

    private CofactorsCommand(CommandServices services, boolean split) {
        this.services = services;
        this.split = split;
    }

    static CofactorsCommand split(CommandServices services) {
        return new CofactorsCommand(services, true);
    }

    static CofactorsCommand merge(CommandServices services) {
        return new CofactorsCommand(services, false);
    }

    @Override
    public TaskIterator createTaskIterator() {
        return new TaskIterator(split ? new SplitTask(services) : new MergeTask(services));
    }

    /** The network and nodes arguments and the update of the view. */
    public abstract static class CofactorsTask extends JsonTask {
        @Tunable(
                description = "Network",
                longDescription = CommandArguments.NETWORK,
                exampleStringValue = CommandArguments.NETWORK_EXAMPLE,
                context = "nogui")
        public CyNetwork network;

        public NodeList nodeList = new NodeList(null);

        protected final CommandServices services;

        CofactorsTask(CommandServices services) {
            this.services = services;
        }

        @Tunable(
                description = "Nodes",
                longDescription = CommandArguments.NODE_LIST,
                exampleStringValue = CommandArguments.NODE_LIST_EXAMPLE,
                context = "nogui")
        public NodeList getnodeList() {
            nodeList.setNetwork(
                    network != null ? network : services.applicationManager().getCurrentNetwork());
            return nodeList;
        }

        public void setnodeList(NodeList value) {
            // set by the tunable handler through getnodeList
        }

        @Override
        public void run(TaskMonitor taskMonitor) {
            CyNetwork target = CommandNetworks.network(network, services);
            CommandNetworks.document(target, services.sbmlManager());
            Collection<CyNetworkView> views = services.networkViewManager().getNetworkViews(target);
            CyNetworkView view = views.isEmpty() ? null : views.iterator().next();
            List<CyNode> nodes = nodeList.getValue() != null ? nodeList.getValue() : List.of();
            List<CyNode> changed = apply(target, view, nodes);
            CofactorViews.update(services.visualMappingManager(), view);
            List<Long> suids = new ArrayList<>();
            for (CyNode node : changed) {
                suids.add(node.getSUID());
            }
            setResult(Map.of(resultKey(), suids));
        }

        abstract List<CyNode> apply(CyNetwork network, CyNetworkView view, List<CyNode> nodes);

        abstract String resultKey();
    }

    public static final class SplitTask extends CofactorsTask {
        SplitTask(CommandServices services) {
            super(services);
        }

        @Override
        List<CyNode> apply(CyNetwork network, CyNetworkView view, List<CyNode> nodes) {
            if (nodes.isEmpty()) {
                throw new IllegalArgumentException("Give the nodes to split with the argument nodeList.");
            }
            return services.cofactorManager().split(network, view, nodes);
        }

        @Override
        String resultKey() {
            return "clones";
        }
    }

    public static final class MergeTask extends CofactorsTask {
        MergeTask(CommandServices services) {
            super(services);
        }

        @Override
        List<CyNode> apply(CyNetwork network, CyNetworkView view, List<CyNode> nodes) {
            return nodes.isEmpty()
                    ? services.cofactorManager().mergeAll(network, view)
                    : services.cofactorManager().merge(network, view, nodes);
        }

        @Override
        String resultKey() {
            return "merged";
        }
    }
}

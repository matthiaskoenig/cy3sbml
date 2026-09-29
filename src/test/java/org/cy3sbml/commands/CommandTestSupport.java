package org.cy3sbml.commands;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.cy3sbml.SBMLManager;
import org.cy3sbml.biomodel.BiomodelLoader;
import org.cy3sbml.biomodel.BiomodelsQuery;
import org.cy3sbml.cofactors.CofactorManager;
import org.cy3sbml.layout.LayoutTools;
import org.cy3sbml.reader.SBMLReaderTask;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.ding.NetworkViewTestSupport;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.group.GroupTestSupport;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.NetworkTestSupport;
import org.cytoscape.task.read.LoadNetworkFileTaskFactory;
import org.cytoscape.task.read.LoadNetworkURLTaskFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.work.FinishStatus;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.TaskObserver;

/**
 * Imports test models as cy3sbml does (networks, views, SBMLManager) and provides the
 * services of the commands, with mocked Cytoscape managers that know the imported networks.
 */
final class CommandTestSupport {
    static final String CORE_MODEL = "/models/unittests/core_01.xml";
    private static final ObjectMapper JSON = new ObjectMapper();

    final CyApplicationManager applicationManager = mock(CyApplicationManager.class);
    final CyNetworkManager networkManager = mock(CyNetworkManager.class);
    final CyNetworkViewManager networkViewManager = mock(CyNetworkViewManager.class);
    final VisualMappingManager visualMappingManager = mock(VisualMappingManager.class);
    final LoadNetworkFileTaskFactory loadNetworkFile = mock(LoadNetworkFileTaskFactory.class);
    final LoadNetworkURLTaskFactory loadNetworkURL = mock(LoadNetworkURLTaskFactory.class);
    final BiomodelsQuery biomodelsQuery = mock(BiomodelsQuery.class);
    final BiomodelLoader biomodelLoader = mock(BiomodelLoader.class);
    final SBMLManager sbmlManager = new SBMLManager(applicationManager);
    final CofactorManager cofactorManager = new CofactorManager(sbmlManager, mock(CyEventHelper.class));

    private final NetworkTestSupport networkTestSupport = new NetworkTestSupport();
    private final Set<CyNetwork> networks = new LinkedHashSet<>();
    private final Map<CyNetwork, CyNetworkView> views = new HashMap<>();

    CommandTestSupport() {
        when(networkManager.getNetworkSet()).thenAnswer(invocation -> new LinkedHashSet<>(networks));
        when(networkManager.networkExists(anyLong()))
                .thenAnswer(invocation ->
                        networks.stream().anyMatch(n -> n.getSUID().equals(invocation.getArgument(0))));
        when(networkViewManager.getNetworkViews(any(CyNetwork.class))).thenAnswer(invocation -> {
            CyNetworkView view = views.get(invocation.<CyNetwork>getArgument(0));
            return view == null ? List.of() : List.of(view);
        });
    }

    CommandServices services() {
        return new CommandServices(
                applicationManager,
                networkManager,
                networkViewManager,
                visualMappingManager,
                loadNetworkFile,
                loadNetworkURL,
                new SynchronousTasks(),
                sbmlManager,
                cofactorManager,
                biomodelsQuery,
                biomodelLoader,
                new LayoutTools(null));
    }

    /**
     * Imports the model resource: its networks with views are registered in the network
     * managers and the SBMLManager, and the first network is the current network.
     */
    List<CyNetwork> importModel(String resource) throws Exception {
        URL url = CommandTestSupport.class.getResource(resource);
        assertNotNull(url, "Resource not found: " + resource);
        SBMLReaderTask task;
        try (InputStream stream = url.openStream()) {
            task = new SBMLReaderTask(
                    stream,
                    resource.substring(resource.lastIndexOf('/') + 1),
                    url.toURI(),
                    networkTestSupport.getNetworkFactory(),
                    new GroupTestSupport().getGroupFactory(),
                    new NetworkViewTestSupport().getNetworkViewFactory(),
                    null,
                    null,
                    null,
                    sbmlManager);
            task.run(mock(TaskMonitor.class));
        }
        List<CyNetwork> imported = new ArrayList<>(Arrays.asList(task.getNetworks()));
        for (CyNetwork network : imported) {
            views.put(network, task.buildCyNetworkView(network));
            networks.add(network);
        }
        when(applicationManager.getCurrentNetwork()).thenReturn(imported.get(0));
        return imported;
    }

    /** A network that was not imported by cy3sbml, registered in the network manager. */
    CyNetwork addOtherNetwork(String name) {
        CyNetwork network = networkTestSupport.getNetwork();
        network.getRow(network).set(CyNetwork.NAME, name);
        networks.add(network);
        return network;
    }

    /** The view of an imported network. */
    CyNetworkView view(CyNetwork network) {
        return views.get(network);
    }

    /** Runs the task and returns its JSON result. */
    static JsonNode run(JsonTask task) throws Exception {
        task.run(mock(TaskMonitor.class));
        return JSON.readTree(task.json());
    }

    static JsonNode parse(String json) throws Exception {
        return JSON.readTree(json);
    }

    /** Runs the tasks one after another, like the synchronous task manager of Cytoscape. */
    static final class SynchronousTasks implements SynchronousTaskManager<Object> {
        @Override
        public Object getConfiguration(TaskFactory factory, Object tunableContext) {
            return null;
        }

        @Override
        public void setExecutionContext(Map<String, Object> context) {}

        @Override
        public void execute(TaskIterator iterator) {
            execute(iterator, null);
        }

        @Override
        public void execute(TaskIterator iterator, TaskObserver observer) {
            FinishStatus status = FinishStatus.getSucceeded();
            while (iterator.hasNext()) {
                Task task = iterator.next();
                try {
                    task.run(mock(TaskMonitor.class));
                } catch (Exception e) {
                    status = FinishStatus.newFailed(task, e);
                    break;
                }
            }
            if (observer != null) {
                observer.allFinished(status);
            }
        }
    }
}

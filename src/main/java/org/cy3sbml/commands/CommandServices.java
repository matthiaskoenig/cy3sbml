package org.cy3sbml.commands;

import org.cy3sbml.SBMLManager;
import org.cy3sbml.biomodel.BiomodelLoader;
import org.cy3sbml.biomodel.BiomodelsQuery;
import org.cy3sbml.cofactors.CofactorManager;
import org.cy3sbml.layout.LayoutTools;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.task.read.LoadNetworkFileTaskFactory;
import org.cytoscape.task.read.LoadNetworkURLTaskFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.work.SynchronousTaskManager;

/**
 * The Cytoscape and cy3sbml services the automation commands use.
 *
 * @param loadNetworkFile the Cytoscape loader of network files, used by the import
 * @param loadNetworkURL the Cytoscape loader of network URLs, used by the import
 * @param synchronousTaskManager runs the loader tasks of the import
 */
public record CommandServices(
        CyApplicationManager applicationManager,
        CyNetworkManager networkManager,
        CyNetworkViewManager networkViewManager,
        VisualMappingManager visualMappingManager,
        LoadNetworkFileTaskFactory loadNetworkFile,
        LoadNetworkURLTaskFactory loadNetworkURL,
        SynchronousTaskManager<?> synchronousTaskManager,
        SBMLManager sbmlManager,
        CofactorManager cofactorManager,
        BiomodelsQuery biomodelsQuery,
        BiomodelLoader biomodelLoader,
        LayoutTools layoutTools) {}

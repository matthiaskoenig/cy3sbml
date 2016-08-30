package org.cy3sbml.validator;

import org.cy3sbml.SBMLCoreTest;
import org.cy3sbml.util.SBMLUtil;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.internal.sync.SyncTaskManager;
import org.cytoscape.work.internal.sync.SyncTunableMutator;
import org.junit.Before;
import org.junit.Test;
import org.sbml.jsbml.SBMLDocument;


public class ValidationTaskObserverTest {

    TaskManager taskManager;

    @Before
    public void setUp() {
        SyncTunableMutator stm = new SyncTunableMutator();
        taskManager = new SyncTaskManager(stm);
    }

    @Test
    public void runValidation() {
        SBMLDocument doc = SBMLUtil.readSBMLDocument(SBMLCoreTest.TEST_MODEL_CORE_01);
        // Validator action
        ValidationTaskObserver taskObserver = new ValidationTaskObserver(taskManager);
        taskObserver.runValidation(doc);
    }

}
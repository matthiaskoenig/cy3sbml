package org.cy3sbml;

import org.cy3sbml.reader.JsbmlSetup;
import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;

/**
 * Sets up JSBML once before the tests, which run in parallel (junit-platform.properties),
 * as {@code CyActivator} does before the SBML reader is registered.
 */
public class JsbmlSetupListener implements LauncherSessionListener {
    @Override
    public void launcherSessionOpened(LauncherSession session) {
        JsbmlSetup.initialize();
    }
}

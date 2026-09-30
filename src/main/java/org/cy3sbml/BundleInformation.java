package org.cy3sbml;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * The symbolic name and version of the cy3sbml bundle at runtime.
 */
public final class BundleInformation {
    private final String name;
    private final String version;

    /**
     * Reads the name and version of the bundle of the context.
     */
    public BundleInformation(BundleContext bc) {
        Bundle bundle = bc.getBundle();
        name = bundle.getSymbolicName();
        version = bundle.getVersion().toString();
    }

    /**
     * {name}-v{version} of bundle.
     */
    public String getInfo() {
        return getName() + "-v" + getVersion();
    }

    /**
     * Name of bundle.
     */
    public String getName() {
        return name;
    }

    /**
     * Version of bundle.
     */
    public String getVersion() {
        return version;
    }
}

package org.cy3sbml.reader;

import org.cy3sbml.SBMLReaderError;
import org.cytoscape.io.read.CyNetworkReader;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

/**
 * The reader of an input that could not be read before the reader was created, e.g. a
 * URL whose connection dropped: running it reports the error, as the reader of a broken
 * file does. Cytoscape's reader manager takes the first task of the task iterator of a
 * reader factory as the reader, so a factory cannot return no reader.
 */
public final class FailedReaderTask extends AbstractTask implements CyNetworkReader {
    private final String message;
    private final Throwable cause;

    public FailedReaderTask(String inputName, Throwable cause) {
        this.message = String.format("cy3sbml could not read '%s': %s", inputName, cause.getMessage());
        this.cause = cause;
    }

    @Override
    public void run(TaskMonitor taskMonitor) {
        throw new SBMLReaderError(message, cause);
    }

    @Override
    public CyNetwork[] getNetworks() {
        return new CyNetwork[0];
    }

    @Override
    public CyNetworkView buildCyNetworkView(CyNetwork network) {
        throw new IllegalStateException(message);
    }
}

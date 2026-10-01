package org.cy3sbml.sbml4humans;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.cy3sbml.SBMLManager;
import org.cy3sbml.archive.CombineArchiveWriter;
import org.cy3sbml.comp.CompModels;
import org.cy3sbml.util.NetworkUtil;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;

/**
 * Opens the model of a network in sbml4humans: writes the document of the collection of the
 * network with the files of its external model definitions into a COMBINE archive, uploads it
 * and opens the report in the system browser, at the model of the collection (the main model,
 * a comp model definition, an external model or the flat model).
 * <p>
 * A failure of the upload is the error of the task, which the task manager shows.
 */
public final class Sbml4HumansTask extends AbstractTask {
    private final SBMLManager sbmlManager;
    private final CyNetwork network;
    private final Supplier<Sbml4HumansClient> client;
    private final Consumer<String> browser;

    /**
     * @param client  the client of sbml4humans, created when the task runs, so an invalid
     *     address of the properties is the error of the task
     * @param browser opens the address of the report, e.g. in the system browser
     */
    public Sbml4HumansTask(
            SBMLManager sbmlManager, CyNetwork network, Supplier<Sbml4HumansClient> client, Consumer<String> browser) {
        this.sbmlManager = sbmlManager;
        this.network = network;
        this.client = client;
        this.browser = browser;
    }

    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        taskMonitor.setTitle("Opening the model in sbml4humans");
        if (cancelled) {
            return;
        }
        SBMLDocument document = sbmlManager.getSBMLDocument(network);
        if (document == null) {
            throw new IllegalStateException("The network has no SBML document.");
        }
        taskMonitor.setStatusMessage("Writing the COMBINE archive");
        CompModels models = sbmlManager.getSBaseRefResolver(document).models();
        String location = CombineArchiveWriter.masterLocation(document, sbmlManager.getArchive(document));
        ByteArrayOutputStream archive = new ByteArrayOutputStream();
        String entry = CombineArchiveWriter.write(document, location, models, archive);
        if (cancelled) {
            return;
        }
        Model model = sbmlManager.getModel(NetworkUtil.getRootNetworkSUID(network));
        Sbml4HumansClient client;
        try {
            client = this.client.get();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "The address of sbml4humans in the cy3sbml properties is invalid: " + e.getMessage(), e);
        }
        taskMonitor.setStatusMessage("Uploading the model to " + client.url());
        taskMonitor.setProgress(0.2);
        URI report =
                client.upload(archive.toByteArray(), entry, model != null && model.isSetId() ? model.getId() : null);
        if (cancelled) {
            return;
        }
        taskMonitor.setProgress(1.0);
        browser.accept(report.toString());
    }
}

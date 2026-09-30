package org.cy3sbml.commands;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLWriter;

/**
 * {@code cy3sbml document}: the SBML document of a network, as string or written to a file.
 */
final class DocumentCommand extends AbstractTaskFactory {
    static final String NAME = "document";

    private final CommandServices services;

    DocumentCommand(CommandServices services) {
        this.services = services;
    }

    @Override
    public TaskIterator createTaskIterator() {
        return new TaskIterator(new DocumentTask(services));
    }

    /** Returns the SBML of the document of the network, or writes it to the file. */
    public static final class DocumentTask extends JsonTask {
        @Tunable(
                description = "Network",
                longDescription = CommandArguments.NETWORK,
                exampleStringValue = CommandArguments.NETWORK_EXAMPLE,
                context = "nogui")
        public CyNetwork network;

        @Tunable(
                description = "File",
                longDescription = "Path of the file the SBML is written to. Without file, the result has the SBML.",
                exampleStringValue = "/home/user/model.xml",
                context = "nogui")
        public String file;

        private final CommandServices services;

        DocumentTask(CommandServices services) {
            this.services = services;
        }

        @Override
        public void run(TaskMonitor taskMonitor) throws IOException, XMLStreamException {
            CyNetwork target = CommandNetworks.network(network, services);
            SBMLDocument document = CommandNetworks.document(target, services.sbmlManager());
            if (file == null || file.isBlank()) {
                // rendering the info panel reads the document concurrently, writing only reads it
                setResult(Map.of("sbml", new SBMLWriter().writeSBMLToString(document)));
                return;
            }
            File output = new File(file.strip());
            try (OutputStream stream = Files.newOutputStream(output.toPath())) {
                new SBMLWriter().write(document, stream);
            }
            setResult(Map.of("file", output.getAbsolutePath()));
        }
    }
}

package org.cy3sbml.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ImportCommandTest {
    @TempDir
    Path tempDir;

    /** A Cytoscape loader task that imports the core test model. */
    private static TaskIterator loader(CommandTestSupport support) {
        return new TaskIterator(new AbstractTask() {
            @Override
            public void run(TaskMonitor taskMonitor) throws Exception {
                support.importModel(CommandTestSupport.CORE_MODEL);
            }
        });
    }

    /** Runs the import command with the given arguments. */
    private static JsonNode runImport(CommandTestSupport support, Consumer<ImportCommand.ImportTask> args)
            throws Exception {
        TaskIterator iterator = new ImportCommand(support.services()).createTaskIterator();
        ImportCommand.ImportTask task = (ImportCommand.ImportTask) iterator.next();
        assertFalse(iterator.hasNext());
        args.accept(task);
        return CommandTestSupport.run(task);
    }

    private static ImportCommand.ImportTask importTask(CommandTestSupport support) {
        return (ImportCommand.ImportTask)
                new ImportCommand(support.services()).createTaskIterator().next();
    }

    @Test
    void importsAFileAndReturnsTheModelWithItsNetworks() throws Exception {
        CommandTestSupport support = new CommandTestSupport();
        support.addOtherNetwork("existing");
        File file = Files.writeString(tempDir.resolve("model.xml"), "<sbml/>").toFile();
        when(support.loadNetworkFile.createTaskIterator(file)).thenReturn(loader(support));

        JsonNode json = runImport(support, task -> task.file = file.getPath());

        JsonNode models = json.get("models");
        assertEquals(1, models.size(), json.toPrettyString());
        JsonNode model = models.get(0);
        assertEquals("BIOMD0000000001", model.get("modelId").asText());
        // the base, kinetic and all network, not the existing network
        assertEquals(3, model.get("networks").size(), json.toPrettyString());
        assertEquals("base", model.get("networks").get(0).get("type").asText());
    }

    @Test
    void importsAnSbmlStringFromATemporaryFileWhichIsDeleted() throws Exception {
        CommandTestSupport support = new CommandTestSupport();
        AtomicReference<File> loaded = new AtomicReference<>();
        when(support.loadNetworkFile.createTaskIterator(any(File.class))).thenAnswer(invocation -> {
            File file = invocation.getArgument(0);
            assertEquals("<sbml>the model</sbml>", Files.readString(file.toPath()));
            loaded.set(file);
            return loader(support);
        });

        JsonNode json = runImport(support, task -> task.sbml = "<sbml>the model</sbml>");

        assertEquals("BIOMD0000000001", json.get("models").get(0).get("modelId").asText());
        assertFalse(loaded.get().exists());
        assertFalse(loaded.get().getParentFile().exists());
    }

    @Test
    void importsAUrlAndABiomodel() throws Exception {
        CommandTestSupport support = new CommandTestSupport();
        when(support.loadNetworkURL.createTaskIterator(any(URL.class), isNull()))
                .thenReturn(loader(support));
        JsonNode json = runImport(support, task -> task.url = "https://example.org/model.xml");
        assertEquals(1, json.get("models").size());

        CommandTestSupport support2 = new CommandTestSupport();
        when(support2.biomodelLoader.createTaskIterator(List.of("BIOMD0000000001")))
                .thenReturn(loader(support2));
        json = runImport(support2, task -> task.biomodelsId = " BIOMD0000000001 ");
        assertEquals(1, json.get("models").size());
    }

    @Test
    void failsIfNothingWasImported() throws Exception {
        CommandTestSupport support = new CommandTestSupport();
        File file = Files.writeString(tempDir.resolve("empty.xml"), "no sbml").toFile();
        when(support.loadNetworkFile.createTaskIterator(any(File.class))).thenReturn(new TaskIterator());

        IllegalStateException error =
                assertThrows(IllegalStateException.class, () -> runImport(support, task -> task.file = file.getPath()));

        assertTrue(error.getMessage().startsWith("No network was imported"), error.getMessage());
    }

    @Test
    void failsWithTheErrorOfTheLoader() throws Exception {
        CommandTestSupport support = new CommandTestSupport();
        when(support.biomodelLoader.createTaskIterator(List.of("BIOMD9")))
                .thenReturn(new TaskIterator(new AbstractTask() {
                    @Override
                    public void run(TaskMonitor taskMonitor) throws Exception {
                        throw new IOException("No SBML could be downloaded for the BioModels:\nBIOMD9: 404");
                    }
                }));

        IOException error =
                assertThrows(IOException.class, () -> runImport(support, task -> task.biomodelsId = "BIOMD9"));

        assertTrue(error.getMessage().startsWith("No SBML could be downloaded"), error.getMessage());
    }

    @Test
    void failsWithoutOrWithSeveralSources() {
        CommandTestSupport support = new CommandTestSupport();
        ImportCommand.ImportTask none = importTask(support);
        ImportCommand.ImportTask two = importTask(support);
        two.file = "model.xml";
        two.biomodelsId = "BIOMD0000000001";

        for (ImportCommand.ImportTask task : List.of(none, two)) {
            IllegalArgumentException error =
                    assertThrows(IllegalArgumentException.class, () -> task.run(mock(TaskMonitor.class)));
            assertEquals("Give exactly one of the arguments file, url, sbml and biomodelsId.", error.getMessage());
        }
    }

    @Test
    void failsForAMissingFileAndAnInvalidUrl() {
        CommandTestSupport support = new CommandTestSupport();
        ImportCommand.ImportTask missing = importTask(support);
        missing.file = tempDir.resolve("missing.xml").toString();
        ImportCommand.ImportTask invalid = importTask(support);
        invalid.url = "not a url";

        IllegalArgumentException error =
                assertThrows(IllegalArgumentException.class, () -> missing.run(mock(TaskMonitor.class)));
        assertTrue(error.getMessage().endsWith("does not exist."), error.getMessage());
        error = assertThrows(IllegalArgumentException.class, () -> invalid.run(mock(TaskMonitor.class)));
        assertTrue(error.getMessage().startsWith("Invalid URL 'not a url'"), error.getMessage());
    }

    @Test
    void failsForAUrlThatIsNoHttpUrl() {
        CommandTestSupport support = new CommandTestSupport();
        for (String url : List.of("file:///etc/passwd", "jar:file:/tmp/a.jar!/model.xml", "ftp://example.org/a.xml")) {
            ImportCommand.ImportTask task = importTask(support);
            task.url = url;

            IllegalArgumentException error =
                    assertThrows(IllegalArgumentException.class, () -> task.run(mock(TaskMonitor.class)));
            assertEquals(
                    "Invalid URL '" + url + "': only http and https URLs are imported, give a local file with"
                            + " the argument file.",
                    error.getMessage());
        }
        verifyNoInteractions(support.loadNetworkURL);
    }
}

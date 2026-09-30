package org.cy3sbml.commands;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.FinishStatus;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.TaskObserver;
import org.cytoscape.work.Tunable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@code cy3sbml import}: imports an SBML model (or the models of a COMBINE archive) from a
 * file, a URL, an SBML string or BioModels with the Cytoscape network loaders, so the networks,
 * views and styles are created as by an import in the GUI, and returns the imported models
 * with their networks. The SBML string is written to a temporary file, deleted after the
 * import.
 */
final class ImportCommand extends AbstractTaskFactory {
    static final String NAME = "import";

    private final CommandServices services;

    ImportCommand(CommandServices services) {
        this.services = services;
    }

    @Override
    public TaskIterator createTaskIterator() {
        return new TaskIterator(new ImportTask(services));
    }

    /**
     * Runs the Cytoscape network loader for the argument, then returns the models of the
     * new networks. The loader runs in its own task manager, so the result of the command is
     * only the result of cy3sbml, and a failed load fails the command.
     */
    public static final class ImportTask extends JsonTask {
        private static final Logger logger = LoggerFactory.getLogger(ImportTask.class);
        private static final String MODEL_FILE = "model.xml";
        private static final Set<String> ALLOWED_URL_SCHEMES = Set.of("http", "https");

        @Tunable(
                description = "SBML file",
                longDescription = "Path of an SBML file or a COMBINE archive (OMEX) to import.",
                exampleStringValue = "/home/user/models/BIOMD0000000012.xml",
                context = "nogui")
        public String file;

        @Tunable(
                description = "SBML URL",
                longDescription = "URL (http or https) of an SBML file or a COMBINE archive to import.",
                exampleStringValue =
                        "https://www.ebi.ac.uk/biomodels/model/download/BIOMD0000000012?filename=BIOMD0000000012_url.xml",
                context = "nogui")
        public String url;

        @Tunable(
                description = "SBML string",
                longDescription = "The SBML of the model as a string.",
                exampleStringValue =
                        "<sbml xmlns=\"http://www.sbml.org/sbml/level3/version2/core\" level=\"3\" version=\"2\">...</sbml>",
                context = "nogui")
        public String sbml;

        @Tunable(
                description = "BioModels id",
                longDescription = "Id of a BioModels model, which is downloaded from BioModels and imported.",
                exampleStringValue = "BIOMD0000000012",
                context = "nogui")
        public String biomodelsId;

        private final CommandServices services;

        ImportTask(CommandServices services) {
            this.services = services;
        }

        @Override
        public void run(TaskMonitor taskMonitor) throws Exception {
            taskMonitor.setTitle("cy3sbml import");
            long given = Stream.of(file, url, sbml, biomodelsId)
                    .filter(ImportTask::isSet)
                    .count();
            if (given != 1) {
                throw new IllegalArgumentException(
                        "Give exactly one of the arguments file, url, sbml and biomodelsId.");
            }
            Set<Long> before = networkSuids();
            Path temporaryDirectory = null;
            try {
                TaskIterator loader;
                if (isSet(file)) {
                    File input = new File(file.strip());
                    if (!input.isFile()) {
                        throw new IllegalArgumentException("The file '" + file + "' does not exist.");
                    }
                    loader = services.loadNetworkFile().createTaskIterator(input);
                } else if (isSet(url)) {
                    loader = services.loadNetworkURL().createTaskIterator(toUrl(url.strip()), null);
                } else if (isSet(sbml)) {
                    temporaryDirectory = Files.createTempDirectory("cy3sbml-import");
                    Path input = temporaryDirectory.resolve(MODEL_FILE);
                    Files.writeString(input, sbml, StandardCharsets.UTF_8);
                    loader = services.loadNetworkFile().createTaskIterator(input.toFile());
                } else {
                    loader = services.biomodelLoader().createTaskIterator(List.of(biomodelsId.strip()));
                }
                load(loader);
            } finally {
                deleteDirectory(temporaryDirectory);
            }
            setResult(Map.of("models", models(before)));
        }

        /** Runs the loader tasks, and fails with the error of a failed task. */
        private void load(TaskIterator loader) throws Exception {
            AtomicReference<FinishStatus> status = new AtomicReference<>(FinishStatus.getSucceeded());
            services.synchronousTaskManager().execute(loader, new TaskObserver() {
                @Override
                public void taskFinished(ObservableTask task) {
                    // the networks are found by comparing the networks before and after
                }

                @Override
                public void allFinished(FinishStatus finishStatus) {
                    status.set(finishStatus);
                }
            });
            FinishStatus finished = status.get();
            if (finished.getType() == FinishStatus.Type.FAILED && finished.getException() != null) {
                throw finished.getException();
            }
            if (finished.getType() == FinishStatus.Type.CANCELLED) {
                cancel();
            }
        }

        /** The SUIDs of all networks. */
        private Set<Long> networkSuids() {
            Set<Long> suids = new HashSet<>();
            for (CyNetwork network : services.networkManager().getNetworkSet()) {
                suids.add(network.getSUID());
            }
            return suids;
        }

        /** The models of the networks created since {@code before}, grouped by root network. */
        private List<Map<String, Object>> models(Set<Long> before) {
            List<CyNetwork> created = new ArrayList<>();
            for (CyNetwork network : services.networkManager().getNetworkSet()) {
                if (!before.contains(network.getSUID())) {
                    created.add(network);
                }
            }
            if (created.isEmpty()) {
                throw new IllegalStateException("No network was imported: the input is no SBML model or COMBINE"
                        + " archive, or it could not be read (see the Cytoscape log).");
            }
            created.sort(Comparator.comparing(CyNetwork::getSUID));
            Map<CyRootNetwork, List<CyNetwork>> byRoot = new LinkedHashMap<>();
            for (CyNetwork network : created) {
                CommandNetworks.root(network)
                        .ifPresent(root -> byRoot.computeIfAbsent(root, r -> new ArrayList<>())
                                .add(network));
            }
            List<Map<String, Object>> models = new ArrayList<>();
            byRoot.forEach(
                    (root, networks) -> models.add(CommandNetworks.modelJson(root, networks, services.sbmlManager())));
            return models;
        }

        private static boolean isSet(String value) {
            return value != null && !value.isBlank();
        }

        /**
         * The URL of the argument, an http or https URL: the command must not read local
         * files or other resources through {@code file:} or {@code jar:} URLs.
         */
        private static URL toUrl(String url) {
            URI uri;
            try {
                uri = new URI(url);
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("Invalid URL '" + url + "': " + e.getMessage(), e);
            }
            String scheme = uri.getScheme() != null ? uri.getScheme().toLowerCase(Locale.ROOT) : "";
            if (!ALLOWED_URL_SCHEMES.contains(scheme)) {
                throw new IllegalArgumentException("Invalid URL '" + url
                        + "': only http and https URLs are imported, give a local file with the argument file.");
            }
            try {
                return uri.toURL();
            } catch (IllegalArgumentException | MalformedURLException e) {
                throw new IllegalArgumentException("Invalid URL '" + url + "': " + e.getMessage(), e);
            }
        }

        private static void deleteDirectory(Path directory) {
            if (directory == null) {
                return;
            }
            try (Stream<Path> files = Files.walk(directory)) {
                for (Path path : files.sorted(Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            } catch (IOException e) {
                logger.warn("The temporary directory {} could not be deleted: {}", directory, e.getMessage());
            }
        }
    }
}

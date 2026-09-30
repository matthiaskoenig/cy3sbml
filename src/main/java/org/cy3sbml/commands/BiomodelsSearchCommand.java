package org.cy3sbml.commands;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.cy3sbml.biomodel.BiomodelSummary;
import org.cy3sbml.biomodel.BiomodelsSearchResult;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;

/**
 * {@code cy3sbml biomodels search}: searches BioModels, like the BioModels dialog.
 */
final class BiomodelsSearchCommand extends AbstractTaskFactory {
    static final String NAME = "biomodels search";

    private final CommandServices services;

    BiomodelsSearchCommand(CommandServices services) {
        this.services = services;
    }

    @Override
    public TaskIterator createTaskIterator() {
        return new TaskIterator(new BiomodelsSearchTask(services));
    }

    /** Searches BioModels and returns the number of matches and the models found. */
    public static final class BiomodelsSearchTask extends JsonTask {
        @Tunable(
                description = "Query",
                longDescription = "The BioModels search query, e.g. a model name, a species or a gene (the"
                        + " search of https://www.biomodels.org). At most 1000 models are returned.",
                exampleStringValue = "glycolysis",
                context = "nogui")
        public String query;

        private final CommandServices services;

        BiomodelsSearchTask(CommandServices services) {
            this.services = services;
        }

        @Override
        public void run(TaskMonitor taskMonitor) throws IOException {
            if (query == null || query.isBlank()) {
                throw new IllegalArgumentException("Give the search query.");
            }
            taskMonitor.setStatusMessage("Searching BioModels for '" + query + "' ...");
            BiomodelsSearchResult result = services.biomodelsQuery().search(query.strip());
            List<Map<String, Object>> models = new ArrayList<>();
            for (BiomodelSummary summary : result.models()) {
                Map<String, Object> model = new LinkedHashMap<>();
                model.put("id", summary.id());
                model.put("name", summary.name());
                model.put("submissionDate", summary.submissionDate());
                model.put("lastModified", summary.lastModified());
                models.add(model);
            }
            Map<String, Object> json = new LinkedHashMap<>();
            json.put("matches", result.matches());
            json.put("models", models);
            setResult(json);
        }
    }
}

package org.cy3sbml.biomodel;

import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchBioModelTaskFactory implements TaskFactory {
    private static final Logger logger = LoggerFactory.getLogger(SearchBioModelTaskFactory.class);

    private final SearchContent searchContent;
    private final BiomodelsQuery biomodelsQuery;

    public SearchBioModelTaskFactory(SearchContent searchContent, BiomodelsQuery biomodelsQuery) {
        this.biomodelsQuery = biomodelsQuery;
        logger.info("SearchBioModelTaskFactory created");
        this.searchContent = searchContent;
    }

    @Override
    public TaskIterator createTaskIterator() {

        SearchBioModelTask searchTask = new SearchBioModelTask(searchContent, biomodelsQuery);
        return new TaskIterator(searchTask);
    }

    @Override
    public boolean isReady() {

        // How to get data out of the task (Observable?)
        return true;
    }
}

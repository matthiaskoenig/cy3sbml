package org.cy3sbml.commands;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.util.List;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.json.JSONResult;

/**
 * A task of a command with a JSON result: the JSON for CyREST ({@link JSONResult}) and, as
 * {@link String}, for the command line.
 */
abstract class JsonTask extends AbstractTask implements ObservableTask {
    private static final ObjectMapper JSON = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private String json = "{}";

    /** Sets the result, converted to JSON (maps, lists, strings, numbers). */
    protected void setResult(Object result) {
        json = toJson(result);
    }

    /** The JSON of the result. */
    String json() {
        return json;
    }

    private static String toJson(Object result) {
        try {
            return JSON.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("The result could not be written as JSON: " + e.getMessage(), e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> R getResults(Class<? extends R> type) {
        if (type == String.class) {
            return (R) json;
        }
        if (type == JSONResult.class) {
            JSONResult result = () -> json;
            return (R) result;
        }
        return null;
    }

    @Override
    public List<Class<?>> getResultClasses() {
        return List.of(String.class, JSONResult.class);
    }
}

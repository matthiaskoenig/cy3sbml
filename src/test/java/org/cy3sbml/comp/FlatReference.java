package org.cy3sbml.comp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Stream;
import org.sbml.jsbml.ASTNode;
import org.sbml.jsbml.Event;
import org.sbml.jsbml.EventAssignment;
import org.sbml.jsbml.ExplicitRule;
import org.sbml.jsbml.InitialAssignment;
import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.NamedSBase;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.Rule;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.SimpleSpeciesReference;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.ext.comp.util.CompFlatteningConverter;

/**
 * The libSBML flattening reference of comp models, written by
 * {@code tools/pycysbml/comp_flat_reference.py}: the ids of the elements of the flat model
 * per type, and the ids each element references. Compares the JSBML flattening with it.
 */
public final class FlatReference {
    private static final String REFERENCES = "references";

    private FlatReference() {}

    /** The models of the reference JSON file, as paths relative to the file, and their summary. */
    public static Stream<Map.Entry<String, JsonNode>> entries(Path json) throws Exception {
        JsonNode root = new ObjectMapper().readTree(json.toFile());
        List<Map.Entry<String, JsonNode>> entries = new ArrayList<>();
        for (Iterator<Map.Entry<String, JsonNode>> it = root.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> entry = it.next();
            if (!entry.getKey().startsWith("_")) {
                entries.add(entry);
            }
        }
        return entries.stream();
    }

    /** Flattens the model file with JSBML and returns its summary in the form of the reference. */
    public static Map<String, Object> flatten(File file) throws Exception {
        SBMLDocument document;
        try (InputStream stream = file.toURI().toURL().openStream()) {
            document = new SBMLReader().readSBMLFromStream(stream);
        }
        document.setLocationURI(file.toURI().toString());
        return summary(new CompFlatteningConverter().flatten(document).getModel());
    }

    /** The reference entry in the form of {@link #summary(Model)}. */
    public static Map<String, Object> expected(JsonNode entry) {
        Map<String, Object> summary = new TreeMap<>();
        for (Iterator<Map.Entry<String, JsonNode>> it = entry.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> field = it.next();
            if (REFERENCES.equals(field.getKey())) {
                Map<String, List<String>> references = new TreeMap<>();
                field.getValue()
                        .fields()
                        .forEachRemaining(ref -> references.put(ref.getKey(), strings(ref.getValue())));
                summary.put(REFERENCES, references);
            } else {
                summary.put(field.getKey(), strings(field.getValue()));
            }
        }
        return summary;
    }

    /** The ids of the elements per type and the ids each element references. */
    public static Map<String, Object> summary(Model model) {
        Map<String, Object> summary = new TreeMap<>();
        summary.put("compartments", ids(model.getListOfCompartments(), FlatReference::id));
        summary.put("events", ids(model.getListOfEvents(), FlatReference::id));
        summary.put("functionDefinitions", ids(model.getListOfFunctionDefinitions(), FlatReference::id));
        summary.put("initialAssignments", ids(model.getListOfInitialAssignments(), InitialAssignment::getVariable));
        summary.put("parameters", ids(model.getListOfParameters(), FlatReference::id));
        summary.put("reactions", ids(model.getListOfReactions(), FlatReference::id));
        summary.put(
                "rules",
                model.getListOfRules().stream()
                        .filter(rule -> !rule.isAlgebraic())
                        .map(rule -> ((ExplicitRule) rule).getVariable())
                        .sorted()
                        .toList());
        summary.put("species", ids(model.getListOfSpecies(), FlatReference::id));
        summary.put("unitDefinitions", ids(model.getListOfUnitDefinitions(), FlatReference::id));
        summary.put(REFERENCES, references(model));
        return summary;
    }

    private static Map<String, List<String>> references(Model model) {
        Map<String, Set<String>> references = new TreeMap<>();
        for (Species species : model.getListOfSpecies()) {
            if (species.isSetCompartment()) {
                references.put("species:" + species.getId(), new TreeSet<>(List.of(species.getCompartment())));
            }
        }
        for (Reaction reaction : model.getListOfReactions()) {
            Set<String> names = new TreeSet<>();
            Stream.of(reaction.getListOfReactants(), reaction.getListOfProducts(), reaction.getListOfModifiers())
                    .flatMap(ListOf::stream)
                    .map(SimpleSpeciesReference::getSpecies)
                    .forEach(names::add);
            if (reaction.isSetKineticLaw()) {
                names(reaction.getKineticLaw().getMath(), names);
            }
            references.put("reaction:" + reaction.getId(), names);
        }
        for (Rule rule : model.getListOfRules()) {
            if (!rule.isAlgebraic()) {
                references.put("rule:" + ((ExplicitRule) rule).getVariable(), names(rule.getMath()));
            }
        }
        for (InitialAssignment assignment : model.getListOfInitialAssignments()) {
            references.put("initialAssignment:" + assignment.getVariable(), names(assignment.getMath()));
        }
        int index = 0;
        for (Event event : model.getListOfEvents()) {
            Set<String> names = new TreeSet<>();
            if (event.isSetTrigger()) {
                names(event.getTrigger().getMath(), names);
            }
            if (event.isSetDelay()) {
                names(event.getDelay().getMath(), names);
            }
            if (event.isSetPriority()) {
                names(event.getPriority().getMath(), names);
            }
            for (EventAssignment assignment : event.getListOfEventAssignments()) {
                names.add(assignment.getVariable());
                names(assignment.getMath(), names);
            }
            references.put("event:" + (event.isSetId() ? event.getId() : "#" + index), names);
            index++;
        }
        for (int i = 0; i < model.getConstraintCount(); i++) {
            references.put("constraint:#" + i, names(model.getConstraint(i).getMath()));
        }
        Map<String, List<String>> result = new TreeMap<>();
        references.forEach((key, names) -> {
            if (!names.isEmpty()) {
                result.put(key, new ArrayList<>(names));
            }
        });
        return result;
    }

    private static Set<String> names(ASTNode math) {
        Set<String> names = new TreeSet<>();
        names(math, names);
        return names;
    }

    /** The ids and function names in the math. */
    private static void names(ASTNode node, Set<String> names) {
        if (node == null) {
            return;
        }
        if (node.getType() == ASTNode.Type.NAME || node.getType() == ASTNode.Type.FUNCTION) {
            names.add(node.getName());
        }
        for (ASTNode child : node.getChildren()) {
            names(child, names);
        }
    }

    private static <T> List<String> ids(List<T> elements, Function<T, String> id) {
        return elements.stream().map(id).sorted().toList();
    }

    private static String id(Object sbase) {
        return ((NamedSBase) sbase).getId();
    }

    private static List<String> strings(JsonNode array) {
        List<String> strings = new ArrayList<>();
        array.forEach(node -> strings.add(node.asText()));
        return strings;
    }
}

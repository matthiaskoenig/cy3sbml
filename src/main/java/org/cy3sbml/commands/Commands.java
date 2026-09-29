package org.cy3sbml.commands;

import java.util.List;
import java.util.Properties;
import org.cytoscape.work.ServiceProperties;
import org.cytoscape.work.TaskFactory;

/**
 * The automation commands of cy3sbml in the command namespace {@value #NAMESPACE}: available
 * in the Cytoscape command line, in automation scripts and in CyREST as
 * {@code POST /v1/commands/cy3sbml/<command>}. See {@code docs/guide/automation.md}.
 */
public final class Commands {
    /** The command namespace. */
    public static final String NAMESPACE = "cy3sbml";

    private static final String MODELS_EXAMPLE = """
            {"models": [{"rootNetwork": 52, "name": "BIOMD0000000012", "modelId": "BIOMD0000000012",
              "modelName": "Elowitz2000 - Repressilator",
              "networks": [{"suid": 1024, "name": "BIOMD0000000012", "type": "base"},
                           {"suid": 1167, "name": "BIOMD0000000012__kinetic", "type": "kinetic"},
                           {"suid": 1310, "name": "BIOMD0000000012__all", "type": "all"}]}]}""";

    /**
     * A command: its name in the namespace, descriptions, an example of its JSON result, and
     * the task factory.
     */
    public record Command(
            String name, String description, String longDescription, String exampleJson, TaskFactory factory) {

        /** The service properties that register the task factory as the command. */
        public Properties properties() {
            Properties properties = new Properties();
            properties.setProperty(ServiceProperties.COMMAND_NAMESPACE, NAMESPACE);
            properties.setProperty(ServiceProperties.COMMAND, name);
            properties.setProperty(ServiceProperties.COMMAND_DESCRIPTION, description);
            properties.setProperty(ServiceProperties.COMMAND_LONG_DESCRIPTION, longDescription);
            properties.setProperty(ServiceProperties.COMMAND_SUPPORTS_JSON, "true");
            properties.setProperty(ServiceProperties.COMMAND_EXAMPLE_JSON, exampleJson);
            return properties;
        }
    }

    private Commands() {}

    /** All commands, with their task factories using the given services. */
    public static List<Command> create(CommandServices services) {
        return List.of(
                new Command(
                        ImportCommand.NAME,
                        "Import an SBML model",
                        "Imports an SBML model from a file, a URL, an SBML string or BioModels (exactly one of"
                                + " the arguments file, url, sbml and biomodelsId), like an import in the GUI: the"
                                + " base, kinetic and all network and a network per layout, with views and the"
                                + " cy3sbml style. A COMBINE archive (file or url) imports its SBML models. Returns"
                                + " the imported models with the SUIDs and types (base, kinetic, all, layout) of"
                                + " their networks.",
                        MODELS_EXAMPLE,
                        new ImportCommand(services)),
                new Command(
                        BiomodelsSearchCommand.NAME,
                        "Search BioModels",
                        "Searches BioModels (https://www.biomodels.org) and returns the number of matches and"
                                + " the matching models (at most 1000) with id, name, submission date and date of"
                                + " the last modification. Import a model with cy3sbml import biomodelsId=<id>.",
                        """
                        {"matches": 1, "models": [{"id": "BIOMD0000000012", "name": "Elowitz2000 - Repressilator",
                          "submissionDate": "2005-02-08T00:00:00Z", "lastModified": "2012-07-05T00:00:00Z"}]}""",
                        new BiomodelsSearchCommand(services)),
                new Command(
                        NetworksCommand.NAME,
                        "List the SBML models and their networks",
                        "Returns the SBML models open in Cytoscape: per model the root network, model id and"
                                + " name, SBML level and version, the packages with their versions, and the"
                                + " networks with SUID, name and type (base, kinetic, all, layout).",
                        """
                        {"models": [{"rootNetwork": 52, "name": "BIOMD0000000012", "modelId": "BIOMD0000000012",
                          "modelName": "Elowitz2000 - Repressilator",
                          "networks": [{"suid": 1024, "name": "BIOMD0000000012", "type": "base"}],
                          "level": 2, "version": 1, "packages": {}}]}""",
                        new NetworksCommand(services)),
                new Command(
                        DocumentCommand.NAME,
                        "Get the SBML of a network",
                        "Returns the SBML document of the model of a network as a string, or writes it to the"
                                + " given file.",
                        """
                        {"sbml": "<?xml version='1.0' encoding='UTF-8' standalone='no'?>\\n<sbml ...>...</sbml>"}""",
                        new DocumentCommand(services)),
                new Command(
                        ElementCommand.NAME,
                        "Get the SBML elements of nodes",
                        "Returns the SBML elements of nodes (nodeList), of an SBML id (sbmlId) or of a metaid"
                                + " (metaId): the JSBML class, id, name, metaid, SBO term, the CV terms of the"
                                + " annotation, the notes (XHTML) and the SUIDs of the nodes of the element in the"
                                + " network.",
                        """
                        {"elements": [{"class": "Species", "id": "PX", "name": "LacI protein",
                          "metaId": "_000006", "sboTerm": "SBO:0000252",
                          "cvTerms": [{"qualifier": "BQB_IS_VERSION_OF",
                                       "resources": ["http://identifiers.org/uniprot/P03023"]}],
                          "notes": "", "nodes": [1047]}]}""",
                        new ElementCommand(services)),
                new Command(
                        NodesCommand.NAME,
                        "Get the nodes of SBML ids",
                        "Returns the SUIDs of the nodes of SBML ids in a network (the column sbml id), for the"
                                + " given comma separated ids or all SBML ids of the network. Use it to map data"
                                + " with SBML ids onto the nodes.",
                        """
                        {"nodes": {"PX": [1047], "PY": [1049]}}""",
                        new NodesCommand(services)),
                new Command(
                        CofactorsCommand.NAME_SPLIT,
                        "Split cofactor nodes",
                        "Splits the given nodes into one node per edge (clones, column cofactorClone), like the"
                                + " Split cofactor nodes button. Returns the SUIDs of the clones.",
                        """
                        {"clones": [2101, 2102, 2103]}""",
                        CofactorsCommand.split(services)),
                new Command(
                        CofactorsCommand.NAME_MERGE,
                        "Merge cofactor nodes",
                        "Merges the given split nodes (clones), or all split nodes of the network if no nodes are"
                                + " given, back into their node, like the Merge cofactor nodes button. Returns the"
                                + " SUIDs of the merged nodes.",
                        """
                        {"merged": [1047]}""",
                        CofactorsCommand.merge(services)),
                new Command(
                        LayoutCommand.NAME_SAVE,
                        "Save the node positions in a layout file",
                        "Saves the positions of the nodes of the view of a network in a layout file (XML), like"
                                + " the Save Layout button. Returns the file and the number of saved positions.",
                        """
                        {"file": "/home/user/layout.xml", "nodes": 12}""",
                        LayoutCommand.save(services)),
                new Command(
                        LayoutCommand.NAME_LOAD,
                        "Load the node positions of a layout file",
                        "Moves the nodes of the view of a network to their positions in a layout file (XML),"
                                + " like the Load Layout button. Returns the file and the number of moved nodes.",
                        """
                        {"file": "/home/user/layout.xml", "nodes": 12}""",
                        LayoutCommand.load(services)));
    }
}

package org.cy3sbml.commands;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.xml.stream.XMLStreamException;
import org.cy3sbml.SBML;
import org.cytoscape.command.util.NodeList;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.NamedSBase;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBase;

/**
 * {@code cy3sbml element}: the SBML elements of nodes, of an SBML id or of a metaid, with
 * their annotations, notes and nodes.
 */
final class ElementCommand extends AbstractTaskFactory {
    static final String NAME = "element";

    private final CommandServices services;

    ElementCommand(CommandServices services) {
        this.services = services;
    }

    @Override
    public TaskIterator createTaskIterator() {
        return new TaskIterator(new ElementTask(services));
    }

    /** Returns the SBML elements of the given nodes, SBML id or metaid. */
    public static final class ElementTask extends JsonTask {
        @Tunable(
                description = "Network",
                longDescription = CommandArguments.NETWORK,
                exampleStringValue = CommandArguments.NETWORK_EXAMPLE,
                context = "nogui")
        public CyNetwork network;

        public NodeList nodeList = new NodeList(null);

        @Tunable(
                description = "Nodes",
                longDescription = CommandArguments.NODE_LIST + " The elements of these nodes are returned.",
                exampleStringValue = CommandArguments.NODE_LIST_EXAMPLE,
                context = "nogui")
        public NodeList getnodeList() {
            nodeList.setNetwork(
                    network != null ? network : services.applicationManager().getCurrentNetwork());
            return nodeList;
        }

        public void setnodeList(NodeList value) {
            // set by the tunable handler through getnodeList
        }

        @Tunable(
                description = "SBML id",
                longDescription = "The SBML id (SId) of the element.",
                exampleStringValue = "glc",
                context = "nogui")
        public String sbmlId;

        @Tunable(
                description = "metaid",
                longDescription = "The metaid of the element.",
                exampleStringValue = "meta_glc",
                context = "nogui")
        public String metaId;

        private final CommandServices services;

        ElementTask(CommandServices services) {
            this.services = services;
        }

        @Override
        public void run(TaskMonitor taskMonitor) throws XMLStreamException {
            CyNetwork target = CommandNetworks.network(network, services);
            SBMLDocument document = CommandNetworks.document(target, services.sbmlManager());
            List<CyNode> nodes = nodeList.getValue() != null ? nodeList.getValue() : List.of();
            long given = Stream.of(!nodes.isEmpty(), isSet(sbmlId), isSet(metaId))
                    .filter(Boolean::booleanValue)
                    .count();
            if (given != 1) {
                throw new IllegalArgumentException("Give exactly one of the arguments nodeList, sbmlId and metaId.");
            }
            List<SBase> elements = new ArrayList<>();
            if (isSet(sbmlId)) {
                SBase element = document.getElementBySId(sbmlId.strip());
                if (element == null) {
                    throw new IllegalArgumentException("No element with the SBML id '" + sbmlId + "'.");
                }
                elements.add(element);
            } else if (isSet(metaId)) {
                SBase element = document.getElementByMetaId(metaId.strip());
                if (element == null) {
                    throw new IllegalArgumentException("No element with the metaid '" + metaId + "'.");
                }
                elements.add(element);
            } else {
                for (CyNode node : nodes) {
                    String cyId = target.getRow(node).get(SBML.ATTR_CYID, String.class);
                    SBase element = cyId != null ? document.getElementByMetaId(cyId) : null;
                    if (element != null && !elements.contains(element)) {
                        elements.add(element);
                    }
                }
            }
            List<Map<String, Object>> json = new ArrayList<>();
            for (SBase element : elements) {
                json.add(elementJson(element, target));
            }
            setResult(Map.of("elements", json));
        }

        private Map<String, Object> elementJson(SBase element, CyNetwork target) throws XMLStreamException {
            Map<String, Object> json = new LinkedHashMap<>();
            json.put("class", element.getClass().getSimpleName());
            NamedSBase named = element instanceof NamedSBase n ? n : null;
            json.put("id", named != null && named.isSetId() ? named.getId() : "");
            json.put("name", named != null && named.isSetName() ? named.getName() : "");
            json.put("metaId", element.isSetMetaId() ? element.getMetaId() : "");
            json.put("sboTerm", element.isSetSBOTerm() ? element.getSBOTermID() : "");
            List<Map<String, Object>> cvTerms = new ArrayList<>();
            if (element.isSetAnnotation()) {
                for (CVTerm term : element.getCVTerms()) {
                    Map<String, Object> cvTerm = new LinkedHashMap<>();
                    cvTerm.put("qualifier", term.getQualifier().name());
                    cvTerm.put("resources", term.getResources());
                    cvTerms.add(cvTerm);
                }
            }
            json.put("cvTerms", cvTerms);
            json.put("notes", element.isSetNotes() ? element.getNotesString() : "");
            List<Long> nodes = new ArrayList<>();
            if (element.isSetMetaId()) {
                for (CyNode node : target.getNodeList()) {
                    if (element.getMetaId().equals(target.getRow(node).get(SBML.ATTR_CYID, String.class))) {
                        nodes.add(node.getSUID());
                    }
                }
            }
            json.put("nodes", nodes);
            return json;
        }

        private static boolean isSet(String value) {
            return value != null && !value.isBlank();
        }
    }
}

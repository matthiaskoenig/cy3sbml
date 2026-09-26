package org.cy3sbml.reader;

import java.util.ArrayList;
import java.util.List;
import org.cy3sbml.SBML;
import org.cy3sbml.util.AttributeUtil;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.ext.qual.FunctionTerm;
import org.sbml.jsbml.ext.qual.Input;
import org.sbml.jsbml.ext.qual.Output;
import org.sbml.jsbml.ext.qual.QualConstants;
import org.sbml.jsbml.ext.qual.QualModelPlugin;
import org.sbml.jsbml.ext.qual.QualitativeSpecies;
import org.sbml.jsbml.ext.qual.Transition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads the qual package: qualitative species and transitions with their inputs and outputs.
 */
final class QualReader implements PackageReader {
    private static final Logger logger = LoggerFactory.getLogger(QualReader.class);

    /**
     * Create nodes, edges and attributes from Qualitative Model.
     */
    @Override
    public void read(ConversionContext context, Model model) {
        CyNetwork network = context.network();
        logger.debug("<qual>");

        QualModelPlugin qualModel = (QualModelPlugin) model.getExtension(QualConstants.namespaceURI);
        if (qualModel == null) {
            return;
        }

        // QualSpecies //
        for (QualitativeSpecies qSpecies : qualModel.getListOfQualitativeSpecies()) {
            CyNode n = context.createNode(qSpecies, SBML.NODETYPE_QUAL_SPECIES);
            AttributeWriter.setNamedSBaseAttributes(network, n, qSpecies);

            if (qSpecies.isSetCompartment()) {
                AttributeUtil.set(network, n, SBML.ATTR_COMPARTMENT, qSpecies.getCompartment(), String.class);
                // edge to compartment
                Compartment comp = qSpecies.getCompartmentInstance();
                CyNode compNode = context.nodeByMetaId(comp.getMetaId()).orElse(null);
                context.createEdge(n, compNode, SBML.INTERACTION_SPECIES_COMPARTMENT);
            }
            if (qSpecies.isSetConstant()) {
                AttributeUtil.set(network, n, SBML.ATTR_CONSTANT, qSpecies.getConstant(), Boolean.class);
            }
            if (qSpecies.isSetInitialLevel()) {
                AttributeUtil.set(network, n, SBML.ATTR_QUAL_INITIAL_LEVEL, qSpecies.getInitialLevel(), Integer.class);
            }
            if (qSpecies.isSetMaxLevel()) {
                AttributeUtil.set(network, n, SBML.ATTR_QUAL_MAX_LEVEL, qSpecies.getMaxLevel(), Integer.class);
            }
        }
        // QualTransitions
        for (Transition transition : qualModel.getListOfTransitions()) {
            CyNode n = context.createNode(transition, SBML.NODETYPE_QUAL_TRANSITION);
            AttributeWriter.setNamedSBaseAttributes(network, n, transition);

            // Inputs
            for (Input input : transition.getListOfInputs()) {
                String qSpeciesId = input.getQualitativeSpecies();
                QualitativeSpecies qSpecies = qualModel.getQualitativeSpecies(qSpeciesId);

                CyNode inNode = context.nodeByMetaId(qSpecies.getMetaId()).orElse(null);
                CyEdge e = context.createEdge(n, inNode, SBML.INTERACTION_QUAL_TRANSITION_INPUT);

                // required (no checking of required -> NullPointerException risk)
                AttributeUtil.set(
                        network,
                        e,
                        SBML.ATTR_QUAL_TRANSITION_EFFECT,
                        input.getTransitionEffect().toString(),
                        String.class);
                AttributeUtil.set(
                        network,
                        e,
                        SBML.ATTR_QUAL_QUALITATIVE_SPECIES,
                        input.getQualitativeSpecies().toString(),
                        String.class);
                // optional
                if (input.isSetId()) {
                    AttributeUtil.set(network, e, SBML.ATTR_ID, input.getId(), String.class);
                }
                if (input.isSetName()) {
                    AttributeUtil.set(network, e, SBML.ATTR_NAME, input.getName(), String.class);
                }
                if (input.isSetSign()) {
                    AttributeUtil.set(
                            network, e, SBML.ATTR_QUAL_SIGN, input.getSign().toString(), String.class);
                }
                if (input.isSetSBOTerm()) {
                    AttributeUtil.set(network, e, SBML.ATTR_SBOTERM, input.getSBOTermID(), String.class);
                }
                if (input.isSetMetaId()) {
                    AttributeUtil.set(network, e, SBML.ATTR_METAID, input.getMetaId(), String.class);
                }
                if (input.isSetThresholdLevel()) {
                    AttributeUtil.set(
                            network, e, SBML.ATTR_QUAL_THRESHOLD_LEVEL, input.getThresholdLevel(), Integer.class);
                }
            }

            // Outputs
            for (Output output : transition.getListOfOutputs()) {
                String qSpeciesString = output.getQualitativeSpecies();
                QualitativeSpecies qSpecies = qualModel.getQualitativeSpecies(qSpeciesString);
                CyNode outNode = context.nodeByMetaId(qSpecies.getMetaId()).orElse(null);
                CyEdge e = context.createEdge(n, outNode, SBML.INTERACTION_QUAL_TRANSITION_OUTPUT);

                // required
                AttributeUtil.set(
                        network,
                        e,
                        SBML.ATTR_QUAL_QUALITATIVE_SPECIES,
                        output.getQualitativeSpecies().toString(),
                        String.class);
                AttributeUtil.set(
                        network,
                        e,
                        SBML.ATTR_QUAL_TRANSITION_EFFECT,
                        output.getTransitionEffect().toString(),
                        String.class);
                // optional
                if (output.isSetId()) {
                    AttributeUtil.set(network, e, SBML.ATTR_ID, output.getId(), String.class);
                }
                if (output.isSetName()) {
                    AttributeUtil.set(network, e, SBML.ATTR_NAME, output.getName(), String.class);
                }
                if (output.isSetSBOTerm()) {
                    AttributeUtil.set(network, e, SBML.ATTR_SBOTERM, output.getSBOTermID(), String.class);
                }
                if (output.isSetMetaId()) {
                    AttributeUtil.set(network, e, SBML.ATTR_METAID, output.getMetaId(), String.class);
                }
                if (output.isSetOutputLevel()) {
                    AttributeUtil.set(network, e, SBML.ATTR_QUAL_OUTPUT_LEVEL, output.getOutputLevel(), Integer.class);
                }
            }

            // parse the default term / function terms
            if (transition.isSetListOfFunctionTerms()) {
                List<Integer> resultLevels = new ArrayList<Integer>();
                for (FunctionTerm term : transition.getListOfFunctionTerms()) {
                    resultLevels.add(term.getResultLevel());
                }
                AttributeUtil.setList(network, n, SBML.ATTR_QUAL_RESULT_LEVELS, resultLevels, Integer.class);
            }
        }
    }
}

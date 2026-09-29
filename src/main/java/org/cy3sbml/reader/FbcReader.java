package org.cy3sbml.reader;

import java.util.ArrayList;
import java.util.List;
import org.cy3sbml.SBML;
import org.cy3sbml.util.AttributeUtil;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.sbml.jsbml.Annotation;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBase;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.ext.fbc.And;
import org.sbml.jsbml.ext.fbc.Association;
import org.sbml.jsbml.ext.fbc.FBCConstants;
import org.sbml.jsbml.ext.fbc.FBCModelPlugin;
import org.sbml.jsbml.ext.fbc.FBCReactionPlugin;
import org.sbml.jsbml.ext.fbc.FBCSpeciesPlugin;
import org.sbml.jsbml.ext.fbc.FluxBound;
import org.sbml.jsbml.ext.fbc.FluxObjective;
import org.sbml.jsbml.ext.fbc.GeneProduct;
import org.sbml.jsbml.ext.fbc.GeneProductAssociation;
import org.sbml.jsbml.ext.fbc.GeneProductRef;
import org.sbml.jsbml.ext.fbc.LogicalOperator;
import org.sbml.jsbml.ext.fbc.Objective;
import org.sbml.jsbml.ext.fbc.UserDefinedConstraint;
import org.sbml.jsbml.ext.fbc.UserDefinedConstraintComponent;
import org.sbml.jsbml.xml.XMLNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads the fbc package: flux bounds, objectives, chemical formulas and charges,
 * the gene product association networks and the user defined constraints (fbc v3).
 */
final class FbcReader implements PackageReader {
    private static final Logger logger = LoggerFactory.getLogger(FbcReader.class);
    // label of a user defined constraint without name and id
    private static final String USER_DEFINED_CONSTRAINT_LABEL = "UDC";

    /**
     * Creates network information from fbc model.
     */
    @Override
    public void read(ConversionContext context, Model model) {
        CyNetwork network = context.network();
        logger.debug("<fbc>");

        FBCModelPlugin fbcModel = (FBCModelPlugin) model.getExtension(FBCConstants.shortLabel);
        if (fbcModel == null) {
            return;
        }

        // Model attributes
        if (fbcModel.isSetStrict()) {
            AttributeUtil.set(network, network, SBML.ATTR_FBC_STRICT, fbcModel.getStrict(), Boolean.class);
        }

        // Species attributes
        for (Species species : model.getListOfSpecies()) {
            FBCSpeciesPlugin fbcSpecies = (FBCSpeciesPlugin) species.getExtension(FBCConstants.shortLabel);
            if (fbcSpecies != null) {
                CyNode n = context.nodeByMetaId(species.getMetaId()).orElse(null);
                // optional
                // the charge is a double in fbc v3, an integer in fbc v1 and v2
                if (fbcSpecies.isSetCharge() && fbcSpecies.getPackageVersion() >= 3) {
                    AttributeUtil.set(network, n, SBML.ATTR_FBC_CHARGE, fbcSpecies.getChargeAsDouble(), Double.class);
                } else if (fbcSpecies.isSetCharge()) {
                    AttributeUtil.set(network, n, SBML.ATTR_FBC_CHARGE, fbcSpecies.getCharge(), Integer.class);
                }
                if (fbcSpecies.isSetChemicalFormula()) {
                    AttributeUtil.set(
                            network, n, SBML.ATTR_FBC_CHEMICAL_FORMULA, fbcSpecies.getChemicalFormula(), String.class);
                }
            }
        }

        // List of flux objectives (handled via reaction attributes)
        // (activeObjective is not parsed)
        for (Objective objective : fbcModel.getListOfObjectives()) {
            // one reaction attribute column per objective
            String key = String.format(SBML.ATTR_FBC_OBJECTIVE_TEMPLATE, objective.getId());
            String variableTypeKey = String.format(SBML.ATTR_FBC_OBJECTIVE_VARIABLE_TYPE_TEMPLATE, objective.getId());
            for (FluxObjective fluxObjective : objective.getListOfFluxObjectives()) {
                Reaction reaction = fluxObjective.getReactionInstance();
                CyNode node = context.nodeByMetaId(reaction.getMetaId()).orElse(null);
                AttributeUtil.set(network, node, key, fluxObjective.getCoefficient(), Double.class);
                if (fluxObjective.isSetVariableType()) {
                    AttributeUtil.set(
                            network,
                            node,
                            variableTypeKey,
                            fluxObjective.getVariableType().toString(),
                            String.class);
                }
            }
        }

        // GeneProducts as nodes
        for (GeneProduct geneProduct : fbcModel.getListOfGeneProducts()) {
            CyNode n = context.createNode(geneProduct, SBML.NODETYPE_FBC_GENEPRODUCT);
            AttributeWriter.setNamedSBaseAttributes(network, n, geneProduct);

            // Overwrite label
            if (geneProduct.isSetLabel()) {
                AttributeUtil.set(network, n, SBML.LABEL, geneProduct.getLabel(), String.class);
            }

            // edge to associated species
            if (geneProduct.isSetAssociatedSpecies()) {
                // id lookup
                CyNode speciesNode =
                        context.nodeById(geneProduct.getAssociatedSpecies()).orElse(null);
                context.createEdge(speciesNode, n, SBML.INTERACTION_FBC_GENEPRODUCT_SPECIES);
            }
        }

        // Reaction attributes
        for (Reaction reaction : model.getListOfReactions()) {
            FBCReactionPlugin fbcReaction = (FBCReactionPlugin) reaction.getExtension(FBCConstants.shortLabel);

            if (fbcReaction != null) {
                // optional bounds
                CyNode node = context.nodeByMetaId(reaction.getMetaId()).orElse(null);
                if (fbcReaction.isSetLowerFluxBound()) {
                    AttributeUtil.set(
                            network,
                            node,
                            SBML.ATTR_FBC_LOWER_FLUX_BOUND,
                            fbcReaction.getLowerFluxBound(),
                            String.class);
                    createReferenceEdge(
                            context,
                            fbcReaction.getLowerFluxBound(),
                            node,
                            SBML.INTERACTION_PARAMETER_REACTION,
                            reaction);
                }
                if (fbcReaction.isSetUpperFluxBound()) {
                    AttributeUtil.set(
                            network,
                            node,
                            SBML.ATTR_FBC_UPPER_FLUX_BOUND,
                            fbcReaction.getUpperFluxBound(),
                            String.class);
                    createReferenceEdge(
                            context,
                            fbcReaction.getUpperFluxBound(),
                            node,
                            SBML.INTERACTION_PARAMETER_REACTION,
                            reaction);
                }

                // Create GeneProteinAssociation (GPA) network
                if (fbcReaction.isSetGeneProductAssociation()) {
                    GeneProductAssociation gpa = fbcReaction.getGeneProductAssociation();

                    // handle And, Or, GeneProductRef recursively
                    Association association = gpa.getAssociation();
                    processAssociation(context, node, SBML.NODETYPE_REACTION, association);
                }
            }
        }

        readUserDefinedConstraints(context, fbcModel);

        // parse fbc v1 fluxBounds and geneAssociations
        if (fbcModel.getVersion() == 1) {
            // geneAssociations
            if (model.isSetAnnotation()) {
                // fbc v1 geneAssociations not in specification or supported by JSBML, so not parsed
                Annotation annotation = model.getAnnotation();
                XMLNode xmlNode = annotation.getXMLNode();

                for (int k = 0; k < xmlNode.getChildCount(); k++) {
                    XMLNode child = xmlNode.getChild(k);
                    String name = child.getName();
                    if (name.equals("listOfGeneAssociations") || name.equals("geneAssociation")) {
                        logger.warn("GeneAssociations of fbc v1 not supported in JSBML.");
                        break;
                    }
                }
            }

            readFluxBounds(context, fbcModel);
        }
    }

    /**
     * Reads the user defined constraints (fbc v3). A constraint is a node with the
     * parameters of its bounds as edges; a component is an edge from the node of its
     * variable (and one from the node of its second variable) to the constraint node, with
     * the coefficient parameter and the variable type as edge columns.
     */
    private static void readUserDefinedConstraints(ConversionContext context, FBCModelPlugin fbcModel) {
        CyNetwork network = context.network();
        for (UserDefinedConstraint constraint : fbcModel.getListOfUserDefinedConstraints()) {
            CyNode n = context.createNode(constraint, SBML.NODETYPE_FBC_USER_DEFINED_CONSTRAINT);
            AttributeWriter.setNamedSBaseAttributes(network, n, constraint);
            if (!constraint.isSetName() && !constraint.isSetId()) {
                AttributeUtil.set(network, n, SBML.LABEL, USER_DEFINED_CONSTRAINT_LABEL, String.class);
            }
            if (constraint.isSetLowerBound()) {
                AttributeUtil.set(network, n, SBML.ATTR_FBC_LOWER_BOUND, constraint.getLowerBound(), String.class);
                createReferenceEdge(
                        context,
                        constraint.getLowerBound(),
                        n,
                        SBML.INTERACTION_FBC_PARAMETER_USER_DEFINED_CONSTRAINT,
                        constraint);
            }
            if (constraint.isSetUpperBound()) {
                AttributeUtil.set(network, n, SBML.ATTR_FBC_UPPER_BOUND, constraint.getUpperBound(), String.class);
                createReferenceEdge(
                        context,
                        constraint.getUpperBound(),
                        n,
                        SBML.INTERACTION_FBC_PARAMETER_USER_DEFINED_CONSTRAINT,
                        constraint);
            }
            for (UserDefinedConstraintComponent component : constraint.getListOfUserDefinedConstraintComponents()) {
                for (String variable : componentVariables(component)) {
                    CyEdge edge = createReferenceEdge(
                            context, variable, n, SBML.INTERACTION_FBC_VARIABLE_USER_DEFINED_CONSTRAINT, component);
                    if (edge == null) {
                        continue;
                    }
                    if (component.isSetCoefficient()) {
                        AttributeUtil.set(
                                network, edge, SBML.ATTR_FBC_COEFFICIENT, component.getCoefficient(), String.class);
                    }
                    if (component.isSetVariableType()) {
                        AttributeUtil.set(
                                network,
                                edge,
                                SBML.ATTR_FBC_VARIABLE_TYPE,
                                component.getVariableType().toString(),
                                String.class);
                    }
                }
            }
        }
    }

    /** The variable and the second variable of the component, those that are set. */
    private static List<String> componentVariables(UserDefinedConstraintComponent component) {
        List<String> variables = new ArrayList<>(2);
        if (component.isSetVariable()) {
            variables.add(component.getVariable());
        }
        if (component.isSetVariable2()) {
            variables.add(component.getVariable2());
        }
        return variables;
    }

    /**
     * Creates the edge from the node of the element with the SId to the target node, or
     * logs and returns null if there is no such node (a missing element, or one without
     * node).
     */
    private static CyEdge createReferenceEdge(
            ConversionContext context, String sid, CyNode target, String interaction, SBase element) {
        CyNode source = context.nodeById(sid).orElse(null);
        if (source == null || target == null) {
            logger.warn("No edge {} from '{}' to {}: the element has no node.", interaction, sid, element);
            return null;
        }
        return context.createEdge(source, target, interaction);
    }

    /**
     * Reads the fbc v1 flux bounds, which were replaced by the flux bound attributes
     * of the reaction in fbc v2.
     */
    // reason: FluxBound is deprecated in JSBML, but needed to read fbc v1 models.
    @SuppressWarnings("deprecation")
    private static void readFluxBounds(ConversionContext context, FBCModelPlugin fbcModel) {
        CyNetwork network = context.network();
        for (FluxBound fluxBound : fbcModel.getListOfFluxBounds()) {
            FluxBound.Operation operation = fluxBound.getOperation();
            if (operation == null) {
                logger.warn("FluxBound without operation skipped: {}", fluxBound);
                continue;
            }
            Reaction reaction = fluxBound.getReactionInstance();
            CyNode n = reaction == null
                    ? null
                    : context.nodeByMetaId(reaction.getMetaId()).orElse(null);
            if (n == null) {
                logger.warn("FluxBound for a missing reaction skipped: {}", fluxBound);
                continue;
            }
            String value = Double.toString(fluxBound.getValue());
            switch (operation) {
                case EQUAL -> {
                    AttributeUtil.set(network, n, SBML.ATTR_FBC_LOWER_FLUX_BOUND, value, String.class);
                    AttributeUtil.set(network, n, SBML.ATTR_FBC_UPPER_FLUX_BOUND, value, String.class);
                }
                case GREATER_EQUAL ->
                    AttributeUtil.set(network, n, SBML.ATTR_FBC_LOWER_FLUX_BOUND, value, String.class);
                case LESS_EQUAL -> AttributeUtil.set(network, n, SBML.ATTR_FBC_UPPER_FLUX_BOUND, value, String.class);
                default -> logger.warn("Unsupported operation of fbc v1 FluxBound: {}", fluxBound);
            }
        }
    }

    /**
     * Recursive function for processing the Associations.
     * Gene product references are linked to their gene product node, and/or
     * operators become nodes whose children are processed recursively.
     */
    private static void processAssociation(
            ConversionContext context, CyNode parentNode, String parentType, Association association) {
        CyNetwork network = context.network();
        String interaction = parentType.equals(SBML.NODETYPE_REACTION)
                ? SBML.INTERACTION_FBC_ASSOCIATION_REACTION
                : SBML.INTERACTION_FBC_ASSOCIATION_ASSOCIATION;

        if (association instanceof GeneProductRef gpRef) {
            CyNode gpNode = context.nodeByMetaId(gpRef.getGeneProductInstance().getMetaId())
                    .orElse(null);
            if (gpNode != null) {
                context.createEdge(gpNode, parentNode, interaction);
            } else {
                logger.error(
                        "GeneProduct does not exist for GeneAssociation: {} in {}",
                        gpRef.getGeneProduct(),
                        association);
            }
        } else if (association instanceof LogicalOperator operator) {
            boolean isAnd = operator instanceof And;
            String nodeType = isAnd ? SBML.NODETYPE_FBC_AND : SBML.NODETYPE_FBC_OR;

            CyNode operatorNode = context.createNode(operator, nodeType);
            AttributeUtil.set(network, operatorNode, SBML.LABEL, isAnd ? "AND" : "OR", String.class);
            context.createEdge(operatorNode, parentNode, interaction);

            for (Association child : operator.getListOfAssociations()) {
                processAssociation(context, operatorNode, nodeType, child);
            }
        }
    }
}

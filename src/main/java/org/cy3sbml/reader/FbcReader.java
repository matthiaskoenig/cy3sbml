package org.cy3sbml.reader;

import org.cy3sbml.SBML;
import org.cy3sbml.util.AttributeUtil;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.sbml.jsbml.Annotation;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Parameter;
import org.sbml.jsbml.Reaction;
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
import org.sbml.jsbml.ext.fbc.Objective;
import org.sbml.jsbml.ext.fbc.Or;
import org.sbml.jsbml.xml.XMLNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads the fbc package: flux bounds, objectives, chemical formulas and charges,
 * and the gene product association networks.
 */
final class FbcReader implements PackageReader {
    private static final Logger logger = LoggerFactory.getLogger(FbcReader.class);

    /**
     * Creates network information from fbc model.
     */
    @Override
    public void read(ConversionContext context, Model model) {
        CyNetwork network = context.network();
        logger.debug("<fbc>");

        FBCModelPlugin fbcModel = (FBCModelPlugin) model.getExtension(FBCConstants.namespaceURI);
        if (fbcModel == null) {
            return;
        }

        // Model attributes
        if (fbcModel.isSetStrict()) {
            AttributeUtil.set(network, network, SBML.ATTR_FBC_STRICT, fbcModel.getStrict(), Boolean.class);
        }

        // Species attributes
        for (Species species : model.getListOfSpecies()) {
            FBCSpeciesPlugin fbcSpecies = (FBCSpeciesPlugin) species.getExtension(FBCConstants.namespaceURI);
            if (fbcSpecies != null) {
                CyNode n = context.nodeByMetaId(species.getMetaId()).orElse(null);
                // optional
                if (fbcSpecies.isSetCharge()) {
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
            for (FluxObjective fluxObjective : objective.getListOfFluxObjectives()) {
                Reaction reaction = fluxObjective.getReactionInstance();
                CyNode node = context.nodeByMetaId(reaction.getMetaId()).orElse(null);
                AttributeUtil.set(network, node, key, fluxObjective.getCoefficient(), Double.class);
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
            FBCReactionPlugin fbcReaction = (FBCReactionPlugin) reaction.getExtension(FBCConstants.namespaceURI);

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
                    // add edge
                    Parameter p = model.getParameter(fbcReaction.getLowerFluxBound());
                    CyNode parameterNode = context.nodeByMetaId(p.getMetaId()).orElse(null);
                    CyEdge edge = network.addEdge(parameterNode, node, true);
                    AttributeUtil.set(
                            network, edge, SBML.INTERACTION_ATTR, SBML.INTERACTION_PARAMETER_REACTION, String.class);
                }
                if (fbcReaction.isSetUpperFluxBound()) {
                    AttributeUtil.set(
                            network,
                            node,
                            SBML.ATTR_FBC_UPPER_FLUX_BOUND,
                            fbcReaction.getUpperFluxBound(),
                            String.class);
                    // add edge
                    Parameter p = model.getParameter(fbcReaction.getUpperFluxBound());
                    CyNode parameterNode = context.nodeByMetaId(p.getMetaId()).orElse(null);
                    CyEdge edge = network.addEdge(parameterNode, node, true);
                    AttributeUtil.set(
                            network, edge, SBML.INTERACTION_ATTR, SBML.INTERACTION_PARAMETER_REACTION, String.class);
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
     * Reads the fbc v1 flux bounds, which were replaced by the flux bound attributes
     * of the reaction in fbc v2.
     */
    // reason: FluxBound is deprecated in JSBML, but needed to read fbc v1 models.
    @SuppressWarnings("deprecation")
    private static void readFluxBounds(ConversionContext context, FBCModelPlugin fbcModel) {
        CyNetwork network = context.network();
        for (FluxBound fluxBound : fbcModel.getListOfFluxBounds()) {
            Reaction reaction = fluxBound.getReactionInstance();
            CyNode n = context.nodeByMetaId(reaction.getMetaId()).orElse(null);
            FluxBound.Operation operation = fluxBound.getOperation();
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
     * FIXME: unnecessary code duplication
     */
    private static void processAssociation(
            ConversionContext context, CyNode parentNode, String parentType, Association association) {
        CyNetwork network = context.network();
        // GeneProductRef
        if (association.getClass().equals(GeneProductRef.class)) {
            GeneProductRef gpRef = (GeneProductRef) association;
            CyNode gpNode = context.nodeByMetaId(gpRef.getGeneProductInstance().getMetaId())
                    .orElse(null);
            if (gpNode != null) {
                if (parentType.equals(SBML.NODETYPE_REACTION)) {
                    context.createEdge(gpNode, parentNode, SBML.INTERACTION_FBC_ASSOCIATION_REACTION);
                } else {
                    context.createEdge(gpNode, parentNode, SBML.INTERACTION_FBC_ASSOCIATION_ASSOCIATION);
                }
            } else {
                logger.error(String.format(
                        "GeneProduct does not exist for GeneAssociation: %s in %s",
                        gpRef.getGeneProduct(), association));
            }
        }
        // And
        else if (association.getClass().equals(And.class)) {
            And andRef = (And) association;

            // Create and node & edge
            CyNode andNode = network.addNode();
            AttributeUtil.set(network, andNode, SBML.LABEL, "AND", String.class);
            AttributeUtil.set(network, andNode, SBML.NODETYPE_ATTR, SBML.NODETYPE_FBC_AND, String.class);
            if (parentType.equals(SBML.NODETYPE_REACTION)) {
                context.createEdge(andNode, parentNode, SBML.INTERACTION_FBC_ASSOCIATION_REACTION);
            } else {
                context.createEdge(andNode, parentNode, SBML.INTERACTION_FBC_ASSOCIATION_ASSOCIATION);
            }
            // recursive association children
            for (Association a : andRef.getListOfAssociations()) {
                processAssociation(context, andNode, SBML.NODETYPE_FBC_AND, a);
            }
        }
        // or
        else if (association.getClass().equals(Or.class)) {
            Or orRef = (Or) association;

            // Create and node & edge
            CyNode orNode = network.addNode();
            AttributeUtil.set(network, orNode, SBML.LABEL, "OR", String.class);
            AttributeUtil.set(network, orNode, SBML.NODETYPE_ATTR, SBML.NODETYPE_FBC_OR, String.class);
            if (parentType.equals(SBML.NODETYPE_REACTION)) {
                context.createEdge(orNode, parentNode, SBML.INTERACTION_FBC_ASSOCIATION_REACTION);
            } else {
                context.createEdge(orNode, parentNode, SBML.INTERACTION_FBC_ASSOCIATION_ASSOCIATION);
            }

            // recursive association children
            for (Association a : orRef.getListOfAssociations()) {
                processAssociation(context, orNode, SBML.NODETYPE_FBC_AND, a);
            }
        }
    }
}

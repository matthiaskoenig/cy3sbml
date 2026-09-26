package org.cy3sbml.reader;

import javax.xml.stream.XMLStreamException;
import org.cy3sbml.SBML;
import org.cy3sbml.util.AttributeUtil;
import org.cy3sbml.util.MappingUtil;
import org.cy3sbml.util.SBMLUtil;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.sbml.jsbml.ASTNode;
import org.sbml.jsbml.AlgebraicRule;
import org.sbml.jsbml.AssignmentRule;
import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.Constraint;
import org.sbml.jsbml.Event;
import org.sbml.jsbml.EventAssignment;
import org.sbml.jsbml.FunctionDefinition;
import org.sbml.jsbml.InitialAssignment;
import org.sbml.jsbml.KineticLaw;
import org.sbml.jsbml.LocalParameter;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.ModifierSpeciesReference;
import org.sbml.jsbml.Parameter;
import org.sbml.jsbml.RateRule;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.Rule;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.SpeciesReference;
import org.sbml.jsbml.UnitDefinition;
import org.sbml.jsbml.Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads SBML core: the model attributes, unit and function definitions,
 * compartments, parameters, species, reactions, assignments, rules,
 * constraints and events.
 * <p>
 * The attributes derived from the complete network are added by {@link DerivedAttributes}.
 */
final class CoreReader implements PackageReader {
    private static final Logger logger = LoggerFactory.getLogger(CoreReader.class);

    @Override
    public void read(ConversionContext context, Model model) {
        logger.debug("<core>");
        readModelNode(context, model);
        readUnitDefinitions(context, model);
        readFunctionDefinitions(context, model);
        readCompartments(context, model);
        readParameters(context, model);
        readSpecies(context, model);
        readReactions(context, model);
        readInitialAssignments(context, model);
        readRules(context, model);
        readConstraints(context, model);
        readEvents(context, model);
    }

    private void readModelNode(ConversionContext context, Model model) {
        CyNetwork network = context.network();
        // SBMLDocument & Model //
        // Mark network as SBML
        AttributeUtil.set(network, network, SBML.NETWORKTYPE_ATTR, SBML.NETWORKTYPE_SBML, String.class);
        AttributeUtil.set(
                network,
                network,
                SBML.LEVEL_VERSION,
                String.format("L%1$s V%2$s", model.getLevel(), model.getVersion()),
                String.class);

        // metaId, SBO, id, name
        AttributeWriter.setNamedSBaseAttributes(network, network, model);

        // Model attributes
        if (model.isSetSubstanceUnits()) {
            AttributeUtil.set(network, network, SBML.ATTR_SUBSTANCE_UNITS, model.getSubstanceUnits(), String.class);
        }
        if (model.isSetTimeUnits()) {
            AttributeUtil.set(network, network, SBML.ATTR_TIME_UNITS, model.getTimeUnits(), String.class);
        }
        if (model.isSetVolumeUnits()) {
            AttributeUtil.set(network, network, SBML.ATTR_VOLUME_UNITS, model.getVolumeUnits(), String.class);
        }
        if (model.isSetAreaUnits()) {
            AttributeUtil.set(network, network, SBML.ATTR_AREA_UNITS, model.getAreaUnits(), String.class);
        }
        if (model.isSetLengthUnits()) {
            AttributeUtil.set(network, network, SBML.ATTR_LENGTH_UNITS, model.getLengthUnits(), String.class);
        }
        if (model.isSetExtentUnits()) {
            AttributeUtil.set(network, network, SBML.ATTR_EXTENT_UNITS, model.getExtentUnits(), String.class);
        }
        if (model.isSetConversionFactor()) {
            AttributeUtil.set(network, network, SBML.ATTR_CONVERSION_FACTOR, model.getConversionFactor(), String.class);
        }
    }

    private void readUnitDefinitions(ConversionContext context, Model model) {
        // UnitDefinition //
        for (UnitDefinition ud : model.getListOfUnitDefinitions()) {
            UnitGraphBuilder.createUnitDefinitionGraph(context, ud);
        }
    }

    private void readFunctionDefinitions(ConversionContext context, Model model) {
        CyNetwork network = context.network();
        // FunctionDefinition //
        for (FunctionDefinition fd : model.getListOfFunctionDefinitions()) {

            // implements both interfaces
            CyNode n = context.createNode(fd, SBML.NODETYPE_FUNCTION_DEFINITION);
            AttributeWriter.setNamedSBaseAttributes(network, n, fd);
            AttributeWriter.setAbstractMathContainerNodeAttributes(network, n, fd);

            // Do not create the math network for the function definition
            // The objects of the FunctionDefinition ASTNode can have different naming conventions
            // than the objects, i.e. a lambda(x), does not mean that it is called with
            // an object x
        }
    }

    private void readCompartments(ConversionContext context, Model model) {
        CyNetwork network = context.network();
        // Compartment //
        for (Compartment compartment : model.getListOfCompartments()) {
            CyNode n = context.createNode(compartment, SBML.NODETYPE_COMPARTMENT);
            AttributeWriter.setSymbolNodeAttributes(network, n, compartment);
            // edge to unit
            UnitGraphBuilder.createUnitEdge(context, n, compartment);

            if (compartment.isSetSpatialDimensions()) {
                AttributeUtil.set(
                        network, n, SBML.ATTR_SPATIAL_DIMENSIONS, compartment.getSpatialDimensions(), Double.class);
            }
            if (compartment.isSetSize()) {
                AttributeUtil.set(network, n, SBML.ATTR_SIZE, compartment.getSize(), Double.class);
            }
        }
    }

    private void readParameters(ConversionContext context, Model model) {
        CyNetwork network = context.network();
        // Parameter //
        for (Parameter parameter : model.getListOfParameters()) {
            CyNode n = context.createNode(parameter, SBML.NODETYPE_PARAMETER);
            AttributeWriter.setSymbolNodeAttributes(network, n, parameter);
            // edge to unit
            UnitGraphBuilder.createUnitEdge(context, n, parameter);
        }
    }

    private void readSpecies(ConversionContext context, Model model) {
        CyNetwork network = context.network();
        // Species //
        for (Species species : model.getListOfSpecies()) {
            CyNode n = context.createNode(species, SBML.NODETYPE_SPECIES);
            AttributeWriter.setSymbolNodeAttributes(network, n, species);
            // edge to unit
            UnitGraphBuilder.createUnitEdge(context, n, species);

            // edge to compartment
            if (species.isSetCompartment()) {
                AttributeUtil.set(network, n, SBML.ATTR_COMPARTMENT, species.getCompartment(), String.class);
                Compartment comp = species.getCompartmentInstance();
                if (comp != null) {
                    CyNode compNode = context.nodeByMetaId(comp.getMetaId()).orElse(null);
                    context.createEdge(n, compNode, SBML.INTERACTION_SPECIES_COMPARTMENT);
                } else {
                    logger.error(String.format(
                            "Compartment does not exist for species: %s for %s",
                            species.getCompartment(), species.getId()));
                }
            }
            if (species.isSetBoundaryCondition()) {
                AttributeUtil.set(
                        network, n, SBML.ATTR_BOUNDARY_CONDITION, species.getBoundaryCondition(), Boolean.class);
            }
            if (species.isSetHasOnlySubstanceUnits()) {
                AttributeUtil.set(
                        network,
                        n,
                        SBML.ATTR_HAS_ONLY_SUBSTANCE_UNITS,
                        species.getHasOnlySubstanceUnits(),
                        Boolean.class);
            }
            if (species.isSetCharge()) {
                // reason: charge is deprecated in SBML L3, but still read from older SBML levels
                @SuppressWarnings("deprecation")
                int charge = species.getCharge();
                AttributeUtil.set(network, n, SBML.ATTR_CHARGE, charge, Integer.class);
            }
            if (species.isSetConversionFactor()) {
                AttributeUtil.set(network, n, SBML.ATTR_CONVERSION_FACTOR, species.getConversionFactor(), String.class);
            }
            if (species.isSetSubstanceUnits()) {
                AttributeUtil.set(network, n, SBML.ATTR_SUBSTANCE_UNITS, species.getSubstanceUnits(), String.class);
            }
            if (species.isSetInitialAmount()) {
                AttributeUtil.set(network, n, SBML.ATTR_INITIAL_AMOUNT, species.getInitialAmount(), Double.class);
            }
            if (species.isSetInitialConcentration()) {
                AttributeUtil.set(
                        network, n, SBML.ATTR_INITIAL_CONCENTRATION, species.getInitialConcentration(), Double.class);
            }
        }
    }

    private void readReactions(ConversionContext context, Model model) {
        CyNetwork network = context.network();
        // Reaction //
        for (Reaction reaction : model.getListOfReactions()) {
            CyNode n = context.createNode(reaction, SBML.NODETYPE_REACTION);
            AttributeWriter.setNamedSBaseWithDerivedUnitAttributes(network, n, reaction);

            if (reaction.isSetReversible()) {
                AttributeUtil.set(network, n, SBML.ATTR_REVERSIBLE, reaction.getReversible(), Boolean.class);
            } else {
                // reversible=true by default
                AttributeUtil.set(network, n, SBML.ATTR_REVERSIBLE, true, Boolean.class);
            }
            if (reaction.isSetFast()) {
                // reason: fast is deprecated in SBML L3V2, but still read from older SBML versions
                @SuppressWarnings("deprecation")
                boolean fast = reaction.getFast();
                AttributeUtil.set(network, n, SBML.ATTR_FAST, fast, Boolean.class);
            }
            if (reaction.isSetCompartment()) {
                AttributeUtil.set(network, n, SBML.ATTR_COMPARTMENT, reaction.getCompartment(), String.class);
                Compartment comp = reaction.getCompartmentInstance();
                if (comp != null) {
                    // edge to compartment
                    CyNode compNode = context.nodeByMetaId(comp.getMetaId()).orElse(null);
                    context.createEdge(n, compNode, SBML.INTERACTION_REACTION_COMPARTMENT);
                } else {
                    logger.error(String.format(
                            "Compartment does not exist for reaction: %s for %s",
                            reaction.getCompartment(), reaction.getId()));
                }
            }

            // Reactants
            for (SpeciesReference speciesRef : reaction.getListOfReactants()) {
                Species species = speciesRef.getSpeciesInstance();
                if (species != null) {
                    CyNode reactantNode =
                            context.nodeByMetaId(species.getMetaId()).orElse(null);
                    CyEdge edge = context.createEdge(n, reactantNode, SBML.INTERACTION_REACTION_REACTANT);
                    AttributeWriter.setSBaseAttributes(network, edge, speciesRef);

                    Double stoichiometry = speciesRef.isSetStoichiometry() ? speciesRef.getStoichiometry() : 1.0;
                    AttributeUtil.set(network, edge, SBML.ATTR_STOICHIOMETRY, stoichiometry, Double.class);
                } else {
                    logger.error(String.format(
                            "Reactant does not exist for reaction: %s for %s",
                            speciesRef.getSpecies(), reaction.getId()));
                }
            }
            // Products
            for (SpeciesReference speciesRef : reaction.getListOfProducts()) {
                Species species = speciesRef.getSpeciesInstance();

                if (species != null) {
                    CyNode productNode =
                            context.nodeByMetaId(species.getMetaId()).orElse(null);
                    CyEdge edge = context.createEdge(n, productNode, SBML.INTERACTION_REACTION_PRODUCT);
                    AttributeWriter.setSBaseAttributes(network, edge, speciesRef);

                    Double stoichiometry = speciesRef.isSetStoichiometry() ? speciesRef.getStoichiometry() : 1.0;
                    AttributeUtil.set(network, edge, SBML.ATTR_STOICHIOMETRY, stoichiometry, Double.class);
                } else {
                    logger.error(String.format(
                            "Product does not exist for reaction: %s for %s",
                            speciesRef.getSpecies(), reaction.getId()));
                }
            }
            // Modifiers
            for (ModifierSpeciesReference msRef : reaction.getListOfModifiers()) {
                Species species = msRef.getSpeciesInstance();
                if (species != null) {
                    CyNode modifierNode =
                            context.nodeByMetaId(species.getMetaId()).orElse(null);
                    CyEdge edge = context.createEdge(n, modifierNode, SBML.INTERACTION_REACTION_MODIFIER);
                    AttributeWriter.setSBaseAttributes(network, edge, msRef);
                } else {
                    logger.error(String.format(
                            "ModifierSpecies does not exist for reaction: %s for %s",
                            msRef.getSpecies(), reaction.getId()));
                }
            }

            // Kinetic law
            if (reaction.isSetKineticLaw()) {
                KineticLaw law = reaction.getKineticLaw();
                CyNode lawNode = context.createNode(law, SBML.NODETYPE_KINETIC_LAW);
                AttributeWriter.setAbstractMathContainerNodeAttributes(network, lawNode, law);
                AttributeUtil.set(network, lawNode, SBML.LABEL, reaction.getId(), String.class);

                // edge to reaction
                CyEdge edge = network.addEdge(n, lawNode, true);
                AttributeUtil.set(
                        network, edge, SBML.INTERACTION_ATTR, SBML.INTERACTION_REACTION_KINETICLAW, String.class);

                // local parameter nodes
                if (law.isSetListOfLocalParameters()) {
                    for (LocalParameter lp : law.getListOfLocalParameters()) {
                        // This changes the SBMLDocument !
                        // but only reliable way to handle LocalParameters in math networks
                        String lpId = MappingUtil.localParameterId(lp);
                        lp.setId(lpId);

                        CyNode lpNode = context.createNode(lp, SBML.NODETYPE_LOCAL_PARAMETER);
                        AttributeWriter.setQuantityWithUnitAttributes(network, lpNode, lp);
                        // edge to unit
                        UnitGraphBuilder.createUnitEdge(context, lpNode, lp);

                        // edge to law
                        context.createEdge(lpNode, lawNode, SBML.INTERACTION_LOCALPARAMETER_KINETICLAW);
                    }
                }

                // referenced nodes in math
                if (law.isSetMath()) {
                    // set math on reaction
                    AttributeUtil.set(
                            network, n, SBML.ATTR_KINETIC_LAW, law.getMath().toFormula(), String.class);
                    MathGraphBuilder.createMathNetwork(context, law, lawNode, SBML.INTERACTION_REFERENCE_KINETICLAW);
                } else {
                    logger.warn(String.format("No math set for kinetic law in reaction: %s", reaction.getId()));
                }
            }
        }
    }

    private void readInitialAssignments(ConversionContext context, Model model) {
        CyNetwork network = context.network();
        // InitialAssignment //
        for (InitialAssignment assignment : model.getListOfInitialAssignments()) {
            Variable variable = assignment.getVariableInstance();
            if (variable != null) {
                CyNode assignmentNode = context.createNode(assignment, SBML.NODETYPE_INITIAL_ASSIGNMENT);
                AttributeWriter.setAbstractMathContainerNodeAttributes(network, assignmentNode, assignment);
                AttributeUtil.set(network, assignmentNode, SBML.ATTR_VARIABLE, variable.getId(), String.class);

                // edge to variable
                CyNode variableNode = context.nodeByMetaId(variable.getMetaId()).orElse(null);
                if (variableNode != null) {
                    context.createEdge(variableNode, assignmentNode, SBML.INTERACTION_VARIABLE_INITIAL_ASSIGNMENT);
                    if (assignment.isSetMath()) {
                        ASTNode astNode = assignment.getMath();
                        AttributeUtil.set(
                                network, variableNode, SBML.ATTR_INITIAL_ASSIGNMENT, astNode.toFormula(), String.class);
                    }
                } else {
                    logger.warn(String.format(
                            "Variable is neither Compartment, Species or Parameter, probably SpeciesReference: %s in %s",
                            variable, assignment));
                }
                // referenced nodes in math
                MathGraphBuilder.createMathNetwork(
                        context, assignment, assignmentNode, SBML.INTERACTION_REFERENCE_INITIAL_ASSIGNMENT);

            } else {
                logger.error(String.format(
                        "Variable does not exist for InitialAssignment: %s for %s", assignment.getVariable(), "?"));
            }
        }
    }

    private void readRules(ConversionContext context, Model model) {
        CyNetwork network = context.network();
        // Rule //
        for (Rule rule : model.getListOfRules()) {
            String ruleType = null;
            Variable variable = null;

            if (rule instanceof AlgebraicRule) {
                ruleType = SBML.NODETYPE_ALGEBRAIC_RULE;
            } else {
                variable = SBMLUtil.getVariableFromRule(rule);
                if (rule instanceof AssignmentRule) {
                    ruleType = SBML.NODETYPE_ASSIGNMENT_RULE;
                } else if (rule instanceof RateRule) {
                    ruleType = SBML.NODETYPE_RATE_RULE;
                }
            }

            CyNode n = context.createNode(rule, ruleType);
            AttributeWriter.setAbstractMathContainerNodeAttributes(network, n, rule);
            // referenced nodes in math
            MathGraphBuilder.createMathNetwork(context, rule, n, SBML.INTERACTION_REFERENCE_RULE);

            String label = SBMLUtil.TEMPLATE_ALGEBRAIC_RULE;
            // edge to variable for rateRule and assignmentRule
            if (variable != null) {
                if (rule instanceof AssignmentRule) {
                    label = String.format(SBMLUtil.TEMPLATE_ASSIGNMENT_RULE, variable.getId());
                } else if (rule instanceof RateRule) {
                    label = String.format(SBMLUtil.TEMPLATE_RATE_RULE, variable.getId());
                }
                AttributeUtil.set(network, n, SBML.ATTR_VARIABLE, variable.getId(), String.class);

                CyNode variableNode = context.nodeByMetaId(variable.getMetaId()).orElse(null);
                if (variableNode != null) {
                    context.createEdge(variableNode, n, SBML.INTERACTION_VARIABLE_RULE);
                } else {
                    //  An assignment rule can refer to the identifier of a Species, SpeciesReference,
                    //    Compartment, or global Parameter object in the model
                    //    The case SpeciesReference is not handled !
                    logger.warn(String.format(
                            "Variable is neither Compartment, Species or Parameter, probably SpeciesReference: %s in %s",
                            variable, rule));
                }
            }
            AttributeUtil.set(network, n, SBML.LABEL, label, String.class);
        }
    }

    private void readConstraints(ConversionContext context, Model model) {
        CyNetwork network = context.network();
        // Constraints
        // No models with constraints exist for testing.
        for (Constraint constraint : model.getListOfConstraints()) {
            CyNode n = context.createNode(constraint, SBML.NODETYPE_CONSTRAINT);
            AttributeWriter.setAbstractMathContainerNodeAttributes(network, n, constraint);
            if (constraint.isSetMessage()) {
                try {
                    AttributeUtil.set(network, n, SBML.ATTR_MESSAGE, constraint.getMessageString(), String.class);
                } catch (XMLStreamException e) {
                    logger.error("Message string could not be created for constraint.", e);
                }
            }
        }
    }

    private void readEvents(ConversionContext context, Model model) {
        CyNetwork network = context.network();
        // Events
        for (Event event : model.getListOfEvents()) {

            CyNode n = context.createNode(event, SBML.NODETYPE_EVENT);
            AttributeWriter.setNamedSBaseWithDerivedUnitAttributes(network, n, event);

            if (event.isSetUseValuesFromTriggerTime()) {
                AttributeUtil.set(
                        network,
                        n,
                        SBML.ATTR_USE_VALUES_FROM_TRIGGER_TIME,
                        event.getUseValuesFromTriggerTime(),
                        Boolean.class);
            }
            // edge via trigger math
            if (event.isSetTrigger()) {
                MathGraphBuilder.createMathNetwork(context, event.getTrigger(), n, SBML.INTERACTION_TRIGGER_EVENT);
            }
            // edge via priority math
            if (event.isSetPriority()) {
                MathGraphBuilder.createMathNetwork(context, event.getPriority(), n, SBML.INTERACTION_PRIORITY_EVENT);
            }
            // edge via delay math
            if (event.isSetDelay()) {
                MathGraphBuilder.createMathNetwork(context, event.getDelay(), n, SBML.INTERACTION_PRIORITY_EVENT);
            }

            for (EventAssignment ea : event.getListOfEventAssignments()) {
                CyNode eaNode = context.createNode(ea, SBML.NODETYPE_EVENT_ASSIGNMENT);
                AttributeWriter.setAbstractMathContainerNodeAttributes(network, eaNode, ea);

                // edge to event
                context.createEdge(n, eaNode, SBML.INTERACTION_EVENT_EVENT_ASSIGNMENT);

                // edge to variable
                if (ea.isSetVariable()) {
                    Variable variable = ea.getVariableInstance();
                    CyNode variableNode =
                            context.nodeByMetaId(variable.getMetaId()).orElse(null);
                    AttributeUtil.set(network, eaNode, SBML.LABEL, variable.getId(), String.class);
                    AttributeUtil.set(network, eaNode, SBML.ATTR_VARIABLE, variable.getId(), String.class);

                    if (variableNode != null) {
                        context.createEdge(variableNode, eaNode, SBML.INTERACTION_VARIABLE_EVENT_ASSIGNMENT);
                    } else {
                        //  An assignment rule can refer to the identifier of a Species, SpeciesReference,
                        //  Compartment, or global Parameter object in the model
                        //  The case SpeciesReference is not handled !

                        logger.warn(String.format(
                                "Variable is neither Compartment, Species or Parameter, probably SpeciesReference: %s in %s",
                                variable, ea));
                    }
                } else {
                    logger.error("Variable not set in EventAssignment: " + ea);
                }

                // referenced nodes in math
                MathGraphBuilder.createMathNetwork(context, ea, eaNode, SBML.INTERACTION_REFERENCE_EVENT_ASSIGNMENT);
            }
        }
    }
}

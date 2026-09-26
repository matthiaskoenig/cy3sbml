package org.cy3sbml.reader;

import java.util.Properties;
import org.cy3sbml.SBML;
import org.cy3sbml.util.AnnotationUtil;
import org.cy3sbml.util.AttributeUtil;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.sbml.jsbml.ASTNode;
import org.sbml.jsbml.AbstractMathContainer;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.NamedSBase;
import org.sbml.jsbml.NamedSBaseWithDerivedUnit;
import org.sbml.jsbml.QuantityWithUnit;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBase;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.Symbol;
import org.sbml.jsbml.Unit;
import org.sbml.jsbml.UnitDefinition;
import org.sbml.jsbml.ext.comp.Port;
import org.sbml.jsbml.ext.comp.SBaseRef;
import org.sbml.jsbml.ext.fbc.FBCConstants;
import org.sbml.jsbml.ext.fbc.FBCModelPlugin;
import org.sbml.jsbml.ext.fbc.GeneProduct;

/**
 * Writes the attributes of SBML objects into the node, edge and network tables.
 */
final class AttributeWriter {

    private AttributeWriter() {}

    /**
     * Set attributes for SBase.
     * In addition RDF & COBRA attributes are set.
     */
    static void setSBaseAttributes(CyNetwork network, CyIdentifiable n, SBase sbase) {
        if (sbase.isSetSBOTerm()) {
            AttributeUtil.set(network, n, SBML.ATTR_SBOTERM, sbase.getSBOTermID(), String.class);
        }
        if (sbase.isSetMetaId()) {
            AttributeUtil.set(network, n, SBML.ATTR_METAID, sbase.getMetaId(), String.class);
        }
        // RDF attributes
        // This creates Cytoscape attributes from the CV terms
        Properties props = AnnotationUtil.parseCVTerms(sbase);
        for (Object key : props.keySet()) {
            String keyString = key.toString();
            String valueString = props.getProperty((String) key);
            AttributeUtil.set(network, n, keyString, valueString, String.class);
        }
        // COBRA attributes (only for fbc models)
        Model model = sbase.getModel();
        if (model != null) {
            FBCModelPlugin fbcModel = (FBCModelPlugin) model.getExtension(FBCConstants.namespaceURI);
            if (fbcModel != null) {
                if ((sbase instanceof Reaction) || (sbase instanceof Species) || (sbase instanceof GeneProduct)) {
                    props.putAll(CobraNotesParser.parse(sbase));
                }
            }
        }

        // create attributes for properties
        for (Object key : props.keySet()) {
            String keyString = key.toString();
            String valueString = props.getProperty((String) key);
            AttributeUtil.set(network, n, keyString, valueString, String.class);
        }
    }

    /**
     * Set attributes for NamedSBase.
     *
     * @param n   CyIdentifiable to set attributes on
     * @param nsb NamedSBase
     */
    static void setNamedSBaseAttributes(CyNetwork network, CyIdentifiable n, NamedSBase nsb) {
        setSBaseAttributes(network, n, nsb);
        if (nsb.isSetId()) {
            String id = nsb.getId();
            // set in the correct namespace
            if (nsb instanceof UnitDefinition || nsb instanceof Unit) {
                AttributeUtil.set(network, n, SBML.ATTR_UNIT_SID, id, String.class);
            } else if (nsb instanceof Port) {
                AttributeUtil.set(network, n, SBML.ATTR_PORT_SID, id, String.class);
            } else {
                AttributeUtil.set(network, n, SBML.ATTR_ID, id, String.class);
            }
            AttributeUtil.set(network, n, SBML.LABEL, id, String.class);
        }
        if (nsb.isSetName()) {
            String name = nsb.getName();
            AttributeUtil.set(network, n, SBML.ATTR_NAME, name, String.class);
            AttributeUtil.set(network, n, SBML.LABEL, name, String.class);
        }
    }

    /**
     * Set attributes for SBaseRef.
     * Attributes are mutually exclusive.
     */
    static void setSBaseRefAttributes(CyNetwork network, CyIdentifiable n, SBaseRef sbaseRef) {
        if (sbaseRef.isSetPortRef()) {
            AttributeUtil.set(network, n, SBML.ATTR_COMP_PORTREF, sbaseRef.getPortRef(), String.class);
        } else if (sbaseRef.isSetIdRef()) {
            AttributeUtil.set(network, n, SBML.ATTR_COMP_IDREF, sbaseRef.getIdRef(), String.class);
        } else if (sbaseRef.isSetUnitRef()) {
            AttributeUtil.set(network, n, SBML.ATTR_COMP_UNITREF, sbaseRef.getUnitRef(), String.class);
        } else if (sbaseRef.isSetMetaIdRef()) {
            AttributeUtil.set(network, n, SBML.ATTR_COMP_METAIDREF, sbaseRef.getMetaIdRef(), String.class);
        }
    }

    /**
     * Set attributes for NamedSBaseWithDerivedUnit.
     */
    static void setNamedSBaseWithDerivedUnitAttributes(
            CyNetwork network, CyIdentifiable n, NamedSBaseWithDerivedUnit nsbu) {
        setNamedSBaseAttributes(network, n, nsbu);
        AttributeUtil.set(network, n, SBML.ATTR_DERIVED_UNITS, nsbu.getDerivedUnits(), String.class);
    }

    /**
     * Set attributes for QuantityWithUnit.
     * e.g. LocalParameters.
     */
    static void setQuantityWithUnitAttributes(CyNetwork network, CyIdentifiable n, QuantityWithUnit q) {
        setNamedSBaseWithDerivedUnitAttributes(network, n, q);
        if (q.isSetValue()) {
            AttributeUtil.set(network, n, SBML.ATTR_VALUE, q.getValue(), Double.class);
        }
        if (q.isSetUnits()) {
            AttributeUtil.set(network, n, SBML.ATTR_UNITS, q.getUnits(), String.class);
        }
    }

    /**
     * Set attributes for Symbol, e.g. Species or Parameters.
     */
    static void setSymbolNodeAttributes(CyNetwork network, CyIdentifiable n, Symbol symbol) {
        setQuantityWithUnitAttributes(network, n, symbol);
        if (symbol.isSetConstant()) {
            AttributeUtil.set(network, n, SBML.ATTR_CONSTANT, symbol.getConstant(), Boolean.class);
        }
    }

    /**
     * Set attributes for AbstractMathContainer.
     * Direct known subclasses
     * AnalyticVolume,
     * Constraint,
     * Delay,
     * EventAssignment,
     * FunctionDefinition,
     * FunctionTerm,
     * Index,
     * InitialAssignment,
     * KineticLaw,
     * Priority,
     * Rule,
     * StoichiometryMath,
     * Trigger
     */
    static void setAbstractMathContainerNodeAttributes(
            CyNetwork network, CyIdentifiable n, AbstractMathContainer container) {
        setSBaseAttributes(network, n, container);
        AttributeUtil.set(network, n, SBML.ATTR_DERIVED_UNITS, container.getDerivedUnits(), String.class);
        if (container.isSetMath()) {
            ASTNode astNode = container.getMath();
            AttributeUtil.set(network, n, SBML.ATTR_MATH, astNode.toFormula(), String.class);
        }
    }

    /**
     * Set attributes for unit.
     */
    static void setUnitAttributes(CyNetwork network, CyIdentifiable n, Unit u) {
        setSBaseAttributes(network, n, u);

        String kind = u.getKind().toString();
        AttributeUtil.set(network, n, SBML.LABEL, kind, String.class);
        AttributeUtil.set(network, n, SBML.ATTR_UNIT_KIND, kind, String.class);
        if (u.isSetExponent()) {
            AttributeUtil.set(network, n, SBML.ATTR_UNIT_EXPONENT, u.getExponent(), Double.class);
        }
        if (u.isSetScale()) {
            AttributeUtil.set(network, n, SBML.ATTR_UNIT_SCALE, u.getScale(), Integer.class);
        }
        if (u.isSetMultiplier()) {
            AttributeUtil.set(network, n, SBML.ATTR_UNIT_MULTIPLIER, u.getMultiplier(), Double.class);
        }
    }
}

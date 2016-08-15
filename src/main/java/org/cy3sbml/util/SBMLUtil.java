package org.cy3sbml.util;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import org.cy3sbml.gui.GUIConstants;
import org.cy3sbml.gui.SBaseHTMLFactory;
import org.sbml.jsbml.*;
import org.cy3sbml.SBML;
import org.sbml.jsbml.ext.SBasePlugin;
import org.sbml.jsbml.ext.comp.Port;
import org.sbml.jsbml.ext.fbc.FBCConstants;
import org.sbml.jsbml.ext.fbc.FBCReactionPlugin;
import org.sbml.jsbml.ext.fbc.FBCSpeciesPlugin;
import org.sbml.jsbml.ext.fbc.GeneProduct;
import org.sbml.jsbml.ext.qual.QualitativeSpecies;
import org.sbml.jsbml.ext.qual.Transition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Some utils to work with SBML and SBML naming.
 */
public class SBMLUtil {
    private static final Logger logger = LoggerFactory.getLogger(SBMLUtil.class);

    /**
     * Read the SBMLDocument from given SBML file resource.
     */
    public static SBMLDocument readSBMLDocument(String resource) {
        InputStream instream = SBMLUtil.class.getResourceAsStream(resource);
        SBMLDocument doc = null;
        try {
            String xml = IOUtil.inputStream2String(instream);
            doc = JSBML.readSBMLFromString(xml);
        } catch (IOException | XMLStreamException e) {
            logger.error("SBMLDocument reading failed.", e);
            e.printStackTrace();
        }
        return doc;
    }

    /**
     * Parses the notes xml from the <notes> </notes>.
     * Removes enclosing <body> elements if existing.
     * Returns null if error occurred.
     */
    public static String parseNotes(SBase sbase){
        String text = "";
        if (sbase.isSetNotes()){
            try {
                String notes = sbase.getNotesString();
                Document doc = XMLUtil.readXMLString(notes);
                XMLUtil.cleanEmptyTextNodes(doc);

                // part of nodes which are of interest
                List<Node> nodes = new LinkedList<>();

                // interested in children of notes element
                NodeList nodeList = doc.getElementsByTagName("notes");
                nodeList = nodeList.item(0).getChildNodes();

                // filter for body
                for (int k=0; k<nodeList.getLength(); k++){
                    Element e = (Element) nodeList.item(k);
                    if (e.getTagName().equals("body")){
                        NodeList children = e.getChildNodes();
                        for (int i=0; i<children.getLength(); i++){
                            nodes.add(children.item(i));
                        }
                    }else{
                        nodes.add(e);
                    }
                }

                // create xml string
                for (Node n: nodes){
                    String nText = XMLUtil.writeNodeToTidyString(n);
                    nText = nText.trim();
                    if (nText != null && !nText.equals("")) {
                        text += String.format("%s\n", nText);
                    }
                }
                return text;
            } catch (XMLStreamException e){
                logger.error("Error parsing notes xml.", e);
                e.printStackTrace();
            }

        }
        return null;
    }

    /**
     * Get the variable from AssignmentRule and RateRule.
     * Returns the variable string if set, returns null if not set or
     * if the rule is an AlgebraicRule.
     */
    public static Variable getVariableFromRule(Rule rule){
        Variable variable = null;
        if (rule.isAssignment()){
            AssignmentRule r = (AssignmentRule) rule;
            if (r.isSetVariable()) {
                return r.getVariableInstance();
            }
        } else if (rule.isRate()){
            RateRule r = (RateRule) rule;
            if (r.isSetVariable()){
                return r.getVariableInstance();
            }
        }
        return variable;
    }


    /**
     * Returns unqualified class name of a given object.
     */
    public static String getUnqualifiedClassName(Object obj){
        String name = obj.getClass().getName();
        if (name.lastIndexOf('.') > 0) {
            name = name.substring(name.lastIndexOf('.')+1);
        }
        // The $ can be converted to a .
        name = name.replace('$', '.');
        return name;
    }


    ////////////////////////////////////////////////////////////
    // Attribute maps
    ////////////////////////////////////////////////////////////
    // necessary to overwrite the SBML constants as long
    //  as not fixed in BaseReader

    public static final String ATTR_ID = "id";
    private static final String ATTR_NAME = "name";
    public static final String ATTR_COMPARTMENT = "compartment";
    public static final String ATTR_INITIAL_CONCENTRATION = "initialConcentration";
    public static final String ATTR_INITIAL_AMOUNT = "amount";
    public static final String ATTR_CHARGE = "charge";


    private static final String UNIT_TEMPLATE = "<span class=\"unit\">%s</span>";

    /** Map for SBase. */
    public static LinkedHashMap<String, String> createSBaseMap(SBase sbase){
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        map.put(SBML.ATTR_METAID,
                (sbase.isSetMetaId()) ? sbase.getMetaId() : SBaseHTMLFactory.ICON_NONE);
        return map;
    }

    /** Map for NamedSBase. */
    public static LinkedHashMap<String, String> createNamedSBaseMap(NamedSBase nsb){
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        // map.put(ATTR_ID,
        //         (sbase.isSetId()) ? sbase.getId() : ICON_NONE);
        map.put(ATTR_NAME,
                (nsb.isSetName()) ? nsb.getName() : SBaseHTMLFactory.ICON_NONE);
        map.putAll(createSBaseMap(nsb));
        return map;
    }

    /** Map for QuantityWithUnit .*/
    public static LinkedHashMap<String, String> createQuantityWithUnitNodeMap(QuantityWithUnit quantity){
        LinkedHashMap<String, String> map = createNamedSBaseMap(quantity);
        String units = quantity.isSetUnits() ? quantity.getUnits() : SBaseHTMLFactory.ICON_NONE;
        String value = quantity.isSetValue() ? ((Double) quantity.getValue()).toString() : SBaseHTMLFactory.ICON_NONE;
        map.put(SBML.ATTR_VALUE, String.format("%s "+UNIT_TEMPLATE, value, units));
        return map;
    }

    /** Map for Symbol. */
    public static LinkedHashMap<String, String> createSymbolMap(Symbol symbol){
        LinkedHashMap<String, String> map = createQuantityWithUnitNodeMap(symbol);
        map.put(SBML.ATTR_CONSTANT,
                symbol.isSetConstant() ? SBaseHTMLFactory.booleanHTML(symbol.getConstant()) : SBaseHTMLFactory.ICON_NONE
        );
        return map;
    }

    public static LinkedHashMap<String, String> createAbstractMathContainerNodeMap(AbstractMathContainer container){
        return createAbstractMathContainerNodeMap(container, null);
    }

    /** Map for AbstractMathContainer. */
    public static LinkedHashMap<String, String> createAbstractMathContainerNodeMap(AbstractMathContainer container, String variable){
        LinkedHashMap<String, String> map = createSBaseMap(container);
        String math = container.isSetMath() ? container.getMath().toFormula() : SBaseHTMLFactory.ICON_NONE;
        String units = getDerivedUnitHTML(container);
        if (variable != null){
            math = String.format("<span class=\"math\">%s = %s</span>", variable, math);
        }
        map.put(SBML.ATTR_MATH, String.format("<span class=\"math\">%s</span>", math));
        map.put(SBML.ATTR_UNITS, String.format(UNIT_TEMPLATE, units));

        return map;
    }

    /** SBMLDocument map. */
    public static LinkedHashMap<String, String> createSBMLDocumentMap(SBMLDocument doc){
        return new LinkedHashMap<>();
    }

    /** Model map. */
    public static LinkedHashMap<String, String> createModelMap(Model model){
        // packages
        Map<String, SBasePlugin> packageMap = model.getExtensionPackages();
        String packages = "";
        if (packageMap != null) {
            packages = "";
            for (SBasePlugin plugin : packageMap.values()) {
                packages += String.format("; <a href=\"%s\">%s-V%s</a>",
                        plugin.getURI(), plugin.getPackageName(), plugin.getPackageVersion());

            }
        }

        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        SBMLDocument doc = (SBMLDocument) model.getParent();

        // default
        map.put(
                String.format("<a href=\"%s\">L%sV%s</a>%s",
                        doc.getURI(), model.getLevel(), model.getVersion(), packages),
                String.format("<a href=\"%s\"><img src=\"./images/logos/sbml_icon.png\" height=\"20\" /></a>", GUIConstants.URL_SBMLFILE)
        );
        map.putAll(createNamedSBaseMap(model));

        // optional
        if (model.isSetSubstanceUnits()){
            map.put(SBML.ATTR_SUBSTANCE_UNITS, String.format(UNIT_TEMPLATE, model.getSubstanceUnits()));
        }
        if (model.isSetTimeUnits()){
            map.put(SBML.ATTR_TIME_UNITS, String.format(UNIT_TEMPLATE, model.getTimeUnits()));
        }
        if (model.isSetVolumeUnits()){
            map.put(SBML.ATTR_VOLUME_UNITS, String.format(UNIT_TEMPLATE, model.getVolumeUnits()));
        }
        if (model.isSetAreaUnits()){
            map.put(SBML.ATTR_AREA_UNITS, String.format(UNIT_TEMPLATE, model.getAreaUnits()));
        }
        if (model.isSetLengthUnits()){
            map.put(SBML.ATTR_LENGTH_UNITS, String.format(UNIT_TEMPLATE, model.getLengthUnits()));
        }
        if (model.isSetExtentUnits()){
            map.put(SBML.ATTR_EXTENT_UNITS, String.format(UNIT_TEMPLATE, model.getExtentUnits()));
        }
        if (model.isSetConversionFactor()){
            map.put(SBML.ATTR_CONVERSION_FACTOR, model.getConversionFactor());
        }
        return map;
    }

    /** FunctionDefinition map. */
    public static LinkedHashMap<String, String> createFunctionDefinitionMap(FunctionDefinition fd) {
        LinkedHashMap<String, String> map = createNamedSBaseMap(fd);
        map.putAll(createAbstractMathContainerNodeMap(fd));
        return map;
    }

    /** Compartment map. */
    public static LinkedHashMap<String, String> createCompartmentMap(Compartment compartment) {
        LinkedHashMap<String, String> map = createSymbolMap(compartment);
        map.put(SBML.ATTR_SPATIAL_DIMENSIONS,
                compartment.isSetSpatialDimensions() ? ((Double) compartment.getSpatialDimensions()).toString() : SBaseHTMLFactory.ICON_NONE
        );
        map.put(SBML.ATTR_SIZE,
                compartment.isSetSize() ? ((Double)compartment.getSize()).toString() : SBaseHTMLFactory.ICON_NONE
        );
        return map;
    }

    /** Parameter map. */
    public static LinkedHashMap<String, String> createParameterMap(Parameter p) {
        LinkedHashMap<String, String> map = createSymbolMap(p);
        return map;
    }

    /** Species map. */
    public static LinkedHashMap<String, String> createSpeciesMap(Species s) {
        LinkedHashMap<String, String> map = createSymbolMap(s);

        String compartment = SBaseHTMLFactory.ICON_NONE;
        if (s.isSetCompartment()){
            compartment = s.getCompartment().toString();
        }
        map.put(ATTR_COMPARTMENT, compartment);
        String boundaryCondition = (s.isSetBoundaryCondition()) ? SBaseHTMLFactory.booleanHTML(s.getBoundaryCondition()) : SBaseHTMLFactory.ICON_NONE;
        map.put(SBML.ATTR_BOUNDARY_CONDITION, boundaryCondition);
        String initialAmount = s.isSetInitialAmount() ? ((Double) s.getInitialAmount()).toString() : SBaseHTMLFactory.ICON_NONE;
        map.put(ATTR_INITIAL_AMOUNT, initialAmount);
        String initialConcentration = SBaseHTMLFactory.ICON_NONE;
        if (s.isSetInitialConcentration()) {
            initialConcentration = ((Double) s.getInitialConcentration()).toString();
        }
        map.put(ATTR_INITIAL_CONCENTRATION, initialConcentration);
        String hasOnlySubstanceUnits = SBaseHTMLFactory.ICON_NONE;
        if (s.isSetHasOnlySubstanceUnits()) {
            hasOnlySubstanceUnits = SBaseHTMLFactory.booleanHTML(s.getHasOnlySubstanceUnits());
        }
        map.put(SBML.ATTR_HAS_ONLY_SUBSTANCE_UNITS, hasOnlySubstanceUnits);

        // optional
        if (s.isSetCharge()){
            map.put(ATTR_CHARGE, ((Integer) s.getCharge()).toString());
        }
        if (s.isSetConversionFactor()){
            map.put(SBML.ATTR_CONVERSION_FACTOR, s.getConversionFactor());
        }
        if (s.isSetSubstanceUnits()){
            map.put(SBML.ATTR_SUBSTANCE_UNITS, s.getSubstanceUnits());
        }

        // fbc
        FBCSpeciesPlugin fbcSpecies = (FBCSpeciesPlugin) s.getExtension(FBCConstants.namespaceURI);
        if (fbcSpecies != null){
            String charge = SBaseHTMLFactory.ICON_NONE;
            if (fbcSpecies.isSetCharge()){
                charge = ((Integer) fbcSpecies.getCharge()).toString();
            }
            map.put(SBML.ATTR_FBC_CHARGE, charge);

            String chemicalFormula = SBaseHTMLFactory.ICON_NONE;
            if (fbcSpecies.isSetChemicalFormula()){
                chemicalFormula = fbcSpecies.getChemicalFormula();
            }
            map.put(SBML.ATTR_FBC_CHEMICAL_FORMULA, chemicalFormula);
        }
        return map;
    }

    /** Reaction map. */
    public static LinkedHashMap<String, String> createReactionMap(Reaction r) {
        LinkedHashMap<String, String> map = createNamedSBaseMap(r);

        String compartment = (r.isSetCompartment()) ? r.getCompartment().toString() : SBaseHTMLFactory.ICON_NONE;
        String reversible = (r.isSetReversible()) ? SBaseHTMLFactory.booleanHTML(r.getReversible()) : SBaseHTMLFactory.ICON_NONE;
        String fast = (r.isSetFast()) ? SBaseHTMLFactory.booleanHTML(r.getFast()) : SBaseHTMLFactory.ICON_NONE;
        String kineticLaw = SBaseHTMLFactory.ICON_NONE;
        if (r.isSetKineticLaw()){
            KineticLaw law = r.getKineticLaw();
            if (law.isSetMath()){
                kineticLaw = law.getMath().toFormula();
            }
        }
        String units = getDerivedUnitHTML(r);

        map.put(ATTR_COMPARTMENT, compartment);
        map.put(SBML.ATTR_REVERSIBLE, reversible);
        map.put(SBML.ATTR_FAST, fast);
        map.put(SBML.ATTR_KINETIC_LAW, kineticLaw);
        map.put(SBML.ATTR_UNITS, String.format(UNIT_TEMPLATE, units));

        // TODO: create an equation string
        // TODO: fbc flux objective from list of fluxObjectives

        // fbc
        FBCReactionPlugin fbcReaction = (FBCReactionPlugin) r.getExtension(FBCConstants.namespaceURI);
        if (fbcReaction != null){
            String lowerFluxBound = SBaseHTMLFactory.ICON_NONE;
            if (fbcReaction.isSetLowerFluxBound()){
                lowerFluxBound = fbcReaction.getLowerFluxBound();
            }
            map.put(SBML.ATTR_FBC_LOWER_FLUX_BOUND, lowerFluxBound);

            String upperFluxBound = SBaseHTMLFactory.ICON_NONE;
            if (fbcReaction.isSetUpperFluxBound()){
                upperFluxBound = fbcReaction.getUpperFluxBound();
            }
            map.put(SBML.ATTR_FBC_UPPER_FLUX_BOUND, upperFluxBound);
        }
        return map;
    }

    /** InitialAssignment map. */
    public static LinkedHashMap<String, String> createInitialAssignmentMap(InitialAssignment ass) {

        String variable = ass.isSetVariable() ? ass.getVariable() : SBaseHTMLFactory.ICON_NONE;
        LinkedHashMap<String, String> map = createAbstractMathContainerNodeMap(ass, variable);

        return map;
    }

    /** Unit map. */
    public static LinkedHashMap<String, String> createUnitMap(Unit u) {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();

        String kind = u.isSetKind() ? u.getKind().toString() : SBaseHTMLFactory.ICON_NONE;
        String exponent = u.isSetExponent() ? ((Double) u.getExponent()).toString() : SBaseHTMLFactory.ICON_NONE;
        String multiplier = u.isSetMultiplier() ? ((Double) u.getMultiplier()).toString() : SBaseHTMLFactory.ICON_NONE;
        String scale = u.isSetScale() ? ((Integer) u.getScale()).toString() : SBaseHTMLFactory.ICON_NONE;

        map.put(SBML.ATTR_UNIT_KIND, kind);
        map.put(SBML.ATTR_UNIT_EXPONENT, exponent);
        map.put(SBML.ATTR_UNIT_MULTIPLIER, multiplier);
        map.put(SBML.ATTR_UNIT_SCALE, scale);
        return map;
    }

    /** Constraint map. */
    public static LinkedHashMap<String, String> createConstraintMap(Constraint constraint) {
        LinkedHashMap<String, String> map = createAbstractMathContainerNodeMap(constraint);
        String message = SBaseHTMLFactory.ICON_NONE;
        if (constraint.isSetMessage()){
            try {
                message = constraint.getMessageString();
            } catch (XMLStreamException e) {
                logger.error("Constraint message could not be created.", e);
                e.printStackTrace();
            }
        }
        map.put(SBML.ATTR_MESSAGE, message);
        return map;
    }

    /** Event map. */
    public static LinkedHashMap<String, String> createEventMap(Event event) {
        LinkedHashMap<String, String> map = null;
        // FIXME: implement

        return map;
    }


    /** Rule map. */
    public static LinkedHashMap<String, String> createRuleMap(Rule rule) {
        Variable variable = SBMLUtil.getVariableFromRule(rule);
        String variableStr;
        if (variable == null){
            variableStr = SBaseHTMLFactory.ICON_NONE;
        } else {
            variableStr = variable.toString();
        }
        LinkedHashMap<String, String> map = createAbstractMathContainerNodeMap(rule, variableStr);
        return map;
    }

    /** LocalParameter map. */
    public static LinkedHashMap<String, String> createLocalParameterMap(LocalParameter lp) {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        KineticLaw law = (KineticLaw) lp.getParent().getParent();
        Reaction reaction = law.getParent();
        map.put("reaction", reaction.getId());
        map.putAll(createQuantityWithUnitNodeMap(lp));
        return map;
    }

    /** KineticLaw map. */
    public static LinkedHashMap<String, String> createKineticLawMap(KineticLaw law) {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        Reaction reaction = law.getParent();
        map.put("reaction", reaction.getId());
        map.putAll(createAbstractMathContainerNodeMap(law));
        return map;
    }

    /// QUAL ///

    /** QualitativeSpecies map. */
    public static LinkedHashMap<String, String> createQualitativeSpeciesMap(QualitativeSpecies qs) {
        LinkedHashMap<String, String> map = createNamedSBaseMap(qs);

        String compartment = (qs.isSetCompartment()) ? qs.getCompartment().toString() : SBaseHTMLFactory.ICON_NONE;
        String initialLevel = (qs.isSetInitialLevel()) ? ((Integer) qs.getInitialLevel()).toString() : SBaseHTMLFactory.ICON_NONE;
        String maxLevel = (qs.isSetMaxLevel()) ? ((Integer) qs.getMaxLevel()).toString() : SBaseHTMLFactory.ICON_NONE;
        String constant = (qs.isSetConstant()) ? SBaseHTMLFactory.booleanHTML(qs.getConstant()) : SBaseHTMLFactory.ICON_NONE;
        map.put(ATTR_COMPARTMENT, compartment);
        map.put(String.format("%s/s", SBML.ATTR_QUAL_INITIAL_LEVEL, SBML.ATTR_QUAL_MAX_LEVEL),
                String.format("%s/%s", initialLevel, maxLevel)
        );
        map.put(SBML.ATTR_CONSTANT, constant);
        return map;
    }

    /** Transition map. */
    public static LinkedHashMap<String, String> createTransitionMap(Transition transition) {
        LinkedHashMap<String, String> map = createNamedSBaseMap(transition);
        return map;
    }

    /// FBC ///

    /** GeneProduct map. */
    public static LinkedHashMap<String, String> createGeneProductMap(GeneProduct gp) {
        LinkedHashMap<String, String> map = createNamedSBaseMap(gp);
        return map;
    }

    /** Port map. */
    public static LinkedHashMap<String, String> createPortMap(Port port) {
        LinkedHashMap<String, String> map = createNamedSBaseMap(port);
        map.put(SBML.ATTR_COMP_PORTREF,
                port.isSetPortRef() ? port.getPortRef() : SBaseHTMLFactory.ICON_NONE
        );
        map.put(SBML.ATTR_COMP_IDREF,
                port.isSetIdRef() ? port.getIdRef() : SBaseHTMLFactory.ICON_NONE
        );
        map.put(SBML.ATTR_COMP_UNITREF,
                port.isSetUnitRef() ? port.getUnitRef() : SBaseHTMLFactory.ICON_NONE
        );
        map.put(SBML.ATTR_COMP_METAIDREF,
                port.isSetMetaIdRef() ? port.getMetaIdRef() : SBaseHTMLFactory.ICON_NONE
        );
        return map;
    }

    /** Derived unit string. */
    private static String getDerivedUnitHTML(SBaseWithDerivedUnit usbase){
        String units = SBaseHTMLFactory.ICON_NONE;
        UnitDefinition udef = usbase.getDerivedUnitDefinition();
        if (udef != null){
            units = udef.toString();
        }
        return units;
    }

}

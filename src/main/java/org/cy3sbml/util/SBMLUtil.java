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
import org.sbml.jsbml.ext.fbc.GeneProduct;
import org.sbml.jsbml.ext.qual.QualitativeSpecies;
import org.sbml.jsbml.ext.qual.Transition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.html.HTMLUListElement;

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
                logger.error("Error parsing notes xml");
                e.printStackTrace();
            }

        }
        return null;
    }


    public static String localParameterId(String reactionId, LocalParameter lp){
        return String.format("%s_%s", reactionId, lp.getId());
    }

    public static String kineticLawId(String reactionId){
        return String.format("%s_%s", reactionId, SBML.SUFFIX_KINETIC_LAW);
    }

    public static String initialAssignmentId(String variable){
        return String.format("%s_%s", variable, SBML.SUFFIX_INITIAL_ASSIGNMENT);
    }

    public static String ruleId(String variable){
        return String.format("%s_%s", variable, SBML.SUFFIX_RULE);
    }

    /**
     * Get the variable from AssignmentRule and RateRule.
     * Returns the variable string if set, returns null if not set or
     * if the rule is an AlgebraicRule.
     */
    public static String getVariableFromRule(Rule rule){
        String variable = null;
        if (rule.isAssignment()){
            AssignmentRule assignmentRule = (AssignmentRule) rule;
            if (assignmentRule.isSetVariable()) {
                variable = assignmentRule.getVariable();
            }
        } else if (rule.isRate()){
            RateRule rateRule = (RateRule) rule;
            if (rateRule.isSetVariable()){
                variable = rateRule.getVariable();
            }
            variable = rateRule.getVariable();
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

    // TODO: display additional attributes of sbase

    public static final String ATTR_ID = "id";
    private static final String ATTR_NAME = "name";
    public static final String ATTR_COMPARTMENT = "compartment";
    public static final String ATTR_INITIAL_CONCENTRATION = "initialConcentration";
    public static final String ATTR_INITIAL_AMOUNT = "amount";
    public static final String ATTR_CHARGE = "charge";


    private static final String UNIT_TEMPLATE = "<span class=\"unit\">%s</span>";

    /** Map with metaid information. */
    public static LinkedHashMap<String, String> createSBaseMap(SBase sbase){
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        map.put(SBML.ATTR_METAID,
                (sbase.isSetMetaId()) ? sbase.getMetaId() : SBaseHTMLFactory.ICON_NONE);
        return map;
    }

    /** Map with name information. */
    public static LinkedHashMap<String, String> createNamedSBaseMap(NamedSBase nsb){
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        // map.put(ATTR_ID,
        //         (sbase.isSetId()) ? sbase.getId() : ICON_NONE);
        map.put(ATTR_NAME,
                (nsb.isSetName()) ? nsb.getName() : SBaseHTMLFactory.ICON_NONE);
        map.putAll(createSBaseMap(nsb));
        return map;
    }

    /** Model map. */
    public static LinkedHashMap<String, String> createModelMap(Model model){
        // packages
        Map<String, SBasePlugin> packageMap = model.getExtensionPackages();
        String packages = "";
        if (packageMap.size()>0) {
            packages = "[";
            for (String key : packageMap.keySet()) {
                packages += String.format("%s; ", key);
            }
            packages += "]";
        }

        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        // default
        map.put(
                String.format("L%sV%s %s", model.getLevel(), model.getVersion(), packages),
                String.format("<a href=\"%s\"><img src=\"./images/logos/sbml_icon.png\" height=\"20\" /></a>", GUIConstants.URL_SBMLFILE)
        );
        map.putAll(createNamedSBaseMap(model));

        // optional
        if (model.isSetSubstanceUnits()){
            map.put(SBML.ATTR_SUBSTANCE_UNITS, model.getSubstanceUnits());
        }
        if (model.isSetTimeUnits()){
            map.put(SBML.ATTR_TIME_UNITS, model.getTimeUnits());
        }
        if (model.isSetVolumeUnits()){
            map.put(SBML.ATTR_VOLUME_UNITS, model.getVolumeUnits());
        }
        if (model.isSetAreaUnits()){
            map.put(SBML.ATTR_AREA_UNITS, model.getAreaUnits());
        }
        if (model.isSetLengthUnits()){
            map.put(SBML.ATTR_LENGTH_UNITS, model.getLengthUnits());
        }
        if (model.isSetExtentUnits()){
            map.put(SBML.ATTR_EXTENT_UNITS, model.getExtentUnits());
        }
        if (model.isSetConversionFactor()){
            map.put(SBML.ATTR_CONVERSION_FACTOR, model.getConversionFactor());
        }
        return map;
    }

    /** Compartment map. */
    public static LinkedHashMap<String, String> createCompartmentMap(Compartment compartment) {
        LinkedHashMap<String, String> map = createNamedSBaseMap(compartment);
        map.put(SBML.ATTR_SPATIAL_DIMENSIONS,
                compartment.isSetSpatialDimensions() ? ((Double) compartment.getSpatialDimensions()).toString() : SBaseHTMLFactory.ICON_NONE
        );
        map.put(SBML.ATTR_SIZE,
                compartment.isSetSize() ? ((Double)compartment.getSize()).toString() : SBaseHTMLFactory.ICON_NONE
        );
        map.put(SBML.ATTR_UNITS,
                compartment.isSetUnits() ? String.format("%s "+UNIT_TEMPLATE, compartment.getUnits()) : SBaseHTMLFactory.ICON_NONE
        );
        map.put(SBML.ATTR_CONSTANT,
                compartment.isSetConstant() ? SBaseHTMLFactory.booleanHTML(compartment.getConstant()) : SBaseHTMLFactory.ICON_NONE
        );
        return map;
    }

    /** Parameter map. */
    public static LinkedHashMap<String, String> createParameterMap(Parameter p) {
        LinkedHashMap<String, String> map = createNamedSBaseMap(p);
        map.put(SBML.ATTR_VALUE,
                p.isSetValue() ? ((Double) p.getValue()).toString() : SBaseHTMLFactory.ICON_NONE
        );
        map.put(SBML.ATTR_UNITS,
                p.isSetUnits() ? String.format("%s "+UNIT_TEMPLATE, p.getUnits()) : SBaseHTMLFactory.ICON_NONE
        );
        map.put(SBML.ATTR_CONSTANT,
                p.isSetConstant() ? SBaseHTMLFactory.booleanHTML(p.getConstant()) : SBaseHTMLFactory.ICON_NONE
        );
        return map;
    }

    /** InitialAssignment map. */
    public static LinkedHashMap<String, String> createInitialAssignmentMap(InitialAssignment ass) {
        LinkedHashMap<String, String> map = createSBaseMap(ass);
        String variable = ass.isSetVariable() ? ass.getVariable() : SBaseHTMLFactory.ICON_NONE;
        String math = ass.isSetMath() ? ass.getMath().toFormula() : SBaseHTMLFactory.ICON_NONE;
        map.put(variable, String.format("= %s", math));
        return map;
    }

    /** Rule map. */
    public static LinkedHashMap<String, String> createRuleMap(Rule rule) {
        LinkedHashMap<String, String> map = createSBaseMap(rule);
        String math = rule.isSetMath() ? rule.getMath().toFormula() : SBaseHTMLFactory.ICON_NONE;
        String variable = SBMLUtil.getVariableFromRule(rule);
        if (variable == null){
            variable = SBaseHTMLFactory.ICON_NONE;
        }
        map.put(variable, String.format("= %s", math));
        return map;
    }

    /** LocalParameter map. */
    public static LinkedHashMap<String, String> createLocalParameterMap(LocalParameter lp) {
        LinkedHashMap<String, String> map = createNamedSBaseMap(lp);
        String value = (lp.isSetValue()) ? ((Double) lp.getValue()).toString() : SBaseHTMLFactory.ICON_NONE;
        String units = (lp.isSetUnits()) ? lp.getUnits() : SBaseHTMLFactory.ICON_NONE;
        map.put(SBML.ATTR_VALUE, String.format("%s "+UNIT_TEMPLATE, value, units));
        return map;
    }

    /** Species map. */
    public static LinkedHashMap<String, String> createSpeciesMap(Species s) {
        LinkedHashMap<String, String> map = createNamedSBaseMap(s);

        String compartment = (s.isSetCompartment()) ? s.getCompartment().toString() : SBaseHTMLFactory.ICON_NONE;
        String value = (s.isSetValue()) ? ((Double) s.getValue()).toString() : SBaseHTMLFactory.ICON_NONE;
        String units = getDerivedUnitString(s);
        String constant = (s.isSetConstant()) ? SBaseHTMLFactory.booleanHTML(s.isConstant()) : SBaseHTMLFactory.ICON_NONE;
        String boundaryCondition = (s.isSetBoundaryCondition()) ? SBaseHTMLFactory.booleanHTML(s.getBoundaryCondition()) : SBaseHTMLFactory.ICON_NONE;

        map.put(ATTR_COMPARTMENT, compartment);
        map.put(SBML.ATTR_VALUE, String.format("%s "+UNIT_TEMPLATE, value, units));
        map.put(SBML.ATTR_CONSTANT, constant);
        map.put(SBML.ATTR_BOUNDARY_CONDITION, boundaryCondition);

        // TODO: charge & package information (formula, charge)

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
        String units = getDerivedUnitString(r);

        map.put(ATTR_COMPARTMENT, compartment);
        map.put(SBML.ATTR_REVERSIBLE, reversible);
        map.put(SBML.ATTR_FAST, fast);
        map.put(SBML.ATTR_KINETIC_LAW, kineticLaw);
        map.put(SBML.ATTR_UNITS, String.format(UNIT_TEMPLATE, units));

        // TODO: extension information (upper & lower bound, objective)

        return map;
    }

    /** KineticLaw map. */
    public static LinkedHashMap<String, String> createKineticLawMap(KineticLaw law) {
        LinkedHashMap<String, String> map = createSBaseMap(law);
        map.put(SBML.ATTR_KINETIC_LAW,
                law.isSetMath() ? law.getMath().toFormula() : SBaseHTMLFactory.ICON_NONE
        );
        return map;
    }

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
        // TODO: implement
        return map;
    }

    /** GeneProduct map. */
    public static LinkedHashMap<String, String> createGeneProductMap(GeneProduct gp) {
        LinkedHashMap<String, String> map = createNamedSBaseMap(gp);
        // TODO: implement
        return map;
    }

    /** FunctionDefinition map. */
    public static LinkedHashMap<String, String> createFunctionDefinitionMap(FunctionDefinition fd) {
        LinkedHashMap<String, String> map = createNamedSBaseMap(fd);
        String math = (fd.isSetMath()) ? fd.getMath().toFormula() : SBaseHTMLFactory.ICON_NONE;
        map.put(SBML.ATTR_MATH, math);
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
    private static String getDerivedUnitString(SBaseWithDerivedUnit usbase){
        String units = SBaseHTMLFactory.ICON_NONE;
        UnitDefinition udef = usbase.getDerivedUnitDefinition();
        if (udef != null){
            units = udef.toString();
        }
        return units;
    }

}

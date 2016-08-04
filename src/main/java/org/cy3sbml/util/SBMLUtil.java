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
        for (String key: packageMap.keySet()){
            packages += String.format("%s; ", key);
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
                (compartment.isSetSpatialDimensions()) ? ((Double) compartment.getSpatialDimensions()).toString() : SBaseHTMLFactory.ICON_NONE
        );
        map.put(SBML.ATTR_SIZE,
                (compartment.isSetSize()) ? ((Double)compartment.getSize()).toString() : SBaseHTMLFactory.ICON_NONE
        );
        map.put(SBML.ATTR_UNITS,
                (compartment.isSetUnits()) ? String.format("%s <span class=\"unit\">%s</span>", compartment.getUnits()) : SBaseHTMLFactory.ICON_NONE
        );
        map.put(SBML.ATTR_CONSTANT,
                (compartment.isSetConstant()) ? SBaseHTMLFactory.booleanHTML(compartment.getConstant()) : SBaseHTMLFactory.ICON_NONE
        );
        return map;
    }

    /** Parameter map. */
    public static LinkedHashMap<String, String> createParameterMap(Parameter p) {
        LinkedHashMap<String, String> map = createNamedSBaseMap(compartment);

        private static final String TEMPLATE_PARAMETER =
                TABLE_START +
                        TS + "value" + TM + "%s <span class=\"unit\">%s</span>" + TE +
                        TS + "constant" + TM + "%s" +
                        TABLE_END;

        String value = (p.isSetValue()) ? ((Double) p.getValue()).toString() : ICON_NONE;
        String units = (p.isSetUnits()) ? p.getUnits() : ICON_NONE;
        String constant = (p.isSetConstant()) ? booleanHTML(p.getConstant()) : ICON_NONE;
        return String.format(TEMPLATE_PARAMETER, value, units, constant);



        return map;
    }

    /** InitialAssignment map. */
    public static LinkedHashMap<String, String> createInitialAssignmentMap(InitialAssignment ass) {
        LinkedHashMap<String, String> map = createNamedSBaseMap(ass);
        private static final String TEMPLATE_INITIAL_ASSIGNMENT =
                TABLE_START +
                        TS + "%s" + TM + "= %s" + TE +
                        TABLE_END;

        String variable = (ass.isSetVariable()) ? ass.getVariable() : ICON_NONE;
        String math = (ass.isSetMath()) ? ass.getMath().toFormula() : ICON_NONE;
        return String.format(TEMPLATE_INITIAL_ASSIGNMENT, variable, math);


        return map;
    }

    /** Rule map. */
    public static LinkedHashMap<String, String> createRuleMap(Rule rule) {
        LinkedHashMap<String, String> map = createNamedSBaseMap(rule);

        private static final String TEMPLATE_RULE = TEMPLATE_INITIAL_ASSIGNMENT;

        String math = (rule.isSetMath()) ? rule.getMath().toFormula() : ICON_NONE;
        String variable = SBMLUtil.getVariableFromRule(rule);
        if (variable == null){
            variable = ICON_NONE;
        }
        return String.format(TEMPLATE_RULE, variable, math);

        return map;
    }

    /** LocalParameter map. */
    public static LinkedHashMap<String, String> createLocalParameterMap(LocalParameter lp) {
        LinkedHashMap<String, String> map = createNamedSBaseMap(lp);

        private static final String TEMPLATE_LOCAL_PARAMETER =
                TABLE_START +
                        TS + "value" + TM + "%s <span class=\"unit\">%s</span>" + TE +
                        TABLE_END;

        String value = (lp.isSetValue()) ? ((Double) lp.getValue()).toString() : ICON_NONE;
        String units = (lp.isSetUnits()) ? lp.getUnits() : ICON_NONE;
        return String.format(TEMPLATE_LOCAL_PARAMETER, value, units);

        return map;
    }


    /** Species map. */
    public static LinkedHashMap<String, String> createSpeciesMap(Species species) {
        LinkedHashMap<String, String> map = createNamedSBaseMap(species);

        private static final String TEMPLATE_SPECIES =
                TABLE_START +
                        TS + "compartment" + TM + "%s" + TE +
                        TS + "value" + TM + "%s <span class=\"unit\">%s</span>" + TE +
                        TS + "constant" + TM + "%s" + TE +
                        TS + "boundaryCondition" + TM + "%s" + TE +
                        TABLE_END;

        String compartment = (s.isSetCompartment()) ? s.getCompartment().toString() : ICON_NONE;
        String value = (s.isSetValue()) ? ((Double) s.getValue()).toString() : ICON_NONE;
        String units = getDerivedUnitString((AbstractNamedSBaseWithUnit) item);
        String constant = (s.isSetConstant()) ? booleanHTML(s.isConstant()) : ICON_NONE;
        String boundaryCondition = (s.isSetBoundaryCondition()) ? booleanHTML(s.getBoundaryCondition()) : ICON_NONE;

        // TODO: charge & package information (formula, charge)

        return String.format(TEMPLATE_SPECIES, compartment, value, units, constant, boundaryCondition);

        return map;
    }


    /** Reaction map. */
    public static LinkedHashMap<String, String> createReactionMap(Reaction reaction) {
        LinkedHashMap<String, String> map = createNamedSBaseMap(reaction);

        private static final String TEMPLATE_REACTION =
                TABLE_START +
                        TS + "compartment" + TM + "%s" + TE +
                        TS + "reversible" + TM + "%s" + TE +
                        TS + "fast" + TM + "%s" + TE +
                        TS + "kineticLaw" + TM + "%s" + TE +
                        TS + "units" + TM + "<span class=\"unit\">%s</span>" + TE +
                        TABLE_END;

        String compartment = (r.isSetCompartment()) ? r.getCompartment().toString() : ICON_NONE;
        String reversible = (r.isSetReversible()) ? booleanHTML(r.getReversible()) : ICON_NONE;
        String fast = (r.isSetFast()) ? booleanHTML(r.getFast()) : ICON_NONE;
        String kineticLaw = ICON_NONE;
        if (r.isSetKineticLaw()){
            KineticLaw law = r.getKineticLaw();
            if (law.isSetMath()){
                kineticLaw = law.getMath().toFormula();
            }
        }
        String units = getDerivedUnitString((SBaseWithDerivedUnit) item);

        // TODO: extension information (upper & lower bound, objective)

        return String.format(TEMPLATE_REACTION,
                compartment,
                reversible,
                fast,
                kineticLaw,
                units);

        return map;
    }

    /** KineticLaw map. */
    public static LinkedHashMap<String, String> createKineticLawMap(KineticLaw law) {
        LinkedHashMap<String, String> map = createNamedSBaseMap(law);

        private static final String TEMPLATE_KINETIC_LAW =
                TABLE_START +
                        TS + "kineticLaw" + TM + "%s" + TE +
                        TABLE_END;

        String kineticLaw = (law.isSetMath()) ? law.getMath().toFormula() : ICON_NONE;
        return String.format(TEMPLATE_KINETIC_LAW, kineticLaw);

        return map;
    }

    /** QualitativeSpecies map. */
    public static LinkedHashMap<String, String> createQualitativeSpeciesMap(QualitativeSpecies qs) {
        LinkedHashMap<String, String> map = createNamedSBaseMap(qs);

        private static final String TEMPLATE_QUALITATIVE_SPECIES =
                TABLE_START +
                        TS + "compartment" + TM + "%s" + TE +
                        TS + "initial/max level" + TM + "%s/%s" + TE +
                        TS + "constant" + TM + "%s" + TE +
                        TABLE_END;

        String compartment = (qs.isSetCompartment()) ? qs.getCompartment().toString() : ICON_NONE;
        String initialLevel = (qs.isSetInitialLevel()) ? ((Integer) qs.getInitialLevel()).toString() : ICON_NONE;
        String maxLevel = (qs.isSetMaxLevel()) ? ((Integer) qs.getMaxLevel()).toString() : ICON_NONE;
        String constant = (qs.isSetConstant()) ? booleanHTML(qs.getConstant()) : ICON_NONE;
        return String.format(TEMPLATE_QUALITATIVE_SPECIES, compartment, initialLevel, maxLevel, constant);

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

        String math = (fd.isSetMath()) ? fd.getMath().toFormula() : ICON_NONE;
        return String.format(TEMPLATE_KINETIC_LAW, math);

        return map;
    }

    /** Port map. */
    public static LinkedHashMap<String, String> createPortMap(Port port) {
        LinkedHashMap<String, String> map = createNamedSBaseMap(port);

        private static final String TEMPLATE_PORT =
                TABLE_START +
                        TS + "portRef" + TM + "%s " + TE +
                        TS + "idRef" + TM + "%s " + TE +
                        TS + "unitRef" + TM + "%s " + TE +
                        TS + "metaIdRef" + TM + "%s" + TE +
                        TABLE_END;

        String portRef = (port.isSetPortRef()) ? port.getPortRef() : ICON_NONE;
        String idRef = (port.isSetIdRef()) ? port.getIdRef() : ICON_NONE;
        String unitRef = (port.isSetUnitRef()) ? port.getUnitRef() : ICON_NONE;
        String metaIdRef = (port.isSetMetaIdRef()) ? port.getMetaIdRef() : ICON_NONE;
        return String.format(TEMPLATE_PORT, portRef, idRef, unitRef, metaIdRef);

        return map;
    }

}
